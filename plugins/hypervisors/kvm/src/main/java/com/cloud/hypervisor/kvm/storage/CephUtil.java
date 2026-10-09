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
package com.cloud.hypervisor.kvm.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.exceptions.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdImage;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;

/**
 * Helpers for the Ceph cluster handles the KVM agent opens.
 *
 * Callers get two things here that are easy to leave out by hand:
 *
 * <ul>
 * <li>Bounded operations. librados defaults <code>rados_osd_op_timeout</code> and
 * <code>rados_mon_op_timeout</code> to 0, which means an operation waits forever. A completion that never
 * arrives parks the calling agent thread for the life of the process, and because the agent runs storage
 * commands for a host in sequence, every command queued behind it stops with it.</li>
 * <li>Deterministic clean up. {@link Rados} releases its native cluster handle from
 * <code>finalize()</code>, so a handle that is not shut down explicitly survives until the garbage
 * collector happens to reach it, at which point <code>rados_shutdown</code> runs on the finalizer
 * thread rather than on the thread that did the work.</li>
 * </ul>
 */
public final class CephUtil {

    private static final Logger logger = LogManager.getLogger(CephUtil.class);

    private static final String CLIENT_MOUNT_TIMEOUT = "client_mount_timeout";
    private static final String RADOS_OSD_OP_TIMEOUT = "rados_osd_op_timeout";
    private static final String RADOS_MON_OP_TIMEOUT = "rados_mon_op_timeout";

    private CephUtil() {
    }

    /**
     * Opens a connected cluster handle with the agent's configured timeouts applied.
     *
     * @param authUserName the cephx user
     * @param monHost monitor host
     * @param monPort monitor port
     * @param authSecret the cephx secret
     * @return a connected handle the caller must pass to {@link #shutDownQuietly(Rados)} when done
     */
    public static Rados connect(String authUserName, String monHost, int monPort, String authSecret) throws RadosException {
        return connect(authUserName, monHost, monPort, authSecret, null);
    }

    /**
     * Opens a connected cluster handle with the agent's configured timeouts applied, optionally placing new
     * images' data in a separate pool.
     *
     * @param dataPool the RBD data pool for images created on this handle, or null to leave it unset
     */
    public static Rados connect(String authUserName, String monHost, int monPort, String authSecret, String dataPool) throws RadosException {
        Rados r = new Rados(authUserName);
        try {
            r.confSet("mon_host", monHost + ":" + monPort);
            /*
             * The secret is null when the pool has no cephx user, and librados aborts the process rather
             * than returning an error if it is handed a null value here.
             */
            if (authUserName != null) {
                r.confSet("key", authSecret);
            } else {
                r.confSet("auth_client_required", "none");
            }
            applyTimeouts(r);
            if (dataPool != null) {
                logger.debug("Setting RBD data pool to [{}] for images created on this connection.", dataPool);
                r.confSet(KVMPhysicalDisk.RBD_DEFAULT_DATA_POOL, dataPool);
            }
            r.connect();
            logger.debug("Successfully connected to Ceph cluster at [{}].", r.confGet("mon_host"));
            return r;
        } catch (Exception e) {
            /*
             * The handle never reaches the caller, so nothing else can release it. rados_create() has
             * already run by this point, so without this it would survive until finalize().
             */
            shutDownQuietly(r);
            throw e;
        }
    }

    /**
     * Applies the connect and operation timeouts from the agent properties. A timeout configured as 0 is
     * left unset, so librados uses its own default: waiting forever for the two operation timeouts, and
     * 300 seconds for the connect timeout.
     */
    private static void applyTimeouts(Rados r) throws RadosException {
        int mountTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.RADOS_CLIENT_MOUNT_TIMEOUT);
        if (mountTimeout > 0) {
            r.confSet(CLIENT_MOUNT_TIMEOUT, String.valueOf(mountTimeout));
        }

        int osdOpTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.RADOS_OSD_OP_TIMEOUT);
        if (osdOpTimeout > 0) {
            r.confSet(RADOS_OSD_OP_TIMEOUT, String.valueOf(osdOpTimeout));
        }

        int monOpTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.RADOS_MON_OP_TIMEOUT);
        if (monOpTimeout > 0) {
            r.confSet(RADOS_MON_OP_TIMEOUT, String.valueOf(monOpTimeout));
        }
    }

    /**
     * Releases the native cluster handle. Safe to call with null and on a handle that never connected, so it
     * can be used from a finally block without guarding the happy path.
     */
    public static void shutDownQuietly(Rados r) {
        if (r == null) {
            return;
        }
        try {
            r.shutDown();
        } catch (Exception e) {
            logger.warn("Failed to shut down the Ceph connection, it will be released when the handle is collected.", e);
        }
    }

    /**
     * Destroys an IO context. Safe to call with nulls so it can be used from a finally block.
     */
    public static void ioCtxDestroyQuietly(Rados r, IoCTX io) {
        if (io == null) {
            return;
        }
        if (r == null) {
            logger.warn("Cannot destroy the Ceph IO context without its cluster handle.");
            return;
        }
        try {
            r.ioCtxDestroy(io);
        } catch (Exception e) {
            logger.warn("Failed to destroy the Ceph IO context.", e);
        }
    }

    /**
     * Closes an RBD image without throwing, so that a failure to close cannot discard a successful result or
     * mask an exception that is already on its way out.
     */
    public static void closeQuietly(Rbd rbd, RbdImage image, String imageName) {
        if (rbd == null || image == null) {
            return;
        }
        try {
            rbd.close(image);
        } catch (Exception e) {
            logger.warn("Failed to close RBD image [{}].", imageName, e);
        }
    }
}
