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

import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreTO;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;

import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.storage.encoding.EncodingType;


public class ImageStoreImpl implements ImageStoreEntity {
    @Inject
    VMTemplateDao imageDao;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    protected ImageStoreDriver driver;
    protected ImageStoreVO imageDataStoreVO;
    protected ImageStoreProvider provider;
    boolean needDownloadToCacheStorage = false;

    public ImageStoreImpl() {

    }

    protected void configure(ImageStoreVO dataStoreVO, ImageStoreDriver imageDataStoreDriver,
            ImageStoreProvider provider) {
        this.driver = imageDataStoreDriver;
        this.imageDataStoreVO = dataStoreVO;
        this.provider = provider;
    }

    public static ImageStoreEntity getDataStore(ImageStoreVO dataStoreVO, ImageStoreDriver imageDataStoreDriver,
            ImageStoreProvider provider) {
        ImageStoreImpl instance = (ImageStoreImpl)ComponentContext.inject(ImageStoreImpl.class);
        instance.configure(dataStoreVO, imageDataStoreDriver, provider);
        return instance;
    }

    @Override
    public Set<TemplateInfo> listTemplates() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataStoreDriver getDriver() {
        // TODO Auto-generated method stub
        return this.driver;
    }

    @Override
    public DataStoreRole getRole() {
        // TODO Auto-generated method stub
        return DataStoreRole.Image;
    }
    @Override
    public long getId() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeInfo getVolume(long volumeId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotInfo getSnapshot(long snapshotId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(DataObject object) {
        return (objectInStoreMgr.findObject(object,
                this) != null) ? true : false;
    }

    @Override
    public String getUuid() {
        return this.imageDataStoreVO.getUuid();
    }

    @Override
    public DataObject create(DataObject obj) {
        DataObject object = objectInStoreMgr.create(obj, this);
        return object;
    }

    @Override
    public boolean delete(DataObject obj) {
        // TODO Auto-generated method stub
        return false;
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
    public DataStoreTO getTO() {
        return null;
    }


}
