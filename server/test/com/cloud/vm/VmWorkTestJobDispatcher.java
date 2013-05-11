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

import javax.inject.Inject;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.async.AsyncJob;
import com.cloud.async.AsyncJobDispatcher;
import com.cloud.async.AsyncJobManager;
import com.cloud.utils.component.AdapterBase;

public class VmWorkTestJobDispatcher extends AdapterBase implements AsyncJobDispatcher {

	@Inject AsyncJobManager _jobMgr;
	
	@Override
	public void runJob(AsyncJob job) {
		VmWorkJobVO workJob = new VmWorkJobVO();
    	
		workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
		workJob.setCmd(VmWorkConstants.VM_WORK_START);
		
		workJob.setAccountId(1L);
		workJob.setUserId(1L);
		workJob.setStep(VmWorkJobVO.Step.Starting);
		workJob.setVmType(VirtualMachine.Type.ConsoleProxy);
		workJob.setVmInstanceId(1L);

		// save work context info (there are some duplications)
		VmWorkStart workInfo = new VmWorkStart();
		workInfo.setAccountId(1L);
		workInfo.setUserId(1L);
		workInfo.setVmId(1L);
		workInfo.setPlan(null);
		workInfo.setParams(null);
		workJob.setCmdInfo(ApiSerializerHelper.toSerializedString(workInfo));
		
		_jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, 1L);
		
		_jobMgr.joinJob(job.getId(), workJob.getId(), "processVmStartWakeup", 
				VmWorkConstants.VM_WORK_JOB_WAKEUP_DISPATCHER, 
				new String[] {}, 
				3000, 120000);
	}
}
