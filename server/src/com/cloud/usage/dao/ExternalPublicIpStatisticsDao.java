/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.List;

import com.cloud.usage.ExternalPublicIpStatisticsVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.db.GenericDao;

public interface ExternalPublicIpStatisticsDao extends GenericDao<ExternalPublicIpStatisticsVO, Long> {
	
	ExternalPublicIpStatisticsVO lock(long accountId, long zoneId, String publicIpAddress);

    ExternalPublicIpStatisticsVO findBy(long accountId, long zoneId, String publicIpAddress);    
	
	List<ExternalPublicIpStatisticsVO> listBy(long accountId, long zoneId);
	
}
