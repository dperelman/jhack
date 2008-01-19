package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.EOFException;
import java.util.Arrays;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.XMLPreferences;

public class SwirlEditor extends EbHackModule implements ActionListener, DocumentListener, ChangeListener {
	private class SwirlPreview extends AbstractButton implements MouseListener, MouseMotionListener {
		private Swirl swirl;
		private int frame = 0, selectedRow = 0;
		
		public SwirlPreview() {
			setPreferredSize(new Dimension(256+2,224+2));
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		
		public void setSwirl(Swirl swirl) {
			this.swirl = swirl;
		}
		
		public void setFrame(int frame) {
			this.frame = frame;
		}
		
		public void setSelectedRow(int selectedRow) {
			this.selectedRow = selectedRow;
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.drawRect(0,0,256+1,224+1);
			g.setColor(Color.WHITE);
			g.fillRect(1, 1, 256, 224);
			g.setColor(Color.BLACK);
			if (swirl != null)
				swirl.drawSwirl(g,frame,1,1,selectedRow);
		}
		
		// MouseListener

		public void mouseClicked(MouseEvent e) { }

		public void mouseEntered(MouseEvent e) { }

		public void mouseExited(MouseEvent e) { }

		public void mousePressed(MouseEvent e) {
			if ((e.getY() > 0) && (e.getY() < 256))
				row.setValue(new Integer(e.getY()-1));
		}
		
		// MouseMotionListener

		public void mouseReleased(MouseEvent e) { }

		public void mouseDragged(MouseEvent e) {
			if ((e.getY() >= 1) && (e.getY() <= 224))
				row.setValue(new Integer(e.getY()-1));
		}

		public void mouseMoved(MouseEvent e) { }
	}
	
	public static class Swirl {
		int[][][] swirlData;
		int speed, swirlNum;
		
		public Swirl(AbstractRom rom, int swirlNum, int speed, int startEntry, int numFrames, int swirlEffectAddr) {
			this.swirlNum = swirlNum;
			this.speed = speed;
			swirlData = new int[numFrames][224][4];
			
			for (int i = 0; i < numFrames; i++) {
				if (swirlEffectAddr == 0)
					readSwirlEffect(rom, i, 0xe0200 + rom.readMulti(swirlEffectPtrTable + (startEntry + i) * 2,2));
				else
					readSwirlEffect(rom, i, toRegPointer(rom.readMulti(swirlEffectAddr + (startEntry + i) * 4,4)));
			}
		}
		
		public String toString() {
			return SwirlEditor.SWIRL_NAMES[swirlNum];
		}
		
		public int getSpeed() {
			return speed;
		}
		
		public void setSpeed(int speed) {
			this.speed = speed;
		}
		
		public int getNumFrames() {
			return swirlData.length;
		}
		
		public void setXA1(int frame, int row, int val) {
			swirlData[frame][row][0] = val;
		}
		
		public void setXA2(int frame, int row, int val) {
			swirlData[frame][row][1] = val;
		}
		
		public void setXB1(int frame, int row, int val) {
			swirlData[frame][row][2] = val;
		}
		
		public void setXB2(int frame, int row, int val) {
			swirlData[frame][row][3] = val;
		}
		
		public int getXA1(int frame, int row) {
			return swirlData[frame][row][0];
		}
		
		public int getXA2(int frame, int row) {
			return swirlData[frame][row][1];
		}
		
		public int getXB1(int frame, int row) {
			return swirlData[frame][row][2];
		}
		
		public int getXB2(int frame, int row) {
			return swirlData[frame][row][3];
		}
		
		private void readSwirlEffect(AbstractRom rom, int num, int addr) {
			rom.seek(addr);
			boolean isMode01 = (rom.readSeek() == 01);
			int numOfScanlines = rom.readSeek(), curScanline = 0;
			int[] data = new int[4];
			while (numOfScanlines != 00) {
				if ((numOfScanlines & 0x80) == 0) { // normal repeating mode
					for (int i = 0; i < (isMode01 ? 2 : 4); i++)
						data[i] = rom.readSeek();
					if (isMode01) {
						data[2] = 0xff;
						data[3] = 0;
					}
					for (int i = curScanline; i < curScanline + numOfScanlines; i++)
						for (int j = 0; j < 4; j++)
							swirlData[num][i][j] = data[j];
					
					curScanline += numOfScanlines;
				} else { // continuous mode
					numOfScanlines -= 0x80;
					
					for (int i = curScanline; i < curScanline + numOfScanlines; i++) {
						for (int j = 0; j < (isMode01 ? 2 : 4); j++)
							data[j] = rom.readSeek();
						if (isMode01) {
							data[2] = 0xff;
							data[3] = 0;
						}
						
						for (int j = 0; j < 4; j++)
							swirlData[num][i][j] = data[j];
					}
					
					curScanline += numOfScanlines;
				}
				
				numOfScanlines = rom.readSeek();
			}
		}
		
