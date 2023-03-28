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

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.router.VirtualRouter;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.dao.NicDao;

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

        NicDao nicDao = visitor.getVirtualNetworkApplianceFactory().getNicDao();
        for (PublicIpAddress ipAddr : _ipAddresses) {
            String broadcastURI = BroadcastDomainType.Vlan.toUri(ipAddr.getVlanTag()).toString();
            Nic nic = nicDao.findByNetworkIdInstanceIdAndBroadcastUri(ipAddr.getNetworkId(), _router.getId(), broadcastURI);

            String macAddress = null;
            if (nic == null) {
                if (ipAddr.getState() != IpAddress.State.Releasing) {
                    throw new CloudRuntimeException("Unable to find the nic in network " + ipAddr.getNetworkId() + "  to apply the ip address " + ipAddr + " for");
                }
                s_logger.debug("Not sending release for ip address " + ipAddr + " as its nic is already gone from VPC router " + _router);
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
}
