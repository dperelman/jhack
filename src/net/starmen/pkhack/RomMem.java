/*
 * Created on Aug 19, 2004
 */
package net.starmen.pkhack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class RomMem extends Rom
{
    /**
     * Contains the loaded ROM. It is perfered that you don't access this
     * directly.
     * 
     * @see #write(int, int)
     * @see #read(int)
     */
    private byte[] rom;

    protected void readFromRom(File rompath) throws FileNotFoundException,
        IOException
    {
        this.rom = new byte[(int) rompath.length()];
        FileInputStream in = new FileInputStream(rompath);
        in.read(rom);
        in.close();
    }

    public boolean saveRom(File rompath)
    {
        if (!this.isLoaded) //don't try to save if nothing is loaded
        {
            return false;
        }
        this.path = rompath;
        setDefaultDir(rompath.getParent());

        try
        {
            FileOutputStream out = new FileOutputStream(rompath);
            out.write(this.rom);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error: File not saved: File not found.");
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            System.err.println("Error: File not saved: Could write file.");
            e.printStackTrace();
            return false;
        }
        System.out.println("Saved ROM: " + this.path.length() + " bytes");
        saveRomType();
        return true;
    }

    public void write(int offset, int arg)
    {
        if (offset > this.rom.length) //don't write past the end of the ROM
        {
            return;
        }

        this.rom[offset] = (byte) (arg & 255);
    }

    public void write(int offset, byte[] arg, int len)
    {
        //OK to use this instead of write()?
        System.arraycopy(arg, 0, rom, offset, len);
    }

    public int read(int offset)
    {
        if ((offset & 0x7fffffff) >= this.length()) //don't write past the end
        // of the ROM
        {
            //			System.out.println(
            //				"Attempted read past end of rom, (0x"
            //					+ Integer.toHexString(offset)
            //					+ ")");
            return -1;
        }
        return this.rom[offset] & 255;
    }

    public byte[] readByte(int offset, int length)
    {
        byte[] returnValue = new byte[length];
        try
        {
            //OK to not end up going to read function?
            System.arraycopy(rom, offset, returnValue, 0, length);
        }
        catch (IndexOutOfBoundsException e)
        {
            return null;
        }
        return returnValue;
    }

    public void resetArea(int offset, int len, Rom orgRom)
    {
        //only works if neither is direct file IO
        if (orgRom instanceof RomMem)
            System.arraycopy(((RomMem) orgRom).rom, offset, rom, offset, len);
        //otherwise, use normal methods to read/write
        else
            super.resetArea(offset, len, orgRom);
    }

    //TODO check _expand()
    protected boolean _expand()
    {
        int rl = length();
        byte[] out = new byte[rl + (4096 * 256)];
        //        for (int i = 0; i < rl; i++)
        //        {
        //            out[i] = (byte) read(i);
        //        }
        System.arraycopy(rom, 0, out, 0, rl);
        for (int j = 0; j < 4096; j++)
        {
            //            for (int i = 0; i < 255; i++)
            //            {
            //                out[((j * 256) + i) + rl] = 0;
            //            }
            Arrays.fill(out, (j * 256) + rl, (j * 256) + rl + 255, (byte) 0);
            out[((j * 256) + 255) + rl] = 2;
        }

        rom = out;

        return true;
    }

    public int length()
    {
        return rom.length;
    }

    public IPSFile createIPS(Rom orgRom, int start, int end)
    {
        if (orgRom instanceof RomMem)
        {
            return IPSFile.createIPS(this.rom, ((RomMem) orgRom).rom, start,
                end);
        }
        else
        {
            IPSFile out = new IPSFile();

            int cStart = -1;
            int len = end - start + 1;

            for (int i = 0; i < len; i++)
            {
                if (rom[i] != orgRom.readByte(i + start))
                {
                    if (cStart == -1)
                        cStart = i;
                }
                else
                {
                    if (cStart != -1)
                    {
                        out.addRecord(cStart + start, ByteBlock.wrap(rom,
                            cStart, i - cStart));
                        cStart = -1;
                    }
                }
            }

            return out;
        }
    }

    public boolean apply(IPSFile ips)
    {
        try
        {
            ips.apply(this.rom);
            return true;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return false;
        }
    }

    public boolean unapply(IPSFile ips, Rom orgRom)
    {
        if (orgRom instanceof RomMem)
        {
            try
            {
                ips.unapply(this.rom, ((RomMem) orgRom).rom);
                return true;
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                return false;
            }
        }
        else
        {
            return super.unapply(ips, orgRom);
        }
    }
    
    public boolean check(IPSFile ips)
    {
        return ips.check(rom);
    }
    
    public boolean isDirectFileIO()
    {
        return false;
    }
}