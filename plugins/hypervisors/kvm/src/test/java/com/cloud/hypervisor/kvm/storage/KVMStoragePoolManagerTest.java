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
package com.cloud.hypervisor.kvm.storage;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ceph.rados.exceptions.RadosException;
import com.ceph.rbd.RbdException;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

public class KVMStoragePoolManagerTest {

    private static final String POOL_UUID = "pool-uuid";
    private static final String VOLUME = "volume";

    private KVMStoragePoolManager manager;
    private KVMStoragePool pool;

    @Before
    public void setUp() {
        // The real constructor scans the classpath for storage adaptors, which getPhysicalDisk does not need.
        manager = mock(KVMStoragePoolManager.class, Mockito.CALLS_REAL_METHODS);
        manager.logger = LogManager.getLogger(KVMStoragePoolManager.class);
        pool = mock(KVMStoragePool.class);
        doReturn(pool).when(manager).getStoragePool(StoragePoolType.RBD, POOL_UUID);
        doNothing().when(manager).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskDoesNotRetryACephTimeout() {
        CloudRuntimeException failure = new CloudRuntimeException("open failed",
                new RbdException("Failed to open image " + VOLUME, -110));
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(failure);

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class,
                () -> manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        assertSame(failure, thrown);
        verify(pool, times(1)).getPhysicalDisk(VOLUME);
        verify(manager, never()).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskDoesNotRetryARadosConnectFailure() {
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(new CloudRuntimeException("connect failed",
                new RadosException("Failed to connect", -110)));

        assertThrows(CloudRuntimeException.class, () -> manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        verify(pool, times(1)).getPhysicalDisk(VOLUME);
        verify(manager, never()).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskDoesNotRetryAMissingPool() {
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(new CloudRuntimeException("ioctx failed",
                new RadosException("Failed to create the IoCTX", -2)));

        assertThrows(CloudRuntimeException.class, () -> manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        verify(pool, times(1)).getPhysicalDisk(VOLUME);
        verify(manager, never()).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskDoesNotRetryATimeoutBehindANotFound() {
        RbdException timeout = new RbdException("Failed to open image " + VOLUME, -110);
        RbdException notFound = new RbdException("Failed to open image " + VOLUME, -2);
        notFound.initCause(timeout);
        RuntimeException failure = new RuntimeException("open failed", notFound);
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(failure);

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class,
                () -> manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        assertSame(failure, thrown.getCause());
        verify(pool, times(1)).getPhysicalDisk(VOLUME);
        verify(manager, never()).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskRetriesAnImageThatIsNotThereYet() {
        KVMPhysicalDisk disk = mock(KVMPhysicalDisk.class);
        CloudRuntimeException notFound = new CloudRuntimeException("open failed",
                new RbdException("Failed to open image " + VOLUME, -2));
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(notFound).thenThrow(notFound).thenReturn(disk);

        assertSame(disk, manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        verify(pool, times(3)).getPhysicalDisk(VOLUME);
        verify(manager, times(2)).sleepBeforeRetry(anyLong());
    }

    @Test
    public void getPhysicalDiskKeepsRetryingAnImageThatNeverAppears() {
        when(pool.getPhysicalDisk(VOLUME)).thenThrow(new CloudRuntimeException("open failed",
                new RbdException("Failed to open image " + VOLUME, -2)));

        assertThrows(CloudRuntimeException.class, () -> manager.getPhysicalDisk(StoragePoolType.RBD, POOL_UUID, VOLUME));

        // 100 tries in the loop and a last one after it
        verify(pool, times(101)).getPhysicalDisk(VOLUME);
    }

    @Test
    public void getPhysicalDiskStillRetriesAnErrorThatIsNotFromCeph() {
        doReturn(pool).when(manager).getStoragePool(StoragePoolType.NetworkFilesystem, POOL_UUID);
        doThrow(new CloudRuntimeException("libvirt lookup failed")).when(pool).getPhysicalDisk(VOLUME);

        assertThrows(CloudRuntimeException.class,
                () -> manager.getPhysicalDisk(StoragePoolType.NetworkFilesystem, POOL_UUID, VOLUME));

        verify(pool, times(101)).getPhysicalDisk(VOLUME);
        verify(manager, times(100)).sleepBeforeRetry(anyLong());
    }
}
