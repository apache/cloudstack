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
package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.host.HostEnvironment;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.hypervisor.xenserver.resource.XsLocalNetwork;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types.XenAPIException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Host.class})
public class XenServer610WrapperTest {

    @Mock
    protected XenServer610Resource xenServer610Resource;

    @Test
    public void testCheckNetworkCommandFailure() {
        final XenServer610Resource xenServer610Resource = new XenServer610Resource();

        final PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();

        final List<PhysicalNetworkSetupInfo> setupInfos = new ArrayList<PhysicalNetworkSetupInfo>();
        setupInfos.add(info);

        final CheckNetworkCommand checkNet = new CheckNetworkCommand(setupInfos);

        final Answer answer = xenServer610Resource.executeRequest(checkNet);

        assertTrue(answer.getResult());
    }

    @Test
    public void testSetupCommand() {
        final XenServer610Resource xenServer610Resource = new XenServer610Resource();

        final HostEnvironment env = Mockito.mock(HostEnvironment.class);

        final SetupCommand setupCommand = new SetupCommand(env);

        final Answer answer = xenServer610Resource.executeRequest(setupCommand);

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageCommand() {
        final String vmName = "small";
        final String uuid = "206b21a7-c6ec-40e2-b5e2-f861b9612f04";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);
        final Map<VolumeTO, StorageFilerTO> volumeToFiler = new  HashMap<VolumeTO, StorageFilerTO>();

        final NicTO nicTO1 = Mockito.mock(NicTO.class);
        final NicTO nicTO2 = Mockito.mock(NicTO.class);
        final NicTO nicTO3 = Mockito.mock(NicTO.class);
        final NicTO [] nicTOs = {nicTO1, nicTO2, nicTO3};

        final XsLocalNetwork nativeNetworkForTraffic = Mockito.mock(XsLocalNetwork.class);
        final Network networkForSm = Mockito.mock(Network.class);

        final XsHost xsHost = Mockito.mock(XsHost.class);
        final Host host = Mockito.mock(Host.class);

        final MigrateWithStorageCommand gpuStats = new MigrateWithStorageCommand(vmSpec, volumeToFiler);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);
        when(vmSpec.getNics()).thenReturn(nicTOs);

        try {
            when(xenServer610Resource.getHost()).thenReturn(xsHost);
            when(xsHost.getUuid()).thenReturn(uuid);

            when(xenServer610Resource.getNativeNetworkForTraffic(conn, TrafficType.Storage, null)).thenReturn(nativeNetworkForTraffic);
            when(nativeNetworkForTraffic.getNetwork()).thenReturn(networkForSm);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(gpuStats, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        try {
            verify(xenServer610Resource, times(1)).prepareISO(conn, vmName);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO1);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO2);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO3);

            verify(xenServer610Resource, times(1)).getNativeNetworkForTraffic(conn, TrafficType.Storage, null);
            verify(nativeNetworkForTraffic, times(1)).getNetwork();
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertFalse(answer.getResult());
    }
}