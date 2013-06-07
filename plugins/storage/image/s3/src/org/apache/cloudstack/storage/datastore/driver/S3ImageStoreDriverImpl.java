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

import java.util.Map;
import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.log4j.Logger;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.S3Utils;
import com.cloud.utils.exception.CloudRuntimeException;

public class S3ImageStoreDriverImpl extends  BaseImageStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(S3ImageStoreDriverImpl.class);

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;


    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl imgStore = (ImageStoreImpl) store;
        Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new S3TO(imgStore.getId(), imgStore.getUuid(), details.get(ApiConstants.S3_ACCESS_KEY),
                details.get(ApiConstants.S3_SECRET_KEY), details.get(ApiConstants.S3_END_POINT),
                details.get(ApiConstants.S3_BUCKET_NAME), details.get(ApiConstants.S3_HTTPS_FLAG) == null ? false
                        : Boolean.parseBoolean(details.get(ApiConstants.S3_HTTPS_FLAG)),
                details.get(ApiConstants.S3_CONNECTION_TIMEOUT) == null ? null : Integer.valueOf(details
                        .get(ApiConstants.S3_CONNECTION_TIMEOUT)),
                details.get(ApiConstants.S3_MAX_ERROR_RETRY) == null ? null : Integer.valueOf(details
                        .get(ApiConstants.S3_MAX_ERROR_RETRY)),
                details.get(ApiConstants.S3_SOCKET_TIMEOUT) == null ? null : Integer.valueOf(details
                        .get(ApiConstants.S3_SOCKET_TIMEOUT)), imgStore.getCreated());

    }


    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format) {
        // for S3, no need to do anything, just return template url for
        // extract template. but we need to set object acl as public_read to
        // make the url accessible
        S3TO s3 = (S3TO)getStoreTO(store);
        String key = installPath;
        try {
            S3Utils.setObjectAcl(s3, s3.getBucketName(), key, CannedAccessControlList.PublicRead);
        } catch (Exception ex) {
            s_logger.error("Failed to set ACL on S3 object " + key + " to PUBLIC_READ", ex);
            throw new CloudRuntimeException("Failed to set ACL on S3 object " + key + " to PUBLIC_READ");
        }
        // construct the url from s3
        StringBuffer s3url = new StringBuffer();
        s3url.append(s3.isHttps() ? "https://" : "http://");
        s3url.append(s3.getEndPoint());
        s3url.append("/");
        s3url.append(s3.getBucketName());
        s3url.append("/");
        s3url.append(key);
        return s3url.toString();
    }


}
