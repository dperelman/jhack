package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.XMLPreferences;

/**
 * GUI for editing SPT entries. Uses {@link SpriteEditor.SpriteInfoBlock}.
 * 
 * @author AnyoneEB
 */
public class SPTEditor extends EbHackModule implements ActionListener
{
    /*
     * private class TileSelector extends AbstractButton implements
     * MouseListener, MouseMotionListener { //TODO 110200 to 1547BF = Sprite
     * data //TODO make int[][] getSprites() in SpriteEditor.Sprite private int
     * currentTile = 0, sprites[][]; private static final int TILES_WIDE = 175,
     * TILES_HIGH = 50, TILE_SIZE = 16; public int getCurrentTile() { return
     * currentTile; } public void setCurrentTile(int newTile) { //only fire
     * ActionPerformed if new tile if (currentTile != newTile) {
     * reHighlight(currentTile, newTile); currentTile = newTile;
     * this.fireActionPerformed( new ActionEvent( this,
     * ActionEvent.ACTION_PERFORMED, this.getActionCommand())); } } private void
     * setCurrentTile(int x, int y) { setCurrentTile(((y / TILE_SIZE) *
     * TILES_WIDE) + (x / TILE_SIZE)); }
     * 
     * private void reHighlight(int oldTile, int newTile) { Graphics g =
     * this.getGraphics(); g.drawImage(
     * tilesets[getCurrentTileset()].getTileImage( oldTile, getCurrentPalette(),
     * getCurrentSubPalette()), (oldTile % TILES_WIDE) * TILE_SIZE, (oldTile /
     * TILES_WIDE) * TILE_SIZE, 16, 16, null); g.setColor(new Color(255, 255, 0,
     * 128)); g.fillRect((newTile % TILES_WIDE) * TILE_SIZE, (newTile /
     * TILES_WIDE) * TILE_SIZE, TILE_SIZE, TILE_SIZE); }
     * 
     * public void paint(Graphics g) { if
     * (tilesets[getCurrentTileset()].isInited()) { if (paletteIsInited &&
     * guiInited) g.drawImage( tilesets[getCurrentTileset()] .getTilesetImage(
     * getCurrentPalette(), getCurrentSubPalette(), getCurrentTile())
     * .getScaledInstance(TILES_WIDE * TILE_SIZE, TILES_HIGH * TILE_SIZE, 0), 0,
     * 0, null); } }
     * 
     * public void mouseClicked(MouseEvent me) { setCurrentTile(me.getX(),
     * me.getY()); } public void mousePressed(MouseEvent me) {
     * setCurrentTile(me.getX(), me.getY()); } public void
     * mouseReleased(MouseEvent me) { setCurrentTile(me.getX(), me.getY()); }
     * public void mouseEntered(MouseEvent arg0) {} public void
     * mouseExited(MouseEvent arg0) {}
     * 
     * public void mouseDragged(MouseEvent me) { if (!(me.getX() < 0 ||
     * me.getY() < 0 || me.getX() > TILES_WIDE * TILE_SIZE - 1 || me.getY() >
     * TILES_HIGH * TILE_SIZE - 1)) setCurrentTile(me.getX(), me.getY()); }
     * public void mouseMoved(MouseEvent arg0) {}
     * 
     * private String actionCommand = new String(); public String
     * getActionCommand() { return this.actionCommand; } public void
     * setActionCommand(String arg0) { this.actionCommand = arg0; }
     * 
     * public TileSelector() { setPreferredSize(new Dimension(TILES_WIDE *
     * TILE_SIZE, TILES_HIGH * TILE_SIZE)); this.addMouseListener(this);
     * this.addMouseMotionListener(this); } }
     */

    /**
     * @param rom
     * @param prefs
     */
    public SPTEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    private SpriteEditor.SpriteInfoBlock sib = null, clipboard = null;
    private final static int NUM_ENTRIES = 463;
    private JComboBox selector, palette, tileset, tilesetPal;
    private Box tilesetBox;
    private JTextField name, width, height, bank,
            address[] = new JTextField[16],
            //		wa,
            //		ws,
            //		ha,
            //		hs,
            //		pa,
            //		ps,
            //		ba,
            //		bs,
            //		aa,
            //		as,
            search;
    private JButton[] pics = new JButton[16];
    private JTextField[] unknown = new JTextField[5];
    private JLabel pointer, diff;

