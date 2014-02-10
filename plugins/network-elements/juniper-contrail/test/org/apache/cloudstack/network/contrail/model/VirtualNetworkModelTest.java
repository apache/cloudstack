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

package org.apache.cloudstack.network.contrail.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import junit.framework.TestCase;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorMock;

import org.apache.log4j.Logger;
import org.junit.Test;

import org.apache.cloudstack.network.contrail.management.ContrailManager;
import org.apache.cloudstack.network.contrail.management.ContrailManagerImpl;
import org.apache.cloudstack.network.contrail.management.ModelDatabase;

import com.cloud.network.Network.State;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;

public class VirtualNetworkModelTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(VirtualNetworkModelTest.class);

    @Test
    public void testDBLookup() {
        ModelDatabase db = new ModelDatabase();
        NetworkVO network = mock(NetworkVO.class);
        VirtualNetworkModel storageModel = new VirtualNetworkModel(network, null, ContrailManager.managementNetworkName, TrafficType.Storage);
        db.getVirtualNetworks().add(storageModel);
        VirtualNetworkModel mgmtModel = new VirtualNetworkModel(network, null, ContrailManager.managementNetworkName, TrafficType.Management);
        db.getVirtualNetworks().add(mgmtModel);
        VirtualNetworkModel guestModel1 = new VirtualNetworkModel(network, UUID.randomUUID().toString(), "test", TrafficType.Guest);
        db.getVirtualNetworks().add(guestModel1);
        VirtualNetworkModel guestModel2 = new VirtualNetworkModel(network, UUID.randomUUID().toString(), "test", TrafficType.Guest);
        db.getVirtualNetworks().add(guestModel2);
        s_logger.debug("networks: " + db.getVirtualNetworks().size());
        s_logger.debug("No of Vitual Networks added to database : " + db.getVirtualNetworks().size());
        assertEquals(4, db.getVirtualNetworks().size());
        assertSame(storageModel, db.lookupVirtualNetwork(null, storageModel.getName(), TrafficType.Storage));
        assertSame(mgmtModel, db.lookupVirtualNetwork(null, mgmtModel.getName(), TrafficType.Management));
        assertSame(guestModel1, db.lookupVirtualNetwork(guestModel1.getUuid(), null, TrafficType.Guest));
        assertSame(guestModel2, db.lookupVirtualNetwork(guestModel2.getUuid(), null, TrafficType.Guest));
    }

    @Test
    public void testCreateVirtualNetwork() throws IOException {

        String uuid = UUID.randomUUID().toString();
        ContrailManagerImpl contrailMgr = mock(ContrailManagerImpl.class);
        ModelController controller      = mock(ModelController.class);
        ApiConnector api = new ApiConnectorMock(null, 0);
        when(controller.getManager()).thenReturn(contrailMgr);
        when(controller.getApiAccessor()).thenReturn(api);

        // Create Virtual-Network (VN)
        NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Allocated);
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getPhysicalNetworkId()).thenReturn(42L);
        when(network.getDomainId()).thenReturn(10L);
        when(network.getAccountId()).thenReturn(42L);

        when(contrailMgr.getCanonicalName(network)).thenReturn("testnetwork");
        when(contrailMgr.getProjectId(network.getDomainId(), network.getAccountId())).thenReturn("testProjectId");

        VirtualNetworkModel vnModel = new VirtualNetworkModel(network, uuid, "testnetwork", TrafficType.Guest);

        assertEquals(vnModel.getName(), "testnetwork");
        assertEquals(vnModel.getUuid(), uuid);

        vnModel.build(controller, network);
        try {
            vnModel.update(controller);
        } catch (Exception ex) {
            fail("virtual-network update failed ");
        }
        assertTrue(vnModel.verify(controller));
    }

}
