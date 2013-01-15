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
import org.apache.cloudstack.storage.image.TemplateEvent;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.manager.ImageDataManager;
import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.fsm.NoTransitionException;

public class TemplateObject implements TemplateInfo {
    private static final Logger s_logger = Logger.getLogger(TemplateObject.class);
    private ImageDataVO imageVO;
    private DataStore dataStore;
    @Inject
    ImageDataManager imageMgr;
    @Inject
    ImageDataDao imageDao;

    public TemplateObject(ImageDataVO template, DataStore dataStore) {
        this.imageVO = template;
        this.dataStore = dataStore;
    }
    
    public static TemplateObject getTemplate(ImageDataVO vo, DataStore store) {
        TemplateObject to = new TemplateObject(vo, store);
        return ComponentContext.inject(to);
    }
    
    public void setImageStoreId(long id) {
        this.imageVO.setImageDataStoreId(id);
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
    public String getPath() {
        //TODO: add installation path if it's downloaded to cache storage already
        return this.imageVO.getUrl();
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUri() {
        return this.dataStore.getUri() + "template/" + this.getPath();
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.TEMPLATE;
    }

    @Override
    public DiskFormat getFormat() {
        return DiskFormat.getFormat(this.imageVO.getFormat());
    }
    
    @Override
    public boolean stateTransit(TemplateEvent e) throws NoTransitionException {
        return imageMgr.getStateMachine().transitTo(this.imageVO, e, null, imageDao);
    }
}
