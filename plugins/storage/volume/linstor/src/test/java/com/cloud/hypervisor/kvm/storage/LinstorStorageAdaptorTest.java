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

import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.Resource;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LinstorStorageAdaptorTest {

    private static final String LOCAL_NODE = "node1";
    private static final String VOLUME = "1b0a8c3e-5f2d-4c67-9a10-3e5b7d9c2f41";
    private static final String RSC_NAME = "cs-" + VOLUME;
    private static final String RSC_GROUP = "Linstor-RG";

    private LinstorStorageAdaptor adaptor;
    private LinstorStoragePool pool;
    private MockedConstruction<ApiClient> apiClients;
    private MockedConstruction<DevelopersApi> apis;
    private final List<ResourceDefinition> rscDfns = new ArrayList<>();
    private final List<Resource> resources = new ArrayList<>();

    @Before
    public void setUp() {
        try (MockedStatic<LinstorStoragePool> ignored = Mockito.mockStatic(LinstorStoragePool.class)) {
            Mockito.when(LinstorStoragePool.getHostname()).thenReturn(LOCAL_NODE);
            adaptor = new LinstorStorageAdaptor();
        }
        pool = Mockito.mock(LinstorStoragePool.class);
        Mockito.lenient().when(pool.getResourceGroup()).thenReturn(RSC_GROUP);

        apiClients = Mockito.mockConstruction(ApiClient.class);
        apis = Mockito.mockConstruction(DevelopersApi.class, (api, context) -> {
            Mockito.lenient().when(api.resourceDefinitionList(Collections.singletonList(RSC_NAME), null, null, null, null))
                    .thenReturn(rscDfns);
            Mockito.lenient().when(api.resourceList(RSC_NAME, null, null)).thenReturn(resources);
        });
    }

    @After
    public void tearDown() {
        apis.close();
        apiClients.close();
    }

    private void addResourceDefinition(String rscGroup) {
        ResourceDefinition rd = new ResourceDefinition();
        rd.setName(RSC_NAME);
        rd.setResourceGroupName(rscGroup);
        rscDfns.add(rd);
    }

    private void addResource(String node, String... flags) {
        Resource rsc = new Resource();
        rsc.setName(RSC_NAME);
        rsc.setNodeName(node);
        List<String> rscFlags = new ArrayList<>();
        Collections.addAll(rscFlags, flags);
        rsc.setFlags(rscFlags);
        resources.add(rsc);
    }

    @Test
    public void checkForImportAcceptsResourceOfPoolGroup() {
        addResourceDefinition(RSC_GROUP.toLowerCase());

        adaptor.checkPhysicalDiskForImport(VOLUME, pool);
    }

    @Test
    public void checkForImportRejectsResourceOfAnotherResourceGroup() {
        addResourceDefinition("OtherRG");

        Assert.assertThrows(CloudRuntimeException.class, () -> adaptor.checkPhysicalDiskForImport(VOLUME, pool));
    }

    @Test
    public void checkForImportRejectsMissingResource() {
        Assert.assertThrows(CloudRuntimeException.class, () -> adaptor.checkPhysicalDiskForImport(VOLUME, pool));
    }

    @Test
    public void inUseWhenPrimaryOnAnyNode() {
        addResource("node2");
        Resource inUse = resources.get(0);
        inUse.setState(new ResourceState().inUse(true));

        Assert.assertTrue(adaptor.isPhysicalDiskInUse(VOLUME, pool));
    }

    @Test
    public void notInUseWhenNoNodeIsPrimary() {
        addResource("node2");
        addResource(LOCAL_NODE, ApiConsts.FLAG_DRBD_DISKLESS);

        Assert.assertFalse(adaptor.isPhysicalDiskInUse(VOLUME, pool));
    }
}
