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
public final class VnfNicResponseTest {

    static long deviceId = 0L;
    static String deviceName = "eth0";
    static boolean required = true;
    static boolean management = false;
    static String description = "description of vnf nic";

    static String networkUuid = "networkuuid";
    static String networkName = "networkname";

    @Test
    public void testNewVnfNicResponse() {
        final VnfNicResponse response = new VnfNicResponse(deviceId, deviceName, required, management, description);
        Assert.assertEquals(deviceId, response.getDeviceId());
        Assert.assertEquals(deviceName, response.getName());
        Assert.assertEquals(required, response.isRequired());
        Assert.assertEquals(management, response.isManagement());
        Assert.assertEquals(description, response.getDescription());
    }

    @Test
    public void testSetVnfNicResponse() {
        final VnfNicResponse response = new VnfNicResponse();
        response.setNetworkId(networkUuid);
        response.setNetworkName(networkName);
        Assert.assertEquals(networkUuid, response.getNetworkId());
        Assert.assertEquals(networkName, response.getNetworkName());
    }
}
