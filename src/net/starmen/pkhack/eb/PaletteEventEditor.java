package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * @author Mr. Tenda
 *
 * TODO Write javadoc for this class.
 */
public class PaletteEventEditor extends EbHackModule implements ActionListener {
	/**
	 * @param rom
	 * @param prefs
	 */
	public PaletteEventEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	public static final int POINTER_TABLE = 0x2f12fb;
	public static final int palPointerBase = 0x1a0200;
	public static final String[] ANIM_STYLE_NAMES = new String[] {
			"None",
			"Fire Spring",
			"Moonside",
			"Sea of Eden",
			"Lumine Hall",
			"Stonehenge Base",
			"Chaos Theater",
			"Stonehenge Shut Down",
			"Stonehenge Cave"
	};
	
	private static EventPalette[] eventPals = 
		new EventPalette[MapEditor.mapTsetNum];
	
	private JComboBox chooser, palChooser, spritePal, animStyle;
	private JTextField flag, pointer;
	private SpritePalette sp;
	
	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init() {
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(
				new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JPanel panel = new JPanel();
		chooser = new JComboBox();
		for (int i = 0; i < MapEditor.mapTsetNum; i++)
			chooser.addItem(getNumberedString(
						TileEditor.TILESET_NAMES[EbMap.getDrawTileset(i)], i));
		chooser.addActionListener(this);
		panel.add(new JLabel("Tileset:"));
		panel.add(chooser);
		
		panel.add(new JLabel("Subpalette:"));
		panel.add(palChooser = HackModule.createJComboBoxFromArray(new Object[6]));
		palChooser.addActionListener(this);
		mainPanel.add(panel);
		
		panel = new JPanel();
		panel.setLayout(
				new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(HackModule.getLabeledComponent("Event Flag:",
				flag = HackModule.createSizedJTextField(3, true, true)));
		panel.add(HackModule.getLabeledComponent("Unknown Pointer:",
				pointer = HackModule.createSizedJTextField(4, true, true)));
		String[] descriptions = new String[7];
		descriptions[0] = "None";
		for (int i = 1; i < 7; i++)
			descriptions[i] = "Subpalette " + (i - 1);
		panel.add(HackModule.getLabeledComponent("Sprite Palette:",
				spritePal = HackModule.createJComboBoxFromArray(descriptions)));
		panel.add(HackModule.getLabeledComponent("Animation Style:",
				animStyle = HackModule.createJComboBoxFromArray(ANIM_STYLE_NAMES, true)));
		mainPanel.add(panel);
		
		sp = new SpritePalette();
		sp.addActionListener(this);
		mainPanel.add(sp);
		
		mainWindow.getContentPane().add(mainPanel, BorderLayout.NORTH);
		
		mainWindow.pack();
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion() {
		return "0.1";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription() {
		return "Palette Event Editor";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits() {
		return "Written by Mr. Tenda";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide() {
		mainWindow.setVisible(false);
	}
	
	public void show() {
		readFromRom();
		super.show();
		if ((chooser.getSelectedIndex() < 0) || (palChooser.getSelectedIndex() < 0)) {
			chooser.removeActionListener(this);
			chooser.setSelectedIndex(0);
			chooser.addActionListener(this);
			palChooser.setSelectedIndex(0);
		}
		mainWindow.setVisible(true);
	}
	
	public void readFromRom() {
		EbMap.loadDrawTilesets(rom);
		readFromRom(rom);
	}
	
	public static void readFromRom(AbstractRom rom) {
		Color[][] palettes;
		byte[] data = new byte[6 * 32];
		for (int i = 0; i < eventPals.length; i++) {
			data = rom.readByte(toRegPointer(rom.readMulti(POINTER_TABLE + 4 * i, 4)),
					data.length);
			palettes = new Color[6][16];
			for (int j = 0; j < palettes.length; j++)
				palettes[j] = HackModule.readPalette(data,
						j * palettes[j].length * 2,
						palettes[j].length);
			eventPals[i] = new EventPalette(palettes,
					(short) (data[0] + data[1] * 0x100),
					(short) (data[32] + data[33] * 0x100),
					data[64],
					data[96]);
		}
	}
	
	public static Color[][] getPalette(int tileset) {
		return eventPals[tileset].getPalette();
	}
	
	public static class EventPalette {
		private int number;
		private short flag, pointer;
		private byte spritePal, animStyle;
		private Color[][] palettes;
		
		public EventPalette(Color[][] palettes, short flag,
				short pointer, byte spritePal, byte animStyle) {
			this.palettes = palettes;
			this.flag = flag;
			this.pointer = pointer;
			this.spritePal = spritePal;
			this.animStyle = animStyle;
		}
		
		public Color[][] getPalette() {
			return palettes;
		}
		
		public Color[] getPalette(int num) {
			return palettes[num];
		}
		
		public short getFlag() {
			return flag;
		}
		
		public short getPointer() {
			return pointer;
		}
		
		public byte getSpritePal() {
			return spritePal;
		}
		
		public byte getAnimStyle() {
			return animStyle;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(chooser) || e.getSource().equals(palChooser)) {
			sp.setPalette(eventPals[chooser.getSelectedIndex()].getPalette(
					palChooser.getSelectedIndex()));
			sp.repaint();
			
			flag.setText(Integer.toHexString(
					eventPals[chooser.getSelectedIndex()].getFlag() & 0xffff));
			pointer.setText(Integer.toHexString(
					eventPals[chooser.getSelectedIndex()].getPointer() & 0xffff));
			spritePal.setSelectedIndex(eventPals[chooser.getSelectedIndex()].getSpritePal());
			animStyle.setSelectedIndex(eventPals[chooser.getSelectedIndex()].getAnimStyle());
		}
	}
}