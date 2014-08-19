package com.cloud.network.router;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloud.network.router.deployment.RouterDeploymentDefinition;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

public interface NetworkHelper {

    public abstract String getRouterControlIp(long routerId);

    public abstract String getRouterIpInNetwork(long networkId, long instanceId);

    //    @Override
    public abstract boolean sendCommandsToRouter(VirtualRouter router,
            Commands cmds) throws AgentUnavailableException;

    public abstract void handleSingleWorkingRedundantRouter(
            List<? extends VirtualRouter> connectedRouters,
            List<? extends VirtualRouter> disconnectedRouters, String reason)
            throws ResourceUnavailableException;

    public abstract int getRealPriority(DomainRouterVO router);

    //    @Override
    public abstract NicTO getNicTO(VirtualRouter router, Long networkId,
            String broadcastUri);

    //    @Override
    public abstract VirtualRouter destroyRouter(long routerId, Account caller,
            Long callerUserId) throws ResourceUnavailableException,
            ConcurrentOperationException;

    /**
     * Checks if the router is at the required version. Compares MS version and router version.
     *
     * @param router
     * @return
     */
    //    @Override
    public abstract boolean checkRouterVersion(VirtualRouter router);

    //    @Override
    public abstract List<DomainRouterVO> startRouters(
            RouterDeploymentDefinition routerDeploymentDefinition)
            throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException;

    //    @Override
    public abstract DomainRouterVO startVirtualRouter(DomainRouterVO router,
            User user, Account caller, Map<Param, Object> params)
            throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException;

    //    @Override
    public abstract DomainRouterVO deployRouter(
            RouterDeploymentDefinition routerDeploymentDefinition,
            LinkedHashMap<Network, List<? extends NicProfile>> networks,
            boolean startRouter, List<HypervisorType> supportedHypervisors)
            throws InsufficientAddressCapacityException,
            InsufficientServerCapacityException, InsufficientCapacityException,
            StorageUnavailableException, ResourceUnavailableException;

    public abstract LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks(
            RouterDeploymentDefinition routerDeploymentDefinition)
            throws ConcurrentOperationException,
            InsufficientAddressCapacityException;

}