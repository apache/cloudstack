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
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
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
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.resource.F5BigIpResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.resource.ResourceManager;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value = { ExternalLoadBalancerManager.class })
public class F5BigIpManagerImpl extends ExternalNetworkManagerImpl implements ExternalLoadBalancerManager {

    @Inject
    NetworkManager _networkMgr;
    @Inject
    NicDao _nicDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceManager _resourcMgr;

    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(F5BigIpManagerImpl.class);

    @Override
    public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd) {
        long zoneId = cmd.getZoneId();

        DataCenterVO zone = _dcDao.findById(zoneId);
        String zoneName;
        if (zone == null) {
            throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
        } else {
            zoneName = zone.getName();
        }

        List<HostVO> externalLoadBalancersInZone = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.ExternalLoadBalancer, zoneId);
        if (externalLoadBalancersInZone.size() != 0) {
            throw new InvalidParameterValueException("Already found an external load balancer in zone: " + zoneName);
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
        String privateInterface = params.get("privateinterface");
        String numRetries = params.get("numretries");

        if (publicInterface == null) {
            throw new InvalidParameterValueException("Please specify a public interface.");
        }

        if (privateInterface == null) {
            throw new InvalidParameterValueException("Please specify a private interface.");
        }

        if (numRetries == null) {
            numRetries = "1";
        }

        F5BigIpResource resource = new F5BigIpResource();
        String guid = getExternalNetworkResourceGuid(zoneId, ExternalNetworkResourceName.F5BigIp, ipAddress);

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

        try {
            resource.configure(guid, hostDetails);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalLoadBalancer, hostDetails);
        if (host != null) {
            zone.setLoadBalancerProvider(Network.Provider.F5BigIp.getName());
            _dcDao.update(zone.getId(), zone);
            return host;
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd) {
        long hostId = cmd.getId();
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        HostVO externalLoadBalancer = _hostDao.findById(hostId);
        if (externalLoadBalancer == null) {
            throw new InvalidParameterValueException("Could not find an external load balancer with ID: " + hostId);
        }

        try {
            if (_resourceMgr.maintain(hostId) && _resourceMgr.deleteHost(hostId, false, false)) {
                DataCenterVO zone = _dcDao.findById(externalLoadBalancer.getDataCenterId());
                
                if (zone.getNetworkType().equals(NetworkType.Advanced)) {
                    zone.setLoadBalancerProvider(Network.Provider.VirtualRouter.getName());
                } else if (zone.getNetworkType().equals(NetworkType.Basic)) {
                    zone.setLoadBalancerProvider(null);
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
    public List<HostVO> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd) {
        long zoneId = cmd.getZoneId();
        return _resourceMgr.listAllHostsInOneZoneByType(Host.Type.ExternalLoadBalancer, zoneId);
    }

    @Override
    public ExternalLoadBalancerResponse getApiResponse(Host externalLoadBalancer) {
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
    public boolean manageGuestNetwork(boolean add, Network guestConfig) throws ResourceUnavailableException {
        if (guestConfig.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("F5BigIpManager can only add/remove guest networks.");
            return false;
        }

        // Find the external load balancer in this zone
        long zoneId = guestConfig.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = getExternalNetworkAppliance(zoneId, Host.Type.ExternalLoadBalancer);

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
            String msg = "F5BigIpManager was unable to " + action + " the guest network on the external load balancer in zone " + zone.getName() + " due to " + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        List<String> reservedIpAddressesForGuestNetwork = _nicDao.listIpAddressInNetwork(guestConfig.getId());
        if (add && (!reservedIpAddressesForGuestNetwork.contains(selfIp))) {
            // Insert a new NIC for this guest network to reserve the self IP
            NicVO nic = new NicVO(null, null, guestConfig.getId(), null);
            nic.setIp4Address(selfIp);
            nic.setReservationStrategy(ReservationStrategy.PlaceHolder);
            nic.setState(Nic.State.Reserved);
            _nicDao.persist(nic);
        }

        Account account = _accountDao.findByIdIncludingRemoved(guestConfig.getAccountId());
        String action = add ? "implemented" : "shut down";
        s_logger.debug("F5BigIpManager has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() + ") with VLAN tag " + guestVlanTag);

        return true;
    }

    @Override
    public boolean applyLoadBalancerRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        // Find the external load balancer in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = getExternalNetworkAppliance(zoneId, Host.Type.ExternalLoadBalancer);

        if (externalLoadBalancer == null) {
            return false;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("F5BigIpManager was asked to apply LB rules for network with ID " + network.getId() + "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        List<LoadBalancingRule> loadBalancingRules = new ArrayList<LoadBalancingRule>();

        for (FirewallRule rule : rules) {
            if (rule.getPurpose().equals(Purpose.LoadBalancing)) {
                loadBalancingRules.add((LoadBalancingRule) rule);
            }
        }

        LoadBalancerTO[] loadBalancers = new LoadBalancerTO[loadBalancingRules.size()];
        for (int i = 0; i < loadBalancingRules.size(); i++) {
            LoadBalancingRule rule = loadBalancingRules.get(i);

            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String srcIp = _networkMgr.getIp(rule.getSourceIpAddressId()).getAddress().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();

            LoadBalancerTO loadBalancer = new LoadBalancerTO(srcIp, srcPort, protocol, algorithm, revoked, false, destinations);
            loadBalancers[i] = loadBalancer;
        }

        if (loadBalancers.length > 0) {
            LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(loadBalancers);
            Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "Unable to apply load balancer rules to the F5 BigIp appliance in zone " + zone.getName() + " due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }
}
