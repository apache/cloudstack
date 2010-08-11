/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import com.cloud.utils.encoding.Base64;
import com.cloud.utils.net.MacAddress;

/**
 * UUID creates a globally unique id.  The format it uses is as follows.
 * The first byte contains a 2 bit version number and a 2 bit jvm
 * instance id.  The next 6 bytes contain the mac address.  The next 5 bytes
 * contain the a snapshot of System.currentTimeMillis() >> 10 & 0xFFFFFFFFFF.
 * The last 4 bytes contain an ever increasing counter.  If the the counter
 * overflows, it resets and another snapshot of
 * System.currentTimeMillis() >> 10 & 0xFFFFFFFFFF is taken.
 * 
 * There are several advantages to this implementation:
 *   1. There's no need to save a system wide number if the system restarts.
 *      The MAC address and currentTimeMillis() together guarantees that the
 *      counter portion will be unique.
 *   2. It allows you to deploy four jvms using the same UUID to generate
 *      unique ids.  The instance id is retrieved from server.properties
 *      using the key server.instance.  Each jvm will need to specify a
 *      different value (0-3).
 *   3. The UUID is guaranteed to be unique for roughly 35,000 years.
 * 
 * Note: Why do we do System.currentTimeMillis() >> 10 & 0xFFFFFFFFFF?
 *   1. >> 10 loses the milliseconds portion of the time.  Well, a little
 *      more than 1000 ms (actually 1024).  This does mean that you can
 *      not restart a server in less than a second or else the old
 *      instance and the new instance can collide.
 *   2. & 0xFFFFFFFFFF reduces the overall size to fit it into 5 bytes.  This
 *      does impose the roughly 35000 years limitation because by then the
 *      value will overflow.  Arithmetic for arriving at that limitation
 *      is posted here for your review.
 * 
 *      2^(5*8) / 60 (s/m) / 60 (m/h) / 60 (h/d) / 365 (d/y) = 34865 years
 **/
public class UUID {

    static final long serialVersionUID = SerialVersionUID.UUID;

    static byte[] s_addr;
    static long s_currentTime = System.currentTimeMillis() >> 10;
    static int s_counter = 0;
    static {
        byte[] addr = MacAddress.getMacAddress().toByteArray();
        byte version = (byte)0x80;
        byte instance = (byte)0x1;
        version |= instance;

        s_addr = new byte[7];
        s_addr[0] = version;

        for (int i = 0; i < addr.length; i++) {
            s_addr[i + 1] = addr[i];
        }
    }

    private byte[] _uuid;

    public UUID() {
        int counter;
        long currentTime;
        synchronized(UUID.class) {
            currentTime = s_currentTime;
            counter = ++s_counter;
            if (s_counter == Integer.MAX_VALUE) {
                s_counter = 0;
                s_currentTime = System.currentTimeMillis() >> 10;
            }
        }
        _uuid = new byte[16];
        System.arraycopy(s_addr, 0, _uuid, 0, s_addr.length);
        _uuid[7] = (byte)((currentTime >> 32) & 0xFF);
        _uuid[8] = (byte)((currentTime >> 24) & 0xFF);
        _uuid[9] = (byte)((currentTime >> 16) & 0xFF);
        _uuid[10] = (byte)((currentTime >> 8) & 0xFF);
        _uuid[11] = (byte)((currentTime >> 0) & 0xFF);
        _uuid[12] = (byte)((counter >> 24) & 0xFF);
        _uuid[13] = (byte)((counter >> 16) & 0xFF);
        _uuid[14] = (byte)((counter >> 8) & 0xFF);
        _uuid[15] = (byte)((counter >> 0) & 0xFF);
    }

    /**
     * Copy constructor for UUID. Values of the given UUID are copied.
     *
     * @param u the UUID, may not be <code>null</code>
     */
    public UUID(UUID u) {
        _uuid = new byte[u._uuid.length];
        System.arraycopy(u._uuid, 0, _uuid, 0, u._uuid.length);
    }

