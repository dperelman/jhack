/*
 * Created on Aug 22, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.Graphics;
import java.awt.event.ActionEvent;

import net.starmen.pkhack.JHack;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public abstract class DoubleSelTileSelector extends TileSelector
{
    private boolean doubleSel = false;

    /**
     * Sets double selection. <strong>Only call this from
     * {@link #canDoubleSelect(int)}. </strong>
     * 
     * @param ds if true, two tiles are selected at once
     * @see #isDoubleSel()
     */
    protected void setDoubleSel(boolean ds)
    {
        doubleSel = ds;
    }

    /**
     * If double selection is true, the current tile and the tile
     * <code>getTilesWide()</code> after it are both selected. The first tile
     * should be shown above the second. Both will have the same palette (if
     * they do not, this would be false).
     * 
     * @return Returns the doubleSel.
     * @see #setDoubleSel(boolean)
     */
    public boolean isDoubleSel()
    {
        return doubleSel;
    }

    /**
     * Returns which subpal to use for the specified tile.
     * 
     * @param tile number of the tile to get subpal for
     * @return subpal number
     */
    public abstract int getSubPalNum(int tile);

    /**
     * Returns the name of the preference to allow double selection.
     * {@link net.starmen.pkhack.XMLPreferences#getValueAsBoolean(String)}is
     * used on this preference name; if true, double selection can occur; if
     * false, double selection marked tiles will not be treated differently.
     * 
     * @return name of the preference to allow double selection
     */
    public abstract String getDoubleSelPrefName();

    /**
     * Checks if the specified tile can be part of a double select. This sets
     * the value of isDoubleSelect() accordingly. If this finds that the
     * specified tile can be the bottom of a double select, it returns the index
     * of the top tile of the double select. The default implementation uses sub
     * palette numbers to check for double selection, another method may be
     * used, but any double selection must be one that would be selected by this
     * method. That is, the palettes of the two tiles selected must be the same.
     * 
     * @param tile tile to select
     * @return tile to select; if <code>isDoubleSel</code>, the top tile of
     *         the selection; if <code>!isDoubleSel</code> the only tile of
     *         the selection
     * @see #isDoubleSel()
     * @see #setDoubleSel(boolean)
     */
    protected int canDoubleSelect(int tile)
    {
        try
        {
            //if tile row number is even and has the same palette
            // as
            //the tile below it
            if (tile + getTilesWide() < getTileCount()
                && (tile / getTilesWide()) % 2 == 0
                && getSubPalNum(tile) == getSubPalNum(tile + getTilesWide()))
                setDoubleSel(true);
            else if (tile - getTilesWide() >= 0
                && getSubPalNum(tile) == getSubPalNum(tile - getTilesWide()))
            {
                tile -= getTilesWide();
                setDoubleSel(true);
            }
            else
                setDoubleSel(false);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            setDoubleSel(false);
        }
        return tile;
    }

    /**
     * Sets the current tile. This is visually show the specificed tile as
     * highlighted.
     * 
     * @param newTile tile to change selection to
     * @param force If true, <code>ActionListener</code>'s will be notified
     *            even if the current tile does not change.
     */
    public void setCurrentTile(int newTile, boolean force)
    {
        //only fire ActionPerformed if new tile
        if (newTile < getTileCount()
            && ((currentTile != newTile && (!isDoubleSel() || currentTile
                + getTilesWide() != newTile)) || force))
        {
            boolean curDoubleSel = doubleSel;
            if (JHack.main.getPrefs().getValueAsBoolean(getDoubleSelPrefName()))
                newTile = canDoubleSelect(newTile);
            else
                doubleSel = false;
            reHighlight(getCurrentTile(), curDoubleSel, newTile);
            currentTile = newTile;
            this.fireActionPerformed(new ActionEvent(this,
                ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
        }
    }

    public void setCurrentTile(int newTile)
    {
        setCurrentTile(newTile, false);
    }

    protected void reHighlight(int oldTile, boolean curDoubleSel, int newTile)
    {
        drawTile(oldTile);
        if (curDoubleSel)
            drawTile(oldTile + getTilesWide());
        highlightTile(newTile);
        if (isDoubleSel())
            highlightTile(newTile + getTilesWide());
    }

    public void repaintCurrent()
    {
        int c = getCurrentTile();
        drawTile(c);
        highlightTile(c);
        if (isDoubleSel())
        {
            c += getTilesWide();
            drawTile(c);
            highlightTile(c);
        }
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        if (isGuiInited() && isDoubleSel())
            highlightTile(g, getCurrentTile() + getTilesWide());
    }
}