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

package com.cloud.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class NuageVspUtilTest {

    @Test
    public void testEncodePassword() {
        String password = "Password!@#$%^&*()-_{}?><";
        String expectedEncodedPassword = "UGFzc3dvcmQhQCMkJV4mKigpLV97fT8+PA==";
        String encodedPassword = NuageVspUtil.encodePassword(password);
        assertEquals(expectedEncodedPassword, encodedPassword);
    }

    @Test
    public void testDecodePassword() {
        String password = "UGFzc3dvcmQhQCMkJV4mKigpLV97fT8+PA==";
        String expectedDecodedPassword = "Password!@#$%^&*()-_{}?><";
        String decodedPassword = NuageVspUtil.decodePassword(password);
        assertEquals(expectedDecodedPassword, decodedPassword);
    }
}
