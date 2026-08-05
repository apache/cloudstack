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
package org.apache.cloudstack.storage.datastore.util;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.ControllerVersion;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.StoragePool;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinstorUtilTest {

    private static final String LINSTOR_URL_TEST = "devnull.com:3370";
    private DevelopersApi api;

    private Node mockNode(String name) {
        Node nodeMock = new Node();
        nodeMock.setName(name);

        return nodeMock;
    }

    private StoragePool mockStoragePool(String name, String node, ProviderKind kind) {
        StoragePool sp = new StoragePool();
        sp.setStoragePoolName(name);
        sp.setNodeName(node);
        sp.setProviderKind(kind);
        return sp;
    }

    private StoragePool mockStoragePool(
            String name, String node, ProviderKind kind, String freeSpaceMgrName, long totalKib, long freeKib) {
        StoragePool sp = mockStoragePool(name, node, kind);
        sp.setFreeSpaceMgrName(freeSpaceMgrName);
        sp.setTotalCapacity(totalKib);
        sp.setFreeCapacity(freeKib);
        return sp;
    }

    @Before
    public void setUp() throws ApiException {
        api = mock(DevelopersApi.class);

        when(api.nodeList(Collections.emptyList(), Collections.emptyList(), null, null))
                .thenReturn(Arrays.asList(mockNode("nodeA"), mockNode("nodeB"), mockNode("nodeC")));

        ResourceGroup csGroup = new ResourceGroup();
        csGroup.setName("cloudstack");
        AutoSelectFilter asf = new AutoSelectFilter();
        asf.setPlaceCount(2);
        csGroup.setSelectFilter(asf);
        when(api.resourceGroupList(Collections.singletonList("cloudstack"), null, null, null))
                .thenReturn(Collections.singletonList(csGroup));

        when(api.viewStoragePools(Collections.emptyList(), null, null, null, null, true))
                .thenReturn(Arrays.asList(
                        mockStoragePool("thinpool", "nodeA", ProviderKind.LVM_THIN),
                        mockStoragePool("thinpool", "nodeB", ProviderKind.LVM_THIN),
                        mockStoragePool("thinpool", "nodeC", ProviderKind.LVM_THIN)
                ));

//        when(LinstorUtil.getLinstorAPI(LINSTOR_URL_TEST)).thenReturn(api);
    }

    @Test
    public void testGetLinstorNodeNames() throws ApiException {
        List<String> linstorNodes = LinstorUtil.getLinstorNodeNames(api);
        Assert.assertEquals(Arrays.asList("nodeA", "nodeB", "nodeC"), linstorNodes);
    }

    @Test
    public void testGetSnapshotPath() {
        {
            StoragePool spLVMThin = new StoragePool();
            Properties lvmThinProps = new Properties();
            lvmThinProps.put("StorDriver/StorPoolName", "storage/storage-thin");
            spLVMThin.setProps(lvmThinProps);
            spLVMThin.setProviderKind(ProviderKind.LVM_THIN);
            String snapPath = LinstorUtil.getSnapshotPath(spLVMThin, "cs-cb32532a-dd8f-47e0-a81c-8a75573d3545", "snap3");
            Assert.assertEquals("/dev/mapper/storage-cs--cb32532a--dd8f--47e0--a81c--8a75573d3545_00000_snap3", snapPath);
        }

        {
            StoragePool spLVM = new StoragePool();
            Properties lvmProps = new Properties();
            lvmProps.put("StorDriver/StorPoolName", "shared");
            spLVM.setProps(lvmProps);
            spLVM.setProviderKind(ProviderKind.LVM);
            String snapPath = LinstorUtil.getSnapshotPath(spLVM, "cs-cb32532a-dd8f-47e0-a81c-8a75573d3545", "cs-6c6b4e95");
            Assert.assertEquals(
                "/dev/mapper/shared-cs--cb32532a--dd8f--47e0--a81c--8a75573d3545_00000_cs--6c6b4e95", snapPath);
        }

        {
            StoragePool spZFS = new StoragePool();
            Properties zfsProps = new Properties();
            zfsProps.put("StorDriver/StorPoolName", "linstorPool");
            spZFS.setProps(zfsProps);
            spZFS.setProviderKind(ProviderKind.ZFS);

            String snapPath = LinstorUtil.getSnapshotPath(spZFS, "cs-cb32532a-dd8f-47e0-a81c-8a75573d3545", "snap2");
            Assert.assertEquals("zfs://linstorPool/cs-cb32532a-dd8f-47e0-a81c-8a75573d3545_00000@snap2", snapPath);
        }
    }

    @Test
    public void testGetCapacityStoragePoolsCountsSharedSpaceOnce() throws ApiException {
        ResourceGroup sharedGroup = new ResourceGroup();
        sharedGroup.setName("shared");
        AutoSelectFilter asf = new AutoSelectFilter();
        asf.setPlaceCount(1);
        sharedGroup.setSelectFilter(asf);
        when(api.resourceGroupList(Collections.singletonList("shared"), null, null, null))
                .thenReturn(Collections.singletonList(sharedGroup));

        // 3 nodes accessing the same shared space, each reporting the full 100 GiB, plus one
        // node-local pool and a diskless pool which must be ignored
        when(api.viewStoragePools(Collections.emptyList(), null, null, null, null, true))
                .thenReturn(Arrays.asList(
                        mockStoragePool("sharedpool", "nodeA", ProviderKind.LVM, "cs-shared", 104857600L, 52428800L),
                        mockStoragePool("sharedpool", "nodeB", ProviderKind.LVM, "cs-shared", 104857600L, 52428800L),
                        mockStoragePool("sharedpool", "nodeC", ProviderKind.LVM, "cs-shared", 104857600L, 52428800L),
                        mockStoragePool("local", "nodeA", ProviderKind.LVM_THIN, "nodeA;local", 10485760L, 10485760L),
                        mockStoragePool("diskless", "nodeB", ProviderKind.DISKLESS, "nodeB;diskless", 0L, 0L)
                ));

        List<StoragePool> pools = LinstorUtil.getCapacityStoragePools(api, "shared");
        Assert.assertEquals(2, pools.size());
        // 100 GiB shared (once) + 10 GiB local
        Assert.assertEquals((104857600L + 10485760L) * 1024, LinstorUtil.getFreeCapacityBytes(api, "shared")
                + LinstorUtil.getUsedCapacityBytes(api, "shared"));
        // half of the shared space is used, the local pool is empty
        Assert.assertEquals(52428800L * 1024, LinstorUtil.getUsedCapacityBytes(api, "shared"));
    }

    @Test
    public void testIsVersionAtLeast() {
        Assert.assertTrue(LinstorUtil.isVersionAtLeast("1.29.1", 1, 29, 1));
        Assert.assertTrue(LinstorUtil.isVersionAtLeast("1.29.2", 1, 29, 1));
        Assert.assertTrue(LinstorUtil.isVersionAtLeast("1.30.0", 1, 29, 1));
        Assert.assertTrue(LinstorUtil.isVersionAtLeast("2.0.0", 1, 29, 1));
        Assert.assertTrue(LinstorUtil.isVersionAtLeast("1.30", 1, 29, 1));

        Assert.assertFalse(LinstorUtil.isVersionAtLeast("1.29.0", 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast("1.29", 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast("1.28.5", 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast("0.99.9", 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast(null, 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast("", 1, 29, 1));
        Assert.assertFalse(LinstorUtil.isVersionAtLeast("garbage", 1, 29, 1));
    }

    private ControllerVersion controllerVersion(String restApiVersion) {
        ControllerVersion version = new ControllerVersion();
        version.setRestApiVersion(restApiVersion);
        return version;
    }

    @Test
    public void testSupportsLiveMigrateApi() throws ApiException {
        DevelopersApi newCtrl = mock(DevelopersApi.class);
        when(newCtrl.controllerVersion()).thenReturn(controllerVersion("1.29.0"));
        Assert.assertTrue(LinstorUtil.supportsLiveMigrateApi(newCtrl));

        DevelopersApi oldCtrl = mock(DevelopersApi.class);
        when(oldCtrl.controllerVersion()).thenReturn(controllerVersion("1.28.3"));
        Assert.assertFalse(LinstorUtil.supportsLiveMigrateApi(oldCtrl));

        DevelopersApi unreachable = mock(DevelopersApi.class);
        when(unreachable.controllerVersion()).thenThrow(new ApiException(503, "unavailable"));
        Assert.assertFalse(LinstorUtil.supportsLiveMigrateApi(unreachable));
    }

    @Test
    public void testGetRscGroupStoragePools() throws ApiException {
        List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(api, "cloudstack");

        List<String> names = storagePools.stream()
                .map(sp -> String.format("%s::%s", sp.getNodeName(), sp.getStoragePoolName()))
                .collect(Collectors.toList());
        Assert.assertEquals(names, Arrays.asList("nodeA::thinpool", "nodeB::thinpool", "nodeC::thinpool"));
    }
}
