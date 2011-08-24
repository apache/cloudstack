/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageVMInstanceVO;
import com.cloud.utils.db.GenericDao;

public interface UsageVMInstanceDao extends GenericDao<UsageVMInstanceVO, Long> {
    public void update(UsageVMInstanceVO instance);
    public void delete(UsageVMInstanceVO instance);
    public List<UsageVMInstanceVO> getUsageRecords(long userId, Date startDate, Date endDate);
}
