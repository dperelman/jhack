package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits the phone list.
 * 
 * @author AnyoneEB
 */
public class PhoneListEditor extends EbHackModule implements ActionListener
{
	/**
     * @param rom
     * @param prefs
     */
    public PhoneListEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
        // TODO Auto-generated constructor stub
    }
	private JComboBox selector;
	private JTextField name, flag;
	private TextEditor.TextOffsetEntry pointer;
	private static final int PHONE_ENTRIES = 6;
	/**
	 * Array of all the phone list entries.
	 * 
	 * @see PhoneNumber
	 */
	public static PhoneNumber[] phone;
	private static Icon phoneIcon = initIcon();

	protected void init()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
		//mainWindow.setSize(340, 170);
		mainWindow.setResizable(false);

		//make a JComboBox to select entry, and a JTextField to edit it
		JPanel entry = new JPanel();
		entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

		selector = new JComboBox();
		selector.setActionCommand("phoneSelector");
		selector.addActionListener(this);
		entry.add(getLabeledComponent("Entry: ", selector));
		entry.add(getLabeledComponent("Name: ", name = HackModule.createSizedJTextField(25)));
		entry.add(getLabeledComponent("Flag: ", flag = HackModule.createSizedJTextField(5)));
//		pointer = new JTextField(8);
//		pointer.setDocument(new MaxLengthDocument(8));
//		entry.add(getLabeledComponent("Pointer $:", pointer));
		entry.add(pointer = new TextEditor.TextOffsetEntry("Pointer", true));

		mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
		mainWindow.pack();
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
		return "Phone List Editor";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by AnyoneEB\n" + "Based on PK Hack source code";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getIcon()
	 */
	public Icon getIcon()
	{
		return phoneIcon;
	}

	private static Icon initIcon()
	{
		BufferedImage out =
			new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = out.getGraphics();

		//Created by ImageFileToCode from net/starmen/pkhack/phone.gif
		g.setColor(new Color(255, 255, 255));
		g.setColor(new Color(45, 33, 33));
		g.drawLine(7, 2, 7, 2);
		g.setColor(new Color(43, 35, 33));
		g.drawLine(8, 2, 8, 2);
		g.setColor(new Color(42, 34, 32));
		g.drawLine(9, 2, 9, 2);
		g.setColor(new Color(43, 34, 35));
		g.drawLine(10, 2, 10, 2);
		g.drawLine(11, 2, 11, 2);
		g.setColor(new Color(44, 35, 26));
		g.drawLine(12, 2, 12, 2);
		g.drawLine(13, 2, 13, 2);
		g.setColor(new Color(42, 36, 36));
		g.drawLine(3, 3, 3, 3);
		g.setColor(new Color(41, 36, 33));
		g.drawLine(4, 3, 4, 3);
		g.setColor(new Color(44, 34, 33));
		g.drawLine(5, 3, 5, 3);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(6, 3, 6, 3);
		g.drawLine(7, 3, 7, 3);
		g.setColor(new Color(44, 35, 26));
		g.drawLine(12, 3, 12, 3);
		g.setColor(new Color(159, 150, 141));
		g.drawLine(13, 3, 13, 3);
		g.setColor(new Color(41, 36, 33));
		g.drawLine(14, 3, 14, 3);
		g.setColor(new Color(44, 35, 28));
		g.drawLine(2, 4, 2, 4);
		g.setColor(new Color(159, 150, 143));
		g.drawLine(3, 4, 3, 4);
		g.setColor(new Color(43, 35, 32));
		g.drawLine(4, 4, 4, 4);
		g.setColor(new Color(40, 36, 35));
		g.drawLine(13, 4, 13, 4);
		g.drawLine(14, 4, 14, 4);
		g.setColor(new Color(44, 35, 28));
		g.drawLine(2, 5, 2, 5);
		g.drawLine(3, 5, 3, 5);
		g.setColor(new Color(43, 35, 32));
		g.drawLine(4, 5, 4, 5);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(10, 6, 10, 6);
		g.setColor(new Color(38, 37, 35));
		g.drawLine(6, 7, 6, 7);
		g.setColor(new Color(41, 36, 33));
		g.drawLine(8, 7, 8, 7);
		g.setColor(new Color(44, 34, 33));
		g.drawLine(9, 7, 9, 7);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(10, 7, 10, 7);
		g.drawLine(11, 7, 11, 7);
		g.drawLine(5, 8, 5, 8);
		g.drawLine(6, 8, 6, 8);
		g.drawLine(7, 8, 7, 8);
		g.setColor(new Color(208, 208, 200));
		g.drawLine(8, 8, 8, 8);
		g.setColor(new Color(38, 38, 30));
		g.drawLine(9, 8, 9, 8);
		g.setColor(new Color(45, 34, 28));
		g.drawLine(10, 8, 10, 8);
		g.setColor(new Color(160, 149, 143));
		g.drawLine(11, 8, 11, 8);
		g.setColor(new Color(43, 36, 28));
		g.drawLine(12, 8, 12, 8);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(4, 9, 4, 9);
		g.drawLine(5, 9, 5, 9);
		g.drawLine(6, 9, 6, 9);
		g.setColor(new Color(248, 236, 236));
		g.drawLine(7, 9, 7, 9);
		g.setColor(new Color(153, 153, 145));
		g.drawLine(8, 9, 8, 9);
		g.setColor(new Color(208, 208, 200));
		g.drawLine(9, 9, 9, 9);
		g.setColor(new Color(44, 33, 27));
		g.drawLine(10, 9, 10, 9);
		g.setColor(new Color(45, 34, 28));
		g.drawLine(11, 9, 11, 9);
		g.setColor(new Color(158, 151, 143));
		g.drawLine(12, 9, 12, 9);
		g.setColor(new Color(40, 37, 28));
		g.drawLine(13, 9, 13, 9);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(3, 10, 3, 10);
		g.drawLine(4, 10, 4, 10);
		g.setColor(new Color(248, 236, 236));
		g.drawLine(5, 10, 5, 10);
		g.setColor(new Color(211, 207, 198));
		g.drawLine(6, 10, 6, 10);
		g.setColor(new Color(156, 152, 143));
		g.drawLine(7, 10, 7, 10);
		g.setColor(new Color(43, 36, 26));
		g.drawLine(8, 10, 8, 10);
		g.setColor(new Color(45, 35, 26));
		g.drawLine(9, 10, 9, 10);
		g.setColor(new Color(48, 32, 32));
		g.drawLine(10, 10, 10, 10);
		g.drawLine(11, 10, 11, 10);
		g.setColor(new Color(160, 150, 141));
		g.drawLine(12, 10, 12, 10);
		g.setColor(new Color(43, 36, 26));
		g.drawLine(13, 10, 13, 10);
		g.setColor(new Color(45, 33, 33));
		g.drawLine(2, 11, 2, 11);
		g.drawLine(3, 11, 3, 11);
		g.drawLine(4, 11, 4, 11);
		g.setColor(new Color(44, 32, 32));
		g.drawLine(5, 11, 5, 11);
		g.setColor(new Color(41, 37, 28));
		g.drawLine(6, 11, 6, 11);
		g.drawLine(7, 11, 7, 11);
		g.setColor(new Color(159, 152, 142));
		g.drawLine(8, 11, 8, 11);
		g.setColor(new Color(45, 35, 26));
		g.drawLine(9, 11, 9, 11);
		g.setColor(new Color(48, 32, 32));
		g.drawLine(10, 11, 10, 11);
		g.drawLine(11, 11, 11, 11);
		g.setColor(new Color(45, 35, 26));
		g.drawLine(12, 11, 12, 11);
		g.setColor(new Color(43, 36, 26));
		g.drawLine(13, 11, 13, 11);
		g.setColor(new Color(42, 38, 29));
		g.drawLine(2, 12, 2, 12);
		g.setColor(new Color(156, 152, 143));
		g.drawLine(3, 12, 3, 12);
		g.drawLine(4, 12, 4, 12);
		g.setColor(new Color(211, 207, 198));
		g.drawLine(5, 12, 5, 12);
		g.setColor(new Color(212, 208, 199));
		g.drawLine(6, 12, 6, 12);
		g.setColor(new Color(155, 151, 142));
		g.drawLine(7, 12, 7, 12);
		g.setColor(new Color(43, 35, 32));
		g.drawLine(8, 12, 8, 12);
		g.setColor(new Color(43, 34, 35));
		g.drawLine(9, 12, 9, 12);
		g.drawLine(10, 12, 10, 12);
		g.drawLine(11, 12, 11, 12);
		g.setColor(new Color(41, 37, 28));
		g.drawLine(3, 13, 3, 13);
		g.drawLine(4, 13, 4, 13);
		g.drawLine(5, 13, 5, 13);
		g.drawLine(6, 13, 6, 13);
		g.drawLine(7, 13, 7, 13);

		return new ImageIcon(out);
	}

	/**
	 * @see net.starmen.pkhack.HackModule#show()
	 */
	public void show()
	{
		super.show();
		readFromRom();
		initSelector();
		mainWindow.setVisible(true);
	}
	
	/**
	 * Reads from the ROM into {@link #phone}.
	 * 
	 * @see PhoneListEditor.PhoneNumber
	 * @see #phone
	 */
	public static void readFromRom(HackModule hm)
	{
		phone = new PhoneNumber[PHONE_ENTRIES];
		for (int i = 0; i < phone.length; i++)
		{
			phone[i] = new PhoneNumber(i, hm);
		}
	}
	private void readFromRom()
	{
	    readFromRom(this);
	}
	private void initSelector()
	{
		selector.removeAllItems();
		for (int i = 0; i < phone.length; i++)
		{
			selector.addItem(phone[i]);
		}
		selector.setSelectedIndex(0);
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
		if(i < 0) return;
		phone[i].setName(name.getText());
		phone[i].setFlag(flag.getText());
		phone[i].setPointer(pointer.getOffset());
		phone[i].writeInfo();
		selector.repaint();
	}
	private void showInfo(int i)
	{
		if(i < 0) return;
		name.setText(phone[i].getName());
		flag.setText(phone[i].getFlag());
		pointer.setOffset(phone[i].getPointerAsInt());
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
	 * Represents a phone list entry in EarthBound.
	 */
	public static class PhoneNumber
	{
	    private HackModule hm;
		private char[] name = new char[25];
		private int pointer;
		private int flag;
		private int address;
		private int num;
		
		/**
		 * Creates a new <code>PhoneNumber</code> based on the specified entry number.
		 * 
		 * @param num Entry number to read in.
		 */
		public PhoneNumber(int num, HackModule hm)
		{
		    this.hm =hm;
			this.num = num;
			this.address = 0x157cae + (num * 31);
			
			Rom rom = hm.rom;
			
			rom.seek(this.address);

			for (int j = 0; j < name.length; j++)
			{
				this.name[j] = hm.simpToRegChar(rom.readCharSeek());
			}

			this.flag = (rom.readSeek() << 8);
			this.flag += rom.readSeek();
			this.pointer = rom.readMultiSeek(4);
		}
		/**
		 * Returns a <code>String</code> representing this.
		 * 
		 * @return A <code>String</code> in the form "[num] name".
		 */
		public String toString()
		{
			return HackModule.getNumberedString(this.getName(), this.num);
		}
		
		/** 
		 * Returns the name of this phone list entry.
		 * 
		 * @return The name of this phone list entry.
		 */
		public String getName()
		{
			return new String(this.name).trim();
		}
		/**
		 * Sets the name of this phonelist entry.
		 * If longer than 25 characters, only the first 25 characters will be used.
		 * 
		 * @param newName Name to set.
		 */
		public void setName(String newName)
		{
			char[] temp = newName.trim().toCharArray();
			for (int j = 0; j < name.length; j++)
			{
				name[j] = (j < temp.length ? temp[j] : (char) 0);
			}
		}
		/**
		 * Returns a <code>String</code> of the text pointer for this phone list entry.
		 * It will always be at least six characters long with leading zeros added as needed.
		 * The pointer is an SNES pointer.
		 * 
		 * @return The text pointer for this phone list entry as a <code>String</code>
		 */
		public String getPointer()
		{
			return addZeros(Integer.toHexString(pointer), 6);
		}
		/**
		 * Returns an <code>int</code> of the text pointer for this phone list entry.
		 * 
		 * @return The text pointer for this phone list entry as an <code>int</code>
		 */
		public int getPointerAsInt()
		{
		    return pointer;
		}
		/**
		 * Sets the text pointer of this phone list entry.
		 * 
		 * @param in A <code>String</code> containing an SNES pointer as hexidecimal.
		 */
		public void setPointer(String in)
		{
			pointer = Integer.parseInt(in, 16);
		}
		/**
		 * Sets the text pointer of this phone list entry.
		 * 
		 * @param in A <code>int</code> containing an SNES pointer.
		 */
		public void setPointer(int in)
		{
		    pointer = in;
		}
		/**
		 * Returns the flag for this phone list entry. If the flag is set
		 * this entry will be shown in the phone list window in the game.
		 * The returned <code>String</code> is made to look nice by leaving
		 * the bytes in reversed order and putting a space between them. As
		 * they would be seen in the text editor.
		 * 
		 * @return The flag in hexidecimal as <code>String</code> with the bytes reversed.
		 */
		public String getFlag()
		{
			String temp;
			temp = addZeros(Integer.toString(this.flag, 16), 4);
			temp = temp.substring(0, 2) + " " + temp.substring(2, 4);
			return temp;
		}
		/**
		 * Sets the flag for this phone list entry. If the flag is set
		 * this entry will be shown in the phone list window in the game.
		 * The <code>String</code> should have the bytes in reversed order.
		 * If there is a space in the middle it will be removed. Basically,
		 * this takes input in the form {@link #getFlag()} outputs.
		 * 
		 * @param in The flag in hexidecimal as <code>String</code> with the bytes reversed.
		 */
		public void setFlag(String in)
		{
			flag = Integer.parseInt(killSpaces(in), 16);
		}
		
		/** Writes the information contained by this into the ROM.
		 */
		public void writeInfo()
		{
		    Rom rom = hm.rom;
		    
			rom.seek(this.address);
			for (int j = 0; j < this.name.length; j++)
			{
				rom.writeSeek(hm.simpToGameChar(this.name[j]));
			}
			rom.writeSeek((this.flag >> 8) & 255);
			rom.writeSeek(this.flag & 255);
			rom.writeSeek(this.pointer, 4);
		}
	}
}
