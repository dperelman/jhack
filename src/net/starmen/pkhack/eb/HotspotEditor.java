/*
 * Created on Jan 8, 2005
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.MapEditor.MapGraphics;

/**
 * @author Mr. Tenda
 *
 * TODO write javadoc for this class
 */
public class HotspotEditor extends EbHackModule implements ActionListener, SeekListener
{
	/**
	 * @param rom
	 * @param prefs
	 */
	public HotspotEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	public static final int HOTSPOTS_ADDR = 0x15f4f3 + 8;
	public static final int NUM_HOTSPOTS = 55;
	private static Hotspot[] entries;
	
	private JComboBox entryChooser;
	private JButton seek1, seek2, goto1, goto2;
	private MapGraphics preview1, preview2;
	private boolean seeking1;

	protected void init()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		JPanel panel = new JPanel();
		panel.setLayout(
				new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		String[] entryNames = new String[NUM_HOTSPOTS];
		for (int i = 0; i < entryNames.length; i++)
			entryNames[i] = "Hotspot #" + i;
		entryChooser = new JComboBox(entryNames);
		entryChooser.addActionListener(this);
		panel.add(entryChooser);
		
		preview1 = new MapGraphics(this, 4, 3, 5, false, false, true, 8, true);
		preview1.setPreferredSize(new Dimension((MapEditor.tileWidth * preview1
	            .getScreenWidth()), (MapEditor.tileHeight * preview1
	            .getScreenHeight())));
		seek1 = new JButton("Seek");
		seek1.addActionListener(this);
		goto1 = new JButton("Go to");
		goto1.addActionListener(this);
		panel.add(new JLabel("Top-left point"));
		JPanel xyPanel1 = new JPanel();
		xyPanel1.add(new JLabel("X:"));
		xyPanel1.add(preview1.getXField());
		xyPanel1.add(new JLabel("Y:"));
		xyPanel1.add(preview1.getYField());
		panel.add(HackModule.pairComponents(
				preview1,
				HackModule.pairComponents(xyPanel1, 
						HackModule.pairComponents(goto1, seek1, false),
						false), true));
		
		preview2 = new MapGraphics(this, 4, 3, 5, false, false, true, 8, true);
		preview2.setPreferredSize(new Dimension((MapEditor.tileWidth * preview2
	            .getScreenWidth()), (MapEditor.tileHeight * preview2
	            .getScreenHeight())));
		seek2 = new JButton("Seek");
		seek2.addActionListener(this);
		goto2 = new JButton("Go to");
		goto2.addActionListener(this);
		panel.add(new JLabel("Bottom-right point"));
		JPanel xyPanel2 = new JPanel();
		xyPanel2.add(new JLabel("X:"));
		xyPanel2.add(preview2.getXField());
		xyPanel2.add(new JLabel("Y:"));
		xyPanel2.add(preview2.getYField());
		panel.add(HackModule.pairComponents(
				preview2,HackModule.pairComponents(xyPanel2,
						HackModule.pairComponents(goto2, seek2, false),
						false), true));
		
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
		return "Hotspot Editor";
	}

	public String getCredits()
	{
		return "Written by Mr. Tenda";
	}
	
	public void show()
	{
		super.show();
		readFromRom();
		mainWindow.setVisible(true);
		if (entryChooser.getSelectedIndex() >= 0)
			updateComponents();
		else
			entryChooser.setSelectedIndex(0);
	}
	
	public void show(Object obj)
	{
		super.show();
		mainWindow.setVisible(true);
		if (obj instanceof Integer)
			entryChooser.setSelectedIndex(((Integer) obj).intValue());
	}
	
	public void updateComponents()
	{
		Hotspot entry = entries[entryChooser.getSelectedIndex()];
		
		preview1.setPreviewBoxXY(entry.getX1() & 0xffff, entry.getY1() & 0xffff);
		preview1.setMapXY(entry.getX1() & 0xffff, entry.getY1() & 0xffff);
		
		preview2.setPreviewBoxXY(entry.getX2(), entry.getY2());
		preview2.setMapXY(entry.getX2(), entry.getY2());
	}
	
	public void saveInfo()
	{
		Hotspot entry = entries[entryChooser.getSelectedIndex()];
		entry.setX1((short) Integer.parseInt(preview1.getXField().getText()));
		entry.setY1((short) Integer.parseInt(preview1.getYField().getText()));
		entry.setX2((short) Integer.parseInt(preview2.getXField().getText()));
		entry.setY2((short) Integer.parseInt(preview2.getYField().getText()));
	}
	
