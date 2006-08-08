/*
 * Created on Feb 29, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.BMPReader;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class BattleSpriteEditor extends EbHackModule implements ActionListener,
    DocumentListener
{

    /**
     * @param rom
     * @param prefs
     */
    public BattleSpriteEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    /*
     * •0E64EE to 0E6713 = In-battle graphics pointer table •0E6714 to 0E6B13 =
     * In-battle graphics palettes
     * 
     * Five-byte entries; a pointer (4 bytes) and one other bytes of unknown
     * purpose Size byte | Image Dimensions (in pixels)
     * ------------------------------------------ 1 | 32 wide x 32 high 2 | 64
     * wide x 32 high 3 | 32 wide x 64 high 4 | 64 wide x 64 high 5 | 128 wide x
     * 64 high 6 | 128 wide x 128 high
     */
    public final static Dimension[] BATTLE_SPRITE_SIZES = new Dimension[]{null,
        new Dimension(32, 32), new Dimension(64, 32), new Dimension(32, 64),
        new Dimension(64, 64), new Dimension(128, 64), new Dimension(128, 128)};

    public static class BattleSprite
    {
        private EbHackModule hm;
        private int num, address, orgPointer, orgCompLen, size;
        private byte[][] sprite;
        private boolean isInited = false;

        public BattleSprite(int i, EbHackModule hm)
        {
            this.hm = hm;
            this.num = i;
            this.address = 0x0E64EE + (i * 5);

            AbstractRom rom = hm.rom;
            rom.seek(address);
            orgPointer = HackModule.toRegPointer(rom.readMultiSeek(4));
            size = rom.readSeek();
        }

        /**
         * Attempts to read battle sprite graphics and returns error code.
         * Negitive return value indicates failure. Non-negitive indicates
         * success.
         * 
         * @param allowFailure if true, will try to load even if decomp() fails
         * @return 0 on success or negitive error code from
         *         {@link EbHackModule#decomp(int, byte[])}on failure
         */
        public int readInfo(boolean allowFailure)
        {
            if (isInited)
                return 0;

            // System.out.println("Reading battle sprite #" + num + " from 0x"
            // + Integer.toHexString(orgPointer));

            Dimension d = BATTLE_SPRITE_SIZES[size];

            sprite = new byte[d.width][d.height];

            byte[] buffer = new byte[8192];
            int[] tmp = hm.decomp(orgPointer, buffer);
            if (tmp[0] < 0)
            {
                System.out.println("Error #" + tmp[0]
                    + " decompressing battle sprite " + num + " ("
                    + EbHackModule.battleSpriteNames[num] + ")");
                if (!allowFailure)
                {
                    return tmp[0];
                }
            }
            // System.out.println("Finished decompressing battle sprite #"
            // + num);
            orgCompLen = tmp[1];
            int offset = 0;
            for (int q = 0; q < (d.height / 32); q++)
            {
                for (int r = 0; r < (d.width / 32); r++)
                {
                    for (int a = 0; a < 4; a++)
                    {
                        for (int j = 0; j < 4; j++)
                        {
                            read4BPPArea(sprite, buffer, offset,
                                (j + r * 4) * 8, (a + q * 4) * 8);
                            offset += 32;
                        }
                    }
                }
            }
            isInited = true;
            // System.out.println("Finished reading battle sprite #" + num);
            if (tmp[0] < 0)
            {
                return tmp[0];
            }
            else
            {
                return 0;
            }
        }

        public void initToNull()
        {
            if (isInited)
                return;

            Dimension d = BATTLE_SPRITE_SIZES[size];

            sprite = new byte[d.width][d.height];
            for (int x = 0; x < d.width; x++)
                Arrays.fill(sprite[x], (byte) 0);
            isInited = true;
        }

        public boolean writeInfo()
        {
            // if not inited, don't write anything
            if (!isInited)
                return false;

            AbstractRom rom = hm.rom;
            // write size 4 bytes after the start, 4 byte pointer is first
            rom.write(address + 4, size);
            Dimension d = BATTLE_SPRITE_SIZES[size];

            byte[] udata = new byte[8192];
            int offset = 0;
            for (int q = 0; q < (d.height / 32); q++)
            {
                for (int r = 0; r < (d.width / 32); r++)
                {
                    for (int a = 0; a < 4; a++)
                    {
                        for (int j = 0; j < 4; j++)
                        {
                            HackModule.write4BPPArea(sprite, udata, offset,
                                (j + r * 4) * 8, (a + q * 4) * 8);
                            offset += 32;
                        }
                    }
                }
            }
            byte[] buffer = new byte[8192];
            int compLen = EbHackModule.comp(udata, buffer, d.width * d.height
                / 2);

            if (!hm.writeToFree(buffer, address, orgCompLen, compLen))
                return false;
            System.out.println("Wrote "
                + (orgCompLen = compLen)
                + " bytes of battle sprite #"
                + num
                + " at "
                + Integer.toHexString(this.orgPointer = toRegPointer(hm.rom
                    .readMulti(0x0E64EE + (num * 5), 4))) + " to "
                + Integer.toHexString(this.orgPointer + compLen - 1) + ".");
            return true;
        }

        /**
         * @return Returns the sprite.
         */
        public byte[][] getSprite()
        {
            readInfo(true);

            byte[][] out = new byte[sprite.length][sprite[0].length];
            for (int x = 0; x < sprite.length; x++)
                for (int y = 0; y < sprite[0].length; y++)
                    out[x][y] = sprite[x][y];
            return out;
        }

        /**
         * @param sprite The sprite to set.
         */
        public void setSprite(byte[][] sprite)
        {
            readInfo(true);

            // resize to incoming sprite
            for (int i = 1; i < BATTLE_SPRITE_SIZES.length; i++)
            {
                Dimension d = BATTLE_SPRITE_SIZES[i];
                if (d.width == sprite.length && d.height == sprite[0].length)
                    setSize(i);
            }

            for (int x = 0; x < sprite.length; x++)
                for (int y = 0; y < sprite[0].length; y++)
                    this.sprite[x][y] = sprite[x][y];
        }

        /**
         * Sets the sprite from an image using the specified palette.
         * 
         * @param img image to read sprite from
         * @param pal palette to read image with
         */
        public void setSprite(Image img, Color[] pal)
        {
            HackModule.convertImage(img, sprite, pal);
        }

        /**
         * @return Returns the num.
         */
        public int getNum()
        {
            return num;
        }

        /**
         * @return Returns the size.
         * @see BattleSpriteEditor#BATTLE_SPRITE_SIZES
         */
        public int getSize()
        {
            return size;
        }

        /**
         * Sets the size of the sprite. Resizes sprite image to agree.
         * 
         * @param size The size to set.
         * @see BattleSpriteEditor#BATTLE_SPRITE_SIZES
         */
        public void setSize(int size)
        {
            readInfo(true);

            this.size = size;

            Dimension d = BATTLE_SPRITE_SIZES[size];

            byte[][] newSprite = new byte[d.width][d.height];

            for (int x = 0; x < Math.min(sprite.length, newSprite.length); x++)
            {
                for (int y = 0; y < Math.min(sprite[0].length,
                    newSprite[0].length); y++)
                {
                    newSprite[x][y] = sprite[x][y];
                }
            }

            this.sprite = newSprite;
        }
    }

    public String getVersion()
    {
        return "0.2";
    }

    public String getDescription()
    {
        return "Battle Sprite Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Orginal Editor by Tomato";
    }

    public final static int NUM_ENTRIES = 110;
    public final static BattleSprite[] battleSprites = new BattleSprite[NUM_ENTRIES];
    public static Color[][] palettes;

    private static void readPalettes(AbstractRom rom)
    {
        palettes = new Color[256][16];
        rom.seek(0x0E6714);
        for (int pal = 0; pal < 256; pal++)
            rom.readPaletteSeek(palettes[pal]);
    }

    private static void writePalettes(AbstractRom rom)
    {
        rom.seek(0x0E6714);
        for (int pal = 0; pal < 32; pal++)
            rom.writePaletteSeek(palettes[pal]);
    }

    public static void readFromRom(EbHackModule hm)
    {
        readPalettes(hm.rom);
        for (int i = 0; i < NUM_ENTRIES; i++)
            battleSprites[i] = new BattleSprite(i, hm);
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    /**
     * Returns an image of specificed battle sprite in the specifed palette.
     * Note that this will fail if {@link #readFromRom(EbHackModule)}is not run
     * first.
     * 
     * @param num Which battle sprite to get the image of (0-109). Out of range
     *            values will return a 0x0 image.
     * @param pal Which palette to use (0-31).
     * @return a <code>Image</code> of the specified battle sprite or and
     *         empty image if either argument is invalid
     */
    public static Image getImage(int num, int pal)
    {
        if (num < 0 || num >= NUM_ENTRIES || pal < 0 || pal > 255)
            return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        return drawImage(battleSprites[num].getSprite(), palettes[pal]);
    }
    private JComboBox spriteSelector, sizeSelector, palSelector;
    private JTextField zoom, name;
    private IntArrDrawingArea da;
    private SpritePalette pal;
    private JScrollPane jsp;
    private JCheckBoxMenuItem gridLines;
    private int currPal;

    protected void init()
    {
        reset();

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());

        mainWindow.getContentPane().add(
            new JSearchableComboBox(spriteSelector = createComboBox(
                battleSpriteNames, true, this), "Battle Sprite: "),
            BorderLayout.NORTH);
        spriteSelector.setActionCommand("spriteSelector");

        JPanel entry = new JPanel(new BorderLayout());

        JPanel entryEast = new JPanel(new BorderLayout());

        Box entryNE = new Box(BoxLayout.Y_AXIS);

        name = new JTextField(25);
        entryNE.add(getLabeledComponent("Name: ", name));

        palSelector = createJComboBoxFromArray(new Object[palettes.length],
            false);
        palSelector.setSelectedIndex(0);
        palSelector.setActionCommand("palSelector");
        palSelector.addActionListener(this);
        palSelector.setEditable(true);
        entryNE.add(getLabeledComponent("Palette: ", palSelector,
            "Palettes 0-31 real palettes, rest are battle swirl data."));

        // sizeSelector = createJComboBoxFromArray(new String[]{"[01] 32x32",
        // "[02] 32x64", "[03] 64x32", "[04] 64x64", "[05] 128x64",
        // "[06] 128x128"});
        sizeSelector = createJComboBoxFromArray(new String[]{"[01] 32x32",
            "[02] 64x32", "[03] 32x64", "[04] 64x64", "[05] 128x64",
            "[06] 128x128"});
        sizeSelector.setActionCommand("sizeSelector");
        sizeSelector.addActionListener(this);
        entryNE.add(getLabeledComponent("Size: ", sizeSelector));

        entryNE.add(HackModule.pairComponents(getLabeledComponent("Zoom: ",
            pairComponents(zoom = new JTextField(4), new JLabel("%"), true)),
            new JLabel(), false));
        zoom.setText("1000");
        zoom.getDocument().addDocumentListener(this);

        entryEast.add(entryNE, BorderLayout.NORTH);

        DrawingToolset dt = new DrawingToolset(this);
        entryEast.add(dt, BorderLayout.CENTER);
        entry.add(entryEast, BorderLayout.EAST);

        pal = new SpritePalette(16);
        pal.setPalette(palettes[0]);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);
        JPanel spalWrapper = new JPanel(new FlowLayout());
        spalWrapper.add(pal);
        entry.add(spalWrapper, BorderLayout.SOUTH);

        da = new IntArrDrawingArea(dt, pal);
        da.setZoom(10);
        // default size for first battle sprite at default zoom (32x32@1000%)
        da.setPreferredSize(new Dimension(321, 321));
        entry.add(jsp = new JScrollPane(da), BorderLayout.CENTER);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);

        // menu
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'a', null,
            "apply", this));
        fileMenu.add(new JSeparator());
        fileMenu.add(HackModule.createJMenuItem("Import sprite from image...",
            'm', null, "importImgReal", this));
        fileMenu.add(HackModule.createJMenuItem("Export sprite as image...",
            'x', null, "exportImgReal", this));
        fileMenu.add(new JSeparator());
        fileMenu.add(HackModule.createJMenuItem("Import sprite...", 'i', null,
            "importImg", this));
        fileMenu.add(HackModule.createJMenuItem("Export sprite...", 'e', null,
            "exportImg", this));
        fileMenu.add(new JSeparator());
        fileMenu.add(HackModule.createJMenuItem("Import sprite from .bmp...",
            'b', null, "importBmpImg", this));

        mb.add(fileMenu);

        JMenu editMenu = HackModule.createEditMenu(this, true);

        editMenu.add(new JSeparator());

        editMenu.add(HackModule.createJMenuItem("H-Flip", 'h', null, "hFlip",
            this));
        editMenu.add(HackModule.createJMenuItem("V-Flip", 'v', null, "vFlip",
            this));

        mb.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');

        gridLines = new JCheckBoxMenuItem("Show gridLines");
        gridLines.setMnemonic('g');
        gridLines.setSelected(true);
        gridLines.setActionCommand("gridLines");
        gridLines.addActionListener(this);
        optionsMenu.add(gridLines);

        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        mainWindow.pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        readFromRom();
        EnemyEditor.readFromRom(this);
        super.show();

        spriteSelector.setSelectedIndex(spriteSelector.getSelectedIndex() == -1
            ? 0
            : spriteSelector.getSelectedIndex());

        mainWindow.setVisible(true);
    }

    public void show(Object in) throws IllegalArgumentException
    {
        super.show(in);

        if (in instanceof Integer)
        {
            int index = ((Integer) in).intValue() - 1;
            if (index >= 0 && index < NUM_ENTRIES)
                spriteSelector.setSelectedIndex(index);
        }
        else
            HackModule.search(in.toString(), spriteSelector, true);
        spriteSelector.repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void doSpriteSelectAction()
    {
        // don't even try if selector is -1
        if (getCurrentSprite() == -1)
            return;
        int err = getSelectedSprite().readInfo(false);
        if (err < 0)
        {
            int opt = JOptionPane.showOptionDialog(mainWindow, "Error #" + err
                + " decompressing the " + battleSpriteNames[getCurrentSprite()]
                + " sprite (#" + getCurrentSprite() + ").",
                "Decompression Error", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE, null, new String[]{"Abort", "Retry",
                    "Fail"}, "Retry");
            if (opt == JOptionPane.CLOSED_OPTION
                || opt == JOptionPane.YES_OPTION)
            {
                spriteSelector.setSelectedIndex((spriteSelector
                    .getSelectedIndex() + 1)
                    % spriteSelector.getItemCount());
                doSpriteSelectAction();
                return;
            }
            else if (opt == JOptionPane.NO_OPTION)
            {
                spriteSelector.setSelectedIndex(spriteSelector
                    .getSelectedIndex());
                doSpriteSelectAction();
                return;
            }
            else if (opt == JOptionPane.CANCEL_OPTION)
            {
                getSelectedSprite().readInfo(true);
            }
        }
        da.setImage(getSelectedSprite().getSprite());
        sizeSelector.setSelectedIndex(getSelectedSprite().getSize() - 1);
        sizeSelector.repaint();
        name.setText(battleSpriteNames[spriteSelector.getSelectedIndex()]);

        for (int i = 0; i < EnemyEditor.enemies.length; i++)
        {
            if (EnemyEditor.enemies[i].getInsidePic() - 1 == spriteSelector
                .getSelectedIndex())
            {
                palSelector.setSelectedIndex(EnemyEditor.enemies[i]
                    .getPalette());
                palSelector.repaint();
                break;
            }
        }

        jsp.updateUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(spriteSelector.getActionCommand()))
        {
            doSpriteSelectAction();
        }
        else if (ae.getActionCommand().equals(palSelector.getActionCommand()))
        {
            if (palSelector.getSelectedIndex() == -1)
            {
                try
                {
                    String numstr = palSelector.getSelectedItem().toString()
                        .trim().replaceAll("[^\\d]", "");
                    if (numstr.length() == 0)
                        palSelector.setSelectedIndex(currPal);
                    else
                        palSelector.setSelectedIndex(Integer.parseInt(numstr));
                }
                catch (NumberFormatException nfe)
                {
                    // we shouldn't get number format exceptions
                    // the regular expression should remove all
                    // non-digit characters
                    System.err
                        .println("This error cannot occur (BattleSpriteEditor"
                            + ".actionPerformed(palSelector) "
                            + "NumberFormatException).");
                    nfe.printStackTrace();
                    // set it back to a legal value
                    palSelector.setSelectedIndex(currPal);
                    return;
                }
                catch (IllegalArgumentException iae)
                {
                    // number out of range
                    // would mean number too high, negitive is prevented
                    // by the regular expression
                    // change the palette selector back to what it was
                    palSelector.setSelectedIndex(currPal);
                    // previous line should have caused a call to this method,
                    // no need to do it again
                    return;
                }
            }
            updatePalette();
            da.repaint();
        }
        else if (ae.getActionCommand().equals(sizeSelector.getActionCommand()))
        {
            if ((sizeSelector.getSelectedIndex() + 1) != getSelectedSprite()
                .getSize())
            {
                if (JOptionPane.showConfirmDialog(mainWindow,
                    "Are you sure you wish to change the size of this "
                        + "sprite to "
                        + (sizeSelector.getSelectedItem().toString().split(
                            "\\]")[1].trim()) + "? "
                        + "Sprite data outside that area "
                        + "will be permanently lost.", "Are you sure?",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION)
                {
                    sizeSelector
                        .setSelectedIndex(getSelectedSprite().getSize() - 1);
                    return;
                }
                getSelectedSprite()
                    .setSize(sizeSelector.getSelectedIndex() + 1);
                da.setImage(getSelectedSprite().getSprite());
                jsp.updateUI();
            }
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            // cache current sprite for when combobox gets changed
            int curr = getCurrentSprite();
            battleSprites[curr].setSprite(da.getByteArrImage());
            battleSprites[curr].writeInfo();

            writePalettes(rom);

            battleSpriteNames[curr] = name.getText();
            notifyDataListeners(battleSpriteNames, this, curr);
            writeArray("insideSprites.txt", false, battleSpriteNames);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
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
        else if (ae.getActionCommand().equals(gridLines.getActionCommand()))
        {
            da.setDrawGridlines(gridLines.isSelected());
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            if (getCurrentPalNum() < 32)
            {
                getCurrentPal()[pal.getSelectedColorIndex()] = pal
                    .getNewColor();
                updatePalette();
                da.repaint();
            }
            else
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "ERROR: You may not edit this palette!\n"
                        + "This palette is past #31, and therefore\n"
                        + "it is not a real palette, it is some part\n"
                        + "of the battle swirl data. Editing it would\n"
                        + "corrupt that data.",
                    "Error, Unable to Edit Palette", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (ae.getActionCommand().equals("importImgReal"))
        {
            importSprFromImg();
        }
        else if (ae.getActionCommand().equals("exportImgReal"))
        {
            exportSprAsImg();
        }
        else if (ae.getActionCommand().equals("importImg"))
        {
            importImg();
        }
        else if (ae.getActionCommand().equals("exportImg"))
        {
            exportImg();
        }
        else if (ae.getActionCommand().equals("importBmpImg"))
        {
            importBmpImg();
        }
    }

    private BattleSprite getSelectedSprite()
    {
        return battleSprites[getCurrentSprite()];
    }

    private int getCurrentSprite()
    {
        return spriteSelector.getSelectedIndex();
    }

    private void updatePalette()
    {
        pal.setPalette(getCurrentPal());
        pal.repaint();
    }

    private Color[] getCurrentPal()
    {
        return palettes[getCurrentPalNum()];
    }

    private int getCurrentPalNum()
    {
        // make sure nothing ever thinks palette is #-1
        int tmp = palSelector.getSelectedIndex();
        if (tmp != -1)
            currPal = tmp;
        return currPal;
    }

    private void updateZoom()
    {
        try
        {
            da.setZoom(Float.parseFloat(zoom.getText()) / 100);
            da.setImage(da.getByteArrImage());
            jsp.updateUI();
        }
        catch (NumberFormatException nfe)
        {}
    }

    public void changedUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    public void insertUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    public void removeUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    public void reset()
    {
        super.reset();
        readArray(DEFAULT_BASE_DIR, "insideSprites.txt", rom.getPath(), false,
            battleSpriteNames);
    }

    private final static Color[] BITMAP_PAL = new Color[]{new Color(0, 0, 0),
        new Color(128, 0, 0), new Color(0, 128, 0), new Color(128, 128, 0),
        new Color(0, 0, 128), new Color(128, 0, 128), new Color(0, 128, 128),
        new Color(128, 128, 128), new Color(192, 192, 192),
        new Color(255, 0, 0), new Color(0, 255, 0), new Color(255, 255, 0),
        new Color(0, 0, 255), new Color(255, 0, 255), new Color(0, 255, 255),
        new Color(255, 255, 255)};
    private final static Hashtable BITMAP_REV_PAL = new Hashtable();
    static
    {
        for (int i = 0; i < BITMAP_PAL.length; i++)
        {
            BITMAP_REV_PAL.put(new Integer(BITMAP_PAL[i].getRGB()), new Byte(
                (byte) i));
        }
    }

    /**
     * Writes the current Sprite to a user-specified file as a normal image
     * using the current palette.
     * 
     * @see #exportSprAsImg(File)
     */
    private void exportSprAsImg()
    {
        exportSprAsImg(getFile(true, "png", "Portable Network Graphics"));
    }

    /**
     * Writes the current Sprite to a file as a normal image using the current
     * palette.
     * 
     * @param f File to export image to.
     */
    public void exportSprAsImg(File f)
    {
        if (f == null)
            return;
        if (!f.getAbsolutePath().endsWith(".png"))
        {
            f = new File(f.getAbsolutePath() + ".png");
        }
        try
        {
            ImageIO.write((BufferedImage) da.getImage(), "png", f);
        }
        catch (IOException e)
        {
            System.out.println("Unable to write image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Sets the current Sprite to the image in a user-specified file. This trys
     * to match the colors in the image to the closest colors in the current
     * palette.
     * 
     * @see #importSprFromImg(File)
     */
    private void importSprFromImg()
    {
        importSprFromImg(getFile(false, "png", "Portable Network Graphics"));
    }

    /**
     * Sets the current Sprite to the image in a file. This trys to match the
     * colors in the image to the closest colors in the current palette.
     * 
     * @param f File to import image from.
     */
    public void importSprFromImg(File f)
    {
        try
        {
            if (f == null)
                return;
            if (!f.exists())
                throw new FileNotFoundException();
            BattleSprite sp = getSelectedSprite();
            BufferedImage img = ImageIO.read(f);
            if (img == null)
            {
                System.out.println("Unknown error loading image in "
                    + "BattleSpriteEditor.importSprFromImg(File).");
                return;
            }
            int w = img.getWidth(), h = img.getHeight();
            if (!BATTLE_SPRITE_SIZES[sp.getSize()].equals(new Dimension(w, h)))
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Invalid image size (" + w + ", " + h + ").\n"
                        + "Make sure the image size matches the\n"
                        + "sprite size you have selected.", "Bad Image Size",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            sp.setSprite(img, getCurrentPal());
            da.setImage(sp.getSprite());
            da.repaint();
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Sets the current Sprite to the image in the specified file. This uses the
     * default palette of a 16-color bitmap to store color indexes as colors.
     * 
     * @param f File to import image from.
     */
    public void importImg(File f)
    {
        try
        {
            if (f == null)
                return;
            if (!f.exists())
                throw new FileNotFoundException();
            BattleSprite sp = getSelectedSprite();
            BufferedImage img = ImageIO.read(f);

            if (img == null)
            {
                System.out
                    .println("How can img be null here!? battle sprite editor");
                return;
            }

            int w = img.getWidth(), h = img.getHeight();
            if (!BATTLE_SPRITE_SIZES[sp.getSize()].equals(new Dimension(w, h)))
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Invalid image size (" + w + ", " + h + ").\n"
                        + "Make sure the image size matches the\n"
                        + "sprite size you have selected.", "Bad Image Size",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            int[] pixels = new int[w * h];
            byte[][] sprite = new byte[w][h];
            PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
            try
            {
                pg.grabPixels();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted waiting for pixels!");
                return;
            }
            for (int j = 0; j < h; j++)
            {
                for (int i = 0; i < w; i++)
                {
                    sprite[i][j] = ((Byte) BITMAP_REV_PAL.get(new Integer(
                        pixels[j * w + i]))).byteValue();
                }
            }

            sp.setSprite(sprite);
            da.setImage(sp.getSprite());
            da.repaint();
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            JOptionPane.showMessageDialog(mainWindow,
                "Invalid colors in image. Image may only contain "
                    + "colors existing in a 16-color non-paletted BMP.\n"
                    + "Try the \"Import sprite from image...\" menu option.",
                "Error Reading Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sets the current Sprite to the image in a user-specified file. This uses
     * the default palette of a 16-color bitmap to store color indexes as
     * colors.
     * 
     * @see #importImg(File)
     */
    public void importImg()
    {
        importImg(getFile(false, "png", "Portable Network Graphics"));
    }

    /**
     * Sets the current Sprite to the image in the specified file. This uses the
     * default palette of a 16-color bitmap to store color indexes as colors.
     * 
     * @param f File to import image from.
     */
    public void importBmpImg(File f)
    {
        try
        {
            if (f == null)
                return;
            if (!f.exists())
                throw new FileNotFoundException();
            BattleSprite sp = getSelectedSprite();
            FileInputStream in = new FileInputStream(f);
            Image img = mainWindow.createImage(BMPReader.getBMPImage(in));
            if (img == null)
            {
                System.out
                    .println("How can img be null here!? battle sprite editor");
                return;
            }
            in.close();
            int w = img.getWidth(mainWindow), h = img.getHeight(mainWindow);
            if (!BATTLE_SPRITE_SIZES[sp.getSize()].equals(new Dimension(w, h)))
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Invalid image size (" + w + ", " + h + ").\n"
                        + "Make sure the image size matches the\n"
                        + "sprite size you have selected.", "Bad Image Size",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[][] sprite = new byte[w][h];

            int[] pixels = new int[w * h];

            PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
            try
            {
                pg.grabPixels();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted waiting for pixels!");
                return;
            }
            if (sprite == null)
                System.out
                    .println("How can sprite be null here!? battle sprite editor bmp import");
            if (BITMAP_REV_PAL == null)
                System.out
                    .println("How can BITMAP_REV_PAL be null here!? battle sprite editor bmp import");
            for (int j = 0; j < h; j++)
            {
                for (int i = 0; i < w; i++)
                {
                    sprite[i][j] = ((Byte) BITMAP_REV_PAL.get(new Integer(
                        pixels[j * w + i]))).byteValue();
                }
            }

            sp.setSprite(sprite);
            da.setImage(sp.getSprite());
            da.repaint();
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Sets the current Sprite to the image in a user-specified file. This uses
     * the default palette of a 16-color bitmap to store color indexes as
     * colors.
     * 
     * @see #importImg(File)
     */
    public void importBmpImg()
    {
        importBmpImg(getFile(false, "bmp", "Windows Bitmap"));
    }

    /**
     * Writes the current Sprite to the specified file. This uses the default
     * palette of a 16-color bitmap to store color indexes as colors.
     * 
     * @param f File to export image to.
     */
    public void exportImg(File f)
    {
        if (f == null)
            return;
        if (!f.getAbsolutePath().endsWith(".png"))
        {
            f = new File(f.getAbsolutePath() + ".png");
        }
        try
        {
            byte[][] img = getSelectedSprite().getSprite();
            ImageIO.write(drawImage(img, BITMAP_PAL), "png", f);
            // new Bitmap(drawImage(img,
            // BITMAP_PAL)).writeTo(f.getAbsolutePath());
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Writes the current Sprite to a user-specified file. This uses the default
     * palette of a 16-color bitmap to store color indexes as colors.
     * 
     * @see #exportImg(File)
     */
    public void exportImg()
    {
        exportImg(getFile(true, "png", "Portable Network Graphics"));
    }
}