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

import com.cloud.exception.InvalidParameterValueException;
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

import static com.cloud.kubernetes.cluster.KubernetesClusterHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesClusterHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesClusterHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class CreateKubernetesClusterCmdTest {

    @Mock
    EntityManager entityManager;
    KubernetesClusterHelper helper = new KubernetesClusterHelperImpl();

    @Mock
    ServiceOffering workerServiceOffering;
    @Mock
    ServiceOffering controlServiceOffering;
    @Mock
    ServiceOffering etcdServiceOffering;

    private final CreateKubernetesClusterCmd cmd = new CreateKubernetesClusterCmd();

    private static final String workerNodesOfferingId = UUID.randomUUID().toString();
    private static final String controlNodesOfferingId = UUID.randomUUID().toString();
    private static final String etcdNodesOfferingId = UUID.randomUUID().toString();
    private static final Long workerOfferingId = 1L;
    private static final Long controlOfferingId = 2L;
    private static final Long etcdOfferingId = 3L;

    @Before
    public void setUp() {
        cmd._entityMgr = entityManager;
        cmd.kubernetesClusterHelper = helper;
        Mockito.when(entityManager.findByUuid(ServiceOffering.class, workerNodesOfferingId)).thenReturn(workerServiceOffering);
        Mockito.when(entityManager.findByUuid(ServiceOffering.class, controlNodesOfferingId)).thenReturn(controlServiceOffering);
        Mockito.when(entityManager.findByUuid(ServiceOffering.class, etcdNodesOfferingId)).thenReturn(etcdServiceOffering);
        Mockito.when(workerServiceOffering.getId()).thenReturn(workerOfferingId);
        Mockito.when(controlServiceOffering.getId()).thenReturn(controlOfferingId);
        Mockito.when(etcdServiceOffering.getId()).thenReturn(etcdOfferingId);
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
        cmd.serviceOfferingNodeTypeMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(WORKER, workerNodesOfferingId);
        Map<String, String> secondMap = createMapEntry(CONTROL, controlNodesOfferingId);
        cmd.serviceOfferingNodeTypeMap.put("map1", firstMap);
        cmd.serviceOfferingNodeTypeMap.put("map2", secondMap);
        Map<String, Long> map = cmd.getServiceOfferingNodeTypeMap();
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey(WORKER.name()) && map.containsKey(CONTROL.name()));
        Assert.assertEquals(workerOfferingId, map.get(WORKER.name()));
        Assert.assertEquals(controlOfferingId, map.get(CONTROL.name()));
    }

    @Test
    public void testNodeOfferingMapNullMap() {
        cmd.serviceOfferingNodeTypeMap = null;
        cmd.serviceOfferingId = controlOfferingId;
        Map<String, Long> map = cmd.getServiceOfferingNodeTypeMap();
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void testNodeOfferingMapEtcdNodes() {
        cmd.serviceOfferingNodeTypeMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(ETCD, etcdNodesOfferingId);
        cmd.serviceOfferingNodeTypeMap.put("map1", firstMap);
        cmd.etcdNodes = 2L;
        Map<String, Long> map = cmd.getServiceOfferingNodeTypeMap();
        Assert.assertNotNull(map);
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(ETCD.name()));
        Assert.assertEquals(etcdOfferingId, map.get(ETCD.name()));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryCompletenessInvalidParameters() {
        cmd.checkNodeTypeOfferingEntryCompleteness(WORKER.name(), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesInvalidNodeType() {
        String invalidNodeType = "invalidNodeTypeName";
        cmd.checkNodeTypeOfferingEntryValues(invalidNodeType, workerServiceOffering, workerNodesOfferingId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesEmptyOffering() {
        String nodeType = WORKER.name();
        cmd.checkNodeTypeOfferingEntryValues(nodeType, null, workerNodesOfferingId);
    }
}
