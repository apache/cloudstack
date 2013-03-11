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
package com.cloud.template;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;

import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.net.*;
import java.util.List;

@Local(value=TemplateAdapter.class)
public class HypervisorTemplateAdapter extends TemplateAdapterBase implements TemplateAdapter {
	private final static Logger s_logger = Logger.getLogger(HypervisorTemplateAdapter.class);
	@Inject DownloadMonitor _downloadMonitor;
	@Inject SecondaryStorageVmManager _ssvmMgr;
	@Inject AgentManager _agentMgr;

    @Inject DataStoreManager storeMgr;
    @Inject ImageService imageService;
    @Inject ImageDataFactory imageFactory;
    @Inject TemplateManager templateMgr;

    @Override
    public String getName() {
        return TemplateAdapterType.Hypervisor.getName();
    }
	
	private String validateUrl(String url) {
		try {
			URI uri = new URI(url);
			if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http")
				&& !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
				throw new IllegalArgumentException("Unsupported scheme for url: " + url);
			}

			int port = uri.getPort();
			if (!(port == 80 || port == 443 || port == -1)) {
				throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
			}
			String host = uri.getHost();
			try {
				InetAddress hostAddr = InetAddress.getByName(host);
				if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress()) {
					throw new IllegalArgumentException("Illegal host specified in url");
				}
				if (hostAddr instanceof Inet6Address) {
					throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
				}
			} catch (UnknownHostException uhe) {
				throw new IllegalArgumentException("Unable to resolve " + host);
			}

