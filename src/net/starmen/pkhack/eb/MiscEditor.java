package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BoxLayout;

import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits misc text in the Earthbound ROM. Locations of the misc text are gotten
 * from miscTextLocations.txt. Look at that file for its format.
 * 
 * @author EBisumaru
 */
public class MiscEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public MiscEditor(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JComboBox selector;
    private JTextField tf;
    private JComboBox listBox;
    private JColorChooser colorBox;
    private ActionEditor.ActionEntry actionBox;
    private TextEditor.TextOffsetEntry textBox;
    
    private int cType; //current type
    
    private boolean initing = true;
    /**
     * Array of all the misc text loaded. Since it could be holding anything,
     * it's probably pretty useless, but you could search for a title that you
     * know is there and use the information up to the next title.
     * 
     * @see MiscEditor.Crap
     */
    public static Crap[] entries;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());

        //make a JComboBox to select entry, and a JTextField to edit it
        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        selector = new JComboBox();
        selector.setActionCommand("miscTextSelector");
        selector.addActionListener(this);
        entry.add(selector);
        
        tf = HackModule.createSizedJTextField(5, true);
        entry.add(tf);
        
        listBox = new JComboBox();
        entry.add(listBox);
        
        colorBox = new JColorChooser();
        entry.add(colorBox);
        
        actionBox = new ActionEditor.ActionEntry("Action");
        entry.add(actionBox);
        
        textBox = new TextEditor.TextOffsetEntry("Text", true);
        entry.add(textBox);
        
        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.setResizable(false);
        mainWindow.setSize(400, 100);
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
        return "Misc. Crap Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by EBisumaru\n"
            + "Data from asmrange.txt";
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
        String[] miscCrapList = new String[0]; //list of misc. text entries

        try
        {
            miscCrapList = new CommentedLineNumberReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream(DEFAULT_BASE_DIR
                    + "miscData.txt"))).readUsedLines();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            miscCrapList = new String[0];
        }

        entries = new Crap[miscCrapList.length];
        for (int i = 0; i < miscCrapList.length; i++)
        {
            entries[i] = new Crap(Integer.parseInt(
                    miscCrapList[i].substring(2, 8), 16), Integer
                    .parseInt(miscCrapList[i].substring(0,1)), hm,
					miscCrapList[i].substring(9));
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
        for (int i = 0; i < entries.length; i++)
        {
            selector.addItem(entries[i].getDesc());
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
        if (entries[i].getType() == 4 || entries[i].getType() == 6)
        	entries[i].setData(Integer.parseInt(tf.getText()));
        else if (entries[i].getType() == 7)
        	entries[i].setColor(colorBox.getColor());
        else if (entries[i].getType() == 5)
        	entries[i].setData(actionBox.getSelectedIndex());
        else if (entries[i].getType() == 3)
        	entries[i].setData(textBox.getOffset());
        else
        	entries[i].setData(listBox.getSelectedIndex());
        entries[i].writeInfo();
        selector.repaint();
    }

    private void showInfo(int i)
    {
        if (i < 0) return;
        
        if (cType != entries[i].getType())
        {
            cType = entries[i].getType();

       		tf.setVisible(false); listBox.setVisible(false); textBox.setVisible(false);
    		colorBox.setVisible(false); actionBox.setVisible(false);
            switch(cType)
			{
	        	case 1:
	           		listBox.setVisible(true);
	        		listBox.removeAllItems();
	        		for (int j = 0; j< musicNames.length; j++)
	        		{
	        			listBox.addItem(musicNames[j]);
	        		}
//	                mainWindow.setSize(298, 100);
	        		break;
	        	case 2:
	           		listBox.setVisible(true);
	        		listBox.removeAllItems();
	        		for (int j = 0; j< soundEffects.length; j++)
	        		{
	        			listBox.addItem(soundEffects[j]);
	        		}
//	        		listBox.removeItemAt(0);
//	                mainWindow.setSize(400, 100);
	        		break;
	        	case 3:
	        		textBox.setVisible(true);
//	        		mainWindow.setSize(400,100);
	        		break;
	        	case 4: case 6:
	        		tf.setVisible(true); 
//	        		mainWindow.setSize(400, 100);
	        		break;
	        	case 5:
	        		actionBox.setVisible(true);
//	                mainWindow.setSize(400, 100);
	        		break;
	        	case 7:
	        		colorBox.setVisible(true);
//	                mainWindow.setSize(430, 520);
	        		break;
			}
        }
        if (cType == 7) //color
        	colorBox.setColor(entries[i].getColor());
        else if (cType == 4 || cType == 6) //number
        	tf.setText(new Integer(entries[i].getData()).toString());
        else if (cType == 5)//action
        	actionBox.setSelectedIndex(entries[i].getData());
        else if (entries[i].getType() == 3)
        	textBox.setOffset(entries[i].getData());
        else
        	listBox.setSelectedIndex(entries[i].getData());
        mainWindow.pack();
        mainWindow.repaint();
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
    
    /**
     * This class represents any misc data of the compatible types.
     */
    public static class Crap
    {
        private HackModule hm;
        private String desc;
        private int address;
        private int len;
        private int type;
        private int data;
        private Color color;
        /**
         * Creates a new Crap by reading <code>len</code> chars from
         * <code>address</code>
         * 
         * @param address Where to read from.
         * @param len How many characters to read.
         */
        public Crap(int address, int type, HackModule hm, String desc) //desc could be a
        // description or the
        // default text
        {
            this.hm = hm;
            this.address = address;
            this.type = type;
            this.desc = desc;
            this.len = 1;
            if (this.type == 3)
            	len = 4;
            if (this.type > 3)
            	len = 2;
            
            AbstractRom rom = hm.rom;
            
            rom.seek(this.address);

            if (this.type == 7)
            	this.color = rom.readPaletteSeek();
            else if (type == 3)
            {
            	rom.readSeek();
            	this.data = rom.readMultiSeek(2);
            	rom.seek(this.address + 6);
            	this.data += rom.readMultiSeek(2) * 65536;
            }
           	else
           		this.data = rom.readMultiSeek(len);
        }

        /**
         * Changes the text this represents. Doesn't work on titles.
         * 
         * @param newInfo New text.
         */
        public void setData(int _)
        {
        	data = _;
        }
        
        public void setColor(Color _) {color = _;}

        /**
         * Returns the maxium number of bytes this can hold.
         * 
         * @return int
         */
        public int getLen()
        {
            return this.len;
        }
        
        public int getData() {return data;}
        public int getType() {return type;}
        public String toString() {return desc;}
        public String getDesc() {return desc;}
               
        public Color getColor() {return color;}

        /**
         * Writes the information stored in this into the ROM. Does nothing if
         * this is a title.
         */
        public void writeInfo()
        {
            AbstractRom rom = hm.rom;
            
            rom.seek(this.address);
            if (this.type == 7)
            	rom.writePalette(address, color);
            else if (type == 3)
            {
                rom.seek(this.address + 1);
            	rom.writeSeek(data % 65536, 2);
            	rom.seek(this.address + 6);
            	rom.writeSeek(data / 65536, 2);
            }
           	else
           		rom.writeSeek(data, len);
        }
    }
}