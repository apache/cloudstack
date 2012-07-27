package com.cloud.network.vpn;

import java.util.List;

import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.vm.DomainRouterVO;

public interface Site2SiteVpnManager extends Site2SiteVpnService {
    boolean cleanupVpnConnectionByVpc(long vpcId);
    boolean cleanupVpnGatewayByVpc(long vpcId);
    void markDisconnectVpnConnByVpc(long vpcId);
    List<Site2SiteVpnConnectionVO> getConnectionsForRouter(DomainRouterVO router);
}
