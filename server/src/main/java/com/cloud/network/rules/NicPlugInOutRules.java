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

package com.cloud.network.rules;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;

public class NicPlugInOutRules extends RuleApplier {

    private static final Logger s_logger = Logger.getLogger(NicPlugInOutRules.class);

    private final List<? extends PublicIpAddress> _ipAddresses;

    private Commands _netUsageCommands;

    public NicPlugInOutRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        super(network);
        _ipAddresses = ipAddresses;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> nicsToChange = getNicsToChangeOnRouter(visitor);

        Map<String, PublicIpAddress> nicsToPlug = nicsToChange.first();
        Map<String, PublicIpAddress> nicsToUnplug = nicsToChange.second();

        NetworkModel networkModel = visitor.getVirtualNetworkApplianceFactory().getNetworkModel();
        VirtualMachineManager itMgr = visitor.getVirtualNetworkApplianceFactory().getItMgr();
        // 1) Unplug the nics
        for (Entry<String, PublicIpAddress> entry : nicsToUnplug.entrySet()) {
            Network publicNtwk = null;
            try {
                publicNtwk = networkModel.getNetwork(entry.getValue().getNetworkId());
                URI broadcastUri = BroadcastDomainType.Vlan.toUri(entry.getKey());
                itMgr.removeVmFromNetwork(_router, publicNtwk, broadcastUri);
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to remove router " + _router + " from vlan " + entry.getKey() + " in public network " + publicNtwk + " due to ", e);
                return false;
            }
        }

        _netUsageCommands = new Commands(Command.OnError.Continue);
        VpcDao vpcDao = visitor.getVirtualNetworkApplianceFactory().getVpcDao();
        VpcVO vpc = vpcDao.findById(_router.getVpcId());