    // This is really for testing purposes only.
    protected UUID(int counter) {
        long currentTime;
        synchronized(UUID.class) {
            s_counter = counter;
            s_counter++;
            currentTime = s_currentTime;
            if (s_counter == Integer.MAX_VALUE) {
                s_counter = 0;
                s_currentTime = System.currentTimeMillis() >> 10;
            }
        }
        _uuid = new byte[16];
        System.arraycopy(s_addr, 0, _uuid, 0, s_addr.length);
        _uuid[7] = (byte)((currentTime >> 32) & 0xFF);
        _uuid[8] = (byte)((currentTime >> 24) & 0xFF);
        _uuid[9] = (byte)((currentTime >> 16) & 0xFF);
        _uuid[10] = (byte)((currentTime >> 8) & 0xFF);
        _uuid[11] = (byte)((currentTime >> 0) & 0xFF);
        _uuid[12] = (byte)((counter >> 24) & 0xFF);
        _uuid[13] = (byte)((counter >> 16) & 0xFF);
        _uuid[14] = (byte)((counter >> 8) & 0xFF);
        _uuid[15] = (byte)((counter >> 0) & 0xFF);
    }

    public final byte[] getBytes() {
        return _uuid;
    }

    public final void writeTo(ByteBuffer buff) {
        buff.put(_uuid);
    }

    /**
     * Return an encoded string.
     * @param options the string should be encoded using this option.
     **/
    public String toString(int options) {
        return Base64.encodeBytes(getBytes(), options);
    }

    /**
     * Return an encoded string that is url safe.
     **/
    public String toUrlSafeString() {
        return Base64.encodeBytes(getBytes(), Base64.URL_SAFE);
    }

    /**
     * To override the toString() method.
     **/
    @Override
	public String toString() {
        return toUrlSafeString();
    }
    
    public int compareTo(Object o) {
        if (this == o) {
            return 0;
        }
        if (!(o instanceof UUID)) {
            throw new ClassCastException(
                    "The argument must be of type '" + getClass().getName() + "'.");
        }
        long time = NumbersUtil.bytesToLong(_uuid, 12);
        UUID t = (UUID) o;
        long ttime = NumbersUtil.bytesToLong(t._uuid, 12);

        if (time > ttime) {
            return 1;
        }
        if (time < ttime) {
            return -1;
        }
        return 0;
    }

    /**
     * Tweaked Serialization routine.
     * 
     * @param out the ObjectOutputStream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.write(_uuid);
    }

    /**
     * Tweaked Serialization routine.
     * 
     * @param in the ObjectInputStream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        _uuid = new byte[16];
        in.read(_uuid, 0, _uuid.length);
    }

    /**
     * Appends a String representation of this to the given {@link StringBuffer} or
     * creates a new one if none is given.
     * 
     * @param in the StringBuffer to append to, may be <code>null</code>
     * @return a StringBuffer
     */
    public StringBuffer toStringBuffer(StringBuffer in) {
        return in.append(toString());
    }

    /**
     * Returns a hash code of this UUID. The hash code is calculated by XOR'ing the
     * upper 32 bits of the time and clockSeqAndNode fields and the lower 32 bits of
     * the time and clockSeqAndNode fields.
     * 
     * @return an <code>int</code> representing the hash code
     * @see java.lang.Object#hashCode()
     */
    @Override
	public int hashCode() {
        return (int)NumbersUtil.bytesToLong(_uuid, 12);
    }

    /**
     * Clones this UUID.
     * 
     * @return a new UUID with identical values, never <code>null</code>
     */
    @Override
	public Object clone() {
        return new UUID(this);
    }

    /**
     * Compares two Objects for equality.
     * 
     * @see java.lang.Object#equals(Object)
     * @param obj the Object to compare this UUID with, may be <code>null</code>
     * @return <code>true</code> if the other Object is equal to this UUID,
     * <code>false</code> if not
     */
    @Override
	public boolean equals(Object obj) {
        if (!(obj instanceof UUID)) {
            return false;
        }
        return compareTo(obj) == 0;
    }

    public static void main(String[] args) {
        UUID uuid1 = new UUID();
        System.out.println("1 = " + NumbersUtil.bytesToString(uuid1.getBytes(), 0,  uuid1.getBytes().length));
        UUID uuid2 = new UUID();
        System.out.println("2 = " + NumbersUtil.bytesToString(uuid2.getBytes(), 0,  uuid1.getBytes().length));

        long start = System.currentTimeMillis();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            UUID uuid = new UUID();
        }
        long end = System.currentTimeMillis();
        System.out.println("That took " + (end - start));

        uuid1 = new UUID();
        System.out.println("1 now = " + NumbersUtil.bytesToString(uuid1.getBytes(), 0,  uuid1.getBytes().length));
    }
}
