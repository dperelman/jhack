/*
 * Created on Feb 27, 2004
 */
package net.starmen.pkhack.eb;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * Base class for all Earthbound-only {@link net.starmen.pkhack.HackModule}'s.
 * 
 * @author AnyoneEB
 */
public abstract class EbHackModule extends HackModule
{
//    public final static String DEFAULT_BASE_DIR = "net" + File.separator +
// "starmen" + File.separator + "pkhack"
//    + File.separator + "eb" + File.separator;
    /**
     * Base directory for Earthbound files within the .jar file. Use with
     * {@link ClassLoader#getSystemResourceAsStream(java.lang.String)}in order
     * to read files within the .jar.
     */
    public final static String DEFAULT_BASE_DIR = "net/starmen/pkhack/eb/";
    /**
     * List of the names of the sound effects. Listed by the number Earthbound
     * uses to identify them.
     */
    public final static String[] soundEffects = new String[127];
    /**
     * List of the names of the music. Listed by the number Earthbound uses to
     * identify them.
     */
    public final static String[] musicNames = new String[0xC0];
    /**
     * List of the names of the inside battle sprites. Used by the enemies table
     * in the ROM.
     * 
     * @see net.starmen.pkhack.eb.EnemyEditor
     * @see BattleSpriteEditor
     */
    public final static String[] battleSpriteNames = new String[BattleSpriteEditor.NUM_ENTRIES];
    /**
     * List of names of the SPT entries. The first SPT entry name is
     * sptNames[0].
     */
    public final static String[] sptNames =
        new String[SpriteEditor.NUM_ENTRIES];
    /**
     * Number of PSI.
     * 
     * @see #effects
     * @see #actionType
     */
    public final static int NUM_PSI = 52;
    /**
     * Number of battle animations.
     * 
     * @see #effects
     * @see #actionType
     */
    public final static int NUM_BA = 34;
    /**
     * List of battle animation names
     */
    public final static String[] baNames = new String[NUM_BA];
    /**
     * List of PSI names. It is used by enemies editor for action arguements of
     * type TYPE_PSI.
     * 
     * @see net.starmen.pkhack.eb.EnemyEditor
     */
    public final static String[] psiNames = new String[NUM_PSI];
    /**
     * Number of effects.
     * 
     * @see #effects
     * @see #actionType
     */
    public final static int NUM_EFFECTS = 319;
    /**
     * Descriptions of effects for items and actions. Effect #0 is described by
     * <code>effects[0]</code>.
     */
    public final static String[] effects = new String[NUM_EFFECTS];
    /**
     * Array saying what type of action each action is. Used by EnemyEditor.
     * 
     * @see net.starmen.pkhack.eb.EnemyEditor
     */
    public final static int[] actionType = new int[NUM_EFFECTS];
    /**
     * Used in actionType[] to identify the type of action.
     * 
     * @see #actionType
     */
    public final static int TYPE_NORMAL = 0,
        TYPE_PSI = 1,
        TYPE_CALL = 2,
        TYPE_ITEM = 3;
    /**
     * Defines the ASCII values corresponding to the values for the credits
     * text. Use the credits text byte as the index to get the ASCII char.
     */
    public final static char[] creditsChars = new char[256];
    /**
     * List of town map names. Used by TownMapEditor.
     * 
     * @see TownMapEditor
     */
    public final static String[] townMapNames = new String[TownMapEditor.NUM_TOWN_MAPS];
    /**
     * List of logo screen names. Used by LogoScreenEditor.
     * 
     * @see LogoScreenEditor
     */
    public final static String[] logoScreenNames = new String[LogoScreenEditor.NUM_LOGO_SCREENS];

    public static String[] itemTypes = new String[17];
    
    private static boolean bigArraysInited = false;
    
