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
import java.io.UnsupportedEncodingException;

import com.amazonaws.util.StringInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DigestHelperTest {

    private final static String INPUT_STRING = "01234567890123456789012345678901234567890123456789012345678901234567890123456789\n";
    private final static String INPUT_STRING_NO2 = "01234567890123456789012345678901234567890123456789012345678901234567890123456789b\n";
    private final static String INPUT_STRING_NO3 = "01234567890123456789012345678901234567890123456789012345678901234567890123456789h\n";
    private final static String SHA256_CHECKSUM = "{SHA-256}c6ab15af7842d23d3c06c138b53a7d09c5e351a79c4eb3c8ca8d65e5ce8900ab";
    private final static String SHA256_NO_PREFIX_CHECKSUM = "c6ab15af7842d23d3c06c138b53a7d09c5e351a79c4eb3c8ca8d65e5ce8900ab";
    private final static String SHA1_CHECKSUM = "{SHA-1}49e4b2f4292b63e88597c127d11bc2cc0f2ca0ff";
    private final static String MD5_CHECKSUM = "{MD5}d141a8eeaf6bba779d1d1dc5102a81c5";
    private final static String MD5_NO_PREFIX_CHECKSUM = "d141a8eeaf6bba779d1d1dc5102a81c5";
    private final static String ZERO_PADDED_MD5_CHECKSUM = "{MD5}0e51dfa74b87f19dd5e0124d6a2195e3";
    private final static String ZERO_PADDED_SHA256_CHECKSUM = "{SHA-256}08b5ae0c7d7d45d8ed406d7c3c7da695b81187903694314d97f8a37752a6b241";
    private static final String MD5 = "MD5";
    private static final String SHA_256 = "SHA-256";
    private static InputStream inputStream;
    private InputStream inputStream2;


    @Test
    public void check_SHA256() throws Exception {
        assertTrue(DigestHelper.check(SHA256_CHECKSUM, inputStream));
    }

    @Test
    public void check_SHA1() throws Exception {
        assertTrue(DigestHelper.check(SHA1_CHECKSUM, inputStream));
    }

    @Test
    public void check_MD5() throws Exception {
        assertTrue(DigestHelper.check(MD5_CHECKSUM, inputStream));
    }

    @Test
    public void testDigestSHA256() throws Exception {
        String result = DigestHelper.digest(SHA_256, inputStream).toString();
        Assert.assertEquals(SHA256_CHECKSUM, result);
    }

    @Test
    public void testDigestSHA1() throws Exception {
        String result = DigestHelper.digest("SHA-1", inputStream).toString();
        Assert.assertEquals(SHA1_CHECKSUM, result);
    }

    @Test
    public void testDigestMD5() throws Exception {
        String result = DigestHelper.digest(MD5, inputStream).toString();
        Assert.assertEquals(MD5_CHECKSUM, result);
    }

    @Test
    public void testZeroPaddedDigestMD5() throws Exception {
        inputStream2 = new StringInputStream(INPUT_STRING_NO2);
        String result = DigestHelper.digest(MD5, inputStream2).toString();
        Assert.assertEquals(ZERO_PADDED_MD5_CHECKSUM, result);
    }

    @Test
    public void testZeroPaddedDigestSHA256() throws Exception {
        inputStream2 = new StringInputStream(INPUT_STRING_NO3);
        String result = DigestHelper.digest(SHA_256, inputStream2).toString();
        Assert.assertEquals(ZERO_PADDED_SHA256_CHECKSUM, result);
    }

    @BeforeClass
    public static void init() throws UnsupportedEncodingException {
        inputStream = new StringInputStream(INPUT_STRING);
    }
    @Before
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChecksumSanityNoPrefixWrongAlgorithm() {
        DigestHelper.validateChecksumString(SHA256_NO_PREFIX_CHECKSUM);
    }

    @Test
    public void testChecksumSanityNoPrefix() {
        DigestHelper.validateChecksumString(MD5_NO_PREFIX_CHECKSUM);
    }

    @Test
    public void testChecksumSanityPrefixEmptyAlgorithm() {
        String checksum = "{}" + MD5_NO_PREFIX_CHECKSUM;
        DigestHelper.validateChecksumString(checksum);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChecksumSanityPrefixWrongAlgorithm() {
        String checksum = "{MD5}" + SHA256_NO_PREFIX_CHECKSUM;
        DigestHelper.validateChecksumString(checksum);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChecksumSanityPrefixWrongChecksumLength() {
        String checksum = SHA256_CHECKSUM + "XXXXX";
        DigestHelper.validateChecksumString(checksum);
    }

    @Test
    public void testIsAlgorithmPresentPositiveCase() {
        assertTrue(DigestHelper.isAlgorithmSupported(SHA256_CHECKSUM));
    }

    @Test
    public void testIsAlgorithmPresentnegativeCase() {
        assertTrue(DigestHelper.isAlgorithmSupported(SHA256_NO_PREFIX_CHECKSUM));
    }

    @Test
    public void testGetHashValueFromChecksumValuePrefixPresent() {
        String checksum = DigestHelper.getHashValueFromChecksumValue(SHA256_CHECKSUM);
        assertEquals(SHA256_NO_PREFIX_CHECKSUM, checksum);
    }

    @Test
    public void testGetHashValueFromChecksumValueNoPrefixPresent() {
        String checksum = DigestHelper.getHashValueFromChecksumValue(SHA256_NO_PREFIX_CHECKSUM);
        assertEquals(SHA256_NO_PREFIX_CHECKSUM, checksum);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme
