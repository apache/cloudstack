package com.cloud.network.dao;

import javax.ejb.Local;

import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=NiciraNvpRouterMappingDao.class)
public class NiciraNvpRouterMappingDaoImpl extends GenericDaoBase<NiciraNvpRouterMappingVO, Long> implements NiciraNvpRouterMappingDao {

	protected final SearchBuilder<NiciraNvpRouterMappingVO> networkSearch;
	
	public NiciraNvpRouterMappingDaoImpl() {
		networkSearch = createSearchBuilder();
		networkSearch.and("network_id", networkSearch.entity().getNetworkId(), Op.EQ);
		networkSearch.done();
	}
	
	@Override
	public NiciraNvpRouterMappingVO findByNetworkIdI(long id) {
		SearchCriteria<NiciraNvpRouterMappingVO> sc = networkSearch.create();
		sc.setParameters("network_id", id);
		return findOneBy(sc);
	}
	

}
