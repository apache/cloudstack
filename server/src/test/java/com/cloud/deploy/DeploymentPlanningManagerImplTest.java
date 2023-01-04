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
package com.cloud.deploy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.DataCenter;
import com.cloud.gpu.GPU;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMReservationDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanner.PlannerResourceUsage;
import com.cloud.deploy.dao.PlannerHostReservationDao;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.host.dao.HostDetailsDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class DeploymentPlanningManagerImplTest {

    @Spy
    @InjectMocks
    DeploymentPlanningManagerImpl _dpm;

    @Inject
    PlannerHostReservationDao _plannerHostReserveDao;

    @Inject
    VirtualMachineProfileImpl vmProfile;

    @Inject
    private AccountDao accountDao;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    ExcludeList avoids;

    @Inject
    DataCenterVO dc;

    @Inject
    DataCenterDao _dcDao;

    @Mock
    FirstFitPlanner _planner;

    @Inject
    ClusterDao _clusterDao;

    @Inject
    DedicatedResourceDao _dedicatedDao;

    @Inject
    UserVmDetailsDao vmDetailsDao;

    @Inject
    VMTemplateDao templateDao;

    @Inject
    HostPodDao hostPodDao;

    @Inject
    VolumeDao volDao;

    @Inject
    HostDao hostDao;

    @Inject
    CapacityManager capacityMgr;

    @Inject
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Inject
    ClusterDetailsDao clusterDetailsDao;

    @Mock
    Host host;

    @Mock
    ConfigurationDao configDao;

    private static final long dataCenterId = 1L;
    private static final long instanceId = 123L;
    private static final long hostId = 0L;
    private static final long podId = 2L;
    private static final long clusterId = 3L;
    private static final long ADMIN_ACCOUNT_ROLE_ID = 1L;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
    }

    @Before
    public void testSetUp() {
        MockitoAnnotations.initMocks(this);

        ComponentContext.initComponentsLifeCycle();

        PlannerHostReservationVO reservationVO = new PlannerHostReservationVO(hostId, dataCenterId, podId, clusterId, PlannerResourceUsage.Shared);
        Mockito.when(_plannerHostReserveDao.persist(Matchers.any(PlannerHostReservationVO.class))).thenReturn(reservationVO);
        Mockito.when(_plannerHostReserveDao.findById(Matchers.anyLong())).thenReturn(reservationVO);
        Mockito.when(_affinityGroupVMMapDao.countAffinityGroupsForVm(Matchers.anyLong())).thenReturn(0L);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.isDeployAsIs()).thenReturn(false);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(template);

        VMInstanceVO vm = new VMInstanceVO();
        Mockito.when(vmProfile.getVirtualMachine()).thenReturn(vm);
        Mockito.when(vmProfile.getId()).thenReturn(instanceId);

        Mockito.when(vmDetailsDao.listDetailsKeyPairs(Matchers.anyLong())).thenReturn(null);

        Mockito.when(volDao.findByInstance(Matchers.anyLong())).thenReturn(new ArrayList<>());

        Mockito.when(_dcDao.findById(Matchers.anyLong())).thenReturn(dc);
        Mockito.when(dc.getId()).thenReturn(dataCenterId);

        ClusterVO clusterVO = new ClusterVO();
        clusterVO.setHypervisorType(HypervisorType.XenServer.toString());
        Mockito.when(_clusterDao.findById(Matchers.anyLong())).thenReturn(clusterVO);

        Mockito.when(_planner.getName()).thenReturn("FirstFitPlanner");
        List<DeploymentPlanner> planners = new ArrayList<DeploymentPlanner>();
        planners.add(_planner);
        _dpm.setPlanners(planners);

        Mockito.when(host.getId()).thenReturn(hostId);
        Mockito.doNothing().when(_dpm).avoidDisabledResources(vmProfile, dc, avoids);
    }

    @Test
    public void dataCenterAvoidTest() throws InsufficientServerCapacityException, AffinityConflictException {
        ServiceOfferingVO svcOffering =
            new ServiceOfferingVO("testOffering", 1, 512, 500, 1, 1, false, false, false, "test dpm",
                    false, VirtualMachine.Type.User, null, "FirstFitPlanner", true, false);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(svcOffering);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);

        Mockito.when(avoids.shouldAvoid((DataCenterVO)Matchers.anyObject())).thenReturn(true);
        DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
        assertNull("DataCenter is in avoid set, destination should be null! ", dest);
    }

    @Test
    public void plannerCannotHandleTest() throws InsufficientServerCapacityException, AffinityConflictException {
        ServiceOfferingVO svcOffering =
            new ServiceOfferingVO("testOffering", 1, 512, 500, 1, 1, false, false, false, "test dpm",
                    false, VirtualMachine.Type.User, null, "UserDispersingPlanner", true, false);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(svcOffering);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);
        Mockito.when(avoids.shouldAvoid((DataCenterVO)Matchers.anyObject())).thenReturn(false);

        Mockito.when(_planner.canHandle(vmProfile, plan, avoids)).thenReturn(false);
        DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
        assertNull("Planner cannot handle, destination should be null! ", dest);
    }

    @Test
    public void emptyClusterListTest() throws InsufficientServerCapacityException, AffinityConflictException {
        ServiceOfferingVO svcOffering =
            new ServiceOfferingVO("testOffering", 1, 512, 500, 1, 1, false, false, false, "test dpm",
                    false, VirtualMachine.Type.User, null, "FirstFitPlanner", true, false);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(svcOffering);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);
        Mockito.when(avoids.shouldAvoid((DataCenterVO)Matchers.anyObject())).thenReturn(false);
        Mockito.when(_planner.canHandle(vmProfile, plan, avoids)).thenReturn(true);

        Mockito.when(((DeploymentClusterPlanner)_planner).orderClusters(vmProfile, plan, avoids)).thenReturn(null);
        DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
        assertNull("Planner cannot handle, destination should be null! ", dest);
    }

    @Test
    public void testCheckAffinityEmptyPreferredHosts() {
        assertTrue(_dpm.checkAffinity(host, new ArrayList<>()));
    }

    @Test
    public void testCheckAffinityNullPreferredHosts() {
        assertTrue(_dpm.checkAffinity(host, null));
    }

    @Test
    public void testCheckAffinityNotEmptyPreferredHostsContainingHost() {
        assertTrue(_dpm.checkAffinity(host, Arrays.asList(3l, 4l, hostId, 2l)));
    }

    @Test
    public void testCheckAffinityNotEmptyPreferredHostsNotContainingHost() {
        assertFalse(_dpm.checkAffinity(host, Arrays.asList(3l, 4l, 2l)));
    }

    @Test
    public void routerInDisabledResourceAssertFalse() {
        Assert.assertFalse(DeploymentPlanningManager.allowRouterOnDisabledResource.value());
    }

    @Test
    public void adminVmInDisabledResourceAssertFalse() {
        Assert.assertFalse(DeploymentPlanningManager.allowAdminVmOnDisabledResource.value());
    }

    @Test
    public void avoidDisabledResourcesTestAdminAccount() {
        Type[] vmTypes = VirtualMachine.Type.values();
        for (int i = 0; i < vmTypes.length - 1; ++i) {
            Mockito.when(vmProfile.getType()).thenReturn(vmTypes[i]);
            if (vmTypes[i].isUsedBySystem()) {
                prepareAndVerifyAvoidDisabledResourcesTest(1, 0, 0, ADMIN_ACCOUNT_ROLE_ID, vmTypes[i], true, false);
            } else {
                prepareAndVerifyAvoidDisabledResourcesTest(0, 1, 1, ADMIN_ACCOUNT_ROLE_ID, vmTypes[i], true, false);
            }
        }
    }

    @Test
    public void avoidDisabledResourcesTestUserAccounAdminCannotDeployOnDisabled() {
        Type[] vmTypes = VirtualMachine.Type.values();
        for (int i = 0; i < vmTypes.length - 1; ++i) {
            Mockito.when(vmProfile.getType()).thenReturn(vmTypes[i]);
            long userAccountId = ADMIN_ACCOUNT_ROLE_ID + 1;
            if (vmTypes[i].isUsedBySystem()) {
                prepareAndVerifyAvoidDisabledResourcesTest(1, 0, 0, userAccountId, vmTypes[i], true, false);
            } else {
                prepareAndVerifyAvoidDisabledResourcesTest(0, 0, 1, userAccountId, vmTypes[i], true, false);
            }
        }
    }

    @Test
    public void avoidDisabledResourcesTestUserAccounAdminCanDeployOnDisabled() {
        Type[] vmTypes = VirtualMachine.Type.values();
        for (int i = 0; i < vmTypes.length - 1; ++i) {
            Mockito.when(vmProfile.getType()).thenReturn(vmTypes[i]);
            long userAccountId = ADMIN_ACCOUNT_ROLE_ID + 1;
            if (vmTypes[i].isUsedBySystem()) {
                prepareAndVerifyAvoidDisabledResourcesTest(1, 0, 0, userAccountId, vmTypes[i], true, true);
            } else {
                prepareAndVerifyAvoidDisabledResourcesTest(0, 0, 1, userAccountId, vmTypes[i], true, true);
            }
        }
    }

    private void prepareAndVerifyAvoidDisabledResourcesTest(int timesRouter, int timesAdminVm, int timesDisabledResource, long roleId, Type vmType, boolean isSystemDepolyable,
            boolean isAdminVmDeployable) {
        Mockito.doReturn(isSystemDepolyable).when(_dpm).isRouterDeployableInDisabledResources();
        Mockito.doReturn(isAdminVmDeployable).when(_dpm).isAdminVmDeployableInDisabledResources();

        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        DataCenter dc = Mockito.mock(DataCenter.class);
        ExcludeList avoids = Mockito.mock(ExcludeList.class);

        Mockito.when(vmProfile.getType()).thenReturn(vmType);
        Mockito.when(vmProfile.getId()).thenReturn(1l);

        Mockito.doNothing().when(_dpm).avoidDisabledDataCenters(dc, avoids);
        Mockito.doNothing().when(_dpm).avoidDisabledPods(dc, avoids);
        Mockito.doNothing().when(_dpm).avoidDisabledClusters(dc, avoids);
        Mockito.doNothing().when(_dpm).avoidDisabledHosts(dc, avoids);

        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vmInstanceDao.findById(Mockito.anyLong())).thenReturn(vmInstanceVO);
        AccountVO owner = Mockito.mock(AccountVO.class);
        Mockito.when(owner.getRoleId()).thenReturn(roleId);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(owner);

        _dpm.avoidDisabledResources(vmProfile, dc, avoids);

        Mockito.verify(_dpm, Mockito.times(timesRouter)).isRouterDeployableInDisabledResources();
        Mockito.verify(_dpm, Mockito.times(timesAdminVm)).isAdminVmDeployableInDisabledResources();
        Mockito.verify(_dpm, Mockito.times(timesDisabledResource)).avoidDisabledDataCenters(dc, avoids);
        Mockito.verify(_dpm, Mockito.times(timesDisabledResource)).avoidDisabledPods(dc, avoids);
        Mockito.verify(_dpm, Mockito.times(timesDisabledResource)).avoidDisabledClusters(dc, avoids);
        Mockito.verify(_dpm, Mockito.times(timesDisabledResource)).avoidDisabledHosts(dc, avoids);
        Mockito.reset(_dpm);
    }

    @Test
    public void avoidDisabledDataCentersTest() {
        DataCenter dc = Mockito.mock(DataCenter.class);
        Mockito.when(dc.getId()).thenReturn(123l);

        ExcludeList avoids = new ExcludeList();
        AllocationState[] allocationStates = AllocationState.values();
        for (int i = 0; i < allocationStates.length - 1; ++i) {
            Mockito.when(dc.getAllocationState()).thenReturn(allocationStates[i]);

            _dpm.avoidDisabledDataCenters(dc, avoids);

            if (allocationStates[i] == AllocationState.Disabled) {
                assertAvoidIsEmpty(avoids, false, true, true, true);
                Assert.assertTrue(avoids.getDataCentersToAvoid().size() == 1);
                Assert.assertTrue(avoids.getDataCentersToAvoid().contains(dc.getId()));
            } else {
                assertAvoidIsEmpty(avoids, true, true, true, true);
            }
        }
    }

    @Test
    public void avoidDisabledPodsTestNoDisabledPod() {
        DataCenter dc = Mockito.mock(DataCenter.class);
        List<Long> podIds = new ArrayList<>();
        long expectedPodId = 123l;
        podIds.add(expectedPodId);
        Mockito.doReturn(new ArrayList<>()).when(hostPodDao).listDisabledPods(Mockito.anyLong());
        ExcludeList avoids = new ExcludeList();

        _dpm.avoidDisabledPods(dc, avoids);
        assertAvoidIsEmpty(avoids, true, true, true, true);
    }

    @Test
    public void avoidDisabledPodsTestHasDisabledPod() {
        DataCenter dc = Mockito.mock(DataCenter.class);
        List<Long> podIds = new ArrayList<>();
        long expectedPodId = 123l;
        podIds.add(expectedPodId);
        Mockito.doReturn(podIds).when(hostPodDao).listDisabledPods(Mockito.anyLong());

        ExcludeList avoids = new ExcludeList();

        _dpm.avoidDisabledPods(dc, avoids);
        assertAvoidIsEmpty(avoids, true, false, true, true);
        Assert.assertTrue(avoids.getPodsToAvoid().size() == 1);
        Assert.assertTrue(avoids.getPodsToAvoid().contains(expectedPodId));
    }

    @Test
    public void avoidDisabledClustersTestNoDisabledCluster() {
        DataCenter dc = prepareAvoidDisabledTests();
        Mockito.doReturn(new ArrayList<>()).when(_clusterDao).listDisabledClusters(Mockito.anyLong(), Mockito.anyLong());
        ExcludeList avoids = new ExcludeList();

        _dpm.avoidDisabledClusters(dc, avoids);
        assertAvoidIsEmpty(avoids, true, true, true, true);
    }

    @Test
    public void avoidDisabledClustersTestHasDisabledCluster() {
        DataCenter dc = prepareAvoidDisabledTests();
        long expectedClusterId = 123l;
        List<Long> disabledClusters = new ArrayList<>();
        disabledClusters.add(expectedClusterId);
        Mockito.doReturn(disabledClusters).when(_clusterDao).listDisabledClusters(Mockito.anyLong(), Mockito.anyLong());
        ExcludeList avoids = new ExcludeList();

        _dpm.avoidDisabledClusters(dc, avoids);

        assertAvoidIsEmpty(avoids, true, true, false, true);
        Assert.assertTrue(avoids.getClustersToAvoid().size() == 1);
        Assert.assertTrue(avoids.getClustersToAvoid().contains(expectedClusterId));
    }

    @Test
    public void volumesRequireEncryptionTest() {
        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path", Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        VolumeVO vol2 = new VolumeVO("vol2", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.DATADISK);
        VolumeVO vol3 = new VolumeVO("vol3", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.DATADISK);
        vol2.setPassphraseId(1L);

        List<VolumeVO> volumes = List.of(vol1, vol2, vol3);
        Assert.assertTrue("Volumes require encryption, but not reporting", _dpm.anyVolumeRequiresEncryption(volumes));
    }

    @Test
    public void volumesDoNotRequireEncryptionTest() {
        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        VolumeVO vol2 = new VolumeVO("vol2", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.DATADISK);
        VolumeVO vol3 = new VolumeVO("vol3", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.DATADISK);

        List<VolumeVO> volumes = List.of(vol1, vol2, vol3);
        Assert.assertFalse("Volumes do not require encryption, but reporting they do", _dpm.anyVolumeRequiresEncryption(volumes));
    }

    /**
     * Root requires encryption, chosen host supports it
     */
    @Test
    public void passEncRootProvidedHostSupportingEncryptionTest() {
        HostVO host = new HostVO("host");
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "true");
        }};
        host.setDetails(hostDetails);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, hostId, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertEquals(dest.getHost(), host);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Root requires encryption, chosen host does not support it
     */
    @Test
    public void failEncRootProvidedHostNotSupportingEncryptionTest() {
        HostVO host = new HostVO("host");
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "false");
        }};
        host.setDetails(hostDetails);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, hostId, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertNull("Destination should be null since host doesn't support encryption and root requires it", dest);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Root does not require encryption, chosen host does not support it
     */
    @Test
    public void passNoEncRootProvidedHostNotSupportingEncryptionTest() {
        HostVO host = new HostVO("host");
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "false");
        }};
        host.setDetails(hostDetails);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, hostId, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertEquals(dest.getHost(), host);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Root does not require encryption, chosen host does support it
     */
    @Test
    public void passNoEncRootProvidedHostSupportingEncryptionTest() {
        HostVO host = new HostVO("host");
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "true");
        }};
        host.setDetails(hostDetails);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, hostId, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertEquals(dest.getHost(), host);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Root requires encryption, last host supports it
     */
    @Test
    public void passEncRootLastHostSupportingEncryptionTest() {
        HostVO host = Mockito.spy(new HostVO("host"));
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "true");
        }};
        host.setDetails(hostDetails);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        VMInstanceVO vm = (VMInstanceVO) vmProfile.getVirtualMachine();
        vm.setLastHostId(hostId);

        // host id is null here so we pick up last host id
        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, null, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertEquals(dest.getHost(), host);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Root requires encryption, last host does not support it
     */
    @Test
    public void failEncRootLastHostNotSupportingEncryptionTest() {
        HostVO host = Mockito.spy(new HostVO("host"));
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "false");
        }};
        host.setDetails(hostDetails);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        setupMocksForPlanDeploymentHostTests(host, vol1);

        VMInstanceVO vm = (VMInstanceVO) vmProfile.getVirtualMachine();
        vm.setLastHostId(hostId);
        // host id is null here so we pick up last host id
        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, null, null, null);
        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, null);
            Assert.assertNull("Destination should be null since last host doesn't support encryption and root requires it", dest);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void passEncRootPlannerHostSupportingEncryptionTest() {
        HostVO host = Mockito.spy(new HostVO("host"));
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "true");
        }};
        host.setDetails(hostDetails);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        DeploymentClusterPlanner planner = setupMocksForPlanDeploymentHostTests(host, vol1);

        // host id is null here so we pick up last host id
        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, null, null, null);

        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, planner);
            Assert.assertEquals(host, dest.getHost());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void failEncRootPlannerHostSupportingEncryptionTest() {
        HostVO host = Mockito.spy(new HostVO("host"));
        Map<String, String> hostDetails = new HashMap<>() {{
            put(Host.HOST_VOLUME_ENCRYPTION, "false");
        }};
        host.setDetails(hostDetails);
        Mockito.when(host.getStatus()).thenReturn(Status.Up);

        VolumeVO vol1 = new VolumeVO("vol1", dataCenterId,podId,1L,1L, instanceId,"folder","path",Storage.ProvisioningType.THIN, (long)10<<30, Volume.Type.ROOT);
        vol1.setPassphraseId(1L);

        DeploymentClusterPlanner planner = setupMocksForPlanDeploymentHostTests(host, vol1);

        // host id is null here so we pick up last host id
        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId, podId, clusterId, null, null, null);

        try {
            DeployDestination dest = _dpm.planDeployment(vmProfile, plan, avoids, planner);
            Assert.assertNull("Destination should be null since last host doesn't support encryption and root requires it", dest);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // This is so ugly but everything is so intertwined...
    private DeploymentClusterPlanner setupMocksForPlanDeploymentHostTests(HostVO host, VolumeVO vol1) {
        long diskOfferingId = 345L;
        List<VolumeVO> volumeVOs = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vol1.setDiskOfferingId(diskOfferingId);
        volumes.add(vol1);
        volumeVOs.add(vol1);

        DiskOfferingVO diskOffering = new DiskOfferingVO();
        diskOffering.setEncrypt(true);

        VMTemplateVO template = new VMTemplateVO();
        template.setFormat(Storage.ImageFormat.QCOW2);

        host.setClusterId(clusterId);

        StoragePool pool = new StoragePoolVO();

        Map<Volume, List<StoragePool>> suitableVolumeStoragePools = new HashMap<>() {{
            put(vol1, List.of(pool));
        }};

        Pair<Map<Volume, List<StoragePool>>, List<Volume>> suitable = new Pair<>(suitableVolumeStoragePools, volumes);

        ServiceOfferingVO svcOffering = new ServiceOfferingVO("test", 1, 256, 1, 1, 1, false, "vm", false, Type.User, false);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(svcOffering);
        Mockito.when(vmProfile.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.doNothing().when(hostDao).loadDetails(host);
        Mockito.doReturn(volumeVOs).when(volDao).findByInstance(ArgumentMatchers.anyLong());
        Mockito.doReturn(suitable).when(_dpm).findSuitablePoolsForVolumes(
            ArgumentMatchers.any(VirtualMachineProfile.class),
            ArgumentMatchers.any(DataCenterDeployment.class),
            ArgumentMatchers.any(ExcludeList.class),
            ArgumentMatchers.anyInt()
        );

        ClusterVO clusterVO = new ClusterVO();
        clusterVO.setHypervisorType(HypervisorType.KVM.toString());
        Mockito.when(_clusterDao.findById(ArgumentMatchers.anyLong())).thenReturn(clusterVO);

        Mockito.doReturn(List.of(host)).when(_dpm).findSuitableHosts(
            ArgumentMatchers.any(VirtualMachineProfile.class),
            ArgumentMatchers.any(DeploymentPlan.class),
            ArgumentMatchers.any(ExcludeList.class),
            ArgumentMatchers.anyInt()
        );

        Map<Volume, StoragePool> suitableVolumeStoragePoolMap = new HashMap<>() {{
            put(vol1, pool);
        }};
        Mockito.doReturn(true).when(_dpm).hostCanAccessSPool(ArgumentMatchers.any(Host.class), ArgumentMatchers.any(StoragePool.class));

        Pair<Host, Map<Volume, StoragePool>> potentialResources = new Pair<>(host, suitableVolumeStoragePoolMap);

        Mockito.when(capacityMgr.checkIfHostReachMaxGuestLimit(host)).thenReturn(false);
        Mockito.when(capacityMgr.checkIfHostHasCpuCapability(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(true);
        Mockito.when(capacityMgr.checkIfHostHasCapacity(
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyInt(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyFloat(),
            ArgumentMatchers.anyFloat(),
            ArgumentMatchers.anyBoolean()
        )).thenReturn(true);
        Mockito.when(serviceOfferingDetailsDao.findDetail(vmProfile.getServiceOfferingId(), GPU.Keys.vgpuType.toString())).thenReturn(null);

        Mockito.doReturn(true).when(_dpm).checkVmProfileAndHost(vmProfile, host);
        Mockito.doReturn(true).when(_dpm).checkIfHostFitsPlannerUsage(ArgumentMatchers.anyLong(), ArgumentMatchers.nullable(PlannerResourceUsage.class));
        Mockito.when(clusterDetailsDao.findDetail(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(new ClusterDetailsVO(clusterId, "mock", "1"));

        DeploymentClusterPlanner planner = Mockito.spy(new FirstFitPlanner());
        try {
            Mockito.doReturn(List.of(clusterId), List.of()).when(planner).orderClusters(
                ArgumentMatchers.any(VirtualMachineProfile.class),
                ArgumentMatchers.any(DeploymentPlan.class),
                ArgumentMatchers.any(ExcludeList.class)
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return planner;
    }

    private DataCenter prepareAvoidDisabledTests() {
        DataCenter dc = Mockito.mock(DataCenter.class);
        Mockito.when(dc.getId()).thenReturn(123l);
        List<Long> podIds = new ArrayList<>();
        podIds.add(1l);
        Mockito.doReturn(podIds).when(hostPodDao).listAllPods(Mockito.anyLong());
        return dc;
    }

    private void assertAvoidIsEmpty(ExcludeList avoids, boolean isDcEmpty, boolean isPodsEmpty, boolean isClustersEmpty, boolean isHostsEmpty) {
        Assert.assertEquals(isDcEmpty, CollectionUtils.isEmpty(avoids.getDataCentersToAvoid()));
        Assert.assertEquals(isPodsEmpty, CollectionUtils.isEmpty(avoids.getPodsToAvoid()));
        Assert.assertEquals(isClustersEmpty, CollectionUtils.isEmpty(avoids.getClustersToAvoid()));
        Assert.assertEquals(isHostsEmpty, CollectionUtils.isEmpty(avoids.getHostsToAvoid()));
    }

    @Configuration
    @ComponentScan(basePackageClasses = {DeploymentPlanningManagerImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class,
                                                                                                         type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public FirstFitPlanner firstFitPlanner() {
            return Mockito.mock(FirstFitPlanner.class);
        }

        @Bean
        public DeploymentPlanner deploymentPlanner() {
            return Mockito.mock(DeploymentPlanner.class);
        }

        @Bean
        public DataCenterVO dataCenter() {
            return Mockito.mock(DataCenterVO.class);
        }

        @Bean
        public ExcludeList excludeList() {
            return Mockito.mock(ExcludeList.class);
        }

        @Bean
        public VirtualMachineProfileImpl virtualMachineProfileImpl() {
            return Mockito.mock(VirtualMachineProfileImpl.class);
        }

        @Bean
        public HostTagsDao hostTagsDao() {
            return Mockito.mock(HostTagsDao.class);
        }

        @Bean
        public HostDetailsDao hostDetailsDao() {
            return Mockito.mock(HostDetailsDao.class);
        }


        @Bean
        public ClusterDetailsDao clusterDetailsDao() {
            return Mockito.mock(ClusterDetailsDao.class);
        }

        @Bean
        public ResourceManager resourceManager() {
            return Mockito.mock(ResourceManager.class);
        }

        @Bean
        public ServiceOfferingDetailsDao serviceOfferingDetailsDao() {
            return Mockito.mock(ServiceOfferingDetailsDao.class);
        }

        @Bean
        public AffinityGroupDomainMapDao affinityGroupDomainMapDao() {
            return Mockito.mock(AffinityGroupDomainMapDao.class);
        }

        @Bean
        public DataStoreManager cataStoreManager() {
            return Mockito.mock(DataStoreManager.class);
        }

        @Bean
        public StorageManager storageManager() {
            return Mockito.mock(StorageManager.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public HostPodDao hostPodDao() {
            return Mockito.mock(HostPodDao.class);
        }

        @Bean
        public ClusterDao clusterDao() {
            return Mockito.mock(ClusterDao.class);
        }

        @Bean
        public DedicatedResourceDao dedicatedResourceDao() {
            return Mockito.mock(DedicatedResourceDao.class);
        }

        @Bean
        public GuestOSDao guestOSDao() {
            return Mockito.mock(GuestOSDao.class);
        }

        @Bean
        public GuestOSCategoryDao guestOSCategoryDao() {
            return Mockito.mock(GuestOSCategoryDao.class);
        }

        @Bean
        public CapacityManager capacityManager() {
            return Mockito.mock(CapacityManager.class);
        }

        @Bean
        public StoragePoolHostDao storagePoolHostDao() {
            return Mockito.mock(StoragePoolHostDao.class);
        }

        @Bean
        public VolumeDao volumeDao() {
            return Mockito.mock(VolumeDao.class);
        }

        @Bean
        public ConfigurationDao configurationDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        @Bean
        public DiskOfferingDao diskOfferingDao() {
            return Mockito.mock(DiskOfferingDao.class);
        }

        @Bean
        public PrimaryDataStoreDao primaryDataStoreDao() {
            return Mockito.mock(PrimaryDataStoreDao.class);
        }

        @Bean
        public CapacityDao capacityDao() {
            return Mockito.mock(CapacityDao.class);
        }

        @Bean
        public PlannerHostReservationDao plannerHostReservationDao() {
            return Mockito.mock(PlannerHostReservationDao.class);
        }

        @Bean
        public AffinityGroupProcessor affinityGroupProcessor() {
            return Mockito.mock(AffinityGroupProcessor.class);
        }

        @Bean
        public AffinityGroupDao affinityGroupDao() {
            return Mockito.mock(AffinityGroupDao.class);
        }

        @Bean
        public AffinityGroupVMMapDao affinityGroupVMMapDao() {
            return Mockito.mock(AffinityGroupVMMapDao.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public AgentManager agentManager() {
            return Mockito.mock(AgentManager.class);
        }

        @Bean
        public MessageBus messageBus() {
            return Mockito.mock(MessageBus.class);
        }

        @Bean
        public UserVmDao userVMDao() {
            return Mockito.mock(UserVmDao.class);
        }

        @Bean
        public UserVmDetailsDao userVmDetailsDao() {
            return Mockito.mock(UserVmDetailsDao.class);
        }

        @Bean
        public VMInstanceDao vmInstanceDao() {
            return Mockito.mock(VMInstanceDao.class);
        }

        @Bean
        public DataCenterDao dataCenterDao() {
            return Mockito.mock(DataCenterDao.class);
        }

        @Bean
        public VMReservationDao reservationDao() {
            return Mockito.mock(VMReservationDao.class);
        }

        @Bean
        public AffinityGroupService affinityGroupService() {
            return Mockito.mock(AffinityGroupService.class);
        }

        @Bean
        public HostGpuGroupsDao hostGpuGroupsDao() {
            return Mockito.mock(HostGpuGroupsDao.class);
        }

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public VMTemplateDao vmTemplateDao() {
            return Mockito.mock(VMTemplateDao.class);
        }

        public static class Library implements TypeFilter {

            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }

    @Test
    public void testReorderHostsByPriority() {
        Map<Long, Integer> priorities = new LinkedHashMap<>();
        priorities.put(1L, 3);
        priorities.put(2L, -6);
        priorities.put(3L, 5);
        priorities.put(5L, 8);
        priorities.put(6L, -1);
        priorities.put(8L, 5);
        priorities.put(9L, DataCenterDeployment.PROHIBITED_HOST_PRIORITY);

        Host host1 = Mockito.mock(Host.class);
        Mockito.when(host1.getId()).thenReturn(1L);
        Host host2 = Mockito.mock(Host.class);
        Mockito.when(host2.getId()).thenReturn(2L);
        Host host3 = Mockito.mock(Host.class);
        Mockito.when(host3.getId()).thenReturn(3L);
        Host host4 = Mockito.mock(Host.class);
        Mockito.when(host4.getId()).thenReturn(4L);
        Host host5 = Mockito.mock(Host.class);
        Mockito.when(host5.getId()).thenReturn(5L);
        Host host6 = Mockito.mock(Host.class);
        Mockito.when(host6.getId()).thenReturn(6L);
        Host host7 = Mockito.mock(Host.class);
        Mockito.when(host7.getId()).thenReturn(7L);
        Host host8 = Mockito.mock(Host.class);
        Mockito.when(host8.getId()).thenReturn(8L);
        Host host9 = Mockito.mock(Host.class);
        Mockito.when(host9.getId()).thenReturn(9L);

        List<Host> hosts = new ArrayList<>(Arrays.asList(host1, host2, host3, host4, host5, host6, host7, host8, host9));
        _dpm.reorderHostsByPriority(priorities, hosts);

        Assert.assertEquals(8, hosts.size());

        Assert.assertEquals(5, hosts.get(0).getId());
        Assert.assertEquals(3, hosts.get(1).getId());
        Assert.assertEquals(8, hosts.get(2).getId());
        Assert.assertEquals(1, hosts.get(3).getId());
        Assert.assertEquals(4, hosts.get(4).getId());
        Assert.assertEquals(7, hosts.get(5).getId());
        Assert.assertEquals(6, hosts.get(6).getId());
        Assert.assertEquals(2, hosts.get(7).getId());
    }
}
