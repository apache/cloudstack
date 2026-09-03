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
package org.apache.cloudstack.vm;

import java.util.List;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;

public class VmwareCbtStorageTargetTest {

    private final VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();

    @Test
    public void testFilesystemTargetDoesNotRequireInPlaceFinalization() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.NetworkFilesystem));

        Assert.assertTrue(target.isSupported());
        Assert.assertEquals(VmwareCbtTargetStorageType.QCOW2_FILE, target.getTargetStorageType());
        Assert.assertFalse(target.requiresInPlaceFinalization());
        Assert.assertTrue(target.supportsNonInPlaceFinalizationFallback());
    }

    @Test
    public void testRbdTargetRequiresInPlaceFinalization() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.RBD));

        Assert.assertTrue(target.isSupported());
        Assert.assertEquals(VmwareCbtTargetStorageType.RBD_RAW, target.getTargetStorageType());
        Assert.assertTrue(target.requiresInPlaceFinalization());
        Assert.assertFalse(target.supportsNonInPlaceFinalizationFallback());
    }

    @Test
    public void testLinstorTargetUsesRawBlockDeviceAndRequiresInPlaceFinalization() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.Linstor));

        Assert.assertTrue(target.isSupported());
        Assert.assertEquals(VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE, target.getTargetStorageType());
        Assert.assertTrue(target.requiresInPlaceFinalization());
        Assert.assertFalse(target.supportsNonInPlaceFinalizationFallback());
    }

    @Test
    public void testBlockDeviceTargetMarkerIsShortAndStable() {
        String marker = VmwareCbtMigrationManagerImpl.getBlockDeviceTargetMarker("12345678-9abc-def0-1234-56789abcdef0");

        Assert.assertEquals("cbt-12345678-", marker);
    }

    @Test
    public void testListCbtCompatibleStoragePoolsIncludesLinstorPools() {
        PrimaryDataStoreDao primaryDataStoreDao = Mockito.mock(PrimaryDataStoreDao.class);
        ReflectionTestUtils.setField(manager, "primaryDataStoreDao", primaryDataStoreDao);
        DataCenterVO zone = createZone(1L);
        ClusterVO cluster = createCluster(2L);
        StoragePoolVO linstorPool = createStoragePool(11L, Storage.StoragePoolType.Linstor);

        Mockito.when(primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(2L,
                Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.Linstor)).thenReturn(List.of(linstorPool));

        List<StoragePoolVO> pools = manager.listCbtCompatibleStoragePools(zone, cluster);

        Assert.assertEquals(1, pools.size());
        Assert.assertSame(linstorPool, pools.get(0));
    }

    @Test
    public void testValidateStorageTargetFinalizationFailsWhenHostDoesNotSupportInPlace() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.RBD));
        HostVO host = createHost("kvm1", "false");

        try {
            manager.validateStorageTargetFinalizationSupport(target, host);
            Assert.fail("Expected validation failure");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("requires virt-v2v in-place finalization"));
            Assert.assertTrue(e.getDescription().contains("kvm1"));
        }
    }

    @Test
    public void testValidateStorageTargetFinalizationPassesWhenHostSupportsInPlace() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.RBD));
        HostVO host = createHost("kvm1", "true");

        manager.validateStorageTargetFinalizationSupport(target, host);
    }

    @Test
    public void testValidateFilesystemTargetFinalizationFailsWhenHostDoesNotSupportInPlaceByDefault() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.NetworkFilesystem));
        HostVO host = createHost("kvm1", "false");

        try {
            manager.validateStorageTargetFinalizationSupport(target, host);
            Assert.fail("Expected validation failure");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("cannot finalize VMware CBT migration in-place"));
            Assert.assertTrue(e.getDescription().contains("kvm1"));
        }
    }

    @Test
    public void testValidateFilesystemTargetFinalizationPassesWhenHostSupportsInPlace() {
        VmwareCbtStorageTarget target = VmwareCbtStorageTarget.forPool(createStoragePool(Storage.StoragePoolType.NetworkFilesystem));
        HostVO host = createHost("kvm1", "true");

        manager.validateStorageTargetFinalizationSupport(target, host);
    }

    @Test
    public void testListCbtCompatibleStoragePoolsIncludesRbdPools() {
        PrimaryDataStoreDao primaryDataStoreDao = Mockito.mock(PrimaryDataStoreDao.class);
        ReflectionTestUtils.setField(manager, "primaryDataStoreDao", primaryDataStoreDao);
        DataCenterVO zone = createZone(1L);
        ClusterVO cluster = createCluster(2L);
        StoragePoolVO rbdPool = createStoragePool(10L, Storage.StoragePoolType.RBD);

        Mockito.when(primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(2L,
                Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.RBD)).thenReturn(List.of(rbdPool));

        List<StoragePoolVO> pools = manager.listCbtCompatibleStoragePools(zone, cluster);

        Assert.assertEquals(1, pools.size());
        Assert.assertSame(rbdPool, pools.get(0));
    }

    @Test
    public void testListCbtCompatibleStoragePoolsDeduplicatesClusterAndZoneScope() {
        PrimaryDataStoreDao primaryDataStoreDao = Mockito.mock(PrimaryDataStoreDao.class);
        ReflectionTestUtils.setField(manager, "primaryDataStoreDao", primaryDataStoreDao);
        DataCenterVO zone = createZone(1L);
        ClusterVO cluster = createCluster(2L);
        StoragePoolVO pool = createStoragePool(10L, Storage.StoragePoolType.NetworkFilesystem);

        Mockito.when(primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(2L,
                Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem)).thenReturn(List.of(pool));
        Mockito.when(primaryDataStoreDao.findZoneWideStoragePoolsByHypervisorAndPoolType(1L,
                Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem)).thenReturn(List.of(pool));

        List<StoragePoolVO> pools = manager.listCbtCompatibleStoragePools(zone, cluster);

        Assert.assertEquals(1, pools.size());
        Assert.assertSame(pool, pools.get(0));
    }

    private StoragePoolVO createStoragePool(Storage.StoragePoolType poolType) {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getPoolType()).thenReturn(poolType);
        return pool;
    }

    private StoragePoolVO createStoragePool(long id, Storage.StoragePoolType poolType) {
        StoragePoolVO pool = createStoragePool(poolType);
        Mockito.when(pool.getId()).thenReturn(id);
        return pool;
    }

    private HostVO createHost(String name, String inPlaceSupported) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getName()).thenReturn(name);
        Mockito.when(host.getDetail(Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT)).thenReturn(inPlaceSupported);
        return host;
    }

    private DataCenterVO createZone(long id) {
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getId()).thenReturn(id);
        return zone;
    }

    private ClusterVO createCluster(long id) {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(id);
        return cluster;
    }
}
