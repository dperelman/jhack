package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AutoSearchBox;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Class providing GUI and API for editing the attack effects in Earthbound.
 * 
 * @author EBisumaru
 */

public class ActionEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public ActionEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    private static Action[] actions = new Action[NUM_EFFECTS];
    private static boolean asmEffectsInited;
    private static String[] asmEffects;

    //GUI components
    private JComboBox actionSel, targetSel, effectSel, dirSel;
    private JTextField actionName, ppBox, unknownBox;
    private AutoSearchBox asmAddBox;
    private TextEditor.TextOffsetEntry textAdd;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(400, 250);
        mainWindow.setResizable(true);
        
        readFromRom();
        initEffectsList();

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        entry.add(Box.createVerticalStrut(10));

        actionSel = HackModule.createComboBox(effects, null, this);
        actionSel.setActionCommand("actionSel");
        //actionSel.addActionListener(this);
        mainWindow.getContentPane().add(
            new JSearchableComboBox(actionSel, "Action: "), BorderLayout.NORTH);

        entry.add(getLabeledComponent("Action Name: ",
            actionName = new JTextField(25)));

        dirSel = new JComboBox();
        dirSel.addItem("Enemy");
        dirSel.addItem("Self/Party");
        //		dirSel.setActionCommand("dirSel");
        //		dirSel.addActionListener(this);
        entry.add(getLabeledComponent("Direction:", dirSel));

        targetSel = new JComboBox();
        targetSel.addItem("None");
        targetSel.addItem("One");
        targetSel.addItem("Random one");
        targetSel.addItem("Row");
        targetSel.addItem("All");
        //		targetSel.setActionCommand("targetSel");
        //		targetSel.addActionListener(this);
        entry.add(getLabeledComponent("Target:", targetSel));

        unknownBox = new JTextField(10);
        //		unknownBox.setActionCommand("unknownBox");
        //		unknownBox.addActionListener(this);
        entry.add(getLabeledComponent("Unknown (Miss Rate?):", unknownBox));

        ppBox = new JTextField(10);
        //		ppBox.setActionCommand("ppBox");
        //		ppBox.addActionListener(this);
        entry.add(getLabeledComponent("PP Cost:", ppBox));

        entry.add(Box.createVerticalStrut(5));

        //		textAdd = new TextEditor.TextOffsetEntry();
        //		textAddBox.setActionCommand("textAddBox");
        //		textAddBox.addActionListener(this);
        entry.add(textAdd = new TextEditor.TextOffsetEntry("Text Address",
            true));

        effectSel = new JComboBox(asmEffects);
        effectSel.addItem("UNKNOWN");
        effectSel.setActionCommand("effectSel");
        effectSel.addActionListener(this);

        asmAddBox = new AutoSearchBox(effectSel, "ASM Address", 6, false);
        entry.add(asmAddBox);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        actionSel.setSelectedIndex(0);

        mainWindow.pack();
    }  
    
    public void show(Object object)
	{
    	super.show();
    	readFromRom();
    	this.actionSel.setSelectedIndex(Integer.parseInt(object.toString()));
    	mainWindow.setVisible(true);
    }
    
    public void show()
	{
    	super.show();
    	readFromRom();
    	
    	mainWindow.setVisible(true);
    }

    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < actions.length; i++)
        {
            actions[i] = new Action(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    public static void initEffectsList()
    {
        asmEffectsInited = true;
        asmEffects = new String[300];
        readArray(DEFAULT_BASE_DIR, "asmEffects.txt", true, asmEffects);
       
        String[] tempArray;
        for(int i = 0; i < asmEffects.length; i++)
        {
        	if (asmEffects[i]==asmEffects[299])
        	{
        		tempArray = new String[i];
        		for(int j = 0; j < i; j++)
        			tempArray[j] = asmEffects[j];
        		asmEffects = tempArray;
        	}
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("actionSel"))
        {
            showInfo(actionSel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(actionSel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
   }

    public void saveInfo(int i)
    {
        effects[i] = actionName.getText();
        notifyDataListeners(effects, this, i);
        writeEffects();

        actions[i].setDirection(dirSel.getSelectedIndex());
        actions[i].setTarget(targetSel.getSelectedIndex());
        actions[i].setUnknown(Integer.parseInt(unknownBox.getText()));
        actions[i].setPPCost(Integer.parseInt(ppBox.getText()));
        actions[i].setTextAdd(textAdd.getOffset());
        actions[i].setASMAdd(Integer.parseInt(asmAddBox.getText(), 16));

        actions[i].writeInfo();
    }

    public void showInfo(int i)
    {
        if (!search(addZeros(Integer.toString(actions[i].getASMAdd(), 16), 6),
            effectSel, true, false)) effectSel.setSelectedIndex(0);

        actionName.setText(effects[i]);

        dirSel.setSelectedIndex(actions[i].getDirection());
        targetSel.setSelectedIndex(actions[i].getTarget());
        unknownBox.setText(Integer.toString(actions[i].getUnknown()));
        ppBox.setText(Integer.toString(actions[i].getPPCost()));
        textAdd.setOffset(actions[i].getTextAdd());
        asmAddBox.setText(addZeros(
            Integer.toString(actions[i].getASMAdd(), 16), 6));
    }
    
    /**
     * JPanel containing combobox and hyperlink label that 
     * calls the Action Editor and sets the data being edited
     * to the data in the combobox when clicked
     *
     */
    public static class ActionEntry extends JPanel
	{
    	protected JComboBox cb;
    	protected JLabel t;

    	/**
    	 * Creates a new <code>ActionEntry</code> component.
    	 * 
    	 * @param label words to identify this component with
    	 */
    	public ActionEntry(final String label) {
    		super(new BorderLayout());

    		cb = HackModule.createComboBox(effects);

    		this.add(cb, BorderLayout.EAST);

    		t = new JLabel("<html><font color = \"blue\"><u>" + label
    				+ "</u></font>" + ":" + "</html>");
    		t.addMouseListener(
    		new MouseListener()
    		{
    			public void mouseClicked(MouseEvent arg0)
				{
    				int index = cb.getSelectedIndex();
    				JHack.main.showModule(ActionEditor.class, new Integer(index));
    			}

    			public void mouseEntered(MouseEvent arg0)
				{}

    			public void mouseExited(MouseEvent arg0)
				{}

    			public void mousePressed(MouseEvent arg0)
				{}

    			public void mouseReleased(MouseEvent arg0)
				{}
    		});
    		t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    		this.add(t, BorderLayout.WEST);
    	}

    	public int getSelectedIndex()
		{
    		return cb.getSelectedIndex();
    	}

    	public void setSelectedIndex(int index)
		{
    		cb.setSelectedIndex(index);
    		cb.repaint();
    	}
    	
    	public void setActionCommand(String s)
    	{
    		cb.setActionCommand(s);
    	}
    	
    	public void addActionListener(ActionListener al)
    	{
    		cb.addActionListener(al);
    	}
    	
    	public String getActionCommand()
		{
    		return cb.getActionCommand();
    	}
    }

    public String getDescription()
    {
        return "Action Editor";
    }

    public String getCredits()
    {
        return "Written by EBisumaru\n"
            + "ASM effects documented by EBisumaru\n";
    }

    public String getVersion()
    {
        return "1.2";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /* (non-Javadoc)
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        super.reset();
        readEffects(rom.getPath());
    }

    public static class Action
    {
        private HackModule hm;
        /**
         * Address of this effect entry in the ROM.
         */
        private int address;

        public int getAddress()
        {
            return this.address;
        }
        /**
         * What number action this is.
         */
        private int number;

        public int getNumber()
        {
            return this.number;
        }
        /**
         * Direction of the attack (0 = party, 1 = enemy)
         *
         */
        private int direction;

        public int getDirection()
        {
            return this.direction;
        }

        public void setDirection(int i)
        {
            this.direction = i;
        }
        /**
         * Target of the attack (0 = none, 1 = one, 2 = random one, 3 = row, 4 = all)
         *
         */
        private int target;

        public int getTarget()
        {
            return this.target;
        }

        public void setTarget(int i)
        {
            this.target = i;
        }
        /**
         * unknown byte
         * 
         */
        private int unknown;

        public int getUnknown()
        {
            return this.unknown;
        }

        public void setUnknown(int i)
        {
            this.unknown = i;
        }
        /**
         * PP cost
         *
         */
        private int ppCost;

        public int getPPCost()
        {
            return this.ppCost;
        }

        public void setPPCost(int i)
        {
            this.ppCost = i;
        }
        /**
         * SNES address of the text for this action.
         */
        private int textAddress;

        public int getTextAdd()
        {
            return this.textAddress;
        }

        public void setTextAdd(int i)
        {
            this.textAddress = i;
        }
        /**
         * SNES address of the ASM for this action.
         */
        private int asmAddress;

        public int getASMAdd()
        {
            return this.asmAddress;
        }

        public void setASMAdd(int i)
        {
            this.asmAddress = i;
        }

        public Action(int actionNumber, HackModule hm)
        {
            this.hm = hm;
            AbstractRom rom = hm.rom;
            this.number = actionNumber;

            this.address = 0x157D68 + this.number * 12;
            rom.seek(this.address);

            this.direction = rom.readSeek();
            this.target = rom.readSeek();
            this.unknown = rom.readSeek();
            this.ppCost = rom.readSeek();
            this.textAddress = rom.readMultiSeek(4);
            this.asmAddress = rom.readMultiSeek(4);
        }

        public void writeInfo()
        {
            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            rom.writeSeek(this.direction);
            rom.writeSeek(this.target);
            rom.writeSeek(this.unknown);
            rom.writeSeek(this.ppCost);
            rom.writeSeek(this.textAddress, 4);
            rom.writeSeek(this.asmAddress, 4);
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
}