    protected void init()
    {
        SpriteEditor.initSptNames(rom.getPath());

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setSize(560, 520);

        JPanel display = new JPanel(new GridLayout(4, 4));
        for (int i = 0; i < pics.length; i++)
        {
            display.add(pics[i] = new JButton(Integer.toString(i)));
            pics[i].setBorder(null);
            pics[i].setActionCommand("pics" + Integer.toHexString(i));
            pics[i].addActionListener(this);
        }
        mainWindow.getContentPane().add(display, BorderLayout.CENTER);

        Box entry = new Box(BoxLayout.Y_AXIS);
        entry.add(getLabeledComponent("Name: ", name = new JTextField(20)));
        entry.add(getLabeledComponent("Width: ", width = createSizedJTextField(
            3, true)));
        entry.add(getLabeledComponent("Height: ",
            height = createSizedJTextField(3, true)));
        entry.add(getLabeledComponent("Palette: ",
            palette = createJComboBoxFromArray(new Object[8])));
        palette.setActionCommand("sptpal");
        palette.addActionListener(this);
        tilesetBox = new Box(BoxLayout.Y_AXIS)
        {
            public void setEnabled(boolean b)
            {
                super.setEnabled(b);
                tileset.setEnabled(b);
                tilesetPal.setEnabled(b);
            }
        };
        tilesetBox.add(getLabeledComponent("Tileset: ",
            tileset = createComboBox(TileEditor.TILESET_NAMES, this)));
        tileset.setActionCommand("tilesetSel");
        tilesetBox.add(getLabeledComponent("Tileset Palette:",
            tilesetPal = new JComboBox()));
        tileset.setSelectedIndex(0);
        entry.add(tilesetBox);
        tilesetBox.setEnabled(false);
        entry.add(getLabeledComponent("Bank: ", bank = createSizedJTextField(2,
            true, true)));
        // bank and addresses shown in hex
        for (int i = 0; i < address.length; i++)
        {
            entry.add(getLabeledComponent("Address #" + i + ": ",
                address[i] = createSizedJTextField(4, true, true)));
        }
        JPanel buttons = new JPanel(new FlowLayout());

        JButton testb = new JButton("Test Entry");
        testb.addActionListener(this);
        buttons.add(testb);

        JButton saveb = new JButton("Save Entry");
        saveb.addActionListener(this);
        buttons.add(saveb);

        JButton copyb = new JButton("Copy");
        copyb.addActionListener(this);
        buttons.add(copyb);

        JButton pasteb = new JButton("Paste");
        pasteb.addActionListener(this);
        buttons.add(pasteb);

        JButton closeb = new JButton("Close");
        closeb.setActionCommand("close");
        closeb.addActionListener(this);
        buttons.add(closeb);

        mainWindow.getContentPane().add(buttons, BorderLayout.SOUTH);

        mainWindow.getContentPane().add(
            pairComponents(entry, new JLabel(), false), BorderLayout.EAST);

        mainWindow.getContentPane().add(
            new JSearchableComboBox(selector = createComboBox(sptNames, true,
                this), "Entry #:"), BorderLayout.NORTH);
        selector.setActionCommand("sptsel");
        //selector.addActionListener(this);

        Box unknowns = new Box(BoxLayout.Y_AXIS);
        unknowns.add(getLabeledComponent("Pointer: ", pointer = new JLabel()));
        unknowns.add(getLabeledComponent("Difference: ", diff = new JLabel(),
            "Difference between the nex pointer and this one"));
        for (int i = 0; i < unknown.length; i++)
        {
            unknowns.add(getLabeledComponent("Unknown #" + i + ": ",
                unknown[i] = createSizedJTextField(2, true, true)));
            if (i == 0)
                unknowns.add(Box.createVerticalStrut(5));
        }
        unknowns.add(Box.createVerticalStrut(20));
        unknowns.add(new JLabel("\tTo all sprites:"));
        unknowns.add(Box.createVerticalStrut(5));
        JButton hFlip = new JButton(), vFlip = new JButton(), swap = new JButton(
            "Swap X<-->Y");
        hFlip.setIcon(DrawingToolset.getHFlipIcon());
        hFlip.setActionCommand("hFlipAll");
        hFlip.addActionListener(this);
        unknowns.add(hFlip);
        vFlip.setIcon(DrawingToolset.getVFlipIcon());
        vFlip.setActionCommand("vFlipAll");
        vFlip.addActionListener(this);
        unknowns.add(vFlip);
        swap.setActionCommand("swapXYAll");
        swap.addActionListener(this);
        unknowns.add(swap);
        mainWindow.getContentPane().add(
            pairComponents(unknowns, new JLabel(), false), BorderLayout.WEST);
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.5";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "SPT Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        selector.setSelectedIndex(Math.max(selector.getSelectedIndex(), 0));
        //		ws.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.ws));
        //		hs.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.hs));
        //		ps.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.ps));
        //		bs.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.bs));
        //		as.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.as));
        //		wa.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.wa, 16));
        //		ha.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.ha, 16));
        //		pa.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.pa, 16));
        //		ba.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.ba, 16));
        //		aa.setText(Integer.toString(SpriteEditor.SpriteInfoBlock.aa, 16));
        SpriteEditor.Sprite.reloadPal();
        mainWindow.setVisible(true);
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void updateImages()
    {
        for (int i = 0; i < this.pics.length; i++)
        {
            if (i < sib.numSprites)
            {
                SpriteEditor.Sprite sp = new SpriteEditor.Sprite(sib
                    .getSpriteInfo(i), this);
                byte[][] spb = sp.getSpriteByte();
                Color[] pal;
                if (sib.palette == 4)
                {
                    TileEditor.Tileset ts = TileEditor.tilesets[tileset
                        .getSelectedIndex()];
                    int pn = tilesetPal.getSelectedIndex();
                    int subpal = (ts.getPaletteColor(0, pn, 2).getRed() >> 3) - 2;
                    pal = ts.getPaletteColors(pn, subpal);
                }
                else
                {
                    pal = sp.getPalette();
                }
                BufferedImage img = drawImage(spb, pal);
                pics[i].setIcon(new ImageIcon(zoomImage(img, 2)));
                pics[i].setEnabled(true);
            }
            else
            {
                pics[i].setIcon(new ImageIcon(new BufferedImage(1, 1,
                    BufferedImage.TYPE_4BYTE_ABGR)));
                pics[i].setEnabled(false);
            }
        }
    }

    private Image zoomImage(BufferedImage in, float zoom)
    {
        return in.getScaledInstance((int) (in.getWidth() * zoom), (int) (in
            .getHeight() * zoom), 0);
    }

    private void updateTFs()
    {
        //reads from sib to top text fields
        name.setText(sib.name);
        name.setEnabled(sib.num != 0);
        width.setText(Integer.toString(sib.width));
        height.setText(Integer.toString(sib.height));
        palette.setSelectedIndex(sib.palette);
        bank.setText(Integer.toHexString(sib.bank));
        for (int i = 0; i < address.length; i++)
        {
            address[i].setText(Integer.toHexString(sib.address[i] & 0xFFFF));
            if (i < sib.numSprites)
                address[i].setEnabled(true);
            else
                address[i].setEnabled(false);
        }
        for (int j = 0; j < unknown.length; j++)
        {
            unknown[j].setText(addZeros(Integer.toHexString(sib.unknown[j]), 2)
                + " ");
        }
    }

    private void showInfo(int i)
    {
        if (i < 0)
            return;
        sib = new SpriteEditor.SpriteInfoBlock(i, rom);
        pointer.setText(Integer.toHexString(sib.pointer));
        int dif = (i < SPTEditor.NUM_ENTRIES - 1
            ? new SpriteEditor.SpriteInfoBlock(i + 1, rom).pointer
                - sib.pointer
            : 0x19);
        diff.setText(i < SPTEditor.NUM_ENTRIES - 1
            ? Integer.toHexString(dif)
            : "N/A");
        if (i == (SPTEditor.NUM_ENTRIES - 1))
        {
            sib.numSprites = 8;
        }
        else if (dif == 0x29)
        {
            sib.numSprites = 16;
        }
        else if (dif == 0x19)
        {
            sib.numSprites = 8;
        }
        else if (dif == 0x1b)
        {
            sib.numSprites = 9;
        }
        updateTFs();
        updateImages();
    }

    //TODO test palete combo box
    private void testEntry()
    {
        sib.name = name.getText();
        sib.width = Integer.parseInt(width.getText());
        sib.height = Integer.parseInt(height.getText());
        sib.palette = palette.getSelectedIndex();
        for (int i = 0; i < address.length; i++)
        {
            sib.address[i] = Integer.parseInt(address[i].getText(), 16);
        }
        sib.bank = Integer.parseInt(bank.getText(), 16);
        updateImages();
    }

    private void saveEntry()
    {
        if (selector.getSelectedIndex() < 0)
            return;
        testEntry();
        SpriteEditor.setSptName(sib.num, sib.name);
        notifyDataListeners(sptNames, this, sib.num);
        //TODO what's the right way to refresh a model
        //		selector.removeActionListener(this);
        //		selector.setSelectedIndex(sib.num);
        //		selector.addActionListener(this);
        //selector.setSelectedIndex(selector.getSelectedIndex());
        SpriteEditor.writeSptNames(rom.getPath());
        for (int i = 0; i < unknown.length; i++)
            sib.unknown[i] = Integer.parseInt(unknown[i].getText().trim(), 16);
        sib.writeInfo();
        //		int temp = selector.getSelectedIndex();
        //		selector =
        //			HackModule.createJComboBoxFromArray(
        //				SpriteEditor.sptNames,
        //				selector,
        //				false);
        //		selector.setSelectedIndex(temp);
        //		selector.updateUI();

        //		int numSptNames = 0;
        //		for (int i = 0; i < SpriteEditor.sptNames.length; i++)
        //		{
        //			if (SpriteEditor.sptNames[i].length() > 0)
        //				numSptNames++;
        //		}
        //		System.out.println(
        //			"Known SPT Names: "
        //				+ numSptNames
        //				+ "/"
        //				+ SpriteEditor.sptNames.length
        //				+ " = "
        //				+ (((float)numSptNames / (float)SpriteEditor.sptNames.length) * 100)
        //				+ "%");
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("sptsel"))
        {
            showInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("Test Entry"))
        {
            testEntry();
        }
        else if (ae.getActionCommand().equals("Save Entry"))
        {
            saveEntry();
        }
        else if (ae.getActionCommand().equals("Find"))
        {
            search(search.getText().toLowerCase(), selector);
        }
        else if (ae.getActionCommand().equals("Copy"))
        {
            clipboard = (SpriteEditor.SpriteInfoBlock) sib.clone();
        }
        else if (ae.getActionCommand().equals("Paste"))
        {
            if (clipboard != null)
            {
                sib.paste(clipboard);
                updateTFs();
                updateImages();
            }
        }
        else if (ae.getActionCommand().equals("sptpal"))
        {
            tilesetBox.setEnabled(palette.getSelectedIndex() == 4);
        }
        else if (ae.getActionCommand().equals("tilesetSel"))
        {
            tilesetPal.removeActionListener(this);
            tilesetPal.removeAllItems();
            TileEditor.Tileset ts = TileEditor.tilesets[tileset
                .getSelectedIndex()];
            for (int i = 0; i < ts.getPaletteCount(); i++)
                tilesetPal.addItem(ts.getPaletteName(i));
            tilesetPal.addActionListener(this);
            tilesetPal.setSelectedIndex(0);
        }
        else if (ae.getActionCommand().equals("hFlipAll"))
        {
            Set addresses = new TreeSet();
            for (int i = 0; i < sib.numSprites; i++)
            {
                SpriteEditor.SpriteInfo si = sib.getSpriteInfo(i);
                SpriteEditor.Sprite sp = new SpriteEditor.Sprite(sib
                    .getSpriteInfo(i), this);
                if (addresses.contains(new Integer(si.address & 0xfffc)))
                    continue;
                addresses.add(new Integer(si.address & 0xfffc));
                byte[][] b = sp.getSpriteByte();
                int w = b.length, h = b[0].length;
                byte[][] n = new byte[w][h];
                for (int x = 0; x < w; x++)
                {
                    System.arraycopy(b[w - x - 1], 0, n[x], 0, h);
                }
                sp.setSprite(n);
                sp.writeInfo();
            }
            updateImages();
        }
        else if (ae.getActionCommand().equals("vFlipAll"))
        {
            Set addresses = new TreeSet();
            for (int i = 0; i < sib.numSprites; i++)
            {
                SpriteEditor.SpriteInfo si = sib.getSpriteInfo(i);
                SpriteEditor.Sprite sp = new SpriteEditor.Sprite(si, this);
                if (addresses.contains(new Integer(si.address & 0xfffc)))
                    continue;
                addresses.add(new Integer(si.address & 0xfffc));
                byte[][] b = sp.getSpriteByte();
                int w = b.length, h = b[0].length;
                byte[][] n = new byte[w][h];
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        n[x][y] = b[x][h - y - 1];
                sp.setSprite(n);
                sp.writeInfo();
            }
            updateImages();
        }
        else if (ae.getActionCommand().equals("swapXYAll"))
        {
            Set addresses = new TreeSet();
            int nh = sib.width, nw = sib.height;
            for (int i = 0; i < sib.numSprites; i++)
            {
                SpriteEditor.SpriteInfo si = sib.getSpriteInfo(i);
                SpriteEditor.Sprite sp = new SpriteEditor.Sprite(si, this);
                if (addresses.contains(new Integer(si.address & 0xfffc)))
                    continue;
                addresses.add(new Integer(si.address & 0xfffc));
                byte[][] b = sp.getSpriteByte();
                int w = b.length, h = b[0].length;
                byte[][] n = new byte[h][w];
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        n[y][x] = b[x][y];
                si.height = nh;
                si.width = nw;
                sp.setSprite(n);
                sp.writeInfo();
            }
            sib.height = nh;
            sib.width = nw;
            updateTFs();
            updateImages();
        }
        else if (ae.getActionCommand().startsWith("pics"))
        {
            //            System.out.println(sib.getSpriteInfo(
            //                Integer.parseInt(ae.getActionCommand().substring(4), 16))
            //                .toString());
            JHack.main.showModule(SpriteEditor.class, sib.getSpriteInfo(Integer
                .parseInt(ae.getActionCommand().substring(4), 16)));
        }
        else
        {
            System.out.println("SPTEditor: actionPerformed(): "
                + "uncaught action command: " + ae.getActionCommand());
        }
    }