    public boolean isRomSupported()
    {
        return rom.getRomType().equals("Earthbound");
    }
    /**
     * @param rom
     * @param prefs
     */
    public EbHackModule(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
        initBigArrays();
    }
    public int findFreeRange(int startAt, int length) throws EOFException
    {
	    // simple recursive spacefinder function
	    // finds the highest occurrence of a data-free block of size [length]
	    // checks for normal expanded area data (i.e., 255 [00]s, one [02], and
	    // so on)
	    // if it finds an interrupt in the pattern, it will skip that area and
	    // search just beyond it, thus leaving any user-modified data intact
	    // (hopefully)
	
	    int i;
	    for (i = startAt; i > startAt - (length - 1); i--)
	    {
	        if (i < 0x300200)
	            //only look at exp
	            throw new EOFException("No free range " + length
	                + " bytes long found.");
	        if (i < 0x410200 && i >= 0x408200)
	            startAt = i = 0x4081ff;
	        if (((i - 0x200) & 0xffff) == 0xffff)
	            startAt = i;
	        int ch = rom.read(i);
	
	        if (((i + 1) % 0x100) == 0 && i < 0x400200)
	        {
	            if (ch != 2)
	            {
	                //return findFreeRange(i - 1, length);
	                startAt = i - 1;
	            }
	        }
	        else
	        {
	            if (ch != 0)
	            {
	                //return findFreeRange(i - 1, length);
	                startAt = i - 1;
	            }
	        }
	    }
	    //        System.out.println(
	    //            "Free range found starting at 0x"
	    //                + addZeros(Integer.toHexString(i), 6)
	    //                + " "
	    //                + length
	    //                + " bytes long.");
	
	    return i;
	}
    public void nullifyArea(int address, int len)
    {
        for (int i = 0; i < len; i++)
        {
            int offset = address + i;
            //in the expanded meg every 256th byte should be [02] instead of
            // [00]
            rom.write(offset, offset < 0x400200 && offset > 0x300200 && 
                ((i + 1) % 0x100) == 0
                ? 0x02
                : 0x00);
        }
    }

    
    //EB graphics compression methods
    public static int getArrSize(byte[] b)
    {
        for (int i = b.length - 1; i >= 0; i--)
            if (b[i] != 0)
                return i + 1;
        return 0;
    }
    
    private static final int nativeComp, nativeCompMinor;
    private static final int LIBCOMP_COMP_VER = 1;
    private static final int LIBCOMP_DECOMP_VER = 3;
    static
    {
        int compver = -1, minor = -1;
        try
        {
            System.loadLibrary("comp");
            compver = getLibcompVersion();
            minor = getLibcompMinorVersion();
            System.out.println("Earthbound compression library libcomp v" +
                compver + "." + minor + " loaded.");
            
        }
        catch (Throwable e)
        {
            System.out.println("Error loading Earthbound compression library " +
                System.mapLibraryName("comp") + ".");
            e.printStackTrace(System.out);
        }
        nativeComp = compver;
        nativeCompMinor= minor;
    }
    
    /**
     * Returns the major version of the currently loaded libcomp. This will
     * throw an exception if there is no loaded libcomp. This tells what
     * functions this version of libcomp supports. Zero or negative mean no
     * functions. 1 means comp(). 2 means it also supports decomp().
     * 
     * @return major version number of currently loaded libcomp.
     */
    public static native int getLibcompVersion();
    /**
     * Returns the minor version of the currently loaded libcomp. This will
     * throw an exception if there is no loaded libcomp. This indicates the
     * bugfix/speed improvement level of this libcomp. The return value of this
     * function is not used except to show the user. It should be used to show
     * the differences between different releases.
     * 
     * @return minor version number of currently loaded libcomp.
     */
    public static native int getLibcompMinorVersion();
    /**
     * Returns string to be shown in credits for libcomp.
     * 
     * @return string to be shown in credits for libcomp
     */
    public static String getLibcompCreditsLine()
    {
        return (nativeComp < 0 ? "Using Java comp implementation" : 
            ("Using libcomp v" + nativeComp + "." + nativeCompMinor)) + 
            " based on cabbage's source, ported by AnyoneEB.";
    }
    
