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
package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

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

import com.google.gson.Gson;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.MigrateWithStorageCompleteCommand;
import com.cloud.agent.api.MigrateWithStorageReceiveCommand;
import com.cloud.agent.api.MigrateWithStorageSendCommand;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
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
import com.cloud.storage.StoragePool;
import com.cloud.utils.Pair;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;

@RunWith(PowerMockRunner.class)
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
        final String path = "/";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);

        final VolumeTO vol1 = Mockito.mock(VolumeTO.class);
        final VolumeTO vol2 = Mockito.mock(VolumeTO.class);
        final StorageFilerTO storage1 = Mockito.mock(StorageFilerTO.class);
        final StorageFilerTO storage2 = Mockito.mock(StorageFilerTO.class);

        final Map<VolumeTO, StorageFilerTO> volumeToFiler = new  HashMap<VolumeTO, StorageFilerTO>();
        volumeToFiler.put(vol1, storage1);
        volumeToFiler.put(vol2, storage2);

        final NicTO nicTO1 = Mockito.mock(NicTO.class);
        final NicTO nicTO2 = Mockito.mock(NicTO.class);
        final NicTO nicTO3 = Mockito.mock(NicTO.class);
        final NicTO [] nicTOs = {nicTO1, nicTO2, nicTO3};

        final XsLocalNetwork nativeNetworkForTraffic = Mockito.mock(XsLocalNetwork.class);
        final Network networkForSm = Mockito.mock(Network.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final SR sr1 = Mockito.mock(SR.class);
        final SR sr2 = Mockito.mock(SR.class);

        final VDI vdi1 = Mockito.mock(VDI.class);
        final VDI vdi2 = Mockito.mock(VDI.class);

        final MigrateWithStorageCommand migrateStorageCommand = new MigrateWithStorageCommand(vmSpec, volumeToFiler);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);
        when(vmSpec.getNics()).thenReturn(nicTOs);

        when(storage1.getUuid()).thenReturn(uuid);
        when(storage2.getUuid()).thenReturn(uuid);

        when(vol1.getPath()).thenReturn(path);
        when(vol2.getPath()).thenReturn(path);

        when(xenServer610Resource.getStorageRepository(conn, storage1.getUuid())).thenReturn(sr1);
        when(xenServer610Resource.getStorageRepository(conn, storage2.getUuid())).thenReturn(sr2);

        when(xenServer610Resource.getVDIbyUuid(conn, storage1.getPath())).thenReturn(vdi1);
        when(xenServer610Resource.getVDIbyUuid(conn, storage2.getPath())).thenReturn(vdi2);

        try {
            when(xenServer610Resource.getNativeNetworkForTraffic(conn, TrafficType.Storage, null)).thenReturn(nativeNetworkForTraffic);
            when(nativeNetworkForTraffic.getNetwork()).thenReturn(networkForSm);

            when(xenServer610Resource.getHost()).thenReturn(xsHost);
            when(xsHost.getUuid()).thenReturn(uuid);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(migrateStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        try {
            verify(xenServer610Resource, times(1)).prepareISO(conn, vmName, null, null);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO1);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO2);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO3);

            verify(xenServer610Resource, times(1)).getNativeNetworkForTraffic(conn, TrafficType.Storage, null);
            verify(nativeNetworkForTraffic, times(1)).getNetwork();

            verify(xenServer610Resource, times(1)).getHost();
            verify(xsHost, times(1)).getUuid();
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageReceiveCommand() {
        final String vmName = "small";
        final String uuid = "206b21a7-c6ec-40e2-b5e2-f861b9612f04";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);

        final VolumeTO vol1 = Mockito.mock(VolumeTO.class);
        final VolumeTO vol2 = Mockito.mock(VolumeTO.class);
        final StorageFilerTO storage1 = Mockito.mock(StorageFilerTO.class);
        final StorageFilerTO storage2 = Mockito.mock(StorageFilerTO.class);

        final List<Pair<VolumeTO, String>> volumeToFiler = new ArrayList<>();
        volumeToFiler.add(new Pair<>(vol1, storage1.getPath()));
        volumeToFiler.add(new Pair<>(vol2, storage2.getPath()));

        final NicTO nicTO1 = Mockito.mock(NicTO.class);
        final NicTO nicTO2 = Mockito.mock(NicTO.class);
        final NicTO nicTO3 = Mockito.mock(NicTO.class);
        final NicTO [] nicTOs = {nicTO1, nicTO2, nicTO3};

        final XsLocalNetwork nativeNetworkForTraffic = Mockito.mock(XsLocalNetwork.class);
        final Network network = Mockito.mock(Network.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final Network nw1 = Mockito.mock(Network.class);
        final Network nw2 = Mockito.mock(Network.class);
        final Network nw3 = Mockito.mock(Network.class);

        final SR sr1 = Mockito.mock(SR.class);
        final SR sr2 = Mockito.mock(SR.class);

        final MigrateWithStorageReceiveCommand migrateStorageCommand = new MigrateWithStorageReceiveCommand(vmSpec, volumeToFiler);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);
        when(vmSpec.getNics()).thenReturn(nicTOs);

        when(storage1.getUuid()).thenReturn(uuid);
        when(storage2.getUuid()).thenReturn(uuid);

        when(xenServer610Resource.getStorageRepository(conn, storage1.getUuid())).thenReturn(sr1);
        when(xenServer610Resource.getStorageRepository(conn, storage2.getUuid())).thenReturn(sr2);

        try {

            when(xenServer610Resource.getNetwork(conn, nicTO1)).thenReturn(nw1);
            when(xenServer610Resource.getNetwork(conn, nicTO2)).thenReturn(nw2);
            when(xenServer610Resource.getNetwork(conn, nicTO3)).thenReturn(nw3);

            when(xenServer610Resource.getNativeNetworkForTraffic(conn, TrafficType.Storage, null)).thenReturn(nativeNetworkForTraffic);
            when(nativeNetworkForTraffic.getNetwork()).thenReturn(network);

            when(xenServer610Resource.getHost()).thenReturn(xsHost);
            when(xsHost.getUuid()).thenReturn(uuid);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(migrateStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        try {
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO1);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO2);
            verify(xenServer610Resource, times(1)).getNetwork(conn, nicTO3);

            verify(xenServer610Resource, times(1)).getNativeNetworkForTraffic(conn, TrafficType.Storage, null);
            verify(nativeNetworkForTraffic, times(1)).getNetwork();

            verify(xenServer610Resource, times(1)).getHost();
            verify(xsHost, times(1)).getUuid();
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageSendCommand() {
        final String vmName = "small";
        final String path = "/";
        final String mac = "3c:15:c2:c4:4f:18";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);

        final VolumeTO volume1 = Mockito.mock(VolumeTO.class);
        final VolumeTO volume2 = Mockito.mock(VolumeTO.class);

        final SR sr1 = Mockito.mock(SR.class);
        final SR sr2 = Mockito.mock(SR.class);

        final VDI vdi1 = Mockito.mock(VDI.class);
        final VDI vdi2 = Mockito.mock(VDI.class);

        final NicTO nic1 = Mockito.mock(NicTO.class);
        final NicTO nic2 = Mockito.mock(NicTO.class);

        final Network network1 = Mockito.mock(Network.class);
        final Network network2 = Mockito.mock(Network.class);

        final List<Pair<VolumeTO, Object>> volumeToSr = new ArrayList<Pair<VolumeTO, Object>>();
        volumeToSr.add(new Pair<VolumeTO, Object>(volume1, sr1));
        volumeToSr.add(new Pair<VolumeTO, Object>(volume2, sr2));

        final List<Pair<NicTO, Object>> nicToNetwork = new ArrayList<Pair<NicTO, Object>>();
        nicToNetwork.add(new Pair<NicTO, Object>(nic1, network1));
        nicToNetwork.add(new Pair<NicTO, Object>(nic2, network2));

        final Map<String, String> token = new HashMap<String, String>();

        final VIF vif1 = Mockito.mock(VIF.class);
        final VIF vif2 = Mockito.mock(VIF.class);

        final MigrateWithStorageSendCommand migrateStorageCommand = new MigrateWithStorageSendCommand(vmSpec, volumeToSr, nicToNetwork, token);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);

        when(volume1.getPath()).thenReturn(path);
        when(volume2.getPath()).thenReturn(path);

        when(nic1.getMac()).thenReturn(mac);
        when(nic2.getMac()).thenReturn(mac);

        when(xenServer610Resource.getVDIbyUuid(conn, volume1.getPath())).thenReturn(vdi1);
        when(xenServer610Resource.getVDIbyUuid(conn, volume2.getPath())).thenReturn(vdi2);

        try {
            when(xenServer610Resource.getVifByMac(conn, null, nic1.getMac())).thenReturn(vif1);
            when(xenServer610Resource.getVifByMac(conn, null, nic2.getMac())).thenReturn(vif2);
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        final Answer answer = wrapper.execute(migrateStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        try {
            verify(xenServer610Resource, times(2)).getVDIbyUuid(conn, volume1.getPath());
            verify(xenServer610Resource, times(2)).getVifByMac(conn, null, nic1.getMac());
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageSendCommandSRException() {
        final String vmName = "small";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);

        final VolumeTO volume1 = Mockito.mock(VolumeTO.class);
        final VolumeTO volume2 = Mockito.mock(VolumeTO.class);

        final List<Pair<VolumeTO, Object>> volumeToSr = new ArrayList<Pair<VolumeTO, Object>>();
        volumeToSr.add(new Pair<VolumeTO, Object>(volume1, new String("a")));
        volumeToSr.add(new Pair<VolumeTO, Object>(volume2, new String("b")));

        final List<Pair<NicTO, Object>> nicToNetwork = new ArrayList<Pair<NicTO, Object>>();
        final Map<String, String> token = new HashMap<String, String>();

        final MigrateWithStorageSendCommand migrateStorageCommand = new MigrateWithStorageSendCommand(vmSpec, volumeToSr, nicToNetwork, token);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);

        final Answer answer = wrapper.execute(migrateStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageSendCommandNetException() {
        final String vmName = "small";
        final String path = "/";

        final Connection conn = Mockito.mock(Connection.class);
        final VirtualMachineTO vmSpec = Mockito.mock(VirtualMachineTO.class);

        final VolumeTO volume1 = Mockito.mock(VolumeTO.class);
        final VolumeTO volume2 = Mockito.mock(VolumeTO.class);

        final SR sr1 = Mockito.mock(SR.class);
        final SR sr2 = Mockito.mock(SR.class);

        final VDI vdi1 = Mockito.mock(VDI.class);
        final VDI vdi2 = Mockito.mock(VDI.class);

        final NicTO nic1 = Mockito.mock(NicTO.class);
        final NicTO nic2 = Mockito.mock(NicTO.class);

        Gson gson = new Gson();
        final List<Pair<VolumeTO, Object>> volumeToSr = new ArrayList<Pair<VolumeTO, Object>>();
        volumeToSr.add(new Pair<VolumeTO, Object>(volume1, sr1));
        volumeToSr.add(new Pair<VolumeTO, Object>(volume2, sr2));

        final List<Pair<NicTO, Object>> nicToNetwork = new ArrayList<Pair<NicTO, Object>>();
        nicToNetwork.add(new Pair<NicTO, Object>(nic1, new String("a")));
        nicToNetwork.add(new Pair<NicTO, Object>(nic2, new String("b")));

        final Map<String, String> token = new HashMap<String, String>();

        final MigrateWithStorageSendCommand migrateStorageCommand = new MigrateWithStorageSendCommand(vmSpec, volumeToSr, nicToNetwork, token);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vmSpec.getName()).thenReturn(vmName);

        when(volume1.getPath()).thenReturn(path);
        when(volume2.getPath()).thenReturn(path);

        when(xenServer610Resource.getVDIbyUuid(conn, volume1.getPath())).thenReturn(vdi1);
        when(xenServer610Resource.getVDIbyUuid(conn, volume2.getPath())).thenReturn(vdi2);

        final Answer answer = wrapper.execute(migrateStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateWithStorageCompleteCommand() {
        final String vmName = "small";
        final String uuid = "206b21a7-c6ec-40e2-b5e2-f861b9612f04";

        final Connection conn = Mockito.mock(Connection.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final VirtualMachineTO vm = Mockito.mock(VirtualMachineTO.class);

        final MigrateWithStorageCompleteCommand createStorageCommand = new MigrateWithStorageCompleteCommand(vm);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(vm.getName()).thenReturn(vmName);
        when(xenServer610Resource.getHost()).thenReturn(xsHost);
        when(xsHost.getUuid()).thenReturn(uuid);

        final Answer answer = wrapper.execute(createStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testXenServer610MigrateVolumeCommandWrapper() {
        final String uuid = "206b21a7-c6ec-40e2-b5e2-f861b9612f04";

        final Connection conn = Mockito.mock(Connection.class);
        final SR destinationPool = Mockito.mock(SR.class);
        final VDI srcVolume = Mockito.mock(VDI.class);
        final Task task = Mockito.mock(Task.class);

        final long volumeId = 1l;
        final String volumePath = "206b21a7-c6ec-40e2-b5e2-f861b9612f04";
        final StoragePool pool = Mockito.mock(StoragePool.class);
        final int timeout = 120;

        final Map<String, String> other = new HashMap<String, String>();
        other.put("live", "true");

        final MigrateVolumeCommand createStorageCommand = new MigrateVolumeCommand(volumeId, volumePath, pool, timeout);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer610Resource.getConnection()).thenReturn(conn);
        when(pool.getUuid()).thenReturn(uuid);
        when(xenServer610Resource.getStorageRepository(conn, uuid)).thenReturn(destinationPool);
        when(xenServer610Resource.getVDIbyUuid(conn, volumePath)).thenReturn(srcVolume);

        try {
            when(srcVolume.poolMigrateAsync(conn, destinationPool, other)).thenReturn(task);
        } catch (final BadServerResponse e) {
            fail(e.getMessage());
        } catch (final XenAPIException e) {
            fail(e.getMessage());
        } catch (final XmlRpcException e) {
            fail(e.getMessage());
        }

        when(xenServer610Resource.getMigrateWait()).thenReturn(120);

        final Answer answer = wrapper.execute(createStorageCommand, xenServer610Resource);

        verify(xenServer610Resource, times(1)).getConnection();

        //        try {
        //            verify(xenServer610Resource, times(1)).waitForTask(conn, task, 1000, timeout);
        //            verify(xenServer610Resource, times(1)).checkForSuccess(conn, task);
        //        } catch (final XenAPIException e) {
        //            fail(e.getMessage());
        //        } catch (final XmlRpcException e) {
        //            fail(e.getMessage());
        //        } catch (final TimeoutException e) {
        //            fail(e.getMessage());
        //        }

        assertFalse(answer.getResult());
    }
}
