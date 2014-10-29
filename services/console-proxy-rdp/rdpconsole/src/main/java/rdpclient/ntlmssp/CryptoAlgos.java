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
package rdpclient.ntlmssp;

import java.lang.reflect.Method;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import rdpclient.rdp.RdpConstants;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc236717.aspx
 */
public class CryptoAlgos implements NtlmConstants {

    /**
     * Indicates the left-to-right concatenation of the string parameters, from
     * the first string to the Nnth. Any numbers are converted to strings and all
     * numeric conversions to strings retain all digits, even nonsignificant ones.
     * The result is a string. For example, ConcatenationOf(0x00122, "XYZ",
     * "Client") results in the string "00122XYZClient."
     */
    public static String concatenationOf(String... args) {
        StringBuffer sb = new StringBuffer();
        for (String arg : args) {
            sb.append(arg);
        }
        return sb.toString();
    }

    /**
     * Concatenate byte arrays.
     */
    public static byte[] concatenationOf(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        byte[] result = new byte[length];
        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, destPos, array.length);
            destPos += array.length;
        }

        return result;
    }

    /** Indicates a 32-bit CRC calculated over m. */
    public static byte[] CRC32(byte[] m) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the encryption of an 8-byte data item d with the 7-byte key k
     * using the Data Encryption Standard (DES) algorithm in Electronic Codebook
     * (ECB) mode. The result is 8 bytes in length ([FIPS46-2]).
     */
    public static byte[] DES(byte[] k, byte[] d) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the encryption of an 8-byte data item D with the 16-byte key K
     * using the Data Encryption Standard Long (DESL) algorithm. The result is 24
     * bytes in length. DESL(K, D) is computed as follows.
     *
     * <pre>
     *   ConcatenationOf( DES(K[0..6], D),
     *     DES(K[7..13], D), DES(
     *       ConcatenationOf(K[14..15], Z(5)), D));
     * </pre>
     *
     * Note K[] implies a key represented as a character array.
     */
    public static byte[] DESL(byte[] k, byte[] d) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * An auxiliary function that returns an operating system version-specific
     * value (section 2.2.2.8).
     */
    public static byte[] getVersion() {
        // Version (6.1, Build 7601), NTLM current revision: 15
        return new byte[] {0x06, 0x01, (byte)0xb1, 0x1d, 0x00, 0x00, 0x00, 0x0f};
    }

    /**
     * Retrieve the user's LM response key from the server database (directory or
     * local database).
     */
    public static byte[] LMGETKEY(byte[] u, byte[] d) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /** Retrieve the user's NT response key from the server database. */
    public static byte[] NTGETKEY(byte[] u, byte[] d) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the encryption of data item m with the key k using the HMAC
     * algorithm ([RFC2104]).
     */
    public static byte[] HMAC(byte[] k, byte[] m) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the computation of a 16-byte HMAC-keyed MD5 message digest of the
     * byte string m using the key k.
     */
    public static byte[] HMAC_MD5(byte[] k, byte[] m) {
        try {
            String algorithm = "HMacMD5";
            Mac hashMac = Mac.getInstance(algorithm);

            Key secretKey = new SecretKeySpec(k, 0, k.length, algorithm);
            hashMac.init(secretKey);
            return hashMac.doFinal(m);
        } catch (Exception e) {
            throw new RuntimeException("Cannot calculate HMAC-MD5.", e);
        }
    }

    /**
     * Produces a key exchange key from the session base key K, LM response and
     * server challenge SC as defined in the sections KXKEY, SIGNKEY, and SEALKEY.
     */
    public static byte[] KXKEY(byte[] sessionBaseKey/*K, byte[] LM, byte[] SC*/) {
        // Key eXchange Key is server challenge
        /* In NTLMv2, KeyExchangeKey is the 128-bit SessionBaseKey */
        return Arrays.copyOf(sessionBaseKey, sessionBaseKey.length);
    }

    /**
     * Computes a one-way function of the user's password to use as the response
     * key. NTLM v1 and NTLM v2 define separate LMOWF() functions in the NTLM v1
     * authentication and NTLM v2 authentication sections, respectively.
     */
    public static byte[] LMOWF() {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the computation of an MD4 message digest of the null-terminated
     * byte string m ([RFC1320]).
     */
    public static byte[] MD4(byte[] m) {
        try {
            return sun.security.provider.MD4.getInstance().digest(m);
        } catch (Exception e) {
            throw new RuntimeException("Cannot calculate MD5.", e);
        }
    }

    /**
     * Indicates the computation of an MD5 message digest of the null-terminated
     * byte string m ([RFC1321]).
     */
    public static byte[] MD5(byte[] m) {
        try {
            return MessageDigest.getInstance("MD5").digest(m);
        } catch (Exception e) {
            throw new RuntimeException("Cannot calculate MD5.", e);
        }
    }

    /**
     * Indicates the computation of an MD5 message digest of a binary blob
     * ([RFC4121] section 4.1.1.2).
     */
    public static byte[] MD5_HASH(byte[] m) {
        try {
            return MessageDigest.getInstance("MD5").digest(m);
        } catch (Exception e) {
            throw new RuntimeException("Cannot calculate MD5.", e);
        }
    }

    /** A zero-length string. */
    public static final String NIL = "";

    /**
     * Indicates the computation of an n-byte cryptographic-strength random
     * number.
     *
     * Note The NTLM Authentication Protocol does not define the statistical
     * properties of the random number generator. It is left to the discretion of
     * the implementation to define the strength requirements of the NONCE(n)
     * operation.
     */
    public static byte[] NONCE(int n) {
        // Generate random nonce for LMv2 and NTv2 responses
        byte[] nonce = new byte[n];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        // Fixed nonce for debugging purposes
        //* DEBUG */for (int i = 0; i < N; i++) nonce[i] = (byte) (i + 1);

        return nonce;
    }

    /**
     * Computes a one-way function of the user's password to use as the response
     * key. NTLM v1 and NTLM v2 define separate NTOWF() functions in the NTLM v1
     * authentication and NTLM v2 authentication sections, respectively.
     */
    public static byte[] NTOWF() {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * The RC4 Encryption Algorithm. To obtain this stream cipher that is licensed
     * by RSA Data Security, Inc., contact this company.
     *
     * Indicates the encryption of data item d with the current session or message
     * key state, using the RC4 algorithm. h is the handle to a key state
     * structure initialized by RC4INIT.
     */
    public static byte[] RC4(Cipher h, byte[] d) {
        return h.update(d);
    }

    /**
     * Indicates the encryption of data item d with the key k using the RC4
     * algorithm.
     *
     * Note The key sizes for RC4 encryption in NTLM are defined in sections
     * KXKEY, SIGNKEY, and SEALKEY, where they are created.
     */
    public static byte[] RC4K(byte[] k, byte[] d) {
        try {
            Cipher cipher = Cipher.getInstance("RC4");
            Key key = new SecretKeySpec(k, "RC4");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialization of the RC4 key and handle to a key state structure for the
     * session.
     */
    public static Cipher RC4Init(byte[] k) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Produces an encryption key from the session key as defined in sections
     * KXKEY, SIGNKEY, and SEALKEY.
     */
    public static byte[] SEALKEY(byte[] f, byte[] k, byte[] string1) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Produces a signing key from the session key as defined in sections KXKEY,
     * SIGNKEY, and SEALKEY.
     */
    public static byte[] SIGNKEY(int flag, byte[] k, byte[] string1) {
        throw new RuntimeException("FATAL: Not implemented.");
    }

    /**
     * Indicates the retrieval of the current time as a 64-bit value, represented
     * as the number of 100-nanosecond ticks elapsed since midnight of January
     * 1st, 1601 (UTC).
     */
    public static byte[] Currenttime() {
        // (current time + milliseconds from 1.01.1601 to 1.01.1970) *
        // 100-nanosecond ticks
        long time = (System.currentTimeMillis() + 11644473600000L) * 10000;

        // Convert 64bit value to byte array.
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++, time >>>= 8) {
            result[i] = (byte)time;
        }

        return result;
    }

    /**
     * Indicates the 2-byte little-endian byte order encoding of the Unicode
     * UTF-16 representation of string. The Byte Order Mark (BOM) is not sent over
     * the wire.
     */
    public static byte[] UNICODE(String string) {
        return string.getBytes(RdpConstants.CHARSET_16);
    }

    /** Indicates the uppercase representation of string. */
    public static String UpperCase(String string) {
        return string.toUpperCase();
    }

    /**
     * Indicates the creation of a byte array of length N. Each byte in the array
     * is initialized to the value zero.
     */
    public static byte[] Z(int n) {
        return new byte[n];
    }

    public static Cipher initRC4(byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("RC4");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "RC4"));
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize RC4 sealing handle with client sealing key.", e);
        }
    }

    /**
     * Helper method for embedded test cases.
     */
    public static void callAll(Object obj) {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("test")) {
                try {
                    m.invoke(obj, (Object[])null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String args[]) {
        callAll(new CryptoAlgos());
    }

}
