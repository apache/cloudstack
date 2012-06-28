package com.cloud.network.dao;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={Site2SiteVpnConnectionDao.class})
public class Site2SiteVpnConnectionDaoImpl extends GenericDaoBase<Site2SiteVpnConnectionVO, Long> implements Site2SiteVpnConnectionDao {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnConnectionDaoImpl.class);

    private final SearchBuilder<Site2SiteVpnConnectionVO> AllFieldsSearch;

    protected Site2SiteVpnConnectionDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("customerGatewayId", AllFieldsSearch.entity().getCustomerGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpnGatewayId", AllFieldsSearch.entity().getVpnGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public Site2SiteVpnConnectionVO findByCustomerGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("customerGatewayId", id);
        return findOneBy(sc);
    }
    
    @Override
    public Site2SiteVpnConnectionVO findByVpnGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpnGatewayId", id);
        return findOneBy(sc);
    }
}
