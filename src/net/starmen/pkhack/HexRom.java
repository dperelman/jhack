/*
 * Created on Jan 16, 2004
 */
package net.starmen.pkhack;

/**
 * TODO Write javadoc for this class
 * @author AnyoneEB
 */
public class HexRom implements HexData
{
    Rom rom;
    public HexRom(Rom rom)
    {
        this.rom = rom;
    }

        public static final int ROW_SIZE = 16;
        
        public int getRowCount()
        {
            int rows = rom.length() / ROW_SIZE;
            if (rows * ROW_SIZE < rom.length()) rows++;
            return rows;
        }
        
        public int getColumnCount()
        {
            return ROW_SIZE;
        }
        
        public int getLastRowSize()
        {
            int max = (getRowCount() - 1) * ROW_SIZE;
            if ((rom.length() - max) == 0) return ROW_SIZE;
            return rom.length() - max;
        }
        
        public byte getByte(int row, int col)
        {
            return rom.readByte(row * ROW_SIZE + col);
        }

        public void setByte(int row, int col, byte value)
        {
            rom.write(row * ROW_SIZE + col,value);
        }

        public byte[] getRow(int row)
        {
            return rom.readByte(row * ROW_SIZE, row == getRowCount() - 1 ? getLastRowSize() : ROW_SIZE);
        }
}
