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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;

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
    VMTemplatePoolDao _tmpltPoolDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    TemplateManager _tmpltMgr;
    @Inject
    ConfigurationDao _configDao;

    class TemplateOpContext<T> extends AsyncRpcContext<T> {
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

        public AsyncCallFuture<TemplateApiResult> getFuture() {
            return future;
        }

    }

    @Override
    public void createTemplateAsync(TemplateInfo template, DataStore store,
            AsyncCompletionCallback<TemplateApiResult> callback) {
        // persist template_store_ref entry
        TemplateObject templateOnStore = (TemplateObject) store.create(template);
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

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(callback,
                templateOnStore, null);

        AsyncCallbackDispatcher<TemplateServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createTemplateCallback(null, null)).setContext(context);
        store.getDriver().createAsync(store, templateOnStore, caller);
    }

    @Override
    public void downloadBootstrapSysTemplate(DataStore store) {
        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();

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
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao
                        .findByStoreTemplate(store.getId(), template.getId());
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
        List<DataStore> stores = _storeMgr.getImageStoresByScope(new ZoneScope(dcId));
        if (stores == null || stores.isEmpty()) {
            return;
        }

        /* Download all the templates in zone with the same hypervisortype */
        for (DataStore store : stores) {
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

            for (VMTemplateVO template : toBeDownloaded) {
                TemplateDataStoreVO tmpltHost = _vmTemplateStoreDao
                        .findByStoreTemplate(store.getId(), template.getId());
                if (tmpltHost == null || tmpltHost.getState() != ObjectInDataStoreStateMachine.State.Ready) {
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
        Long zoneId = store.getScope().getScopeId();

        Map<String, TemplateProp> templateInfos = listTemplate(store);
        if (templateInfos == null) {
            return;
        }

        Set<VMTemplateVO> toBeDownloaded = new HashSet<VMTemplateVO>();
        List<VMTemplateVO> allTemplates = null;
        if (zoneId == null) {
            // region wide store
            allTemplates = _templateDao.listAllActive();
        } else {
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
                    s_logger.info("Template Sync found " + uniqueName + " already in the image store");
                    if (tmpltStore.getDownloadState() != Status.DOWNLOADED) {
                        tmpltStore.setErrorString("");
                    }
                    if (tmpltInfo.isCorrupted()) {
                        tmpltStore.setDownloadState(Status.DOWNLOAD_ERROR);
                        String msg = "Template " + tmplt.getName() + ":" + tmplt.getId()
                                + " is corrupted on secondary storage " + tmpltStore.getId();
                        tmpltStore.setErrorString(msg);
                        s_logger.info("msg");
                        if (tmplt.getUrl() == null) {
                            msg = "Private Template (" + tmplt + ") with install path " + tmpltInfo.getInstallPath()
                                    + "is corrupted, please check in image store: " + tmpltStore.getDataStoreId();
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
                        // update size in vm_template table
                        VMTemplateVO tmlpt = _templateDao.findById(tmplt.getId());
                        tmlpt.setSize(tmpltInfo.getSize());
                        _templateDao.update(tmplt.getId(), tmlpt);

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
                                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, zoneId, null,
                                        e.getMessage(), e.getMessage());
                            } finally {
                                _resourceLimitMgr.recalculateResourceCount(accountId, _accountMgr.getAccount(accountId)
                                        .getDomainId(), com.cloud.configuration.Resource.ResourceType.secondary_storage
                                        .getOrdinal());
                            }
                        }
                    }
                    _vmTemplateStoreDao.update(tmpltStore.getId(), tmpltStore);
                } else {
                    tmpltStore = new TemplateDataStoreVO(storeId, tmplt.getId(), new Date(), 100, Status.DOWNLOADED,
                            null, null, null, tmpltInfo.getInstallPath(), tmplt.getUrl());
                    tmpltStore.setSize(tmpltInfo.getSize());
                    tmpltStore.setPhysicalSize(tmpltInfo.getPhysicalSize());
                    tmpltStore.setDataStoreRole(store.getRole());
                    _vmTemplateStoreDao.persist(tmpltStore);

                    // update size in vm_template table
                    VMTemplateVO tmlpt = _templateDao.findById(tmplt.getId());
                    tmlpt.setSize(tmpltInfo.getSize());
                    _templateDao.update(tmplt.getId(), tmlpt);
                    associateTemplateToZone(tmplt.getId(), zoneId);


                }
            } else {
                if (tmpltStore != null) {
                    s_logger.info("Template Sync did not find " + uniqueName + " on image store " + storeId
                            + ", may request download based on available hypervisor types");
                    s_logger.info("Removing leftover template " + uniqueName + " entry from template store table");
                    // remove those leftover entries
                    _vmTemplateStoreDao.remove(tmpltStore.getId());
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
                if (tmplt.getUrl() == null) { // If url is null we can't
                    // initiate the download
                    continue;
                }

                // if this is private template, skip
                if (!tmplt.isPublicTemplate() && !tmplt.isFeatured()) {
                    continue;
                }
                if (availHypers.contains(tmplt.getHypervisorType())) {
                    s_logger.info("Downloading template " + tmplt.getUniqueName() + " to image store "
                            + store.getName());
                    associateTemplateToZone(tmplt.getId(), zoneId);
                    TemplateInfo tmpl = _templateFactory.getTemplate(tmplt.getId(), DataStoreRole.Image);
                    createTemplateAsync(tmpl, store, null);
                }
            }
        }

        for (String uniqueName : templateInfos.keySet()) {
            TemplateProp tInfo = templateInfos.get(uniqueName);
            if (_tmpltMgr.templateIsDeleteable(tInfo.getId())) {
                // we cannot directly call deleteTemplateSync here to
                // reuse delete logic since in this case, our db does not have
                // this template at all.
                TemplateObjectTO tmplTO = new TemplateObjectTO();
                tmplTO.setDataStore(store.getTO());
                tmplTO.setPath(tInfo.getInstallPath());
                tmplTO.setId(tInfo.getId());
                DeleteCommand dtCommand = new DeleteCommand(tmplTO);
                EndPoint ep = _epSelector.select(store);
                Answer answer = ep.sendMessage(dtCommand);
                if (answer == null || !answer.getResult()) {
                    s_logger.info("Failed to deleted template at store: " + store.getName());

                } else {
                    String description = "Deleted template " + tInfo.getTemplateName() + " on secondary storage "
                            + storeId;
                    s_logger.info(description);
                }

            }
        }

    }

    // persist entry in template_zone_ref table. zoneId can be empty for
    // region-wide image store, in that case,
    // we will associate the template to all the zones.
    private void associateTemplateToZone(long templateId, Long zoneId) {
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

    private Map<String, TemplateProp> listTemplate(DataStore ssStore) {
        ListTemplateCommand cmd = new ListTemplateCommand(ssStore.getTO());
        EndPoint ep = _epSelector.select(ssStore);
        Answer answer = ep.sendMessage(cmd);
        if (answer != null && answer.getResult()) {
            ListTemplateAnswer tanswer = (ListTemplateAnswer) answer;
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
    public AsyncCallFuture<TemplateApiResult> deleteTemplateAsync(TemplateInfo template) {
        TemplateObject to = (TemplateObject) template;
        // update template_store_ref status
        to.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null, to, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteTemplateCallback(null, null)).setContext(context);
        to.getDataStore().getDriver().deleteAsync(to.getDataStore(), to, caller);
        return future;
    }

    public Void deleteTemplateCallback(AsyncCallbackDispatcher<TemplateServiceImpl, CommandResult> callback,
            TemplateOpContext<TemplateApiResult> context) {
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

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null,
                (TemplateObject) templateOnStore, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null)).setContext(context);
        _motionSrv.copyAsync(source, templateOnStore, caller);
        return future;
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromSnapshotAsync(SnapshotInfo snapshot,
            TemplateInfo template, DataStore store) {
        return copyAsync(snapshot, template, store);
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> createTemplateFromVolumeAsync(VolumeInfo volume, TemplateInfo template,
            DataStore store) {
        return copyAsync(volume, template, store);
    }

    @Override
    public AsyncCallFuture<TemplateApiResult> copyTemplate(TemplateInfo srcTemplate, DataStore destStore) {
        // generate a URL from source template ssvm to download to destination data store
        String url = generateCopyUrl(srcTemplate);
        if (url == null) {
            s_logger.warn("Unable to start/resume copy of template " + srcTemplate.getUniqueName() + " to " + destStore.getName() + ", no secondary storage vm in running state in source zone");
            throw new CloudRuntimeException("No secondary VM in running state in source template zone ");
        }

        TemplateObject tmplForCopy = (TemplateObject)_templateFactory.getTemplate(srcTemplate, destStore);
        tmplForCopy.setUrl(url);

        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        DataObject templateOnStore = destStore.create(tmplForCopy);
        templateOnStore.processEvent(Event.CreateOnlyRequested);

        TemplateOpContext<TemplateApiResult> context = new TemplateOpContext<TemplateApiResult>(null,
                (TemplateObject) templateOnStore, future);
        AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyTemplateCallBack(null, null)).setContext(context);
        destStore.getDriver().createAsync(destStore, templateOnStore, caller);
        return future;
    }


    private String generateCopyUrl(String ipAddress, String dir, String path){
        String hostname = ipAddress;
        String scheme = "http";
        boolean _sslCopy = false;
        String sslCfg = _configDao.getValue(Config.SecStorageEncryptCopy.toString());
        if ( sslCfg != null ){
            _sslCopy = Boolean.parseBoolean(sslCfg);
        }
        if (_sslCopy) {
            hostname = ipAddress.replace(".", "-");
            hostname = hostname + ".realhostip.com";
            scheme = "https";
        }
        return scheme + "://" + hostname + "/copy/SecStorage/" + dir + "/" + path;
    }

    private String generateCopyUrl(TemplateInfo  srcTemplate) {
        DataStore srcStore = srcTemplate.getDataStore();
        EndPoint ep = _epSelector.select(srcTemplate);
        if ( ep != null ){
            if (ep.getPublicAddr() == null) {
                s_logger.warn("A running secondary storage vm has a null public ip?");
                return null;
            }
            return generateCopyUrl(ep.getPublicAddr(), ((ImageStoreEntity) srcStore).getMountPoint(), srcTemplate.getInstallPath());
        }

        VMTemplateVO tmplt = _templateDao.findById(srcTemplate.getId());
        HypervisorType hyperType = tmplt.getHypervisorType();
        /*No secondary storage vm yet*/
        if (hyperType != null && hyperType == HypervisorType.KVM) {
            return "file://" + ((ImageStoreEntity) srcStore).getMountPoint() + "/" + srcTemplate.getInstallPath();
        }
        return null;
    }


    @Override
    public AsyncCallFuture<TemplateApiResult> prepareTemplateOnPrimary(TemplateInfo srcTemplate, StoragePool pool) {
        return copyAsync(srcTemplate, srcTemplate, (DataStore) pool);
    }

    protected Void copyTemplateCallBack(AsyncCallbackDispatcher<TemplateServiceImpl, CopyCommandResult> callback,
            TemplateOpContext<TemplateApiResult> context) {
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

    @Override
    public void addSystemVMTemplatesToSecondary(DataStore store) {
        long storeId = store.getId();
        List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
        for (VMTemplateVO tmplt : rtngTmplts) {
            TemplateDataStoreVO tmpltStore = _vmTemplateStoreDao.findByStoreTemplate(storeId, tmplt.getId());
            if (tmpltStore == null) {
                tmpltStore = new TemplateDataStoreVO(storeId, tmplt.getId(), new Date(), 100, Status.DOWNLOADED, null,
                        null, null, TemplateConstants.DEFAULT_SYSTEM_VM_TEMPLATE_PATH + tmplt.getId() + File.separator,
                        tmplt.getUrl());
                tmpltStore.setSize(0L);
                tmpltStore.setPhysicalSize(0); // no size information for
                // pre-seeded system vm templates
                tmpltStore.setDataStoreRole(store.getRole());
                _vmTemplateStoreDao.persist(tmpltStore);
            }
        }
    }
}
