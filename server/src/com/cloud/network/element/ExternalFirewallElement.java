/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.ExternalFirewallManager;
import com.cloud.network.ExternalNetworkManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class ExternalFirewallElement extends AdapterBase implements NetworkElement  {

    private static final Logger s_logger = Logger.getLogger(ExternalFirewallElement.class);
    
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    
    @Inject NetworkManager _networkManager;
    @Inject ExternalNetworkManager _externalNetworkManager;
    @Inject ExternalFirewallManager _externalFirewallManager;
    @Inject HostDao _hostDao;
    @Inject ConfigurationManager _configMgr;
    @Inject NetworkOfferingDao _networkOfferingDao;
    
    private boolean canHandle(Network config) {
        DataCenter zone = _configMgr.getZone(config.getDataCenterId());
        if ((zone.getNetworkType() == NetworkType.Advanced && config.getGuestType() != Network.GuestIpType.Virtual) || (zone.getNetworkType() == NetworkType.Basic && config.getGuestType() != Network.GuestIpType.Direct)) {
            s_logger.trace("Not handling guest ip type = " + config.getGuestType());
            return false;
        }   
        
        return _networkManager.zoneIsConfiguredForExternalNetworking(zone.getId());
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientNetworkCapacityException {                              
        DataCenter zone = _configMgr.getZone(network.getDataCenterId());
        
        //don't have to implement network is Basic zone
        if (zone.getNetworkType() == NetworkType.Basic) {
            s_logger.debug("Not handling network implement in zone of type " + NetworkType.Basic);
            return false;
        }
        
        if (!canHandle(network)) {
            return false;
        }
    	
    	return _externalFirewallManager.manageGuestNetwork(true, network, offering);
    }

    @Override
    public boolean prepare(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException {
    	return true;
    }

    @Override
    public boolean release(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) {
    	return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException {
        DataCenter zone = _configMgr.getZone(network.getDataCenterId());
        NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        
        //don't have to implement network is Basic zone
        if (zone.getNetworkType() == NetworkType.Basic) {
            s_logger.debug("Not handling network shutdown in zone of type " + NetworkType.Basic);
            return false;
        }
        
        if (!canHandle(network)) {
            return false;
        }
        
        return _externalFirewallManager.manageGuestNetwork(false, network, offering);
    }
    
    @Override
    public boolean destroy(Network config) {
        return true;
    }
    
    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddresses) throws ResourceUnavailableException {
        if (!canHandle(network)) {
            return false;
        }
        
    	return _externalFirewallManager.applyIps(network, ipAddresses);
    }
    

    @Override
    public boolean applyRules(Network config, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config)) {
            return false;
        }
    	
    	return _externalFirewallManager.applyFirewallRules(config, rules);
    }
    
    @Override
    public Provider getProvider() {
        return Provider.JuniperSRX;
    }
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
  
        // Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.StaticNat, "true");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        firewallCapabilities.put(Capability.SupportedSourceNatTypes, "per account, per zone");
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.PortForwarding, "true");
        
        capabilities.put(Service.Firewall, firewallCapabilities);
        capabilities.put(Service.Gateway, null);

        return capabilities;
    }
    
    @Override
    public boolean restart(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        return true;
    }
    
    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        return false;
    }
}


