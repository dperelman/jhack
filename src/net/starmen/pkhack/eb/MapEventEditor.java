package net.starmen.pkhack.eb;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.MapEditor.EbMap;
import net.starmen.pkhack.eb.MapEditor.TileChooser;

/**
 * TODO Write javadoc for this class
 * 
 * @author Mr. Tenda
 */
public class MapEventEditor extends EbHackModule implements ActionListener, DocumentListener
{
	/**
	 * @param rom
	 * @param prefs
	 */
	public MapEventEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	public static int asmPointer = 0x90d;
	
	private ButtonGroup group;
	private JPanel tilesPanel;
	private JButton add, del, addGroup, delGroup;
	private JComboBox tileset, palette, page, flagGroup;
	private JCheckBox reverse;
	private JTextField flag, limit;
	private TileChooser tileChooser;
	private static ArrayList[] entries;
	private int selected = -1;
	private static final int end = 0x101a80;
	private static final String[] errorMessages = new String[] {
			"You left the event flag field empty.",
			"If there are tile changes, then the flag cannot be 0x0."
	};

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init()
	{
		readFromRom(this.rom);
		
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		String[] tsetNames = new String[MapEditor.drawTsetNum];
		for (int i = 0; i < tsetNames.length; i++)
			tsetNames[i] = i + " - " + TileEditor.TILESET_NAMES[i];
		tileset = new JComboBox(tsetNames);
		tileset.addActionListener(this);
		
		flagGroup = new JComboBox();
		flagGroup.addActionListener(this);
		
		flag = HackModule.createSizedJTextField(3, false);
		flag.addActionListener(this);
		
		reverse = new JCheckBox();
		reverse.addActionListener(this);
		
		palette = new JComboBox();
		palette.addActionListener(this);
		
		add = new JButton("Add Tile Change");
		add.addActionListener(this);
		
		del = new JButton("Delete Tile Change");
		del.addActionListener(this);
		
		addGroup = new JButton("Add Flag Group");
		addGroup.addActionListener(this);
		
		delGroup = new JButton("Delete Flag Group");
		delGroup.addActionListener(this);
		
		tilesPanel = new JPanel();
		tilesPanel.setLayout(new BoxLayout(tilesPanel, BoxLayout.Y_AXIS));
		
		tileChooser = new TileChooser(23, 3, false);
		tileChooser.addActionListener(this);
		
		limit = HackModule.createSizedJTextField(2, true);
		limit.setText(Integer.toString(5));
		limit.getDocument().addDocumentListener(this);
		
		page = new JComboBox();
		page.addActionListener(this);
				
		JPanel topPanel = new JPanel();
		topPanel.add(HackModule.getLabeledComponent("Tileset: ", tileset));
		topPanel.add(HackModule.getLabeledComponent("Preview palette: ", palette));
		topPanel.add(HackModule.getLabeledComponent("Flag Group : ", flagGroup));
		topPanel.add(addGroup);
		topPanel.add(delGroup);
		
		JPanel middlePanel = new JPanel();
		middlePanel.add(HackModule.getLabeledComponent("Flag: ", flag));
		middlePanel.add(HackModule.getLabeledComponent("Reversal: ", reverse));
		middlePanel.add(add);
		middlePanel.add(del);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JLabel("Limit per page: "));
		bottomPanel.add(limit);
		bottomPanel.add(new JLabel("Page: "));
		bottomPanel.add(page);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(topPanel);
		panel.add(middlePanel);
		panel.add(tilesPanel);
		panel.add(tileChooser);
		panel.add(tileChooser.getScrollBar());
		panel.add(bottomPanel);
		
