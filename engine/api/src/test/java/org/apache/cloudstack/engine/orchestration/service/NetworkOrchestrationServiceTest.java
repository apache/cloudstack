/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.orchestration.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class NetworkOrchestrationServiceTest {

    @Test
    public void parseUsageExemptCidrsTestNotSet() {
        Assert.assertTrue(NetworkOrchestrationService.parseUsageExemptCidrs(null).isEmpty());
        Assert.assertTrue(NetworkOrchestrationService.parseUsageExemptCidrs(" ").isEmpty());
    }

    @Test
    public void parseUsageExemptCidrsTestNormalisesEntries() {
        List<String> cidrs = NetworkOrchestrationService.parseUsageExemptCidrs("192.0.2.5, 198.51.100.70/24,,192.0.2.5/32");
        Assert.assertEquals(List.of("192.0.2.5/32", "198.51.100.0/24"), cidrs);
    }

    @Test
    public void parseUsageExemptCidrsTestNormalisesIpv6Entries() {
        List<String> cidrs = NetworkOrchestrationService.parseUsageExemptCidrs("2001:db8::5, 2001:db8:0:1::7/64,192.0.2.5");
        Assert.assertEquals(List.of("2001:db8::5/128", "2001:db8:0:1::/64", "192.0.2.5/32"), cidrs);
    }

    @Test
    public void parseUsageExemptCidrsTestSkipsInvalidEntries() {
        List<String> cidrs = NetworkOrchestrationService.parseUsageExemptCidrs("not-an-ip,0.0.0.0/0,::/0,10.0.0.0/33,192.168.1.300,172.16.0.0/12");
        Assert.assertEquals(List.of("172.16.0.0/12"), cidrs);
    }
}
