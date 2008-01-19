/*
 * Created on Apr 17, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.CopyAndPaster;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.ImageDrawingArea;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.RomWriteOutOfRangeException;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.Undoable;
import net.starmen.pkhack.XMLPreferences;

/**
 * This class provides the API and GUI for editing tile graphics, collision
 * data, and tile arrangements. Palette editing may come later.
 * 
 * @see TileEditor.Tileset
 * @author AnyoneEB
 */
public class TileEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public TileEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    // Static stuff
    public static final int NUM_TILESETS = 20;
    /**
     * All information about the 20 tilesets: graphics, arrangements, collision
     * data.
     * 
     * @see TileEditor.Tileset
     */
    public static Tileset[] tilesets = new Tileset[NUM_TILESETS];
    private static boolean paletteIsInited = false;
    /** The names of 20 tilesets. */
    public static final String[] TILESET_NAMES = {"Underworld", "Onett",
        "Twoson", "Threed", "Fourside", "Magicant", "Outdoors", "Summers",
        "Desert", "Dalaam", "Indoors 1", "Indoors 2", "Stores 1", "Caves 1",
        "Indoors 3", "Stores 2", "Indoors 4", "Winters", "Scaraba", "Caves 2"};

    /**
     * Class representing all data on a tileset. Can read/write mini tile
     * graphics, arrangements, and collision data. Writing not yet implemented.
     * Palette read-only right now, writing will be added after TileEditor v0.1
     * (that is after the rest works).
     * 
     * @author AnyoneEB
     */
    public static class Tileset
    {
        private EbHackModule hm;
        private byte[][][] tiles; // deinterlaced tiles
        private short[][][] arrangements; // arrangements
        private byte[][][] collision; // collision data
        private ArrayList palettes;
        private int tileAddress;
        private int arrangmentsAddress;
        private int collisionAddress;
        private boolean isInited = false;
        // should be written in expanded meg
        private boolean tilesChanged = false, arrangementsChanged = false,
                collisionChanged = false;
        private int num; // number of the tileset
        private String name; // String to identify tileset by
        private int tileOldCompLen;
        private int arrOldCompLen;

        /**
         * Create a new Tileset object.
         * 
         * @param num What number tileset is this? (0-19)
         * @param name
         */
        public Tileset(int num, String name, EbHackModule hm)
        {
            this.hm = hm;
            this.num = num;
            this.name = name;
            this.palettes = new ArrayList();
            readAddresses();
        }

        private void readAddresses()
        {
            this.tileAddress = HackModule.toRegPointer(hm.rom.readMulti(
                0x2F125B + (num * 4), 4));
            this.tilesChanged = tileAddress > 0x3001ff;

            this.arrangmentsAddress = HackModule.toRegPointer(hm.rom.readMulti(
                0x2F12AB + (num * 4), 4));
            this.arrangementsChanged = arrangmentsAddress > 0x3001ff;
        }

        /**
         * Initate the data in this Tileset. Will only run once no matter how
         * many times it is called. This will be done automatically at the first
         * read/write attempt on this.
         */
        public boolean init()
        {
            if (!isInited)
            {
                readAddresses();
                if (!readTiles())
                    return false;
                if (!readArrangements())
                    return false;
                readCollision();
                // readPalettes();
                // all palettes must be done at the same time by
                // TileEditor.readPalettes()
                isInited = true;
            }
            return true;
        }

        public void initToNull()
        {
            if (!isInited)
            {
                readAddresses();
                readTiles();
                readArrangements();
                readCollision();
                isInited = true;
            }
        }

        /**
         * Returns true if this tileset has been inited. {@link #init()}checks
         * this before doing anything so you do not have to.
         * 
         * @return True if this tileset has been inited.
         */
        public boolean isInited()
        {
            return isInited;
        }

        /**
         * Reads in and deinterlaces tile graphics.
         */
        private boolean readTiles()
        {
            byte[] buffer = new byte[28673]; // int[] to decompress into
            int[] tmp;
            if ((tmp = hm.decomp(tileAddress, buffer, 28673))[0] != 28673)
            {
                System.out.println("Error bad compressed data on tileset #"
                    + num + " tiles. (" + tmp[0] + ")");
            }
            tileOldCompLen = tmp[1];

            tiles = new byte[1024][8][8];
            tileloop: for (int t = 0; t < 1024; t++)
            {
                try
                {
                    read4BPPArea(this.tiles[t], buffer, t * 32, 0, 0);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    break tileloop;
                    // if the array has ended, there's nothing left to do
                }
            }
            if (tmp[0] == 28673)
                return true;
            else
                return false;
        }

        private boolean readArrangements()
        {
            byte arrBuffer[] = new byte[32768];
            int[] tmp;
            if ((tmp = hm.decomp(this.arrangmentsAddress, arrBuffer, 32768))[0] < 0)
            {
                System.out.println("Error bad compressed data on tileset #"
                    + num + " arrangments. (" + tmp[0] + ")");
            }
            arrOldCompLen = tmp[1];

            arrangements = new short[1024][4][4];
            int a = 0;
            try
            {
                for (int i = 0; i < 1024; i++)
                {
                    for (int y = 0; y < 4; y++)
                    {
                        for (int x = 0; x < 4; x++)
                        {
                            this.arrangements[i][x][y] = (short) ((arrBuffer[a++] & 0xff) + ((arrBuffer[a++] & 0xff) << 8));
                        }
                    }
                }
            }
            catch (ArrayIndexOutOfBoundsException e)
            {}
            return tmp[0] > 0;
        }

        private void readCollision()
        {
            /*
             * Collsion data uses three tables. First table is list of 20
             * pointers, one for each tileset. Those point to the second table:
             * The list of pointers for each arrangement. Each of those pointers
             * point to a third table where the 16 collision bytes for that
             * arrangement are listed. Look at the code for a better
             * explaination.
             */
            collision = new byte[1024][4][4];
            this.collisionAddress = HackModule.toRegPointer(hm.rom.readMulti(
                0x2F137B + (num * 4), 4));
            for (int j = 0; j < 1024; j++)
            {
                int tmpAddress = hm.rom.readMulti(this.collisionAddress
                    + (j * 2), 2);
                for (int k = 0; k < 16; k++)
                {
                    this.collision[j][k % 4][k / 4] = hm.rom
                        .readByte(tmpAddress + 0x180200 + k);
                }
            }
        }

        /**
         * Contains palette info.
         * 
         * @author AnyoneEB
         */
        public static class Palette
        {
            private int mtileset, mpalette, start;

            /**
             * Creates a map palette information object.
             * 
             * @param mtileset the map tileset this palette is associated with
             * @param mpalette the map palette this palette is associated with
             * @param start the starting address of this palette as a regular
             *            (not SNES) pointer
             */
            public Palette(int mtileset, int mpalette, int start)
            {
                this.mtileset = mtileset;
                this.mpalette = mpalette;
                this.start = start;
            }

            /**
             * Returns a String containing the map tileset and palette. For
             * example, "2/4" means map tileset 2, palette 4.
             * 
             * @return a String containing the map tileset, a slash, and then
             *         the map palette
             */
            public String toString()
            {
                return mtileset + "/" + mpalette;
            }

            /**
             * Checks if this has a specific map tileset and palette.
             * 
             * @param mtileset map tileset
             * @param mpalette map palette
             * @return true if this has the same map tileset and palette as the
             *         inputs
             */
            public boolean equals(int mtileset, int mpalette)
            {
                return this.mtileset == mtileset && this.mpalette == mpalette;
            }

            /**
             * Checks if this has the same map tileset and palette as another
             * <code>Palette</code>.
             * 
             * @param other other palette to compare this one to
             * @return true if this has the same map tileset and palette as the
             *         inputted <code>Palette</code>
             */
            public boolean equals(Palette other)
            {
                return equals(other.mtileset, other.mpalette);
            }

            /**
             * Returns false.
             * 
             * @return false
             */
            public boolean equals(Object obj)
            {
                return false;
            }

            /**
             * Returns the address of the specified color of this palette as a
             * regular (not SNES) address. Note that color 0 has special uses
             * and is always rendered as transparent.
             * 
             * @param subPalette which subPalette the color is in (0-5)
             * @param c which color (0-15)
             * @return regular address of the specified color
             */
            public int getStart(int subPalette, int c)
            {
                return getStart() + (32 * subPalette) + (2 * c);
            }

            /**
             * Returns the address of the specified subPalette of this palette
             * as a regular (not SNES) address.
             * 
             * @param subPalette which subPalette the color is in (0-5)
             * @return regular address of the specified subPalette
             */
            public int getStart(int subPalette)
            {
                return getStart(subPalette, 0);
            }

            /**
             * Returns the address of this palette as a regular (not SNES)
             * address.
             * 
             * @return regular address of this palette
             */
            public int getStart()
            {
                return start;
            }

            /**
             * Returns the number of the map tileset this palette is associated
             * with.
             * 
             * @return the map tileset this palette is associated with
             */
            public int getMapTileset()
            {
                return mtileset;
            }

            /**
             * Returns the number of the map palette this palette is associated
             * with.
             * 
             * @return the map palette this palette is associated with
             */
            public int getMapPalette()
            {
                return mpalette;
            }
        }

        /**
         * Returns the requested palette.
         * 
         * @param number used to identify the palette internally
         * @return the palette
         */
        public Palette getPalette(int palette)
        {
            return (Palette) palettes.get(palette);
        }

        /**
         * Finds the internal palette number used for the specified map palette.
         * 
         * @param mtileset map tileset number
         * @param mpalette map palette number
         * @return number used to identify that palette internally
         */
        public int getPaletteNum(int mtileset, int mpalette)
        {
            for (int i = 0; i < getPaletteCount(); i++)
                if (getPalette(i).equals(mtileset, mpalette))
                    return i;
            return 0;
        }

        /**
         * Finds the internal palette number used for the specified map palette.
         * Format for the input is the map tileset number and then the map
         * palette number separated by a forward slash.
         * 
         * @param pal map palette number in the form
         *            <code>mtileset + "/" + mpalette</code>
         * @return number used to identify that palette internally
         */
        public int getPaletteNum(String pal)
        {
            String[] split = pal.split("/");
            int mtileset = Integer.parseInt(split[0]), mpalette = Integer
                .parseInt(split[1]);
            return getPaletteNum(mtileset, mpalette);
        }

        // Public funtions

        // Palete stuff

        /**
         * Adds a new palette. Should only be used by TileEditor to init the
         * palettes.
         * 
         * @param palette Palette to add.
         */
        public void addPalette(Palette palette)
        {
            this.palettes.add(palette);
        }

        /**
         * Returns the <code>Color</code> of a given color number in the
         * specified palette. If trueColors is false then the color number =
         * (red & 1) + ((green & 1) &gt;&gt; 1) + ((blue & 3) &gt;&gt; 2), which
         * is a number 0-15.
         * 
         * @param c Number of the color (0-15).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param trueColors If false, adds to the colors to identify their
         *            color number.
         * @return Color
         */
        public Color getPaletteColor(int c, int palette, int subPalette,
            boolean trueColors)
        {
            int col = hm.rom.readMulti(getPalette(palette).getStart(subPalette,
                c), 2) & 0x7fff;
            return new Color(
                ((col & 0x001f) << 3) | (!trueColors ? c & 1 : 0),
                (((col & 0x03e0) >> 5) << 3) | (!trueColors ? (c & 2) >> 1 : 0),
                ((col >> 10) << 3) | (!trueColors ? (c & 0xC) >> 2 : 0));
        }

        /**
         * Sets the specified color in the specified palette to the given new
         * color.
         * 
         * @param c Number of the color (1-15).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param col New color to set.
         */
        public void setPaletteColor(int c, int palette, int subPalette,
            Color col)
        {
            if (c < 1 || c > 15)
            {
                return;
            }

            hm.rom.writePalette(getPalette(palette).getStart(subPalette, c),
                col);
        }

        /**
         * Returns the <code>Color</code> of a given color number in the
         * specified palette.
         * 
         * @param c Number of the color (0-15).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return Color
         */
        public Color getPaletteColor(int c, int palette, int subPalette)
        {
            return getPaletteColor(c, palette, subPalette, true);
        }

        /**
         * Returns an array of all the <code>Color</code>'s in the specified
         * palette. If trueColors is false then the color number = (red & 1) +
         * ((green & 1) &gt;&gt; 1) + ((blue & 3) &gt;&gt; 2), which is a number
         * 0-15.
         * 
         * @see #getPaletteColor(int, int, int, boolean)
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param trueColors If false, adds to the colors to identify their
         *            color number.
         * @return <code>Color[]</code> of all the color's in a palette.
         */
        public Color[] getPaletteColors(int palette, int subPalette,
            boolean trueColors)
        {
            Color[] cols = new Color[16];
            for (int i = 0; i < 16; i++)
            {
                cols[i] = this.getPaletteColor(i, palette, subPalette,
                    trueColors);
            }
            return cols;
        }

        /**
         * Returns an array of all the <code>Color</code>'s in the specified
         * palette.
         * 
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return <code>Color[]</code> of all the color's in a palette.
         */
        public Color[] getPaletteColors(int palette, int subPalette)
        {
            return getPaletteColors(palette, subPalette, false);
        }

        /**
         * Returns the color number given a <code>Color</code> with the
         * specified palette.
         * 
         * @param c Color to get number for.
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return Color number of the given <code>Color</code>.
         */
        public byte getPaletteNum(Color c, int palette, int subPalette)
        {
            for (byte i = 0; i < 16; i++)
            {
                if (c == this.getPaletteColor(i, palette, subPalette))
                {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns the display name of the specified palette. This will be in
         * the form (map tileset number)/(map palette number). For example:
         * "11/2".
         * 
         * @param palette index of the palette to get the name of
         * @return palette "name" in a mtileset/mpalette <code>String</code>.
         */
        public String getPaletteName(int palette)
        {
            return getPalette(palette).toString();
        }
        
        public String getPaletteName2(int palette)
        {
            return getPalette(palette).getMapTileset() + "-" + getPalette(palette).getMapPalette();
        }

        /**
         * Returns the number palettes for this tileset. Note that this may
         * include palettes for multiple map tilesets, but only for this
         * graphics tileset.
         * 
         * @return number of palettes for this tileset
         */
        public int getPaletteCount()
        {
            return palettes.size();
        }

        // Tile stuff

        // Basics
        /**
         * Returns the number of the color of a point on a specific tile.
         * 
         * @param tile Number tile to get color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @return Number of the color (0-15).
         */
        public byte getTilePixel(int tile, int x, int y)
        {
            init();
            return this.tiles[tile][x][y];
        }

        /**
         * Sets the specified pixel to the given color number.
         * 
         * @param tile Number tile to get color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @param c Number of the color (0-15).
         */
        public void setTilePixel(int tile, int x, int y, byte c)
        {
            init();
            this.tiles[tile][x][y] = c;
            this.tilesChanged = true;
        }

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
         * Returns the <code>Color</code> of a point on a specific tile with
         * using given palette. If trueColors is false then the color number =
         * (red & 1) + ((green & 1) &gt;&gt; 1) + ((blue & 3) &gt;&gt; 2), which
         * is a number 0-15.
         * 
         * @param tile Number tile to get color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param trueColors If false, adds to the colors to identify their
         *            color number.
         * @return Color the specified pixel is.
         */
        public Color getTilePixel(int tile, int x, int y, int palette,
            int subPalette, boolean trueColors)
        {
            return this.getPaletteColor(this.getTilePixel(tile, x, y), palette,
                subPalette, trueColors);
        }

        /**
         * Returns the <code>Color</code> of a point on a specific tile with
         * using given palette.
         * 
         * @param tile Number tile to get color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return Color the specified pixel is.
         */
        public Color getTilePixel(int tile, int x, int y, int palette,
            int subPalette)
        {
            return getTilePixel(tile, x, y, palette, subPalette, true);
        }

        /**
         * Sets the specified pixel to the specified color. Returns true if the
         * color is found in the palette. Returns false and does not change tile
         * if the color is not found.
         * 
         * @param tile Number tile to set color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param col <code>Color</code> to set pixel to.
         * @return True if change made, false if color not in palette.
         */
        public boolean setTilePixel(int tile, int x, int y, int palette,
            int subPalette, Color col)
        {
            byte c;
            if ((c = this.getPaletteNum(col, palette, subPalette)) != -1)
            {
                this.setTilePixel(tile, x, y, c);
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Sets the specified pixel to the specified <b>false </b> color. This
         * only works if the color was gotten using a false "trueColor" argument
         * on {@link #getPaletteColor(int, int, int, boolean)},
         * {@link #getTilePixel(int, int, int, int, int, boolean)},
         * {@link #getTileImage(int, int, int, boolean, boolean, boolean)}, or
         * {@link #getTileImage(int, int, int, boolean)}.
         * 
         * @param tile Number tile to set color for (0-1023).
         * @param x X-coordinate on the tile (0-7).
         * @param y Y-coordinate on the tile (0-7).
         * @param col <b>False </b> <code>Color</code> to set pixel to.
         */
        public void setTilePixel(int tile, int x, int y, Color col)
        {
            byte c = 0;
            c |= col.getRed() & 1;
            c |= (col.getGreen() & 1) << 1;
            c |= (col.getBlue() & 3) << 2;
            setTilePixel(tile, x, y, c);
        }

        // Image
        /**
         * Returns an image of the specified tile using the specified palette.
         * If trueColors is false then the color number = (red & 1) + ((green &
         * 1) &gt;&gt; 1) + ((blue & 3) &gt;&gt; 2), which is a number 0-15.
         * 
         * @param tile Number tile (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param hFlip If true, flip output horizontally.
         * @param vFlip If true, flip output vertically.
         * @param trueColors If false, adds to the colors to identify their
         *            color number.
         * @return Image of the tile (8x8).
         */
        public Image getTileImage(int tile, int palette, int subPalette,
            boolean hFlip, boolean vFlip, boolean trueColors)
        {
            BufferedImage out = new BufferedImage(8, 8,
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            for (int x = 0; x < 8; x++)
            {
                for (int y = 0; y < 8; y++)
                {
                    g.setColor(this.getTilePixel(tile, (hFlip ? 7 - x : x),
                        (vFlip ? 7 - y : y), palette, subPalette, trueColors));
                    g.drawLine(x, y, x, y);
                    // there's no draw point, WHY?!?
                }
            }
            return out;
        }

        /**
         * Returns an image of the specified tile using the specified palette.
         * 
         * @param tile Number tile (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param hFlip If true, flip output horizontally.
         * @param vFlip If true, flip output vertically.
         * @return Image of the tile (8x8).
         */
        public Image getTileImage(int tile, int palette, int subPalette,
            boolean hFlip, boolean vFlip)
        {
            return getTileImage(tile, palette, subPalette, hFlip, vFlip, true);
        }

        /**
         * Returns an image of the specified tile using the specified palette.
         * If trueColors is false then the color number = (red & 1) + ((green &
         * 1) &gt;&gt; 1) + ((blue & 3) &gt;&gt; 2), which is a number 0-15.
         * 
         * @see #getTileImage(int, int, int, boolean, boolean, boolean)
         * @param tile Number tile (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param trueColors If false, adds to the colors to identify their
         *            color number.
         * @return Image of the tile (8x8).
         */
        public Image getTileImage(int tile, int palette, int subPalette,
            boolean trueColors)
        {
            return this.getTileImage(tile, palette, subPalette, false, false,
                trueColors);
        }

        /**
         * Returns an image of the specified tile using the specified palette.
         * 
         * @see #getTileImage(int, int, int, boolean, boolean, boolean)
         * @param tile Number tile (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return Image of the tile (8x8).
         */
        public Image getTileImage(int tile, int palette, int subPalette)
        {
            return this.getTileImage(tile, palette, subPalette, true);
        }

        /**
         * Returns an image of the specified tile using the specified palette.
         * 
         * @param tile Number tile (0-1023).
         * @param palette <code>Color[][]</code> of special palette to use
         * @param subPalette Number of the subpalette to use (0-5).
         * @param hFlip If true, flip output horizontally.
         * @param vFlip If true, flip output vertically.
         * @return Image of the tile (8x8).
         * @see #getTileImage(int, int, int, boolean, boolean, boolean)
         */
        public Image getTileImage(int tile, Color[][] palette, int subPalette,
            boolean hFlip, boolean vFlip)
        {
            if (subPalette < 0)
                subPalette += 2;
            return HackModule.drawImage(getTile(tile), palette[subPalette],
                hFlip, vFlip);
        }

        /**
         * Returns the contents of a tile as a String. Each character is the hex
         * value of the pixel and rows are separated by "\n"'s (newlines).
         * 
         * @see #setTileAsString(int, String)
         * @param tile Which tile to get values for.
         * @return Tile values as a String.
         */
        public String getTileAsString(int tile)
        {
            String out = new String();

            for (int y = 0; y < 8; y++)
            {
                for (int x = 0; x < 8; x++)
                {
                    out += Integer
                        .toHexString(this.getTilePixel(tile, x, y) & 0xff);
                }
            }

            return out;
        }

        /**
         * Sets specified tile based on the given String.
         * 
         * @see #getTileAsString(int)
         * @param tile Which tile to set the values of.
         * @param in String of tile graphics.
         */
        public void setTileAsString(int tile, String in)
        {
            for (int y = 0; y < 8; y++)
            {
                for (int x = 0; x < 8; x++)
                {
                    this.setTilePixel(tile, x, y, (byte) Integer.parseInt(in
                        .substring((y * 8) + x, (y * 8) + x + 1), 16));
                }
            }
        }

        /**
         * Returns the tileset graphics as a String. Each character is the hex
         * value of the pixel and rows are separated by "\n"'s (newlines). There
         * is a blank row to separate tiles. Tiles are in order: 0, 512, 1, 513,
         * etc. (background0, foreground0, background1, foreground1, etc.).
         * 
         * @see #setTilesetAsString(String)
         * @return Tileset graphics values in a String.
         */
        public String getTilesetAsString()
        {
            String out = new String();

            for (int tile = 0; tile < 512; tile++)
            {
                out += (tile != 0 ? "\n\n" : "") + this.getTileAsString(tile)
                    + "\n" + this.getTileAsString(tile ^ 512);
            }

            return out;
        }

        /**
         * Sets tileset graphics based on the given String.
         * 
         * @see #getTilesetAsString()
         * @param in String of tileset graphics.
         */
        public void setTilesetAsString(String in)
        {
            String[] tilesCsv = in.split("\n\n");
            for (int tile = 0; tile < tilesCsv.length; tile++)
            {
                String[] tmp = tilesCsv[tile].split("\n");
                setTileAsString(tile, tmp[0]);
                setTileAsString(tile ^ 512, tmp[1]);
            }
        }

        /**
         * Returns a String containing the specified subPalette. Format: Each of
         * 16 colors: red, green, blue each 5 bit values in base 32 (1 character
         * each).
         * 
         * @see #setSubPaletteAsString(int, int, String)
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return A 48 character long String containing the specified
         *         subPalette.
         */
        public String getSubPaletteAsString(int palette, int subPalette)
        {
            String out = new String();
            for (int i = 0; i < 16; i++)
            {
                Color tmp = this.getPaletteColor(i, palette, subPalette);
                out += Integer.toString(tmp.getRed() >> 3, 32);
                out += Integer.toString(tmp.getGreen() >> 3, 32);
                out += Integer.toString(tmp.getBlue() >> 3, 32);
            }
            return out;
        }

        /**
         * Sets the specified subPalette according to the given String.
         * 
         * @see #getSubPaletteAsString(int, int)
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param in A 48 character long String containing the specified
         *            subPalette.
         */
        public void setSubPaletteAsString(int palette, int subPalette, String in)
        {
            for (int i = 0; i < 16; i++)
            {
                this.setPaletteColor(i, palette, subPalette,
                    new Color(Integer.parseInt(in.substring(i * 3, i * 3 + 1),
                        32) << 3, Integer.parseInt(in.substring(i * 3 + 1,
                        i * 3 + 2), 32) << 3, Integer.parseInt(in.substring(
                        i * 3 + 2, i * 3 + 3), 32) << 3));
            }
        }

        /**
         * Returns the specified palette as a String. Format: mtileset in base
         * 32, mpalette in base 32, 6 {@link #getSubPaletteAsString(int, int)}
         * 's.
         * 
         * @see #getSubPaletteAsString(int, int)
         * @see #setPaletteAsString(String)
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @return A 290 character String containing the specified palette.
         */
        public String getPaletteAsString(int palette)
        {
            String out = new String();

            out += Integer.toString(getPalette(palette).getMapTileset(), 32);
            out += Integer.toString(getPalette(palette).getMapPalette(), 32);
            for (int i = 0; i < 6; i++)
                out += getSubPaletteAsString(palette, i);

            return out;
        }

        /**
         * Sets the specified palette to the values in the String.
         * 
         * @see #setPaletteAsString(String)
         * @param pal A 290 character String containing the specified palette.
         * @param palette which palette number to set
         */
        public void setPaletteAsString(String pal, int palette)
        {
            for (int i = 0; i < 6; i++)
                this.setSubPaletteAsString(palette, i, pal.substring(
                    2 + i * 48, 50 + i * 48));
        }

        /**
         * Sets the palette specified in the String to the values in the String.
         * 
         * @see #getPaletteAsString(int)
         * @param pal A 290 character String containing the specified palette.
         */
        public void setPaletteAsString(String pal)
        {
            int palette = this.getPaletteNum(Integer.parseInt(pal.substring(0,
                1), 32)
                + "/" + Integer.parseInt(pal.substring(1, 2), 32));
            setPaletteAsString(pal, palette);
        }

        /**
         * Returns all palettes in a single String separated by newlines.
         * 
         * @see #getPaletteAsString(int)
         * @see #setPalettesAsString(String)
         * @return All palettes in a single String separated by newlines.
         */
        public String getPalettesAsString()
        {
            String out = new String();
            for (int i = 0; i < getPaletteCount(); i++)
            {
                out += (i != 0 ? "\n" : "") + this.getPaletteAsString(i);
            }
            return out;
        }

        /**
         * Sets all palettes based on the given String.
         * 
         * @see #getPalettesAsString()
         * @param pal All palettes in a single String separated by newlines.
         */
        public void setPalettesAsString(String pal)
        {
            String[] pals = pal.split("\n");
            for (int i = 0; i < pals.length; i++)
                this.setPaletteAsString(pals[i]);
        }

        /**
         * Returns specified arrangement as a string with collision data.
         * Format: Reading across, for each position: 4 chars of arrangement
         * data in hex, 2 chars of collision data in hex. Total 6 chars each for
         * 16 positions = 92 chars.
         * 
         * @see #setArrangementAsString(int, String)
         * @param arrangement Which arrangement (0-1023).
         * @return A 92 character String containing arrangement and collision
         *         data.
         */
        public String getArrangementAsString(int arrangement)
        {
            String out = new String();
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    out += HackModule.addZeros(Integer.toHexString(this
                        .getArrangementData(arrangement, x, y) & 0xffff), 4);
                    out += HackModule.addZeros(Integer.toHexString(this
                        .getCollisionData(arrangement, x, y) & 0xff), 2);
                }
            }
            return out;
        }

        /**
         * Sets the specified arrangement and collision data based on the given
         * String.
         * 
         * @see #getArrangementAsString(int)
         * @param arrangement Which arrangement (0-1023).
         * @param arr A 92 character String containing arrangement and collision
         *            data.
         */
        public void setArrangementAsString(int arrangement, String arr)
        {
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    this.setArrangementData(arrangement, x, y, (short) Integer
                        .parseInt(arr.substring((y * 4 + x) * 6,
                            (y * 4 + x) * 6 + 4), 16));
                    this.setCollisionData(arrangement, x, y, (byte) Integer
                        .parseInt(arr.substring((y * 4 + x) * 6 + 4,
                            (y * 4 + x) * 6 + 6), 16));
                }
            }
        }

        /**
         * Returns a String containing all arrangement and collision data for
         * this tileset.
         * 
         * @see #getArrangementAsString(int)
         * @see #setArrangementsAsString(String)
         * @return All arrangement & collision data in a single String separated
         *         by newlines.
         */
        public String getArrangementsAsString()
        {
            String out = new String();
            for (int i = 0; i < 1024; i++)
            {
                out += (i == 0 ? "" : "\n") + this.getArrangementAsString(i);
            }
            return out;
        }

        /**
         * Sets all arrangement and collision data for this tileset based on the
         * given String.
         * 
         * @see #getArrangementsAsString()
         * @param arr All arrangement & collision data in a single String
         *            separated by newlines.
         */
        public void setArrangementsAsString(String arr)
        {
            String[] arrs = arr.split("\n");
            for (int i = 0; i < arrs.length; i++)
                this.setArrangementAsString(i, arrs[i]);
        }

        /**
         * Returns specified arrangement's collision data as a string. Format:
         * Reading across, for each position: 2 chars of collision data in hex.
         * 
         * @see #setCollisionAsString(int, String)
         * @param arrangement Which arrangement (0-1023).
         * @return A 32 character String containing collision data.
         */
        public String getCollisionAsString(int arrangement)
        {
            String out = new String();
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    out += HackModule.addZeros(Integer.toHexString(this
                        .getCollisionData(arrangement, x, y) & 0xff), 2);
                }
            }
            return out;
        }

        /**
         * Sets the specified collision data based on the given String.
         * 
         * @see #getCollisionAsString(int)
         * @param arrangement Which arrangement (0-1023).
         * @param arr A 32 character String containing arrangement and collision
         *            data.
         */
        public void setCollisionAsString(int arrangement, String arr)
        {
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    this.setCollisionData(arrangement, x, y, (byte) Integer
                        .parseInt(arr.substring((y * 4 + x) * 2,
                            (y * 4 + x) * 2 + 2), 16));
                }
            }
        }

        /**
         * Returns a String containing all collision data for this tileset.
         * 
         * @see #getCollisionAsString(int)
         * @see #setCollisionsAsString(String)
         * @return All collision data in a single String separated by newlines.
         */
        public String getCollisionsAsString()
        {
            String out = new String();
            for (int i = 0; i < 1024; i++)
            {
                out += (i != 0 ? "\n" : "") + this.getCollisionAsString(i);
            }
            return out;
        }

        /**
         * Sets all collision data for this tileset based on the given String.
         * 
         * @see #getCollisionsAsString()
         * @param arr All collision data in a single String separated by
         *            newlines.
         */
        public void setCollisionsAsString(String arr)
        {
            String[] arrs = arr.split("\n");
            for (int i = 0; i < arrs.length; i++)
                this.setCollisionAsString(i, arrs[i]);
        }

        /**
         * Returns all tileset data (graphics, palettes, arrangements,
         * collision) in a single String. Format: All tile graphics, 3 newlines,
         * all palette info, 3 newlines, all arrangement/collision info.
         * 
         * @see #getTilesetAsString()
         * @see #getPalettesAsString()
         * @see #getArrangementsAsString()
         * @see #setAllDataAsString(String)
         * @return All tileset data in a single String.
         */
        public String getAllDataAsString()
        {
            return this.getTilesetAsString() + "\n\n\n"
                + this.getPalettesAsString() + "\n\n\n"
                + this.getArrangementsAsString();
        }

        /**
         * Sets all tileset data (graphics, palettes, arrangements, collision)
         * based on the given String.
         * 
         * @see #setTilesetAsString(String)
         * @see #setPalettesAsString(String)
         * @see #setArrangementsAsString(String)
         * @see #getAllDataAsString()
         * @param all All tileset data in a single String.
         */
        public void setAllDataAsString(String all)
        {
            String[] tmp = all.split("\n\n\n");
            this.setTilesetAsString(tmp[0]);
            this.setPalettesAsString(tmp[1]);
            this.setArrangementsAsString(tmp[2]);
        }

        /**
         * Sets the specified tile to the given image. Requires exact color
         * matches.
         * 
         * @see #setTilePixel(int, int, int, int, int, Color)
         * @param tile Number tile (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param in Image of the tile (8x8).
         */
        public void setTileImage(int tile, int palette, int subPalette, Image in)
        {
            BufferedImage img = (BufferedImage) in;
            int w = img.getWidth(), h = img.getHeight();
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
            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    int pixel = pixels[y * w + x];
                    int alpha = (pixel >> 24) & 0xff;
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = (pixel) & 0xff;
                    Color col = new Color(red, green, blue, alpha);

                    this.setTilePixel(tile, x, y, palette, subPalette, col);
                }
            }
        }

        /**
         * Sets the specified tile to the given image. Requires use of false
         * colors in making of image. Made by
         * {@link #getTileImage(int, int, int, boolean, boolean, boolean)}, or
         * {@link #getTileImage(int, int, int, boolean)}.
         * 
         * @see #setTilePixel(int, int, int, Color)
         * @param tile Number tile (0-1023).
         * @param in Image of the tile (8x8).
         */
        public void setTileImage(int tile, Image in)
        {
            BufferedImage img = (BufferedImage) in;
            int w = img.getWidth(), h = img.getHeight();
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
            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    int pixel = pixels[y * w + x];
                    int alpha = (pixel >> 24) & 0xff;
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = (pixel) & 0xff;
                    Color col = new Color(red, green, blue, alpha);

                    this.setTilePixel(tile, x, y, col);
                }
            }
        }

        /**
         * Returns an image of every tile in the tileset together. Only tiles
         * 0-511, since the others are for transparent foregrounds.
         * 
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @param highlightTile Tile to highlight.
         * @return Image of the tileset (8*32 x 8*16)
         */
        public Image getTilesetImage(int palette, int subPalette,
            int highlightTile)
        {
            BufferedImage out = new BufferedImage(8 * 32, 8 * 16,
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            for (int i = 0; i < 512; i++)
            {
                g.drawImage(this.getTileImage(i, palette, subPalette),
                    (i % 32) * 8, (i / 32) * 8, null);
            }
            if (highlightTile >= 0 && highlightTile <= 511)
            {
                g.setColor(new Color(255, 255, 0, 128));
                g.fillRect((highlightTile % 32) * 8, (highlightTile / 32) * 8,
                    8, 8);
            }
            return out;
        }

        /**
         * Returns an image of every tile in the tileset together. Only tiles
         * 0-511, since the others are for transparent foregrounds.
         * 
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param subPalette Number of the subpalette to use (0-5).
         * @return Image of the tileset (8*32 x 8*16)
         */
        public Image getTilesetImage(int palette, int subPalette)
        {
            return getTilesetImage(palette, subPalette, -1);
        }

        // Arrangement stuff

        // Basics
        /**
         * Returns an arrangement number based on the input. Note that is does
         * not actually write to number to anything, it just returns it. <br>
         * <br>
         * The arrangement format is binary: <br>
         * <code>VH?S SS?T  TTTT TTTT</code><br>
         * <br>
         * V = vertical flip flag (1 = flip) <br>
         * H = horizonal flip flag (1 = flip) <br>
         * S = sub-palette + 2 (2-7) <br>
         * T = tile number (0-511), upper 512 tiles can be transparent
         * foregrounds for the lower tiles depending on collision byte.
         * 
         * @param tile Number tile (0-511).
         * @param subPalette Number of the subpalette to use (0-5).
         * @param hFlip If true, tile is flipped horizontally in arrangement.
         * @param vFlip If true, tile is flipped vertically in arrangement.
         * @return Number to be stored in arrangement with given information.
         */
        public static int makeArrangementNumber(int tile, int subPalette,
            boolean hFlip, boolean vFlip)
        {
            return (tile & 0x01ff) | (((subPalette + 2) & 7) << 10)
                | (hFlip ? 0x4000 : 0) | (vFlip ? 0x8000 : 0);
        }

        /**
         * Returns the number of used arrangements. Arrangements numbered less
         * the return value probably have data.
         * 
         * @return number of arrangements with data
         */
        public int getArrangementCount()
        {
            init();
            for (int j = 1024; j > 0; j--)
                for (int x = 0; x < 4; x++)
                    for (int y = 0; y < 4; y++)
                        if (this.arrangements[j - 1][x][y] != 0)
                            return j;
            return 0;
        }

        /**
         * Returns which tile is at the specified position in the specified
         * arrangement. The number contains more than just the tile, see
         * {@link #makeArrangementNumber(int, int, boolean, boolean)}for more
         * information.
         * 
         * @see #makeArrangementNumber(int, int, boolean, boolean)
         * @param arrangement Which arrangement (0-1023).
         * @param x X-coordinate on the arrangement (0-3).
         * @param y Y-coordinate on the arrangement (0-3).
         * @return Which tile & other information.
         */
        public short getArrangementData(int arrangement, int x, int y)
        {
            init();
            return this.arrangements[arrangement][x][y];
        }

        /**
         * Sets which tile is at the specified position in the specified
         * arrangement. The data number contains more than just the tile, see
         * {@link #makeArrangementNumber(int, int, boolean, boolean)}for more
         * information.
         * 
         * @see #makeArrangementNumber(int, int, boolean, boolean)
         * @param arrangement Which arrangement (0-1023).
         * @param x X-coordinate on the arrangement (0-3).
         * @param y Y-coordinate on the arrangement (0-3).
         * @param data Which tile & other information.
         */
        public void setArrangementData(int arrangement, int x, int y, short data)
        {
            init();
            this.arrangements[arrangement][x][y] = data;
            this.arrangementsChanged = true;
        }

        // Array stuff
        /**
         * Returns an int array of the tiles in the specified arrangement. The
         * number contains more than just the tile, see
         * {@link #makeArrangementNumber(int, int, boolean, boolean)}for more
         * information.
         * 
         * @see #makeArrangementNumber(int, int, boolean, boolean)
         * @param arrangement Which arrangement (0-1023).
         * @return int[4][4] of which tile is at the each position & other info.
         */
        public short[][] getArrangementData(int arrangement)
        {
            // Make a new array so you don't have to worry about pointer stuff
            short[][] out = new short[4][4];
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    out[x][y] = this.getArrangementData(arrangement, x, y);
                }
            }
            return out;
        }

        /**
         * Sets arrangement data based on the data in the given int array. The
         * number contains more than just the tile, see
         * {@link #makeArrangementNumber(int, int, boolean, boolean)}for more
         * information.
         * 
         * @see #makeArrangementNumber(int, int, boolean, boolean)
         * @param arrangement Which arrangement (0-1023).
         * @param data int[4][4] of which tile is at the each position & other
         *            info.
         */
        public void setArrangementData(int arrangement, short[][] data)
        {
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    this.setArrangementData(arrangement, x, y, data[x][y]);
                }
            }
        }

        // Image stuff
        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param selection <code>int[4][4]</code> where -1 means not selected
         *            and other values indicate which tile
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         */
        public Image getArrangementImage(int arrangement, int palette,
            int[][] selection, float zoom, boolean gridLines)
        {
            BufferedImage out = new BufferedImage((gridLines ? 3 : 0)
                + (int) (32 * zoom), (gridLines ? 3 : 0) + (int) (32 * zoom),
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            init();
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    int tile = (selection[x][y] == -1
                        ? this.arrangements[arrangement][x][y]
                        : selection[x][y]);
                    g.drawImage(getTileImage(tile & 0x01ff, palette,
                        ((tile & 0x1C00) >> 10) - 2, (tile & 0x4000) != 0,
                        (tile & 0x8000) != 0), (int) (x * 8 * zoom)
                        + (gridLines ? x : 0), (int) (y * 8 * zoom)
                        + (gridLines ? y : 0), (int) (8 * zoom),
                        (int) (8 * zoom), null);
                    if (selection[x][y] != -1)
                    {
                        g.setColor(new Color(255, 255, 0, 128));
                        g.fillRect((int) (x * 8 * zoom) + (gridLines ? x : 0),
                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
                            (int) (8 * zoom), (int) (8 * zoom));
                    }
                }
            }
            return out;
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         */
        public Image getArrangementImage(int arrangement, int palette,
            float zoom, boolean gridLines)
        {
            init();
            BufferedImage out = new BufferedImage((gridLines ? 3 : 0)
                + (int) (32 * zoom), (gridLines ? 3 : 0) + (int) (32 * zoom),
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    g
                        .drawImage(
                            getTileImage(
                                this.arrangements[arrangement][x][y] & 0x01ff,
                                palette,
                                ((this.arrangements[arrangement][x][y] & 0x1C00) >> 10) - 2,
                                (this.arrangements[arrangement][x][y] & 0x4000) != 0,
                                (this.arrangements[arrangement][x][y] & 0x8000) != 0),
                            (int) (x * 8 * zoom) + (gridLines ? x : 0),
                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
                            (int) (8 * zoom), (int) (8 * zoom), null);
                }
            }
            return out;
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         */
        public Image getArrangementImage(int arrangement, int palette,
            boolean gridLines)
        {
            return getArrangementImage(arrangement, palette, 1, gridLines);
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @return Image of the arrangement using the given palette (32x32).
         */
        public Image getArrangementImage(int arrangement, int palette)
        {
            return getArrangementImage(arrangement, palette, false);
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette <code>Color[][]</code> of specical palette to use.
         * @param selection <code>int[4][4]</code> where -1 means not selected
         *            and other values indicate which tile
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         * @see #getArrangementImage(int, int, int[][], float, boolean)
         */
        public Image getArrangementImage(int arrangement, Color[][] palette,
            int[][] selection, float zoom, boolean gridLines)
        {
            BufferedImage out = new BufferedImage((gridLines ? 3 : 0)
                + (int) (32 * zoom), (gridLines ? 3 : 0) + (int) (32 * zoom),
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            init();
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    int tile = (selection[x][y] == -1
                        ? this.arrangements[arrangement][x][y]
                        : selection[x][y]);
                    g
                        .drawImage(
                            getTileImage(
                                tile & 0x01ff,
                                palette,
                                ((this.arrangements[arrangement][x][y] & 0x1C00) >> 10) - 2,
                                (tile & 0x4000) != 0, (tile & 0x8000) != 0),
                            (int) (x * 8 * zoom) + (gridLines ? x : 0),
                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
                            (int) (8 * zoom), (int) (8 * zoom), null);
                    if (selection[x][y] != -1)
                    {
                        g.setColor(new Color(255, 255, 0, 128));
                        g.fillRect((int) (x * 8 * zoom) + (gridLines ? x : 0),
                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
                            (int) (8 * zoom), (int) (8 * zoom));
                    }
                }
            }
            return out;
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette <code>Color[][]</code> of specical palette to use.
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         */
        public Image getArrangementImage(int arrangement, Color[][] palette,
            float zoom, boolean gridLines)
        {
            init();
            BufferedImage out = new BufferedImage((gridLines ? 3 : 0)
                + (int) (32 * zoom), (gridLines ? 3 : 0) + (int) (32 * zoom),
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    g
                        .drawImage(
                            getTileImage(
                                this.arrangements[arrangement][x][y] & 0x01ff,
                                palette,
                                ((this.arrangements[arrangement][x][y] & 0x1C00) >> 10) - 2,
                                (this.arrangements[arrangement][x][y] & 0x4000) != 0,
                                (this.arrangements[arrangement][x][y] & 0x8000) != 0),
                            (int) (x * 8 * zoom) + (gridLines ? x : 0),
                            (int) (y * 8 * zoom) + (gridLines ? y : 0),
                            (int) (8 * zoom), (int) (8 * zoom), null);
                }
            }
            return out;
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette <code>Color[][]</code> of specical palette to use.
         * @param gridLines If true, minitiles are spaced out to put gridlines
         *            between them
         * @return Image of the arrangement using the given palette (32x32 or
         *         35x35).
         */
        public Image getArrangementImage(int arrangement, Color[][] palette,
            boolean gridLines)
        {
            return getArrangementImage(arrangement, palette, 1, gridLines);
        }

        /**
         * Returns an image of the specified arrangement using the specified
         * palette. Note that sub-palette is specfied by the arrangement data.
         * 
         * @param arrangement Which arrangement (0-1023).
         * @param palette <code>Color[][]</code> of specical palette to use.
         * @return Image of the arrangement using the given palette (32x32).
         */
        public Image getArrangementImage(int arrangement, Color[][] palette)
        {
            return getArrangementImage(arrangement, palette, false);
        }

        /**
         * Returns an image of multiple arrangments.
         * 
         * @see #getArrangementsImage(int, int, int, int)
         * @param start Arrangement to draw in top-left (0-1023).
         * @param width Number of arrangments wide image should be.
         * @param height Number of arrangements high image should be.
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param bg Background color for lines between arrangements.
         * @param hightlightedArrangement If this arrangement is drawn, it will
         *            be highlighted in yellow.
         * @return Image of specified arrangements ((33*width)-1 x
         *         (33*height)-1).
         */
        public Image getArrangementsImage(int start, int width, int height,
            int palette, Color bg, int hightlightedArrangement)
        {
            BufferedImage out = new BufferedImage((33 * width) - 1,
                (33 * height) - 1, BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            g.setColor(bg);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            for (int x = 0; x < width; x++)
            {
                for (int y = 0; y < height; y++)
                {
                    int arr = start + y + (x * height);
                    if (arr <= 1023 && arr >= 0)
                    {
                        g.drawImage(getArrangementImage(arr, palette), x * 33,
                            y * 33, null);
                        // if (arr == hightlightedArrangement)
                        // {
                        // g.setColor(new Color(255, 255, 0, 128));
                        // g.fillRect(x * 33, y * 33, 32, 32);
                        // }
                    }
                }
            }
            if (hightlightedArrangement >= start
                && hightlightedArrangement >= 0
                && hightlightedArrangement < start + (width * height)
                && hightlightedArrangement < 1024)
            {
                g.setColor(new Color(255, 255, 0, 128));
                g.fillRect(((hightlightedArrangement - start) / height) * 33,
                    ((hightlightedArrangement - start) % height) * 33, 32, 32);
            }
            return out;
        }

        /**
         * Returns an image of multiple arrangments.
         * 
         * @see #getArrangementsImage(int, int, int, int)
         * @param start Arrangement to draw in top-left (0-1023).
         * @param width Number of arrangments wide image should be.
         * @param height Number of arrangements high image should be.
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @param bg Background color for lines between arrangements.
         * @return Image of specified arrangements ((33*width)-1 x
         *         (33*height)-1).
         */
        public Image getArrangementsImage(int start, int width, int height,
            int palette, Color bg)
        {
            return this.getArrangementsImage(start, width, height, palette, bg,
                -1);
        }

        /**
         * Returns an image of multiple arrangments with a black background.
         * 
         * @see #getArrangementsImage(int, int, int, int, Color)
         * @param start Arrangement to draw in top-left (0-1023).
         * @param width Number of arrangments wide image should be.
         * @param height Number of arrangements high image should be.
         * @param palette Number of the palette to use (0-59). Note that there
         *            probably are not 60 palettes.
         * @return Image of specified arrangements ((33*width)-1 x
         *         (33*height)-1).
         */
        public Image getArrangementsImage(int start, int width, int height,
            int palette)
        {
            return this.getArrangementsImage(start, width, height, palette,
                Color.BLACK);
        }

        // Collision stuff

        // basics
        /**
         * Returns the collision byte for the specified position in the
         * specified arrangement. Little is known about the collision byte, look
         * for the Tile Editor topic on the forums for information. Any
         * discoveries will be helpful! :)<br>
         * <br>
         * Here is what BlueAntoid has to say about the collision byte (posted
         * 2003-04-07 00:05): <br>
         * By the way, this data is stored as binary, so here are the functions
         * of each bit: <br>
         * <br>[ 80 40 20 10 | 08 04 02 01 ]<br>
         * <br>
         * 80 - Collide (solid) <br>
         * 40 - Unknown? <br>
         * 20 - Unknown? <br>
         * 10 - Activate doors on contact <br>
         * <br>
         * The 08, 04, and 02 bits seem to be some sort of combinant group, not
         * simply the sum of the individual effects. Here are the combinations:
         * <br>
         * <br>
         * [---|000-] (00) - No effect <br>
         * [---|001-] (02) - Unknown? <br>
         * [---|010-] (04) - Sweating and sunstroke <br>
         * [---|011-] (06) - Unknown? <br>
         * [---|100-] (08) - Shallow water <br>
         * [---|101-] (0A) - Unknown? <br>
         * [---|110-] (0C) - Deep water <br>
         * [---|111-] (0E) - Unknown? <br>
         * <br>
         * 01 - Activate Layer-2 graphics (Overhead/floating)
         * 
         * @param arrangement Which arrangement to get collision data on
         *            (0-1023).
         * @param x X-coordinate on the arrangement (0-3).
         * @param y Y-coordinate on the arrangement (0-3).
         * @return Collision byte (0-255).
         */
        public byte getCollisionData(int arrangement, int x, int y)
        {
            init();
            return this.collision[arrangement][x][y];
        }

        /**
         * Sets the collision byte for the specified position in the specified
         * arrangement. Little is known about the collision byte, look for the
         * Tile Editor topic on the forums for information. Any discoveries will
         * be helpful! :)
         * 
         * @see #getCollisionData(int, int, int)
         * @param arrangement Which arrangement to set collision data of
         *            (0-1023).
         * @param x X-coordinate on the arrangement (0-3).
         * @param y Y-coordinate on the arrangement (0-3).
         * @param collision Collision byte (0-255).
         */
        public void setCollisionData(int arrangement, int x, int y,
            byte collision)
        {
            init();
            this.collision[arrangement][x][y] = collision;
            this.collisionChanged = true;
        }

        // Array stuff
        /**
         * Returns an array of the collision bytes for the specified
         * arrangement.
         * 
         * @see #getCollisionData(int, int, int)
         * @param arrangement Which arrangement to get collision data on
         *            (0-1023).
         * @return An int[4][4] of collison bytes (0-255).
         */
        public byte[][] getCollisionData(int arrangement)
        {
            // Make a new array so you don't have to worry about pointer stuff
            byte[][] out = new byte[4][4];
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    out[x][y] = this.getCollisionData(arrangement, x, y);
                }
            }
            return out;
        }

        /**
         * Returns an array of the collision bytes for the specified
         * arrangement.
         * 
         * @see #getCollisionData(int, int, int)
         * @see #getCollisionData(int)
         * @param arrangement Which arrangement to get collision data on
         *            (0-1023).
         * @return A byte[16] of collison bytes (0-255).
         */
        public byte[] getCollisionDataFlat(int arrangement)
        {
            // Make a new array so you don't have to worry about pointer stuff
            byte out[] = new byte[16], i = 0;

            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    out[i++] = this.getCollisionData(arrangement, x, y);
                }
            }
            return out;
        }

        /**
         * Sets the collision bytes for the specified arrangement to the values
         * in the given array.
         * 
         * @see #getCollisionData(int, int, int)
         * @see #setCollisionData(int, int, int, int)
         * @param arrangement Which arrangement to set collision data of
         *            (0-1023).
         * @param collision An int[4][4] of collison bytes (0-255).
         */
        public void setCollisionData(int arrangement, byte[][] collision)
        {
            for (int x = 0; x < 4; x++)
            {
                for (int y = 0; y < 4; y++)
                {
                    this.setCollisionData(arrangement, x, y, collision[x][y]);
                }
            }
        }

        private byte[] interlaceTile(byte[][] tile)
        {
            byte[] out = new byte[32];
            HackModule.write4BPPArea(tile, out, 0, 0, 0);
            return out;
        }

        /**
         * Writes the tiles held by this to the specified location and returns
         * number of bytes written. Do next write at
         * <code>offset + return value</code>.
         * 
         * @see #writeInfo(int)
         * @param offset Where to start writting.
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeTiles(int offset)
        {
            hm.rom.write(0x2F125B + (num * 4), HackModule
                .toSnesPointer(this.tileAddress = offset), 4);

            byte[] buffer = new byte[32 * 1024], tmpTile;
            int i = 0;

            init();
            for (int t = 0; t < tiles.length; t++)
            {
                tmpTile = this.interlaceTile(this.tiles[t]);
                for (int y = 0; y < tmpTile.length; y++)
                {
                    try
                    {
                        buffer[i++] = tmpTile[y];
                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {}
                }
            }

            byte[] compTileset; // , compTilesetTv;
            int compTileLen = comp(buffer, compTileset = new byte[65536], 28673);
            if (compTileLen + offset - 1 > hm.rom.length())
                return -1;
            // compTileLenTv =
            // TileViewer.Tileset.comp(
            // buffer,
            // compTilesetTv = new int[65536],
            // i);
            // if (compTileLen != compTileLenTv)
            // {
            // System.out.println(
            // "Compressed tiles different lengths: "
            // + compTileLen
            // + " Tv: "
            // + compTileLenTv);
            // }
            // else
            // {
            // for (int j = 0; j < compTileLen; j++)
            // {
            // if (compTileset[j] != compTilesetTv[j])
            // {
            // System.out.println(
            // "Different byte at "
            // + j
            // + " ("
            // + Integer.toHexString(compTileset[j])
            // + " / tv: "
            // + Integer.toHexString(compTilesetTv[j])
            // + ")");
            // }
            // }
            // }
            hm.rom.write(this.tileAddress, compTileset, compTileLen);
            System.out
                .println("Wrote " + compTileLen + " bytes of tileset #" + num
                    + " tiles at " + Integer.toHexString(this.tileAddress)
                    + " to "
                    + Integer.toHexString(this.tileAddress + compTileLen - 1)
                    + ".");
            return compTileLen;
        }

        /**
         * Writes the arrangements held by this to the specified location and
         * returns number of bytes written. Do next write at
         * <code>offset + return value</code>.
         * 
         * @see #writeInfo(int)
         * @param offset Where to start writting.
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeArrangements(int offset)
        {
            init();
            hm.rom.write(0x2F12AB + (num * 4), HackModule
                .toSnesPointer(this.arrangmentsAddress = offset), 4);

            byte[] arrBuffer = new byte[1024 * 32];
            int a = 0;

            for (int x = 0; x < this.getArrangementCount(); x++)
            {
                for (int y = 0; y < arrangements[0].length; y++)
                {
                    for (int z = 0; z < arrangements[0][0].length; z++)
                    {

                        try
                        {
                            arrBuffer[a++] = (byte) (arrangements[x][z][y] & 255);
                            arrBuffer[a++] = (byte) (arrangements[x][z][y] >> 8);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {}
                    }
                }
            }

            byte[] compArr;
            int compArrLen = comp(arrBuffer, compArr = new byte[65536], a);
            if (compArrLen + offset - 1 > hm.rom.length())
                return -1;
            hm.rom.write(this.arrangmentsAddress, compArr, compArrLen);
            System.out.println("Wrote " + compArrLen + " bytes of tileset #"
                + num + " arrangements at "
                + Integer.toHexString(this.arrangmentsAddress) + " to "
                + Integer.toHexString(this.arrangmentsAddress + compArrLen - 1)
                + ".");
            return compArrLen;
        }

        /**
         * Writes the tile and arrangment information to the specified location
         * and returns bytes written. Do next write at
         * <code>offset + return value</code>. Best to call this instead of
         * the individual methods unless you have a good reason. Even better to
         * call {@link TileEditor#writeInfo(int, AbstractRom)}, which also
         * writes collision info. Will only write if data was read from the
         * expanded meg or if data was changed.
         * 
         * @see #writeTiles(int)
         * @see #writeArrangements(int)
         * @see TileEditor#writeInfo(int, AbstractRom)
         * @param offset Where to start writting.
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeInfo(int offset)
        {
            int len = 0, tmp;
            if (tilesChanged)
            {
                if ((tmp = writeTiles(offset + len)) != -1)
                    len += tmp;
                else
                    return -1;
            }
            if (arrangementsChanged)
            {
                if ((tmp = writeArrangements(offset + len)) != -1)
                    len += tmp;
                else
                    return -1;
            }
            return len;
        }

        /**
         * Writes the tiles held by this and returns number of bytes written.
         * 
         * @see #writeInfo(int)
         * @see HackModule#writeToFree(byte[], int, int, int)
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeTiles()
        {
            byte[] buffer = new byte[32 * 1024], tmpTile;
            int i = 0;

            for (int t = 0; t < tiles.length; t++)
            {
                tmpTile = this.interlaceTile(this.tiles[t]);
                for (int y = 0; y < tmpTile.length; y++)
                {
                    try
                    {
                        buffer[i++] = tmpTile[y];
                    }
                    catch (ArrayIndexOutOfBoundsException e)
                    {}
                }
            }

            byte[] compTileset; // , compTilesetTv;
            int compTileLen = comp(buffer, compTileset = new byte[65536], 28673);

            if (!hm.writeToFree(compTileset, 0x2F125B + (num * 4),
                tileOldCompLen, compTileLen))
                return -1;
            System.out
                .println("Wrote "
                    + (tileOldCompLen = compTileLen)
                    + " bytes of tileset #"
                    + num
                    + " tiles at "
                    + Integer
                        .toHexString(this.tileAddress = toRegPointer(hm.rom
                            .readMulti(0x2F125B + (num * 4), 4))) + " to "
                    + Integer.toHexString(this.tileAddress + compTileLen - 1)
                    + ".");
            return compTileLen;
        }

        /**
         * Writes the arrangements held by this to free space in the ROM and
         * returns number of bytes written.
         * 
         * @see #writeInfo(int)
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeArrangements()
        {
            init();
            byte[] arrBuffer = new byte[1024 * 32];
            int a = 0;

            for (int x = 0; x < this.getArrangementCount(); x++)
            {
                for (int y = 0; y < arrangements[0].length; y++)
                {
                    for (int z = 0; z < arrangements[0][0].length; z++)
                    {

                        try
                        {
                            arrBuffer[a++] = (byte) (arrangements[x][z][y] & 255);
                            arrBuffer[a++] = (byte) (arrangements[x][z][y] >> 8);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {}
                    }
                }
            }

            byte[] compArr;
            int compArrLen = comp(arrBuffer, compArr = new byte[65536], a);

            if (!hm.writeToFree(compArr, 0x2F12AB + (num * 4), arrOldCompLen,
                compArrLen))
                return -1;
            System.out.println("Wrote "
                + (arrOldCompLen = compArrLen)
                + " bytes of tileset #"
                + num
                + " arrangements at "
                + Integer
                    .toHexString(this.arrangmentsAddress = toRegPointer(hm.rom
                        .readMulti(0x2F12AB + (num * 4), 4))) + " to "
                + Integer.toHexString(this.arrangmentsAddress + compArrLen - 1)
                + ".");
            return compArrLen;
        }

        /**
         * Writes the tile and arrangment information to the ROM and returns
         * bytes written. Best to call this instead of the individual methods
         * unless you have a good reason. Even better to call
         * {@link TileEditor#writeInfo(AbstractRom)}, which also writes
         * collision info. Will only write if data was read from the expanded
         * meg or if data was changed.
         * 
         * @see #writeTiles()
         * @see #writeArrangements()
         * @see TileEditor#writeInfo(AbstractRom)
         * @return Number of bytes written or -1 if not enough space to write.
         */
        public int writeInfo()
        {
            int len = 0, tmp;
            if (tilesChanged)
            {
                if ((tmp = writeTiles()) != -1)
                    len += tmp;
                else
                    return -1;
            }
            if (arrangementsChanged)
            {
                if ((tmp = writeArrangements()) != -1)
                    len += tmp;
                else
                    return -1;
            }
            return len;
        }

        /**
         * Returns true if arrangements need to be written. True if arrangements
         * were read from the expanded meg or if they have been changed.
         * 
         * @return True if arrangements need to be written
         */
        public boolean isArrangementsChanged()
        {
            return arrangementsChanged;
        }

        /**
         * Returns true if collision data has been changed.
         * 
         * @return True if collision data has been changed.
         */
        public boolean isCollisionChanged()
        {
            return collisionChanged;
        }

        /**
         * Returns true if tiles need to be written. True if tiles were read
         * from the expanded meg or if they have been changed.
         * 
         * @return True if tiles need to be written
         */
        public boolean isTilesChanged()
        {
            return tilesChanged;
        }
    }
    
    public void dumpPalettesTxt() {
    	File f = getFile(true, "txt", "Plaintext");
    	try {
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			int k;
			String subpal;
			Color c;
			for (int i = 0; i < tilesets.length; i++) {
				k = tilesets[i].getPaletteCount();
				for (int j = 0; j < k; j++) {
					ps.println(tilesets[i].getPaletteName(j));
					for (int m = 0; m < 6; m++) {
						subpal = "";
						for (int n = 0; n < 16; n++) {
							c = tilesets[i].getPaletteColor(n, j, m);
							subpal += HackModule.addZeros(Integer.toHexString(c.getRGB()).substring(2),6) + " ";
						}
						ps.println("  sub" + m + ": " + subpal);
					}
				}
			}
    	} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (NullPointerException e) {
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
					mainWindow, "Error.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
    }
    
    public void dumpPalettesImg() {
    	int boxSize = 16;
		
    	BufferedImage out = null;
		Graphics2D g;
    	
    	File f = null;
    	try {
    		int k;
			//String subpal;
			Color c;
			for (int i = 0; i < tilesets.length; i++) {
				k = tilesets[i].getPaletteCount();
				for (int j = 0; j < k; j++) {
					//ps.println(tilesets[i].getPaletteName(j));
					out = new BufferedImage(8*boxSize, 6*boxSize*3 - boxSize, BufferedImage.TYPE_4BYTE_ABGR_PRE);
					g = out.createGraphics();
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, 8*boxSize, 6*boxSize*3 - boxSize);
					for (int m = 0; m < 6; m++) {
						//subpal = "";
						for (int n = 0; n < 16; n++) {
							c = tilesets[i].getPaletteColor(n, j, m);
							g.setColor(c);
							g.fillRect((n%8)*boxSize, 3*m*boxSize + (n/8>=1 ? boxSize : 0), boxSize, boxSize);
						}
						//ps.println("  sub" + m + ": " + subpal);
					}
					f = new File("/home/max/pals/" + tilesets[i].getPaletteName2(j) + ".png");
					ImageIO.write(out, "png", f);
				}
			}
			ImageIO.write(out, "png", f);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (NullPointerException e) {
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
					mainWindow, "Error.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
    }

    /**
     * Reads in all of the tileset data into {@link #tilesets}.
     */
    public static void readFromRom(EbHackModule hm)
    {
        for (int i = 0; i < 20; i++)
        {
            tilesets[i] = new Tileset(i, HackModule.getNumberedString(
                TILESET_NAMES[i], i, false), hm);
            // tilesets[i].init();
        }
        readPalettes(hm.rom);
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    public void reset()
    {
        readFromRom();
    }

    private static void readPalettes(AbstractRom rom)
    {
        for (int i = 0; i < 32; i++)
        {
            int t = rom.read(0x2F121B + 2 * i);
            int k = rom.readMulti(0x2F12FB + ((i + 1) * 4), 4)
                - rom.readMulti(0x2F12FB + (i * 4), 4);
            if (i == 31)
                k = 0xDAFAA7 - rom.readMulti(0x2F12FB + (i * 4), 4);
            if (t < 0 || t >= NUM_TILESETS || k < 0)
            {
                JOptionPane
                    .showMessageDialog(null,
                        "Unknown error reading tileset palettes.\n"
                            + "Some or all palettes may be missing.",
                        "Error reading tileset palettes",
                        JOptionPane.ERROR_MESSAGE);
                continue;
            }
            for (int j = 0; j < k / 0xC0; j++)
            {
                tilesets[t].addPalette(new Tileset.Palette(i, j, rom.readMulti(
                    0x2F12FB + (i * 4), 4)
                    - 0xBFFE00 + 0xC0 * j));
            }
        }
        paletteIsInited = true;
    }

    private static boolean isCollisionChanged()
    {
        for (int i = 0; i < tilesets.length; i++)
            if (tilesets[i].isCollisionChanged())
                return true;
        return false;
    }

    /*
     * private void printHexArr(byte[] b) { for (int i = 0; i < b.length; i++) {
     * int x = b[i] & 0xff; System.out.print(addZeros(Integer.toHexString(x), 2) + "
     * "); } }
     * 
     * 
     * private void verifyCollision() { boolean good = true; for (int i = 0; i <
     * NUM_TILESETS; i++) { Tileset t = new Tileset(i, "Unamed #" + i, this), l =
     * tilesets[i]; t.readCollision();
     * 
     * for (int a = 0; a < l.getArrangementCount(); a++) { byte[] tc =
     * t.getCollisionDataFlat(a), lc = l .getCollisionDataFlat(a); if
     * (!Arrays.equals(tc, lc)) { good = false; System.out .println("Incorrectly
     * saved collision data on tileset #" + i + ":"); printHexArr(lc);
     * System.out.print("Arrangement #" + a + "\n"); printHexArr(tc);
     * System.out.println("\n"); } } } if (good) { System.out.println("Collision
     * data was saved correctly."); } }
     */

    private static void writeCollision(AbstractRom rom)
    {
        // Collision info can not be written separately

        // Check if collision data has been changed, just return if not
        if (!isCollisionChanged())
            return;

        int l, lm = 0; // cc = number of collision sequences written so
        // far
        int cp[] = new int[20480]; // cp = collision pointers
        int cpi = 0; // cp incrementer
        boolean tmp;

        try
        {
            for (int i = 0; i < tilesets.length; i++)
            {
                // only care about used arrangements
                int j = tilesets[i].getArrangementCount();
                // collision pointer offset is number of pointers written so
                // far * 2 (each pointer is two bytes)
                tilesets[i].collisionAddress = cpi * 2;
                for (int k = 0; k < j; k++) // go through every used
                // arrangement...
                {
                    // look through collision data written so far
                    tmp = true;
                    byte[] curCollision = tilesets[i].getCollisionDataFlat(k);
                    colloop: for (l = 0; l < lm; l++)
                        if (rom.compare(0x180200 + l, curCollision, 16))
                        {
                            // if collision data already written, use it
                            tmp = false;
                            break colloop;
                        }
                    if (tmp) // if collision data not yet written, write it
                    {
                        if (cpi == 0)
                        {
                            l = 0;
                        }
                        else
                        {
                            if (0x180200 + lm + 16 > 0x18F8B6)
                                throw (new RomWriteOutOfRangeException(
                                    "No space for another collision sequence."));
                            if (lm >= 16 * 4092)
                                throw (new RomWriteOutOfRangeException(
                                    "Someone should optimize this saving routine..."
                                        + " (too many unique properties)"));
                            int overlap = 0;
                            for (int ol = 15; ol > 0; ol--)
                            {
                                if (overlap == 0
                                    && rom.compare(0x180200 + lm + (16 - ol),
                                        curCollision, ol))
                                {
                                    overlap = ol;
                                }
                            }

                            l = lm + (16 - overlap);
                        }
                        rom.write(0x180200 + l, curCollision, 16);
                        tmp = false;
                    }
                    cp[cpi++] = l; // pointer to where the data is
                    lm = Math.max(l, lm);
                }
            }
            lm += 16;
            if (0x180200 + lm + (cpi * 2) > 0x18F8B6)
                throw (new RomWriteOutOfRangeException(
                    "No space for collision pointers."));
            rom.write(0x180200 + lm, cp, cpi, 2);
            System.out.println("Collision stuff ends at: 0x"
                + Integer.toHexString(0x180200 + lm + (cpi * 2)));
            // write collision pointers after collision data
            for (int i = 0; i < 20; i++)
            {
                // collisionAddress currently holds the offset of where the
                // info is
                // add the place of the start of the pointers
                tilesets[i].collisionAddress += 0x180200 + lm;
                rom.write(0x2F137B + (4 * i), HackModule
                    .toSnesPointer(tilesets[i].collisionAddress), 4);
            }
        }
        catch (RomWriteOutOfRangeException e)
        {
            System.out.println("You have too much collision data.");
            JOptionPane.showMessageDialog(null,
                "You have too much collision data:\n" + e.getMessage(),
                "Error: Unable to Write", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Writes all tileset data to the specified location and returns the number
     * of bytes written. Note that if the data has not been changed, it
     * recompresses it and writes it anyway.
     * 
     * @see TileEditor.Tileset#writeInfo(int)
     * @param offset Where to start writting.
     * @return Number of bytes written or -1 if not enough space to write.
     */
    public static int writeInfo(int offset, AbstractRom rom)
    {
        int len = 0, tmp;
        boolean exp = rom.length() == 0x400200, inited[] = new boolean[20];
        if (!exp)
            rom.expand();
        for (int i = 0; i < tilesets.length; i++)
        {
            inited[i] = tilesets[i].isInited();
            tilesets[i].init();
        }
        for (int i = 0; i < tilesets.length; i++)
        {
            if (!(!inited[i] && (!exp || (tilesets[i].tileAddress < 0x300200 && tilesets[i].arrangmentsAddress < 0x300200))))
            {
                if ((tmp = tilesets[i].writeInfo(offset + len)) != -1)
                    len += tmp;
                else
                    return -1;
            }
        }
        writeCollision(rom);
        return len;
    }

    /**
     * Writes all tileset data to 0x3A0200 and returns the number of bytes
     * written. If that write fails and <code>writeLower</code> is true then
     * it trys 0x10000 bytes eariler until it has space. You can tell if it did
     * that by looking at how high the return value is. Note that if the data
     * has not been changed, it recompresses it and writes it anyway. The offset
     * is the one used by Cabbage's tile editor.
     * 
     * @see #writeInfo(AbstractRom)
     * @see TileEditor.Tileset#writeInfo(int)
     * @param writeLower If true tries to write eariler in the ROM until it
     *            works.
     * @return Number of bytes written.
     */
    public static int writeInfo(boolean writeLower, AbstractRom rom)
    {
        int tmp = -1, offset = 0x3A0200;
        while (tmp == -1 && writeLower && offset > 0x3001ff)
        {
            tmp = writeInfo(offset, rom);
            offset -= 0x10000;
        }

        return tmp;
    }

    /**
     * Writes all tileset data to the ROM and returns the number of bytes
     * written. Note that if the data has not been changed, it recompresses it
     * and writes it anyway.
     * 
     * @see TileEditor.Tileset#writeInfo()
     * @see HackModule#writeToFree(byte[], int, int, int)
     * @return Number of bytes written.
     */
    public static int writeInfo(AbstractRom rom)
    {
        int len = 0, tmp;
        boolean exp = rom.length() != AbstractRom.EB_ROM_SIZE_REGULAR;
        for (int i = 0; i < tilesets.length; i++)
        {
            /* Write tilesets which have been inited */
            if (tilesets[i].isInited()
                || (exp && (tilesets[i].tilesChanged || tilesets[i].arrangementsChanged)))
            {
                if (!tilesets[i].isInited())
                {
                    tilesets[i].init();
                }
                if ((tmp = tilesets[i].writeInfo()) != -1)
                    len += tmp;
                else
                    return -1;
            }
        }
        writeCollision(rom);
        return len;
    }

    // GUI stuff
    /*
     * GUI Design: Main Window Menu File Import... (.png & .csv) Export... (.png &
     * .csv) Edit Undo (Single undo list? (no, make arrow pointing to current
     * graphics editor) -------- Cut Copy Paste Delete (no flip stuff,
     * arrangements flip) Components Display/selection (BorderLayout.WEST) Tile
     * viewer/selector, 2x zoom (BorderLayout.NORTH) Arrangement viewer/selector
     * with scroll, no zoom (BorderLayout.SOUTH) Editing (BorderLayout.CENTER)
     * (BoxLayout inside?) Tileset Selector Palette Selector SubPalette Selector
     * Tile graphics editor (encode color number into color. ex. R&1 < < 3 | G&1 < <
     * 2 | B&3 ==n) Background layer, 10x zoom? (#0-511) (BorderLayout.WEST)
     * Foreground layer, 10x zoom? (#0-511 + 512) (BorderLayout.EAST) Tile
     * editing palette (Make palette smaller? No help) (NOTE: palette edit by
     * right-clicking) Arrangment editing Collision editor (BorderLayout.WEST)
     * Collision editor text boxes, 4x4 GridLayout (JTable didn't work well for
     * this) Arrangement tile placement editor, with gridlines, 2x zoom
     * (BorderLayout.EAST) Drawing toolset (BorderLayout.EAST)
     */

    private boolean guiInited = false;

    private TileSelector tileSelector;
    private ArrangementSelector arrangementSelector;

    private JComboBox tilesetSelector, paletteSelector, subPaletteSelector;

    // private JTable collisionEditor;
    // private AbstractTableModel collisionTableModel;
    private CollisionEditor collisionEditor;
    private TileArrangementEditor arrangementEditor;

    private SpritePalette tileDrawingPalette;
    private DrawingToolset tileDrawingToolset;
    private IntArrDrawingArea tileDrawingArea, tileForegroundDrawingArea;

    private FocusIndicator drawingAreaFocusIndicator,
            arrangementFocusIndicator;

    private JDialog cbdia; // clipboard dialog
    private TileSelector cbsel; // clipboard tile selector
    private byte[][][][] cb; // clipboard

    private class ArrangementSelector extends AbstractButton implements
        MouseListener, MouseMotionListener, AdjustmentListener
    {
        private int currentArrangement = 0;

        public int getCurrentArrangement()
        {
            return currentArrangement;
        }

        private int getArrangementOffset()
        {
            return scroll.getValue() * 4;
            // each increment shows four arrangments (height)
        }

        public void repaintTile(int tile)
        {
            Graphics g = display.getGraphics();
            Image img[] = new Image[8]; // one for each subpal
            for (int a = getArrangementOffset(); a < getArrangementOffset() + 60; a++)
            {
                for (int x = 0; x < 4; x++)
                {
                    for (int y = 0; y < 4; y++)
                    {
                        int arr = getSelectedTileset().getArrangementData(a, x,
                            y);
                        if ((arr & 0x1ff) == tile)
                        {
                            int subPal = (((arr & 0x1C00) >> 10) - 2);
                            if (img[subPal] == null)
                                img[subPal] = getSelectedTileset()
                                    .getTileImage(tile, getCurrentPalette(),
                                        subPal);
                            // (dx, dy) = top-left corner of destination
                            int dx = (((a - getArrangementOffset()) / 4) * 33)
                                + (x * 8), dy = (((a - getArrangementOffset()) % 4) * 33)
                                + (y * 8);
                            g.drawImage(img[subPal], dx, dy, dx + 8, dy + 8,
                                ((arr & 0x4000) == 0 ? 0 : 8),
                                ((arr & 0x8000) == 0 ? 0 : 8),
                                ((arr & 0x4000) != 0 ? 0 : 8),
                                ((arr & 0x8000) != 0 ? 0 : 8), null);
                            // if this just drew on the current arrangement...
                            if (a == getCurrentArrangement())
                            {
                                // rehighlight the part that was just drawn over
                                g.setColor(new Color(255, 255, 0, 128));
                                g.fillRect(
                                    (((a - getArrangementOffset()) / 4) * 33)
                                        + (x * 8),
                                    (((a - getArrangementOffset()) % 4) * 33)
                                        + (y * 8), 8, 8);
                            }
                        }
                    }
                }
            }
        }

        public void repaintCurrentTile()
        {
            repaintTile(getCurrentTile());
        }

        public void repaintCurrentArrangement()
        {
            int a = getCurrentArrangement() - getArrangementOffset();
            // is it being shown?
            if (a >= 0 && a < 60)
            {
                drawArrangement(display.getGraphics(), getCurrentArrangement());
                highlightArrangement(display.getGraphics(),
                    getCurrentArrangement());
            }
        }

        private void drawArrangement(Graphics g, int arr)
        {
            g.drawImage(getSelectedTileset().getArrangementImage(arr,
                getCurrentPalette()),
                ((arr - getArrangementOffset()) / 4) * 33,
                ((arr - getArrangementOffset()) % 4) * 33, null);
        }

        private void highlightArrangement(Graphics g, int arr)
        {
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect(((arr - getArrangementOffset()) / 4) * 33,
                ((arr - getArrangementOffset()) % 4) * 33, 32, 32);
        }

        public void setCurrentArrangement(int newArrangement)
        {
            // only fire action performed if new arrangment
            if (currentArrangement != newArrangement)
            {
                scroll.setValue(newArrangement / 4);
                reHightlight(currentArrangement, newArrangement);
                currentArrangement = newArrangement;
                this.fireActionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
            }
        }

        private void setCurrentArrangement(int x, int y)
        {
            int newArrangement = getArrangementOffset() + ((y / 33))
                + ((x / 33) * 4);
            // only fire action performed if new arrangment
            if (currentArrangement != newArrangement)
            {
                reHightlight(currentArrangement, newArrangement);
                currentArrangement = newArrangement;
                this.fireActionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
            }
        }

        private void reHightlight(int oldArr, int newArr)
        {
            Graphics g = display.getGraphics();
            if (oldArr >= getArrangementOffset()
                && oldArr < getArrangementOffset() + 60)
            {
                drawArrangement(g, oldArr);
                // g.drawImage(getSelectedTileset().getArrangementImage(oldArr,
                // getCurrentPalette()),
                // ((oldArr - getArrangementOffset()) / 4) * 33,
                // ((oldArr - getArrangementOffset()) % 4) * 33, null);
            }
            highlightArrangement(g, newArr);
            // g.setColor(new Color(255, 255, 0, 128));
            // g.fillRect(((newArr - getArrangementOffset()) / 4) * 33,
            // ((newArr - getArrangementOffset()) % 4) * 33, 32, 32);
        }

        public void paint(Graphics g)
        {
            super.paint(g);
            display.repaint();
        }

        private String actionCommand = new String();

        public String getActionCommand()
        {
            return this.actionCommand;
        }

        public void setActionCommand(String arg0)
        {
            this.actionCommand = arg0;
        }

        public void mouseClicked(MouseEvent me)
        {
            setCurrentArrangement(me.getX(), me.getY());
        }

        public void mousePressed(MouseEvent me)
        {
            setCurrentArrangement(me.getX(), me.getY());
        }

        public void mouseReleased(MouseEvent me)
        {
            setCurrentArrangement(me.getX(), me.getY());
        }

        public void mouseEntered(MouseEvent me)
        {}

        public void mouseExited(MouseEvent me)
        {}

        public void mouseDragged(MouseEvent me)
        {
            if (!(me.getX() < 0 || me.getY() < 0 || me.getX() > (33 * 15) - 1 || me
                .getY() > (33 * 4) - 1))
                setCurrentArrangement(me.getX(), me.getY());
        }

        public void mouseMoved(MouseEvent me)
        {}

        public void adjustmentValueChanged(AdjustmentEvent arg0)
        {
            repaint();
        }

        private JPanel display;
        private JScrollBar scroll;

        public ArrangementSelector()
        {

            display = new JPanel()
            {
                public void paint(Graphics g)
                {
                    if (paletteIsInited && guiInited)
                        g.drawImage(getSelectedTileset().getArrangementsImage(
                            getArrangementOffset(), 15, 4, getCurrentPalette(),
                            Color.BLACK, getCurrentArrangement()), 0, 0, null);
                }
            };
            display
                .setPreferredSize(new Dimension((33 * 15) - 1, (33 * 4) - 1));
            scroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, 15, 0, 255);
            scroll.addAdjustmentListener(this);
            this.setLayout(new BorderLayout());
            this.add(HackModule.createFlowLayout(display), BorderLayout.CENTER);
            this.add(scroll, BorderLayout.SOUTH);

            display.addMouseListener(this);
            display.addMouseMotionListener(this);
        }
    }

    private class CollisionEditor extends AbstractButton implements Undoable,
        CopyAndPaster, FocusListener
    {
        private JTextField[][] tf = new JTextField[4][4];
        private boolean reading = false;

        private class CollisionDocumentListener implements DocumentListener
        {
            private int x, y;
            private Component c;

            public CollisionDocumentListener(int x, int y, Component c)
            {
                this.x = x;
                this.y = y;
                this.c = c;
            }

            private void valueChanged()
            {
                if (!reading)
                {
                    addUndo();
                    getSelectedTileset().setCollisionData(
                        getCurrentArrangement(),
                        x,
                        y,
                        (byte) Integer.parseInt(HackModule.addZeros(tf[x][y]
                            .getText(), 2), 16));
                    setFocus(c);
                }
            }

            public void insertUpdate(DocumentEvent arg0)
            {
                valueChanged();
            }

            public void removeUpdate(DocumentEvent arg0)
            {
                valueChanged();
            }

            public void changedUpdate(DocumentEvent arg0)
            {
                valueChanged();
            }
        }

        public void updateTfs()
        {
            reading = true;
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    tf[x][y].setText(HackModule.addZeros(Integer
                        .toHexString(getSelectedTileset().getCollisionData(
                            getCurrentArrangement(), x, y) & 0xff), 2));
                }
            }
            reading = false;
        }

        public CollisionEditor()
        {
            this.setLayout(new GridLayout(4, 4));
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    this.add(tf[x][y] = HackModule.createSizedJTextField(2));
                    tf[x][y].setHorizontalAlignment(SwingConstants.CENTER);
                    tf[x][y].getDocument().addDocumentListener(
                        new CollisionDocumentListener(x, y, this));
                    tf[x][y].addFocusListener(this);
                }
            }
            Dimension d = this.getPreferredSize();
            int size = Math.max(d.height, d.width);
            this.setPreferredSize(new Dimension(size, size));
        }

        private ArrayList undoList = new ArrayList();

        public void addUndo()
        {
            String[][] newUndo = new String[4][4];
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++)
                    newUndo[x][y] = tf[x][y].getText();
            undoList.add(newUndo);
        }

        public void undo()
        {
            if (undoList.size() > 0)
            {
                String[][] undo = (String[][]) undoList
                    .get(undoList.size() - 1);
                for (int x = 0; x < 4; x++)
                    for (int y = 0; y < 4; y++)
                        tf[x][y].setText(undo[x][y]);
                undoList.remove(undoList.size() - 1);
            }
        }

        public void resetUndo()
        {
            undoList = new ArrayList();
        }

        private byte[][] cb = null;

        public void copy()
        {
            cb = getSelectedTileset().getCollisionData(getCurrentArrangement());
        }

        public void paste()
        {
            if (cb == null)
                return;
            getSelectedTileset().setCollisionData(getCurrentArrangement(), cb);
            updateCollisionEditor();
        }

        public void delete()
        {
            addUndo();
            getSelectedTileset().setCollisionData(getCurrentArrangement(),
                new byte[4][4]);
            updateTfs();
            // for (int x = 0; x < 4; x++)
            // for (int y = 0; y < 4; y++)
            // tf[x][y].setText("00");
        }

        public void cut()
        {
            copy();
            delete();
        }

        public void focusGained(FocusEvent arg0)
        {
            setFocus(this);
        }

        public void focusLost(FocusEvent arg0)
        {}
    }

    private class TileArrangementEditor extends ArrangementEditor
    {
        protected boolean isEditable()
        {
            return true;
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
            return 4;
        }

        protected int getTilesHigh()
        {
            return 4;
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
                    .getValueAsBoolean("eb.TileEditor.arrEditor.gridLines");
            }
            catch (NullPointerException e)
            {
                return JHack.main.getPrefs().getValueAsBoolean(
                    "eb.TileEditor.arrEditor.gridLines");
            }
        }

        protected boolean isGuiInited()
        {
            return guiInited && paletteIsInited;
        }

        protected int getCurrentSubPalette()
        {
            return TileEditor.this.getCurrentSubPalette();
        }

        protected short getArrangementData(int x, int y)
        {
            return getSelectedTileset().getArrangementData(
                getCurrentArrangement(), x, y);
        }

        protected short[][] getArrangementData()
        {
            return getSelectedTileset().getArrangementData(
                getCurrentArrangement());
        }

        protected void setArrangementData(int x, int y, short data)
        {
            getSelectedTileset().setArrangementData(getCurrentArrangement(), x,
                y, data);
        }

        protected void setArrangementData(short[][] data)
        {
            getSelectedTileset().setArrangementData(getCurrentArrangement(),
                data);
        }

        protected Image getArrangementImage(short[][] selection)
        {
            return getSelectedTileset().getArrangementImage(
                getCurrentArrangement(), getCurrentPalette(), getZoom(),
                isDrawGridLines());
        }

        public Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            return getSelectedTileset().getTileImage(tile, getCurrentPalette(),
                subPal - 2, hFlip, vFlip);
        }
    }

    private class MinitileSelector extends TileSelector
    {
        public int getTilesWide()
        {
            return 32;
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
            // TileEditor t = TileEditor.this;
            // System.out.println(t == null
            // ? "TileEditor.this is null"
            // : "TileEditor.this is good!");
            // XMLPreferences prefs = JHack.main.getPrefs();
            // System.out.println(JHack.main.getPrefs() == prefs
            // ? "the same"
            // : "different");
            try
            {
                return prefs
                    .getValueAsBoolean("eb.TileEditor.tileSelector.gridLines");
            }
            catch (NullPointerException e)
            {
                return JHack.main.getPrefs().getValueAsBoolean(
                    "eb.TileEditor.tileSelector.gridLines");
            }
        }

        public int getTileCount()
        {
            return 512;
        }

        public Image getTileImage(int tile)
        {
            return getSelectedTileset().getTileImage(tile, getCurrentPalette(),
                getCurrentSubPalette());
        }

        protected boolean isGuiInited()
        {
            return guiInited && paletteIsInited;
        }
    }

    private class FocusIndicator extends AbstractButton implements
        FocusListener, MouseListener
    {
        // 0 = other FI, 1 = left component, 2 = right component
        private int focus = 1;
        private Component c1, c2;
        private boolean otherUp;
        private FocusIndicator fi;

        public Component getCurrentFocus()
        {
            return (focus == 0 ? fi.getCurrentFocus() : (focus == 1 ? c1 : c2));
        }

        private void cycleFocus()
        {
            focus++;
            if (focus > 2)
                focus = 1;
            fi.setFocus(this);
            repaint();
        }

        private void setFocus(Component c)
        {
            if (c == c1)
                focus = 1;
            else if (c == c2)
                focus = 2;
            else
                focus = 0;
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
            int[] arrowX = new int[]{10, 40, 40, 50, 40, 40, 10};
            int[] arrowY = new int[]{22, 22, 15, 25, 35, 28, 28};

            // flip if arrow should point the other way
            if (focus == 1 || (focus == 0 && otherUp))
            {
                for (int i = 0; i < arrowX.length; i++)
                {
                    arrowX[i] = 50 - arrowX[i];
                }
            }

            if (focus == 0) // switch X and Y for pointing up
                g.fillPolygon(arrowY, arrowX, 7);
            else
                g.fillPolygon(arrowX, arrowY, 7);
        }

        public void setOtherFocusIndicator(FocusIndicator fi)
        {
            this.fi = fi;
            focus = 0;
            fi.addFocusListener(this);
        }

        public FocusIndicator(Component c1, Component c2,
            Component[] otherComponents, boolean otherUp, FocusIndicator fi)
        {
            if (fi != null)
            {
                this.fi = fi;
                this.fi.addFocusListener(this);
            }

            (this.c1 = c1).addFocusListener(this);
            (this.c2 = c2).addFocusListener(this);

            for (int i = 0; i < otherComponents.length; i++)
                otherComponents[i].addFocusListener(this);

            this.otherUp = otherUp;

            this.addMouseListener(this);

            this.setPreferredSize(new Dimension(50, 50));

            this
                .setToolTipText("This arrow points to which component will recive menu commands. "
                    + "Click to change.");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        // mainWindow.setSize(900, 515);

        // Menu
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'y', "ctrl S",
            "apply", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import Minitile...", 'i',
            null, "importTile", this));
        fileMenu.add(HackModule.createJMenuItem("Export Minitile...", 'e',
            null, "exportTile", this));
        fileMenu.add(HackModule.createJMenuItem("Import Palette...", 'm', null,
            "importPal", this));
        fileMenu.add(HackModule.createJMenuItem("Export Palette...", 'o', null,
            "exportPal", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule
            .createJMenuItem("Import All Tileset Minitiles...", 'm', null,
                "importTileset", this));
        fileMenu.add(HackModule
            .createJMenuItem("Export All Tileset Minitiles...", 'x', null,
                "exportTileset", this));
        fileMenu.add(HackModule.createJMenuItem(
            "Import All Tileset Properties...", 'p', null, "importCollision",
            this));
        fileMenu.add(HackModule.createJMenuItem(
            "Export All Tileset Properties...", 'r', null, "exportCollision",
            this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import All Tileset Data...",
            't', null, "importAllTileset", this));
        fileMenu.add(HackModule.createJMenuItem("Export All Tileset Data...",
            's', null, "exportAllTileset", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import All Tilesets Data...",
            'a', null, "importAll", this));
        fileMenu.add(HackModule.createJMenuItem("Export All Tilesets Data...",
            'l', null, "exportAll", this));

        mb.add(fileMenu);

        JMenu editMenu = HackModule.createEditMenu(this, true);

        editMenu.add(new JSeparator());

        editMenu.add(HackModule.createJMenuItem("Cut Both", 'b',
            "ctrl shift X", "cutBoth", this));
        editMenu.add(HackModule.createJMenuItem("Copy Both", 'o',
            "ctrl shift C", "copyBoth", this));
        editMenu.add(HackModule.createJMenuItem("Paste Both", 's',
            "ctrl shift V", "pasteBoth", this));
        editMenu.add(HackModule.createJMenuItem("Delete Both", 'h',
            "shift DELETE", "deleteBoth", this));

        editMenu.addSeparator();

        editMenu.add(createJMenuItem("Show Multi-Clipboard", 'm', "alt M",
            "cb_show", this));

        mb.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Enable Tile Selector Grid Lines",
            prefs, "eb.TileEditor.tileSelector.gridLines", false, 't', null,
            "tileSelGridLines", this));
        optionsMenu.add(new PrefsCheckBox(
            "Enable Arrangement Editor Grid Lines", prefs,
            "eb.TileEditor.arrEditor.gridLines", true, 'a', null,
            "arrEdGridLines", this));
        mb.add(optionsMenu);

        JMenu focusMenu = new JMenu("Focus");
        focusMenu.setMnemonic('c');

        focusMenu.add(HackModule.createJMenuItem("Background Graphics Editor",
            'b', "ctrl B", "bgeFocus", this));
        focusMenu.add(HackModule.createJMenuItem("Foreground Graphics Editor",
            'f', "ctrl F", "fgeFocus", this));
        focusMenu.add(HackModule.createJMenuItem(
            "Arrangement Properties Editor", 'p', "ctrl P", "colFocus", this));
        focusMenu.add(HackModule.createJMenuItem("Arrangement Editor", 'a',
            "ctrl A", "arrFocus", this));

        focusMenu.add(new JSeparator());

        focusMenu.add(HackModule.createJMenuItem("Cycle Focus", 'y', "ctrl Y",
            "cycFocus", this));

        mb.add(focusMenu);

        mainWindow.setJMenuBar(mb);

        JPanel scrolledArea = new JPanel(new BorderLayout());
        JPanel display = new JPanel(new BorderLayout());

        display.add(tileSelector = new MinitileSelector(), BorderLayout.NORTH);
        tileSelector.setActionCommand("tileSelector");
        tileSelector.addActionListener(this);

        display.add(arrangementSelector = new ArrangementSelector(),
            BorderLayout.SOUTH);
        arrangementSelector.setActionCommand("arrangementSelector");
        arrangementSelector.addActionListener(this);

        scrolledArea.add(display, BorderLayout.WEST);

        Box edit = new Box(BoxLayout.Y_AXIS);

        Box selectors = new Box(BoxLayout.Y_AXIS);
        // combo boxes
        selectors.add(HackModule.getLabeledComponent("Tileset: ",
            this.tilesetSelector = HackModule.createJComboBoxFromArray(
                TileEditor.TILESET_NAMES, false)));
        tilesetSelector.setActionCommand("tilesetSelector");
        tilesetSelector.addActionListener(this);
        selectors.add(HackModule.getLabeledComponent("Palette: ",
            this.paletteSelector = new JComboBox()));
        paletteSelector.setActionCommand("paletteSelector");
        paletteSelector.addActionListener(this);
        selectors.add(HackModule.getLabeledComponent("subPalette: ",
            this.subPaletteSelector = new JComboBox()));
        for (int i = 0; i < 6; i++)
        {
            this.subPaletteSelector.addItem(Integer.toString(i));
        }
        subPaletteSelector.setActionCommand("subPaletteSelector");
        subPaletteSelector.addActionListener(this);

        edit.add(pairComponents(selectors, new JLabel(), false));
        // edit.add(Box.createVerticalStrut(70));
        edit.add(Box.createVerticalGlue());

        // Tile Editing
        JPanel tileEdit = new JPanel(new FlowLayout());
        this.tileDrawingPalette = new SpritePalette(16, true);
        tileDrawingPalette.setActionCommand("paletteEditor");
        tileDrawingPalette.addActionListener(this);
        this.tileDrawingToolset = new DrawingToolset(this);

        this.tileDrawingArea = new IntArrDrawingArea(this.tileDrawingToolset,
            this.tileDrawingPalette, this);
        tileDrawingArea.setZoom(10);
        tileDrawingArea.setPreferredSize(new Dimension(80, 80));
        tileDrawingArea.setActionCommand("tileDrawingArea");
        this.tileForegroundDrawingArea = new IntArrDrawingArea(
            this.tileDrawingToolset, this.tileDrawingPalette, this);
        tileForegroundDrawingArea.setZoom(10);
        tileForegroundDrawingArea.setPreferredSize(new Dimension(80, 80));
        tileForegroundDrawingArea.setActionCommand("tileForegroundDrawingArea");
        // use same clipboard
        tileForegroundDrawingArea.setIntArrClipboard(tileDrawingArea
            .getIntArrClipboard());

        collisionEditor = new CollisionEditor();
        arrangementEditor = new TileArrangementEditor();
        arrangementEditor.setActionCommand("arrangementEditor");
        arrangementEditor.addActionListener(this);

        this.drawingAreaFocusIndicator = new FocusIndicator(tileDrawingArea,
            tileForegroundDrawingArea, new Component[]{collisionEditor,
                arrangementEditor}, false, null);
        this.arrangementFocusIndicator = new FocusIndicator(collisionEditor,
            arrangementEditor, new Component[]{tileDrawingArea,
                tileForegroundDrawingArea}, true, drawingAreaFocusIndicator);
        this.drawingAreaFocusIndicator
            .setOtherFocusIndicator(arrangementFocusIndicator);

        // background to foreground copy
        JButton bfCopy = new JButton("--> Copy -->");
        bfCopy.setActionCommand("bfCopy");
        bfCopy.addActionListener(this);

        // foreground to background copy
        JButton fbCopy = new JButton("<-- Copy <--");
        fbCopy.setActionCommand("fbCopy");
        fbCopy.addActionListener(this);

        // drawing area buttons
        JPanel daButtons = new JPanel(new BorderLayout());
        daButtons.add(HackModule
            .createFlowLayout(this.drawingAreaFocusIndicator),
            BorderLayout.CENTER);
        daButtons.add(bfCopy, BorderLayout.NORTH);
        daButtons.add(fbCopy, BorderLayout.SOUTH);

        tileEdit.add(this.tileDrawingArea);
        tileEdit.add(daButtons);
        tileEdit.add(this.tileForegroundDrawingArea);

        edit.add(tileEdit);

        edit.add(HackModule.createFlowLayout(this.tileDrawingPalette));

        edit.add(Box.createVerticalGlue());

        JPanel arrEdit = new JPanel(new FlowLayout());

        arrEdit.add(collisionEditor);
        arrEdit.add(arrangementFocusIndicator);
        arrEdit.add(arrangementEditor);

        edit.add(arrEdit);

        scrolledArea.add(edit, BorderLayout.CENTER);

        Box toolsetBox = new Box(BoxLayout.Y_AXIS);
        toolsetBox.add(tileDrawingToolset);
        toolsetBox.add(Box.createVerticalStrut(200));

        scrolledArea.add(toolsetBox, BorderLayout.EAST);

        mainWindow.getContentPane().add(new JScrollPane(scrolledArea),
            BorderLayout.CENTER);

        mainWindow.pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.8";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Tile Editor";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Based on code by Cabbage";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        // readFromRom();
        super.show();

        mainWindow.setVisible(true);
        guiInited = true;
        tilesetSelector.setSelectedIndex(0);
    }

    /**
     * Shows a specific arrangement or tile in a specific palette.
     * 
     * @param obj A <code>Integer[]</code> or <code>int[]</code> with 3, 4,
     *            or 5 elements: graphics tileset, palette index, arrangement
     *            number, (if 5 elements) [mini]tile number, (if 6 elements)
     *            subpalette number.
     */
    public void show(Object obj)
    {
        show();
        if (obj instanceof Integer[])
        {
            Integer[] iarr = (Integer[]) obj;
            int[] narr = new int[iarr.length];
            for (int i = 0; i < iarr.length; i++)
                narr[i] = iarr[i].intValue();
            obj = narr;
        }
        if (obj instanceof int[])
        {
            int[] arr = (int[]) obj;
            tilesetSelector.setSelectedIndex(arr[0]);
            paletteSelector.setSelectedIndex(arr[1]);
            arrangementSelector.setCurrentArrangement(arr[2]);
            if (arr.length > 3)
            {
                tileSelector.setCurrentTile(arr[3]);
                if (arr.length > 4)
                {
                    subPaletteSelector.setSelectedIndex(arr[4]);
                }
            }
        }
    }

    private void doTilesetSelectAction()
    {
        if (!getSelectedTileset().init())
        {
            guiInited = false;
            Object opt = JOptionPane.showInputDialog(mainWindow,
                "Error decompressing the " + getSelectedTileset().name
                    + " tileset (#" + getCurrentTileset() + ").",
                "Decompression Error", JOptionPane.ERROR_MESSAGE, null,
                new String[]{"Abort", "Retry", "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                tilesetSelector.setSelectedIndex((tilesetSelector
                    .getSelectedIndex() + 1)
                    % tilesetSelector.getItemCount());
                doTilesetSelectAction();
                return;
            }
            else if (opt.equals("Retry"))
            {
                // mapSelector.setSelectedIndex(mapSelector.getSelectedIndex());
                doTilesetSelectAction();
                return;
            }
            else if (opt.equals("Fail"))
            {
                getSelectedTileset().initToNull();
            }
        }
        guiInited = true;

        updatePaletteSelector();
        updateTileSelector();
        updateArrangementSelector();
        resetArrangementUndo();
        updateCollisionEditor();
        arrangementEditor.clearSelection();
        updateArrangementEditor();
        updatePaletteDisplay();
        updateTileGraphicsEditor();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        // respond to each component
        if (ae.getActionCommand().equals("tilesetSelector"))
        {
            doTilesetSelectAction();
        }
        else if (ae.getActionCommand().equals("paletteSelector"))
        {
            updateTileSelector();
            updateArrangementSelector();
            updateArrangementEditor();
            updatePaletteDisplay();
            updateTileGraphicsEditor();
        }
        else if (ae.getActionCommand().equals("subPaletteSelector"))
        {
            updateTileSelector();
            updatePaletteDisplay();
            updateTileGraphicsEditor();
        }
        else if (ae.getActionCommand().equals("tileSelector"))
        {
            updateTileGraphicsEditor();
            if (!(this.getCurrentComponent() instanceof IntArrDrawingArea))
                setFocus(tileDrawingArea);
        }
        else if (ae.getActionCommand().equals("arrangementSelector"))
        {
            resetArrangementUndo();
            arrangementEditor.clearSelection();
            updateCollisionEditor();
            updateArrangementEditor();
            if (this.getCurrentComponent() instanceof IntArrDrawingArea)
                setFocus(arrangementEditor);
        }
        else if (ae.getActionCommand().equals("arrangementEditor"))
        {
            setFocus((Component) ae.getSource());
            // updateArrangementSelector();
            arrangementSelector.repaintCurrentArrangement();
        }
        else if (ae.getActionCommand().equals("tileDrawingArea"))
        {
            setFocus((Component) ae.getSource());
            getSelectedTileset().setTile(getCurrentTile(),
                tileDrawingArea.getByteArrImage());
            tileSelector.repaintCurrent();
            arrangementSelector.repaintCurrentTile();
            arrangementEditor.repaintCurrentTile();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            getSelectedTileset().setPaletteColor(
                tileDrawingPalette.getSelectedColorIndex(),
                getCurrentPalette(), getCurrentSubPalette(),
                tileDrawingPalette.getNewColor());
            updatePaletteDisplay();
            updateTileGraphicsEditor();
            updateTileSelector();
            updateArrangementSelector();
            updateArrangementEditor();
        }
        else if (ae.getActionCommand().equalsIgnoreCase(
            "tileForegroundDrawingArea"))
        {
            setFocus((Component) ae.getSource());
            getSelectedTileset().setTile(getCurrentTile() + 512,
                tileForegroundDrawingArea.getByteArrImage());
        }
        // default window stuff
        else if (ae.getActionCommand().equals("apply"))
        {
            // writeInfo(true);
            writeInfo(rom);
            /* Verify collision code works. Debug use only. */
            // verifyCollision();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        // flipping
        else if (ae.getActionCommand().equals("hFlip"))
        {
            if (getCurrentComponent() instanceof ImageDrawingArea)
                ((ImageDrawingArea) getCurrentComponent()).doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            if (getCurrentComponent() instanceof ImageDrawingArea)
                ((ImageDrawingArea) getCurrentComponent()).doVFlip();
        }
        // undo
        else if (ae.getActionCommand().equals("undo"))
        {
            getCurrentUndoable().undo();
            getCurrentComponent().repaint();
        }
        // copy&paste stuff
        else if (ae.getActionCommand().equals("cut"))
        {
            getCurrentCopyAndPaster().cut();
            getCurrentComponent().repaint();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            getCurrentCopyAndPaster().copy();
            getCurrentComponent().repaint();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            getCurrentCopyAndPaster().paste();
            getCurrentComponent().repaint();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            getCurrentCopyAndPaster().delete();
            getCurrentComponent().repaint();
        }
        // copy&paste both stuff
        else if (ae.getActionCommand().equals("cutBoth"))
        {
            this.cutBoth();
        }
        else if (ae.getActionCommand().equals("copyBoth"))
        {
            this.copyBoth();
        }
        else if (ae.getActionCommand().equals("pasteBoth"))
        {
            this.pasteBoth();
        }
        else if (ae.getActionCommand().equals("deleteBoth"))
        {
            this.deleteBoth();
        }
        // copy&paste cb stuff
        else if (ae.getActionCommand().equals("cb_show"))
        {
            if (cbdia == null)
                initCbDia();
            cbdia.setVisible(true);
        }
        else if (ae.getActionCommand().equals("cb_copy"))
        {
            int i = cbsel.getCurrentTile();
            byte[][] fg = tileForegroundDrawingArea.getByteArrImage(), bg = tileDrawingArea
                .getByteArrImage();
            for (int x = 0; x < 8; x++)
            {
                System.arraycopy(bg[x], 0, cb[i][0][x], 0, 8);
                System.arraycopy(fg[x], 0, cb[i][1][x], 0, 8);
            }
            cbsel.repaintCurrent();
        }
        else if (ae.getActionCommand().equals("cb_paste"))
        {
            int i = cbsel.getCurrentTile();
            byte[][] fg = new byte[8][8], bg = new byte[8][8];
            for (int x = 0; x < 8; x++)
            {
                System.arraycopy(cb[i][0][x], 0, bg[x], 0, 8);
                System.arraycopy(cb[i][1][x], 0, fg[x], 0, 8);
            }
            tileDrawingArea.paste(bg, true);
            tileDrawingArea.repaint();
            tileForegroundDrawingArea.paste(fg, true);
            tileForegroundDrawingArea.repaint();
        }
        // backgroud <--> foreground copies
        else if (ae.getActionCommand().equals("bfCopy"))
        {
            this.tileForegroundDrawingArea.setImage(this.tileDrawingArea
                .getIntArrImage());
            this.tileForegroundDrawingArea.repaint();
            this.setFocus(tileForegroundDrawingArea);
        }
        else if (ae.getActionCommand().equals("fbCopy"))
        {
            this.tileDrawingArea.setImage(this.tileForegroundDrawingArea
                .getIntArrImage());
            this.tileDrawingArea.repaint();
            this.setFocus(tileDrawingArea);
        }
        // gridline toggle
        else if (ae.getActionCommand().equals("tileSelGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            mainWindow.getContentPane().validate();
            if (cbdia != null)
            {
                cbdia.invalidate();
                cbsel.invalidate();
                cbsel.resetPreferredSize();
                cbsel.validate();
                cbsel.repaint();
                cbdia.validate();
                cbdia.pack();
            }
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
        // import/export
        else if (ae.getActionCommand().equals("importTile"))
        {
            importTile(getCurrentTileset(), getCurrentTile());

            updateTileGraphicsEditor();
            updateTileSelector();
        }
        else if (ae.getActionCommand().equals("exportTile"))
        {
            exportTile(getCurrentTileset(), getCurrentTile());
        }
        else if (ae.getActionCommand().equals("importPal"))
        {
            importPalette(getCurrentTileset(), getCurrentPalette());

            updateTileSelector();
            updateArrangementSelector();
            updateArrangementEditor();
            updatePaletteDisplay();
            updateTileGraphicsEditor();
        }
        else if (ae.getActionCommand().equals("exportPal"))
        {
            exportPalette(getCurrentTileset(), getCurrentPalette());
        }
        else if (ae.getActionCommand().equals("importTileset"))
        {
            importTileset(getCurrentTileset());

            updateTileGraphicsEditor();
            updateTileSelector();
        }
        else if (ae.getActionCommand().equals("exportTileset"))
        {
            exportTileset(getCurrentTileset());
        }
        else if (ae.getActionCommand().equals("importCollision"))
        {
            importCollision(getCurrentTileset());

            updateCollisionEditor();
        }
        else if (ae.getActionCommand().equals("exportCollision"))
        {
            exportCollision(getCurrentTileset());
        }
        else if (ae.getActionCommand().equals("importAllTileset"))
        {
            importAllTileset(getCurrentTileset());

            updatePaletteDisplay();
            updateTileGraphicsEditor();
            updateTileSelector();
            resetArrangementUndo();
            updateCollisionEditor();
            updateArrangementSelector();
            updateArrangementEditor();
        }
        else if (ae.getActionCommand().equals("exportAllTileset"))
        {
            exportAllTileset(getCurrentTileset());
        }
        else if (ae.getActionCommand().equals("importAll"))
        {
            importAll();

            updatePaletteDisplay();
            updateTileGraphicsEditor();
            updateTileSelector();
            resetArrangementUndo();
            updateCollisionEditor();
            updateArrangementSelector();
            updateArrangementEditor();
        }
        else if (ae.getActionCommand().equals("exportAll"))
        {
            exportAll();
        }
        // focus stuff
        else if (ae.getActionCommand().equals("bgeFocus"))
        {
            setFocus(this.tileDrawingArea);
        }
        else if (ae.getActionCommand().equals("fgeFocus"))
        {
            setFocus(this.tileForegroundDrawingArea);
        }
        else if (ae.getActionCommand().equals("colFocus"))
        {
            setFocus(this.collisionEditor);
        }
        else if (ae.getActionCommand().equals("arrFocus"))
        {
            setFocus(this.arrangementEditor);
        }
        else if (ae.getActionCommand().equals("cycFocus"))
        {
            cycleFocus();
        }
        else
        {
            System.err.println("Uncaught action command in eb.TileEditor: "
                + ae.getActionCommand());
        }
    }

    // update/redraw methods
    private void updatePaletteSelector()
    {
        paletteSelector.removeActionListener(this);
        paletteSelector.removeAllItems();
        for (int i = 0; i < getSelectedTileset().getPaletteCount(); i++)
        {
            paletteSelector.addItem(getSelectedTileset().getPalette(i)
                .toString());
        }
        paletteSelector.addActionListener(this);
    }

    private void updateTileSelector()
    {
        tileSelector.repaint();
        if (cbsel != null)
            cbsel.repaint();
    }

    private void updateArrangementSelector()
    {
        arrangementSelector.repaint();
    }

    private void updateCollisionEditor()
    {
        collisionEditor.updateTfs();
    }

    private void updateArrangementEditor()
    {
        arrangementEditor.repaint();
    }

    private void updatePaletteDisplay()
    {
        this.tileDrawingPalette
            .setPalette(getSelectedTileset().getPaletteColors(
                getCurrentPalette(), getCurrentSubPalette(), true));
        this.tileDrawingPalette.repaint();
    }

    private void updateTileGraphicsEditor()
    {
        // this.tileDrawingArea.setImage(
        // getSelectedTileset().getTileImage(
        // getCurrentTile(),
        // getCurrentPalette(),
        // getCurrentSubPalette(),
        // false));
        this.tileDrawingArea.setImage(getSelectedTileset().getTile(
            getCurrentTile()));
        // this.tileForegroundDrawingArea.setImage(
        // getSelectedTileset().getTileImage(
        // getCurrentTile() ^ 512,
        // getCurrentPalette(),
        // getCurrentSubPalette(),
        // false));
        this.tileForegroundDrawingArea
            .setEnabled(((getCurrentTile() & 511) < 384));
        this.tileForegroundDrawingArea.setImage(getSelectedTileset().getTile(
            getCurrentTile() ^ 512));
    }

    private void resetArrangementUndo()
    {
        collisionEditor.resetUndo();
        arrangementEditor.resetUndo();
    }

    // getCurrent...() methods
    private int getCurrentTileset()
    {
        return this.tilesetSelector.getSelectedIndex();
    }

    private Tileset getSelectedTileset()
    {
        return tilesets[getCurrentTileset()];
    }

    private int getCurrentPalette()
    {
        return getSelectedTileset().getPaletteNum(
            this.paletteSelector.getSelectedItem().toString());
    }

    private int getCurrentSubPalette()
    {
        return this.subPaletteSelector.getSelectedIndex();
    }

    private int getCurrentTile()
    {
        return this.tileSelector.getCurrentTile();
        // that and that + 512 are current
    }

    private int getCurrentArrangement()
    {
        return this.arrangementSelector.getCurrentArrangement();
    }

    private CopyAndPaster getCurrentCopyAndPaster()
    {
        return (CopyAndPaster) drawingAreaFocusIndicator.getCurrentFocus();
    }

    private Undoable getCurrentUndoable()
    {
        return (Undoable) drawingAreaFocusIndicator.getCurrentFocus();
    }

    private Component getCurrentComponent()
    {
        return drawingAreaFocusIndicator.getCurrentFocus();
    }

    // focus changing
    private void setFocus(Component focusedComponent)
    {
        drawingAreaFocusIndicator.setFocus(focusedComponent);
        drawingAreaFocusIndicator.repaint();
        arrangementFocusIndicator.setFocus(focusedComponent);
        arrangementFocusIndicator.repaint();
    }

    private int getFocusNum()
    {
        Component c = getCurrentComponent();
        if (c == this.tileDrawingArea)
            return 1;
        if (c == this.tileForegroundDrawingArea)
            return 2;
        if (c == this.arrangementEditor)
            return 3;
        if (c == this.collisionEditor)
            return 4;
        return 0;
    }

    private void setFocus(int f)
    {
        switch (f)
        {
            case 2:
                setFocus(this.tileForegroundDrawingArea);
                break;
            case 3:
                setFocus(this.arrangementEditor);
                break;
            case 4:
                setFocus(this.collisionEditor);
                break;
            case 1:
            default:
                setFocus(this.tileDrawingArea);
        }
    }

    private void cycleFocus()
    {
        setFocus(getFocusNum() + 1);
    }

    // copy and paste both stuff
    private class Paster
    {
        private byte[][] data1, data2;
        private short[][] arrData;
        private boolean graphicsPaste;

        public Paster(boolean gr, byte[][] data1, byte[][] data2)
        {
            this.graphicsPaste = gr;
            this.data1 = data1;
            this.data2 = data2;
        }

        public Paster(boolean gr, byte[][] data1, short[][] data2)
        {
            this.graphicsPaste = gr;
            this.data1 = data1;
            this.arrData = data2;
        }

        public void paste()
        {
            if (graphicsPaste)
            {
                getSelectedTileset().setTile(getCurrentTile(), data1);
                getSelectedTileset().setTile(getCurrentTile() | 512, data2);
                updateTileGraphicsEditor();
                updateTileSelector();
                updateArrangementEditor();
                updateArrangementSelector();
            }
            else
            {
                getSelectedTileset().setCollisionData(getCurrentArrangement(),
                    data1);
                updateCollisionEditor();
                getSelectedTileset().setArrangementData(
                    getCurrentArrangement(), arrData);
                updateArrangementEditor();
                updateArrangementSelector();
            }
        }
    }
    private Paster arrpaster = null, grpaster = null;

    private void cutBoth()
    {
        copyBoth();
        deleteBoth();
    }

    private void copyBoth()
    {
        if (getCurrentCopyAndPaster() instanceof IntArrDrawingArea)
            grpaster = new Paster(true, getSelectedTileset().getTile(
                getCurrentTile()), getSelectedTileset().getTile(
                getCurrentTile() | 512));
        else
            arrpaster = new Paster(false, getSelectedTileset()
                .getCollisionData(getCurrentArrangement()),
                getSelectedTileset()
                    .getArrangementData(getCurrentArrangement()));
    }

    private void pasteBoth()
    {
        try
        {
            if (getCurrentCopyAndPaster() instanceof IntArrDrawingArea)
                grpaster.paste();
            else
                arrpaster.paste();
        }
        catch (NullPointerException e)
        {}
    }

    private void deleteBoth()
    {
        if (getCurrentCopyAndPaster() instanceof IntArrDrawingArea)
        {
            tileDrawingArea.delete();
            tileForegroundDrawingArea.delete();
        }
        else
        {
            collisionEditor.delete();
            arrangementEditor.delete();
        }
    }

    /** Initialize the clipboard dialog window. */
    private void initCbDia()
    {
        cb = new byte[256][2][8][8];
        cbdia = new JDialog(mainWindow, "Tile Editor Clipboard");
        JButton copy = new JButton("Copy to clipboard"), paste = new JButton(
            "Paste from clipboard");
        copy.setActionCommand("cb_copy");
        copy.addActionListener(this);
        paste.setActionCommand("cb_paste");
        paste.addActionListener(this);
        cbdia.getContentPane().setLayout(new BorderLayout());
        cbdia.getContentPane().add(cbsel = new MinitileSelector()
        {
            public int getTileCount()
            {
                return cb.length;
            }

            public int getTilesWide()
            {
                return 16;
            }

            public Image getTileImage(int i)
            {
                return drawImage(cb[i][0], getSelectedTileset()
                    .getPaletteColors(getCurrentPalette(),
                        getCurrentSubPalette()));
            }
        }, BorderLayout.CENTER);
        cbdia.getContentPane().add(
            createFlowLayout(new JButton[]{copy, paste}), BorderLayout.SOUTH);
        cbdia.pack();
    }

    // import and export stuff
    public static void importTile(int tileset, int tile, File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] cbuf = new char[(int) f.length()];
            in.read(cbuf);
            in.close();
            String tmp[] = new String(cbuf).split("\n");
            tilesets[tileset].setTileAsString(tile, tmp[0]);
            tilesets[tileset].setTileAsString(tile ^ 512, tmp[1]);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to find file to import tile from.");
        }
        catch (IOException e)
        {
            System.err.println("Error reading file to import tile from.");
        }
    }

    public static void exportTile(int tileset, int tile, File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            out.write(tilesets[tileset].getTileAsString(tile));
            out.write("\n");
            out.write(tilesets[tileset].getTileAsString(tile ^ 512));
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("Error writing file to export tile to.");
        }
    }

    public static void importTileset(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] cbuf = new char[(int) f.length()];
            in.read(cbuf);
            in.close();
            tilesets[tileset].setTilesetAsString(new String(cbuf));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to find file to import tileset from.");
        }
        catch (IOException e)
        {
            System.err.println("Error reading file to import tileset from.");
        }
    }

    public static void exportTileset(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            out.write(tilesets[tileset].getTilesetAsString());
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("Error writing file to export tileset to.");
        }
    }

    public static void importCollision(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] cbuf = new char[(int) f.length()];
            in.read(cbuf);
            in.close();
            tilesets[tileset].setCollisionsAsString(new String(cbuf));
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("Unable to find file to import tileset properties from.");
        }
        catch (IOException e)
        {
            System.err
                .println("Error reading file to import tileset properties from.");
        }
    }

    public static void exportCollision(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            out.write(tilesets[tileset].getCollisionsAsString());
            out.close();
        }
        catch (IOException e)
        {
            System.err
                .println("Error writing file to export tileset properties to.");
        }
    }

    public static void importAllTileset(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] cbuf = new char[(int) f.length()];
            in.read(cbuf);
            in.close();
            tilesets[tileset].setAllDataAsString(new String(cbuf));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to find file to import tileset from.");
        }
        catch (IOException e)
        {
            System.err.println("Error reading file to import tileset from.");
        }
    }

    public static void exportAllTileset(int tileset, File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            out.write(tilesets[tileset].getAllDataAsString());
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("Error writing file to export tileset to.");
        }
    }

    public static void importAll(File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] cbuf = new char[(int) f.length()];
            in.read(cbuf);
            in.close();
            String[] tils = new String(cbuf).split("\n\n\n\n");
            for (int i = 0; i < tils.length; i++)
            {
                tilesets[i].init();
                tilesets[i].setAllDataAsString(tils[i]);
            }
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to find file to import tilesets from.");
        }
        catch (IOException e)
        {
            System.err.println("Error reading file to import tilesets from.");
        }
    }

    public static void exportAll(File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            for (int i = 0; i < tilesets.length; i++)
            {
                tilesets[i].init();
                out.write(tilesets[i].getAllDataAsString());
                if (i != tilesets.length - 1)
                    out.write("\n\n\n\n");
            }
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("Error writing file to export tilesets to.");
        }
    }

    public static void exportPalette(int tileset, int palette, File f)
    {
        if (f == null)
            return;
        try
        {
            FileWriter out = new FileWriter(f);
            out.write(tilesets[tileset].getPaletteAsString(palette));
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("Error writing file to export palette to.");
        }
    }

    public static void importPalette(int tileset, int palette, File f)
    {
        if (f == null)
            return;
        try
        {
            FileReader in = new FileReader(f);
            char[] c = new char[290];
            in.read(c);
            in.close();
            tilesets[tileset].setPaletteAsString(new String(c), palette);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to find file to import palette from.");
        }
        catch (IOException e)
        {
            System.err.println("Error reading file to import palette from.");
        }
    }

    public static void importTile(int tileset, int tile)
    {
        importTile(tileset, tile, getFile(false, "mtl", "MiniTiLe"));
    }

    public static void exportTile(int tileset, int tile)
    {
        exportTile(tileset, tile, getFile(true, "mtl", "MiniTiLe"));
    }

    public static void importTileset(int tileset)
    {
        importTileset(tileset, getFile(false, "mts", "MiniTileSet"));
    }

    public static void exportTileset(int tileset)
    {
        exportTileset(tileset, getFile(true, "mts", "MiniTileSet"));
    }

    public static void importCollision(int tileset)
    {
        importCollision(tileset, getFile(false, "tsp", "TileSet Properties"));
    }

    public static void exportCollision(int tileset)
    {
        exportCollision(tileset, getFile(true, "tsp", "TileSet Properties"));
    }

    public static void importAllTileset(int tileset)
    {
        importAllTileset(tileset, getFile(false, "fts", "Full TileSet"));
    }

    public static void exportAllTileset(int tileset)
    {
        exportAllTileset(tileset, getFile(true, "fts", "Full TileSet"));
    }

    public static void importAll()
    {
        importAll(getFile(false, "ats", "All full TileSets"));
    }

    public static void exportAll()
    {
        exportAll(getFile(true, "ats", "All full TileSets"));
    }

    public static void importPalette(int tileset, int palette)
    {
        importPalette(tileset, palette,
            getFile(false, "tpa", "Tileset PAlette"));
    }

    public static void exportPalette(int tileset, int palette)
    {
        exportPalette(tileset, palette, getFile(true, "tpa", "Tileset PAlette"));
    }
}