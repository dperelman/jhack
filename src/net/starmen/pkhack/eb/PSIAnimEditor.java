/*
 * Created on Apr 28, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

public class PSIAnimEditor extends EbHackModule implements ActionListener
{
    public PSIAnimEditor(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    public String getVersion()
    {
        return "0.1a";
    }

    public String getDescription()
    {
        return "PSI Animation Editor";
    }

    public String getCredits()
    {
        return "Written by n42";
    }

    public static class PSIAnim
    {
        private EbHackModule hm;
        private boolean isInited = false;
        private int num, arngPointer, dataPointer;
        
        /** Base address for the tiles */
        public static final int baseAddr = 0xC0200;
        /** Base address for the arrangement */
        public static final int arngPointers = 0xCF78F;
        /** Base address for the PSI data */
        public static final int dataPointers = 0xCC2E19;
        
        /** Two-dimentional array of arrangements used. */
        private int[][] arrangement = new int[32][28];
        
        
        public PSIAnim(int i, EbHackModule hm)
        {
            this.hm = hm;
            this.num = i;

            arngPointer = toRegPointer(hm.rom.readMulti(arngPointer + (i * 4), 4));
            dataPointer = toRegPointer(hm.rom.readMulti(arngPointer + (i * 12), 12));
        }
        public boolean readInfo()
        {
            if (isInited) return true;
            
            isInited = true;
			return true;
        }
    }
    
    protected void init()
    {
    }
    public void hide()
    {
        mainWindow.setVisible(false);
    }
    public void actionPerformed(ActionEvent ae)
    {
    }
}