/*
 * Created on Apr 24, 2003
 */
package net.starmen.pkhack.eb;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Quick and dirty module to dump basic item info to the console.
 * 
 * @author AnyoneEB
 */
public class ItemDumper extends EbHackModule
{

	/**
     * @param rom
     * @param prefs
     */
    public ItemDumper(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    /* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.1";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription()
	{
		return "Item Dumper";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by AnyoneEB";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#show()
	 */
	public void show()
	{
		super.show();

		ItemEditor.readFromRom(this);
		StoreEditor.readFromRom(rom);
		EnemyEditor.readFromRom(this);

		for (int j = 1; j < ItemEditor.items.length; j++)
		{
			ItemEditor.Item i = ItemEditor.items[j];

			println("#" + i.number + ": " + new String(i.name).trim());
			println("Action: " + effects[i.effect]);
			//need action stuff
			println("Price: $" + i.cost + ".00");

			String storestr = new String();
			boolean first = true;
			for (int s = 0; s < StoreEditor.stores.length; s++)
			{
				for (int si = 0; si < 7; si++)
				{
					if (StoreEditor.stores[s].getItem(si) == i.number)
					{
						storestr += (first ? "" : ", ")
							+ StoreEditor.storeNames[s];
						if (first)
							first = false;
					}
				}
			}
			println("Stores: " + (storestr.length() > 0 ? storestr : "(none)"));

			String enemystr = new String();
			first = true;
			for (int e = 0; e < EnemyEditor.enemies.length; e++)
			{
				if (EnemyEditor.enemies[e].getItem() == i.number)
				{
					enemystr += (first ? "" : ", ")
						+ EnemyEditor.enemies[e].getName()
						+ " ("
						+ (int) Math.pow(
							(double) 2,
							(double) EnemyEditor.enemies[e].getFreq())
						+ "/128)";
					if (first)
						first = false;
				}
			}
			println(
				"Enemies: " + (enemystr.length() > 0 ? enemystr : "(none)"));
			println("");
		}
	}

	private void println(String in)
	{
		System.out.println(in);
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init()
	{}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{}
}
