/*
 * Created on Apr 24, 2004
 */
package net.starmen.pkhack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class RomTypeFinder
{
    protected static class RomType
    {
        protected ArrayList checks = new ArrayList();
        protected String name;

        protected class RomTypeCheck
        {
            private int offset;
            private byte[] bytes;

            public RomTypeCheck(int offset, byte[] bytes)
            {
                this.offset = offset;
                this.bytes = bytes;
            }

            public RomTypeCheck(String line)
            {
                line = line.trim();
                if (line.startsWith("*")) line = line.substring(1);
                String[] info = line.split(":");
                offset = Integer.parseInt(info[0].trim(), 16);
                String[] data = info[1].trim().split("\\s+");
                bytes = new byte[data.length];
//                System.out.println();
//                System.out.println("info[1] = " + info[1].trim());
                for (int i = 0; i < data.length; i++)
                {
//                    System.out.println("data[" + i + "] = " + data[i]);
                    bytes[i] = (byte) Integer.parseInt(data[i].trim(), 16);
                }
            }

            public boolean check(Rom rom)
            {
                return rom.compare(offset, bytes);
                //                boolean tmp = rom.compare(offset, bytes);
                //                for (int i = 0; i < bytes.length; i++)
                //                    System.out.println("Checking 0x"
                //                        + HackModule.addZeros(Integer.toHexString(offset), 6)
                //                        + "... "
                //                        + HackModule.addZeros(Integer.toHexString(rom
                //                            .read(offset + i)), 2)
                //                        + "... looking for: "
                //                        + HackModule.addZeros(Integer
                //                            .toHexString(bytes[i] & 0xff), 2));
                //                System.out.println("ROM " + (tmp ? "passed" : "failed")
                //                    + " test to check if ROM type is " + getName());
                //                return tmp;
            }
        }

        public RomType(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public void addCheck(RomTypeCheck c)
        {
            checks.add(c);
        }

        public void addCheck(String line)
        {
            addCheck(new RomTypeCheck(line));
        }

        public boolean isRomOfType(Rom rom)
        {
            for (Iterator i = checks.iterator(); i.hasNext();)
                if (!((RomTypeCheck) i.next()).check(rom)) return false;
            return true;
        }
    }

    private static boolean loaded = false;
    protected static ArrayList romTypes = new ArrayList();

    public static void loadRomTypeFile()
    {
        //if already done this, don't do it again
        if (loaded) return;

        RomType crt = null; //current RomType
        String[] rtlist; //rom type list
        try
        {
            rtlist = new CommentedLineNumberReader(
                new InputStreamReader(
                    ClassLoader
                        .getSystemResourceAsStream("net/starmen/pkhack/romtypes.txt")))
                .readUsedLines();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            //default list marks all ROMs as the ROM type "Unknown"
            rtlist = new String[]{"Unknown"};
            JOptionPane.showMessageDialog(null,
                "Error loading ROM type identification file"
                    + "(romtype.txt). This file is required to reconize"
                    + "previously unloaded ROMs, but the ROM type can "
                    + "still be entered manually.",
                "Error reading romtype.txt!", JOptionPane.ERROR_MESSAGE);
        }
        for (int i = 0; i < rtlist.length; i++)
        {
            String cline = rtlist[i].trim(); //current line
            if (cline.startsWith("*"))
            {
                //if it starts with a *, it's a check
                //make sure crt has been set to something
                //and then add the check to the current RomType
                if (crt != null)
                    crt.addCheck(cline);
                else
                    JOptionPane.showMessageDialog(null,
                        "romtype.txt has a check (line with a * at its "
                            + "beginning) before the first ROM type name."
                            + "This line will be ignored.",
                        "Warning: Invalid romtype.txt",
                        JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                //if it does not start with a *, it's a ROM type name
                //set the current RomType to that name
                crt = new RomType(cline);
                //and add that RomType to the list of all RomType's
                romTypes.add(crt);
            }
        }

        //mark that ROM type info has been loaded
        loaded = true;
    }

    public static String getRomType(Rom rom)
    {
        loadRomTypeFile();

        RomType rt;
        for (Iterator i = romTypes.iterator(); i.hasNext();)
            if ((rt = (RomType) i.next()).isRomOfType(rom))
                return rt.getName();
        System.err.println("No ROM type matches. "
            + "Setting type as unknown. "
            + "This error message cannot be displayed.");
        return "Unknown";
    }

    public static String[] getRomTypeNames()
    {
        String[] out = new String[romTypes.size()];
        int j = 0;
        for (Iterator i = romTypes.iterator(); i.hasNext(); out[j++] = ((RomType) i
            .next()).getName())
            ;
        return out;
    }
}