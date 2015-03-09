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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVMInstanceVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {UsageVMInstanceDao.class})
public class UsageVMInstanceDaoImpl extends GenericDaoBase<UsageVMInstanceVO, Long> implements UsageVMInstanceDao {
    public static final Logger s_logger = Logger.getLogger(UsageVMInstanceDaoImpl.class.getName());

    protected static final String UPDATE_USAGE_INSTANCE_SQL = "UPDATE usage_vm_instance SET end_date = ? "
        + "WHERE account_id = ? and vm_instance_id = ? and usage_type = ? and end_date IS NULL";
    protected static final String DELETE_USAGE_INSTANCE_SQL = "DELETE FROM usage_vm_instance WHERE account_id = ? and vm_instance_id = ? and usage_type = ?";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT =
        "SELECT usage_type, zone_id, account_id, vm_instance_id, vm_name, cpu_speed, cpu_cores, memory, service_offering_id, template_id, hypervisor_type, start_date, end_date "
            + "FROM usage_vm_instance WHERE account_id = ? AND ((end_date IS NULL) OR (start_date BETWEEN ? AND ?) OR "
            + "      (end_date BETWEEN ? AND ?) OR ((start_date <= ?) AND (end_date >= ?)))";

    public UsageVMInstanceDaoImpl() {
    }

    @Override
    public void update(UsageVMInstanceVO instance) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = UPDATE_USAGE_INSTANCE_SQL;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), instance.getEndDate()));
            pstmt.setLong(2, instance.getAccountId());
            pstmt.setLong(3, instance.getVmInstanceId());
            pstmt.setInt(4, instance.getUsageType());
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            s_logger.warn(e);
        } finally {
            txn.close();
        }
    }

    @Override
    public void delete(UsageVMInstanceVO instance) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = DELETE_USAGE_INSTANCE_SQL;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, instance.getAccountId());
            pstmt.setLong(2, instance.getVmInstanceId());
            pstmt.setInt(3, instance.getUsageType());
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("error deleting usage vm instance with vmId: " + instance.getVmInstanceId() + ", for account with id: " + instance.getAccountId());
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageVMInstanceVO> getUsageRecords(long accountId, Date startDate, Date endDate) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        List<UsageVMInstanceVO> usageInstances = new ArrayList<UsageVMInstanceVO>();
        try {
            String sql = GET_USAGE_RECORDS_BY_ACCOUNT;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, accountId);
            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(4, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(5, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(6, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(7, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int r_usageType = rs.getInt(1);
                long r_zoneId = rs.getLong(2);
                long r_accountId = rs.getLong(3);
                long r_vmId = rs.getLong(4);
                String r_vmName = rs.getString(5);
                Long r_cpuSpeed = rs.getLong(6);
                if (rs.wasNull()) {
                    r_cpuSpeed = null;
                }
                Long r_cpuCores = rs.getLong(7);
                if (rs.wasNull()) {
                    r_cpuCores = null;
                }
                Long r_memory = rs.getLong(8);
                if (rs.wasNull()) {
                    r_memory = null;
                }
                long r_soId = rs.getLong(9);
                long r_tId = rs.getLong(10);
                String hypervisorType = rs.getString(11);
                String r_startDate = rs.getString(12);
                String r_endDate = rs.getString(13);
                Date instanceStartDate = null;
                Date instanceEndDate = null;
                if (r_startDate != null) {
                    instanceStartDate = DateUtil.parseDateString(s_gmtTimeZone, r_startDate);
                }
                if (r_endDate != null) {
                    instanceEndDate = DateUtil.parseDateString(s_gmtTimeZone, r_endDate);
                }
                UsageVMInstanceVO usageInstance =
                    new UsageVMInstanceVO(r_usageType, r_zoneId, r_accountId, r_vmId, r_vmName, r_soId, r_tId, r_cpuSpeed, r_cpuCores, r_memory, hypervisorType, instanceStartDate, instanceEndDate);
                usageInstances.add(usageInstance);
            }
        } catch (Exception ex) {
            s_logger.error("error retrieving usage vm instances for account id: " + accountId, ex);
        } finally {
            txn.close();
        }
        return usageInstances;
    }
}
