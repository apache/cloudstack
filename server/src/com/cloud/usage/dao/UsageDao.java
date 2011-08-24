/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
