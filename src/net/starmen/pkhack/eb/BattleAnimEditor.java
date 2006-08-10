package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * Class providing GUI and API for editing the battle animations in Earthbound.
 * 
 * @author EBisumaru
 */

public class BattleAnimEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public BattleAnimEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    private static BA[] bas = new BA[NUM_BA];

    // GUI components
    private JComboBox entrySel, tilesetSel;
    private JTextField frameDurBox, palDurBox, numFramesBox, unknown1Box,
            unknown2Box, unknown3Box, unknown4Box, burnDurBox, burnColorBox,
            titleBox;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        // mainWindow.setSize(400, 250);
        mainWindow.setResizable(true);

        readFromRom();
        // initEffectsList();

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        titleBox = new JTextField(30);
        entry.add(getLabeledComponent("PSI Title", titleBox));

        entry.add(Box.createVerticalStrut(10));

        tilesetSel = new JComboBox();
        tilesetSel.addItem("1");
        tilesetSel.addItem("2");
        tilesetSel.addItem("3");
        tilesetSel.addItem("4");
        entry.add(getLabeledComponent("Tileset:", tilesetSel));

        numFramesBox = new JTextField(3);
        entry.add(getLabeledComponent("Number of frames:", numFramesBox));

        frameDurBox = new JTextField(3);
        entry.add(getLabeledComponent("Frame duration (ticks):", frameDurBox));

        palDurBox = new JTextField(3);
        entry.add(getLabeledComponent("Palette duration:", palDurBox));

        entry.add(Box.createVerticalStrut(5));

        burnColorBox = new JTextField(3);
        entry.add(getLabeledComponent("Enemy burn color:", burnColorBox));

        burnDurBox = new JTextField(3);
        entry.add(getLabeledComponent("Enemy burn duration:", burnDurBox));

        entry.add(Box.createVerticalStrut(5));

        unknown1Box = new JTextField(3);
        entry.add(getLabeledComponent("Unknown 1:", unknown1Box));

        unknown2Box = new JTextField(3);
        entry.add(getLabeledComponent("Unknown 2:", unknown2Box));

        unknown3Box = new JTextField(3);
        entry.add(getLabeledComponent("Unknown 3:", unknown3Box));

        unknown4Box = new JTextField(3);
        entry.add(getLabeledComponent("Unknown 4:", unknown4Box));

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        entrySel.setSelectedIndex(0);

        mainWindow.pack();
    }

    public void show(Object object)
    {
        super.show();
        readFromRom();
        this.entrySel.setSelectedIndex(Integer.parseInt(object.toString()));
        mainWindow.setVisible(true);
    }

    public void show()
    {
        super.show();
        readFromRom();
        showInfo(entrySel.getSelectedIndex());
        mainWindow.setVisible(true);
    }

    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < bas.length; i++)
        {
            bas[i] = new BA(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("PSISel"))
        {
            entrySel.repaint();
            showInfo(entrySel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(entrySel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    public void saveInfo(int i)
    {
        bas[i].setTileset(tilesetSel.getSelectedIndex());
        bas[i].setBurnColor(Integer.parseInt(burnColorBox.getText()));
        bas[i].setBurnDur(Integer.parseInt(burnDurBox.getText()));
        bas[i].setFrameDur(Integer.parseInt(frameDurBox.getText()));
        bas[i].setNumFrames(Integer.parseInt(numFramesBox.getText()));
        bas[i].setPalDur(Integer.parseInt(palDurBox.getText()));
        bas[i].setUnknown1(Integer.parseInt(unknown1Box.getText()));
        bas[i].setUnknown2(Integer.parseInt(unknown2Box.getText()));
        bas[i].setUnknown3(Integer.parseInt(unknown3Box.getText()));
        bas[i].setUnknown4(Integer.parseInt(unknown4Box.getText()));

        bas[i].writeInfo();

        int temp = entrySel.getSelectedIndex();
        entrySel.removeActionListener(this);
        psiNames[entrySel.getSelectedIndex()] = titleBox.getText();
        notifyDataListeners(psiNames, this, entrySel.getSelectedIndex());
        writeArray("battleAnims.txt", false, baNames);
        entrySel.setSelectedIndex(temp);
        entrySel.addActionListener(this);
        entrySel.repaint();
    }

    public void showInfo(int i)
    {
        tilesetSel.setSelectedIndex(bas[i].getTileset());
        frameDurBox.setText(Integer.toString(bas[i].getFrameDur()));
        palDurBox.setText(Integer.toString(bas[i].getPalDur()));
        numFramesBox.setText(Integer.toString(bas[i].getNumFrames()));
        unknown1Box.setText(Integer.toString(bas[i].getUnknown1()));
        unknown2Box.setText(Integer.toString(bas[i].getUnknown2()));
        unknown3Box.setText(Integer.toString(bas[i].getUnknown3()));
        unknown4Box.setText(Integer.toString(bas[i].getUnknown4()));
        burnDurBox.setText(Integer.toString(bas[i].getBurnDur()));
        burnColorBox.setText(Integer.toString(bas[i].getBurnColor()));

        titleBox.setText(baNames[i]);
    }

    /**
     * I dunno...
     */

    public String getDescription()
    {
        return "Battle Animation Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by EBisumaru\n"
            + "PSI Animation stuff discovered by Micheal1 probably\n";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "1.0";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        super.reset();
        readArray("baNames.txt", false, baNames);
    }

    public static class BA
    /**
     * Battle animation table entry class.
     */
    {
        private HackModule hm;
        /**
         * Address of this Battle Animation in the ROM.
         */
        private int address;
        /**
         * Number of this entry in the table
         */
        private int number;
        /**
         * pointer to arrangement (is in pointer table, not like the rest)
         */
        private int pointer;
        /**
         * tileset used by this entry
         */
        private int tileset;
        /**
         * duration of each frame
         */
        private int frameDur;
        /**
         * Duration of palette (??)
         */
        private int palDur;
        /**
         * unknown 1
         */
        private int unknown1;
        /**
         * unknown 2
         */
        private int unknown2;
        /**
         * number of frames
         */
        private int numFrames;
        /**
         * unknown 3
         */
        private int unknown3;
        /**
         * unknown 4
         */
        private int unknown4;
        /**
         * time the enemy's palette is warped in ticks
         */
        private int burnDur;
        /**
         * color the enemy's palette is wapred
         */
        private int burnColor;

        /**
         * Constructor of PSI data entry
         * 
         * @param BANumber Number of this PSI in the table
         * @param hm HackModule to use
         */
        public BA(int BANumber, HackModule hm)
        {
            this.hm = hm;
            AbstractRom rom = hm.rom;
            this.number = BANumber;

            this.address = 0xCF24D + this.number * 12;
            rom.seek(this.address);

            System.out.println(this.address + "\n");

            int tilesetPointer = rom.readMultiSeek(2);
            switch (tilesetPointer)
            {
                case 0xAC25:
                    this.tileset = 0;
                    break;
                case 0xB613:
                    this.tileset = 1;
                    break;
                case 0xDB27:
                    this.tileset = 2;
                    break;
                case 0xE31D:
                default:
                    this.tileset = 3;
                    break;
            }
            this.frameDur = rom.readSeek();
            this.palDur = rom.readSeek();
            this.unknown1 = rom.readSeek();
            this.unknown2 = rom.readSeek();
            this.numFrames = rom.readSeek();
            this.unknown3 = rom.readSeek();
            this.unknown4 = rom.readSeek();
            this.burnDur = rom.readSeek();
            this.burnColor = rom.readSeek();

            rom.seek(0xCF78F + 4 * this.number);
            this.pointer = rom.readMultiSeek(4);
        }

        /**
         * Writes entry's info to the ROM
         * 
         */
        public void writeInfo()
        {
            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            int tilesetPointer;
            switch (this.tileset)
            {
                case 0:
                    tilesetPointer = 0xAC25;
                    break;
                case 1:
                    tilesetPointer = 0xB613;
                    break;
                case 2:
                    tilesetPointer = 0xDB27;
                    break;
                default:
                    tilesetPointer = 0xE31D;
                    break;
            }
            rom.writeSeek(tilesetPointer, 2);
            rom.writeSeek(this.frameDur);
            rom.writeSeek(this.palDur);
            rom.writeSeek(this.unknown1);
            rom.writeSeek(this.unknown2);
            rom.writeSeek(this.numFrames);
            rom.writeSeek(this.unknown3);
            rom.writeSeek(this.unknown4);
            rom.writeSeek(this.burnDur);
            rom.writeSeek(this.burnColor);

            rom.seek(0xCF78F + 4 * this.number);
            rom.writeSeek(this.pointer);
        }

        /**
         * Gets number of this entry in the table
         */
        public int getNumber()
        {
            return this.number;
        }

        /**
         * Gets address of this PSI in the ROM.
         */
        public int getAddress()
        {
            return this.address;
        }

        /**
         * @return Returns the tileset.
         */
        public int getTileset()
        {
            return tileset;
        }

        /**
         * @param tileset Sets tileset.
         */
        public void setTileset(int tileset)
        {
            this.tileset = tileset;
        }

        /**
         * @return Returns the burnColor.
         */
        public int getBurnColor()
        {
            return burnColor;
        }

        /**
         * @param burnColor The burnColor to set.
         */
        public void setBurnColor(int burnColor)
        {
            this.burnColor = burnColor;
        }

        /**
         * @return Returns the burnDur.
         */
        public int getBurnDur()
        {
            return burnDur;
        }

        /**
         * @param burnDur The burnDur to set.
         */
        public void setBurnDur(int burnDur)
        {
            this.burnDur = burnDur;
        }

        /**
         * @return Returns the frameDur.
         */
        public int getFrameDur()
        {
            return frameDur;
        }

        /**
         * @param frameDur The frameDur to set.
         */
        public void setFrameDur(int frameDur)
        {
            this.frameDur = frameDur;
        }

        /**
         * @return Returns the numFrames.
         */
        public int getNumFrames()
        {
            return numFrames;
        }

        /**
         * @param numFrames The numFrames to set.
         */
        public void setNumFrames(int numFrames)
        {
            this.numFrames = numFrames;
        }

        /**
         * @return Returns the palDur.
         */
        public int getPalDur()
        {
            return palDur;
        }

        /**
         * @param palDur The palDur to set.
         */
        public void setPalDur(int palDur)
        {
            this.palDur = palDur;
        }

        /**
         * @return Returns the pointer.
         */
        public int getPointer()
        {
            return pointer;
        }

        /**
         * @param pointer The pointer to set.
         */
        public void setPointer(int pointer)
        {
            this.pointer = pointer;
        }

        /**
         * @return Returns the unknown1.
         */
        public int getUnknown1()
        {
            return unknown1;
        }

        /**
         * @param unknown1 The unknown1 to set.
         */
        public void setUnknown1(int unknown1)
        {
            this.unknown1 = unknown1;
        }

        /**
         * @return Returns the unknown2.
         */
        public int getUnknown2()
        {
            return unknown2;
        }

        /**
         * @param unknown2 The unknown2 to set.
         */
        public void setUnknown2(int unknown2)
        {
            this.unknown2 = unknown2;
        }

        /**
         * @return Returns the unknown3.
         */
        public int getUnknown3()
        {
            return unknown3;
        }

        /**
         * @param unknown3 The unknown3 to set.
         */
        public void setUnknown3(int unknown3)
        {
            this.unknown3 = unknown3;
        }

        /**
         * @return Returns the unknown4.
         */
        public int getUnknown4()
        {
            return unknown4;
        }

        /**
         * @param unknown4 The unknown4 to set.
         */
        public void setUnknown4(int unknown4)
        {
            this.unknown4 = unknown4;
        }
    }
}