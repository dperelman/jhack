/*
 * Created on Mar 1, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class PointerEditor extends EbHackModule implements ActionListener
{

    /**
     * @param rom
     * @param prefs
     */
    public PointerEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public String getVersion()
    {
        return "0.3";
    }

    public String getDescription()
    {
        return "Pointer Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Address list by Chris_Davis";
    }

    private class Pointer
    {
        private int address;
        private String desc;

        public Pointer(int address, String desc)
        {
            this.address = address;
            this.desc = desc;
        }

        public String toString()
        {
            return "[" + Integer.toHexString(address) + "] " + desc;
        }

        /**
         * @return Returns the address.
         */
        public int getAddress()
        {
            return address;
        }

        /**
         * @return Returns the desc.
         */
        public String getDesc()
        {
            return desc;
        }

        public String readPointer(boolean reg)
        {
            return Integer.toHexString(reg ? toRegPointer(rom
                .readAsmPointer(address)) : rom.readAsmPointer(address));
        }

        public void writePointer(String pointer, boolean reg)
        {
            rom.writeAsmPointer(reg ? toSnesPointer(address) : address, Integer
                .parseInt(pointer, 16));
        }

        public boolean isValidPointer()
        {
            return rom.readAsmPointer(address) != -1;
        }
    }

    private ArrayList pointers = new ArrayList();

    private void readPointerList()
    {
        try
        {
            String[] pointerList = new CommentedLineNumberReader(
                new InputStreamReader(ClassLoader
                    .getSystemResourceAsStream(DEFAULT_BASE_DIR
                        + "pointerList.txt"))).readUsedLines();

            for (int i = 0; i < pointerList.length; i++)
            {
                if (pointerList[i].startsWith("-"))
                {
                    // title line
                    pointers.add("_"
                        + pointerList[i].replaceAll("-", "").toUpperCase()
                        + "_");
                }
                else
                {
                    // pointer
                    String[] args = pointerList[i].split("=", 2);
                    pointers.add(new Pointer(Integer.parseInt(args[0].trim(),
                        16), args[1].trim()));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private JLabel asmPreview;
    private JTextField tf;
    private JComboBox pointSel;
    private JRadioButton reg, snes;

    protected void init()
    {
        readPointerList();

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());

        Box edit = new Box(BoxLayout.Y_AXIS);

        pointSel = new JComboBox(pointers.toArray());
        pointSel.setEditable(true);
        pointSel.setActionCommand("pointSel");
        pointSel.addActionListener(this);
        pointSel.setSelectedIndex(0);
        edit.add(pointSel);

        asmPreview = new JLabel("A9 YY ZZ 85 A9 WW XX 85");
        edit.add(getLabeledComponent("ASM: ", asmPreview));

        ButtonGroup type = new ButtonGroup();
        reg = new JRadioButton("0x");
        reg.setSelected(false);
        reg.setActionCommand("type_reg");
        reg.addActionListener(this);
        type.add(reg);
        snes = new JRadioButton("$");
        snes.setSelected(true);
        snes.setActionCommand("type_snes");
        snes.addActionListener(this);
        type.add(snes);

        tf = createSizedJTextField(6, true, true);
        tf.getDocument().addDocumentListener(new DocumentListener()
        {
            public void change()
            {
                try
                {
                    int snes_ptr = Integer.parseInt(tf.getText(), 16);
                    if (reg.isSelected())
                        snes_ptr = toSnesPointer(snes_ptr);
                    if (tf.getText().length() > 0)
                        asmPreview.setText("A9 "
                            + addZeros(Integer.toHexString(snes_ptr & 0xff), 2)
                            + " "
                            + addZeros(Integer
                                .toHexString((snes_ptr >> 8) & 0xff), 2)
                            + " 85 0E A9 "
                            + addZeros(Integer
                                .toHexString((snes_ptr >> 16) & 0xff), 2)
                            + " "
                            + addZeros(Integer
                                .toHexString((snes_ptr >> 24) & 0xff), 2));
                }
                catch (NumberFormatException e)
                {}
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
        JPanel tfPanel = new JPanel(new BorderLayout());
        tfPanel.add(tf, BorderLayout.EAST);
        tfPanel.add(new JLabel("Pointer: "), BorderLayout.WEST);
        tfPanel.add(createFlowLayout(new JRadioButton[]{reg, snes}),
            BorderLayout.CENTER);
        // edit.add(getLabeledComponent("Pointer: $", tf));
        edit.add(tfPanel);

        mainWindow.getContentPane().add(edit, BorderLayout.CENTER);

        mainWindow.pack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        showInfo();
        mainWindow.show();
    }

    public void hide()
    {
        mainWindow.hide();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(pointSel.getActionCommand()))
        {
            showInfo();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo();
        }
        else if (ae.getActionCommand().equals("type_reg"))
        {
            int ptr = Integer.parseInt(tf.getText(), 16);
            tf.setText(Integer.toHexString(toRegPointer(ptr)));
        }
        else if (ae.getActionCommand().equals("type_snes"))
        {
            int ptr = Integer.parseInt(tf.getText(), 16);
            tf.setText(Integer.toHexString(toSnesPointer(ptr)));
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    private Pointer getCurrentPointer()
    {
        int i = pointSel.getSelectedIndex();
        if (i == -1)
        {
            try
            {
                return new Pointer(Integer.parseInt(pointSel.getSelectedItem()
                    .toString(), 16), "");
            }
            catch (NumberFormatException e)
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Entered address is not hex.", "Invalid address",
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        else
        {
            try
            {
                return (Pointer) pointers.get(i);
            }
            catch (ClassCastException e)
            {
                // highlight next if current is a title
                pointSel.setSelectedIndex(pointSel.getSelectedIndex() + 1);
                return null;
            }
        }
    }

    private void showInfo()
    {
        Pointer p = getCurrentPointer();
        if (p != null)
        {
            tf.setText(p.readPointer(reg.isSelected()));
            if (!p.isValidPointer())
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "No ASM pointer present at entered address.",
                    "Invalid pointer at address", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void saveInfo()
    {
        Pointer p = getCurrentPointer();
        if (p != null)
        {
            try
            {
                p.writePointer(tf.getText(), reg.isSelected());
            }
            catch (NumberFormatException e)
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Entered pointer is not hex.", "Invalid pointer",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
