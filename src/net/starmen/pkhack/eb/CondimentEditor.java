/*
 * Created on Jul 20, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits the condiments table. Affects what food items do with condiments.
 * 
 * @author AnyoneEB
 */
public class CondimentEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public CondimentEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static int NUM_ENTRIES = 45;
    public static CondimentEntry[] entries = new CondimentEntry[NUM_ENTRIES];
    public static String[] condimentEffects = {"Restore HP", "Restore PP",
        "Restore HP/PP", "Like rock candy (random stat + 1)", "Increase IQ",
        "Increase Guts", "Increase Speed", "Increase Vitality",
        "Increase Luck", "Cold Cure", "Poison Cure"};

    /**
     * This provides a API for reading/writing condiment table entries. Byte
     * info is in the "get" methods, not the "set" methods.
     * 
     * @author AnyoneEB
     */
    public static class CondimentEntry
    {
        private AbstractRom rom;
        private int num, address;
        private int item, condiment[] = new int[2], effect, goodRev, badRev,
                runTime;

        public CondimentEntry(int num, AbstractRom rom)
        {
            this.rom = rom;
            this.num = num;
            this.address = 0x15EC5B + (num * 7) + 28;
            rom.seek(address);

            this.item = rom.readSeek();
            this.condiment = rom.readSeek(2);
            this.effect = rom.readSeek();
            this.goodRev = rom.readSeek();
            this.badRev = rom.readSeek();
            this.runTime = rom.readSeek();
        }

        public void writeInfo()
        {
            rom.seek(address);

            rom.writeSeek(this.item);
            rom.writeSeek(this.condiment);
            rom.writeSeek(this.effect);
            rom.writeSeek(this.goodRev);
            rom.writeSeek(this.badRev);
            rom.writeSeek(this.runTime);
        }

        /**
         * Returns the amount of the increase/recovery with a bad condiment.
         * Note that HP recovers that value * 6. Also note that a random factor
         * is involved, you will not get the same amount (in game) every time.
         * 
         * @see #setBadAmount(int)
         * @return Amount of the increase/recovery with a bad condiment.
         */
        public int getBadAmount()
        {
            return badRev;
        }

        /**
         * Returns the item number of the specified condiment which tastes good
         * with the food. Condiments are of item type 40. There are two in each
         * table entry. The first one is always set to Jar of Delisauce by
         * default.
         * 
         * @see #setCondiment(int, int)
         * @param i Which condiment to get (0-1).
         * @return Item number of the condiment.
         */
        public int getCondiment(int i)
        {
            return condiment[i];
        }

        /**
         * Returns the effect byte.
         * <ul>
         * Here are the possible values:
         * <li>00 = Restore HP</li>
         * <li>01 = Restore PP</li>
         * <li>02 = Restore HP/PP</li>
         * <li>03 = Like rock candy (random stat + 1)</li>
         * <li>04 = Increase IQ</li>
         * <li>05 = Increase Guts</li>
         * <li>06 = Increase Speed</li>
         * <li>07 = Increase Vitality</li>
         * <li>08 = Increase Luck</li>
         * <li>09 = No visible effect (that's what it says if you use it)</li>
         * </ul>
         * 
         * @see #setEffect(int)
         * @return Effect byte.
         */
        public int getEffect()
        {
            return effect;
        }

        /**
         * Returns the amount of the increase/recovery with a good condiment.
         * Note that HP recovers that value * 6. Also note that a random factor
         * is involved, you will not get the same amount (in game) every time.
         * 
         * @see #setGoodAmount(int)
         * @return Amount of the increase/recovery with a good condiment.
         */
        public int getGoodAmount()
        {
            return goodRev;
        }

        /**
         * Returns the food item this affects. Food items are type 32.
         * 
         * @see #setItem(int)
         * @return Which item this affects.
         */
        public int getItem()
        {
            return item;
        }

        /**
         * Returns how long this item causes you to run in tenths of a second.
         * Run = skip sandwhich effect.
         * 
         * @see #setRunTime(int)
         * @return How long this item causes you to run in 1/10's of a second.
         */
        public int getRunTime()
        {
            return runTime;
        }

        /**
         * Sets the amount of the increase/recovery with a bad condiment.
         * 
         * @see #getBadAmount()
         * @param i Value to set.
         */
        public void setBadAmount(int i)
        {
            badRev = i;
        }

        /**
         * Sets the item number of the specified condiment which tastes good
         * with the food.
         * 
         * @see #getCondiment(int)
         * @param i Which condiment to get (0-1).
         * @param newCondiment Value to set.
         */
        public void setCondiment(int i, int newCondiment)
        {
            condiment[i] = newCondiment;
        }

        /**
         * Sets the effect byte.
         * 
         * @see #getEffect()
         * @param i Value to set.
         */
        public void setEffect(int i)
        {
            effect = i;
        }

        /**
         * Sets the amount of the increase/recovery with a good condiment.
         * 
         * @see #getGoodAmount()
         * @param i Value to set.
         */
        public void setGoodAmount(int i)
        {
            goodRev = i;
        }

        /**
         * Sets the food item this affects. Food items are type 32.
         * 
         * @see #getItem()
         * @param i Value to set.
         */
        public void setItem(int i)
        {
            item = i;
        }

        /**
         * Sets how long this item causes you to run in tenths of a second.
         * 
         * @see #getRunTime()
         * @param i Value to set.
         */
        public void setRunTime(int i)
        {
            runTime = i;
        }

        /**
         * Returns the name of the food item this affects.
         * 
         * @return Name of the food item.
         */
        public String toString()
        {
            return new String(ItemEditor.items[this.getItem()].name);
        }

    }

    private JComboBox selector, effectSelector;
    private ItemEditor.ItemEntry foodSelector,
            condimentSelector[] = new ItemEditor.ItemEntry[2];

    private JTextField goodAmount, badAmount, runTime;

    protected void init()
    {
        //Init item editor
        ItemEditor.readFromRom(this);

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(400, 270);
        mainWindow.setResizable(false);

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        selector = new JComboBox();
        selector.setActionCommand("condimentEditorSelector");
        selector.addActionListener(this);
        //entry.add(getLabeledComponent("Entry: ", selector));
        entry.add(new JSearchableComboBox(selector, "Entry: "));

        foodSelector = new ItemEditor.ItemEntry("Food (type-32)", this, 32);
        entry.add(foodSelector);

        for (int i = 0; i < 2; i++)
        {
            condimentSelector[i] = new ItemEditor.ItemEntry("Condiment "
                + (i + 1) + " (type-40)", this, 32);
            entry.add(condimentSelector[i]);
        }

        effectSelector = HackModule
            .createJComboBoxFromArray(CondimentEditor.condimentEffects);
        entry.add(getLabeledComponent("Effect: ", effectSelector));

        goodAmount = HackModule.createSizedJTextField(4, true);
        entry.add(getLabeledComponent("Good Amount (x6 for HP): ", goodAmount));

        badAmount = HackModule.createSizedJTextField(4, true);
        entry.add(getLabeledComponent("Bad Amount (x6 for HP): ", badAmount));

        runTime = HackModule.createSizedJTextField(4, true);
        entry.add(getLabeledComponent("Run Time (1/10 sec's): ", runTime));

        mainWindow.getContentPane().add(entry);
    }

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Condiment Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Table discovered by michael_cayer and BlueAntoid";
    }

    public static void readFromRom(AbstractRom rom)
    {
        for (int i = 0; i < entries.length; i++)
        {
            entries[i] = new CondimentEntry(i, rom);
        }
    }

    private void initSelector()
    {
        selector.removeAllItems();
        for (int i = 0; i < entries.length; i++)
        {
            selector.addItem(HackModule.getNumberedString(new String(
                ItemEditor.items[entries[i].getItem()].name), i));
        }
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public void show()
    {
        super.show();

        readFromRom(rom);
        ItemEditor.createItemSelectorForType(foodSelector.getJComboBox(), 32,
            this);
        for (int i = 0; i < 2; i++)
            ItemEditor.createItemSelectorForType(condimentSelector[i]
                .getJComboBox(), 40, this);
        initSelector();
        selector.setSelectedIndex(0);
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    private void setSelectorToNum(JComboBox sel, int n)
    {
        for (int i = 0; i < sel.getItemCount(); i++)
            if (HackModule.getNumberOfString(sel.getItemAt(i).toString()) == n)
                sel.setSelectedIndex(i);
    }

    private CondimentEntry getCurrentEntry()
    {
        return entries[selector.getSelectedIndex()];
    }

    private void showInfo()
    {
        if (selector.getSelectedIndex() < 0)
            return;
        setSelectorToNum(foodSelector.getJComboBox(), getCurrentEntry()
            .getItem());
        for (int i = 0; i < 2; i++)
            setSelectorToNum(condimentSelector[i].getJComboBox(),
                getCurrentEntry().getCondiment(i));
        effectSelector.setSelectedIndex(getCurrentEntry().getEffect());
        goodAmount.setText(Integer.toString(getCurrentEntry().getGoodAmount()));
        badAmount.setText(Integer.toString(getCurrentEntry().getBadAmount()));
        runTime.setText(Integer.toString(getCurrentEntry().getRunTime()));

        mainWindow.repaint();
    }

    private void saveInfo()
    {
        if (selector.getSelectedIndex() < 0)
            return;

        if (foodSelector.getSelectedIndex() == -1)
        {
            JOptionPane.showMessageDialog(mainWindow,
                "Please select a food item and try again.", "Save Failed",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (int j = 0; j < condimentSelector.length; j++)
            if (condimentSelector[j].getSelectedIndex() == -1)
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Please select an item for condiment " + (j + 1)
                        + " and try again.", "Save Failed",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

        //Reset selector if item changed
        boolean resetSel = getCurrentEntry().getItem() != HackModule
            .getNumberOfString(foodSelector.getSelectedItem().toString());

        getCurrentEntry().setItem(
            HackModule.getNumberOfString(foodSelector.getSelectedItem()
                .toString()));
        for (int i = 0; i < 2; i++)
            getCurrentEntry().setCondiment(
                i,
                HackModule.getNumberOfString(condimentSelector[i]
                    .getSelectedItem().toString()));
        getCurrentEntry().setEffect(effectSelector.getSelectedIndex());
        getCurrentEntry().setGoodAmount(Integer.parseInt(goodAmount.getText()));
        getCurrentEntry().setBadAmount(Integer.parseInt(badAmount.getText()));
        getCurrentEntry().setRunTime(Integer.parseInt(runTime.getText()));

        CondimentEditor.entries[selector.getSelectedIndex()].writeInfo();

        if (resetSel)
            initSelector();
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equalsIgnoreCase("apply"))
        {
            saveInfo();
        }
    }
}