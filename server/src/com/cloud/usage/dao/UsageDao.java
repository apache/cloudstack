/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.util.Date;
import java.util.List;

import com.cloud.event.UsageEventVO;
import com.cloud.exception.UsageServerException;
import com.cloud.usage.UsageVO;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

public interface UsageDao extends GenericDao<UsageVO, Long> {
    void deleteRecordsForAccount(Long accountId);
    List<UsageVO> searchAllRecords(SearchCriteria<UsageVO> sc, Filter filter);
    void saveAccounts(List<AccountVO> accounts) throws UsageServerException;
    void updateAccounts(List<AccountVO> accounts) throws UsageServerException;
    void saveUserStats(List<UserStatisticsVO> userStats) throws UsageServerException;
    void updateUserStats(List<UserStatisticsVO> userStats) throws UsageServerException;
    Long getLastAccountId() throws UsageServerException;
    Long getLastUserStatsId() throws UsageServerException;
    List<Long> listPublicTemplatesByAccount(long accountId);
}
