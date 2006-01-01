package net.starmen.pkhack.wof;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.MaxLengthDocument;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.XMLPreferences;

public class TextEditor extends WofHackModule implements ActionListener, ListSelectionListener {
	public TextEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	private static final int NAMES_LIST = 0;
	
	public static final int POINTERS = 0x101e8;
	public static final int NUM_POINTERS = 5;
	
	public static final int NAMES_ADDR = 0x409f;
	public static final int NUM_NAMES = 20;
	public static final int NAME_LENGTH = 10;
	
	public static final int TEXT_ADDR = 0x109d0;
	//public static final int NUM_ENTRIES = 1002;
	public static final int[] ROW_SIZES = { 11, 13, 13, 11 };
	public static final char LINE_BREAK = '*';
	public static final int TEXT_BLOCK_END = 0x17f8f;
    
	public static int[][] textAddresses = new int[NUM_POINTERS][];
    
	private JList[] textJLists = new JList[NUM_POINTERS];
	private JList namesJList;
	private JTextField editField;
	private JTextArea preview;
	private JTabbedPane tp;
	private PrefsCheckBox showLineNumbers;
	private int selectedTab = 1;
	private String lastSearch = "";
	
	private class WofListModel implements ListModel {
		private int num;
		public WofListModel(int num) {
			this.num = num;
		}
		
        public int getSize()
        {
            return textAddresses[num].length;
        }

        public Object getElementAt(int a)
        {
        	if (showLineNumbers())
        		return getNumberedString(getText(rom, num, a), a+1);
        	else
        		return getText(rom, num, a);
        }

        ArrayList listeners = new ArrayList();

        public void addListDataListener(ListDataListener ldl)
        {
            listeners.add(ldl);
        }

        public void removeListDataListener(ListDataListener ldl)
        {
            listeners.remove(ldl);
        }
	}
	
	private class WofMouseListener implements MouseListener {
		private int num;
		public WofMouseListener(int num) {
			this.num = num;
		}
		
		public void mouseClicked(MouseEvent me)
        {
            if (me.getButton() == 1)
                if (textJLists[num].getSelectedIndex() > -1)
                    showInfo(num + 1, textJLists[num].getSelectedIndex());
        }

        public void mouseEntered(MouseEvent me)
        {}

        public void mouseExited(MouseEvent me)
        {}

        public void mousePressed(MouseEvent me)
        {}

        public void mouseReleased(MouseEvent me)
        {}
	}
	
