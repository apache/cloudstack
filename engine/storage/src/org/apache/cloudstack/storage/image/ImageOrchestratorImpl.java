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
package org.apache.cloudstack.storage.image;

import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.storage.manager.SecondaryStorageManager;

import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.utils.component.Inject;

public class ImageOrchestratorImpl implements ImageOrchestrator {
	@Inject
	SecondaryStorageManager _secStorageMgr;
	@Inject
	VMTemplateZoneDao _templateZoneDao;
	public void registerTemplate(long templateId) {
		List<VMTemplateZoneVO> tpZones = _templateZoneDao.listByTemplateId(templateId);
		
		for (VMTemplateZoneVO tpZone : tpZones) {
			DataStore imageStore = null;
			List<DataStore> imageStores = _secStorageMgr.getImageStores(tpZone.getZoneId());
			for (DataStore imgStore : imageStores) {
				TemplateStrategy ts = imgStore.getTemplateStrategy();
				if (ts.canRegister(templateId)) {
					imageStore = imgStore;
					break;
				}
			}
			
			if (imageStore == null) {
				continue;
			}
			
			TemplateStrategy ts = imageStore.getTemplateStrategy();
			ts.register(ts.get(templateId));
		}
	}

	public void registerSnapshot(long snapshotId) {
		// TODO Auto-generated method stub
		
	}

	public void registerVolume(long volumeId) {
		// TODO Auto-generated method stub
		
	}

	public void registerIso(long isoId) {
		// TODO Auto-generated method stub
		
	}


}
