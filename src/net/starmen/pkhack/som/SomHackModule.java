/*
 * Created on Feb 27, 2004
 */
package net.starmen.pkhack.som;

import java.io.EOFException;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Base class for all Secret of Mana-only {@link net.starmen.pkhack.HackModule}'s.
 * 
 * @author AnyoneEB
 */
public abstract class SomHackModule extends HackModule
{
    /**
     * @param rom
     * @param prefs
     */
    public SomHackModule(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    public boolean isRomSupported()
    {
        return rom.getRomType().equals("Secret of Mana");
    }

    /**
     * WARNING: This method not implemented!
     */
    public int findFreeRange(int startAt, int length) throws EOFException
    {
        throw new UnsupportedOperationException(
            "findFreeRange(int, int) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public void nullifyArea(int address, int len)
    {
        throw new UnsupportedOperationException(
            "nullifyArea(int, int) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public boolean writeToFree(byte[] data, int pointerLoc, int oldLen,
        int newLen)
    {
        throw new UnsupportedOperationException(
            "writeToFree(byte[], int, int, int) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public char simpToGameChar(char regChr)
    {
        throw new UnsupportedOperationException(
            "simpToGameChar(char) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public char[] simpToGameString(char[] string)
    {
        throw new UnsupportedOperationException(
            "simpToGameString(char[]) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public char simpToRegChar(char gameChr)
    {
        throw new UnsupportedOperationException(
            "simpToRegChar(char) not implemented.");
    }

    /**
     * WARNING: This method not implemented!
     */
    public char[] simpToRegString(char[] string)
    {
        throw new UnsupportedOperationException(
            "simpToRegString(char[]) not implemented.");
    }
}
