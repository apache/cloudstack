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

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.store.ImageDataStoreInfo;

public class TemplateObject implements TemplateInfo {
    private ImageDataVO imageVO;
    private ImageDataStoreInfo dataStore;

    public TemplateObject(ImageDataVO template, ImageDataStoreInfo dataStore) {
        this.imageVO = template;
        this.dataStore = dataStore;
    }

    @Override
    public ImageDataStoreInfo getDataStore() {
        return this.dataStore;
    }

    @Override
    public long getId() {
        return this.imageVO.getId();
    }

    @Override
    public VolumeDiskType getDiskType() {
        return VolumeDiskTypeHelper.getDiskType(imageVO.getFormat());
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
}
