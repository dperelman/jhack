package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.MaxLengthDocument;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits the starting stats of the chosen four. These stats include exp, level,
 * items, and money. Requires <code>ItemEditor</code> because it shows the
 * item names.
 * 
 * @author AnyoneEB
 * @see ItemEditor
 */
public class StartingStatsEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public StartingStatsEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JComboBox selector;
    private JComboBox[] items;
    private JTextField[] unknown;
    private JTextField money, level, exp;
    /**
     * Array of starting stats entries. There are four of them, one for each
     * character.
     * 
     * @see StartingStatsEditor.StartingStatsEntry
     */
    public static StartingStatsEntry[] stats = new StartingStatsEntry[4];

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(300, 500);
        mainWindow.setResizable(false);

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        selector = new JComboBox();
        selector.setActionCommand("startingStatsSelector");
        selector.addActionListener(this);
        entry.add(getLabeledComponent("Entry:", selector));

        unknown = new JTextField[2]; //assuming 2 two-byte values
        for (int i = 0; i < unknown.length; i++)
        {
            unknown[i] = new JTextField(5);
            unknown[i].setDocument(new MaxLengthDocument(5));
            entry.add(getLabeledComponent("Unknown " + (i + 1) + ":",
                unknown[i]));
        }

        money = new JTextField(5);
        money.setDocument(new MaxLengthDocument(5));
        entry.add(getLabeledComponent("Money:", money));

        level = new JTextField(5);
        level.setDocument(new MaxLengthDocument(5));
        entry.add(getLabeledComponent("Level:", level));

        exp = new JTextField(5);
        exp.setDocument(new MaxLengthDocument(5));
        entry.add(getLabeledComponent("Experience Points:", exp));

        items = new JComboBox[10];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = ItemEditor.createItemComboBox(this, this);
            entry.add(getLabeledComponent("Item " + (i + 1) + ":", items[i]));
        }

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.1";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Starting Stats Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Table format from Michael1";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        readFromRom();
        this.initSelector();
        mainWindow.pack();

        mainWindow.setVisible(true);
    }

    /**
     * Reads information from ROM into {@link #stats}.
     */
    public static void readFromRom(Rom rom)
    {
        for (int i = 0; i < stats.length; i++)
        {
            stats[i] = new StartingStatsEntry(i, rom);
        }
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }

    private void initSelector()
    {
        selector.removeAllItems();
        for (int i = 0; i < stats.length; i++)
        {
            selector.addItem(stats[i]);
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void saveInfo(int i)
    {
        if (i < 0) return;
        for (int j = 0; j < unknown.length; j++)
        {
            stats[i].setUnknownAsTwoByte(j, Integer.parseInt(unknown[j]
                .getText()));
        }
        stats[i].setMoney(Integer.parseInt(money.getText()));
        stats[i].setLevel(Integer.parseInt(level.getText()));
        stats[i].setExp(Integer.parseInt(exp.getText()));
        for (int j = 0; j < items.length; j++)
        {
            stats[i].setItems(j, items[j].getSelectedIndex());
        }

        stats[i].writeInfo();
    }

    private void showInfo(int i)
    {
        if (i < 0) return;
        for (int j = 0; j < unknown.length; j++)
        {
            unknown[j].setText(Integer
                .toString(stats[i].getUnknownAsTwoByte(j)));
        }
        money.setText(Integer.toString(stats[i].getMoney()));
        level.setText(Integer.toString(stats[i].getLevel()));
        exp.setText(Integer.toString(stats[i].getExp()));
        for (int j = 0; j < items.length; j++)
        {
            items[j].setSelectedIndex(stats[i].getItems(j));
        }

        mainWindow.repaint();
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    /**
     * Represents an entry in the starting stats table in Earthbound. <br>
     * <br>
     * <em>Format:</em><br>
     * 15F7F5 to 15F808 = Ness's starting money, level, exp and items <br>
     * 15F809 to 15F81C = Paula's starting money, level, exp and items <br>
     * 15F81D to 15F830 = Jeff's starting money, level, exp and items <br>
     * 15F831 to 15F844 = Poo's starting money, level, exp and items <br>
     * <br>
     * First four bytes = unused/unknown (0 for all but Ness, for Ness =
     * location of his bed) <br>
     * Next 2 bytes = money <br>
     * Next 2 bytes = starting level <br>
     * Next 2 bytes = starting exp <br>
     * Next 10 bytes = starting items
     */
    public static class StartingStatsEntry
    {
        private Rom rom;
        private int[] unknown = new int[4];
        private int money;
        private int level;
        private int exp;
        private int[] items = new int[10];
        private int num;
        private int address;

        /**
         * Reads the specified starting stats entry from the ROM.
         * 
         * @param num Number of the entry to read. (0 = Ness, 1 = Paula, 2 =
         *            Jeff, 3 = Poo)
         */
        public StartingStatsEntry(int num, Rom rom) {
            this.rom = rom;
            this.num = num;
            this.address = 0x15F7F5 + (num * 20);

            rom.seek(this.address);

            unknown = rom.readSeek(unknown.length);
            this.money = rom.readMultiSeek(2);
            this.level = rom.readMultiSeek(2);
            this.exp = rom.readMultiSeek(2);
            items = rom.readSeek(items.length);
        }

        /**
         * Writes the information in this into the ROM.
         */
        public void writeInfo()
        {
            rom.seek(this.address);

            rom.writeSeek(unknown);
            rom.writeSeek(this.money, 2);
            rom.writeSeek(this.level, 2);
            rom.writeSeek(this.exp, 2);
            rom.writeSeek(items);
        }

        /**
         * Returns the exp.
         * 
         * @return int
         */
        public int getExp()
        {
            return exp;
        }

        /**
         * Returns the <code>i</code> 'th item.
         * 
         * @param i Which item to return
         * @return int
         */
        public int getItems(int i)
        {
            return items[i];
        }

        /**
         * Returns the level.
         * 
         * @return int
         */
        public int getLevel()
        {
            return level;
        }

        /**
         * Returns the money.
         * 
         * @return int
         */
        public int getMoney()
        {
            return money;
        }

        /**
         * Returns byte #<code>i</code> of unknown.
         * 
         * @param i Byte number of unknown to return (0-3)
         * @return int
         */
        public int getUnknown(int i)
        {
            return unknown[i];
        }

        /**
         * Returns byte pair #<code>i</code> of unknown. Switches the byte
         * order, as normal when reading/writing multi-byte values.
         * 
         * @param i Byte pair number of unknown to return (0-1)
         * @return int
         */
        public int getUnknownAsTwoByte(int i)
        //reads unknown as if it were two two-byte values
        {
            return (unknown[i * 2] & 255) + ((unknown[(i * 2) + 1] << 8) & 255);
        }

        /**
         * Sets the exp.
         * 
         * @param exp The exp to set
         */
        public void setExp(int exp)
        {
            this.exp = exp;
        }

        /**
         * Sets the <code>i</code> 'th item.
         * 
         * @param i Which item to set
         * @param item The item to set
         */
        public void setItems(int i, int item)
        {
            this.items[i] = item;
        }

        /**
         * Sets the level.
         * 
         * @param level The level to set
         */
        public void setLevel(int level)
        {
            this.level = level;
        }

        /**
         * Sets the money.
         * 
         * @param money The money to set
         */
        public void setMoney(int money)
        {
            this.money = money;
        }

        /**
         * Sets byte #<code>i</code> of unknown.
         * 
         * @param i Byte number of unknown to return (0-3)
         * @param unknown Byte to set
         */
        public void setUnknown(int i, int unknown)
        {
            this.unknown[i] = unknown;
        }

        /**
         * Sets byte pair #<code>i</code> of unknown. Switches the byte
         * order, as normal when reading/writing multi-byte values.
         * 
         * @param i Byte pair number of unknown to return (0-1)
         * @param unknown Byte pair to set
         */
        public void setUnknownAsTwoByte(int i, int unknown)
        //sets unknown as if it were two two-byte values
        {
            this.unknown[i * 2] = (unknown & 255);
            this.unknown[(i * 2) + 1] = ((unknown >> 8) & 255);
        }

        /**
         * Returns a string in the form "[(0-3)] nameOfCharacter". Ex. "[0]
         * Ness"
         * 
         * @return String
         */
        public String toString()
        {
            String name = "Ness";
            switch (num)
            {
                case 1:
                    name = "Paula";
                    break;
                case 2:
                    name = "Jeff";
                    break;
                case 3:
                    name = "Poo";
                    break;
            }
            return getNumberedString(name, num);
        }
    }
}
