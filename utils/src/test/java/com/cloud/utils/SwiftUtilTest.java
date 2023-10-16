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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.utils.SwiftUtil.SwiftClientCfg;

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

    @Test
    public void testGetSwiftCmd() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String cmd = SwiftUtil.getSwiftCmd(cfg, "swift", "stat");

        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword stat";
        assertThat(cmd, is(equalTo(expected)));
    }

    @Test
    public void testGetSwiftObjectCmd() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String objectCmd = SwiftUtil.getSwiftObjectCmd(cfg, "swift", "delete", "T-123", "template.vhd");

        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword delete T-123 template.vhd";
        assertThat(objectCmd, is(equalTo(expected)));
    }

    @Test
    public void testGetSwiftContainerCmd() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String containerCmd = SwiftUtil.getSwiftContainerCmd(cfg, "swift", "list", "T-123");

        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword list T-123";
        assertThat(containerCmd, is(equalTo(expected)));
    }

    @Test
    public void testGetUploadCmd() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String uploadCmd = SwiftUtil.getUploadObjectCommand(cfg, "swift", "T-1", "template.vhd", 1024);
        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword upload T-1 template.vhd";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    @Test
    public void testGetUploadCmdWithSegmentsBecauseOfSize() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String uploadCmd = SwiftUtil.getUploadObjectCommand(cfg, "swift", "T-1", "template.vhd", 5368709121L);

        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword upload T-1 template.vhd -S 5368709120";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    @Test
    public void testGetUploadCmdWithStoragePolicy() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg("policy1");

        String uploadCmd = SwiftUtil.getUploadObjectCommand(cfg, "swift", "T-1", "template.vhd", 1024L);
        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword upload T-1 template.vhd --storage-policy \"policy1\"";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    @Test
    public void testGetUploadCmdWithSegmentsAndStoragePolicy() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg("policy1");
        String uploadCmd = SwiftUtil.getUploadObjectCommand(cfg, "swift", "T-1", "template.vhd", 5368709121L);
        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword upload T-1 template.vhd --storage-policy \"policy1\" -S 5368709120";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    @Test
    public void testListContainerCmdWithStoragePolicyButNotSupportedByOperation() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg("policy1");

        String uploadCmd = SwiftUtil.getSwiftContainerCmd(cfg, "swift", "list", "T-1");
        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword list T-1";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    @Test
    public void testListContainerCmdWithoutStoragePolicy() {
        SwiftClientCfg cfg = CreateMockSwiftClientCfg(null);

        String uploadCmd = SwiftUtil.getSwiftContainerCmd(cfg, "swift", "list", "T-1");
        String expected = "/usr/bin/python swift -A swift.endpoint -U cs:sec-storage -K mypassword list T-1";
        assertThat(uploadCmd, is(equalTo(expected)));
    }

    private SwiftClientCfg CreateMockSwiftClientCfg(String policy){
        SwiftClientCfg cfg = mock(SwiftClientCfg.class);
        given(cfg.getEndPoint()).willReturn("swift.endpoint");
        given(cfg.getAccount()).willReturn("cs");
        given(cfg.getUserName()).willReturn("sec-storage");
        given(cfg.getKey()).willReturn("mypassword");
        given(cfg.getStoragePolicy()).willReturn(policy);
        return cfg;
    }
}
