// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.event.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.event.UsageEventDetailsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {UsageEventDetailsDao.class})
public class UsageEventDetailsDaoImpl extends GenericDaoBase<UsageEventDetailsVO, Long> implements UsageEventDetailsDao {
    public static final Logger s_logger = Logger.getLogger(UsageEventDetailsDaoImpl.class.getName());

    private static final String EVENT_DETAILS_QUERY = "SELECT details.id, details.usage_event_id, details.name, details.value FROM `cloud`.`usage_event_details` details WHERE details.usage_event_id = ?";

    protected final SearchBuilder<UsageEventDetailsVO> EventDetailsSearch;
    protected final SearchBuilder<UsageEventDetailsVO> DetailSearch;

    public UsageEventDetailsDaoImpl() {

        EventDetailsSearch = createSearchBuilder();
        EventDetailsSearch.and("eventId", EventDetailsSearch.entity().getUsageEventId(), SearchCriteria.Op.EQ);
        EventDetailsSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("eventId", DetailSearch.entity().getUsageEventId(), SearchCriteria.Op.EQ);
        DetailSearch.and("key", DetailSearch.entity().getKey(), SearchCriteria.Op.EQ);
        DetailSearch.done();

    }

    @Override
    public void deleteDetails(long eventId) {
        SearchCriteria<UsageEventDetailsVO> sc = EventDetailsSearch.create();
        sc.setParameters("eventId", eventId);

        List<UsageEventDetailsVO> results = search(sc, null);
        for (UsageEventDetailsVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public UsageEventDetailsVO findDetail(long eventId, String key) {
        SearchCriteria<UsageEventDetailsVO> sc = DetailSearch.create();
        sc.setParameters("eventId", eventId);
        sc.setParameters("key", key);

        return findOneBy(sc);
    }

    @Override
    public Map<String, String> findDetails(long eventId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        Map<String, String> details = new HashMap<String, String>();
        try {
            conn = TransactionLegacy.getStandaloneConnection();

            pstmt = conn.prepareStatement(EVENT_DETAILS_QUERY);
            pstmt.setLong(1, eventId);
            resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                details.put(resultSet.getString(3), resultSet.getString(4));
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while executing SQL prepared statement", e);
        }  catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return details;
    }

    @Override
    public void persist(long eventId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<UsageEventDetailsVO> sc = EventDetailsSearch.create();
        sc.setParameters("eventId", eventId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            UsageEventDetailsVO vo = new UsageEventDetailsVO(eventId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

}
