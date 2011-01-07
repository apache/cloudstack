package com.cloud.network.ovs.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import javax.ejb.Local;

@Local(value={VmFlowLogDao.class})
public class VmFlowLogDaoImpl extends GenericDaoBase<VmFlowLogVO, Long>
		implements VmFlowLogDao {
	private SearchBuilder<VmFlowLogVO> VmIdSearch;
	private SearchBuilder<VmFlowLogVO> VmNameSearch;

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
		
		VmNameSearch = createSearchBuilder();
		VmNameSearch.and("name", VmNameSearch.entity().getName(),
				SearchCriteria.Op.EQ);
		VmNameSearch.done();

	}

	@Override
	public VmFlowLogVO findOrNewByVmId(long vmId, String name) {
		VmFlowLogVO log = findByVmId(vmId);
		if (log == null) {
			log = new VmFlowLogVO(vmId, name);
			log = persist(log);
		}
		return log;
	}

	@Override
	public void deleteByVmId(long vmId) {
		SearchCriteria<VmFlowLogVO> sc = VmIdSearch.create();
		sc.setParameters("vmId", vmId);
		expunge(sc);
	}

	@Override
	public VmFlowLogVO findByName(String name) {
		SearchCriteria<VmFlowLogVO> sc = VmNameSearch.create();
		sc.setParameters("name", name);
		return findOneIncludingRemovedBy(sc);
	}
}
