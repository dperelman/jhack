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
public class RomFileIO extends Rom
{
    RandomAccessFile rom;

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#readFromRom(java.io.File)
     */
    protected void readFromRom(File rompath) throws FileNotFoundException,
        IOException
    {
        rom = new RandomAccessFile(rompath, "rwd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#read(int)
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
            return -1; }
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
     * @see net.starmen.pkhack.Rom#write(int, int)
     */
    public void write(int offset, int arg)
    {
        try
        {
            if (offset >= rom.length()) return;
            rom.seek((long) offset);
            rom.writeByte(arg);
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
            if (offset >= rom.length()) return;
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
     * @see net.starmen.pkhack.Rom#readSeek()
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
     * @see net.starmen.pkhack.Rom#seek(int)
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
     * @see net.starmen.pkhack.Rom#writeSeek(int)
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
     * @see net.starmen.pkhack.Rom#saveRom(java.io.File)
     */
    public boolean saveRom(File rompath)
    {
        if (!this.isLoaded) //don't try to save if nothing is loaded
        { return false; }

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

    public boolean expand()
    {
        if (getRomType() != "Earthbound" || length() == 0x400200) { return false; }

        try
        {
            rom.seek(rom.length());
            byte[] b = new byte[1 << 20];
            for (int j = 0; j < 4096; j++)
                b[(j * 256) + 255] = 2;
            rom.write(b);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        //        isExpanded = true;

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#length()
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
     * @see net.starmen.pkhack.Rom#isDirectFileIO()
     */
    public boolean isDirectFileIO()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#apply(net.starmen.pkhack.IPSFile)
     */
    public boolean apply(IPSFile ips)
    {
        try
        {
            for (int i = 0; i < ips.getRecordCount(); i++)
            {
                IPSFile.IPSRecord ipsr = ips.getRecord(i);
                if (ipsr.isInfoString())
                {
                    if (ipsr.getSize() > 0)
                    {
                        for (int j = 0; j < ipsr.getSize(); j++)
                        {
                            this.write(ipsr.getOffset() + j, (byte) ipsr
                                .getInfo().charAt(j));
                        }
                    }
                    else
                    {
                        for (int j = 0; j < ipsr.getRleSize(); j++)
                        {
                            this.write(ipsr.getOffset() + j, (byte) ipsr
                                .getRleInfo());
                        }
                    }
                }
                else
                {
                    if (ipsr.getSize() > 0)
                    {
                        for (int j = 0; j < ipsr.getSize(); j++)
                        {
                            this.write(ipsr.getOffset() + j, ipsr.getInfoBB()
                                .get(j));
                        }
                    }
                    else
                    {
                        for (int j = 0; j < ipsr.getRleSize(); j++)
                        {
                            this.write(ipsr.getOffset() + j, ipsr.getRleInfo());
                        }
                    }
                }
            }
            return true;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#apply(net.starmen.pkhack.IPSFile)
     */
    public boolean unapply(IPSFile ips, Rom orgRom)
    {
        try
        {
            for (int i = 0; i < ips.getRecordCount(); i++)
            {
                IPSFile.IPSRecord ipsr = ips.getRecord(i);
                if (ipsr.getSize() > 0)
                {
                    for (int j = 0; j < ipsr.getSize(); j++)
                    {
                        this.write(ipsr.getOffset() + j, orgRom.read(ipsr
                            .getOffset()
                            + j));
                    }
                }
                else
                {
                    for (int j = 0; j < ipsr.getRleSize(); j++)
                    {
                        this.write(ipsr.getOffset() + j, orgRom.read(ipsr
                            .getOffset()
                            + j));
                    }
                }
            }
            return true;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return false;
        }
    }

    public IPSFile createIPS(Rom orgRom, int start, int end)
    {
        IPSFile out = new IPSFile();

        int cStart = -1;
        int len = end - start + 1;

        byte[] romData = readByte(start, len);

        for (int i = 0; i < len; i++)
        {
            if (romData[i] != orgRom.readByte(i + start))
            {
                if (cStart == -1) cStart = i;
            }
            else
            {
                if (cStart != -1)
                {
                    out.addRecord(cStart + start, ByteBlock.wrap(romData,
                        cStart, i - cStart));
                    cStart = -1;
                }
            }
        }

        return out;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#createIPS(net.starmen.pkhack.Rom)
     */
    public IPSFile createIPS(Rom orgRom)
    {
        return this.createIPS(orgRom, 0, this.length());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.Rom#check(net.starmen.pkhack.IPSFile)
     */
    public boolean check(IPSFile ips)
    {
        for (int i = 0; i < ips.getRecordCount(); i++)
        {
            IPSFile.IPSRecord ipsr = ips.getRecord(i);
            if (ipsr.isInfoString())
            {
                if (ipsr.getSize() > 0)
                {
                    for (int j = 0; j < ipsr.getSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr.getInfo()
                            .charAt(j)) return false;
                    }
                }
                else
                {
                    for (int j = 0; j < ipsr.getRleSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr
                            .getRleInfo()) return false;
                    }
                }
            }
            else
            {
                if (ipsr.getSize() > 0)
                {
                    for (int j = 0; j < ipsr.getSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr.getInfoBB()
                            .get(j)) return false;
                    }
                }
                else
                {
                    for (int j = 0; j < ipsr.getRleSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr
                            .getRleInfo()) return false;
                    }
                }
            }
        }
        return true;
    }

}
