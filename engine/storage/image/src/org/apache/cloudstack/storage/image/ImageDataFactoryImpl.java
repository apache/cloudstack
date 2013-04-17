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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;

@Component
public class ImageDataFactoryImpl implements ImageDataFactory {
    private static final Logger s_logger = Logger
            .getLogger(ImageDataFactoryImpl.class);
    @Inject
    VMTemplateDao imageDataDao;
    @Inject
    DataStoreManager storeMgr;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    TemplateDataStoreDao templateStoreDao;
    @Override
    public TemplateInfo getTemplate(long templateId, DataStore store) {
        VMTemplateVO templ = imageDataDao.findById(templateId);
        if (store == null) {
            TemplateObject tmpl =  TemplateObject.getTemplate(templ, null);
            return tmpl;
        }
        // verify if the given input parameters are consistent with our db data.
        boolean found = false;
        if (store.getRole() == DataStoreRole.Primary) {
            VMTemplateStoragePoolVO templatePoolVO = templatePoolDao.findByPoolTemplate(store.getId(), templateId);
            if (templatePoolVO != null) {
                found = true;
            }
        } else {
            TemplateDataStoreVO templateStoreVO = templateStoreDao.findByStoreTemplate(store.getId(), templateId);
            if (templateStoreVO != null) {
                found = true;
            }
        }

        if (!found) {
            s_logger.debug("template " + templateId + " is not in store:" + store.getId() + ", type:" + store.getRole());
        }

        TemplateObject tmpl =  TemplateObject.getTemplate(templ, store);
        return tmpl;
    }

    // NOTE that this method can only be used for get template information stored in secondary storage
    //TODO: this method is problematic, since one template can be stored in multiple image stores.
    // need to see if we can get rid of this method or change to plural format, or restrict to 1:1 mapping
    @Override
    public TemplateInfo getTemplate(long templateId) {
        VMTemplateVO templ = imageDataDao.findById(templateId);
        TemplateDataStoreVO tmplStore = templateStoreDao.findByTemplate(templateId);
        DataStore store = null;
        if ( tmplStore != null ){
            store = this.storeMgr.getDataStore(tmplStore.getDataStoreId(), DataStoreRole.Image);
        }
        return this.getTemplate(templateId, store);
    }

    @Override
    public TemplateInfo getTemplate(DataObject obj, DataStore store) {
        return this.getTemplate(obj.getId(), store);
    }
}
