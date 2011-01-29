/*
 * BDecoder - Converts an InputStream to BEValues. Copyright (C) 2003 Mark J.
 * Wielaard
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decodes a bencoded stream to <code>BEValue</code>s.
 * 
 * A bencoded byte stream can represent byte arrays, numbers, lists and maps
 * (dictionaries).
 * 
 * It currently contains a hack to indicate a name of a dictionary of which a
 * SHA-1 digest hash should be calculated (the hash over the original bencoded
 * bytes).
 * 
 * @author Mark Wielaard (mark@klomp.org).
 */
public class BDecoder
{
    // The InputStream to BDecode.
    private final InputStream in;

    // The last indicator read.
    // Zero if unknown.
    // '0'..'9' indicates a byte[].
    // 'i' indicates an Number.
    // 'l' indicates a List.
    // 'd' indicates a Map.
    // 'e' indicates end of Number, List or Map (only used internally).
    // -1 indicates end of stream.
    // Call getNextIndicator to get the current value (will never return zero).
    private int indicator = 0;

    // Used for ugly hack to get SHA hash over the metainfo info map
    private String special_map = "info";

    private boolean in_special_map = false;

    private final MessageDigest sha_digest;

    // Ugly hack. Return the SHA has over bytes that make up the special map.
    public byte[] get_special_map_digest ()
    {
        byte[] result = sha_digest.digest();
        return result;
    }

    // Ugly hack. Name defaults to "info".
    public void set_special_map_name (String name)
    {
        special_map = name;
    }

    /**
     * Initalizes a new BDecoder. Nothing is read from the given
     * <code>InputStream</code> yet.
     */
    public BDecoder (InputStream in)
    {
        this.in = in;
        // XXX - Used for ugly hack.
        try {
            sha_digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsa) {
            throw new InternalError(nsa.toString());
        }
    }

    /**
     * Creates a new BDecoder and immediatly decodes the first value it sees.
     * 
     * @return The first BEValue on the stream or null when the stream has
     *         ended.
     * 
     * @exception InvalidBEncoding
     *                when the stream doesn't start with a bencoded value or the
     *                stream isn't a bencoded stream at all.
     * @exception IOException
     *                when somthing bad happens with the stream to read from.
     */
    public static BEValue bdecode (InputStream in) throws IOException
    {
        return new BDecoder(in).bdecode();
    }

    /**
     * Returns what the next bencoded object will be on the stream or -1 when
     * the end of stream has been reached. Can return something unexpected (not
     * '0' .. '9', 'i', 'l' or 'd') when the stream isn't bencoded.
     * 
     * This might or might not read one extra byte from the stream.
     */
    public int getNextIndicator () throws IOException
    {
        if (indicator == 0) {
            indicator = in.read();
            // XXX - Used for ugly hack
            if (in_special_map) {
                sha_digest.update((byte)indicator);
            }
        }
        return indicator;
    }

    /**
     * Gets the next indicator and returns either null when the stream has ended
     * or bdecodes the rest of the stream and returns the appropriate BEValue
     * encoded object.
     */
    public BEValue bdecode () throws IOException
    {
        indicator = getNextIndicator();
        if (indicator == -1) {
            return null;
        }

        if (indicator >= '0' && indicator <= '9') {
            return bdecodeBytes();
        } else if (indicator == 'i') {
            return bdecodeNumber();
        } else if (indicator == 'l') {
            return bdecodeList();
        } else if (indicator == 'd') {
            return bdecodeMap();
        } else {
            throw new InvalidBEncodingException("Unknown indicator '"
                + indicator + "'");
        }
    }

    /**
     * Returns the next bencoded value on the stream and makes sure it is a byte
     * array. If it is not a bencoded byte array it will throw
     * InvalidBEncodingException.
     */
    public BEValue bdecodeBytes () throws IOException
    {
        int c = getNextIndicator();
        int num = c - '0';
        if (num < 0 || num > 9) {
            throw new InvalidBEncodingException("Number expected, not '"
                + (char)c + "'");
        }
        indicator = 0;

        c = read();
        int i = c - '0';
        while (i >= 0 && i <= 9) {
            // XXX - This can overflow!
            num = num * 10 + i;
            c = read();
            i = c - '0';
        }

        if (c != ':') {
            throw new InvalidBEncodingException("Colon expected, not '"
                + (char)c + "'");
        }

        return new BEValue(read(num));
    }

