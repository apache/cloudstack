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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
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
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateEvent;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.alert.AlertManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.template.TemplateProp;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.UriUtils;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

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
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    DownloadMonitor _dlMonitor;
    @Inject
    AgentManager _agentMgr;
    @Inject
    SecondaryStorageVmManager _ssvmMgr;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    TemplateDataFactory _templateFactory;
    @Inject VMTemplatePoolDao _tmpltPoolDao;
    @Inject
    EndPointSelector _epSelector;

    class TemplateOpContext<T> extends AsyncRpcConext<T> {
        final TemplateObject template;
        final AsyncCallFuture<TemplateApiResult> future;
        public TemplateOpContext(AsyncCompletionCallback<T> callback, TemplateObject template,
        		AsyncCallFuture<TemplateApiResult> future) {
            super(callback);
            this.template = template;
            this.future = future;
        }

        public TemplateObject getTemplate() {
            return template;
        }
    }

    @Override
    public void createTemplateAsync(
            TemplateInfo template, DataStore store, AsyncCompletionCallback<TemplateApiResult> callback) {
        // persist template_store_ref entry
        DataObject templateOnStore = store.create(template);
        // update template_store_ref state
        templateOnStore.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(callback,
                (TemplateObject)templateOnStore, null);

        AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createTemplateCallback(null, null)).setContext(context);
        store.getDriver().createAsync(templateOnStore, caller);
    }

    @Override
    public void downloadBootstrapSysTemplate(DataStore store) {
        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();

        List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();

        for (VMTemplateVO rtngTmplt : rtngTmplts) {
            toBeDownloaded.add(rtngTmplt);
        }

        for (VMTemplateVO template : toBeDownloaded) {
            TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
            if (tmpltHost == null || tmpltHost.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            	TemplateInfo tmplt = this._templateFactory.getTemplate(template.getId());
                this.createTemplateAsync(tmplt, store, null);
            }
        }
    }

    @Override
    public void handleSysTemplateDownload(HypervisorType hostHyper, Long dcId) {
        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();
        List<DataStore> ssHosts = this._storeMgr.getImageStoresByScope(new ZoneScope(dcId));
        if (ssHosts == null || ssHosts.isEmpty()){
            return;
        }

        /*Download all the templates in zone with the same hypervisortype*/
        for ( DataStore ssHost : ssHosts) {
            List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
            List<VMTemplateVO> defaultBuiltin = _templateDao.listDefaultBuiltinTemplates();


            for (VMTemplateVO rtngTmplt : rtngTmplts) {
                if (rtngTmplt.getHypervisorType() == hostHyper) {
                    toBeDownloaded.add(rtngTmplt);
                }
            }

            for (VMTemplateVO builtinTmplt : defaultBuiltin) {
                if (builtinTmplt.getHypervisorType() == hostHyper) {
                    toBeDownloaded.add(builtinTmplt);
                }
            }

            for (VMTemplateVO template: toBeDownloaded) {
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao.findByStoreTemplate(ssHost.getId(), template.getId());
                if (tmpltHost == null || tmpltHost.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                	DataObject tmpl = this._templateFactory.getTemplate(template.getId(), ssHost);
                    _dlMonitor.downloadTemplateToStorage(tmpl, ssHost, null);
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
        Long zoneId = store.getScope().getScopeId();

        Map<String, TemplateProp> templateInfos = listTemplate(store);
        if (templateInfos == null) {
            return;
        }

        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();
        List<VMTemplateVO> allTemplates = null;
        if (zoneId == null){
            // region wide store
            allTemplates = _templateDao.listAll();
        }
        else{
            // zone wide store
            allTemplates = _templateDao.listAllInZone(zoneId);
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

        toBeDownloaded.addAll(allTemplates);

        for (VMTemplateVO tmplt : allTemplates) {
            String uniqueName = tmplt.getUniqueName();
            TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());
            if (templateInfos.containsKey(uniqueName)) {
                TemplateProp tmpltInfo = templateInfos.remove(uniqueName);
                toBeDownloaded.remove(tmplt);
                if (tmpltStore != null) {
                    s_logger.info("Template Sync found " + uniqueName + " already in the template host table");
                    if (tmpltStore.getDownloadState() != Status.DOWNLOADED) {
                        tmpltStore.setErrorString("");
                    }
                    if (tmpltInfo.isCorrupted()) {
                        tmpltStore.setDownloadState(Status.DOWNLOAD_ERROR);
                        String msg = "Template " + tmplt.getName() + ":" + tmplt.getId() + " is corrupted on secondary storage " + tmpltStore.getId();
                        tmpltStore.setErrorString(msg);
                        s_logger.info("msg");
                        if (tmplt.getUrl() == null) {
                            msg = "Private Template (" + tmplt + ") with install path " + tmpltInfo.getInstallPath() + "is corrupted, please check in image store: " + tmpltStore.getDataStoreId();
                            s_logger.warn(msg);
                        } else {
                            toBeDownloaded.add(tmplt);
                        }

                    } else {
                        tmpltStore.setDownloadPercent(100);
                        tmpltStore.setDownloadState(Status.DOWNLOADED);
                        tmpltStore.setInstallPath(tmpltInfo.getInstallPath());
                        tmpltStore.setSize(tmpltInfo.getSize());
                        tmpltStore.setPhysicalSize(tmpltInfo.getPhysicalSize());
                        tmpltStore.setLastUpdated(new Date());

                        if (tmpltInfo.getSize() > 0) {
                            long accountId = tmplt.getAccountId();
                            try {
                                _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(accountId),
                                        com.cloud.configuration.Resource.ResourceType.secondary_storage,
                                        tmpltInfo.getSize() - UriUtils.getRemoteSize(tmplt.getUrl()));
                            } catch (ResourceAllocationException e) {
                                s_logger.warn(e.getMessage());
                                _alertMgr.sendAlert(_alertMgr.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, zoneId,
                                        null, e.getMessage(), e.getMessage());
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
                    _vmTemplateStoreDao.persist(tmpltStore);
                    this.associateTemplateToZone(tmplt.getId(), zoneId);
                }

                continue;
            }
            if (tmpltStore != null && tmpltStore.getDownloadState() != Status.DOWNLOADED) {
                s_logger.info("Template Sync did not find " + uniqueName + " ready on server " + storeId + ", will request download to start/resume shortly");

            } else if (tmpltStore == null) {
                s_logger.info("Template Sync did not find " + uniqueName + " on the server " + storeId + ", will request download shortly");
                TemplateDataStoreVO templtStore = new TemplateDataStoreVO(storeId, tmplt.getId(), new Date(), 0, Status.NOT_DOWNLOADED, null, null, null, null, tmplt.getUrl());
                _vmTemplateStoreDao.persist(templtStore);
                this.associateTemplateToZone(tmplt.getId(), zoneId);
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
                if (tmplt.getUrl() == null) { // If url is null we can't
                                              // initiate the download
                    continue;
                }
                // check if there is a record for this template in this store
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());

                // if this is private template, and there is no record for this
                // template in this store, skip
                if (!tmplt.isPublicTemplate() && !tmplt.isFeatured()) {
                    if (tmpltHost == null) {
                        continue;
                    }
                }
                if (availHypers.contains(tmplt.getHypervisorType())) {
                     if (tmpltHost != null ) {
                        continue;
                    }
                    s_logger.debug("Template " + tmplt.getName() + " needs to be downloaded to " + store.getName());
                    //TODO: we should pass a callback here
                    DataObject tmpl = this._templateFactory.getTemplate(tmplt.getId(), store);
                    _dlMonitor.downloadTemplateToStorage(tmpl, store, null);
                }
            }
        }

        for (String uniqueName : templateInfos.keySet()) {
            TemplateProp tInfo = templateInfos.get(uniqueName);
            List<UserVmVO> userVmUsingIso = _userVmDao.listByIsoId(tInfo.getId());
            //check if there is any Vm using this ISO.
            if (userVmUsingIso == null || userVmUsingIso.isEmpty()) {
                deleteTemplateAsync(_templateFactory.getTemplate(tInfo.getId(), store));
                String description = "Deleted template " + tInfo.getTemplateName() + " on secondary storage " + storeId;
                s_logger.info(description);
            }
        }

    }


    // persist entry in template_zone_ref table. zoneId can be empty for region-wide image store, in that case,
    // we will associate the template to all the zones.
    private void associateTemplateToZone(long templateId, Long zoneId){
        List<Long> dcs = new ArrayList<Long>();
        if (zoneId != null ){
            dcs.add(zoneId);
        }
        else{
            List<DataCenterVO> zones = _dcDao.listAll();
            for (DataCenterVO zone : zones){
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


    private Map<String, TemplateProp> listTemplate(DataStore ssStore) {
        ListTemplateCommand cmd = new ListTemplateCommand(ssStore.getUri());
        EndPoint ep = _epSelector.select(ssStore);
        Answer answer = ep.sendMessage(cmd);
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


    protected Void createTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> callback,
            TemplateOpContext<TemplateApiResult> context) {
        TemplateObject template = (TemplateObject)context.getTemplate();
        AsyncCompletionCallback<TemplateApiResult> parentCallback = context.getParentCallback();
        TemplateApiResult result = new TemplateApiResult(template);
        CreateCmdResult callbackResult = callback.getResult();
        if (callbackResult.isFailed()) {
            try {
                template.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                template.stateTransit(TemplateEvent.OperationFailed);
            } catch (NoTransitionException e) {
               s_logger.debug("Failed to update template state", e);
            }
            result.setResult(callbackResult.getResult());
            parentCallback.complete(result);
            return null;
        }

        try {
            template.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
            template.stateTransit(TemplateEvent.OperationSucceeded);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to transit state", e);
            result.setResult(e.toString());
            parentCallback.complete(result);
            return null;
        }

        parentCallback.complete(result);
        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> deleteTemplateAsync(
            TemplateInfo template) {
        TemplateObject to = (TemplateObject) template;
        // update template_store_ref status
        to.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, to, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteTemplateCallback(null, null)).setContext(context);
        to.getDataStore().getDriver().deleteAsync(to, caller);
        return future;
    }

    public Void deleteTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, TemplateApiResult> callback, TemplateOpContext<TemplateApiResult> context) {
        TemplateApiResult result = callback.getResult();
        TemplateObject vo = context.getTemplate();
        // we can only update state in template_store_ref table
         if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed);
        } else {
            vo.processEvent(Event.OperationFailed);
         }
        context.future.complete(result);
        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromSnapshotAsync(
            SnapshotInfo snapshot, TemplateInfo template, DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromVolumeAsync(
            VolumeInfo volume, TemplateInfo template, DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> copyTemplate(TemplateInfo srcTemplate,
            DataStore destStore) {
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        TemplateApiResult res = new TemplateApiResult(srcTemplate);
        try{
        // create one entry in template_store_ref
            TemplateDataStoreVO destTmpltStore = _vmTemplateStoreDao.findByStoreTemplate(destStore.getId(), srcTemplate.getId());
            if (destTmpltStore == null) {
                destTmpltStore = new TemplateDataStoreVO(destStore.getId(), srcTemplate.getId());
                destTmpltStore.setCopy(true);
                _vmTemplateStoreDao.persist(destTmpltStore);
            }
            TemplateInfo destTemplate = this._templateFactory.getTemplate(destTmpltStore.getTemplateId(), destStore);
            destTemplate.processEvent(Event.CreateOnlyRequested);
            srcTemplate.processEvent(Event.CopyingRequested);

            CopyTemplateContext<TemplateApiResult> context = new CopyTemplateContext<TemplateApiResult>(null, future, srcTemplate,
                    destTemplate,
                    destStore);
            AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null))
            .setContext(context);
            this._motionSrv.copyAsync(srcTemplate, destTemplate, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to copy template", e);
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }

    protected Void copyTemplateCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> callback,
            CopyTemplateContext<TemplateApiResult> context) {
        TemplateInfo srcTemplate = context.getSrcTemplate();
        TemplateInfo destTemplate = context.getDestTemplate();
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        TemplateApiResult res = new TemplateApiResult(destTemplate);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destTemplate.processEvent(Event.OperationFailed);
                srcTemplate.processEvent(Event.OperationFailed);
                // remove entry from template_store_ref
                TemplateDataStoreVO destTmpltStore = _vmTemplateStoreDao.findByStoreTemplate(context.getDestStore().getId(), destTemplate.getId());
                _vmTemplateStoreDao.remove(destTmpltStore.getId());
                future.complete(res);
                return null;
            }
            srcTemplate.processEvent(Event.OperationSuccessed);
            destTemplate.processEvent(Event.OperationSuccessed);
            future.complete(res);
            return null;
        } catch (Exception e) {
            s_logger.debug("Failed to process copy template callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    protected Void prepareTemplateCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> callback,
            CopyTemplateContext<TemplateApiResult> context) {
        TemplateInfo srcTemplate = context.getSrcTemplate();
        TemplateInfo destTemplate = context.getDestTemplate();
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<TemplateApiResult> future = context.getFuture();
        TemplateApiResult res = new TemplateApiResult(destTemplate);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destTemplate.processEvent(Event.OperationFailed);
                srcTemplate.processEvent(Event.OperationFailed);
                // remove entry from template_spool_ref
                VMTemplateStoragePoolVO destTmpltPool =  _tmpltPoolDao.findByPoolTemplate(context.getDestStore().getId(), destTemplate.getId());
                _vmTemplateStoreDao.remove(destTmpltPool.getId());
                future.complete(res);
                return null;
            }
            srcTemplate.processEvent(Event.OperationSuccessed);
            // update other information in template_spool_ref through templateObject event processing.
            destTemplate.processEvent(Event.OperationSuccessed);
            future.complete(res);
            return null;
        } catch (Exception e) {
            s_logger.debug("Failed to process prepare template callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> prepareTemplateOnPrimary(TemplateInfo srcTemplate, StoragePool pool) {
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        TemplateApiResult res = new TemplateApiResult(srcTemplate);
        long poolId = pool.getId();
        long templateId = srcTemplate.getId();
        try{
            // create one entry in template_spool_ref
            VMTemplateStoragePoolVO templateStoragePoolRef = _tmpltPoolDao.findByPoolTemplate(poolId, templateId);
            if (templateStoragePoolRef == null) {
                templateStoragePoolRef = new VMTemplateStoragePoolVO(poolId, templateId);
                templateStoragePoolRef = _tmpltPoolDao.persist(templateStoragePoolRef);
            }
            DataStore destStore = (DataStore)pool;
            TemplateInfo destTemplate = this._templateFactory.getTemplate(templateStoragePoolRef.getTemplateId(), destStore);
            destTemplate.processEvent(Event.CreateOnlyRequested);
            srcTemplate.processEvent(Event.CopyingRequested);

            CopyTemplateContext<TemplateApiResult> context = new CopyTemplateContext<TemplateApiResult>(null, future, srcTemplate,
                    destTemplate,
                    destStore
                    );
            AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().prepareTemplateCallBack(null, null))
            .setContext(context);
            this._motionSrv.copyAsync(srcTemplate, destTemplate, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to prepare template on storage pool", e);
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }



    class CopyTemplateContext<T> extends AsyncRpcConext<T> {
        final TemplateInfo srcTemplate;
        final TemplateInfo destTemplate;
        final DataStore destStore;
        final AsyncCallFuture<TemplateApiResult> future;

        /**
         * @param callback
         */
        public CopyTemplateContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<TemplateApiResult> future, TemplateInfo srcTemplate,
                TemplateInfo destTemplate, DataStore destStore) {
            super(callback);
            this.srcTemplate = srcTemplate;
            this.destTemplate = destTemplate;
            this.destStore = destStore;
            this.future = future;
        }

        public TemplateInfo getSrcTemplate() {
            return srcTemplate;
        }

        public TemplateInfo getDestTemplate() {
            return destTemplate;
        }

        public DataStore getDestStore() {
            return destStore;
        }

        public AsyncCallFuture<TemplateApiResult> getFuture() {
            return future;
        }
    }
}
