package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Editor for what exp gives a level-up for each character.
 * 
 * @author AnyoneEB
 * @see #readExp(int, int, Rom)
 * @see #writeExp(int, int, int, Rom)
 */
public class LevelUpEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public LevelUpEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JComboBox charSelector, levelSelector;
    private JTextField exp;
    private boolean initing = true;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(250, 150);
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

        levelSelector = new JComboBox();
        for (int i = 2; i < 100; i++)
        {
            levelSelector.addItem(Integer.toString(i));
        }
        levelSelector.setActionCommand("levelSelector");
        levelSelector.addActionListener(this);
        entry.add(getLabeledComponent("Level:", levelSelector));

        exp = createSizedJTextField(10);
        entry.add(getLabeledComponent("Experience points:", exp));

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.2";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Level-Up Experience Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        this.charSelector.setSelectedIndex(0);
        this.initing = false;
        this.levelSelector.setSelectedIndex(0);
        mainWindow.setVisible(true);
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * Reads from the ROM the experience needed for <code>chr</code> to get
     * to level <code>lvl</code>.
     * 
     * @param chr Character to get exp needed of. 0 = Ness, 1 = Paula, 2 =
     *            Jeff, 3 = Poo
     * @param lvl Level to get exp needed for. (2-99)
     * @return Exp needed by character <code>chr</code> to get to level
     *         <code>lvl</code>.
     */
    public static int readExp(int chr, int lvl, Rom rom)
    {
        //each entry 4 bytes, 99 entries/character
        return rom.readMulti(0x159151 + (((lvl + (chr * 100)) - 2) * 4), 4);
    }

    /**
     * Writes to the ROM a new experience needed value. Takes <code>exp</code>
     * as the experience needed for <code>chr</code> to get to level <code>lvl</code>.
     * 
     * @param chr Character to set exp needed of. 0 = Ness, 1 = Paula, 2 =
     *            Jeff, 3 = Poo
     * @param lvl Level to set exp needed for. (2-99)
     * @param exp Value to set as the new exp needed.
     */
    public static void writeExp(int chr, int lvl, int exp, Rom rom)
    {
        //each entry 4 bytes, 99 entries/character
        rom.write(0x159151 + (((lvl + (chr * 100)) - 2) * 4), exp, 4);
    }

    private void saveInfo(int chr, int lvl)
    {
        writeExp(chr, lvl, Integer.parseInt(exp.getText()), rom);
    }

    private void showInfo(int chr, int lvl)
    {
        if (!this.initing)
        {
            exp.setText(Integer.toString(readExp(chr, lvl, rom)));
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(charSelector.getActionCommand())
            || ae.getActionCommand().equals(levelSelector.getActionCommand()))
        {
            showInfo(charSelector.getSelectedIndex(), (levelSelector
                .getSelectedIndex() + 2));
            //level counting starts at 2, index counting starts at 0
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(charSelector.getSelectedIndex(), (levelSelector
                .getSelectedIndex() + 2));
            //level counting starts at 2, index counting starts at 0
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

}
