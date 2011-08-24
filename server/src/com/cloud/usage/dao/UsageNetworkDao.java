/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Map;

import com.cloud.usage.UsageNetworkVO;
import com.cloud.utils.db.GenericDao;

public interface UsageNetworkDao extends GenericDao<UsageNetworkVO, Long> {
    Map<String, UsageNetworkVO> getRecentNetworkStats();
    void deleteOldStats(long maxEventTime);
}
