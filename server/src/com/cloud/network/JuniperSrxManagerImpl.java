/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.commands.AddExternalFirewallCmd;
import com.cloud.api.commands.DeleteExternalFirewallCmd;
import com.cloud.api.commands.ListExternalFirewallsCmd;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.resource.JuniperSrxResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.Nic.State;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;

@Local(value = { ExternalFirewallManager.class })
public class JuniperSrxManagerImpl extends ExternalNetworkManagerImpl implements ExternalFirewallManager {

    @Inject
    NetworkManager _networkMgr;
    @Inject
    NicDao _nicDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    FirewallRulesDao _firewallRulesDao;
    @Inject
    UserStatisticsDao _userStatsDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountManager _accountMgr;
	@Inject
	ResourceManager _resourceMgr;

    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(JuniperSrxManagerImpl.class);

    @Override
    public Host addExternalFirewall(AddExternalFirewallCmd cmd) {
        long zoneId = cmd.getZoneId();

        DataCenterVO zone = _dcDao.findById(zoneId);
        String zoneName;
        if (zone == null) {
            throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
        } else {
            zoneName = zone.getName();
        }

        List<HostVO> externalFirewallsInZone = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.ExternalFirewall, zoneId);
        if (externalFirewallsInZone.size() != 0) {
            throw new InvalidParameterValueException("Already added an external firewall in zone: " + zoneName);
        }

