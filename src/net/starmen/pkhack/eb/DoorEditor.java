package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
	private JComboBox typeBox, dirClimbBox, typeDestBox, styleBox, dirBox, entryNumBox, destNumBox;
	private JLabel warnLabel;
	private JButton setXYButton, jumpButton, delDoorButton, addDestButton, delDestButton;
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
		destPreview = new MapEditor.MapGraphics(this, 4, 2, 5, false, false, true, 8, true);
		createGUI();
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.3";
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
		reloadDestChooser();
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
		reloadDestChooser();
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
		
		DocumentListener listener = new DocumentListener()
		{
	        public void changedUpdate(DocumentEvent de)
	        {
	        	if (!muteDL
	        			//&& (destNumBox.getSelectedIndex() >= 0)
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
		
		//HackModule.createFlowLayout(comps)
		
		JComponent tmp;
		
		JPanel doorInfo = new JPanel();
		doorInfo.setLayout(new BoxLayout(doorInfo, BoxLayout.Y_AXIS));
		doorInfo.setBorder(BorderFactory.createTitledBorder("Door information"));
		
		areaXField = HackModule.createSizedJTextField(2, true);
		areaXField.getDocument().addDocumentListener(listener);
		areaYField = HackModule.createSizedJTextField(2, true);
		areaYField.getDocument().addDocumentListener(listener);
		entryNumBox = new JComboBox();
		entryNumBox.addActionListener(this);
		JButton button = new JButton("Add");
		button.addActionListener(this);
		button.setActionCommand("addDoor");
		delDoorButton = new JButton("Delete");
		delDoorButton.addActionListener(this);
		delDoorButton.setActionCommand("delDoor");
		doorInfo.add(HackModule.createFlowLayout(
				new Component[] {
						new JLabel("Area X:"), areaXField, 
						new JLabel("Area Y:"), areaYField,
						new JLabel("Num:"), entryNumBox,
						button, delDoorButton
				}));

		typeBox = new JComboBox(typeNames);
		typeBox.addActionListener(this);
		tmp = HackModule.createFlowLayout(new Component[] { new JLabel("Type:"), typeBox });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		doorInfo.add(tmp);
		
		ropeCheck = new JCheckBox();
		dirClimbBox = new JComboBox(climbDirections);
		tmp = HackModule.createFlowLayout(new Component[] {
				new JLabel("Rope:"), ropeCheck,
				new JLabel("Stairs direction:"), dirClimbBox });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		doorInfo.add(tmp);
		
		destNumBox = new JComboBox(); // the destination's number
		destNumBox.addActionListener(this);
		addDestButton = new JButton("Add");
		addDestButton.addActionListener(this);
		delDestButton = new JButton("Delete");
		delDestButton.addActionListener(this);
		tmp = HackModule.createFlowLayout(
				new Component[] {
						new JLabel("Destination:"), destNumBox,
						addDestButton, delDestButton
					});
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		doorInfo.add(tmp);
		
		JPanel destInfo = new JPanel();
		destInfo.setLayout(new BoxLayout(destInfo, BoxLayout.Y_AXIS));
		destInfo.setBorder(BorderFactory.createTitledBorder("Destination information"));
		
		typeDestBox = new JComboBox(typeDestNames);
		typeDestBox.addActionListener(this);
		tmp = HackModule.createFlowLayout(new Component[] { new JLabel("Type:"), typeDestBox });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		tmp = HackModule.createFlowLayout(new Component[] {
					new JLabel("X:"), destPreview.getXField(),
					new JLabel("Y:"), destPreview.getYField()
				});
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		dirBox = new JComboBox(new String[] { "Down", "Up", "Right", "Left" });
		tmp = HackModule.createFlowLayout(new Component[] { new JLabel("Direction:"), dirBox });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		styleBox = HackModule.createJComboBoxFromArray(TeleportTableEditor.warpStyleNames);
		tmp = HackModule.createFlowLayout(new Component[] { new JLabel("Style:"), styleBox });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		flagField = HackModule.createSizedJTextField(4, false);
		reverseCheck = new JCheckBox();
		tmp = HackModule.createFlowLayout(new Component[] {
				new JLabel("Flag:"), flagField,
				reverseCheck, new JLabel("Reverse") });
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		ptrField = new TextEditor.TextOffsetEntry("Text", true);
		tmp = HackModule.createFlowLayout(ptrField);
		((FlowLayout) tmp.getLayout()).setAlignment(FlowLayout.LEFT);
		destInfo.add(tmp);
		
		destInfo.add(HackModule.createFlowLayout(destPreview));
		
		setXYButton = new JButton("Seek");
		setXYButton.addActionListener(this);
		jumpButton = new JButton("Go");
		jumpButton.addActionListener(this);
		destInfo.add(HackModule.createFlowLayout(new Component[] {
				setXYButton, jumpButton
		}));
		
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.add(doorInfo);
		contentPanel.add(destInfo);
		warnLabel = new JLabel("Types match.");
		contentPanel.add(HackModule.createFlowLayout(warnLabel));
		
		this.mainWindow.getContentPane().add(contentPanel, BorderLayout.CENTER);
		this.mainWindow.pack();
	}
	
	private void disableDestGUI()
	{
		typeDestBox.setEnabled(false);
		ptrField.setEnabled(false);
		setXYButton.setEnabled(false);
		jumpButton.setEnabled(false);
		dirBox.setEnabled(false);
		flagField.setEditable(false);
		reverseCheck.setEnabled(false);
		styleBox.setEnabled(false);
		destPreview.setEnabled(false); 
	}
	
	private void reloadDestChooser() {
		destNumBox.removeActionListener(this);
		destNumBox.removeAllItems();
		for (int i = 0; i < EbMap.getNumDests(); i++)
			destNumBox.addItem(getNumberedString(EbMap.getDestination(i).toString(), i));
		destNumBox.addActionListener(this);
		delDestButton.setEnabled(EbMap.getNumDests() > 1);
	}

	private void updateComponents(boolean loadDest, boolean loadEntry, boolean loadDestType) {
		//muteDL = true;
		int x = Integer.parseInt(areaXField.getText()),
			y = Integer.parseInt(areaYField.getText()),
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
				|| (num < 0)) {
			//disable everything if trying to show an invalid entry
			destNumBox.removeActionListener(this);
			destNumBox.setSelectedIndex(-1);
			destNumBox.setEnabled(false);
			destNumBox.addActionListener(this);
			addDestButton.setEnabled(false);
			delDestButton.setEnabled(false);
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
		} else {
			EbMap.DoorLocation doorLocation = null;
			if (loadEntry)
				doorLocation = EbMap.getDoorLocation(x,y,num);
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
			}
			
			if (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] == -1)
			{
				ropeCheck.setEnabled(true);
				if (loadEntry)
					ropeCheck.setSelected(doorLocation.isMiscRope());
				else
					ropeCheck.setSelected(false);
				dirClimbBox.setEnabled(false);
				dirClimbBox.setSelectedIndex(-1);
				destNumBox.removeActionListener(this);
				destNumBox.setEnabled(false);
				destNumBox.addActionListener(this);
				addDestButton.setEnabled(false);
				delDestButton.setEnabled(false);
			}
			else if (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] == -2)
			{
				ropeCheck.setEnabled(false);
				ropeCheck.setSelected(false);
				dirClimbBox.setEnabled(true);
				if (loadEntry)
					dirClimbBox.setSelectedIndex(doorLocation.getMiscDirection());
				else
					dirClimbBox.setSelectedIndex(0);
				destNumBox.removeActionListener(this);
				destNumBox.setEnabled(false);
				destNumBox.addActionListener(this);
				addDestButton.setEnabled(false);
				delDestButton.setEnabled(false);
			}
			else if (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] >= 0)
			{
				ropeCheck.setEnabled(false);
				ropeCheck.setSelected(false);
				dirClimbBox.setEnabled(false);
				dirClimbBox.setSelectedIndex(-1);
				destNumBox.removeActionListener(this);
				destNumBox.setEnabled(true);
				if (loadEntry)
					destNumBox.setSelectedIndex(EbMap.getDestIndex(doorLocation.getDestination()));
				destNumBox.addActionListener(this);
				addDestButton.setEnabled(true);
				delDestButton.setEnabled(EbMap.getNumDests() > 1);
			}
			
			//update destination's type?
			if (loadDestType && (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] >= 0)) {
				EbMap.Destination dest = EbMap.getDestination(destNumBox.getSelectedIndex());
				typeDestBox.removeActionListener(this);
				typeDestBox.setEnabled(true);
				typeDestBox.setSelectedIndex(dest.getType());
				typeDestBox.addActionListener(this);
			}
			
			//update destination properties?
			if (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] < 0) {
				warnLabel.setText("Types match.");
				disableDestGUI();
			} else {
				if (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] == typeDestBox.getSelectedIndex())
					warnLabel.setText("Types match.");
				else
					warnLabel.setText("<html><font color = \"red\">Types do not match.</font></html>");
				EbMap.Destination dest = EbMap.getDestination(destNumBox.getSelectedIndex());
				
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
					dirBox.setEnabled(false);
					styleBox.setEnabled(false);
					ptrField.setEnabled(true);
					setXYButton.setEnabled(false);
					jumpButton.setEnabled(false);
					flagField.setEditable(true);
					reverseCheck.setEnabled(true);
					
					if (loadDest)
					{
						dirBox.setSelectedIndex(0);
						styleBox.setSelectedIndex(0);
						ptrField.setOffset(dest.getPointer());
						flagField.setText(Integer.toHexString(dest.getFlag()));
		    			reverseCheck.setSelected(dest.isFlagReversed());
		    			destPreview.setMapXY(0,0);
		    			destPreview.reloadMap();
					}
				}
				else
				{
					ptrField.setEnabled(true);
					setXYButton.setEnabled(false);
					jumpButton.setEnabled(false);
					dirBox.setEnabled(false);
					flagField.setEditable(false);
					reverseCheck.setEnabled(false);
					styleBox.setEnabled(false);
					
					if (loadDest) {
						dirBox.setSelectedIndex(0);
						flagField.setText("0");
						reverseCheck.setSelected(false);
						styleBox.setSelectedIndex(0);
						ptrField.setOffset(dest.getPointer());
						destPreview.setMapXY(0,0);
		    			destPreview.reloadMap();
					}
				}
			}
		}

		destPreview.repaint();
		//muteDL = false;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getActionCommand().equals("apply")) {
	    	if ((EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] != -1)
	    			&& (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] != typeDestBox.getSelectedIndex())) {
	    		JOptionPane.showMessageDialog(mainWindow,
	    				"The types of the door entry and the destination\n"
	    				+ "are not compatible. Please adjust this so that\n"
	    				+ "the editor reports that the \"Types match.\"\n"
	    				+ "at the bottom of the window.");
	    	} else if (rom.length() == AbstractRom.EB_ROM_SIZE_REGULAR) {
				int sure = JOptionPane.showConfirmDialog(mainWindow,
						"You need to expand your ROM to apply changes in the Map Editor.\n"
								+ "Do you want to?", "This ROM is not expanded",
						JOptionPane.YES_NO_OPTION);
				if (sure == JOptionPane.YES_OPTION) {
					this.askExpandType();
					actionPerformed(e);
				} else
					JOptionPane.showMessageDialog(mainWindow, "Changes were not applied.");
	    	} else {
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
					if (destNumBox.isEnabled())
					{
						doorLocation.setDestination(EbMap.getDestination(destNumBox.getSelectedIndex()));
			    		EbMap.Destination dest = doorLocation.getDestination();
			    		if (typeDestBox.isEnabled())
			    			dest.setType((byte) typeDestBox.getSelectedIndex());
			    		if (ptrField.isEnabled())
			    			dest.setPointer(ptrField.getOffset());
			    		if (flagField.isEditable())
			    			dest.setFlag((short) Integer.parseInt(flagField.getText(),16));
			    		if (reverseCheck.isEnabled())
			    			dest.setFlagReversed(reverseCheck.isSelected());
			    		if (destPreview.isEnabled()) {
			    			dest.setX((short) destPreview.getMapX());
			    			dest.setY((short) destPreview.getMapY());
			    		}
			    		if (styleBox.isEnabled())
			    			dest.setStyle((byte) styleBox.getSelectedIndex());
			    		if (dirBox.isEnabled())
			    			dest.setDirection((byte) dirBox.getSelectedIndex());
					}
					boolean doorWrite = EbMap.writeDoors(this, false);
					if (! doorWrite)
		        		JOptionPane.showMessageDialog(mainWindow,
		        				"I can not save the door data.\n"
		        				+ "This is usually because there is not enough space\n"
		        				+ "in the expanded area of your ROM. If this is so,\n"
		        				+ "either you have filled it up for a program has used\n"
		        				+ "up more space than it should have.\n"
		        				+ "If you have a recent backup or revert patch, you should\n"
		        				+ "try to use that and figure out what went wrong, then report\n"
		        				+ "it to a PK Hack developer.");
					else {
						reloadDestChooser();
						updateComponents(true, true, true);
					}
				}
	    	}
	    } else if (e.getActionCommand().equals("close")) {
	        hide();
	    } else if (e.getSource() == setXYButton) {
	    	setXYButton.setEnabled(false);
	    	net.starmen.pkhack.JHack.main.showModule(MapEditor.class, this);
	    } else if (e.getSource() == jumpButton) {
	    	net.starmen.pkhack.JHack.main.showModule(
        			MapEditor.class, new Integer[] {
        					new Integer(destPreview.getMapTileX()),
							new Integer(destPreview.getMapTileY())
        			});
	    } else if (e.getSource() == typeBox) {
			if ((! destNumBox.isEnabled())
					&& (EbMap.DOOR_DEST_TYPES[typeBox.getSelectedIndex()] >= 0))
			{
				destNumBox.removeActionListener(this);
				destNumBox.setEnabled(true);
				destNumBox.setSelectedIndex(0);
				destNumBox.addActionListener(this);
				updateComponents(true, false, true);
			}
			else
				updateComponents(false, false, false);
		} else if (e.getSource() == typeDestBox)
			updateComponents(true, false, false);
		else if (e.getSource().equals(entryNumBox))
			updateComponents(true, true, true);
		else if (e.getSource().equals(destNumBox))
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
		} else if (e.getSource().equals(addDestButton)) {
			EbMap.addNewDestination();
			reloadDestChooser();
			destNumBox.setSelectedIndex(EbMap.getNumDests() - 1);
		} else if (e.getSource().equals(delDestButton)) {
			int[] tmp;
			if (EbMap.getNumDests() <= 1)
				JOptionPane.showMessageDialog(mainWindow,
        				"PK Hack must have at least one destination available.");
			else if ((tmp = EbMap.findDoor(EbMap.getDestination(destNumBox.getSelectedIndex()))) != null)
				JOptionPane.showMessageDialog(mainWindow,
        				"You must remove all references to this destination from all\n" +
						"door entries before you can delete it.\n" +
						"This destination is still being used by door #" + tmp[2] + " in area " + tmp[0] + "," + tmp[1] + ".");
			else {
				int selected = destNumBox.getSelectedIndex();
				EbMap.removeDestination(selected);
				reloadDestChooser();
				destNumBox.setSelectedIndex(selected > EbMap.getNumDests() ? EbMap.getNumDests() - 1 : selected);
			}
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