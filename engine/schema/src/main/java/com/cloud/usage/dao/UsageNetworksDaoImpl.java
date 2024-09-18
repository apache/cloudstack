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
package com.cloud.usage.dao;

import com.cloud.network.Network;
import com.cloud.usage.UsageNetworksVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Component
public class UsageNetworksDaoImpl extends GenericDaoBase<UsageNetworksVO, Long> implements UsageNetworksDao {
    private static final Logger LOGGER = LogManager.getLogger(UsageNetworksDaoImpl.class);
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, network_id, network_offering_id, zone_id, account_id, domain_id, state, created, removed FROM usage_networks WHERE " +
            " account_id = ? AND ((removed IS NULL AND created <= ?) OR (created BETWEEN ? AND ?) OR (removed BETWEEN ? AND ?) " +
            " OR ((created <= ?) AND (removed >= ?)))";


    @Override
    public void update(long networkId, long newNetworkOffering, String state) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageNetworksVO> sc = this.createSearchCriteria();
            sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
            sc.addAnd("removed", SearchCriteria.Op.NULL);
            UsageNetworksVO vo = findOneBy(sc);
            if (vo != null) {
                vo.setNetworkOfferingId(newNetworkOffering);
                vo.setState(state);
                update(vo.getId(), vo);
            }
        } catch (final Exception e) {
            txn.rollback();
            LOGGER.error(String.format("Error updating usage of network due to [%s].", e.getMessage()), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public void remove(long networkId, Date removed) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageNetworksVO> sc = this.createSearchCriteria();
            sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
            sc.addAnd("removed", SearchCriteria.Op.NULL);
            UsageNetworksVO vo = findOneBy(sc);
            if (vo != null) {
                vo.setRemoved(removed);
                vo.setState(Network.State.Destroy.name());
                update(vo.getId(), vo);
            }
        } catch (final Exception e) {
            txn.rollback();
            LOGGER.error(String.format("Error updating usage of network due to [%s].", e.getMessage()), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageNetworksVO> getUsageRecords(Long accountId, Date startDate, Date endDate) {
        List<UsageNetworksVO> usageRecords = new ArrayList<>();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt;
        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(GET_USAGE_RECORDS_BY_ACCOUNT);
            pstmt.setLong(i++, accountId);

            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                long networkId = rs.getLong(2);
                long networkOfferingId = rs.getLong(3);
                long zoneId = rs.getLong(4);
                long acctId = rs.getLong(5);
                long domId = rs.getLong(6);
                String stateTS = rs.getString(7);
                Date createdDate = null;
                Date removedDate = null;
                String createdTS = rs.getString(8);
                String removedTS = rs.getString(9);

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (removedTS != null) {
                    removedDate = DateUtil.parseDateString(s_gmtTimeZone, removedTS);
                }
                usageRecords.add(new UsageNetworksVO(id, networkId, networkOfferingId, zoneId, acctId, domId, stateTS, createdDate, removedDate));
            }
        } catch (Exception e) {
            txn.rollback();
            LOGGER.warn("Error getting networks usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }
}
