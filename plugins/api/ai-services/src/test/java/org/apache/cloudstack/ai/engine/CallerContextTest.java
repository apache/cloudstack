// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The caller context the engine filters every response by
 * (SPEC.md section 10).
 */
class CallerContextTest {

    @Test
    void rendersTheFourHeadersTheEngineExpects() {
        Map<String, String> headers =
                new CallerContext("u-1", "acme", "/Customers/Acme", "User").asHeaders();

        assertEquals(4, headers.size());
        assertEquals("u-1", headers.get("X-Aisvc-User-Id"));
        assertEquals("acme", headers.get("X-Aisvc-Account"));
        assertEquals("/Customers/Acme", headers.get("X-Aisvc-Domain"));
        assertEquals("User", headers.get("X-Aisvc-Role"));
    }

    @ParameterizedTest
    @CsvSource({
        ",acme,/d,User",
        "u-1,,/d,User",
        "u-1,acme,,User",
        "u-1,acme,/d,",
        "'  ',acme,/d,User",
    })
    void refusesAnIncompleteContext(
            String user, String account, String domain, String role) {
        // The engine trusts this assertion, so a half-built one must not
        // reach it: a blank domain would widen rather than narrow access.
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallerContext(user, account, domain, role));
    }

    @Test
    void theFailureNamesTheMissingField() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new CallerContext("u-1", "acme", null, "User"));

        assertTrue(failure.getMessage().contains("domainPath"));
    }

    @Test
    void equalContextsCompareEqual() {
        assertEquals(
                new CallerContext("u-1", "acme", "/d", "User"),
                new CallerContext("u-1", "acme", "/d", "User"));
    }

    @Test
    void adifferentAccountIsADifferentContext() {
        assertNotEquals(
                new CallerContext("u-1", "acme", "/d", "User"),
                new CallerContext("u-1", "other", "/d", "User"));
    }

    @Test
    void headerOrderIsStable() {
        Map<String, String> headers =
                new CallerContext("u", "a", "/d", "User").asHeaders();

        assertEquals(
                java.util.Arrays.asList(
                        "X-Aisvc-User-Id", "X-Aisvc-Account",
                        "X-Aisvc-Domain", "X-Aisvc-Role"),
                new java.util.ArrayList<>(headers.keySet()));
    }
}
