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

import com.cloud.storage.VolumeVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class VolumeManagerImpl implements VolumeManager {
	private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
	@Inject
	protected VolumeDao _volumeDao;
	

	public VolumeVO allocateDuplicateVolume(VolumeVO oldVol) {
		VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(), oldVol.getSize());
		newVol.setTemplateId(oldVol.getTemplateId());
		newVol.setDeviceId(oldVol.getDeviceId());
		newVol.setInstanceId(oldVol.getInstanceId());
		newVol.setRecreatable(oldVol.isRecreatable());
		newVol.setReservationId(oldVol.getReservationId());
		
		return _volumeDao.persist(newVol);
	}
	

	public VolumeVO processEvent(Volume vol, Volume.Event event) throws NoTransitionException {
		_volStateMachine.transitTo(vol, event, null, _volumeDao);
		return _volumeDao.findById(vol.getId());
	}
}
