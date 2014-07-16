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
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NEWVirtualNetworkApplianceManager;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.StringUtils;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

public abstract class RuleApplier {

    protected NEWVirtualNetworkApplianceManager applianceManager;

    protected NetworkModel networkModel;

    protected LoadBalancingRulesManager lbMgr;

    protected LoadBalancerDao loadBalancerDao;

    protected ConfigurationDao configDao;

    protected NicDao nicDao;

    protected NetworkOfferingDao networkOfferingDao;

    protected DataCenterDao dcDao;

    protected DomainRouterDao routerDao;

    protected UserVmDao userVmDao;

    protected ServiceOfferingDao serviceOfferingDao;

    protected VMTemplateDao templateDao;

    protected NetworkDao networkDao;

    protected FirewallRulesDao rulesDao;

    protected VirtualMachineManager itMgr;

    protected Network network;

    protected VirtualRouter router;

    protected RouterControlHelper routerControlHelper;

    public RuleApplier(final Network network) {
        this.network = network;
    }

    public abstract boolean accept(NetworkTopologyVisitor visitor, VirtualRouter router) throws ResourceUnavailableException;

    public Network getNetwork() {
        return network;
    }

    public VirtualRouter getRouter() {
        return router;
    }

    public NEWVirtualNetworkApplianceManager getApplianceManager() {
        return applianceManager;
    }

    public void createVmDataCommand(final VirtualRouter router, final UserVm vm, final NicVO nic, final String publicKey, final Commands cmds) {
        final String serviceOffering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
        final String zoneName = dcDao.findById(router.getDataCenterId()).getName();
        cmds.addCommand(
                "vmdata",
                generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName, nic.getIp4Address(), vm.getHostName(), vm.getInstanceName(),
                        vm.getId(), vm.getUuid(), publicKey, nic.getNetworkId()));
    }

    public VmDataCommand generateVmDataCommand(final VirtualRouter router, final String vmPrivateIpAddress, final String userData, final String serviceOffering, final String zoneName,
            final String guestIpAddress, final String vmName, final String vmInstanceName, final long vmId, final String vmUuid, final String publicKey, final long guestNetworkId) {
        final VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName, networkModel.getExecuteInSeqNtwkElmtCmd());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
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

        String cloudIdentifier = configDao.getValue("cloud.identifier");
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