package net.starmen.pkhack;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import net.starmen.pkhack.eb.EbHackModule;

/**
 * Main GUI class of JHack. Shows the main window with the list of module
 * buttons. This is called by {@link JHack#main(String[])}.
 * 
 * @author AnyoneEB
 * @version 0.5.6
 */
//Made by AnyoneEB.
//Code released under the GPL - http://www.gnu.org/licenses/gpl.txt
public class MainGUI implements ActionListener, WindowListener
{
    private JFrame mainWindow = new JFrame();
    private AbstractRom rom, orgRom = null;
    private ArrayList moduleList = new ArrayList();
    private boolean showload = false;
    private JFrame loadingDialog;
    private JProgressBar loadingBar;
    private JMenu revertMenu;
    private JMenuItem[] recentLoadsItems = new JMenuItem[5];
    private XMLPreferences prefs;
    private ArrayList recentLoads = new ArrayList();
    private final static String DATE_FORMAT = "yyyyMMddHHmmss";
    private Box buttons;
    private JButton[] modButtons;

    private String[] addToArr(String[] in, String arg)
    {
        String[] out = new String[in.length + 1];
        for (int i = 0; i < in.length; i++)
        {
            out[i] = in[i];
        }
        out[in.length] = arg;
        return out;
    }

    private int getModuleCount()
    {
        return moduleList.size();
    }

    private HackModule getModuleAt(int i)
    {
        return (HackModule) moduleList.get(i);
    }

