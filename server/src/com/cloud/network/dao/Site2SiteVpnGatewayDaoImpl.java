package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={Site2SiteVpnGatewayDao.class})
public class Site2SiteVpnGatewayDaoImpl extends GenericDaoBase<Site2SiteVpnGatewayVO, Long> implements Site2SiteVpnGatewayDao {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnGatewayDaoImpl.class);
    
    private final SearchBuilder<Site2SiteVpnGatewayVO> AllFieldsSearch;

    protected Site2SiteVpnGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("addrId", AllFieldsSearch.entity().getAddrId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public Site2SiteVpnGatewayVO findByIpAddrId(long id) {
        SearchCriteria<Site2SiteVpnGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("addrId", id);
        return findOneBy(sc);
    }
}
