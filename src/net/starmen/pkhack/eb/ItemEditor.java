package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.starmen.pkhack.AutoSearchBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JLinkComboBox;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.XMLPreferences;

/**
 * Class providing GUI and API for editing the items in Earthbound.
 * 
 * @author AnyoneEB, EBisumaru
 */
public class ItemEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public ItemEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    /**
     * Array holding all the items.
     * 
     * @see #readFromRom(HackModule)
     */
    public static Item[] items = new Item[254];
    private static Icon closedPresentIcon = initIcon();

    //GUI components
    private JComboBox itemSelector, typeSel;
    private ActionEditor.ActionEntry effectSelector;
    private JTextField name, cost, strength, extraPower, epIncrease, special,
    //	helpAdd,
            search;
    private AutoSearchBox type;
    private TextEditor.TextOffsetEntry helpAdd;
    private JCheckBox ness, paula, jeff, poo, noGive, unknown, chicken,
            infinite;
    //labels for relabelable components
    private JLabel strLabel, epLabel, incLabel, specialLabel;

    //auxillary panels
    private JPanel foodAux, armorAux, brokenAux;
    private AutoSearchBox recoverType, fixedItem;
    private JRadioButton[] protect;
    private JPanel[] protectSections;
    private ButtonGroup[] bg;
    //    private JButton selectItem; //this isn't working and I'm tired of it.

    private final DocumentListener protectSetter = new DocumentListener()
    {
        public void changedUpdate(DocumentEvent de)
        {
            try
            {
                setRadioButtons(Integer.parseInt(special.getText()));
            }
            catch (NumberFormatException nfe)
            {}
            catch (ArrayIndexOutOfBoundsException ae)
            {}
        }

        public void insertUpdate(DocumentEvent de)
        {
            changedUpdate(de);
        }

        public void removeUpdate(DocumentEvent de)
        {
            changedUpdate(de);
        }
    };
    /**
     * Array of labels used for str, ep, inc, and special. Access by
     * <code>labels[0=str, 1=ep, 2=inc, 3=special][type]</code>.
     */
    protected String[][] labels = null;

    /** Inits {@link #labels}. */
    protected void initLabels()
    {
        labels = new String[4][256];
        Arrays.fill(labels[0], "Strength (N/A): ");
        Arrays.fill(labels[1], "Extra Power (N/A): ");
        Arrays.fill(labels[2], "E.P. Increase (N/A): ");
        Arrays.fill(labels[3], "Special (N/A): ");

        //exceptions
        //type 4 - buyable character - 0000 0100
        labels[0][4] = "PST entry + 1: ";
        labels[1][4] = "Unknown(E.P.): ";
        labels[2][4] = "Unknown(E.P.inc): ";

        //type 8 - broken item - 0000 1000
        labels[1][8] = "Fixed Item: ";
        labels[2][8] = "IQ Required: ";

        //food items
        //type 32 - 0010 0000
        labels[0][32] = "Recovery Type: ";
        labels[1][32] = "Poo Increase: ";
        labels[2][32] = "Increase: ";
        labels[3][32] = "Skip Sandwich Effect Time: ";

        //type 36 - 0010 0100
        labels[0][36] = "Recovery Type: ";
        labels[1][36] = "Poo Increase: ";
        labels[2][36] = "Increase: ";
        labels[3][36] = "Skip Sandwich Effect Time: ";

        //type 40 - 0010 1000
        labels[0][40] = "Recovery Type: ";
        labels[1][40] = "Poo Increase: ";
        labels[2][40] = "Increase: ";
        labels[3][40] = "Skip Sandwich Effect Time: ";

        //type 44 - 0010 1100
        labels[0][44] = "Recovery Type: ";
        labels[1][44] = "Poo Increase: ";
        labels[2][44] = "Increase: ";
        labels[3][44] = "Skip Sandwich Effect Time: ";

        //armor items
        //type 20 - body - 0001 0100
        labels[0][20] = "Defense: ";
        labels[1][20] = "Speed: ";
        labels[2][20] = "4th Character Def";
        labels[3][20] = "Protection: ";

        //type 24 - arm - 0001 1000
        labels[0][24] = "Defense: ";
        labels[1][24] = "Luck: ";
        labels[2][24] = "4th Character Def";
        labels[3][24] = "Protection: ";

        //type 28 - other - 0001 1100
        labels[0][28] = "Defense: ";
        labels[1][28] = "Luck: ";
        labels[2][28] = "4th Character Def";
        labels[3][28] = "Protection: ";

        //weapon items
        //type 16 - 0001 0000
        labels[0][16] = "Offense: ";
        labels[1][16] = "Guts: ";
        labels[2][16] = "Poo Offense: ";
        labels[3][16] = "Miss Rate: (/16)";

        //type 17 - 0001 0001
        labels[0][17] = "Offense: ";
        labels[1][17] = "Guts(?): ";
        labels[2][17] = "Poo Offense(?): ";
        labels[3][17] = "Miss Rate: (/16)";
    }

    protected void init()
    {
        readFromRom();

        initLabels();

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
        //mainWindow.setSize(400, 320);
        mainWindow.setResizable(false);

        JPanel itemStats = new JPanel();
        itemStats.setLayout(new BoxLayout(itemStats, BoxLayout.Y_AXIS));

        JPanel upper = new JPanel();
        upper.setLayout(new GridLayout(3, 2));
        //		upper.add(new JLabel("Item:"));
        itemSelector = createItemComboBox(this, this);
        itemSelector.setActionCommand("itemSelect");

        JButton searchb = new JButton("Find");
        searchb.addActionListener(this);

        mainWindow.getContentPane().add(
            new JSearchableComboBox(itemSelector, "Item: "),
            //			pairComponents(
            //				pairComponents(search = new JTextField(10), searchb, true),
            //				getLabeledComponent("Item:", itemSelector),
            //				true),
            BorderLayout.NORTH);
        //		upper.add(new JLabel("Name:"));
        this.name = createSizedJTextField(25);
        upper.add(FontEditor.StringViewer.createWithFontSelector(name, this));
        upper.add(getLabeledComponent("Name:", this.name));
        //		upper.add(new JLabel("Effect:"));
        upper.add(effectSelector = new ActionEditor.ActionEntry("Effect"));
        itemStats.add(upper);

        JPanel lower = new JPanel();
        lower.setLayout(new BoxLayout(lower, BoxLayout.X_AXIS));

        JPanel lowerLeft = new JPanel();
        lowerLeft.setLayout(new BoxLayout(lowerLeft, BoxLayout.Y_AXIS));
        lowerLeft.add(getLabeledComponent(
            "Cost (0 to make unsellable and undroppable):",
            this.cost = createSizedJTextField(5, true)));

        typeSel = new JComboBox(itemTypes);

        lowerLeft.add(this.type = new AutoSearchBox(typeSel,
            createSizedJTextField(3, true), "Type", true, true, true));
        this.type.getTF().getDocument().addDocumentListener(
            new DocumentListener()
            {

                public void changedUpdate(DocumentEvent de)
                {
                    try
                    {
                        setLabels(Integer.parseInt(type.getText()));
                    }
                    catch (NumberFormatException nfe)
                    {}
                    catch (ArrayIndexOutOfBoundsException ae)
                    {}
                }

                public void insertUpdate(DocumentEvent de)
                {
                    changedUpdate(de);
                }

                public void removeUpdate(DocumentEvent de)
                {
                    changedUpdate(de);
                }
            });
        lowerLeft.add(pairComponents(strLabel = new JLabel(labels[0][255]),
            this.strength = createSizedJTextField(3, true), true));
        strength.addActionListener(this);
        strength.setActionCommand("strength");
        lowerLeft.add(pairComponents(epLabel = new JLabel(labels[1][255]),
            this.extraPower = createSizedJTextField(3, true), true));
        lowerLeft.add(pairComponents(incLabel = new JLabel(labels[2][255]),
            this.epIncrease = createSizedJTextField(3, true), true));
        lowerLeft.add(pairComponents(specialLabel = new JLabel(labels[3][255]),
            this.special = createSizedJTextField(3, true), true));
        special.getDocument().addDocumentListener(protectSetter);
        //		lowerLeft.add(
        //			getLabeledComponent(
        //				"Help Text Address $:",
        //				this.helpAdd = new JTextField(8)));
        lowerLeft.add(this.helpAdd = new TextEditor.TextOffsetEntry(
            "Help Text Address", true));
        lower.add(lowerLeft);

        lower.add(Box.createHorizontalStrut(5));

        JPanel ownerChecks = new JPanel();
        ownerChecks.setLayout(new BoxLayout(ownerChecks, BoxLayout.Y_AXIS));
        ownerChecks.add(new JLabel("Who can use?"));
        ness = new JCheckBox("Ness");
        paula = new JCheckBox("Paula");
        jeff = new JCheckBox("Jeff");
        poo = new JCheckBox("Poo");
        ownerChecks.add(ness);
        ownerChecks.add(paula);
        ownerChecks.add(jeff);
        ownerChecks.add(poo);
        lower.add(ownerChecks);

        lower.add(Box.createHorizontalStrut(5));

        JPanel specialFlags = new JPanel();
        specialFlags.setLayout(new BoxLayout(specialFlags, BoxLayout.Y_AXIS));
        specialFlags.add(new JLabel("Misc. Options"));
        noGive = new JCheckBox("Disallow Give");
        unknown = new JCheckBox("Unknown");
        chicken = new JCheckBox("Chicken (Transformable)");
        infinite = new JCheckBox("Infinite Use");
        specialFlags.add(noGive);
        specialFlags.add(unknown);
        specialFlags.add(chicken);
        specialFlags.add(infinite);
        lower.add(specialFlags);

        itemStats.add(lower);

        foodAux = new JPanel();

        recoverType = new AutoSearchBox(new JComboBox(new String[]{"0 HP",
            "1 PP", "2 HP & PP", "3 Random IQ-Luck", "4 IQ", "5 Guts",
            "6 Speed", "7 Vitality", "8 Luck", "9 Cold cure", "10 Poison cure",
            "11 Nothing"}), strength, "Recovery Type", true, false, true);
        foodAux.add(recoverType);

        brokenAux = new JPanel();

        fixedItem = new AutoSearchBox(createDecItemComboBox(this, this),
            extraPower, "Fixed Item", true, false);
        fixedItem.setNumberIndex(1);
        brokenAux.add(fixedItem);

        /*
         * selectItem = new JButton("Edit this item");
         * selectItem.setActionCommand("editCurr");
         * selectItem.addActionListener(this); brokenAux.add(selectItem);
         */

        protect = new JRadioButton[16];
        protectSections = new JPanel[4];
        bg = new ButtonGroup[4];

        for (int i = 0; i < 4; i++)
        {
            protectSections[i] = new JPanel();
            bg[i] = new ButtonGroup();
        }

        for (int i = 0; i < 16; i++)
        {
            protect[i] = new JRadioButton(i % 4 + "");
            protectSections[i / 4].add(protect[i]);
            bg[i / 4].add(protect[i]);
            protect[i].addActionListener(this);
            protect[i].setActionCommand("protect");
        }

        JPanel mid = new JPanel(), low = new JPanel(), high = new JPanel();
        mid.setLayout(new FlowLayout());
        high.setLayout(new FlowLayout());
        low.setLayout(new FlowLayout());

        armorAux = new JPanel();
        armorAux.setLayout(new BoxLayout(armorAux, BoxLayout.Y_AXIS));

        high.add(getLabeledComponent("Paralysis", protectSections[0]));
        high.add(getLabeledComponent("Flash", protectSections[1]));
        mid.add(getLabeledComponent("Freeze", protectSections[2]));
        mid.add(getLabeledComponent("Fire", protectSections[3]));
        low.add(new JLabel("Set Fire to 1 or 2 and the rest to 0 for "
            + "Sleep protection."));

        armorAux.add(high);
        armorAux.add(mid);
        armorAux.add(low);

        JPanel aux = new JPanel();
        aux.setLayout(new BoxLayout(aux, BoxLayout.Y_AXIS));
        aux.add(armorAux);
        aux.add(foodAux);
        aux.add(brokenAux);
        itemStats.add(aux);

        armorAux.setVisible(false);
        foodAux.setVisible(false);
        brokenAux.setVisible(false);

        mainWindow.getContentPane().add(itemStats, BorderLayout.CENTER);

        mainWindow.pack();
    }

    /**
     * Makes a <code>JComboBox</code> into an item selector. Removes all items
     * first. NOTE: This changes the ComboBox to use a non-mutable
     * ComboBoxModel, if you want to change the contents afterwards, you need to
     * change the ComoBoxModel.
     * 
     * @param out <code>JComboBox</code> to add the items too.
     * @return <code>out</code> with a list of the items in the ROM as it's
     *         contents.
     */
    public static JComboBox createItemSelector(JComboBox out, HackModule hm)
    {
        ItemEditor.readFromRom(hm);
        out.setModel(createItemComboBoxModel());
        return out;
    }

    /**
     * Makes a <code>JComboBox</code> into an item selector for items of the
     * specified type. Removes all items first.
     * 
     * @param out <code>JComboBox</code> to add the items too.
     * @param type Item type to display.
     * @return <code>out</code> with a list of the items in the ROM as it's
     *         contents.
     */
    public static JComboBox createItemSelectorForType(JComboBox out, int type,
        HackModule hm)
    {
        ItemEditor.readFromRom(hm);
        out.removeAllItems();
        ArrayList tmp = new ArrayList();
        for (int i = 0; i < items.length; i++)
        {
            if (items[i].type == type)
                out.addItem(HackModule
                    .getNumberedString(items[i].toString(), i));
        }
        return out;
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.2";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Item Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Based on source code by Tomato\n"
            + "Str/EP/EPinc/Special meanings by EBisumaru\n"
            + "Type/Aux Windows Coolification by EBisumaru";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        readFromRom();
        itemSelector.setSelectedIndex(itemSelector.getSelectedIndex() == -1
            ? 0
            : itemSelector.getSelectedIndex());

        mainWindow.setVisible(true);
    }

    /**
     * Reads the items from the ROM into the {@link #items}array.
     * 
     * @see #items
     * @see ItemEditor.Item
     */
    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * Class that represents an entry in the Earthbound items table.
     */
    public static class Item
    {
        private HackModule hm;
        /**
         * Address of this item entry in the ROM.
         */
        public int address;
        /**
         * Name of the item this entry represents.
         */
        public String name;
        /**
         * What number item this is.
         */
        public int number;
        /**
         * What type item this is. Look elsewhere for what different values of
         * this means in the ROM.
         */
        public int type;
        /**
         * Cost of this item in dollars. Sells for 1/3.
         */
        public int cost;
        /**
         * Who can hold this item and some other info. <br>
         * <br>
         * Ownership format (written by Tomato): <br>
         * This is somewhat more complex than other things, so pay special
         * attention. Ownership refers to who can use what item, and it also
         * goes so far as to specify who can hold what items. I'll give some
         * examples later. <br>
         * <br>
         * Ownership is 8 bits long. <br>
         * bit: _ _ _ _ _ _ _ _<br>
         * bit #: 8 7 6 5 4 3 2 1 <br>
         * <br>
         * Bit 1 determines if Ness can use this item <br>
         * Bit 2 determines if Paula can use this item <br>
         * Bit 3 determines if Jeff can use this item <br>
         * Bit 4 determines if Poo can use this item <br>
         * <br>
         * Bit 5 is wether the item is in the item transformation table. <br>
         * Bit 6 and 7 are weird. If both are 1, then once you get an item, you
         * can't give them to anybody else in your party (and obviously to any
         * non-playable characters). <br>
         * Bit 8 signifies if the item disappears after use or not.
         */
        public int ownership;
        /**
         * Effect of using this item.
         * 
         * @see EbHackModule#effects
         */
        public int effect;
        /**
         * How strong the item is. I'm not sure exactly what this does: ask
         * someone else :).
         */
        public int strength;
        /**
         * I'm not sure exactly what this does: ask someone else :).
         */
        public int increase;
        /**
         * I'm not sure exactly what this does: ask someone else :).
         */
        public int extraPower;
        /**
         * I'm not sure exactly what this does: ask someone else :).
         */
        public int specialProperties;
        /**
         * SNES address of the help text for this item.
         */
        public int descAddress;

        /**
         * Creates a new <code>Item</code> from it's number by reading from
         * the ROM.
         */
        public Item(int itemNumber, HackModule hm)
        {
            this.hm = hm;
            AbstractRom rom = hm.rom;
            this.number = itemNumber;

            this.address = 0x155200 + (itemNumber * 39);
            rom.seek(this.address);

            //            for (int j = 0; j < 25; j++)
            //            {
            //                this.name[j] = simpToRegChar(rom.readCharSeek());
            //            }
            this.name = hm.readSeekRegString(25);
            this.type = rom.readSeek();
            this.cost = rom.readMultiSeek(2);
            this.ownership = rom.readSeek();
            this.effect = rom.readMultiSeek(2);
            this.strength = rom.readSeek();
            this.increase = rom.readSeek();
            this.extraPower = rom.readSeek();
            this.specialProperties = rom.readSeek();
            this.descAddress = rom.readMultiSeek(4);
        }

        /**
         * Returns a <code>String</code> to show what item this is.
         * 
         * @return A <code>String</code> with the item number in []'s and the
         *         item name.
         */
        public String toString() //gets name as regular text
        {
            /*
             * return HackModule.getNumberedString( new
             * String(this.name).trim(),
             */
            return new String(this.name).trim();
        }

        /**
         * Writes the information stored by this <code>Item</code> into the
         * ROM. Will not write to item #0.
         */
        public void writeInfo()
        {
            //don't overwrite null item
            if (number == 0)
                return;

            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            //            for (int j = 0; j < 25; j++)
            //            {
            //                rom.writeSeek(simpToEbChar(this.name[j]));
            //            }
            hm.writeSeekRegString(25, this.name);
            rom.writeSeek(this.type);
            rom.writeSeek(this.cost, 2);
            rom.writeSeek(this.ownership);
            rom.writeSeek(this.effect, 2);
            rom.writeSeek(this.strength);
            rom.writeSeek(this.increase);
            rom.writeSeek(this.extraPower);
            rom.writeSeek(this.specialProperties);
            rom.writeSeek(this.descAddress, 4);
        }
    }

    private void saveItemInfo(int i)
    //puts the changed info into the Item
    // object
    {
        if (i < 0)
            return;

        //        char[] tfName = this.name.getText().toCharArray();
        //        for (int j = 0; j < items[i].name.length; j++)
        //        {
        //            items[i].name[j] = (j < tfName.length ? tfName[j] : (char) 0);
        //        }
        items[i].name = this.name.getText();
        ItemEditor.notifyItemDataListeners(new ListDataEvent(this,
            ListDataEvent.CONTENTS_CHANGED, i, i));

        items[i].effect = this.effectSelector.getSelectedIndex() & 0xffff;
        items[i].cost = numberize(this.cost.getText()) & 0xffff;
        items[i].type = numberize(typeSel.getSelectedItem().toString()) & 0xff;
        items[i].strength = numberize(strength.getText()) & 0xff;
        items[i].extraPower = numberize(this.extraPower.getText()) & 0xff;
        items[i].increase = numberize(this.epIncrease.getText()) & 0xff;
        items[i].specialProperties = numberize(this.special.getText()) & 0xff;
        items[i].descAddress = this.helpAdd.getOffset();

        items[i].ownership = 0;
        items[i].ownership += (this.ness.isSelected() ? 1 : 0) << 0;
        items[i].ownership += (this.paula.isSelected() ? 1 : 0) << 1;
        items[i].ownership += (this.jeff.isSelected() ? 1 : 0) << 2;
        items[i].ownership += (this.poo.isSelected() ? 1 : 0) << 3;
        items[i].ownership += (this.chicken.isSelected() ? 1 : 0) << 4;
        items[i].ownership += (this.noGive.isSelected() ? 1 : 0) << 5;
        items[i].ownership += (this.unknown.isSelected() ? 1 : 0) << 6;
        items[i].ownership += (!this.infinite.isSelected() ? 1 : 0) << 7;

        items[i].writeInfo();
    }

    private void showItemInfo(int i)
    {
        if (i < 0)
            return;
        //assumes that itemSelector was changed to i
        //set text boxes and combo boxes
        this.name.setText(items[i].name.trim());
        this.effectSelector.setSelectedIndex(items[i].effect);
        effectSelector.repaint();
        this.cost.setText(Integer.toString(items[i].cost));
        this.type.setText(Integer.toString(items[i].type));
        this.strength.setText(Integer.toString(items[i].strength));
        this.extraPower.setText(Integer.toString(items[i].extraPower));
        this.epIncrease.setText(Integer.toString(items[i].increase));
        this.special.setText(Integer.toString(items[i].specialProperties));
        this.helpAdd.setOffset(items[i].descAddress);

        this.ness.setSelected(getBit(items[i].ownership, 0));
        this.paula.setSelected(getBit(items[i].ownership, 1));
        this.jeff.setSelected(getBit(items[i].ownership, 2));
        this.poo.setSelected(getBit(items[i].ownership, 3));
        this.chicken.setSelected(getBit(items[i].ownership, 4));
        this.noGive.setSelected(getBit(items[i].ownership, 5));
        this.unknown.setSelected(getBit(items[i].ownership, 6));
        this.infinite.setSelected(!getBit(items[i].ownership, 7));
        //bit is true if not infinite

        setLabels(items[i].type);
    }

    private boolean getBit(int i, int b) //return true if bit #b = 1 (#'s 0-7)
    {
        return ((i >> b) & 1) == 1;
    }

    private void setLabels(int type)
    {
        type &= 0xff;
        this.strLabel.setText(this.labels[0][type]);
        this.epLabel.setText(this.labels[1][type]);
        this.incLabel.setText(this.labels[2][type]);
        this.specialLabel.setText(this.labels[3][type]);
        //aux windows
        armorAux.setVisible(false);
        foodAux.setVisible(false);
        brokenAux.setVisible(false);
        recoverType.setCorr(false);
        fixedItem.setCorr(false);
        if ((type == 32) || (type == 36) || (type == 40) || (type == 44))
        {
            foodAux.setVisible(true);
            recoverType.setCorr(true);
        }
        else if ((type == 20) || (type == 24) || (type == 28))
        {
            armorAux.setVisible(true);
        }
        else if ((type == 8))
        {
            brokenAux.setVisible(true);
            fixedItem.setCorr(true);
        }
        mainWindow.pack();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(itemSelector.getActionCommand()))
        {
            if (itemSelector.getSelectedIndex() < 0)
                return;
            showItemInfo(itemSelector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equalsIgnoreCase("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("apply"))
        {
            //put text field info into the Item object, then call
            // items[i].writeInfo();
            int i = itemSelector.getSelectedIndex();
            if (i < 0)
                return;
            if (i == 0)
                JOptionPane.showMessageDialog(mainWindow,
                    "Writing to item #0 is not allowed.", "Write Failed",
                    JOptionPane.WARNING_MESSAGE);
            else
                saveItemInfo(i);
        }
        else if (ae.getActionCommand().equals("Find"))
        {
            search(search.getText().toLowerCase(), itemSelector);
        }
        else if (ae.getActionCommand().equals("protect"))
            setSpecial();
        /*
         * else if (ae.getActionCommand().equals("editCurr")); {
         * if(!(extraPower.getText().equals(null))) {
         * itemSelector.setSelectedIndex(numberize(extraPower.getText()));
         * showItemInfo(numberize(extraPower.getText())); } }
         */
    }

    public static SimpleComboBoxModel createItemComboBoxModel()
    {
        SimpleComboBoxModel out = new SimpleComboBoxModel()
        {
            public int getSize()
            {
                return items.length;
            }

            public Object getElementAt(int i)
            {
                try
                {
                    return HackModule.getNumberedString(items[i].toString(), i);
                }
                catch (NullPointerException e)
                {
                    return HackModule.getNumberedString("Null", 0);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    return getElementAt(0);
                }
            }
        };
        addItemDataListener(out);

        return out;
    }

    public static SimpleComboBoxModel createDecItemComboBoxModel()
    {
        SimpleComboBoxModel out = new SimpleComboBoxModel()
        {
            public int getSize()
            {
                return items.length;
            }

            public Object getElementAt(int i)
            {
                try
                {
                    return HackModule.getNumberedString(items[i].toString(), i,
                        false);
                }
                catch (NullPointerException e)
                {
                    return HackModule.getNumberedString("Null", 0, false);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    return getElementAt(0);
                }
            }
        };
        addItemDataListener(out);

        return out;
    }
    private static ArrayList itemListeners = new ArrayList();

    protected static void addItemDataListener(ListDataListener ldl)
    {
        itemListeners.add(ldl);
    }

    protected static void removeItemDataListener(ListDataListener ldl)
    {
        itemListeners.remove(ldl);
    }

    protected static void notifyItemDataListeners(ListDataEvent lde)
    {
        for (Iterator i = itemListeners.iterator(); i.hasNext();)
        {
            ((ListDataListener) i.next()).contentsChanged(lde);
        }
    }

    public static JComboBox createItemComboBox(final ActionListener al,
        HackModule hm)
    {
        SimpleComboBoxModel model = createItemComboBoxModel();
        if (items[0] == null)
            readFromRom(hm);
        final JComboBox out = new JComboBox(model);
        if (al != null)
            out.addActionListener(al);
        model.addListDataListener(new ListDataListener()
        {

            public void contentsChanged(ListDataEvent lde)
            {
                if (out.getSelectedIndex() == -1)
                {
                    out.removeActionListener(al);
                    out.setSelectedIndex(lde.getIndex0());
                    out.addActionListener(al);
                }
            }

            public void intervalAdded(ListDataEvent arg0)
            {}

            public void intervalRemoved(ListDataEvent arg0)
            {}
        });

        return out;
    }

    public static JComboBox createDecItemComboBox(final ActionListener al,
        HackModule hm)
    {
        SimpleComboBoxModel model = createDecItemComboBoxModel();
        if (items[0] == null)
            readFromRom(hm);
        final JComboBox out = new JComboBox(model);
        if (al != null)
            out.addActionListener(al);
        model.addListDataListener(new ListDataListener()
        {

            public void contentsChanged(ListDataEvent lde)
            {
                if (out.getSelectedIndex() == -1)
                {
                    out.removeActionListener(al);
                    out.setSelectedIndex(lde.getIndex0());
                    out.addActionListener(al);
                }
            }

            public void intervalAdded(ListDataEvent arg0)
            {}

            public void intervalRemoved(ListDataEvent arg0)
            {}
        });

        return out;
    }

    /**
     * @see net.starmen.pkhack.HackModule#getIcon()
     */
    public Icon getIcon()
    {
        return closedPresentIcon;
    }

    private static Icon initIcon()
    {
        BufferedImage out = new BufferedImage(16, 16,
            BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = out.getGraphics();

        //Created by ImageFileToCode from net/starmen/pkhack/closepresent.gif
        g.setColor(new Color(255, 255, 255));
        g.setColor(new Color(58, 50, 47));
        g.drawLine(3, 0, 3, 0);
        g.drawLine(4, 0, 4, 0);
        g.setColor(new Color(58, 49, 50));
        g.drawLine(9, 0, 9, 0);
        g.drawLine(10, 0, 10, 0);
        g.setColor(new Color(55, 51, 50));
        g.drawLine(11, 0, 11, 0);
        g.setColor(new Color(70, 52, 6));
        g.drawLine(3, 1, 3, 1);
        g.setColor(new Color(211, 158, 124));
        g.drawLine(4, 1, 4, 1);
        g.setColor(new Color(125, 20, 24));
        g.drawLine(5, 1, 5, 1);
        g.setColor(new Color(125, 18, 34));
        g.drawLine(6, 1, 6, 1);
        g.setColor(new Color(94, 33, 38));
        g.drawLine(7, 1, 7, 1);
        g.drawLine(8, 1, 8, 1);
        g.setColor(new Color(255, 137, 103));
        g.drawLine(9, 1, 9, 1);
        g.setColor(new Color(213, 89, 55));
        g.drawLine(10, 1, 10, 1);
        g.setColor(new Color(65, 47, 47));
        g.drawLine(11, 1, 11, 1);
        g.setColor(new Color(70, 52, 6));
        g.drawLine(3, 2, 3, 2);
        g.setColor(new Color(93, 40, 6));
        g.drawLine(4, 2, 4, 2);
        g.setColor(new Color(195, 90, 94));
        g.drawLine(5, 2, 5, 2);
        g.setColor(new Color(195, 88, 104));
        g.drawLine(6, 2, 6, 2);
        g.setColor(new Color(94, 33, 38));
        g.drawLine(7, 2, 7, 2);
        g.setColor(new Color(164, 103, 108));
        g.drawLine(8, 2, 8, 2);
        g.setColor(new Color(213, 89, 55));
        g.drawLine(9, 2, 9, 2);
        g.setColor(new Color(143, 19, 0));
        g.drawLine(10, 2, 10, 2);
        g.setColor(new Color(65, 47, 47));
        g.drawLine(11, 2, 11, 2);
        g.setColor(new Color(56, 50, 50));
        g.drawLine(12, 2, 12, 2);
        g.drawLine(1, 3, 1, 3);
        g.setColor(new Color(59, 49, 50));
        g.drawLine(2, 3, 2, 3);
        g.setColor(new Color(255, 252, 251));
        g.drawLine(3, 3, 3, 3);
        g.setColor(new Color(62, 48, 48));
        g.drawLine(4, 3, 4, 3);
        g.setColor(new Color(125, 19, 29));
        g.drawLine(5, 3, 5, 3);
        g.drawLine(6, 3, 6, 3);
        g.setColor(new Color(94, 33, 38));
        g.drawLine(7, 3, 7, 3);
        g.drawLine(8, 3, 8, 3);
        g.drawLine(9, 3, 9, 3);
        g.drawLine(10, 3, 10, 3);
        g.setColor(new Color(160, 154, 140));
        g.drawLine(11, 3, 11, 3);
        g.setColor(new Color(255, 255, 241));
        g.drawLine(12, 3, 12, 3);
        g.setColor(new Color(59, 51, 38));
        g.drawLine(13, 3, 13, 3);
        g.setColor(new Color(59, 50, 41));
        g.drawLine(14, 3, 14, 3);
        g.setColor(new Color(55, 51, 50));
        g.drawLine(0, 4, 0, 4);
        g.setColor(new Color(255, 253, 253));
        g.drawLine(1, 4, 1, 4);
        g.setColor(new Color(255, 252, 253));
        g.drawLine(2, 4, 2, 4);
        g.setColor(new Color(59, 49, 48));
        g.drawLine(3, 4, 3, 4);
        g.setColor(new Color(62, 48, 48));
        g.drawLine(4, 4, 4, 4);
        g.setColor(new Color(195, 89, 99));
        g.drawLine(5, 4, 5, 4);
        g.drawLine(6, 4, 6, 4);
        g.setColor(new Color(94, 33, 38));
        g.drawLine(7, 4, 7, 4);
        g.setColor(new Color(164, 103, 108));
        g.drawLine(8, 4, 8, 4);
        g.setColor(new Color(94, 33, 38));
        g.drawLine(9, 4, 9, 4);
        g.setColor(new Color(164, 103, 108));
        g.drawLine(10, 4, 10, 4);
        g.setColor(new Color(58, 52, 38));
        g.drawLine(11, 4, 11, 4);
        g.drawLine(12, 4, 12, 4);
        g.setColor(new Color(161, 153, 140));
        g.drawLine(13, 4, 13, 4);
        g.setColor(new Color(255, 253, 244));
        g.drawLine(14, 4, 14, 4);
        g.setColor(new Color(56, 51, 48));
        g.drawLine(15, 4, 15, 4);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 5, 0, 5);
        g.setColor(new Color(59, 49, 48));
        g.drawLine(1, 5, 1, 5);
        g.setColor(new Color(62, 48, 48));
        g.drawLine(2, 5, 2, 5);
        g.setColor(new Color(194, 91, 95));
        g.drawLine(3, 5, 3, 5);
        g.drawLine(4, 5, 4, 5);
        g.setColor(new Color(62, 52, 27));
        g.drawLine(5, 5, 5, 5);
        g.setColor(new Color(59, 53, 31));
        g.drawLine(6, 5, 6, 5);
        g.setColor(new Color(158, 154, 142));
        g.drawLine(7, 5, 7, 5);
        g.setColor(new Color(56, 52, 40));
        g.drawLine(8, 5, 8, 5);
        g.setColor(new Color(82, 38, 39));
        g.drawLine(9, 5, 9, 5);
        g.setColor(new Color(170, 100, 110));
        g.drawLine(10, 5, 10, 5);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(11, 5, 11, 5);
        g.drawLine(12, 5, 12, 5);
        g.setColor(new Color(62, 49, 33));
        g.drawLine(13, 5, 13, 5);
        g.setColor(new Color(60, 50, 41));
        g.drawLine(14, 5, 14, 5);
        g.setColor(new Color(60, 50, 48));
        g.drawLine(15, 5, 15, 5);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 6, 0, 6);
        g.setColor(new Color(255, 252, 251));
        g.drawLine(1, 6, 1, 6);
        g.setColor(new Color(62, 48, 48));
        g.drawLine(2, 6, 2, 6);
        g.setColor(new Color(124, 21, 25));
        g.drawLine(3, 6, 3, 6);
        g.drawLine(4, 6, 4, 6);
        g.setColor(new Color(164, 154, 129));
        g.drawLine(5, 6, 5, 6);
        g.setColor(new Color(161, 155, 133));
        g.drawLine(6, 6, 6, 6);
        g.setColor(new Color(255, 255, 243));
        g.drawLine(7, 6, 7, 6);
        g.drawLine(8, 6, 8, 6);
        g.setColor(new Color(255, 242, 243));
        g.drawLine(9, 6, 9, 6);
        g.setColor(new Color(100, 30, 40));
        g.drawLine(10, 6, 10, 6);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 6, 11, 6);
        g.drawLine(12, 6, 12, 6);
        g.setColor(new Color(63, 50, 34));
        g.drawLine(13, 6, 13, 6);
        g.setColor(new Color(162, 152, 143));
        g.drawLine(14, 6, 14, 6);
        g.setColor(new Color(59, 49, 47));
        g.drawLine(15, 6, 15, 6);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 7, 0, 7);
        g.setColor(new Color(161, 152, 145));
        g.drawLine(1, 7, 1, 7);
        g.setColor(new Color(62, 50, 36));
        g.drawLine(2, 7, 2, 7);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(3, 7, 3, 7);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(4, 7, 4, 7);
        g.setColor(new Color(59, 52, 36));
        g.drawLine(5, 7, 5, 7);
        g.setColor(new Color(255, 253, 246));
        g.drawLine(6, 7, 6, 7);
        g.setColor(new Color(255, 252, 253));
        g.drawLine(7, 7, 7, 7);
        g.drawLine(8, 7, 8, 7);
        g.setColor(new Color(59, 53, 29));
        g.drawLine(9, 7, 9, 7);
        g.setColor(new Color(62, 51, 29));
        g.drawLine(10, 7, 10, 7);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 7, 11, 7);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 7, 12, 7);
        g.setColor(new Color(59, 52, 36));
        g.drawLine(13, 7, 13, 7);
        g.setColor(new Color(255, 253, 246));
        g.drawLine(14, 7, 14, 7);
        g.setColor(new Color(59, 49, 47));
        g.drawLine(15, 7, 15, 7);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 8, 0, 8);
        g.setColor(new Color(255, 253, 246));
        g.drawLine(1, 8, 1, 8);
        g.setColor(new Color(62, 50, 36));
        g.drawLine(2, 8, 2, 8);
        g.setColor(new Color(195, 91, 98));
        g.drawLine(3, 8, 3, 8);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(4, 8, 4, 8);
        g.setColor(new Color(161, 154, 138));
        g.drawLine(5, 8, 5, 8);
        g.setColor(new Color(59, 50, 43));
        g.drawLine(6, 8, 6, 8);
        g.setColor(new Color(58, 49, 50));
        g.drawLine(7, 8, 7, 8);
        g.drawLine(8, 8, 8, 8);
        g.setColor(new Color(161, 155, 131));
        g.drawLine(9, 8, 9, 8);
        g.setColor(new Color(164, 153, 131));
        g.drawLine(10, 8, 10, 8);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 8, 11, 8);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 8, 12, 8);
        g.setColor(new Color(59, 52, 36));
        g.drawLine(13, 8, 13, 8);
        g.setColor(new Color(161, 152, 145));
        g.drawLine(14, 8, 14, 8);
        g.setColor(new Color(59, 49, 47));
        g.drawLine(15, 8, 15, 8);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 9, 0, 9);
        g.setColor(new Color(160, 153, 145));
        g.drawLine(1, 9, 1, 9);
        g.setColor(new Color(60, 51, 36));
        g.drawLine(2, 9, 2, 9);
        g.setColor(new Color(195, 90, 97));
        g.drawLine(3, 9, 3, 9);
        g.setColor(new Color(125, 20, 27));
        g.drawLine(4, 9, 4, 9);
        g.setColor(new Color(155, 158, 131));
        g.drawLine(5, 9, 5, 9);
        g.setColor(new Color(255, 255, 236));
        g.drawLine(6, 9, 6, 9);
        g.setColor(new Color(59, 51, 38));
        g.drawLine(7, 9, 7, 9);
        g.setColor(new Color(161, 153, 140));
        g.drawLine(8, 9, 8, 9);
        g.setColor(new Color(157, 156, 135));
        g.drawLine(9, 9, 9, 9);
        g.setColor(new Color(255, 255, 232));
        g.drawLine(10, 9, 10, 9);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 9, 11, 9);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 9, 12, 9);
        g.setColor(new Color(62, 50, 36));
        g.drawLine(13, 9, 13, 9);
        g.setColor(new Color(255, 253, 246));
        g.drawLine(14, 9, 14, 9);
        g.setColor(new Color(59, 49, 48));
        g.drawLine(15, 9, 15, 9);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(0, 10, 0, 10);
        g.setColor(new Color(255, 254, 246));
        g.drawLine(1, 10, 1, 10);
        g.setColor(new Color(60, 51, 36));
        g.drawLine(2, 10, 2, 10);
        g.setColor(new Color(195, 90, 97));
        g.drawLine(3, 10, 3, 10);
        g.setColor(new Color(125, 20, 27));
        g.drawLine(4, 10, 4, 10);
        g.setColor(new Color(255, 255, 232));
        g.drawLine(5, 10, 5, 10);
        g.setColor(new Color(154, 156, 134));
        g.drawLine(6, 10, 6, 10);
        g.setColor(new Color(59, 51, 38));
        g.drawLine(7, 10, 7, 10);
        g.setColor(new Color(255, 254, 241));
        g.drawLine(8, 10, 8, 10);
        g.setColor(new Color(255, 255, 236));
        g.drawLine(9, 10, 9, 10);
        g.setColor(new Color(157, 157, 131));
        g.drawLine(10, 10, 10, 10);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 10, 11, 10);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 10, 12, 10);
        g.setColor(new Color(62, 50, 36));
        g.drawLine(13, 10, 13, 10);
        g.setColor(new Color(161, 152, 145));
        g.drawLine(14, 10, 14, 10);
        g.setColor(new Color(59, 49, 48));
        g.drawLine(15, 10, 15, 10);
        g.setColor(new Color(55, 51, 50));
        g.drawLine(0, 11, 0, 11);
        g.setColor(new Color(160, 153, 143));
        g.drawLine(1, 11, 1, 11);
        g.setColor(new Color(66, 48, 34));
        g.drawLine(2, 11, 2, 11);
        g.setColor(new Color(171, 100, 96));
        g.drawLine(3, 11, 3, 11);
        g.setColor(new Color(102, 31, 27));
        g.drawLine(4, 11, 4, 11);
        g.setColor(new Color(155, 158, 131));
        g.drawLine(5, 11, 5, 11);
        g.setColor(new Color(255, 255, 236));
        g.drawLine(6, 11, 6, 11);
        g.setColor(new Color(60, 52, 39));
        g.drawLine(7, 11, 7, 11);
        g.setColor(new Color(161, 153, 140));
        g.drawLine(8, 11, 8, 11);
        g.setColor(new Color(157, 156, 135));
        g.drawLine(9, 11, 9, 11);
        g.setColor(new Color(255, 255, 232));
        g.drawLine(10, 11, 10, 11);
        g.setColor(new Color(124, 20, 27));
        g.drawLine(11, 11, 11, 11);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 11, 12, 11);
        g.setColor(new Color(63, 47, 47));
        g.drawLine(13, 11, 13, 11);
        g.setColor(new Color(255, 252, 253));
        g.drawLine(14, 11, 14, 11);
        g.setColor(new Color(57, 51, 53));
        g.drawLine(15, 11, 15, 11);
        g.setColor(new Color(58, 51, 41));
        g.drawLine(1, 12, 1, 12);
        g.setColor(new Color(66, 48, 34));
        g.drawLine(2, 12, 2, 12);
        g.setColor(new Color(208, 137, 133));
        g.drawLine(3, 12, 3, 12);
        g.setColor(new Color(102, 31, 27));
        g.drawLine(4, 12, 4, 12);
        g.setColor(new Color(255, 255, 232));
        g.drawLine(5, 12, 5, 12);
        g.setColor(new Color(155, 157, 135));
        g.drawLine(6, 12, 6, 12);
        g.setColor(new Color(59, 51, 38));
        g.drawLine(7, 12, 7, 12);
        g.setColor(new Color(255, 254, 241));
        g.drawLine(8, 12, 8, 12);
        g.setColor(new Color(255, 255, 236));
        g.drawLine(9, 12, 9, 12);
        g.setColor(new Color(157, 157, 131));
        g.drawLine(10, 12, 10, 12);
        g.setColor(new Color(125, 21, 28));
        g.drawLine(11, 12, 11, 12);
        g.setColor(new Color(194, 90, 97));
        g.drawLine(12, 12, 12, 12);
        g.setColor(new Color(63, 47, 47));
        g.drawLine(13, 12, 13, 12);
        g.setColor(new Color(58, 49, 50));
        g.drawLine(14, 12, 14, 12);
        g.setColor(new Color(60, 48, 50));
        g.drawLine(3, 13, 3, 13);
        g.drawLine(4, 13, 4, 13);
        g.setColor(new Color(160, 156, 131));
        g.drawLine(5, 13, 5, 13);
        g.setColor(new Color(255, 255, 232));
        g.drawLine(6, 13, 6, 13);
        g.setColor(new Color(59, 52, 36));
        g.drawLine(7, 13, 7, 13);
        g.setColor(new Color(161, 153, 142));
        g.drawLine(8, 13, 8, 13);
        g.setColor(new Color(161, 153, 140));
        g.drawLine(9, 13, 9, 13);
        g.setColor(new Color(255, 253, 244));
        g.drawLine(10, 13, 10, 13);
        g.setColor(new Color(58, 50, 48));
        g.drawLine(11, 13, 11, 13);
        g.setColor(new Color(58, 49, 52));
        g.drawLine(12, 13, 12, 13);
        g.setColor(new Color(60, 48, 50));
        g.drawLine(4, 14, 4, 14);
        g.setColor(new Color(58, 54, 29));
        g.drawLine(5, 14, 5, 14);
        g.setColor(new Color(160, 156, 131));
        g.drawLine(6, 14, 6, 14);
        g.setColor(new Color(59, 52, 36));
        g.drawLine(7, 14, 7, 14);
        g.setColor(new Color(255, 254, 243));
        g.drawLine(8, 14, 8, 14);
        g.setColor(new Color(60, 52, 39));
        g.drawLine(9, 14, 9, 14);
        g.setColor(new Color(59, 50, 41));
        g.drawLine(10, 14, 10, 14);
        g.setColor(new Color(59, 49, 50));
        g.drawLine(6, 15, 6, 15);
        g.setColor(new Color(63, 47, 47));
        g.drawLine(7, 15, 7, 15);
        g.drawLine(8, 15, 8, 15);

        return new ImageIcon(out);
    }

    /**
     * Shows the item editor window and goes to the item indicated by
     * <code>in</code>. If <code>in</code> is not an Integer, String, or
     * Item, <code>in.toString()</code> will be used for the String. For
     * Item's the item number will be used. Other data in the Item object will
     * be ignored.
     * 
     * @see net.starmen.pkhack.HackModule#show(java.lang.Object)
     * @param in An Integer (of item number), String (of partial item name) or
     *            {@link ItemEditor.Item}.
     * @throws IllegalArgumentException If <code>in</code> is not an Integer
     *             or String.
     */
    public void show(Object in) throws IllegalArgumentException
    {
        show();

        if (in instanceof Integer)
        {
            this.itemSelector.setSelectedIndex(((Integer) in).intValue()
                % itemSelector.getItemCount());
        }
        else if (in instanceof String)
        {
            HackModule.search((String) in, itemSelector);
        }
        else if (in instanceof Item)
        {
            this.itemSelector.setSelectedIndex(((Item) in).number);
        }
        else
        {
            HackModule.search(in.toString(), itemSelector);
            throw new IllegalArgumentException(
                "Object not Integer, String, or ItemEditor.Item.");
        }
        itemSelector.repaint();
    }

    /**
     * JPanel containing combobox and hyperlink label that calls the Item Editor
     * and sets the data being edited to the data in the combobox when clicked.
     */
    public static class ItemEntry extends JLinkComboBox
    {
        /**
         * Creates a new <code>ItemEntry</code> component.
         * 
         * @param label words to identify this component with
         * @param hm <code>HackModule</code> to get rom to read info from
         * @param al <code>ActionListener</code> to add to this.
         * @param type if negitive, all items will be shown; otherwise only
         *            items of this type will be shown
         * @param smode Search mode, one of {@link #SEARCH_NONE},
         *            {@link #SEARCH_LEFT},{@link #SEARCH_RIGHT},
         *            {@link #SEARCH_EDIT}.
         */
        public ItemEntry(final String label, HackModule hm, ActionListener al,
            int type, int smode)
        {
            super(ItemEditor.class, type < 0
                ? createItemComboBox(null, hm)
                : createItemSelectorForType(new JComboBox(), type, hm), label,
                smode);

            if (al != null)
                comboBox.addActionListener(al);
        }

        /**
         * Creates a new <code>ItemEntry</code> component. It will have the
         * default search mode of {@link JSearchableComboBox#SEARCH_EDIT}.
         * 
         * @param label words to identify this component with
         * @param hm <code>HackModule</code> to get rom to read info from
         * @param al <code>ActionListener</code> to add to this.
         * @param type if negitive, all items will be shown; otherwise only
         *            items of this type will be shown
         */
        public ItemEntry(final String label, HackModule hm, ActionListener al,
            int type)
        {
            this(label, hm, al, type, SEARCH_EDIT);
        }

        /**
         * Creates a new <code>ItemEntry</code> component. It will have the
         * default search mode of {@link JSearchableComboBox#SEARCH_EDIT}.
         * 
         * @param label words to identify this component with
         * @param hm <code>HackModule</code> to get rom to read info from
         * @param type if negitive, all items will be shown; otherwise only
         *            items of this type will be shown
         */
        public ItemEntry(final String label, HackModule hm, int type)
        {
            this(label, hm, null, type);
        }

        /**
         * Creates a new <code>ItemEntry</code> component. It will have the
         * default search mode of {@link JSearchableComboBox#SEARCH_EDIT}.
         * 
         * @param label words to identify this component with
         * @param hm <code>HackModule</code> to get rom to read info from
         * @param al <code>ActionListener</code> to add to this. If non-null,
         *            the action command will be set to <code>label</code>.
         */
        public ItemEntry(final String label, HackModule hm, ActionListener al)
        {
            this(label, hm, al, -1);
        }

        /**
         * Creates a new <code>ItemEntry</code> component. It will have the
         * default search mode of {@link JSearchableComboBox#SEARCH_EDIT}.
         * 
         * @param label words to identify this component with
         * @param hm <code>HackModule</code> to get rom to read info from
         */
        public ItemEntry(final String label, HackModule hm)
        {
            this(label, hm, null);
        }
    }

    public void setRadioButtons(int num)
    {
        for (int i = 0; i < 4; i++)
        {
            protect[(3 - i) * 4 + num % 4].setSelected(true);
            num /= 4;
        }
    }

    public void setSpecial()
    {
        int[] arg = new int[4];
        int i = 0, j = 0;
        for (i = 0; i < 4; i++)
        {
            for (j = 0; j < 4; j++)
            {
                if (protect[(3 - i) * 4 + j].isSelected())
                {
                    arg[i] = j;
                }
            }
        }
        j = 0;
        for (i = 0; i < 4; i++)
            j += Math.pow(4, i) * arg[i];
        special.getDocument().removeDocumentListener(protectSetter);
        special.setText("" + j);
        special.getDocument().addDocumentListener(protectSetter);
    }
}