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
package org.apache.cloudstack.storage.datastore.driver;

import java.net.URL;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;


import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.configuration.Config;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.storage.S3.S3Utils;

public class S3ImageStoreDriverImpl extends BaseImageStoreDriverImpl {

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;

    @Inject
    ConfigurationDao _configDao;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl imgStore = (ImageStoreImpl)store;
        Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new S3TO(imgStore.getId(),
                        imgStore.getUuid(),
                        details.get(ApiConstants.S3_ACCESS_KEY),
                        details.get(ApiConstants.S3_SECRET_KEY),
                        details.get(ApiConstants.S3_END_POINT),
                        details.get(ApiConstants.S3_BUCKET_NAME),
                        details.get(ApiConstants.S3_SIGNER),
                        details.get(ApiConstants.S3_HTTPS_FLAG) == null ? false : Boolean.parseBoolean(details.get(ApiConstants.S3_HTTPS_FLAG)),
                        details.get(ApiConstants.S3_CONNECTION_TIMEOUT) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_CONNECTION_TIMEOUT)),
                        details.get(ApiConstants.S3_MAX_ERROR_RETRY) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_MAX_ERROR_RETRY)),
                        details.get(ApiConstants.S3_SOCKET_TIMEOUT) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_SOCKET_TIMEOUT)),
                        imgStore.getCreated(),
                        _configDao.getValue(Config.S3EnableRRS.toString()) == null ? false : Boolean.parseBoolean(_configDao.getValue(Config.S3EnableRRS.toString())),
                        getMaxSingleUploadSizeInBytes(),
                        details.get(ApiConstants.S3_CONNECTION_TTL) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_CONNECTION_TTL)),
                        details.get(ApiConstants.S3_USE_TCP_KEEPALIVE) == null ? null : Boolean.parseBoolean(details.get(ApiConstants.S3_USE_TCP_KEEPALIVE)));
    }

    private long getMaxSingleUploadSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue(Config.S3MaxSingleUploadSize.toString())) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            // use default 1TB
            return 1024L * 1024L * 1024L * 1024L;
        }
    }

    @Override
    public String createEntityExtractUrl(DataStore store, String key, ImageFormat format, DataObject dataObject) {
        /**
         * Generate a pre-signed URL for the given object.
         */
        S3TO s3 = (S3TO)getStoreTO(store);

        if(logger.isDebugEnabled()) {
            logger.debug("Generating pre-signed s3 entity extraction URL for object: " + key);
        }
        Date expiration = new Date();
        long milliSeconds = expiration.getTime();

        // Get extract url expiration interval set in global configuration (in seconds)
        String urlExpirationInterval = _configDao.getValue(Config.ExtractURLExpirationInterval.toString());

        // Expired after configured interval (in milliseconds), default 14400 seconds
        milliSeconds += 1000 * NumbersUtil.parseInt(urlExpirationInterval, 14400);
        expiration.setTime(milliSeconds);

        URL s3url = S3Utils.generatePresignedUrl(s3, s3.getBucketName(), key, expiration);

        logger.info("Pre-Signed URL = " + s3url.toString());

        return s3url.toString();
    }
}
