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

package org.apache.cloudstack.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CustomActionResultResponseTest {

    private CustomActionResultResponse response;

    @Before
    public void setUp() {
        response = new CustomActionResultResponse();
    }

    @Test
    public void getResultReturnsNullByDefault() {
        assertNull(response.getResult());
    }

    @Test
    public void getResultReturnsSetValue() {
        Map<String, String> result = Map.of("message", "OK", "details", "All good");
        response.setResult(result);
        assertEquals(result, response.getResult());
        assertEquals("OK", response.getResult().get("message"));
    }

    @Test
    public void isSuccessReturnsFalseWhenSuccessIsNull() {
        // success is null by default
        assertFalse(response.isSuccess());
    }

    @Test
    public void isSuccessReturnsFalseWhenSuccessIsFalse() {
        response.setSuccess(false);
        assertFalse(response.isSuccess());
    }

    @Test
    public void isSuccessReturnsTrueWhenSuccessIsTrue() {
        response.setSuccess(true);
        assertTrue(response.isSuccess());
    }

    @Test
    public void getSuccessReturnsNullByDefault() {
        assertNull(response.getSuccess());
    }

    @Test
    public void getSuccessReturnsTrueAfterSetSuccessTrue() {
        response.setSuccess(true);
        assertTrue(response.getSuccess());
    }

    @Test
    public void getSuccessReturnsFalseAfterSetSuccessFalse() {
        response.setSuccess(false);
        assertFalse(response.getSuccess());
    }

    @Test
    public void setAndGetResultWithNullResult() {
        response.setResult(null);
        assertNull(response.getResult());
    }
}

