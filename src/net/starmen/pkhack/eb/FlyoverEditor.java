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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.starmen.pkhack.AbstractRom;
import net.starmen.pkhack.XMLPreferences;

// code by Chris (AIM: daemionx)
// credits to anyoneEB and BA...for JHack and the flyover tutorial respectively

public class FlyoverEditor extends EbHackModule implements ActionListener, ItemListener, DocumentListener
{
    /**
     * @param rom
     * @param prefs
     */
    public FlyoverEditor(AbstractRom rom, XMLPreferences prefs) {
        super(rom, prefs);
    }
    //variables other than GUI:
    private Flyover scene1, scene2, scene3;
    
    private boolean textHexToggle = false;
    
    private boolean preventOverwriteToggle = true;
    
    //number format conversions...1pp and standard are just a difference of different bases
    //8ppu = hex and 1/8 of standard
    private int     numberFormat = 0;
    private boolean numbersHexToggle = false;
    private boolean numbers1ppuToggle = true;
    
    
    private static String [] directions = new String [] {
        "00 - North",
        "01 - N. East",
        "02 - East",
        "03 - S. East",
        "04 - South",
        "05 - S. West",
        "06 - West",
        "07 - N. West" };
    
    //CONSTANTS:
    //Coordinate Addresses:
    //starting addresses (2 bytes - 1ppu reverse format):
    private static final int    
        START_X_OFFSET      = 0x2009E,
        START_Y_OFFSET      = 0x2009B,
    //flyover coordinate addresses (2 bytes - 1ppu reverse format):
        SCENE1_X_OFFSET     = 0x3ADEE,
        SCENE1_Y_OFFSET     = 0x3ADF1,
        SCENE2_X_OFFSET     = 0x3AE28,
        SCENE2_Y_OFFSET     = 0x3AE2B,
        SCENE3_X_OFFSET     = 0x3AE62,
        SCENE3_Y_OFFSET     = 0x3AE65,
        SCENE3_X2_OFFSET    = 0x3AE85,
        SCENE3_Y2_OFFSET    = 0x3AE89,
    //teleport addresses (2 bytes - 8ppu reverse format):
        TELE_OFFSET         = 0x15EDAB,
        //SCENE2_TEL_X_OFFSET = 0x15F25B,
        //SCENE2_TEL_Y_OFFSET = 0x15F25D,
        SCENE2_TEL_OFFSET   = 0x05E923, //one byte, should be 0x96, reference to the tp table
        //SCENE3_TEL_X_OFFSET = 0x15F263,
        //SCENE3_TEL_Y_OFFSET = 0x15F265,
        SCENE3_TEL_OFFSET   = 0x05E936, //one byte, should be 0x97, reference to the tp table
    //movement addresses (1 byte - values from 0 to 7 (from north clockwise to northwest)
        SCENE1_MOVE_OFFSET  = 0x3AE06,
        SCENE2_MOVE_OFFSET  = 0x3AE40,
        
    //Text Pointers:
    //3 byte addresses - reverse snes format, to text block data
        SCENE1_TEXT_OFFSET  = 0x4A0A4,
        SCENE2_TEXT_OFFSET  = 0x4A0A8,
        SCENE3_TEXT_OFFSET  = 0x4A0AC;
    
    //wrapper for all flyover stuff
    public class Flyover
    {
        private int     sceneNumber;
        
        //only scene 3 has finish coords...
        private int     startX,
                        startY,
                        finishX,
                        finishY;
        
        //backup values
        private int     bStartX,
                        bStartY,
                        bFinishX, 
                        bFinishY;
        
        private int     teleportOffset, //for scene 2 or 3, should be 96 and 97 respectively unless changed
                        bTeleportOffset;
        
        //value from 00 to 07
        private byte    movement,
                        bMovement; //backup
        
        private Text    textBlock;
        
        //send scene 0-2 so it knows which pointers to use
        public Flyover( int sceneNum )
        {
            sceneNumber = sceneNum;
            
            switch( sceneNumber )
            {
                case 0:
                    startX = readReverseHex( SCENE1_X_OFFSET, 2 );
                    startY = readReverseHex( SCENE1_Y_OFFSET, 2 );
                    
                    bStartX = startX;
                    bStartY = startY;
                    
                    finishX = finishY = bFinishX = bFinishY = 0;
                    
                    movement = rom.readByte( SCENE1_MOVE_OFFSET );
                    bMovement = movement;
                    
                    teleportOffset = bTeleportOffset = 0;
                    
                    textBlock = new Text( SCENE1_TEXT_OFFSET, false );
                    break;
                
                case 1:
                    startX = readReverseHex( SCENE2_X_OFFSET, 2 );
                    startY = readReverseHex( SCENE2_Y_OFFSET, 2 );
                    
                    bStartX = startX;
                    bStartY = startY;
                    
                    finishX = finishY = bFinishX = bFinishY = 0;
                    
                    movement = rom.readByte( SCENE2_MOVE_OFFSET );
                    bMovement = movement;
                    
                    teleportOffset = rom.readByte( SCENE2_TEL_OFFSET ) & 0xff;
                    bTeleportOffset = teleportOffset;
                    
                    textBlock = new Text( SCENE2_TEXT_OFFSET, false );
                    break;
                    
                case 2:
                    startX = readReverseHex( SCENE3_X_OFFSET, 2 );
                    startY = readReverseHex( SCENE3_Y_OFFSET, 2 );
                    
                    bStartX = startX;
                    bStartY = startY;
                    
                    finishX = readReverseHex( SCENE3_X2_OFFSET, 2 );
                    finishY = readReverseHex( SCENE3_Y2_OFFSET, 2 );
                    
                    movement = bMovement = 0;
                    
                    teleportOffset = rom.readByte( SCENE3_TEL_OFFSET ) & 0xff;
                    bTeleportOffset = teleportOffset;
                    
                    bFinishX = finishX;
                    bFinishY = finishY;
                    
                    textBlock = new Text( SCENE3_TEXT_OFFSET, false );
                    break;
                    
            }
        }
        
