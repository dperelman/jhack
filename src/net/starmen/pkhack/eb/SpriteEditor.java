package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * Editor for Sprites in Earthbound.
 * 
 * @author AnyoneEB
 * @see SpriteEditor.Sprite
 * @see SpriteEditor.SpriteInfo
 * @see SpriteEditor.SpriteInfoBlock
 */
public class SpriteEditor extends EbHackModule implements ActionListener,
    DocumentListener
{
    /**
     * @param rom
     * @param prefs
     */
    public SpriteEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    private JComboBox selector, palNum;
    private JTextField zoom, search;
    private JLabel addressLabel;
    private IntArrDrawingArea spriteDrawingArea;
    private SpritePalette spal;
    private JScrollPane jsp;
    private JCheckBoxMenuItem showRepeats, gridLines;
    /** Array of all {@link SpriteEditor.SpriteInfo}'s */
    public static SpriteInfo[] si = null;
    /** Array of all {@link SpriteEditor.SpriteInfoBlock}'s */
    public static SpriteInfoBlock[] sib = null;
    /**
     * <code>Color</code> used as the background color when displaying
     * Sprites. It should never have all three values (r, g, b) have 0's as
     * their last 3 bits or bad things will happen.
     */
    public static Color bgColor = new Color(1, 1, 1);

    /** Number of SPT entries in EarthBound. */
    public final static int NUM_ENTRIES = 464;

    protected void init()
    {
        SpriteEditor.initSptNames(rom.getPath());

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(600, 510);

        mainWindow.getContentPane().add(
            new JSearchableComboBox(selector = createJComboBoxFromArray(
                new String[NUM_ENTRIES * 16], false), "Sprite: "),
            BorderLayout.NORTH);

        selector.setActionCommand("spriteSelector");
        selector.addActionListener(this);

        JPanel entry = new JPanel(new BorderLayout());

        JPanel entryEast = new JPanel();
        entryEast.setLayout(new BoxLayout(entryEast, BoxLayout.Y_AXIS));

        entryEast.add(getLabeledComponent("Address: ",
            addressLabel = new JLabel()));
        entryEast.add(getLabeledComponent("Palette: ",
            palNum = createJComboBoxFromArray(new Object[8])));
        palNum.setActionCommand("palNumChanged");
        palNum.addActionListener(this);
        entryEast.add(HackModule.pairComponents(getLabeledComponent("Zoom: ",
            pairComponents(zoom = new JTextField(4), new JLabel("%"), true)),
            new JLabel(), false));
        zoom.setText("1000");
        zoom.getDocument().addDocumentListener(this);

        DrawingToolset dt = new DrawingToolset(this);
        entryEast.add(dt);
        entry
            .add(pairComponents(entryEast, dt, false, true), BorderLayout.EAST);

        spal = new SpritePalette(16);
        spal.setActionCommand("spal");
        spal.addActionListener(this);
        JPanel spalWrapper = new JPanel(new FlowLayout());
        spalWrapper.add(spal);
        entry.add(spalWrapper, BorderLayout.SOUTH);

        spriteDrawingArea = new IntArrDrawingArea(dt, spal);
        spriteDrawingArea.setZoom(10);
        spriteDrawingArea.setPreferredSize(new Dimension(10 * 32, 10 * 32));
        entry
            .add(jsp = new JScrollPane(spriteDrawingArea), BorderLayout.CENTER);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);

        //menu
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Import sprite...", 'i', null,
            "importImg", this));
        fileMenu.add(HackModule.createJMenuItem("Export sprite...", 'e', null,
            "exportImg", this));

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

        showRepeats = new JCheckBoxMenuItem("Show repeat sprites");
        showRepeats.setMnemonic('s');
        showRepeats.setSelected(false);
        showRepeats.setActionCommand("showRepeats");
        showRepeats.addActionListener(this);
        optionsMenu.add(showRepeats);

        gridLines = new JCheckBoxMenuItem("Show gridLines");
        gridLines.setMnemonic('g');
        gridLines.setSelected(true);
        gridLines.setActionCommand("gridLines");
        gridLines.addActionListener(this);
        optionsMenu.add(gridLines);

        optionsMenu.add(HackModule.createJMenuItem("Set background color", 'b',
            null, "setBgColor", this));

        optionsMenu.add(HackModule.createJMenuItem("Jump to SPT Editor", 'j',
            null, "jumpSPT", this));

        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);

        addDataListener(sptNames, new ListDataListener()
        {

            public void contentsChanged(ListDataEvent lde)
            {
                initSelector(showRepeats.isSelected());
            }

            public void intervalAdded(ListDataEvent arg0)
            {}

            public void intervalRemoved(ListDataEvent arg0)
            {}
        });

        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.9.1";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Sprite Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Sprite i/o based on source code by Tomato\n"
            + "SPT i/o based on source by DrAndonuts\n"
            + "Sprite names list started by Tomato\n"
            + "More sprite names by AnyoneEB and DistortGiygas";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        this.reset();
        readFromRom();
        selector.setSelectedIndex(Math.max(0, selector.getSelectedIndex()));
        initSelector(showRepeats.isSelected());
        selector.updateUI();
        mainWindow.pack();
        mainWindow.setVisible(true);
        mainWindow.repaint();

        //		//sprite dump (change dir :)
        //		initSelector(true);
        //		selector.updateUI();
        //		for(int i = 0; i < selector.getItemCount(); i++)
        //		{
        //			selector.setSelectedIndex(i);
        //			exportImg(new File("D:/Daniel/Earthbound/PKHack/spritedump/" +
        // selector.getSelectedItem().toString().replace('?','
        // ').replaceAll("(w/|w\\\\)","with").replace('"','\'') + ".png"));
        //		}
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * Returns the specified SpriteInfo.
     * 
     * @param i SpriteInfo number to read.
     * @return <code>{@link #si}[i]</code>
     */
    public static SpriteInfo getSpriteInfo(int i)
    {
        //return sib[(i - (i % 16)) / 16].getSpriteInfo(i % 16);
        return si[i];
    }

    private void showInfo(int i)
    {
        Sprite sp = new Sprite(getSpriteInfo(i), this);
        //spriteDrawingArea.setImage(sp.getImage());
        spriteDrawingArea.setImage(sp.getSprite());
        palNum.setSelectedIndex(sp.si.getPalette());
        spal.setPalette(sp.getPalette());
        spal.repaint();

        addressLabel.setText("0x"
            + HackModule.addZeros(
                Integer.toHexString(getSpriteInfo(i).address), 6)
            + " | $"
            + HackModule.addZeros(Integer
                .toHexString(toSnesPointer(getSpriteInfo(i).address)), 6));
    }

    private void saveInfo(int i)
    {
        Sprite sp = new Sprite(getSpriteInfo(i), this);
        sp.setImage(spriteDrawingArea.getIntArrImage());
        sp.writeInfo();
        Sprite.writeSpriteRGB(sp.si.getPalette(), rom);

        sib[getSpriteInfo(i).sptNum].writeInfo();
    }

    //	private int getSelectorNum(Object in)
    //	{
    //		StringTokenizer st = new StringTokenizer(in.toString(), "[]");
    //		return Integer.parseInt(st.nextToken());
    //	}

    private void initSelector(boolean showRepeats)
    {
        int current = HackModule.getNumberOfString(selector.getSelectedItem()
            .toString(), false);
        selector.removeActionListener(this);
        selector.setModel(new DefaultComboBoxModel());
        selector.addItem(getNumberedString(si[0].toString(), 0, false));
        for (int i = 1; i < si.length; i++)
        {
            if (showRepeats
                || (!si[i].equals(si[i - 1]) && (i > 2 ? !si[i]
                    .equals(si[i - 2]) : true)))
            {
                selector.addItem(getNumberedString(si[i].toString(), i, false));
            }
        }

        if (showRepeats)
        {
            selector.setSelectedIndex(current);
            selector.addActionListener(this);
        }
        else
        {
            for (int i = 0; i < selector.getItemCount(); i++)
            {
                if (HackModule.getNumberOfString(selector.getItemAt(i)
                    .toString(), false) == current)
                {
                    selector.setSelectedIndex(i);
                    selector.addActionListener(this);
                    return;
                }
                else if (HackModule.getNumberOfString(selector.getItemAt(i)
                    .toString(), false) > current)
                {
                    selector.addActionListener(this);
                    selector.setSelectedIndex(i - 1 >= 0 ? i - 1 : i);
                    return;
                }
            }
        }
    }

    /**
     * Sets the current Sprite to the image in the specified file.
     * 
     * @param f File to import image from.
     */
    public void importImg(File f)
    {
        if (f == null)
            return;
        try
        {
            if (!f.exists())
                throw new FileNotFoundException();
            Sprite sp = new Sprite(getSpriteInfo(HackModule.getNumberOfString(
                selector.getSelectedItem().toString(), false)), this);
            try
            {
                sp.setImage(ImageIO.read(f));
                //this.spriteDrawingArea.setImage(sp.getImage());
                this.spriteDrawingArea.setImage(sp.getSprite());
                this.spriteDrawingArea.repaint();
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Image import failed due to an image of invalid size.\n"
                        + "Try checking the size of the image "
                        + "and changing the size\n"
                        + "of the target sprite using the "
                        + "SPT editor if necessary.\n"
                        + "Also note that this feature is only to be used\n"
                        + "with sprite images exported by JHack and having\n"
                        + "identical palettes.", "Error: Import Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Sets the current Sprite to the image in a user-specified file.
     * 
     * @see #importImg(File)
     */
    public void importImg()
    {
        //        //choose file
        //        JFileChooser jfc = new JFileChooser();
        //        jfc.setFileFilter(new FileFilter()
        //        {
        //            public boolean accept(File f)
        //            {
        //                if ((f.getAbsolutePath().toLowerCase().endsWith(".png") || f
        //                    .isDirectory())
        //                    && f.exists()) { return true; }
        //                return false;
        //            }
        //
        //            public String getDescription()
        //            {
        //                return "PNG Image (*.png)";
        //            }
        //        });
        //        if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        //        {
        //            importImg(jfc.getSelectedFile());
        //        }
        importImg(getFile(false, "png", "Portable Network Graphics"));
    }

    /**
     * Writes the current Sprite to the specified file.
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
            Sprite sp = new Sprite(getSpriteInfo(HackModule.getNumberOfString(
                selector.getSelectedItem().toString(), false)), this);
            ImageIO.write(sp.getImage(), "png", f);
        }
        catch (IOException e)
        {
            System.out.println("Unable to read image: " + f.toString());
            e.printStackTrace();
        }
    }

    /**
     * Writes the current Sprite to a user-specified file.
     * 
     * @see #exportImg(File)
     */
    public void exportImg()
    {
        //        //choose file
        //        JFileChooser jfc = new JFileChooser();
        //        jfc.setFileFilter(new FileFilter()
        //        {
        //            public boolean accept(File f)
        //            {
        //                if (f.getAbsolutePath().toLowerCase().endsWith(".png")
        //                    || f.isDirectory()) { return true; }
        //                return false;
        //            }
        //
        //            public String getDescription()
        //            {
        //                return "PNG Image (*.png)";
        //            }
        //        });
        //        if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        //        {
        //            exportImg(jfc.getSelectedFile());
        //        }
        exportImg(getFile(true, "png", "Portable Network Graphics"));
    }

    private void setBgCol(Color tc)
    {
        if (tc == null)
            return;
        SpriteEditor.bgColor = new Color((tc.getRed() >= 255 ? 254 : tc
            .getRed()) | 1, (tc.getGreen() >= 255 ? 254 : tc.getGreen()) | 1,
            (tc.getBlue() >= 255 ? 254 : tc.getBlue()) | 1);
        this.changePaletteCol(0, bgColor);
        for (int i = 0; i < Sprite.pals.length; i++)
        {
            Sprite.pals[i][0] = bgColor;
        }
    }

    private void changePaletteCol(int num, Color col)
    {
        Sprite sp = new Sprite(getSpriteInfo(HackModule.getNumberOfString(
            selector.getSelectedItem().toString(), false)), this);
        sp.setImage(this.spriteDrawingArea.getIntArrImage());
        sp.palette[num] = col;
        this.spal.setPalette(sp.getPalette());
        spal.repaint();
        this.spriteDrawingArea.setImage(sp.getSprite());
    }

    private void changePalette(int num)
    {
        if (num < 0 || num > 7)
            return;
        SpriteInfo spi = getSpriteInfo(HackModule.getNumberOfString(selector
            .getSelectedItem().toString(), false));
        sib[spi.sptNum].palette = num;
        Sprite sp = new Sprite(spi, this);
        sp.setImage(this.spriteDrawingArea.getIntArrImage());
        this.spal.setPalette(sp.getPalette());
        spal.repaint();
        this.spriteDrawingArea.setImage(sp.getSprite());
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo(HackModule.getNumberOfString(selector.getSelectedItem()
                .toString(), false));
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(HackModule.getNumberOfString(selector.getSelectedItem()
                .toString(), false));
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("palNumChanged"))
        {
            changePalette(palNum.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("hFlip"))
        {
            spriteDrawingArea.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            spriteDrawingArea.doVFlip();
        }
        else if (ae.getActionCommand().equals("undo"))
        {
            spriteDrawingArea.undo();
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("cut"))
        {
            spriteDrawingArea.cut();
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            spriteDrawingArea.copy();
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            spriteDrawingArea.paste();
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            spriteDrawingArea.delete();
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals(showRepeats.getActionCommand()))
        {
            initSelector(showRepeats.isSelected());
            selector.updateUI();
        }
        else if (ae.getActionCommand().equals(gridLines.getActionCommand()))
        {
            spriteDrawingArea.setDrawGridlines(gridLines.isSelected());
            spriteDrawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("Find"))
        {
            search(search.getText().toLowerCase(), selector);
        }
        else if (ae.getActionCommand().equals("setBgColor"))
        {
            setBgCol(JColorChooser.showDialog(null,
                "Select a new background color", SpriteEditor.bgColor));
        }
        else if (ae.getActionCommand().equals("spal"))
        {
            int cnum = spal.getSelectedColorIndex();
            Color col = spal.getNewColor();
            if (cnum == 0)
                setBgCol(col);
            else
                this.changePaletteCol(cnum, col);
        }
        else if (ae.getActionCommand().equals("importImg"))
        {
            importImg();
        }
        else if (ae.getActionCommand().equals("exportImg"))
        {
            exportImg();
        }
        else if (ae.getActionCommand().equals("jumpSPT"))
        {
            JHack.main.showModule(SPTEditor.class, new Integer(
                getSpriteInfo(HackModule.getNumberOfString(selector
                    .getSelectedItem().toString(), false)).sptNum));
        }
    }

    /**
     * Represents a sprite.
     * 
     * @author AnyoneEB
     */
    public static class Sprite
    {
        private HackModule hm;
        private byte[][] sprite;
        private SpriteInfo si;
        private Color[] palette;
        private static Color[][] pals = null;

        /**
         * Reads a new Sprite from the ROM using the given SpriteInfo.
         * 
         * @param si SpriteInfo to base sprite off of
         * @see SpriteEditor.SpriteInfo
         * @see SpriteEditor.SpriteInfoBlock
         */
        public Sprite(SpriteInfo si, HackModule hm)
        {
            this.hm = hm;
            if (pals == null)
            {
                pals = new Color[8][];
                for (int i = 0; i < 8; i++)
                {
                    pals[i] = getSpriteRGB(i, hm.rom);
                }
            }

            this.si = si;
            this.palette = pals[si.getPalette()];
            int offset = 0;

            sprite = new byte[si.width * 8][si.height * 8];

            for (int a = 0; a < si.height; a++)
            {
                for (int i = 0; i < si.width; i++)
                {
                    hm.read4BPPArea(sprite, si.address + offset, i * 8, a * 8);
                    offset += 32;
                }
            }
        }

        /**
         * Writes this Sprite back into the ROM.
         */
        public void writeInfo()
        {
            int offset = 0;
            for (int a = 0; a < si.height; a++)
            {
                for (int i = 0; i < si.width; i++)
                {
                    hm.write4BPPArea(sprite, si.address + offset, i * 8, a * 8);
                    offset += 32;
                }
            }
        }

        /**
         * Read the specified palette from the ROM into a <code>Color[]</code>.
         * Color 0 is transparent in the game so it is set to
         * {@link SpriteEditor#bgColor}in the returned array.
         * 
         * @param pal Palette number to read.
         * @return <code>Color[]</code> containing the palette.
         */
        public static Color[] getSpriteRGB(int pal, AbstractRom rom)
        {
            Color[] out = rom.readPalette(0x30200 + (pal * 32), 16);
            out[0] = SpriteEditor.bgColor; //transparent

            return out;
        }

        /**
         * Writes the specified palette to the ROM. NOTE: There is currently no
         * way to change the palette through a public method. If you have a use
         * for this, I will implement it.
         * 
         * @param pal Palette number to write
         */
        public static void writeSpriteRGB(int pal, AbstractRom rom)
        {
            // don't write transparent color differently
            pals[pal][0] = rom.readPalette(0x30200 + (pal * 32));
            rom.writePalette(0x30200 + (pal * 32), pals[pal]);
            // set transparent color back to user selection
            pals[pal][0] = SpriteEditor.bgColor;
        }

        /**
         * Call this if something other than the sprite editor changes the
         * palette.
         */
        public static void reloadPal()
        {
            pals = null;
        }

        /**
         * Returns the color number at given coordinate. Color numbers are 0 -
         * 16. 0 = transparent. Other colors depend on the palette. Color 1 =
         * <code>this.getPalette[1]</code>.
         * 
         * @param x Coordinate to read
         * @param y Coordinate to read
         * @return int of color number at given coordinate.
         */
        public int getPixel(int x, int y)
        {
            return sprite[x][y];
        }

        /**
         * Writes a specified color number at the given coordinate. Color
         * numbers are 0 - 16. 0 = transparent. Other colors depend on the
         * palette. Color 1 =<code>this.getPalette[1]</code>.
         * 
         * @param x Coordinate
         * @param y Coordinate
         * @param pixel int of color number to set at given coordinate.
         */
        public void setPixel(int x, int y, int pixel)
        {
            sprite[x][y] = (byte) pixel;
        }

        /**
         * Returns the Color at given coordinate.
         * 
         * @param x Coordinate to read
         * @param y Coordinate to read
         * @return Color at the given coordinate
         */
        public Color getPixelColor(int x, int y)
        {
            return this.palette[this.getPixel(x, y)];
        }

        /**
         * Sets the Color at given coordinate. Only works if the Color is in
         * this Sprite's palette.
         * 
         * @param x Coordinate
         * @param y Coordinate
         * @param pixel Any Color in this Sprite's palette
         */
        public void setPixelColor(int x, int y, Color pixel)
        {
            for (int i = 0; i < this.palette.length; i++)
            {
                if (pixel.equals(palette[i]))
                {
                    this.sprite[x][y] = (byte) i;
                    return;
                }
            }
        }

        /**
         * Sets the Color at given coordinate. The pixel is a color in the form
         * 0xAARRGGBB.
         * 
         * @param x Coordinate
         * @param y Coordinate
         * @param pixel An int of any Color in this Sprite's palette
         */
        public void setPixelColor(int x, int y, int pixel)
        {
            int alpha = (pixel >> 24) & 0xff;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;

            Color c = new Color(red, green, blue, alpha);
            for (int i = 0; i < this.palette.length; i++)
            {
                if (c.equals(palette[i]))
                {
                    this.sprite[x][y] = (byte) i;
                }
            }
        }

        /**
         * Returns the <code>int[][]</code> of this.
         * 
         * @return This Sprite as an <code>int[][]</code> of color numbers
         * @see #setSprite(int[][])
         */
        public int[][] getSprite()
        {
            int[][] out = new int[sprite.length][sprite[0].length];
            for (int x = 0; x < sprite.length; x++)
                for (int y = 0; y < sprite[0].length; y++)
                    out[x][y] = sprite[x][y];

            return out;
        }

        /**
         * Sets the <code>int[][]</code> of this. This doesn't check the size.
         * If the input is a different size this will get messed up.
         * 
         * @param in A Sprite as an <code>int[][]</code> of color numbers
         * @see #getSprite()
         */
        public void setSprite(int[][] in)
        {
            for (int x = 0; x < sprite.length; x++)
                for (int y = 0; y < sprite[0].length; y++)
                    sprite[x][y] = (byte) in[x][y];
        }

        /**
         * Returns an image of this Sprite.
         * 
         * @return A BufferedImage of what this Sprite looks like.
         * @see #setImage(BufferedImage)
         */
        public BufferedImage getImage()
        {
            BufferedImage out = new BufferedImage(sprite.length,
                sprite[0].length, BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();

            for (int x = 0; x < sprite.length; x++)
            {
                for (int y = 0; y < sprite[x].length; y++)
                {
                    g.setColor(this.getPixelColor((si.isHFliped()
                        ? (sprite.length - 1) - x
                        : x), y));
                    g.drawLine(x, y, x, y); //there's no draw point, WHY?!?
                }
            }

            return out;
        }

        /**
         * Sets the contents of this Sprite based on an Image. Any pixels not
         * exactly matching a color on this Sprite's palette will be assumed to
         * be transparent. If anyone could write a method to "round" colors to
         * the ones in a Sprite's palette, that would be greatly apprciated.
         * 
         * @param in Image to set this Sprite to.
         * @see #getImage()
         */
        public void setImage(BufferedImage in)
        {
            sprite = new byte[sprite.length][sprite[0].length];
            BufferedImage img = (BufferedImage) in;

            int[] pixels = new int[img.getWidth() * img.getHeight()];
            int w = img.getWidth(), h = img.getHeight();
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
                    this.setPixelColor((si.isHFliped() ? (w - 1) - i : i), j,
                        pixels[j * w + i]);
                }
            }
        }

        /**
         * Sets the contents of this Sprite based on an int[][].
         * 
         * @param in int[][] containing values (0-15).
         */
        public void setImage(int[][] in)
        {
            for (int x = 0; x < this.sprite.length; x++)
            {
                for (int y = 0; y < this.sprite[0].length; y++)
                {
                    this.setPixel(x, y, in[x][y]);
                }
            }
        }

        /**
         * Returns the palette.
         * 
         * @return Color[]
         */
        public Color[] getPalette()
        {
            return palette;
        }

        /**
         * Returns a String to identify this by. Calls <code>toString()</code>
         * on the {SpriteEditor.SpriteInfo} this was created with.
         * 
         * @see SpriteEditor.SpriteInfo#toString()
         * @return {@link SpriteEditor.SpriteInfo#toString()}
         */
        public String toString()
        {
            return si.toString();
        }
    }

    /**
     * Stores the infomation needed to make a {@link SpriteEditor.Sprite}
     * object.
     */
    public static class SpriteInfo
    {
        /** Where the sprite is stored in the ROM (reg pointer). */
        public int address;
        /** Width of sprite in number of tiles. */
        public int width;
        /** Height of sprite in number of tiles. */
        public int height;

        /** Palette of sprite (0-7). */
        public int getPalette()
        {
            return sibEntry.palette;
        }
        /** Last two bits of the address. Left out of {@link #address}. */
        public int flags;
        /** Which SPT entry this is defined by. */
        public int sptNum;
        /** <code>String</code> to identify this sprite by. */
        public String subDesc = new String();
        private SpriteInfoBlock sibEntry;

        /**
         * Creates a new SpriteInfo with no values set.
         */
        public SpriteInfo()
        {}

        /**
         * Creates a new SpriteInfo with the specified values set.
         * 
         * @param address int
         * @param width int
         * @param height int
         * @param flags int
         * @param sptNum which SPT entry this is defined by
         * @param subDesc direction of this specific sprite
         */
        public SpriteInfo(int address, int width, int height, int flags,
            int sptNum, String subDesc, SpriteInfoBlock sibEntry)
        {
            this.address = address;
            this.width = width;
            this.height = height;
            this.flags = flags;
            this.sptNum = sptNum;
            this.subDesc = subDesc;
            this.sibEntry = sibEntry;

            //			if (flags > 1)
            //			{
            //				System.out.println(
            //					"Flags: "
            //						+ flags
            //						+ " Address: "
            //						+ address
            //						+ " Name: "
            //						+ name);
            //			}
        }

        /**
         * Returns a String to identify this by.
         * 
         * @return the correct String from {@link EbHackModule#sptNames}
         */
        public String toString()
        {
            return sptNames[sptNum] + " (" + this.subDesc + ")";
        }

        /**
         * Reads {@link #flags}to find out whether this Sprite should be shown
         * flipped horizontally.
         * 
         * @return Whether this Sprite should be shown flipped horizontally.
         */
        public boolean isHFliped()
        {
            return (flags & 1) == 1;
            //sprite is h fliped if last address bit is 1
        }

        /**
         * Returns whether this SpriteInfo is the same as another SpriteInfo.
         * Tests the address, width, and height.
         * 
         * @param in SpriteInfo to test against
         * @return Whether this SpriteInfo is the same as <code>in</code>
         */
        public boolean equals(SpriteInfo in)
        {
            return (this.address == in.address && this.width == in.width && this.height == in.height);
        }
    }

    /**
     * Class that represents an entry in the SPT. It can create
     * {@link SpriteEditor.SpriteInfo}'s based on the information.
     * 
     * @author AnyoneEB
     */
    public static class SpriteInfoBlock implements Cloneable
    {
        //ex first in a SIB is "up"
        private static String[] subNames = {"up, walking(L)", "up, walking(R)",
            "right, standing", "right, walking", "down, walking(L)",
            "down, walking(R)", "left, standing", "left, walking",
            "up-right, standing", "up-right, walking", "down-right, standing",
            "down-right, walking", "down-left, standing", "down-left, walking",
            "up-left, standing", "up-left, walking"};
        private AbstractRom rom;
        /**
         * Pretty self-expanitory. Width/height in sprite tiles. Address/bank
         * are SNES pointers. Pointer is a SNES pointer.
         */
        public int address[] = new int[16], bank, width, height, palette, num,
                unknown[] = new int[5], pointer;
        /**
         * How to change the values in the ROM into the right info. First
         * letter: h = height, w = width, p = palette, b = bank, a = address.
         * Second letter: s = ammount to shift left, a = amount to bitwise and
         * by. On aa, the value cut off by the and is used as
         * {@link SpriteEditor.SpriteInfo#flags}.
         */
        public static int hs = 0, ha = 0xff, ws = 4, wa = 0xff, ps = 1,
                pa = 0x07, bs = 0, ba = 0xff, as = 0, aa = 0xfffffc;
        /**
         * Number of sprites this SPT entry points to. Currently found by
         * knowing the difference between this entry's pointer and the next. If
         * anyone knows a better way (using the unknowns?) please tell me.
         */
        public int numSprites; //number of sprites
        //s=shift left, a=and
        /** String to identify this by. */
        public String name = new String();

        private SpriteInfoBlock()
        {}

        /**
         * Reads the specified entry into a new SpriteInfoBlock.
         * 
         * @param num Entry number to read
         */
        public SpriteInfoBlock(int num, AbstractRom rom)
        {
            this.rom = rom;
            this.num = num;
            name = SpriteEditor.sptNames[num].toString();
            //int temp;
            int offset = 3085635 + (4 * (num - 1));

            pointer = rom.read(offset++);
            pointer += rom.read(offset++) << 8;
            pointer += rom.read(offset++) << 16;
            pointer += rom.read(offset++) << 24;

            offset = HackModule.toRegPointer(pointer);

            this.height = (rom.read(offset++) >> hs) & ha;

            this.width = (rom.read(offset++) >> ws) & wa;
            //mess with bit order?

            this.unknown[0] = rom.read(offset++);

            //this.palette = ((rom.read(offset++) & 0x1f) >> 2); //mess with
            // bit order?
            this.palette = (rom.read(offset++) >> ps) & pa;

            for (int i = 1; i < unknown.length; i++)
            {
                unknown[i] = rom.read(offset++);
            }

            this.bank = (rom.read(offset++) >> bs) & ba;

            for (int i = 0; i < 16; i++)
            {
                address[i] = rom.read(offset++);
                address[i] += rom.read(offset++) << 8;
                address[i] = (address[i] >> as);
                //address[i] += bank << 16;
                //address[i] = HackModule.toRegPointer(address[i]);
            }

            //mess with guessing the unknowns
        }

        /**
         * Returns a String to identify this by.
         * 
         * @return {@link #name}
         */
        public String toString()
        {
            return this.name;
        }

        /**
         * Returns the {@link SpriteEditor.SpriteInfo}for the specified address
         * number.
         * 
         * @param num Number of the address to use. (0 >= num >
         *            {@link #numSprites})
         * @return A new SpriteInfo based on this and the specified address
         *         number.
         */
        public SpriteInfo getSpriteInfo(int num)
        {
            //System.out.println("Debug: getting sprite #" + ((this.num*16) +
            // num));
            return new SpriteInfo(HackModule.toRegPointer(address[num]
                + (bank << 16))
                & aa, width, height, address[num] & (0xffffff - aa), this.num,
                (this.numSprites == 9 && num == 8
                    ? "resting"
                    : SpriteInfoBlock.subNames[num]), this);
        }

        /**
         * Writes the information stored by this into the ROM. Does not change
         * the pointer to the information (does change addresses to sprites).
         */
        public void writeInfo()
        {
            int offset = HackModule.toRegPointer(pointer);

            rom.write(offset++, (this.height & ha) << hs);
            rom.write(offset++, (this.width & wa) << ws);
            rom.write(offset++, this.unknown[0]);
            rom.write(offset++, (this.palette & pa) << ps);
            //not 100% sure on palette
            //offset++; //instead of ++ on palette
            for (int i = 1; i < this.unknown.length; i++)
            {
                rom.write(offset++, this.unknown[i]);
            }
            rom.write(offset++, (this.bank & ba) << bs);
            for (int i = 0; i < this.numSprites; i++)
            {
                this.address[i] = (this.address[i] & 0xffff) << as;
                rom.write(offset++, this.address[i]);
                rom.write(offset++, this.address[i] >> 8);
            }
        }

        public int getLength()
        {
            switch (this.numSprites)
            {
                case 16:
                    return 0x29;
                case 9:
                    return 0x1b;
                case 8:
                default:
                    return 0x19;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#clone()
         */
        public Object clone()
        {
            SpriteInfoBlock out = new SpriteInfoBlock();

            for (int i = 0; i < address.length; i++)
            {
                out.address[i] = this.address[i];
            }
            out.bank = this.bank;
            out.height = this.height;
            out.width = this.width;
            out.name = new String(this.name);
            out.num = this.num;
            out.numSprites = this.numSprites;
            out.palette = this.palette;
            out.pointer = this.pointer;
            for (int i = 0; i < unknown.length; i++)
            {
                out.unknown[i] = this.unknown[i];
            }

            return out;
        }

        /**
         * Changes the values in this to the values in the specified
         * SpriteInfoBlock
         * 
         * @param newsib SpriteInfoBlock to paste values of
         */
        public void paste(SpriteInfoBlock newsib)
        {
            if (this.numSprites != newsib.numSprites)
            {
                if (JOptionPane.showConfirmDialog(null,
                    "The number of sprites in the "
                        + "copied SPT entry does not match\n"
                        + "the number of sprites in "
                        + "this SPT entry. Continue anyway?", "Error!",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                    return;
            }
            for (int i = 0; i < address.length; i++)
            {
                this.address[i] = newsib.address[i];
            }
            this.bank = newsib.bank;
            this.height = newsib.height;
            this.width = newsib.width;
            //this.name = new String(newsib.name);
            //this.num = newsib.num;
            //this.numSprites = newsib.numSprites;
            this.palette = newsib.palette;
            //this.pointer =newsib.pointer;
            for (int i = 0; i < unknown.length; i++)
            {
                this.unknown[i] = newsib.unknown[i];
            }
        }
    }

    //TODO there must be a good way to do this!
    private static class StringHolder
    {
        public String value;
        private int i;

        public StringHolder(int i, String value)
        {
            this.i = i;
            this.value = value;
        }

        public String toString()
        {
            return HackModule.getNumberedString(value, i);
        }
    }

    /**
     * Reads in {@link #sptNames}if it hasn't already been read in. Reads from
     * net/starmen/pkhack/sptNames.txt. The {@link SPTEditor}GUI allows for
     * easy editing of this file.
     */
    public static void initSptNames(String romPath)
    {
        String[] tmp = new String[sptNames.length - 1];
        readArray(DEFAULT_BASE_DIR, "sptNames.txt", romPath, false, tmp);
        System.arraycopy(tmp, 0, sptNames, 1, tmp.length);
        sptNames[0] = "Null";
    }

    private static void initSpriteInfo(AbstractRom rom)
    {
        int siNum = 0;
        int[][] tempSi;
        //if (sib == null)
        //{
        sib = new SpriteInfoBlock[NUM_ENTRIES];
        tempSi = new int[sib.length * 16][2];
        for (int i = 0; i < sib.length; i++)
        {
            sib[i] = new SpriteInfoBlock(i, rom);
        }
        for (int i = 0; i < sib.length; i++)
        {
            //assume last is 8 sprites, may be changed?
            if (i == (sib.length - 1))
            {
                sib[i].numSprites = 8;
            }
            else if (sib[i + 1].pointer - sib[i].pointer == 0x29)
            {
                sib[i].numSprites = 16;
            }
            else if (sib[i + 1].pointer - sib[i].pointer == 0x19)
            {
                sib[i].numSprites = 8;
            }
            else if (sib[i + 1].pointer - sib[i].pointer == 0x1b)
            {
                sib[i].numSprites = 9;
            }

            for (int j = 0; j < sib[i].numSprites; j++)
            {
                tempSi[siNum++] = new int[]{i, j};
            }
        }

        //make si only as large as it needs to be
        si = new SpriteInfo[siNum];
        for (int i = 0; i < si.length; i++)
        {
            si[i] = sib[tempSi[i][0]].getSpriteInfo(tempSi[i][1]);
        }
        //}
    }

    private void updateZoom()
    {
        try
        {
            this.spriteDrawingArea
                .setZoom(Float.parseFloat(zoom.getText()) / 100);
            jsp.updateUI();
        }
        catch (NumberFormatException nfe)
        {}
    }

    /**
     * @see javax.swing.event.DocumentListener#changedUpdate(DocumentEvent)
     */
    public void changedUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    /**
     * @see javax.swing.event.DocumentListener#insertUpdate(DocumentEvent)
     */
    public void insertUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    /**
     * @see javax.swing.event.DocumentListener#removeUpdate(DocumentEvent)
     */
    public void removeUpdate(DocumentEvent arg0)
    {
        updateZoom();
    }

    /**
     * Writes {@link #sptNames}into <i>ROM_NAME </i> .sptNames.txt. This is
     * used by the {@link SPTEditor}GUI for allow for easy editing of this
     * file.
     */
    public static void writeSptNames(String romPath)
    {
        //TODO use writeArray()?
        String output = new String();
        for (int i = 1; i < sptNames.length; i++)
        {
            output += (sptNames[i].toString().length() > 0 ? (i - 1) + "-"
                + sptNames[i] + "\n" : "");
        }

        try
        {
            FileOutputStream out = new FileOutputStream(romPath
                + ".sptNames.txt");
            //FileOutputStream out =
            //	new FileOutputStream("net/starmen/pkhack/sptNames.txt");
            byte[] b = new byte[output.length()];
            for (int i = 0; i < b.length; i++)
            {
                b[i] = (byte) output.charAt(i);
            }
            out.write(b);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Error: File not saved: File not found.");
            e.printStackTrace();
            return;
        }
        catch (IOException e)
        {
            System.out.println("Error: File not saved: Could write file.");
            e.printStackTrace();
            return;
        }
    }

    public static void readFromRom(AbstractRom rom)
    {
        sib = null;
        si = null;
        SpriteEditor.initSpriteInfo(rom);
        Sprite.pals = null;
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }

    public void reset()
    {
        initSptNames(rom.getPath());
    }

    /**
     * Shows the sprite editor window and goes to the sprite indicated by
     * <code>in</code>. Integers are used as the number of the SpriteInfo
     * entry. Strings are used as search strings through the sprite names. If
     * <code>in</code> is not an Integer, String, Sprite, or SpriteInfo,
     * <code>in.toString()</code> will be used for the String.
     * 
     * @see net.starmen.pkhack.HackModule#show(java.lang.Object)
     * @param in An Integer, String, Sprite, SpriteInfo, or SpriteInfoBlock.
     * @throws IllegalArgumentException If <code>in</code> is not an accepted
     *             Object type.
     */
    public void show(Object in) throws IllegalArgumentException
    {
        show();

        if (in instanceof Integer)
        {
            if (showRepeats.isSelected())
                this.selector.setSelectedIndex(((Integer) in).intValue()
                    % selector.getItemCount());
            else
                HackModule.search("[" + ((Integer) in).intValue() + "]",
                    selector);

        }
        else if (in instanceof SpriteInfo)
        {
            SpriteInfo isi = (SpriteInfo) in;
            for (int i = 0; i < si.length; i++)
            {
                if (si[i].sptNum == isi.sptNum
                    && si[i].subDesc.equals(isi.subDesc))
                {
                    //same
                    showRepeats.setSelected(true);
                    initSelector(true);
                    selector.setSelectedIndex(i);
                    return;
                }
            }
        }
        else
        {
            if (!HackModule.search(in.toString(), selector))
                HackModule.search(new StringTokenizer(in.toString(), "(")
                    .nextToken(), selector);
            if (!(in instanceof String || in instanceof Sprite
                || in instanceof SpriteInfo || in instanceof SpriteInfoBlock))
                throw new IllegalArgumentException("Object not "
                    + "Integer, String, SpriteEditor.SpriteInfo, "
                    + "or SpriteEditor.Sprite.");
        }
    }

    /**
     * Sets the specified SPT name for the current ROM.
     * 
     * @param i which SPT name to set (1 based counting, 0 = Null)
     * @param string what to set SPT name to
     */
    public static void setSptName(int i, String string)
    {
        if (i != 0)
            sptNames[i] = string;
    }
}