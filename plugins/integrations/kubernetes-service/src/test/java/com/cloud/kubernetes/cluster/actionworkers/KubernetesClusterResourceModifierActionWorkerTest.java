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

import java.util.List;

import com.cloud.capacity.CapacityManager;
import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.template.VirtualMachineTemplate;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterResourceModifierActionWorkerTest {
    @Mock
    private KubernetesClusterDao kubernetesClusterDaoMock;

    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDaoMock;

    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDaoMock;

    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDaoMock;

    @Mock
    private KubernetesClusterManagerImpl kubernetesClusterManagerMock;

    @Mock
    private KubernetesCluster kubernetesClusterMock;

    private KubernetesClusterResourceModifierActionWorker kubernetesClusterResourceModifierActionWorker;

    @Before
    public void setUp() {
        kubernetesClusterManagerMock.kubernetesClusterDao = kubernetesClusterDaoMock;
        kubernetesClusterManagerMock.kubernetesSupportedVersionDao = kubernetesSupportedVersionDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterDetailsDao = kubernetesClusterDetailsDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterVmMapDao = kubernetesClusterVmMapDaoMock;

        kubernetesClusterResourceModifierActionWorker = new KubernetesClusterResourceModifierActionWorker(kubernetesClusterMock, kubernetesClusterManagerMock);
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestReturnOriginalPrefixWhenNamingAllRequirementsAreMet() {
        String originalPrefix = "k8s-cluster-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyContainLowerCaseCharacters() {
        String originalPrefix = "k8s-CLUSTER-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequired() {
        int maxPrefixLength = 43;

        String originalPrefix = "c".repeat(maxPrefixLength + 1);
        String expectedPrefix = "c".repeat(maxPrefixLength);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequiredAndWhenOriginalPrefixIsInvalid() {
        int maxPrefixLength = 43;

        String originalPrefix = "1!" + "c".repeat(maxPrefixLength);
        String expectedPrefix = "k8s-1" + "c".repeat(maxPrefixLength - 5);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyIncludeAlphanumericCharactersAndHyphen() {
        String originalPrefix = "Cluster!@#$%^&*()_+?.-01|<>";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldContainClusterUuidWhenAllCharactersAreInvalid() {
        String clusterUuid = "2699b547-cb56-4a59-a2c6-331cfb21d2e4";
        String originalPrefix = "!@#$%^&*()_+?.|<>";
        String expectedPrefix = "k8s-" + clusterUuid;

        Mockito.when(kubernetesClusterMock.getUuid()).thenReturn(clusterUuid);
        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldNotStartWithADigit() {
        String originalPrefix = "1 cluster";
        String expectedPrefix = "k8s-1cluster";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void planTestDeployDestinationShouldContainClusterOfSuitableHost() throws InsufficientServerCapacityException {
        long zoneId = 1L;
        long clusterId = 2L;
        DataCenter zone = Mockito.mock(DataCenter.class);
        Mockito.when(zone.getId()).thenReturn(zoneId);
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(2);
        Mockito.when(offering.getSpeed()).thenReturn(1000);
        Mockito.when(offering.getRamSize()).thenReturn(2048);

        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(template.getArch()).thenReturn(CPU.CPUArch.amd64);
        kubernetesClusterResourceModifierActionWorker.clusterTemplate = template;

        HostVO kvmHost = Mockito.mock(HostVO.class);
        Mockito.when(kvmHost.getUuid()).thenReturn("kvm-host");
        Mockito.when(kvmHost.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(kvmHost.getArch()).thenReturn(CPU.CPUArch.amd64);
        Mockito.when(kvmHost.getClusterId()).thenReturn(clusterId);
        HostVO vmwareHost = Mockito.mock(HostVO.class);
        Mockito.when(vmwareHost.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);

        ClusterVO kvmCluster = Mockito.mock(ClusterVO.class);
        Mockito.when(kvmCluster.getId()).thenReturn(clusterId);
        ClusterDetailsVO overcommitDetail = Mockito.mock(ClusterDetailsVO.class);
        Mockito.when(overcommitDetail.getValue()).thenReturn("1.0");

        AffinityGroupDao affinityGroupDao = Mockito.mock(AffinityGroupDao.class);
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.when(resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, zoneId)).thenReturn(List.of(vmwareHost, kvmHost));
        ClusterDao clusterDao = Mockito.mock(ClusterDao.class);
        Mockito.when(clusterDao.findById(clusterId)).thenReturn(kvmCluster);
        ClusterDetailsDao clusterDetailsDao = Mockito.mock(ClusterDetailsDao.class);
        Mockito.when(clusterDetailsDao.findDetail(Mockito.eq(clusterId), Mockito.anyString())).thenReturn(overcommitDetail);
        CapacityManager capacityManager = Mockito.mock(CapacityManager.class);
        Mockito.when(capacityManager.checkIfHostHasCapacity(Mockito.eq(kvmHost), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyBoolean(), Mockito.anyFloat(), Mockito.anyFloat(), Mockito.anyBoolean())).thenReturn(true);

        kubernetesClusterResourceModifierActionWorker.affinityGroupDao = affinityGroupDao;
        kubernetesClusterResourceModifierActionWorker.resourceManager = resourceManager;
        kubernetesClusterResourceModifierActionWorker.clusterDao = clusterDao;
        kubernetesClusterResourceModifierActionWorker.clusterDetailsDao = clusterDetailsDao;
        kubernetesClusterResourceModifierActionWorker.capacityManager = capacityManager;
        kubernetesClusterResourceModifierActionWorker.hostDao = Mockito.mock(HostDao.class);

        DeployDestination dest = kubernetesClusterResourceModifierActionWorker.plan(1, zone, offering, null, null,
                Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64);

        Assert.assertEquals(kvmCluster, dest.getCluster());
        Assert.assertNull(dest.getHost());
    }
}
