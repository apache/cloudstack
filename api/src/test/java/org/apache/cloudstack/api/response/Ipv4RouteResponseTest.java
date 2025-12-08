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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class Ipv4RouteResponseTest {

    private static String subnet = "10.10.10.0/24";
    private static String gateway = "10.10.10.1";

    @Test
    public void testIpv4RouteResponse() {
        final Ipv4RouteResponse response = new Ipv4RouteResponse(subnet, gateway);

        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(gateway, response.getGateway());
    }

    @Test
    public void testIpv4RouteResponse2() {
        final Ipv4RouteResponse response = new Ipv4RouteResponse();

        response.setSubnet(subnet);
        response.setGateway(gateway);

        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(gateway, response.getGateway());
    }
}
