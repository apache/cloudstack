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
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.log4j.Logger;

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
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.Account;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;

@Local(value=TemplateAdapter.class)
public class HypervisorTemplateAdapter extends TemplateAdapterBase implements TemplateAdapter {
	private final static Logger s_logger = Logger.getLogger(HypervisorTemplateAdapter.class);
	@Inject DownloadMonitor _downloadMonitor;
	@Inject SecondaryStorageVmManager _ssvmMgr;
	@Inject AgentManager _agentMgr;

    @Inject DataStoreManager storeMgr;
    @Inject TemplateService imageService;
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
		// Check that the resource limit for secondary storage won't be exceeded
		_resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()),
		        ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
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
	        &&(!url.toLowerCase().endsWith("tar"))&&(!url.toLowerCase().endsWith("tar.zip"))
	        &&(!url.toLowerCase().endsWith("tar.bz2"))&&(!url.toLowerCase().endsWith("tar.gz"))
	        &&(!url.toLowerCase().endsWith("img"))&&(!url.toLowerCase().endsWith("raw"))){
	        throw new InvalidParameterValueException("Please specify a valid "+ cmd.getFormat().toLowerCase());
	    }

		if ((cmd.getFormat().equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith("vhd") && !url.toLowerCase().endsWith("vhd.zip") && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase().endsWith("vhd.gz") ))
			|| (cmd.getFormat().equalsIgnoreCase("qcow2") && (!url.toLowerCase().endsWith("qcow2") && !url.toLowerCase().endsWith("qcow2.zip") && !url.toLowerCase().endsWith("qcow2.bz2") && !url.toLowerCase().endsWith("qcow2.gz") ))
			|| (cmd.getFormat().equalsIgnoreCase("ova") && (!url.toLowerCase().endsWith("ova") && !url.toLowerCase().endsWith("ova.zip") && !url.toLowerCase().endsWith("ova.bz2") && !url.toLowerCase().endsWith("ova.gz")))
			|| (cmd.getFormat().equalsIgnoreCase("tar") && (!url.toLowerCase().endsWith("tar") && !url.toLowerCase().endsWith("tar.zip") && !url.toLowerCase().endsWith("tar.bz2") && !url.toLowerCase().endsWith("tar.gz")))
			|| (cmd.getFormat().equalsIgnoreCase("raw") && (!url.toLowerCase().endsWith("img") && !url.toLowerCase().endsWith("raw")))) {
	        throw new InvalidParameterValueException("Please specify a valid URL. URL:" + url + " is an invalid for the format " + cmd.getFormat().toLowerCase());
		}

		profile.setUrl(validateUrl(url));
		// Check that the resource limit for secondary storage won't be exceeded
		_resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()),
		        ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
		return profile;
	}

	@Override
	public VMTemplateVO create(TemplateProfile profile) {
	    // persist entry in vm_template, vm_template_details and template_zone_ref tables
		VMTemplateVO template = persistTemplate(profile);

		if (template == null) {
			throw new CloudRuntimeException("Unable to persist the template " + profile.getTemplate());
		}

		// find all eligible image stores for this zone scope
		List<DataStore> imageStores = this.storeMgr.getImageStoresByScope(new ZoneScope(profile.getZoneId()));
		if ( imageStores == null || imageStores.size() == 0 ){
		    throw new CloudRuntimeException("Unable to find image store to download template "+ profile.getTemplate());
		}
        for (DataStore imageStore : imageStores) {
            AsyncCallFuture<CommandResult> future = this.imageService
                    .createTemplateAsync(this.imageFactory.getTemplate(template.getId(), imageStore), imageStore);
            try {
                future.get();
            } catch (InterruptedException e) {
                s_logger.debug("create template Failed", e);
                throw new CloudRuntimeException("create template Failed", e);
            } catch (ExecutionException e) {
                s_logger.debug("create template Failed", e);
                throw new CloudRuntimeException("create template Failed", e);
            }
        }
        _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.template);
        _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.secondary_storage,
                UriUtils.getRemoteSize(profile.getUrl()));
        return template;
    }

	@Override @DB
	public boolean delete(TemplateProfile profile) {
		boolean success = true;

    	VMTemplateVO template = (VMTemplateVO)profile.getTemplate();

        // find all eligible image stores for this template
        List<DataStore> imageStores = this.templateMgr.getImageStoreByTemplate(template.getId(), profile.getZoneId());
        if ( imageStores == null || imageStores.size() == 0 ){
            throw new CloudRuntimeException("Unable to find image store to delete template "+ profile.getTemplate());
        }

        // Make sure the template is downloaded to all found image stores
        for (DataStore store : imageStores) {
            long storeId = store.getId();
            List<TemplateDataStoreVO> templateStores = _tmpltStoreDao.listByTemplateStore(template.getId(), storeId);
            for (TemplateDataStoreVO templateStore : templateStores) {
                if (templateStore.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
                    String errorMsg = "Please specify a template that is not currently being downloaded.";
                    s_logger.debug("Template: " + template.getName() + " is currently being downloaded to secondary storage host: " + store.getName() + "; cant' delete it.");
                    throw new CloudRuntimeException(errorMsg);
                }
            }
        }


        for (DataStore imageStore : imageStores) {
            s_logger.info("Delete template from image store: " + imageStore.getName());
            AsyncCallFuture<CommandResult> future = this.imageService
                    .deleteTemplateAsync(this.imageFactory.getTemplate(template.getId(), imageStore));
            try {
                CommandResult result = future.get();
                success = result.isSuccess();
                if ( !success )
                    break;
            } catch (InterruptedException e) {
                s_logger.debug("delete template Failed", e);
                throw new CloudRuntimeException("delete template Failed", e);
            } catch (ExecutionException e) {
                s_logger.debug("delete template Failed", e);
                throw new CloudRuntimeException("delete template Failed", e);
            }
        }

        if (success) {
            s_logger.info("Delete template from template table");
            // remove template from vm_templates table
            if (_tmpltDao.remove(template.getId())) {
                // Decrement the number of templates and total secondary storage
                // space used by the account
                Account account = _accountDao.findByIdIncludingRemoved(template.getAccountId());
                _resourceLimitMgr.decrementResourceCount(template.getAccountId(), ResourceType.template);
                _resourceLimitMgr.recalculateResourceCount(template.getAccountId(), account.getDomainId(),
                        ResourceType.secondary_storage.getOrdinal());
            }
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
