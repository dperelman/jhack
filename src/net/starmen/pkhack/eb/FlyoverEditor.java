/*
 * FlyoverEditor.java
 * 
 * Created on November 1, 2003, 12:54 AM
 */

package net.starmen.pkhack.eb;

//import java.util.Map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.CCInfo;
import net.starmen.pkhack.XMLPreferences;
import net.starmen.pkhack.eb.TextEditor.StrInfo;

// code by Chris (AIM: daemionx)
// credits to anyoneEB and BA...for JHack and the flyover tutorial respectively

public class FlyoverEditor extends EbHackModule implements ActionListener,
    ItemListener, DocumentListener
{
    /**
     * @param rom
     * @param prefs
     */
    public FlyoverEditor(AbstractRom rom, XMLPreferences prefs)
    {
        super(rom, prefs);
    }
    //variables other than GUI:
    private Flyover[] scene;
    private CCInfo textParser;

    private boolean textHexToggle = false;

    private boolean preventOverwriteToggle = true;

    //number format conversions...1pp and standard are just a difference of
    // different bases
    //8ppu = hex and 1/8 of standard
    private int numberFormat = 0;
    private boolean numbersHexToggle = false;
    private boolean numbers1ppuToggle = true;

    private static String[] directions = new String[]{"00 - North",
        "01 - N. East", "02 - East", "03 - S. East", "04 - South",
        "05 - S. West", "06 - West", "07 - N. West"};

    //CONSTANTS:
    //Coordinate Addresses:
    //starting addresses (2 bytes - 1ppu reverse format):
    private static final int NUM_SCENES = 3, START_X_OFFSET = 0x2009E,
            START_Y_OFFSET = 0x2009B,
            //flyover coordinate addresses (2 bytes - 1ppu reverse format):
            X_OFFSET[] = {0x3ADEE, 0x3AE28, 0x3AE62}, Y_OFFSET[] = {0x3ADF1,
                0x3AE2B, 0x3AE65}, SCENE3_X2_OFFSET = 0x3AE85,
            SCENE3_Y2_OFFSET = 0x3AE89,
            //teleport addresses (2 bytes - 8ppu reverse format):
            TELE_OFFSET = 0x15EDAB,
            //SCENE2_TEL_X_OFFSET = 0x15F25B,
            //SCENE2_TEL_Y_OFFSET = 0x15F25D,
            TEL_OFFSET[] = {0, 0x05E923, //one byte, should be 0x96, reference
                // to the tp table
                //SCENE3_TEL_X_OFFSET = 0x15F263,
                //SCENE3_TEL_Y_OFFSET = 0x15F265,
                0x05E936}, //one byte, should be 0x97, reference
            // to the tp table
            //movement addresses (1 byte - values from 0 to 7 (from north
            // clockwise to northwest)
            MOVE_OFFSET[] = {0x3AE06, 0x3AE40},

            //Text Pointers:
            //3 byte addresses - reverse snes format, to text block data
            TEXT_OFFSET[] = {0x4A0A4, 0x4A0A8, 0x4A0AC};

    //wrapper for all flyover stuff
    public class Flyover
    {
        private int sceneNumber;

        //only scene 3 has finish coords...
        private int startX, startY, finishX, finishY;

        //backup values
        private int bStartX, bStartY, bFinishX, bFinishY;

        private int teleportOffset, //for scene 2 or 3, should be 96 and 97
                // respectively unless changed
                bTeleportOffset;

        //value from 00 to 07
        private byte movement, bMovement; //backup

        private Text textBlock;

        //send scene 0-2 so it knows which pointers to use
        public Flyover(int sceneNumber)
        {
            this.sceneNumber = sceneNumber;

            startX = readReverseHex(X_OFFSET[sceneNumber], 2);
            startY = readReverseHex(Y_OFFSET[sceneNumber], 2);

            bStartX = startX;
            bStartY = startY;

            if (sceneNumber < 2)
            {
                finishX = finishY = bFinishX = bFinishY = 0;

                movement = rom.readByte(MOVE_OFFSET[sceneNumber]);
                bMovement = movement;
            }
            else
            {
                finishX = readReverseHex(SCENE3_X2_OFFSET, 2);
                finishY = readReverseHex(SCENE3_Y2_OFFSET, 2);

                bFinishX = finishX;
                bFinishY = finishY;

                movement = bMovement = 0;
            }
            if (sceneNumber == 0)
            {
                teleportOffset = bTeleportOffset = 0;
            }
            else
            {
                teleportOffset = rom.readByte(TEL_OFFSET[sceneNumber]) & 0xff;
                bTeleportOffset = teleportOffset;
            }

            textBlock = new Text(TEXT_OFFSET[sceneNumber], false);
        }

        //as a precaution this function will not write anything until all
        // values have been checked
        //startX and y, and finishX and y must be parsable and <= 0xffff
        //direction doesn't need to be checked
        //updateText must work, meaning that it's a parsable field with a
        // defined size...
        //it will check for overwrites, and provide a warning
        //it will handle all of the coords all over, tp coords, start coords,
        // and normal coords
        public boolean writeInfo()
        {
            boolean error = false;
            String errorMessage = new String();
            byte left, right;

            //must update these values before checked otherwise, check if
            // updating fails
            if (!this.parseCoords())
            {
                errorMessage += "invalid coordinate entries.\n";
                error = true;
            }
            //now check actual values
            if (startX < 0 || startX > 0xffff || finishX < 0
                || finishX > 0xffff)
            {
                errorMessage += "coordinate value out of bounds, must be positive and less than 0x10000.\n";
                error = true;
            }
            if (teleportOffset < 0 || teleportOffset > 0xe8)
            {
                errorMessage += "teleport offset must be from 0 to e8.\n";
                error = true;
            }
            //warn the user if they are using a different teleport offset,
            // unless they were using one to start
            else if (sceneNumber != 0
                && !(teleportOffset == 0x96 || teleportOffset == 0x97 || teleportOffset == bTeleportOffset))
            {
                int choice = JOptionPane
                    .showConfirmDialog(
                        null,
                        "Warning! changing the value of the teleport offsets will overwrite the new"
                            + "teleport choices with the necessary values, would you like to proceed?",
                        "Warning!", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.NO_OPTION)
                    return true; //pretend that it was successful so that
                // there's no error message

            }

            if (!textBlock.updateTextPointer())
            {
                errorMessage += "invalid pointer.\n";
                error = true;

            }

            if (!textBlock.updateCurrentArray(sceneArea[sceneNumber].getText()))
            {
                errorMessage += "invalid text block entry for scene "
                    + (sceneNumber + 1) + ".\n";
                error = true;
            }

            if ((textBlock.currentArraySize() > textBlock.originalSize())
                && preventOverwriteToggle)
            {
                errorMessage += "uncheck prevent overwrites or make text shorter.\n";
                error = true;
            }

            if (error)
            {
                JOptionPane.showMessageDialog(null, errorMessage, "error!",
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }

            textBlock.writeTextInfo();

            if (sceneNumber == 0)
            {
                rom.write(START_X_OFFSET, startX, 2);
                rom.write(START_Y_OFFSET, startY, 2);
            }
            else
            {
                rom.seek(TELE_OFFSET + teleportOffset * 0x8);
                rom.writeSeek(startX / 8, 2);
                rom.writeSeek(startY / 8, 2);

                rom.write(TEL_OFFSET[sceneNumber], teleportOffset);
            }
            rom.write(X_OFFSET[sceneNumber], startX, 2);
            rom.write(Y_OFFSET[sceneNumber], startY, 2);
            if (sceneNumber == 2)
            {
                //FINISH
                //X2:
                rom.write(SCENE3_X2_OFFSET, finishX, 2);

                //Y2:
                rom.write(SCENE3_Y2_OFFSET, finishY, 2);
            }
            else
            {
                //direction:
                movement = (byte) directionSceneCombo[sceneNumber]
                    .getSelectedIndex();
                rom.write(MOVE_OFFSET[sceneNumber], movement);
            }

            System.out.println("Flyover Editor: Succesfully saved!");

            return true;
        }

        public void initGUI()
        {
            updateCoords();
            updateText(false);

            if (sceneNumber < 2)
                directionSceneCombo[sceneNumber]
                    .setSelectedIndex((int) movement);
        }

        public void restore()
        {
            startX = bStartX;
            startY = bStartY;

            finishX = bFinishX;
            finishY = bFinishY;

            movement = bMovement;

            teleportOffset = bTeleportOffset;

            initGUI();
        }

        //makes sure the values are proper hex or base 10 numbers, sets startx,
        // y, finishx, y
        public boolean parseCoords()
        {
            try
            {
                switch (numberFormat)
                {
                    case 0:
                        startX = Integer.parseInt(xSceneField[sceneNumber]
                            .getText());
                        startY = Integer.parseInt(ySceneField[sceneNumber]
                            .getText());
                        break;
                    case 1:
                        startX = Integer.parseInt(xSceneField[sceneNumber]
                            .getText(), 16);
                        startY = Integer.parseInt(ySceneField[sceneNumber]
                            .getText(), 16);
                        break;
                    case 2:
                        startX = Integer.parseInt(xSceneField[sceneNumber]
                            .getText(), 16) * 8;
                        startY = Integer.parseInt(ySceneField[sceneNumber]
                            .getText(), 16) * 8;
                        break;
                }
                if (sceneNumber == 2)
                {
                    switch (numberFormat)
                    {
                        case 0:
                            finishX = Integer.parseInt(x2Scene3Field.getText());
                            finishY = Integer.parseInt(y2Scene3Field.getText());
                            break;
                        case 1:
                            finishX = Integer.parseInt(x2Scene3Field.getText(),
                                16);
                            finishY = Integer.parseInt(y2Scene3Field.getText(),
                                16);
                            break;
                        case 2:
                            finishX = Integer.parseInt(x2Scene3Field.getText(),
                                16) * 8;
                            finishY = Integer.parseInt(y2Scene3Field.getText(),
                                16) * 8;
                            break;
                    }
                }
            }
            catch (NumberFormatException e)
            {
                return false;
            }
            return true;
        }

        public int getTeleportOffset()
        {
            return teleportOffset;
        }

        public boolean updateTeleportOffset(int offset)
        {
            if (sceneNumber == 0)
                return false;

            teleportOffset = offset;

            return true;
        }

        public void updateCoords()
        {
            if (!numbersHexToggle)
            {
                xSceneField[sceneNumber].setText(Integer.toString(startX));
                ySceneField[sceneNumber].setText(Integer.toString(startY));
            }
            else
            {
                if (numbers1ppuToggle)
                {
                    xSceneField[sceneNumber].setText(Integer
                        .toHexString(startX));
                    ySceneField[sceneNumber].setText(Integer
                        .toHexString(startY));
                }
                else
                {
                    xSceneField[sceneNumber].setText(Integer
                        .toHexString(startX / 8));
                    ySceneField[sceneNumber].setText(Integer
                        .toHexString(startY / 8));
                }
            }

            if (sceneNumber == 2)
            {
                if (!numbersHexToggle)
                {
                    x2Scene3Field.setText(Integer.toString(finishX));
                    y2Scene3Field.setText(Integer.toString(finishY));
                }
                else
                {
                    if (numbers1ppuToggle)
                    {
                        x2Scene3Field.setText(Integer.toHexString(finishX));
                        y2Scene3Field.setText(Integer.toHexString(finishY));
                    }
                    else
                    {
                        x2Scene3Field.setText(Integer.toHexString(finishX / 8));
                        y2Scene3Field.setText(Integer.toHexString(finishY / 8));
                    }
                }
            }
        }

        //either used to restore original text or update the offset and present
        // that (false and true respectively)
        public boolean updateText(boolean update)
        {
            if (update)
            {
                try
                {
                    int address = Integer.parseInt(
                        textPointerField[sceneNumber].getText(), 16);

                    //this is just making sure that it won't have an
                    // error reading the rom
                    //these values in the bounds should never be used
                    // for a pointer =p
                    if (address < rom.length() - 1 && address >= 0)
                    {
                        textBlock = new Text(address, true);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null,
                            "pointer out of bounds", "error",
                            JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    JOptionPane.showMessageDialog(null,
                        "invalid entry for address 1", "error",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            textBlock.updateGUI();

            return true;
        }

        private class Text
        {
            //pointer to text block, for use if user moves the text block
            private int textPointer;

            //original text block length
            private int originalTextBlockLength;
            //current...update as typing
            private String currentParsedTextBlock;

            public void updateGUI()
            {
                textPointerField[sceneNumber].setText(Integer
                    .toHexString(textPointer));
                sceneArea[sceneNumber].setText(currentParsedTextBlock);
                originalSizeSceneField[sceneNumber].setText(new Integer(
                    originalTextBlockLength).toString());
                currentSizeSceneField[sceneNumber].setText(new Integer(
                    originalTextBlockLength).toString());
            }

            //depends on toggle textHexToggle...
            //if true, pass an array of bytes, returns a string in format "[01
            // 10 20 00]"
            //if false, pass an array of bytes, returns a string with mix of
            // hex (control codes)
            //and normal chars (converted from byte to eb char) ie.
            // "[10]hello[00]"
            //*note* it seperates all control code sets with brackets so that
            // it's easier to tell which are which
            //the parser doesn't care if they're seperated like that however,
            // just as long as they're in brackets
            private String toFormattedText(String array)
            {
                return textParser.parseString(array, true, textHexToggle);
            }

            //gets size from currentSize, then assumes that the string is
            // formatted correctly for it's algorithm
            public boolean updateCurrentArray(String text)
            {
                if (textParser.getStringLength(text) != -1)
                {
                    currentParsedTextBlock = text;
                    return true;
                }
                else
                {
                    return false;
                }
            }

            //this reads either an address from the rom then reads from that
            // address, or reads directly
            //from the specified address
            public Text(int address, boolean actualAddress)
            {
                if (!actualAddress)
                    textPointer = toRegPointer(readReverseHex(address, 3));
                else
                    textPointer = address;

                StrInfo si = textParser.readString(textPointer);
                originalTextBlockLength = si.str.length();
                currentParsedTextBlock = toFormattedText(si.str);
            }

            //updates the pointer, catches invalid and out of bounds errors
            protected boolean updateTextPointer()
            {
                try
                {
                    int address = Integer.parseInt(
                        textPointerField[sceneNumber].getText(), 16);

                    //this is just making sure that it won't have
                    // an error reading the rom
                    //these values in the bounds should never be
                    // used for a pointer =p
                    if (address < rom.length() - 1 && address >= 0)
                    {
                        textBlock = new Text(address, true);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null,
                            "pointer out of bounds", "error",
                            JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    return false;
                }
                return true;
            }

            protected void writeTextInfo()
            {
                rom.write(TEXT_OFFSET[sceneNumber], toSnesPointer(textPointer),
                    3);

                //write the block
                //System.out.println( "text Block: size = " +
                // currentTextBlock.length + " data = " + currentTextBlock );
                //System.out.println( "Writing text block " + sceneNumber + "
                // to offset " + textPointer );
                textParser.writeString(textParser
                    .deparseString(currentParsedTextBlock), textPointer);
            }

            public int originalSize()
            {
                return originalTextBlockLength;
            }

            public int currentArraySize()
            {
                return textParser.getStringLength(currentParsedTextBlock);
            }

            //parses text length
            //throws an exception if unable to parse...should only be unable to
            // parse if there's invalid hex codes..
            //or unfinished ones...ie. "[01 04 02 "

            //doubles array size if needed
            private byte[] doubleArray(byte[] array)
            {
                byte[] returnValue = new byte[array.length * 2];
                System.arraycopy(array, 0, returnValue, 0, array.length);
                return returnValue;
            }
            /*
             * TODO: add parser function that parses a text field on events add
             * parser function for raw hex as well also add something to output
             * both from the array at beginning
             */
        }

        private int readReverseHex(int offset, int length)
        {
            return rom.readMulti(offset, length);
        }
    }

    //parses string text and returns the current size
    //brackets do not count against the size, nor do spaces in brackets, as
    // long as the brackets are not nested
    //ie. [][][][][aa 01 ]dajdkdjfad[][00] is a valid string (size 13)
    public int currentSize(String text) throws UndefinedSizeException,
        InvalidNumberFormatException
    {
        return textParser.getStringLength(text);
    }

    class InvalidNumberFormatException extends NumberFormatException
    {
        public InvalidNumberFormatException()
        {
            super(
                "invalid number format, hex values range from 0-9 and a-f only");
        }
    }

    class UndefinedSizeException extends Exception
    {
        public UndefinedSizeException()
        {
            super("Undefined text block size");
        }
    }

    public void show()
    {
        super.show();

        for (int i = 0; i < NUM_SCENES; i++)
        {
            scene[i] = new Flyover(i);
            scene[i].initGUI();
        }

        mainWindow.setVisible(true);
    }

    public String getCredits()
    {
        return "Code by Chris - all the addresses and data info from BA";
    }

    public String getDescription()
    {
        return "Flyover Editor";
    }

    public String getVersion()
    {
        return "0.2";
    }

    public void hide()
    {
        mainWindow.setVisible(false);
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("close"))
            hide();

        if (ae.getActionCommand().equals("apply"))
        {
            for (int i = 0; i < NUM_SCENES; i++)
            {
                if (!scene[i].writeInfo())
                    JOptionPane.showMessageDialog(null, "Error saving scene "
                        + (i + 1), "error", JOptionPane.ERROR_MESSAGE);
            }
        }

        else if (ae.getActionCommand().equals("Restore Values"))
        {
            System.out.println("restoring from backups");

            for (int i = 0; i < NUM_SCENES; i++)
            {
                scene[i].restore();
            }
        }

        else if (ae.getSource() == teleportMenuItem)
        {
            TPDialog dialog = new TPDialog(mainWindow, true);
            dialog.setVisible(true);
        }

        else if (ae.getSource() == blueFlyoverTutorialMenuItem)
        {
            try
            {
                JEditorPane pane = new JEditorPane(new URL(
                    "http://pkhack.starmen.net/down/tuts/flyover_edit.html"));
                pane.setEditable(false);
                JScrollPane helpPane = new JScrollPane(pane);
                helpPane
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                //helpPane.setPreferredSize(new Dimension(480, 480));
                //helpPane.setMinimumSize(new Dimension(320, 320));

                JFrame helpWindow = new JFrame();
                helpWindow.getContentPane().add(helpPane);
                helpWindow.setSize(new Dimension(540, 540));
                helpWindow.setTitle("Blue Antoid's Flyover Tutorial");

                helpWindow.setVisible(true);
                //JOptionPane.showMessageDialog( null, pane.getDocument(),
                // "Blue Antoid's Flyover Tutorial", JOptionPane.OK_OPTION );
            }
            catch (MalformedURLException e) //do nothing because this shouldn't
            // happen
            {}
            catch (IOException e)
            {
                JOptionPane.showMessageDialog(null,
                    "Sorry, it appears blue antoid's guide is down right now",
                    "error", JOptionPane.ERROR_MESSAGE);
            }

        }

        else if (ae.getSource() == aboutFlyoverMenuItem)
        {
            try
            {
                JEditorPane pane = new JEditorPane(
                    new URL(
                        "http://www.geocities.com/xeminemxgurlx/Flyover_Editor.html"));
                pane.setEditable(false);
                JScrollPane helpPane = new JScrollPane(pane);
                helpPane
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                //helpPane.setPreferredSize(new Dimension(480, 480));
                //helpPane.setMinimumSize(new Dimension(320, 320));

                JFrame helpWindow = new JFrame();
                helpWindow.getContentPane().add(helpPane);
                helpWindow.setSize(new Dimension(480, 480));
                helpWindow
                    .setTitle("Chris_Davis' shorter more confusing guide");

                helpWindow.setVisible(true);
                //JOptionPane.showMessageDialog( null, pane.getDocument(),
                // "Blue Antoid's Flyover Tutorial", JOptionPane.OK_OPTION );
            }
            catch (MalformedURLException e) //do nothing because this shouldn't
            // happen
            {}
            catch (IOException e)
            {
                JOptionPane.showMessageDialog(null,
                    "this guide is temporarily down...sorry =(", "error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        //POINTER UPDATES
        else if (ae.getSource() == textPointerUpdateButton[0])
            scene[0].updateText(true);

        else if (ae.getSource() == textPointerUpdateButton[1])
            scene[1].updateText(true);

        else if (ae.getSource() == textPointerUpdateButton[2])
            scene[2].updateText(true);

        //NUMBER FORMAT
        else if (ae.getActionCommand().equals("Standard Number Format"))
        {
            System.out.println("standard");

            switch (numberFormat)
            {
                case 0:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                case 1:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = true;
                        numbers1ppuToggle = true;

                        number1ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;

                        numberFormat = 0;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }

                    break;
                case 2:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = true;
                        numbers1ppuToggle = false;

                        number8ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;

                        numberFormat = 0;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }

                    break;
            }

        }

        else if (ae.getActionCommand().equals("1ppu Number Format"))
        {
            System.out.println("1ppu");
            switch (numberFormat)
            {
                case 0:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = false;
                        numbers1ppuToggle = true;

                        numberStandardRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;

                        numberFormat = 1;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }
                    break;
                case 1:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                case 2:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = true;
                        numbers1ppuToggle = false;

                        number8ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;

                        numberFormat = 1;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }
                    break;
            }
        }

        else if (ae.getActionCommand().equals("8ppu Number Format"))
        {
            System.out.println("8ppu");
            switch (numberFormat)
            {
                case 0:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = false;
                        numbers1ppuToggle = true;

                        numberStandardRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;

                        numberFormat = 2;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }
                    break;
                case 1:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);

                        numbersHexToggle = true;
                        numbers1ppuToggle = true;

                        number1ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;

                        numberFormat = 2;

                        for (int i = 0; i < NUM_SCENES; i++)
                        {
                            scene[i].updateCoords();
                        }
                    }
                    break;
                case 2:
                    if (!scene[0].parseCoords() || !scene[1].parseCoords()
                        || !scene[2].parseCoords())
                    {
                        JOptionPane
                            .showMessageDialog(
                                null,
                                "error parsing coordinate field, please use a valid number",
                                "error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
            }
        }
    }

    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getSource() == hexTextCheckBox)
        {
            if (ie.getStateChange() == ItemEvent.SELECTED)
            {
                textHexToggle = true;
            }
            else
            {
                textHexToggle = false;
            }

            if (!scene[0].textBlock.updateCurrentArray(sceneArea[0].getText())
                || !scene[1].textBlock.updateCurrentArray(sceneArea[1]
                    .getText())
                || !scene[2].textBlock.updateCurrentArray(sceneArea[2]
                    .getText()))
            {
                JOptionPane.showMessageDialog(null,
                    "error converting the string", "error",
                    JOptionPane.ERROR_MESSAGE);

                if (hexTextCheckBox.getState())
                {
                    textHexToggle = false;
                    hexTextCheckBox.setState(false);
                }
                else
                {
                    textHexToggle = false;
                    hexTextCheckBox.setState(false);
                }
            }
            else
            {
                for (int i = 0; i < NUM_SCENES; i++)
                {
                    scene[i].updateText(false);
                }

                try
                {
                    for (int i = 0; i < NUM_SCENES; i++)
                    {
                        currentSizeSceneField[i].setText(new Integer(
                            currentSize(sceneArea[i].getText())).toString());
                    }
                }
                catch (InvalidNumberFormatException e)
                {

                }
                catch (UndefinedSizeException e)
                {

                }
            }

        }
        else if (ie.getSource() == preventOverwritesCheckBox)
        {
            if (ie.getStateChange() == ItemEvent.DESELECTED)
                preventOverwriteToggle = false;
            else
                preventOverwriteToggle = true;
        }
    }

    public void changedUpdate(DocumentEvent de)
    {
        for (int i = 0; i < NUM_SCENES; i++)
        {
            if (de.getDocument() == sceneArea[i].getDocument())
            {
                try
                {
                    currentSizeSceneField[i].setText(new Integer(
                        currentSize(sceneArea[i].getText())).toString());
                }
                catch (InvalidNumberFormatException e)
                {
                    currentSizeSceneField[i].setText(" ");
                }
                catch (UndefinedSizeException e)
                {
                    currentSizeSceneField[i].setText(" ");
                }
                return;
            }
        }
    }

    public void insertUpdate(DocumentEvent de)
    {
        changedUpdate(de);
    }

    public void removeUpdate(DocumentEvent de)
    {
        changedUpdate(de);
    }

    //keep this at the end cuz it's so long and pretty much uneditable (grid
    // bag layouts)...and ugly
    protected void init()
    {
        scene = new Flyover[NUM_SCENES];
        textParser = new CCInfo(DEFAULT_BASE_DIR + "teacodelist.txt", rom,
            false, false);
        textPointerField = new JTextField[NUM_SCENES];
        sceneArea = new JTextArea[NUM_SCENES];
        originalSizeSceneField = new JTextField[NUM_SCENES];
        currentSizeSceneField = new JTextField[NUM_SCENES];
        directionSceneCombo = new JComboBox[2];
        xSceneField = new JTextField[NUM_SCENES];
        ySceneField = new JTextField[NUM_SCENES];
        textPointerUpdateButton = new JButton[NUM_SCENES];

        GridBagConstraints gridBagConstraints;

        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        //mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
        mainWindow.setSize(210, 450);
        mainWindow.setResizable(false);

        JPanel mainPanel = new JPanel();

        ButtonGroup numberFormatButtonGroup = new ButtonGroup();
        JTabbedPane flyoverPanel = new JTabbedPane();
        JPanel scene1Panel = new JPanel();
        JLabel startLabel1 = new JLabel();
        JLabel xScene1Label = new JLabel();
        JLabel yScene1Label = new JLabel();
        directionSceneCombo[0] = new JComboBox(directions);
        JLabel directionScene1Label = new JLabel();
        xSceneField[0] = new JTextField();
        ySceneField[0] = new JTextField();
        JLabel textPointer1Label = new JLabel();
        for (int i = 0; i < NUM_SCENES; i++)
        {
            textPointerField[i] = new JTextField();
            sceneArea[i] = new JTextArea();
            originalSizeSceneField[i] = new JTextField();
            currentSizeSceneField[i] = new JTextField();
        }
        textPointerUpdateButton[0] = new JButton();
        JLabel originalSizeLabel1 = new JLabel();
        JLabel currentSizeLabel1 = new JLabel();
        JPanel scene2Panel = new JPanel();
        JLabel startLabel2 = new JLabel();
        JLabel xScene2Label = new JLabel();
        JLabel yScene2Label = new JLabel();
        directionSceneCombo[1] = new JComboBox(directions);
        JLabel directionScene2Label = new JLabel();
        xSceneField[1] = new JTextField();
        ySceneField[1] = new JTextField();
        JLabel textPointer2Label = new JLabel();
        textPointerUpdateButton[1] = new JButton();
        JLabel originalSizeLabel2 = new JLabel();
        JLabel currentSizeLabel2 = new JLabel();
        JPanel scene3Panel = new JPanel();
        JLabel startAndEndLabel = new JLabel();
        JLabel xScene3Label = new JLabel();
        JLabel yScene3Label = new JLabel();
        xSceneField[2] = new JTextField();
        ySceneField[2] = new JTextField();
        JLabel textPointer3Label = new JLabel();
        textPointerUpdateButton[2] = new JButton();
        JLabel originalSizeLabel3 = new JLabel();
        JLabel currentSizeLabel3 = new JLabel();
        y2Scene3Field = new JTextField();
        JLabel y2Scene3Label = new JLabel();
        x2Scene3Field = new JTextField();
        JLabel x2Scene3Label = new JLabel();
        JMenuBar flyoverMenu = new JMenuBar();
        JMenu optionsMenu = new JMenu();
        preventOverwritesCheckBox = new JCheckBoxMenuItem();
        hexTextCheckBox = new JCheckBoxMenuItem();
        numberStandardRadio = new JRadioButtonMenuItem();
        number8ppuRadio = new JRadioButtonMenuItem();
        number1ppuRadio = new JRadioButtonMenuItem();
        teleportMenuItem = new JMenuItem();
        JMenu helpMenu = new JMenu();
        blueFlyoverTutorialMenuItem = new JMenuItem();
        aboutFlyoverMenuItem = new JMenuItem();
        JMenuItem restoreValuesMenuItem = new JMenuItem();
        JPanel scene1BPanel = new JPanel();
        JPanel scene2BPanel = new JPanel();
        JPanel scene3BPanel = new JPanel();

        flyoverPanel.setFont(new Font("Dialog", 1, 10));
        scene1Panel.setLayout(new GridBagLayout());

        scene1Panel.setFont(new Font("Dialog", 0, 10));
        startLabel1.setText("Start Coordinates & Movement:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new Insets(10, 10, 0, 8);
        scene1Panel.add(startLabel1, gridBagConstraints);

        xScene1Label.setText("x:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(4, 20, 0, 44);
        scene1Panel.add(xScene1Label, gridBagConstraints);

        yScene1Label.setText(" y:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 8;
        gridBagConstraints.insets = new Insets(4, 36, 0, 0);
        scene1Panel.add(yScene1Label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 9;
        gridBagConstraints.ipady = -5;
        gridBagConstraints.insets = new Insets(14, 6, 0, 0);
        scene1Panel.add(directionSceneCombo[0], gridBagConstraints);

        directionScene1Label.setText("Direction:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(14, 20, 4, 0);
        scene1Panel.add(directionScene1Label, gridBagConstraints);

        xSceneField[0].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 40, 30, 40);
        scene1Panel.add(xSceneField[0], gridBagConstraints);

        ySceneField[0].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 30, 0);
        scene1Panel.add(ySceneField[0], gridBagConstraints);

        textPointer1Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 4, 38);
        scene1Panel.add(textPointer1Label, gridBagConstraints);

        textPointerField[0].setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene1Panel.add(textPointerField[0], gridBagConstraints);

        textPointerUpdateButton[0].setText("Update Text from Pointer");
        //mainWindow.getContentPane().add( textPointerUpdateButton[0] ,
        // BorderLayout.SOUTH );
        textPointerUpdateButton[1].setText("Update Text from Pointer");
        textPointerUpdateButton[2].setText("Update Text from Pointer");
        /*
         * gridBagConstraints = new GridBagConstraints();
         * gridBagConstraints.gridx = 2; gridBagConstraints.gridy = 3;
         * gridBagConstraints.gridwidth = 2; gridBagConstraints.insets = new
         * Insets(10, 20, 4, 38); scene1Panel.add(textPointer1Label,
         * gridBagConstraints);
         */

        originalSizeLabel1.setText("Original Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 0, 36);
        scene1Panel.add(originalSizeLabel1, gridBagConstraints);

        currentSizeLabel1.setText("Current Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(4, 20, 4, 37);
        scene1Panel.add(currentSizeLabel1, gridBagConstraints);

        originalSizeSceneField[0].setEditable(false);
        originalSizeSceneField[0].setText("0");
        originalSizeSceneField[0].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene1Panel.add(originalSizeSceneField[0], gridBagConstraints);

        currentSizeSceneField[0].setEditable(false);
        currentSizeSceneField[0].setText("0");
        currentSizeSceneField[0].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 26, 0, 0);
        scene1Panel.add(currentSizeSceneField[0], gridBagConstraints);

        sceneArea[0].setColumns(20);
        sceneArea[0].setLineWrap(true);
        sceneArea[0].setWrapStyleWord(true);
        sceneArea[0].setTabSize(0);
        sceneArea[0].setBorder(new LineBorder(new Color(0, 0, 0)));
        sceneArea[0].setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 0);
        scene1Panel.add(new JScrollPane(sceneArea[0]), gridBagConstraints);

        scene1BPanel.setLayout(new BorderLayout());
        scene1BPanel.add(scene1Panel, BorderLayout.CENTER);
        scene1BPanel.add(textPointerUpdateButton[0], BorderLayout.SOUTH);
        flyoverPanel.addTab("Scene 1", scene1BPanel);

        scene2Panel.setLayout(new GridBagLayout());

        scene2Panel.setFont(new Font("Dialog", 0, 10));
        startLabel2.setText("Start Coordinates & Movement:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new Insets(10, 10, 0, 8);
        scene2Panel.add(startLabel2, gridBagConstraints);

        xScene2Label.setText("x:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(4, 20, 0, 44);
        scene2Panel.add(xScene2Label, gridBagConstraints);

        yScene2Label.setText(" y:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 8;
        gridBagConstraints.insets = new Insets(4, 36, 0, 0);
        scene2Panel.add(yScene2Label, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 9;
        gridBagConstraints.ipady = -5;
        gridBagConstraints.insets = new Insets(14, 6, 0, 0);
        scene2Panel.add(directionSceneCombo[1], gridBagConstraints);

        directionScene2Label.setText("Direction:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(14, 20, 4, 0);
        scene2Panel.add(directionScene2Label, gridBagConstraints);

        xSceneField[1].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 40, 30, 40);
        scene2Panel.add(xSceneField[1], gridBagConstraints);

        ySceneField[1].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 30, 0);
        scene2Panel.add(ySceneField[1], gridBagConstraints);

        textPointer2Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 4, 38);
        scene2Panel.add(textPointer2Label, gridBagConstraints);

        textPointerField[1].setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene2Panel.add(textPointerField[1], gridBagConstraints);

        originalSizeLabel2.setText("Original Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 0, 36);
        scene2Panel.add(originalSizeLabel2, gridBagConstraints);

        currentSizeLabel2.setText("Current Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(4, 20, 4, 37);
        scene2Panel.add(currentSizeLabel2, gridBagConstraints);

        originalSizeSceneField[1].setEditable(false);
        originalSizeSceneField[1].setText("0");
        originalSizeSceneField[1].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene2Panel.add(originalSizeSceneField[1], gridBagConstraints);

        currentSizeSceneField[1].setEditable(false);
        currentSizeSceneField[1].setText("0");
        currentSizeSceneField[1].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 26, 0, 0);
        scene2Panel.add(currentSizeSceneField[1], gridBagConstraints);

        sceneArea[1].setColumns(20);
        sceneArea[1].setLineWrap(true);
        sceneArea[1].setWrapStyleWord(true);
        sceneArea[1].setTabSize(0);
        sceneArea[1].setBorder(new LineBorder(new Color(0, 0, 0)));
        sceneArea[1].setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 0);
        scene2Panel.add(new JScrollPane(sceneArea[1]), gridBagConstraints);

        scene2BPanel.setLayout(new BorderLayout());
        scene2BPanel.add(scene2Panel, BorderLayout.CENTER);
        scene2BPanel.add(textPointerUpdateButton[1], BorderLayout.SOUTH);
        flyoverPanel.addTab("Scene 2", scene2BPanel);

        scene3Panel.setLayout(new GridBagLayout());

        scene3Panel.setFont(new Font("Dialog", 0, 10));
        startAndEndLabel.setText("Start & End Coordinates:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new Insets(10, 10, 0, 46);
        scene3Panel.add(startAndEndLabel, gridBagConstraints);

        xScene3Label.setText("x:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(4, 20, 0, 0);
        scene3Panel.add(xScene3Label, gridBagConstraints);

        yScene3Label.setText(" y:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 8;
        gridBagConstraints.insets = new Insets(4, 20, 0, 0);
        scene3Panel.add(yScene3Label, gridBagConstraints);

        xSceneField[2].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 10, 26, 0);
        scene3Panel.add(xSceneField[2], gridBagConstraints);

        ySceneField[2].setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 26, 15);
        scene3Panel.add(ySceneField[2], gridBagConstraints);

        textPointer3Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new Insets(14, 20, 4, 38);
        scene3Panel.add(textPointer3Label, gridBagConstraints);

        textPointerField[2].setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(14, 10, 0, 15);
        scene3Panel.add(textPointerField[2], gridBagConstraints);

        originalSizeLabel3.setText("Original Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new Insets(10, 20, 0, 36);
        scene3Panel.add(originalSizeLabel3, gridBagConstraints);

        currentSizeLabel3.setText("Current Size:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new Insets(4, 20, 4, 37);
        scene3Panel.add(currentSizeLabel3, gridBagConstraints);

        originalSizeSceneField[2].setEditable(false);
        originalSizeSceneField[2].setText("0");
        originalSizeSceneField[2].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        scene3Panel.add(originalSizeSceneField[2], gridBagConstraints);

        currentSizeSceneField[2].setEditable(false);
        currentSizeSceneField[2].setText("0");
        currentSizeSceneField[2].setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 10, 0, 0);
        scene3Panel.add(currentSizeSceneField[2], gridBagConstraints);

        sceneArea[2].setColumns(20);
        sceneArea[2].setLineWrap(true);
        sceneArea[2].setWrapStyleWord(true);
        sceneArea[2].setTabSize(0);
        sceneArea[2].setBorder(new LineBorder(new Color(0, 0, 0)));
        sceneArea[2].setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 15);
        scene3Panel.add(new JScrollPane(sceneArea[2]), gridBagConstraints);

        y2Scene3Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(14, 0, 30, 15);
        scene3Panel.add(y2Scene3Field, gridBagConstraints);

        y2Scene3Label.setText(" y2:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 1;
        gridBagConstraints.insets = new Insets(14, 20, 0, 0);
        scene3Panel.add(y2Scene3Label, gridBagConstraints);

        x2Scene3Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(14, 10, 30, 0);
        scene3Panel.add(x2Scene3Field, gridBagConstraints);

        x2Scene3Label.setText("x2:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(14, 20, 0, 53);
        scene3Panel.add(x2Scene3Label, gridBagConstraints);

        scene3BPanel.setLayout(new BorderLayout());
        scene3BPanel.add(scene3Panel, BorderLayout.CENTER);
        scene3BPanel.add(textPointerUpdateButton[2], BorderLayout.SOUTH);
        flyoverPanel.addTab("Scene 3", scene3BPanel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 30, 2);
        mainPanel.add(flyoverPanel, gridBagConstraints);

        optionsMenu.setText("Options");

        restoreValuesMenuItem.setText("Restore Values");
        optionsMenu.add(restoreValuesMenuItem);

        optionsMenu.addSeparator();

        preventOverwritesCheckBox.setSelected(true);
        preventOverwritesCheckBox.setText("Prevent Overwrites");
        optionsMenu.add(preventOverwritesCheckBox);

        hexTextCheckBox.setText("Hex Text");
        optionsMenu.add(hexTextCheckBox);

        optionsMenu.addSeparator();

        numberStandardRadio.setSelected(true);
        numberStandardRadio.setText("Standard Number Format");
        numberFormatButtonGroup.add(numberStandardRadio);
        optionsMenu.add(numberStandardRadio);

        number8ppuRadio.setText("8ppu Number Format");
        numberFormatButtonGroup.add(number8ppuRadio);
        optionsMenu.add(number8ppuRadio);

        number1ppuRadio.setText("1ppu Number Format");
        numberFormatButtonGroup.add(number1ppuRadio);
        optionsMenu.add(number1ppuRadio);

        optionsMenu.addSeparator();

        teleportMenuItem.setText("Change Teleport Links");
        //teleportMenuItem.setEnabled(false);
        optionsMenu.add(teleportMenuItem);

        flyoverMenu.add(optionsMenu);

        helpMenu.setText("Help");
        blueFlyoverTutorialMenuItem.setText("BA's Flyover Tutorial");
        helpMenu.add(blueFlyoverTutorialMenuItem);

        aboutFlyoverMenuItem.setText("My Simple Guide");
        helpMenu.add(aboutFlyoverMenuItem);

        flyoverMenu.add(helpMenu);

        mainWindow.getContentPane().add(flyoverMenu, BorderLayout.NORTH);
        mainWindow.getContentPane().add(mainPanel, BorderLayout.CENTER);

        //listeners
        hexTextCheckBox.addItemListener(this);

        numberStandardRadio.addActionListener(this);
        number1ppuRadio.addActionListener(this);
        number8ppuRadio.addActionListener(this);

        teleportMenuItem.addActionListener(this);

        sceneArea[0].getDocument().addDocumentListener(this);
        sceneArea[1].getDocument().addDocumentListener(this);
        sceneArea[2].getDocument().addDocumentListener(this);

        restoreValuesMenuItem.addActionListener(this);

        textPointerUpdateButton[0].addActionListener(this);
        textPointerUpdateButton[1].addActionListener(this);
        textPointerUpdateButton[2].addActionListener(this);

        preventOverwritesCheckBox.addItemListener(this);

        blueFlyoverTutorialMenuItem.addActionListener(this);
        aboutFlyoverMenuItem.addActionListener(this);
    }

    public class TPDialog extends javax.swing.JDialog implements ActionListener
    {

        /** Creates new form tpform */
        public TPDialog(java.awt.Frame parent, boolean modal)
        {
            super(parent, modal);
            initComponents();
        }

        /**
         * This method is called from within the constructor to initialize the
         * form. WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
        private void initComponents()
        {
            warningLabel = new javax.swing.JLabel();
            jPanel1 = new javax.swing.JPanel();
            tpScene2Label = new javax.swing.JLabel();
            tpScene2Field = new javax.swing.JTextField();
            tpScene3Label = new javax.swing.JLabel();
            tpScene3Field = new javax.swing.JTextField();
            updateButton = new javax.swing.JButton();
            cancelButton = new javax.swing.JButton();

            setTitle("Teleport Address changer");
            setName("teleportAddressChangerDialog");
            setResizable(false);
            addWindowListener(new java.awt.event.WindowAdapter()
            {
                public void windowClosing(java.awt.event.WindowEvent evt)
                {
                    closeDialog(evt);
                }
            });

            warningLabel
                .setText("If you don't know what these are, don't change them");
            getContentPane().add(warningLabel, java.awt.BorderLayout.NORTH);

            jPanel1.setLayout(new java.awt.GridLayout(0, 2, 5, 10));

            tpScene2Label.setText("Scene 2 TP (0x96):");
            jPanel1.add(tpScene2Label);

            tpScene2Field.setText(Integer.toString(
                scene[1].getTeleportOffset(), 16));
            jPanel1.add(tpScene2Field);

            tpScene3Label.setText("Scene 3 TP (0x97):");
            jPanel1.add(tpScene3Label);

            tpScene3Field.setText(Integer.toString(
                scene[2].getTeleportOffset(), 16));
            jPanel1.add(tpScene3Field);

            updateButton.setText("Update");
            jPanel1.add(updateButton);

            cancelButton.setText("Cancel");
            jPanel1.add(cancelButton);

            getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

            pack();

            cancelButton.addActionListener(this);
            updateButton.addActionListener(this);
        }

        /** Closes the dialog */
        private void closeDialog(java.awt.event.WindowEvent evt)
        {
            setVisible(false);
            dispose();
        }

        public void actionPerformed(ActionEvent ae)
        {
            if (ae.getSource() == cancelButton)
                closeDialog(null);
            else if (ae.getSource() == updateButton)
            {
                try
                {
                    int scene2offset = Integer.parseInt(
                        tpScene2Field.getText(), 16);
                    int scene3offset = Integer.parseInt(
                        tpScene3Field.getText(), 16);

                    if ((scene2offset < 0 || scene2offset > 0xe8)
                        || (scene3offset < 0 || scene3offset > 0xe8))
                    {
                        JOptionPane.showMessageDialog(null,
                            "must be from 0 to e8", "error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    else
                    {
                        scene[1].updateTeleportOffset(scene2offset);
                        scene[2].updateTeleportOffset(scene3offset);

                        closeDialog(null);
                    }
                }
                catch (NumberFormatException e)
                {
                    JOptionPane.showMessageDialog(null, "Invalid entry",
                        "error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // Variables declaration - do not modify
        private javax.swing.JButton cancelButton;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextField tpScene2Field;
        private javax.swing.JLabel tpScene2Label;
        private javax.swing.JTextField tpScene3Field;
        private javax.swing.JLabel tpScene3Label;
        private javax.swing.JButton updateButton;
        private javax.swing.JLabel warningLabel;
        // End of variables declaration

    }

    //GUI Variable declaration - do not modify

    //MENU:
    //options section:
    private JCheckBoxMenuItem preventOverwritesCheckBox, hexTextCheckBox;
    private JRadioButtonMenuItem numberStandardRadio, number1ppuRadio,
            number8ppuRadio;
    private JMenuItem teleportMenuItem;
    //help section
    private JMenuItem aboutFlyoverMenuItem, blueFlyoverTutorialMenuItem;

    //TEXT:
    //pointers
    private JTextField[] textPointerField;
    private JButton[] textPointerUpdateButton;
    //original size:
    private JTextField[] originalSizeSceneField;
    //current size:
    private JTextField[] currentSizeSceneField;
    //text blocks
    private JTextArea[] sceneArea;

    //MOVEMENT:
    //coordinates
    private JTextField[] xSceneField, ySceneField;
    //scene 3
    private JTextField x2Scene3Field, y2Scene3Field;
    //direction
    private JComboBox[] directionSceneCombo;

    // End of GUI variables declaration

}