    /**
     * Inits HackModules.
     * 
     * @return String[] indicating after which modules there should be
     *         separators and their titles.
     */
    private String[] initModules()
    {
        //HackModule.initBigArrays();
        String[] moduleNames = new String[0]; //list of module class names

        try
        {
            moduleNames = new CommentedLineNumberReader(
                new InputStreamReader(
                    ClassLoader
                        .getSystemResourceAsStream("net/starmen/pkhack/modulelist.txt")))
                .readUsedLines();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            moduleNames = new String[]{"DontCare"};
        }

        if (showload)
        {
            loadingBar.setMaximum(moduleNames.length);
            loadingBar.setStringPainted(true);
        }

        String[] spacingList = new String[moduleNames.length + 1];
        Arrays.fill(spacingList, null);
        String tmp;
        //modules = new HackModule[moduleList.length];
        moduleList.ensureCapacity(moduleNames.length);
        for (int i = 0; i < moduleNames.length; i++)
        {
            if (showload)
            {
                loadingBar.setValue(i);
            }
            try
            {
                /*
                 * If moduleName is like ------- add a separator after last
                 * module added
                 */
                if (moduleNames[i].startsWith("-"))
                {
                    try
                    {
                        spacingList[moduleList.size()] = new StringTokenizer(
                            moduleNames[i], "-", false).nextToken();
                    }
                    catch (NoSuchElementException nsee)
                    {
                        spacingList[moduleList.size()] = "";
                    }
                }
                else
                    moduleList.add((HackModule) Class.forName(
                        "net.starmen.pkhack." + moduleNames[i]).getConstructor(
                        new Class[]{AbstractRom.class, XMLPreferences.class})
                        .newInstance(new Object[]{rom, prefs}));
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            catch (NoSuchMethodException e)
            {
                e.printStackTrace();
            }
        }
        moduleList.trimToSize();

        return spacingList;
    }

    private JMenuItem save, saveAs, useFileIO;

    private void initGraphics()
    {
        //show loading dialog first
        if (showload)
        {
            loadingDialog = new JFrame("Loading...");
            loadingBar = new JProgressBar();
            loadingDialog.getContentPane().add(loadingBar);
            loadingDialog.setVisible(true);
            loadingDialog.invalidate();
            loadingDialog.setSize(200, 60);
            loadingDialog.setResizable(false);
            loadingDialog.validate();
        }

        loadPrefs();

        //main init stuff
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion());

        mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainWindow.addWindowListener(this);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');
        fileMenu.add(HackModule.createJMenuItem("Open ROM...", 'o', "ctrl O",
            "load", this));
        fileMenu.add(HackModule.createJMenuItem("Close ROM", 'c', "ctrl F4",
            "closeROM", this));
        fileMenu.add(save = HackModule.createJMenuItem("Save ROM", 's',
            "ctrl S", "save", this));
        fileMenu.add(saveAs = HackModule.createJMenuItem("Save ROM As...", 'a',
            "ctrl A", "saveAs", this));
        fileMenu.add(new JSeparator());
        revertMenu = new JMenu("Revert");
        revertMenu.setMnemonic('r');
        fileMenu.add(revertMenu);
        refreshRevertMenu();
        fileMenu.add(new JSeparator());

        for (int i = 0; i < recentLoadsItems.length; i++)
        {
            recentLoadsItems[i] = new JMenuItem();
            recentLoadsItems[i].setMnemonic(Integer.toString(i + 1).charAt(0));
            recentLoadsItems[i].setActionCommand("recentLoad" + i);
            recentLoadsItems[i].addActionListener(this);
            recentLoadsItems[i].setVisible(false);
            fileMenu.add(recentLoadsItems[i]);
        }
        refreshRecentLoads();

        fileMenu.add(new JSeparator());
        fileMenu.add(HackModule.createJMenuItem("Exit", 'x', "alt F4", "exit",
            this));
        menuBar.add(fileMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');
        optionsMenu.add(new PrefsCheckBox("Display Console Dialog", prefs,
            "consoleDialog", false, 'c', null, "consoleDialog", this));
        optionsMenu.add(new PrefsCheckBox("Hide Error Console", prefs,
        		"noErrorDialog", false, 'e', null, "noErrorDialog", this));
        JMenu toolkitMenu = new JMenu("Look & Feel");
        toolkitMenu.setMnemonic('f');
        UIManager.LookAndFeelInfo[] lafi = UIManager.getInstalledLookAndFeels();
        ButtonGroup lafbg = new ButtonGroup();
        for (int i = 0; i < lafi.length; i++)
        {
            if (lafi[i].getName().equals("CDE/Motif"))
                continue;
            JRadioButtonMenuItem laf = new JRadioButtonMenuItem(lafi[i]
                .getName());
            laf.setSelected(UIManager.getLookAndFeel().getName().equals(
                lafi[i].getName()));
            laf.setActionCommand("LAF_" + lafi[i].getClassName());
            laf.addActionListener(this);
            lafbg.add(laf);
            toolkitMenu.add(laf);
        }
        optionsMenu.add(toolkitMenu);
        optionsMenu.add(new PrefsCheckBox("Use Hex Numbers", prefs,
            "useHexNumbers", true, 'h'));
        optionsMenu.addSeparator();
        optionsMenu.add(useFileIO = new PrefsCheckBox("Use Direct File IO",
            prefs, "useDirectFileIO", false, 'd', null, "directio", this));
        optionsMenu.add(new PrefsCheckBox("Load Last ROM on Startup", prefs,
            "autoLoad", true, 'l'));
        optionsMenu.add(HackModule.createJMenuItem("Change Default ROM...",
            'c', null, "selectDefROM", this));
        optionsMenu.addSeparator();
        optionsMenu.add(new PrefsCheckBox("Auto Check for Updates", prefs,
            "checkVersion", true, 'u'));
        JMenu compType = new JMenu("Prefered Compression Format");
        compType.setMnemonic('p');
        String[][] compTypes = new String[][]{{"zip", "ZIP, most common"},
            {"bz2", "BZip2"}, {"rar", "RAR"}, {"7z", "7-Zip, best"}};
        ButtonGroup compTypeGroup = new ButtonGroup();
        String formatPref = prefs.getValue("updateFormat");
        for (int i = 0; i < compTypes.length; i++)
        {
            JRadioButtonMenuItem type = new JRadioButtonMenuItem("."
                + compTypes[i][0] + " (" + compTypes[i][1] + ")");
            type.setMnemonic(compTypes[i][0].charAt(0));
            type.setActionCommand("comptype_" + compTypes[i][0]);
            type.addActionListener(this);
            compType.add(type);
            compTypeGroup.add(type);
            if ((formatPref == null && i == 0)
                || (formatPref != null && formatPref.equals(compTypes[i][0])))
                type.setSelected(true);
        }
        optionsMenu.add(compType);

        menuBar.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('h');
        helpMenu.add(HackModule.createJMenuItem("About...", 'a', null, "about",
            this));
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(helpMenu);

        mainWindow.setJMenuBar(menuBar);

        //add buttons
        String[] spList = initModules();
        /*
         * int spaces = 0; for (int i = 0; i < spList.length; i++) if
         * (spList[i])
         */
        buttons = new Box(BoxLayout.Y_AXIS);
        Box currBox = null;
        HeadingLabel prevLabel = null;
        //buttons.setLayout(new GridLayout(getModuleCount() + spaces, 1));
        JScrollPane scroll = new JScrollPane(buttons,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        modButtons = new JButton[getModuleCount()];
        for (int i = 0; i < getModuleCount(); i++)
        {
            if (spList[i] != null)
            {
                if (currBox != null)
                {
                    buttons.add(currBox);
                    currBox.setVisible(false);
                    if (prevLabel != null)
                        prevLabel.setCollapsableArea(currBox);
                }
                currBox = new Box(BoxLayout.Y_AXIS);
                if (spList[i] == "")
                    buttons.add(Box.createVerticalStrut(16));
                else
                {
                    HeadingLabel t = new HeadingLabel(spList[i],
                        SwingConstants.CENTER);
                    t.setMaximumSize(new Dimension(274, 16));
                    buttons.add(t);
                    prevLabel = t;
                }
            }

            modButtons[i] = new JButton(getModuleAt(i).getDescription());
            if (getModuleAt(i).getIcon() != null)
            {
                modButtons[i].setIcon(getModuleAt(i).getIcon());
            }
            modButtons[i].setActionCommand("module" + i);
            modButtons[i].addActionListener(this);
            modButtons[i].setMaximumSize(new Dimension(274, 26));
            currBox.add(modButtons[i]);
        }
        if (currBox != null)
        {
            currBox.setVisible(false);
            buttons.add(currBox);
            if (prevLabel != null)
                prevLabel.setCollapsableArea(currBox);
        }
        mainWindow.getContentPane().add(scroll);

        if (showload)
        {
            loadingDialog.setVisible(false);
        }
        mainWindow.setVisible(true);
        mainWindow.invalidate();
        mainWindow.setBounds(200, 200, 300, 400);
        mainWindow.validate();
        mainWindow.setResizable(false);
    }

    private class HeadingLabel extends JLabel
    {
        private JComponent ca;

        private Color regCol, hoverCol;

        public HeadingLabel(String label, int align, Color col, Color hover)
        {
            super(label, align);

            this.regCol = col;
            this.hoverCol = hover;

            this.setForeground(col);

            this.addMouseListener(new MouseListener()
            {

                public void mouseClicked(MouseEvent arg0)
                {
                    if (ca != null)
                        ca.setVisible(!ca.isShowing());
                    if (ca.isVisible())
                        setFont(getFont().deriveFont(Font.BOLD));
                    else
                        setFont(getFont().deriveFont(0));
                    buttons.doLayout();
                    buttons.repaint();
                    ca.doLayout();
                    ((JComponent) mainWindow.getContentPane().getComponents()[0])
                        .updateUI();
                }

                public void mouseEntered(MouseEvent arg0)
                {
                    if (ca != null)
                        setForeground(hoverCol);
                }

                public void mouseExited(MouseEvent arg0)
                {
                    if (ca != null)
                        setForeground(regCol);
                }

                public void mousePressed(MouseEvent arg0)
                {}

                public void mouseReleased(MouseEvent arg0)
                {}
            });
        }

        public HeadingLabel(String label, int align)
        {
            this(label, align, Color.BLUE, Color.RED);
        }

        public void setCollapsableArea(JComponent ca)
        {
            this.ca = ca;
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    public MainGUI(boolean visible)
    {
    	if (visible)
    		initGraphics();
    	else
    		loadPrefs();
    }
    
    /**
     * Creates a new <code>MainGUI</code> and shows its main window.
     */
    public MainGUI()
    {
        initGraphics();
    }

    /**
     * Returns the name of this program ("JHack").
     * 
     * @return "JHack".
     */
    public static String getDescription() //Return one line description of
    // class
    {
        return "PK Hack";
    }

    /**
     * Returns the version of JHack as a <code>String</code>. Can have any
     * number of numbers and dots ex. "0.3.3.5".
     * 
     * @return The version of this class.
     */
    public static String getVersion()
    {
        return "0.5.6";
    }

    /**
     * Returns the credits for this class.
     * 
     * @return The credits for this class.
     */
    public static String getCredits() //Return who made it
    {
        return "Written by AnyoneEB\n" + "Various lists from PK Hack website\n"
            + "Icons from PK Hack v0.2 source";
    }

    private void exit()
    {
        if (rom.isLoaded && !rom.isDirectFileIO())
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want to save before exiting?", "Save?",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (ques == JOptionPane.CANCEL_OPTION)
            {
                return;
            }
            else if (ques == JOptionPane.YES_OPTION)
            {
                rom.saveRom();
            }
        }
        doBackup();
        savePrefs();

        JHack.out.stop();
        JHack.err.stop();

        System.exit(0);
    }

    /**
     * Saves the preferences to JHack.xml.
     */
    private void savePrefs()
    {
        this.getPrefs().save();
    }

    /**
     * Loads the preferences from JHack.ini. Case-insensive.
     */
    private void loadPrefs()
    {
        rom = new RomMem();
        File xmlFile = new File(JHack.JHACK_DIR.toString() + File.separator
            + "JHackPrefs.xml");
        HashMap iniPrefs = new HashMap();
        if (!xmlFile.exists())
        {
            try
            {
                String[] input = new CommentedLineNumberReader("JHack.ini")
                    .readUsedLines();
                for (int i = 0; i < input.length; i++)
                {
                    String[] split = input[i].split("=");
                    split[0] = split[0].trim();
                    split[1] = split[1].trim();
                    iniPrefs.put(split[0], split[1]);
                }
            }
            catch (FileNotFoundException e)
            {
                iniPrefs.put("useHexNumbers", Boolean.toString(true));
                iniPrefs.put("useDirectFileIO", Boolean.toString(false));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                iniPrefs.put("useHexNumbers", Boolean.toString(true));
                iniPrefs.put("useDirectFileIO", Boolean.toString(false));
            }
        }
        this.prefs = new XMLPreferences(xmlFile, iniPrefs);
        try
        {
            if (this.getPrefs().getValueAsBoolean("useDirectFileIO"))
            {
                rom = new RomFileIO();
            }
        }
        catch (NullPointerException e)
        {}
        try
        {
            try
            {
                UIManager.setLookAndFeel(prefs.getValue("laf"));
            }
            catch (NullPointerException e)
            {
                System.out
                    .println("No look and feel preference found, using default.");
                UIManager.setLookAndFeel(UIManager
                    .getSystemLookAndFeelClassName());
            }
            finally
            {
                JHack.out.updateUI();
                JHack.err.updateUI();
            }
        }
        catch (Exception e)
        {
            System.out.println("Error loading look and feel preference.");
            e.printStackTrace();
        }
        if (!this.getPrefs().hasValue("checkVersion"))
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want JHack to check for updates\n"
                    + "automatically on startup?\n"
                    + "NOTE: This accesses AnyoneEB's server and\n"
                    + "could (but won't) be used to gather usage\n"
                    + "statistics if enabled.",
                "Check for updates automatically?", JOptionPane.YES_NO_OPTION);
            this.getPrefs().setValueAsBoolean("checkVersion",
                ques == JOptionPane.YES_OPTION);
        }
        if (this.getPrefs().hasValue("consoleDialog"))
        {
            if (!JHack.isUseConsole())
                JHack.out.setEnabled(this.getPrefs().getValueAsBoolean(
                    "consoleDialog"));
        }
        if (this.getPrefs().hasValue("noErrorDialog"))
        {
        	if (!JHack.isUseConsole())
        		JHack.err.setEnabled(! this.getPrefs().getValueAsBoolean(
        				"noErrorDialog"));
        }
        //convert expRomPath and orgRomPath to game specific names
        if (this.getPrefs().hasValue("expRomPath"))
        {
            this.getPrefs().setValue("Earthbound.expRomPath",
                getPrefs().getValue("expRomPath"));
            this.getPrefs().removeValue("expRomPath");
        }
        if (this.getPrefs().hasValue("orgRomPath"))
        {
            this.getPrefs().setValue("Earthbound.orgRomPath",
                getPrefs().getValue("orgRomPath"));
            this.getPrefs().removeValue("orgRomPath");
        }
        if (this.getPrefs().getValueAsBoolean("checkVersion"))
        {
            askToUpdate();
        }
        loadRecentLoadsPref();

        //delete old prefs
        new File("JHack.ini").delete();
        new File("JHackPrefs.xml").delete();
        //new File("preferences.dtd").delete();
    }

    private String getModuleCredits()
    {
        String returnValue = "\n\n" + EbHackModule.getLibcompCreditsLine()
            + "\n\n" + AbstractRom.getDescription() + " "
            + AbstractRom.getVersion() + "\n" + AbstractRom.getCredits();
        for (int i = 0; i < getModuleCount(); i++)
        {
            returnValue += "\n\n" + getModuleAt(i).getDescription() + " "
                + getModuleAt(i).getVersion() + "\n"
                + getModuleAt(i).getCredits();
        }
        return returnValue;
    }

    private String getFullCredits()
    {
        return MainGUI.getDescription() + " " + MainGUI.getVersion() + "\n"
            + MainGUI.getCredits() + this.getModuleCredits();
    }

    private JScrollPane createScollingLabel(String text)
    {
        int emptyLine = new JLabel("newline").getPreferredSize().height;
        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        text = text.replaceAll("\n\n", "\nnewline\n");

        StringTokenizer st = new StringTokenizer(text, "\n");
        JLabel temp;
        while (st.hasMoreTokens())
        {
            temp = new JLabel(st.nextToken());
            if (temp.getText().equals("newline"))
            {
                labels.add(Box.createVerticalStrut(emptyLine));
            }
            else
            {
                labels.add(temp);
            }
        }

        JScrollPane out = new JScrollPane(labels,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        out.setPreferredSize(new Dimension(out.getPreferredSize().width, 200));

        return out;
    }

    public void windowClosed(WindowEvent we)
    {
        exit();
    }

    public void windowActivated(WindowEvent we)
    {}

    public void windowClosing(WindowEvent we)
    {
        exit();
    }

    public void windowDeactivated(WindowEvent we)
    {}

    public void windowDeiconified(WindowEvent we)
    {}

    public void windowIconified(WindowEvent we)
    {}

    public void windowOpened(WindowEvent we)
    {}

    private static File getFileOfSize(final long size1, final long size2,
        String msg, boolean repeat)
    {
        JFileChooser jfc = new JFileChooser(AbstractRom.getDefaultDir());
        jfc.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                if ((((f.getAbsolutePath().toLowerCase().endsWith(".smc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".sfc") || f
                    .getAbsolutePath().toLowerCase().endsWith(".fig")) && (f
                    .length() == size1 || f.length() == size2)) || f
                    .isDirectory())
                    && f.exists())
                {
                    return true;
                }
                return false;
            }

            public String getDescription()
            {
                return "SNES ROMs (*.smc, *.sfc, *.fig)";
            }
        });
        do
        {
            JOptionPane.showMessageDialog(null, msg, "Where is this file?",
                JOptionPane.QUESTION_MESSAGE);
        }
        while (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION
            && repeat);
        return jfc.getSelectedFile();
    }