		public void drawSwirl(Graphics g, int frame, int x, int y, int selectedRow) {
			g.setColor(Color.BLACK);
			for (int i = 0; i < swirlData[frame].length; i++) {
				if (i == selectedRow) {
					g.setColor(Color.BLUE);
					g.drawLine(x, i + y, x+255, i+y);
					g.setColor(Color.RED);
				} else
					g.setColor(Color.BLACK);
				if ((swirlData[frame][i][0] != 0xff) && (swirlData[frame][i][0] < swirlData[frame][i][1]))
					g.drawLine(swirlData[frame][i][0] + x, i + y, swirlData[frame][i][1] + x, i + y);
				if ((swirlData[frame][i][2] != 0xff) && (swirlData[frame][i][2] < swirlData[frame][i][3]))
					g.drawLine(swirlData[frame][i][2] + x, i + y, swirlData[frame][i][3] + x, i + y);
			}
		}
		
		public int[][] getFrameData() {
			int[][] out = new int[swirlData.length][];
			boolean isMode01;
			int hdmaEntries, hdmaLines, pos, repeatNo, continuousNo;
			for (int i = 0; i < swirlData.length; i++) {
				isMode01 = true;
				continuousNo = 0;
				hdmaEntries = 0;
				hdmaLines = 0;
				repeatNo = 0;
				
				for (int j = 0; j < swirlData[i].length; j++) { // find the length of the frame data first
					if (isMode01 && (swirlData[i][j][2] != 0xff))
						isMode01 = false;
					
					if ((j == 0) || !Arrays.equals(swirlData[i][j-1],swirlData[i][j])
							|| (repeatNo == 0x7f)) { // continuous mode
						if (repeatNo != 0)
							repeatNo = 0;
						if ((continuousNo == 0x7f) || (continuousNo == 0)) {
							hdmaEntries++;
							continuousNo = 0;
						}
						hdmaLines++;
						continuousNo++;
					} else if (Arrays.equals(swirlData[i][j-1],swirlData[i][j])) { // repetition mode
						if (continuousNo > 1)
							hdmaEntries++;
						continuousNo = 0;
						repeatNo++;
					}
				}
				
				out[i] = new int[1 + hdmaEntries + (hdmaLines * (isMode01 ? 2 : 4)) + 1];
				
				repeatNo = 0; // repeatNo stores the position of the repetition byte in the following loop
				continuousNo = 0; // so does continuousNo
				// now actually write to the array
				out[i][0] = isMode01 ? 1 : 4;
				out[i][out[i].length-1] = 0;
				pos = 1;
				for (int j = 0; j < swirlData[i].length; j++) { // now actually write to the array
					if ((j == 0) || !Arrays.equals(swirlData[i][j-1],swirlData[i][j])
							|| (out[i][repeatNo] == 0x7f)) { // start continuous mode
						if (repeatNo != 0) // end repeating mode
							repeatNo = 0;
						if ((out[i][continuousNo] == 0xff) || (continuousNo == 0)) {
							continuousNo = pos;
							out[i][pos++] = 0x80;
						}
						
						out[i][pos++] = swirlData[i][j][0]; // write data
						out[i][pos++] = swirlData[i][j][1];
						if (!isMode01) {
							out[i][pos++] = swirlData[i][j][2];
							out[i][pos++] = swirlData[i][j][3];
						}
						
						out[i][continuousNo]++;
					} else if (Arrays.equals(swirlData[i][j-1],swirlData[i][j])) { // start repeating mode
						if (continuousNo != 0) { // end continuous mode
							if (out[i][continuousNo] - 0x80 == 1) {
								repeatNo = continuousNo;
								out[i][repeatNo] = 0x01;
							} else {
								out[i][continuousNo]--; // subtract 1 from last continuous sequence length
								for (int k = pos; k > pos - (isMode01 ? 2 : 4); k--)
									out[i][k] = out[i][k-1]; // move hdma table forward to make room for numofscanlines byte
								repeatNo = pos - (isMode01 ? 2 : 4);
								out[i][repeatNo] = 0x01;
								pos++;
							}

							continuousNo = 0;
						}
						out[i][repeatNo]++;
					}					
				}
			}
			return out;
		}
	}
	
