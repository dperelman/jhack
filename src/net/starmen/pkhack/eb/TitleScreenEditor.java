/*
 * Created on Mar 20, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class TitleScreenEditor extends FullScreenGraphicsEditor
{
    protected String getClassName()
    {
        return "eb.TitleScreenEditor";
    }

    public static final int NUM_TITLE_SCREENS = 2;

    public int getNumScreens()
    {
        return NUM_TITLE_SCREENS;
    }

    public TitleScreenEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);

        /*
         * try { Class[] c = new Class[]{byte[].class, TitleScreenEditor.class};
         * IPSDatabase.registerExtension("lscn", TitleScreenEditor.class
         * .getMethod("importData", c), TitleScreenEditor.class.getMethod(
         * "restoreData", c), TitleScreenEditor.class.getMethod( "checkData",
         * c), this); } catch (SecurityException e) { // no security model,
         * shouldn't have to worry about this e.printStackTrace(); } catch
         * (NoSuchMethodException e) { // spelling mistake, maybe? ^_^;
         * e.printStackTrace(); }
         */
    }

    public String getVersion()
    {
        return "0.0";
    }

    public String getDescription()
    {
        return "Title Screen Editor";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB";
    }

    public static class TitleScreen extends FullScreenGraphics
    {
        private EbHackModule hm;
        private int num, tilePointer, tileLen, animPalPointer, palPointer,
                palLen, animPalLen, arngPointer, arngLen;

        /** Number of palettes. */
        public static final int NUM_PALETTES = 1;
        /** Number of animated palettes. */
        public static final int NUM_ANIM_PALETTES = 20;
        /** Number of animated palettes for the characters. */
        public static final int NUM_CHAR_ANIM_PALETTES = 14;
        /**
         * Number of arrangements. Note that this is more than fits on the
         * screen, so the last 128 are unused.
         */
        public static final int NUM_ARRANGEMENTS = 1024;

        /** Pointers to ASM pointers * */
        public static final int[] tilePointerArray = new int[]{0xedf2, 0xee49};
        public static final int[] palPointerArray = new int[]{0xeec6, 0x3f692};
        public static final int[] animPalPointerArray = new int[]{0xee9d,
            0xee83};
        public static final int[] arngPointerArray = new int[]{0xee1d, 0xee1d};

        public Color[][] animPal;

        /* private boolean showTextLayer = false; */

        public TitleScreen(int i, EbHackModule hm)
        {
            this.hm = hm;
            this.num = i;

            palPointer = hm.rom.readRegAsmPointer(palPointerArray[num]);
            animPalPointer = hm.rom.readRegAsmPointer(animPalPointerArray[num]);
            tilePointer = hm.rom.readRegAsmPointer(tilePointerArray[num]);
            arngPointer = hm.rom.readRegAsmPointer(arngPointerArray[num]);
        }

        public int getNumSubPalettes()
        {
            return NUM_PALETTES;
        }

        /* Animation is done via subpals. Need to force subpal 0. */
        public Color[] getSubPal(int pal)
        {
            return super.getSubPal(0);
        }

        public void setPaletteColor(int col, int frame, Color color)
        {
            if (frame < 0 || frame > NUM_ANIM_PALETTES
                || (num == 0 && (col < 112 || col >= 144)))
            {
                super.setPaletteColor(col, 0, color);
            }
            else if (num == 1)
            {
                animPal[frame][col] = color;
            }
            else if (col < 128)
            {
                animPal[frame][col - 112] = color;
            }
            else
            {
                titleScreens[1].animPal[frame][col - 128] = color;
            }
        }

        public void setSubPal(int pal, Color[] subPal)
        {
            super.setSubPal(0, subPal);
        }

        public int getSubPaletteSize()
        {
            return num == 0 ? 256 : 16;
        }

        public static int getNumAnimPalettes()
        {
            return NUM_ANIM_PALETTES;
        }

        public static int getAnimPaletteSize()
        {
            return 16;
        }

        public int getNumArrangements()
        {
            return NUM_ARRANGEMENTS;
        }

        public int getNumTiles()
        {
            return num == 0 ? 256 : 1024;
        }

        /*
         * public void setShowTextLayer(boolean b) { showTextLayer = b; } public
         * boolean isShowTextLayer() { return showTextLayer; }
         */

        public Color[] getAnimPalette(int i)
        {
            readInfo();
            Color[] out = new Color[getAnimPaletteSize()];
            System.arraycopy(animPal[i], 0, out, 0, getAnimPaletteSize());
            return out;
        }

        public void setAnimPalette(int i, Color[] newPal)
        {
            readInfo();
            System.arraycopy(newPal, 0, animPal[i], 0, getAnimPaletteSize());
        }

        public Image getTileImage(int tile, int frame, boolean hFlip,
            boolean vFlip)
        {
            readInfo();
            return HackModule.drawImage(tiles[tile], getAnimatedPalette(num,
                frame), hFlip, vFlip);
        }

        private boolean readGraphics(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] tileBuffer = new byte[getNumTiles()
                * (getSubPaletteSize() == 256 ? 64 : 32)];

            /** * DECOMPRESS GRAPHICS ** */
            System.out.println("About to attempt decompressing "
                + tileBuffer.length + " bytes of title screen #" + num
                + " graphics.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(tilePointerArray[num]) : tilePointer,
                tileBuffer, r);
            tiles = new byte[getNumTiles()][8][8];
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing title screen #" + num + " graphics.");
                if (allowFailure)
                {
                    // EMPTY TILES
                    for (int i = 0; i < tiles.length; i++)
                        for (int x = 0; x < tiles[i].length; x++)
                            Arrays.fill(tiles[i][x], (byte) 0);
                    tileLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                tileLen = tmp[1];
                System.out.println("TitleScreen graphics: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int gfxOffset = 0;
                for (int i = 0; i < getNumTiles(); i++)
                {
                    tiles[i] = new byte[8][8];
                    if (getSubPaletteSize() == 256)
                    {
                        gfxOffset += HackModule.read8BPPArea(tiles[i],
                            tileBuffer, gfxOffset, 0, 0);
                    }
                    else if (getSubPaletteSize() == 16)
                    {
                        gfxOffset += HackModule.read4BPPArea(tiles[i],
                            tileBuffer, gfxOffset, 0, 0);
                    }
                }
            }
            return true;
        }

        private boolean readPalette(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] palBuffer = new byte[getNumSubPalettes()
                * getSubPaletteSize() * 2];
            /** * DECOMPRESS PALETTE ** */
            System.out.println("About to attempt decompressing "
                + palBuffer.length + " bytes of title screen #" + num
                + " palette.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(palPointerArray[num]) : palPointer,
                palBuffer, r);
            palette = new Color[NUM_PALETTES][getSubPaletteSize()];
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing title screen #" + num + " palette.");
                if (allowFailure)
                { // EMPTY PALETTES
                    for (int i = 0; i < palette.length; i++)
                        Arrays.fill(palette[i], Color.BLACK);
                    palLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                palLen = tmp[1];
                System.out.println("TitleScreen palette: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int palOffset = 0;
                for (int i = 0; i < NUM_PALETTES; i++)
                {
                    HackModule.readPalette(palBuffer, palOffset, palette[i]);
                    palOffset += palette[i].length * 2;
                }
            }
            return true;
        }

        private boolean readAnimPalette(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] palBuffer = new byte[getNumAnimPalettes()
                * getAnimPaletteSize() * 2];
            /** * DECOMPRESS PALETTE ** */
            System.out.println("About to attempt decompressing "
                + palBuffer.length + " bytes of title screen #" + num
                + " animated palette.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(animPalPointerArray[num]) : animPalPointer,
                palBuffer, r);
            animPal = new Color[NUM_ANIM_PALETTES][getAnimPaletteSize()];
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing title screen #" + num
                    + " animated palette.");
                if (allowFailure)
                { // EMPTY PALETTES
                    for (int i = 0; i < animPal.length; i++)
                        Arrays.fill(animPal[i], Color.BLACK);
                    animPalLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                animPalLen = tmp[1];
                System.out
                    .println("TitleScreen animated palette: Decompressed "
                        + tmp[0] + " bytes from a " + tmp[1]
                        + " byte compressed block.");

                int palOffset = 0;
                for (int i = 0; i < NUM_ANIM_PALETTES; i++)
                {
                    HackModule.readPalette(palBuffer, palOffset, animPal[i]);
                    palOffset += animPal[i].length * 2;
                }
            }
            return true;
        }

        private boolean readArrangement(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            byte[] arngBuffer = new byte[getNumArrangements() * 2];

            /** * DECOMPRESS ARRANGEMENT ** */
            System.out.println("About to attempt decompressing "
                + arngBuffer.length + " bytes of title screen #" + num
                + " arrangement.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(arngPointerArray[num]) : arngPointer,
                arngBuffer, r);
            arrangementList = new short[NUM_ARRANGEMENTS];
            arrangement = new short[32][28];
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing title screen #" + num + " arrangement.");
                if (allowFailure)
                { // EMPTY ARRANGEMENTS
                    Arrays.fill(arrangementList, (short) 0);
                    for (int x = 0; x < arrangement.length; x++)
                        Arrays.fill(arrangement[x], (short) 0);
                    arngLen = 0;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                arngLen = tmp[1];
                System.out.println("TitleScreen arrangement: Decompressed "
                    + tmp[0] + " bytes from a " + tmp[1]
                    + " byte compressed block.");

                int arngOffset = 0;
                for (int i = 0; i < NUM_ARRANGEMENTS; i++)
                {
                    arrangementList[i] = (short) ((arngBuffer[arngOffset++] & 0xff) + ((arngBuffer[arngOffset++] & 0xff) << 8));
                }
                int j = 0;
                for (int y = 0; y < arrangement[0].length; y++)
                    for (int x = 0; x < arrangement.length; x++)
                        arrangement[x][y] = arrangementList[j++];
            }

            return true;
        }

        public boolean readInfo(boolean allowFailure)
        {
            if (isInited)
                return true;

            if (!readGraphics(allowFailure, false))
                return false;
            if (!readPalette(allowFailure, false))
                return false;
            if (!readAnimPalette(allowFailure, false))
                return false;
            if (!readArrangement(allowFailure, false))
                return false;

            isInited = true;
            return true;
        }

        private boolean writeGraphics()
        {
            byte[] udataTiles = new byte[getNumTiles()
                * (getSubPaletteSize() == 256 ? 64 : 32)];
            int tileOff = 0;
            /* COMPRESS TILES */
            for (int i = 0; i < getNumTiles(); i++)
            {
                if (getSubPaletteSize() == 256)
                {
                    tileOff += HackModule.write8BPPArea(tiles[i], udataTiles,
                        tileOff, 0, 0);
                }
                else if (getSubPaletteSize() == 16)
                {
                    tileOff += HackModule.write4BPPArea(tiles[i], udataTiles,
                        tileOff, 0, 0);
                }
            }

            byte[] compTile;
            int tileCompLen = comp(udataTiles, compTile = new byte[tileOff]);
            if (!hm.writeToFreeASMLink(compTile, tilePointerArray[num],
                tileLen, tileCompLen))
                return false;
            System.out.println("Wrote "
                + (tileLen = tileCompLen)
                + " bytes of the title screen #"
                + num
                + " tiles at "
                + Integer.toHexString(tilePointer = hm.rom
                    .readRegAsmPointer(tilePointerArray[num])) + " to "
                + Integer.toHexString(tilePointer + tileCompLen - 1) + ".");

            return true;
        }

        private boolean writePalette()
        {
            byte[] udataPal = new byte[getSubPaletteSize()
                * getNumSubPalettes() * 2];
            int palOff = 0;
            /* COMPRESS PALETTE */
            for (int i = 0; i < NUM_PALETTES; i++)
            {
                HackModule.writePalette(udataPal, palOff, palette[i]);
                palOff += palette[i].length * 2;
            }

            byte[] compPal;
            int palCompLen = comp(udataPal, compPal = new byte[600]);
            if (!hm.writeToFreeASMLink(compPal, palPointerArray[num], palLen,
                palCompLen))
                return false;
            System.out.println("Wrote "
                + (palLen = palCompLen)
                + " bytes of the title screen #"
                + num
                + " palette at "
                + Integer.toHexString(palPointer = hm.rom
                    .readRegAsmPointer(palPointerArray[num])) + " to "
                + Integer.toHexString(palPointer + palCompLen - 1) + ".");
            return true;
        }

        private boolean writeAnimPalette()
        {
            int numPals = num == 0 ? NUM_ANIM_PALETTES : NUM_CHAR_ANIM_PALETTES;
            byte[] udataPal = new byte[getAnimPaletteSize() * numPals * 2];
            int palOff = 0;
            /* COMPRESS ANIM PALETTE */
            for (int i = 0; i < numPals; i++)
            {
                HackModule.writePalette(udataPal, palOff, animPal[i]);
                palOff += animPal[i].length * 2;
            }

            byte[] compPal;
            int palCompLen = comp(udataPal, compPal = new byte[600]);
            if (!hm.writeToFreeASMLink(compPal, animPalPointerArray[num],
                animPalLen, palCompLen))
                return false;
            System.out.println("Wrote "
                + (animPalLen = palCompLen)
                + " bytes of the title screen #"
                + num
                + " animated palette at "
                + Integer.toHexString(animPalPointer = hm.rom
                    .readRegAsmPointer(animPalPointerArray[num])) + " to "
                + Integer.toHexString(animPalPointer + palCompLen - 1) + ".");
            return true;
        }

        private boolean writeArrangement()
        {
            byte[] udataArng = new byte[getNumArrangements() * 2];
            int arngOff = 0;
            /* COMPRESS ARRANGEMENT */
            int j = 0;
            for (int y = 0; y < arrangement[0].length; y++)
                for (int x = 0; x < arrangement.length; x++)
                    arrangementList[j++] = arrangement[x][y];
            for (int i = 0; i < NUM_ARRANGEMENTS; i++)
            {
                udataArng[arngOff++] = (byte) (arrangementList[i] & 0xff);
                udataArng[arngOff++] = (byte) ((arrangementList[i] >> 8) & 0xff);
            }

            byte[] compArng;
            int arngCompLen = comp(udataArng, compArng = new byte[3000]);
            if (!hm.writeToFreeASMLink(compArng, arngPointerArray[num],
                arngLen, arngCompLen))
                return false;
            System.out.println("Wrote "
                + (arngLen = arngCompLen)
                + " bytes of the title screen #"
                + num
                + " arrangement at "
                + Integer.toHexString(arngPointer = hm.rom
                    .readRegAsmPointer(arngPointerArray[num])) + " to "
                + Integer.toHexString(arngPointer + arngCompLen - 1) + ".");

            return true;
        }

        public boolean writeInfo()
        {
            if (!isInited)
                return false;

            if (!writePalette())
                return false;
            if (!writeAnimPalette())
                return false;
            if (!writeArrangement())
                return false;
            if (!writeGraphics())
                return false;

            return true;
        }
    }

    public static class TitleScreenAnimData
    {
        public static final int NUM_ENTRIES = 9;

        public static final int TABLE_START = 0x21D008;
        public static final int POINTER_OFF = 0x210200;

        private EbHackModule hm;
        private int num;
        private List entries;

        public TitleScreenAnimData(int i, EbHackModule hm)
        {
            this.num = i;
            this.hm = hm;
        }

        protected void readInfo()
        {
            hm.rom.seek(hm.rom.readMulti(TABLE_START + num * 2, 2)
                + POINTER_OFF);
            entries = new ArrayList();
            while (entries.size() == 0
                || !((TitleScreenAnimEntry) entries.get(entries.size() - 1))
                    .isFinalEntry())
            {
                entries.add(TitleScreenAnimEntry.readSeek(hm.rom));
            }
        }

        public static class TitleScreenAnimEntry
        {
            private byte y, x, flags;
            private short tile;

            public TitleScreenAnimEntry(byte y, short tile, byte x, byte flags)
            {
                this.y = y;
                this.tile = tile;
                this.x = x;
                this.flags = flags;
            }

            public static TitleScreenAnimEntry readSeek(AbstractRom rom)
            {
                return new TitleScreenAnimEntry(rom.readByteSeek(), (short) rom
                    .readMultiSeek(2), rom.readByteSeek(), rom.readByteSeek());
            }

            public static TitleScreenAnimEntry read(AbstractRom rom, int offset)
            {
                rom.seek(offset);
                return readSeek(rom);
            }

            public void writeSeek(AbstractRom rom)
            {
                rom.writeSeek(y);
                rom.writeSeek(tile, 2);
                rom.writeSeek(x);
                rom.writeSeek(flags);
            }

            public void write(AbstractRom rom, int offset)
            {
                rom.seek(offset);
                writeSeek(rom);
            }

            /**
             * @return Returns the flags.
             */
            public byte getFlags()
            {
                return flags;
            }

            /**
             * @param flags The flags to set.
             */
            public void setFlags(byte flags)
            {
                this.flags = flags;
            }

            /**
             * Returns true if this ends a block of entries.
             * 
             * @return (getFlags() & 0x80) != 0
             */
            public boolean isFinalEntry()
            {
                return (flags & 0x80) != 0;
            }

            /**
             * @return Returns the tile.
             */
            public short getTile()
            {
                return tile;
            }

            /**
             * @param tile The tile to set.
             */
            public void setTile(short tile)
            {
                this.tile = tile;
            }

            /**
             * @return Returns the x.
             */
            public byte getX()
            {
                return x;
            }

            /**
             * @param x The x to set.
             */
            public void setX(byte x)
            {
                this.x = x;
            }

            /**
             * @return Returns the y.
             */
            public byte getY()
            {
                return y;
            }

            /**
             * @param y The y to set.
             */
            public void setY(byte y)
            {
                this.y = y;
            }
        }
    }

    public static final TitleScreen[] titleScreens = new TitleScreen[NUM_TITLE_SCREENS];
    public static final TitleScreenAnimData[] animData = new TitleScreenAnimData[TitleScreenAnimData.NUM_ENTRIES];

    public FullScreenGraphics getScreen(int i)
    {
        return titleScreens[i];
    }

    public String getScreenName(int i)
    {
        return getScreenNames()[i];
    }

    public String[] getScreenNames()
    {
        return new String[]{"Title screen background", "Title screen text"};
    }

    public void setScreenName(int i, String newName)
    {
        return;
    }

    public static void readFromRom(EbHackModule hm)
    {
        for (int i = 0; i < titleScreens.length; i++)
        {
            titleScreens[i] = new TitleScreen(i, hm);
        }
        for (int i = 0; i < animData.length; i++)
        {
            animData[i] = new TitleScreenAnimData(i, hm);
        }
    }

    protected void readFromRom()
    {
        readFromRom(this);
    }

    public void reset()
    {
        readFromRom();
    }

    protected int getTileSelectorWidth()
    {
        return getCurrentScreen() == 0 ? 16 : 16;
    }

    protected int getTileSelectorHeight()
    {
        return getCurrentScreen() == 0 ? 16 : 64;
    }

    protected int focusDaDir()
    {
        return SwingConstants.TOP;
    }

    protected int focusArrDir()
    {
        return SwingConstants.BOTTOM;
    }

    protected void initComponents()
    {
        super.initComponents();
        pal = new SpritePalette(256, 8, 16);
        pal.setActionCommand("paletteEditor");
        pal.addActionListener(this);

        subPalSelector = new JComboBox();
        for (int i = 0; i < TitleScreen.NUM_ANIM_PALETTES; i++)
        {
            subPalSelector.addItem((i + 1) + "/"
                + TitleScreen.NUM_ANIM_PALETTES);
        }
        subPalSelector.addItem("Final(?)");
        subPalSelector.setActionCommand("subPalSelector");
        subPalSelector.addActionListener(this);

        da = new IntArrDrawingArea(dt, pal, this);
        da.setActionCommand("drawingArea");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(81, 81));

        name = null;
    }

    protected JComponent layoutComponents()
    {
        Box center = new Box(BoxLayout.Y_AXIS);
        center.add(getLabeledComponent("Screen: ", screenSelector));
        // center.add(getLabeledComponent("Screen Name: ", name));
        // center.add(getLabeledComponent("SubPalette: ", subPalSelector));
        center.add(Box.createVerticalStrut(15));
        center.add(createFlowLayout(da));
        center.add(Box.createVerticalStrut(5));
        center.add(getLabeledComponent("Animation Frame: ", subPalSelector));
        center.add(createFlowLayout(pal));
        center.add(Box.createVerticalStrut(10));
        center.add(createFlowLayout(fi));
        center.add(Box.createVerticalGlue());

        JPanel display = new JPanel(new BorderLayout());
        display.add(pairComponents(dt, null, false), BorderLayout.EAST);
        display.add(pairComponents(pairComponents(tileSelector,
            createFlowLayout(center), true), arrangementEditor, false),
            BorderLayout.WEST);

        return display;
    }

    /*
     * protected int getCurrentSubPalette() { return 0; }
     */

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.eb.FullScreenGraphicsEditor#getSelectedSubPalette()
     */
    protected Color[] getSelectedSubPalette()
    {
        return getAnimatedPalette(getCurrentScreen(), subPalSelector
            .getSelectedIndex());
    }

    public static Color[] getAnimatedPalette(int num, int i)
    {
        Color[] out = titleScreens[num].getSubPal(0);

        if (i > 0 && i < TitleScreen.NUM_ANIM_PALETTES)
        {
            if (num == 0)
            {
                /*
                 * Overwrite the "normal" colors with the correct animated
                 * palettes.
                 */
                Color[] highlightAnimPal = titleScreens[0].getAnimPalette(i);
                Color[] textAnimPal = titleScreens[1].getAnimPalette(i);
                System.arraycopy(highlightAnimPal, 0, out, 112, TitleScreen
                    .getAnimPaletteSize());
                System.arraycopy(textAnimPal, 0, out, 128, TitleScreen
                    .getAnimPaletteSize());
            }
            else
            {
                out = titleScreens[num].getAnimPalette(i);
            }
        }
        /*
         * Else: A animated palette was not selected. The original Color[] is
         * fine.
         */
        return out;
    }

    protected boolean isSinglePalImport()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.eb.FullScreenGraphicsEditor#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        super.actionPerformed(ae);
        if (ae.getActionCommand().equals("subPalSelector"))
        {
            tileSelector.repaint();
            arrangementEditor.repaint();
        }
        else if (ae.getActionCommand().equals("mapSelector"))
        {
            mainWindow.getContentPane().invalidate();
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            arrangementEditor
                .setVisible(screenSelector.getSelectedIndex() == 0);
            mainWindow.getContentPane().validate();
        }
    }

    /** TODO: Import/export */
    protected boolean importData()
    {
        return false;
    }

    protected boolean exportData()
    {
        return false;
    }
}