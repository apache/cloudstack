/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsagePortForwardingRuleVO;
import com.cloud.utils.db.GenericDao;

public interface UsagePortForwardingRuleDao extends GenericDao<UsagePortForwardingRuleVO, Long> {
	public void removeBy(long userId, long id);
	public void update(UsagePortForwardingRuleVO usage);
	public List<UsagePortForwardingRuleVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page);
}
