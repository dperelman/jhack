/*
 * Created on Apr 3, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSFile;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.Stopwatch;
import net.starmen.pkhack.XMLPreferences;

/**
 * Resets an area of the ROM.
 * 
 * @author AnyoneEB
 */
/*
 * /----------------------------------------------------\ |--[Load]----
 * D:\daniel\earthbound\pkhack\earth.smc -|
 * |----------------------------------------------------| |--[Section: (combo
 * box)]--[subSection: (combobox)]--|
 * |----------------------------------------------------| |-
 * <==||============================================> -|
 * |----------------------------------------------------| |----- 0x [
 * *textField* ] to 0x [ *textField* ] -----|
 * |----------------------------------------------------| |-
 * <============================================||==> -|
 * |----------------------------------------------------|
 * |-------[Reset]--------[MakeIPS]-------[Close]-------|
 * \----------------------------------------------------/
 */
public class ResetButton extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public ResetButton(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    private JSlider start, end;
    private JLabel path;
    private JTextField startTF, endTF, lenTF;
    private JComboBox rangeSelector, subrangeSelector;
    private boolean isCustom = true; //false = preset range

    private Rom orgRom = null;
    private ArrayList ranges = new ArrayList();

    /**
     * Represents a range in a file. May have more specific sub-ranges.
     * 
     * @author AnyoneEB
     */
    private class ResetRange
    {
        private int start, len;
        private String desc;
        private ResetRange[] subRanges = new ResetRange[0];

        /**
         * Creates a ResetRange.
         * 
         * @param start Offset of the first byte.
         * @param len Length of range
         * @param desc Description of range, may be just a number in a subrange.
         */
        public ResetRange(int start, int len, String desc)
        {
            this.start = start;
            this.len = len;
            this.desc = desc;
        }

        /**
         * Returns the length of this range.
         * 
         * @return int
         */
        public int getLen()
        {
            return len;
        }

        /**
         * Returns the starting byte of this range.
         * 
         * @return The location of the first byte of this range.
         */
        public int getStart()
        {
            return start;
        }

        /**
         * Returns the last byte of this range.
         * 
         * @return The location of the last byte of this range.
         */
        public int getEnd()
        {
            return start + len - 1;
        }

        /**
         * Returns the sub ranges.
         * 
         * @return ResetRange[]
         */
        public ResetRange[] getSubRanges()
        {
            return subRanges;
        }

        /**
         * Adds a ResetRange to the list of sub ranges. Sub ranges should be
         * inside their super range.
         * 
         * @param rr ResetRange to add
         */
        public void addSubRange(ResetRange rr)
        {
            if (rr != this)
            {
                ResetRange[] newSubs = new ResetRange[this.subRanges.length + 1];
                for (int i = 0; i < this.subRanges.length; i++)
                {
                    newSubs[i] = this.subRanges[i];
                }
                newSubs[this.subRanges.length] = rr;
                this.subRanges = newSubs;
            }
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return new String(desc);
        }

        /**
         * Resets the area based on this.
         */
        public void resetArea()
        {
            rom.write(getStart(), orgRom.readByte(getStart(), getLen()));
        }

        /**
         * Makes an IPS based on this and saves it a user-specified location.
         */
        public void makeIPS()
        {
            String ips = rom.createIPS(orgRom, getStart(),
                getStart() + getLen() - 1).toString();

            if (ips.length() <= 8) //"PATCH" and "EOF" total 8 bytes
            {
                JOptionPane.showMessageDialog(null,
                    "Cannot create patch. ROMs are identical.", "Error!",
                    JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new FileFilter()
                {
                    public boolean accept(File f)
                    {
                        if (f.getAbsolutePath().toLowerCase().endsWith(".ips")
                            || f.isDirectory())
                        {
                            return true;
                        }
                        return false;
                    }

                    public String getDescription()
                    {
                        return "IPS Files (*.ips)";
                    }
                });
                if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
                {
                    try
                    {
                        FileOutputStream out = new FileOutputStream(jfc
                            .getSelectedFile());
                        for (int i = 0; i < ips.length(); i++)
                        {
                            out.write(ips.charAt(i));
                        }
                        out.close();
                    }
                    catch (FileNotFoundException e)
                    {
                        System.out
                            .println("Error: File not saved: File not found.");
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        System.out
                            .println("Error: File not saved: Could write file.");
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void initResetRanges()
    {
        try
        {
            String[] ranges = new CommentedLineNumberReader(
                new InputStreamReader(ClassLoader
                    .getSystemResourceAsStream(DEFAULT_BASE_DIR
                        + "resetranges.txt"))).readUsedLines();

            ResetRange lastRange = new ResetRange(0, 0, "");
            for (int i = 0; i < ranges.length; i++)
            {
                String[] split = ranges[i].split("=");
                int st;
                int len;
                boolean isSubrange = false;
                if (split[0].charAt(0) == '*')
                {
                    isSubrange = true;
                    split[0] = split[0].substring(1);
                }

                if (split[0].charAt(0) == '_')
                {
                    //stuff that should already be somewhere else
                    split[0] = split[0].substring(1).trim();
                    if (split[0].equalsIgnoreCase("items"))
                    {
                        ItemEditor.readFromRom(this);
                        for (int j = 0; j < ItemEditor.items.length; j++)
                        {
                            //Item entries are 39 bytes long
                            lastRange.addSubRange(new ResetRange(
                                ItemEditor.items[j].address, 39,
                                ItemEditor.items[j].toString()));
                        }
                    }
                    else if (split[0].equalsIgnoreCase("sprites"))
                    {
                        SpriteEditor.readFromRom(rom);
                        for (int j = 0; j < SpriteEditor.si.length; j++)
                        {
                            lastRange.addSubRange(new ResetRange(
                                SpriteEditor.si[j].address,
                                SpriteEditor.si[j].width
                                    * SpriteEditor.si[j].height * 32,
                                getNumberedString(
                                    SpriteEditor.si[j].toString(), j, false)));
                        }
                    }
                    else if (split[0].equalsIgnoreCase("sprites_grouped"))
                    {
                        SpriteEditor.readFromRom(rom);
                        for (int j = 0; j < SpriteEditor.sib.length; j++)
                        {
                            SpriteEditor.SpriteInfo si;
                            ResetRange temp = null;
                            for (int k = 0; k < SpriteEditor.sib[j].numSprites; k++)
                            {
                                si = SpriteEditor.sib[j].getSpriteInfo(k);
                                try
                                {
                                    temp = new ResetRange(
                                        temp.getStart(),
                                        ((si.address + (si.height * si.width * 32)) - temp
                                            .getStart()), temp.toString());
                                }
                                catch (NullPointerException e1)
                                {
                                    temp = new ResetRange(si.address, si.height
                                        * si.width * 32, getNumberedString(
                                        SpriteEditor.sib[j].toString(), j,
                                        false));
                                }

                            }

                            lastRange.addSubRange(temp);
                        }
                    }
                    else if (split[0].equalsIgnoreCase("spt"))
                    {
                        SpriteEditor.readFromRom(rom);
                        for (int j = 0; j < SpriteEditor.sib.length; j++)
                        {
                            lastRange.addSubRange(new ResetRange(HackModule
                                .toRegPointer(SpriteEditor.sib[j].pointer),
                                SpriteEditor.sib[j].getLength(),
                                getNumberedString(SpriteEditor.sib[j]
                                    .toString(), j, false)));
                        }
                    }
                    else if (split[0].equalsIgnoreCase("enemies"))
                    {
                        EnemyEditor.readFromRom(this);
                        for (int j = 0; j < EnemyEditor.enemies.length; j++)
                        {
                            //Enemy entries are 94 bytes long
                            lastRange.addSubRange(new ResetRange(
                                EnemyEditor.enemies[j].getAddress(), 94,
                                EnemyEditor.enemies[j].toString()));
                        }
                    }
                }
                else
                {
                    if (split[0].indexOf("to") != -1)
                    {
                        String[] sp = split[0].split("to");
                        st = Integer.parseInt(sp[0].trim(), 16);
                        len = (Integer.parseInt(sp[1].trim(), 16) - st) + 1;
                    }
                    else
                    //if (split[0].indexOf("len") != -1)
                    {
                        String[] sp = split[0].split("len");
                        if (sp[0].trim().length() == 0)
                        {
                            if (isSubrange)
                            {
                                try
                                {
                                    st = lastRange.getSubRanges()[lastRange
                                        .getSubRanges().length - 1].getStart()
                                        + lastRange.getSubRanges()[lastRange
                                            .getSubRanges().length - 1]
                                            .getLen();
                                }
                                catch (ArrayIndexOutOfBoundsException e1)
                                {
                                    st = lastRange.getStart();
                                }
                            }
                            else
                            {
                                st = lastRange.getStart() + lastRange.getLen();
                            }
                        }
                        else
                        {
                            st = Integer.parseInt(sp[0].trim(), 16);
                        }
                        len = Integer.parseInt(sp[1].trim(), 16);
                    }

                    if (isSubrange)
                        lastRange.addSubRange(new ResetRange(st, len, split[1]
                            .trim()));
                    else
                        addRange(lastRange = new ResetRange(st, len, split[1]
                            .trim()));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void addRange(ResetRange rr)
    {
        ranges.add(rr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    protected void init()
    {
        initResetRanges();
        mainWindow = new JFrame(this.getDescription());
        mainWindow.setSize(620, 210);

        mainWindow.getContentPane().setLayout(new BorderLayout());

        //start bottom buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());

        JButton reset = new JButton("Reset Area");
        reset.setActionCommand("reset");
        reset.addActionListener(this);
        buttons.add(reset);

        JButton makeips = new JButton("Make Partial IPS");
        makeips.setActionCommand("makeips");
        makeips.addActionListener(this);
        buttons.add(makeips);

        buttons.add(new JSeparator());

        JButton decomp = new JButton("Decomp...");
        decomp.setActionCommand("decomp");
        decomp.addActionListener(this);
        buttons.add(decomp);

        JButton comp = new JButton("Comp...");
        comp.setActionCommand("comp");
        comp.addActionListener(this);
        buttons.add(comp);

        buttons.add(new JSeparator());

        JButton close = new JButton("Close");
        close.setActionCommand("close");
        close.addActionListener(this);
        buttons.add(close);

        mainWindow.getContentPane().add(buttons, BorderLayout.SOUTH);
        //end bottom buttons

        Box entry = new Box(BoxLayout.Y_AXIS);

        entry.add(pairComponents(rangeSelector = HackModule
            .createJComboBoxFromArray(ranges.toArray()),
            subrangeSelector = new JComboBox(), true));
        rangeSelector.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                ResetRange rr = ((ResetRange) ranges.get(rangeSelector
                    .getSelectedIndex()));
                setSelectedRange(rr);

                subrangeSelector.removeAllItems();
                subrangeSelector.addItem("All");
                for (int i = 0; i < rr.getSubRanges().length; i++)
                {
                    subrangeSelector.addItem(rr.getSubRanges()[i]);
                }
                subrangeSelector.setSelectedIndex(0);
            }
        });
        subrangeSelector.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                isCustom = false;
                ResetRange rr;
                try
                {
                    rr = ((ResetRange) ranges.get(rangeSelector
                        .getSelectedIndex())).getSubRanges()[subrangeSelector
                        .getSelectedIndex() - 1];
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    rr = ((ResetRange) ranges.get(rangeSelector
                        .getSelectedIndex()));
                }
                setSelectedRange(rr);
            }
        });

        //slider ChangeListener
        ChangeListener scl = new ChangeListener()
        {
            public void stateChanged(ChangeEvent arg0)
            {
                try
                {
                    startTF.setText(addZeros(Integer.toString(start.getValue(),
                        16), 6));
                    endTF.setText(addZeros(
                        Integer.toString(end.getValue(), 16), 6));
                    lenTF.setText(Integer.toString(end.getValue()
                        - start.getValue() + 1, 16));
                }
                catch (IllegalStateException e)
                {
                    //e.printStackTrace();
                }
            }
        };
        //TextField DocumentListner
        DocumentListener tdl = new DocumentListener()
        {
            public void doStuff()
            {
                isCustom = true;
                try
                {
                    start.setValue(Integer.parseInt(startTF.getText(), 16));
                }
                catch (NumberFormatException e)
                {}
                try
                {
                    end.setValue(Integer.parseInt(endTF.getText(), 16));
                }
                catch (NumberFormatException e)
                {}
                //				lenTF.setText(
                //					Integer.toString(
                //						end.getValue() - start.getValue() + 1,
                //						16));
            }

            public void insertUpdate(DocumentEvent arg0)
            {
                doStuff();
            }

            public void removeUpdate(DocumentEvent arg0)
            {
                doStuff();
            }

            public void changedUpdate(DocumentEvent arg0)
            {
                doStuff();
            }
        };
        entry.add(start = new JSlider(0, rom.length() - 1, 0));
        entry.add(HackModule.createFlowLayout(new JComponent[]{
            startTF = HackModule.createSizedJTextField(6), new JLabel(" to "),
            endTF = HackModule.createSizedJTextField(6),
            new JLabel(" (Length: "),
            lenTF = HackModule.createSizedJTextField(6), new JLabel(")")}));
        lenTF.setEnabled(false);
        entry.add(end = new JSlider(0, rom.length() - 1, rom.length() - 1));

        start.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent arg0)
            {
                isCustom = true;
                try
                {
                    startTF.setText(addZeros(Integer.toString(start.getValue(),
                        16), 6));
                    lenTF.setText(Integer.toString(end.getValue()
                        - start.getValue() + 1, 16));
                }
                catch (IllegalStateException e)
                {
                    //e.printStackTrace();
                }
            }
        });
        end.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent arg0)
            {
                isCustom = false;
                try
                {
                    endTF.setText(addZeros(
                        Integer.toString(end.getValue(), 16), 6));
                    lenTF.setText(Integer.toString(end.getValue()
                        - start.getValue() + 1, 16));
                }
                catch (IllegalStateException e)
                {
                    //e.printStackTrace();
                }
            }
        });
        startTF.getDocument().addDocumentListener(tdl);
        endTF.getDocument().addDocumentListener(tdl);
        lenTF.getDocument().addDocumentListener(tdl);
        scl.stateChanged(new ChangeEvent(this));

        mainWindow.getContentPane().add(
            HackModule.pairComponents(new JLabel(), entry, false),
            BorderLayout.CENTER);

        //load ROM stuff
        orgRom = JHack.main.getOrginalRomFile(rom.getRomType());
        JButton loadb;
        JPanel loadPane = HackModule.pairComponents(
            loadb = new JButton("Load"), this.path = new JLabel(orgRom
                .getPath()), true);
        loadb.addActionListener(this);
        loadb.setActionCommand("load");
        mainWindow.getContentPane().add(loadPane, BorderLayout.NORTH);

        rangeSelector.setSelectedIndex(0);

        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.5";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Reset Button";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Idea based on Dr Andount's Reset Button";
        //			+ "\nRanges from the PK Rom Map";
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void setSelectedRange(ResetRange rr)
    {
        start.setValue(rr.getStart());
        end.setValue(rr.getStart() + rr.getLen() - 1);
    }

    private ResetRange getSelectedRange()
    {
        return new ResetRange(start.getValue(), (end.getValue() - start
            .getValue()) + 1, "");
    }

    private String getRangeName(int offset, String out, ResetRange[] ranges)
    {
        for (int i = 0; i < ranges.length; i++)
        {
            if (offset - ranges[i].getStart() < ranges[i].getLen()
                && offset >= ranges[i].getStart())
            {
                out += ranges[i].toString() + " - ";
                out += getRangeName(offset, out, ranges[i].getSubRanges());
            }
        }
        return out;
    }

    /**
     * Gets the name of a range from an offset in that range.
     * 
     * @param offset Offset in ROM
     * @return Name of range
     */
    public String getRangeName(int offset)
    {
        String out = getRangeName(offset, new String(), (ResetRange[]) ranges
            .toArray(new ResetRange[0]));
        String[] o = out.split(" - ");
        return o[o.length - 1];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("load"))
        {
            //don't overwrite orginal ROM file
            Rom tmpRom = orgRom;
            if (orgRom == JHack.main.getOrginalRomFile(rom.getRomType()))
            {
                orgRom = new Rom();
            }
            //if user cancels load, use previously loaded ROM
            if (orgRom.loadRom())
                path.setText(orgRom.getPath());
            else
                orgRom = tmpRom;
        }
        else if (ae.getActionCommand().equals("reset"))
        {
            if (!orgRom.isLoaded)
            {
                JOptionPane.showMessageDialog(null,
                    "Load an orginal ROM first!", "Load a ROM!",
                    JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                getSelectedRange().resetArea();
            }
        }
        else if (ae.getActionCommand().equals("makeips"))
        {
            if (!orgRom.isLoaded)
            {
                JOptionPane.showMessageDialog(null,
                    "Load an orginal ROM first!", "Load a ROM!",
                    JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                getSelectedRange().makeIPS();
            }
        }
        else if (ae.getActionCommand().equals("decomp"))
        {
            ResetRange rr = this.getSelectedRange();
            byte[] buffer = new byte[1024 * 1024];
            int[] tmp = decomp(rr.getStart(), buffer, buffer.length);
            if (tmp[0] < 0)
                JOptionPane.showMessageDialog(this.mainWindow, "Error #"
                    + tmp[0] + " decompressing data. Data may be invalid\n"
                    + "or buffer may be too small.", "Error: Unable to decomp",
                    JOptionPane.ERROR_MESSAGE);
            else
            {
                JOptionPane.showMessageDialog(this.mainWindow, "Decompressed "
                    + tmp[0] + " bytes.\n" + "Select an output file.",
                    "Decomp successful", JOptionPane.INFORMATION_MESSAGE);
                try
                {
                    FileOutputStream out = new FileOutputStream(getFile(true,
                        "smc", "Decompressed Graphics Dump"));
                    out.write(buffer, 0, Math.max(tmp[0], 8192));
                    out.close();
                }
                catch (NullPointerException e)
                {
                    //User canceled
                }
                catch (FileNotFoundException e)
                {
                    System.out
                        .println("File not found for saving graphics dump... this can't happen!");
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "File system error while saving data.",
                        "Error: Unable to save", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        else if (ae.getActionCommand().equals("comp"))
        {
            try
            {
                File f = getFile(false, "smc", "Decompressed Graphics Dump");
                FileInputStream in = new FileInputStream(f);
                byte[] udata = new byte[(int) f.length()], buffer = new byte[(int) f
                    .length()];
                in.read(udata);
                in.close();

                int tmp = comp(udata, buffer, getArrSize(udata));
                if (tmp > this.getSelectedRange().getLen())
                {
                    //longer than selected area
                    if (JOptionPane.showConfirmDialog(this.mainWindow,
                        "Compressed data is larger than the selected range.\n"
                            + "Do you wish to write past selected range?",
                        "Overwrite?", JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }
                rom.write(this.getSelectedRange().getStart(), buffer, tmp);
                JOptionPane.showMessageDialog(this.mainWindow, "Compressed to "
                    + tmp + " bytes.", "Comp successful",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            catch (NullPointerException e)
            {
                //User canceled
            }
            catch (FileNotFoundException e)
            {
                System.out
                    .println("File not found for reading graphics dump... this can't happen!");
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this.mainWindow,
                    "File system error while reading data.",
                    "Error: Unable to read file", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        mainWindow.setVisible(true);

        //IPSInfo.main(this);
    }

}