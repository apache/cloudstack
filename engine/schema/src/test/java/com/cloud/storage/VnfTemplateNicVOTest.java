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
package com.cloud.storage;

import org.junit.Assert;
import org.junit.Test;

public class VnfTemplateNicVOTest {

    static long templateId  = 100L;
    static long deviceId = 0L;
    static String deviceName = "eth0";
    static boolean required = true;
    static boolean management = false;
    static String description = "description of vnf nic";


    @Test
    public void testVnfTemplateNicVOProperties() {
        VnfTemplateNicVO nicVO = new VnfTemplateNicVO(templateId, deviceId, deviceName, required, management, description);

        Assert.assertEquals(templateId, nicVO.getTemplateId());
        Assert.assertEquals(deviceId, nicVO.getDeviceId());
        Assert.assertEquals(deviceName, nicVO.getDeviceName());
        Assert.assertEquals(required, nicVO.isRequired());
        Assert.assertEquals(management, nicVO.isManagement());
        Assert.assertEquals(description, nicVO.getDescription());

        String expected = String.format("Template {\"deviceId\":%d,\"id\":0,\"required\":%s,\"templateId\":%d}", deviceId, required, templateId);
        Assert.assertEquals(expected, nicVO.toString());
    }
}
