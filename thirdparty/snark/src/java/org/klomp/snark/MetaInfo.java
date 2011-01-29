/*
 * MetaInfo - Holds all information gotten from a torrent file. Copyright (C)
 * 2003 Mark J. Wielaard
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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.klomp.snark.bencode.BDecoder;
import org.klomp.snark.bencode.BEValue;
import org.klomp.snark.bencode.BEncoder;
import org.klomp.snark.bencode.InvalidBEncodingException;

public class MetaInfo
{
    private final String announce;

    private final byte[] info_hash;

    private final String name;

    private final List<List<String>> files;

    private final List<Long> lengths;

    private final int piece_length;

    private final byte[] piece_hashes;

    private final long length;

    private byte[] torrentdata;

    MetaInfo (String announce, String name, List<List<String>> files,
        List<Long> lengths, int piece_length, byte[] piece_hashes, long length)
    {
        this.announce = announce;
        this.name = name;
        this.files = files;
        this.lengths = lengths;
        this.piece_length = piece_length;
        this.piece_hashes = piece_hashes;
        this.length = length;

        this.info_hash = calculateInfoHash();
    }

    /**
     * Creates a new MetaInfo from the given InputStream. The InputStream must
     * start with a correctly bencoded dictonary describing the torrent.
     */
    public MetaInfo (InputStream in) throws IOException
    {
        this(new BDecoder(in));
    }

    /**
     * Creates a new MetaInfo from the given BDecoder. The BDecoder must have a
     * complete dictionary describing the torrent.
     */
    public MetaInfo (BDecoder be) throws IOException
    {
        // Note that evaluation order matters here...
        this(be.bdecodeMap().getMap());
    }

    /**
     * Creates a new MetaInfo from a Map of BEValues and the SHA1 over the
     * original bencoded info dictonary (this is a hack, we could reconstruct
     * the bencoded stream and recalculate the hash). Will throw a
     * InvalidBEncodingException if the given map does not contain a valid
     * announce string or info dictonary.
     */
    public MetaInfo (Map m) throws InvalidBEncodingException
    {
        BEValue val = (BEValue)m.get("announce");
        if (val == null) {
            throw new InvalidBEncodingException("Missing announce string");
        }
        this.announce = val.getString();

        val = (BEValue)m.get("info");
        if (val == null) {
            throw new InvalidBEncodingException("Missing info map");
        }
        Map info = val.getMap();

        val = (BEValue)info.get("name");
        if (val == null) {
            throw new InvalidBEncodingException("Missing name string");
        }
        name = val.getString();

        val = (BEValue)info.get("piece length");
        if (val == null) {
            throw new InvalidBEncodingException("Missing piece length number");
        }
        piece_length = val.getInt();

        val = (BEValue)info.get("pieces");
        if (val == null) {
            throw new InvalidBEncodingException("Missing piece bytes");
        }
        piece_hashes = val.getBytes();

        val = (BEValue)info.get("length");
        if (val != null) {
            // Single file case.
            length = val.getLong();
            files = null;
            lengths = null;
        } else {
            // Multi file case.
            val = (BEValue)info.get("files");
            if (val == null) {
                throw new InvalidBEncodingException(
                    "Missing length number and/or files list");
            }

            List list = val.getList();
            int size = list.size();
            if (size == 0) {
                throw new InvalidBEncodingException("zero size files list");
            }

            files = new ArrayList<List<String>>(size);
            lengths = new ArrayList<Long>(size);
            long l = 0;
            for (int i = 0; i < list.size(); i++) {
                Map desc = ((BEValue)list.get(i)).getMap();
                val = (BEValue)desc.get("length");
                if (val == null) {
                    throw new InvalidBEncodingException("Missing length number");
                }
                long len = val.getLong();
                lengths.add(len);
                l += len;

                val = (BEValue)desc.get("path");
                if (val == null) {
                    throw new InvalidBEncodingException("Missing path list");
                }
                List<BEValue> path_list = val.getList();
                int path_length = path_list.size();
                if (path_length == 0) {
                    throw new InvalidBEncodingException(
                        "zero size file path list");
                }

                List<String> file = new ArrayList<String>(path_length);
                for (BEValue value : path_list) {
                    file.add(value.getString());
                }

                files.add(file);
            }
            length = l;
        }

        info_hash = calculateInfoHash();
    }

    /**
     * Returns the string representing the URL of the tracker for this torrent.
     */
    public String getAnnounce ()
    {
        return announce;
    }

    /**
     * Returns the original 20 byte SHA1 hash over the bencoded info map.
     */
    public byte[] getInfoHash ()
    {
        // XXX - Should we return a clone, just to be sure?
        return info_hash;
    }

    public String getHexInfoHash ()
    {
        return hexencode(info_hash);
    }

    /**
     * Returns the piece hashes. Only used by storage so package local.
     */
    byte[] getPieceHashes ()
    {
        return piece_hashes;
    }

    /**
     * Returns the requested name for the file or toplevel directory. If it is a
     * toplevel directory name getFiles() will return a non-null List of file
     * name hierarchy name.
     */
    public String getName ()
    {
        return name;
    }

    /**
     * Returns a list of lists of file name hierarchies or null if it is a
     * single name. It has the same size as the list returned by getLengths().
     */
    public List getFiles ()
    {
        // XXX - Immutable?
        return files;
    }

    /**
     * Returns a list of Longs indication the size of the individual files, or
     * null if it is a single file. It has the same size as the list returned by
     * getFiles().
     */
    public List getLengths ()
    {
        // XXX - Immutable?
        return lengths;
    }

    /**
     * Returns the number of pieces.
     */
    public int getPieces ()
    {
        return piece_hashes.length / 20;
    }

    /**
     * Return the length of a piece. All pieces are of equal length except for
     * the last one (<code>getPieces()-1</code>).
     * 
     * @exception IndexOutOfBoundsException
     *                when piece is equal to or greater then the number of
     *                pieces in the torrent.
     */
    public int getPieceLength (int piece)
    {
        int pieces = getPieces();
        if (piece >= 0 && piece < pieces - 1) {
            return piece_length;
        } else if (piece == pieces - 1) {
            return (int)(length - piece * piece_length);
        } else {
            throw new IndexOutOfBoundsException("no piece: " + piece);
        }
    }

    /**
     * Checks that the given piece has the same SHA1 hash as the given byte
     * array. Returns random results or IndexOutOfBoundsExceptions when the
     * piece number is unknown.
     */
    public boolean checkPiece (int piece, byte[] bs, int off, int length)
    {
        // Check digest
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("No SHA digest available: " + nsae);
        }

        sha1.update(bs, off, length);
        byte[] hash = sha1.digest();
        for (int i = 0; i < 20; i++) {
            if (hash[i] != piece_hashes[20 * piece + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the total length of the torrent in bytes.
     */
    public long getTotalLength ()
    {
        return length;
    }

    @Override
    public String toString ()
    {
        return "MetaInfo[info_hash='" + hexencode(info_hash) + "', announce='"
            + announce + "', name='" + name + "', files=" + files
            + ", #pieces='" + piece_hashes.length / 20 + "', piece_length='"
            + piece_length + "', length='" + length + "']";
    }

    /**
     * Encode a byte array as a hex encoded string.
     */
    private static String hexencode (byte[] bs)
    {
        StringBuffer sb = new StringBuffer(bs.length * 2);
        for (byte element : bs) {
            int c = element & 0xFF;
            if (c < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(c));
        }

        return sb.toString();
    }

    /**
     * Creates a copy of this MetaInfo that shares everything except the
     * announce URL.
     */
    public MetaInfo reannounce (String announce)
    {
        return new MetaInfo(announce, name, files, lengths, piece_length,
            piece_hashes, length);
    }

    public byte[] getTorrentData ()
    {
        if (torrentdata == null) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("announce", announce);
            Map info = createInfoMap();
            m.put("info", info);
            torrentdata = BEncoder.bencode(m);
        }
        return torrentdata;
    }

    private Map<String, Object> createInfoMap ()
    {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("name", name);
        info.put("piece length", piece_length);
        info.put("pieces", piece_hashes);
        if (files == null) {
            info.put("length", new Long(length));
        } else {
            List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < files.size(); i++) {
                Map<String, Object> file = new HashMap<String, Object>();
                file.put("path", files.get(i));
                file.put("length", lengths.get(i));
                l.add(file);
            }
            info.put("files", l);
        }
        return info;
    }

    private byte[] calculateInfoHash ()
    {
        Map<String, Object> info = createInfoMap();
        byte[] infoBytes = BEncoder.bencode(info);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            return digest.digest(infoBytes);
        } catch (NoSuchAlgorithmException nsa) {
            throw new InternalError(nsa.toString());
        }
    }

}
