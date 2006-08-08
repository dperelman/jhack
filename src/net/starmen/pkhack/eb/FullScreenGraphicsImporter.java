/*
 * Created on Apr 17, 2006
 */
package net.starmen.pkhack.eb;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import net.starmen.pkhack.BMPReader;
import net.starmen.pkhack.ByteArrHasher;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.eb.LogoScreenEditor.LogoScreen;

public class FullScreenGraphicsImporter
{
    private FullScreenGraphics target;
    private Component parent;

    public FullScreenGraphicsImporter(FullScreenGraphics target,
        Component parent)
    {
        if (target == null)
        {
            throw new IllegalArgumentException(
                "FullScreenGraphicsImporter requires a non-null target.");
        }
        this.target = target;
        this.parent = parent;
    }

    public void setTarget(FullScreenGraphics target)
    {
        if (target != null)
        {
            this.target = target;
        }
    }

    public void setParent(Component parent)
    {
        this.parent = parent;
    }

    public static void importImg(FullScreenGraphics target, Component parent)
    {
        new FullScreenGraphicsImporter(target, parent).importImg();
    }

    public static void importImg(FullScreenGraphics target, Component parent,
        File f)
    {
        new FullScreenGraphicsImporter(target, parent).importImg(f);
    }

    public static void importImg(FullScreenGraphics target, Component parent,
        Image img)
    {
        new FullScreenGraphicsImporter(target, parent).importImg(img);
    }

    public static void importImg(FullScreenGraphics target, Component parent,
        boolean singlePal)
    {
        new FullScreenGraphicsImporter(target, parent).importImg(singlePal);
    }

    public static void importImg(FullScreenGraphics target, Component parent,
        File f, boolean singlePal)
    {
        new FullScreenGraphicsImporter(target, parent).importImg(f, singlePal);
    }

    public static void importImg(FullScreenGraphics target, Component parent,
        Image img, boolean singlePal)
    {
        new FullScreenGraphicsImporter(target, parent)
            .importImg(img, singlePal);
    }

    public void importImg()
    {
        importImg(false);
    }

    public void importImg(File f)
    {
        importImg(f, false);
    }

    public void importImg(Image img)
    {
        importImg(img, false);
    }

    public void importImg(boolean singlePal)
    {
        importImg(HackModule.getFile(false, new String[]{"bmp", "gif", "png"},
            new String[]{"Windows BitMaP", "Compuserv GIF format",
                "Portable Network Graphics"}), singlePal);
    }

