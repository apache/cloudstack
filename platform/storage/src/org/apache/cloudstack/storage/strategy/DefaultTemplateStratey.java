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
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageEvent;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateStrategy;
import org.apache.cloudstack.storage.image.ImageManager;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.DownloadCommand.Proxy;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplateHostDao;
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
    VMTemplateHostDao _templateImageStoreDao;
    @Inject
    ImageManager _templateMgr;
    
    public DefaultTemplateStratey(DataStore ds) {
    	_ds = ds;
    }
    
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
					templateStoragePoolRef.setLocalDownloadPath(tp.getLocalPath());
					templateStoragePoolRef.setInstallPath(tp.getLocalPath());
					templateStoragePoolRef.setTemplateSize(tp.getSize());
					_templatePoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Template " + tp.getTemplateId() + " is installed via " + ep.getHostId());
					}
					return get(tp.getTemplateId());
				} catch (CloudRuntimeException e) {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Template " + tp.getTemplateId() + " download to pool " + _ds.getId() + " failed due to " + e.toString());          
					}
				}
			}
		} finally {
			_templatePoolDao.releaseFromLockTable(tp.getTemplatePoolRefId());
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Template " + tp.getTemplateId() + " is not found on and can not be downloaded to pool " + _ds.getId());
		}
		return null;
	}

	public TemplateProfile register(TemplateProfile tp) {
		
		VMTemplateHostVO vmTemplateHost = _templateImageStoreDao.findByHostTemplate(_ds.getId(), tp.getTemplateId());
		if (vmTemplateHost == null) {
			vmTemplateHost = new VMTemplateHostVO(_ds.getId(), tp.getTemplateId(), new Date(), 0, VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED, null, null, "jobid0000", null, tp.getUrl());
			_templateImageStoreDao.persist(vmTemplateHost);
		}
		
		DataStoreEndPointSelector dseps = _ds.getEndPointSelector();
		List<DataStoreEndPoint> eps = dseps.getEndPoints(StorageEvent.RegisterTemplate);
		
		Collections.shuffle(eps);
		DataStoreEndPoint ep = eps.get(0);
		_driver.register(tp, ep);

		return null;
	}
	
	protected boolean checkHypervisor(HypervisorType hypervisor) {
		StorageProvider sp = _ds.getProvider();
		List<HypervisorType> spHys = sp.supportedHypervisors();
		boolean checkHypervisor = false;
		for (HypervisorType hy : spHys) {
			if (hy == hypervisor) {
				checkHypervisor = true;
			}
		}
		return checkHypervisor;
	}
	
	protected boolean checkFormat(String url, String format) {
		if ((!url.toLowerCase().endsWith("vhd")) && (!url.toLowerCase().endsWith("vhd.zip")) && (!url.toLowerCase().endsWith("vhd.bz2")) && (!url.toLowerCase().endsWith("vhd.gz"))
				&& (!url.toLowerCase().endsWith("qcow2")) && (!url.toLowerCase().endsWith("qcow2.zip")) && (!url.toLowerCase().endsWith("qcow2.bz2")) && (!url.toLowerCase().endsWith("qcow2.gz"))
				&& (!url.toLowerCase().endsWith("ova")) && (!url.toLowerCase().endsWith("ova.zip")) && (!url.toLowerCase().endsWith("ova.bz2")) && (!url.toLowerCase().endsWith("ova.gz"))
				&& (!url.toLowerCase().endsWith("img")) && (!url.toLowerCase().endsWith("raw"))) {
			throw new InvalidParameterValueException("Please specify a valid " + format.toLowerCase());
		}

		if ((format.equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith("vhd") && !url.toLowerCase().endsWith("vhd.zip") && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase().endsWith(
				"vhd.gz")))
				|| (format.equalsIgnoreCase("qcow2") && (!url.toLowerCase().endsWith("qcow2") && !url.toLowerCase().endsWith("qcow2.zip") && !url.toLowerCase().endsWith("qcow2.bz2") && !url
						.toLowerCase().endsWith("qcow2.gz")))
				|| (format.equalsIgnoreCase("ova") && (!url.toLowerCase().endsWith("ova") && !url.toLowerCase().endsWith("ova.zip") && !url.toLowerCase().endsWith("ova.bz2") && !url.toLowerCase()
						.endsWith("ova.gz"))) || (format.equalsIgnoreCase("raw") && (!url.toLowerCase().endsWith("img") && !url.toLowerCase().endsWith("raw")))) {
			throw new InvalidParameterValueException("Please specify a valid URL. URL:" + url + " is an invalid for the format " + format.toLowerCase());
		}
		return true;
	}

	public boolean canRegister(long templateId) {
		TemplateProfile tp = get(templateId);
		
		if (!checkHypervisor(tp.getHypervisorType())) {
			return false;
		}
		
		if (!checkFormat(tp.getUrl(), tp.getFormat().toString())) {
			return false;
		}
		
		return true;
	}

	public int getDownloadWait() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getMaxTemplateSizeInBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Proxy getHttpProxy() {
		// TODO Auto-generated method stub
		return null;
	}

}
