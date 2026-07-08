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
package org.apache.cloudstack.api.command.user.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Test;

import com.cloud.network.Network;
import com.cloud.user.Account;

public class DisassociateDnsZoneFromNetworkCmdTest extends BaseDnsCmdTest {

    private static final long NETWORK_ID = 200L;

    private DisassociateDnsZoneFromNetworkCmd createCmd() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd =
                new DisassociateDnsZoneFromNetworkCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "_entityMgr", entityManager);
        setField(cmd, "networkId", NETWORK_ID);
        return cmd;
    }

    @Test
    public void testGetNetworkId() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        assertEquals(Long.valueOf(NETWORK_ID), cmd.getNetworkId());
    }

    @Test
    public void testGetEntityOwnerIdWithNetwork() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entityManager.findById(Network.class, NETWORK_ID))
                .thenReturn(mockNetwork);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetEntityOwnerIdNetworkNotFound() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        when(entityManager.findById(Network.class, NETWORK_ID))
                .thenReturn(null);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        when(dnsProviderManager.disassociateZoneFromNetwork(cmd))
                .thenReturn(true);

        cmd.execute();

        SuccessResponse response =
                (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        verify(dnsProviderManager).disassociateZoneFromNetwork(cmd);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsFalse() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        when(dnsProviderManager.disassociateZoneFromNetwork(cmd))
                .thenReturn(false);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        DisassociateDnsZoneFromNetworkCmd cmd = createCmd();
        when(dnsProviderManager.disassociateZoneFromNetwork(cmd))
                .thenThrow(new RuntimeException("Error"));
        cmd.execute();
    }
}