        //as a precaution this function will not write anything until all values have been checked
        //startX and y, and finishX and y must be parsable and <= 0xffff
        //direction doesn't need to be checked
        //updateText must work, meaning that it's a parsable field with a defined size...
        //it will check for overwrites, and provide a warning
        //it will handle all of the coords all over, tp coords, start coords, and normal coords
        public boolean writeInfo()
        {
            boolean error = false;
            String  errorMessage = new String();
            byte left, right;
            
            //must update these values before checked otherwise, check if updating fails
            if( !this.parseCoords() )
            {
                errorMessage += "invalid coordinate entries.\n";
                error = true;
            }
            //now check actual values
            if( startX < 0 || startX > 0xffff || finishX < 0 || finishX > 0xffff )
            {
                errorMessage += "coordinate value out of bounds, must be positive and less than 0x10000.\n";
                error = true;
            }
            if( teleportOffset < 0 || teleportOffset > 0xe8 )
            {
                errorMessage += "teleport offset must be from 0 to e8.\n";
                error = true;
            }
            //warn the user if they are using a different teleport offset, unless they were using one to start
            else if( sceneNumber != 0 && 
                !( teleportOffset == 0x96 || teleportOffset == 0x97 ||
                   teleportOffset == bTeleportOffset ) )
            {
                int choice =    
                    JOptionPane.showConfirmDialog( 
                        null, 
                        "Warning! changing the value of the teleport offsets will overwrite the new" +
                        "teleport choices with the necessary values, would you like to proceed?", 
                        "Warning!", JOptionPane.YES_NO_OPTION );
                
                if( choice == JOptionPane.NO_OPTION )
                    return true; //pretend that it was successful so that there's no error message
                
            }
            
            if( !textBlock.updateTextPointer() )
            {
                errorMessage += "invalid pointer.\n";
                error = true;
                
            }
            
            switch( sceneNumber )
            {
                case 0:
                    if( !textBlock.updateCurrentArray( scene1Area.getText() ) )
                    {
                        errorMessage += "invalid text block entry for scene 1.\n";
                        error = true;
                    }
                    break;
                case 1:
                    if( !textBlock.updateCurrentArray( scene2Area.getText() ) )
                    {
                        errorMessage += "invalid text block entry for scene 2.\n";
                        error = true;
                    }
                    break;
                case 2:
                    if( !textBlock.updateCurrentArray( scene3Area.getText() ) )
                    {
                        errorMessage += "invalid text block entry for scene 3.\n";
                        error = true;
                    }
                    break;
            }
            
            if( ( textBlock.currentArraySize() > textBlock.originalSize() ) && preventOverwriteToggle )
            {
                errorMessage += "uncheck prevent overwrites or make text shorter.\n";
                error = true;
            }
            
            if( error )
            {
                JOptionPane.showMessageDialog( null, errorMessage, "error!", JOptionPane.ERROR_MESSAGE );
                return false;
            }
            
            textBlock.writeTextInfo();
            
            switch( sceneNumber )
            {
                case 0:
                    //SCENE 1
                    //X:
                    left = (byte) startX;
                    right = (byte) ( startX >> 8 );
                    //start
                    rom.write( START_X_OFFSET, left );
                    rom.write( START_X_OFFSET + 1, right );
                    //move pattern
                    rom.write( SCENE1_X_OFFSET, left );
                    rom.write( SCENE1_X_OFFSET + 1, right );
                    
                    //Y:
                    left = (byte) startY;
                    right = (byte) ( startY >> 8 );
                    //start
                    rom.write( START_Y_OFFSET, left );
                    rom.write( START_Y_OFFSET + 1, right );
                    //move pattern
                    rom.write( SCENE1_Y_OFFSET, left );
                    rom.write( SCENE1_Y_OFFSET + 1, right );
                    
                    //direction:
                    movement = (byte) directionScene1Combo.getSelectedIndex();
                    rom.write( SCENE1_MOVE_OFFSET, movement );
                    break;
                    
                case 1:
                    //SCENE 2:
                    //X:
                    left = (byte) startX;
                    right = (byte) ( startX >> 8 );
                    //move pattern:
                    rom.write( SCENE2_X_OFFSET, left );
                    rom.write( SCENE2_X_OFFSET + 1, right );
                    //teleport:
                    left = (byte) ( startX / 8 );
                    right = (byte) ( ( startX / 8 ) >> 8 );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8, left );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 1, right );
                    
                    //Y:
                    left = (byte) startY;
                    right = (byte) ( startY >> 8 );
                    //move pattern:
                    rom.write( SCENE2_Y_OFFSET, left );
                    rom.write( SCENE2_Y_OFFSET + 1, right );
                    
