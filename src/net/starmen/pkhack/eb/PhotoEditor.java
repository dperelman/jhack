package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.MapEditor.EbMap;

/**
 * @author Mr. Tenda
 *
 * TODO Write javadoc for this class
 */
public class PhotoEditor extends EbHackModule 
	implements ActionListener, DocumentListener, SeekListener
{

	/**
	 * @param rom
	 * @param prefs
	 */
	public PhotoEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	public static final int photoPalsPointer = 0xa6d;
	public static final int NUM_PALETTES = 120; // TODO confirm this number
	
	private JButton centerSeek, refresh;
	private JComboBox entryChooser, direction, photoPaletteChooser;
	private JTextField flag, centerX, centerY, distance, landX, landY;
	private JComponent[][] party = new JComponent[6][3];
	private JComponent[][] extra = new JComponent[4][4];
	private static PhotoEntry[] entries;
	private PhotoPreview preview;
	private SpritePalette sp;
	private static Color[][] palettes;
	private int movingSprite = -1;
	private static int palLen;
	private boolean muteEvents = false;

	protected void init()
	{
		SpriteCharacterTableEditor.readFromRom(rom);
		readFromRom(this);
		
		String[] palNames = new String[NUM_PALETTES];
		for (int i = 0; i < NUM_PALETTES; i++)
			palNames[i] = "Palette #" + i;
		
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		
		JPanel topPanel = new JPanel(new FlowLayout());
		topPanel.add(new JLabel("Entry: "));
		String[] entryNames = new String[31];
		for (int i = 0; i < entryNames.length; i++)
			entryNames[i] = "Entry #" + i;
		entryChooser = new JComboBox(entryNames);
		entryChooser.addActionListener(this);
		topPanel.add(entryChooser);
		mainWindow.getContentPane().add(
				topPanel, BorderLayout.NORTH);
		
		JPanel panel = new JPanel(new FlowLayout());
		
		JPanel tmpPanel = new JPanel();
		tmpPanel.setLayout(
				new BoxLayout(tmpPanel, BoxLayout.Y_AXIS));
		
		JPanel tmpPanel2 = new JPanel();
		tmpPanel2.setLayout(
				new BoxLayout(tmpPanel2, BoxLayout.Y_AXIS));
		for (int i = 0; i < party.length; i++)
		{
			for (int j = 0; j < party[i].length; j++)
			{
				String description;
				if (j == 0)
				{
					description = "Enable Party Member #" + (i + 1);
					JCheckBox cbox = new JCheckBox();
					cbox.addActionListener(this);
					party[i][j] = cbox;
				}
				else
				{
					if (j == 1)
						description = "X: ";
					else
						description = "Y: ";
					JTextField text =
						HackModule.createSizedJTextField(4, true);
					text.getDocument().addDocumentListener(this);
					party[i][j] = text;
				}
				tmpPanel2.add(
						HackModule.getLabeledComponent(
								description, party[i][j]));
			}
		}
		
		JPanel tmpPanel3 = new JPanel();
		tmpPanel3.setLayout(
				new BoxLayout(tmpPanel3, BoxLayout.Y_AXIS));
		SpriteEditor.initSptNames(rom.getPath());
		for (int i = 0; i < extra.length; i++)
		{
			for (int j = 0; j < extra[i].length; j++)
			{
				String description;
				if (j == 0)
				{
					JCheckBox cbox = new JCheckBox();
					cbox.addActionListener(this);
					extra[i][j] = cbox;
					description = "Enable Extra Sprite #" + (i + 1);
				}
				else if (j == 1)
				{
					JComboBox cbox = new JComboBox(sptNames);
					cbox.addActionListener(this);
					extra[i][j] = cbox;
					description = "Sprite: ";
				}
				else
				{
					if (j == 2)
						description = "X: ";
					else
						description = "Y: ";
					JTextField text =
						HackModule.createSizedJTextField(4, true);
					text.getDocument().addDocumentListener(this);
					extra[i][j] = text;
				}
				tmpPanel3.add(
						HackModule.getLabeledComponent(
								description, extra[i][j]));
			}
		}
		
		photoPaletteChooser = new JComboBox(palNames);
		landX = HackModule.createSizedJTextField(4, true);
		landY = HackModule.createSizedJTextField(4, true);
		
		sp = new SpritePalette(2, 20, 16);
		preview = new PhotoPreview(this, party, extra, photoPaletteChooser,
				landX, landY);
		PhotoPreviewListener ears = new PhotoPreviewListener();
		preview.addMouseListener(ears);
		preview.addMouseMotionListener(ears);
		preview.setPreferredSize(
				new Dimension(
						preview.getPreviewWidth() 
							* MapEditor.tileWidth + 2,
						preview.getPreviewHeight() 
							* MapEditor.tileHeight + 2));
				
				
		tmpPanel.add(HackModule.pairComponents(sp,preview,true));
		flag = HackModule.createSizedJTextField(5, false);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Event Flag: ", flag));
		DocumentListener centerListener =
			new DocumentListener()
			{
				public void changedUpdate(DocumentEvent e)
				{
					if ((centerX.getText().length() > 0)
							&& (centerY.getText().length() > 0))
					{
						preview.setXY(
								Integer.parseInt(centerX.getText()),
								Integer.parseInt(centerY.getText()));
						preview.remoteRepaint();
					}
				}
				
				public void insertUpdate(DocumentEvent e)
				{
					changedUpdate(e);
				}

				public void removeUpdate(DocumentEvent e)
				{
					changedUpdate(e);
				}
			};
		refresh = new JButton("Refresh");
		refresh.addActionListener(this);
		tmpPanel.add(refresh);
		centerSeek = new JButton("Find on Map");
		centerSeek.addActionListener(this);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Center of Picture: ", centerSeek));
		centerX = HackModule.createSizedJTextField(4, true);
		centerX.getDocument().addDocumentListener(centerListener);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Center of Picture (X): ", centerX));
		centerY = HackModule.createSizedJTextField(4, true);
		centerY.getDocument().addDocumentListener(centerListener);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Center of Picture (Y): ", centerY));
		photoPaletteChooser.addActionListener(this);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Palette: ", photoPaletteChooser));
		direction = new JComboBox(
				new String[] {
						"Down", "45 Degree Down and Left",
						"Left", "45 Degree Up and Left",
						"Up", "45 Degree Up and Right",
						"Right", "45 Degree Down and Right"
				});
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Photo Movement: ", direction));
		distance = HackModule.createSizedJTextField(3, true);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Photo Movement Distance: ", distance));
		landX.getDocument().addDocumentListener(this);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Photoman Landing Location (X): ", landX));
		landY.getDocument().addDocumentListener(this);
		tmpPanel.add(
				HackModule.getLabeledComponent(
						"Photoman Landing Location (Y): ", landY));
		tmpPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.add(tmpPanel);
		
		panel.add(tmpPanel2);
		panel.add(tmpPanel3);
		
		mainWindow.getContentPane().add(panel, BorderLayout.CENTER);
		mainWindow.pack();
	}

	public String getVersion()
	{
		return "0.1";
	}

	public String getDescription()
	{
		return "Photo Editor";
	}

	public String getCredits()
	{
		return "Written by Mr. Tenda";
	}
	
	public void show()
	{
		super.show();
		
		TPTEditor.readFromRom(this);
		SpriteEditor.readFromRom(rom);
		if (entryChooser.getSelectedIndex() >= 0)
			updateComponents();
		else
			entryChooser.setSelectedIndex(0);
		
		mainWindow.setVisible(true);
	}

	public void hide()
	{
		mainWindow.hide();
	}
	
	public static void readFromRom(HackModule hm)
	{
		int[] palPointers = new int[NUM_PALETTES];
		readPalettes(hm, palPointers, true);
		
		AbstractRom rom = hm.rom;
		entries = new PhotoEntry[32];
		for (int i = 0; i < entries.length; i++)
		{
			int address = 0x21318A + (i * 62);
			short flag = (short) rom.readMulti(address, 2);
			short centerX = (short) rom.readMulti(address + 2, 2);
			short centerY = (short) rom.readMulti(address + 4, 2);
			short palette = (short) rom.readMulti(address + 6, 2);
			int palNum = -1;
			for (int j = 0; j < palPointers.length; j++)
				if ((palette & 0xffff) == palPointers[j])
					palNum = j;
			if (palNum == -1)
				System.out.println("Error getting palette number");
			byte direction = (byte) (rom.readByte(address + 8) / 0x8);
			byte distance = rom.readByte(address + 9);
			short landX = (short) rom.readMulti(address + 10, 2);
			short landY = (short) rom.readMulti(address + 12, 2);
			
			short[][] partyXY = new short[6][2];
			for (int j = 0; j < partyXY.length; j++)
			{
				partyXY[j][0] = (short) 
					rom.readMulti(address + 14 + (j * 4), 2);
				partyXY[j][1] = (short) 
					rom.readMulti(address + 16 + (j * 4), 2);
			}
			short[][] extraSprites = new short[4][3];
			for (int j = 0; j < extraSprites.length; j++)
			{
				extraSprites[j][0] = (short)
					rom.readMulti(address + 38 + (j * 6), 2);
				extraSprites[j][1] = (short)
					rom.readMulti(address + 40 + (j * 6), 2);
				extraSprites[j][2] = (short)
					rom.readMulti(address + 42 + (j * 6), 2);
			}
			
			entries[i] = new PhotoEntry(flag, centerX, centerY,
					palNum, direction, distance, landX, landY,
					partyXY, extraSprites);
		}
	}
	
	private static boolean readPalettes(HackModule hm, int[] palPointers,
			boolean allowFailure)
	{
		AbstractRom r = hm.rom;
		
		palettes = new Color[NUM_PALETTES][16];
        byte[] palBuffer = new byte[4300];
        /*** DECOMPRESS PALETTES ** */
        System.out.println("About to attempt decompressing "
        	+ palBuffer.length + " bytes of photo palettes.");
        int[] tmp = EbHackModule.decomp(r.readRegAsmPointer(photoPalsPointer), palBuffer, r);
        if (tmp[0] < 0)
        {
            System.out.println("Error " + tmp[0]
                + " decompressing photo palettes.");
            if (allowFailure)
            {
            	Arrays.fill(palettes, Color.BLACK); // EMPTY PALETTES
            	palLen = 0;
            }
            else
            	return false;
        }
        else
        {
        	palLen = tmp[1];
            System.out.println("Photo palettes: Decompressed " + tmp[0]
                + " bytes from a " + tmp[1] + " byte compressed block.");

            int palOffset = 0;
            for (int i = 0; i < NUM_PALETTES; i++)
            {
            	palPointers[i] = palOffset;
                HackModule.readPalette(palBuffer, palOffset, palettes[i]);
                palOffset += palettes[i].length * 2;
            }
        }
        return true;
	}
	
	public void writeToRom()
	{
		int palPointer = 0;
		writePalettes(this, palPointer, photoPaletteChooser.getSelectedIndex());
		
		PhotoEntry photo = entries[entryChooser.getSelectedIndex()];
		photo.setFlag((short) Integer.parseInt(flag.getText(),16));
		photo.setCenterX((short) Integer.parseInt(centerX.getText()));
		photo.setCenterY((short) Integer.parseInt(centerY.getText()));
		photo.setPalette(photoPaletteChooser.getSelectedIndex());
		photo.setLandX((short) Integer.parseInt(landX.getText()));
		photo.setLandY((short) Integer.parseInt(landY.getText()));
		photo.setDirection((byte) direction.getSelectedIndex());
		photo.setDistance((byte) Integer.parseInt(distance.getText()));
		for (int i = 0; i < party.length; i++)
		{
			if (((JCheckBox) party[i][0]).isSelected()) 
				photo.setPartyXY(i,
						(short) Integer.parseInt(((JTextField) party[i][1]).getText()),
						(short) Integer.parseInt(((JTextField) party[i][2]).getText()));
			else
				photo.setPartyXY(i,(short) 0,(short) 0);
		}
		for (int i = 0; i < extra.length; i++)
		{
			if (((JCheckBox) extra[i][0]).isSelected())
			{
				photo.setExtraSprite(i,
						(short) ((JComboBox) extra[i][1]).getSelectedIndex());
				photo.setExtraXY(i,
						(short) Integer.parseInt(((JTextField) extra[i][2]).getText()),
						(short) Integer.parseInt(((JTextField) extra[i][3]).getText()));
			}
			else
			{
				photo.setExtraSprite(i, (short) 0);
				photo.setExtraXY(i, (short) 0, (short) 0);
			}
		}
		
		rom.write(0x21318A + (entryChooser.getSelectedIndex() * 62),
				photo.toByteArray(palPointer));
	}
	
	public static boolean writePalettes(HackModule hm, int palPointer, int palNum)
	{
        byte[] udataPal = new byte[4300];
        int palOff = 0;
        /* COMPRESS PALETTE */
        for (int i = 0; i < NUM_PALETTES; i++)
        {
            HackModule.writePalette(udataPal, palOff, palettes[i]);
            palOff += palettes[i].length * 2;
        }

        byte[] compPal;
        int palCompLen = comp(udataPal, compPal = new byte[2800]);
        if (!hm.writeToFreeASMLink(compPal, photoPalsPointer, palLen,
            palCompLen))
            return false;
        System.out.println("Wrote "
            + (palLen = palCompLen)
            + " bytes of the photo palettes at "
            + Integer.toHexString(palPointer = hm.rom
                .readRegAsmPointer(photoPalsPointer)) + " to "
            + Integer.toHexString(palPointer + palCompLen - 1) + ".");
        return true;
	}
	
	public void updateComponents()
	{
		muteEvents = true;
		PhotoEntry photo = entries[entryChooser.getSelectedIndex()];
		direction.setSelectedIndex(photo.getDirection());
		flag.setText(Integer.toString(photo.getFlag(),16));
		centerX.setText(Integer.toString(photo.getCenterX()));
		centerY.setText(Integer.toString(photo.getCenterY()));
		photoPaletteChooser.setSelectedIndex(photo.getPalette());
		distance.setText(Integer.toString(photo.getDistance()));
		landX.setText(Integer.toString(photo.getLandX()));
		landY.setText(Integer.toString(photo.getLandY()));
		
		preview.setXY(photo.getCenterX(), photo.getCenterY());
		
		for (int i = 0; i < party.length; i++)
		{
			if ((photo.getPartyX(i) > 0) && (photo.getPartyY(i) > 0))
			{
				((JCheckBox) party[i][0]).setSelected(true);
				((JTextField) party[i][1]).setEditable(true);
				((JTextField) party[i][2]).setEditable(true);
			}
			else
			{
				((JCheckBox) party[i][0]).setSelected(false);
				((JTextField) party[i][1]).setEditable(false);
				((JTextField) party[i][2]).setEditable(false);
			}
			((JTextField) party[i][1]).setText(
					Integer.toString(photo.getPartyX(i)));
			((JTextField) party[i][2]).setText(
					Integer.toString(photo.getPartyY(i)));
		}
		
		for (int i = 0; i < extra.length; i++)
		{
			if ((photo.getExtraX(i) > 0) && (photo.getExtraY(i) > 0))
			{
				((JCheckBox) extra[i][0]).setSelected(true);
				((JComboBox) extra[i][1]).setEnabled(true);
				((JTextField) extra[i][2]).setEditable(true);
				((JTextField) extra[i][3]).setEditable(true);
			}
			else
			{
				((JCheckBox) extra[i][0]).setSelected(false);
				((JComboBox) extra[i][1]).setEnabled(false);
				((JTextField) extra[i][2]).setEditable(false);
				((JTextField) extra[i][3]).setEditable(false);
			}
			((JComboBox) extra[i][1]).setSelectedIndex(
					photo.getExtraSprite(i));
			((JTextField) extra[i][2]).setText(
					Integer.toString(photo.getExtraX(i)));
			((JTextField) extra[i][3]).setText(
					Integer.toString(photo.getExtraY(i)));
		}
		
		preview.remoteRepaint();
		muteEvents = false;
	}
	
	public void updatePaletteDisplay()
	{
		muteEvents = true;
		sp.setPalette(palettes[photoPaletteChooser.getSelectedIndex()]);
		sp.repaint();
		muteEvents = false;
	}
	
	public void returnSeek(int x, int y, int tileX, int tileY) {
		int centerXnum = 
			((tileX * MapEditor.tileWidth) / 8) + (x / 8);
		int centerYnum = 
			((tileY * MapEditor.tileWidth) / 8) + (y / 8);
		centerX.setText(Integer.toString(centerXnum));
		centerY.setText(Integer.toString(centerYnum));
		centerSeek.setEnabled(true);
		preview.remoteRepaint();
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals("apply"))
			writeToRom();
		else if (ae.getActionCommand().equals("close"))
			hide();
		else if (ae.getSource().equals(entryChooser))
			updateComponents();
		else if (ae.getSource().equals(photoPaletteChooser))
			updatePaletteDisplay();
		else if (ae.getSource().equals(centerSeek))
			net.starmen.pkhack.JHack.main.showModule(
        			MapEditor.class, this);
		else if (ae.getSource().equals(refresh))
			preview.remoteRepaint();
		else if (ae.getSource() instanceof JCheckBox)
		{
			for (int i = 0; i < party.length; i++)
				if (ae.getSource().equals(party[i][0]))
				{
					JCheckBox cbox = (JCheckBox) party[i][0];
					((JTextField) party[i][1]).setEditable(
							cbox.isSelected());
					((JTextField) party[i][2]).setEditable(
							cbox.isSelected());
					preview.remoteRepaint();
					return;
				}
					
			
			for (int i = 0; i < extra.length; i++)
				if (ae.getSource().equals(extra[i][0]))
				{
					JCheckBox cbox = (JCheckBox) extra[i][0];
					((JComboBox) extra[i][1]).setEnabled(
							cbox.isSelected());
					((JTextField) extra[i][2]).setEditable(
							cbox.isSelected());
					((JTextField) extra[i][3]).setEditable(
							cbox.isSelected());
					preview.remoteRepaint();
					return;
				}
		}
		else if (ae.getSource() instanceof JComboBox)
			preview.remoteRepaint();
	}

	public void changedUpdate(DocumentEvent e)
	{
		if ((e.getDocument().getLength() > 0) && !muteEvents)
			preview.remoteRepaint();
	}

	public void insertUpdate(DocumentEvent e)
	{
		changedUpdate(e);
	}

	public void removeUpdate(DocumentEvent e)
	{
		changedUpdate(e);
	}
	
	public static class PhotoEntry
	{
		private int palette;
		private short flag, centerX, centerY, landX, landY;
		private short[][] partyXY, extraSprites;
		private byte direction, distance;
		
		public PhotoEntry(short flag, short centerX, short centerY,
				int palette, byte direction, byte distance,
				short landX, short landY, short[][] partyXY,
				short[][] extraSprites)
		{
			this.flag = flag;
			this.centerX = centerX;
			this.centerY = centerY;
			this.palette = palette;
			this.direction = direction;
			this.distance = distance;
			this.landX = landX;
			this.landY = landY;
			this.partyXY = partyXY;
			this.extraSprites = extraSprites;
		}
		
		public short getFlag()
		{
			return flag;
		}
		
		public void setFlag(short flag)
		{
			this.flag = flag;
		}
		
		public short getCenterX()
		{
			return centerX;
		}
		
		public void setCenterX(short centerX)
		{
			this.centerX = centerX;
		}
		
		public short getCenterY()
		{
			return centerY;
		}
		
		public void setCenterY(short centerY)
		{
			this.centerY = centerY;
		}
		
		public int getPalette()
		{
			return palette;
		}
		
		public void setPalette(int palette)
		{
			this.palette = palette;
		}
		
		public short getLandX()
		{
			return landX;
		}
		
		public void setLandX(short landX)
		{
			this.landX = landX;
		}
		
		public short getLandY()
		{
			return landY;
		}
		
		public void setLandY(short landY)
		{
			this.landY = landY;
		}
		
		public byte getDirection()
		{
			return direction;
		}
		
		public void setDirection(byte direction)
		{
			this.direction = direction;
		}
		
		public byte getDistance()
		{
			return distance;
		}
		
		public void setDistance(byte distance)
		{
			this.distance = distance;
		}
		
		public short getParty(int num, int subNum)
		{
			return partyXY[num][subNum];
		}
		
		public short getPartyX(int num)
		{
			return partyXY[num][0];
		}
		
		public short getPartyY(int num)
		{
			return partyXY[num][1];
		}
		
		public void setPartyXY(int num, short x, short y)
		{
			partyXY[num][0] = x;
			partyXY[num][1] = y;
		}
		
		public short getExtra(int num, int subNum)
		{
			return extraSprites[num][subNum];
		}
		
		public short getExtraSprite(int num)
		{
			return extraSprites[num][2];
		}
		
		public void setExtraSprite(int num, short sprite)
		{
			extraSprites[num][2] = sprite;
		}
		
		public short getExtraX(int num)
		{
			return extraSprites[num][0];
		}
		
		public short getExtraY(int num)
		{
			return extraSprites[num][1];
		}
		
		public void setExtraXY(int num, short x, short y)
		{
			extraSprites[num][0] = x;
			extraSprites[num][1] = y;
		}
		
		public byte[] toByteArray(int palPointer)
		{
			byte[] byteArray = new byte[62];
			byteArray[0] = (byte) (flag & 0xff);
			byteArray[1] = (byte) ((flag & 0xff00) / 0x100);
			byteArray[2] = (byte) (centerX & 0xff);
			byteArray[3] = (byte) ((centerX & 0xff00) / 0x100);
			byteArray[4] = (byte) (centerY & 0xff);
			byteArray[5] = (byte) ((centerY & 0xff00) / 0x100);
			byteArray[6] = (byte) (palPointer & 0xff);
			byteArray[7] = (byte) ((palPointer & 0xff00) / 0x100);
			byteArray[8] = direction;
			byteArray[9] = distance;
			byteArray[10] = (byte) (landX & 0xff);
			byteArray[11] = (byte) ((landX & 0xff00) / 0x100);
			byteArray[12] = (byte) (landY & 0xff);
			byteArray[13] = (byte) ((landY & 0xff00) / 0x100);
			for (int i = 0; i < partyXY.length; i++)
			{
				byteArray[14 + (i * 4)] =
					(byte) (partyXY[i][0] & 0xff);
				byteArray[15 + (i * 4)] =
					(byte) ((partyXY[i][0] & 0xff00) / 0x100);
				byteArray[16 + (i * 4)] =
					(byte) (partyXY[i][1] & 0xff);
				byteArray[17 + (i * 4)] =
					(byte) ((partyXY[i][1] & 0xff00) / 0x100);
			}
			for (int i = 0; i < extraSprites.length; i++)
			{
				byteArray[38 + (i * 6)] =
					(byte) (extraSprites[i][0] & 0xff);
				byteArray[39 + (i * 6)] =
					(byte) ((extraSprites[i][0] & 0xff00) / 0x100);
				byteArray[40 + (i * 6)] =
					(byte) (extraSprites[i][1] & 0xff);
				byteArray[41 + (i * 6)] =
					(byte) ((extraSprites[i][1] & 0xff00) / 0x100);
				byteArray[42 + (i * 6)] =
					(byte) (extraSprites[i][2] & 0xff);
				byteArray[43 + (i * 6)] =
					(byte) ((extraSprites[i][2] & 0xff00) / 0x100);
			}
			
			return byteArray;
		}
	}

	public class PhotoPreview extends AbstractButton
    {
		private HackModule hm;
        private int[][] mapArray;
        private boolean knowsMap = false, showPhotoman = true;
        private int centerX, centerY, tileX, tileY;
    	private int photoHeight = 20, photoWidth = 24,
			previewHeight = 10, previewWidth = 10,
			movingSprite = -1, movingSpriteX,
			movingSpriteY;
    	private JComponent[][] party, extra;
    	private JTextField landX, landY;
    	private JComboBox palette;
    	
    	public PhotoPreview(HackModule hm,
    			JComponent[][] party,
    			JComponent[][] extra,
				JComboBox palette,
				JTextField landX,
				JTextField landY)
    	{
    		this.hm = hm;
    		this.party = party;
    		this.extra = extra;
    		this.palette = palette;
    		this.landX = landX;
    		this.landY = landY;
    	}
    	
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            drawBorder(g2d);
            if (knowsMap)
            	drawMap(g, g2d);
        }
        
        private void drawBorder(Graphics2D g2d)
        {
        	g2d.draw(new Rectangle2D.Double(
        			0,0,
					previewWidth * MapEditor.tileWidth + 1,
					previewHeight * MapEditor.tileHeight + 1)); 
        }

        private void drawMap(Graphics g, Graphics2D g2d)
        {
        	int tile_set, tile_tile, tile_pal;
            for (int i = 0; i < mapArray.length; i++)
            {
            	int sectorY = (tileY + i) / MapEditor.sectorHeight;
                for (int j = 0; j < mapArray[i].length; j++)
                {
                	int sectorX = (tileX + j) / MapEditor.sectorWidth;
                	if (! MapEditor.EbMap.isSectorDataLoaded(
                			sectorX, sectorY))
                		MapEditor.EbMap.loadSectorData(
                				rom, sectorX, sectorY);
                	
                    tile_set = MapEditor.EbMap.getDrawTileset(
                    		MapEditor.EbMap.getTset(sectorX, sectorY));
                    tile_tile = mapArray[i][j]
                        | (MapEditor.EbMap.getLocalTileset(rom,
                        		tileX + j,
								tileY + i) << 8);
                    
                    // TODO Have it use the photo's palette
                    tile_pal = 
                    	TileEditor.tilesets[tile_set].getPaletteNum(
                    			EbMap.getTset(sectorX, sectorY),
								EbMap.getPal(sectorX, sectorY));
                    
                    MapEditor.EbMap.loadTileImage(tile_set, tile_tile, tile_pal);
                    g.drawImage(
                    	MapEditor.EbMap.getTileImage(tile_set,tile_tile,tile_pal),
                        j * MapEditor.tileWidth + 1, i * MapEditor.tileHeight + 1,
						MapEditor.tileWidth, MapEditor.tileHeight, this);
                }
            }
            
            for (int k = 0; k < preview.getPreviewHeight(); k++)
        	{
        		if ((((tileY + k) % (MapEditor.sectorHeight * 2)) == 0)
        				|| (k == 0))
        		{
        			for (int i = 0; i < preview.getPreviewWidth(); i++)
                	{
                		if ((((tileX + i) % MapEditor.sectorWidth) == 0)
                				|| (i == 0))
                		{
                			if (! EbMap.isSpriteDataLoaded(
                					(tileX + i) / MapEditor.sectorWidth,
									(tileY + k) / (MapEditor.sectorHeight * 2)))
                			{
                				EbMap.loadSpriteData(rom,
                						(tileX + i) / MapEditor.sectorWidth,
										(tileY + k) / (MapEditor.sectorHeight * 2));
                			}
                			int spritesNum = EbMap.getSpritesNum(
                					(tileX + i) / MapEditor.sectorWidth,
									(tileY + k) / (MapEditor.sectorHeight * 2));
                        	short[][] spriteLocs = EbMap.getSpriteLocs(
                        			(tileX + i) / MapEditor.sectorWidth,
									(tileY + k) / (MapEditor.sectorHeight * 2));
                        	short[] spriteTpts = EbMap.getSpriteTpts(
                        			(tileX + i) / MapEditor.sectorWidth, (tileY + k) / (MapEditor.sectorHeight * 2));
                        	// this.spriteLocs = new int[spriteData[0]][5];
                            for (int j = 0; j < spritesNum; j++)
                            {
                            	TPTEditor.TPTEntry tptEntry = 
                            		TPTEditor.tptEntries[spriteTpts[j]];
                            	int spriteNum = tptEntry.getSprite();
                            	int spriteDrawY = spriteLocs[j][1];
                            	int spriteDrawX = spriteLocs[j][0];
                            	EbMap.loadSpriteImage(hm,
                            			spriteNum, tptEntry.getDirection());
                            	SpriteEditor.SpriteInfoBlock sib =
                            		SpriteEditor.sib[spriteNum];
                            	
                            	if (((tileY + k) % (MapEditor.sectorHeight * 2)) > 0)
                            	{
                            		spriteDrawY -= ((tileY + k) % (MapEditor.sectorHeight * 2)) *
											MapEditor.tileHeight;
                            	}
                            	
                            	if (((tileX + i) % MapEditor.sectorWidth) > 0)
                            	{
                            		spriteDrawX -= ((tileX + i) % MapEditor.sectorWidth)
											* MapEditor.tileWidth;
                            	}
                            	
                            	g.drawImage(
                            			EbMap.getSpriteImage(
                            					spriteNum,tptEntry.getDirection()),
            							spriteDrawX + (i * MapEditor.tileWidth),
    									spriteDrawY + (k * MapEditor.tileHeight),
										this);
                            	/*if (spriteBoxes)
                            	{
                            		g2d.setPaint(Color.red);
                            		g2d.draw(new Rectangle2D.Double(
                                			spriteDrawX + (i * MapEditor.tileWidth) - 1,
											spriteDrawY + (k * MapEditor.tileHeight) - 1,
											EbMap.getSpriteImage(
													spriteNum,tptEntry.getDirection())
																	.getWidth(this) + 1,
											EbMap.getSpriteImage(
													spriteNum,tptEntry.getDirection())
																	.getHeight(this) + 1));
                            	}*/
                            }
                		}
                	}
        		}
        	}
            
            g2d.setPaint(Color.yellow);
            g2d.draw(new Line2D.Double(
            		(centerX * 8) - (tileX * MapEditor.tileWidth) - 10,
            		(centerY * 8) - (tileY * MapEditor.tileHeight),
					(centerX * 8) - (tileX * MapEditor.tileWidth) + 10,
            		(centerY * 8) - (tileY * MapEditor.tileHeight)));
            g2d.draw(new Line2D.Double(
            		(centerX * 8) - (tileX * MapEditor.tileWidth),
            		(centerY * 8) - (tileY * MapEditor.tileHeight) - 10,
					(centerX * 8) - (tileX * MapEditor.tileWidth),
            		(centerY * 8) - (tileY * MapEditor.tileHeight) + 10));
        	g2d.draw(new Rectangle2D.Double(
        			(centerX * 8) - 96 - (tileX * MapEditor.tileWidth),
					(centerY * 8) - 80 - (tileY * MapEditor.tileHeight),
					192, 160));
        	
        	int firstX = -1, firstY = -1;
        	for (int i = 0; i < party.length; i++)
        	{
        		if (((JCheckBox) party[i][0]).isSelected())
        		{         		
            		SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[SpriteCharacterTableEditor.getPlayableSPT(i,0)];
            		
            		int x = (Integer.parseInt(((JTextField) party[i][1]).getText()) * 8)
            				- (tileX * MapEditor.tileWidth) - (sib.width * 4);
            		int y = (Integer.parseInt(((JTextField) party[i][2]).getText()) * 8)
            				- (tileY * MapEditor.tileHeight) - (sib.height * 6);
            		if (((JCheckBox) party[i][0]).isSelected()
            				&& (x >= 0) && (x <= previewWidth * MapEditor.tileWidth)
    						&& (y >= 0) && (y <= previewHeight * MapEditor.tileHeight))
            		{
            			if ((firstX < 0) && (firstY < 0))
            			{
            				firstX = x;
            				firstY = y;
            			}
            			EbMap.loadSpriteImage(hm, SpriteCharacterTableEditor.getPlayableSPT(i,0),4);
            			g.drawImage(
                    			EbMap.getSpriteImage(SpriteCharacterTableEditor.getPlayableSPT(i,0),4),x,y,this);
            			g2d.setPaint(Color.red);
                		g2d.draw(new Rectangle2D.Double(
                    			x - 1, y - 1,
								sib.width * 8 + 1,
								sib.height * 8 + 1));
            		}
        		}
        	}
        	
    		// Draw the photoman
    		if (showPhotoman)
    		{
    			SpriteEditor.SpriteInfoBlock sib =
            		SpriteEditor.sib[0x8f];
        		int manX = (Integer.parseInt((landX).getText()) * 8)
    				- (tileX * MapEditor.tileWidth) - (sib.width * 4);
        		int manY = (Integer.parseInt(landY.getText()) * 8)
    				- (tileY * MapEditor.tileHeight) - (sib.height * 6);
        		if ((manX >= 0) && (manX <= previewWidth * MapEditor.tileWidth)
    					&& (manY >= 0) && (manY <= previewHeight * MapEditor.tileHeight))
        		{
        			int direction;
        			if ((manY > firstY) && (manX > firstX))
        				if (manY - firstY > manX - firstX)
        					direction = 0; // face up
        				else
        					direction = 6; // face left
        			else if (manY > firstY)
        				if (manY - firstY > firstX - manX)
        					direction = 0; // face up 
        				else
        					direction = 2; // face right
        			else if (manX > firstX)
        				if (firstY - manY > manX - firstX)
        					direction = 4; // face down
        				else
        					direction = 6; //face left
        			else
        				if (firstY - manY > firstX - manX)
        					direction = 4; //face down
        				else
        					direction = 2; //face right
        			EbMap.loadSpriteImage(hm, 0x8f,direction);
        			g.drawImage(
        					EbMap.getSpriteImage(0x8f,direction),manX,manY,this);
        			g2d.setPaint(Color.blue);
        			g2d.draw(new Rectangle2D.Double(
                			manX - 1,
							manY - 1,
							sib.width * 8 + 1,
							sib.height * 8 + 1));
        		}
    		}
        	
        	for (int i = 0; i < extra.length; i++)
        	{
        		if (((JCheckBox) extra[i][0]).isSelected())
        		{
            		int spriteNum = ((JComboBox) extra[i][1]).getSelectedIndex();
            		SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spriteNum];
            		
            		int x = (Integer.parseInt(((JTextField) extra[i][2]).getText()) * 8)
            				- (tileX * MapEditor.tileWidth) - (sib.width * 4);
            		int y = (Integer.parseInt(((JTextField) extra[i][3]).getText()) * 8)
            				- (tileY * MapEditor.tileHeight) - (sib.height * 6);
            		if ((x >= 0) && (x <= previewWidth * MapEditor.tileWidth)
    						&& (y >= 0) && (y <= previewHeight * MapEditor.tileHeight))
            		{
            			EbMap.loadSpriteImage(hm, spriteNum,4);
            			g.drawImage(
                    			EbMap.getSpriteImage(spriteNum,4),x,y,this);
            			g2d.setPaint(Color.green);
            			g2d.draw(new Rectangle2D.Double(
                    			x - 1, y - 1,
								sib.width * 8 + 1,
								sib.height * 8 + 1));
            		}
        		}
        	}
        	
        	if (movingSprite > -1)
        		if (movingSprite < 6)
        		{
        			int spt = SpriteCharacterTableEditor.getPlayableSPT(movingSprite,0);
        			//SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[movingSprite];
                	EbMap.loadSpriteImage(hm,
                			SpriteCharacterTableEditor.getPlayableSPT(spt,0),4);
        			g.drawImage(
                			EbMap.getSpriteImage(spt,4),
        					movingSpriteX,
							movingSpriteY,this);
        			g2d.setPaint(Color.red);
            		g2d.draw(new Rectangle2D.Double(
                			movingSpriteX - 1,
							movingSpriteY - 1,
							EbMap.getSpriteImage(spt,4).getWidth(this) + 1,
							EbMap.getSpriteImage(spt,4).getHeight(this) + 1));
        		}
        		else if (movingSprite < 10)
        		{
        			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[movingSprite - 6];
                	EbMap.loadSpriteImage(hm,
                			((JComboBox) extra[movingSprite - 6][1]).getSelectedIndex(),4);
        			g.drawImage(
                			EbMap.getSpriteImage(
                					((JComboBox) extra[movingSprite - 6][1]).getSelectedIndex(),4),
        					movingSpriteX,
							movingSpriteY,this);
        			g2d.setPaint(Color.green);
            		g2d.draw(new Rectangle2D.Double(
                			movingSpriteX - 1,
							movingSpriteY - 1,
							sib.width * 8 + 1,
							sib.height * 8 + 1));
        		}
        		else
        		{
        			SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[movingSprite - 6];
                	EbMap.loadSpriteImage(hm,0x8f,4);
        			g.drawImage(
                			EbMap.getSpriteImage(0x8f,4),
        					movingSpriteX,
							movingSpriteY,this);
        			g2d.setPaint(Color.blue);
            		g2d.draw(new Rectangle2D.Double(
                			movingSpriteX - 1,
							movingSpriteY - 1,
							sib.width * 8 + 1,
							sib.height * 8 + 1));
        		}
        }
        
        public void placeMovingSprite(int movingSpriteX, int movingSpriteY)
        {
        	this.movingSpriteX = movingSpriteX;
        	this.movingSpriteY = movingSpriteY;
        }
        
        public void showPhotoman(boolean showPhotoman)
        {
        	this.showPhotoman = showPhotoman;
        }

        public void setMapArray(int[][] newMapArray)
        {
            this.knowsMap = true;
            this.mapArray = newMapArray;
        }
        
        public void setXY(int centerX, int centerY)
        {
        	this.centerX = centerX;
        	this.centerY = centerY;
            int[][] maparray =
            	new int[previewHeight][previewWidth];
            tileX = (centerX * 8) / MapEditor.tileWidth
				- (previewWidth / 2);
            if (tileX < 0)
            	tileX = 0;
            else if (tileX / MapEditor.tileWidth + previewWidth
            		> MapEditor.width)
            	tileX = MapEditor.width - previewWidth;
            for (int i = 0; i < previewHeight; i++)
            {
            	int y2 = i + ((centerY * 8) / MapEditor.tileHeight)
					- (previewHeight / 2);
                if (y2 < 0)
                	y2 = 0;
                maparray[i] = MapEditor.EbMap.getTiles(rom,
                		y2, tileX,
						previewWidth);
            }
            tileY = (centerY * 8) / MapEditor.tileHeight
				- (previewHeight / 2);
            if (tileY < 0)
            	tileY = 0;
            else if (tileY + previewHeight > MapEditor.height)
            	tileY = MapEditor.height - previewHeight;
            setMapArray(maparray);
        }
        
        public int getTileX()
        {
        	return tileX;
        }
        
        public int getTileY()
        {
        	return tileY;
        }
        
        public void clearMapArray()
        {
        	this.knowsMap = false;
        	this.mapArray = null;
        }
        
        public void remoteRepaint()
        {
            repaint();
        }
        
        public int getPhotoWidth()
        {
        	return photoWidth;
        }
        
        public int getPhotoHeight()
        {
        	return photoHeight;
        }
        
        public int getPreviewWidth()
        {
        	return previewWidth;
        }
        
        public int getPreviewHeight()
        {
        	return previewHeight;
        }
        
        public int getMovingSprite()
        {
        	return movingSprite;
        }
        
        public void setMovingSprite(int movingSprite)
        {
        	this.movingSprite = movingSprite;
        }
    }

	private class PhotoPreviewListener implements MouseListener,
		MouseMotionListener
	{

		public void mouseClicked(MouseEvent e)
		{
			
		}

		public void mouseEntered(MouseEvent e)
		{
			
		}

		public void mouseExited(MouseEvent e)
		{
			
		}
		

		public void mouseDragged(MouseEvent e)
		{
        	// Draw a moving sprite
        	if (preview.getMovingSprite() > -1)
        	{
        		preview.placeMovingSprite(
    					e.getX(), e.getY());
    			preview.remoteRepaint();
        	}
		}

		public void mousePressed(MouseEvent e)
		{
            if ((e.getButton() == 1)
					&& (movingSprite == -1))
            {
        		int mousex = e.getX();
        		int mousey = e.getY();
            	for (int i = 0; i < party.length; i++)
            	{
            		SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[SpriteCharacterTableEditor.getPlayableSPT(i,0)];
            		int x = (Integer.parseInt(((JTextField) party[i][1]).getText()) * 8)
    					- (preview.getTileX() * MapEditor.tileWidth)
						- (sib.width * 4);
            		int y = (Integer.parseInt(((JTextField) party[i][2]).getText()) * 8)
    					- (preview.getTileY() * MapEditor.tileHeight)
						- (sib.height * 6);
							
            		if ((e.getX() - x >= 0)
            				&& (e.getX() - x <= sib.width * 8)
							&& (e.getY() - y >= 0)
							&& (e.getY() - y <= sib.height * 8))
            		{
            			preview.setMovingSprite(i);
            			((JCheckBox) party[i][0]).setSelected(false);
            			((JTextField) party[i][1]).setEditable(false);
            			((JTextField) party[i][2]).setEditable(false);
            			preview.placeMovingSprite(
            					e.getX(), e.getY());
            			preview.remoteRepaint();
                		return;
            		}
            	}
            	for (int i = 0; i < extra.length; i++)
            	{
            		SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[((JComboBox) extra[i][1]).getSelectedIndex()];
            		int x = (Integer.parseInt(((JTextField) extra[i][2]).getText()) * 8)
    					- (preview.getTileX() * MapEditor.tileWidth)
						- (sib.width * 4);
            		int y = (Integer.parseInt(((JTextField) extra[i][3]).getText()) * 8)
    					- (preview.getTileY() * MapEditor.tileHeight)
						- (sib.height * 6);
							
            		if ((e.getX() - x >= 0)
            				&& (e.getX() - x <= sib.width * 8)
							&& (e.getY() - y >= 0)
							&& (e.getY() - y <= sib.height * 8))
            		{
            			preview.setMovingSprite(i + 6);
            			((JCheckBox) extra[i][0]).setSelected(false);
            			((JComboBox) extra[i][1]).setEnabled(false);
            			((JTextField) extra[i][2]).setEditable(false);
            			((JTextField) extra[i][3]).setEditable(false);
            			preview.placeMovingSprite(
            					e.getX(), e.getY());
            			preview.remoteRepaint();
                		return;
            		}
            	}
            	
            	SpriteEditor.SpriteInfoBlock sib =
            		SpriteEditor.sib[0x8f];
        		int x = (Integer.parseInt(landX.getText()) * 8)
					- (preview.getTileX() * MapEditor.tileWidth)
					- (sib.width * 4);
        		int y = (Integer.parseInt(landY.getText()) * 8)
					- (preview.getTileY() * MapEditor.tileHeight)
					- (sib.height * 6);
						
        		if ((e.getX() - x >= 0)
        				&& (e.getX() - x <= sib.width * 8)
						&& (e.getY() - y >= 0)
						&& (e.getY() - y <= sib.height * 8))
        		{
        			preview.setMovingSprite(10);
        			landX.setEditable(false);
        			landY.setEditable(false);
        			preview.showPhotoman(false);
        			preview.placeMovingSprite(
        					e.getX(), e.getY());
        			preview.remoteRepaint();
            		return;
        		}
            }
		}

		public void mouseReleased(MouseEvent e)
		{
			if (preview.getMovingSprite() > -1)
				if (preview.getMovingSprite() < 6)
				{
					SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[SpriteCharacterTableEditor.getPlayableSPT(
                				preview.getMovingSprite(),0)];
					((JCheckBox) party[preview.getMovingSprite()][0])
						.setSelected(true);
					((JTextField) party[preview.getMovingSprite()][1])
						.setEditable(true);
        			((JTextField) party[preview.getMovingSprite()][1])
						.setText(Integer.toString(
								(e.getX() / 8)
								+ ((preview.getTileX() * MapEditor.tileWidth) / 8)
								+ (sib.width / 2)));
        			((JTextField) party[preview.getMovingSprite()][2])
						.setEditable(true);
        			((JTextField) party[preview.getMovingSprite()][2])
						.setText(Integer.toString(
								(e.getY() / 8)
								+ ((preview.getTileY() * MapEditor.tileHeight) / 8)
								+ ((sib.height * 3) / 4)));
        			preview.setMovingSprite(-1);
        			preview.remoteRepaint();
				}
				else if (preview.getMovingSprite() < 10)
				{
					SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[((JComboBox) extra[preview.getMovingSprite() - 6][1])
										 .getSelectedIndex()];
					((JCheckBox) extra[preview.getMovingSprite() - 6][0])
						.setSelected(true);
					((JComboBox) extra[preview.getMovingSprite() - 6][1])
						.setEnabled(true);
					((JTextField) extra[preview.getMovingSprite() - 6][2])
						.setEditable(true);
        			((JTextField) extra[preview.getMovingSprite() - 6][2])
						.setText(Integer.toString(
								(e.getX() / 8)
								+ ((preview.getTileX() * MapEditor.tileWidth) / 8)
								+ (sib.width / 2)));
        			((JTextField) extra[preview.getMovingSprite() - 6][3])
						.setEditable(true);
        			((JTextField) extra[preview.getMovingSprite() - 6][3])
						.setText(Integer.toString(
								(e.getY() / 8)
								+ ((preview.getTileY() * MapEditor.tileHeight) / 8)
								+ ((sib.height * 3) / 4)));
        			preview.setMovingSprite(-1);
        			preview.remoteRepaint();
				}
				else
				{
					SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[0x8f];
					landX.setEditable(true);
        			landX.setText(Integer.toString(
								(e.getX() / 8)
								+ ((preview.getTileX() * MapEditor.tileWidth) / 8)
								+ (sib.width / 2)));
        			landY.setEditable(true);
        			landY.setText(Integer.toString(
								(e.getY() / 8)
								+ ((preview.getTileY() * MapEditor.tileHeight) / 8)
								+ ((sib.height * 3) / 4)));
        			preview.setMovingSprite(-1);
        			preview.showPhotoman(true);
        			preview.remoteRepaint();
				}
		}

		public void mouseMoved(MouseEvent e)
		{
			
		}
		
	}
}