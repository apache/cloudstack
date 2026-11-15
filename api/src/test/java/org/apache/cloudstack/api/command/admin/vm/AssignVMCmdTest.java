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

package org.apache.cloudstack.api.command.admin.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import org.apache.cloudstack.api.ResponseGenerator;

public class AssignVMCmdTest {

    @Test
    public void getNetworkIdsReturnsPlainNetworkIdsWhenNoNicNetworksList() {
        AssignVMCmd cmd = new AssignVMCmd();
        List<Long> networkIds = Arrays.asList(1L, 2L);
        ReflectionTestUtils.setField(cmd, "networkIds", networkIds);

        assertEquals(networkIds, cmd.getNetworkIds());
    }

    @Test
    public void getNetworkIdsThrowsWhenBothNicNetworksListAndNetworkIdsGiven() {
        AssignVMCmd cmd = new AssignVMCmd();
        ReflectionTestUtils.setField(cmd, "networkIds", Arrays.asList(1L));
        Map<String, Map<String, String>> nicNetworksList = new LinkedHashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put("networkids", "some-network-uuid");
        nicNetworksList.put("0", entry);
        ReflectionTestUtils.setField(cmd, "nicNetworksList", nicNetworksList);

        assertThrows(InvalidParameterValueException.class, cmd::getNetworkIds);
    }

    @Test
    public void getNetworkIdsReturnsPrimaryNetworkIdsFromNicNetworksList() {
        AssignVMCmd cmd = new AssignVMCmd();
        NetworkService networkService = mock(NetworkService.class);
        cmd._networkService = networkService;

        Network primaryNetwork = mock(Network.class);
        when(primaryNetwork.getId()).thenReturn(55L);
        when(networkService.getNetwork("primary-uuid")).thenReturn(primaryNetwork);
        Network associatedNetwork = mock(Network.class);
        when(associatedNetwork.getId()).thenReturn(66L);
        when(networkService.getNetwork("associated-uuid")).thenReturn(associatedNetwork);

        Map<String, Map<String, String>> nicNetworksList = new LinkedHashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put("networkids", "primary-uuid,associated-uuid");
        nicNetworksList.put("0", entry);
        ReflectionTestUtils.setField(cmd, "nicNetworksList", nicNetworksList);

        assertEquals(Arrays.asList(55L), cmd.getNetworkIds());
    }

    @Test
    public void getEntityOwnerIdReturnsVmAccountWhenVmExists() {
        AssignVMCmd cmd = new AssignVMCmd();
        ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
        cmd._responseGenerator = responseGenerator;
        ReflectionTestUtils.setField(cmd, "virtualMachineId", 7L);
        UserVm vm = mock(UserVm.class);
        when(vm.getAccountId()).thenReturn(42L);
        when(responseGenerator.findUserVmById(7L)).thenReturn(vm);

        assertEquals(42L, cmd.getEntityOwnerId());
    }

    @Test
    public void getEntityOwnerIdFallsBackToSystemAccountWhenVmMissing() {
        AssignVMCmd cmd = new AssignVMCmd();
        ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
        cmd._responseGenerator = responseGenerator;
        ReflectionTestUtils.setField(cmd, "virtualMachineId", 7L);
        when(responseGenerator.findUserVmById(7L)).thenReturn(null);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void test_setSkipNetwork_default() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        Object value = ReflectionTestUtils.getField(assignVMCmd, "skipNetwork");
        Assert.assertTrue(value instanceof Boolean);
        Assert.assertFalse((Boolean) value);
    }

    @Test
    public void test_setSkipNetwork_set() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        assignVMCmd.setSkipNetwork(true);
        Object value = ReflectionTestUtils.getField(assignVMCmd, "skipNetwork");
        Assert.assertTrue(value instanceof Boolean);
        Assert.assertTrue((Boolean) value);
    }

    @Test
    public void test_isSkipNetwork_default() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        Assert.assertFalse(assignVMCmd.isSkipNetwork());
    }

    @Test
    public void test_isSkipNetwork_set() {
        AssignVMCmd assignVMCmd = new AssignVMCmd();
        ReflectionTestUtils.setField(assignVMCmd, "skipNetwork", true);
        Assert.assertTrue(assignVMCmd.isSkipNetwork());
    }

    private void runAsCallerOfType(Account.Type type, Runnable action) {
        Account caller = mock(Account.class);
        when(caller.getType()).thenReturn(type);
        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(caller);
        try (MockedStatic<CallContext> callContextMock = Mockito.mockStatic(CallContext.class)) {
            callContextMock.when(CallContext::current).thenReturn(callContext);
            action.run();
        }
    }

    private AssignVMCmd buildCmdWithRawNicNetworksListDropped() {
        AssignVMCmd cmd = new AssignVMCmd();
        Map<String, String> rawParams = new HashMap<>();
        rawParams.put("nicnetworkslist[0].networkids", "1,2");
        cmd.setFullUrlParams(rawParams);
        return cmd;
    }

    @Test
    public void getNicNetworksListRejectsNonRootAdminCaller() {
        AssignVMCmd cmd = buildCmdWithRawNicNetworksListDropped();
        runAsCallerOfType(Account.Type.DOMAIN_ADMIN, () -> {
            PermissionDeniedException thrown = assertThrows(PermissionDeniedException.class, cmd::getNicNetworksList);
            assertTrue(thrown.getMessage().contains(ApiConstants.NIC_NETWORKS_LIST));
        });
    }

    @Test
    public void getNetworkIdsRejectsNonRootAdminCaller() {
        AssignVMCmd cmd = buildCmdWithRawNicNetworksListDropped();
        runAsCallerOfType(Account.Type.DOMAIN_ADMIN, () -> assertThrows(PermissionDeniedException.class, cmd::getNetworkIds));
    }

    @Test
    public void getNicNetworksListAllowsRootAdminCaller() {
        AssignVMCmd cmd = buildCmdWithRawNicNetworksListDropped();
        runAsCallerOfType(Account.Type.ADMIN, () -> assertTrue(cmd.getNicNetworksList().isEmpty()));
    }
}
