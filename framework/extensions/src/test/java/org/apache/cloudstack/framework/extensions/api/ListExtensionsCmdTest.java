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

package org.apache.cloudstack.framework.extensions.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;

public class ListExtensionsCmdTest {

    private ListExtensionsCmd cmd;

    @Before
    public void setUp() {
        cmd = new ListExtensionsCmd();
    }

    private void setPrivateField(String fieldName, Object value) {
        ReflectionTestUtils.setField(cmd, fieldName, value);
    }

    @Test
    public void testGetName() {
        String testName = "testExtension";
        setPrivateField("name", testName);
        assertEquals(testName, cmd.getName());
    }

    @Test
    public void testGetExtensionId() {
        Long testId = 123L;
        setPrivateField("extensionId", testId);
        assertEquals(testId, cmd.getExtensionId());
    }

    @Test
    public void testGetDetailsReturnsAllWhenNull() {
        setPrivateField("details", null);
        EnumSet<ApiConstants.ExtensionDetails> result = cmd.getDetails();
        assertEquals(EnumSet.of(ApiConstants.ExtensionDetails.all), result);
    }

    @Test
    public void testGetDetailsReturnsAllWhenEmpty() {
        setPrivateField("details", Collections.emptyList());
        EnumSet<ApiConstants.ExtensionDetails> result = cmd.getDetails();
        assertEquals(EnumSet.of(ApiConstants.ExtensionDetails.all), result);
    }

    @Test
    public void testGetDetailsWithValidValues() {
        List<String> detailsList = Arrays.asList("all", "resource");
        setPrivateField("details", detailsList);
        EnumSet<ApiConstants.ExtensionDetails> result = cmd.getDetails();
        assertTrue(result.contains(ApiConstants.ExtensionDetails.all));
        assertTrue(result.contains(ApiConstants.ExtensionDetails.resource));
        assertEquals(2, result.size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetDetailsWithInvalidValueThrowsException() {
        List<String> detailsList = Arrays.asList("invalidValue");
        setPrivateField("details", detailsList);
        cmd.getDetails();
    }
}
