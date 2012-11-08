/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreManager;
import org.apache.cloudstack.storage.volume.db.VolumeDao;

import org.springframework.stereotype.Service;

import com.cloud.utils.db.DB;

@Service
public class VolumeServiceImpl implements VolumeService {
	@Inject
	VolumeDao volDao;
	@Inject
	PrimaryDataStoreManager dataStoreMgr;
	@Override
	public VolumeEntity createVolume(long volumeId, long dataStoreId, VolumeDiskType diskType) {
		PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(dataStoreId);
		return dataStore.createVolume(volumeId, diskType);
	}

	@DB
	@Override
	public boolean deleteVolume(long volumeId) {
		return true;
	}

	@Override
	public boolean cloneVolume(long volumeId, long baseVolId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean createVolumeFromSnapshot(long volumeId, long snapshotId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(long volumeId, long endpointId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean rokeAccess(long volumeId, long endpointId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VolumeEntity allocateVolumeInDb(long size, VolumeType type, String volName, Long templateId) {
		volDao.allocVolume(size, type, volName, templateId);
		return null;
	}

}
