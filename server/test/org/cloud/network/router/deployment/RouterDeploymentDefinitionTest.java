package org.cloud.network.router.deployment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Assert;
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
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;

@RunWith(MockitoJUnitRunner.class)
public class RouterDeploymentDefinitionTest {

    private static final String ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED = "Only the provided as default destination was expected";
    protected static final Long DATA_CENTER_ID = 100l;
    protected static final Long POD_ID1 = 111l;
    protected static final Long POD_ID2 = 112l;
    protected static final Long POD_ID3 = 113l;
    protected static final Long NW_ID = 102l;

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
    protected NetworkVO mockNwLock;
    @Mock
    protected Account mockOwner;
    @Mock
    protected DomainRouterDao mockRouterDao;

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
        when(this.mockNwLock.getId()).thenReturn(NW_ID);

        this.deployment = this.builder.create()
                .setGuestNetwork(this.mockNwLock)
                .setDeployDestination(this.mockDestination)
                .build();
    }

    @Test
    public void testLock() {
        // Prepare
        when(this.mockNwDao.acquireInLockTable(NW_ID, NetworkOrchestrationService.NetworkLockTimeout.value()))
        .thenReturn(mockNwLock);

        // Execute
        this.deployment.lock();

        // Assert
        verify(this.mockNwDao, times(1)).acquireInLockTable(NW_ID, 600);
        Assert.assertNotNull(this.deployment.tableLockId);
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
        Assert.assertNotNull(this.deployment.tableLockId);
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
        when(this.mockDestination.getPod()).thenReturn(mockPod);
        when(mockDataCenter.getNetworkType()).thenReturn(NetworkType.Basic);

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
        Assert.assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        Assert.assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
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
        Assert.assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
                1, destinations.size());
        Assert.assertEquals(ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED,
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
        RouterDeploymentDefinition deployment = Mockito.spy(this.deployment);
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
        Assert.assertEquals("",
                2, destinations.size());
        Assert.assertEquals("",
                this.mockDataCenter, destinations.get(0).getDataCenter());
        Assert.assertEquals("",
                this.mockHostPodVO1, destinations.get(0).getPod());
        Assert.assertEquals("",
                this.mockDataCenter, destinations.get(1).getDataCenter());
        Assert.assertEquals("",
                this.mockHostPodVO3, destinations.get(1).getPod());
    }

    @Test
    public void testListByDataCenterIdVMTypeAndStates() {
        // TODO Implement this test
    }

    @Test
    public void testFindOrDeployVirtualRouter() {
        // TODO Implement this test
    }

    @Test
    public void testDeployVirtualRouter() {
        // TODO Implement this test
    }

    @Test
    public void testExecuteDeployment() {
        // TODO Implement this test
    }

    @Test
    public void testPlanDeploymentRouters() {
        // TODO Implement this test
    }

    @Test
    public void testCreateRouterNetworks() {
        // TODO Implement this test
    }

}
