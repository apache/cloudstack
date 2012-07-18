package com.cloud.network.vpn;

public interface Site2SiteVpnManager extends Site2SiteVpnService {
    boolean cleanupVpnConnectionByVpc(long vpcId);
    boolean cleanupVpnGatewayByVpc(long vpcId);
}
