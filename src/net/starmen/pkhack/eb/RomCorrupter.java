package net.starmen.pkhack.eb;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Messes up the loaded ROM in order to test a .ips maker.
 * Adds one to the first 0x210 bytes of the ROM.
 * 
 * @author AnyoneEB
 */
public class RomCorrupter extends EbHackModule
{
	/**
     * @param rom
     * @param prefs
     */
    public RomCorrupter(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    protected void init(){}
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
		return "ROM Corrupter";
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
		for (int i = 0; i < 0x210; i++)
		{
			rom.write(i, rom.read(i) < 255 ? rom.read(i) + 1 : 0);
		}
		System.out.println(
			"Your ROM is now completely destroyed. " + "Have a nice day!");
	}

	/**
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{}

}
