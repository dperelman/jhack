package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

public class EventMusicEditor extends EbHackModule implements ActionListener
{
    public EventMusicEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    
    private EventMusicEntry[] entries = new EventMusicEntry[164];
    private JComboBox entryChooser, coorChooser, musicChooser;
    private JTextField flagField;
    private JCheckBox reverseCheck;
    private boolean updatingCoors = false;
    public static final int asmPointer = 0x6b39;
    public static final String[] musicNames = new String[] {
    		"00 - None",
			"01 - Gas station",
			"02 - Naming screen",
			"03 - Setup screen",
			"04 - None",
			"05 - You win 1",
			"06 - Level up",
			"07 - You lose",
			"08 - Battle swirl 1",
			"09 - Battle swirl 2",
			"0A - What the heck? [Ness' house]",
			"0B - Fanfare [Battle swirl 3]",
			"0C - You win!",
			"0D - Teleport out",
			"0E - Teleport fail",
			"0F - Falling underground",
			"10 - Dr. Andonuts' lab",
			"11 - Monotoli building",
			"12 - Sloppy house",
			"13 - Neighbor's house",
			"14 - Arcade",
			"15 - Pokey's house",
			"16 - Hospital",
			"17 - Ness' house (Pollyanna)",
			"18 - Paula's theme",
			"19 - Chaos Theater",
			"1A - Hotel",
			"1B - Good morning",
			"1C - Department Store",
			"1D - Onett at night 1",
			"1E - Your Sanctuary 1",
			"1F - Your Sanctuary 2",
			"20 - Giant Step",
			"21 - Lilliput Steps",
			"22 - Milky Well",
			"23 - Rainy Circle",
			"24 - Magnet Hill",
			"25 - Pink Cloud",
			"26 - Lumine Hall",
			"27 - Fire Spring",
			"28 - near a boss",
			"29 - Alien Investigation",
			"2A - Fire Springs hall",
			"2B - Belch's Base",
			"2C - Zombie Threed",
			"2D - Spooky Cave",
			"2E - Onett",
			"2F - Fourside",
			"30 - Saturn Valley",
			"31 - Monkey Caves",
			"32 - Moonside",
			"33 - Dusty Dunes Desert",
			"34 - Peaceful Rest Valley",
			"35 - Zombie Threed",
			"36 - Winters",
			"37 - Cave near a boss",
			"38 - Summers",
			"39 - Jackie's Cafe",
			"3A - Sailing to Scaraba",
			"3B - Dalaam",
			"3C - Mu Training",
			"3D - Bazaar",
			"3E - Scaraba desert",
			"3F - Pyramid",
			"40 - Deep Darkness",
			"41 - Tenda Village",
			"42 - Welcome home",
			"43 - The Sea of Eden",
			"44 - Lost Underworld",
			"45 - First step back",
			"46 - Second step back",
			"47 - The Place",
			"48 - Giygas Awakens",
			"49 - Giygas phase 2",
			"4A - Giygas is weakened",
			"4B - Giygas death",
			"4C - Runaway 5 concert 1",
			"4D - Runaway 5 tour bus",
			"4E - Runaway 5 concert 2",
			"4F - Power",
			"50 - Venus' concert",
			"51 - Yellow submarine",
			"52 - Bicycle",
			"53 - Sky Runner",
			"54 - Sky Runner falling",
			"55 - Bulldozer",
			"56 - Tessie",
			"57 - City bus",
			"58 - Fuzzy Pickles",
			"59 - Delivery",
			"5A - Return to your body",
			"5B - Phase Distorter going back in time",
			"5C - Coffee break",
			"5D - Because I Love You",
			"5E - Good Friends, Bad Friends",
			"5F - Smiles and Tears",
			"60 - vs. Cranky Lady",
			"61 - vs. Spinning Robo",
			"62 - vs. S. Evil Mushroom",
			"63 - vs. Master Belch",
			"64 - vs. N. A. Retro Hippie",
			"65 - vs. Runaway Dog",
			"66 - vs. Cave Boy",
			"67 - vs. Your Sanctuary Boss",
			"68 - vs. Kraken",
			"69 - Giygas heavy metal",
			"6A - Inside the Dungeon",
			"6B - Megaton Walk",
			"6C - Sea of Eden",
			"6D - Explosion? [Stonehenge destructs]",
			"6E - Sky Runner crash",
			"6F - Magic Cake",
			"70 - Pokey's House (Buzz Buzz)",
			"71 - Buzz Buzz swatted",
			"72 - Onett at night (Buzz Buzz)",
			"73 - Phone call",
			"74 - Knock knock - right",
			"75 - Rabbit Cave",
			"76 - Onett at night 3",
			"77 - Apple of Enlightenment",
			"78 - Hotel of the Living Dead",
			"79 - Onett Intro",
			"7A - Sunrise, Onett",
			"7B - Someone joins",
			"7C - Enter Starman Jr.",
			"7D - Boarding school",
			"7E - Phase Distorter",
			"7F - Phase Distorter II",
			"80 - Boy Meets Girl (Twoson)",
			"81 - Happy Threed",
			"82 - Runaway 5 are freed",
			"83 - Flying Man",
			"84 - Onett at night 2",
			"85 - Hidden Song",
			"86 - Your Sanctuary boss",
			"87 - Teleport in",
			"88 - Saturn Valley cave",
			"89 - Elevator down",
			"8A - Elevator up",
			"8B - Elevator stopping",
			"8C - Topolla Theater",
			"8D - vs. Master Barf",
			"8E - going to Magicant",
			"8F - leaving Magicant",
			"90 - defeated the Kraken",
			"91 - stonehenge destructs",
			"92 - Tessie sighting",
			"93 - Meteor fall",
			"94 - vs. Starman Jr.",
			"95 - Runaway 5 help out",
			"96 - Knock knock - left",
			"97 - Onett after meteor 1",
			"98 - Onett after meteor 2",
			"99 - Pokey's theme",
			"9A - Onett at night 4 (Buzz Buzz)",
			"9B - Your Sanctuary boss",
			"9C - Meteor strike",
			"9D - Attract mode",
			"9E - Are you sure?  Yep",
			"9F - Peaceful Rest Valley 2",
			"A0 - rec. Giant Step",
			"A1 - rec. Lilliput Steps",
			"A2 - rec. Milky well",
			"A3 - rec. Rainy Circle",
			"A4 - rec. Magnet Hill",
			"A5 - rec. Pink Cloud",
			"A6 - rec. Lumine Hall",
			"A7 - rec. Fire Spring",
			"A8 - Sound Stone",
			"A9 - Eight Melodies",
			"AA - Dalaam Intro",
			"AB - Winters intro",
			"AC - Pokey escapes",
			"AD - Wake up - Moonside",
			"AE - Gas Station 2",
			"AF - Title screen",
			"B0 - Battle swirl (Normal)",
			"B1 - Pokey Intro",
			"B2 - Wake up - Scaraba",
			"B3 - Robotomy part 1",
			"B4 - Pokey escapes",
			"B5 - Return to your body",
			"B6 - Giygas static",
			"B7 - sudden victory",
			"B8 - You win vs. boss",
			"B9 - Giygas phase three",
			"BA - Giygas phase one",
			"BB - Give us strength",
			"BC - Good Morning",
			"BD - Sound Stone",
			"BE - Giygas death",
			"BF - Giygas weakened"
    };
    /*public static final String[] musicNames = new String[] {
    		"None",
    		"Gas station",
    		"Naming screen",
    		"Setup screen",
    		"None",
    		"You win 1",
    		"Level up",
    		"You lose",
    		"Battle swirl 1",
    		"Battle swirl 2",
    		"What the heck? [Ness' house]",
    		"Fanfare [Battle swirl 3]",
    		"You win!",
    		"Teleport out",
    		"Teleport fail",
    		"Falling underground",
    		"Dr. Andonuts' lab",
    		"Monotoli building",
    		"Sloppy house",
    		"Neighbor's house",
    		"Arcade",
    		"Pokey's house",
    		"Hospital",
    		"Ness' house (Pollyanna)",
    		"Paula's theme",
    		"Chaos Theater",
    		"Hotel",
    		"Good morning",
    		"Department Store",
    		"Onett at night 1",
    		"Your Sanctuary 1",
    		"Your Sanctuary 2",
    		"Giant Step",
    		"Lilliput Steps",
    		"Milky Well",
    		"Rainy Circle",
    		"Magnet Hill",
    		"Pink Cloud",
    		"Lumine Hall",
    		"Fire Spring",
    		"near a boss",
    		"Alien Investigation",
    		"Fire Springs hall",
    		"Belch's Base",
    		"Zombie Threed",
    		"Spooky Cave",
    		"Onett",
    		"Fourside",
    		"Saturn Valley",
    		"Monkey Caves",
    		"Moonside",
    		"Dusty Dunes Desert",
    		"Peaceful Rest Valley",
    		"Zombie Threed",
    		"Winters",
    		"Cave near a boss",
    		"Summers",
    		"Jackie's Cafe",
    		"Sailing to Scaraba",
    		"Dalaam",
    		"Mu Training",
    		"Bazaar",
    		"Scaraba desert",
    		"Pyramid",
    		"Deep Darkness",
    		"Tenda Village",
    		"Welcome home",
    		"The Sea of Eden",
    		"Lost Underworld",
    		"First step back",
    		"Second step back",
    		"The Place",
    		"Giygas Awakens",
    		"Giygas phase 2",
    		"Giygas is weakened",
    		"Giygas death",
    		"Runaway 5 concert 1",
    		"Runaway 5 tour bus",
    		"Runaway 5 concert 2",
    		"Power",
    		"Venus' concert",
    		"Yellow submarine",
    		"Bicycle",
    		"Sky Runner",
    		"Sky Runner falling",
    		"Bulldozer",
    		"Tessie",
    		"City bus",
    		"Fuzzy Pickles",
    		"Delivery",
    		"Return to your body",
    		"Phase Distorter going back in time",
    		"Coffee break",
    		"Because I Love You",
    		"Good Friends, Bad Friends",
    		"Smiles and Tears",
    		"vs. Cranky Lady",
    		"vs. Spinning Robo",
    		"vs. S. Evil Mushroom",
    		"vs. Master Belch",
    		"vs. N. A. Retro Hippie",
    		"vs. Runaway Dog",
    		"vs. Cave Boy",
    		"vs. Your Sanctuary Boss",
    		"vs. Kraken",
    		"Giygas heavy metal",
    		"Inside the Dungeon",
    		"Megaton Walk",
    		"Sea of Eden",
    		"Explosion? [Stonehenge destructs]",
    		"Sky Runner crash",
    		"Magic Cake",
    		"Pokey's House (Buzz Buzz)",
    		"Buzz Buzz swatted",
    		"Onett at night (Buzz Buzz)",
    		"Phone call",
    		"Knock knock - right",
    		"Rabbit Cave",
    		"Onett at night 3",
    		"Apple of Enlightenment",
    		"Hotel of the Living Dead",
    		"Onett Intro",
    		"Sunrise, Onett",
    		"Someone joins",
    		"Enter Starman Jr.",
    		"Boarding school",
    		"Phase Distorter",
    		"Phase Distorter II",
    		"Boy Meets Girl (Twoson)",
    		"Happy Threed",
    		"Runaway 5 are freed",
    		"Flying Man",
    		"Onett at night 2",
    		"Hidden Song",
    		"Your Sanctuary boss",
    		"Teleport in",
    		"Saturn Valley cave",
    		"Elevator down",
    		"Elevator up",
    		"Elevator stopping",
    		"Topolla Theater",
    		"vs. Master Barf",
    		"going to Magicant",
    		"leaving Magicant",
    		"defeated the Kraken",
    		"stonehenge destructs",
    		"Tessie sighting",
    		"Meteor fall",
    		"vs. Starman Jr.",
    		"Runaway 5 help out",
    		"Knock knock - left",
    		"Onett after meteor 1",
    		"Onett after meteor 2",
    		"Pokey's theme",
    		"Onett at night 4 (Buzz Buzz)",
    		"Your Sanctuary boss",
    		"Meteor strike",
    		"Attract mode",
    		"Are you sure?  Yep",
    		"Peaceful Rest Valley 2",
    		"rec. Giant Step",
    		"rec. Lilliput Steps",
    		"rec. Milky well",
    		"rec. Rainy Circle",
    		"rec. Magnet Hill",
    		"rec. Pink Cloud",
    		"rec. Lumine Hall",
    		"rec. Fire Spring",
    		"Sound Stone",
    		"Eight Melodies",
    		"Dalaam Intro",
    		"Winters intro",
    		"Pokey escapes",
    		"Wake up - Moonside",
    		"Gas Station 2",
    		"Title screen",
    		"Battle swirl (Normal)",
    		"Pokey Intro",
    		"Wake up - Scaraba",
    		"Robotomy part 1",
    		"Pokey escapes",
    		"Return to your body",
    		"Giygas static",
    		"sudden victory",
    		"You win vs. boss",
    		"Giygas phase three",
    		"Giygas phase one",
    		"Give us strength",
    		"Good Morning",
    		"Sound Stone",
    		"Giygas death",
    		"Giygas weakened"
    	};*/

