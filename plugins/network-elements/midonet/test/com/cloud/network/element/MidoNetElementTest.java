/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.cloud.network.element.MidoNetElement;
import junit.framework.TestCase;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import com.midokura.midonet.client.MidonetApi;
import com.midokura.midonet.client.resource.*;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.cloud.network.*;
import com.cloud.vm.*;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.element.MidoNetElement;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import sun.security.util.Resources_es;

import java.util.*;


public class MidoNetElementTest extends TestCase {

    /*
     * Test the standard case of addDhcpEntry with no errors.
     */
    public void testAddDhcpEntry() {

        //mockMgmt
        MidonetApi api = mock(MidonetApi.class, RETURNS_DEEP_STUBS);
        ArrayList<String> arr = new ArrayList<String>();
        arr.add("MidoNet");
        NetworkServiceMapDao mockNSMD = mock(NetworkServiceMapDao.class);
        when(mockNSMD.getDistinctProviders(anyLong())).thenReturn(arr);

        //mockDhcpHost
        DhcpHost mockDhcpHost = mock(DhcpHost.class);

        //mockHostCollection
        ResourceCollection<DhcpHost> hosts =
                new ResourceCollection<DhcpHost>(new ArrayList<DhcpHost>());

        //mockDhcpSubnet
        DhcpSubnet mockSub = mock(DhcpSubnet.class);
        when(mockSub.addDhcpHost()).thenReturn(mockDhcpHost);
        when(mockSub.getDhcpHosts()).thenReturn(hosts);

        //mockSubnetCollection
        ResourceCollection mockSubnetCollection = mock(ResourceCollection.class);
        when(mockSubnetCollection.get(anyInt())).thenReturn(mockSub);

        //mockBridge
        Bridge mockBridge = mock(Bridge.class);
        when(api.addBridge().tenantId(anyString()).name(anyString()).create()).thenReturn(mockBridge);
        when(mockBridge.getDhcpSubnets()).thenReturn(mockSubnetCollection);

        //mockRouter
        Router mockRouter = mock(Router.class);
        when(api.addRouter().tenantId(anyString()).name(anyString()).create()).thenReturn(mockRouter);

        //mockNetwork
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getAccountId()).thenReturn((long)1);
        when(mockNetwork.getGateway()).thenReturn("1.2.3.4");
        when(mockNetwork.getCidr()).thenReturn("1.2.3.0/24");
        when(mockNetwork.getId()).thenReturn((long)2);

        //mockNic
        NicProfile mockNic = mock(NicProfile.class);
        when(mockNic.getIp4Address()).thenReturn("10.10.10.170");
        when(mockNic.getMacAddress()).thenReturn("02:00:73:3e:00:01");
        when(mockNic.getName()).thenReturn("Fake Name");

        //mockVm
        @SuppressWarnings("unchecked")
        VirtualMachineProfile<? extends VirtualMachine> mockVm =
                (VirtualMachineProfile<? extends VirtualMachine>)mock(VirtualMachineProfile.class);
        when(mockVm.getType()).thenReturn(VirtualMachine.Type.User);

        MidoNetElement elem = new MidoNetElement();
        elem.setNtwkSrvcDao(mockNSMD);
        elem.setMidonetApi(api);

        boolean result = false;
        try {
            result = elem.addDhcpEntry(mockNetwork, mockNic, mockVm, null, null);
        } catch (ConcurrentOperationException e) {
            fail(e.getMessage());
        } catch (InsufficientCapacityException e) {
            fail(e.getMessage());
        } catch (ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        assertEquals(result, true);
    }

    /*
     * Test the standard case of implement with no errors.
     */
    public void testImplement() {
        //mock
        MidonetApi api = mock(MidonetApi.class, RETURNS_DEEP_STUBS);
        ArrayList<String> arr = new ArrayList<String>();
        arr.add("MidoNet");
        NetworkServiceMapDao mockNSMD = mock(NetworkServiceMapDao.class);
        when(mockNSMD.getDistinctProviders(anyLong())).thenReturn(arr);

        MidoNetElement elem = new MidoNetElement();
        elem.setNtwkSrvcDao(mockNSMD);
        elem.setMidonetApi(api);

        //mockRPort
        RouterPort mockRPort = mock(RouterPort.class);
        when(mockRPort.getId()).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        //mockBPort
        BridgePort mockBPort = mock(BridgePort.class);
        when(mockBPort.link(any(UUID.class))).thenReturn(mockBPort);

        //mockPort
        Port mockPort = mock(Port.class);

        ResourceCollection<Port> peerPorts =
            new ResourceCollection<Port>(new ArrayList<Port>());

        peerPorts.add(mockPort);

        //mockBridge
        Bridge mockBridge = mock(Bridge.class, RETURNS_DEEP_STUBS);
        when(api.addBridge().tenantId(anyString()).name(anyString()).create()).thenReturn(mockBridge);
        when(mockBridge.addInteriorPort().create()).thenReturn(mockBPort);
        when(mockBridge.getPeerPorts()).thenReturn(peerPorts);

        //mockRouter
        Router mockRouter = mock(Router.class, RETURNS_DEEP_STUBS);
        when(api.addRouter().tenantId(anyString()).name(anyString()).create()).thenReturn(mockRouter);
        when(mockRouter.addInteriorRouterPort().create()).thenReturn(mockRPort);

        //mockNetwork
        Network mockNetwork = mock(Network.class);
        when(mockNetwork.getAccountId()).thenReturn((long)1);
        when(mockNetwork.getGateway()).thenReturn("1.2.3.4");
        when(mockNetwork.getCidr()).thenReturn("1.2.3.0/24");
        when(mockNetwork.getId()).thenReturn((long)2);

        boolean result = false;
        try {
            result = elem.implement(mockNetwork, null, null, null);
        } catch (ConcurrentOperationException e) {
            fail(e.getMessage());
        } catch (InsufficientCapacityException e) {
            fail(e.getMessage());
        } catch (ResourceUnavailableException e) {
            fail(e.getMessage());
        }

        assertEquals(result, true);
    }
}
