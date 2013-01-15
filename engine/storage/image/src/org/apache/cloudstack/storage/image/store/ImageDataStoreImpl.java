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
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.datastore.ImageDataStore;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;


public class ImageDataStoreImpl implements ImageDataStore {
    @Inject
    ImageDataDao imageDao;
    ImageDataStoreDriver driver;
    ImageDataStoreVO imageDataStoreVO;
    boolean needDownloadToCacheStorage = false;

    public ImageDataStoreImpl(ImageDataStoreVO dataStoreVO, ImageDataStoreDriver imageDataStoreDriver) {
        this.driver = imageDataStoreDriver;
        this.imageDataStoreVO = dataStoreVO;
    }

   

    @Override
    public Set<TemplateInfo> listTemplates() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public DataStoreDriver getDriver() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public DataStoreRole getRole() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }



    @Override
    public String getUri() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public Scope getScope() {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return false;
    }
}
