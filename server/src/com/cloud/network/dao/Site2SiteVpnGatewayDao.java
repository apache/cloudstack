package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.utils.db.GenericDao;

public interface Site2SiteVpnGatewayDao extends GenericDao<Site2SiteVpnGatewayVO, Long> {
    Site2SiteVpnGatewayVO findByIpAddrId(long id);
    List<Site2SiteVpnGatewayVO> listByVpcId(long vpcId);
}
