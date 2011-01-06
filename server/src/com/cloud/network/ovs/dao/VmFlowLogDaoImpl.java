package com.cloud.network.ovs.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import javax.ejb.Local;

@Local(value={VmFlowLogDao.class})
public class VmFlowLogDaoImpl extends GenericDaoBase<VmFlowLogVO, Long>
		implements VmFlowLogDao {
	private SearchBuilder<VmFlowLogVO> VmIdSearch;

	@Override
	public VmFlowLogVO findByVmId(long vmId) {
		SearchCriteria<VmFlowLogVO> sc = VmIdSearch.create();
		sc.setParameters("vmId", vmId);
		return findOneIncludingRemovedBy(sc);
	}

	protected VmFlowLogDaoImpl() {
		VmIdSearch = createSearchBuilder();
		VmIdSearch.and("vmId", VmIdSearch.entity().getInstanceId(),
				SearchCriteria.Op.EQ);
		VmIdSearch.done();

	}
}
