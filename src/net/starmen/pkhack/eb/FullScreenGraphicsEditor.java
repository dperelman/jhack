/*
 * Created on Apr 17, 2006
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.CopyAndPaster;
import net.starmen.pkhack.DrawingToolset;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.PrefsCheckBox;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.Undoable;
import net.starmen.pkhack.XMLPreferences;

/**
 * General class for any editor of full screen graphics.
 * 
 * @author AnyoneEB
 * @see net.starmen.pkhack.eb.FullScreenGraphics
 */
public abstract class FullScreenGraphicsEditor extends EbHackModule implements
    ActionListener
{
    protected ArrangementEditor arrangementEditor;
    protected TileSelector tileSelector;
    protected IntArrDrawingArea da;
    protected SpritePalette pal;
    protected DrawingToolset dt;
    protected FocusIndicator fi;

    protected JComboBox screenSelector, subPalSelector;
    protected JTextField name;

    private boolean guiInited;

    /**
     * Returns the class name used to identify this in preferences. For example,
     * {@link LogoScreenEditor} may use <code>"eb.LogoScreenEditor"</code>.
     */
    protected abstract String getClassName();

    public void show()
    {
        readFromRom();
        super.show();

        if (screenSelector == null)
        {
            doSelectAction();
        }
        else
        {
            screenSelector
                .setSelectedIndex(screenSelector.getSelectedIndex() == -1
                    ? 0
                    : screenSelector.getSelectedIndex());
        }

        mainWindow.setVisible(true);
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    /**
     * Returns the number of full screen graphics this editor deals with.
     * 
     * @return the number of full screen graphics this editor deals with
     */
    public abstract int getNumScreens();

    public FullScreenGraphicsEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
        guiInited = false;
    }

    /** Reads the data from the ROM. */
    protected abstract void readFromRom();

    /**
     * Returns the specified screen.
     * 
     * @param num number of the screen; at least 0 inclusive and less than
     *            {@link #getNumScreens()}.
     * @return the {@link FullScreenGraphics} object for the specified screen
     */
    public abstract FullScreenGraphics getScreen(int num);

    /**
     * Returns the name of the specified screen.
     * 
     * @param num number of the screen; at least 0 inclusive and less than
     *            {@link #getNumScreens()}.
     * @return the name for the specified screen
     */
    public abstract String getScreenName(int num);

    /**
     * Returns the array used to hold the names of this set of screens or
     * <code>null</code>.
     * 
     * @return the array of screen names
     */
    public abstract String[] getScreenNames();

    /**
     * Returns the name of the current screen.
     * 
     * @return the name for the specified screen
     */
    public String getCurrentScreenName()
    {
        return getScreenName(getCurrentScreen());
    }

    /**
     * Sets the name of the specified screen.
     * 
     * @param num number of the screen; at least 0 inclusive and less than
     *            {@link #getNumScreens()}.
     * @param newName new name of the screen
     */
    public abstract void setScreenName(int num, String newName);

    /**
     * Sets the name of the current screen.
     * 
     * @param newName new name of the screen
     */
    public void setCurrentScreenName(String newName)
    {
        setScreenName(getCurrentScreen(), newName);
    }

    /**
     * Returns <code>true</code> if subpalettes should be ignored for
     * importing. This defaults to <code>false</code>.
     * 
     * @return <code>false</code>
     */
    protected boolean isSinglePalImport()
    {
        return false;
    }

    protected abstract boolean importData();

    protected abstract boolean exportData();

    protected void doSelectAction()
    {
        if (!getSelectedScreen().readInfo())
        {
            guiInited = false;
            Object opt = JOptionPane.showInputDialog(mainWindow,
                "Error decompressing the " + getCurrentScreenName()
                    + " screen (#" + getCurrentScreen() + ").",
                "Decompression Error", JOptionPane.ERROR_MESSAGE, null,
                new String[]{"Abort", "Retry", "Fail"}, "Retry");
            if (opt == null || opt.equals("Abort"))
            {
                screenSelector.setSelectedIndex((screenSelector
                    .getSelectedIndex() + 1)
                    % screenSelector.getItemCount());
                doSelectAction();
                return;
            }
            else if (opt.equals("Retry"))
            {
                doSelectAction();
                return;
            }
            else if (opt.equals("Fail"))
            {
                getSelectedScreen().initToNull();
            }
        }
        guiInited = true;
        if (name != null)
        {
            name.setText(getCurrentScreenName());
        }
        updatePaletteDisplay();
        tileSelector.repaint();
        arrangementEditor.clearSelection();
        arrangementEditor.repaint();
        updateTileEditor();
    }

    protected int getCurrentScreen()
    {
        return screenSelector.getSelectedIndex();
    }

    protected FullScreenGraphics getSelectedScreen()
    {
        return getScreen(getCurrentScreen());
    }

    protected int getCurrentSubPalette()
    {
        return subPalSelector.getSelectedIndex();
    }

    protected Color[] getSelectedSubPalette()
    {
        return getSelectedScreen().getSubPal(getCurrentSubPalette());
    }

    protected int getCurrentTile()
    {
        return tileSelector.getCurrentTile();
    }

    protected void updatePaletteDisplay()
    {
        pal.setPalette(getSelectedSubPalette());
        pal.repaint();
    }

    protected void updateTileEditor()
    {
        da.setImage(getSelectedScreen().getTile(getCurrentTile()));
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("drawingArea"))
        {
            getSelectedScreen().setTile(getCurrentTile(), da.getByteArrImage());
            tileSelector.repaintCurrent();
            arrangementEditor.repaintCurrentTile();
            fi.setFocus(da);
        }
        else if (ae.getActionCommand().equals("arrangementEditor"))
        {
            fi.setFocus(arrangementEditor);
        }
        else if (ae.getActionCommand().equals("mapSelector"))
        {
            doSelectAction();
        }
        else if (ae.getActionCommand().equals("tileSelector"))
        {
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("subPalSelector"))
        {
            updatePaletteDisplay();
            tileSelector.repaint();
            da.repaint();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            getSelectedScreen().setPaletteColor(pal.getSelectedColorIndex(),
                getCurrentSubPalette(), pal.getNewColor());
            updatePaletteDisplay();
            da.repaint();
            tileSelector.repaint();
            arrangementEditor.repaint();
        }
        // flip
        else if (ae.getActionCommand().equals("hFlip"))
        {
            da.doHFlip();
        }
        else if (ae.getActionCommand().equals("vFlip"))
        {
            da.doVFlip();
        }
        // edit menu
        // undo
        else if (ae.getActionCommand().equals("undo"))
        {
            fi.getCurrentUndoable().undo();
        }
        // copy&paste stuff
        else if (ae.getActionCommand().equals("cut"))
        {
            fi.getCurrentCopyAndPaster().cut();
        }
        else if (ae.getActionCommand().equals("copy"))
        {
            fi.getCurrentCopyAndPaster().copy();
        }
        else if (ae.getActionCommand().equals("paste"))
        {
            fi.getCurrentCopyAndPaster().paste();
        }
        else if (ae.getActionCommand().equals("delete"))
        {
            fi.getCurrentCopyAndPaster().delete();
        }
        else if (ae.getActionCommand().equals("tileSelGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("arrEdGridLines"))
        {
            mainWindow.getContentPane().invalidate();
            arrangementEditor.invalidate();
            arrangementEditor.resetPreferredSize();
            arrangementEditor.validate();
            arrangementEditor.repaint();
            mainWindow.getContentPane().validate();
        }
        else if (ae.getActionCommand().equals("import"))
        {
            importData();

            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.clearSelection();
            arrangementEditor.repaint();
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("export"))
        {
            exportData();
        }
        else if (ae.getActionCommand().equals("importImg"))
        {
            FullScreenGraphicsImporter.importImg(getSelectedScreen(),
                mainWindow, isSinglePalImport());

            updatePaletteDisplay();
            tileSelector.repaint();
            arrangementEditor.clearSelection();
            arrangementEditor.repaint();
            updateTileEditor();
        }
        else if (ae.getActionCommand().equals("apply"))
        {
            // writeInfo() will only write if the screen has been inited
            for (int i = 0; i < getNumScreens(); i++)
                getScreen(i).writeInfo();
            if (name != null)
            {
                setCurrentScreenName(name.getText());
            }
        }
        else if (ae.getActionCommand().equals("close"))
        {
            hide();
        }
        else
        {
            System.err
                .println("FullScreenGraphicsEditor.actionPerformed: ERROR: unhandled "
                    + "action command: \"" + ae.getActionCommand() + "\"");
        }
    }

    protected abstract int getTileSelectorWidth();

    protected abstract int getTileSelectorHeight();

    protected void initMenu()
    {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        fileMenu.add(HackModule.createJMenuItem("Apply Changes", 'y', "ctrl S",
            "apply", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import...", 'i', null,
            "import", this));
        fileMenu.add(HackModule.createJMenuItem("Export...", 'e', null,
            "export", this));

        fileMenu.addSeparator();

        fileMenu.add(HackModule.createJMenuItem("Import Image...", 'm', null,
            "importImg", this));
        // fileMenu.add(HackModule.createJMenuItem("Export Image...", 'x', null,
        // "exportImg", this));

        mb.add(fileMenu);

        JMenu editMenu = HackModule.createEditMenu(this, true);

        editMenu.addSeparator();

        editMenu.add(HackModule.createJMenuItem("H-Flip", 'h', null, "hFlip",
            this));
        editMenu.add(HackModule.createJMenuItem("V-Flip", 'v', null, "vFlip",
            this));

        mb.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Enable Tile Selector Grid Lines",
            prefs, getClassName() + ".tileSelector.gridLines", false, 't',
            null, "tileSelGridLines", this));
        optionsMenu.add(new PrefsCheckBox(
            "Enable Arrangement Editor Grid Lines", prefs, getClassName()
                + ".arrEditor.gridLines", false, 'a', null, "arrEdGridLines",
            this));
        mb.add(optionsMenu);

        mainWindow.setJMenuBar(mb);
    }

    /**
     * The subclass should layout the components here and return a JComponent
     * containing all of them. It is safe to assume that the components are not
     * null.
     * 
     * @return a <code>JComponent</code> of the entire layout
     */
    protected abstract JComponent layoutComponents();

    protected void initComponents()
    {
        tileSelector = new FullScreenTileSelector();
        tileSelector.resetPreferredSize();
        tileSelector.setActionCommand("tileSelector");
        tileSelector.addActionListener(this);

        arrangementEditor = new FullScreenArrangementEditor();
        arrangementEditor.setActionCommand("arrangementEditor");
        arrangementEditor.addActionListener(this);

        dt = new DrawingToolset(this);

        pal = new SpritePalette(getScreen(0).getSubPaletteSize(), true);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);

        da = new IntArrDrawingArea(dt, pal, this);
        da.setActionCommand("drawingArea");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(81, 81));

        if (getScreenNames() == null)
        {
            screenSelector = createJComboBoxFromArray(new Object[getNumScreens()]);
            screenSelector.addActionListener(this);
        }
        else
        {
            screenSelector = createComboBox(getScreenNames(), this);
        }
        screenSelector.setActionCommand("mapSelector");

        name = new JTextField(15);

        subPalSelector = createJComboBoxFromArray(new Object[getScreen(0)
            .getNumSubPalettes()], false);
        subPalSelector.setSelectedIndex(0);
        subPalSelector.setActionCommand("subPalSelector");
        subPalSelector.addActionListener(this);

        fi = new FocusIndicator();
    }

    protected void init()
    {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(getDescription());

        initMenu();
        initComponents();

        mainWindow.getContentPane().add(new JScrollPane(layoutComponents()),
            BorderLayout.CENTER);

        mainWindow.pack();
    }

    /**
     * Returns the subpalette offset used in the arrangement. This value is
     * subtracted from the subpalette number when making an arrangement number.
     * By default this is 2.
     * 
     * @return the subpalette offset for the arrangement
     */
    protected int getSubPalOffset()
    {
        return 2;
    }

    private class FullScreenArrangementEditor extends ArrangementEditor
    {
        public FullScreenArrangementEditor()
        {
            super();
            this.setPreferredSize(new Dimension(getTilesWide()
                * (getTileSize() * getZoom()), getTilesHigh()
                * (getTileSize() * getZoom())));
        }

        public short makeArrangementNumber(int tile, int subPalette,
            boolean hFlip, boolean vFlip)
        {
            return super.makeArrangementNumber(tile, isSinglePalImport()
                ? 0
                : subPalette - getSubPalOffset(), hFlip, vFlip);
        }

        /*
         * public int getTileOfArr(int arr) { return arr & 0x00ff; }
         */

        protected int getCurrentTile()
        {
            return tileSelector.getCurrentTile();
        }

        protected void setCurrentTile(int tile)
        {
            tileSelector.setCurrentTile(tile);
        }

        protected int getTilesWide()
        {
            return 32;
        }

        protected int getTilesHigh()
        {
            return 28;
        }

        protected int getTileSize()
        {
            return 8;
        }

        protected int getZoom()
        {
            return 2;
        }

        protected boolean isDrawGridLines()
        {
            try
            {
                return prefs.getValueAsBoolean(getClassName()
                    + ".arrEditor.gridLines");
            }
            catch (NullPointerException e)
            {
                try
                {
                    return JHack.main.getPrefs().getValueAsBoolean(
                        getClassName() + ".arrEditor.gridLines");
                }
                catch (NullPointerException npe)
                {
                    return false;
                }
            }
        }

        protected boolean isEditable()
        {
            return true;
        }

        protected boolean isGuiInited()
        {
            return guiInited;
        }

        protected int getCurrentSubPalette()
        {
            return FullScreenGraphicsEditor.this.getCurrentSubPalette();
        }

        protected short getArrangementData(int x, int y)
        {
            return getSelectedScreen().getArrangementData(x, y);
        }

        protected short[][] getArrangementData()
        {
            return getSelectedScreen().getArrangementData();
        }

        protected void setArrangementData(int x, int y, short data)
        {
            getSelectedScreen().setArrangementData(x, y, data);
        }

        protected void setArrangementData(short[][] data)
        {
            getSelectedScreen().setArrangementData(data);
        }

        protected Image getTileImage(int tile, int subPal, boolean hFlip,
            boolean vFlip)
        {
            return getSelectedScreen().getTileImage(tile,
                isSinglePalImport() ? getCurrentSubPalette() : subPal, hFlip,
                vFlip);
        }
    }

    private class FullScreenTileSelector extends TileSelector
    {
        public int getTilesWide()
        {
            try
            {
                return getTileSelectorWidth();
            }
            catch (NullPointerException npe)
            {
                return 16;
            }
        }

        public int getTilesHigh()
        {
            try
            {
                return getTileSelectorHeight();
            }
            catch (NullPointerException npe)
            {
                return 16;
            }
        }

        public int getTileSize()
        {
            return 8;
        }

        public int getZoom()
        {
            return 2;
        }

        public boolean isDrawGridLines()
        {
            try
            {
                return prefs.getValueAsBoolean(getClassName()
                    + ".tileSelector.gridLines");
            }
            catch (NullPointerException e)
            {
                try
                {
                    return JHack.main.getPrefs().getValueAsBoolean(
                        getClassName() + ".tileSelector.gridLines");
                }
                catch (NullPointerException npe)
                {
                    return false;
                }
            }
        }

        public int getTileCount()
        {
            return getScreen(0).getNumTiles();
        }

        public Image getTileImage(int tile)
        {
            return getSelectedScreen().getTileImage(tile,
                getCurrentSubPalette());
        }

        protected boolean isGuiInited()
        {
            return guiInited;
        }
    }

    protected abstract int focusDaDir();

    protected abstract int focusArrDir();

    private class FocusIndicator extends AbstractButton implements
        FocusListener, MouseListener
    {
        // true = tile editor, false = arrangement editor
        private boolean focus = true;

        public Component getCurrentFocus()
        {
            return (focus ? (Component) da : (Component) arrangementEditor);
        }

        public Undoable getCurrentUndoable()
        {
            return (focus ? (Undoable) da : (Undoable) arrangementEditor);
        }

        public CopyAndPaster getCurrentCopyAndPaster()
        {
            return (focus
                ? (CopyAndPaster) da
                : (CopyAndPaster) arrangementEditor);
        }

        private void cycleFocus()
        {
            focus = !focus;
            repaint();
        }

        private void setFocus(Component c)
        {
            focus = c == da;
            repaint();
        }

        public void focusGained(FocusEvent fe)
        {
            System.out.println("FocusIndicator.focusGained(FocusEvent)");
            setFocus(fe.getComponent());
            repaint();
        }

        public void focusLost(FocusEvent arg0)
        {}

        public void mouseClicked(MouseEvent me)
        {
            cycleFocus();
        }

        public void mousePressed(MouseEvent arg0)
        {}

        public void mouseReleased(MouseEvent arg0)
        {}

        public void mouseEntered(MouseEvent arg0)
        {}

        public void mouseExited(MouseEvent arg0)
        {}

        private void rev(int[] arr)
        {
            for (int i = 0; i < arr.length; i++)
            {
                arr[i] = 50 - arr[i];
            }
        }

        private void paintDir(Graphics g, int dir)
        {
            int[] y = new int[]{40, 10, 10, 00, 10, 10, 40};
            int[] x = new int[]{22, 22, 15, 25, 35, 28, 28};

            switch (dir)
            {
                case SwingConstants.LEFT:
                    g.fillPolygon(y, x, 7);
                    break;
                case SwingConstants.RIGHT:
                    rev(x);
                    g.fillPolygon(y, x, 7);
                    break;
                case SwingConstants.BOTTOM:
                    rev(y);
                    g.fillPolygon(x, y, 7);
                    break;
                case SwingConstants.TOP:
                default:
                    g.fillPolygon(x, y, 7);
                    break;
            }
        }

        public void paint(Graphics g)
        {
            paintDir(g, focus ? focusDaDir() : focusArrDir());
        }

        public FocusIndicator()
        {
            da.addFocusListener(this);
            arrangementEditor.addFocusListener(this);

            this.addMouseListener(this);

            this.setPreferredSize(new Dimension(50, 50));

            this
                .setToolTipText("This arrow points to which component will recive menu commands. "
                    + "Click to change.");
        }
    }
}
