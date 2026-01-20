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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
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

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterHelperImplTest {

    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private ServiceOfferingVO workerServiceOffering;
    @Mock
    private ServiceOfferingVO controlServiceOffering;
    @Mock
    private ServiceOfferingVO etcdServiceOffering;

    private static final String workerNodesOfferingId = UUID.randomUUID().toString();
    private static final String controlNodesOfferingId = UUID.randomUUID().toString();
    private static final String etcdNodesOfferingId = UUID.randomUUID().toString();
    private static final Long workerOfferingId = 1L;
    private static final Long controlOfferingId = 2L;
    private static final Long etcdOfferingId = 3L;

    private final KubernetesServiceHelperImpl helper = new KubernetesServiceHelperImpl();

    @Before
    public void setUp() {
        helper.serviceOfferingDao = serviceOfferingDao;
        Mockito.when(serviceOfferingDao.findByUuid(workerNodesOfferingId)).thenReturn(workerServiceOffering);
        Mockito.when(serviceOfferingDao.findByUuid(controlNodesOfferingId)).thenReturn(controlServiceOffering);
        Mockito.when(serviceOfferingDao.findByUuid(etcdNodesOfferingId)).thenReturn(etcdServiceOffering);
        Mockito.when(workerServiceOffering.getId()).thenReturn(workerOfferingId);
        Mockito.when(controlServiceOffering.getId()).thenReturn(controlOfferingId);
        Mockito.when(etcdServiceOffering.getId()).thenReturn(etcdOfferingId);
    }

    @Test
    public void testIsValidNodeTypeEmptyNodeType() {
        Assert.assertFalse(helper.isValidNodeType(null));
    }

    @Test
    public void testIsValidNodeTypeInvalidNodeType() {
        String nodeType = "invalidNodeType";
        Assert.assertFalse(helper.isValidNodeType(nodeType));
    }

    @Test
    public void testIsValidNodeTypeValidNodeTypeLowercase() {
        String nodeType = KubernetesServiceHelper.KubernetesClusterNodeType.WORKER.name().toLowerCase();
        Assert.assertTrue(helper.isValidNodeType(nodeType));
    }

    private Map<String, String> createMapEntry(KubernetesServiceHelper.KubernetesClusterNodeType nodeType,
                                               String nodeTypeOfferingUuid) {
        Map<String, String> map = new HashMap<>();
        map.put(VmDetailConstants.CKS_NODE_TYPE, nodeType.name().toLowerCase());
        map.put(VmDetailConstants.OFFERING, nodeTypeOfferingUuid);
        return map;
    }

    @Test
    public void testNodeOfferingMap() {
        Map<String, Map<String, String>> serviceOfferingNodeTypeMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(WORKER, workerNodesOfferingId);
        Map<String, String> secondMap = createMapEntry(CONTROL, controlNodesOfferingId);
        serviceOfferingNodeTypeMap.put("map1", firstMap);
        serviceOfferingNodeTypeMap.put("map2", secondMap);
        Map<String, Long> map = helper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey(WORKER.name()) && map.containsKey(CONTROL.name()));
        Assert.assertEquals(workerOfferingId, map.get(WORKER.name()));
        Assert.assertEquals(controlOfferingId, map.get(CONTROL.name()));
    }

    @Test
    public void testNodeOfferingMapNullMap() {
        Map<String, Long> map = helper.getServiceOfferingNodeTypeMap(null);
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void testNodeOfferingMapEtcdNodes() {
        Map<String, Map<String, String>> serviceOfferingNodeTypeMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(ETCD, etcdNodesOfferingId);
        serviceOfferingNodeTypeMap.put("map1", firstMap);
        Map<String, Long> map = helper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);
        Assert.assertNotNull(map);
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(ETCD.name()));
        Assert.assertEquals(etcdOfferingId, map.get(ETCD.name()));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryCompletenessInvalidParameters() {
        helper.checkNodeTypeOfferingEntryCompleteness(WORKER.name(), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesInvalidNodeType() {
        String invalidNodeType = "invalidNodeTypeName";
        helper.checkNodeTypeOfferingEntryValues(invalidNodeType, workerServiceOffering, workerNodesOfferingId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesEmptyOffering() {
        String nodeType = WORKER.name();
        helper.checkNodeTypeOfferingEntryValues(nodeType, null, workerNodesOfferingId);
    }
}
