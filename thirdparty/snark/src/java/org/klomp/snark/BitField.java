/*
 * BitField - Container of a byte array representing set and unset bits.
 * Copyright (C) 2003 Mark J. Wielaard
 * 
 * This file is part of Snark.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.klomp.snark;

/**
 * Container of a byte array representing set and unset bits.
 */
public class BitField
{

    private final byte[] bitfield;

    private final int size;

    /**
     * Creates a new BitField that represents <code>size</code> unset bits.
     */
    public BitField (int size)
    {
        this.size = size;
        int arraysize = ((size - 1) / 8) + 1;
        bitfield = new byte[arraysize];
    }

    /**
     * Creates a new BitField that represents <code>size</code> bits as set by
     * the given byte array. This will make a copy of the array. Extra bytes
     * will be ignored.
     * 
     * @exception ArrayOutOfBoundsException
     *                if give byte array is not large enough.
     */
    public BitField (byte[] bitfield, int size)
    {
        this.size = size;
        int arraysize = ((size - 1) / 8) + 1;
        this.bitfield = new byte[arraysize];

        // XXX - More correct would be to check that unused bits are
        // cleared or clear them explicitly ourselves.
        System.arraycopy(bitfield, 0, this.bitfield, 0, arraysize);
    }

    /**
     * This returns the actual byte array used. Changes to this array effect
     * this BitField. Note that some bits at the end of the byte array are
     * supposed to be always unset if they represent bits bigger then the size
     * of the bitfield.
     */
    public byte[] getFieldBytes ()
    {
        return bitfield;
    }

    /**
     * Return the size of the BitField. The returned value is one bigger then
     * the last valid bit number (since bit numbers are counted from zero).
     */
    public int size ()
    {
        return size;
    }

    /**
     * Sets the given bit to true.
     * 
     * @exception IndexOutOfBoundsException
     *                if bit is smaller then zero bigger then size (inclusive).
     */
    public void set (int bit)
    {
        if (bit < 0 || bit >= size) {
            throw new IndexOutOfBoundsException(Integer.toString(bit));
        }
        int index = bit / 8;
        int mask = 128 >> (bit % 8);
        bitfield[index] |= mask;
    }

    /**
     * Return true if the bit is set or false if it is not.
     * 
     * @exception IndexOutOfBoundsException
     *                if bit is smaller then zero bigger then size (inclusive).
     */
    public boolean get (int bit)
    {
        if (bit < 0 || bit >= size) {
            throw new IndexOutOfBoundsException(Integer.toString(bit));
        }

        int index = bit / 8;
        int mask = 128 >> (bit % 8);
        return (bitfield[index] & mask) != 0;
    }

    @Override
    public String toString ()
    {
        // Not very efficient
        StringBuffer sb = new StringBuffer("BitField[");
        for (int i = 0; i < size; i++) {
            if (get(i)) {
                sb.append(' ');
                sb.append(i);
            }
        }
        sb.append(" ]");

        return sb.toString();
    }

    public String getHumanReadable()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            if (get(i)) {
                sb.append('+');
            } else {
                sb.append('-');
            }
        }
        return sb.toString();
    }
}
