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

package org.apache.cloudstack.api.command.user.network;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.EntityManager;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
public class CreateNetworkCmdTest extends TestCase {

    @Mock
    public EntityManager _entityMgr;
    @Mock
    public NetworkService networkService;
    private ResponseGenerator responseGenerator;

    @InjectMocks
    CreateNetworkCmd cmd = new CreateNetworkCmd();
    public void testGetNetworkOfferingId() {
        Long networkOfferingId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        Assert.assertEquals(cmd.getNetworkOfferingId(), networkOfferingId);
    }

    public void testGetGateway() {
        String gateway = "10.10.10.1";
        ReflectionTestUtils.setField(cmd, "gateway", gateway);
        Assert.assertEquals(cmd.getGateway(), gateway);
    }

    public void testGetIsolatedPvlan() {
        String isolatedPvlan = "1234";
        ReflectionTestUtils.setField(cmd, "isolatedPvlan", isolatedPvlan);
        Assert.assertEquals(cmd.getIsolatedPvlan(), isolatedPvlan);
    }

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

    public void testGetNetmask() {
        String netmask = "255.255.255.0";
        ReflectionTestUtils.setField(cmd, "netmask", netmask);
        Assert.assertEquals(cmd.getNetmask(), netmask);
    }

    public void testGetStartIp() {
        String startIp = "10.10.10.2";
        ReflectionTestUtils.setField(cmd, "startIp", startIp);
        Assert.assertEquals(cmd.getStartIp(), startIp);
    }

    public void testGetEndIp() {
        String endIp = "10.10.10.10";
        ReflectionTestUtils.setField(cmd, "endIp", endIp);
        Assert.assertEquals(cmd.getEndIp(), endIp);
    }

    public void testGetNetworkName() {
        String netName = "net-isolated";
        ReflectionTestUtils.setField(cmd, "name", netName);
        Assert.assertEquals(cmd.getNetworkName(), netName);
    }

    public void testGetDisplayTextWhenNotEmpty() {
        String description = "Isolated Network";
        ReflectionTestUtils.setField(cmd, "displayText", description);
        Assert.assertEquals(cmd.getDisplayText(), description);
    }

    public void testGetDisplayTextWhenEmpty() {
        String description = null;
        String netName = "net-isolated";
        ReflectionTestUtils.setField(cmd, "name", netName);
        Assert.assertEquals(cmd.getDisplayText(), netName);
    }

    public void testGetNetworkDomain() {
        String netDomain = "cs1cloud.internal";
        ReflectionTestUtils.setField(cmd, "networkDomain", netDomain);
        Assert.assertEquals(cmd.getNetworkDomain(), netDomain);
    }

    public void testGetProjectId() {
        Long projectId = 1L;
        ReflectionTestUtils.setField(cmd, "projectId", projectId);
        Assert.assertEquals(cmd.getProjectId(), projectId);
    }

    public void testGetAclType() {
        String aclType = "account";
        ReflectionTestUtils.setField(cmd, "aclType", aclType);
        Assert.assertEquals(cmd.getAclType(), aclType);
    }

    public void testGetSubdomainAccess() {
        Boolean subDomAccess = false;
        ReflectionTestUtils.setField(cmd, "subdomainAccess", subDomAccess);
        Assert.assertEquals(cmd.getSubdomainAccess(), subDomAccess);
    }

    public void testGetVpcId() {
        Long vpcId = 1L;
        ReflectionTestUtils.setField(cmd, "vpcId", vpcId);
        Assert.assertEquals(cmd.getVpcId(), vpcId);
    }

    public void testGetDisplayNetwork() {
        Boolean displayNet = true;
        ReflectionTestUtils.setField(cmd, "displayNetwork", displayNet);
        Assert.assertEquals(cmd.getDisplayNetwork(), displayNet);
    }

    public void testGetExternalId() {
        String externalId = "1";
        ReflectionTestUtils.setField(cmd, "externalId", externalId);
        Assert.assertEquals(cmd.getExternalId(), externalId);
    }

    public void testGetAssociatedNetworkId() {
        Long associatedNetId = 1L;
        ReflectionTestUtils.setField(cmd, "associatedNetworkId", associatedNetId);
        Assert.assertEquals(cmd.getAssociatedNetworkId(), associatedNetId);
    }

