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
package org.apache.cloudstack.api.command.user.vpn;

import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.vpn.Site2SiteVpnService;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VpnConnectionLifecycleCmdTest {

    private static final Long CONNECTION_ID = 1L;
    private static final Long VPN_GATEWAY_ID = 2L;
    private static final Long VPC_ID = 3L;

    @Mock
    private EntityManager entityManager;
    @Mock
    private Site2SiteVpnService vpnService;
    @Mock
    private Site2SiteVpnConnection connection;
    @Mock
    private Site2SiteVpnGateway gateway;

    private ResetVpnConnectionCmd resetCmd;
    private DeleteVpnConnectionCmd deleteCmd;
    private DeleteVpnGatewayCmd deleteGatewayCmd;

    @Before
    public void setUp() {
        resetCmd = new ResetVpnConnectionCmd();
        resetCmd._entityMgr = entityManager;
        resetCmd._s2sVpnService = vpnService;
        ReflectionTestUtils.setField(resetCmd, "id", CONNECTION_ID);

        deleteCmd = new DeleteVpnConnectionCmd();
        deleteCmd._entityMgr = entityManager;
        deleteCmd._s2sVpnService = vpnService;
        ReflectionTestUtils.setField(deleteCmd, "id", CONNECTION_ID);

        deleteGatewayCmd = new DeleteVpnGatewayCmd();
        deleteGatewayCmd._entityMgr = entityManager;
        ReflectionTestUtils.setField(deleteGatewayCmd, "id", VPN_GATEWAY_ID);
    }

    @Test
    public void testResetUsesVpcSynchronization() {
        configureExistingConnection();

        assertEquals(BaseAsyncCmd.vpcSyncObject, resetCmd.getSyncObjType());
        assertEquals(VPC_ID, resetCmd.getSyncObjId());
    }

    @Test
    public void testDeleteUsesVpcSynchronization() {
        configureExistingConnection();

        assertEquals(BaseAsyncCmd.vpcSyncObject, deleteCmd.getSyncObjType());
        assertEquals(VPC_ID, deleteCmd.getSyncObjId());
    }

    @Test
    public void testDeleteGatewayUsesVpcSynchronization() {
        when(entityManager.findById(Site2SiteVpnGateway.class, VPN_GATEWAY_ID)).thenReturn(gateway);
        when(gateway.getVpcId()).thenReturn(VPC_ID);

        assertEquals(BaseAsyncCmd.vpcSyncObject, deleteGatewayCmd.getSyncObjType());
        assertEquals(VPC_ID, deleteGatewayCmd.getSyncObjId());
    }

    @Test
    public void testResetWithMissingConnectionHasNoSynchronizationId() {
        when(entityManager.findById(Site2SiteVpnConnection.class, CONNECTION_ID)).thenReturn(null);

        assertNull(resetCmd.getSyncObjId());
    }

    @Test
    public void testDeleteWithMissingConnectionHasNoSynchronizationId() {
        when(entityManager.findById(Site2SiteVpnConnection.class, CONNECTION_ID)).thenReturn(null);

        assertNull(deleteCmd.getSyncObjId());
    }

    @Test
    public void testDeleteGatewayWithMissingGatewayHasNoSynchronizationId() {
        when(entityManager.findById(Site2SiteVpnGateway.class, VPN_GATEWAY_ID)).thenReturn(null);

        assertNull(deleteGatewayCmd.getSyncObjId());
    }

    @Test
    public void testResetWithMissingGatewayHasNoSynchronizationId() {
        when(entityManager.findById(Site2SiteVpnConnection.class, CONNECTION_ID)).thenReturn(connection);
        when(connection.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(vpnService.getVpnGateway(VPN_GATEWAY_ID)).thenReturn(null);

        assertNull(resetCmd.getSyncObjId());
    }

    @Test
    public void testDeleteWithMissingGatewayHasNoSynchronizationId() {
        when(entityManager.findById(Site2SiteVpnConnection.class, CONNECTION_ID)).thenReturn(connection);
        when(connection.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(vpnService.getVpnGateway(VPN_GATEWAY_ID)).thenReturn(null);

        assertNull(deleteCmd.getSyncObjId());
    }

    private void configureExistingConnection() {
        when(entityManager.findById(Site2SiteVpnConnection.class, CONNECTION_ID)).thenReturn(connection);
        when(connection.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(vpnService.getVpnGateway(VPN_GATEWAY_ID)).thenReturn(gateway);
        when(gateway.getVpcId()).thenReturn(VPC_ID);
    }
}
