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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VmDetailConstants;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesServiceHelperImplTest {
    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    KubernetesClusterDao kubernetesClusterDao;
    @Mock
    AffinityGroupDao affinityGroupDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;

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

    @Test
    public void testIsValidNodeTypeEmptyNodeType() {
        Assert.assertFalse(kubernetesServiceHelper.isValidNodeType(null));
    }

    @Test
    public void testIsValidNodeTypeInvalidNodeType() {
        Assert.assertFalse(kubernetesServiceHelper.isValidNodeType("invalidNodeType"));
    }

    @Test
    public void testIsValidNodeTypeValidNodeTypeLowercase() {
        String nodeType = KubernetesClusterNodeType.WORKER.name().toLowerCase();
        Assert.assertTrue(kubernetesServiceHelper.isValidNodeType(nodeType));
    }

    private Map<String, String> createServiceOfferingMapEntry(KubernetesClusterNodeType nodeType, String offeringUuid) {
        Map<String, String> map = new HashMap<>();
        map.put(VmDetailConstants.CKS_NODE_TYPE, nodeType.name().toLowerCase());
        map.put(VmDetailConstants.OFFERING, offeringUuid);
        return map;
    }

    @Test
    public void testGetServiceOfferingNodeTypeMap() {
        String workerOfferingUuid = UUID.randomUUID().toString();
        String controlOfferingUuid = UUID.randomUUID().toString();

        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(workerOffering.getId()).thenReturn(1L);
        Mockito.when(serviceOfferingDao.findByUuid(workerOfferingUuid)).thenReturn(workerOffering);

        ServiceOfferingVO controlOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(controlOffering.getId()).thenReturn(2L);
        Mockito.when(serviceOfferingDao.findByUuid(controlOfferingUuid)).thenReturn(controlOffering);

        Map<String, Map<String, String>> serviceOfferingNodeTypeMap = new HashMap<>();
        serviceOfferingNodeTypeMap.put("map1", createServiceOfferingMapEntry(KubernetesClusterNodeType.WORKER, workerOfferingUuid));
        serviceOfferingNodeTypeMap.put("map2", createServiceOfferingMapEntry(KubernetesClusterNodeType.CONTROL, controlOfferingUuid));

        Map<String, Long> result = kubernetesServiceHelper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsKey(KubernetesClusterNodeType.WORKER.name()));
        Assert.assertTrue(result.containsKey(KubernetesClusterNodeType.CONTROL.name()));
        Assert.assertEquals(Long.valueOf(1L), result.get(KubernetesClusterNodeType.WORKER.name()));
        Assert.assertEquals(Long.valueOf(2L), result.get(KubernetesClusterNodeType.CONTROL.name()));
    }

    @Test
    public void testGetServiceOfferingNodeTypeMapNullMap() {
        Map<String, Long> result = kubernetesServiceHelper.getServiceOfferingNodeTypeMap(null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetServiceOfferingNodeTypeMapEtcdNodes() {
        String etcdOfferingUuid = UUID.randomUUID().toString();

        ServiceOfferingVO etcdOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(etcdOffering.getId()).thenReturn(3L);
        Mockito.when(serviceOfferingDao.findByUuid(etcdOfferingUuid)).thenReturn(etcdOffering);

        Map<String, Map<String, String>> serviceOfferingNodeTypeMap = new HashMap<>();
        serviceOfferingNodeTypeMap.put("map1", createServiceOfferingMapEntry(KubernetesClusterNodeType.ETCD, etcdOfferingUuid));

        Map<String, Long> result = kubernetesServiceHelper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey(KubernetesClusterNodeType.ETCD.name()));
        Assert.assertEquals(Long.valueOf(3L), result.get(KubernetesClusterNodeType.ETCD.name()));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryCompletenessInvalidParameters() {
        kubernetesServiceHelper.checkNodeTypeOfferingEntryCompleteness(KubernetesClusterNodeType.WORKER.name(), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesInvalidNodeType() {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        kubernetesServiceHelper.checkNodeTypeOfferingEntryValues("invalidNodeTypeName", offering, "some-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesEmptyOffering() {
        kubernetesServiceHelper.checkNodeTypeOfferingEntryValues(KubernetesClusterNodeType.WORKER.name(), null, "some-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeAffinityGroupEntryCompletenessBlankNodeType() {
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryCompleteness("", "affinity-group-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeAffinityGroupEntryCompletenessBlankAffinityGroupUuid() {
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryCompleteness("control", "");
    }

    @Test
    public void testCheckNodeTypeAffinityGroupEntryCompletenessValid() {
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryCompleteness("control", "affinity-group-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeAffinityGroupEntryValuesInvalidNodeType() {
        AffinityGroup affinityGroup = Mockito.mock(AffinityGroup.class);
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryValues("invalid-node-type", affinityGroup, "affinity-group-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeAffinityGroupEntryValuesNullAffinityGroup() {
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryValues("control", null, "affinity-group-uuid");
    }

    @Test
    public void testCheckNodeTypeAffinityGroupEntryValuesValid() {
        AffinityGroup affinityGroup = Mockito.mock(AffinityGroup.class);
        kubernetesServiceHelper.checkNodeTypeAffinityGroupEntryValues("control", affinityGroup, "affinity-group-uuid");
    }

    @Test
    public void testAddNodeTypeAffinityGroupEntry() {
        AffinityGroup affinityGroup = Mockito.mock(AffinityGroup.class);
        Mockito.when(affinityGroup.getId()).thenReturn(100L);
        Map<String, Long> mapping = new HashMap<>();
        kubernetesServiceHelper.addNodeTypeAffinityGroupEntry("control", "affinity-group-uuid", affinityGroup, mapping);
        Assert.assertEquals(1, mapping.size());
        Assert.assertEquals(Long.valueOf(100L), mapping.get("CONTROL"));
    }

    @Test
    public void testProcessNodeTypeAffinityGroupEntryAndAddToMappingIfValidEmptyEntry() {
        Map<String, Long> mapping = new HashMap<>();
        kubernetesServiceHelper.processNodeTypeAffinityGroupEntryAndAddToMappingIfValid(new HashMap<>(), mapping);
        Assert.assertTrue(mapping.isEmpty());
    }

    @Test
    public void testProcessNodeTypeAffinityGroupEntryAndAddToMappingIfValidValidEntry() {
        AffinityGroupVO affinityGroup = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(affinityGroup.getId()).thenReturn(100L);
        Mockito.when(affinityGroupDao.findByUuid("affinity-group-uuid")).thenReturn(affinityGroup);

        Map<String, String> entry = new HashMap<>();
        entry.put(VmDetailConstants.CKS_NODE_TYPE, "control");
        entry.put(VmDetailConstants.AFFINITY_GROUP, "affinity-group-uuid");

        Map<String, Long> mapping = new HashMap<>();
        kubernetesServiceHelper.processNodeTypeAffinityGroupEntryAndAddToMappingIfValid(entry, mapping);
        Assert.assertEquals(1, mapping.size());
        Assert.assertEquals(Long.valueOf(100L), mapping.get("CONTROL"));
    }

    @Test
    public void testGetAffinityGroupNodeTypeMapEmptyMap() {
        Map<String, Long> result = kubernetesServiceHelper.getAffinityGroupNodeTypeMap(null);
        Assert.assertTrue(result.isEmpty());

        result = kubernetesServiceHelper.getAffinityGroupNodeTypeMap(new HashMap<>());
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAffinityGroupNodeTypeMapValidEntries() {
        AffinityGroupVO controlAffinityGroup = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(controlAffinityGroup.getId()).thenReturn(100L);
        Mockito.when(affinityGroupDao.findByUuid("control-affinity-uuid")).thenReturn(controlAffinityGroup);

        AffinityGroupVO workerAffinityGroup = Mockito.mock(AffinityGroupVO.class);
        Mockito.when(workerAffinityGroup.getId()).thenReturn(200L);
        Mockito.when(affinityGroupDao.findByUuid("worker-affinity-uuid")).thenReturn(workerAffinityGroup);

        Map<String, Map<String, String>> affinityGroupNodeTypeMap = new HashMap<>();

        Map<String, String> controlEntry = new HashMap<>();
        controlEntry.put(VmDetailConstants.CKS_NODE_TYPE, "control");
        controlEntry.put(VmDetailConstants.AFFINITY_GROUP, "control-affinity-uuid");
        affinityGroupNodeTypeMap.put("0", controlEntry);

        Map<String, String> workerEntry = new HashMap<>();
        workerEntry.put(VmDetailConstants.CKS_NODE_TYPE, "worker");
        workerEntry.put(VmDetailConstants.AFFINITY_GROUP, "worker-affinity-uuid");
        affinityGroupNodeTypeMap.put("1", workerEntry);

        Map<String, Long> result = kubernetesServiceHelper.getAffinityGroupNodeTypeMap(affinityGroupNodeTypeMap);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(100L), result.get("CONTROL"));
        Assert.assertEquals(Long.valueOf(200L), result.get("WORKER"));
    }
}
