/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.ApiConstants;
import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.AddNetworkDeviceCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteNetworkDeviceCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
import com.cloud.api.commands.ListNetworkDeviceCmd;
import com.cloud.baremetal.ExternalDhcpManager;
import com.cloud.baremetal.PxeServerManager;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.baremetal.PxeServerProfile;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkExternalDeviceDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.resource.F5BigIpResource;
import com.cloud.network.resource.JuniperSrxResource;
import com.cloud.network.resource.NetscalerResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StaticNatRuleImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ServerResource;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.server.api.response.NetworkDeviceResponse;
import com.cloud.server.api.response.NwDeviceDhcpResponse;
import com.cloud.server.api.response.PxePingResponse;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.Nic.State;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Local(value = {ExternalNetworkDeviceManager.class})
public class ExternalNetworkDeviceManagerImpl implements ExternalNetworkDeviceManager {
    public enum ExternalNetworkResourceName {
        JuniperSrx,
        F5BigIp,
        NetscalerMPX;
    }

    @Inject ExternalDhcpManager _dhcpMgr;
    @Inject PxeServerManager _pxeMgr;
    @Inject AgentManager _agentMgr;
    @Inject NetworkManager _networkMgr;
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
    @Inject HostDetailsDao _detailsDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject NicDao _nicDao;
    @Inject VpnUserDao _vpnUsersDao;
    @Inject InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject AccountManager _accountMgr;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject PhysicalNetworkExternalDeviceDao _physicalNetworkExternalDeviceDao;

