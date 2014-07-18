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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;
import org.apache.log4j.Logger;

import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.router.VirtualRouter;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;

public class VpcIpAssociationRules extends RuleApplier {

    private static final Logger s_logger = Logger.getLogger(VpcIpAssociationRules.class);

    private final List<? extends PublicIpAddress> _ipAddresses;

    private Map<String, String> _vlanMacAddress;

    private List<PublicIpAddress> _ipsToSend;

    public VpcIpAssociationRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        super(network);
        _ipAddresses = ipAddresses;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        _vlanMacAddress = new HashMap<String, String>();
        _ipsToSend = new ArrayList<PublicIpAddress>();

        for (PublicIpAddress ipAddr : _ipAddresses) {
            String broadcastURI = BroadcastDomainType.Vlan.toUri(ipAddr.getVlanTag()).toString();
            Nic nic = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(ipAddr.getNetworkId(), router.getId(), broadcastURI);

            String macAddress = null;
            if (nic == null) {
                if (ipAddr.getState() != IpAddress.State.Releasing) {
                    throw new CloudRuntimeException("Unable to find the nic in network " + ipAddr.getNetworkId() + "  to apply the ip address " + ipAddr + " for");
                }
                s_logger.debug("Not sending release for ip address " + ipAddr + " as its nic is already gone from VPC router " + router);
            } else {
                macAddress = nic.getMacAddress();
                _vlanMacAddress.put(BroadcastDomainType.getValue(BroadcastDomainType.fromString(ipAddr.getVlanTag())), macAddress);
                _ipsToSend.add(ipAddr);
            }
        }

        return visitor.visit(this);
    }

    public List<? extends PublicIpAddress> getIpAddresses() {
        return _ipAddresses;
    }

    public Map<String, String> getVlanMacAddress() {
        return _vlanMacAddress;
    }

    public List<PublicIpAddress> getIpsToSend() {
        return _ipsToSend;
    }

    public void createVpcAssociatePublicIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips, final Commands cmds,
            final Map<String, String> vlanMacAddress) {

        Pair<IpAddressTO, Long> sourceNatIpAdd = null;
        Boolean addSourceNat = null;
        // Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            //VR doesn't support release for sourceNat IP address; so reset the state
            if (ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                ipAddress.setState(IpAddress.State.Allocated);
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        for (Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();

            // Get network rate - required for IpAssoc
            Integer networkRate = _networkModel.getNetworkRate(ipAddrList.get(0).getNetworkId(), router.getId());
            Network network = _networkModel.getNetwork(ipAddrList.get(0).getNetworkId());

            IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;

            for (final PublicIpAddress ipAddr : ipAddrList) {
                boolean add = (ipAddr.getState() == IpAddress.State.Releasing ? false : true);

                String macAddress = vlanMacAddress.get(BroadcastDomainType.getValue(BroadcastDomainType.fromString(ipAddr.getVlanTag())));

                IpAddressTO ip =
                        new IpAddressTO(ipAddr.getAccountId(), ipAddr.getAddress().addr(), add, false, ipAddr.isSourceNat(), ipAddr.getVlanTag(), ipAddr.getGateway(),
                                ipAddr.getNetmask(), macAddress, networkRate, ipAddr.isOneToOneNat());

                ip.setTrafficType(network.getTrafficType());
                ip.setNetworkName(_networkModel.getNetworkTag(router.getHypervisorType(), network));
                ipsToSend[i++] = ip;
                if (ipAddr.isSourceNat()) {
                    sourceNatIpAdd = new Pair<IpAddressTO, Long>(ip, ipAddr.getNetworkId());
                    addSourceNat = add;
                }
            }
            IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("IPAssocVpcCommand", cmd);
        }

        //set source nat ip
        if (sourceNatIpAdd != null) {
            IpAddressTO sourceNatIp = sourceNatIpAdd.first();
            SetSourceNatCommand cmd = new SetSourceNatCommand(sourceNatIp, addSourceNat);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
            cmds.addCommand("SetSourceNatCommand", cmd);
        }
    }
}