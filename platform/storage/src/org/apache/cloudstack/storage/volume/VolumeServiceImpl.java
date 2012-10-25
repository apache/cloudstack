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

import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.utils.db.DB;

@Component
public class VolumeServiceImpl implements VolumeService {
	@Autowired
	VolumeDao _volumeDao;
	@Override
	public Volume createVolume(long volumeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@DB
	@Override
	public boolean deleteVolume(long volumeId) {
		VolumeVO vol = new VolumeVO(VolumeType.ROOT, "root", 1, 1,1 ,1,1);
		_volumeDao.persist(vol);
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
}
