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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UuidUtilsTest {

    @Test
    public void isUuidTestAllLowerCaseReturnTrue() {
        String serviceUuid = "f81a9aa3-1f7d-466f-b04b-f2b101486bae";
        assertTrue(UuidUtils.isUuid(serviceUuid));
    }

    @Test
    public void isUuidTestAllUpperCaseReturnTrue() {
        String serviceUuid = "F81A9AA3-1F7D-466F-B04B-F2B101486BAE";
        assertTrue(UuidUtils.isUuid(serviceUuid));
    }

    @Test
    public void isUuidTestAlternatingCaseReturnTrue() {
        String serviceUuid = "f81A9Aa3-1f7d-466F-B04b-f2b101486BaE";
        assertTrue(UuidUtils.isUuid(serviceUuid));
    }

    @Test
    public void isUuidTestNotUuidReturnFalse() {
        String serviceUuid = "6fc6ce7-d503-4f95-9e68-c9cd281b13df";
        assertFalse(UuidUtils.isUuid(serviceUuid));
    }
}
