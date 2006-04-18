/*
 * Created on Dec 20, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class WindowPositionEditor extends EbHackModule implements
    ActionListener
{

    /**
     * @param rom
     * @param prefs
     */
    public WindowPositionEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    public static class WindowPosition
    {
        private int num, x, y, w, h;
        private EbHackModule hm;

        public WindowPosition(int num, EbHackModule hm)
        {
            this.hm = hm;
            this.num = num;

            AbstractRom rom = hm.rom;
            rom.seek(0x3E450 + num * 8);
            x = rom.readMultiSeek(2);
            y = rom.readMultiSeek(2);
            w = rom.readMultiSeek(2);
            h = rom.readMultiSeek(2);
        }

        public void writeInfo()
        {
            AbstractRom rom = hm.rom;
            rom.seek(0x3E450 + num * 8);
            rom.writeSeek(x, 2);
            rom.writeSeek(y, 2);
            rom.writeSeek(w, 2);
            rom.writeSeek(h, 2);
        }

        public int getX()
        {
            return x;
        }

        public int getY()
        {
            return y;
        }

        public int getWidth()
        {
            return w;
        }

        public int getHeight()
        {
            return h;
        }

        public void setX(int nx)
        {
            x = nx;
        }

        public void setY(int ny)
        {
            y = ny;
        }

        public void setWidth(int width)
        {
            w = width;
        }

        public void setHeight(int height)
        {
            h = height;
        }

        public Rectangle getRect()
        {
            return new Rectangle(x, y, w, h);
        }

        public void setRect(Rectangle rect)
        {
            x = rect.x;
            y = rect.y;
            w = rect.width;
            h = rect.height;
        }
    }
    public static final int NUM_WINDOWS = 0x35;
    public static String[] windowNames = new String[NUM_WINDOWS];
    public static WindowPosition[] positions = new WindowPosition[NUM_WINDOWS];

    private class WindowPlacer extends JComponent implements DocumentListener,
        ActionListener, MouseListener, MouseMotionListener
    {
        private final static int TILE_CORNER = 16;
        private final static int TILE_HORZ = 17;
        private final static int TILE_VERT = 18;
        private int x, y, w, h, xn, yn, wn, hn, size;

        public WindowPlacer(float zoom)
        {
            size = (int) (zoom * 8);
            this.setPreferredSize(new Dimension((int) (256 * zoom),
                (int) (224 * zoom)));
            this.setBackground(Color.BLACK);
            xtf.getDocument().addDocumentListener(this);
            ytf.getDocument().addDocumentListener(this);
            wtf.getDocument().addDocumentListener(this);
            htf.getDocument().addDocumentListener(this);
            selector.addActionListener(this);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        private void loadDimensions()
        {
            xn = Integer.parseInt(xtf.getText());
            yn = Integer.parseInt(ytf.getText());
            x = xn * size;
            y = yn * size;
            wn = Integer.parseInt(wtf.getText()) - 1;
            hn = Integer.parseInt(htf.getText()) - 1;
            w = wn * size;
            h = hn * size;
        }

        private static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;

        private int mouseLoc(int mx, int my)
        {
            int tmp = 0;
            try
            {
                loadDimensions();
                if (mx >= x && my >= y && mx <= x + w + size
                    && my <= y + h + size)
                {
                    if (mx <= x + size)
                        tmp |= 1;
                    else if (mx >= x + w)
                        tmp |= 2;
                    if (my <= y + size)
                        tmp |= 4;
                    else if (my >= y + h)
                        tmp |= 8;
                }
                else
                    tmp = -1;
            }
            catch (NumberFormatException nfe)
            {}
            return tmp;
        }

        public void paint(Graphics g)
        {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            try
            {
                loadDimensions();
                Color[] pal = new Color[4];
                System.arraycopy(WindowBorderEditor.palettes[0][7], 0, pal, 0,
                    4);
                pal[0] = new Color(0, 0, 0, 0);

                byte[][] corner = WindowBorderEditor.graphics[TILE_CORNER], horz = WindowBorderEditor.graphics[TILE_HORZ], vert = WindowBorderEditor.graphics[TILE_VERT];

                Image top = drawImage(horz, pal), bottom = drawImage(horz, pal,
                    false, true), left = drawImage(vert, pal), right = drawImage(
                    vert, pal, true, false);

                g.drawImage(drawImage(corner, pal), x, y, size, size, null);
                g.drawImage(drawImage(corner, pal, true, false), x + w, y,
                    size, size, null);
                g.drawImage(drawImage(corner, pal, false, true), x, y + h,
                    size, size, null);
                g.drawImage(drawImage(corner, pal, true, true), x + w, y + h,
                    size, size, null);
                for (int i = 1; i < wn; i++)
                {
                    g.drawImage(top, x + size * i, y, size, size, null);
                    g.drawImage(bottom, x + size * i, y + h, size, size, null);
                }
                for (int i = 1; i < hn; i++)
                {
                    g.drawImage(left, x, y + size * i, size, size, null);
                    g.drawImage(right, x + w, y + size * i, size, size, null);
                }
            }
            catch (NumberFormatException e)
            {}
        }

        public void changedUpdate(DocumentEvent e)
        {
            repaint();
        }

        public void insertUpdate(DocumentEvent e)
        {
            repaint();
        }

        public void removeUpdate(DocumentEvent e)
        {
            repaint();
        }

        public void actionPerformed(ActionEvent e)
        {
            repaint();
        }

        public void mouseClicked(MouseEvent e)
        {}

        public void mouseEntered(MouseEvent e)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        public void mouseExited(MouseEvent e)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        private int sx, sy, swxn, swyn, swwn, swhn, stmp = -1;

        public void mousePressed(MouseEvent e)
        {
            sx = e.getX();
            sy = e.getY();
            stmp = mouseLoc(sx, sy);
            swxn = xn;
            swyn = yn;
            swwn = wn;
            swhn = hn;
        }

        public void mouseReleased(MouseEvent e)
        {}

        public void mouseDragged(MouseEvent e)
        {
            if (stmp == -1)
                return;
            int mx = e.getX(), my = e.getY();
            if (mx < 0 || my < 0 || mx > getWidth() || my > getHeight())
                return;
            if (stmp == 0) // move
            {
                int tx = Math.max(0, swxn + ((mx - sx) / size)), ty = Math.max(
                    0, swyn + ((my - sy) / size));
                if (tx + swwn + 1 > 32)
                    tx = 32 - (swwn + 1);
                if (ty + swhn + 1 > 28)
                    ty = 28 - (swhn + 1);
                xtf.setText(Integer.toString(tx));
                ytf.setText(Integer.toString(ty));
                repaint();
                return;
            }
            if ((stmp & RIGHT) != 0) // +/- width
            {
                int tmp = Math.max(2, 1 + swwn + ((mx - sx) / size));
                if (tmp + swxn > 32)
                    tmp = 32 - swxn;
                wtf.setText(Integer.toString(tmp));
            }
            if ((stmp & BOTTOM) != 0) // +/- height
            {
                int tmp = Math.max(2, 1 + swhn + ((my - sy) / size));
                if (tmp + swyn > 28)
                    tmp = 28 - swyn;
                htf.setText(Integer.toString(tmp));
            }
            if ((stmp & LEFT) != 0)
            {
                xtf.setText(Integer.toString(Math.max(0, swxn
                    + ((mx - sx) / size))));
                wtf.setText(Integer.toString(Math.max(2, 1 + swwn
                    + ((sx - mx) / size))));
            }
            if ((stmp & TOP) != 0)
            {
                ytf.setText(Integer.toString(Math.max(0, swyn
                    + ((my - sy) / size))));
                htf.setText(Integer.toString(Math.max(2, 1 + swhn
                    + ((sy - my) / size))));
            }
            repaint();
        }

        public void mouseMoved(MouseEvent e)
        {
            int tmp = mouseLoc(e.getX(), e.getY());
            switch (tmp)
            {
                case -1:
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    break;
                case 0:
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    break;
                case LEFT:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    break;
                case RIGHT:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;
                case TOP:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    break;
                case BOTTOM:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    break;
                case LEFT | TOP:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    break;
                case LEFT | BOTTOM:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    break;
                case RIGHT | TOP:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    break;
                case RIGHT | BOTTOM:
                    setCursor(Cursor
                        .getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    break;
                default:
                    System.out
                        .println("Error: WindowPositionEditor.WindowPlacer."
                            + "mouseMoved.tmp=" + tmp);
            }
        }
    }

    private JTextField xtf, ytf, wtf, htf;
    private JComboBox selector;

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    protected void init()
    {
        WindowBorderEditor.readFromRom(this);

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        Box entry = new Box(BoxLayout.Y_AXIS);
        entry.add(getLabeledComponent("Window: ", selector = createComboBox(
            windowNames, this)));
        selector.setActionCommand("windowSel");
        Box tfs = new Box(BoxLayout.Y_AXIS);
        tfs
            .add(getLabeledComponent("X: ",
                xtf = createSizedJTextField(5, true)));
        tfs
            .add(getLabeledComponent("Y: ",
                ytf = createSizedJTextField(5, true)));
        tfs.add(getLabeledComponent("Width: ", wtf = createSizedJTextField(5,
            true)));
        tfs.add(getLabeledComponent("Height: ", htf = createSizedJTextField(5,
            true)));
        entry.add(pairComponents(pairComponents(tfs, new JLabel(), false),
            new WindowPlacer(1), true));

        mainWindow.getContentPane().add(entry, BorderLayout.CENTER);
        selector.setSelectedIndex(0);
        mainWindow.pack();
    }

    public String getVersion()
    {
        return "0.1";
    }

    public String getDescription()
    {
        return "Window Position Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    public void hide()
    {
        mainWindow.hide();
    }

    public void show()
    {
        readFromRom();
        super.show();

        mainWindow.show();
    }

    public static void readFromRom(EbHackModule hm)
    {
        for (int i = 0; i < positions.length; i++)
            positions[i] = new WindowPosition(i, hm);
    }

    private void readFromRom()
    {
        readFromRom(this);
    }

    private void saveInfo(int i)
    {
        WindowPosition p = positions[i];
        p.setX(Integer.parseInt(xtf.getText()));
        p.setY(Integer.parseInt(ytf.getText()));
        p.setWidth(Integer.parseInt(wtf.getText()));
        p.setHeight(Integer.parseInt(htf.getText()));
        p.writeInfo();
    }

    private void showInfo(int i)
    {
        WindowPosition p = positions[i];
        xtf.setText(Integer.toString(p.getX()));
        ytf.setText(Integer.toString(p.getY()));
        wtf.setText(Integer.toString(p.getWidth()));
        htf.setText(Integer.toString(p.getHeight()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("windowSel"))
        {
            showInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            saveInfo(selector.getSelectedIndex());
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
    }

    public void reset()
    {
        readArray("windowNames.txt", true, windowNames);
        readFromRom();
    }
}