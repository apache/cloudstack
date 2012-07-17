package com.cloud.network.dao;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={Site2SiteVpnGatewayDao.class})
public class Site2SiteVpnGatewayDaoImpl extends GenericDaoBase<Site2SiteVpnGatewayVO, Long> implements Site2SiteVpnGatewayDao {
    protected final IPAddressDaoImpl _addrDao = ComponentLocator.inject(IPAddressDaoImpl.class);
    
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnGatewayDaoImpl.class);
    
    private final SearchBuilder<Site2SiteVpnGatewayVO> AllFieldsSearch;

    protected Site2SiteVpnGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }
    
    @Override
    public Site2SiteVpnGatewayVO findByVpcId(long vpcId) {
        SearchCriteria<Site2SiteVpnGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }
}
