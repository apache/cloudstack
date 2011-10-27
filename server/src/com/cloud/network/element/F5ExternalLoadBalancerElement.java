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

package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class F5ExternalLoadBalancerElement extends AdapterBase implements LoadBalancingServiceProvider {

    private static final Logger s_logger = Logger.getLogger(F5ExternalLoadBalancerElement.class);
    
    @Inject NetworkManager _networkManager;
    @Inject ExternalNetworkDeviceManager _externalNetworkManager;
    @Inject ConfigurationManager _configMgr;
    @Inject NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    
    private boolean canHandle(Network config) {
        DataCenter zone = _configMgr.getZone(config.getDataCenterId());
        if (config.getGuestType() != Network.GuestType.Isolated || config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Not handling network with Type  " + config.getGuestType() + " and traffic type " + config.getTrafficType());
            return false;
        }
        
        return (_networkManager.networkIsConfiguredForExternalNetworking(zone.getId(), config.getNetworkOfferingId()) && 
                _ntwkOfferingSrvcDao.isProviderSupported(config.getNetworkOfferingId(), Service.Lb, Network.Provider.F5BigIp));
    }

    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientNetworkCapacityException {
        
        if (!canHandle(guestConfig)) {
            return false;
        }
        
        return _externalNetworkManager.manageGuestNetworkWithExternalLoadBalancer(true, guestConfig);
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
    public boolean shutdown(Network guestConfig, ReservationContext context, boolean cleanup) throws ResourceUnavailableException, ConcurrentOperationException {
        if (!canHandle(guestConfig)) {
            return false;
        }
        
        return _externalNetworkManager.manageGuestNetworkWithExternalLoadBalancer(false, guestConfig);
    }
    
    @Override
    public boolean destroy(Network config) {
        return true;
    }
    
    @Override
    public boolean applyLBRules(Network config, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config)) {
            return false;
        }
    	
    	return _externalNetworkManager.applyLoadBalancerRules(config, rules);
    }
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
    	 Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
    	 
    	 // Set capabilities for LB service
         Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
         
         // Specifies that the RoundRobin and Leastconn algorithms are supported for load balancing rules
         lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn");
         
         // Specifies that load balancing rules can be made for either TCP or UDP traffic
         lbCapabilities.put(Capability.SupportedProtocols, "tcp,udp");
         
         // Specifies that this element can measure network usage on a per public IP basis
         lbCapabilities.put(Capability.TrafficStatistics, "per public ip");
         
         // Specifies that load balancing rules can only be made with public IPs that aren't source NAT IPs
         lbCapabilities.put(Capability.LoadBalancingSupportedIps, "additional");
         
         capabilities.put(Service.Lb, lbCapabilities);
         
         return capabilities;
    }
    
    @Override
    public Provider getProvider() {
        return Provider.F5BigIp;
    }
   
}
