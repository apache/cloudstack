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

import org.junit.Assert;
import org.junit.Test;

public class HMACSignUtilTest {

    @Test
    public void generateSignatureValidInputsReturnsExpectedSignature() throws Exception {
        String data = "CloudStack works!";
        String key = "Pj4pnwSUBZ4wQFXw2zWdVY1k5Ku9bIy70wCNG1DmS8keO7QapCLw2Axtgc2nEPYzfFCfB38ATNLt6caDqU2dSw";
        String result = "HYLWSII5Ap23WeSaykNsIo6mOhmV3d18s5p2cq2ebCA=";
        final String signature = HMACSignUtil.generateSignature(data, key);
        Assert.assertNotNull(signature);
        Assert.assertEquals(result, signature);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateSignatureInvalidKeyThrowsException() throws Exception {
        final String data = "testData";
        final String key = ""; // Empty key
        HMACSignUtil.generateSignature(data, key);
    }

    @Test
    public void generateSignatureDifferentInputsProduceDifferentSignatures() throws Exception {
        final String data1 = "testData1";
        final String data2 = "testData2";
        final String key = "testKey";
        final String signature1 = HMACSignUtil.generateSignature(data1, key);
        final String signature2 = HMACSignUtil.generateSignature(data2, key);
        Assert.assertNotEquals(signature1, signature2);
    }

    @Test
    public void generateSignatureSameInputsProduceSameSignature() throws Exception {
        final String data = "testData";
        final String key = "testKey";
        final String signature1 = HMACSignUtil.generateSignature(data, key);
        final String signature2 = HMACSignUtil.generateSignature(data, key);
        Assert.assertEquals(signature1, signature2);
    }
}