    /**
     * The decompressor function. Takes a pointer to the compressed block, a
     * pointer to the buffer which it decompresses into, and the maximum length.
     * Returns the number of bytes uncompressed, or -1 if decompression failed.
     * Ported by AnyoneEB from Cabbage's tile editor source.
     * 
     * @param cdata Address of compressed data
     * @param buffer byte arrray to put decompressed data into, must be inited
     *            to the right size
     * @param maxlen Maximum decompression length, probably
     *            <code>buffer.length</code>
     * @param rom <code>AbstractRom</code> to read from
     * @return <code>int[]</code> where [0] = Negitive on error, number of
     *         bytes decompressed into buffer otherwise. [1] = number of bytes
     *         read from ROM
     */
    public static int[] decomp(int cdata, byte[] buffer, int maxlen, AbstractRom rom)
    {
        if(nativeComp >= LIBCOMP_DECOMP_VER)
            return native_decomp(cdata, buffer, maxlen, rom);
        else
            return _decomp(cdata, buffer, maxlen, rom);
        
/*
 * Stopwatch nsw = new Stopwatch(), jsw = new Stopwatch(); nsw.start();
 * native_decomp(cdata, buffer, maxlen, rom); nsw.stop(); jsw.start();
 * _decomp(cdata, buffer, maxlen, rom); jsw.stop(); System.out.println("Java
 * decomp() took " + jsw + "; native decomp took " + nsw);
 * 
 * return native_decomp(cdata, buffer, maxlen, rom);
 */
    }
    private static native int[] native_decomp(int cdata, byte[] buffer, int maxlen, AbstractRom rom);
    private static int[] _decomp(int cdata, byte[] buffer, int maxlen, AbstractRom rom)
    {
        if(EbHackModule.bitrevs == null)
            initBitrevs();
        
        int start = cdata;
        int bpos = 0, bpos2 = 0;
        byte tmp;
        while (rom.read(cdata) != 0xFF)
        {
            int cmdtype = rom.read(cdata) >> 5;
            int len = (rom.read(cdata) & 0x1F) + 1;
            if (cmdtype == 7)
            {
                cmdtype = (rom.read(cdata) & 0x1C) >> 2;
                len = ((rom.read(cdata) & 3) << 8) + rom.read(cdata + 1) + 1;
                cdata++;
            }
            if (bpos + len > maxlen || bpos + len < 0)
                return new int[] { -1, cdata - start + 1 };
            cdata++;
            if (cmdtype >= 4)
            {
                bpos2 = (rom.read(cdata) << 8) + rom.read(cdata + 1);
                if (bpos2 >= maxlen || bpos2 < 0)
                    return new int[] { -2, cdata - start + 1 };
                cdata += 2;
            }

            switch (cmdtype)
            {
                case 0 : //uncompressed ?
                    System.arraycopy(rom.readByte(cdata, len), 0, buffer, bpos, len);
                    bpos += len;
                    cdata += len;
//                    for (int i = 0; i < len; i++)
//                    {
//                        buffer[bpos++] = rom.readByte(cdata++);
//                    }
                    break;
                case 1 : //RLE ?
                    Arrays.fill(buffer, bpos, bpos + len, rom.readByte(cdata));
                    bpos += len;
//                    for (int i = 0; i < len; i++)
//                    {
//                        buffer[bpos++] = rom.readByte(cdata);
//                    }
                    cdata++;
                    break;
                case 2 : //??? TODO way to do this with Arrays?
                    if (bpos + 2 * len > maxlen || bpos < 0)
                        return new int[] { -3, cdata - start + 1 };
                    while (len-- != 0)
                    {
                        buffer[bpos++] = rom.readByte(cdata);
                        buffer[bpos++] = rom.readByte(cdata + 1);
                    }
                    cdata += 2;
                    break;
                case 3 : //each byte is one more than previous ?
                    tmp = rom.readByte(cdata++);
                    while (len-- != 0)
                        buffer[bpos++] = tmp++;
                    break;
                case 4 : //use previous data ?
                    if (bpos2 + len > maxlen || bpos2 < 0)
                        return new int[] { -4, cdata - start + 1 };
                    System.arraycopy(buffer, bpos2, buffer, bpos, len);
                    bpos += len;
//                    for (int i = 0; i < len; i++)
//                    {
//                        buffer[bpos++] = buffer[bpos2 + i];
//                    }
                    break;
                case 5 :
                    if (bpos2 + len > maxlen || bpos2 < 0)
                        return new int[] { -5, cdata - start + 1 };
                    while (len-- != 0)
                    {
//                        tmp = buffer[bpos2++];
//                        /* reverse the bits */
//                        tmp =
//                            (byte) (((tmp >> 1) & 0x55) | ((tmp << 1) & 0xAA));
//                        tmp =
//                            (byte) (((tmp >> 2) & 0x33) | ((tmp << 2) & 0xCC));
//                        tmp =
//                            (byte) (((tmp >> 4) & 0x0F) | ((tmp << 4) & 0xF0));
//                        buffer[bpos++] = tmp;
                        buffer[bpos++] = bitrevs[buffer[bpos2++] & 0xff];
                    }
                    break;
                case 6 :
                    if (bpos2 - len + 1 < 0)
                        return new int[] { -6, cdata - start + 1 };
                    while (len-- != 0)
                        buffer[bpos++] = buffer[bpos2--];
                    break;
                case 7 :
                    return new int[] { -7, cdata - start + 1 };
            }
        }
        return new int[] { bpos, cdata - start + 1 };
    }
    /**
     * The decompressor function. Takes a pointer to the compressed block, a
     * pointer to the buffer which it decompresses into, and the maximum length.
     * Returns the number of bytes uncompressed, or -1 if decompression failed.
     * Ported by AnyoneEB from Cabbage's tile editor source.
     * 
     * @param cdata Address of compressed data
     * @param buffer byte arrray to put decompressed data into, must be inited
     *            to the right size
     * @param maxlen Maximum decompression length, probably
     *            <code>buffer.length</code>
     * @return <code>int[]</code> where [0] = Negitive on error, number of
     *         bytes decompressed into buffer otherwise. [1] = number of bytes
     *         read from ROM
     */
    public int[] decomp(int cdata, byte[] buffer, int maxlen)
    {
        return decomp(cdata, buffer, maxlen, rom);
    }
    /**
     * The decompressor function. Takes a pointer to the compressed block and a
     * pointer to the buffer which it decompresses into sized to the maximum
     * length. Returns the number of bytes uncompressed, or -1 if decompression
     * failed. Ported by AnyoneEB from Cabbage's tile editor source.
     * 
     * @param cdata Address of compressed data
     * @param buffer byte arrray to put decompressed data into, must be inited
     *            to the right size
     * @param rom <code>AbstractRom</code> to read from
     * @return <code>int[]</code> where [0] = Negitive on error, number of
     *         bytes decompressed into buffer otherwise. [1] = number of bytes
     *         read from ROM
     */
    public static int[] decomp(int cdata, byte[] buffer, AbstractRom r)
    {
        return decomp(cdata, buffer, buffer.length, r);
    }
    /**
     * The decompressor function. Takes a pointer to the compressed block and a
     * pointer to the buffer which it decompresses into sized to the maximum
     * length. Returns the number of bytes uncompressed, or -1 if decompression
     * failed. Ported by AnyoneEB from Cabbage's tile editor source.
     * 
     * @param cdata Address of compressed data
     * @param buffer byte arrray to put decompressed data into, must be inited
     *            to the right size
     * @return <code>int[]</code> where [0] = Negitive on error, number of
     *         bytes decompressed into buffer otherwise. [1] = number of bytes
     *         read from ROM
     */
    public int[] decomp(int cdata, byte[] buffer)
    {
        return decomp(cdata, buffer, buffer.length);
    }

