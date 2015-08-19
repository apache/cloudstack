//
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
//

package com.cloud.utils.net;

import static com.cloud.utils.AutoCloseableUtil.closeAutoCloseable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Formatter;

import org.apache.log4j.Logger;

import com.cloud.utils.NumbersUtil;

/**
 * copied from the public domain utility from John Burkard.
 * @author <a href="mailto:jb@eaio.com">Johann Burkard</a>
 * @version 2.1.3
 **/
public class MacAddress {
    private static final Logger s_logger = Logger.getLogger(MacAddress.class);
    private long _addr = 0;

    protected MacAddress() {
    }

    public MacAddress(long addr) {
        _addr = addr;
    }

    public long toLong() {
        return _addr;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[6];
        bytes[0] = (byte)((_addr >> 40) & 0xff);
        bytes[1] = (byte)((_addr >> 32) & 0xff);
        bytes[2] = (byte)((_addr >> 24) & 0xff);
        bytes[3] = (byte)((_addr >> 16) & 0xff);
        bytes[4] = (byte)((_addr >> 8) & 0xff);
        bytes[5] = (byte)((_addr >> 0) & 0xff);
        return bytes;
    }

    public String toString(String separator) {
        StringBuilder buff = new StringBuilder();
        Formatter formatter = new Formatter(buff);
        formatter.format("%02x%s%02x%s%02x%s%02x%s%02x%s%02x", _addr >> 40 & 0xff, separator, _addr >> 32 & 0xff, separator, _addr >> 24 & 0xff, separator,
            _addr >> 16 & 0xff, separator, _addr >> 8 & 0xff, separator, _addr & 0xff);
        return buff.toString();

        /*

        String str = Long.toHexString(_addr);

        for (int i = str.length() - 1; i >= 0; i--) {
            buff.append(str.charAt(i));
            if (separator != null && (str.length() - i) % 2 == 0) {
                buff.append(separator);
            }
        }
        return buff.reverse().toString();
         */
    }

    @Override
    public String toString() {
        return toString(":");
    }

    private static MacAddress s_address;
    static {
        String macAddress = null;

        Process p = null;
        BufferedReader in = null;

        try {
            String osname = System.getProperty("os.name");

            if (osname.startsWith("Windows")) {
                p = Runtime.getRuntime().exec(new String[] {"ipconfig", "/all"}, null);
            } else if (osname.startsWith("Solaris") || osname.startsWith("SunOS")) {
                // Solaris code must appear before the generic code
                String hostName = MacAddress.getFirstLineOfCommand(new String[] {"uname", "-n"});
                if (hostName != null) {
                    p = Runtime.getRuntime().exec(new String[] {"/usr/sbin/arp", hostName}, null);
                }
            } else if (new File("/usr/sbin/lanscan").exists()) {
                p = Runtime.getRuntime().exec(new String[] {"/usr/sbin/lanscan"}, null);
            } else if (new File("/sbin/ifconfig").exists()) {
                p = Runtime.getRuntime().exec(new String[] {"/sbin/ifconfig", "-a"}, null);
            }

            if (p != null) {
                in = new BufferedReader(new InputStreamReader(p.getInputStream()), 128);
                String l = null;
                while ((l = in.readLine()) != null) {
                    macAddress = MacAddress.parse(l);
                    if (macAddress != null) {
                        short parsedShortMacAddress = MacAddress.parseShort(macAddress);
                        if (parsedShortMacAddress != 0xff && parsedShortMacAddress != 0x00)
                            break;
                    }
                    macAddress = null;
                }
            }

        } catch (SecurityException ex) {
            s_logger.info("[ignored] security exception in static initializer of MacAddress", ex);
        } catch (IOException ex) {
            s_logger.info("[ignored] io exception in static initializer of MacAddress");
        } finally {
            if (p != null) {
                closeAutoCloseable(in, "closing init process input stream");
                closeAutoCloseable(p.getErrorStream(), "closing init process error output stream");
                closeAutoCloseable(p.getOutputStream(), "closing init process std output stream");
                p.destroy();
            }
        }

        long clockSeqAndNode = 0;

        if (macAddress != null) {
            if (macAddress.indexOf(':') != -1) {
                clockSeqAndNode |= MacAddress.parseLong(macAddress);
            } else if (macAddress.startsWith("0x")) {
                clockSeqAndNode |= MacAddress.parseLong(macAddress.substring(2));
            }
        } else {
            try {
                byte[] local = InetAddress.getLocalHost().getAddress();
                clockSeqAndNode |= (local[0] << 24) & 0xFF000000L;
                clockSeqAndNode |= (local[1] << 16) & 0xFF0000;
                clockSeqAndNode |= (local[2] << 8) & 0xFF00;
                clockSeqAndNode |= local[3] & 0xFF;
            } catch (UnknownHostException ex) {
                clockSeqAndNode |= (long)(Math.random() * 0x7FFFFFFF);
            }
        }

        s_address = new MacAddress(clockSeqAndNode);
    }

    public static MacAddress getMacAddress() {
        return s_address;
    }

    private static String getFirstLineOfCommand(String[] commands) throws IOException {

        Process p = null;
        BufferedReader reader = null;

        try {
            p = Runtime.getRuntime().exec(commands);
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()), 128);

