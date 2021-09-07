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

import java.util.List;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.AdvancedVpnRules;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.RuleApplier;
import com.cloud.network.rules.RuleApplierWrapper;
import com.cloud.network.rules.StaticRoutesRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AdvancedNetworkTopology extends BasicNetworkTopology {

    private static final Logger s_logger = Logger.getLogger(AdvancedNetworkTopology.class);

    @Autowired
    @Qualifier("advancedNetworkVisitor")
    protected AdvancedNetworkVisitor _advancedVisitor;

    @Override
    public BasicNetworkVisitor getVisitor() {
        return _advancedVisitor;
    }

    @Override
    public String[] applyVpnUsers(final RemoteAccessVpn remoteAccessVpn, final List<? extends VpnUser> users, final VirtualRouter router) throws ResourceUnavailableException {

        s_logger.debug("APPLYING ADVANCED VPN USERS RULES");

        final AdvancedVpnRules routesRules = new AdvancedVpnRules(remoteAccessVpn, users);

        final boolean agentResult = routesRules.accept(_advancedVisitor, router);

        final String[] result = new String[users.size()];
        for (int i = 0; i < result.length; i++) {
            if (agentResult) {
                result[i] = null;
            } else {
                result[i] = String.valueOf(agentResult);
            }
        }

        return result;
    }

    @Override
    public boolean applyStaticRoutes(final List<StaticRouteProfile> staticRoutes, final List<DomainRouterVO> routers) throws ResourceUnavailableException {

        s_logger.debug("APPLYING STATIC ROUTES RULES");

        if (staticRoutes == null || staticRoutes.isEmpty()) {
            s_logger.debug("No static routes to apply");
            return true;
        }

        final StaticRoutesRules routesRules = new StaticRoutesRules(staticRoutes);

        boolean result = true;
        for (final VirtualRouter router : routers) {
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

        s_logger.debug("SETUP DHCP PVLAN RULES");

        if (!nic.getBroadCastUri().getScheme().equals("pvlan")) {
            return false;
        }

        final DhcpPvlanRules pvlanRules = new DhcpPvlanRules(isAddPvlan, nic);

        return pvlanRules.accept(_advancedVisitor, router);
    }

    @Override
    public boolean setupPrivateGateway(final PrivateGateway gateway, final VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("SETUP PRIVATE GATEWAY RULES");

        final PrivateGatewayRules routesRules = new PrivateGatewayRules(gateway);

        return routesRules.accept(_advancedVisitor, router);
    }

    @Override
    public boolean applyUserData(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final DomainRouterVO router)
            throws ResourceUnavailableException {

        s_logger.debug("APPLYING VPC USERDATA RULES");

        final String typeString = "userdata and password entry";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        final UserdataPwdRules pwdRules = new UserdataPwdRules(network, nic, profile, dest);

        return applyRules(network, router, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(pwdRules));
    }

    @Override
    public boolean applyDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest,
            final DomainRouterVO router) throws ResourceUnavailableException {

        s_logger.debug("APPLYING VPC DHCP ENTRY RULES");

        final String typeString = "dhcp entry";
        final Long podId = null;
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;

        final DhcpEntryRules dhcpRules = new DhcpEntryRules(network, nic, profile, dest);

        return applyRules(network, router, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(dhcpRules));
    }

    @Override
    public boolean removeDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile profile, VirtualRouter virtualRouter) throws ResourceUnavailableException {
        s_logger.debug("REMOVE VPC DHCP ENTRY RULES");

        final String typeString = "dhcp entry";
        final Long podId = null;
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;

        final DhcpEntryRules dhcpRules = new DhcpEntryRules(network, nic, profile, null);
        dhcpRules.setRemove(true);

        return applyRules(network, virtualRouter, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(dhcpRules));
    }

    @Override
    public boolean associatePublicIP(final Network network, final List<? extends PublicIpAddress> ipAddresses, final VirtualRouter router)
            throws ResourceUnavailableException {

        if (ipAddresses == null || ipAddresses.isEmpty()) {
            s_logger.debug("No ip association rules to be applied for network " + network.getId());
            return true;
        }

        if (network.getVpcId() == null) {
            return super.associatePublicIP(network, ipAddresses, router);
        }

        s_logger.debug("APPLYING VPC IP RULES");

        final String typeString = "vpc ip association";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        final NicPlugInOutRules nicPlugInOutRules = new NicPlugInOutRules(network, ipAddresses);
        nicPlugInOutRules.accept(_advancedVisitor, router);

        final VpcIpAssociationRules ipAssociationRules = new VpcIpAssociationRules(network, ipAddresses);
        final boolean result = applyRules(network, router, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(ipAssociationRules));

        if (result) {
            if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
                s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending NicPlugInOutRules command to the backend");
            } else {
                _advancedVisitor.visit(nicPlugInOutRules);
            }
        }

        return result;
    }

    @Override
    public boolean applyNetworkACLs(final Network network, final List<? extends NetworkACLItem> rules, final VirtualRouter router, final boolean isPrivateGateway)
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

        final NetworkAclsRules aclsRules = new NetworkAclsRules(network, rules, isPrivateGateway);

        final boolean result = applyRules(network, router, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(aclsRules));
        return result;
    }
}
