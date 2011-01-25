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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.vpn.RemoteAccessVpnElement;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;


@Local(value=NetworkElement.class)
public class VirtualRouterElement extends AdapterBase implements NetworkElement, RemoteAccessVpnElement {
    private static final Logger s_logger = Logger.getLogger(VirtualRouterElement.class);
    
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    
    @Inject NetworkDao _networkConfigDao;
    @Inject NetworkManager _networkMgr;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject UserVmManager _userVmMgr;
    @Inject UserVmDao _userVmDao;
    @Inject DomainRouterDao _routerDao;
    @Inject DataCenterDao _dataCenterDao;
    @Inject LoadBalancerDao _lbDao;
    @Inject AccountManager _accountMgr;
    
    private boolean canHandle(GuestIpType ipType, DataCenter dc) {
        String provider = dc.getGatewayProvider();
        return (ipType == GuestIpType.Virtual && provider.equals(Provider.VirtualRouter.getName()));
    }

    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(guestConfig.getGuestType(), dest.getDataCenter())) {
            return false;
        }
        
        _routerMgr.deployVirtualRouter(guestConfig, dest, context.getAccount());
        
        return true;
    }

    @Override
    public boolean prepare(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(config.getGuestType(), dest.getDataCenter())) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
            
            return _routerMgr.addVirtualMachineIntoNetwork(config, nic, uservm, dest, context, false) != null;
        } else {
            return false;
        }
    }

    @Override
    public boolean release(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) {
        return true;
    }

    @Override
    public boolean shutdown(Network config, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(config.getId());
        if (router == null) {
            return true;
        }
        if (_routerMgr.stopRouter(router.getId()) != null) {
            return true;
        } else {
            return false;
        }
    }
      
    @Override
    public boolean destroy(Network config) throws ConcurrentOperationException, ResourceUnavailableException{
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(config.getId());
        if (router == null) {
            return true;
        }
        return _routerMgr.destroyRouter(router.getId());
    }

    @Override
    public boolean applyRules(Network config, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
    
        DataCenter dc = _dataCenterDao.findById(config.getDataCenterId());
        if (canHandle(config.getGuestType(),dc)) {
            
            long networkId = config.getId();
            DomainRouterVO router = _routerDao.findByNetworkConfiguration(networkId);
            if (router == null) {
                s_logger.warn("Unable to apply firewall rules, virtual router doesn't exist in the network " + config.getId());
                throw new CloudRuntimeException("Unable to apply firewall rules");
            }
            
            if (router.getState() == State.Running || router.getState() == State.Starting) {
                if (rules != null && !rules.isEmpty()) {
                    if (rules.get(0).getPurpose() == Purpose.LoadBalancing) {
                        //for load balancer we have to resend all lb rules for the network
                        List<LoadBalancerVO> lbs = _lbDao.listByNetworkId(config.getId());
                        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
                        for (LoadBalancerVO lb : lbs) {
                            List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
                            lbRules.add(loadBalancing);
                        }
                        
                        return _routerMgr.applyLBRules(config, lbRules);
                    } else if (rules.get(0).getPurpose() == Purpose.PortForwarding) {
                        return _routerMgr.applyPortForwardingRules(config, (List<PortForwardingRule>)rules);
                    }
                } else {
                    return true;
                }
            } else if (router.getState() == State.Stopped || router.getState() == State.Stopping){
                s_logger.debug("Router is in " + router.getState() + ", so not sending apply firewall rules commands to the backend");
                return true;
            } else {
                s_logger.warn("Unable to apply firewall rules, virtual router is not in the right state " + router.getState());
                throw new CloudRuntimeException("Unable to apply firewall rules, domR is not in right state " + router.getState());
            }
        } 
        return false;
    }
    
    
    @Override
    public String[] applyVpnUsers(RemoteAccessVpn vpn, List<? extends VpnUser> users) throws ResourceUnavailableException{
        Network network = _networkConfigDao.findById(vpn.getNetworkId());
        DataCenter dc = _dataCenterDao.findById(network.getDataCenterId());
        if (canHandle(network.getGuestType(),dc)) {
            return _routerMgr.applyVpnUsers(network, users);
        } else {
            s_logger.debug("Element " + this.getName() + " doesn't handle applyVpnUsers command");
            return null;
        }
    }
    
    @Override
    public boolean start(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {
        DataCenter dc = _dataCenterDao.findById(network.getDataCenterId());
        if (canHandle(network.getGuestType(),dc)) {
            return _routerMgr.startRemoteAccessVpn(network, vpn);
        } else {
            s_logger.debug("Element " + this.getName() + " doesn't handle createVpn command");
            return false;
        }
    }
    
    @Override
    public boolean stop(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {
        DataCenter dc = _dataCenterDao.findById(network.getDataCenterId());
        if (canHandle(network.getGuestType(),dc)) {
            return _routerMgr.deleteRemoteAccessVpn(network, vpn);
        } else {
            s_logger.debug("Element " + this.getName() + " doesn't handle removeVpn command");
            return false;
        }
    }


    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException {
        DataCenter dc = _dataCenterDao.findById(network.getDataCenterId());
        if (canHandle(network.getGuestType(),dc)) {
            return _routerMgr.associateIP(network, ipAddress);
        } else {
            return false;
        }
    }
    

    @Override
    public Provider getProvider() {
        return Provider.VirtualRouter;
    }
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        
        //Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp");
        
        capabilities.put(Service.Lb, lbCapabilities);
        
        //Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.PortForwarding, "true");
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.StaticNat, "true");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        firewallCapabilities.put(Capability.SupportedSourceNatTypes, "per account");
        
        capabilities.put(Service.Firewall, firewallCapabilities);
        
        //Set capabilities for vpn
        Map<Capability, String> vpnCapabilities = new HashMap<Capability, String>();
        vpnCapabilities.put(Capability.SupportedVpnTypes, "pptp,l2tp,ipsec");
        
        capabilities.put(Service.Vpn, vpnCapabilities);
        capabilities.put(Service.Dns, null);
        capabilities.put(Service.UserData, null);
        capabilities.put(Service.Dhcp, null);
        capabilities.put(Service.Gateway, null);
        
        return capabilities;
    }
    
}
