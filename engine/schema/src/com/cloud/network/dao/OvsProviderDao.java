package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.element.OvsProviderVO;
import com.cloud.utils.db.GenericDao;

public interface OvsProviderDao extends GenericDao<OvsProviderVO, Long> {
	public OvsProviderVO findByNspId(long nspId);

	public List<OvsProviderVO> listByEnabled(boolean enabled);

	public OvsProviderVO findByIdAndEnabled(long id, boolean enabled);
}
