package com.cloud.network.dao;

import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.utils.db.GenericDao;

public interface Site2SiteVpnConnectionDao extends GenericDao<Site2SiteVpnConnectionVO, Long> {
    Site2SiteVpnConnectionVO findByCustomerGatewayId(long id);
    Site2SiteVpnConnectionVO findByVpnGatewayId(long id);
}
