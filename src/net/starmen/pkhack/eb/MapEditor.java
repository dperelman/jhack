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

public class MapEditor extends EbHackModule implements ActionListener,
    PropertyChangeListener, MouseListener, AdjustmentListener
{

    public MapEditor(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    private JFrame mainWindow;

    // Stuff for the top buttons
    private JPanel top_buttons;
    private int x = 0;
    private int y = 0;
    private int palette = 0;
    private int music = 0;
    // real dimensions are 256 x 320, but I'm starting from 0.
    private int width = 255;
    private int height = 319;
    private int screen_x = 5;
    private int screen_y = 10;
    private int tilewidth = 32;
    private int tileheight = 32;
    private int screen_width = 24;
    private int screen_height = 12;
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
    private JScrollBar scrollh, scrollv;

    public static final String[][][] menuNames = {
        {{"File", "f"}, {"Save Changes", "s"}, {"Exit", "q"}},
        {{"Mode", "m"}, {"Map View (Text)", "1"}, {"Map View", "2"},
            {"Map Edit", "3"}}, {{"Help", "h"}, {"About", "a"}}};

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
        musicField.setValue(new Integer(music));
        musicField.setColumns(3);
        musicField.addPropertyChangeListener("value", this);
        panel.add(musicField);

        return panel;
    }

    // A neat little way to create a JMenuBar from an array
    public JMenuBar createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        // Create the actual menu
        for (int i = 0; i <= menuNames.length - 1; i++)
        {
            JMenu menu = new JMenu(menuNames[i][0][0]);
            menu.setMnemonic(menuNames[i][0][0].charAt(0));
            for (int i2 = 1; i2 < menuNames[i].length; i2++)
            {
                // JMenuItem menuItem = new JMenuItem(menuNames[i][i2]);
                JMenuItem menuItem = EbHackModule.createJMenuItem(
                    menuNames[i][i2][0], menuNames[i][i2][1].charAt(0), null,
                    "M" + i + i2, this);
                // menuItem.setActionCommand("M" + i + i2);
                // menuItem.addActionListener(this);
                menu.add(menuItem);
            }
            menuBar.add(menu);
        }

        return menuBar;
    }

    public void createGUI()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        // Create and set up the window.
        // mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // mainWindow.setSize(window_width, window_height);

        top_buttons = createTopButtons();

        scrollh = new JScrollBar(JScrollBar.HORIZONTAL, 0, 15, 0, width);
        scrollh.addAdjustmentListener(this);

        scrollv = new JScrollBar(JScrollBar.VERTICAL, 0, 15, 0, height);
        scrollv.addAdjustmentListener(this);

        gfxcontrol
            .setPreferredSize(new Dimension(tilewidth * screen_width
                + screen_width + 1, tileheight * screen_height + screen_height
                + 1));

        // mainWindow.getContentPane().add(gfxcontrol);
        mainWindow.getContentPane().add(top_buttons, BorderLayout.NORTH);
        mainWindow.getContentPane().add(scrollh, BorderLayout.SOUTH);
        mainWindow.getContentPane().add(scrollv, BorderLayout.EAST);
        mainWindow.getContentPane().add(gfxcontrol);
        mainWindow.setJMenuBar(createMenuBar());

        mainWindow.pack();

        gfxcontrol.addMouseListener(this);
    }

    protected void init()
    {
        mapcontrol = new EbMap(this, width, height, sector_width, sector_height);
        gfxcontrol = new MapGraphics(this, x, y, screen_x, screen_y,
            screen_width, screen_height, tilewidth, tileheight, sector_width,
            sector_height, 2);

        // Create the GUI.
        createGUI();

        // Testing stuff.
        /*
         * int[] test_row = mapcontrol.getTiles(0, 0, 5); for (int i = 0; i <
         * test_row.length; i++) { System.out.println("Test row tile " + i + ": " +
         * Integer.toHexString(test_row[i])); }
         */

        int[][] maparray = new int[screen_height][screen_width];
        for (int i = 0; i < screen_height; i++)
        {
            maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
        }
        gfxcontrol.setMapArray(maparray);
        gfxcontrol.remoteRepaint();
    }

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
            gfxcontrol.setx(x);
            gfxcontrol.remoteRepaint();

            xField.setValue(new Integer(x));
        }
        else if (source == scrollv)
        {
            y = ae.getValue();
            int[][] maparray = new int[screen_height][screen_width];
            for (int i = 0; i < screen_height; i++)
            {
                maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
            }
            gfxcontrol.setMapArray(maparray);
            gfxcontrol.sety(y);
            gfxcontrol.remoteRepaint();

            yField.setValue(new Integer(y));
        }
    }

    public void propertyChange(PropertyChangeEvent e)
    {
        // System.out.println("Property change!");
        Object source = e.getSource();
        // System.out.println("source: " + e.getSource());
        if (source == xField)
        {
            int newx = ((Number) xField.getValue()).intValue();
            if ((newx >= 0) && (newx <= width))
            {
                x = newx;
                int[][] maparray = new int[screen_height][screen_width];
                for (int i = 0; i < screen_height; i++)
                {
                    maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                }
                gfxcontrol.setMapArray(maparray);
                gfxcontrol.setx(x);
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
            if ((newy >= 0) && (newy <= height))
            {
                y = newy;
                int[][] maparray = new int[screen_height][screen_width];
                for (int i = 0; i < screen_height; i++)
                {
                    maparray[i] = mapcontrol.getTiles(i + y, x, screen_width);
                }
                gfxcontrol.setMapArray(maparray);
                gfxcontrol.sety(y);
                gfxcontrol.remoteRepaint();

                scrollv.setValue(y);
            }
            else
            {
                yField.setValue(new Integer(y));
            }
        }

    }

    public void actionPerformed(ActionEvent e)
    {
        String name = e.getActionCommand();
        String type = name.substring(0, 1);
        if (type.equals("M"))
        {
            menuAction(name.substring(1, 2), name.substring(2, 3));
        }
        else if (type.equals("T"))
        {
            // System.out.println("A top buttons action: " + name.substring(1));
        }
    }

    public void menuAction(String n1, String n2)
    {
        // System.out.println("A menu action: " + n1 + n2);
        if (Integer.parseInt(n1) == 1)
        {
            gfxcontrol.changeMode(Integer.parseInt(n2) - 1);
            gfxcontrol.remoteRepaint();
        }
    }

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
        // System.out.println("Mouse clicked! Button: " + e.getButton());
        if (e.getButton() == 3)
        {
            int mousex = e.getX();
            int mousey = e.getY();
            if ((mousex >= screen_x) && (mousex <= screen_width * tilewidth)
                && (mousey >= screen_y)
                && (mousey <= screen_height * tileheight))
            {
                int sectorx = getSectorXorY(getTileXorY(mousex - screen_x,
                    tilewidth, screen_width)
                    + x, sector_width);
                int sectory = getSectorXorY(getTileXorY(mousey - screen_y,
                    tileheight, screen_height)
                    + y, sector_height);

                int[] modeprops = gfxcontrol.getModeProps();
                if (modeprops[0] == 2)
                {
                    // System.out.println("Sector xy: " + sectorx + "," +
                    // sectory);
                    int[] tsetpal = mapcontrol.getTsetPal(sectorx, sectory);
                    tilesetList.setSelectedIndex(tsetpal[0]);
                    paletteField.setValue(new Integer(tsetpal[1]));
                }

                // System.out.println("Tile: " + getTileXorY(mousex - screen_x,
                // tilewidth, screen_width) + "x" + getTileXorY(mousey -
                // screen_y, tileheight, screen_height));
                gfxcontrol.setSector(sectorx, sectory);
                gfxcontrol.remoteRepaint();
            }
        }
    }

    public int getSectorXorY(int tilexory, int sector_woh)
    {
        for (int i = 0; i <= width; i++)
        {
            if (tilexory < sector_woh)
            {
                return i;
            }
            else
            {
                tilexory = tilexory - sector_woh;
            }
        }
        return -1;
    }

    public int getTileXorY(int mousexory, int tile_woh, int screen_limit)
    {
        for (int i = 0; i <= screen_limit; i++)
        {
            if (mousexory < tile_woh)
            {
                return i;
            }
            else
            {
                mousexory = mousexory - tile_woh;
            }
        }
        return -1;
    }

    public void show()
    {
        super.show();
        this.reset();
        tilesetList.setSelectedIndex(0);
        tilesetList.updateUI();
        mainWindow.setVisible(true);
        mainWindow.repaint();
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public String getDescription()
    {
        return "Map Viewer";
    }

    public String getVersion()
    {
        return "0.1.5";
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
        private int starteditx;
        private int startedity;

        private boolean knowsmap = false;
        private boolean knowssector = false;
        // This variable should define what a mode does.
        private int[][] modeprops = new int[][]{
        // { allow sectors to be selectable with right-click (0 = no, 1 = yes, 2
            // = change sector vars too), draw map (0=no, 1=text tiles, 2=gfx
            // tiles),
            //   allow map editing}
            {2, 1, 0}, {2, 2, 0}, {2, 2, 1}};
        // tile_images is sorted by tilset, tile, palette
        private Image[][][] tile_images = new Image[TILESET_NAMES.length][1024][59];

        public MapGraphics(EbHackModule newhm, int newx, int newy,
            int newstartx, int newstarty, int newwidth, int newheight,
            int newtilewidth, int newtileheight, int newsector_width,
            int newsector_height, int newmode)
        {
            // System.out.println("Now evaluating MapGraphics()");
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
            this.starteditx = this.startx;
            this.startedity = this.starty + (this.height * this.tileheight) + 5;

            this.mode = newmode;

            // TileEditor.readFromRom(this.hm); // load tileset data into
            // "tilesets" array
        }

        public void paintComponent(Graphics g)
        {
            // System.out.println("Now evaluating paintComponent()");
            // System.out.println("Map array is set?: " + this.maparrayisset);
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            // drawBorder(g2d); // this is pointless...
            if (this.knowsmap)
            {
                drawMap(g, g2d);
            }
        }

        protected void drawMap(Graphics g, Graphics2D g2d)
        {
            Font test_font = new Font("Arial", Font.PLAIN, 12);
            if (this.modeprops[this.mode][1] == 1)
            {
                g2d.setPaint(Color.black);
                g2d.setFont(test_font);
                for (int i = 0; i < maparray.length; i++)
                {
                    int[] row2draw = maparray[i];
                    for (int i2 = 0; i2 < row2draw.length; i2++)
                    {
                        // System.out.println("Row " + i + ", Column " + i2 + ";
                        // " + maparray[i][i2]);
                        g2d.drawString(addZeros(Integer
                            .toHexString(row2draw[i2]), 2), this.startx
                            + (i2 * this.tilewidth) + (this.tilewidth / 2),
                            this.starty + (i * this.tileheight)
                                + (this.tileheight / 2));
                        g2d.draw(new Rectangle2D.Double(this.startx
                            + (i2 * this.tilewidth), this.starty
                            + (i * this.tileheight), tilewidth, tileheight));
                    }
                }
            }
            else if (this.modeprops[this.mode][1] == 2)
            {
                int tile_set, tile_tile, tile_pal;
                for (int i = 0; i < maparray.length; i++)
                {
                    int[] row2draw = maparray[i];
                    for (int i2 = 0; i2 < row2draw.length; i2++)
                    {
                        int[] tsetPal = mapcontrol.getTsetPal(getSectorXorY(i2
                            + x, this.sector_width), getSectorXorY(i + y,
                            this.sector_height));
                        //                        System.out.println("Local tileset: "
                        //                            + mapcontrol.getLocalTileset(i2 + x, i + y));
                        // this.loadTsetImages(mapcontrol.getDrawTileset(tsetPal[0]));
                        tile_set = mapcontrol.getDrawTileset(tsetPal[0]);
                        tile_tile = row2draw[i2]
                            | (mapcontrol.getLocalTileset(i2 + x, i + y) << 8);
                        // tile_pal = tsetPal[1]; //
                        // TileEditor.tilesets[mapcontrol.getDrawTileset(tsetPal[0])].getPaletteNum(tsetPal[0],
                        // tsetPal[1]);
                        tile_pal = TileEditor.tilesets[mapcontrol
                            .getDrawTileset(tsetPal[0])].getPaletteNum(
                            tsetPal[0], tsetPal[1]);
                        // System.out.println("Tileset: " + tile_set + " Tile: "
                        // + tile_tile + " Palette: " + tile_pal);
                        this.loadTileImage(tile_set, tile_tile, tile_pal);
                        /*
                         * g.drawImage(TileEditor.tilesets[mapcontrol
                         * .getDrawTileset(tsetPal[0])] .getArrangementImage(
                         * row2draw[i2] | (mapcontrol .getLocalTileset(i2 + x, i +
                         * y) < < 8), TileEditor.tilesets[mapcontrol
                         * .getDrawTileset(tsetPal[0])].getPaletteNum(
                         * tsetPal[0], tsetPal[1]), false), this.startx + (i2 *
                         * this.tilewidth), this.starty + (i * this.tileheight),
                         * this.tilewidth, this.tileheight, this);
                         */
                        g.drawImage(
                            this.tile_images[tile_set][tile_tile][tile_pal],
                            this.startx + (i2 * this.tilewidth), this.starty
                                + (i * this.tileheight), this.tilewidth,
                            this.tileheight, this);

                        g2d.draw(new Rectangle2D.Double(this.startx
                            + (i2 * this.tilewidth), this.starty
                            + (i * this.tileheight), tilewidth, tileheight));
                    }
                }

            }

            if ((this.modeprops[this.mode][0] >= 1) && this.knowssector)
            {
                g2d.setPaint(Color.yellow);

                /*
                 * int draw_sector_x, draw_sector_y, draw_sector_w,
                 * draw_sector_h;
                 * 
                 * if (this.sectorx == 0) { draw_sector_x = this.startx;
                 * draw_sector_w = (this.sector_width - this.x) *
                 * this.tilewidth; } else { draw_sector_x = this.startx +
                 * (this.sectorx * this.tilewidth * this.sector_width) -
                 * (this.tilewidth * this.x); if ((draw_sector_x +
                 * (this.sector_width * this.tilewidth)) > (this.width *
                 * this.tilewidth)) { System.out.println("The sector being drawn
                 * DOES extend horizontally past the border.");
                 * System.out.println("Expected end of sector: " +
                 * (draw_sector_x + (this.sector_width * this.tilewidth)) + ";
                 * Width of screen: " + (this.width * this.tilewidth) + ";");
                 * draw_sector_w = (this.sector_width * this.tilewidth) -
                 * ((draw_sector_x + (this.sector_width * this.tilewidth)) -
                 * (this.width * this.tilewidth)); System.out.println("New width
                 * of sector: " + draw_sector_w + ";"); } else {
                 * System.out.println("The sector being drawn DOES NOT extend
                 * horizontally past the border."); draw_sector_w =
                 * this.sector_width * this.tilewidth; } }
                 */

                /*
                 * g2d.draw(new Rectangle2D.Double(this.startx + (this.sectorx *
                 * this.tilewidth * this.sector_width), this.starty +
                 * (this.sectory * this.tileheight * this.sector_height),
                 * this.tilewidth * sectorDisplayWoH(this.sectorx,
                 * this.sector_width, this.width), this.tileheight *
                 * sectorDisplayWoH(this.sectory, this.sector_height,
                 * this.height)));
                 */

                int[] draw_sector_xw = sectorDisplayWoH(this.sectorx,
                    this.sector_width, this.width, this.startx, this.x,
                    this.tilewidth);
                int[] draw_sector_yh = sectorDisplayWoH(this.sectory,
                    this.sector_height, this.height, this.starty, this.y,
                    this.tileheight);

                g2d.draw(new Rectangle2D.Double(draw_sector_xw[0],
                    draw_sector_yh[0], draw_sector_xw[1], draw_sector_yh[1]));
            }

            if (this.modeprops[this.mode][2] == 1)
            {
                this.drawEditGUI(g, g2d);
            }
        }

        protected int[] sectorDisplayWoH(int sectorxory, int sector_woh,
            int woh, int startxory, int xory, int tilewoh)
        {
            /*
             * if (((sectorxory + 1) * sector_woh) > woh) {
             * System.out.println("sectorDisplayWoH case 1: new woh: " +
             * (sector_woh - (((sectorxory + 1) * sector_woh) - woh))); return
             * sector_woh - (((sectorxory + 1) * sector_woh) - woh); } else {
             * System.out.println("sectorDisplayWoH case 2: new woh: " +
             * sector_woh); return sector_woh; }
             */

            int draw_sector_xoy, draw_sector_woh;

            if (sectorxory == 0)
            {
                draw_sector_xoy = startxory;
                draw_sector_woh = (sector_woh - xory) * tilewoh;
            }
            else
            {
                draw_sector_xoy = startxory
                    + (sectorxory * tilewoh * sector_woh) - (tilewoh * xory);
                if ((draw_sector_xoy + (sector_woh * tilewoh)) > (woh * tilewoh))
                {
                    //System.out
                    //    .println("The sector being drawn DOES extend horizontally
                    // past the border.");
                    //System.out.println("Expected end of sector: "
                    //    + (draw_sector_xoy + (sector_woh * tilewoh))
                    //    + "; Width of screen: " + (woh * tilewoh) + ";");
                    draw_sector_woh = (sector_woh * tilewoh)
                        - ((draw_sector_xoy + (sector_woh * tilewoh)) - (woh * tilewoh));
                    //System.out.println("New width of sector: "
                    //    + draw_sector_woh + ";");
                }
                else
                {
                    //System.out
                    //    .println("The sector being drawn DOES NOT extend
                    // horizontally past the border.");
                    draw_sector_woh = sector_woh * tilewoh;
                }
            }

            return new int[]{draw_sector_xoy, draw_sector_woh};
        }

        /*
         * protected void drawBorder(Graphics2D g2d) {
         * g2d.setPaint(Color.black); Rectangle2D.Double border = new
         * Rectangle2D.Double(this.startx - 1, this.starty - 1, (this.width *
         * this.tilewidth) + 1, (this.height * this.tileheight) + 1);
         * g2d.draw(border); }
         */

        public void setSector(int newsectorx, int newsectory)
        {
            // System.out.println("New Sector: " + newsectorx + "x" +
            // newsectory);
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

        public void loadTileImage(int loadtset, int loadtile, int loadpalette)
        {
            // if (!((this.tile_images == null) &&
            // (this.tile_images[loadtset][loadtile][loadpalette] == null)))
            if (this.tile_images[loadtset][loadtile][loadpalette] == null)
            {
                // System.out.println("Now loading tile " + loadtset + "/" +
                // loadtile + "/" + loadpalette + " and there are " +
                // TILESET_NAMES.length + " unique tilesets");
                // load tile images into a local array tile_images
                this.tile_images[loadtset][loadtile][loadpalette] = TileEditor.tilesets[loadtset]
                    .getArrangementImage(loadtile, loadpalette);
            }
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

        public void setx(int newx)
        {
            this.x = newx;
        }

        public void sety(int newy)
        {
            this.y = newy;
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

        private int gui_x;

        private void drawEditGUI(Graphics g, Graphics2D g2d)
        {
            // System.out.println("Now drawing the Editing UI");
            if (this.knowssector)
            {
                int[] tsetPal = mapcontrol.getTsetPal(sectorx, sectory);
                int tile_set = mapcontrol.getDrawTileset(tsetPal[0]);
                int tile_pal = TileEditor.tilesets[mapcontrol
                    .getDrawTileset(tsetPal[0])].getPaletteNum(tsetPal[0],
                    tsetPal[1]);
                int tile_tile;

                g2d.setPaint(Color.black);
                for (int i = 0; i < this.width; i++)
                {
                    for (int i2 = 0; i2 < 2; i2++)
                    {
                        tile_tile = (gui_x + i) * (i2 + 1)
                            | (mapcontrol.getLocalTileset(i2 + x, i + y) << 8);
                        gfxcontrol.loadTileImage(tile_set, tile_tile, tile_pal);
                        g.drawImage(
                            this.tile_images[tile_set][tile_tile][tile_pal],
                            this.starteditx + (i * this.tilewidth),
                            this.startedity + ((i2 + 1) * this.tileheight),
                            this.tilewidth, this.tileheight, this);
                        g2d.draw(new Rectangle2D.Double(this.starteditx
                            + (i * this.tilewidth), this.startedity
                            + ((i2 + 1) * this.tileheight), tilewidth,
                            tileheight));
                    }
                }
            }
            else
            {
                g2d.setPaint(Color.black);
                for (int i = 0; i < this.width; i++)
                {
                    for (int i2 = 0; i2 < 2; i2++)
                    {
                        g2d.draw(new Rectangle2D.Double(this.starteditx
                            + (i * this.tilewidth), this.startedity
                            + ((i2 + 1) * this.tileheight), tilewidth,
                            tileheight));
                    }
                }
            }
        }
    }

    // Represents the whole EarthBound map and map-related data in the rom.
    public static class EbMap
    {
        private HackModule hm;
        private int width;
        private int height;
        private int sector_width;
        private int sector_height;
        // this.all_addresses = new int[] { 0x160200, 0x162A00, 0x165200,
        // 0x168200, 0x16AA00, 0x16D200, 0x170200, 0x172A00 };
        private final static int[] all_addresses = new int[]{
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
        private final static int tsetpal_address = 0x17AA00;
        private final static int tsettbl_address = 0x2F121B;
        private final static int localtset_address = 0x175200;

        private Rom rom;

        public EbMap(HackModule hm, int newwidth, int newheight,
            int newsector_width, int newsector_height)
        {
            this.hm = hm;
            this.rom = hm.rom;
            this.width = newwidth;
            this.height = newheight;
            this.sector_width = newsector_width;
            this.sector_height = newsector_height;
        }

        public int[] getTiles(int row, int start, int length)
        {
            int address = EbMap.all_addresses[row] + start;
            // this.rom.seek(this.address);

            int[] output = new int[length];
            for (int i = 0; i < output.length; i++)
            {
                int read_byte = rom.read(address + i);
                if (read_byte == -1)
                {
                    // System.out.println("Error in getTiles at address "
                    //    + address);
                }
                else
                {
                    output[i] = read_byte;
                }
            }
            return output;
        }

        public int[] getTsetPal(int sectorx, int sectory)
        {
            int address = EbMap.tsetpal_address
                + (sectory * ((width + 1) / sector_width)) + sectorx;
            /*
             * String tsetpal_data =
             * Integer.toBinaryString(rom.read(this.address));
             * System.out.println("tstpal_data:" + tsetpal_data + " Address: " +
             * Integer.toHexString(this.address)); int tileset =
             * Integer.parseInt(tsetpal_data.substring(0, 5), 2); int palette =
             * Integer.parseInt(tsetpal_data.substring(6, 8), 2);
             * System.out.println("tsetpal_data: " + tsetpal_data + " tileset: " +
             * tileset + " palette: " + palette);
             */
            int tsetpal_data = rom.read(address);
            int palette = tsetpal_data & 0x7;
            int tileset = (tsetpal_data & 0xf8) >> 3;
            //System.out.println("palette: " + Integer.toBinaryString(palette)
            // + " tileset: " + Integer.toBinaryString(tileset) + ";");
            int[] tsetpal = new int[]{tileset, palette};
            return tsetpal;
        }

        public int getDrawTileset(int mapTset)
        {
            if ((mapTset > map_tsets) || (mapTset < 0))
            {
                return -1;
            }
            int address = EbMap.tsettbl_address + (mapTset * 2);
            // System.out.println(Integer.toHexString(this.address));
            int drawTset = rom.read(address);
            return drawTset;
            //return 0;
        }

        public int getLocalTileset(int gltx, int glty)
        {
            int address = EbMap.localtset_address
                + ((glty / 8) * (this.width + 1)) + gltx;
            if (((glty / 4) % 2) == 1)
                address += 0x3000;
            //            System.out.println("address: " + Integer.toHexString(address)
            //                + " glty: " + glty + " glty/4*256 " + ((glty / 4) * 256)
            //                + " gltx: " + gltx);
            int local_tset = (rom.read(address) >> ((glty % 4) * 2)) & 3;

            return local_tset;
            //return 3;
        }

        /*
         * public Image getTileImage(int tset, int tile, int pallete) { //
         * TileEditor.Tileset tile_data = new
         * TileEditor.Tileset(MAP_TILESETS[tset],
         * TILESET_NAMES[MAP_TILESETS[tset]], this.hm); TileEditor.Tileset
         * tset_class = TileEditor.tilesets[MAP_TILESETS[tset]]; return
         * tset_class.getArrangementImage(tile, pallete, false); }
         */

    }
}