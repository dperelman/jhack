/*
 * Created on Nov 23, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JLinkComboBox;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class TPTEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public TPTEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static final int NUM_ENTRIES = 1584;

    /**
     * TODO Write javadoc for this class
     * 
     * @author AnyoneEB
     */
    public static class TPTEntry
    {
        public static final int TYPE_PERSON = 1;
        public static final int TYPE_ITEM = 2;
        public static final int TYPE_OBJECT = 3;

        public static final int SHOW_SPRITE_GIFT = 0;
        public static final int SHOW_SPRITE_ON = 1;
        public static final int SHOW_SPRITE_OFF = 2;

        private HackModule hm;
        private int num, address;
        private int sprite;
        private int type;
        private int pointer, secPointer;
        private int movement;
        private int direction;
        private int eventFlag;
        private int showSprite;

        public TPTEntry(int num, HackModule hm)
        {
            this.hm = hm;
            AbstractRom rom = hm.rom;

            address = /* 0x0F8B96 */0xF8B85 + (num * 0x11);
            rom.seek(address);

            type = rom.readSeek();
            sprite = rom.readMultiSeek(2);
            direction = rom.readSeek();
            movement = rom.readMultiSeek(2);
            eventFlag = rom.readMultiSeek(2);
            showSprite = rom.readSeek();
            pointer = rom.readMultiSeek(4);
            secPointer = rom.readMultiSeek(4);
        }

        public void writeInfo()
        {
            AbstractRom rom = hm.rom;

            rom.seek(address);

            rom.writeSeek(type);
            rom.writeSeek(sprite, 2);
            rom.writeSeek(direction);
            rom.writeSeek(movement, 2);
            rom.writeSeek(eventFlag, 2);
            rom.writeSeek(showSprite);
            rom.writeSeek(pointer, 4);
            rom.writeSeek(secPointer, 4);
        }

        public String toString()
        {
            if (sprite == 0)
                return "Nothing";
            else
                return sptNames[sprite];
        }

        /**
         * @return Returns the direction.
         */
        public int getDirection()
        {
            return direction;
        }

        /**
         * @param direction The direction to set.
         */
        public void setDirection(int direction)
        {
            this.direction = direction;
        }

        /**
         * @return Returns the eventFlag.
         */
        public int getEventFlag()
        {
            return eventFlag;
        }

        /**
         * @param eventFlag The eventFlag to set.
         */
        public void setEventFlag(int eventFlag)
        {
            this.eventFlag = eventFlag;
        }

        /**
         * @return Returns the item.
         */
        public int getItem()
        {
            return getSecPointer() & 255;
        }

        /**
         * @param item The item to set.
         */
        public void setItem(int item)
        {
            this.secPointer = item & 255;
        }

        /**
         * @return Returns the money.
         */
        public int getMoney()
        {
            return getSecPointer() - 0x100;
        }

        /**
         * @param money The money to set.
         */
        public void setMoney(int money)
        {
            this.secPointer = money + 0x100;
        }

        /**
         * @return Returns the movement.
         */
        public int getMovement()
        {
            return movement;
        }

        /**
         * @param movement The movement to set.
         */
        public void setMovement(int movement)
        {
            this.movement = movement;
        }

        /**
         * @return Returns the pointer.
         */
        public int getPointer()
        {
            return pointer;
        }

        /**
         * @param pointer The pointer to set.
         */
        public void setPointer(int pointer)
        {
            this.pointer = pointer;
        }

        /**
         * @return Returns the secPointer.
         */
        public int getSecPointer()
        {
            return secPointer;
        }

        /**
         * @param secPointer The secPointer to set.
         */
        public void setSecPointer(int secPointer)
        {
            this.secPointer = secPointer;
        }

        /**
         * @return Returns the showSprite.
         */
        public int getShowSprite()
        {
            return showSprite;
        }

        /**
         * @param showSprite The showSprite to set.
         */
        public void setShowSprite(int showSprite)
        {
            this.showSprite = showSprite;
        }

        /**
         * @return Returns the sprite.
         */
        public int getSprite()
        {
            return sprite;
        }

        /**
         * @param sprite The sprite to set.
         */
        public void setSprite(int sprite)
        {
            this.sprite = sprite;
        }

        /**
         * @return Returns the type.
         */
        public int getType()
        {
            return type;
        }

        /**
         * @param type The type to set.
         */
        public void setType(int type)
        {
            this.type = type;
        }

        /**
         * @return Returns the address.
         */
        public int getAddress()
        {
            return address;
        }

        /**
         * @return Returns the num.
         */
        public int getNum()
        {
            return num;
        }

    }

    public static TPTEntry[] tptEntries = new TPTEntry[NUM_ENTRIES];

    public void show(Object in)
    {
        show();
        int i;
        try
        {
            i = ((Integer) in).intValue();
        }
        catch (RuntimeException e)
        {
            i = Integer.parseInt(in.toString());
        }
        selector.setSelectedIndex(i);
    }

    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < tptEntries.length; i++)
        {
            tptEntries[i] = new TPTEntry(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }
    private JComboBox selector, type, direction, showSprite;
    private JLinkComboBox sprite, item;
    private JTextField money, eventFlag, movement;
    private TextEditor.TextOffsetEntry pointer, secPointer;
    private JLabel spritePreview;
    private JCheckBox moneyCheckBox;

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setResizable(false);

        Box main = new Box(BoxLayout.Y_AXIS);

        main.add(new JSearchableComboBox(selector = HackModule.createComboBox(
            tptEntries, this), "Entry: "));
        selector.setActionCommand("TPTSelector");

        main
            .add(sprite = new JLinkComboBox(SPTEditor.class, sptNames, "Sprite"));
        sprite.addActionListener(this);
        sprite.setActionCommand("spriteSelector");

        main.add(item = new ItemEditor.ItemEntry("Item", this, this, -1,
            JLinkComboBox.SEARCH_LEFT));
        item.setActionCommand("itemSelector");

        JPanel bottom = new JPanel(new BorderLayout());

        spritePreview = new JLabel();
        Box temp = new Box(BoxLayout.Y_AXIS);
        temp.add(Box.createVerticalGlue());
        temp.add(HackModule.createFlowLayout(spritePreview));
        temp.add(Box.createVerticalGlue());
        bottom.add(temp, BorderLayout.CENTER);

        Box entry = new Box(BoxLayout.Y_AXIS);

        entry.add(pairComponents(moneyCheckBox = new JCheckBox("Money: "),
            money = new JTextField(6), true));
        moneyCheckBox.setActionCommand("moneyCheckBox");
        moneyCheckBox.addActionListener(this);

        entry.add(HackModule.getLabeledComponent("Type: ", type = HackModule
            .createJComboBoxFromArray(new String[]{"[1] Person", "[2] Item",
                "[3] Object"})));
        type.setActionCommand("typeSelector");
        type.addActionListener(this);

        //        entry.add(
        //            HackModule.getLabeledComponent(
        //                "Pointer: $",
        //                pointer = HackModule.createSizedJTextField(6)));
        //        entry.add(
        //            HackModule.getLabeledComponent(
        //                "Secondary Pointer: $",
        //                secPointer = HackModule.createSizedJTextField(6)));
        entry.add(pointer = new TextEditor.TextOffsetEntry("Pointer", true));
        entry.add(secPointer = new TextEditor.TextOffsetEntry(
            "Secondary Pointer", true));

        entry.add(HackModule.getLabeledComponent("Movement: ",
            movement = HackModule.createSizedJTextField(5)));

        entry.add(HackModule.getLabeledComponent("Direction: ",
            direction = HackModule.createJComboBoxFromArray(new String[]{"Up",
                "Up-Right", "Right", "Down-Right", "Down", "Down-Left", "Left",
                "Up-Left"})));
        direction.setActionCommand("DirectionSelector");
        direction.addActionListener(this);

        entry.add(HackModule.getLabeledComponent("Event Flag: ",
            eventFlag = HackModule.createSizedJTextField(5)));

        entry.add(HackModule.getLabeledComponent("Show Sprite: ",
            showSprite = new JComboBox(new String[]{"Always (Gifts)",
                "When OFF", "When ON"})));

        bottom.add(entry, BorderLayout.EAST);
        //bottom.add(Box.createHorizontalStrut(20), BorderLayout.CENTER);

        main.add(bottom);

        mainWindow.getContentPane().add(main, BorderLayout.CENTER);
        mainWindow.pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.2";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "TPT Editor";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Based on editor in Mr. A's Map Editor";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        readFromRom();
        super.show();

        SpriteEditor.readFromRom(rom);
        int i = selector.getSelectedIndex();
        selector.setSelectedIndex(i == -1 ? 0 : i);
        selector.updateUI();
        mainWindow.pack();

        mainWindow.setVisible(true);
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

    private Image zoomImage(BufferedImage in, float zoom)
    {
        return in.getScaledInstance((int) (in.getWidth() * zoom), (int) (in
            .getHeight() * zoom), 0);
    }

    private void updateSpritePreview(int spt, int dir)
    {
        if (spt == 0)
        {
            this.spritePreview.setIcon(new ImageIcon());
            return;
        }
        SpriteEditor.SpriteInfoBlock sib = SpriteEditor.sib[spt];
        int d = dir;
        if (sib.numSprites > 9)
            switch (dir)
            {
                case 0:
                    d = 0;
                    break;
                case 1:
                    d = 8;
                    break;
                case 2:
                    d = 2;
                    break;
                case 3:
                    d = 10;
                    break;
                case 4:
                    d = 4;
                    break;
                case 5:
                    d = 12;
                    break;
                case 6:
                    d = 6;
                    break;
                case 7:
                    d = 14;
                    break;
            }
        this.spritePreview
            .setIcon(new ImageIcon(zoomImage(new SpriteEditor.Sprite(sib
                .getSpriteInfo(d), this).getImage(), 2)));
    }

    private void showInfo(int i)
    {
        TPTEntry e = tptEntries[i];

        this.sprite.setSelectedIndex(e.getSprite());
        this.moneyCheckBox.setSelected(e.getMoney() >= 0);
        this.type.setSelectedIndex(e.getType() - 1);
        this.pointer.setOffset(e.getPointer());
        this.secPointer.setOffset(e.getSecPointer());
        this.item.setSelectedIndex(e.getItem());
        this.money.setText(Integer.toString(e.getMoney()));
        this.movement.setText(Integer.toString(e.getMovement()));
        this.direction.setSelectedIndex(e.getDirection());

        String temp = addZeros(Integer.toHexString(e.getEventFlag()), 4);
        temp = temp.substring(2, 4) + " " + temp.substring(0, 2);
        this.eventFlag.setText(temp);

        this.showSprite.setSelectedIndex(e.getShowSprite());

        updateSpritePreview(e.getSprite(), e.getDirection());

        mainWindow.repaint();
    }

    private void saveInfo(int i)
    {
        TPTEntry e = tptEntries[i];

        e.setSprite(sprite.getSelectedIndex());
        e.setType(type.getSelectedIndex() + 1);
        e.setPointer(pointer.getOffset());
        if (e.getType() != TPTEntry.TYPE_ITEM)
        {
            e.setSecPointer(secPointer.getOffset());
        }
        else
        {
            if (moneyCheckBox.isSelected())
            {
                e.setMoney(Integer.parseInt(money.getText()));
            }
            else
            {
                e.setItem(item.getSelectedIndex());
            }
        }
        e.setMovement(Integer.parseInt(movement.getText()));
        e.setDirection(direction.getSelectedIndex());

        String temp = killSpaces(eventFlag.getText());
        temp = temp.substring(2, 4) + temp.substring(0, 2);
        e.setEventFlag(Integer.parseInt(temp, 16));

        e.setShowSprite(showSprite.getSelectedIndex());

        e.writeInfo();
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals(type.getActionCommand()))
        {
            if (type.getSelectedIndex() + 1 == TPTEntry.TYPE_ITEM)
            {
                item.setEnabled(!moneyCheckBox.isSelected());
                money.setEnabled(moneyCheckBox.isSelected());
                moneyCheckBox.setEnabled(true);
                secPointer.setEnabled(false);
            }
            else
            {
                item.setEnabled(false);
                money.setEnabled(false);
                moneyCheckBox.setEnabled(false);
                secPointer.setEnabled(true);
            }
        }
        else if (ae.getActionCommand().equals(moneyCheckBox.getActionCommand()))
        {
            if (type.getSelectedIndex() + 1 == TPTEntry.TYPE_ITEM)
            {
                item.setEnabled(!moneyCheckBox.isSelected());
                money.setEnabled(moneyCheckBox.isSelected());
            }
        }
        else if (ae.getActionCommand().equals(sprite.getActionCommand())
            || ae.getActionCommand().equals(direction.getActionCommand()))
        {
            if (direction.getSelectedIndex() == -1)
                return;
            updateSpritePreview(sprite.getSelectedIndex(), direction
                .getSelectedIndex());
        }
        else if (ae.getActionCommand().equalsIgnoreCase("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("apply"))
        {
            saveInfo(selector.getSelectedIndex());
        }
    }
}