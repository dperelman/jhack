/*
 * Created on Mar 20, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.BMPReader;
import net.starmen.pkhack.ByteArrHasher;
import net.starmen.pkhack.CheckNode;
import net.starmen.pkhack.CheckRenderer;
import net.starmen.pkhack.CopyAndPaster;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.NodeSelectionListener;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.Undoable;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author n42
 */
public class GasStationEditor extends EbHackModule implements ActionListener
{
    public static final int NUM_GAS_STATIONS = 1;

    public GasStationEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        try
        {
            Class[] c = new Class[]{byte[].class, GasStationEditor.class};
            IPSDatabase.registerExtension("gas", GasStationEditor.class
                .getMethod("importData", c), GasStationEditor.class.getMethod(
                "restoreData", c), GasStationEditor.class.getMethod(
                "checkData", c), this);
        }
        catch (SecurityException e)
        {
            // no security model, shouldn't have to worry about this
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            // spelling mistake, maybe? ^_^;
            e.printStackTrace();
        }
    }

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Gas Station Editor";
    }

    public String getCredits()
    {
        return "Written by n42, based on AnyoneEB's gas editor";
    }

    public static class GasStation extends FullScreenGraphics
    {
        private EbHackModule hm;
        private int num, tilePointer, tileLen,
                palPointer[] = new int[NUM_PALETTES],
                palLen[] = new int[NUM_PALETTES], arngPointer, arngLen;

        /** Number of palettes. */
        public static final int NUM_PALETTES = 3;
        /**
         * Number of arrangements. Note that this is more than fits on the
         * Station, so the last 128 are unused.
         */
        public static final int NUM_ARRANGEMENTS = 896;
        /** Number of tiles. */
        public static final int NUM_TILES = 632;

        /** Pointers to ASM pointers * */
        public static final int[] tilePointerArray = new int[]{0x0F2F0};
        public static final int[] palPointerArray = new int[]{0x0F347, 0x0F5BA,
            0x0F5F0};
        public static final int[] arngPointerArray = new int[]{0x0F31B};

        public GasStation(EbHackModule hm)
        {
            this.hm = hm;
            this.num = 0;

            for (int i = 0; i < NUM_PALETTES; i++)
                palPointer[i] = hm.rom.readRegAsmPointer(palPointerArray[i]);
            tilePointer = hm.rom.readRegAsmPointer(tilePointerArray[0]);
            arngPointer = hm.rom.readRegAsmPointer(arngPointerArray[0]);
        }

        public int getNumSubPalettes()
        {
            return NUM_PALETTES;
        }

        public int getSubPaletteSize()
        {
            return 256;
        }

        public int getNumArrangements()
        {
            return NUM_ARRANGEMENTS;
        }

        public int getNumTiles()
        {
            return NUM_TILES;
        }

        private boolean readGraphics(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] tileBuffer = new byte[49153];

            /** * DECOMPRESS GRAPHICS ** */
            System.out.println("About to attempt decompressing "
                + tileBuffer.length + " bytes of Gas Station #" + num
                + " graphics.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(tilePointerArray[0]) : tilePointer,
                tileBuffer, r);
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing Gas Station #" + num + ".");
                if (allowFailure)
                {
                    // EMPTY TILES
                    for (int i = 0; i < tiles.length; i++)
                        for (int x = 0; x < tiles[i].length; x++)
                            Arrays.fill(tiles[i][x], (byte) 0);
                    tileLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                tileLen = tmp[1];
                System.out.println("Gas Station graphics: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int gfxOffset = 0;
                for (int i = 0; i < NUM_TILES; i++)
                {
                    gfxOffset += HackModule.read8BPPArea(tiles[i], tileBuffer,
                        gfxOffset, 0, 0);
                }
            }

            return true;
        }

        private boolean readPalettes(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            /** * DECOMPRESS PALETTE ** */
            for (int i = 0; i < NUM_PALETTES; i++)
            {
                byte[] palBuffer = new byte[512];
                System.out.println("About to attempt decompressing "
                    + palBuffer.length + " bytes of the Gas Station palette #"
                    + i + ".");
                int addr = readOrg
                    ? r.readRegAsmPointer(palPointerArray[i])
                    : palPointer[i];
                int[] tmp = EbHackModule.decomp(addr, palBuffer, r);
                if (tmp[0] < 0)
                {
                    System.out.println("Error " + tmp[0]
                        + " decompressing Gas Station #" + num + " palette.");
                    if (allowFailure)
                    { // EMPTY PALETTES
                        Arrays.fill(palette[i], Color.BLACK);
                        palLen[i] = 0;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    palLen[i] = tmp[1];
                    System.out.println("Gas Station palette #" + i
                        + ": Decompressed " + tmp[0] + " bytes from a "
                        + tmp[1] + " byte compressed block at "
                        + Integer.toHexString(addr) + ".");

                    HackModule.readPalette(palBuffer, 0, palette[i]);
                }
            }

            return true;
        }

        private boolean readArrangement(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] arngBuffer = new byte[2048];
            /** * DECOMPRESS ARRANGEMENT ** */
            System.out.println("About to attempt decompressing "
                + arngBuffer.length + " bytes of Gas Station #" + num
                + " arrangement.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(arngPointerArray[0]) : arngPointer,
                arngBuffer, r);
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing Gas Station #" + num + " palette.");
                if (allowFailure)
                { // EMPTY ARRANGEMENTS
                    Arrays.fill(arrangementList, (short) 0);
                    for (int x = 0; x < arrangement.length; x++)
                        Arrays.fill(arrangement[x], (short) 0);
                    arngLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                arngLen = tmp[1];
                System.out.println("Gas Station arrangement: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                for (int i = 0; i < NUM_ARRANGEMENTS; i++)
                    arrangementList[i] = (short) ((arngBuffer[i * 2] & 0xff) + ((arngBuffer[i * 2 + 1] & 0xff) << 8));

                int j = 0;
                for (int y = 0; y < arrangement[0].length; y++)
                    for (int x = 0; x < arrangement.length; x++)
                        arrangement[x][y] = arrangementList[j++];
            }
            return true;
        }

        /**
         * Decompresses information from ROM. allowFailure defaults to true, and
         * is set to false when "fail" is selected from the abort/retry/fail box
         * presented to the user when a problem is encountered while reading.
         * 
         * @param allowFailure if true, false will not be returned on failure,
         *            instead the failed item will be set to zeros and reading
         *            will continue
         * @return true if everything is read or if allowFailure is true, false
         *         if any decompression failed and allowFailure is false
         * @throws DecompressionException on bad compressed data
         */
        public boolean readInfo(boolean allowFailure)
        {
            if (isInited)
                return true;
            // short curcuit
            return isInited = readGraphics(allowFailure, false)
                && readPalettes(allowFailure, false)
                && readArrangement(allowFailure, false);
        }

        private boolean writeGraphics()
        {
            byte[] udataTiles = new byte[49153];
            int tileOff = 0;

            /* COMPRESS TILES */
            for (int i = 0; i < NUM_TILES; i++)
            {
                tileOff += HackModule.write8BPPArea(tiles[i], udataTiles,
                    tileOff, 0, 0);
            }
            System.out.println("Tile data converted to SNES format.");
            byte[] compTile;
            int tileCompLen = comp(udataTiles, compTile = new byte[50000]);
            if (!hm.writeToFreeASMLink(compTile, tilePointerArray[num],
                tileLen, tileCompLen))
                return false;
            System.out.println("Wrote "
                + (tileLen = tileCompLen)
                + " bytes of the Gas Station #"
                + num
                + " tiles at "
                + Integer.toHexString(tilePointer = hm.rom
                    .readRegAsmPointer(tilePointerArray[num])) + " to "
                + Integer.toHexString(tilePointer + tileCompLen - 1) + ".");
            return true;
        }

        private boolean writePalettes()
        {
            /* COMPRESS PALETTES */
            boolean same = hm.rom.readAsmPointer(palPointerArray[0]) == hm.rom
                .readAsmPointer(palPointerArray[2]);
            for (int i = 0; i < NUM_PALETTES; i++)
            {
                byte[] udataPal = new byte[512];
                HackModule.writePalette(udataPal, 0, palette[i]);

                byte[] compPal;
                int palCompLen = comp(udataPal, compPal = new byte[600], 512);
                if (!hm.writeToFreeASMLink(compPal, palPointerArray[i], (i == 2
                    && same ? 0 : palLen[i]), palCompLen, true))
                    return false;
                System.out
                    .println("Wrote "
                        + (palLen[i] = palCompLen)
                        + " bytes of the Gas Station #"
                        + i
                        + " palette at "
                        + Integer.toHexString(palPointer[i] = hm.rom
                            .readRegAsmPointer(palPointerArray[i])) + " to "
                        + Integer.toHexString(palPointer[i] + palCompLen - 1)
                        + ".");
            }
            return true;
        }

        private boolean writeArrangement()
        {
            byte[] udataArng = new byte[2048];
            int arngOff = 0;

            /* COMPRESS ARRANGEMENT */
            int j = 0;
            for (int y = 0; y < arrangement[0].length; y++)
                for (int x = 0; x < arrangement.length; x++)
                    arrangementList[j++] = arrangement[x][y];
            for (int i = 0; i < NUM_ARRANGEMENTS; i++)
            {
                udataArng[arngOff++] = (byte) (arrangementList[i] & 0xff);
                udataArng[arngOff++] = (byte) ((arrangementList[i] >> 8) & 0xff);
            }

            byte[] compArng;
            int arngCompLen = comp(udataArng, compArng = new byte[3000]);
            if (!hm.writeToFreeASMLink(compArng, arngPointerArray[num],
                arngLen, arngCompLen))
                return false;
            System.out.println("Wrote "
                + (arngLen = arngCompLen)
                + " bytes of the Gas Station #"
                + num
                + " arrangement at "
                + Integer.toHexString(arngPointer = hm.rom
                    .readRegAsmPointer(arngPointerArray[num])) + " to "
                + Integer.toHexString(arngPointer + arngCompLen - 1) + ".");
            return true;
        }

        public boolean writeInfo()
        {
            if (!isInited)
                return false;

            return writePalettes() && writeArrangement() && writeGraphics();
        }
    }

    public static final GasStation[] gasStations = new GasStation[NUM_GAS_STATIONS];

    public static void readFromRom(EbHackModule hm)
    {
        gasStations[0] = new GasStation(hm);
        inited = true;
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    private static boolean inited = false;

    public void reset()
    {
        inited = false;
    }

    private class GasStationArrangementEditor extends ArrangementEditor
    {
        public GasStationArrangementEditor()
        {
            super();
            // this.setPreferredSize(new Dimension(getTilesWide()
            // * (getTileSize() * getZoom()), getTilesHigh()
            // * (getTileSize() * getZoom())));
        }

        protected int getCurrentTile()
        {
            return tileSelector.getCurrentTile();
        }

        protected void setCurrentTile(int tile)
        {
            tileSelector.setCurrentTile(tile);
        }

        protected int getTilesWide()
        {
            return 32;
        }

        protected int getTilesHigh()
        {
            return 28;
        }

        protected int getTileSize()
        {
            return 8;
        }

        protected int getZoom()
        {
            return 2;
        }

        protected boolean isDrawGridLines()
        {
            try
            {
                return prefs
                    .getValueAsBoolean("eb.GasStationEditor.arrEditor.gridLines");
            }
            catch (NullPointerException e)
            {
                return JHack.main.getPrefs().getValueAsBoolean(
                    "eb.GasStationEditor.arrEditor.gridLines");
            }
        }

        protected boolean isEditable()
        {
            return true;
        }

        protected boolean isGuiInited()
        {
            return true;
        }

        protected int getCurrentSubPalette()
        {
            return 0;
        }

        protected short getArrangementData(int x, int y)
        {
            return getSelectedStation().getArrangementData(x, y);
        }

        protected short[][] getArrangementData()
        {
            return getSelectedStation().getArrangementData();
        }

        protected void setArrangementData(int x, int y, short data)
        {
            getSelectedStation().setArrangementData(x, y, data);
        }

        protected void setArrangementData(short[][] data)
        {
            getSelectedStation().setArrangementData(data);
        }

        protected Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            return getSelectedStation().getTileImage(tile,
                GasStationEditor.this.getCurrentSubPalette(), hFlip, vFlip);
        }
    }

    private class FocusIndicator extends AbstractButton implements
        FocusListener, MouseListener
    {
        // true = tile editor, false = arrangement editor
        private boolean focus = true;

        public Component getCurrentFocus()
        {
            return (focus ? (Component) da : (Component) arrangementEditor);
        }

        public Undoable getCurrentUndoable()
        {
            return (focus ? (Undoable) da : (Undoable) arrangementEditor);
        }

        public CopyAndPaster getCurrentCopyAndPaster()
        {
            return (focus
                ? (CopyAndPaster) da
                : (CopyAndPaster) arrangementEditor);
        }

        private void cycleFocus()
        {
            focus = !focus;
            repaint();
        }

        private void setFocus(Component c)
        {
            focus = c == da;
            repaint();
        }

        public void focusGained(FocusEvent fe)
        {
            System.out.println("FocusIndicator.focusGained(FocusEvent)");
            setFocus(fe.getComponent());
            repaint();
        }

        public void focusLost(FocusEvent arg0)
        {}

        public void mouseClicked(MouseEvent me)
        {
            cycleFocus();
        }

        public void mousePressed(MouseEvent arg0)
        {}

        public void mouseReleased(MouseEvent arg0)
        {}

        public void mouseEntered(MouseEvent arg0)
        {}

        public void mouseExited(MouseEvent arg0)
        {}

        public void paint(Graphics g)
        {
            int[] arrowX = new int[]{40, 10, 10, 00, 10, 10, 40};
            int[] arrowY = new int[]{22, 22, 15, 25, 35, 28, 28};

            if (focus) // switch X and Y for pointing up
                g.fillPolygon(arrowY, arrowX, 7);
            else
                g.fillPolygon(arrowX, arrowY, 7);
        }

        public FocusIndicator()
        {
            da.addFocusListener(this);
            arrangementEditor.addFocusListener(this);

            this.addMouseListener(this);

            this.setPreferredSize(new Dimension(50, 50));

            this
                .setToolTipText("This arrow points to which component will recive menu commands. "
                    + "Click to change.");
        }
    }

    private GasStationArrangementEditor arrangementEditor;
    private TileSelector tileSelector;
    private IntArrDrawingArea da;
    private SpritePalette pal;
    private DrawingToolset dt;
    private FocusIndicator fi;

    // private JComboBox mapSelector
    private JComboBox palSelector;

    // private JTextField name;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        // menu
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'y', "ctrl S",
            "apply", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import...", 'i', null,
            "import", this));
        fileMenu.add(HackModule.createJMenuItem("Export...", 'e', null,
            "export", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import Image...", 'm', null,
            "importImg", this));
        // fileMenu.add(HackModule.createJMenuItem("Export Image...", 'x', null,
        // "exportImg", this));

        mb.add(fileMenu);

        JMenu editMenu = HackModule.createEditMenu(this, true);

        editMenu.addSeparator();

        editMenu.add(HackModule.createJMenuItem("H-Flip", 'h', null, "hFlip",
            this));
        editMenu.add(HackModule.createJMenuItem("V-Flip", 'v', null, "vFlip",
            this));

        mb.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Enable Tile Selector Grid Lines",
            prefs, "eb.GasStationEditor.tileSelector.gridLines", false, 't',
            null, "tileSelGridLines", this));
        optionsMenu.add(new PrefsCheckBox(
            "Enable Arrangement Editor Grid Lines", prefs,
            "eb.GasStationEditor.arrEditor.gridLines", false, 'a', null,
            "arrEdGridLines", this));
        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        // components
        tileSelector = new TileSelector()
        {
            public int getTilesWide()
            {
                return 36;
            }

            public int getTilesHigh()
            {
                return 18;
            }

            public int getTileSize()
            {
                return 6;
            }

            public int getZoom()
            {
                return 2;
            }

            public boolean isDrawGridLines()
            {
                try
                {
                    return prefs
                        .getValueAsBoolean("eb.GasStationEditor.tileSelector.gridLines");
                }
                catch (NullPointerException e)
                {
                    return JHack.main.getPrefs().getValueAsBoolean(
                        "eb.GasStationEditor.tileSelector.gridLines");
                }
            }

            public int getTileCount()
            {
                return GasStation.NUM_TILES;
            }

            public Image getTileImage(int tile)
            {
                return getSelectedStation().getTileImage(tile,
                    getCurrentSubPalette());
            }

            protected boolean isGuiInited()
            {
                return true;
            }
        };
        tileSelector.setActionCommand("tileSelector");
        tileSelector.addActionListener(this);

        arrangementEditor = new GasStationArrangementEditor();
        arrangementEditor.setActionCommand("arrangementEditor");
        arrangementEditor.addActionListener(this);

        dt = new DrawingToolset(this);

        pal = new SpritePalette(256, 8, 16);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);

        da = new IntArrDrawingArea(dt, pal, this);
        da.setActionCommand("drawingArea");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(81, 81));

        // mapSelector = createComboBox(gasStationNames, this);
        // mapSelector.setActionCommand("mapSelector");

        // name = new JTextField(15);

        palSelector = createJComboBoxFromArray(new String[]{"Regular", "Flash",
            "Regular after 1st flash"}, false);
        palSelector.setSelectedIndex(0);
        palSelector.setActionCommand("palSelector");
        palSelector.addActionListener(this);

        fi = new FocusIndicator();

        JButton copyPal = new JButton("Copy Palette");
        copyPal.setActionCommand("copyPal");
        copyPal.addActionListener(this);

        JButton pastePal = new JButton("Paste Palette");
        pastePal.setActionCommand("pastePal");
        pastePal.addActionListener(this);

        Box center = new Box(BoxLayout.Y_AXIS);
        center.add(createFlowLayout(dt));
        center.add(Box.createVerticalStrut(10));
        center.add(getLabeledComponent("Palette: ", palSelector));
        center.add(Box.createVerticalStrut(10));
        center.add(createFlowLayout(da));
        center.add(Box.createVerticalStrut(5));
        center.add(createFlowLayout(pal));
        center.add(createFlowLayout(new JButton[]{copyPal, pastePal}));
        center.add(Box.createVerticalStrut(10));
        center.add(createFlowLayout(fi));
        center.add(Box.createVerticalGlue());

        JPanel display = new JPanel(new BorderLayout());
        display.add(pairComponents(center, null, false), BorderLayout.CENTER);
        display.add(pairComponents(tileSelector, arrangementEditor, false),
            BorderLayout.WEST);
        mainWindow.getContentPane().add(new JScrollPane(display),
            BorderLayout.CENTER);

        mainWindow.pack();
    }

    public void show()
    {
        super.show();

        readFromRom();
        if (doMapSelectAction())
            mainWindow.setVisible(true);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private boolean doMapSelectAction()
    {
        // name.setText(gasStationNames[getCurrentStation()]);
        if (!getSelectedStation().readInfo())
        {
            Object opt = JOptionPane.showInputDialog(mainWindow,
                "Error decompressing the Gas Station (#" + getCurrentStation()
                    + ").", "Decompression Error", JOptionPane.ERROR_MESSAGE,
                null, new String[]{"Abort", "Retry", "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                hide();
                return false;
            }
            else if (opt.equals("Retry"))
            {
                return doMapSelectAction();
            }
            else if (opt.equals("Fail"))
            {
                getSelectedStation().initToNull();
            }
        }
        updatePaletteDisplay();
        tileSelector.repaint();
        arrangementEditor.clearSelection();
        arrangementEditor.repaint();
        updateTileEditor();

        return true;
    }

    private Color[] palcb = null;

    private void copyPal()
    {
        palcb = new Color[256];
        System.arraycopy(pal.getPalette(), 0, palcb, 0, 256);
    }

    private void pastePal()
    {
        if (palcb != null)
        {
            Color[] paltmp = new Color[256];
            System.arraycopy(palcb, 0, paltmp, 0, 256);
            getSelectedStation().setSubPal(getCurrentSubPalette(), paltmp);

            updatePaletteDisplay();
            da.repaint();
            tileSelector.repaint();
            arrangementEditor.repaint();
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("drawingArea"))
        {
            getSelectedStation()
                .setTile(getCurrentTile(), da.getByteArrImage());
            tileSelector.repaintCurrent();
            arrangementEditor.repaintCurrentTile();
            fi.setFocus(da);
        }
        else if (ae.getActionCommand().equals("arrangementEditor"))
        {
            fi.setFocus(arrangementEditor);
        }
        else if (ae.getActionCommand().equals("tileSelector"))
        {
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("palSelector"))
        {
            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.repaint();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            getSelectedStation().setPaletteColor(pal.getSelectedColorIndex(),
                getCurrentSubPalette(), pal.getNewColor());
            updatePaletteDisplay();
            da.repaint();
            tileSelector.repaint();
            arrangementEditor.repaint();
        }
        // flip
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
        // edit menu
        // undo
        else if (ae.getActionCommand().equals("undo"))
        {
            fi.getCurrentUndoable().undo();
        }
        // copy&paste stuff
        else if (ae.getActionCommand().equals("cut"))
        {
            fi.getCurrentCopyAndPaster().cut();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            fi.getCurrentCopyAndPaster().copy();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            fi.getCurrentCopyAndPaster().paste();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            fi.getCurrentCopyAndPaster().delete();
        }
        else if (ae.getActionCommand().equals("copyPal"))
        {
            copyPal();
        }
        else if (ae.getActionCommand().equals("pastePal"))
        {
            pastePal();
        }
        else if (ae.getActionCommand().equals("tileSelGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("arrEdGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            arrangementEditor.invalidate();
            arrangementEditor.resetPreferredSize();
            arrangementEditor.validate();
            arrangementEditor.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("import"))
        {
            importData();

            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.clearSelection();
            arrangementEditor.repaint();
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("export"))
        {
            exportData();
        }
        else if (ae.getActionCommand().equals("importImg"))
        {
            FullScreenGraphicsImporter.importImg(getSelectedStation(),
                mainWindow, true);

            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.clearSelection();
            arrangementEditor.repaint();
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            // TownMap.writeInfo() will only write if the map has been inited
            for (int i = 0; i < gasStations.length; i++)
                gasStations[i].writeInfo();
            // int m = getCurrentStation();
            // gasStationNames[m] = name.getText();
            // notifyDataListeners(gasStationNames, this, m);
            // writeArray("gasStationNames.txt", false, gasStationNames);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else
        {
            System.err
                .println("GasStationEditor.actionPerformed: ERROR: unhandled "
                    + "action command: \"" + ae.getActionCommand() + "\"");
        }
    }

    private int getCurrentStation()
    {
        return 0;
    }

    private GasStation getSelectedStation()
    {
        return gasStations[getCurrentStation()];
    }

    private int getCurrentSubPalette()
    {
        return palSelector.getSelectedIndex();
    }

    private Color[] getSelectedSubPalette()
    {
        return getSelectedStation().getSubPal(getCurrentSubPalette());
    }

    private int getCurrentTile()
    {
        return tileSelector.getCurrentTile();
    }

    private void updatePaletteDisplay()
    {
        pal.setPalette(getSelectedSubPalette());
        pal.repaint();
    }

    private void updateTileEditor()
    {
        da.setImage(getSelectedStation().getTile(getCurrentTile()));
    }

    public static final int NODE_BASE = 0;
    public static final int NODE_TILES = 1;
    public static final int NODE_ARR = 2;
    public static final int NODE_PAL = 3;

    public static class GasImportData
    {
        public byte[][][] tiles;
        public short[] arrangement;
        public Color[][] palette;
    }

    public static final byte GAS_VERSION = 2;

    public static void exportData(File f, boolean[][] a)
    {
        // make a byte whichMaps. for each map if it is used set the bit at the
        // place equal to the map number to 1
        byte whichMaps = 0;
        for (int i = 0; i < a.length; i++)
            whichMaps |= (a[i][NODE_BASE] ? 1 : 0) << i;

        try
        {
            FileOutputStream out = new FileOutputStream(f);

            out.write(GAS_VERSION);
            for (int m = 0; m < a.length; m++)
            {
                if (a[m][NODE_BASE])
                {
                    // if writing this map...
                    // say what parts we will write, once again as a bit mask
                    byte whichParts = 0;
                    for (int i = 1; i < a[m].length; i++)
                        whichParts |= (a[m][i] ? 1 : 0) << (i - 1);
                    out.write(whichParts);
                    // write tiles?
                    if (a[m][NODE_TILES])
                    {
                        byte[] b = new byte[GasStation.NUM_TILES * 64];
                        int offset = 0;
                        for (int i = 0; i < GasStation.NUM_TILES; i++)
                            offset += write8BPPArea(gasStations[m].getTile(i),
                                b, offset, 0, 0);
                        out.write(b);
                    }
                    // write arrangements?
                    if (a[m][NODE_ARR])
                    {
                        short[] arr = gasStations[m].getArrangementArr();
                        byte[] barr = new byte[arr.length * 2];
                        int off = 0;
                        for (int i = 0; i < arr.length; i++)
                        {
                            barr[off++] = (byte) (arr[i] & 0xff);
                            barr[off++] = (byte) ((arr[i] >> 8) & 0xff);
                        }
                        out.write(barr);
                    }
                    // write palettes?
                    if (a[m][NODE_PAL])
                    {
                        for (int i = 0; i < GasStation.NUM_PALETTES; i++)
                        {
                            byte[] pal = new byte[256 * 2];
                            writePalette(pal, 0, gasStations[m].getSubPal(i));
                            out.write(pal);
                        }
                    }
                }
            }

            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error exporting Gas Station data to "
                    + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error exporting Gas Station data to "
                + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
    }

    public static GasImportData[] importData(InputStream in) throws IOException
    {
        GasImportData[] out = new GasImportData[gasStations.length];

        byte version = (byte) in.read();
        if (version > GAS_VERSION)
        {
            if (JOptionPane.showConfirmDialog(null,
                "GAS file version not supported." + "Try to load anyway?",
                "GAS Version " + version + " Not Supported",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
                return null;
        }
        for (int m = 0; m < gasStations.length; m++)
        {
            out[m] = new GasImportData();
            byte whichParts = (byte) in.read();
            // if tile bit set...
            if ((whichParts & 1) != 0)
            {
                byte[] b = new byte[GasStation.NUM_TILES * 64];
                in.read(b);

                int offset = 0;
                out[m].tiles = new byte[GasStation.NUM_TILES][8][8];
                for (int i = 0; i < GasStation.NUM_TILES; i++)
                    offset += read8BPPArea(out[m].tiles[i], b, offset, 0, 0);
            }
            // if arr bit set...
            if (((whichParts >> 1) & 1) != 0)
            {
                out[m].arrangement = new short[GasStation.NUM_ARRANGEMENTS];
                byte[] barr = new byte[out[m].arrangement.length * 2];
                in.read(barr);

                int off = 0;
                for (int i = 0; i < out[m].arrangement.length; i++)
                {
                    out[m].arrangement[i] = (short) ((barr[off++] & 0xff) + ((barr[off++] & 0xff) << 8));
                }
            }
            // if pal bit set...
            if (((whichParts >> 2) & 1) != 0)
            {
                out[m].palette = new Color[GasStation.NUM_PALETTES][256];
                for (int i = 0; i < GasStation.NUM_PALETTES; i++)
                {
                    if (version == 1 && i == 2)
                    {
                        out[m].palette[i] = out[m].palette[0];
                    }
                    else
                    {
                        byte[] pal = new byte[256 * 2];
                        in.read(pal);

                        readPalette(pal, 0, out[m].palette[i]);
                    }
                }
            }
        }

        in.close();

        return out;
    }

    public static GasImportData[] importData(File f)
    {
        try
        {
            return importData(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            System.err
                .println("File not found error importing Gas Station data from "
                    + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println("IO error importing Gas Station data from "
                + f.getAbsolutePath() + ".");
            e.printStackTrace();
        }
        return null;
    }

    public static GasImportData[] importData(byte[] b)
    {
        try
        {
            return importData(new ByteArrayInputStream(b));
        }
        catch (IOException e)
        {
            System.err.println("IO error importing Gas Station data from "
                + "byte array.");
            e.printStackTrace();
        }
        return null;
    }

    private boolean exportData()
    {
        boolean[][] a = showCheckList(null, "<html>"
            + "Select which items you wish to export." + "</html>",
            "Export What?");
        if (a == null)
            return false;

        File f = getFile(true, "gas", "Gas Station");
        if (f != null)
            exportData(f, a);
        return true;
    }

    private static boolean[][] showCheckList(boolean[][] in, String text,
        String title)
    {
        CheckNode[][] mapNodes = new CheckNode[NUM_GAS_STATIONS][4];
        if (in == null)
        {
            boolean[] tmp = new boolean[4];
            Arrays.fill(tmp, true);
            in = new boolean[NUM_GAS_STATIONS][4];
            Arrays.fill(in, tmp);
        }

        for (int i = 0; i < mapNodes.length; i++)
        {
            if (in[i][NODE_BASE])
            {
                mapNodes[i][NODE_BASE] = new CheckNode("Gas Station", true,
                    true);
                mapNodes[i][NODE_BASE]
                    .setSelectionMode(CheckNode.DIG_IN_SELECTION);
                if (in[i][NODE_TILES])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_TILES] = new CheckNode("Tiles",
                            false, true));
                if (in[i][NODE_ARR])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_ARR] = new CheckNode(
                            "Arrangement", false, true));
                if (in[i][NODE_PAL])
                    mapNodes[i][NODE_BASE]
                        .add(mapNodes[i][NODE_PAL] = new CheckNode("Palettes",
                            false, true));
            }
        }
        JTree checkTree = new JTree(mapNodes[0][NODE_BASE]);
        checkTree.setCellRenderer(new CheckRenderer());
        checkTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);
        checkTree.putClientProperty("JTree.lineStyle", "Angled");
        checkTree.addMouseListener(new NodeSelectionListener(checkTree));

        // if user clicked cancel, don't take action
        if (JOptionPane.showConfirmDialog(null, pairComponents(
            new JLabel(text), new JScrollPane(checkTree), false), title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.CANCEL_OPTION)
            return null;

        final boolean[][] a = new boolean[NUM_GAS_STATIONS][4];
        for (int m = 0; m < NUM_GAS_STATIONS; m++)
            for (int i = 0; i < 4; i++)
                a[m][i] = mapNodes[m][i] == null ? false : mapNodes[m][i]
                    .isSelected();

        return a;
    }

    private boolean importData()
    {
        File f = getFile(false, "gas", "Gas Station");
        GasImportData[] tmid;
        if (f == null || (tmid = importData(f)) == null)
            return false;
        return importData(tmid);
    }

    private boolean importData(GasImportData[] tmid)
    {
        boolean[][] in = new boolean[NUM_GAS_STATIONS][4];

        for (int i = 0; i < in.length; i++)
        {
            if (tmid[i] != null)
            {
                in[i][NODE_BASE] = true;
                if (tmid[i].tiles != null)
                    in[i][NODE_TILES] = true;
                if (tmid[i].arrangement != null)
                    in[i][NODE_ARR] = true;
                if (tmid[i].palette != null)
                    in[i][NODE_PAL] = true;
            }
        }

        boolean[][] a = showCheckList(in, "<html>"
            + "Select which items you wish to<br>"
            + "import. You will have a chance<br>"
            + "to select which Station you want to<br>"
            + "actually put the imported data." + "</html>", "Import What?");
        if (a == null)
            return false;

        for (int m = 0; m < NUM_GAS_STATIONS; m++)
        {
            if (a[m][NODE_BASE])
            {
                int n = 0;
                if (a[m][NODE_TILES])
                    for (int i = 0; i < GasStation.NUM_TILES; i++)
                        gasStations[n].setTile(i, tmid[m].tiles[i]);
                if (a[m][NODE_ARR])
                    gasStations[n].setArrangementArr(tmid[m].arrangement);
                if (a[m][NODE_PAL])
                    for (int p = 0; p < GasStation.NUM_PALETTES; p++)
                        for (int c = 0; c < 256; c++)
                            gasStations[n].setPaletteColor(c, p,
                                tmid[m].palette[p][c]);
            }
        }
        return true;
    }

    /**
     * Imports data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "applying" files with .gas
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param gse instance of <code>LogoScreenEditor</code> to call
     *            <code>importData()</code> on
     */
    public static boolean importData(byte[] b, GasStationEditor gse)
    {
        if (!inited)
            readFromRom(gse);
        for (int i = 0; i < NUM_GAS_STATIONS; i++)
            gasStations[i].readInfo();
        boolean out = gse.importData(importData(b));
        if (out)
        {
            if (gse.mainWindow != null)
            {
                gse.mainWindow.repaint();
                gse.updatePaletteDisplay();
                gse.tileSelector.repaint();
                gse.arrangementEditor.clearSelection();
                gse.arrangementEditor.repaint();
                gse.updateTileEditor();
            }
            for (int i = 0; i < gasStations.length; i++)
                gasStations[i].writeInfo();
        }
        return out;
    }

    private static boolean checkStation(GasImportData gid, int i)
    {
        gasStations[i].readInfo();
        if (gid.tiles != null)
        {
            // check tiles
            for (int t = 0; t < gid.tiles.length; t++)
                for (int x = 0; x < gid.tiles[t].length; x++)
                    if (!Arrays.equals(gid.tiles[t][x],
                        gasStations[i].tiles[t][x]))
                        return false;
        }
        if (gid.arrangement != null)
        {
            // check arrangement
            if (!Arrays.equals(gid.arrangement, gasStations[i]
                .getArrangementArr()))
                return false;
        }
        if (gid.palette != null)
        {
            // check palette
            for (int p = 0; p < gid.palette.length; p++)
                for (int c = 0; c < gid.palette[p].length; c++)
                    if (!gid.palette[p][c].equals(gasStations[i].palette[p][c]))
                        return false;
        }

        // nothing found wrong
        return true;
    }

    private static boolean checkStation(GasImportData gid)
    {
        for (int i = 0; i < NUM_GAS_STATIONS; i++)
            if (checkStation(gid, i))
                return true;
        return false;
    }

    /**
     * Checks if data from the given <code>byte[]</code> has been imported.
     * This method exists to be called by <code>IPSDatabase</code> for
     * "checking" files with .gas extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param gse instance of <code>GasStationEditor</code>
     */
    public static boolean checkData(byte[] b, GasStationEditor gse)
    {
        if (!inited)
            readFromRom(gse);
        GasImportData[] gid = importData(b);

        for (int i = 0; i < gid.length; i++)
            if (gid[i] != null)
                if (!checkStation(gid[i]))
                    return false;

        return true;
    }

    /**
     * Restore data from the given <code>byte[]</code> based on user input.
     * User input will always be expected by this method. This method exists to
     * be called by <code>IPSDatabase</code> for "unapplying" files with .gas
     * extensions.
     * 
     * @param b <code>byte[]</code> containing exported data
     * @param gse instance of <code>GasStationEditor</code>
     */
    public static boolean restoreData(byte[] b, GasStationEditor gse)
    {
        if (!inited)
            readFromRom(gse);
        for (int i = 0; i < NUM_GAS_STATIONS; i++)
            gasStations[i].readInfo();
        boolean[][] a = showCheckList(null, "<html>Select which items you wish"
            + "to restore to the orginal EarthBound verions.</html>",
            "Restore what?");
        if (a == null)
            return false;

        for (int i = 0; i < a.length; i++)
        {
            if (a[i][NODE_BASE])
            {
                if (a[i][NODE_TILES])
                {
                    gasStations[i].readGraphics(true, true);
                    gasStations[i].writeGraphics();
                }
                if (a[i][NODE_ARR])
                {
                    gasStations[i].readArrangement(true, true);
                    gasStations[i].writeArrangement();
                }
                if (a[i][NODE_PAL])
                {
                    gasStations[i].readPalettes(true, true);
                    gasStations[i].writePalettes();
                }
            }
        }

        if (gse.mainWindow != null)
        {
            gse.mainWindow.repaint();
            gse.updatePaletteDisplay();
            gse.tileSelector.repaint();
            gse.arrangementEditor.clearSelection();
            gse.arrangementEditor.repaint();
            gse.updateTileEditor();
        }

        return true;
    }
}