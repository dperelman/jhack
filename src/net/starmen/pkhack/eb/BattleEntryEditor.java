package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.util.Arrays;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * @author Mr. Tenda
 *
 * TODO Write javadoc for this class
 */
public class BattleEntryEditor extends EbHackModule implements ActionListener
{
	public BattleEntryEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
	public static final int NUM_ENTRIES = 484;
	public static final int POINTERS = 0x10c80d;
	private static final String DEL_ENEMY_COMMAND = "DNMY";
	
	private static BattleEntry[] battleEntries;
	private static ArrayList enemyGroups;
	private static ArrayList oldGroupLengths;
	
	private JTextField flag;
	private JComboBox selector, groupSelector, boxSize, flagEffect;
	private JButton addGroup, delGroup, addEnemy;
	private JPanel groupEditPanel;
	
	private JComboBox[] enemies = new JComboBox[0];
	private JTextField[] amounts = new JTextField[0];
	private boolean[] deletedEnemies;
	private ArrayList addedEnemies;

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init()
	{
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		selector = new JComboBox();
		selector.addActionListener(this);
		groupSelector = new JComboBox();
		groupSelector.addActionListener(this);
		flag = HackModule.createSizedJTextField(3, true, true);
		flagEffect = new JComboBox(new String[] {
				"Unset", "Set"
		});
		boxSize = new JComboBox(new String[] {
				"None", "Large", "Medium", "Small"
		});
		addGroup = new JButton("Add Group");
		addGroup.addActionListener(this);
		delGroup = new JButton("Delete Group");
		delGroup.addActionListener(this);
		
		groupEditPanel = new JPanel();
		groupEditPanel.setLayout(
				new BoxLayout(groupEditPanel, BoxLayout.Y_AXIS));
		
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
		topPanel.setLayout(
				new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.add(selector);
		topPanel.add(panel);
		topPanel.add(groupSelector);
		topPanel.add(HackModule.pairComponents(addGroup, delGroup, true));
		topPanel.add(new JLabel("Enemy Group contents:"));
		topPanel.add(groupEditPanel);
		topPanel.add(addEnemy);
		
		mainWindow.getContentPane().add(
				topPanel, BorderLayout.CENTER);
		
		mainWindow.pack();
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion()
	{
		return "0.1";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription()
	{
		return "Battle Entry Editor";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by Mr. Tenda";
	}

	/* (non-Javadoc)
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{
		mainWindow.setVisible(false);
	}
	
	public void show()
	{
		super.show();
		EnemyEditor.readFromRom(this);
		readFromRom(rom);
		reloadEntryNames();
		reloadGroupNames();
		updateComponents();
		mainWindow.setVisible(true);
	}
	
	private void reloadEntryNames()
	{
		selector.removeActionListener(this);
		int selected = selector.getSelectedIndex();
		if (selected < 0)
			selected = 0;
		selector.removeAllItems();
		for (int i = 0; i < NUM_ENTRIES; i++)
			selector.addItem("0x" + i + " - " + 
				getGroupName(battleEntries[i].getEnemyGroup()));
		selector.setSelectedIndex(selected);
		selector.addActionListener(this);
	}
	
	private void reloadGroupNames()
	{
		groupSelector.removeActionListener(this);
		int selected = groupSelector.getSelectedIndex();
		groupSelector.removeAllItems();
		for (int i = 0; i < enemyGroups.size(); i++)
			groupSelector.addItem(getGroupName(i));
		groupSelector.setSelectedIndex(selected);
		groupSelector.addActionListener(this);
	}
	
	private void updateComponents()
	{
		BattleEntry entry = battleEntries[selector.getSelectedIndex()];
		
		flag.setText(Integer.toHexString(entry.getFlag() & 0xffff));
		flagEffect.setSelectedIndex(entry.getFlagUsage() ? 1 : 0);
		boxSize.setSelectedIndex(entry.getBoxSize() & 0xff);
		groupSelector.setSelectedIndex(entry.getEnemyGroup());
	}
	
	private void updateGroupDisplay(boolean reset)
	{
		ArrayList group = (ArrayList) enemyGroups.get(groupSelector.getSelectedIndex());
		int numDeleted = 0;
		if (reset)
		{
			enemies = new JComboBox[0];
			amounts = new JTextField[0];
			addedEnemies = new ArrayList();
			deletedEnemies = new boolean[group.size()];
			Arrays.fill(deletedEnemies, false);
		}
		else
			for (int i = 0; i < deletedEnemies.length; i++)
				if (deletedEnemies[i])
					numDeleted++;
		
		groupEditPanel.removeAll();
		if ((group.size() - numDeleted == 0) && (addedEnemies.size() == 0))
			groupEditPanel.add(new JLabel("No enemies in this group."));
		else
		{
			JComboBox[] enemies = new JComboBox[group.size() + addedEnemies.size()];
			JTextField[] amounts = new JTextField[group.size() + addedEnemies.size()];
			
			String[] enemyNames = new String[EnemyEditor.enemies.length];
			for (int i = 0; i < enemyNames.length; i++)
				enemyNames[i] = EnemyEditor.enemies[i].getName();
			for (int i = 0; i < group.size() + addedEnemies.size(); i++)
			{
				boolean real = i < group.size();
				if (!real || !deletedEnemies[i])
				{
					EnemyEntry enemy;
					if (real)
						enemy = (EnemyEntry) group.get(i);
					else
						enemy = (EnemyEntry) addedEnemies.get(i - group.size());
					JButton delButton = new JButton("Delete");
					delButton.setActionCommand(DEL_ENEMY_COMMAND + Integer.toString(i));
					delButton.addActionListener(this);
					
					int oldEnemy;
					String amountText;
					if ((i < this.enemies.length) && (i < this.amounts.length))
					{
						oldEnemy = this.enemies[i].getSelectedIndex();
						amountText = this.amounts[i].getText();
					}
					else
					{
						oldEnemy = enemy.getEnemy();
						amountText = Integer.toString(enemy.getAmount() & 0xff);
					}
					
					enemies[i] = new JComboBox(enemyNames);
					enemies[i].setSelectedIndex(oldEnemy);
					enemies[i].addActionListener(this);
					
					amounts[i] = HackModule.createSizedJTextField(3, true, false);
					amounts[i].setText(amountText);
					
					JPanel row = new JPanel();
					row.add(delButton);
					row.add(new JLabel("Enemy: "));
					row.add(enemies[i]);
					row.add(new JLabel("Amount: "));
					row.add(amounts[i]);
					groupEditPanel.add(row);
				}
			}
			this.enemies = enemies;
			this.amounts = amounts;
		}
		
		mainWindow.pack();
	}
	
	public static String getGroupName(int groupNum)
	{
		ArrayList group = (ArrayList) enemyGroups.get(groupNum);
		if (group.size() == 0)
			return "Empty";
		else if (group.size() == 1)
			return ((EnemyEntry) group.get(0)).getName();
		else
		{
			String name = "";
			for (int i = 0; i < group.size(); i++)
			{
				if (i > 0)
					name = name + ", ";
				if (i + 1 == group.size())
					name = name + "and ";
				name = name + ((EnemyEntry) group.get(i)).getName();
			}
			return name;
		}
	}
	
	public static void readFromRom(AbstractRom rom)
	{
		ArrayList groupAddresses = new ArrayList();
		
		battleEntries = new BattleEntry[NUM_ENTRIES];
		enemyGroups = new ArrayList();
		oldGroupLengths = new ArrayList();
		
		for (int i = 0; i < battleEntries.length; i++)
		{
			int groupNum, pointer = toRegPointer(rom.readMulti(POINTERS + (8 * i), 4));
			if (! groupAddresses.contains(new Integer(pointer)))
			{
				ArrayList group = new ArrayList();
				int num = 0;
				boolean done = false;
				while (!done)
				{
					if (rom.read(pointer + (num * 3)) == 0xff)
						done = true;
					else
					{
						group.add(new EnemyEntry(
								rom.readByte(pointer + (num * 3)),
								(short) rom.readMulti(pointer + (num * 3) + 1, 2)));
						num++;
					}
				}
				enemyGroups.add(group);
				oldGroupLengths.add(new LengthRecord(pointer, num));
				groupNum = enemyGroups.indexOf(group);
				groupAddresses.add(new Integer(pointer));
			}
			else
				groupNum = groupAddresses.indexOf(new Integer(pointer));
			
			battleEntries[i] = new BattleEntry(
					groupNum,
					(short) rom.readMulti(POINTERS + (8 * i) + 4, 2),
					rom.readByte(POINTERS + (8 * i) + 6),
					rom.readByte(POINTERS + (8 * i) + 7));
		}
	}
	
	public static void writeToRom(HackModule hm)
	{
		int[] groupAddresses = new int[enemyGroups.size()];
		Arrays.fill(groupAddresses, -1);
		int tmp = 0;
		
		for (int i = 0; i < oldGroupLengths.size(); i++)
		{
			LengthRecord lr = (LengthRecord) oldGroupLengths.get(i);
			hm.nullifyArea(lr.address, lr.length);
		}
		oldGroupLengths = new ArrayList();
		
		byte[] allGroupData = new byte[8000];
		for (int i = 0; i < battleEntries.length; i++)
		{
			BattleEntry entry = battleEntries[i];
			
			if (groupAddresses[entry.getEnemyGroup()] == -1)
			{
				groupAddresses[entry.getEnemyGroup()] = tmp;
				ArrayList group = (ArrayList) enemyGroups.get(entry.getEnemyGroup());
				oldGroupLengths.add(new LengthRecord(tmp, group.size() * 3 + 1));
				for (int j = 0; j < group.size(); j++)
				{
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
				hm.rom.write(POINTERS + i * 8,
						toSnesPointer(((LengthRecord) oldGroupLengths.get(
								battleEntries[i].getEnemyGroup())).address),
						4);
		} catch (EOFException e) {
			System.out.println("Not enough space to save enemy groups.");
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("apply"))
       {
			battleEntries[selector.getSelectedIndex()].setEnemyGroup(
					groupSelector.getSelectedIndex());
			battleEntries[selector.getSelectedIndex()].setFlag(
					(short) (Integer.parseInt(flag.getText(), 16) & 0xffff));
			battleEntries[selector.getSelectedIndex()].setFlagUsage(
					flagEffect.getSelectedIndex() > 0);
			battleEntries[selector.getSelectedIndex()].setBoxSize(
					(byte) (boxSize.getSelectedIndex() & 0xff));
			ArrayList group = new ArrayList();
			for (int i = 0; i < enemies.length; i++)
				group.add(new EnemyEntry(
						(byte) Integer.parseInt(amounts[i].getText()),
						(short) enemies[i].getSelectedIndex()));
			enemyGroups.set(groupSelector.getSelectedIndex(), group);
			updateComponents();
			reloadEntryNames();
			reloadGroupNames();
			
			writeToRom(this);
       }    
       else if (e.getActionCommand().equals("close"))
       {
            hide();
       }
		else if (e.getSource().equals(selector))
			updateComponents();
		else if (e.getSource().equals(groupSelector))
			updateGroupDisplay(true);
		else if (e.getSource().equals(addGroup))
		{
			int index = enemyGroups.size();
			enemyGroups.add(new ArrayList());
			reloadGroupNames();
			groupSelector.setSelectedIndex(index);
		}
		else if (e.getSource().equals(delGroup))
		{
			if (enemyGroups.size() > 0)
			{
				int group = groupSelector.getSelectedIndex();
				ArrayList entriesUsing = new ArrayList();
				for (int i = 0; i < battleEntries.length; i++)
					if (battleEntries[i].getEnemyGroup() == group)
						entriesUsing.add(new Integer(i));
				String message;
				if (entriesUsing.size() > 0)
				{
					message = "WARNING: There are entries using this group,\nwhich will be" 
						+ " set to use group 0 if not changed. The entries are:\n";
					for (int i = 0; i < entriesUsing.size(); i++)
						message = message + (i > 0 || (i - 1) % 5 == 0 ? " " : "") + "0x"
							+ Integer.toHexString(((Integer) entriesUsing.get(i)).intValue())
							+ (i % 10 == 0 ? "\n" : "");
				}
				else
					message = "This are no entries using this group, so it should be OK.";
				int confirm = JOptionPane.showConfirmDialog(
						mainWindow,
						"Are you sure you want to delete this enemy group?\n" + message,
						"Are you sure?",
						JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION)
				{
					for (int i = 0; i < entriesUsing.size(); i++)
						battleEntries[((Integer) entriesUsing.get(i)).intValue()]
									  .setEnemyGroup(0);
					enemyGroups.remove(group);
					reloadGroupNames();
					groupSelector.setSelectedIndex(0);
				}
			}
			else
				JOptionPane.showMessageDialog(mainWindow,
            		    "There needs to be at least one enemy group!",
            		    "Could not obey command",
            		    JOptionPane.ERROR_MESSAGE);
		}
		else if (e.getSource().equals(addEnemy))
		{
			addedEnemies.add(new EnemyEntry((byte) 0, (short) 0));
			updateGroupDisplay(false);
		}
		else if (e.getActionCommand().substring(0, 4).equals(DEL_ENEMY_COMMAND))
		{
			int num = Integer.parseInt(e.getActionCommand().substring(4));
			ArrayList group = (ArrayList) enemyGroups.get(groupSelector.getSelectedIndex());
			int numDeleted = 0;
			for (int i = 0; i < deletedEnemies.length; i++)
				if (deletedEnemies[i])
					numDeleted++;
			if (num >= group.size() - numDeleted)
				addedEnemies.remove(num - group.size());
			else
				deletedEnemies[num] = true;
			JComboBox[] enemies = new JComboBox[this.enemies.length - 1];
			JTextField[] amounts = new JTextField[this.amounts.length - 1];
			boolean passedDeleted = false;
			for (int i = 0; i < this.enemies.length; i++)
				if (i != num)
				{
					enemies[i - (passedDeleted ? 1 : 0)] = this.enemies[i];
					amounts[i - (passedDeleted ? 1 : 0)] = this.amounts[i];
				}
				else
					passedDeleted = true;
			this.enemies = enemies;
			this.amounts = amounts;
			updateGroupDisplay(false);
		}
	}
	
	public static class BattleEntry
	{
		private int enemyGroup;
		private short flag;
		private boolean flagUsage; // false = run away when flag is unset, true = run away when flag set
		private byte boxSize;
		
		public BattleEntry(int enemyGroup, short flag, byte flagUsage, byte boxSize)
		{
			this.enemyGroup = enemyGroup;
			this.flag = flag;
			this.flagUsage = flagUsage > 0;
			this.boxSize = boxSize;
		}
		
		public int getEnemyGroup()
		{
			return enemyGroup;
		}
		
		public void setEnemyGroup(int enemyGroup)
		{
			this.enemyGroup = enemyGroup;
		}
		
		public short getFlag()
		{
			return flag;
		}
		
		public void setFlag(short flag)
		{
			this.flag = flag;
		}
		
		public boolean getFlagUsage()
		{
			return flagUsage;
		}
		
		public byte getFlagUsageByte()
		{
			return (byte) (flagUsage ? 1 : 0);
		}
		
		public void setFlagUsage(boolean flagUsage)
		{
			this.flagUsage = flagUsage;
		}
		
		public byte getBoxSize()
		{
			return boxSize;
		}
		
		public void setBoxSize(byte boxSize)
		{
			this.boxSize = boxSize;
		}
	}
	
	public static class EnemyEntry
	{
		private byte amount;
		private short enemy;
		
		public EnemyEntry(byte amount, short enemy)
		{
			this.amount = amount;
			this.enemy = enemy;
		}
		
		public short getEnemy()
		{
			return enemy;
		}
		
		public byte getAmount()
		{
			return amount;
		}
		
		public String getName()
		{
			return Integer.toString(amount & 0xff) + " "
				+ EnemyEditor.enemies[enemy].getName();
		}
		
		public byte[] toByteArray()
		{
			byte[] out = new byte[3];
			out[0] = amount;
			out[1] = (byte) (enemy & 0xff);
			out[2] = (byte) ((enemy & 0xff00) / 0x100);
			return out;
		}
	}
	
	public static class LengthRecord
	{
		public int address, length;
		
		public LengthRecord(int address, int length)
		{
			this.address = address;
			this.length = length;
		}
	}
}