	public static final int swirlTable = 0xedf41 + 4; // first entry is null? 
	public static final int swirlEffectPtrTable = 0xede45;
	public static final int[] asmHackPtrs = new int[] { 0x4ac8f, 0x4ac95, 0x4acdc, 0x4ace4 };
	public static final String[] SWIRL_NAMES = new String[] {
		"Normal Battle Swirl",
		"Phase Distorter Swirl",
		"Boss Battle Swirl",
		"Shield Swirl",
		"Enemy PSI Swirl",
		"Giygas Phase Shift Swirl"
	};
	
	private static IPSDatabase.DatabaseEntry swirlRelocateHack = null;
	private static Swirl[] swirls = new Swirl[6];
	private static int oldAddr, oldLen;
	
	private SwirlPreview sprev;
	private JComboBox swirlChooser, frameChooser;
	private JTextField speed; 
	private JSpinner row,xa1,xa2,xb1,xb2;
	
	public SwirlEditor(AbstractRom rom, XMLPreferences prefs) {
		super(rom, prefs);
	}
	
    public static boolean isSwirlRelocateHacked(AbstractRom rom)
    {
        if (swirlRelocateHack == null)
        {
            return rom.read(0x4ac86) == 0xa5;
        }
        else
        {
            return swirlRelocateHack.isApplied();
        }
    }
	
	protected void init() {
        IPSDatabase.readXML();
        swirlRelocateHack = IPSDatabase.getPatch("Battle Swirl Relocation Hack");
        swirlRelocateHack.checkApplied(rom);
		
		mainWindow = createBaseWindow(this);
		mainWindow.setTitle(getDescription());
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		swirlChooser = EbHackModule.createJComboBoxFromArray(swirls);
		swirlChooser.addActionListener(this);
		panel.add(EbHackModule.pairComponents(new JLabel("Swirl:"), swirlChooser, true, true));
		frameChooser = new JComboBox();
		frameChooser.addActionListener(this);
		panel.add(EbHackModule.pairComponents(new JLabel("Frame:"), frameChooser, true, true));
		sprev = new SwirlPreview();
		panel.add(sprev);
		speed = EbHackModule.createSizedJTextField(3, true, false);
		speed.getDocument().addDocumentListener(this);
		panel.add(EbHackModule.pairComponents(new JLabel("Speed:"), speed, true, false));
		
		row = new JSpinner(new SpinnerNumberModel(0,0,223,1));
		row.addChangeListener(this);
		xa1 = new JSpinner(new SpinnerNumberModel(0,0,255,1));
		xa1.addChangeListener(this);
		xa2 = new JSpinner(new SpinnerNumberModel(0,0,255,1));
		xa2.addChangeListener(this);
		xb1 = new JSpinner(new SpinnerNumberModel(0,0,255,1));
		xb1.addChangeListener(this);
		xb2 = new JSpinner(new SpinnerNumberModel(0,0,255,1));
		xb2.addChangeListener(this);
		
		JPanel panel2, panel3;
		panel2 = new JPanel();
		panel2.add(new JLabel("x1:"));
		panel2.add(xa1);
		panel2.add(new JLabel("x2:"));
		panel2.add(xa2);
		panel3 = new JPanel();
		panel3.add(new JLabel("x1:"));
		panel3.add(xb1);
		panel3.add(new JLabel("x2:"));
		panel3.add(xb2);
		panel.add(
				EbHackModule.pairComponents(EbHackModule.pairComponents(new JLabel("Row:"), row, true, true),
						EbHackModule.pairComponents(panel2,panel3,false), true));
		
		mainWindow.getContentPane().add(
				panel, BorderLayout.CENTER);
		mainWindow.pack();
	}
	