            return reader.readLine();
        } finally {
            if (p != null) {
                closeAutoCloseable(reader, "closing process input stream");
                closeAutoCloseable(p.getErrorStream(), "closing process error output stream");
                closeAutoCloseable(p.getOutputStream(), "closing process std output stream");
                p.destroy();
            }
        }

    }

    /**
     * The MAC address parser attempts to find the following patterns:
     * <ul>
     * <li>.{1,2}:.{1,2}:.{1,2}:.{1,2}:.{1,2}:.{1,2}</li>
     * <li>.{1,2}-.{1,2}-.{1,2}-.{1,2}-.{1,2}-.{1,2}</li>
     * </ul>
     *
     * This is copied from the author below.  The author encouraged copying
     * it.
     *
     */
    static String parse(String in) {

        // lanscan

        int hexStart = in.indexOf("0x");
        if (hexStart != -1) {
            int hexEnd = in.indexOf(' ', hexStart);
            if (hexEnd != -1) {
                return in.substring(hexStart, hexEnd);
            }
        }

        int octets = 0;
        int lastIndex, old, end;

        if (in.indexOf('-') > -1) {
            in = in.replace('-', ':');
        }

        lastIndex = in.lastIndexOf(':');

        if (lastIndex > in.length() - 2)
            return null;

        end = Math.min(in.length(), lastIndex + 3);

        ++octets;
        old = lastIndex;
        while (octets != 5 && lastIndex != -1 && lastIndex > 1) {
            lastIndex = in.lastIndexOf(':', --lastIndex);
            if (old - lastIndex == 3 || old - lastIndex == 2) {
                ++octets;
                old = lastIndex;
            }
        }

        if (octets == 5 && lastIndex > 1) {
            return in.substring(lastIndex - 2, end).trim();
        }
        return null;
    }

    public static void main(String[] args) {
        MacAddress addr = MacAddress.getMacAddress();
        System.out.println("addr in integer is " + addr.toLong());
        System.out.println("addr in bytes is " + NumbersUtil.bytesToString(addr.toByteArray(), 0, addr.toByteArray().length));
        System.out.println("addr in char is " + addr.toString(":"));
    }

    /**
     * Parses a <code>long</code> from a hex encoded number. This method will skip
     * all characters that are not 0-9 and a-f (the String is lower cased first).
     * Returns 0 if the String does not contain any interesting characters.
     *
     * @param s the String to extract a <code>long</code> from, may not be <code>null</code>
     * @return a <code>long</code>
     * @throws NullPointerException if the String is <code>null</code>
     */
    public static long parseLong(String s) throws NullPointerException {
        s = s.toLowerCase();
        long out = 0;
        byte shifts = 0;
        char c;
        for (int i = 0; i < s.length() && shifts < 16; i++) {
            c = s.charAt(i);
            if ((c > 47) && (c < 58)) {
                out <<= 4;
                ++shifts;
                out |= c - 48;
            } else if ((c > 96) && (c < 103)) {
                ++shifts;
                out <<= 4;
                out |= c - 87;
            }
        }
        return out;
    }

    /**
     * Parses an <code>int</code> from a hex encoded number. This method will skip
     * all characters that are not 0-9 and a-f (the String is lower cased first).
     * Returns 0 if the String does not contain any interesting characters.
     *
     * @param s the String to extract an <code>int</code> from, may not be <code>null</code>
     * @return an <code>int</code>
     * @throws NullPointerException if the String is <code>null</code>
     */
    public static int parseInt(String s) throws NullPointerException {
        s = s.toLowerCase();
        int out = 0;
        byte shifts = 0;
        char c;
        for (int i = 0; i < s.length() && shifts < 8; i++) {
            c = s.charAt(i);
            if ((c > 47) && (c < 58)) {
                out <<= 4;
                ++shifts;
                out |= c - 48;
            } else if ((c > 96) && (c < 103)) {
                ++shifts;
                out <<= 4;
                out |= c - 87;
            }
        }
        return out;
    }

    /**
     * Parses a <code>short</code> from a hex encoded number. This method will skip
     * all characters that are not 0-9 and a-f (the String is lower cased first).
     * Returns 0 if the String does not contain any interesting characters.
     *
     * @param s the String to extract a <code>short</code> from, may not be <code>null</code>
     * @return a <code>short</code>
     * @throws NullPointerException if the String is <code>null</code>
     */
    public static short parseShort(String s) throws NullPointerException {
        s = s.toLowerCase();
        short out = 0;
        byte shifts = 0;
        char c;
        for (int i = 0; i < s.length() && shifts < 4; i++) {
            c = s.charAt(i);
            if ((c > 47) && (c < 58)) {
                out <<= 4;
                ++shifts;
                out |= c - 48;
            } else if ((c > 96) && (c < 103)) {
                ++shifts;
                out <<= 4;
                out |= c - 87;
            }
        }
        return out;
    }

    /**
     * Parses a <code>byte</code> from a hex encoded number. This method will skip
     * all characters that are not 0-9 and a-f (the String is lower cased first).
     * Returns 0 if the String does not contain any interesting characters.
     *
     * @param s the String to extract a <code>byte</code> from, may not be <code>null</code>
     * @return a <code>byte</code>
     * @throws NullPointerException if the String is <code>null</code>
     */
    public static byte parseByte(String s) throws NullPointerException {
        s = s.toLowerCase();
        byte out = 0;
        byte shifts = 0;
        char c;
        for (int i = 0; i < s.length() && shifts < 2; i++) {
            c = s.charAt(i);
            if ((c > 47) && (c < 58)) {
                out <<= 4;
                ++shifts;
                out |= c - 48;
            } else if ((c > 96) && (c < 103)) {
                ++shifts;
                out <<= 4;
                out |= c - 87;
            }
        }
        return out;
    }
}