			return uri.toString();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL " + url);
		}
	}

	@Override
	public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException {
		TemplateProfile profile = super.prepare(cmd);
		String url = profile.getUrl();

		if((!url.toLowerCase().endsWith("iso"))&&(!url.toLowerCase().endsWith("iso.zip"))&&(!url.toLowerCase().endsWith("iso.bz2"))
        		&&(!url.toLowerCase().endsWith("iso.gz"))){
        	throw new InvalidParameterValueException("Please specify a valid iso");
        }

		profile.setUrl(validateUrl(url));
		return profile;
	}

	@Override
	public TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException {
		TemplateProfile profile = super.prepare(cmd);
		String url = profile.getUrl();

		if((!url.toLowerCase().endsWith("vhd"))&&(!url.toLowerCase().endsWith("vhd.zip"))
	        &&(!url.toLowerCase().endsWith("vhd.bz2"))&&(!url.toLowerCase().endsWith("vhd.gz"))
	        &&(!url.toLowerCase().endsWith("qcow2"))&&(!url.toLowerCase().endsWith("qcow2.zip"))
	        &&(!url.toLowerCase().endsWith("qcow2.bz2"))&&(!url.toLowerCase().endsWith("qcow2.gz"))
	        &&(!url.toLowerCase().endsWith("ova"))&&(!url.toLowerCase().endsWith("ova.zip"))
	        &&(!url.toLowerCase().endsWith("ova.bz2"))&&(!url.toLowerCase().endsWith("ova.gz"))
	        &&(!url.toLowerCase().endsWith("img"))&&(!url.toLowerCase().endsWith("raw"))){
	        throw new InvalidParameterValueException("Please specify a valid "+ cmd.getFormat().toLowerCase());
	    }

		if ((cmd.getFormat().equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith("vhd") && !url.toLowerCase().endsWith("vhd.zip") && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase().endsWith("vhd.gz") ))
			|| (cmd.getFormat().equalsIgnoreCase("qcow2") && (!url.toLowerCase().endsWith("qcow2") && !url.toLowerCase().endsWith("qcow2.zip") && !url.toLowerCase().endsWith("qcow2.bz2") && !url.toLowerCase().endsWith("qcow2.gz") ))
			|| (cmd.getFormat().equalsIgnoreCase("ova") && (!url.toLowerCase().endsWith("ova") && !url.toLowerCase().endsWith("ova.zip") && !url.toLowerCase().endsWith("ova.bz2") && !url.toLowerCase().endsWith("ova.gz")))
			|| (cmd.getFormat().equalsIgnoreCase("raw") && (!url.toLowerCase().endsWith("img") && !url.toLowerCase().endsWith("raw")))) {
	        throw new InvalidParameterValueException("Please specify a valid URL. URL:" + url + " is an invalid for the format " + cmd.getFormat().toLowerCase());
		}

		profile.setUrl(validateUrl(url));
		return profile;
	}

	@Override
	public VMTemplateVO create(TemplateProfile profile) {
		VMTemplateVO template = persistTemplate(profile);

		if (template == null) {
			throw new CloudRuntimeException("Unable to persist the template " + profile.getTemplate());
		}

		DataStore imageStore = this.storeMgr.getDataStore(profile.getImageStoreId(), DataStoreRole.Image);
		
		AsyncCallFuture<CommandResult> future = this.imageService.createTemplateAsync(this.imageFactory.getTemplate(template.getId()), imageStore);
		try {
            future.get();
        } catch (InterruptedException e) {
            s_logger.debug("create template Failed", e);
            throw new CloudRuntimeException("create template Failed", e);
        } catch (ExecutionException e) {
            s_logger.debug("create template Failed", e);
            throw new CloudRuntimeException("create template Failed", e);
        }
		_resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.template);

        return template;
	}

	@Override @DB
	public boolean delete(TemplateProfile profile) {
		boolean success = true;

    	VMTemplateVO template = (VMTemplateVO)profile.getTemplate();
    	Long zoneId = profile.getZoneId();
    	Long templateId = template.getId();

    	String zoneName;
    	List<HostVO> secondaryStorageHosts;
    	if (!template.isCrossZones() && zoneId != null) {
    		DataCenterVO zone = _dcDao.findById(zoneId);
    		zoneName = zone.getName();
    		secondaryStorageHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(zoneId);
    	} else {
    		zoneName = "(all zones)";
    		secondaryStorageHosts = _ssvmMgr.listSecondaryStorageHostsInAllZones();
    	}

    	s_logger.debug("Attempting to mark template host refs for template: " + template.getName() + " as destroyed in zone: " + zoneName);

		// Make sure the template is downloaded to all the necessary secondary storage hosts
		for (HostVO secondaryStorageHost : secondaryStorageHosts) {
			long hostId = secondaryStorageHost.getId();
			List<VMTemplateHostVO> templateHostVOs = _tmpltHostDao.listByHostTemplate(hostId, templateId);
			for (VMTemplateHostVO templateHostVO : templateHostVOs) {
				if (templateHostVO.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
					String errorMsg = "Please specify a template that is not currently being downloaded.";
					s_logger.debug("Template: " + template.getName() + " is currently being downloaded to secondary storage host: " + secondaryStorageHost.getName() + "; cant' delete it.");
					throw new CloudRuntimeException(errorMsg);
				}
			}
		}

		Account account = _accountDao.findByIdIncludingRemoved(template.getAccountId());
		String eventType = "";

		if (template.getFormat().equals(ImageFormat.ISO)){
			eventType = EventTypes.EVENT_ISO_DELETE;
		} else {
			eventType = EventTypes.EVENT_TEMPLATE_DELETE;
		}

		// Iterate through all necessary secondary storage hosts and mark the template on each host as destroyed
		for (HostVO secondaryStorageHost : secondaryStorageHosts) {
			long hostId = secondaryStorageHost.getId();
			long sZoneId = secondaryStorageHost.getDataCenterId();
			List<VMTemplateHostVO> templateHostVOs = _tmpltHostDao.listByHostTemplate(hostId, templateId);
			for (VMTemplateHostVO templateHostVO : templateHostVOs) {
				VMTemplateHostVO lock = _tmpltHostDao.acquireInLockTable(templateHostVO.getId());
				try {
					if (lock == null) {
						s_logger.debug("Failed to acquire lock when deleting templateHostVO with ID: " + templateHostVO.getId());
						success = false;
						break;
					}
					UsageEventUtils.publishUsageEvent(eventType, account.getId(), sZoneId, templateId, null, null, null);
                    templateHostVO.setDestroyed(true);
					_tmpltHostDao.update(templateHostVO.getId(), templateHostVO);
                    String installPath = templateHostVO.getInstallPath();
                    List<UserVmVO> userVmUsingIso = _userVmDao.listByIsoId(templateId);
                    //check if there is any VM using this ISO.
                    if (userVmUsingIso == null || userVmUsingIso.isEmpty()) {
                    if (installPath != null) {
                        Answer answer = _agentMgr.sendToSecStorage(secondaryStorageHost, new DeleteTemplateCommand(secondaryStorageHost.getStorageUrl(), installPath));

                        if (answer == null || !answer.getResult()) {
                            s_logger.debug("Failed to delete " + templateHostVO + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
                        } else {
                            _tmpltHostDao.remove(templateHostVO.getId());
                            s_logger.debug("Deleted template at: " + installPath);
                        }
                    } else {
                        _tmpltHostDao.remove(templateHostVO.getId());
                    }
                    }
					VMTemplateZoneVO templateZone = _tmpltZoneDao.findByZoneTemplate(sZoneId, templateId);

					if (templateZone != null) {
						_tmpltZoneDao.remove(templateZone.getId());
					}
				} finally {
					if (lock != null) {
						_tmpltHostDao.releaseFromLockTable(lock.getId());
					}
				}
			}

			if (!success) {
				break;
			}
		}

		s_logger.debug("Successfully marked template host refs for template: " + template.getName() + " as destroyed in zone: " + zoneName);

		// If there are no more non-destroyed template host entries for this template, delete it
		if (success && (_tmpltHostDao.listByTemplateId(templateId).size() == 0)) {
			long accountId = template.getAccountId();

			VMTemplateVO lock = _tmpltDao.acquireInLockTable(templateId);

			try {
				if (lock == null) {
					s_logger.debug("Failed to acquire lock when deleting template with ID: " + templateId);
					success = false;
				} else if (_tmpltDao.remove(templateId)) {
					// Decrement the number of templates
				    _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.template);
				}

			} finally {
				if (lock != null) {
					_tmpltDao.releaseFromLockTable(lock.getId());
				}
			}

			s_logger.debug("Removed template: " + template.getName() + " because all of its template host refs were marked as destroyed.");
		}

		return success;
	}

	public TemplateProfile prepareDelete(DeleteTemplateCmd cmd) {
		TemplateProfile profile = super.prepareDelete(cmd);
		VMTemplateVO template = (VMTemplateVO)profile.getTemplate();
		Long zoneId = profile.getZoneId();

		if (template.getTemplateType() == TemplateType.SYSTEM) {
			throw new InvalidParameterValueException("The DomR template cannot be deleted.");
		}

		if (zoneId != null && (_ssvmMgr.findSecondaryStorageHost(zoneId) == null)) {
			throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
		}

		return profile;
	}

	public TemplateProfile prepareDelete(DeleteIsoCmd cmd) {
		TemplateProfile profile = super.prepareDelete(cmd);
		Long zoneId = profile.getZoneId();

		if (zoneId != null && (_ssvmMgr.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}

		return profile;
	}
}
