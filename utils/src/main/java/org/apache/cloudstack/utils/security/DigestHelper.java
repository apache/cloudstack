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
package org.apache.cloudstack.utils.security;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DigestHelper {

    public static ChecksumValue digest(String algorithm, InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        ChecksumValue checksum = null;
        byte[] buffer = new byte[8192];
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] md5sum = digest.digest();
        // TODO make sure this is valid for all types of checksums !?!
        BigInteger bigInt = new BigInteger(1, md5sum);
        checksum = new ChecksumValue(digest.getAlgorithm(), getPaddedDigestString(digest,bigInt));
        return checksum;
    }

    public static boolean check(String checksum, InputStream is) throws IOException, NoSuchAlgorithmException {
        ChecksumValue toCheckAgainst = new ChecksumValue(checksum);
        String algorithm = toCheckAgainst.getAlgorithm();
        ChecksumValue result = digest(algorithm,is);
        return result.equals(toCheckAgainst);
    }

    public static String getPaddedDigest(String algorithm, String inputString) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        String checksum;
        digest.reset();
        BigInteger pwInt = new BigInteger(1, digest.digest(inputString.getBytes()));
        return getPaddedDigestString(digest, pwInt);
    }

    private static String getPaddedDigestString(MessageDigest digest, BigInteger pwInt) {
        String checksum;
        String pwStr = pwInt.toString(16);
        // we have half byte string representation, so
        int padding = 2*digest.getDigestLength() - pwStr.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < padding; i++) {
            sb.append('0'); // make sure the MD5 password is 32 digits long
        }
        sb.append(pwStr);
        checksum = sb.toString();
        return checksum;
    }

    static final Map<String, Integer> paddingLengths = creatPaddingLengths();

    private static final Map<String, Integer> creatPaddingLengths() {
        Map<String, Integer> map = new HashMap<>();
        map.put("MD5", 32);
        map.put("SHA-1", 40);
        map.put("SHA-224", 56);
        map.put("SHA-256", 64);
        map.put("SHA-384", 96);
        map.put("SHA-512", 128);
        return map;
    }

    public static boolean isAlgorithmSupported(String checksum) {
        ChecksumValue toCheckAgainst = new ChecksumValue(checksum);
        String algorithm = toCheckAgainst.getAlgorithm();
        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        return true;
    }
}
