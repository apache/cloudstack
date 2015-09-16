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
package org.apache.cloudstack.framework.jobs.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import org.apache.cloudstack.framework.jobs.impl.AsyncJobJoinMapVO;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

public class AsyncJobJoinMapDaoImpl extends GenericDaoBase<AsyncJobJoinMapVO, Long> implements AsyncJobJoinMapDao {

    private final SearchBuilder<AsyncJobJoinMapVO> RecordSearch;
    private final SearchBuilder<AsyncJobJoinMapVO> RecordSearchByOwner;
    private final SearchBuilder<AsyncJobJoinMapVO> CompleteJoinSearch;
    private final SearchBuilder<AsyncJobJoinMapVO> WakeupSearch;

//    private final GenericSearchBuilder<AsyncJobJoinMapVO, Long> JoinJobSearch;

    protected AsyncJobJoinMapDaoImpl() {
        RecordSearch = createSearchBuilder();
        RecordSearch.and("jobId", RecordSearch.entity().getJobId(), Op.EQ);
        RecordSearch.and("joinJobId", RecordSearch.entity().getJoinJobId(), Op.EQ);
        RecordSearch.done();

        RecordSearchByOwner = createSearchBuilder();
        RecordSearchByOwner.and("jobId", RecordSearchByOwner.entity().getJobId(), Op.EQ);
        RecordSearchByOwner.done();

        CompleteJoinSearch = createSearchBuilder();
        CompleteJoinSearch.and("joinJobId", CompleteJoinSearch.entity().getJoinJobId(), Op.EQ);
        CompleteJoinSearch.done();

        WakeupSearch = createSearchBuilder();
        WakeupSearch.and("nextWakeupTime", WakeupSearch.entity().getNextWakeupTime(), Op.LT);
        WakeupSearch.and("expiration", WakeupSearch.entity().getExpiration(), Op.GT);
        WakeupSearch.and("joinStatus", WakeupSearch.entity().getJoinStatus(), Op.EQ);
        WakeupSearch.done();

//        JoinJobSearch = createSearchBuilder(Long.class);
//        JoinJobSearch.and(JoinJobSearch.entity().getJoinJobId(), Op.SC, "joinJobId");
//        JoinJobSearch.done();
    }

    @Override
    public Long joinJob(long jobId, long joinJobId, long joinMsid, long wakeupIntervalMs, long expirationMs, Long syncSourceId, String wakeupHandler,
            String wakeupDispatcher) {

        AsyncJobJoinMapVO record = new AsyncJobJoinMapVO();
        record.setJobId(jobId);
        record.setJoinJobId(joinJobId);
        record.setJoinMsid(joinMsid);
        record.setJoinStatus(JobInfo.Status.IN_PROGRESS);
        record.setSyncSourceId(syncSourceId);
        record.setWakeupInterval(wakeupIntervalMs / 1000);        // convert millisecond to second
        record.setWakeupHandler(wakeupHandler);
        record.setWakeupDispatcher(wakeupDispatcher);
        if (wakeupHandler != null) {
            record.setNextWakeupTime(new Date(DateUtil.currentGMTTime().getTime() + wakeupIntervalMs));
            record.setExpiration(new Date(DateUtil.currentGMTTime().getTime() + expirationMs));
        }

        persist(record);
        return record.getId();
    }

    @Override
    public void disjoinJob(long jobId, long joinedJobId) {
        SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearch.create();
        sc.setParameters("jobId", jobId);
        sc.setParameters("joinJobId", joinedJobId);

        this.expunge(sc);
    }

    @Override
    public void disjoinAllJobs(long jobId) {
        SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearchByOwner.create();
        sc.setParameters("jobId", jobId);

        this.expunge(sc);
    }

    @Override
    public AsyncJobJoinMapVO getJoinRecord(long jobId, long joinJobId) {
        SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearch.create();
        sc.setParameters("jobId", jobId);
        sc.setParameters("joinJobId", joinJobId);

        List<AsyncJobJoinMapVO> result = this.listBy(sc);
        if (result != null && result.size() > 0) {
            assert (result.size() == 1);
            return result.get(0);
        }

        return null;
    }

    @Override
    public List<AsyncJobJoinMapVO> listJoinRecords(long jobId) {
        SearchCriteria<AsyncJobJoinMapVO> sc = RecordSearchByOwner.create();
        sc.setParameters("jobId", jobId);

        return this.listBy(sc);
    }

