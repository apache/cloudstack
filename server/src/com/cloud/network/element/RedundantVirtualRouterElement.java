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
import com.cloud.network.Network.Type;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.VirtualRouterElementsDao;
import com.cloud.network.element.VirtualRouterElements.VirtualRouterElementsType;
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
    @Inject VirtualRouterElementsDao _vrElementsDao;
    
    private boolean canHandle(Type networkType, long offeringId) {
        boolean result = (networkType == Network.Type.Isolated && _networkMgr.isProviderSupported(offeringId, Service.Gateway, Provider.VirtualRouter));
        if (!result) {
            s_logger.trace("Virtual router element only takes care of networktype " + Network.Type.Isolated + " for provider " + Provider.VirtualRouter.getName());
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
    public boolean configure(ConfigureRedundantVirtualRouterElementCmd cmd) {
        VirtualRouterElementsVO element = _vrElementsDao.findByUUID(cmd.getUUID());
        if (element == null) {
            s_logger.trace("Can't find element with UUID " + cmd.getUUID());
            return false;
        }
        if (cmd.getDhcpService() && cmd.getDhcpRange() == null) {
            s_logger.trace("DHCP service is provided, but no specific DHCP range!");
            return false;
        }
        if (cmd.getDnsService() && (cmd.getDns1() == null || cmd.getDomainName() == null)) {
            s_logger.trace("DNS service is provided, but no domain name or dns server!");
            return false;
        }
        if (cmd.getGatewayService() && cmd.getGateway() == null) {
            s_logger.trace("Gateway service is provided, but no gateway IP specific!");
            return false;
        }
        element.setIsDhcpProvided(cmd.getDhcpService());
        element.setDhcpRange(cmd.getDhcpRange());
        
        element.setIsDnsProvided(cmd.getDnsService());
        element.setDefaultDomainName(cmd.getDomainName());
        element.setDns1(cmd.getDns1());
        element.setDns2(cmd.getDns2());
        element.setInternalDns1(cmd.getInternalDns1());
        element.setInternalDns2(cmd.getInternalDns2());
        
        element.setIsGatewayProvided(cmd.getGatewayService());
        element.setGatewayIp(cmd.getGateway());
        
        element.setIsFirewallProvided(cmd.getFirewallService());
        element.setIsLoadBalanceProvided(cmd.getLbService());
        element.setIsSourceNatProvided(cmd.getSourceNatService());
        element.setIsVpnProvided(cmd.getVpnService());
        
        element.setIsReady(true);
        _vrElementsDao.persist(element);
        
        return true;
    }
    
    @Override
    public boolean addElement(Long nspId, String uuid) {
        long serviceOfferingId = _routerMgr.getDefaultVirtualRouterServiceOfferingId();
        if (serviceOfferingId == 0) {
            return false;
        }
        VirtualRouterElementsVO element = new VirtualRouterElementsVO(nspId, uuid, serviceOfferingId, false, VirtualRouterElementsType.RedundantVirtualRouterElement, 
                                        false, false, false, false, false, false, false);
        _vrElementsDao.persist(element);
        return true;
    }
}
