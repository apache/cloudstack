/*
 * BEValue - Holds different types that a bencoded byte array can represent.
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

package org.klomp.snark.bencode;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * Holds different types that a bencoded byte array can represent. You need to
 * call the correct get method to get the correct java type object. If the
 * BEValue wasn't actually of the requested type you will get a
 * InvalidBEncodingException.
 * 
 * @author Mark Wielaard (mark@klomp.org)
 */
public class BEValue
{
    // This is either a byte[], Number, List or Map.
    private final Object value;

    public BEValue (byte[] value)
    {
        this.value = value;
    }

    public BEValue (Number value)
    {
        this.value = value;
    }

    public BEValue (List value)
    {
        this.value = value;
    }

    public BEValue (Map value)
    {
        this.value = value;
    }

    /**
     * Returns this BEValue as a String. This operation only succeeds when the
     * BEValue is a byte[], otherwise it will throw a InvalidBEncodingException.
     * The byte[] will be interpreted as UTF-8 encoded characters.
     */
    public String getString () throws InvalidBEncodingException
    {
        try {
            return new String(getBytes(), "UTF-8");
        } catch (ClassCastException cce) {
            throw new InvalidBEncodingException(cce.toString());
        } catch (UnsupportedEncodingException uee) {
            throw new InternalError(uee.toString());
        }
    }

    /**
     * Returns this BEValue as a byte[]. This operation only succeeds when the
     * BEValue is actually a byte[], otherwise it will throw a
     * InvalidBEncodingException.
     */
    public byte[] getBytes () throws InvalidBEncodingException
    {
        try {
            return (byte[])value;
        } catch (ClassCastException cce) {
            throw new InvalidBEncodingException(cce.toString());
        }
    }

    /**
     * Returns this BEValue as a Number. This operation only succeeds when the
     * BEValue is actually a Number, otherwise it will throw a
     * InvalidBEncodingException.
     */
    public Number getNumber () throws InvalidBEncodingException
    {
        try {
            return (Number)value;
        } catch (ClassCastException cce) {
            throw new InvalidBEncodingException(cce.toString());
        }
    }

    /**
     * Returns this BEValue as int. This operation only succeeds when the
     * BEValue is actually a Number, otherwise it will throw a
     * InvalidBEncodingException. The returned int is the result of
     * <code>Number.intValue()</code>.
     */
    public int getInt () throws InvalidBEncodingException
    {
        return getNumber().intValue();
    }

    /**
     * Returns this BEValue as long. This operation only succeeds when the
     * BEValue is actually a Number, otherwise it will throw a
     * InvalidBEncodingException. The returned long is the result of
     * <code>Number.longValue()</code>.
     */
    public long getLong () throws InvalidBEncodingException
    {
        return getNumber().longValue();
    }

    /**
     * Returns this BEValue as a List of BEValues. This operation only succeeds
     * when the BEValue is actually a List, otherwise it will throw a
     * InvalidBEncodingException.
     */
    public List<BEValue> getList () throws InvalidBEncodingException
    {
        try {
            return (List<BEValue>)value;
        } catch (ClassCastException cce) {
            throw new InvalidBEncodingException(cce.toString());
        }
    }

    /**
     * Returns this BEValue as a Map of BEValue keys and BEValue values. This
     * operation only succeeds when the BEValue is actually a Map, otherwise it
     * will throw a InvalidBEncodingException.
     */
    public Map getMap () throws InvalidBEncodingException
    {
        try {
            return (Map)value;
        } catch (ClassCastException cce) {
            throw new InvalidBEncodingException(cce.toString());
        }
    }

    @Override
    public String toString ()
    {
        String valueString;
        if (value instanceof byte[]) {
            byte[] bs = (byte[])value;
            // XXX - Stupid heuristic...
            if (bs.length <= 12) {
                valueString = new String(bs);
            } else {
                valueString = "bytes:" + bs.length;
            }
        } else {
            valueString = value.toString();
        }

        return "BEValue[" + valueString + "]";
    }
}
