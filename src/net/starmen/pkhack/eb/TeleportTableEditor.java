/*
 * Created on Aug 21, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Editor for the [1F 21 XX] Teleport Table. More information about the table
 * can be found at {@link TeleportTableEntry}.
 * 
 * @author AnyoneEB
 */
public class TeleportTableEditor extends EbHackModule implements ActionListener
{

	/**
     * @param rom
     * @param prefs
     */
    public TeleportTableEditor(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    /**
	 * This represents an entry of the [1F 21 XX] Teleport Table. The table is
	 * located at 0x15EDAB. Table entries are eight bytes long. For more
	 * information read the documentation at
	 * <a href = "http://pkhack.starmen.net/down/docs/1F21table.txt">
	 * http://pkhack.starmen.net/down/docs/1F21table.txt</a>.
	 * 
	 * @author AnyoneEB
	 */
	public static class TeleportTableEntry
	{
	    private AbstractRom rom;
		private int x, y, dir, style, unknown[];
		private int address, num;

		public TeleportTableEntry(int num, AbstractRom rom)
		{
		    this.rom = rom;
			this.num = num;
			address = 0x15EDAB + (8 * num);
			
			rom.seek(address);

			x = rom.readMultiSeek(2);
			y = rom.readMultiSeek(2);
			dir = rom.readSeek();
			style = rom.readSeek();
			unknown = rom.readSeek(2);
		}

		/**
		 * Returns the direction the characters will be facing after
		 * teleporting. The values increment by 45 degrees clockwise at a time.
		 * 1 = N, 2 = NE, 3 = E, 4 = SE, etc. Higher values probably wrap, not
		 * sure.
		 * 
		 * @return Direction (1 - 8)
		 */
		public int getDirection()
		{
			return dir;
		}

		/**
		 * Sets the direction the characters will be facing after teleporting.
		 * The values increment by 45 degrees clockwise at a time. 1 = N, 2 =
		 * NE, 3 = E, 4 = SE, etc. Higher values probably wrap, not sure.
		 * 
		 * @param dir Direction (1 - 8)
		 */
		public void setDirection(int dir)
		{
			this.dir = dir;
		}

		/**
		 * Returns the warp style of this teleport. A list of documented
		 * entries can be found <a  href =
		 * "http://pkhack.starmen.net/down/docs/1F21table.txt">here</a>.
		 * 
		 * @return Warp style (0x00 - 0x33?) (may be up to 0xFF)
		 */
		public int getStyle()
		{
			return style;
		}

		/**
		 * Sets the warp style of this teleport. A list of documented entries
		 * can be found <a  href =
		 * "http://pkhack.starmen.net/down/docs/1F21table.txt">here</a>.
		 * 
		 * @param style Warp style (0x00 - 0x33?) (may be up to 0xFF)
		 */
		public void setStyle(int style)
		{
			this.style = style;
		}

		/**
		 * Returns the specified unknown value.
		 * 
		 * @return the n'th unknown value.
		 * @param n Which unknown (0-1)
		 */
		public int getUnknown(int n)
		{
			return unknown[n];
		}

		/**
		 * Sets te specified unknown value.
		 * 
		 * @param unknown Value to set
		 * @param n Which unknown (0-1)
		 */
		public void setUnknown(int unknown, int n)
		{
			this.unknown[n] = unknown;
		}

		/**
		 * Returns the destination X-coordinate.
		 * 
		 * @return X-coordinate of destination.
		 */
		public int getX()
		{
			return x;
		}

		/**
		 * Sets the destination X-coordinate.
		 * 
		 * @param x X-coordinate of destination.
		 */
		public void setX(int x)
		{
			this.x = x;
		}

		/**
		 * Returns the destination Y-coordinate.
		 * 
		 * @return Y-coordinate of destination.
		 */
		public int getY()
		{
			return y;
		}

		/**
		 * Sets the destination Y-coordinate.
		 * 
		 * @param y Y-coordinate of destination.
		 */
		public void setY(int y)
		{
			this.y = y;
		}

		/**
		 * Writes this [1F 21 XX] entry to the ROM.
		 */
		public void writeInfo()
		{
			rom.seek(address);
			
			rom.writeSeek(x, 2);
			rom.writeSeek(y, 2);
			rom.writeSeek(dir);
			rom.writeSeek(style);
			rom.writeSeek(unknown, 2);
		}
	}

	/** Array of {@link TeleportTableEntry}'s. */
	public static TeleportTableEntry[] ttentries = new TeleportTableEntry[0xE9];

	/** Reads information from ROM into {@link #ttentries}. */
	public static void readFromRom(AbstractRom rom)
	{
		for (int i = 0; i < ttentries.length; i++)
		{
			ttentries[i] = new TeleportTableEntry(i, rom);
		}
	}
	private void readFromRom()
	{
	    readFromRom(rom);
	}

	/** Array of Strings describing the different warp styles. */
	private static boolean warpStyleNamesInited = false;
	public static String[] warpStyleNames = new String[256];;

	/** Inits {@link #warpStyleNames} from warpStyleNames.txt. */
	public static void initWarpStyleNames()
	{
		if (!warpStyleNamesInited)
		{
			warpStyleNamesInited = true;

			readArray(DEFAULT_BASE_DIR,"warpStyleNames.txt", true, warpStyleNames);
		}
	}
	private JComboBox selector;

	private JTextField x, y;
	private JComboBox dir, style;
	private JTextField[] unknown = new JTextField[2];

	protected void init()
	{
		initWarpStyleNames();

		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		//mainWindow.setSize(710, 230);
		//mainWindow.setResizable(false);

		Box entry = new Box(BoxLayout.Y_AXIS);

		selector =
			HackModule.createJComboBoxFromArray(new Object[ttentries.length]);
		selector.addActionListener(this);
		entry.add(HackModule.getLabeledComponent("Entry: ", selector));

		entry.add(
			HackModule.getLabeledComponent(
				"X: ",
				x = HackModule.createSizedJTextField(5, true)));
		entry.add(
			HackModule.getLabeledComponent(
				"Y: ",
				y = HackModule.createSizedJTextField(5, true)));

		entry.add(
			HackModule.getLabeledComponent(
				"Direction: ",
				dir =
					HackModule.createJComboBoxFromArray(
						new String[] {
							"[1] Up",
							"[2] Up-Right",
							"[3] Right",
							"[4] Down-Right",
							"[5] Down",
							"[6] Down-Left",
							"[7] Left",
							"[8] Up-Left" })));
		entry.add(
			HackModule.getLabeledComponent(
				"Warp Style: ",
				style = HackModule.createJComboBoxFromArray(warpStyleNames)));

		for (int i = 0; i < unknown.length; i++)
			entry.add(
				HackModule.getLabeledComponent(
					"Unknown #" + i + ": ",
					unknown[i] = HackModule.createSizedJTextField(3, true)));

		mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
		mainWindow.pack();
	}

	public String getVersion()
	{
		return "0.1";
	}
	public String getDescription()
	{
		return "[1F 21 XX] Teleport Table Editor";
	}
	public String getCredits()
	{
		return "Written by AnyoneEB\n"
			+ "Based on documentation by michael_cayer";
	}

	public void hide()
	{
		mainWindow.setVisible(false);
	}
	public void show()
	{
		super.show();

		readFromRom();
		selector.setSelectedIndex(0);

		mainWindow.setVisible(true);
	}

	private void showInfo()
	{
		TeleportTableEntry t = ttentries[selector.getSelectedIndex()];

		x.setText(Integer.toString(t.getX()));
		y.setText(Integer.toString(t.getY()));
		dir.setSelectedIndex(t.getDirection() - 1);
		style.setSelectedIndex(t.getStyle());
		for (int i = 0; i < unknown.length; i++)
			unknown[i].setText(Integer.toString(t.getUnknown(i)));

		mainWindow.repaint();
	}
	private void saveInfo()
	{
		TeleportTableEntry t = ttentries[selector.getSelectedIndex()];

		t.setX(Integer.parseInt(x.getText()));
		t.setY(Integer.parseInt(y.getText()));
		t.setDirection(dir.getSelectedIndex() + 1);
		t.setStyle(style.getSelectedIndex());
		for (int i = 0; i < unknown.length; i++)
			t.setUnknown(Integer.parseInt(unknown[i].getText()), i);

		t.writeInfo();
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals(selector.getActionCommand()))
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