    /**
     * Shows the SPT editor window and goes to the SPT indicated by
     * <code>in</code>. Integers are used as the number of the SPT entry (0
     * based counting). Strings are used as search strings through the sprite
     * names. If <code>in</code> is not an Integer, String, Sprite,
     * SpriteInfo, or SpriteInfoBlock, <code>in.toString()</code> will be used
     * for the String.
     * 
     * @see net.starmen.pkhack.HackModule#show(java.lang.Object)
     * @see SpriteEditor.Sprite
     * @see SpriteEditor.SpriteInfo
     * @see SpriteEditor.SpriteInfoBlock
     * @param in An Integer, String, Sprite, SpriteInfo, or SpriteInfoBlock.
     * @throws IllegalArgumentException If <code>in</code> is not an accepted
     *             Object type.
     */
    public void show(Object in) throws IllegalArgumentException
    {
        show();

        if (in instanceof Integer)
        {
            this.selector.setSelectedIndex(((Integer) in).intValue()
                % selector.getItemCount());
            selector.repaint();
        }
        else
        {
            if (!HackModule.search(in.toString(), selector))
                HackModule.search(new StringTokenizer(in.toString(), "(")
                    .nextToken(), selector);
            if (!(in instanceof String || in instanceof SpriteEditor.Sprite
                || in instanceof SpriteEditor.SpriteInfo || in instanceof SpriteEditor.SpriteInfoBlock))
                throw new IllegalArgumentException("Object not "
                    + "Integer, String, SpriteEditor.SpriteInfoBlock, "
                    + "SpriteEditor.SpriteInfo, or SpriteEditor.Sprite.");
        }
    }
}