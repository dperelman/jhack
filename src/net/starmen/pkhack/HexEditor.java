/*
 * Created on Jan 16, 2004
 */
package net.starmen.pkhack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class HexEditor extends GeneralHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public HexEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
        // TODO Auto-generated constructor stub
    }
    private HexTable table;
    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    private JHexEdit hexEdit;

    protected void init()
    {
        mainWindow = new JFrame(getDescription());
        mainWindow.getContentPane().setLayout(new BorderLayout());
        JButton close = new JButton("Close");
        close.setActionCommand("close");
        close.addActionListener(this);
        JButton gotob = new JButton("Goto");
        gotob.setActionCommand("goto");
        gotob.addActionListener(this);
        JButton findb = new JButton("Find");
        findb.setActionCommand("find");
        findb.addActionListener(this);
        mainWindow.getContentPane().add(
            createFlowLayout(new JComponent[]{gotob, findb, close}),
            BorderLayout.SOUTH);
        JHexEdit t = hexEdit = new JHexEdit(new HexRom(rom));
        mainWindow.getContentPane().add(t, BorderLayout.CENTER);
        table = t.table;
        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.2";
    }

    public String getDescription()
    {
        return "Hex Editor";
    }

    public String getCredits()
    {
        return "Hex Editor Component by Claude Duguay\n"
            + "Adaptation to JHack by AnyoneEB";
    }

    public void show()
    {
        super.show();
        mainWindow.setVisible(true);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }
    private JDialog gotoDialog, findWindow;
    private JTextArea findTA;

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("goto"))
        {
            if (gotoDialog == null) initGotoDialog();
            gotoDialog.setVisible(true);
        }
        else if (ae.getActionCommand().equals("find"))
        {
            initFindWindow();
            findWindow.setVisible(true);
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    private boolean gotoOffset(int off)
    {
        if (off > rom.length() || off < 0) return false;
        table.gotoOff(off);
        return true;
    }
    private int findOff = 0;

    private void find()
    {
        String[] f = findTA.getText().split(" ");
        byte[] s = new byte[f.length];
        for (int i = 0; i < f.length; i++)
        {
            s[i] = (byte) Integer.parseInt(f[i], 16);
        }
        int off = rom.find(findOff, s);
        if (off == -1)
        {
            JOptionPane.showMessageDialog(findWindow,
                "The search you entered\n" + "is not in the ROM.",
                "Unable to find", JOptionPane.ERROR_MESSAGE);
            findOff = 0;
        }
        else
        {
            findOff = off + 1;
            gotoOffset(off);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#reset()
     */
    public void reset()
    {
        JHexEdit t = new JHexEdit(new HexRom(rom));
        mainWindow.getContentPane().remove(hexEdit);
        mainWindow.getContentPane().add(t, BorderLayout.CENTER);
        table = t.table;
    }

    private void initGotoDialog()
    {
        gotoDialog = new JDialog(mainWindow, "Goto Offset", true);
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
                int offset;
                try
                {
                    offset = regType.isSelected() ? Integer.parseInt(tf
                        .getText(), 16) : HackModule.toRegPointer(Integer.parseInt(
                        tf.getText(), 16));
                }
                catch (NumberFormatException e)
                {
                    e.printStackTrace();
                    return;
                }
                if (!gotoOffset(offset))
                    JOptionPane.showMessageDialog(gotoDialog,
                        "The offset you entered is not\n" + "in the ROM.",
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

    private void initFindWindow()
    {
        if (findWindow == null)
        {
            findWindow = new JDialog(mainWindow, "Hex Editor Find", false);
            findWindow.getContentPane().setLayout(new BorderLayout());
            findWindow.getContentPane().add(findTA = new JTextArea(5, 30),
                BorderLayout.NORTH);
            findTA.setLineWrap(true);
            findTA.setWrapStyleWord(true);
            findTA.setDocument(new PlainDocument()
            {
                public void insertString(int offs, String str, AttributeSet a)
                    throws BadLocationException
                {
                    if (str == "\n")
                        find();
                    else
                        super.insertString(offs, str, a);
                }
            });
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
}
