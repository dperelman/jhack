package net.starmen.pkhack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Object representing a .ips patch file. Includes methods to make an
 * <code>IPSFile</code> from two <code>int[]</code>'s and to apply this
 * patch to a file loaded into an <code>int[]</code>. This does not load the
 * file from the file system.
 * 
 * @author AnyoneEB
 * @see IPSPatchMaker
 * @see IPSPatchApplier
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class IPSFile
{
    private List records = new ArrayList();

    /**
     * Creates an <code>IPSFile</code> that doesn't change anything.
     */
    public IPSFile()
    {}

    /**
     * Reads in a .ips file and creates an <code>IPSFile</code> from it.
     * 
     * @param in .ips file as an int[]
     */
    public IPSFile(int[] in)
    {
        int i = 5, size;
        while (!subString(in, i, i + 3).equals("EOF"))
        {
            if ((in.length - i) < 8) //not enough space for more records
            {
                return;
            }
            size = (in[i + 3] << 8) & 0xFF00;
            size += in[i + 4] & 0xFF;

            //			System.out.println("Size: " + size);

            if (size == 0)
            {
                //RLE record is 8 bytes: OF FS ET SI ZE RL ES CB
                addRecord(new IPSRecord(subString(in, i, (i + 8))));
                i += 8;
            }
            else
            {
                addRecord(new IPSRecord(subString(in, i, (i + 5 + size))));
                i += (5 + size);
            }
        }
    }

    /**
     * Reads in a .ips file and creates an <code>IPSFile</code> from it.
     * 
     * @param in .ips file as an int[]
     */
    public IPSFile(byte[] in)
    {
        int i = 5, size;
        while (!subString(in, i, i + 3).equals("EOF"))
        {
            if ((in.length - i) < 8) //not enough space for more records
            {
                return;
            }
            size = (in[i + 3] << 8) & 0xFF00;
            size += in[i + 4] & 0xFF;

            //			System.out.println("Size: " + size);

            if (size == 0)
            {
                //RLE record is 8 bytes: OF FS ET SI ZE RL ES CB
                addRecord(new IPSRecord(ByteBlock.wrap(in, i, 8)));
                //subString(in, i, (i + 8))));
                i += 8;
            }
            else
            {
                addRecord(new IPSRecord(ByteBlock.wrap(in, i, 5 + size)));
                //subString(in, i, (i + 5 + size))));
                i += (5 + size);
            }
        }
    }

    /**
     * Reads in a .ips file and returns an IPSFile object.
     * 
     * @param ips File to read in
     * @return A IPSFile representing the file on success or an empty IPSFile on
     *         failure
     */
    public static IPSFile loadIPSFile(File ips)
    {
        try
        {
            FileInputStream in = new FileInputStream(ips);
            byte[] b = new byte[(int) ips.length()];
            in.read(b);
            in.close();
            //			int[] ipsFile = new int[b.length];
            //			for (int i = 0; i < b.length; i++)
            //			{
            //				ipsFile[i] = (b[i] & 255);
            //			}

            return new IPSFile(b);
        }
        catch (FileNotFoundException e)
        {
            String errmsg = "File not found: " + ips.getAbsolutePath(), errtitle = "Error: File not loaded";
            JOptionPane.showMessageDialog(null, errmsg, errtitle,
                JOptionPane.ERROR_MESSAGE);
            System.out.println(errtitle + ": " + errmsg);
            //e.printStackTrace();
        }
        catch (IOException e)
        {
            String errmsg = "Could not read file: " + ips.getAbsolutePath()
                + "\n" + e.getClass() + ": " + e.getMessage(), errtitle = "Error: File not loaded";
            JOptionPane.showMessageDialog(null, errmsg, errtitle,
                JOptionPane.ERROR_MESSAGE);
            System.out.println(errtitle + ": " + errmsg);
            //e.printStackTrace();
        }
        return new IPSFile();
    }

    public void saveIPSFile(File ips)
    {
        try
        {
            FileOutputStream out = new FileOutputStream(ips);
            out.write("PATCH".getBytes());
            for (int i = 0; i < this.getRecordCount(); i++)
            {
                out.write(this.getRecord(i).toByteArr());
            }
            out.write("EOF".getBytes());
            out.close();
        }
        catch (FileNotFoundException e)
        {
            String errmsg = "File not found: " + ips.getAbsolutePath(), errtitle = "Error: File not saved";
            JOptionPane.showMessageDialog(null, errmsg, errtitle,
                JOptionPane.ERROR_MESSAGE);
            System.out.println(errtitle + ": " + errmsg);
        }
        catch (IOException e)
        {
            String errmsg = "Could not write to file: " + ips.getAbsolutePath()
                + "\n" + e.getClass() + ": " + e.getMessage(), errtitle = "Error: File not saved";
            JOptionPane.showMessageDialog(null, errmsg, errtitle,
                JOptionPane.ERROR_MESSAGE);
            System.out.println(errtitle + ": " + errmsg);
        }
    }

    private String subString(int[] in, int start, int end)
    {
        String out = new String();

        for (int i = start; i < end; i++)
        {
            out += (char) (in[i] & 255);
        }

        return out;
    }

    private String subString(byte[] in, int start, int end)
    {
        StringBuffer out = new StringBuffer(end - start + 1);

        for (int i = start; i < end; i++)
        {
            out.append((char) (in[i] & 255));
        }

        return out.toString();
    }

    /**
     * Compares two <code>int[]</code>'s and creates an <code>IPSFile</code>
     * from their differences. The two files must be the same size or it will
     * not work.
     * 
     * @param file1 The file that has been changed.
     * @param file2 The orginal file. The file this patch will be applied to.
     * @param start Offset to start looking for differences.
     * @param end Offset to stop looking for differences.
     * @return A new <code>IPSFile</code> based on <code>file1</code> and
     *         <code>file2</code>
     * @see #createIPS(int[], int[])
     */
    public static IPSFile createIPS(int[] file1, int[] file2, int start, int end)
    {
        // Creates an IPS file by comparing two files.
        // file1 is the modified file, file2 is the orginal file

        //System.out.println("Creating a .IPS file");

        IPSFile out = new IPSFile();

        String temp = new String();

        for (int i = start; i < end; i++)
        {
            if (file1[i] != file2[i])
            {
                temp += (char) file1[i];
                //if(temp.length() % 1000 == 0)
                // {System.out.println(temp.length());}
            }
            else
            {
                if (temp.length() > 0)
                {
                    out.addRecord(i - temp.length(), temp);
                    temp = new String();
                }
            }
        }

        return out;
    }

    /**
     * Compares two <code>int[]</code>'s and creates an <code>IPSFile</code>
     * from their differences. The two files must be the same size or it will
     * not work.
     * 
     * @param file1 The file that has been changed.
     * @param file2 The orginal file. The file this patch will be applied to.
     * @return A new <code>IPSFile</code> based on <code>file1</code> and
     *         <code>file2</code>
     * @see #createIPS(int[], int[], int, int)
     */
    public static IPSFile createIPS(int[] file1, int[] file2)
    {
        return createIPS(file1, file2, 0, file1.length);
    }

    /**
     * Compares two <code>byte[]</code>'s and creates an <code>IPSFile</code>
     * from their differences. The two files must be the same size or it will
     * not work.
     * 
     * @param file1 The file that has been changed.
     * @param file2 The orginal file. The file this patch will be applied to.
     * @param start Offset to start looking for differences inclusive.
     * @param end Offset to stop looking for differences exclusive.
     * @return A new <code>IPSFile</code> based on <code>file1</code> and
     *         <code>file2</code>
     * @see #createIPS(byte[], byte[])
     */
    public static IPSFile createIPS(byte[] file1, byte[] file2, int start,
        int end)
    {
        // Creates an IPS file by comparing two files.
        // file1 is the modified file, file2 is the orginal file

        //System.out.println("Creating a .IPS file");
        if (start > file1.length - 1 || start > file2.length - 1
            || end > file1.length || end > file2.length)
        {
            JOptionPane.showMessageDialog(null,
                "An IPS file cannot be made for this range\n"
                    + "because it is past the end of either the\n"
                    + "original or modified file.\n" + "\n" + "The range is ["
                    + start + ", " + end + ") in decimal,\n" + "or [0x"
                    + Integer.toHexString(start) + ", 0x"
                    + Integer.toHexString(end) + ") in hex.",
                "Error: Illegal Range", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        IPSFile out = new IPSFile();

        //String temp = new String();
        int cStart = -1;

        for (int i = start; i < end; i++)
        {
            if (file1[i] != file2[i])
            {
                //temp += (char) file1[i];
                if (cStart == -1)
                    cStart = i;
            }
            else
            {
                //if (temp.length() > 0)
                if (cStart != -1)
                {
                    //out.addRecord(i - temp.length(), temp);
                    out.addRecord(cStart, ByteBlock.wrap(file1, cStart, i
                        - cStart));
                    //temp = new String();
                    cStart = -1;
                }
            }
        }

        return out;
    }

    /**
     * Compares two <code>byte[]</code>'s and creates an <code>IPSFile</code>
     * from their differences. The two files must be the same size or it will
     * not work.
     * 
     * @param file1 The file that has been changed.
     * @param file2 The orginal file. The file this patch will be applied to.
     * @return A new <code>IPSFile</code> based on <code>file1</code> and
     *         <code>file2</code>
     * @see #createIPS(byte[], byte[], int, int)
     */
    public static IPSFile createIPS(byte[] file1, byte[] file2)
    {
        return createIPS(file1, file2, 0, file1.length);
    }

    private void addRecord(IPSRecord record)
    {
        //		System.out.println("Debug: adding IPSRecord #" + (records.length + 1)
        // +
        //			": offset: " + record.offset +
        //			" size: " + record.size +
        //			" data: " + record.info);
        /*
         * IPSRecord[] newRecords = new IPSRecord[this.getRecordCount() + 1];
         * for (int i = 0; i < this.getRecordCount(); i++) { newRecords[i] =
         * records[i]; } newRecords[this.records.length] = record; this.records =
         * newRecords;
         */
        this.records.add(record);
    }

    /**
     * Adds a record to this representing that <code>info</code> should be at
     * <code>offset</code>. This automatically figures out compression and
     * makes sure the record isn't longer than allowed, so it may actually add
     * more than one record.
     * 
     * @param offset Where the change is.
     * @param info What it should be changed to.
     */
    public void addRecord(int offset, String info)
    {
        if (info.length() == 0)
        {
            return;
        }
        char chr = info.charAt(0);
        int streak = 1;

        for (int i = 1; i < info.length(); i++)
        {
            if (i == 0xFFFF) //max record length
            {
                this.addRecord(new IPSRecord(offset, info.substring(0, i)));
                if (info.length() >= i) //if there's more.
                {
                    this.addRecord(offset + i, info.substring(i));
                }
                return;
            }
            else if (info.charAt(i) == chr)
            {
                streak++;
            }
            else
            //different char
            {
                if (streak > 12)
                //if the steak is long enough to make compression worth it
                {
                    this.addRecord(new IPSRecord(offset, info.substring(0, i
                        - streak)));
                    //make uncompressed record for info before streak
                    this.addRecord(new IPSRecord(offset + (i - streak), streak,
                        info.charAt(i - 1)));
                    //make compressed record of streak
                    if (info.length() >= i) //if there's more...
                    {
                        this.addRecord(offset + i, info.substring(i));
                        //add records for after compressed part
                    }
                    return;
                }
                else
                {
                    chr = info.charAt(i);
                    streak = 1;
                }
            }
        }

        addRecord(new IPSRecord(offset, info));
    }

    /**
     * Adds a record to this representing that <code>info</code> should be at
     * <code>offset</code>. Works the about same as the eqivlent method for
     * <code>String</code>, but is more efficent in time and space. CAREFUL:
     * ByteBlock are pointers to data NOT copies!
     * 
     * @see #addRecord(int, String)
     * @param offset Where the change is.
     * @param info What it should be changed to.
     */
    public void addRecord(int offset, ByteBlock info)
    {
        int i = 0, tmp;
        while ((tmp = _addRecord(offset + i, info.subBlock(i))) != -1)
            i += tmp;
    }

    private int _addRecord(int offset, ByteBlock info)
    {
        if (info.getLength() == 0)
        {
            return -1;
        }
        byte rle = info.get(0);
        int streak = 1;

        for (int i = 1; i < info.getLength(); i++)
        {
            if (i == 0xFFFF) //max record length
            {
                this.addRecord(new IPSRecord(offset, info.subBlock(0, i)));
                if (info.getLength() >= i) //if there's more.
                {
                    //this.addRecord(offset + i, info.subBlock(i));
                    return i;
                }
                return -1;
            }
            else if (info.get(i) == rle)
            {
                streak++;
            }
            else
            //different char
            {
                if (streak > 12)
                //if the steak is long enough to make compression worth it
                {
                    this.addRecord(new IPSRecord(offset, info.subBlock(0, i
                        - streak)));
                    //make uncompressed record for info before streak
                    this.addRecord(new IPSRecord(offset + (i - streak), streak,
                        info.get(i - 1)));
                    //make compressed record of streak
                    if (info.getLength() >= i) //if there's more...
                    {
                        //this.addRecord(offset + i, info.subBlock(i));
                        //add records for after compressed part
                        return i;
                    }
                    return -1;
                }
                else
                {
                    rle = info.get(i);
                    streak = 1;
                }
            }
        }

        addRecord(new IPSRecord(offset, info));
        return -1;
    }

    /**
     * Returns a <code>String</code> of what the .ips file this represents
     * should contain.
     * 
     * @return A <code>String</code> of the .ips file this represents.
     */
    public String toString()
    {
        String out = "PATCH";
        for (int i = 0; i < getRecordCount(); i++)
        {
            out += getRecord(i).toString();
        }
        return out + "EOF";
    }

    /**
     * Applies this patch to a file stored in a <code>int[]</code>. NOTE:
     * This does not create a new <code>int[]</code>!
     * 
     * @param arg A file to apply the patch to
     * @return <code>arg</code> with this patch applied to it.
     */
    public int[] apply(int[] arg)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            arg = getRecord(i).patch(arg);
        }
        return arg;
    }

    /**
     * Applies this patch to a file stored in a <code>byte[]</code>. NOTE:
     * This does not create a new <code>byte[]</code>!
     * 
     * @param arg A file to apply the patch to
     * @return <code>arg</code> with this patch applied to it.
     */
    public byte[] apply(byte[] arg)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            arg = getRecord(i).patch(arg);
        }
        return arg;
    }

    /**
     * Unapplies this patch from a file stored in a <code>int[]</code>. NOTE:
     * This does not create a new <code>int[]</code>!
     * 
     * @param arg A file to unapply the patch from
     * @param org Orginal file this patch was based on
     * @return <code>arg</code> with this patch applied to it.
     */
    public int[] unapply(int[] arg, int[] org)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            arg = getRecord(i).unpatch(arg, org);
        }
        return arg;
    }

    /**
     * Unapplies this patch from a file stored in a <code>byte[]</code>.
     * NOTE: This does not create a new <code>byte[]</code>!
     * 
     * @param arg A file to unapply the patch from
     * @param org Orginal file this patch was based on
     * @return <code>arg</code> with this patch applied to it.
     */
    public byte[] unapply(byte[] arg, byte[] org)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            arg = getRecord(i).unpatch(arg, org);
        }
        return arg;
    }

    /**
     * Checks if this patch has been applied to a file stored in a
     * <code>int[]</code>. Returns true if the patch has been applied.
     * 
     * @param arg A file to check for the patch
     * @return True if applying this patch would have no effect
     */
    public boolean check(int[] arg)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            if (!getRecord(i).check(arg))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this patch has been applied to a file stored in a
     * <code>byte[]</code>. Returns true if the patch has been applied.
     * 
     * @param arg A file to check for the patch
     * @return True if applying this patch would have no effect
     */
    public boolean check(byte[] arg)
    {
        for (int i = 0; i < getRecordCount(); i++)
        {
            if (!getRecord(i).check(arg))
            {
                return false;
            }
        }
        return true;
    }

    /** Used internally to represent a single record in a .ips file. */
    protected static class IPSRecord
    {
        private int offset;
        private int size;
        private int rleSize;
        private byte rleInfo;
        //use non-null info
        private String info = null;
        private ByteBlock infoBB = null;

        public IPSRecord(int offset, String info) //create new uncompressed
        // IPSRecord
        {
            this.offset = offset;
            this.size = info.length();
            this.info = info;
        }

        public IPSRecord(int offset, ByteBlock info)
        {
            this.offset = offset;
            this.size = info.getLength();
            this.infoBB = info;
        }

        public IPSRecord(int offset, int rleSize, char info)
        {
            this.offset = offset;
            this.size = 0;
            this.rleSize = rleSize;
            this.rleInfo = (byte) info;
            //this.info = new String(new char[] { info });
        }

        public IPSRecord(int offset, int rleSize, byte info)
        {
            this.offset = offset;
            this.size = 0;
            this.rleSize = rleSize;
            this.rleInfo = info;
        }

        public IPSRecord(String in)
        {
            this.offset = (int) ((in.charAt(0) << 16)) & 0xFF0000;
            this.offset += (int) ((in.charAt(1) << 8)) & 0xFF00;
            this.offset += (int) (in.charAt(2)) & 0xFF;

            this.size = (int) ((in.charAt(3) << 8)) & 0xFF00;
            this.size += (int) (in.charAt(4)) & 0xFF;

            this.info = new String();

            if (this.size == 0)
            {
                this.rleSize = (int) ((in.charAt(5) << 8)) & 0xFF00;
                this.rleSize += (int) (in.charAt(6)) & 0xFF;

                this.rleInfo = (byte) in.charAt(7);
            }
            else
            {
                for (int i = 0; i < (size > 0 ? size : 1); i++)
                {
                    this.info += in.charAt(5 + i);
                }
            }
        }

        public IPSRecord(byte[] in)
        {
            this.offset = (int) ((in[0] << 16)) & 0xFF0000;
            this.offset += (int) ((in[1] << 8)) & 0xFF00;
            this.offset += (int) (in[2]) & 0xFF;

            this.size = (int) ((in[3] << 8)) & 0xFF00;
            this.size += (int) (in[4]) & 0xFF;

            if (this.size == 0)
            {
                this.rleSize = (int) ((in[5] << 8)) & 0xFF00;
                this.rleSize += (int) (in[6]) & 0xFF;

                this.rleInfo = (byte) in[7];
            }
            else
            {
                infoBB = ByteBlock.wrap(in, 5, size);
            }
        }

        public IPSRecord(ByteBlock in)
        {
            this(in.toByteArray());
            //above method seems faster

            //            this.offset = (int) ((in.get(0) << 16)) & 0xFF0000;
            //            this.offset += (int) ((in.get(1) << 8)) & 0xFF00;
            //            this.offset += (int) (in.get(2)) & 0xFF;
            //
            //            this.size = (int) ((in.get(3) << 8)) & 0xFF00;
            //            this.size += (int) (in.get(4)) & 0xFF;
            //
            //            if (this.size == 0)
            //            {
            //                this.rleSize = (int) ((in.get(5) << 8)) & 0xFF00;
            //                this.rleSize += (int) (in.get(6)) & 0xFF;
            //
            //                this.rleInfo = (byte) in.get(7);
            //                //this.info += in.charAt(7);
            //            }
            //            else
            //            {
            //                infoBB = in.subBlock(5, size);
            //            }
        }

        public String toString()
        {
            String out = new String();

            out += (char) ((this.offset >> 16) & 255); //write offset
            out += (char) ((this.offset >> 8) & 255);
            out += (char) (this.offset & 255);

            out += (char) ((this.size >> 8) & 255); //write size
            out += (char) (this.size & 255);

            if (size == 0)
            {
                out += (char) ((this.rleSize >> 8) & 255); //write RLE size
                out += (char) (this.rleSize & 255);

                out += (char) this.rleInfo;
            }
            else
            {
                out += (this.isInfoString() ? this.info : new String(
                    this.infoBB.toByteArray()));
            }

            return out;
        }

        public byte[] toByteArr()
        {
            byte[] out = new byte[(this.size == 0 ? 8 : 5 + size)];

            out[0] = (byte) ((this.offset >> 16) & 255);
            out[1] = (byte) ((this.offset >> 8) & 255);
            out[2] = (byte) (this.offset & 255);

            out[3] = (byte) ((this.size >> 8) & 255);
            out[4] = (byte) (this.size & 255);

            if (size == 0)
            {
                out[5] = (byte) ((this.rleSize >> 8) & 255); //write RLE size
                out[6] = (byte) (this.rleSize & 255);
                out[7] = this.rleInfo;
                /*
                 * (this.isInfoString() ? (byte) this.info.charAt(0) :
                 * this.infoBB.get(0));
                 */
            }
            else
            {
                if (this.isInfoString())
                {
                    byte[] b = this.info.getBytes();
                    System.arraycopy(b, 0, out, 5, size);
                    /*
                     * for (int i = 0; i < this.size; i++) { out[5 + i] = (byte)
                     * this.info .charAt(i); }
                     */
                }
                else
                {
                    this.infoBB.copyTo(out, 5);
                }

            }

            return out;
        }

        /**
         * If true, use info (String). If false, use infoBB (ByteBlock). If RLE
         * always use RLEInfo.
         * 
         * @see #getInfo()
         * @see #getInfoBB()
         */
        protected boolean isInfoString()
        {
            return info != null;
        }

        public int[] patch(int[] arg)
        {
            //do stuff first
            if (this.isInfoString())
            {
                if (size > 0)
                {
                    for (int i = 0; i < this.size; i++)
                    {
                        arg[offset + i] = (int) this.info.charAt(i);
                    }
                }
                else
                {
                    Arrays.fill(arg, offset, offset + rleSize, rleInfo);
                    /*
                     * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                     * (int) this.rleInfo; }
                     */
                }
            }
            else
            {
                if (size > 0)
                {
                    for (int i = 0; i < this.size; i++)
                    {
                        arg[offset + i] = (int) this.infoBB.get(i);
                    }
                }
                else
                {
                    Arrays.fill(arg, offset, offset + rleSize, rleInfo);
                    /*
                     * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                     * (int) this.rleInfo; }
                     */
                }
            }

            return arg;
        }

        public byte[] patch(byte[] arg)
        {
            //do stuff first
            if (this.isInfoString())
            {
                if (size > 0)
                {
                    for (int i = 0; i < this.size; i++)
                    {
                        arg[offset + i] = (byte) this.info.charAt(i);
                    }
                }
                else
                {
                    Arrays.fill(arg, offset, offset + rleSize, rleInfo);
                    /*
                     * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                     * (int) this.rleInfo; }
                     */
                }
            }
            else
            {
                if (size > 0)
                {
                    infoBB.copyTo(arg, offset);
                    /*
                     * for (int i = 0; i < this.size; i++) { arg[offset + i] =
                     * this.infoBB.get(i); }
                     */
                }
                else
                {
                    Arrays.fill(arg, offset, offset + rleSize, rleInfo);
                    /*
                     * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                     * (int) this.rleInfo; }
                     */
                }
            }

            return arg;
        }

        public int[] unpatch(int[] arg, int[] org)
        {
            if (size > 0)
            {
                System.arraycopy(org, offset, arg, offset, size);
                /*
                 * for (int i = 0; i < this.size; i++) { arg[offset + i] =
                 * org[offset + i]; }
                 */
            }
            else
            {
                Arrays.fill(arg, offset, offset + rleSize, org[offset]);
                /*
                 * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                 * org[offset + 1]; }
                 */
            }

            return arg;
        }

        public byte[] unpatch(byte[] arg, byte[] org)
        {
            if (size > 0)
            {
                System.arraycopy(org, offset, arg, offset, size);
                /*
                 * for (int i = 0; i < this.size; i++) { arg[offset + i] =
                 * org[offset + i]; }
                 */
            }
            else
            {
                Arrays.fill(arg, offset, offset + rleSize, org[offset]);
                /*
                 * for (int i = 0; i < this.rleSize; i++) { arg[offset + i] =
                 * org[offset + 1]; }
                 */
            }

            return arg;
        }

        public boolean check(int[] arg)
        {
            if (this.isInfoString())
            {
                if (size > 0)
                {
                    for (int i = 0; i < this.size; i++)
                    {
                        if (arg[offset + i] != (byte) this.info.charAt(i))
                            return false;
                    }
                }
                else
                {
                    for (int i = 0; i < this.rleSize; i++)
                    {
                        if (arg[offset + i] != this.rleInfo)
                            return false;
                    }
                }
            }
            else
            {
                if (size > 0)
                {
                    for (int i = 0; i < this.size; i++)
                    {
                        if (arg[offset + i] != this.infoBB.get(i))
                            return false;
                    }
                }
                else
                {
                    for (int i = 0; i < this.rleSize; i++)
                    {
                        if (arg[offset + i] != this.rleInfo)
                            return false;
                    }
                }
            }
            return true;
        }

        public boolean check(byte[] arg)
        {
            try
            {
                if (this.isInfoString())
                {
                    if (size > 0)
                    {
                        for (int i = 0; i < this.size; i++)
                        {
                            if (arg[offset + i] != (byte) this.info.charAt(i))
                            {
                                return false;
                            }
                        }
                    }
                    else
                    {
                        for (int i = 0; i < this.rleSize; i++)
                        {
                            if (arg[offset + i] != this.rleInfo)
                            {
                                return false;
                            }
                        }
                    }
                }
                else
                {
                    if (size > 0)
                    {
                        for (int i = 0; i < this.size; i++)
                        {
                            if (arg[offset + i] != this.infoBB.get(i))
                            {
                                return false;
                            }
                        }
                    }
                    else
                    {
                        for (int i = 0; i < this.rleSize; i++)
                        {
                            if (arg[offset + i] != this.rleInfo)
                            {
                                return false;
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

        public String getInfo()
        {
            return info;
        }

        public ByteBlock getInfoBB()
        {
            return infoBB;
        }

        public int getOffset()
        {
            return offset;
        }

        public int getRleSize()
        {
            return rleSize;
        }

        public int getSize()
        {
            return size;
        }

        public byte getRleInfo()
        {
            return rleInfo;
        }

    }

    public IPSRecord getRecord(int record)
    {
        return (IPSRecord) records.get(record);
    }

    public int getRecordCount()
    {
        return records.size();
    }
}