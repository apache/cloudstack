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

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.StringUtils;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.UserVmDao;

public abstract class RuleApplier {

    protected NetworkModel _networkModel;

    protected LoadBalancingRulesManager _lbMgr;

    protected LoadBalancerDao _loadBalancerDao;

    protected ConfigurationDao _configDao;

    protected NicDao _nicDao;

    protected NetworkOfferingDao _networkOfferingDao;

    protected DataCenterDao _dcDao;

    protected DomainRouterDao _routerDao;

    protected UserVmDao _userVmDao;

    protected ServiceOfferingDao _serviceOfferingDao;

    protected VMTemplateDao _templateDao;

    protected NetworkDao _networkDao;

    protected FirewallRulesDao _rulesDao;

    protected UserStatisticsDao _userStatsDao;

    protected VpcDao _vpcDao;

    protected NicIpAliasDao _nicIpAliasDao;

    protected HostPodDao _podDao;

    protected VlanDao _vlanDao;

    protected IPAddressDao _ipAddressDao;

    protected VpcManager _vpcMgr;

    protected VirtualMachineManager _itMgr;

    protected IpAddressManager _ipAddrMgr;

    protected Network _network;

    protected VirtualRouter _router;

    protected RouterControlHelper _routerControlHelper;

    protected NetworkHelper _networkHelper;

    public RuleApplier(final Network network) {
        _network = network;
    }

    public abstract boolean accept(NetworkTopologyVisitor visitor, VirtualRouter router) throws ResourceUnavailableException;

    public Network getNetwork() {
        return _network;
    }

    public VirtualRouter getRouter() {
        return _router;
    }

    public void createVmDataCommand(final VirtualRouter router, final UserVm vm, final NicVO nic, final String publicKey, final Commands cmds) {
        final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
        final String zoneName = _dcDao.findById(router.getDataCenterId()).getName();
        cmds.addCommand(
                "vmdata",
                generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName, nic.getIp4Address(), vm.getHostName(), vm.getInstanceName(),
                        vm.getId(), vm.getUuid(), publicKey, nic.getNetworkId()));
    }

    public VmDataCommand generateVmDataCommand(final VirtualRouter router, final String vmPrivateIpAddress, final String userData, final String serviceOffering, final String zoneName,
            final String guestIpAddress, final String vmName, final String vmInstanceName, final long vmId, final String vmUuid, final String publicKey, final long guestNetworkId) {
        final VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName, _networkModel.getExecuteInSeqNtwkElmtCmd());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", StringUtils.unicodeEscape(serviceOffering));
        cmd.addVmData("metadata", "availability-zone", StringUtils.unicodeEscape(zoneName));
        cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "local-hostname", StringUtils.unicodeEscape(vmName));
        if (dcVo.getNetworkType() == NetworkType.Basic) {
            cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
            cmd.addVmData("metadata", "public-hostname", StringUtils.unicodeEscape(vmName));
        } else {
            if (router.getPublicIpAddress() == null) {
                cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
            } else {
                cmd.addVmData("metadata", "public-ipv4", router.getPublicIpAddress());
            }
            cmd.addVmData("metadata", "public-hostname", router.getPublicIpAddress());
        }
        if (vmUuid == null) {
            setVmInstanceId(vmInstanceName, vmId, cmd);
        } else {
            setVmInstanceId(vmUuid, cmd);
        }
        cmd.addVmData("metadata", "public-keys", publicKey);

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        } else {
            cloudIdentifier = "CloudStack-{" + cloudIdentifier + "}";
        }
        cmd.addVmData("metadata", "cloud-identifier", cloudIdentifier);

        return cmd;
    }

    private void setVmInstanceId(final String vmUuid, final VmDataCommand cmd) {
        cmd.addVmData("metadata", "instance-id", vmUuid);
        cmd.addVmData("metadata", "vm-id", vmUuid);
    }

    private void setVmInstanceId(final String vmInstanceName, final long vmId, final VmDataCommand cmd) {
        cmd.addVmData("metadata", "instance-id", vmInstanceName);
        cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
    }
}