		mainWindow.getContentPane().add(panel, BorderLayout.CENTER);
		mainWindow.pack();
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
		return "Event-Based Map Changes Editor";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by Mr. Tenda";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{
		mainWindow.setVisible(false);
	}
	
	public void show()
	{
		super.show();
		mainWindow.setVisible(true);
		
		if (tileset.getSelectedIndex() < 0)
			tileset.setSelectedIndex(0);
		else
			updateComponents(true, false, false);
	}
	
	public void show(Object obj)
	{
		super.show();
		mainWindow.setVisible(true);
		
		if (obj instanceof Integer)
			tileset.setSelectedIndex(((Integer) obj).intValue());
	}
	
	public void reset()
	{
		readFromRom(rom);
	}
	
	private void updateComponents(boolean changingTileset, boolean updatePage, boolean ignorePalette)
	{
		if (changingTileset)
		{
			if (! ignorePalette)
			{
				palette.removeActionListener(this);
		        palette.removeAllItems();
		        TileEditor.Tileset tset = TileEditor.tilesets[tileset.getSelectedIndex()];
		        for (int i = 0; i < tset.getPaletteCount(); i++)
		        	palette.addItem(new String(
				               tset.getPalette(i).mtileset + "/"
				                    + tset.getPalette(i).mpalette));
		        palette.addActionListener(this);
			}
	        
			flagGroup.removeActionListener(this);
			flagGroup.removeAllItems();
			for (int i = 0; i < entries[tileset.getSelectedIndex()].size(); i++)
				flagGroup.addItem("Group #" + i);
			flagGroup.setEnabled(entries[tileset.getSelectedIndex()].size() > 0);
			if (entries[tileset.getSelectedIndex()].size() > 0)
				flagGroup.setSelectedIndex(0);
			else
				flagGroup.setSelectedIndex(-1);
			flagGroup.addActionListener(this);
		}
		add.setEnabled(flagGroup.getSelectedIndex() != -1);
		delGroup.setEnabled(flagGroup.getSelectedIndex() != -1);
		flag.setEnabled(flagGroup.getSelectedIndex() != -1);
		reverse.setEnabled(flagGroup.getSelectedIndex() != -1);
		page.setEnabled(flagGroup.getSelectedIndex() != -1);
		if (flagGroup.getSelectedIndex() == -1)
		{
			del.setEnabled(false);
			flag.setText("");
			reverse.setSelected(false);
			page.removeActionListener(this);
			page.removeAllItems();
			page.addActionListener(this);
			tilesPanel.removeAll();
			tilesPanel.add(new JLabel("This tileset has no flag groups."));
			selected = -1;
		}
		else
		{
			TilesetChange entry = (TilesetChange) entries[tileset.getSelectedIndex()].get(
					flagGroup.getSelectedIndex());
			if (changingTileset)
			{
				flagGroup.removeActionListener(this);
				flagGroup.removeAllItems();
				for (int i = 0; i < entries[tileset.getSelectedIndex()].size(); i++)
					flagGroup.addItem("Group #" + i);
				flagGroup.setSelectedIndex(0);
				flagGroup.addActionListener(this);
				flag.setText(Integer.toHexString(entry.getFlag() & 0xffff));
				reverse.setSelected(entry.isFlagReversed());
			}
			
			if (changingTileset || updatePage)
			{
				int formerPage = page.getSelectedIndex();
				page.removeActionListener(this);
				page.removeAllItems();
				int extra = 0;
				if ((entry.size() % Integer.parseInt(limit.getText()) > 0) || (entry.size() == 0))
					extra = 1;
				if (Integer.parseInt(limit.getText()) > 0)
					for (int i = 0; i < (entry.size() / Integer.parseInt(limit.getText())) + extra; i++)
						page.addItem("Page " + (i + 1));
				for (int i = formerPage; i >= 0; i--)
					if (page.getItemAt(i) != null)
					{
						page.setSelectedIndex(i);
						i = -1;
					}
				page.addActionListener(this);
			}
			
			tilesPanel.removeAll();
			del.setEnabled(entry.size() > 0);
			if (entry.size() > 0)
			{
				group = new ButtonGroup();
				int start = Integer.parseInt(limit.getText()) * page.getSelectedIndex(), end;
				if (start + Integer.parseInt(limit.getText()) > entry.size())
					end = entry.size();
				else
					end = start + Integer.parseInt(limit.getText());
				if (entry.size() - 1 < selected / 2)
					selected = -1;
				for (int i = start; i < end; i++)
				{
					TilesetChange.TileChange change = entry.getTileChange(i);
					JPanel row = new JPanel();
					row.add(new JLabel("#" + (i + 1) + " Before: "));
					TileButton button = new TileButton(change.getTile1(), tileset.getSelectedIndex(), palette.getSelectedIndex());
					if (i * 2 == selected)
					{
						button.setSelected(true);
						if (changingTileset)
							tileChooser.setSelected(change.getTile1());
					}
					group.add(button);
					button.setActionCommand(Integer.toString(i * 2));
					button.addActionListener(this);
					row.add(button);
					row.add(new JLabel(" After: "));
					button = new TileButton(change.getTile2(), tileset.getSelectedIndex(), palette.getSelectedIndex());
					if (i * 2 + 1 == selected)
					{
						button.setSelected(true);
						if (changingTileset)
							tileChooser.setSelected(change.getTile1());
					}
					group.add(button);
					button.setActionCommand(Integer.toString(i * 2 + 1));
					button.addActionListener(this);
					row.add(button);
					tilesPanel.add(row);
				}
			}
			else
				tilesPanel.add(new JLabel("No tile changes."));
		}
		
		tileChooser.setTsetPal(tileset.getSelectedIndex(), palette.getSelectedIndex());
		tileChooser.repaint();
		
		mainWindow.pack();
	}
	
	public static void readFromRom(AbstractRom rom)
	{
		entries = new ArrayList[MapEditor.drawTsetNum];
		for (int i = 0; i < entries.length; i++)
		{
			entries[i] = new ArrayList();
			int address = 0x100200 + rom.readMulti(toRegPointer(rom.readMulti(asmPointer,3)) + (i * 2), 2);
			while (rom.readMulti(address, 2) != 0)
			{
				boolean reverse = false;
				int flag = rom.readMulti(address,2);
				if (flag >= 0x8000)
				{
					reverse = true;
					flag -= 0x8000;
				}
				TilesetChange tilesetChange = new TilesetChange((short) flag, reverse);
				int num = rom.readMulti(address + 2, 2);
				for (int j = 0; j < num; j++)
					tilesetChange.addTileChange(new TilesetChange.TileChange(
							rom.readByte(address + ((j + 1) * 4)),
							rom.readByte(address + ((j + 1) * 4) + 1),
							rom.readByte(address + ((j + 1) * 4) + 2),
							rom.readByte(address + ((j + 1) * 4) + 3)));
				entries[i].add(tilesetChange);
				address += (num + 1) * 4;
			}
		}
	}
	
	private int saveInfo()
	{
		ArrayList tilesetEntries = entries[tileset.getSelectedIndex()];
		if (tilesetEntries.size() > 0)
		{
			TilesetChange entry = (TilesetChange) tilesetEntries.get(
					flagGroup.getSelectedIndex());
			if ((flag.getText().length() == 0))
				return 0;
			else
			{
				int flagNum = Integer.parseInt(flag.getText(), 16);
				if ((flagNum <= 0) && (entry.size() > 0))
					return -1;
				else
					entry.setFlag((short) flagNum);
			}
			
			entry.setReverse(reverse.isSelected());
		}
		
		return 1;
	}
	
	public static boolean writeToRom(HackModule hm)
	{
		byte[][] data = new byte[entries.length][];
		byte[] pointers = new byte[2 * entries.length];
		int pointer = 0x101798, nullAddress = -1;
		for (int i = 0; i < entries.length; i++)
		{
			if (entries[i].size() == 0)
			{
				if (nullAddress == -1)
				{
					if (pointer + 2 >= end)
						return false;
					nullAddress = pointer;
					data[i] = new byte[] { 0, 0 };
					pointer += data[i].length;
				}
				pointers[i * 2] = (byte) ((nullAddress - 0x100200) & 0xff);
				pointers[i * 2 + 1] = (byte) (((nullAddress - 0x100200) & 0xff00) / 0x100);
			}
			else
			{
				byte[][] entry = new byte[entries[i].size()][];
				int length = 0;
				for (int j = 0; j < entry.length; j++)
				{
					entry[j] = ((TilesetChange) entries[i].get(j)).toByteArray();
					length += entry[j].length;
				}
				byte[] entryData = new byte[length + 2];
				int pos = 0;
				for (int j = 0; j < entry.length; j++)
				{
					System.arraycopy(entry[j],0,entryData,pos,entry[j].length);
					pos += entry[j].length;
				}
				System.arraycopy(new byte[] { 0, 0 }, 0, entryData, pos, 2);
				
				if (pointer + entryData.length >= end)
					return false;
				data[i] = entryData;
				pointers[i * 2] = (byte) ((pointer - 0x100200) & 0xff);
				pointers[i * 2 + 1] = (byte) (((pointer - 0x100200) & 0xff00) / 0x100);
				pointer += entryData.length;
			}
		}
		
		hm.writetoFree(pointers, asmPointer, 3, pointers.length, pointers.length, true);
		
		for (int i = 0; i < data.length; i++)
			if (data[i] != null)
				hm.rom.write(0x100200 + hm.rom.readMulti(
						toRegPointer(hm.rom.readMulti(asmPointer, 3)) + (i * 2), 2), data[i]);
		
		return true;
	}
	
	public static TilesetChange.TileChange getTileChange(int tileset, int group, int num)
	{
		return ((TilesetChange) entries[tileset].get(group)).getTileChange(num);
	}
	
	public static int countGroups(int tileset)
	{
		return entries[tileset].size();
	}
	
	public static int countTileChanges(int tileset, int group)
	{
		return ((TilesetChange) entries[tileset].get(group)).size();
	}
	

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("apply"))
        {
            int status = saveInfo();
            if (status < 1)
            	JOptionPane.showMessageDialog(mainWindow,
            		    "ERROR: " + errorMessages[status * -1],
            		    "Error saving changes!",
            		    JOptionPane.ERROR_MESSAGE);
            else if (! writeToRom(this))
            	JOptionPane.showMessageDialog(mainWindow,
            		    "ERROR: Not enough space for changes, so remove some!",
            		    "Error writing!",
            		    JOptionPane.ERROR_MESSAGE);
        }
        else if (e.getActionCommand().equals("close"))
        	hide();
        else if (e.getSource().equals(addGroup))
        {
        	TilesetChange change = new TilesetChange((short) 1, false);
        	entries[tileset.getSelectedIndex()].add(change);
        	updateComponents(true, false, true);
        	flagGroup.setSelectedIndex(entries[tileset.getSelectedIndex()].indexOf(change));
        }
        else if (e.getSource().equals(delGroup))
        {
        	int confirm = JOptionPane.showConfirmDialog(
        		    mainWindow,
        		    "Do you really want to delete this flag group?",
        		    "Are you sure?",
        		    JOptionPane.YES_NO_OPTION);
        	if (confirm == JOptionPane.YES_OPTION)
        		entries[tileset.getSelectedIndex()].remove(flagGroup.getSelectedIndex());
        	updateComponents(true, false, true);
        }
        else if (e.getSource().equals(tileset))
        	updateComponents(true, false, false);
        else if (e.getSource().equals(flagGroup))
        	updateComponents(false, false, true);
        else if (e.getSource().equals(palette))
        	updateComponents(false, false, false);
        else if (e.getSource().equals(page))
        	updateComponents(false, false, false);
        else if (e.getSource().equals(add))
        {
        	((TilesetChange) entries[tileset.getSelectedIndex()].get(
        			flagGroup.getSelectedIndex())).addTileChange(new TilesetChange.TileChange((short) 0, (short) 0));
        	updateComponents(false, true, false);
        }
        else if (e.getSource().equals(del))
        {
        	String input = JOptionPane.showInputDialog(
                    mainWindow,
                    "Delete which tile change?",
                    Integer.toString(1));
        	if ((input != null) && (input.length() > 0))
        	{
        		int num = Integer.parseInt(input) - 1;
        		TilesetChange entry = (TilesetChange) entries[tileset.getSelectedIndex()].get(
            			flagGroup.getSelectedIndex());
        		if ((num >= 0) && (num <= entry.size()))
        		{
        			if (selected / 2 == num)
        				selected = -1;
        			entry.removeTileChange(num);
        			updateComponents(false, true, false);
        		}
        		else
        			JOptionPane.showMessageDialog(mainWindow,
                		    "ERROR: No change with number " + (num + 1) + "!",
                		    "Error deleting!",
                		    JOptionPane.ERROR_MESSAGE);
        	}
        }
        else if ((e.getSource().equals(tileChooser)) && (selected != -1))
        {
        	TilesetChange change = ((TilesetChange) entries[tileset.getSelectedIndex()].get(
        			flagGroup.getSelectedIndex()));
        	if (selected % 2 == 0)
    			change.getTileChange(selected / 2).setTile1((short) tileChooser.getSelected());
    		else
    			change.getTileChange(selected / 2).setTile2((short) tileChooser.getSelected());
    		updateComponents(false, false, false);	
        }
        else if (e.getSource() instanceof TileButton)
        {
        	selected = Integer.parseInt(e.getActionCommand());
        	TilesetChange change = ((TilesetChange) entries[tileset.getSelectedIndex()].get(
        			flagGroup.getSelectedIndex()));
        	if (selected % 2 == 0)
        		tileChooser.setSelected(change.getTileChange(selected / 2).getTile1());
        	else
        		tileChooser.setSelected(change.getTileChange(selected / 2).getTile2());
        	if (! tileChooser.isSelectedVisible())
        		tileChooser.scrollToSelected();
        	tileChooser.repaint();
        }
	}
	

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent e)
	{
		if (e.getDocument().equals(limit.getDocument()) 
				&& (limit.getText().length() > 0)
				&& (Integer.parseInt(limit.getText()) > 0))
		{
			updateComponents(true, false, true);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	public void insertUpdate(DocumentEvent e)
	{
		changedUpdate(e);
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	public void removeUpdate(DocumentEvent e)
	{
		changedUpdate(e);
	}
	
	public static class TilesetChange
	{
		private short flag;
		private boolean reverse;
		private ArrayList tiles;
		
		public TilesetChange(short flag, boolean reverse)
		{
			this.flag = flag;
			this.reverse = reverse;
			tiles = new ArrayList();
		}
		
		public short getFlag()
		{
			return flag;
		}
		
		public void setFlag(short flag)
		{
			this.flag = flag;
		}
		
		public void setReverse(boolean reverse)
		{
			this.reverse = reverse;
		}
		
		public boolean isFlagReversed()
		{
			return reverse;
		}
		
		public short getFullFlag()
		{
			if (isFlagReversed())
				return (short) (getFlag() + 0x8000);
			else
				return getFlag();
		}
		
		public void setFlagReversed(boolean reverse)
		{
			this.reverse = reverse;
		}
		
		public void addTileChange(TileChange tc)
		{
			tiles.add(tc);
		}
		
		public TileChange getTileChange(int num)
		{
			return (TileChange) tiles.get(num);
		}
		
		public void removeTileChange(int num)
		{
			tiles.remove(num);
		}
		
		public int size()
		{
			return tiles.size();
		}
		
		public byte[] toByteArray()
		{
			if (size() == 0)
				return new byte[0];
			else
			{
				byte[] out = new byte[(size() + 1) * 4];
				out[0] = (byte) (getFullFlag() & 0xff);
				out[1] = (byte) ((getFullFlag() & 0xff00) / 0x100);
				out[2] = (byte) (size() & 0xff);
				out[3] = (byte) ((size() & 0xff00) / 0x100);
				for (int i = 0; i < size(); i++)
				{
					TileChange change = getTileChange(i);
					out[(i + 1) * 4] = (byte) (change.getTile1() & 0xff);
					out[(i + 1) * 4 + 1] = (byte) ((change.getTile1() & 0xff00) / 0x100);
					out[(i + 1) * 4 + 2] = (byte) (change.getTile2() & 0xff);
					out[(i + 1) * 4 + 3] = (byte) ((change.getTile2() & 0xff00) / 0x100);
				}
				return out;
			}
		}
		
		public static class TileChange
		{
			private short tile1, tile2; 
			
			public TileChange(byte tile1, byte localTset1, byte tile2, byte localTset2)
			{
				this.tile1 = (short) (tile1 + (localTset1 * 0x100));
				this.tile2 = (short) (tile2 + (localTset2 * 0x100));
			}
			
			public TileChange(short tile1, short tile2)
			{
				this.tile1 = tile1;
				this.tile2 = tile2;
			}
			
			public short getTile1()
			{
				return tile1;
			}
			
			public void setTile1(short tile1)
			{
				this.tile1 = tile1;
			}
			
			public short getTile2()
			{
				return tile2;
			}
			
			public void setTile2(short tile2)
			{
				this.tile2 = tile2;
			}
		}
	}
	
	public static class TileButton extends JToggleButton implements MouseListener
	{
		private int tile, tileset, palette;
		
		public TileButton(int tile, int tileset, int palette)
		{
			this.tile = tile;
			this.tileset = tileset;
			this.palette = palette;
			
			addMouseListener(this);
			setPreferredSize(new Dimension(MapEditor.tileWidth + 2, MapEditor.tileHeight + 2));
		}
		
		public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            EbMap.loadTileImage(tileset, tile, palette);
            g.drawImage(
                    EbMap.getTileImage(tileset, tile, palette),
                    1, 1, MapEditor.tileWidth, MapEditor.tileHeight, this);
            
            g2d.setPaint(Color.black);
            g2d.draw(new Rectangle2D.Double(
    					0, 0, MapEditor.tileWidth + 1, MapEditor.tileHeight + 1));
            if (isSelected())
            {
                g2d.setPaint(Color.orange);
    			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F));
    			Rectangle2D.Double rect = new Rectangle2D.Double(
    					1, 1, MapEditor.tileWidth, MapEditor.tileHeight);
    			g2d.fill(rect);
    			g2d.draw(rect);
            }
        }

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent e)
		{
			setSelected(! isSelected());
			this.fireActionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent e)
		{}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent e)
		{}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent e)
		{}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent e)
		{}
	}
}
