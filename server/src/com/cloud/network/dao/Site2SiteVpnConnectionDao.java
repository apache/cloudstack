package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.utils.db.GenericDao;

public interface Site2SiteVpnConnectionDao extends GenericDao<Site2SiteVpnConnectionVO, Long> {
    List<Site2SiteVpnConnectionVO> listByCustomerGatewayId(long id);
    List<Site2SiteVpnConnectionVO> listByVpnGatewayId(long id);
    List<Site2SiteVpnConnectionVO> listByVpcId(long vpcId);
    Site2SiteVpnConnectionVO findByVpnGatewayIdAndCustomerGatewayId(long vpnId, long customerId);
    Site2SiteVpnConnectionVO findByCustomerGatewayId(long customerId);
}
