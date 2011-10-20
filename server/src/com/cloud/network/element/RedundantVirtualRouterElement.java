package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.Type;
import com.cloud.network.NetworkManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.element.RedundantVirtualRouterElementService;
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
    
    private boolean canHandle(Type networkType, long offeringId) {
        boolean result = (networkType == Network.Type.Isolated && _networkMgr.isProviderSupported(offeringId, Service.Gateway, Provider.VirtualRouter));
        if (!result) {
            s_logger.trace("Virtual router element only takes care of guest ip type " + GuestIpType.Virtual + " for provider " + Provider.VirtualRouter.getName());
        }
        return result;
    }


    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(guestConfig.getType(), offering.getId())) {
            return false;
        }
        
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.RestartNetwork, true);

        _routerMgr.deployVirtualRouter(guestConfig, dest, _accountMgr.getAccount(guestConfig.getAccountId()), params, true);

        return true;
    }
    
    
    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(network.getType(), network.getNetworkOfferingId())) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
            List<DomainRouterVO> routers = _routerMgr.deployVirtualRouter(network, dest, _accountMgr.getAccount(network.getAccountId()), uservm.getParameters(), true);
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
    public boolean configure() {
        // TODO Auto-generated method stub
        return false;
    }
}
