/*
 * Created on Mar 20, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.IntArrDrawingArea;
import net.starmen.pkhack.JHack;
import net.starmen.pkhack.SpritePalette;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.TitleScreenEditor.TitleScreenAnimData.TitleScreenAnimEntry;

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
        return "0.1";
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

        private Color[][] animPal;
        private Color[] palette;
        /* True if the background static palette only has 128 colors. */
        private boolean isOrgTitleScreen;

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
            /* "Initial Flash" not editable */
            if (frame == TitleScreen.NUM_CHAR_ANIM_PALETTES
                + TitleScreen.NUM_ANIM_PALETTES + 1)
            {
                return;
            }
            /*
             * Just go straight to the regular, nonanimated palette if the frame
             * is out of range.
             */
            else if (frame < 0
                || frame >= NUM_CHAR_ANIM_PALETTES + NUM_ANIM_PALETTES)
            {
                if (num == 0 && col >= 128 && col < 144)
                {
                    titleScreens[1].setPaletteColor(col - 128, -1, color);
                }
                else
                {
                    super.setPaletteColor(col, 0, color);
                }
            }
            else
            /* Valid animated frame number. */
            {
                /*
                 * If we are looking at the letters, only look at the animated
                 * palette.
                 */
                if (num == 1)
                {
                    if (frame >= NUM_CHAR_ANIM_PALETTES)
                    {
                        /*
                         * Not animated there, so the other static palette gets
                         * used.
                         */
                        titleScreens[0].palette[col + 128] = color;
                    }
                    else
                    {
                        animPal[frame][col] = color;
                    }
                }
                else
                /* num == 0 */
                {
                    if (col >= 112 && col < 128)
                    {
                        /*
                         * The 16 colors 112-127 are the background animated
                         * palette. It is animated after the letters.
                         */
                        if (frame >= NUM_CHAR_ANIM_PALETTES)
                        {
                            animPal[frame - NUM_CHAR_ANIM_PALETTES][col - 112] = color;
                        }
                        else
                        {
                            super.setPaletteColor(col, 0, color);
                        }
                    }
                    else if (col >= 128 && col < 144)
                    {
                        /*
                         * The 16 colors 128-143 are the letters animated
                         * palette. It is animated first. Let the above "if (num ==
                         * 1)" line do the work of choosing the correct place to
                         * save to.
                         */
                        titleScreens[1]
                            .setPaletteColor(col - 128, frame, color);
                    }
                    else
                    {
                        super.setPaletteColor(col, 0, color);
                    }
                }
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

        public int getNumAnimPalettes()
        {
            return num == 0 ? NUM_ANIM_PALETTES : NUM_CHAR_ANIM_PALETTES;
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

            isOrgTitleScreen = readOrg;
            byte[] palBuffer = new byte[getNumSubPalettes()
                * getSubPaletteSize() * 2];
            /** * DECOMPRESS PALETTE ** */
            System.out.println("About to attempt decompressing "
                + palBuffer.length + " bytes of title screen #" + num
                + " palette.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(palPointerArray[num]) : palPointer,
                palBuffer, r);
            palette = new Color[getSubPaletteSize()];
            /* Make superclass's methods work. */
            super.palette[0] = palette;
            if (tmp[0] < 0)
            {
                System.out.println("Error " + tmp[0]
                    + " decompressing title screen #" + num + " palette.");
                if (allowFailure)
                { // EMPTY PALETTES
                    Arrays.fill(palette, Color.BLACK);
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
                if (num == 0 && tmp[0] == 256)
                {
                    isOrgTitleScreen = true;
                }

                HackModule.readPalette(palBuffer, 0, palette);
            }
            return true;
        }

        /**
         * Returns true if the most recently loaded static palette had 128
         * colors. This means that the next 16 colors must be filled in with the
         * last animated palette for the foreground in order to correctly
         * display the screen.
         * 
         * @return true if this is in the original EB title screen format, false
         *         if it was created by this editor
         */
        public boolean isOrgTitleScreen()
        {
            return isOrgTitleScreen;
        }

        private boolean readAnimPalette(boolean allowFailure, boolean readOrg)
        {
            AbstractRom r = readOrg ? JHack.main.getOrginalRomFile(hm.rom
                .getRomType()) : hm.rom;

            /* Extra large buffer in case of bad writes. */
            byte[] palBuffer = new byte[getNumAnimPalettes()
                * getAnimPaletteSize() * 2];
            /** * DECOMPRESS PALETTE ** */
            System.out.println("About to attempt decompressing "
                + palBuffer.length + " bytes of title screen #" + num
                + " animated palette.");
            int[] tmp = EbHackModule.decomp(readOrg ? r
                .readRegAsmPointer(animPalPointerArray[num]) : animPalPointer,
                palBuffer, r);
            animPal = new Color[getNumAnimPalettes()][getAnimPaletteSize()];
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
                for (int i = 0; i < animPal.length; i++)
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
            /*
             * Copy the last frame of the character animated palette into the
             * background static palette if it was 128 colors so it does not
             * have it.
             */
            if (num == 1 && titleScreens[0].isOrgTitleScreen())
            {
                System.arraycopy(animPal[NUM_CHAR_ANIM_PALETTES - 1], 0,
                    titleScreens[0].palette, 128, 16);
            }
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
            /* COMPRESS PALETTE */
            /* Hmm... we only read 128 colors for palette 0, I wonder why... */
            /*
             * Color[] pal; if (num == 0) { pal = new Color[128];
             * System.arraycopy(palette, 0, pal, 0, 128); } else { pal =
             * palette; }
             */
            HackModule.writePalette(udataPal, 0, palette);

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
            byte[] udataPal = new byte[getAnimPaletteSize()
                * getNumAnimPalettes() * 2];
            int palOff = 0;
            /* COMPRESS ANIM PALETTE */
            for (int i = 0; i < animPal.length; i++)
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
            /* If not read yet, read and then write. */
            if (!isInited && !readInfo())
                return false;

            if (!writePalette())
                return false;
            if (!writeAnimPalette())
                return false;
            /* Only #0 has an arrangement to write. */
            if (num == 0 && !writeArrangement())
                return false;
            if (!writeGraphics())
                return false;

            return true;
        }
    }

    public static class TitleScreenAnimData
    {
        public static final int NUM_ENTRIES = 9;

        public static final int TABLE_START = 0x21D19D;
        public static final int POINTER_OFF = 0x210200;

        private EbHackModule hm;
        private int num;
        private List entries;

        public TitleScreenAnimData(int i, EbHackModule hm)
        {
            this.num = i;
            this.hm = hm;

            readInfo();
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

        public Iterator getIterator()
        {
            return entries.iterator();
        }

        public List getEntries()
        {
            return entries;
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

    private class LetterPositionEditor extends JComponent
    {
        public LetterPositionEditor()
        {
            this.setPreferredSize(arrangementEditor.getPreferredSize());
        }

        public int getZoom()
        {
            return arrangementEditor.getZoom();
        }

        public void paint(Graphics g)
        {
            if (arrImg != null
            // && subPalSelector.getSelectedIndex() >=
            // TitleScreen.NUM_CHAR_ANIM_PALETTES
            )
            {
                g.drawImage(arrImg, 0, 0, null);
            }

            // TODO Read Anim Data properly
            int[] xOffs = {0, 0, 2, 0, -2, 26, 26, 27, 28};
            // System.out.println();
            for (int i = 0; i < animData.length; i++)
            {
                // System.out.println();
                int xOff = xOffs[i] + 34 + i * 20, yOff = 100;
                /*
                 * System.out.println("LPE: xOff for [" + i + "] = 0x" +
                 * Integer.toHexString(xOff));
                 */
                for (Iterator j = animData[i].getIterator(); j.hasNext();)
                {
                    TitleScreenAnimEntry entry = (TitleScreenAnimEntry) j
                        .next();
                    int usedTile = entry.getTile()
                        & (tileSelector.getTileCount() - 1);

                    /*
                     * System.out.println("LPE: animData[" + i + "]:
                     * usedTile=0x" + Integer.toHexString(usedTile) + " tile=0x" +
                     * Integer.toHexString(entry.getTile() & 0xffffffff) + " X=" +
                     * entry.getX() + " Y=" + entry.getY() + " flags=0x" +
                     * Integer.toHexString(entry.getFlags() & 0xff));
                     */

                    Color[] pal = getSelectedSubPalette();
                    pal[0] = new Color(0, 0, 0, 0);
                    Image img = HackModule.drawImage(titleScreens[1]
                        .getTile(usedTile), pal);

                    g.drawImage(img, (entry.getX() + xOff) * getZoom(), (entry
                        .getY() + yOff)
                        * getZoom(), arrangementEditor.getTileSize()
                        * getZoom(), arrangementEditor.getTileSize()
                        * getZoom(), null);
                    /*
                     * Flag 0x01 = draw square with first tile number as
                     * top-left.
                     */
                    if ((entry.getFlags() & 0x01) != 0)
                    {
                        g.drawImage(drawImage(titleScreens[1]
                            .getTile(usedTile + 1), pal),
                            (entry.getX() + xOff + arrangementEditor
                                .getTileSize())
                                * getZoom(), (entry.getY() + yOff) * getZoom(),
                            arrangementEditor.getTileSize() * getZoom(),
                            arrangementEditor.getTileSize() * getZoom(), null);
                        g.drawImage(drawImage(titleScreens[1]
                            .getTile(usedTile + 16), pal),
                            (entry.getX() + xOff) * getZoom(), (entry.getY()
                                + yOff + arrangementEditor.getTileSize())
                                * getZoom(), arrangementEditor.getTileSize()
                                * getZoom(), arrangementEditor.getTileSize()
                                * getZoom(), null);
                        g.drawImage(drawImage(titleScreens[1]
                            .getTile(usedTile + 17), pal),
                            (entry.getX() + xOff + arrangementEditor
                                .getTileSize())
                                * getZoom(),
                            (entry.getY() + yOff + arrangementEditor
                                .getTileSize())
                                * getZoom(), arrangementEditor.getTileSize()
                                * getZoom(), arrangementEditor.getTileSize()
                                * getZoom(), null);
                    }
                }
            }
        }
    }

    public static final TitleScreen[] titleScreens = new TitleScreen[NUM_TITLE_SCREENS];
    public static final TitleScreenAnimData[] animData = new TitleScreenAnimData[TitleScreenAnimData.NUM_ENTRIES];

    private BufferedImage arrImg;
    private LetterPositionEditor lpe;

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
        int totAnimFrames = TitleScreen.NUM_CHAR_ANIM_PALETTES
            + TitleScreen.NUM_ANIM_PALETTES;
        for (int i = 0; i < totAnimFrames; i++)
        {
            subPalSelector.addItem((i + 1) + "/" + totAnimFrames);
        }
        subPalSelector.addItem("Initial");
        subPalSelector.addItem("Initial Flash");
        subPalSelector.setActionCommand("subPalSelector");
        subPalSelector.addActionListener(this);

        da = new IntArrDrawingArea(dt, pal, this);
        da.setActionCommand("drawingArea");
        da.setZoom(10);
        da.setPreferredSize(new Dimension(81, 81));

        lpe = new LetterPositionEditor();
        lpe.setVisible(false);

        name = null;
    }

    protected JComponent layoutComponents()
    {
        Box center = new Box(BoxLayout.Y_AXIS);
        center.add(getLabeledComponent("Screen: ", screenSelector));
        center.add(Box.createVerticalStrut(15));
        center.add(createFlowLayout(da));
        center.add(Box.createVerticalStrut(5));
        center.add(getLabeledComponent("Animation Frame: ", subPalSelector));
        center.add(createFlowLayout(pal));
        center.add(Box.createVerticalStrut(10));
        center.add(createFlowLayout(fi));
        center.add(Box.createVerticalStrut(10));
        center.add(lpe);
        // center.add(Box.createVerticalGlue());

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
        Color[] out = getAnimatedPalette(getCurrentScreen(), subPalSelector
            .getSelectedIndex());
        if (out == null)
        {
            throw new NullPointerException("Null palette.");
        }
        return out;
    }

    public static Color[] getAnimatedPalette(int num, int frame)
    {
        if (frame >= 0
            && frame < TitleScreen.NUM_CHAR_ANIM_PALETTES
                + TitleScreen.NUM_ANIM_PALETTES)
        {
            if (num == 0)
            {
                /*
                 * Overwrite the "normal" colors with the correct animated
                 * palettes.
                 */

                Color[] textAnimPal, out;
                if (frame < TitleScreen.NUM_CHAR_ANIM_PALETTES)
                {
                    out = new Color[titleScreens[0].getSubPaletteSize()];
                    for (int j = 0; j < out.length; j++)
                    {
                        out[j] = new Color(0, 0, 0);
                    }
                    textAnimPal = titleScreens[1].getAnimPalette(frame);
                    System.arraycopy(textAnimPal, 0, out, 128, TitleScreen
                        .getAnimPaletteSize());
                }
                else
                {
                    out = titleScreens[num].getSubPal(0);

                    Color[] highlightAnimPal = titleScreens[0]
                        .getAnimPalette(frame
                            - TitleScreen.NUM_CHAR_ANIM_PALETTES);
                    System.arraycopy(highlightAnimPal, 0, out, 112, TitleScreen
                        .getAnimPaletteSize());
                }

                return out;
            }
            else
            {
                if (frame < TitleScreen.NUM_CHAR_ANIM_PALETTES)
                {
                    return titleScreens[num].getAnimPalette(frame);
                }
                else
                {
                    Color[] out = new Color[16];
                    System.arraycopy(titleScreens[0].getSubPal(0), 128, out, 0,
                        16);
                    return out;
                }
            }
        }
        else if (frame == TitleScreen.NUM_CHAR_ANIM_PALETTES
            + TitleScreen.NUM_ANIM_PALETTES + 1)
        {
            /* "Initial Flash" */
            if (num == 0)
            {
                Color[] out = new Color[256];
                Arrays.fill(out, 0, 128, Color.WHITE);
                Arrays.fill(out, 128, 256, Color.BLACK);
                return out;
            }
            else
            /* if(num == 1) */
            {
                Color[] out = new Color[16];
                Arrays.fill(out, Color.BLACK);
                return out;
            }
        }
        /*
         * Else: A animated palette was not selected. The original Color[] is
         * fine.
         */
        Color[] out = titleScreens[num].getSubPal(0);
        if (num == 0)
        {
            System.arraycopy(titleScreens[1].getSubPal(0), 0, out, 128, 16);
        }
        return out;
    }

    protected boolean isSinglePalImport()
    {
        return true;
    }

    private void cacheArrImg()
    {
        arrImg = new BufferedImage(arrangementEditor.getWidth(),
            arrangementEditor.getHeight(), BufferedImage.TYPE_INT_RGB);
        screenSelector.removeActionListener(this);
        int realSelectedScreen = screenSelector.getSelectedIndex();
        screenSelector.setSelectedIndex(0);
        arrangementEditor.paint(arrImg.getGraphics());
        screenSelector.setSelectedIndex(realSelectedScreen);
        screenSelector.addActionListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.starmen.pkhack.eb.FullScreenGraphicsEditor#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("mapSelector")
            || ae.getActionCommand().equals("subPalSelector"))
        {
            if (screenSelector.getSelectedIndex() != 0)
            {
                cacheArrImg();
            }
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            int frame = getCurrentSubPalette();
            int col = pal.getSelectedColorIndex();
            if (screenSelector.getSelectedIndex() == 0 && frame >= 0
                && frame < TitleScreen.NUM_CHAR_ANIM_PALETTES
                && (col < 128 || col >= 144))
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Sorry, at this point in the animation,\n"
                        + "that color is always black.\n"
                        + "Try editing that color later on in the animation\n"
                        + "or edit the graphics/arrangement so that you are\n"
                        + "using one of the letters animated palette colors.",
                    "Unable to edit color", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            else if (frame == TitleScreen.NUM_CHAR_ANIM_PALETTES
                + TitleScreen.NUM_ANIM_PALETTES + 1)
            {
                JOptionPane.showMessageDialog(mainWindow,
                    "Sorry, the \"Initial Flash\" colors\n"
                        + "are hardcoded and cannot be changed.",
                    "Unable to edit color", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        super.actionPerformed(ae);
        if (ae.getActionCommand().equals("subPalSelector"))
        {
            tileSelector.repaint();
            (screenSelector.getSelectedIndex() == 0
                ? (JComponent) arrangementEditor
                : (JComponent) lpe).repaint();
        }
        else if (ae.getActionCommand().equals("paletteEditor"))
        {
            if (screenSelector.getSelectedIndex() == 1)
            {
                cacheArrImg();
                lpe.repaint();
            }
        }
        else if (ae.getActionCommand().equals("mapSelector"))
        {
            /* Correctly change color selection when changing modes. */
            int col = pal.getSelectedColorIndex();
            if (screenSelector.getSelectedIndex() == 1)
            {
                if (col >= 128 && col < 144)
                {
                    col -= 128;
                }
                else
                {
                    col = 0;
                }
            }
            else
            {
                /* Shift the color up to where it is on the big palette. */
                col += 128;
            }
            mainWindow.getContentPane().invalidate();
            pal.setSelectedColorIndex(col);
            if (screenSelector.getSelectedIndex() == 0)
            {
                pal.changeSize(8, 16);
            }
            else
            {
                pal.changeSize(20, 2);
            }
            tileSelector.invalidate();
            tileSelector.resetPreferredSize();
            tileSelector.validate();
            tileSelector.repaint();
            arrangementEditor
                .setVisible(screenSelector.getSelectedIndex() == 0);
            lpe.setVisible(screenSelector.getSelectedIndex() == 1);
            mainWindow.getContentPane().validate();
        }
    }

    /** TODO: Import/export */
    protected boolean importData()
    {
        JOptionPane.showMessageDialog(mainWindow,
            "Sorry, import and export have not yet\n"
                + "been implemented. Look forward to them\n"
                + "in a future version.", "Import not implemented yet",
            JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    protected boolean exportData()
    {
        JOptionPane.showMessageDialog(mainWindow,
            "Sorry, import and export have not yet\n"
                + "been implemented. Look forward to them\n"
                + "in a future version.", "Export not implemented yet",
            JOptionPane.INFORMATION_MESSAGE);
        return false;
    }
}