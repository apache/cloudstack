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
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final String FQDN_ENV_VAR = "CLOUDSTACK_MSID_FROM_ID";
    private static final String FQDN_SYS_PROP = "cloudstack.msid.from.id";
    private static final String HOSTNAME_ENV_VAR = "HOSTNAME";
    

    // op_lock.mac is varchar(17) and holds the msid, so the id must stay within the 48-bit MAC address range.
    private static final int MSID_BYTES = 6;

    private static final Logger s_logger = LogManager.getLogger(ManagementServerNode.class);

    private static String s_nodeIdSource;
    private static final long s_nodeId = initNodeId();

    private static long initNodeId() {

        s_logger.info("Initializing management server node ID");
        // Check if FQDN_ENV_VAR or FQDN_SYS_PROP has a value
        String fqdnEnv = System.getenv(FQDN_ENV_VAR);
        String fqdnSysProp = System.getProperty(FQDN_SYS_PROP);

        String identity = null;
        if (fqdnEnv != null) {
            identity = trimToNull(fqdnEnv);
            s_logger.info("FQDN environment variable: {}", fqdnEnv);
        }
        if (fqdnSysProp != null) {
            identity = trimToNull(fqdnSysProp);
            s_logger.info("FQDN system property: {}", fqdnSysProp);
        }

        if (identity != null) {
            s_nodeIdSource = "fqdn";
            s_logger.info("Using {} for management server node ID: {}", s_nodeIdSource, identity);
            return hashNodeIdentity(identity);
        }

        // Use mac address
        s_nodeIdSource = "mac-address";
        long macAddress = MacAddress.getMacAddress().toLong();
        s_logger.info("Using {} for management server node ID: {}", s_nodeIdSource, macAddress);
        return macAddress;
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
