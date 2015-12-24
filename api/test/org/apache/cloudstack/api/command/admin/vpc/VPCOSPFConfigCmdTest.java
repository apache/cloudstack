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

package org.apache.cloudstack.api.command.admin.vpc;

import java.lang.reflect.Field;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.utils.net.cidr.BadCIDRException;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class VPCOSPFConfigCmdTest extends TestCase {

    @Mock
    public VpcProvisioningService vpcProvSvc;

    @InjectMocks
    VPCOSPFConfigUpdateCmd vpcOSPFConfig = new VPCOSPFConfigUpdateCmd();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Field f = VPCOSPFConfigUpdateCmd.class.getDeclaredField("_vpcProvSvc");
        f.setAccessible(true);
        f.set(vpcOSPFConfig, vpcProvSvc);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
    }

    @Test
    public void testInvocation() throws IllegalArgumentException, IllegalAccessException, BadCIDRException {
        ReflectionTestUtils.setField(vpcOSPFConfig, ApiConstants.ZONE_ID, 1L);
        ReflectionTestUtils.setField(vpcOSPFConfig, "password", "sfkjsdkk123");
        ReflectionTestUtils.setField(vpcOSPFConfig, "superCIDR", "192.168.100.0/20");

        vpcOSPFConfig.execute();
        Mockito.verify(vpcProvSvc, Mockito.times(1)).updateQuaggaConfig(1L, null, null, null, null, null, null, null, "sfkjsdkk123", "192.168.100.0/20", null);
    }

}
