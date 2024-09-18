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

import com.cloud.network.vpc.Vpc;
import com.cloud.usage.UsageVpcVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Component
public class UsageVpcDaoImpl extends GenericDaoBase<UsageVpcVO, Long> implements UsageVpcDao {
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, vpc_id, zone_id, account_id, domain_id, state, created, removed FROM usage_vpc WHERE " +
            " account_id = ? AND ((removed IS NULL AND created <= ?) OR (created BETWEEN ? AND ?) OR (removed BETWEEN ? AND ?) " +
            " OR ((created <= ?) AND (removed >= ?)))";

    @Override
    public void update(UsageVpcVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageVpcVO> sc = this.createSearchCriteria();
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, usage.getVpcId());
            sc.addAnd("created", SearchCriteria.Op.EQ, usage.getCreated());
            UsageVpcVO vo = findOneBy(sc);
            if (vo != null) {
                vo.setRemoved(usage.getRemoved());
                update(vo.getId(), vo);
            }
        } catch (final Exception e) {
            logger.error(String.format("Error updating usage of VPC due to [%s].", e.getMessage()), e);
            txn.rollback();
        } finally {
            txn.close();
        }
    }

    @Override
    public void remove(long vpcId, Date removed) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageVpcVO> sc = this.createSearchCriteria();
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
            sc.addAnd("removed", SearchCriteria.Op.NULL);
            UsageVpcVO vo = findOneBy(sc);
            if (vo != null) {
                vo.setRemoved(removed);
                vo.setState(Vpc.State.Inactive.name());
                update(vo.getId(), vo);
            }
        } catch (final Exception e) {
            txn.rollback();
            logger.error(String.format("Error updating usage of VPC due to [%s].", e.getMessage()), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageVpcVO> getUsageRecords(Long accountId, Date startDate, Date endDate) {
        List<UsageVpcVO> usageRecords = new ArrayList<>();
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
                long vpcId = rs.getLong(2);
                long zoneId = rs.getLong(3);
                long acctId = rs.getLong(4);
                long domId = rs.getLong(5);
                String stateTS = rs.getString(6);
                Date createdDate = null;
                Date removedDate = null;
                String createdTS = rs.getString(7);
                String removedTS = rs.getString(8);

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (removedTS != null) {
                    removedDate = DateUtil.parseDateString(s_gmtTimeZone, removedTS);
                }
                usageRecords.add(new UsageVpcVO(id, vpcId, zoneId, acctId, domId, stateTS, createdDate, removedDate));
            }
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error getting VPC usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }
}
