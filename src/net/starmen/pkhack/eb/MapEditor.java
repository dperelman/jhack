package net.starmen.pkhack.eb;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.text.*;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

public class MapEditor extends EbHackModule implements ActionListener, PropertyChangeListener, MouseListener {

	public MapEditor(Rom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}

	private JFrame mainWindow;

	// Stuff for the top buttons
	private JPanel top_buttons;
	private int x = 0;
	private int y = 0;
	private int palette = 0;
	private int music = 0;
	private int window_width = 650;
	private int window_height = 500;
	// real dimensions are 256 x 320, but I'm starting from 0.
	private int width = 255;
	private int height = 319;
	private int screen_x = 5;
	private int screen_y = 10;
	private int tilewidth = 32;
	private int tileheight = 32;
	private int screen_width = ((window_width - (2 * screen_x)) / tilewidth) - 1;
	private int screen_height = ((window_height - (2 * screen_y)) / tileheight) - 4;
	private int sector_width = 8;
	private int sector_height = 4;
	private static final int draw_tsets = 20;
	private static final int map_tsets = 32;
	private NumberFormat num_format;
	private JFormattedTextField xField;
	private JFormattedTextField yField;
	private JComboBox tilesetList;
	private JFormattedTextField paletteField;
	private JFormattedTextField musicField;

	public static String[][][] menuNames = {
		{ { "File", "f" }, { "Save Changes", "s" }, { "Exit", "q" } },
		{ { "Mode", "m" }, { "Map (Text)", "1" }, { "Map (Graphical)", "2" } },
		{ { "Help", "h" }, {"About", "a" } }
	};

	public static final String[] TILESET_NAMES = {"Underworld", "Onett",
        "Twoson", "Threed", "Fourside", "Magicant", "Outdoors", "Summers",
        "Desert", "Dalaam", "Indoors 1", "Indoors 2", "Stores 1", "Caves 1",
        "Indoors 3", "Stores 2", "Indoors 4", "Winters", "Scaraba", "Caves 2"};

	// public static final int[] MAP_TILESETS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 17, 10, 10, 10, 10, 18, 16, 12, 11, 11, 11, 15, 14, 19, 13, 13, 13, 13, 0 };

	//	private MapEditor app = new MapEditor();
	private EbMap mapcontrol;
	private MapGraphics gfxcontrol;

//	public static MapGraphics gfxcontrol = new MapGraphics();

	public JPanel createTopButtons() {
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
		for (int i = 0; i < map_tsets; i++) {
			tList_names[i] = i + " - " + TILESET_NAMES[mapcontrol.getDrawTileset(i)];
		}
		tilesetList = new JComboBox(tList_names);
		tilesetList.setActionCommand("TtilesetList");
		tilesetList.addActionListener(this);
		panel.add(tilesetList);

		panel.add(new JLabel("Palette: "));
		paletteField = new JFormattedTextField(num_format);
		paletteField.setValue(new Integer(palette));
		paletteField.setColumns(2);
		paletteField.addPropertyChangeListener("value", this);
		panel.add(paletteField);

		panel.add(new JLabel("Music: "));
		musicField = new JFormattedTextField(num_format);
		musicField.setValue(new Integer (music));
		musicField.setColumns(3);
		musicField.addPropertyChangeListener("value", this);
		panel.add(musicField);

		return panel;
	}

	// A neat little way to create a JMenuBar from an array
	public JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// Create the actual menu
		for (int i = 0; i <= menuNames.length - 1; i++) {
			JMenu menu = new JMenu(menuNames[i][0][0]);
			menu.setMnemonic(menuNames[i][0][0].charAt(0));
			for (int i2 = 1; i2 < menuNames[i].length; i2++) {
				// JMenuItem menuItem = new JMenuItem(menuNames[i][i2]);
				JMenuItem menuItem = EbHackModule.createJMenuItem(menuNames[i][i2][0], menuNames[i][i2][1].charAt(0), null, "M" + i + i2, this);
				// menuItem.setActionCommand("M" + i + i2);
				// menuItem.addActionListener(this);
				menu.add(menuItem);
			}
			menuBar.add(menu);
		}

