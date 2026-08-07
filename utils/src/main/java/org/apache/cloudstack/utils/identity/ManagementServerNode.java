//
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
//

package org.apache.cloudstack.utils.identity;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

/**
 * Canonical source of the management-server node id ({@code msid}).
 *
 * <p>By default the id is derived from the host hardware MAC address. When the MAC address is
 * not stable across restarts, the {@code msid} changes, which orphans the {@code mshost} row
 * and breaks async jobs, HA work ({@code fk_op_ha_work__mgmt_server_id}), and router/stats
 * ownership.
 *
 * <p>Setting the environment variable {@code CLOUDSTACK_MSID_FROM_FQDN=true} (or the system
 * property {@code cloudstack.msid.from.fqdn=true}) instead derives the id from a SHA-256 hash
 * of the node FQDN, which stays stable across restarts. All node-identity consumers must
 * obtain the id from {@link #getManagementServerId()} so they agree on the same value.
 */
public class ManagementServerNode extends AdapterBase implements SystemIntegrityChecker {

    private static final String FQDN_ENV_VAR = "CLOUDSTACK_MSID_FROM_FQDN";
    private static final String FQDN_SYS_PROP = "cloudstack.msid.from.fqdn";
    private static final String IDENTITY_ENV_VAR = "CLOUDSTACK_MSID_IDENTITY";
    private static final String IDENTITY_SYS_PROP = "cloudstack.msid.identity";
    private static final String HOSTNAME_ENV_VAR = "HOSTNAME";
    private static final String POD_NAMESPACE_ENV_VAR = "POD_NAMESPACE";

    // op_lock.mac is varchar(17) and holds the msid, so the id must stay within the 48-bit MAC address range.
    private static final int MSID_BYTES = 6;

    private static String s_nodeIdSource;
    private static final long s_nodeId = initNodeId();

    private static long initNodeId() {
        if (isTruthy(System.getenv(FQDN_ENV_VAR)) || isTruthy(System.getProperty(FQDN_SYS_PROP))) {
            return generateIdFromStableIdentity();
        }

        s_nodeIdSource = "mac-address";
        return MacAddress.getMacAddress().toLong();
    }

    static String resolveNodeIdentity(String explicitIdentity, String hostnameEnv, String podNamespaceEnv,
            String detectedHostName, String canonicalHostName) {
        String configuredIdentity = trimToNull(explicitIdentity);
        if (configuredIdentity != null) {
            return configuredIdentity;
        }

        String hostName = trimToNull(hostnameEnv);
        if (hostName != null) {
            String podNamespace = trimToNull(podNamespaceEnv);
            return podNamespace == null ? hostName : hostName + "." + podNamespace;
        }

        String detected = trimToNull(detectedHostName);
        String canonical = trimToNull(canonicalHostName);
        if (detected != null && canonical != null && !detected.equals(canonical)) {
            return detected + "|" + canonical;
        }

        return canonical != null ? canonical : detected;
    }

    static long hashNodeIdentity(String nodeIdentity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(nodeIdentity.getBytes(StandardCharsets.UTF_8));
            long id = 0;
            for (int i = 0; i < MSID_BYTES; i++) {
                id = (id << 8) | (hash[i] & 0xFFL);
            }

            return id == 0 ? 1 : id;
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("SHA-256 algorithm not available for management server ID generation", e);
        }
    }

    private static long generateIdFromStableIdentity() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String nodeIdentity = resolveNodeIdentity(
                    firstNonBlank(System.getProperty(IDENTITY_SYS_PROP), System.getenv(IDENTITY_ENV_VAR)),
                    System.getenv(HOSTNAME_ENV_VAR),
                    System.getenv(POD_NAMESPACE_ENV_VAR),
                    localHost.getHostName(),
                    localHost.getCanonicalHostName());

            if (nodeIdentity == null) {
                throw new CloudRuntimeException("Unable to resolve a stable management server identity");
            }

            s_nodeIdSource = "identity:" + nodeIdentity;
            return hashNodeIdentity(nodeIdentity);
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to generate management server ID from host identity", e);
        }
    }

    private static String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim();
        return "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed) || "yes".equalsIgnoreCase(trimmed);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public ManagementServerNode() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK_BOOTSTRAP);
    }

    @Override
    public void check() {
        if (s_nodeId <= 0) {
            throw new CloudRuntimeException("Unable to get the management server node id");
        }
    }

    public static long getManagementServerId() {
        return s_nodeId;
    }

    @Override
    public boolean start() {
        try {
            check();
        } catch (Exception e) {
            logger.error("System integrity check exception", e);
            System.exit(1);
        }

        logger.info("Management server node id: {} (source: {})", s_nodeId, s_nodeIdSource);
        return true;
    }
}
