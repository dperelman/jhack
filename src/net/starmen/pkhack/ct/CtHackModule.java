/*
 * Created on Feb 27, 2004
 */
package net.starmen.pkhack.ct;

import java.io.EOFException;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Base class for all Chrono Trigger-only {@link net.starmen.pkhack.HackModule}'s.
 * 
 * @author AnyoneEB
 */
public abstract class CtHackModule extends HackModule
{
    /**
     * @param rom
     * @param prefs
     */
    public CtHackModule(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    public boolean isRomSupported()
    {
        return rom.getRomType().equals("Chrono Trigger");
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
