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
package org.cloud.network.router.deployment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.router.NicProfileHelper;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.vm.DomainRouterVO;

public class VpcRouterDeploymentDefinitionTest extends RouterDeploymentDefinitionTestBase {

    private static final String FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED =
            "For Vpc only the given destination should be used";

    private static final long VPC_ID = 201L;
    private static final long ZONE_ID = 211L;

    @Mock
    protected VpcDao mockVpcDao;
    @Mock
    protected PhysicalNetworkDao mockPhNwDao;
    @Mock
    protected VpcVO mockVpc;
    @Mock
    protected VpcOfferingDao mockVpcOffDao;
    @Mock
    protected VpcManager mockVpcMgr;
    @Mock
    protected NicProfileHelper vpcHelper;

    protected RouterDeploymentDefinition deployment;

    @Override
    protected void initMocks() {
        super.initMocks();
        when(this.mockVpc.getId()).thenReturn(VPC_ID);
        when(this.mockVpc.getZoneId()).thenReturn(ZONE_ID);
        when(this.mockVpc.getVpcOfferingId()).thenReturn(OFFERING_ID);
    }

    @Before
    public void initTest() {
        this.initMocks();

        this.deployment = this.builder.create()
                .setVpc(this.mockVpc)
                .setDeployDestination(this.mockDestination)
                .setAccountOwner(this.mockOwner)
                .setParams(this.params)
                .build();
    }

    @Test
    public void testConstructionFieldsAndFlags() {
        assertTrue("Not really a VpcRouterDeploymentDefinition what the builder created",
                this.deployment instanceof VpcRouterDeploymentDefinition);
        assertTrue("A VpcRouterDeploymentDefinition should declare it is",
                this.deployment.isVpcRouter());
        assertEquals("A VpcRouterDeploymentDefinition should have a Vpc",
                this.mockVpc, this.deployment.getVpc());
    }

