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

import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.TemplateObject;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.downloader.ImageDownloader;
import org.apache.cloudstack.storage.image.driver.ImageDataStoreDriver;

public class ImageDataStoreImpl implements ImageDataStore {
    @Inject
    ImageDataDao imageDao;
    ImageDataStoreDriver driver;
    ImageDownloader downloader;
    ImageDataStoreVO imageDataStoreVO;
    boolean needDownloadToCacheStorage = false;

    public ImageDataStoreImpl(ImageDataStoreVO dataStoreVO, ImageDataStoreDriver driver, boolean needDownloadToCacheStorage, ImageDownloader downloader) {
        this.driver = driver;
        this.needDownloadToCacheStorage = needDownloadToCacheStorage;
        this.downloader = downloader;
        this.imageDataStoreVO = dataStoreVO;
    }

    /*
     * @Override public TemplateInfo registerTemplate(long templateId) {
     * ImageDataVO idv = imageDao.findById(templateId); TemplateInfo template =
     * new TemplateInfo(this, idv); if (driver.registerTemplate(template)) {
     * template.setImageDataStoreId(imageDataStoreVO.getId()); return template;
     * } else { return null; } }
     */

    @Override
    public String grantAccess(long templateId, long endPointId) {
        ImageDataVO idv = imageDao.findById(templateId);
        return idv.getUrl();
    }

    @Override
    public boolean revokeAccess(long templateId, long endPointId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteTemplate(long templateId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needDownloadToCacheStorage() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ImageDownloader getImageDownloader() {
        return this.downloader;
    }

    @Override
    public long getImageDataStoreId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public TemplateObject registerTemplate(long templateId) {
        ImageDataVO image = imageDao.findById(templateId);
        image.setImageDataStoreId(this.getImageDataStoreId());
        imageDao.update(templateId, image);
        return getTemplate(templateId);
    }

    @Override
    public TemplateObject getTemplate(long templateId) {
        ImageDataVO image = imageDao.findById(templateId);
        TemplateObject to = new TemplateObject(image, this);
        return to;
    }

}
