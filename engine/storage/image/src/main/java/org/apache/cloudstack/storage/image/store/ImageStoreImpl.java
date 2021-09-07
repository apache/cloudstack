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
import java.util.Set;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import com.cloud.storage.Upload;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.to.ImageStoreTO;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.component.ComponentContext;

public class ImageStoreImpl implements ImageStoreEntity {
    private static final Logger s_logger = Logger.getLogger(ImageStoreImpl.class);
    @Inject
    VMTemplateDao imageDao;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    private CapacityDao capacityDao;
    protected ImageStoreDriver driver;
    protected ImageStoreVO imageDataStoreVO;
    protected ImageStoreProvider provider;

    public ImageStoreImpl() {
        super();
    }

    protected void configure(ImageStoreVO dataStoreVO, ImageStoreDriver imageDataStoreDriver, ImageStoreProvider provider) {
        this.driver = imageDataStoreDriver;
        this.imageDataStoreVO = dataStoreVO;
        this.provider = provider;
    }

    public static ImageStoreEntity getDataStore(ImageStoreVO dataStoreVO, ImageStoreDriver imageDataStoreDriver, ImageStoreProvider provider) {
        ImageStoreImpl instance = ComponentContext.inject(ImageStoreImpl.class);
        instance.configure(dataStoreVO, imageDataStoreDriver, provider);
        return instance;
    }

    @Override
    public Set<TemplateInfo> listTemplates() {
        return null;
    }

    @Override
    public DataStoreDriver getDriver() {
        return this.driver;
    }

    @Override
    public DataStoreRole getRole() {
        return this.imageDataStoreVO.getRole();
    }

    @Override
    public long getId() {
        return this.imageDataStoreVO.getId();
    }

    @Override
    public String getUri() {
        return this.imageDataStoreVO.getUrl();
    }

    @Override
    public Scope getScope() {
        return new ZoneScope(imageDataStoreVO.getDataCenterId());
    }

    @Override
    public TemplateInfo getTemplate(long templateId) {
        return null;
    }

    @Override
    public VolumeInfo getVolume(long volumeId) {
        return null;
    }

    @Override
    public SnapshotInfo getSnapshot(long snapshotId) {
        return null;
    }

    @Override
    public boolean exists(DataObject object) {
        return (objectInStoreMgr.findObject(object, this) != null) ? true : false;
    }

    @Override
    public String getUuid() {
        return this.imageDataStoreVO.getUuid();
    }

    public Date getCreated() {
        return this.imageDataStoreVO.getCreated();
    }

    @Override
    public DataObject create(DataObject obj) {
        DataObject object = objectInStoreMgr.create(obj, this);
        return object;
    }

    @Override
    public boolean delete(DataObject obj) {
        AsyncCallFuture<CommandResult> future = new AsyncCallFuture<CommandResult>();
        this.driver.deleteAsync(obj.getDataStore(), obj, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            s_logger.debug("failed delete obj", e);
            return false;
        } catch (ExecutionException e) {
            s_logger.debug("failed delete obj", e);
            return false;
        }
        objectInStoreMgr.delete(obj);
        return true;
    }

    @Override
    public String getName() {
        return imageDataStoreVO.getName();
    }

    @Override
    public Long getDataCenterId() {
        return imageDataStoreVO.getDataCenterId();
    }

    @Override
    public String getProviderName() {
        return imageDataStoreVO.getProviderName();
    }

    @Override
    public String getProtocol() {
        return imageDataStoreVO.getProtocol();
    }

    @Override
    public String getUrl() {
        return imageDataStoreVO.getUrl();
    }

    @Override
    public DataStoreTO getTO() {
        DataStoreTO to = getDriver().getStoreTO(this);
        if (to == null) {
            ImageStoreTO primaryTO = new ImageStoreTO();
            primaryTO.setProviderName(getProviderName());
            primaryTO.setRole(getRole());
            primaryTO.setType(getProtocol());
            primaryTO.setUri(getUri());
            return primaryTO;
        }
        return to;
    }

    @Override
    public String getMountPoint() {
        return imageDataStoreVO.getParent();
    }

    @Override
    public String createEntityExtractUrl(String installPath, ImageFormat format, DataObject dataObject) {
        return driver.createEntityExtractUrl(this, installPath, format, dataObject);
    }

    @Override
    public void deleteExtractUrl(String installPath, String url, Upload.Type entityType) {
        driver.deleteEntityExtractUrl(this, installPath, url, entityType);
    }

    @Override
    public List<DatadiskTO> getDataDiskTemplates(DataObject obj, String configurationId) {
        return driver.getDataDiskTemplates(obj, configurationId);
    }

    @Override
    public Void createDataDiskTemplateAsync(TemplateInfo dataDiskTemplate, String path, String diskId, long fileSize, boolean bootable, AsyncCompletionCallback<CreateCmdResult> callback) {
        return driver.createDataDiskTemplateAsync(dataDiskTemplate, path, diskId, bootable, fileSize, callback);
    }

}
