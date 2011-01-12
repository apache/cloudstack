package com.cloud.network.ovs.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;

public interface GreTunnelDao extends GenericDao<GreTunnelVO, Long> {
	List<GreTunnelVO> getByFrom(long from);
	GreTunnelVO getByFromAndTo(long from, long To);
	GreTunnelVO lockByFromAndTo(long from, long to);
}
