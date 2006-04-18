/*
 * Created on Apr 17, 2006
 */
package net.starmen.pkhack.eb;

import java.awt.Color;
import java.awt.Image;

import net.starmen.pkhack.HackModule;

public abstract class FullScreenGraphics
{
    protected boolean isInited = false;

    /** The <code>Color<code>'s of each palette. */
    protected Color[][] palette;
    /** List of all arrangements. */
    protected short[] arrangementList;
    /** Two-dimentional array of arrangements used. */
    protected short[][] arrangement;
    /** All tiles stored as pixels being found at [tile_num][x][y]. */
    protected byte[][][] tiles;

    public abstract int getNumSubPalettes();

    public abstract int getSubPaletteSize();

    public abstract int getNumArrangements();

    public abstract int getNumTiles();

    /**
     * Decompresses information from ROM. allowFailure defaults to true, and is
     * set to false when "fail" is selected from the abort/retry/fail box
     * presented to the user when a problem is encountered while reading.
     * 
     * @param allowFailure if true, false will not be returned on failure,
     *            instead the failed item will be set to zeros and reading will
     *            continue
     * @return true if everything is read or if allowFailure is true, false if
     *         any decompression failed and allowFailure is false
     */
    public abstract boolean readInfo(boolean allowFailure);

    public boolean readInfo()
    {
        return readInfo(false);
    }

    /**
     * Inits all values to zero. Will have no effect if {@link #readInfo()} or
     * this has already been run successfully. Use this if
     * <code>readInfo()</code> always fails.
     */
    public void initToNull()
    {
        readInfo(true);
    }

    public abstract boolean writeInfo();

    /**
     * @return Returns <code>true</code> if this has been initialized with
     *         data from the ROM.
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
    public void setArrangementData(int x, int y, short data)
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
    public short getArrangementData(int x, int y)
    {
        readInfo();
        return arrangement[x][y];
    }

    /**
     * TODO Write javadoc for this method
     * 
     * @return
     */
    public short[][] getArrangementData()
    {
        readInfo();
        short[][] out = new short[arrangement.length][arrangement[0].length];
        for (int x = 0; x < out.length; x++)
            for (int y = 0; y < out[0].length; y++)
                out[x][y] = arrangement[x][y];
        return out;
    }

    public short[] getArrangementArr()
    {
        readInfo();
        int j = 0;
        short out[] = new short[getNumArrangements()];
        for (int y = 0; y < arrangement[0].length; y++)
            for (int x = 0; x < arrangement.length; x++)
                out[j++] = arrangement[x][y];
        for (; j < getNumArrangements(); j++)
            out[j] = arrangementList[j];
        return out;
    }

    public void setArrangementArr(short[] arr)
    {
        readInfo();
        int j = 0;
        for (int y = 0; y < arrangement[0].length; y++)
            for (int x = 0; x < arrangement.length; x++)
                arrangement[x][y] = arr[j++];
        for (; j < getNumArrangements(); j++)
            arrangementList[j] = arr[j];
    }

    /**
     * TODO Write javadoc for this method
     * 
     * @param data
     */
    public void setArrangementData(short[][] data)
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
    public Image getTileImage(int tile, int subPal, boolean hFlip, boolean vFlip)
    {
        readInfo();
        return HackModule.drawImage(tiles[tile], palette[subPal], hFlip, vFlip);
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

    /**
     * TODO Write javadoc for this method
     * 
     * @param pal
     * @return
     */
    public Color[] getSubPal(int pal)
    {
        readInfo();
        Color[] out = new Color[getSubPaletteSize()];
        System.arraycopy(palette[pal], 0, out, 0, getSubPaletteSize());
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
    protected byte getTilePixel(int tile, int x, int y)
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
    protected void setTilePixel(int tile, int x, int y, byte i)
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

    /**
     * TODO Write javadoc for this method
     * 
     * @param i
     * @param paltmp
     */
    public void setSubPal(int i, Color[] paltmp)
    {
        if (paltmp.length >= getSubPaletteSize() && i < getNumSubPalettes()
            && i >= 0)
            System.arraycopy(paltmp, 0, palette[i], 0, getSubPaletteSize());
    }
}
