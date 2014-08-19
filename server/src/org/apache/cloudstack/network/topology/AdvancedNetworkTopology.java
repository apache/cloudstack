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

package org.apache.cloudstack.network.topology;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Pod;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Status;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.RuleApplier;
import com.cloud.network.rules.RuleApplierWrapper;
import com.cloud.network.rules.StaticRoutesRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class AdvancedNetworkTopology extends BasicNetworkTopology {

    private static final Logger s_logger = Logger.getLogger(AdvancedNetworkTopology.class);

    @Autowired
    @Qualifier("advancedNetworkVisitor")
    protected AdvancedNetworkVisitor _advancedVisitor;

    @Override
    public boolean applyStaticRoutes(final List<StaticRouteProfile> staticRoutes, final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        
    	if (staticRoutes == null || staticRoutes.isEmpty()) {
            s_logger.debug("No static routes to apply");
            return true;
        }

    	StaticRoutesRules routesRules = _virtualNetworkApplianceFactory.createStaticRoutesRules(staticRoutes);
        
        boolean result = true;
        for (VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
            	
                result = result && routesRules.accept(_advancedVisitor, router);
                
            } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
                s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending StaticRoute command to the backend");
            } else {
                s_logger.warn("Unable to apply StaticRoute, virtual router is not in the right state " + router.getState());

                throw new ResourceUnavailableException("Unable to apply StaticRoute on the backend," + " virtual router is not in the right state", DataCenter.class,
                        router.getDataCenterId());
            }
        }
        return result;
    }

    @Override
    public boolean setupDhcpForPvlan(final boolean isAddPvlan, final DomainRouterVO router, final Long hostId, final NicProfile nic) throws ResourceUnavailableException {

        if (!nic.getBroadCastUri().getScheme().equals("pvlan")) {
            return false;
        }

        DhcpPvlanRules pvlanRules = _virtualNetworkApplianceFactory.createDhcpPvlanRules(isAddPvlan, nic);

        return pvlanRules.accept(_advancedVisitor, router);
    }

    @Override
    public boolean configDhcpForSubnet(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest,
            final List<DomainRouterVO> routers) throws ResourceUnavailableException {

        s_logger.debug("CONFIG DHCP FOR SUBNETS RULES");

        //Asuming we have only one router per network For Now.
        final DomainRouterVO router = routers.get(0);
        if (router.getState() != State.Running) {
            s_logger.warn("Failed to configure dhcp: router not in running state");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR is not in right state " + router.getState(), DataCenter.class,
                    network.getDataCenterId());
        }

        DhcpSubNetRules subNetRules = _virtualNetworkApplianceFactory.createDhcpSubNetRules(network, nic, profile);

        return subNetRules.accept(_advancedVisitor, router);
    }

    @Override
    public boolean applyUserData(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest,
            final List<DomainRouterVO> routers) throws ResourceUnavailableException {

        s_logger.debug("APPLYING VPC USERDATA RULES");

        final String typeString = "userdata and password entry";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        UserdataPwdRules pwdRules = _virtualNetworkApplianceFactory.createUserdataPwdRules(network, nic, profile, dest);

        return applyRules(network, routers, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(pwdRules));
    }

    @Override
    public boolean applyDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException {

        s_logger.debug("APPLYING VPC DHCP ENTRY RULES");

        final String typeString = "dhcp entry";
        final Long podId = null;
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;

        DhcpEntryRules dhcpRules = _virtualNetworkApplianceFactory.createDhcpEntryRules(network, nic, profile, dest);

        return applyRules(network, routers, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(dhcpRules));
    }

    @Override
    public boolean associatePublicIP(final Network network, final List<? extends PublicIpAddress> ipAddresses, final List<? extends VirtualRouter> routers)
            throws ResourceUnavailableException {

        if (ipAddresses == null || ipAddresses.isEmpty()) {
            s_logger.debug("No ip association rules to be applied for network " + network.getId());
            return true;
        }

        //only one router is supported in VPC now
        VirtualRouter router = routers.get(0);

        if (router.getVpcId() == null) {
            return super.associatePublicIP(network, ipAddresses, routers);
        }

        s_logger.debug("APPLYING VPC IP RULES");

        final String typeString = "vpc ip association";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        NicPlugInOutRules nicPlugInOutRules = _virtualNetworkApplianceFactory.createNicPluInOutRules(network, ipAddresses);
        nicPlugInOutRules.accept(_advancedVisitor, router);

        VpcIpAssociationRules ipAssociationRules = _virtualNetworkApplianceFactory.createVpcIpAssociationRules(network, ipAddresses);
        boolean result = applyRules(network, routers, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(ipAssociationRules));

        if (result) {
            _advancedVisitor.visit(nicPlugInOutRules);
        }

        return result;
    }

    @Override
    public boolean applyNetworkACLs(final Network network, final List<? extends NetworkACLItem> rules, final List<? extends VirtualRouter> routers, final boolean isPrivateGateway)
            throws ResourceUnavailableException {

        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No network ACLs to be applied for network " + network.getId());
            return true;
        }

        s_logger.debug("APPLYING NETWORK ACLs RULES");

        final String typeString = "network acls";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        NetworkAclsRules aclsRules = _virtualNetworkApplianceFactory.createNetworkAclRules(network, rules, isPrivateGateway);

        return applyRules(network, routers, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(aclsRules));
    }

    @Override
    public boolean applyRules(final Network network, final List<? extends VirtualRouter> routers, final String typeString, final boolean isPodLevelException, final Long podId, final boolean failWhenDisconnect,
            final RuleApplierWrapper<RuleApplier> ruleApplierWrapper)
                    throws ResourceUnavailableException {

        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Unable to apply " + typeString + ", virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to apply " + typeString, DataCenter.class, network.getDataCenterId());
        }

        RuleApplier ruleApplier =  ruleApplierWrapper.getRuleType();

        final DataCenter dc = _dcDao.findById(network.getDataCenterId());
        final boolean isZoneBasic = (dc.getNetworkType() == NetworkType.Basic);

        // isPodLevelException and podId is only used for basic zone
        assert !((!isZoneBasic && isPodLevelException) || (isZoneBasic && isPodLevelException && podId == null));

        final List<VirtualRouter> connectedRouters = new ArrayList<VirtualRouter>();
        final List<VirtualRouter> disconnectedRouters = new ArrayList<VirtualRouter>();
        boolean result = true;
        final String msg = "Unable to apply " + typeString + " on disconnected router ";
        for (final VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                s_logger.debug("Applying " + typeString + " in network " + network);

                if (router.isStopPending()) {
                    if (_hostDao.findById(router.getHostId()).getState() == Status.Up) {
                        throw new ResourceUnavailableException("Unable to process due to the stop pending router " + router.getInstanceName() +
                                " haven't been stopped after it's host coming back!", DataCenter.class, router.getDataCenterId());
                    }
                    s_logger.debug("Router " + router.getInstanceName() + " is stop pending, so not sending apply " + typeString + " commands to the backend");
                    continue;
                }

                try {
                    ruleApplier.accept(_advancedVisitor, router);

                    connectedRouters.add(router);
                } catch (final AgentUnavailableException e) {
                    s_logger.warn(msg + router.getInstanceName(), e);
                    disconnectedRouters.add(router);
                }

                //If rules fail to apply on one domR and not due to disconnection, no need to proceed with the rest
                if (!result) {
                    if (isZoneBasic && isPodLevelException) {
                        throw new ResourceUnavailableException("Unable to apply " + typeString + " on router ", Pod.class, podId);
                    }
                    throw new ResourceUnavailableException("Unable to apply " + typeString + " on router ", DataCenter.class, router.getDataCenterId());
                }

            } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
                s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending apply " + typeString + " commands to the backend");
            } else {
                s_logger.warn("Unable to apply " + typeString + ", virtual router is not in the right state " + router.getState());
                if (isZoneBasic && isPodLevelException) {
                    throw new ResourceUnavailableException("Unable to apply " + typeString + ", virtual router is not in the right state", Pod.class, podId);
                }
                throw new ResourceUnavailableException("Unable to apply " + typeString + ", virtual router is not in the right state", DataCenter.class,
                        router.getDataCenterId());
            }
        }

        if (!connectedRouters.isEmpty()) {
            if (!isZoneBasic && !disconnectedRouters.isEmpty() && disconnectedRouters.get(0).getIsRedundantRouter()) {
                // These disconnected redundant virtual routers are out of sync now, stop them for synchronization
                _nwHelper.handleSingleWorkingRedundantRouter(connectedRouters, disconnectedRouters, msg);
            }
        } else if (!disconnectedRouters.isEmpty()) {
            for (final VirtualRouter router : disconnectedRouters) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg + router.getInstanceName() + "(" + router.getId() + ")");
                }
            }
            if (isZoneBasic && isPodLevelException) {
                throw new ResourceUnavailableException(msg, Pod.class, podId);
            }
            throw new ResourceUnavailableException(msg, DataCenter.class, disconnectedRouters.get(0).getDataCenterId());
        }

        result = true;
        if (failWhenDisconnect) {
            result = !connectedRouters.isEmpty();
        }
        return result;
    }
}