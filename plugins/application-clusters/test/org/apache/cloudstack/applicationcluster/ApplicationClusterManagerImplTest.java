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
package org.apache.cloudstack.applicationcluster;

import com.cloud.capacity.CapacityManager;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterDao;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterDetailsDao;
import org.apache.cloudstack.applicationcluster.dao.ApplicationClusterVmMapDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:**/applicationClustersContext.xml"})
public class ApplicationClusterManagerImplTest {
    public static final Logger s_logger = Logger.getLogger(ApplicationClusterManagerImplTest.class);

    @Spy
    ApplicationClusterManagerImpl ccManager = new ApplicationClusterManagerImpl();

    @Mock
    ApplicationClusterDao applicationClusterDao;
    @Mock
    ApplicationClusterVmMapDao clusterVmMapDao;
    @Mock
    ApplicationClusterDetailsDao applicationClusterDetailsDao;
    @Mock
    protected SSHKeyPairDao sshKeyPairDao;
    @Mock
    public UserVmService userVmService;
    @Mock
    protected DataCenterDao dcDao;
    @Mock
    protected ServiceOfferingDao offeringDao;
    @Mock
    protected VMTemplateDao templateDao;
    @Mock
    protected AccountDao accountDao;
    @Mock
    private UserVmDao vmDao;
    @Mock
    ConfigurationDao globalConfigDao;
    @Mock
    NetworkService networkService;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    protected NetworkModel networkModel;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    protected NetworkOrchestrationService networkMgr;
    @Mock
    protected NetworkDao networkDao;
    @Mock
    private IPAddressDao publicIpAddressDao;
    @Mock
    PortForwardingRulesDao portForwardingDao;
    @Mock
    private FirewallService firewallService;
    @Mock
    public RulesService rulesService;
    @Mock
    public NetworkOfferingServiceMapDao ntwkOfferingServiceMapDao;
    @Mock
    public AccountManager accountMgr;
    @Mock
    public ApplicationClusterVmMapDao applicationClusterVmMapDao;
    @Mock
    public ServiceOfferingDao srvOfferingDao;
    @Mock
    public UserVmDao userVmDao;
    @Mock
    public CapacityManager capacityMgr;
    @Mock
    public ResourceManager resourceMgr;
    @Mock
    public ClusterDetailsDao clusterDetailsDao;
    @Mock
    public ClusterDao clusterDao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ccManager._applicationClusterDao = applicationClusterDao;
        ccManager._clusterVmMapDao = clusterVmMapDao;
        ccManager._applicationClusterDetailsDao = applicationClusterDetailsDao;
        ccManager._sshKeyPairDao = sshKeyPairDao;
        ccManager._userVmService = userVmService;
        ccManager._dcDao = dcDao;
        ccManager._offeringDao = offeringDao;
        ccManager._templateDao = templateDao;
        ccManager._accountDao = accountDao;
        ccManager._globalConfigDao = globalConfigDao;
        ccManager._networkService = networkService;
        ccManager._networkOfferingDao = networkOfferingDao;
        ccManager._networkMgr = networkMgr;
        ccManager._physicalNetworkDao = physicalNetworkDao;
        ccManager._networkMgr = networkMgr;
        ccManager._portForwardingDao = portForwardingDao;
        ccManager._rulesService = rulesService;
        ccManager._srvOfferingDao = srvOfferingDao;
        ccManager._userVmDao = userVmDao;
        ccManager._capacityMgr = capacityMgr;
        ccManager._resourceMgr = resourceMgr;
        ccManager._clusterDetailsDao = clusterDetailsDao;
        ccManager._clusterDao = clusterDao;
    }

    @Test(expected = InsufficientServerCapacityException.class)
    public void checkPlanWithNoHostInDC() throws InsufficientServerCapacityException {
        ApplicationClusterVO containerCluster = new ApplicationClusterVO();
        containerCluster.setServiceOfferingId(1L);
        containerCluster.setNodeCount(5);
        when(applicationClusterDao.findById(1L)).thenReturn(containerCluster);
        ServiceOfferingVO offering = new ServiceOfferingVO("test", 1, 500, 512, 0, 0, true, "test", null, false, true, "", true, VirtualMachine.Type.User, true);
        when(srvOfferingDao.findById(1L)).thenReturn(offering);

        List<HostVO> hl = new ArrayList<HostVO>();
        when(resourceMgr.listAllHostsInAllZonesByType(Type.Routing)).thenReturn(hl);

        ClusterVO cluster = new ClusterVO(1L);
        when(clusterDao.findById(1L)).thenReturn(cluster);

        ClusterDetailsVO cluster_detail_cpu = new ClusterDetailsVO(1L, "cpuOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio")).thenReturn(cluster_detail_cpu);
        ClusterDetailsVO cluster_detail_ram = new ClusterDetailsVO(1L, "memoryOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio")).thenReturn(cluster_detail_ram);

        when(capacityMgr.checkIfHostHasCapacity(anyLong(), anyInt(), anyInt(), anyBoolean(), anyFloat(), anyFloat(), anyBoolean())).thenReturn(true);

        ccManager.plan(1, 1);
    }

    @Test
    public void checkPlanWithHostInDC() throws InsufficientServerCapacityException {
        ApplicationClusterVO containerCluster = new ApplicationClusterVO();
        containerCluster.setServiceOfferingId(1L);
        containerCluster.setNodeCount(0);
        when(applicationClusterDao.findById(1L)).thenReturn(containerCluster);
        ServiceOfferingVO offering = new ServiceOfferingVO("test", 1, 500, 512, 0, 0, true, "test", null, false, true, "", true, null, true);
        when(srvOfferingDao.findById(1L)).thenReturn(offering);

        List<HostVO> hl = new ArrayList<HostVO>();
        HostVO h1 = new HostVO(1L, "testHost1", Type.Routing, "", "", "", "", "", "", "", "", "", "", "", "", "", Status.Up, "1.0", "", new Date(), 1L, 1L, 1L, 1L, "", 1L,
                StoragePoolType.Filesystem);
        h1.setClusterId(1L);
        h1.setUuid("uuid-test");
        hl.add(h1);
        when(resourceMgr.listAllHostsInOneZoneByType(Type.Routing, 1)).thenReturn(hl);
        ClusterVO cluster = new ClusterVO(1L);
        when(clusterDao.findById(1L)).thenReturn(cluster);

        ClusterDetailsVO cluster_detail_cpu = new ClusterDetailsVO(1L, "cpuOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio")).thenReturn(cluster_detail_cpu);
        ClusterDetailsVO cluster_detail_ram = new ClusterDetailsVO(1L, "memoryOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio")).thenReturn(cluster_detail_ram);

        when(capacityMgr.checkIfHostHasCapacity(anyLong(), anyInt(), anyInt(), anyBoolean(), anyFloat(), anyFloat(), anyBoolean())).thenReturn(true);
        when(dcDao.findById(1L)).thenReturn(new DataCenterVO(1L, "test-dc", "test-desc", "", "", "", "", "", "", 1L, NetworkType.Advanced, "", ""));

        DeployDestination dd = ccManager.plan(1, 1);

        Assert.assertEquals(dd.getDataCenter().getId(), 1L);
    }

    @Test(expected = InsufficientServerCapacityException.class)
    public void checkPlanWithHostInDCNoCapacity() throws InsufficientServerCapacityException {
        ApplicationClusterVO containerCluster = new ApplicationClusterVO();
        containerCluster.setServiceOfferingId(1L);
        containerCluster.setNodeCount(0);
        when(applicationClusterDao.findById(1L)).thenReturn(containerCluster);
        ServiceOfferingVO offering = new ServiceOfferingVO("test", 1, 500, 512, 0, 0, true, "test", null, false, true, "", true, VirtualMachine.Type.User, true);
        when(srvOfferingDao.findById(1L)).thenReturn(offering);

        List<HostVO> hl = new ArrayList<HostVO>();
        HostVO h1 = new HostVO(1L, "testHost1", Type.Routing, "", "", "", "", "", "", "", "", "", "", "", "", "", Status.Up, "1.0", "", new Date(), 1L, 1L, 1L, 1L, "", 1L,
                StoragePoolType.Filesystem);
        h1.setClusterId(1L);
        h1.setUuid("uuid-test");
        hl.add(h1);
        when(resourceMgr.listAllHostsInAllZonesByType(Type.Routing)).thenReturn(hl);

        ClusterVO cluster = new ClusterVO(1L);
        when(clusterDao.findById(1L)).thenReturn(cluster);

        ClusterDetailsVO cluster_detail_cpu = new ClusterDetailsVO(1L, "cpuOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio")).thenReturn(cluster_detail_cpu);
        ClusterDetailsVO cluster_detail_ram = new ClusterDetailsVO(1L, "memoryOvercommitRatio", "1");
        when(clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio")).thenReturn(cluster_detail_ram);

        when(capacityMgr.checkIfHostHasCapacity(anyLong(), anyInt(), anyInt(), anyBoolean(), anyFloat(), anyFloat(), anyBoolean())).thenReturn(false);
        when(dcDao.findById(1L)).thenReturn(new DataCenterVO(1L, "test-dc", "test-desc", "", "", "", "", "", "", 1L, NetworkType.Advanced, "", ""));

        DeployDestination dd = ccManager.plan(1, 1);
    }

}
