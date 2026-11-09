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

package com.cloud.hypervisor.kvm.storage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.storage.Storage.StoragePoolType;

@RunWith(MockitoJUnitRunner.class)
public class KVMStoragePoolManagerTest {

    private static final String POOL_UUID = "7684f6e5-fd91-3435-b04e-59ae0efb3914";

    private final KVMHAMonitor monitor = mock(KVMHAMonitor.class);
    private final StorageAdaptor adaptor = mock(StorageAdaptor.class);
    private final Map<String, String> details = new HashMap<>();
    private KVMStoragePoolManager manager;

    @Before
    public void setUp() throws Exception {
        // the constructor is not run, as it looks for all the storage adaptors of the agent, some of which need scripts and libraries of a host
        manager = mock(KVMStoragePoolManager.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        // every type of pool gets the adaptor of the test
        Map<String, StorageAdaptor> adaptors = new HashMap<>();
        adaptors.put("libvirt", adaptor);
        setField("_storageMapper", adaptors);
        setField("_storagePools", new ConcurrentHashMap<String, Object>());
        setField("_haMonitor", monitor);
        setField("logger", LogManager.getLogger(KVMStoragePoolManager.class));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = KVMStoragePoolManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(manager, value);
    }

    @Test
    public void testPoolsWithHaSupportAreRemovedFromTheMonitorWhenDeleted() {
        for (StoragePoolType type : new StoragePoolType[] {StoragePoolType.NetworkFilesystem, StoragePoolType.SharedMountPoint, StoragePoolType.RBD}) {
            when(adaptor.deleteStoragePool(POOL_UUID)).thenReturn(true);
            when(adaptor.deleteStoragePool(POOL_UUID, details)).thenReturn(true);

            assertTrue(manager.deleteStoragePool(type, POOL_UUID));
            assertTrue(manager.deleteStoragePool(type, POOL_UUID, details));
        }
        verify(monitor, times(6)).removeStoragePool(POOL_UUID);
    }

    @Test
    public void testPoolsAreDeletedWithDetailsAsBefore() {
        // the delete which takes the details does not destroy the libvirt pool, which the VMs may still use, e.g. in maintenance of the pool
        for (StoragePoolType type : new StoragePoolType[] {StoragePoolType.NetworkFilesystem, StoragePoolType.RBD, StoragePoolType.SharedMountPoint}) {
            reset(adaptor, monitor);
            when(adaptor.deleteStoragePool(POOL_UUID, details)).thenReturn(true);

            assertTrue(manager.deleteStoragePool(type, POOL_UUID, details));

            verify(monitor).removeStoragePool(POOL_UUID);
            verify(adaptor).deleteStoragePool(POOL_UUID, details);
            verify(adaptor, never()).deleteStoragePool(POOL_UUID);
        }
    }

    @Test
    public void testPoolWithoutHaSupportIsNotRemovedFromTheMonitor() {
        when(adaptor.deleteStoragePool(POOL_UUID, details)).thenReturn(true);

        assertTrue(manager.deleteStoragePool(StoragePoolType.CLVM, POOL_UUID, details));

        verify(monitor, never()).removeStoragePool(any());
        verify(adaptor, never()).deleteStoragePool(POOL_UUID);
    }

    private KVMStoragePool mockCreatedPool(String prerequisiteError) {
        KVMStoragePool pool = mock(KVMStoragePool.class);
        when(pool.getUuid()).thenReturn(POOL_UUID);
        when(pool.isPoolSupportHA()).thenReturn(true);
        when(pool.getHeartBeatPrerequisiteError()).thenReturn(prerequisiteError);
        when(adaptor.createStoragePool(any(), any(), anyInt(), any(), any(), any(), any(), eq(true))).thenReturn(pool);
        return pool;
    }

    @Test
    public void testPoolWhichHeartBeatCanBeUsedIsMonitoredForHa() {
        mockCreatedPool(null);

        manager.createStoragePool("pool", "10.0.0.1", 6789, "rbdpool", "cephuser:cephkey", StoragePoolType.RBD, details);

        verify(monitor).addStoragePool(any());
    }

    @Test
    public void testPoolWhichHeartBeatCanNotBeUsedIsNotMonitoredForHa() {
        // e.g. the Python bindings of Ceph are not installed: the host could not write its heartbeat, and would be fenced
        mockCreatedPool("the package python3-rados is not installed");

        KVMStoragePool pool = manager.createStoragePool("pool", "10.0.0.1", 6789, "rbdpool", "cephuser:cephkey", StoragePoolType.RBD, details);

        verify(monitor, never()).addStoragePool(any());
        // the pool itself is still usable
        assertNotNull(pool);
    }
}
