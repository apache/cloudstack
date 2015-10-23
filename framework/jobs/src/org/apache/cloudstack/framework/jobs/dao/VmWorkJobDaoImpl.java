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
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO.Step;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.VirtualMachine;

public class VmWorkJobDaoImpl extends GenericDaoBase<VmWorkJobVO, Long> implements VmWorkJobDao {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDaoImpl.class);

    protected SearchBuilder<VmWorkJobVO> PendingWorkJobSearch;
    protected SearchBuilder<VmWorkJobVO> PendingWorkJobByCommandSearch;
    protected SearchBuilder<VmWorkJobVO> ExpungingWorkJobSearch;

    @Inject
    protected AsyncJobDao _baseJobDao;

    public VmWorkJobDaoImpl() {
    }

    @PostConstruct
    public void init() {
        PendingWorkJobSearch = createSearchBuilder();
        PendingWorkJobSearch.and("jobStatus", PendingWorkJobSearch.entity().getStatus(), Op.EQ);
        PendingWorkJobSearch.and("vmType", PendingWorkJobSearch.entity().getVmType(), Op.EQ);
        PendingWorkJobSearch.and("vmInstanceId", PendingWorkJobSearch.entity().getVmInstanceId(), Op.EQ);
        PendingWorkJobSearch.done();

        PendingWorkJobByCommandSearch = createSearchBuilder();
        PendingWorkJobByCommandSearch.and("jobStatus", PendingWorkJobByCommandSearch.entity().getStatus(), Op.EQ);
        PendingWorkJobByCommandSearch.and("vmType", PendingWorkJobByCommandSearch.entity().getVmType(), Op.EQ);
        PendingWorkJobByCommandSearch.and("vmInstanceId", PendingWorkJobByCommandSearch.entity().getVmInstanceId(), Op.EQ);
        PendingWorkJobByCommandSearch.and("step", PendingWorkJobByCommandSearch.entity().getStep(), Op.NEQ);
        PendingWorkJobByCommandSearch.and("cmd", PendingWorkJobByCommandSearch.entity().getCmd(), Op.EQ);
        PendingWorkJobByCommandSearch.done();

        ExpungingWorkJobSearch = createSearchBuilder();
        ExpungingWorkJobSearch.and("jobStatus", ExpungingWorkJobSearch.entity().getStatus(), Op.NEQ);
        ExpungingWorkJobSearch.and("cutDate", ExpungingWorkJobSearch.entity().getLastUpdated(), Op.LT);
        ExpungingWorkJobSearch.and("dispatcher", ExpungingWorkJobSearch.entity().getDispatcher(), Op.EQ);
        ExpungingWorkJobSearch.done();
    }

    @Override
    public VmWorkJobVO findPendingWorkJob(VirtualMachine.Type type, long instanceId) {

        SearchCriteria<VmWorkJobVO> sc = PendingWorkJobSearch.create();
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        sc.setParameters("vmType", type);
        sc.setParameters("vmInstanceId", instanceId);

        Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
        List<VmWorkJobVO> result = this.listBy(sc, filter);
        if (result != null && result.size() > 0)
            return result.get(0);

        return null;
    }

    @Override
    public List<VmWorkJobVO> listPendingWorkJobs(VirtualMachine.Type type, long instanceId) {

        SearchCriteria<VmWorkJobVO> sc = PendingWorkJobSearch.create();
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        sc.setParameters("vmType", type);
        sc.setParameters("vmInstanceId", instanceId);

        Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
        return this.listBy(sc, filter);
    }

    @Override
    public List<VmWorkJobVO> listPendingWorkJobs(VirtualMachine.Type type, long instanceId, String jobCmd) {

        SearchCriteria<VmWorkJobVO> sc = PendingWorkJobByCommandSearch.create();
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        sc.setParameters("vmType", type);
        sc.setParameters("vmInstanceId", instanceId);
        sc.setParameters("cmd", jobCmd);

        Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
        return this.listBy(sc, filter);
    }

    @Override
    public void updateStep(long workJobId, Step step) {
        VmWorkJobVO jobVo = findById(workJobId);
        jobVo.setStep(step);
        jobVo.setLastUpdated(DateUtil.currentGMTTime());
        update(workJobId, jobVo);
    }

    @Override
    public void expungeCompletedWorkJobs(final Date cutDate) {
        // current DAO machenism does not support following usage
        /*
                SearchCriteria<VmWorkJobVO> sc = ExpungeWorkJobSearch.create();
                sc.setParameters("lastUpdated",cutDate);
                sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);

                expunge(sc);
        */

        // loop at application level to avoid mysql deadlock issues
        SearchCriteria<VmWorkJobVO> sc = ExpungingWorkJobSearch.create();
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        sc.setParameters("cutDate", cutDate);
        sc.setParameters("dispatcher", "VmWorkJobDispatcher");
        List<VmWorkJobVO> expungeList = listBy(sc);
        for (VmWorkJobVO job : expungeList) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Expunge completed work job-" + job.getId());
            expunge(job.getId());
            _baseJobDao.expunge(job.getId());
        }
    }

    @Override
    public void expungeLeftoverWorkJobs(final long msid) {
        // current DAO machenism does not support following usage
        /*
                SearchCriteria<VmWorkJobVO> sc = ExpungePlaceHolderWorkJobSearch.create();
                sc.setParameters("dispatcher", "VmWorkJobPlaceHolder");
                sc.setParameters("msid", msid);

                expunge(sc);
        */
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionLegacy txn = TransactionLegacy.currentTxn();

                try (
                        PreparedStatement pstmt = txn
                                .prepareAutoCloseStatement(
                            "DELETE FROM vm_work_job WHERE id IN (SELECT id FROM async_job WHERE (job_dispatcher='VmWorkJobPlaceHolder' OR job_dispatcher='VmWorkJobDispatcher') AND job_init_msid=?)");
                ) {
                    pstmt.setLong(1, msid);

                    pstmt.execute();
                } catch (SQLException e) {
                    s_logger.info("[ignored]"
                            + "SQL failed to delete vm work job: " + e.getLocalizedMessage());
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "caught an error during delete vm work job: " + e.getLocalizedMessage());
                }

                try (
                        PreparedStatement pstmt = txn.prepareAutoCloseStatement(
                            "DELETE FROM async_job WHERE (job_dispatcher='VmWorkJobPlaceHolder' OR job_dispatcher='VmWorkJobDispatcher') AND job_init_msid=?");
                ) {
                    pstmt.setLong(1, msid);

                    pstmt.execute();
                } catch (SQLException e) {
                    s_logger.info("[ignored]"
                            + "SQL failed to delete async job: " + e.getLocalizedMessage());
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "caught an error during delete async job: " + e.getLocalizedMessage());
                }
            }
        });
    }
}
