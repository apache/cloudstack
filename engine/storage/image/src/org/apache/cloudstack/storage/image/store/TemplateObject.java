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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateEvent;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.manager.ImageDataManager;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine;
import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.storage.encoding.EncodingType;

public class TemplateObject implements TemplateInfo {
    private static final Logger s_logger = Logger
            .getLogger(TemplateObject.class);
    private ImageDataVO imageVO;
    private DataStore dataStore;
    @Inject
    ImageDataManager imageMgr;
    @Inject
    ImageDataDao imageDao;
    @Inject
    ObjectInDataStoreManager ojbectInStoreMgr;

    protected TemplateObject() {
    }

    protected void configure(ImageDataVO template, DataStore dataStore) {
        this.imageVO = template;
        this.dataStore = dataStore;
    }

    public static TemplateObject getTemplate(ImageDataVO vo, DataStore store) {
        TemplateObject to = ComponentContext.inject(TemplateObject.class);
        to.configure(vo, store);
        return to;
    }

    public void setImageStoreId(long id) {
        this.imageVO.setImageDataStoreId(id);
    }
    
    public void setSize(Long size) {
        this.imageVO.setSize(size);
    }

    public ImageDataVO getImage() {
        return this.imageVO;
    }

    @Override
    public DataStore getDataStore() {
        return this.dataStore;
    }

    @Override
    public long getId() {
        return this.imageVO.getId();
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUri() {
        ImageDataVO image = imageDao.findById(this.imageVO.getId());
        if (this.dataStore == null) {
            return image.getUrl();
        } else {
            ObjectInDataStoreVO obj = ojbectInStoreMgr.findObject(
                    this.imageVO.getId(), DataObjectType.TEMPLATE,
                    this.dataStore.getId(), this.dataStore.getRole());
            StringBuilder builder = new StringBuilder();
            if (obj.getState() == ObjectInDataStoreStateMachine.State.Ready
                    || obj.getState() == ObjectInDataStoreStateMachine.State.Copying) {
                
                builder.append(this.dataStore.getUri());
                builder.append("&" + EncodingType.OBJTYPE + "=" + DataObjectType.TEMPLATE);
                builder.append("&" + EncodingType.PATH + "=" + obj.getInstallPath());
                builder.append("&" + EncodingType.SIZE + "=" + image.getSize());
                return builder.toString();
            } else {
                builder.append(this.dataStore.getUri());
                builder.append("&" + EncodingType.OBJTYPE + "=" + DataObjectType.TEMPLATE);
                builder.append("&" + EncodingType.SIZE + "=" + image.getSize());
                builder.append("&" + EncodingType.PATH + "=" + image.getUrl());
                return builder.toString();
            }
        }
    }

    @Override
    public Long getSize() {
        if (this.dataStore == null) {
            return this.imageVO.getSize();
        }
        ObjectInDataStoreVO obj = ojbectInStoreMgr.findObject(
                this.imageVO.getId(), DataObjectType.TEMPLATE,
                this.dataStore.getId(), this.dataStore.getRole());
        return obj.getSize();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.TEMPLATE;
    }

    @Override
    public DiskFormat getFormat() {
        return DiskFormat.getFormat(this.imageVO.getFormat());
    }

    public boolean stateTransit(TemplateEvent e) throws NoTransitionException {
        boolean result= imageMgr.getStateMachine().transitTo(this.imageVO, e, null,
                imageDao);
        this.imageVO = imageDao.findById(this.imageVO.getId());
        return result;
    }
}
