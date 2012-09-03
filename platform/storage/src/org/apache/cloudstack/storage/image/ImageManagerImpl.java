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
import org.apache.log4j.Logger;

import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

public class ImageManagerImpl implements ImageManager {
	private static final Logger s_logger = Logger.getLogger(ImageManagerImpl.class);
	@Inject
	VMTemplateDao _templateDao;
	@Inject
	VMTemplatePoolDao _templatePoolDao;
	@Inject
	DataCenterDao _dcDao;
	
	public boolean contains(VirtualMachineTemplate template, DataStore ds) {
		long templateId = template.getId();
		long poolId = ds.getId();
		VMTemplateStoragePoolVO templateStoragePoolRef = null;
		templateStoragePoolRef = _templatePoolDao.findByPoolTemplate(poolId, templateId);
		return templateStoragePoolRef == null ? false : true;
	}
	
	public TemplateProfile AssociateTemplateStoragePool(TemplateProfile tp, DataStore ds) {
		long templateId = tp.getTemplateId();
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

	public TemplateProfile getProfile(long templateId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected TemplateProfile persistTemplate(TemplateProfile profile) {
		Long zoneId = profile.getZoneId();
		VMTemplateVO template = new VMTemplateVO(profile.getTemplateId(), profile.getName(), profile.getFormat(), profile.getIsPublic(),
				profile.getFeatured(), profile.getIsExtractable(), TemplateType.USER, profile.getUrl(), profile.getRequiresHVM(),
				profile.getBits(), profile.getAccountId(), profile.getCheckSum(), profile.getDisplayText(),
				profile.getPasswordEnabled(), profile.getGuestOsId(), profile.getBootable(), profile.getHypervisorType(), profile.getTemplateTag(), 
				profile.getDetails(), profile.getSshKeyEnabled());
        
		if (zoneId == null || zoneId.longValue() == -1) {
            List<DataCenterVO> dcs = _dcDao.listAll();
            
            if (dcs.isEmpty()) {
            	throw new CloudRuntimeException("No zones are present in the system, can't add template");
            }

            template.setCrossZones(true);
        	for (DataCenterVO dc: dcs) {
    			_templateDao.addTemplateToZone(template, dc.getId());
    		}
        } else {
			_templateDao.addTemplateToZone(template, zoneId);
        }

		return getProfile(template.getId());
	}
	
	protected boolean parameterCheck(RegisterTemplateCmd cmd) {
		Long zoneId = cmd.getZoneId();
		if (zoneId == -1) {
			zoneId = null;
		}

		ImageFormat imgfmt = ImageFormat.valueOf(cmd.getFormat().toUpperCase());
		if (imgfmt == null) {
			throw new IllegalArgumentException("Image format is incorrect " + cmd.getFormat() + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
		}
         
        // If a zoneId is specified, make sure it is valid
        if (zoneId != null) {
        	DataCenterVO zone = _dcDao.findById(zoneId);
        	if (zone == null) {
        		throw new IllegalArgumentException("Please specify a valid zone.");
        	}
        }
       
        List<VMTemplateVO> systemvmTmplts = _templateDao.listAllSystemVMTemplates();
        for (VMTemplateVO template : systemvmTmplts) {
            if (template.getName().equalsIgnoreCase(cmd.getTemplateName()) || template.getDisplayText().equalsIgnoreCase(cmd.getDisplayText())) {
                throw new IllegalArgumentException("Cannot use reserved names for templates");
            }
        }
        
        return true;
	}

	public TemplateProfile allocateTemplateInDB(RegisterTemplateCmd cmd) {
		parameterCheck(cmd);
		TemplateProfile tp = new TemplateProfile(cmd);
		return persistTemplate(tp);
	}
}
