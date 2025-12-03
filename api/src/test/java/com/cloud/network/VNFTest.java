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
package com.cloud.network;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class VNFTest {

    static long deviceId = 0L;
    static String deviceName = "eth0";
    static boolean required = true;
    static boolean management = false;
    static String description = "description of vnf nic";

    @Before
    public void setUp() {
    }

    @Test
    public void testAccessMethods() {
        Assert.assertEquals(VNF.AccessMethod.CONSOLE, VNF.AccessMethod.fromValue("console"));
        Assert.assertEquals(VNF.AccessMethod.HTTP, VNF.AccessMethod.fromValue("http"));
        Assert.assertEquals(VNF.AccessMethod.HTTPS, VNF.AccessMethod.fromValue("https"));
        Assert.assertEquals(VNF.AccessMethod.SSH_WITH_KEY, VNF.AccessMethod.fromValue("ssh-key"));
        Assert.assertEquals(VNF.AccessMethod.SSH_WITH_PASSWORD, VNF.AccessMethod.fromValue("ssh-password"));
    }

    @Test
    public void testVnfNic() {
        VNF.VnfNic vnfNic = new VNF.VnfNic(deviceId, deviceName, required, management, description);

        Assert.assertEquals(deviceId, vnfNic.getDeviceId());
        Assert.assertEquals(deviceName, vnfNic.getName());
        Assert.assertEquals(required, vnfNic.isRequired());
        Assert.assertEquals(management, vnfNic.isManagement());
        Assert.assertEquals(description, vnfNic.getDescription());
    }
}
