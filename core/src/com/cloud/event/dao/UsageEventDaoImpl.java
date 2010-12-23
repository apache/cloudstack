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

package com.cloud.event.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.event.UsageEventVO;
import com.cloud.exception.UsageServerException;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={UsageEventDao.class})
public class UsageEventDaoImpl extends GenericDaoBase<UsageEventVO, Long> implements UsageEventDao {
    public static final Logger s_logger = Logger.getLogger(UsageEventDaoImpl.class.getName());

    private final SearchBuilder<UsageEventVO> latestEventsSearch;
    private final SearchBuilder<UsageEventVO> allEventsSearch;
    private static final String GET_LATEST_EVENT_DATE = "SELECT created FROM usage_event ORDER BY created DESC LIMIT 1";
    private static final String COPY_EVENTS = "INSERT INTO cloud_usage.usage_event SELECT id, type, account_id, created, zone_id, resource_id, resource_name, offering_id, template_id, size FROM cloud.usage_event vmevt WHERE vmevt.created > ? and vmevt.created <= ? ";
    private static final String COPY_ALL_EVENTS = "INSERT INTO cloud_usage.usage_event SELECT id, type, account_id, created, zone_id, resource_id, resource_name, offering_id, template_id, size FROM cloud.usage_event where created <= ? ";


    public UsageEventDaoImpl () {
        latestEventsSearch = createSearchBuilder();
        latestEventsSearch.and("recentEventDate", latestEventsSearch.entity().getCreateDate(), SearchCriteria.Op.GT);
        latestEventsSearch.and("enddate", latestEventsSearch.entity().getCreateDate(), SearchCriteria.Op.LTEQ);
        latestEventsSearch.done();

        allEventsSearch = createSearchBuilder();
        allEventsSearch.and("enddate", allEventsSearch.entity().getCreateDate(), SearchCriteria.Op.LTEQ);
        allEventsSearch.done();

    }

    @Override
    public List<UsageEventVO> listLatestEvents(Date recentEventDate, Date endDate) {
        Filter filter = new Filter(UsageEventVO.class, "createDate", Boolean.TRUE, null, null);
        SearchCriteria<UsageEventVO> sc = latestEventsSearch.create();
        sc.setParameters("recentEventDate", recentEventDate);
        sc.setParameters("enddate", endDate);
        return listBy(sc, filter);
    }

    @Override
    public List<UsageEventVO> listAllEvents(Date endDate) {
        Filter filter = new Filter(UsageEventVO.class, "createDate", Boolean.TRUE, null, null);
        SearchCriteria<UsageEventVO> sc = latestEventsSearch.create();
        sc.setParameters("enddate", endDate);
        return listBy(sc, filter);
    }

    @Override
    public List<UsageEventVO> getLatestEventDate() {
        Filter filter = new Filter(UsageEventVO.class, "createDate", Boolean.FALSE, null, 1L);
        return listAll(filter);
    }

    @Override
    public List<UsageEventVO> searchAllUsageEvents(SearchCriteria<UsageEventVO> sc, Filter filter) {
        return listIncludingRemovedBy(sc, filter);
    }


    public synchronized List<UsageEventVO> getRecentEvents(Date endDate) throws UsageServerException {
        Transaction txn = Transaction.open(Transaction.USAGE_DB);
        Date recentEventDate = getMostRecentEventDate();
        String sql = COPY_EVENTS;
        if (recentEventDate == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("no recent event date, copying all events");
            }
            sql = COPY_ALL_EVENTS;
        }

        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 1;
            if (recentEventDate != null) {
                pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), recentEventDate));
            }
            pstmt.setString(i, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.executeUpdate();
            txn.commit();
            return findRecentEvents(recentEventDate, endDate);
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("error copying events from cloud db to usage db", ex);
            throw new UsageServerException(ex.getMessage());
        } finally {
            txn.close();
        }
    }

    private Date getMostRecentEventDate() throws UsageServerException {
        Transaction txn = Transaction.open(Transaction.USAGE_DB);
        PreparedStatement pstmt = null;
        String sql = GET_LATEST_EVENT_DATE;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String mostRecentTimestampStr = rs.getString(1);
                if (mostRecentTimestampStr != null) {
                    return DateUtil.parseDateString(s_gmtTimeZone, mostRecentTimestampStr);
                }
            }
        } catch (Exception ex) {
            s_logger.error("error getting most recent event date", ex);
            throw new UsageServerException(ex.getMessage());
        } finally {
            txn.close();
        }
        return null;
    }

    private List<UsageEventVO> findRecentEvents(Date recentEventDate, Date endDate) throws UsageServerException {
        Transaction txn = Transaction.open(Transaction.USAGE_DB);
        try {
            int i = 1;
            if (recentEventDate == null) {
                return listAllEvents(endDate);
            } else {
                return listLatestEvents(recentEventDate, endDate);
            }
        } catch (Exception ex) {
            s_logger.error("error getting most recent event date", ex);
            throw new UsageServerException(ex.getMessage());
        } finally {
            txn.close();
        }
    }

}
