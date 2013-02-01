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

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.network.AddNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkDeviceCmd;
import org.apache.cloudstack.api.response.NetworkDeviceResponse;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.baremetal.ExternalDhcpManager;
import com.cloud.baremetal.PxeServerManager;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.baremetal.PxeServerProfile;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
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
import com.cloud.server.api.response.NwDeviceDhcpResponse;
import com.cloud.server.api.response.PxePingResponse;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Component
@Local(value = {ExternalNetworkDeviceManager.class})
public class ExternalNetworkDeviceManagerImpl extends ManagerBase implements ExternalNetworkDeviceManager {

    @Inject ExternalDhcpManager _dhcpMgr;
    @Inject PxeServerManager _pxeMgr;
    @Inject AgentManager _agentMgr;
    @Inject NetworkModel _networkMgr;
    @Inject HostDao _hostDao;
    @Inject DataCenterDao _dcDao;
    @Inject AccountDao _accountDao;
    @Inject DomainRouterDao _routerDao;
    @Inject IPAddressDao _ipAddressDao;
    @Inject VlanDao _vlanDao;
    @Inject UserStatisticsDao _userStatsDao;
    @Inject NetworkDao _networkDao;
    @Inject PortForwardingRulesDao _portForwardingRulesDao;
    @Inject LoadBalancerDao _loadBalancerDao;
    @Inject ConfigurationDao _configDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject NicDao _nicDao;
    @Inject VpnUserDao _vpnUsersDao;
    @Inject InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject AccountManager _accountMgr;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject ExternalFirewallDeviceDao _externalFirewallDeviceDao;
    @Inject NetworkExternalLoadBalancerDao _networkExternalLBDao;
    @Inject NetworkExternalFirewallDao _networkExternalFirewallDao;

    ScheduledExecutorService _executor;
    int _externalNetworkStatsInterval;

    // obsolete
    // private final static IdentityService _identityService = (IdentityService)ComponentLocator.getLocator(ManagementServer.Name).getManager(IdentityService.class); 

    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalNetworkDeviceManagerImpl.class);

    @Override
    public Host addNetworkDevice(AddNetworkDeviceCmd cmd) {
        Map paramList = cmd.getParamList();
        if (paramList == null) {
            throw new CloudRuntimeException("Parameter list is null");
        }

        Collection paramsCollection = paramList.values();
        HashMap params = (HashMap) (paramsCollection.toArray())[0];
        if (cmd.getDeviceType().equalsIgnoreCase(NetworkDevice.ExternalDhcp.getName())) {
            //Long zoneId = _identityService.getIdentityId("data_center", (String) params.get(ApiConstants.ZONE_ID));
            //Long podId = _identityService.getIdentityId("host_pod_ref", (String)params.get(ApiConstants.POD_ID));
            Long zoneId = Long.valueOf((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.valueOf((String)params.get(ApiConstants.POD_ID));
            String type = (String) params.get(ApiConstants.DHCP_SERVER_TYPE);
            String url = (String) params.get(ApiConstants.URL);
            String username = (String) params.get(ApiConstants.USERNAME);
            String password = (String) params.get(ApiConstants.PASSWORD);

            return _dhcpMgr.addDhcpServer(zoneId, podId, type, url, username, password);
        } else if (cmd.getDeviceType().equalsIgnoreCase(NetworkDevice.PxeServer.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            //Long zoneId = _identityService.getIdentityId("data_center", (String) params.get(ApiConstants.ZONE_ID));
            //Long podId = _identityService.getIdentityId("host_pod_ref", (String)params.get(ApiConstants.POD_ID));
            String type = (String) params.get(ApiConstants.PXE_SERVER_TYPE);
            String url = (String) params.get(ApiConstants.URL);
            String username = (String) params.get(ApiConstants.USERNAME);
            String password = (String) params.get(ApiConstants.PASSWORD);
            String pingStorageServerIp = (String) params.get(ApiConstants.PING_STORAGE_SERVER_IP);
            String pingDir = (String) params.get(ApiConstants.PING_DIR);
            String tftpDir = (String) params.get(ApiConstants.TFTP_DIR);
            String pingCifsUsername = (String) params.get(ApiConstants.PING_CIFS_USERNAME);
            String pingCifsPassword = (String) params.get(ApiConstants.PING_CIFS_PASSWORD);
            PxeServerProfile profile = new PxeServerProfile(zoneId, podId, url, username, password, type, pingStorageServerIp, pingDir, tftpDir,
                    pingCifsUsername, pingCifsPassword);
            return _pxeMgr.addPxeServer(profile);
        } else {
            throw new CloudRuntimeException("Unsupported network device type:" + cmd.getDeviceType());
        }
    }

    @Override
    public NetworkDeviceResponse getApiResponse(Host device) {
        NetworkDeviceResponse response;
        HostVO host = (HostVO)device;
        _hostDao.loadDetails(host);
        if (host.getType() == Host.Type.ExternalDhcp) {
            NwDeviceDhcpResponse r = new NwDeviceDhcpResponse();
            r.setZoneId(host.getDataCenterId());
            r.setPodId(host.getPodId());
            r.setUrl(host.getPrivateIpAddress());
            r.setType(host.getDetail("type"));
            response = r;
        } else if (host.getType() == Host.Type.PxeServer) {
            String pxeType = host.getDetail("type");
            if (pxeType.equalsIgnoreCase(PxeServerType.PING.getName())) {
                PxePingResponse r = new PxePingResponse();
                DataCenter zone = ApiDBUtils.findZoneById(host.getDataCenterId());
                if (zone != null) {
                    r.setZoneId(zone.getUuid());
                }
                if (host.getPodId() != null) {
                    Pod pod = ApiDBUtils.findPodById(host.getPodId());
                    if (pod != null) {
                        r.setPodId(pod.getUuid());
                    }
                }
                r.setUrl(host.getPrivateIpAddress());
                r.setType(pxeType);
                r.setStorageServerIp(host.getDetail("storageServer"));
                r.setPingDir(host.getDetail("pingDir"));
                r.setTftpDir(host.getDetail("tftpDir"));
                response = r;
            } else {
                throw new CloudRuntimeException("Unsupported PXE server type:" + pxeType);
            }
        } else {
            throw new CloudRuntimeException("Unsupported network device type:" + host.getType());
        }

        response.setId(device.getUuid());
        return response;
    }

    private List<Host> listNetworkDevice(Long zoneId, Long physicalNetworkId, Long podId, Host.Type type) {
//        List<Host> res = new ArrayList<Host>();
//        if (podId != null) {
//            List<HostVO> devs = _hostDao.listBy(type, null, podId, zoneId);
//            if (devs.size() == 1) {
//                res.add(devs.get(0));
//            } else {
//                s_logger.debug("List " + type + ": " + devs.size() + " found");
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
        HashMap params = (HashMap) (paramsCollection.toArray())[0];
        if (NetworkDevice.ExternalDhcp.getName().equalsIgnoreCase(cmd.getDeviceType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.ExternalDhcp);
        } else if (NetworkDevice.PxeServer.getName().equalsIgnoreCase(cmd.getDeviceType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.PxeServer);
        } else if (cmd.getDeviceType() == null){
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            Long physicalNetworkId = (params.get(ApiConstants.PHYSICAL_NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.PHYSICAL_NETWORK_ID)):null;            
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
