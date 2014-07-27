package org.cloud.network.router.deployment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.router.NetworkGeneralHelper;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class RouterDeploymentDefinitionTest {

    private static final long OFFERING_ID = 16L;
    private static final String NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED = "Number of routers to deploy is not the expected";
    private static final String ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED = "Only the provided as default destination was expected";
    protected static final Long DATA_CENTER_ID = 100l;
    protected static final Long NW_ID = 102l;
    protected static final Long POD_ID1 = 111l;
    protected static final Long POD_ID2 = 112l;
    protected static final Long POD_ID3 = 113l;
    protected static final Long ROUTER1_ID = 121l;
    protected static final Long ROUTER2_ID = 122l;
    private static final long PROVIDER_ID = 131L;
    private static final long PHYSICAL_NW_ID = 141L;

    // General delegates (Daos, Mgrs...)
    @Mock
    protected NetworkDao mockNwDao;

    // Instance specific parameters to use during build
    @Mock
    protected DeployDestination mockDestination;
    @Mock
    protected DataCenter mockDataCenter;
    @Mock
    protected Pod mockPod;
    @Mock
    protected HostPodVO mockHostPodVO1;
    @Mock
    protected HostPodVO mockHostPodVO2;
    @Mock
    protected HostPodVO mockHostPodVO3;
    @Mock
    protected NetworkVO mockNw;
    @Mock
    NetworkOfferingVO mockNwOfferingVO;
    @Mock
    protected Account mockOwner;
    @Mock
    protected DomainRouterDao mockRouterDao;
    @Mock
    protected NetworkGeneralHelper mockNetworkGeneralHelper;
    @Mock
    protected VMInstanceDao mockVmDao;
    @Mock
    protected HostPodDao mockPodDao;
    @Mock
    protected VirtualRouterProviderDao mockVrProviderDao;
    @Mock
    protected PhysicalNetworkServiceProviderDao physicalProviderDao;
    @Mock
    protected NetworkModel mockNetworkModel;
    @Mock
    protected IpAddressManager mockIpAddrMgr;
    @Mock
    protected NetworkOfferingDao mockNetworkOfferingDao;
    @Mock
    protected AccountManager mockAccountMgr;


    protected List<HostPodVO> mockPods = new ArrayList<>();
    protected Map<Param, Object> params = new HashMap<>();

    @InjectMocks
    protected RouterDeploymentDefinitionBuilder builder = new RouterDeploymentDefinitionBuilder();

    protected RouterDeploymentDefinition deployment;


    @Before
    public void initTest() {
        when(this.mockDestination.getDataCenter()).thenReturn(this.mockDataCenter);
        when(this.mockDataCenter.getId()).thenReturn(DATA_CENTER_ID);
        when(this.mockPod.getId()).thenReturn(POD_ID1);
        when(this.mockHostPodVO1.getId()).thenReturn(POD_ID1);
        when(this.mockHostPodVO2.getId()).thenReturn(POD_ID2);
        when(this.mockHostPodVO3.getId()).thenReturn(POD_ID3);
        when(this.mockNw.getId()).thenReturn(NW_ID);

        this.deployment = this.builder.create()
                .setGuestNetwork(this.mockNw)
                .setDeployDestination(this.mockDestination)
                .setAccountOwner(this.mockOwner)
                .build();
    }

    @Test
    public void testRedundancyProperty() {
        // Set and confirm is redundant
        RouterDeploymentDefinition deployment1 = this.builder.create()
                .setGuestNetwork(this.mockNw)
                .setDeployDestination(this.mockDestination)
                .makeRedundant()
                .build();
        assertTrue("The builder ignored \".makeRedundant()\"", deployment1.isRedundant());
        RouterDeploymentDefinition deployment2 = this.builder.create()
                .setGuestNetwork(this.mockNw)
                .setDeployDestination(this.mockDestination)
                .setRedundant(true)
                .build();
        assertTrue("The builder ignored \".setRedundant(true)\"", deployment2.isRedundant());
    }

    @Test
    public void testConstructionFieldsAndFlags() {
        // Vpc type
        assertFalse(this.deployment.isVpcRouter());
        // Offering null
        this.deployment.offeringId = null;
        assertNull(this.deployment.getOfferingId());
        // Offering null
        ServiceOfferingVO offeringVO = mock(ServiceOfferingVO.class);
        this.deployment.offeringId = OFFERING_ID;
        assertEquals(OFFERING_ID, this.deployment.getOfferingId().longValue());
        // Routers
        assertNotNull(this.deployment.getRouters());
        // Guest network
        assertNotNull(this.deployment.getGuestNetwork());
        // Deploy Destination
        assertNotNull(this.deployment.getDest());
        // Account owner
        assertNotNull(this.deployment.getOwner());
        // Deployment plan
        this.deployment.plan = mock(DeploymentPlan.class);
        assertNotNull(this.deployment.getPlan());
        // Redundant : by default is not
        assertFalse(this.deployment.isRedundant());
        this.deployment.isRedundant = true;
        assertTrue(this.deployment.isRedundant());
    }

    @Test
    public void testLock() {
        // Prepare
        when(this.mockNwDao.acquireInLockTable(NW_ID, NetworkOrchestrationService.NetworkLockTimeout.value()))
        .thenReturn(mockNw);

        // Execute
        this.deployment.lock();

        // Assert
        verify(this.mockNwDao, times(1)).acquireInLockTable(NW_ID, 600);
        assertNotNull(this.deployment.tableLockId);
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testLockFails() {
        // Prepare
        when(this.mockNwDao.acquireInLockTable(NW_ID, NetworkOrchestrationService.NetworkLockTimeout.value()))
        .thenReturn(null);

        // Execute
        this.deployment.lock();

        // Assert
        verify(this.mockNwDao, times(1)).acquireInLockTable(NW_ID, 600);
        assertNotNull(this.deployment.tableLockId);
    }

    @Test
    public void testUnlock() {
        // Prepare
        this.deployment.tableLockId = NW_ID;

        // Execute
        this.deployment.unlock();

        // Assert
        verify(this.mockNwDao, times(1)).releaseFromLockTable(NW_ID);
    }

    @Test
    public void testUnlockWithoutLock() {
        // Prepare
        this.deployment.tableLockId = null;

        // Execute
        this.deployment.unlock();

        // Assert
        verify(this.mockNwDao, times(0)).releaseFromLockTable(anyLong());
    }

    /**
     * If it's not a basic network, pod is not needed in the generated DataCenterDeployment
     */
    @Test
    public void testGenerateDeploymentPlanNoPodNeeded() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);

        // Execute
        this.deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) this.deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, this.deployment.dest);
        assertEquals("", null, this.deployment.getPod());
        assertEquals("", null, this.deployment.getPodId());
    }

    /**
     * If it's Basic, it should have pod
     */
    @Test
    public void testGenerateDeploymentPlanBasic() {
        // Prepare
        when(this.mockDestination.getPod()).thenReturn(this.mockPod);
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);

        // Execute
        this.deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) this.deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, this.deployment.dest);
        assertEquals("", mockPod, this.deployment.getPod());
        assertEquals("", POD_ID1, this.deployment.getPodId());
    }

    /**
     * If it's Basic, it should have pod, otherwise fail with
     * {@link CloudRuntimeException}
     */
    @Test(expected = CloudRuntimeException.class)
    public void testGenerateDeploymentPlanBasicFailNoPod() {
        // Prepare
        when(this.mockDestination.getPod()).thenReturn(null);
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);

        // Execute
        this.deployment.generateDeploymentPlan();

        // Assert
        assertEquals("", DATA_CENTER_ID, (Long) this.deployment.plan.getDataCenterId());
        assertEquals("", mockDestination, this.deployment.dest);
    }

    @Test
    public void testCheckPreconditions() throws ResourceUnavailableException {
        // Prepare
        Network.State states[] = {
                Network.State.Implemented,
                Network.State.Setup,
                Network.State.Implementing
        };
        when(this.deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Guest);

        // Drive specific tests
        for (Network.State state : states) {
            this.driveTestCheckPreconditionsCorrectNwState(state);
        }
    }

    public void driveTestCheckPreconditionsCorrectNwState(Network.State state) throws ResourceUnavailableException {
        // Prepare
        when(this.deployment.guestNetwork.getState()).thenReturn(state);

        // Execute
        this.deployment.checkPreconditions();

        // Assert : It just should raise no exceptions
    }

    @Test(expected = ResourceUnavailableException.class)
    public void testCheckPreconditionsWrongTrafficType() throws ResourceUnavailableException {
        // Prepare wrong traffic type to trigger error
        when(this.deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Public);

        // Execute
        this.driveTestCheckPreconditionsCorrectNwState(Network.State.Implemented);
    }

    @Test(expected = ResourceUnavailableException.class)
    public void testCheckPreconditionsWrongState() throws ResourceUnavailableException {
        // Prepare wrong traffic type to trigger error
        when(this.deployment.guestNetwork.getTrafficType()).thenReturn(TrafficType.Guest);

        // Execute
        this.driveTestCheckPreconditionsCorrectNwState(Network.State.Shutdown);
    }

    @Test
    public void testFindDestinationsNonBasicZone() {
        // Prepare
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);

        // Execute
        List<DeployDestination> destinations = this.deployment.findDestinations();

        // Assert
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                this.mockDestination, destinations.get(0));
    }

    @Test
    public void testFindDestinationsPredefinedPod() {
        // Prepare
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(this.mockDestination.getPod()).thenReturn(this.mockPod);

        // Execute
        List<DeployDestination> destinations = this.deployment.findDestinations();

        // Assert
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                this.mockDestination, destinations.get(0));
    }

    @Test
    public void testFindDestinations() {
        // Prepare
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(this.mockDestination.getPod()).thenReturn(null);

        // Stub local method listByDataCenterIdVMTypeAndStates
        this.mockPods.add(this.mockHostPodVO1);
        this.mockPods.add(this.mockHostPodVO2);
        this.mockPods.add(this.mockHostPodVO3);
        RouterDeploymentDefinition deployment = spy(this.deployment);
        doReturn(mockPods).when(deployment).listByDataCenterIdVMTypeAndStates(
                DATA_CENTER_ID, VirtualMachine.Type.User,
                VirtualMachine.State.Starting, VirtualMachine.State.Running);

        // Leave this one empty to force adding add destination for this pod
        List<DomainRouterVO> virtualRouters1 = new ArrayList<>();
        when(this.mockRouterDao.listByPodIdAndStates(POD_ID1,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters1);

        // This list is not empty, so it will not add any for this pod, and continue with next pod
        List<DomainRouterVO> virtualRouters2 = new ArrayList<>();
        DomainRouterVO domainRouterVO1 = mock(DomainRouterVO.class);
        virtualRouters2.add(domainRouterVO1);
        when(this.mockRouterDao.listByPodIdAndStates(POD_ID2,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters2);

        // Leave this last one empty to check we finally added more than one afterwards
        List<DomainRouterVO> virtualRouters3 = new ArrayList<>();
        when(this.mockRouterDao.listByPodIdAndStates(POD_ID3,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters3);

        // Execute
        List<DeployDestination> destinations = deployment.findDestinations();

        // Assert that 2 were added (for the 1st and 3rd
        assertEquals("",
                2, destinations.size());
        assertEquals("",
                this.mockDataCenter, destinations.get(0).getDataCenter());
        assertEquals("",
                this.mockHostPodVO1, destinations.get(0).getPod());
        assertEquals("",
                this.mockDataCenter, destinations.get(1).getDataCenter());
        assertEquals("",
                this.mockHostPodVO3, destinations.get(1).getPod());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindDestinationsMoreThan1PodPerBasicZone() {
        // Prepare
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(this.mockDestination.getPod()).thenReturn(null);

        // Stub local method listByDataCenterIdVMTypeAndStates
        this.mockPods.add(this.mockHostPodVO1);
        this.mockPods.add(this.mockHostPodVO2);
        // Deployment under test is a Mockito spy
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doReturn(mockPods).when(deploymentUT).listByDataCenterIdVMTypeAndStates(
                DATA_CENTER_ID, VirtualMachine.Type.User,
                VirtualMachine.State.Starting, VirtualMachine.State.Running);

        // Leave this one empty to force adding add destination for this pod
        List<DomainRouterVO> virtualRouters1 = new ArrayList<>();
        when(this.mockRouterDao.listByPodIdAndStates(POD_ID1,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters1);

        // This list is not empty, so it will not add any for this pod, and continue with next pod
        List<DomainRouterVO> virtualRouters2 = new ArrayList<>();
        DomainRouterVO domainRouterVO1 = mock(DomainRouterVO.class);
        DomainRouterVO domainRouterVO2 = mock(DomainRouterVO.class);
        virtualRouters2.add(domainRouterVO1);
        virtualRouters2.add(domainRouterVO2);
        when(this.mockRouterDao.listByPodIdAndStates(POD_ID2,
                VirtualMachine.State.Starting, VirtualMachine.State.Running)).thenReturn(virtualRouters2);

        // Execute
        deploymentUT.findDestinations();

        // Assert by expected exception
    }

    @Test
    public void testPlanDeploymentRoutersBasic() {
        // Prepare
        when(this.mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);
        when(this.mockDestination.getPod()).thenReturn(this.mockPod);

        // Execute
        this.deployment.planDeploymentRouters();

        // Assert
        verify(this.mockRouterDao, times(1)).listByNetworkAndPodAndRole(this.mockNw.getId(),
                POD_ID1, Role.VIRTUAL_ROUTER);
    }

    @Test
    public void testPlanDeploymentRoutersNonBasic() {
        // Prepare
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(this.mockDestination.getPod()).thenReturn(this.mockPod);

        // Execute
        this.deployment.planDeploymentRouters();

        // Assert
        verify(this.mockRouterDao, times(1)).listByNetworkAndRole(
                this.mockNw.getId(), Role.VIRTUAL_ROUTER);
    }

    @Test
    public void testListByDataCenterIdVMTypeAndStates() {
        // Prepare
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        final SearchBuilder<VMInstanceVO> vmInstanceSearch = mock(SearchBuilder.class);
        when(this.mockVmDao.createSearchBuilder()).thenReturn(vmInstanceSearch);
        when(vmInstanceSearch.entity()).thenReturn(vmInstanceVO);
        when(vmInstanceVO.getType()).thenReturn(VirtualMachine.Type.Instance);
        when(vmInstanceVO.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vmInstanceVO.getPodIdToDeployIn()).thenReturn(POD_ID1);

        final SearchBuilder<HostPodVO> podIdSearch = mock(SearchBuilder.class);
        when(this.mockPodDao.createSearchBuilder()).thenReturn(podIdSearch);
        final SearchCriteria<HostPodVO> sc = mock(SearchCriteria.class);
        HostPodVO hostPodVO = mock(HostPodVO.class);
        when(podIdSearch.entity()).thenReturn(hostPodVO);
        when(hostPodVO.getId()).thenReturn(POD_ID1);
        when(hostPodVO.getDataCenterId()).thenReturn(DATA_CENTER_ID);
        when(podIdSearch.create()).thenReturn(sc);

        final List<HostPodVO> expectedPods = mock(List.class);
        when(this.mockPodDao.search(sc, null)).thenReturn(expectedPods);

        // Execute
        final List<HostPodVO> pods = this.deployment.listByDataCenterIdVMTypeAndStates(DATA_CENTER_ID,
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
        verify(this.mockPodDao, times(1)).search(sc, null);
    }

    @Test
    public void testFindOrDeployVirtualRouter() throws ConcurrentOperationException,
    InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        RouterDeploymentDefinition deploymentUT = spy(this.deployment);
        doNothing().when(deploymentUT).findOrDeployVirtualRouter();

        // Execute
        deploymentUT.deployVirtualRouter();

        // Assert
        verify(this.mockNetworkGeneralHelper, times(1)).startRouters(deploymentUT);
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testDeployVirtualRouter() throws ConcurrentOperationException,
            InsufficientCapacityException, ResourceUnavailableException {

        // Prepare
        List<DeployDestination> mockDestinations = new ArrayList<>();
        mockDestinations.add(mock(DeployDestination.class));
        mockDestinations.add(mock(DeployDestination.class));

        RouterDeploymentDefinition deploymentUT = spy(this.deployment);
        doNothing().when(deploymentUT).lock();
        doNothing().when(deploymentUT).checkPreconditions();
        doReturn(mockDestinations).when(deploymentUT).findDestinations();
        doNothing().when(deploymentUT).planDeploymentRouters();
        doNothing().when(deploymentUT).generateDeploymentPlan();
        // Let's test that if the last step fails in the last iteration it unlocks the table
        ConcurrentOperationException exception =
                new ConcurrentOperationException(null);
        doNothing().doThrow(exception).when(deploymentUT).executeDeployment();
        doNothing().when(deploymentUT).unlock();

        // Execute
        try {
            deploymentUT.findOrDeployVirtualRouter();
        } catch (ConcurrentOperationException e) {
            throw e;
        } finally {
            // Assert
            verify(deploymentUT, times(1)).lock();
            verify(deploymentUT, times(1)).checkPreconditions();
            verify(deploymentUT, times(1)).findDestinations();
            verify(deploymentUT, times(2)).planDeploymentRouters();
            verify(deploymentUT, times(2)).generateDeploymentPlan();
            verify(deploymentUT, times(2)).executeDeployment();
            verify(deploymentUT, times(1)).unlock();
        }

        fail();
    }

    /**
     * If any router is NOT redundant, then it shouldn't update routers
     */
    @Test
    public void testSetupPriorityOfRedundantRouterWithNonRedundantRouters() {
        // Prepare
        this.deployment.routers = new ArrayList<>();
        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO1);
        when(routerVO1.getIsRedundantRouter()).thenReturn(true);
        when(routerVO1.getState()).thenReturn(VirtualMachine.State.Stopped);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO2);
        when(routerVO2.getIsRedundantRouter()).thenReturn(false);
        when(routerVO2.getState()).thenReturn(VirtualMachine.State.Stopped);
        // If this deployment is not redundant nothing will be executed
        this.deployment.isRedundant = true;

        // Execute
        this.deployment.setupPriorityOfRedundantRouter();

        // Assert
        verify(routerVO1, times(0)).setPriority(anyInt());
        verify(routerVO1, times(0)).setIsPriorityBumpUp(anyBoolean());
        verify(this.mockRouterDao, times(0)).update(anyLong(), (DomainRouterVO) anyObject());
    }

    /**
     * If any router is NOT Stopped, then it shouldn't update routers
     */
    @Test
    public void testSetupPriorityOfRedundantRouterWithRunningRouters() {
        // Prepare
        this.deployment.routers = new ArrayList<>();
        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO1);
        when(routerVO1.getIsRedundantRouter()).thenReturn(true);
        when(routerVO1.getState()).thenReturn(VirtualMachine.State.Stopped);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO2);
        when(routerVO2.getIsRedundantRouter()).thenReturn(true);
        when(routerVO2.getState()).thenReturn(VirtualMachine.State.Running);
        // If this deployment is not redundant nothing will be executed
        this.deployment.isRedundant = true;

        // Execute
        this.deployment.setupPriorityOfRedundantRouter();

        // Assert
        verify(routerVO1, times(0)).setPriority(anyInt());
        verify(routerVO1, times(0)).setIsPriorityBumpUp(anyBoolean());
        verify(this.mockRouterDao, times(0)).update(anyLong(), (DomainRouterVO) anyObject());
    }

    /**
     * Given all routers are redundant and Stopped, then it should update ALL routers
     */
    @Test
    public void testSetupPriorityOfRedundantRouter() {
        // Prepare
        this.deployment.routers = new ArrayList<>();
        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO1);
        when(routerVO1.getId()).thenReturn(ROUTER1_ID);
        when(routerVO1.getIsRedundantRouter()).thenReturn(true);
        when(routerVO1.getState()).thenReturn(VirtualMachine.State.Stopped);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO2);
        when(routerVO2.getId()).thenReturn(ROUTER2_ID);
        when(routerVO2.getIsRedundantRouter()).thenReturn(true);
        when(routerVO2.getState()).thenReturn(VirtualMachine.State.Stopped);
        // If this deployment is not redundant nothing will be executed
        this.deployment.isRedundant = true;

        // Execute
        this.deployment.setupPriorityOfRedundantRouter();

        // Assert
        verify(routerVO1, times(1)).setPriority(0);
        verify(routerVO1, times(1)).setIsPriorityBumpUp(false);
        verify(this.mockRouterDao, times(1)).update(ROUTER1_ID, routerVO1);
        verify(routerVO2, times(1)).setPriority(0);
        verify(routerVO2, times(1)).setIsPriorityBumpUp(false);
        verify(this.mockRouterDao, times(1)).update(ROUTER2_ID, routerVO2);
    }

    /**
     * If this is not a redundant deployment, then we shouldn't reset priorities
     */
    @Test
    public void testSetupPriorityOfRedundantRouterWithNonRedundantDeployment() {
        // Prepare
        this.deployment.routers = new ArrayList<>();
        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO1);
        when(routerVO1.getIsRedundantRouter()).thenReturn(true);
        when(routerVO1.getState()).thenReturn(VirtualMachine.State.Stopped);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        this.deployment.routers.add(routerVO2);
        when(routerVO2.getIsRedundantRouter()).thenReturn(true);
        when(routerVO2.getState()).thenReturn(VirtualMachine.State.Stopped);

        // Execute
        this.deployment.setupPriorityOfRedundantRouter();

        // Assert
        verify(routerVO1, times(0)).setPriority(anyInt());
        verify(routerVO1, times(0)).setIsPriorityBumpUp(anyBoolean());
        verify(this.mockRouterDao, times(0)).update(anyLong(), (DomainRouterVO) anyObject());
    }

    @Test
    public void testGetNumberOfRoutersToDeploy() {
        // Prepare
        this.deployment.routers = new ArrayList<>(); // Empty list

        // Execute and assert
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                1, this.deployment.getNumberOfRoutersToDeploy());

        // Execute and assert, just the same but for redundant deployment
        this.deployment.isRedundant = true;
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                2, this.deployment.getNumberOfRoutersToDeploy());

        // Just the same, instead of an empty list, a 1 items list
        this.deployment.routers.add(mock(DomainRouterVO.class));
        this.deployment.isRedundant = false;
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                0, this.deployment.getNumberOfRoutersToDeploy());

        this.deployment.isRedundant = true;
        assertEquals(NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED,
                1, this.deployment.getNumberOfRoutersToDeploy());
    }

    @Test
    public void testFindVirtualProvider() {
        // Prepare
        when(this.mockNetworkModel.getPhysicalNetworkId(this.deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        Type type = Type.VirtualRouter;
        PhysicalNetworkServiceProviderVO physicalNwSrvProvider = mock(PhysicalNetworkServiceProviderVO.class);
        when(this.physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
            .thenReturn(physicalNwSrvProvider);
        when(physicalNwSrvProvider.getId()).thenReturn(PROVIDER_ID);

        VirtualRouterProviderVO vrProvider = mock(VirtualRouterProviderVO.class);
        when(this.mockVrProviderDao.findByNspIdAndType(PROVIDER_ID, type))
            .thenReturn(vrProvider);

        // Execute
        this.deployment.findVirtualProvider();

        // Assert
        assertEquals("Didn't find and set the VirtualRouterProvider as expected",
                vrProvider, this.deployment.getVirtualProvider());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindVirtualProviderWithNullPhyNwSrvProvider() {
        // Prepare
        when(this.mockNetworkModel.getPhysicalNetworkId(this.deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        Type type = Type.VirtualRouter;
        when(this.physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
            .thenReturn(null);

        // Execute
        this.deployment.findVirtualProvider();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFindVirtualProviderWithNullVrProvider() {
        // Prepare
        when(this.mockNetworkModel.getPhysicalNetworkId(this.deployment.guestNetwork)).thenReturn(PHYSICAL_NW_ID);
        Type type = Type.VirtualRouter;
        PhysicalNetworkServiceProviderVO physicalNwSrvProvider = mock(PhysicalNetworkServiceProviderVO.class);
        when(this.physicalProviderDao.findByServiceProvider(PHYSICAL_NW_ID, type.toString()))
            .thenReturn(physicalNwSrvProvider);
        when(physicalNwSrvProvider.getId()).thenReturn(PROVIDER_ID);

        when(this.mockVrProviderDao.findByNspIdAndType(PROVIDER_ID, type))
            .thenReturn(null);

        // Execute
        this.deployment.findVirtualProvider();
    }

    @Test
    public void testFindSourceNatIPPublicNw() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        PublicIp sourceNatIp = mock(PublicIp.class);
        when(this.mockIpAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                this.mockOwner, this.mockNw)).thenReturn(sourceNatIp);
        this.deployment.publicNetwork = true;

        // It should be null until this method finds it
        assertNull(this.deployment.sourceNatIp);
        // Execute
        this.deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIP was not correctly found and set", sourceNatIp, this.deployment.sourceNatIp);
    }

    @Test
    public void testFindSourceNatIPNonPublicNw() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        PublicIp sourceNatIp = mock(PublicIp.class);
        when(this.mockIpAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                this.mockOwner, this.mockNw)).thenReturn(sourceNatIp);
        this.deployment.publicNetwork = false;

        // It should be null until this method finds it
        assertNull(this.deployment.sourceNatIp);
        // Execute
        this.deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIP should remain null given a non public network",
                null, this.deployment.sourceNatIp);
    }

    @Test
    public void testFindOfferingIdReceivingNewOne() {
        // Prepare
        this.deployment.offeringId = 1L;
        when(this.mockNw.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(this.mockNetworkOfferingDao.findById(OFFERING_ID)).thenReturn(this.mockNwOfferingVO);
        when(this.mockNwOfferingVO.getServiceOfferingId()).thenReturn(OFFERING_ID);

        // Execute
        this.deployment.findOfferingId();

        // Assert
        assertEquals("Given that no Offering was found, the previous Offering Id should be kept",
                OFFERING_ID, this.deployment.offeringId.longValue());
    }

    @Test
    public void testFindOfferingIdReceivingKeepingPrevious() {
        // Prepare
        this.deployment.offeringId = 1L;
        when(this.mockNw.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(this.mockNetworkOfferingDao.findById(OFFERING_ID)).thenReturn(this.mockNwOfferingVO);
        when(this.mockNwOfferingVO.getServiceOfferingId()).thenReturn(null);

        // Execute
        this.deployment.findOfferingId();

        // Assert
        assertEquals("Found Offering Id didn't replace previous one",
                1L, this.deployment.offeringId.longValue());
    }

    @Test
    public void testDeployAllVirtualRouters()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        // Prepare
        this.deployment.routers = new ArrayList<>();
        this.deployment.isRedundant = true;
        //this.deployment.routers.add(routerVO1);
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doReturn(2).when(deploymentUT).getNumberOfRoutersToDeploy();
        doReturn(null).when(deploymentUT).createRouterNetworks();

        final DomainRouterVO routerVO1 = mock(DomainRouterVO.class);
        final DomainRouterVO routerVO2 = mock(DomainRouterVO.class);
        when(this.mockNetworkGeneralHelper.deployRouter(deploymentUT, null, false, null))
            .thenReturn(routerVO1).thenReturn(routerVO2);

        // Execute
        deploymentUT.deployAllVirtualRouters();

        // Assert
        verify(this.mockRouterDao, times(1)).addRouterToGuestNetwork(routerVO1, this.mockNw);
        verify(this.mockRouterDao, times(1)).addRouterToGuestNetwork(routerVO2, this.mockNw);
        assertEquals("First router to deploy was not added to list of available routers",
                routerVO1, this.deployment.routers.get(0));
        assertEquals("Second router to deploy was not added to list of available routers",
                routerVO2, this.deployment.routers.get(1));
    }

    @Test
    public void testExecuteDeploymentPublicNw()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        this.deployment.isRedundant = true;
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doNothing().when(deploymentUT).setupPriorityOfRedundantRouter();
        doReturn(2).when(deploymentUT).getNumberOfRoutersToDeploy();
        doNothing().when(deploymentUT).findVirtualProvider();
        doNothing().when(deploymentUT).findOfferingId();
        doNothing().when(deploymentUT).findSourceNatIP();
        doNothing().when(deploymentUT).deployAllVirtualRouters();

        when(this.mockNetworkModel.isNetworkSystem(this.mockNw)).thenReturn(true);
        Account newAccountOwner = mock(Account.class);
        when(this.mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(newAccountOwner);

        when(this.mockNetworkModel.isProviderSupportServiceInNetwork(
                            NW_ID, Service.SourceNat, Provider.VirtualRouter)).thenReturn(true);

        // Execute
        deploymentUT.executeDeployment();

        // Assert
        assertEquals("New account owner not properly set", newAccountOwner, deploymentUT.owner);
        verify(deploymentUT, times(1)).findVirtualProvider();
        verify(deploymentUT, times(1)).findOfferingId();
        verify(deploymentUT, times(1)).findSourceNatIP();
        verify(deploymentUT, times(1)).deployAllVirtualRouters();
    }

    @Test
    public void testExecuteDeploymentNonRedundant()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        this.deployment.isRedundant = false;
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doNothing().when(deploymentUT).setupPriorityOfRedundantRouter();
        doReturn(2).when(deploymentUT).getNumberOfRoutersToDeploy();
        doNothing().when(deploymentUT).findVirtualProvider();
        doNothing().when(deploymentUT).findOfferingId();
        doNothing().when(deploymentUT).findSourceNatIP();
        doNothing().when(deploymentUT).deployAllVirtualRouters();

        when(this.mockNetworkModel.isNetworkSystem(this.mockNw)).thenReturn(true);
        Account newAccountOwner = mock(Account.class);
        when(this.mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(newAccountOwner);

        when(this.mockNetworkModel.isProviderSupportServiceInNetwork(
                            NW_ID, Service.SourceNat, Provider.VirtualRouter)).thenReturn(true);

        // Execute
        deploymentUT.executeDeployment();

        // Assert
        assertEquals("New account owner not properly set", newAccountOwner, deploymentUT.owner);
        verify(deploymentUT, times(1)).findVirtualProvider();
        verify(deploymentUT, times(1)).findOfferingId();
        verify(deploymentUT, times(1)).findSourceNatIP();
        verify(deploymentUT, times(1)).deployAllVirtualRouters();
    }

    @Test
    public void testExecuteDeploymentRedundantNonPublicNw()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        this.deployment.isRedundant = true;
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doNothing().when(deploymentUT).setupPriorityOfRedundantRouter();
        doReturn(2).when(deploymentUT).getNumberOfRoutersToDeploy();
        doNothing().when(deploymentUT).findVirtualProvider();
        doNothing().when(deploymentUT).findOfferingId();
        doNothing().when(deploymentUT).findSourceNatIP();
        doNothing().when(deploymentUT).deployAllVirtualRouters();

        when(this.mockNetworkModel.isNetworkSystem(this.mockNw)).thenReturn(true);
        Account newAccountOwner = mock(Account.class);
        when(this.mockAccountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM)).thenReturn(newAccountOwner);

        when(this.mockNetworkModel.isProviderSupportServiceInNetwork(
                            NW_ID, Service.SourceNat, Provider.VirtualRouter)).thenReturn(false);

        // Execute
        deploymentUT.executeDeployment();

        // Assert
        assertEquals("New account owner not properly set", newAccountOwner, deploymentUT.owner);
        assertEquals("Since is redundant deployment in non public nw there should be 0 routers to start",
                0, this.deployment.routers.size());
        verify(this.mockNetworkModel, times(1)).isNetworkSystem(this.mockNw);
        verify(deploymentUT, times(0)).findVirtualProvider();
        verify(deploymentUT, times(0)).findOfferingId();
        verify(deploymentUT, times(0)).findSourceNatIP();
        verify(deploymentUT, times(0)).deployAllVirtualRouters();
    }

    @Test
    public void testExecuteDeploymentNoRoutersToDeploy()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        this.deployment.isRedundant = true;
        this.deployment.publicNetwork = false;
        RouterDeploymentDefinition deploymentUT = Mockito.spy(this.deployment);
        doNothing().when(deploymentUT).setupPriorityOfRedundantRouter();
        doReturn(0).when(deploymentUT).getNumberOfRoutersToDeploy();
        doNothing().when(deploymentUT).findVirtualProvider();
        doNothing().when(deploymentUT).findOfferingId();
        doNothing().when(deploymentUT).findSourceNatIP();
        doNothing().when(deploymentUT).deployAllVirtualRouters();

        // Execute
        deploymentUT.executeDeployment();

        // Assert
        assertEquals("New account owner not properly set", this.mockOwner, deploymentUT.owner);
        verify(this.mockNetworkModel, times(0)).isNetworkSystem((Network)anyObject());
        verify(deploymentUT, times(0)).findVirtualProvider();
        verify(deploymentUT, times(0)).findOfferingId();
        verify(deploymentUT, times(0)).findSourceNatIP();
        verify(deploymentUT, times(0)).deployAllVirtualRouters();
    }

    @Test
    public void testCreateRouterNetworks() {
        // TODO Implement this test
    }

}
