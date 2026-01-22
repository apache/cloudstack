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

package org.apache.cloudstack.backup.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.backup.BackupScheduleVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.dao.VMInstanceDao;

public class BackupScheduleDaoImpl extends GenericDaoBase<BackupScheduleVO, Long> implements BackupScheduleDao {

    @Inject
    VMInstanceDao vmInstanceDao;

    private SearchBuilder<BackupScheduleVO> backupScheduleSearch;
    private SearchBuilder<BackupScheduleVO> executableSchedulesSearch;

    public BackupScheduleDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupScheduleSearch = createSearchBuilder();
        backupScheduleSearch.and("vm_id", backupScheduleSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        backupScheduleSearch.and("async_job_id", backupScheduleSearch.entity().getAsyncJobId(), SearchCriteria.Op.EQ);
        backupScheduleSearch.and("interval_type", backupScheduleSearch.entity().getScheduleType(), SearchCriteria.Op.EQ);
        backupScheduleSearch.done();

        executableSchedulesSearch = createSearchBuilder();
        executableSchedulesSearch.and("scheduledTimestamp", executableSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        executableSchedulesSearch.and("asyncJobId", executableSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        executableSchedulesSearch.done();
    }

    @Override
    public List<BackupScheduleVO> listByVM(Long vmId) {
        SearchCriteria<BackupScheduleVO> sc = backupScheduleSearch.create();
        sc.setParameters("vm_id", vmId);
        return listBy(sc, null);
    }

    @Override
    public BackupScheduleVO findByVMAndIntervalType(Long vmId, DateUtil.IntervalType intervalType) {
        SearchCriteria<BackupScheduleVO> sc = backupScheduleSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("interval_type", intervalType.ordinal());
        return findOneBy(sc);
    }

    @Override
    public List<BackupScheduleVO> getSchedulesToExecute(Date currentTimestamp) {
        SearchCriteria<BackupScheduleVO> sc = executableSchedulesSearch.create();
        sc.setParameters("scheduledTimestamp", currentTimestamp);
        return listBy(sc);
    }

    @DB
    @Override
    public boolean remove(Long id) {
        String sql = "UPDATE backups SET backup_schedule_id = NULL WHERE backup_schedule_id = ?";
        TransactionLegacy transaction = TransactionLegacy.currentTxn();
        try {
            PreparedStatement preparedStatement = transaction.prepareAutoCloseStatement(sql);
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
            return super.remove(id);
        } catch (SQLException e) {
            return false;
        }
    }
}