	protected void init()
	{
		readFromRom();
		
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		
		JPanel panel = new JPanel();
		panel.setLayout(
				new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		String[] entryNames = new String[entries.length];
		for (int i = 0; i < entryNames.length; i++)
			entryNames[i] = "#" + i + " (" + entries[i].getDefaultName() + ")";
		entryChooser = new JComboBox(entryNames);
		entryChooser.addActionListener(this);
		panel.add(HackModule.getLabeledComponent(
				"Entry: ", entryChooser));
		
		coorChooser = new JComboBox();
		coorChooser.addActionListener(this);
		panel.add(HackModule.getLabeledComponent(
				"Coorelation: ", coorChooser));
		
		flagField = HackModule.createSizedJTextField(3, true);
		panel.add(HackModule.getLabeledComponent(
				"Event Flag: ", flagField));
		
		reverseCheck = new JCheckBox();
		panel.add(HackModule.getLabeledComponent(
				"Reverse Flag?: ", reverseCheck));
		
		musicChooser = new JComboBox(musicNames);
		panel.add(HackModule.getLabeledComponent(
				"Music: ", musicChooser));
		
		mainWindow.getContentPane().add(
				panel, BorderLayout.CENTER);
		mainWindow.pack();
	}

	public String getVersion()
	{
		return "0.1";
	}

	public String getDescription()
	{
		return "Music/Flags Correlations Editor";
	}

	public String getCredits()
	{
		return null;
	}
	
	public void show()
	{
		super.show();
		
		readFromRom();
		updateCoorelationChooser();
		updateComponents();
		
		mainWindow.setVisible(true);
	}

	public void hide()
	{
		mainWindow.setVisible(false);
	}
	
	public void readFromRom()
	{
		int ptrTable = HackModule.toRegPointer(rom.readMulti(asmPointer, 3)) + 2;
		for (int i = 0; i < entries.length; i++)
		{
			ArrayList correlations = new ArrayList();
			int address = rom.readMulti(ptrTable + (i * 2), 2) + 0xf0200;
			boolean reading = true;
			int j = 0;
			byte defaultMusic = 0;
			while (reading)
			{
				int flag = rom.readMulti(address + (j * 4), 2);
				boolean reversed = false;
				if (flag >= 0x8000)
				{
					reversed = true;
					flag -= 0x8000;
				}
				byte music = rom.readByte(address + (j * 4) + 2);
				if (flag == 0)
				{
					reading = false;
					defaultMusic = music;
				}
				else
					correlations.add(
							new EventMusicEntry.Correlation(
									(short) flag, reversed, music));
				j++;
			}
			entries[i] = new EventMusicEntry(correlations, defaultMusic, (j * 4));
		}
	}
	
	public void saveChanges()
	{
		EventMusicEntry entry =
			entries[entryChooser.getSelectedIndex()];
		if (coorChooser.getSelectedIndex() < entry.size())
		{
			EventMusicEntry.Correlation corr = 
				entry.getCorrelation(
						coorChooser.getSelectedIndex());
			corr.setFlag((short) 
					Integer.parseInt(flagField.getText(), 16));
			corr.setReversed(reverseCheck.isSelected());
			corr.setMusic((byte) musicChooser.getSelectedIndex());
		}
		else
		{
			entry.setDefaultMusic((byte) musicChooser.getSelectedIndex());
		}
	}
	
	public void writeToRom()
	{
		byte[] pointersData = rom.readByte(
				toRegPointer(rom.readMulti(asmPointer, 3)),
				entries.length * 2);
		boolean a = writetoFree(pointersData, asmPointer, 3,
				pointersData.length, pointersData.length, true);
		/*boolean a = writeToFree(pointersData, asmPointer, 0xf0200, 3,
				pointersData.length, pointersData.length);*/
		if (!a)
			System.out.println("oops writing pointers");
		int pointersLoc = toRegPointer(rom.readMulti(asmPointer, 3));
		for (int i = 0; i < entries.length; i++)
		{
			byte[] data = entries[i].toByteArray();
			String debug = "";
			for (int j = 0; j < data.length; j++)
				debug = debug + Integer.toHexString(data[j] & 0xff) + " ";
			System.out.println(i + ": " + debug);
			boolean b = writeToFree(data, pointersLoc + (i * 2), 0xf0000, 2,
					entries[i].getOldLength(), data.length);
			if (!b)
				System.out.println("oops writing data #" + i);
		}
	}
	
	public void updateCoorelationChooser()
	{
		updatingCoors = true;
		EventMusicEntry entry = entries[entryChooser.getSelectedIndex()];
		coorChooser.removeAllItems();
		for (int i = 0; i < entry.size(); i++)
			coorChooser.addItem("#" + i + " (" + entry.getCorrelation(i).getMusicName() + ")");
		coorChooser.addItem("Default (" + entry.getDefaultName() + ")");
		coorChooser.setSelectedIndex(0);
		mainWindow.pack();
		updatingCoors = false;
		updateComponents();
	}
	
	public void updateComponents()
	{
		EventMusicEntry entry =
			entries[entryChooser.getSelectedIndex()];
		if (coorChooser.getSelectedIndex() < entry.size())
		{
			EventMusicEntry.Correlation corr = 
				entry.getCorrelation(
						coorChooser.getSelectedIndex());
			flagField.setEnabled(true);
			flagField.setText(Integer.toHexString(corr.getFlag()));
			reverseCheck.setEnabled(true);
			reverseCheck.setSelected(corr.isReversed());
			musicChooser.setSelectedIndex(corr.getMusic() & 0xff);
		}
		else
		{
			flagField.setText("");
			flagField.setEnabled(false);
			reverseCheck.setSelected(false);
			reverseCheck.setEnabled(false);
			musicChooser.setSelectedIndex(entry.getDefaultMusic() & 0xff);
		}
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals("apply"))
		{
			System.out.println("Applying changes");
			saveChanges();
			writeToRom();
		}
		else if (ae.getActionCommand().equals("close"))
			hide();
		else if (ae.getSource().equals(entryChooser))
			updateCoorelationChooser();
		else if (ae.getSource().equals(coorChooser) && !updatingCoors)
			updateComponents();
	}
	
