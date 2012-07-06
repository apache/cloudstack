package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.IPAddressVO;
import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={Site2SiteVpnGatewayDao.class})
public class Site2SiteVpnGatewayDaoImpl extends GenericDaoBase<Site2SiteVpnGatewayVO, Long> implements Site2SiteVpnGatewayDao {
    protected final IPAddressDaoImpl _addrDao = ComponentLocator.inject(IPAddressDaoImpl.class);
    
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnGatewayDaoImpl.class);
    
    private final SearchBuilder<Site2SiteVpnGatewayVO> AllFieldsSearch;
    private final SearchBuilder<Site2SiteVpnGatewayVO> VpcSearch;
    private final SearchBuilder<IPAddressVO> AddrSearch;

    protected Site2SiteVpnGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("addrId", AllFieldsSearch.entity().getAddrId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        VpcSearch = createSearchBuilder();
        AddrSearch = _addrDao.createSearchBuilder();
        AddrSearch.and("vpcId", AddrSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        VpcSearch.join("addrSearch", AddrSearch, AddrSearch.entity().getId(), VpcSearch.entity().getAddrId(), JoinType.INNER);
        VpcSearch.done();
    }
    
    @Override
    public Site2SiteVpnGatewayVO findByIpAddrId(long id) {
        SearchCriteria<Site2SiteVpnGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("addrId", id);
        return findOneBy(sc);
    }

    @Override
    public List<Site2SiteVpnGatewayVO> listByVpcId(long vpcId) {
        SearchCriteria<Site2SiteVpnGatewayVO> sc = VpcSearch.create();
        sc.setJoinParameters("addrSearch", "vpcId", vpcId);
        return listBy(sc);
    }
}
