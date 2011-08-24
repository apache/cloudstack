/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageIPAddressVO;
import com.cloud.utils.db.GenericDao;

public interface UsageIPAddressDao extends GenericDao<UsageIPAddressVO, Long> {
	public void removeBy(long userId, String address);
	public void update(UsageIPAddressVO usage);
	public List<UsageIPAddressVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, Long startIndex, Long pageSize);
}
