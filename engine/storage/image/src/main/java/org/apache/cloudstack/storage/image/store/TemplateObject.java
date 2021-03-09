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
package org.apache.cloudstack.storage.image.store;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import com.cloud.agent.api.storage.CreateDatadiskTemplateAnswer;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

import com.google.common.base.Strings;

@SuppressWarnings("serial")
public class TemplateObject implements TemplateInfo {
    private static final Logger s_logger = Logger.getLogger(TemplateObject.class);
    private VMTemplateVO imageVO;
    private DataStore dataStore;
    private String url;
    private String installPath; // temporarily set installPath before passing to resource for entries with empty installPath for object store migration case
    private String deployAsIsConfiguration; // Temporarily set
    @Inject
    VMTemplateDao imageDao;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    TemplateDataStoreDao templateStoreDao;

    public TemplateObject() {
    }

    protected void configure(VMTemplateVO template, DataStore dataStore) {
        imageVO = template;
        this.dataStore = dataStore;
    }

    public static TemplateObject getTemplate(VMTemplateVO vo, DataStore store, String configuration) {
        TemplateObject to = ComponentContext.inject(TemplateObject.class);
        to.deployAsIsConfiguration = configuration;
        to.configure(vo, store);
        return to;
    }

    public void setSize(Long size) {
        imageVO.setSize(size);
    }

