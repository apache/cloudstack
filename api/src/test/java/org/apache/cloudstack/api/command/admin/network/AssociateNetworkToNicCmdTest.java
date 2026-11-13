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
package org.apache.cloudstack.api.command.admin.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.user.Account;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.Nic;

@RunWith(MockitoJUnitRunner.class)
public class AssociateNetworkToNicCmdTest {

    private static final long NIC_ID = 11L;
    private static final long INSTANCE_ID = 22L;
    private static final long ACCOUNT_ID = 33L;

    private EntityManager entityMgr;
    private NetworkService networkService;
    private ResponseGenerator responseGenerator;
    private AssociateNetworkToNicCmd cmd;

    @Before
    public void setUp() {
        entityMgr = mock(EntityManager.class);
        networkService = mock(NetworkService.class);
        responseGenerator = mock(ResponseGenerator.class);

        cmd = new AssociateNetworkToNicCmd();
        cmd._entityMgr = entityMgr;
        cmd._networkService = networkService;
        cmd._responseGenerator = responseGenerator;
        ReflectionTestUtils.setField(cmd, "nicId", NIC_ID);
    }

    @Test
    public void getEntityOwnerIdReturnsVmAccountWhenVmExists() {
        NicNetworkMapCmdTestFixtures.mockNic(entityMgr, NIC_ID, INSTANCE_ID);
        NicNetworkMapCmdTestFixtures.mockVm(entityMgr, INSTANCE_ID, ACCOUNT_ID);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdFallsBackToSystemAccountWhenVmMissing() {
        NicNetworkMapCmdTestFixtures.mockNic(entityMgr, NIC_ID, INSTANCE_ID);
        NicNetworkMapCmdTestFixtures.mockMissingVm(entityMgr, INSTANCE_ID);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdThrowsWhenNicMissing() {
        NicNetworkMapCmdTestFixtures.mockMissingNic(entityMgr, NIC_ID);

        assertThrows(InvalidParameterValueException.class, cmd::getEntityOwnerId);
    }

    @Test
    public void getApiResourceIdReturnsNicInstanceId() {
        NicNetworkMapCmdTestFixtures.mockNic(entityMgr, NIC_ID, INSTANCE_ID);

        assertEquals((Long) INSTANCE_ID, cmd.getApiResourceId());
    }

    @Test
    public void getIpAddressesMapReturnsEmptyMapWhenNoneProvided() {
        assertTrue(cmd.getIpAddressesMap().isEmpty());
    }

    @Test
    public void getIpAddressesMapThrowsWhenEntryMissingNetworkId() {
        Map<String, Map<String, String>> ipAddresses = new LinkedHashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put(ApiConstants.IP_ADDRESS, "10.0.0.5");
        ipAddresses.put("0", entry);
        ReflectionTestUtils.setField(cmd, "ipAddresses", ipAddresses);

        assertThrows(InvalidParameterValueException.class, cmd::getIpAddressesMap);
    }

    @Test
    public void getIpAddressesMapThrowsWhenNetworkUuidUnknown() {
        String unknownUuid = "unknown-network-uuid";
        Map<String, Map<String, String>> ipAddresses = new LinkedHashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put(ApiConstants.NETWORK_ID, unknownUuid);
        ipAddresses.put("0", entry);
        ReflectionTestUtils.setField(cmd, "ipAddresses", ipAddresses);
        when(entityMgr.findByUuid(Network.class, unknownUuid)).thenReturn(null);

        assertThrows(InvalidParameterValueException.class, cmd::getIpAddressesMap);
    }

    @Test
    public void getIpAddressesMapResolvesNetworkIdAndIps() {
        String networkUuid = "known-network-uuid";
        long networkId = 44L;
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(networkId);
        when(entityMgr.findByUuid(Network.class, networkUuid)).thenReturn(network);

        Map<String, Map<String, String>> ipAddresses = new LinkedHashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put(ApiConstants.NETWORK_ID, networkUuid);
        entry.put(ApiConstants.IP_ADDRESS, "10.0.0.5");
        entry.put(ApiConstants.IP6_ADDRESS, "fd00::5");
        ipAddresses.put("0", entry);
        ReflectionTestUtils.setField(cmd, "ipAddresses", ipAddresses);

        Map<Long, Network.IpAddresses> result = cmd.getIpAddressesMap();

        assertEquals(1, result.size());
        Network.IpAddresses resolved = result.get(networkId);
        assertEquals("10.0.0.5", resolved.getIp4Address());
        assertEquals("fd00::5", resolved.getIp6Address());
    }

    @Test
    public void executeBuildsResponseOnSuccess() throws Exception {
        NicNetworkMapCmdTestFixtures.mockNic(entityMgr, NIC_ID, INSTANCE_ID);
        Nic resultNic = mock(Nic.class);
        when(networkService.associateNetworkToNic(cmd)).thenReturn(resultNic);
        NicResponse response = new NicResponse();
        when(responseGenerator.createNicResponse(resultNic)).thenReturn(response);

        cmd.execute();

        assertEquals(response, cmd.getResponseObject());
        assertEquals("associatenetworktonicresponse", response.getResponseName());
        assertEquals("nic", response.getObjectName());
    }

    @Test
    public void executeThrowsServerApiExceptionWhenServiceReturnsNull() throws Exception {
        NicNetworkMapCmdTestFixtures.mockNic(entityMgr, NIC_ID, INSTANCE_ID);
        when(networkService.associateNetworkToNic(cmd)).thenReturn(null);

        assertThrows(ServerApiException.class, cmd::execute);
    }
}
