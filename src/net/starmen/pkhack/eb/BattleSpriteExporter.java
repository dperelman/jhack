/*
 * Created on Oct 10, 2004
 */
package net.starmen.pkhack.eb;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.HackModule;
import net.starmen.pkhack.XMLPreferences;

/**
 * TODO Write javadoc for this class
 * 
 * @author AnyoneEB
 */
public class BattleSpriteExporter extends EbHackModule
{

    /**
     * @param rom
     * @param prefs
     */
    public BattleSpriteExporter(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }

    protected void init()
    {}

    public String getVersion()
    {
        return "0.0";
    }

    public String getDescription()
    {
        return "Battle Sprite Exporter";
    }

    public String getCredits()
    {
        return "Written by AnyoneEB\n" + "Requested by Jeffman";
    }

    public void hide()
    {}

    public void show()
    {
        /*
         * Create a File object for the target directory. This will be a
         * directory named "bsimgs" in the working directory of PK Hack, which
         * is wherever the .jar file is.
         */
        File baseDir = new File("bsimgs" + File.separatorChar);
        /* And create that directory, so it's ready to be used. */
        baseDir.mkdir();

        //make sure information is read from ROM
        EnemyEditor.readFromRom(this);
        BattleSpriteEditor.readFromRom(this);

        //iterate through enemies (start at 1 because 0 is null)
        for (int i = 1; i < EnemyEditor.enemies.length; i++)
        {
            /*
             * Put a reference to the enemy object we are working with into a
             * varible.
             */
            EnemyEditor.Enemy ce = EnemyEditor.enemies[i];
            /*
             * If the battle sprite number is 0 (invisible), skip this iteration
             * of the loop.
             */
            if (ce.getInsidePic() == 0)
                continue;
            /*
             * Put a reference to the battle sprite object this enemy references
             * into a varible. We substract 1 because 0 is invisible.
             */
            BattleSpriteEditor.BattleSprite bs = BattleSpriteEditor.battleSprites[ce
                .getInsidePic() - 1];

            /* Try to read battle sprite from ROM, if it works, continue... */
            if (bs.readInfo(false) == 0)
            {
                /*
                 * Store reference to battle spite "image" (array of color
                 * numbers, which are indexes in the palette).
                 */
                byte[][] sprite = bs.getSprite();
                /*
                 * Store reference to the palette the enemy uses for its battle
                 * sprite.
                 */
                Color[] pal = BattleSpriteEditor.palettes[ce.getPalette()];
                /*
                 * Set the 0th color of the palette to transparent because the
                 * game always uses it as transparent.
                 */
                pal[0] = new Color(0, 0, 0, 0);
                /*
                 * Use the HackModule drawImage() method to create an actual
                 * Image from the sprite and palette data.
                 */
                BufferedImage img = HackModule.drawImage(sprite, pal);
                /* Create a File object for the image we are going to write. */
                File f = new File(baseDir.toString() + File.separatorChar
                    + HackModule.addZeros(Integer.toString(i), 3) + ".png");
                /* Try to write the Image img to the File f as a png. */
                try
                {
                    ImageIO.write(img, "png", f);

                    System.out.println("Successfully exported image of ["
                        + HackModule.addZeros(Integer.toString(i), 3) + "] "
                        + ce.getName());
                }
                catch (IOException e)
                {
                    /*
                     * If it doesn't work, write the error message to standard
                     * output (instead of standard error) and continue.
                     */
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}