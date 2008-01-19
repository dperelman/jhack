package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * @author Mr. Tenda
 * 
 * TODO Write javadoc for this class
 */
public class BattleEntryEditor extends EbHackModule implements ActionListener {
	public BattleEntryEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}

	public static final int NUM_ENTRIES = 484;
	public static final int POINTERS = 0x10c80d;
	private static final String DEL_ENEMY_COMMAND = "DNMY";
	private static final String CHG_ENEMY_COMMAND = "CNMY";

	private static BattleEntry[] battleEntries;
	private static ArrayList enemyGroups;
	private static ArrayList oldGroupLengths;
	private static boolean useGameOrder;
	private JTextField flag;
	private JComboBox selector, boxSize, flagEffect;
	private JButton addEnemy;
	private JPanel groupEditPanel;
	private JRadioButton ordGame, ordReg;
	private JComboBox[] enemies = new JComboBox[0];
	private JTextField[] amounts = new JTextField[0];

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init() {
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());

		ButtonGroup ordBG = new ButtonGroup();
		ordGame = new JRadioButton("Game Order");
		ordGame.setSelected(useGameOrder);
		ordGame.addActionListener(this);
		ordBG.add(ordGame);
		ordReg = new JRadioButton("ROM Order");
		ordReg.setSelected(!useGameOrder);
		ordReg.addActionListener(this);
		ordBG.add(ordReg);

		selector = new JComboBox();
		selector.addActionListener(this);
		flag = HackModule.createSizedJTextField(3, true, true);
		flagEffect = new JComboBox(new String[] { "Unset", "Set" });
		boxSize = new JComboBox(new String[] { "None", "Large", "Medium",
				"Small" });

		groupEditPanel = new JPanel();
		groupEditPanel
				.setLayout(new BoxLayout(groupEditPanel, BoxLayout.Y_AXIS));

		addEnemy = new JButton("Add Enemy");
		addEnemy.addActionListener(this);

		JPanel panel = new JPanel();
		panel.add(new JLabel("Flag: "));
		panel.add(flag);
		panel.add(new JLabel("Run away while flag is "));
		panel.add(flagEffect);
		panel.add(new JLabel("Battle Letterbox Size: "));
		panel.add(boxSize);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.add(selector);
		topPanel.add(panel);
		topPanel.add(new JLabel("Enemy Group contents:"));
		topPanel.add(createFlowLayout(new JRadioButton[] { ordGame, ordReg }));
		topPanel.add(groupEditPanel);
		topPanel.add(addEnemy);

		mainWindow.getContentPane().add(topPanel, BorderLayout.CENTER);

		mainWindow.pack();
	}

	public static SimpleComboBoxModel createEnemyComboBoxModel() {
		SimpleComboBoxModel out = new SimpleComboBoxModel() {
			public int getSize() {
				return EnemyEditor.enemies.length;
			}

			public Object getElementAt(int i) {
				String out;
				int j = useGameOrder ? EnemyEditor.gameOrder[i] : i;
				try {
					out = EnemyEditor.enemies[j].toString();
				} catch (NullPointerException e) {
					out = "Null";
				} catch (ArrayIndexOutOfBoundsException e) {
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

	protected static void addEnemyDataListener(ListDataListener ldl) {
		enemyListeners.add(ldl);
	}

	protected static void removeEnemyDataListener(ListDataListener ldl) {
		enemyListeners.remove(ldl);
	}

	protected static void notifyEnemyDataListeners(ListDataEvent lde) {
		for (Iterator i = enemyListeners.iterator(); i.hasNext();) {
			((ListDataListener) i.next()).contentsChanged(lde);
		}
	}

	public static JComboBox createEnemyComboBox(final ActionListener al, int num) {
		SimpleComboBoxModel model = createEnemyComboBoxModel();
		final JComboBox out = new JComboBox(model);
		out.setActionCommand(CHG_ENEMY_COMMAND + Integer.toString(num));
		model.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent lde) {
				if (out.getSelectedIndex() == -1) {
					System.out.println("a");
					out.removeActionListener(al);
					out.setSelectedIndex(lde.getIndex0());
					out.addActionListener(al);
				}
			}

			public void intervalAdded(ListDataEvent arg0) {
			}

			public void intervalRemoved(ListDataEvent arg0) {
			}
		});
		out.addActionListener(al);

		return out;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion() {
		return "0.3";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription() {
		return "Battle Entry Editor";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits() {
		return "Written by Mr. Tenda";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide() {
		mainWindow.setVisible(false);
	}

	public void show() {
		super.show();
		EnemyEditor.readFromRom(this);
		readFromRom(rom);
		reloadEntryNames(selector, this);
		updateComponents();
		mainWindow.setVisible(true);
	}

	public static void reloadEntryNames(JComboBox selector, ActionListener ac) {
		if (ac != null)
			selector.removeActionListener(ac);
		int selected = selector.getSelectedIndex();
		if (selected < 0)
			selected = 0;
		selector.removeAllItems();
		for (int i = 0; i < NUM_ENTRIES; i++)
			selector.addItem(getNumberedString(getGroupName(battleEntries[i]
					.getEnemyGroup()), i));
		selector.setSelectedIndex(selected);
		if (ac != null)
			selector.addActionListener(ac);
	}

	public static void reloadEntryNames(JComboBox selector) {
		reloadEntryNames(selector, null);
	}

	private void updateComponents() {
		BattleEntry entry = battleEntries[selector.getSelectedIndex()];

		flag.setText(Integer.toHexString(entry.getFlag() & 0xffff));
		flagEffect.setSelectedIndex(entry.getFlagUsage() ? 1 : 0);
		boxSize.setSelectedIndex(entry.getBoxSize() & 0xff);
		updateGroupDisplay();
	}

	private void updateGroupDisplay() {
		ArrayList group = (ArrayList) enemyGroups.get(selector.getSelectedIndex());

		groupEditPanel.removeAll();
		if (group.size() == 0)
			groupEditPanel.add(new JLabel("No enemies in this group."));
		else {
			enemies = new JComboBox[group.size()];
			amounts = new JTextField[group.size()];

			/*
			 * String[] enemyNames = new String[EnemyEditor.enemies.length]; for
			 * (int i = 0; i < enemyNames.length; i++) enemyNames[i] =
			 * EnemyEditor.enemies[i].getName();
			 */
			for (int i = 0; i < group.size(); i++) {
					JButton delButton = new JButton("Delete");
					delButton.setActionCommand(DEL_ENEMY_COMMAND
							+ Integer.toString(i));
					delButton.addActionListener(this);

					enemies[i] = createEnemyComboBox(this,i);
					enemies[i].removeActionListener(this);
					enemies[i].setSelectedIndex(((EnemyEntry) group.get(i)).getEnemy());
					enemies[i].addActionListener(this);
					
					amounts[i] = HackModule.createSizedJTextField(3, true,false);
					amounts[i].setText(""+((EnemyEntry) group.get(i)).getAmount());
					amounts[i].getDocument().addDocumentListener(new AmountListener(i));

					JPanel row = new JPanel();
					row.add(delButton);
					row.add(new JLabel("Enemy: "));
					row.add(enemies[i]);
					row.add(new JLabel("Amount: "));
					row.add(amounts[i]);
					groupEditPanel.add(row);
			}
		}

		mainWindow.pack();
	}
	
	public static ArrayList getGroup(int groupNum) {
		return (ArrayList) enemyGroups.get(groupNum);
	}
	
	public static BattleEntry getBattleEntry(int beNum) {
		return battleEntries[beNum];
	}

	public static String getGroupName(int groupNum) {
		ArrayList group = (ArrayList) enemyGroups.get(groupNum);
		if (group.size() == 0)
			return "Empty";
		else if (group.size() == 1)
			return ((EnemyEntry) group.get(0)).getName();
		else {
			String name = "";
			for (int i = 0; i < group.size(); i++) {
				if (i > 0)
					name = name + ", ";
				if (i + 1 == group.size())
					name = name + "and ";
				name = name + ((EnemyEntry) group.get(i)).getName();
			}
			return name;
		}
	}

	public static void readFromRom(AbstractRom rom) {
		ArrayList groupAddresses = new ArrayList();

		battleEntries = new BattleEntry[NUM_ENTRIES];
		enemyGroups = new ArrayList();
		oldGroupLengths = new ArrayList();

		for (int i = 0; i < battleEntries.length; i++) {
			int groupNum, pointer = toRegPointer(rom.readMulti(POINTERS
					+ (8 * i), 4));
			if (!groupAddresses.contains(new Integer(pointer))) {
				ArrayList group = new ArrayList();
				int num = 0;
				boolean done = false;
				while (!done) {
					if (rom.read(pointer + (num * 3)) == 0xff)
						done = true;
					else {
						group.add(new EnemyEntry(rom.readByte(pointer
								+ (num * 3)), (short) rom.readMulti(pointer
								+ (num * 3) + 1, 2)));
						num++;
					}
				}
				enemyGroups.add(group);
				oldGroupLengths.add(new LengthRecord(pointer, num));
				groupNum = enemyGroups.indexOf(group);
				groupAddresses.add(new Integer(pointer));
			} else
				groupNum = groupAddresses.indexOf(new Integer(pointer));

			battleEntries[i] = new BattleEntry(groupNum, (short) rom.readMulti(
					POINTERS + (8 * i) + 4, 2), rom.readByte(POINTERS + (8 * i)
					+ 6), rom.readByte(POINTERS + (8 * i) + 7));
		}
	}

	public static void writeToRom(HackModule hm) {
		int[] groupAddresses = new int[enemyGroups.size()];
		Arrays.fill(groupAddresses, -1);
		int tmp = 0;

		for (int i = 0; i < oldGroupLengths.size(); i++) {
			LengthRecord lr = (LengthRecord) oldGroupLengths.get(i);
			hm.nullifyArea(lr.address, lr.length);
		}
		oldGroupLengths = new ArrayList();

		byte[] allGroupData = new byte[8000];
		for (int i = 0; i < battleEntries.length; i++) {
			BattleEntry entry = battleEntries[i];

			if (groupAddresses[entry.getEnemyGroup()] == -1) {
				groupAddresses[entry.getEnemyGroup()] = tmp;
				ArrayList group = (ArrayList) enemyGroups.get(entry
						.getEnemyGroup());
				oldGroupLengths
						.add(new LengthRecord(tmp, group.size() * 3 + 1));
				for (int j = 0; j < group.size(); j++) {
					EnemyEntry enemy = (EnemyEntry) group.get(j);
					allGroupData[tmp] = enemy.getAmount();
					allGroupData[tmp + 1] = (byte) (enemy.getEnemy() & 0xff);
					allGroupData[tmp + 2] = (byte) ((enemy.getEnemy() & 0xff00) / 0x100);
					tmp += 3;
				}
				allGroupData[tmp] = (byte) 0xff;
				tmp++;
			}

			hm.rom.write(POINTERS + i * 8 + 4, entry.getFlag(), 2);
			hm.rom.write(POINTERS + i * 8 + 6, entry.getFlagUsageByte());
			hm.rom.write(POINTERS + i * 8 + 7, entry.getBoxSize());
		}

		hm.askExpandType();
		try {
			int address = hm.findFreeRange(hm.rom.length(), tmp);
			hm.rom.write(address, allGroupData, tmp);
			for (int i = 0; i < oldGroupLengths.size(); i++)
				((LengthRecord) oldGroupLengths.get(i)).address += address;
			for (int i = 0; i < battleEntries.length; i++)
				hm.rom
						.write(
								POINTERS + i * 8,
								toSnesPointer(((LengthRecord) oldGroupLengths
										.get(battleEntries[i].getEnemyGroup())).address),
								4);
		} catch (EOFException e) {
			System.out.println("Not enough space to save enemy groups.");
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("apply")) {
			//battleEntries[selector.getSelectedIndex()]
			//		.setEnemyGroup(groupSelector.getSelectedIndex());
			battleEntries[selector.getSelectedIndex()].setFlag((short) (Integer
					.parseInt(flag.getText(), 16) & 0xffff));
			battleEntries[selector.getSelectedIndex()].setFlagUsage(flagEffect
					.getSelectedIndex() > 0);
			battleEntries[selector.getSelectedIndex()]
					.setBoxSize((byte) (boxSize.getSelectedIndex() & 0xff));
			//(short) enemies[i].getSelectedIndex()));
			//enemyGroups.set(groupSelector.getSelectedIndex(), group);
			updateComponents();
			reloadEntryNames(selector, this);

			writeToRom(this);
		} else if (e.getActionCommand().equals("close")) {
			hide();
		} else if (e.getSource().equals(selector))
			updateComponents();
		else if (e.getSource().equals(addEnemy)) {
			ArrayList group = (ArrayList) enemyGroups.get(selector.getSelectedIndex());
			group.add(new EnemyEntry((byte) 0, (short) 0));
			
			updateGroupDisplay();
		} else if (e.getActionCommand().substring(0, DEL_ENEMY_COMMAND.length()).equals(DEL_ENEMY_COMMAND)) {
			int num = Integer.parseInt(e.getActionCommand().substring(DEL_ENEMY_COMMAND.length()));
			ArrayList group = (ArrayList) enemyGroups.get(selector.getSelectedIndex());
			group.remove(num);
			updateGroupDisplay();
		} else if (e.getActionCommand().substring(0, CHG_ENEMY_COMMAND.length()).equals(CHG_ENEMY_COMMAND)) {
			System.out.println("b");
			int num = Integer.parseInt(e.getActionCommand().substring(CHG_ENEMY_COMMAND.length()));
			ArrayList group = (ArrayList) enemyGroups.get(selector.getSelectedIndex());
			((EnemyEntry) group.get(num)).setEnemy((short) enemies[num].getSelectedIndex());
			((EnemyEntry) group.get(num)).setAmount((byte) (Integer.parseInt(amounts[num].getText()) & 0xff));
		} else if (e.getSource().equals(ordGame)) {
			useGameOrder = true;
			notifyEnemyDataListeners(new ListDataEvent(this,
					ListDataEvent.CONTENTS_CHANGED, 0, EnemyEditor.NUM_ENEMIES));
		} else if (e.getSource().equals(ordReg)) {
			useGameOrder = false;
			notifyEnemyDataListeners(new ListDataEvent(this,
					ListDataEvent.CONTENTS_CHANGED, 0, EnemyEditor.NUM_ENEMIES));
		}
	}
	
	private class AmountListener implements DocumentListener {
		int num;
		
		public AmountListener(int num) {
			this.num = num;
		}
		
		public void changedUpdate(DocumentEvent e) {
			if (amounts[num].getText().length() != 0)
				((EnemyEntry) ((ArrayList) enemyGroups.get(selector.getSelectedIndex())).get(num)).setAmount(
						(byte) (Integer.parseInt(amounts[num].getText()) & 0xff));
		}

		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}

	public static class BattleEntry {
		private int enemyGroup;

		private short flag;

		private boolean flagUsage; // false = run away when flag is unset, true

		// = run away when flag set

		private byte boxSize;

		public BattleEntry(int enemyGroup, short flag, byte flagUsage,
				byte boxSize) {
			this.enemyGroup = enemyGroup;
			this.flag = flag;
			this.flagUsage = flagUsage > 0;
			this.boxSize = boxSize;
		}

		public int getEnemyGroup() {
			return enemyGroup;
		}

		public void setEnemyGroup(int enemyGroup) {
			this.enemyGroup = enemyGroup;
		}

		public short getFlag() {
			return flag;
		}

		public void setFlag(short flag) {
			this.flag = flag;
		}

		public boolean getFlagUsage() {
			return flagUsage;
		}

		public byte getFlagUsageByte() {
			return (byte) (flagUsage ? 1 : 0);
		}

		public void setFlagUsage(boolean flagUsage) {
			this.flagUsage = flagUsage;
		}

		public byte getBoxSize() {
			return boxSize;
		}

		public void setBoxSize(byte boxSize) {
			this.boxSize = boxSize;
		}
	}

	public static class EnemyEntry {
		private byte amount;
		private short enemy;

		public EnemyEntry(byte amount, short enemy) {
			this.amount = amount;
			this.enemy = enemy;
		}

		public short getEnemy() {
			return enemy;
		}
		
		public void setEnemy(short enemy) {
			this.enemy = enemy;
		}

		public byte getAmount() {
			return amount;
		}
		
		public void setAmount(byte amount) {
			this.amount = amount;
		}

		public String getName() {
			return Integer.toString(amount & 0xff) + " "
					+ EnemyEditor.enemies[enemy].getName();
		}

		public byte[] toByteArray() {
			byte[] out = new byte[3];
			out[0] = amount;
			out[1] = (byte) (enemy & 0xff);
			out[2] = (byte) ((enemy & 0xff00) / 0x100);
			return out;
		}
	}

	public static class LengthRecord {
		public int address, length;

		public LengthRecord(int address, int length) {
			this.address = address;
			this.length = length;
		}
	}
}