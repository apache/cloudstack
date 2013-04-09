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

package org.apache.cloudstack.network.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.router.ConfigureVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.CreateVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.ListVirtualRouterElementsCmd;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VirtualRouterElementService;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;

@Local(value = {NetworkElement.class})
public class InternalLoadBalancerElement extends AdapterBase implements LoadBalancingServiceProvider, VirtualRouterElementService{
    private static final Logger s_logger = Logger.getLogger(InternalLoadBalancerElement.class);
    protected static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject NetworkModel _ntwkModel;
    @Inject NetworkServiceMapDao _ntwkSrvcDao;
    @Inject DomainRouterDao _routerDao;
    @Inject VirtualRouterProviderDao _vrProviderDao;
    
    private boolean canHandle(Network config, List<LoadBalancingRule> rules) {
        if (config.getGuestType() != Network.GuestType.Isolated || config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Not handling network with Type  " + config.getGuestType() + " and traffic type " + config.getTrafficType());
            return false;
        }
        
        Map<Capability, String> lbCaps = this.getCapabilities().get(Service.Lb);
        if (!lbCaps.isEmpty()) {
            String schemeCaps = lbCaps.get(Capability.LbSchemes);
            if (schemeCaps != null && rules != null && !rules.isEmpty()) {
                for (LoadBalancingRule rule : rules) {
                    if (!schemeCaps.contains(rule.getScheme().toString())) {
                        s_logger.debug("Scheme " + rules.get(0).getScheme() + " is not supported by the provider " + this.getName());
                        return false;
                    }
                }
            }
        }
        
        if (!_ntwkModel.isProviderSupportServiceInNetwork(config.getId(), Service.Lb, getProvider())) {
            s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + Service.Lb
                    + " in the network " + config);
            return false;
        }
        return true;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.InternalLbVm;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Shutdown all the internal lb elements
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Shutdown all the internal lb elements
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        // TODO Shutdown all the internal lb elements
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Shutdown all the internal lb elements
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return null;
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        rules.add(rule);
        if (canHandle(network, rules)) {
            List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.InternalLB);
            if (routers == null || routers.isEmpty()) {
                return true;
            }
            return VirtualRouterElement.validateHAProxyLBRule(rule);
        }
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules) {
        return null;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp");
        lbCapabilities.put(Capability.SupportedStickinessMethods, VirtualRouterElement.getHAProxyStickinessCapability());
        lbCapabilities.put(Capability.LbSchemes, LoadBalancerContainer.Scheme.Internal.toString());

        capabilities.put(Service.Lb, lbCapabilities);
        return capabilities;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateVirtualRouterElementCmd.class);
        cmdList.add(ConfigureVirtualRouterElementCmd.class);
        cmdList.add(ListVirtualRouterElementsCmd.class);
        return cmdList;
    }

    @Override
    public VirtualRouterProvider configure(ConfigureVirtualRouterElementCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualRouterProvider addElement(Long nspId, VirtualRouterProviderType providerType) {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(nspId, providerType);
        if (element != null) {
            s_logger.debug("There is already a virtual router element with service provider id " + nspId);
            return null;
        }
        element = new VirtualRouterProviderVO(nspId, providerType);
        _vrProviderDao.persist(element);
        return element;
    }

    @Override
    public VirtualRouterProvider getCreatedElement(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends VirtualRouterProvider> searchForVirtualRouterElement(ListVirtualRouterElementsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

}
