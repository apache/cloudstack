// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package streamer;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a slice in a buffer.
 */
public class ByteBuffer {

    public static final String SEQUENCE_NUMBER = "seq";

    public byte data[];
    public int offset = 0;
    public int length = 0;
    public int cursor = 0;

    private int refCount = 1;
    private ByteBuffer parentByteBuffer = null;

    private Order order;

    /**
     * Create buffer of size no less than length. Buffer can be a bit larger than
     * length. Offset also can be set to non-zero value to leave some place for
     * future headers.
     */
    public ByteBuffer(int minLength) {
        // Get buffer of acceptable size from buffer pool
        this.data = BufferPool.allocateNewBuffer(minLength);
        this.offset = 0;
        this.length = minLength;
    }

    public ByteBuffer(byte data[]) {
        if (data == null)
            throw new NullPointerException("Data must be non-null.");

        this.data = data;
        this.offset = 0;
        this.length = data.length;
    }

    public ByteBuffer(byte[] data, int offset, int length) {
        if (data == null)
            throw new NullPointerException("Data must be non-null.");

        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Create byte buffer of requested size with some space reserved for future
     * headers.
     */
    public ByteBuffer(int minLength, boolean reserveSpaceForHeader) {
        // Get buffer of acceptable size from buffer pool
        this.data = BufferPool.allocateNewBuffer(128 + minLength);
        this.offset = 128; // 100 bytes should be enough for headers
        this.length = minLength;
    }

    /**
     * Create empty buffer with given order only.
     */
    public ByteBuffer(Order order) {
        this.order = order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return toString(100);
    }

    /**
     * Return string representation of this byte buffer.
     *
     * @param maxLength
     *          number of bytes to show in string
     */
    public String toString(int maxLength) {
        return "ByteRange(){offset=" + offset + ", length=" + length + ", cursor=" + cursor + ", data=" + ((data == null) ? "null" : toHexString(maxLength)) +
            ((metadata == null || metadata.size() == 0) ? "" : ", metadata=" + metadata) + "}";
    }

    /**
     * Return string representation of this byte buffer as hexadecimal numbers,
     * e.g. "[0x01, 0x02]".
     *
     * @param maxLength
     *          number of bytes to show in string
     */
    public String toHexString(int maxLength) {
        StringBuilder builder = new StringBuilder(maxLength * 6);
        builder.append('[');
        for (int i = 0; i < maxLength && i < length; i++) {
            if (i > 0)
                builder.append(", ");
            int b = data[offset + i] & 0xff;
            builder.append("0x" + ((b < 16) ? "0" : "") + Integer.toString(b, 16));
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * Return string representation of this byte buffer as hexadecimal numbers,
     * e.g. "01 02".
     *
     * @param maxLength
     *          number of bytes to show in string
     */
    public String toPlainHexString(int maxLength) {
        StringBuilder builder = new StringBuilder(maxLength * 3);
        for (int i = 0; i < maxLength && i < length; i++) {
            if (i > 0)
                builder.append(" ");
            int b = data[offset + i] & 0xff;
            builder.append(((b < 16) ? "0" : "") + Integer.toString(b, 16));
        }
        return builder.toString();
    }

    public void dump() {
        System.out.println(toString(length));
    }

    public void extend(int newLength) {
        if (data.length < newLength)
            Arrays.copyOf(data, newLength);
    }

    public void ref() {
        refCount++;
    }

    public void unref() {
        refCount--;

        if (refCount == 0) {

            if (parentByteBuffer != null) {
                parentByteBuffer.unref();
                parentByteBuffer = null;
            } else {
                // Return buffer to buffer pool
                BufferPool.recycleBuffer(data);
            }

            data = null;
        }

    }

    public boolean isSoleOwner() {
        return refCount == 1 && (parentByteBuffer == null);
    }

    /**
     * Create shared lightweight copy of part of this buffer.
     */
    public ByteBuffer slice(int offset, int length, boolean copyMetadata) {
        ref();

        if (this.length < (offset + length))
            throw new RuntimeException("Length of region is larger that length of this buffer. Buffer length: " + this.length + ", offset: " + offset +
                ", new region length: " + length + ".");

        ByteBuffer slice = new ByteBuffer(data, this.offset + offset, length);

        if (copyMetadata && this.metadata != null)
            slice.metadata = new HashMap<String, Object>(metadata);

        return slice;
    }

    private Map<String, Object> metadata = null;

    public Object putMetadata(String key, Object value) {
        if (metadata == null)
            metadata = new HashMap<String, Object>();
        return metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return (metadata != null) ? metadata.get(key) : null;
    }

    /**
     * Create new buffer, which holds data from both buffers. Expensive operation.
     *
     * @TODO if only one reference to this ByteBuffer exists, then extend this
     *       buffer instead of creating new buffer
     * @TODO support list of buffers to avoid expensive joins until absolute
     *       necessary
     */
    public ByteBuffer join(ByteBuffer buf) {
        // Extend byte array for new data
        int newLength = length + buf.length;
        byte newData[] = new byte[newLength];

        // Copy data from our buffer
        System.arraycopy(data, offset, newData, 0, length);

        // Copy data from other buffer
        System.arraycopy(buf.data, buf.offset, newData, length, buf.length);

        ByteBuffer newBuf = new ByteBuffer(newData);

        // Copy our (older) metadata to new buffer, because handler might store some
        // metadata in buffer, which is pushed back.
        if (metadata != null)
            newBuf.metadata = new HashMap<String, Object>(metadata);

        return newBuf;
    }

    /**
     * Copy used portion of buffer to new byte array. Expensive operation.
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(data, offset, offset + length);
    }

    public short[] toShortArray() {
        if (length % 2 != 0)
            throw new ArrayIndexOutOfBoundsException("Length of byte array must be dividable by 2 without remainder. Array length: " + length + ", remainder: " +
                (length % 2) + ".");

        short[] buf = new short[length / 2];

        for (int i = 0, j = offset; i < buf.length; i++, j += 2) {
            buf[i] = (short)((data[j + 0] & 0xFF) | ((data[j + 1] & 0xFF) << 8));
        }
        return buf;
    }

    /**
     * Return array of int's in little endian order.
     */
    public int[] toIntLEArray() {
        if (length % 4 != 0)
            throw new ArrayIndexOutOfBoundsException("Length of byte array must be dividable by 4 without remainder. Array length: " + length + ", remainder: " +
                (length % 4) + ".");

        int[] buf = new int[length / 4];

        for (int i = 0, j = offset; i < buf.length; i++, j += 4) {
            buf[i] = (data[j + 0] & 0xFF) | ((data[j + 1] & 0xFF) << 8) | ((data[j + 2] & 0xFF) << 16) | ((data[j + 3] & 0xFF) << 24);
        }
        return buf;
    }

    /**
     * Return array of int's in little endian order, but use only 3 bytes per int (3RGB).
     */
    public int[] toInt3LEArray() {
        if (length % 3 != 0)
            throw new ArrayIndexOutOfBoundsException("Length of byte array must be dividable by 3 without remainder. Array length: " + length + ", remainder: " +
                (length % 3) + ".");

        int[] buf = new int[length / 3];

        for (int i = 0, j = offset; i < buf.length; i++, j += 3) {
            buf[i] = (data[j + 0] & 0xFF) | ((data[j + 1] & 0xFF) << 8) | ((data[j + 2] & 0xFF) << 16);
        }
        return buf;
    }

    /**
     * Helper method for test cases to convert array of byte arrays to array of
     * byte buffers.
     */
    public static ByteBuffer[] convertByteArraysToByteBuffers(byte[]... bas) {
        ByteBuffer bufs[] = new ByteBuffer[bas.length];

        int i = 0;
        for (byte[] ba : bas) {
            bufs[i++] = new ByteBuffer(ba);
        }
        return bufs;
    }

    /**
     * Read signed int in network order. Cursor is advanced by 4.
     */
    public int readSignedInt() {
        if (cursor + 4 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 4 bytes from this buffer: " + this + ".");

        int result =
            (((data[offset + cursor] & 0xff) << 24) + ((data[offset + cursor + 1] & 0xff) << 16) + ((data[offset + cursor + 2] & 0xff) << 8) + (data[offset + cursor + 3] & 0xff));
        cursor += 4;
        return result;
    }

    /**
     * Read signed int in little endian order. Cursor is advanced by 4.
     */
    public int readSignedIntLE() {
        if (cursor + 4 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 4 bytes from this buffer: " + this + ".");

        int result =
            (((data[offset + cursor + 3] & 0xff) << 24) + ((data[offset + cursor + 2] & 0xff) << 16) + ((data[offset + cursor + 1] & 0xff) << 8) + (data[offset + cursor] & 0xff));
        cursor += 4;
        return result;
    }

    /**
     * Read unsigned int in little endian order. Cursor is advanced by 4.
     */
    public long readUnsignedIntLE() {
        if (cursor + 4 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 4 bytes from this buffer: " + this + ".");

        long result =
            (((long)(data[offset + cursor + 3] & 0xff) << 24) + ((long)(data[offset + cursor + 2] & 0xff) << 16) + ((long)(data[offset + cursor + 1] & 0xff) << 8) + (data[offset +
                cursor] & 0xff));
        cursor += 4;
        return result;
    }

    /**
     * Read signed int in variable length format. Top most bit of each byte
     * indicates that next byte contains additional bits. Cursor is advanced by
     * 1-5 bytes.
     */
    public int readVariableSignedIntLE() {
        int result = 0;

        for (int shift = 0; shift < 32; shift += 7) {
            int b = readUnsignedByte();
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0)
                break;
        }

        return result;
    }

    /**
     * Read unsigned int in network order in variable length format. Cursor is
     * advanced by 1 to 4 bytes.
     *
     * Two most significant bits of first byte indicates length of field: 0x00 - 1
     * byte, 0x40 - 2 bytes, 0x80 - 3 bytes, 0xc0 - 4 bytes.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc241614.aspx
     */
    public int readEncodedUnsignedInt() {
        int firstByte = readUnsignedByte();
        int result;
        switch (firstByte & 0xc0) {
            default:
            case 0x00:
                result = firstByte & 0x3f;
                break;
            case 0x40:
                result = (firstByte & 0x3f << 8) | readUnsignedByte();
                break;
            case 0x80:
                result = (((firstByte & 0x3f << 8) | readUnsignedByte()) << 8) | readUnsignedByte();
                break;
            case 0xc0:
                result = ((((firstByte & 0x3f << 8) | readUnsignedByte()) << 8) | readUnsignedByte() << 8) | readUnsignedByte();
                break;
        }

        return result;
    }

    /**
     * Read unsigned byte. Cursor is advanced by 1.
     */
    public int readUnsignedByte() {
        if (cursor + 1 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 1 byte from this buffer: " + this + ".");

        int b = data[offset + cursor] & 0xff;
        cursor += 1;
        return b;
    }

    /**
     * Read signed byte. Cursor is advanced by 1.
     */
    public byte readSignedByte() {
        if (cursor + 1 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 1 byte from this buffer: " + this + ".");

        byte b = data[offset + cursor];
        cursor += 1;
        return b;
    }

    /**
     * Read unsigned short in network order. Cursor is advanced by 2.
     */
    public int readUnsignedShort() {
        if (cursor + 2 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 2 bytes from this buffer: " + this + ".");

        int result = (((data[offset + cursor] & 0xff) << 8) | (data[offset + cursor + 1] & 0xff));
        cursor += 2;
        return result;
    }

    /**
     * Read signed short in little endian order. Cursor is advanced by 2.
     */
    public short readSignedShortLE() {
        if (cursor + 2 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 2 bytes from this buffer: " + this + ".");

        short result = (short)(((data[offset + cursor + 1] & 0xff) << 8) | (data[offset + cursor] & 0xff));
        cursor += 2;
        return result;
    }

    /**
     * Read unsigned short in network order in variable length format. Cursor is
     * advanced by 1 or 2 bytes.
     *
     * Most significant bit of first byte indicates length of field: 0 - 1 byte, 1
     * - 2 bytes.
     */
    public int readVariableUnsignedShort() {
        int firstByte = readUnsignedByte();

        int result;
        if ((firstByte & 0x80) == 0)
            result = firstByte & 0x7f;
        else {
            int secondByte = readUnsignedByte();
            result = (((firstByte & 0x7f) << 8) | secondByte);
        }

        return result;
    }

    /**
     * Read unsigned short in little endian order. Cursor is advanced by 2.
     */
    public int readUnsignedShortLE() {
        if (cursor + 2 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read 2 bytes from this buffer: " + this + ".");

        int result = (((data[offset + cursor + 1] & 0xff) << 8) | (data[offset + cursor] & 0xff));
        cursor += 2;
        return result;
    }

    /**
     * Read unsigned short in network order in variable length format. Cursor is
     * advanced by 1 or 2 bytes.
     *
     * Most significant bit of first byte indicates length of field: 0x00 - 1
     * byte, 0x80 - 2 bytes.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc241612.aspx
     */
    public int readEncodedUnsignedShort() {
        int firstByte = readUnsignedByte();

        int result;
        if ((firstByte & 0x80) == 0)
            result = firstByte & 0x7f;
        else {
            int secondByte = readUnsignedByte();
            result = (((firstByte & 0x7f) << 8) | secondByte);
        }

        return result;
    }

    /**
     * Read signed short in network order in variable length format. Cursor is
     * advanced by 1 or 2 bytes.
     *
     * Most significant bit of first byte indicates length of field: 0x00 - 1
     * byte, 0x80 - 2 bytes. Second most significant bit indicates is value
     * positive or negative.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc241613.aspx
     */
    public int readEncodedSignedShort() {
        int firstByte = readUnsignedByte();

        int result;
        if ((firstByte & 0x80) == 0)
            result = firstByte & 0x3f;
        else {
            int secondByte = readUnsignedByte();
            result = (((firstByte & 0x3f) << 8) | secondByte);
        }

        if ((firstByte & 0x40) > 0)
            return -result;
        else
            return result;
    }

    /**
     * Read signed long in little endian order. Cursor is advanced by 8 bytes.
     */
    public long readSignedLongLE() {
        return ((readSignedIntLE()) & 0xffFFffFFL) | (((long)readSignedIntLE()) << 32);
    }

    /**
     * Read string from buffer. Cursor is advanced by string length.
     */
    public String readString(int length, Charset charset) {
        if (cursor + length > this.length)
            throw new ArrayIndexOutOfBoundsException("Cannot read " + length + " bytes from this buffer: " + this + ".");

        String string = new String(data, offset + cursor, length, charset);
        cursor += length;
        return string;
    }

    /**
     * Get bytes as lightweight slice. Cursor is advanced by data length.
     */
    public ByteBuffer readBytes(int dataLength) {
        if (cursor + dataLength > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read " + dataLength + " bytes from this buffer: " + this + ".");

        ByteBuffer slice = slice(cursor, dataLength, false);
        cursor += dataLength;
        return slice;
    }

    /**
     * Cursor is advanced by given number of bytes.
     */
    public void skipBytes(int numOfBytes) {
        if (cursor + numOfBytes > length)
            throw new ArrayIndexOutOfBoundsException("Cannot read " + numOfBytes + " bytes from this buffer: " + this + ".");

        cursor += numOfBytes;
    }

    /**
     * Write byte. Cursor is advanced by 1.
     */
    public void writeByte(int b) {
        if (cursor + 1 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot write 1 byte to this buffer: " + this + ".");

        data[offset + cursor] = (byte)b;
        cursor += 1;
    }

    /**
     * Write short in network order. Cursor is advanced by 2.
     */
    public void writeShort(int x) {
        if (cursor + 2 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot write 2 bytes to this buffer: " + this + ".");

        data[offset + cursor] = (byte)(x >> 8);
        data[offset + cursor + 1] = (byte)x;
        cursor += 2;
    }

    /**
     * Write short in little endian order. Cursor is advanced by 2.
     */
    public void writeShortLE(int x) {
        if (cursor + 2 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot write 2 bytes to this buffer: " + this + ".");

        data[offset + cursor + 1] = (byte)(x >> 8);
        data[offset + cursor] = (byte)x;
        cursor += 2;
    }

    /**
     * Write int in network order. Cursor is advanced by 4.
     */
    public void writeInt(int i) {
        if (cursor + 4 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot write 4 bytes to this buffer: " + this + ".");

        data[offset + cursor] = (byte)(i >> 24);
        data[offset + cursor + 1] = (byte)(i >> 16);
        data[offset + cursor + 2] = (byte)(i >> 8);
        data[offset + cursor + 3] = (byte)i;
        cursor += 4;
    }

    public void writeIntLE(int i) {
        if (cursor + 4 > length)
            throw new ArrayIndexOutOfBoundsException("Cannot write 4 bytes to this buffer: " + this + ".");

        data[offset + cursor] = (byte)i;
        data[offset + cursor + 1] = (byte)(i >> 8);
        data[offset + cursor + 2] = (byte)(i >> 16);
        data[offset + cursor + 3] = (byte)(i >> 24);
        cursor += 4;
    }

    /**
     * Write int in variable length format. Cursor is advanced by number of bytes
     * written (1-5).
     *
     * Topmost bit of each byte is set to 1 to indicate that next byte has data.
     */
    public void writeVariableIntLE(int i) {
        while (i != 0) {
            // Get lower bits of number
            int b = i & 0x7f;
            i >>= 7;

            if (i > 0)
                // Set topmost bit of byte to indicate that next byte(s) contains
                // remainder bits
                b |= 0x80;

            writeByte(b);
        }
    }

    /**
     * Write short in variable length format. Cursor is advanced by number of
     * bytes written (1-2).
     *
     * Topmost bit of first byte is set to 1 to indicate that next byte has data.
     */
    public void writeVariableShort(int length) {
        if (length > 0x7f | length < 0)
            writeShort(length | 0x8000);
        else
            writeByte(length);
    }

    /**
     * Prepend given data to this byte buffer.
     */
    public void prepend(ByteBuffer buf) {
        prepend(buf.data, buf.offset, buf.length);
    }

    /**
     * Prepend given data to this byte buffer.
     */
    public void prepend(byte[] data) {
        prepend(data, 0, data.length);
    }

    /**
     * Prepend given data to this byte buffer.
     */
    public void prepend(byte[] data, int offset, int length) {
        if (!isSoleOwner()) {
            throw new RuntimeException("Create full copy of this byte buffer data for modification. refCount: " + refCount + ", parentByteBuffer: " + parentByteBuffer +
                ".");
        }

        // If there is no enough space for header to prepend
        if (!(this.offset >= length)) {
            throw new RuntimeException("Reserve data to have enough space for header.");
        }

        // Copy header
        System.arraycopy(data, offset, this.data, this.offset - length, length);

        // Extend byte range to include header
        this.offset -= length;
        this.length += length;
        this.cursor += length;
    }

    public void writeString(String str, Charset charset) {
        writeBytes(str.getBytes(charset));
    }

    /**
     * Write string of fixed size. When string is shorted, empty space is filled
     * with zeros. When string is larger, it is truncated.
     */
    public void writeFixedString(int length, String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        writeBytes(bytes, 0, Math.min(bytes.length, length));

        for (int i = bytes.length; i < length; i++)
            writeByte(0);
    }

    public void writeBytes(ByteBuffer buf) {
        writeBytes(buf.data, buf.offset, buf.length);
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int length) {
        System.arraycopy(bytes, offset, this.data, this.offset + this.cursor, length);
        cursor += length;
    }

    // /**
    // * Write BER encoded definite long variant of the ASN.1 length field.
    // */
    // public void writeBerLength(int value) {
    // int fieldLength;
    // if (value > 0xFFffFF)
    // fieldLength = 4;
    // else if (value > 0xFFff)
    // fieldLength = 3;
    // else if (value > 0xFF)
    // fieldLength = 2;
    // else
    // fieldLength = 1;
    //
    // if (cursor + fieldLength + 1 > length)
    // throw new ArrayIndexOutOfBoundsException("Cannot write " + (fieldLength +
    // 1) + " byte(s) to this buffer: " + this + ".");
    //
    // // Write length of length field itself
    // writeByte(0x80 | fieldLength);
    //
    // switch (fieldLength) {
    // case 4:
    // data[offset + cursor++] = (byte) (value >> 24);
    // case 3:
    // data[offset + cursor++] = (byte) (value >> 16);
    // case 2:
    // data[offset + cursor++] = (byte) (value >> 8);
    // case 1:
    // data[offset + cursor++] = (byte) value;
    // }
    //
    // }

    /**
     * Reduce length of buffer to cursor position.
     */
    public void trimAtCursor() {
        length = cursor;
    }

    /**
     * Rewind cursor to beginning of buffer.
     */
    public void rewindCursor() {
        cursor = 0;
    }

    /**
     * Read RGB color in LE order. Cursor is advanced by 3.
     *
     * @return color as int, with red in lowest octet.
     */
    public int readRGBColor() {
        return readUnsignedByte() | (readUnsignedByte() << 8) | (readUnsignedByte() << 16);
    }

    public void assertThatBufferIsFullyRead() {
        if (cursor != length)
            throw new RuntimeException("Data in buffer is not read fully. Buf: " + this + ".");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        int end = offset + length;
        for (int i = offset; i < end; i++)
            result = 31 * result + data[i];

        result = prime * result + length;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        ByteBuffer other = (ByteBuffer)obj;
        if (length != other.length)
            return false;

        for (int i = 0; i < length; i++)
            if (data[offset + i] != other.data[other.offset + i])
                return false;

        return true;
    }

}