    @Test
    public void testLock() {
        // Prepare
        when(this.mockVpcDao.acquireInLockTable(VPC_ID))
        .thenReturn(mockVpc);

        // Execute
        this.deployment.lock();

        // Assert
        verify(this.mockVpcDao, times(1)).acquireInLockTable(VPC_ID);
        assertNotNull(LOCK_NOT_CORRECTLY_GOT, this.deployment.tableLockId);
        assertEquals(LOCK_NOT_CORRECTLY_GOT, VPC_ID, this.deployment.tableLockId.longValue());
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testLockFails() {
        // Prepare
        when(this.mockVpcDao.acquireInLockTable(VPC_ID))
        .thenReturn(null);

        // Execute
        try {
            this.deployment.lock();
        } finally {
            // Assert
            verify(this.mockVpcDao, times(1)).acquireInLockTable(VPC_ID);
            assertNull(this.deployment.tableLockId);
        }
    }

    @Test
    public void testUnlock() {
        // Prepare
        this.deployment.tableLockId = VPC_ID;

        // Execute
        this.deployment.unlock();

        // Assert
        verify(this.mockVpcDao, times(1)).releaseFromLockTable(VPC_ID);
    }

    @Test
    public void testUnlockWithoutLock() {
        // Prepare
        this.deployment.tableLockId = null;

        // Execute
        this.deployment.unlock();

        // Assert
        verify(this.mockVpcDao, times(0)).releaseFromLockTable(anyLong());
    }

    @Test
    public void testFindDestinations() {
        // Execute
        List<DeployDestination> foundDestinations = this.deployment.findDestinations();
        // Assert
        assertEquals(FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED,
                this.deployment.dest, foundDestinations.get(0));
        assertEquals(FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED,
                1, foundDestinations.size());
    }

    @Test
    public void testGetNumberOfRoutersToDeploy() {
        assertEquals("If there are no routers, it should deploy one",
                1, this.deployment.getNumberOfRoutersToDeploy());
        this.deployment.routers.add(mock(DomainRouterVO.class));
        assertEquals("If there is already a router found, there is no need to deploy more",
                0, this.deployment.getNumberOfRoutersToDeploy());
    }

    @Test
    public void testPrepareDeployment() {
        assertTrue("There are no preconditions for Vpc Deployment, thus it should always pass",
                this.deployment.prepareDeployment());
    }

    @Test
    public void testFindVirtualProvider() {
        // Prepare
        final List<PhysicalNetworkVO> pNtwks = new ArrayList<>();
        PhysicalNetworkVO pnw1 = mock(PhysicalNetworkVO.class);
        when(pnw1.getId()).thenReturn(NW_ID_1);
        pNtwks.add(pnw1);
        PhysicalNetworkVO pnw2 = mock(PhysicalNetworkVO.class);
        when(pnw2.getId()).thenReturn(NW_ID_2);
        pNtwks.add(pnw2);

        when(mockPhNwDao.listByZone(ZONE_ID)).thenReturn(pNtwks);

        PhysicalNetworkServiceProviderVO provider1 = mock(PhysicalNetworkServiceProviderVO.class);
        when(this.mockPhysicalProviderDao.findByServiceProvider(NW_ID_1, Type.VPCVirtualRouter.toString()))
            .thenReturn(provider1);
        when(provider1.getId()).thenReturn(PROVIDER_ID_1);
        PhysicalNetworkServiceProviderVO provider2 = mock(PhysicalNetworkServiceProviderVO.class);
        when(this.mockPhysicalProviderDao.findByServiceProvider(NW_ID_2, Type.VPCVirtualRouter.toString()))
            .thenReturn(provider2);
        when(provider2.getId()).thenReturn(PROVIDER_ID_2);

        when(this.mockVrProviderDao.findByNspIdAndType(PROVIDER_ID_1, Type.VPCVirtualRouter))
            .thenReturn(null);
        VirtualRouterProviderVO vrp = mock(VirtualRouterProviderVO.class);
        when(this.mockVrProviderDao.findByNspIdAndType(PROVIDER_ID_2, Type.VPCVirtualRouter))
            .thenReturn(vrp);


        // Execute
        this.deployment.findVirtualProvider();

        // Assert
        assertEquals(vrp, this.deployment.vrProvider);
    }

    @Test
    public void testFindOfferingIdLeavingPrevious() {
        // Prepare
        Long initialOfferingId = this.deployment.offeringId;
        VpcOfferingVO vpcOffering = mock(VpcOfferingVO.class);
        when(this.mockVpcOffDao.findById(OFFERING_ID)).thenReturn(vpcOffering);
        when(vpcOffering.getServiceOfferingId()).thenReturn(null);

        // Execute
        this.deployment.findOfferingId();

        // Assert
        assertEquals("Offering Id shouldn't have been updated",
                initialOfferingId, this.deployment.offeringId);
    }

    @Test
    public void testFindOfferingIdSettingNewOne() {
        // Prepare
        VpcOfferingVO vpcOffering = mock(VpcOfferingVO.class);
        when(this.mockVpcOffDao.findById(OFFERING_ID)).thenReturn(vpcOffering);
        when(vpcOffering.getServiceOfferingId()).thenReturn(OFFERING_ID);

        // Test
        this.deployment.findOfferingId();

        // Assert
        assertEquals("Offering Id should have been updated",
                OFFERING_ID, this.deployment.offeringId.longValue());
    }

    @Test
    public void testGenerateDeploymentPlan() {
        // Execute
        this.deployment.generateDeploymentPlan();

        // Assert
        assertEquals("Destination was not created with the correct Data Center Id",
                DATA_CENTER_ID.longValue(), this.deployment.plan.getDataCenterId());
    }

    @Test
    public void testDeployAllVirtualRouters()
            throws InsufficientAddressCapacityException, InsufficientServerCapacityException,
            StorageUnavailableException, InsufficientCapacityException, ResourceUnavailableException {

        DomainRouterVO router = mock(DomainRouterVO.class);
        this.driveTestDeployAllVirtualRouters(router);

        // Assert
        assertEquals("Router for deployment was not correctly set",
                router, this.deployment.routers.get(0));
        assertEquals("No more than 1 routers should have been set",
                1, this.deployment.routers.size());

    }

    @Test
    public void testDeployAllVirtualRoutersWithNoDeployedRouter()
            throws InsufficientAddressCapacityException, InsufficientServerCapacityException,
            StorageUnavailableException, InsufficientCapacityException, ResourceUnavailableException {

        this.driveTestDeployAllVirtualRouters(null);

        // Assert
        assertTrue("No router should have been set as deployed", this.deployment.routers.isEmpty());

    }

    public void driveTestDeployAllVirtualRouters(final DomainRouterVO router)
            throws InsufficientAddressCapacityException, InsufficientServerCapacityException,
            StorageUnavailableException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        VpcRouterDeploymentDefinition vpcDeployment = (VpcRouterDeploymentDefinition) this.deployment;
        List<HypervisorType> hypervisors = new ArrayList<>();
        when(vpcDeployment.nwHelper.deployRouter(vpcDeployment, true, hypervisors)).thenReturn(router);

        // Execute
        vpcDeployment.deployAllVirtualRouters();
    }

    @Test
    public void testPlanDeploymentRouters() {
        // Prepare
        List<DomainRouterVO> routers = new ArrayList<>();
        when(this.mockRouterDao.listByVpcId(VPC_ID)).thenReturn(routers);

        // Execute
        this.deployment.planDeploymentRouters();

        // Assert
        assertEquals("Routers returned by RouterDao not properly set",
                routers, this.deployment.routers);
    }


    @Test
    public void testFindSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        PublicIp publicIp = mock(PublicIp.class);
        when(this.mockVpcMgr.assignSourceNatIpAddressToVpc(this.mockOwner, this.mockVpc)).thenReturn(publicIp);

        // Execute
        this.deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIp returned by the VpcManager was not correctly set",
                publicIp, this.deployment.sourceNatIp);
    }


}
