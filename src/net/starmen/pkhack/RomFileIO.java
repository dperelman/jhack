/*
 * Created on May 25, 2003
 */
package net.starmen.pkhack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Wrapper class for an Earthbound ROM that uses direct file i/o.
 * 
 * @author AnyoneEB
 */
public class RomFileIO extends AbstractRom
{
    RandomAccessFile rom;

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#readFromRom(java.io.File)
     */
    protected void readFromRom(File rompath) throws FileNotFoundException,
        IOException
    {
        rom = new RandomAccessFile(rompath, "rwd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#read(int)
     */
    public int read(int offset)
    {
        try
        {
            if (offset >= this.length()) //don't write past the end of the ROM
            {
                //			System.out.println(
                //				"Attempted read past end of rom, (0x"
                //					+ Integer.toHexString(offset)
                //					+ ")");
                return -1;
            }
            rom.seek((long) offset);
            return rom.readUnsignedByte();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public byte[] readByte(int offset, int length)
    {
        try
        {
            rom.seek((long) offset);
            byte[] out = new byte[length];
            rom.read(out);
            return out;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] readByteSeek(int length)
    {
        try
        {
            byte[] out = new byte[length];
            rom.read(out);
            return out;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#write(int, int)
     */
    public void write(int offset, int arg)
    {
        try
        {
            if (offset >= rom.length())
                return;
            rom.seek((long) offset);
            rom.writeByte(arg);

            if (getRomType().equals("Earthbound") && length() == 0x600200
                && offset >= 0x008200 && offset < 0x009200)
                write(offset + 0x400000, arg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void write(int offset, byte[] arg, int len)
    {
        try
        {
            if (offset >= rom.length())
                return;
            rom.seek((long) offset);
            rom.write(arg, 0, len);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void writeSeek(byte[] arg, int len)
    {
        try
        {
            rom.write(arg, 0, len);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#readSeek()
     */
    public int readSeek()
    {
        try
        {
            return rom.readUnsignedByte();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#seek(int)
     */
    public void seek(int offset)
    {
        try
        {
            rom.seek(offset);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#writeSeek(int)
     */
    public void writeSeek(int arg)
    {
        try
        {
            rom.write(arg);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#saveRom(java.io.File)
     */
    public boolean saveRom(File rompath)
    {
        if (!this.isLoaded) //don't try to save if nothing is loaded
        {
            return false;
        }

        //ensure mirror for ExHiRom
        if (length() == 0x600200)
            write(0x408200, readByte(0x008200, 0x8000));
        if (rompath != super.path)
        {
            try
            {
                RandomAccessFile newRom = new RandomAccessFile(rompath, "rwd");
                rom.seek(0);
                newRom.seek(0);
                byte[] b = new byte[(int) rom.length()];
                rom.read(b);
                newRom.write(b);
                rom = newRom;

                path = rompath;
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            setDefaultDir(rompath.getParent());
        }

        return true;
    }

    protected boolean _expand()
    {
        try
        {
            rom.seek(rom.length());
            byte[] b = new byte[1 << 20]; //1 mebibyte
            for (int j = 0; j < 4096; j++)
                b[(j * 256) + 255] = 2;
            rom.write(b);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected boolean _expandEx()
    {
        try
        {
            rom.seek(rom.length());
            byte[] b = new byte[2 << 20]; //2 mebibytes
            System.arraycopy(readByte(0x008200, 0x8000), 0, b, 0x8000, 0x8000);
            rom.write(b);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#length()
     */
    public int length()
    {
        try
        {
            return (int) rom.length();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.AbstractRom#isDirectFileIO()
     */
    public boolean isDirectFileIO()
    {
        return true;
    }
}