        URI uri;
        try {
            uri = new URI(cmd.getUrl());
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        Map<String, String> params = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), true, params);
        String publicInterface = params.get("publicinterface");
        String usageInterface = params.get("usageinterface");
        String privateInterface = params.get("privateinterface");
        String publicZone = params.get("publiczone");
        String privateZone = params.get("privatezone");
        String numRetries = params.get("numretries");
        String timeout = params.get("timeout");

        if (publicInterface != null) {
            if (!publicInterface.contains(".")) {
                publicInterface += ".0";
            }
        } else {
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

        JuniperSrxResource resource = new JuniperSrxResource();
        String guid = getExternalNetworkResourceGuid(zoneId, ExternalNetworkResourceName.JuniperSrx, ipAddress);

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

        Host externalFirewall = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, hostDetails);
        if (externalFirewall != null) {
            zone.setFirewallProvider(Network.Provider.JuniperSRX.getName());                      
            zone.setUserDataProvider(Network.Provider.DhcpServer.getName());
            zone.setVpnProvider(null);
            
            if (zone.getGatewayProvider() == null || !zone.getGatewayProvider().equals(Network.Provider.ExternalGateWay)) {
                zone.setGatewayProvider(Network.Provider.JuniperSRX.getName());
            }
            
            if (zone.getDnsProvider() == null || !zone.getDnsProvider().equals(Network.Provider.ExternalDhcpServer)) {
                zone.setDnsProvider(Network.Provider.DhcpServer.getName());
            }
            
            if (zone.getDhcpProvider() == null || !zone.getDhcpProvider().equals(Network.Provider.ExternalDhcpServer)) {
                zone.setDhcpProvider(Network.Provider.DhcpServer.getName());
            }                                    
            
            if (zone.getLoadBalancerProvider() == null || !zone.getLoadBalancerProvider().equals(Network.Provider.F5BigIp.getName())) {
                zone.setLoadBalancerProvider(Network.Provider.None.getName());
            }

            _dcDao.update(zone.getId(), zone);
            return externalFirewall;
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteExternalFirewall(DeleteExternalFirewallCmd cmd) {
        long hostId = cmd.getId();
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        HostVO externalFirewall = _hostDao.findById(hostId);
        if (externalFirewall == null) {
            throw new InvalidParameterValueException("Could not find an external firewall with ID: " + hostId);
        }

		try {
			if (_resourceMgr.maintain(hostId) && _resourceMgr.deleteHost(hostId, false, false)) {
                DataCenterVO zone = _dcDao.findById(externalFirewall.getDataCenterId());
                zone.setFirewallProvider(Network.Provider.VirtualRouter.getName()); 
                zone.setUserDataProvider(Network.Provider.VirtualRouter.getName());
                zone.setVpnProvider(Network.Provider.VirtualRouter.getName());
                
                if (zone.getGatewayProvider() != null && !zone.getGatewayProvider().equals(Network.Provider.ExternalGateWay)) {
                    zone.setGatewayProvider(Network.Provider.VirtualRouter.getName());
                }                                              
                
                if (zone.getDnsProvider() != null && !zone.getDnsProvider().equals(Network.Provider.ExternalDhcpServer)) {
                    zone.setDnsProvider(Network.Provider.VirtualRouter.getName());
                }
                
                if (zone.getDhcpProvider() != null && !zone.getDhcpProvider().equals(Network.Provider.ExternalDhcpServer)) {
                    zone.setDhcpProvider(Network.Provider.VirtualRouter.getName());
                }
                                
                if (zone.getLoadBalancerProvider() != null && zone.getLoadBalancerProvider().equals(Network.Provider.None)) {
                    if (zone.getNetworkType().equals(NetworkType.Advanced)) {
                        zone.setLoadBalancerProvider(Network.Provider.VirtualRouter.getName());
                    } else if (zone.getNetworkType().equals(NetworkType.Basic)) {
                        zone.setLoadBalancerProvider(null);
                    }
                }
                
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
    public List<HostVO> listExternalFirewalls(ListExternalFirewallsCmd cmd) {
        long zoneId = cmd.getZoneId();
        return _resourceMgr.listAllHostsInOneZoneByType(Host.Type.ExternalFirewall, zoneId);
    }

    @Override
    public ExternalFirewallResponse getApiResponse(Host externalFirewall) {
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
    public boolean manageGuestNetwork(boolean add, Network network, NetworkOffering offering) throws ResourceUnavailableException {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("JuniperSrxManager can only add/remove guest networks.");
            return false;
        }

        // Find the external firewall in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalFirewall = getExternalNetworkAppliance(zoneId, Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }

        Account account = _accountDao.findByIdIncludingRemoved(network.getAccountId());
        boolean sharedSourceNat = offering.isSharedSourceNatService();
        String sourceNatIp = null;
        if (!sharedSourceNat) {
            // Get the source NAT IP address for this network          
            List<IPAddressVO> sourceNatIps = _networkMgr.listPublicIpAddressesInVirtualNetwork(network.getAccountId(), zoneId, true, null);

            if (sourceNatIps.size() != 1) {
                String errorMsg = "JuniperSrxManager was unable to find the source NAT IP address for account " + account.getAccountName();
                return true;
            } else {
                sourceNatIp = sourceNatIps.get(0).getAddress().addr();
            }
        }

        // Send a command to the external firewall to implement or shutdown the guest network
        long guestVlanTag = Long.parseLong(network.getBroadcastUri().getHost());
        String guestVlanGateway = network.getGateway();
        String guestVlanNetmask = NetUtils.cidr2Netmask(network.getCidr());

        // Get network rate
        Integer networkRate = _networkMgr.getNetworkRate(network.getId(), null);

        IpAddressTO ip = new IpAddressTO(network.getAccountId(), sourceNatIp, add, false, !sharedSourceNat, String.valueOf(guestVlanTag), guestVlanGateway, guestVlanNetmask, null, null, networkRate, false);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        Answer answer = _agentMgr.easySend(externalFirewall.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String action = add ? "implement" : "shutdown";
            String answerDetails = (answer != null) ? answer.getDetails() : "answer was null";
            String msg = "JuniperSrxManager was unable to " + action + " the guest network on the external firewall in zone " + zone.getName() + " due to " + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        List<String> reservedIpAddressesForGuestNetwork = _nicDao.listIpAddressInNetwork(network.getId());
        if (add && (!reservedIpAddressesForGuestNetwork.contains(network.getGateway()))) {
            // Insert a new NIC for this guest network to reserve the gateway address
            NicVO nic = new NicVO(null, null, network.getId(), null);
            nic.setIp4Address(network.getGateway());
            nic.setReservationStrategy(ReservationStrategy.PlaceHolder);
            nic.setState(State.Reserved);
            _nicDao.persist(nic);
        }

        String action = add ? "implemented" : "shut down";
        s_logger.debug("JuniperSrxManager has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() + ") with VLAN tag " + guestVlanTag);

        return true;
    }

    @Override
    public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        // Find the external firewall in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalFirewall = getExternalNetworkAppliance(zoneId, Host.Type.ExternalFirewall);

        if (externalFirewall == null) {
            return false;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("JuniperSrxManager was asked to apply firewall rules for network with ID " + network.getId() + "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        List<StaticNatRuleTO> staticNatRules = new ArrayList<StaticNatRuleTO>();
        List<PortForwardingRuleTO> portForwardingRules = new ArrayList<PortForwardingRuleTO>();

        for (FirewallRule rule : rules) {
            IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
            if (rule.getPurpose() == Purpose.StaticNat) {
                StaticNatRule staticNatRule = (StaticNatRule) rule;
                StaticNatRuleTO ruleTO = new StaticNatRuleTO(staticNatRule, sourceIp.getAddress().addr(), staticNatRule.getDestIpAddress());
                staticNatRules.add(ruleTO);
            } else if (rule.getPurpose() == Purpose.PortForwarding) {
                PortForwardingRuleTO ruleTO = new PortForwardingRuleTO((PortForwardingRule) rule, null, sourceIp.getAddress().addr());
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
                String msg = "JuniperSrxManager was unable to apply static nat rules to the SRX appliance in zone " + zone.getName() + " due to: " + details + ".";
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
                String msg = "JuniperSrxManager was unable to apply port forwarding rules to the SRX appliance in zone " + zone.getName() + " due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, zone.getId());
            }
        }
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddresses) throws ResourceUnavailableException {
        return true;
    }

}
