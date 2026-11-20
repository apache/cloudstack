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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Test;

public class ManagementServerNodeTest {

    private static final String FQDN_SYS_PROP = "cloudstack.msid.from.fqdn";

    @After
    public void tearDown() {
        System.clearProperty(FQDN_SYS_PROP);
    }

    private static boolean invokeIsTruthy(String value) throws Exception {
        Method m = ManagementServerNode.class.getDeclaredMethod("isTruthy", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, value);
    }

    private static boolean invokeIsFqdnModeEnabled() throws Exception {
        Method m = ManagementServerNode.class.getDeclaredMethod("isFqdnModeEnabled");
        m.setAccessible(true);
        return (boolean) m.invoke(null);
    }

    private static long invokeGenerateIdFromFqdn() throws Exception {
        Method m = ManagementServerNode.class.getDeclaredMethod("generateIdFromFqdn");
        m.setAccessible(true);
        return (long) m.invoke(null);
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
    public void testIsTruthyRecognizesTrueValues() throws Exception {
        assertTrue(invokeIsTruthy("true"));
        assertTrue(invokeIsTruthy("TRUE"));
        assertTrue(invokeIsTruthy("True"));
        assertTrue(invokeIsTruthy("1"));
        assertTrue(invokeIsTruthy("yes"));
        assertTrue(invokeIsTruthy("YES"));
    }

    @Test
    public void testIsTruthyTrimsWhitespace() throws Exception {
        assertTrue(invokeIsTruthy("  true  "));
        assertTrue(invokeIsTruthy("\t1\n"));
        assertTrue(invokeIsTruthy(" yes "));
    }

    @Test
    public void testIsTruthyRejectsFalseValues() throws Exception {
        assertFalse(invokeIsTruthy(null));
        assertFalse(invokeIsTruthy(""));
        assertFalse(invokeIsTruthy("   "));
        assertFalse(invokeIsTruthy("false"));
        assertFalse(invokeIsTruthy("0"));
        assertFalse(invokeIsTruthy("no"));
        assertFalse(invokeIsTruthy("enabled"));
        assertFalse(invokeIsTruthy("2"));
    }

    @Test
    public void testIsFqdnModeEnabledDefaultsFalse() throws Exception {
        System.clearProperty(FQDN_SYS_PROP);
        assertFalse(invokeIsFqdnModeEnabled());
    }

    @Test
    public void testIsFqdnModeEnabledWhenSystemPropertyTruthy() throws Exception {
        System.setProperty(FQDN_SYS_PROP, "true");
        assertTrue(invokeIsFqdnModeEnabled());
    }

    @Test
    public void testIsFqdnModeEnabledWhenSystemPropertyFalsy() throws Exception {
        System.setProperty(FQDN_SYS_PROP, "false");
        assertFalse(invokeIsFqdnModeEnabled());
    }

    @Test
    public void testGenerateIdFromFqdnIsPositiveAndNonZero() throws Exception {
        long id = invokeGenerateIdFromFqdn();
        assertTrue("Generated id must be positive and non-zero", id > 0);
    }

    @Test
    public void testGenerateIdFromFqdnFitsIn48Bits() throws Exception {
        long id = invokeGenerateIdFromFqdn();
        assertTrue("Generated id must fit within 48 bits", id <= 0xFFFFFFFFFFFFL);
    }

    @Test
    public void testGenerateIdFromFqdnIsDeterministic() throws Exception {
        long first = invokeGenerateIdFromFqdn();
        long second = invokeGenerateIdFromFqdn();
        assertEquals("Generated id must be stable across calls for the same FQDN", first, second);
    }
}
