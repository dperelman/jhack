/*
 * Created on Apr 3, 2005
 */
package net.starmen.pkhack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ByteArrHasher
{
    private int hint;
    private byte[] b;

    public ByteArrHasher(byte[] b)
    {
        this.b = b;
        byte[] hash;
        try
        {
            hash = MessageDigest.getInstance("MD5").digest(b);
        }
        catch (NoSuchAlgorithmException e)
        {
            hash = new byte[]{0, 0, 0, 0};
            e.printStackTrace();
        }
        hint = (hash[0] & 0xff) + ((hash[1] & 0xff) << 8)
            + ((hash[2] & 0xff) << 16) + ((hash[3] & 0xff) << 24);
    }

    public int hashCode()
    {
        return hint;
    }

    public boolean equals(ByteArrHasher o)
    {
        return Arrays.equals(b, o.b);
    }

    protected Object clone()
    {
        return new ByteArrHasher(b);
    }

    public String toString()
    {
        return Integer.toString(hint);
    }

    public Integer toInteger()
    {
        return new Integer(hint);
    }
}