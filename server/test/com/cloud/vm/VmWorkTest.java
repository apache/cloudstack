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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gson.Gson;

import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.vm.jobs.VmWorkJobDao;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO.Step;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.cluster.ClusterManager;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.utils.LogUtils;
import com.cloud.utils.Predicate;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Transaction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/VmWorkTestContext.xml")
public class VmWorkTest extends TestCase {
	@Inject AsyncJobManager _jobMgr;
	@Inject VirtualMachineManager _vmMgr;
    @Inject ClusterManager _clusterMgr;
    @Inject VmWorkJobDao _vmworkJobDao;
	
	Gson _gson = new Gson();
	
	@Before
	public void setup() {
		LogUtils.initLog4j("log4j-vmops.xml");
		
    	ComponentContext.initComponentsLifeCycle();
       	_vmMgr = Mockito.spy(_vmMgr);
       	Mockito.when(_clusterMgr.getManagementNodeId()).thenReturn(1L);
    	
    	Transaction.open("dummy");
    	
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
 	}
	
    @Override
    @After
    public void tearDown() {
    	Transaction.currentTxn().close();
    }
	
	@Test
	public void testDeployPlanSerialization() {
		DeploymentPlan plan = new DataCenterDeployment(1L);
		ExcludeList excludeList = new ExcludeList();
		
		excludeList.addCluster(1);
		plan.setAvoids(excludeList);
		
		String json = _gson.toJson(plan);
		DeploymentPlan planClone = _gson.fromJson(json, DataCenterDeployment.class);
		Assert.assertTrue(planClone.getDataCenterId() == plan.getDataCenterId());
	}
	
	@Test
	public void testVmWorkStart() {
		VmWorkStart work = new VmWorkStart();
		Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>();
		params.put(VirtualMachineProfile.Param.HaTag, "HA");
		params.put(VirtualMachineProfile.Param.ControlNic, new Long(100));
		work.setParams(params);
		
		VmWorkStart workClone = _gson.fromJson(_gson.toJson(work), VmWorkStart.class);
		Assert.assertTrue(work.getParams().size() == workClone.getParams().size());
		Assert.assertTrue(work.getParams().get(VirtualMachineProfile.Param.HaTag).equals(workClone.getParams().get(VirtualMachineProfile.Param.HaTag)));
	}
	
	public void testVmWorkDispatcher() {
        VmWorkJobVO workJob = new VmWorkJobVO(UUID.randomUUID().toString());
		workJob.setDispatcher("VmWorkJobDispatcher");
		workJob.setCmd("doVmWorkStart");
		workJob.setAccountId(1L);
		workJob.setUserId(2L);
		workJob.setStep(Step.Starting);
		workJob.setVmType(VirtualMachine.Type.ConsoleProxy);
		workJob.setVmInstanceId(1L);
		
		VmWorkStart workInfo = new VmWorkStart();
		workJob.setCmdInfo(ApiSerializerHelper.toSerializedString(workInfo));
		
		_jobMgr.submitAsyncJob(workJob, "VM", 1);
		
		_jobMgr.waitAndCheck(new String[] {"Done"}, 120000, 120000, new Predicate() {

			@Override
			public boolean checkCondition() {
				return true;
			}
		});
	}
	
	@Test
	public void testVmWorkWakeup() {
		AsyncJobVO mainJob = new AsyncJobVO();
		
		mainJob.setDispatcher("TestApiJobDispatcher");
		mainJob.setAccountId(1L);
		mainJob.setUserId(1L);
		mainJob.setCmd("Dummy");
		mainJob.setCmdInfo("Dummy");
		
		_jobMgr.submitAsyncJob(mainJob);
		
		try {
			Thread.sleep(120000);
		} catch (InterruptedException e) {
		}
	}
	
	@Test
	public void testExceptionSerialization() {
		InsufficientCapacityException exception = new InsufficientStorageCapacityException("foo", VmWorkJobVO.class, 1L);
		
		String encodedString = JobSerializerHelper.toObjectSerializedString(exception);
		System.out.println(encodedString);

		exception = (InsufficientCapacityException)JobSerializerHelper.fromObjectSerializedString(encodedString);
		Assert.assertTrue(exception.getScope() == VmWorkJobVO.class);
		Assert.assertTrue(exception.getMessage().equals("foo"));
	}
}
