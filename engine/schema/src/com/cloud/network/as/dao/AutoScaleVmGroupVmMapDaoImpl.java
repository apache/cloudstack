package com.cloud.network.as.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={AutoScaleVmGroupVmMapDao.class})
public class AutoScaleVmGroupVmMapDaoImpl extends GenericDaoBase<AutoScaleVmGroupVmMapVO, Long> implements AutoScaleVmGroupVmMapDao {

	@Override
	public Integer countByGroup(long vmGroupId) {

		SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
		sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
		return getCount(sc);
	}

	@Override
	public List<AutoScaleVmGroupVmMapVO> listByGroup(long vmGroupId) {
		SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
		sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
		return listBy(sc);
	}

	@Override
	public int remove(long vmGroupId, long vmId) {
		SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
		sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
		sc.addAnd("vmId", SearchCriteria.Op.EQ, vmId);
		return remove(sc);
	}

}