    public VMTemplateVO getImage() {
        return imageVO;
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public String getUniqueName() {
        return imageVO.getUniqueName();
    }

    @Override
    public long getId() {
        return imageVO.getId();
    }

    @Override
    public State getState() {
        return imageVO.getState();
    }

    @Override
    public String getUuid() {
        return imageVO.getUuid();
    }

    @Override
    public String getUri() {
        if (url != null) {
            return url;
        }
        VMTemplateVO image = imageDao.findById(imageVO.getId());

        return image.getUrl();

    }

    @Override
    public Long getSize() {
        if (dataStore == null) {
            return imageVO.getSize();
        }
        VMTemplateVO image = imageDao.findById(imageVO.getId());
        return image.getSize();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.TEMPLATE;
    }

    @Override
    public ImageFormat getFormat() {
        return imageVO.getFormat();
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        try {
            objectInStoreMgr.update(this, event);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("Failed to update state", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Failed to update state", e);
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (getDataStore().getRole() == DataStoreRole.Primary) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    TemplateObjectTO newTemplate = (TemplateObjectTO)cpyAnswer.getNewData();

                    String deployAsIsConfiguration = newTemplate.getDeployAsIsConfiguration();
                    VMTemplateStoragePoolVO templatePoolRef = templatePoolDao.findByPoolTemplate(getDataStore().getId(), getId(), deployAsIsConfiguration);
                    templatePoolRef.setDownloadPercent(100);

                    setTemplateSizeIfNeeded(newTemplate, templatePoolRef);

                    templatePoolRef.setDownloadState(Status.DOWNLOADED);

                    setDownloadPathIfNeeded(newTemplate, templatePoolRef);

                    setInstallPathIfNeeded(newTemplate, templatePoolRef);

                    templatePoolDao.update(templatePoolRef.getId(), templatePoolRef);
                }
            } else if (getDataStore().getRole() == DataStoreRole.Image || getDataStore().getRole() == DataStoreRole.ImageCache) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    TemplateObjectTO newTemplate = (TemplateObjectTO)cpyAnswer.getNewData();
                    TemplateDataStoreVO templateStoreRef = templateStoreDao.findByStoreTemplate(getDataStore().getId(), getId());
                    if (newTemplate.getPath() != null) {
                        templateStoreRef.setInstallPath(newTemplate.getPath());
                    }
                    templateStoreRef.setDownloadPercent(100);
                    templateStoreRef.setDownloadState(Status.DOWNLOADED);
                    templateStoreRef.setSize(newTemplate.getSize());
                    if (newTemplate.getPhysicalSize() != null) {
                        templateStoreRef.setPhysicalSize(newTemplate.getPhysicalSize());
                    }
                    templateStoreDao.update(templateStoreRef.getId(), templateStoreRef);
                    if (getDataStore().getRole() == DataStoreRole.Image) {
                        VMTemplateVO templateVO = imageDao.findById(getId());
                        if (newTemplate.getFormat() != null) {
                            templateVO.setFormat(newTemplate.getFormat());
                        }
                        if (newTemplate.getName() != null) {
                            // For template created from snapshot, template name is determine by resource code.
                            templateVO.setUniqueName(newTemplate.getName());
                        }
                        if (newTemplate.getHypervisorType() != null) {
                            templateVO.setHypervisorType(newTemplate.getHypervisorType());
                        }
                        templateVO.setSize(newTemplate.getSize());
                        imageDao.update(templateVO.getId(), templateVO);
                    }
                } else if (answer instanceof CreateDatadiskTemplateAnswer) {
                    CreateDatadiskTemplateAnswer createAnswer = (CreateDatadiskTemplateAnswer)answer;
                    TemplateObjectTO dataDiskTemplate = createAnswer.getDataDiskTemplate();
                    TemplateDataStoreVO templateStoreRef = templateStoreDao.findByStoreTemplate(getDataStore().getId(), dataDiskTemplate.getId());
                    templateStoreRef.setInstallPath(dataDiskTemplate.getPath());
                    templateStoreRef.setDownloadPercent(100);
                    templateStoreRef.setDownloadState(Status.DOWNLOADED);
                    templateStoreRef.setSize(dataDiskTemplate.getSize());
                    templateStoreRef.setPhysicalSize(dataDiskTemplate.getPhysicalSize());
                    templateStoreDao.update(templateStoreRef.getId(), templateStoreRef);
                }
            }
            objectInStoreMgr.update(this, event);
        } catch (NoTransitionException e) {
            s_logger.debug("failed to update state", e);
            throw new CloudRuntimeException("Failed to update state" + e.toString());
        } catch (Exception ex) {
            s_logger.debug("failed to process event and answer", ex);
            objectInStoreMgr.delete(this);
            throw new CloudRuntimeException("Failed to process event", ex);
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    /**
     * In the case of managed storage, the install path may already be specified (by the storage plug-in), so do not overwrite it.
     */
    private void setInstallPathIfNeeded(TemplateObjectTO template, VMTemplateStoragePoolVO templatePoolRef) {
        if (Strings.isNullOrEmpty(templatePoolRef.getInstallPath())) {
            templatePoolRef.setInstallPath(template.getPath());
        }
    }

    /**
     * In the case of managed storage, the local download path may already be specified (by the storage plug-in), so do not overwrite it.
     */
    private void setDownloadPathIfNeeded(TemplateObjectTO template, VMTemplateStoragePoolVO templatePoolRef) {
        if (Strings.isNullOrEmpty(templatePoolRef.getLocalDownloadPath())) {
            templatePoolRef.setLocalDownloadPath(template.getPath());
        }
    }

    /**
     *  In the case of managed storage, the template size may already be specified (by the storage plug-in), so do not overwrite it.
     */
    private void setTemplateSizeIfNeeded(TemplateObjectTO template, VMTemplateStoragePoolVO templatePoolRef) {
        if (templatePoolRef.getTemplateSize() == 0 && template.getSize() != null) {
            templatePoolRef.setTemplateSize(template.getSize());
        }
    }

    @Override
    public void incRefCount() {
        if (dataStore == null) {
            return;
        }

        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            TemplateDataStoreVO store = templateStoreDao.findByStoreTemplate(dataStore.getId(), getId());
            store.incrRefCnt();
            store.setLastUpdated(new Date());
            templateStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public void decRefCount() {
        if (dataStore == null) {
            return;
        }
        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            TemplateDataStoreVO store = templateStoreDao.findByStoreTemplate(dataStore.getId(), getId());
            store.decrRefCnt();
            store.setLastUpdated(new Date());
            templateStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (dataStore == null) {
            return null;
        }
        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            TemplateDataStoreVO store = templateStoreDao.findByStoreTemplate(dataStore.getId(), getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public String getDeployAsIsConfiguration() {
        return deployAsIsConfiguration;
    }

    @Override
    public DataTO getTO() {
        DataTO to = null;
        if (dataStore == null) {
            to = new TemplateObjectTO(this);
        } else {
            to = dataStore.getDriver().getTO(this);
            if (to == null) {
                to = new TemplateObjectTO(this);
            }
        }

        return to;
    }

    @Override
    public String getInstallPath() {
        if (installPath != null) {
            return installPath;
        }

        if (dataStore == null) {
            return null;
        }

        DataObjectInStore obj = objectInStoreMgr.findObject(this, dataStore);

        return obj != null ? obj.getInstallPath() : null;
    }

    @Override
    public boolean isDirectDownload() {
        if (this.imageVO == null) {
            return false;
        }
        return this.imageVO.isDirectDownload();
    }

    @Override
    public boolean canBeDeletedFromDataStore() {
        Status downloadStatus = Status.UNKNOWN;
        int downloadPercent = -1;
        if (getDataStore().getRole() == DataStoreRole.Primary) {
            VMTemplateStoragePoolVO templatePoolRef = templatePoolDao.findByPoolTemplate(getDataStore().getId(), getId(), null);
            if (templatePoolRef != null) {
                downloadStatus = templatePoolRef.getDownloadState();
                downloadPercent = templatePoolRef.getDownloadPercent();
            }
        } else if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            TemplateDataStoreVO templateStoreRef = templateStoreDao.findByStoreTemplate(dataStore.getId(), getId());
            if (templateStoreRef != null) {
                downloadStatus = templateStoreRef.getDownloadState();
                downloadPercent = templateStoreRef.getDownloadPercent();
                templateStoreRef.getState();
            }
        }

        // Marking downloaded templates for deletion, but might skip any deletion handled for failed templates.
        // Only templates not downloaded and in error state (with no install path) cannot be deleted from the datastore, so doesn't impact last behavior for templates with other states
        if (downloadStatus == null  || downloadStatus == Status.NOT_DOWNLOADED || (downloadStatus == Status.DOWNLOAD_ERROR && downloadPercent == 0)) {
            s_logger.debug("Template: " + getId() + " cannot be deleted from the store: " + getDataStore().getId());
            return false;
        }

        return true;
    }

    @Override
    public boolean isDeployAsIs() {
        if (this.imageVO == null) {
            return false;
        }
        return this.imageVO.isDeployAsIs();
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    @Override
    public long getAccountId() {
        return imageVO.getAccountId();
    }

    @Override
    public boolean isFeatured() {
        return imageVO.isFeatured();
    }

    @Override
    public boolean isPublicTemplate() {
        return imageVO.isPublicTemplate();
    }

    @Override
    public boolean isExtractable() {
        return imageVO.isExtractable();
    }

    @Override
    public String getName() {
        return imageVO.getName();
    }

    @Override
    public boolean isRequiresHvm() {
        return imageVO.isRequiresHvm();
    }

    @Override
    public String getDisplayText() {
        return imageVO.getDisplayText();
    }

    @Override
    public boolean isEnablePassword() {
        return imageVO.isEnablePassword();
    }

    @Override
    public boolean isEnableSshKey() {
        return imageVO.isEnableSshKey();
    }

    @Override
    public boolean isCrossZones() {
        return imageVO.isCrossZones();
    }

    @Override
    public Date getCreated() {
        return imageVO.getCreated();
    }

    @Override
    public long getGuestOSId() {
        return imageVO.getGuestOSId();
    }

    @Override
    public boolean isBootable() {
        return imageVO.isBootable();
    }

    @Override
    public TemplateType getTemplateType() {
        return imageVO.getTemplateType();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return imageVO.getHypervisorType();
    }

    @Override
    public int getBits() {
        return imageVO.getBits();
    }

    @Override
    public String getUrl() {
        if (url != null) {
            return url;
        }
        return imageVO.getUrl();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getChecksum() {
        return imageVO.getChecksum();
    }

    @Override
    public Long getSourceTemplateId() {
        return imageVO.getSourceTemplateId();
    }

    @Override
    public Long getParentTemplateId() {
        return imageVO.getParentTemplateId();
    }

    @Override
    public String getTemplateTag() {
        return imageVO.getTemplateTag();
    }

    @Override
    public Map<String, String> getDetails() {
        return imageVO.getDetails();
    }

    @Override
    public boolean isDynamicallyScalable() {
        return false;
    }

    @Override
    public long getDomainId() {
        return imageVO.getDomainId();
    }

    @Override
    public boolean delete() {
        if (dataStore != null) {
            return dataStore.delete(this);
        }
        return true;
    }

    @Override
    public Class<?> getEntityType() {
        return VirtualMachineTemplate.class;
    }

    @Override
    public long getUpdatedCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void incrUpdatedCount() {
        // TODO Auto-generated method stub
    }

    @Override
    public Date getUpdated() {
        // TODO Auto-generated method stub
        return null;
    }
}
