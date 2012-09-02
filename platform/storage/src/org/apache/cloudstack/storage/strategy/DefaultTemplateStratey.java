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
package org.apache.cloudstack.storage.strategy;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageEvent;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateStrategy;
import org.apache.cloudstack.storage.manager.TemplateManager;
import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultTemplateStratey implements TemplateStrategy {
	private static final Logger s_logger = Logger.getLogger(DefaultTemplateStratey.class);
	protected DataStore _ds;
	protected DataStoreDriver _driver;
	protected int _primaryStorageDownloadWait;
	protected int _installTries = 3;
    protected int _storagePoolMaxWaitSeconds = 3600;
    @Inject
    VMTemplatePoolDao _templatePoolDao;
    @Inject
    TemplateManager _templateMgr;
    
    public TemplateProfile get(long templateId) {
    	return _templateMgr.getProfile(templateId);
    }
    
	public TemplateProfile install(TemplateProfile tp) {
		DataStoreEndPointSelector dseps = _ds.getEndPointSelector();
		List<DataStoreEndPoint> eps = dseps.getEndPoints(StorageEvent.DownloadTemplateToPrimary);
		int tries = Math.min(eps.size(), _installTries);

		VMTemplateStoragePoolVO templateStoragePoolRef = _templatePoolDao.acquireInLockTable(tp.getTemplatePoolRefId(), _storagePoolMaxWaitSeconds);
		if (templateStoragePoolRef == null) {
			throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + tp.getTemplatePoolRefId());
		}

		try {
			for (int retry = 0; retry < tries; retry++) {
				Collections.shuffle(eps);
				DataStoreEndPoint ep = eps.get(0);
				try {
					tp = _driver.install(tp, ep);
					templateStoragePoolRef.setDownloadPercent(100);
					templateStoragePoolRef.setDownloadState(Status.DOWNLOADED);
					templateStoragePoolRef.setLocalDownloadPath(tp.getInstallPath());
					templateStoragePoolRef.setInstallPath(tp.getInstallPath());
					templateStoragePoolRef.setTemplateSize(tp.getTemplateSize());
					_templatePoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Template " + tp.getId() + " is installed via " + ep.getHostId());
					}
					return get(tp.getId());
				} catch (CloudRuntimeException e) {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Template " + tp.getId() + " download to pool " + _ds.getId() + " failed due to " + e.toString());          
					}
				}
			}
		} finally {
			_templatePoolDao.releaseFromLockTable(tp.getTemplatePoolRefId());
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Template " + tp.getId() + " is not found on and can not be downloaded to pool " + _ds.getId());
		}
		return null;
	}

}
