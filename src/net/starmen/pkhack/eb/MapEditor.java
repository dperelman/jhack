package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
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
public class MapEditor extends EbHackModule implements ActionListener,
    PropertyChangeListener, ItemListener
{
    public MapEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
        doorEditor = new DoorEditor(rom, prefs);
    }
	
    private JFrame mainWindow;
    private int palette = 0;
    private int music = 0;
    public static int sectorWidth = 8;
    public static int sectorHeight = 4;
    public static int widthInSectors = 32;
    public static int heightInSectors = 80;
    public static int width = (widthInSectors * sectorWidth) - 1;
    public static int height = (heightInSectors * sectorHeight) - 1;
    public static int tileWidth = 32;
    public static int tileHeight = 32;
    private int screenWidth = 24;
    private int screenHeight = 12;
    private int editHeight = 3;
    private int editWidth = screenWidth - 1;
    private static final int draw_tsets = 20;
    private static final int map_tsets = 32;
    private static final int maxpals = 59;
    private NumberFormat num_format;
    private JFormattedTextField xField, yField, paletteField, musicField;
    private JComboBox tilesetList;
    private JScrollBar scrollh, scrollv, scrollh2;
    private JMenu modeMenu;
    private boolean changingSectors = false, movingSprite = false,
		movingDoor = false;
    private boolean userShown;
    private int[] movingSpriteInfo;
    private int[] movingDoorInfo;
    private short copyTpt = -1;

    public static final String[] TILESET_NAMES = {"Underworld", "Onett",
        "Twoson", "Threed", "Fourside", "Magicant", "Outdoors", "Summers",
        "Desert", "Dalaam", "Indoors 1", "Indoors 2", "Stores 1", "Caves 1",
        "Indoors 3", "Stores 2", "Indoors 4", "Winters", "Scaraba", "Caves 2"};

    // public static final int[] MAP_TILESETS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    // 10, 10, 11, 17, 10, 10, 10, 10, 18, 16, 12, 11, 11, 11, 15, 14, 19, 13,
    // 13, 13, 13, 0 };

    // private EbMap EbMap;
    private MapGraphics gfxcontrol;
    private EditBox editbox;
    private DoorEditor doorEditor;

    public JPanel createTopButtons()
    {
        JPanel panel = new JPanel(new FlowLayout());

        panel.add(new JLabel("X: "));
        // xField = new JFormattedTextField(num_format);
        xField.setValue(new Integer(gfxcontrol.getMapX()));
        xField.setColumns(3);
        xField.addPropertyChangeListener("value", this);
        panel.add(xField);

        panel.add(new JLabel("Y: "));
        // yField = new JFormattedTextField(num_format);
        yField.setValue(new Integer(gfxcontrol.getMapY()));
        yField.setColumns(3);
        yField.addPropertyChangeListener("value", this);
        panel.add(yField);

        panel.add(new JLabel("Tileset: "));
        String[] tList_names = new String[map_tsets];
        for (int i = 0; i < map_tsets; i++)
        {
            tList_names[i] = i + " - "
                + TILESET_NAMES[EbMap.getDrawTileset(rom, i)];
        }
        tilesetList = new JComboBox(tList_names);
        tilesetList.addItemListener(this);
        panel.add(tilesetList);

        panel.add(new JLabel("Palette: "));
        paletteField = new JFormattedTextField(num_format);
        paletteField.setValue(new Integer(palette));
        paletteField.setColumns(2);
        paletteField.addPropertyChangeListener("value", this);
        panel.add(paletteField);

        panel.add(new JLabel("Music: "));
        musicField = new JFormattedTextField(num_format);
        musicField.setValue(new Integer(music));
        musicField.setColumns(3);
        musicField.addPropertyChangeListener("value", this);
        panel.add(musicField);

        return panel;
    }

    public JMenuBar createMenuBar()
    {
    	MenuListener menuListener = new MenuListener();
        JMenuBar menuBar = new JMenuBar();
        ButtonGroup group = new ButtonGroup();
        JCheckBoxMenuItem checkBox;
        JRadioButtonMenuItem radioButton;
        
        JMenu menu = new JMenu("File");
        menu.add(EbHackModule.createJMenuItem(
        		"Save Changes", 's', null, 
				MenuListener.SAVE, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Exit", 'x', null, 
				MenuListener.EXIT, 
				menuListener));
        menuBar.add(menu);
		
		group = new ButtonGroup();
		radioButton = new JRadioButtonMenuItem("Map Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE0);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Sprite Edit");
		radioButton.setSelected(false);
		radioButton.setActionCommand(MenuListener.MODE1);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Door Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE2);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		modeMenu.add(radioButton);
        menuBar.add(modeMenu);
        
        menu = new JMenu("Tools");
        menu.add(EbHackModule.createJMenuItem(
        		"Delete All Sprites", 'd', null, 
				MenuListener.DEL_ALL_SPRITES, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Clear Tile Image Cache", 't', null, 
				MenuListener.RESET_TILE_IMAGES, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Clear Sprite Image Cache", 's', null, 
				MenuListener.RESET_SPRITE_IMAGES, 
				menuListener));
        menuBar.add(menu);
        
        menu = new JMenu("Options");
        checkBox = new JCheckBoxMenuItem("Show grid");
        checkBox.setMnemonic('g');
        checkBox.setSelected(true);
        checkBox.setActionCommand(MenuListener.GRIDLINES);
        checkBox.addActionListener(menuListener);
        menu.add(checkBox);
        checkBox = new JCheckBoxMenuItem("Show sprite boxes");
        checkBox.setMnemonic('b');
        checkBox.setSelected(true);
        checkBox.setActionCommand(MenuListener.SPRITEBOXES);
        checkBox.addActionListener(menuListener);
        menu.add(checkBox);
        menuBar.add(menu);

        return menuBar;
    }

    public void createGUI()
    {
        mainWindow = HackModule.createBaseWindow(this);
        mainWindow.setTitle(getDescription());
        mainWindow.addComponentListener(
        		new ComponentAdapter() {
        			public void componentResized(ComponentEvent e)
        			{
        				boolean effected = false;
        				int xDiff = ((int) gfxcontrol.getSize().getWidth())
							- (MapEditor.tileWidth * screenWidth) - 1;
        				/*
        				int oldScreenWidth = screenWidth;
        				int oldScreenHeight = screenHeight;
        				Dimension oldFrameSize = mainWindow.getSize();
        				 */
        				
        				if ((MapEditor.tileWidth <= xDiff) || (0 - MapEditor.tileWidth >= xDiff))
        				{
        					screenWidth = ((int) gfxcontrol.getSize().getWidth()) 
								/ MapEditor.tileHeight;
        					editWidth = screenWidth - 1;
        			        if (scrollh.getValue() + screenWidth > MapEditor.width + 1)
        			        	scrollh.setValue(MapEditor.width - screenWidth + 1);
        			        if (scrollh2.getValue() + editWidth > MapEditor.width + 1)
        			        	scrollh2.setValue(MapEditor.width - screenWidth + 1);
        			        scrollh.setVisibleAmount(screenWidth);
        			        scrollh2.setVisibleAmount(screenWidth);
        			        editbox.remoteRepaint();
        			        effected = true;
        				}
        				
        				int yDiff = ((int) gfxcontrol.getSize().getHeight())
							- (MapEditor.tileHeight * screenHeight) - 1;
        				if ((MapEditor.tileHeight <= yDiff) || (0 - MapEditor.tileHeight >= yDiff))
        				{
        					screenHeight = ((int) gfxcontrol.getSize().getHeight()) 
								/ MapEditor.tileHeight;
        			        if (scrollv.getValue() + screenHeight > MapEditor.height)
        			        	scrollv.setValue(MapEditor.height - screenHeight + 1);
        			        scrollv.setVisibleAmount(screenHeight);
        			        effected = true;
        				}
        				
        				if (effected)
        				{
        					gfxcontrol.setPreferredSize(
        							new Dimension(
        									(MapEditor.tileWidth * screenWidth) + 1,
											(MapEditor.tileHeight * screenHeight) + 1));
        			        /*
        			           mainWindow.setSize(
        			        		new Dimension(
        			        				(int) (oldFrameSize.getWidth() - 
        			        						((screenWidth - oldScreenWidth) 
        			        								* MapEditor.tileWidth)),
											(int) (oldFrameSize.getHeight() -
													((screenHeight - oldScreenHeight))
													* MapEditor.tileHeight)));
        			         */
        			        int[][] maparray = new int[screenHeight][screenWidth];
                            for (int i = 0; i < screenHeight; i++)
                            	maparray[i] = 
                                	EbMap.getTiles(rom, i + gfxcontrol.getMapY(),
                                			gfxcontrol.getMapX(), screenWidth);
                            gfxcontrol.setMapArray(maparray);        			        
        			        gfxcontrol.remoteRepaint();
        			        
        			        // This slows it down so much!
        			        // mainWindow.pack();
        				}
        			}
        		});

        scrollh.addAdjustmentListener(new ScrollListener());

        scrollv.addAdjustmentListener(new ScrollListener());
        
        scrollh2.addAdjustmentListener(new ScrollListener());

        gfxcontrol
            .setPreferredSize(new Dimension((MapEditor.tileWidth * screenWidth)
                + 1, (MapEditor.tileHeight * screenHeight) + 1));
        editbox.setPreferredSize(new Dimension(
        		(editWidth + 1) * MapEditor.tileWidth + 1,
        		(editHeight + 1) * MapEditor.tileHeight + 1));
        gfxListener gfxEars = new gfxListener();
        gfxcontrol.addMouseListener(gfxEars);
        gfxcontrol.addMouseMotionListener(gfxEars);
        gfxcontrol.addMouseWheelListener(gfxEars);
        editbox.addMouseListener(new editboxListener());
        
        JPanel top_buttons = createTopButtons();
        
        JPanel subpanel = new JPanel();
        
        JPanel mapgfxpanel = new JPanel(new BorderLayout());
        mapgfxpanel.add(gfxcontrol, BorderLayout.CENTER);
        mapgfxpanel.add(scrollv, BorderLayout.LINE_END);
        mapgfxpanel.add(scrollh, BorderLayout.PAGE_END);
        
        JPanel editpanel = new JPanel(new BorderLayout());
        editpanel.add(scrollh2, BorderLayout.PAGE_END);
        editpanel.add(editbox, BorderLayout.CENTER);
        
        /*BoxLayout layout = new BoxLayout(subpanel, BoxLayout.X_AXIS);
        subpanel.add(editpanel);
        subpanel.add(mapgfxpanel);*/
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        //mainWindow.getContentPane().setLayout(new BorderLayout());
        mainWindow.setJMenuBar(createMenuBar());
        contentPanel.add(top_buttons, BorderLayout.PAGE_START);
        contentPanel.add(mapgfxpanel, BorderLayout.CENTER);
        contentPanel.add(editpanel, BorderLayout.PAGE_END);
        mainWindow.getContentPane().add(contentPanel, BorderLayout.CENTER);
        
        mainWindow.pack();
    }
    
    protected void init()
    {
    	xField = new JFormattedTextField(num_format);
    	yField = new JFormattedTextField(num_format);
        scrollh = new JScrollBar(JScrollBar.HORIZONTAL, 0,
        		screenWidth, 0, MapEditor.width + 1);
        scrollv = new JScrollBar(JScrollBar.VERTICAL, 0,
        		screenHeight, 0, MapEditor.height + 1);
        scrollh2 = new JScrollBar(JScrollBar.HORIZONTAL, 0,
        		screenWidth, 0, 1024 / (editHeight + 1));
        modeMenu = new JMenu("Mode");
    	
        gfxcontrol = new MapGraphics(this, 0, true, true, 
        		xField, yField, scrollh, scrollv,
				modeMenu);
        editbox = new EditBox(scrollh2);

        createGUI();
    }
    
    public void writeToRom()
    {
    	if (rom.length() == AbstractRom.EB_ROM_SIZE_REGULAR)
    		JOptionPane.showMessageDialog(mainWindow,
    			"Please expand your rom first...\n"
    			+ "Either to 4 MB or 6 MB (reccomended).");
    	else
    	{
        	EbMap.loadSpriteData(rom);
        	EbMap.loadDoorData(rom);
        	
        	EbMap.writeMapChanges(rom);
        	EbMap.writeSectorData(rom);
        	boolean doorWrite = EbMap.writeDoors(this);
        	boolean spWrite = EbMap.writeSprites(this);
        	if (! doorWrite)
        		JOptionPane.showMessageDialog(mainWindow,
            			"This is so embarassing!\n"
            			+ "For some reason, I could not save "
    					+ "the door data?\nThis shouldn't happen...");
        	if (! spWrite)
        		JOptionPane.showMessageDialog(mainWindow,
            			"This is so embarassing!\n"
            			+ "For some reason, I could not save "
    					+ "the sprite data?\nThis shouldn't happen...");
        	if (doorWrite && spWrite)
        		JOptionPane.showMessageDialog(mainWindow,
        			"Saved successfully!");
        	EbMap.reset();
    	}
    }

    public void propertyChange(PropertyChangeEvent e)
    {
        if (!changingSectors)
        {
            Object source = e.getSource();
            if (source == xField)
            {
                int newx = ((Number) xField.getValue()).intValue();
                if ((newx >= 0) && (newx <= (MapEditor.width - screenWidth)))
                {
                    gfxcontrol.setMapX(newx);
                    gfxcontrol.remoteRepaint();
                }
            }
            else if (source == yField)
            {
                int newy = ((Number) yField.getValue()).intValue();
                if ((newy >= 0) && (newy <= (MapEditor.height - screenHeight)))
                {
                    gfxcontrol.setMapY(newy);
                    gfxcontrol.remoteRepaint();
                }
            }
            else if ((source == paletteField)
            		&& (gfxcontrol.knowsSector()))
            {
            	int newpal = ((Number) paletteField.getValue()).intValue();
            	if (((newpal >= 0) && (newpal <= maxpals)) &&
            			(gfxcontrol.getModeProps()[2] == 1))
            	{
            		palette = newpal;
                	int[] sectorxy = gfxcontrol.getSectorxy();
                	EbMap.setPal(sectorxy[0], sectorxy[1], newpal);
                	gfxcontrol.remoteRepaint();
            	}
            	else
            	{
            		paletteField.setValue(new Integer(palette));
            	}
            }
            else if ((source == musicField)
            		&& (gfxcontrol.knowsSector()))
            {
            	int newMusic = ((Number) musicField.getValue()).intValue();
            	if (((newMusic >= 0) && (newMusic <= 255)) &&
					(gfxcontrol.getModeProps()[2] == 1))
            	{
            		music = newMusic & 0xff;
                	int[] sectorxy = gfxcontrol.getSectorxy();
                	EbMap.setMusic(
                			sectorxy[0], sectorxy[1], music);
                	gfxcontrol.remoteRepaint();
            	}
            	else
            	{
            		musicField.setValue(new Integer(music));
            	}
            }
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("apply"))
        {
        	writeToRom();
        }
        else if (e.getActionCommand().equals("close"))
        {
            hide();
        }
    }
    
    public void itemStateChanged(ItemEvent e)
    {
    	if ((e.getSource() == tilesetList)
    			&& (e.getStateChange() == ItemEvent.SELECTED)
				&& (gfxcontrol.getModeProps()[2] == 1)
				&& (!changingSectors)
				&& (gfxcontrol.knowsSector()))
    	{
			int[] sectorxy = gfxcontrol.getSectorxy();
			EbMap.setTset(sectorxy[0], sectorxy[1],
					tilesetList.getSelectedIndex());		
			gfxcontrol.remoteRepaint();
			
			int tset = EbMap.getTset(sectorxy[0], sectorxy[1]);
			int pal = EbMap.getPal(sectorxy[0], sectorxy[1]);
            if (gfxcontrol.getModeProps()[2] == 1)
            {
            	boolean isSame = editbox.setTsetPal(
            			EbMap.getDrawTileset(rom, tset),
                        TileEditor.tilesets[EbMap.getDrawTileset(rom, 
                        		tset)].getPaletteNum(tset,
                        				pal));
            	if (! isSame)
            	{
            		editbox.remoteRepaint();
            	}
            }
    	}
    }
    
    class ScrollListener implements AdjustmentListener
	{
        public void adjustmentValueChanged(AdjustmentEvent ae)
        {
            Object source = ae.getSource();
            if (source == scrollh)
            {
                gfxcontrol.setMapX(ae.getValue());
                gfxcontrol.remoteRepaint();
            }
            else if (source == scrollv)
            {
                gfxcontrol.setMapY(ae.getValue());
                gfxcontrol.remoteRepaint();
            }
            else if (source == scrollh2)
            {
            	editbox.setScroll(ae.getValue());
            	editbox.remoteRepaint();
            }
        }
	}
    
    class MenuListener implements ActionListener
	{
    	public static final String SAVE = "save";
    	public static final String EXIT = "exit";
    	public static final String MODE0 = "mode0";
    	public static final String MODE1 = "mode1";
    	public static final String MODE2 = "mode2";
    	public static final String DEL_ALL_SPRITES =
    		"delAllSprites";
    	public static final String RESET_TILE_IMAGES =
    		"resetTileImages";
    	public static final String RESET_SPRITE_IMAGES =
    		"resetSpriteImages";
    	public static final String GRIDLINES =
    		"gridLines";
    	public static final String SPRITEBOXES =
    		"spriteBoxes";
    	
		public void actionPerformed(ActionEvent e)
		{
			String ac = e.getActionCommand();
			if (ac.equals(SAVE))
			{
				writeToRom();
			}
			else if (ac.equals(EXIT))
			{
				hide();
			}
			else if (ac.equals(MODE0))
			{
				gfxcontrol.changeMode(0);
	            gfxcontrol.remoteRepaint();
	            editbox.remoteRepaint();
			}
			else if (ac.equals(MODE1))
			{
				gfxcontrol.changeMode(1);
	            gfxcontrol.remoteRepaint();
	            editbox.remoteRepaint();
			}
			else if (ac.equals(MODE2))
			{
				gfxcontrol.changeMode(2);
				gfxcontrol.remoteRepaint();
				editbox.remoteRepaint();
			}
			else if (ac.equals(DEL_ALL_SPRITES))
			{
        		int sure = JOptionPane.showConfirmDialog(
        			    mainWindow,
        			    "Are you sure you want to "
						+ "delete all of the sprites?",
        			    "Are you sure?",
        			    JOptionPane.YES_NO_OPTION);
        		if (sure == JOptionPane.YES_OPTION)
        		{
        			EbMap.nullSpriteData();
        			gfxcontrol.remoteRepaint();
        		}
			}
			else if (ac.equals(RESET_TILE_IMAGES))
			{
				EbMap.resetTileImages();
				if (gfxcontrol.getModeProps()[1] >= 2)
				{
					gfxcontrol.remoteRepaint();
					editbox.remoteRepaint();
				}
			}
			else if (ac.equals(RESET_SPRITE_IMAGES))
			{
				EbMap.resetSpriteImages();
				if (gfxcontrol.getModeProps()[3] >= 1)
					gfxcontrol.remoteRepaint();
			}
			else if (ac.equals(GRIDLINES))
			{
				gfxcontrol.toggleGrid();
				gfxcontrol.remoteRepaint();
			}
			else if (ac.equals(SPRITEBOXES))
			{
				gfxcontrol.toggleSpriteBoxes();
				gfxcontrol.remoteRepaint();
			}
		}
	}
    
    class editboxListener implements MouseListener
	{
        public void mousePressed(MouseEvent e)
        {}

        public void mouseReleased(MouseEvent e)
        {}

        public void mouseEntered(MouseEvent e)
        {}

        public void mouseExited(MouseEvent e)
        {}

        public void mouseClicked(MouseEvent e)
        {
            if ((e.getButton() == 1)
            		&& (gfxcontrol.getModeProps()[2] == 1))
            {
            	if (e.getModifiers() == 18)
            	{
            		int tile = 
            			(((e.getX() / MapEditor.tileWidth) + editbox.getScroll())
            					* (editHeight + 1))
						+ (e.getY() / MapEditor.tileHeight);
            		net.starmen.pkhack.JHack.main.showModule(
            				TileEditor.class,
							new int[] {
            					editbox.getTset(),
								editbox.getPal(),
								tile
            				});
            	}
            	else
            	{
                	int mousex = e.getX();
                    int mousey = e.getY();
                    editbox.setSelected(mousex, mousey);
                    editbox.remoteRepaint();
            	}
            }
        }
	}

    class gfxListener implements MouseListener, 
			MouseMotionListener, MouseWheelListener
	{   	
    	public void mouseWheelMoved(MouseWheelEvent e)
    	{
            int y = gfxcontrol.getMapY() + e.getWheelRotation();
            if (y > MapEditor.height - screenHeight + 1)
            	y = MapEditor.height - screenHeight + 1;
            else if (y < 0)
            	y = 0;
            gfxcontrol.setMapY(y);
            gfxcontrol.remoteRepaint();
    	}
    	
    	public void mouseMoved(MouseEvent e)
    	{
    		if (gfxcontrol.getModeProps()[5] == 1)
    		{
    			gfxcontrol.setCrosshairs(e.getX(), e.getY());
    			gfxcontrol.remoteRepaint();
    		}
    	}
    	
    	public void mouseDragged(MouseEvent e)
    	{
    		if (movingSprite
    				&& (gfxcontrol.getModeProps()[3] == 1))
    		{
    			gfxcontrol.setSprite(
    					e.getX(), e.getY(),
						movingSpriteInfo[3], movingSpriteInfo[4]);
    			gfxcontrol.remoteRepaint();
    		}
    		else if (movingDoor
    				&& (gfxcontrol.getModeProps()[4] >= 2))
    		{
    			gfxcontrol.setMovingDoor(
    					e.getX(), e.getY());
    			gfxcontrol.remoteRepaint();
    		}
    	}
    	
        public void mousePressed(MouseEvent e)
        {
            if (e.getButton() == 1)
            {
            	if ((gfxcontrol.getModeProps()[3] == 1)
    					&& (! movingSprite))
            	{
            		int mousex = e.getX();
            		int mousey = e.getY();
                	int spNum = gfxcontrol.getSpriteNum(
                			mousex, mousey);
                	if (spNum != -1)
                	{
                    	int[] areaXY =
                    		gfxcontrol.getAreaXY(mousex, mousey);
                    	TPTEditor.TPTEntry tpt = 
                    		TPTEditor.tptEntries[EbMap.getSpriteTpt(
    							areaXY[0], areaXY[1], spNum)];
                		movingSpriteInfo = 
                			new int[] {
                				areaXY[0], areaXY[1], spNum,
    							tpt.getSprite(), tpt.getDirection()
                		};
                		movingSprite = true;
                	}
            	}
            	else if ((gfxcontrol.getModeProps()[4] >= 2)
            				&& (! movingDoor))
            	{
            		int doorNum = gfxcontrol.getDoorNum(
                			e.getX(), e.getY());
            		if (doorNum != -1)
            		{
            			int[] doorCoords =
                			gfxcontrol.getCoords(e.getX(), e.getY());
            			movingDoorInfo = new int[] {
            					doorCoords[0], doorCoords[1], doorNum
            			};
            			movingDoor = true;
            		}
            	}
            }
        }

        public void mouseReleased(MouseEvent e)
        {
        	if ((e.getButton() == 1)
        			&& (((gfxcontrol.getModeProps()[3] == 1) && movingSprite)
						|| ((gfxcontrol.getModeProps()[4] >= 2) && movingDoor)))
        	{
        		int mousex = e.getX();
        		int mousey = e.getY();
        		if (mousex < 0)
        		{
        			mousex = 0;
        		}
        		if (mousey < 0)
        		{
        			mousey = 0;
        		}
        		if (mousex > MapEditor.tileWidth * screenWidth)
        		{
        			mousex = MapEditor.tileWidth * screenWidth;
        		}
        		if (mousey > MapEditor.tileHeight * screenHeight)
        		{
        			mousey = MapEditor.tileHeight * screenHeight;
        		}
        		
        		if ((gfxcontrol.getModeProps()[3] == 1) && movingSprite)
        		{
        			short tpt =
            			EbMap.getSpriteTpt(movingSpriteInfo[0],
            					movingSpriteInfo[1],
    							movingSpriteInfo[2]);
            		int[] spLocXY =
            			gfxcontrol.getCoords(mousex, mousey);
            		if (spLocXY[0] >= MapEditor.widthInSectors)
            			spLocXY[0] = MapEditor.widthInSectors - 1;
            		else if (spLocXY[0] < 0)
            			spLocXY[0] = 0;
            		if (spLocXY[1] >= MapEditor.heightInSectors)
            			spLocXY[1] = MapEditor.heightInSectors - 1;
            		else if (spLocXY[1] < 0)
            			spLocXY[1] = 0;
            		EbMap.removeSprite(
            				movingSpriteInfo[0],
    						movingSpriteInfo[1],
            				movingSpriteInfo[2]);
            		EbMap.addSprite(spLocXY[0], spLocXY[1],
            				(short) spLocXY[2], (short) spLocXY[3],
    						tpt);
            		movingSprite = false;
            		gfxcontrol.remoteRepaint();
        		}
        		else if ((gfxcontrol.getModeProps()[4] >= 2) && movingDoor)
        		{
        			int[] coords =
        				gfxcontrol.getCoords(mousex, mousey);
        			EbMap.DoorLocation oldDoor =
        				EbMap.getDoorLocation(
        						movingDoorInfo[0],
								movingDoorInfo[1],
								movingDoorInfo[2]);
        			EbMap.removeDoor(
        					movingDoorInfo[0],
							movingDoorInfo[1],
							movingDoorInfo[2]);
        			EbMap.addDoor(
        					coords[0], coords[1],
							(short) (coords[2] / 8), (short) (coords[3] / 8),
							(byte) oldDoor.getType(),
							(short) oldDoor.getPointer(),
							oldDoor.getDestIndex());
        			movingDoor = false;
        			gfxcontrol.remoteRepaint();
        		}
        	}
        }

        public void mouseEntered(MouseEvent e)
        {}

        public void mouseExited(MouseEvent e)
        {}

        public void mouseClicked(MouseEvent e)
        {
        	if (e.getButton() == 1)
        	{
        		if (editbox.knowsTsetPal() 
        				&& (gfxcontrol.getModeProps()[2] == 1))
        		{
            		int mapx = e.getX() / MapEditor.tileWidth;
            		int mapy = e.getY() / MapEditor.tileHeight;
            		if (e.getModifiers() == 17)
            		{
            			int selected = EbMap.getTile(rom,
            					mapx + gfxcontrol.getMapX(),
								mapy + gfxcontrol.getMapY());
            			int selectedPos;
                		if (selected > (1024 / (editHeight + 1)) - editWidth)
                			selectedPos = (selected / (editHeight + 1)) - editWidth;
                		else
                			selectedPos = selected / (editHeight + 1);
            			editbox.setSelected(selected);
            			scrollh2.setValue(selectedPos);
            			editbox.remoteRepaint();
            		}
            		else if (editbox.isSelected())
            		{
                		int tile = editbox.getSelected();
                		int localtset = 0;
                		for (int i = 1; editbox.getSelected() - (i << 8) >= 0; i++)
                		{
                			tile -= (1 << 8);
                			localtset++;
                		}
                		
                		EbMap.changeTile(
                				gfxcontrol.getMapX() + mapx,
    							gfxcontrol.getMapY() + mapy, (byte) tile);
                		gfxcontrol.changeMapArray(mapx, mapy, tile);
                		EbMap.setLocalTileset(rom,
                				gfxcontrol.getMapX() + mapx,
    							gfxcontrol.getMapY() + mapy,
                				localtset);
                		gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (gfxcontrol.getMode() == 3)
        		{
        			doorEditor.setDestXY(
        					(gfxcontrol.getMapX() * 4) + (e.getX() / 8),
							(gfxcontrol.getMapY() * 4) + (e.getY() / 8));
        			doorEditor.enableSetXY();
        			gfxcontrol.changeMode(gfxcontrol.getOldMode());
        			gfxcontrol.remoteRepaint();
        		}
        		else if (gfxcontrol.getMode() == 4)
        		{
        			gfxcontrol.getSeekSource().returnSeek(
        					e.getX(), e.getY(),
        					gfxcontrol.getMapX(), gfxcontrol.getMapY());
        			gfxcontrol.changeMode(gfxcontrol.getOldMode());
        			if (userShown)
        				gfxcontrol.remoteRepaint();
        			else
        				hide();
        		}
        	}
            else if (e.getButton() == 3)
            {
            	if ((gfxcontrol.getModeProps()[3] == 1)
            			&& (! movingSprite))
            	{
            		JPopupMenu popup = new JPopupMenu();

                	int spNum = gfxcontrol.getSpriteNum(
                			e.getX(), e.getY());
                	if (spNum != -1)
                	{
                		int[] spLoc =
                			gfxcontrol.getCoords(e.getX(), e.getY());
                		PopupListener popupListener =
                			new PopupListener(spLoc[0], spLoc[1],
                					spLoc[2], spLoc[3], spNum);
                		popup.add(EbHackModule.createJMenuItem(
                        		"Add sprite", 'a', null, 
								PopupListener.ADD_SPRITE, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                        		"Delete sprite", 'd', null, 
								PopupListener.DEL_SPRITE, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                        		"Cut sprite", 'u', null, 
								PopupListener.CUT_SPRITE, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                        		"Copy sprite", 'c', null, 
								PopupListener.COPY_SPRITE, 
								popupListener));
                		if (copyTpt >= 0)
                			popup.add(EbHackModule.createJMenuItem(
                            		"Paste sprite", 'p', null, 
    								PopupListener.PASTE_SPRITE, 
    								popupListener));
                		String tpt = addZeros(Integer.toHexString(
                				EbMap.getSpriteTpt(
                				spLoc[0], spLoc[1], spNum)),4);
                		popup.add(EbHackModule.createJMenuItem(
                				"Switch TPT entry (" + tpt + ")",
								's', null,
								PopupListener.CHANGE_TPT, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                				"Edit TPT entry (" + tpt + ")",
								'e', null,
								PopupListener.EDIT_TPT, 
								popupListener));
                	}
                	else
                	{
                		int[] spLoc =
                			gfxcontrol.getCoords(e.getX(), e.getY());
                		PopupListener popupListener =
                			new PopupListener(spLoc[0], spLoc[1],
                					spLoc[2], spLoc[3]);
                		popup.add(EbHackModule.createJMenuItem(
                        		"Add sprite", 'a', null, 
								PopupListener.ADD_SPRITE, 
								popupListener));
                		if (copyTpt >= 0)
                			popup.add(EbHackModule.createJMenuItem(
                            		"Paste sprite", 'p', null, 
    								PopupListener.PASTE_SPRITE, 
    								popupListener));
                	}
                    popup.show(gfxcontrol, 
                    		e.getX(), 
							e.getY());
            	}
            	else if ((gfxcontrol.getModeProps()[4] == 2)
            				&& (! movingDoor))
            	{
               		JPopupMenu popup = new JPopupMenu();

                	int num = gfxcontrol.getDoorNum(
                			e.getX(), e.getY());
                	int[] doorCoords =
            			gfxcontrol.getCoords(e.getX(), e.getY());
                	if (num != -1)
                	{
                		PopupListener popupListener =
                			new PopupListener(doorCoords[0], doorCoords[1],
                					doorCoords[2], doorCoords[3], num);
                		popup.add(EbHackModule.createJMenuItem(
                        		"Add door", 'a', null, 
								PopupListener.ADD_DOOR, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                        		"Delete door", 'd', null, 
								PopupListener.DEL_DOOR, 
								popupListener));
                		/*popup.add(EbHackModule.createJMenuItem(
                        		"Cut sprite", 'u', null, 
								PopupListener.CUT_SPRITE, 
								popupListener));
                		popup.add(EbHackModule.createJMenuItem(
                        		"Copy sprite", 'c', null, 
								PopupListener.COPY_SPRITE, 
								popupListener));
                		if (copyTpt >= 0)
                			popup.add(EbHackModule.createJMenuItem(
                            		"Paste sprite", 'p', null, 
    								PopupListener.PASTE_SPRITE, 
    								popupListener));*/
                		String dest = Integer.toString(
                				EbMap.getDoorLocation(
                						doorCoords[0], doorCoords[1], num)
										.getDestIndex());
                		popup.add(EbHackModule.createJMenuItem(
                				"Edit entry (Destination #" + dest + ")",
								's', null,
								PopupListener.EDIT_DEST, 
								popupListener));
                	}
                	else
                	{
                		PopupListener popupListener =
                			new PopupListener(doorCoords[0], doorCoords[1],
                					doorCoords[2], doorCoords[3]);
                		popup.add(EbHackModule.createJMenuItem(
                        		"Add door", 'a', null, 
								PopupListener.ADD_DOOR, 
								popupListener));
                		/*if (copyTpt >= 0)
                			popup.add(EbHackModule.createJMenuItem(
                            		"Paste sprite", 'p', null, 
    								PopupListener.PASTE_SPRITE, 
    								popupListener));*/
                	}
                    popup.show(gfxcontrol, 
                    		e.getX(), 
							e.getY());
            	}
            	else if (gfxcontrol.getModeProps()[0] == 2)
            	{
            		changingSectors = true;
                    int mousex = e.getX();
                    int mousey = e.getY();
                    int sectorx = getSectorXorY(getTileXorY(mousex,
                        MapEditor.tileWidth) + gfxcontrol.getMapX(), sectorWidth);
                    int sectory = getSectorXorY(getTileXorY(mousey,
                        MapEditor.tileHeight) + gfxcontrol.getMapY(), MapEditor.sectorHeight);
                    
                    int tset = EbMap.getTset(sectorx, sectory);
                    int pal = EbMap.getPal(sectorx, sectory);
                    int music = EbMap.getMusic(sectorx, sectory);
                    int[] modeprops = gfxcontrol.getModeProps();
                    tilesetList.setSelectedIndex(tset);
                    paletteField.setValue(new Integer(pal));
                    musicField.setValue(new Integer(music));
                    
                    palette = pal;
                    
                    if (modeprops[2] == 1)
                    {
                    	boolean isSame = editbox.setTsetPal(
                    			EbMap.getDrawTileset(rom, tset),
                                TileEditor.tilesets[EbMap.getDrawTileset(rom, 
                                		tset)].getPaletteNum(tset,
                                				pal));
                    	if (! isSame)
                    	{
                    		editbox.remoteRepaint();
                    	}
                    }

                    gfxcontrol.setSector(sectorx, sectory);
                    gfxcontrol.remoteRepaint();
                    changingSectors = false;
            	}
            }
        }
        
        class PopupListener implements ActionListener
		{
        	public static final String ADD_SPRITE = "addSprite";
        	public static final String DEL_SPRITE = "delSprite";
        	public static final String CHANGE_TPT = "changeTpt";
        	public static final String EDIT_TPT = "editTpt";
        	public static final String COPY_SPRITE = "copySprite";
        	public static final String PASTE_SPRITE = "pasteSprite";
        	public static final String CUT_SPRITE = "cutSprite";
        	
        	public static final String ADD_DOOR = "addDoor";
        	public static final String DEL_DOOR = "delDoor";
        	public static final String EDIT_DEST = "editDest";
        	public static final String COPY_DOOR = "copyDoor";
        	public static final String CUT_DOOR = "cutDoor";
        	public static final String PASTE_DOOR = "pasteDoor";
        	
        	private int areaX, areaY, coordX, coordY, num, doorType, doorPtr;
	    	
        	public PopupListener(int areaX, int areaY,
	    			int coordX, int coordY, int num)
	    	{
	    		this.areaX = areaX;
	    		this.areaY = areaY;
	    		this.coordX = coordX;
	    		this.coordY = coordY;
	    		this.num = num;
	    	}
        	
        	public PopupListener(int areaX, int areaY,
	    			int coordX, int coordY)
	    	{
	    		this.areaX = areaX;
	    		this.areaY = areaY;
	    		this.coordX = coordX;
	    		this.coordY = coordY;
	    	}
        	
        	public void actionPerformed(ActionEvent e)
        	{
        		String ac = e.getActionCommand();
        		if (ac.equals(ADD_SPRITE))
        		{
        			EbMap.addSprite(areaX, areaY,
        					(short) coordX, (short) coordY,
							(short) 0);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(DEL_SPRITE))
        		{
            		int sure = JOptionPane.showConfirmDialog(
            			    mainWindow,
            			    "Are you sure you want to delete "
							+ "this sprite entry?\nThis action "
							+ "cannot be undone.",
            			    "Are you sure?",
            			    JOptionPane.YES_NO_OPTION);
            		if (sure == JOptionPane.YES_OPTION)
            		{
            			EbMap.removeSprite(areaX,
            					areaY, num);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(CHANGE_TPT))
        		{
            		short tpt = EbMap.getSpriteTpt(
            				areaX, areaY, num);
            		String input = JOptionPane.showInputDialog(
                            mainWindow,
                            "Change which TPT entry this"
							+ " sprite entry will display. (Hexidecimal input)",
                            Integer.toHexString((int) tpt));
            		if (input != null)
            		{
            			short newTpt = (short) Integer.parseInt(input,16);
            			short[] spriteXY = EbMap.getSpriteXY(
            					areaX, areaY, num);
            			EbMap.removeSprite(
            					areaX, areaY, num);
            			EbMap.addSprite(
            					areaX, areaY,
    							spriteXY[0], spriteXY[1],
    							newTpt);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(EDIT_TPT))
        		{
            		short tpt = EbMap.getSpriteTpt(
            				areaX, areaY, num);
        			net.starmen.pkhack.JHack.main.showModule(
                			TPTEditor.class, new Integer (tpt));
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(COPY_SPRITE))
        		{
        			copyTpt = EbMap.getSpriteTpt(
        					areaX, areaY, num);
        		}
        		else if (ac.equals(CUT_SPRITE))
        		{
        			copyTpt = EbMap.getSpriteTpt(
        					areaX, areaY, num);
        			EbMap.removeSprite(areaX,
        					areaY, num);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(PASTE_SPRITE))
        		{
        			EbMap.addSprite(areaX, areaY,
        					(short) coordX, (short) coordY,
							copyTpt);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(ADD_DOOR))
        		{
        			EbMap.addDoor(areaX, areaY,
        					(short) (coordX / 8), (short) (coordY / 8),
							(byte) 0, (byte) 0);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(DEL_DOOR))
        		{
        			EbMap.removeDoor(areaX, areaY, num);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(EDIT_DEST))
        		{
        			EbMap.DoorLocation doorLocation =
        				EbMap.getDoorLocation(areaX, areaY, num);
        			doorEditor.show(doorLocation);
        		}
        	}
		}
	}

    public int getSectorXorY(int tilexory, int sector_woh)
    {
    	return tilexory / sector_woh;
    }

    public int getTileXorY(int mousexory, int tile_woh)
    {
    	return mousexory / tile_woh;
    }

    public void show()
    {
    	show(true);
    }
    
    public void show(boolean userShown)
    {
        super.show();
        // this.reset();
        mainWindow.setVisible(true);
        mainWindow.repaint();
        
        gfxcontrol.remoteRepaint();
        
        TPTEditor.readFromRom(this);
        SpriteEditor.readFromRom(rom);
        EbMap.loadDoorData(rom);
        
        if (! this.userShown)
        	this.userShown = userShown;
    }
    
    public void show(Object source)
    {
    	SeekListener seekSource = (SeekListener) source;
    	show(false);
    	gfxcontrol.setSeekSource(seekSource);
    	gfxcontrol.changeMode(4);
    	gfxcontrol.remoteRepaint();
    }

    public void hide()
    {
        mainWindow.setVisible(false);
        doorEditor.hide();
        userShown = false;
    }
    
    public void reset()
    {
    	EbMap.reset();
    	gfxcontrol.setMapXY(
    			gfxcontrol.getMapX(), gfxcontrol.getMapY());
    }

    public String getDescription()
    {
        return "Map Editor";
    }

    public String getVersion()
    {
        return "0.3";
    }

    public String getCredits()
    {
        return "Written by Mr. Tenda\n"
            + "Original Map Editor written by Mr. Accident\n"
            + "Very special thanks to AnyoneEB\n"
            + "Additional features by YOURNAMEHERE\n";
    }

    // Controls the graphics stuff.
    public class MapGraphics extends AbstractButton
    {
    	private HackModule hm;
        private int[][] maparray;
        private boolean maparrayisset = false;
        // modes: 0 = map editing (text), 1 = map editing (graphics)
        private int x = 0, y = 0, mode, sectorx, sectory, crossX, crossY, oldMode;
        private JFormattedTextField xField, yField;
        private JScrollBar xScroll, yScroll;
        private JMenu modeMenu;
        private boolean grid, spriteBoxes;
        private SeekListener seekSource;
        
        private boolean knowsmap = false;
        private boolean knowssector = false;
        // This variable should define what a mode does.
        private int[][] modeprops = new int[][]{
        // { allow sectors to be selectable with right-click (0 = no, 1 = yes, 2
            // = change sector vars too), draw map (0=no, 1=text tiles, 2=gfx
            // tiles),
            //   allow map editing, sprite editing, door (1=view,2=edit)
        	//	draw crosshairs and set XY for DoorEditor, disable modeMenu}
        		
            // {2, 1, 0, 0},
			// {2, 2, 0, 0}, 
			{2, 2, 1, 0, 0, 0, 0}, 
			{0, 2, 0, 1, 0, 0, 0},
			{0, 2, 0, 0, 2, 0, 0},
			{0, 2, 0, 0, 1, 1, 1},
			{0, 2, 0, 0, 0, 1, 1}
			};
        private int[] spriteProps, doorProps;

        public MapGraphics(HackModule hm, int mode, boolean grid,
        		boolean spriteBoxes, JFormattedTextField xField,
        		 JFormattedTextField yField,JScrollBar xScroll,
				 JScrollBar yScroll, JMenu modeMenu)
        {
        	this.hm = hm;
            this.mode = mode;
            this.grid = grid;
            this.spriteBoxes = spriteBoxes;
            this.xField = xField;
            this.yField = yField;
            this.xScroll = xScroll;
            this.yScroll = yScroll;
            this.modeMenu = modeMenu;
            setMapXY(0,0);
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            if (this.knowsmap)
            {
                drawMap(g, g2d);
            }
    		if (movingSprite
    				&& (getModeProps()[3] == 1))
    		{
    			Image spImage =
    				EbMap.getSpriteImage(spriteProps[2], spriteProps[3]);
    			g.drawImage(
    					spImage, spriteProps[0], spriteProps[1],
						this);
    			if (spriteBoxes)
    			{
    				g2d.setPaint(Color.red);
    				g2d.draw(new Rectangle2D.Double(
                			spriteProps[0] - 1,
    						spriteProps[1] - 1,
    						spImage.getWidth(this) + 1,
    						spImage.getHeight(this) + 1));
    			}
    		}
    		if (movingDoor
    				&& (getModeProps()[4] >= 2))
    		{
            	g2d.setPaint(Color.blue);
            	g2d.draw(new Rectangle2D.Double(
            			doorProps[0], doorProps[1],
						8,8));
    		}
        }

        private void drawMap(Graphics g, Graphics2D g2d)
        {
            if (getModeProps()[1] == 1)
            {
                g2d.setPaint(Color.black);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                for (int i = 0; i < maparray.length; i++)
                {
                    int[] row2draw = maparray[i];
                    for (int i2 = 0; i2 < row2draw.length; i2++)
                    {
                        g2d.drawString(addZeros(Integer
                            .toHexString(row2draw[i2]), 2), (i2 * MapEditor.tileWidth)
							+ (MapEditor.tileWidth / 2), (i * MapEditor.tileHeight)
                            + (MapEditor.tileHeight / 2));
                        if (grid)
                        	g2d.draw(new Rectangle2D.Double(i2 * MapEditor.tileWidth,
                                    i * MapEditor.tileHeight, MapEditor.tileWidth, MapEditor.tileHeight));
                    }
                }
            }
            else if (getModeProps()[1] == 2)
            {
                int tile_set, tile_tile, tile_pal;
                for (int i = 0; i < maparray.length; i++)
                {
                    int[] row2draw = maparray[i];
                    for (int i2 = 0; i2 < row2draw.length; i2++)
                    {
                    	int sectorX = (i2 + x) / sectorWidth;
                    	int sectorY = (i + y) / MapEditor.sectorHeight;
                    	if (! EbMap.isSectorDataLoaded(
                    			sectorX, sectorY))
                    	{
                    		EbMap.loadSectorData(rom,
                    				sectorX, sectorY);
                    	}
                        tile_set = EbMap.getDrawTileset(rom, 
                        		EbMap.getTset(sectorX, sectorY));
                        tile_tile = row2draw[i2];
                        tile_pal = 
                        	TileEditor.tilesets[tile_set].getPaletteNum(
                        			EbMap.getTset(sectorX, sectorY),
									EbMap.getPal(sectorX, sectorY));
                        EbMap.loadTileImage(tile_set, tile_tile, tile_pal);

                        g.drawImage(
                            EbMap.getTileImage(tile_set,tile_tile,tile_pal),
                            i2 * MapEditor.tileWidth, i * MapEditor.tileHeight,
							MapEditor.tileWidth, MapEditor.tileHeight, this);
                        if (grid)
                        	g2d.draw(new Rectangle2D.Double(i2 * MapEditor.tileWidth,
                            		(i * MapEditor.tileHeight), MapEditor.tileWidth, MapEditor.tileHeight));
                    }
                }

            }

            if ((getModeProps()[0] >= 1) && this.knowssector)
            {
                g2d.setPaint(Color.yellow);
                
                int drawSectorX =
                	(sectorx * MapEditor.sectorWidth * MapEditor.tileWidth)
					- (MapEditor.tileWidth * x);
                int drawSectorY =
                	(sectory * MapEditor.sectorHeight * MapEditor.tileHeight)
					- (MapEditor.tileWidth * y);
            	int drawSectorW =
            		MapEditor.sectorWidth * MapEditor.tileWidth;
            	int drawSectorH =
            		MapEditor.sectorHeight * MapEditor.tileHeight;
              
                g2d.draw(new Rectangle2D.Double(drawSectorX,
                    drawSectorY, drawSectorW, drawSectorH));
            }
            
            if (getModeProps()[3] == 1)
            {
            	// this.spriteLocs = new int[spriteData[0]][5];
            	for (int k = 0; k < screenHeight; k++)
            	{
            		if ((((y + k) % (MapEditor.sectorHeight * 2)) == 0)
            				|| (k == 0))
            		{
            			for (int i = 0; i < screenWidth; i++)
                    	{
                    		if ((((x + i) % MapEditor.sectorWidth) == 0)
                    				|| (i == 0))
                    		{
                    			if (! EbMap.isSpriteDataLoaded(
                    					(x + i) / MapEditor.sectorWidth,
										(y + k) / (MapEditor.sectorHeight * 2)))
                    			{
                    				EbMap.loadSpriteData(rom,
                    						(x + i) / MapEditor.sectorWidth,
											(y + k) / (MapEditor.sectorHeight * 2));
                    			}
                    			int spritesNum = EbMap.getSpritesNum(
                    					(x + i) / MapEditor.sectorWidth, (y + k) / (MapEditor.sectorHeight * 2));
                            	short[][] spriteLocs = EbMap.getSpriteLocs(
                            			(x + i) / MapEditor.sectorWidth, (y + k) / (MapEditor.sectorHeight * 2));
                            	short[] spriteTpts = EbMap.getSpriteTpts(
                            			(x + i) / MapEditor.sectorWidth, (y + k) / (MapEditor.sectorHeight * 2));
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
                                	
                                	if (((y + k) % (MapEditor.sectorHeight * 2)) > 0)
                                	{
                                		spriteDrawY -= ((y + k) % (MapEditor.sectorHeight * 2)) *
												MapEditor.tileHeight;
                                	}
                                	
                                	if (((x + i) % MapEditor.sectorWidth) > 0)
                                	{
                                		spriteDrawX -= ((x + i) % MapEditor.sectorWidth)
												* MapEditor.tileWidth;
                                	}
                                	
                                	g.drawImage(
                                			EbMap.getSpriteImage(
                                					spriteNum,tptEntry.getDirection()),
                							spriteDrawX + (i * MapEditor.tileWidth),
        									spriteDrawY + (k * MapEditor.tileHeight),
											this);
                                	if (spriteBoxes)
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
                                	}
                                }
                    		}
                    	}
            		}
            	}
            }
            
            if (getModeProps()[4] >= 1)
            {
            	for (int k = 0; k < screenHeight; k++)
            	{
            		if ((((y + k) % MapEditor.sectorHeight) == 0)
            				|| (k == 0))
            		{
            			for (int i = 0; i < screenWidth; i++)
                    	{
                    		if ((((x + i) % MapEditor.sectorWidth) == 0)
                    				|| (i == 0))
                    		{
                    			if (! EbMap.isDoorDataLoaded(
                    					(x + i) / MapEditor.sectorWidth,
										(y + k) / (MapEditor.sectorHeight * 2)))
                    			{
                    				EbMap.loadDoorData(rom,
                    						(x + i) / MapEditor.sectorWidth,
											(y + k) / (MapEditor.sectorHeight * 2));
                    			}
                    			int doorsNum = EbMap.getDoorsNum(
                    					(x + i) / MapEditor.sectorWidth,
										(y + k) / (MapEditor.sectorHeight * 2));

                                for (int j = 0; j < doorsNum; j++)
                                {
                                	short[] doorXY = EbMap.getDoorXY(
                                			(x + i) / MapEditor.sectorWidth,
											(y + k) / (MapEditor.sectorHeight * 2),
											j);
                                	int doorDrawX = ((int) doorXY[0]) * 8;
                                	int doorDrawY = ((int) doorXY[1]) * 8;
                                	
                                	if (((y + k) % (MapEditor.sectorHeight * 2)) > 0)
                                	{
                                		doorDrawY -= ((y + k) % (MapEditor.sectorHeight * 2)) *
												MapEditor.tileHeight;
                                	}
                                	
                                	if (((x + i) % MapEditor.sectorWidth) > 0)
                                	{
                                		doorDrawX -= ((x + i) % MapEditor.sectorWidth)
												* MapEditor.tileWidth;
                                	}
                                	
                                	g2d.setPaint(Color.blue);
                                	g2d.draw(new Rectangle2D.Double(
                                			doorDrawX + (i * MapEditor.tileWidth),
											doorDrawY + (k * MapEditor.tileHeight),
											8,8));
                                }
                    		}
                    	}
            		}
            	}
            }
            
            if (getModeProps()[5] == 1)
            {
            	g2d.setPaint(Color.blue);
            	g2d.draw(new Line2D.Double(
            			0, crossY, this.getWidth(), crossY));
            	g2d.draw(new Line2D.Double(
            			crossX, 0, crossX, this.getHeight()));
            }
        }
        
        public void setSprite(int spriteX, int spriteY,
        		int spt, int direction)
        {
        	spriteProps = new int[] {
        			spriteX, spriteY, spt, direction
        	};
        }
        
        public void setMovingDoor(int doorX, int doorY)
        {
        	doorProps = new int[] { doorX, doorY };
        }
        
        public int getSpriteNum(int spriteX, int spriteY)
        {
        	int[] spLocXY = getCoords(spriteX, spriteY);
        	
        	return EbMap.findSprite(hm,
        			spLocXY[0], spLocXY[1],
					(short) spLocXY[2], (short) spLocXY[3]);
        }
        
        public short getSpriteTpt(int spriteX, int spriteY)
        {
        	int[] spLocXY = getCoords(spriteX, spriteY);
        	
        	int spriteNum = EbMap.findSprite(hm,
        			spLocXY[0], spLocXY[1],
					(short) spLocXY[2], (short) spLocXY[3]);
        	if (spriteNum == -1)
        	{
        		return -1;
        	}
        	else
        	{
        		return EbMap.getSpriteTpt(
        				spLocXY[0], spLocXY[1], spriteNum);
        	}
        }
        
        public int getDoorNum(int doorX, int doorY)
        {
        	int[] doorXY = getCoords(doorX, doorY);
        	
        	return EbMap.findDoor(
        			doorXY[0], doorXY[1],
					(short) doorXY[2], (short) doorXY[3]);
        }
        
        public int[] getAreaXY(int spriteX, int spriteY)
        {
        	int areaX = 
        		((spriteX / MapEditor.tileWidth) + x) / MapEditor.sectorWidth;
        	int areaY = 
        		((spriteY / MapEditor.tileHeight) + y) / (MapEditor.sectorHeight * 2);
        	return new int[] { areaX, areaY };
        }
        
        public int[] getCoords(int coordX, int coordY)
        {
        	int areaX = ((coordX / MapEditor.tileWidth) + x) / MapEditor.sectorWidth;
        	int areaY = ((coordY / MapEditor.tileHeight) +y) / (MapEditor.sectorHeight * 2);
        	if ((x % MapEditor.sectorWidth) > 0)
        	{
        		if (((coordX / MapEditor.tileWidth) / MapEditor.sectorWidth) == 0)
        		{
        			coordX += (x % MapEditor.sectorWidth) * MapEditor.tileWidth;
        		}
        		else
        		{
            		coordX -= (MapEditor.sectorWidth - 
            				(x % MapEditor.sectorWidth)) * MapEditor.tileWidth;
        		}
        	}
    		coordX -= (((coordX / MapEditor.tileWidth) / MapEditor.sectorWidth)
    				* MapEditor.sectorWidth * MapEditor.tileWidth);
        	if ((y % (MapEditor.sectorHeight * 2)) > 0)
        	{
        		if (((coordY / MapEditor.tileHeight) / (MapEditor.sectorHeight * 2)) == 0)
        		{
        			coordY += (y % (MapEditor.sectorHeight * 2)) * MapEditor.tileHeight;
        		}
        		else
        		{
        			coordY -= ((MapEditor.sectorHeight * 2) - 
            				(y % (MapEditor.sectorHeight * 2))) * MapEditor.tileHeight;
        		}
        	}
        	coordY -= (((coordY / MapEditor.tileHeight) / (MapEditor.sectorHeight * 2))
        			* (MapEditor.sectorHeight * 2) * MapEditor.tileHeight);
        	return new int[] { areaX, areaY, coordX, coordY };
        }

        public void setSector(int newsectorx, int newsectory)
        {
            if (this.knowssector && (this.sectorx == newsectorx)
                && (this.sectory == newsectory))
            {
                this.knowssector = false;
            }
            else
            {
                if (!this.knowssector)
                {
                    this.knowssector = true;
                }
                this.sectorx = newsectorx;
                this.sectory = newsectory;
            }
        }
        
        public boolean knowsSector()
        {
        	return this.knowssector;
        }
        
        public int[] getSectorxy()
        {
        	return new int[] { sectorx, sectory };
        }

        public void changeMode(int mode)
        {
        	this.oldMode = this.mode;
            this.mode = mode;
            if (getModeProps()[6] >= 1)
            	modeMenu.setEnabled(false);
            else
            	modeMenu.setEnabled(true);
        }

        public void setMapArray(int[][] newmaparray)
        {
            if (!this.knowsmap)
            {
                this.knowsmap = true;
            }
            this.maparray = newmaparray;
        }
        
        public void changeMapArray(int mapx, int mapy, int tile)
        {
        	this.maparray[mapy][mapx] = tile;
        }

        public void remoteRepaint()
        {
            repaint();
        }

        public String[] getModeNames()
        {
            return new String[]{"Map Viewing (text), Map Viewing (graphical)"};
        }

        public int getMode()
        {
            return this.mode;
        }
        
        public int getOldMode()
        {
        	return oldMode;
        }

        public int[] getModeProps()
        {
            return this.modeprops[this.mode];
        }
        
        public int[] getModeProps(int mode2get)
        {
        	return this.modeprops[mode2get];
        }
        
        public void toggleGrid()
        {
        	grid = ! grid;
        }
        
        public void toggleSpriteBoxes()
        {
        	spriteBoxes = ! spriteBoxes;
        }
        
        public void setCrosshairs(int crossX, int crossY)
        {
        	this.crossX = crossX;
        	this.crossY = crossY;
        }
        
        public void setMapXY(int x, int y)
        {
        	if (x + screenWidth > MapEditor.width + 1)
        		this.x = MapEditor.width - screenWidth + 1;
        	else
        		this.x = x;
        	xField.setText(Integer.toString(this.x));
        	xScroll.setValue(this.x);
        	
        	if (y + screenHeight > MapEditor.height + 1)
        		this.y = MapEditor.height - screenHeight + 1;
        	else
        		this.y = y;
        	yField.setText(Integer.toString(this.y));
        	yScroll.setValue(this.y);
        	
        	int[][] maparray = new int[screenHeight][screenWidth];
            for (int i = 0; i < screenHeight; i++)
            {
                maparray[i] = EbMap.getTiles(rom, 
                		i + y, this.x, screenWidth);
            }
            setMapArray(maparray);
        }
        
        public void setMapX(int x)
        {
        	setMapXY(x, this.y);
        }
        
        public int getMapX()
        {
        	return x;
        }
        
        public void setMapY(int y)
        {
        	setMapXY(this.x, y);
        }
        
        public int getMapY()
        {
        	return y;
        }
        
        public void setSeekSource(SeekListener seekSource)
        {
        	this.seekSource = seekSource;
        }
        
        public SeekListener getSeekSource()
        {
        	return seekSource;
        }
    }

    // Represents the whole EarthBound map and map-related data in the rom.
    public static class EbMap
    {
        private static final int[] mapAddresses = new int[]{
        // Taken from MrA's Map Editor code. DESPERATELY NEEDS TO BE
            // FORMATTED!!!!!!
            0x160200, // 1
            0x162A00, // 2
            0x165200, // 3
            0x168200, // 4
            0x16AA00, // 5
            0x16D200, // 6
            0x170200, // 7
            0x172A00, // 8

            0x160300, // 9
            0x162B00, // 10
            0x165300, // 11
            0x168300, // 12
            0x16AB00, // 13
            0x16D300, // 14
            0x170300, // 15
            0x172B00, // 16

            0x160400, // 17
            0x162C00, // 18
            0x165400, // 19
            0x168400, // 20
            0x16AC00, // 21
            0x16D400, // 22
            0x170400, // 23
            0x172C00, // 24

            0x160500, // 25
            0x162D00, // 26
            0x165500, // 27
            0x168500, // 28
            0x16AD00, // 29
            0x16D500, // 30
            0x170500, // 31
            0x172D00, // 32

            0x160600, // 33
            0x162E00, // 34
            0x165600, // 35
            0x168600, // 36
            0x16AE00, // 37
            0x16D600, // 38
            0x170600, // 39
            0x172E00, // 40

            0x160700, // 41
            0x162F00, // 42
            0x165700, // 43
            0x168700, // 44
            0x16AF00, // 45
            0x16D700, // 46
            0x170700, // 47
            0x172F00, // 48

            0x160800, // 49
            0x163000, // 50
            0x165800, // 51
            0x168800, // 52
            0x16B000, // 53
            0x16D800, // 54
            0x170800, // 55
            0x173000, // 56

            0x160900, // 57
            0x163100, // 58
            0x165900, // 59
            0x168900, // 60
            0x16B100, // 61
            0x16D900, // 62
            0x170900, // 63
            0x173100, // 64

            0x160A00, // 65
            0x163200, // 66
            0x165A00, // 67
            0x168A00, // 68
            0x16B200, // 69
            0x16DA00, // 70
            0x170A00, // 71
            0x173200, // 72

            0x160B00, // 73
            0x163300, // 74
            0x165B00, // 75
            0x168B00, // 76
            0x16B300, // 77
            0x16DB00, // 78
            0x170B00, // 79
            0x173300, // 80

            0x160C00, // 81
            0x163400, // 82
            0x165C00, // 83
            0x168C00, // 84
            0x16B400, // 85
            0x16DC00, // 86
            0x170C00, // 87
            0x173400, // 88

            0x160D00, // 89
            0x163500, // 90
            0x165D00, // 91
            0x168D00, // 92
            0x16B500, // 93
            0x16DD00, // 94
            0x170D00, // 95
            0x173500, // 96

            0x160E00, // 97
            0x163600, // 98
            0x165E00, // 99
            0x168E00, // 100
            0x16B600, // 101
            0x16DE00, // 102
            0x170E00, // 103
            0x173600, // 104

            0x160F00, // 105
            0x163700, // 106
            0x165F00, // 107
            0x168F00, // 108
            0x16B700, // 109
            0x16DF00, // 110
            0x170F00, // 111
            0x173700, // 112

            0x161000, // 113
            0x163800, // 114
            0x166000, // 115
            0x169000, // 116
            0x16B800, // 117
            0x16E000, // 118
            0x171000, // 119
            0x173800, // 120

            0x161100, // 121
            0x163900, // 122
            0x166100, // 123
            0x169100, // 124
            0x16B900, // 125
            0x16E100, // 126
            0x171100, // 127
            0x173900, // 128

            0x161200, // 129
            0x163A00, // 130
            0x166200, // 131
            0x169200, // 132
            0x16BA00, // 133
            0x16E200, // 134
            0x171200, // 135
            0x173A00, // 136

            0x161300, // 137
            0x163B00, // 138
            0x166300, // 139
            0x169300, // 140
            0x16BB00, // 141
            0x16E300, // 142
            0x171300, // 143
            0x173B00, // 144

            0x161400, // 145
            0x163C00, // 146
            0x166400, // 147
            0x169400, // 148
            0x16BC00, // 149
            0x16E400, // 150
            0x171400, // 151
            0x173C00, // 152

            0x161500, // 153
            0x163D00, // 154
            0x166500, // 155
            0x169500, // 156
            0x16BD00, // 157
            0x16E500, // 158
            0x171500, // 159
            0x173D00, // 160

            0x161600, // 161
            0x163E00, // 162
            0x166600, // 163
            0x169600, // 164
            0x16BE00, // 165
            0x16E600, // 166
            0x171600, // 167
            0x173E00, // 168

            0x161700, // 169
            0x163F00, // 170
            0x166700, // 171
            0x169700, // 172
            0x16BF00, // 173
            0x16E700, // 174
            0x171700, // 175
            0x173F00, // 176

            0x161800, // 177
            0x164000, // 178
            0x166800, // 179
            0x169800, // 180
            0x16C000, // 181
            0x16E800, // 182
            0x171800, // 183
            0x174000, // 184

            0x161900, // 185
            0x164100, // 186
            0x166900, // 187
            0x169900, // 188
            0x16C100, // 189
            0x16E900, // 190
            0x171900, // 191
            0x174100, // 192

            0x161A00, // 193
            0x164200, // 194
            0x166A00, // 195
            0x169A00, // 196
            0x16C200, // 197
            0x16EA00, // 198
            0x171A00, // 199
            0x174200, // 200

            0x161B00, // 201
            0x164300, // 202
            0x166B00, // 203
            0x169B00, // 204
            0x16C300, // 205
            0x16EB00, // 206
            0x171B00, // 207
            0x174300, // 208

            0x161C00, // 209
            0x164400, // 210
            0x166C00, // 211
            0x169C00, // 212
            0x16C400, // 213
            0x16EC00, // 214
            0x171C00, // 215
            0x174400, // 216

            0x161D00, // 217
            0x164500, // 218
            0x166D00, // 219
            0x169D00, // 220
            0x16C500, // 221
            0x16ED00, // 222
            0x171D00, // 223
            0x174500, // 224

            0x161E00, // 225
            0x164600, // 226
            0x166E00, // 227
            0x169E00, // 228
            0x16C600, // 229
            0x16EE00, // 230
            0x171E00, // 231
            0x174600, // 232

            0x161F00, // 233
            0x164700, // 234
            0x166F00, // 235
            0x169F00, // 236
            0x16C700, // 237
            0x16EF00, // 238
            0x171F00, // 239
            0x174700, // 240

            0x162000, // 241
            0x164800, // 242
            0x167000, // 243
            0x16A000, // 244
            0x16C800, // 245
            0x16F000, // 246
            0x172000, // 247
            0x174800, // 248

            0x162100, // 249
            0x164900, // 250
            0x167100, // 251
            0x16A100, // 252
            0x16C900, // 253
            0x16F100, // 254
            0x172100, // 255
            0x174900, // 256

            0x162200, // 257
            0x164A00, // 258
            0x167200, // 259
            0x16A200, // 260
            0x16CA00, // 261
            0x16F200, // 262
            0x172200, // 263
            0x174A00, // 264

            0x162300, // 265
            0x164B00, // 266
            0x167300, // 267
            0x16A300, // 268
            0x16CB00, // 269
            0x16F300, // 270
            0x172300, // 271
            0x174B00, // 272

            0x162400, // 273
            0x164C00, // 274
            0x167400, // 275
            0x16A400, // 276
            0x16CC00, // 277
            0x16F400, // 278
            0x172400, // 279
            0x174C00, // 280

            0x162500, // 281
            0x164D00, // 282
            0x167500, // 283
            0x16A500, // 284
            0x16CD00, // 285
            0x16F500, // 286
            0x172500, // 287
            0x174D00, // 288

            0x162600, // 289
            0x164E00, // 290
            0x167600, // 291
            0x16A600, // 292
            0x16CE00, // 293
            0x16F600, // 294
            0x172600, // 295
            0x174E00, // 296

            0x162700, // 297
            0x164F00, // 298
            0x167700, // 299
            0x16A700, // 300
            0x16CF00, // 301
            0x16F700, // 302
            0x172700, // 303
            0x174F00, // 304

            0x162800, // 305
            0x165000, // 306
            0x167800, // 307
            0x16A800, // 308
            0x16D000, // 309
            0x16F800, // 310
            0x172800, // 311
            0x175000, // 312

            0x162900, // 313
            0x165100, // 314
            0x167900, // 315
            0x16A900, // 316
            0x16D100, // 317
            0x16F900, // 318
            0x172900, // 319
            0x175100, // 320
        };
        private static final int[][] doorCorrections = {
        		{49,-1},{81,-1},{97,-1},{98,-1},{140,-1},
        		{300,-1},{333,-1},{395,-1},{562,-1},
				{604,-1},{613,-1},{681,-1},{688,-1},
				{752,-1},{815,-1},{816,-1},{911,-1},
				{1136,-1},{1073,-2}
        };
    	private static final int[] doorDestTypes = {
    			1, -1, 0, -1, -1, 2, 2
    	};

        private static final int tsetpalAddress = 0x17AA00;
        private static final int musicAddress = 0x1cd837;
        private static final int dPointersAddress = 0x100200;
        private static final int tsetTblAddress = 0x2F121B;
        private static final int localTsetAddress = 0x175200;
        private static final int spDataEnd = 0xf8b91;
        private static final int spAsmPointer = 0x2461;
        private static final int spDataBase = 0xf0200; // 2 byte ptr + 0xf0200
        private static ArrayList mapChanges = new ArrayList();
        private static ArrayList[] spData =
        	new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
        private static Sector[] sectorData =
        	new Sector[MapEditor.heightInSectors * MapEditor.widthInSectors];
        private static ArrayList[] doorData =
        	new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
        private static int[] oldDoorEntryLengths =
        	new int[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
        private static ArrayList destData = new ArrayList();
        private static ArrayList destsLoaded = new ArrayList();
        private static ArrayList destsIndexes = new ArrayList();
        private static Image[][][] tileImages =
        	new Image[TILESET_NAMES.length][1024][maxpals];
        private static Image[][] spriteImages =
        	new Image[SpriteEditor.NUM_ENTRIES][8];
        
        public static void reset()
        {
        	mapChanges = new ArrayList();
            spData =
            	new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
            sectorData =
            	new Sector[MapEditor.heightInSectors * MapEditor.widthInSectors];
            doorData =
            	new ArrayList[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
            oldDoorEntryLengths =
            	new int[(MapEditor.heightInSectors / 2) * MapEditor.widthInSectors];
            destData = new ArrayList();
            destsLoaded = new ArrayList();
            destsIndexes = new ArrayList();
            tileImages =
            	new Image[TILESET_NAMES.length][1024][maxpals];
            spriteImages =
            	new Image[SpriteEditor.NUM_ENTRIES][8];
        }
        
        public static void loadTileImage(int loadtset, int loadtile, int loadpalette)
        {
            if (tileImages[loadtset][loadtile][loadpalette] == null)
            {
                tileImages[loadtset][loadtile][loadpalette] = TileEditor.tilesets[loadtset]
                    .getArrangementImage(loadtile, loadpalette);
            }
        }
        
        public static Image getTileImage(int loadtset, int loadtile, int loadpalette)
        {
        	return tileImages[loadtset][loadtile][loadpalette];
        }
        
        public static void resetTileImages()
        {
        	tileImages =
            	new Image[TILESET_NAMES.length][1024][maxpals];
        }
        
        public static Image getSpriteImage(int spt, int direction)
        {
        	return spriteImages[spt][direction];
        }
        
        public static void loadSpriteImage(HackModule hm, int spt, int direction)
        {
           	if (spriteImages[spt][direction] == null)
           	{
           		SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spt];
           		spriteImages[spt][direction] = new SpriteEditor.Sprite(sib
                        .getSpriteInfo(direction), hm).getImage(true);
           	}
        }
        
        public static void resetSpriteImages()
        {
        	spriteImages =
            	new Image[SpriteEditor.NUM_ENTRIES][8];
        }
        
        public static void changeTile(int x, int y, byte tile)
        {
        	for (int i = 0; i < mapChanges.size(); i++)
        	{
        		MapChange change = (MapChange) mapChanges.get(i);
        		if ((x == change.getX()) && (y == change.getY()))
        			change.setTile(tile);
        	}
        	mapChanges.add(new MapChange(x,y,tile));
        	
        	/*int address = mapAddresses[y] + x;
        	rom.write(address, tile);*/
        }
        
        public static int getTile(AbstractRom rom, int x, int y)
        {
        	for (int i = 0; i < mapChanges.size(); i++)
        	{
        		MapChange change = (MapChange) mapChanges.get(i);
        		if ((x == change.getX()) && (y == change.getY()))
        			return change.getTile();
        	}
        	
        	return rom.read(mapAddresses[y] + x)
				| (getLocalTileset(rom, x, y) << 8);
        }

        public static int[] getTiles(AbstractRom rom, int y, int x, int length)
        {
            int[] output = new int[length];
            for (int i = 0; i < output.length; i++)
            	output[i] = getTile(rom, x + i, y);
            return output;
        }
        
        public static void writeMapChanges(AbstractRom rom)
        {
        	for (int i = 0; i < mapChanges.size(); i++)
        	{
        		MapChange change = (MapChange) mapChanges.get(i);
        		rom.write(mapAddresses[change.getY()] + change.getX(),
        				change.getTile());
        	}
        	mapChanges = new ArrayList();
        }
        
        public static boolean isSectorDataLoaded(int sectorX, int sectorY)
        {
        	return (sectorData[sectorX + 
						   (sectorY * MapEditor.widthInSectors)]
						   != null);
        }
        
        public static void loadSectorData(AbstractRom rom, int sectorX, int sectorY)
        {
        	
            int address = tsetpalAddress 
				+ (sectorY * ((MapEditor.width + 1) / MapEditor.sectorWidth)) + sectorX;
            byte tsetpal_data = rom.readByte(address);
            short music = (short) ((rom.read(musicAddress + 
            		(sectorY * ((MapEditor.width + 1) / MapEditor.sectorWidth))
					+ sectorX)) & 0xff);
        	sectorData[sectorX + (sectorY * MapEditor.widthInSectors)]
					   = new Sector(
					   		(byte) ((tsetpal_data & 0xf8) >> 3),
							(byte) (tsetpal_data & 0x7),
							music);
        }
        
        public static int getTset(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * MapEditor.widthInSectors)]
							  .getTileset();
        }
        
        public static int getPal(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * MapEditor.widthInSectors)]
							  .getPalette();
        }
        
        public static int getMusic(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * MapEditor.widthInSectors)]
							  .getMusic();
        }
        
        public static void setTset(int sectorX, int sectorY, int newTset)
        {
        	sectorData[sectorX + 
						(sectorY * MapEditor.widthInSectors)]
				  .setTileset((byte) newTset);
        }
        
        public static void setPal(int sectorX, int sectorY, int newPal)
        {
        	sectorData[sectorX + 
						(sectorY * MapEditor.widthInSectors)]
				  .setPalette((byte) newPal);
        }
        
        public static void setMusic(int sectorX, int sectorY, int newMusic)
        {
        	sectorData[sectorX + 
						(sectorY * MapEditor.widthInSectors)]
				  .setMusic((byte) newMusic);
        }
        
        public static void writeSectorData(AbstractRom rom)
        {
        	for (int i = 0; i < sectorData.length; i++)
        	{
        		if (sectorData[i] != null)
        		{
        			rom.write(tsetpalAddress + i,
        					(sectorData[i].getTileset() << 3)
							+ sectorData[i].getPalette());
        			rom.write(musicAddress + i,
        					sectorData[i].getMusic());
        		}
        	}
        }

        public static int getDrawTileset(AbstractRom rom, int mapTset)
        {
            if ((mapTset > map_tsets) || (mapTset < 0))
            	return -1;
            int address = EbMap.tsetTblAddress + (mapTset * 2);
            int drawTset = rom.read(address);
            return drawTset;
        }

        public static int getLocalTileset(AbstractRom rom, int gltx, int glty)
        {
            int address = localTsetAddress
                + ((glty / 8) * (MapEditor.width + 1)) + gltx;
            if (((glty / 4) % 2) == 1)
                address += 0x3000;
            int local_tset = (rom.read(address) >>
            		((glty % 4) * 2)) & 3;

            return local_tset;
        }
        
        public static void setLocalTileset(
        		AbstractRom rom, int tilex, int tiley, int newltset)
        {
        	int address = localTsetAddress
			    + ((tiley / 8) * (MapEditor.width + 1)) + tilex;
        	if (((tiley / 4) % 2) == 1)
        		address += 0x3000;
        	int newLtsetData = 0;
        	int local_tset = rom.read(address);
        	int newLtset2set;
        	int localtiley = tiley - ((tiley / 4) * 4);
        	for (int i = 0; i <= 3; i++)
        	{
        		if (i == localtiley)
        		{
        			newLtset2set = newltset;
        		}
        		else
        		{
        			newLtset2set = (local_tset >> (i * 2)) & 3;
        		}
        		
        		newLtsetData += newLtset2set << (i * 2);
        	}
        	rom.write(address, newLtsetData);
        }
        
        public static boolean isSpriteDataLoaded(int areaNum)
        {
        	return (spData[areaNum] != null);
        }
        
        public static boolean isSpriteDataLoaded(int areaX, int areaY)
        {
        	return isSpriteDataLoaded(areaX + (areaY * MapEditor.widthInSectors));
        }
        
        public static void loadSpriteData(AbstractRom rom, int areaNum)
        {
        	int spPtrsAddress = 
        		HackModule.toRegPointer(rom.readMulti(spAsmPointer,3));
        	int ptr = rom.readMulti(spPtrsAddress + (areaNum * 2), 2);
        	spData[areaNum] = new ArrayList();
       		if (ptr > 0)
       		{
            	int[] data = rom.read(
            			spDataBase + ptr,
            			(rom.read(spDataBase + ptr)
            					* 4) + 2);
           		for (int j = 0; j < data[0]; j++)
           		{
           			short tpt = 
           				(short) (data[2 + (j * 4)] +
           						(data[3 + (j * 4)] 
									  * 0x100));
                	TPTEditor.TPTEntry tptEntry = 
                		TPTEditor.tptEntries[tpt];
                	SpriteEditor.SpriteInfoBlock sib =
                		SpriteEditor.sib[tptEntry.getSprite()];
                	short spriteX = 
                		(short) (data[5 + (j * 4)] - (sib.width * 4));
                	short spriteY =
                		(short) (data[4 + (j * 4)] - (sib.height * 6));
           			spData[areaNum].add(new SpriteLocation(
           					tpt, spriteX, spriteY));
           		}
       		}
       	}
        
        public static void loadSpriteData(AbstractRom rom, int areaX, int areaY)
        {
        	loadSpriteData(rom, areaX + (areaY * MapEditor.widthInSectors));
        }
        
        public static void loadSpriteData(AbstractRom rom)
        {
        	for (int i = 0; i < spData.length; i++)
        	{ 			
        		if (! isSpriteDataLoaded(i))
        			loadSpriteData(rom,i);
        	}
        }
		
        public static short[][] getSpriteLocs(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	
        	short[][] returnValue = 
        		new short[spData[areaNum].size()][2];
        	
        	for (int i = 0; i < spData[areaNum].size(); i++)
        	{
        		returnValue[i] = new short[] {
        				((SpriteLocation) spData[areaNum].get(i)).getX(),
						((SpriteLocation) spData[areaNum].get(i)).getY()
        		};
        	}
        	return returnValue;
        }
        
        public static short[] getSpriteTpts(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	
        	short[] returnValue = 
        		new short[spData[areaNum].size()];
        	
        	for (int i = 0; i < spData[areaNum].size(); i++)
        	{
        		returnValue[i] = 
        			((SpriteLocation) spData[areaNum].get(i)).getTpt();
        	}
        	return returnValue;
        }
        
        public static short[] getSpriteXY(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	SpriteLocation spLoc = (SpriteLocation) 
        			spData[areaNum].get(spNum);
        	return new short[] { spLoc.getX(), spLoc.getY() };
        }
        
        public static short getSpriteTpt(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	return ((SpriteLocation) 
        			spData[areaNum].get(spNum)).getTpt();
        }
        
        public static int getSpritesNum(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	return spData[areaNum].size();
        }
        
        public static void removeSprite(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	spData[areaNum].remove(spNum);
        }
        
        public static void removeSprite(int areaX, int areaY, short spTpt,
        		byte spX, byte spY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	spData[areaNum].remove(
        			spData[areaNum].indexOf(
        					new SpriteLocation(spTpt, spX, spY)));
        }
        
        public static void addSprite(int areaX, int areaY, short newX,
        		short newY, short newTpt)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	spData[areaNum].add(
        			new SpriteLocation(newTpt, newX, newY));
        }
        
        public static int findSprite(HackModule hm, int areaX, int areaY,
        		short spriteX, short spriteY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	for (int i = 0; i < spData[areaNum].size(); i++)
        	{
        		SpriteLocation spLoc = 
        			(SpriteLocation) spData[areaNum].get(i);
            	TPTEditor.TPTEntry tptEntry = 
            		TPTEditor.tptEntries[spLoc.getTpt()];
            	SpriteEditor.SpriteInfoBlock sib =
            		SpriteEditor.sib[tptEntry.getSprite()];
            	Image image = 
        			new SpriteEditor.Sprite(
        					sib.getSpriteInfo(
        							tptEntry.getDirection()), hm).getImage();
        		if (((sib.width * 8) > spriteX - spLoc.getX())
        			&& (0 < spriteX - spLoc.getX())
					&& ((sib.height * 8) > spriteY - spLoc.getY())
					&& (0 < spriteY - spLoc.getY()))
        		{
        			return i;
        		}
        	}
        	return -1;
        }
        
        public static boolean writeSprites(HackModule hm)
        {
        	AbstractRom rom = hm.rom;
        	int spPtrsAddress = 
        		HackModule.toRegPointer(rom.readMulti(spAsmPointer,3));
        	ArrayList spriteData = new ArrayList();
        	byte[] pointerData = new byte[2 * spData.length];
        	int whereToPut = 0xf63e7 - spDataBase;
        	int debugLength = 0;
        	for (int i = 0; i < spData.length; i++)
        	{     		
    			if (spData[i].size() == 0)
    			{
					pointerData[i * 2] = 0;
					pointerData[(i * 2) + 1] = 0;
    			}
        		else
        		{
					pointerData[i * 2] = (byte) (whereToPut & 0xff);
					pointerData[(i * 2) + 1] = 
						(byte) ((whereToPut & 0xff00) / 0x100);
					
        			byte[] areaData = new byte[2 + (spData[i].size() * 4)];
        			areaData[0] = (byte) spData[i].size();
        			areaData[1] = 0;
        			for (int j = 0; j < spData[i].size(); j++)
        			{
        				byte[] data = 
        					((SpriteLocation) spData[i].get(j)).toByteArray();
        				System.arraycopy(
        						data, 0, areaData, (j * 4) + 2, data.length);
        			}
        			spriteData.add(areaData);
        			whereToPut += areaData.length;
        		}
        	}
        	
        	boolean writeOK = hm.writetoFree(pointerData,
        			spAsmPointer, 3,
        			pointerData.length,
        			pointerData.length,
					true);
        	if (! writeOK)
        		return false;
        	
        	byte[] spriteDataArray = toByteArray(spriteData);
        	if (spDataBase + (0xf63e7 - spDataBase) 
        			+ spriteDataArray.length >= spDataEnd)
        		return false;		
        	rom.write(spDataBase + (0xf63e7 - spDataBase),
        			spriteDataArray);
        	
        	return true;
        }
        
        private static byte[] toByteArray(ArrayList list)
        {
            ListIterator iter = list.listIterator();
            // get the total size of all elements (flattened size)
            int size = 0;
            while (iter.hasNext()) {
              size += ( (byte[]) iter.next()).length;
            }

            // now build the flat array
            byte[] retVal = new byte[size];
            iter = list.listIterator();
            int idx = 0; //placeholder
            while (iter.hasNext()) {
              byte[] thisArray = (byte[]) iter.next();

              for (int i = 0; i < thisArray.length; i++)
              {
                retVal[idx] = thisArray[i];
                idx++;
              }
            }

            return retVal;
          }
        
        public static void nullSpriteData()
        {
        	for (int i = 0; i < spData.length; i++)
        		spData[i] = new ArrayList();
        }
        
        public static void loadDoorData(AbstractRom rom)
        {
        	for (int i = 0; i < (MapEditor.heightInSectors / 2) * MapEditor.widthInSectors; i++)
        		if (doorData[i] == null)
        			loadDoorData(rom, i);
        }
        
        public static void loadDoorData(AbstractRom rom, int areaNum)
        {
        	int ptr = HackModule.toRegPointer(rom.readMulti(
        			dPointersAddress + (areaNum * 4), 4));
        	doorData[areaNum] = new ArrayList();
        	oldDoorEntryLengths[areaNum] = rom.read(ptr);
       		for (byte i = 0; i < rom.readByte(ptr); i++)
       		{
       			short doorX = (short) rom.read(ptr + 3 + (i * 5));
       			short doorY = (short) rom.read(ptr + 2 + (i * 5));
       			byte doorType = rom.readByte(ptr + 4 + (i * 5));
       			short doorPtr = (short) rom.readMulti(
       					ptr + 5 + (i * 5), 2);
       			try {
       				if (doorDestTypes[doorType] == -1)
           				doorData[areaNum].add(new DoorLocation(
           						doorX, doorY, doorType,
    							(byte) ((doorPtr & 0xc0) >> 6), -1));
           			else
           			{
           				int destIndex =
           					loadDestData(rom, 0xf0200 + doorPtr, doorType);
           				doorData[areaNum].add(new DoorLocation(
               					doorX, doorY, doorType, 
    							doorPtr, destIndex));
           			}
       			}
       			catch (ArrayIndexOutOfBoundsException e)
					{
       				System.out.println("Could not load door entry #" + i
       					+ " of area #" + areaNum + " at 0x"
							+ Integer.toHexString(ptr + (i * 5))
							+ " because it has an invalid type (" + doorType + ").");
					}
       		}
        }
        
        public static void loadDoorData(AbstractRom rom, int areaX, int areaY)
        {
        	loadDoorData(rom, areaX + (areaY * MapEditor.widthInSectors));
       	}
        
        public static boolean isDoorDataLoaded(int areaNum)
        {
        	return (doorData[areaNum] != null);
        }
        public static boolean isDoorDataLoaded(int areaX, int areaY)
        {
        	return isDoorDataLoaded(areaX + (areaY * MapEditor.widthInSectors));
        }
        
        public static int getDoorsNum(int areaNum)
        {
        	return doorData[areaNum].size();
        }
        
        public static int getDoorsNum(int areaX, int areaY)
        {
        	return getDoorsNum(areaX + (areaY * MapEditor.widthInSectors));
        }
        
        public static short[] getDoorXY(int areaX, int areaY, int doorNum)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	DoorLocation dLoc = 
        		(DoorLocation) doorData[areaNum].get(doorNum);
        	return new short[] { dLoc.getX(), dLoc.getY() };
        }
        
        public static void addDoor(
        		int areaX, int areaY, short doorX,
        		short doorY, byte doorType, short doorPtr)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	doorData[areaNum].add(
        			new DoorLocation(
        					doorX, doorY, doorType, doorPtr, 0));
        }
        
        public static void addDoor(
        		int areaX, int areaY, short doorX,
        		short doorY, byte doorType, short doorPtr, int destNum)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	doorData[areaNum].add(
        			new DoorLocation(
        					doorX, doorY, doorType, doorPtr, destNum));
        }
        
        public static void removeDoor(int areaX, int areaY, int num)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	doorData[areaNum].remove(num);
        }
        
        public static DoorLocation getDoorLocation(int areaNum, int num)
        {
        	return (DoorLocation) doorData[areaNum].get(num);
        }
        
        public static DoorLocation getDoorLocation(int areaX, int areaY, int num)
        {
        	return getDoorLocation(areaX + (areaY * MapEditor.widthInSectors), num);
        }
        
        public static int findDoor(int areaX, int areaY,
        		short doorX, short doorY)
        {
        	int areaNum = areaX + (areaY * MapEditor.widthInSectors);
        	for (int i = 0; i < doorData[areaNum].size(); i++)
        	{
        		DoorLocation doorLocation = 
        			(DoorLocation) doorData[areaNum].get(i);
        		if ((8 >= doorX - (doorLocation.getX() * 8))
        				&& (0 <= doorX - (doorLocation.getX() * 8))
						&& (8 >= doorY - (doorLocation.getY() * 8))
        				&& (0 <= doorY - (doorLocation.getY() * 8)))
        		{
        			return i;
        		}
        	}
        	return -1;
        }
        
        public static boolean writeDoors(HackModule hm)
        {
        	AbstractRom rom = hm.rom;
        	ArrayList destPointers = new ArrayList();
        	int address = 0;
        	for (int i = 0; i < destData.size(); i++)
        	{
        		byte[] destBytes =
        			((Destination) destData.get(i)).toByteArray();
        		rom.write(0xf0200 + address, destBytes);
        		destPointers.add(new Integer(address));
        		address += destBytes.length;
        	}
        	
        	for (int i = 0; i < doorData.length; i++)
        	{
        		if (doorData[i].size() > 0)
        		{
        			byte[] toWrite =
        				new byte[(doorData[i].size() * 5) + 2];
        			toWrite[0] = (byte) doorData[i].size();
        			toWrite[1] = 0;
        			for (int j = 0; j < doorData[i].size(); j++)
            		{
        				DoorLocation doorLocation =
        					(DoorLocation) doorData[i].get(j);
            			toWrite[2 + (j * 5)] = 
            				(byte) doorLocation.getY();
            			toWrite[3 + (j * 5)] =
            				(byte) doorLocation.getX();
            			toWrite[4 + (j * 5)] =
            				doorLocation.getType();
            			if ((doorLocation.getType() == 1)
            					|| (doorLocation.getType() == 3) 
								|| (doorLocation.getType() == 4))
            			{
            				int data = doorLocation.getMisc() << 6;
            				toWrite[5 + (j * 5)] =
            					(byte) (data & 0xff);
            				toWrite[6 + (j * 5)] =
            					(byte) ((data & 0xff00) >> 2);
            			}
            			else
            			{
            				int destAddress =
            					((Integer) destPointers.get(
            							doorLocation.getDestIndex()))
											.intValue();
            				toWrite[5 + (j * 5)] =
            					(byte) (destAddress & 0xff);
            				toWrite[6 + (j * 5)] =
            					(byte) ((destAddress & 0xff00) / 0x100);
            			}
            		}
            		boolean writeOK = hm.writetoFree(toWrite,
                			dPointersAddress + (i * 4), 4,
							(oldDoorEntryLengths[i] * 5) + 2,
							toWrite.length, 0x4001ff, true);
            		if (! writeOK)
            			return false;
        		}
        		else
        		{
        			// TODO check if someone hasn't written to 0xA012F...
        			int nullSpace = HackModule.toSnesPointer(0xA012F);
        			rom.write(dPointersAddress + (i * 4), nullSpace & 0xff);
        			rom.write(dPointersAddress + (i * 4) + 1,
        					(nullSpace & 0xff00) / 0x100);
        			rom.write(dPointersAddress + (i * 4) + 2,
        					(nullSpace & 0xff0000) / 0x10000);
        			rom.write(dPointersAddress + (i * 4) + 3,0);
        		}
        	}
        	return true;
        }
        
        private static int loadDestData(
        		AbstractRom rom, int address, int type)
        {
        	if (! destsLoaded.contains(new Integer(address)))
        	{
        		Destination dest = null;
        		if (doorDestTypes[type] == 0)
        		{
        			int pointer = rom.readMulti(address, 4);
        			int flag = rom.readMulti(address + 4, 2);
        			boolean flagReversed;
        			if (flag > 0x8000)
        			{
        				flag -= 0x8000;
        				flagReversed = true;
        			}
        			else
        				flagReversed = false;
        			short yCoord = (short) rom.read(address + 6);
        			yCoord += (short) ((rom.read(address + 7) & 0x3F) << 8);
        			short xCoord = (short) (rom.readMulti(address + 8, 2));
        			byte style = rom.readByte(address + 10);
        			byte direction = 
        				(byte) ((rom.read(address + 7) & 0xC0) >> 6);
        			dest = new Destination(pointer, (short) flag, flagReversed,
        					xCoord, yCoord, style, direction);
        		}
        		else if (doorDestTypes[type] == 1)
        		{
        			short flag = (short) (rom.readMulti(address, 2));
        			boolean flagReversed;
        			if (flag > 0x8000)
        			{
        				flag -= 0x8000;
        				flagReversed = true;
        			}
        			else
        				flagReversed = false;
        			int pointer = rom.readMulti(address + 2, 4);
        			dest = new Destination(flag, flagReversed, pointer);
        		}
        		else if (doorDestTypes[type] == 2)
        		{
        			int pointer = rom.readMulti(address, 4);
        			dest = new Destination(pointer);
        		}
        		
        		if (dest != null)
        		{
        			destData.add(dest);
        			int destIndex = destData.indexOf(dest);
            		destsLoaded.add(new Integer(address));
            		destsIndexes.add(new Integer(destIndex));
            		return destIndex;
        		}
        		else
        			return -1;
        	}
        	else
        		return ((Integer) (
        				destsIndexes.get(
        						destsLoaded.indexOf(
        								new Integer(address)))))
										.intValue();
        }
        
        public static Destination getDestination(int index)
        {
        	return (Destination) destData.get(index);
        }
        
        public static int getDoorDestType(int doorType)
        {
        	return doorDestTypes[doorType];
        }
        
        public static class MapChange
		{
        	private int x, y;
        	private byte tile;
        	
        	public MapChange(int x, int y, byte tile)
        	{
        		this.x = x;
        		this.y = y;
        		this.tile = tile;
        	}
        	
        	public int getX()
        	{
        		return x;
        	}
        	
        	public int getY()
        	{
        		return y;
        	}
        	
        	public byte getTile()
        	{
        		return tile;
        	}
        	
        	public void setTile(byte tile)
        	{
        		this.tile = tile;
        	}
		}
        
        public static class SpriteLocation
		{
        	private short x, y, tpt;
        	
        	public SpriteLocation(short tpt,
        			short x, short y)
        	{
        		this.tpt = tpt;
        		this.x = x;
        		this.y = y;
        	}
        	
        	public void setTpt(short tpt)
        	{
        		this.tpt = tpt;
        	}
        	
        	public short getTpt()
        	{
        		return tpt;
        	}
        	
        	public void setX(short x)
        	{
        		this.x = x;
        	}
        	
        	public void setY(short y)
        	{
        		this.y = y;
        	}
        	
        	public short getX()
        	{
        		return this.x;
        	}
        	
        	public short getY()
        	{
        		return this.y;
        	}
        	
        	public byte[] toByteArray()
        	{
        		byte[] byteArray = new byte[4];
				TPTEditor.TPTEntry tptEntry = 
            		TPTEditor.tptEntries[getTpt()];
            	SpriteEditor.SpriteInfoBlock sib =
            		SpriteEditor.sib[tptEntry.getSprite()];
				byteArray[0] = 
					(byte) (getTpt() & 0xff);
				byteArray[1] = 
					(byte) (getTpt() / 0x100);
				byteArray[2] = 
					(byte) (getY() + 
							((short) (sib.height * 6)));
				byteArray[3] =
					(byte) (getX() +
							((short) (sib.width * 4)));
				return byteArray;
        	}
		}
        
        public static class DoorLocation
		{
        	private int destIndex;
        	private byte type, misc;
        	private short x, y, pointer;
        	
        	public DoorLocation(short x, short y,
        			byte type, short pointer, int destIndex)
        	{
        		this.x = x;
        		this.y = y;
        		this.type = type;
        		this.pointer = pointer;
        		this.destIndex = destIndex;
        	}
        	
        	public DoorLocation(short x, short y,
        			byte type, byte misc)
        	{
        		this.x = x;
        		this.y = y;
        		this.type = type;
        		this.misc = misc;
        		this.destIndex = -1;
        	}
        	
        	public void setX(short x)
        	{
        		this.x = x;
        	}
        	
        	public short getX()
        	{
        		return x;
        	}
        	
        	public void setY(short y)
        	{
        		this.y = y;
        	}
        	
        	public short getY()
        	{
        		return y;
        	}
        	
        	public void setType(byte type)
        	{
        		this.type = type;
        	}
        	
        	public byte getType()
        	{
        		return type;
        	}
        	
        	public void setPointer(short pointer)
        	{
        		this.pointer = pointer;
        	}
        	
        	public short getPointer()
        	{
        		return pointer;
        	}
        	
        	public byte getMisc()
        	{
        		return misc;
        	}
        	
        	public void setMisc(byte misc)
        	{
        		this.misc = misc;
        	}
        	
        	public int getDestIndex()
        	{
        		return destIndex;
        	}
        	
        	public void setDestIndex(int destIndex)
        	{
        		this.destIndex = destIndex;
        	}
		}

        public static class Sector
		{
        	private byte tileset, palette;
        	private short music;
        	
        	public Sector(byte tileset, byte palette, short music)
        	{
        		this.tileset = tileset;
        		this.palette = palette;
        		this.music = music;
        	}
        	
        	public void setTileset(byte tileset)
			{
        		this.tileset = tileset;
			}
        	
        	public byte getTileset()
        	{
        		return tileset;
        	}
        	
        	public void setPalette(byte palette)
        	{
        		this.palette = palette;
        	}
        	
        	public byte getPalette()
        	{
        		return palette;
        	}
        	
        	public void setMusic(byte music)
        	{
        		this.music = music;
        	}
        	
        	public short getMusic()
        	{
        		return music;
        	}
		}
       
        public static class Destination
		{
        	private int pointer;
        	private short flag, yCoord, xCoord;
        	private byte style, direction, type;
        	private boolean flagReversed;
        	
        	public Destination(int pointer, short flag, boolean flagReversed,
        			short xCoord, short yCoord, byte style, byte direction)
        	{
        		this.pointer = pointer;
        		this.flag = flag;
        		this.flagReversed = flagReversed;
        		this.xCoord = xCoord;
        		this.yCoord = yCoord;
        		this.style = style;
        		this.direction = direction;
        		this.type = 0;
        	}
        	
        	public Destination(short flag, boolean flagReversed, int pointer)
        	{
        		this.flag = flag;
        		this.flagReversed = flagReversed;
        		this.pointer = pointer;
        		this.type = 1;
        	}
        	
        	public Destination(int pointer)
        	{
        		this.pointer = pointer;
        		this.type = 2;
        	}
        	
        	public void setType(byte type)
        	{
        		this.type = type;
        	}
        	
        	public byte getType()
        	{
        		return type;
        	}
        	
        	public int getPointer()
        	{
        		return pointer;
        	}
        	
        	public void setPointer(int pointer)
        	{
        		this.pointer = pointer;
        	}
        	
        	public int getFlag()
        	{
        		return flag;
        	}
        	
        	public void setFlag(short flag)
        	{
        		this.flag = flag;
        	}
        	
        	public short getY()
        	{
        		return yCoord;
        	}
        	
        	public void setY(short yCoord)
        	{
        		this.yCoord = yCoord;
        	}
        	
        	public short getX()
        	{
        		return xCoord;
        	}
        	
        	public void setX(short xCoord)
        	{
        		this.xCoord = xCoord;
        	}
        	
        	public byte getStyle()
        	{
        		return style;
        	}
        	
        	public void setStyle(byte style)
        	{
        		this.style = style;
        	}
        	
        	public byte getDirection()
        	{
        		return direction;
        	}
        	
        	public void setDirection(byte direction)
        	{
        		this.direction = direction;
        	}
        	
        	public boolean isFlagReversed()
        	{
        		return flagReversed;
        	}
        	
        	public void setFlagReversed(boolean flagReversed)
        	{
        		this.flagReversed = flagReversed;
        	}
        	
        	public byte[] toByteArray()
        	{
        		byte[] byteArray;
        		if (type == 0)
        		{
        			byteArray = new byte[11];
        			byteArray[0] = (byte) (pointer & 0xff);
        			byteArray[1] = (byte) ((pointer & 0xff00) / 0x100);
        			byteArray[2] = (byte) ((pointer & 0xff0000) / 0x10000);
        			byteArray[3] = 0;
        			byteArray[4] = (byte) (flag & 0xff);
        			byteArray[5] = (byte) ((flag & 0xff00) / 0x100);
        			if (flagReversed)
        				byteArray[5] += 0x80;
        			byteArray[6] = (byte) (yCoord & 0xff);
        			byteArray[7] = (byte) (((yCoord & 0xff00) / 0x100)
        					+ (direction << 6));
        			byteArray[8] = (byte) (xCoord & 0xff);
        			byteArray[9] = (byte) ((xCoord & 0xff00) / 0x100);
        			byteArray[10] = style;
        		}
        		else if (type == 1)
        		{
        			byteArray = new byte[6];
        			byteArray[0] = (byte) (flag & 0xff);
        			byteArray[1] = (byte) ((flag & 0xff00) / 0x100);
        			if (flagReversed)
        				byteArray[1] += 0x80;
        			byteArray[2] = (byte) (pointer & 0xff);
        			byteArray[3] = (byte) ((pointer & 0xff00) / 0x100);
        			byteArray[4] = (byte) ((pointer & 0xff0000) / 0x10000);
        			byteArray[5] = 0;
        		}
        		else if (type == 2)
        		{
        			byteArray = new byte[4];
        			byteArray[0] = (byte) (pointer & 0xff);
        			byteArray[1] = (byte) ((pointer & 0xff00) / 0x100);
        			byteArray[2] = (byte) ((pointer & 0xff0000) / 0x10000);
        			byteArray[3] = 0;
        		}
        		else
        		{
        			return null;
        		}
        		return byteArray;
        	}
		}
    }

    public class EditBox extends AbstractButton
	{
    	private JScrollBar scrollh2;
    	private int selected, selectedx, selectedy, tset, pal, hscroll;
    	
    	public EditBox(JScrollBar scrollh2)
    	{
    		this.tset = -1;
    		this.pal = -1;
    		this.selected = -1;
    		this.selectedx = -1;
    		this.selectedy = -1;
    		this.hscroll = 0;
    		this.scrollh2 = scrollh2;
    	}
    	
    	public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            if (gfxcontrol.getModeProps()[2] == 1)
            {
                if ((this.tset > -1) && (this.pal > -1))
                {
                	drawTiles(g, g2d);
                	scrollh2.setEnabled(true);
                }
                else
                	scrollh2.setEnabled(false);
                drawBorder(g2d);
                drawSelected(g2d);
            }
            else
            	scrollh2.setEnabled(false);
        }
    	
    	public void remoteRepaint()
    	{
    		repaint();
    	}
    	
    	public boolean setTsetPal(int newtset, int newpal)
    	{
    		boolean isSame = (this.tset == newtset) && (this.pal == newpal);
    		this.tset = newtset;
    		this.pal = newpal;
    		return isSame;
    	}
    	
    	public int getTset()
    	{
    		return tset;
    	}
    	
    	public int getPal()
    	{
    		return pal;
    	}
    	
    	public boolean knowsTsetPal()
    	{
    		return (tset > -1) && (pal > -1);
    	}
    	
    	public void setScroll(int newhscroll)
    	{
    		this.hscroll = newhscroll;
    	}
    	
    	public int getScroll()
    	{
    		return hscroll;
    	}
    	
    	public void setSelected(int newselectedx, int newselectedy)
    	{ 		
    		int tilex = newselectedx / MapEditor.tileWidth;
    		int tiley = newselectedy / MapEditor.tileHeight;
    		
    		this.selected = ((this.hscroll + tilex)
    				* (editHeight + 1)) + tiley;
    		this.selectedx = tilex + this.hscroll;
    		this.selectedy = tiley;
    	}
    	
    	public void setSelected(int tile)
    	{
    		selected = tile;
    		selectedx = tile / (editHeight + 1);
    		selectedy = tile % (editHeight + 1);
    	}
    	
    	public boolean isSelected()
    	{
    		return (this.selectedx != -1)
				&& (this.selectedy != -1);
    	}
    	
    	public int getSelected()
    	{
    		return this.selected;
    	}
    	
    	public void drawSelected(Graphics2D g2d)
    	{
    		if ((this.selectedx != -1)
    				&& (this.selectedy != -1)
					&& (this.selectedx >= this.hscroll)
					&& (this.selectedx <= this.hscroll + editWidth))
    		{
    			g2d.setPaint(Color.yellow);
    			g2d.draw(new Rectangle2D.Double(
    					(this.selectedx - this.hscroll) * MapEditor.tileWidth,
    					this.selectedy * MapEditor.tileHeight, MapEditor.tileWidth, MapEditor.tileHeight));
    		}
    	}
    	
    	public void drawBorder(Graphics2D g2d)
		{
            g2d.setPaint(Color.black);
            for (int i = 0; i <= editWidth; i++)
            {
                for (int j = 0; j <= editHeight; j++)
                {
                    g2d.draw(new Rectangle2D.Double(i * MapEditor.tileWidth,
                    		j * MapEditor.tileHeight, MapEditor.tileWidth,
							MapEditor.tileHeight));
                }
            }
		}
    	
    	public void drawTiles(Graphics g, Graphics2D g2d)
    	{
            g2d.setPaint(Color.black);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
    		for (int i = 0; i <= editWidth; i++)
    		{
    			for (int j = 0; j <= editHeight; j++)
    			{
    				int tile = ((hscroll + i) * (editHeight + 1)) + j;
    				if (gfxcontrol.getModeProps()[1] == 1)
    				{
                        g2d.drawString(addZeros(
                        		Integer.toHexString(tile), 2), 
    							(i * MapEditor.tileWidth), (j * MapEditor.tileHeight));
    				}
    				else if (gfxcontrol.getModeProps()[1] == 2)
    				{
        				EbMap.loadTileImage(tset, tile, pal);
        				g.drawImage(
        						EbMap.getTileImage(tset,tile,pal),
    							(i * MapEditor.tileWidth), (j * MapEditor.tileHeight),
    							MapEditor.tileWidth, MapEditor.tileHeight, this);
    				}
    			}
    		}
    	}
	}
    
    public class DoorEditor extends EbHackModule 
		implements ActionListener, ItemListener
    {
    	private JTextField 
			numField, ptrField, xField, yField,
			flagField;
    	private JComboBox typeBox, typeDestBox, styleBox, dirBox;
    	private JLabel warnLabel;
    	private JButton setXYButton, jumpButton;
    	private JCheckBox reverseCheck;
    	private EbMap.DoorLocation doorLocation;
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
    		TeleportTableEditor.initWarpStyleNames();
    		
    		destPreview = new DestPreview();
    		
    		createGUI();
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
    		return "Door Editor";
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
    		if (isInited)
    		{
    			mainWindow.setVisible(false);
        		
        		if (gfxcontrol.getMode() == 3)
        		{
        			gfxcontrol.changeMode(2);
        			gfxcontrol.remoteRepaint();
        			enableSetXY();
        		}
    		}
    	}
    	
    	public void show()
    	{
    		super.show();
    		this.reset();
    		this.mainWindow.setVisible(true);
    	}
    	
    	public void show(EbMap.DoorLocation doorLocation)
    	{
    		this.doorLocation = doorLocation;
    		this.show();

    		updateComponents(true, true, true);
    	}
    	
    	public void createGUI()
    	{
    		mainWindow = createBaseWindow(this);
    		mainWindow.setTitle(this.getDescription());
    		
    		JPanel entryPanel = new JPanel();
    		entryPanel.setLayout(
    				new BoxLayout(entryPanel, BoxLayout.Y_AXIS));
    		
    		numField = HackModule.createSizedJTextField(5, true);
    		numField.getDocument().addDocumentListener(new DocumentListener()
    				{
                public void changedUpdate(DocumentEvent de)
                {
                	if (numField.getText().length() > 0)
                		updateComponents(true, false, true);
                }

                public void insertUpdate(DocumentEvent de)
                {
                    changedUpdate(de);
                }

                public void removeUpdate(DocumentEvent de)
                {
                    changedUpdate(de);
                }
    				});
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
    		jumpButton = new JButton("Jump to this on the map");
    		jumpButton.addActionListener(this);
    		buttonPanel.add(jumpButton);
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
				destNum = doorLocation.getDestIndex();
				numField.setText(Integer.toString(destNum));
				typeBox.setSelectedIndex(doorLocation.getType());
			}
			else
				destNum = Integer.parseInt(numField.getText());
			
    		if (destNum < 0)
    		{
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
    			jumpButton.setEnabled(false);
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
        			jumpButton.setEnabled(true);
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
        			jumpButton.setEnabled(false);
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
        			jumpButton.setEnabled(false);
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
            else if (e.getActionCommand().equals("close"))
            {
                hide();
            }
            else if (e.getSource() == setXYButton)
            {
            	setXYButton.setEnabled(false);
            	gfxcontrol.changeMode(3);
            	gfxcontrol.remoteRepaint();
            }
            else if (e.getSource() == jumpButton)
            {
            	gfxcontrol.setMapX(
            			(Integer.parseInt(xField.getText()) * 8) 
						/ MapEditor.tileWidth);
            	gfxcontrol.setMapY(
            			(Integer.parseInt(yField.getText()) * 8)
						/ MapEditor.tileWidth);
            	gfxcontrol.remoteRepaint();
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
		
		public void setDestXY(int destX, int destY)
		{
			xField.setText(Integer.toString(destX));
			yField.setText(Integer.toString(destY));
			destPreview.setXY(destX, destY);
			destPreview.remoteRepaint();
		}
		
		public void enableSetXY()
		{
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
                    	
                        tile_set = EbMap.getDrawTileset(rom, 
                        		EbMap.getTset(sectorX, sectorY));
                        tile_tile = mapArray[i][j]
                            | (EbMap.getLocalTileset(rom, 
                            		tileX + j,
									tileY + i) << 8);
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
}