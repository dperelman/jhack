/*
 * Created on Apr 3, 2005
 */
package net.starmen.pkhack.eb;

public class TileRef
{
    private int tile;
    private boolean vFlip, hFlip;
    private byte subpal;

    public boolean isHFlip()
    {
        return hFlip;
    }

    public int getTile()
    {
        return tile;
    }

    public boolean isVFlip()
    {
        return vFlip;
    }

    public int getSubPal()
    {
        return subpal;
    }

    public int getArrangementData()
    {
        return (tile & 0x03ff) | ((subpal & 7) << 10) | (hFlip ? 0x4000 : 0)
            | (vFlip ? 0x8000 : 0);
    }

    public TileRef(int tile, boolean hFlip, boolean vFlip, int subpal)
    {
        this.tile = tile;
        this.vFlip = vFlip;
        this.hFlip = hFlip;
        this.subpal = (byte) subpal;
    }

    public TileRef(int tile, boolean hFlip, boolean vFlip)
    {
        this(tile, hFlip, vFlip, 0);
    }

    public TileRef(int tile)
    {
        this(tile, false, false);
    }
}