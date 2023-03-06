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

package org.apache.cloudstack.storage.endpoint;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.image.datastore.ImageStoreInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ScopeType;
import com.cloud.vm.VirtualMachine;

public class DefaultEndPointSelectorTest {

    @Mock
    HostDao hostDao;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    @InjectMocks
    private DefaultEndPointSelector defaultEndPointSelector = new DefaultEndPointSelector();

    @Test
    public void testGetVmwareHostFromVolumeToDeleteBasic() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(null);
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(null);
        Mockito.when(virtualMachine.getLastHostId()).thenReturn(null);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));

        Long hostId = 1L;
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        Mockito.when(hostDao.findById(hostId)).thenReturn(null);
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }

    @Test
    public void testGetVmwareHostFromVolumeToDeleteNotPrimary() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Long hostId = 1L;
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        Mockito.when(hostDao.findById(hostId)).thenReturn(Mockito.mock(HostVO.class));
        Mockito.when(volumeInfo.getDataStore()).thenReturn(Mockito.mock(ImageStoreInfo.class));
        Assert.assertNotNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }

    @Test
    public void testGetVmwareHostFromVolumeToDeletePrimaryNoScope() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Long hostId = 1L;
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        Mockito.when(hostDao.findById(hostId)).thenReturn(Mockito.mock(HostVO.class));
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Mockito.when(store.getScope()).thenReturn(null);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }

    @Test
    public void testGetVmwareHostFromVolumeToDeletePrimaryNotHostOrClusterScope() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Long hostId = 1L;
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        Mockito.when(hostDao.findById(hostId)).thenReturn(Mockito.mock(HostVO.class));
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Scope scope = Mockito.mock(Scope.class);
        Mockito.when(scope.getScopeType()).thenReturn(ScopeType.ZONE);
        Mockito.when(store.getScope()).thenReturn(scope);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);
        Assert.assertNotNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }

    @Test
    public void testGetVmwareHostFromVolumeToDeletePrimaryClusterScope() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Long hostId = 1L;
        Long clusterId = 1L;
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostVO.getClusterId()).thenReturn(clusterId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(hostVO);
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Scope scope = Mockito.mock(Scope.class);
        Mockito.when(scope.getScopeType()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(store.getScope()).thenReturn(scope);
        Mockito.when(store.getClusterId()).thenReturn(clusterId);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);
        Assert.assertNotNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));

        Mockito.when(store.getClusterId()).thenReturn(2L);
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }

    @Test
    public void testGetVmwareHostFromVolumeToDeletePrimaryHostScope() {
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        Long hostId = 1L;
        String storageIp = "something";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.getHostId()).thenReturn(hostId);
        Mockito.when(volumeInfo.getAttachedVM()).thenReturn(virtualMachine);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostVO.getStorageIpAddress()).thenReturn(storageIp);
        Mockito.when(hostDao.findById(hostId)).thenReturn(hostVO);
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        Scope scope = Mockito.mock(Scope.class);
        Mockito.when(scope.getScopeType()).thenReturn(ScopeType.HOST);
        Mockito.when(store.getScope()).thenReturn(scope);
        Mockito.when(store.getHostAddress()).thenReturn(storageIp);
        Mockito.when(volumeInfo.getDataStore()).thenReturn(store);
        Assert.assertNotNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));

        Mockito.when(store.getHostAddress()).thenReturn("something-else");
        Assert.assertNull(defaultEndPointSelector.getVmwareHostFromVolumeToDelete(volumeInfo));
    }
}