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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.agent.api.routing.CreateLBApplianceCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.ApiConstants;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.network.ExternalNetworkDeviceManager.NetworkDevice;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StaticNatRuleImpl;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.Nic.State;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public abstract class ExternalLoadBalancerDeviceManagerImpl extends AdapterBase implements ExternalLoadBalancerDeviceManager, ResourceStateAdapter {

    @Inject NetworkExternalLoadBalancerDao _networkExternalLBDao;
    @Inject ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject HostDao _hostDao;
    @Inject DataCenterDao _dcDao;
    @Inject HostDetailsDao _detailsDao;
    @Inject NetworkManager _networkMgr;
    @Inject InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject NicDao _nicDao;
    @Inject AgentManager _agentMgr;
    @Inject ResourceManager _resourceMgr;
    @Inject IPAddressDao _ipAddressDao;
    @Inject VlanDao _vlanDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject AccountDao _accountDao;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject AccountManager _accountMgr;
    @Inject UserStatisticsDao _userStatsDao;
    @Inject NetworkDao _networkDao;
    @Inject DomainRouterDao _routerDao;
    @Inject LoadBalancerDao _loadBalancerDao;
    @Inject PortForwardingRulesDao _portForwardingRulesDao;
    @Inject ConfigurationDao _configDao;
    @Inject HostDetailsDao _hostDetailDao;
    @Inject NetworkExternalLoadBalancerDao _networkLBDao;
    @Inject NetworkServiceMapDao _ntwkSrvcProviderDao;

    ScheduledExecutorService _executor;
    int _externalNetworkStatsInterval;
    protected String _name;
    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalLoadBalancerDeviceManagerImpl.class);

    @Override
    @DB
    public ExternalLoadBalancerDeviceVO addExternalLoadBalancer(long physicalNetworkId, String url, String username, String password, String deviceName, ServerResource resource) {

        String guid;
        PhysicalNetworkVO pNetwork=null;
        NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        long zoneId;

        if ((ntwkDevice == null) || (url == null) || (username == null) || (resource == null) || (password == null) ) {
            throw new InvalidParameterValueException("Atleast one of the required parameters (url, username, password," +
                    " server resource, zone id/physical network id) is not specified or a valid parameter.");
        }

        pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        zoneId = pNetwork.getDataCenterId();

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null ) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkDevice.getNetworkServiceProvder() + 
                    " is not enabled in the physical network: " + physicalNetworkId + "to add this device" );
        } else if ((ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown)
                || (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Disabled)) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + 
                    " is not added or in shutdown state in the physical network: " + physicalNetworkId + "to add this device" );
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
        Map hostDetails = new HashMap<String, String>();
        guid = getExternalNetworkResourceGuid(pNetwork.getId(), deviceName, ipAddress);
        hostDetails.put("guid", guid);
        hostDetails.put("zoneId", String.valueOf(pNetwork.getDataCenterId()));
        hostDetails.put("ip", ipAddress);
        hostDetails.put("physicalNetworkId", String.valueOf(pNetwork.getId()));
        hostDetails.put("username", username);
        hostDetails.put("password", password);
        hostDetails.put("deviceName", deviceName);

        // leave parameter validation to be part server resource configure
        Map<String, String> params = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), false, params);
        hostDetails.putAll(params);

        try {
            resource.configure(guid, hostDetails);

            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalLoadBalancer, hostDetails);
            if (host != null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();

                ExternalLoadBalancerDeviceVO lbDeviceVO = new ExternalLoadBalancerDeviceVO(host.getId(), pNetwork.getId(), ntwkSvcProvider.getProviderName(), deviceName);
                _externalLoadBalancerDeviceDao.persist(lbDeviceVO);
                
                DetailVO hostDetail = new DetailVO(host.getId(), ApiConstants.LOAD_BALANCER_DEVICE_ID, String.valueOf(lbDeviceVO.getId()));
                _hostDetailDao.persist(hostDetail);

                txn.commit();
                return lbDeviceVO;
            } else {
                throw new CloudRuntimeException("Failed to add load balancer device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean deleteExternalLoadBalancer(long hostId) {
        HostVO externalLoadBalancer = _hostDao.findById(hostId);
        if (externalLoadBalancer == null) {
            throw new InvalidParameterValueException("Could not find an external load balancer with ID: " + hostId);
        }

        DetailVO lbHostDetails = _hostDetailDao.findDetail(hostId, ApiConstants.LOAD_BALANCER_DEVICE_ID);
        long lbDeviceId = Long.parseLong(lbHostDetails.getValue());
        
        // check if any networks are using this load balancer device
        List<NetworkExternalLoadBalancerVO> networks = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
        if ((networks != null) && !networks.isEmpty()) {
            throw new CloudRuntimeException("Delete can not be done as there are networks using this load balancer device ");
        }

        try {
            if (_resourceMgr.maintain(hostId) && _resourceMgr.deleteHost(hostId, false, false)) {
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
    public List<Host> listExternalLoadBalancers(long physicalNetworkId, String deviceName) {
        List<Host> lbHosts = new ArrayList<Host>();
        NetworkDevice lbNetworkDevice = NetworkDevice.getNetworkDevice(deviceName);
        PhysicalNetworkVO pNetwork=null;

        pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        
        if ((pNetwork == null) || (lbNetworkDevice == null)) {
            throw new InvalidParameterValueException("Atleast one of the required parameter physical networkId, device name is invalid.");
        }

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(),
                lbNetworkDevice.getNetworkServiceProvder());
        //if provider not configured in to physical network, then there can be no instances
        if (ntwkSvcProvider == null) {
            return null; 
        }

        List<ExternalLoadBalancerDeviceVO> lbDevices = _externalLoadBalancerDeviceDao.listByPhysicalNetworkAndProvider(physicalNetworkId,
                ntwkSvcProvider.getProviderName());
        for (ExternalLoadBalancerDeviceVO provderInstance : lbDevices) {
            lbHosts.add(_hostDao.findById(provderInstance.getHostId()));
        }
        return lbHosts;
    }

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

    public String getExternalNetworkResourceGuid(long physicalNetworkId, String deviceName, String ip) {
        return physicalNetworkId + "-" + deviceName + "-" + ip;
    }

    @Override
    public ExternalLoadBalancerDeviceVO findSuitableLoadBalancerForNetwork(Network network, boolean dedicatedLb) throws InsufficientCapacityException {
        long physicalNetworkId = network.getPhysicalNetworkId();
        List<ExternalLoadBalancerDeviceVO> lbDevices =null;
        String provider = _ntwkSrvcProviderDao.getProviderForServiceInNetwork(network.getId(), Service.Lb);
        assert(provider != null);

        if (dedicatedLb) {
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Free);
            if (lbDevices != null && !lbDevices.isEmpty()) {

              //return first device that is free, fully configured and meant for dedicated use
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {
                    if (lbdevice.getState() == LBDeviceState.Enabled && lbdevice.getIsDedicatedDevice()) {
                        return lbdevice; 
                    }
                }

                //if there are no dedicated lb device then return first device that is free, fully configured
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {
                    if (lbdevice.getState() == LBDeviceState.Enabled) {
                        return lbdevice; 
                    }
                }
            }
        } else {
            // get the LB devices that are already allocated for shared use
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Shared);

            if (lbDevices != null) {

                ExternalLoadBalancerDeviceVO maxFreeCapacityLbdevice = null;
                long maxFreeCapacity = 0;

                // loop through the LB device in the physical network and pick the one with maximum free capacity 
                for (ExternalLoadBalancerDeviceVO lbdevice: lbDevices) {

                    // skip if device is not enabled 
                    if (lbdevice.getState() != LBDeviceState.Enabled) {
                        continue;
                    }

                    // skip if the device is intended to be used in dedicated mode only
                    if (lbdevice.getIsDedicatedDevice()) {
                        continue;
                    }

                    List<NetworkExternalLoadBalancerVO> mappedNetworks = _networkExternalLBDao.listByLoadBalancerDeviceId(lbdevice.getId());
                    // get the list of guest networks that are mapped to this load balancer
                    long usedCapacity = ((mappedNetworks == null) || (mappedNetworks.isEmpty()))? 0 : mappedNetworks.size();
                    // get max number of guest networks that can be mapped to this device
                    long fullCapacity = lbdevice.getCapacity();
 
                    long freeCapacity = fullCapacity - usedCapacity;
                    if (freeCapacity > 0) {
                        if (maxFreeCapacityLbdevice == null) {
                            maxFreeCapacityLbdevice = lbdevice;
                            maxFreeCapacity = freeCapacity;
                        }
                        if (freeCapacity > maxFreeCapacity) {
                            maxFreeCapacityLbdevice = lbdevice;
                            maxFreeCapacity = freeCapacity;
                        }
                    }
                }
            }

            // if we are here then there are no existing LB devices in shared use or the devices in shared use has no free capacity
            // so allocate a new one from the pool of free LB devices
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Free);
            if (lbDevices != null && !lbDevices.isEmpty()) {
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {
                    if (lbdevice.getState() == LBDeviceState.Enabled && !lbdevice.getIsDedicatedDevice()) {
                        return lbdevice;
                    }
                }
            }
        }

        throw new InsufficientNetworkCapacityException("Unable to find a load balancing provider with sufficient capcity " +
                " to implement the network", Network.class, network.getId());
    }

    HostVO getFirewallProviderForNetwork(Network network) {
        String fwProvider = _ntwkSrvcProviderDao.getProviderForServiceInNetwork(network.getId(),  Service.Firewall);

        if (fwProvider.equalsIgnoreCase("VirtualRouter")) {
            //FIXME: get the host Id of the host on which the virtual router is running
        } else {
            //FIXME: external firewall host object
        }

        return null;
    }

    @Override
    public ExternalLoadBalancerDeviceVO getExternalLoadBalancerForNetwork(Network network) {
        NetworkExternalLoadBalancerVO lbDeviceForNetwork = _networkExternalLBDao.findByNetworkId(network.getId());
        if (lbDeviceForNetwork != null) {
            long lbDeviceId = lbDeviceForNetwork.getExternalLBDeviceId();
            ExternalLoadBalancerDeviceVO lbDeviceVo = _externalLoadBalancerDeviceDao.findById(lbDeviceId);
            assert(lbDeviceVo != null);
        }
        return null;
    }

    public void setExternalLoadBalancerForNetwork(Network network, long externalLBDeviceID) {
        NetworkExternalLoadBalancerVO lbDeviceForNetwork = new NetworkExternalLoadBalancerVO(network.getId(), externalLBDeviceID);
        _networkExternalLBDao.persist(lbDeviceForNetwork);
    }

    private boolean externalLoadBalancerIsInline(HostVO externalLoadBalancer) {
        DetailVO detail = _detailsDao.findDetail(externalLoadBalancer.getId(), "inline");
        return (detail != null && detail.getValue().equals("true"));
    }

    private NicVO savePlaceholderNic(Network network, String ipAddress) {
        NicVO nic = new NicVO(null, null, network.getId(), null);
        nic.setIp4Address(ipAddress);
        nic.setReservationStrategy(ReservationStrategy.PlaceHolder);
        nic.setState(State.Reserved);
        return _nicDao.persist(nic);
    }
    
    private void applyStaticNatRuleForInlineLBRule(DataCenterVO zone, Network network, HostVO firewallHost, boolean revoked, String publicIp, String privateIp) throws ResourceUnavailableException {
        List<StaticNatRuleTO> staticNatRules = new ArrayList<StaticNatRuleTO>();
        IPAddressVO ipVO = _ipAddressDao.listByDcIdIpAddress(zone.getId(), publicIp).get(0);
        VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
        FirewallRuleVO fwRule = new FirewallRuleVO(null, ipVO.getId(), -1, -1, "any", network.getId(), network.getAccountId(), network.getDomainId(), Purpose.StaticNat, null, null, null, null);
        FirewallRule.State state = !revoked ? FirewallRule.State.Add : FirewallRule.State.Revoke;
        fwRule.setState(state);
        StaticNatRule rule = new StaticNatRuleImpl(fwRule, privateIp);
        StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, vlan.getVlanTag(), publicIp, privateIp);
        staticNatRules.add(ruleTO);
        
        applyStaticNatRules(staticNatRules, network, firewallHost.getId());
    }

    protected void applyStaticNatRules(List<StaticNatRuleTO> staticNatRules, Network network, long firewallHostId) throws ResourceUnavailableException {
        if (!staticNatRules.isEmpty()) {
            SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(staticNatRules);
            Answer answer = _agentMgr.easySend(firewallHostId, cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "firewall provider for the network was unable to apply static nat rules due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, Network.class, network.getId());
            }
        }
    }

    @Override
    public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        // Find the external load balancer in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);

        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network); 
        assert(lbDeviceVO != null) : "There is no device assigned to this network how apply rules ended up here??";
        HostVO externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());

        boolean externalLoadBalancerIsInline = externalLoadBalancerIsInline(externalLoadBalancer);

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
                HostVO firewallProviderHost = null;

                if (externalLoadBalancerIsInline) {
                    firewallProviderHost = getFirewallProviderForNetwork(network);
                }

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
                        
                        // On the firewall provider for the network, create a static NAT rule between the source IP address and the load balancing IP address                  
                        applyStaticNatRuleForInlineLBRule(zone, network, firewallProviderHost, revoked, srcIp, loadBalancingIpNic.getIp4Address());
                    } else {
                        loadBalancingIpNic = _nicDao.findById(mapping.getNicId());
                    }
                } else {
                    if (mapping != null) {
                        // Find the NIC that the mapping refers to
                        loadBalancingIpNic = _nicDao.findById(mapping.getNicId());
                        
                        // On the firewall provider for the network, delete the static NAT rule between the source IP address and the load balancing IP address
                        applyStaticNatRuleForInlineLBRule(zone, network, firewallProviderHost, revoked, srcIp, loadBalancingIpNic.getIp4Address());
                        
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
    @DB
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException, InsufficientCapacityException {
        if (guestConfig.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("External load balancer can only be user for guest networks.");
            return false;
        }

        long zoneId = guestConfig.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = null;
        String provider = _ntwkSrvcProviderDao.getProviderForServiceInNetwork(guestConfig.getId(), Service.Lb);
        long physicalNetworkId = guestConfig.getPhysicalNetworkId();

        if (add) {
            boolean retry = true;
            boolean provisionLB = false;

            while (retry) {
                GlobalLock deviceMapLock =  GlobalLock.getInternLock("LoadBalancerAllocLock");
                Transaction txn = Transaction.currentTxn();
                try {
                    if (deviceMapLock.lock(120)) {
                        try {
                            NetworkOfferingVO offering = _networkOfferingDao.findById(guestConfig.getNetworkOfferingId());
                            long lbDeviceId;
                            txn.start();

                            // FIXME: should the device allocation be done during network implement phase or do a lazy allocation 
                            // when first rule for the network is configured??

                            // find a load balancer device as per the network offering
                            boolean dedicatedLB = offering.getDedicatedLB();
                            try {
                                ExternalLoadBalancerDeviceVO lbDevice = findSuitableLoadBalancerForNetwork(guestConfig, dedicatedLB);
                                lbDeviceId = lbDevice.getId();

                                // persist the load balancer device id that will be used for this network. Once a network
                                // is implemented on a LB device then later on all rules will be programmed on to same device
                                NetworkExternalLoadBalancerVO networkLB = new NetworkExternalLoadBalancerVO(guestConfig.getId(), lbDeviceId);
                                _networkExternalLBDao.persist(networkLB);

                                // mark device to be in use
                                lbDevice.setAllocationState(dedicatedLB ? LBDeviceAllocationState.Dedicated : LBDeviceAllocationState.Shared);
                                _externalLoadBalancerDeviceDao.update(lbDeviceId, lbDevice);

                                // return the HostVO for the lb device
                                externalLoadBalancer = _hostDao.findById(lbDevice.getHostId());
                                txn.commit();

                                // got the load balancer for the network, so skip retry
                                provisionLB = false;
                                retry = false;
                            } catch (InsufficientCapacityException exception) {
                                if (provisionLB) {
                                    retry = false;
                                    throw exception; // if already attempted once throw out of capacity exception
                                }
                                provisionLB = true;  // if possible provision a LB appliance in the physical network
                            }
                        } finally {
                            deviceMapLock.unlock();
                            if (externalLoadBalancer == null) {
                                txn.rollback();
                            }
                        }
                    }
                } finally {
                    deviceMapLock.releaseRef();
                }

                // there are no LB devices or there is no free capacity on the devices in the physical network so provision a new LB appliance
                if (provisionLB) {
                    // check if LB appliance can be dynamically provisioned
                    List<ExternalLoadBalancerDeviceVO> providerLbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Provider);
                    if ((providerLbDevices != null) && (!providerLbDevices.isEmpty())) {
                        for (ExternalLoadBalancerDeviceVO lbProviderDevice : providerLbDevices) {
                            if (lbProviderDevice.getState() == LBDeviceState.Enabled) {
                                // acquire a private IP needed for LB appliance to be provisioned
                                // TODO: get the ip from the pool of private IP's configured for physical network
                                String lbIP = null;
                                
                                //TODO: get the configuration details needed to provision LB instances
                                String username = null;
                                String password = null;
                                String publiInterface = null;
                                String privateInterface = null;

                                // send CreateLBApplianceCommand to the host capable of provisioning
                                CreateLBApplianceCommand lbProvisionCmd = new CreateLBApplianceCommand(lbIP);
                                Answer answer = _agentMgr.easySend(lbProviderDevice.getHostId(), lbProvisionCmd);

                                if (answer == null || !answer.getResult()) {
                                    s_logger.trace("Could not provision load balancer instance on the load balancer device " + lbProviderDevice.getId());
                                    continue;
                                }

                                //add the appliance as external load balancer
                                //addExternalLoadBalancer();
                                //add the appliance to pool of the load balancers
                            }
                        }
                    }
                }
            }
        } else {
            // find the load balancer device allocated for the network
            ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(guestConfig); 
            assert(lbDeviceVO != null) : "There is no device assigned to this network how did shutdown network ended up here??";
            externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            assert (externalLoadBalancer != null) : "There is no device assigned to this network how did shutdown network ended up here??";
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

        if (!add) {
            Transaction txn = Transaction.currentTxn();
            txn.start();

            // since network is shutdown remove the network mapping to the load balancer device
            NetworkExternalLoadBalancerVO networkLBDevice = _networkExternalLBDao.findByNetworkId(guestConfig.getId());
            _networkExternalLBDao.remove(networkLBDevice.getId());

            // if this is the last network mapped to the load balancer device then set device allocation state to be free
            List<NetworkExternalLoadBalancerVO> ntwksMapped = _networkExternalLBDao.listByLoadBalancerDeviceId(networkLBDevice.getExternalLBDeviceId());
            if (ntwksMapped == null || ntwksMapped.isEmpty()) {
                ExternalLoadBalancerDeviceVO lbDevice = _externalLoadBalancerDeviceDao.findById(networkLBDevice.getExternalLBDeviceId());
                lbDevice.setAllocationState(LBDeviceAllocationState.Free);
                _externalLoadBalancerDeviceDao.update(lbDevice.getId(), lbDevice);
                
                //TODO: if device is cloud managed then take action
            }
            txn.commit();
        }

        Account account = _accountDao.findByIdIncludingRemoved(guestConfig.getAccountId());
        String action = add ? "implemented" : "shut down";
        s_logger.debug("External load balancer has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() + ") with VLAN tag " + guestVlanTag);

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _externalNetworkStatsInterval = NumbersUtil.parseInt(_configDao.getValue(Config.RouterStatsInterval.key()), 300);
        if (_externalNetworkStatsInterval > 0){
            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ExternalNetworkMonitor"));        
        }

        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean start() {
        if (_externalNetworkStatsInterval > 0){
            _executor.scheduleAtFixedRate(new ExternalLoadBalancerDeviceNetworkUsageTask(), _externalNetworkStatsInterval, _externalNetworkStatsInterval, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected class ExternalLoadBalancerDeviceNetworkUsageTask implements Runnable {

        public ExternalLoadBalancerDeviceNetworkUsageTask() {

        }

        @Override
        public void run() {
            GlobalLock scanLock = GlobalLock.getInternLock("ExternalLoadBalancerDeviceManagerImpl");
            try {
                if (scanLock.lock(20)) {
                    try {
                        runExternalLoadBalancerNetworkUsageTask();
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

        private void runExternalLoadBalancerNetworkUsageTask() {
            s_logger.debug("External load balancer devices stats collector is running...");

            for (DataCenterVO zone : _dcDao.listAll()) {
                List<DomainRouterVO> domainRoutersInZone = _routerDao.listByDataCenter(zone.getId());
                for (DomainRouterVO domainRouter : domainRoutersInZone) {
                    long accountId = domainRouter.getAccountId();
                    long zoneId = zone.getId();
                    List<NetworkVO> networksForAccount = _networkDao.listBy(accountId, zoneId, Network.GuestType.Isolated);
                    
                    for (NetworkVO network : networksForAccount) {
                        if (!_networkMgr.networkIsConfiguredForExternalNetworking(zoneId, network.getId())) {
                            s_logger.debug("Network " + network.getId() + " is not configured for external networking, so skipping usage check.");
                            continue;
                        }

                        HostVO externalFirewall = getFirewallProviderForNetwork(network);
                        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network); 
                        assert(lbDeviceVO != null);
                        HostVO externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
                        s_logger.debug("Collecting external network stats for network " + network.getId());

                        ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();

                        // Get network stats from the external firewall
                        ExternalNetworkResourceUsageAnswer firewallAnswer = null;
                        if (externalFirewall != null) {
                            firewallAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalFirewall.getId(), cmd);
                            if (firewallAnswer == null || !firewallAnswer.getResult()) {
                                String details = (firewallAnswer != null) ? firewallAnswer.getDetails() : "details unavailable";
                                String msg = "Unable to get external firewall stats for " + zone.getName() + " due to: " + details + ".";
                                s_logger.error(msg);
                                continue;
                            }
                        }

                       // Get network stats from the external load balancer
                        ExternalNetworkResourceUsageAnswer lbAnswer = null;
                        if (externalLoadBalancer != null) {
                            lbAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
                            if (lbAnswer == null || !lbAnswer.getResult()) {
                                String details = (lbAnswer != null) ? lbAnswer.getDetails() : "details unavailable";
                                String msg = "Unable to get external load balancer stats for " + zone.getName() + " due to: " + details + ".";
                                s_logger.error(msg);
                            }
                        }

                        AccountVO account = _accountDao.findById(accountId);
                        if (account == null) {
                            s_logger.debug("Skipping stats update for account with ID " + accountId);
                            continue;
                        }
                        
                        if (!manageStatsEntries(true, accountId, zoneId, network, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer)) {
                            continue;
                        }
                        
                        manageStatsEntries(false, accountId, zoneId, network, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer);
                    }
                }
            }
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
        private boolean manageStatsEntries(boolean create, long accountId, long zoneId, Network network,
                                           HostVO externalFirewall, ExternalNetworkResourceUsageAnswer firewallAnswer,
                                           HostVO externalLoadBalancer, ExternalNetworkResourceUsageAnswer lbAnswer) {
            String accountErrorMsg = "Failed to update external network stats entry. Details: account ID = " + accountId;
            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
            try {
                txn.start();
                String networkErrorMsg = accountErrorMsg + ", network ID = " + network.getId();
                    
                boolean sharedSourceNat = false;
                Map<Network.Capability, String> sourceNatCapabilities = _networkMgr.getNetworkServiceCapabilities(network.getId(), Service.SourceNat);
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
                return txn.commit();
            } catch (Exception e) {
                s_logger.warn("Exception: ", e);
                txn.rollback();
                return false;
            } finally {
                txn.close();
            }
        }
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource,
            Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupExternalLoadBalancerCommand)) {
            return null;
        }
        host.setType(Host.Type.ExternalLoadBalancer);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // TODO Auto-generated method stub
        return null;
    }
}
