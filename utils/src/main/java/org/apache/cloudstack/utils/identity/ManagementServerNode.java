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

    private static String s_nodeIdSource;
    private static Exception s_initError;
    private static final long s_nodeId = initNodeId();

    private static long initNodeId() {
        if (isFqdnModeEnabled()) {
            return generateIdFromFqdn();
        }
        s_nodeIdSource = "mac-address";
        return MacAddress.getMacAddress().toLong();
    }

    private static boolean isFqdnModeEnabled() {
        return isTruthy(System.getenv(FQDN_ENV_VAR)) || isTruthy(System.getProperty(FQDN_SYS_PROP));
    }

    private static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed) || "yes".equalsIgnoreCase(trimmed);
    }

    /**
     * Derives a stable node id from a SHA-256 hash of the local FQDN.
     *
     * <p>On failure it records the cause and returns {@code 0} (an invalid id) rather than
     * silently reverting to an unstable MAC-based id. The invalid id makes {@link #check()}
     * fail the system-integrity check, which stops startup cleanly via {@link #start()}
     * instead of raising an {@code ExceptionInInitializerError} from static initialization.
     *
     * @return a positive, non-zero 48-bit id, or {@code 0} if it cannot be derived
     */
    private static long generateIdFromFqdn() {
        try {
            String fqdn = InetAddress.getLocalHost().getCanonicalHostName();
            s_nodeIdSource = "fqdn:" + fqdn;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fqdn.getBytes(StandardCharsets.UTF_8));
            long id = 0;
            for (int i = 0; i < 6; i++) {
                id = (id << 8) | (hash[i] & 0xFF);
            }
            // Ensure positive and non-zero
            id = id & 0x7FFFFFFFFFFFFFFFL;
            if (id == 0) {
                id = 1;
            }
            return id;
        } catch (Exception e) {
            s_nodeIdSource = "fqdn-error";
            s_initError = e;
            return 0;
        }
    }

    public ManagementServerNode() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK_BOOTSTRAP);
    }

    @Override
    public void check() {
        if (s_nodeId <= 0) {
            throw new CloudRuntimeException(
                    "Unable to derive the management server node id (source: " + s_nodeIdSource + ")", s_initError);
        }
    }

    public static long getManagementServerId() {
        return s_nodeId;
    }

    @Override
    public boolean start() {
        try {
            check();
        } catch (CloudRuntimeException e) {
            logger.error("System integrity check failed for the management server node id", e);
            throw e;
        }
        logger.info("Management server node id: {} (source: {})", s_nodeId, s_nodeIdSource);
        return true;
    }
}