	public static class EventMusicEntry
	{
		private ArrayList correlations;
		private byte defaultMusic;
		private int oldLength;
		
		public EventMusicEntry(ArrayList correlations, byte defaultMusic, int oldLength)
		{
			this.correlations = correlations;
			this.defaultMusic = defaultMusic;
			this.oldLength = oldLength;
		}
		
		public int getOldLength()
		{
			return oldLength;
		}
		
		public byte getDefaultMusic()
		{
			return defaultMusic;
		}
		
		public String getDefaultName()
		{
			return EventMusicEditor.musicNames[defaultMusic & 0xff];
		}
		
		public void setDefaultMusic(byte defaultMusic)
		{
			this.defaultMusic = defaultMusic;
		}
		
		public Correlation getCorrelation(int num)
		{
			return (Correlation) correlations.get(num);
		}
		
		public int size()
		{
			return correlations.size();
		}
		
		public byte[] toByteArray()
		{
			byte[] out = new byte[(correlations.size() + 1) * 4];
			for (int i = 0; i < correlations.size(); i++)
			{
				Correlation corr = (Correlation) correlations.get(i);
				out[i * 4] = corr.getFlagByte(0);
				out[i * 4 + 1] = corr.getFlagByte(1);
				out[i * 4 + 2] = corr.getMusic();
				out[i * 4 + 3] = 0;
			}
			out[correlations.size() * 4] = 0;
			out[correlations.size() * 4 + 1] = 0;
			out[correlations.size() * 4 + 2] = defaultMusic;
			out[correlations.size() * 4 + 3] = 0;
			return out;
		}
		
		public static class Correlation
		{
			private short flag;
			private boolean reversed;
			private byte music;
			
			public Correlation(short flag, boolean reversed, byte music)
			{
				this.flag = flag;
				this.reversed = reversed;
				this.music = music;
			}
			
			public short getFlag()
			{
				return flag;
			}
			
			public byte getFlagByte(int num)
			{
				if (num == 0)
				{
					return (byte) (flag & 0xff);
				}
				else
				{
					byte flagByte = (byte) ((flag & 0xff00) >> 2);
					if (reversed)
						return (byte) (flagByte + 0x80);
					else
						return flagByte;
				}
			}
			
			public boolean isReversed()
			{
				return reversed;
			}
			
			public byte getMusic()
			{
				return music;
			}
			
			public String getMusicName()
			{
				return EventMusicEditor.musicNames[music & 0xff];
			}
			
			public void setFlag(short flag)
			{
				this.flag = flag;
			}
			
			public void setReversed(boolean reversed)
			{
				this.reversed = reversed;
			}
			
			public void setMusic(byte music)
			{
				this.music = music;
			}
		}
	}
}