    private static File getFile(String msg, boolean repeat)
    {
        JFileChooser jfc = new JFileChooser(AbstractRom.getDefaultDir());
        jfc.setFileFilter(new FileFilter()
        {
            public boolean accept(File f)
            {
                if ((((f.getAbsolutePath().toLowerCase().endsWith(".smc")
                    || f.getAbsolutePath().toLowerCase().endsWith(".sfc") || f
                    .getAbsolutePath().toLowerCase().endsWith(".fig"))) || f
                    .isDirectory())
                    && f.exists())
                {
                    return true;
                }
                return false;
            }

            public String getDescription()
            {
                return "SNES ROMs (*.smc, *.sfc, *.fig)";
            }
        });
        do
        {
            JOptionPane.showMessageDialog(null, msg, "Where is this file?",
                JOptionPane.QUESTION_MESSAGE);
        }
        while (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION
            && repeat);
        return jfc.getSelectedFile();
    }

    /**
     * Creates a exp'd ROM from the org ROM pref and sets the exp ROM pref.
     * 
     * @return expanded AbstractRom object.
     */
    private AbstractRom orgRomToExp(String romType)
    {
        String orgRom = this.getPrefs().getValue(romType + ".orgRomPath");
        String expRom = orgRom.substring(0, orgRom.length() - 3) + "expanded."
            + orgRom.substring(orgRom.length() - 3);

        String dd = AbstractRom.getDefaultDir();

        AbstractRom r = new RomMem();
        r.loadRom(new File(orgRom));
        r.expandEx();
        r.saveRom(new File(expRom));

        this.getPrefs().setValue(romType + ".expRomPath", expRom);

        AbstractRom.setDefaultDir(dd);

        return r;
    }

