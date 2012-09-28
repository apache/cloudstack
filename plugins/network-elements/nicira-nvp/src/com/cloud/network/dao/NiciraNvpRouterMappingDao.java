package com.cloud.network.dao;

import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.utils.db.GenericDao;

public interface NiciraNvpRouterMappingDao extends GenericDao<NiciraNvpRouterMappingVO, Long> {

	public NiciraNvpRouterMappingVO findByNetworkIdI(long id);
}
