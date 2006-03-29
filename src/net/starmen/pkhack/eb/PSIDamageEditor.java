/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.starmen.pkhack.eb;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * @author JeffMan
 * Edits the damage values for various PSI Powers
 */
public class PSIDamageEditor extends EbHackModule implements ActionListener
{

	/**
	 * @param rom
	 * @param prefs
	 */
	public PSIDamageEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
		
	}

	private JComboBox s;
	private int[] psidamage = new int[22];
	private int[] damageaddresses = new int[22];
	private int[] dmin = new int[22];
	private int[] dmax = new int[22];
	private int[] strikes = new int[4];
	private int[] strikeaddresses = new int[4];

	private String[] psinames = new String[22];

	private JTextField damage;
	private JLabel ranges;
	private JLabel multis;
	private JTextField strike;
	
	protected void init() {
		
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(this.getDescription());
		//mainWindow.setSize(300, 120);
	
		InitBasics();
		
		//make a JComboBox to select entry, and a JTextField to edit it
		JPanel entry = new JPanel();
		entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
		Box edit = new Box(BoxLayout.Y_AXIS);
		
		s = new JComboBox(psinames);
		s.setSelectedIndex(0);
		s.setActionCommand("change_entry");
		s.addActionListener(this);
		
		damage = createSizedJTextField(5, true);
		//damage.setActionCommand("change_damage");
        damage.getDocument().addDocumentListener(new DocumentListener()
        {
            public void change()
            {
                if (damage.getText().length() > 0)
					ranges.setText(Integer.toString((Integer.parseInt(damage.getText()) * dmin[s.getSelectedIndex()]) / 100) + " / " + Integer.toString((Integer.parseInt(damage.getText()) * dmax[s.getSelectedIndex()]) / 100));
            }

            public void changedUpdate(DocumentEvent e)
            {
                change();
            }

            public void insertUpdate(DocumentEvent e)
            {
                change();
            }

            public void removeUpdate(DocumentEvent e)
            {
                change();
            }
        });
		strike = createSizedJTextField(3, true);

		ranges = new JLabel();
		multis = new JLabel();

//		s.addItem("testing");
//		s.addItem("testing2");
//		s.addItem("testing3");
//		s.addItem("testing4");
//		s.addItem("testing5");
//		s.addItem("testing6");
//		s.setSelectedIndex(blah);
		
		edit.add(getLabeledComponent("Entry:", s));
		edit.add(getLabeledComponent("Range:", ranges));
		edit.add(getLabeledComponent("Damage:", damage));
		edit.add(getLabeledComponent("Multipliers:", multis));
		edit.add(getLabeledComponent("PSI Thunder strikes:", strike));

		mainWindow.getContentPane().add(edit, BorderLayout.CENTER);
		
		mainWindow.setResizable(false);
		mainWindow.setVisible(true);
		
		//mainWindow.setSize(500, 500);
		
		mainWindow.pack();
		
		strike.setVisible(false);

		//InitBasics();
		ReadDamageInfo();
		
		s.setSelectedIndex(0);

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
		return "PSI Damage Editor";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#getCredits()
	 */
	public String getCredits()
	{
		return "Written by JeffMan";
	}

	/**
	 * @see net.starmen.pkhack.HackModule#hide()
	 */
	public void hide()
	{
		mainWindow.setVisible(false);
	}
	
	/**
	 * @see net.starmen.pkhack.HackModule#show()
	 */
	public void show()
	{
		super.show();
		init();

		mainWindow.setVisible(true);
	}


	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getActionCommand().equals(s.getActionCommand())) {
			//int lindex = 0;
			//lindex = s.getSelectedIndex();
			damage.setText(Integer.toString(psidamage[s.getSelectedIndex()]));
			//damage.setText(Integer.toString(psidamage[lindex]));
			ranges.setText(Integer.toString((psidamage[s.getSelectedIndex()] * dmin[s.getSelectedIndex()]) / 100) + " / " + Integer.toString((psidamage[s.getSelectedIndex()] * dmax[s.getSelectedIndex()]) / 100));
			multis.setText("x " + Integer.toString(dmin[s.getSelectedIndex()]) + "% / x " + Integer.toString(dmax[s.getSelectedIndex()]) + "%");
			if (s.getSelectedIndex() > 11) {
				if (s.getSelectedIndex() < 16) {
					strike.setVisible(true);
					strike.setText(Integer.toString(strikes[s.getSelectedIndex() - 12]));
				}
			}
			if (s.getSelectedIndex() < 12) {
				strike.setVisible(false);
			}
			else if (s.getSelectedIndex() > 15) {
				strike.setVisible(false);
			}
		}
		else if (ae.getActionCommand().equalsIgnoreCase("close")) {
			hide();
			//System.out.println("close");
		}
		else if (ae.getActionCommand().equalsIgnoreCase("change_damage")) {
			psidamage[s.getSelectedIndex()] = Integer.parseInt(damage.getText());
			//System.out.println("change_damage");
		}
		else if (ae.getActionCommand().equalsIgnoreCase("apply")) {
			psidamage[s.getSelectedIndex()] = Integer.parseInt(damage.getText());
			if (s.getSelectedIndex() > 11) {
				if (s.getSelectedIndex() < 16) {
					strikes[s.getSelectedIndex() - 12] = Integer.parseInt(strike.getText());
				}
			}
			WriteDamageInfo();
			s.setSelectedIndex(s.getSelectedIndex());
			//System.out.println("apply " + psidamage[s.getSelectedIndex()]);
		}
	}
	
	public void ReadDamageInfo(){
		for (int i = 0; i < 22; i++) {
			psidamage[i] = rom.readMulti(damageaddresses[i], 2);
		}
		for (int j = 0; j < 4; j++) {
			strikes[j] = rom.readMulti(strikeaddresses[j], 2);
		}
	}
	
	public void WriteDamageInfo(){
	    rom.write(damageaddresses[s.getSelectedIndex()], psidamage[s.getSelectedIndex()], 2);
		if (s.getSelectedIndex() > 11) {
			if (s.getSelectedIndex() < 16) {
				rom.write(strikeaddresses[s.getSelectedIndex() - 12], strikes[s.getSelectedIndex() - 12]);
			}
		}
	}
	
	public void InitBasics() {
		
		damageaddresses[0] = 0x29759;
		damageaddresses[1] = 0x29762;
		damageaddresses[2] = 0x2976B;
		damageaddresses[3] = 0x29774;
		
		damageaddresses[4] = 0x297ae;
		damageaddresses[5] = 0x297b7;
		damageaddresses[6] = 0x297c0;
		damageaddresses[7] = 0x297c9;
		
		damageaddresses[8] = 0x2984A;
		damageaddresses[9] = 0x29853;
		damageaddresses[10] = 0x2985C;
		damageaddresses[11] = 0x29865;
		
		damageaddresses[12] = 0x29a77;
		damageaddresses[13] = 0x29a83;
		damageaddresses[14] = 0x29a8F;
		damageaddresses[15] = 0x29a9B;
		
		damageaddresses[16] = 0x29CA9;
		damageaddresses[17] = 0x29CB2;
		
		damageaddresses[18] = 0x29cc9;
		damageaddresses[19] = 0x29cd2;
		damageaddresses[20] = 0x29cdb;
		damageaddresses[21] = 0x29ce4;
		
		psinames[0] = "PSI Rockin 1";
		psinames[1] = "PSI Rockin 2";
		psinames[2] = "PSI Rockin 3";
		psinames[3] = "PSI Rockin 4";
		
		psinames[4] = "PSI Fire 1";
		psinames[5] = "PSI Fire 2";
		psinames[6] = "PSI Fire 3";
		psinames[7] = "PSI Fire 4";
		
		psinames[8] = "PSI Freeze 1";
		psinames[9] = "PSI Freeze 2";
		psinames[10] = "PSI Freeze 3";
		psinames[11] = "PSI Freeze 4";
		
		psinames[12] = "PSI Thunder 1";
		psinames[13] = "PSI Thunder 2";
		psinames[14] = "PSI Thunder 3";
		psinames[15] = "PSI Thunder 4";
		
		psinames[16] = "PSI Starstorm 1";
		psinames[17] = "PSI Starstorm 2";
		
		psinames[18] = "Lifeup 1";
		psinames[19] = "Lifeup 2";
		psinames[20] = "Lifeup 3";
		psinames[21] = "Lifeup 4";
		
		dmin[0] = 50;
		dmax[0] = 150;
		dmin[1] = 50;
		dmax[1] = 150;
		dmin[2] = 50;
		dmax[2] = 150;
		dmin[3] = 50;
		dmax[3] = 150;
		dmin[4] = 75;
		dmax[4] = 125;
		dmin[5] = 75;
		dmax[5] = 125;
		dmin[6] = 75;
		dmax[6] = 125;
		dmin[7] = 75;
		dmax[7] = 125;
		dmin[8] = 75;
		dmax[8] = 125;
		dmin[9] = 75;
		dmax[9] = 125;
		dmin[10] = 75;
		dmax[10] = 125;
		dmin[11] = 75;
		dmax[11] = 125;
		dmin[12] = 50;
		dmax[12] = 150;
		dmin[13] = 50;
		dmax[13] = 150;
		dmin[14] = 50;
		dmax[14] = 150;
		dmin[15] = 50;
		dmax[15] = 150;
		dmin[16] = 75;
		dmax[16] = 125;
		dmin[17] = 75;
		dmax[17] = 125;
		dmin[18] = 75;
		dmax[18] = 125;
		dmin[19] = 75;
		dmax[19] = 125;
		dmin[20] = 100;
		dmax[20] = 100;
		dmin[21] = 75;
		dmax[21] = 125;
		
		strikeaddresses[0] = 0x29a74;
		strikeaddresses[1] = 0x29a80;
		strikeaddresses[2] = 0x29a8c;
		strikeaddresses[3] = 0x29a98;

		//for (int i = 0; i < 17; i ++) {
			//s.addItem(psinames[i]);
		//}
	}
	
}
