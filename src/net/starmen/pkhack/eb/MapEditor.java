package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
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
        destEditor = new DoorDestinationEditor(rom, prefs);
    }
	
    private JFrame mainWindow;
    private JPanel top_buttons;
    private int x = 0;
    private int y = 0;
    private int palette = 0;
    private int music = 0;
    private int sector_width = 8;
    private int sector_height = 4;
    private int width_sectors = 32;
    private int height_sectors = 80;
    private int width = (width_sectors * sector_width) - 1;
    private int height = (height_sectors * sector_height) - 1;
    private int tilewidth = 32;
    private int tileheight = 32;
    private int screen_width = 24;
    private int screen_height = 12;
    private int editheight = 3;
    private int editwidth = screen_width - 1;
    private static final int draw_tsets = 20;
    private static final int map_tsets = 32;
    private static final int maxpals = 59;
    private NumberFormat num_format;
    private JFormattedTextField xField;
    private JFormattedTextField yField;
    private JComboBox tilesetList;
    private JFormattedTextField paletteField;
    private JFormattedTextField musicField;
    private JScrollBar scrollh, scrollv, scrollh2;
    private JPanel mapgfxpanel;
    private boolean changingSectors = false;
    private boolean movingSprite = false;
    private int[] movingSpriteInfo;
    private short copyTpt = -1; 

    public static final String[][][] menuNames = {
        {
        	{"File", "f"},
			{"Save Changes", "s"},
			{"Exit", "q"}
		},
        {
        	{"Mode", "m"}, 
//        	{"Map View (Text)", "1"}, 
//			{"Map View", "2"},
            {"Map Edit", "1"}, 
			{"Sprite Edit", "2"}
            }
        };

    public static final String[] TILESET_NAMES = {"Underworld", "Onett",
        "Twoson", "Threed", "Fourside", "Magicant", "Outdoors", "Summers",
        "Desert", "Dalaam", "Indoors 1", "Indoors 2", "Stores 1", "Caves 1",
        "Indoors 3", "Stores 2", "Indoors 4", "Winters", "Scaraba", "Caves 2"};

    // public static final int[] MAP_TILESETS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    // 10, 10, 11, 17, 10, 10, 10, 10, 18, 16, 12, 11, 11, 11, 15, 14, 19, 13,
    // 13, 13, 13, 0 };

    private EbMap mapcontrol;
    private MapGraphics gfxcontrol;
    private EditBox editbox;
    private DoorDestinationEditor destEditor;

    public JPanel createTopButtons()
    {
        JPanel panel = new JPanel(new FlowLayout());

        panel.add(new JLabel("X: "));
        xField = new JFormattedTextField(num_format);
        xField.setValue(new Integer(x));
        xField.setColumns(3);
        xField.addPropertyChangeListener("value", this);
        panel.add(xField);

        panel.add(new JLabel("Y: "));
        yField = new JFormattedTextField(num_format);
        yField.setValue(new Integer(y));
        yField.setColumns(3);
        yField.addPropertyChangeListener("value", this);
        panel.add(yField);

        panel.add(new JLabel("Tileset: "));
        String[] tList_names = new String[map_tsets];
        for (int i = 0; i < map_tsets; i++)
        {
            tList_names[i] = i + " - "
                + TILESET_NAMES[mapcontrol.getDrawTileset(i)];
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
		
		menu = new JMenu("Mode");
		group = new ButtonGroup();
		radioButton = new JRadioButtonMenuItem("Map Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE0);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		menu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Sprite Edit");
		radioButton.setSelected(false);
		radioButton.setActionCommand(MenuListener.MODE1);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		menu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Door Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE2);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		menu.add(radioButton);
        menuBar.add(menu);
        
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
							- (tilewidth * screen_width) - 1;
        				/*
        				int oldScreenWidth = screen_width;
        				int oldScreenHeight = screen_height;
        				Dimension oldFrameSize = mainWindow.getSize();
        				 */
        				
        				if ((tilewidth <= xDiff) || (0 - tilewidth >= xDiff))
        				{
        					screen_width = ((int) gfxcontrol.getSize().getWidth()) 
								/ tileheight;
        					editwidth = screen_width - 1;
        			        if (scrollh.getValue() + screen_width > width + 1)
        			        	scrollh.setValue(width - screen_width + 1);
        			        if (scrollh2.getValue() + editwidth > width + 1)
        			        	scrollh2.setValue(width - screen_width + 1);
        			        scrollh.setVisibleAmount(screen_width);
        			        scrollh2.setVisibleAmount(screen_width);
        			        editbox.remoteRepaint();
        			        effected = true;
        				}
        				
        				int yDiff = ((int) gfxcontrol.getSize().getHeight())
							- (tileheight * screen_height) - 1;
        				if ((tileheight <= yDiff) || (0 - tileheight >= yDiff))
        				{
        					screen_height = ((int) gfxcontrol.getSize().getHeight()) 
								/ tileheight;
        			        if (scrollv.getValue() + screen_height > height)
        			        	scrollv.setValue(height - screen_height + 1);
        			        scrollv.setVisibleAmount(screen_height);
        			        effected = true;
        				}
        				
        				if (effected)
        				{
        					gfxcontrol.setPreferredSize(
        							new Dimension(
        									(tilewidth * screen_width) + 1,
											(tileheight * screen_height) + 1));
        			        /*
        			           mainWindow.setSize(
        			        		new Dimension(
        			        				(int) (oldFrameSize.getWidth() - 
        			        						((screen_width - oldScreenWidth) 
        			        								* tilewidth)),
											(int) (oldFrameSize.getHeight() -
													((screen_height - oldScreenHeight))
													* tileheight)));
        			         */
        			        int[][] maparray = new int[screen_height][screen_width];
                            for (int i = 0; i < screen_height; i++)
                            	maparray[i] = 
                                	mapcontrol.getTiles(i + y, x, screen_width);
                            gfxcontrol.setMapArray(maparray);        			        
        			        gfxcontrol.remoteRepaint();
        			        
        			        // This slows it down so much!
        			        // mainWindow.pack();
        				}
        			}
        		});

        scrollh = new JScrollBar(JScrollBar.HORIZONTAL, 0,
        		screen_width, 0, width + 1);
        scrollh.addAdjustmentListener(new ScrollListener());

        scrollv = new JScrollBar(JScrollBar.VERTICAL, 0,
        		screen_height, 0, height + 1);
        scrollv.addAdjustmentListener(new ScrollListener());
        
        scrollh2 = new JScrollBar(JScrollBar.HORIZONTAL, 0,
        		screen_width, 0, 1024 / (editheight + 1));
        scrollh2.addAdjustmentListener(new ScrollListener());

        gfxcontrol
            .setPreferredSize(new Dimension((tilewidth * screen_width)
                + 1, (tileheight * screen_height) + 1));
        editbox.setPreferredSize(new Dimension((editwidth + 1) * tilewidth,
        		(editheight + 1) * tileheight));
        gfxListener gfxEars = new gfxListener();
        gfxcontrol.addMouseListener(gfxEars);
        gfxcontrol.addMouseMotionListener(gfxEars);
        gfxcontrol.addMouseWheelListener(gfxEars);
        editbox.addMouseListener(new editboxListener());
        
        top_buttons = createTopButtons();
        
        JPanel subpanel = new JPanel();
        
        mapgfxpanel = new JPanel(new BorderLayout());
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
        mapcontrol = new EbMap(this);
        gfxcontrol = new MapGraphics(this, 0, true, true);
        editbox = new EditBox();

        createGUI();
    }
    
    public void writeToRom()
    {
    	JOptionPane.showMessageDialog(mainWindow,
    			"Everything but map changes will be saved.\n"
    			+ "Map changes are saved immediately");
    	mapcontrol.writeSectorData();
    	boolean doorWrite = mapcontrol.writeDoors();
    	boolean spWrite = mapcontrol.writeSprites();
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
    			"Done!");
    }

    public void propertyChange(PropertyChangeEvent e)
    {
        if (!changingSectors)
        {
            Object source = e.getSource();
            if (source == xField)
            {
                int newx = ((Number) xField.getValue()).intValue();
                if ((newx >= 0) && (newx <= (width - screen_width)))
                {
                    x = newx;
                    int[][] maparray = new int[screen_height][screen_width];
                    for (int i = 0; i < screen_height; i++)
                    {
                        maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                    }
                    gfxcontrol.setMapArray(maparray);
                    gfxcontrol.remoteRepaint();

                    scrollh.setValue(x);
                }
                else
                {
                    xField.setValue(new Integer(x));
                }
            }
            else if (source == yField)
            {
                int newy = ((Number) yField.getValue()).intValue();
                if ((newy >= 0) && (newy <= (height - screen_height)))
                {
                    y = newy;
                    int[][] maparray = new int[screen_height][screen_width];
                    for (int i = 0; i < screen_height; i++)
                    {
                        maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                    }
                    gfxcontrol.setMapArray(maparray);
                    gfxcontrol.remoteRepaint();

                    scrollv.setValue(y);
                }
                else
                {
                    yField.setValue(new Integer(y));
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
                	mapcontrol.setPal(sectorxy[0], sectorxy[1], newpal);
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
                	mapcontrol.setMusic(
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
			mapcontrol.setTset(sectorxy[0], sectorxy[1],
					tilesetList.getSelectedIndex());		
			gfxcontrol.remoteRepaint();
			
			int tset = mapcontrol.getTset(sectorxy[0], sectorxy[1]);
			int pal = mapcontrol.getPal(sectorxy[0], sectorxy[1]);
            if (gfxcontrol.getModeProps()[2] == 1)
            {
            	boolean isSame = editbox.setTsetPal(
            			mapcontrol.getDrawTileset(tset),
                        TileEditor.tilesets[mapcontrol.getDrawTileset(
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
            // System.out.println("source: " + ae.getSource() + " value: " +
            // ae.getValue());
            Object source = ae.getSource();
            if (source == scrollh)
            {
                x = ae.getValue();
                int[][] maparray = new int[screen_height][screen_width];
                for (int i = 0; i < screen_height; i++)
                {
                    maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                }
                gfxcontrol.setMapArray(maparray);
                gfxcontrol.remoteRepaint();

                xField.setValue(new Integer(x));
            }
            else if (source == scrollv)
            {
                y = ae.getValue();
                int[][] maparray = new int[screen_height][screen_width];
                String rows = "";
                for (int i = 0; i < screen_height; i++)
                {
                	rows = rows + " " + (i + y);
                    maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                }
                gfxcontrol.setMapArray(maparray);
                gfxcontrol.remoteRepaint();

                yField.setValue(new Integer(y));
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
        			mapcontrol.nullSpriteData();
        			gfxcontrol.remoteRepaint();
        		}
			}
			else if (ac.equals(RESET_TILE_IMAGES))
			{
				mapcontrol.resetTileImages();
				if (gfxcontrol.getModeProps()[1] >= 2)
				{
					gfxcontrol.remoteRepaint();
					editbox.remoteRepaint();
				}
			}
			else if (ac.equals(RESET_SPRITE_IMAGES))
			{
				mapcontrol.resetSpriteImages();
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
            			(((e.getX() / tilewidth) + editbox.getScroll())
            					* (editheight + 1))
						+ (e.getY() / tileheight);
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
            y = y + e.getWheelRotation();
            if (y > height - screen_height + 1)
            	y = height - screen_height + 1;
            else if (y < 0)
            	y = 0;
            int[][] maparray = new int[screen_height][screen_width];
            for (int i = 0; i < screen_height; i++)
            {
                maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
            }
            gfxcontrol.setMapArray(maparray);
            gfxcontrol.remoteRepaint();

            yField.setValue(new Integer(y));
    	}
    	
    	public void mouseMoved(MouseEvent e)
    	{}
    	
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
    	}
    	
        public void mousePressed(MouseEvent e)
        {
            if ((e.getButton() == 1)
            		&& (gfxcontrol.getModeProps()[3] == 1)
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
                		TPTEditor.tptEntries[mapcontrol.getSpriteTpt(
							areaXY[0], areaXY[1], spNum)];
            		movingSpriteInfo = 
            			new int[] {
            				areaXY[0], areaXY[1], spNum,
							tpt.getSprite(), tpt.getDirection()
            		};
            		movingSprite = true;
            	}
            }
        }

        public void mouseReleased(MouseEvent e)
        {
        	if ((e.getButton() == 1)
        			&& (gfxcontrol.getModeProps()[3] == 1)
					&& movingSprite)
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
        		if (mousex > tilewidth * screen_width)
        		{
        			mousex = tilewidth * screen_width;
        		}
        		if (mousey > tileheight * screen_height)
        		{
        			mousey = tileheight * screen_height;
        		}
        		
        		short tpt =
        			mapcontrol.getSpriteTpt(movingSpriteInfo[0],
        					movingSpriteInfo[1],
							movingSpriteInfo[2]);
        		int[] spLocXY =
        			gfxcontrol.getCoords(mousex, mousey);
        		if (spLocXY[0] >= width_sectors)
        			spLocXY[0] = width_sectors - 1;
        		else if (spLocXY[0] < 0)
        			spLocXY[0] = 0;
        		if (spLocXY[1] >= height_sectors)
        			spLocXY[1] = height_sectors - 1;
        		else if (spLocXY[1] < 0)
        			spLocXY[1] = 0;
        		mapcontrol.removeSprite(
        				movingSpriteInfo[0],
						movingSpriteInfo[1],
        				movingSpriteInfo[2]);
        		mapcontrol.addSprite(spLocXY[0], spLocXY[1],
        				(short) spLocXY[2], (short) spLocXY[3],
						tpt);
        		movingSprite = false;
        		gfxcontrol.remoteRepaint();
        	}
        }

        public void mouseEntered(MouseEvent e)
        {}

        public void mouseExited(MouseEvent e)
        {}

        public void mouseClicked(MouseEvent e)
        {
            // System.out.println("Mouse clicked! Button: " + e.getButton());
        	// System.out.println("Mouse clicked! Data: " + e);
        	if ((e.getButton() == 1) && editbox.knowsTsetPal()
        			&& (gfxcontrol.getModeProps()[2] == 1))
        	{
        		int mousex = e.getX();
        		int mousey = e.getY();
        		int mapx = mousex / tilewidth;
        		int mapy = mousey / tileheight;
        		if (e.getModifiers() == 17)
        		{
        			int selected = mapcontrol.getTile(mapx, mapy);
        			int selectedPos;
            		if (selected > (1024 / (editheight + 1)) - editwidth)
            			selectedPos = (selected / (editheight + 1)) - editwidth;
            		else
            			selectedPos = selected / (editheight + 1);
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
            		
            		mapcontrol.changeTile(x + mapx, y + mapy, tile);
            		gfxcontrol.changeMapArray(mapx, mapy, tile);
            		mapcontrol.setLocalTileset(x + mapx, y + mapy,
            				localtset);
            		gfxcontrol.remoteRepaint();
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
                				mapcontrol.getSpriteTpt(
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
            	else if (gfxcontrol.getModeProps()[4] == 1)
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
                				mapcontrol.getDoorLocation(
                						doorCoords[0], doorCoords[1], num)
										.getDestIndex());
                		popup.add(EbHackModule.createJMenuItem(
                				"Edit destination (" + dest + ")",
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
                        tilewidth) + x, sector_width);
                    int sectory = getSectorXorY(getTileXorY(mousey,
                        tileheight) + y, sector_height);
                    
                    int tset = mapcontrol.getTset(sectorx, sectory);
                    int pal = mapcontrol.getPal(sectorx, sectory);
                    int music = mapcontrol.getMusic(sectorx, sectory);
                    int[] modeprops = gfxcontrol.getModeProps();
                    tilesetList.setSelectedIndex(tset);
                    paletteField.setValue(new Integer(pal));
                    musicField.setValue(new Integer(music));
                    
                    palette = pal;
                    
                    if (modeprops[2] == 1)
                    {
                    	boolean isSame = editbox.setTsetPal(
                    			mapcontrol.getDrawTileset(tset),
                                TileEditor.tilesets[mapcontrol.getDrawTileset(
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
        			mapcontrol.addSprite(areaX, areaY,
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
            			mapcontrol.removeSprite(areaX,
            					areaY, num);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(CHANGE_TPT))
        		{
            		short tpt = mapcontrol.getSpriteTpt(
            				areaX, areaY, num);
            		String input = JOptionPane.showInputDialog(
                            mainWindow,
                            "Change which TPT entry this"
							+ " sprite entry will display.",
                            Integer.toString((int) tpt));
            		if (input != null)
            		{
            			short newTpt = (new Short(input)).shortValue();
            			short[] spriteXY = mapcontrol.getSpriteXY(
            					areaX, areaY, num);
            			mapcontrol.removeSprite(
            					areaX, areaY, num);
            			mapcontrol.addSprite(
            					areaX, areaY,
    							spriteXY[0], spriteXY[1],
    							newTpt);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(EDIT_TPT))
        		{
            		short tpt = mapcontrol.getSpriteTpt(
            				areaX, areaY, num);
        			net.starmen.pkhack.JHack.main.showModule(
                			TPTEditor.class, new Integer (tpt));
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(COPY_SPRITE))
        		{
        			copyTpt = mapcontrol.getSpriteTpt(
        					areaX, areaY, num);
        		}
        		else if (ac.equals(CUT_SPRITE))
        		{
        			copyTpt = mapcontrol.getSpriteTpt(
        					areaX, areaY, num);
        			mapcontrol.removeSprite(areaX,
        					areaY, num);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(PASTE_SPRITE))
        		{
        			mapcontrol.addSprite(areaX, areaY,
        					(short) coordX, (short) coordY,
							copyTpt);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(ADD_DOOR))
        		{
        			mapcontrol.addDoor(areaX, areaY,
        					(short) (coordX / 8), (short) (coordY / 8),
							(byte) 0, (byte) 0);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(DEL_DOOR))
        		{
        			mapcontrol.removeDoor(areaX, areaY, num);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(EDIT_DEST))
        		{
        			EbMap.DoorLocation doorLocation =
        				mapcontrol.getDoorLocation(areaX, areaY, num);
        			destEditor.show(doorLocation);
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
        super.show();
        this.reset();
        mainWindow.setVisible(true);
        mainWindow.repaint();

        int[][] maparray = new int[screen_height][screen_width];
        for (int i = 0; i < screen_height; i++)
        {
            maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
        }
        gfxcontrol.setMapArray(maparray);
        gfxcontrol.remoteRepaint();
        
        TPTEditor.readFromRom(this);
        SpriteEditor.readFromRom(rom);
        
        /*if (mapcontrol.isDefaultSprites())
        {
        	JOptionPane.showMessageDialog(mainWindow,
        			"This ROM still has its map data in the default place." +
        			"\nIt will be moved and the rom will be expanded if necessary.");
        	mapcontrol.moveSpriteData();
        }*/
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }
    
    public void reset()
    {
    	mapcontrol = new EbMap(this);
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
        private EbHackModule hm;
        private int[][] maparray;
        private boolean maparrayisset = false;
        // modes: 0 = map editing (text), 1 = map editing (graphics)
        private int mode;
        private int sectorx;
        private int sectory;
        private boolean grid;
        private boolean spriteBoxes;

        private boolean knowsmap = false;
        private boolean knowssector = false;
        // This variable should define what a mode does.
        private int[][] modeprops = new int[][]{
        // { allow sectors to be selectable with right-click (0 = no, 1 = yes, 2
            // = change sector vars too), draw map (0=no, 1=text tiles, 2=gfx
            // tiles),
            //   allow map editing, sprite editing, door editing}
        		
            // {2, 1, 0, 0},
			// {2, 2, 0, 0}, 
			{2, 2, 1, 0, 0}, 
			{0, 2, 0, 1, 0},
			{0, 2, 0, 0, 1}
			};
        private int[] spriteProps;

        public MapGraphics(EbHackModule hm, int mode, 
        		boolean grid, boolean spriteBoxes)
        {
            this.hm = hm;
            this.mode = mode;
            this.grid = grid;
            this.spriteBoxes = spriteBoxes;
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
    				mapcontrol.getSpriteImage(spriteProps[2], spriteProps[3]);
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
                            .toHexString(row2draw[i2]), 2), (i2 * tilewidth)
							+ (tilewidth / 2), (i * tileheight)
                            + (tileheight / 2));
                        if (grid)
                        	g2d.draw(new Rectangle2D.Double(i2 * tilewidth,
                                    i * tileheight, tilewidth, tileheight));
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
                    	int sectorX = (i2 + x) / sector_width;
                    	int sectorY = (i + y) / sector_height;
                    	if (! mapcontrol.isSectorDataLoaded(
                    			sectorX, sectorY))
                    	{
                    		mapcontrol.loadSectorData(
                    				sectorX, sectorY);
                    	}
                        tile_set = mapcontrol.getDrawTileset(
                        		mapcontrol.getTset(sectorX, sectorY));
                        tile_tile = row2draw[i2]
                            | (mapcontrol.getLocalTileset(i2 + x, i + y) << 8);
                        tile_pal = 
                        	TileEditor.tilesets[tile_set].getPaletteNum(
                        			mapcontrol.getTset(sectorX, sectorY),
									mapcontrol.getPal(sectorX, sectorY));
                        mapcontrol.loadTileImage(tile_set, tile_tile, tile_pal);

                        g.drawImage(
                            mapcontrol.getTileImage(tile_set,tile_tile,tile_pal),
                            i2 * tilewidth, i * tileheight,
							tilewidth, tileheight, this);
                        if (grid)
                        	g2d.draw(new Rectangle2D.Double(i2 * tilewidth,
                            		(i * tileheight), tilewidth, tileheight));
                    }
                }

            }

            if ((getModeProps()[0] >= 1) && this.knowssector)
            {
                g2d.setPaint(Color.yellow);
                
                int drawSectorX =
                	(sectorx * sector_width * tilewidth)
					- (tilewidth * x);
                int drawSectorY =
                	(sectory * sector_height * tileheight)
					- (tilewidth * y);
            	int drawSectorW =
            		sector_width * tilewidth;
            	int drawSectorH =
            		sector_height * tileheight;
              
                g2d.draw(new Rectangle2D.Double(drawSectorX,
                    drawSectorY, drawSectorW, drawSectorH));
            }
            
            if (getModeProps()[3] == 1)
            {
            	// this.spriteLocs = new int[spriteData[0]][5];
            	for (int k = 0; k < screen_height; k++)
            	{
            		if ((((y + k) % (sector_height * 2)) == 0)
            				|| (k == 0))
            		{
            			for (int i = 0; i < screen_width; i++)
                    	{
                    		if ((((x + i) % sector_width) == 0)
                    				|| (i == 0))
                    		{
                    			if (! mapcontrol.isSpriteDataLoaded(
                    					(x + i) / sector_width,
										(y + k) / (sector_height * 2)))
                    			{
                    				mapcontrol.loadSpriteData(
                    						(x + i) / sector_width,
											(y + k) / (sector_height * 2));
                    			}
                    			int spritesNum = mapcontrol.getSpritesNum(
                    					(x + i) / sector_width, (y + k) / (sector_height * 2));
                            	short[][] spriteLocs = mapcontrol.getSpriteLocs(
                            			(x + i) / sector_width, (y + k) / (sector_height * 2));
                            	short[] spriteTpts = mapcontrol.getSpriteTpts(
                            			(x + i) / sector_width, (y + k) / (sector_height * 2));
                            	// this.spriteLocs = new int[spriteData[0]][5];
                                for (int j = 0; j < spritesNum; j++)
                                {
                                	TPTEditor.TPTEntry tptEntry = 
                                		TPTEditor.tptEntries[spriteTpts[j]];
                                	int spriteNum = tptEntry.getSprite();
                                	int spriteDrawY = spriteLocs[j][1];
                                	int spriteDrawX = spriteLocs[j][0];
                                	mapcontrol.loadSpriteImage(
                                			spriteNum, tptEntry.getDirection());
                                	SpriteEditor.SpriteInfoBlock sib =
                                		SpriteEditor.sib[spriteNum];
                                	
                                	if (((y + k) % (sector_height * 2)) > 0)
                                	{
                                		spriteDrawY -= ((y + k) % (sector_height * 2)) *
												tileheight;
                                	}
                                	
                                	if (((x + i) % sector_width) > 0)
                                	{
                                		spriteDrawX -= ((x + i) % sector_width)
												* tilewidth;
                                	}
                                	
                                	g.drawImage(
                                			mapcontrol.getSpriteImage(
                                					spriteNum,tptEntry.getDirection()),
                							spriteDrawX + (i * tilewidth),
        									spriteDrawY + (k * tileheight),
											this);
                                	if (spriteBoxes)
                                	{
                                		g2d.setPaint(Color.red);
                                		g2d.draw(new Rectangle2D.Double(
                                    			spriteDrawX + (i * tilewidth) - 1,
    											spriteDrawY + (k * tileheight) - 1,
    											mapcontrol.getSpriteImage(
    													spriteNum,tptEntry.getDirection())
    																	.getWidth(this) + 1,
    											mapcontrol.getSpriteImage(
    													spriteNum,tptEntry.getDirection())
    																	.getHeight(this) + 1));
                                	}
                                }
                    		}
                    	}
            		}
            	}
            }
            
            if (getModeProps()[4] == 1)
            {
            	for (int k = 0; k < screen_height; k++)
            	{
            		if ((((y + k) % sector_height) == 0)
            				|| (k == 0))
            		{
            			for (int i = 0; i < screen_width; i++)
                    	{
                    		if ((((x + i) % sector_width) == 0)
                    				|| (i == 0))
                    		{
                    			if (! mapcontrol.isDoorDataLoaded(
                    					(x + i) / sector_width,
										(y + k) / (sector_height * 2)))
                    			{
                    				mapcontrol.loadDoorData(
                    						(x + i) / sector_width,
											(y + k) / (sector_height * 2));
                    			}
                    			int doorsNum = mapcontrol.getDoorsNum(
                    					(x + i) / sector_width,
										(y + k) / (sector_height * 2));

                                for (int j = 0; j < doorsNum; j++)
                                {
                                	short[] doorXY = mapcontrol.getDoorXY(
                                			(x + i) / sector_width,
											(y + k) / (sector_height * 2),
											j);
                                	int doorDrawX = ((int) doorXY[0]) * 8;
                                	int doorDrawY = ((int) doorXY[1]) * 8;
                                	
                                	if (((y + k) % (sector_height * 2)) > 0)
                                	{
                                		doorDrawY -= ((y + k) % (sector_height * 2)) *
												tileheight;
                                	}
                                	
                                	if (((x + i) % sector_width) > 0)
                                	{
                                		doorDrawX -= ((x + i) % sector_width)
												* tilewidth;
                                	}
                                	
                                	g2d.setPaint(Color.blue);
                                	g2d.draw(new Rectangle2D.Double(
                                			doorDrawX + (i * tilewidth),
											doorDrawY + (k * tileheight),
											8,8));
                                }
                    		}
                    	}
            		}
            	}
            }
        }
        
        public void setSprite(int spriteX, int spriteY,
        		int spt, int direction)
        {
        	spriteProps = new int[] {
        			spriteX, spriteY, spt, direction
        	};
        }
        
        public int getSpriteNum(int spriteX, int spriteY)
        {
        	int[] spLocXY = getCoords(spriteX, spriteY);
        	
        	return mapcontrol.findSprite(
        			spLocXY[0], spLocXY[1],
					(short) spLocXY[2], (short) spLocXY[3]);
        }
        
        public short getSpriteTpt(int spriteX, int spriteY)
        {
        	int[] spLocXY = getCoords(spriteX, spriteY);
        	
        	int spriteNum = mapcontrol.findSprite(
        			spLocXY[0], spLocXY[1],
					(short) spLocXY[2], (short) spLocXY[3]);
        	if (spriteNum == -1)
        	{
        		return -1;
        	}
        	else
        	{
        		return mapcontrol.getSpriteTpt(
        				spLocXY[0], spLocXY[1], spriteNum);
        	}
        }
        
        public int getDoorNum(int doorX, int doorY)
        {
        	int[] doorXY = getCoords(doorX, doorY);
        	
        	return mapcontrol.findDoor(
        			doorXY[0], doorXY[1],
					(short) doorXY[2], (short) doorXY[3]);
        }
        
        public int[] getAreaXY(int spriteX, int spriteY)
        {
        	int areaX = 
        		((spriteX / tilewidth) + x) / sector_width;
        	int areaY = 
        		((spriteY / tileheight) + y) / (sector_height * 2);
        	return new int[] { areaX, areaY };
        }
        
        public int[] getCoords(int coordX, int coordY)
        {
        	int areaX = ((coordX / tilewidth) + x) / sector_width;
        	int areaY = ((coordY / tileheight) +y) / (sector_height * 2);
        	if ((x % sector_width) > 0)
        	{
        		if (((coordX / tilewidth) / sector_width) == 0)
        		{
        			coordX += (x % sector_width) * tilewidth;
        		}
        		else
        		{
            		coordX -= (sector_width - 
            				(x % sector_width)) * tilewidth;
        		}
        	}
    		coordX -= (((coordX / tilewidth) / sector_width)
    				* sector_width * tilewidth);
        	if ((y % (sector_height * 2)) > 0)
        	{
        		if (((coordY / tileheight) / (sector_height * 2)) == 0)
        		{
        			coordY += (y % (sector_height * 2)) * tileheight;
        		}
        		else
        		{
        			coordY -= ((sector_height * 2) - 
            				(y % (sector_height * 2))) * tileheight;
        		}
        	}
        	coordY -= (((coordY / tileheight) / (sector_height * 2))
        			* (sector_height * 2) * tileheight);
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

        public void changeMode(int newmode)
        {
            this.mode = newmode;
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

        public int getModeMax()
        {
            return this.modeprops.length;
        }

        public int getMode()
        {
            return this.mode;
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
    }

    // Represents the whole EarthBound map and map-related data in the rom.
    public class EbMap
    {
        private EbHackModule hm;
        private final int[] all_addresses = new int[]{
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
        private final int[][] doorCorrections = {
        		{49,-1},{81,-1},{97,-1},{98,-1},{140,-1},
        		{300,-1},{333,-1},{395,-1},{562,-1},
				{604,-1},{613,-1},{681,-1},{688,-1},
				{752,-1},{815,-1},{816,-1},{911,-1},
				{1136,-1},{1073,-2}
        };
    	private final int[] doorDestTypes = {
    			1, -1, 0, -1, -1, 2, 2
    	};

        private int tsetpalAddress = 0x17AA00;
        private int musicAddress = 0x1cd837;
        private int dPointersAddress = 0x100200;
        private int tsettbl_address = 0x2F121B;
        private int localtset_address = 0x175200;
        private final int spDataEnd = 0xf8b91;
        private final int spAsmPointer = 0x2461;
        private final int spDataBase = 0xf0200; // 2 byte ptr + 0xf0200 
        private ArrayList[] spData =
        	new ArrayList[(height_sectors / 2) * width_sectors];
        private Sector[] sectorData =
        	new Sector[height_sectors * width_sectors];
        private ArrayList[] doorData =
        	new ArrayList[(height_sectors / 2) * width_sectors];
        private int[] oldDoorEntryLengths =
        	new int[(height_sectors / 2) * width_sectors];
        private ArrayList destData = new ArrayList();
        private ArrayList destsLoaded = new ArrayList();
        private ArrayList destsIndexes = new ArrayList();
        private Image[][][] tileImages =
        	new Image[TILESET_NAMES.length][1024][maxpals];
        private Image[][] spriteImages =
        	new Image[SpriteEditor.NUM_ENTRIES][8];

        private AbstractRom rom;
        
        public EbMap(EbHackModule hm)
        {
            this.hm = hm;
            this.rom = hm.rom;
            
            
            	correctDoors();
        }
        
        public void loadTileImage(int loadtset, int loadtile, int loadpalette)
        {
            if (tileImages[loadtset][loadtile][loadpalette] == null)
            {
                tileImages[loadtset][loadtile][loadpalette] = TileEditor.tilesets[loadtset]
                    .getArrangementImage(loadtile, loadpalette);
            }
        }
        
        public Image getTileImage(int loadtset, int loadtile, int loadpalette)
        {
        	return tileImages[loadtset][loadtile][loadpalette];
        }
        
        public void resetTileImages()
        {
        	tileImages =
            	new Image[TILESET_NAMES.length][1024][maxpals];
        }
        
        public Image getSpriteImage(int spt, int direction)
        {
        	return spriteImages[spt][direction];
        }
        
        public void loadSpriteImage(int spt, int direction)
        {
           	if (this.spriteImages[spt][direction] == null)
           	{
           		SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spt];
           		spriteImages[spt][direction] = new SpriteEditor.Sprite(sib
                        .getSpriteInfo(direction), hm).getImage(true);
           	}
        }
        
        public void resetSpriteImages()
        {
        	spriteImages =
            	new Image[SpriteEditor.NUM_ENTRIES][8];
        }
        
        public void changeTile(int tilex, int tiley, int tile)
        {
        	int address = all_addresses[tiley] + tilex;
        	this.rom.write(address, tile);
        }
        
        public int getTile(int x, int y)
        {
        	return rom.read(all_addresses[y] + x)
				| (mapcontrol.getLocalTileset(x, y) << 8);
        }

        public int[] getTiles(int row, int start, int length)
        {
            int address = all_addresses[row] + start;

            int[] output = new int[length];
            for (int i = 0; i < output.length; i++)
            {
                int read_byte = rom.read(address + i);
                if (read_byte != -1)
                {
                	output[i] = read_byte;
                }
            }
            return output;
        }

        /*public int[] getTsetPal(int sectorx, int sectory)
        {
            int address = EbMap.tsetpal_address
                + (sectory * ((width + 1) / sector_width)) + sectorx;
            int tsetpal_data = rom.read(address);
            int palette = tsetpal_data & 0x7;
            int tileset = (tsetpal_data & 0xf8) >> 3;
            int[] tsetpal = new int[]{tileset, palette};
            return tsetpal;
        }*/
        
        public boolean isSectorDataLoaded(int sectorX, int sectorY)
        {
        	return (sectorData[sectorX + 
						   (sectorY * width_sectors)]
						   != null);
        }
        
        public void loadSectorData(int sectorX, int sectorY)
        {
        	
            int address = tsetpalAddress 
				+ (sectorY * ((width + 1) / sector_width)) + sectorX;
            byte tsetpal_data = rom.readByte(address);
            byte music = (byte) ((rom.readByte(musicAddress + 
            		(sectorY * ((width + 1) / sector_width))
					+ sectorX)) & 0xff);
        	sectorData[sectorX + (sectorY * width_sectors)]
					   = new Sector(
					   		(byte) ((tsetpal_data & 0xf8) >> 3),
							(byte) (tsetpal_data & 0x7),
							music);
        }
        
        public int getTset(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * width_sectors)]
							  .getTileset();
        }
        
        public int getPal(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * width_sectors)]
							  .getPalette();
        }
        
        public int getMusic(int sectorX, int sectorY)
        {
        	return (int) sectorData[sectorX + 
									(sectorY * width_sectors)]
							  .getMusic();
        }
        
        public void setTset(int sectorX, int sectorY, int newTset)
        {
        	sectorData[sectorX + 
						(sectorY * width_sectors)]
				  .setTileset((byte) newTset);
        }
        
        public void setPal(int sectorX, int sectorY, int newPal)
        {
        	sectorData[sectorX + 
						(sectorY * width_sectors)]
				  .setPalette((byte) newPal);
        }
        
        public void setMusic(int sectorX, int sectorY, int newMusic)
        {
        	sectorData[sectorX + 
						(sectorY * width_sectors)]
				  .setMusic((byte) newMusic);
        }
        
        public void writeSectorData()
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

        public int getDrawTileset(int mapTset)
        {
            if ((mapTset > map_tsets) || (mapTset < 0))
            {
                return -1;
            }
            int address = tsettbl_address + (mapTset * 2);
            int drawTset = rom.read(address);
            return drawTset;
        }

        public int getLocalTileset(int gltx, int glty)
        {
            int address = localtset_address
                + ((glty / 8) * (width + 1)) + gltx;
            if (((glty / 4) % 2) == 1)
                address += 0x3000;
            int local_tset = (rom.read(address) >>
            		((glty % 4) * 2)) & 3;

            return local_tset;
        }
        
        public void setLocalTileset(int tilex, int tiley, int newltset)
        {
        	int address = localtset_address
			    + ((tiley / 8) * (width + 1)) + tilex;
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
        
        public boolean isSpriteDataLoaded(int areaNum)
        {
        	return (spData[areaNum] != null);
        }
        
        public boolean isSpriteDataLoaded(int areaX, int areaY)
        {
        	return isSpriteDataLoaded(areaX + (areaY * width_sectors));
        }
        
        public void loadSpriteData(int areaNum)
        {
        	int spPtrsAddress = 
        		HackModule.toRegPointer(rom.readMulti(spAsmPointer,3));
        	int ptr = rom.readMulti(spPtrsAddress + (areaNum * 2), 2);
        	spData[areaNum] = new ArrayList();
       		if (ptr > 0)
       		{
       			/*System.out.println("spPtrsAddress: "
       					+ Integer.toHexString(spPtrsAddress)
						+ " ptr: "
						+ Integer.toHexString(ptr)
						+ " address: "
						+ Integer.toHexString(ptr + spDataBase)
						+ " length: "
						+ rom.read(spPtrsAddress + ptr));*/
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
           			this.spData[areaNum].add(new SpriteLocation(
           					tpt, spriteX, spriteY));
           		}
       		}
       	}
        
        public void loadSpriteData(int areaX, int areaY)
        {
        	loadSpriteData(areaX + (areaY * width_sectors));
        }
		
        public short[][] getSpriteLocs(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	
        	short[][] returnValue = 
        		new short[this.spData[areaNum].size()][2];
        	
        	for (int i = 0; i < this.spData[areaNum].size(); i++)
        	{
        		returnValue[i] = new short[] {
        				((SpriteLocation) this.spData[areaNum].get(i)).getX(),
						((SpriteLocation) this.spData[areaNum].get(i)).getY()
        		};
        	}
        	return returnValue;
        }
        
        public short[] getSpriteTpts(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	
        	short[] returnValue = 
        		new short[this.spData[areaNum].size()];
        	
        	for (int i = 0; i < this.spData[areaNum].size(); i++)
        	{
        		returnValue[i] = 
        			((SpriteLocation) this.spData[areaNum].get(i)).getTpt();
        	}
        	return returnValue;
        }
        
        public short[] getSpriteXY(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	SpriteLocation spLoc = (SpriteLocation) 
        			this.spData[areaNum].get(spNum);
        	return new short[] { spLoc.getX(), spLoc.getY() };
        }
        
        public short getSpriteTpt(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	return ((SpriteLocation) 
        			this.spData[areaNum].get(spNum)).getTpt();
        }
        
        public int getSpritesNum(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	
        	return this.spData[areaNum].size();
        }
        
        public void removeSprite(int areaX, int areaY, int spNum)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	this.spData[areaNum].remove(spNum);
        }
        
        public void removeSprite(int areaX, int areaY, short spTpt,
        		byte spX, byte spY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	this.spData[areaNum].remove(
        			this.spData[areaNum].indexOf(
        					new SpriteLocation(spTpt, spX, spY)));
        }
        
        public void addSprite(int areaX, int areaY, short newX,
        		short newY, short newTpt)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	this.spData[areaNum].add(
        			new SpriteLocation(newTpt, newX, newY));
        }
        
        public int findSprite(int areaX, int areaY,
        		short spriteX, short spriteY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	for (int i = 0; i < this.spData[areaNum].size(); i++)
        	{
        		SpriteLocation spLoc = 
        			(SpriteLocation) this.spData[areaNum].get(i);
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
        
        /*public void moveSpriteData()
        {
        	int oldSpDataBase = spDataBase;
        	spDataBase = spPtrsAddress - spDataAddress;
        	int oldSpPtrsAddress = spPtrsAddress;
        	
        	byte[] newPointerData = new byte[spData.length * 2];
        	for (int i = 0; i < spData.length; i++)
        	{
                int oldPointer = 
                	rom.readMulti(oldSpPtrsAddress + (i * 2), 2);
        		if (oldPointer == 0)
        		{
        			newPointerData[i * 2] = 0;
        			newPointerData[1 + (i * 2)] = 0;
        		}
        		else
        		{
        			int newPointer = oldPointer - oldSpDataBase + spDataBase;

        			newPointerData[i * 2] =
        				(byte) (newPointer & 0xFF);
        			newPointerData[1 + (i * 2)] =
        				(byte) (newPointer / 0x100);
        		}
        	}

        	boolean write = hm.writetoFree(newPointerData,
        			spAsmPointer, 3,
        			spData.length * 2,
        			spData.length * 2,
					true);
        	System.out.println("moving went ok? " + write);
        	
        	rom.write(spDataAddress + spDataBase,
        			rom.read(spDataAddress + oldSpDataBase,
        					0x1daa));
        	spPtrsAddress = HackModule.toRegPointer(
        			rom.readMulti(spAsmPointer, 3));
        }*/
        
        public boolean writeSprites()
        {
        	int spPtrsAddress = 
        		HackModule.toRegPointer(rom.readMulti(spAsmPointer,3));
        	ArrayList spriteData = new ArrayList();
        	byte[] pointerData = new byte[2 * spData.length];
        	int whereToPut = 0xf63e7 - spDataBase;
        	int debugLength = 0;
        	for (int i = 0; i < spData.length; i++)
        	{ 			
        		if (! isSpriteDataLoaded(i))
        			loadSpriteData(i);
        		
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
        
        private byte[] toByteArray(ArrayList list)
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
        
        public void nullSpriteData()
        {
        	for (int i = 0; i < spData.length; i++)
        		spData[i] = new ArrayList();
        }
        
        public void loadDoorData(int areaNum)
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
       			if (doorDestTypes[doorType] == -1)
       				doorData[areaNum].add(new DoorLocation(
       						doorX, doorY, doorType,
							(byte) ((doorPtr & 0xc0) >> 6)));
       			else
       			{
       				int destIndex =
       					loadDestData(0xf0200 + doorPtr, doorType);
       				doorData[areaNum].add(new DoorLocation(
           					doorX, doorY, doorType, 
							doorPtr, destIndex));
       			}
       		}
        }
        
        public void loadDoorData(int areaX, int areaY)
        {
        	loadDoorData(areaX + (areaY * width_sectors));
       	}
        
        public boolean isDoorDataLoaded(int areaNum)
        {
        	return (doorData[areaNum] != null);
        }
        public boolean isDoorDataLoaded(int areaX, int areaY)
        {
        	return isDoorDataLoaded(areaX + (areaY * width_sectors));
        }
        
        public int getDoorsNum(int areaNum)
        {
        	return doorData[areaNum].size();
        }
        
        public int getDoorsNum(int areaX, int areaY)
        {
        	return getDoorsNum(areaX + (areaY * width_sectors));
        }
        
        public short[] getDoorXY(int areaX, int areaY, int doorNum)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	DoorLocation dLoc = 
        		(DoorLocation) doorData[areaNum].get(doorNum);
        	return new short[] { dLoc.getX(), dLoc.getY() };
        }
        
        public void addDoor(
        		int areaX, int areaY, short doorX,
        		short doorY, byte doorType, short doorPtr)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	doorData[areaNum].add(
        			new DoorLocation(
        					doorX, doorY, doorType, doorPtr, 0));
        }
        
        public void removeDoor(int areaX, int areaY, int num)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	doorData[areaNum].remove(num);
        }
        
        public DoorLocation getDoorLocation(int areaNum, int num)
        {
        	return (DoorLocation) doorData[areaNum].get(num);
        }
        
        public DoorLocation getDoorLocation(int areaX, int areaY, int num)
        {
        	return getDoorLocation(areaX + (areaY * width_sectors), num);
        }
        
        public int findDoor(int areaX, int areaY,
        		short doorX, short doorY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
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
        
        private void correctDoors()
        {
        	for (int i = 0; i < doorCorrections.length; i++)
        	{
        		int ptr = HackModule.toRegPointer(rom.readMulti(
            			dPointersAddress
						+ (doorCorrections[i][0] * 4), 4));
            	byte oldLen = rom.readByte(ptr);
            	rom.write(ptr, oldLen + doorCorrections[i][1]);
        	}
        	rom.write(0x2e9430,new int[] {0xff,0xff,0xff});
        }
        
        public boolean writeDoors()
        {
        	for (int i = 0; i < doorData.length; i++)
        	{
        		if (! isDoorDataLoaded(i))
        			loadDoorData(i);
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
            				toWrite[5 + (j * 5)] =
            					(byte) 
									(doorLocation.getPointer() & 0xff);
            				toWrite[6 + (j * 5)] =
            					(byte)
									((doorLocation.getPointer() & 0xff00) 
											/ 0x100);
            			}
            		}
            		boolean writeOK = hm.writetoFree(toWrite,
                			dPointersAddress + (i * 4), 4,
							(oldDoorEntryLengths[i] * 5) + 2,
							toWrite.length, true);
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
        
        public int loadDestData(int address, int type)
        {
        	System.out.println("loadDestData address: "
        			+ Integer.toHexString(address)
					+ " is Loaded: "
					+ destsLoaded.contains(new Integer(address)));
        	if (! destsLoaded.contains(new Integer(address)))
        	{
        		Destination dest = null;
        		if (doorDestTypes[type] == 0)
        		{
        			int pointer = rom.readMulti(address, 4);
        			short flag = (short) (rom.readMulti(address + 4, 2));
        			short yCoord = (short) (rom.readMulti(address + 6, 2));
        			short xCoord = (short) (rom.readMulti(address + 8, 2));
        			byte style = rom.readByte(address + 10);
        			byte direction = 0; // highest 2 bits of yCoord wtf? ):
        			dest = new Destination(pointer, flag, xCoord,
        					yCoord, style, direction);
        		}
        		else if (doorDestTypes[type] == 1)
        		{
        			short flag = (short) (rom.readMulti(address, 2));
        			int pointer = rom.readMulti(address + 2, 4);
        			dest = new Destination(flag, pointer);
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
        			System.out.println("destIndex: " + destIndex);
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
        
        public Destination getDestination(int index)
        {
        	return (Destination) destData.get(index);
        }
        
        public class SpriteLocation
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
        
        public class DoorLocation
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
        	}
        	
        	public DoorLocation(short x, short y,
        			byte type, byte misc)
        	{
        		this.x = x;
        		this.y = y;
        		this.type = type;
        		this.misc = misc;
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

        public class Sector
		{
        	private byte tileset, palette, music;
        	
        	public Sector(byte tileset, byte palette, byte music)
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
        	
        	public byte getMusic()
        	{
        		return music;
        	}
		}
        
        
        public class Destination
		{
        	private int pointer;
        	private short flag, yCoord, xCoord;
        	private byte style, direction, type;
        	
        	public Destination(int pointer, short flag, short xCoord,
        			short yCoord, byte style, byte direction)
        	{
        		this.pointer = pointer;
        		this.flag = flag;
        		this.xCoord = xCoord;
        		this.yCoord = yCoord;
        		this.style = style;
        		this.direction = direction;
        		this.type = 0;
        	}
        	
        	public Destination(short flag, int pointer)
        	{
        		this.flag = flag;
        		this.pointer = pointer;
        		this.type = 1;
        	}
        	
        	public Destination(int pointer)
        	{
        		this.pointer = pointer;
        		this.type = 2;
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
		}
    }

    public class EditBox extends AbstractButton
	{
    	private int selected, selectedx, selectedy, tset, pal, hscroll;
    	
    	public EditBox()
    	{
    		this.tset = -1;
    		this.pal = -1;
    		this.selected = -1;
    		this.selectedx = -1;
    		this.selectedy = -1;
    		this.hscroll = 0;
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
                }
                drawBorder(g2d);
                drawSelected(g2d);
            }
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
    		int tilex = newselectedx / tilewidth;
    		int tiley = newselectedy / tileheight;
    		
    		this.selected = ((this.hscroll + tilex)
    				* (editheight + 1)) + tiley;
    		this.selectedx = tilex + this.hscroll;
    		this.selectedy = tiley;
    	}
    	
    	public void setSelected(int tile)
    	{
    		selected = tile;
    		selectedx = tile / (editheight + 1);
    		selectedy = tile % (editheight + 1);
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
					&& (this.selectedx <= this.hscroll + editwidth))
    		{
    			g2d.setPaint(Color.yellow);
    			g2d.draw(new Rectangle2D.Double(
    					(this.selectedx - this.hscroll) * tilewidth,
    					this.selectedy * tileheight, tilewidth, tileheight));
    		}
    	}
    	
    	public void drawBorder(Graphics2D g2d)
		{
            g2d.setPaint(Color.black);
            for (int i = 0; i <= editwidth; i++)
            {
                for (int j = 0; j <= editheight; j++)
                {
                    g2d.draw(new Rectangle2D.Double(i * tilewidth,
                    		j * tileheight, tilewidth,
							tileheight));
                }
            }
		}
    	
    	public void drawTiles(Graphics g, Graphics2D g2d)
    	{
            g2d.setPaint(Color.black);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
    		for (int i = 0; i <= editwidth; i++)
    		{
    			for (int j = 0; j <= editheight; j++)
    			{
    				int tile = ((hscroll + i) * (editheight + 1)) + j;
    				if (gfxcontrol.getModeProps()[1] == 1)
    				{
                        g2d.drawString(addZeros(
                        		Integer.toHexString(tile), 2), 
    							(i * tilewidth), (j * tileheight));
    				}
    				else if (gfxcontrol.getModeProps()[1] == 2)
    				{
        				mapcontrol.loadTileImage(tset, tile, pal);
        				g.drawImage(
        						mapcontrol.getTileImage(tset,tile,pal),
    							(i * tilewidth), (j * tileheight),
    							tilewidth, tileheight, this);
    				}
    			}
    		}
    	}
	}
    
    public class DoorDestinationEditor extends EbHackModule implements ActionListener
    {
    	private JTextField 
			numField, ptrField, xField, yField,
			dirField, flagField, styleField;
    	private JComboBox typeBox;
    	private EbMap.DoorLocation doorLocation;
    	private final String[] typeNames = {
    			"Switch", "Rope/Ladder", "Door", "Escalator",
				"Stairway", "Object", "Person"
    	};
    	
        /**
         * @param rom
         * @param prefs
         */
        public DoorDestinationEditor(AbstractRom rom, XMLPreferences prefs)
        {
            super(rom, prefs);
        }

    	/* (non-Javadoc)
    	 * @see net.starmen.pkhack.HackModule#init()
    	 */
    	protected void init()
    	{
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
    		return "Door Destination Editor";
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
    		mainWindow.hide();
    	}
    	
    	public void show()
    	{
    		super.show();
    		this.reset();
    		mainWindow.setVisible(true);
    	}
    	
    	public void show(EbMap.DoorLocation doorLocation)
    	{
    		this.doorLocation = doorLocation;
    		this.show();
    		updateComponents();
    	}
    	
    	public void createGUI()
    	{
    		mainWindow = createBaseWindow(this);
    		mainWindow.setTitle(this.getDescription());
    		
    		JPanel entryPanel = new JPanel(new GridLayout(2,2));
    		
    		entryPanel.add(new JLabel("Destination:"));
    		numField = new JTextField();
    		entryPanel.add(numField);
    		
    		entryPanel.add(new JLabel("Type:"));
    		typeBox = new JComboBox(typeNames);
    		entryPanel.add(typeBox);
    		
    		JPanel destPanel = new JPanel(new GridLayout(6,2));
    		
    		destPanel.add(new JLabel("Pointer:"));
    		ptrField = new JTextField();
    		destPanel.add(ptrField);
    		
    		destPanel.add(new JLabel("Event flag:"));
    		flagField = new JTextField();
    		destPanel.add(flagField);
    		
    		destPanel.add(new JLabel("X:"));
    		xField = new JTextField();
    		destPanel.add(xField);
    		
    		destPanel.add(new JLabel("Y:"));
    		yField = new JTextField();
    		destPanel.add(yField);
    		
    		destPanel.add(new JLabel("Direction:"));
    		dirField = new JTextField();
    		destPanel.add(dirField);
    		
    		destPanel.add(new JLabel("Style:"));
    		styleField = new JTextField();
    		destPanel.add(styleField);
    		
    		JPanel contentPanel = new JPanel();
    		contentPanel.setLayout(
    				new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    		contentPanel.add(new JLabel("Entry Properties:", 0));
    		contentPanel.add(entryPanel);
    		contentPanel.add(new JLabel("Destination Properties", 0));
    		contentPanel.add(destPanel);
    		
    		mainWindow.getContentPane().add(
    				contentPanel, BorderLayout.CENTER);
    		mainWindow.pack();
    	}
    	
    	public void updateComponents()
    	{
    		numField.setText(Integer.toString(doorLocation.getDestIndex()));
    		typeBox.setSelectedIndex((int) doorLocation.getType());
    		EbMap.Destination dest = 
    			mapcontrol.getDestination(
    					doorLocation.getDestIndex());
    		ptrField.setText(Integer.toHexString(dest.getPointer()));
    		xField.setText(Integer.toString(dest.getX()));
    		yField.setText(Integer.toString(dest.getY()));
    		dirField.setText(Integer.toString(dest.getDirection()));
    		flagField.setText(Integer.toHexString(dest.getFlag()));
    		styleField.setText(Integer.toHexString(dest.getStyle()));
    	}
    	
        public void actionPerformed(ActionEvent e)
        {
            if (e.getActionCommand().equals("apply"))
            {
            	// writeToRom();
            }
            else if (e.getActionCommand().equals("close"))
            {
                hide();
            }
        }
    }
}