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
package org.apache.cloudstack.utils.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.Test;

public class ManagementServerNodeTest {

    private static String invokeTrimToNull(String value) throws Exception {
        Method m = ManagementServerNode.class.getDeclaredMethod("trimToNull", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, value);
    }

    @Test
    public void testGetManagementServerIdIsPositive() {
        assertTrue("Node id must be a positive, non-zero value", ManagementServerNode.getManagementServerId() > 0);
    }

    @Test
    public void testCheckPassesWithValidNodeId() {
        // Node id is derived at class-load time and should be valid, so check() must not throw.
        new ManagementServerNode().check();
    }

    @Test
    public void testStartReturnsTrueWithValidNodeId() {
        // With a valid node id, start() must succeed and return true without terminating the JVM.
        assertTrue(new ManagementServerNode().start());
    }

    @Test
    public void testHashNodeIdentityIsDeterministicAndFitsInMacAddressRange() {
        long first = ManagementServerNode.hashNodeIdentity("cloudstack-mgmt-0.cloudstack-mgmt.cloudstack-mgmt.svc.cluster.local");
        long second = ManagementServerNode.hashNodeIdentity("cloudstack-mgmt-0.cloudstack-mgmt.cloudstack-mgmt.svc.cluster.local");

        System.out.println("First hash: " + first);
        System.out.println("Second hash: " + second);

        assertEquals(first, second);
        assertTrue(first > 0);
        assertTrue(first <= 0xFFFFFFFFFFFFL);
    }

    @Test
    public void testHashNodeIdentityUsesFirstSixSha256Bytes() throws Exception {
        String identity = "cloudstack-mgmt-0.cloudstack-mgmt.cloudstack-mgmt.svc.cluster.local";
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
        long expected = 0;
        for (int i = 0; i < 6; i++) {
            expected = (expected << 8) | (hash[i] & 0xFFL);
        }

        assertEquals(expected, ManagementServerNode.hashNodeIdentity(identity));
    }

    @Test
    public void testTrimToNullReturnsTrimmedValue() throws Exception {
        assertEquals("management.example.test", invokeTrimToNull("  management.example.test \n"));
    }

    @Test
    public void testTrimToNullReturnsNullForNullAndBlankValues() throws Exception {
        assertEquals(null, invokeTrimToNull(null));
        assertEquals(null, invokeTrimToNull(" \t\n "));
    }
}
