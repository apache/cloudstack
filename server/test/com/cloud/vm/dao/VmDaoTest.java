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
package com.cloud.vm.dao;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.vm.jobs.VmWorkJobDao;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

import junit.framework.Assert;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/vmdaoTestContext.xml")
public class VmDaoTest extends TestCase {
	
	@Inject UserVmDao userVmDao;
	@Inject VMInstanceDao instanceDao;
	@Inject VmWorkJobDao workJobDao;
	
	@Before
	public void setup() {
		Transaction.open("Dummy");

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
	
	@After
	public void cleanup() {
		Transaction.currentTxn().close();
	}
	
	@Test
	public void testPowerStateUpdate() {
		UserVmVO userVmInstance = new UserVmVO(1L, "Dummy", "DummyInstance", 
				1L, HypervisorType.Any, 1L, true, false, 1L, 1L, 1L, null, null, null);
		userVmDao.persist(userVmInstance);
		
		userVmInstance = new UserVmVO(2L, "Dummy2", "DummyInstance2", 
				1L, HypervisorType.Any, 1L, true, false, 1L, 1L, 1L, null, null, null);
		userVmDao.persist(userVmInstance);
		
		VMInstanceVO instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getInstanceName().equals("Dummy"));
		
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOn);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 1);
		
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOn);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 2);
		
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOn);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 3);

		// after 3 times, the update count should stay at 3
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOn);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOn);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 3);
		
		// if power state is changed, the update count will be reset
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOff);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 1);

		// reset state tracking
		instanceDao.resetVmPowerStateTracking(1L);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerState() == VirtualMachine.PowerState.PowerOff);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 0);

		// update state tracking status twice (vm id: 1)
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 1);
		
		instanceDao.updatePowerState(1L, 1L, VirtualMachine.PowerState.PowerOff);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 2);

		// update state tracking (vm id: 2)
		instanceDao.updatePowerState(2L, 1L, VirtualMachine.PowerState.PowerOn);
		instanceDao.updatePowerState(2L, 1L, VirtualMachine.PowerState.PowerOn);
		instance = instanceDao.findById(2L);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 2);
		
		instanceDao.resetHostPowerStateTracking(1L);
		instance = instanceDao.findById(1L);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 0);
		instance = instanceDao.findById(2L);
		Assert.assertTrue(instance.getPowerStateUpdateCount() == 0);
		
		userVmDao.expunge(1L);
		userVmDao.expunge(2L);
	}
	
	@Test
	public void testVmWork() {
		VmWorkJobVO workJob = new VmWorkJobVO();
		workJob.setAccountId(1);
		workJob.setUserId(1L);
		workJob.setCmd("StartVM");
		workJob.setInitMsid(1L);
		workJob.setStep(VmWorkJobVO.Step.Prepare);
		workJob.setVmType(VirtualMachine.Type.ConsoleProxy);
		workJob.setVmInstanceId(1L);
		
		workJobDao.persist(workJob);

		VmWorkJobVO workJob2 = new VmWorkJobVO();
		workJob2.setAccountId(1);
		workJob2.setUserId(1L);
		workJob2.setCmd("StopVM");
		workJob2.setInitMsid(1L);
		workJob2.setStep(VmWorkJobVO.Step.Prepare);
		workJob2.setVmType(VirtualMachine.Type.ConsoleProxy);
		workJob2.setVmInstanceId(1L);
		
		workJobDao.persist(workJob2);
		
		VmWorkJobVO pendingWorkJob = workJobDao.findPendingWorkJob(VirtualMachine.Type.ConsoleProxy, 1L);
		Assert.assertTrue(pendingWorkJob.getCmd().equals("StartVM"));
		Assert.assertTrue(pendingWorkJob.getInitMsid() == 1);
		Assert.assertTrue(pendingWorkJob.getAccountId() == 1);
		Assert.assertTrue(pendingWorkJob.getStep() == VmWorkJobVO.Step.Prepare);
		
		workJobDao.updateStep(pendingWorkJob.getId(), VmWorkJobVO.Step.Starting);
		pendingWorkJob = workJobDao.findPendingWorkJob(VirtualMachine.Type.ConsoleProxy, 1L);
		Assert.assertTrue(pendingWorkJob.getStep() == VmWorkJobVO.Step.Starting);
	
		List<VmWorkJobVO> pendingStartJobs = workJobDao.listPendingWorkJobs(VirtualMachine.Type.ConsoleProxy, 1L, "StartVM");
		Assert.assertTrue(pendingStartJobs.size() == 1);
		
		workJobDao.expunge(workJob.getId());
		workJobDao.expunge(workJob2.getId());
	}
}
