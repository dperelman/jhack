package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.SpriteEditor.SpriteInfoBlock;

/**
 * GUI for editing SPT entries. Uses {@link SpriteEditor.SpriteInfoBlock}.
 * 
 * @author AnyoneEB
 */
public class SPTEditor extends EbHackModule implements ActionListener
{
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
            address[] = new JTextField[16], search;
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

        JButton reallocateb = new JButton("Reallocate Addresses");
        reallocateb.addActionListener(this);
        buttons.add(reallocateb);

        buttons.add(new JSeparator());

        JButton testb = new JButton("Test Entry");
        testb.addActionListener(this);
        buttons.add(testb);

        JButton saveb = new JButton("Save Entry");
        saveb.addActionListener(this);
        buttons.add(saveb);

        buttons.add(new JSeparator());

        JButton copyb = new JButton("Copy");
        copyb.addActionListener(this);
        buttons.add(copyb);

        JButton pasteb = new JButton("Paste");
        pasteb.addActionListener(this);
        buttons.add(pasteb);

        buttons.add(new JSeparator());

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
        return "0.6";
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
                SpriteEditor.SpriteInfo si = sib.getSpriteInfo(i);
                SpriteEditor.Sprite sp = new SpriteEditor.Sprite(si, this);
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

    //TODO test palette combo box
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
        SpriteEditor.writeSptNames(rom.getPath());
        for (int i = 0; i < unknown.length; i++)
            sib.unknown[i] = Integer.parseInt(unknown[i].getText().trim(), 16);
        sib.writeInfo();
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
        else if (ae.getActionCommand().equals("Reallocate Addresses"))
        {
            reallocateAddresses();
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
     * Finds space in the expanded meg for sprites.
     */
    private void reallocateAddresses()
    {
        if (sib.numSprites == 0)
        {
            return;
        }
        TreeSet s = new TreeSet();
        for (int i = 0; i < sib.numSprites; i++)
        {
            s.add(new Integer(sib.address[i] & SpriteInfoBlock.aa));
        }

        int ans = JOptionPane
            .showOptionDialog(
                mainWindow,
                "<html>"
                    + "<p>You have chosen to find a new place for this entry "
                    + "to point.<br>Your current sprite data <em>will be lost</em>.<br>"
                    + "You should only use this when you wish to make larger "
                    + "sprites.</p><br>"
                    + "<p>Some sprites are simply mirrors of other sprites,<br>"
                    + "and therefore their graphics data "
                    + "is not stored separately.<br>"
                    + "Instead their addresses are set some address with the<br>0x0001 "
                    + "bit set, which indicates a horizontal flip.</p><br>"
                    + "<p>Do you wish to allocate space for only previously "
                    + "unique sprites,<br>or do you wish for all sprites to have "
                    + "unique graphics space allocated?</p>" + "</html>",
                "Allocate how many sprites?", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new String[]{
                    "Allocate " + s.size() + " sprite(s)",
                    "Allocate " + sib.numSprites + " sprites", "Cancel"},
                "Cancel");
        if (ans == JOptionPane.CANCEL_OPTION)
        {
            return;
        }
        int allocSpr = (ans == JOptionPane.YES_OPTION
            ? s.size()
            : sib.numSprites);
        int spriteSize = 32 * numberize(width.getText())
            * numberize(height.getText());
        /*
         * The +2 is because we need a 0xFF shield on either side. The expanded
         * meg is a dangerous place. The +3 is to make sure there is space to
         * adjust the address because the lowest two bits cannot be used (they
         * are read as flags, not part of the address).
         */
        int bytes = allocSpr * spriteSize + 2 + 3;
        /*
         * "Null" with 0xFF's so it does not accidently get overwritten.
         */
        byte[] nulls = new byte[bytes];
        Arrays.fill(nulls, (byte) 0xff);
        try
        {
            int baseAddress = findFreeRange(bytes);
            /*
             * Adjust to make sure the lowest two bits of the address are zeros.
             */
            while (((baseAddress + 1) & 3) != 0)
            {
                baseAddress++;
                bytes--;
            }
            rom.write(baseAddress, nulls, bytes);
            baseAddress = toSnesPointer(baseAddress);

            int block = baseAddress >> 16;
            int[] newAddr = new int[16];
            int addr = ((baseAddress + 1) & 0xffff) - spriteSize;
            if (allocSpr == sib.numSprites)
            {
                for (int i = 0; i < allocSpr; i++)
                {
                    addr += spriteSize;
                    newAddr[i] = addr;
                }
            }
            else
            {
                Hashtable ht = new Hashtable();
                for (Iterator i = s.iterator(); i.hasNext(); ht.put(i.next(),
                    new Integer(addr += spriteSize)))
                    ;
                for (int i = 0; i < sib.numSprites; i++)
                {
                    int flags = (0xffff - SpriteInfoBlock.aa) & sib.address[i];
                    newAddr[i] = ((Integer) ht.get(new Integer(sib.address[i]
                        & SpriteInfoBlock.aa))).intValue()
                        | flags;
                }
            }

            this.bank.setText(Integer.toHexString(block));
            for (int i = 0; i < sib.numSprites; i++)
            {
                this.address[i].setText(Integer.toHexString(newAddr[i]));
            }
            testEntry();
        }
        catch (EOFException e)
        {
            JOptionPane.showMessageDialog(mainWindow, "No space left in ROM.\n"
                + "Try expanding your ROM.", "No space",
                JOptionPane.ERROR_MESSAGE);
            return;
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