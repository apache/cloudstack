/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.lb.ElasticLoadBalancerManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;


@Local(value=NetworkElement.class)
public class ElasticLoadBalancerElement extends AdapterBase implements NetworkElement{
    private static final Logger s_logger = Logger.getLogger(ElasticLoadBalancerElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    @Inject ConfigurationManager _configMgr;
    @Inject NetworkManager _networkManager;
    @Inject ElasticLoadBalancerManager _lbMgr;
    
    private boolean canHandle(Network network) {
       // DataCenter zone = _configMgr.getZone(network.getDataCenterId());
        if (network.getGuestType() != Network.GuestIpType.Direct || network.getTrafficType() != TrafficType.Guest) {
            s_logger.debug("Not handling network with guest Type  " + network.getGuestType() + " and traffic type " + network.getTrafficType());
            return false;
        }
        
        return true;
    }
    
    @Override
    public Provider getProvider() {
        return Provider.ElasticLoadBalancerVm;
    }
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp");
        
        capabilities.put(Service.Lb, lbCapabilities);   
        return capabilities;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
       
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {
        
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO kill all loadbalancer vms by calling the ElasticLoadBalancerManager
        return false;
    }

    @Override
    public boolean restart(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO restart all loadbalancer vms by calling the ElasticLoadBalancerManager
        return false;
    }

    @Override
    public boolean destroy(Network network) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO  kill all loadbalancer vms by calling the ElasticLoadBalancerManager
        return false;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean applyRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network)) {
            return false;
        }
        
        return _lbMgr.applyLoadBalancerRules(network, rules);
    }
}
