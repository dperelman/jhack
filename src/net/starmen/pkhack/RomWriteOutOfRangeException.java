/*
 * Created on Sep 11, 2003
 */
package net.starmen.pkhack;

/**
 * Exception thrown when a write to the ROM to outside the area for that data on the ROM map.
 * See <code>Exception</code> for descriptions of the constuctors.  
 * 
 * @author AnyoneEB
 */
public class RomWriteOutOfRangeException extends Exception
{

	public RomWriteOutOfRangeException()
	{
		super();
	}
	public RomWriteOutOfRangeException(String arg0)
	{
		super(arg0);
	}
	public RomWriteOutOfRangeException(Throwable arg0)
	{
		super(arg0);
	}
	public RomWriteOutOfRangeException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
