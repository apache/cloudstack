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
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicVO;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> InstanceSearch;
    private final GenericSearchBuilder<NicVO, String> IpSearch;
    private final SearchBuilder<NicVO> NetworkSearch;
    private final GenericSearchBuilder<NicVO, Long> GarbageCollectSearch;
    
    protected NicDaoImpl() {
        super();
        
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instance", InstanceSearch.entity().getInstanceId(), Op.EQ);
        InstanceSearch.done();
        
        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("nc", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();
        
        NetworkSearch = createSearchBuilder();
        NetworkSearch.and("networkId", NetworkSearch.entity().getNetworkId(), Op.EQ);
        NetworkSearch.done();
        
        GarbageCollectSearch = createSearchBuilder(Long.class);
        GarbageCollectSearch.select(null, Func.DISTINCT, GarbageCollectSearch.entity().getNetworkId());
        GarbageCollectSearch.and("reservation", GarbageCollectSearch.entity().getReservationId(), Op.NULL);
        GarbageCollectSearch.groupBy(GarbageCollectSearch.entity().getNetworkId()).having(Func.COUNT, GarbageCollectSearch.entity().getId(), Op.EQ, null);
        GarbageCollectSearch.done();
    }
    
    @Override
    public void removeNicsForInstance(long instanceId) {
        SearchCriteria<NicVO> sc = InstanceSearch.create();
        sc.setParameters("instance", instanceId);
        remove(sc);
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
    
    @Override
    public List<Long> listNetworksWithNoActiveNics() {
        SearchCriteria<Long> sc = GarbageCollectSearch.create();
        
        return customSearch(sc, null);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = createSearchCriteria();
        sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return findOneBy(sc);
    }
}
