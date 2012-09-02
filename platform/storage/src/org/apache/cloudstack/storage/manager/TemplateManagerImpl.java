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
package org.apache.cloudstack.storage.manager;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateProfile;
import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

public class TemplateManagerImpl implements TemplateManager {
	private static final Logger s_logger = Logger.getLogger(TemplateManagerImpl.class);
	@Inject
	VMTemplateDao _templateDao;
	@Inject
	VMTemplatePoolDao _templatePoolDao;
	
	public boolean contains(VirtualMachineTemplate template, DataStore ds) {
		long templateId = template.getId();
		long poolId = ds.getId();
		VMTemplateStoragePoolVO templateStoragePoolRef = null;
		templateStoragePoolRef = _templatePoolDao.findByPoolTemplate(poolId, templateId);
		return templateStoragePoolRef == null ? false : true;
	}
	
	public TemplateProfile AssociateTemplateStoragePool(TemplateProfile tp, DataStore ds) {
		long templateId = tp.getId();
		long poolId = ds.getId();
		VMTemplateStoragePoolVO templateStoragePoolRef = null;
		long templateStoragePoolRefId;
		
		templateStoragePoolRef = _templatePoolDao.findByPoolTemplate(poolId, templateId);
		if (templateStoragePoolRef != null) {
			templateStoragePoolRef.setMarkedForGC(false);
			_templatePoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);

			if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Template " + templateId + " has already been downloaded to pool " + poolId);
				}

				tp.setLocalPath(templateStoragePoolRef.getInstallPath());
				tp.setTemplatePoolRefId(templateStoragePoolRef.getId());
				return tp;
			}
		}
		
		if (templateStoragePoolRef == null) {
			templateStoragePoolRef = new VMTemplateStoragePoolVO(poolId, templateId);
			try {
				templateStoragePoolRef = _templatePoolDao.persist(templateStoragePoolRef);
				templateStoragePoolRefId =  templateStoragePoolRef.getId();
			} catch (Exception e) {
				s_logger.debug("Assuming we're in a race condition: " + e.getMessage());
				templateStoragePoolRef = _templatePoolDao.findByPoolTemplate(poolId, templateId);
				if (templateStoragePoolRef == null) {
					throw new CloudRuntimeException("Unable to persist a reference for pool " + poolId + " and template " + templateId);
				}
				templateStoragePoolRefId = templateStoragePoolRef.getId();
			}
		} else {
			templateStoragePoolRefId = templateStoragePoolRef.getId();
		}
		tp.setTemplatePoolRefId(templateStoragePoolRefId);
		return tp;
	}
}