    ScheduledExecutorService _executor;
    int _externalNetworkStatsInterval;
    
    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalNetworkDeviceManagerImpl.class);
    protected String _name;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _externalNetworkStatsInterval = NumbersUtil.parseInt(_configDao.getValue(Config.RouterStatsInterval.key()), 300);
        if (_externalNetworkStatsInterval > 0){
            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ExternalNetworkMonitor"));        
        }
        return true;
    }
    
    @Override
    public boolean start() {
        if (_externalNetworkStatsInterval > 0){
            _executor.scheduleAtFixedRate(new ExternalNetworkUsageTask(), _externalNetworkStatsInterval, _externalNetworkStatsInterval, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }
    
    @Override
    public Host addNetworkDevice(AddNetworkDeviceCmd cmd) {
        Map paramList = cmd.getParamList();
        if (paramList == null) {
            throw new CloudRuntimeException("Parameter list is null");
        }
    
        Collection paramsCollection = paramList.values();
        HashMap params = (HashMap) (paramsCollection.toArray())[0];
        if (cmd.getType().equalsIgnoreCase(NetworkDevice.ExternalDhcp.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            String type = (String) params.get(ApiConstants.DHCP_SERVER_TYPE);
            String url = (String) params.get(ApiConstants.URL);
            String username = (String) params.get(ApiConstants.USERNAME);
            String password = (String) params.get(ApiConstants.PASSWORD);

            return _dhcpMgr.addDhcpServer(zoneId, podId, type, url, username, password);
        } else if (cmd.getType().equalsIgnoreCase(NetworkDevice.PxeServer.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
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
        } else if (cmd.getType().equalsIgnoreCase(NetworkDevice.JuniperSRXFirewall.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return addExternalFirewall(zoneId, networkId, NetworkDevice.JuniperSRXFirewall.getName(), cmd.getParamList());
        } else if (cmd.getType().equalsIgnoreCase(NetworkDevice.NetscalerLoadBalancer.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return addExternalLoadBalancer(zoneId, networkId, NetworkDevice.NetscalerLoadBalancer.getName(), cmd.getParamList());
        } else if (cmd.getType().equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return addExternalLoadBalancer(zoneId, networkId, NetworkDevice.F5BigIpLoadBalancer.getName(), cmd.getParamList());
        } else {
            throw new CloudRuntimeException("Unsupported network device type:" + cmd.getType());
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
                r.setZoneId(host.getDataCenterId());
                r.setPodId(host.getPodId());
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
        
        response.setId(device.getId());
        return response;
    }

    private List<Host> listNetworkDevice(Long zoneId, Long physicalNetworkId, Long podId, Host.Type type) {
        List<Host> res = new ArrayList<Host>();
        if (podId != null) {
            List<HostVO> devs = _hostDao.listBy(type, null, podId, zoneId);
            if (devs.size() == 1) {
                res.add(devs.get(0));
            } else {
                s_logger.debug("List " + type + ": " + devs.size() + " found");
            }
        } else {
            List<HostVO> devs = _hostDao.listBy(type, zoneId);
            res.addAll(devs);
        }
        
        return res;
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
        if (NetworkDevice.ExternalDhcp.getName().equalsIgnoreCase(cmd.getType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.ExternalDhcp);
        } else if (NetworkDevice.PxeServer.getName().equalsIgnoreCase(cmd.getType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            res = listNetworkDevice(zoneId, null, podId, Host.Type.PxeServer);
        } else if (NetworkDevice.F5BigIpLoadBalancer.getName().equalsIgnoreCase(cmd.getType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return listExternalLoadBalancers(zoneId, networkId, NetworkDevice.F5BigIpLoadBalancer.getName());
        } else if (NetworkDevice.NetscalerLoadBalancer.getName().equalsIgnoreCase(cmd.getType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return listExternalLoadBalancers(zoneId, networkId, NetworkDevice.NetscalerLoadBalancer.getName());            
        } else if (NetworkDevice.JuniperSRXFirewall.getName().equalsIgnoreCase(cmd.getType())) {
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
            return listExternalFirewalls(zoneId, networkId, NetworkDevice.JuniperSRXFirewall.getName());
        } else if (cmd.getType() == null){
            Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
            Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
            Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;            
            List<Host> res1 = listNetworkDevice(zoneId, networkId, podId, Host.Type.PxeServer);
            List<Host> res2 = listNetworkDevice(zoneId, networkId, podId, Host.Type.ExternalDhcp);
            List<Host> res3 = listNetworkDevice(zoneId, networkId, podId, Host.Type.ExternalLoadBalancer);
            List<Host> res4 = listNetworkDevice(zoneId, networkId, podId, Host.Type.ExternalFirewall);
            List<Host> deviceAll = new ArrayList<Host>();
            deviceAll.addAll(res1);
            deviceAll.addAll(res2);
            deviceAll.addAll(res3);
            deviceAll.addAll(res4);
            res = deviceAll;
        } else {
            throw new CloudRuntimeException("Unknown network device type:" + cmd.getType());
        }
        
        return res;
    }

    @Override
    public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd) {
       HostVO device = _hostDao.findById(cmd.getId());
       if (device.getType() == Type.ExternalLoadBalancer) {
           return deleteExternalLoadBalancer(cmd.getId());
       } else if (device.getType() == Type.ExternalLoadBalancer) {
           return deleteExternalFirewall(cmd.getId());
       }
       return true;
    }    
    
    public String getExternalNetworkResourceGuid(long zoneId, ExternalNetworkResourceName name, String ip) {
        return zoneId + "-" + name + "-" + ip;
    }
    
    protected HostVO getExternalNetworkAppliance(long zoneId, long networkOfferingId, Host.Type type) {
/*        DataCenterVO zone = _dcDao.findById(zoneId);
        if (!_networkMgr.networkIsConfiguredForExternalNetworking(zoneId, networkOfferingId)) {
            s_logger.debug("Zone " + zone.getName() + " is not configured for external networking.");
            return null;
        } else {
            List<HostVO> externalNetworkAppliancesInZone = _hostDao.listBy(type, zoneId);
            if (externalNetworkAppliancesInZone.size() != 1) {
                return null;
            } else {
                return externalNetworkAppliancesInZone.get(0);
            }            
        }*/
        return null;
    }
    
    @Override
    @Deprecated  // should use more generic addNetworkDevice command to add external load balancer
    public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd) {
        Long zoneId = cmd.getZoneId();
        // AddExternalLoadBalancerCmd support only F5 Big IP (api exists for backward compatibility)
        String deviceType = NetworkDevice.F5BigIpLoadBalancer.getName();
        Map deviceParams = new HashMap<String, String>();
        deviceParams.put(ApiConstants.USERNAME, cmd.getUsername());
        deviceParams.put(ApiConstants.PASSWORD, cmd.getPassword());
        deviceParams.put(ApiConstants.URL, cmd.getUrl());
        return addExternalLoadBalancer(zoneId, null, deviceType, deviceParams);
    }

    public Host addExternalLoadBalancer(Long zoneId, Long physicalNetworkId, String deviceName, Map deviceParamList) {

        ServerResource resource =null;
        String guid;
        PhysicalNetworkVO pNetwork=null;
        DataCenterVO zone =null;
        NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        String url=null;
        String username=null;
        String password=null;
        
        if (deviceParamList != null) {
            url      = (String) deviceParamList.get(ApiConstants.URL);
            username = (String) deviceParamList.get(ApiConstants.USERNAME);
            password = (String) deviceParamList.get(ApiConstants.PASSWORD);
        }

        if (((zoneId == null) && (physicalNetworkId == null)) || (ntwkDevice == null) || (url == null) || (username == null) || (password == null) ) {
            throw new InvalidParameterValueException("Atleast one of the required parameters (url, username, password," +
                    " zone id/physical network id) is not specified or a valid parameter.");
        }

        if (physicalNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
            }
        }

        if (zoneId != null) {
            zone = _dcDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
            }
            if (pNetwork == null) {
                List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZone(zoneId);
                if ((physicalNetworks == null) || (physicalNetworks.size() > 1)) {
                    throw new InvalidParameterValueException("There are multiple physical networks configured in zone with ID: "
                            + zoneId + ". Physical network ID must be passed to select a physical network in this zonea.");
                }
                pNetwork = physicalNetworks.get(0);
            }
        }

        PhysicalNetworkServiceProviderVO ntwkSvcProider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProider.getState() != PhysicalNetworkServiceProvider.State.Enabled) { //TODO: check for other states: Shutdown?
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProider.getProviderName() + 
                    " is not in enabled state in the physical network: " + physicalNetworkId + "to add this device" );
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
   
        Map<String, String> params = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), true, params);
        String publicInterface = params.get("publicinterface");
        String privateInterface = params.get("privateinterface");
        String numRetries = params.get("numretries");
        boolean inline =  Boolean.parseBoolean(params.get("inline"));

        if (publicInterface == null) {
            throw new InvalidParameterValueException("Please specify a public interface.");
        }

        if (privateInterface == null) {
            throw new InvalidParameterValueException("Please specify a private interface.");
        }

        if (numRetries == null) {
            numRetries = "1";
        }

        if (deviceName.equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            resource = new F5BigIpResource();
            guid = getExternalNetworkResourceGuid(zoneId, ExternalNetworkResourceName.F5BigIp, ipAddress);
        } else if (deviceName.equalsIgnoreCase(NetworkDevice.NetscalerLoadBalancer.getName())) {
            resource = new NetscalerResource();
            guid = getExternalNetworkResourceGuid(zoneId, ExternalNetworkResourceName.NetscalerMPX, ipAddress);
        } else {
            throw new CloudRuntimeException("An unsupported networt device type is added as external load balancer.");
        }


        Map hostDetails = new HashMap<String, String>();
        hostDetails.put("zoneId", String.valueOf(zoneId));
        hostDetails.put("ip", ipAddress);
        hostDetails.put("username", username);
        hostDetails.put("password", password);
        hostDetails.put("publicInterface", publicInterface);
        hostDetails.put("privateInterface", privateInterface);
        hostDetails.put("numRetries", numRetries);
        hostDetails.put("guid", guid);
        hostDetails.put("name", guid);
        hostDetails.put("inline", String.valueOf(inline));

        try {
            resource.configure(guid, hostDetails);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        Host host = _agentMgr.addHost(zoneId, resource, Host.Type.ExternalLoadBalancer, hostDetails);
        if (host != null) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            PhysicalNetworkExternalDeviceVO device = new PhysicalNetworkExternalDeviceVO(ntwkSvcProider.getId(), host.getId());
            _physicalNetworkExternalDeviceDao.persist(device);
            txn.commit();
            return host;
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd) {
        return deleteExternalLoadBalancer(cmd.getId());
    }

    public boolean deleteExternalLoadBalancer(Long hostId) {
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        HostVO externalLoadBalancer = _hostDao.findById(hostId);
        if (externalLoadBalancer == null) {
            throw new InvalidParameterValueException("Could not find an external load balancer with ID: " + hostId);
        }

        try {
            if (_agentMgr.maintain(hostId) && _agentMgr.deleteHost(hostId, false, false, caller)) {
                // FIXME: device maintenance , rules reprogramming
                DataCenterVO zone = _dcDao.findById(externalLoadBalancer.getDataCenterId());
                return _dcDao.update(zone.getId(), zone);
            } else {
                return false;
            }
        } catch (AgentUnavailableException e) {
            s_logger.debug(e);
            return false;
        }
    }

    @Override
    @Deprecated  // should use more generic listNetworkDevice command
    public List<Host> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd) {
        List<Host> lbHosts = new ArrayList<Host>();
        lbHosts.addAll(listExternalLoadBalancers(cmd.getZoneId(), null, NetworkDevice.F5BigIpLoadBalancer.getName()));
        return lbHosts;    
    }
    
    public List<Host> listExternalLoadBalancers(Long zoneId, Long physicalNetworkId, String deviceName) {
        List<Host> lbHosts = new ArrayList<Host>();
        List<Host> lbHostsInZone = new ArrayList<Host>();
        NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        PhysicalNetworkVO pNetwork=null;

        if (((zoneId == null) && (physicalNetworkId == null)) || (ntwkDevice == null)) {
            throw new InvalidParameterValueException("Atleast one of ther required parameter zone Id, physical networkId, device name is missing or invalid.");
        }

        if (physicalNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
            }
        }

        if (zoneId != null) {
            if (_dcDao.findById(zoneId) == null) {
                throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
            }
            lbHostsInZone.addAll(_hostDao.listByTypeDataCenter(Host.Type.ExternalLoadBalancer, zoneId));
        }
        if (physicalNetworkId == null) {
            return lbHostsInZone;
        }
        PhysicalNetworkServiceProviderVO ntwkSvcProider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProider == null) {
            return null; 
        }
        List<PhysicalNetworkExternalDeviceVO> providerInstances = _physicalNetworkExternalDeviceDao.listByNetworkServiceProviderId(ntwkSvcProider.getId());
        for (PhysicalNetworkExternalDeviceVO provderInstance : providerInstances) {
            lbHosts.add(_hostDao.findById(provderInstance.getHostId()));
        }
        return lbHosts;    
    }

    @Override
    public ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLoadBalancer) {
        Map<String, String> lbDetails = _detailsDao.findDetails(externalLoadBalancer.getId());
        ExternalLoadBalancerResponse response = new ExternalLoadBalancerResponse();
        response.setId(externalLoadBalancer.getId());
        response.setIpAddress(externalLoadBalancer.getPrivateIpAddress());
        response.setUsername(lbDetails.get("username"));
        response.setPublicInterface(lbDetails.get("publicInterface"));
        response.setPrivateInterface(lbDetails.get("privateInterface"));
        response.setNumRetries(lbDetails.get("numRetries"));
        return response;
    }

    @Override
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException {
        if (guestConfig.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("External load balancer can only be user for add/remove guest networks.");
            return false;
        }

        // Find the external load balancer in this zone
        long zoneId = guestConfig.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = getExternalNetworkAppliance(zoneId, guestConfig.getNetworkOfferingId(), Host.Type.ExternalLoadBalancer);

        if (externalLoadBalancer == null) {
            return false;
        }

        // Send a command to the external load balancer to implement or shutdown the guest network
        long guestVlanTag = Long.parseLong(guestConfig.getBroadcastUri().getHost());
        String selfIp = NetUtils.long2Ip(NetUtils.ip2Long(guestConfig.getGateway()) + 1);
        String guestVlanNetmask = NetUtils.cidr2Netmask(guestConfig.getCidr());
        Integer networkRate = _networkMgr.getNetworkRate(guestConfig.getId(), null);

        IpAddressTO ip = new IpAddressTO(guestConfig.getAccountId(), null, add, false, true, String.valueOf(guestVlanTag), selfIp, guestVlanNetmask, null, null, networkRate, false);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String action = add ? "implement" : "shutdown";
            String answerDetails = (answer != null) ? answer.getDetails() : "answer was null";
            String msg = "External load balancer was unable to " + action + " the guest network on the external load balancer in zone " + zone.getName() + " due to " + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        List<String> reservedIpAddressesForGuestNetwork = _nicDao.listIpAddressInNetwork(guestConfig.getId());
        if (add && (!reservedIpAddressesForGuestNetwork.contains(selfIp))) {
            // Insert a new NIC for this guest network to reserve the self IP
            savePlaceholderNic(guestConfig, selfIp);
        }

        Account account = _accountDao.findByIdIncludingRemoved(guestConfig.getAccountId());
        String action = add ? "implemented" : "shut down";
        s_logger.debug("External load balancer has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() + ") with VLAN tag " + guestVlanTag);

        return true;
    }

    @Override 
    public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        // Find the external load balancer in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = getExternalNetworkAppliance(zoneId, network.getNetworkOfferingId(), Host.Type.ExternalLoadBalancer);

        if (externalLoadBalancer == null) {
            return false;
        }
        
        // If the load balancer is inline, find the external firewall in this zone
        boolean externalLoadBalancerIsInline = externalLoadBalancerIsInline(externalLoadBalancer);
        HostVO externalFirewall = null;
        if (externalLoadBalancerIsInline) {
            externalFirewall = getExternalNetworkAppliance(zoneId, network.getNetworkOfferingId(), Host.Type.ExternalFirewall);
            if (externalFirewall == null) {
                String msg = "External load balancer in zone " + zone.getName() + " is inline, but no external firewall in this zone.";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External load balancer was asked to apply LB rules for network with ID " + network.getId() + "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        List<LoadBalancingRule> loadBalancingRules = new ArrayList<LoadBalancingRule>();

        for (FirewallRule rule : rules) {
            if (rule.getPurpose().equals(Purpose.LoadBalancing)) {
                loadBalancingRules.add((LoadBalancingRule) rule);
            }
        }

        List<LoadBalancerTO> loadBalancersToApply = new ArrayList<LoadBalancerTO>();
        for (int i = 0; i < loadBalancingRules.size(); i++) {
            LoadBalancingRule rule = loadBalancingRules.get(i);

            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String srcIp = _networkMgr.getIp(rule.getSourceIpAddressId()).getAddress().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            List<String> sourceCidrs = rule.getSourceCidrList();

            if (externalLoadBalancerIsInline) {
                InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(srcIp);
                NicVO loadBalancingIpNic = null;
                if (!revoked) {
                    if (mapping == null) {
                        // Acquire a new guest IP address and save it as the load balancing IP address
                        String loadBalancingIpAddress = _networkMgr.acquireGuestIpAddress(network, null);
                        
                        if (loadBalancingIpAddress == null) {
                            String msg = "Ran out of guest IP addresses.";
                            s_logger.error(msg);
                            throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
                        }
                        
                        // If a NIC doesn't exist for the load balancing IP address, create one
                        loadBalancingIpNic = _nicDao.findByIp4Address(loadBalancingIpAddress);
                        if (loadBalancingIpNic == null) {
                            loadBalancingIpNic = savePlaceholderNic(network, loadBalancingIpAddress); 
                        }

                        // Save a mapping between the source IP address and the load balancing IP address NIC
                        mapping = new InlineLoadBalancerNicMapVO(rule.getId(), srcIp, loadBalancingIpNic.getId());
                        _inlineLoadBalancerNicMapDao.persist(mapping);
                        
                        // On the external firewall, create a static NAT rule between the source IP address and the load balancing IP address                  
                        applyStaticNatRuleForInlineLBRule(zone, network, externalFirewall, revoked, srcIp, loadBalancingIpNic.getIp4Address());
                    } else {
                        loadBalancingIpNic = _nicDao.findById(mapping.getNicId());
                    }
                } else {
                    if (mapping != null) {
                        // Find the NIC that the mapping refers to
                        loadBalancingIpNic = _nicDao.findById(mapping.getNicId());
                        
                        // On the external firewall, delete the static NAT rule between the source IP address and the load balancing IP address
                        applyStaticNatRuleForInlineLBRule(zone, network, externalFirewall, revoked, srcIp, loadBalancingIpNic.getIp4Address());
                        
                        // Delete the mapping between the source IP address and the load balancing IP address
                        _inlineLoadBalancerNicMapDao.expunge(mapping.getId());
                        
                        // Delete the NIC
                        _nicDao.expunge(loadBalancingIpNic.getId());
                    } else {
                        s_logger.debug("Revoking a rule for an inline load balancer that has not been programmed yet.");
                        continue;
                    }
                }
                
                // Change the source IP address for the load balancing rule to be the load balancing IP address
                srcIp = loadBalancingIpNic.getIp4Address();
            }
            
            if (destinations != null && !destinations.isEmpty()) {
                LoadBalancerTO loadBalancer = new LoadBalancerTO(srcIp, srcPort, protocol, algorithm, revoked, false, destinations);
                loadBalancersToApply.add(loadBalancer);
            }
        }

        if (loadBalancersToApply.size() > 0) {
            int numLoadBalancersForCommand = loadBalancersToApply.size(); 
            LoadBalancerTO[] loadBalancersForCommand = loadBalancersToApply.toArray(new LoadBalancerTO[numLoadBalancersForCommand]);
            LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(loadBalancersForCommand);
            long guestVlanTag = Integer.parseInt(network.getBroadcastUri().getHost());
            cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));
            Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "Unable to apply load balancer rules to the external load balancer appliance in zone " + zone.getName() + " due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }
    
    @Override
    @Deprecated // should use more generic addNetworkDevice command to add firewall
    public Host addExternalFirewall(AddExternalFirewallCmd cmd) {
        Long zoneId = cmd.getZoneId();
        // AddExternalFirewallCmd support only Juniper SRX (api exists for backward compatibility)
        String deviceType = NetworkDevice.JuniperSRXFirewall.getName();
        Map deviceParams = new HashMap<String, String>();
        deviceParams.put(ApiConstants.USERNAME, cmd.getUsername());
        deviceParams.put(ApiConstants.URL, cmd.getUrl());
        deviceParams.put(ApiConstants.PASSWORD, cmd.getPassword());
        return addExternalFirewall(zoneId, null, deviceType, deviceParams);
    }
    
    public Host addExternalFirewall(Long zoneId, Long physicalNetworkId, String deviceName, Map deviceParamList) {

        PhysicalNetworkVO pNetwork=null;
        DataCenterVO zone =null;
        NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        String url=null;
        String username=null;
        String password=null;
        
        if (deviceParamList != null) {
            url      = (String) deviceParamList.get(ApiConstants.URL);
            username = (String) deviceParamList.get(ApiConstants.USERNAME);
            password = (String) deviceParamList.get(ApiConstants.PASSWORD);
        }

        if (((zoneId == null) && (physicalNetworkId == null)) || (ntwkDevice == null) || (url == null) || (username == null) || (password == null) ) {
            throw new InvalidParameterValueException("Atleast one of the required parameters (url, username, password," +
                    " zone id/physical network id) is not specified or a valid parameter.");
        }

        if (physicalNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
            }
        }

        if (zoneId != null) {
            zone = _dcDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
            }
            if (pNetwork == null) {
                List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZone(zoneId);
                if ((physicalNetworks == null) || (physicalNetworks.size() > 1)) {
                    throw new InvalidParameterValueException("There are multiple physical networks configured in zone with ID: "
                            + zoneId + ". Physical network ID must be passed to select a physical network in this zone.");
                }
                pNetwork = physicalNetworks.get(0);
            }
        }

        PhysicalNetworkServiceProviderVO ntwkSvcProider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProider.getState() != PhysicalNetworkServiceProvider.State.Enabled) { //TODO: check for other states: Shutdown?
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProider.getProviderName() + 
                    " is not in enabled state in the physical network: " + physicalNetworkId + "to add this device" );
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
        Map<String, String> params = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), true, params);
        String publicInterface = params.get("publicinterface");
        String usageInterface = params.get("usageinterface");
        String privateInterface = params.get("privateinterface");
        String publicZone = params.get("publiczone");
        String privateZone = params.get("privatezone");
        String numRetries = params.get("numretries");
        String timeout = params.get("timeout");
        ServerResource resource;
        String guid;

        if (publicInterface == null) {
            throw new InvalidParameterValueException("Please specify a public interface.");
        }

        if (usageInterface != null) {
            if (!usageInterface.contains(".")) {
                usageInterface += ".0";
            }
        }

        if (privateInterface != null) {
            if (privateInterface.contains(".")) {
                throw new InvalidParameterValueException("The private interface name must not have a unit identifier.");
            }
        } else {
            throw new InvalidParameterValueException("Please specify a private interface.");
        }

        if (publicZone == null) {
            publicZone = "untrust";
        }

        if (privateZone == null) {
            privateZone = "trust";
        }

        if (numRetries == null) {
            numRetries = "1";
        }

        if (timeout == null) {
            timeout = "300";
        }

        if (deviceName ==null) {
        	deviceName = NetworkDevice.JuniperSRXFirewall.getName(); //default it to Juniper for now
        }
        if (deviceName.equalsIgnoreCase(NetworkDevice.JuniperSRXFirewall.getName())) {
            resource = new JuniperSrxResource();
            guid = getExternalNetworkResourceGuid(zoneId, ExternalNetworkResourceName.JuniperSrx, ipAddress);            
        } else {
            throw new CloudRuntimeException("An unsupported networt device type is added as external firewall.");
        }
        
        Map hostDetails = new HashMap<String, String>();
        hostDetails.put("zoneId", String.valueOf(zoneId));
        hostDetails.put("ip", ipAddress);
        hostDetails.put("username", username);
        hostDetails.put("password", password);
        hostDetails.put("publicInterface", publicInterface);
        hostDetails.put("privateInterface", privateInterface);
        hostDetails.put("publicZone", publicZone);
        hostDetails.put("privateZone", privateZone);
        hostDetails.put("numRetries", numRetries);
        hostDetails.put("timeout", timeout);
        hostDetails.put("guid", guid);
        hostDetails.put("name", guid);

        if (usageInterface != null) {
            hostDetails.put("usageInterface", usageInterface);
        }

        try {
            resource.configure(guid, hostDetails);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        Host externalFirewall = _agentMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, hostDetails);
        if (externalFirewall != null) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            _dcDao.update(zone.getId(), zone);
            PhysicalNetworkExternalDeviceVO device = new PhysicalNetworkExternalDeviceVO(ntwkSvcProider.getId(), externalFirewall.getId());
            _physicalNetworkExternalDeviceDao.persist(device);
            txn.commit();
            return externalFirewall;
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd) {
        return deleteExternalFirewall(cmd.getId());
    }
    
    public boolean deleteExternalFirewall(Long hostId) {
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        HostVO externalFirewall = _hostDao.findById(hostId);
        if (externalFirewall == null) {
            throw new InvalidParameterValueException("Could not find an external firewall with ID: " + hostId);
        }

        try {
            // FIXME: device maintenance , rules reprogramming
            if (_agentMgr.maintain(hostId) && _agentMgr.deleteHost(hostId, false, false, caller)) {
                return true;
            } else {
                return false;
            }
        } catch (AgentUnavailableException e) {
            s_logger.debug(e);
            return false;
        }
    }

    @Override
    @Deprecated  // should use more generic listNetworkDevice command    
    public List<Host> listExternalFirewalls(ListExternalFirewallsCmd cmd) {
        List<Host> firewallHosts = new ArrayList<Host>();
        firewallHosts.addAll(listExternalFirewalls(cmd.getZoneId(), null, NetworkDevice.JuniperSRXFirewall.getName()));
        return firewallHosts;
    }

    public List<Host> listExternalFirewalls(Long zoneId, Long physicalNetworkId, String deviceName) {
        List<Host> firewallHosts = new ArrayList<Host>();
        List<Host> firewallhostsInZone = new ArrayList<Host>();
        NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        PhysicalNetworkVO pNetwork=null;

        if (((zoneId == null) && (physicalNetworkId == null)) || (ntwkDevice == null)) {
            throw new InvalidParameterValueException("Atleast one of ther required parameter zone Id, physical networkId, device name is missing or invalid.");
        }

        if (physicalNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
            }
        }

        if (zoneId != null) {
            if (_dcDao.findById(zoneId) == null) {
                throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
            }
            firewallhostsInZone.addAll(_hostDao.listByTypeDataCenter(Host.Type.ExternalFirewall, zoneId));
        }
        if (physicalNetworkId == null) {
            return firewallhostsInZone;
        }
        PhysicalNetworkServiceProviderVO ntwkSvcProider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProider == null) {
            return null; 
        }
        List<PhysicalNetworkExternalDeviceVO> providerInstances = _physicalNetworkExternalDeviceDao.listByNetworkServiceProviderId(ntwkSvcProider.getId());
        for (PhysicalNetworkExternalDeviceVO provderInstance : providerInstances) {
            firewallHosts.add(_hostDao.findById(provderInstance.getHostId()));
        }
        return firewallHosts;
    }
    
    @Override
    public ExternalFirewallResponse createExternalFirewallResponse(Host externalFirewall) {
        Map<String, String> fwDetails = _detailsDao.findDetails(externalFirewall.getId());
        ExternalFirewallResponse response = new ExternalFirewallResponse();
        response.setId(externalFirewall.getId());
        response.setIpAddress(externalFirewall.getPrivateIpAddress());
        response.setUsername(fwDetails.get("username"));
        response.setPublicInterface(fwDetails.get("publicInterface"));
        response.setUsageInterface(fwDetails.get("usageInterface"));
        response.setPrivateInterface(fwDetails.get("privateInterface"));
        response.setPublicZone(fwDetails.get("publicZone"));
        response.setPrivateZone(fwDetails.get("privateZone"));
        response.setNumRetries(fwDetails.get("numRetries"));
        response.setTimeout(fwDetails.get("timeout"));
        return response;
    }

    @Override
    public boolean manageGuestNetworkWithExternalFirewall(boolean add, Network network, NetworkOffering offering) throws ResourceUnavailableException {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("External firewall can only be used for add/remove guest networks.");
            return false;
        }

        // Find the external firewall in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalFirewall = getExternalNetworkAppliance(zoneId, network.getNetworkOfferingId(), Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }

        Account account = _accountDao.findByIdIncludingRemoved(network.getAccountId());
        
        boolean sharedSourceNat = false;
        Map<Network.Capability, String> sourceNatCapabilities = _networkMgr.getServiceCapabilities(network.getNetworkOfferingId(), Service.SourceNat);
        if (sourceNatCapabilities != null) {
            String supportedSourceNatTypes = sourceNatCapabilities.get(Capability.SupportedSourceNatTypes).toLowerCase();
            if (supportedSourceNatTypes.contains("zone")) {
                sharedSourceNat = true;
            }
        }
        
        IPAddressVO sourceNatIp = null;
        if (!sharedSourceNat) {
            // Get the source NAT IP address for this network          
            List<IPAddressVO> sourceNatIps = _networkMgr.listPublicIpAddressesInVirtualNetwork(network.getAccountId(), zoneId, true, null);

            if (sourceNatIps.size() != 1) {
                String errorMsg = "External firewall was unable to find the source NAT IP address for account " + account.getAccountName();
                s_logger.error(errorMsg);
                return true;
            } else {
                sourceNatIp = sourceNatIps.get(0);
            }
        }

        // Send a command to the external firewall to implement or shutdown the guest network
        long guestVlanTag = Long.parseLong(network.getBroadcastUri().getHost());
        String guestVlanGateway = network.getGateway();
        String guestVlanCidr = network.getCidr();
        String sourceNatIpAddress = sourceNatIp.getAddress().addr();
        
        VlanVO publicVlan = _vlanDao.findById(sourceNatIp.getVlanId());
        String publicVlanTag = publicVlan.getVlanTag();

        // Get network rate
        Integer networkRate = _networkMgr.getNetworkRate(network.getId(), null);

        IpAddressTO ip = new IpAddressTO(account.getAccountId(), sourceNatIpAddress, add, false, !sharedSourceNat, publicVlanTag, null, null, null, null, networkRate, sourceNatIp.isOneToOneNat());
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, guestVlanGateway);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, guestVlanCidr);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));
        Answer answer = _agentMgr.easySend(externalFirewall.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String action = add ? "implement" : "shutdown";
            String answerDetails = (answer != null) ? answer.getDetails() : "answer was null";
            String msg = "External firewall was unable to " + action + " the guest network on the external firewall in zone " + zone.getName() + " due to " + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        List<String> reservedIpAddressesForGuestNetwork = _nicDao.listIpAddressInNetwork(network.getId());
        if (add && (!reservedIpAddressesForGuestNetwork.contains(network.getGateway()))) {
            // Insert a new NIC for this guest network to reserve the gateway address
            savePlaceholderNic(network,  network.getGateway());
        }
        
        // Delete any mappings used for inline external load balancers in this network
        List<NicVO> nicsInNetwork = _nicDao.listByNetworkId(network.getId());
        for (NicVO nic : nicsInNetwork) {
            InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByNicId(nic.getId());
            
            if (mapping != null) {
                _nicDao.expunge(mapping.getNicId());
                _inlineLoadBalancerNicMapDao.expunge(mapping.getId());
                
            }
        }
        

        String action = add ? "implemented" : "shut down";
        s_logger.debug("External firewall has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() + ") with VLAN tag " + guestVlanTag);

        return true;
    }

    @Override
    public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        // Find the external firewall in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalFirewall = getExternalNetworkAppliance(zoneId, network.getNetworkOfferingId(), Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External firewall was asked to apply firewall rules for network with ID " + network.getId() + "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        List<StaticNatRuleTO> staticNatRules = new ArrayList<StaticNatRuleTO>();
        List<PortForwardingRuleTO> portForwardingRules = new ArrayList<PortForwardingRuleTO>();

        for (FirewallRule rule : rules) {
            IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
            Vlan vlan = _vlanDao.findById(sourceIp.getVlanId());

            if (rule.getPurpose() == Purpose.StaticNat) {
                StaticNatRule staticNatRule = (StaticNatRule) rule;
                StaticNatRuleTO ruleTO = new StaticNatRuleTO(staticNatRule, vlan.getVlanTag(), sourceIp.getAddress().addr(), staticNatRule.getDestIpAddress());
                staticNatRules.add(ruleTO);
            } else if (rule.getPurpose() == Purpose.PortForwarding) {
                PortForwardingRuleTO ruleTO = new PortForwardingRuleTO((PortForwardingRule) rule, vlan.getVlanTag(), sourceIp.getAddress().addr());
                portForwardingRules.add(ruleTO);
            }
        }

        // Apply static nat rules
        applyStaticNatRules(staticNatRules, zone, externalFirewall.getId());

        // apply port forwarding rules
        applyPortForwardingRules(portForwardingRules, zone, externalFirewall.getId());

        return true;
    }

    protected void applyStaticNatRules(List<StaticNatRuleTO> staticNatRules, DataCenter zone, long externalFirewallId) throws ResourceUnavailableException {
        if (!staticNatRules.isEmpty()) {
            SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(staticNatRules);
            Answer answer = _agentMgr.easySend(externalFirewallId, cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "External firewall was unable to apply static nat rules to the SRX appliance in zone " + zone.getName() + " due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, zone.getId());
            }
        }
    }

    protected void applyPortForwardingRules(List<PortForwardingRuleTO> portForwardingRules, DataCenter zone, long externalFirewallId) throws ResourceUnavailableException {
        if (!portForwardingRules.isEmpty()) {
            SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(portForwardingRules);
            Answer answer = _agentMgr.easySend(externalFirewallId, cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "External firewall was unable to apply port forwarding rules to the SRX appliance in zone " + zone.getName() + " due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, zone.getId());
            }
        }
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddresses) throws ResourceUnavailableException {
        return true;
    }
    
    
    public boolean manageRemoteAccessVpn(boolean create, Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {
        HostVO externalFirewall = getExternalNetworkAppliance(network.getDataCenterId(), network.getNetworkOfferingId(), Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }
        
        // Create/delete VPN
        IpAddress ip = _networkMgr.getIp(vpn.getServerAddressId());
        
        // Mask the IP range with the network's VLAN tag
        String[] ipRange = vpn.getIpRange().split("-");
        DataCenterVO zone = _dcDao.findById(network.getDataCenterId());
        int vlanTag = Integer.parseInt(network.getBroadcastUri().getHost());
        int offset = getVlanOffset(network.getPhysicalNetworkId(), vlanTag);
        int cidrSize = getGloballyConfiguredCidrSize();
        
        for (int i = 0; i < 2; i++) {
            ipRange[i] = NetUtils.long2Ip((NetUtils.ip2Long(ipRange[i]) & 0xff000000) | (offset << (32 - cidrSize)));
        }
        
        String maskedIpRange = ipRange[0] + "-" + ipRange[1];
        
        RemoteAccessVpnCfgCommand createVpnCmd = new RemoteAccessVpnCfgCommand(create, ip.getAddress().addr(), vpn.getLocalIp(), maskedIpRange, vpn.getIpsecPresharedKey());
        createVpnCmd.setAccessDetail(NetworkElementCommand.ACCOUNT_ID, String.valueOf(network.getAccountId()));
        createVpnCmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr()); 
        Answer answer = _agentMgr.easySend(externalFirewall.getId(), createVpnCmd);
        if (answer == null || !answer.getResult()) {
             String details = (answer != null) ? answer.getDetails() : "details unavailable";
             String msg = "External firewall was unable to create a remote access VPN in zone " + zone.getName() + " due to: " + details + ".";
             s_logger.error(msg);
             throw new ResourceUnavailableException(msg, DataCenter.class, zone.getId());
        }
        
        // Add/delete users
        List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());
        return manageRemoteAccessVpnUsers(network, vpn, vpnUsers);
    }  
    
    public boolean manageRemoteAccessVpnUsers(Network network, RemoteAccessVpn vpn, List<? extends VpnUser> vpnUsers) throws ResourceUnavailableException {
        HostVO externalFirewall = getExternalNetworkAppliance(network.getDataCenterId(), network.getNetworkOfferingId(), Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }
        
        List<VpnUser> addUsers = new ArrayList<VpnUser>();
        List<VpnUser> removeUsers = new ArrayList<VpnUser>();
        for (VpnUser user : vpnUsers) {
            if (user.getState() == VpnUser.State.Add ||
                user.getState() == VpnUser.State.Active) {
                addUsers.add(user);
            } else if (user.getState() == VpnUser.State.Revoke) {
                removeUsers.add(user);
            }
        }
        
        VpnUsersCfgCommand addUsersCmd = new VpnUsersCfgCommand(addUsers, removeUsers);
        addUsersCmd.setAccessDetail(NetworkElementCommand.ACCOUNT_ID, String.valueOf(network.getAccountId()));
        addUsersCmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr());        
        
        Answer answer = _agentMgr.easySend(externalFirewall.getId(), addUsersCmd);
        if (answer == null || !answer.getResult()) {
             String details = (answer != null) ? answer.getDetails() : "details unavailable";
             DataCenterVO zone = _dcDao.findById(network.getDataCenterId());
             String msg = "External firewall was unable to add remote access users in zone " + zone.getName() + " due to: " + details + ".";
             s_logger.error(msg);
             throw new ResourceUnavailableException(msg, DataCenter.class, zone.getId());
        }
        
        return true;
    }
    
    private void applyStaticNatRuleForInlineLBRule(DataCenterVO zone, Network network, HostVO externalFirewall, boolean revoked, String publicIp, String privateIp) throws ResourceUnavailableException {
        List<StaticNatRuleTO> staticNatRules = new ArrayList<StaticNatRuleTO>();
        IPAddressVO ipVO = _ipAddressDao.listByDcIdIpAddress(zone.getId(), publicIp).get(0);
        VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
        FirewallRuleVO fwRule = new FirewallRuleVO(null, ipVO.getId(), -1, -1, "any", network.getId(), network.getAccountId(), network.getDomainId(), Purpose.StaticNat, null, null, null, null);
        FirewallRule.State state = !revoked ? FirewallRule.State.Add : FirewallRule.State.Revoke;
        fwRule.setState(state);
        StaticNatRule rule = new StaticNatRuleImpl(fwRule, privateIp);
        StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, vlan.getVlanTag(), publicIp, privateIp);
        staticNatRules.add(ruleTO);
        
        applyStaticNatRules(staticNatRules, zone, externalFirewall.getId());
    }
    
    private boolean externalLoadBalancerIsInline(HostVO externalLoadBalancer) {
        DetailVO detail = _detailsDao.findDetail(externalLoadBalancer.getId(), "inline");
        return (detail != null && detail.getValue().equals("true"));
    }
    
    public int getVlanOffset(long physicalNetworkId, int vlanTag) {
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throw new CloudRuntimeException("Could not find the physical Network " + physicalNetworkId + ".");
        }

        if (pNetwork.getVnet() == null) {
            throw new CloudRuntimeException("Could not find vlan range for physical Network " + physicalNetworkId + ".");
        }
        String vlanRange[] = pNetwork.getVnet().split("-");
        int lowestVlanTag = Integer.valueOf(vlanRange[0]);
        return vlanTag - lowestVlanTag;
    }
    
    private NicVO savePlaceholderNic(Network network, String ipAddress) {
        NicVO nic = new NicVO(null, null, network.getId(), null);
        nic.setIp4Address(ipAddress);
        nic.setReservationStrategy(ReservationStrategy.PlaceHolder);
        nic.setState(State.Reserved);
        return _nicDao.persist(nic);
    }
    
    public int getGloballyConfiguredCidrSize() {
        try {
            String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
            return 8 + Integer.parseInt(globalVlanBits);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to read the globally configured VLAN bits size.");
        }
    }
    
    protected class ExternalNetworkUsageTask implements Runnable {                
        
        public ExternalNetworkUsageTask() {        
        }
        
        private boolean updateBytes(UserStatisticsVO userStats, long newCurrentBytesSent, long newCurrentBytesReceived) {            
            long oldNetBytesSent = userStats.getNetBytesSent();
            long oldNetBytesReceived = userStats.getNetBytesReceived();
            long oldCurrentBytesSent = userStats.getCurrentBytesSent();
            long oldCurrentBytesReceived = userStats.getCurrentBytesReceived();
            String warning = "Received an external network stats byte count that was less than the stored value. Zone ID: " + userStats.getDataCenterId() + ", account ID: " + userStats.getAccountId() + ".";
                        
            userStats.setCurrentBytesSent(newCurrentBytesSent);
            if (oldCurrentBytesSent > newCurrentBytesSent) {
                s_logger.warn(warning + "Stored bytes sent: " + oldCurrentBytesSent + ", new bytes sent: " + newCurrentBytesSent + ".");            
                userStats.setNetBytesSent(oldNetBytesSent + oldCurrentBytesSent);
            } 
            
            userStats.setCurrentBytesReceived(newCurrentBytesReceived);
            if (oldCurrentBytesReceived > newCurrentBytesReceived) {
                s_logger.warn(warning + "Stored bytes received: " + oldCurrentBytesReceived + ", new bytes received: " + newCurrentBytesReceived + ".");                        
                userStats.setNetBytesReceived(oldNetBytesReceived + oldCurrentBytesReceived);
            } 
                    
            return _userStatsDao.update(userStats.getId(), userStats);
        }
        
        /*
         * Creates a new stats entry for the specified parameters, if one doesn't already exist.
         */
        private boolean createStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId) {
            HostVO host = _hostDao.findById(hostId);
            UserStatisticsVO userStats = _userStatsDao.findBy(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
            if (userStats == null) {
                return (_userStatsDao.persist(new UserStatisticsVO(accountId, zoneId, publicIp, hostId, host.getType().toString(), networkId)) != null);
            } else {
                return true;
            }
        }
        
        /*
         * Updates an existing stats entry with new data from the specified usage answer.
         */
        private boolean updateStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId, ExternalNetworkResourceUsageAnswer answer) {
            AccountVO account = _accountDao.findById(accountId);
            DataCenterVO zone = _dcDao.findById(zoneId);
            NetworkVO network = _networkDao.findById(networkId);
            HostVO host = _hostDao.findById(hostId);
            String statsEntryIdentifier = "account " + account.getAccountName() + ", zone " + zone.getName() + ", network ID " + networkId + ", host ID " + host.getName();            
            
            long newCurrentBytesSent = 0;
            long newCurrentBytesReceived = 0;
            
            if (publicIp != null) {
                long[] bytesSentAndReceived = null;
                statsEntryIdentifier += ", public IP: " + publicIp;
                
                if (host.getType().equals(Host.Type.ExternalLoadBalancer) && externalLoadBalancerIsInline(host)) {
                    // Look up stats for the guest IP address that's mapped to the public IP address
                    InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(publicIp);
                    
                    if (mapping != null) {
                        NicVO nic = _nicDao.findById(mapping.getNicId());
                        String loadBalancingIpAddress = nic.getIp4Address();
                        bytesSentAndReceived = answer.ipBytes.get(loadBalancingIpAddress);
                        
                        if (bytesSentAndReceived != null) {
                            bytesSentAndReceived[0] = 0;
                        }
                    }
                } else {
                    bytesSentAndReceived = answer.ipBytes.get(publicIp);
                }
                
                if (bytesSentAndReceived == null) {
                    s_logger.debug("Didn't get an external network usage answer for public IP " + publicIp);
                } else {
                    newCurrentBytesSent += bytesSentAndReceived[0];
                    newCurrentBytesReceived += bytesSentAndReceived[1];
                }
            } else {
                URI broadcastURI = network.getBroadcastUri();
                if (broadcastURI == null) {
                    s_logger.debug("Not updating stats for guest network with ID " + network.getId() + " because the network is not implemented.");
                    return true;
                } else {
                    long vlanTag = Integer.parseInt(broadcastURI.getHost());
                    long[] bytesSentAndReceived = answer.guestVlanBytes.get(String.valueOf(vlanTag));                                   
                    
                    if (bytesSentAndReceived == null) {
                        s_logger.warn("Didn't get an external network usage answer for guest VLAN " + vlanTag);                      
                    } else {
                        newCurrentBytesSent += bytesSentAndReceived[0];
                        newCurrentBytesReceived += bytesSentAndReceived[1];
                    }
                }
            }
            
            UserStatisticsVO userStats;
            try {
                userStats = _userStatsDao.lock(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
            } catch (Exception e) {
                s_logger.warn("Unable to find user stats entry for " + statsEntryIdentifier);
                return false;
            }
                              
            if (updateBytes(userStats, newCurrentBytesSent, newCurrentBytesReceived)) {
                s_logger.debug("Successfully updated stats for " + statsEntryIdentifier);
                return true;
            } else {
                s_logger.debug("Failed to update stats for " + statsEntryIdentifier);
                return false;
            }
        }                
        
        private boolean createOrUpdateStatsEntry(boolean create, long accountId, long zoneId, long networkId, String publicIp, long hostId, ExternalNetworkResourceUsageAnswer answer) {
            if (create) {
                return createStatsEntry(accountId, zoneId, networkId, publicIp, hostId);
            } else {
                return updateStatsEntry(accountId, zoneId, networkId, publicIp, hostId, answer);
            }        
        }
        
        /*
         * Creates/updates all necessary stats entries for an account and zone.
         * Stats entries are created for source NAT IP addresses, static NAT rules, port forwarding rules, and load balancing rules
         */
        private boolean manageStatsEntries(boolean create, long accountId, long zoneId, 
                                           HostVO externalFirewall, ExternalNetworkResourceUsageAnswer firewallAnswer,
                                           HostVO externalLoadBalancer, ExternalNetworkResourceUsageAnswer lbAnswer) {
            String accountErrorMsg = "Failed to update external network stats entry. Details: account ID = " + accountId;
            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
            try {
                txn.start();
                
                List<NetworkVO> networksForAccount = _networkDao.listBy(accountId, zoneId, Network.Type.Isolated);
                
                for (NetworkVO network : networksForAccount) {
                    String networkErrorMsg = accountErrorMsg + ", network ID = " + network.getId();                
                    
                    boolean sharedSourceNat = false;
                    Map<Network.Capability, String> sourceNatCapabilities = _networkMgr.getServiceCapabilities(network.getNetworkOfferingId(), Service.SourceNat);
                    if (sourceNatCapabilities != null) {
                        String supportedSourceNatTypes = sourceNatCapabilities.get(Capability.SupportedSourceNatTypes).toLowerCase();
                        if (supportedSourceNatTypes.contains("zone")) {
                            sharedSourceNat = true;
                        }
                    }
                    
                    if (!sharedSourceNat) {
                        // Manage the entry for this network's source NAT IP address
                        List<IPAddressVO> sourceNatIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
                        if (sourceNatIps.size() == 1) {
                            String publicIp = sourceNatIps.get(0).getAddress().addr();
                            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
                                throw new ExecutionException(networkErrorMsg + ", source NAT IP = " + publicIp);
                            }
                        }
                        
                        // Manage one entry for each static NAT rule in this network
                        List<IPAddressVO> staticNatIps = _ipAddressDao.listStaticNatPublicIps(network.getId());
                        for (IPAddressVO staticNatIp : staticNatIps) {
                            String publicIp = staticNatIp.getAddress().addr();
                            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
                                throw new ExecutionException(networkErrorMsg + ", static NAT rule public IP = " + publicIp);
                            }  
                        }
                        
                        // Manage one entry for each port forwarding rule in this network
                        List<PortForwardingRuleVO> portForwardingRules = _portForwardingRulesDao.listByNetwork(network.getId());
                        for (PortForwardingRuleVO portForwardingRule : portForwardingRules) {
                            String publicIp = _networkMgr.getIp(portForwardingRule.getSourceIpAddressId()).getAddress().addr();                 
                            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
                                throw new ExecutionException(networkErrorMsg + ", port forwarding rule public IP = " + publicIp);
                            }   
                        }
                    } else {
                        // Manage the account-wide entry for the external firewall
                        if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), null, externalFirewall.getId(), firewallAnswer)) {
                            throw new ExecutionException(networkErrorMsg);
                        }
                    }                                        
                    
                    // If an external load balancer is added, manage one entry for each load balancing rule in this network
                    if (externalLoadBalancer != null && lbAnswer != null) {
                        List<LoadBalancerVO> loadBalancers = _loadBalancerDao.listByNetworkId(network.getId());
                        for (LoadBalancerVO loadBalancer : loadBalancers) {
                            String publicIp = _networkMgr.getIp(loadBalancer.getSourceIpAddressId()).getAddress().addr();               
                            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalLoadBalancer.getId(), lbAnswer)) {
                                throw new ExecutionException(networkErrorMsg + ", load balancing rule public IP = " + publicIp);
                            }   
                        }
                    }
                                      
                }                
                
                return txn.commit();
            } catch (Exception e) {
                s_logger.warn("Exception: ", e);
                txn.rollback();
                return false;
            } finally {
                txn.close();
            }
        }
        
        private void runExternalNetworkUsageTask() {
            s_logger.debug("External network stats collector is running...");
            for (DataCenterVO zone : _dcDao.listAll()) {                
                // Make sure the zone is configured for external networking
                
                //FIXME: add another way to check if zone has external networking.
                
                if (!_networkMgr.zoneIsConfiguredForExternalNetworking(zone.getId())) {
                    s_logger.debug("Zone " + zone.getName() + " is not configured for external networking, so skipping usage check.");
                    continue;
                }
                
                // Only collect stats if there is an external firewall in this zone
                
                //FIXME: add another way to check if zone has external networking.
                
                HostVO externalFirewall = getExternalNetworkAppliance(zone.getId(), 0, Host.Type.ExternalFirewall);
                HostVO externalLoadBalancer = getExternalNetworkAppliance(zone.getId(), 0,  Host.Type.ExternalLoadBalancer);
                
                if (externalFirewall == null) {
                    s_logger.debug("Skipping usage check for zone " + zone.getName());
                    continue;
                }
                
                s_logger.debug("Collecting external network stats for zone " + zone.getName());
                
                ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();
        
                // Get network stats from the external firewall
                ExternalNetworkResourceUsageAnswer firewallAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalFirewall.getId(), cmd);
                if (firewallAnswer == null || !firewallAnswer.getResult()) {
                    String details = (firewallAnswer != null) ? firewallAnswer.getDetails() : "details unavailable";
                    String msg = "Unable to get external firewall stats for " + zone.getName() + " due to: " + details + ".";
                    s_logger.error(msg);
                    continue;
                } 
                                            
                ExternalNetworkResourceUsageAnswer lbAnswer = null;
                if (externalLoadBalancer != null) {
                    // Get network stats from the external load balancer
                    lbAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
                    if (lbAnswer == null || !lbAnswer.getResult()) {
                        String details = (lbAnswer != null) ? lbAnswer.getDetails() : "details unavailable";
                        String msg = "Unable to get external load balancer stats for " + zone.getName() + " due to: " + details + ".";
                        s_logger.error(msg);
                    }                    
                }
                
                List<DomainRouterVO> domainRoutersInZone = _routerDao.listByDataCenter(zone.getId());
                for (DomainRouterVO domainRouter : domainRoutersInZone) {
                    long accountId = domainRouter.getAccountId();
                    long zoneId = domainRouter.getDataCenterIdToDeployIn();
                    
                    AccountVO account = _accountDao.findById(accountId);
                    if (account == null) {
                        s_logger.debug("Skipping stats update for account with ID " + accountId);
                        continue;
                    }
                    
                    if (!manageStatsEntries(true, accountId, zoneId, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer)) {
                        continue;
                    }
                    
                    manageStatsEntries(false, accountId, zoneId, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer);
                }                
            }                                    
        }
        
        @Override
        public void run() {            
            GlobalLock scanLock = GlobalLock.getInternLock("ExternalNetworkManagerImpl");
            try {
                if (scanLock.lock(20)) {
                    try {
                        runExternalNetworkUsageTask();
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Problems while getting external network usage", e);
            } finally {
                scanLock.releaseRef();
            }
        }
    }
}