		return menuBar;
	}
	public void createGUI() {
		mainWindow = createBaseWindow(this);
	        mainWindow.setTitle(getDescription());

		// Create and set up the window.
		// mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.setSize(window_width, window_height);

		top_buttons = createTopButtons();

		// mainWindow.getContentPane().add(gfxcontrol);
		mainWindow.getContentPane().add(top_buttons, BorderLayout.NORTH);
		mainWindow.getContentPane().add(gfxcontrol);
		mainWindow.setJMenuBar(createMenuBar());

		// mainWindow.pack();

		gfxcontrol.addMouseListener(this);
	}

	protected void init() {
		mapcontrol = new EbMap(this, width, height, sector_width, sector_height);
		gfxcontrol = new MapGraphics(this, x, y, screen_x, screen_y, screen_width, screen_height, tilewidth, tileheight, sector_width, sector_height, 0);

		// Create the GUI.
		createGUI();

		// Testing stuff.
		int[] test_row = mapcontrol.getTiles(0, 0, 5);
		for (int i = 0; i < test_row.length; i++) {
			System.out.println("Test row tile " + i + ": " + Integer.toHexString(test_row[i]));
		}

		int[][] maparray = new int[screen_height][screen_width];
		for (int i = 0; i < screen_height; i++) {
				maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
		}
		gfxcontrol.setMapArray(maparray);
		gfxcontrol.remoteRepaint();
    	}

	public void propertyChange(PropertyChangeEvent e) {
		System.out.println("Property change!");
		Object source = e.getSource();
		// System.out.println("source: " + e.getSource());
		if (source == xField) {
			int newx = ((Number)xField.getValue()).intValue();
			if ((newx >= 0) && (newx <= width)) {
				x = newx;
				int[][] maparray = new int[screen_height][screen_width];
				for (int i = 0; i < screen_height; i++) {
					maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
				}
			gfxcontrol.setMapArray(maparray);
			gfxcontrol.setx(x);
			gfxcontrol.remoteRepaint();
			} else {
				xField.setValue(new Integer(x));
			}
		} else if (source == yField) {
			int newy = ((Number)yField.getValue()).intValue();
			if ((newy >= 0) && (newy <= height)) {
				y = newy;
				int[][] maparray = new int[screen_height][screen_width];
				for (int i = 0; i < screen_height; i++) {
					maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
				}
			gfxcontrol.setMapArray(maparray);
			gfxcontrol.sety(y);
			gfxcontrol.remoteRepaint();
			} else {
				yField.setValue(new Integer(y));
			}
		}

	}

	public void actionPerformed(ActionEvent e) {
		String name = e.getActionCommand();
		String type = name.substring(0, 1);
		if (type.equals("M")) {
			menuAction(name.substring(1, 2), name.substring(2, 3));
		} else if (type.equals("T")) {
			System.out.println("A top buttons action: " + name.substring(1));
		}
	}

	public void menuAction(String n1, String n2) {
		System.out.println("A menu action: " + n1 + n2);
		if (Integer.parseInt(n1) == 1) {
			gfxcontrol.changeMode(Integer.parseInt(n2) - 1);
			gfxcontrol.remoteRepaint();
		}
	}

	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mouseClicked(MouseEvent e) {
		System.out.println("Mouse clicked! Button: " + e.getButton());
		if (e.getButton() == 3) {
			int mousex = e.getX();
			int mousey = e.getY();
			if ((mousex >= screen_x) && (mousex <= screen_width * tilewidth) && (mousey >= screen_y) && (mousey <= screen_height * tileheight)) {
				int sectorx = getSectorXorY(getTileXorY(mousex - screen_x, tilewidth, screen_width) + x, sector_width);
				int sectory = getSectorXorY(getTileXorY(mousey - screen_y, tileheight, screen_height) + y, sector_height);

				int[] modeprops = gfxcontrol.getModeProps();
				if (modeprops[0] == 2) {
					System.out.println("Sector xy: " + sectorx + "," + sectory);
					int[] tsetpal = mapcontrol.getTsetPal(sectorx, sectory);
					tilesetList.setSelectedIndex(tsetpal[0]);
					paletteField.setValue(new Integer(tsetpal[1]));
				}

				// System.out.println("Tile: " + getTileXorY(mousex - screen_x, tilewidth, screen_width) + "x" + getTileXorY(mousey - screen_y, tileheight, screen_height));
				gfxcontrol.setSector(sectorx, sectory);
				gfxcontrol.remoteRepaint();
			}
		}
	}

	public int getSectorXorY(int tilexory, int sector_woh) {
		for (int i = 0; i <= width; i++) {
			if (tilexory < sector_woh) {
				return i;
			} else {
				tilexory = tilexory - sector_woh;
			}
		}
		return -1;
	}
	public int getTileXorY(int mousexory, int tile_woh, int screen_limit) {
		for (int i = 0; i <= screen_limit; i++) {
			if (mousexory < tile_woh) {
				return i;
			} else {
				mousexory = mousexory - tile_woh;
			}
		}
		return -1;
	}

	public void show() {
		super.show();
		this.reset();
		tilesetList.setSelectedIndex(0);
		tilesetList.updateUI();
		mainWindow.setVisible(true);
		mainWindow.repaint();
	}

	public void hide() {
		mainWindow.setVisible(false);
	}

	public String getDescription() {
		return "Map Editor";
	}

	public String getVersion() {
		return "0.1";
	}

	public String getCredits()
	{
		return "Written by MrTenda\n"
		+ "Original Map Editor written by Mr. Accident\n"
		+ "Tileset names from AnyoneEB\n"
		+ "Tile rendering code from Tile Editor by AnyoneEB";
	}

	// Controls the graphics stuff.
	public class MapGraphics extends AbstractButton {
		private EbHackModule hm;
		private int[][] maparray;
		private boolean maparrayisset = false;
		private int x;
		private int y;
		private int startx;
		private int starty;
		private int width;
		private int height;
		private int tilewidth;
		private int tileheight;
		private int sector_width;
		private int sector_height;
		// modes: 0 = map editing (text), 1 = map editing (graphics)
		private int mode;
		private int sectorx;
		private int sectory;

		private boolean knowsmap = false;
		private boolean knowssector = false;
		// This variable should define what a mode does.
		private int[][] modeprops = new int[][] {
			// { allow sectors to be selectable with right-click (0 = no, 1 = yes, 2 = change sector vars too), draw map (0=no, 1=text tiles, 2=gfx tiles) }
			{ 2, 1 },
			{ 2, 2 }
		};

		public MapGraphics(EbHackModule newhm, int newx, int newy, int newstartx, int newstarty, int newwidth, int newheight, int newtilewidth, int newtileheight, int newsector_width, int newsector_height, int newmode) {
			System.out.println("Now evaluating MapGraphics()");
			// System.out.println("Map array is set?: " + this.knowsmap);
			this.hm = newhm;
			this.x = newx;
			this.y = newy;
			this.startx = newstartx;
			this.starty = newstarty;
			this.width = newwidth;
			this.height = newheight;
			this.tilewidth = newtilewidth;
			this.tileheight = newtileheight;
			this.sector_width = newsector_width;
			this.sector_height = newsector_height;

			this.mode = newmode;

			// TileEditor.readFromRom(this.hm); // load tileset data into "tilesets" array
		}

		public void paintComponent(Graphics g) {
			System.out.println("Now evaluating paintComponent()");
			// System.out.println("Map array is set?: " + this.maparrayisset);
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)g;
			drawBorder(g2d);
			if (this.knowsmap) {
				drawMap(g, g2d);
			}
		}

		protected void drawMap(Graphics g, Graphics2D g2d) {
			Font test_font = new Font("Arial", Font.PLAIN, 12);
			if (this.modeprops[this.mode][1] == 1) {
				g2d.setPaint(Color.black);
				g2d.setFont(test_font);
				for (int i = 0; i < maparray.length; i++) {
					int[] row2draw = maparray[i];
					for (int i2 = 0; i2 < row2draw.length; i2++) {
						// System.out.println("Row " + i + ", Column " + i2 + "; " + maparray[i][i2]);
						g2d.drawString(addZeros(Integer.toHexString(row2draw[i2]), 2), this.startx + (i2 * this.tilewidth) + (this.tilewidth / 2), this.starty + (i * this.tileheight) + (this.tileheight / 2));
						g2d.draw(new Rectangle2D.Double(this.startx + (i2 * this.tilewidth), this.starty + (i * this.tileheight), tilewidth, tileheight));
					}
				}
			} else if (this.modeprops[this.mode][1] == 2) {
				for (int i = 0; i < maparray.length; i++) {
					int[] row2draw = maparray[i];
					for (int i2 = 0; i2 < row2draw.length; i2++) {
						//g2d.drawString(addZeros(Integer.toHexString(row2draw[i2]), 2), this.startx + (i2 * this.tilewidth) + (this.tilewidth / 2), this.starty + (i * this.tileheight) + (this.tileheight / 2));
						int[] tsetPal = mapcontrol.getTsetPal(getSectorXorY(i2, this.sector_width), getSectorXorY(i, this.sector_height));
						g.drawImage(TileEditor.tilesets[mapcontrol.getDrawTileset(tsetPal[0])].getArrangementImage(row2draw[i2], TileEditor.tilesets[mapcontrol.getDrawTileset(tsetPal[0])].getPaletteNum(tsetPal[0], tsetPal[1]), false), this.startx + (i2 * this.tilewidth), this.starty + (i * this.tileheight), this.tilewidth, this.tileheight, this);
						
						g2d.draw(new Rectangle2D.Double(this.startx + (i2 * this.tilewidth), this.starty + (i * this.tileheight), tilewidth, tileheight));
					}
				}

			}

			if ((this.modeprops[this.mode][0] >= 1) && knowssector) {
				g2d.setPaint(Color.yellow);

				/*int draw_sector_x, draw_sector_y, draw_sector_w, draw_sector_h;

				if (this.sectorx == 0) {
					draw_sector_x = this.startx;
					draw_sector_w = (this.sector_width - this.x) * this.tilewidth;
				} else {
					draw_sector_x = this.startx + (this.sectorx * this.tilewidth * this.sector_width) - (this.tilewidth * this.x);
					if ((draw_sector_x + (this.sector_width * this.tilewidth)) > (this.width * this.tilewidth)) {
						System.out.println("The sector being drawn DOES extend horizontally past the border.");
						System.out.println("Expected end of sector: " + (draw_sector_x + (this.sector_width * this.tilewidth)) + "; Width of screen: " + (this.width * this.tilewidth) + ";");
						draw_sector_w = (this.sector_width * this.tilewidth) - ((draw_sector_x + (this.sector_width * this.tilewidth)) - (this.width * this.tilewidth));
						System.out.println("New width of sector: " + draw_sector_w + ";");
					} else {
						System.out.println("The sector being drawn DOES NOT extend horizontally past the border.");
						draw_sector_w = this.sector_width * this.tilewidth;
					}
				}*/

				/*g2d.draw(new Rectangle2D.Double(this.startx + (this.sectorx * this.tilewidth * this.sector_width), this.starty + (this.sectory * this.tileheight * this.sector_height), this.tilewidth * sectorDisplayWoH(this.sectorx, this.sector_width, this.width), this.tileheight * sectorDisplayWoH(this.sectory, this.sector_height, this.height)));*/

				int[] draw_sector_xw = sectorDisplayWoH(this.sectorx, this.sector_width, this.width, this.startx, this.x, this.tilewidth);
				int[] draw_sector_yh = sectorDisplayWoH(this.sectory, this.sector_height, this.height, this.starty, this.y, this.tileheight);

				g2d.draw(new Rectangle2D.Double(draw_sector_xw[0], draw_sector_yh[0], draw_sector_xw[1], draw_sector_yh[1]));
			}

		}

		protected int[] sectorDisplayWoH(int sectorxory, int sector_woh, int woh, int startxory, int xory, int tilewoh) {
			/*if (((sectorxory + 1) * sector_woh) > woh) {
				System.out.println("sectorDisplayWoH case 1: new woh: " + (sector_woh - (((sectorxory + 1) * sector_woh) - woh)));
				return sector_woh - (((sectorxory + 1) * sector_woh) - woh);
			} else {
				System.out.println("sectorDisplayWoH case 2: new woh: " + sector_woh);
				return sector_woh;
			}*/

			int draw_sector_xoy, draw_sector_woh;

			if (sectorxory == 0) {
				draw_sector_xoy = startxory;
				draw_sector_woh = (sector_woh - xory) * tilewoh;
			} else {
				draw_sector_xoy = startxory + (sectorxory * tilewoh * sector_woh) - (tilewoh * xory);
				if ((draw_sector_xoy + (sector_woh * tilewoh)) > (woh * tilewoh)) {
					System.out.println("The sector being drawn DOES extend horizontally past the border.");
					System.out.println("Expected end of sector: " + (draw_sector_xoy + (sector_woh * tilewoh)) + "; Width of screen: " + (woh * tilewoh) + ";");
					draw_sector_woh = (sector_woh * tilewoh) - ((draw_sector_xoy + (sector_woh * tilewoh)) - (woh * tilewoh));
					System.out.println("New width of sector: " + draw_sector_woh + ";");
				} else {
					System.out.println("The sector being drawn DOES NOT extend horizontally past the border.");
					draw_sector_woh = sector_woh * tilewoh;
				}
			}

			return new int[] { draw_sector_xoy, draw_sector_woh };
		}

		protected void drawBorder(Graphics2D g2d) {
			g2d.setPaint(Color.black);
			Rectangle2D.Double border = new Rectangle2D.Double(this.startx - 1, this.starty - 1, (this.width * this.tilewidth) + 1, (this.height * this.tileheight) + 1);
			g2d.draw(border);
		}

		public void setSector(int newsectorx, int newsectory) {
			System.out.println("New Sector: " + newsectorx + "x" + newsectory);
			if (this.knowssector && (this.sectorx == newsectorx) && (this.sectory == newsectory)) {
				this.knowssector = false;
			} else {
				if (! this.knowssector) {
					this.knowssector = true;
				}
				this.sectorx = newsectorx;
				this.sectory = newsectory;
			}
		}

		public void changeMode(int newmode) {
			this.mode = newmode;
		}

		public void setMapArray(int[][] newmaparray) {
			if (! this.knowsmap) {
				this.knowsmap = true;
			}
			this.maparray = newmaparray;
		}

		public void setx(int newx) {
			this.x = newx;
		}

		public void sety(int newy) {
			this.y = newy;
		}

		public void remoteRepaint() {
			repaint();
		}

		public String[] getModeNames() {
			return new String[] { "Map Viewing (text), Map Viewing (graphical)" };
		}

		public int getModeMax() {
			return this.modeprops.length;
		}

		public int getMode() {
			return this.mode;
		}

		public int[] getModeProps() {
			return this.modeprops[this.mode];
		}
	}

	// Represents the whole EarthBound map and map-related data in the rom.
	public static class EbMap {
		private HackModule hm;
		private int width;
		private int height;
		private int sector_width;
		private int sector_height;
		private int[] all_addresses;
		private int tsetpal_address;
		private int tsettbl_address;
		private int address;
		private int tile;
		private Rom rom;

		public EbMap(HackModule hm, int newwidth, int newheight, int newsector_width, int newsector_height) {
			this.hm = hm;
			this.rom = hm.rom;
			this.width = newwidth;
			this.height = newheight;
			this.sector_width = newsector_width;
			this.sector_height = newsector_height;
			this.all_addresses = new int[] { 0x160200, 0x162A00, 0x165200, 0x168200, 0x16AA00, 0x16D200, 0x170200, 0x172A00 };
			this.tsetpal_address = 0x17AA00;
			this.tsettbl_address = 0x2F121B;
		}

		public int[] getTiles(int row, int start, int length) {
			this.address = this.all_addresses[row % 8] + start;
			// this.rom.seek(this.address);

			int[] output = new int[length];
			for (int i = 0; i < output.length; i++) {
				int read_byte = rom.read(address + i);
				if (read_byte == -1) {
					System.out.println("Error in getTiles at address " + address);
				} else {
					output[i] = read_byte;
				}
			}
			return output;
		}

		public int[] getTsetPal(int sectorx, int sectory) {
			this.address = this.tsetpal_address + (sectory * ((width + 1) / sector_width)) + sectorx;
			String tsetpal_data = Integer.toBinaryString(rom.read(this.address));
			System.out.println("tstpal_data:" + tsetpal_data + " Address: " + Integer.toHexString(this.address));
			int tileset = Integer.parseInt(tsetpal_data.substring(0, 5), 2);
			int palette = Integer.parseInt(tsetpal_data.substring(6, 8), 2);
			System.out.println("tsetpal_data: " + tsetpal_data + " tileset: " + tileset + " palette: " + palette);
			int[] tsetpal = new int[] { tileset, palette };
			return tsetpal;
		}

		public int getDrawTileset(int mapTset) {
			if ((mapTset > map_tsets) || (mapTset < 0)) { return -1; }
			this.address = this.tsettbl_address + (mapTset * 2);
			System.out.println(Integer.toHexString(this.address));
			int drawTset = rom.read(this.address);
			return drawTset;
			//return 0;
		}
		/*public Image getTileImage(int tset, int tile, int pallete) {
			// TileEditor.Tileset tile_data = new TileEditor.Tileset(MAP_TILESETS[tset], TILESET_NAMES[MAP_TILESETS[tset]], this.hm);
			TileEditor.Tileset tset_class = TileEditor.tilesets[MAP_TILESETS[tset]];
			return tset_class.getArrangementImage(tile, pallete, false);
		}*/

	}
}
