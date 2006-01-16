/*
 * Created on Dec 24, 2003
 */
package net.starmen.pkhack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.starmen.pkhack.eb.EbHackModule;
import net.starmen.pkhack.eb.TextEditor.StrInfo;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class CCInfo
{
    public static final int TYPE_CODE = 0;
    public static final int TYPE_ARGLIST = 1;

    public static class CCNode implements Comparable
    {
        public String cc; //how cc looks in file
        public String desc; //desc from file

        public int level; // depth, i.e., CC byte # (sorta)
        public int value; // byte value (-1 for arguments)

        public int type; // code byte or argument
        public int num_arg; // number of argument bytes
        public int[] args = new int[32]; // argument list
        public boolean arg_multiplier;
        // if true, number of arguments remaining is multiplied by the
        // value of the first ('!')

        public boolean endcc; //if true this CC ends a text block ('/')

        public boolean menu_raise; //if true, increases menu level ('{')
        public boolean menu_lower; //if true, decreases menu level ('}')
        //a negitive menu level results in the end of a text block

        public boolean isTerminator;

        public CCNode parent;
        public List nodes = new ArrayList();

        public String toString()
        {
            return "[" + cc + "]";
        }

        public int compareTo(Object obj)
        {
            return this.toString().compareTo(obj.toString());
        }
    }

    public static class CompressionPattern implements Comparable
    {
        private Pattern pattern;
        private String replace;
        private Integer length;

        public CompressionPattern(int num)
        {
            length = new Integer(comprTable[num].length * -1);
            pattern = Pattern.compile(new String(comprTable[num]).replaceAll(
                "(\\(|\\)|\\.|\\\\|\\{|\\}|\\?)", "\\\\$1"));
            replace = "[" + (15 + (num >> 8)) + " "
                + HackModule.addZeros(Integer.toHexString(num & 0xff), 2) + "]";
        }

        public String compress(String str)
        {
            try
            {
                return pattern.matcher(str).replaceAll(replace);
            }
            catch (PatternSyntaxException e)
            {
                System.out.println("Pattern errror at " + replace + " ("
                    + pattern.pattern() + ")");
                return str;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object obj)
        {
            try
            {
                return length.compareTo(((CompressionPattern) obj).length);
            }
            catch (ClassCastException e)
            {
                System.out
                    .println("Compression Pattern ERROR: compareTo() called wrong.");
                return pattern.toString().compareTo(obj.toString());
            }
        }
    }

    public static char[][] comprTable = new char[768][];
    public static CompressionPattern[] comprPatTable;

    public CCNode ccTable = new CCNode(); // initial node
    private static final int MAX_ARG_MULT = 0xff;
    private AbstractRom rom;
    private boolean allowComp = false, crdChr = false;
    private static final int asciiOff = 0x30;

    public CCInfo(String codefile, AbstractRom rom, boolean allowComp,
        boolean crdChr)
    {
        this.rom = rom;
        this.allowComp = allowComp;
        this.crdChr = crdChr;

        createCCTable(ccTable, codefile);
        //        System.out.println(
        //            "("
        //                + new Date().toGMTString()
        //                + ") Done with CC table, starting on compression table.");
        createCompressionTable(comprTable);
    }

    public CCInfo(String codefile, AbstractRom rom)
    {
        this(codefile, rom, true, false);
    }

    public StrInfo readString(int address)
    {
        // Reads a string fHackModule.rom ROM at address.
        // Returns a struct containing a pointer to the string and
        // the string's length. This function does not do any formatting
        // or decompression.

        //        char[] buffer = new char[8192];
        //        char[] headBuff = new char[100];
        StringBuffer buffer = new StringBuffer(), headBuff = new StringBuffer();
        int headlen = 100;

        int pos = 0, hpos = 0;
        int len = 0;
        int offset = address;

        CCNode activeNode = ccTable, prevNode = null;

        boolean opcode = false, endcc = false;
        int curcode = 0;
        int arglevel = 0;
        int menulevel = 0;

        int ch;

        for (int i = 0; i < 8192; i++)
        {
            ch = rom.readChar(offset++);
            len++;

            if (arglevel > 0)
            {
                buffer.append((char) ch);

                arglevel--;
                continue;
            }
            if (endcc)
            {
                break;
            }

            if (!opcode)
            {
                if (isCC(ch))
                {
                    opcode = true;
                    curcode = ch;
                }
            }

            if (opcode)
            {
                int n = searchNode(activeNode, ch);
                if (n == -1)
                {
                    opcode = false;
                    activeNode = ccTable;
                    prevNode = null;
                    buffer.append((char) ch);
                    continue;
                }
                else
                {
                    prevNode = activeNode;
                    activeNode = (CCNode) activeNode.nodes.get(n);
                }

                if (activeNode.type == TYPE_ARGLIST)
                {
                    arglevel = activeNode.num_arg - 1;
                    //if arg multipler, multiply arguments
                    if (activeNode.arg_multiplier && ch < MAX_ARG_MULT)
                    {
                        CCNode t = prevNode;
                        while (!t.isTerminator)
                            t = (CCNode) t.nodes.get(0);
                        arglevel = ((t.num_arg - 1) * ch);
                        //if(ch > 20) System.out.println(ch + " offset: " +
                        // Integer.toHexString(address));
                    }

                    if (headBuff.length() < headlen - 1
                        && (curcode == 0x15 || curcode == 0x16 || curcode == 0x17))
                    {
                        int c = ((curcode - 0x15) * 256) + ch;

                        for (int p = 0; p < comprTable[c].length
                            && hpos < headlen - 1;)
                        {
                            headBuff.append(comprTable[c][p++]);
                        }
                    }

                    buffer.append((char) ch);
                }
                else if (activeNode.type == TYPE_CODE)
                {
                    buffer.append((char) ch);
                }

                //if (activeNode.level == 0 && activeNode.value == 0x02)
                if (activeNode.menu_lower)
                {
                    if (menulevel > 0)
                        menulevel--;
                    else
                        break;
                }
                //                if (activeNode.toString().equalsIgnoreCase("[0a xx xx xx
                // xx]"))
                if (activeNode.endcc && activeNode.cc != null)
                {
                    //                    if (ch == 10)
                    //                    {
                    //                        System.out.println();
                    //                    }
                    endcc = true;
                }
                //                else if (
                //                    activeNode.isTerminator
                //                        && activeNode.toString().equals("[0A XX XX XX XX]"))
                //                {
                //                    System.out.println("0x" + Integer.toHexString(address));
                //                    break;
                //                }

                if (activeNode.isTerminator)
                {
                    opcode = false;
                    //if (activeNode.toString().equals("[19 02]"))
                    if (activeNode.menu_raise)
                        menulevel++;

                    activeNode = ccTable;
                }
            }
            else
            {
                //buffer[pos++] = (char) ((((int) ch) & 0xFF) - asciiOff);
                buffer.append((char) ch);
                if (headBuff.length() < headlen - 1)
                    headBuff.append(crdChr
                        ? EbHackModule.simpCreditsToRegChar((char) (ch & 0xFF))
                        : (char) ((ch & 0xFF) - asciiOff));
            }
        }

        StrInfo s = new StrInfo();
        s.address = address;
        s.str = buffer.toString();
        s.header = headBuff.length() == 0 ? " " : headBuff.toString();

        return s;
    }

    public void writeString(String s, int address)
    {
        rom.write(address, s.toCharArray());
    }

    /**
     * Compresses a parsed String.
     * 
     * @param str a parsed String
     * @return a parsed String with compressable sequences replaced with
     *         compression CCs
     */
    public String compressStringNoCC(String str, int c)
    {
        if (!allowComp)
            return str;
        //System.out.println("Compressing \""+str+"\"!");

        //        for (int i = 0; i < comprTable.length; i++)
        //        {
        //        String pattern =
        //            new String(comprTable[c]).replaceAll(
        //                "(\\(|\\)|\\.|\\\\|\\{|\\}|\\?)",
        //                "\\\\$1");
        //        String replaceCC =
        //            "["
        //                + (15 + (c >> 8))
        //                + " "
        //                + HackModule.addZeros(Integer.toHexString(c & 0xff), 2)
        //                + "]";
        //        //String replace = "" + (char) (c << 8);
        String newStr = str;
        //        try
        //        {
        //            // System.out.println(
        //            // "s/" + pattern + "/" + replace + "/g");
        //            newStr = str.replaceAll(pattern, replaceCC);
        //        }
        //        catch (PatternSyntaxException e)
        //        {
        //            System.out.println(
        //                "Pattern errror at " + c + " (" + new String(pattern) + ")");
        //        }
        newStr = comprPatTable[c].compress(str);
        return (c < comprPatTable.length - 1 ? (newStr.equals(str)
            ? compressStringNoCC(newStr, c + 1)
            : compressString(newStr, c + 1)) : newStr);
        //        }
        //        for (int i = 0; i < comprTable.length; i++)
        //        {
        //            str =
        //                str.replaceAll(
        //                    (((char) (i << 8)) + "").replaceAll(
        //                        "(\\(|\\)|\\.|\\\\|\\{|\\}|\\?)",
        //                        "\\\\$1"),
        //                    "["
        //                        + (15 + (i >> 8))
        //                        + " "
        //                        + HackModule.addZeros(Integer.toHexString(i & 0xff), 2)
        //                        + "]");
        //        }
        //        return (str);
    }

    public String compressString(String str, int c)
    {
        if (!allowComp)
            return str;

        String out = new String();
        boolean CC = str.startsWith("[");
        int menuLevel = 0;

        StringTokenizer st = new StringTokenizer(str, "[]", true);
        while (st.hasMoreTokens())
        {
            String tmp = st.nextToken();

            //length of 2 or less can never be compressed
            if (tmp.length() <= 2)
            {
                if (tmp.equals("["))
                    CC = true;
                else if (tmp.equals("]"))
                    CC = false;
                else if (CC && tmp.equals("02"))
                    menuLevel--;
                out += tmp;
                //System.out.println(tmp + " (" + CC +")");
            }
            else
            {
                if (CC && (tmp.startsWith("02") || tmp.startsWith("2")))
                    menuLevel--;
                if (CC && (tmp.endsWith("19 02") || tmp.endsWith("19 2")))
                    menuLevel++;
                out += (CC || menuLevel != 0 ? tmp : compressStringNoCC(tmp, c));
                //System.out.println(tmp + " (" + CC +")");
            }
        }

        return out;
    }

    public String compressString(String str)
    {
        if (allowComp)
            return compressString(str, 0);
        else
            return str;
    }

    public String parseString(String str, boolean showCC, boolean codesOnly)
    {
        return parseString(str, showCC, codesOnly, false);
    }

    public String parseString(String str, boolean showCC, boolean codesOnly,
        boolean codesCaps)
    {
        //char[] buffer = new char[81920];
        //aribitary length guess
        StringBuffer buffer = new StringBuffer(str.length() * 2);

        //int pos = 0; // position in returned string
        int ipos = 0;

        CCNode activeNode = ccTable, prevNode = null;

        boolean opcode = false, endcc = false;
        int arglevel = 0;
        int menulevel = 0;

        int curcode = 0; // initial byte of current control code TODO init to ?

        int ch;

        for (int i = 0; i < str.length(); i++)
        {
            ch = str.charAt(ipos++) & 0xFF;
            //len++;

            // if there are arguments left to count, decrement arg level and
            // continue
            if (arglevel > 0)
            {
                if (showCC)
                    addCode(buffer, ch, codesCaps);

                arglevel--;
                if (arglevel == 0)
                    buffer.append('\u1234');
                continue;
            }
            if (endcc)
                break;

            // if opcode was not previously set
            if (!opcode)
            {
                if (isCC(ch))
                {
                    curcode = ch;
                    opcode = true;
                    if (codesOnly && showCC)
                    {
                        buffer.append('\u1234');
                    }
                }
            }

            // do more stuff

            // if opcode is true, ...

            // note that none of this will happen when there are arguments in
            // the queue,
            // so the active node will still be the original argument node when
            // the arguments
            // finally close, so everything will proceed in an orderly fashion.
            // (an argument node will just run through a few extra bytes and
            // add them to the string,
            // whereas other nodes involve adding only one character to the
            // string)

            if (opcode)
            {

                // search for a subnode matching the current byte
                // (as of initial iteration, activeNode is the root node)
                int n = searchNode(activeNode, ch);
                if (n == -1)
                {
                    // if no match was found, this is an invalid code
                    opcode = false;
                    activeNode = ccTable;
                    prevNode = null;
                }
                else
                {
                    // set the active node to a subnode of itself
                    prevNode = activeNode;
                    activeNode = (CCNode) activeNode.nodes.get(n);
                }

                // okay, now we have a byte from the ROM, and have
                // associated
                // it
                // with a node. depending on the node type, do stuff

                if (activeNode.type == TYPE_ARGLIST)
                {
                    // set arg level to num_arg - 1 because we've already read
                    // one of the argument bytes
                    arglevel = activeNode.num_arg - 1;
                    //                    System.out.println(activeNode.toString() + ": num_arg = "
                    //                        + activeNode.num_arg);
                    //if arg multipler, multiply arguments
                    if (activeNode.arg_multiplier && ch < MAX_ARG_MULT)
                    {
                        CCNode t = prevNode;
                        while (!t.isTerminator)
                            t = (CCNode) t.nodes.get(0);
                        arglevel = ((t.num_arg - 1) * ch);
                        //                        if (!showCC)
                        //                            System.out.println("arg_mult debug: " + "ch = "
                        //                                + ch + ", t = " + t.toString()
                        //                                + ", t.num_arg = " + t.num_arg
                        //                                + ", arglevel = " + arglevel);
                        //                        arglevel = t.num_arg;
                        //                        System.out.println(
                        //                            "parseString(): arglevel = "
                        //                                + arglevel
                        //                                + " ("
                        //                                + t.toString()
                        //                                + "'s num_arg = "
                        //                                + t.num_arg
                        //                                + ", val = "
                        //                                + t.value
                        //                                + ", level = "
                        //                                + t.level
                        //                                + ") (will be mult'd by "
                        //                                + ch
                        //                                + ") at offset "
                        //                                + i);
                        //                        arglevel *= ch;
                        //                        arglevel--;

                    }
                    //                    if (!showCC && curcode == 9)
                    //                        System.out.println("[09] has " + arglevel + " args.");

                    if (curcode == 0x15 || curcode == 0x16 || curcode == 0x17)
                    {
                        if (codesOnly)
                        {
                            if (showCC)
                            {
                                addCode(buffer, curcode, codesCaps);
                                addCode(buffer, ch, codesCaps);
                            }
                        }
                        else
                        {
                            int c = ((curcode - 0x15) * 256) + ch;

                            buffer.append(comprTable[c]);
                        }
                    }
                    else if (showCC)
                    {
                        addCode(buffer, ch, codesCaps);
                    }
                }
                if (activeNode.type == TYPE_CODE)
                {
                    // only add a bracketed code if the current code is not a
                    // compression code
                    if (curcode != 0x15 && curcode != 0x16 && curcode != 0x17
                        && showCC)
                    {
                        addCode(buffer, ch, codesCaps);
                    }
                }

                // if the current node is a first-byte code with value 2,
                // terminate text block
                //if (activeNode.level == 0 && activeNode.value == 0x02)
                if (activeNode.menu_lower)
                {
                    if (menulevel > 0)
                        menulevel--;
                    else
                        break;
                }
                //                if (activeNode.toString().equalsIgnoreCase("[0a xx xx xx
                // xx]"))
                if (activeNode.endcc && activeNode.cc != null)
                {
                    endcc = true;
                }
                //                else if (
                //                    activeNode.isTerminator
                //                        && activeNode.toString().equals("[0A XX XX XX XX]"))
                //                {
                //                    break;
                //                }

                // if the active node is a terminator node, stop stuff
                if (activeNode.isTerminator)
                {
                    //                    System.out.println("End of " + activeNode.toString());
                    if (arglevel == 0)
                        buffer.append('\u1234');
                    opcode = false;
                    //if (activeNode.toString().equals("[19 02]"))
                    if (activeNode.menu_raise)
                        menulevel++;

                    // reset the active node so we start from the
                    // beginning
                    activeNode = ccTable;
                }
            }
            else
            {
                // if the current byte is not a control code,
                // add it to the string as-is
                //buffer[pos++] = (char) ch;
                char cht = (crdChr ? EbHackModule
                    .simpCreditsToRegChar((char) ch) : (char) (ch - asciiOff));
                //make sure brackets never get added as characters
                if (codesOnly && showCC)
                {
                    addCode(buffer, ch, codesCaps);
                }
                else
                {
                    if (cht == '[' || cht == ']')
                    {
                        if (showCC)
                        {
                            addCode(buffer, ch, codesCaps);
                            buffer.append('\u1234');
                        }
                    }
                    else
                    {
                        buffer.append(cht);
                    }
                }
            }
        }

        //return parseGrouping(new String(buffer, 0, pos));
        return parseGrouping(buffer.toString());

        //		char* newstr = new char[pos];
        //		for(i = 0; i < pos; i++)
        //			newstr[i] = buffer[i];
        //
        //		newstr[pos] = 0;
        //
        //		return newstr;
    }

    public String parseString(String str)
    {
        return parseString(str, true, false);
    }

    public String parseString(String str, boolean codesCaps)
    {
        return parseString(str, true, false, codesCaps);
    }

    public String parseCodesOnly(String str, boolean codesCaps)
    {
        return parseString(str, true, true, codesCaps);
        //        String out = new String();
        //        for (int i = 0; i < str.length(); i++)
        //        {
        //            if (i != 0) out += " ";
        //            out += HackModule.addZeros(Integer.toHexString(str.charAt(i)), 2);
        //        }
        //        return "[" + out + "]";
    }

    public String parseCodesOnly(String str)
    {
        return parseString(str, true, true);
    }

    public String parseHeader(String str)
    {
        String newString = parseString(str, false, false).trim();
        //newString = newString.substring(0, Math.min(70,
        // newString.length()));
        if (newString.length() == 0)
            newString = " ";
        return newString;
    }

    public String parseGrouping(String str)
    {
        return str.replaceAll("\\]\\[", " ").replaceAll("\\u1234", "");
    }

    public CCNode[] getCCsUsed(String str)
    {
        int ipos = 0;

        CCNode activeNode = ccTable, prevNode = null;
        SortedSet ccs = new TreeSet();

        boolean opcode = false, endcc = false;
        int arglevel = 0;
        int menulevel = 0;

        int curcode = 0;

        int ch;

        for (int i = 0; i < str.length(); i++)
        {
            ch = str.charAt(ipos++) & 0xFF;
            //len++;

            // if there are arguments left to count, decrement arg level and
            // continue
            if (arglevel > 0)
            {
                arglevel--;
                continue;
            }
            if (endcc)
            {
                //                ccs.add(activeNode);
                //                System.out.println("getCCsUsed(): ending, added "
                //                    + activeNode.toString());
                break;
            }

            // if opcode was not previously set
            if (!opcode)
            {
                if (isCC(ch))
                {
                    curcode = ch;
                    opcode = true;
                }
            }

            // do more stuff

            // if opcode is true, ...

            // note that none of this will happen when there are arguments in
            // the queue,
            // so the active node will still be the original argument node when
            // the arguments
            // finally close, so everything will proceed in an orderly fashion.
            // (an argument node will just run through a few extra bytes and
            // add them to the string,
            // whereas other nodes involve adding only one character to the
            // string)

            if (opcode)
            {

                // search for a subnode matching the current byte
                // (as of initial iteration, activeNode is the root node)
                int n = searchNode(activeNode, ch);
                if (n == -1)
                {
                    // if no match was found, this is an invalid code
                    opcode = false;
                    activeNode = ccTable;
                    prevNode = null;
                }
                else
                {
                    prevNode = activeNode;
                    // set the active node to a subnode of itself
                    activeNode = (CCNode) activeNode.nodes.get(n);
                }

                // okay, now we have a byte from the ROM, and have
                // associated
                // it
                // with a node. depending on the node type, do stuff

                if (activeNode.type == TYPE_ARGLIST)
                {
                    // set arg level to num_arg - 1 because we've already read
                    // one of the argument bytes
                    arglevel = activeNode.num_arg - 1;
                    //if arg multipler, multiply arguments
                    if (activeNode.arg_multiplier && ch < MAX_ARG_MULT)
                    {
                        CCNode t = prevNode;
                        while (!t.isTerminator)
                            t = (CCNode) t.nodes.get(0);
                        arglevel = (t.num_arg * ch) - 1;
                    }
                }
                if (activeNode.type == TYPE_CODE)
                {
                    // only add a bracketed code if the current code is not a
                    // compression code
                }

                // if the current node is a first-byte code with value 2,
                // terminate text block
                //if (activeNode.level == 0 && activeNode.value == 0x02)
                if (activeNode.menu_lower)
                {
                    if (menulevel > 0)
                        menulevel--;
                    else
                    {
                        endcc = true;
                        ccs.add(activeNode);
                    }
                }
                //                if (activeNode.toString().equalsIgnoreCase("[0a xx xx xx
                // xx]"))
                if (activeNode.endcc)
                {
                    endcc = true;
                    if (activeNode.cc == null && activeNode.nodes.size() > 0)
                        ccs.add(activeNode.nodes.get(0));
                }
                //                else if (
                //                    activeNode.isTerminator
                //                        && activeNode.toString().equals("[0A XX XX XX XX]"))
                //                {
                //                    ccs.add(activeNode);
                //                    break;
                //                }

                // if the active node is a terminator node, stop stuff
                if (activeNode.isTerminator)
                {
                    //don't report compression codes
                    if (!(activeNode.toString().startsWith("[15")
                        || activeNode.toString().startsWith("[16") || activeNode
                        .toString().startsWith("[17")))
                        ccs.add(activeNode);
                    //if (activeNode.toString().equals("[19 02]"))
                    if (activeNode.menu_raise)
                        menulevel++;

                    opcode = false;

                    // reset the active node so we start from the
                    // beginning
                    activeNode = ccTable;
                }
            }
        }
        return (CCNode[]) ccs.toArray(new CCNode[0]);
    }

    /**
     * Gets the length of a string, using compression if it is available and
     * requested.
     * 
     * @param str String to get the length of
     * @param comp If true, compression is used if it is available
     * @return Length of string <code>str</code> or -1 on error
     */
    public int getStringLength(String str, boolean comp)
    {
        // This is similar to DeparseString (below);
        // but this function does not deparse the string, it just counts
        // the number of characters.
        // Naturally, the value returned represents the string's un-
        // compressed length.
        // (xlen is the length of the code-formatted string)
        // (Returns -1 if a bracket error is encountered)

        if (allowComp && comp)
            str = compressString(str);

        int len = 0;
        int bracketlevel = 0;
        int ch;

        int xlen = str.length();

        for (int i = 0; i < xlen; i++)
        {
            //            if(str.charAt(i) == '\0')
            //            {
            //                CString x;
            //                x.Format("%d", i);
            //                AfxMessageBox(x);
            //                break;
            //            }

            if (str.charAt(i) == '[')
            {
                bracketlevel++;
                continue;
            }

            if (str.charAt(i) == ']')
            {
                bracketlevel--;
                continue;
            }

            if (bracketlevel < 0)
            {
                return -1;
            }

            if (bracketlevel > 0)
            {
                int j = 0;

                do
                {
                    try
                    {
                        ch = str.charAt(i++);
                    }
                    catch (StringIndexOutOfBoundsException se)
                    {
                        //return -1 on an error
                        return -1;
                    }

                    if (ch == ']')
                    {
                        len++;
                        j = 0;
                        i -= 2;
                        break;
                    }

                    if (ch == ' ' || ch == ']')
                    {
                        if (j != 0)
                        {
                            len++;
                            j = 0;
                        }

                    }
                    else
                        j++;

                    if (j == 32)
                        break;

                }
                while (ch != ']');
            }
            else
            {
                len++;
            }
        }

        if (bracketlevel != 0)
        {
            return -1;
        }
        return len;
    }

    /**
     * Gets the length of a string, using compression if it is available.
     * 
     * @param str String to get the length of
     * @return Length of string <code>str</code> or -1 on error
     */
    public int getStringLength(String str)
    {
        return getStringLength(str, this.allowComp);
    }

    /**
     * Gets the length of a substring. Uses
     * <code>String.substring(start,end)</code>.
     * 
     * @param str String to get length of part of
     * @param comp If true, returns the length of the string compressed
     * @param start start offset as used by <code>String.substring()</code>
     * @param end end offset as used by <code>String.substring()</code>
     * @return Length of string between <code>start</code> and
     *         <code>end</code> or -1 on error
     * @see #getStringLength(String, boolean)
     */
    public int getStringLength(String str, boolean comp, int start, int end)
        throws StringIndexOutOfBoundsException
    {
        //only try to get length of strings with an equal number of brackets
        if (str.split("\\[", -1).length - 1 == str.split("\\]", -1).length - 1) //num
        // [ ==
        // num
        // ]
        {
            //part is the substring we are getting the length of
            String part = str.substring(start, end);
            //make sure brackets match
            //if the last '[' comes after the last ']' then add a ']' at the
            // end
            if (part.lastIndexOf('[') > part.lastIndexOf(']'))
                part = part + ']';
            //if the first ']' comes before the first '[' then add a '[' at the
            // start
            if (part.indexOf(']') < part.indexOf('['))
                part = '[' + part;
            //get the length of the partial string
            //System.out.println("Finding partial length of \"" + part + "\"");
            return getStringLength(part, comp);
        }
        //if there's an unequal number of ['s and ]'s then return -1 to
        // indicate failure
        else
            return -1;
    }

    public int getStringIndex(String str, boolean comp, int offset)
    {
        for (int i = 0; i < str.length(); i++)
        {
            if (getStringLength(str, comp, 0, i) == offset)
                return i;
        }
        return -1;
    }

    public String deparseString(String str)
    {
        // This function takes a visually formatted string (like the user
        // input into the text editing box) and converts it into a string
        // that is ready to be written directly to the ROM.

        /* Convert all control characters to spaces. */
        for (char i = 0; i < 0x20; i++)
        {
            str = str.replace(i, ' ');
        }

        StringBuffer buffer = new StringBuffer(str.length());

        int bracketlevel = 0;
        int ch;
        int len = str.length();

        for (int i = 0; i < len; i++)
        {
            if (str.charAt(i) == '[')
            {
                bracketlevel++;
                continue;
            }

            if (str.charAt(i) == ']')
            {
                bracketlevel--;
                continue;
            }

            if (bracketlevel < 0)
            {
                System.out.println("WARNING: Bracket error detected: " + str);
                return null;
            }

            // if we're still inside brackets (if bracketlevel > 0),
            // get everything up to and including the next space, or
            // up to the next closing bracket.

            if (bracketlevel > 0)
            {
                char[] t = new char[32];
                int j = 0;

                do
                {
                    try
                    {
                        ch = str.charAt(i++);
                    }
                    catch (StringIndexOutOfBoundsException e)
                    {
                        System.out.println("WARNING: Bracket error detected: "
                            + str);
                        return null;
                    }

                    try
                    {
                        if (ch == ']')
                        {
                            t[j] = 0;

                            int a = Integer.parseInt(new String(t).trim(), 16) & 0xFF;
                            buffer.append((char) a);

                            j = 0;
                            i -= 2;
                            break;
                        }

                        if (ch == ' ' || ch == ']')
                        {
                            if (j != 0)
                            {
                                t[j] = 0;

                                int a = Integer.parseInt(new String(t).trim(),
                                    16) & 0xFF;
                                buffer.append((char) a);

                                j = 0;
                            }

                        }
                        else
                            t[j++] = (char) ch;
                    }
                    catch (NumberFormatException e)
                    {
                        System.out
                            .println("WARNING: Invalid data between brackets: "
                                + str);
                        return null;
                    }

                    if (j == 32)
                        break;

                }
                while (ch != ']');
            }

            else
                buffer.append(crdChr ? EbHackModule.simpRegToCreditsChar(str
                    .charAt(i)) : (char) (str.charAt(i) + asciiOff));

        }

        if (bracketlevel != 0)
        {
            System.out.println("WARNING: Bracket error detected: " + str);
            return null;
        }

        //buffer[pos++] = 0;

        //return new String(buffer, 0, pos);
        return buffer.toString();

        //		char* newstr = new char[pos];
        //		for(i = 0; i < pos; i++)
        //			newstr[i] = buffer[i];
        //
        //		newstr[pos] = 0;
        //
        //		return newstr;
    }

    public int searchNode(CCNode node, int val)
    {
        // note: if there's only one subnode of the specified node,
        // and it's an argument list, return its index regardless of
        // the specified value.

        if (node.nodes.size() == 1
            && ((CCNode) node.nodes.get(0)).type == TYPE_ARGLIST)
        {
            return 0;
        }

        for (int i = 0; i < node.nodes.size(); i++)
        {
            if (((CCNode) node.nodes.get(i)).value == val)
                return i;
        }

        return -1;
    }

    public boolean isCC(int ch)
    {
        return searchNode(ccTable, ch) != -1; //-1 = no find
        //return ch >= 0 && ch <= 0x1F;
    }

    public void addCode(char[] str, int val, int pos, boolean codesCaps)
    {
        String tmpstr = ("["
            + HackModule.addZeros(Integer.toHexString(val & 0xff), 2) + "]");
        if (codesCaps)
            tmpstr = tmpstr.toUpperCase();
        char[] tmp = tmpstr.toCharArray();

        str[pos++] = tmp[0];
        str[pos++] = tmp[1];
        str[pos++] = tmp[2];
        str[pos++] = tmp[3];
    }

    public void addCode(StringBuffer str, int val, boolean codesCaps)
    {
        String tmp = "["
            + HackModule.addZeros(Integer.toHexString(val & 0xff), 2) + "]";
        if (codesCaps)
            str.append(tmp.toUpperCase());
        else
            str.append(tmp);
    }

    protected void createCompressionTable(char tbl[][])
    {
        List patterns = new ArrayList();

        int[] buffer = new int[256];
        int ch;
        int pos;

        for (int i = 0; i < 768; i++)
        {
            pos = 0;

            int adr = HackModule.toRegPointer(rom.readMulti(0x8CFED + (i * 4),
                3));

            for (int j = 0; j < 256; j++)
            {
                ch = rom.read(adr++);
                buffer[pos++] = ch;
                if (ch == 0)
                    break;
            }

            tbl[i] = new char[pos - 1];

            for (int j = 0; j < pos - 1; j++)
                tbl[i][j] = (char) (buffer[j] == 0 ? 0 : buffer[j] - 0x30);
            patterns.add(new CompressionPattern(i));
        }
        comprPatTable = (CompressionPattern[]) patterns
            .toArray(new CompressionPattern[0]);
        Arrays.sort(comprPatTable);
        //System.out.println("Created compression table!");
    }

    protected void createCCTable(CCNode table, String filename)
    {
        int[] nodestr;
        String descstr;

        try
        {
            String[] file = new CommentedLineNumberReader(
                new InputStreamReader(ClassLoader
                    .getSystemResourceAsStream(filename))).readLines();

            /*
             * if(file) { fseek(file, 0, SEEK_SET); while(!feof(file))
             */
            for (int f = 0; f < file.length; f++)
            {
                boolean argMulti = false; //is CC arg multipler
                boolean endcc = false; //if true this CC ends a text block
                // ('/')
                boolean menu_raise = false; //if true, increases menu level
                // ('{')
                boolean menu_lower = false; //if true, decreases menu level
                // ('}')

                String[] tmp = file[f].split(",", 2);
                descstr = new StringTokenizer(tmp[1], "\"", false).nextToken();

                if (tmp[0].startsWith("!")) //a ! means arg multipler
                {
                    argMulti = true;
                    tmp[0] = tmp[0].substring(1);
                }
                if (tmp[0].startsWith("/")) //a / means endcc
                {
                    endcc = true;
                    tmp[0] = tmp[0].substring(1);
                }
                if (tmp[0].startsWith("{")) //a { means menu raiser
                {
                    menu_raise = true;
                    tmp[0] = tmp[0].substring(1);
                }
                if (tmp[0].startsWith("}")) //a } means menu lowerer
                {
                    menu_lower = true;
                    tmp[0] = tmp[0].substring(1);
                }
                // now convert the string into a series of single bytes
                String[] nodestrtmp = tmp[0].split(" ");
                nodestr = new int[nodestrtmp.length];
                for (int i = 0; i < nodestrtmp.length; i++)
                {
                    char fc = nodestrtmp[i].charAt(0); //first char
                    //check if first and second are both equal and not hex
                    // digits
                    //isDigit() checks if it's a number, isLetter() checks
                    //if it is a letter and then checks if it's a letter A-F
                    if (fc == nodestrtmp[i].charAt(1)
                        && !Character.isDigit(fc)
                        && !(Character.isLetter(fc) && Character
                            .toUpperCase(fc) <= 'F'))
                        nodestr[i] = nodestrtmp[i].charAt(0) * -1;
                    else
                        nodestr[i] = (int) Integer.parseInt(nodestrtmp[i], 16);
                }
                //				nodestr[pos] = 0;
                //				len = pos;
                //				pos = 0;

                //printf("%s\n", nodestr);

                // now loop through the code string and add a series of nodes

                int prev = 0;
                boolean arg_counting = false;
                int arg_count = 0;
                int[] args = new int[32];

                CCNode activeNode = table;
                int nodeLevel = 0;
                int i;

                for (i = 0; i < nodestr.length; i++)
                {
                    //printf("%X: ", nodestr[i]);
                    // if we encounter an argument value (0xAA, 0xBB, etc.),
                    // start counting arguments

                    // stop counting when the last code no is no longer equal
                    // to the current
                    if (arg_counting)
                    {
                        if (nodestr[i] >= 0 && prev != nodestr[i])
                        {
                            //printf("STOP! %d arguments!\n", arg_count);

                            // now add latent node
                            activeNode = addNode(activeNode, nodeLevel,
                                nodestr[i - 1], TYPE_ARGLIST, arg_count,
                                argMulti, endcc, menu_lower, menu_raise, false);
                            nodeLevel++;
                            arg_count = 0;
                            arg_counting = false;
                        }
                    }
                    //                    if ((nodestr[i] - 0xAA) % 0x11 == 0
                    //                        && nodestr[i] != 0
                    if (nodestr[i] < 0 && !arg_counting)
                    {
                        //System.out.println("COUNTING ARGUMENTS!\n");
                        arg_counting = true;
                    }

                    if (arg_counting)
                    {
                        // if we're still counting arguments, increment the
                        // argument count
                        // and add the current value to the argument list
                        args[arg_count] = nodestr[i];
                        arg_count++;
                    }
                    else
                    {
                        // add default code node here
                        boolean term = (i + 1 == nodestr.length);
                        activeNode = addNode(activeNode, nodeLevel, nodestr[i],
                            TYPE_CODE, 0, argMulti, endcc, menu_lower,
                            menu_raise, term, (term ? tmp[0] : null), (term
                                ? descstr
                                : null));
                        nodeLevel++;
                        //printf("\n");
                    }

                    prev = nodestr[i];
                }
                if (arg_counting)
                {
                    // if we're still counting arguments, tidy up

                    //printf("STOP! %d arguments!\n", arg_count);

                    addNode(activeNode, nodeLevel, nodestr[i - 1],
                        TYPE_ARGLIST, arg_count, argMulti, endcc, menu_lower,
                        menu_raise, true, tmp[0], descstr);

                    arg_counting = false;
                    arg_count = 0;
                }

                //printf("\n");
            }
            //System.out.println("Created control node network!");
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public CCNode addNode(CCNode node, int level, int value, int type,
        int num_arg, boolean multi, boolean ecc, boolean ml, boolean mr,
        boolean terminate, String cc, String desc)
    {
        // Adds a subnode with specified properties to the provided node

        // if a node of the specified value already exists, return a pointer to
        // it
        for (int i = 0; i < node.nodes.size(); i++)
        {
            if (((CCNode) node.nodes.get(i)).value == value)
                return ((CCNode) node.nodes.get(i));
        }

        CCNode newnode = new CCNode();
        newnode.cc = cc;
        newnode.desc = desc;
        newnode.level = level;
        newnode.value = value;
        newnode.type = type;
        newnode.num_arg = num_arg;
        newnode.arg_multiplier = multi;
        newnode.endcc = ecc;
        newnode.menu_lower = ml;
        newnode.menu_raise = mr;
        newnode.isTerminator = terminate;
        newnode.parent = node;

        node.nodes.add(newnode);

        // return a pointer to the node we just created
        return newnode;

    }

    public CCNode addNode(CCNode node, int level, int value, int type,
        int num_arg, boolean multi, boolean endcc, boolean menu_lower,
        boolean menu_raise, boolean terminate)
    {
        return addNode(node, level, value, type, num_arg, multi, endcc,
            menu_lower, menu_raise, terminate, null, null);
    }

    /**
     * @return Returns the allowComp.
     */
    public boolean isAllowComp()
    {
        return allowComp;
    }
}