package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.MaxLengthDocument;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits misc text in the Earthbound ROM. Locations of the misc text are gotten
 * from miscTextLocations.txt. Look at that file for its format.
 * 
 * @author AnyoneEB
 */
public class MiscTextEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public MiscTextEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JComboBox selector;
    private JTextField tf;
    private boolean initing = true;
    /**
     * Array of all the misc text loaded. Since it could be holding anything,
     * it's probably pretty useless, but you could search for a title that you
     * know is there and use the information up to the next title.
     * 
     * @see MiscTextEditor.MiscText
     */
    public static MiscText[] miscText;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(300, 120);

        //make a JComboBox to select entry, and a JTextField to edit it
        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        selector = new JComboBox();
        selector.setActionCommand("miscTextSelector");
        selector.addActionListener(this);
        entry.add(selector);
        tf = new JTextField();
        entry.add(tf);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.4";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Misc. Text Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Text offsets of start text list made by JeffMan\n"
            + "Battle commands and ailment text from PK Hack source";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        readFromRom();
        initSelector();
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    /**
     * Reads information from miscTextLocations.txt and the ROM.
     */
    public static void readFromRom(HackModule hm)
    {
        String[] miscTextList = new String[0]; //list of misc. text entries

        try
        {
            miscTextList = new CommentedLineNumberReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream(DEFAULT_BASE_DIR
                    + "miscTextLocations.txt"))).readUsedLines();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            miscTextList = new String[0];
        }

        miscText = new MiscText[miscTextList.length];
        for (int i = 0; i < miscTextList.length; i++)
        {
            miscText[i] = (Integer
                .parseInt(miscTextList[i].substring(0, 6), 16) != 0
                ? new MiscText(Integer.parseInt(
                    miscTextList[i].substring(0, 6), 16), Integer
                    .parseInt(miscTextList[i].substring(7)), hm)
                : new MiscText(miscTextList[i].substring(7)));
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    private void initSelector()
    {
        this.initing = true;
        selector.removeAllItems();
        for (int i = 0; i < miscText.length; i++)
        {
            selector.addItem(miscText[i]);
        }
        this.initing = false;
        selector.setSelectedIndex(0);
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void saveInfo(int i)
    {
        if (i < 0) return;
        miscText[i].setInfo(tf.getText());
        miscText[i].writeInfo();
        selector.repaint();
    }

    private void showInfo(int i)
    {
        if (i < 0) return;
        if (miscText[i].isRealEntry())
        {
            tf.setDocument(new MaxLengthDocument(miscText[i].getLen()));
            tf.setColumns(miscText[i].getLen());
            tf.setText(miscText[i].toString());
        }
        else
        //note: two titles next to each other _WILL_ cause a crash
        {
            if (this.initing) { return; }
            int current = selector.getSelectedIndex();
            try
            {
                selector.setSelectedIndex(current + 1);
            }
            catch (IllegalArgumentException e) //if it's the last then try one
            // before
            {
                try
                {
                    selector.setSelectedIndex(current - 1);
                }
                catch (IllegalArgumentException iae) //if it's the only then
                // leave it
                {
                    selector.setSelectedIndex(current);
                    return;
                }
            }
            showInfo(selector.getSelectedIndex());
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(selector.getActionCommand()))
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
    
    public static String getMiscText(int index)
	{
    	return miscText[index].getInfo();
    }

/*    public static void setMiscText(int index, String text)
	{
    	miscText[index].setInfo(text);
    	saveInfo(index);
    }*/
    /**
     * This class represents any misc text in EarthBound. The text must not
     * have any control codes or compression.
     */
    public static class MiscText
    {
        private HackModule hm;
        private char[] info;
        private int address;
        private int len;
        private boolean isRealEntry = true;

        /**
         * Creates a new MiscText by reading <code>len</code> chars from
         * <code>address</code>
         * 
         * @param address Where to read from.
         * @param len How many characters to read.
         */
        public MiscText(int address, int len, HackModule hm) //desc could be a
        // description or the
        // default text
        {
            this.hm = hm;
            this.address = address;
            this.len = len;
            this.info = new char[len];
            
            Rom rom = hm.rom;
            
            rom.seek(this.address);

            for (int j = 0; j < info.length; j++)
            {
                this.info[j] = hm.simpToRegChar(rom.readCharSeek());
            }
        }

        /**
         * Creates a MiscText to be used as a title in the <code>JComboBox</code>.
         * The title is displayed in all caps with an underscore added on both
         * sides.
         * 
         * @param title Title to use
         */
        public MiscText(String title) //make a fake entry for a separator
        {
            title = "_" + title.toUpperCase() + "_";
            this.info = new char[title.length()];
            this.setInfo(title);
            this.isRealEntry = false;
        }

        /**
         * Returns true if this is a real entry, false if it's a title.
         * 
         * @return boolean
         */
        public boolean isRealEntry()
        {
            return this.isRealEntry;
        }

        /**
         * Returns a <code>String</code> of the text this represents.
         * Identical to <code>getInfo()</code>.
         * 
         * @return String
         * @see #getInfo()
         */
        public String toString()
        {
            return this.getInfo();
        }

        /**
         * Returns a <code>String</code> of the text this represents.
         * 
         * @return String
         */
        public String getInfo()
        {
            return stripNull(new String(this.info));
        }

        /**
         * Changes the text this represents. Doesn't work on titles.
         * 
         * @param newInfo New text.
         */
        public void setInfo(String newInfo)
        {
            if (!isRealEntry) return;
            char[] temp = newInfo.toCharArray();
            for (int j = 0; j < info.length; j++)
            {
                info[j] = (j < temp.length ? temp[j] : (char) 0);
            }
        }

        /**
         * Returns the maxium number of characters this can hold.
         * 
         * @return int
         */
        public int getLen()
        {
            return this.len;
        }

        /**
         * Writes the information stored in this into the ROM. Does nothing if
         * this is a title.
         */
        public void writeInfo()
        {
            if (!isRealEntry) return;
            
            Rom rom = hm.rom;
            
            rom.seek(this.address);
            for (int j = 0; j < this.info.length; j++)
            {
                rom.writeSeek(hm.simpToGameChar(this.info[j]));
            }
        }

        private String stripNull(String in)
        {
            return new StringTokenizer(in, "\0").nextToken();
        }
    }
}
