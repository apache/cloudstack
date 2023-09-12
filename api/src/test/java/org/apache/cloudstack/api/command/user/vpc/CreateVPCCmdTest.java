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

package org.apache.cloudstack.api.command.user.vpc;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkService;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcService;
import com.cloud.user.AccountService;
import com.cloud.utils.db.EntityManager;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.VpcResponse;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
public class CreateVPCCmdTest extends TestCase {

    @Mock
    public VpcService _vpcService;
    @Mock
    public EntityManager _entityMgr;
    @Mock
    public AccountService _accountService;
    private ResponseGenerator responseGenerator;
    @InjectMocks
    CreateVPCCmd cmd = new CreateVPCCmd() {
        @Override
        public Long getEntityId() {
            return 2L;
        }
    };

    public void testGetAccountName() {
        String accountName = "admin";
        ReflectionTestUtils.setField(cmd, "accountName", accountName);
        Assert.assertEquals(cmd.getAccountName(), accountName);
    }

    public void testGetDomainId() {
        Long domainId = 1L;
        ReflectionTestUtils.setField(cmd, "domainId", domainId);
        Assert.assertEquals(cmd.getDomainId(), domainId);
    }

    public void testGetZoneId() {
        Long zoneId = 1L;
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        Assert.assertEquals(cmd.getZoneId(), zoneId);
    }

    public void testGetVpcName() {
        String vpcName = "vpcNet";
        ReflectionTestUtils.setField(cmd, "vpcName", vpcName);
        Assert.assertEquals(cmd.getVpcName(), vpcName);
    }

    public void testGetCidr() {
        String cidr = "10.0.0.0/8";
        ReflectionTestUtils.setField(cmd, "cidr", cidr);
        Assert.assertEquals(cmd.getCidr(), cidr);
    }

    public void testGetDisplayText() {
        String displayText = "VPC Network";
        ReflectionTestUtils.setField(cmd, "displayText", displayText);
        Assert.assertEquals(cmd.getDisplayText(), displayText);
    }

    public void testGetVpcOffering() {
        Long vpcOffering = 1L;
        ReflectionTestUtils.setField(cmd, "vpcOffering", vpcOffering);
        Assert.assertEquals(cmd.getVpcOffering(), vpcOffering);
    }

    public void testGetNetworkDomain() {
        String netDomain = "cs1cloud.internal";
        ReflectionTestUtils.setField(cmd, "networkDomain", netDomain);
        Assert.assertEquals(cmd.getNetworkDomain(), netDomain);
    }

    public void testGetPublicMtuWhenNotSet() {
        Integer publicMtu = null;
        ReflectionTestUtils.setField(cmd, "publicMtu", publicMtu);
        Assert.assertEquals(NetworkService.DEFAULT_MTU, cmd.getPublicMtu());
    }

    public void testGetPublicMtuWhenSet() {
        Integer publicMtu = 1450;
        ReflectionTestUtils.setField(cmd, "publicMtu", publicMtu);
        Assert.assertEquals(cmd.getPublicMtu(), publicMtu);
    }

    public void testIsStartWhenNull() {
        Boolean start = null;
        ReflectionTestUtils.setField(cmd, "start", start);
        Assert.assertTrue(cmd.isStart());
    }

    public void testIsStartWhenValidValuePassed() {
        Boolean start = true;
        ReflectionTestUtils.setField(cmd, "start", start);
        Assert.assertTrue(cmd.isStart());
    }

    public void testGetDisplayVpc() {
        Boolean display = true;
        ReflectionTestUtils.setField(cmd, "display", display);
        Assert.assertTrue(cmd.getDisplayVpc());
    }

    public void testGetDisplayTextWhenEmpty() {
        String netName = "net-vpc";
        ReflectionTestUtils.setField(cmd, "vpcName", netName);
        Assert.assertEquals(cmd.getDisplayText(), netName);
    }

    public void testCreate() throws ResourceAllocationException {
        Vpc vpc = Mockito.mock(Vpc.class);
        ReflectionTestUtils.setField(cmd, "zoneId", 1L);
        ReflectionTestUtils.setField(cmd, "vpcOffering", 1L);
        ReflectionTestUtils.setField(cmd, "vpcName", "testVpc");
        ReflectionTestUtils.setField(cmd, "displayText", "Test Vpc Network");
        ReflectionTestUtils.setField(cmd, "cidr", "10.0.0.0/8");
        ReflectionTestUtils.setField(cmd, "networkDomain", "cs1cloud.internal");
        ReflectionTestUtils.setField(cmd, "display", true);
        ReflectionTestUtils.setField(cmd, "publicMtu", 1450);
        Mockito.when(_accountService.finalyzeAccountId(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(1L);
        Mockito.when(cmd.getEntityOwnerId()).thenReturn(1L);
        Mockito.when(_vpcService.createVpc(Mockito.any(CreateVPCCmd.class))).thenReturn(vpc);
        cmd.create();
        Mockito.verify(_vpcService, Mockito.times(1)).createVpc(Mockito.any(CreateVPCCmd.class));
    }

    public void testExecute() throws ResourceUnavailableException, InsufficientCapacityException {
        ReflectionTestUtils.setField(cmd, "start", true);
        Vpc vpc = Mockito.mock(Vpc.class);
        VpcResponse response = Mockito.mock(VpcResponse.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        Mockito.when(_vpcService.startVpc(1L, true)).thenReturn(true);
        Mockito.when(_entityMgr.findById(Mockito.eq(Vpc.class), Mockito.any(Long.class))).thenReturn(vpc);
        cmd._responseGenerator = responseGenerator;
        Mockito.when(responseGenerator.createVpcResponse(ResponseObject.ResponseView.Restricted, vpc)).thenReturn(response);
        cmd.execute();
        Mockito.verify(_vpcService, Mockito.times(1)).startVpc(Mockito.anyLong(), Mockito.anyBoolean());
    }
}
