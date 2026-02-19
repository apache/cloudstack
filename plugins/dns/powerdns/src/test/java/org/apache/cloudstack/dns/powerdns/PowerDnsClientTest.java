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

package org.apache.cloudstack.dns.powerdns;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.InjectMocks;

public class PowerDnsClientTest {
    @InjectMocks
    PowerDnsClient client = new PowerDnsClient();

    @Test
    public void testNormalizeApexRecord() {
        String result = client.normalizeRecordName("@", "example.com");
        assertEquals("example.com.", result);

        result = client.normalizeRecordName("", "example.com");
        assertEquals("example.com.", result);
    }

    @Test
    public void testNormalizeRelativeRecord() {
        String result = client.normalizeRecordName("www", "example.com");
        assertEquals("www.example.com.", result);

        result = client.normalizeRecordName("WWW", "example.com"); // test case-insensitive
        assertEquals("www.example.com.", result);
    }

    @Test
    public void testNormalizeAbsoluteRecordWithinZone() {
        String result = client.normalizeRecordName("www.example.com.", "example.com");
        assertEquals("www.example.com.", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeAbsoluteRecordOutsideZoneThrows() {
        client.normalizeRecordName("other.com.", "example.com");
    }

    @Test
    public void testNormalizeDottedNameWithoutTrailingDot() {
        String result = client.normalizeRecordName("api.test.com", "example.com");
        assertEquals("api.test.com.", result);
    }

    @Test
    public void testNormalizeRelativeSubdomain() {
        String result = client.normalizeRecordName("mail", "example.com");
        assertEquals("mail.example.com.", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeNullRecordNameThrows() {
        client.normalizeRecordName(null, "example.com");
    }

    @Test
    public void testNormalizeZoneNormalization() {
        String result = client.normalizeRecordName("www", "Example.Com");
        assertEquals("www.example.com.", result);

        result = client.normalizeRecordName("www", "example.com.");
        assertEquals("www.example.com.", result);
    }

}