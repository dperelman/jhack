package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.Rom;
import net.starmen.pkhack.XMLPreferences;

/**
 * Edits the PSI Teleport destination entries.
 * 
 * @author AnyoneEB
 */
public class TeleportEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public TeleportEditor(Rom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    private JTextField[][] tfs = new JTextField[16][4];
    private JComboBox ppuBox;
    /**
     * Array of teleport destination entries.
     * 
     * @see TeleportData
     */
    public static TeleportData[] td;
    private static Icon blackenedNessIcon = initIcon();

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
        //mainWindow.setSize(400, 500);
        mainWindow.setResizable(false);

        JPanel entry = new JPanel();
        entry.setLayout(new GridLayout(17, 1));

        ppuBox = new JComboBox();
        ppuBox.addItem("8 (Door coords)");
        ppuBox.addItem("32 (Map coords)");
        ppuBox.setActionCommand("ppuBox");
        ppuBox.addActionListener(this);
        mainWindow.getContentPane()
            .add(getLabeledComponent("Pixels per unit:", ppuBox),
                BorderLayout.NORTH);

        String[] titles = {"Name", "Flag", "X", "Y"};
        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.X_AXIS));
        for (int i = 0; i < titles.length; i++)
        {
            labels.add(getSizedJLabel(titles[i], i == 0 ? 25 : 5));
        }
        entry.add(labels);

        for (int i = 0; i < tfs.length; i++)
        {
            entry.add(getTeleportRow(tfs[i]));
        }

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        mainWindow.pack();
    }

    private JPanel getTeleportRow(JTextField[] in)
    {
        JPanel out = new JPanel();
        out.setLayout(new BoxLayout(out, BoxLayout.X_AXIS));
        for (int j = 0; j < in.length; j++)
        {
            in[j] = HackModule.createSizedJTextField(j == 0 ? 25 : 5);
            out.add(in[j]);
        }
        return out;
    }

    private static JLabel getSizedJLabel(String text, int cols)
    {
        JLabel out = new JLabel(text);
        Dimension size = new JTextField(cols).getPreferredSize();
        out.setMaximumSize(size);
        return out;
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
        return "Teleport Destination Editor";
    }

    /**
     * @see net.starmen.pkhack.HackModule#getCredits()
     */
    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Based on source code by Tomato\n"
            + "PPU code done by EBisumaru";
    }

    /**
     * @see net.starmen.pkhack.HackModule#show()
     */
    public void show()
    {
        super.show();
        readFromRom();
        showInfo();
        mainWindow.setVisible(true);
    }

    /**
     * Reads in teleport entries from the ROM into {@link #td}.
     * 
     * @see TeleportData
     */
    public static void readFromRom(HackModule hm)
    {
        td = new TeleportData[16];
        for (int i = 0; i < td.length; i++)
        {
            td[i] = new TeleportData(i, hm);
        }
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    private void showInfo()
    {
        String temp;
        if (ppuBox.getSelectedIndex() == 0)
            for (int i = 0; i < td.length; i++)
            {
                tfs[i][0].setText(new String(td[i].name).trim());
                temp = addZeros(Integer.toString(td[i].flag, 16), 4);
                temp = temp.substring(0, 2) + " " + temp.substring(2, 4);
                tfs[i][1].setText(temp);
                tfs[i][2].setText(Integer.toString(td[i].x));
                tfs[i][3].setText(Integer.toString(td[i].y));
            }
        else
            for (int i = 0; i < td.length; i++)
            {
                tfs[i][0].setText(new String(td[i].name).trim());
                temp = addZeros(Integer.toString(td[i].flag, 16), 4);
                temp = temp.substring(0, 2) + " " + temp.substring(2, 4);
                tfs[i][1].setText(temp);
                tfs[i][2].setText(Integer.toString(td[i].x / 4));
                tfs[i][3].setText(Integer.toString(td[i].y / 4));
            }
    }

    private void saveInfo()
    {
        //String temp;
        for (int i = 0; i < td.length; i++)
        {
            td[i].name = tfs[i][0].getText();

            td[i].flag = Integer.parseInt(killSpaces(tfs[i][1].getText()), 16);
            if (ppuBox.getSelectedIndex() == 0)
            {
                td[i].x = Integer.parseInt(tfs[i][2].getText());
                td[i].y = Integer.parseInt(tfs[i][3].getText());
            }
            else
            {
                td[i].x = Integer.parseInt(tfs[i][2].getText()) * 4;
                td[i].y = Integer.parseInt(tfs[i][3].getText()) * 4;
            }
            td[i].writeInfo();
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#hide()
     */
    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("apply"))
        {
            saveInfo();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else if (ae.getActionCommand().equals("ppuBox"))
        {
            showInfo();
        }
    }

    /**
     * Represents an entry in the PSI Teleport destinations table.
     */
    public static class TeleportData
    {
        private HackModule hm;
        /**
         * Where this entry is located in the ROM.
         */
        public int address;
        /**
         * What number entry this is.
         */
        public int num;
        /**
         * Name of this entry. This is shown in the PSI Teleport window in the
         * game.
         */
        public String name;
        /**
         * Flag that decides if this entry should be shown in the game's PSI
         * Teleport window. If it's set the entry is shown, if it's not set,
         * the entry isn't shown.
         */
        public int flag;
        /**
         * X of the destination. This is a two byte value.
         */
        public int x;
        /**
         * Y of the destination. This is a two byte value.
         */
        public int y;

        /**
         * Reads the specified entry number from the ROM into this.
         * 
         * @param num Entry number to read.
         */
        public TeleportData(int num, HackModule hm) {
            this.hm = hm;
            this.num = num;
            this.address = 0x157a9f + (num * 31);

            Rom rom = hm.rom;

            rom.seek(this.address);

            name = hm.readSeekRegString(25);

            flag = (rom.readSeek() << 8) | rom.readSeek();
            x = rom.readMultiSeek(2);
            y = rom.readMultiSeek(2);
        }

        /**
         * Writes this into the ROM.
         */
        public void writeInfo()
        {
            Rom rom = hm.rom;

            rom.seek(this.address);

            hm.writeSeekRegString(25, name);
            rom.writeSeek((flag >> 8) & 255);
            rom.writeSeek(flag & 255);
            rom.writeSeek(x, 2);
            rom.writeSeek(y, 2);
        }
    }

    /**
     * @see net.starmen.pkhack.HackModule#getIcon()
     */
    public Icon getIcon()
    {
        return blackenedNessIcon;
    }

    private static Icon initIcon()
    {
        BufferedImage out = new BufferedImage(16, 16,
            BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = out.getGraphics();

        //Created by ImageFileToCode from net/starmen/pkhack/blackenedNess.gif
        g.setColor(new Color(192, 192, 192));
        g.setColor(new Color(48, 32, 32));
        g.drawLine(5, 0, 5, 0);
        g.drawLine(10, 0, 10, 0);
        g.drawLine(0, 1, 0, 1);
        g.drawLine(3, 1, 3, 1);
        g.drawLine(5, 1, 5, 1);
        g.drawLine(9, 1, 9, 1);
        g.drawLine(12, 1, 12, 1);
        g.drawLine(15, 1, 15, 1);
        g.drawLine(1, 2, 1, 2);
        g.drawLine(3, 2, 3, 2);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(5, 2, 5, 2);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(6, 2, 6, 2);
        g.drawLine(7, 2, 7, 2);
        g.drawLine(8, 2, 8, 2);
        g.drawLine(9, 2, 9, 2);
        g.drawLine(10, 2, 10, 2);
        g.drawLine(11, 2, 11, 2);
        g.drawLine(14, 2, 14, 2);
        g.drawLine(2, 3, 2, 3);
        g.drawLine(3, 3, 3, 3);
        g.drawLine(4, 3, 4, 3);
        g.drawLine(5, 3, 5, 3);
        g.drawLine(6, 3, 6, 3);
        g.drawLine(7, 3, 7, 3);
        g.drawLine(8, 3, 8, 3);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(9, 3, 9, 3);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(10, 3, 10, 3);
        g.drawLine(11, 3, 11, 3);
        g.drawLine(12, 3, 12, 3);
        g.drawLine(13, 3, 13, 3);
        g.drawLine(15, 3, 15, 3);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(0, 4, 0, 4);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(1, 4, 1, 4);
        g.drawLine(2, 4, 2, 4);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(3, 4, 3, 4);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(4, 4, 4, 4);
        g.drawLine(5, 4, 5, 4);
        g.drawLine(6, 4, 6, 4);
        g.drawLine(7, 4, 7, 4);
        g.drawLine(8, 4, 8, 4);
        g.drawLine(9, 4, 9, 4);
        g.drawLine(10, 4, 10, 4);
        g.drawLine(11, 4, 11, 4);
        g.drawLine(12, 4, 12, 4);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(13, 4, 13, 4);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(14, 4, 14, 4);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(15, 4, 15, 4);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(0, 5, 0, 5);
        g.drawLine(1, 5, 1, 5);
        g.drawLine(2, 5, 2, 5);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(3, 5, 3, 5);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(4, 5, 4, 5);
        g.drawLine(5, 5, 5, 5);
        g.drawLine(6, 5, 6, 5);
        g.drawLine(7, 5, 7, 5);
        g.drawLine(8, 5, 8, 5);
        g.drawLine(9, 5, 9, 5);
        g.drawLine(10, 5, 10, 5);
        g.drawLine(11, 5, 11, 5);
        g.drawLine(12, 5, 12, 5);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(13, 5, 13, 5);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(14, 5, 14, 5);
        g.drawLine(2, 6, 2, 6);
        g.drawLine(3, 6, 3, 6);
        g.drawLine(4, 6, 4, 6);
        g.drawLine(5, 6, 5, 6);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(6, 6, 6, 6);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(7, 6, 7, 6);
        g.drawLine(8, 6, 8, 6);
        g.drawLine(9, 6, 9, 6);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(10, 6, 10, 6);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(11, 6, 11, 6);
        g.drawLine(12, 6, 12, 6);
        g.drawLine(13, 6, 13, 6);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(14, 6, 14, 6);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(1, 7, 1, 7);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(2, 7, 2, 7);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(3, 7, 3, 7);
        g.drawLine(4, 7, 4, 7);
        g.drawLine(5, 7, 5, 7);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(6, 7, 6, 7);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(7, 7, 7, 7);
        g.drawLine(8, 7, 8, 7);
        g.drawLine(9, 7, 9, 7);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(10, 7, 10, 7);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(11, 7, 11, 7);
        g.drawLine(12, 7, 12, 7);
        g.drawLine(13, 7, 13, 7);
        g.drawLine(14, 7, 14, 7);
        g.drawLine(15, 7, 15, 7);
        g.drawLine(1, 8, 1, 8);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(2, 8, 2, 8);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(3, 8, 3, 8);
        g.drawLine(4, 8, 4, 8);
        g.drawLine(5, 8, 5, 8);
        g.drawLine(6, 8, 6, 8);
        g.drawLine(7, 8, 7, 8);
        g.drawLine(8, 8, 8, 8);
        g.drawLine(9, 8, 9, 8);
        g.drawLine(10, 8, 10, 8);
        g.drawLine(11, 8, 11, 8);
        g.drawLine(12, 8, 12, 8);
        g.drawLine(13, 8, 13, 8);
        g.drawLine(14, 8, 14, 8);
        g.drawLine(15, 8, 15, 8);
        g.drawLine(1, 9, 1, 9);
        g.drawLine(2, 9, 2, 9);
        g.drawLine(3, 9, 3, 9);
        g.drawLine(4, 9, 4, 9);
        g.drawLine(5, 9, 5, 9);
        g.drawLine(6, 9, 6, 9);
        g.drawLine(7, 9, 7, 9);
        g.drawLine(8, 9, 8, 9);
        g.drawLine(9, 9, 9, 9);
        g.drawLine(10, 9, 10, 9);
        g.drawLine(11, 9, 11, 9);
        g.drawLine(12, 9, 12, 9);
        g.drawLine(13, 9, 13, 9);
        g.drawLine(14, 9, 14, 9);
        g.drawLine(15, 9, 15, 9);
        g.drawLine(2, 10, 2, 10);
        g.drawLine(3, 10, 3, 10);
        g.drawLine(4, 10, 4, 10);
        g.drawLine(5, 10, 5, 10);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(6, 10, 6, 10);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(7, 10, 7, 10);
        g.drawLine(8, 10, 8, 10);
        g.drawLine(9, 10, 9, 10);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(10, 10, 10, 10);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(11, 10, 11, 10);
        g.drawLine(12, 10, 12, 10);
        g.drawLine(13, 10, 13, 10);
        g.drawLine(14, 10, 14, 10);
        g.drawLine(3, 11, 3, 11);
        g.drawLine(4, 11, 4, 11);
        g.drawLine(5, 11, 5, 11);
        g.drawLine(6, 11, 6, 11);
        g.setColor(new Color(242, 242, 242));
        g.drawLine(7, 11, 7, 11);
        g.drawLine(8, 11, 8, 11);
        g.drawLine(9, 11, 9, 11);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(10, 11, 10, 11);
        g.drawLine(11, 11, 11, 11);
        g.drawLine(12, 11, 12, 11);
        g.drawLine(13, 11, 13, 11);
        g.drawLine(1, 12, 1, 12);
        g.drawLine(2, 12, 2, 12);
        g.drawLine(4, 12, 4, 12);
        g.drawLine(5, 12, 5, 12);
        g.drawLine(6, 12, 6, 12);
        g.drawLine(7, 12, 7, 12);
        g.drawLine(8, 12, 8, 12);
        g.drawLine(9, 12, 9, 12);
        g.drawLine(10, 12, 10, 12);
        g.drawLine(11, 12, 11, 12);
        g.drawLine(12, 12, 12, 12);
        g.drawLine(14, 12, 14, 12);
        g.drawLine(15, 12, 15, 12);
        g.drawLine(1, 13, 1, 13);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(2, 13, 2, 13);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(5, 13, 5, 13);
        g.drawLine(6, 13, 6, 13);
        g.drawLine(7, 13, 7, 13);
        g.drawLine(8, 13, 8, 13);
        g.drawLine(9, 13, 9, 13);
        g.drawLine(10, 13, 10, 13);
        g.drawLine(11, 13, 11, 13);
        g.drawLine(14, 13, 14, 13);
        g.drawLine(15, 13, 15, 13);
        g.drawLine(1, 14, 1, 14);
        g.drawLine(2, 14, 2, 14);
        g.drawLine(3, 14, 3, 14);
        g.drawLine(4, 14, 4, 14);
        g.drawLine(5, 14, 5, 14);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(6, 14, 6, 14);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(7, 14, 7, 14);
        g.drawLine(8, 14, 8, 14);
        g.drawLine(9, 14, 9, 14);
        g.drawLine(10, 14, 10, 14);
        g.drawLine(11, 14, 11, 14);
        g.drawLine(12, 14, 12, 14);
        g.drawLine(13, 14, 13, 14);
        g.setColor(new Color(80, 113, 97));
        g.drawLine(14, 14, 14, 14);
        g.setColor(new Color(48, 32, 32));
        g.drawLine(15, 14, 15, 14);
        g.drawLine(2, 15, 2, 15);
        g.drawLine(3, 15, 3, 15);
        g.drawLine(4, 15, 4, 15);
        g.drawLine(5, 15, 5, 15);
        g.drawLine(6, 15, 6, 15);
        g.drawLine(7, 15, 7, 15);
        g.drawLine(8, 15, 8, 15);
        g.drawLine(9, 15, 9, 15);
        g.drawLine(10, 15, 10, 15);
        g.drawLine(11, 15, 11, 15);
        g.drawLine(12, 15, 12, 15);
        g.drawLine(13, 15, 13, 15);
        g.drawLine(14, 15, 14, 15);

        return new ImageIcon(out);
    }

}
