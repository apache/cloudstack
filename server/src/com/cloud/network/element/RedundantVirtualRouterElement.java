package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ConfigureRedundantVirtualRouterElementCmd;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.NetworkOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class RedundantVirtualRouterElement extends VirtualRouterElement implements RedundantVirtualRouterElementService {
    private static final Logger s_logger = Logger.getLogger(RedundantVirtualRouterElement.class);
    
    @Inject NetworkManager _networkMgr;
    @Inject VirtualRouterProviderDao _vrElementsDao;
    
    @Override
    public Provider getProvider() {
        return Provider.RedundantVirtualRouter;
    }
    
    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(guestConfig, Service.Gateway)) {
            return false;
        }
        
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramNetwork, true);

        _routerMgr.deployVirtualRouter(guestConfig, dest, _accountMgr.getAccount(guestConfig.getAccountId()), params, getProvider());

        return true;
    }
    
    
    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(network, Service.Gateway)) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
            List<DomainRouterVO> routers = _routerMgr.deployVirtualRouter(network, dest, _accountMgr.getAccount(network.getAccountId()), uservm.getParameters(), getProvider());
            if ((routers == null) || (routers.size() == 0)) {
                throw new ResourceUnavailableException("Can't find at least one running router!", this.getClass(), 0);
            }
            List<VirtualRouter> rets = _routerMgr.addVirtualMachineIntoNetwork(network, nic, uservm, dest, context, routers);                                                                                                                      
            return (rets != null) && (!rets.isEmpty());
        } else {
            return false;
        }
    }
    
    @Override
    public String getPropertiesFile() {
        return "virtualrouter_commands.properties";
    }
    
    @Override
    public boolean configure(ConfigureRedundantVirtualRouterElementCmd cmd) {
        VirtualRouterProviderVO element = _vrElementsDao.findByNspIdAndType(cmd.getNspId(), VirtualRouterProviderType.RedundantVirtualRouterElement);
        if (element == null) {
            s_logger.trace("Can't find element with UUID " + cmd.getNspId());
            return false;
        }
        element.setEnabled(cmd.getEnabled());
        _vrElementsDao.persist(element);
        
        return true;
    }
    
    @Override
    public VirtualRouterProvider addElement(Long nspId) {
        VirtualRouterProviderVO element = _vrElementsDao.findByNspIdAndType(nspId, VirtualRouterProviderType.RedundantVirtualRouterElement);
        if (element != null) {
            s_logger.trace("There is already a redundant virtual router element with service provider id " + nspId);
            return null;
        }
        element = new VirtualRouterProviderVO(nspId, null, VirtualRouterProviderType.RedundantVirtualRouterElement);
        _vrElementsDao.persist(element);
        return element;
    }
    
    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(provider.getId(), VirtualRouterProviderType.RedundantVirtualRouterElement);
        if (element == null) {
            return false;
        }
        return element.isEnabled();
    }

    @Override
    public Long getIdByNspId(Long nspId) {
        VirtualRouterProviderVO vr = _vrElementsDao.findByNspIdAndType(nspId, VirtualRouterProviderType.RedundantVirtualRouterElement);
        return vr.getId();
    }
    
    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(provider.getId(), VirtualRouterProviderType.RedundantVirtualRouterElement);
        if (element == null) {
            return true;
        }
        //Find domain routers
        long elementId = element.getId();
        List<DomainRouterVO> routers = _routerDao.listByElementId(elementId);
        boolean result = true;
        for (DomainRouterVO router : routers) {
            result = result && (_routerMgr.destroyRouter(router.getId()) != null);
        }
        return result;
    }
}
