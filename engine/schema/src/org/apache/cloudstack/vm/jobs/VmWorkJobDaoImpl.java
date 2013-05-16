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
package org.apache.cloudstack.vm.jobs;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO.Step;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;

public class VmWorkJobDaoImpl extends GenericDaoBase<VmWorkJobVO, Long> implements VmWorkJobDao {

    protected SearchBuilder<VmWorkJobVO> PendingWorkJobSearch;
    protected SearchBuilder<VmWorkJobVO> PendingWorkJobByCommandSearch;
    protected SearchBuilder<VmWorkJobVO> ExpungeWorkJobSearch;
	
	public VmWorkJobDaoImpl() {
	}
	
	@PostConstruct
	public void init() {
		PendingWorkJobSearch = createSearchBuilder();
		PendingWorkJobSearch.and("vmType", PendingWorkJobSearch.entity().getVmType(), Op.EQ);
		PendingWorkJobSearch.and("vmInstanceId", PendingWorkJobSearch.entity().getVmInstanceId(), Op.EQ);
		PendingWorkJobSearch.and("step", PendingWorkJobSearch.entity().getStep(), Op.NEQ);
		PendingWorkJobSearch.done();

		PendingWorkJobByCommandSearch = createSearchBuilder();
		PendingWorkJobByCommandSearch.and("vmType", PendingWorkJobByCommandSearch.entity().getVmType(), Op.EQ);
		PendingWorkJobByCommandSearch.and("vmInstanceId", PendingWorkJobByCommandSearch.entity().getVmInstanceId(), Op.EQ);
		PendingWorkJobByCommandSearch.and("step", PendingWorkJobByCommandSearch.entity().getStep(), Op.NEQ);
		PendingWorkJobByCommandSearch.and("cmd", PendingWorkJobByCommandSearch.entity().getCmd(), Op.EQ);
		PendingWorkJobByCommandSearch.done();
		
		ExpungeWorkJobSearch = createSearchBuilder();
		ExpungeWorkJobSearch.and("lastUpdated", ExpungeWorkJobSearch.entity().getLastUpdated(), Op.LT);
		ExpungeWorkJobSearch.and("status", ExpungeWorkJobSearch.entity().getStatus(), Op.NEQ);
		ExpungeWorkJobSearch.done();
	}
	
	public VmWorkJobVO findPendingWorkJob(VirtualMachine.Type type, long instanceId) {
		
		SearchCriteria<VmWorkJobVO> sc = PendingWorkJobSearch.create();
		sc.setParameters("vmType", type);
		sc.setParameters("vmInstanceId", instanceId);
		sc.setParameters("step", Step.Done);
		
		Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
		List<VmWorkJobVO> result = this.listBy(sc, filter);
		if(result != null && result.size() > 0)
			return result.get(0);
		
		return null;
	}
	
	public List<VmWorkJobVO> listPendingWorkJobs(VirtualMachine.Type type, long instanceId) {
		
		SearchCriteria<VmWorkJobVO> sc = PendingWorkJobSearch.create();
		sc.setParameters("vmType", type);
		sc.setParameters("vmInstanceId", instanceId);
		sc.setParameters("step", Step.Done);
		
		Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
		return this.listBy(sc, filter);
	}

	public List<VmWorkJobVO> listPendingWorkJobs(VirtualMachine.Type type, long instanceId, String jobCmd) {
		
		SearchCriteria<VmWorkJobVO> sc = PendingWorkJobByCommandSearch.create();
		sc.setParameters("vmType", type);
		sc.setParameters("vmInstanceId", instanceId);
		sc.setParameters("step", Step.Done);
		sc.setParameters("cmd", jobCmd);
		
		Filter filter = new Filter(VmWorkJobVO.class, "created", true, null, null);
		return this.listBy(sc, filter);
	}
	
	public void updateStep(long workJobId, Step step) {
		VmWorkJobVO jobVo = findById(workJobId);
		jobVo.setStep(step);
		jobVo.setLastUpdated(DateUtil.currentGMTTime());
		update(workJobId, jobVo);
	}
	
	public void expungeCompletedWorkJobs(Date cutDate) {
		SearchCriteria<VmWorkJobVO> sc = ExpungeWorkJobSearch.create();
		sc.setParameters("lastUpdated",cutDate);
		sc.setParameters("status", AsyncJobConstants.STATUS_IN_PROGRESS);
		
		expunge(sc);
	}
}
