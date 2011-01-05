package com.cloud.network.ovs.dao;

import com.cloud.utils.db.GenericDao;

public interface VlanMappingDirtyDao extends GenericDao<VlanMappingDirtyVO, Long> {
	public boolean isDirty(long accountId);
	public void markDirty(long accountId);
	public void clean(long accountId);
}
