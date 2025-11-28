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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet; // change to POST if ECS needs it
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

public class EcsObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {

    private static final Logger LOG = LogManager.getLogger(EcsObjectStoreLifeCycleImpl.class);

    // detail keys coming from the API
    private static final String MGMT_URL   = "mgmt_url";
    private static final String SA_USER    = "sa_user";
    private static final String SA_PASS    = "sa_password";
    private static final String INSECURE   = "insecure";

    // optional details (currently not used in persistence logic but accepted)
    private static final String S3_HOST    = "s3_host";
    private static final String NAMESPACE  = "namespace";

    private static final String PROVIDER_NAME = "ECS";

    @Inject
    ObjectStoreHelper objectStoreHelper;

    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    public EcsObjectStoreLifeCycleImpl() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        if (objectStoreHelper == null) {
            throw new CloudRuntimeException("ECS: ObjectStoreHelper is not injected");
        }
        if (objectStoreMgr == null) {
            throw new CloudRuntimeException("ECS: ObjectStoreProviderManager is not injected");
        }

        // Top-level params (follow Ceph pattern)
        String url          = (String) dsInfos.get("url");
        String name         = (String) dsInfos.get("name");
        Long   size         = (Long)   dsInfos.get("size");          // may be null
        String providerName = (String) dsInfos.get("providerName");  // should be "ECS"

        Map<String, String> details = (Map<String, String>) dsInfos.get("details");
        if (details == null) {
            throw new CloudRuntimeException("ECS: details map is missing");
        }

        // Validate required details
        String mgmtUrl   = trim(details.get(MGMT_URL));
        String saUser    = safe(details.get(SA_USER));
        String saPass    = safe(details.get(SA_PASS));
        boolean insecure = Boolean.parseBoolean(details.getOrDefault(INSECURE, "false"));

        if (mgmtUrl == null || mgmtUrl.isEmpty()) {
            throw new CloudRuntimeException("ECS: missing required detail '" + MGMT_URL + "'");
        }
        if (saUser.isEmpty()) {
            throw new CloudRuntimeException("ECS: missing required detail '" + SA_USER + "'");
        }
        if (saPass.isEmpty()) {
            throw new CloudRuntimeException("ECS: missing required detail '" + SA_PASS + "'");
        }

        if (providerName == null || providerName.isEmpty()) {
            providerName = PROVIDER_NAME;
        }

        LOG.info("ECS initialize: provider='{}', name='{}', url='{}', mgmt_url='{}', insecure={}, s3_host='{}', namespace='{}'",
                providerName, name, url, mgmtUrl, insecure,
                details.get(S3_HOST), details.get(NAMESPACE));

        // Try ECS login up-front so we fail fast on bad config
        loginAndGetToken(mgmtUrl, saUser, saPass, insecure);

        // Put “canonical” values back into details (so DB keeps what we validated)
        details.put(MGMT_URL,  mgmtUrl);
        details.put(SA_USER,   saUser);
        details.put(SA_PASS,   saPass);
        details.put(INSECURE,  Boolean.toString(insecure));

        // Build objectStore parameters exactly like Ceph does
        Map<String, Object> objectStoreParameters = new HashMap<>();
        objectStoreParameters.put("name",         name);
        objectStoreParameters.put("url",          url);
        objectStoreParameters.put("size",         size);
        objectStoreParameters.put("providerName", providerName);

        try {
            LOG.info("ECS: creating ObjectStore in DB: name='{}', provider='{}', url='{}'",
                    name, providerName, url);
            ObjectStoreVO objectStore = objectStoreHelper.createObjectStore(objectStoreParameters, details);
            if (objectStore == null) {
                throw new CloudRuntimeException("ECS: createObjectStore returned null");
            }

            DataStore store = objectStoreMgr.getObjectStore(objectStore.getId());
            if (store == null) {
                throw new CloudRuntimeException("ECS: getObjectStore returned null for id=" + objectStore.getId());
            }

            LOG.info("ECS: object store created: id={}, name='{}'", objectStore.getId(), name);
            return store;
        } catch (RuntimeException e) {
            String msg = "ECS: failed to persist object store '" + name + "': " + safeMsg(e);
            LOG.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
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

    // ---------- helpers ----------

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    private static String safeMsg(Throwable t) {
        if (t == null) {
            return "unknown";
        }
        String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }

    private CloseableHttpClient buildHttpClient(boolean insecure) {
        if (!insecure) {
            return HttpClients.createDefault();
        }
        try {
            TrustStrategy trustAll = (chain, authType) -> true;
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, trustAll)
                    .build();
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS: failed to build HttpClient", e);
        }
    }

    private String loginAndGetToken(String mgmtUrl, String user, String pass, boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            HttpGet get = new HttpGet(mgmtUrl + "/login");
            get.addHeader(new BasicScheme().authenticate(
                    new UsernamePasswordCredentials(user, pass), get, null));
            try (CloseableHttpResponse resp = http.execute(get)) {
                int status = resp.getStatusLine().getStatusCode();
                if (status != 200 && status != 201) {
                    throw new CloudRuntimeException("ECS /login failed: HTTP " + status);
                }
                if (resp.getFirstHeader("X-SDS-AUTH-TOKEN") == null) {
                    throw new CloudRuntimeException("ECS /login missing X-SDS-AUTH-TOKEN");
                }
                return resp.getFirstHeader("X-SDS-AUTH-TOKEN").getValue();
            }
        } catch (Exception e) {
            String msg = "ECS: management login error: " + safeMsg(e);
            LOG.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
}
