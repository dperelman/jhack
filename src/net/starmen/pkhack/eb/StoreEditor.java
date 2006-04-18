package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Editor for what items are sold in what stores. Requires ItemEditor because it
 * shows the item names and allows editing of their costs.
 * 
 * @author AnyoneEB
 * @see ItemEditor
 */
public class StoreEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public StoreEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    private JComboBox selector;
    private JComboBox[] item = new JComboBox[7];
    private JTextField[] money = new JTextField[7];
    /**
     * Number of store entries.
     * 
     * @see #stores
     * @see #storeNames
     */
    public static final int NUM_STORES = 66;
    /**
     * Array of all the {@link Store}'s in the game.
     * 
     * @see StoreEditor.Store
     */
    public static Store[] stores = new Store[NUM_STORES];
    /**
     * Array of the names of all the stores in the game. This list is
     * incomplete, if you know what any of the unknowns are please tell me
     * (AnyoneEB). This list is stored in the file storeNames.txt.
     * 
     * @see #initStoreNames()
     * @see HackModule#readArray(String, boolean, String[])
     */
    public static String[] storeNames = new String[NUM_STORES];
    private static boolean storeNamesInited = false;

    /**
     * Inits {@link #storeNames}if it has not already been inited.
     */
    public static void initStoreNames()
    {
        if (!storeNamesInited)
        {
            storeNamesInited = true;
            readArray(DEFAULT_BASE_DIR, "storeNames.txt", false, storeNames);
        }
    }

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(350, 300);
        mainWindow.setResizable(true);

        initStoreNames();
        mainWindow.getContentPane().add(
            HackModule.getLabeledComponent("Store: ",
                selector = createJComboBoxFromArray(storeNames, false)),
            BorderLayout.NORTH);
        selector.setActionCommand("storeSelector");
        selector.addActionListener(this);

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
        entry.add(pairComponents(new JLabel("Item"), new JLabel("Cost"), true));
        for (int i = 0; i < item.length; i++)
        {
            entry.add(pairComponents(item[i] = ItemEditor.createItemComboBox(
                this, this), money[i] = createSizedJTextField(5, true), true));
            item[i].setActionCommand("storeItemList" + i);
            item[i].addActionListener(this);
        }

        JPanel entryWrapper = new JPanel(new FlowLayout());
        entryWrapper.add(entry);

        mainWindow.getContentPane().add(entryWrapper, BorderLayout.CENTER);
        mainWindow.pack();
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.1";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Store Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Store names list from `Frieza's store editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        ItemEditor.readFromRom(this);
        readFromRom();
        selector.setSelectedIndex(0);
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    /**
     * Reads all of the stores from to ROM.
     * 
     * @see StoreEditor.Store
     * @see #stores
     */
    public static void readFromRom(AbstractRom rom)
    {
        for (int i = 0; i < stores.length; i++)
        {
            stores[i] = new Store(i, rom);
        }
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }

    private void showInfo(int i)
    {
        if (i < 0)
            return;
        for (int j = 0; j < item.length; j++)
        {
            item[j].setSelectedIndex(stores[i].getItem(j));
            item[j].repaint();
            money[j].setText(Integer.toString(ItemEditor.items[item[j]
                .getSelectedIndex()].cost));
        }
    }

    private void saveInfo(int i)
    {
        if (i < 0)
            return;
        else if (i == 0)
        {
            JOptionPane.showMessageDialog(mainWindow,
                "Writing to store #0 is not allowed.", "Write Failed",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (int j = 0; j < item.length; j++)
        {
            stores[i].setItem(j, item[j].getSelectedIndex());
            ItemEditor.items[item[j].getSelectedIndex()].cost = Integer
                .parseInt(money[j].getText());
            ItemEditor.items[item[j].getSelectedIndex()].writeInfo();
        }
        stores[i].writeInfo();
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().startsWith("storeItemList"))
        {
            int j = Integer.parseInt(ae.getActionCommand().substring(13, 14));
            if (item[j].getSelectedIndex() < 0)
                return;
            //last char
            money[j].setText(Integer.toString(ItemEditor.items[item[j]
                .getSelectedIndex()].cost));
        }
        else if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * Represents a store in Earthbound. Holds what item that store sells.
     */
    public static class Store
    {
        private AbstractRom rom;
        private int address, num;
        private int[] items = new int[7];

        /**
         * Reads the information from the ROM on the given store number.
         * 
         * @param num Store number to read information on.
         */
        public Store(int num, AbstractRom rom)
        {
            this.rom = rom;
            this.num = num;
            this.address = 0x1578b2 + (num * 7);

            rom.seek(this.address);
            for (int i = 0; i < items.length; i++)
            {
                items[i] = rom.readSeek();
            }
        }
        
        /**
         * Returns which store number this is.
         * 
         * @return the store number of this
         */
        public int getNum()
        {
            return num;
        }

        /**
         * Writes the information stored in this to the ROM.
         */
        public void writeInfo()
        {
            rom.seek(this.address);
            for (int i = 0; i < items.length; i++)
            {
                rom.writeSeek(items[i]);
            }
        }

        /**
         * Gets the item number of the <code>i</code> 'th item in this store's
         * list.
         * 
         * @param i Place in list to get item of.
         * @return Item number of item at <code>i</code>.
         */
        public int getItem(int i)
        {
            return items[i];
        }

        /**
         * Sets the item number of the <code>i</code> 'th item in this store's
         * list.
         * 
         * @param i Place in list to set item of.
         * @param in Item number of item to set at <code>i</code>.
         */
        public void setItem(int i, int in)
        {
            items[i] = in;
        }
    }
}