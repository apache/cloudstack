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
package com.cloud.kubernetes.cluster.actionworkers;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterDestroyWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private UserVmDao userVmDao;

    private KubernetesClusterDestroyWorker worker;

    @Before
    public void setUp() {
        worker = new KubernetesClusterDestroyWorker(kubernetesCluster, clusterManager);
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        worker.userVmDao = userVmDao;
    }

    private KubernetesClusterVmMapVO mockVmMap(long id, long vmId) {
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vmMap.getId()).thenReturn(id);
        Mockito.when(vmMap.getVmId()).thenReturn(vmId);
        return vmMap;
    }

    @Test
    public void testDestroyClusterVMsRemovesMappingsOfMissingVms() {
        KubernetesClusterVmMapVO expunged = mockVmMap(1L, 10L);
        Mockito.when(userVmDao.findById(10L)).thenReturn(null);
        worker.clusterVMs = Collections.singletonList(expunged);

        Assert.assertTrue(worker.destroyClusterVMs());

        Mockito.verify(kubernetesClusterVmMapDao).expunge(1L);
    }
}
