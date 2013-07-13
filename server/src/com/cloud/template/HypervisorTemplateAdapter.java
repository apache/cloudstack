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

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.PrepareOVAPackingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.user.Account;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=TemplateAdapter.class)
public class HypervisorTemplateAdapter extends TemplateAdapterBase {
    private final static Logger s_logger = Logger.getLogger(HypervisorTemplateAdapter.class);
    @Inject DownloadMonitor _downloadMonitor;
    @Inject AgentManager _agentMgr;

    @Inject DataStoreManager storeMgr;
    @Inject TemplateService imageService;
    @Inject TemplateDataFactory imageFactory;
    @Inject TemplateManager templateMgr;
    @Inject AlertManager alertMgr;
    @Inject VMTemplateZoneDao templateZoneDao;
    @Inject
    EndPointSelector _epSelector;

    @Override
    public String getName() {
        return TemplateAdapterType.Hypervisor.getName();
    }


    @Override
    public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException {
        TemplateProfile profile = super.prepare(cmd);
        String url = profile.getUrl();

        if((!url.toLowerCase().endsWith("iso"))&&(!url.toLowerCase().endsWith("iso.zip"))&&(!url.toLowerCase().endsWith("iso.bz2"))
                &&(!url.toLowerCase().endsWith("iso.gz"))){
            throw new InvalidParameterValueException("Please specify a valid iso");
        }

        UriUtils.validateUrl(url);
        profile.setUrl(url);
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

        UriUtils.validateUrl(url);
        profile.setUrl(url);
        // Check that the resource limit for secondary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()),
                ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
        return profile;
    }

