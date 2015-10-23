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
package org.apache.cloudstack.api.response;

import junit.framework.TestCase;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class HostResponseTest extends TestCase {

    private static final String VALID_KEY = "validkey";
    private static final String VALID_VALUE = "validvalue";

    @Test
    public void testSetDetailsNull() {

        final HostResponse hostResponse = new HostResponse();
        hostResponse.setDetails(null);

        assertEquals(null, hostResponse.getDetails());

    }

    @Test
    public void testSetDetailsWithRootCredentials() {

        final HostResponse hostResponse = new HostResponse();
        final Map details = new HashMap<>();

        details.put(VALID_KEY, VALID_VALUE);
        details.put("username", "test");
        details.put("password", "password");

        final Map expectedDetails = new HashedMap();
        expectedDetails.put(VALID_KEY, VALID_VALUE);

        hostResponse.setDetails(details);
        final Map actualDetails = hostResponse.getDetails();

        assertTrue(details != actualDetails);
        assertEquals(expectedDetails, actualDetails);

    }

    @Test
    public void testSetDetailsWithoutRootCredentials() {

        final HostResponse hostResponse = new HostResponse();
        final Map details = new HashMap<>();

        details.put(VALID_KEY, VALID_VALUE);

        final Map expectedDetails = new HashedMap();
        expectedDetails.put(VALID_KEY, VALID_VALUE);

        hostResponse.setDetails(details);
        final Map actualDetails = hostResponse.getDetails();

        assertTrue(details != actualDetails);
        assertEquals(expectedDetails, actualDetails);

    }
}
