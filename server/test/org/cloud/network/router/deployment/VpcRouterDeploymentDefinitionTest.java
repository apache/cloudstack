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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.vm.DomainRouterVO;

public class VpcRouterDeploymentDefinitionTest extends RouterDeploymentDefinitionTestBase {

    private static final String FOR_VPC_ONLY_THE_GIVEN_DESTINATION_SHOULD_BE_USED = "For Vpc only the given destination should be used";

    private static final long VPC_ID = 201L;
    private static final long ZONE_ID = 211L;

    @Mock
    protected VpcDao mockVpcDao;
    @Mock
    protected PhysicalNetworkDao mockPhNwDao;
    protected PhysicalNetworkServiceProviderDao mockPhProviderDao;

    @Mock
    protected VpcVO mockVpc;

    protected RouterDeploymentDefinition deployment;

    @Override
    protected void initMocks() {
        super.initMocks();
        when(this.mockVpc.getId()).thenReturn(VPC_ID);
        when(this.mockVpc.getZoneId()).thenReturn(VPC_ID);
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
    public void testGenerateDeploymentPlan() {
        // TODO Implement this test
    }

    @Test
    public void testCheckPreconditions() {
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
    public void testDeployVpcRouter() {
        // TODO Implement this test
    }

    @Test
    public void testCreateVpcRouterNetworks() {
        // TODO Implement this test
    }

}
