package net.starmen.pkhack.eb;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.Math;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
		MapGraphicsListener {
	public MapEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}

	public static int sectorWidth = 8;

	public static int sectorHeight = 4;

	public static int widthInSectors = 32;

	public static int heightInSectors = 80;

	public static int width = (widthInSectors * sectorWidth) - 1;

	public static int height = (heightInSectors * sectorHeight) - 1;

	public static int tileWidth = 32;

	public static int tileHeight = 32;

	public static final int drawTsetNum = 20;

	public static final int mapTsetNum = 32;

	public static final int palsNum = 59;

	private static boolean hasLoaded = false;

	private JMenu modeMenu;

	private JMenuItem sectorProps, findSprite, copySector, pasteSector;

	private boolean oldCompatability = false;

	private int[][][] copiedSectorTiles = new int[sectorHeight][sectorWidth][2];

	private EbMap.Sector copiedSector;

	private JFrame errorWindow;

	private JLabel errorTitle;

	private JTextArea errorText;

	private JButton errorClose;

	private MapGraphics gfxcontrol;

	public JPanel createTopButtons() {
		JPanel panel = new JPanel(new FlowLayout());

		panel.add(new JLabel("X: "));
		panel.add(gfxcontrol.getXField());

		panel.add(new JLabel("Y: "));
		panel.add(gfxcontrol.getYField());

		JLabel tilesetLabel = new JLabel(
				"<html><font color = \"blue\"><u>Tileset</u></font>: </html>");
		tilesetLabel.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if ((e.getButton() == 1)
						&& (gfxcontrol.getTilesetList().getSelectedIndex() > 0)
						&& (gfxcontrol.getPaletteBox().getSelectedIndex() > 0)) {
					int tileset = EbMap.getDrawTileset(gfxcontrol
							.getTilesetList().getSelectedIndex()), palette = TileEditor.tilesets[tileset]
							.getPaletteNum(gfxcontrol.getTilesetList()
									.getSelectedIndex(), gfxcontrol
									.getPaletteBox().getSelectedIndex());
					net.starmen.pkhack.JHack.main.showModule(TileEditor.class,
							new int[] { tileset, palette, 0 });
				} else if ((e.getButton() == 3)
						&& (gfxcontrol.getTilesetList().getSelectedIndex() > 0))
					net.starmen.pkhack.JHack.main.showModule(
							MapEventEditor.class, new Integer(EbMap
									.getDrawTileset(gfxcontrol.getTilesetList()
											.getSelectedIndex())));
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		tilesetLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		panel.add(tilesetLabel);
		panel.add(gfxcontrol.getTilesetList());

		JLabel paletteLabel = new JLabel(
				"<html><font color = \"blue\"><u>Palette</u></font>: </html>");
		paletteLabel.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if ((gfxcontrol.getTilesetList().getSelectedIndex() > 0)
						&& (gfxcontrol.getPaletteBox().getSelectedIndex() > 0)) {
					int tileset = EbMap.getDrawTileset(gfxcontrol
							.getTilesetList().getSelectedIndex()), palette = TileEditor.tilesets[tileset]
							.getPaletteNum(tileset, gfxcontrol.getPaletteBox()
									.getSelectedIndex());
					net.starmen.pkhack.JHack.main.showModule(TileEditor.class,
							new int[] { tileset, palette, 0 });
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		paletteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		panel.add(paletteLabel);
		panel.add(gfxcontrol.getPaletteBox());

		JLabel musicLabel = new JLabel(
				"<html><font color = \"blue\"><u>Music</u></font>: </html>");
		musicLabel.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if (gfxcontrol.getMusicBox().getSelectedIndex() > 0)
					net.starmen.pkhack.JHack.main.showModule(
							EventMusicEditor.class, new Integer(gfxcontrol
									.getMusicBox().getSelectedIndex()));
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		musicLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		panel.add(musicLabel);
		panel.add(gfxcontrol.getMusicBox());

		return panel;
	}

	public JMenuBar createMenuBar() {
		MenuListener menuListener = new MenuListener();
		JMenuBar menuBar = new JMenuBar();
		ButtonGroup group = new ButtonGroup();
		JCheckBoxMenuItem checkBox;
		JRadioButtonMenuItem radioButton;
		JMenu menu;

		menu = new JMenu("File");
		menu.add(EbHackModule.createJMenuItem("Save Changes", 's', null,
				MenuListener.SAVE, menuListener));
		menu.add(EbHackModule.createJMenuItem("Exit", 'x', null,
				MenuListener.EXIT, menuListener));
		menuBar.add(menu);

		menu = new JMenu("Edit");
		copySector = EbHackModule.createJMenuItem("Copy Sector", 'c', null,
				MenuListener.COPY_SECTOR, menuListener);
		menu.add(copySector);
		pasteSector = EbHackModule.createJMenuItem("Paste Sector", 'p', null,
				MenuListener.PASTE_SECTOR, menuListener);
		menu.add(pasteSector);
		sectorProps = EbHackModule.createJMenuItem("Edit Sector's Properties",
				'r', null, MenuListener.SECTOR_PROPS, menuListener);
		menu.add(sectorProps);
		menuBar.add(menu);

		modeMenu = new JMenu("Mode");
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
		radioButton = new JRadioButtonMenuItem("Hotspot Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE6);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Enemy Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand(MenuListener.MODE7);
		radioButton.addActionListener(menuListener);
		group.add(radioButton);
		modeMenu.add(radioButton);
		menuBar.add(modeMenu);

		menu = new JMenu("Tools");
		findSprite = EbHackModule.createJMenuItem("Find Sprite Entry", 'f',
				null, MenuListener.FIND_SPRITE, menuListener);
		menu.add(findSprite);
		menu.add(EbHackModule.createJMenuItem("Delete All Sprites", 'd', null,
				MenuListener.DEL_ALL_SPRITES, menuListener));
		menu.add(EbHackModule.createJMenuItem("Clear Tile Image Cache", 't',
				null, MenuListener.RESET_TILE_IMAGES, menuListener));
		menu.add(EbHackModule.createJMenuItem("Clear Sprite Image Cache", 's',
				null, MenuListener.RESET_SPRITE_IMAGES, menuListener));
		menu.add(EbHackModule.createJMenuItem("Reload Music Names", 'm', null,
				MenuListener.MUSIC_NAMES, menuListener));
		menuBar.add(menu);

		menu = new JMenu("Options");
		checkBox = new JCheckBoxMenuItem("Windows Map Editor 4.x Compatibility");
		checkBox.setMnemonic('4');
		checkBox.setSelected(oldCompatability);
		checkBox.setActionCommand(MenuListener.COMPATABILITY);
		checkBox.addActionListener(menuListener);
		// menu.add(checkBox); // This feature doesn't work, and NOBODY KNOWS
		// WHY :O!!
		checkBox = new JCheckBoxMenuItem("Show Grid");
		checkBox.setMnemonic('g');
		checkBox.setSelected(true);
		checkBox.setActionCommand(MenuListener.GRIDLINES);
		checkBox.addActionListener(menuListener);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Sprite Boxes");
		checkBox.setMnemonic('b');
		checkBox.setSelected(true);
		checkBox.setActionCommand(MenuListener.SPRITEBOXES);
		checkBox.addActionListener(menuListener);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Enemy Sprites");
		checkBox.setMnemonic('e');
		checkBox.setSelected(true);
		checkBox.setActionCommand(MenuListener.ENEMY_SPRITES);
		checkBox.addActionListener(menuListener);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Enemy Colors");
		checkBox.setMnemonic('l');
		checkBox.setSelected(true);
		checkBox.setActionCommand(MenuListener.ENEMY_COLORS);
		checkBox.addActionListener(menuListener);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Map Changes");
		checkBox.setMnemonic('c');
		checkBox.setSelected(false);
		checkBox.setActionCommand(MenuListener.MAPCHANGES);
		checkBox.addActionListener(menuListener);
		menu.add(checkBox);
		/*
		 * checkBox = new JCheckBoxMenuItem("Use Event Palette");
		 * checkBox.setMnemonic('v'); checkBox.setSelected(false);
		 * checkBox.setActionCommand(MenuListener.EVENTPAL);
		 * checkBox.addActionListener(menuListener); menu.add(checkBox);
		 */
		menuBar.add(menu);

		menu = new JMenu("Debug");
		menu.add(EbHackModule.createJMenuItem("View Sprite Errors", 's', null,
				MenuListener.SPRITE_ERR, menuListener));
		menu.add(EbHackModule.createJMenuItem("View Door Errors", 'd', null,
				MenuListener.DOOR_ERR, menuListener));
		//menu.add(EbHackModule.createJMenuItem("Change Sprite Write Address", 'p', null,
		//		MenuListener.SPR_WRITE_ADDR, menuListener));
		//menu.add(EbHackModule.createJMenuItem("Change Door Write Address", 'o', null,
		//		MenuListener.DOOR_WRITE_ADDR, menuListener));
		menuBar.add(menu);

		return menuBar;
	}

	public void createGUI() {
		mainWindow = HackModule.createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		mainWindow.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				boolean effected = false;
				int xDiff = ((int) gfxcontrol.getSize().getWidth())
						- (MapEditor.tileWidth * gfxcontrol.getScreenWidth())
						- 1;
				/*
				 * int oldScreenWidth = screenWidth; int oldScreenHeight =
				 * screenHeight; Dimension oldFrameSize = mainWindow.getSize();
				 */

				JScrollBar scrollh = gfxcontrol.getXScrollBar(), scrollh2 = gfxcontrol
						.getTileChooser().getScrollBar(), scrollv = gfxcontrol
						.getYScrollBar();

				if ((MapEditor.tileWidth <= xDiff)
						|| (0 - MapEditor.tileWidth >= xDiff)) {
					gfxcontrol.setScreenWidth(((int) gfxcontrol.getSize()
							.getWidth())
							/ MapEditor.tileHeight);
					gfxcontrol.getTileChooser().setWidthTiles(
							gfxcontrol.getScreenWidth() - 1);
					if (scrollh.getValue() + gfxcontrol.getScreenWidth() > MapEditor.width + 1)
						scrollh.setValue(MapEditor.width
								- gfxcontrol.getScreenWidth() + 1);
					if (scrollh2.getValue()
							+ gfxcontrol.getTileChooser().getWidthTiles() > MapEditor.width + 1)
						scrollh2.setValue(MapEditor.width
								- gfxcontrol.getScreenWidth() + 1);
					scrollh.setVisibleAmount(gfxcontrol.getScreenWidth());
					scrollh2.setVisibleAmount(gfxcontrol.getScreenWidth());
					gfxcontrol.getTileChooser().repaint();
					effected = true;
				}

				int yDiff = ((int) gfxcontrol.getSize().getHeight())
						- (MapEditor.tileHeight * gfxcontrol.getScreenHeight())
						- 1;
				if ((MapEditor.tileHeight <= yDiff)
						|| (0 - MapEditor.tileHeight >= yDiff)) {
					gfxcontrol.setScreenHeight(((int) gfxcontrol.getSize()
							.getHeight())
							/ MapEditor.tileHeight);
					if (scrollv.getValue() + gfxcontrol.getScreenHeight() > MapEditor.height)
						scrollv.setValue(MapEditor.height
								- gfxcontrol.getScreenHeight() + 1);
					scrollv.setVisibleAmount(gfxcontrol.getScreenHeight());
					effected = true;
				}

				if (effected) {
					gfxcontrol
							.setPreferredSize(new Dimension(
									(MapEditor.tileWidth * gfxcontrol
											.getScreenWidth()) + 1,
									(MapEditor.tileHeight * gfxcontrol
											.getScreenHeight()) + 1));
					/*
					 * mainWindow.setSize( new Dimension( (int)
					 * (oldFrameSize.getWidth() - ((screenWidth -
					 * oldScreenWidth) MapEditor.tileWidth)), (int)
					 * (oldFrameSize.getHeight() - ((screenHeight -
					 * oldScreenHeight)) MapEditor.tileHeight)));
					 */
					gfxcontrol.reloadMap();
					gfxcontrol.remoteRepaint();

					// This slows it down so much!
					// mainWindow.pack();
				}
			}
		});

		JPanel top_buttons = createTopButtons();

		JPanel mapgfxpanel = new JPanel(new BorderLayout());
		mapgfxpanel.add(gfxcontrol, BorderLayout.CENTER);
		mapgfxpanel.add(gfxcontrol.getYScrollBar(), BorderLayout.LINE_END);
		mapgfxpanel.add(gfxcontrol.getXScrollBar(), BorderLayout.PAGE_END);

		JPanel editpanel = new JPanel(new BorderLayout());
		editpanel.add(gfxcontrol.getTileChooser().getScrollBar(),
				BorderLayout.PAGE_END);
		editpanel.add(gfxcontrol.getTileChooser(), BorderLayout.CENTER);

		JPanel contentPanel = new JPanel(new BorderLayout());
		mainWindow.setJMenuBar(createMenuBar());
		contentPanel.add(top_buttons, BorderLayout.PAGE_START);
		contentPanel.add(mapgfxpanel, BorderLayout.CENTER);
		contentPanel.add(editpanel, BorderLayout.PAGE_END);
		mainWindow.getContentPane().add(contentPanel, BorderLayout.CENTER);

		mainWindow.pack();

		errorWindow = new JFrame();
		errorTitle = new JLabel();
		errorText = new JTextArea(20, 60);
		errorText.setEditable(false);
		errorClose = new JButton("Close");
		errorClose.addActionListener(this);
		errorWindow.getContentPane().add(errorTitle, BorderLayout.PAGE_START);
		errorWindow.getContentPane().add(new JScrollPane(errorText),
				BorderLayout.CENTER);
		errorWindow.getContentPane().add(errorClose, BorderLayout.PAGE_END);
	}

	protected void init() {
		gfxcontrol = new MapGraphics(this, 24, 12, true, true, true, true, this);

		createGUI();

		gfxcontrol.changeMode(0);
		gfxcontrol.setSector(-1, -1);
	}

	private void readFromRom() {
		readFromRom(this);
		gfxcontrol.loadMusicNames();
		gfxcontrol.loadTilesetNames();
	}

	public static void readFromRom(HackModule hm) {
		if (!hasLoaded) {
			EbMap.loadData(hm, true, true, true, true, true);
			//EbMap.loadData(hm, true, true, true, false, true);
			EventMusicEditor.readFromRom(hm.rom);
			hasLoaded = true;
		}
	}

	public void writeToRom() {
		if (rom.length() == AbstractRom.EB_ROM_SIZE_REGULAR) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"You need to expand your ROM to save in the Map Editor.\n"
							+ "Do you want to?", "This ROM is not expanded",
					JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				this.askExpandType();
				writeToRom();
			} else
				JOptionPane.showMessageDialog(mainWindow,
						"Changes were not saved.");
		} else {
			EbMap.writeMapChanges(rom);
			EbMap.writeLocalTilesetChanges(rom);
			EbMap.writeSectorData(rom);
			EbMap.writeEnemyLocs(rom);
			HotspotEditor.writeToRom(this);
			boolean doorWrite = EbMap.writeDoors(this, oldCompatability);
			boolean spWrite = EbMap.writeSprites(this);
			if (!doorWrite)
				JOptionPane.showMessageDialog(mainWindow,
						"This is so embarassing!\n"
								+ "For some reason, I could not save "
								+ "the door data?\nThis shouldn't happen...");
			if (!spWrite)
				JOptionPane.showMessageDialog(mainWindow,
						"This is so embarassing!\n"
								+ "For some reason, I could not save "
								+ "the sprite data?\nThis shouldn't happen...");
			if (doorWrite && spWrite)
				JOptionPane
						.showMessageDialog(mainWindow, "Saved successfully!");
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("apply")) {
			writeToRom();
		} else if (e.getActionCommand().equals("close")) {
			hide();
		} else if (e.getSource().equals(errorClose))
			errorWindow.hide();
	}

	public class MenuListener implements ActionListener {
		public static final String SAVE = "save", 
			EXIT = "exit",
			MODE0 = "mode0",
			MODE1 = "mode1",
			MODE2 = "mode2",
			MODE6 = "mode6",
			MODE7 = "mode7",
			DEL_ALL_SPRITES = "delAllSprites",
			RESET_TILE_IMAGES = "resetTileImages",
			RESET_SPRITE_IMAGES = "resetSpriteImages",
			GRIDLINES = "gridLines",
			SPRITEBOXES = "spriteBoxes",
			COMPATABILITY = "4xCompatability",
			MAPCHANGES = "mapChanges",
			MUSIC_NAMES = "musicNames",
			SECTOR_PROPS = "sectorProps",
			FIND_SPRITE = "findSprite",
			DOOR_ERR = "doorErrors",
			SPRITE_ERR = "spriteErrors",
			COPY_SECTOR = "copySector",
			PASTE_SECTOR = "pasteSector",
			ENEMY_SPRITES = "enemySprites",
			ENEMY_COLORS = "enemyColors",
			EVENTPAL = "eventPal",
			SPR_WRITE_ADDR = "sprWriteAddr",
			DOOR_WRITE_ADDR = "doorWriteAddr";

		public void actionPerformed(ActionEvent e) {
			String ac = e.getActionCommand();
			if (ac.equals(SAVE)) {
				writeToRom();
			} else if (ac.equals(EXIT)) {
				hide();
			} else if (ac.equals(MODE0)) {
				gfxcontrol.changeMode(0);
				gfxcontrol.remoteRepaint();
				gfxcontrol.getTileChooser().repaint();
			} else if (ac.equals(MODE1)) {
				gfxcontrol.changeMode(1);
				gfxcontrol.remoteRepaint();
				gfxcontrol.getTileChooser().repaint();
			} else if (ac.equals(MODE2)) {
				gfxcontrol.changeMode(2);
				gfxcontrol.remoteRepaint();
				gfxcontrol.getTileChooser().repaint();
			} else if (ac.equals(MODE6)) {
				gfxcontrol.changeMode(6);
				gfxcontrol.remoteRepaint();
				gfxcontrol.getTileChooser().repaint();
			} else if (ac.equals(MODE7)) {
				gfxcontrol.changeMode(7);
				gfxcontrol.updateComponents();
				gfxcontrol.remoteRepaint();
				gfxcontrol.getTileChooser().repaint();
			} else if (ac.equals(DEL_ALL_SPRITES)) {
				int sure = JOptionPane.showConfirmDialog(mainWindow,
						"Are you sure you want to "
								+ "delete all of the sprites?",
						"Are you sure?", JOptionPane.YES_NO_OPTION);
				if (sure == JOptionPane.YES_OPTION) {
					EbMap.nullSpriteData();
					gfxcontrol.remoteRepaint();
				}
			} else if (ac.equals(RESET_TILE_IMAGES)) {
				EbMap.resetTileImages();
				if (gfxcontrol.getModeProps()[1] >= 2) {
					gfxcontrol.remoteRepaint();
					gfxcontrol.getTileChooser().repaint();
				}
			} else if (ac.equals(RESET_SPRITE_IMAGES)) {
				EbMap.resetSpriteImages();
				gfxcontrol.remoteRepaint();
			} else if (ac.equals(GRIDLINES)) {
				gfxcontrol.toggleGrid();
				gfxcontrol.remoteRepaint();
			} else if (ac.equals(SPRITEBOXES)) {
				gfxcontrol.toggleSpriteBoxes();
				gfxcontrol.remoteRepaint();
			} else if (ac.equals(COMPATABILITY)) {
				oldCompatability = !oldCompatability;
			} else if (ac.equals(MAPCHANGES)) {
				gfxcontrol.toggleMapChanges();
				gfxcontrol.remoteRepaint();
			} else if (ac.equals(MUSIC_NAMES))
				gfxcontrol.reloadMusicNames();
			else if (ac.equals(SECTOR_PROPS))
				net.starmen.pkhack.JHack.main.showModule(
						MapSectorPropertiesEditor.class, gfxcontrol
								.getSectorxy());
			else if (ac.equals(FIND_SPRITE)) {
				String tpt = JOptionPane.showInputDialog(mainWindow,
						"Enter TPT entry to search for.", Integer
								.toHexString(0));
				if (tpt != null) {
					int tptNum = Integer.parseInt(tpt, 16);
					for (int i = 0; i < (MapEditor.heightInSectors / 2)
							* MapEditor.widthInSectors; i++) {
						ArrayList sprites = EbMap.getSpritesData(i);
						EbMap.SpriteLocation spLoc;
						for (int j = 0; j < sprites.size(); j++) {
							spLoc = (EbMap.SpriteLocation) sprites.get(j);
							if (spLoc.getTpt() == tptNum) {
								int areaY = i / MapEditor.widthInSectors, areaX = i
										- (areaY * MapEditor.widthInSectors);
								gfxcontrol
										.setMapXY(
												(areaX * MapEditor.sectorWidth)
														+ (spLoc.getX() / MapEditor.tileWidth),
												(areaY * MapEditor.sectorHeight * 2)
														+ (spLoc.getY() / MapEditor.tileHeight));
								gfxcontrol.reloadMap();
								gfxcontrol.updateComponents();
								gfxcontrol.repaint();
								int yesno = JOptionPane
										.showConfirmDialog(
												mainWindow,
												"I found a sprite with that TPT entry. Do you want to find another?",
												"Continue Search?",
												JOptionPane.YES_NO_OPTION);
								if (yesno == JOptionPane.NO_OPTION)
									return;
							}
						}
					}
					JOptionPane.showMessageDialog(mainWindow,
							"Could not find a sprite entry using TPT entry 0x"
									+ tpt + ".");
				}
			} else if (ac.equals(DOOR_ERR)) {
				ArrayList errors = EbMap.getErrors();
				if (errors.size() == 0)
					JOptionPane.showMessageDialog(mainWindow,
							"No errors encountered!");
				else {
					int num = 0;
					String out = "";
					for (int i = 0; i < errors.size(); i++) {
						EbMap.ErrorRecord er = (EbMap.ErrorRecord) errors
								.get(i);
						if (er.getType() == EbMap.ErrorRecord.DOOR_ERROR) {
							out = out + "#" + (num + 1) + ": "
									+ er.getMessage()
									+ System.getProperty("line.separator");
							num++;
						}
					}

					if (num == 0)
						JOptionPane.showMessageDialog(mainWindow,
								"No door errors encountered!");
					else {
						errorWindow.setTitle("Door Errors - Map Editor");
						errorTitle
								.setText(errors.size()
										+ " errors were encountered while reading door entry data.");
						errorText.setText(out);
						errorWindow.pack();
						errorWindow.show();
					}
				}
			} else if (ac.equals(SPRITE_ERR)) {
				ArrayList errors = EbMap.getErrors();
				if (errors.size() == 0)
					JOptionPane.showMessageDialog(mainWindow,
							"No errors encountered!");
				else {
					int num = 0;
					String out = "";
					for (int i = 0; i < errors.size(); i++) {
						EbMap.ErrorRecord er = (EbMap.ErrorRecord) errors
								.get(i);
						if (er.getType() == EbMap.ErrorRecord.SPRITE_ERROR) {
							out = out + "#" + (num + 1) + ": "
									+ er.getMessage()
									+ System.getProperty("line.separator");
							num++;
						}
					}

					if (num == 0)
						JOptionPane.showMessageDialog(mainWindow,
								"No sprite errors encountered!");
					else {
						errorWindow.setTitle("Sprite Errors - Map Editor");
						errorTitle
								.setText(errors.size()
										+ " errors were encountered while reading sprite entry data.");
						errorText.setText(out);
						errorWindow.pack();
						errorWindow.show();
					}
				}
			} else if (ac.equals(COPY_SECTOR)) {
				pasteSector.setEnabled(true);

				int[] sectorXY = gfxcontrol.getSectorxy();
				for (int i = 0; i < copiedSectorTiles.length; i++)
					for (int j = 0; j < copiedSectorTiles[i].length; j++) {
						copiedSectorTiles[i][j][0] = EbMap.getTile(rom, j
								+ sectorXY[0] * sectorWidth, i + sectorXY[1]
								* sectorHeight);
						copiedSectorTiles[i][j][1] = EbMap.getLocalTileset(rom,
								j + sectorXY[0] * sectorWidth, i + sectorXY[1]
										* sectorHeight);
					}
				copiedSector = EbMap.getSectorData(sectorXY[0], sectorXY[1]);
			} else if (ac.equals(PASTE_SECTOR)) {
				int[] sectorXY = gfxcontrol.getSectorxy();
				for (int i = 0; i < copiedSectorTiles.length; i++)
					for (int j = 0; j < copiedSectorTiles[i].length; j++) {
						EbMap.changeTile(sectorXY[0] * sectorWidth + j,
								sectorXY[1] * sectorHeight + i,
								(byte) (copiedSectorTiles[i][j][0] & 0xff));
						EbMap.setLocalTileset(rom, sectorXY[0] * sectorWidth
								+ j, sectorXY[1] * sectorHeight + i,
								copiedSectorTiles[i][j][1]);
					}
				EbMap.setSectorData(sectorXY[0], sectorXY[1], copiedSector);
				gfxcontrol.reloadMap();
				gfxcontrol.repaint();
				gfxcontrol.updateComponents();
			} else if (ac.equals(ENEMY_SPRITES)) {
				gfxcontrol.toggleEnemySprites();
				gfxcontrol.repaint();
			} else if (ac.equals(ENEMY_COLORS)) {
				gfxcontrol.toggleEnemyColors();
				gfxcontrol.repaint();
			} else if (ac.equals(EVENTPAL)) {
				gfxcontrol.toggleEventPalette();
				gfxcontrol.repaint();
			}
		}
	}

	public void show() {
		show(true);
	}

	public void show(boolean userShown) {
		super.show();
		readFromRom();
		mainWindow.setVisible(true);
		mainWindow.repaint();

		gfxcontrol.reloadMap();
		gfxcontrol.remoteRepaint();

		readFromRom();
	}

	public void show(Object obj) {
		show(false);
		if (obj instanceof SeekListener) {
			if (gfxcontrol.getMode() == 4)
				JOptionPane.showMessageDialog(mainWindow,
						"Sorry, already seeking something else for the "
								+ gfxcontrol.getSeekSource().getDescription()
								+ ".");
			else {
				SeekListener seekSource = (SeekListener) obj;
				gfxcontrol.setSeekSource(seekSource);
				gfxcontrol.changeMode(4);
				gfxcontrol.remoteRepaint();
			}
		} else if (obj instanceof Integer[]) {
			Integer[] coords = (Integer[]) obj;
			gfxcontrol.setMapXY(coords[0].intValue(), coords[1].intValue());
			gfxcontrol.reloadMap();
			gfxcontrol.updateComponents();
			gfxcontrol.remoteRepaint();
		}
	}

	public void hide() {
		mainWindow.setVisible(false);
		errorWindow.hide();
	}

	public void reset() {
		EbMap.reset();
		hasLoaded = false;

		gfxcontrol.updateComponents();
	}

	public String getDescription() {
		return "Map Editor";
	}

	public String getVersion() {
		return "0.4.4";
	}

	public String getCredits() {
		return "Written by Mr. Tenda\n"
				+ "Original Map Editor written by Mr. Accident\n";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.eb.ModeChangeListener#changedMode(int, int)
	 */
	public void changedMode(int newMode, int oldMode) {
		findSprite.setEnabled(gfxcontrol.getModeProps()[3] >= 1);
		modeMenu.setEnabled(gfxcontrol.getModeProps()[6] < 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.eb.MapGraphicsListener#changedSector(boolean,
	 *      int, int)
	 */
	public void changedSector(boolean knowsSector, int sectorX, int sectorY) {
		sectorProps.setEnabled(knowsSector);
		copySector.setEnabled(knowsSector);
		pasteSector.setEnabled(knowsSector && (copiedSector != null));
	}

	// Controls the graphics stuff.
	public static class MapGraphics extends AbstractButton implements
			DocumentListener, ActionListener, MouseListener,
			MouseMotionListener, MouseWheelListener, AdjustmentListener {
		private HackModule hm;

		private int[][] maparray;

		private int x = 0, y = 0, ppu = MapEditor.tileWidth, mode, sectorx,
				sectory, crossX, crossY, oldMode, screenHeight, screenWidth,
				previewBoxX, previewBoxY;

		private JTextField xField, yField;

		private JComboBox tilesetList, paletteBox, musicBox;

		private JScrollBar xScroll, yScroll;

		private boolean grid, spriteBoxes, centered, mapChanges = false,
				enabled = true, enemySprites, enemyColors,
				eventPalette = false;

		private SeekListener seekSource;

		private MapGraphicsListener mgl;

		private TileChooser editBox;

		private ArrayList visibleHotspots;

		private static final Color noEnemyColor = new Color(0x444444);

		/*
		 * Moving data types (first int): 0 - moving types:
		 * -1 = no, 0 = sprite, 1 = door, 2 = hotspot, 3 = resize hotspot
		 */
		private int[] movingData = new int[] { -1, 0, 0, 0 };
		private Object movingObject;

		private boolean knowsmap = false, knowssector = false;

		PopupListener popupListener;

		// This variable should define what a mode does.
		private final int[][] modeprops = new int[][] {
		/*
		 * Mode Properties:
		 * 
		 * 0 - right-click selects sectors (0=no, 1=yes, 2=yes & change sector
		 * vars)
		 * 1 - draw map (0=no, 1=yes, 2=gfx)
		 * 2 - map editing
		 * 3 - sprites (0=no, 1=view, 2=edit)
		 * 4 - doors (0=no, 1=view, 2=edit)
		 * 5 - draw crosshairs and return XY to seekSource
		 * 6 - disable modeMenu while in use
		 * 7 - show preview doors, sprites, etc (in binary, 1 = show preview box)
		 * 8 - hotspots (0=no,1=view,2=edit)
		 * 9 - enemies (0=no,1=view,2=edit)
		 * 10 - the grid unit size in tiles (it's always a square)
		 * 11 - tilechooser style (0=none,1=text,2=map gfx,3=text&color)
		 */
				{ 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 2 }, // map edit
				{ 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 1, 0 }, // sprite edit
				{ 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0 }, // door edit
				{ 0, 2, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0 },
				{ 0, 2, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0 },
				{ 0, 2, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0 }, // for previewing
				{ 0, 2, 0, 1, 1, 0, 0, 0, 2, 0, 1, 0 }, // hotspot edit
				{ 0, 2, 0, 1, 1, 0, 0, 0, 1, 2, 2, 3 } // enemy edit
		};

		public MapGraphics(HackModule hm, int screenWidth, int screenHeight,
				int mode, boolean grid, boolean spriteBoxes, boolean centered,
				int ppu, boolean makeFields) {
			this.hm = hm;
			this.screenWidth = screenWidth;
			this.screenHeight = screenHeight;
			this.mode = mode;
			this.grid = grid;
			this.spriteBoxes = spriteBoxes;
			this.centered = centered;
			this.ppu = ppu;

			if (makeFields) {
				xField = HackModule.createSizedJTextField(Integer.toString(
						MapEditor.widthInSectors * MapEditor.sectorWidth
								* (MapEditor.tileWidth / ppu)).length(), true);
				xField.setText(Integer.toString(getMapX()));
				xField.getDocument().addDocumentListener(this);
				yField = HackModule.createSizedJTextField(Integer.toString(
						MapEditor.heightInSectors * MapEditor.sectorHeight
								* (MapEditor.tileHeight / ppu)).length(), true);
				yField.setText(Integer.toString(getMapY()));
				yField.getDocument().addDocumentListener(this);
			}

			setPreferredSize(new Dimension(screenWidth * MapEditor.tileWidth,
					screenHeight * MapEditor.tileHeight));
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}

		public MapGraphics(HackModule hm, int screenWidth, int screenHeight,
				int mode, boolean grid, boolean spriteBoxes, boolean centered) {
			this.hm = hm;
			this.screenWidth = screenWidth;
			this.screenHeight = screenHeight;
			this.mode = mode;
			this.grid = grid;
			this.spriteBoxes = spriteBoxes;
			this.centered = centered;

			setPreferredSize(new Dimension(screenWidth * MapEditor.tileWidth
					+ 2, screenHeight * MapEditor.tileHeight + 2));
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}

		public MapGraphics(HackModule hm, int screenWidth, int screenHeight,
				boolean grid, boolean spriteBoxes, boolean enemySprites,
				boolean enemyColors, MapGraphicsListener mgl) {
			this.hm = hm;
			this.screenWidth = screenWidth;
			this.screenHeight = screenHeight;
			// this.mode = mode;
			this.grid = grid;
			this.spriteBoxes = spriteBoxes;
			this.enemySprites = enemySprites;
			this.enemyColors = enemyColors;

			xField = new JTextField();
			xField.setText(Integer.toString(getMapX()));
			xField.setColumns(3);
			xField.getDocument().addDocumentListener(this);
			yField = new JTextField();
			yField.setText(Integer.toString(getMapY()));
			yField.setColumns(3);
			yField.getDocument().addDocumentListener(this);
			tilesetList = new JComboBox();
			tilesetList.addActionListener(this);
			paletteBox = new JComboBox();
			paletteBox.addActionListener(this);
			musicBox = new JComboBox();
			musicBox.addActionListener(this);

			tilesetList.setEnabled(knowssector);
			paletteBox.setEnabled(knowssector);
			musicBox.setEnabled(knowssector);

			xScroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, screenWidth, 0,
					MapEditor.width + 1);
			xScroll.addAdjustmentListener(this);
			yScroll = new JScrollBar(JScrollBar.VERTICAL, 0, screenHeight, 0,
					MapEditor.height + 1);
			yScroll.addAdjustmentListener(this);
			editBox = new MapEditor.TileChooser(23, 3, getModeProps()[11]);

			this.mgl = mgl;
			this.mode = 0;

			//setMapXY(0, 0);
			//updateComponents();
			//reloadMap();

			setPreferredSize(new Dimension(screenWidth * MapEditor.tileWidth
					+ 2, screenHeight * MapEditor.tileHeight + 2));
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}

		public void loadMusicNames() {
			musicBox.removeActionListener(this);
			musicBox.removeAllItems();
			for (int i = 0; i < EventMusicEditor.MUSIC_NUM; i++)
				musicBox.addItem(getNumberedString(EventMusicEditor
						.getEventMusicEntry(i).getDefaultName(), i, false));
			musicBox.addActionListener(this);
		}

		public void loadTilesetNames() {
			tilesetList.removeActionListener(this);
			tilesetList.removeAllItems();
			for (int i = 0; i < MapEditor.mapTsetNum; i++)
				tilesetList.addItem(getNumberedString(
						TileEditor.TILESET_NAMES[EbMap.getDrawTileset(i)], i,
						false));
			tilesetList.addActionListener(this);
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
			if (xField != null)
				xField.setEnabled(enabled);
			if (yField != null)
				yField.setEnabled(enabled);
			if (tilesetList != null)
				tilesetList.setEnabled(enabled);
			if (paletteBox != null)
				paletteBox.setEnabled(enabled);
			if (musicBox != null)
				musicBox.setEnabled(enabled);
			if (xScroll != null)
				xScroll.setEnabled(enabled);
			if (yScroll != null)
				yScroll.setEnabled(enabled);
			if (editBox != null)
				editBox.setEnabled(enabled);

			removeMouseListener(this);
			removeMouseMotionListener(this);
			removeMouseWheelListener(this);

			if (enabled) {
				addMouseListener(this);
				addMouseMotionListener(this);
				addMouseWheelListener(this);
			}
		}

		public void reloadMusicNames() {
			int selected = musicBox.getSelectedIndex();
			musicBox.removeActionListener(this);
			musicBox.removeAllItems();
			String[] musicNames = new String[EventMusicEditor.MUSIC_NUM];
			for (int i = 0; i < musicNames.length; i++)
				musicBox.addItem(i
						+ " - "
						+ EventMusicEditor.getEventMusicEntry(i)
								.getDefaultName());
			musicBox.setSelectedIndex(selected);
			musicBox.addActionListener(this);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			if (enabled && this.knowsmap)
				drawMap(g, g2d);
			if (!enabled || !grid) {
				g2d.setPaint(Color.black);
				g2d.draw(new Rectangle2D.Double(0, 0, MapEditor.tileWidth
						* screenWidth - 1, MapEditor.tileHeight * screenHeight
						- 1));
			}
		}

		private void drawMap(Graphics g, Graphics2D g2d) {
			visibleHotspots = new ArrayList();
			int tileX = getMapTileX(), tileY = getMapTileY();

			if (getModeProps()[1] == 1) {
				g2d.setPaint(Color.black);
				g2d.setFont(new Font("Arial", Font.PLAIN, 12));
				for (int i = 0; i < maparray.length; i++) {
					int[] row2draw = maparray[i];
					for (int j = 0; j < row2draw.length; j++) {
						g2d.drawString(addZeros(Integer
								.toHexString(row2draw[j]), 2),
								(j * MapEditor.tileWidth)
										+ (MapEditor.tileWidth / 2),
								(i * MapEditor.tileHeight)
										+ (MapEditor.tileHeight / 2));
						if (grid && (i % getModeProps()[10] == 0)
								&& (j % getModeProps()[10] == 0))
							g2d.draw(new Rectangle2D.Double(j
									* MapEditor.tileWidth, i
									* MapEditor.tileHeight,
									MapEditor.tileWidth, MapEditor.tileHeight));
					}
				}
			} else if (getModeProps()[1] == 2) {
				int tile_set, tile, tile_pal, sectorY, sectorX;
				for (int i = 0; i < maparray.length; i++) {
					int[] row2draw = maparray[i];
					sectorY = (i + tileY) / MapEditor.sectorHeight;
					for (int j = 0; j < row2draw.length; j++) {
						sectorX = (j + tileX) / MapEditor.sectorWidth;
						if (!EbMap.isSectorDataLoaded(sectorX, sectorY))
							EbMap.loadSectorData(hm.rom, sectorX, sectorY);
						EbMap.Sector sector = EbMap.getSectorData(sectorX,
								sectorY);
						tile_set = EbMap.getDrawTileset(sector.getTileset());
						tile = row2draw[j];
						boolean changed = false;
						if (mapChanges)
							for (int k = 0; k < MapEventEditor
									.countGroups(tile_set); k++) {
								for (int l = 0; l < MapEventEditor
										.countTileChanges(tile_set, k); l++)
									if (tile == MapEventEditor.getTileChange(
											tile_set, k, l).getTile1()) {
										tile = MapEventEditor.getTileChange(
												tile_set, k, l).getTile2();
										changed = true;
										break;
									}
								if (changed)
									break;
							}
						tile_pal = TileEditor.tilesets[tile_set].getPaletteNum(
								sector.getTileset(), sector.getPalette());
						EbMap.loadTileImage(tile_set, tile, tile_pal);

						if (!eventPalette)
							g.drawImage(EbMap.getTileImage(tile_set, tile,
									tile_pal), j * MapEditor.tileWidth, i
									* MapEditor.tileHeight,
									MapEditor.tileWidth, MapEditor.tileHeight,
									this);
						/*
						 * else g.drawImage(TileEditor
						 * .tilesets[tile_set].getArrangementImage(tile,
						 * PaletteEventEditor.getPalette(tile_set)), j *
						 * MapEditor.tileWidth, i * MapEditor.tileHeight,
						 * MapEditor.tileWidth, MapEditor.tileHeight, this);
						 */

						if (changed) {
							Rectangle2D.Double rect = new Rectangle2D.Double(
									(j * MapEditor.tileWidth),
									(i * MapEditor.tileHeight),
									MapEditor.tileWidth, MapEditor.tileHeight);
							g2d.setPaint(Color.red);
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.5F));
							g2d.fill(rect);
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 1.0F));
						}
					}
				}
			}

			if (getModeProps()[3] >= 1) {
				// this.spriteLocs = new int[spriteData[0]][5];
				for (int k = 0; k < screenHeight; k++) {
					if ((((tileY + k) % (MapEditor.sectorHeight * 2)) == 0)
							|| (k == 0)) {
						for (int i = 0; i < screenWidth; i++) {
							if ((((tileX + i) % MapEditor.sectorWidth) == 0)
									|| (i == 0)) {
								/*
								 * if (! EbMap.isSpriteDataLoaded( (tileX + i) /
								 * MapEditor.sectorWidth, (tileY + k) /
								 * (MapEditor.sectorHeight * 2)))
								 * EbMap.loadSpriteData(hm.rom, (tileX + i) /
								 * MapEditor.sectorWidth, (tileY + k) /
								 * (MapEditor.sectorHeight * 2));
								 */
								int spritesNum = EbMap.getSpritesNum(
										(tileX + i) / MapEditor.sectorWidth,
										(tileY + k)
												/ (MapEditor.sectorHeight * 2));
								short[][] spriteLocs = EbMap.getSpriteLocs(
										(tileX + i) / MapEditor.sectorWidth,
										(tileY + k)
												/ (MapEditor.sectorHeight * 2));
								short[] spriteTpts = EbMap.getSpriteTpts(
										(tileX + i) / MapEditor.sectorWidth,
										(tileY + k)
												/ (MapEditor.sectorHeight * 2));
								// this.spriteLocs = new int[spriteData[0]][5];
								for (int j = 0; j < spritesNum; j++) {
									TPTEditor.TPTEntry tptEntry = TPTEditor.tptEntries[spriteTpts[j]];
									int spriteNum = tptEntry.getSprite();
									int spriteDrawY = spriteLocs[j][1];
									int spriteDrawX = spriteLocs[j][0];
									EbMap.loadSpriteImage(hm, spriteNum,
											tptEntry.getDirection());
									SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spriteNum];

									if (((tileY + k) % (MapEditor.sectorHeight * 2)) > 0)
										spriteDrawY -= ((tileY + k) % (MapEditor.sectorHeight * 2))
												* MapEditor.tileHeight;

									if (((tileX + i) % MapEditor.sectorWidth) > 0)
										spriteDrawX -= ((tileX + i) % MapEditor.sectorWidth)
												* MapEditor.tileWidth;

									if (spriteDrawX + (i * MapEditor.tileWidth) <= screenWidth
											* MapEditor.tileWidth
											&& spriteDrawY
													+ (k * MapEditor.tileHeight) <= screenHeight
													* MapEditor.tileHeight) {
										g
												.drawImage(
														EbMap
																.getSpriteImage(
																		spriteNum,
																		tptEntry
																				.getDirection()),
														// new
														// SpriteEditor.Sprite(sib.getSpriteInfo(tptEntry.getDirection()),
														// hm).getImage(true),
														spriteDrawX
																+ (i * MapEditor.tileWidth),
														spriteDrawY
																+ (k * MapEditor.tileHeight),
														this);
										if (spriteBoxes) {
											g2d.setPaint(Color.red);
											g2d
													.draw(new Rectangle2D.Double(
															spriteDrawX
																	+ (i * MapEditor.tileWidth)
																	- 1,
															spriteDrawY
																	+ (k * MapEditor.tileHeight)
																	- 1,
															sib.width * 8 + 1,
															sib.height * 8 + 1));
										}
									}
								}
							}
						}
					}
				}
				

				if (movingData[0] == 0) {
					int spriteNum = TPTEditor.tptEntries[((EbMap.SpriteLocation) movingObject).getTpt()].getSprite();
					EbMap.loadSpriteImage(hm, spriteNum, 5);
					g2d.drawImage(
							EbMap.getSpriteImage(spriteNum,
									TPTEditor.tptEntries[((EbMap.SpriteLocation) movingObject).getTpt()].getDirection()),
							movingData[1], movingData[2], this);
					
					if (spriteBoxes) {
						g2d.setPaint(Color.red);
						g2d.draw(new Rectangle2D.Double(
								movingData[1] - 1, movingData[2] - 1,
								SpriteEditor.sib[spriteNum].width * 8 + 1,
								SpriteEditor.sib[spriteNum].height * 8 + 1));
					}		
				}
			}

			if (getModeProps()[4] >= 1) {
				for (int k = 0; k < screenHeight; k++) {
					if ((((tileY + k) % MapEditor.sectorHeight) == 0)
							|| (k == 0)) {
						for (int i = 0; i < screenWidth; i++) {
							if ((((tileX + i) % MapEditor.sectorWidth) == 0)
									|| (i == 0)) {
								/*
								 * if (! EbMap.isDoorDataLoaded( (tileX + i) /
								 * MapEditor.sectorWidth, (tileY + k) /
								 * (MapEditor.sectorHeight * 2)))
								 * EbMap.loadDoorData(hm.rom, (tileX + i) /
								 * MapEditor.sectorWidth, (tileY + k) /
								 * (MapEditor.sectorHeight * 2));
								 */
								int doorsNum = EbMap.getDoorsNum((tileX + i)
										/ MapEditor.sectorWidth, (tileY + k)
										/ (MapEditor.sectorHeight * 2));

								for (int j = 0; j < doorsNum; j++) {
									short[] doorXY = EbMap
											.getDoorXY(
													(tileX + i)
															/ MapEditor.sectorWidth,
													(tileY + k)
															/ (MapEditor.sectorHeight * 2),
													j);
									int doorDrawX = ((int) doorXY[0]) * 8;
									int doorDrawY = ((int) doorXY[1]) * 8;

									if (((tileY + k) % (MapEditor.sectorHeight * 2)) > 0)
										doorDrawY -= ((tileY + k) % (MapEditor.sectorHeight * 2))
												* MapEditor.tileHeight;

									if (((tileX + i) % MapEditor.sectorWidth) > 0)
										doorDrawX -= ((tileX + i) % MapEditor.sectorWidth)
												* MapEditor.tileWidth;

									g2d.setPaint(Color.blue);
									g2d
											.draw(new Rectangle2D.Double(
													doorDrawX
															+ (i * MapEditor.tileWidth),
													doorDrawY
															+ (k * MapEditor.tileHeight),
													8, 8));
								}
							}
						}
					}
				}
				
				if (movingData[0] == 1) {
					g2d.setPaint(Color.blue);
					g2d.draw(new Rectangle2D.Double(movingData[1], movingData[2], 8, 8));
				}
			}

			if (getModeProps()[8] >= 1) {
				HotspotEditor.Hotspot spot;
				Rectangle2D.Double rect;
				g2d.setPaint(Color.orange);
				g2d.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.5F));
				for (int i = 0; i < HotspotEditor.NUM_HOTSPOTS; i++) {
					spot = HotspotEditor.getHotspot(i);
					int x1Tile = spot.getX1() * 8 / MapEditor.tileWidth,
						y1Tile = spot.getY1() * 8 / MapEditor.tileHeight,
						x2Tile = spot.getX2() * 8 / MapEditor.tileWidth,
						y2Tile = spot.getY2() * 8 / MapEditor.tileHeight;
					if ((x1Tile <= tileX + screenWidth)
							&& (x2Tile >= tileX)
							&& (y1Tile <= tileY + screenHeight)
							&& (y2Tile >= tileY)
							&& (i != movingData[3])) {
						visibleHotspots.add(new Integer(i));
						rect = new Rectangle2D.Double(
								spot.getX1() * 8 - tileX * MapEditor.tileWidth,
								spot.getY1() * 8 - tileY * MapEditor.tileHeight,
								(spot.getX2() - spot.getX1()) * 8,
								(spot.getY2() - spot.getY1()) * 8);
						g2d.fill(rect);
					}
				}
				
				if (movingData[0] >= 2) {
					spot = (HotspotEditor.Hotspot) movingObject;
					if (movingData[0] == 2)
						rect = new Rectangle2D.Double(
								movingData[1], movingData[2],
								8 * (spot.getX2() - spot.getX1()),
								8 * (spot.getY2() - spot.getY1()));
					else
						rect = new Rectangle2D.Double(
								8 * spot.getX1() - tileX * MapEditor.tileWidth,
								8 * spot.getY1() - tileY * MapEditor.tileHeight,
								movingData[1] - (8 * spot.getX1() - tileX * MapEditor.tileWidth),
								movingData[2] - (8 * spot.getY1() - tileY * MapEditor.tileHeight));
					g2d.fill(rect);
				}
			}

			if (getModeProps()[5] == 1) {
				g2d.setPaint(Color.blue);
				g2d.draw(new Line2D.Double(0, crossY, this.getWidth(), crossY));
				g2d
						.draw(new Line2D.Double(crossX, 0, crossX, this
								.getHeight()));
			}

			if ((getModeProps()[7] & 1) == 1 && previewBoxX >= 0
					&& previewBoxY >= 0) {
				g2d.setPaint(Color.magenta);
				g2d
						.draw(new Rectangle2D.Double(previewBoxX, previewBoxY,
								8, 8));
			}

			if (getModeProps()[9] >= 1) {
				int group, incX = 2, incY = 2;
				Rectangle2D rect;
				g2d.setFont(new Font("Arial", Font.PLAIN, 12));
				String message;
				for (int i = 0; i < screenHeight; i += incY)
					for (int j = 0; j < screenWidth; j += incX) {
						group = EbMap.getEnemyLoc(hm.rom, (tileX + j) / 2,
								(tileY + i) / 2);
						incX = 2 - (tileX + j) % 2;
						incY = 2 - (tileY + i) % 2;
						if (enemyColors) {
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.5F));

							if (group > 0)
								g2d
										.setPaint(new Color(
												((int) (Math.E * 0x100000 * group)) & 0xffffff));
							else
								g2d.setPaint(noEnemyColor);

							g2d.fill(new Rectangle2D.Double(j
									* MapEditor.tileWidth, i
									* MapEditor.tileHeight, MapEditor.tileWidth
									* incX, MapEditor.tileHeight * incY));
						}

						if (group > 0) {
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 1.0F));
							g2d.setPaint(Color.black);
							message = addZeros(Integer.toHexString(group), 2);
							rect = g2d.getFontMetrics().getStringBounds(
									message, g2d);
							rect.setRect((j + 2) * MapEditor.tileWidth
									- rect.getWidth(), (i + 2)
									* MapEditor.tileHeight - rect.getHeight(),
									rect.getWidth(), rect.getHeight());
							g2d.fill(rect);
							g2d.setPaint(Color.white);
							g2d.drawString(message, (float) ((j + 2)
									* MapEditor.tileWidth - rect.getWidth()),
									(float) ((i + 2) * MapEditor.tileHeight));
							if (enemySprites)
								g2d.drawImage(getEnemyImage(group,
										MapEditor.tileWidth * 2,
										MapEditor.tileHeight * 2), j
										* MapEditor.tileWidth, i
										* MapEditor.tileHeight, this);
						}
					}
			}

			if (grid) {
				g2d.setPaint(Color.black);
				for (int i = 0; i < screenHeight; i++)
					for (int j = 0; j < screenWidth; j++) {
						if ((tileY + i) % getModeProps()[10] == 0)
							g2d.draw(new Line2D.Double(j * MapEditor.tileWidth,
									i * MapEditor.tileHeight,
									(getModeProps()[10] + j)
											* MapEditor.tileWidth, i
											* MapEditor.tileHeight));

						if ((tileX + j) % getModeProps()[10] == 0)
							g2d.draw(new Line2D.Double(j * MapEditor.tileWidth,
									i * MapEditor.tileHeight, j
											* MapEditor.tileWidth,
									(getModeProps()[10] + i)
											* MapEditor.tileHeight));
					}
			}

			if ((getModeProps()[0] >= 1) && this.knowssector) {
				g2d.setPaint(Color.yellow);

				int drawSectorX = (sectorx * MapEditor.sectorWidth * MapEditor.tileWidth)
						- (MapEditor.tileWidth * tileX);
				int drawSectorY = (sectory * MapEditor.sectorHeight * MapEditor.tileHeight)
						- (MapEditor.tileWidth * tileY);
				int drawSectorW = MapEditor.sectorWidth * MapEditor.tileWidth;
				int drawSectorH = MapEditor.sectorHeight * MapEditor.tileHeight;

				g2d.draw(new Rectangle2D.Double(drawSectorX, drawSectorY,
						drawSectorW, drawSectorH));
			}
		}

		public Image getEnemyImage(int num, int w, int h) {
			BufferedImage out = new BufferedImage(w, h,
					BufferedImage.TYPE_4BYTE_ABGR_PRE);
			Graphics2D g = (Graphics2D) out.getGraphics();
			ArrayList enemies = new ArrayList(), group;
			EnemyEditor.Enemy enemy;
			SpriteEditor.SpriteInfoBlock sib;
			EnemyPlacementGroupsEditor.EnemyPlGroup plGroup = EnemyPlacementGroupsEditor
					.getEnemyPlGroup(num);

			int x = 0, y = 0, j, k, largestHeight = 0;

			for (int i = 0; i < 2; i++) // for each subgroup
			{
				j = 0;
				if (plGroup.getEncounterRate(i) > 0)
					while ((x < w) && (j < 8)) {
						if ((plGroup.getEntry(i, j).getProbability() > 0)
								&& (plGroup.getEntry(i, j).getEnemy() > 0)) {
							group = plGroup.getEntry(i, j).getGroupEntry();
							k = 0;
							while ((x < w) && (k < group.size())) {
								enemy = EnemyEditor.enemies[((BattleEntryEditor.EnemyEntry) group
										.get(k)).getEnemy()];
								if ((!enemies.contains(enemy))
										&& (x
												+ (sib = SpriteEditor.sib[enemy
														.getOutsidePic()]).width
												* 8 <= w)) {
									EbMap.loadSpriteImage(hm, enemy
											.getOutsidePic(), 5);
									g
											.drawImage(
													EbMap
															.getSpriteImage(
																	enemy
																			.getOutsidePic(),
																	5), x, y,
													sib.width * 8,
													sib.height * 8, this);
									// (MapEditor.EbMap.getSpriteImage(enemyPic.intValue(),
									// 5), x, y, out);
									x += sib.width * 8;
									enemies.add(enemy);
									if (sib.height > largestHeight)
										largestHeight = sib.height;
								}
								k++;
							}
						}
						j++;
					}
				x = 0;
				y += largestHeight * 8;
			}

			return out;
		}

		public int getSpriteNum(int spriteX, int spriteY) {
			int[] spLocXY = getCoords(spriteX, spriteY);
			if ((spLocXY[0] > (MapEditor.widthInSectors))
					|| (spLocXY[1] > (MapEditor.heightInSectors / 2)))
				return -1;
			else
				return EbMap.findSprite(hm, spLocXY[0], spLocXY[1],
						(short) spLocXY[2], (short) spLocXY[3]);
		}
		
		public EbMap.SpriteLocation getSpriteLocation(int spriteX, int spriteY) {
			int[] spLocXY = getCoords(spriteX, spriteY);
			if ((spLocXY[0] > (MapEditor.widthInSectors))
					|| (spLocXY[1] > (MapEditor.heightInSectors / 2)))
				return null;
			else
				return EbMap.findSpriteLocation(hm, spLocXY[0], spLocXY[1],
						(short) spLocXY[2], (short) spLocXY[3]);
		}

		public short getSpriteTpt(int spriteX, int spriteY) {
			EbMap.SpriteLocation spLoc = getSpriteLocation(spriteX, spriteY);
			if (spLoc != null)
				return spLoc.getTpt();
			else
				return -1;
		}

		public int getDoorNum(int doorX, int doorY) {
			int[] doorXY = getCoords(doorX, doorY);

			return EbMap.findDoor(doorXY[0], doorXY[1], (short) doorXY[2],
					(short) doorXY[3]);
		}

		public int[] getAreaXY(int spriteX, int spriteY) {
			int areaX = ((spriteX / MapEditor.tileWidth) + getMapTileX())
					/ MapEditor.sectorWidth;
			int areaY = ((spriteY / MapEditor.tileHeight) + getMapTileY())
					/ (MapEditor.sectorHeight * 2);
			return new int[] { areaX, areaY };
		}

		public int[] getCoords(int coordX, int coordY) {
			int tileX = getMapTileX(), tileY = getMapTileY();
			int areaX = ((coordX / MapEditor.tileWidth) + tileX)
					/ MapEditor.sectorWidth;
			int areaY = ((coordY / MapEditor.tileHeight) + tileY)
					/ (MapEditor.sectorHeight * 2);
			if ((x % MapEditor.sectorWidth) > 0) {
				if (((coordX / MapEditor.tileWidth) / MapEditor.sectorWidth) == 0) {
					coordX += (tileX % MapEditor.sectorWidth)
							* MapEditor.tileWidth;
				} else {
					coordX -= (MapEditor.sectorWidth - (tileX % MapEditor.sectorWidth))
							* MapEditor.tileWidth;
				}
			}
			coordX -= (((coordX / MapEditor.tileWidth) / MapEditor.sectorWidth)
					* MapEditor.sectorWidth * MapEditor.tileWidth);
			if ((tileY % (MapEditor.sectorHeight * 2)) > 0) {
				if (((coordY / MapEditor.tileHeight) / (MapEditor.sectorHeight * 2)) == 0) {
					coordY += (tileY % (MapEditor.sectorHeight * 2))
							* MapEditor.tileHeight;
				} else {
					coordY -= ((MapEditor.sectorHeight * 2) - (tileY % (MapEditor.sectorHeight * 2)))
							* MapEditor.tileHeight;
				}
			}
			coordY -= (((coordY / MapEditor.tileHeight) / (MapEditor.sectorHeight * 2))
					* (MapEditor.sectorHeight * 2) * MapEditor.tileHeight);
			return new int[] { areaX, areaY, coordX, coordY };
		}
		
		public int[] get1BppCoords(int coordX, int coordY) {
			return new int[] { (getMapTileX() * MapEditor.tileWidth) + coordX,
					(getMapTileY() * MapEditor.tileHeight) + coordY };
		}

		public void setSector(int sectorx, int sectory) {
			if (((sectorx < 0) && (sectory < 0))
					|| (knowssector && (this.sectorx == sectorx) && (this.sectory == sectory)))
				knowssector = false;
			else {
				this.knowssector = true;
				this.sectorx = sectorx;
				this.sectory = sectory;
			}
			// editBox.
			mgl.changedSector(this.knowssector, this.sectorx, this.sectory);
		}

		public boolean knowsSector() {
			return this.knowssector;
		}

		public int[] getSectorxy() {
			return new int[] { sectorx, sectory };
		}

		public void changeMode(int mode) {
			this.oldMode = this.mode;
			this.mode = mode;
			if (getModeProps()[0] < 1) {
				knowssector = false;
				mgl.changedSector(false, -1, -1);
				tilesetList.setEnabled(false);
				tilesetList.removeActionListener(this);
				tilesetList.setSelectedIndex(-1);
				tilesetList.addActionListener(this);
				paletteBox.setEnabled(false);
				paletteBox.removeActionListener(this);
				paletteBox.removeAllItems();
				paletteBox.setSelectedIndex(-1);
				paletteBox.addActionListener(this);
				musicBox.setEnabled(false);
				musicBox.removeActionListener(this);
				musicBox.setSelectedIndex(-1);
				musicBox.addActionListener(this);
			}
			// findSprite.setEnabled(getModeProps()[3] >= 1);
			// modeMenu.setEnabled(getModeProps()[6] < 1);
			// editBox.setEnabled(getModeProps()[11] >= 1) && knowssector);
			editBox.setMode(getModeProps()[11]);

			mgl.changedMode(oldMode, mode);
		}

		public void setMapArray(int[][] newmaparray) {
			if (!this.knowsmap) {
				this.knowsmap = true;
			}
			this.maparray = newmaparray;
		}

		public void changeMapArray(int mapx, int mapy, int tile) {
			this.maparray[mapy][mapx] = tile;
		}

		public void remoteRepaint() {
			repaint();
		}

		public int getMode() {
			return this.mode;
		}

		public int getOldMode() {
			return oldMode;
		}

		public int[] getModeProps() {
			return this.modeprops[this.mode];
		}

		public int[] getModeProps(int mode2get) {
			return this.modeprops[mode2get];
		}

		public void toggleGrid() {
			grid = !grid;
		}

		public void toggleSpriteBoxes() {
			spriteBoxes = !spriteBoxes;
		}

		public void toggleMapChanges() {
			mapChanges = !mapChanges;
			editBox.toggleMapChanges();
			editBox.repaint();
		}

		public void toggleEnemySprites() {
			enemySprites = !enemySprites;
		}

		public void toggleEnemyColors() {
			enemyColors = !enemyColors;
		}

		public void toggleEventPalette() {
			eventPalette = !eventPalette;
		}

		public void setCrosshairs(int crossX, int crossY) {
			this.crossX = crossX;
			this.crossY = crossY;
		}

		public void reloadMap() {
			int tileX = getMapTileX(), tileY = getMapTileY();
			int[][] maparray = new int[screenHeight][screenWidth];
			for (int i = 0; i < screenHeight; i++)
				maparray[i] = EbMap.getTiles(hm.rom, i + tileY, tileX,
						screenWidth);
			setMapArray(maparray);
		}

		public void setMapXY(int x, int y) {
			this.x = x;
			this.y = y;
			if ((getModeProps()[7] & 1) == 1)
				setPreviewBoxXY(x, y);
		}

		public int getMapTileX() {
			int tileX = x / (MapEditor.tileWidth / ppu);
			if (centered)
				tileX -= screenWidth / 2;
			if (tileX + screenWidth > MapEditor.width + 1)
				tileX = MapEditor.width - screenWidth + 1;
			else if (tileX < 0)
				tileX = 0;
			return tileX;
		}

		public int getMapTileY() {
			int tileY = y / (MapEditor.tileHeight / ppu);
			if (centered)
				tileY -= screenHeight / 2;
			if (tileY + screenHeight > MapEditor.height + 1)
				tileY = MapEditor.height - screenHeight + 1;
			else if (tileY < 0)
				tileY = 0;
			return tileY;
		}

		public void updateComponents() {
			updateScrollBars();
			updateFields();
			updateSectorComponents();
		}

		private void updateScrollBars() {
			if (xScroll != null) {
				xScroll.removeAdjustmentListener(this);
				xScroll.setValue(this.x);
				xScroll.addAdjustmentListener(this);
			}
			if (yScroll != null) {
				yScroll.removeAdjustmentListener(this);
				yScroll.setValue(this.y);
				yScroll.addAdjustmentListener(this);
			}
		}

		private void updateFields() {
			xField.getDocument().removeDocumentListener(this);
			if (xField != null)
				xField.setText(Integer.toString(this.x));
			xField.getDocument().addDocumentListener(this);
			yField.getDocument().removeDocumentListener(this);
			if (yField != null)
				yField.setText(Integer.toString(this.y));
			yField.getDocument().addDocumentListener(this);
		}

		private void updateSectorComponents() {
			if (tilesetList != null)
				tilesetList.setEnabled(knowssector);
			if (paletteBox != null)
				paletteBox.setEnabled(knowssector);
			if (musicBox != null)
				musicBox.setEnabled(knowssector);
			if (knowssector) {
				int oldTset = -1;
				if (tilesetList.isEnabled())
					oldTset = tilesetList.getSelectedIndex();
				EbMap.Sector sector = EbMap.getSectorData(sectorx, sectory);
				tilesetList.removeActionListener(this);
				tilesetList.setSelectedIndex(sector.getTileset());
				tilesetList.addActionListener(this);
				paletteBox.removeActionListener(this);
				if (oldTset != sector.getTileset()) {
					paletteBox.removeAllItems();
					TileEditor.Tileset tileset = TileEditor.tilesets[EbMap
							.getDrawTileset(sector.getTileset())];
					for (int i = 0; i < tileset.getPaletteCount(); i++)
						if (tileset.getPalette(i).mtileset == sector
								.getTileset())
							paletteBox.addItem(Integer.toString(tileset
									.getPalette(i).mpalette));
				}
				paletteBox.setSelectedIndex(sector.getPalette());
				paletteBox.addActionListener(this);
				musicBox.removeActionListener(this);
				musicBox.setSelectedIndex(sector.getMusic());
				musicBox.addActionListener(this);

				if (getModeProps()[2] == 1) {
					boolean isSame = editBox.setTsetPal(EbMap
							.getDrawTileset(sector.getTileset()),
							TileEditor.tilesets[EbMap.getDrawTileset(sector
									.getTileset())].getPaletteNum(sector
									.getTileset(), sector.getPalette()));
					if (!isSame)
						editBox.repaint();
				}
			} else {
				if (tilesetList != null) {
					tilesetList.removeActionListener(this);
					tilesetList.setSelectedIndex(-1);
				}
				if (paletteBox != null) {
					paletteBox.removeActionListener(this);
					paletteBox.removeAllItems();
					paletteBox.setSelectedIndex(-1);
				}
				if (musicBox != null) {
					musicBox.removeActionListener(this);
					musicBox.setSelectedIndex(-1);
				}
				if (editBox != null)
					editBox.setTsetPal(-1, -1);
			}

			repaint();
			if (editBox != null)
				editBox.repaint();
		}

		public void setMapX(int x) {
			setMapXY(x, this.y);
		}

		public int getMapX() {
			return x;
		}

		public void setMapY(int y) {
			setMapXY(this.x, y);
		}

		public int getMapY() {
			return y;
		}

		public void setSeekSource(SeekListener seekSource) {
			this.seekSource = seekSource;
		}

		public SeekListener getSeekSource() {
			return seekSource;
		}

		public void setScreenWidth(int screenWidth) {
			this.screenWidth = screenWidth;
		}

		public int getScreenWidth() {
			return screenWidth;
		}

		public void setScreenHeight(int screenHeight) {
			this.screenHeight = screenHeight;
		}

		public int getScreenHeight() {
			return screenHeight;
		}

		public void setPreviewBoxXY(int x, int y) {
			int tileX = getMapTileX(), tileY = getMapTileY();
			previewBoxX = (x - (tileX * 4)) * 8;
			previewBoxY = (y - (tileY * 4)) * 8;
		}

		public void disablePreviewBox() {
			previewBoxX = -1;
			previewBoxY = -1;
		}

		public JTextField getXField() {
			return xField;
		}

		public JTextField getYField() {
			return yField;
		}

		public JComboBox getTilesetList() {
			return tilesetList;
		}

		public JComboBox getPaletteBox() {
			return paletteBox;
		}

		public JComboBox getMusicBox() {
			return musicBox;
		}

		public TileChooser getTileChooser() {
			return editBox;
		}

		public JScrollBar getXScrollBar() {
			return xScroll;
		}

		public JScrollBar getYScrollBar() {
			return yScroll;
		}

		public void changedUpdate(DocumentEvent e) {
			if (!(xField == null)
					&& !(yField == null)
					&& (e.getDocument().equals(xField.getDocument()) || e
							.getDocument().equals(yField.getDocument()))
					&& (yField.getText().length() > 0)
					&& (xField.getText().length() > 0)) {
				int newx = Integer.parseInt(xField.getText());
				int newy = Integer.parseInt(yField.getText());
				setMapXY(newx, newy);
				updateScrollBars();
				reloadMap();
				repaint();
			}
		}

		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void actionPerformed(ActionEvent e) {
			if ((e.getSource().equals(tilesetList)) && (getModeProps()[2] == 1)
					&& knowsSector()) {
				int[] sectorxy = getSectorxy();
				EbMap.Sector sector = EbMap.getSectorData(sectorxy[0],
						sectorxy[1]);
				sector.setTileset((byte) tilesetList.getSelectedIndex());

				paletteBox.removeActionListener(this);
				paletteBox.removeAllItems();
				TileEditor.Tileset tileset = TileEditor.tilesets[EbMap
						.getDrawTileset(sector.getTileset())];
				boolean couldSetOldPalette = false;
				for (int i = 0; i < tileset.getPaletteCount(); i++)
					if (tileset.getPalette(i).mtileset == sector.getTileset()) {
						paletteBox.addItem(Integer.toString(tileset
								.getPalette(i).mpalette));
						if (!couldSetOldPalette
								&& (tileset.getPalette(i).mpalette == sector
										.getPalette())) {
							couldSetOldPalette = true;
							paletteBox
									.setSelectedIndex(tileset.getPalette(i).mpalette);
						}
					}
				if (!couldSetOldPalette)
					paletteBox.setSelectedIndex(0);
				paletteBox.addActionListener(this);

				repaint();

				if (getModeProps()[2] == 1) {
					boolean isSame = editBox.setTsetPal(EbMap
							.getDrawTileset(sector.getTileset()),
							TileEditor.tilesets[EbMap.getDrawTileset(sector
									.getTileset())].getPaletteNum(sector
									.getTileset(), sector.getPalette()));
					if (!isSame)
						editBox.repaint();
				}
			} else if (!(paletteBox == null)
					&& (e.getSource().equals(paletteBox)) && knowsSector()) {
				int[] sectorxy = getSectorxy();
				EbMap.Sector sector = EbMap.getSectorData(sectorxy[0],
						sectorxy[1]);
				sector.setPalette((byte) paletteBox.getSelectedIndex());
				repaint();

				if (getModeProps()[2] == 1) {
					boolean isSame = editBox.setTsetPal(EbMap
							.getDrawTileset(sector.getTileset()),
							TileEditor.tilesets[EbMap.getDrawTileset(sector
									.getTileset())].getPaletteNum(sector
									.getTileset(), sector.getPalette()));
					if (!isSame)
						editBox.repaint();
				}
			} else if (!(musicBox == null) && (e.getSource().equals(musicBox))
					&& knowsSector()) {
				int newMusic = musicBox.getSelectedIndex();
				if (((newMusic >= 0) && (newMusic <= 255))
						&& (getModeProps()[2] == 1)) {
					int[] sectorxy = getSectorxy();
					EbMap.getSectorData(sectorxy[0], sectorxy[1]).setMusic(
							(byte) newMusic);
				}
			}
		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			int y = getMapY() + e.getWheelRotation();
			if (y > MapEditor.height - getScreenHeight() + 1)
				y = MapEditor.height - getScreenHeight() + 1;
			else if (y < 0)
				y = 0;
			setMapY(y);
			reloadMap();
			updateComponents();
			repaint();
		}

		public void mouseMoved(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			if ((mx >= getPreferredSize().width - 2)
					|| (my >= getPreferredSize().height - 2))
				return;
			else if ((getModeProps()[3] >= 2) && (getSpriteNum(mx, my) != -1))
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			else if ((getModeProps()[8] >= 2) && (visibleHotspots.size() > 0)) {
				int tileX = getMapTileX(), tileY = getMapTileY();
				for (int i = 0; i < visibleHotspots.size(); i++) {
					HotspotEditor.Hotspot spot = HotspotEditor
							.getHotspot(((Integer) visibleHotspots.get(i))
									.intValue());
					int xDiff = e.getX()
							- (spot.getX1() * 8 - (MapEditor.tileWidth * tileX)), yDiff = e
							.getY()
							- (spot.getY1() * 8 - (MapEditor.tileHeight * tileY));
					if (xDiff <= ((spot.getX2() - spot.getX1()) * 8)
							&& xDiff >= 0
							&& yDiff <= ((spot.getY2() - spot.getY1()) * 8)
							&& yDiff >= 0) {
						if ((xDiff >= ((spot.getX2() - spot.getX1() - 1) * 8))
								&& (yDiff >= ((spot.getY2() - spot.getY1() - 1) * 8)))
							setCursor(Cursor
									.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
						else
							setCursor(Cursor
									.getPredefinedCursor(Cursor.MOVE_CURSOR));
						return;
					}
				}
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			} else if (getModeProps()[5] == 1) {
				setCrosshairs(e.getX(), e.getY());
				repaint();
			} else
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		public void mouseDragged(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			if (mx < 0 || my < 0 || mx > getWidth() || my > getHeight())
				return;
			
			if ((movingData[0] >= 0)
					&& ((mx - movingData[1] != 0) || (my - movingData[1] != 0))) {
				movingData[1] = mx;
				movingData[2] = my;
				repaint();
			}
		}

		public void mousePressed(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			if (e.getButton() == 1) {
				if ((getModeProps()[3] >= 2) && (movingData[0] == -1)) {
					int spNum = getSpriteNum(mx, my);
					if (spNum >= 0) {
						int[] areaXY = getAreaXY(mx, my);
						movingData = new int[] { 0, e.getX(), e.getY(), -1 };
						movingObject = EbMap.getSpriteLocation(areaXY[0], areaXY[1], spNum);
						EbMap.removeSprite(areaXY[0], areaXY[1], spNum);
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					}
				} else if ((getModeProps()[4] >= 2) && (movingData[0] == -1)) {
					int doorNum = getDoorNum(mx, my);
					if (doorNum != -1) {
						int[] areaXY = getAreaXY(mx, my);
						movingData = new int[] { 1, e.getX(), e.getY(), -1 };
						movingObject = EbMap.getDoorLocation(areaXY[0], areaXY[1], doorNum);
						EbMap.removeDoor(areaXY[0], areaXY[1], doorNum);
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					}
				}
			}
			if ((getModeProps()[8] >= 2) && ((e.getButton() == 1) || (e.getButton() == 3))) {
				int tileX = getMapTileX(), tileY = getMapTileY(), num;
				for (int i = 0; i < visibleHotspots.size(); i++) {
					num = ((Integer) visibleHotspots.get(i)).intValue();
					HotspotEditor.Hotspot spot = HotspotEditor.getHotspot(num);
					int xDiff = e.getX()
							- (spot.getX1() * 8 - (MapEditor.tileWidth * tileX)), yDiff = e
							.getY()
							- (spot.getY1() * 8 - (MapEditor.tileHeight * tileY));
					if (xDiff <= ((spot.getX2() - spot.getX1()) * 8)
							&& xDiff >= 0
							&& yDiff <= ((spot.getY2() - spot.getY1()) * 8)
							&& yDiff >= 0) {
						if (e.getButton() == 3)
							net.starmen.pkhack.JHack.main.showModule(
									HotspotEditor.class, new Integer(num));
						else {
							if ((xDiff >= ((spot.getX2() - spot.getX1() - 1) * 8))
									&& (yDiff >= ((spot.getY2() - spot.getY1() - 1) * 8))) {
								setCursor(Cursor
										.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
								movingData = new int[] { 3, e.getX(), e.getY(), num };
							} else {
								setCursor(Cursor
										.getPredefinedCursor(Cursor.MOVE_CURSOR));
								movingData = new int[] { 2, e.getX(), e.getY(), num };
							}
							movingObject = spot;
						}
						return;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (movingData[0] >= 0) {
				if (movingData[0] <= 1) {
					int[] coords = getCoords(e.getX(), e.getY());
					if (movingData[0] == 0) {
						EbMap.SpriteLocation sl = (EbMap.SpriteLocation) movingObject;
						sl.setX((short) coords[2]);
						sl.setY((short) coords[3]);
						EbMap.addSprite(coords[0], coords[1], sl);
					} else if (movingData[0] == 1) {
						EbMap.DoorLocation dl = (EbMap.DoorLocation) movingObject;
						dl.setX((short) (coords[2] / 8));
						dl.setY((short) (coords[3] / 8));
						EbMap.addDoor(coords[0], coords[1], dl);
					}
				} else if (movingData[0] <= 3) {
					int[] coords = get1BppCoords(e.getX(), e.getY());
					if (movingData[0] == 2) {
						HotspotEditor.getHotspot(movingData[3]).setX2((short) ((coords[0] / 8)
								+ (HotspotEditor.getHotspot(movingData[3]).getX2()
										- HotspotEditor.getHotspot(movingData[3]).getX1())));
						HotspotEditor.getHotspot(movingData[3]).setX1((short) (coords[0] / 8));
						HotspotEditor.getHotspot(movingData[3]).setY2((short) ((coords[1] / 8)
								+ (HotspotEditor.getHotspot(movingData[3]).getY2()
										- HotspotEditor.getHotspot(movingData[3]).getY1())));
						HotspotEditor.getHotspot(movingData[3]).setY1((short) (coords[1] / 8));
					} else {
						HotspotEditor.getHotspot(movingData[3]).setX2((short) (coords[0] / 8));
						HotspotEditor.getHotspot(movingData[3]).setY2((short) (coords[1] / 8));
					}
				}
				movingData[0] = -1;
				movingData[3] = -1;
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				repaint();
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == 1) {
				if ((editBox != null) && editBox.knowsTsetPal()
						&& (getModeProps()[2] >= 1)) {
					int mapx = e.getX() / MapEditor.tileWidth;
					int mapy = e.getY() / MapEditor.tileHeight;
					if (e.getModifiers() == 17) {
						int selected = EbMap.getTile(hm.rom, mapx + getMapX(),
								mapy + getMapY());
						editBox.setSelected(selected);
						if (!editBox.isSelectedVisible())
							editBox.scrollToSelected();
						editBox.repaint();
					} else if (editBox.isSelected()) {
						int tile = editBox.getSelected();
						int localtset = tile / 0x100;
						tile -= localtset * 0x100;

						EbMap.changeTile(getMapX() + mapx, getMapY() + mapy,
								(byte) (tile & 0xff));
						changeMapArray(mapx, mapy, tile);
						EbMap.setLocalTileset(hm.rom, getMapX() + mapx,
								getMapY() + mapy, localtset);
						reloadMap();
						repaint();
					}
				} else if ((getMode() == 3) || (getMode() == 4)) {
					getSeekSource().returnSeek(e.getX(), e.getY(), getMapX(),
							getMapY());
					changeMode(getOldMode());
					repaint();
				} else if (getModeProps()[9] >= 2) {
					int enemyX = (getMapTileX() + e.getX()
							/ MapEditor.tileWidth) / 2, enemyY = (getMapTileY() + e
							.getY()
							/ MapEditor.tileHeight) / 2;
					if (e.getModifiers() == 17) {
						editBox.setSelected(EbMap.getEnemyLoc(hm.rom, enemyX,
								enemyY));
						if (!editBox.isSelectedVisible())
							editBox.scrollToSelected();
						editBox.repaint();
					} else if (editBox.isSelected()) {
						EbMap.changeEnemyLoc(enemyX, enemyY, (byte) editBox
								.getSelected());
						repaint();
					}
				}
			} else if (e.getButton() == 3) {
				if ((getModeProps()[3] >= 2) && (movingData[0] == -1)) {
					JPopupMenu popup = new JPopupMenu();

					int spNum = getSpriteNum(e.getX(), e.getY());
					if (spNum != -1) {
						int[] spLoc = getCoords(e.getX(), e.getY());
						popupListener = new PopupListener(this, spLoc[0],
								spLoc[1], spLoc[2], spLoc[3], spNum);
						popup.add(EbHackModule.createJMenuItem("Add sprite",
								'a', null, PopupListener.ADD_SPRITE,
								popupListener));
						popup.add(EbHackModule.createJMenuItem("Delete sprite",
								'd', null, PopupListener.DEL_SPRITE,
								popupListener));
						popup.add(EbHackModule.createJMenuItem("Cut sprite",
								'u', null, PopupListener.CUT_SPRITE,
								popupListener));
						popup.add(EbHackModule.createJMenuItem("Copy sprite",
								'c', null, PopupListener.COPY_SPRITE,
								popupListener));
						if (PopupListener.hasCopiedTPT())
							popup.add(EbHackModule.createJMenuItem(
									"Paste sprite", 'p', null,
									PopupListener.PASTE_SPRITE, popupListener));
						String tpt = addZeros(Integer.toHexString(EbMap
								.getSpriteTpt(spLoc[0], spLoc[1], spNum)), 4);
						popup.add(EbHackModule.createJMenuItem(
								"Switch TPT entry (" + tpt + ")", 's', null,
								PopupListener.CHANGE_TPT, popupListener));
						popup.add(EbHackModule.createJMenuItem(
								"Edit TPT entry (" + tpt + ")", 'e', null,
								PopupListener.EDIT_TPT, popupListener));
					} else {
						int[] spLoc = getCoords(e.getX(), e.getY());
						popupListener = new PopupListener(this, spLoc[0],
								spLoc[1], spLoc[2], spLoc[3]);
						popup.add(EbHackModule.createJMenuItem("Add sprite",
								'a', null, PopupListener.ADD_SPRITE,
								popupListener));
						if (PopupListener.hasCopiedTPT())
							popup.add(EbHackModule.createJMenuItem(
									"Paste sprite", 'p', null,
									PopupListener.PASTE_SPRITE, popupListener));
					}
					popup.show(this, e.getX(), e.getY());
				} else if ((getModeProps()[4] == 2) && (movingData[0] == -1)) {
					JPopupMenu popup = new JPopupMenu();

					int num = getDoorNum(e.getX(), e.getY());
					int[] doorCoords = getCoords(e.getX(), e.getY());
					if (num != -1) {
						popupListener = new PopupListener(this, doorCoords[0],
								doorCoords[1], doorCoords[2], doorCoords[3],
								num);
						popup.add(EbHackModule.createJMenuItem("Add door", 'a',
								null, PopupListener.ADD_DOOR, popupListener));
						popup.add(EbHackModule.createJMenuItem("Delete door",
								'd', null, PopupListener.DEL_DOOR,
								popupListener));
						EbMap.DoorLocation doorLocation = EbMap
								.getDoorLocation(doorCoords[0], doorCoords[1],
										num);
						String dest;
						if (EbMap.getDoorDestType(doorLocation.getType()) >= 0)
							dest = "Destination #"
									+ Integer.toString(doorLocation
											.getDestIndex());
						else
							dest = "No Destination";
						popup.add(EbHackModule.createJMenuItem("Edit entry ("
								+ dest + ")", 's', null,
								PopupListener.EDIT_DEST, popupListener));
						if (EbMap.getDoorDestType(doorLocation.getType()) == 0)
							popup.add(EbHackModule.createJMenuItem(
									"Jump to destination", 'j', null,
									PopupListener.JUMP_DEST, popupListener));
					} else {
						popupListener = new PopupListener(this, doorCoords[0],
								doorCoords[1], doorCoords[2], doorCoords[3]);
						popup.add(EbHackModule.createJMenuItem("Add door", 'a',
								null, PopupListener.ADD_DOOR, popupListener));
					}
					popup.show(this, e.getX(), e.getY());
				} else if (getModeProps()[0] == 2) {
					int sectorx = ((e.getX() / MapEditor.tileWidth) + getMapX())
							/ sectorWidth;
					int sectory = ((e.getY() / MapEditor.tileHeight) + getMapY())
							/ sectorHeight;
					setSector(sectorx, sectory);

					updateSectorComponents();
				} else if (getModeProps()[9] >= 2)
					net.starmen.pkhack.JHack.main.showModule(
							EnemyPlacementGroupsEditor.class,
							new Integer(EbMap.getEnemyLoc(hm.rom,
									(getMapTileX() + e.getX()
											/ MapEditor.tileWidth) / 2,
									(getMapTileY() + e.getY()
											/ MapEditor.tileHeight) / 2)));
			}
		}

		public void adjustmentValueChanged(AdjustmentEvent ae) {
			if (ae.getSource().equals(xScroll))
				setMapX(ae.getValue());
			else if (ae.getSource().equals(yScroll))
				setMapY(ae.getValue());
			else
				return;
			reloadMap();
			updateComponents();
			repaint();
		}

		public static class PopupListener implements ActionListener {
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

			public static final String JUMP_DEST = "jumpDest";

			private int areaX, areaY, coordX, coordY, num;

			private static short copyTpt = -1;

			private MapGraphics gfxcontrol;

			public PopupListener(MapGraphics gfxcontrol, int areaX, int areaY,
					int coordX, int coordY, int num) {
				this.gfxcontrol = gfxcontrol;
				this.areaX = areaX;
				this.areaY = areaY;
				this.coordX = coordX;
				this.coordY = coordY;
				this.num = num;
				PopupListener.copyTpt = copyTpt;
			}

			public PopupListener(MapGraphics gfxcontrol, int areaX, int areaY,
					int coordX, int coordY) {
				this.gfxcontrol = gfxcontrol;
				this.areaX = areaX;
				this.areaY = areaY;
				this.coordX = coordX;
				this.coordY = coordY;
				PopupListener.copyTpt = copyTpt;
			}

			public static boolean hasCopiedTPT() {
				return copyTpt >= 0;
			}

			public void actionPerformed(ActionEvent e) {
				String ac = e.getActionCommand();
				if (ac.equals(ADD_SPRITE)) {
					int sure = JOptionPane.YES_OPTION;
					if (EbMap.getSpritesNum(areaX, areaY) >= 30)
						sure = JOptionPane
								.showConfirmDialog(
										gfxcontrol,
										"By adding this sprite, you exceed the limit of sprites per area (30)."
												+ "\nDo you still want to add this sprite entry?",
										"Are you sure?",
										JOptionPane.YES_NO_OPTION);
					if (sure == JOptionPane.YES_OPTION) {
						EbMap.addSprite(areaX, areaY, (short) coordX,
								(short) coordY, (short) 0);
						gfxcontrol.remoteRepaint();
					}
				} else if (ac.equals(DEL_SPRITE)) {
					int sure = JOptionPane.showConfirmDialog(gfxcontrol,
							"Are you sure you want to delete "
									+ "this sprite entry?\nThis action "
									+ "cannot be undone.", "Are you sure?",
							JOptionPane.YES_NO_OPTION);
					if (sure == JOptionPane.YES_OPTION) {
						EbMap.removeSprite(areaX, areaY, num);
						gfxcontrol.remoteRepaint();
					}
				} else if (ac.equals(CHANGE_TPT)) {
					short tpt = EbMap.getSpriteTpt(areaX, areaY, num);
					String input = JOptionPane
							.showInputDialog(
									gfxcontrol,
									"Change which TPT entry this"
											+ " sprite entry will display. (Hexidecimal input)",
									Integer.toHexString((int) tpt));
					if (input != null) {
						short newTpt = (short) Integer.parseInt(input, 16);
						short[] spriteXY = EbMap.getSpriteXY(areaX, areaY, num);
						EbMap.removeSprite(areaX, areaY, num);
						EbMap.addSprite(areaX, areaY, spriteXY[0], spriteXY[1],
								newTpt);
						gfxcontrol.remoteRepaint();
					}
				} else if (ac.equals(EDIT_TPT)) {
					short tpt = EbMap.getSpriteTpt(areaX, areaY, num);
					net.starmen.pkhack.JHack.main.showModule(TPTEditor.class,
							new Integer(tpt));
					gfxcontrol.remoteRepaint();
				} else if (ac.equals(COPY_SPRITE)) {
					copyTpt = EbMap.getSpriteTpt(areaX, areaY, num);
				} else if (ac.equals(CUT_SPRITE)) {
					copyTpt = EbMap.getSpriteTpt(areaX, areaY, num);
					EbMap.removeSprite(areaX, areaY, num);
					gfxcontrol.remoteRepaint();
				} else if (ac.equals(PASTE_SPRITE)) {
					EbMap.addSprite(areaX, areaY, (short) coordX,
							(short) coordY, copyTpt);
					gfxcontrol.remoteRepaint();
				} else if (ac.equals(ADD_DOOR)) {
					EbMap.addDoor(areaX, areaY, (short) (coordX / 8),
							(short) (coordY / 8), (byte) 0, (byte) 0);
					gfxcontrol.remoteRepaint();
				} else if (ac.equals(DEL_DOOR)) {
					EbMap.removeDoor(areaX, areaY, num);
					gfxcontrol.remoteRepaint();
				} else if (ac.equals(EDIT_DEST)) {
					net.starmen.pkhack.JHack.main.showModule(DoorEditor.class,
							new int[] { areaX, areaY, num });
				} else if (ac.equals(JUMP_DEST)) {
					EbMap.Destination dest = EbMap.getDestination(EbMap
							.getDoorLocation(areaX, areaY, num).getDestIndex());
					gfxcontrol.setMapXY(dest.getX() * 8 / MapEditor.tileWidth,
							dest.getY() * 8 / MapEditor.tileHeight);
					gfxcontrol.updateComponents();
					gfxcontrol.reloadMap();
					gfxcontrol.remoteRepaint();
				}
			}
		}
	}



	public static class TileChooser extends AbstractButton implements
			MouseListener, AdjustmentListener {
		private JScrollBar scroll;

		private int selected = -1, tset = -1, pal = -1, width, height, mode;

		private boolean mapChanges = false;

		public TileChooser(int width, int height, int mode) {
			this.width = width;
			this.height = height;
			this.mode = mode;
			addMouseListener(this);
			scroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, width, 0,
					1024 / (height + 1) - 1);
			scroll.addAdjustmentListener(this);

			this.setPreferredSize(new Dimension((getWidthTiles() + 1)
					* MapEditor.tileWidth + 1, (getHeightTiles() + 1)
					* MapEditor.tileHeight + 1));
		}

		public TileChooser(int width, int height, boolean textTiles) {
			this(width, height, textTiles ? 1 : 2);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			if (isEnabled()) {
				drawTiles(g, g2d);
				scroll.setEnabled(true);
			} else {
				drawBorder(g2d);
				scroll.setEnabled(false);
			}
		}

		public boolean isEnabled() {
			return (knowsTsetPal() || (mode != 2)) && (mode > 0);
		}

		public void setMode(int mode) {
			this.mode = mode;

			if (mode == 3)
				scroll.setMaximum(EnemyPlacementGroupsEditor.ENEMY_GROUPS_COUNT
						/ (height + 1));
			else
				scroll.setMaximum(1024 / (height + 1) - 1);
		}

		public void toggleMapChanges() {
			mapChanges = !mapChanges;
		}

		public boolean setTsetPal(int newtset, int newpal) {
			boolean isSame = (this.tset == newtset) && (this.pal == newpal);
			this.tset = newtset;
			this.pal = newpal;
			return isSame;
		}

		public int getTset() {
			return tset;
		}

		public int getPal() {
			return pal;
		}

		public boolean knowsTsetPal() {
			return (tset > -1) && (pal > -1);
		}

		public void setSelected(int newselectedx, int newselectedy) {
			int tilex = newselectedx / MapEditor.tileWidth;
			int tiley = newselectedy / MapEditor.tileHeight;

			if ((tilex <= width) && (tiley <= height))
				this.selected = ((scroll.getValue() + tilex) * (height + 1))
						+ tiley;
		}

		public void setSelected(int selected) {
			this.selected = selected;
		}

		public int getSelected() {
			return selected;
		}

		public boolean isSelected() {
			return this.selected != -1;
		}

		public void scrollToSelected() {
			int selectedPos;
			if (selected > (1024 / (getHeightTiles() + 1)) - getWidthTiles())
				selectedPos = (selected / (getHeightTiles() + 1))
						- getWidthTiles();
			else
				selectedPos = selected / (getHeightTiles() + 1);
			scroll.setValue(selectedPos);
		}

		public boolean isSelectedVisible() {
			int selectedX = selected / (getHeightTiles() + 1);
			return (selectedX >= (scroll.getValue()))
					&& (selectedX <= scroll.getValue() + width);
		}

		private void drawBorder(Graphics2D g2d) {
			g2d.setPaint(Color.black);
			for (int i = 0; i <= width; i++) {
				for (int j = 0; j <= height; j++) {
					g2d.draw(new Rectangle2D.Double(i * MapEditor.tileWidth, j
							* MapEditor.tileHeight, MapEditor.tileWidth,
							MapEditor.tileHeight));
				}
			}
		}

		private void drawTiles(Graphics g, Graphics2D g2d) {
			g2d.setPaint(Color.black);
			g2d.setFont(new Font("Arial", Font.PLAIN, 12));
			for (int i = 0; i <= width; i++) {
				for (int j = 0; j <= height; j++) {
					int tile = ((scroll.getValue() + i) * (height + 1)) + j;
					boolean changed = false;
					if ((mapChanges) && ((mode == 1) || (mode == 2)))
						for (int k = 0; k < MapEventEditor.countGroups(tset); k++) {
							for (int l = 0; l < MapEventEditor
									.countTileChanges(tset, k); l++)
								if (tile == MapEventEditor.getTileChange(tset,
										k, l).getTile1()) {
									tile = MapEventEditor.getTileChange(tset,
											k, l).getTile2();
									changed = true;
									break;
								}
							if (changed)
								break;
						}
					if ((mode == 3)
							&& (tile < EnemyPlacementGroupsEditor.ENEMY_GROUPS_COUNT)) {
						g2d.setPaint(new Color(
								((int) (Math.E * 0x100000 * tile)) & 0xffffff));
						g2d.fill(new Rectangle2D.Double(
								(i * MapEditor.tileWidth),
								(j * MapEditor.tileHeight),
								MapEditor.tileWidth, MapEditor.tileHeight));
						g2d.setPaint(Color.WHITE);
						g2d.fill(new Rectangle2D.Double(
								(i * MapEditor.tileWidth),
								((j + 0.25) * MapEditor.tileHeight),
								MapEditor.tileWidth, MapEditor.tileHeight / 2));
						g2d.setPaint(Color.BLACK);
						g2d.drawString(addZeros(Integer.toHexString(tile), 2),
								(float) ((i + 0.25) * MapEditor.tileWidth),
								(float) ((j + 0.75) * MapEditor.tileHeight));
					} else if (mode == 1)
						g2d.drawString(addZeros(Integer.toHexString(tile), 2),
								(i * MapEditor.tileWidth),
								(j * MapEditor.tileHeight));
					else if (mode == 2) {
						EbMap.loadTileImage(tset, tile, pal);
						g
								.drawImage(EbMap.getTileImage(tset, tile, pal),
										(i * MapEditor.tileWidth),
										(j * MapEditor.tileHeight),
										MapEditor.tileWidth,
										MapEditor.tileHeight, this);
					}

					g2d.setPaint(Color.black);
					g2d.draw(new Rectangle2D.Double((i * MapEditor.tileWidth),
							(j * MapEditor.tileHeight), MapEditor.tileWidth,
							MapEditor.tileHeight));

					tile = ((scroll.getValue() + i) * (height + 1)) + j;
					if (changed || (selected == tile)) {
						Rectangle2D.Double rect = new Rectangle2D.Double(
								(i * MapEditor.tileWidth),
								(j * MapEditor.tileHeight),
								MapEditor.tileWidth, MapEditor.tileHeight);
						if (changed && (selected == tile)) {
							g2d.setPaint(Color.orange);
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.7F));
						} else if (changed) {
							g2d.setPaint(Color.red);
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.5F));
						} else if (selected == tile) {
							g2d.setPaint(Color.yellow);
							g2d.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.6F));
						}
						g2d.fill(rect);
						g2d.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 1.0F));
					}
				}
			}
		}

		public int getWidthTiles() {
			return width;
		}

		public void setWidthTiles(int width) {
			this.width = width;
		}

		public int getHeightTiles() {
			return height;
		}

		public void setHeightTiles(int height) {
			this.height = height;
		}

		public JScrollBar getScrollBar() {
			return scroll;
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
			if ((e.getButton() == 1) && isEnabled()) {
				int tile = ((e.getX() / MapEditor.tileWidth) + scroll
						.getValue())
						* (height + 1) + (e.getY() / MapEditor.tileHeight);
				if (e.isShiftDown())
					if (mode == 3)
						net.starmen.pkhack.JHack.main.showModule(
								EnemyPlacementGroupsEditor.class, new Integer(
										tile));
					else
						net.starmen.pkhack.JHack.main.showModule(
								TileEditor.class, new int[] { getTset(),
										getPal(), tile });
				else if ((mode != 3)
						|| (tile < EnemyPlacementGroupsEditor.ENEMY_GROUPS_COUNT)) {
					setSelected(e.getX(), e.getY());
					repaint();
					this.fireActionPerformed(new ActionEvent(this,
					// ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
							ActionEvent.ACTION_PERFORMED, "tile chosen"));
				}
			}
		}

		public void adjustmentValueChanged(AdjustmentEvent e) {
			repaint();
		}
	}
}