    /**
     * Returns the next bencoded value on the stream and makes sure it is a
     * number. If it is not a number it will throw InvalidBEncodingException.
     */
    public BEValue bdecodeNumber () throws IOException
    {
        int c = getNextIndicator();
        if (c != 'i') {
            throw new InvalidBEncodingException("Expected 'i', not '" + (char)c
                + "'");
        }
        indicator = 0;

        c = read();
        if (c == '0') {
            c = read();
            if (c == 'e') {
                return new BEValue(BigInteger.ZERO);
            } else {
                throw new InvalidBEncodingException("'e' expected after zero,"
                    + " not '" + (char)c + "'");
            }
        }

        // XXX - We don't support more the 255 char big integers
        char[] chars = new char[256];
        int off = 0;

        if (c == '-') {
            c = read();
            if (c == '0') {
                throw new InvalidBEncodingException("Negative zero not allowed");
            }
            chars[off] = (char)c;
            off++;
        }

        if (c < '1' || c > '9') {
            throw new InvalidBEncodingException("Invalid Integer start '"
                + (char)c + "'");
        }
        chars[off] = (char)c;
        off++;

        c = read();
        int i = c - '0';
        while (i >= 0 && i <= 9) {
            chars[off] = (char)c;
            off++;
            c = read();
            i = c - '0';
        }

        if (c != 'e') {
            throw new InvalidBEncodingException("Integer should end with 'e'");
        }

        String s = new String(chars, 0, off);
        return new BEValue(new BigInteger(s));
    }

    /**
     * Returns the next bencoded value on the stream and makes sure it is a
     * list. If it is not a list it will throw InvalidBEncodingException.
     */
    public BEValue bdecodeList () throws IOException
    {
        int c = getNextIndicator();
        if (c != 'l') {
            throw new InvalidBEncodingException("Expected 'l', not '" + (char)c
                + "'");
        }
        indicator = 0;

        List<BEValue> result = new ArrayList<BEValue>();
        c = getNextIndicator();
        while (c != 'e') {
            result.add(bdecode());
            c = getNextIndicator();
        }
        indicator = 0;

        return new BEValue(result);
    }

    /**
     * Returns the next bencoded value on the stream and makes sure it is a map
     * (dictonary). If it is not a map it will throw InvalidBEncodingException.
     */
    public BEValue bdecodeMap () throws IOException
    {
        int c = getNextIndicator();
        if (c != 'd') {
            throw new InvalidBEncodingException("Expected 'd', not '" + (char)c
                + "'");
        }
        indicator = 0;

        Map<String, BEValue> result = new HashMap<String, BEValue>();
        c = getNextIndicator();
        while (c != 'e') {
            // Dictonary keys are always strings.
            String key = bdecode().getString();

            // XXX ugly hack
            boolean special = special_map.equals(key);
            if (special) {
                in_special_map = true;
            }

            BEValue value = bdecode();
            result.put(key, value);

            // XXX ugly hack continued
            if (special) {
                in_special_map = false;
            }

            c = getNextIndicator();
        }
        indicator = 0;

        return new BEValue(result);
    }

    /**
     * Returns the next byte read from the InputStream (as int). Throws
     * EOFException if InputStream.read() returned -1.
     */
    private int read () throws IOException
    {
        int c = in.read();
        if (c == -1) {
            throw new EOFException();
        }
        if (in_special_map) {
            sha_digest.update((byte)c);
        }
        return c;
    }

    /**
     * Returns a byte[] containing length valid bytes starting at offset zero.
     * Throws EOFException if InputStream.read() returned -1 before all
     * requested bytes could be read. Note that the byte[] returned might be
     * bigger then requested but will only contain length valid bytes. The
     * returned byte[] will be reused when this method is called again.
     */
    private byte[] read (int length) throws IOException
    {
        byte[] result = new byte[length];

        int read = 0;
        while (read < length) {
            int i = in.read(result, read, length - read);
            if (i == -1) {
                throw new EOFException();
            }
            read += i;
        }

        if (in_special_map) {
            sha_digest.update(result, 0, length);
        }
        return result;
    }

}
