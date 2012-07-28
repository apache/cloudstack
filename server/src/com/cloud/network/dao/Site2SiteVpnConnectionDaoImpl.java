package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.IPAddressVO;
import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={Site2SiteVpnConnectionDao.class})
public class Site2SiteVpnConnectionDaoImpl extends GenericDaoBase<Site2SiteVpnConnectionVO, Long> implements Site2SiteVpnConnectionDao {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnConnectionDaoImpl.class);

    protected final IPAddressDaoImpl _addrDao = ComponentLocator.inject(IPAddressDaoImpl.class);
    protected final Site2SiteVpnGatewayDaoImpl _vpnGatewayDao = ComponentLocator.inject(Site2SiteVpnGatewayDaoImpl.class);
    
    private final SearchBuilder<Site2SiteVpnConnectionVO> AllFieldsSearch;
    private final SearchBuilder<Site2SiteVpnConnectionVO> VpcSearch;
    private final SearchBuilder<Site2SiteVpnGatewayVO> VpnGatewaySearch;

    protected Site2SiteVpnConnectionDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("customerGatewayId", AllFieldsSearch.entity().getCustomerGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpnGatewayId", AllFieldsSearch.entity().getVpnGatewayId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        VpcSearch = createSearchBuilder();
        VpnGatewaySearch = _vpnGatewayDao.createSearchBuilder();
        VpnGatewaySearch.and("vpcId", VpnGatewaySearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        VpcSearch.join("vpnGatewaySearch", VpnGatewaySearch, VpnGatewaySearch.entity().getId(), VpcSearch.entity().getVpnGatewayId(), JoinType.INNER);
        VpcSearch.done();
    }
    
    @Override
    public List<Site2SiteVpnConnectionVO> listByCustomerGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("customerGatewayId", id);
        return listBy(sc);
    }
    
    @Override
    public List<Site2SiteVpnConnectionVO> listByVpnGatewayId(long id) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpnGatewayId", id);
        return listBy(sc);
    }

    @Override
    public List<Site2SiteVpnConnectionVO> listByVpcId(long vpcId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = VpcSearch.create();
        sc.setJoinParameters("vpnGatewaySearch", "vpcId", vpcId);
        return listBy(sc);
    }

    @Override
    public Site2SiteVpnConnectionVO findByVpnGatewayIdAndCustomerGatewayId(long vpnId, long customerId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpnGatewayId", vpnId);
        sc.setParameters("customerGatewayId", customerId);
        return findOneBy(sc);
    }

    @Override
    public Site2SiteVpnConnectionVO findByCustomerGatewayId(long customerId) {
        SearchCriteria<Site2SiteVpnConnectionVO> sc = AllFieldsSearch.create();
        sc.setParameters("customerGatewayId", customerId);
        return findOneBy(sc);
    }
}
