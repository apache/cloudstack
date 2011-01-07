package com.cloud.network.ovs.dao;

import com.cloud.utils.db.GenericDao;

public interface VmFlowLogDao extends GenericDao<VmFlowLogVO, Long> {
	VmFlowLogVO findByVmId(long vmId);
	VmFlowLogVO findOrNewByVmId(long vmId, String name);
	VmFlowLogVO findByName(String name);
	void deleteByVmId(long vmId);
}
