package com.cloud.network.router;

import java.util.List;
import java.util.Map;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

public interface VpcVirtualNetworkHelper {

    List<DomainRouterVO> deployVirtualRouterInVpc(Vpc vpc,
            DeployDestination dest, Account owner, Map<Param, Object> params, boolean isRedundant)
            throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException;

    NicProfile createPrivateNicProfileForGateway(VpcGateway privateGateway);

    List<DomainRouterVO> getVpcRouters(long vpcId);
}
