package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * Editor for what exp gives a level-up for each character.
 * 
 * @author AnyoneEB
 * @see #readExp(int, int, AbstractRom)
 * @see #writeExp(int, int, int, AbstractRom)
 */
public class LevelUpEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public LevelUpEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    private JComboBox charSelector, levelSelector;
    private JTextField exp;
    private boolean initing = true;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        // mainWindow.setSize(250, 150);
        mainWindow.setResizable(false);

        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(HackModule.createJMenuItem("Import From .csv", 'i', null,
            "import", this));
        file.add(HackModule.createJMenuItem("Export As .csv", 'e', null,
            "export", this));
        mb.add(file);
        mainWindow.setJMenuBar(mb);

        // make a JComboBox to select entry, and a JTextField to edit it
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

        exp = createSizedJTextField(10, true);
        entry.add(getLabeledComponent("Experience points:", exp));

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.3";
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
     * Reads from the ROM the experience needed for <code>chr</code> to get to
     * level <code>lvl</code>.
     * 
     * @param chr Character to get exp needed of. 0 = Ness, 1 = Paula, 2 = Jeff,
     *            3 = Poo
     * @param lvl Level to get exp needed for. (2-99)
     * @return Exp needed by character <code>chr</code> to get to level
     *         <code>lvl</code>.
     */
    public static int readExp(int chr, int lvl, AbstractRom rom)
    {
        // each entry 4 bytes, 99 entries/character
        return rom.readMulti(0x159151 + (((lvl + (chr * 100)) - 2) * 4), 4);
    }

    /**
     * Writes to the ROM a new experience needed value. Takes <code>exp</code>
     * as the experience needed for <code>chr</code> to get to level
     * <code>lvl</code>.
     * 
     * @param chr Character to set exp needed of. 0 = Ness, 1 = Paula, 2 = Jeff,
     *            3 = Poo
     * @param lvl Level to set exp needed for. (2-99)
     * @param exp Value to set as the new exp needed.
     */
    public static void writeExp(int chr, int lvl, int exp, AbstractRom rom)
    {
        // each entry 4 bytes, 99 entries/character
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
            // level counting starts at 2, index counting starts at 0
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(charSelector.getSelectedIndex(), (levelSelector
                .getSelectedIndex() + 2));
            // level counting starts at 2, index counting starts at 0
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("export"))
        {
            exportExp(rom);
        }
        else if (ae.getActionCommand().equals("import"))
        {
            importExp(rom);
            showInfo(charSelector.getSelectedIndex(), (levelSelector
                .getSelectedIndex() + 2));
        }
    }

    private static final String EXPORT_FIRST_LINE = "Level,Ness,Paula,Jeff,Poo";

    public static void exportExp(AbstractRom rom)
    {
        exportExp(rom, getFile(true, "csv", "Comma Separated Values"));
    }

    public static void exportExp(AbstractRom rom, File f)
    {
        if (f == null)
            return;

        try
        {
            FileWriter out = new FileWriter(f);
            out.write(EXPORT_FIRST_LINE);
            for (int lvl = 2; lvl <= 99; lvl++)
            {
                out.write("\n" + lvl);
                for (int chr = 0; chr < 4; chr++)
                {
                    out.write("," + readExp(chr, lvl, rom));
                }
            }
            out.close();
            System.out.println(".CSV export successful.");
        }
        catch (FileNotFoundException e)
        {
            JOptionPane.showMessageDialog(null,
                "Error: File not saved: File not found.", "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(null,
                "Error: File not saved: Could write file.", "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
    }

    public static void importExp(AbstractRom rom)
    {
        importExp(rom, getFile(false, "csv", "Comma Separated Values"));
    }

    public static void importExp(AbstractRom rom, File f)
    {
        if (f == null)
            return;

        try
        {
            BufferedReader in = new BufferedReader(new FileReader(f));
            if (!in.readLine().equals(EXPORT_FIRST_LINE))
            {
                JOptionPane.showMessageDialog(null,
                    "Experience .csv files must begin with the following line:\n"
                        + EXPORT_FIRST_LINE, "Not Exported File",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            String line;
            while ((line = in.readLine()) != null)
            {
                String[] split = line.split(",");
                int lvl = Integer.parseInt(split[0]);
                for (int chr = 0; chr < 4; chr++)
                {
                    writeExp(chr, lvl, Integer.parseInt(split[chr + 1]), rom);
                }
            }
            in.close();
        }
        catch (FileNotFoundException e)
        {
            JOptionPane.showMessageDialog(null,
                "Error: File not saved: File not found.", "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(null,
                "Error: File not saved: Could write file.", "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
    }
}
