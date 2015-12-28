package com.cloud.naming;

import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.DomainRouterVO;

public interface RouterNamingPolicy extends ResourceNamingPolicy<VirtualRouter, DomainRouterVO> {

    public String getElasticLBName(Long lbId);

    public String getInternalLBName(Long lbId);

    public String getRouterName(Long routerId);

    public boolean isValidRouterName(String routerName);

}
