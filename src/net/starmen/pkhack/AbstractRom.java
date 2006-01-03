package net.starmen.pkhack;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * Wrapper class for a ROM. Loads the ROM into memory and provides
 * <code>read()</code> and <code>write()</code> methods.
 * 
 * @author AnyoneEB
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public abstract class AbstractRom
{
    /** Size in bytes of a regular Earthbound ROM. */
    public final static long EB_ROM_SIZE_REGULAR = 3146240;

    /** Size in bytes of an expanded Earthbound ROM. */
    public final static long EB_ROM_SIZE_EXPANDED = 4194816;

    /** Path to the ROM. */
    protected File path; //path to the rom

    /**
     * Returns the default directory for saving and loading.
     * 
     * @return The default directory for saving and loading.
     */
    public static String getDefaultDir()
    {
        return JHack.main.getPrefs().getValue("defaultDir");
    }

    /**
     * Sets the default directory for saving and loading.
     * 
     * @param dir The default directory for saving and loading.
     */
    public static void setDefaultDir(String dir)
    {
        JHack.main.getPrefs().setValue("defaultDir", dir);
    }

    //    /**
    //     * True if ROM is expanded.
    //     */
    //    public boolean isExpanded;
    /**
     * True if ROM is a valid size. (Either exactly 3 Megabytes, or exactly 4
     * Megabytes)
     */
    public boolean isValid = true;

    /**
     * True if a ROM is loaded. Changing this is a very bad idea.
     */
    public boolean isLoaded = false;

    /**
     * Current "place" in ROM.
     * 
     * @see #seek(int)
     */
    private int seekOffset;

    /**
     * Stores the type of ROM.
     */
    protected String romType;

    /**
     * Creates an AbstractRom object and loads the <code>File rompath</code>.
     */
    public AbstractRom(File rompath)
    {
        this.loadRom(rompath);
    }

    /**
     * Creates an AbstractRom object. There will be no ROM loaded so calls to
     * <code>read()</code> or <code>write()</code> will give exceptions.
     */
    public AbstractRom()
    {}

    /**
     * Returns the ROM type. Type being which game this ROM is. Should be one of
     * the ROM type constants.
     * 
     * @return what game loaded ROM is
     */
    public String getRomType()
    {
        return romType;
    }

    /**
     * Sets the ROM type. Type being which game this ROM is. Should be one of
     * the ROM type constants.
     * 
     * @param romType what game loaded ROM is
     */
    public void setRomType(String romType)
    {
        this.romType = (romType.length() == 0 ? "Unknown" : romType);
        saveRomType();
    }

    /**
     * Loads the ROM from the location specificied by the <code>File</code>.
     * 
     * @param rompath Where the ROM to load is.
     * @return True if the ROM was successfully loaded.
     * @see #loadRom()
     */
    public boolean loadRom(File rompath)
    {
        setDefaultDir(rompath.getParent());

        try
        {
            readFromRom(rompath);
        }
        catch (FileNotFoundException e)
        {
            JOptionPane.showMessageDialog(null, "File not found:\n"
                + rompath.getAbsolutePath(), "Error Loading ROM",
                JOptionPane.ERROR_MESSAGE);
            System.out.println("Error: File not loaded: File not found.");
            //            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(null,
                "Error: File not loaded: Could read file:\n" + e.getClass()
                    + ": " + e.getMessage(), "Error Loading ROM",
                JOptionPane.ERROR_MESSAGE);
            System.out.println("Error: File not loaded: Could read file.");
            e.printStackTrace(System.out);
            return false;
        }

        this.path = rompath;

        System.out.println("Opened ROM: " + rompath.toString());
        System.out.println("AbstractRom size is: " + rompath.length());

        this.isLoaded = true;

        //set rom type
        //first look for .romtype file
        if (!loadRomType())
        {
            setRomType(RomTypeFinder.getRomType(this));
        }

        return true;
    }

    /**
     * Attemps to load ROM type from .romtype file. Returns false if unable to
     * read that file. If this returns false, other methods must be used to
     * discover the ROM type.
     * 
     * @return false if .romtype meta data file not found.
     */
    protected boolean loadRomType()
    {
        try
        {
            FileReader in = new FileReader(getPath() + ".romtype");
            char[] c = new char[(int) new File(getPath() + ".romtype").length()];
            in.read(c);
            in.close();
            String type = new String(c);
            //            for (int i = 0; i < TYPES.length; i++)
            //            {
            //                if (type.equals(TYPES[i])) setRomType(TYPES[i]);
            //            }
            setRomType(type);
            return true;
        }
        catch (FileNotFoundException e)
        {}
        catch (IOException e)
        {}
        return false;
    }

    protected void saveRomType()
    {
        try
        {
            FileWriter out = new FileWriter(getPath() + ".romtype");
            out.write(getRomType());
            out.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Reads the ROM into memory, DO NOT CALL. This method is here to be
     * overridden by classes extending AbstractRom.
     * 
     * @param rompath Path to load from.
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected abstract void readFromRom(File rompath)
        throws FileNotFoundException, IOException;

    /**
     * Loads the ROM from a location selected by the user.
     * 
     * @return True if the ROM was successfully loaded.
     * @see #loadRom(File)
     */
    public boolean loadRom()
    {
        try
        {
            JFileChooser jfc = new JFileChooser(AbstractRom.getDefaultDir());
            jfc.setFileFilter(new FileFilter()
            {
                public boolean accept(File f)
                {
                    if ((f.getAbsolutePath().toLowerCase().endsWith(".smc")
                        || f.getAbsolutePath().toLowerCase().endsWith(".sfc")
                        || f.getAbsolutePath().toLowerCase().endsWith(".fig") || f
                        .isDirectory())
                        && f.exists())
                    {
                        return true;
                    }
                    return false;
                }

                public String getDescription()
                {
                    return "SNES ROMs (*.smc, *.sfc, *.fig)";
                }
            });

            if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                return loadRom(jfc.getSelectedFile());
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            JOptionPane.showMessageDialog(null,
                "An error occured in the load ROM dialog.\n"
                    + "Please try again. Full details on the\n"
                    + "error are on the console. The error was:\n"
                    + e.getClass() + ": " + e.getMessage(),
                "Error in Loading Dialog", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Saves the ROM to the location specificied by the <code>File</code>.
     * 
     * @param rompath Where to save the ROM to.
     * @return True if the ROM was successfully saved.
     * @see #saveRom()
     * @see #saveRomAs()
     */
    public abstract boolean saveRom(File rompath);

    /**
     * Saves the ROM to the location {@link #loadRom(File)}was last called for.
     * Note that {@link #loadRom()}calls <code>loadRom(File)</code> with the
     * selected <code>File</code>.
     * 
     * @return True if the ROM was successfully saved.
     * @see #saveRomAs()
     * @see #saveRom(File)
     */
    public boolean saveRom()
    {
        return saveRom(this.path);
    }

    /**
     * Saves the ROM to a location selected by the user.
     * 
     * @return True if the ROM was successfully saved.
     * @see #saveRom(File)
     */
    public boolean saveRomAs()
    {
        if (!this.isLoaded) //don't try to save if nothing is loaded
        {
            return false;
        }
        JFileChooser jfc = new JFileChooser(AbstractRom.getDefaultDir());
        jfc.setFileFilter(new FileFilter()
        {

            public boolean accept(File f)
            {
                if (f.getAbsolutePath().toLowerCase().endsWith(".smc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".sfc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".fig")
                    || f.isDirectory())
                {
                    return true;
                }
                return false;
            }

            public String getDescription()
            {
                return "SNES ROMs (*.smc, *.sfc, *.fig)";
            }
        });
        if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            return saveRom(jfc.getSelectedFile());
        }
        else
        {
            return false;
        }
    }

    /**
     * Truncates to a ROM to a new length.
     * 
     * @param newLen target length for the ROM; must be less than or equal to
     *            the current length
     * @return true on success, false on failure
     */
    public boolean truncate(int newLen)
    {
        if (newLen < length())
        {
            return _truncate(newLen);
        }
        else
        {
            return true;
        }
    }

    /**
     * Truncates to a ROM to a new length. Override this method, not
     * {@see #truncate(int)}.
     * 
     * @param newLen target length for the ROM; will be less than the current
     *            length
     * @return true on success, false on failure
     */
    public abstract boolean _truncate(int newLen);

    /**
     * Writes <code>arg</code> at <code>offset</code> in the rom. This does
     * not actually write to the filesystem. {@link #saveRom(File)}writes to
     * the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @see #write(int, int, int)
     * @see #write(int, int[])
     * @see #write(int, char)
     * @see #write(int, char[])
     */
    public abstract void write(int offset, int arg); //main write method

    /**
     * Writes the specified length multibyte value <code>arg</code> at
     * <code>offset</code> in the rom. This does not actually write to the
     * filesystem. {@link #saveRom(File)}writes to the filesystem. This writes
     * a multibyte value in the standard reverse bytes format.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @param bytes How many bytes long this is.
     * @see #write(int, int[])
     * @see #write(int, char)
     * @see #write(int, char[])
     */
    public void write(int offset, int arg, int bytes)
    {
        for (int i = 0; i < bytes; i++)
        {
            this.write(offset + i, arg >> (i * 8));
        }
    }

    /**
     * Writes <code>arg</code> at <code>offset</code> in the rom. This does
     * not actually write to the filesystem. {@link #saveRom(File)}writes to
     * the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @see #write(int, int)
     * @see #write(int, int[])
     * @see #write(int, char[])
     */
    public void write(int offset, char arg)
    {
        write(offset, (int) arg);
    }

    /**
     * Writes <code>arg</code> at <code>offset</code> in the rom. This
     * writes more than one byte to <code>offset</code>. The first byte is
     * written to <code>offset</code>, next to <code>offset</code>+ 1,
     * etc. This does not actually write to the filesystem.
     * {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @see #write(int, int)
     * @see #write(int, int[], int)
     * @see #write(int, char)
     * @see #write(int, char[])
     */
    public void write(int offset, int[] arg) //write a [multibyte] string to a
    // place
    {
        write(offset, arg, arg.length);
    }

    /**
     * Writes <code>arg</code> at <code>offset</code> in the rom. This
     * writes more than one byte to <code>offset</code>. The first byte is
     * written to <code>offset</code>, next to <code>offset</code>+ 1,
     * etc. This does not actually write to the filesystem.
     * {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @see #write(int, int)
     * @see #write(int, int[], int)
     * @see #write(int, char)
     * @see #write(int, char[])
     */
    public void write(int offset, byte[] arg) //write a [multibyte] string to
    // a place
    {
        write(offset, arg, arg.length);
    }

    /**
     * Writes <code>len</code> bytes of <code>arg</code> at
     * <code>offset</code> in the rom. This writes more than one byte to
     * <code>offset</code>. The first byte is written to <code>offset</code>,
     * next to <code>offset</code>+ 1, etc. It is suggested that this is
     * overrided in order to provide a faster way to do this. The default
     * implementation uses a for loop, which is universal, but slow. This does
     * not actually write to the filesystem. {@link #saveRom(File)}writes to
     * the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @param len Number of bytes to write
     * @see #write(int, int)
     * @see #write(int, int[])
     */
    public void write(int offset, byte[] arg, int len) //write a [multibyte]
    // string to a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(offset + i > this.length()))
            //don't write past the end of the ROM
            {
                this.write(offset + i, arg[i]);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes arg at offset 0.
     * 
     * @param arg
     */
    public void writeFullRom(int[] arg)
    {
        write(0, arg);
    }

    /**
     * Writes arg at offset 0.
     * 
     * @param arg
     */
    public void writeFullRom(byte[] arg)
    {
        write(0, arg);
    }

    /**
     * Writes <code>len</code> bytes of <code>arg</code> at
     * <code>offset</code> in the rom. This writes more than one byte to
     * <code>offset</code>. The first byte is written to <code>offset</code>,
     * next to <code>offset</code>+ 1, etc. This does not actually write to
     * the filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @param len Number of bytes to write
     * @see #write(int, int)
     * @see #write(int, char)
     * @see #write(int, char[])
     */
    public void write(int offset, int[] arg, int len) //write a [multibyte]
    // string to a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(offset + i > this.length()))
            //don't write past the end of the ROM
            {
                this.write(offset + i, arg[i]);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes <code>len</code> multibyte indexes of <code>arg</code> at
     * <code>offset</code> in the rom. This writes
     * <code>len * bytes<code> bytes.
     * This does not actually write to the filesystem. {@link #saveRom(File)}
     * writes to the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @param len Number of array indexes to go through
     * @param bytes How many bytes to write for each index
     * @see #write(int, int)
     * @see #write(int, int, int)
     * @see #write(int, int[], int)
     */
    public void write(int offset, int[] arg, int len, int bytes) //write a
    // [multibyte]
    // string to a
    // place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(offset + (i * bytes) > this.length()))
            //don't write past the end of the ROM
            {
                this.write(offset + (i * bytes), arg[i], bytes);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes <code>arg</code> at <code>offset</code> in the rom. This
     * writes more than one byte to <code>offset</code>. The first byte is
     * written to <code>offset</code>, next to <code>offset</code>+ 1,
     * etc. This does not actually write to the filesystem.
     * {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param offset Where in the ROM to write the value. Counting starts at
     *            zero, as normal.
     * @param arg What to write at <code>offset</code>.
     * @see #write(int, int)
     * @see #write(int, int[])
     * @see #write(int, char)
     */
    public void write(int offset, char[] arg)
    {
        int[] newArg = new int[arg.length];
        for (int i = 0; i < arg.length; i++)
        {
            newArg[i] = (int) arg[i];
        }
        write(offset, newArg);
    }

    /**
     * Reads an <code>int</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @return <code>int</code> at <code>offset</code>. If
     *         <code>offset &gt; the rom.length</code> then it is -1.
     */
    public abstract int read(int offset);

    /**
     * Reads a <code>byte</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @return <code>byte</code> at <code>offset</code>. If
     *         <code>offset &gt; the rom.length</code> then it is -1.
     */
    public byte readByte(int offset)
    {
        return (byte) this.read(offset);
    }

    /**
     * Reads an <code>int[]</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @param length Number of bytes to read.
     * @return <code>int[]</code> at <code>offset</code> with a length of
     *         <code>length</code>. If
     *         <code>offset &gt; the rom.length</code> then it is -1.
     */
    public int[] read(int offset, int length)
    {
        int[] returnValue = new int[length];
        for (int i = 0; i < length; i++)
        {
            returnValue[i] = this.read(offset + i);
        }
        return returnValue;
    }

    /**
     * Reads a <code>List</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @param length Number of bytes to read.
     * @return <code>ArrayList</code> at <code>offset</code> with a length
     *         of <code>length</code>. If
     *         <code>offset &gt; the rom.length</code> then it is -1.
     */
    public ArrayList readList(int offset, int length)
    {
        ArrayList returnValue = new ArrayList();
        for (int i = 0; i < length; i++)
        {
            returnValue.add(new Integer(this.read(offset + i)));
        }
        return returnValue;
    }

    /**
     * Reads a <code>byte[]</code> from <code>offset</code> in the rom. It
     * is highly recommended that this method is overriden with a faster
     * version. The default implementation uses a for loop, which always works,
     * but it very slow.
     * 
     * @param offset Where to read from.
     * @param length Number of bytes to read.
     * @return <code>byte[]</code> at <code>offset</code> with a length of
     *         <code>length</code>. If
     *         <code>offset &gt; the rom.length</code> then it is -1 or null
     *         may be returned.
     */
    public byte[] readByte(int offset, int length)
    {
        byte[] returnValue = new byte[length];
        for (int i = 0; i < length; i++)
        {
            returnValue[i] = this.readByte(offset + i);
        }
        return returnValue;
    }

    /**
     * Reads an <code>int[]</code> of the entire ROM.
     * 
     * @return The entire ROM as a <code>int[]</code>
     */
    public int[] readFullRom()
    {
        return read(0, this.length());
    }

    /**
     * Reads a <code>byte[]</code> of the entire ROM.
     * 
     * @return The entire ROM as a <code>byte[]</code>
     */
    public byte[] readFullRomByteArr()
    {
        return readByte(0, this.length());
    }

    /**
     * Reads an <code>char</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @return <code>char</code> at <code>offset</code>. If
     *         <code>offset<code> is past the end of the rom then it is -1.
     */
    public char readChar(int offset)
    {
        return (char) read(offset);
    }

    /**
     * Reads an <code>char[]</code> from <code>offset</code> in the rom.
     * 
     * @param offset Where to read from.
     * @param length Number of bytes to read.
     * @return <code>char[]</code> at <code>offset</code> with a length of
     *         <code>length</code>. If
     *         <code>offset<code> is past the end of the rom then it is -1.
     */
    public char[] readChar(int offset, int length) //read as a char[] instead
    // of int[]
    {
        char[] returnValue = new char[length];
        int[] in = read(offset, length);

        for (int i = 0; i < length; i++)
        {
            returnValue[i] = (char) in[i];
        }
        return returnValue;
    }

    /**
     * Reads a mulibyte number with the specified offset and length. Reverses
     * the byte order to get the correct value.
     * 
     * @param offset Where the number is.
     * @param len How many bytes long the number is.
     * @return Multibyte value as an int.
     */
    public int readMulti(int offset, int len)
    {
        int out = 0;
        for (int i = 0; i < len; i++)
        {
            out += this.read(offset + i) << (i * 8);
        }
        return out;
    }

    /**
     * Reads a SNES pointer from an ASM link. ASM links look like [A9 WW XX 85
     * 0E A9 YY ZZ] for a pointer to $ZZYYXXWW.
     * 
     * @param offset offset of ASM link to read
     * @return an SNES pointer
     */
    public int readAsmPointer(int offset)
    {
        int out = 0;
        if (read(offset++) != 0xA9)
            return -1;
        out |= read(offset++);
        out |= read(offset++) << 8;
        if (read(offset++) != 0x85)
            return -1;
        if (read(offset++) != 0x0E)
            return -1;
        if (read(offset++) != 0xA9)
            return -1;
        out |= read(offset++) << 16;
        out |= read(offset++) << 24;

        return out;
    }

    /**
     * Reads a regular pointer from an ASM link. ASM links look like [A9 WW XX
     * 85 0E A9 YY ZZ] for a pointer to $ZZYYXXWW.
     * 
     * @param offset offset of ASM link to read
     * @return an regular pointer
     */
    public int readRegAsmPointer(int offset)
    {
        return HackModule.toRegPointer(readAsmPointer(offset));
    }

    /**
     * Writes a SNES pointer to an ASM link. ASM links look like [A9 WW XX 85 0E
     * A9 YY ZZ] for a pointer to $ZZYYXXWW.
     * 
     * @param offset offset of ASM link to write
     * @param snesPointer an SNES pointer
     */
    public void writeAsmPointer(int offset, int snesPointer)
    {
        write(offset++, 0xA9);
        write(offset++, snesPointer & 0xFF);
        write(offset++, (snesPointer >> 8) & 0xFF);
        write(offset++, 0x85);
        write(offset++, 0x0E);
        write(offset++, 0xA9);
        write(offset++, (snesPointer >> 16) & 0xFF);
        write(offset++, (snesPointer >> 24) & 0xFF);
    }

    /**
     * Writes a regular pointer to an ASM link. ASM links look like [A9 WW XX 85
     * 0E A9 YY ZZ] for a pointer to $ZZYYXXWW.
     * 
     * @param offset offset of ASM link to write
     * @param regPointer an regular pointer
     */
    public void writeRegAsmPointer(int offset, int regPointer)
    {
        writeAsmPointer(offset, HackModule.toSnesPointer(regPointer));
    }

    //pallette
    /**
     * Reads an SNES format palette color from the specificed place in the rom.
     * This reads one color of a palette, which is two bytes long. SNES palettes
     * are made up of 16-bit little endian color entries. 5 bits each are used
     * for (from lowest order to highest order bits) red, green, and blue, and
     * one bit is left unused.
     * 
     * @param offset offset in the rom palette color is at; note that the byte
     *            at <code>offset</code> and the byte after will be read
     * @return a {@link Color}that is equivalent to the specified SNES color
     * @see #read(int)
     * @see #readByte(int, int)
     * @see #readPalette(int, Color[])
     * @see #readPalette(int, int)
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(Color[])
     * @see #readPaletteSeek(int)
     * @see HackModule#readPalette(byte[], int)
     */
    public Color readPalette(int offset)
    {
        return HackModule.readPalette(readByte(offset, 2));
    }

    /**
     * Reads an SNES format palette from the specificed place in the rom. This
     * reads as many colors of the palette as the
     * <code>Color[]<code> array is long;
     * each color is two bytes long. SNES palettes
     * are made up of 16-bit little endian color entries. 5 bits each are used
     * for (from lowest order to highest order bits) red, green, and blue, and
     * one bit is left unused.
     * 
     * @param offset offset in the rom palette color is at; note
     *            that <code>c.length() * 2</code> bytes
     *            will be read
     * @param c <code>Color[]</code> to read {@link Color}'s into, which are equivalent to the SNES colors 
     * @see #read(int)
     * @see #readByte(int, int)
     * @see #readPalette(int)
     * @see #readPalette(int, int)
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(Color[])
     * @see #readPaletteSeek(int)
     * @see HackModule#readPalette(byte[], int)
     * @see HackModule#readPalette(byte[], int, Color[])
     */
    public void readPalette(int offset, Color[] c)
    {
        HackModule.readPalette(readByte(offset, 2 * c.length), c);
    }

    /**
     * Reads an SNES format palette from the specificed place in the rom. This
     * reads <code>size</code> colors of the palette; each color is two bytes
     * long. SNES palettes are made up of 16-bit little endian color entries. 5
     * bits each are used for (from lowest order to highest order bits) red,
     * green, and blue, and one bit is left unused.
     * 
     * @param offset offset in the rom palette color is at; note that
     *            <code>size * 2</code> bytes will be read
     * @param size number of colors to read, <code>size * 2</code> equals the
     *            number of bytes to read
     * @return a <code>Color[]</code> of {@link Color}'s that are equivalent
     *         to the SNES colors
     * @see #read(int)
     * @see #readByte(int, int)
     * @see #readPalette(int)
     * @see #readPalette(int, Color[])
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(Color[])
     * @see #readPaletteSeek(int)
     * @see HackModule#readPalette(byte[], int)
     * @see HackModule#readPalette(byte[], int, int)
     */
    public Color[] readPalette(int offset, int size)
    {
        Color[] c = new Color[size];
        readPalette(offset, c);
        return c;
    }

    /**
     * Reads an SNES format palette color from the seek offset in the rom. This
     * reads one color of a palette, which is two bytes long. SNES palettes are
     * made up of 16-bit little endian color entries. 5 bits each are used for
     * (from lowest order to highest order bits) red, green, and blue, and one
     * bit is left unused.
     * 
     * @return a {@link Color}that is equivalent to the specified SNES color
     * @see #seek(int)
     * @see #readSeek()
     * @see #readByteSeek(int)
     * @see #readPalette(int, Color[])
     * @see #readPalette(int, int)
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(Color[])
     * @see #readPaletteSeek(int)
     * @see HackModule#readPalette(byte[], int)
     */
    public Color readPaletteSeek()
    {
        return HackModule.readPalette(readByteSeek(2));
    }

    /**
     * Reads an SNES format palette from the seek offset in the rom. This reads
     * as many colors of the palette as the <code>Color[]<code> array is long;
     * each color is two bytes long. SNES palettes
     * are made up of 16-bit little endian color entries. 5 bits each are used
     * for (from lowest order to highest order bits) red, green, and blue, and
     * one bit is left unused.
     * 
     * @param c <code>Color[]</code> to read {@link Color}'s into, which are equivalent to the SNES colors 
     * @see #seek(int)
     * @see #readSeek()
     * @see #readByteSeek(int)
     * @see #readPalette(int)
     * @see #readPalette(int, int)
     * @see #readPalette(int, Color[])
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(int)
     * @see HackModule#readPalette(byte[], int)
     * @see HackModule#readPalette(byte[], int, Color[])
     */
    public void readPaletteSeek(Color[] c)
    {
        HackModule.readPalette(readByteSeek(2 * c.length), c);
    }

    /**
     * Reads an SNES format palette from the seek offset in the rom. This reads
     * <code>size</code> colors of the palette; each color is two bytes long.
     * SNES palettes are made up of 16-bit little endian color entries. 5 bits
     * each are used for (from lowest order to highest order bits) red, green,
     * and blue, and one bit is left unused.
     * 
     * @param size number of colors to read, <code>size * 2</code> equals the
     *            number of bytes to read
     * @return a <code>Color[]</code> of {@link Color}'s that are equivalent
     *         to the SNES colors
     * @see #seek(int)
     * @see #readSeek()
     * @see #readByteSeek(int)
     * @see #readPalette(int)
     * @see #readPalette(int, int)
     * @see #readPalette(int, Color[])
     * @see #readPaletteSeek()
     * @see #readPaletteSeek(Color[])
     * @see HackModule#readPalette(byte[], int)
     * @see HackModule#readPalette(byte[], int, int)
     */
    public Color[] readPaletteSeek(int size)
    {
        Color[] c = new Color[size];
        readPaletteSeek(c);
        return c;
    }

    /**
     * Writes an SNES format palette color to the specificed place in the rom.
     * This writes one color of a palette, which is two bytes long. SNES
     * palettes are made up of 16-bit little endian color entries. 5 bits each
     * are used for (from lowest order to highest order bits) red, green, and
     * blue, and one bit is left unused.
     * 
     * @param offset offset in rom palette color will be written at at; note
     *            that the byte at <code>offset</code> and the byte after will
     *            be written to
     * @param c {@link Color}to write as an SNES color; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #write(int)
     * @see #write(int, int)
     * @see #readPalette(int)
     * @see #writePalette(int, Color[])
     * @see #writePaletteSeek(Color)
     * @see #writePaletteSeek(Color[])
     * @see HackModule#writePalette(byte[], int, Color)
     */
    public void writePalette(int offset, Color c)
    {
        write(offset, HackModule.writePalette(c));
    }

    /**
     * Writes an SNES format palette to the specificed place in the rom. This
     * writes all <code>Color</code>'s in <code>c</code> to a palette; each
     * color is two bytes long. SNES palettes are made up of 16-bit little
     * endian color entries. 5 bits each are used for (from lowest order to
     * highest order bits) red, green, and blue, and one bit is left unused.
     * 
     * @param offset offset in rom palette color will be written at at; note
     *            that <code>c.length() * 2</code> bytes will be written to
     * @param c {@link Color}[] to write as an SNES palette; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #write(int)
     * @see #write(int, int)
     * @see #readPalette(int)
     * @see #writePalette(int, Color[])
     * @see #writePaletteSeek(Color)
     * @see #writePaletteSeek(Color[])
     * @see HackModule#writePalette(byte[], int, Color[])
     */
    public void writePalette(int offset, Color[] c)
    {
        write(offset, HackModule.writePalette(c));
    }

    /**
     * Writes an SNES format palette color to the seek offset in the rom. This
     * writes one color of a palette, which is two bytes long. SNES palettes are
     * made up of 16-bit little endian color entries. 5 bits each are used for
     * (from lowest order to highest order bits) red, green, and blue, and one
     * bit is left unused.
     * 
     * @param c {@link Color}to write as an SNES color; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #seek(int)
     * @see #writeSeek()
     * @see #writeSeek(int)
     * @see #readPalette(int)
     * @see #writePalette(int, Color)
     * @see #writePalette(int, Color[])
     * @see #writePaletteSeek(Color[])
     * @see HackModule#writePalette(byte[], int, Color)
     */
    public void writePaletteSeek(Color c)
    {
        writeSeek(HackModule.writePalette(c));
    }

    /**
     * Writes an SNES format palette to the seek offset in the rom. This writes
     * all <code>Color</code>'s in <code>c</code> to a palette; each color
     * is two bytes long. SNES palettes are made up of 16-bit little endian
     * color entries. 5 bits each are used for (from lowest order to highest
     * order bits) red, green, and blue, and one bit is left unused.
     * 
     * @param c {@link Color}[] to write as an SNES palette; note that
     *            <code>Color</code> supports up to 8 bits per color component
     *            (red, green, blue) as well as an alpha channel and SNES colors
     *            have only 5 bits per color component and no alpha support
     * @see #seek(int)
     * @see #writeSeek()
     * @see #writeSeek(int)
     * @see #readPalette(int)
     * @see #writePalette(int, Color)
     * @see #writePalette(int, Color[])
     * @see #writePaletteSeek(Color)
     * @see HackModule#writePalette(byte[], int, Color[])
     */
    public void writePaletteSeek(Color[] c)
    {
        writeSeek(HackModule.writePalette(c));
    }

    //seeking read/write
    /**
     * Places marks current place in ROM as <code>offset</code>.
     * 
     * @param offset offset in ROM to seek to
     * @see #writeSeek(int)
     * @see #readSeek()
     */
    public void seek(int offset)
    {
        this.seekOffset = offset;
    }

    /**
     * Reads the <code>int</code> at
     * <code>seekOffset<code> and increments <code>seekOffset</code>.
     * 
     * @return <code>int</code> at <code>seekOffset</code>
     * @see #seek(int)
     * @see #readByteSeek()
     * @see #writeSeek(int)
     */
    public int readSeek()
    {
        return read(seekOffset++);
    }

    /**
     * Reads the <code>byte</code> at
     * <code>seekOffset<code> and increments <code>seekOffset</code>.
     * 
     * @return <code>byte</code> at <code>seekOffset</code>
     * @see #seek(int)
     * @see #readSeek()
     * @see #writeSeek(int)
     */
    public byte readByteSeek()
    {
        return readByte(seekOffset++);
    }

    /**
     * Writes <code>arg</code> at <code>seekOffset</code> in the rom and
     * increments <code>seekOffset</code>. This does not actually write to
     * the filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @see #seek(int)
     * @see #write(int, int)
     */
    public void writeSeek(int arg) //main write method
    {
        write(seekOffset++, arg);
    }

    /**
     * Writes the specified length multibyte value <code>arg</code> at
     * <code>seekOffset</code> in the rom and increments
     * <code>seekOffset</code>. This does not actually write to the
     * filesystem. {@link #saveRom(File)}writes to the filesystem. This writes
     * a multibyte value in the standard reverse bytes format.
     * 
     * @param arg What to write at <code>offset</code>.
     * @param bytes How many bytes long this is.
     * @see #seek(int)
     * @see #writeSeek(int)
     */
    public void writeSeek(int arg, int bytes)
    {
        for (int i = 0; i < bytes; i++)
        {
            this.writeSeek(arg >> (i * 8));
        }
    }

    /**
     * Writes <code>arg</code> at <code>seekOffset</code> in the rom and
     * increments <code>seekOffset</code>. This does not actually write to
     * the filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @see #seek(int)
     * @see #writeSeek(int)
     */
    public void writeSeek(char arg)
    {
        writeSeek((int) arg);
    }

    /**
     * Writes <code>arg</code> at <code>seekOffset</code> in the rom and
     * moves <code>seekOffset</code>.<code>seekOffset</code> will point to
     * the byte after the last byte written or the byte after the end of the
     * ROM. This writes more than one byte to <code>seekOffset</code>. The
     * first byte is written to <code>seekOffset</code>, next to
     * <code>seekOffset</code>+ 1, etc. This does not actually write to the
     * filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @see #seek(int)
     * @see #writeSeek(int)
     */
    public void writeSeek(int[] arg)
    {
        writeSeek(arg, arg.length);
    }

    /**
     * Writes <code>arg</code> at <code>seekOffset</code> in the rom and
     * moves <code>seekOffset</code>.<code>seekOffset</code> will point to
     * the byte after the last byte written or the byte after the end of the
     * ROM. This writes more than one byte to <code>seekOffset</code>. The
     * first byte is written to <code>seekOffset</code>, next to
     * <code>seekOffset</code>+ 1, etc. This does not actually write to the
     * filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @see #seek(int)
     * @see #writeSeek(int)
     */
    public void writeSeek(byte[] arg) //write a [multibyte] string to a place
    {
        writeSeek(arg, arg.length);
    }

    /**
     * Writes <code>len</code> bytes of <code>arg</code> at
     * <code>seekOffset</code> in the rom and moves <code>seekOffset</code>.
     * <code>seekOffset</code> will point to the byte after the last byte
     * written or the byte after the end of the ROM. This writes more than one
     * byte to <code>seekOffset</code>. The first byte is written to
     * <code>seekOffset</code>, next to <code>seekOffset</code>+ 1, etc.
     * This does not actually write to the filesystem. {@link #saveRom(File)}
     * writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @param len Number of bytes to write
     * @see #seek(int)
     * @see #writeSeek(int)
     * @see #writeSeek(byte[])
     */
    public void writeSeek(byte[] arg, int len) //write a [multibyte] string to
    // a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(seekOffset > this.length()))
            //don't write past the end of the ROM
            {
                writeSeek(arg[i]);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes <code>len</code> bytes of <code>arg</code> at
     * <code>seekOffset</code> in the rom and moves <code>seekOffset</code>.
     * <code>seekOffset</code> will point to the byte after the last byte
     * written or the byte after the end of the ROM. This writes more than one
     * byte to <code>seekOffset</code>. The first byte is written to
     * <code>seekOffset</code>, next to <code>seekOffset</code>+ 1, etc.
     * This does not actually write to the filesystem. {@link #saveRom(File)}
     * writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @param len Number of bytes to write
     * @see #seek(int)
     * @see #writeSeek(int)
     * @see #writeSeek(byte[])
     */
    public void writeSeek(int[] arg, int len) //write a [multibyte] string to
    // a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(seekOffset > this.length()))
            //don't write past the end of the ROM
            {
                writeSeek(arg[i]);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes <code>len</code> multibyte indexes of <code>arg</code> at
     * <code>seekOffset</code> in the rom and moves <code>seekOffset</code>.
     * <code>seekOffset</code> will point to the byte after the last byte
     * written or the byte after the end of the ROM. This writes
     * <code>len * bytes<code> bytes.
     * This does not actually write to the filesystem. {@link #saveRom(File)}
     * writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @param len Number of array indexes to go through
     * @param bytes How many bytes to write for each index
     * @see #seek(int)
     * @see #writeSeek(int)
     * @see #writeSeek(int, int)
     */
    public void writeSeek(int[] arg, int len, int bytes) //write a [multibyte]
    // string to a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(seekOffset + (i * bytes) > this.length()))
            //don't write past the end of the ROM
            {
                this.writeSeek(arg[i], bytes);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Writes <code>arg</code> at <code>seekOffset</code> in the rom and
     * moves <code>seekOffset</code>.<code>seekOffset</code> will point to
     * the byte after the last byte written or the byte after the end of the
     * ROM. This writes more than one byte to <code>seekOffset</code>. The
     * first byte is written to <code>seekOffset</code>, next to
     * <code>seekOffset</code>+ 1, etc. This does not actually write to the
     * filesystem. {@link #saveRom(File)}writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @see #seek(int)
     * @see #writeSeek(int)
     */
    public void writeSeek(char[] arg)
    {
        writeSeek(arg, arg.length);
    }

    /**
     * Writes <code>len</code> bytes of <code>arg</code> at
     * <code>seekOffset</code> in the rom and moves <code>seekOffset</code>.
     * <code>seekOffset</code> will point to the byte after the last byte
     * written or the byte after the end of the ROM. This writes more than one
     * byte to <code>seekOffset</code>. The first byte is written to
     * <code>seekOffset</code>, next to <code>seekOffset</code>+ 1, etc.
     * This does not actually write to the filesystem. {@link #saveRom(File)}
     * writes to the filesystem.
     * 
     * @param arg What to write at <code>seekOffset</code>.
     * @param len Number of bytes to write
     * @see #seek(int)
     * @see #writeSeek(char)
     * @see #writeSeek(char[])
     */
    public void writeSeek(char[] arg, int len) //write a [multibyte] string to
    // a place
    {
        for (int i = 0; i < len; i++)
        {
            if (!(seekOffset > this.length()))
            //don't write past the end of the ROM
            {
                writeSeek(arg[i]);
            }
            else
            {   /***/
                //System.out.println("Error: attempted write past end of
                // ROM.");
            }
        }
    }

    /**
     * Reads an <code>int[]</code> from <code>seekOffset</code> in the rom
     * and moves <code>seekOffset</code>.<code>seekOffset</code> will
     * point to the byte after the last byte read. This may be past the end of
     * the ROM.
     * 
     * @param length Number of bytes to read.
     * @return <code>int[]</code> at <code>seekOffset</code> with a length
     *         of <code>length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     * @see #seek(int)
     * @see #readSeek()
     */
    public int[] readSeek(int length)
    {
        int[] returnValue = new int[length];
        readSeek(returnValue, length);
        return returnValue;
    }

    /**
     * Reads an <code>int[]</code> from <code>seekOffset</code> in the rom
     * and moves <code>seekOffset</code>.<code>seekOffset</code> will
     * point to the byte after the last byte read. This may be past the end of
     * the ROM.
     * 
     * @param target <code>int[]</code> to read into
     * @param length Number of bytes to read.
     * @see #seek(int)
     * @see #readSeek()
     */
    public void readSeek(int[] target, int length)
    {
        for (int i = 0; i < length; i++)
        {
            target[i] = this.readSeek();
        }
    }

    /**
     * Reads an <code>int[]</code> from <code>seekOffset</code> in the rom
     * and moves <code>seekOffset</code>.<code>seekOffset</code> will
     * point to the byte after the last byte read. This may be past the end of
     * the ROM.
     * 
     * @param target <code>int[]</code> to read into
     * @see #seek(int)
     * @see #readSeek()
     * @see #readSeek(int[], int)
     */
    public void readSeek(int[] target)
    {
        readSeek(target, target.length);
    }

    /**
     * Reads <code>length</code> multibyte indexes into an <code>int[]</code>
     * from offset <code>seekOffset</code> in the rom and moves
     * <code>seekOffset</code>.<code>seekOffset</code> will point to the
     * byte after the last byte read. This may be past the end of the ROM.
     * 
     * @param length Number of indexes to read.
     * @param bytes Number of bytes per index
     * @return <code>int[]</code> at <code>seekOffset</code> with a length
     *         of <code>length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     * @see #seek(int)
     * @see #readSeek()
     * @see #readMultiSeek(int)
     * @see #readMultiSeek(int[], int)
     * @see #readMultiSeek(int[], int, int)
     */
    public int[] readMultiSeek(int length, int bytes)
    {
        int[] returnValue = new int[length];
        readMultiSeek(returnValue, length, bytes);
        return returnValue;
    }

    /**
     * Reads <code>length</code> multibyte indexes into <code>target</code>
     * from offset <code>seekOffset</code> in the rom and moves
     * <code>seekOffset</code>.<code>seekOffset</code> will point to the
     * byte after the last byte read. This may be past the end of the ROM.
     * 
     * @param target <code>int[]</code> to read into
     * @param length Number of indexes to read.
     * @param bytes Number of bytes per index
     * @return <code>int[]</code> at <code>seekOffset</code> with a length
     *         of <code>length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     * @see #seek(int)
     * @see #readSeek()
     * @see #readMultiSeek(int)
     * @see #readMultiSeek(int, int)
     * @see #readMultiSeek(int[], int)
     */
    public void readMultiSeek(int[] target, int length, int bytes)
    {
        for (int i = 0; i < length; i++)
        {
            target[i] = this.readMultiSeek(bytes);
        }
    }

    /**
     * Reads <code>target.length</code> multibyte indexes into
     * <code>target</code> from offset <code>seekOffset</code> in the rom
     * and moves <code>seekOffset</code>.<code>seekOffset</code> will
     * point to the byte after the last byte read. This may be past the end of
     * the ROM.
     * 
     * @param target <code>int[]</code> to read into
     * @param bytes Number of bytes per index
     * @return <code>int[]</code> at <code>seekOffset</code> with a length
     *         of <code>target.length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     * @see #seek(int)
     * @see #readSeek()
     * @see #readMultiSeek(int)
     * @see #readMultiSeek(int, int)
     * @see #readMultiSeek(int[], int, int)
     */
    public void readMultiSeek(int[] target, int bytes)
    {
        readMultiSeek(target, target.length, bytes);
    }

    /**
     * Reads a <code>byte[]</code> from <code>offset</code> in the rom and
     * moves <code>seekOffset</code>.<code>seekOffset</code> will point to
     * the byte after the last byte read. This may be past the end of the ROM. *
     * 
     * @param length Number of bytes to read.
     * @return <code>byte[]</code> at <code>seekOffset</code> with a length
     *         of <code>length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     */
    public byte[] readByteSeek(int length)
    {
        byte[] returnValue = new byte[length];
        for (int i = 0; i < length; i++)
        {
            returnValue[i] = readByteSeek();
        }
        return returnValue;
    }

    /**
     * Reads an <code>char</code> from <code>seekOffset</code> in the rom
     * and increments <code>seekOffset</code>.
     * 
     * @return <code>char</code> at <code>seekOffset</code>. If
     *         <code>seekOffset<code> is past the end of the rom then it is -1.
     */
    public char readCharSeek()
    {
        return (char) readSeek();
    }

    /**
     * Reads an <code>char[]</code> from <code>seekOffset</code> in the rom
     * and moves <code>seekOffset</code>.<code>seekOffset</code> will
     * point to the byte after the last byte read. This may be past the end of
     * the ROM.
     * 
     * @param length Number of bytes to read.
     * @return <code>char[]</code> at <code>seekOffset</code> with a length
     *         of <code>length</code>. If
     *         <code>seekOffset &gt; the rom.length</code> then it is -1.
     */
    public char[] readCharSeek(int length)
    {
        char[] returnValue = new char[length];
        for (int i = 0; i < length; i++)
        {
            returnValue[i] = readCharSeek();
        }
        return returnValue;
    }

    /**
     * Reads a mulibyte number with the specified length from
     * <code>seekOffset</code> in the rom and moves <code>seekOffset</code>.
     * <code>seekOffset</code> will point to the byte after the last byte
     * read. This may be past the end of the ROM. Reverses the byte order to get
     * the correct value.
     * 
     * @param len How many bytes long the number is.
     * @return Multibyte value as an int.
     */
    public int readMultiSeek(int len)
    {
        int out = 0;
        for (int i = 0; i < len; i++)
        {
            out += readSeek() << (i * 8);
        }
        return out;
    }

    /**
     * Writes the specified range from an orginal ROM into this ROM.
     * 
     * @param offset range to start reseting to orginal
     * @param len number of bytes to reset to orginal
     * @param orgRom orginal ROM to read from
     * @see net.starmen.pkhack.eb.ResetButton
     */
    public void resetArea(int offset, int len, AbstractRom orgRom)
    {
        write(offset, orgRom.readByte(offset, len));
    }

    /**
     * Checks if an <code>int[]</code> matches a specified part of the ROM.
     * 
     * @param offset Where in the rom to compare to.
     * @param values What to compare to.
     * @param len How many bytes to compare.
     * @return True if the same, false if different.
     */
    public boolean compare(int offset, int[] values, int len)
    {
        for (int i = 0; i < len; i++)
            if (this.read(offset + i) != values[i])
                return false;
        return true;
    }

    /**
     * Searches for an <code>int[]</code> in the ROM.
     * 
     * @param offset offset to start search at
     * @param values values to search for
     * @param len how many bytes of values to look at
     * @return offset <code>values</code> was found at in the ROM or -1 on
     *         failure.
     */
    public int find(int offset, int[] values, int len)
    {
        int rl = length(); //rom length
        for (int i = offset; i < rl; i++)
        {
            if (compare(i, values, len))
                return i;
        }
        return -1;
    }

    /**
     * Checks if an <code>int[]</code> matches a specified part of the ROM.
     * 
     * @param offset Where in the rom to compare to.
     * @param values What to compare to.
     * @return True if the same, false if different.
     */
    public boolean compare(int offset, int[] values)
    {
        return compare(offset, values, values.length);
    }

    /**
     * Checks if an <code>byte[]</code> matches a specified part of the ROM.
     * 
     * @param offset Where in the rom to compare to.
     * @param values What to compare to.
     * @param len How many bytes to compare.
     * @return True if the same, false if different.
     */
    public boolean compare(int offset, byte[] values, int len)
    {
        for (int i = 0; i < len; i++)
            if (this.readByte(offset + i) != values[i])
                return false;
        return true;
    }

    /**
     * Checks if an <code>byte[]</code> matches a specified part of the ROM.
     * 
     * @param offset Where in the rom to compare to.
     * @param values What to compare to.
     * @return True if the same, false if different.
     */
    public boolean compare(int offset, byte[] values)
    {
        return compare(offset, values, values.length);
    }

    /**
     * Searches for an <code>byte[]</code> in the ROM.
     * 
     * @param offset offset to start search at
     * @param values values to search for
     * @param len how many bytes of values to look at
     * @return offset <code>values</code> was found at in the ROM or -1 on
     *         failure.
     */
    public int find(int offset, byte[] values, int len)
    {
        int rl = length(); //rom length
        for (int i = offset; i < rl; i++)
        {
            if (compare(i, values, len))
                return i;
        }
        return -1;
    }

    /**
     * Searches for an <code>byte[]</code> in the ROM.
     * 
     * @param offset offset to start search at
     * @param values values to search for
     * @return offset <code>values</code> was found at in the ROM or -1 on
     *         failure.
     * @see #find(int, byte[], int)
     */
    public int find(int offset, byte[] values)
    {
        return find(offset, values, values.length);
    }

    /**
     * Expands an Earthbound ROM from 24 megabits to 32 megabits. Will fail if
     * ROM is already expanded or is not Earthbound. Important implementation
     * note: override {@link #_expand()}to do actual expansion, not this.
     * 
     * @return True if succesful, false if ROM already expanded or not
     *         Earthbound.
     */
    public boolean expand()
    {
        //Only expand Earthbound ROMs that are unexpanded with a 0x200 header
        if ((getRomType().equals("Earthbound")) && length() == 0x300200)
            return _expand();
        else
            return false;
    }

    /**
     * Actual implementation of ROM expansion from 24 to 32 megabits. Called
     * only when this contains a 0x300200 byte EarthBound ROM. Adds a megabyte
     * of 4096 copies of the same 256 byte set. This 256 set is all zeros except
     * the last byte is 0x02.
     * 
     * @return true if expansion was successful
     */
    protected abstract boolean _expand();

    /**
     * Expands an Earthbound ROM from 24 or 32 megabits to 48 megabits. Will
     * fail if ROM is already expanded to 48 megabits or is not Earthbound.
     * Important implementation note: override {@link #_expandEx()}to do actual
     * expansion, not this.
     * 
     * @return True if succesful, false if ROM already expanded or not
     *         Earthbound.
     */
    public boolean expandEx()
    {
        //Only expand Earthbound ROMs that are expanded to 32 megabits and
        // have a 0x200 header. Expand from 24 megabits to 32 megabits first.
        expand();
        if ((getRomType().equals("Earthbound")) && length() == 0x400200)
            return _expandEx();
        else
            return false;
    }

    /**
     * Actual implementation of ROM expansion from 32 to 48 megabits. Called
     * only when this contains a 0x400200 byte EarthBound ROM. Adds 2MB of zeros
     * except the code from 0x008000-0x00FFFF has to be copied to
     * 0x408000-0x40FFFF (those addresses not counting 0x200 byte header).
     * 
     * @return true if expansion was successful
     */
    protected abstract boolean _expandEx();

    //class info functions
    /**
     * Returns a description of this class.
     * 
     * @return A short (one-line) description of this class.
     */
    public static String getDescription() //Return one line description of
    // class
    {
        return "Earthbound ROM wrapper class";
    }

    /**
     * Returns the version of this class as a <code>String</code>. Can have
     * any number of numbers and dots ex. "0.3.3.5".
     * 
     * @return The version of this class.
     */
    public static String getVersion() //Return version as a string that may
    // have more than one decimal point (.)
    {
        return "0.7";
    }

    /**
     * Returns the credits for this class.
     * 
     * @return The credits for this class.
     */
    public static String getCredits() //Return who made it
    {
        return "Written by AnyoneEB\n"
            + "Inspiration for faster file i/o from Cabbage\n"
            + "Idea for direct file IO mode from EBisumaru";
    }

    /**
     * Returns the path of the loaded file as a String. This is changed whenever
     * the ROM is loaded or saved to a different location.
     * 
     * @return The path of the loaded file.
     */
    public String getPath()
    {
        return path.toString();
    }

    /**
     * Returns the path of the loaded file as a File. This is changed whenever
     * the ROM is loaded or saved to a different location.
     * 
     * @return The path of the loaded file.
     */
    public File getFilePath()
    {
        return new File(getPath());
    }

    /**
     * Returns the number of bytes in the ROM.
     * 
     * @return int
     */
    public abstract int length();

    /**
     * Returns whether this AbstractRom object writes directly to the
     * filesystem.
     * 
     * @return boolean
     */
    public abstract boolean isDirectFileIO();

    /**
     * Returns an {@link IPSFile}object containing the differences between this
     * ROM and the specified ROM. The IPSFile will only contain differences
     * between the byte offsets <code>start</code> and <code>end</code>.
     * 
     * @see #createIPS(AbstractRom)
     * @param orgRom ROM to base patch off of.
     * @param start Byte offset to start looking for differences inclusive.
     * @param end Byte offset to stop looking for differences exclusive.
     * @return IPSFile
     */
    public IPSFile createIPS(AbstractRom orgRom, int start, int end)
    {
        if (start > this.length() - 1 || start > orgRom.length() - 1
            || end > this.length() || end > orgRom.length())
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

        int cStart = -1;
        int len = end - start;

        byte[] romData = readByte(start, len);

        for (int i = 0; i < len; i++)
        {
            if (romData[i] != orgRom.readByte(i + start))
            {
                if (cStart == -1)
                    cStart = i;
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

    /**
     * Returns an {@link IPSFile}object containing the differences between this
     * ROM and the specified ROM.
     * 
     * @see #createIPS(AbstractRom, int, int)
     * @param orgRom ROM to base patch off of.
     * @return IPSFile
     */
    public IPSFile createIPS(AbstractRom orgRom)
    {
        return this.createIPS(orgRom, 0, this.length());
    }

    /**
     * Applies the specified patch to this ROM.
     * 
     * @param ips IPSFile object to get patch from.
     * @return Returns true if successful, false if fails because ROM is not
     *         expanded.
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

    /**
     * Unapplies the specified patch from this ROM.
     * 
     * @param ips IPSFile object to get patch from.
     * @param orgRom <code>AbstractRom</code> to read orginal bytes from
     * @return Returns true if successful, false if fails because ROM is not
     *         expanded.
     */
    public boolean unapply(IPSFile ips, AbstractRom orgRom)
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

    /**
     * Checks if the specified patch has been applied to this ROM.
     * 
     * @param ips IPSFile object to get patch from.
     * @return True if applying this patch would have no effect
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
                            .charAt(j))
                            return false;
                    }
                }
                else
                {
                    for (int j = 0; j < ipsr.getRleSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr
                            .getRleInfo())
                            return false;
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
                            .get(j))
                            return false;
                    }
                }
                else
                {
                    for (int j = 0; j < ipsr.getRleSize(); j++)
                    {
                        if (this.read(ipsr.getOffset() + j) != ipsr
                            .getRleInfo())
                            return false;
                    }
                }
            }
        }
        return true;
    }
}