    public void importImg(File f, boolean singlePal)
    {
        if (f == null)
            return;

        Image img;
        try
        {
            if (f.getName().endsWith(".bmp"))
            {
                FileInputStream in = new FileInputStream(f);
                img = parent.createImage(BMPReader.getBMPImage(in));
                in.close();
            }
            else
            {
                img = ImageIO.read(f);
            }
        }
        catch (IOException e1)
        {
            JOptionPane.showMessageDialog(parent, "Unexpected IO error:\n"
                + e1.getLocalizedMessage(), "ERROR: IO Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        importImg(img, singlePal);
    }

    public void importImg(Image img, boolean singlePal)
    {
        int w = img.getWidth(parent), h = img.getHeight(parent);
        if (w != 256)
        {
            JOptionPane.showMessageDialog(parent,
                "Image width must be 256 pixels.", "ERROR: Invalid width",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (h != 224)
        {
            JOptionPane.showMessageDialog(parent,
                "Image height must be 224 pixels.", "ERROR: Invalid height",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        int[] pixels = new int[w * h];

        PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
        try
        {
            pg.grabPixels();
        }
        catch (InterruptedException e)
        {
            System.err.println("Interrupted waiting for pixels!");
            return;
        }
        if (singlePal)
        {
            importImgOnePass(pixels);
        }
        else
        {
            importImgPassOne(pixels);
        }
    }

    private void importImgOnePass(int[] pixels)
    {
        short[][] arrangement = new short[32][28];
        byte[][] tiles = new byte[target.getNumTiles()][64];
        Color[] pal = new Color[target.getSubPaletteSize()];
        Arrays.fill(pal, new Color(0, 0, 0));
        int tnum = 0, pnum = 0;
        Hashtable tilerefs = new Hashtable(), palrefs = new Hashtable();
        for (int yt = 0; yt < 28; yt++)
        {
            int js = yt * 8;
            for (int xt = 0; xt < 32; xt++)
            {
                int is = xt * 8;
                byte[] tile = new byte[64];
                for (int j = 0; j < 8; j++)
                {
                    for (int i = 0; i < 8; i++)
                    {
                        // sprite[i][j] = pixels[j * w + i];
                        int c = pixels[((j + js) * 256) + (i + is)];
                        Color col = new Color(c & 0xf8f8f8);
                        int cn;
                        Object tmpc;
                        if ((tmpc = palrefs.get(col)) != null)
                        {
                            cn = ((Integer) tmpc).intValue();
                        }
                        else
                        {
                            cn = pnum;
                            palrefs.put(col, new Integer(pnum));
                            try
                            {
                                pal[pnum++] = col;
                            }
                            catch (ArrayIndexOutOfBoundsException e)
                            {
                                JOptionPane
                                    .showMessageDialog(
                                        parent,
                                        "Image must have no more than "
                                            + target.getSubPaletteSize()
                                            + " colors.\n"
                                            + "Please use the dithering option on your\n"
                                            + "favorite image editor program to decrease\n"
                                            + "the number of colors to "
                                            + target.getSubPaletteSize() + ".",
                                        "ERROR: Too many colors",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        tile[(j * 8) + i] = (byte) cn;
                    }
                }
                TileRef tn;
                byte[] htile, vtile, hvtile;
                htile = HackModule.hFlip(tile);
                vtile = HackModule.vFlip(tile);
                hvtile = HackModule.hFlip(vtile);

                Object tmpt;
                if ((tmpt = tilerefs.get(new ByteArrHasher(tile).toInteger())) != null)
                {
                    tn = (TileRef) tmpt;
                }
                else
                {
                    tn = new TileRef(tnum, false, false);
                    tilerefs.put(new ByteArrHasher(tile).toInteger(), tn);
                    tilerefs.put(new ByteArrHasher(htile).toInteger(),
                        new TileRef(tnum, true, false));
                    tilerefs.put(new ByteArrHasher(vtile).toInteger(),
                        new TileRef(tnum, false, true));
                    tilerefs.put(new ByteArrHasher(hvtile).toInteger(),
                        new TileRef(tnum, true, true));
                    try
                    {
                        tiles[tnum++] = tile;
                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {
                        JOptionPane
                            .showMessageDialog(
                                parent,
                                "Image must have no more than "
                                    + tiles.length
                                    + " unique 8x8 tiles.\n"
                                    + "Flipping of tiles is currently not supported.",
                                "ERROR: Too many tiles",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                arrangement[xt][yt] = tn.getArrangementData();
            }
        }

        for (int i = 0; i < target.palette.length; i++)
        {
            System.arraycopy(pal, 0, target.palette[i], 0, pal.length);
        }
        for (int t = 0; t < tnum; t++)
        {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    target.tiles[t][x][y] = tiles[t][(y * 8) + x];
        }
        for (int t = tnum; t < target.tiles.length; t++)
        {
            target.tiles[t] = new byte[8][8];
        }
        target.arrangement = arrangement;
    }

    private void importImgPassOne(int[] pixels)
    {

        Set pals = new HashSet();
        Set[][] tilepalhash = new Set[32][28];
        for (int yt = 0; yt < 28; yt++)
        {
            int js = yt * 8;
            for (int xt = 0; xt < 32; xt++)
            {
                int is = xt * 8;
                Set pal = new HashSet();
                for (int j = 0; j < 8; j++)
                {
                    for (int i = 0; i < 8; i++)
                    {
                        int c = pixels[((j + js) * 256) + (i + is)];
                        Color col = new Color(c & 0xf8f8f8);
                        pal.add(col);
                        if (pal.size() > target.getSubPaletteSize())
                        {
                            JOptionPane
                                .showMessageDialog(
                                    parent,
                                    "Image must have no more than "
                                        + target.getSubPaletteSize()
                                        + " colors per 8x8 tile.\n"
                                        + "Please use the dithering option on your\n"
                                        + "favorite image editor program to decrease\n"
                                        + "the number of colors per 8x8 tile to "
                                        + target.getSubPaletteSize() + ".",
                                    "ERROR: Too many colors",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                pals.add(pal);
                tilepalhash[xt][yt] = pal;
            }
        }

        Hashtable hashhash = mergePals(pals);

        if (pals.size() > LogoScreen.NUM_PALETTES)
        {
            JOptionPane.showMessageDialog(parent,
                "Image must have no more than " + LogoScreen.NUM_PALETTES
                    + " unique palettes of " + target.getSubPaletteSize()
                    + " colors.\n"
                    + "Please use the dithering option on your\n"
                    + "favorite image editor program to decrease\n"
                    + "the number of colors per 8x8 tile to "
                    + target.getSubPaletteSize()
                    + " and number of palettes to " + LogoScreen.NUM_PALETTES
                    + ".", "ERROR: Too many colors", JOptionPane.ERROR_MESSAGE);
            return;
        }

        byte[][] tilepal = new byte[32][28];
        Color[][] palarr = new Color[LogoScreen.NUM_PALETTES][target
            .getSubPaletteSize()];
        Hashtable palhash = new Hashtable();
        byte pnum = 0;
        for (Iterator i = pals.iterator(); i.hasNext();)
        {
            int cnum = 0;
            Set tmp = (Set) i.next();
            palhash.put(tmp, new Byte(pnum));
            for (Iterator j = tmp.iterator(); j.hasNext();)
            {
                palarr[pnum][cnum++] = (Color) j.next();
            }
            for (; cnum < target.getSubPaletteSize(); cnum++)
            {
                palarr[pnum][cnum] = Color.BLACK;
            }
            pnum++;
        }
        for (; pnum < palarr.length; pnum++)
        {
            Arrays.fill(palarr[pnum], Color.BLACK);
        }

        // System.out.println("hashhash: " + hashhash);
        // System.out.println("palhash: " + palhash);

        for (int yt = 0; yt < 28; yt++)
        {
            for (int xt = 0; xt < 32; xt++)
            {
                Set phash = tilepalhash[xt][yt];
                while (hashhash.containsKey(phash))
                {
                    phash = (Set) hashhash.get(phash);
                }
                tilepal[xt][yt] = ((Byte) palhash.get(phash)).byteValue();
            }
        }

        importImgPassTwo(pixels, palarr, tilepal);
    }

    private boolean superSetExists(Set pals, Set pal, Hashtable out)
    {
        for (Iterator j = pals.iterator(); j.hasNext();)
        {
            Set opal = (Set) j.next();
            if (!pal.equals(opal) && opal.containsAll(pal))
            {
                out.put(pal, opal);
                return true;
            }
        }
        return false;
    }

    private boolean findMergeable(Set pals, Set pal, Hashtable out)
    {
        for (Iterator j = pals.iterator(); j.hasNext();)
        {
            Set opal = (Set) j.next();
            if (!pal.equals(opal))
            {
                Set upal = new HashSet(opal);
                upal.addAll(pal);
                if (upal.size() <= target.getSubPaletteSize())
                {
                    pals.remove(pal);
                    pals.remove(opal);
                    pals.add(upal);
                    out.put(pal, upal);
                    out.put(opal, upal);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes extranous palettes.
     * 
     * @param pals a Set of Set's of Color's.
     */
    private Hashtable mergePals(Set pals)
    {
        Hashtable out = new Hashtable();
        for (Iterator i = pals.iterator(); i.hasNext();)
        {
            if (superSetExists(pals, (Set) i.next(), out))
            {
                i.remove();
            }
        }

        for (Iterator i = pals.iterator(); i.hasNext();)
        {
            if (findMergeable(pals, (Set) i.next(), out))
            {
                /* Need to restart because we removed elements. */
                i = pals.iterator();
            }
        }
        return out;
    }

    private void importImgPassTwo(int[] pixels, Color[][] pals, byte[][] tilepal)
    {
        short[][] arrangement = new short[32][28];
        byte[][] tiles = new byte[LogoScreen.NUM_TILES][64];
        int tnum = 0;
        Hashtable tilerefs = new Hashtable();

        for (int yt = 0; yt < 28; yt++)
        {
            int js = yt * 8;
            for (int xt = 0; xt < 32; xt++)
            {
                int is = xt * 8;
                byte[] tile = new byte[64];
                Color[] pal = pals[tilepal[xt][yt]];
                for (int j = 0; j < 8; j++)
                {
                    for (int i = 0; i < 8; i++)
                    {
                        int c = pixels[((j + js) * 256) + (i + is)];
                        Color col = new Color(c & 0xf8f8f8);
                        byte cn = -1;
                        for (byte k = 0; k < pal.length; k++)
                        {
                            if (col.equals(pal[k]))
                            {
                                cn = k;
                            }
                        }
                        if (cn == -1)
                        {
                            JOptionPane.showMessageDialog(parent,
                                "The color could not be found in "
                                    + "the palette.\n"
                                    + "This should not be possible.",
                                "ERROR: Invalid palette",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        tile[(j * 8) + i] = cn;
                    }
                }
                TileRef tn;
                byte[] htile, vtile, hvtile;
                htile = HackModule.hFlip(tile);
                vtile = HackModule.vFlip(tile);
                hvtile = HackModule.hFlip(vtile);

                Object tmpt;
                if ((tmpt = tilerefs.get(new ByteArrHasher(tile).toInteger())) != null)
                {
                    tn = (TileRef) tmpt;
                }
                else
                {
                    tn = new TileRef(tnum, false, false);
                    tilerefs.put(new ByteArrHasher(tile).toInteger(), tn);
                    tilerefs.put(new ByteArrHasher(htile).toInteger(),
                        new TileRef(tnum, true, false));
                    tilerefs.put(new ByteArrHasher(vtile).toInteger(),
                        new TileRef(tnum, false, true));
                    tilerefs.put(new ByteArrHasher(hvtile).toInteger(),
                        new TileRef(tnum, true, true));
                    try
                    {
                        tiles[tnum++] = tile;
                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {
                        JOptionPane
                            .showMessageDialog(
                                parent,
                                "Image must have no more than "
                                    + tiles.length
                                    + " unique 8x8 tiles.\n"
                                    + "Flipping of tiles is currently not supported.",
                                "ERROR: Too many tiles",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                tn = new TileRef(tn.getTile(), tn.isHFlip(), tn.isVFlip(),
                    tilepal[xt][yt]);
                arrangement[xt][yt] = tn.getArrangementData();
            }
        }

        for (int i = 0; i < target.palette.length; i++)
        {
            System.arraycopy(pals[i], 0, target.palette[i], 0, pals[i].length);
        }
        for (int t = 0; t < tnum; t++)
        {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    target.tiles[t][x][y] = tiles[t][(y * 8) + x];
        }
        for (int t = tnum; t < target.tiles.length; t++)
        {
            target.tiles[t] = new byte[8][8];
        }
        target.arrangement = arrangement;
    }
}