	protected void init() {
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.setMnemonic('f');
		menu.add(createJMenuItem("Apply Changes", 'a', null,
	            "apply", this));
		menu.add(new JSeparator());
		menu.add(createJMenuItem("Import Text", 'i', null,
	            "importText", this));
		menu.add(createJMenuItem("Export Text", 'e', null,
	            "exportText", this));
		menu.add(new JSeparator());
		menu.add(createJMenuItem("Export Names", 'x', null,
	            "exportNames", this));
		menu.add(createJMenuItem("Import Names", 'm', null,
	            "importNames", this));
		mb.add(menu);
		menu = new JMenu("Edit");
		menu.setMnemonic('e');
		menu.add(createJMenuItem("Cut", 't', "ctrl X", "cut",
	            this));
        menu.add(createJMenuItem("Copy", 'c', "ctrl C", "copy",
	            this));
        menu.add(createJMenuItem("Paste", 'p', "ctrl V",
	            "paste", this));
        menu.add(createJMenuItem("Delete", 'd', "DELETE",
	            "delete", this));
        menu.add(new JSeparator());
        menu.add(createJMenuItem("Find", 'f', "ctrl F", "find",
	            this));
        menu.add(createJMenuItem("Find Next", 'n', "F3",
	            "findNext", this));
        menu.add(new JSeparator());
        menu.add(createJMenuItem("Goto", 'g', "ctrl G", "goto",
	            this));
        mb.add(menu);
        menu = new JMenu("Options");
        menu.setMnemonic('o');
        menu.add(showLineNumbers = new PrefsCheckBox("Show Line Numbers", JHack.main
                .getPrefs(), "wof_text_editor.line_nums", false, 'n', "alt N",
                "lineNums", this));
        mb.add(menu);
        mainWindow.setJMenuBar(mb);
		
		tp = new JTabbedPane();
		
		JPanel panel;
		JScrollPane scroll;
		
		namesJList = new JList(new ListModel()
                {

                    public int getSize()
                    {
                        return NUM_NAMES;
                    }

                    public Object getElementAt(int a)
                    {
                    	if (showLineNumbers())
                    		return getNumberedString(getName(rom, a), a+1);
                    	else
                    		return getName(rom, a);
                    }

                    ArrayList listeners = new ArrayList();

                    public void addListDataListener(ListDataListener ldl)
                    {
                        listeners.add(ldl);
                    }

                    public void removeListDataListener(ListDataListener ldl)
                    {
                        listeners.remove(ldl);
                    }
                });
        namesJList.addMouseListener(new MouseListener()
                {
                    public void mouseClicked(MouseEvent me)
                    {
                        if (me.getButton() == 1)
                            if (namesJList.getSelectedIndex() > -1)
                                showInfo(NAMES_LIST, namesJList.getSelectedIndex());
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
        namesJList.addListSelectionListener(this);
        namesJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scroll = new JScrollPane(namesJList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(550, 270));
        tp.addTab("Names", scroll);
        
		for (int i = 0; i < textJLists.length; i++) {
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			textJLists[i] = new JList(new WofListModel(i));
			textJLists[i].addMouseListener(new WofMouseListener(i));
	        textJLists[i].addListSelectionListener(this);
	        textJLists[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	        scroll = new JScrollPane(textJLists[i],
	                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
	                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	        scroll.setPreferredSize(new Dimension(550, 270));
	        panel.add(scroll);
	        tp.addTab("Text " + i, panel);
		}
        
        panel = new JPanel();
		panel.setLayout(
				new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(tp);
        
        preview = new JTextArea();
        preview.setEditable(false);
        preview.setFont(new Font("Courier", Font.PLAIN, 12));
        preview.setSize(13, 3);
        panel.add(new JLabel("Preview:"));
        panel.add(preview);
        panel.add(preview);
        
        editField = createSizedJTextField(0, false);
        editField.getDocument().addDocumentListener(new DocumentListener()
                {
                    public void changedUpdate(DocumentEvent de)
                    {
                    	preview.setText(getPreviewText(editField.getText()));
                    }

                    public void insertUpdate(DocumentEvent de)
                    {
                        changedUpdate(de);
                    }

                    public void removeUpdate(DocumentEvent de)
                    {
                        if (!editField.getText().equals(""))
                        	changedUpdate(de);
                    }
                });
        panel.add(new JLabel("Edit:"));
        panel.add(editField);
        
        mainWindow.getContentPane().add(panel, BorderLayout.CENTER);
        mainWindow.pack();
	}
	
	private static void readFromRom(AbstractRom rom) {
		int numEntries;
		for (int i = 0; i < NUM_POINTERS; i++) {
			if (i + 1 == NUM_POINTERS)
				numEntries = (0x89bf - rom.readMulti(POINTERS + i * 2, 2)) / 2;
			else
				numEntries = (rom.readMulti(POINTERS + (i + 1) * 2, 2) - rom.readMulti(POINTERS + i * 2, 2)) / 2;
			
			textAddresses[i] = new int[numEntries];
			
			for (int j = 0; j < numEntries; j++)
				textAddresses[i][j] = rom.readMulti(rom.readMulti(POINTERS + i * 2, 2) + 0x8010 + 2 * j, 2) + 0x8010;
		}
	}
	
	private static boolean writeTextToRom(AbstractRom rom, int category, int num, byte[] newText) {
		// back up old data in case this doesn't work
		byte[] oldData = rom.readByte(TEXT_ADDR, TEXT_BLOCK_END - TEXT_ADDR);
		byte[] oldPointers = rom.readByte(POINTERS, NUM_POINTERS * 2);
		byte[] oldPointers2 = rom.readByte(0x101fc, 0x109cf - 0x101fc);
		
		int address = TEXT_ADDR;
		byte[] text;
		for (int i = 0; i < NUM_POINTERS; i++) {
			for (int j = 0; j < textAddresses[i].length; j++) {
				if ((i == category) && (j == num))
					text = newText;
				else
					text = getRawText(getText(rom, i, j));
				
				if (address + text.length > TEXT_BLOCK_END) {
					rom.write(POINTERS, oldPointers);
					rom.write(0x101fc, oldPointers2);
					rom.write(TEXT_ADDR, oldData);
					return false;
				}
				
				rom.write(address, text);
				rom.write(0x8010 + rom.readMulti(POINTERS + i * 2, 2) + j * 2,
						address - 0x8010, 2);
				address += text.length;
			}
		}
		return true;
	}
	
	public static void writeNameToRom(AbstractRom rom, int num, byte[] newName) {
		rom.write(NAMES_ADDR + num * NAME_LENGTH, newName);
	}
	
	public static String getText(AbstractRom rom, int list, int num) {
		if (textAddresses[list][num] < 0)
			return "Null";
		
		String s = new String();
		int breaks = 0, letter = -1;
		char brokenChar = ' ';
		
		rom.seek(textAddresses[list][num]);
		while (breaks < 2) {
			if ((letter = rom.readSeek()) <= 0x5a) {
				if (breaks > 0) {
					s = s + brokenChar + LINE_BREAK;
					breaks = 0;
				}
				s = s + getCharFromInt(letter);
			} else {
				if (breaks == 0)
					brokenChar = getCharFromInt(letter - 0x80);
				else
					s = s + brokenChar + getCharFromInt(letter - 0x80);
				breaks++;
			}
		}
		
		return s;
	}
	
	public static String getName(AbstractRom rom, int num) {
		String name = new String();
		int spaces = 0;
		char tmp;
		rom.seek(NAMES_ADDR + num * NAME_LENGTH);
		for (int i = 0; i < NAME_LENGTH; i++) {
			if ((tmp = getCharFromInt(rom.readSeek())) == ' ')
				spaces++;
			else {
				if (spaces > 0) {
					name = name + repeatChar(' ', spaces);
					spaces = 0;
				}
				name = name + tmp;
			}
		}
		return name;
	}
	
	public static char getCharFromInt(int value) {
		return (char) value;
	}
	
	private boolean showLineNumbers() {
		return showLineNumbers.isSelected();
	}
	
	private void showInfo(int listNum, int entryNumber) {
		selectedTab = listNum;
		tp.setSelectedIndex(listNum);
		if (listNum == NAMES_LIST) {
			namesJList.setSelectedIndex(entryNumber);
			namesJList.ensureIndexIsVisible(entryNumber);
			editField.setColumns(NAME_LENGTH);
			((MaxLengthDocument) editField.getDocument()).setMaxLength(NAME_LENGTH);
			editField.setText(getName(rom, entryNumber));
			preview.setText(getPreviewText(editField.getText()));
		} else {
			textJLists[listNum - 1].setSelectedIndex(entryNumber);
			textJLists[listNum - 1].ensureIndexIsVisible(entryNumber);
			editField.setColumns(ROW_SIZES[0] + ROW_SIZES[1] + ROW_SIZES[2] + ROW_SIZES[3] + 3);
			((MaxLengthDocument) editField.getDocument()).setMaxLength(
					ROW_SIZES[0] + ROW_SIZES[1] + ROW_SIZES[2] + ROW_SIZES[3] + 3);
			editField.setText(getText(rom, listNum - 1, entryNumber));
			preview.setText(getPreviewText(editField.getText()));
		}
	}
	
	public static String getPreviewText(String str) {
		str = str.toUpperCase();
		String[] previewText = new String[4];
		int i = 0, j, lines = 0;
		while (true) {
			j = str.indexOf(LINE_BREAK, i);
			if (j == -1) {
				previewText[lines] = str.substring(i, str.length());
				lines++;
				break;
			} else {
				previewText[lines] = str.substring(i, j);
				i = j + 1;
				lines++;
			}
		}
		
		switch (lines) {
			case 1:
				return " \n"
					+ repeatChar(' ', (ROW_SIZES[1] - previewText[0].length()) / 2) + previewText[0] + "\n"
					+ "\n";
			case 2:
				return "\n"
					+ repeatChar(' ', (ROW_SIZES[1] - previewText[0].length()) / 2) + previewText[0] + "\n"
					+ repeatChar(' ', (ROW_SIZES[2] - previewText[1].length()) / 2) + previewText[1] + "\n";
			case 3:
				return repeatChar(' ', (ROW_SIZES[0] - previewText[0].length()) / 2) + previewText[0] + "\n"
					+ repeatChar(' ', (ROW_SIZES[1] - previewText[1].length()) / 2) + previewText[1] + "\n"
					+ repeatChar(' ', (ROW_SIZES[2] - previewText[2].length()) / 2) + previewText[2] + "\n";
			case 4:
				return repeatChar(' ', (ROW_SIZES[0] - previewText[0].length()) / 2) + previewText[0] + "\n"
					+ repeatChar(' ', (ROW_SIZES[1] - previewText[1].length()) / 2) + previewText[1] + "\n"
					+ repeatChar(' ', (ROW_SIZES[2] - previewText[2].length()) / 2) + previewText[2] + "\n"
					+ repeatChar(' ', (ROW_SIZES[3] - previewText[3].length()) / 2) + previewText[3];
			default:
				return "Error";
		}
	}
	
	public static String repeatChar(char ch, int iterations) {
		String out = new String();
		for (int i = 0; i < iterations; i++)
			out = out + ch;
		return out;
	}

	public String getVersion() {
		return "0.1";
	}

	public String getDescription() {
		return "Text Editor";
	}

	public String getCredits() {
		return "Written by Mr. Tenda\n"
			+ "Pointer table locations from Michael1/Alchemic";
	}
	
	public void show() {
		readFromRom(rom);
		super.show();
		
		if (textJLists[selectedTab].getSelectedIndex() < 0)
			showInfo(selectedTab, 0);
		else
			showInfo(selectedTab,
					getSelectedJList().getSelectedIndex());
		
		mainWindow.setVisible(true);
	}
	
	private JList getSelectedJList() {
		if (selectedTab == NAMES_LIST)
			return namesJList;
		else
			return textJLists[selectedTab];
	}

	public void hide() {
		mainWindow.setVisible(false);
	}
	
	public static int getNumOccurances(String str, char ch) {
		int i = 0, j, occ = 0;
		while (true) {
			j = str.indexOf(ch, i);
			if (j == -1)
				return occ;
			else {
				occ++;
				i = j + 1;
			}
		}
	}
	
	public static byte[] getRawText(String str) {
		str = str.toUpperCase();
		byte[] raw = new byte[str.length() - getNumOccurances(str, LINE_BREAK)];
		int byteNum = 0, lettersInLine = 0;
		boolean multipleLines = false;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == LINE_BREAK) {
				if (lettersInLine < 2)
					return null; // each line must have at least 2 letters
				lettersInLine = 0;
				multipleLines = true;
				raw[byteNum - 1] += 0x80;
			} else {
				//if str.charAt
				raw[byteNum] = (byte) str.charAt(i);
				byteNum++;
				lettersInLine++;
			}
		}
		if (multipleLines && (lettersInLine < 3))
			return null; // if there are multiple lines, the last must have at least 3 letters
		raw[raw.length - 1] += 0x80;
		raw[raw.length - 2] += 0x80;
		return raw;
	}
	
	public static byte[] getRawName(String str) {
		str = str.toUpperCase();
		byte[] raw = new byte[NAME_LENGTH];
		for (int i = 0; i < raw.length; i++) {
			if (i < str.length())
				raw[i] = (byte) str.charAt(i);
			else
				raw[i] = (byte) ' ';
		}
		return raw;
	}
	
	private boolean find(String search, int startAt) {
		if (selectedTab == NAMES_LIST) {
			for (int i = startAt + 1; i < NUM_NAMES; i++)
				if (getName(rom, i).indexOf(search) != -1) {
					showInfo(NAMES_LIST, i);
					return true;
				}
		} else {
			for (int i = startAt + 1; i < textAddresses[selectedTab].length; i++)
				if (getText(rom, selectedTab, i).indexOf(search) != -1) {
					showInfo(selectedTab, i);
					return true;
				}
		}
		return false;
	}
	
	private void exportText() {
		File f = getFile(true, "txt", "Plaintext");
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			for (int i = 0; i < textAddresses.length; i++) {
				ps.println(Character.toString(LINE_BREAK) + i);
				for (int j = 0; j < textAddresses[i].length; j++)
					ps.println(getText(rom, i, j).replace(LINE_BREAK, ' '));
			}
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void exportNames() {
		File f = getFile(true, "txt", "Plaintext");
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			for (int i = 0; i < NUM_NAMES; i++)
				ps.println(getName(rom, i));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	// TODO Make this take advantage of the two 13-character lines
	public static String addLineBreaks(String str) {
		int numLines = 1, lineLen = 0, lastSpaceIndex = -1;
		for (int i = 0; i < str.length() - 1; i++) {
			if (numLines > 4)
				return "";
			
			if (str.charAt(i) == ' ')
				lastSpaceIndex = i;
			
			if (lineLen > 11) {
				if (lastSpaceIndex == -1)
					return "";
				else
					str = str.substring(0, lastSpaceIndex) + LINE_BREAK
						+ str.substring(lastSpaceIndex + 1);
				numLines++;
				lineLen = lineLen - lastSpaceIndex - 1;
				lastSpaceIndex = -1;
			} else
				lineLen++;
		}
		
		return str;
	}
	
	private boolean importNames() {
		String[] listFile;
		try {
			listFile = new CommentedLineNumberReader(
			        new InputStreamReader(
			        		new FileInputStream(
			        				getFile(false, "txt", "Plaintext")))).readUsedLines();
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "Could not read file.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
		
		for (int i = 0; i < NUM_NAMES; i++)
			writeNameToRom(rom, i, getRawName(listFile[i]));
		return true;
	}
	
	private boolean importText() {
		String[] listFile;
		try {
			listFile = new CommentedLineNumberReader(
			        new InputStreamReader(
			        		new FileInputStream(
			        				getFile(false, "txt", "Plaintext")))).readUsedLines();
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
					mainWindow, "Could not read file.", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
		
		byte[] oldData = rom.readByte(TEXT_ADDR, TEXT_BLOCK_END - TEXT_ADDR);
		byte[] oldPointers = rom.readByte(POINTERS, NUM_POINTERS * 2);
		byte[] oldPointers2 = rom.readByte(0x101fc, 0x109cf - 0x101fc);
		
		int category = 0, catLines = 0, address = TEXT_ADDR;
		byte[] text;
		for (int i = 0; i < listFile.length; i++) {
			if (listFile[i].substring(0,1).equals(Character.toString(LINE_BREAK))) {
				text = getRawText("BLANK");
				while (catLines < textAddresses[category].length) {
					if (address + text.length > TEXT_BLOCK_END) {
						rom.write(POINTERS, oldPointers);
						rom.write(0x101fc, oldPointers2);
						rom.write(TEXT_ADDR, oldData);
						JOptionPane.showMessageDialog(
								mainWindow,
								"I could not import the data because it ended up taking up\n"
								+ "too much space. This seems very unlikely and is probably\n"
								+ "an error on my part. Please contact the author for support.",
								"Error", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					rom.write(address, text);
					rom.write(0x8010 + rom.readMulti(POINTERS + i * 2, 2) + catLines * 2,
							address - 0x8010, 2);
					textAddresses[category][catLines] = address;
					address += text.length;
					catLines++;
				}
				category = Integer.parseInt(listFile[i].substring(1,2));
				catLines = 0;
			} else if (catLines < textAddresses[category].length) {
				text = getRawText(addLineBreaks(listFile[i])); 
				
				if (address + text.length > TEXT_BLOCK_END) {
					rom.write(POINTERS, oldPointers);
					rom.write(0x101fc, oldPointers2);
					rom.write(TEXT_ADDR, oldData);
					JOptionPane.showMessageDialog(
							mainWindow,
							"I could not import the data because it ended up taking up\n"
							+ "too much space. This seems very unlikely and is probably\n"
							+ "an error on my part. Please contact the author for support.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				rom.write(address, text);
				rom.write(0x8010 + rom.readMulti(POINTERS + category * 2, 2) + catLines * 2,
						address - 0x8010, 2);
				textAddresses[category][catLines] = address;
				address += text.length;
				catLines++;
			}
		}
		
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("exportText"))
			exportText();
		if (e.getActionCommand().equals("exportNames"))
			exportNames();
		else if (e.getActionCommand().equals("importText")) {
			if (importText())
				for (int i = 0; i < textJLists.length; i++)
					textJLists[i].repaint();
		} else if (e.getActionCommand().equals("importNames")) {
			if (importNames())
				namesJList.repaint();
		} else if (e.getActionCommand().equals("find")) {
			lastSearch = JOptionPane.showInputDialog(mainWindow,
					"Enter string to search for.", lastSearch).toUpperCase();
			if (lastSearch == null)
				lastSearch = "";
			else if (!find(lastSearch, getSelectedJList().getSelectedIndex()))
				JOptionPane.showMessageDialog(mainWindow, "No matches found.");
		} else if (e.getActionCommand().equals("findNext")) {
			if (!find(lastSearch, getSelectedJList().getSelectedIndex()))
				JOptionPane.showMessageDialog(mainWindow, "No matches found.");
		} else if (e.getActionCommand().equals("copy"))
			editField.copy();
        else if (e.getActionCommand().equals("cut"))
        	editField.cut();
        else if (e.getActionCommand().equals("paste"))
        	editField.paste();
        else if (e.getActionCommand().equals("delete"))
        	editField.replaceSelection("");
        else if (e.getActionCommand().equals("goto")) {
        	try {
        		int input = Integer.parseInt(
        			JOptionPane.showInputDialog(mainWindow, "Enter the entry number to go to."));
        		if (input <= 0)
        			JOptionPane.showMessageDialog(mainWindow,
    						input + " is too small. The first index is 1.");
        		else if (selectedTab > NAMES_LIST) {
        			if (input > textAddresses[selectedTab].length)
        				JOptionPane.showMessageDialog(mainWindow,
        						input + " is too large. There are only " + textAddresses[selectedTab].length + " entries.");
        			else
        				showInfo(selectedTab, input - 1);
        		} else {
        			if (input > NUM_NAMES)
        				JOptionPane.showMessageDialog(mainWindow,
        						input + " is too large. There are only " + NUM_NAMES + " entries.");
        			else
        				showInfo(NAMES_LIST, input - 1);
        		}
        	} catch (NumberFormatException nfe) {
        		JOptionPane.showMessageDialog(mainWindow, "Inputted string was not a number.");
        	}
        } else if (e.getActionCommand().equals("lineNums")) {
        	for (int i = 0; i < textJLists.length; i++)
        		textJLists[i].repaint();
        	namesJList.repaint();
        } else if (e.getActionCommand().equals("apply")) {
			if (selectedTab > NAMES_LIST) {
				byte[] rawText = getRawText(editField.getText());
				if (rawText == null)
					JOptionPane.showMessageDialog(
							mainWindow,
							"Your string is invalid. Make sure that every line has\n"
							+ "at least two letters and, if there are multiple lines,\n"
							+ "the last one has at least three letters.",
							"Error", JOptionPane.ERROR_MESSAGE);
				else if (!writeTextToRom(rom, selectedTab - 1, textJLists[selectedTab - 1].getSelectedIndex(), rawText))
					JOptionPane.showMessageDialog(
							mainWindow,
							"I could not write the data because it ended up taking up\n"
							+ "too much space. This seems very unlikely and is probably\n"
							+ "an error on my part. Please contact the author for support.",
							"Error", JOptionPane.ERROR_MESSAGE);
				else {
					readFromRom(rom);
					textJLists[selectedTab - 1].repaint();
				}
			} else if (selectedTab == NAMES_LIST) {
				writeNameToRom(rom, namesJList.getSelectedIndex(), getRawName(editField.getText()));
				namesJList.repaint();
			}
		}
		else if (e.getActionCommand().equals("close"))
			hide();
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(namesJList))
			showInfo(NAMES_LIST, namesJList.getSelectedIndex());
		else {
			for (int i = 0; i < textJLists.length; i++)
				if (e.getSource().equals(textJLists[i])) {
					showInfo(i + 1, textJLists[i].getSelectedIndex());
					return;
				}			
		}
	}
}