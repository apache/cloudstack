/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageVolumeVO;
import com.cloud.utils.db.GenericDao;

public interface UsageVolumeDao extends GenericDao<UsageVolumeVO, Long> {
	public void removeBy(long userId, long id);
	public void update(UsageVolumeVO usage);
	public List<UsageVolumeVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page);
}
