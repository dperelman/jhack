/*
 * Created on Aug 7, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;

import net.starmen.pkhack.CCInfo;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.Undoable;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.TPTEditor.TPTEntry;

/**
 * TODO Write [javadoc for] this class
 * 
 * @author AnyoneEB
 */
public class TextEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public TextEditor(Rom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static final int TPT = 0;
    public static final int RAW = 1;
    public static final int EXP = 2;
    public static final int NUM_TEXT_TYPES = 3;

    //	String class
    public static class StrInfo
    {
        public int address;
        public String str, header;
        /** Entry number in table. Negative means not in a table. */
        public int num = -1;
    }

    public static List textLists[] = new List[NUM_TEXT_TYPES];
    static
    {
        for (int i = 0; i < textLists.length; i++)
            textLists[i] = new ArrayList();
    }

    public static CCInfo cc;

    private class ActionCreator extends AbstractButton implements
        ListSelectionListener
    {
        private JList src;
        private String ac;

        public ActionCreator(JList src, ActionListener al, String ac)
        {
            this.src = src;
            this.ac = ac;
            this.addActionListener(al);
            src.addListSelectionListener(this);
        }

        public void valueChanged(ListSelectionEvent lse)
        {
            fireActionPerformed(new ActionEvent(src, 0, "textList" + ac
                + src.getSelectedIndex()));
        }
    }

    private static String[] tabShortNames = new String[]{"tpt", "raw", "exp"};

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());

        //menu
        JMenuBar mb = new JMenuBar();

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('e');

        editMenu.add(HackModule.createJMenuItem("Cut", 't', "ctrl X", "cut",
            this));
        editMenu.add(HackModule.createJMenuItem("Copy", 'c', "ctrl C", "copy",
            this));
        editMenu.add(HackModule.createJMenuItem("Paste", 'p', "ctrl V",
            "paste", this));
        editMenu.add(HackModule.createJMenuItem("Delete", 'd', "DELETE",
            "delete", this));
        editMenu.add(new JSeparator());
        editMenu.add(HackModule.createJMenuItem("Undo", 'u', "ctrl Z",
            "undo", this));
        editMenu.add(HackModule.createJMenuItem("Redo", 'r', "ctrl Y",
            "redo", this));
        editMenu.add(new JSeparator());
        editMenu.add(HackModule.createJMenuItem("Find", 'f', "ctrl F", "find",
            this));
        editMenu.add(HackModule.createJMenuItem("Find Next", 'n', "F3",
            "findNext", this));
        editMenu.add(new JSeparator());
        editMenu.add(HackModule.createJMenuItem("Goto", 'g', "ctrl G", "goto",
            this));

        mb.add(editMenu);

        JMenu optionMenu = new JMenu("Options");
        optionMenu.setMnemonic('o');

        optionMenu.add(HackModule.createJMenuItem("Show Code Help", 'h',
            "alt F1", "showCodeHelp", this));
        optionMenu.add(codesOnly = new PrefsCheckBox("Codes Only", JHack.main
            .getPrefs(), "eb_text_editor.codes_only", false, 'c', "alt C",
            "codesOnly", this));
        optionMenu.add(useComp = new PrefsCheckBox("Use Compression",
            JHack.main.getPrefs(), "eb_text_editor.use_compression", true, 'u',
            "alt U", "useComp", this));
        optionMenu
            .add(preventOverwrites = new PrefsCheckBox("Prevent Overwrites",
                JHack.main.getPrefs(), "eb_text_editor.prevent_overwrites",
                true, 'p', "alt P", null, null));

        mb.add(optionMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic('t');

        toolsMenu.add(showEntry = HackModule.createJMenuItem("Edit TPT Entry",
            'e', "ctrl e", "showEntry", this));

        showPreview = new JMenuItem("Show Text Preview");
        showPreview.setMnemonic('p');
        showPreview.setAccelerator(KeyStroke.getKeyStroke("ctrl P"));
        showPreview.setActionCommand("showPreview");
        showPreview.addActionListener(this);
        //        toolsMenu.add(showPreview);

        mb.add(toolsMenu);

        mainWindow.setJMenuBar(mb);
        //end menu

        Box entry = new Box(BoxLayout.Y_AXIS);

        String[] tabNames = new String[]{"Text Pointer Table", "Raw Text",
            "Expanded Area"};
        final List[] tabContents = textLists;
        textJLists = new JList[tabNames.length];
        selectorArea = new JTabbedPane();
        for (int i = 0; i < tabNames.length; i++)
        {
            JPanel contents = new JPanel();

            final int j = i;
            JList textList = new JList(new ListModel()
            {

                public int getSize()
                {
                    return tabContents[j].size();
                }

                public Object getElementAt(int a)
                {
                    //                    if(a < 10)
                    //                    System.out.println(
                    //                        "("
                    //                            + new Date().toGMTString()
                    //                            + ") Getting element below 10 at "
                    //                            + a
                    //                            + " on "
                    //                            + tabShortNames[j]
                    //                            + " JList.");
                    //                    return cc.parseHeader(
                    //                        ((StrInfo) tabContents[j].get(a)).str);
                    return ((StrInfo) tabContents[j].get(a)).header;
                }

                List listeners = new ArrayList();

                public void addListDataListener(ListDataListener ldl)
                {
                    listeners.add(ldl);
                }

                public void removeListDataListener(ListDataListener ldl)
                {
                    listeners.remove(ldl);
                }
            });
            textList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            textList.addListSelectionListener(new ActionCreator(textList, this,
                tabShortNames[i]));
            textList.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent me)
                {
                    if (me.getButton() == 1)
                    {
                        if (textJLists[j].getSelectedIndex() > -1)
                            showInfo(j, textJLists[j].getSelectedIndex());
                    }
                    else if (me.getButton() == 3)
                    {
                        showEntry();
                    }
                }

                public void mouseEntered(MouseEvent me)
                {}

                public void mouseExited(MouseEvent me)
                {}

                public void mousePressed(MouseEvent me)
                {}

                public void mouseReleased(MouseEvent me)
                {}
            });

            JScrollPane scroll = new JScrollPane(textList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(550, 270));

            contents.add(scroll);
            selectorArea.addTab(tabNames[i], contents);

            textJLists[i] = textList;
        }
        entry.add(selectorArea);
        entry
            .add(HackModule.createFlowLayout(new Component[]{
                positionLabel = new JLabel("Line:  /    "),
                Box.createHorizontalStrut(10),
                addressLabel = new JLabel("Address: 0x       | $      "),
                Box.createHorizontalStrut(10),
                sizeLabel = new JLabel("Size:   ")}));

        Box bottomLabels = new Box(BoxLayout.X_AXIS);
        bottomLabels.add(currSizeLabel = new JLabel("Current Size: "));
        bottomLabels.add(Box.createHorizontalStrut(10));
        bottomLabels.add(currPosLabel = new JLabel("Current Position: "));
        bottomLabels.add(Box.createHorizontalGlue());

        entry.add(HackModule.pairComponents(new JScrollPane(
            ta = new JTextArea(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), bottomLabels, false));
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setColumns(30);
        ta.setRows(7);
        ta.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent de)
            {
                updateCurrSize();
                updateCurrPos();
            }

            public void insertUpdate(DocumentEvent de)
            {
                updateCurrSize();
                updateCurrPos();
            }

            public void removeUpdate(DocumentEvent de)
            {
                if (!ta.getText().equals(""))
                {
                    updateCurrSize();
                    updateCurrPos();
                }
            }
        });
        undo = new ArrayList();
        undoPos = -1;
        ta.getDocument().addUndoableEditListener(new UndoableEditListener()
        {
            public void undoableEditHappened(UndoableEditEvent e)
            {
                if (undoPos + 1 != undo.size())
                    for (int i = undo.size() - 1; i > undoPos; i--)
                        undo.remove(i);
                undo.add(e.getEdit());
                undoPos++;
            }
        });
        ta.addCaretListener(new CaretListener()
        {
            public void caretUpdate(CaretEvent e)
            {
                updateCurrPos();
            }
        });

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    public static int loadRawText()
    {
        // Loads the raw text area(s) into the specified list of strings.
        // Text from 0x50200 - 0x08BE2D, 0x8DC31 - 0x0A012E, 0x2F5020 -
        // 0x2FA57A
        //int address = 0x51D12;
        int address = 0x50200;
        int istr = 0;

        textLists[RAW].clear();

        do
        {
            if (address < 0x0A012E || address >= 0x2F5020)
            {
                if (address < 0x08BE2D || address >= 0x8DC31)
                {
                    StrInfo s = cc.readString(address);

                    textLists[RAW].add(s);

                    address += s.str.length();
                    istr++;
                }
                else
                    address = 0x8DC31;
            }
            else
                address = 0x2F5020;
        }
        while (address < 0x2FA57A);

        //        System.out.println(
        //            "Done loading raw text! " + istr + " strings loaded.");
        return istr;
    }

    public static int loadEXPText(Rom rom)
    {
        int address = 0x300200;
        int istr;
        istr = 0;

        textLists[EXP].clear();

        if (rom.length() == 0x400200)
        {
            do
            {
                StrInfo s = cc.readString(address);

                textLists[EXP].add(s);

                address += s.str.length();
                istr++;

            }
            while (address < 0x400200);

            //        System.out.println(
            //            "Done loading exp text! " + istr + " strings loaded.");
            return istr;
        }
        else
        {
            return 0;
        }
    }

    public static int loadTPTText(HackModule hm)
    {
        TPTEditor.readFromRom(hm);

        textLists[TPT].clear();

        for (int i = 0; i < TPTEditor.NUM_ENTRIES; i++)
        {
            int address = TPTEditor.tptEntries[i].getPointer();
            if (address > 0 && (address = toRegPointer(address)) > 0)
            {
                StrInfo s = cc.readString(address);
                s.num = i;
                textLists[TPT].add(s);
            }

            if (TPTEditor.tptEntries[i].getType() != TPTEntry.TYPE_ITEM
                && (TPTEditor.tptEntries[i].getSecPointer() & 0xffffff) != 0)
            {
                address = TPTEditor.tptEntries[i].getSecPointer();
                if (address > 0 && (address = toRegPointer(address)) > 0)
                {
                    StrInfo s = cc.readString(address);
                    s.num = i;
                    textLists[TPT].add(s);
                }
            }
        }

        return textLists[TPT].size();
    }

    public static int loadText(int type, HackModule hm)
    {
        switch (type)
        {
            case TPT:
                return loadTPTText(hm);
            case RAW:
                return loadRawText();
            case EXP:
                return loadEXPText(hm.rom);
            default:
                return -1;
        }
    }

    public static void readFromRom(HackModule hm)
    {
        //        System.out.println(
        //            "("
        //                + new Date().toGMTString()
        //                + ") Going to init cc and comp list.");
        if (cc == null)
            cc = new CCInfo(DEFAULT_BASE_DIR + "codelist.txt", hm.rom);
        //        System.out.println(
        //            "("
        //                + new Date().toGMTString()
        //                + ") Finished initing cc and comp list.");

        for (int i = 0; i < NUM_TEXT_TYPES; i++)
        {
            //            System.out.println(
            //                "("
            //                    + new Date().toGMTString()
            //                    + ") Going to read "
            //                    + tabShortNames[i]
            //                    + " text.");
            loadText(i, hm);
            //            System.out.println(
            //                "("
            //                    + new Date().toGMTString()
            //                    + ") Finished reading "
            //                    + tabShortNames[i]
            //                    + " text.");
        }
    }

    public static class TextOffsetEntry extends JPanel
    {
        protected JTextField tf;
        protected JLabel t;
        protected boolean snes;
        protected String labelText, disabledLabelText;

        /**
         * Creates a new <code>TextOffsetEntry</code> component.
         * 
         * @param label words to identify this component with
         * @param snesDisplay If true, this is assumed to be containing a snes
         *            pointer and will have a "$" before the text field. If
         *            false, this is assumed to be a regular pointer, and will
         *            have "0x" before the text field.
         */
        public TextOffsetEntry(final String label, boolean snesDisplay)
        {
            super(new BorderLayout());

            this.snes = snesDisplay;

            tf = HackModule.createSizedJTextField(6);

            this.add(tf, BorderLayout.EAST);

            t = new JLabel(labelText = "<html><font color = \"blue\"><u>"
                + label + "</u></font>" + ": " + (snes ? "$" : "0x")
                + "</html>");
            disabledLabelText = label + ": " + (snes ? "$" : "0x");
            t.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent arg0)
                {
                    if (isEnabled())
                    {
                        int off = Integer.parseInt(tf.getText(), 16);
                        if (snes)
                            off = HackModule.toRegPointer(off);
                        JHack.main.showModule(TextEditor.class,
                            new Integer(off));
                    }
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

        public int getOffset(boolean snes)
        {
            int t = Integer.parseInt(tf.getText(), 16);
            if (this.snes == snes)
                return t;
            else if (this.snes)
                return HackModule.toRegPointer(t);
            else
                return HackModule.toSnesPointer(t);
        }

        public int getOffset()
        {
            return getOffset(snes);
        }

        public void setOffset(int offset, boolean snes)
        {
            if (this.snes == snes)
                tf.setText(HackModule.addZeros(Integer.toHexString(offset), 6));
            else if (this.snes)
                tf.setText(HackModule.addZeros(Integer.toHexString(HackModule
                    .toSnesPointer(offset)), 6));
            else
                tf.setText(HackModule.addZeros(Integer.toHexString(HackModule
                    .toRegPointer(offset)), 6));
        }

        public void setOffset(int offset)
        {
            setOffset(offset, snes);
        }

        public void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);
            tf.setEnabled(enabled);
            t.setText(enabled ? labelText : disabledLabelText);
            t.setEnabled(enabled);
        }
    }

    public String getVersion()
    {
        return "0.5";
    }

    public String getDescription()
    {
        return "Text Editor";
    }

    public String getCredits()
    {
        return "Ported by AnyoneEB\n" + "Based on source code by Mr. Accident";
    }
    private JTabbedPane selectorArea;
    private JLabel positionLabel, addressLabel, sizeLabel, currSizeLabel,
            currPosLabel;
    private JTextArea ta, codeHelpTa;
    private JList[] textJLists;
    private JCheckBoxMenuItem preventOverwrites, codesOnly, useComp;
    private JComponent previewDisp;
    private JMenuItem showCodeHelp, showEntry, showPreview;
    private boolean gotoDialogInited = false;
    private JDialog gotoDialog, findWindow, codeHelp, previewDialog,
            quickCodeDialog;
    private List undo;
    private int undoPos;

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        readFromRom(this);
        for (int i = 0; i < textJLists.length; i++)
        {
            //            System.out.println(
            //                "("
            //                    + new Date().toGMTString()
            //                    + ") Going to updateUI "
            //                    + tabShortNames[i]
            //                    + " text JList.");
            textJLists[i].updateUI();
            //            System.out.println(
            //                "("
            //                    + new Date().toGMTString()
            //                    + ") Done with updateUI on "
            //                    + tabShortNames[i]
            //                    + " text JList.");
        }

        //        System.out.println(
        //            "(" + new Date().toGMTString() + ") Going to show main window.");
        mainWindow.setVisible(true);
        //        System.out.println(
        //            "("
        //                + new Date().toGMTString()
        //                + ") Done with showing main window.");
        showInfo(currentList, currentSelection);
    }

    /**
     * Shows text editor and jumps to the specified line. <code>in</code> can
     * be an <code>int[]</code> with the first index being the type of text
     * (ex. {@link #TPT}or {@link #EXP}) and the second being which line to
     * jump to. <code>in</code> can also be an <code>Integer</code> which
     * will be taken as the offset to jump to. If <code>in</code> matches
     * neither of those, <code>in.toString()</code> will be taken as a hex
     * string of the offset to jump to.
     * 
     * @param in where to jump to
     */
    public void show(Object in) throws IllegalArgumentException
    {
        if (mainWindow == null || !mainWindow.isShowing())
            show();
        if (in instanceof int[])
        {
            int[] tmp = (int[]) in;
            selectorArea.setSelectedIndex(tmp[0]);
            textJLists[tmp[0]].setSelectedIndex(tmp[1]);
            textJLists[tmp[0]].ensureIndexIsVisible(tmp[1]);
        }
        else if (in instanceof Integer)
        {
            gotoOffset(((Integer) in).intValue());
        }
        else
        {
            gotoOffset(Integer.parseInt(in.toString(), 16));
        }
    }

    public void hide()
    {
        if (findWindow != null)
            findWindow.setVisible(false);
        mainWindow.setVisible(false);
    }

    private boolean isPreventOverwrites()
    {
        return preventOverwrites.isSelected();
    }

    private boolean isCodesOnly()
    {
        return codesOnly.isSelected();
    }

    private void resetList()
    {
        textJLists[currentList].updateUI();
    }

    private void saveInfo()
    {
        boolean comp = useComp.isSelected();
        if (comp && cc.getStringLength(ta.getText(), true) == 0)
        {
            comp = false;
        }
        String text = comp ? cc.compressString(ta.getText()) : ta.getText();
        StrInfo si = (StrInfo) textLists[currentList].get(currentSelection);
        if (cc.getStringLength(text, false) > si.str.length()
            && isPreventOverwrites())
        {
            JOptionPane.showMessageDialog(mainWindow,
                "The new text is larger than the old text. Shorten the text,\n"
                    + "or uncheck the \"Prevent Overwrites\" box.\n"
                    + "NOTE: Do not uncheck \"Prevent Overwrites\"\n"
                    + "unless you know what you are doing!",
                "Pre-Write Error: Text too long", JOptionPane.ERROR_MESSAGE);
            return;
        }
        cc.writeString(cc.deparseString(text), si.address);
        loadText(currentList, this);
        resetList();
        showInfo();
    }

    private StrInfo si;

    private void showInfo()
    {
        si = (StrInfo) textLists[currentList].get(currentSelection);

        positionLabel.setText("Line: " + (currentSelection + 1) + "/"
            + textLists[currentList].size());
        addressLabel.setText("Address: 0x" + Integer.toHexString(si.address)
            + " | $"
            + Integer.toHexString(HackModule.toSnesPointer(si.address)));
        sizeLabel.setText("Size: " + si.str.length());

        if (isCodesOnly())
        {
            ta.setText(cc.parseCodesOnly(si.str));
        }
        else
        {
            ta.setText(cc.parseString(si.str));
        }
        if (codeHelp != null && codeHelp.isShowing())
            updateCodeHelp();
        if (currentList == TPT)
            showEntry.setEnabled(true);
        else
            showEntry.setEnabled(false);
        
        clearUndo();
    }

    public int currentList = TPT;
    public int currentSelection = 0;

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().startsWith("textList"))
        {
            String tmp = ae.getActionCommand().substring(8, 11);
            if (tmp.equals("tpt"))
                currentList = TPT;
            else if (tmp.equals("raw"))
                currentList = RAW;
            else if (tmp.equals("exp"))
                currentList = EXP;
            else
            {
                System.out.println("textList action code failed");
                return;
            }
            currentSelection = Integer.parseInt(ae.getActionCommand()
                .substring(11));
            showInfo();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            ta.copy();
        }
        else if (ae.getActionCommand().equals("cut"))
        {
            ta.cut();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            ta.paste();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            ta.replaceSelection("");
        }
        else if(ae.getActionCommand().equals("undo"))
        {
            undo();
        }
        else if(ae.getActionCommand().equals("redo"))
        {
            redo();
        }
        else if (ae.getActionCommand().equals("useComp"))
        {
            updateCurrSize();
        }
        else if (ae.getActionCommand().equals("find"))
        {
            initFindWindow();
            findWindow.setVisible(true);
        }
        else if (ae.getActionCommand().equals("findNext"))
        {
            find();
        }
        else if (ae.getActionCommand().equals("goto"))
        {
            if (!gotoDialogInited)
            {
                gotoDialogInited = true;
                initGotoDialog();
            }
            gotoDialog.setVisible(true);
        }
        else if (ae.getActionCommand().equals("codesOnly"))
        {
            String text = useComp.isSelected()
                && cc.getStringLength(ta.getText(), true) > 0 ? cc
                .compressString(ta.getText()) : ta.getText();
            String tmp = cc.deparseString(text);
            if (isCodesOnly())
            {
                ta.setText(cc.parseCodesOnly(tmp));
            }
            else
            {
                ta.setText(cc.parseString(tmp));
            }
        }
        else if (ae.getActionCommand().equals("showCodeHelp"))
        {
            if (codeHelp == null)
                initCodeHelp();
            codeHelp.setVisible(true);
            updateCodeHelp();
        }
        else if (ae.getActionCommand().equals("showPreview"))
        {
            initPreview();
            previewDialog.setVisible(true);
            updatePreview();
        }
        else if (ae.getActionCommand().equals("showEntry"))
        {
            showEntry();
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

    private void showEntry()
    {
        if (currentList == TPT)
        {
            Class editor = TPTEditor.class;
            JHack.main.showModule(editor, new Integer(
                ((StrInfo) textLists[currentList].get(currentSelection)).num));
        }
    }
    
    private void clearUndo()
    {
        undo.clear();
        undoPos = -1;
    }
    private void undo()
    {
        if(undoPos > -1)
        ((UndoableEdit)undo.get(undoPos--)).undo();
    }
    private void redo()
    {
        if(undo.size() > undoPos + 1)
        ((UndoableEdit)undo.get(++undoPos)).redo();
    }

    private boolean findWindowInited = false;
    private JTextField findTF;

    private void find()
    {
        if (findTF == null)
            return;
        String f = cc.deparseString(findTF.getText().toLowerCase());
        int tmp = 0, s = textLists[currentList].size(), c = currentSelection + 1;
        for (int i = 0; i < textLists[currentList].size(); i++)
        {
            int j = i + c < s ? i + c : i + c - s;
            if (cc.deparseString(
                cc.parseString((((StrInfo) textLists[currentList].get(j)).str))
                    .toLowerCase()).indexOf(f) != -1)
            {
                showInfo(currentList, j);
                return;
            }
        }
    }

    private void initFindWindow()
    {
        if (!findWindowInited)
        {
            findWindowInited = true;
            findWindow = new JDialog(mainWindow, "Text Editor Find", false);
            findWindow.setLocation(findWindow.getOwner().getLocation());
            findWindow.getContentPane().setLayout(new BorderLayout());

            findWindow.getContentPane().add(findTF = new JTextField(),
                BorderLayout.NORTH);

            JPanel buttons = new JPanel(new FlowLayout());
            JButton findb = new JButton("Find");
            findb.setActionCommand("findb");
            findb.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    find();
                }
            });
            findWindow.getRootPane().setDefaultButton(findb);
            buttons.add(findb);
            JButton closeb = new JButton("Close");
            closeb.setActionCommand("closefindb");
            closeb.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    findWindow.setVisible(false);
                }
            });
            buttons.add(closeb);
            findWindow.getContentPane().add(buttons);

            findWindow.pack();
        }
    }

    private void showInfo(int list, int sel)
    {
        selectorArea.setSelectedIndex(list);
        textJLists[list].setSelectedIndex(sel);
        textJLists[list].ensureIndexIsVisible(sel);
        currentList = list;
        currentSelection = sel;
        showInfo();
    }

    private boolean isOffsetInStr(StrInfo si, int offset)
    {
        if (offset == si.address)
            return true;
        if (offset > si.address && offset < si.address + si.str.length())
            return true;
        return false;
    }

    private boolean gotoRaw(int listNum, int target)
    {
        for (int i = 0; i < textLists[listNum].size(); i++)
        {
            //make sure offset is before the offset of the next entry
            if (target < (i + 1 < textLists[listNum].size()
                ? ((StrInfo) textLists[listNum].get(i + 1)).address
                : (listNum == EXP ? 0x400201 : 0x300200))
                && isOffsetInStr((StrInfo) textLists[listNum].get(i), target))
            {
                showInfo(listNum, i);
                return true;
            }
        }
        return false;
    }

    private boolean gotoTpt(int target)
    {
        for (int i = 0; i < textLists[TPT].size(); i++)
        {
            if (isOffsetInStr((StrInfo) textLists[TPT].get(i), target))
            {
                showInfo(TPT, i);
                return true;
            }
        }
        return false;
    }

    public boolean gotoOffsetBlock(int offset)
    {
        //check exp first
        if (offset >= 0x300200 && gotoRaw(EXP, offset))
            return true;
        //then raw (text from 0x51D12 - 0x08BE2D, 0x8DC31 - 0x0A012E,
        //    0x2F5020 - 0x2FA57A)
        else if (((offset >= 0x51d12 && offset <= 0x08be2d)
            || (offset >= 0x8DC31 && offset <= 0x0A012E) || (offset >= 0x2F5020 && offset <= 0x2FA57A))
            && gotoRaw(RAW, offset))
            return true;
        else if (gotoTpt(offset))
            return true;
        return false;
    }

    public boolean gotoOffset(int offset)
    {
        if (gotoOffsetBlock(offset))
        {
            try
            {
                ta.setCaretPosition(cc.getStringIndex(ta.getText(), useComp
                    .isSelected(), offset - si.address));
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            return true;
        }
        else
            return false;
    }

    private void initGotoDialog()
    {
        gotoDialog = new JDialog(mainWindow, "Goto Offset", true);
        gotoDialog.setLocation(gotoDialog.getOwner().getLocation());
        final JTextField tf = HackModule.createSizedJTextField(6);
        final JLabel prefix = new JLabel("0x");
        prefix.setHorizontalAlignment(SwingConstants.RIGHT);
        prefix.setPreferredSize(new Dimension(20, 5));
        JPanel diaTop = new JPanel(new BorderLayout());
        diaTop.add(tf, BorderLayout.CENTER);
        diaTop.add(prefix, BorderLayout.WEST);
        gotoDialog.getContentPane().setLayout(new BorderLayout());
        gotoDialog.getContentPane().add(diaTop, BorderLayout.NORTH);
        ButtonGroup type = new ButtonGroup();
        final JRadioButton regType = new JRadioButton("Regular (0x)"), snesType = new JRadioButton(
            "SNES ($)");
        regType.setSelected(true);
        type.add(regType);
        type.add(snesType);
        regType.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent arg0)
            {
                prefix.setText(regType.isSelected() ? "0x" : "$");
            }
        });
        JPanel typeSel = new JPanel(new BorderLayout());
        typeSel.add(regType, BorderLayout.WEST);
        typeSel.add(snesType, BorderLayout.EAST);
        gotoDialog.getContentPane().add(typeSel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout());
        JButton gotob = new JButton("Goto");
        gotoDialog.getRootPane().setDefaultButton(gotob);
        gotob.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                int offset = 0;
                try
                {
                    if (tf.getText().startsWith("-"))
                        throw new NumberFormatException("Value is negative.");
                    offset = regType.isSelected() ? Integer.parseInt(tf
                        .getText(), 16) : HackModule.toRegPointer(Integer
                        .parseInt(tf.getText(), 16));
                }
                catch (NumberFormatException e)
                {
                    if (tf.getText().charAt(0) == '-'
                        || tf.getText().charAt(0) == '+')
                    {
                        //                        System.out.println("Going to a relative offset... ("
                        //                            + tf.getText() + ")");
                        offset = si.address
                            + cc.getStringLength(ta.getText(), useComp
                                .isSelected(), 0, ta.getCaretPosition());
                        String tmp = tf.getText().substring(1);
                        try
                        {
                            offset += (Integer.parseInt(tmp, 16) * (tf
                                .getText().charAt(0) == '-' ? -1 : 1));
                        }
                        catch (NumberFormatException e1)
                        {}
                    }
                }
                if (!gotoOffset(offset))
                    JOptionPane.showMessageDialog(gotoDialog,
                        "The offset you entered is not "
                            + "inside any text block.\n"
                            + "Make sure you were using only "
                            + "legal hexidecimal digits except\n"
                            + "for a possible + or - prefix.",
                        "Unable to find offset", JOptionPane.ERROR_MESSAGE);
                else
                    gotoDialog.setVisible(false);
            }
        });
        buttons.add(gotob);
        JButton closeb = new JButton("Cancel");
        closeb.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                gotoDialog.setVisible(false);
            }
        });
        buttons.add(closeb);
        gotoDialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        gotoDialog.pack();
    }

    private void updateCodeHelp()
    {
        CCInfo.CCNode[] ccs = cc.getCCsUsed(((StrInfo) textLists[currentList]
            .get(currentSelection)).str);
        String out = new String();
        for (int i = 0; i < ccs.length; i++)
        {
            if (i != 0)
                out += "\n";
            out += ccs[i].toString() + ": " + ccs[i].desc;
        }
        codeHelpTa.setText(out);
    }

    private void initCodeHelp()
    {
        if (codeHelp != null)
            return;
        codeHelp = new JDialog(mainWindow, "CC Help", false);
        codeHelp.setLocation(codeHelp.getOwner().getLocation());
        codeHelp.getContentPane().setLayout(new BorderLayout());

        codeHelp.getContentPane().add(
            new JScrollPane(codeHelpTa = new JTextArea()), BorderLayout.CENTER);
        codeHelpTa.setEditable(false);
        codeHelpTa.setColumns(80);
        codeHelpTa.setRows(15);
        codeHelpTa.setLineWrap(false);

        JButton closeb = new JButton("Close");
        codeHelp.getRootPane().setDefaultButton(closeb);
        closeb.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                codeHelp.setVisible(false);
            }
        });
        codeHelp.getContentPane().add(HackModule.createFlowLayout(closeb),
            BorderLayout.SOUTH);

        codeHelp.pack();
    }

    private void updateCurrSize()
    {
        boolean comp = useComp.isSelected();
        int a = cc.getStringLength(ta.getText(), comp);
        if (comp && a <= 0)
        {
            comp = false;
            a = cc.getStringLength(ta.getText(), false);
            //            System.out.println(
            //                "Compression failed, uncompressed size is "
            //                    + cc.getStringLength(ta.getText())
            //                    + " of \""
            //                    + ta.getText()
            //                    + "\"");
        }
        //System.out.println(cc.compressString(ta.getText()));
        if (a >= 0)
        {
            currSizeLabel.setText("Current Size: "
                + a
                + (comp ? "" : (comp == useComp.isSelected()
                    ? " (uncompressed)"
                    : " (uncompressed, compression failed)")));
            currSizeLabel
                .setForeground((a > ((StrInfo) textLists[currentList]
                    .get(currentSelection)).str.length())
                    ? Color.RED
                    : Color.BLACK);
        }
        if (previewDialog != null && previewDialog.isShowing())
            updatePreview();
    }

    private void updateCurrPos()
    {
        int pos = -1;
        try
        {
            pos = cc.getStringLength(ta.getText(), useComp.isSelected(), 0, ta
                .getCaretPosition());
        }
        catch (StringIndexOutOfBoundsException e)
        {}
        if (pos < 0)
            return;
        int posAdd = si.address + pos;
        currPosLabel.setText("Current Position: " + pos + " | 0x"
            + Integer.toHexString(posAdd) + " | $"
            + Integer.toHexString(toSnesPointer(posAdd)));
    }

    private void initPreview()
    {
        previewDialog = new JDialog(mainWindow, "Text Preview", false);
        previewDialog.getContentPane().setLayout(new BorderLayout());

        previewDisp = new JPanel();
        //size of text space is 136x48, border is 8 px on each side
        previewDisp.setPreferredSize(new Dimension(136 + 8 + 8, 48 + 8 + 8));
        previewDisp.setBackground(Color.BLACK);
        previewDialog.getContentPane().add(
            HackModule.createFlowLayout(previewDisp), BorderLayout.NORTH);

        JButton closeb = new JButton("Close");
        previewDialog.getRootPane().setDefaultButton(closeb);
        closeb.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                previewDialog.setVisible(false);
            }
        });
        previewDialog.getContentPane().add(HackModule.createFlowLayout(closeb),
            BorderLayout.SOUTH);

        previewDialog.pack();

        FontEditor.readFromRom(this);
    }

    private void updatePreview()
    {
    //        Image img = previewDisp.createImage(136, 48);
    //        previewDisp.getGraphics().drawImage(
    //            FontEditor.mainFont.drawString("Testing", img, 0, 0),
    //            8,
    //            8,
    //            null);
    //TODO make class for preview component (exteral)
    }
}