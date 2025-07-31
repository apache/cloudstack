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
    public void testGetRscGroupStoragePools() throws ApiException {
        List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(api, "cloudstack");

        List<String> names = storagePools.stream()
                .map(sp -> String.format("%s::%s", sp.getNodeName(), sp.getStoragePoolName()))
                .collect(Collectors.toList());
        Assert.assertEquals(names, Arrays.asList("nodeA::thinpool", "nodeB::thinpool", "nodeC::thinpool"));
    }
}
