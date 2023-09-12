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



package org.apache.cloudstack.api.command.user.template;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.template.TemplateApiService;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class RegisterTemplateCmdTest {

    @InjectMocks
    private RegisterTemplateCmd registerTemplateCmd;

    @Mock
    public TemplateApiService _templateService;

    @Test
    public void testZoneidAndZoneIdListEmpty() throws ResourceAllocationException {
        try {
            registerTemplateCmd = new RegisterTemplateCmd();
            registerTemplateCmd.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Api should fail when both zoneid and zoneids aren't passed");
            }
        }
    }

    @Test
    public void testZoneidAndZoneIdListBothPresent() throws ResourceAllocationException {
        try {
            registerTemplateCmd = new RegisterTemplateCmd();
            registerTemplateCmd.zoneId = -1L;
            registerTemplateCmd.zoneIds = new ArrayList<>();
            registerTemplateCmd.zoneIds.add(-1L);

            registerTemplateCmd.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Api should fail when both zoneid and zoneids are passed");
            }
        }
    }


    @Test
    public void testZoneidMinusOne() throws ResourceAllocationException {
        // If zoneId is passed as -1, then zone ids list should be null.
        registerTemplateCmd = new RegisterTemplateCmd();
        registerTemplateCmd.zoneId = -1L;

        Assert.assertNull(registerTemplateCmd.getZoneIds());
    }

    @Test
    public void testZoneidListMinusOne() throws ResourceAllocationException {
        // If zoneId List has only one parameter -1, then zone ids list should be null.
        registerTemplateCmd = new RegisterTemplateCmd();
        registerTemplateCmd.zoneIds = new ArrayList<>();
        registerTemplateCmd.zoneIds.add(-1L);

        Assert.assertNull(registerTemplateCmd.getZoneIds());
    }
    @Test
    public void testZoneidListMoreThanMinusOne() throws ResourceAllocationException {
        try {
            registerTemplateCmd = new RegisterTemplateCmd();
            registerTemplateCmd.zoneIds = new ArrayList<>();
            registerTemplateCmd.zoneIds.add(-1L);
            registerTemplateCmd.zoneIds.add(1L);
            registerTemplateCmd.execute();
        } catch (ServerApiException e) {
            if (e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Parameter zoneids cannot combine all zones (-1) option with other zones");
            }
        }
    }
    @Test
    public void testZoneidPresentZoneidListAbsent() throws ResourceAllocationException {
            registerTemplateCmd = new RegisterTemplateCmd();
            registerTemplateCmd.zoneIds = null;
            registerTemplateCmd.zoneId = 1L;
            Assert.assertEquals((Long)1L,registerTemplateCmd.getZoneIds().get(0));
    }

    private void testIsDeployAsIsBase(Hypervisor.HypervisorType hypervisorType, Boolean deployAsIsParameter, boolean expectedResult) {
        registerTemplateCmd = new RegisterTemplateCmd();
        registerTemplateCmd.hypervisor = hypervisorType.name();
        registerTemplateCmd.deployAsIs = deployAsIsParameter;
        boolean isDeployAsIs = registerTemplateCmd.isDeployAsIs();
        Assert.assertEquals(expectedResult, isDeployAsIs);
    }

    @Test
    public void testIsDeployAsIsVmwareNullAsIs() {
        testIsDeployAsIsBase(Hypervisor.HypervisorType.VMware, null, false);
    }

    @Test
    public void testIsDeployAsIsVmwareNotAsIs() {
        testIsDeployAsIsBase(Hypervisor.HypervisorType.VMware, false, false);
    }

    @Test
    public void testIsDeployAsIsVmwareAsIs() {
        testIsDeployAsIsBase(Hypervisor.HypervisorType.VMware, true, true);
    }

    @Test
    public void testIsDeployAsIsNonVmware() {
        testIsDeployAsIsBase(Hypervisor.HypervisorType.KVM, true, false);
        testIsDeployAsIsBase(Hypervisor.HypervisorType.XenServer, true, false);
        testIsDeployAsIsBase(Hypervisor.HypervisorType.Any, true, false);
    }

    @Test
    public void testGetDisplayTextWhenEmpty() {
        registerTemplateCmd = new RegisterTemplateCmd();
        String netName = "net-template";
        ReflectionTestUtils.setField(registerTemplateCmd , "templateName", netName);
        Assert.assertEquals(registerTemplateCmd.getDisplayText(), netName);
    }
}
