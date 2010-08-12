/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.user.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={UserStatisticsDao.class})
public class UserStatisticsDaoImpl extends GenericDaoBase<UserStatisticsVO, Long> implements UserStatisticsDao {
    private static final Logger s_logger = Logger.getLogger(UserStatisticsDaoImpl.class);
    private static final String ACTIVE_AND_RECENTLY_DELETED_SEARCH = "SELECT us.id, us.data_center_id, us.account_id, us.net_bytes_received, us.net_bytes_sent, us.current_bytes_received, us.current_bytes_sent " +
                                                                     "FROM user_statistics us, account a " +
                                                                     "WHERE us.account_id = a.id AND (a.removed IS NULL OR a.removed >= ?) " +
                                                                     "ORDER BY us.id";
    private final SearchBuilder<UserStatisticsVO> UserDcSearch;
    private final SearchBuilder<UserStatisticsVO> UserSearch;
    
    public UserStatisticsDaoImpl() {
    	UserSearch = createSearchBuilder();
    	UserSearch.and("account", UserSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
    	UserSearch.done();

    	UserDcSearch = createSearchBuilder();
        UserDcSearch.and("account", UserDcSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        UserDcSearch.and("dc", UserDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        UserDcSearch.done();
    }
    
    @Override
    public UserStatisticsVO findBy(long accountId, long dcId) {
        SearchCriteria sc = UserDcSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        return findOneActiveBy(sc);
    }

    @Override
    public UserStatisticsVO lock(long accountId, long dcId) {
        SearchCriteria sc = UserDcSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        return lock(sc, true);
    }

    @Override
    public List<UserStatisticsVO> listBy(long accountId) {
        SearchCriteria sc = UserSearch.create();
        sc.setParameters("account", accountId);
        return search(sc, null);
    }

    @Override
    public List<UserStatisticsVO> listActiveAndRecentlyDeleted(Date minRemovedDate, int startIndex, int limit) {
        List<UserStatisticsVO> userStats = new ArrayList<UserStatisticsVO>();
        if (minRemovedDate == null) return userStats;

        Transaction txn = Transaction.currentTxn();
        try {
            String sql = ACTIVE_AND_RECENTLY_DELETED_SEARCH + " LIMIT " + startIndex + "," + limit;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), minRemovedDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                userStats.add(toEntityBean(rs, false));
            }
        } catch (Exception ex) {
            s_logger.error("error saving user stats to cloud_usage db", ex);
        }
        return userStats;
    }
}