                    //teleport:
                    left = (byte) ( startY / 8 );
                    right = (byte) ( ( startY / 8 ) >> 8 );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 2, left );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 2 + 1, right );
                    
                    //teleport table value:
                    rom.write( SCENE2_TEL_OFFSET, teleportOffset );
                    
                    //direction
                    movement = (byte) directionScene2Combo.getSelectedIndex();
                    rom.write( SCENE2_MOVE_OFFSET, movement );
                    break;
                    
                case 2:
                    //SCENE 3:
                    //X1:
                    left = (byte) startX;
                    right = (byte) ( startX >> 8 );
                    //move pattern:
                    rom.write( SCENE3_X_OFFSET, left );
                    rom.write( SCENE3_X_OFFSET + 1, right );
                    //teleport:
                    left = (byte) ( startX / 8 );
                    right = (byte) ( ( startX / 8 ) >> 8 );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8, left );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 1, right );
                    
                    //Y1:
                    left = (byte) startY;
                    right = (byte) ( startY >> 8 );
                    //move pattern:
                    rom.write( SCENE3_Y_OFFSET, left );
                    rom.write( SCENE3_Y_OFFSET + 1, right );
                    //teleport:
                    left = (byte) ( startY / 8 );
                    right = (byte) ( ( startY / 8 ) >> 8 );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 2, left );
                    rom.write( TELE_OFFSET + teleportOffset * 0x8 + 2 + 1, right );
                    
                    //FINISH
                    //X2:
                    left = (byte) finishX;
                    right = (byte) ( finishX >> 8 );
                    rom.write( SCENE3_X2_OFFSET, left );
                    rom.write( SCENE3_X2_OFFSET + 1, right );
                    
                    //Y2:
                    left = (byte) finishY;
                    right = (byte) ( finishY >> 8 );
                    //move pattern:
                    rom.write( SCENE3_Y2_OFFSET, left );
                    rom.write( SCENE3_Y2_OFFSET + 1, right );
                    
                    //teleport table value:
                    rom.write( SCENE3_TEL_OFFSET, teleportOffset );
                    break;
            }
            
            System.out.println( "Succesfully saved!" );
            
            return true;
        }
        
        public void initGUI()
        {
            updateCoords();
            updateText( false );
            
            switch( sceneNumber )
            {
                case 0:
                    directionScene1Combo.setSelectedIndex( (int) movement );
                    break;
                    
                case 1:
                    directionScene2Combo.setSelectedIndex( (int) movement );
                    break;
             }
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
        
        //makes sure the values are proper hex or base 10 numbers, sets startx, y, finishx, y
        public boolean parseCoords()
        {
            switch( sceneNumber )
            {
                case 0:
                    try
                    {
                        switch( numberFormat )
                        {
                            case 0:
                                startX = Integer.parseInt( xScene1Field.getText() );
                                startY = Integer.parseInt( yScene1Field.getText() );
                                break;
                            case 1:
                                startX = Integer.parseInt( xScene1Field.getText(), 16 );
                                startY = Integer.parseInt( yScene1Field.getText(), 16 );
                                break;
                            case 2:
                                startX = Integer.parseInt( xScene1Field.getText(), 16 ) * 8;
                                startY = Integer.parseInt( yScene1Field.getText(), 16 ) * 8;
                                break;
                        }
                    }
                    catch( NumberFormatException e )
                    {
                        return false;
                    }
                    break;
                case 1:
                    try
                    {
                        switch( numberFormat )
                        {
                            case 0:
                                startX = Integer.parseInt( xScene2Field.getText() );
                                startY = Integer.parseInt( yScene2Field.getText() );
                                break;
                            case 1:
                                startX = Integer.parseInt( xScene2Field.getText(), 16 );
                                startY = Integer.parseInt( yScene2Field.getText(), 16 );
                                break;
                            case 2:
                                startX = Integer.parseInt( xScene2Field.getText(), 16 ) * 8;
                                startY = Integer.parseInt( yScene2Field.getText(), 16 ) * 8;
                                break;
                        }
                    }
                    catch( NumberFormatException e )
                    {
                        return false;
                    }
                    break;
                case 2:
                    try
                    {
                        switch( numberFormat )
                        {
                            case 0:
                                startX = Integer.parseInt( xScene3Field.getText() );
                                startY = Integer.parseInt( yScene3Field.getText() );
                                
                                finishX = Integer.parseInt( x2Scene3Field.getText() );
                                finishY = Integer.parseInt( y2Scene3Field.getText() );
                                break;
                            case 1:
                                startX = Integer.parseInt( xScene3Field.getText(), 16 );
                                startY = Integer.parseInt( yScene3Field.getText(), 16 );
                                
                                finishX = Integer.parseInt( x2Scene3Field.getText(), 16 );
                                finishY = Integer.parseInt( y2Scene3Field.getText(), 16 );
                                break;
                            case 2:
                                startX = Integer.parseInt( xScene3Field.getText(), 16 ) * 8;
                                startY = Integer.parseInt( yScene3Field.getText(), 16 ) * 8;
                                
                                finishX = Integer.parseInt( x2Scene3Field.getText(), 16 ) * 8;
                                finishY = Integer.parseInt( y2Scene3Field.getText(), 16 ) * 8;
                                break;
                        }
                    }
                    catch( NumberFormatException e )
                    {
                        return false;
                    }
                    break;
            }
            return true;
        }
        
        public int getTeleportOffset( )
        {
            return teleportOffset;
        }
        
        public boolean updateTeleportOffset( int offset )
        {
            if( sceneNumber == 0 )
                return false;
            
            teleportOffset = offset;
            
            return true;
        }
        
        public void updateCoords()
        {
            switch( sceneNumber )
            {
                case 0:
                    if(!numbersHexToggle)
                    {
                        xScene1Field.setText( Integer.toString( startX ) );
                        yScene1Field.setText( Integer.toString( startY ) );
                    }
                    else
                    {
                        if( numbers1ppuToggle )
                        {
                            xScene1Field.setText( Integer.toHexString( startX ) );
                            yScene1Field.setText( Integer.toHexString( startY ) );
                        }
                        else
                        {
                            xScene1Field.setText( Integer.toHexString( startX / 8 ) );
                            yScene1Field.setText( Integer.toHexString( startY / 8 ) );
                        }
                    }
                    break;
                    
                case 1:
                    if( !numbersHexToggle )
                    {
                        xScene2Field.setText( Integer.toString( startX ) );
                        yScene2Field.setText( Integer.toString( startY ) );
                    }
                    else
                    {
                        if( numbers1ppuToggle )
                        {
                            xScene2Field.setText( Integer.toHexString( startX ) );
                            yScene2Field.setText( Integer.toHexString( startY ) );
                        }
                        else
                        {
                            xScene2Field.setText( Integer.toHexString( startX / 8 ) );
                            yScene2Field.setText( Integer.toHexString( startY / 8 ) );
                        }
                    }
                    break;
                    
                case 2:
                    if(!numbersHexToggle)
                    {
                        xScene3Field.setText( Integer.toString( startX ) );
                        yScene3Field.setText( Integer.toString( startY ) );
                        
                        x2Scene3Field.setText( Integer.toString( finishX ) );
                        y2Scene3Field.setText( Integer.toString( finishY ) );
                    }
                    else
                    {
                        if( numbers1ppuToggle )
                        {
                            xScene3Field.setText( Integer.toHexString( startX ) );
                            yScene3Field.setText( Integer.toHexString( startY ) );
                            
                            x2Scene3Field.setText( Integer.toHexString( finishX ) );
                            y2Scene3Field.setText( Integer.toHexString( finishY ) );
                        }
                        else
                        {
                            xScene3Field.setText( Integer.toHexString( startX / 8 ) );
                            yScene3Field.setText( Integer.toHexString( startY / 8 ) );
                            
                            x2Scene3Field.setText( Integer.toHexString( finishX / 8 ) );
                            y2Scene3Field.setText( Integer.toHexString( finishY / 8 ) );
                        }
                    }
                    break;
            }
            
        }
        
        //either used to restore original text or update the offset and present that (false and true respectively)
        public boolean updateText( boolean update )
        {
            if( update)
            {
                switch( sceneNumber )
                {
                    case 0:
                        try
                        {
                            int address = Integer.parseInt(textPointer1Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            JOptionPane.showMessageDialog( 
                                null, "invalid entry for address 1", "error", JOptionPane.ERROR_MESSAGE );
                            return false;
                        }
                        
                        break;
                    case 1:
                        try
                        {
                            int address = Integer.parseInt(textPointer2Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            JOptionPane.showMessageDialog( 
                                null, "invalid entry for address 1", "error", JOptionPane.ERROR_MESSAGE );
                            return false;
                        }
                        break;
                    case 2:
                        try
                        {
                            int address = Integer.parseInt(textPointer3Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            JOptionPane.showMessageDialog( 
                                null, "invalid entry for address 1", "error", JOptionPane.ERROR_MESSAGE );
                            return false;
                        }
                        break;
                }
            }
            
            textBlock.updateGUI();
            
            return true;
        }
        
        private class Text
        {
            //pointer to text block, for use if user moves the text block
            private int     textPointer;
            
            //original text block...do not modify until write is called...
            private byte [] originalTextBlock;
            //current...update as typing
            private byte [] currentTextBlock;
        
            public void updateGUI()
            {
                switch( sceneNumber )
                {
                    case 0:
                        textPointer1Field.setText( Integer.toHexString(textPointer) );
                        scene1Area.setText( toFormattedText( currentTextBlock ) );
                        originalSizeScene1Field.setText( new Integer(originalTextBlock.length).toString() );
                        currentSizeScene1Field.setText( new Integer(originalTextBlock.length).toString() );
                        break;
                        
                    case 1:
                        textPointer2Field.setText( Integer.toHexString(textPointer) );
                        scene2Area.setText( toFormattedText( currentTextBlock ) );
                        originalSizeScene2Field.setText( new Integer(originalTextBlock.length).toString() );
                        currentSizeScene2Field.setText( new Integer(originalTextBlock.length).toString() );
                        break;
                        
                    case 2:
                        textPointer3Field.setText( Integer.toHexString(textPointer) );
                        scene3Area.setText( toFormattedText( currentTextBlock ) );
                        originalSizeScene3Field.setText( new Integer(originalTextBlock.length).toString() );
                        currentSizeScene3Field.setText( new Integer(originalTextBlock.length).toString() );
                        break;
                }
            }
            
            //depends on toggle textHexToggle...
            //if true, pass an array of bytes, returns a string in format "[01 10 20 00]"
            //if false, pass an array of bytes, returns a string with mix of hex (control codes)
            //and normal chars (converted from byte to eb char) ie. "[10]hello[00]"
            //*note* it seperates all control code sets with brackets so that it's easier to tell which are which
            //the parser doesn't care if they're seperated like that however, just as long as they're in brackets
            private String toFormattedText( byte [] array )
            {
                //char [] returnValue;
                String tempArray = new String();
                int size = 0, position = 0;
                boolean inHex = false; //used to determine when to use brackets
                
                //control codes
                //[01 XX]: Move the text over a distance noted by XX.
                //[02 XX]: Move the text down a distance noted by XX.
                //[08 XX]: Print a main character name. Values are 01 - 04.
                //[09]: Drop down one line.
                //[00]: from what i can tell, this ends the string, similar to [02] for standard cc's
                if( !textHexToggle )
                {
                    for( position = 0; position < array.length; position++ )
                    {
                        //double array size if possibly needed (checks for at least 4 chars available)
                        //if( size >= tempArray.length - 5 )
                        //    tempArray = doubleString( tempArray );
                        
                        byte current = array[ position ]; //current byte
                        int cur = current & 0xFF;
                        
                        //System.out.println( "position " + position + " = " + cur );
                        
                        if( !inHex )
                        {
                            if( cur == 0x01 || cur == 0x02 || cur == 0x08 )
                            {
                                inHex = true;
                                tempArray += '[';
                                tempArray += '0';
                                tempArray += cur; //no "hex" values only 1 to 9
                            }
                            else if( cur == 0x09 || cur == 0x00 )
                            {
                                tempArray += '[';
                                tempArray += '0';
                                tempArray += cur;
                                tempArray += ']';
                            }
                            else
                            {
                                tempArray += simpToRegChar( (char) cur );                                
                            }
                        }
                        else
                        {
                            tempArray += ' ';
                            
                            //make sure that small values still have two chars per byte
                            if( cur < 0x10 )
                            {
                                tempArray += '0';
                                //tempArray += (int) cur;
                            }
                            
                            //take the byte, convert to a hex value, then convert to a char, take the first value from array
                            tempArray += Integer.toHexString( cur );
                            //tempArray += new Integer( (int) cur).toHexString( cur );
                            
                            
                            //figure out whether to put a bracket or not...always put one if it's the last position in array
                            if( position + 1 < array.length )
                            {
                                byte next = array[ position + 1 ];
                                byte prev = array[ position - 1 ];
                                
                                if( ( prev == 0x01 || prev == 0x02 || prev == 0x08 ) || 
                                    ( cur != 0x01 && cur != 0x02 && cur != 0x08 ) ||
                                    ( next != 0x00 && next != 0x01 && next != 0x02 && next != 0x02 && next != 0x09) )
                                {
                                    inHex = false;
                                    tempArray += ']';
                                }
                                
                            }
                            else
                            {
                                inHex = false;
                                tempArray += ']';
                            }
                        }
                    }   
                }
                
                //if hex is toggled
                else
                {
                    tempArray += '[';
                    
                    for( position = 0; position < array.length; position++ )
                    {
                        //double array size if possibly needed (checks for at least 4 chars available)
                        //if( size >= tempArray.length - 4 )
                        //    tempArray = doubleString( tempArray );
                        
                        if( position != 0 )
                            tempArray += ' ';
                        
                        byte current = array[ position ]; //current byte
                        int cur = current & 0xff;
                        
                        if( cur < 0x10 )
                        {
                            tempArray += '0';
                            tempArray += Integer.toHexString( cur );
                        }
                        else
                        {
                            tempArray += Integer.toHexString( cur );
                            //tempArray += new Integer( (int) cur ).toHexString( cur );
                        }
                    }
                    
                    tempArray += ']';
                }
                
                /*returnValue = new char [size];
                
                for ( int i = 0; i < size; i++ )
                    returnValue[i] = tempArray[i];
                    */
                return tempArray;
                
            }
            
            //gets size from currentSize, then assumes that the string is formatted correctly for it's algorithm
            public boolean updateCurrentArray( String text )
            {
                int     currentValue = 0,
                        index = 0,
                        arrayIndex = 0;
                char    current = 0;
                        //previous = 0;
                        
        	boolean inHex = false;
		boolean twoChars = false;
                
                try
                {
                    currentTextBlock = new byte[ currentSize( text ) ];
                }
                catch( UndefinedSizeException e )
                {
                    return false;
                }
                catch( InvalidNumberFormatException e )
                {
                    return false;
                }
                
                while( index < text.length() )
        	{
                    current = text.charAt( index );
			
                    //go into a loop that parses until a hex string ends (everything between [ and ]...
                    //throws exception if ] never found
                    //throws exception if invalid hex values found
                    //does not do anything for empty brackets "[     ]" or "[][][][][][]"
                    //adds 5 to return value for [01 2 03 04 5    ]
                    //throws exception for internal sets of brackets ie. [0 0 [0 0] ]
                    if( inHex )
                    {
                        if(index < text.length() )
                        {
                            //current = text.charAt( index );
                            //checks to see if hex string ends...
                            if( current == ']' )
                            {
                                inHex = false;
                                index++;
                            }
                            //check for space
                            else if( current == ' ' )
                            {
                                index++;
                            }
                            //else if valid hex value
                            //must check if it's one or two char long, if it's longer, throw an exception
                            else if( (current >= '0' &&  current <= '9' ) || ( current >= 'a' && current <= 'f' ) )
                            {
                                index++;
				
                                current = text.charAt(index);
                                //if there's a second char and it's valid
                                if( (current >= '0' &&  current <= '9' ) || ( current >= 'a' && current <= 'f' ) )
                                {
                                    twoChars = true;
                                                                        
                                    index++;
                                        
                                    current = text.charAt(index);
                                            
                                    //make sure that it's only two chars long for each hex value
                                    if( current == ' ' )
                                    {
                                        index++;
                                    }
                                    else
                                    {
                                        index++;
                                        inHex = false;
                                    }
                                    
                                }
                                //store the currentValue if it made it this far
                                if( twoChars )
                                {
                                    currentValue = 
                                        Integer.parseInt( text.substring( index - 3, index - 1 ), 16 );
                                }
                                else
                                {
                                    currentValue = 
                                        Integer.parseInt( text.substring( index - 1, index ), 16 );
                                }
                                
                                currentTextBlock[ arrayIndex++ ] = (byte) currentValue;
                                twoChars = false;
                            }
                            //else if invalid hex value
                        }
                    }
            
                    //if it's not hex, check to see for new hex
                    //also check to make sure if it's formatted properly
                    //if it's just standard text, add them to the current size
                    else if ( current == '[' )
                    {
                        inHex = true;
                        index++;
                    }
                    else
                    {
                        index++;
                        //currentValue = new String(int;
                        currentTextBlock[ arrayIndex++ ] = (byte) simpToGameChar( (char) current );
                    }
                }
                
                return true;
            }
            
            //this reads either an address from the rom then reads from that address, or reads directly
            //from the specified address
            public Text( int address, boolean actualAddress )
            {
                if( !actualAddress )
                    textPointer = readReverseHex( address, 3 ) - 0xBFFE00; //convert to standard pointer
                else
                    textPointer = address;
                
                byte [] tempArray = new byte[50];
                int currentOffset = 0;
                byte currentByte;
                
                //read textBlock till array deliminated by 0
                do
                {
                    if( currentOffset >= tempArray.length )
                        tempArray = doubleArray(tempArray);
                    
                    currentByte = rom.readByte( textPointer + currentOffset );
                    tempArray[ currentOffset++] = currentByte;
                    
                    //check to make sure it's not a 0x0 for an attribute for 0x01 or 0x02 before ending the loop
                    if( currentByte == 0x0 )
                    {
                        if( currentOffset > 1 )
                        {
                            if( tempArray[ currentOffset - 2 ] == 0x01 || tempArray[ currentOffset - 2 ] == 0x02 )
                            {
                                if( currentOffset >= tempArray.length )
                                    tempArray = doubleArray(tempArray);
                    
                                currentByte = rom.readByte( textPointer + currentOffset );
                                tempArray[ currentOffset++ ] = currentByte;
                            }
                        }
                    }
                    
                } while ( currentByte != 0x0 );
                
                //copy temp to original and current
                originalTextBlock = new byte[ currentOffset ];
                currentTextBlock = new byte[ currentOffset ];
                
                for( int i = 0; i < currentOffset; i++)
                {
                    originalTextBlock [i] = tempArray[i];
                    currentTextBlock [i] = tempArray[i];
                }
                
            }
            
            private char[] doubleString( char [] array )
            {
                char[] returnValue = new char[ array.length * 2 ];
                
                for( int i = 0; i < array.length; i++ )
                {
                    returnValue[i] = array[i];
                }
                
                return returnValue;
            }
            
            //updates the pointer, catches invalid and out of bounds errors
            protected boolean updateTextPointer()
            {
                switch( sceneNumber )
                {
                    case 0:
                        try
                        {
                            int address = Integer.parseInt(textPointer1Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            return false;
                        }
                        break;
                        
                    case 1:
                        try
                        {
                            int address = Integer.parseInt(textPointer2Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            return false;
                        }
                        break;
                        
                    case 2:
                        try
                        {
                            int address = Integer.parseInt(textPointer3Field.getText(), 16);
                            
                            if( rom.length() == 0x400200 )
                            {
                                //this is just making sure that it won't have an error reading the rom
                                //these values in the bounds should never be used for a pointer =p
                                if( address < AbstractRom.EB_ROM_SIZE_EXPANDED - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                            else
                            {
                                if( address < AbstractRom.EB_ROM_SIZE_REGULAR - 1 && address >= 0 )
                                {
                                    textBlock = new Text( address, true );
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(
                                        null, "pointer out of bounds", "error", JOptionPane.ERROR_MESSAGE );
                                    return false;
                                }
                            }
                        }
                        catch( NumberFormatException e )
                        {
                            return false;
                        }
                        break;
                }
                return true;
            }
            
            protected void writeTextInfo()
            {
                int pointer = toSnesPointer( textPointer );
                byte first, second, third;
                first = (byte) pointer;
                second = (byte) ( pointer >> 8 );
                third = (byte) ( pointer >> 16 );
                
                //System.out.println( 
                //    "textPointer: first byte = " + (int)( first& 0xff ) + " second = " + (int)( second & 0xff ) + " third = " + (int)( third & 0xff ) );
                
                //write the pointer
                switch( sceneNumber)
                {
                    case 0:
                        //System.out.println( "Writing, in snes format, text pointer 1 (" + textPointer + ") to offset " + SCENE1_TEXT_OFFSET );
                        rom.write( SCENE1_TEXT_OFFSET, first );
                        rom.write( SCENE1_TEXT_OFFSET + 1, second );
                        rom.write( SCENE1_TEXT_OFFSET + 2, third );
                        break;
                    case 1:
                        //System.out.println( "Writing, in snes format, text pointer 2 (" + textPointer + ") to offset " + SCENE2_TEXT_OFFSET );
                        rom.write( SCENE2_TEXT_OFFSET, first );
                        rom.write( SCENE2_TEXT_OFFSET + 1, second );
                        rom.write( SCENE2_TEXT_OFFSET + 2, third );
                        break;
                    case 2:
                        //System.out.println( "Writing, in snes format, text pointer (" + textPointer + ") to offset " + SCENE3_TEXT_OFFSET );
                        rom.write( SCENE3_TEXT_OFFSET, first );
                        rom.write( SCENE3_TEXT_OFFSET + 1, second );
                        rom.write( SCENE3_TEXT_OFFSET + 2, third );
                        break;
                }
                
                //write the block
                //System.out.println( "text Block: size = " + currentTextBlock.length + " data = " + currentTextBlock );
                //System.out.println( "Writing text block " + sceneNumber + " to offset " + textPointer );
                rom.write( textPointer, currentTextBlock );
            }
            
            public int originalSize()
            {
                return originalTextBlock.length;
            }
            
            public int currentArraySize()
            {
                return currentTextBlock.length;
            }
            
            //parses text length
            //throws an exception if unable to parse...should only be unable to parse if there's invalid hex codes..
            //or unfinished ones...ie. "[01 04 02 "
            
            
            //doubles array size if needed
            private byte[] doubleArray( byte [] array )
            {
                byte[] returnValue = new byte[ array.length * 2 ];
                
                for( int i = 0; i < array.length; i++ )
                {
                    returnValue[i] = array[i];
                }
                
                return returnValue;
            }
            /*TODO:
             *add parser function that parses a text field on events
             *add parser function for raw hex as well
             *also add something to output both from the array at beginning
             */
        }
        
        private int readReverseHex( int offset, int length )
        {
            int returnValue = 0;
            if( length > 0 && length < 4 )
            {
                for( int i = 0; i < length; i++ )
                {
                    returnValue += rom.read( offset + i ) << ( i * 8 );
                }
            }
            return returnValue;
        }
    }
    
    //parses string text and returns the current size
    //brackets do not count against the size, nor do spaces in brackets, as long as the brackets are not nested
    //ie. [][][][][aa 01 ]dajdkdjfad[][00] is a valid string (size 13)
    public int currentSize( String text )
        throws UndefinedSizeException, InvalidNumberFormatException
    {
        int     returnValue = 0,
                index = 0;
		
	char    current = 0;
		//previous = 0;
		
	boolean inHex = false;
		
	while( index < text.length() )
	{
            current = text.charAt( index );
			
            //go into a loop that parses until a hex string ends (everything between [ and ]...
            //throws exception if ] never found
            //throws exception if invalid hex values found
            //does not do anything for empty brackets "[     ]" or "[][][][][][]"
            //adds 5 to return value for [01 2 03 04 5    ]
            //throws exception for internal sets of brackets ie. [0 0 [0 0] ]
            if( inHex )
            {
                if(index < text.length() )
                {
                    //current = text.charAt( index );
                    //checks to see if hex string ends...
                    if( current == ']' )
                    {
                        inHex = false;
                        index++;
                    }
                    //check for internal bracket
                    else if( current == '[' )
                    {
                        throw new UndefinedSizeException();	
                    }
                    //check for space
                    else if( current == ' ' )
                    {
                        index++;
                    }
                    //else if valid hex value
                    //must check if it's one or two char long, if it's longer, throw an exception
                    else if( (current >= '0' &&  current <= '9' ) || ( current >= 'a' && current <= 'f' ) )
                    {
                        index++;
					
                        if( index < text.length() )
                        {
                            current = text.charAt(index);
                            //if there's a second char and it's valid
                            if( (current >= '0' &&  current <= '9' ) || ( current >= 'a' && current <= 'f' ) )
                            {
                                index++;
                                if( index < text.length() )
                                {
                                    current = text.charAt(index);
                                    //System.out.println( "current = \"" + current + "\"" );
                                    //make sure that it's only two chars long for each hex value
                                    if( current == ' ' )
                                    {
                                        index++;
                                        returnValue++;
                                    }
                                    else if( current == ']' )
                                    {
                                        index++;
                                        returnValue++;
                                        inHex = false;
                                    }
                                    //if more than two chars long
                                    else
                                    {
                                        throw new InvalidNumberFormatException();
                                    }
                                }
                                else
                                {
                                    throw new UndefinedSizeException();
                                }
                            }
                            //if there's a space after first char and it's valid
                            else if( current == ' ' )
                            {
                                index++;
                                returnValue++;
                            }
                            //if there's a closing bracket after first char and it's valid
                            else if( current == ']' )
                            {
                                index++;
                                returnValue++;
                                inHex = false;
                            }
                            else
                            {
                                throw new InvalidNumberFormatException();
                            }
                        }					
                        else
                        {
                            throw new UndefinedSizeException();
                        }
                    
                    }
                    //else if invalid hex value
                    else
                    {
                        throw new InvalidNumberFormatException();
                    }
                }
                else
                    throw new UndefinedSizeException();
            }
            
            
            //if it's not hex, check to see for new hex
            //also check to make sure if it's formatted properly
            //if it's just standard text, add them to the current size
            else if ( current == '[' )
	    {
                inHex = true;
                index++;
            }
            else if ( current == ']' )
            {
                throw new UndefinedSizeException();
            }
            else
            {
            	index++;
		returnValue++;
            }
			
            //previous = current;	
	}
	
        if( inHex )
            throw new UndefinedSizeException();
            
	return returnValue;
    }
                
    class InvalidNumberFormatException extends Exception
    {
        public InvalidNumberFormatException()
        {
            super("invalid number format, hex values range from 0-9 and a-f only");
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
	
        scene1 = new Flyover(0);
        scene2 = new Flyover(1);
        scene3 = new Flyover(2);
        
        scene1.initGUI();
        scene2.initGUI();
        scene3.initGUI();
        
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
        return "0.0.0.1";
    }
    
    public void hide() 
    {
        mainWindow.setVisible(false);
    }
    
    public void actionPerformed( ActionEvent ae )
    {
        if( ae.getActionCommand().equals( "close" ) )
            hide();
        
        if( ae.getActionCommand().equals( "apply" ) )
        {
            if( !scene1.writeInfo() )
                JOptionPane.showMessageDialog( null, "Error saving scene 1", "error", JOptionPane.ERROR_MESSAGE );
            if( !scene2.writeInfo() )
                JOptionPane.showMessageDialog( null, "Error saving scene 2", "error", JOptionPane.ERROR_MESSAGE );
            if( !scene3.writeInfo() )
                JOptionPane.showMessageDialog( null, "Error saving scene 3", "error", JOptionPane.ERROR_MESSAGE );
            
        }
        
        else if( ae.getActionCommand().equals( "Restore Values" ) )
        {
            System.out.println( "restoring from backups" );
            
            scene1.restore();
            scene2.restore();
            scene3.restore();
            
        }
        
        else if( ae.getSource() == teleportMenuItem )
        {
            TPDialog dialog = new TPDialog( mainWindow, true );
            dialog.setVisible(true);
        }
        
        else if( ae.getSource() == blueFlyoverTutorialMenuItem )
        {
            try
            {
                JEditorPane pane = new JEditorPane( new URL( "http://pkhack.starmen.net/down/tuts/flyover_edit.html" ) );
                pane.setEditable(false);
                JScrollPane helpPane = new JScrollPane( pane );
                helpPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                //helpPane.setPreferredSize(new Dimension(480, 480));
                //helpPane.setMinimumSize(new Dimension(320, 320));
                
                JFrame helpWindow = new JFrame();
                helpWindow.getContentPane().add( helpPane );
                helpWindow.setSize( new Dimension( 540, 540 ) );
                helpWindow.setTitle( "Blue Antoid's Flyover Tutorial" );
                
                helpWindow.setVisible(true);
                //JOptionPane.showMessageDialog( null, pane.getDocument(), "Blue Antoid's Flyover Tutorial", JOptionPane.OK_OPTION );
            }
            catch( MalformedURLException e ) //do nothing because this shouldn't happen
            { }
            catch( IOException e )
            {
                JOptionPane.showMessageDialog( null, "Sorry, it appears blue antoid's guide is down right now", "error", JOptionPane.ERROR_MESSAGE );
            }
            
        }
        
        else if( ae.getSource() == aboutFlyoverMenuItem )
        {
            try
            {
                JEditorPane pane = new JEditorPane( new URL ( "http://www.geocities.com/xeminemxgurlx/Flyover_Editor.html" ) );
                pane.setEditable(false);
                JScrollPane helpPane = new JScrollPane( pane );
                helpPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                //helpPane.setPreferredSize(new Dimension(480, 480));
                //helpPane.setMinimumSize(new Dimension(320, 320));
                
                JFrame helpWindow = new JFrame();
                helpWindow.getContentPane().add( helpPane );
                helpWindow.setSize( new Dimension( 480, 480 ) );
                helpWindow.setTitle( "My shorter more confusing guide" );
                
                helpWindow.setVisible(true);
                //JOptionPane.showMessageDialog( null, pane.getDocument(), "Blue Antoid's Flyover Tutorial", JOptionPane.OK_OPTION );
            }
            catch( MalformedURLException e ) //do nothing because this shouldn't happen
            { }
            catch( IOException e )
            {
                JOptionPane.showMessageDialog( null, "this guide is temporarily down...sorry =(", "error", JOptionPane.ERROR_MESSAGE );
            }
        }
        
        //POINTER UPDATES
        else if( ae.getSource() == textPointer1UpdateButton )
            scene1.updateText( true );
        
        else if( ae.getSource() == textPointer2UpdateButton )
            scene2.updateText( true );
        
        else if( ae.getSource() == textPointer3UpdateButton )
            scene3.updateText( true );
        
        //NUMBER FORMAT
        else if( ae.getActionCommand().equals( "Standard Number Format" ) )
        {
            System.out.println("standard");
            
            switch (numberFormat)
            {
                case 0:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                    }
                    break;
                case 1:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;
                        
                        number1ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;
                        
                        numberFormat = 0;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
            
                    break;
                case 2:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;
                        
                        number8ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;
                        
                        numberFormat = 0;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
                    
                    break;
            }
            
        }
        
        else if( ae.getActionCommand().equals( "1ppu Number Format" ) )
        {
            System.out.println("1ppu");
            switch (numberFormat)
            {
                case 0:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;
                        
                        numberStandardRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;
                        
                        numberFormat = 1;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
                    break;
                case 1:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                    }
                    break;
                case 2:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;
                        
                        number8ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;
                        
                        numberFormat = 1;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
                    break;
            }
        }
        
        else if( ae.getActionCommand().equals( "8ppu Number Format" ) )
        {
            System.out.println("8ppu");
            switch (numberFormat)
            {
                case 0:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = false;
                        numbers1ppuToggle = true;
                        
                        numberStandardRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;
                        
                        numberFormat = 2;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
                    break;
                case 1:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                        
                        numbersHexToggle = true;
                        numbers1ppuToggle = true;
                        
                        number1ppuRadio.setSelected(true);
                    }
                    else
                    {
                        numbersHexToggle = true;
                        numbers1ppuToggle = false;
                        
                        numberFormat = 2;
                        
                        scene1.updateCoords();
                        scene2.updateCoords();
                        scene3.updateCoords();
                    }
                    break;
                case 2:
                    if( !scene1.parseCoords() || !scene2.parseCoords() || !scene3.parseCoords() )
                    {
                        JOptionPane.showMessageDialog( 
                            null, "error parsing coordinate field, please use a valid number", 
                            "error", JOptionPane.ERROR_MESSAGE );
                    }
                    break;
            }
        }
    }
    
    public void itemStateChanged( ItemEvent ie ) 
    {
        if( ie.getSource() == hexTextCheckBox )
        {
            if( ie.getStateChange() == ItemEvent.SELECTED )
            {
                textHexToggle = true;
            }
            else
            {
                textHexToggle = false;
            }
            
            if( !scene1.textBlock.updateCurrentArray( scene1Area.getText() ) ||
                !scene2.textBlock.updateCurrentArray( scene2Area.getText() ) ||
                !scene3.textBlock.updateCurrentArray( scene3Area.getText() ) )
            {
                JOptionPane.showMessageDialog( 
                    null, "error converting the string", "error", JOptionPane.ERROR_MESSAGE );
                
                if( hexTextCheckBox.getState() )
                {
                    textHexToggle = false;
                    hexTextCheckBox.setState( false );
                }
                else
                {
                    textHexToggle = false;
                    hexTextCheckBox.setState( false );
                }
            }
            else
            {
                scene1.updateText( false );
                scene2.updateText( false );
                scene3.updateText( false );
                
                try
                {
                    currentSizeScene1Field.setText(
                        new Integer( currentSize( scene1Area.getText() ) ).toString() );
                    currentSizeScene2Field.setText(
                        new Integer( currentSize( scene2Area.getText() ) ).toString() );
                    currentSizeScene3Field.setText(
                        new Integer( currentSize( scene3Area.getText() ) ).toString() );
                }
                catch( InvalidNumberFormatException e )
                {
                    
                }
                catch( UndefinedSizeException e )
                {
                    
                }
            }
            
        }
        else if( ie.getSource() == preventOverwritesCheckBox )
        {
            if( ie.getStateChange() == ItemEvent.DESELECTED )
                preventOverwriteToggle = false;
            else
                preventOverwriteToggle = true;
        }
    }
    
    public void changedUpdate( DocumentEvent de ) 
    {
        if( de.getDocument() == scene1Area.getDocument() )
        {
            try
            {
                currentSizeScene1Field.setText(
                    new Integer( currentSize( scene1Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            
        }
        else if( de.getDocument() == scene2Area.getDocument() )
        {
            try
            {
                currentSizeScene2Field.setText(
                    new Integer( currentSize( scene2Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene2Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene2Field.setText(" ");
            }
        }
        else if( de.getDocument() == scene3Area.getDocument() )
        {
            try
            {
                currentSizeScene3Field.setText(
                    new Integer( currentSize( scene3Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene3Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene3Field.setText(" ");
            }
        }
    }    
    
    public void insertUpdate( DocumentEvent de ) 
    {
        if( de.getDocument() == scene1Area.getDocument() )
        {
            try
            {
                currentSizeScene1Field.setText(
                    new Integer( currentSize( scene1Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            
        }
        else if( de.getDocument() == scene2Area.getDocument() )
        {
            try
            {
                currentSizeScene2Field.setText(
                    new Integer( currentSize( scene2Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene2Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene2Field.setText(" ");
            }
        }
        else if( de.getDocument() == scene3Area.getDocument() )
        {
            try
            {
                currentSizeScene3Field.setText(
                    new Integer( currentSize( scene3Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene3Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene3Field.setText(" ");
            }
        }
    }    
    
    public void removeUpdate( DocumentEvent de ) 
    {
        if( de.getDocument() == scene1Area.getDocument() )
        {
            try
            {
                currentSizeScene1Field.setText(
                    new Integer( currentSize( scene1Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene1Field.setText(" ");
            }
            
        }
        else if( de.getDocument() == scene2Area.getDocument() )
        {
            try
            {
                currentSizeScene2Field.setText(
                    new Integer( currentSize( scene2Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene2Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene2Field.setText(" ");
            }
        }
        else if( de.getDocument() == scene3Area.getDocument() )
        {
            try
            {
                currentSizeScene3Field.setText(
                    new Integer( currentSize( scene3Area.getText() ) ).toString() );
            }
            catch( InvalidNumberFormatException e )
            {
                currentSizeScene3Field.setText(" ");
            }
            catch( UndefinedSizeException e )
            {
                currentSizeScene3Field.setText(" ");
            }
        }
    }
    
    //keep this at the end cuz it's so long and pretty much uneditable (grid bag layouts)...and ugly
    protected void init()
    {
        GridBagConstraints gridBagConstraints;

        mainWindow = createBaseWindow(this);
	mainWindow.setTitle(this.getDescription());
	//mainWindow.setIconImage(((ImageIcon) this.getIcon()).getImage());
	mainWindow.setSize(210, 450);
	mainWindow.setResizable(false);
        
                
        mainPanel = new JPanel();
        
        numberFormatButtonGroup = new ButtonGroup();
        flyoverPanel = new JTabbedPane();
        scene1Panel = new JPanel();
        startLabel1 = new JLabel();
        xScene1Label = new JLabel();
        yScene1Label = new JLabel();
        directionScene1Combo = new JComboBox( directions );
        directionScene1Label = new JLabel();
        xScene1Field = new JTextField();
        yScene1Field = new JTextField();
        textPointer1Label = new JLabel();
        textPointer1Field = new JTextField();
        textPointer1UpdateButton = new JButton();
        originalSizeLabel1 = new JLabel();
        currentSizeLabel1 = new JLabel();
        originalSizeScene1Field = new JTextField();
        currentSizeScene1Field = new JTextField();
        scene1Area = new JTextArea();
        scene2Panel = new JPanel();
        startLabel2 = new JLabel();
        xScene2Label = new JLabel();
        yScene2Label = new JLabel();
        directionScene2Combo = new JComboBox( directions );
        directionScene2Label = new JLabel();
        xScene2Field = new JTextField();
        yScene2Field = new JTextField();
        textPointer2Label = new JLabel();
        textPointer2Field = new JTextField();
        textPointer2UpdateButton = new JButton();
        originalSizeLabel2 = new JLabel();
        currentSizeLabel2 = new JLabel();
        originalSizeScene2Field = new JTextField();
        currentSizeScene2Field = new JTextField();
        scene2Area = new JTextArea();
        scene3Panel = new JPanel();
        startAndEndLabel = new JLabel();
        xScene3Label = new JLabel();
        yScene3Label = new JLabel();
        xScene3Field = new JTextField();
        yScene3Field = new JTextField();
        textPointer3Label = new JLabel();
        textPointer3Field = new JTextField();
        textPointer3UpdateButton = new JButton();
        originalSizeLabel3 = new JLabel();
        currentSizeLabel3 = new JLabel();
        originalSizeScene3Field = new JTextField();
        currentSizeScene3Field = new JTextField();
        scene3Area = new JTextArea();
        y2Scene3Field = new JTextField();
        y2Scene3Label = new JLabel();
        x2Scene3Field = new JTextField();
        x2Scene3Label = new JLabel();
        flyoverMenu = new JMenuBar();
        optionsMenu = new JMenu();
        preventOverwritesCheckBox = new JCheckBoxMenuItem();
        hexTextCheckBox = new JCheckBoxMenuItem();
        jSeparator1 = new JSeparator();
        numberStandardRadio = new JRadioButtonMenuItem();
        number8ppuRadio = new JRadioButtonMenuItem();
        number1ppuRadio = new JRadioButtonMenuItem();
        jSeparator2 = new JSeparator();
        teleportMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        blueFlyoverTutorialMenuItem = new JMenuItem();
        aboutFlyoverMenuItem = new JMenuItem();
        jSeparator = new JSeparator();
        restoreValuesMenuItem = new JMenuItem();
        scene1BPanel = new JPanel();
        scene2BPanel = new JPanel();
        scene3BPanel = new JPanel();
        
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
        scene1Panel.add(directionScene1Combo, gridBagConstraints);

        directionScene1Label.setText("Direction:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(14, 20, 4, 0);
        scene1Panel.add(directionScene1Label, gridBagConstraints);

        xScene1Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 40, 30, 40);
        scene1Panel.add(xScene1Field, gridBagConstraints);

        yScene1Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 30, 0);
        scene1Panel.add(yScene1Field, gridBagConstraints);

        textPointer1Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 4, 38);
        scene1Panel.add(textPointer1Label, gridBagConstraints);

        textPointer1Field.setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene1Panel.add(textPointer1Field, gridBagConstraints);
        
        textPointer1UpdateButton.setText("Update Text from Pointer");
        //mainWindow.getContentPane().add( textPointer1UpdateButton , BorderLayout.SOUTH );
        textPointer2UpdateButton.setText("Update Text from Pointer");
        textPointer3UpdateButton.setText("Update Text from Pointer");
        /*gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 4, 38);
        scene1Panel.add(textPointer1Label, gridBagConstraints);*/

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

        originalSizeScene1Field.setEditable(false);
        originalSizeScene1Field.setText("0");
        originalSizeScene1Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene1Panel.add(originalSizeScene1Field, gridBagConstraints);

        currentSizeScene1Field.setEditable(false);
        currentSizeScene1Field.setText("0");
        currentSizeScene1Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 26, 0, 0);
        scene1Panel.add(currentSizeScene1Field, gridBagConstraints);

        scene1Area.setColumns(20);
        scene1Area.setLineWrap(true);
        scene1Area.setWrapStyleWord(true);
        scene1Area.setTabSize(0);
        scene1Area.setBorder(new LineBorder(new Color(0, 0, 0)));
        scene1Area.setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 0);
        scene1Panel.add(scene1Area, gridBagConstraints);

        scene1BPanel.setLayout( new BorderLayout() );
        scene1BPanel.add( scene1Panel, BorderLayout.CENTER );
        scene1BPanel.add( textPointer1UpdateButton, BorderLayout.SOUTH );
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
        scene2Panel.add(directionScene2Combo, gridBagConstraints);

        directionScene2Label.setText("Direction:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(14, 20, 4, 0);
        scene2Panel.add(directionScene2Label, gridBagConstraints);

        xScene2Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 40, 30, 40);
        scene2Panel.add(xScene2Field, gridBagConstraints);

        yScene2Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 30, 0);
        scene2Panel.add(yScene2Field, gridBagConstraints);

        textPointer2Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(10, 20, 4, 38);
        scene2Panel.add(textPointer2Label, gridBagConstraints);

        textPointer2Field.setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene2Panel.add(textPointer2Field, gridBagConstraints);

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

        originalSizeScene2Field.setEditable(false);
        originalSizeScene2Field.setText("0");
        originalSizeScene2Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 26, 0, 0);
        scene2Panel.add(originalSizeScene2Field, gridBagConstraints);

        currentSizeScene2Field.setEditable(false);
        currentSizeScene2Field.setText("0");
        currentSizeScene2Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 26, 0, 0);
        scene2Panel.add(currentSizeScene2Field, gridBagConstraints);

        scene2Area.setColumns(20);
        scene2Area.setLineWrap(true);
		scene2Area.setWrapStyleWord(true);
        scene2Area.setTabSize(0);
        scene2Area.setBorder(new LineBorder(new Color(0, 0, 0)));
        scene2Area.setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 0);
        scene2Panel.add(scene2Area, gridBagConstraints);

        scene2BPanel.setLayout( new BorderLayout() );
        scene2BPanel.add( scene2Panel, BorderLayout.CENTER );
        scene2BPanel.add( textPointer2UpdateButton, BorderLayout.SOUTH );
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

        xScene3Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 10, 26, 0);
        scene3Panel.add(xScene3Field, gridBagConstraints);

        yScene3Field.setText("0000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.insets = new Insets(4, 0, 26, 15);
        scene3Panel.add(yScene3Field, gridBagConstraints);

        textPointer3Label.setText("Text Pointer:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new Insets(14, 20, 4, 38);
        scene3Panel.add(textPointer3Label, gridBagConstraints);

        textPointer3Field.setText("000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 34;
        gridBagConstraints.insets = new Insets(14, 10, 0, 15);
        scene3Panel.add(textPointer3Field, gridBagConstraints);

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

        originalSizeScene3Field.setEditable(false);
        originalSizeScene3Field.setText("0");
        originalSizeScene3Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        scene3Panel.add(originalSizeScene3Field, gridBagConstraints);

        currentSizeScene3Field.setEditable(false);
        currentSizeScene3Field.setText("0");
        currentSizeScene3Field.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.insets = new Insets(4, 10, 0, 0);
        scene3Panel.add(currentSizeScene3Field, gridBagConstraints);

        scene3Area.setColumns(20);
        scene3Area.setLineWrap(true);
		scene3Area.setWrapStyleWord(true);
        scene3Area.setTabSize(0);
        scene3Area.setBorder(new LineBorder(new Color(0, 0, 0)));
        scene3Area.setRows(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        //gridBagConstraints.ipadx = -62;
        //gridBagConstraints.ipady = 102;
        gridBagConstraints.insets = new Insets(0, 20, 14, 15);
        scene3Panel.add(scene3Area, gridBagConstraints);

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

        scene3BPanel.setLayout( new BorderLayout() );
        scene3BPanel.add( scene3Panel, BorderLayout.CENTER );
        scene3BPanel.add( textPointer3UpdateButton, BorderLayout.SOUTH );
        flyoverPanel.addTab("Scene 3", scene3BPanel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 30, 2);
        mainPanel.add(flyoverPanel, gridBagConstraints);

        optionsMenu.setText("Options");
        
        restoreValuesMenuItem.setText( "Restore Values" );
        optionsMenu.add( restoreValuesMenuItem );
        
        optionsMenu.add( jSeparator );
       
        preventOverwritesCheckBox.setSelected(true);
        preventOverwritesCheckBox.setText("Prevent Overwrites");
        optionsMenu.add(preventOverwritesCheckBox);

        hexTextCheckBox.setText("Hex Text");
        optionsMenu.add(hexTextCheckBox);

        optionsMenu.add(jSeparator1);

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

        optionsMenu.add(jSeparator2);

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

        
        mainWindow.getContentPane().add( flyoverMenu, BorderLayout.NORTH );
        mainWindow.getContentPane().add( mainPanel, BorderLayout.CENTER );
        

        //listeners
        hexTextCheckBox.addItemListener( this );
        
        numberStandardRadio.addActionListener( this );
        number1ppuRadio.addActionListener( this );
        number8ppuRadio.addActionListener( this );
        
        teleportMenuItem.addActionListener( this );
        
        scene1Area.getDocument().addDocumentListener( this );
        scene2Area.getDocument().addDocumentListener( this );
        scene3Area.getDocument().addDocumentListener( this );
        
        restoreValuesMenuItem.addActionListener( this );
        
        textPointer1UpdateButton.addActionListener( this );
        textPointer2UpdateButton.addActionListener( this );
        textPointer3UpdateButton.addActionListener( this );
        
        preventOverwritesCheckBox.addItemListener( this );
        
        blueFlyoverTutorialMenuItem.addActionListener( this );
        aboutFlyoverMenuItem.addActionListener( this );
    }
    
    
    public class TPDialog extends javax.swing.JDialog implements ActionListener
    {
    
        /** Creates new form tpform */
        public TPDialog(java.awt.Frame parent, boolean modal) 
        {
            super(parent, modal);
            initComponents();
        }
    
        /** This method is called from within the constructor to
        * initialize the form.
        * WARNING: Do NOT modify this code. The content of this method is
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
            addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    closeDialog(evt);
                }
            });

            warningLabel.setText("If you don't know what these are, don't change them");
            getContentPane().add(warningLabel, java.awt.BorderLayout.NORTH);

            jPanel1.setLayout(new java.awt.GridLayout(0, 2, 5, 10));

            tpScene2Label.setText("Scene 2 TP (0x96):");
            jPanel1.add(tpScene2Label);

            tpScene2Field.setText( Integer.toString( scene2.getTeleportOffset(), 16 ) );
            jPanel1.add(tpScene2Field);

            tpScene3Label.setText("Scene 3 TP (0x97):");
            jPanel1.add(tpScene3Label);

            tpScene3Field.setText( Integer.toString( scene3.getTeleportOffset(), 16 ) );
            jPanel1.add(tpScene3Field);

            updateButton.setText("Update");
            jPanel1.add(updateButton);

            cancelButton.setText("Cancel");
            jPanel1.add(cancelButton);

            getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

            pack();
            
            cancelButton.addActionListener( this );
            updateButton.addActionListener( this );
        }
    
        /** Closes the dialog */
        private void closeDialog(java.awt.event.WindowEvent evt) {
            setVisible(false);
            dispose();
        }
    
        public void actionPerformed(ActionEvent ae) 
        {
            if( ae.getSource() == cancelButton )
                closeDialog( null );
            else if( ae.getSource() == updateButton )
            {
                try
                {
                    int scene2offset = Integer.parseInt( tpScene2Field.getText(), 16 );
                    int scene3offset = Integer.parseInt( tpScene3Field.getText(), 16 );
                    
                    if( ( scene2offset < 0 || scene2offset > 0xe8 ) || ( scene3offset < 0 || scene3offset > 0xe8 ) )
                    {
                        JOptionPane.showMessageDialog( null, "must be from 0 to e8", "error", JOptionPane.ERROR_MESSAGE );
                    }
                    else
                    {
                        scene2.updateTeleportOffset( scene2offset );
                        scene3.updateTeleportOffset( scene3offset );
                        
                        closeDialog( null );
                    }
                }
                catch( NumberFormatException e )
                {
                    JOptionPane.showMessageDialog( null, "Invalid entry", "error", JOptionPane.ERROR_MESSAGE );
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
    
    //MAIN: //add mainPanel to this so that border layout doesn't conflict with gridbag
    private JPanel mainPanel;
    
    //TAB PANELS:
    private JTabbedPane     flyoverPanel;
    private JPanel          scene1Panel,
                            scene2Panel,
                            scene3Panel,
                            scene1BPanel,
                            scene2BPanel,
                            scene3BPanel;
    
    //MENU:
    private JMenuBar        flyoverMenu;
    //options section:
    private JMenu           optionsMenu;
    private JMenuItem       restoreValuesMenuItem;
    private JSeparator      jSeparator;
    private JCheckBoxMenuItem preventOverwritesCheckBox, 
                            hexTextCheckBox;
    private JSeparator      jSeparator1;
    private ButtonGroup     numberFormatButtonGroup;
    private JRadioButtonMenuItem numberStandardRadio,
                            number1ppuRadio,
                            number8ppuRadio;
    private JSeparator      jSeparator2;
    private JMenuItem       teleportMenuItem;
    //help section
    private JMenu           helpMenu;
    private JMenuItem       aboutFlyoverMenuItem, 
                            blueFlyoverTutorialMenuItem;
    
    //TEXT:
    //pointers
    private JTextField      textPointer1Field,
                            textPointer2Field,
                            textPointer3Field;
    private JLabel          textPointer1Label,
                            textPointer2Label,
                            textPointer3Label;
    private JButton         textPointer1UpdateButton,
                            textPointer2UpdateButton,
                            textPointer3UpdateButton;
    //original size:
    private JLabel          originalSizeLabel1,
                            originalSizeLabel2,
                            originalSizeLabel3;
    private JTextField      originalSizeScene1Field,
                            originalSizeScene2Field,
                            originalSizeScene3Field;
    //current size:
    private JLabel          currentSizeLabel1,
                            currentSizeLabel2,
                            currentSizeLabel3;
    private JTextField      currentSizeScene1Field,
                            currentSizeScene2Field,
                            currentSizeScene3Field;
    //text blocks
    private JTextArea       scene1Area,
                            scene2Area,
                            scene3Area;
    
    //MOVEMENT:
    //coordinates
    //scene 1
    private JLabel          startLabel1,
                            xScene1Label,
                            yScene1Label;
    private JTextField      xScene1Field,
                            yScene1Field;
    //scene 2
    private JLabel          startLabel2,
                            xScene2Label,
                            yScene2Label;
    private JTextField      xScene2Field,
                            yScene2Field;
    //scene 3
    private JLabel          startAndEndLabel,
                            xScene3Label,
                            yScene3Label,
                            x2Scene3Label,
                            y2Scene3Label;
    private JTextField      xScene3Field,
                            yScene3Field,
                            x2Scene3Field,
                            y2Scene3Field;
    //direction
    private JLabel          directionScene1Label,
                            directionScene2Label;
    private JComboBox       directionScene1Combo,
                            directionScene2Combo;
    
    // End of GUI variables declaration
    
}
