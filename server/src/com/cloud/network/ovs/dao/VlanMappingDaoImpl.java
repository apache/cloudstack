package com.cloud.network.ovs.dao;

import java.util.List;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

import javax.ejb.Local;

@Local(value = { VlanMappingDao.class })
public class VlanMappingDaoImpl extends GenericDaoBase<VlanMappingVO, Long>
		implements VlanMappingDao {
	protected final SearchBuilder<VlanMappingVO> AllFieldsSearch;
	
	public VlanMappingDaoImpl() {
		super();
		AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("host_id", AllFieldsSearch.entity().getHostId(), Op.EQ);
        AllFieldsSearch.and("account_id", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("vlan", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.done();
	}
	
	@Override
	public List<VlanMappingVO> listByAccountIdAndHostId(long accountId,
			long hostId) {
		SearchCriteria<VlanMappingVO> sc = AllFieldsSearch.create();
        sc.setParameters("account_id", accountId);
        sc.setParameters("host_id", hostId);
        return listBy(sc, null);
	}

	@Override
	public List<VlanMappingVO> listByHostId(long hostId) {
		SearchCriteria<VlanMappingVO> sc = AllFieldsSearch.create();
        sc.setParameters("host_id", hostId);
        
        return listBy(sc, null);
	}

	@Override
	public List<VlanMappingVO> listByAccountId(long accountId) {
		SearchCriteria<VlanMappingVO> sc = AllFieldsSearch.create();
        sc.setParameters("account_id", accountId);
        
        return listBy(sc, null);
	}
}
