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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.DataStoreManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.springframework.stereotype.Component;

@Component
public class ImageDataFactoryImpl implements ImageDataFactory {
    @Inject
    ImageDataDao imageDataDao;
    @Inject
    ObjectInDataStoreManager objMap;
    @Inject
    DataStoreManager storeMgr;
    @Override
    public TemplateInfo getTemplate(long templateId, DataStore store) {
        ImageDataVO templ = imageDataDao.findById(templateId);
        if (store == null) {
            TemplateObject tmpl =  TemplateObject.getTemplate(templ, null);
            return tmpl;
        }
        ObjectInDataStoreVO obj = objMap.findObject(templateId, DataObjectType.TEMPLATE, store.getId(), store.getRole());
        if (obj == null) {
            TemplateObject tmpl =  TemplateObject.getTemplate(templ, null);
            return tmpl;
        }
        
        TemplateObject tmpl =  TemplateObject.getTemplate(templ, store);
        return tmpl;
    }
}