    @Override
    public VMTemplateVO create(TemplateProfile profile) {
        // persist entry in vm_template, vm_template_details and template_zone_ref tables, not that entry at template_store_ref is not created here, and created in createTemplateAsync.
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
            TemplateInfo tmpl = this.imageFactory.getTemplate(template.getId(), imageStore);
            CreateTemplateContext<TemplateApiResult> context = new CreateTemplateContext<TemplateApiResult>(null, tmpl);
            AsyncCallbackDispatcher<HypervisorTemplateAdapter, TemplateApiResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createTemplateAsyncCallBack(null, null));
            caller.setContext(context);
            this.imageService.createTemplateAsync(tmpl, imageStore, caller);
        }
        _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.template);

        return template;
    }

	private class CreateTemplateContext<T> extends AsyncRpcContext<T> {
		final TemplateInfo template;
		public CreateTemplateContext(AsyncCompletionCallback<T> callback, TemplateInfo template) {
			super(callback);
			this.template = template;
		}
	}

    protected Void createTemplateAsyncCallBack(AsyncCallbackDispatcher<HypervisorTemplateAdapter, TemplateApiResult> callback,
            CreateTemplateContext<TemplateApiResult> context) {
        TemplateApiResult result = callback.getResult();
        TemplateInfo template = context.template;
        if (result.isFailed()) {
            // failed in creating template, we need to remove those already
            // populated template entry
            _tmpltDao.remove(template.getId());
        } else {
            VMTemplateVO tmplt = this._tmpltDao.findById(template.getId());
            long accountId = tmplt.getAccountId();
            if (template.getSize() != null) {
                _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.secondary_storage, template.getSize());
            }
        }

        return null;
    }

    @Override @DB
    public boolean delete(TemplateProfile profile) {
        boolean success = true;

        VMTemplateVO template = profile.getTemplate();

        // find all eligible image stores for this template
        List<DataStore> imageStores = this.templateMgr.getImageStoreByTemplate(template.getId(), profile.getZoneId());
        if (imageStores == null || imageStores.size() == 0) {
            // already destroyed on image stores
            s_logger.info("Unable to find image store still having template: " + template.getName()
                    + ", so just mark the template removed");
        } else {
            // Make sure the template is downloaded to all found image stores
            for (DataStore store : imageStores) {
                long storeId = store.getId();
                List<TemplateDataStoreVO> templateStores = _tmpltStoreDao
                        .listByTemplateStore(template.getId(), storeId);
                for (TemplateDataStoreVO templateStore : templateStores) {
                    if (templateStore.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
                        String errorMsg = "Please specify a template that is not currently being downloaded.";
                        s_logger.debug("Template: " + template.getName()
                                + " is currently being downloaded to secondary storage host: " + store.getName()
                                + "; cant' delete it.");
                        throw new CloudRuntimeException(errorMsg);
                    }
                }
            }

            String eventType = "";
            if (template.getFormat().equals(ImageFormat.ISO)) {
                eventType = EventTypes.EVENT_ISO_DELETE;
            } else {
                eventType = EventTypes.EVENT_TEMPLATE_DELETE;
            }

            for (DataStore imageStore : imageStores) {
                // publish zone-wide usage event
                Long sZoneId = ((ImageStoreEntity) imageStore).getDataCenterId();
                if (sZoneId != null) {
                    UsageEventUtils.publishUsageEvent(eventType, template.getAccountId(), sZoneId, template.getId(),
                            null, null, null);
                }

                s_logger.info("Delete template from image store: " + imageStore.getName());
                AsyncCallFuture<TemplateApiResult> future = this.imageService.deleteTemplateAsync(this.imageFactory
                        .getTemplate(template.getId(), imageStore));
                try {
                    TemplateApiResult result = future.get();
                    success = result.isSuccess();
                    if (!success) {
                        s_logger.warn("Failed to delete the template " + template + 
                                " from the image store: " + imageStore.getName() + " due to: " + result.getResult());
                        break;
                    }

                    // remove from template_zone_ref
                    List<VMTemplateZoneVO> templateZones = templateZoneDao
                            .listByZoneTemplate(sZoneId, template.getId());
                    if (templateZones != null) {
                        for (VMTemplateZoneVO templateZone : templateZones) {
                            templateZoneDao.remove(templateZone.getId());
                        }
                    }
                } catch (InterruptedException e) {
                    s_logger.debug("delete template Failed", e);
                    throw new CloudRuntimeException("delete template Failed", e);
                } catch (ExecutionException e) {
                    s_logger.debug("delete template Failed", e);
                    throw new CloudRuntimeException("delete template Failed", e);
                }
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

    @Override
    public TemplateProfile prepareDelete(DeleteTemplateCmd cmd) {
        TemplateProfile profile = super.prepareDelete(cmd);
        VMTemplateVO template = profile.getTemplate();
        Long zoneId = profile.getZoneId();

        if (template.getTemplateType() == TemplateType.SYSTEM) {
            throw new InvalidParameterValueException("The DomR template cannot be deleted.");
        }

        if (zoneId != null && (this.storeMgr.getImageStore(zoneId) == null)) {
            throw new InvalidParameterValueException("Failed to find a secondary storage in the specified zone.");
        }

        return profile;
    }

    @Override
    public TemplateProfile prepareDelete(DeleteIsoCmd cmd) {
        TemplateProfile profile = super.prepareDelete(cmd);
        Long zoneId = profile.getZoneId();

        if (zoneId != null && (this.storeMgr.getImageStore(zoneId) == null)) {
            throw new InvalidParameterValueException("Failed to find a secondary storage in the specified zone.");
        }

        return profile;
    }

    @Override
    public TemplateProfile prepareExtractTemplate(ExtractTemplateCmd extractcmd) {
        TemplateProfile profile = super.prepareExtractTemplate(extractcmd);
        VMTemplateVO template = profile.getTemplate();
        Long zoneId = profile.getZoneId();
        Long templateId = template.getId();

        // Simply return profile if non-ESX hypervisor.
        if (template.getHypervisorType() == HypervisorType.VMware) {
            PrepareOVAPackingCommand cmd = null;
            String zoneName="";
            List<DataStore> imageStores = null;

            if (!template.isCrossZones()) {
                if (zoneId == null) {
                    throw new CloudRuntimeException("ZoneId cannot be null for a template that is not available across zones");
                }
                // Else get the list of image stores in this zone's scope.
                DataCenterVO zone = _dcDao.findById(zoneId);
                zoneName = zone.getName();
                imageStores = this.storeMgr.getImageStoresByScope(new ZoneScope(profile.getZoneId()));
            } else {
                // template is available across zones. Get a list of all image stores.
                imageStores = this.storeMgr.listImageStores();
            }

            if (imageStores == null || imageStores.size() == 0) {
                throw new CloudRuntimeException("Unable to find an image store zone when trying to download template " + profile.getTemplate());
            }

            s_logger.debug("Attempting to mark template host refs for template: " + template.getName() + " as destroyed in zone: " + zoneName);

            // Make sure the template is downloaded to all the necessary secondary storage hosts

            for (DataStore store : imageStores) {
                long storeId = store.getId();
                List<TemplateDataStoreVO> templateStoreVOs = _tmpltStoreDao.listByTemplateStore(templateId, storeId);
                for (TemplateDataStoreVO templateStoreVO : templateStoreVOs) {
                    if (templateStoreVO.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
                        String errorMsg = "Please specify a template that is not currently being downloaded.";
                        s_logger.debug("Template: " + template.getName() + " is currently being downloaded to secondary storage host: " + store.getName() + ".");
                        throw new CloudRuntimeException(errorMsg);
                    }
                    String installPath = templateStoreVO.getInstallPath();
                    if (installPath != null) {
                        EndPoint ep = _epSelector.select(store);
                        if (ep == null) {
                            s_logger.warn("prepareOVAPacking (hyervisorTemplateAdapter): There is no secondary storage VM for secondary storage host " + store.getName());
                            throw new CloudRuntimeException("PrepareExtractTemplate: can't locate ssvm for SecStorage Host.");
                        }
                        cmd = new PrepareOVAPackingCommand(store.getUri(), installPath);
                        cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
                        Answer answer = ep.sendMessage(cmd);

                        if (answer == null || !answer.getResult()) {
                            s_logger.debug("Failed to create OVA for template " + templateStoreVO + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
                            throw new CloudRuntimeException("PrepareExtractTemplate: Failed to create OVA for template extraction. ");
                        }
                    }
                }
            }
        }
        return profile;
    }
}