	public static void writeToRom(HackModule hm)
	{
		for (int i = 0; i < NUM_HOTSPOTS; i++)
			hm.rom.write(HOTSPOTS_ADDR + (i * 8), entries[i].toByteArray(), 8);
	}

	public void hide()
	{
		mainWindow.setVisible(false);
	}
	
	public void reset()
	{
		entries = null;
	}
	
	public void readFromRom()
	{
		//readFromRom(this);
		
		EbMap.loadData(this, true, false, true);
	}
	
	public static void readFromRom(HackModule hm)
	{	
		if (entries == null)
		{
			entries = new Hotspot[NUM_HOTSPOTS];
			for (int i = 0; i < entries.length; i++)
			{
				short x1 = (short) hm.rom.readMulti(HOTSPOTS_ADDR + (i * 8),2),
					y1 = (short) hm.rom.readMulti(HOTSPOTS_ADDR + (i * 8) + 2,2),
					x2 = (short) hm.rom.readMulti(HOTSPOTS_ADDR + (i * 8) + 4,2),
					y2 = (short) hm.rom.readMulti(HOTSPOTS_ADDR + (i * 8) + 6,2);
				entries[i] = new Hotspot(x1,y1,x2,y2);
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("apply"))
        {
            saveInfo();
            writeToRom(this);
        }
        else if (e.getActionCommand().equals("close"))
        {
            hide();
        }
		else if (e.getSource().equals(entryChooser))
			updateComponents();
		else if (e.getSource().equals(goto1))
			net.starmen.pkhack.JHack.main.showModule(
        			MapEditor.class, new Integer[] { 
        					new Integer(preview1.getMapTileX()),
							new Integer(preview1.getMapTileY())
        			});
		else if (e.getSource().equals(goto2))
			net.starmen.pkhack.JHack.main.showModule(
        			MapEditor.class, new Integer[] { 
        					new Integer(preview2.getMapTileX()),
							new Integer(preview2.getMapTileY())
        			});
		else if (e.getSource().equals(seek1))
		{
			seeking1 = true;
			net.starmen.pkhack.JHack.main.showModule(MapEditor.class, this);
			seek1.setEnabled(false);
			seek2.setEnabled(false);
		}
		else if (e.getSource().equals(seek2))
		{
			seeking1 = false;
			net.starmen.pkhack.JHack.main.showModule(MapEditor.class, this);
			seek1.setEnabled(false);
			seek2.setEnabled(false);
		}
	}
	
	public static Hotspot getHotspot(int num)
	{
		return entries[num];
	}
	
	public static class Hotspot
	{
		private short x1, y1, x2, y2;
		
		public Hotspot(short x1, short y1, short x2, short y2)
		{
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		
		public short getX1()
		{
			return x1;
		}
		
		public void setX1(short x1)
		{
			this.x1 = x1;
		}
		
		public short getY1()
		{
			return y1;
		}
		
		public void setY1(short y1)
		{
			this.y1 = y1;
		}
		
		public short getX2()
		{
			return x2;
		}
		
		public void setX2(short x2)
		{
			this.x2 = x2;
		}
		
		public short getY2()
		{
			return y2;
		}
		
		public void setY2(short y2)
		{
			this.y2 = y2;
		}
		
		public byte[] toByteArray()
		{
			byte[] out = new byte[8];
			out[0] = (byte) (x1 & 0xff);
			out[1] = (byte) ((x1 & 0xff00) / 0x100);
			out[2] = (byte) (y1 & 0xff);
			out[3] = (byte) ((y1 & 0xff00) / 0x100);
			out[4] = (byte) (x2 & 0xff);
			out[5] = (byte) ((x2 & 0xff00) / 0x100);
			out[6] = (byte) (y2 & 0xff);
			out[7] = (byte) ((y2 & 0xff00) / 0x100);
			return out;
		}
	}

	public void returnSeek(int x, int y, int tileX, int tileY)
	{
		MapGraphics target;
		if (seeking1)
			target = preview1;
		else
			target = preview2;
		target.setMapXY(x / 8 + tileX * MapEditor.tileWidth / 8,
				y / 8 + tileY * MapEditor.tileHeight / 8);
		seek1.setEnabled(true);
		seek2.setEnabled(true);
	}
}
