/*
 * Created on Aug 9, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class CreditsFontEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public CreditsFontEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    /** Location of ASM point to credits font graphics. */
    public static final int CREDITS_FONT_POINTER = 0x04f3a7;
    /** Number of tiles in the credits font. */
    public static final int NUM_CREDITS_CHARS = 192;
    public static final int NUM_SUBPALS = 2;
    public static final int NUM_SUBPAL_COLS = 4;
    public static byte[][][] tiles;
    public static Color[][] pal = new Color[NUM_SUBPALS][NUM_SUBPAL_COLS];

    private static BitSet subPalNums = null;

    private static void readSubPalNums()
    {
        if (subPalNums == null)
        {
            subPalNums = new BitSet(NUM_CREDITS_CHARS);
            try
            {
                InputStream in = CreditsFontEditor.class
                    .getResourceAsStream("creditFPals.dat");
                int i = 0;
                while (i < NUM_CREDITS_CHARS)
                {
                    int r = in.read();
                    for (int j = 0; j < 8; j++)
                    {
                        if (((r >> j) & 1) != 0)
                            subPalNums.set(i);
                        i++;
                    }
                }
                in.close();
            }
            catch (IOException e)
            {
                System.err.println("Error reading subpalette numbers file "
                    + "(creditFPals.dat).");
                e.printStackTrace();
            }
        }
    }

    /*
     * private static void writeSubPalNums() { try { OutputStream out = new
     * FileOutputStream("src" + File.separator + DEFAULT_BASE_DIR +
     * "creditFPals.dat"); int i = 0; while (i < NUM_CREDITS_CHARS) { int r = 0;
     * for (int j = 0; j < 8; j++) { if (subPalNums.get(i)) r |= 1 << j; i++; }
     * out.write(r); } out.close(); } catch (IOException e) {
     * System.err.println("Error writing subpalette numbers file " +
     * "(creditFPals.dat)."); e.printStackTrace(); } }
     */

    public static int oldPointer, oldLen;

    /**
     * Reads the credits font graphics from the ROM into {@link #tiles}.
     * 
     * @param rom AbstractRom being edited to read from. Only used if readOrg is
     *            false.
     * @param readOrg If true, reading is done from the orginal ROM instead of
     *            <code>rom</code>
     * @return Positive means success. On failure user is presented with option
     *         between abort (negative return value), retry (return value of
     *         trying this method again), fail (return value of 0),
     */
    private static int readGraphics(AbstractRom rom, boolean readOrg)
    {
        tiles = new byte[NUM_CREDITS_CHARS][8][8];
        byte[] buffer = new byte[NUM_CREDITS_CHARS * 32];
        int[] tmp;
        AbstractRom r = readOrg ? JHack.main
            .getOrginalRomFile(rom.getRomType()) : rom;
        int address = oldPointer = r.readRegAsmPointer(CREDITS_FONT_POINTER);
        System.out.println("Reading from address: 0x"
            + Integer.toHexString(address) + " (" + address + ")");
        tmp = EbHackModule.decomp(address, buffer, r);
        if (tmp[0] < 0)
        {
            System.out.println("Error #" + tmp[0]
                + " decompressing credits font.");
            Object opt = JOptionPane.showInputDialog(null, "Error " + tmp[0]
                + " decompressing the credits font.", "Decompression Error",
                JOptionPane.ERROR_MESSAGE, null, new String[]{"Abort", "Retry",
                    "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                return tmp[0];
            }
            else if (opt.equals("Retry"))
            {
                return readGraphics(rom, readOrg);
            }
            else if (opt.equals("Fail"))
            {
                for (int j = 0; j < NUM_CREDITS_CHARS; j++)
                    for (int x = 0; x < 8; x++)
                        Arrays.fill(tiles[j][x], (byte) 0);
                System.out.println("CreditsFontEditor: fail: zeroed tiles");
                return 0;
            }
            return tmp[0];
        }
        System.out.println("Decompressed " + tmp[0] + " bytes ("
            + (tmp[0] / 16) + " tiles) from a " + tmp[1]
            + " byte compressed block.");
        int off = 0;
        for (int j = 0; j < NUM_CREDITS_CHARS * 16; j += 16)
        {
            read2BPPArea(tiles[off++], buffer, j, 0, 0);
        }
        return tmp[1];
    }

    public static boolean readFromRom(AbstractRom rom)
    {
        readSubPalNums();

        oldLen = readGraphics(rom, false);
        rom.seek(0x21EB14);
        for (int s = 0; s < NUM_SUBPALS; s++)
            rom.readPaletteSeek(pal[s]);

        return true;
    }

    private boolean readFromRom()
    {
        return readFromRom(rom);
    }

    private static int writeGraphics(EbHackModule hm)
    {
        if (oldLen < 0)
            return oldLen;
        byte[] buffer = new byte[8192]; // new byte[num * 16];
        int offset = 0;
        for (int i = 0; i < NUM_CREDITS_CHARS; i++)
            offset += HackModule.write2BPPArea(tiles[i], buffer, offset, 0, 0);
        byte[] comp = new byte[8192];
        int tmp = comp(buffer, comp, NUM_CREDITS_CHARS * 16);
        if (tmp < 0)
        {
            System.out.println("Error " + tmp + " compressing credits font.");
            return tmp;
        }
        hm.writeToFreeASMLink(comp, CREDITS_FONT_POINTER, oldLen, tmp);
        System.out.println("CreditsFontEditor.writeGraphics(): compressed "
            + NUM_CREDITS_CHARS + " tiles from " + (NUM_CREDITS_CHARS * 16)
            + " bytes to " + tmp + " bytes.");
        return tmp;
    }

    public static boolean writeInfo(EbHackModule hm)
    {
        writeGraphics(hm);
        hm.rom.seek(0x21EB14);
        for (int s = 0; s < NUM_SUBPALS; s++)
            hm.rom.writePaletteSeek(pal[s]);

        return false;
    }

    private boolean guiInited = false;

    private class CredTileSelector extends DoubleSelTileSelector
    {
        public void mousePressed(MouseEvent me)
        {
            if (me.getButton() == MouseEvent.BUTTON1)
                super.mousePressed(me);
        }

        public void mouseClicked(MouseEvent me)
        {
            super.mouseClicked(me);
            if (me.getButton() == MouseEvent.BUTTON3)
            {
                subPalNums.flip(getCurrentTile());
                repaintCurrent();
            }
            else if (me.getButton() == MouseEvent.BUTTON2)
            {
                subPalNums.flip(0, 192);
                repaint();
            }
            else
            {
                return;
            }
        }

        public int getTilesWide()
        {
            return 16;
        }

        public int getTilesHigh()
        {
            return 12;
        }

        public int getTileSize()
        {
            return 8;
        }

        public int getZoom()
        {
            return 2;
        }

        protected boolean isDrawGridLines()
        {
            return false;
        }

        public int getTileCount()
        {
            return NUM_CREDITS_CHARS;
        }

        public Image getTileImage(int tile)
        {
            return drawImage(tiles[tile], pal[getSubPalNum(tile)]);
        }

        protected boolean isGuiInited()
        {
            return guiInited;
        }

        protected int canDoubleSelect(int tile)
        {
            if (getSubPalNum(tile) == 0)
            {
                setDoubleSel(false);
            }
            else
            {
                try
                {
                    // if tile row number is even and has the same palette
                    // as
                    // the tile below it
                    if (tile + getTilesWide() < getTileCount()
                        && (tile / getTilesWide()) % 2 == 0
                        && getSubPalNum(tile) == getSubPalNum(tile
                            + getTilesWide()))
                        setDoubleSel(true);
                    else if (tile - getTilesWide() >= 0
                        && getSubPalNum(tile) == getSubPalNum(tile
                            - getTilesWide()))
                    {
                        tile -= getTilesWide();
                        setDoubleSel(true);
                    }
                    else
                        setDoubleSel(false);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    setDoubleSel(false);
                }
            }
            return tile;
        }

        public int getSubPalNum(int tile)
        {
            return subPalNums.get(tile) ? 1 : 0;
        }

        public static final String DOUBLE_SEL_PREF = "eb_credits_font_editor.allow_2x_sel";

        public String getDoubleSelPrefName()
        {
            return DOUBLE_SEL_PREF;
        }
    }

    private DoubleSelTileSelector tileSel;
    private IntArrDrawingArea da;
    private SpritePalette sp;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');
        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'y', "ctrl S",
            "apply", this));
        fileMenu.addSeparator();
        JMenuItem im, ex;
        fileMenu.add(im = HackModule.createJMenuItem("Import...", 'i', null,
            "import", this));
        im.setEnabled(false);
        fileMenu.add(ex = HackModule.createJMenuItem("Export...", 'e', null,
            "export", this));
        ex.setEnabled(false);
        mb.add(fileMenu);

        mb.add(createEditMenu(this, true));

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Allow double select", JHack.main
            .getPrefs(), CredTileSelector.DOUBLE_SEL_PREF, true, 'a', "alt D",
            "2xSelect", this));
        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        JPanel main = new JPanel(new BorderLayout());
        main.add(tileSel = new CredTileSelector(), BorderLayout.WEST);
        tileSel.setActionCommand("creditsTileSel");
        tileSel.addActionListener(this);

        DrawingToolset dt = new DrawingToolset(this);
        main.add(dt, BorderLayout.EAST);

        Box center = new Box(BoxLayout.Y_AXIS);
        sp = new SpritePalette(4, true);
        sp.setActionCommand("credPalEdit");
        sp.addActionListener(this);
        center.add(da = new IntArrDrawingArea(dt, sp, this));
        da.setActionCommand("credDA");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(80, 160));
        center.add(Box.createVerticalStrut(5));
        center.add(sp);
        main.add(center, BorderLayout.CENTER);

        mainWindow.getContentPane().add(main, BorderLayout.CENTER);

        guiInited = true;
        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Credits Font Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public void show()
    {
        int currTile = tileSel == null ? 0 : tileSel.getCurrentTile();
        readFromRom();
        super.show();

        mainWindow.setVisible(true);
        tileSel.setCurrentTile(currTile, true);
    }

    private boolean doubleSelInit = false;

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("creditsTileSel"))
        {
            int c = tileSel.getCurrentTile();
            if (tileSel.isDoubleSel())
            {
                byte[][] gr = new byte[8][16];
                int d = c + tileSel.getTilesWide();
                for (int x = 0; x < 8; x++)
                {
                    for (int y = 0; y < 8; y++)
                    {
                        gr[x][y] = tiles[c][x][y];
                        gr[x][y + 8] = tiles[d][x][y];
                    }
                }
                da.setImage(gr);
                if (!doubleSelInit)
                {
                    da.getParent().doLayout();
                    mainWindow.pack();
                    doubleSelInit = true;
                }
            }
            else
            {
                da.setImage(tiles[c]);
            }

            updatePalette();
        }
        else if (ae.getActionCommand().equals("credPalEdit"))
        {
            getSelectedSubPal()[sp.getSelectedColorIndex()] = sp.getNewColor();
            updatePalette();
            da.repaint();
            tileSel.repaint();
        }
        else if (ae.getActionCommand().equals("credDA"))
        {
            byte[][] gr = da.getByteArrImage();
            if (tileSel.isDoubleSel())
            {
                int c = tileSel.getCurrentTile(), d = c
                    + tileSel.getTilesWide();
                for (int x = 0; x < 8; x++)
                {
                    for (int y = 0; y < 8; y++)
                    {
                        tiles[c][x][y] = gr[x][y];
                        tiles[d][x][y] = gr[x][y + 8];
                    }
                }
            }
            else
            {
                setTile(tileSel.getCurrentTile(), da.getByteArrImage());
            }
            tileSel.repaintCurrent();
        }
        else if (ae.getActionCommand().equals("2xSelect"))
        {
            tileSel.setCurrentTile(tileSel.getCurrentTile(), true);
        }
        // edit menu
        else if (ae.getActionCommand().equals("undo"))
        {
            da.undo();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("cut"))
        {
            da.cut();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            da.copy();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            da.paste();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            da.delete();
            da.repaint();
        }
        // flipping
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            writeInfo(this);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    private void setTile(int i, byte[][] tile)
    {
        tiles[i] = tile;
    }

    /*
     * private byte[][] getTile(int i) { return tiles[i]; }
     */

    /*
     * private byte[][] getSelectedTile() { return
     * getTile(tileSel.getCurrentTile()); }
     */

    private int getCurrentSubPal()
    {
        return tileSel.getSubPalNum(tileSel.getCurrentTile());
    }

    private Color[] getSelectedSubPal()
    {
        return pal[getCurrentSubPal()];
    }

    private void updatePalette()
    {
        sp.setPalette(getSelectedSubPal());
        sp.repaint();
    }
}