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
package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterFactory;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdaptivePrimaryDatastoreAdapterFactoryMap {
    protected Logger logger = LogManager.getLogger(getClass());
    private Map<String,ProviderAdapterFactory> factoryMap = new HashMap<String,ProviderAdapterFactory>();
    private Map<String,ProviderAdapter> apiMap = new HashMap<String,ProviderAdapter>();

    public AdaptivePrimaryDatastoreAdapterFactoryMap() {

    }

    /**
     * Given a storage pool return current client. Reconfigure if changes are
     * discovered
     */
    public final ProviderAdapter getAPI(String uuid, String providerName, Map<String, String> details) {
        ProviderAdapter api = apiMap.get(uuid);
        if (api == null) {
            synchronized (this) {
                api = apiMap.get(uuid);
                if (api == null) {
                    api = createNewAdapter(uuid, providerName, details);
                    apiMap.put(uuid, api);
                    logger.debug("Cached the new ProviderAdapter for storage pool " + uuid);
                }
            }
        }
        return api;
    }

    /**
     * Update the API with the given UUID.  allows for URL changes and authentication updates
     * @param uuid
     * @param providerName
     * @param details
     */
    public final void updateAPI(String uuid, String providerName, Map<String, String> details) {
        // attempt to create (which validates) the new info before updating the cache
        ProviderAdapter adapter = createNewAdapter(uuid, providerName, details);

        // if its null its likely because no action has occured yet to trigger the API object to be loaded
        if (adapter == null) {
            throw new CloudRuntimeException("Adapter configruation failed for an unknown reason");
        }

        ProviderAdapter oldAdapter = apiMap.get(uuid);
        apiMap.put(uuid, adapter);
        try {
            if (oldAdapter != null) oldAdapter.disconnect();
        } catch (Throwable e) {
            logger.debug("Failure closing the old ProviderAdapter during an update of the cached data after validation of the new adapter configuration, likely the configuration is no longer valid", e);
        }
    }

    public void register(ProviderAdapterFactory factory) {
        factoryMap.put(factory.getProviderName(), factory);
    }

    protected ProviderAdapter createNewAdapter(String uuid, String providerName, Map<String, String> details) {
        String authnType = details.get(ProviderAdapter.API_AUTHENTICATION_TYPE_KEY);
        if (authnType == null) authnType = "basicauth";
        String lookupKey = null;
        if (authnType.equals("basicauth")) {
            lookupKey = details.get(ProviderAdapter.API_USERNAME_KEY);
            if (lookupKey == null) {
                throw new RuntimeException("Storage provider configuration property [" + ProviderAdapter.API_USERNAME_KEY + "] is required when using authentication type [" + authnType + "]");
            }
        } else if (authnType.equals("apitoken")) {
            lookupKey = details.get(ProviderAdapter.API_TOKEN_KEY);
            if (lookupKey == null) {
                throw new RuntimeException("Storage provider configuration property [" + ProviderAdapter.API_TOKEN_KEY + "] is required when using authentication type [" + authnType + "]");
            }
        } else {
            throw new RuntimeException("Storage provider configuration property [" + ProviderAdapter.API_AUTHENTICATION_TYPE_KEY + "] not set to valid value");
        }

        String url = details.get(ProviderAdapter.API_URL_KEY);
        if (url == null) {
            throw new RuntimeException("URL required when configuring a Managed Block API storage provider");
        }

        logger.debug("Looking for Provider [" + providerName + "] at [" + url + "]");
        ProviderAdapterFactory factory = factoryMap.get(providerName);
        if (factory == null) {
            throw new RuntimeException("Unable to find a storage provider API factory for provider: " + providerName);
        }

        // decrypt password or token before sending to provider
        if (authnType.equals("basicauth")) {
            try {
                details.put(ProviderAdapter.API_PASSWORD_KEY, DBEncryptionUtil.decrypt(details.get(ProviderAdapter.API_PASSWORD_KEY)));
            } catch (Exception e) {
                logger.warn("Failed to decrypt managed block API property: [" + ProviderAdapter.API_PASSWORD_KEY + "], trying to use as-is");
            }
        } else if (authnType.equals("apitoken")) {
            try {
                details.put(ProviderAdapter.API_TOKEN_KEY, DBEncryptionUtil.decrypt(details.get(ProviderAdapter.API_TOKEN_KEY)));
            } catch (Exception e) {
                logger.warn("Failed to decrypt managed block API property: [" + ProviderAdapter.API_TOKEN_KEY + "], trying to use as-is");
            }
        }

        ProviderAdapter api = factory.create(url, details);
        api.validate();
        logger.debug("Creating new ProviderAdapter object for endpoint: " + providerName + "@" + url);
        return api;
    }

    public ProviderAdapterFactory getFactory(String providerName) {
        return this.factoryMap.get(providerName);
    }
}
