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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.CommentedLineNumberReader;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
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
        return "0.1";
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

        public String readPointer()
        {
            return Integer.toHexString(rom.readAsmPointer(address));
        }

        public void writePointer(String pointer)
        {
            rom.writeAsmPointer(address, Integer.parseInt(pointer, 16));
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
                    //title line
                    pointers.add("_"
                        + pointerList[i].replaceAll("-", "").toUpperCase()
                        + "_");
                }
                else
                {
                    //pointer
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

        tf = createSizedJTextField(6);
        tf.getDocument().addDocumentListener(new DocumentListener()
        {
            public void change()
            {
                if (tf.getText().length() > 0)
                    asmPreview.setText("A9 "
                        + addZeros(Integer.toHexString(Integer.parseInt(tf
                            .getText(), 16) & 0xff), 2)
                        + " "
                        + addZeros(Integer.toHexString((Integer.parseInt(tf
                            .getText(), 16) >> 8) & 0xff), 2)
                        + " 85 0E A9 "
                        + addZeros(Integer.toHexString((Integer.parseInt(tf
                            .getText(), 16) >> 16) & 0xff), 2)
                        + " "
                        + addZeros(Integer.toHexString((Integer.parseInt(tf
                            .getText(), 16) >> 24) & 0xff), 2));
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
        edit.add(getLabeledComponent("Pointer: $", tf));

        mainWindow.getContentPane().add(edit, BorderLayout.CENTER);

        mainWindow.pack();
    }

    /* (non-Javadoc)
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();

        pointSel.setSelectedIndex(pointSel.getSelectedIndex() != -1 ? pointSel
            .getSelectedIndex() : 0);

        mainWindow.show();
    }

    public void hide()
    {
        mainWindow.hide();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals(pointSel.getActionCommand()))
        {
            showInfo(pointSel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(pointSel.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    private void showInfo(int i)
    {
        Pointer p;
        if (i == -1)
        {
            try
            {
                p = new Pointer(Integer.parseInt(pointSel.getSelectedItem()
                    .toString(), 16), "");
            }
            catch (NumberFormatException e)
            {
                System.out
                    .println("Pointer Editor: Entered address is not hex.");
                return;
            }
        }
        else
        {
            try
            {
                p = ((Pointer) pointers.get(i));
            }
            catch (ClassCastException e)
            {
                //highlight next if current is a title
                pointSel.setSelectedIndex(pointSel.getSelectedIndex() + 1);
                return;
            }
        }
        tf.setText(p.readPointer());
    }

    private void saveInfo(int i)
    {
        Pointer p;
        if (i == -1)
        {
            try
            {
                p = new Pointer(Integer.parseInt(pointSel.getSelectedItem()
                    .toString(), 16), "");
            }
            catch (NumberFormatException e)
            {
                System.out
                    .println("Pointer Editor: Entered address is not hex.");
                return;
            }
        }
        else
        {
            p = ((Pointer) pointers.get(i));
        }
        try
        {
            p.writePointer(tf.getText());
        }
        catch (NumberFormatException e)
        {
            System.out.println("Pointer Editor: Entered pointer is not hex.");
        }

    }

}
