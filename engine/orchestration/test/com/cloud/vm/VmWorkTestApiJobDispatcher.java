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

package com.cloud.vm;

import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.Transaction;

public class VmWorkTestApiJobDispatcher extends AdapterBase implements AsyncJobDispatcher {

	@Inject AsyncJobManager _jobMgr;
	
	@Override
    public void runJob(AsyncJob job) {
		
		// drop constraint check in order to do single table test
		Statement stat = null;
		try {
			stat = Transaction.currentTxn().getConnection().createStatement();
			stat.execute("SET foreign_key_checks = 0;");
		} catch (SQLException e) {
		} finally {
			if(stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
				}
			}
		}
		
        VmWorkJobVO workJob = new VmWorkJobVO(job.getRelated());
    	
		workJob.setDispatcher("TestWorkJobDispatcher");
        workJob.setCmd(VmWorkJobDispatcher.Start);
		
		workJob.setAccountId(1L);
		workJob.setUserId(1L);
		workJob.setStep(VmWorkJobVO.Step.Starting);
		workJob.setVmType(VirtualMachine.Type.ConsoleProxy);
		workJob.setVmInstanceId(1L);

		// save work context info (there are some duplications)
        VmWorkStart workInfo = new VmWorkStart(1L, 1L, 1L);
		workInfo.setPlan(null);
		workInfo.setParams(null);
        workJob.setCmdInfo(VmWorkJobDispatcher.serialize(workInfo));
		
        _jobMgr.submitAsyncJob(workJob, VmWorkJobDispatcher.VM_WORK_QUEUE, 1L);
		
		_jobMgr.joinJob(job.getId(), workJob.getId(), "processVmStartWakeup",
                VmWorkJobDispatcher.VM_WORK_JOB_WAKEUP_DISPATCHER,
				new String[] {},
				3000, 120000);
		AsyncJobExecutionContext.getCurrentExecutionContext().resetSyncSource();
	}
}