    /* Used internally by comp */
    private static byte[] bitrevs = null;
    private static void initBitrevs()
    {
        EbHackModule.bitrevs = new byte[256];
        for (int tmp = 0; tmp < 256; tmp++)
        {
            byte x = (byte) tmp;
            x = (byte) (((x >> 1) & 0x55) | ((x << 1) & 0xAA));
            x = (byte) (((x >> 2) & 0x33) | ((x << 2) & 0xCC));
            EbHackModule.bitrevs[tmp] = (byte) (((x >> 4) & 0x0F) | ((x << 4) & 0xF0));
        }
    }
    //private static int bpos, pos, buffer[], udata[];
    //returns bpos because it gets changed
    private static int encode(int length, int type, byte[] buffer, int bpos)
    {
        if (length > 32)
        {
            buffer[bpos++] = (byte) (0xE0 + 4 * type + ((length - 1) >> 8));
            buffer[bpos++] = (byte) ((length - 1) & 0xFF);
        }
        else
            buffer[bpos++] = (byte) (0x20 * type + length - 1);

        return bpos;
    }
    private static int rencode(
        int length,
        byte[] buffer,
        int bpos,
        byte[] udata,
        int pos)
    {
        if (length <= 0)
            return bpos;
        bpos = encode(length, 0, buffer, bpos);
        //memcpy(bpos, pos, length);
        System.arraycopy(udata, pos, buffer, bpos, length);
//        for (int i = 0; i < length; i++)
//        {
//            buffer[bpos + i] = udata[pos + i];
//        }
        bpos += length;

        return bpos;
    }
    //meant to emulate the C function FOR THIS SPECIFIC USE
    private static int memchr(int st, byte needle, int len, byte[] haystack)
    {
        len += st;
        for (int i = st; i < len; i++)
        {
            if (haystack[i] == needle)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * The compressor function. Takes a pointer to the uncompressed block, a
     * pointer to 65536 bytes which it compresses into, and the size of the
     * uncompressed block. Returns the number of bytes compressed. Ported by
     * AnyoneEB from Cabbage's tile editor source.
     * 
     * @param udata Uncompressed data
     * @param buffer Output buffer of compressed data
     * @param limit Length of uncompressed data
     * @return Length of compressed output.
     */
    public static int comp(final byte[] udata, final byte[] buffer, final int limit)
    {
        //Stopwatch jsw = new Stopwatch();
        //Stopwatch nsw = new Stopwatch();
        //int nret = -255, jret = -255;
        if(nativeComp >= LIBCOMP_COMP_VER)
        {
            //nsw.start();
            return native_comp(udata, buffer, limit);
            //nsw.stop();
        }
        else
        {
            //jsw.start();
            return _comp(udata, buffer, limit);
            //jsw.stop();
        }
        //System.out.println("Native comp()="+ nret +" took " +
        // nsw.toString());
        //System.out.println("Java comp()=" + jret + " took " +
        // jsw.toString());
        //return jret;
    }
    private static native int native_comp(final byte[] udata, final byte[] buffer, final int limit);
    
    private static int _comp(final byte[] udata, final byte[] buffer, final int limit)
    {
//        Stopwatch sw = new Stopwatch(), swf = new Stopwatch(); //sw temp
//        sw.start();
        //Tileset.buffer = buffer;
        //Tileset.udata = udata;
        //unsigned char *limit = &udata[length]; //udata start = 0, so limit =
        // length
        //int limit = length; //probably unneeded, could just use length
        int pos2 = 0, pos3 = 0;
//        byte bitrevs[] = new byte[256];
        int tmp;
        int bpos = 0; //position in buffer
        int pos = 0; //postition in udata
//        for (tmp = 0; tmp < 256; tmp++)
//        {
//            byte x = (byte) tmp;
//            x = (byte) (((x >> 1) & 0x55) | ((x << 1) & 0xAA));
//            x = (byte) (((x >> 2) & 0x33) | ((x << 2) & 0xCC));
//            bitrevs[tmp] = (byte) (((x >> 4) & 0x0F) | ((x << 4) & 0xF0));
//        }
        if(EbHackModule.bitrevs == null)
            initBitrevs();
        while (pos < limit)
        {
            try
            {
	            /* Look for patterns */
	            mainloop : for (
	                pos2 = pos; pos2 < limit && pos2 < pos + 1024; pos2++)
	            {
	//                try
	//                {
	//                    swf.stop();
	//                    System.out.println("comp() mainloop: " + swf);
	//                }
	//                catch(IllegalStateException e){}
	//                swf.start();
	                for (pos3 = pos2;
	                    pos3 < limit
	                        && pos3 < pos2 + 1024
	                        && udata[pos2] == udata[pos3];
	                    pos3++);
	                if (pos3 - pos2 >= 3)
	                {
	                    bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                    bpos = encode(pos3 - pos2, 1, buffer, bpos);
	                    buffer[bpos++] = udata[pos2];
	                    pos = pos3;
	                    break;
	                }
	                for (pos3 = pos2;
	                    pos3 + 1 < limit
	                        && pos3 < pos2 + 2048
	                        && udata[pos3] == udata[pos2]
	                        && udata[pos3 + 1] == udata[pos2 + 1];
	                    pos3 += 2);
	                if (pos3 - pos2 >= 6)
	                {
	                    bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                    bpos = encode((pos3 - pos2) / 2, 2, buffer, bpos);
	                    buffer[bpos++] = udata[pos2];
	                    buffer[bpos++] = udata[pos2 + 1];
	                    pos = pos3;
	                    break;
	                }
	                for (tmp = 0, pos3 = pos2;
	                    pos3 < limit
	                        && pos3 < pos2 + 1024
	                        && udata[pos3] == udata[pos2] + tmp;
	                    pos3++, tmp++);
	                if (pos3 - pos2 >= 4)
	                {
	                    bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                    bpos = encode(pos3 - pos2, 3, buffer, bpos);
	                    buffer[bpos++] = udata[pos2];
	                    pos = pos3;
	                    break;
	                }
	                for (pos3 = 0;
	                    (pos3 = memchr(pos3, udata[pos2], pos2 - pos3, udata))
	                        != -1;
	                    pos3++)
	                {
	                    for (tmp = 0;
	                    	pos2 + tmp < limit &&
	                        udata[pos3 + tmp] == udata[pos2 + tmp]
	                            && tmp < pos2 - pos3
	                            && tmp < 1024;
	                        tmp++);
	                    if (tmp >= 5)
	                    {
	                        bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                        bpos = encode(tmp, 4, buffer, bpos);
	                        buffer[bpos++] = (byte) ((pos3) >> 8);
	                        buffer[bpos++] = (byte) ((pos3) & 0xFF);
	                        pos = pos2 + tmp;
	                        break mainloop;
	                    }
	                    for (tmp = 0;
	                        tmp <= pos3 && pos2 + tmp < limit
	                            && udata[pos3 - tmp] == udata[pos2 + tmp]
	                            && tmp < 1024;
	                        tmp++);
	                    if (tmp >= 5)
	                    {
	                        bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                        bpos = encode(tmp, 6, buffer, bpos);
	                        buffer[bpos++] = (byte) ((pos3) >> 8);
	                        buffer[bpos++] = (byte) ((pos3) & 0xFF);
	                        pos = pos2 + tmp;
	                        break mainloop;
	                    }
	                }
	                for (pos3 = 0;
	                    (pos3 =
	                        memchr(
	                            pos3,
	                            EbHackModule.bitrevs[udata[pos2] & 255],
	                            pos2 - pos3,
	                            udata))
	                        != -1;
	                    pos3++)
	                {
	                    for (tmp = 0;
	                    	pos2 + tmp < limit &&
	                        udata[pos3 + tmp] == EbHackModule.bitrevs[udata[pos2 + tmp] & 255]
	                            && tmp < pos2 - pos3
	                            && tmp < 1024;
	                        tmp++);
	                    if (tmp >= 5)
	                    {
	                        bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
	                        bpos = encode(tmp, 5, buffer, bpos);
	                        buffer[bpos++] = (byte) ((pos3) >> 8);
	                        buffer[bpos++] = (byte) ((pos3) & 0xFF);
	                        pos = pos2 + tmp;
	                        break mainloop;
	                    }
	                }
	            }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            DONE:
            /* Can't compress, so just use 0 (raw) */
            bpos = rencode(pos2 - pos, buffer, bpos, udata, pos);
            if (pos < pos2)
                pos = pos2;
        }
        buffer[bpos++] = (byte) 0xFF;
//        sw.stop();
//        System.out.println("comp(): " + sw);
        return bpos;
    }
    /**
     * The compressor function. Takes a pointer to the uncompressed block and a
     * pointer to 65536 bytes which it compresses into. Returns the number of
     * bytes compressed. Ported by AnyoneEB from Cabbage's tile editor source.
     * 
     * @param udata Uncompressed data
     * @param buffer Output buffer of compressed data
     * @return Length of compressed output.
     */
    public static int comp(final byte[] udata, final byte[] buffer)
    {
        return comp(udata, buffer, udata.length);
    }
    
    /**
     * Simple conversion from a regular <code>char</code> to an EarthBound
     * <code>char</code>. Adds 0x30 to the character value. Simple because it
     * doesn't handle control codes or compression. Works fine for the scattered
     * strings not accessable by the text editor.
     * 
     * @param regChr A regular <code>char</code>
     * @return An EarthBound <code>char</code>
     * @see #simpToGameString(char[])
     * @see #simpToRegChar(char)
     * @see #simpToRegString(char[])
     */
    public char simpToGameChar(char regChr)
    {
        if (regChr < 32) return '\0';
        return (char) (regChr + 0x30);
    }

    /**
     * Simple conversion from a regular <code>char[]</code> to an EarthBound
     * <code>char[]</code>. Adds 0x30 to the character value. Simple because
     * it doesn't handle control codes or compression. Works fine for the
     * scattered strings not accessable by the text editor. Calls
     * {@link #simpToGameChar(char)}on every <code>char</code> in the array.
     * 
     * @param string An array of regular <code>char</code>'s
     * @return An array of EarthBound <code>char</code>'s
     * @see #simpToGameChar(char)
     * @see #simpToRegChar(char)
     * @see #simpToRegString(char[])
     */
    public  char[] simpToGameString(char[] string)
    {
        for (int i = 0; i < string.length; i++)
        {
            string[i] = simpToGameChar(string[i]);
        }
        return string;
    }

    /**
     * Simple conversion from an EarthBound <code>char</code> to a regular
     * <code>char</code>. Subracts 0x30 from the character value. Simple
     * because it doesn't handle control codes or compression. Works fine for
     * the scattered strings not accessable by the text editor.
     * 
     * @param gameChr An EarthBound <code>char</code>
     * @return A regular <code>char</code>
     * @see #simpToGameChar(char)
     * @see #simpToGameString(char[])
     * @see #simpToRegString(char[])
     */
    public char simpToRegChar(char gameChr)
    {
        if (gameChr < 80) return '\0';
        return (char) (gameChr - 0x30);
    }

    /**
     * Simple conversion from an EarthBound <code>char</code> to a regular
     * <code>char</code>. Subracts 0x30 from the character value. Simple
     * because it doesn't handle control codes or compression. Works fine for
     * the scattered strings not accessable by the text editor. Calls
     * {@link #simpToRegChar(char)}on every <code>char</code> in the array.
     * 
     * @param string An array of EarthBound <code>char</code>'s
     * @return An arry of regular <code>char</code>'s
     * @see #simpToGameChar(char)
     * @see #simpToGameString(char[])
     * @see #simpToRegString(char[])
     */
    public  char[] simpToRegString(char[] string)
    {
        for (int i = 0; i < string.length; i++)
        {
            string[i] = simpToRegChar(string[i]);
        }

        return string;
    }
    
    /**
     * Simple conversion from an EarthBound credits <code>char</code> to a
     * regular <code>char</code>. Look at the <code>creditsChars</code>
     * array. Simple because it doesn't handle control codes or compression. Use
     * for opening and ending credits text.
     * 
     * @param credChr An EarthBound credits <code>char</code>
     * @return A regular <code>char</code>
     * @see #creditsChars
     */
    public static char simpCreditsToRegChar(char credChr)
    {
        return creditsChars[credChr];
    }
    /**
     * Simple conversion from a regular <code>char</code> to an EarthBound
     * credits <code>char</code>. Look at the <code>creditsChars</code>
     * array. Simple because it doesn't handle control codes or compression. Use
     * for opening and ending credits text.
     * 
     * @param regChr An regular <code>char</code>
     * @return An EarthBound credits <code>char</code>
     * @see #creditsChars
     */
    public static char simpRegToCreditsChar(char regChr)
    {
        for(char c = 0; c < 256; c++)
            if(creditsChars[c] == regChr)
                return c;
        return 0;
    }

//    public static SimpleComboBoxModel createSptComboBoxModel(final boolean
// zeroBased)
//    {
//        return createComboBoxModel(sptNames, zeroBased, "Nothing");
//    }
//    protected static void addSptDataListener(ListDataListener ldl)
//    {
//        addDataListener(sptNames, ldl);
//    }
//    protected static void removeSptDataListener(ListDataListener ldl)
//    {
//        removeDataListener(sptNames, ldl);
//    }
//    protected static void notifySptDataListeners(ListDataEvent lde)
//    {
//        notifyDataListeners(sptNames, lde);
//    }
//    public static JComboBox createSptComboBox(
//        boolean zeroBased,
//        final ActionListener al)
//    {
//        return createComboBox(sptNames, zeroBased, "Nothing", al);
//    }
    
    public String getDefaultBaseDir()
    {
        return DEFAULT_BASE_DIR;
    }
    
    /**
     * Writes {@link #effects}and {@link #actionType}into effects.txt. The two
     * arrays are converted so they are together and then are written to a
     * single file with <code>writeArray()</code>.
     * 
     * @see #writeArray(String, boolean, String[])
     * @see #effects
     * @see #actionType
     */
    public void writeEffects()
    {
        String[] out = new String[NUM_EFFECTS];
        for (int i = 0; i < NUM_EFFECTS; i++)
        {
            String typeStr = new String();
            switch (actionType[i])
            {
                case TYPE_NORMAL :
                    typeStr = "NORMAL";
                    break;
                case TYPE_PSI :
                    typeStr = "PSI";
                    break;
                case TYPE_ITEM :
                    typeStr = "ITEM";
                    break;
                case TYPE_CALL :
                    typeStr = "CALL";
                    break;
            }
            out[i] = typeStr + " - " + effects[i];
        }
        writeArray("effects.txt", false, out);
    }    
    /**
     * Sets up big arrays discribing parts of the game. It will only set up the
     * arrays the first time it is called so it is okay to call it to make sure
     * they are set up.
     * 
     * @see #battleSpriteNames
     * @see #actionType
     * @see #psiNames
     * @see #soundEffects
     * @see #readArray(String, String, boolean, String[])
     * @see #readArray(String, String, String, boolean, String[], int)
     */
    public static void initBigArrays()
    {
        if (!bigArraysInited)
        {
            readArray(DEFAULT_BASE_DIR,"soundeffectslisting.txt", true, soundEffects);
            //readArray(DEFAULT_BASE_DIR,"psiNames.txt", false, psiNames);
            //readArray(DEFAULT_BASE_DIR,"insideSprites.txt", false,
            // battleSpriteNames);
            readArray(DEFAULT_BASE_DIR,"musiclisting.txt", true, musicNames);
            //readEffects(null);
            initCreditsChars();
            try
            {
                itemTypes = new CommentedLineNumberReader(new InputStreamReader(
                    ClassLoader.getSystemResourceAsStream(DEFAULT_BASE_DIR
                        + "itemTypes.txt"))).readUsedLines();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                itemTypes = new String[0];
            }

            bigArraysInited = true;
        }
    }
    
    protected static void readEffects(String romPath)
    {
        readArray(DEFAULT_BASE_DIR,"effects.txt", romPath, false, effects);
        for (int i = 0; i < effects.length; i++)
        {
            String[] tmp = effects[i].split("-", 2);
            effects[i] = tmp[1].trim();
            String type = tmp[0].trim();
            if (type.equalsIgnoreCase("normal"))
                actionType[i] = TYPE_NORMAL;
            else if (type.equalsIgnoreCase("psi"))
                actionType[i] = TYPE_PSI;
            else if (type.equalsIgnoreCase("item"))
                actionType[i] = TYPE_ITEM;
            else if (type.equalsIgnoreCase("call"))
                actionType[i] = TYPE_CALL;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        super.reset();
    }
    
    private static void initCreditsChars()
    {
        creditsChars[129] = 'A';
        creditsChars[130] = 'B';
        creditsChars[131] = 'C';
        creditsChars[132] = 'D';
        creditsChars[133] = 'E';
        creditsChars[134] = 'F';
        creditsChars[135] = 'G';
        creditsChars[136] = 'H';
        creditsChars[137] = 'I';
        creditsChars[138] = 'J';
        creditsChars[139] = 'K';
        creditsChars[140] = 'L';
        creditsChars[141] = 'M';
        creditsChars[142] = 'N';
        creditsChars[143] = 'O';
        creditsChars[160] = 'P';
        creditsChars[161] = 'Q';
        creditsChars[162] = 'R';
        creditsChars[163] = 'S';
        creditsChars[164] = 'T';
        creditsChars[165] = 'U';
        creditsChars[166] = 'V';
        creditsChars[167] = 'W';
        creditsChars[168] = 'X';
        creditsChars[169] = 'Y';
        creditsChars[170] = 'Z';
        creditsChars[66] = 'a';
        creditsChars[82] = 'b';
        creditsChars[68] = 'c';
        creditsChars[84] = 'd';
        creditsChars[69] = 'e';
        creditsChars[85] = 'f';
        creditsChars[70] = 'g';
        creditsChars[86] = 'h';
        creditsChars[72] = 'i';
        creditsChars[88] = 'j';
        creditsChars[73] = 'k';
        creditsChars[89] = 'l';
        creditsChars[74] = 'm';
        creditsChars[90] = 'n';
        creditsChars[75] = 'o';
        creditsChars[91] = 'p';
        creditsChars[122] = 'r';
        creditsChars[107] = 's';
        creditsChars[123] = 't';
        creditsChars[108] = 'u';
        creditsChars[124] = 'v';
        creditsChars[109] = 'w';
        creditsChars[125] = 'x';
        creditsChars[110] = 'y';
        creditsChars[126] = 'z';
        creditsChars[76] = '<';
        creditsChars[0x4e] = '>';
        creditsChars[80] = '_'; //why is this here?
        creditsChars[64] = ' ';
        creditsChars[0xad] = '.';
        
        creditsChars[0x80] = '-';
        creditsChars[0x47] = '\'';
        creditsChars[0x6f] = '?';
    }
}
