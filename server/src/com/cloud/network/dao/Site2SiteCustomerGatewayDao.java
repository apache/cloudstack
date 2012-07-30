package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.Site2SiteCustomerGatewayVO;
import com.cloud.utils.db.GenericDao;

public interface Site2SiteCustomerGatewayDao extends GenericDao<Site2SiteCustomerGatewayVO, Long> {
    Site2SiteCustomerGatewayVO findByGatewayIp(String ip);
    Site2SiteCustomerGatewayVO findByName(String name);
    List<Site2SiteCustomerGatewayVO> listByAccountId(long accountId);
}
