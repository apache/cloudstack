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
package org.apache.cloudstack.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class PrometheusExporterServerImplTest {

    private final PrometheusExporterServerImpl server = new PrometheusExporterServerImpl();

    @Test
    public void testNormalizeAddressesIpv6LoopbackForms() {
        Set<String> addresses = server.normalizeAddresses("::1");
        assertEquals(server.normalizeAddresses("0:0:0:0:0:0:0:1"), addresses);
        assertTrue(addresses.contains("0:0:0:0:0:0:0:1"));
    }

    @Test
    public void testNormalizeAddressesTrimsEntries() {
        Set<String> addresses = server.normalizeAddresses(" 127.0.0.1 , 10.1.1.1");
        assertEquals(Set.of("127.0.0.1", "10.1.1.1"), addresses);
    }

    @Test
    public void testNormalizeAddressesIgnoresBlankEntries() {
        assertTrue(server.normalizeAddresses("").isEmpty());
        assertEquals(Set.of("10.1.1.1"), server.normalizeAddresses(",10.1.1.1,, ,"));
    }

    @Test
    public void testNormalizeAddressesIgnoresNonIpEntries() {
        assertEquals(Set.of("10.1.1.1"), server.normalizeAddresses("localhost,10.1.1.1,not-an-ip"));
    }
}
