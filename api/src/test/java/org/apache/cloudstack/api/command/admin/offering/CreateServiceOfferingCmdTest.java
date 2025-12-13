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

package org.apache.cloudstack.api.command.admin.offering;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.vm.lease.VMLeaseManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CreateServiceOfferingCmdTest {

    @InjectMocks
    private CreateServiceOfferingCmd createServiceOfferingCmd;

    @Test
    public void testGetDisplayTextWhenEmpty() {
        String netName = "net-offering";
        ReflectionTestUtils.setField(createServiceOfferingCmd, "serviceOfferingName", netName);
        Assert.assertEquals(createServiceOfferingCmd.getDisplayText(), netName);
    }

    @Test
    public void testIsPurgeResourcesNoOrNullValue() {
        Assert.assertFalse(createServiceOfferingCmd.isPurgeResources());
        ReflectionTestUtils.setField(createServiceOfferingCmd, "purgeResources", false);
        Assert.assertFalse(createServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testIsPurgeResourcesFalse() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "purgeResources", false);
        Assert.assertFalse(createServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testIsPurgeResourcesTrue() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "purgeResources", true);
        Assert.assertTrue(createServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testGetLeaseDuration() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "leaseDuration", 10);
        Assert.assertEquals(10, createServiceOfferingCmd.getLeaseDuration().longValue());
    }

    @Test
    public void testGetLeaseExpiryAction() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "leaseExpiryAction", "stop");
        Assert.assertEquals(VMLeaseManager.ExpiryAction.STOP, createServiceOfferingCmd.getLeaseExpiryAction());

        ReflectionTestUtils.setField(createServiceOfferingCmd, "leaseExpiryAction", "DESTROY");
        Assert.assertEquals(VMLeaseManager.ExpiryAction.DESTROY, createServiceOfferingCmd.getLeaseExpiryAction());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLeaseExpiryActionInvalidValue() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "leaseExpiryAction", "Unknown");
        Assert.assertEquals(null, createServiceOfferingCmd.getLeaseExpiryAction());
    }

    @Test
    public void testGetVgpuProfileId() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "vgpuProfileId", 10L);
        Assert.assertEquals(10L, createServiceOfferingCmd.getVgpuProfileId().longValue());
    }

    @Test
    public void testGetGpuCount() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "gpuCount", 2);
        Assert.assertEquals(2, createServiceOfferingCmd.getGpuCount().intValue());
    }

    @Test
    public void testGetGpuDisplay() {
        ReflectionTestUtils.setField(createServiceOfferingCmd, "gpuDisplay", true);
        Assert.assertTrue(createServiceOfferingCmd.getGpuDisplay());
    }

}