    /**
     * Asks the user for the location of a unmodified organial or expanded ROM.
     * Puts this value in "expRomPath" preferences, after copying and expanding
     * the copy, if nessacary. It checks that and the "orgRomPath" values and
     * does not ask if they are already set with valid paths to ROMs of the
     * correct length.
     * 
     * @return An AbstractRom with path of ExpRomPath if just expanded, null if
     *         ExpRomPath pref set.
     */
    public AbstractRom requestOrgRomFile(String romType)
    {
        String tmp;
        File tmpf;
        if ((tmp = this.getPrefs().getValue(romType + ".expRomPath")) == null
            || !(tmpf = new File(tmp)).exists())
        {
            //if orgRomPath from old version set, use it
            if (!(tmp == null)
                && (tmpf = new File(tmp)).exists()
                && ((tmp = this.getPrefs().getValue(romType + ".orgRomPath")) != null))
            {
                return orgRomToExp(romType);
            }
            this.getPrefs().setValue(
                romType + ".expRomPath",
                (tmpf = getFile("Select a unmodified or expanded " + romType
                    + " ROM.", true)).toString());
            if (romType.equals("Earthbound")
                && (tmpf.length() == AbstractRom.EB_ROM_SIZE_REGULAR || tmpf
                    .length() == AbstractRom.EB_ROM_SIZE_EXPANDED))
            {
                this.getPrefs().setValue(romType + ".orgRomPath",
                    tmpf.toString());
                return orgRomToExp(romType);
            }
        }
        System.out.println((new File(getPrefs().getValue(
            romType + ".expRomPath")).length())
            + " == "
            + AbstractRom.EB_ROM_SIZE_REGULAR
            + "; romType = "
            + romType);
        long expromlen = new File(getPrefs().getValue(romType + ".expRomPath"))
            .length();
        if (romType.equals("Earthbound")
            && (expromlen == AbstractRom.EB_ROM_SIZE_REGULAR || expromlen == AbstractRom.EB_ROM_SIZE_EXPANDED))
        {
            System.out
                .println("Exp rom not actually expanded. Bug in previous versions.");
            this.getPrefs().setValue(romType + ".orgRomPath",
                getPrefs().getValue(romType + ".expRomPath"));
            this.getPrefs().removeValue(romType + ".expRomPath");
            return orgRomToExp(romType);
        }
        //null means expRomPath is set to something valid
        return null;
    }

    /**
     * Returns the orginal ROM file's path. Will always get an expanded ROM. If
     * there is not a good recorded value, it will ask the user for the
     * location. Sets private orgRom var to the returned value.
     * 
     * @return The path to an unmodified ROM.
     */
    public AbstractRom getOrginalRomFile(String romType)
    {
        if (orgRom != null && orgRom.isLoaded
            && orgRom.getRomType().equals(romType))
            return orgRom;
        AbstractRom out = requestOrgRomFile(romType);
        if (out != null)
            return orgRom = out;
        else
        {
            out = new RomMem();
            String dd = AbstractRom.getDefaultDir();
            out.loadRom(new File(this.getPrefs().getValue(
                romType + ".expRomPath")));
            AbstractRom.setDefaultDir(dd);
            return orgRom = out;
        }
    }

    private File getBackupDir()
    {
        return new File(rom.getFilePath().getParent() + File.separator
            + "backup");
    }

