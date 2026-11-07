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

import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.DrbdVolume;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.ResourceState;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StoragePool;
import com.linbit.linstor.api.model.StorageVolume;
import com.linbit.linstor.api.model.Volume;
import com.linbit.linstor.api.model.VolumeDefinition;
import com.linbit.linstor.api.model.VolumeLayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
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
            // dashes in the volume group name must be escaped as well (GH issue #14011)
            StoragePool spLVMThin = new StoragePool();
            Properties lvmThinProps = new Properties();
            lvmThinProps.put("StorDriver/StorPoolName", "linstor_pool-lvm-thin/thin");
            spLVMThin.setProps(lvmThinProps);
            spLVMThin.setProviderKind(ProviderKind.LVM_THIN);
            String snapPath = LinstorUtil.getSnapshotPath(spLVMThin,
                "cs-12fc4055-3985-4025-8eb5-d6fd53effe37", "cs-d7aea646-5f40-46a2-b9dc-77e41ea29336");
            Assert.assertEquals(
                "/dev/mapper/linstor_pool--lvm--thin-cs--12fc4055--3985--4025--8eb5--d6fd53effe37_00000_cs--d7aea646--5f40--46a2--b9dc--77e41ea29336",
                snapPath);
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

    private ResourceWithVolumes mockResource(String node, String pool, boolean inUse, boolean inactive) {
        ResourceWithVolumes rwv = new ResourceWithVolumes();
        rwv.setName("cs-test");
        rwv.setNodeName(node);
        Volume vol = new Volume();
        vol.setProviderKind(ProviderKind.LVM);
        vol.setStoragePoolName(pool);
        rwv.setVolumes(Collections.singletonList(vol));
        ResourceState state = new ResourceState();
        state.setInUse(inUse);
        rwv.setState(state);
        if (inactive) {
            rwv.setFlags(Collections.singletonList(ApiConsts.FLAG_RSC_INACTIVE));
        }
        return rwv;
    }

    @Test
    public void testDiskfulStoragePoolsOrderedByPreference() throws ApiException {
        // inactive copy first, in-use copy last: the result must be in-use, active, inactive
        when(api.viewResources(Collections.emptyList(), Collections.singletonList("cs-test"),
                Collections.emptyList(), Collections.emptyList(), null, null))
                .thenReturn(Arrays.asList(
                        mockResource("nodeC", "poolC", false, true),
                        mockResource("nodeB", "poolB", false, false),
                        mockResource("nodeA", "poolA", true, false)));
        // the pools are queried in preference order (in use, active, inactive)
        when(api.viewStoragePools(Arrays.asList("nodeA", "nodeB", "nodeC"),
                Arrays.asList("poolA", "poolB", "poolC"), Collections.emptyList(), null, null, true))
                .thenReturn(Arrays.asList(
                        mockStoragePool("poolB", "nodeB", ProviderKind.LVM),
                        mockStoragePool("poolA", "nodeA", ProviderKind.LVM),
                        mockStoragePool("poolC", "nodeC", ProviderKind.LVM)));

        List<StoragePool> pools = LinstorUtil.getDiskfulStoragePoolsByPreference(api, "cs-test");
        Assert.assertEquals(Arrays.asList("nodeA", "nodeB", "nodeC"),
                pools.stream().map(StoragePool::getNodeName).collect(Collectors.toList()));
        // the single-pool accessor keeps returning the best suited copy
        Assert.assertEquals("nodeA", LinstorUtil.getDiskfulStoragePool(api, "cs-test").getNodeName());
    }

    @Test
    public void testDiskfulStoragePoolsIgnoresDiskless() throws ApiException {
        ResourceWithVolumes diskless = new ResourceWithVolumes();
        diskless.setName("cs-test");
        diskless.setNodeName("nodeD");
        Volume dlVol = new Volume();
        dlVol.setProviderKind(ProviderKind.DISKLESS);
        diskless.setVolumes(Collections.singletonList(dlVol));
        when(api.viewResources(Collections.emptyList(), Collections.singletonList("cs-test"),
                Collections.emptyList(), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(diskless));

        Assert.assertTrue(LinstorUtil.getDiskfulStoragePoolsByPreference(api, "cs-test").isEmpty());
        Assert.assertNull(LinstorUtil.getDiskfulStoragePool(api, "cs-test"));
    }

    @Test
    public void testGetRscGroupStoragePools() throws ApiException {
        List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(api, "cloudstack");

        List<String> names = storagePools.stream()
                .map(sp -> String.format("%s::%s", sp.getNodeName(), sp.getStoragePoolName()))
                .collect(Collectors.toList());
        Assert.assertEquals(names, Arrays.asList("nodeA::thinpool", "nodeB::thinpool", "nodeC::thinpool"));
    }

    private void mockResourceDefinition(String rscName, long sizeKib, Properties props) throws ApiException {
        VolumeDefinition vlmDfn = new VolumeDefinition();
        vlmDfn.setSizeKib(sizeKib);
        ResourceDefinition rscDfn = new ResourceDefinition();
        rscDfn.setName(rscName);
        rscDfn.addVolumeDefinitionsItem(vlmDfn);
        rscDfn.setProps(props);
        when(api.resourceDefinitionList(Collections.singletonList(rscName), true, null, null, null))
                .thenReturn(Collections.singletonList(rscDfn));
    }

    private ResourceWithVolumes mockResource(String node, boolean diskless, LayerType topLayer, long usableSizeKib) {
        VolumeLayer layer = new VolumeLayer();
        layer.setType(topLayer);
        if (topLayer == LayerType.DRBD) {
            DrbdVolume drbdVolume = new DrbdVolume();
            drbdVolume.setUsableSizeKib(usableSizeKib);
            layer.setData(drbdVolume);
        } else {
            StorageVolume storageVolume = new StorageVolume();
            storageVolume.setUsableSizeKib(usableSizeKib);
            layer.setData(storageVolume);
        }
        Volume volume = new Volume();
        volume.addLayerDataListItem(layer);
        ResourceWithVolumes rsc = new ResourceWithVolumes();
        rsc.setNodeName(node);
        rsc.setFlags(diskless ? Collections.singletonList(ApiConsts.FLAG_DISKLESS) : Collections.emptyList());
        rsc.setVolumes(Collections.singletonList(volume));
        return rsc;
    }

    private void mockResources(String rscName, ResourceWithVolumes... resources) throws ApiException {
        when(api.viewResources(Collections.emptyList(), Collections.singletonList(rscName), Collections.emptyList(),
                null, null, null)).thenReturn(Arrays.asList(resources));
    }

    @Test
    public void testGetVolumeDeviceSizeIsTheSmallestDiskfulDrbdDevice() throws ApiException {
        // a 512 MiB volume definition on LVM-thin, the diskless resource only reports the volume definition's size
        mockResourceDefinition("cs-vol", 524288L, new Properties());
        mockResources("cs-vol",
                mockResource("nodeA", false, LayerType.DRBD, 528232L),
                mockResource("nodeB", true, LayerType.DRBD, 524288L),
                mockResource("nodeC", false, LayerType.DRBD, 528236L));

        Assert.assertEquals(528232L * 1024, LinstorUtil.getVolumeDeviceSize(api, "cs-vol"));
    }

    @Test
    public void testGetVolumeDeviceSizeWithoutDrbd() throws ApiException {
        mockResourceDefinition("cs-vol", 524288L, new Properties());
        mockResources("cs-vol", mockResource("nodeA", false, LayerType.STORAGE, 528384L));

        Assert.assertEquals(528384L * 1024, LinstorUtil.getVolumeDeviceSize(api, "cs-vol"));
    }

    @Test
    public void testGetVolumeDeviceSizeWithExactSize() throws ApiException {
        Properties props = new Properties();
        props.put("DrbdOptions/ExactSize", "True");
        mockResourceDefinition("cs-vol", 524288L, props);

        Assert.assertEquals(524288L * 1024, LinstorUtil.getVolumeDeviceSize(api, "cs-vol"));
        Mockito.verify(api, Mockito.never()).viewResources(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testGetVolumeDeviceSizeFallsBackToVolumeDefinition() throws ApiException {
        // no diskful resource reports a size, e.g. while the resources are being created
        mockResourceDefinition("cs-vol", 524288L, new Properties());
        mockResources("cs-vol", mockResource("nodeB", true, LayerType.DRBD, 524288L));

        Assert.assertEquals(524288L * 1024, LinstorUtil.getVolumeDeviceSize(api, "cs-vol"));
    }

    @Test
    public void testGetVolumeDeviceSizeOfMissingResourceDefinition() throws ApiException {
        when(api.resourceDefinitionList(Collections.singletonList("cs-gone"), true, null, null, null))
                .thenReturn(Collections.emptyList());

        Assert.assertEquals(0L, LinstorUtil.getVolumeDeviceSize(api, "cs-gone"));
    }
}
