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
package com.cloud.storage.snapshot;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.commands.CreateSnapshotPolicyCmd;
import com.cloud.api.commands.DeleteSnapshotPoliciesCmd;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.commands.ListSnapshotPoliciesCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.Filter;

@Local(value = { SnapshotManager.class, SnapshotService.class })
public class MockSnapshotManagerImpl implements SnapshotManager, SnapshotService, Manager {

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Snapshot> listSnapshots(ListSnapshotsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteSnapshot(long snapshotId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SnapshotPolicy createPolicy(CreateSnapshotPolicyCmd cmd,
			Account policyOwner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends SnapshotSchedule> findRecurringSnapshotSchedule(
			ListRecurringSnapshotScheduleCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends SnapshotPolicy> listPoliciesforVolume(
			ListSnapshotPoliciesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Snapshot allocSnapshot(Long volumeId, Long policyId)
			throws ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Snapshot createSnapshot(Long volumeId, Long policyId,
			Long snapshotId, Account snapshotOwner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean backupSnapshotToSecondaryStorage(SnapshotVO snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void postCreateSnapshot(Long volumeId, Long snapshotId,
			Long policyId, boolean backedUp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean destroySnapshot(long userId, long snapshotId, long policyId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deletePolicy(long userId, Long policyId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SnapshotVO> listSnapsforVolume(long volumeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deletePoliciesForVolume(Long volumeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean deleteSnapshotDirsForAccount(long accountId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SnapshotPolicyVO getPolicyForVolume(long volumeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean destroySnapshotBackUp(long snapshotId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SnapshotVO createSnapshotOnPrimary(VolumeVO volume, Long polocyId,
			Long snapshotId) throws ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void downloadSnapshotsFromSwift(SnapshotVO ss) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HostVO getSecondaryStorageHost(SnapshotVO snapshot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSecondaryStorageURL(SnapshotVO snapshot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSnapshotsForVolume(String secondaryStoragePoolUrl,
			Long dcId, Long accountId, Long volumeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl,
			Long dcId, Long accountId, Long volumeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canOperateOnVolume(VolumeVO volume) {
		// TODO Auto-generated method stub
		return false;
	}
}
