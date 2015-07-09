//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.cloudstack.quota.QuotaJobVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = { QuotaJobDao.class })
public class QuotaJobDaoImpl extends GenericDaoBase<QuotaJobVO, Long> implements QuotaJobDao {
    private static final Logger s_logger = Logger.getLogger(QuotaJobDaoImpl.class.getName());

    private static final String GET_LAST_JOB_SUCCESS_DATE_MILLIS = "SELECT end_millis FROM cloud_usage.usage_job WHERE end_millis > 0 and success = 1 ORDER BY end_millis DESC LIMIT 1";

    @Override
    public long getLastJobSuccessDateMillis() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_LAST_JOB_SUCCESS_DATE_MILLIS;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception ex) {
            s_logger.error("error getting last usage job success date", ex);
        } finally {
            txn.close();
        }
        return 0L;
    }

    @Override
    public void updateJobSuccess(Long jobId, long startMillis, long endMillis, long execTime, boolean success) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();

            QuotaJobVO job = lockRow(jobId, Boolean.TRUE);
            QuotaJobVO jobForUpdate = createForUpdate();
            jobForUpdate.setStartMillis(startMillis);
            jobForUpdate.setEndMillis(endMillis);
            jobForUpdate.setExecTime(execTime);
            jobForUpdate.setStartDate(new Date(startMillis));
            jobForUpdate.setEndDate(new Date(endMillis));
            jobForUpdate.setSuccess(success);
            update(job.getId(), jobForUpdate);

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("error updating job success date", ex);
            throw new CloudRuntimeException(ex.getMessage());
        } finally {
            txn.close();
        }
    }

    @Override
    public Long checkHeartbeat(String hostname, int pid, int aggregationDuration) {
        QuotaJobVO job = getNextRecurringJob();
        if (job == null) {
            return null;
        }

        if (job.getHost().equals(hostname) && (job.getPid() != null) && (job.getPid().intValue() == pid)) {
            return job.getId();
        }

        Date lastHeartbeat = job.getHeartbeat();
        if (lastHeartbeat == null) {
            return null;
        }

        long sinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeat.getTime();

        // TODO: Make this check a little smarter..but in the mean time we want
        // the mgmt
        // server to monitor the usage server, we need to make sure other usage
        // servers take over as the usage job owner more aggressively. For now
        // this is hardcoded to 5 minutes.
        if (sinceLastHeartbeat > (5 * 60 * 1000)) {
            return job.getId();
        }
        return null;
    }

    @Override
    public QuotaJobVO isOwner(String hostname, int pid) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            if ((hostname == null) || (pid <= 0)) {
                return null;
            }

            QuotaJobVO job = getLastJob();
            if (job == null) {
                return null;
            }

            if (hostname.equals(job.getHost()) && (job.getPid() != null) && (pid == job.getPid().intValue())) {
                return job;
            }
        } finally {
            txn.close();
        }
        return null;
    }

    @Override
    public void createNewJob(String hostname, int pid, int jobType) {
        QuotaJobVO newJob = new QuotaJobVO();
        newJob.setHost(hostname);
        newJob.setPid(pid);
        newJob.setHeartbeat(new Date());
        newJob.setJobType(jobType);
        persist(newJob);
    }

    @Override
    public QuotaJobVO getLastJob() {
        Filter filter = new Filter(QuotaJobVO.class, "id", false, Long.valueOf(0), Long.valueOf(1));
        SearchCriteria<QuotaJobVO> sc = createSearchCriteria();
        sc.addAnd("endMillis", SearchCriteria.Op.EQ, Long.valueOf(0));
        List<QuotaJobVO> jobs = search(sc, filter);

        if ((jobs == null) || jobs.isEmpty()) {
            return null;
        }
        return jobs.get(0);
    }

    private QuotaJobVO getNextRecurringJob() {
        Filter filter = new Filter(QuotaJobVO.class, "id", false, Long.valueOf(0), Long.valueOf(1));
        SearchCriteria<QuotaJobVO> sc = createSearchCriteria();
        sc.addAnd("endMillis", SearchCriteria.Op.EQ, Long.valueOf(0));
        sc.addAnd("jobType", SearchCriteria.Op.EQ, Integer.valueOf(QuotaJobVO.JOB_TYPE_RECURRING));
        List<QuotaJobVO> jobs = search(sc, filter);

        if ((jobs == null) || jobs.isEmpty()) {
            return null;
        }
        return jobs.get(0);
    }

    @Override
    public QuotaJobVO getNextImmediateJob() {
        Filter filter = new Filter(QuotaJobVO.class, "id", false, Long.valueOf(0), Long.valueOf(1));
        SearchCriteria<QuotaJobVO> sc = createSearchCriteria();
        sc.addAnd("endMillis", SearchCriteria.Op.EQ, Long.valueOf(0));
        sc.addAnd("jobType", SearchCriteria.Op.EQ, Integer.valueOf(QuotaJobVO.JOB_TYPE_SINGLE));
        sc.addAnd("scheduled", SearchCriteria.Op.EQ, Integer.valueOf(0));
        List<QuotaJobVO> jobs = search(sc, filter);

        if ((jobs == null) || jobs.isEmpty()) {
            return null;
        }
        return jobs.get(0);
    }

    @Override
    public Date getLastHeartbeat() {
        Filter filter = new Filter(QuotaJobVO.class, "heartbeat", false, Long.valueOf(0), Long.valueOf(1));
        SearchCriteria<QuotaJobVO> sc = createSearchCriteria();
        List<QuotaJobVO> jobs = search(sc, filter);

        if ((jobs == null) || jobs.isEmpty()) {
            return null;
        }
        return jobs.get(0).getHeartbeat();
    }
}
