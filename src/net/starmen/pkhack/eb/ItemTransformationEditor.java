package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Class providing GUI and API for editing the the item transformation table in EarthBound.
 * Requires ItemEditor because the GUI automatically sets the "chicken" flag that indicates
 * an item is in this table.
 * 
 * @author AnyoneEB
 * @see ItemEditor
 */
public class ItemTransformationEditor
	extends EbHackModule
	implements ActionListener
{
	/**
     * @param rom
     * @param prefs
     */
    public ItemTransformationEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
	private JTextField soundFreq, delay;
	private JComboBox entries, soundEffect;
	private ItemEditor.ItemEntry baseItem, newItem;
	/**
	 * Holds the four ItemTransformation entries.
	 * 
	 * @see ItemTransformation
	 */
	public static ItemTransformation[] its = new ItemTransformation[4];

	protected void init()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		//mainWindow.setSize(470, 230);
		mainWindow.setResizable(false);

		JPanel entry = new JPanel();
		entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
		entry.add(
			getLabeledComponent("Entry Number:", entries = new JComboBox()));
		entries.setActionCommand("entries");
		entries.addActionListener(this);
		baseItem = new ItemEditor.ItemEntry("Base Item", this);
		entry.add(baseItem);
		entry.add(
			getLabeledComponent(
				"Sound Effect: ",
				soundEffect = new JComboBox()));
		entry.add(
			getLabeledComponent(
				"Sound Frequency: ",
				soundFreq = new JTextField(3)));
		newItem = new ItemEditor.ItemEntry("New Item", this);
		entry.add(newItem);
		entry.add(getLabeledComponent("Delay: ", delay = new JTextField(3)));
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
		return "Item Transformation Editor";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by AnyoneEB\n"
			+ "Information discovered by michael_cayer\n"
			+ "Sound Effects list from Control Code Chrestomathy";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#show()
	 */
	public void show()
	{
		super.show();
		ItemEditor.readFromRom(this);
		initSelectors();
		
		mainWindow.pack();
		mainWindow.setVisible(true);
	}
	
	/**
	 * Reads information from the ROM into {@link #its}.
	 */
	public static void readFromRom(Rom rom)
	{
		for (int i = 0; i < its.length; i++)
		{
			its[i] = new ItemTransformation(i, rom);
		}
	}
	private void readFromRom()
	{
	    readFromRom(rom);
	}
	private void initSelectors()
	{
		readFromRom();
		
		soundEffect.removeAllItems();
		for (int i = 0; i < soundEffects.length; i++)
		{
			soundEffect.addItem(getNumberedString(soundEffects[i], i));
		}

		entries.removeAllItems();
		for (int i = 0; i < its.length; i++)
		{
			entries.addItem(its[i]);
		}/*
		entries.setMaximumSize(baseItem.getPreferredSize());
		entries.setMinimumSize(baseItem.getPreferredSize());
		entries.setPreferredSize(baseItem.getPreferredSize());
*/	}

	/**
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{
		mainWindow.setVisible(false);
	}

	private void showInfo(int i)
	{
		this.baseItem.setSelectedIndex(its[i].baseItem);
		this.soundEffect.setSelectedIndex(its[i].soundEffect);
		this.soundFreq.setText(Integer.toString(its[i].soundFreq));
		this.newItem.setSelectedIndex(its[i].newItem);
		this.delay.setText(Integer.toString(its[i].delay));
	}

	private void saveInfo(int i)
	{
		its[i].baseItem = this.baseItem.getSelectedIndex();
		its[i].soundEffect = this.soundEffect.getSelectedIndex();
		its[i].soundFreq = Integer.parseInt(this.soundFreq.getText());
		its[i].newItem = this.newItem.getSelectedIndex();
		its[i].delay = Integer.parseInt(this.delay.getText());

		its[i].writeInfo();

		ItemEditor.Item baseI = ItemEditor.items[its[i].baseItem];
		baseI.ownership |= ((1 << 4) & 255); //set chicken bit to 1
		baseI.writeInfo();
		ItemEditor.Item newI = ItemEditor.items[its[i].newItem];
		newI.ownership |= ((1 << 4) & 255); //set chicken bit to 1
		newI.writeInfo();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals(entries.getActionCommand()))
		{
			//call showItemInfo based on the text of the new itemSelector selection
			if(entries.getSelectedIndex() < 0) return;
			showInfo(
				Integer.parseInt(
					new StringTokenizer(
						entries.getSelectedItem().toString(),
						"[]",
						false)
						.nextToken(),
					(HackModule.getUseHexNumbers() ? 16 : 10)));
		}
		else if (ae.getActionCommand().equalsIgnoreCase("close"))
		{
			hide();
		}
		else if (ae.getActionCommand().equalsIgnoreCase("apply"))
		{
			//put text field info into the Item object, then call items[i].writeInfo();
			if(entries.getSelectedIndex() < 0) return;
			saveInfo(
				Integer.parseInt(
					new StringTokenizer(
						entries.getSelectedItem().toString(),
						"[]",
						false)
						.nextToken(),
					(HackModule.getUseHexNumbers() ? 16 : 10)));
			entries.repaint();
		}
	}
	
	/**
	 * Represents an item transformation entry in the ROM.
	 * These entries are used in EarthBound for the fresh egg/chick/chicken thing.
	 */
	public static class ItemTransformation
	{
	    private Rom rom;
		/** Where this is located in the ROM.
		 */
		public int address;
		/** What number entry this is (0-3).
		 */
		public int num;
		/** Item that gets transformed.
		 */
		public int baseItem;
		/** Sound effect to be made while holding item. Uses the [1F 02 XX] listing.
		 * 
		 * @see EbHackModule#soundEffects
		 */
		public int soundEffect;
		/** How often the sound is played.
		 * A low number will have it often, and a higher one will have the sound occuring more rarely.
		 */
		public int soundFreq;
		/** Item it transforms to.
		 */
		public int newItem;
		/** How long before transforming. Believed to be measured in seconds (not tested).
		 */
		public int delay;
	
		/** Creates a new <code>ItemTransformation</code> representing entry #<code>entryNum</code>.
		 * <code>entryNum<code> should be 0-3, otherwise none of the varibles will be set.
		 * 
		 * @param entryNum Entry number to read in. (0-3)
		 */
		public ItemTransformation(int entryNum, Rom rom)
		{
		    this.rom = rom;
			this.num = entryNum;
			if (this.num > 4 || this.num < 0)
			{
				return;
			}
			this.address = 0x15F6BB + (5 * this.num);

			rom.seek(this.address);

			this.baseItem = rom.readSeek();
			this.soundEffect = rom.readSeek();
			this.soundFreq = rom.readSeek();
			this.newItem = rom.readSeek();
			this.delay = rom.readSeek();
		}
		
		/** Writes information in this into the ROM.
		 */
		public void writeInfo()
		{
			rom.seek(this.address);

			rom.writeSeek(this.baseItem);
			rom.writeSeek(this.soundEffect);
			rom.writeSeek(this.soundFreq);
			rom.writeSeek(this.newItem);
			rom.writeSeek(this.delay);
		}
		
		/** 
		 * Makes a <code>String</code> containing the entry number and base item of this.
		 * The form is "[entryNum] baseItem".
		 * 
		 * @return A <code>String</code> in the form "[entryNum] baseItem".
		 */
		public String toString()
		{
			return "["
				+ this.num
				+ "] "
				+ new String(ItemEditor.items[baseItem].name).trim();
		}
	}
}
