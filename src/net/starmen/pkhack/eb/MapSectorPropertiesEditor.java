package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.MapEditor.MapGraphics;
import net.starmen.pkhack.eb.MapEditor.EbMap;

/**
 * @author Mr. Tenda
 *
 * TODO Write javadoc for this class
 */
public class MapSectorPropertiesEditor extends EbHackModule implements ActionListener, DocumentListener
{
	public MapSectorPropertiesEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	private JTextField sectorX, sectorY;
	private JCheckBox cantTeleport, unknown; 
	private JComboBox townMap, misc, item;
	private MapGraphics preview;
	
	private static final String[] miscEffects = new String[] {
		"No special settings",
		"Area is indoors",
		"Exit Mice will function",
		"Lost Underworld sprites",
		"Magicant sprites",
		"Robotomized sprites",
		"Super-frequent Magic Butterflies",
		"Super-frequent Magic Butterflies"
	};

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		preview = new MapGraphics(this, MapEditor.sectorWidth, MapEditor.sectorHeight,
				5, false, false, false);
		
		sectorX = HackModule.createSizedJTextField(2, true, false);
		sectorX.getDocument().addDocumentListener(this);
		sectorY = HackModule.createSizedJTextField(2, true, false);
		sectorY.getDocument().addDocumentListener(this);
		cantTeleport = new JCheckBox();
		unknown = new JCheckBox();
		String[] tmp = new String[townMapNames.length + 2];
		tmp[0] = "No Map";
		tmp[tmp.length - 1] = "No Map";
		System.arraycopy(townMapNames, 0, tmp, 1, townMapNames.length);
		townMap = new JComboBox(tmp);
		misc = new JComboBox(miscEffects);
		item = ItemEditor.createItemSelector(new JComboBox(), this);
		
		JPanel topPanel = new JPanel();
		topPanel.add(new JLabel("Sector X:"));
		topPanel.add(sectorX);
		topPanel.add(new JLabel("Sector Y:"));
		topPanel.add(sectorY);
		
		JPanel middlePanel = new JPanel();
		middlePanel.add(new JLabel("Can't Teleport:"));
		middlePanel.add(cantTeleport);
		middlePanel.add(new JLabel("Unknown?:"));
		middlePanel.add(unknown);
		middlePanel.add(new JLabel("Town Map:"));
		middlePanel.add(townMap);
		middlePanel.add(new JLabel("Misc:"));
		middlePanel.add(misc);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(HackModule.createFlowLayout(preview));
		mainPanel.add(topPanel);
		mainPanel.add(middlePanel);
		mainPanel.add(HackModule.getLabeledComponent("Type-58 Usable Item:", item));
		
		mainWindow.getContentPane().add(mainPanel, BorderLayout.CENTER);
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
		return "Map Sector Properties Editor";
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
		readFromRom();
		updateComponents();
		mainWindow.setVisible(true);
	}
	
	public void show(Object obj)
	{
		super.show();
		if (obj instanceof int[])
		{
			int[] coords = (int[]) obj;
			setSectorXY(coords[0], coords[1]);
		}
		updateComponents();
		mainWindow.setVisible(true);
	}
	
	public void readFromRom()
	{
		EbMap.loadData(this, true, false, false);
	}
	
	private void setSectorXY(int x, int y)
	{
		sectorX.getDocument().removeDocumentListener(this);
		sectorX.setText(Integer.toString(x));
		sectorX.getDocument().addDocumentListener(this);
		sectorY.getDocument().removeDocumentListener(this);
		sectorY.setText(Integer.toString(y));
		sectorY.getDocument().addDocumentListener(this);
	}
	 
	private void updateComponents()
	{
		int sectorX = Integer.parseInt(this.sectorX.getText()),
			sectorY = Integer.parseInt(this.sectorY.getText());
		boolean ok = (sectorX < MapEditor.widthInSectors) && (sectorY < MapEditor.heightInSectors);
		
		cantTeleport.setEnabled(ok);
		unknown.setEnabled(ok);
		townMap.setEnabled(ok);
		misc.setEnabled(ok);
		item.setEnabled(ok);
		
		preview.setEnabled(ok);
		if (ok)
		{
			if (! EbMap.isSectorDataLoaded(sectorX, sectorY))
				EbMap.loadSectorData(rom, sectorX, sectorY);
			preview.setMapXY(sectorX * MapEditor.sectorWidth,
					sectorY * MapEditor.sectorHeight);
			preview.reloadMap();
		}
		preview.repaint();
		
		EbMap.Sector sector = (ok ? EbMap.getSectorData(sectorX, sectorY) : null);
		cantTeleport.setSelected(ok ? sector.cantTeleport() : false);
		unknown.setSelected(ok ? sector.isUnknownEnabled() : false);
		townMap.setSelectedIndex(ok ? sector.getTownMap() : -1);
		misc.setSelectedIndex(ok ? sector.getMisc() : -1);
		item.setSelectedIndex(ok ? sector.getItem() & 0xff : -1);
		item.repaint();
	}
	
	private void saveChanges()
	{
		int sectorX = Integer.parseInt(this.sectorX.getText()),
			sectorY = Integer.parseInt(this.sectorY.getText());
		EbMap.Sector sector = EbMap.getSectorData(sectorX, sectorY);
		sector.setCantTeleport(cantTeleport.isSelected());
		sector.setUnknown(unknown.isSelected());
		sector.setTownMap((byte) (townMap.getSelectedIndex() & 0xff));
		sector.setMisc((byte) (misc.getSelectedIndex() & 0xff));
		sector.setItem((byte) (item.getSelectedIndex() & 0xff));
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("apply"))
		{
			saveChanges();
			EbMap.writeSectorData(rom);
		}
       else if (e.getActionCommand().equals("close"))
       	hide();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent e)
	{
		if ((sectorX.getText().length() > 0) && (sectorY.getText().length() > 0))
			updateComponents();
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
}
