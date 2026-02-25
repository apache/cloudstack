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

package org.apache.cloudstack.dns;

import static org.apache.cloudstack.dns.DnsProviderUtil.appendPublicSuffixToZone;
import static org.apache.cloudstack.dns.DnsProviderUtil.normalizeDomain;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DnsProviderUtilTest {
    private final String userZoneName;
    private final String publicSuffix;
    private final String expectedResult;
    private final boolean expectException;

    public DnsProviderUtilTest(String userZoneName,
                               String publicSuffix,
                               String expectedResult,
                               boolean expectException) {
        this.userZoneName = userZoneName;
        this.publicSuffix = publicSuffix;
        this.expectedResult = expectedResult;
        this.expectException = expectException;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"tenant1.com", "example.com", "tenant1.example.com", false},
                {"dev.tenant2.com", "example.com", "dev.tenant2.example.com", false},
                {"tenant3.example.com", "example.com", "tenant3.example.com", false},
                {"Tenant1.CoM", "ExAmple.CoM", "tenant1.example.com", false},
                {"tenant1.com.", "example.com.", "tenant1.example.com", false},
                {"tenant1.com", "", "tenant1.com", false},
                {"tenant1.com", null, "tenant1.com", false},
                {"test.abc.com", "abc.com", "test.abc.com", false},
                {"sub.test.abc.com", "abc.com", "sub.test.abc.com", false},
                {"test.ai.abc.com", "abc.com", "test.ai.abc.com", false},
                {"deep.sub.abc.com", "abc.com", "deep.sub.abc.com", false},
                {"abc.com", "xyz.com", "abc.xyz.com", false},
                {"test.xyz.com", "xyz.com", "test.xyz.com", false},
                {"test.com.xyz.com", "xyz.com", "test.com.xyz.com", false},
                {"tenant", "example.com", null, true}, // single label
                {"test", "abc.com", null, true},
                {"example.com.", "example.com", null, true},
                {"example.com", "example.com", null, true}, // root level forbidden
                {"abc.com", "abc.com", null, true},   // root level forbidden
                {"tenant1.org", "example.com", null, true}, // TLD mismatch
                {"test.ai", "abc.com", null, true}, // TLD mismatch
                {null, "example.com", null, true},
        });
    }

    @Test
    public void testAppendPublicSuffix() {
        if (expectException) {
            try {
                executeAppendSuffixTest(userZoneName, publicSuffix);
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException ignored) {
                // noop
            }
        } else {
            String result;
            if (Strings.isNotBlank(publicSuffix)) {
                result = executeAppendSuffixTest(userZoneName, publicSuffix);
            } else {
                result = appendPublicSuffixToZone(normalizeDomain(userZoneName), publicSuffix);
            }
            assertEquals(expectedResult, result);
        }
    }

    String executeAppendSuffixTest(String zoneName, String domainSuffix) {
        return appendPublicSuffixToZone(normalizeDomain(zoneName), domainSuffix);
    }
}
