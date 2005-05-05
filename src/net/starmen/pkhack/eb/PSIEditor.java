package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AutoSearchBox;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Class providing GUI and API for editing the PSI in Earthbound.
 * 
 * @author EBisumaru
 */

public class PSIEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public PSIEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    private static PSI[] psis = new PSI[NUM_PSI];
    private static MiscText[] PSIText = new MiscText[17];

    //GUI components
    private JComboBox PSISel, nameSel, letterSel, typeSel, animSel, hPosSel;
    private AutoSearchBox letterBox, hPosBox;
    private JTextField nessLevelBox, paulaLevelBox, pooLevelBox, nameBox,
            titleBox, vPosBox;
    private JButton updateButton;
    private TextEditor.TextOffsetEntry helpToe;
    private ActionEditor.ActionEntry actionAe;

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setSize(400, 250);
        mainWindow.setResizable(true);

        readFromRom();
        //      initEffectsList();

        JPanel entry = new JPanel();
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));

        PSISel = createComboBox(psiNames, true, this);
        PSISel.setActionCommand("PSISel");
        PSISel.addActionListener(this);
        mainWindow.getContentPane().add(
            new JSearchableComboBox(PSISel, "PSI: "), BorderLayout.NORTH);

        titleBox = new JTextField(30);
        entry.add(getLabeledComponent("PSI Title", titleBox));

        entry.add(Box.createVerticalStrut(10));

        nameSel = new JComboBox();
        for (int i = 0; i < PSIText.length; i++)
            nameSel.addItem(PSIText[i].getInfo());
        nameSel.setActionCommand("nameSel");
        nameSel.addActionListener(this);

        nameBox = new JTextField(25);
        //      nameSel.setEditable(true);
        entry.add(pairComponents(getLabeledComponent("Name:", nameSel),
            nameBox, true, true, "Name Selector", "Name Box"));

        letterSel = new JComboBox();
        letterSel.addItem("1 \u03B1 (Alpha)");
        letterSel.addItem("2 \u03B2 (Beta)");
        letterSel.addItem("3 \u03B3 (Gamma)");
        letterSel.addItem("4 \u03A3 (Sigma)");
        letterSel.addItem("5 \u03A9 (Omega)");
        //		letterSel.setActionCommand("letterSel");
        //		letterSel.addActionListener(this);
        letterBox = new AutoSearchBox(letterSel, "Power:", 3, false, true);
        entry.add(letterBox);

        typeSel = new JComboBox();
        typeSel.addItem("Offense");
        typeSel.addItem("Recover");
        typeSel.addItem("Assist");
        typeSel.addItem("Other");
        entry.add(getLabeledComponent("Type:", typeSel));

        animSel = new JComboBox();
        animSel.addItem("None");
        animSel.addItem("Normal");
        animSel.addItem("Lines");
        entry.add(getLabeledComponent("Animation:", animSel));

        hPosSel = new JComboBox();
        hPosSel.addItem("9 First");
        hPosSel.addItem("11 Second");
        hPosSel.addItem("13 Third");
        hPosSel.addItem("15 Fourth");
        JTextField hPos = createSizedJTextField(2, true);
        hPosBox = new AutoSearchBox(hPosSel, hPos, "Horizontal Position:", false, true,
        		true);
        entry.add(hPosBox);

        vPosBox = createSizedJTextField(2, true);
        entry.add(getLabeledComponent("Vertical Position (00 is top):", vPosBox));

        nessLevelBox = new JTextField(3);
        entry.add(getLabeledComponent("Ness Level", nessLevelBox));

        paulaLevelBox = new JTextField(3);
        entry.add(getLabeledComponent("Paula Level", paulaLevelBox));

        pooLevelBox = new JTextField(3);
        entry.add(getLabeledComponent("Poo Level", pooLevelBox));

        helpToe = new TextEditor.TextOffsetEntry("Help Address", true);
        entry.add(helpToe);

        actionAe = new ActionEditor.ActionEntry("Action");
        entry.add(actionAe);

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        PSISel.setSelectedIndex(0);

        mainWindow.pack();
    }

    public void show(Object object)
    {
        super.show();
        readFromRom();
        this.PSISel.setSelectedIndex(Integer.parseInt(object.toString()));
        mainWindow.setVisible(true);
    }

    public void show()
    {
        super.show();
        readFromRom();
        showInfo(PSISel.getSelectedIndex());
        mainWindow.setVisible(true);
    }

    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < psis.length; i++)
        {
            psis[i] = new PSI(i, hm);
        }
        for (int i = 0; i < PSIText.length; i++)
        {
            PSIText[i] = new MiscText(0x158f7a + i * 25, 25, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("PSISel"))
        {
            PSISel.repaint();
            showInfo(PSISel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(PSISel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("nameSel"))
        {
            nameBox.setText(nameSel.getItemAt(nameSel.getSelectedIndex())
                .toString());
        }
    }

    /**
     * Updates the PSI name selector to reflect changes.
     *  
     */
    private void nameUpdate()
    {
        int tmp = nameSel.getSelectedIndex();
        nameSel.removeActionListener(this);
        nameSel.removeAllItems();
        for (int i = 0; i < 17; i++)
            nameSel.addItem(PSIText[i].getInfo());

        if (tmp != -1)
            nameSel.setSelectedIndex(tmp);
        nameSel.addActionListener(this);
    }

    public void saveInfo(int i)
    {
        notifyDataListeners(effects, this, i);

        /*
         * EC ASB C C AE TF TF TF ASB C TE name, letter, type, anim, action,
         * nessLevel, paulaLevel, pooLevel, hPos, vPos, helpAdd
         */
        psis[i].setName(nameSel.getSelectedIndex() + 1);
        psis[i].setLetter(Integer.parseInt(letterBox.getText()));
        if ((typeSel.getSelectedIndex() == 0)
            || (typeSel.getSelectedIndex() == 1))
            psis[i].setType(typeSel.getSelectedIndex() + 1);
        else if (typeSel.getSelectedIndex() == 2)
            psis[i].setType(4);
        else
            psis[i].setType(8);
        psis[i].setAnim(animSel.getSelectedIndex() + 1);
        psis[i].setAction(actionAe.getSelectedIndex());
        psis[i].setNessLevel(Integer.parseInt(nessLevelBox.getText()));
        psis[i].setPaulaLevel(Integer.parseInt(paulaLevelBox.getText()));
        psis[i].setPooLevel(Integer.parseInt(pooLevelBox.getText()));
        psis[i].setHPos(Integer.parseInt(hPosBox.getText()));
        psis[i].setVPos(Integer.parseInt(vPosBox.getText()));
        psis[i].setHelpAdd(helpToe.getOffset());

        psis[i].writeInfo();

        PSIText[nameSel.getSelectedIndex()].setInfo(nameBox.getText());
        PSIText[nameSel.getSelectedIndex()].writeInfo();

        nameUpdate();

        int temp = PSISel.getSelectedIndex();
        PSISel.removeActionListener(this);
        psiNames[PSISel.getSelectedIndex()] = titleBox.getText();
        notifyDataListeners(psiNames, this, PSISel.getSelectedIndex());
        writeArray("psiNames.txt", false, psiNames);
        PSISel.setSelectedIndex(temp);
        PSISel.addActionListener(this);
        PSISel.repaint();
    }

    public void showInfo(int i)
    {
        nameSel.setSelectedIndex(psis[i].getName() - 1);
        nameBox.setText(nameSel.getItemAt(nameSel.getSelectedIndex())
            .toString());
        letterBox.setText(Integer.toString(psis[i].getLetter()));
        if ((psis[i].getType() == 1) || (psis[i].getType() == 2))
            typeSel.setSelectedIndex(psis[i].getType() - 1);
        else if (psis[i].getType() == 4)
            typeSel.setSelectedIndex(2);
        else
            typeSel.setSelectedIndex(3);
        animSel.setSelectedIndex(psis[i].getAnim() - 1);
        actionAe.setSelectedIndex(psis[i].getAction());
        actionAe.repaint();
        nessLevelBox.setText(Integer.toString(psis[i].getNessLevel()));
        paulaLevelBox.setText(Integer.toString(psis[i].getPaulaLevel()));
        pooLevelBox.setText(Integer.toString(psis[i].getPooLevel()));
        hPosBox.setText(Integer.toString(psis[i].getHPos()));
        vPosBox.setText(Integer.toString(psis[i].getVPos()));
        helpToe.setOffset(psis[i].getHelpAdd());
        titleBox.setText(psiNames[i]);
    }

    /**
     * I dunno...
     */

    public String getDescription()
    {
        return "PSI Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by EBisumaru\n"
            + "PSI Table discovered and documented by unknown\n";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "1.0";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        super.reset();
        readArray(DEFAULT_BASE_DIR, "psiNames.txt", rom.getPath(), false,
            psiNames);
    }

    public static class PSI
    {
        private HackModule hm;
        /**
         * Address of this PSI in the ROM.
         */
        private int address;

        /**
         * Gets address of this PSI in the ROM.
         */
        public int getAddress()
        {
            return this.address;
        }
        /**
         * Number of this entry in the table
         */
        private int number;

        /**
         * Gets number of this entry in the table
         */
        public int getNumber()
        {
            return this.number;
        }
        /**
         * Name of PSI (uses name table)
         *  
         */
        private int name;

        /**
         * Gets name of PSI (uses name table)
         *  
         */
        public int getName()
        {
            return this.name;
        }

        /**
         * Sets name of PSI (uses name table)
         *  
         */
        public void setName(int i)
        {
            name = i;
        }
        /**
         * Greek letter of the PSI(1 = alpha, 2 = beta, 3 = gamma, 4 = sigma, 5 =
         * omega)
         *  
         */
        private int letter;

        /**
         * Gets Greek letter of the PSI(1 = alpha, 2 = beta, 3 = gamma, 4 =
         * sigma, 5 = omega)
         *  
         */
        public int getLetter()
        {
            return this.letter;
        }

        /**
         * Sets Greek letter of the PSI(1 = alpha, 2 = beta, 3 = gamma, 4 =
         * sigma, 5 = omega)
         *  
         */
        public void setLetter(int i)
        {
            this.letter = i;
        }
        /**
         * Type of PSI (1 = Offense, 2 = Recover, 4 = Assist, 8 = Other)
         *  
         */
        private int type;

        /**
         * Gets type of PSI (1 = Offense, 2 = Recover, 4 = Assist, 8 = Other)
         *  
         */
        public int getType()
        {
            return this.type;
        }

        /**
         * Sets type of PSI (1 = Offense, 2 = Recover, 4 = Assist, 8 = Other)
         *  
         */
        public void setType(int i)
        {
            this.type = i;
        }
        /**
         * Animation style
         *  
         */
        private int anim;

        /**
         * Gets animation style
         *  
         */
        public int getAnim()
        {
            return this.anim;
        }

        /**
         * Sets animation style
         *  
         */
        public void setAnim(int i)
        {
            this.anim = i;
        }
        /**
         * Action this PSI uses
         *  
         */
        private int action;

        /**
         * Gets action this PSI uses
         *  
         */
        public int getAction()
        {
            return this.action;
        }

        /**
         * Sets action this PSI uses
         *  
         */
        public void setAction(int i)
        {
            this.action = i;
        }
        /**
         * Level at which Ness learns this PSI
         *  
         */
        private int nessLevel;

        /**
         * Gets level at which Ness learns this PSI
         *  
         */
        public int getNessLevel()
        {
            return this.nessLevel;
        }

        /**
         * Sets level at which Ness learns this PSI
         *  
         */
        public void setNessLevel(int i)
        {
            this.nessLevel = i;
        }
        /**
         * Level at which Paula learns this PSI
         *  
         */
        private int paulaLevel;

        /**
         * Gets level at which Paula learns this PSI
         *  
         */
        public int getPaulaLevel()
        {
            return this.paulaLevel;
        }

        /**
         * Sets level at which Paula learns this PSI
         *  
         */
        public void setPaulaLevel(int i)
        {
            this.paulaLevel = i;
        }
        /**
         * Level at which Poo learns this PSI
         *  
         */
        private int pooLevel;

        /**
         * Gets level at which Poo learns this PSI
         *  
         */
        public int getPooLevel()
        {
            return this.pooLevel;
        }

        /**
         * Sets level at which Poo learns this PSI
         *  
         */
        public void setPooLevel(int i)
        {
            this.pooLevel = i;
        }
        /**
         * Horizontal position (9 = 1st, 11 = 2nd, 13 = 3rd, 15 = 4th)
         *  
         */
        private int hPos;

        /**
         * Gets horizontal position (9 = 1st, 11 = 2nd, 13 = 3rd, 15 = 4th)
         *  
         */
        public int getHPos()
        {
            return this.hPos;
        }

        /**
         * Sets horizontal position (9 = 1st, 11 = 2nd, 13 = 3rd, 15 = 4th)
         *  
         */
        public void setHPos(int i)
        {
            this.hPos = i;
        }
        /**
         * Vertical position (0 = top, 1 = mid, 2 = bot)
         *  
         */
        private int vPos;

        /**
         * Gets vertical position (0 = top, 1 = mid, 2 = bot)
         *  
         */
        public int getVPos()
        {
            return this.vPos;
        }

        /**
         * Sets vertical position (0 = top, 1 = mid, 2 = bot)
         *  
         */
        public void setVPos(int i)
        {
            this.vPos = i;
        }
        /**
         * SNES address of help text
         *  
         */
        private int helpAdd;

        /**
         * Gets SNES address of help text
         *  
         */
        public int getHelpAdd()
        {
            return this.helpAdd;
        }

        /**
         * Sets SNES address of help text
         *  
         */
        public void setHelpAdd(int i)
        {
            this.helpAdd = i;
        }

        /**
         * Constructor of PSI data entry
         * 
         * @param PSINumber Number of this PSI in the table
         * @param hm HackModule to use
         */
        public PSI(int PSINumber, HackModule hm)
        {
            this.hm = hm;
            AbstractRom rom = hm.rom;
            this.number = PSINumber;

            this.address = 0x158C5F + this.number * 15;
            rom.seek(this.address);

            this.name = rom.readSeek();
            this.letter = rom.readSeek();
            this.type = rom.readSeek();
            this.anim = rom.readSeek();
            this.action = rom.readMultiSeek(2);
            this.nessLevel = rom.readSeek();
            this.paulaLevel = rom.readSeek();
            this.pooLevel = rom.readSeek();
            this.hPos = rom.readSeek();
            this.vPos = rom.readSeek();
            this.helpAdd = rom.readMultiSeek(4);
        }

        /**
         * Writes entry's info to the ROM
         *  
         */
        public void writeInfo()
        {
            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            rom.writeSeek(this.name);
            rom.writeSeek(this.letter);
            rom.writeSeek(this.type);
            rom.writeSeek(this.anim);
            rom.writeSeek(this.action, 2);
            rom.writeSeek(this.nessLevel);
            rom.writeSeek(this.paulaLevel);
            rom.writeSeek(this.pooLevel);
            rom.writeSeek(this.hPos);
            rom.writeSeek(this.vPos);
            rom.writeSeek(this.helpAdd, 4);
        }
    }

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

            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            for (int j = 0; j < info.length; j++)
            {
                this.info[j] = hm.simpToRegChar(rom.readCharSeek());
            }
        }

        /**
         * Creates a MiscText to be used as a title in the
         * <code>JComboBox</code>. The title is displayed in all caps with an
         * underscore added on both sides.
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
            if (!isRealEntry)
                return;
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
            if (!isRealEntry)
                return;

            AbstractRom rom = hm.rom;

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