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
package com.cloud.api.query.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class UserVmKubernetesClusterTypeTest {
    @Test
    public void reportsManagementTypeForUiIncludingAdoptedNodes() {
        KubernetesServiceHelper helper = mock(KubernetesServiceHelper.class);
        KubernetesCluster cluster = mock(KubernetesCluster.class);
        when(helper.findByVmId(1L)).thenReturn(cluster);
        try (MockedStatic<ComponentContext> components = mockStatic(ComponentContext.class)) {
            components.when(() -> ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class)).thenReturn(helper);
            for (KubernetesCluster.ClusterType type : KubernetesCluster.ClusterType.values()) {
                when(cluster.getClusterType()).thenReturn(type);
                UserVmResponse response = new UserVmResponse();
                new UserVmJoinDaoImpl().populateKubernetesClusterType(1L, response);
                assertEquals(type.name(), response.getKubernetesClusterType());
            }
        }
    }

    @Test
    public void ordinaryVmHasNoClusterType() {
        KubernetesServiceHelper helper = mock(KubernetesServiceHelper.class);
        try (MockedStatic<ComponentContext> components = mockStatic(ComponentContext.class)) {
            components.when(() -> ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class)).thenReturn(helper);
            UserVmResponse response = new UserVmResponse();
            new UserVmJoinDaoImpl().populateKubernetesClusterType(1L, response);
            assertNull(response.getKubernetesClusterType());
        }
    }

    @Test
    public void absentPluginHasNoClusterType() {
        try (MockedStatic<ComponentContext> components = mockStatic(ComponentContext.class)) {
            components.when(() -> ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class))
                    .thenThrow(new NoSuchBeanDefinitionException(KubernetesServiceHelper.class));
            UserVmResponse response = new UserVmResponse();
            new UserVmJoinDaoImpl().populateKubernetesClusterType(1L, response);
            assertNull(response.getKubernetesClusterType());
        }
    }
}
