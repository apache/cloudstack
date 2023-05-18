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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VmDetailConstants;

@Component
public class TemplateServiceImpl implements TemplateService {
    private static final Logger s_logger = Logger.getLogger(TemplateServiceImpl.class);
    @Inject
    ObjectInDataStoreManager _objectInDataStoreMgr;
    @Inject
    DataObjectManager _dataObjectMgr;
    @Inject
    DataStoreManager _storeMgr;
    @Inject
    DataMotionService _motionSrv;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    TemplateDataFactory _templateFactory;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    TemplateManager _tmpltMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    StorageCacheManager _cacheMgr;
    @Inject
    MessageBus _messageBus;
    @Inject
    ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    TemplateDataFactory imageFactory;

    class TemplateOpContext<T> extends AsyncRpcContext<T> {
        final TemplateObject template;
        final AsyncCallFuture<TemplateApiResult> future;

        public TemplateOpContext(AsyncCompletionCallback<T> callback, TemplateObject template, AsyncCallFuture<TemplateApiResult> future) {
            super(callback);
            this.template = template;
            this.future = future;
        }

        public TemplateObject getTemplate() {
            return template;
        }

        public AsyncCallFuture<TemplateApiResult> getFuture() {
            return future;
        }

    }

    @Override
    public void createTemplateAsync(TemplateInfo template, DataStore store, AsyncCompletionCallback<TemplateApiResult> callback) {
        // persist template_store_ref entry
        TemplateObject templateOnStore = (TemplateObject)store.create(template);
        // update template_store_ref and template state
        try {
            templateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);
        } catch (Exception e) {
            TemplateApiResult result = new TemplateApiResult(templateOnStore);
            result.setResult(e.toString());
            result.setSuccess(false);
            if (callback != null) {
                callback.complete(result);
            }
            return;
        }

