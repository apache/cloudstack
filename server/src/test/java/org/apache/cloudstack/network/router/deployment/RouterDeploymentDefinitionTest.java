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
package org.apache.cloudstack.network.router.deployment;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.user.Account;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RouterDeploymentDefinitionTest extends RouterDeploymentDefinitionTestBase {

    @Mock
    protected NetworkVO mockNw;
    @Mock
    protected NsxProviderDao nsxProviderDao;

    protected RouterDeploymentDefinition deployment;


    @Override
    protected void initMocks() {
        when(mockDestination.getDataCenter()).thenReturn(mockDataCenter);
        when(mockDataCenter.getId()).thenReturn(DATA_CENTER_ID);
        when(mockPod.getId()).thenReturn(POD_ID1);
        when(mockHostPodVO1.getId()).thenReturn(POD_ID1);
        when(mockHostPodVO2.getId()).thenReturn(POD_ID2);
        when(mockHostPodVO3.getId()).thenReturn(POD_ID3);
        when(mockNw.getId()).thenReturn(NW_ID_1);
    }

    @Before
    public void initTest() {
        initMocks();

        deployment = builder.create()
                .setGuestNetwork(mockNw)
                .setDeployDestination(mockDestination)
                .setAccountOwner(mockOwner)
                .setParams(params)
                .build();
    }

    @Test
    public void testRedundancyProperty() {
        // Set and confirm is redundant
        when(mockNw.isRedundant()).thenReturn(true);
        final RouterDeploymentDefinition deployment = builder.create()
                .setGuestNetwork(mockNw)
                .setDeployDestination(mockDestination)
                .build();
        assertTrue("The builder ignored redundancy from its inner network", deployment.isRedundant());
        when(mockNw.isRedundant()).thenReturn(false);
        assertFalse("The builder ignored redundancy from its inner network", deployment.isRedundant());
    }

    @Test
    public void testConstructionFieldsAndFlags() {
        // Vpc type
        assertFalse(deployment.isVpcRouter());
        // Offering null
        deployment.serviceOfferingId = null;
        assertNull(deployment.getServiceOfferingId());
        deployment.serviceOfferingId = OFFERING_ID;
        assertEquals(OFFERING_ID, deployment.getServiceOfferingId().longValue());
        assertNotNull(deployment.getRouters());
        assertNotNull(deployment.getGuestNetwork());
        assertNotNull(deployment.getDest());
        assertNotNull(deployment.getOwner());
        deployment.plan = mock(DeploymentPlan.class);
        assertNotNull(deployment.getPlan());
        assertFalse(deployment.isPublicNetwork());
        deployment.isPublicNetwork = true;
        assertTrue(deployment.isPublicNetwork());
        // This could never be a Vpc deployment
        assertNull(deployment.getVpc());
        assertEquals(params, deployment.getParams());
    }

    @Test
    public void testLock() {
        // Prepare
        when(mockNwDao.acquireInLockTable(NW_ID_1, NetworkOrchestrationService.NetworkLockTimeout.value()))
        .thenReturn(mockNw);

        // Execute
        deployment.lock();

        // Assert
        verify(mockNwDao, times(1)).acquireInLockTable(NW_ID_1, 600);
        assertNotNull(LOCK_NOT_CORRECTLY_GOT, deployment.tableLockId);
        assertEquals(LOCK_NOT_CORRECTLY_GOT, NW_ID_1, NW_ID_1, deployment.tableLockId.longValue());
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testLockFails() {
        // Prepare
        when(mockNwDao.acquireInLockTable(NW_ID_1, NetworkOrchestrationService.NetworkLockTimeout.value()))
        .thenReturn(null);

        // Execute
        try {
            deployment.lock();
        } finally {
            // Assert
            verify(mockNwDao, times(1)).acquireInLockTable(NW_ID_1, 600);
            assertNull(deployment.tableLockId);
        }

    }

    @Test
    public void testUnlock() {
        // Prepare
        deployment.tableLockId = NW_ID_1;

        // Execute
        deployment.unlock();

        // Assert
        verify(mockNwDao, times(1)).releaseFromLockTable(NW_ID_1);
    }

    @Test
    public void testUnlockWithoutLock() {
        // Prepare
        deployment.tableLockId = null;

        // Execute
        deployment.unlock();

        // Assert
        verify(mockNwDao, times(0)).releaseFromLockTable(anyLong());
    }

    /**
     * If it's not a basic network, pod is not needed in the generated DataCenterDeployment
     */
    @Test
    public void testGenerateDeploymentPlanNoPodNeeded() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);

        // Execute
        deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, deployment.dest);
        assertEquals("", null, deployment.getPod());
        assertEquals("", null, deployment.getPodId());
    }

    /**
     * If it's Basic, it should have pod
     */
    @Test
    public void testGenerateDeploymentPlanBasic() {
        // Prepare
        when(mockDestination.getPod()).thenReturn(mockPod);
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);

        // Execute
        deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, deployment.dest);
        assertEquals("", mockPod, deployment.getPod());
        assertEquals("", POD_ID1, deployment.getPodId());
    }

    /**
     * If it's Basic, it should have pod, otherwise fail with
     * {@link CloudRuntimeException}
     */
    @Test(expected = CloudRuntimeException.class)
    public void testGenerateDeploymentPlanBasicFailNoPod() {
        // Prepare
        when(mockDestination.getPod()).thenReturn(null);
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);

        // Execute
        deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, deployment.dest);
    }

    @Test
    public void testCheckPreconditions() throws ResourceUnavailableException {
        // Prepare
        final Network.State states[] = {
                Network.State.Implemented,
                Network.State.Setup,
                Network.State.Implementing
        };
        when(deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Guest);

        // Drive specific tests
        for (final Network.State state : states) {
            driveTestCheckPreconditionsCorrectNwState(state);
        }
    }

    public void driveTestCheckPreconditionsCorrectNwState(final Network.State state) throws ResourceUnavailableException {
        // Prepare
        when(deployment.guestNetwork.getState()).thenReturn(state);

        // Execute
        deployment.checkPreconditions();

        // Assert : It just should raise no exceptions
    }

    @Test(expected = ResourceUnavailableException.class)
    public void testCheckPreconditionsWrongTrafficType() throws ResourceUnavailableException {
        // Prepare wrong traffic type to trigger error
        when(deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Public);

        // Execute
        driveTestCheckPreconditionsCorrectNwState(Network.State.Implemented);
    }

    @Test(expected = ResourceUnavailableException.class)
    public void testCheckPreconditionsWrongState() throws ResourceUnavailableException {
        // Prepare wrong traffic type to trigger error
        lenient().when(deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Guest);

        // Execute
        driveTestCheckPreconditionsCorrectNwState(Network.State.Shutdown);
    }

    @Test
    public void testFindDestinationsNonBasicZone() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);

        // Execute
        final List<DeployDestination> destinations = deployment.findDestinations();

        // Assert
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                mockDestination, destinations.get(0));
    }

    @Test
    public void testFindDestinationsPredefinedPod() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(mockDestination.getPod()).thenReturn(mockPod);

        // Execute
        final List<DeployDestination> destinations = deployment.findDestinations();

        // Assert
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                mockDestination, destinations.get(0));
    }

    @Test
    public void testFindDestinations() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(mockDestination.getPod()).thenReturn(null);

        // Stub local method listByDataCenterIdVMTypeAndStates
        mockPods.add(mockHostPodVO1);
        mockPods.add(mockHostPodVO2);
        mockPods.add(mockHostPodVO3);
        final RouterDeploymentDefinition deployment = spy(this.deployment);
        doReturn(mockPods).when(deployment).listByDataCenterIdVMTypeAndStates(
                DATA_CENTER_ID, VirtualMachine.Type.User,
                VirtualMachine.State.Starting, VirtualMachine.State.Running);

        // Leave this one empty to force adding add destination for this pod
        final List<DomainRouterVO> virtualRouters1 = new ArrayList<>();
        when(mockRouterDao.listByPodIdAndStates(POD_ID1,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters1);

        // This list is not empty, so it will not add any for this pod, and continue with next pod
        final List<DomainRouterVO> virtualRouters2 = new ArrayList<>();
        final DomainRouterVO domainRouterVO1 = mock(DomainRouterVO.class);
        virtualRouters2.add(domainRouterVO1);
        when(mockRouterDao.listByPodIdAndStates(POD_ID2,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters2);

        // Leave this last one empty to check we finally added more than one afterwards
        final List<DomainRouterVO> virtualRouters3 = new ArrayList<>();
        when(mockRouterDao.listByPodIdAndStates(POD_ID3,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters3);

        // Execute
        final List<DeployDestination> destinations = deployment.findDestinations();

        // Assert that 2 were added (for the 1st and 3rd
        assertEquals("",
                2, destinations.size());
        assertEquals("",
                mockDataCenter, destinations.get(0).getDataCenter());
        assertEquals("",
                mockHostPodVO1, destinations.get(0).getPod());
        assertEquals("",
                mockDataCenter, destinations.get(1).getDataCenter());
        assertEquals("",
                mockHostPodVO3, destinations.get(1).getPod());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindDestinationsMoreThan1PodPerBasicZone() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(mockDestination.getPod()).thenReturn(null);

        // Stub local method listByDataCenterIdVMTypeAndStates
        mockPods.add(mockHostPodVO1);
        mockPods.add(mockHostPodVO2);
        // Deployment under test is a Mockito spy
        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doReturn(mockPods).when(deploymentUT).listByDataCenterIdVMTypeAndStates(
                DATA_CENTER_ID, VirtualMachine.Type.User,
                VirtualMachine.State.Starting, VirtualMachine.State.Running);

        // Leave this one empty to force adding add destination for this pod
        final List<DomainRouterVO> virtualRouters1 = new ArrayList<>();
        when(mockRouterDao.listByPodIdAndStates(POD_ID1,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters1);

        // This list is not empty, so it will not add any for this pod, and continue with next pod
        final List<DomainRouterVO> virtualRouters2 = new ArrayList<>();
        final DomainRouterVO domainRouterVO1 = mock(DomainRouterVO.class);
        final DomainRouterVO domainRouterVO2 = mock(DomainRouterVO.class);
        virtualRouters2.add(domainRouterVO1);
        virtualRouters2.add(domainRouterVO2);
        when(mockRouterDao.listByPodIdAndStates(POD_ID2,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters2);

        // Execute
        deploymentUT.findDestinations();

        // Assert by expected exception
    }

    @Test
    public void testPlanDeploymentRoutersBasic() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(mockDestination.getPod()).thenReturn(mockPod);

        // Execute
        deployment.planDeploymentRouters();

        // Assert
        verify(mockRouterDao, times(1)).listByNetworkAndPodAndRole(mockNw.getId(),
                POD_ID1, Role.VIRTUAL_ROUTER);
    }

    @Test
    public void testPlanDeploymentRoutersNonBasic() {
        // Prepare
        lenient().when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);
        lenient().when(mockDestination.getPod()).thenReturn(mockPod);

        // Execute
        deployment.planDeploymentRouters();

        // Assert
        verify(mockRouterDao, times(1)).listByNetworkAndRole(
                mockNw.getId(), Role.VIRTUAL_ROUTER);
    }

    @Test
    public void testListByDataCenterIdVMTypeAndStates() {
        // Prepare
        final VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        final SearchBuilder<VMInstanceVO> vmInstanceSearch = mock(SearchBuilder.class);
        when(mockVmDao.createSearchBuilder()).thenReturn(vmInstanceSearch);
        when(vmInstanceSearch.entity()).thenReturn(vmInstanceVO);
        when(vmInstanceVO.getType()).thenReturn(VirtualMachine.Type.Instance);
        when(vmInstanceVO.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vmInstanceVO.getPodIdToDeployIn()).thenReturn(POD_ID1);

        final SearchBuilder<HostPodVO> podIdSearch = mock(SearchBuilder.class);
        when(mockPodDao.createSearchBuilder()).thenReturn(podIdSearch);
        final SearchCriteria<HostPodVO> sc = mock(SearchCriteria.class);
        final HostPodVO hostPodVO = mock(HostPodVO.class);
        when(podIdSearch.entity()).thenReturn(hostPodVO);
        when(hostPodVO.getId()).thenReturn(POD_ID1);
        when(hostPodVO.getDataCenterId()).thenReturn(DATA_CENTER_ID);
        when(podIdSearch.create()).thenReturn(sc);

        final List<HostPodVO> expectedPods = mock(List.class);
        when(mockPodDao.search(sc, null)).thenReturn(expectedPods);

        // Execute
        final List<HostPodVO> pods = deployment.listByDataCenterIdVMTypeAndStates(DATA_CENTER_ID,
                VirtualMachine.Type.User,
                VirtualMachine.State.Starting,
                VirtualMachine.State.Running);

        // Assert
        assertNotNull(pods);
        assertEquals(expectedPods, pods);
        verify(sc, times(1)).setParameters("dc", DATA_CENTER_ID);
        verify(sc, times(1)).setJoinParameters("vmInstanceSearch", "type", VirtualMachine.Type.User);
        verify(sc, times(1)).setJoinParameters("vmInstanceSearch", "states",
                VirtualMachine.State.Starting, VirtualMachine.State.Running);
        verify(mockPodDao, times(1)).search(sc, null);
    }

    @Test
    public void testFindOrDeployVirtualRouter() throws ConcurrentOperationException,
    InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doNothing().when(deploymentUT).findOrDeployVirtualRouter();

        // Execute
        deploymentUT.deployVirtualRouter();

        // Assert
        verify(mockNetworkHelper, times(1)).startRouters(deploymentUT);
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testDeployVirtualRouter() throws ConcurrentOperationException,
    InsufficientCapacityException, ResourceUnavailableException {

        // Prepare
        final List<DeployDestination> mockDestinations = new ArrayList<>();
        mockDestinations.add(mock(DeployDestination.class));
        mockDestinations.add(mock(DeployDestination.class));

        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doNothing().when(deploymentUT).lock();
        doNothing().when(deploymentUT).checkPreconditions();
        doReturn(mockDestinations).when(deploymentUT).findDestinations();
        doNothing().when(deploymentUT).planDeploymentRouters();
        doNothing().when(deploymentUT).generateDeploymentPlan();
        // Let's test that if the last step fails in the last iteration it unlocks the table
        final ConcurrentOperationException exception =
                new ConcurrentOperationException(null);
        doNothing().doThrow(exception).when(deploymentUT).executeDeployment();
        doNothing().when(deploymentUT).unlock();

        // Execute
        try {
            deploymentUT.findOrDeployVirtualRouter();
        } finally {
            // Assert
            verify(deploymentUT, times(1)).lock();
            verify(deploymentUT, times(2)).checkPreconditions();
            verify(deploymentUT, times(2)).findDestinations();
            verify(deploymentUT, times(3)).generateDeploymentPlan();
            verify(deploymentUT, times(2)).executeDeployment();
            //verify(deploymentUT, times(2)).planDeploymentRouters();
            verify(deploymentUT, times(1)).unlock();
        }

        fail();
    }

    @Test
    public void testDeployVirtualRouterSkip() throws ConcurrentOperationException,
    InsufficientCapacityException, ResourceUnavailableException {

        // Prepare
        final List<DeployDestination> mockDestinations = new ArrayList<>();
        mockDestinations.add(mock(DeployDestination.class));
        mockDestinations.add(mock(DeployDestination.class));

        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doNothing().when(deploymentUT).checkPreconditions();
        doReturn(mockDestinations).when(deploymentUT).findDestinations();
        doNothing().when(deploymentUT).planDeploymentRouters();
        doNothing().when(deploymentUT).generateDeploymentPlan();
        doReturn(0).when(deploymentUT).getNumberOfRoutersToDeploy();

        // Execute
        try {
            deploymentUT.findOrDeployVirtualRouter();
        } finally {
            // Assert
            verify(deploymentUT, times(0)).lock(); // lock shouldn't be acquired when VR already present
            verify(deploymentUT, times(1)).checkPreconditions();
            verify(deploymentUT, times(1)).findDestinations();
            verify(deploymentUT, times(2)).generateDeploymentPlan();
            verify(deploymentUT, times(0)).executeDeployment(); // no need to deploy VR as already present
            verify(deploymentUT, times(0)).unlock(); // same as lock
        }
    }

    @Test
    public void testGetNumberOfRoutersToDeploy() {
        // Prepare
        deployment.routers = new ArrayList<>(); // Empty list

        // Execute and assert
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                1, deployment.getNumberOfRoutersToDeploy());

        // Execute and assert, just the same but for redundant deployment
        when(mockNw.isRedundant()).thenReturn(true);
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                2, deployment.getNumberOfRoutersToDeploy());

        // Just the same, instead of an empty list, a 1 items list
        deployment.routers.add(mock(DomainRouterVO.class));
        when(mockNw.isRedundant()).thenReturn(false);
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                0, deployment.getNumberOfRoutersToDeploy());

        when(mockNw.isRedundant()).thenReturn(true);
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                1, deployment.getNumberOfRoutersToDeploy());
    }

    @Test
    public void testFindVirtualProvider() {
        // Prepare
        when(mockNetworkModel.getPhysicalNetworkId(deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        final Type type = Type.VirtualRouter;
        final PhysicalNetworkServiceProviderVO physicalNwSrvProvider = mock(PhysicalNetworkServiceProviderVO.class);
        when(physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
        .thenReturn(physicalNwSrvProvider);
        when(physicalNwSrvProvider.getId()).thenReturn(PROVIDER_ID);

        final VirtualRouterProviderVO vrProvider = mock(VirtualRouterProviderVO.class);
        when(mockVrProviderDao.findByNspIdAndType(PROVIDER_ID, type))
        .thenReturn(vrProvider);

        // Execute
        deployment.findVirtualProvider();

        // Assert
        assertEquals("Didn't find and set the VirtualRouterProvider as expected",
                vrProvider, deployment.getVirtualProvider());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindVirtualProviderWithNullPhyNwSrvProvider() {
        // Prepare
        when(mockNetworkModel.getPhysicalNetworkId(deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        final Type type = Type.VirtualRouter;
        when(physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
        .thenReturn(null);

        // Execute
        deployment.findVirtualProvider();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindVirtualProviderWithNullVrProvider() {
        // Prepare
        when(mockNetworkModel.getPhysicalNetworkId(deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        final Type type = Type.VirtualRouter;
        final PhysicalNetworkServiceProviderVO physicalNwSrvProvider = mock(PhysicalNetworkServiceProviderVO.class);
        when(physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
        .thenReturn(physicalNwSrvProvider);
        when(physicalNwSrvProvider.getId()).thenReturn(PROVIDER_ID);

        when(mockVrProviderDao.findByNspIdAndType(PROVIDER_ID, type))
        .thenReturn(null);

        // Execute
        deployment.findVirtualProvider();
    }

    @Test
    public void testFindSourceNatIPPublicNw() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        final PublicIp sourceNatIp = mock(PublicIp.class);
        when(mockIpAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                mockOwner, mockNw)).thenReturn(sourceNatIp);
        deployment.isPublicNetwork = true;

        // It should be null until this method finds it
        assertNull(deployment.sourceNatIp);
        // Execute
        deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIP was not correctly found and set", sourceNatIp, deployment.sourceNatIp);
    }

    @Test
    public void testFindSourceNatIPNonPublicNw() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        final PublicIp sourceNatIp = mock(PublicIp.class);
        lenient().when(mockIpAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                mockOwner, mockNw)).thenReturn(sourceNatIp);
        deployment.isPublicNetwork = false;

        // It should be null until this method finds it
        assertNull(deployment.sourceNatIp);
        // Execute
        deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIP should remain null given a non public network",
                null, deployment.sourceNatIp);
    }

    @Test
    public void testFindOfferingIdFromNetwork() {
        // Prepare
        deployment.serviceOfferingId = 1L;
        when(mockNw.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(mockNetworkOfferingDao.findById(OFFERING_ID)).thenReturn(mockNwOfferingVO);
        when(mockNwOfferingVO.getServiceOfferingId()).thenReturn(OFFERING_ID);

        // Execute
        deployment.findServiceOfferingId();

        // Assert
        assertEquals("Service offering id not matching the one associated with network offering",
                OFFERING_ID, deployment.serviceOfferingId.longValue());
    }

    @Test
    public void testFindOfferingIdDefault() {
        // Prepare
        deployment.serviceOfferingId = 1L;
        when(mockNw.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(mockNetworkOfferingDao.findById(OFFERING_ID)).thenReturn(mockNwOfferingVO);
        when(mockNwOfferingVO.getServiceOfferingId()).thenReturn(null);
        when(mockServiceOfferingDao.findDefaultSystemOffering(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).thenReturn(mockSvcOfferingVO);
        when(mockSvcOfferingVO.getId()).thenReturn(DEFAULT_OFFERING_ID);

        // Execute
        deployment.findServiceOfferingId();

        // Assert
        assertEquals("Since there is no service offering associated with network offering, offering id should have matched default one",
                DEFAULT_OFFERING_ID, deployment.serviceOfferingId.longValue());
    }

    @Test
    public void testDeployAllVirtualRouters()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        // Prepare
        deployment.routers = new ArrayList<>();
        lenient().when(mockNw.isRedundant()).thenReturn(true);
        //this.deployment.routers.add(routerVO1);
        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doReturn(2).when(deploymentUT).getNumberOfRoutersToDeploy();

        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        when(mockNetworkHelper.deployRouter(deploymentUT, false))
        .thenReturn(routerVO1).thenReturn(routerVO2);
        lenient().when(networkDetailsDao.findById(anyLong())).thenReturn(null);
        // Execute
        deploymentUT.deployAllVirtualRouters();

        // Assert
        verify(mockRouterDao, times(1)).addRouterToGuestNetwork(routerVO1, mockNw);
        verify(mockRouterDao, times(1)).addRouterToGuestNetwork(routerVO2, mockNw);
        assertEquals("First router to deploy was not added to list of available routers",
                routerVO1, deployment.routers.get(0));
        assertEquals("Second router to deploy was not added to list of available routers",
                routerVO2, deployment.routers.get(1));
    }

    @Test
    public void testSetupAccountOwner() {
        // Prepare
        when(mockNetworkModel.isNetworkSystem(mockNw)).thenReturn(true);
        final Account newAccountOwner = mock(Account.class);
        when(mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(newAccountOwner);
        //Execute
        deployment.setupAccountOwner();
        // Assert
        assertEquals("New account owner not properly set", newAccountOwner, deployment.owner);
    }

    @Test
    public void testSetupAccountOwnerNotNetworkSystem() {
        // Prepare
        when(mockNetworkModel.isNetworkSystem(mockNw)).thenReturn(false);
        when(mockNw.getGuestType()).thenReturn(Network.GuestType.Shared);
        final Account newAccountOwner = mock(Account.class);
        when(mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(newAccountOwner);
        //Execute
        deployment.setupAccountOwner();
        // Assert
        assertEquals("New account owner not properly set", newAccountOwner, deployment.owner);
    }

    @Test
    public void testSetupAccountOwnerNotSharedNeitherNetworkSystem() {
        // Prepare
        when(mockNetworkModel.isNetworkSystem(mockNw)).thenReturn(false);
        when(mockNw.getGuestType()).thenReturn(Network.GuestType.Isolated);
        lenient().when(mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(null);
        //Execute
        deployment.setupAccountOwner();
        // Assert
        assertEquals("New account shouldn't have been updated", mockOwner, deployment.owner);
    }




    protected void driveTestPrepareDeployment(final boolean isRedundant, final boolean isPublicNw) {
        // Prepare
        when(mockNw.isRedundant()).thenReturn(isRedundant);
        when(mockNetworkModel.isAnyServiceSupportedInNetwork(
                NW_ID_1, Provider.VirtualRouter, Service.SourceNat, Service.Gateway)).thenReturn(isPublicNw);
        // Execute
        final boolean canProceedDeployment = deployment.prepareDeployment();
        // Assert
        boolean shouldProceedDeployment = true;
        if (isRedundant && !isPublicNw) {
            shouldProceedDeployment = false;
        }
        assertEquals(shouldProceedDeployment, canProceedDeployment);
        if (!shouldProceedDeployment) {
            assertEquals("Since deployment cannot proceed we should empty the list of routers",
                    0, deployment.routers.size());
        }
    }

    @Test
    public void testPrepareDeploymentPublicNw() {
        driveTestPrepareDeployment(true, true);
    }

    @Test
    public void testPrepareDeploymentNonRedundant() {
        driveTestPrepareDeployment(false, true);
    }

    @Test
    public void testPrepareDeploymentRedundantNonPublicNw() {
        driveTestPrepareDeployment(true, false);
    }

    protected void driveTestExecuteDeployment(final int noOfRoutersToDeploy, final boolean passPreparation)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        final RouterDeploymentDefinition deploymentUT = spy(deployment);
        doReturn(noOfRoutersToDeploy).when(deploymentUT).getNumberOfRoutersToDeploy();
        doReturn(passPreparation).when(deploymentUT).prepareDeployment();
        doNothing().when(deploymentUT).findVirtualProvider();
        doNothing().when(deploymentUT).findServiceOfferingId();
        doNothing().when(deploymentUT).findSourceNatIP();
        doNothing().when(deploymentUT).deployAllVirtualRouters();

        // Execute
        deploymentUT.executeDeployment();

        // Assert
        verify(deploymentUT, times(1)).getNumberOfRoutersToDeploy();
        int proceedToDeployment = 0;
        if (noOfRoutersToDeploy > 0) {
            verify(deploymentUT, times(1)).prepareDeployment();
            if (passPreparation) {
                proceedToDeployment = 1;
            }
        }
        verify(deploymentUT, times(proceedToDeployment)).findVirtualProvider();
        verify(deploymentUT, times(proceedToDeployment)).findServiceOfferingId();
        verify(deploymentUT, times(proceedToDeployment)).findSourceNatIP();
        verify(deploymentUT, times(proceedToDeployment)).deployAllVirtualRouters();
    }

    @Test
    public void testExecuteDeploymentNoRoutersToDeploy()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        driveTestExecuteDeployment(0, true);
    }

    @Test
    public void testExecuteDeploymentFailPreparation()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        driveTestExecuteDeployment(2, false);
    }

    @Test
    public void testExecuteDeployment()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        driveTestExecuteDeployment(2, true);
    }
}
