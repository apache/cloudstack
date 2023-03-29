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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VirtualNetworkPolicyType;

import org.apache.cloudstack.network.contrail.management.ContrailManager;
import org.apache.cloudstack.network.contrail.management.ContrailManagerImpl;
import org.apache.cloudstack.network.contrail.management.ModelDatabase;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.cloud.dc.dao.VlanDao;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;

public class VirtualNetworkModelTest extends TestCase {

    private static final Logger s_logger = Logger.getLogger(VirtualNetworkModelTest.class);

    private ModelController controller;

    private VirtualNetworkModel vnModel;
    private VirtualNetworkModel vnModel1;
    private VirtualNetworkModel vnModel2;
    private VirtualNetworkModel vnModel3;

    @Override
    @Before
    public void setUp() throws IOException {
        //Network UUIDs
        String uuid = UUID.randomUUID().toString();
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        String uuid3 = UUID.randomUUID().toString();

        //ContrailManager
        ContrailManagerImpl contrailMgr = mock(ContrailManagerImpl.class);

        controller = mock(ModelController.class);
        VlanDao vlanDao = mock(VlanDao.class);

        ApiConnector api = mock(ApiConnectorMock.class);

        //Mock classes/methods
        when(controller.getManager()).thenReturn(contrailMgr);
        when(controller.getApiAccessor()).thenReturn(api);
        when(controller.getVlanDao()).thenReturn(vlanDao);

        //Policy References used by vnModel1
        List<ObjectReference<VirtualNetworkPolicyType>> policyRefs1 = new ArrayList<ObjectReference<VirtualNetworkPolicyType>>();
        ObjectReference<VirtualNetworkPolicyType> objectReference1 = new ObjectReference<VirtualNetworkPolicyType>();
        policyRefs1.add(objectReference1);

        //Policy References used by vnModel2
        List<ObjectReference<VirtualNetworkPolicyType>> policyRefs2 = new ArrayList<ObjectReference<VirtualNetworkPolicyType>>();
        ObjectReference<VirtualNetworkPolicyType> objectReference2 = new ObjectReference<VirtualNetworkPolicyType>();
        policyRefs2.add(objectReference2);

        //Policy References used by vnModel3
        List<ObjectReference<VirtualNetworkPolicyType>> policyRefs3 = new ArrayList<ObjectReference<VirtualNetworkPolicyType>>();
        ObjectReference<VirtualNetworkPolicyType> objectReference3 = new ObjectReference<VirtualNetworkPolicyType>();
        objectReference3.setReference(Arrays.asList(""), null, null, UUID.randomUUID().toString());

        policyRefs3.add(objectReference3);

        //Network to be compared with
        VirtualNetwork vn = mock(VirtualNetwork.class);
        when(api.findById(VirtualNetwork.class, uuid)).thenReturn(vn);

        //Network to be compared with
        VirtualNetwork vn1 = mock(VirtualNetwork.class);
        when(api.findById(VirtualNetwork.class, uuid1)).thenReturn(vn1);
        when(vn1.getNetworkPolicy()).thenReturn(policyRefs1);

        //Network to be compared to
        VirtualNetwork vn2 = mock(VirtualNetwork.class);
        when(api.findById(VirtualNetwork.class, uuid2)).thenReturn(vn2);
        when(vn2.getNetworkPolicy()).thenReturn(policyRefs2);

        //Network to be compared to
        VirtualNetwork vn3 = mock(VirtualNetwork.class);
        when(api.findById(VirtualNetwork.class, uuid3)).thenReturn(vn3);
        when(vn3.getNetworkPolicy()).thenReturn(policyRefs3);

        //Virtual-Network 1
        NetworkVO network1 = mock(NetworkVO.class);
        when(network1.getName()).thenReturn("testnetwork");
        when(network1.getState()).thenReturn(State.Allocated);
        when(network1.getGateway()).thenReturn("10.1.1.1");
        when(network1.getCidr()).thenReturn("10.1.1.0/24");
        when(network1.getPhysicalNetworkId()).thenReturn(42L);
        when(network1.getDomainId()).thenReturn(10L);
        when(network1.getAccountId()).thenReturn(42L);

        //Virtual-Network 2
        NetworkVO network2 = mock(NetworkVO.class);
        when(network2.getName()).thenReturn("Testnetwork");
        when(network2.getState()).thenReturn(State.Allocated);
        when(network2.getGateway()).thenReturn("10.1.1.1");
        when(network2.getCidr()).thenReturn("10.1.1.0/24");
        when(network2.getPhysicalNetworkId()).thenReturn(42L);
        when(network2.getDomainId()).thenReturn(10L);
        when(network2.getAccountId()).thenReturn(42L);

        //Virtual-Network 3
        NetworkVO network3 = mock(NetworkVO.class);
        when(network3.getName()).thenReturn("Testnetwork");
        when(network3.getState()).thenReturn(State.Allocated);
        when(network3.getGateway()).thenReturn("10.1.1.1");
        when(network3.getCidr()).thenReturn("10.1.1.0/24");
        when(network3.getPhysicalNetworkId()).thenReturn(42L);
        when(network3.getDomainId()).thenReturn(10L);
        when(network3.getAccountId()).thenReturn(42L);

        when(contrailMgr.getCanonicalName(network1)).thenReturn("testnetwork");
        when(contrailMgr.getProjectId(network1.getDomainId(), network1.getAccountId())).thenReturn("testProjectId");

        vnModel = new VirtualNetworkModel(network1, uuid, "testnetwork", TrafficType.Guest);
        vnModel1 = new VirtualNetworkModel(network1, uuid1, "testnetwork", TrafficType.Guest);
        vnModel2 = new VirtualNetworkModel(network2, uuid2, "testnetwork", TrafficType.Guest);
        vnModel3 = new VirtualNetworkModel(network3, uuid3, "testnetwork", TrafficType.Guest);
    }

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

    @Test
    public void testCompareDifferentVirtualNetwork() throws IOException {
        //This one returns false because one network has Policy References
        vnModel.read(controller);

        assertFalse(vnModel.compare(controller, vnModel1));
    }

    @Test
    public void testCompareSameVirtualNetwork() throws IOException {
        //This one returns true because both networks have the same Policy References
        vnModel1.read(controller);

        assertTrue(vnModel1.compare(controller, vnModel2));
    }

    @Test
    public void testCompareDifferentDeeperVirtualNetwork() throws IOException {
        //This one returns false because one network has Policy References
        vnModel2.read(controller);

        assertFalse(vnModel2.compare(controller, vnModel3));
    }
}
