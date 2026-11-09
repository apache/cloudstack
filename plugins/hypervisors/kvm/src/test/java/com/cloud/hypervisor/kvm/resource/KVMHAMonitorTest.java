//
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
//

package com.cloud.hypervisor.kvm.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class KVMHAMonitorTest {

    private static final String PRIVATE_IP = "10.0.0.1";
    private static final String GUID = "7c5f1d70-1f36-4e3a-9b64-2d0d8a2f5f10-LibvirtComputingResource";

    private final KVMHAMonitor monitor = new KVMHAMonitor(PRIVATE_IP, GUID);

    /** The type of the pools is not one with HA support, so that the monitor does not look for the pool in libvirt. */
    private KVMStoragePool addPool(String uuid) {
        return addPool(monitor, uuid);
    }

    private KVMStoragePool addPool(KVMHAMonitor haMonitor, String uuid) {
        KVMStoragePool pool = mock(KVMStoragePool.class);
        when(pool.getUuid()).thenReturn(uuid);
        when(pool.getType()).thenReturn(StoragePoolType.CLVM);
        haMonitor.addStoragePool(new HAStoragePool(pool, "10.0.0.2", "/export", PoolType.PrimaryStorage));
        return pool;
    }

    @Test
    public void testPoolNamesTheHeartBeatOfTheHost() {
        KVMStoragePool pool = addPool("a");
        when(pool.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn("the id of the host");
        when(pool.createHeartBeatCommand(any(), eq("the id of the host"), eq(true))).thenReturn(null);

        monitor.runHeartBeat();

        verify(pool).createHeartBeatCommand(any(), eq("the id of the host"), eq(true));
    }

    @Test
    public void testPoolWhichCanNotRunItsHeartBeatDoesNotStopTheOthers() {
        KVMStoragePool broken = addPool("a");
        when(broken.getHeartBeatHostId(PRIVATE_IP, GUID)).thenThrow(new CloudRuntimeException("the host has no GUID"));
        KVMStoragePool working = addPool("b");
        when(working.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn(PRIVATE_IP);
        when(working.createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true))).thenReturn(null);

        // it must not throw: the thread of the monitor would stop, and with it the heartbeats of all the pools
        monitor.runHeartBeat();

        verify(broken, never()).createHeartBeatCommand(any(), any(), eq(true));
        verify(working).createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true));
    }

    @Test
    public void testPoolWhichThrowsWhileRunningItsHeartBeatDoesNotStopTheOthers() {
        KVMStoragePool broken = addPool("a");
        when(broken.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn(PRIVATE_IP);
        when(broken.createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true))).thenThrow(new CloudRuntimeException("Unable to find script"));
        KVMStoragePool working = addPool("b");
        when(working.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn(PRIVATE_IP);
        when(working.createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true))).thenReturn(null);

        monitor.runHeartBeat();

        // a heartbeat which can not be run is not tried again, it would fail again
        verify(broken, times(1)).createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true));
        verify(working).createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true));
    }

    @Test
    public void testHostIsStoppedAfterAFailedHeartBeatAndAFailedStopDoesNotStopTheMonitor() {
        try (MockedStatic<AgentPropertiesFileHandler> properties = Mockito.mockStatic(AgentPropertiesFileHandler.class, Mockito.CALLS_REAL_METHODS)) {
            properties.when(() -> AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT)).thenReturn(true);
            KVMHAMonitor stoppingMonitor = new KVMHAMonitor(PRIVATE_IP, GUID);

            KVMStoragePool failing = addPool(stoppingMonitor, "a");
            when(failing.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn(PRIVATE_IP);
            // not a result of the script, which would be tried again at once and then each 10 seconds
            when(failing.createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true))).thenThrow(new CloudRuntimeException("Unable to find script"));
            when(failing.createHeartBeatCommand(any(), isNull(), eq(false))).thenThrow(new CloudRuntimeException("Unable to find script"));
            KVMStoragePool working = addPool(stoppingMonitor, "b");
            when(working.getHeartBeatHostId(PRIVATE_IP, GUID)).thenReturn(PRIVATE_IP);
            when(working.createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true))).thenReturn(null);

            stoppingMonitor.runHeartBeat();

            verify(failing).createHeartBeatCommand(any(), isNull(), eq(false));
            verify(working).createHeartBeatCommand(any(), eq(PRIVATE_IP), eq(true));
            verify(working, never()).createHeartBeatCommand(any(), isNull(), eq(false));
        }
    }
}
