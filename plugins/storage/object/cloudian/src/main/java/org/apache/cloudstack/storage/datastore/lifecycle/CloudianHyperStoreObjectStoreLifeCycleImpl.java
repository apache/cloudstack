// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.lifecycle;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.CloudianHyperStoreUtil;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

public class CloudianHyperStoreObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {

    protected Logger logger = LogManager.getLogger(CloudianHyperStoreObjectStoreLifeCycleImpl.class);

    @Inject
    ObjectStoreHelper objectStoreHelper;
    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    public CloudianHyperStoreObjectStoreLifeCycleImpl() {
    }

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String name = (String)dsInfos.get(CloudianHyperStoreUtil.STORE_KEY_NAME);
        String url = (String)dsInfos.get(CloudianHyperStoreUtil.STORE_KEY_URL);
        String providerName = (String)dsInfos.get(CloudianHyperStoreUtil.STORE_KEY_PROVIDER_NAME);

        // Check the providerName is what we expect
        if (! StringUtils.equalsIgnoreCase(providerName, CloudianHyperStoreUtil.OBJECT_STORE_PROVIDER_NAME)) {
            String msg = String.format("Unexpected providerName \"%s\". Expected \"%s\"", providerName, CloudianHyperStoreUtil.OBJECT_STORE_PROVIDER_NAME);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        Map<String, Object> objectStoreParameters = new HashMap<String, Object>();
        objectStoreParameters.put(CloudianHyperStoreUtil.STORE_KEY_NAME, name);
        objectStoreParameters.put(CloudianHyperStoreUtil.STORE_KEY_URL, url);
        objectStoreParameters.put(CloudianHyperStoreUtil.STORE_KEY_PROVIDER_NAME, providerName);

        // Pull out the details map
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get(CloudianHyperStoreUtil.STORE_KEY_DETAILS);
        if (details == null) {
            String msg = String.format("Unexpected null receiving Object Store initialization \"%s\"", CloudianHyperStoreUtil.STORE_KEY_DETAILS);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        // Note: The Admin Username/Password are available respectively as accesskey/secretkey
        String adminUsername = details.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_USER_NAME);
        String adminPassword = details.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_PASSWORD);
        String validateSSL = details.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_VALIDATE_SSL);
        boolean adminValidateSSL = Boolean.parseBoolean(validateSSL);
        String s3Url = details.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_S3_URL);
        String iamUrl = details.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_IAM_URL);

        if (StringUtils.isAnyBlank(adminUsername, adminPassword, validateSSL, s3Url, iamUrl)) {
            final String asteriskPassword = (adminPassword == null) ? null : "*".repeat(adminPassword.length());
            logger.error("Required parameters are missing; username={} password={} validateSSL={} s3Url={} iamUrl={}",
                adminUsername, asteriskPassword, validateSSL, s3Url, iamUrl);
            throw new CloudRuntimeException("Required Cloudian HyperStore configuration parameters are missing/empty.");
        }

        // Validate the ADMIN API Service Information
        logger.info("Confirming connection to the HyperStore Admin Service at: {}", url);
        CloudianClient client = CloudianHyperStoreUtil.getCloudianClient(url, adminUsername, adminPassword, adminValidateSSL);
        String version = client.getServerVersion();

        // Validate S3 and IAM Service URLs.
        CloudianHyperStoreUtil.validateS3Url(s3Url);
        CloudianHyperStoreUtil.validateIAMUrl(iamUrl);

        logger.info("Successfully connected to HyperStore: {}", version);

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

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

}
