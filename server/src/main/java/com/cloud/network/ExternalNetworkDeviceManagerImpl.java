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
package com.cloud.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.network.AddNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkDeviceCmd;
import org.apache.cloudstack.api.response.NetworkDeviceResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.dao.ExternalFirewallDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalFirewallDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Component
public class ExternalNetworkDeviceManagerImpl extends ManagerBase implements ExternalNetworkDeviceManager {

    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    UserStatisticsDao _userStatsDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VpnUserDao _vpnUsersDao;
    @Inject
    InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject
    ExternalFirewallDeviceDao _externalFirewallDeviceDao;
    @Inject
    NetworkExternalLoadBalancerDao _networkExternalLBDao;
    @Inject
    NetworkExternalFirewallDao _networkExternalFirewallDao;

    ScheduledExecutorService _executor;
    int _externalNetworkStatsInterval;

    // obsolete
    // private final static IdentityService _identityService = (IdentityService)ComponentLocator.getLocator(ManagementServer.Name).getManager(IdentityService.class);


    @Override
    public Host addNetworkDevice(AddNetworkDeviceCmd cmd) {
        Map paramList = cmd.getParamList();
        if (paramList == null) {
            throw new CloudRuntimeException("Parameter list is null");
        }

        Collection paramsCollection = paramList.values();
        HashMap params = (HashMap)(paramsCollection.toArray())[0];
        return null;
    }

    @Override
    public NetworkDeviceResponse getApiResponse(Host device) {
        return null;
    }

    private List<Host> listNetworkDevice(Long zoneId, Long physicalNetworkId, Long podId, Host.Type type) {
//        List<Host> res = new ArrayList<Host>();
//        if (podId != null) {
//            List<HostVO> devs = _hostDao.listBy(type, null, podId, zoneId);
//            if (devs.size() == 1) {
//                res.add(devs.get(0));
//            } else {
//                logger.debug("List " + type + ": " + devs.size() + " found");
//            }
//        } else {
//            List<HostVO> devs = _hostDao.listBy(type, zoneId);
//            res.addAll(devs);
        //       }

        //       return res;
        return null;
    }

    @Override
    public List<Host> listNetworkDevice(ListNetworkDeviceCmd cmd) {
        Map paramList = cmd.getParamList();
        if (paramList == null) {
            throw new CloudRuntimeException("Parameter list is null");
        }

        List<Host> res;
        Collection paramsCollection = paramList.values();
        HashMap params = (HashMap)(paramsCollection.toArray())[0];
        if (NetworkDevice.ExternalDhcp.getName().equalsIgnoreCase(cmd.getDeviceType())) {
            Long zoneId = Long.parseLong((String)params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.ExternalDhcp);
        } else if (NetworkDevice.PxeServer.getName().equalsIgnoreCase(cmd.getDeviceType())) {
            Long zoneId = Long.parseLong((String)params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.PxeServer);
        } else if (cmd.getDeviceType() == null) {
            Long zoneId = Long.parseLong((String)params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            Long physicalNetworkId = (params.get(ApiConstants.PHYSICAL_NETWORK_ID) == null) ? Long.parseLong((String)params.get(ApiConstants.PHYSICAL_NETWORK_ID)) : null;
            List<Host> res1 = listNetworkDevice(zoneId, physicalNetworkId, podId, Host.Type.PxeServer);
            List<Host> res2 = listNetworkDevice(zoneId, physicalNetworkId, podId, Host.Type.ExternalDhcp);
            List<Host> res3 = listNetworkDevice(zoneId, physicalNetworkId, podId, Host.Type.ExternalLoadBalancer);
            List<Host> res4 = listNetworkDevice(zoneId, physicalNetworkId, podId, Host.Type.ExternalFirewall);
            List<Host> deviceAll = new ArrayList<Host>();
            deviceAll.addAll(res1);
            deviceAll.addAll(res2);
            deviceAll.addAll(res3);
            deviceAll.addAll(res4);
            res = deviceAll;
        } else {
            throw new CloudRuntimeException("Unknown network device type:" + cmd.getDeviceType());
        }

        return res;
    }

    @Override
    public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd) {
        HostVO device = _hostDao.findById(cmd.getId());
        return true;
    }
}
