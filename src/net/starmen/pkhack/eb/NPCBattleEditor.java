/*
 * Created on Nov 26, 2003
 */
package net.starmen.pkhack.eb;

import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class NPCBattleEditor extends EbHackModule
{

    /**
     * @param rom
     * @param prefs
     */
    public NPCBattleEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }

    /**
     * TODO Write javadoc for this class
     * 
     * @author AnyoneEB
     */
    public static class NPCBattleEntry
    {
        private Rom rom;
        private int address;
        private int num;
        private int target;
        /** Enemy number plus one. */
        private int enemy;

        /**
         *  
         */
        public NPCBattleEntry(int n, Rom rom) {
            this.rom = rom;
            num = n;
            address = 0x15912D + (n * 2); //TODO length?

            rom.seek(address);
            target = rom.readSeek();
            enemy = rom.readSeek();
        }

        public void writeInfo()
        {
            rom.seek(address);
            rom.writeSeek(target);
            rom.writeSeek(enemy);
        }

        /**
         * @return Returns the enemy.
         */
        public int getEnemy()
        {
            return enemy;
        }

        /**
         * @param enemy The enemy to set.
         */
        public void setEnemy(int enemy)
        {
            this.enemy = enemy;
        }

        /**
         * @return Returns the target.
         */
        public int getTarget()
        {
            return target;
        }

        /**
         * @param target The target to set.
         */
        public void setTarget(int target)
        {
            this.target = target;
        }

        /**
         * @return Returns the address.
         */
        public int getAddress()
        {
            return address;
        }

    }

    public static final int NUM_ENTRIES = 13;
    public static NPCBattleEntry[] npcbEntries = new NPCBattleEntry[NUM_ENTRIES];

    public static void readFromRom(Rom rom)
    {
        for (int i = 0; i < NUM_ENTRIES; i++)
            npcbEntries[i] = new NPCBattleEntry(i, rom);
    }

    private void readFromRom()
    {
        readFromRom(rom);
    }

    public void reset()
    {
        readFromRom();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.1";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "NPC Battle Table Editor";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Information discovered by Blue Antoid";
    }

    //NO graphical interface
    protected void init()
    {}

    public void hide()
    {}
}
