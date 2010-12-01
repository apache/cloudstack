/**
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.vm.NicVO;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> InstanceSearch;
    private final GenericSearchBuilder<NicVO, String> IpSearch;
    private final SearchBuilder<NicVO> NetworkSearch;
    
    protected NicDaoImpl() {
        super();
        
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instance", InstanceSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceSearch.done();
        
        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("nc", IpSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), SearchCriteria.Op.NNULL);
        IpSearch.done();
        
        NetworkSearch = createSearchBuilder();
        NetworkSearch.and("networkId", NetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkSearch.done();
    }
    
    @Override
    public List<NicVO> listBy(long instanceId) {
        SearchCriteria<NicVO> sc = InstanceSearch.create();
        sc.setParameters("instance", instanceId);
        return listBy(sc);
    }
    
    @Override
    public List<String> listIpAddressInNetworkConfiguration(long networkConfigId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("nc", networkConfigId);
        return customSearch(sc, null);
    }
    
    @Override
    public List<NicVO> listByNetworkId(long networkId) {
        SearchCriteria<NicVO> sc = NetworkSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }
}
