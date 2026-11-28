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
package com.cloud.kubernetes.cluster.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterDaoImplTest {

    @Spy
    KubernetesClusterDaoImpl kubernetesClusterDao;

    private boolean updateStateWithRowsUpdated(int rows, KubernetesClusterVO cluster, UpdateBuilder builder) {
        Mockito.doReturn(rows).when(kubernetesClusterDao).update(Mockito.eq(builder), Mockito.any(SearchCriteria.class), Mockito.isNull());
        try (MockedStatic<GenericDaoBase> dao = Mockito.mockStatic(GenericDaoBase.class)) {
            dao.when(() -> GenericDaoBase.getUpdateBuilder(cluster)).thenReturn(builder);
            return kubernetesClusterDao.updateState(KubernetesCluster.State.Running, KubernetesCluster.Event.ScaleUpRequested,
                    KubernetesCluster.State.Scaling, cluster, null);
        }
    }

    @Test
    public void updateStateSetsStateOnlyWhereClusterIsStillInCurrentState() {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        UpdateBuilder builder = Mockito.mock(UpdateBuilder.class);

        Assert.assertTrue(updateStateWithRowsUpdated(1, cluster, builder));

        Mockito.verify(builder).set(cluster, "state", KubernetesCluster.State.Scaling);
        Mockito.verify(kubernetesClusterDao).update(Mockito.eq(builder), Mockito.argThat(sc -> {
            String where = sc.getWhereClause();
            return where.contains("id") && where.contains("state");
        }), Mockito.isNull());
    }

    @Test
    public void updateStateFailsWhenClusterLeftCurrentState() {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.lenient().doReturn(null).when(kubernetesClusterDao).findByIdIncludingRemoved(1L);

        UpdateBuilder builder = Mockito.mock(UpdateBuilder.class);
        Assert.assertFalse(updateStateWithRowsUpdated(0, cluster, builder));

        // the cluster object gets back the state it still has, with no change left for a later update
        org.mockito.InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).set(cluster, "state", KubernetesCluster.State.Scaling);
        inOrder.verify(builder).set(cluster, "state", KubernetesCluster.State.Running);
        inOrder.verify(builder).clear();
    }
}
