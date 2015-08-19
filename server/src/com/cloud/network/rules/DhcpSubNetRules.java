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
import java.util.List;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicIpAliasVO;
import com.cloud.vm.dao.UserVmDao;

public class DhcpSubNetRules extends RuleApplier {

    private static final Logger s_logger = Logger.getLogger(DhcpSubNetRules.class);

    private final NicProfile _nic;
    private final VirtualMachineProfile _profile;

    private NicIpAliasVO _nicAlias;
    private String _routerAliasIp;

    public DhcpSubNetRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        super(network);

        _nic = nic;
        _profile = profile;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        UserVmDao userVmDao = visitor.getVirtualNetworkApplianceFactory().getUserVmDao();
        final UserVmVO vm = userVmDao.findById(_profile.getId());
        userVmDao.loadDetails(vm);

        NicDao nicDao = visitor.getVirtualNetworkApplianceFactory().getNicDao();
        // check if this is not the primary subnet.
        final NicVO domrGuestNic = nicDao.findByInstanceIdAndIpAddressAndVmtype(_router.getId(), nicDao.getIpAddress(_nic.getNetworkId(), _router.getId()),
                VirtualMachine.Type.DomainRouter);
        // check if the router ip address and the vm ip address belong to same
        // subnet.
        // if they do not belong to same netwoek check for the alias ips. if not
        // create one.
        // This should happen only in case of Basic and Advanced SG enabled
        // networks.
        if (!NetUtils.sameSubnet(domrGuestNic.getIPv4Address(), _nic.getIPv4Address(), _nic.getIPv4Netmask())) {
            final NicIpAliasDao nicIpAliasDao = visitor.getVirtualNetworkApplianceFactory().getNicIpAliasDao();
            final List<NicIpAliasVO> aliasIps = nicIpAliasDao.listByNetworkIdAndState(domrGuestNic.getNetworkId(), NicIpAlias.state.active);
            boolean ipInVmsubnet = false;
            for (final NicIpAliasVO alias : aliasIps) {
                // check if any of the alias ips belongs to the Vm's subnet.
                if (NetUtils.sameSubnet(alias.getIp4Address(), _nic.getIPv4Address(), _nic.getIPv4Netmask())) {
                    ipInVmsubnet = true;
                    break;
                }
            }

            PublicIp routerPublicIP = null;
            DataCenterDao dcDao = visitor.getVirtualNetworkApplianceFactory().getDcDao();
            final DataCenter dc = dcDao.findById(_router.getDataCenterId());
            if (ipInVmsubnet == false) {
                try {
                    if (_network.getTrafficType() == TrafficType.Guest && _network.getGuestType() == GuestType.Shared) {
                        HostPodDao podDao = visitor.getVirtualNetworkApplianceFactory().getPodDao();
                        podDao.findById(vm.getPodIdToDeployIn());
                        final Account caller = CallContext.current().getCallingAccount();

                        VlanDao vlanDao = visitor.getVirtualNetworkApplianceFactory().getVlanDao();
                        final List<VlanVO> vlanList = vlanDao.listVlansByNetworkIdAndGateway(_network.getId(), _nic.getIPv4Gateway());
                        final List<Long> vlanDbIdList = new ArrayList<Long>();
                        for (final VlanVO vlan : vlanList) {
                            vlanDbIdList.add(vlan.getId());
                        }
                        IpAddressManager ipAddrMgr = visitor.getVirtualNetworkApplianceFactory().getIpAddrMgr();
                        if (dc.getNetworkType() == NetworkType.Basic) {
                            routerPublicIP = ipAddrMgr.assignPublicIpAddressFromVlans(_router.getDataCenterId(), vm.getPodIdToDeployIn(), caller, Vlan.VlanType.DirectAttached,
                                    vlanDbIdList, _nic.getNetworkId(), null, false);
                        } else {
                            routerPublicIP = ipAddrMgr.assignPublicIpAddressFromVlans(_router.getDataCenterId(), null, caller, Vlan.VlanType.DirectAttached, vlanDbIdList,
                                    _nic.getNetworkId(), null, false);
                        }

                        _routerAliasIp = routerPublicIP.getAddress().addr();
                    }
                } catch (final InsufficientAddressCapacityException e) {
                    s_logger.info(e.getMessage());
                    s_logger.info("unable to configure dhcp for this VM.");
                    return false;
                }
                // this means we did not create an IP alias on the router.
                _nicAlias = new NicIpAliasVO(domrGuestNic.getId(), _routerAliasIp, _router.getId(), CallContext.current().getCallingAccountId(), _network.getDomainId(),
                        _nic.getNetworkId(), _nic.getIPv4Gateway(), _nic.getIPv4Netmask());
                _nicAlias.setAliasCount(routerPublicIP.getIpMacAddress());
                nicIpAliasDao.persist(_nicAlias);

                final boolean result = visitor.visit(this);

                if (result == false) {
                    final NicIpAliasVO ipAliasVO = nicIpAliasDao.findByInstanceIdAndNetworkId(_network.getId(), _router.getId());
                    final PublicIp routerPublicIPFinal = routerPublicIP;
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(final TransactionStatus status) {
                            nicIpAliasDao.expunge(ipAliasVO.getId());

                            IPAddressDao ipAddressDao = visitor.getVirtualNetworkApplianceFactory().getIpAddressDao();
                            ipAddressDao.unassignIpAddress(routerPublicIPFinal.getId());
                        }
                    });
                    throw new CloudRuntimeException("failed to configure ip alias on the router as a part of dhcp config");
                }
            }
            return true;
        }
        return true;
    }

    public NicIpAliasVO getNicAlias() {
        return _nicAlias;
    }

    public String getRouterAliasIp() {
        return _routerAliasIp;
    }
}