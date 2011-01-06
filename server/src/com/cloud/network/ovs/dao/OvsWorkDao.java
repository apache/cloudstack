package com.cloud.network.ovs.dao;

import java.util.Date;
import java.util.List;

import com.cloud.network.ovs.dao.OvsWorkVO.Step;
import com.cloud.utils.db.GenericDao;

public interface OvsWorkDao extends GenericDao<OvsWorkVO, Long> {
	OvsWorkVO findByVmId(long vmId, boolean taken);

	OvsWorkVO findByVmIdStep(long vmId, Step step);

	OvsWorkVO take(long serverId);

	void updateStep(Long vmId, Long logSequenceNumber, Step done);

	void updateStep(Long workId, Step done);

	int deleteFinishedWork(Date timeBefore);

	List<OvsWorkVO> findUnfinishedWork(Date timeBefore);
}
