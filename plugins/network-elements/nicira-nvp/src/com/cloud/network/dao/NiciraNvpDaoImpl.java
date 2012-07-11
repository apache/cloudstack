package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=NiciraNvpDao.class)
public class NiciraNvpDaoImpl extends GenericDaoBase<NiciraNvpDeviceVO, Long>
        implements NiciraNvpDao {
    
    protected final SearchBuilder<NiciraNvpDeviceVO> physicalNetworkIdSearch;
    
    public NiciraNvpDaoImpl() {
        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkIdSearch.done();
    }

    @Override
    public List<NiciraNvpDeviceVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<NiciraNvpDeviceVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

}
