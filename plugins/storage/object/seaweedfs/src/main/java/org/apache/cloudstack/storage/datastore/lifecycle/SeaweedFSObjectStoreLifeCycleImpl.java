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
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.lifecycle;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.SeaweedFSObjectStoreUtil;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

public class SeaweedFSObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {

    protected Logger logger = LogManager.getLogger(SeaweedFSObjectStoreLifeCycleImpl.class);

    @Inject
    ObjectStoreHelper objectStoreHelper;
    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    public SeaweedFSObjectStoreLifeCycleImpl() {
    }

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String name = (String)dsInfos.get(SeaweedFSObjectStoreUtil.STORE_KEY_NAME);
        String url = (String)dsInfos.get(SeaweedFSObjectStoreUtil.STORE_KEY_URL);
        String providerName = (String)dsInfos.get(SeaweedFSObjectStoreUtil.STORE_KEY_PROVIDER_NAME);

        // Check the providerName is what we expect
        if (! StringUtils.equalsIgnoreCase(providerName, SeaweedFSObjectStoreUtil.OBJECT_STORE_PROVIDER_NAME)) {
            String msg = String.format("Unexpected providerName \"%s\". Expected \"%s\"", providerName, SeaweedFSObjectStoreUtil.OBJECT_STORE_PROVIDER_NAME);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        Map<String, Object> objectStoreParameters = new HashMap<String, Object>();
        objectStoreParameters.put(SeaweedFSObjectStoreUtil.STORE_KEY_NAME, name);
        objectStoreParameters.put(SeaweedFSObjectStoreUtil.STORE_KEY_URL, url);
        objectStoreParameters.put(SeaweedFSObjectStoreUtil.STORE_KEY_PROVIDER_NAME, providerName);

        // Pull out the details map
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get(SeaweedFSObjectStoreUtil.STORE_KEY_DETAILS);
        if (details == null) {
            String msg = String.format("Unexpected null receiving Object Store initialization \"%s\"", SeaweedFSObjectStoreUtil.STORE_KEY_DETAILS);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        // The admin/root access key and secret key are available as accesskey/secretkey
        String accessKey = details.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY);
        String secretKey = details.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY);
        String s3Url = details.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL);
        String iamUrl = details.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL);

        // If s3Url is not provided, default it to the store url
        if (StringUtils.isBlank(s3Url)) {
            s3Url = url;
        }
        // If iamUrl is not provided, default it to the s3Url.
        // SeaweedFS registers its embedded IAM API at POST / on the same S3
        // endpoint (UnifiedPostHandler), so the IAM endpoint is the same as
        // the S3 endpoint unless the deployment runs a separate weed iam server.
        if (StringUtils.isBlank(iamUrl)) {
            iamUrl = s3Url;
        }

        if (StringUtils.isAnyBlank(accessKey, secretKey, s3Url, iamUrl)) {
            final String asteriskPassword = (secretKey == null) ? null : "*".repeat(secretKey.length());
            logger.error("Required parameters are missing; accessKey={} secretKey={} s3Url={} iamUrl={}",
                accessKey, asteriskPassword, s3Url, iamUrl);
            throw new CloudRuntimeException("Required SeaweedFS configuration parameters are missing/empty.");
        }

        // Update the details map with the resolved URLs so the driver can read them later
        details.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, s3Url);
        details.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL, iamUrl);

        // Validate S3 and IAM Service URLs.
        logger.info("Validating SeaweedFS S3 endpoint: {}", s3Url);
        SeaweedFSObjectStoreUtil.validateS3Url(s3Url);
        logger.info("Validating SeaweedFS IAM endpoint: {}", iamUrl);
        SeaweedFSObjectStoreUtil.validateIAMUrl(iamUrl);

        logger.info("Successfully validated SeaweedFS object store: {} (quota management via S3 ?seaweedfs-quota extension)", name);

        ObjectStoreVO objectStore = objectStoreHelper.createObjectStore(objectStoreParameters, details);
        return objectStoreMgr.getObjectStore(objectStore.getId());
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return false;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        return false;
    }

    @Override
    public boolean maintain(DataStore store) {
        return false;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return false;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return false;
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

}
