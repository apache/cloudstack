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
package org.apache.cloudstack.api.command.user.kubernetes.cluster;

import com.cloud.kubernetes.cluster.KubernetesClusterHelper;
import com.cloud.kubernetes.cluster.KubernetesClusterHelperImpl;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VmDetailConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.cloud.kubernetes.cluster.KubernetesClusterHelper.KubernetesClusterNodeType.MASTER;
import static com.cloud.kubernetes.cluster.KubernetesClusterHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class CreateKubernetesClusterCmdTest {

    @Mock
    EntityManager entityManager;
    KubernetesClusterHelper helper = new KubernetesClusterHelperImpl();

    @Mock
    ServiceOffering workerServiceOffering;
    @Mock
    ServiceOffering masterServiceOffering;

    private final CreateKubernetesClusterCmd cmd = new CreateKubernetesClusterCmd();

    private static final String workerNodesOfferingId = UUID.randomUUID().toString();
    private static final String masterNodesOfferingId = UUID.randomUUID().toString();
    private static final Long workerOfferingId = 1L;
    private static final Long masterOfferingId = 2L;

    @Before
    public void setUp() {
        cmd._entityMgr = entityManager;
        cmd.kubernetesClusterHelper = helper;
        Mockito.when(entityManager.findByUuid(ServiceOffering.class, workerNodesOfferingId)).thenReturn(workerServiceOffering);
        Mockito.when(entityManager.findByUuid(ServiceOffering.class, masterNodesOfferingId)).thenReturn(masterServiceOffering);
        Mockito.when(workerServiceOffering.getId()).thenReturn(workerOfferingId);
        Mockito.when(masterServiceOffering.getId()).thenReturn(masterOfferingId);
    }

    private Map<String, String> createMapEntry(KubernetesClusterHelper.KubernetesClusterNodeType nodeType,
                                String nodeTypeOfferingUuid) {
        Map<String, String> map = new HashMap<>();
        map.put(VmDetailConstants.CKS_NODE_TYPE, nodeType.name().toLowerCase());
        map.put(VmDetailConstants.OFFERING, nodeTypeOfferingUuid);
        return map;
    }

    @Test
    public void testNodeOfferingMap() {
        cmd.nodeTypeOfferingMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(WORKER, workerNodesOfferingId);
        Map<String, String> secondMap = createMapEntry(MASTER, masterNodesOfferingId);
        cmd.nodeTypeOfferingMap.put("map1", firstMap);
        cmd.nodeTypeOfferingMap.put("map2", secondMap);
        Map<String, Long> map = cmd.getNodeTypeOfferingMap();
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey(WORKER.name()) && map.containsKey(MASTER.name()));
        Assert.assertEquals(workerOfferingId, map.get(WORKER.name()));
        Assert.assertEquals(masterOfferingId, map.get(MASTER.name()));
    }
}
