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
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

public class MapEditor extends EbHackModule implements ActionListener,
    PropertyChangeListener, ItemListener
{
    public MapEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
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

    //	private MapEditor app = new MapEditor();
    private EbMap mapcontrol;
    private MapGraphics gfxcontrol;
    private EditBox editbox;

    //	public static MapGraphics gfxcontrol = new MapGraphics();

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
        
        JMenu menu = new JMenu("File");
        menu.add(EbHackModule.createJMenuItem(
        		"Save Changes", 's', null, 
				MenuListener.SAVE, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Exit", 'q', null, 
				MenuListener.EXIT, 
				menuListener));
        menuBar.add(menu);
		
		menu = new JMenu("Mode");
        menu.add(EbHackModule.createJMenuItem(
        		"Map Edit", '1', null, 
				MenuListener.MODE0, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Sprite Edit", '2', null, 
				MenuListener.MODE1, 
				menuListener));
        menu.add(EbHackModule.createJMenuItem(
        		"Door Edit", '2', null, 
				MenuListener.MODE2, 
				menuListener));
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
        		screen_width, 0, width + 1);
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
        gfxcontrol = new MapGraphics(this, 0);
        editbox = new EditBox();

        // Create the GUI.
        createGUI();

        // Testing stuff.
        /*
         * int[] test_row = mapcontrol.getTiles(0, 0, 5); for (int i = 0; i <
         * test_row.length; i++) { System.out.println("Test row tile " + i + ": " +
         * Integer.toHexString(test_row[i])); }
         */
    }
    
    public void writeToRom()
    {
    	JOptionPane.showMessageDialog(mainWindow,
    			"Map changes save on-the-fly.\n"
    			+ "This will only save sprite data.");
    	mapcontrol.writeSectorData();
    	boolean spWrite = mapcontrol.writeSprites();
    	if (! spWrite)
    		JOptionPane.showMessageDialog(mainWindow,
        			"This is so embarassing!\n"
        			+ "For some reason, I could not save "
					+ "the sprite data?\nThis shouldn't happen...");
    	else
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
            		music = newMusic;
                	int[] sectorxy = gfxcontrol.getSectorxy();
                	mapcontrol.setMusic(
                			sectorxy[0], sectorxy[1], newMusic);
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
			int[] modeprops = gfxcontrol.getModeProps();
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
            		&& (gfxcontrol.getModeProps()[2] == 1)
					&& (gfxcontrol.knowsSector()))
            {
            	int mousex = e.getX();
                int mousey = e.getY();
                editbox.setSelected(mousex, mousey);
                editbox.remoteRepaint();
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
        			gfxcontrol.getSpLocXY(mousex, mousey);
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
        	if ((e.getButton() == 1) && (editbox.isSelected())
        			&& (gfxcontrol.getModeProps()[2] == 1))
        	{
        		int mousex = e.getX();
        		int mousey = e.getY();
        		int mapx = mousex / tilewidth;
        		int mapy = mousey / tileheight;
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
                			gfxcontrol.getSpLocXY(e.getX(), e.getY());
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
                			gfxcontrol.getSpLocXY(e.getX(), e.getY());
                		PopupListener popupListener =
                			new PopupListener(spLoc[0], spLoc[1],
                					spLoc[2], spLoc[3], -1);
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
        	
        	private int areaX, areaY, spriteX, spriteY, spNum;
	    	
        	public PopupListener(int areaX, int areaY,
	    			int spriteX, int spriteY, int spNum)
	    	{
	    		this.areaX = areaX;
	    		this.areaY = areaY;
	    		this.spriteX = spriteX;
	    		this.spriteY = spriteY;
	    		this.spNum = spNum;
	    	}
        	
        	public void actionPerformed(ActionEvent e)
        	{
        		String ac = e.getActionCommand();
        		if (ac.equals(ADD_SPRITE))
        		{
        			mapcontrol.addSprite(areaX, areaY,
        					(short) spriteX, (short) spriteY,
							(short) 0);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(DEL_SPRITE)
        				&& (spNum != -1))
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
            					areaY, spNum);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(CHANGE_TPT)
        				&& (spNum != -1))
        		{
            		short tpt = mapcontrol.getSpriteTpt(
            				areaX, areaY, spNum);
            		String input = JOptionPane.showInputDialog(
                            mainWindow,
                            "Change which TPT entry this"
							+ " sprite entry will display.",
                            Integer.toString((int) tpt));
            		if (input != null)
            		{
            			short newTpt = (new Short(input)).shortValue();
            			short[] spriteXY = mapcontrol.getSpriteXY(
            					areaX, areaY, spNum);
            			mapcontrol.removeSprite(
            					areaX, areaY, spNum);
            			mapcontrol.addSprite(
            					areaX, areaY,
    							spriteXY[0], spriteXY[1],
    							newTpt);
            			gfxcontrol.remoteRepaint();
            		}
        		}
        		else if (ac.equals(EDIT_TPT)
        				&& (spNum != -1))
        		{
            		short tpt = mapcontrol.getSpriteTpt(
            				areaX, areaY, spNum);
        			net.starmen.pkhack.JHack.main.showModule(
                			TPTEditor.class, new Integer (tpt));
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(COPY_SPRITE)
        				&& (spNum != -1))
        		{
        			copyTpt = mapcontrol.getSpriteTpt(
        					areaX, areaY, spNum);
        		}
        		else if (ac.equals(CUT_SPRITE)
        				&& (spNum != -1))
        		{
        			copyTpt = mapcontrol.getSpriteTpt(
        					areaX, areaY, spNum);
        			mapcontrol.removeSprite(areaX,
        					areaY, spNum);
        			gfxcontrol.remoteRepaint();
        		}
        		else if (ac.equals(PASTE_SPRITE))
        		{
        			mapcontrol.addSprite(areaX, areaY,
        					(short) spriteX, (short) spriteY,
							copyTpt);
        			gfxcontrol.remoteRepaint();
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
        
        if (mapcontrol.isDefaultSprites())
        {
        	JOptionPane.showMessageDialog(mainWindow,
        			"This ROM still has its map data in the default place." +
        			"\nIt will be moved and the rom will be expanded if necessary.");
        	mapcontrol.moveSpriteData();
        }
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
        return "Written by MrTenda\n"
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

        public MapGraphics(EbHackModule newhm, int newmode)
        {
            this.hm = newhm;

            this.mode = newmode;
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
    			g2d.setPaint(Color.red);
    			g2d.draw(new Rectangle2D.Double(
            			spriteProps[0] - 1,
						spriteProps[1] - 1,
						spImage.getWidth(this) + 1,
						spImage.getHeight(this) + 1));
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
        	this.spriteProps = new int[] {
        			spriteX, spriteY, spt, direction
        	};
        }
        
        public int getSpriteNum(int spriteX, int spriteY)
        {
        	int[] spLocXY = getSpLocXY(spriteX, spriteY);
        	
        	return mapcontrol.findSprite(
        			spLocXY[0], spLocXY[1],
					(short) spLocXY[2], (short) spLocXY[3]);
        }
        
        public short getSpriteTpt(int spriteX, int spriteY)
        {
        	int[] spLocXY = getSpLocXY(spriteX, spriteY);
        	
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
        
        public int[] getAreaXY(int spriteX, int spriteY)
        {
        	int areaX = 
        		((spriteX / tilewidth) + x) / sector_width;
        	int areaY = 
        		((spriteY / tileheight) + y) / (sector_height * 2);
        	return new int[] { areaX, areaY };
        }
        
        public int[] getSpLocXY(int spriteX, int spriteY)
        {
        	int areaX = ((spriteX / tilewidth) + x) / sector_width;
        	int areaY = ((spriteY / tileheight) +y) / (sector_height * 2);
        	if ((x % sector_width) > 0)
        	{
        		if (((spriteX / tilewidth) / sector_width) == 0)
        		{
        			spriteX += (x % sector_width) * tilewidth;
        		}
        		else
        		{
            		spriteX -= (sector_width - 
            				(x % sector_width)) * tilewidth;
        		}
        	}
    		spriteX -= (((spriteX / tilewidth) / sector_width)
    				* sector_width * tilewidth);
        	if ((y % (sector_height * 2)) > 0)
        	{
        		if (((spriteY / tileheight) / (sector_height * 2)) == 0)
        		{
        			spriteY += (y % (sector_height * 2)) * tileheight;
        		}
        		else
        		{
        			spriteY -= ((sector_height * 2) - 
            				(y % (sector_height * 2))) * tileheight;
        		}
        	}
        	spriteY -= (((spriteY / tileheight) / (sector_height * 2))
        			* (sector_height * 2) * tileheight);
        	return new int[] { areaX, areaY, spriteX, spriteY };
        }
        
        /*protected int[] sectorDisplayWoH(int sectorxory, int sector_woh,
            int woh, int xory, int tilewoh)
        {

            int draw_sector_xoy, draw_sector_woh;

            if (sectorxory == 0)
            {
            	draw_sector_xoy = 0; 
                draw_sector_woh = (sector_woh - xory) * tilewoh;
            }
            else
            {
                draw_sector_xoy = (sectorxory * tilewoh * sector_woh)
				- (tilewoh * xory);
                if ((draw_sector_xoy + (sector_woh * tilewoh)) > (woh * tilewoh))
                {
                    draw_sector_woh = (sector_woh * tilewoh)
                        - ((draw_sector_xoy + (sector_woh * tilewoh)) - (woh * tilewoh));
                }
                else
                {
                    draw_sector_woh = sector_woh * tilewoh;
                }
            }

            return new int[]{draw_sector_xoy, draw_sector_woh};
        }*/

        /*
         * protected void drawBorder(Graphics2D g2d) {
         * g2d.setPaint(Color.black); Rectangle2D.Double border = new
         * Rectangle2D.Double(this.startx - 1, this.starty - 1, (this.width *
         * this.tilewidth) + 1, (this.height * this.tileheight) + 1);
         * g2d.draw(border); }
         */

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
    }

    // Represents the whole EarthBound map and map-related data in the rom.
    public class EbMap
    {
        private EbHackModule hm;
        // this.all_addresses = new int[] { 0x160200, 0x162A00, 0x165200,
        // 0x168200, 0x16AA00, 0x16D200, 0x170200, 0x172A00 };
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
        private int tsetpalAddress = 0x17AA00;
        private int musicAddress = 0x1cd837;
        private int dPointersAddress = 0x100200;
        private int tsettbl_address = 0x2F121B;
        private int localtset_address = 0x175200;
        private int spPtrsAddress;
        private int spDataAddress = 0xf0200;
        private int spDataBase;
        private int spDataEnd = 0xf8b91;
        private int spAsmPointer = 0x2461;
        private ArrayList[] spData =
        	new ArrayList[(height_sectors / 2) * width_sectors];
        private Sector[] sectorData =
        	new Sector[height_sectors * width_sectors];
        private ArrayList[] doorData =
        	new ArrayList[(height_sectors / 2) * width_sectors];
        private Image[][][] tileImages =
        	new Image[TILESET_NAMES.length][1024][maxpals];
        private Image[][] spriteImages =
        	new Image[SpriteEditor.NUM_ENTRIES][8];

        private AbstractRom rom;
        
        public EbMap(EbHackModule hm)
        {
            this.hm = hm;
            this.rom = hm.rom;
            
            spPtrsAddress = HackModule.toRegPointer(
            		rom.readMulti(spAsmPointer, 3));
            
            if (spPtrsAddress == 0xF63E7)
            {
            	spDataBase = 0x6be7;
            }
            else
            {
            	spDataBase = 0x61e7;
            }
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
            byte music = rom.readByte(musicAddress + 
            		(sectorY * ((width + 1) / sector_width))
					+ sectorX);
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
        
        public boolean isSpriteDataLoaded(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	return (spData[areaNum] != null);
        }
        
        public void loadSpriteData(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	int[] ptr = rom.read(spPtrsAddress + (areaNum * 2), 2);
        	int ptrInt = ptr[0]  + (ptr[1] * 0x100);
        	this.spData[areaNum] = new ArrayList();
       		if (ptrInt > 0)
       		{
            	int[] data = rom.read(
            			spDataAddress + ptrInt,
            			(rom.read(spDataAddress + ptrInt)
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
        
        public boolean isDefaultSprites()
        {
        	return (spPtrsAddress == 0xf63e7);
        }
        
        public void moveSpriteData()
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
					false);
        	
        	rom.write(spDataAddress + spDataBase,
        			rom.read(spDataAddress + oldSpDataBase,
        					0x1daa));
        	spPtrsAddress = HackModule.toRegPointer(
        			rom.readMulti(spAsmPointer, 3));
        }
        
        public boolean writeSprites()
        {
        	int spritesNum = 0, whereToPut = 0;
        	for (int i = 0; i < spData.length; i++)
        	{
        		if (spData[i] != null)
        		{
        			if (spData[i].size() > 0)
        				spritesNum += 2 + (spData[i].size() * 4);
        		}
        		else
        		{
        			int pointer =
        				rom.readMulti(spPtrsAddress + (i * 2), 2);
        			if (pointer > 0)
        			{
        				spritesNum += 2 + (rom.read(
        						spDataAddress + pointer) * 4);
        			}
        		}
        	}
        	byte[] newSpData = new byte[spritesNum];
        	byte[] newPointerData = new byte[spData.length * 2];
        	for (int i = 0; i < spData.length; i++)
        	{
        		if (spData[i] == null)
        		{
        			int pointer =
        				rom.readMulti(spPtrsAddress + (i * 2), 2);
        			if (pointer > 0)
        			{
        				int newPointer = spDataBase + whereToPut;
            			newPointerData[i * 2] =
            				(byte) (newPointer & 0xFF);
            			newPointerData[1 + (i * 2)] =
            				(byte) (newPointer / 0x100);
        				
        				byte[] areaData =
        					rom.readByte(spDataAddress + pointer,
        							2 + (rom.read(spDataAddress + pointer)
        									* 4));
    					System.arraycopy(areaData, 0, 
        						newSpData, whereToPut,
        						areaData.length);
        				whereToPut += areaData.length;
        			}
        			else
        			{
        				newPointerData[i * 2] = 0;
        				newPointerData[1 + (i * 2)] = 0;
        			}
        		}
    			else if (spData[i].size() == 0)
    			{
        			newPointerData[i * 2] = 0;
        			newPointerData[1 + (i * 2)] = 0;
    			}
        		else
        		{
        			int newPointer = spDataBase + whereToPut;
        			newPointerData[i * 2] =
        				(byte) (newPointer & 0xFF);
        			newPointerData[1 + (i * 2)] =
        				(byte) (newPointer / 0x100);
        			byte[] areaData = new byte[2 + (spData[i].size() * 4)];
        			areaData[0] = (byte) spData[i].size();
        			areaData[1] = 0;
        			for (int j = 0; j < spData[i].size(); j++)
        			{
        				SpriteLocation spLoc = 
        					((SpriteLocation) spData[i].get(j));
        				TPTEditor.TPTEntry tptEntry = 
                    		TPTEditor.tptEntries[spLoc.getTpt()];
                    	SpriteEditor.SpriteInfoBlock sib =
                    		SpriteEditor.sib[tptEntry.getSprite()];
        				areaData[2 + (j * 4)] = 
        					(byte) (spLoc.getTpt() & 0xff);
        				areaData[3 + (j * 4)] = 
        					(byte) (spLoc.getTpt() / 0x100);
        				areaData[4 + (j * 4)] = 
        					(byte) (spLoc.getY() + 
        							((short) (sib.height * 6)));
        				areaData[5 + (j * 4)] =
        					(byte) (spLoc.getX() +
        							((short) (sib.width * 4)));
        			}
        			System.arraycopy(areaData, 0,
        					newSpData, whereToPut, areaData.length);
        			whereToPut += areaData.length;
        		}
        	}
        	
        	boolean write = hm.writetoFree(newPointerData,
        			spAsmPointer, 3,
        			spData.length * 2,
        			spData.length * 2,
					false);
        	if (! write)
        		return false;
        	
        	if (spDataAddress + spDataBase + newSpData.length
        			>= spDataEnd)
        		return false;
        			
        	rom.write(spDataAddress + spDataBase,
        			newSpData);
        	spPtrsAddress = HackModule.toRegPointer(
        			rom.readMulti(spAsmPointer, 3));
        	
        	return true;
        }
        
        public void nullSpriteData()
        {
        	for (int i = 0; i < spData.length; i++)
        		spData[i] = new ArrayList();
        }
        
        public void loadDoorData(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	int ptr = HackModule.toRegPointer(rom.readMulti(
        			dPointersAddress + (areaNum * 4), 4));
        	doorData[areaNum] = new ArrayList();
       		if (ptr > 0)
       		{
       			int len = rom.read(ptr);
           		for (int i = 0; i < len; i++)
           		{
           			byte doorX = rom.readByte(ptr + 3 + (i * 4));
           			byte doorY = rom.readByte(ptr + 2 + (i * 4));
           			byte doorType = rom.readByte(ptr + 4 + (i * 4));
           			short doorPtr = (short) rom.readMulti(
           					ptr + 5 + (i * 4), 2);
           			doorData[areaNum].add(new DoorLocation(
           					doorX, doorY, doorType, doorPtr));	
           		}
       		}
       	}
        
        public boolean isDoorDataLoaded(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	return (doorData[areaNum] != null);
        }
        
        public int getDoorsNum(int areaX, int areaY)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	return doorData[areaNum].size();
        }
        
        public short[] getDoorXY(int areaX, int areaY, int doorNum)
        {
        	int areaNum = areaX + (areaY * width_sectors);
        	DoorLocation dLoc = 
        		(DoorLocation) doorData[areaNum].get(doorNum);
        	return new short[] { dLoc.getX(), dLoc.getY() };
        }
        
        private class SpriteLocation
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
		}
        
        private class DoorLocation
		{
        	private byte type;
        	private short x, y, pointer;
        	
        	public DoorLocation(short x, short y,
        			byte type, short pointer)
        	{
        		this.x = x;
        		this.y = y;
        		this.type = type;
        		this.pointer = pointer;
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
		}

        private class Sector
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
    	
    	public void setScroll(int newhscroll)
    	{
    		this.hscroll = newhscroll;
    	}
    	
    	public void setSelected(int newselectedx, int newselectedy)
    	{
    		int tilex = 0;
    		int tiley = 0;
    		for (int i = 0; newselectedx - (i * tilewidth) > tilewidth; i++)
    		{
    			tilex++;
    		}
    		
    		for (int i = 0; newselectedy - (i * tileheight) > tileheight; i++)
    		{
    			tiley++;
    		}
    		
    		this.selected = ((this.hscroll + tilex)
    				* (editheight + 1)) + tiley;
    		this.selectedx = tilex + this.hscroll;
    		this.selectedy = tiley;
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
}
