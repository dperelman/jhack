/*
 * Created on Jul 17, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * This class provides API and GUI for editing the average stat increase at
 * level-up for each character.
 * 
 * @author AnyoneEB
 */
public class LevelUpStatsEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public LevelUpStatsEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    /**
     * Array of {@link LevelUpStatsEditor.LevelUpStats}for each of the four
     * characters.
     */
    public static LevelUpStats[] stats = new LevelUpStats[4];
    /**
     * Names of the seven level-up stats in order. Should be: Offense, Defense,
     * Speed, Guts, Vitality, IQ, Luck.
     */
    public static String[] statNames = {"Offense", "Defense", "Speed", "Guts",
        "Vitality", "IQ", "Luck"};

    /**
     * This reads, writes, and modifies the average stat increase per level-up.
     * 
     * @author AnyoneEB
     */
    public static class LevelUpStats
    {
        private Rom rom;
        private int num, offset;
        private int[] rawStats = new int[7];

        /**
         * Creates a new LevelUpStats object and reads the information from the
         * ROM.
         * 
         * @param charNum
         */
        public LevelUpStats(int charNum, Rom rom) {
            this.rom = rom;
            this.num = charNum;
            this.offset = 0x15EC5B + (num * 7);
            this.readInfo();
        }

        /**
         * Returns the specified stat as a raw value. This value divided by
         * 10.2 is the average increase of this stat for each level.
         * 
         * @see LevelUpStatsEditor#statNames
         * @param statNum Which stat to get (0-6).
         * @return Specified stat as raw value (0-255).
         */
        public int getRawStat(int statNum)
        {
            return rawStats[statNum];
        }

        /**
         * Returns the specified stat as a converted value. This value is the
         * average increase of this state for each level. This value multipled
         * by 10.2 is the raw value.
         * 
         * @param statNum Which stat to get (0-6).
         * @return Specified stat as converted value (0.0-25.0).
         */
        public float getConvertedStat(int statNum)
        {
            return (float) (((float) rawStats[statNum]) / 10.2);
        }

        /**
         * Sets the specified stat as a raw value. This value divided by 10.2
         * is the average increase of this stat for each level.
         * 
         * @param statNum Which stat to set (0-6).
         * @param rawValue Raw value to set stat to (0-255).
         */
        public void setRawStat(int statNum, int rawValue)
        {
            this.rawStats[statNum] = rawValue;
        }

        /**
         * Sets the specified stat as a converted value. This value is the
         * average increase of this state for each level. This value multipled
         * by 10.2 is the raw value.
         * 
         * @param statNum Which stat to set (0-6).
         * @param convertedValue Converted value to set stat to (0.0-25.0).
         */
        public void setConvertedStat(int statNum, float convertedValue)
        {
            this.rawStats[statNum] = (int) Math.round(convertedValue * 10.2);
        }

        private void readInfo()
        {
            rawStats = rom.read(offset, 7);
        }

        public void writeInfo()
        {
            rom.write(offset, rawStats);
        }
    }
    private JTextField[] tf = new JTextField[7];
    private JComboBox charSelector, typeSelector;

    private class LevelUpStatsDocumentListener implements DocumentListener
    {
        private int snum;

        public void valueChanged()
        {
            try
            {
                if (typeSelector.getSelectedIndex() == 0)
                {
                    //raw (int)
                    LevelUpStatsEditor.stats[charSelector.getSelectedIndex()]
                        .setRawStat(snum, Integer.parseInt(tf[snum].getText()));
                }
                else
                {
                    //converted (float)
                    LevelUpStatsEditor.stats[charSelector.getSelectedIndex()]
                        .setConvertedStat(snum, Float.parseFloat(tf[snum]
                            .getText()));
                }
            }
            catch (NumberFormatException e)
            {}
        }

        public void insertUpdate(DocumentEvent arg0)
        {
            valueChanged();
        }

        public void removeUpdate(DocumentEvent arg0)
        {
            valueChanged();
        }

        public void changedUpdate(DocumentEvent arg0)
        {
            valueChanged();
        }

        public LevelUpStatsDocumentListener(int i) {
            snum = i;
        }
    }

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(200, 290);
        mainWindow.setResizable(false);

        //make a JComboBox to select entry, and a JTextField to edit it
        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        charSelector = new JComboBox();
        charSelector.addItem("Ness");
        charSelector.addItem("Paula");
        charSelector.addItem("Jeff");
        charSelector.addItem("Poo");
        charSelector.setActionCommand("charSelector");
        charSelector.addActionListener(this);
        entry.add(getLabeledComponent("Character:", charSelector));

        typeSelector = new JComboBox();
        typeSelector.addItem("Raw");
        typeSelector.addItem("Converted");
        typeSelector.setActionCommand("typeSelector");
        typeSelector.addActionListener(this);
        entry.add(getLabeledComponent("Editing Mode:", typeSelector));

        for (int i = 0; i < 7; i++)
        {
            tf[i] = new JTextField(10);
            tf[i].getDocument().addDocumentListener(
                new LevelUpStatsDocumentListener(i));
            entry.add(getLabeledComponent(LevelUpStatsEditor.statNames[i]
                + ": ", tf[i]));
        }

        mainWindow.getContentPane().add(entry);
        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Level-Up Stats Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Table discovered and documented by EBisumaru";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public static void readFromRom(Rom rom)
    {
        for (int i = 0; i < 4; i++)
        {
            LevelUpStatsEditor.stats[i] = new LevelUpStats(i,rom);
        }
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        readFromRom();
        charSelector.setSelectedIndex(0);
        typeSelector.setSelectedIndex(0);
        showInfo();
        mainWindow.setVisible(true);
    }

    private void showInfo(int chr, boolean raw)
    {
        for (int i = 0; i < 7; i++)
        {
            tf[i].setText(raw
                ? Integer.toString(stats[chr].getRawStat(i))
                : Float.toString(stats[chr].getConvertedStat(i)));
        }
    }

    private void showInfo()
    {
        showInfo(charSelector.getSelectedIndex(), typeSelector
            .getSelectedIndex() == 0);
    }

    /** writeInfo() all. */
    private void saveInfo()
    {
        for (int i = 0; i < 4; i++)
        {
            LevelUpStatsEditor.stats[i].writeInfo();
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(charSelector.getActionCommand())
            || ae.getActionCommand().equals(typeSelector.getActionCommand()))
        {
            showInfo();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }
}
