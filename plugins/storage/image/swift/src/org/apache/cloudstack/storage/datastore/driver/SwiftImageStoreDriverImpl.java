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

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.storage.Storage.ImageFormat;

public class SwiftImageStoreDriverImpl extends BaseImageStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(SwiftImageStoreDriverImpl.class);

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;


    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl imgStore = (ImageStoreImpl) store;
        Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new SwiftTO(imgStore.getId(), imgStore.getUri(), details.get(ApiConstants.ACCOUNT),
                details.get(ApiConstants.USERNAME), details.get(ApiConstants.KEY));
    }


    @Override
    public String createEntityExtractUrl(DataStore store, String installPath, ImageFormat format) {
        throw new UnsupportedServiceException("Extract entity url is not yet supported for Swift image store provider");
    }


}
