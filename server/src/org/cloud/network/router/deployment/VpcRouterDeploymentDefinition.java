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
import java.util.LinkedHashMap;
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
import com.cloud.network.router.VpcNetworkHelper;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;

public class VpcRouterDeploymentDefinition extends RouterDeploymentDefinition {
    private static final Logger logger = Logger.getLogger(VpcRouterDeploymentDefinition.class);

    protected DomainRouterDao routerDao;
    protected VpcDao vpcDao;
    protected VpcOfferingDao vpcOffDao;
    protected PhysicalNetworkDao pNtwkDao;
    protected VpcManager vpcMgr;
    protected VlanDao vlanDao;
    protected VpcNetworkHelper vpcNetworkHelper;

    protected Vpc vpc;


    protected VpcRouterDeploymentDefinition(final Vpc vpc, final DeployDestination dest, final Account owner,
            final Map<Param, Object> params, final boolean isRedundant) {

        super(null, dest, owner, params, isRedundant);

        this.vpc = vpc;
    }

    @Override
    public Vpc getVpc() {
        return this.vpc;
    }

    @Override
    public boolean isVpcRouter() {
        return true;
    }

    @Override
    protected void lock() {
        Vpc vpcLock = vpcDao.acquireInLockTable(vpc.getId());
        if (vpcLock == null) {
            throw new ConcurrentOperationException("Unable to lock vpc " + vpc.getId());
        }
        this.tableLockId = vpcLock.getId();
    }

    @Override
    protected void unlock() {
        if (this.tableLockId != null) {
            vpcDao.releaseFromLockTable(this.tableLockId);
            if (logger.isDebugEnabled()) {
                logger.debug("Lock is released for vpc id " + this.tableLockId
                        + " as a part of router startup in " + dest);
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
        destinations.add(this.dest);
        return destinations;
    }

    @Override
    protected int getNumberOfRoutersToDeploy() {
        // TODO Should we make our changes here in order to enable Redundant Router for VPC?
        return this.routers.isEmpty() ? 1 : 0;
    }

    /**
     * @see RouterDeploymentDefinition#prepareDeployment()
     *
     * @return if the deployment can proceed
     */
    @Override
    protected boolean prepareDeployment() {
        return true;
    }

    @Override
    protected void setupPriorityOfRedundantRouter() {
        // Nothing to do for now
        // TODO Shouldn't we add this behavior once Redundant Router works for Vpc too
    }

    @Override
    protected void findSourceNatIP() throws InsufficientAddressCapacityException, ConcurrentOperationException {
        this.sourceNatIp = vpcMgr.assignSourceNatIpAddressToVpc(this.owner, vpc);
    }

    @Override
    protected void findVirtualProvider() {
        List<? extends PhysicalNetwork> pNtwks = pNtwkDao.listByZone(vpc.getZoneId());

        for (PhysicalNetwork pNtwk : pNtwks) {
            PhysicalNetworkServiceProvider provider = physicalProviderDao.findByServiceProvider(pNtwk.getId(), Type.VPCVirtualRouter.toString());
            if (provider == null) {
                throw new CloudRuntimeException("Cannot find service provider " + Type.VPCVirtualRouter.toString() + " in physical network " + pNtwk.getId());
            }
            this.vrProvider = vrProviderDao.findByNspIdAndType(provider.getId(), Type.VPCVirtualRouter);
            if (this.vrProvider != null) {
                break;
            }
        }
    }

    @Override
    protected void findOfferingId() {
        Long vpcOfferingId = vpcOffDao.findById(vpc.getVpcOfferingId()).getServiceOfferingId();
        if (vpcOfferingId != null) {
            this.offeringId = vpcOfferingId;
        }
    }

    @Override
    protected void deployAllVirtualRouters()
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        LinkedHashMap<Network, List<? extends NicProfile>> networks = this.vpcNetworkHelper.createRouterNetworks(this);

        DomainRouterVO router =
                nwHelper.deployRouter(this, networks, true, vpcMgr.getSupportedVpcHypervisors());

        if (router != null) {
            this.routers.add(router);
        }
    }

    @Override
    protected void planDeploymentRouters() {
        this.routers = this.routerDao.listByVpcId(this.vpc.getId());
    }

    @Override
    protected void generateDeploymentPlan() {
        this.plan = new DataCenterDeployment(this.dest.getDataCenter().getId());
    }
}
