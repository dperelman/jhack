/*
 * Created on Feb 9, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.starmen.pkhack.CheckNode;
import net.starmen.pkhack.CheckRenderer;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.MaxLengthDocument;
import net.starmen.pkhack.NodeSelectionListener;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class WindowBorderEditor extends EbHackModule implements ActionListener
{
    /*
     * 200200 to 200953 = Plain windows/battle window graphics, status symbols,
     * etc. (Compressed)
     */
    // 200954 to 20099F = Flavored windows graphics (compressed)
    // 2021C8 to 202387 = Text window flavor palettes
    /*
     * palette notes [p][s][c]
     * 
     * p = which flavor (or dead) s = which palette within the flavor c = which
     * color of the palette
     * 
     * s0 = c1 - text color, c3 - text background color s1 = text cursor, battle
     * box colors -- HP/PP shadow effect colors s2 = ??? s3 = You won, smash,
     * and equip symbol colors s4 = (very bright) HP.PP/Battle box -- not
     * numbers s5 = (very dark) Black or flashing black battle box s6 = AUTO
     * button -- affliction colors s7 = (grayscale) number colors?? -- border of
     * plain text boxes
     */

    /**
     * @param rom
     * @param prefs
     */
    public WindowBorderEditor(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        try
        {
            Class[] c = new Class[]{byte[].class, WindowBorderEditor.class};
            IPSDatabase.registerExtension("wbg", WindowBorderEditor.class
                .getMethod("importData", c), WindowBorderEditor.class
                .getMethod("restoreData", c), WindowBorderEditor.class
                .getMethod("checkData", c), this);
        }
        catch (SecurityException e)
        {
            // no security model, shouldn't have to worry about this
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            // spelling mistake, maybe? ^_^;
            e.printStackTrace();
        }
    }
    public static byte[][][] graphics;
    public static Color[] tempPal = new Color[]{Color.BLACK, Color.RED,
        Color.GREEN, Color.BLUE};
    public static Color[][][] palettes;
    public static byte[] subPalNums;
    public static final int[] FLAVOR_NAME_POINTERS = new int[]{0x1F90F,
        0x1F92A, 0x1F945, 0x1F960, 0x1F97B};
    public static String[] flavorNames = new String[7];
    private static int[] flavorLens = new int[5];
    public static String[] subPalNames = new String[]{"Text",
        "Text cursor, battle box, battle numbers", "???",
        "You Won, smash, equip", "HP/PP in battle box", "darkened battle box",
        "AUTO, afflictions", "non-battle text boxes"};
    private static int rOff, wOff, oldLens[] = new int[2];

    /**
     * 
     * TODO Write javadoc for this method
     * 
     * @param pointerAddress
     * @param num
     * @param hm
     * @return Positive means success. On failure user is presented with option
     *         between abort (negative return value), retry (return value of
     *         trying this method again), fail (return value of 0),
     */
    private static int readGraphics(int pointerAddress, int num,
        EbHackModule hm, boolean readOrg)
    {
        byte[] buffer = new byte[8192];
        int[] tmp;
        Rom r = readOrg
            ? JHack.main.getOrginalRomFile(hm.rom.getRomType())
            : hm.rom;
        int address = r.readRegAsmPointer(pointerAddress);
        System.out.println("Reading from address: 0x"
            + Integer.toHexString(address) + " (" + address + ")");
        tmp = EbHackModule.decomp(address, buffer, r);
        if (tmp[0] < 0)
        {
            System.err.println("Error #" + tmp[0]
                + " decompressing window graphics.");
            Object opt = JOptionPane.showInputDialog(null, "Error " + tmp[0]
                + " decompressing the window border graphics.",
                "Decompression Error", JOptionPane.ERROR_MESSAGE, null,
                new String[]{"Abort", "Retry", "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                return tmp[0];
            }
            else if (opt.equals("Retry"))
            {
                return readGraphics(pointerAddress, num, hm, readOrg);
            }
            else if (opt.equals("Fail"))
            {
                for (int j = 0; j < num; j++)
                {
                    for (int x = 0; x < 8; x++)
                        Arrays.fill(graphics[rOff][x], (byte) 0);
                    rOff++;
                }
                System.out.println("WindowBorderEditor: fail: zeroed below "
                    + rOff);
                return 0;
            }
            return tmp[0];
        }
        System.out.println("Decompressed " + tmp[0] + " bytes ("
            + (tmp[0] / 16) + " tiles) from a " + tmp[1]
            + " byte compressed block.");
        for (int j = 0; j < num * 16; j += 16)
        {
            read2BPPArea(graphics[rOff++], buffer, j, 0, 0);
        }
        return tmp[1];
    }

    private static void readPalettes(Rom rom)
    {
        rom.seek(0x2021C8);
        palettes = new Color[7][8][4];
        for (int pal = 0; pal < 7; pal++)
            for (int s = 0; s < 8; s++)
                rom.readPaletteSeek(palettes[pal][s]);
    }

    //    public static final int[][] FLAVOR_NAME_LOC = new int[][]{{0x04c34d, 12},
    //        {0x04c35a, 11}, {0x04c366, 18}, {0x04c378, 14}, {0x04c386, 14}};

    private static void readFlavorNames(HackModule hm, boolean readOrg)
    {
        /*
         * 04c34d:12 #19 from JeffMan's list 04c35a:11 #20 from JeffMan's list
         * 04c366:18 #21 from JeffMan's list 04c378:14 #22 from JeffMan's list
         * 04c386:14 #23 from JeffMan's list
         */
        Rom r = readOrg
            ? JHack.main.getOrginalRomFile(hm.rom.getRomType())
            : hm.rom;
        for (int i = 0; i < FLAVOR_NAME_POINTERS.length; i++)
        {
            //            int offset = hm.rom.readRegAsmPointer(FLAVOR_NAME_POINTERS[i]);
            //            System.out
            //                .println("WindowBorderEditor.readFlavorNames(): offset=0x"
            //                    + Integer.toHexString(offset));
            flavorNames[i] = hm.readRegString(r
                .readRegAsmPointer(FLAVOR_NAME_POINTERS[i]));
            flavorLens[i] = flavorNames[i].length() + 1;
        }
        flavorNames[5] = "Dead";
        flavorNames[6] = "???";
    }

    public static void readSubPalNums()
    {
        if (subPalNums == null)
        {
            subPalNums = new byte[423];
            try
            {
                InputStream in = ClassLoader
                    .getSystemResourceAsStream(DEFAULT_BASE_DIR
                        + "windowGPals.dat");
                int i = 0;
                while (i < 423)
                {
                    int r = in.read();
                    r |= in.read() << 8;
                    r |= in.read() << 16;
                    for (int j = 0; j < 8; j++)
                    {
                        if (i < 423)
                            subPalNums[i++] = (byte) ((r >> (j * 3)) & 7);
                    }
                }
                in.close();
                //                boolean[] tmp = new boolean[8];
                //                for (int j = 0; j < 423; j++)
                //                {
                //                    tmp[subPalNums[j]] = true;
                //                }
                //                for (int j = 0; j < 8; j++)
                //                    System.out.println("subpal #" + j + " is "
                //                        + (tmp[j] ? "" : "un") + "used.");
            }
            catch (IOException e)
            {
                System.err
                    .println("Error reading subpalette numbers file (windowGPals.dat).");
                e.printStackTrace();
            }
        }
    }

    public static boolean readGraphics(EbHackModule hm, boolean readOrg)
    {
        graphics = new byte[423][8][8];
        rOff = 0;
        oldLens[0] = readGraphics(0x47E47, 416, hm, readOrg); // and B798
        if (oldLens[0] < 0)
            return false;
        //oldLens[0] = readGraphics(0xB798);
        oldLens[1] = readGraphics(0x47EAA, 7, hm, readOrg);
        if (oldLens[1] < 0)
            return false;
        return true;
    }

    public static boolean readFromRom(EbHackModule hm, boolean readOrg)
    {
        if (!readGraphics(hm, readOrg))
            return false;
        readPalettes(readOrg ? JHack.main
            .getOrginalRomFile(hm.rom.getRomType()) : hm.rom);
        readSubPalNums();
        readFlavorNames(hm, readOrg);

        return true;
    }

    public static boolean readFromRom(EbHackModule hm)
    {
        return readFromRom(hm, false);
    }

    private boolean readFromRom()
    {
        return readFromRom(this);
    }

    private static int writeGraphics(int[] pointerLoc, int oldLen, int num,
        EbHackModule hm)
    {
        if (oldLen < 0)
            return oldLen;
        byte[] buffer = new byte[8192]; //new byte[num * 16];
        for (int i = 0; i < num; i++)
            HackModule.write2BPPArea(graphics[wOff++], buffer, 16 * i, 0, 0);
        byte[] comp = new byte[8192];
        int tmp = comp(buffer, comp, num * 16);
        if (tmp < 0)
        {
            System.out
                .println("Error " + tmp + " compressing window graphics.");
            return tmp;
        }
        hm.writeToFreeASMLink(comp, pointerLoc, oldLen, tmp);
        System.out.println("writeGraphics(): compressed " + num
            + " tiles from " + (num * 16) + " bytes to " + tmp + " bytes.");
        return tmp;
    }

    private static void writePalettes(Rom rom)
    {
        rom.seek(0x2021C8);
        for (int pal = 0; pal < 7; pal++)
            for (int s = 0; s < 8; s++)
                rom.writePaletteSeek(palettes[pal][s]);
    }

    private static void writeFlavorName(HackModule hm, int i)
    {
        if (i >= 0 && i < 5)
        {
            byte[] str = new byte[flavorNames[i].length() + 1];
            char[] c = hm.simpToGameString(flavorNames[i].toCharArray());
            for (int j = 0; j < c.length; j++)
                str[j] = (byte) c[j];
            str[str.length - 1] = 0;
            hm.writeToFreeASMLink(str, FLAVOR_NAME_POINTERS[i], flavorLens[i],
                str.length);
        }
    }

    private static void writeFlavorNames(HackModule hm)
    {
        for (int i = 0; i < FLAVOR_NAME_POINTERS.length; i++)
        {
            writeFlavorName(hm, i);
            //            byte[] str = new byte[flavorNames[i].length() + 1];
            //            char[] c = hm.simpToGameString(flavorNames[i].toCharArray());
            //            for (int j = 0; j < c.length; j++)
            //                str[j] = (byte) c[j];
            //            str[str.length - 1] = 0;
            //            hm.writeToFreeASMLink(str, FLAVOR_NAME_POINTERS[i],
            // flavorLens[i],
            //                str.length);
            //            // hm.writeRegString(FLAVOR_NAME_LOC[i][0],
            // FLAVOR_NAME_LOC[i][1],
            //            // flavorNames[i]);
        }
    }

    private static void writeSubPalNums()
    {
        try
        {
            OutputStream out = new FileOutputStream("windowGPals.dat");
            int i = 0;
            while (i < 423)
            {
                int w = 0;
                for (int j = 0; j < 8; j++)
                {
                    if (i < 423)
                        w |= (subPalNums[i++] & 7) << (j * 3);
                }
                out.write(w);
                out.write(w >> 8);
                out.write(w >> 16);
            }
            out.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void writeGraphics(EbHackModule hm)
    {
        wOff = 0;
        oldLens[0] = writeGraphics(new int[]{0x47E47, 0xB798}, oldLens[0], 416,
            hm);
        oldLens[1] = writeGraphics(new int[]{0x47EAA}, oldLens[1], 7, hm);
    }

    public static void writeInfo(EbHackModule hm)
    {
        writeGraphics(hm);
        writePalettes(hm.rom);
        writeFlavorNames(hm);
        //writeSubPalNums();
    }

    private void drawTile(int i, Graphics g, int subpal, boolean zeroBlack,
        boolean hFlip, boolean vFlip, int x, int y, int zoom)
    {
        if (getCurrentFlavor() == -1)
            return;
        byte[][] img = graphics[i];
        //        if (hFlip)
        //        {
        //            byte[][] tmp = new byte[img.length][img[0].length];
        //            for (int ax = 0; ax < tmp.length; ax++)
        //                for (int ay = 0; ay < tmp[0].length; ay++)
        //                    tmp[ax][ay] = img[img.length - ax - 1][ay];
        //            img = tmp;
        //        }
        //        if (vFlip)
        //        {
        //            byte[][] tmp = new byte[img.length][img[0].length];
        //            for (int ax = 0; ax < tmp.length; ax++)
        //                for (int ay = 0; ay < tmp[0].length; ay++)
        //                    tmp[ax][ay] = img[ax][img.length - ay - 1];
        //            img = tmp;
        //        }
        Color[] pal = new Color[4];
        System.arraycopy(palettes[getCurrentFlavor()][subpal], 0, pal, 0, 4);
        if (zeroBlack)
        {
            pal[0] = Color.BLACK;
        }
        g.drawImage(HackModule.drawImage(img, pal, hFlip, vFlip), x, y,
            8 * zoom, 8 * zoom, null);
    }

    private void drawTile(int i, Graphics g, int subpal, int x, int y, int zoom)
    {
        drawTile(i, g, subpal, false, false, false, x, y, zoom);
    }

    private void drawTile(int i, Graphics g, int x, int y, int zoom)
    {
        drawTile(i, g, subPalNums[i], x, y, zoom);
    }

    private class TileSelector extends DoubleSelTileSelector
    {
        public int getSubPalNum(int tile)
        {
            return subPalNums[tile];
        }

        public String getDoubleSelPrefName()
        {
            return "eb_window_border_editor.allow_2x_sel";
        }

        public int getTilesWide()
        {
            return 16;
        }

        public int getTilesHigh()
        {
            return 32;
        }

        public int getTileSize()
        {
            return 8;
        }

        public int getZoom()
        {
            return 2;
        }

        protected boolean isDrawGridLines()
        {
            return false;
        }

        public int getTileCount()
        {
            return graphics.length; //TODO better way?
        }

        public Image getTileImage(int tile)
        {
            return HackModule.drawImage(graphics[tile],
                palettes[getCurrentFlavor()][subPalNums[tile]]);
        }

        protected boolean isGuiInited()
        {
            return true;
        }

    }

    private class WindowGraphicPrevArrangementViewer extends ArrangementEditor
    {
        private boolean arrInited = false;
        private int[][] arrangement = new int[getTilesWide()][getTilesHigh()];

        private void initArr()
        {
            if (arrInited)
                return;
            try
            {
                DataInputStream in = new DataInputStream((ClassLoader
                    .getSystemResourceAsStream(DEFAULT_BASE_DIR
                        + "windowPrevArr.dat")));
                for (int x = 0; x < arrangement.length; x++)
                    for (int y = 0; y < arrangement[0].length; y++)
                        arrangement[x][y] = ((int) in.readShort()) & 0xffff;
                arrInited = true;
                in.close();
            }
            catch (IOException e)
            {
                System.out.println("Error reading window border editor "
                    + "preview arrangement file (windowPrevArr.dat).");
                e.printStackTrace();
            }
        }

        protected int getCurrentTile()
        {
            return WindowBorderEditor.this.getCurrentTile();
        }

        protected void setCurrentTile(int tile)
        {
            tileSelector.setCurrentTile(tile);
        }

        protected int getTilesWide()
        {
            return 32;
        }

        protected int getTilesHigh()
        {
            return 28;
        }

        protected int getTileSize()
        {
            return 8;
        }

        protected int getZoom()
        {
            return 1;
        }

        protected boolean isDrawGridLines()
        {
            return false;
        }

        protected boolean isEditable()
        {
            return false;
        }

        protected boolean isGuiInited()
        {
            return true;
        }

        protected int getCurrentSubPalette()
        {
            return WindowBorderEditor.this.getCurrentSubPal();
        }

        protected int getArrangementData(int x, int y)
        {
            initArr();
            return arrangement[x][y];
        }

        protected int[][] getArrangementData()
        {
            initArr();
            int[][] a = new int[getTilesWide()][getTilesHigh()];
            for (int i = 0; i < a.length; i++)
                for (int j = 0; j < a[0].length; j++)
                    a[i][j] = getArrangementData(i, j);
            return a;
        }

        protected void setArrangementData(int x, int y, int data)
        {
            arrangement[x][y] = data;
        }

        protected void setArrangementData(int[][] data)
        {
            for (int i = 0; i < data.length; i++)
                for (int j = 0; j < data[0].length; j++)
                    arrangement[i][j] = data[i][j];
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.starmen.pkhack.eb.ArrangementEditor#getTileImage(int, int,
         *      boolean, boolean)
         */
        protected Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            if (getCurrentFlavor() > 0 && tile > 15 && tile < 23)
                tile += 400;
            Image img = this.createImage(8, 8);
            WindowBorderEditor.this.drawTile(tile, img.getGraphics(), subPal,
                true, hFlip, vFlip, 0, 0, 1);
            return img;
        }

        public void repaintCurrentTile()
        {
            repaintTile(getCurrentTile());
            if (tileSelector.isDoubleSel())
                repaintTile(getCurrentTile() + tileSelector.getTilesWide());
        }

        //        private void draw(int x, int y, Graphics g)
        //        {
        //            int a = arrangement[x][y], arr = a & 0x1ff;
        //            if (getCurrentFlavor() > 0 && arr > 15 && arr < 23) arr += 400;
        //            drawTile(arr, g, (a >> 10) & 7, true, (a & 0x4000) != 0,
        //                (a & 0x8000) != 0, x * 8 * getZoom(), y * 8 * getZoom(),
        //                getZoom());
        //        }
        //
        //        public void draw(int x, int y)
        //        {
        //            draw(x, y, this.getGraphics());
        //        }
        //
        //        public void draw(int i)
        //        {
        //            for (int x = 0; x < arrangement.length; x++)
        //                for (int y = 0; y < arrangement[0].length; y++)
        //                {
        //                    int arr = arrangement[x][y] & 0x1ff;
        //                    if (arr
        //                        + (getCurrentFlavor() > 0 && arr > 15 && arr < 23
        //                            ? 400
        //                            : 0) == i) draw(x, y);
        //                }
        //        }
        //
        //        public void repaintCurrent()
        //        {
        //            draw(getCurrentTile());
        //            if (tileSelector.isDoubleSel())
        //                draw(getCurrentTile() + TileSelector.TILES_WIDE);
        //        }
        //
        //        protected Image getArrangementImage(int[][] selection)
        //        {
        //            initArr();
        //            
        //            BufferedImage out = new BufferedImage(
        //                (isDrawGridLines() ? arrangement.length - 1 : 0)
        //                    + (int) (getTileSize() * arrangement.length * getZoom()),
        //                (isDrawGridLines() ? arrangement[0].length - 1 : 0)
        //                    + (int) (getTileSize() * arrangement[0].length * getZoom()),
        //                BufferedImage.TYPE_4BYTE_ABGR_PRE);
        //            Graphics g = out.getGraphics();
        //            for (int x = 0; x < arrangement.length; x++)
        //            {
        //                for (int y = 0; y < arrangement[0].length; y++)
        //                {
        //                    draw(x, y, g);
        //
        //                    if (selection[x][y] != -1)
        //                    {
        //                        g.setColor(new Color(255, 255, 0, 128));
        //                        g.fillRect((int) (x * 8 * getZoom())
        //                            + (isDrawGridLines() ? x : 0),
        //                            (int) (y * 8 * getZoom())
        //                                + (isDrawGridLines() ? y : 0),
        //                            (int) (8 * getZoom()), (int) (8 * getZoom()));
        //                    }
        //                }
        //            }
        //            return out;
        //        }
    }
    //    private class ArrangementEditor extends AbstractButton implements
    //        MouseListener, MouseMotionListener
    //    {
    //        private static final int WIDTH = 32, HEIGHT = 28; //size of EB screen for
    // 8x8's
    //        private static final int TILE_SIZE = 8;
    //        private static final int ZOOM = 1;
    //        private static final boolean EDITABLE = false;
    //
    //        private boolean drawGridLines = false;
    //        private int[][] arrangement = new int[WIDTH][HEIGHT];
    //
    //        public boolean drawGridLines()
    //        {
    //            return drawGridLines;
    //        }
    //
    //        public void setDrawGridLines(boolean in)
    //        {
    //            drawGridLines = in;
    //        }
    //
    //        private int makeArrangementNumber(int tile, int subPalette,
    //            boolean hFlip, boolean vFlip)
    //        {
    //            return (tile & 0x01ff) | (((subPalette) & 7) << 10)
    //                | (hFlip ? 0x4000 : 0) | (vFlip ? 0x8000 : 0);
    //        }
    //
    //        private void leftClickAction(int x, int y)
    //        {
    //            if (EDITABLE)
    //            {
    //                //put current tile with current subPalette with no flip at clicked
    //                // on location
    //                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT)
    //                    arrangement[x][y] = makeArrangementNumber(getCurrentTile(),
    //                        getCurrentSubPal(), false, false);
    //                if (tileSelector.isDoubleSel() && x >= 0 && x < WIDTH
    //                    && y + 1 >= 0 && y + 1 < HEIGHT)
    //                    arrangement[x][y + 1] = makeArrangementNumber(
    //                        getCurrentTile() + TileSelector.TILES_WIDE,
    //                        getCurrentSubPal(), false, false);
    //                this.repaint();
    //            }
    //            else
    //            {
    //                leftShiftClickAction(x, y);
    //            }
    //        }
    //
    //        private void rightClickAction(int x, int y)
    //        {
    //            if (EDITABLE)
    //            {
    //                //add one to flip of current tile (sorta rotation)
    //                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT)
    //                {
    //                    arrangement[x][y] += 0x4000;
    //                    arrangement[x][y] &= 0xffff;
    //                }
    //                this.repaint();
    //            }
    //            else
    //            {
    //                leftShiftClickAction(x, y);
    //            }
    //        }
    //
    //        private void leftShiftClickAction(int x, int y)
    //        {
    //            //set tile editor to current tile
    //            if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT)
    //            {
    //                tileSelector.setCurrentTile(arrangement[x][y] & 0x1ff);
    //                palSelector.setSelectedIndex((arrangement[x][y] >> 10) & 7);
    //            }
    //        }
    //
    //        public void mouseClicked(MouseEvent me)
    //        {
    //            if ((me.getModifiers() & MouseEvent.ALT_MASK) != 0) return;
    //
    //            int x = me.getX() / (TILE_SIZE * ZOOM + (drawGridLines() ? 1 : 0));
    //            int y = me.getY() / (TILE_SIZE * ZOOM + (drawGridLines() ? 1 : 0));
    //            if (me.getButton() == MouseEvent.BUTTON1)
    //            {
    //                if ((me.getModifiers() & MouseEvent.SHIFT_MASK) != 0)
    //                {
    //                    leftShiftClickAction(x, y);
    //                }
    //                else if (!((me.getModifiers() & MouseEvent.CTRL_MASK) != 0))
    //                {
    //                    leftClickAction(x, y);
    //                }
    //            }
    //            else if (me.getButton() == MouseEvent.BUTTON3)
    //            {
    //                rightClickAction(x, y);
    //            }
    //            this.fireActionPerformed(new ActionEvent(this,
    //                ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
    //        }
    //
    //        public void mousePressed(MouseEvent me)
    //        {}
    //
    //        public void mouseReleased(MouseEvent me)
    //        {}
    //
    //        public void mouseEntered(MouseEvent me)
    //        {}
    //
    //        public void mouseExited(MouseEvent me)
    //        {}
    //
    //        /*
    //         * (non-Javadoc)
    //         *
    //         * @see
    // java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
    //         */
    //        public void mouseDragged(MouseEvent me)
    //        {
    //            int x = me.getX() / (TILE_SIZE * ZOOM + (drawGridLines() ? 1 : 0));
    //            int y = me.getY() / (TILE_SIZE * ZOOM + (drawGridLines() ? 1 : 0));
    //            if ((me.getModifiers() & MouseEvent.SHIFT_MASK) != 0)
    //            {
    //                leftShiftClickAction(x, y);
    //            }
    //            else
    //            {
    //                leftClickAction(x, y);
    //            }
    //        }
    //
    //        public void mouseMoved(MouseEvent me)
    //        {}
    //
    //        private void draw(int x, int y, Graphics g)
    //        {
    //            int a = arrangement[x][y], arr = a & 0x1ff;
    //            if (getCurrentFlavor() > 0 && arr > 15 && arr < 23) arr += 400;
    //            drawTile(arr, g, (a >> 10) & 7, true, (a & 0x4000) != 0,
    //                (a & 0x8000) != 0, x * 8 * ZOOM, y * 8 * ZOOM, ZOOM);
    //        }
    //
    //        public void draw(int x, int y)
    //        {
    //            draw(x, y, this.getGraphics());
    //        }
    //
    //        public void draw(int i)
    //        {
    //            for (int x = 0; x < arrangement.length; x++)
    //                for (int y = 0; y < arrangement[0].length; y++)
    //                {
    //                    int arr = arrangement[x][y] & 0x1ff;
    //                    if (arr
    //                        + (getCurrentFlavor() > 0 && arr > 15 && arr < 23
    //                            ? 400
    //                            : 0) == i) draw(x, y);
    //                }
    //        }
    //
    //        public void repaintCurrent()
    //        {
    //            draw(getCurrentTile());
    //            if (tileSelector.isDoubleSel())
    //                draw(getCurrentTile() + TileSelector.TILES_WIDE);
    //        }
    //
    //        public void paint(Graphics g)
    //        {
    //            for (int x = 0; x < arrangement.length; x++)
    //                for (int y = 0; y < arrangement[0].length; y++)
    //                    draw(x, y, g);
    //        }
    //
    //        private String actionCommand = new String();
    //
    //        public String getActionCommand()
    //        {
    //            return this.actionCommand;
    //        }
    //
    //        public void setActionCommand(String arg0)
    //        {
    //            this.actionCommand = arg0;
    //        }
    //
    //        public void writeInfo()
    //        {
    //            try
    //            {
    //                DataOutputStream out = new DataOutputStream(
    //                    new FileOutputStream("src/" + DEFAULT_BASE_DIR
    //                        + "windowPrevArr.dat"));
    //                System.out.println("Writing to " + "src/" + DEFAULT_BASE_DIR
    //                    + "windowPrevArr.dat");
    //                for (int x = 0; x < arrangement.length; x++)
    //                    for (int y = 0; y < arrangement[0].length; y++)
    //                        out.writeShort(arrangement[x][y]);
    //                out.close();
    //            }
    //            catch (IOException e)
    //            {
    //                System.out.println("Error writing window border editor "
    //                    + "preview arrangement file (windowPrevArr.dat).");
    //                e.printStackTrace();
    //            }
    //        }
    //
    //        public ArrangementEditor()
    //        {
    //            try
    //            {
    //                DataInputStream in = new DataInputStream((ClassLoader
    //                    .getSystemResourceAsStream(DEFAULT_BASE_DIR
    //                        + "windowPrevArr.dat")));
    //                for (int x = 0; x < arrangement.length; x++)
    //                    for (int y = 0; y < arrangement[0].length; y++)
    //                        arrangement[x][y] = ((int) in.readShort()) & 0xffff;
    //                in.close();
    //            }
    //            catch (IOException e)
    //            {
    //                System.out.println("Error reading window border editor "
    //                    + "preview arrangement file (windowPrevArr.dat).");
    //                e.printStackTrace();
    //            }
    //
    //            this.setPreferredSize(new Dimension(ZOOM * WIDTH * 8, ZOOM * HEIGHT
    //                * 8));
    //            this.addMouseListener(this);
    //            this.addMouseMotionListener(this);
    //        }
    //    }
    private TileSelector tileSelector;
    private JComboBox flavorSelector, palSelector;
    private JTextField flavorNameEdit;
    private MaxLengthDocument flavorNameDoc;
    private JButton editName;
    private SpritePalette pal;
    private DrawingToolset toolset;
    private IntArrDrawingArea da;
    private WindowGraphicPrevArrangementViewer arre;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());

        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');
        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'y', "ctrl S",
            "apply", this));
        fileMenu.addSeparator();
        fileMenu.add(HackModule.createJMenuItem("Import...", 'i', null,
            "import", this));
        fileMenu.add(HackModule.createJMenuItem("Export...", 'e', null,
            "export", this));
        mb.add(fileMenu);

        mb.add(createEditMenu(this, true));

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Allow double select", JHack.main
            .getPrefs(), "eb_window_border_editor.allow_2x_sel", true, 'a',
            "alt D", "2xSelect", this));
        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        JPanel center = new JPanel(new BorderLayout());

        tileSelector = new TileSelector();
        tileSelector.setActionCommand("tileSelector");
        tileSelector.addActionListener(this);
        center.add(tileSelector, BorderLayout.WEST);

        JPanel edit = new JPanel(new BorderLayout());
        Box editTop = new Box(BoxLayout.Y_AXIS);

        flavorSelector = new JComboBox();
        updateFlavorSelector();
        flavorSelector.setSelectedIndex(0);
        flavorSelector.setActionCommand("flavorSelector");
        flavorSelector.addActionListener(this);
        editTop.add(flavorSelector);

        flavorNameEdit = new JTextField();
        editTop.add(flavorNameEdit);

        editName = new JButton("Change Flavor Name");
        editName.setActionCommand("changeFlavorName");
        editName.addActionListener(this);
        editTop.add(editName);

        edit.add(editTop, BorderLayout.NORTH);

        Box editBottom = new Box(BoxLayout.Y_AXIS);

        pal = new SpritePalette(4, true);
        pal.setPalette(tempPal);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);

        palSelector = HackModule.createJComboBoxFromArray(subPalNames);
        palSelector.setSelectedIndex(0);
        palSelector.setActionCommand("palSelector");
        palSelector.addActionListener(this);

        toolset = new DrawingToolset(this);

        da = new IntArrDrawingArea(toolset, pal, this);
        da.setZoom(10);
        da.setPreferredSize(new Dimension(80, 160));
        da.setActionCommand("drawingArea");
        da.setImage(graphics[getCurrentTile()]);

        arre = new WindowGraphicPrevArrangementViewer();

        editBottom.add(createFlowLayout(da));
        editBottom.add(Box.createVerticalStrut(5));
        editBottom.add(palSelector);
        editBottom.add(Box.createVerticalStrut(3));
        editBottom.add(createFlowLayout(pal));
        editBottom.add(Box.createVerticalStrut(10));
        editBottom.add(createFlowLayout(arre));

        edit.add(editBottom, BorderLayout.SOUTH);

        center.add(edit, BorderLayout.CENTER);
        center.add(toolset, BorderLayout.EAST);

        mainWindow.getContentPane().add(center, BorderLayout.CENTER);

        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.4";
    }

    public String getDescription()
    {
        return "Window Border Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public void show()
    {
        if (readFromRom())
        {
            super.show();
            updateFlavorSelector();
            mainWindow.setVisible(true);
        }
    }

    private boolean doubleSelInit = false;

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("tileSelector"))
        {
            if (tileSelector.isDoubleSel())
            {
                byte[][] gr = new byte[8][16];
                for (int x = 0; x < 8; x++)
                {
                    for (int y = 0; y < 8; y++)
                    {
                        gr[x][y] = graphics[getCurrentTile()][x][y];
                        gr[x][y + 8] = graphics[getCurrentTile() + 16][x][y];
                    }
                }
                da.setImage(gr);
                if (!doubleSelInit)
                {
                    da.getParent().doLayout();
                    mainWindow.pack();
                    doubleSelInit = true;
                }
            }
            else
            {
                da.setImage(graphics[getCurrentTile()]);
            }

            palSelector.setSelectedIndex(subPalNums[getCurrentTile()]);
            updatePalette();
        }
        else if (ae.getActionCommand().equals("flavorSelector"))
        {
            if (getCurrentFlavor() == -1)
                return;
            updatePalette();
            da.repaint();
            tileSelector.repaint();
            arre.repaint();
            if (getCurrentFlavor() < FLAVOR_NAME_POINTERS.length)
            {
                flavorNameEdit.setEnabled(true);
                editName.setEnabled(true);
                flavorNameEdit.setText(flavorNames[getCurrentFlavor()]);
            }
            else
            {
                flavorNameEdit.setEnabled(false);
                flavorNameEdit.setText(flavorSelector.getSelectedItem()
                    .toString().split("\\]")[1].trim());
                editName.setEnabled(false);
            }
        }
        else if (ae.getActionCommand().equals("palSelector"))
        {
            palSelector.repaint();
            updatePalette();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            getCurrentPal()[pal.getSelectedColorIndex()] = pal.getNewColor();
            updatePalette();
            da.repaint();
            tileSelector.repaint();
            arre.repaint();
        }
        else if (ae.getActionCommand().equals("drawingArea"))
        {
            byte[][] gr = da.getByteArrImage();
            if (tileSelector.isDoubleSel())
            {
                for (int x = 0; x < 8; x++)
                {
                    for (int y = 0; y < 8; y++)
                    {
                        graphics[getCurrentTile()][x][y] = gr[x][y];
                        graphics[getCurrentTile() + 16][x][y] = gr[x][y + 8];
                    }
                }
            }
            else
            {
                graphics[getCurrentTile()] = da.getByteArrImage();
            }
            tileSelector.repaintCurrent();
            arre.repaintCurrentTile();
        }
        else if (ae.getActionCommand().equals("2xSelect"))
        {
            //            System.out.println("2xSelect now set to "
            //                + Boolean.toString(JHack.main.getPrefs().getValueAsBoolean(
            //                    "eb_window_border_editor.allow_2x_sel")) + ".");
            tileSelector.setCurrentTile(getCurrentTile(), true);
        }
        //edit menu
        else if (ae.getActionCommand().equals("undo"))
        {
            da.undo();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("cut"))
        {
            da.cut();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            da.copy();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            da.paste();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            da.delete();
            da.repaint();
        }
        //flipping
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
        else if (ae.getActionCommand().equals("changeFlavorName"))
        {
            //put edited name in names array
            flavorNames[getCurrentFlavor()] = flavorNameEdit.getText();
            //reload flavor selector with updated names array
            updateFlavorSelector();
            //write modified name
            writeFlavorName(this, getCurrentFlavor());
            //            writeRegString(FLAVOR_NAME_LOC[getCurrentFlavor()][0],
            //                FLAVOR_NAME_LOC[getCurrentFlavor()][1],
            //                flavorNames[getCurrentFlavor()]);
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            writeInfo(this);
            //            arre.writeInfo();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("export"))
        {
            exportData();
        }
        else if (ae.getActionCommand().equals("import"))
        {
            importData();
        }
    }

    private void updateFlavorSelector()
    {
        int tmp = flavorSelector.getSelectedIndex();
        flavorSelector.removeAllItems();
        for (int i = 0; i < flavorNames.length; i++)
            flavorSelector.addItem(HackModule.getNumberedString(flavorNames[i],
                i));
        if (tmp != -1)
            flavorSelector.setSelectedIndex(tmp);
    }

    private void updatePalette()
    {
        pal.setPalette(palettes[getCurrentFlavor()][palSelector
            .getSelectedIndex()]);
        pal.repaint();
    }

    private int getCurrentTile()
    {
        return tileSelector.getCurrentTile();
    }

    private int getCurrentFlavor()
    {
        return flavorSelector.getSelectedIndex();
    }

    private int getCurrentSubPal()
    {
        return palSelector.getSelectedIndex();
    }

    private Color[] getPal(int i)
    {
        return palettes[getCurrentFlavor()][subPalNums[i]];
    }

    private Color[] getCurrentPal()
    {
        return palettes[getCurrentFlavor()][getCurrentSubPal()];
    }

    public static final int NODE_TILES = 0;

    public static class WindowBorderImportData
    {
        public byte[][][] tiles;
        public Color[][][] palettes = new Color[8][][];
    }

    public static final byte WBG_VERSION = 1;

    public static void exportData(File file, boolean[] a)
    {
        //make a byte whichMaps. for each map if it is used set the bit at the
        // place equal to the map number to 1
        int whichFlavors = 0;
        for (int i = 0; i < a.length; i++)
            whichFlavors |= (a[i] ? 1 : 0) << i;

        try
        {
            FileOutputStream out = new FileOutputStream(file);

            out.write(WBG_VERSION);
            out.write(whichFlavors);
            for (int f = 0; f < a.length; f++)
            {
                //write flavor/tiles?
                if (a[f])
                {
                    //if is tiles...
                    if (f == 0)
                    {
                        byte[] b = new byte[graphics.length * 32];
                        int offset = 0;
                        for (int i = 0; i < graphics.length; i++)
                            offset += write2BPPArea(graphics[i], b, offset, 0,
                                0);
                        out.write(b);
                    }
                    //... else is flavors/palettes
                    else
                    {
                        byte[] pal = new byte[palettes[f - 1].length * 4 * 2];
                        for (int i = 0; i < palettes[f - 1].length; i++)
                            writePalette(pal, i * 4 * 2, palettes[f - 1][i]);
                        out.write(pal);
                    }
                }
            }

            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error exporting window border data to "
                    + file.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error exporting window border data to "
                + file.getAbsolutePath() + ".");
            e.printStackTrace();
        }
    }

    public static WindowBorderImportData importData(InputStream in)
        throws IOException
    {
        WindowBorderImportData out = new WindowBorderImportData();

        //FileInputStream in = new FileInputStream(f);

        byte version = (byte) in.read();
        if (version > WBG_VERSION)
        {
            if (JOptionPane.showConfirmDialog(null,
                "WBG file version not supported." + "Try to load anyway?",
                "WBG Version " + version + " Not Supported",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
                return null;
        }
        byte whichMaps = (byte) in.read();
        for (int m = 0; m < 8; m++)
        {
            //if bit for this flavor/tiles set...
            if (((whichMaps >> m) & 1) != 0)
            {
                //if tiles...
                if (m == 0)
                {
                    byte[] b = new byte[graphics.length * 32];
                    in.read(b);

                    int offset = 0;
                    out.tiles = new byte[graphics.length][8][8];
                    for (int i = 0; i < graphics.length; i++)
                        offset += read2BPPArea(out.tiles[i], b, offset, 0, 0);
                }
                //... else flavor (palettes)
                else
                {
                    byte[] pal = new byte[8 * 4 * 2];
                    in.read(pal);

                    out.palettes[m - 1] = new Color[8][4];
                    for (int i = 0; i < out.palettes[m - 1].length; i++)
                        readPalette(pal, i * 4 * 2, out.palettes[m - 1][i]);
                }
            }
        }

        in.close();

        return out;
    }

    public static WindowBorderImportData importData(File f)
    {
        try
        {
            return importData(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error importing window border data from "
                    + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error importing window border data from "
                + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        return null;
    }

    public static WindowBorderImportData importData(byte[] b)
    {
        try
        {
            return importData(new ByteArrayInputStream(b));
        }
        catch (IOException e)
        {
            System.err.println("IO error importing window border data "
                + "from byte array.");
            e.printStackTrace();
        }
        return null;
    }

    private void exportData()
    {
        boolean[] a = showChecklist(null, "<html>"
            + "Select which items you wish to export." + "</html>",
            "Export What?");
        if (a == null)
            return;

        File f = getFile(true, "wbg", "Window Border Graphics & palettes");
        if (f != null)
            exportData(f, a);
    }

    private void importData()
    {
        File f = getFile(false, "wbg", "Window Border Graphics & palettes");
        WindowBorderImportData wbid;
        if (f == null || (wbid = importData(f)) == null)
            return;
        importData(wbid);
    }

    private static boolean[] showChecklist(boolean[] in, String text,
        String title)
    {
        CheckNode topNode = new CheckNode("Window Border Graphics & Palettes",
            true, true);
        topNode.setSelectionMode(CheckNode.DIG_IN_SELECTION);
        CheckNode[] flavorNodes = new CheckNode[8];
        for (int i = 0; i < 8; i++)
        {
            if (in == null || in[i])
            {
                flavorNodes[i] = new CheckNode(i == 0
                    ? "Graphics"
                    : flavorNames[i - 1], true, true);
                topNode.add(flavorNodes[i]);
            }
        }
        JTree checkTree = new JTree(topNode);
        checkTree.setCellRenderer(new CheckRenderer());
        checkTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);
        checkTree.putClientProperty("JTree.lineStyle", "Angled");
        checkTree.addMouseListener(new NodeSelectionListener(checkTree));

        //if user clicked cancel, don't take action
        if (JOptionPane.showConfirmDialog(null, pairComponents(
            new JLabel(text), new JScrollPane(checkTree), false), title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.CANCEL_OPTION)
            return null;

        final boolean[] a = new boolean[8];
        for (int m = 0; m < a.length; m++)
            a[m] = flavorNodes[m] == null ? false : flavorNodes[m].isSelected();

        return a;
    }

    private boolean importData(WindowBorderImportData wbid)
    {
        boolean[] in = new boolean[8];
        Arrays.fill(in, false);
        for (int i = 0; i < 8; i++)
        {
            if (i == 0)
            {
                if (wbid.tiles != null)
                    in[i] = true;
            }
            else if (wbid.palettes[i - 1] != null)
            {
                in[i] = true;
            }
        }
        final boolean[] a = showChecklist(in, "<html>"
            + "Select which items you wish to<br>"
            + "import. You will have a chance<br>"
            + "to select which screen you want to<br>"
            + "actually put the imported data." + "</html>", "Import What?");
        if (a == null)
            return false;

        Box targetMap = new Box(BoxLayout.Y_AXIS);
        final JComboBox[] targets = new JComboBox[flavorNames.length];
        boolean showDialog = false;
        for (int m = 0; m < targets.length; m++)
        {
            if (a[m + 1])
            {
                showDialog = true;
                targets[m] = createComboBox(flavorNames);
                targets[m].setSelectedIndex(m);
                targetMap.add(getLabeledComponent(flavorNames[m], targets[m]));

            }
        }

        if (showDialog)
        {
            final JDialog targetDialog = new JDialog(mainWindow,
                "Select Import Targets", true);
            targetDialog.getContentPane().setLayout(new BorderLayout());
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    targetDialog.setTitle("Canceled");
                    targetDialog.setVisible(false);
                }
            });
            JButton ok = new JButton("OK");
            ok.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    //t = array of used targets
                    boolean[] t = new boolean[flavorNames.length];
                    //m = source screen
                    for (int m = 0; m < flavorNames.length; m++)
                    {
                        if (targets[m] != null)
                        {
                            //n = target map
                            int n = targets[m].getSelectedIndex();
                            if (a[m])
                            {
                                //if part already used...
                                if (t[n])
                                {
                                    //fail
                                    JOptionPane.showMessageDialog(targetDialog,
                                        "Imported data must not overlap,\n"
                                            + "check your targets.",
                                        "Invalid Selection Error",
                                        JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                else
                                {
                                    //set target part as used
                                    t[n] = true;
                                }
                            }
                        }
                    }
                    targetDialog.setVisible(false);
                }
            });
            targetDialog.getContentPane().add(
                createFlowLayout(new Component[]{ok, cancel}),
                BorderLayout.SOUTH);
            targetDialog.getContentPane().add(
                pairComponents(new JLabel("<html>"
                    + "Select which map you would like<br>"
                    + "the data to be imported into.<br>"
                    + "For example, if you wish to import<br>" + "the "
                    + flavorNames[0] + " flavor into the<br>" + flavorNames[1]
                    + " flavor, then change the pull-down menu<br>"
                    + "labeled " + flavorNames[0] + " to " + flavorNames[1]
                    + ". If you do not<br>"
                    + "wish to make any changes, just click ok." + "</html>"),
                    targetMap, false), BorderLayout.CENTER);
            targetDialog.pack();

            targetDialog.setVisible(true);
            if (targetDialog.getTitle().equals("Canceled"))
                return false;
        }

        for (int m = 0; m < a.length; m++)
        {
            if (a[m])
            {
                if (m == 0)
                {
                    graphics = wbid.tiles;
                }
                else
                {
                    palettes[targets[m - 1].getSelectedIndex()] = wbid.palettes[m - 1];
                }
            }
        }
        if (mainWindow != null)
            mainWindow.repaint();
        return true;
    }

    /**
     * Imports data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "applying" files with .wbg
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param wbe instance of <code>WindowBorderEditor</code> to call
     *            <code>importData()</code> on
     */
    public static boolean importData(byte[] b, WindowBorderEditor wbe)
    {
        boolean out = wbe.importData(importData(b));
        if (out)
        {
            writeInfo(wbe);
            if (wbe.mainWindow != null)
                wbe.mainWindow.repaint();
        }
        return out;
    }

    private static boolean checkPal(Color[][] pal, int p)
    {
        for (int s = 0; s < pal.length; s++)
            for (int c = 0; c < pal[s].length; c++)
                if (!pal[s][c].equals(palettes[p][s][c]))
                    return false;
        return true;
    }

    private static boolean checkPal(Color[][] pal)
    {
        //could be applied to any palette, check all
        for (int p = 0; p < palettes.length; p++)
            if (checkPal(pal, p))
                return true;
        return false;
    }

    /**
     * Checks if data from the given <code>byte[]</code> has been imported.
     * This method exists to be called by <code>IPSDatabase</code> for
     * "checking" files with .wbg extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param wbe instance of <code>WindowBorderEditor</code> to call
     *            <code>importData()</code> on
     */
    public static boolean checkData(byte[] b, WindowBorderEditor wbe)
    {
        WindowBorderImportData wbid = importData(b);

        //check graphics if included
        if (wbid.tiles != null)
        {
            for (int t = 0; t < wbid.tiles.length; t++)
                for (int x = 0; x < wbid.tiles[t].length; x++)
                    if (!Arrays.equals(wbid.tiles[t][x], graphics[t][x]))
                        return false;
        }
        //check palettes
        for (int p = 0; p < wbid.palettes.length; p++)
            if (wbid.palettes[p] != null) //make sure pal is there
                if (!checkPal(wbid.palettes[p]))
                    return false;

        //didn't find anything missing
        return true;
    }

    /**
     * Restore data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "unapplying" files with .wbg
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param wbe instance of <code>WindowBorderEditor</code> to call
     *            <code>importData()</code> on
     */
    public static void restoreData(byte[] b, WindowBorderEditor wbe)
    {
        boolean[] a = showChecklist(null, "<html>Select which items you wish"
            + "to restore to the orginal EarthBound verions.</html>",
            "Restore what?");
        if (a[0])
        {
            readGraphics(wbe, true);
            writeGraphics(wbe);
        }
        Rom orgRom = JHack.main.getOrginalRomFile(wbe.rom.getRomType());
        //restore palettes
        //each of 7 palettes has 8 subpals of 4 colors each
        //2 bytes/color = 8*4*2 = 64 bytes/palette
        //reset areas of selected palettes
        for (int i = 1; i < a.length; i++)
            if (a[i])
                wbe.rom.resetArea(0x2021C8 + (i - 1) * 64, 64, orgRom);
        //reread palettes
        readPalettes(wbe.rom);

        if (wbe.mainWindow != null)
            wbe.mainWindow.repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        super.reset();
        readFromRom();
    }
}