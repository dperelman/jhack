/*
 * Created on Aug 24, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JLinkComboBox;
import net.starmen.pkhack.XMLPreferences;

/**
 * Provides GUI and API for editing the sprite character table. This table
 * controls which sprite is used for which playable character. For more
 * information look at {@link SpriteCharacterTableEntry}.
 * 
 * @see SpriteCharacterTableEntry
 * @see #sctEntries
 * @author AnyoneEB
 */
public class SpriteCharacterTableEditor extends EbHackModule implements
    ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public SpriteCharacterTableEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * Represents an entry in the sprite character table. This table controls
     * which sprite is used for which playable character. Each entry controls
     * the sprites for a single character in different situations. These 7
     * situations are listed in {@link SpriteCharacterTableEditor#spriteLabels}.
     * It is interesting to note that there is space for an eighth, but it is
     * always 0xFF. This does not edit that value. If you want to know which
     * character you are editing the sprite for, look at
     * {@link SpriteCharacterTableEditor#sctEntries}.
     * </p>
     * <p>
     * This editor is based off information from <a href =
     * "http://forum.starmen.net/ultimatebb.php?ubb=get_topic;f=8;t=000397#000000">
     * this forum topic </a>.
     * </p>
     * 
     * @see SpriteCharacterTableEditor#spriteLabels
     * @see SpriteCharacterTableEditor#sctEntries
     * @author AnyoneEB
     */
    public static class SpriteCharacterTableEntry
    {
        private AbstractRom rom;
        private int address, num, sprite[] = new int[7];

        /**
         * Creates a new SpriteCharacterTableEntry and loads entry
         * <code>num</code>. If you want to know which character you are
         * changing the sprites for, look at
         * {@link SpriteCharacterTableEditor#sctEntries}.
         * 
         * @see SpriteCharacterTableEditor#sctEntries
         * @param num Which entry to edit.
         */
        public SpriteCharacterTableEntry(int num, AbstractRom rom)
        {
            this.rom = rom;
            address = 0x3F4B5 + (num * 16);

            rom.seek(address);
            for (int i = 0; i < 7; i++)
            {
                sprite[i] = rom.readMultiSeek(2);
            }
        }

        /**
         * Returns the specified sprite SPT value.
         * 
         * @see SpriteCharacterTableEditor#spriteLabels
         * @param n Which sprite to get (see: <code>spriteLabels</code>).
         * @return 1-based SPT entry number of the specified sprite (0=nothing)
         */
        public int getSprite(int n)
        {
            return sprite[n];
        }

        /**
         * Sets the specified sprite SPT value.
         * 
         * @see SpriteCharacterTableEditor#spriteLabels
         * @param n Which sprite to set (see: <code>spriteLabels</code>).
         * @param spr 1-based SPT entry number of the sprite to set (0=nothing)
         */
        public void setSprite(int n, int spr)
        {
            sprite[n] = spr;
        }

        /**
         * Writes this to the ROM.
         */
        public void writeInfo()
        {
            rom.seek(address);
            for (int i = 0; i < 7; i++)
            {
                rom.writeSeek(sprite[i], 2);
            }
        }
    }
    /**
     * Array containing all 17 SpriteCharacterTableEntry's. Call
     * {@link SpriteCharacterTableEditor#readFromRom(AbstractRom)}to be sure
     * the entries are inited.
     * 
     * @see #readFromRom(AbstractRom)
     */
    public static SpriteCharacterTableEntry[] sctEntries = new SpriteCharacterTableEntry[17];
    /**
     * Array of <code>String</code>'s labeling the sprites in a
     * SpriteCharacterTableEntry. The list is: <br>
     * [0] = "Normal" <br>
     * [1] = "Dead" <br>
     * [2] = "Ladder" <br>
     * [3] = "Rope" <br>
     * [4] = "Tiny" (Lost Underworld) <br>
     * [5] = "Tiny & Dead" (Lost Underworld) <br>
     * [6] = "Robot"
     * 
     * @see SpriteCharacterTableEntry#getSprite(int)
     * @see SpriteCharacterTableEntry#setSprite(int, int)
     */
    public static String[] spriteLabels = new String[]{"Normal", "Dead",
        "Ladder", "Rope", "Tiny", "Tiny & Dead", "Robot"};
    /**
     * Array of <code>String</code>'s labeling the sprite character table
     * entries. The list is: <br>
     * [0] = "Ness" <br>
     * [1] = "Paula" <br>
     * [2] = "Jeff" <br>
     * [3] = "Poo" <br>
     * [4] = "Pokey" <br>
     * [5] = "Picky" <br>
     * [6] = "King" <br>
     * [7] = "Tony" <br>
     * [8] = "Bubble Monkey" <br>
     * [9] = "Brick Road" <br>
     * [10 - 14] = "Flying Man 1-5" <br>
     * [15 - 16] = "Teddy Bear 1-2"
     * 
     * @see SpriteCharacterTableEntry#SpriteCharacterTableEntry(int,
     *      AbstractRom)
     */
    public static String[] entryNames = new String[]{"Ness", "Paula", "Jeff",
        "Poo", "Pokey", "Picky", "King", "Tony", "Bubble Monkey", "Brick Road",
        "Flying Man 1", "Flying Man 2", "Flying Man 3", "Flying Man 4",
        "Flying Man 5", "Teddy Bear 1", "Teddy Bear 2"};

    /**
     * Reads the sprite character table into <code>sctEntries</code>.
     * 
     * @see SpriteCharacterTableEditor#sctEntries
     * @see SpriteCharacterTableEntry
     */
    public static void readFromRom(AbstractRom rom)
    {
        for (int i = 0; i < sctEntries.length; i++)
        {
            sctEntries[i] = new SpriteCharacterTableEntry(i, rom);
        }
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }
    private JComboBox selector;
    private JLinkComboBox[] sprite = new JLinkComboBox[7];

    //NPC only stuff
    private Box npcEntry;
    private JLabel targetLabel;
    private JComboBox target;
    private JComboBox enemy;
    private JLinkComboBox enemyWrapper;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(500, 300);
        mainWindow.setResizable(false);

        Box entry = new Box(BoxLayout.Y_AXIS);

        entry.add(HackModule.getLabeledComponent("Entry: ",
            selector = HackModule.createJComboBoxFromArray(entryNames)));
        selector.setActionCommand("SpriteCharacterTableSelector");
        selector.addActionListener(this);

        for (int i = 0; i < 7; i++)
        {
            entry.add(sprite[i] = new JLinkComboBox(SPTEditor.class, sptNames,
                spriteLabels[i]));
            sprite[i].addActionListener(this);
        }

        npcEntry = new Box(BoxLayout.Y_AXIS);

        npcEntry.add(enemyWrapper = new JLinkComboBox(EnemyEditor.class,
            enemy = EnemyEditor.createEnemyComboBox(this), "Enemy Stats"));
        enemy.setActionCommand("spctEnemySelector");
        enemy.setSelectedIndex(0);
        npcEntry.add(pairComponents(targetLabel = new JLabel("Targeting: "),
            target = new JComboBox(new String[]{"[1] Not targetable",
                "[6] Always targeted", "[7] Normal targeting"}), true));

        entry.add(npcEntry);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.2";
    }

    public String getDescription()
    {
        return "Playable Sprite Table Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Table discovered by BlueAntoid";
    }

    public void show()
    {
        super.show();

        NPCBattleEditor.readFromRom(rom);
        EnemyEditor.readFromRom(this);
        readFromRom();
        selector.setSelectedIndex(0);
        mainWindow.setVisible(true);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void showInfo()
    {
        for (int i = 0; i < 7; i++)
        {
            sprite[i].setSelectedIndex(sctEntries[selector.getSelectedIndex()]
                .getSprite(i));
            sprite[i].repaint();
        }
        int j = selector.getSelectedIndex() - 4;
        if (j < 0)
        {
            target.setEnabled(false);
            targetLabel.setEnabled(false);
            enemyWrapper.setEnabled(false);
        }
        else
        {
            target.setEnabled(true);
            targetLabel.setEnabled(true);
            enemyWrapper.setEnabled(true);

            int tar = NPCBattleEditor.npcbEntries[j].getTarget();
            int t = 0;
            if (tar == 6)
                t = 1;
            else if (tar == 7)
                t = 2;
            target.setSelectedIndex(t);

            enemy.setSelectedIndex(NPCBattleEditor.npcbEntries[j].getEnemy());
            enemy.updateUI();
        }
        npcEntry.repaint();
    }

    private void saveInfo()
    {
        for (int i = 0; i < 7; i++)
            sctEntries[selector.getSelectedIndex()].setSprite(i, sprite[i]
                .getSelectedIndex());
        sctEntries[selector.getSelectedIndex()].writeInfo();
        int j = selector.getSelectedIndex() - 4;
        if (j >= 0)
        {
            NPCBattleEditor.npcbEntries[j].setTarget(getNumberOfString(
                (String) target.getSelectedItem(), false));
            NPCBattleEditor.npcbEntries[j].setEnemy(enemy.getSelectedIndex());
            NPCBattleEditor.npcbEntries[j].writeInfo();
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            showInfo();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }
}