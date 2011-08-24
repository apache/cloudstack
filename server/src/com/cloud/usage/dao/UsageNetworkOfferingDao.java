/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageNetworkOfferingVO;
import com.cloud.utils.db.GenericDao;

public interface UsageNetworkOfferingDao extends GenericDao<UsageNetworkOfferingVO, Long> {
	public void update(UsageNetworkOfferingVO usage);
	public List<UsageNetworkOfferingVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page);
}
