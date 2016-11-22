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

package com.cloud.utils;


import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SwiftUtilTest {

    @Test
    public void testCalculateRFC2104HMAC() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        String inputData = "testData";
        String inputKey  = "testKey";
        String expected  = "1d541ecb5cdb2d850716bfd55585e20a1cd8984b";
        String output = SwiftUtil.calculateRFC2104HMAC(inputData, inputKey);

        assertEquals(expected, output);
    }

    @Test
    public void testToHexString(){
        final byte[] input = "testing".getBytes();
        final String expected = "74657374696e67";
        final String result = SwiftUtil.toHexString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGenerateTempUrl() {

        SwiftUtil.SwiftClientCfg cfg = Mockito.mock(SwiftUtil.SwiftClientCfg.class);
        when(cfg.getEndPoint()).thenReturn("http://localhost:8080/v1/");
        when(cfg.getAccount()).thenReturn("test");

        String container = "testContainer";
        String object = "testObject";
        String tempKey = "testKey";
        int urlExpirationInterval = 3600;
        String expected = "http://localhost:8080/v1/AUTH_test/testContainer/testObject";
        URL output = SwiftUtil.generateTempUrl(cfg, container, object, tempKey, urlExpirationInterval);

        assertTrue(output.toString().contains(expected));
    }

    @Test
    public void testSplitSwiftPath(){
        String input = "container" + File.separator + "object";
        String[] output = SwiftUtil.splitSwiftPath(input);
        String[] expected = {"container", "object"};

        assertArrayEquals(expected, output);
    }

    @Test
    public void testGetContainerName(){

        String inputType = "Template";
        long inputId = 45;
        String output = SwiftUtil.getContainerName(inputType, inputId);
        String expected = "T-45";

        assertEquals(expected, output);
    }
}
