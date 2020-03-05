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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineProfile.Param;

public class VpcRouterDeploymentDefinition extends RouterDeploymentDefinition {
    private static final Logger logger = Logger.getLogger(VpcRouterDeploymentDefinition.class);

    protected VpcDao vpcDao;
    protected VpcOfferingDao vpcOffDao;
    protected PhysicalNetworkDao pNtwkDao;
    protected VpcManager vpcMgr;
    protected VlanDao vlanDao;

    protected Vpc vpc;

    protected VpcRouterDeploymentDefinition(final Network guestNetwork, final Vpc vpc, final DeployDestination dest, final Account owner,
            final Map<Param, Object> params) {

        super(guestNetwork, dest, owner, params);

        this.vpc = vpc;
    }

    @Override
    public Vpc getVpc() {
        return vpc;
    }

    @Override
    public boolean isVpcRouter() {
        return true;
    }

    @Override
    protected void lock() {
        final Vpc vpcLock = vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }
        tableLockId = vpcLock.getId();
    }

    @Override
    protected void unlock() {
        if (tableLockId != null) {
            vpcDao.releaseFromLockTable(tableLockId);
            if (logger.isDebugEnabled()) {
                logger.debug("Lock is released for vpc id " + tableLockId + " as a part of router startup in " + dest);
            }
        }
    }

    @Override
    protected void checkPreconditions() {
        // No preconditions for Vpc
    }

    @Override
    protected List<DeployDestination> findDestinations() {
        final List<DeployDestination> destinations = new ArrayList<>();
        destinations.add(dest);
        return destinations;
    }

    /**
     * @see RouterDeploymentDefinition#prepareDeployment()
     *
     * @return if the deployment can proceed
     */
    @Override
    protected boolean prepareDeployment() {
        //Check if the VR is the src NAT provider...
        isPublicNetwork = vpcMgr.isSrcNatIpRequired(vpc.getVpcOfferingId());

        // Check if public network has to be set on VR
        return true;
    }

    @Override
    protected void findSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        sourceNatIp = null;
        if (isPublicNetwork) {
            sourceNatIp = vpcMgr.assignSourceNatIpAddressToVpc(owner, vpc);
        }
    }

    @Override
    protected void findOrDeployVirtualRouter() throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        final Vpc vpc = getVpc();
        if (vpc != null) {
            // This call will associate any existing router to the "routers" attribute.
            // It's needed in order to continue with the VMs deployment.
            planDeploymentRouters();
            if (routers.size() == MAX_NUMBER_OF_ROUTERS) {
                // If we have 2 routers already deployed, do nothing and return.
                return;
            }
        }
        super.findOrDeployVirtualRouter();
    }

    @Override
    protected void findVirtualProvider() {
        final List<? extends PhysicalNetwork> pNtwks = pNtwkDao.listByZone(vpc.getZoneId());

        for (final PhysicalNetwork pNtwk : pNtwks) {
            final PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(pNtwk.getId(), Type.VPCVirtualRouter.toString());
            if (provider == null) {
                throw new CloudRuntimeException("Cannot find service provider " + Type.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
            }
            vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), Type.VPCVirtualRouter);
            if (vrProvider != null) {
                break;
            }
        }
    }

    @Override
    protected void findServiceOfferingId() {
        serviceOfferingId = vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
        if (serviceOfferingId == null) {
            findAccountServiceOfferingId(vpc.getAccountId());
        }
        if (serviceOfferingId == null) {
            findDefaultServiceOfferingId();
        }
    }

    @Override
    protected void deployAllVirtualRouters() throws ConcurrentOperationException, InsufficientCapacityException,
    ResourceUnavailableException {

        // Implement Redundant Vpc
        final int routersToDeploy = getNumberOfRoutersToDeploy();
        for(int i = 0; i < routersToDeploy; i++) {
            // Don't start the router as we are holding the network lock that needs to be released at the end of router allocation
            final DomainRouterVO router = nwHelper.deployRouter(this, false);

            if (router != null) {
                routers.add(router);
            }
        }
    }

    @Override
    protected void planDeploymentRouters() {
        routers = routerDao.listByVpcId(vpc.getId());
    }

    @Override
    public void generateDeploymentPlan() {
        plan = new DataCenterDeployment(dest.getDataCenter().getId());
    }

    @Override
    public boolean isRedundant() {
        return vpc.isRedundant();
    }

    @Override
    public boolean isRollingRestart() {
        return vpc.isRollingRestart();
    }
}
