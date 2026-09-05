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

package org.apache.cloudstack.api.command.admin.cluster;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.Hypervisor;

@RunWith(MockitoJUnitRunner.class)
public class ListClustersCmdTest {

    ExtensionHelper extensionHelper;
    ListClustersCmd listClustersCmd = new ListClustersCmd();

    @Before
    public void setUp() {
        extensionHelper = mock(ExtensionHelper.class);
        listClustersCmd.extensionHelper = extensionHelper;
    }

    @Test
    public void updateClustersExtensions_emptyList_noAction() {
        listClustersCmd.updateClustersExtensions(Collections.emptyList());
        // No exception, nothing to verify
    }

    @Test
    public void updateClustersExtensions_nullList_noAction() {
        listClustersCmd.updateClustersExtensions(null);
        // No exception, nothing to verify
    }

    @Test
    public void updateClustersExtensions_withClusterResponses_setsExtension() {
        ClusterResponse cluster1 = mock(ClusterResponse.class);
        ClusterResponse cluster2 = mock(ClusterResponse.class);
        when(cluster1.getInternalId()).thenReturn(1L);
        when(cluster1.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External.name());
        when(cluster2.getInternalId()).thenReturn(2L);
        when(cluster2.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External.name());
        Extension ext1 = mock(Extension.class);
        when(ext1.getUuid()).thenReturn("a");
        Extension ext2 = mock(Extension.class);
        when(ext2.getUuid()).thenReturn("b");
        when(extensionHelper.getExtensionIdForCluster(anyLong())).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(extensionHelper.getExtension(1L)).thenReturn(ext1);
        when(extensionHelper.getExtension(2L)).thenReturn(ext2);
        List<ClusterResponse> clusters = Arrays.asList(cluster1, cluster2);
        listClustersCmd.updateClustersExtensions(clusters);
        verify(cluster1).setExtensionId("a");
        verify(cluster2).setExtensionId("b");
    }
}
