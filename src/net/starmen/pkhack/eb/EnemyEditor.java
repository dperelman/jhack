package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JLinkComboBox;
import net.starmen.pkhack.JSearchableComboBox;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.FontEditor.StringViewer;

/**
 * Edits enemies table in Earthbound.
 * 
 * @author AnyoneEB
 */
public class EnemyEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public EnemyEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static final int NUM_ENEMIES = 231;

    /**
     * Array of the all entries in the Earthbound enemies table.
     * 
     * @see EnemyEditor.Enemy
     * @see #readFromRom(HackModule)
     */
    public static Enemy[] enemies = new Enemy[NUM_ENEMIES]; //data
    private static short[] gameOrder = new short[NUM_ENEMIES];
    private static boolean useGameOrder = false;

    private static Icon fobbyIcon = initIcon();
    private JComboBox selector;
    private JTextField search;

    private JRadioButton ordGame, ordReg;

    //stats tab stuff
    private JTextField name, hp, pp, exp, money, speed, offense, defense,
            level, guts, iq;
    private JCheckBox theFlag, runFlag, bossFlag;
    private JComboBox gender, itemFreq, status;
    private ItemEditor.ItemEntry item;
    private JTextField missRate;
    private JComboBox type;

    //actions tab stuff
    //action/argument 4 (the 5th one) is final action/arguement
    private ActionEditor.ActionEntry[] actions = new ActionEditor.ActionEntry[5];
    private JComboBox[] arguements = new JComboBox[5];
    private JComboBox actionOrder; //, finalAction;
    private JTextField maxCall;
    private TextEditor.TextOffsetEntry startPointer, deathPointer;

    //gfx/sound tab stuff
    private JLinkComboBox outsidePic, insidePic;
    private JComboBox deathSound, music, row;
    private JTextField movement, palette;
    private JLabel insidePicPrev;

    //weakness tab stuff
    private JComboBox weakness[] = new JComboBox[5];
    private JTextField mirrorRate;

    //unknowns tab stuff
    private JTextField unknowna, unknownb, unknownc, unknownd, unknowne,
            unknownf, unknowng, unknownh, unknowni, unknownj, unknownk,
            unknownl;

    protected void init()
    {
        readFromRom();
        SpriteEditor.readFromRom(rom);

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
        mainWindow.setResizable(false);

        selector = EnemyEditor.createEnemyComboBox(this);
        selector.setActionCommand("enemySelector");

        ButtonGroup ordBG = new ButtonGroup();
        ordGame = new JRadioButton("Game Order");
        ordGame.setSelected(false);
        ordGame.setActionCommand("ordGame");
        ordGame.addActionListener(this);
        ordBG.add(ordGame);
        ordReg = new JRadioButton("ROM Order");
        ordReg.setSelected(true);
        ordReg.setActionCommand("ordReg");
        ordReg.addActionListener(this);
        ordBG.add(ordReg);

        mainWindow.getContentPane().add(
            pairComponents(
                createFlowLayout(new JRadioButton[]{ordGame, ordReg}),
                createFlowLayout(new JSearchableComboBox(selector, "Enemy: ")),
                true), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        //stats tab init
        JPanel statsTab = new JPanel(new BorderLayout());

        name = createSizedJTextField(25);
        statsTab.add(pairComponents(StringViewer.createWithFontSelector(name,
            this), pairComponents(getLabeledComponent("Name: ", name),
            theFlag = new JCheckBox("'The' Flag"), true, false, null,
            "If checked 'The' is put before the enemy's name."), false),
            BorderLayout.NORTH);

        Box statsTabLeft = new Box(BoxLayout.Y_AXIS);
        statsTabLeft.add(getLabeledComponent("HP: ",
            (hp = createSizedJTextField(5, true))));
        statsTabLeft.add(getLabeledComponent("PP: ",
            (pp = createSizedJTextField(5, true))));
        statsTabLeft.add(getLabeledComponent("Exp:          ",
            (exp = createSizedJTextField(10, true))));
        statsTabLeft.add(getLabeledComponent("Money: ",
            (money = createSizedJTextField(5, true))));
        statsTabLeft.add(getLabeledComponent("Speed: ",
            (speed = createSizedJTextField(3, true))));
        statsTabLeft.add(getLabeledComponent("Offense: ",
            (offense = createSizedJTextField(3, true))));
        statsTabLeft.add(getLabeledComponent("Defense: ",
            (defense = createSizedJTextField(3, true))));
        status = new JComboBox();
        status.addItem("Normal");
        status.addItem("PSI Shield \u03B1 (Blocks PSI)");
        status.addItem("PSI Shield \u03B2 (Reflects PSI)");
        status.addItem("Shield Alpha \u03B1 (Blocks physical)");
        status.addItem("Shield Alpha \u03B2 (Reflects physical)");
        status.addItem("Asleep");
        status.addItem("Can't concentrate");
        status.addItem("Feeling strange");
        statsTabLeft.add(getLabeledComponent("Status: ", status));
        statsTabLeft.add(getLabeledComponent("Level: ",
            (level = createSizedJTextField(3, true))));
        statsTabLeft.add(getLabeledComponent("Guts: ",
            (guts = createSizedJTextField(3, true))));
        statsTabLeft.add(getLabeledComponent("IQ: ",
            (iq = createSizedJTextField(3, true))));
        statsTab.add(pairComponents(statsTabLeft, new JLabel(), false),
            BorderLayout.WEST);

        Box statsTabRight = new Box(BoxLayout.Y_AXIS);
        statsTabRight.add(HackModule.getLabeledComponent("Gender: ",
            gender = new JComboBox()));
        gender.addItem(HackModule.getNumberedString("Male", 1));
        gender.addItem(HackModule.getNumberedString("Female", 2));
        gender.addItem(HackModule.getNumberedString("Neutral", 3));
        gender.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                unknowna.setText(Integer
                    .toString(gender.getSelectedIndex() + 1));
            }
        });
        statsTabRight.add(Box.createVerticalStrut(40));
        statsTabRight.add(pairComponents(runFlag = new JCheckBox(
            "Fear higher leveled PCs?"), new JLabel(), true));
        runFlag
            .setToolTipText("If checked this enemy will run from your party "
                + "(out of battle).");
        statsTabRight.add(pairComponents(bossFlag = new JCheckBox(
            "Impossible to run from"), new JLabel(), true));
        bossFlag.setToolTipText("If checked, attempts to use the run battle "
            + "command automatically fail.");
        statsTabRight.add(Box.createVerticalStrut(10));
        statsTabRight.add(item = new ItemEditor.ItemEntry("Item", this, null));
        statsTabRight.add(getLabeledComponent("Item Freq: ",
            itemFreq = new JComboBox()));
        //init itemFreq
        for (int i = 0; i < 8; i++)
        {
            itemFreq.addItem(getNumberedString(Integer.toString((int) Math.pow(
                2, i))
                + "/128", i));
        }
        statsTabRight.add(Box.createVerticalStrut(10));
        statsTabRight.add(getLabeledComponent("Miss Rate: ",
            (missRate = createSizedJTextField(3, true))));
        statsTabRight
            .add(getLabeledComponent("Type: ", type = new JComboBox()));
        type.addItem(getNumberedString("Normal", 0));
        type.addItem(getNumberedString("Insect", 1));
        type.addItem(getNumberedString("Metal", 2));
        statsTabRight.add(Box.createVerticalStrut(10));
        statsTab.add(pairComponents(statsTabRight, new JLabel(), false),
            BorderLayout.EAST);

        tabs.addTab("Stats", pairComponents(statsTab, new JLabel(), true));

        //actions tab init
        JPanel actionsTab = new JPanel(new BorderLayout());

        JPanel actionsTabTop = new JPanel();
        actionsTabTop.setLayout(new BoxLayout(actionsTabTop, BoxLayout.Y_AXIS));
        String[] tmp = new String[256];
        Arrays.fill(tmp, "");
        String afk;
        for (int i = 0; i < actions.length; i++)
        {
            afk = "Action " + (i + 1);
            if (i == 4)
            {
                actionsTabTop.add(Box.createVerticalStrut(10));
                afk = "End Action";
            }
            actions[i] = new ActionEditor.ActionEntry(afk);
            actionsTabTop.add(pairComponents(actions[i], getLabeledComponent(
            //                        (i < 4 ? "Action " + (i + 1) + ": " : "End Action: "),
                //                        actions[i] =
                //                            createComboBox(effects)),
                //                    getLabeledComponent(
                (i < 4 ? "Argument " + (i + 1) + ": " : "End Argument: "),
                arguements[i] = createJComboBoxFromArray(tmp)), true, true,
                null, null));
            actions[i].setSelectedIndex(0);
            arguements[i].setSelectedIndex(0);
            final int z = i;
            ListDataListener argldl = new ListDataListener()
            {
                public void contentsChanged(ListDataEvent lde)
                {
                    if (arguements[z].getSelectedIndex() == -1)
                        arguements[z].setSelectedIndex(lde.getIndex0());
                }

                public void intervalAdded(ListDataEvent arg0)
                {}

                public void intervalRemoved(ListDataEvent arg0)
                {}
            };
            ItemEditor.addItemDataListener(argldl);
            //EnemyEditor.addEnemyDataListener(argldl);
            actions[i].setActionCommand("enemyActionChanged" + i);
            actions[i].addActionListener(this);
            for (int j = 0; j < 256; j++)
            {
                arguements[i].addItem(getNumberedString("", j));
            }
        }
        actionsTab.add(actionsTabTop, BorderLayout.NORTH);

        JPanel actionsTabBottom = new JPanel();
        actionsTabBottom.setLayout(new BoxLayout(actionsTabBottom,
            BoxLayout.Y_AXIS));
        actionsTabBottom
            .add(getLabeledComponent(
                "Order: ",
                actionOrder = new JComboBox(),
                "Staggered Order goes something like \"Attack 3, 4, 3, 4, 3, 4, 3, 1, 2, 1, 2, etc.\""));
        actionOrder.addItem(getNumberedString("Random", 0));
        actionOrder.addItem(getNumberedString("Random, Favor Third", 1));
        actionOrder.addItem(getNumberedString("In Order", 2));
        actionOrder.addItem(getNumberedString("Staggered Order", 3));
        actionsTabBottom.add(startPointer = new TextEditor.TextOffsetEntry(
            "Start Text", true));
        actionsTabBottom.add(deathPointer = new TextEditor.TextOffsetEntry(
            "Death Text", true));
        actionsTabBottom.add(getLabeledComponent("Max Call: ",
            maxCall = createSizedJTextField(3, true),
            "Values over 8 are treated as 8 by EB"));
        actionsTab.add(pairComponents(actionsTabBottom, new JLabel(), true),
            BorderLayout.SOUTH);

        tabs.addTab("Actions", actionsTab);

        //gfx/sound tab init
        JPanel appearenceTab = new JPanel();
        appearenceTab.setLayout(new BoxLayout(appearenceTab, BoxLayout.Y_AXIS));
        appearenceTab.add(outsidePic = new JLinkComboBox(SPTEditor.class,
            sptNames, "Outside Pic", JSearchableComboBox.SEARCH_EDIT));
        outsidePic.setToolTipText("Appearance out of battle.");
        appearenceTab.add(Box.createVerticalStrut(5));
        appearenceTab.add(getLabeledComponent("Movement: ",
            movement = createSizedJTextField(5, true),
            "Movement pattern, most values unknown"));
        appearenceTab.add(Box.createVerticalStrut(10));
        appearenceTab.add(getLabeledComponent("Row: ", row = HackModule
            .createJComboBoxFromArray(new String[]{"Front", "Back"}, false)));
        appearenceTab.add(Box.createVerticalStrut(10));
        appearenceTab
            .add(insidePic = new JLinkComboBox(BattleSpriteEditor.class,
                createComboBox(battleSpriteNames, "Invisible"),
                "Battle Sprite", JSearchableComboBox.SEARCH_EDIT));
        insidePic.setToolTipText("Appearance in battle.");
        insidePic.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                updateInsidePicPrev();
            }
        });
        appearenceTab.add(getLabeledComponent("Palette: ",
            palette = createSizedJTextField(3, true),
            "Colors used for battle sprite (0-31)"));
        palette.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent e)
            {
                updateInsidePicPrev();
            }

            public void insertUpdate(DocumentEvent e)
            {
                updateInsidePicPrev();
            }

            public void removeUpdate(DocumentEvent e)
            {
                updateInsidePicPrev();
            }
        });
        appearenceTab.add(Box.createVerticalStrut(10));
        appearenceTab.add(getLabeledComponent("Death Sound: ",
            deathSound = new JComboBox()));
        deathSound.addItem(getNumberedString("Normal", 0));
        deathSound.addItem(getNumberedString("Boss", 1));
        appearenceTab.add(getLabeledComponent("Music: ",
            music = createJComboBoxFromArray(musicNames)));
        tabs.addTab("Graphics / Sound", pairComponents(pairComponents(
            appearenceTab, insidePicPrev = new JLabel(), true), new JLabel(),
            false));

        //weaknesses tab init
        Box weaknessTab = new Box(BoxLayout.Y_AXIS);
        String[] weaknessNames = new String[]{"PSI Fire", "PSI Freeze",
            "PSI Flash", "Paralysis", "Hypnosis/Brainshock"}, weaknessCat = new String[]{
            "100% effective", "75% effective", "50% effective",
            "25% effective", "1% effective"}, ffWeaknessCat = new String[]{
            "200% effective", "150% effective", "100% effective",
            "50% effective", "1% effective"};
        for (int i = 0; i < weaknessNames.length; i++)
        {
            weaknessTab.add(getLabeledComponent(weaknessNames[i],
                weakness[i] = HackModule.createJComboBoxFromArray(i < 2
                    ? ffWeaknessCat
                    : weaknessCat)));
        }
        weaknessTab.add(getLabeledComponent("Mirror success rate: ",
            pairComponents(mirrorRate = HackModule.createSizedJTextField(3,
                true), new JLabel("%"), true)));
        tabs.addTab("Weaknesses", pairComponents(pairComponents(weaknessTab,
            new JLabel(), true), new JLabel(), false));

        //unknowns tab init
        JPanel unknownsTab = new JPanel();
        unknownsTab.setLayout(new BoxLayout(unknownsTab, BoxLayout.Y_AXIS));

        unknownsTab.add(getLabeledComponent("Unknown A (gender): ",
            unknowna = createSizedJTextField(3, true),
            "Replaced by gender on the first tab."));
        unknowna.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown B (PSI Fire): ",
            unknownb = createSizedJTextField(3, true),
            "Replaced by PSI Fire in weaknesses tab."));
        unknownb.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown C (PSI Freeze): ",
            unknownc = createSizedJTextField(3, true),
            "Replaced by PSI Freese in weaknesses tab."));
        unknownc.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown D (PSI Flash): ",
            unknownd = createSizedJTextField(3, true),
            "Replaced by PSI Flash in weaknesses tab."));
        unknownd.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown E (Paralysis): ",
            unknowne = createSizedJTextField(3, true),
            "Replaced by Paralysis in weaknesses tab."));
        unknowne.setEnabled(false);
        unknownsTab.add(getLabeledComponent(
            "Unknown F (Hypnosis/Brainshock): ",
            unknownf = createSizedJTextField(3, true),
            "Replaced by Hypnosis/Brainshock in weaknesses tab."));
        unknownf.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown G (final action): ",
            unknowng = createSizedJTextField(3, true),
            "Final action arguement. Edit on the actions tab."));
        unknowng.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown H: ",
            unknownh = createSizedJTextField(3, true),
            "Byte after Unknown G, see: unknown3.txt"));
        unknownsTab.add(getLabeledComponent("Unknown I (row): ",
            unknowni = createSizedJTextField(3, true),
            "Replaced by row in Graphics/Sound tab."));
        unknowni.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown J (mirror percentage): ",
            unknownj = createSizedJTextField(3, true),
            "Replaced by mirror success rate (%) on weaknesses tab"));
        unknownj.setEnabled(false);
        unknownsTab.add(getLabeledComponent("Unknown K: ",
            unknownk = createSizedJTextField(3, true),
            "Byte after offense, offense is confirmed one byte"));
        unknownsTab
            .add(getLabeledComponent("Unknown L: ",
                unknownl = createSizedJTextField(3, true),
                "Byte after defense, defense is confirmed one byte; see unknown4.txt"));

        tabs.addTab("Unknowns", pairComponents(pairComponents(new JLabel(
            "Unknown*.txt's at http://pkhack.starmen.net/old/misc/"),
            pairComponents(unknownsTab, new JLabel(), true), false, true, null,
            null), new JLabel(), false));

        mainWindow.getContentPane().add(tabs, BorderLayout.CENTER);

        selector.setSelectedIndex(0);
        mainWindow.pack();
        //make sure there's room for arguments
        mainWindow.setSize(mainWindow.getWidth() + 50, mainWindow.getHeight());
    }

    /**
     * @see net.starmen.pkhack.HackModule#getVersion()
     */
    public String getVersion()
    {
        return "0.6";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getDescription()
     */
    public String getDescription()
    {
        return "Enemy Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n"
            + "Table format from Tomato's source code\n"
            + "Gender byte discovered by BlueAntoid (with help from AnyoneEB :)\n"
            + "Final action and row identified by EBisumaru";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        readFromRom();
        BattleSpriteEditor.readFromRom(this);
        selector.setSelectedIndex(selector.getSelectedIndex());
        mainWindow.setVisible(true);
    }

    /**
     * Reads from the ROM into {@link #enemies}.
     */
    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < enemies.length; i++)
        {
            enemies[i] = new Enemy(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    private void updateArguements(int j)
    {
        int temp = getNumberOfString(arguements[j].getSelectedItem().toString());
        //make arguements[j] have the right list
        switch (actionType[actions[j].getSelectedIndex()])
        {
            case TYPE_ITEM:
                arguements[j] = ItemEditor.createItemSelector(arguements[j],
                    this);
                break;

            case TYPE_CALL:
                arguements[j].setModel(EnemyEditor.createEnemyComboBoxModel());
                break;

            case TYPE_PSI:
                arguements[j].setModel(createComboBoxModel(psiNames, "Null"));
                //                arguements[j] =
                //                    createComboBoxFromArray(
                //                        psiNames,
                //                        arguements[j]);
                break;

            case TYPE_NORMAL:
            default:
                String[] tmp = new String[256];
                Arrays.fill(tmp, "");
                arguements[j] = createJComboBoxFromArray(tmp, arguements[j]);
                break;
        }
        try
        {
            if (!search(getNumberedString("", temp), arguements[j]))
                arguements[j].setSelectedIndex(0);
            //arguements[j].setSelectedIndex(temp);
        }
        catch (IllegalArgumentException e)
        {
            //new list doesn't contain old number
            JOptionPane.showMessageDialog(mainWindow,
                "The argument of action #" + (j + 1)
                    + " had an illegal value for the action type.\n"
                    + "The value was 0x" + Integer.toHexString(temp) + " ("
                    + temp + " decimal). It will now be set to zero.",
                "Illegal Argument Value", JOptionPane.WARNING_MESSAGE);
            arguements[j].setSelectedIndex(0);
        }
    }

    private void updateInsidePicPrev()
    {
        try
        {
            insidePicPrev.setIcon(new ImageIcon(BattleSpriteEditor.getImage(
                insidePic.getSelectedIndex() - 1, Integer.parseInt(palette
                    .getText()))));
        }
        catch (NumberFormatException e)
        {
            //contents of palette text field bad, don't redraw
        }
    }

    private void showInfo(int i)
    {
        if (i < 0)
            return;
        //stats tab - left side
        name.setText(enemies[i].getName());
        hp.setText(Integer.toString(enemies[i].getHp()));
        pp.setText(Integer.toString(enemies[i].getPp()));
        exp.setText(Integer.toString(enemies[i].getExp()));
        money.setText(Integer.toString(enemies[i].getMoney()));
        speed.setText(Integer.toString(enemies[i].getSpeed()));
        offense.setText(Integer.toString(enemies[i].getOffense()));
        defense.setText(Integer.toString(enemies[i].getDefense()));
        status.setSelectedIndex(enemies[i].getStatus());
        level.setText(Integer.toString(enemies[i].getLevel()));
        guts.setText(Integer.toString(enemies[i].getGuts()));
        iq.setText(Integer.toString(enemies[i].getIq()));
        //stats tab - right side
        theFlag.setSelected(enemies[i].getTheFlag());
        runFlag.setSelected((enemies[i].getRunFlag() & 1) == 1);
        bossFlag.setSelected((enemies[i].getBossFlag() & 1) == 1);
        int gi = enemies[i].getGender() - 1; //gender index; 0 isn't used
        if (gi >= 0 && gi < 3)
            gender.setSelectedIndex(gi);
        else
        {
            JOptionPane.showMessageDialog(mainWindow,
                "The enemy you have selected has an invalid\n"
                    + "value set for gender (a.k.a. Unknown A) of " + (gi + 1)
                    + ".\n" + "The gender value has been set to [01] Male.",
                "Invaild Gender Value", JOptionPane.WARNING_MESSAGE);
            gender.setSelectedIndex(0);
        }
        item.setSelectedIndex(enemies[i].getItem());
        itemFreq.setSelectedIndex(enemies[i].getFreq());
        missRate.setText(Integer.toString(enemies[i].getMissRate()));
        type.setSelectedIndex(enemies[i].getType());

        //actions tab
        for (int j = 0; j < actions.length; j++)
        {
            //set to zero to make sure no incorrect error message is
            //shown by updateArguements()
            arguements[j].setSelectedIndex(0);
            actions[j].setSelectedIndex(enemies[i].getAction(j));
            //updateArguements(j); //done by actionPerformed
            int earg = enemies[i].getArguement(j);
            if (earg >= arguements[j].getItemCount())
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "The argument of action #" + (j + 1)
                        + " had an illegal value for the action type.\n"
                        + "The value was 0x" + Integer.toHexString(earg) + " ("
                        + earg + " decimal). It will now be set to zero.",
                    "Illegal Argument Value", JOptionPane.WARNING_MESSAGE);
                arguements[j].setSelectedIndex(0);
            }
            else
            {
                if (!search(getNumberedString("", earg), arguements[j]))
                    arguements[j].setSelectedIndex(0);
                //arguements[j].setSelectedIndex(earg);
            }
        }
        actionOrder.setSelectedIndex(enemies[i].getOrder());
        //finalAction.setSelectedIndex(enemies[i].getFinalAction());
        //		startPointer.setText(
        //			addZeros(Integer.toString(enemies[i].getStartPointer(), 16), 6));
        //		deathPointer.setText(
        //			addZeros(Integer.toString(enemies[i].getDeathPointer(), 16), 6));
        startPointer.setOffset(enemies[i].getStartPointer());
        deathPointer.setOffset(enemies[i].getDeathPointer());
        maxCall.setText(Integer.toString(enemies[i].getMaxCall()));

        //gfx/sound tab
        outsidePic.setSelectedIndex(enemies[i].getOutsidePic()
            % outsidePic.comboBox.getItemCount());
        movement.setText(Integer.toString(enemies[i].getMovement()));
        row.setSelectedIndex(enemies[i].getRow());
        insidePic.setSelectedIndex(enemies[i].getInsidePic());
        //        insidePic.getEditor().setItem(insidePic.getSelectedItem());
        palette.setText(Integer.toString(enemies[i].getPalette()));
        try
        {
            deathSound.setSelectedIndex(enemies[i].getDieSound());
        }
        catch (IllegalArgumentException iae)
        {
            JOptionPane.showConfirmDialog(mainWindow, iae,
                "Invalid death sound value in ROM", JOptionPane.OK_OPTION,
                JOptionPane.ERROR_MESSAGE);
        }
        music.setSelectedIndex(enemies[i].getMusic());

        updateInsidePicPrev();

        //weaknesses tab
        for (int j = 0; j < weakness.length; j++)
            weakness[j].setSelectedIndex(enemies[i].getWeakness(j));
        mirrorRate.setText(Integer.toString(enemies[i].getMirrorPercent()));

        //unknowns tab
        unknowna.setText(Integer.toString(enemies[i].getUnknowna()));
        unknownb.setText(Integer.toString(enemies[i].getUnknownb()));
        unknownc.setText(Integer.toString(enemies[i].getUnknownc()));
        unknownd.setText(Integer.toString(enemies[i].getUnknownd()));
        unknowne.setText(Integer.toString(enemies[i].getUnknowne()));
        unknownf.setText(Integer.toString(enemies[i].getUnknownf()));
        unknowng.setText(Integer.toString(enemies[i].getUnknowng()));
        unknownh.setText(Integer.toString(enemies[i].getUnknownh()));
        unknowni.setText(Integer.toString(enemies[i].getUnknowni()));
        unknownj.setText(Integer.toString(enemies[i].getUnknownj()));
        unknownk.setText(Integer.toString(enemies[i].getUnknownk()));
        unknownl.setText(Integer.toString(enemies[i].getUnknownl()));

        mainWindow.repaint();
    }

    private void saveInfo(int i)
    {
        if (i < 0)
            return;
        try
        {
            //stats tab - left side
            enemies[i].setName(name.getText());
            EnemyEditor.notifyEnemyDataListeners(new ListDataEvent(this,
                ListDataEvent.CONTENTS_CHANGED, i, i));
            enemies[i].setHp(Integer.parseInt(hp.getText()));
            enemies[i].setPp(Integer.parseInt(pp.getText()));
            enemies[i].setExp(Integer.parseInt(exp.getText()));
            enemies[i].setMoney(Integer.parseInt(money.getText()));
            enemies[i].setSpeed(Integer.parseInt(speed.getText()));
            enemies[i].setOffense(Integer.parseInt(offense.getText()));
            enemies[i].setDefense(Integer.parseInt(defense.getText()));
            enemies[i].setStatus(status.getSelectedIndex());
            enemies[i].setLevel(Integer.parseInt(level.getText()));
            enemies[i].setGuts(Integer.parseInt(guts.getText()));
            enemies[i].setIq(Integer.parseInt(iq.getText()));
            //stats tab - right side
            enemies[i].setTheFlag(theFlag.isSelected());
            enemies[i].setRunFlag((enemies[i].getRunFlag() & 0xfe)
                | (runFlag.isSelected() ? 1 : 0));
            enemies[i].setBossFlag((enemies[i].getBossFlag() & 0xfe)
                | (bossFlag.isSelected() ? 1 : 0));
            enemies[i].setGender(gender.getSelectedIndex() + 1); //0 isn't used
            if (item.getSelectedIndex() != -1)
                enemies[i].setItem(item.getSelectedIndex());
            enemies[i].setFreq(itemFreq.getSelectedIndex());
            enemies[i].setMissRate(Integer.parseInt(missRate.getText()));
            enemies[i].setType(type.getSelectedIndex());

            //actions tab
            for (int j = 0; j < actions.length; j++)
            {
                enemies[i].setAction(j, actions[j].getSelectedIndex());
                enemies[i].setArguement(j, getNumberOfString(arguements[j]
                    .getSelectedItem().toString()));
            }
            enemies[i].setOrder(actionOrder.getSelectedIndex());
            //enemies[i].setFinalAction(finalAction.getSelectedIndex());
            //		enemies[i].setStartPointer(
            //			Integer.parseInt(startPointer.getText(), 16));
            //		enemies[i].setDeathPointer(
            //			Integer.parseInt(deathPointer.getText(), 16));
            enemies[i].setStartPointer(startPointer.getOffset());
            enemies[i].setDeathPointer(deathPointer.getOffset());
            enemies[i].setMaxCall(Integer.parseInt(maxCall.getText()));

            //gfx/sound tab
            if (outsidePic.getSelectedIndex() != -1)
                enemies[i].setOutsidePic(outsidePic.getSelectedIndex());
            enemies[i].setMovement(Integer.parseInt(movement.getText()));
            enemies[i].setRow(row.getSelectedIndex());
            if (insidePic.getSelectedIndex() != -1)
                enemies[i].setInsidePic(insidePic.getSelectedIndex());
            enemies[i].setPalette(Integer.parseInt(palette.getText()));
            enemies[i].setDieSound(deathSound.getSelectedIndex());
            enemies[i].setMusic(music.getSelectedIndex());

            //weaknesses tab
            for (int j = 0; j < weakness.length; j++)
                enemies[i].setWeakness(j, weakness[j].getSelectedIndex());
            enemies[i].setMirrorPercent(Integer.parseInt(mirrorRate.getText()));

            //unknowns tab
            //enemies[i].setUnknowna(Integer.parseInt(unknowna.getText()));
            //enemies[i].setUnknownb(Integer.parseInt(unknownb.getText()));
            //enemies[i].setUnknownc(Integer.parseInt(unknownc.getText()));
            //enemies[i].setUnknownd(Integer.parseInt(unknownd.getText()));
            //enemies[i].setUnknowne(Integer.parseInt(unknowne.getText()));
            //enemies[i].setUnknownf(Integer.parseInt(unknownf.getText()));
            //enemies[i].setUnknowng(Integer.parseInt(unknowng.getText()));
            enemies[i].setUnknownh(Integer.parseInt(unknownh.getText()));
            //enemies[i].setRow(Integer.parseInt(unknowni.getText()));
            //enemies[i].setMirrorPercent(Integer.parseInt(unknownj.getText()));
            enemies[i].setUnknownk(Integer.parseInt(unknownk.getText()));
            enemies[i].setUnknownl(Integer.parseInt(unknownl.getText()));
        }
        catch (NumberFormatException nfe)
        {
            JOptionPane
                .showMessageDialog(
                    mainWindow,
                    "One of the text fields was detected as having\n "
                        + "an invalid number format. This was most likely caused by\n"
                        + "a text field left blank. Please correct the problem and\n"
                        + "click the \"Apply Changes\" button again. No data has been\n"
                        + "written to the ROM.", "Invalid Number Format",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        //write to ROM
        enemies[i].writeInfo();
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().startsWith("enemyActionChanged"))
        {
            updateArguements(Integer.parseInt(ae.getActionCommand().substring(
                18)));
        }
        else if (ae.getActionCommand().equals(selector.getActionCommand()))
        {
            int i = getNumberOfString(selector.getSelectedItem().toString());
            showInfo(i);
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            int i = getNumberOfString(selector.getSelectedItem().toString());
            if (i == 0)
                JOptionPane.showMessageDialog(mainWindow,
                    "Writing to enemy #0 is not allowed.", "Write Failed",
                    JOptionPane.WARNING_MESSAGE);
            else
                saveInfo(i);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("Find"))
        {
            search(search.getText().toLowerCase(), selector);
        }
        else if (ae.getActionCommand().equals("ordGame"))
        {
            useGameOrder = true;
            notifyEnemyDataListeners(new ListDataEvent(this,
                ListDataEvent.CONTENTS_CHANGED, 0, NUM_ENEMIES));
        }
        else if (ae.getActionCommand().equals("ordReg"))
        {
            useGameOrder = false;
            notifyEnemyDataListeners(new ListDataEvent(this,
                ListDataEvent.CONTENTS_CHANGED, 0, NUM_ENEMIES));
        }
        else
        {
            System.err.println("Uncaught action command in eb.EnemyEditor: "
                + ae.getActionCommand());
        }
    }

    private void loadGameOrder()
    {
        String[] ord = readArray("enemyGameOrder.txt", false, NUM_ENEMIES);
        for (int i = 0; i < ord.length; i++)
        {
            try
            {
                gameOrder[i] = (short) (Integer.parseInt(ord[i]) + 1);
            }
            catch (NumberFormatException e)
            {
                gameOrder[i] = 0;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        //load game order
        loadGameOrder();

        //outsidePic = createJComboBoxFromArray(oobSpriteNames);
        selector.setSelectedIndex(selector.getSelectedIndex());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show(java.lang.Object)
     */
    public void show(Object in) throws IllegalArgumentException
    {
        super.show(in);

        if (in instanceof Integer)
            selector.setSelectedIndex(((Integer) in).intValue());
        else
            HackModule.search(in.toString(), selector, true);
    }

    public static SimpleComboBoxModel createEnemyComboBoxModel()
    {
        SimpleComboBoxModel out = new SimpleComboBoxModel()
        {
            public int getSize()
            {
                return enemies.length;
            }

            public Object getElementAt(int i)
            {
                String out;
                int j = useGameOrder ? gameOrder[i] : i;
                try
                {
                    out = enemies[j].toString();
                }
                catch (NullPointerException e)
                {
                    out = "Null";
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    return getElementAt(0);
                }
                return HackModule.getNumberedString(out, j);
            }
        };
        addEnemyDataListener(out);
        //out.setOffset(zeroBased ? 0 : 1);

        return out;
    }
    private static ArrayList enemyListeners = new ArrayList();

    protected static void addEnemyDataListener(ListDataListener ldl)
    {
        enemyListeners.add(ldl);
    }

    protected static void removeEnemyDataListener(ListDataListener ldl)
    {
        enemyListeners.remove(ldl);
    }

    protected static void notifyEnemyDataListeners(ListDataEvent lde)
    {
        for (Iterator i = enemyListeners.iterator(); i.hasNext();)
        {
            ((ListDataListener) i.next()).contentsChanged(lde);
        }
    }

    public static JComboBox createEnemyComboBox(final ActionListener al)
    {
        SimpleComboBoxModel model = createEnemyComboBoxModel();
        final JComboBox out = new JComboBox(model);
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
     * Represents an entry in the Earthbound enemies table. Information about
     * vars is in the "get" methods for those vars.
     */
    public static class Enemy
    {
        private int num, address;
        private int theFlag;
        private String name;
        //see http://pkhack.starmen.net/old/misc/unknown5.txt
        private int gender;
        private int type; // 0 - normal, 1 - insect, 2 - metal
        private int insidePic;
        private int outsidePic;
        private int runFlag;
        private int hp;
        private int pp;
        private int exp;
        private int money;
        private int movement; //assume two-byte, but not sure
        private int startPointer;
        private int deathPointer;
        private int palette;
        private int level;
        private int music;
        private int offense;
        private int unknownk;
        private int defense;
        private int unknownl;
        //see http://pkhack.starmen.net/old/misc/unknown4.txt
        private int speed;
        private int guts;
        private int iq;
        private int weakness[] = new int[5];
        /*
         * private int unknownb; private int unknownc; private int unknownd;
         * private int unknowne;
         */
        private int missRate;
        private int order;
        private int[] action = new int[5]; //last one is final action
        //private int finalAction;
        private int[] arguement = new int[5]; //last one is final action
        //		private int unknowng;
        private int unknownh;
        //see http://pkhack.starmen.net/old/misc/unknown3.txt
        private int bossFlag;
        private int freq;
        private int item;
        private int status;
        private int dieSound;
        private int row;
        //see http://pkhack.starmen.net/old/misc/unknown2.txt
        private int maxCall; // <= 8
        private int mirrorPercent;
        //see http://pkhack.starmen.net/old/misc/unknown1.txt
        private HackModule hm;

        /**
         * Creates an <code>Enemy</code> object by reading from the specified
         * entry number.
         * 
         * @param num Entry number
         */
        public Enemy(int num, HackModule hm)
        {
            this.hm = hm;
            this.num = num;
            this.address = /* 0x1597e7 */0x159789 + (num * 94);

            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            this.theFlag = rom.readSeek();
            this.name = hm.readSeekRegString(25);

            this.gender = rom.readSeek();
            this.type = rom.readSeek();

            this.insidePic = rom.readMultiSeek(2);

            this.outsidePic = rom.readMultiSeek(2);

            this.runFlag = rom.readSeek();

            this.hp = rom.readMultiSeek(2);

            this.pp = rom.readMultiSeek(2);

            this.exp = rom.readMultiSeek(4);

            this.money = rom.readMultiSeek(2);

            this.movement = rom.readMultiSeek(2);

            this.startPointer = rom.readMultiSeek(4);

            this.deathPointer = rom.readMultiSeek(4);

            this.palette = rom.readSeek();

            this.level = rom.readSeek();

            this.music = rom.readSeek();

            this.offense = rom.readSeek();

            this.unknownk += rom.readSeek();

            this.defense = rom.readSeek();

            this.unknownl += rom.readSeek();

            this.speed = rom.readSeek();

            this.guts = rom.readSeek();

            this.iq = rom.readSeek();

            this.weakness = rom.readSeek(5);

            this.missRate = rom.readSeek();

            this.order = rom.readSeek();

            rom.readMultiSeek(action, 2);

            rom.readSeek(this.arguement);

            this.unknownh = rom.readSeek();

            this.bossFlag = rom.readSeek();

            this.freq = rom.readSeek();
            this.item = rom.readSeek();

            this.status = rom.readSeek();
            this.dieSound = rom.readSeek();

            this.row = rom.readSeek();

            this.maxCall = rom.readSeek();

            this.mirrorPercent = rom.readSeek();

            //			if (this.runFlag != 7)
            //			{
            //				System.out.println(this.getName() + ": " + this.getRunFlag());
            //			}
        }

        /**
         * Writes the information stored in this into the ROM. Will not write to
         * enemy #0.
         */
        public void writeInfo()
        {
            if (num == 0)
                return;

            AbstractRom rom = hm.rom;

            rom.seek(this.address);

            rom.writeSeek(this.theFlag);
            hm.writeSeekRegString(25, name);

            rom.writeSeek(this.gender);
            rom.writeSeek(this.type);

            rom.writeSeek(this.insidePic, 2);

            rom.writeSeek(this.outsidePic, 2);

            rom.writeSeek(this.runFlag);

            rom.writeSeek(this.hp, 2);

            rom.writeSeek(this.pp, 2);

            rom.writeSeek(this.exp, 4);

            rom.writeSeek(this.money, 2);

            rom.writeSeek(this.movement, 2);

            rom.writeSeek(this.startPointer, 4);

            rom.writeSeek(this.deathPointer, 4);

            rom.writeSeek(this.palette);

            rom.writeSeek(this.level);

            rom.writeSeek(this.music);

            rom.writeSeek(this.offense);

            rom.writeSeek(this.unknownk);

            rom.writeSeek(this.defense);

            rom.writeSeek(this.unknownl);

            rom.writeSeek(this.speed);

            rom.writeSeek(this.guts);

            rom.writeSeek(this.iq);

            rom.writeSeek(weakness);

            rom.writeSeek(this.missRate);

            rom.writeSeek(this.order);

            rom.writeSeek(action, action.length, 2);

            rom.writeSeek(this.arguement);

            rom.writeSeek(this.unknownh);

            rom.writeSeek(this.bossFlag);

            rom.writeSeek(this.freq);
            rom.writeSeek(this.item);

            rom.writeSeek(this.status);
            rom.writeSeek(this.dieSound);

            rom.writeSeek(this.row);

            rom.writeSeek(this.maxCall);

            rom.writeSeek(this.mirrorPercent);
        }

        /**
         * Returns a numbered <code>String</code> with the enemy's name
         * 
         * @return String
         */
        public String toString()
        {
            return this.getName();
        }

        /**
         * Returns action #i. Values can be 0-4. #4 is the final action.
         * 
         * @param i Action number to get.
         * @return int
         * @see EbHackModule#effects
         */
        public int getAction(int i)
        {
            return action[i];
        }

        /**
         * Returns arguement #i. Values can be 0-4. #4 is the final action
         * arguement.
         * 
         * @param i Action number to get arguement for.
         * @return int
         * @see EbHackModule#actionType
         * @see EbHackModule#psiNames
         * @see EbHackModule#effects
         */
        public int getArguement(int i)
        {
            return arguement[i];
        }

        /**
         * Returns the bossFlag.
         * 
         * @return int
         */
        public int getBossFlag()
        {
            return bossFlag;
        }

        /**
         * Returns the deathPointer as a SNES pointer.
         * 
         * @return int
         */
        public int getDeathPointer()
        {
            return deathPointer;
        }

        /**
         * Returns the defense.
         * 
         * @return int
         */
        public int getDefense()
        {
            return defense;
        }

        /**
         * Returns the dieSound.
         * 
         * @return int
         * @see EbHackModule#soundEffects
         */
        public int getDieSound()
        {
            return dieSound;
        }

        /**
         * Returns the exp.
         * 
         * @return int
         */
        public int getExp()
        {
            return exp;
        }

        /**
         * Returns the final action.
         * 
         * @return int
         * @see EbHackModule#effects
         */
        public int getFinalAction()
        {
            return action[4];
        }

        /**
         * Returns the freq of getting the item (0-7). Chance of getting the
         * item is (2^freq)/128.
         * 
         * @return int
         */
        public int getFreq()
        {
            return freq;
        }

        /**
         * Returns the guts.
         * 
         * @return int
         */
        public int getGuts()
        {
            return guts;
        }

        /**
         * Returns the hp.
         * 
         * @return int
         */
        public int getHp()
        {
            return hp;
        }

        /**
         * Returns the insidePic.
         * 
         * @return int
         * @see EbHackModule#battleSpriteNames
         */
        public int getInsidePic()
        {
            return insidePic;
        }

        /**
         * Returns the iq.
         * 
         * @return int
         */
        public int getIq()
        {
            return iq;
        }

        /**
         * Returns the item.
         * 
         * @return int
         */
        public int getItem()
        {
            return item;
        }

        /**
         * Returns the level.
         * 
         * @return int
         */
        public int getLevel()
        {
            return level;
        }

        /**
         * Returns the maxCall.
         * 
         * @return int
         */
        public int getMaxCall()
        {
            return maxCall;
        }

        /**
         * Returns the missRate (0-255). Zero = never miss. 255 = always miss.
         * This is when they attack you. Anyway, I'm not completely sure how
         * this figures in with your speed setting or whatever. Perhaps there
         * are supposed to be enemies that always hit you, no matter what your
         * speed or something.
         * 
         * @return int
         */
        public int getMissRate()
        {
            return missRate;
        }

        /**
         * Returns the money.
         * 
         * @return int
         */
        public int getMoney()
        {
            return money;
        }

        /**
         * Returns the movement.
         * 
         * @return int
         */
        public int getMovement()
        {
            return movement;
        }

        /**
         * Returns the music.
         * 
         * @return int
         * @see EbHackModule#musicNames
         */
        public int getMusic()
        {
            return music;
        }

        /**
         * Returns the name.
         * 
         * @return String
         */
        public String getName()
        {
            return name;
        }

        /**
         * Returns the offense.
         * 
         * @return int
         */
        public int getOffense()
        {
            return offense;
        }

        /**
         * Returns the order of actions. <br>
         * <br>
         * 0 - Random <br>
         * 1 - Random, but tends to do the 3rd attack often <br>
         * 2 - In order (Attack 1, Attack 2, Attack 3, Attack 4, repeat...) <br>
         * 3 - "Staggered Order" (difficult to explain, just it goes something
         * like "Attack 3, 4, 3, 4, 3, 4, 3, 1, 2, 1, 2, etc. It's not a very
         * logical pattern, but it's a pattern. This is what Buzz Buzz does when
         * he's in your party, and what other enemies do to. Just call it
         * "Staggered Order" in the GUI, cuz it's too hard to figure out a
         * better name for it.
         * 
         * @return int
         */
        public int getOrder()
        {
            return order;
        }

        /**
         * Returns the SPT entry number <em>plus 1</em>. One is added because
         * zero is invisible (no sprite).
         * 
         * @return int
         */
        public int getOutsidePic()
        {
            return outsidePic;
        }

        /**
         * Returns the palette of the battle sprite (0-31).
         * 
         * @return int
         */
        public int getPalette()
        {
            return palette;
        }

        /**
         * Returns the pp.
         * 
         * @return int
         */
        public int getPp()
        {
            return pp;
        }

        /**
         * Returns the runFlag.
         * 
         * @return int
         */
        public int getRunFlag()
        {
            return runFlag;
        }

        /**
         * Returns the speed.
         * 
         * @return int
         */
        public int getSpeed()
        {
            return speed;
        }

        /**
         * Returns the startPointer as a SNES pointer.
         * 
         * @return int
         */
        public int getStartPointer()
        {
            return startPointer;
        }

        /**
         * Returns the status.
         * 
         * @return int
         */
        public int getStatus()
        {
            return status;
        }

        /**
         * Returns the type (0-3). <br>
         * <br>
         * 0 = normal <br>
         * 1 = insect <br>
         * 2 = metal
         * 
         * @return int
         */
        public int getType()
        {
            return type;
        }

        /**
         * Returns the gender.
         * 
         * @return int
         * @deprecated
         * @see #getGender()
         */
        public int getUnknowna()
        {
            return gender;
        }

        /**
         * Returns the PSI Fire weakness.
         * 
         * @return int
         */
        public int getUnknownb()
        {
            return weakness[0];
        }

        /**
         * Returns the PSI Freeze weakness.
         * 
         * @return int
         */
        public int getUnknownc()
        {
            return weakness[1];
        }

        /**
         * Returns the PSI Flash weakness.
         * 
         * @return int
         */
        public int getUnknownd()
        {
            return weakness[2];
        }

        /**
         * Returns the paralysis weakness.
         * 
         * @return int
         */
        public int getUnknowne()
        {
            return weakness[3];
        }

        /**
         * Returns the hynosis/brainshock weakness.
         * 
         * @return int
         */
        public int getUnknownf()
        {
            return weakness[4];
        }

        /**
         * Returns the final action arugment.
         * 
         * @return int
         * @deprecated
         * @see #getArguement(int)
         */
        public int getUnknowng()
        {
            return arguement[4];
        }

        /**
         * Returns the unknownh.
         * 
         * @return int
         */
        public int getUnknownh()
        {
            return unknownh;
        }

        /**
         * Returns the row.
         * 
         * @return int
         */
        public int getRow()
        {
            return row;
        }

        /**
         * Returns the row.
         * 
         * @return int
         * @deprecated
         * @see #getRow()
         */
        public int getUnknowni()
        {
            return row;
        }

        /**
         * Returns the percent chance of Poo's mirror success.
         * 
         * @return int
         * @deprecated
         * @see #getMirrorPercent()
         */
        public int getUnknownj()
        {
            return mirrorPercent;
        }

        /**
         * Returns the percent chance of Poo's mirror success.
         * 
         * @return int
         */
        public int getMirrorPercent()
        {
            return mirrorPercent;
        }

        /**
         * Sets the <code>i</code> th action.
         * 
         * @param i Which action to set. Values can be 0-4. #4 is the final
         *            action.
         * @param action The action to set
         * @see #getAction(int)
         */
        public void setAction(int i, int action)
        {
            this.action[i] = action;
        }

        /**
         * Sets the arguement for the <code>i</code> th action.
         * 
         * @param i Number of the arguement to set. Values can be 0-4. #4 is the
         *            final action arguement.
         * @param arguement The arguement to set
         * @see #getArguement(int)
         */
        public void setArguement(int i, int arguement)
        {
            this.arguement[i] = arguement;
        }

        /**
         * Sets the bossFlag.
         * 
         * @param bossFlag The bossFlag to set
         * @see #getBossFlag()
         */
        public void setBossFlag(int bossFlag)
        {
            this.bossFlag = bossFlag;
        }

        /**
         * Sets the deathPointer.
         * 
         * @param deathPointer The deathPointer to set
         * @see #getDeathPointer()
         */
        public void setDeathPointer(int deathPointer)
        {
            this.deathPointer = deathPointer;
        }

        /**
         * Sets the defense.
         * 
         * @param defense The defense to set
         * @see #getDefense()
         */
        public void setDefense(int defense)
        {
            this.defense = defense;
        }

        /**
         * Sets the dieSound.
         * 
         * @param dieSound The dieSound to set
         * @see #getDieSound()
         */
        public void setDieSound(int dieSound)
        {
            this.dieSound = dieSound;
        }

        /**
         * Sets the exp.
         * 
         * @param exp The exp to set
         * @see #getExp()
         */
        public void setExp(int exp)
        {
            this.exp = exp;
        }

        /**
         * Sets the final action.
         * 
         * @param finalAction The final action to set
         * @see #getFinalAction()
         */
        public void setFinalAction(int finalAction)
        {
            this.action[4] = finalAction;
        }

        /**
         * Sets the freq.
         * 
         * @param freq The freq to set
         * @see #getFreq()
         */
        public void setFreq(int freq)
        {
            this.freq = freq;
        }

        /**
         * Sets the guts.
         * 
         * @param guts The guts to set
         * @see #getGuts()
         */
        public void setGuts(int guts)
        {
            this.guts = guts;
        }

        /**
         * Sets the hp.
         * 
         * @param hp The hp to set
         * @see #getHp()
         */
        public void setHp(int hp)
        {
            this.hp = hp;
        }

        /**
         * Sets the insidePic.
         * 
         * @param insidePic The insidePic to set
         * @see #getInsidePic()
         */
        public void setInsidePic(int insidePic)
        {
            this.insidePic = insidePic;
        }

        /**
         * Sets the iq.
         * 
         * @param iq The iq to set
         * @see #getIq()
         */
        public void setIq(int iq)
        {
            this.iq = iq;
        }

        /**
         * Sets the item.
         * 
         * @param item The item to set
         * @see #getItem()
         */
        public void setItem(int item)
        {
            this.item = item;
        }

        /**
         * Sets the level.
         * 
         * @param level The level to set
         * @see #getLevel()
         */
        public void setLevel(int level)
        {
            this.level = level;
        }

        /**
         * Sets the maxCall.
         * 
         * @param maxCall The maxCall to set
         * @see #getMaxCall()
         */
        public void setMaxCall(int maxCall)
        {
            this.maxCall = maxCall;
        }

        /**
         * Sets the missRate.
         * 
         * @param missRate The missRate to set
         * @see #getMissRate()
         */
        public void setMissRate(int missRate)
        {
            this.missRate = missRate;
        }

        /**
         * Sets the money.
         * 
         * @param money The money to set
         * @see #getMoney()
         */
        public void setMoney(int money)
        {
            this.money = money;
        }

        /**
         * Sets the movement.
         * 
         * @param movement The movement to set
         * @see #getMovement()
         */
        public void setMovement(int movement)
        {
            this.movement = movement;
        }

        /**
         * Sets the music.
         * 
         * @param music The music to set
         * @see #getMusic()
         */
        public void setMusic(int music)
        {
            this.music = music;
        }

        /**
         * Sets the name.
         * 
         * @param name The name to set
         * @see #getName()
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * Sets the offense.
         * 
         * @param offense The offense to set
         * @see #getOffense()
         */
        public void setOffense(int offense)
        {
            this.offense = offense;
        }

        /**
         * Sets the order.
         * 
         * @param order The order to set
         * @see #getOrder()
         */
        public void setOrder(int order)
        {
            this.order = order;
        }

        /**
         * Sets the outsidePic.
         * 
         * @param outsidePic The outsidePic to set
         * @see #getOutsidePic()
         */
        public void setOutsidePic(int outsidePic)
        {
            this.outsidePic = outsidePic;
        }

        /**
         * Sets the palette.
         * 
         * @param palette The palette to set
         * @see #getPalette()
         */
        public void setPalette(int palette)
        {
            this.palette = palette;
        }

        /**
         * Sets the pp.
         * 
         * @param pp The pp to set
         * @see #getPp()
         */
        public void setPp(int pp)
        {
            this.pp = pp;
        }

        /**
         * Sets the runFlag.
         * 
         * @param runFlag The runFlag to set
         * @see #getRunFlag()
         */
        public void setRunFlag(int runFlag)
        {
            this.runFlag = runFlag;
        }

        /**
         * Sets the speed.
         * 
         * @param speed The speed to set
         * @see #getSpeed()
         */
        public void setSpeed(int speed)
        {
            this.speed = speed;
        }

        /**
         * Sets the startPointer.
         * 
         * @param startPointer The startPointer to set
         * @see #getStartPointer()
         */
        public void setStartPointer(int startPointer)
        {
            this.startPointer = startPointer;
        }

        /**
         * Sets the status.
         * 
         * @param status The status to set
         * @see #getStatus()
         */
        public void setStatus(int status)
        {
            this.status = status;
        }

        /**
         * Sets the type.
         * 
         * @param type The type to set
         * @see #getType()
         */
        public void setType(int type)
        {
            this.type = type;
        }

        /**
         * Sets the gender.
         * 
         * @param gender The gender to set
         * @deprecated
         * @see #setGender(int)
         */
        public void setUnknowna(int gender)
        {
            this.gender = gender;
        }

        /**
         * Sets the PSI Fire weakness.
         * 
         * @param unknownb The unknownb to set
         */
        public void setUnknownb(int unknownb)
        {
            this.weakness[0] = unknownb;
        }

        /**
         * Sets the PSI Freeze weakness.
         * 
         * @param unknownc The unknownc to set
         */
        public void setUnknownc(int unknownc)
        {
            this.weakness[1] = unknownc;
        }

        /**
         * Sets the PSI Flash weakness.
         * 
         * @param unknownd The unknownd to set
         */
        public void setUnknownd(int unknownd)
        {
            this.weakness[2] = unknownd;
        }

        /**
         * Sets the paralysis weakness.
         * 
         * @param unknowne The unknowne to set
         */
        public void setUnknowne(int unknowne)
        {
            this.weakness[3] = unknowne;
        }

        /**
         * Sets the hypnosis/brainshock weakness.
         * 
         * @param unknownf The unknownf to set
         */
        public void setUnknownf(int unknownf)
        {
            this.weakness[4] = unknownf;
        }

        /**
         * Sets the final action arugment.
         * 
         * @param unknowng The unknowng to set
         * @deprecated
         * @see #setArguement(int, int)
         */
        public void setUnknowng(int unknowng)
        {
            this.arguement[4] = unknowng;
        }

        /**
         * Sets the unknownh.
         * 
         * @param unknownh The unknownh to set
         */
        public void setUnknownh(int unknownh)
        {
            this.unknownh = unknownh;
        }

        /**
         * Sets the row. Row 0 is the front. Row 1 is the back.
         * 
         * @param row The row to set
         */
        public void setRow(int row)
        {
            this.row = row;
        }

        /**
         * Sets the percentage chance of Poo's mirror being successful.
         * 
         * @param unknownj The mirrorPercent to set
         * @deprecated
         * @see #setMirrorPercent(int)
         */
        public void setUnknownj(int unknownj)
        {
            this.mirrorPercent = unknownj;
        }

        /**
         * Sets the percentage chance of Poo's mirror being successful.
         * 
         * @param mirrorPercent The mirrorPercent to set
         */
        public void setMirrorPercent(int mirrorPercent)
        {
            this.mirrorPercent = mirrorPercent;
        }

        /**
         * Returns the theFlag. If true "the" is put before the enemies name.
         * 
         * @return boolean
         */
        public boolean getTheFlag()
        {
            return theFlag == 1;
        }

        /**
         * Sets the theFlag.
         * 
         * @param theFlag The theFlag to set
         * @see #getTheFlag()
         */
        public void setTheFlag(boolean theFlag)
        {
            this.theFlag = (theFlag ? 1 : 0);
        }

        /**
         * Returns the unknownk.
         * 
         * @return int
         */
        public int getUnknownk()
        {
            return unknownk;
        }

        /**
         * Returns the unknownl.
         * 
         * @return int
         */
        public int getUnknownl()
        {
            return unknownl;
        }

        /**
         * Sets the unknownk.
         * 
         * @param unknownk The unknownk to set
         */
        public void setUnknownk(int unknownk)
        {
            this.unknownk = unknownk;
        }

        /**
         * Sets the unknownl.
         * 
         * @param unknownl The unknownl to set
         */
        public void setUnknownl(int unknownl)
        {
            this.unknownl = unknownl;
        }

        /**
         * Sets the row.
         * 
         * @param row The row to set
         * @deprecated
         * @see #setRow(int)
         */
        public void setUnknowni(int row)
        {
            this.row = row;
        }

        /**
         * Returns the gender.
         * 
         * @return int 1 is male, 2 is female, and 3 is neutral.
         */
        public int getGender()
        {
            return gender;
        }

        /**
         * Sets the gender.
         * 
         * @param gender 1 is male, 2 is female, and 3 is neutral.
         */
        public void setGender(int gender)
        {
            this.gender = gender;
        }

        /**
         * Returns the address of the entry.
         * 
         * @return int
         */
        public int getAddress()
        {
            return address;
        }

        /**
         * Returns the <code>i</code> th weakness value. Values of i: [0] =
         * PSI Fire, [1] = PSI Freeze, [2] = PSI Flash, [3] = Paralysis, [4] =
         * Hypnosis/Brainshock.
         * 
         * @param i Which weakness value.
         * @return weakness amount from 0 (regular) to 3 (weakest)
         */
        public int getWeakness(int i)
        {
            return weakness[i];
        }

        /**
         * Sets the <code>i</code> th weakness value. Values of i: [0] = PSI
         * Fire, [1] = PSI Freeze, [2] = PSI Flash, [3] = Paralysis, [4] =
         * Hypnosis/Brainshock.
         * 
         * @param i Which weakness value.
         * @param weakness weakness amount from 0 (regular) to 3 (weakest)
         */
        public void setWeakness(int i, int weakness)
        {
            this.weakness[i] = weakness;
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#getIcon()
     */
    public Icon getIcon()
    {
        return fobbyIcon;
    }

    private static Icon initIcon()
    {
        BufferedImage out = new BufferedImage(16, 16,
            BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = out.getGraphics();

        //Created by ImageFileToCode from net/starmen/pkhack/fobby.gif
        g.setColor(new Color(255, 255, 255));
        g.setColor(new Color(72, 18, 34));
        g.drawLine(5, 2, 5, 2);
        g.setColor(new Color(91, 12, 34));
        g.drawLine(6, 2, 6, 2);
        g.setColor(new Color(90, 11, 33));
        g.drawLine(7, 2, 7, 2);
        g.setColor(new Color(90, 10, 35));
        g.drawLine(8, 2, 8, 2);
        g.drawLine(9, 2, 9, 2);
        g.setColor(new Color(41, 35, 35));
        g.drawLine(3, 3, 3, 3);
        g.setColor(new Color(59, 26, 35));
        g.drawLine(4, 3, 4, 3);
        g.setColor(new Color(85, 31, 47));
        g.drawLine(5, 3, 5, 3);
        g.setColor(new Color(102, 23, 45));
        g.drawLine(6, 3, 6, 3);
        g.drawLine(7, 3, 7, 3);
        g.setColor(new Color(102, 22, 47));
        g.drawLine(8, 3, 8, 3);
        g.drawLine(9, 3, 9, 3);
        g.setColor(new Color(45, 33, 35));
        g.drawLine(10, 3, 10, 3);
        g.setColor(new Color(40, 36, 35));
        g.drawLine(11, 3, 11, 3);
        g.setColor(new Color(90, 12, 28));
        g.drawLine(2, 4, 2, 4);
        g.setColor(new Color(136, 6, 40));
        g.drawLine(3, 4, 3, 4);
        g.setColor(new Color(163, 0, 45));
        g.drawLine(4, 4, 4, 4);
        g.setColor(new Color(255, 95, 161));
        g.drawLine(5, 4, 5, 4);
        g.setColor(new Color(255, 94, 161));
        g.drawLine(6, 4, 6, 4);
        g.setColor(new Color(225, 8, 86));
        g.drawLine(7, 4, 7, 4);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(8, 4, 8, 4);
        g.drawLine(9, 4, 9, 4);
        g.setColor(new Color(181, 0, 53));
        g.drawLine(10, 4, 10, 4);
        g.setColor(new Color(140, 2, 51));
        g.drawLine(11, 4, 11, 4);
        g.setColor(new Color(80, 15, 35));
        g.drawLine(12, 4, 12, 4);
        g.setColor(new Color(41, 36, 33));
        g.drawLine(1, 5, 1, 5);
        g.setColor(new Color(102, 24, 40));
        g.drawLine(2, 5, 2, 5);
        g.setColor(new Color(248, 118, 152));
        g.drawLine(3, 5, 3, 5);
        g.setColor(new Color(196, 25, 78));
        g.drawLine(4, 5, 4, 5);
        g.setColor(new Color(212, 16, 82));
        g.drawLine(5, 5, 5, 5);
        g.setColor(new Color(214, 15, 82));
        g.drawLine(6, 5, 6, 5);
        g.setColor(new Color(225, 8, 86));
        g.drawLine(7, 5, 7, 5);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(8, 5, 8, 5);
        g.setColor(new Color(240, 1, 95));
        g.drawLine(9, 5, 9, 5);
        g.setColor(new Color(215, 13, 87));
        g.drawLine(10, 5, 10, 5);
        g.setColor(new Color(140, 2, 51));
        g.drawLine(11, 5, 11, 5);
        g.setColor(new Color(92, 27, 47));
        g.drawLine(12, 5, 12, 5);
        g.setColor(new Color(52, 29, 35));
        g.drawLine(13, 5, 13, 5);
        g.setColor(new Color(81, 13, 34));
        g.drawLine(1, 6, 1, 6);
        g.setColor(new Color(191, 0, 58));
        g.drawLine(2, 6, 2, 6);
        g.setColor(new Color(224, 8, 91));
        g.drawLine(3, 6, 3, 6);
        g.setColor(new Color(203, 19, 91));
        g.drawLine(4, 6, 4, 6);
        g.setColor(new Color(158, 0, 46));
        g.drawLine(5, 6, 5, 6);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(6, 6, 6, 6);
        g.drawLine(7, 6, 7, 6);
        g.setColor(new Color(203, 19, 91));
        g.drawLine(8, 6, 8, 6);
        g.setColor(new Color(158, 0, 46));
        g.drawLine(9, 6, 9, 6);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(10, 6, 10, 6);
        g.drawLine(11, 6, 11, 6);
        g.setColor(new Color(144, 1, 47));
        g.drawLine(12, 6, 12, 6);
        g.setColor(new Color(90, 10, 35));
        g.drawLine(13, 6, 13, 6);
        g.setColor(new Color(51, 30, 35));
        g.drawLine(0, 7, 0, 7);
        g.setColor(new Color(94, 26, 47));
        g.drawLine(1, 7, 1, 7);
        g.setColor(new Color(224, 8, 91));
        g.drawLine(2, 7, 2, 7);
        g.drawLine(3, 7, 3, 7);
        g.setColor(new Color(203, 19, 91));
        g.drawLine(4, 7, 4, 7);
        g.drawLine(5, 7, 5, 7);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(6, 7, 6, 7);
        g.drawLine(7, 7, 7, 7);
        g.setColor(new Color(203, 19, 91));
        g.drawLine(8, 7, 8, 7);
        g.drawLine(9, 7, 9, 7);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(10, 7, 10, 7);
        g.drawLine(11, 7, 11, 7);
        g.setColor(new Color(144, 1, 47));
        g.drawLine(12, 7, 12, 7);
        g.setColor(new Color(102, 22, 47));
        g.drawLine(13, 7, 13, 7);
        g.setColor(new Color(42, 33, 36));
        g.drawLine(14, 7, 14, 7);
        g.setColor(new Color(51, 30, 35));
        g.drawLine(0, 8, 0, 8);
        g.setColor(new Color(94, 26, 47));
        g.drawLine(1, 8, 1, 8);
        g.setColor(new Color(215, 13, 89));
        g.drawLine(2, 8, 2, 8);
        g.setColor(new Color(233, 5, 94));
        g.drawLine(3, 8, 3, 8);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(4, 8, 4, 8);
        g.drawLine(5, 8, 5, 8);
        g.drawLine(6, 8, 6, 8);
        g.drawLine(7, 8, 7, 8);
        g.drawLine(8, 8, 8, 8);
        g.drawLine(9, 8, 9, 8);
        g.setColor(new Color(230, 3, 92));
        g.drawLine(10, 8, 10, 8);
        g.setColor(new Color(217, 12, 89));
        g.drawLine(11, 8, 11, 8);
        g.setColor(new Color(139, 4, 47));
        g.drawLine(12, 8, 12, 8);
        g.setColor(new Color(105, 21, 47));
        g.drawLine(13, 8, 13, 8);
        g.setColor(new Color(42, 33, 36));
        g.drawLine(14, 8, 14, 8);
        g.setColor(new Color(82, 14, 35));
        g.drawLine(1, 9, 1, 9);
        g.setColor(new Color(182, 0, 56));
        g.drawLine(2, 9, 2, 9);
        g.setColor(new Color(232, 4, 93));
        g.drawLine(3, 9, 3, 9);
        g.setColor(new Color(239, 0, 94));
        g.drawLine(4, 9, 4, 9);
        g.drawLine(5, 9, 5, 9);
        g.drawLine(6, 9, 6, 9);
        g.drawLine(7, 9, 7, 9);
        g.drawLine(8, 9, 8, 9);
        g.setColor(new Color(238, 0, 93));
        g.drawLine(9, 9, 9, 9);
        g.setColor(new Color(231, 4, 93));
        g.drawLine(10, 9, 10, 9);
        g.setColor(new Color(184, 0, 56));
        g.drawLine(11, 9, 11, 9);
        g.setColor(new Color(139, 4, 47));
        g.drawLine(12, 9, 12, 9);
        g.setColor(new Color(92, 8, 34));
        g.drawLine(13, 9, 13, 9);
        g.setColor(new Color(89, 11, 35));
        g.drawLine(2, 10, 2, 10);
        g.setColor(new Color(135, 5, 51));
        g.drawLine(3, 10, 3, 10);
        g.setColor(new Color(151, 0, 51));
        g.drawLine(4, 10, 4, 10);
        g.setColor(new Color(201, 20, 87));
        g.drawLine(5, 10, 5, 10);
        g.setColor(new Color(209, 18, 88));
        g.drawLine(6, 10, 6, 10);
        g.setColor(new Color(208, 17, 87));
        g.drawLine(7, 10, 7, 10);
        g.setColor(new Color(208, 16, 89));
        g.drawLine(8, 10, 8, 10);
        g.drawLine(9, 10, 9, 10);
        g.setColor(new Color(142, 2, 47));
        g.drawLine(10, 10, 10, 10);
        g.setColor(new Color(104, 20, 46));
        g.drawLine(11, 10, 11, 10);
        g.setColor(new Color(41, 35, 35));
        g.drawLine(12, 10, 12, 10);
        g.setColor(new Color(40, 36, 35));
        g.drawLine(1, 11, 1, 11);
        g.setColor(new Color(134, 56, 80));
        g.drawLine(2, 11, 2, 11);
        g.setColor(new Color(123, 0, 39));
        g.drawLine(3, 11, 3, 11);
        g.setColor(new Color(151, 0, 51));
        g.drawLine(4, 11, 4, 11);
        g.setColor(new Color(168, 0, 54));
        g.drawLine(5, 11, 5, 11);
        g.setColor(new Color(175, 0, 54));
        g.drawLine(6, 11, 6, 11);
        g.drawLine(7, 11, 7, 11);
        g.setColor(new Color(175, 0, 56));
        g.drawLine(8, 11, 8, 11);
        g.drawLine(9, 11, 9, 11);
        g.setColor(new Color(142, 2, 47));
        g.drawLine(10, 11, 10, 11);
        g.setColor(new Color(93, 9, 35));
        g.drawLine(11, 11, 11, 11);
        g.setColor(new Color(41, 35, 35));
        g.drawLine(1, 12, 1, 12);
        g.setColor(new Color(166, 38, 86));
        g.drawLine(2, 12, 2, 12);
        g.drawLine(3, 12, 3, 12);
        g.setColor(new Color(43, 34, 35));
        g.drawLine(4, 12, 4, 12);
        g.drawLine(5, 12, 5, 12);
        g.setColor(new Color(102, 23, 45));
        g.drawLine(6, 12, 6, 12);
        g.drawLine(7, 12, 7, 12);
        g.setColor(new Color(77, 33, 46));
        g.drawLine(8, 12, 8, 12);
        g.setColor(new Color(66, 22, 35));
        g.drawLine(9, 12, 9, 12);
        g.setColor(new Color(85, 13, 35));
        g.drawLine(10, 12, 10, 12);
        g.drawLine(11, 12, 11, 12);
        g.setColor(new Color(45, 33, 35));
        g.drawLine(12, 12, 12, 12);
        g.setColor(new Color(121, 0, 41));
        g.drawLine(2, 13, 2, 13);
        g.setColor(new Color(120, 0, 40));
        g.drawLine(3, 13, 3, 13);
        g.setColor(new Color(90, 11, 33));
        g.drawLine(6, 13, 6, 13);
        g.drawLine(7, 13, 7, 13);
        g.setColor(new Color(66, 22, 35));
        g.drawLine(8, 13, 8, 13);
        g.setColor(new Color(85, 13, 35));
        g.drawLine(10, 13, 10, 13);
        g.setColor(new Color(130, 58, 80));
        g.drawLine(11, 13, 11, 13);
        g.setColor(new Color(45, 33, 35));
        g.drawLine(12, 13, 12, 13);
        g.setColor(new Color(40, 36, 35));
        g.drawLine(11, 14, 11, 14);

        return new ImageIcon(out);
    }
}