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

import com.cloud.agent.api.Answer;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import com.cloud.configuration.Config;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.agent.directdownload.CheckUrlAnswer;
import org.apache.cloudstack.agent.directdownload.CheckUrlCommand;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
import org.apache.cloudstack.api.command.user.template.GetUploadParamsForTemplateCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.storage.command.TemplateOrVolumePostUploadCommand;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.log4j.Logger;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.org.Grouping;
import com.cloud.server.StatsCollector;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

public class HypervisorTemplateAdapter extends TemplateAdapterBase {
    private final static Logger s_logger = Logger.getLogger(HypervisorTemplateAdapter.class);
    @Inject
    DownloadMonitor _downloadMonitor;
    @Inject
    AgentManager _agentMgr;
    @Inject
    StatsCollector _statsCollector;
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    DataStoreManager storeMgr;
    @Inject
    TemplateService imageService;
    @Inject
    TemplateDataFactory imageFactory;
    @Inject
    TemplateManager templateMgr;
    @Inject
    AlertManager alertMgr;
    @Inject
    VMTemplateZoneDao templateZoneDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    MessageBus _messageBus;
    @Inject
    ResourceManager resourceManager;
    @Inject
    VMTemplateDao templateDao;

    @Override
    public String getName() {
        return TemplateAdapterType.Hypervisor.getName();
    }