    private void writeBackup() throws FileNotFoundException, IOException
    {
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            backupDir.mkdir();
            getOrginalRomFile(rom.getRomType());
            //			byte[] orgRomArr = new byte[rom.length()];
            //			new FileInputStream(getOrginalRomFile()).read(orgRomArr);
            System.out.println("About to write backup .ips...");
            IPSFile ips = rom.createIPS(orgRom);
            if (ips == null)
            {
                System.out.println("Failed writting backup .ips.");
                return;
            }
            //			FileOutputStream out =
            //				new FileOutputStream(
            File f = new File(backupDir.toString()
                + File.separator
                + rom.getFilePath().getName()
                + "."
                + new SimpleDateFormat(DATE_FORMAT)
                    .format(new java.util.Date()) + ".bak.ips");
            ips.saveIPSFile(f);
            //			for (int i = 0; i < ips.length(); i++)
            //			{
            //				out.write(ips.charAt(i));
            //			}
            //			out.close();
            System.out.println("Completed writting backup .ips.");

            this.refreshRevertMenu();
        }
    }

    /**
     * Call to make a backup. Called on exit, restore from backup, and the menu
     * options save, save as, load, and close.
     */
    private void doBackup()
    {
        if (rom.isLoaded && rom.getRomType().equals("Unknown"))
        {
            System.out
                .println("Warning: Unknown ROM type. No backups will be made.");
            return;
        }

        try
        {
            writeBackup();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readBackup(String date) throws FileNotFoundException,
        IOException
    {
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            if (backupDir.exists())
            {
                File backupFile = new File(backupDir.toString()
                    + File.separator + rom.getFilePath().getName() + "." + date
                    + ".bak.ips");
                //backup current rom path
                File romPath = rom.getFilePath();
                //load orginal ROM and patch it
                rom.loadRom(getOrginalRomFile(rom.getRomType()).path);
                if (rom.isDirectFileIO())
                    rom.saveRom(romPath);
                rom.apply(IPSFile.loadIPSFile(backupFile));
                //change path back so it saves in the right place
                rom.path = romPath;
                AbstractRom.setDefaultDir(romPath.getParent());
                resetModules();
            }
        }
    }

    private void restoreFromBackup(String date)
    {
        doBackup();
        try
        {
            System.out.println("About to restore from backup .ips");
            readBackup(date);
            System.out.println("Completed restored from backup .ips");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void refreshRevertMenu()
    {
        revertMenu.removeAll();
        if (rom.isLoaded)
        {
            File backupDir = getBackupDir();
            if (backupDir.exists())
            {
                //get list of backed up files
                String[] backups = backupDir.list(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        if (name.startsWith(rom.getFilePath().getName())
                            && name.endsWith(".bak.ips"))
                            return true;
                        return false;
                    }
                });
                //sort list so it will be ordered by date
                Arrays.sort(backups);
                JMenu currMenu = revertMenu;
                int c = 0;
                //look through list and make menu options from it.
                for (int i = backups.length - 1; i >= 0; i--)
                {
                    //show 20 entries then a more menu
                    c++;
                    if (c > 20)
                    {
                        c = 0;
                        currMenu.add(currMenu = new JMenu("More"));
                    }
                    String date = backups[i].substring(rom.getFilePath()
                        .getName().length() + 1, rom.getFilePath().getName()
                        .length()
                        + 1 + DATE_FORMAT.length());
                    String formatedDate;
                    try
                    {
                        formatedDate = new SimpleDateFormat(DATE_FORMAT).parse(
                            date).toString();
                    }
                    catch (ParseException e)
                    {
                        formatedDate = date;
                    }
                    JMenuItem item = new JMenuItem(formatedDate);
                    item.setActionCommand("revert-" + date);
                    item.addActionListener(this);
                    currMenu.add(item);
                }
            }
        }
    }

    private void saveRecentLoadsPref()
    {
        String out = "";
        String[] tmpRecent = (String[]) recentLoads.toArray(new String[0]), recent = new String[Math
            .min(5, tmpRecent.length)];
        for (int i = 0; i < recent.length; i++)
        {
            recent[i] = tmpRecent[tmpRecent.length - (i + 1)];
            out += recent[i];
            if (i != recent.length - 1)
                out += File.pathSeparatorChar;
        }
        this.getPrefs().setValue("recentLoads", out);
        loadRecentLoadsPref(); //force down to 5
    }

    private void loadRecentLoadsPref()
    {
        try
        {
            String[] recent = this.getPrefs().getValue("recentLoads").split(
                File.pathSeparator);
            recentLoads.clear();
            for (int i = recent.length - 1; i >= 0; i--)
            {
                recentLoads.add(recent[i]);
            }
        }
        catch (NullPointerException e)
        {
            recentLoads.clear();
            System.out.println("Unable to load recent loads.");
        }
    }

    private void refreshRecentLoads()
    {
        String[] tmpRecent = (String[]) recentLoads.toArray(new String[0]), recent = new String[Math
            .min(5, tmpRecent.length)];
        int i = 0;
        for (; i < recent.length; i++)
        {
            recent[i] = tmpRecent[tmpRecent.length - (i + 1)];
            recentLoadsItems[i].setText((i + 1) + " " + recent[i]);
            recentLoadsItems[i].setVisible(true);
        }
        for (; i < 5; i++)
        {
            recentLoadsItems[i].setVisible(false);
        }
    }

    private void addRecentLoad(String path)
    {
        File pathf = new File(path);
        for (Iterator i = recentLoads.iterator(); i.hasNext();)
            if (new File((String) i.next()).equals(pathf))
                i.remove();
        recentLoads.add(path);
        saveRecentLoadsPref();
    }

    private void closeRom()
    {
        if (rom.isLoaded && !rom.isDirectFileIO() && save.isEnabled())
        {
            int ques = JOptionPane.showConfirmDialog(null,
                "Do you want to save your changes?", "Save?",
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (ques == JOptionPane.CANCEL_OPTION)
            {
                return;
            }
            else if (ques == JOptionPane.YES_OPTION)
            {
                rom.saveRom();
            }
        }
        hideModules();
        doBackup();

        rom.isLoaded = false;
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion());
        this.refreshRevertMenu();

        /*
         * Make sure save and use file io are enabled after loading of an
         * original ROM.
         */
        save.setEnabled(true);
        saveAs.setEnabled(true);
        useFileIO.setEnabled(true);
    }

    private void hideModules()
    {
        for (int i = 0; i < getModuleCount(); i++)
        {
            try
            {
                getModuleAt(i).hide();
            }
            catch (NullPointerException npe)
            {
                //If not module inited yet
            }
        }
    }

    private void resetModules()
    {
        //offset in module list where IPSDatabase is
        //used to make sure IPSDatabase's reset() is called last
        int ipsdOffset = -1;
        for (int i = 0; i < getModuleCount(); i++)
        {
            try
            {
                if (getModuleAt(i).isRomSupported())
                {
                    //                    System.out.println(loc.getName() + " is a "
                    //                        + rom.getRomType() + " ROM and is usable by "
                    //                        + modButtons[i].getText() + ".");
                    modButtons[i].setVisible(true);
                    //make sure IPSDatabase is reset last
                    if (getModuleAt(i) instanceof IPSDatabase)
                        ipsdOffset = i;
                    else
                        getModuleAt(i).reset();
                }
                else
                {
                    //                    System.out.println(loc.getName() + " is a "
                    //                        + rom.getRomType() + " ROM and is not usable by "
                    //                        + modButtons[i].getText() + ".");
                    modButtons[i].setVisible(false);
                }
                getModuleAt(i).hide();
            }
            catch (NullPointerException npe)
            {
                //If not module inited yet
            }
        }
        try
        {
            if (ipsdOffset != -1)
                getModuleAt(ipsdOffset).reset();
        }
        catch (NullPointerException npe)
        {
            //If not module inited yet
        }
    }

    private boolean loading = false;

    private boolean isLoading()
    {
        return loading;
    }

    private boolean loadRom(File loc)
    {
        loading = true;
        if (loc == null)
        {
            if (!rom.loadRom())
            {
                loading = false;
                return false;
            }
        }
        else if (!rom.loadRom(loc))
        {
            loading = false;
            return false;
        }

        // If ROM just loaded is the original ROM...
        if (rom.getFilePath().getAbsoluteFile()
            .equals(
                getOrginalRomFile(rom.getRomType()).getFilePath()
                    .getAbsoluteFile()))
        {
            // If using direct file IO, changes could be made without saving.
            if (rom.isDirectFileIO())
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "You have attempted to load the ROM you have\n"
                        + "designated as the original ROM. In order\n"
                        + "for this program to work correctly, that\n"
                        + "ROM must stay unmodified. If you wish to\n"
                        + "do any hacking, make a copy of that ROM,\n"
                        + "and edit the copy. If you wish to simply\n"
                        + "view but not modify the original ROM,\n"
                        + "disable direct file IO mode, and try again.",
                    "May Not Load ROM", JOptionPane.WARNING_MESSAGE);
                rom.isLoaded = false;
                loading = false;
                return false;
            }
            // If not using direct file IO, just disallowing saving
            else
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "You have choosen to load the ROM you have\n"
                        + "designated as the original ROM. In order\n"
                        + "for this program to work correctly, that\n"
                        + "ROM must stay unmodified. If you wish to\n"
                        + "do any hacking, make a copy of that ROM,\n"
                        + "and edit the copy. If you wish to simply\n"
                        + "view but not modify the original ROM,\n"
                        + "continue, but remember you will not be\n"
                        + "allowed to save your changes in any way.",
                    "May Not Save Over Original ROM",
                    JOptionPane.WARNING_MESSAGE);
                save.setEnabled(false);
                saveAs.setEnabled(false);
                useFileIO.setEnabled(false);
            }
        }

        //if a Earthbound ROM was just loaded and it is unexpanded (3 MB + 512
        // byte header) then we may want to expand it
        if (rom.getRomType().equals("Earthbound") && rom.length() == 0x300200)
        {
            //name of the automatic expansion preference
            String prefName = "Earthbound.autoExpand";
            //check preferences for user set default
            //Earthbound.autoExpand:
            // true = always expand unexpanded ROMs on load
            // false = never expand unexpanded ROMs on load
            // null (unset) = ask user
            if (getPrefs().hasValue(prefName))
            {
                if (getPrefs().getValueAsBoolean(prefName))
                {
                    //if user selected always expand, do so
                    rom.expand();
                }
                //if user selected never expand, do nothing
            }
            else
            {
                //if user has not set preference, or choose to have it ask
                //pop up a dialog explaining preference, with a remember
                //my preference checkbox

                JCheckBox remember = new JCheckBox(
                    "Remember my choice and use it next time.", false);

                JTextArea text = new JTextArea(
                    "The Earthbound ROM you loaded is "
                        + "not expanded. Expanding a ROM allows you to "
                        + "have an extra megabyte of storage for more "
                        + "of anything. ROM expansion cannot be undone"
                        + "If you do not expand your ROM now "
                        + "you can do so later by using the "
                        + "ROM Expander located in the \"General\" "
                        + "group. You can also use the ROM Expander "
                        + "to expand by another 2 MB at any time."
                        + "\n\nDo you wish to expand this ROM?", 10, 30);
                //make sure text area looks right and does word wrap
                text.setEditable(false);
                text.setEnabled(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                //little L&F specific stuff, hopefully it won't mess up other
                //L&F's
                //makes text in the JTextArea look like the text in
                //the JCheckBox
                text.setBackground(remember.getBackground());
                text.setDisabledTextColor(remember.getForeground());
                text.setFont(remember.getFont());

                int opt = JOptionPane.showConfirmDialog(mainWindow,
                    new JComponent[]{text, remember}, "Expand ROM?",
                    JOptionPane.YES_NO_OPTION);
                //yes is true if user selected yes, false if they did not
                boolean yes = opt == JOptionPane.YES_OPTION;
                if (yes)
                    rom.expand();
                //if user selected to remember their selection, put it into
                //the preferences
                if (remember.isSelected())
                    getPrefs().setValueAsBoolean(prefName, yes);
            }
        }
        resetModules();
        updateTitle();
        addRecentLoad(rom.getPath());
        refreshRecentLoads();
        doBackup();
        loading = false;
        return true;
    }
    
    private void updateTitle()
    {
        mainWindow.setTitle(MainGUI.getDescription() + " "
            + MainGUI.getVersion() + " - " + rom.getPath());
    }

    private void loadRom()
    {
        loadRom(null);
    }

    /**
     * Loads the ROM most recently loaded. If the most recently loaded ROM is
     * not known, nothing is done and false is returned.
     * 
     * @return true if most recent ROM successfully loaded
     * @see #loadRom(File)
     */
    public boolean loadLastRom()
    {
        int rls = recentLoads.size();
        if (rls != 0)
        {
            loadRom(new File((String) recentLoads.get(rls - 1)));
            return true;
        }
        return false;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        //stuff that can be done while ROM is loading
        if (ae.getActionCommand().equalsIgnoreCase("about"))
        {
            JOptionPane.showMessageDialog(null, this.createScollingLabel(this
                .getFullCredits()), "About" + MainGUI.getDescription() + " "
                + MainGUI.getVersion(), JOptionPane.INFORMATION_MESSAGE);
        }
        else if (ae.getActionCommand().equalsIgnoreCase("consoleDialog"))
        {
            if (!JHack.isUseConsole())
                JHack.out.setEnabled(this.getPrefs().getValueAsBoolean(
                    "consoleDialog"));
        }
        else if (ae.getActionCommand().equalsIgnoreCase("noErrorDialog"))
        {
        	if (!JHack.isUseConsole())
        		JHack.err.setEnabled(! this.getPrefs().getValueAsBoolean(
        				"noErrorDialog"));
        }
        else if (ae.getActionCommand().startsWith("LAF_"))
        {
            String laf = ae.getActionCommand().substring(4);
            prefs.setValue("laf", laf);
            try
            {
                if (!UIManager.getLookAndFeel().getClass().getName()
                    .equals(laf))
                {
                    UIManager.setLookAndFeel(laf);
                    SwingUtilities.updateComponentTreeUI(SwingUtilities
                        .getRoot(mainWindow));
                    JHack.out.updateUI();
                    JHack.err.updateUI();

                    for (Iterator i = moduleList.iterator(); i.hasNext();)
                        ((HackModule) i.next()).updateUI();
                }
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (UnsupportedLookAndFeelException e)
            {
                e.printStackTrace();
            }
        }
        else if (ae.getActionCommand().startsWith("comptype_"))
        {
            String type = ae.getActionCommand().substring(9);
            prefs.setValue("updateFormat", type);
        }
        else
        {
            //if ROM is still loading, stop
            if (isLoading())
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Please wait for the ROM to load.", "Loading ROM",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            //stuff that should not be done _during_ loading
            if (ae.getActionCommand().equalsIgnoreCase("load"))
            {
                loadRom(null);
            }
            else if (ae.getActionCommand().equalsIgnoreCase("save"))
            {
                if (rom.isLoaded)
                {
                    rom.saveRom();
                    doBackup();
                }
            }
            else if (ae.getActionCommand().equalsIgnoreCase("saveAs"))
            {
                if (rom.isLoaded)
                {
                    rom.saveRomAs();
                    updateTitle();
                    //ROM file name changed, add to recent list
                    addRecentLoad(rom.getPath());
                    refreshRecentLoads();
                    doBackup();
                }
            }
            else if (ae.getActionCommand().equalsIgnoreCase("closeRom"))
            {
                if (rom.isLoaded)
                    closeRom();
            }
            else if (ae.getActionCommand().startsWith("revert-"))
            {
                this.restoreFromBackup(ae.getActionCommand().substring(7));
            }
            else if (ae.getActionCommand().startsWith("recentLoad"))
            {
                loadRom(new File(recentLoadsItems[Integer.parseInt(ae
                    .getActionCommand().substring(10))].getText().substring(2)));
            }
            else if (ae.getActionCommand().equalsIgnoreCase("exit"))
            {
                this.exit();
            }
            else if (ae.getActionCommand().equalsIgnoreCase("directio"))
            {
                //            this.getPrefs().setValueAsBoolean(
                //                "useDirectFileIO",
                //                !rom.isDirectFileIO());
                if (rom.isLoaded && !rom.isDirectFileIO())
                {
                    int ques = JOptionPane.showConfirmDialog(null,
                        "Do you want to save your changes?", "Save?",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                    if (ques == JOptionPane.CANCEL_OPTION)
                    {
                        return;
                    }
                    else if (ques == JOptionPane.YES_OPTION)
                    {
                        rom.saveRom();
                    }
                }

                File rompath = null;
                if (rom.isLoaded)
                    rompath = rom.getFilePath();

                if (rom instanceof RomMem)
                {
                    rom = new RomFileIO();
                }
                else
                {
                    rom = new RomMem();
                }

                //            HackModule.rom = rom;
                if (rompath != null)
                    rom.loadRom(rompath);
                for (int i = 0; i < getModuleCount(); i++)
                {
                    try
                    {
                        getModuleAt(i).rom = rom;
                        getModuleAt(i).hide();
                        getModuleAt(i).reset();
                    }
                    catch (NullPointerException npe)
                    {
                        //If not module inited yet
                    }
                }
                System.gc();

                //System.out.println("Debug: rom classname: " +
                // rom.getClass().getName());
            }
            else if (ae.getActionCommand().equals("selectDefROM"))
            {
                String romType;
                if (rom.isLoaded)
                {
                    romType = rom.getRomType();
                }
                else
                {
                    //JDialog typeDia = new JDialog(mainWindow, "Select or
                    // Enter
                    // ROM Type", true);
                    //typeDia.getContentPane().setLayout(new BorderLayout());

                    JComboBox typeSel = new JComboBox(RomTypeFinder
                        .getRomTypeNames());
                    typeSel.setEditable(true);
                    typeSel.setSelectedIndex(0);
                    //typeDia.add(typeSel, BorderLayout.NORTH);
                    if (JOptionPane.showConfirmDialog(mainWindow, typeSel,
                        "Select or Enter ROM Type",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
                        return;

                    romType = typeSel.getSelectedItem().toString();
                }
                getPrefs().removeValue(romType + ".orgRomPath");
                getPrefs().removeValue(romType + ".expRomPath");
                requestOrgRomFile(romType);
            }
            else if (ae.getActionCommand().startsWith("module"))
            {
                if (rom.isLoaded)
                {
                    getModuleAt(
                        Integer.parseInt(ae.getActionCommand().substring(6)))
                        .show();
                }
                else
                {
                    JOptionPane.showMessageDialog(null,
                        "You must load a ROM first!", "Error!",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Shows the specified module with the given input. Calls
     * {@link HackModule#show(Object)}on the module.
     * 
     * @see #showModule(String, Object)
     * @param modClass Class you want to show, must extend HackModule.
     * @param input Input to send to class's {@link HackModule#show(Object)}.
     * @return True if successful, false if module not loaded or input invalid.
     */
    public boolean showModule(Class modClass, Object input)
    {
        for (int i = 0; i < getModuleCount(); i++)
        {
            if (getModuleAt(i).getClass().equals(modClass))
            {
                try
                {
                    getModuleAt(i).show(input);
                    return true;
                }
                catch (IllegalArgumentException e)
                {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Shows the specified module with the given input. Calls
     * {@link HackModule#show(Object)}on the module.
     * 
     * @see #showModule(Class, Object)
     * @param modClass Name of the class you want to show, must extend
     *            HackModule.
     * @param input Input to send to class's {@link HackModule#show(Object)}.
     * @return True if successful, false if module not loaded or input invalid.
     * @throws ClassNotFoundException On named class not existing.
     */
    public boolean showModule(String modClass, Object input)
        throws ClassNotFoundException
    {
        return showModule(Class.forName(modClass), input);
    }

    /**
     * Returns the XMLPreferences instance. Use this to get and set preferences
     * that should be presistant across sessions.
     * 
     * @return the XMLPreferences instance.
     */
    public XMLPreferences getPrefs()
    {
        return this.prefs;
    }

    /**
     * Returns the latest version number or null if current version is latest.
     * Also returns null on failure. Uses the site <a href =
     * "http://anyoneeb.ath.cx:83/jhack/checkversion?ver=0.3.6.5">
     * http://anyoneeb.ath.cx:83/jhack/checkversion?ver=0.3.6.5 </a> to check.
     * 
     * @see #getChangeLog(String)
     * @see #askToUpdate()
     * @return The lastest JHack version number or null if current is latest.
     */
    public static String checkVersion()
    {
        try
        {
            URL checkSite = new URL(
                "http://anyoneeb.ath.cx:83/jhack/checkversion?ver="
                    + MainGUI.getVersion());
            String ver = new String();
            InputStreamReader in = new InputStreamReader(checkSite.openStream());
            char[] cbuf = new char[10];
            while (in.read(cbuf) != -1)
                ver += new String(cbuf);
            ver = ver.trim();
            if (ver.equals("NO UPDATE"))
                return null;
            else
                return ver;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            //no internet connection, return null
            //TODO any way to get here with internet up?
            System.out.println("Update check failed due to connection error: "
                + e.getClass() + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns the changelog entry for the specified version. Returns null on
     * failure. Uses the website <a href =
     * "http://anyoneeb.ath.cx:83/jhack/changelog?ver=0.3.6.5">
     * http://anyoneeb.ath.cx:83/jhack/changelog?ver=0.3.6.5 </a> for info.
     * 
     * @see #getVersion()
     * @see #checkVersion()
     * @see #askToUpdate()
     * @param version Version to get changelog for.
     * @return Changelog entry for specified version or null on failure.
     */
    public static String getChangeLog(String version)
    {
        try
        {
            URL clogSite = new URL(
                "http://anyoneeb.ath.cx:83/jhack/changelog?ver=" + version);
            String out = new String();
            InputStreamReader in = new InputStreamReader(clogSite.openStream());
            char[] cbuf = new char[1024];
            while (in.read(cbuf) != -1)
                out += new String(cbuf);
            out = out.trim();
            return out;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks for updates and if one is found, asks the user if it should
     * download it. If user requests to not download, version is remembered and
     * user is not asked about that version again. This should be called every
     * time JHack starts if the "checkVersion" pref is set to true.
     * 
     * @see #getChangeLog(String)
     * @see #checkVersion()
     * @return True if new version downloaded, false if not.
     */
    public boolean askToUpdate()
    {
        try
        {
            final String ver = checkVersion();
            if (ver != null)
            {
                if (ver.equals(getPrefs().getValue("ignoreUpdateVer")))
                    return false;
                JEditorPane clogDisplay = new JEditorPane(new URL(
                    "http://anyoneeb.ath.cx:83/jhack/changelog?ver=" + ver));
                clogDisplay.setEditable(false);
                JScrollPane clogScroll = new JScrollPane(clogDisplay);
                clogScroll.setPreferredSize(new Dimension(250, 150));
                String[] dispverarr = ver.split("\\.");
                String dispver = new String();
                for (int i = 0; i < dispverarr.length; i++)
                {
                    if (i > 0)
                        dispver += ".";
                    dispver += Integer.toString(Integer.parseInt(dispverarr[i],
                        36));
                }
                int ques = JOptionPane.showConfirmDialog(null, clogScroll,
                    "Download JHack v" + dispver + "?",
                    JOptionPane.YES_NO_OPTION);
                if (ques == JOptionPane.YES_OPTION)
                {
                    String[] fileverarr = ver.split("\\.");
                    String filever = new String();
                    for (int i = 0; i < fileverarr.length; i++)
                        filever += fileverarr[i];
                    String ext = prefs.hasValue("updateFormat") ? prefs
                        .getValue("updateFormat") : "zip";
                    URL dl = new URL("http://anyoneeb.ath.cx:83/jhack/JHack."
                        + filever + ".jar." + ext);
                    InputStream in = dl.openStream();
                    FileOutputStream out = new FileOutputStream(new File(
                        "JHack." + filever + ".jar." + ext));
                    byte[] b = new byte[50000];
                    int tmp;
                    while ((tmp = in.read(b)) != -1)
                    {
                        out.write(b, 0, tmp);
                    }
                    out.close();
                    return true;
                }
                else
                {
                    //remember version to not update
                    //message telling user we did this, no more news about this
                    //update
                    getPrefs().setValue("ignoreUpdateVer", ver);
                    JOptionPane.showMessageDialog(null,
                        "You will not be notifed of JHack updates again\n"
                            + "until the next version is released.",
                        "Not updating", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @return Returns the mainWindow.
     */
    public JFrame getMainWindow()
    {
        return mainWindow;
    }
}