        // 2) Plug the nics
        for (String vlanTag : nicsToPlug.keySet()) {
            PublicIpAddress ip = nicsToPlug.get(vlanTag);
            // have to plug the nic(s)
            NicProfile defaultNic = new NicProfile();
            if (ip.isSourceNat()) {
                defaultNic.setDefaultNic(true);
            }
            defaultNic.setIPv4Address(ip.getAddress().addr());
            defaultNic.setIPv4Gateway(ip.getGateway());
            defaultNic.setIPv4Netmask(ip.getNetmask());
            defaultNic.setMacAddress(ip.getMacAddress());
            defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
            defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
            defaultNic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));

            NicProfile publicNic = null;
            Network publicNtwk = null;
            try {
                publicNtwk = networkModel.getNetwork(ip.getNetworkId());
                publicNic = itMgr.addVmToNetwork(_router, publicNtwk, defaultNic);
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Failed to add router " + _router + " to vlan " + vlanTag + " in public network " + publicNtwk + " due to ", e);
            } catch (InsufficientCapacityException e) {
                s_logger.warn("Failed to add router " + _router + " to vlan " + vlanTag + " in public network " + publicNtwk + " due to ", e);
            } finally {
                if (publicNic == null) {
                    s_logger.warn("Failed to add router " + _router + " to vlan " + vlanTag + " in public network " + publicNtwk);
                    return false;
                }
            }
            // Create network usage commands. Send commands to router after
            // IPAssoc
            NetworkUsageCommand netUsageCmd = new NetworkUsageCommand(_router.getPrivateIpAddress(), _router.getInstanceName(), true, defaultNic.getIPv4Address(), vpc.getCidr());
            _netUsageCommands.addCommand(netUsageCmd);

            UserStatisticsDao userStatsDao = visitor.getVirtualNetworkApplianceFactory().getUserStatsDao();
            UserStatisticsVO stats = userStatsDao.findBy(_router.getAccountId(), _router.getDataCenterId(), publicNtwk.getId(), publicNic.getIPv4Address(), _router.getId(),
                    _router.getType().toString());
            if (stats == null) {
                stats = new UserStatisticsVO(_router.getAccountId(), _router.getDataCenterId(), publicNic.getIPv4Address(), _router.getId(), _router.getType().toString(),
                        publicNtwk.getId());
                userStatsDao.persist(stats);
            }
        }

        // The visit will be done from the AdvancedNetworkTopology, after the
        // VpcIpAssociation is done.
        return true;
    }

    public List<? extends PublicIpAddress> getIpAddresses() {
        return _ipAddresses;
    }

    public Commands getNetUsageCommands() {
        return _netUsageCommands;
    }

    private Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> getNicsToChangeOnRouter(final NetworkTopologyVisitor visitor) {
        // 1) check which nics need to be plugged/unplugged and plug/unplug them

        final Map<String, PublicIpAddress> nicsToPlug = new HashMap<String, PublicIpAddress>();
        final Map<String, PublicIpAddress> nicsToUnplug = new HashMap<String, PublicIpAddress>();

        VpcManager vpcMgr = visitor.getVirtualNetworkApplianceFactory().getVpcMgr();
        NicDao nicDao = visitor.getVirtualNetworkApplianceFactory().getNicDao();
        IPAddressDao ipAddressDao = visitor.getVirtualNetworkApplianceFactory().getIpAddressDao();

        // find out nics to unplug
        for (PublicIpAddress ip : _ipAddresses) {
            long publicNtwkId = ip.getNetworkId();

            // if ip is not associated to any network, and there are no firewall
            // rules, release it on the backend
            if (!vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }

            if (ip.getState() == IpAddress.State.Releasing) {
                NicVO nic = nicDao.findByIp4AddressAndNetworkIdAndInstanceId(publicNtwkId, _router.getId(), ip.getAddress().addr());
                if (nic != null) {
                    final List<IPAddressVO> allIps = ipAddressDao.listByAssociatedVpc(ip.getVpcId(), null);
                    boolean ipUpdated = false;
                    for (IPAddressVO allIp : allIps) {
                        if (allIp.getId() != ip.getId() && allIp.getVlanId() == ip.getVlanId() && allIp.getVmIp() != null) {
                            s_logger.debug("Updating the nic " + nic + " with new ip address " + allIp.getAddress().addr());
                            nic.setIPv4Address(allIp.getAddress().addr());
                            nicDao.update(nic.getId(), nic);
                            ipUpdated = true;
                            break;
                        }
                    }
                    if (!ipUpdated) {
                        nicsToUnplug.put(ip.getVlanTag(), ip);
                        s_logger.debug("Need to unplug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + " in public network id =" + publicNtwkId);
                    }
                }
            }
        }

        // find out nics to plug
        for (PublicIpAddress ip : _ipAddresses) {
            URI broadcastUri = BroadcastDomainType.Vlan.toUri(ip.getVlanTag());
            long publicNtwkId = ip.getNetworkId();

            // if ip is not associated to any network, and there are no firewall
            // rules, release it on the backend
            if (!vpcMgr.isIpAllocatedToVpc(ip)) {
                ip.setState(IpAddress.State.Releasing);
            }

            if (ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) {
                // nic has to be plugged only when there are no nics for this
                // vlan tag exist on VR
                Nic nic = nicDao.findByNetworkIdInstanceIdAndBroadcastUri(publicNtwkId, _router.getId(), broadcastUri.toString());

                if (nic == null && nicsToPlug.get(ip.getVlanTag()) == null) {
                    nicsToPlug.put(ip.getVlanTag(), ip);
                    s_logger.debug("Need to plug the nic for ip=" + ip + "; vlan=" + ip.getVlanTag() + " in public network id =" + publicNtwkId);
                } else {
                    final PublicIpAddress nicToUnplug = nicsToUnplug.get(ip.getVlanTag());
                    if (nicToUnplug != null) {
                        NicVO nicVO = nicDao.findByIp4AddressAndNetworkIdAndInstanceId(publicNtwkId, _router.getId(), nicToUnplug.getAddress().addr());
                        nicVO.setIPv4Address(ip.getAddress().addr());
                        nicDao.update(nicVO.getId(), nicVO);
                        s_logger.debug("Updated the nic " + nicVO + " with the new ip address " + ip.getAddress().addr());
                        nicsToUnplug.remove(ip.getVlanTag());
                    }
                }
            }
        }

        Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>> nicsToChange = new Pair<Map<String, PublicIpAddress>, Map<String, PublicIpAddress>>(nicsToPlug,
                nicsToUnplug);

        return nicsToChange;
    }
}
