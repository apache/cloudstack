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

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.router.NicProfileHelper;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.vm.DomainRouterVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VpcRouterDeploymentDefinitionTest extends RouterDeploymentDefinitionTestBase {

    private static final String FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED = "For Vpc only the given destination should be used";

    private static final long VPC_ID = 201L;
    public static final long VPC_OFFERING_ID = 210L;

    @Mock
    protected VpcDao mockVpcDao;
    @Mock
    protected NsxProviderDao nsxProviderDao;
    @Mock
    protected PhysicalNetworkDao mockPhNwDao;
    protected PhysicalNetworkServiceProviderDao mockPhProviderDao;

    @Mock
    protected VpcVO mockVpc;

    @Mock
    protected VpcOfferingDao mockVpcOffDao;
    @Mock
    protected VpcManager vpcMgr;
    @Mock
    protected NicProfileHelper vpcHelper;

    protected RouterDeploymentDefinition deployment;

    @Override
    protected void initMocks() {
        super.initMocks();
        when(mockVpc.getId()).thenReturn(VPC_ID);
        lenient().when(mockVpc.getZoneId()).thenReturn(VPC_ID);
        when(mockVpc.getVpcOfferingId()).thenReturn(VPC_OFFERING_ID);
    }

    @Before
    public void initTest() {
        initMocks();

        deployment = builder.create().setVpc(mockVpc).setDeployDestination(mockDestination).setAccountOwner(mockOwner).setParams(params).build();
    }

    @Test
    public void testConstructionFieldsAndFlags() {
        assertTrue("Not really a VpcRouterDeploymentDefinition what the builder created", deployment instanceof VpcRouterDeploymentDefinition);
        assertTrue("A VpcRouterDeploymentDefinition should declare it is", deployment.isVpcRouter());
        assertEquals("A VpcRouterDeploymentDefinition should have a Vpc", mockVpc, deployment.getVpc());
    }

    @Test
    public void testLock() {
        // Prepare
        when(mockVpcDao.acquireInLockTable(VPC_ID)).thenReturn(mockVpc);

        // Execute
        deployment.lock();

        // Assert
        verify(mockVpcDao, times(1)).acquireInLockTable(VPC_ID);
        assertNotNull(LOCK_NOT_CORRECTLY_GOT, deployment.tableLockId);
        assertEquals(LOCK_NOT_CORRECTLY_GOT, VPC_ID, deployment.tableLockId.longValue());
    }

    @Test(expected = ConcurrentOperationException.class)
    public void testLockFails() {
        // Prepare
        when(mockVpcDao.acquireInLockTable(VPC_ID)).thenReturn(null);

        // Execute
        try {
            deployment.lock();
        } finally {
            // Assert
            verify(mockVpcDao, times(1)).acquireInLockTable(VPC_ID);
            assertNull(deployment.tableLockId);
        }
    }

    @Test
    public void testUnlock() {
        // Prepare
        deployment.tableLockId = VPC_ID;

        // Execute
        deployment.unlock();

        // Assert
        verify(mockVpcDao, times(1)).releaseFromLockTable(VPC_ID);
    }

    @Test
    public void testUnlockWithoutLock() {
        // Prepare
        deployment.tableLockId = null;

        // Execute
        deployment.unlock();

        // Assert
        verify(mockVpcDao, times(0)).releaseFromLockTable(anyLong());
    }

    @Test
    public void testFindDestinations() {
        // Execute
        final List<DeployDestination> foundDestinations = deployment.findDestinations();
        // Assert
        assertEquals(FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED, deployment.dest, foundDestinations.get(0));
        assertEquals(FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED, 1, foundDestinations.size());
    }

    @Test
    public void testGetNumberOfRoutersToDeploy() {
        assertEquals("If there are no routers, it should deploy one", 1, deployment.getNumberOfRoutersToDeploy());
        deployment.routers.add(mock(DomainRouterVO.class));
        assertEquals("If there is already a router found, there is no need to deploy more", 0, deployment.getNumberOfRoutersToDeploy());
    }

    protected void driveTestPrepareDeployment(final boolean isRedundant, final boolean isPublicNw) {
        // Prepare
        when(vpcMgr.isSrcNatIpRequired(mockVpc.getVpcOfferingId())).thenReturn(isPublicNw);

        // Execute
        final boolean canProceedDeployment = deployment.prepareDeployment();
        // Assert
        assertTrue("There are no preconditions for Vpc Deployment, thus it should always pass", canProceedDeployment);
        assertEquals(isPublicNw, deployment.isPublicNetwork());
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

    @Test
    public void testGenerateDeploymentPlan() {
        // TODO Implement this test
    }

    @Test
    public void testFindOfferingIdDefault() {
        // Prepare
        final VpcOfferingVO vpcOffering = mock(VpcOfferingVO.class);
        when(mockVpcOffDao.findById(VPC_OFFERING_ID)).thenReturn(vpcOffering);
        when(vpcOffering.getServiceOfferingId()).thenReturn(null);
        when(mockServiceOfferingDao.findDefaultSystemOffering(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).thenReturn(mockSvcOfferingVO);
        when(mockSvcOfferingVO.getId()).thenReturn(DEFAULT_OFFERING_ID);

        // Execute
        deployment.findServiceOfferingId();

        // Assert
        assertEquals("Since there is no service offering associated with VPC offering, offering id should have matched default one",
                DEFAULT_OFFERING_ID, deployment.serviceOfferingId.longValue());
    }

    @Test
    public void testFindOfferingIdFromVPC() {
        // Prepare
        final VpcOfferingVO vpcOffering = mock(VpcOfferingVO.class);
        when(mockVpcOffDao.findById(VPC_OFFERING_ID)).thenReturn(vpcOffering);
        when(vpcOffering.getServiceOfferingId()).thenReturn(VPC_OFFERING_ID);

        // Test
        deployment.findServiceOfferingId();

        // Assert
        assertEquals("Service offering id not matching the one associated with VPC offering",
                VPC_OFFERING_ID, deployment.serviceOfferingId.longValue());
    }

    @Test
    public void testPlanDeploymentRouters() {
        // TODO Implement this test
    }

    @Test
    public void testDeployAllVirtualRoutersWithNoDeployedRouter() throws InsufficientAddressCapacityException, InsufficientServerCapacityException, StorageUnavailableException,
    InsufficientCapacityException, ResourceUnavailableException {

        driveTestDeployAllVirtualRouters(null);

        // Assert
        assertTrue("No router should have been set as deployed", deployment.routers.isEmpty());

    }

    public void driveTestDeployAllVirtualRouters(final DomainRouterVO router) throws InsufficientAddressCapacityException, InsufficientServerCapacityException,
    StorageUnavailableException, InsufficientCapacityException, ResourceUnavailableException {
        // Prepare
        final VpcRouterDeploymentDefinition vpcDeployment = (VpcRouterDeploymentDefinition) deployment;
        lenient().when(vpcDeployment.nwHelper.deployRouter(vpcDeployment, true)).thenReturn(router);

        // Execute
        vpcDeployment.deployAllVirtualRouters();
    }

    @Test
    public void testCreateVpcRouterNetworks() {
        // TODO Implement this test
    }

    @Test
    public void testFindSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        // Prepare
        final PublicIp publicIp = mock(PublicIp.class);
        when(vpcMgr.assignSourceNatIpAddressToVpc(mockOwner, mockVpc, null)).thenReturn(publicIp);
        deployment.isPublicNetwork = true;

        // Execute
        deployment.findSourceNatIP();

        // Assert
        assertEquals("SourceNatIp returned by the VpcManager was not correctly set", publicIp, deployment.sourceNatIp);
    }

    @Test
    public void testRedundancyProperty() {
        // Set and confirm is redundant
        when(mockVpc.isRedundant()).thenReturn(true);
        final RouterDeploymentDefinition deployment = builder.create()
                .setVpc(mockVpc)
                .setDeployDestination(mockDestination)
                .build();
        assertTrue("The builder ignored redundancy from its inner network", deployment.isRedundant());
        when(mockVpc.isRedundant()).thenReturn(false);
        assertFalse("The builder ignored redundancy from its inner network", deployment.isRedundant());
    }
}
