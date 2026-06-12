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

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.driver.EcsCfg;
import org.apache.cloudstack.storage.datastore.driver.EcsUtils;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

public class EcsObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {

    private static final Logger LOG = LogManager.getLogger(EcsObjectStoreLifeCycleImpl.class);

    private static final String MGMT_URL  = "mgmt_url";
    private static final String SA_USER   = "sa_user";
    private static final String SA_PASS   = "sa_password";
    private static final String INSECURE  = "insecure";
    private static final String S3_HOST   = "s3_host";
    private static final String NAMESPACE = "namespace";

    static final String PROVIDER_NAME = "ECS";

    @Inject ObjectStoreHelper objectStoreHelper;
    @Inject ObjectStoreProviderManager objectStoreMgr;

    public EcsObjectStoreLifeCycleImpl() { }

    @Override
    public DataStore initialize(final Map<String, Object> dsInfos) {
        requireInjected();

        final String url  = getString(dsInfos, "url", true);
        final String name = getString(dsInfos, "name", true);
        final Long size   = getLong(dsInfos, "size");

        final Map<String, String> details = getDetails(dsInfos);
        final EcsCfg cfg = verifyAndNormalize(details);

        LOG.info("ECS initialize: provider='{}', name='{}', url='{}', mgmt_url='{}', insecure={}, s3_host='{}', namespace='{}'",
                PROVIDER_NAME, name, url, cfg.mgmtUrl, cfg.insecure,
                details.get(S3_HOST), details.get(NAMESPACE));

        EcsUtils.verifyLogin(cfg.mgmtUrl, cfg.saUser, cfg.saPass, cfg.insecure);

        applyCanonicalDetails(details, cfg);

        final Map<String, Object> objectStoreParameters = buildObjectStoreParams(name, url, size);

        try {
            LOG.info("ECS: creating ObjectStore in DB: name='{}', provider='{}', url='{}'",
                    name, PROVIDER_NAME, url);

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

    @Override public boolean attachCluster(final DataStore store, final ClusterScope scope) { return false; }
    @Override public boolean attachHost(final DataStore store, final HostScope scope, final StoragePoolInfo existingInfo) { return false; }
    @Override public boolean attachZone(final DataStore dataStore, final ZoneScope scope, final HypervisorType hypervisorType) { return false; }
    @Override public boolean maintain(final DataStore store) { return false; }
    @Override public boolean cancelMaintain(final DataStore store) { return false; }
    @Override public boolean deleteDataStore(final DataStore store) { return false; }
    @Override public boolean migrateToObjectStore(final DataStore store) { return false; }

    // ---------- helpers ----------

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
        final String s = v == null ? null : v.toString().trim();
        if (required && StringUtils.isEmpty(s)) {
            throw new CloudRuntimeException("ECS: missing required parameter '" + key + "'");
        }
        return s;
    }

    private static Long getLong(final Map<String, Object> dsInfos, final String key) {
        final Object v = dsInfos.get(key);
        if (v == null) return null;
        return (Long) v;
    }

    private static Map<String, String> getDetails(final Map<String, Object> dsInfos) {
        final Object v = dsInfos.get("details");
        if (!(v instanceof Map)) {
            throw new CloudRuntimeException("ECS: details map is missing");
        }
        final Map<?, ?> raw = (Map<?, ?>) v;
        final Map<String, String> out = new HashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getKey() == null) continue;
            out.put(e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString());
        }
        return out;
    }

    private static EcsCfg verifyAndNormalize(final Map<String, String> details) {
        final String mgmtUrl = EcsUtils.trimTail(details.get(MGMT_URL));
        final String saUser  = safe(details.get(SA_USER));
        final String saPass  = safe(details.get(SA_PASS));
        final String ns      = StringUtils.defaultIfBlank(details.get(NAMESPACE), "default");
        final boolean insecure = Boolean.parseBoolean(details.getOrDefault(INSECURE, "false"));

        verifyRequiredDetail(MGMT_URL, mgmtUrl);
        verifyRequiredDetail(SA_USER, saUser);
        verifyRequiredDetail(SA_PASS, saPass);

        return new EcsCfg(mgmtUrl, saUser, saPass, ns, insecure);
    }

    private static void verifyRequiredDetail(final String key, final String value) {
        if (StringUtils.isEmpty(value)) {
            throw new CloudRuntimeException("ECS: missing required detail '" + key + "'");
        }
    }

    private static void applyCanonicalDetails(final Map<String, String> details, final EcsCfg cfg) {
        details.put(MGMT_URL, cfg.mgmtUrl);
        details.put(SA_USER, cfg.saUser);
        details.put(SA_PASS, cfg.saPass);
        details.put(INSECURE, Boolean.toString(cfg.insecure));
    }

    private static Map<String, Object> buildObjectStoreParams(final String name, final String url, final Long size) {
        final Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("url", url);
        p.put("size", size);
        p.put("providerName", PROVIDER_NAME);
        return p;
    }

    private static String safe(final String v) {
        return v == null ? "" : v.trim();
    }

    private static String safeMsg(final Throwable t) {
        if (t == null) return "unknown";
        final String m = t.getMessage();
        return StringUtils.isEmpty(m) ? t.getClass().getSimpleName() : m;
    }
}
