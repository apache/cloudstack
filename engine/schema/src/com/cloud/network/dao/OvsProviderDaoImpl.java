package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.element.OvsProviderVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = OvsProviderDao.class)
@DB()
public class OvsProviderDaoImpl extends GenericDaoBase<OvsProviderVO, Long>
		implements OvsProviderDao {
	final SearchBuilder<OvsProviderVO> AllFieldsSearch;

	public OvsProviderDaoImpl() {
		super();
		AllFieldsSearch = createSearchBuilder();
		AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("nsp_id", AllFieldsSearch.entity().getNspId(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("enabled", AllFieldsSearch.entity().isEnabled(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.done();
	}

	@Override
	public OvsProviderVO findByNspId(long nspId) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("nsp_id", nspId);
		return findOneBy(sc);
	}

	@Override
	public List<OvsProviderVO> listByEnabled(boolean enabled) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("enabled", enabled);
		return listBy(sc);
	}

	@Override
	public OvsProviderVO findByIdAndEnabled(long id, boolean enabled) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("id", id);
		sc.setParameters("enabled", enabled);
		return findOneBy(sc);
	}
}
