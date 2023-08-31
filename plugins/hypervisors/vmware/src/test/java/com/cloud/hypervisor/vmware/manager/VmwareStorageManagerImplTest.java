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
package com.cloud.hypervisor.vmware.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.storage.Storage;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDisk;

public class VmwareStorageManagerImplTest {

    @InjectMocks
    private VmwareStorageManagerImpl storageManager;

    @Before
    public void init() {
        VmwareStorageMount mountService = Mockito.mock(VmwareStorageMount.class);

        storageManager = new VmwareStorageManagerImpl(mountService);
    }

    private void testCommon(Storage.StoragePoolType poolType, Storage.StoragePoolType parentPoolType, boolean dsChange) {
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        VmwareContext context = Mockito.mock(VmwareContext.class);
        VmwareHypervisorHost hyperHost = Mockito.mock(VmwareHypervisorHost.class);
        final String volumePath = "somepath";
        final String uuid1 = UUID.randomUUID().toString();
        final String uuid2 = UUID.randomUUID().toString();
        VolumeObjectTO volumeTO = new VolumeObjectTO();
        volumeTO.setPath(volumePath);
        PrimaryDataStoreTO primaryDataStoreTO = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.when(primaryDataStoreTO.getPoolType()).thenReturn(poolType);
        Mockito.when(primaryDataStoreTO.getParentPoolType()).thenReturn(parentPoolType);
        Mockito.when(primaryDataStoreTO.getUuid()).thenReturn(uuid1);
        volumeTO.setDataStore(primaryDataStoreTO);
        boolean isVolumeOnDatastoreCluster = Storage.StoragePoolType.DatastoreCluster.equals(poolType) || Storage.StoragePoolType.DatastoreCluster.equals(parentPoolType);
        if (isVolumeOnDatastoreCluster && dsChange) {
            volumeTO.setDataStoreUuid(uuid2);
        }
        VmwareClient vmwareClient = Mockito.mock(VmwareClient.class);
        VimPortType service = Mockito.mock(VimPortType.class);
        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ArrayList<HostDatastoreBrowserSearchResults> arr = new ArrayList<>();
        Mockito.when(context.getVimClient()).thenReturn(vmwareClient);
        Mockito.when(context.getService()).thenReturn(service);
        List<VolumeObjectTO> volumes = List.of(volumeTO);
        try {
            Mockito.when(vmMo.getVmName()).thenReturn("dummy-vm");
            String key = "browser";
            Mockito.when(vmwareClient.getDynamicProperty(null, key)).thenReturn(null);
            Mockito.when(vmwareClient.getDynamicProperty(mor, key)).thenReturn(mor);
            Mockito.when(vmwareClient.waitForTask(Mockito.any())).thenReturn(true);
            Mockito.doNothing().when(context).waitForTaskProgressDone(Mockito.any(ManagedObjectReference.class));
            key = "info.result";
            Mockito.when(vmwareClient.getDynamicProperty(null, key)).thenReturn(arr);
            Mockito.doThrow(RuntimeException.class).when(service).searchDatastoreSubFoldersTask(Mockito.eq(null), Mockito.anyString(), Mockito.any());
            Mockito.when(vmMo.getAllDiskDevice()).thenReturn(new VirtualDisk[0]);
            if (isVolumeOnDatastoreCluster) {
                if (dsChange) {
                    Mockito.when(hyperHost.findDatastore(uuid2)).thenReturn(mor);
                } else {
                    Mockito.when(hyperHost.findDatastore(uuid1)).thenReturn(mor);
                }
            } else {
                Mockito.when(hyperHost.findDatastoreByName(volumePath)).thenReturn(mor);
            }
            storageManager.setVolumeToPathAndSize(volumes, vmMo, Mockito.mock(VmwareHostService.class), context, hyperHost);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(0L, (long) volumes.get(0).getSize());
    }

    @Test
    public void testSetVolumeToPathAndSizeNotDatastoreCluster() {
        testCommon(Storage.StoragePoolType.VMFS, null, false);
    }

    @Test
    public void testSetVolumeToPathAndSizeDatastoreClusterSameChildStore() {
        testCommon(Storage.StoragePoolType.PreSetup, Storage.StoragePoolType.DatastoreCluster, false);
    }

    @Test
    public void testSetVolumeToPathAndSizeDatastoreClusterDifferentChildStore() {
        testCommon(Storage.StoragePoolType.PreSetup, Storage.StoragePoolType.DatastoreCluster, true);
    }

    @Test
    public void testIsManagedStorageDatastorePath() {
        Assert.assertTrue("Test if [-iqn... is a managed storage", storageManager.isManagedStorageDatastorePath("[-iqn.2010-01.com.solidfire:3p53.data-9999.97-0] i-2-9999-VM.vmdk"));
        Assert.assertFalse("Test if [SomeDS] is not a managed storage", storageManager.isManagedStorageDatastorePath("[SomeDS] i-2-9999-VM/disk.vmdk"));
    }

    @Test
    public void testGetManagedDatastoreName() {
        Assert.assertEquals("[-iqn.2010-01.com.solidfire:3p53.data-9999.97-0]", storageManager.getManagedDatastoreName("[-iqn.2010-01.com.solidfire:3p53.data-9999.97-0] i-2-9999-VM.vmdk"));
    }
}
