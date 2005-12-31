package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

/**
 * TODO Write javadoc for this class
 * 
 * @author Mr. Tenda
 */
public class DoorEditor extends EbHackModule 
	implements ActionListener, SeekListener
{
	private JTextField areaXField, areaYField, flagField;
	private TextEditor.TextOffsetEntry ptrField;
	private JComboBox typeBox, dirClimbBox, typeDestBox, styleBox, dirBox, entryNumBox, numBox;
	private JLabel warnLabel;
	private JButton setXYButton, jumpButton, delDoorButton;
	private JCheckBox ropeCheck, reverseCheck;
	private final String[] typeNames = {
			"Switch", "Rope/Ladder", "Door", "Escalator",
			"Stairway", "Object", "Person"
	};
	private final String[] typeDestNames = {
			"Door", "Switch", "Object"
	};
	private final String[] climbDirections = {
			"Northwest", "Northeast", "Southwest", "Southeast", "Nowhere"
	};
	private boolean muteDL = false;

	private MapEditor.MapGraphics destPreview;
	
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
		TeleportTableEditor.initWarpStyleNames();
		destPreview = new MapEditor.MapGraphics(this, 5, 5, 5, false, false, true, 8, true);
		createGUI();
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.2";
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
		readFromRom();
		if (entryNumBox.getSelectedIndex() < 0) {
			updateComponents(false, false, false);
			if (entryNumBox.getItemCount() > 0)
				entryNumBox.setSelectedIndex(0);
		} else
			updateComponents(true,true,true);
		mainWindow.setVisible(true);
	}

	public void show(Object obj)
	{
		super.show();
		readFromRom();
		mainWindow.setVisible(true);
		int[] data = (int[]) obj;
		muteDL = true;
		areaXField.setText(Integer.toString(data[0]));
		areaYField.setText(Integer.toString(data[1]));
		muteDL = false;
		updateComponents(false, false, false);
		entryNumBox.removeActionListener(this);
		entryNumBox.setSelectedIndex(data[2]);
		entryNumBox.addActionListener(this);
		updateComponents(true, true, true);
	}
	
	public void readFromRom()
	{
		EbMap.loadData(this, true, true, false);
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
	        	if (!muteDL
	        			//&& (numBox.getSelectedIndex() >= 0)
	        			//&& (entryNumBox.getSelectedIndex() >= 0)
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
		/*entryNumField = HackModule.createSizedJTextField(3, true);
		entryNumField.getDocument().addDocumentListener(listener);
		entryLoc.add(HackModule.getLabeledComponent("Num: ", entryNumField));*/
		entryNumBox = new JComboBox();
		entryNumBox.addActionListener(this);
		entryLoc.add(HackModule.getLabeledComponent("Num: ", entryNumBox));
		
		JButton button = new JButton("Add");
		button.addActionListener(this);
		button.setActionCommand("addDoor");
		entryLoc.add(button);
		delDoorButton = new JButton("Delete");
		delDoorButton.addActionListener(this);
		delDoorButton.setActionCommand("delDoor");
		entryLoc.add(delDoorButton);
		entryPanel.add(entryLoc);
		
		ropeCheck = new JCheckBox();
		entryPanel.add(HackModule.getLabeledComponent("Rope: ", ropeCheck));
		
		dirClimbBox = new JComboBox(climbDirections);
		entryPanel.add(HackModule.getLabeledComponent("Escalator/Stair Direction: ", dirClimbBox));
		
		numBox = new JComboBox();
		numBox.addActionListener(this);
		entryPanel.add(
				HackModule.getLabeledComponent(
						"Destination: ", numBox));
				
		typeBox = new JComboBox(typeNames);
		typeBox.addActionListener(this);
		entryPanel.add(
				HackModule.getLabeledComponent(
						"Type: ", typeBox));
		
		JPanel destPanel = new JPanel();
		destPanel.setLayout(
				new BoxLayout(destPanel, BoxLayout.Y_AXIS));
		
		typeDestBox = new JComboBox(typeDestNames);
		typeDestBox.addActionListener(this);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Type: ", typeDestBox));
		
		//ptrField = HackModule.createSizedJTextField(6, false);
		ptrField = new TextEditor.TextOffsetEntry("Text", true);
		
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
		setXYButton = new JButton("Set X&Y using Map Editor");
		setXYButton.addActionListener(this);
		buttonPanel.add(setXYButton);
		jumpButton = new JButton("Go to in Map Editor");
		jumpButton.addActionListener(this);
		buttonPanel.add(jumpButton);
		destPanel.add(buttonPanel);
		
		destPanel.add(
				HackModule.getLabeledComponent(
						"X: ", destPreview.getXField()));
		
		destPanel.add(
				HackModule.getLabeledComponent(
						"Y: ", destPreview.getYField()));
		
		dirBox = new JComboBox(new String[] {
				"Down", "Up", "Right", "Left"
		});
		destPanel.add(
				HackModule.getLabeledComponent(
						"Direction:  ", dirBox));
		
		// styleBox = new JComboBox(TeleportTableEditor.warpStyleNames);
		styleBox = HackModule.createJComboBoxFromArray(TeleportTableEditor.warpStyleNames);
		destPanel.add(
				HackModule.getLabeledComponent(
						"Style: ", styleBox));
		
		JPanel previewPanel = new JPanel();
		previewPanel.setLayout(
				new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.add(new JLabel("Destination Preview",0));
		/*destPreview.setPreferredSize(
				new Dimension(
						destPreview.getWidthTiles() * MapEditor.tileWidth + 2,
						destPreview.getHeightTiles() * MapEditor.tileHeight + 2));*/
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
		warnLabel = new JLabel("<html>Type match.</html>");
		warnLabel.setHorizontalAlignment(0);
		contentPanel.add(warnLabel);
		
		this.mainWindow.getContentPane().add(
				contentPanel, BorderLayout.CENTER);
		this.mainWindow.getContentPane().add(
				previewPanel, BorderLayout.LINE_START);
		this.mainWindow.pack();
	}
	
	private void disableDestGUI()
	{
		//ptrField.setText(null);
		dirBox.setSelectedIndex(0);
		flagField.setText(null);
		reverseCheck.setSelected(false);
		styleBox.setSelectedIndex(0);
		typeDestBox.removeActionListener(this);
		typeDestBox.setEnabled(false);
		typeDestBox.setSelectedIndex(-1);
		typeDestBox.addActionListener(this);
		ptrField.setEnabled(false);
		setXYButton.setEnabled(false);
		jumpButton.setEnabled(false);
		dirBox.setEnabled(false);
		flagField.setEditable(false);
		reverseCheck.setEnabled(false);
		styleBox.setEnabled(false);
		destPreview.setEnabled(false); 
	}

	private void updateComponents(boolean loadDest, boolean loadEntry, 
			boolean loadDestType)
	{
		if (numBox.getItemCount() != EbMap.getNumDests()) {
			numBox.removeActionListener(this);
			numBox.removeAllItems();
			for (int i = 0; i < EbMap.getNumDests(); i++)
				numBox.addItem(getNumberedString("", i));
			numBox.addActionListener(this);
		}
		
		muteDL = true;
		int x = Integer.parseInt(areaXField.getText()),
			y = Integer.parseInt(areaYField.getText()),
			// num = Integer.parseInt(entryNumField.getText());
			num = entryNumBox.getSelectedIndex();

    	if (EbMap.getDoorsNum(x,y) != entryNumBox.getItemCount()) {
    		entryNumBox.removeActionListener(this);
    		entryNumBox.removeAllItems();
    		for (int i = 0; i < EbMap.getDoorsNum(x,y); i++)
    			entryNumBox.addItem(getNumberedString("", i));
    		entryNumBox.addActionListener(this);
    	}
    	if (entryNumBox.getSelectedIndex() == -1) {
    		if (entryNumBox.getItemCount() > 0) {
    			entryNumBox.removeActionListener(this);
    			entryNumBox.setSelectedIndex(num = 0);
    			entryNumBox.addActionListener(this);
    		} else
    			num = -1;
    	}
    	
		if ((x >= MapEditor.widthInSectors)
				|| (y >= MapEditor.heightInSectors / 2)
				|| (x < 0) || (y < 0)
				|| (num >= EbMap.getDoorsNum(x,y))
				|| (num < 0))
		{
			//disable everything if trying to show an invalid entry
			numBox.removeActionListener(this);
			numBox.setSelectedIndex(-1);
			numBox.setEnabled(false);
			numBox.addActionListener(this);
			ropeCheck.setEnabled(false);
			ropeCheck.setSelected(false);
			typeBox.removeActionListener(this);
			typeBox.setEnabled(false);
			typeBox.setSelectedIndex(0);
			typeBox.addActionListener(this);
			dirClimbBox.setEnabled(false);
			dirClimbBox.setSelectedIndex(0);
			delDoorButton.setEnabled(false);
			disableDestGUI();
		}
		else
		{
			EbMap.DoorLocation doorLocation = EbMap.getDoorLocation(x,y,num);
			delDoorButton.setEnabled(true);
			// update entry stuff?
			typeBox.removeActionListener(this);
			typeBox.setEnabled(true);
			typeBox.addActionListener(this);
			if (loadEntry)
			{
				typeBox.removeActionListener(this);
				typeBox.setSelectedIndex(doorLocation.getType());
				typeBox.addActionListener(this);
				int doorDestType = EbMap.getDoorDestType(doorLocation.getType());
				if (doorDestType == -1)
				{
					ropeCheck.setEnabled(true);
					ropeCheck.setSelected(doorLocation.isMiscRope());
					dirClimbBox.setEnabled(false);
					dirClimbBox.setSelectedIndex(0);
					numBox.removeActionListener(this);
					numBox.setEnabled(false);
					numBox.setSelectedIndex(-1);
					numBox.addActionListener(this);
				}
				else if (doorDestType == -2)
				{
					ropeCheck.setEnabled(false);
					ropeCheck.setSelected(false);
					dirClimbBox.setEnabled(true);
					dirClimbBox.setSelectedIndex(doorLocation.getMiscDirection());
					numBox.removeActionListener(this);
					numBox.setEnabled(false);
					numBox.setSelectedIndex(-1);
					numBox.addActionListener(this);
				}
				else if (doorDestType >= 0)
				{
					ropeCheck.setEnabled(false);
					ropeCheck.setSelected(false);
					dirClimbBox.setEnabled(false);
					dirClimbBox.setSelectedIndex(0);
					numBox.removeActionListener(this);
					numBox.setEnabled(true);
					numBox.setSelectedIndex(doorLocation.getDestIndex());
					numBox.addActionListener(this);
				}
			}
			
			//update destination's type?
			if (loadDestType
					&& (EbMap.getDoorDestType(typeBox.getSelectedIndex()) >= 0))
			{
				EbMap.Destination dest = EbMap.getDestination(numBox.getSelectedIndex());
				typeDestBox.removeActionListener(this);
				typeDestBox.setEnabled(true);
				typeDestBox.setSelectedIndex(dest.getType());
				typeDestBox.addActionListener(this);
			}
			
			//update destination properties?
			if (EbMap.getDoorDestType(typeBox.getSelectedIndex()) < 0)
			{
				warnLabel.setText("Type match.");
				disableDestGUI();
			}
			else
			{
				if (EbMap.getDoorDestType(typeBox.getSelectedIndex()) == typeDestBox.getSelectedIndex())
					warnLabel.setText("<html>Type match.</html>");
				else
					warnLabel.setText("<html><font color = \"red\"><u>TYPE MISMATCH!</u></font></html>");
				EbMap.Destination dest = EbMap.getDestination(numBox.getSelectedIndex());
				
				destPreview.setEnabled(typeDestBox.getSelectedIndex() == 0);
				
				if (typeDestBox.getSelectedIndex() == 0)
				{
					ptrField.setEnabled(true);
					dirBox.setEnabled(true);
					flagField.setEditable(true);
					reverseCheck.setEnabled(true);
					styleBox.setEnabled(true);
					setXYButton.setEnabled(true);
					jumpButton.setEnabled(true);
					
					if (loadDest)
					{
						ptrField.setOffset(dest.getPointer());
						destPreview.setMapXY(dest.getX(), dest.getY());
						destPreview.reloadMap();
						destPreview.updateComponents();
						destPreview.repaint();
		        		dirBox.setSelectedIndex(dest.getDirection());
		        		flagField.setText(Integer.toHexString(dest.getFlag()));
		        		reverseCheck.setSelected(dest.isFlagReversed());
		        		styleBox.setSelectedIndex(dest.getStyle());
					}
				}
				else if (typeDestBox.getSelectedIndex() == 1)
				{
					dirBox.setSelectedIndex(0);
					dirBox.setEnabled(false);
					styleBox.setSelectedIndex(0);
					styleBox.setEnabled(false);
					ptrField.setEnabled(true);
					setXYButton.setEnabled(false);
					jumpButton.setEnabled(false);
					flagField.setEditable(true);
					reverseCheck.setEnabled(true);
					
					if (loadDest)
					{
						ptrField.setOffset(dest.getPointer());
						flagField.setText(Integer.toHexString(dest.getFlag()));
		    			reverseCheck.setSelected(dest.isFlagReversed());
					}
				}
				else
				{
					dirBox.setSelectedIndex(0);
					flagField.setText(null);
					reverseCheck.setSelected(false);
					styleBox.setSelectedIndex(0);
					ptrField.setEnabled(true);
					setXYButton.setEnabled(false);
					jumpButton.setEnabled(false);
					dirBox.setEnabled(false);
					flagField.setEditable(false);
					reverseCheck.setEnabled(false);
					styleBox.setEnabled(false);
					
					if (loadDest)
						ptrField.setOffset(dest.getPointer());
				}
			}
		}

		destPreview.remoteRepaint();
		muteDL = false;
	}

	public void actionPerformed(ActionEvent e)
	{
	    if (e.getActionCommand().equals("apply"))
	    {
	    	int x = Integer.parseInt(areaXField.getText()),
				y = Integer.parseInt(areaYField.getText()),
				//num = Integer.parseInt(entryNumField.getText());
				num = entryNumBox.getSelectedIndex();
	    	if (EbMap.getDoorsNum(x,y) > num)
			{
	    		EbMap.DoorLocation doorLocation = EbMap.getDoorLocation(x,y,num);
	    		doorLocation.setType(
						(byte) typeBox.getSelectedIndex());
				if (ropeCheck.isEnabled())
	    			doorLocation.setMiscRope(ropeCheck.isSelected());
				else if (dirClimbBox.isEnabled())
					doorLocation.setMiscDirection(dirClimbBox.getSelectedIndex());
				if (numBox.isEnabled())
				{
					doorLocation.setDestIndex(numBox.getSelectedIndex());
		    		EbMap.Destination dest =
		    			EbMap.getDestination(
		    					doorLocation.getDestIndex());
		    		if (typeDestBox.isEnabled())
		    			dest.setType((byte) 
								typeDestBox.getSelectedIndex());
		    		if (ptrField.isEnabled())
		    			dest.setPointer(ptrField.getOffset());
		    		if (flagField.isEditable())
		    			dest.setFlag(
		        				(short) Integer.parseInt(
		        						flagField.getText(),16));
		    		if (reverseCheck.isEnabled())
		    			dest.setFlagReversed(reverseCheck.isSelected());
		    		if (destPreview.isEnabled())
		    		{
		    			dest.setX((short) destPreview.getMapX());
		    			dest.setY((short) destPreview.getMapY());
		    		}
		    		if (styleBox.isEnabled())
		    			dest.setStyle(
		        				(byte) styleBox.getSelectedIndex());
		    		if (dirBox.isEnabled())
		    			dest.setDirection(
		        				(byte) dirBox.getSelectedIndex());
				}
				updateComponents(true, true, true);
				boolean doorWrite = EbMap.writeDoors(this, false);
				if (! doorWrite)
	        		JOptionPane.showMessageDialog(mainWindow,
	            			"This is so embarassing!\n"
	            			+ "For some reason, I could not save "
	    					+ "the door data?\nThis shouldn't happen...");
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
	    else if (e.getSource() == jumpButton)
	    {
	    	net.starmen.pkhack.JHack.main.showModule(
        			MapEditor.class, new Integer[] {
        					new Integer(destPreview.getMapTileX()),
							new Integer(destPreview.getMapTileY())
        			});
	    }
		else if (e.getSource() == typeBox)
		{
			if ((! numBox.isEnabled())
					&& (EbMap.getDoorDestType(typeBox.getSelectedIndex()) >= 0))
			{
				numBox.removeActionListener(this);
				numBox.setEnabled(true);
				numBox.setSelectedIndex(0);
				numBox.addActionListener(this);
				updateComponents(true, false, true);
			}
			else
				updateComponents(false, false, false);
		}
		else if (e.getSource() == typeDestBox)
			updateComponents(true, false, false);
		else if (e.getSource().equals(entryNumBox))
			updateComponents(true, true, true);
		else if (e.getSource().equals(numBox))
			updateComponents(true, false, true);
		else if (e.getActionCommand().equals("addDoor")) {
			EbMap.addDoor(Integer.parseInt(areaXField.getText()),
					Integer.parseInt(areaYField.getText()),
					(short) 0, (short) 0, (byte) 5, (short) 0, 0);
			updateComponents(false, false, false);
			entryNumBox.setSelectedIndex(entryNumBox.getItemCount() - 1);
		} else if (e.getActionCommand().equals("delDoor")) {
			EbMap.removeDoor(Integer.parseInt(areaXField.getText()),
					Integer.parseInt(areaYField.getText()),
					entryNumBox.getSelectedIndex());
			entryNumBox.setSelectedIndex(0);
		}
	}
	
	public void returnSeek(int x, int y, int tileX, int tileY)
	{
		int newX = ((tileX * MapEditor.tileWidth) / 8) + (x / 8),
			newY = ((tileY * MapEditor.tileHeight) / 8) + (y / 8);
		destPreview.setMapXY(newX, newY);
		destPreview.updateComponents();
		destPreview.reloadMap();
		destPreview.repaint();
		setXYButton.setEnabled(true);
	}
}