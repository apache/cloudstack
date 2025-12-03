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
import org.apache.http.client.methods.HttpGet;
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
    private static final String MGMT_URL = "mgmt_url";
    private static final String SA_USER = "sa_user";
    private static final String SA_PASS = "sa_password";
    private static final String INSECURE = "insecure";

    // optional details (currently not used in persistence logic but accepted)
    private static final String S3_HOST = "s3_host";
    private static final String NAMESPACE = "namespace";

    private static final String PROVIDER_NAME = "ECS";

    @Inject
    ObjectStoreHelper objectStoreHelper;

    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    public EcsObjectStoreLifeCycleImpl() {
    }

    @Override
    public DataStore initialize(final Map<String, Object> dsInfos) {
        requireInjected();

        final String url = getString(dsInfos, "url", true);
        final String name = getString(dsInfos, "name", true);
        final Long size = getLong(dsInfos, "size");
        final String providerName = getProviderName(dsInfos);

        final Map<String, String> details = getDetails(dsInfos);

        final EcsConfig cfg = verifyAndNormalize(details);

        LOG.info("ECS initialize: provider='{}', name='{}', url='{}', mgmt_url='{}', insecure={}, s3_host='{}', namespace='{}'",
                providerName, name, url, cfg.mgmtUrl, cfg.insecure,
                details.get(S3_HOST), details.get(NAMESPACE));

        loginAndGetToken(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure);

        applyCanonicalDetails(details, cfg);

        final Map<String, Object> objectStoreParameters = buildObjectStoreParams(name, url, size, providerName);

        try {
            LOG.info("ECS: creating ObjectStore in DB: name='{}', provider='{}', url='{}'",
                    name, providerName, url);

            final ObjectStoreVO objectStore = objectStoreHelper.createObjectStore(objectStoreParameters, details);
            if (objectStore == null) {
                throw new CloudRuntimeException("ECS: createObjectStore returned null");
            }

            final DataStore store = objectStoreMgr.getObjectStore(objectStore.getId());
            if (store == null) {
                throw new CloudRuntimeException("ECS: getObjectStore returned null for id=" + objectStore.getId());
            }

            LOG.info("ECS: object store created: id={}, name='{}'", objectStore.getId(), name);
            return store;
        } catch (RuntimeException e) {
            final String msg = "ECS: failed to persist object store '" + name + "': " + safeMsg(e);
            LOG.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    @Override
    public boolean attachCluster(final DataStore store, final ClusterScope scope) {
        return false;
    }

    @Override
    public boolean attachHost(final DataStore store, final HostScope scope, final StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(final DataStore dataStore, final ZoneScope scope, final HypervisorType hypervisorType) {
        return false;
    }

    @Override
    public boolean maintain(final DataStore store) {
        return false;
    }

    @Override
    public boolean cancelMaintain(final DataStore store) {
        return false;
    }

    @Override
    public boolean deleteDataStore(final DataStore store) {
        return false;
    }

    @Override
    public boolean migrateToObjectStore(final DataStore store) {
        return false;
    }

    // ---------- helpers ----------

    private static final class EcsConfig {
        final String mgmtUrl;
        final String saUser;
        final String saPass;
        final boolean insecure;

        private EcsConfig(final String mgmtUrl, final String saUser, final String saPass, final boolean insecure) {
            this.mgmtUrl = mgmtUrl;
            this.saUser = saUser;
            this.saPass = saPass;
            this.insecure = insecure;
        }
    }

    private void requireInjected() {
        if (objectStoreHelper == null) {
            throw new CloudRuntimeException("ECS: ObjectStoreHelper is not injected");
        }
        if (objectStoreMgr == null) {
            throw new CloudRuntimeException("ECS: ObjectStoreProviderManager is not injected");
        }
    }

    private static String getString(final Map<String, Object> dsInfos, final String key, final boolean required) {
        final Object v = dsInfos.get(key);
        final String s = (v == null) ? null : v.toString().trim();
        if (required && (s == null || s.isEmpty())) {
            throw new CloudRuntimeException("ECS: missing required parameter '" + key + "'");
        }
        return s;
    }

    private static Long getLong(final Map<String, Object> dsInfos, final String key) {
        final Object v = dsInfos.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS: invalid long for '" + key + "': " + v);
        }
    }

    private static String getProviderName(final Map<String, Object> dsInfos) {
        final String p = getString(dsInfos, "providerName", false);
        return (p == null || p.isEmpty()) ? PROVIDER_NAME : p;
    }

    private static Map<String, String> getDetails(final Map<String, Object> dsInfos) {
        final Object v = dsInfos.get("details");
        if (!(v instanceof Map)) {
            throw new CloudRuntimeException("ECS: details map is missing");
        }

        final Map<?, ?> raw = (Map<?, ?>) v;
        final Map<String, String> out = new HashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            final String k = e.getKey().toString();
            final String val = e.getValue() == null ? null : e.getValue().toString();
            out.put(k, val);
        }
        return out;
    }

    private static EcsConfig verifyAndNormalize(final Map<String, String> details) {
        final String mgmtUrl = trim(details.get(MGMT_URL));
        final String saUser = safe(details.get(SA_USER));
        final String saPass = safe(details.get(SA_PASS));
        final boolean insecure = Boolean.parseBoolean(details.getOrDefault(INSECURE, "false"));

        verifyRequiredDetail(MGMT_URL, mgmtUrl);
        verifyRequiredDetail(SA_USER, saUser);
        verifyRequiredDetail(SA_PASS, saPass);

        return new EcsConfig(mgmtUrl, saUser, saPass, insecure);
    }

    private static void verifyRequiredDetail(final String key, final String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new CloudRuntimeException("ECS: missing required detail '" + key + "'");
        }
    }

    private static void applyCanonicalDetails(final Map<String, String> details, final EcsConfig cfg) {
        details.put(MGMT_URL, cfg.mgmtUrl);
        details.put(SA_USER, cfg.saUser);
        details.put(SA_PASS, cfg.saPass);
        details.put(INSECURE, Boolean.toString(cfg.insecure));
    }

    private static Map<String, Object> buildObjectStoreParams(final String name,
                                                             final String url,
                                                             final Long size,
                                                             final String providerName) {
        final Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("url", url);
        p.put("size", size);
        p.put("providerName", providerName);
        return p;
    }

    private static String safe(final String v) {
        return v == null ? "" : v.trim();
    }

    private static String trim(final String v) {
        if (v == null) {
            return null;
        }
        final String s = v.trim();
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String safeMsg(final Throwable t) {
        if (t == null) {
            return "unknown";
        }
        final String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }

    private CloseableHttpClient buildHttpClient(final boolean insecure) {
        if (!insecure) {
            return HttpClients.createDefault();
        }
        try {
            final TrustStrategy trustAll = (chain, authType) -> true;
            final SSLContext sslContext = SSLContextBuilder.create()
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

    private String loginAndGetToken(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(mgmtUrl + "/login");
            get.addHeader(new BasicScheme().authenticate(
                    new UsernamePasswordCredentials(user, pass), get, null));
            try (CloseableHttpResponse resp = http.execute(get)) {
                final int status = resp.getStatusLine().getStatusCode();
                if (status != 200 && status != 201) {
                    throw new CloudRuntimeException("ECS /login failed: HTTP " + status);
                }
                if (resp.getFirstHeader("X-SDS-AUTH-TOKEN") == null) {
                    throw new CloudRuntimeException("ECS /login missing X-SDS-AUTH-TOKEN");
                }
                return resp.getFirstHeader("X-SDS-AUTH-TOKEN").getValue();
            }
        } catch (Exception e) {
            final String msg = "ECS: management login error: " + safeMsg(e);
            LOG.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
}
