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
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.provider.ImageDataStoreProvider;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageDataStore;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;

import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.storage.encoding.EncodingType;


public class DefaultImageDataStoreImpl implements ImageDataStore {
    @Inject
    VMTemplateDao imageDao;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    protected ImageDataStoreDriver driver;
    protected ImageDataStoreVO imageDataStoreVO;
    protected ImageDataStoreProvider provider;
    boolean needDownloadToCacheStorage = false;

    public DefaultImageDataStoreImpl() {
     
    }
    
    protected void configure(ImageDataStoreVO dataStoreVO, ImageDataStoreDriver imageDataStoreDriver,
            ImageDataStoreProvider provider) {
        this.driver = imageDataStoreDriver;
        this.imageDataStoreVO = dataStoreVO;
        this.provider = provider;
    }

    public static ImageDataStore getDataStore(ImageDataStoreVO dataStoreVO, ImageDataStoreDriver imageDataStoreDriver,
            ImageDataStoreProvider provider) {
        DefaultImageDataStoreImpl instance = (DefaultImageDataStoreImpl)ComponentContext.inject(DefaultImageDataStoreImpl.class);
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
        return this.imageDataStoreVO.getProtocol() + "://" + "?" + EncodingType.ROLE + "=" + this.getRole();
    }

    @Override
    public Scope getScope() {
        return new ZoneScope(imageDataStoreVO.getDcId());
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
}
