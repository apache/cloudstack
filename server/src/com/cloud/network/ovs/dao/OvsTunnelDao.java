package com.cloud.network.ovs.dao;

import com.cloud.utils.db.GenericDao;

public interface OvsTunnelDao extends GenericDao<OvsTunnelVO, Long> {
	OvsTunnelVO lockByFromAndTo(long from, long to);
	OvsTunnelVO getByFromAndTo(long from, long to);
	int askKey(long from, long to);
}
