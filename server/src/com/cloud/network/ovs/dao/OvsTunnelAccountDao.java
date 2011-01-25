package com.cloud.network.ovs.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;

public interface OvsTunnelAccountDao extends
		GenericDao<OvsTunnelAccountVO, Long> {
	OvsTunnelAccountVO getByFromToAccount(long from, long to, long account);
	void removeByFromAccount(long from, long account);
	void removeByFromToAccount(long from, long to, long account);
	List<OvsTunnelAccountVO> listByToAccount(long to, long account);
}