    @Override
    public void completeJoin(long joinJobId, JobInfo.Status joinStatus, String joinResult, long completeMsid) {
        AsyncJobJoinMapVO record = createForUpdate();
        record.setJoinStatus(joinStatus);
        record.setJoinResult(joinResult);
        record.setCompleteMsid(completeMsid);
        record.setLastUpdated(DateUtil.currentGMTTime());

        UpdateBuilder ub = getUpdateBuilder(record);

        SearchCriteria<AsyncJobJoinMapVO> sc = CompleteJoinSearch.create();
        sc.setParameters("joinJobId", joinJobId);
        update(ub, sc, null);
    }

//    @Override
//    public List<Long> wakeupScan() {
//        List<Long> standaloneList = new ArrayList<Long>();
//
//        Date cutDate = DateUtil.currentGMTTime();
//
//        TransactionLegacy txn = TransactionLegacy.currentTxn();
//        PreparedStatement pstmt = null;
//        try {
//            txn.start();
//
//            //
//            // performance sensitive processing, do it in plain SQL
//            //
//            String sql = "UPDATE async_job SET job_pending_signals=? WHERE id IN " +
//                    "(SELECT job_id FROM async_job_join_map WHERE next_wakeup < ? AND expiration > ?)";
//            pstmt = txn.prepareStatement(sql);
//            pstmt.setInt(1, AsyncJob.Constants.SIGNAL_MASK_WAKEUP);
//            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.executeUpdate();
//            pstmt.close();
//
//            sql = "UPDATE sync_queue_item SET queue_proc_msid=NULL, queue_proc_number=NULL WHERE content_id IN " +
//                    "(SELECT job_id FROM async_job_join_map WHERE next_wakeup < ? AND expiration > ?)";
//            pstmt = txn.prepareStatement(sql);
//            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.executeUpdate();
//            pstmt.close();
//
//            sql = "SELECT job_id FROM async_job_join_map WHERE next_wakeup < ? AND expiration > ? AND job_id NOT IN (SELECT content_id FROM sync_queue_item)";
//            pstmt = txn.prepareStatement(sql);
//            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            ResultSet rs = pstmt.executeQuery();
//            while(rs.next()) {
//                standaloneList.add(rs.getLong(1));
//            }
//            rs.close();
//            pstmt.close();
//
//            // update for next wake-up
//            sql = "UPDATE async_job_join_map SET next_wakeup=DATE_ADD(next_wakeup, INTERVAL wakeup_interval SECOND) WHERE next_wakeup < ? AND expiration > ?";
//            pstmt = txn.prepareStatement(sql);
//            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
//            pstmt.executeUpdate();
//            pstmt.close();
//
//            txn.commit();
//        } catch (SQLException e) {
//            logger.error("Unexpected exception", e);
//        }
//
//        return standaloneList;
//    }

    @Override
    public List<Long> findJobsToWake(long joinedJobId) {
        // TODO: We should fix this.  We shouldn't be crossing daos in a dao code.
        List<Long> standaloneList = new ArrayList<Long>();
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        String sql = "SELECT job_id FROM async_job_join_map WHERE join_job_id = ? AND job_id NOT IN (SELECT content_id FROM sync_queue_item)";
        try (PreparedStatement pstmt = txn.prepareStatement(sql)) {
            pstmt.setLong(1, joinedJobId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                standaloneList.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + sql, e);
        }
        return standaloneList;
    }

    @Override
    public List<Long> findJobsToWakeBetween(Date cutDate) {
        List<Long> standaloneList = new ArrayList<Long>();
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        String sql = "SELECT job_id FROM async_job_join_map WHERE next_wakeup < ? AND expiration > ? AND job_id NOT IN (SELECT content_id FROM sync_queue_item)";
        try (PreparedStatement pstmt = txn.prepareStatement(sql)) {
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                standaloneList.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to handle SQL exception", e);
        }

        // update for next wake-up
        sql = "UPDATE async_job_join_map SET next_wakeup=DATE_ADD(next_wakeup, INTERVAL wakeup_interval SECOND) WHERE next_wakeup < ? AND expiration > ?";
        try (PreparedStatement pstmt = txn.prepareStatement(sql)) {
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutDate));
            pstmt.executeUpdate();

            return standaloneList;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to handle SQL exception", e);
        }
    }

}
