/*
 * Created on Dec 31, 2003
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IPSDatabase;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.FontEditor.Font.Character;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class FontEditor extends EbHackModule implements ActionListener
{
    /**
     * @param rom
     * @param prefs
     */
    public FontEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        try
        {
            String[] exts = new String[]{"efn", "efs", "eft"};
            Class[] c = new Class[]{byte[].class, Object[].class};
            for (int i = 0; i < exts.length; i++)
                IPSDatabase.registerExtension(exts[i], FontEditor.class
                    .getMethod("importFont", c), FontEditor.class.getMethod(
                    "restore", c), FontEditor.class.getMethod("check", c),
                    new Object[]{new Integer(i), this});
        }
        catch (SecurityException e)
        {
            // no security model, shouldn't have to worry about this
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            // spelling mistake, maybe? ^_^;
            e.printStackTrace();
        }
    }

    //TODO Make Font.createFont() create an instance of a NormalFont,
    // SmallFont, or TinyFont?
    public static class Font
    {
        public static class Character
        {
            public static Color FOREGROUND = Color.WHITE;
            public static Color BACKGROUND = Color.BLACK;
            public static final int[] CHAR_SIZES = new int[]{32, 16, 8};
            private int width, num, size;
            private int address, widthAddress;
            private byte[][] img;
            private HackModule hm;

            public Character(int address, int widthAddress, int size, int num,
                HackModule hm)
            {
                this.hm = hm;
                this.address = address;
                this.widthAddress = widthAddress;
                this.size = size;
                this.num = num;

                readInfo();
            }

            public void readInfo()
            {
                this.width = hm.rom.read(widthAddress);
                int offset = address;
                if (size == FONT_SIZE_NORMAL)
                {
                    img = new byte[16][16];
                    offset += hm.read1BPPArea(img, offset, 16, 0, 0);
                    offset += hm.read1BPPArea(img, offset, 16, 8, 0);
                }
                else if (size == FONT_SIZE_SMALL)
                {
                    img = new byte[8][16];
                    offset += hm.read1BPPArea(img, offset, 16, 0, 0);
                }
                else if (size == FONT_SIZE_TINY)
                {
                    img = new byte[8][8];
                    offset += hm.read1BPPArea(img, offset, 8, 0, 0);
                }
            }

            public void writeInfo()
            {
                hm.rom.write(widthAddress, width);

                int offset = address;
                if (size == FONT_SIZE_NORMAL)
                {
                    offset += hm.write1BPPArea(img, offset, 16, 0, 0);
                    hm.write1BPPArea(img, offset, 16, 8, 0);
                }
                else if (size == FONT_SIZE_SMALL)
                {
                    hm.write1BPPArea(img, offset, 16, 0, 0);
                }
                else if (size == FONT_SIZE_TINY)
                {
                    hm.write1BPPArea(img, offset, 8, 0, 0);
                }
            }

            //            public Image draw(Image target, int sx, int sy)
            //            {
            //                Graphics g = target.getGraphics();
            //                g.setColor(Color.BLUE);
            //                g.fillRect(sx, sy, width, 16);
            //                g.setColor(Color.WHITE);
            //                for (int y = 0; y < 16; y++)
            //                    for (int x = 0; x < width; x++)
            //                        if (img[x][y] == 0)
            //                            g.drawLine(sx + x, sy + y, sx + x, sy + y);
            //                debug();
            //                return target;
            //            }
            public void debug()
            {
                System.out.println("Character DEBUG (0x"
                    + Integer.toHexString(num) + "):");
                for (int y = 0; y < img[0].length; y++)
                {
                    for (int x = 0; x < img.length; x++)
                    {
                        System.out.print(img[x][y] == 0 ? (x < width
                            ? "X"
                            : "|") : (x < width ? "," : "."));
                    }
                    System.out.print("\n");
                }
            }

            public void setPixel(int x, int y, byte c)
            {
                img[x][y] = (byte) (c & 1);
            }

            public byte getPixel(int x, int y)
            {
                return img[x][y];
            }

            public byte[][] getImage()
            {
                byte[][] out = new byte[img.length][img[0].length];
                for (int i = 0; i < img.length; i++)
                    for (int j = 0; j < img[0].length; j++)
                        out[i][j] = img[i][j];
                return out;
            }

            public int[][] getIntArrImage()
            {
                int[][] out = new int[img.length][img[0].length];
                for (int i = 0; i < img.length; i++)
                    for (int j = 0; j < img[0].length; j++)
                        out[i][j] = img[i][j];
                return out;
            }

            public void setImage(byte[][] newImg)
            {
                for (int i = 0; i < img.length; i++)
                    for (int j = 0; j < img[0].length; j++)
                        img[i][j] = newImg[i][j];
            }

            public void setImage(int[][] newImg)
            {
                for (int i = 0; i < img.length; i++)
                    for (int j = 0; j < img[0].length; j++)
                        img[i][j] = (byte) newImg[i][j];
            }

            public void drawImage(Graphics g, int x, int y, boolean trueWidth,
                int zoom)
            {
                int actualWidth = (size == FONT_SIZE_NORMAL ? 16 : 8);
                g.setColor(FOREGROUND);
                //if trueWidth is false, make sure width is not wider than
                // character
                g.fillRect(x, y, zoom * (trueWidth ? width : actualWidth), zoom
                    * img[0].length);
                g.setColor(BACKGROUND);
                for (int i = 0; i < img.length; i++)
                    for (int j = 0; j < img[0].length; j++)
                        if (img[i][j] == 1)
                            g.fillRect(x + i * zoom, y + j * zoom, zoom, zoom);
            }

            public void drawImage(Image image, int x, int y, boolean trueWidth,
                int zoom)
            {
                drawImage(image.getGraphics(), x, y, trueWidth, zoom);
            }

            public void drawImage(Image image, int x, int y)
            {
                drawImage(image, x, y, true, 1);
            }

            public void drawImage(Graphics g, int x, int y)
            {
                drawImage(g, x, y, true, 1);
            }

            public Image drawImage()
            {
                Image out = new BufferedImage(img.length, img[0].length,
                    BufferedImage.TYPE_4BYTE_ABGR_PRE);
                drawImage(out, 0, 0);
                return out;
            }

            /**
             * Writes char to a <code>byte[]</code> with the first byte being
             * the width, and the rest being 1BPP image(s) of the character.
             * Length depends on size.
             * 
             * @return a <code>byte[]</code> with the width and images of this
             *         character
             * @see #importChar(byte[], int)
             */
            public byte[] exportChar()
            {
                int offset = 1; //first byte is width
                byte[] b;
                if (size == FONT_SIZE_NORMAL)
                {
                    b = new byte[33];
                    offset += HackModule
                        .write1BPPArea(img, b, offset, 16, 0, 0);
                    HackModule.write1BPPArea(img, b, offset, 16, 8, 0);
                }
                else if (size == FONT_SIZE_SMALL)
                {
                    b = new byte[17];
                    HackModule.write1BPPArea(img, b, offset, 16, 0, 0);
                }
                else if (size == FONT_SIZE_TINY)
                {
                    b = new byte[9];
                    HackModule.write1BPPArea(img, b, offset, 8, 0, 0);
                }
                else
                {
                    b = new byte[1];
                }
                b[0] = (byte) width;

                return b;
            }

            /**
             * Imports character from <code>byte[]</code> into this. Takes
             * <code>byte[]</code>'s like what {@link #exportChar()}returns.
             * 
             * @param b <code>byte[]</code> holding the width and 1BPP images
             *            of this character
             * @param off offset in <code>b</code> char starts from
             * @return number of bytes read
             * @see #exportChar()
             */
            public int importChar(byte[] b, int off)
            {
                int offset = off;
                width = b[offset++];
                if (size == FONT_SIZE_NORMAL)
                {
                    img = new byte[16][16];
                    offset += read1BPPArea(img, b, offset, 16, 0, 0);
                    offset += read1BPPArea(img, b, offset, 16, 8, 0);
                }
                else if (size == FONT_SIZE_SMALL)
                {
                    img = new byte[8][16];
                    offset += read1BPPArea(img, b, offset, 16, 0, 0);
                }
                else if (size == FONT_SIZE_TINY)
                {
                    img = new byte[8][8];
                    offset += read1BPPArea(img, b, offset, 8, 0, 0);
                }
                return offset - off;
            }

            /**
             * Checks if character from <code>byte[]</code> is the same as
             * this. Takes <code>byte[]</code>'s like what
             * {@link #exportChar()}returns.
             * 
             * @param b <code>byte[]</code> holding the width and 1BPP images
             *            of this character
             * @param off offset in <code>b</code> char starts from
             * @return number of bytes read or -1 if a difference is found
             * @see #exportChar()
             * @see #importChar(byte[], int)
             */
            public boolean checkChar(byte[] b, int off)
            {
                byte[] curr = exportChar();
                byte[] exp = new byte[curr.length];
                System.arraycopy(b, off, exp, 0, exp.length);
                return Arrays.equals(curr, exp);
            }

            public void restoreChar()
            {
                AbstractRom rom = hm.rom, orgRom = JHack.main.getOrginalRomFile(rom
                    .getRomType());
                rom.write(widthAddress, orgRom.readByte(widthAddress));
                rom.write(address, orgRom.readByte(address, CHAR_SIZES[size]));

                readInfo();
            }

            /**
             * @return Returns the width.
             */
            public int getWidth()
            {
                return width;
            }

            /**
             * @param width The width to set.
             */
            public void setWidth(int width)
            {
                this.width = width;
            }

        }

        /** Number of characters in an Earthbound font. */
        public static final int NUM_CHARS = 96;
        public Character[] chars = new Character[NUM_CHARS];
        private int num, address, widthAddress, size, charSize;
        private HackModule hm;

        public Font(int num, HackModule hm)
        {
            this.num = num;
            this.address = ADDRESSES[num];
            this.widthAddress = WIDTH_ADDRESSES[num];
            this.size = FONT_SIZES[num];
            charSize = 32;
            if (size == FONT_SIZE_SMALL)
                charSize = 16;
            else if (size == FONT_SIZE_TINY)
                charSize = 8;
            this.hm = hm;
            readInfo();
        }

        public void readInfo()
        {
            for (int c = 0; c < chars.length; c++)
                chars[c] = new FontEditor.Font.Character(
                    address + c * charSize, widthAddress + c, size, c, hm);
        }

        public void writeInfo()
        {
            for (int i = 0; i < chars.length; i++)
                chars[i].writeInfo();
        }

        public boolean checkFont(byte[] b)
        {
            int offset = 0;
            for (int i = 0; i < chars.length; i++)
            {
                if (!chars[i].checkChar(b, offset))
                    return false;
                offset += charSize + 1;
            }
            return true;
        }

        public void restoreFont()
        {
            for (int i = 0; i < chars.length; i++)
                chars[i].restoreChar();
        }

        public Image drawFontImage()
        {
            //draw each char as 16x16 px, TINY at 2x size, SMALL half-width

            Image out = new BufferedImage(16 * 16, 6 * 16,
                BufferedImage.TYPE_4BYTE_ABGR_PRE);
            Graphics g = out.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 16 * 16, 6 * 16);
            for (int y = 0; y < 6; y++)
            {
                for (int x = 0; x < 16; x++)
                {
                    g.fillRect(x * 16, y * 16, 16, 16);
                    chars[y * 16 + x].drawImage(out, x * 16, y * 16, false,
                        (size == FONT_SIZE_TINY ? 2 : 1));
                }
            }

            return out;
        }

        public void drawString(String text, Graphics g, int x, int y, int zoom)
        {
            for (int i = 0; i < text.length(); i++)
            {
                Character ch = chars[text.charAt(i) - 0x20];
                ch.drawImage(g, x, y, true, zoom);
                x += (ch.width + (num == BATTLE ? 0 : 1)) * zoom;
            }
        }

        public void drawString(String text, Graphics g, int x, int y)
        {
            drawString(text, g, x, y, 1);
        }
    }

    /** Number of fonts. */
    public final static int NUM_FONTS = 5;

    /** Constants for fonts. */
    public final static int MAIN = 0;
    public final static int SATURN = 1;
    public final static int BIG = 2;
    public final static int BATTLE = 3;
    public final static int TINY = 4;

    /** Constants for font sizes. */
    public final static int FONT_SIZE_NORMAL = 0;
    public final static int FONT_SIZE_SMALL = 1;
    public final static int FONT_SIZE_TINY = 2;

    /**
     * File sizes of exported files indexed by font size constants.
     * 
     * @see #FONT_SIZE_NORMAL
     * @see #FONT_SIZE_SMALL
     * @see #FONT_SIZE_TINY
     */
    public final static int FONT_FILE_SIZES[] = new int[]{3168, 1632, 864};

    /** Holds the fonts of Earthbound. */
    public static Font fonts[] = new Font[NUM_FONTS];
    /**
     * Holds the sizes of the fonts of Earthbound. <code>FONT_SIZES[MAIN]</code>
     * equals the font size constant for the main font (
     * <code>FONT_SIZE_NORMAL</code>).
     */
    public final static int[] FONT_SIZES = new int[]{FONT_SIZE_NORMAL,
        FONT_SIZE_NORMAL, FONT_SIZE_NORMAL, FONT_SIZE_SMALL, FONT_SIZE_TINY};
    public final static int[] ADDRESSES = new int[]{0x210eda, 0x2015b9,
        0x2124fa, 0x211b3a, 0x21219a};
    public final static int[] WIDTH_ADDRESSES = new int[]{0x210e7a, 0x201559,
        0x21249a, 0x211ada, 0x21213a};

    /** Reads font data from the ROM into {@link #fonts}. */
    public static void readFromRom(HackModule hm)
    {
        for (int i = 0; i < fonts.length; i++)
        {
            fonts[i] = new Font(i, hm);
        }
        inited = true;
    }

    private class CharSelector extends AbstractButton implements MouseListener,
        MouseMotionListener
    {
        private int currentChar = 0;
        private static final int CHARS_WIDE = 16, CHARS_HIGH = 6,
                CHAR_SIZE = 32;

        public int getCurrentChar()
        {
            return currentChar;
        }

        public void setCurrentChar(int newChar)
        {
            //only fire ActionPerformed if new tile
            if (currentChar != newChar)
            {
                reHighlight(currentChar, newChar);
                currentChar = newChar;
                this.fireActionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED, this.getActionCommand()));
            }
        }

        private void setCurrentChar(int x, int y)
        {
            //new char
            int nc = ((y / CHAR_SIZE) * CHARS_WIDE) + (x / CHAR_SIZE);
            if (nc < 96 && nc > -1)
                setCurrentChar(nc);
        }

        private void reHighlight(int oldChar, int newChar)
        {
            Graphics g = this.getGraphics();
            g.setColor(Font.Character.BACKGROUND);
            g.fillRect((oldChar % CHARS_WIDE) * CHAR_SIZE,
                (oldChar / CHARS_WIDE) * CHAR_SIZE, CHAR_SIZE, CHAR_SIZE);
            fonts[getCurrentFont()].chars[oldChar].drawImage(g,
                (oldChar % CHARS_WIDE) * CHAR_SIZE, (oldChar / CHARS_WIDE)
                    * CHAR_SIZE, false, (CHAR_SIZE / 16)
                    * (fonts[getCurrentFont()].size == FONT_SIZE_TINY ? 2 : 1));
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect((newChar % CHARS_WIDE) * CHAR_SIZE,
                (newChar / CHARS_WIDE) * CHAR_SIZE, CHAR_SIZE, CHAR_SIZE);
        }

        /**
         * TODO Write javadoc for this method
         *  
         */
        public void repaintCurrent()
        {
            Graphics g = this.getGraphics();
            g.setColor(Font.Character.BACKGROUND);
            g.fillRect((getCurrentChar() % CHARS_WIDE) * CHAR_SIZE,
                (getCurrentChar() / CHARS_WIDE) * CHAR_SIZE, CHAR_SIZE,
                CHAR_SIZE);
            fonts[getCurrentFont()].chars[getCurrentChar()].drawImage(g,
                (getCurrentChar() % CHARS_WIDE) * CHAR_SIZE,
                (getCurrentChar() / CHARS_WIDE) * CHAR_SIZE, false,
                (CHAR_SIZE / 16)
                    * (fonts[getCurrentFont()].size == FONT_SIZE_TINY ? 2 : 1));
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect((getCurrentChar() % CHARS_WIDE) * CHAR_SIZE,
                (getCurrentChar() / CHARS_WIDE) * CHAR_SIZE, CHAR_SIZE,
                CHAR_SIZE);
        }

        public void paint(Graphics g)
        {
            g.drawImage(fonts[getCurrentFont()].drawFontImage()
                .getScaledInstance(CHARS_WIDE * CHAR_SIZE,
                    CHARS_HIGH * CHAR_SIZE, 0), 0, 0, null);
            g.setColor(new Color(255, 255, 0, 128));
            g.fillRect((getCurrentChar() % CHARS_WIDE) * CHAR_SIZE,
                (getCurrentChar() / CHARS_WIDE) * CHAR_SIZE, CHAR_SIZE,
                CHAR_SIZE);
        }

        public void mouseClicked(MouseEvent me)
        {
            setCurrentChar(me.getX(), me.getY());
            if (me.getClickCount() == 2)
                try
                {
                    prevTF
                        .getDocument()
                        .insertString(
                            prevTF.getDocument().getLength(),
                            String
                                .valueOf((char) (((me.getY() / CHAR_SIZE) * CHARS_WIDE)
                                    + (me.getX() / CHAR_SIZE) + 0x20)), null);
                }
                catch (BadLocationException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }

        public void mousePressed(MouseEvent me)
        {
            setCurrentChar(me.getX(), me.getY());
        }

        public void mouseReleased(MouseEvent me)
        {}

        public void mouseEntered(MouseEvent arg0)
        {}

        public void mouseExited(MouseEvent arg0)
        {}

        public void mouseDragged(MouseEvent me)
        {
            if (!(me.getX() < 0 || me.getY() < 0
                || me.getX() > CHARS_WIDE * CHAR_SIZE - 1 || me.getY() > CHARS_HIGH
                * CHAR_SIZE - 1))
                setCurrentChar(me.getX(), me.getY());
        }

        public void mouseMoved(MouseEvent arg0)
        {}

        private String actionCommand = new String();

        public String getActionCommand()
        {
            return this.actionCommand;
        }

        public void setActionCommand(String arg0)
        {
            this.actionCommand = arg0;
        }

        public CharSelector()
        {
            setPreferredSize(new Dimension(CHARS_WIDE * CHAR_SIZE, CHARS_HIGH
                * CHAR_SIZE));
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }
    }

    private class WidthSelector extends JPanel implements MouseListener,
        MouseMotionListener, DocumentListener
    {
        private JPanel line;
        private JTextField tf;

        public WidthSelector()
        {
            setLayout(new BorderLayout());

            tf = HackModule.createSizedJTextField(3);
            tf.getDocument().addDocumentListener(this);
            add(HackModule.getLabeledComponent("Width: ", tf),
                BorderLayout.NORTH);

            line = new JPanel()
            {
                public void paint(Graphics g)
                {
                    g.setColor(Color.GRAY);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(isEnabled() ? Color.RED : Color.DARK_GRAY);
                    int pos = (int) (getSelectedChar().getWidth() * drawingArea
                        .getZoom());
                    g.drawLine(pos, 0, pos, getHeight());
                }
            };
            line.addMouseListener(this);
            line.addMouseMotionListener(this);
            line.setPreferredSize(new Dimension(161, 20));
            line.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            this.add(line, BorderLayout.CENTER);
        }

        public void linePos(int x)
        {
            if (isEnabled())
                tf.setText(Integer.toString(Math.round((float) x / 10)));
        }

        public void mouseClicked(MouseEvent me)
        {
            linePos(me.getX());
        }

        public void mouseEntered(MouseEvent me)
        {}

        public void mouseExited(MouseEvent me)
        {}

        public void mousePressed(MouseEvent me)
        {}

        public void mouseReleased(MouseEvent me)
        {
            linePos(me.getX());
        }

        public void mouseDragged(MouseEvent me)
        {
            linePos(me.getX());
        }

        public void mouseMoved(MouseEvent me)
        {}

        private String actionCommand = new String();

        public String getActionCommand()
        {
            return this.actionCommand;
        }

        public void setActionCommand(String ac)
        {
            this.actionCommand = ac;
        }

        public void setWidthToTF()
        {
            if (isEnabled())
                try
                {
                    int nw = Integer.parseInt(tf.getText());
                    getSelectedChar().setWidth(nw);
                    line.repaint();
                }
                catch (NumberFormatException e)
                {}
        }

        public void changedUpdate(DocumentEvent de)
        {
            setWidthToTF();
        }

        public void insertUpdate(DocumentEvent de)
        {
            setWidthToTF();
        }

        public void removeUpdate(DocumentEvent de)
        {
            setWidthToTF();
        }

        public void updateWidth()
        {
            tf.setText(Integer.toString(getSelectedChar().getWidth()));
        }

        public void setEnabled(boolean en)
        {
            super.setEnabled(en);
            tf.setEnabled(en);
            line.setEnabled(en);
        }
    }

    /**
     * Component for displaying strings as they will appear in EarthBound.
     * 
     * @author AnyoneEB
     */
    public static class StringViewer extends JPanel
    {
        private int font;
        private String text;
        private int zoom = 1;

        /**
         * Constuctor for <code>StringViewer</code>. Calls
         * {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param text initial text
         * @param zoom initial zoom factor
         * @param font initial font
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(String text, int zoom, int font, HackModule hm)
        {
            FontEditor.readFromRom(hm);
            this.text = text;
            this.zoom = zoom;
            this.font = font;

            this.initGraphics();
            this.repaint();
        }

        /**
         * Constuctor for <code>StringViewer</code>. The initial font is set
         * to the main font. Calls {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param text initial text
         * @param zoom initial zoom factor
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(String text, int zoom, HackModule hm)
        {
            this(text, zoom, MAIN, hm);
        }

        /**
         * Constuctor for <code>StringViewer</code>. The initial zoom is set
         * to 1 (actual size). The initial font is set to the main font. Calls
         * {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param text initial text
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(String text, HackModule hm)
        {
            this(text, 1, hm);
        }

        /**
         * Constuctor for <code>StringViewer</code>. The inital text is set
         * to an empty string. Calls {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param zoom initial zoom factor
         * @param font initial font
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(int zoom, int font, HackModule hm)
        {
            this("", zoom, font, hm);
        }

        /**
         * Constuctor for <code>StringViewer</code>. The inital text is set
         * to an empty string. The initial font is set to the main font. Calls
         * {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param zoom initial zoom factor
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(int zoom, HackModule hm)
        {
            this("", zoom, hm);
        }

        /**
         * Constuctor for <code>StringViewer</code>. The inital text is set
         * to an empty string. The initial zoom is set to 1 (actual size). The
         * initial font is set to the main font. Calls
         * {@link FontEditor#readFromRom(HackModule)}.
         * 
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         */
        public StringViewer(HackModule hm)
        {
            this("", hm);
        }

        /**
         * Returns which of EarthBound's five fonts is being used. Possibilities
         * are main, saturn, big, battle, and tiny.
         * 
         * @return Returns the font being used.
         * @see #setEbFont(int)
         * @see FontEditor#MAIN
         * @see FontEditor#SATURN
         * @see FontEditor#BIG
         * @see FontEditor#BATTLE
         * @see FontEditor#TINY
         */
        public int getEbFont()
        {
            return font;
        }

        /**
         * Sets which of EarthBound's five fonts to use. Choices are main,
         * saturn, big, battle, and tiny.
         * 
         * @param font Which font to use.
         * @see #getEbFont()
         * @see FontEditor#MAIN
         * @see FontEditor#SATURN
         * @see FontEditor#BIG
         * @see FontEditor#BATTLE
         * @see FontEditor#TINY
         */
        public void setEbFont(int font)
        {
            this.font = font;
        }

        /**
         * Returns the text currently being previewed. Note that this text may
         * not have actually been drawn if repaint() has not been called since
         * the text last changed.
         * 
         * @return Returns the text being previewed.
         * @see #setText(String)
         */
        public String getText()
        {
            return text;
        }

        /**
         * Sets the text to preview. Note that this does not repaint.
         * 
         * @param text The text to preview.
         * @see #getText()
         */
        public void setText(String text)
        {
            this.text = text;
        }

        /**
         * Returns the current zoom setting. A zoom of 1 means same size as the
         * text will appear in EarthBound. A zoom of 2 means twice the height
         * and twice the width as it will appear in EarthBound.
         * 
         * @return Returns the zoom.
         */
        public int getZoom()
        {
            return zoom;
        }

        /**
         * Sets the zoom. A zoom of 1 means same size as the text will appear in
         * EarthBound. A zoom of 2 means twice the height and twice the width as
         * it will appear in EarthBound.
         * 
         * @param zoom The zoom to set.
         */
        public void setZoom(int zoom)
        {
            this.zoom = zoom;
        }

        /**
         * Sets the background and preferred size to the correct values.
         */
        private void initGraphics()
        {
            this.setBackground(Color.BLACK);
            //            int height;
            //            int width;
            //            try
            //            {
            //                height = FontEditor.fonts[font].size == FONT_SIZE_TINY ? 8 : 16;
            //                width = 0;
            //                for (int i = 0; i < text.length(); i++)
            //                {
            //                    width += FontEditor.fonts[font].chars[text.charAt(i)
            //                        - 0x20].getWidth();
            //                    width++;
            //                    //one extra pixel between chars (TODO is this right
            //                    // spacing?)
            //                }
            //                width--; //no extra spacing after last char
            //
            //            }
            //            catch (NullPointerException e)
            //            {
            //                width = text.length() * 16;
            //            }
            this.setPreferredSize(new Dimension(25 * 16, 16 * zoom));
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        public void paint(Graphics g)
        {
            initGraphics();
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            for (int i = 0; i < text.length(); i++)
            {
                FontEditor.fonts[font].drawString(text, g, 0, 0, zoom);
            }
        }

        /**
         * Attaches a {@link JTextComponent}and changes the text of this
         * whenever the text of it changes. Note that {@link JTextField}and
         * {@link JTextArea}both extend <code>JTextComponent<code>.
         * 
         * @param text <code>JTextComponent<code> to sync the text of this with
         */
        public void attach(final JTextComponent text)
        {
            text.getDocument().addDocumentListener(new DocumentListener()
            {
                public void changed()
                {
                    setText(text.getText());
                    repaint();
                }

                public void changedUpdate(DocumentEvent de)
                {
                    changed();
                }

                public void insertUpdate(DocumentEvent de)
                {
                    changed();
                }

                public void removeUpdate(DocumentEvent de)
                {
                    changed();
                }
            });
        }

        /**
         * Creates a <code>StringViewer</code> with a {@link JTextField}for
         * text entry and a font selector.
         * 
         * @param text initial text
         * @param zoom initial zoom
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         * @return a component for previewing text that allows the user to
         *         select the text and font
         * @see #StringViewer(String, int, HackModule)
         */
        public static JComponent createWithFontSelector(final JTextField text,
            int zoom, HackModule hm)
        {
            final StringViewer sv = new StringViewer(text.getText(), zoom, hm);

            final JComboBox f = new JComboBox(new String[]{"Main", "Saturn",
                "Large", "Battle", "Tiny"});
            f.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    sv.setEbFont(f.getSelectedIndex());
                    sv.repaint();
                }
            });

            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    sv.setText(text.getText());
                    sv.repaint();
                }
            });

            return HackModule.pairComponents(sv, pairComponents(refresh,
                getLabeledComponent("Font: ", f), true), true);
        }

        /**
         * Creates a <code>StringViewer</code> with a {@link JTextField}for
         * text entry and a font selector. Sets the initial zoom to 1 (actual
         * size).
         * 
         * @param text initial text
         * @param hm a <code>HackModule</code> with a loaded EarthBound ROM
         * @return a component for previewing text that allows the user to
         *         select the text and font
         * @see #StringViewer(String, int, HackModule)
         */
        public static JComponent createWithFontSelector(JTextField text,
            HackModule hm)
        {
            return createWithFontSelector(text, 1, hm);
        }
    }
    private JComboBox fontSelector;
    private CharSelector charSelector;
    private IntArrDrawingArea drawingArea;
    private SpritePalette pal;
    private DrawingToolset dt;
    private WidthSelector ws;
    private JLabel ascii;
    private StringViewer prev;
    private JTextField prevTF;

    private static IPSDatabase.DatabaseEntry batFontWidthHack = null;

    public static boolean isBatFontWidthHacked()
    {
        if (batFontWidthHack == null)
            batFontWidthHack = IPSDatabase.getPatch("Battle Font Width Hack");
        if (batFontWidthHack == null)
        {
            throw new NullPointerException("batFontWidthHack is null. "
                + "It is probably missing from ipslisting.xml.");
        }
        else
        {
            return batFontWidthHack.isApplied();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.HackModule#init()
     */
    protected void init()
    {
        batFontWidthHack = IPSDatabase.getPatch("Battle Font Width Hack");
        batFontWidthHack.checkApplied(rom);

        if (JHack.main.getPrefs().getValue("batFontWidthHack") == null
            && batFontWidthHack != null && !isBatFontWidthHacked())
        {
            Box quesBox = new Box(BoxLayout.Y_AXIS);
            quesBox.add(new JLabel(
                "Would you like to apply the battle font width hack?"));
            quesBox.add(new JLabel(
                "This hack by Mr. Accident allows you to have"));
            quesBox.add(new JLabel(
                "choose the character widths for the battle font."));
            JCheckBox saveBatFontWidthPref = new JCheckBox(
                "Always use this selection.");
            quesBox.add(saveBatFontWidthPref);
            int ques = JOptionPane.showConfirmDialog(null, quesBox,
                "Battle font width hack?", JOptionPane.YES_NO_OPTION);
            if (ques == JOptionPane.YES_OPTION)
            {
                batFontWidthHack.apply();
            }
            if (saveBatFontWidthPref.isSelected())
                JHack.main.getPrefs().setValueAsBoolean("batFontWidthHack",
                    ques == JOptionPane.YES_OPTION);
        }
        else if (JHack.main.getPrefs().getValueAsBoolean("batFontWidthHack")
            && !isBatFontWidthHacked())
        {
            batFontWidthHack.apply();
        }
        batFontWidthHack.checkApplied(rom);

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        mainWindow.setResizable(false);

        //Menu
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 's', "ctrl S",
            "apply", this));
        fileMenu.addSeparator();
        fileMenu.add(HackModule.createJMenuItem("Import Font", 'i', null,
            "importFont", this));
        fileMenu.add(HackModule.createJMenuItem("Export Font", 'e', null,
            "exportFont", this));

        mb.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('e');

        editMenu.add(HackModule.createJMenuItem("Undo", 'u', "ctrl Z", "undo",
            this));
        editMenu.addSeparator();
        editMenu.add(HackModule.createJMenuItem("Cut", 't', "ctrl X", "cut",
            this));
        editMenu.add(HackModule.createJMenuItem("Copy", 'c', "ctrl C", "copy",
            this));
        editMenu.add(HackModule.createJMenuItem("Paste", 'p', "ctrl V",
            "paste", this));
        editMenu.add(HackModule.createJMenuItem("Delete", 'd', "DELETE",
            "delete", this));

        mb.add(editMenu);

        mainWindow.setJMenuBar(mb);
        //end menu

        JPanel left = new JPanel(new BorderLayout());

        charSelector = new CharSelector();
        charSelector.setActionCommand("charSelector");
        charSelector.addActionListener(this);
        left.add(HackModule.createFlowLayout(charSelector), BorderLayout.NORTH);

        prev = new StringViewer(2, this);
        prevTF = new JTextField(50);
        prevTF.setText("@This is a test string! :)");
        prev.setText(prevTF.getText());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                prev.setText(prevTF.getText());
                prev.repaint();
            }
        });
        left.add(HackModule.createFlowLayout(pairComponents(pairComponents(
            prev, refresh, true), prevTF, false)), BorderLayout.CENTER);

        left.add(HackModule.pairComponents(HackModule.getLabeledComponent(
            "ASCII Char: ", ascii = new JLabel()), (JComponent) Box
            .createHorizontalGlue(), true), BorderLayout.SOUTH);

        mainWindow.getContentPane().add(left, BorderLayout.WEST);

        Box edit = new Box(BoxLayout.Y_AXIS);

        fontSelector = new JComboBox(new String[]{"Main", "Saturn", "Big",
            "Battle", "Tiny"});
        fontSelector.setActionCommand("fontSelector");
        fontSelector.addActionListener(this);
        edit.add(fontSelector);
        //edit.add(Box.createVerticalStrut(50));

        ws = new WidthSelector();
        edit.add(ws);

        pal = new SpritePalette(2);
        pal.setEditable(false);
        pal.setPalette(new Color[]{Font.Character.BACKGROUND,
            Font.Character.FOREGROUND});

        dt = new DrawingToolset(this);

        drawingArea = new IntArrDrawingArea(dt, pal, this);
        drawingArea.setZoom(10);
        drawingArea.setPreferredSize(new Dimension(160, 160));
        drawingArea.setActionCommand("charDrawingArea");

        edit.add(drawingArea);
        edit.add(HackModule.createFlowLayout(pal));

        edit.add(Box.createVerticalGlue());

        mainWindow.getContentPane().add(edit, BorderLayout.CENTER);

        mainWindow.getContentPane().add(dt, BorderLayout.EAST);

        mainWindow.pack();

        addDataListener(fonts, new ListDataListener()
        {

            public void contentsChanged(ListDataEvent e)
            {
                int curr = getCurrentFont();
                if (e.getIndex0() <= curr && e.getIndex1() >= curr)
                {
                    mainWindow.getContentPane().repaint();
                }
            }

            public void intervalAdded(ListDataEvent e)
            {
            //won't happen
            }

            public void intervalRemoved(ListDataEvent e)
            {
            //won't happen
            }
        });
    }

    public String getVersion()
    {
        return "0.4";
    }

    public String getDescription()
    {
        return "Font Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Based on source code by Tomato";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public void show()
    {
        super.show();

        readFromRom(this);
        prev.repaint();
        updateDrawingArea();

        mainWindow.setVisible(true);
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("fontSelector"))
        {
            int i = fontSelector.getSelectedIndex();
            charSelector.repaint();
            updateDrawingArea();
            prev.setEbFont(i);
            prev.repaint();
            ws.setEnabled(i != BATTLE || isBatFontWidthHacked());
        }
        else if (ae.getActionCommand().equals("charSelector"))
        {
            updateDrawingArea();
        }
        else if (ae.getActionCommand().equals("charDrawingArea"))
        {
            getSelectedChar().setImage(drawingArea.getIntArrImage());
            charSelector.repaintCurrent();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            for (int i = 0; i < fonts.length; i++)
                fonts[i].writeInfo();
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        //undo
        else if (ae.getActionCommand().equals("undo"))
        {
            drawingArea.undo();
            drawingArea.repaint();
        }
        //copy&paste stuff
        else if (ae.getActionCommand().equals("cut"))
        {
            drawingArea.cut();
            drawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            drawingArea.copy();
            drawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            drawingArea.paste();
            drawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            drawingArea.delete();
            drawingArea.repaint();
        }
        else if (ae.getActionCommand().equals("hFlip"))
        {
            drawingArea.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            drawingArea.doVFlip();
        }
        else if (ae.getActionCommand().equals("importFont"))
        {
            importFont(getCurrentFont());
            charSelector.repaint();
            updateDrawingArea();
        }
        else if (ae.getActionCommand().equals("exportFont"))
        {
            exportFont(getCurrentFont());
        }
    }

    private void updateDrawingArea()
    {
        drawingArea.setImage(getSelectedChar().getIntArrImage());
        ws.updateWidth();
        ascii.setText(((char) (getCurrentChar() + 0x20)) + " OR ["
            + (Integer.toHexString(getCurrentChar() + 0x50)) + "]");
    }

    private Font getSelectedFont()
    {
        return fonts[getCurrentFont()];
    }

    private int getCurrentFont()
    {
        return fontSelector.getSelectedIndex();
    }

    private int getCurrentChar()
    {
        return charSelector.getCurrentChar();
    }

    private Character getSelectedChar()
    {
        return getSelectedFont().chars[getCurrentChar()];
    }

    /**
     * Returns the file extension and file type describtion for a font of the
     * given size. Note that the extension returned does <strong>not </strong>
     * include the preceeding period.
     * 
     * @param fontSize one of <code>FONT_SIZE_NORMAL</code>,
     *            <code>FONT_SIZE_SMALL</code>,<code>FONT_SIZE_TINY</code>
     * @return a <code>String[2]</code> of the format {ext, desc}.
     */
    public static String[] fileType(int fontSize)
    {
        switch (fontSize)
        {
            case FONT_SIZE_NORMAL:
                return new String[]{"efn", "Earthbound Font, Normal size"};
            case FONT_SIZE_SMALL:
                return new String[]{"efs", "Earthbound Font, Small size"};
            case FONT_SIZE_TINY:
                return new String[]{"eft", "Earthbound Font, Tiny size"};
            default:
                return null;
        }
    }

    /**
     * Imports the font imformation which was exported to the given
     * <code>byte[]</code> into the specified font.
     * <em>This does not call <code>writeInfo()</code> or save.</em>
     * 
     * @param font number of the font to use
     * @param b exported data to import
     * @see #importFont(int, File)
     * @see #importFont(int)
     * @see #exportFont(int, File)
     * @see Font#writeInfo()
     * @see #fonts
     * @see #MAIN
     * @see #SATURN
     * @see #BIG
     * @see #BATTLE
     * @see #TINY
     */
    public static void importFont(int font, byte[] b)
    {
        int off = 0;
        for (int i = 0; i < fonts[font].chars.length; i++)
        {
            off += fonts[font].chars[i].importChar(b, off);
        }
        notifyDataListeners(fonts, b, font);
    }

    /**
     * Imports the font imformation which was exported to the given
     * <code>File</code> into the specified font.
     * <em>This does not call <code>writeInfo()</code> or save.</em>
     * 
     * @param font number of the font to use
     * @param fn exported data to import
     * @see #importFont(int, byte[])
     * @see #importFont(int)
     * @see #exportFont(int, File)
     * @see Font#writeInfo()
     * @see #fonts
     * @see #MAIN
     * @see #SATURN
     * @see #BIG
     * @see #BATTLE
     * @see #TINY
     */
    public static void importFont(int font, File fn)
    {
        try
        {
            FileInputStream in = new FileInputStream(fn);
            byte[] b = new byte[(int) fn.length()];
            in.read(b);
            in.close();
            importFont(font, b);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Unable to find file to import font from.");
        }
        catch (IOException e)
        {
            System.out.println("Error reading file to import font from.");
        }
    }

    /**
     * Asks the user which normal font they want to select. Used by
     * {@link #importNormal(byte[], Object)}.
     * 
     * @param action text =
     *            <code>"Select which font you wish to " + action + "."</code>
     * @return -1 if user cancels or number of font selected
     */
    private static int askWhichNormalFont(String action)
    {
        String[] options = new String[]{"Main", "Mr. Saturn", "Big"};
        Object opt = JOptionPane.showInputDialog(null,
            "Select which font you wish to " + action + ".", "Select Font",
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (opt == null)
            return -1;
        for (int i = 0; i < options.length; i++)
            if (opt.equals(options[i]))
                return i;
        return -1;
    }

    private static int getFontOfSize(Integer size, String action)
    {
        int i = size.intValue(), font = -1;
        switch (i)
        {
            case FONT_SIZE_NORMAL:
                font = askWhichNormalFont(action);
                break;
            case FONT_SIZE_SMALL:
                font = BATTLE;
                break;
            case FONT_SIZE_TINY:
                font = TINY;
                break;
        }
        return font;
    }

    /**
     * Imports the given <code>byte[]</code> into a font of the specified
     * size. If <code>size.intValue() == {@link #FONT_SIZE_NORMAL}</code>,
     * the user will be asked which font they want to import to. This font could
     * be the "main" font, the "Mr. Saturn" font, or the "big" font. This
     * <strong>does </strong> call {@link Font#writeInfo()}. This method exists
     * to be called by <code>IPSDatabase</code> for "applying" files with .ef?
     * extensions.
     * 
     * @param b exported data to import
     * @param obj <code>Object[]</code>: first element is an
     *            <code>Integer</code> called <code>size</code> (Size of the
     *            font to restore. <code>FONT_SIZE_NORMAL</code>,
     *            <code>FONT_SIZE_SMALL</code>, or
     *            <code>FONT_SIZE_TINY</code> .), second element is a
     *            <code>HackModule</code> containing a reference to the
     *            current EarthBound ROM to init font data from if it has not
     *            been inited.
     * @see net.starmen.pkhack.IPSDatabase#applyFile(String, byte[])
     * @see net.starmen.pkhack.IPSDatabase#registerExtension(String, Method,
     *      Method, Method, Object)
     * @see #askWhichNormalFont(String)
     * @see #importFont(int, byte[])
     * @see #MAIN
     * @see #SATURN
     * @see #BIG
     */
    public static boolean importFont(byte[] b, Object[] obj)
    {
        Integer size = (Integer) obj[0];
        if (!inited)
            readFromRom((HackModule) obj[1]);
        int font = getFontOfSize(size, "import to");
        if (font == -1)
            return false;
        importFont(font, b);
        fonts[font].writeInfo();
        return true;
    }

    /**
     * Imports the given <code>byte[]</code> into the battle font. This
     * <strong>does </strong> call {@link Font#writeInfo()}. This method exists
     * to be called by <code>IPSDatabase</code> for "applying" files with the
     * .efs extension.
     * 
     * @param b exported data to import
     * @param obj ignored, exists as an implementation detail
     * @see net.starmen.pkhack.IPSDatabase#applyFile(String, byte[])
     * @see net.starmen.pkhack.IPSDatabase#registerExtension(String, Method,
     *      Method, Method, Object)
     * @see #importFont(int, byte[])
     * @see #BATTLE
     */
    public static boolean importSmall(byte[] b, Object obj)
    {
        importFont(BATTLE, b);
        fonts[BATTLE].writeInfo();
        return true;
    }

    /**
     * Imports the given <code>byte[]</code> into the tiny font. This
     * <strong>does </strong> call {@link Font#writeInfo()}. This method exists
     * to be called by <code>IPSDatabase</code> for "applying" files with the
     * .eft extension.
     * 
     * @param b exported data to import
     * @param obj ignored, exists as an implementation detail
     * @see net.starmen.pkhack.IPSDatabase#applyFile(String, byte[])
     * @see net.starmen.pkhack.IPSDatabase#registerExtension(String, Method,
     *      Method, Method, Object)
     * @see #importFont(int, byte[])
     * @see #TINY
     */
    public static boolean importTiny(byte[] b, Object obj)
    {
        importFont(TINY, b);
        fonts[TINY].writeInfo();
        return true;
    }

    public static void exportFont(int font, File fn)
    {
        try
        {
            FileOutputStream out = new FileOutputStream(fn);
            for (int i = 0; i < fonts[font].chars.length; i++)
            {
                out.write(fonts[font].chars[i].exportChar());
            }
            out.close();
        }
        catch (IOException e)
        {
            System.out.println("Error writing file to export font to.");
        }
    }

    public static void importFont(int font)
    {
        String[] ft = fileType(fonts[font].size);
        File fn = getFile(false, ft[0], ft[1]);
        if (fn != null)
            importFont(font, fn);
    }

    public static void exportFont(int font)
    {
        String[] ft = fileType(fonts[font].size);
        File fn = getFile(true, ft[0], ft[1]);
        if (fn != null)
            exportFont(font, fn);
    }

    /**
     * Filenames of the exported versions of the orginal Earthbound fonts. These
     * are in the {@link EbHackModule#DEFAULT_BASE_DIR}directory.
     * 
     * @see #MAIN
     * @see #SATURN
     * @see #BIG
     * @see #BATTLE
     * @see #TINY
     */
    /*
     * public static final String[] ORG_FONT_FILENAMES = new
     * String[]{"main.efn", "saturn.efn", "big.efn", "battle.efs", "tiny.eft"};
     *//**
        * Cache of exported versions of orginal Earthbound fonts.
        * 
        * @see #ORG_FONT_FILENAMES
        * @see #MAIN
        * @see #SATURN
        * @see #BIG
        * @see #BATTLE
        * @see #TINY
        */
    /*
     * protected static byte[][] orgFontFiles = new byte[NUM_FONTS][];
     *  
     *//**
        * Gets the orginal Earthbound font <code>byte[]</code> for the
        * requested font. The information is cached in <code>orgFontFiles</code>
        * for future calls to this method.
        * 
        * @param font which font to get the orginal Earthbound version of
        * @return export of the orginal Earthbound version of the specified font
        * @see #importFont(int, byte[])
        * @see #ORG_FONT_FILENAMES
        * @see #orgFontFiles
        */
    /*
     * protected static byte[] getOrgFont(int font) { byte[] b = null; if ((b =
     * orgFontFiles[font]) != null) return b; try {
     * ClassLoader.getSystemResourceAsStream( DEFAULT_BASE_DIR +
     * ORG_FONT_FILENAMES[font]).read( b = new
     * byte[FONT_FILE_SIZES[FONT_SIZES[font]]]); //if we get to the next line, b
     * is loaded correctly orgFontFiles[font] = b; } catch (IOException e) {
     * //IO exception. null will be returned for b e.printStackTrace(); } return
     * b; }
     */

    /**
     * Restores the specified font to the orginal Earthbound version. This does
     * call <code>writeInfo()</code> on the font.
     * 
     * @param font which font to restore
     * @return true if successful, false on failure
     */
    public static void restore(int font)
    {
        fonts[font].restoreFont();
        fonts[font].writeInfo();
        notifyDataListeners(fonts, fonts[font], font);
        /*
         * byte[] b = getOrgFont(font); if (b == null) return false;
         * importFont(font, b); fonts[font].writeInfo(); return true;
         */
    }

    /**
     * Restores a font of size <code>size</code>. If <code>size</code> is
     * <code>FONT_SIZE_NORMAL</code> the user is asked which one of the normal
     * fonts they want to restore. For the other sizes there is only one font of
     * each, so no user interaction is required. If this fails at any step,
     * false will be returned. This method exists to be called by
     * <code>IPSDatabase</code> for "unapplying" files with .ef? extensions.
     * 
     * @param b ignored, exists as an implementation detail
     * @param obj <code>Object[]</code>: first element is an
     *            <code>Integer</code> called <code>size</code> (Size of the
     *            font to restore. <code>FONT_SIZE_NORMAL</code>,
     *            <code>FONT_SIZE_SMALL</code>, or
     *            <code>FONT_SIZE_TINY</code> .), second element is a
     *            <code>HackModule</code> containing a reference to the
     *            current EarthBound ROM to init font data from if it has not
     *            been inited.
     * @return false if any problems occur
     */
    public static boolean restore(byte[] b, Object[] obj)
    {
        Integer size = (Integer) obj[0];
        if (!inited)
            readFromRom((HackModule) obj[1]);
        int font = getFontOfSize(size, "restore to orginal Earthbound version");
        if (font == -1)
            return false;
        restore(font);
        return true;
    }

    /**
     * Checks if any font of the specified size is the same as the given
     * exported font. This method exists to be called by
     * <code>IPSDatabase</code> for "checking" files with .ef? extensions.
     * 
     * @param b an exported font in the correct format for a font of size
     *            <code>size</code>
     * @param obj <code>Object[]</code>: first element is an
     *            <code>Integer</code> called <code>size</code> (Size of the
     *            font to restore. <code>FONT_SIZE_NORMAL</code>,
     *            <code>FONT_SIZE_SMALL</code>, or
     *            <code>FONT_SIZE_TINY</code> .), second element is a
     *            <code>HackModule</code> containing a reference to the
     *            current EarthBound ROM to init font data from if it has not
     *            been inited.
     * @return <code>true</code> if at least one font of the specified size is
     *         has had <code>b</code> imported into it
     * @see #FONT_SIZE_NORMAL
     * @see #FONT_SIZE_SMALL
     * @see #FONT_SIZE_TINY
     */
    public static boolean check(byte[] b, Object[] obj)
    {
        Integer size = (Integer) obj[0];
        if (!inited)
            readFromRom((HackModule) obj[1]);
        int s = size.intValue();
        for (int font = 0; font < fonts.length; font++)
        {
            Font f = fonts[font];
            if (f.size == s)
                if (f.checkFont(b))
                    return true;
        }
        return false;
    }

    private static boolean inited = false;

    public void reset()
    {
        inited = false;
    }

}