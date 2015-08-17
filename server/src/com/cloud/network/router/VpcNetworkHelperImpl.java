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
package com.cloud.network.router;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cloud.network.router.deployment.RouterDeploymentDefinition;

import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.vm.NicProfile;


public class VpcNetworkHelperImpl extends NetworkHelperImpl {

    private static final Logger s_logger = Logger.getLogger(VpcNetworkHelperImpl.class);

    @Inject
    private VlanDao _vlanDao;
    @Inject
    protected VpcManager vpcMgr;
    @Inject
    protected NicProfileHelper nicProfileHelper;

    protected String noHypervisorsErrMsgDetails;

    @PostConstruct
    protected void setupNoHypervisorsErrMsgDetails() {
        noHypervisorsErrMsgDetails = StringUtils.join(vpcMgr.getSupportedVpcHypervisors(), ',');
        noHypervisorsErrMsgDetails += " are the only supported Hypervisors";
    }

    @Override
    protected String getNoHypervisorsErrMsgDetails() {
        return noHypervisorsErrMsgDetails;
    }

    @Override
    protected void filterSupportedHypervisors(final List<HypervisorType> hypervisors) {
        hypervisors.retainAll(vpcMgr.getSupportedVpcHypervisors());
    }

    @Override
    public void reallocateRouterNetworks(final RouterDeploymentDefinition vpcRouterDeploymentDefinition, final VirtualRouter router, final VMTemplateVO template, final HypervisorType hType)
            throws ConcurrentOperationException, InsufficientCapacityException {

        final TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(vpcRouterDeploymentDefinition.getSourceNatIP().getVlanTag());

        //1) allocate nic for control and source nat public ip
        final LinkedHashMap<Network, List<? extends NicProfile>> networks = configureDefaultNics(vpcRouterDeploymentDefinition);

        final Long vpcId = vpcRouterDeploymentDefinition.getVpc().getId();
        //2) allocate nic for private gateways if needed
        final List<PrivateGateway> privateGateways = vpcMgr.getVpcPrivateGateways(vpcId);
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (final PrivateGateway privateGateway : privateGateways) {
                final NicProfile privateNic = nicProfileHelper.createPrivateNicProfileForGateway(privateGateway);
                final Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
                networks.put(privateNetwork, new ArrayList<NicProfile>(Arrays.asList(privateNic)));
            }
        }

        //3) allocate nic for guest gateway if needed
        final List<? extends Network> guestNetworks = vpcMgr.getVpcNetworks(vpcId);
        for (final Network guestNetwork : guestNetworks) {
            if (_networkModel.isPrivateGateway(guestNetwork.getId())) {
                continue;
            }
            if (guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup) {
                final NicProfile guestNic = nicProfileHelper.createGuestNicProfileForVpcRouter(vpcRouterDeploymentDefinition, guestNetwork);
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
            }
        }

        //4) allocate nic for additional public network(s)
        final List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(vpcId, false);
        final List<NicProfile> publicNics = new ArrayList<NicProfile>();
        Network publicNetwork = null;
        for (final IPAddressVO ip : ips) {
            final PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
            if ((ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) && vpcMgr.isIpAllocatedToVpc(ip) &&
                    !publicVlans.contains(publicIp.getVlanTag())) {
                s_logger.debug("Allocating nic for router in vlan " + publicIp.getVlanTag());
                final NicProfile publicNic = new NicProfile();
                publicNic.setDefaultNic(false);
                publicNic.setIPv4Address(publicIp.getAddress().addr());
                publicNic.setIPv4Gateway(publicIp.getGateway());
                publicNic.setIPv4Netmask(publicIp.getNetmask());
                publicNic.setMacAddress(publicIp.getMacAddress());
                publicNic.setBroadcastType(BroadcastDomainType.Vlan);
                publicNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(publicIp.getVlanTag()));
                publicNic.setIsolationUri(IsolationType.Vlan.toUri(publicIp.getVlanTag()));
                final NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
                if (publicNetwork == null) {
                    final List<? extends Network> publicNetworks = _networkMgr.setupNetwork(s_systemAccount, publicOffering, vpcRouterDeploymentDefinition.getPlan(), null, null, false);
                    publicNetwork = publicNetworks.get(0);
                }
                publicNics.add(publicNic);
                publicVlans.add(publicIp.getVlanTag());
            }
        }
        if (publicNetwork != null) {
            if (networks.get(publicNetwork) != null) {
                @SuppressWarnings("unchecked")
                final
                List<NicProfile> publicNicProfiles = (List<NicProfile>)networks.get(publicNetwork);
                publicNicProfiles.addAll(publicNics);
                networks.put(publicNetwork, publicNicProfiles);
            } else {
                networks.put(publicNetwork, publicNics);
            }
        }

        final ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(vpcRouterDeploymentDefinition.getServiceOfferingId());

        _itMgr.allocate(router.getInstanceName(), template, routerOffering, networks, vpcRouterDeploymentDefinition.getPlan(), hType);
    }
}