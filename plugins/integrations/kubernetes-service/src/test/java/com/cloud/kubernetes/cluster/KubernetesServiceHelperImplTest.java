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
package com.cloud.kubernetes.cluster;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesServiceHelperImplTest {
    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @InjectMocks
    KubernetesServiceHelperImpl kubernetesServiceHelper = new KubernetesServiceHelperImpl();

    @Test
    public void testCheckVmCanBeDestroyedNotCKSNode() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getUserVmType()).thenReturn("");
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).findByVmId(Mockito.anyLong());
    }

    @Test
    public void testCheckVmCanBeDestroyedNotInCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(null);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckVmCanBeDestroyedInCloudManagedCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.getClusterId()).thenReturn(1L);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(map);
        KubernetesClusterVO kubernetesCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(kubernetesClusterDao.findById(1L)).thenReturn(kubernetesCluster);
        Mockito.when(kubernetesCluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }

    @Test
    public void testCheckVmCanBeDestroyedInExternalManagedCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.getClusterId()).thenReturn(1L);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(map);
        KubernetesClusterVO kubernetesCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(kubernetesClusterDao.findById(1L)).thenReturn(kubernetesCluster);
        Mockito.when(kubernetesCluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }
}
