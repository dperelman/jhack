package net.starmen.pkhack.eb;

import javax.swing.JOptionPane;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Expands the loaded ROM. This is done by adding 4096 blocks of (255 zero's and
 * then a two). 4096 * 256 bytes = 2^20 bytes or 1 megabyte.
 * 
 * @author AnyoneEB
 */
public class RomExpander extends EbHackModule
{
    /**
     * @param rom
     * @param prefs
     */
    public RomExpander(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    protected void init()
    {}

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
        return "ROM Expander";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Based on source code by DrAndonuts";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        if (rom.length() == 0x600200)
        {
            JOptionPane.showMessageDialog(null,
                "Your ROM is already expanded.", "Error!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (rom.length() == 0x300200)
        {
            Object[] options = new String[]{"Expand to 32 megabits",
                "Expand to 48 megabits", "Cancel"};
            int ans = JOptionPane.showOptionDialog(null,
                "Your ROM is currently 24 megabits (3 megabytes).\n"
                    + "It can be expanded to 32 megabits (4 megabytes) or\n"
                    + "48 megabits (6 megabytes) (ExHiRom).\n\n"
                    + "You cannot undo this. Make sure you have a back-up.\n"
                    + "Are you sure you want to expand your ROM?",
                "Expand to?", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
            if (ans == 0)
                rom.expand();
            else if (ans == 1)
                rom.expandEx();
            else
                return;
        }
        else if (rom.length() == 0x400200)
        {
            if (JOptionPane.showConfirmDialog(null,
                "This will expand your ROM from 32 to 48 megabits (ExHiRom).\n"
                    + "You cannot undo this. Make sure you have a back-up.\n"
                    + "Are you sure you want to expand your ROM?", "Continue?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
            {
                return;
            }
            rom.expandEx();
        }

        JOptionPane.showMessageDialog(null, "Your ROM is now expanded.",
            "Finished", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {}

}