        try {
            TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(callback, templateOnStore, null);

            AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createTemplateCallback(null, null)).setContext(context);
            store.getDriver().createAsync(store, templateOnStore, caller);
        } catch (CloudRuntimeException ex) {
            // clean up already persisted template_store_ref entry in case of createTemplateCallback is never called
            TemplateDataStoreVO templateStoreVO = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
            if (templateStoreVO != null) {
                TemplateInfo tmplObj = _templateFactory.getTemplate(template, store, null);
                tmplObj.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            TemplateApiResult result = new TemplateApiResult(template);
            result.setResult(ex.getMessage());
            if (callback != null) {
                callback.complete(result);
            }
        }
    }

    @Override
    public void downloadBootstrapSysTemplate(DataStore store) {
        Set<VMTemplateVO> toBeDownloaded = new HashSet();

        List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();

        for (VMTemplateVO rtngTmplt : rtngTmplts) {
            toBeDownloaded.add(rtngTmplt);
        }

        List<HypervisorType> availHypers = _clusterDao.getAvailableHypervisorInZone(store.getScope().getScopeId());
        if (availHypers.isEmpty()) {
            /*
             * This is for cloudzone, local secondary storage resource started
             * before cluster created
             */
            availHypers.add(HypervisorType.KVM);
        }
        /* Baremetal need not to download any template */
        availHypers.remove(HypervisorType.BareMetal);
        availHypers.add(HypervisorType.None); // bug 9809: resume ISO
        // download.

        for (VMTemplateVO template : toBeDownloaded) {
            if (availHypers.contains(template.getHypervisorType())) {
                // only download sys template applicable for current hypervisor
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
                if (tmpltHost == null || tmpltHost.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                    TemplateInfo tmplt = _templateFactory.getTemplate(template.getId(), DataStoreRole.Image);
                    createTemplateAsync(tmplt, store, null);
                }
            }
        }
    }

    @Override
    public void handleSysTemplateDownload(HypervisorType hostHyper, Long dcId) {
        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();
        List<DataStore> stores = _storeMgr.getImageStoresByScopeExcludingReadOnly(new ZoneScope(dcId));
        if (stores == null || stores.isEmpty()) {
            return;
        }

        /* Download all the templates in zone with the same hypervisortype */
        for (DataStore store : stores) {
            List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
            List<VMTemplateVO> defaultBuiltin = _templateDao.listDefaultBuiltinTemplates();

            for (VMTemplateVO rtngTmplt : rtngTmplts) {
                if (rtngTmplt.getHypervisorType() == hostHyper && !rtngTmplt.isDirectDownload()) {
                    toBeDownloaded.add(rtngTmplt);
                }
            }

            for (VMTemplateVO builtinTmplt : defaultBuiltin) {
                if (builtinTmplt.getHypervisorType() == hostHyper && !builtinTmplt.isDirectDownload()) {
                    toBeDownloaded.add(builtinTmplt);
                }
            }

            for (VMTemplateVO template : toBeDownloaded) {
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
                if (tmpltHost == null) {
                    associateTemplateToZone(template.getId(), dcId);
                    s_logger.info("Downloading builtin template " + template.getUniqueName() + " to data center: " + dcId);
                    TemplateInfo tmplt = _templateFactory.getTemplate(template.getId(), DataStoreRole.Image);
                    createTemplateAsync(tmplt, store, null);
                }
            }
        }
    }

    @Override
    public void handleTemplateSync(DataStore store) {
        if (store == null) {
            s_logger.warn("Huh? image store is null");
            return;
        }
        long storeId = store.getId();

        // add lock to make template sync for a data store only be done once
        String lockString = "templatesync.storeId:" + storeId;
        GlobalLock syncLock = GlobalLock.getInternLock(lockString);
        try {
            if (syncLock.lock(3)) {
                try {
                    Long zoneId = store.getScope().getScopeId();

                    Map<String, TemplateProp> templateInfos = listTemplate(store);
                    if (templateInfos == null) {
                        return;
                    }

                    Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();
                    List<VMTemplateVO> allTemplates = null;
                    if (zoneId == null) {
                        // region wide store
                        allTemplates = _templateDao.listByState(VirtualMachineTemplate.State.Active, VirtualMachineTemplate.State.NotUploaded, VirtualMachineTemplate.State.UploadInProgress);
                    } else {
                        // zone wide store
                        allTemplates = _templateDao.listInZoneByState(zoneId, VirtualMachineTemplate.State.Active, VirtualMachineTemplate.State.NotUploaded, VirtualMachineTemplate.State.UploadInProgress);
                    }
                    List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
                    List<VMTemplateVO> defaultBuiltin = _templateDao.listDefaultBuiltinTemplates();

                    if (rtngTmplts != null) {
                        for (VMTemplateVO rtngTmplt : rtngTmplts) {
                            if (!allTemplates.contains(rtngTmplt)) {
                                allTemplates.add(rtngTmplt);
                            }
                        }
                    }

                    if (defaultBuiltin != null) {
                        for (VMTemplateVO builtinTmplt : defaultBuiltin) {
                            if (!allTemplates.contains(builtinTmplt)) {
                                allTemplates.add(builtinTmplt);
                            }
                        }
                    }

                    for (Iterator<VMTemplateVO> iter = allTemplates.listIterator(); iter.hasNext();) {
                        VMTemplateVO child_template = iter.next();
                        if (child_template.getParentTemplateId() != null) {
                            String uniqueName = child_template.getUniqueName();
                            if (templateInfos.containsKey(uniqueName)) {
                                templateInfos.remove(uniqueName);
                            }
                            iter.remove();
                        }
                    }

                    toBeDownloaded.addAll(allTemplates);

                    final StateMachine2<VirtualMachineTemplate.State, VirtualMachineTemplate.Event, VirtualMachineTemplate> stateMachine = VirtualMachineTemplate.State.getStateMachine();
                    for (VMTemplateVO tmplt : allTemplates) {
                        String uniqueName = tmplt.getUniqueName();
                        TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());
                        if (templateInfos.containsKey(uniqueName)) {
                            TemplateProp tmpltInfo = templateInfos.remove(uniqueName);
                            toBeDownloaded.remove(tmplt);
                            if (tmpltStore != null) {
                                s_logger.info("Template Sync found " + uniqueName + " already in the image store");
                                if (tmpltStore.getDownloadState() != Status.DOWNLOADED) {
                                    tmpltStore.setErrorString("");
                                }
                                if (tmpltInfo.isCorrupted()) {
                                    tmpltStore.setDownloadState(Status.DOWNLOAD_ERROR);
                                    String msg = "Template " + tmplt.getName() + ":" + tmplt.getId() + " is corrupted on secondary storage " + tmpltStore.getId();
                                    tmpltStore.setErrorString(msg);
                                    s_logger.info(msg);
                                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED, zoneId, null, msg, msg);
                                    if (tmplt.getState() == VirtualMachineTemplate.State.NotUploaded || tmplt.getState() == VirtualMachineTemplate.State.UploadInProgress) {
                                        s_logger.info("Template Sync found " + uniqueName + " on image store " + storeId + " uploaded using SSVM as corrupted, marking it as failed");
                                        tmpltStore.setState(State.Failed);
                                        try {
                                            stateMachine.transitTo(tmplt, VirtualMachineTemplate.Event.OperationFailed, null, _templateDao);
                                        } catch (NoTransitionException e) {
                                            s_logger.error("Unexpected state transition exception for template " + tmplt.getName() + ". Details: " + e.getMessage());
                                        }
                                    } else if (tmplt.getUrl() == null) {
                                        msg = "Private template (" + tmplt + ") with install path " + tmpltInfo.getInstallPath() + " is corrupted, please check in image store: " + tmpltStore.getDataStoreId();
                                        s_logger.warn(msg);
                                    } else {
                                        s_logger.info("Removing template_store_ref entry for corrupted template " + tmplt.getName());
                                        _vmTemplateStoreDao.remove(tmpltStore.getId());
                                        toBeDownloaded.add(tmplt);
                                    }
                                } else {
                                    if(tmpltStore.getDownloadState() != Status.DOWNLOADED) {
                                        String etype = EventTypes.EVENT_TEMPLATE_CREATE;
                                        if (tmplt.getFormat() == ImageFormat.ISO) {
                                            etype = EventTypes.EVENT_ISO_CREATE;
                                        }

                                        if (zoneId != null) {
                                            UsageEventUtils.publishUsageEvent(etype, tmplt.getAccountId(), zoneId, tmplt.getId(), tmplt.getName(), null, null,
                                                    tmpltInfo.getPhysicalSize(), tmpltInfo.getSize(), VirtualMachineTemplate.class.getName(), tmplt.getUuid());
                                        }
                                    }

                                    tmpltStore.setDownloadPercent(100);
                                    tmpltStore.setDownloadState(Status.DOWNLOADED);
                                    tmpltStore.setState(ObjectInDataStoreStateMachine.State.Ready);
                                    tmpltStore.setInstallPath(tmpltInfo.getInstallPath());
                                    tmpltStore.setSize(tmpltInfo.getSize());
                                    tmpltStore.setPhysicalSize(tmpltInfo.getPhysicalSize());
                                    tmpltStore.setLastUpdated(new Date());
                                    // update size in vm_template table
                                    VMTemplateVO tmlpt = _templateDao.findById(tmplt.getId());
                                    tmlpt.setSize(tmpltInfo.getSize());
                                    _templateDao.update(tmplt.getId(), tmlpt);

                                    if (tmplt.getState() == VirtualMachineTemplate.State.NotUploaded || tmplt.getState() == VirtualMachineTemplate.State.UploadInProgress) {
                                        VirtualMachineTemplate.Event event = VirtualMachineTemplate.Event.OperationSucceeded;
                                        // For multi-disk OVA, check and create data disk templates
                                        if (tmplt.getFormat().equals(ImageFormat.OVA)) {
                                            if (!createOvaDataDiskTemplates(_templateFactory.getTemplate(tmlpt.getId(), store), tmplt.isDeployAsIs())) {
                                                event = VirtualMachineTemplate.Event.OperationFailed;
                                            }
                                        }
                                        try {
                                            stateMachine.transitTo(tmplt, event, null, _templateDao);
                                        } catch (NoTransitionException e) {
                                            s_logger.error("Unexpected state transition exception for template " + tmplt.getName() + ". Details: " + e.getMessage());
                                        }
                                    }

                                    // Skipping limit checks for SYSTEM Account and for the templates created from volumes or snapshots
                                    // which already got checked and incremented during createTemplate API call.
                                    if (tmpltInfo.getSize() > 0 && tmplt.getAccountId() != Account.ACCOUNT_ID_SYSTEM && tmplt.getUrl() != null) {
                                        long accountId = tmplt.getAccountId();
                                        try {
                                            _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(accountId),
                                                    com.cloud.configuration.Resource.ResourceType.secondary_storage,
                                                    tmpltInfo.getSize() - UriUtils.getRemoteSize(tmplt.getUrl()));
                                        } catch (ResourceAllocationException e) {
                                            s_logger.warn(e.getMessage());
                                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, zoneId, null, e.getMessage(), e.getMessage());
                                        } finally {
                                            _resourceLimitMgr.recalculateResourceCount(accountId, _accountMgr.getAccount(accountId).getDomainId(),
                                                    com.cloud.configuration.Resource.ResourceType.secondary_storage.getOrdinal());
                                        }
                                    }
                                }
                                _vmTemplateStoreDao.update(tmpltStore.getId(), tmpltStore);
                            } else {
                                tmpltStore = new TemplateDataStoreVO(storeId, tmplt.getId(), new Date(), 100, Status.DOWNLOADED, null, null, null, tmpltInfo.getInstallPath(), tmplt.getUrl());
                                tmpltStore.setSize(tmpltInfo.getSize());
                                tmpltStore.setPhysicalSize(tmpltInfo.getPhysicalSize());
                                tmpltStore.setDataStoreRole(store.getRole());
                                _vmTemplateStoreDao.persist(tmpltStore);

                                // update size in vm_template table
                                VMTemplateVO tmlpt = _templateDao.findById(tmplt.getId());
                                tmlpt.setSize(tmpltInfo.getSize());
                                _templateDao.update(tmplt.getId(), tmlpt);
                                associateTemplateToZone(tmplt.getId(), zoneId);

                                String etype = EventTypes.EVENT_TEMPLATE_CREATE;
                                if (tmplt.getFormat() == ImageFormat.ISO) {
                                    etype = EventTypes.EVENT_ISO_CREATE;
                                }

                                UsageEventUtils.publishUsageEvent(etype, tmplt.getAccountId(), zoneId, tmplt.getId(), tmplt.getName(), null, null,
                                        tmpltInfo.getPhysicalSize(), tmpltInfo.getSize(), VirtualMachineTemplate.class.getName(), tmplt.getUuid());
                            }
                        } else if (tmplt.getState() == VirtualMachineTemplate.State.NotUploaded || tmplt.getState() == VirtualMachineTemplate.State.UploadInProgress) {
                            s_logger.info("Template Sync did not find " + uniqueName + " on image store " + storeId + " uploaded using SSVM, marking it as failed");
                            toBeDownloaded.remove(tmplt);
                            tmpltStore.setDownloadState(Status.DOWNLOAD_ERROR);
                            String msg = "Template " + tmplt.getName() + ":" + tmplt.getId() + " is corrupted on secondary storage " + tmpltStore.getId();
                            tmpltStore.setErrorString(msg);
                            tmpltStore.setState(State.Failed);
                            _vmTemplateStoreDao.update(tmpltStore.getId(), tmpltStore);
                            try {
                                stateMachine.transitTo(tmplt, VirtualMachineTemplate.Event.OperationFailed, null, _templateDao);
                            } catch (NoTransitionException e) {
                                s_logger.error("Unexpected state transition exception for template " + tmplt.getName() + ". Details: " + e.getMessage());
                            }
                        } else if (tmplt.isDirectDownload()) {
                            s_logger.info("Template " + tmplt.getName() + ":" + tmplt.getId() + " is marked for direct download, discarding it for download on image stores");
                            toBeDownloaded.remove(tmplt);
                        } else {
                            s_logger.info("Template Sync did not find " + uniqueName + " on image store " + storeId + ", may request download based on available hypervisor types");
                            if (tmpltStore != null) {
                                if (_storeMgr.isRegionStore(store) && tmpltStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED
                                        && tmpltStore.getState() == State.Ready
                                        && tmpltStore.getInstallPath() == null) {
                                    s_logger.info("Keep fake entry in template store table for migration of previous NFS to object store");
                                } else {
                                    s_logger.info("Removing leftover template " + uniqueName + " entry from template store table");
                                    // remove those leftover entries
                                    _vmTemplateStoreDao.remove(tmpltStore.getId());
                                }
                            }
                        }
                    }

                    if (toBeDownloaded.size() > 0) {
                        /* Only download templates whose hypervirsor type is in the zone */
                        List<HypervisorType> availHypers = _clusterDao.getAvailableHypervisorInZone(zoneId);
                        if (availHypers.isEmpty()) {
                            /*
                             * This is for cloudzone, local secondary storage resource
                             * started before cluster created
                             */
                            availHypers.add(HypervisorType.KVM);
                        }
                        /* Baremetal need not to download any template */
                        availHypers.remove(HypervisorType.BareMetal);
                        availHypers.add(HypervisorType.None); // bug 9809: resume ISO
                        // download.
                        for (VMTemplateVO tmplt : toBeDownloaded) {
                            if (tmplt.getUrl() == null) { // If url is null, skip downloading
                                s_logger.info("Skip downloading template " + tmplt.getUniqueName() + " since no url is specified.");
                                continue;
                            }
                            // if this is private template, skip sync to a new image store
                            if (!tmplt.isPublicTemplate() && !tmplt.isFeatured() && tmplt.getTemplateType() != TemplateType.SYSTEM) {
                                s_logger.info("Skip sync downloading private template " + tmplt.getUniqueName() + " to a new image store");
                                continue;
                            }

                            // if this is a region store, and there is already an DOWNLOADED entry there without install_path information, which
                            // means that this is a duplicate entry from migration of previous NFS to staging.
                            if (_storeMgr.isRegionStore(store)) {
                                TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());
                                if (tmpltStore != null && tmpltStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED && tmpltStore.getState() == State.Ready
                                        && tmpltStore.getInstallPath() == null) {
                                    s_logger.info("Skip sync template for migration of previous NFS to object store");
                                    continue;
                                }
                            }

                            if (availHypers.contains(tmplt.getHypervisorType())) {
                                s_logger.info("Downloading template " + tmplt.getUniqueName() + " to image store " + store.getName());
                                associateTemplateToZone(tmplt.getId(), zoneId);
                                TemplateInfo tmpl = _templateFactory.getTemplate(tmplt.getId(), store);
                                TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<>(null,(TemplateObject)tmpl, null);
                                AsyncCallbackDispatcher<TemplateServiceImpl, TemplateApiResult> caller = AsyncCallbackDispatcher.create(this);
                                caller.setCallback(caller.getTarget().createTemplateAsyncCallBack(null, null));
                                caller.setContext(context);
                                createTemplateAsync(tmpl, store, caller);
                            } else {
                                s_logger.info("Skip downloading template " + tmplt.getUniqueName() + " since current data center does not have hypervisor " +
                                        tmplt.getHypervisorType().toString());
                            }
                        }
                    }

                    for (String uniqueName : templateInfos.keySet()) {
                        TemplateProp tInfo = templateInfos.get(uniqueName);
                        if (_tmpltMgr.templateIsDeleteable(tInfo.getId())) {
                            // we cannot directly call deleteTemplateSync here to reuse delete logic since in this case db does not have this template at all.
                            TemplateObjectTO tmplTO = new TemplateObjectTO();
                            tmplTO.setDataStore(store.getTO());
                            tmplTO.setPath(tInfo.getInstallPath());
                            tmplTO.setId(tInfo.getId());
                            DeleteCommand dtCommand = new DeleteCommand(tmplTO);
                            EndPoint ep = _epSelector.select(store);
                            Answer answer = null;
                            if (ep == null) {
                                String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                                s_logger.error(errMsg);
                                answer = new Answer(dtCommand, false, errMsg);
                            } else {
                                answer = ep.sendMessage(dtCommand);
                            }
                            if (answer == null || !answer.getResult()) {
                                s_logger.info("Failed to deleted template at store: " + store.getName());

                            } else {
                                String description = "Deleted template " + tInfo.getTemplateName() + " on secondary storage " + storeId;
                                s_logger.info(description);
                            }

                        }
                    }
                } finally {
                    syncLock.unlock();
                }
            } else {
                s_logger.info("Couldn't get global lock on " + lockString + ", another thread may be doing template sync on data store " + storeId + " now.");
            }
        } finally {
            syncLock.releaseRef();
        }

    }

    // persist entry in template_zone_ref table. zoneId can be empty for
    // region-wide image store, in that case,
    // we will associate the template to all the zones.
    @Override
    public void associateTemplateToZone(long templateId, Long zoneId) {
        List<Long> dcs = new ArrayList<Long>();
        if (zoneId != null) {
            dcs.add(zoneId);
        } else {
            List<DataCenterVO> zones = _dcDao.listAll();
            for (DataCenterVO zone : zones) {
                dcs.add(zone.getId());
            }
        }
        for (Long id : dcs) {
            VMTemplateZoneVO tmpltZoneVO = _vmTemplateZoneDao.findByZoneTemplate(id, templateId);
            if (tmpltZoneVO == null) {
                tmpltZoneVO = new VMTemplateZoneVO(id, templateId, new Date());
                _vmTemplateZoneDao.persist(tmpltZoneVO);
            } else {
                tmpltZoneVO.setLastUpdated(new Date());
                _vmTemplateZoneDao.update(tmpltZoneVO.getId(), tmpltZoneVO);
            }
        }
    }

    // update template_zone_ref for cross-zone template for newly added zone
    @Override
    public void associateCrosszoneTemplatesToZone(long dcId) {
        VMTemplateZoneVO tmpltZone;

        List<VMTemplateVO> allTemplates = _templateDao.listAll();
        for (VMTemplateVO vt : allTemplates) {
            if (vt.isCrossZones()) {
                tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
                if (tmpltZone == null) {
                    VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
                    _vmTemplateZoneDao.persist(vmTemplateZone);
                }
            }
        }
    }

    protected Void createTemplateAsyncCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, TemplateApiResult> callback,
                                               TemplateOpContext<TemplateApiResult> context) {
        TemplateInfo template = context.template;
        TemplateApiResult result = callback.getResult();
        if (result.isSuccess()) {
            VMTemplateVO tmplt = _templateDao.findById(template.getId());
            // need to grant permission for public templates
            if (tmplt.isPublicTemplate()) {
                _messageBus.publish(null, TemplateManager.MESSAGE_REGISTER_PUBLIC_TEMPLATE_EVENT, PublishScope.LOCAL, tmplt.getId());
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
                TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(ds.getId(), template.getId());
                if (tmpltStore != null) {
                    physicalSize = tmpltStore.getPhysicalSize();
                } else {
                    s_logger.warn("No entry found in template_store_ref for template id: " + template.getId() + " and image store id: " + ds.getId() +
                            " at the end of registering template!");
                }
                Scope dsScope = ds.getScope();
                if (dsScope.getScopeId() != null) {
                    UsageEventUtils.publishUsageEvent(etype, template.getAccountId(), dsScope.getScopeId(), template.getId(), template.getName(), null, null,
                            physicalSize, template.getSize(), VirtualMachineTemplate.class.getName(), template.getUuid());
                } else {
                    s_logger.warn("Zone scope image store " + ds.getId() + " has a null scope id");
                }
                _resourceLimitMgr.incrementResourceCount(accountId, Resource.ResourceType.secondary_storage, template.getSize());
            }
        }

        return null;
    }

    private Map<String, TemplateProp> listTemplate(DataStore ssStore) {
        String nfsVersion = imageStoreDetailsUtil.getNfsVersion(ssStore.getId());
        ListTemplateCommand cmd = new ListTemplateCommand(ssStore.getTO(), nfsVersion);
        EndPoint ep = _epSelector.select(ssStore);
        Answer answer = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        if (answer != null && answer.getResult()) {
            ListTemplateAnswer tanswer = (ListTemplateAnswer)answer;
            return tanswer.getTemplateInfo();
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("can not list template for secondary storage host " + ssStore.getId());
            }
        }

        return null;
    }

    protected Void createTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> callback, TemplateOpContext<TemplateApiResult> context) {
        TemplateObject template = context.getTemplate();
        AsyncCompletionCallback<TemplateApiResult> parentCallback = context.getParentCallback();
        TemplateApiResult result = new TemplateApiResult(template);
        CreateCmdResult callbackResult = callback.getResult();
        if (callbackResult.isFailed()) {
            template.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            result.setResult(callbackResult.getResult());
            if (parentCallback != null) {
                parentCallback.complete(result);
            }
            return null;
        }

        // For multi-disk OVA, check and create data disk templates
        if (template.getFormat().equals(ImageFormat.OVA)) {
            if (!createOvaDataDiskTemplates(template, template.isDeployAsIs())) {
                template.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                result.setResult(callbackResult.getResult());
                if (parentCallback != null) {
                    parentCallback.complete(result);
                }
                return null;
            }
        }

        try {
            template.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } catch (Exception e) {
            result.setResult(e.toString());
            if (parentCallback != null) {
                parentCallback.complete(result);
            }
            return null;
        }

        if (parentCallback != null) {
            parentCallback.complete(result);
        }
        return null;
    }

    @Override
    public List<DatadiskTO> getTemplateDatadisksOnImageStore(TemplateInfo templateInfo, String configurationId) {
        ImageStoreEntity tmpltStore = (ImageStoreEntity)templateInfo.getDataStore();
        return tmpltStore.getDataDiskTemplates(templateInfo, configurationId);
    }

    @Override
    public boolean createOvaDataDiskTemplates(TemplateInfo parentTemplate, boolean deployAsIs) {
        try {
            // Get Datadisk template (if any) for OVA
            List<DatadiskTO> dataDiskTemplates = new ArrayList<DatadiskTO>();
            ImageStoreEntity tmpltStore = (ImageStoreEntity)parentTemplate.getDataStore();
            dataDiskTemplates = tmpltStore.getDataDiskTemplates(parentTemplate, null);
            int diskCount = 0;
            VMTemplateVO templateVO = _templateDao.findById(parentTemplate.getId());
            _templateDao.loadDetails(templateVO);
            DataStore imageStore = parentTemplate.getDataStore();
            Map<String, String> details = parentTemplate.getDetails();
            if (details == null) {
                details = templateVO.getDetails();
                if (details == null) {
                    details = new HashMap<>();
                }
            }

            for (DatadiskTO diskTemplate : dataDiskTemplates) {
                if (!deployAsIs) {
                    if (!diskTemplate.isBootable()) {
                        createChildDataDiskTemplate(diskTemplate, templateVO, parentTemplate, imageStore, diskCount++);
                        if (!diskTemplate.isIso() && StringUtils.isEmpty(details.get(VmDetailConstants.DATA_DISK_CONTROLLER))){
                            details.put(VmDetailConstants.DATA_DISK_CONTROLLER, getOvaDiskControllerDetails(diskTemplate, false));
                            details.put(VmDetailConstants.DATA_DISK_CONTROLLER + diskTemplate.getDiskId(), getOvaDiskControllerDetails(diskTemplate, false));
                        }
                    } else {
                        finalizeParentTemplate(diskTemplate, templateVO, parentTemplate, imageStore, diskCount++);
                        if (StringUtils.isEmpty(VmDetailConstants.ROOT_DISK_CONTROLLER)) {
                            final String rootDiskController = getOvaDiskControllerDetails(diskTemplate, true);
                            if (StringUtils.isNotEmpty(rootDiskController)) {
                                details.put(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDiskController);
                            }
                        }
                    }
                }
            }

            templateVO.setDetails(details);
            _templateDao.saveDetails(templateVO);
            return true;
        } catch (CloudRuntimeException | InterruptedException | ExecutionException e) {
            return false;
        }
    }

    private boolean createChildDataDiskTemplate(DatadiskTO dataDiskTemplate, VMTemplateVO template, TemplateInfo parentTemplate, DataStore imageStore, int diskCount) throws ExecutionException, InterruptedException {
        // Make an entry in vm_template table
        Storage.ImageFormat format = dataDiskTemplate.isIso() ? Storage.ImageFormat.ISO : template.getFormat();
        String suffix = dataDiskTemplate.isIso() ? "-IsoDiskTemplate-" : "-DataDiskTemplate-";
        TemplateType ttype = dataDiskTemplate.isIso() ? TemplateType.ISODISK : TemplateType.DATADISK;
        final long templateId = _templateDao.getNextInSequence(Long.class, "id");
        long guestOsId = dataDiskTemplate.isIso() ? 1 : 0;
        String templateName = dataDiskTemplate.isIso() ? dataDiskTemplate.getPath().substring(dataDiskTemplate.getPath().lastIndexOf(File.separator) + 1) : template.getName() + suffix + diskCount;
        VMTemplateVO templateVO = new VMTemplateVO(templateId, templateName, format, false, false, false, ttype, template.getUrl(),
                template.requiresHvm(), template.getBits(), template.getAccountId(), null, templateName, false, guestOsId, false, template.getHypervisorType(), null,
                null, false, false, false, false);
        if (dataDiskTemplate.isIso()){
            templateVO.setUniqueName(templateName);
        }
        templateVO.setParentTemplateId(template.getId());
        templateVO.setSize(dataDiskTemplate.getVirtualSize());
        templateVO = _templateDao.persist(templateVO);
        // Make sync call to create Datadisk templates in image store
        TemplateApiResult result = null;
        TemplateInfo dataDiskTemplateInfo = imageFactory.getTemplate(templateVO.getId(), imageStore);
        AsyncCallFuture<TemplateApiResult> future = createDatadiskTemplateAsync(parentTemplate, dataDiskTemplateInfo, dataDiskTemplate.getPath(), dataDiskTemplate.getDiskId(),
                dataDiskTemplate.getFileSize(), dataDiskTemplate.isBootable());
        result = future.get();
        if (result.isSuccess()) {
            // Make an entry in template_zone_ref table
            if (imageStore.getScope().getScopeType() == ScopeType.REGION) {
                associateTemplateToZone(templateId, null);
            } else if (imageStore.getScope().getScopeType() == ScopeType.ZONE) {
                Long zoneId = ((ImageStoreEntity)imageStore).getDataCenterId();
                VMTemplateZoneVO templateZone = new VMTemplateZoneVO(zoneId, templateId, new Date());
                _vmTemplateZoneDao.persist(templateZone);
            }
            _resourceLimitMgr.incrementResourceCount(template.getAccountId(), ResourceType.secondary_storage, templateVO.getSize());
        } else {
            // Delete the Datadisk templates that were already created as they are now invalid
            s_logger.debug("Since creation of Datadisk template: " + templateVO.getId() + " failed, delete other Datadisk templates that were created as part of parent"
                    + " template download");
            TemplateInfo parentTemplateInfo = imageFactory.getTemplate(templateVO.getParentTemplateId(), imageStore);
            cleanupDatadiskTemplates(parentTemplateInfo);
        }
        return result.isSuccess();
    }

    private boolean finalizeParentTemplate(DatadiskTO dataDiskTemplate, VMTemplateVO templateVO, TemplateInfo parentTemplate, DataStore imageStore, int diskCount) throws ExecutionException, InterruptedException, CloudRuntimeException {
        TemplateInfo templateInfo = imageFactory.getTemplate(templateVO.getId(), imageStore);
        AsyncCallFuture<TemplateApiResult> templateFuture = createDatadiskTemplateAsync(parentTemplate, templateInfo, dataDiskTemplate.getPath(), dataDiskTemplate.getDiskId(),
                dataDiskTemplate.getFileSize(), dataDiskTemplate.isBootable());
        TemplateApiResult result = null;
        result = templateFuture.get();
        if (!result.isSuccess()) {
            s_logger.debug("Since creation of parent template: " + templateInfo.getId() + " failed, delete Datadisk templates that were created as part of parent"
                    + " template download");
            cleanupDatadiskTemplates(templateInfo);
        }
        return result.isSuccess();
    }

    private String getOvaDiskControllerDetails(DatadiskTO diskTemplate, boolean isRootDisk) {
        String controller = diskTemplate.getDiskController() ;
        String controllerSubType = diskTemplate.getDiskControllerSubType();

        if (controller != null) {
            controller = controller.toLowerCase();
        }

        if (controllerSubType != null) {
            controllerSubType = controllerSubType.toLowerCase();
        }

        if (StringUtils.isNotBlank(controller)) {
            if (controller.contains("ide")) {
                return "ide";
            }
            if (controller.contains("scsi")) {
                if (StringUtils.isNotBlank(controllerSubType)) {
                    if (controllerSubType.equals("lsilogicsas")) {
                        return "lsisas1068";
                    }
                    return controllerSubType;
                }
                if (!isRootDisk) {
                    return "scsi";
                }
            }
            if (!isRootDisk) {
                return "osdefault";
            }
        }

        // Root disk to use global setting vmware.root.disk.controller
        if (!isRootDisk) {
            return "scsi";
        }
        return controller;
    }

    private void cleanupDatadiskTemplates(TemplateInfo parentTemplateInfo) {
        DataStore imageStore = parentTemplateInfo.getDataStore();
        List<VMTemplateVO> datadiskTemplatesToDelete = _templateDao.listByParentTemplatetId(parentTemplateInfo.getId());
        for (VMTemplateVO datadiskTemplateToDelete: datadiskTemplatesToDelete) {
            s_logger.info("Delete template: " + datadiskTemplateToDelete.getId() + " from image store: " + imageStore.getName());
            AsyncCallFuture<TemplateApiResult> future = deleteTemplateAsync(imageFactory.getTemplate(datadiskTemplateToDelete.getId(), imageStore));
            try {
                TemplateApiResult result = future.get();
                if (!result.isSuccess()) {
                    s_logger.warn("Failed to delete datadisk template: " + datadiskTemplateToDelete + " from image store: " + imageStore.getName() + " due to: " + result.getResult());
                    break;
                }
                _vmTemplateZoneDao.deletePrimaryRecordsForTemplate(datadiskTemplateToDelete.getId());
                _resourceLimitMgr.decrementResourceCount(datadiskTemplateToDelete.getAccountId(), ResourceType.secondary_storage, datadiskTemplateToDelete.getSize());
            } catch (Exception e) {
                s_logger.debug("Delete datadisk template failed", e);
                throw new CloudRuntimeException("Delete template Failed", e);
            }
        }
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> deleteTemplateAsync(TemplateInfo template) {
        TemplateObject to = (TemplateObject)template;
        // update template_store_ref status
        to.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, to, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteTemplateCallback(null, null)).setContext(context);

        if (to.canBeDeletedFromDataStore()) {
            to.getDataStore().getDriver().deleteAsync(to.getDataStore(), to, caller);
        } else {
            CommandResult result = new CommandResult();
            caller.complete(result);
        }

        return future;
    }

    public Void deleteTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> callback, TemplateOpContext<TemplateApiResult> context) {
        CommandResult result = callback.getResult();
        TemplateObject vo = context.getTemplate();
        if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed);
        } else {
            vo.processEvent(Event.OperationFailed);
        }
        TemplateApiResult apiResult = new TemplateApiResult(vo);
        apiResult.setResult(result.getResult());
        apiResult.setSuccess(result.isSuccess());
        context.future.complete(apiResult);
        return null;
    }

    private AsyncCallFuture<TemplateApiResult> copyAsync(DataObject source, TemplateInfo template, DataStore store) {
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        DataObject templateOnStore = store.create(template);
        templateOnStore.processEvent(Event.CreateOnlyRequested);

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, (TemplateObject)templateOnStore, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null)).setContext(context);
        _motionSrv.copyAsync(source, templateOnStore, caller);
        return future;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromSnapshotAsync(SnapshotInfo snapshot, TemplateInfo template, DataStore store) {
        return copyAsync(snapshot, template, store);
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromVolumeAsync(VolumeInfo volume, TemplateInfo template, DataStore store) {
        return copyAsync(volume, template, store);
    }

    private AsyncCallFuture<TemplateApiResult> syncToRegionStoreAsync(TemplateInfo template, DataStore store) {
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        // no need to create entry on template_store_ref here, since entries are already created when prepareSecondaryStorageForMigration is invoked.
        // But we need to set default install path so that sync can be done in the right s3 path
        TemplateInfo templateOnStore = _templateFactory.getTemplate(template, store, null);
        String installPath =
                TemplateConstants.DEFAULT_TMPLT_ROOT_DIR + "/" + TemplateConstants.DEFAULT_TMPLT_FIRST_LEVEL_DIR + template.getAccountId() + "/" + template.getId() + "/" +
                        template.getUniqueName();
        ((TemplateObject)templateOnStore).setInstallPath(installPath);
        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, (TemplateObject)templateOnStore, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().syncTemplateCallBack(null, null)).setContext(context);
        _motionSrv.copyAsync(template, templateOnStore, caller);
        return future;
    }

    protected Void syncTemplateCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> callback, TemplateOpContext<TemplateApiResult> context) {
        TemplateInfo destTemplate = context.getTemplate();
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        TemplateApiResult res = new TemplateApiResult(destTemplate);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                // no change to existing template_store_ref, will try to re-sync later if other call triggers this sync operation, like copy template
            } else {
                // this will update install path properly, next time it will not sync anymore.
                destTemplate.processEvent(Event.OperationSuccessed, result.getAnswer());
            }
            future.complete(res);
        } catch (Exception e) {
            s_logger.debug("Failed to process sync template callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    // This routine is used to push templates currently on cache store, but not in region store to region store.
    // used in migrating existing NFS secondary storage to S3.
    @Override
    public void syncTemplateToRegionStore(long templateId, DataStore store) {
        if (_storeMgr.isRegionStore(store)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sync template " + templateId + " from cache to object store...");
            }
            // if template is on region wide object store, check if it is really downloaded there (by checking install_path). Sync template to region
            // wide store if it is not there physically.
            TemplateInfo tmplOnStore = _templateFactory.getTemplate(templateId, store);
            if (tmplOnStore == null) {
                throw new CloudRuntimeException("Cannot find an entry in template_store_ref for template " + templateId + " on region store: " + store.getName());
            }
            if (tmplOnStore.getInstallPath() == null || tmplOnStore.getInstallPath().length() == 0) {
                // template is not on region store yet, sync to region store
                TemplateInfo srcTemplate = _templateFactory.getReadyTemplateOnCache(templateId);
                if (srcTemplate == null) {
                    throw new CloudRuntimeException("Cannot find template " + templateId + "  on cache store");
                }
                AsyncCallFuture<TemplateApiResult> future = syncToRegionStoreAsync(srcTemplate, store);
                try {
                    TemplateApiResult result = future.get();
                    if (result.isFailed()) {
                        throw new CloudRuntimeException("sync template from cache to region wide store failed for image store " + store.getName() + ":" +
                                result.getResult());
                    }
                    _cacheMgr.releaseCacheObject(srcTemplate); // reduce reference count for template on cache, so it can recycled by schedule
                } catch (Exception ex) {
                    throw new CloudRuntimeException("sync template from cache to region wide store failed for image store " + store.getName());
                }
            }
        }
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> copyTemplate(TemplateInfo srcTemplate, DataStore destStore) {
        // for vmware template, we need to check if ova packing is needed, since template created from snapshot does not have .ova file
        // we invoke createEntityExtractURL to trigger ova packing. Ideally, we can directly use extractURL to pass to following createTemplate.
        // Need to understand what is the background to use two different urls for copy and extract.
        if (srcTemplate.getFormat() == ImageFormat.OVA){
            ImageStoreEntity tmpltStore = (ImageStoreEntity)srcTemplate.getDataStore();
            tmpltStore.createEntityExtractUrl(srcTemplate.getInstallPath(), srcTemplate.getFormat(), srcTemplate);
        }
        // generate a URL from source template ssvm to download to destination data store
        String url = generateCopyUrl(srcTemplate);
        if (url == null) {
            s_logger.warn("Unable to start/resume copy of template " + srcTemplate.getUniqueName() + " to " + destStore.getName() +
                    ", no secondary storage vm in running state in source zone");
            throw new CloudRuntimeException("No secondary VM in running state in source template zone ");
        }

        TemplateObject tmplForCopy = (TemplateObject)_templateFactory.getTemplate(srcTemplate, destStore, null);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Setting source template url to " + url);
        }
        tmplForCopy.setUrl(url);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Mark template_store_ref entry as Creating");
        }
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        DataObject templateOnStore = destStore.create(tmplForCopy);
        templateOnStore.processEvent(Event.CreateOnlyRequested);

        if (templateOnStore instanceof TemplateObject) {
            ((TemplateObject)templateOnStore).getImage().setChecksum(null);
        } // else we don't know what to do.

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Invoke datastore driver createAsync to create template on destination store");
        }
        try {
            TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, (TemplateObject)templateOnStore, future);
            AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyTemplateCrossZoneCallBack(null, null)).setContext(context);
            destStore.getDriver().createAsync(destStore, templateOnStore, caller);
        } catch (CloudRuntimeException ex) {
            // clean up already persisted template_store_ref entry in case of createTemplateCallback is never called
            TemplateDataStoreVO templateStoreVO = _vmTemplateStoreDao.findByStoreTemplate(destStore.getId(), srcTemplate.getId());
            if (templateStoreVO != null) {
                TemplateInfo tmplObj = _templateFactory.getTemplate(srcTemplate, destStore, null);
                tmplObj.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            TemplateApiResult res = new TemplateApiResult((TemplateObject)templateOnStore);
            res.setResult(ex.getMessage());
            future.complete(res);
        }
        return future;
    }

    private String generateCopyUrl(String ipAddress, String dir, String path) {
        String hostname = ipAddress;
        String scheme = "http";
        boolean _sslCopy = false;
        String sslCfg = _configDao.getValue(Config.SecStorageEncryptCopy.toString());
        String _ssvmUrlDomain = _configDao.getValue("secstorage.ssl.cert.domain");
        if (sslCfg != null) {
            _sslCopy = Boolean.parseBoolean(sslCfg);
        }
        if(_sslCopy && (_ssvmUrlDomain == null || _ssvmUrlDomain.isEmpty())){
            s_logger.warn("Empty secondary storage url domain, ignoring SSL");
            _sslCopy = false;
        }
        if (_sslCopy) {
            if(_ssvmUrlDomain.startsWith("*")) {
                hostname = ipAddress.replace(".", "-");
                hostname = hostname + _ssvmUrlDomain.substring(1);
            } else {
                hostname = _ssvmUrlDomain;
            }
            scheme = "https";
        }
        return scheme + "://" + hostname + "/copy/SecStorage/" + dir + "/" + path;
    }

    private String generateCopyUrl(TemplateInfo srcTemplate) {
        DataStore srcStore = srcTemplate.getDataStore();
        EndPoint ep = _epSelector.select(srcTemplate);
        if (ep != null) {
            if (ep.getPublicAddr() == null) {
                s_logger.warn("A running secondary storage vm has a null public ip?");
                return null;
            }
            return generateCopyUrl(ep.getPublicAddr(), ((ImageStoreEntity)srcStore).getMountPoint(), srcTemplate.getInstallPath());
        }

        VMTemplateVO tmplt = _templateDao.findById(srcTemplate.getId());
        HypervisorType hyperType = tmplt.getHypervisorType();
        /*No secondary storage vm yet*/
        if (hyperType != null && hyperType == HypervisorType.KVM) {
            return "file://" + ((ImageStoreEntity)srcStore).getMountPoint() + "/" + srcTemplate.getInstallPath();
        }
        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> prepareTemplateOnPrimary(TemplateInfo srcTemplate, StoragePool pool) {
        return copyAsync(srcTemplate, srcTemplate, (DataStore)pool);
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> deleteTemplateOnPrimary(TemplateInfo template, StoragePool pool) {
        TemplateObject templateObject = (TemplateObject)_templateFactory.getTemplateOnPrimaryStorage(template.getId(), (DataStore)pool, template.getDeployAsIsConfiguration());

        templateObject.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

        DataStore dataStore = _storeMgr.getPrimaryDataStore(pool.getId());

        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<>();
        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<>(null, templateObject, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);

        caller.setCallback(caller.getTarget().deleteTemplateCallback(null, null)).setContext(context);

        dataStore.getDriver().deleteAsync(dataStore, templateObject, caller);

        return future;
    }

    protected Void copyTemplateCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> callback, TemplateOpContext<TemplateApiResult> context) {
        TemplateInfo destTemplate = context.getTemplate();
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        TemplateApiResult res = new TemplateApiResult(destTemplate);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destTemplate.processEvent(Event.OperationFailed);
            } else {
                destTemplate.processEvent(Event.OperationSuccessed, result.getAnswer());
            }
            future.complete(res);
        } catch (Exception e) {
            s_logger.debug("Failed to process copy template callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    protected Void copyTemplateCrossZoneCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> callback, TemplateOpContext<TemplateApiResult> context) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Performing copy template cross zone callback after completion");
        }
        TemplateInfo destTemplate = context.getTemplate();
        CreateCmdResult result = callback.getResult();
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        TemplateApiResult res = new TemplateApiResult(destTemplate);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destTemplate.processEvent(Event.OperationFailed);
            } else {
                destTemplate.processEvent(Event.OperationSuccessed, result.getAnswer());
            }
            future.complete(res);
        } catch (Exception e) {
            s_logger.debug("Failed to process copy template cross zones callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    @Override
    public void addSystemVMTemplatesToSecondary(DataStore store) {
        long storeId = store.getId();
        List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
        for (VMTemplateVO tmplt : rtngTmplts) {
            TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());
            if (tmpltStore == null) {
                if (_vmTemplateStoreDao.isTemplateMarkedForDirectDownload(tmplt.getId())) {
                    continue;
                }
                tmpltStore =
                        new TemplateDataStoreVO(storeId, tmplt.getId(), new Date(), 100, Status.DOWNLOADED, null, null, null,
                                TemplateConstants.DEFAULT_SYSTEM_VM_TEMPLATE_PATH + tmplt.getId() + '/', tmplt.getUrl());
                tmpltStore.setSize(0L);
                tmpltStore.setPhysicalSize(0); // no size information for
                // pre-seeded system vm templates
                tmpltStore.setDataStoreRole(store.getRole());
                _vmTemplateStoreDao.persist(tmpltStore);
            }
        }
    }

    private class CreateDataDiskTemplateContext<T> extends AsyncRpcContext<T> {
        private final DataObject dataDiskTemplate;
        private final AsyncCallFuture<TemplateApiResult> future;

        public CreateDataDiskTemplateContext(AsyncCompletionCallback<T> callback, DataObject dataDiskTemplate, AsyncCallFuture<TemplateApiResult> future) {
            super(callback);
            this.dataDiskTemplate = dataDiskTemplate;
            this.future = future;
        }

        public AsyncCallFuture<TemplateApiResult> getFuture() {
            return this.future;
        }
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createDatadiskTemplateAsync(TemplateInfo parentTemplate, TemplateInfo dataDiskTemplate, String path, String diskId, long fileSize, boolean bootable) {
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        // Make an entry for disk template in template_store_ref table
        DataStore store = parentTemplate.getDataStore();
        TemplateObject dataDiskTemplateOnStore;
        if (!bootable) {
            dataDiskTemplateOnStore = (TemplateObject)store.create(dataDiskTemplate);
            dataDiskTemplateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);
        } else {
            dataDiskTemplateOnStore = (TemplateObject) imageFactory.getTemplate(parentTemplate, store, null);
        }
        try {
            CreateDataDiskTemplateContext<TemplateApiResult> context = new CreateDataDiskTemplateContext<TemplateApiResult>(null, dataDiskTemplateOnStore, future);
            AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createDatadiskTemplateCallback(null, null)).setContext(context);
            ImageStoreEntity tmpltStore = (ImageStoreEntity)parentTemplate.getDataStore();
            tmpltStore.createDataDiskTemplateAsync(dataDiskTemplate, path, diskId, fileSize, bootable, caller);
        } catch (CloudRuntimeException ex) {
            dataDiskTemplateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            TemplateApiResult result = new TemplateApiResult(dataDiskTemplate);
            result.setResult(ex.getMessage());
            if (future != null) {
                future.complete(result);
            }
        }
        return future;
    }

    protected Void createDatadiskTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> callback,
                                                  CreateDataDiskTemplateContext<TemplateApiResult> context) {
        DataObject dataDiskTemplate = context.dataDiskTemplate;
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        CreateCmdResult result = callback.getResult();
        TemplateApiResult dataDiskTemplateResult = new TemplateApiResult((TemplateObject)dataDiskTemplate);
        try {
            if (result.isSuccess()) {
                dataDiskTemplate.processEvent(Event.OperationSuccessed, result.getAnswer());
            } else {
                dataDiskTemplate.processEvent(Event.OperationFailed);
                dataDiskTemplateResult.setResult(result.getResult());
            }
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to process create template callback", e);
            dataDiskTemplateResult.setResult(e.toString());
        }
        future.complete(dataDiskTemplateResult);
        return null;
    }
}