	private static void readFromRom(AbstractRom rom) {
        IPSDatabase.readXML();
        
		if (isSwirlRelocateHacked(rom)) {
			oldAddr = toRegPointer(rom.readMulti(asmHackPtrs[0],3)) - 1; // -1 because there will always be FF shielding in front
			System.out.println(Integer.toHexString(oldAddr));
		} else {
			oldAddr = 0xe0200 + rom.readMulti(swirlEffectPtrTable,2);
		}
		for (int i = 0; i < swirls.length; i++) {
			swirls[i] = new Swirl(rom, i, rom.read(swirlTable + i * 4),
					rom.read(swirlTable + i * 4 + 1),
					rom.read(swirlTable + i * 4 + 2),
					(isSwirlRelocateHacked(rom) ?
							toRegPointer(rom.readMulti(asmHackPtrs[0],3)) : 0));
		}
		oldLen = rom.getSeek() - oldAddr; // (rom.getSeek()-1)+1 because there will always be FF shielding at the end
		System.out.println("oldLen: " + Integer.toHexString(rom.getSeek()) + " - "
				+ Integer.toHexString(oldAddr) + " = " + Integer.toHexString(oldLen));
	}
	
	private static void writeToRom(HackModule hm) throws EOFException {
		AbstractRom rom = hm.rom;
		int[][][] swirlData = new int[swirls.length + 1][][];
		int swirlDataLen = 0, numFrames = 0;
		for (int i = 0; i < swirls.length; i++) {
			rom.write(swirlTable + i * 4, swirls[i].getSpeed());
			rom.write(swirlTable + i * 4 + 1, numFrames);
			swirlData[i+1] = swirls[i].getFrameData();
			rom.write(swirlTable + i * 4 + 2, swirlData[i+1].length);
			for (int j = 0; j < swirlData[i+1].length; j++) {
				swirlDataLen += swirlData[i+1][j].length;
			}
			numFrames += swirlData[i+1].length;
		}
		swirlData[0] = new int[1][numFrames*4];
		Arrays.fill(swirlData[0][0], 0); // filling with 0x00 will make 0xff shielding always occur
		
		System.out.println("sdl: " + Integer.toHexString(swirlDataLen + (numFrames * 4)));
		// will need to write old pointer if rom hasn't been swirl relocate hacked
		rom.write(asmHackPtrs[0],toSnesPointer(oldAddr),3);
		hm.writetoFree(swirlData, asmHackPtrs, 0, 3, oldLen, swirlDataLen + (numFrames * 4), rom.length(), true);
		
		// Write pointers to frame data
		int addr = toRegPointer(rom.readMulti(asmHackPtrs[0], 3));
		int pos = addr + (numFrames * 4), frameNo = 0;
		for (int i = 1; i < swirlData.length; i++)
			for (int j = 0; j < swirlData[i].length; j++) {
				rom.write(addr + frameNo * 4, toSnesPointer(pos), 4);
				pos += swirlData[i][j].length;
				frameNo++;
			}
		
		rom.write(asmHackPtrs[1], rom.readMulti(asmHackPtrs[1],3) + 2, 3); // +2 to these pointers for some reason
		rom.write(asmHackPtrs[3], rom.readMulti(asmHackPtrs[3],3) + 2, 3); // Michael1 is a crazy dude
	}
	
	public void show()
	{
		super.show();
		readFromRom(rom);
		swirlChooser.setSelectedIndex(0);
		mainWindow.setVisible(true);
	}

	public String getCredits() {
		return "Written by Mr. Tenda\n"
		     + "Documented by Michael1 (Alchemic)";
	}

	public String getDescription() {
		return "Battle Swirl Editor";
	}

	public String getVersion() {
		return "0.1";
	}

