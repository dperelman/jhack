/*
 * Created on Dec 31, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.MapEditor.EbMap;

/**
 * @author max
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DoorEditor extends EbHackModule 
	implements ActionListener, ItemListener, SeekListener
{
	private JTextField areaXField, areaYField, entryNumField, 
		numField, ptrField, xField, yField, flagField;
	private JComboBox typeBox, typeDestBox, styleBox, dirBox;
	private JLabel warnLabel;
	private JButton setXYButton;
	private JCheckBox reverseCheck;
	private final String[] typeNames = {
			"Switch", "Rope/Ladder", "Door", "Escalator",
			"Stairway", "Object", "Person"
	};
	private final String[] typeDestNames = {
			"Door", "Switch", "Object"
	};
	private boolean muteEvents = false;

	private DestPreview destPreview;
	
	/**
	 * @param rom
	 * @param prefs
	 */
	public DoorEditor(AbstractRom rom, XMLPreferences prefs)
	{
	    super(rom, prefs);
	}
	
	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init()
	{
		readFromRom();
		TeleportTableEditor.initWarpStyleNames();
		destPreview = new DestPreview();
		createGUI();
	}
	
	public void reset()
	{
		readFromRom();
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
		return "Door Entry/Destination Editor";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits() {
		return "Written by Mr. Tenda\n"
			+ "Info from Mr. Accident";
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
		updateComponents(true,true,true);
		this.mainWindow.setVisible(true);
	}

	public void show(Object obj)
	{
		super.show();
		mainWindow.setVisible(true);
		int[] data = (int[]) obj;
		muteEvents = true;
		areaXField.setText(Integer.toString(data[0]));
		areaYField.setText(Integer.toString(data[1]));
		entryNumField.setText(Integer.toString(data[2]));
		muteEvents = false;
		updateComponents(true, true, true);
	}
	
	public void readFromRom()
	{
		EbMap.loadDoorData(rom);
	}

	public void createGUI()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		
		JPanel entryPanel = new JPanel();
		entryPanel.setLayout(
				new BoxLayout(entryPanel, BoxLayout.Y_AXIS));
		
		DocumentListener listener = new DocumentListener()
		{
	        public void changedUpdate(DocumentEvent de)
	        {
	        	if (!muteEvents
	        			&& (numField.getText().length() > 0)
	        			&& (entryNumField.getText().length() > 0)
						&& (areaXField.getText().length() > 0)
						&& (areaYField.getText().length() > 0))
	        		updateComponents(true, true, true);
	        }

	        public void insertUpdate(DocumentEvent de)
	        {
	            changedUpdate(de);
	        }

	        public void removeUpdate(DocumentEvent de)
	        {
	            changedUpdate(de);
	        }
		};
		
		JPanel entryLoc = new JPanel();
		areaXField = HackModule.createSizedJTextField(2, true);
		areaXField.getDocument().addDocumentListener(listener);
		entryLoc.add(HackModule.getLabeledComponent("Area X: ", areaXField));
		areaYField = HackModule.createSizedJTextField(2, true);
		areaYField.getDocument().addDocumentListener(listener);
		entryLoc.add(HackModule.getLabeledComponent("Area Y: ", areaYField));
		entryNumField = HackModule.createSizedJTextField(3, true);
		entryNumField.getDocument().addDocumentListener(listener);
		entryLoc.add(HackModule.getLabeledComponent("Num: ", entryNumField));
		entryPanel.add(entryLoc);
		
		numField = HackModule.createSizedJTextField(5, true);
		numField.getDocument().addDocumentListener(listener);
		entryPanel.add(
				HackModule.getLabeledComponent(
						"Destination: ", numField));
		
		typeBox = new JComboBox(typeNames);
		typeBox.addItemListener(this);
		entryPanel.add(
				HackModule.getLabeledComponent(
						"Type: ", typeBox));
		
		JPanel destPanel = new JPanel();
		destPanel.setLayout(
				new BoxLayout(destPanel, BoxLayout.Y_AXIS));
		
		typeDestBox = new JComboBox(typeDestNames);
		typeDestBox.addItemListener(this);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Type: ", typeDestBox));
		
		ptrField = HackModule.createSizedJTextField(6, false);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Pointer: ", ptrField));
		
		flagField = HackModule.createSizedJTextField(4, false);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Event flag: ", flagField));
		
		reverseCheck = new JCheckBox();
		destPanel.add(
				HackModule.getLabeledComponent(
						"Reverse event flag effect: ",
						reverseCheck));
		
		JPanel buttonPanel = new JPanel();
		setXYButton = new JButton("Set X&Y using map");
		setXYButton.addActionListener(this);
		buttonPanel.add(setXYButton);
		destPanel.add(buttonPanel);
		
		DocumentListener xyListener = 
			new DocumentListener()
					{
	            public void changedUpdate(DocumentEvent de)
	            {
	            	if ((xField.getText().length() > 0)
	            			&& (yField.getText().length() > 0))
	            	{
	            		destPreview.setXY(
	    						Integer.parseInt(xField.getText()), 
								Integer.parseInt(yField.getText()));
	            		destPreview.remoteRepaint();
	            	}
	            }

	            public void insertUpdate(DocumentEvent de)
	            {
	                changedUpdate(de);
	            }

	            public void removeUpdate(DocumentEvent de)
	            {
	                changedUpdate(de);
	            }
					};
		
		xField = HackModule.createSizedJTextField(5, true);
		xField.getDocument().addDocumentListener(xyListener);
		destPanel.add(
				HackModule.getLabeledComponent(
						"X: ", xField));
		
		yField = HackModule.createSizedJTextField(5, true);
		yField.getDocument().addDocumentListener(xyListener);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Y: ", yField));
		
		dirBox = new JComboBox(new String[] {
				"Down", "Up", "Right", "Left"
		});
		destPanel.add(
				HackModule.getLabeledComponent(
						"Direction:  ", dirBox));
		
		styleBox = new JComboBox(TeleportTableEditor.warpStyleNames);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Style: ", styleBox));
		
		JPanel previewPanel = new JPanel();
		previewPanel.setLayout(
				new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.add(new JLabel("Destination Preview",0));
		destPreview.setPreferredSize(
				new Dimension(
						destPreview.getWidthTiles() * MapEditor.tileWidth + 2,
						destPreview.getHeightTiles() * MapEditor.tileHeight + 2));
		destPreview.setAlignmentX(0);
		previewPanel.add(destPreview);
		
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(
				new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.add(new JLabel("Entry Properties:",0));
		entryPanel.setAlignmentX(0);
		contentPanel.add(entryPanel);
		contentPanel.add(new JLabel("Destination Properties:",0));
		destPanel.setAlignmentX(0);
		contentPanel.add(destPanel);
		warnLabel = new JLabel("Type match.");
		warnLabel.setHorizontalAlignment(0);
		contentPanel.add(warnLabel);
		
		this.mainWindow.getContentPane().add(
				contentPanel, BorderLayout.CENTER);
		this.mainWindow.getContentPane().add(
				previewPanel, BorderLayout.LINE_START);
		this.mainWindow.pack();
	}

	private void updateComponents(boolean load, boolean loadEntry, 
			boolean loadDestType)
	{
		muteEvents = true;
		int destNum;
		if (loadEntry)
		{
			int x = Integer.parseInt(areaXField.getText()),
				y = Integer.parseInt(areaYField.getText()),
				num = Integer.parseInt(entryNumField.getText());
			if (EbMap.getDoorsNum(x,y) > num)
			{
				EbMap.DoorLocation doorLocation = EbMap.getDoorLocation(x,y,num);
				destNum = doorLocation.getDestIndex();
				numField.setText(Integer.toString(destNum));
				typeBox.setSelectedIndex(doorLocation.getType());
			}
			else
			{
				numField.setEnabled(false);
				numField.setText("");
				typeBox.setEnabled(false);
				typeBox.setSelectedIndex(0);
				ptrField.setText(null);
				xField.setText(null);
				yField.setText(null);
				dirBox.setSelectedIndex(0);
				flagField.setText(null);
				reverseCheck.setSelected(false);
				styleBox.setSelectedIndex(0);
				typeDestBox.setEnabled(false);
				ptrField.setEditable(false);
				setXYButton.setEnabled(false);
				xField.setEditable(false);
				yField.setEditable(false);
				dirBox.setEnabled(false);
				flagField.setEditable(false);
				reverseCheck.setEnabled(false);
				styleBox.setEnabled(false);
				
				destPreview.clearMapArray();
				destPreview.remoteRepaint();
				
				muteEvents = false;
				return;
			}
		}
		else
			destNum = Integer.parseInt(numField.getText());
		
		numField.setEnabled(true);
		typeBox.setEnabled(true);
		if (destNum < 0)
		{
			typeDestBox.setEnabled(true);
			typeDestBox.setSelectedIndex(0);
			ptrField.setText(null);
			xField.setText(null);
			yField.setText(null);
			dirBox.setSelectedIndex(0);
			flagField.setText(null);
			reverseCheck.setSelected(false);
			styleBox.setSelectedIndex(0);
			typeDestBox.setEnabled(false);
			ptrField.setEditable(false);
			setXYButton.setEnabled(false);
			xField.setEditable(false);
			yField.setEditable(false);
			dirBox.setEnabled(false);
			flagField.setEditable(false);
			reverseCheck.setEnabled(false);
			styleBox.setEnabled(false);
			
			destPreview.clearMapArray();
			destPreview.remoteRepaint();
		}
		else
		{
			EbMap.Destination dest = 
				EbMap.getDestination(destNum);
			int destType;
			if (loadDestType)
			{
				destType = dest.getType();
				typeDestBox.setSelectedIndex(destType);
			}
			else
				destType = typeDestBox.getSelectedIndex();
			typeDestBox.setEnabled(true);
			if (destType == 0)
			{
				if (load)
				{
					ptrField.setText(Integer.toHexString(dest.getPointer()));
	        		xField.setText(Integer.toString(dest.getX()));
	        		yField.setText(Integer.toString(dest.getY()));
	        		dirBox.setSelectedIndex(dest.getDirection());
	        		flagField.setText(Integer.toHexString(dest.getFlag()));
	        		reverseCheck.setSelected(dest.isFlagReversed());
	        		styleBox.setSelectedIndex(dest.getStyle());
				}
				ptrField.setEditable(true);
				setXYButton.setEnabled(true);
				xField.setEditable(true);
				yField.setEditable(true);
				dirBox.setEnabled(true);
				flagField.setEditable(true);
				reverseCheck.setEnabled(true);
				styleBox.setEnabled(true);
	    		
				if (load)
					destPreview.setXY(dest.getX(), dest.getY());
				else
					destPreview.setXY(
							Integer.parseInt(xField.getText()), 
							Integer.parseInt(yField.getText()));
				destPreview.remoteRepaint();
			}
			else if (destType == 1)
			{
				if (load)
				{
					ptrField.setText(Integer.toHexString(dest.getPointer()));
					flagField.setText(Integer.toHexString(dest.getFlag()));
	    			reverseCheck.setSelected(dest.isFlagReversed());
				}
				xField.setText(null);
				yField.setText(null);
				dirBox.setSelectedIndex(0);
				styleBox.setSelectedIndex(0);
				ptrField.setEditable(true);
				setXYButton.setEnabled(false);
				xField.setEditable(false);
				yField.setEditable(false);
				dirBox.setEnabled(false);
				flagField.setEditable(true);
				reverseCheck.setEnabled(true);
				styleBox.setEnabled(false);
				
				destPreview.clearMapArray();
				destPreview.remoteRepaint();
			}
			else if (destType == 2)
			{
				if (load)
					ptrField.setText(
							Integer.toHexString(dest.getPointer()));
				xField.setText(null);
				yField.setText(null);
				dirBox.setSelectedIndex(0);
				flagField.setText(null);
				reverseCheck.setSelected(false);
				styleBox.setSelectedIndex(0);
				ptrField.setEditable(true);
				setXYButton.setEnabled(false);
				xField.setEditable(false);
				yField.setEditable(false);
				dirBox.setEnabled(false);
				flagField.setEditable(false);
				reverseCheck.setEnabled(false);
				styleBox.setEnabled(false);
				
				destPreview.clearMapArray();
				destPreview.remoteRepaint();
			}
			
			if (EbMap.getDoorDestType(typeBox.getSelectedIndex())
					!= destType)
			{
	    		warnLabel.setForeground(Color.RED);
	    		warnLabel.setText("Warning!! Type mismatch!");
			}
			else
			{
				warnLabel.setForeground(Color.BLACK);
				warnLabel.setText("Type match.");
			}
		}
		muteEvents = false;
	}

	public void actionPerformed(ActionEvent e)
	{
	    if (e.getActionCommand().equals("apply"))
	    {
	    	int x = Integer.parseInt(areaXField.getText()),
				y = Integer.parseInt(areaYField.getText()),
				num = Integer.parseInt(entryNumField.getText());
	    	if (EbMap.getDoorsNum(x,y) > num)
			{
	    		EbMap.DoorLocation doorLocation = EbMap.getDoorLocation(x,y,num);
				doorLocation.setDestIndex(
						Integer.parseInt(numField.getText()));
				doorLocation.setType(
						(byte) typeBox.getSelectedIndex());
				if (doorLocation.getDestIndex()
						== Integer.parseInt(numField.getText()))
				{
		    		EbMap.Destination dest =
		    			EbMap.getDestination(
		    					doorLocation.getDestIndex());
		    		if (typeDestBox.isEnabled())
		    			dest.setType((byte) 
								typeDestBox.getSelectedIndex());
		    		if (ptrField.isEditable())
		    			dest.setPointer(
		        				Integer.parseInt(
		        						ptrField.getText(),16));
		    		if (flagField.isEditable())
		    			dest.setFlag(
		        				(short) Integer.parseInt(
		        						flagField.getText(),16));
		    		if (reverseCheck.isEnabled())
		    			dest.setFlagReversed(reverseCheck.isSelected());
		    		if (yField.isEditable())
		    			dest.setY(
		        				(short) Integer.parseInt(
		        						yField.getText()));
		    		if (xField.isEditable())
		    			dest.setX(
		        				(short) Integer.parseInt(
		        						xField.getText()));
		    		if (styleBox.isEnabled())
		    			dest.setStyle(
		        				(byte) styleBox.getSelectedIndex());
		    		if (dirBox.isEnabled())
		    			dest.setDirection(
		        				(byte) dirBox.getSelectedIndex());
				}
				updateComponents(true, true, true);
			}
	    }
	    else if (e.getActionCommand().equals("close"))
	    {
	        hide();
	    }
	    else if (e.getSource() == setXYButton)
	    {
	    	setXYButton.setEnabled(false);
	    	net.starmen.pkhack.JHack.main.showModule(MapEditor.class, this);
	    }
	}

	public void itemStateChanged(ItemEvent e) {
		if ((e.getSource() == typeBox)
				&& (! muteEvents))
		{
			if ((typeBox.getSelectedIndex() == -1)
					&& (EbMap.getDoorDestType(
							typeBox.getSelectedIndex()) != -1))
			{
	    		JOptionPane.showMessageDialog(this.mainWindow,
	        			"You can't change the type to this since"
	    				+ "\nthis type requires a destination.");
	    		muteEvents = true;
				EbMap.DoorLocation doorLocation = 
					EbMap.getDoorLocation(
							Integer.parseInt(areaXField.getText()),
							Integer.parseInt(areaYField.getText()),
							Integer.parseInt(entryNumField.getText()));
	    		typeBox.setSelectedIndex(
	    				doorLocation.getType());
	    		muteEvents = false;
			}
			updateComponents(false, false, false);
		}
		else if ((e.getSource() == typeDestBox)
				&& (! muteEvents))
			updateComponents(true, false, false);
	}
	
	public void returnSeek(int x, int y, int tileX, int tileY)
	{
		int newX = ((tileX * MapEditor.tileWidth) / 8) + (x / 8),
			newY = ((tileY * MapEditor.tileHeight) / 8) + (y / 8);
		xField.setText(Integer.toString(newX));
		yField.setText(Integer.toString(newY));
		destPreview.setXY(newX, newY);
		destPreview.remoteRepaint();
		setXYButton.setEnabled(true);
	}
	
	public class DestPreview extends AbstractButton
	{
	    private int[][] mapArray;
	    private boolean knowsMap = false;
	    private int tileX, tileY, doorX, doorY;
		private int heightTiles = 5, widthTiles = 5;

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
					widthTiles * MapEditor.tileWidth + 1,
					heightTiles * MapEditor.tileHeight + 1)); 
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
	            	if (! EbMap.isSectorDataLoaded(
	            			sectorX, sectorY))
	            		EbMap.loadSectorData(rom,
	            				sectorX, sectorY);
	            	
	                tile_set = EbMap.getDrawTileset( 
	                		EbMap.getTset(sectorX, sectorY));
	                tile_tile = mapArray[i][j];
	                    /*| (EbMap.getLocalTileset( 
	                    		tileX + j,
								tileY + i) << 8);*/
	                tile_pal = 
	                	TileEditor.tilesets[tile_set].getPaletteNum(
	                			EbMap.getTset(sectorX, sectorY),
								EbMap.getPal(sectorX, sectorY));
	                EbMap.loadTileImage(tile_set, tile_tile, tile_pal);

	                g.drawImage(
	                    EbMap.getTileImage(tile_set,tile_tile,tile_pal),
	                    j * MapEditor.tileWidth + 1, i * MapEditor.tileHeight + 1,
						MapEditor.tileWidth, MapEditor.tileHeight, this);
	            }
	        }
	        g2d.setPaint(Color.blue);
	    	g2d.draw(new Rectangle2D.Double(
	    			doorX * 8,
					doorY * 8,
					8,8));
	    }
	    

	    public void setMapArray(int[][] newMapArray)
	    {
	        this.knowsMap = true;
	        this.mapArray = newMapArray;
	    }
	    
	    public void setXY(int x, int y)
	    {
	        int[][] maparray = new int[heightTiles][widthTiles];
	        tileX = ((x * 8) / MapEditor.tileWidth) - 2;
	        if (tileX < 0)
	        	tileX = 0;
	        else if (tileX + widthTiles > MapEditor.width)
	        	tileX = MapEditor.width - widthTiles;
	        for (int i = 0; i < heightTiles; i++)
	        {
	        	int y2 = i + ((y * 8) / MapEditor.tileHeight) - 2;
	            if (y2 < 0)
	            	y2 = 0;
	            maparray[i] = EbMap.getTiles(rom, 
	            		y2, tileX, widthTiles);
	        }
	        tileY = (y * 8) / MapEditor.tileHeight - 2;
	        if (tileY < 0)
	        	tileY = 0;
	        else if (tileY + heightTiles > MapEditor.height)
	        	tileY = MapEditor.height - heightTiles;
	        doorX = x - (tileX * 4);
	        doorY = y - (tileY * 4);
	        setMapArray(maparray);
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
	    
	    public int getWidthTiles()
	    {
	    	return widthTiles;
	    }
	    
	    public int getHeightTiles()
	    {
	    	return heightTiles;
	    }
	}
}