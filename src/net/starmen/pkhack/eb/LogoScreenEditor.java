/*
 * Created on Mar 20, 2004
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.swing.tree.TreeSelectionModel;

import net.starmen.pkhack.CheckNode;
import net.starmen.pkhack.CheckRenderer;
import net.starmen.pkhack.CopyAndPaster;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.NodeSelectionListener;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.Undoable;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class LogoScreenEditor extends EbHackModule implements ActionListener
{
    public static final int NUM_LOGO_SCREENS = 3;

    public LogoScreenEditor(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        try
        {
            Class[] c = new Class[]{byte[].class, LogoScreenEditor.class};
            IPSDatabase.registerExtension("lscn", LogoScreenEditor.class
                .getMethod("importData", c), LogoScreenEditor.class.getMethod(
                "restoreData", c), LogoScreenEditor.class.getMethod(
                "checkData", c), this);
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

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Logo Screen Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB/n42";
    }

    public static class LogoScreen
    {
        private EbHackModule hm;
        private boolean isInited = false;
        private int num, tilePointer, tileLen, palPointer, palLen, arngPointer,
                arngLen;

        /** Number of palettes. */
        public static final int NUM_PALETTES = 5;
        /**
         * Number of arrangements. Note that this is more than fits on the
         * screen, so the last 128 are unused.
         */
        public static final int NUM_ARRANGEMENTS = 1024;
        /** Number of tiles. */
        public static final int NUM_TILES = 256;

        /** Pointers to ASM pointers * */
        public static final int[] tilePointerArray = new int[]{0x0F0A3,
            0x0F0FB, 0x0F152};
        public static final int[] palPointerArray = new int[]{0x0F0D3, 0x0F12B,
            0x0F182};
        public static final int[] arngPointerArray = new int[]{0x0F0BB,
            0x0F113, 0x0F16A};

        /** The <code>Color<code>'s of each 16 color palette. */
        private Color[][] palette = new Color[NUM_PALETTES][4];
        /** List of all arrangements. */
        private int[] arrangementList = new int[NUM_ARRANGEMENTS];
        /** Two-dimentional array of arrangements used. */
        private int[][] arrangement = new int[32][28];
        /** All tiles stored as pixels being found at [tile_num][x][y]. */
        private byte[][][] tiles = new byte[NUM_TILES][8][8];

        public LogoScreen(int i, EbHackModule hm)
        {
            this.hm = hm;
            this.num = i;

            palPointer = hm.rom.readRegAsmPointer(palPointerArray[num]);
            tilePointer = hm.rom.readRegAsmPointer(tilePointerArray[num]);
            arngPointer = hm.rom.readRegAsmPointer(arngPointerArray[num]);
        }

        private boolean readGraphics(boolean allowFailure, boolean readOrg)
        {
            Rom r = readOrg
                ? JHack.main.getOrginalRomFile(hm.rom.getRomType())
                : hm.rom;

            byte[] tileBuffer = new byte[4096];

            /** * DECOMPRESS GRAPHICS ** */
            System.out.println("About to attempt decompressing "
                + tileBuffer.length + " bytes of logo screen #" + num
                + " graphics.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(tilePointerArray[num]) : tilePointer,
                tileBuffer, r);
            if (tmp[0] < 0)
            {
                System.err.println("Error " + tmp[0]
                    + " decompressing logo screen #" + num + " graphics.");
                if (allowFailure)
                {
                    //EMPTY TILES
                    for (int i = 0; i < tiles.length; i++)
                        for (int x = 0; x < tiles[i].length; x++)
                            Arrays.fill(tiles[i][x], (byte) 0);
                    tileLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                tileLen = tmp[1];
                System.out.println("LogoScreen graphics: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int gfxOffset = 0;
                for (int i = 0; i < NUM_TILES; i++)
                {
                    tiles[i] = new byte[8][8];
                    gfxOffset += HackModule.read2BPPArea(tiles[i], tileBuffer,
                        gfxOffset, 0, 0);
                }
            }
            return true;
        }

        private boolean readPalette(boolean allowFailure, boolean readOrg)
        {
            Rom r = readOrg
                ? JHack.main.getOrginalRomFile(hm.rom.getRomType())
                : hm.rom;

            byte[] palBuffer = new byte[512];
            /** * DECOMPRESS PALETTE ** */
            System.out.println("About to attempt decompressing "
                + palBuffer.length + " bytes of logo screen #" + num
                + " palette.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(palPointerArray[num]) : palPointer,
                palBuffer, r);
            if (tmp[0] < 0)
            {
                System.err.println("Error " + tmp[0]
                    + " decompressing logo screen #" + num + " palette.");
                if (allowFailure)
                { //EMPTY PALETTES
                    for (int i = 0; i < palette.length; i++)
                        Arrays.fill(palette[i], Color.BLACK);
                    palLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                palLen = tmp[1];
                System.out.println("LogoScreen palette: Decompressed " + tmp[0]
                    + " bytes from a " + tmp[1] + " byte compressed block.");

                int palOffset = 0;
                for (int i = 0; i < NUM_PALETTES; i++)
                {
                    HackModule.readPalette(palBuffer, palOffset, palette[i]);
                    palOffset += palette[i].length * 2;
                }
            }
            return true;
        }

        private boolean readArrangement(boolean allowFailure, boolean readOrg)
        {
            Rom r = readOrg
                ? JHack.main.getOrginalRomFile(hm.rom.getRomType())
                : hm.rom;

            byte[] arngBuffer = new byte[2048];

            /** * DECOMPRESS ARRANGEMENT ** */
            System.out.println("About to attempt decompressing "
                + arngBuffer.length + " bytes of logo screen #" + num
                + " arrangement.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(arngPointerArray[num]) : arngPointer,
                arngBuffer, r);
            if (tmp[0] < 0)
            {
                System.err.println("Error " + tmp[0]
                    + " decompressing logo screen #" + num + " arrangement.");
                if (allowFailure)
                { //EMPTY ARRANGEMENTS
                    Arrays.fill(arrangementList, 0);
                    for (int x = 0; x < arrangement.length; x++)
                        Arrays.fill(arrangement[x], 0);
                    arngLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                arngLen = tmp[1];
                System.out.println("LogoScreen arrangement: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int arngOffset = 0;
                for (int i = 0; i < NUM_ARRANGEMENTS; i++)
                {
                    arrangementList[i] = (arngBuffer[arngOffset++] & 0xff)
                        + ((arngBuffer[arngOffset++] & 0xff) << 8);
                }
                int j = 0;
                for (int y = 0; y < arrangement[0].length; y++)
                    for (int x = 0; x < arrangement.length; x++)
                        arrangement[x][y] = arrangementList[j++];
            }

            return true;
        }

        /**
         * Decompresses information from ROM. allowFailure defaults to true, and
         * is set to false when "fail" is selected from the abort/retry/fail box
         * presented to the user when a problem is encountered while reading.
         * 
         * @param allowFailure if true, false will not be returned on failure,
         *            instead the failed item will be set to zeros and reading
         *            will continue
         * @return true if everything is read or if allowFailure is true, false
         *         if any decompression failed and allowFailure is false
         */
        public boolean readInfo(boolean allowFailure)
        {
            if (isInited)
                return true;

            if (!readGraphics(allowFailure, false))
                return false;
            if (!readPalette(allowFailure, false))
                return false;
            if (!readArrangement(allowFailure, false))
                return false;

            isInited = true;
            return true;
        }

        public boolean readInfo()
        {
            return readInfo(false);
        }

        /**
         * Inits all values to zero. Will have no effect if {@link #readInfo()}
         * or this has already been run successfully. Use this if
         * <code>readInfo()</code> always fails.
         */
        public void initToNull()
        {
            readInfo(true);
        }

        private boolean writeGraphics()
        {
            byte[] udataTiles = new byte[4096];
            int tileOff = 0;
            /* COMPRESS TILES */
            for (int i = 0; i < NUM_TILES; i++)
            {
                tileOff += HackModule.write2BPPArea(tiles[i], udataTiles,
                    tileOff, 0, 0);
            }

            byte[] compTile;
            int tileCompLen = comp(udataTiles, compTile = new byte[5000]);
            if (!hm.writeToFreeASMLink(compTile, tilePointerArray[num],
                tileLen, tileCompLen))
                return false;
            System.out.println("Wrote "
                + (tileLen = tileCompLen)
                + " bytes of the logo screen #"
                + num
                + " ("
                + logoScreenNames[num]
                + ") tiles at "
                + Integer.toHexString(tilePointer = hm.rom
                    .readRegAsmPointer(tilePointerArray[num])) + " to "
                + Integer.toHexString(tilePointer + tileCompLen - 1) + ".");

            return true;
        }

        private boolean writePalette()
        {
            byte[] udataPal = new byte[512];
            int palOff = 0;
            /* COMPRESS PALETTE */
            for (int i = 0; i < NUM_PALETTES; i++)
            {
                HackModule.writePalette(udataPal, palOff, palette[i]);
                palOff += palette[i].length * 2;
            }

            byte[] compPal;
            int palCompLen = comp(udataPal, compPal = new byte[600]);
            if (!hm.writeToFreeASMLink(compPal, palPointerArray[num], palLen,
                palCompLen))
                return false;
            System.out.println("Wrote "
                + (palLen = palCompLen)
                + " bytes of the logo screen #"
                + num
                + " ("
                + logoScreenNames[num]
                + ") palette at "
                + Integer.toHexString(palPointer = hm.rom
                    .readRegAsmPointer(palPointerArray[num])) + " to "
                + Integer.toHexString(palPointer + palCompLen - 1) + ".");
            return true;
        }

        private boolean writeArrangement()
        {
            byte[] udataArng = new byte[2048];
            int arngOff = 0;
            /* COMPRESS ARRANGEMENT */
            int j = 0;
            for (int y = 0; y < arrangement[0].length; y++)
                for (int x = 0; x < arrangement.length; x++)
                    arrangementList[j++] = arrangement[x][y];
            for (int i = 0; i < NUM_ARRANGEMENTS; i++)
            {
                udataArng[arngOff++] = (byte) (arrangementList[i] & 0xff);
                udataArng[arngOff++] = (byte) ((arrangementList[i] >> 8) & 0xff);
            }

            byte[] compArng;
            int arngCompLen = comp(udataArng, compArng = new byte[3000]);
            if (!hm.writeToFreeASMLink(compArng, arngPointerArray[num],
                arngLen, arngCompLen))
                return false;
            System.out.println("Wrote "
                + (arngLen = arngCompLen)
                + " bytes of the logo screen #"
                + num
                + " ("
                + logoScreenNames[num]
                + ") arrangement at "
                + Integer.toHexString(arngPointer = hm.rom
                    .readRegAsmPointer(arngPointerArray[num])) + " to "
                + Integer.toHexString(arngPointer + arngCompLen - 1) + ".");

            return true;
        }

        public boolean writeInfo()
        {
            if (!isInited)
                return false;

            if (!writePalette())
                return false;
            if (!writeArrangement())
                return false;
            if (!writeGraphics())
                return false;

            return true;
        }

        /**
         * @return Returns the isInited.
         */
        public boolean isInited()
        {
            return isInited;
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param x
         * @param y
         * @param data
         */
        public void setArrangementData(int x, int y, int data)
        {
            readInfo();
            arrangement[x][y] = data;
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param x
         * @param y
         * @return
         */
        public int getArrangementData(int x, int y)
        {
            readInfo();
            return arrangement[x][y];
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @return
         */
        public int[][] getArrangementData()
        {
            readInfo();
            int[][] out = new int[arrangement.length][arrangement[0].length];
            for (int x = 0; x < out.length; x++)
                for (int y = 0; y < out[0].length; y++)
                    out[x][y] = arrangement[x][y];
            return out;
        }

        public int[] getArrangementArr()
        {
            readInfo();
            int j = 0, out[] = new int[NUM_ARRANGEMENTS];
            for (int y = 0; y < arrangement[0].length; y++)
                for (int x = 0; x < arrangement.length; x++)
                    out[j++] = arrangement[x][y];
            for (; j < NUM_ARRANGEMENTS; j++)
                out[j] = arrangementList[j];
            return out;
        }

        public void setArrangementArr(int[] arr)
        {
            readInfo();
            int j = 0;
            for (int y = 0; y < arrangement[0].length; y++)
                for (int x = 0; x < arrangement.length; x++)
                    arrangement[x][y] = arr[j++];
            for (; j < NUM_ARRANGEMENTS; j++)
                arrangementList[j] = arr[j];
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param data
         */
        public void setArrangementData(int[][] data)
        {
            readInfo();
            for (int x = 0; x < data.length; x++)
                for (int y = 0; y < data[0].length; y++)
                    arrangement[x][y] = data[x][y];
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @param subPal
         * @return
         */
        public Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            readInfo();
            //            byte[][] img = new byte[8][8];
            //            for (int x = 0; x < 8; x++)
            //                for (int y = 0; y < 8; y++)
            //                    img[x][y] = tiles[tile][hFlip ? 7 - x : x][vFlip
            //                        ? 7 - y
            //                        : y];
            return HackModule.drawImage(tiles[tile], palette[subPal], hFlip,
                vFlip);
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @param subPal
         * @return
         */
        public Image getTileImage(int tile, int subPal)
        {
            readInfo();
            return HackModule.drawImage(tiles[tile], palette[subPal]);
        }

        //        /**
        //         * TODO Write javadoc for this method
        //         *
        //         * @param i
        //         * @param j
        //         * @return
        //         */
        //        public Image getTilesetImage(int subPal, int width, int height)
        //        {
        //            readInfo();
        //            BufferedImage out = new BufferedImage(width * 8, height * 8,
        //                BufferedImage.TYPE_INT_ARGB);
        //            Graphics g = out.getGraphics();
        //            int i = 0;
        //            try
        //            {
        //                for (int y = 0; y < height; y++)
        //                    for (int x = 0; x < width; x++)
        //                        g.drawImage(HackModule.drawImage(tiles[i++],
        //                            palette[subPal]), x * 8, y * 8, null);
        //            }
        //            catch (ArrayIndexOutOfBoundsException e)
        //            {}
        //            return out;
        //        }
        //
        //        public Image getTilesetImage(int subPal, int width, int height,
        //            int highlightTile)
        //        {
        //            readInfo();
        //            Image out = getTilesetImage(subPal, width, height);
        //            Graphics g = out.getGraphics();
        //            if (highlightTile >= 0 && highlightTile <= tiles.length - 1)
        //            {
        //                g.setColor(new Color(255, 255, 0, 128));
        //                g.fillRect((highlightTile % width) * 8,
        //                    (highlightTile / width) * 8, 8, 8);
        //            }
        //            return out;
        //        }
        //
        //        /**
        //         * TODO Write javadoc for this method
        //         *
        //         * @param is
        //         * @param f
        //         * @param b
        //         * @return
        //         */
        //        public Image getArrangementImage(int[][] selection, float zoom,
        //            boolean gridLines)
        //        {
        //            readInfo();
        //            BufferedImage out = new BufferedImage((gridLines
        //                ? arrangement.length - 1
        //                : 0)
        //                + (int) (8 * arrangement.length * zoom), (gridLines
        //                ? arrangement[0].length - 1
        //                : 0)
        //                + (int) (8 * arrangement[0].length * zoom),
        //                BufferedImage.TYPE_4BYTE_ABGR_PRE);
        //            Graphics g = out.getGraphics();
        //            for (int x = 0; x < arrangement.length; x++)
        //            {
        //                for (int y = 0; y < arrangement[0].length; y++)
        //                {
        //                    // System.out.println(addZeros(Integer
        //                    // .toBinaryString(this.arrangement[x][y]), 16));
        //                    int arr = selection[x][y] == -1
        //                        ? arrangement[x][y]
        //                        : selection[x][y];
        //                    g.drawImage(getTileImage(arr & 0x01ff,
        //                        ((arr & 0x0400) >> 10), (arr & 0x4000) != 0,
        //                        (arr & 0x8000) != 0), (int) (x * 8 * zoom)
        //                        + (gridLines ? x : 0), (int) (y * 8 * zoom)
        //                        + (gridLines ? y : 0), (int) (8 * zoom),
        //                        (int) (8 * zoom), null);
        //                    if (selection[x][y] != -1)
        //                    {
        //                        g.setColor(new Color(255, 255, 0, 128));
        //                        g.fillRect((int) (x * 8 * zoom) + (gridLines ? x : 0),
        //                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
        //                            (int) (8 * zoom), (int) (8 * zoom));
        //                    }
        //                }
        //            }
        //            return out;
        //        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param pal
         * @return
         */
        public Color[] getSubPal(int pal)
        {
            readInfo();
            Color[] out = new Color[4];
            System.arraycopy(palette[pal], 0, out, 0, 4);
            return out;
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @return
         */
        public byte[][] getTile(int tile)
        {
            byte[][] out = new byte[8][8];
            for (int x = 0; x < 8; x++)
            {
                for (int y = 0; y < 8; y++)
                {
                    out[x][y] = this.getTilePixel(tile, x, y);
                }
            }
            return out;
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @param x
         * @param y
         * @return
         */
        private byte getTilePixel(int tile, int x, int y)
        {
            readInfo();
            return tiles[tile][x][y];
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @param in
         */
        public void setTile(int tile, byte[][] in)
        {
            for (int x = 0; x < 8; x++)
            {
                for (int y = 0; y < 8; y++)
                {
                    this.setTilePixel(tile, x, y, in[x][y]);
                }
            }
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param tile
         * @param x
         * @param y
         * @param i
         */
        private void setTilePixel(int tile, int x, int y, byte i)
        {
            readInfo();
            this.tiles[tile][x][y] = i;
        }

        /**
         * TODO Write javadoc for this method
         * 
         * @param col
         * @param subPal
         * @param color
         */
        public void setPaletteColor(int col, int subPal, Color color)
        {
            readInfo();
            palette[subPal][col] = color;
        }
    }

    public static final LogoScreen[] logoScreens = new LogoScreen[NUM_LOGO_SCREENS];

    public static void readFromRom(EbHackModule hm)
    {
        for (int i = 0; i < logoScreens.length; i++)
        {
            logoScreens[i] = new LogoScreen(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    /**
     * Reads in {@link EbHackModule#logoScreenNames}if it hasn't already been
     * read in. Reads from net/starmen/pkhack/logoScreenNames.txt.
     */
    public static void initLogoScreenNames(String romPath)
    {
        readArray(DEFAULT_BASE_DIR, "logoScreenNames.txt", romPath, false,
            logoScreenNames);
    }

    public void reset()
    {
        initLogoScreenNames(rom.getPath());
        readFromRom();
    }

    private boolean guiInited = false;

    private class LogoScreenArrangementEditor extends ArrangementEditor
    {
        public LogoScreenArrangementEditor()
        {
            super();
            this.setPreferredSize(new Dimension(getTilesWide()
                * (getTileSize() * getZoom()), getTilesHigh()
                * (getTileSize() * getZoom())));
        }

        public int makeArrangementNumber(int tile, int subPalette,
            boolean hFlip, boolean vFlip)
        {
            return super.makeArrangementNumber(tile, subPalette - 2, hFlip,
                vFlip);
        }

        protected int getCurrentTile()
        {
            return tileSelector.getCurrentTile();
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
            return 2;
        }

        protected boolean isDrawGridLines()
        {
            try
            {
                return prefs
                    .getValueAsBoolean("eb.LogoScreenEditor.arrEditor.gridLines");
            }
            catch (NullPointerException e)
            {
                return JHack.main.getPrefs().getValueAsBoolean(
                    "eb.LogoScreenEditor.arrEditor.gridLines");
            }
        }

        protected boolean isEditable()
        {
            return true;
        }

        protected boolean isGuiInited()
        {
            return guiInited;
        }

        protected int getCurrentSubPalette()
        {
            return LogoScreenEditor.this.getCurrentSubPalette();
        }

        protected int getArrangementData(int x, int y)
        {
            return getSelectedScreen().getArrangementData(x, y);
        }

        protected int[][] getArrangementData()
        {
            return getSelectedScreen().getArrangementData();
        }

        protected void setArrangementData(int x, int y, int data)
        {
            getSelectedScreen().setArrangementData(x, y, data);
        }

        protected void setArrangementData(int[][] data)
        {
            getSelectedScreen().setArrangementData(data);
        }

        //        protected Image getArrangementImage(int[][] selection)
        //        {
        //            return getSelectedScreen().getArrangementImage(selection,
        //                getZoom(), isDrawGridLines());
        //        }

        protected Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            return getSelectedScreen().getTileImage(tile, subPal, hFlip, vFlip);
        }
    }

    private class FocusIndicator extends AbstractButton implements
        FocusListener, MouseListener
    {
        //true = tile editor, false = arrangement editor
        private boolean focus = true;

        public Component getCurrentFocus()
        {
            return (focus ? (Component) da : (Component) arrangementEditor);
        }

        public Undoable getCurrentUndoable()
        {
            return (focus ? (Undoable) da : (Undoable) arrangementEditor);
        }

        public CopyAndPaster getCurrentCopyAndPaster()
        {
            return (focus
                ? (CopyAndPaster) da
                : (CopyAndPaster) arrangementEditor);
        }

        private void cycleFocus()
        {
            focus = !focus;
            repaint();
        }

        private void setFocus(Component c)
        {
            focus = c == da;
            repaint();
        }

        public void focusGained(FocusEvent fe)
        {
            System.out.println("FocusIndicator.focusGained(FocusEvent)");
            setFocus(fe.getComponent());
            repaint();
        }

        public void focusLost(FocusEvent arg0)
        {}

        public void mouseClicked(MouseEvent me)
        {
            cycleFocus();
        }

        public void mousePressed(MouseEvent arg0)
        {}

        public void mouseReleased(MouseEvent arg0)
        {}

        public void mouseEntered(MouseEvent arg0)
        {}

        public void mouseExited(MouseEvent arg0)
        {}

        public void paint(Graphics g)
        {
            int[] y = new int[]{40, 10, 10, 00, 10, 10, 40};
            int[] x = new int[]{22, 22, 15, 25, 35, 28, 28};

            if (!focus)
                for (int i = 0; i < y.length; i++)
                    y[i] = 50 - y[i];

            g.fillPolygon(x, y, 7);
        }

        public FocusIndicator()
        {
            da.addFocusListener(this);
            arrangementEditor.addFocusListener(this);

            this.addMouseListener(this);

            this.setPreferredSize(new Dimension(50, 50));

            this
                .setToolTipText("This arrow points to which component will recive menu commands. "
                    + "Click to change.");
        }
    }

    private LogoScreenArrangementEditor arrangementEditor;
    private TileSelector tileSelector;
    private IntArrDrawingArea da;
    private SpritePalette pal;
    private DrawingToolset dt;
    private FocusIndicator fi;

    private JComboBox mapSelector, subPalSelector;
    private JTextField name;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        //menu
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

        JMenu editMenu = HackModule.createEditMenu(this, true);

        editMenu.addSeparator();

        editMenu.add(HackModule.createJMenuItem("H-Flip", 'h', null, "hFlip",
            this));
        editMenu.add(HackModule.createJMenuItem("V-Flip", 'v', null, "vFlip",
            this));

        mb.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Enable Tile Selector Grid Lines",
            prefs, "eb.LogoScreenEditor.tileSelector.gridLines", false, 't',
            null, "tileSelGridLines", this));
        optionsMenu.add(new PrefsCheckBox(
            "Enable Arrangement Editor Grid Lines", prefs,
            "eb.LogoScreenEditor.arrEditor.gridLines", false, 'a', null,
            "arrEdGridLines", this));
        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        //components
        tileSelector = new TileSelector()
        {
            public int getTilesWide()
            {
                return 16;
            }

            public int getTilesHigh()
            {
                return 16;
            }

            public int getTileSize()
            {
                return 8;
            }

            public int getZoom()
            {
                return 2;
            }

            public boolean isDrawGridLines()
            {
                try
                {
                    return prefs
                        .getValueAsBoolean("eb.LogoScreenEditor.tileSelector.gridLines");
                }
                catch (NullPointerException e)
                {
                    return JHack.main.getPrefs().getValueAsBoolean(
                        "eb.LogoScreenEditor.tileSelector.gridLines");
                }
            }

            public int getTileCount()
            {
                return LogoScreen.NUM_TILES;
            }

            public Image getTileImage(int tile)
            {
                return getSelectedScreen().getTileImage(tile,
                    getCurrentSubPalette());
            }

            protected boolean isGuiInited()
            {
                return guiInited;
            }
        };
        tileSelector.setActionCommand("tileSelector");
        tileSelector.addActionListener(this);

        arrangementEditor = new LogoScreenArrangementEditor();
        arrangementEditor.setActionCommand("arrangementEditor");
        arrangementEditor.addActionListener(this);

        dt = new DrawingToolset(this);

        pal = new SpritePalette(4, true);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);

        da = new IntArrDrawingArea(dt, pal, this);
        da.setActionCommand("drawingArea");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(80, 80));

        mapSelector = createComboBox(logoScreenNames, this);
        mapSelector.setActionCommand("mapSelector");

        name = new JTextField(15);

        subPalSelector = createJComboBoxFromArray(
            new Object[LogoScreen.NUM_PALETTES], false);
        subPalSelector.setSelectedIndex(0);
        subPalSelector.setActionCommand("subPalSelector");
        subPalSelector.addActionListener(this);

        fi = new FocusIndicator();

        Box center = new Box(BoxLayout.Y_AXIS);
        center.add(getLabeledComponent("Map: ", mapSelector));
        center.add(getLabeledComponent("Map Name: ", name));
        center.add(getLabeledComponent("SubPalette: ", subPalSelector));
        center.add(Box.createVerticalStrut(15));
        center.add(createFlowLayout(da));
        center.add(Box.createVerticalStrut(5));
        center.add(createFlowLayout(pal));
        center.add(Box.createVerticalStrut(10));
        center.add(createFlowLayout(fi));
        center.add(Box.createVerticalGlue());

        JPanel display = new JPanel(new BorderLayout());
        display.add(pairComponents(dt, null, false), BorderLayout.EAST);
        display.add(pairComponents(pairComponents(tileSelector, center, true),
            arrangementEditor, false), BorderLayout.WEST);

        mainWindow.getContentPane().add(new JScrollPane(display),
            BorderLayout.CENTER);

        mainWindow.pack();
    }

    public void show()
    {
        super.show();

        readFromRom();
        mapSelector.setSelectedIndex(mapSelector.getSelectedIndex() == -1
            ? 0
            : mapSelector.getSelectedIndex());

        mainWindow.setVisible(true);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void doLogoSelectAction()
    {
        if (!getSelectedScreen().readInfo())
        {
            guiInited = false;
            Object opt = JOptionPane.showInputDialog(mainWindow,
                "Error decompressing the "
                    + logoScreenNames[getCurrentScreen()] + " screen (#"
                    + getCurrentScreen() + ").", "Decompression Error",
                JOptionPane.ERROR_MESSAGE, null, new String[]{"Abort", "Retry",
                    "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                mapSelector
                    .setSelectedIndex((mapSelector.getSelectedIndex() + 1)
                        % mapSelector.getItemCount());
                doLogoSelectAction();
                return;
            }
            else if (opt.equals("Retry"))
            {
                //                mapSelector.setSelectedIndex(mapSelector.getSelectedIndex());
                doLogoSelectAction();
                return;
            }
            else if (opt.equals("Fail"))
            {
                getSelectedScreen().initToNull();
            }
        }
        guiInited = true;
        name.setText(logoScreenNames[getCurrentScreen()]);
        updatePaletteDisplay();
        tileSelector.repaint();
        arrangementEditor.clearSelection();
        arrangementEditor.repaint();
        updateTileEditor();
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("drawingArea"))
        {
            getSelectedScreen().setTile(getCurrentTile(), da.getByteArrImage());
            tileSelector.repaintCurrent();
            arrangementEditor.repaintCurrentTile();
            fi.setFocus(da);
        }
        else if (ae.getActionCommand().equals("arrangementEditor"))
        {
            fi.setFocus(arrangementEditor);
        }
        else if (ae.getActionCommand().equals("mapSelector"))
        {
            doLogoSelectAction();
        }
        else if (ae.getActionCommand().equals("tileSelector"))
        {
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("subPalSelector"))
        {
            updatePaletteDisplay();
            tileSelector.repaint();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            getSelectedScreen().setPaletteColor(pal.getSelectedColorIndex(),
                getCurrentSubPalette(), pal.getNewColor());
            updatePaletteDisplay();
            da.repaint();
            tileSelector.repaint();
            arrangementEditor.repaint();
        }
        //flip
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
        //edit menu
        //undo
        else if (ae.getActionCommand().equals("undo"))
        {
            fi.getCurrentUndoable().undo();
        }
        //copy&paste stuff
        else if (ae.getActionCommand().equals("cut"))
        {
            fi.getCurrentCopyAndPaster().cut();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            fi.getCurrentCopyAndPaster().copy();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            fi.getCurrentCopyAndPaster().paste();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            fi.getCurrentCopyAndPaster().delete();
        }
        else if (ae.getActionCommand().equals("tileSelGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("arrEdGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            arrangementEditor.invalidate();
            arrangementEditor.resetPreferredSize();
            arrangementEditor.validate();
            arrangementEditor.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("import"))
        {
            importData();

            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.clearSelection();
            arrangementEditor.repaint();
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("export"))
        {
            exportData();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            //TownMap.writeInfo() will only write if the map has been inited
            for (int i = 0; i < logoScreens.length; i++)
                logoScreens[i].writeInfo();
            int m = getCurrentScreen();
            logoScreenNames[m] = name.getText();
            notifyDataListeners(logoScreenNames, this, m);
            writeArray("logoScreenNames.txt", false, logoScreenNames);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else
        {
            System.err
                .println("LogoScreenEditor.actionPerformed: ERROR: unhandled "
                    + "action command: \"" + ae.getActionCommand() + "\"");
        }
    }

    private int getCurrentScreen()
    {
        return mapSelector.getSelectedIndex();
    }

    private LogoScreen getSelectedScreen()
    {
        return logoScreens[getCurrentScreen()];
    }

    private int getCurrentSubPalette()
    {
        return subPalSelector.getSelectedIndex();
    }

    private Color[] getSelectedSubPalette()
    {
        return getSelectedScreen().getSubPal(getCurrentSubPalette());
    }

    private int getCurrentTile()
    {
        return tileSelector.getCurrentTile();
    }

    private void updatePaletteDisplay()
    {
        pal.setPalette(getSelectedSubPalette());
        pal.repaint();
    }

    private void updateTileEditor()
    {
        da.setImage(getSelectedScreen().getTile(getCurrentTile()));
    }

    public static final int NODE_BASE = 0;
    public static final int NODE_TILES = 1;
    public static final int NODE_ARR = 2;
    public static final int NODE_PAL = 3;

    public static class LogoScreenImportData
    {
        public byte[][][] tiles;
        public int[] arrangement;
        public Color[][] palette;
    }

    public static final byte LSCN_VERSION = 1;

    public static void exportData(File f, boolean[][] a)
    {
        //make a byte whichMaps. for each map if it is used set the bit at the
        // place equal to the map number to 1
        byte whichMaps = 0;
        for (int i = 0; i < a.length; i++)
            whichMaps |= (a[i][NODE_BASE] ? 1 : 0) << i;

        try
        {
            FileOutputStream out = new FileOutputStream(f);

            out.write(LSCN_VERSION);
            out.write(whichMaps);
            for (int m = 0; m < a.length; m++)
            {
                if (a[m][NODE_BASE])
                {
                    //if writing this map...
                    //say what parts we will write, once again as a bit mask
                    byte whichParts = 0;
                    for (int i = 1; i < a[m].length; i++)
                        whichParts |= (a[m][i] ? 1 : 0) << (i - 1);
                    out.write(whichParts);
                    //write tiles?
                    if (a[m][NODE_TILES])
                    {
                        byte[] b = new byte[LogoScreen.NUM_TILES * 32];
                        int offset = 0;
                        for (int i = 0; i < LogoScreen.NUM_TILES; i++)
                            offset += write2BPPArea(logoScreens[m].getTile(i),
                                b, offset, 0, 0);
                        out.write(b);
                    }
                    //write arrangements?
                    if (a[m][NODE_ARR])
                    {
                        int[] arr = logoScreens[m].getArrangementArr();
                        byte[] barr = new byte[arr.length * 2];
                        int off = 0;
                        for (int i = 0; i < arr.length; i++)
                        {
                            barr[off++] = (byte) (arr[i] & 0xff);
                            barr[off++] = (byte) ((arr[i] >> 8) & 0xff);
                        }
                        out.write(barr);
                    }
                    //write palettes?
                    if (a[m][NODE_PAL])
                    {
                        byte[] pal = new byte[LogoScreen.NUM_PALETTES * 4 * 2];
                        for (int i = 0; i < LogoScreen.NUM_PALETTES; i++)
                            writePalette(pal, i * 4 * 2, logoScreens[m]
                                .getSubPal(i));
                        out.write(pal);
                    }
                }
            }

            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error exporting logo screen data to "
                    + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error exporting logo screen data to "
                + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
    }

    public static LogoScreenImportData[] importData(InputStream in)
        throws IOException
    {
        LogoScreenImportData[] out = new LogoScreenImportData[logoScreens.length];

        byte version = (byte) in.read();
        if (version > LSCN_VERSION)
        {
            if (JOptionPane.showConfirmDialog(null,
                "LSCN file version not supported." + "Try to load anyway?",
                "LSCN Version " + version + " Not Supported",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
                return null;
        }
        byte whichMaps = (byte) in.read();
        for (int m = 0; m < logoScreens.length; m++)
        {
            //if bit for this map set...
            if (((whichMaps >> m) & 1) != 0)
            {
                out[m] = new LogoScreenImportData();
                byte whichParts = (byte) in.read();
                //if tile bit set...
                if ((whichParts & 1) != 0)
                {
                    byte[] b = new byte[LogoScreen.NUM_TILES * 32];
                    in.read(b);

                    int offset = 0;
                    out[m].tiles = new byte[LogoScreen.NUM_TILES][8][8];
                    for (int i = 0; i < LogoScreen.NUM_TILES; i++)
                        offset += read2BPPArea(out[m].tiles[i], b, offset, 0, 0);
                }
                //if arr bit set...
                if (((whichParts >> 1) & 1) != 0)
                {
                    out[m].arrangement = new int[LogoScreen.NUM_ARRANGEMENTS];
                    byte[] barr = new byte[out[m].arrangement.length * 2];
                    in.read(barr);

                    int off = 0;
                    for (int i = 0; i < out[m].arrangement.length; i++)
                    {
                        out[m].arrangement[i] = (barr[off++] & 0xff);
                        out[m].arrangement[i] += ((barr[off++] & 0xff) << 8);
                    }
                }
                //if pal bit set...
                if (((whichParts >> 2) & 1) != 0)
                {
                    byte[] pal = new byte[LogoScreen.NUM_PALETTES * 4 * 2];
                    in.read(pal);

                    out[m].palette = new Color[LogoScreen.NUM_PALETTES][4];
                    for (int i = 0; i < out[m].palette.length; i++)
                        readPalette(pal, i * 4 * 2, out[m].palette[i]);
                }
            }
        }

        in.close();

        return out;
    }

    public static LogoScreenImportData[] importData(byte[] b)
    {
        try
        {
            return importData(new ByteArrayInputStream(b));
        }
        catch (IOException e)
        {
            System.err.println("IO error importing logo screen data "
                + "from byte array.");
            e.printStackTrace();
        }
        return null;
    }

    public static LogoScreenImportData[] importData(File f)
    {
        try
        {
            return importData(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error importing logo screen data from "
                    + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error importing logo screen data from "
                + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        return null;
    }

    private static boolean[][] showCheckList(boolean[][] in, String text,
        String title)
    {
        CheckNode topNode = new CheckNode("Logo Screens", true, true);
        topNode.setSelectionMode(CheckNode.DIG_IN_SELECTION);
        CheckNode[][] mapNodes = new CheckNode[NUM_LOGO_SCREENS][4];
        for (int i = 0; i < mapNodes.length; i++)
        {
            if (in == null || in[i][NODE_BASE])
            {
                mapNodes[i][NODE_BASE] = new CheckNode(logoScreenNames[i],
                    true, true);
                mapNodes[i][NODE_BASE]
                    .setSelectionMode(CheckNode.DIG_IN_SELECTION);
                if (in == null || in[i] == null || in[i][NODE_TILES])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_TILES] = new CheckNode("Tiles",
                            false, true));
                if (in == null || in[i] == null || in[i][NODE_ARR])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_ARR] = new CheckNode(
                            "Arrangement", false, true));
                if (in == null || in[i] == null || in[i][NODE_PAL])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_PAL] = new CheckNode("Palettes",
                            false, true));

                topNode.add(mapNodes[i][NODE_BASE]);
            }
        }
        JTree checkTree = new JTree(topNode);
        checkTree.setCellRenderer(new CheckRenderer());
        checkTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);
        checkTree.putClientProperty("JTree.lineStyle", "Angled");
        checkTree.addMouseListener(new NodeSelectionListener(checkTree));

        if (JOptionPane.showConfirmDialog(null, pairComponents(
            new JLabel(text), new JScrollPane(checkTree), false), title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.CANCEL_OPTION)
            return null;

        boolean[][] a = new boolean[NUM_LOGO_SCREENS][4];
        for (int m = 0; m < NUM_LOGO_SCREENS; m++)
            for (int i = 0; i < 4; i++)
                a[m][i] = mapNodes[m][i] == null ? false : mapNodes[m][i]
                    .isSelected();

        return a;
    }

    private boolean exportData()
    {
        boolean[][] a = showCheckList(null, "<html>"
            + "Select which items you wish to export." + "</html>",
            "Export What?");
        if (a == null)
            return false;

        File f = getFile(true, "lscn", "Logo SCreeN");
        if (f != null)
            exportData(f, a);
        return true;
    }

    private boolean importData()
    {
        File f = getFile(false, "lscn", "Logo SCreeN");
        LogoScreenImportData[] tmid;
        if (f == null || (tmid = importData(f)) == null)
            return false;
        return importData(tmid);
    }

    private boolean importData(LogoScreenImportData[] tmid)
    {
        boolean[][] in = new boolean[NUM_LOGO_SCREENS][4];
        for (int i = 0; i < in.length; i++)
        {
            if (tmid[i] != null)
            {
                in[i][NODE_BASE] = true;
                in[i][NODE_TILES] = tmid[i].tiles != null;
                in[i][NODE_ARR] = tmid[i].arrangement != null;
                in[i][NODE_PAL] = tmid[i].palette != null;
            }
        }

        final boolean[][] a = showCheckList(in, "<html>"
            + "Select which items you wish to<br>"
            + "import. You will have a chance<br>"
            + "to select which screen you want to<br>"
            + "actually put the imported data." + "</html>", "Import What?");
        if (a == null)
            return false;

        Box targetMap = new Box(BoxLayout.Y_AXIS);
        final JComboBox[] targets = new JComboBox[NUM_LOGO_SCREENS];
        for (int m = 0; m < targets.length; m++)
        {
            if (a[m][NODE_BASE])
            {
                targets[m] = createComboBox(logoScreenNames);
                targets[m].setSelectedIndex(m);
                targetMap.add(getLabeledComponent(logoScreenNames[m] + " ("
                    + (a[m][NODE_TILES] ? "T" : "")
                    + (a[m][NODE_ARR] ? "A" : "") + (a[m][NODE_PAL] ? "P" : "")
                    + "): ", targets[m]));

            }
        }

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
                boolean[][] t = new boolean[NUM_LOGO_SCREENS][4];
                //m = source screen
                for (int m = 0; m < NUM_LOGO_SCREENS; m++)
                {
                    if (targets[m] != null)
                    {
                        //n = target map
                        int n = targets[m].getSelectedIndex();
                        for (int i = 1; i < 4; i++)
                        {
                            if (a[m][i])
                            {
                                //if part already used...
                                if (t[n][i])
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
                                    t[n][i] = true;
                                }
                            }
                        }
                    }
                }
                targetDialog.setVisible(false);
            }
        });
        targetDialog.getContentPane().add(
            createFlowLayout(new Component[]{ok, cancel}), BorderLayout.SOUTH);
        targetDialog.getContentPane().add(
            pairComponents(new JLabel("<html>"
                + "Select which map you would like<br>"
                + "the data to be imported into.<br>"
                + "For example, if you wish to import<br>" + "the "
                + logoScreenNames[0] + " screen into the<br>"
                + logoScreenNames[1]
                + " screen, then change the pull-down menu<br>" + "labeled "
                + logoScreenNames[0] + " to " + logoScreenNames[1]
                + ". If you do not<br>"
                + "wish to make any changes, just click ok.<br>" + "<br>"
                + "The T, A, and P indictate that you will be<br>"
                + "importing tiles, arrangements, and palettes<br>"
                + "respectively from that screen." + "</html>"), targetMap,
                false), BorderLayout.CENTER);
        targetDialog.pack();

        targetDialog.setVisible(true);
        if (targetDialog.getTitle().equals("Canceled"))
            return false;

        for (int m = 0; m < NUM_LOGO_SCREENS; m++)
        {
            if (a[m][NODE_BASE])
            {
                int n = targets[m].getSelectedIndex();
                if (a[m][NODE_TILES])
                    for (int i = 0; i < LogoScreen.NUM_TILES; i++)
                        logoScreens[n].setTile(i, tmid[m].tiles[i]);
                if (a[m][NODE_ARR])
                    logoScreens[n].setArrangementArr(tmid[m].arrangement);
                if (a[m][NODE_PAL])
                    for (int p = 0; p < LogoScreen.NUM_PALETTES; p++)
                        for (int c = 0; c < 4; c++)
                            logoScreens[n].setPaletteColor(c, p,
                                tmid[m].palette[p][c]);
            }
        }
        return true;
    }

    /**
     * Imports data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "applying" files with .lscn
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param lse instance of <code>LogoScreenEditor</code> to call
     *            <code>importData()</code> on
     */
    public static boolean importData(byte[] b, LogoScreenEditor lse)
    {
        boolean out = lse.importData(importData(b));
        if (out)
        {
            if (lse.mainWindow != null)
            {
                lse.mainWindow.repaint();
                lse.updatePaletteDisplay();
                lse.tileSelector.repaint();
                lse.arrangementEditor.clearSelection();
                lse.arrangementEditor.repaint();
                lse.updateTileEditor();
            }
            for (int i = 0; i < logoScreens.length; i++)
                logoScreens[i].writeInfo();
        }
        return out;
    }

    private static boolean checkScreen(LogoScreenImportData lsid, int i)
    {
        if (lsid.tiles != null)
        {
            //check tiles
            for (int t = 0; t < lsid.tiles.length; t++)
                for (int x = 0; x < lsid.tiles[t].length; x++)
                    if (!Arrays.equals(lsid.tiles[t][x],
                        logoScreens[i].tiles[t][x]))
                        return false;
        }
        if (lsid.arrangement != null)
        {
            //check arrangement
            //TODO does this work?
            if (!Arrays
                .equals(lsid.arrangement, logoScreens[i].arrangementList))
                ;
        }
        if (lsid.palette != null)
        {
            //check palette
            for (int p = 0; p < lsid.palette.length; p++)
                for (int c = 0; c < lsid.palette[p].length; c++)
                    if (!lsid.palette[p][c]
                        .equals(logoScreens[i].palette[p][c]))
                        return false;
        }

        //nothing found wrong
        return true;
    }

    private static boolean checkScreen(LogoScreenImportData lsid)
    {
        for (int i = 0; i < NUM_LOGO_SCREENS; i++)
            if (checkScreen(lsid, i))
                return true;
        return false;
    }

    /**
     * Checks if data from the given <code>byte[]</code> has been imported.
     * This method exists to be called by <code>IPSDatabase</code> for
     * "checking" files with .lscn extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param lse instance of <code>LogoScreenEditor</code>
     */
    public static boolean checkData(byte[] b, LogoScreenEditor lse)
    {
        LogoScreenImportData[] lsid = importData(b);

        for (int i = 0; i < lsid.length; i++)
            if (lsid[i] != null)
                if (!checkScreen(lsid[i]))
                    return false;

        return true;
    }

    /**
     * Restore data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "unapplying" files with .lscn
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param lse instance of <code>LogoScreenEditor</code>
     */
    public static boolean restoreData(byte[] b, LogoScreenEditor lse)
    {
        boolean[][] a = showCheckList(null, "<html>Select which items you wish"
            + "to restore to the orginal EarthBound verions.</html>",
            "Restore what?");
        if (a == null)
            return false;

        for (int i = 0; i < a.length; i++)
        {
            if (a[i][NODE_BASE])
            {
                if (a[i][NODE_TILES])
                {
                    logoScreens[i].readGraphics(true, true);
                    logoScreens[i].writeGraphics();
                }
                if (a[i][NODE_ARR])
                {
                    logoScreens[i].readArrangement(true, true);
                    logoScreens[i].writeArrangement();
                }
                if (a[i][NODE_PAL])
                {
                    logoScreens[i].readPalette(true, true);
                    logoScreens[i].writePalette();
                }
            }
        }

        if (lse.mainWindow != null)
        {
            lse.mainWindow.repaint();
            lse.updatePaletteDisplay();
            lse.tileSelector.repaint();
            lse.arrangementEditor.clearSelection();
            lse.arrangementEditor.repaint();
            lse.updateTileEditor();
        }

        return true;
    }
}