	public void hide() {
		mainWindow.setVisible(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("apply")) {
	        if (JHack.main.getPrefs().getValue("swirlRelocateHack") == null
	                && swirlRelocateHack != null
	                && !isSwirlRelocateHacked(rom)) {
                Box quesBox = new Box(BoxLayout.Y_AXIS);
                quesBox.add(
                    new JLabel("In order to save in the Battle Swirl Editor, you will need to apply a patch to relocate the data to the expanded area."));
                quesBox.add(
                	new JLabel("Do you still want to save and apply this patch?"));
                JCheckBox savePref =
                    new JCheckBox("Always use this selection.");
                quesBox.add(savePref);
                int ques =
                    JOptionPane.showConfirmDialog(
                        null,
                        quesBox,
                        "Swirl relocation hack?",
                        JOptionPane.YES_NO_OPTION);
                if (ques == JOptionPane.YES_OPTION)
                    swirlRelocateHack.apply();
                if (savePref.isSelected())
                    JHack.main.getPrefs().setValueAsBoolean(
                    	"swirlRelocateHack",
                        ques == JOptionPane.YES_OPTION);
	        } else if (JHack.main.getPrefs().getValueAsBoolean("swirlRelocateHack")
	                    && !isSwirlRelocateHacked(rom)) {
	        	swirlRelocateHack.apply();
	        }
	        swirlRelocateHack.checkApplied(rom);
	        
	        if (isSwirlRelocateHacked(rom)) {
	        	try {
					writeToRom(this);
					readFromRom(rom);
					sprev.setSwirl(swirls[swirlChooser.getSelectedIndex()]);
					sprev.repaint();
				} catch (EOFException e1) {
					e1.printStackTrace();
				}
	        }
		} else if (e.getActionCommand().equals("close"))
        	hide();
        else if (e.getSource().equals(swirlChooser)) {
			frameChooser.removeActionListener(this);
			frameChooser.removeAllItems();
			for (int i = 0; i < swirls[swirlChooser.getSelectedIndex()].getNumFrames(); i++)
				frameChooser.addItem(""+i);
			frameChooser.addActionListener(this);
			frameChooser.setVisible(true);
			
			sprev.setFrame(0);
			sprev.setSwirl(swirls[swirlChooser.getSelectedIndex()]);
			sprev.repaint();
			
			speed.setText(Integer.toString(swirls[swirlChooser.getSelectedIndex()].getSpeed()));
			
			frameChooser.setSelectedIndex(0);
		} else if (e.getSource().equals(frameChooser)) {
			sprev.setFrame(frameChooser.getSelectedIndex());
			sprev.repaint();
			
			xa1.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXA1(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xa2.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXA2(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xb1.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXB1(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xb2.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXB2(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
		}
		
	}

	public void changedUpdate(DocumentEvent e) {
		if (speed.getText().length() > 0)
			swirls[swirlChooser.getSelectedIndex()].setSpeed(Integer.parseInt(speed.getText()));
	}

	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(row)) {
			xa1.removeChangeListener(this);
			xa1.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXA1(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xa1.addChangeListener(this);
			
			xa2.removeChangeListener(this);
			xa2.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXA2(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xa2.addChangeListener(this);
			
			xb1.removeChangeListener(this);
			xb1.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXB1(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xb1.addChangeListener(this);
			
			xb2.removeChangeListener(this);
			xb2.setValue(new Integer(swirls[swirlChooser.getSelectedIndex()].getXB2(frameChooser.getSelectedIndex(), ((Integer) row.getValue()).intValue())));
			xb2.addChangeListener(this);
			
			sprev.setSelectedRow(((Integer) row.getValue()).intValue());
			sprev.repaint();
		} else if (e.getSource().equals(xa1)) {
			swirls[swirlChooser.getSelectedIndex()].setXA1(frameChooser.getSelectedIndex(),
					((Integer) row.getValue()).intValue(), ((Integer) xa1.getValue()).intValue());
			sprev.repaint();
		} else if (e.getSource().equals(xa2)) {
			swirls[swirlChooser.getSelectedIndex()].setXA2(frameChooser.getSelectedIndex(),
					((Integer) row.getValue()).intValue(), ((Integer) xa2.getValue()).intValue());
			sprev.repaint();
		} else if (e.getSource().equals(xb1)) {
			swirls[swirlChooser.getSelectedIndex()].setXB1(frameChooser.getSelectedIndex(),
					((Integer) row.getValue()).intValue(), ((Integer) xb1.getValue()).intValue());
			sprev.repaint();
		} else if (e.getSource().equals(xb2)) {
			swirls[swirlChooser.getSelectedIndex()].setXB2(frameChooser.getSelectedIndex(),
					((Integer) row.getValue()).intValue(), ((Integer) xb2.getValue()).intValue());
			sprev.repaint();
		}
	}
}