    /**
     * Validate on random running KVM host that URL is reachable
     * @param url url
     */
    private Long performDirectDownloadUrlValidation(final String url) {
        HostVO host = resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM);
        if (host == null) {
            throw new CloudRuntimeException("Couldn't find a host to validate URL " + url);
        }
        CheckUrlCommand cmd = new CheckUrlCommand(url);
        s_logger.debug("Performing URL " + url + " validation on host " + host.getId());
        Answer answer = _agentMgr.easySend(host.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException("URL: " + url + " validation failed on host id " + host.getId());
        }
        CheckUrlAnswer ans = (CheckUrlAnswer) answer;
        return ans.getTemplateSize();
    }

    @Override
    public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException {
        TemplateProfile profile = super.prepare(cmd);
        String url = profile.getUrl();
        UriUtils.validateUrl(ImageFormat.ISO.getFileExtension(), url);
        if (cmd.isDirectDownload()) {
            DigestHelper.validateChecksumString(cmd.getChecksum());
            Long templateSize = performDirectDownloadUrlValidation(url);
            profile.setSize(templateSize);
        }
        profile.setUrl(url);
        // Check that the resource limit for secondary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()), ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
        return profile;
    }

    @Override
    public TemplateProfile prepare(GetUploadParamsForIsoCmd cmd) throws ResourceAllocationException {
        TemplateProfile profile = super.prepare(cmd);

        // Check that the resource limit for secondary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()), ResourceType.secondary_storage);
        return profile;
    }

    @Override
    public TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException {
        TemplateProfile profile = super.prepare(cmd);
        String url = profile.getUrl();
        UriUtils.validateUrl(cmd.getFormat(), url);
        if (cmd.isDirectDownload()) {
            DigestHelper.validateChecksumString(cmd.getChecksum());
            Long templateSize = performDirectDownloadUrlValidation(url);
            profile.setSize(templateSize);
        }
        profile.setUrl(url);
        // Check that the resource limit for secondary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()), ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
        return profile;
    }

    @Override
    public TemplateProfile prepare(GetUploadParamsForTemplateCmd cmd) throws ResourceAllocationException {
        TemplateProfile profile = super.prepare(cmd);

        // Check that the resource limit for secondary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(cmd.getEntityOwnerId()), ResourceType.secondary_storage);
        return profile;
    }

    /**
     * Persist template marking it for direct download to Primary Storage, skipping Secondary Storage
     */
    private void persistDirectDownloadTemplate(long templateId, Long size) {
        TemplateDataStoreVO directDownloadEntry = templateDataStoreDao.createTemplateDirectDownloadEntry(templateId, size);
        templateDataStoreDao.persist(directDownloadEntry);
    }

    @Override
    public VMTemplateVO create(TemplateProfile profile) {
        // persist entry in vm_template, vm_template_details and template_zone_ref tables, not that entry at template_store_ref is not created here, and created in createTemplateAsync.
        VMTemplateVO template = persistTemplate(profile, State.Active);

        if (template == null) {
            throw new CloudRuntimeException("Unable to persist the template " + profile.getTemplate());
        }

        if (!profile.isDirectDownload()) {
            List<Long> zones = profile.getZoneIdList();

            //zones is null when this template is to be registered to all zones
            if (zones == null){
                createTemplateWithinZone(null, profile, template);
            }
            else {
                for (Long zId : zones) {
                    createTemplateWithinZone(zId, profile, template);
                }
            }
        } else {
            //KVM direct download templates bypassing Secondary Storage
            persistDirectDownloadTemplate(template.getId(), profile.getSize());
        }

        _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.template);
        return template;
    }

    private void createTemplateWithinZone(Long zId, TemplateProfile profile, VMTemplateVO template) {
        // find all eligible image stores for this zone scope
        List<DataStore> imageStores = storeMgr.getImageStoresByScope(new ZoneScope(zId));
        if (imageStores == null || imageStores.size() == 0) {
            throw new CloudRuntimeException("Unable to find image store to download template " + profile.getTemplate());
        }

        Set<Long> zoneSet = new HashSet<Long>();
        Collections.shuffle(imageStores);
        // For private templates choose a random store. TODO - Have a better algorithm based on size, no. of objects, load etc.
        for (DataStore imageStore : imageStores) {
            // skip data stores for a disabled zone
            Long zoneId = imageStore.getScope().getScopeId();
            if (zoneId != null) {
                DataCenterVO zone = _dcDao.findById(zoneId);
                if (zone == null) {
                    s_logger.warn("Unable to find zone by id " + zoneId + ", so skip downloading template to its image store " + imageStore.getId());
                    continue;
                }

                // Check if zone is disabled
                if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
                    s_logger.info("Zone " + zoneId + " is disabled. Skip downloading template to its image store " + imageStore.getId());
                    continue;
                }

                // Check if image store has enough capacity for template
                if (!_statsCollector.imageStoreHasEnoughCapacity(imageStore)) {
                    s_logger.info("Image store doesn't have enough capacity. Skip downloading template to this image store " + imageStore.getId());
                    continue;
                }
                // We want to download private template to one of the image store in a zone
                if (isPrivateTemplate(template) && zoneSet.contains(zoneId)) {
                    continue;
                } else {
                    zoneSet.add(zoneId);
                }
            }
            TemplateInfo tmpl = imageFactory.getTemplate(template.getId(), imageStore);
            CreateTemplateContext<TemplateApiResult> context = new CreateTemplateContext<TemplateApiResult>(null, tmpl);
            AsyncCallbackDispatcher<HypervisorTemplateAdapter, TemplateApiResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createTemplateAsyncCallBack(null, null));
            caller.setContext(context);
            imageService.createTemplateAsync(tmpl, imageStore, caller);
        }
    }

    @Override
    public List<TemplateOrVolumePostUploadCommand> createTemplateForPostUpload(final TemplateProfile profile) {
        // persist entry in vm_template, vm_template_details and template_zone_ref tables, not that entry at template_store_ref is not created here, and created in createTemplateAsync.
        return Transaction.execute(new TransactionCallback<List<TemplateOrVolumePostUploadCommand>>() {

            @Override
            public List<TemplateOrVolumePostUploadCommand> doInTransaction(TransactionStatus status) {

                VMTemplateVO template = persistTemplate(profile, State.NotUploaded);

                if (template == null) {
                    throw new CloudRuntimeException("Unable to persist the template " + profile.getTemplate());
                }

                if (profile.getZoneIdList() != null && profile.getZoneIdList().size() > 1)
                    throw new CloudRuntimeException("Operation is not supported for more than one zone id at a time");

                Long zoneId = null;
                if (profile.getZoneIdList() != null)
                    zoneId = profile.getZoneIdList().get(0);

                // find all eligible image stores for this zone scope
                List<DataStore> imageStores = storeMgr.getImageStoresByScope(new ZoneScope(zoneId));
                if (imageStores == null || imageStores.size() == 0) {
                    throw new CloudRuntimeException("Unable to find image store to download template " + profile.getTemplate());
                }

                List<TemplateOrVolumePostUploadCommand> payloads = new LinkedList<>();
                Set<Long> zoneSet = new HashSet<Long>();
                Collections.shuffle(imageStores); // For private templates choose a random store. TODO - Have a better algorithm based on size, no. of objects, load etc.
                for (DataStore imageStore : imageStores) {
                    // skip data stores for a disabled zone
                    Long zoneId_is = imageStore.getScope().getScopeId();
                    if (zoneId != null) {
                        DataCenterVO zone = _dcDao.findById(zoneId_is);
                        if (zone == null) {
                            s_logger.warn("Unable to find zone by id " + zoneId_is +
                                        ", so skip downloading template to its image store " + imageStore.getId());
                            continue;
                        }

                        // Check if zone is disabled
                        if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
                            s_logger.info("Zone " + zoneId_is +
                                    " is disabled, so skip downloading template to its image store " + imageStore.getId());
                            continue;
                        }

                        // We want to download private template to one of the image store in a zone
                        if (isPrivateTemplate(template) && zoneSet.contains(zoneId_is)) {
                            continue;
                        } else {
                            zoneSet.add(zoneId_is);
                        }

                    }

                    TemplateInfo tmpl = imageFactory.getTemplate(template.getId(), imageStore);
                    //imageService.createTemplateAsync(tmpl, imageStore, caller);

                    // persist template_store_ref entry
                    DataObject templateOnStore = imageStore.create(tmpl);
                    // update template_store_ref and template state

                    EndPoint ep = _epSelector.select(templateOnStore);
                    if (ep == null) {
                        String errMsg = "There is no secondary storage VM for downloading template to image store " + imageStore.getName();
                        s_logger.warn(errMsg);
                        throw new CloudRuntimeException(errMsg);
                    }

                    TemplateOrVolumePostUploadCommand payload = new TemplateOrVolumePostUploadCommand(template.getId(), template.getUuid(), tmpl.getInstallPath(), tmpl
                            .getChecksum(), tmpl.getType().toString(), template.getUniqueName(), template.getFormat().toString(), templateOnStore.getDataStore().getUri(),
                            templateOnStore.getDataStore().getRole().toString());
                    //using the existing max template size configuration
                    payload.setMaxUploadSize(_configDao.getValue(Config.MaxTemplateAndIsoSize.key()));
                    payload.setDefaultMaxAccountSecondaryStorage(_configDao.getValue(Config.DefaultMaxAccountSecondaryStorage.key()));
                    payload.setAccountId(template.getAccountId());
                    payload.setRemoteEndPoint(ep.getPublicAddr());
                    payload.setRequiresHvm(template.requiresHvm());
                    payload.setDescription(template.getDisplayText());
                    payloads.add(payload);
                }
                if(payloads.isEmpty()) {
                    throw new CloudRuntimeException("unable to find zone or an image store with enough capacity");
                }
                _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), ResourceType.template);
                return payloads;
            }
        });
    }

    private boolean isPrivateTemplate(VMTemplateVO template){

        // if public OR featured OR system template
        if(template.isPublicTemplate() || template.isFeatured() || template.getTemplateType() == TemplateType.SYSTEM)
            return false;
        else
            return true;
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
        if (result.isSuccess()) {
            VMTemplateVO tmplt = _tmpltDao.findById(template.getId());
            // need to grant permission for public templates
            if (tmplt.isPublicTemplate()) {
                _messageBus.publish(_name, TemplateManager.MESSAGE_REGISTER_PUBLIC_TEMPLATE_EVENT, PublishScope.LOCAL, tmplt.getId());
            }
            long accountId = tmplt.getAccountId();
            if (template.getSize() != null) {
                // publish usage event
                String etype = EventTypes.EVENT_TEMPLATE_CREATE;
                if (tmplt.getFormat() == ImageFormat.ISO) {
                    etype = EventTypes.EVENT_ISO_CREATE;
                }
                // get physical size from template_store_ref table
                long physicalSize = 0;
                DataStore ds = template.getDataStore();
                TemplateDataStoreVO tmpltStore = _tmpltStoreDao.findByStoreTemplate(ds.getId(), template.getId());
                if (tmpltStore != null) {
                    physicalSize = tmpltStore.getPhysicalSize();
                } else {
                    s_logger.warn("No entry found in template_store_ref for template id: " + template.getId() + " and image store id: " + ds.getId() +
                        " at the end of registering template!");
                }
                Scope dsScope = ds.getScope();
                if (dsScope.getScopeType() == ScopeType.ZONE) {
                    if (dsScope.getScopeId() != null) {
                        UsageEventUtils.publishUsageEvent(etype, template.getAccountId(), dsScope.getScopeId(), template.getId(), template.getName(), null, null,
                            physicalSize, template.getSize(), VirtualMachineTemplate.class.getName(), template.getUuid());
                    } else {
                        s_logger.warn("Zone scope image store " + ds.getId() + " has a null scope id");
                    }
                } else if (dsScope.getScopeType() == ScopeType.REGION) {
                    // publish usage event for region-wide image store using a -1 zoneId for 4.2, need to revisit post-4.2
                    UsageEventUtils.publishUsageEvent(etype, template.getAccountId(), -1, template.getId(), template.getName(), null, null, physicalSize,
                        template.getSize(), VirtualMachineTemplate.class.getName(), template.getUuid());
                }
                _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.secondary_storage, template.getSize());
            }
        }

        return null;
    }

    @Override
    @DB
    public boolean delete(TemplateProfile profile) {
        boolean success = false;

        VMTemplateVO template = profile.getTemplate();
        Account account = _accountDao.findByIdIncludingRemoved(template.getAccountId());

        if (profile.getZoneIdList() != null && profile.getZoneIdList().size() > 1)
            throw new CloudRuntimeException("Operation is not supported for more than one zone id at a time");

        Long zoneId = null;
        if (profile.getZoneIdList() != null)
            zoneId = profile.getZoneIdList().get(0);

        // find all eligible image stores for this template
        List<DataStore> imageStores = templateMgr.getImageStoreByTemplate(template.getId(),
                                            zoneId);

        if (imageStores == null || imageStores.size() == 0) {
            // already destroyed on image stores
            success = true;
            s_logger.info("Unable to find image store still having template: " + template.getName() + ", so just mark the template removed");
        } else {
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

            String eventType = "";
            if (template.getFormat().equals(ImageFormat.ISO)) {
                eventType = EventTypes.EVENT_ISO_DELETE;
            } else {
                eventType = EventTypes.EVENT_TEMPLATE_DELETE;
            }

            for (DataStore imageStore : imageStores) {
                // publish zone-wide usage event
                Long sZoneId = ((ImageStoreEntity)imageStore).getDataCenterId();
                if (sZoneId != null) {
                    UsageEventUtils.publishUsageEvent(eventType, template.getAccountId(), sZoneId, template.getId(), null, VirtualMachineTemplate.class.getName(),
                            template.getUuid());
                }

                boolean dataDiskDeletetionResult = true;
                List<VMTemplateVO> dataDiskTemplates = templateDao.listByParentTemplatetId(template.getId());
                if (dataDiskTemplates != null && dataDiskTemplates.size() > 0) {
                    s_logger.info("Template: " + template.getId() + " has Datadisk template(s) associated with it. Delete Datadisk templates before deleting the template");
                    for (VMTemplateVO dataDiskTemplate : dataDiskTemplates) {
                        s_logger.info("Delete Datadisk template: " + dataDiskTemplate.getId() + " from image store: " + imageStore.getName());
                        AsyncCallFuture<TemplateApiResult> future = imageService.deleteTemplateAsync(imageFactory.getTemplate(dataDiskTemplate.getId(), imageStore));
                        try {
                            TemplateApiResult result = future.get();
                            dataDiskDeletetionResult = result.isSuccess();
                            if (!dataDiskDeletetionResult) {
                                s_logger.warn("Failed to delete datadisk template: " + dataDiskTemplate + " from image store: " + imageStore.getName() + " due to: "
                                        + result.getResult());
                                break;
                            }
                            // Remove from template_zone_ref
                            List<VMTemplateZoneVO> templateZones = templateZoneDao.listByZoneTemplate(sZoneId, dataDiskTemplate.getId());
                            if (templateZones != null) {
                                for (VMTemplateZoneVO templateZone : templateZones) {
                                    templateZoneDao.remove(templateZone.getId());
                                }
                            }
                            // Mark datadisk template as Inactive
                            List<DataStore> iStores = templateMgr.getImageStoreByTemplate(dataDiskTemplate.getId(), null);
                            if (iStores == null || iStores.size() == 0) {
                                dataDiskTemplate.setState(VirtualMachineTemplate.State.Inactive);
                                _tmpltDao.update(dataDiskTemplate.getId(), dataDiskTemplate);
                            }
                            // Decrement total secondary storage space used by the account
                            _resourceLimitMgr.recalculateResourceCount(dataDiskTemplate.getAccountId(), account.getDomainId(), ResourceType.secondary_storage.getOrdinal());
                        } catch (Exception e) {
                            s_logger.debug("Delete datadisk template failed", e);
                            throw new CloudRuntimeException("Delete datadisk template failed", e);
                        }
                    }
                }
                // remove from template_zone_ref
                if (dataDiskDeletetionResult) {
                    s_logger.info("Delete template: " + template.getId() + " from image store: " + imageStore.getName());
                    AsyncCallFuture<TemplateApiResult> future = imageService.deleteTemplateAsync(imageFactory.getTemplate(template.getId(), imageStore));
                    try {
                        TemplateApiResult result = future.get();
                        success = result.isSuccess();
                        if (!success) {
                            s_logger.warn("Failed to delete the template: " + template + " from the image store: " + imageStore.getName() + " due to: " + result.getResult());
                            break;
                        }

                        // remove from template_zone_ref
                        List<VMTemplateZoneVO> templateZones = templateZoneDao.listByZoneTemplate(sZoneId, template.getId());
                        if (templateZones != null) {
                            for (VMTemplateZoneVO templateZone : templateZones) {
                                templateZoneDao.remove(templateZone.getId());
                            }
                        }
                    } catch (InterruptedException|ExecutionException e) {
                        s_logger.debug("Delete template Failed", e);
                        throw new CloudRuntimeException("Delete template Failed", e);
                    }
                } else {
                    s_logger.warn("Template: " + template.getId() + " won't be deleted from image store: " + imageStore.getName() + " because deletion of one of the Datadisk"
                            + " templates that belonged to the template failed");
                }
            }

        }
        if (success) {
            if ((imageStores != null && imageStores.size() > 1) && (profile.getZoneIdList() != null)) {
                //if template is stored in more than one image stores, and the zone id is not null, then don't delete other templates.
                return success;
            }

            // delete all cache entries for this template
            List<TemplateInfo> cacheTmpls = imageFactory.listTemplateOnCache(template.getId());
            for (TemplateInfo tmplOnCache : cacheTmpls) {
                s_logger.info("Delete template: " + tmplOnCache.getId() + " from image cache store: " + tmplOnCache.getDataStore().getName());
                tmplOnCache.delete();
            }

            // find all eligible image stores for this template
            List<DataStore> iStores = templateMgr.getImageStoreByTemplate(template.getId(), null);
            if (iStores == null || iStores.size() == 0) {
                // Mark template as Inactive.
                template.setState(VirtualMachineTemplate.State.Inactive);
                _tmpltDao.update(template.getId(), template);

                    // Decrement the number of templates and total secondary storage
                    // space used by the account
                    _resourceLimitMgr.decrementResourceCount(template.getAccountId(), ResourceType.template);
                    _resourceLimitMgr.recalculateResourceCount(template.getAccountId(), account.getDomainId(), ResourceType.secondary_storage.getOrdinal());

            }

            // remove its related ACL permission
            Pair<Class<?>, Long> tmplt = new Pair<Class<?>, Long>(VirtualMachineTemplate.class, template.getId());
            _messageBus.publish(_name, EntityManager.MESSAGE_REMOVE_ENTITY_EVENT, PublishScope.LOCAL, tmplt);

        }
        return success;

    }

    @Override
    public TemplateProfile prepareDelete(DeleteTemplateCmd cmd) {
        TemplateProfile profile = super.prepareDelete(cmd);
        VMTemplateVO template = profile.getTemplate();
        List<Long> zoneIdList = profile.getZoneIdList();

        if (template.getTemplateType() == TemplateType.SYSTEM) {
            throw new InvalidParameterValueException("The DomR template cannot be deleted.");
        }

        if (zoneIdList != null && (storeMgr.getImageStoreWithFreeCapacity(zoneIdList.get(0)) == null)) {
            throw new InvalidParameterValueException("Failed to find a secondary storage in the specified zone.");
        }

        return profile;
    }

    @Override
    public TemplateProfile prepareDelete(DeleteIsoCmd cmd) {
        TemplateProfile profile = super.prepareDelete(cmd);
        List<Long> zoneIdList = profile.getZoneIdList();

        if (zoneIdList != null &&
                (storeMgr.getImageStoreWithFreeCapacity(zoneIdList.get(0)) == null)) {
            throw new InvalidParameterValueException("Failed to find a secondary storage in the specified zone.");
        }

        return profile;
    }
}
