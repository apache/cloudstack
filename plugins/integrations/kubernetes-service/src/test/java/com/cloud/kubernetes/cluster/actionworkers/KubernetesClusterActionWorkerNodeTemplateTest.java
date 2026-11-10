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

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.AccountDao;

/**
 * The node templates of a cluster, including one created before its node types had templates of their own.
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterActionWorkerNodeTemplateTest {

    private static final long CLUSTER_TEMPLATE_ID = 3L;

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    KubernetesCluster kubernetesCluster;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    VMTemplateVO defaultTemplate;

    KubernetesClusterActionWorker actionWorker;

    @Before
    public void setUp() {
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.templateDao = templateDao;
        actionWorker.accountDao = Mockito.mock(AccountDao.class);
        actionWorker.dataCenterDao = Mockito.mock(DataCenterDao.class);
        actionWorker.configurationDao = Mockito.mock(ConfigurationDao.class);
        actionWorker.kubernetesSupportedVersionDao = Mockito.mock(KubernetesSupportedVersionDao.class);
        Mockito.lenient().when(templateDao.findById(CLUSTER_TEMPLATE_ID)).thenReturn(Mockito.mock(VMTemplateVO.class));
        Mockito.lenient().when(kubernetesClusterManager.getKubernetesServiceTemplate(Mockito.any(), Mockito.any(), Mockito.isNull(),
                Mockito.eq(KubernetesClusterNodeType.DEFAULT), Mockito.any())).thenReturn(defaultTemplate);
    }

    private void mockClusterTemplates(Long controlTemplateId, Long workerTemplateId, Long etcdTemplateId) {
        Mockito.lenient().when(kubernetesCluster.getTemplateId()).thenReturn(CLUSTER_TEMPLATE_ID);
        Mockito.lenient().when(kubernetesCluster.getControlNodeTemplateId()).thenReturn(controlTemplateId);
        Mockito.lenient().when(kubernetesCluster.getWorkerNodeTemplateId()).thenReturn(workerTemplateId);
        Mockito.lenient().when(kubernetesCluster.getEtcdNodeTemplateId()).thenReturn(etcdTemplateId);
    }

    @Test
    public void testInitClusterWithoutNodeTypeTemplates() {
        // a cluster created before 4.21
        mockClusterTemplates(null, null, null);

        actionWorker.init();

        Assert.assertSame(defaultTemplate, actionWorker.controlNodeTemplate);
        Assert.assertSame(defaultTemplate, actionWorker.workerNodeTemplate);
        Assert.assertSame(defaultTemplate, actionWorker.etcdTemplate);
        Assert.assertTrue(actionWorker.isDefaultTemplateUsed());
    }

    @Test
    public void testInitClusterWithNodeTypeTemplates() {
        mockClusterTemplates(5L, 6L, 7L);
        VMTemplateVO controlTemplate = Mockito.mock(VMTemplateVO.class);
        VMTemplateVO workerTemplate = Mockito.mock(VMTemplateVO.class);
        VMTemplateVO etcdTemplate = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateDao.findById(5L)).thenReturn(controlTemplate);
        Mockito.when(templateDao.findById(6L)).thenReturn(workerTemplate);
        Mockito.when(templateDao.findById(7L)).thenReturn(etcdTemplate);

        actionWorker.init();

        Assert.assertSame(defaultTemplate, actionWorker.clusterTemplate);
        Assert.assertSame(controlTemplate, actionWorker.controlNodeTemplate);
        Assert.assertSame(workerTemplate, actionWorker.workerNodeTemplate);
        Assert.assertSame(etcdTemplate, actionWorker.etcdTemplate);
        Assert.assertFalse(actionWorker.isDefaultTemplateUsed());
    }

    @Test
    public void testIsDefaultTemplateUsedByANodeType() {
        mockClusterTemplates(5L, CLUSTER_TEMPLATE_ID, null);
        Assert.assertTrue(actionWorker.isDefaultTemplateUsed());
    }
}