    public void testIsDisplayNullDefaultsToTrue() {
        Boolean displayNetwork = null;
        ReflectionTestUtils.setField(cmd, "displayNetwork", displayNetwork);
        Assert.assertTrue(cmd.isDisplay());
    }

    public void testGetPhysicalNetworkIdForInvalidNetOfferingId() {
        Long physicalNetworkId = 1L;

        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, 1L)).thenReturn(null);
        try {
            cmd.getPhysicalNetworkId();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().startsWith("Unable to find network offering by ID"));
        }
    }

    public void testGetPhysicalNetworkIdForInvalidAssociatedNetId() {
        Long physicalNetworkId = 1L;
        Long networkOfferingId = 1L;
        Long associatedNetworkId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        ReflectionTestUtils.setField(cmd, "associatedNetworkId", associatedNetworkId);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(_entityMgr.findById(Network.class, associatedNetworkId)).thenReturn(null);
        try {
            cmd.getPhysicalNetworkId();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().startsWith("Unable to find network by ID"));
        }
    }

    public void testGetPhysicalNetworkIdForAssociatedNetIdForNonSharedNet() {
        Long physicalNetworkId = 1L;
        Long networkOfferingId = 1L;
        Long associatedNetworkId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        ReflectionTestUtils.setField(cmd, "associatedNetworkId", associatedNetworkId);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Network network = Mockito.mock(Network.class);
        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(_entityMgr.findById(Network.class, associatedNetworkId)).thenReturn(network);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        try {
            cmd.getPhysicalNetworkId();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().startsWith("Associated network ID can be specified for networks of guest IP type Shared only"));
        }
    }

    public void testGetPhysicalNetworkIdForNonSharedNet() {
        Long physicalNetworkId = 1L;
        Long networkOfferingId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        try {
            cmd.getPhysicalNetworkId();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().startsWith("Physical network ID can be specified for networks of guest IP type Shared only"));
        }
    }

    public void testGetPhysicalNetworkIdForSharedNet() {
        Long physicalNetworkId = 1L;
        Long networkOfferingId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Shared);
        try {
            Assert.assertEquals(cmd.getPhysicalNetworkId(), physicalNetworkId);
        } catch (Exception e) {
            Assert.fail("Failed to get physical network id");
        }
    }

    public void testGetZoneId() {
        Long physicalNetworkId = 1L;
        Long networkOfferingId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        ReflectionTestUtils.setField(cmd, "physicalNetworkId", physicalNetworkId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Shared);
        Long zoneId = 1L;
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        Assert.assertEquals(cmd.getZoneId(), zoneId);
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

    public void testGetPrivateMtuWhenNotSet() {
        Integer privateMtu = null;
        ReflectionTestUtils.setField(cmd, "privateMtu", privateMtu);
        Assert.assertEquals(NetworkService.DEFAULT_MTU, cmd.getPrivateMtu());
    }

    public void testGetPrivateMtuWhenSet() {
        Integer privateMtu = 1250;
        ReflectionTestUtils.setField(cmd, "privateMtu", privateMtu);
        Assert.assertEquals(cmd.getPrivateMtu(), privateMtu);
    }

    public void testExecute() throws InsufficientCapacityException, ResourceAllocationException {
        ReflectionTestUtils.setField(cmd, "displayText", "testNetwork");
        ReflectionTestUtils.setField(cmd, "name", "testNetwork");
        ReflectionTestUtils.setField(cmd, "networkOfferingId", 1L);
        ReflectionTestUtils.setField(cmd, "zoneId", 1L);
        Network createdNetwork = Mockito.mock(Network.class);
        NetworkResponse response = Mockito.mock(NetworkResponse.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        Mockito.when(networkService.createGuestNetwork(cmd)).thenReturn(createdNetwork);
        Mockito.when(responseGenerator.createNetworkResponse(ResponseObject.ResponseView.Restricted, createdNetwork)).thenReturn(response);
        cmd._responseGenerator = responseGenerator;

        try {
            cmd.execute();
            Mockito.verify(networkService, Mockito.times(1)).createGuestNetwork(cmd);
        } catch (Exception e) {
            System.out.println(e);
            Assert.fail("Should successfully create the network");
        }
    }
}
