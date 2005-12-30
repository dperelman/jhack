package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * @author Mr. Tenda
 * 
 * TODO Write javadoc for this class
 */
public class EnemyPlacementGroupsEditor extends EbHackModule implements
		ActionListener, DocumentListener {
	/**
	 * @param rom
	 * @param prefs
	 */
	public EnemyPlacementGroupsEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}

	private static final int enemyGroupsPtrsAddress = 0x10ba80;

	public static final int ENEMY_GROUPS_COUNT = 0xcb;

	private static EnemyPlGroup[] enemyPlGroups = new EnemyPlGroup[ENEMY_GROUPS_COUNT];

	private JComboBox entryChooser;

	private JTextField flag;

	private EnemyPlGroupPanel group1, group2;

	private static boolean wasWrittenContiguously;

	private static int oldStartAddress, oldLength;

	private static int[][] oldEntryLocs = new int[ENEMY_GROUPS_COUNT][2];
	
	private int tmp = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#init()
	 */
	protected void init() {
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		entryChooser = new JComboBox();
		for (int i = 0; i < ENEMY_GROUPS_COUNT; i++)
			entryChooser.addItem(getNumberedString("Entry 0x"
					+ Integer.toHexString(i), i, true));
		entryChooser.addActionListener(this);

		flag = HackModule.createSizedJTextField(3, true, true);
		flag.getDocument().addDocumentListener(this);

		mainPanel
				.add(HackModule
						.pairComponents(
								entryChooser,
								HackModule
										.getLabeledComponent(
												"Flag (if set, subgroup 2 is enabled over subgroup 1):",
												flag), true));

		JPanel tmp = new JPanel(new FlowLayout());
		tmp
				.add(new JLabel(
						"(Encounter rates range from 0x0 to 0xff,"
								+ " with 0xff being the most likely and 0x0 disabling the subgroup.)"));
		mainPanel.add(tmp);

		group1 = new EnemyPlGroupPanel();
		group1.setLayout(new BoxLayout(group1, BoxLayout.Y_AXIS));
		group2 = new EnemyPlGroupPanel();
		group2.setLayout(new BoxLayout(group2, BoxLayout.Y_AXIS));
		mainPanel.add(HackModule.pairComponents(group1, group2, true));

		mainWindow.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainWindow.pack();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#getVersion()
	 */
	public String getVersion() {
		return "0.1";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.starmen.pkhack.HackModule#getDescription()
	 */
	public String getDescription() {
		return "Enemy Placement Groups Editor";
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
		readFromRom(rom);
		EnemyEditor.readFromRom(this);
		BattleEntryEditor.readFromRom(rom);

		super.show();

		mainWindow.setVisible(true);
		if (entryChooser.getSelectedIndex() < 0)
			entryChooser.setSelectedIndex(0);
		else
			entryChooser.setSelectedIndex(entryChooser.getSelectedIndex());
	}

	public void show(Object obj) {
		readFromRom(rom);
		EnemyEditor.readFromRom(this);
		BattleEntryEditor.readFromRom(rom);

		super.show();

		mainWindow.setVisible(true);

		if (obj instanceof Integer)
			entryChooser.setSelectedIndex(((Integer) obj).intValue());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("apply")) {
			writeToRom(this, mainWindow);
			group1.refresh();
			group2.refresh();
		} else if (e.getActionCommand().equals("close"))
			hide();
		else if (e.getSource().equals(entryChooser)) {
			flag.getDocument().removeDocumentListener(this);
			flag.setText(Integer.toHexString(enemyPlGroups[entryChooser
					.getSelectedIndex()].getFlag() & 0xfff));
			flag.getDocument().addDocumentListener(this);
			group1.setEnemyPlGroup(enemyPlGroups[entryChooser
					.getSelectedIndex()], 0);
			group2.setEnemyPlGroup(enemyPlGroups[entryChooser
					.getSelectedIndex()], 1);
			mainWindow.pack();
		}
	}
	
	public static void readFromRom(AbstractRom rom) {
		int sum, k, addr = 0, newAddr;
		wasWrittenContiguously = true;
		oldLength = 0;
		for (int i = 0; i < enemyPlGroups.length; i++) {
			rom.seek(newAddr = toRegPointer(rom.readMulti(
					enemyGroupsPtrsAddress + 4 * i, 4)));
			oldEntryLocs[i][0] = newAddr;
			oldEntryLocs[i][1] = 4;

			if (i == 0)
				oldStartAddress = newAddr;
			else if (wasWrittenContiguously && (addr != newAddr))
				wasWrittenContiguously = false;

			enemyPlGroups[i] = new EnemyPlGroup((short) rom.readMultiSeek(2),
					rom.readByteSeek(), rom.readByteSeek());
			addr = newAddr + 4;
			oldLength += 4;
			for (int j = 0; j < 2; j++)
				if (enemyPlGroups[i].getEncounterRate(j) > 0) {
					sum = 0;
					k = 0;
					while (sum < 8) {
						enemyPlGroups[i].getEntry(j, k).setProbability(
								rom.readByteSeek());
						enemyPlGroups[i].getEntry(j, k).setEnemy(
								(short) rom.readMultiSeek(2));
						sum += enemyPlGroups[i].getEntry(j, k).getProbability();
						k++;
						oldEntryLocs[i][1] += 3;
						oldLength += 3;
						addr += 3;
					}
				}
		}
	}
	
	public static EnemyPlGroup getEnemyPlGroup(int num)
	{
		return enemyPlGroups[num];
	}

	public static void writeToRom(HackModule hm, JFrame mainWindow) {
		byte[] data;
		int size = 0, sum, startFrom;
		boolean autoCorrect;
		for (int i = 0; i < enemyPlGroups.length; i++) {
			size += enemyPlGroups[i].getSize();

			for (int j = 0; j < 2; j++)
				if (enemyPlGroups[i].getEncounterRate(j) > 0) {
					sum = 0;
					autoCorrect = false;
					for (int k = 0; k < 8; k++) {
						sum += enemyPlGroups[i].getEntry(j, k).getProbability();
						if (autoCorrect)
							enemyPlGroups[i].getEntry(j, k).setProbability(
									(byte) 0);
						else if (sum > 8) {
							int sure = JOptionPane
									.showConfirmDialog(
											mainWindow,
											"Entry 0x"
													+ Integer.toHexString(i)
													+ " (subgroup "
													+ j
													+ ") "
													+ " has a total probability over 8.\n"
													+ "Should I try to auto-correct this?",
											"Invalid Probability Total",
											JOptionPane.YES_NO_OPTION);
							if (sure == JOptionPane.YES_OPTION) {
								autoCorrect = true;
								enemyPlGroups[i].getEntry(j, k).setProbability(
										(byte) (enemyPlGroups[i].getEntry(j, k)
												.getProbability() - (sum - 8)));
							} else {
								JOptionPane.showMessageDialog(mainWindow,
										"Changes were not saved.");
								return;
							}
						}
					}

					if (sum < 8) {
						int sure = JOptionPane
								.showConfirmDialog(
										mainWindow,
										"Entry 0x"
												+ Integer.toHexString(i)
												+ " (subgroup "
												+ j
												+ ") "
												+ " has a total probability less than 8.\n"
												+ "Should I try to auto-correct this?",
										"Invalid Probability Total",
										JOptionPane.YES_NO_OPTION);
						if (sure == JOptionPane.YES_OPTION)
							enemyPlGroups[i].getEntry(j, 0).setProbability(
									(byte) (8 - sum + enemyPlGroups[i]
											.getEntry(j, 0).getProbability()));
						else {
							JOptionPane.showMessageDialog(mainWindow,
									"Changes were not saved.");
							return;
						}
					}
				}
		}

		if (wasWrittenContiguously && (size <= oldLength)) {
			startFrom = oldStartAddress;
			for (int i = 0; i < enemyPlGroups.length; i++) {
				hm.rom.write(enemyGroupsPtrsAddress + 4 * i,
						toSnesPointer(startFrom), 4);
				hm.rom.write(startFrom, data = enemyPlGroups[i].toByteArray());
				startFrom += data.length;
			}
		} else {
			if (!wasWrittenContiguously)
				for (int i = 0; i < oldEntryLocs.length; i++)
					hm.nullifyArea(oldEntryLocs[i][0], oldEntryLocs[i][1]);
			else
				hm.nullifyArea(oldStartAddress, oldLength);
			data = new byte[size];
			startFrom = 0;
			byte[] entryData;
			for (int i = 1; i < enemyPlGroups.length; i++) {
				hm.rom.write(enemyGroupsPtrsAddress + 4 * i, startFrom, 4);
				System.arraycopy(entryData = enemyPlGroups[i].toByteArray(), 0,
						data, startFrom, entryData.length);
				startFrom += entryData.length;
			}

			hm.writetoFree(data, enemyGroupsPtrsAddress, 4, 0, data.length,
					true);
			startFrom = hm.rom.readMulti(enemyGroupsPtrsAddress, 4);
			for (int i = 1; i < enemyPlGroups.length; i++)
				hm.rom.write(enemyGroupsPtrsAddress + 4 * i,
						toSnesPointer(hm.rom.readMulti(enemyGroupsPtrsAddress
								+ 4 * i, 4)
								+ startFrom), 4);
		}
	}

	public class EnemyPlGroupPanel extends JPanel implements ActionListener,
			DocumentListener {
		private EnemyPlGroup group;

		private JLabel title;

		private JTextField eRate;

		private int subgroup;

		private EnemyPlGroupEntryPanel[] entryPanels = new EnemyPlGroupEntryPanel[8];

		public EnemyPlGroupPanel() {
			super();
			add(title = new JLabel());
			add(HackModule.getLabeledComponent("Encounter Rate:",
					eRate = HackModule.createSizedJTextField(2, true, true)));
			eRate.getDocument().addDocumentListener(this);
			for (int i = 0; i < entryPanels.length; i++) {
				entryPanels[i] = new EnemyPlGroupEntryPanel(this, i);
				add(HackModule.pairComponents(entryPanels[i].getEnemyChooser(),
						entryPanels[i].getProbMeter(), true));
			}
		}

		public void setEnemyPlGroup(EnemyPlGroup group, int subgroup) {
			this.group = group;
			this.subgroup = subgroup;
			title.setText("Subgroup " + (subgroup + 1));
			eRate.getDocument().removeDocumentListener(this);
			eRate.setText(Integer
					.toHexString(group.getEncounterRate(subgroup) & 0xff));
			eRate.getDocument().addDocumentListener(this);
			for (int i = 0; i < entryPanels.length; i++)
				entryPanels[i].setEntry(group.getEntry(subgroup, i));
		}

		public void refresh() {
			for (int i = 0; i < entryPanels.length; i++) {
				entryPanels[i].setEntry(group.getEntry(subgroup, i));
				entryPanels[i].repaint();
			}
		}

		public class EnemyPlGroupEntryPanel {
			private EnemyPlGroupPanel parent;

			// private int groupNum;

			// private EnemyPlGroup.EnemyPlGroupEntry entry;

			private JComboBox enemy = new JComboBox();

			private ProbabilityMeter probMeter = new ProbabilityMeter(8);

			public EnemyPlGroupEntryPanel(EnemyPlGroupPanel parent, int groupNum) {
				this.parent = parent;
				// this.groupNum = groupNum;
				enemy.addActionListener(parent);
				enemy.setActionCommand("E" + Integer.toString(groupNum));
				probMeter.addActionListener(parent);
				probMeter.setActionCommand("P" + Integer.toString(groupNum));
				BattleEntryEditor.reloadEntryNames(enemy, parent);
			}

			public void unsetEntry() {
				// this.entry = null;
				enemy.setSelectedIndex(0);
				enemy.setEnabled(false);
				probMeter.setStatus(0);
			}

			public void setEntry(EnemyPlGroup.EnemyPlGroupEntry entry) {
				// this.entry = entry;
				enemy.removeActionListener(parent);
				enemy.setSelectedIndex(entry.getEnemy() & 0xffff);
				enemy.addActionListener(parent);
				enemy.setEnabled(entry.getProbability() > 0);
				probMeter.setStatus(entry.getProbability());
			}

			public JComboBox getEnemyChooser() {
				return enemy;
			}

			public ProbabilityMeter getProbMeter() {
				return probMeter;
			}

			public void repaint() {
				enemy.repaint();
				probMeter.repaint();
			}
		}

		public void actionPerformed(ActionEvent e) {
			String ac = e.getActionCommand();
			if (ac.substring(0, 1).equals("E"))
				group.getEntry(subgroup, Integer.parseInt(ac.substring(1, 2)))
						.setEnemy(
								(short) ((JComboBox) e.getSource())
										.getSelectedIndex());
			else if (ac.substring(0, 1).equals("P")) {
				group.getEntry(subgroup, Integer.parseInt(ac.substring(1, 2)))
						.setProbability(
								(byte) ((ProbabilityMeter) e.getSource())
										.getStatus());
				entryPanels[Integer.parseInt(ac.substring(1, 2))]
						.getEnemyChooser()
						.setEnabled(
								((ProbabilityMeter) e.getSource()).getStatus() > 0);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
		 */
		public void changedUpdate(DocumentEvent e) {
			if (eRate.getText().length() > 0)
				group.setEncounterRate(subgroup, (byte) (Integer.parseInt(eRate
						.getText(), 16) & 0xff));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
		 */
		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
		 */
		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}

	public static class ProbabilityMeter extends AbstractButton implements
			MouseListener {
		private int max, status;

		private String actionCommand;

		public ProbabilityMeter(int max) {
			this.max = max;
			setPreferredSize(new Dimension(5 * max + 2, 9));
			addMouseListener(this);
		}

		public void setStatus(int status) {
			this.status = status;
			repaint();
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			g2d.drawRect(0, 0, 5 * max + 1, 9);
			for (int i = 0; i < status; i++)
				g2d.fill(new Rectangle2D.Double(i * 5 + 2, 2, 3, 6));
		}

		public int getStatus() {
			return status;
		}

		private void changeStatus(int amount) {
			status += amount;
			if (status < 0)
				status = 0;
			else if (status > max)
				status = max;
		}

		/**
		 * @see javax.swing.AbstractButton#getActionCommand()
		 */
		public String getActionCommand() {
			return this.actionCommand;
		}

		/**
		 * @see javax.swing.AbstractButton#setActionCommand(String)
		 */
		public void setActionCommand(String arg0) {
			this.actionCommand = arg0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1)
				if (e.isControlDown())
					changeStatus(-1);
				else
					changeStatus(1);
			else if (e.getButton() == MouseEvent.BUTTON3)
				if (e.isControlDown())
					changeStatus(1);
				else
					changeStatus(-1);
			else
				return;
			this.fireActionPerformed(new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
			repaint();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent e) {
		}
	}

	public static class EnemyPlGroup {
		private short flag;

		private byte eRate1, eRate2;

		private EnemyPlGroupEntry[][] subgroups = new EnemyPlGroupEntry[2][8];

		public EnemyPlGroup(short flag, byte eRate1, byte eRate2) {
			this.flag = flag;
			this.eRate1 = eRate1;
			this.eRate2 = eRate2;
			for (int i = 0; i < subgroups.length; i++)
				for (int j = 0; j < subgroups[i].length; j++)
					subgroups[i][j] = new EnemyPlGroupEntry((short) 0, (byte) 0);
		}

		public short getFlag() {
			return flag;
		}

		public void setFlag(short flag) {
			this.flag = flag;
		}

		public byte getEncounterRate(int num) {
			if (num == 0)
				return eRate1;
			else
				return eRate2;
		}

		public void setEncounterRate(int num, byte eRate) {
			if (num == 0)
				eRate1 = eRate;
			else
				eRate2 = eRate;
		}

		public int getSize() {
			int size = 4;
			if (eRate1 > 0)
				for (int i = 0; i < 8; i++)
					if (subgroups[0][i].getProbability() > 0)
						size += 3;
			if (eRate2 > 0)
				for (int i = 0; i < 8; i++)
					if (subgroups[1][i].getProbability() > 0)
						size += 3;
			return size;
		}

		public EnemyPlGroupEntry getEntry(int subgroup, int num) {
			return subgroups[subgroup][num];
		}

		public byte[] toByteArray() {
			byte[] out = new byte[getSize()];
			out[0] = (byte) (flag & 0xff);
			out[1] = (byte) ((flag & 0xff00) >> 2);
			out[2] = eRate1;
			out[3] = eRate2;
			int startFrom = 4;
			for (int i = 0; i < 2; i++)
				if ((i == 0 ? eRate1 : eRate2) > 0)
					for (int j = 0; j < 8; j++)
						if (subgroups[i][j].getProbability() > 0) {
							out[startFrom] = subgroups[i][j].getProbability();
							out[startFrom + 1] = (byte) (subgroups[i][j]
									.getEnemy() & 0xff);
							out[startFrom + 2] = (byte) ((subgroups[i][j]
									.getEnemy() & 0xff00) >> 2);
							startFrom += 3;
						}

			return out;
		}

		public static class EnemyPlGroupEntry {
			private short enemy;

			private byte probability;

			public EnemyPlGroupEntry(short enemy, byte probability) {
				this.enemy = enemy;
				this.probability = probability;
			}

			public byte getProbability() {
				return probability;
			}

			public void setProbability(byte probability) {
				this.probability = probability;
			}

			public short getEnemy() {
				return enemy;
			}

			public void setEnemy(short enemy) {
				this.enemy = enemy;
			}
			
			public ArrayList getGroupEntry() {
				return BattleEntryEditor.getGroup(
						BattleEntryEditor.getBattleEntry(enemy).getEnemyGroup());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent e) {
		if (flag.getText().length() > 0)
			enemyPlGroups[entryChooser.getSelectedIndex()]
					.setFlag((short) Integer.parseInt(flag.getText(), 16));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}
}