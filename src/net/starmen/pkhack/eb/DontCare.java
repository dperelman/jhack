package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.MaxLengthDocument;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits "Don't Care" names.
 * 
 * @author AnyoneEB
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class DontCare extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public DontCare(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JTextField[][] textBoxes = new JTextField[7][7];
    private static IPSDatabase.IPSDatabaseEntry sixCharHack = null;

    public static boolean isSixByteNamedHacked(Rom rom)
    {
        if (sixCharHack == null)
        {
            return rom.read(0x2ff788) == 0x06;
        }
        else
        {
            return sixCharHack.isApplied();
        }
    }
    public static int maxLength(int colnum, Rom rom)
    {
        if (colnum < 4)
        {
            return DontCare.isSixByteNamedHacked(rom) ? 6 : 5;
        }
        else
        {
            return 6;
        }
    }

    protected void init()
    {
        IPSDatabase.readXML(rom);
        sixCharHack = IPSDatabase.getPatch("Six Character Name");

        if (JHack.main.getPrefs().getValue("sixCharHack") == null
            && sixCharHack != null
            && !isSixByteNamedHacked(rom))
        {
            Box quesBox = new Box(BoxLayout.Y_AXIS);
            quesBox.add(
                new JLabel("Would you like to apply the six character name hack?"));
            quesBox.add(
                new JLabel("This BETA hack by Mr. Accident allows you to have"));
            quesBox.add(new JLabel("character names six characters long."));
            JCheckBox saveSixCharPref =
                new JCheckBox("Always use this selection.");
            quesBox.add(saveSixCharPref);
            int ques =
                JOptionPane.showConfirmDialog(
                    null,
                    quesBox,
                    "Six character name hack?",
                    JOptionPane.YES_NO_OPTION);
            if (ques == JOptionPane.YES_OPTION)
            {
                sixCharHack.apply(rom);
            }
            if (saveSixCharPref.isSelected())
                JHack.main.getPrefs().setValueAsBoolean(
                    "sixCharHack",
                    ques == JOptionPane.YES_OPTION);
        }
        else if (
            JHack.main.getPrefs().getValueAsBoolean("sixCharHack")
                && !isSixByteNamedHacked(rom))
        {
            sixCharHack.apply(rom);
        }

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(420, 230);
        mainWindow.setResizable(false);

        JPanel entry = new JPanel();
        entry.setLayout(new GridLayout(1, 7));

        JPanel[] tfHolders = new JPanel[7];
        for (int col = 0; col < 7; col++)
        {
            tfHolders[col] = new JPanel();
            tfHolders[col].setLayout(new GridLayout(8, 1));
            tfHolders[col].add(
                new JLabel(
                    (new String[] { "Ness",
                    "Paula",
                    "Jeff",
                    "Poo",
                    "Ness' Dog",
                    "Fav. Food",
                    "Fav. Thing" })[col]));
            //Make title label for each column
            for (int row = 0; row < 7; row++)
            {
                textBoxes[col][row] =
                    HackModule.createSizedJTextField(DontCare.maxLength(col, rom));
                tfHolders[col].add(textBoxes[col][row]);
            }
            entry.add(tfHolders[col]);
        }

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB";
    }
    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "\"Don't Care\" Names Editor";
    }
    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.2";
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        //if sixChar has been applied since this opened, reinit
        if (sixCharHack.isApplied()
            && ((MaxLengthDocument) this.textBoxes[0][0].getDocument())
                .getMaxLength()
                != 6)
        {
            init();
            System.gc();
        }

        this.readInfo();
        mainWindow.setVisible(true);
    }

    private void readInfo() //get info from rom
    {
        for (int col = 0; col < 7; col++)
        {
            for (int row = 0; row < 7; row++)
            {
                this.textBoxes[col][row].setText(
                    new String(
                        simpToRegString(
                            rom.readChar(
                                0x15f6cf + ((row + (col * 7)) * 6),
                                6)))
                        .trim());
                //starts at 0x15f6cf
            }
        }
    }
    private void writeInfo() //write to rom
    {
        int maxLen = maxLength(0, rom); //max length of output
        String value; //temp var
        for (int col = 0; col < 7; col++)
        {
            if (col == 4)
            {
                maxLen = maxLength(col, rom);
            }
            for (int row = 0; row < 7; row++)
            {
                //write the stuff
                value = this.textBoxes[col][row].getText();
                while (value.length() < maxLen)
                    //extend value to max length to make sure it writes over
                    // the old entry
                {
                    value += "\0";
                }
                rom.write(
                    0x15f6cf + ((row + (col * 7)) * 6),
                    simpToGameString(value.toCharArray()));
                //starts at 0x15f6cf
            }
        }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equalsIgnoreCase("apply"))
        {
            this.writeInfo();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("close"))
        {
            this.hide();
        }
    }
}