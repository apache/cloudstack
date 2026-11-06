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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * Reads a host's sanlock delta-lease state for a CLVM/CLVM_NG volume group; the lease is renewed
 * by sanlock itself, and a host that stops renewing is reset by its watchdog.
 */
public class ClvmSanlockHaChecker {

    protected static final String LOCKSPACE_PREFIX = "lvm_";
    private static final String LVMLOCKD_SOCKET = "/run/lvm/lvmlockd.socket";
    protected static final long WATCHDOG_FIRE_TIMEOUT_SECONDS = 60;
    protected static final long ID_RENEWAL_FAIL_IO_TIMEOUTS = 8;
    private static final long CHECK_TIMEOUT_MS = 30000;

    private static final Logger LOGGER = LogManager.getLogger(ClvmSanlockHaChecker.class);
    private static final Pattern HOST_ID_LINE = Pattern.compile("^host_id=(\\d+)$");
    private static final Pattern HOST_ENTRY = Pattern.compile("^(\\d+) timestamp \\d+\\s*$");

    protected static class LeaseState {
        final long lastCheck;
        final long lastLive;
        final long ioTimeout;

        LeaseState(long lastCheck, long lastLive, long ioTimeout) {
            this.lastCheck = lastCheck;
            this.lastLive = lastLive;
            this.ioTimeout = ioTimeout;
        }
    }

    /** Returns this host's lvmlockd sanlock host_id, or null when lvmlockd is not running. */
    public static Long readLocalHostId() {
        if (!new File(LVMLOCKD_SOCKET).exists()) {
            return null;
        }
        Script cmd = new Script("lvmconfig", CHECK_TIMEOUT_MS, LOGGER);
        cmd.add("local/host_id");
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        if (cmd.execute(parser) != null) {
            return null;
        }
        return parseHostId(parser.getLine());
    }

    protected static Long parseHostId(String line) {
        Matcher matcher = HOST_ID_LINE.matcher(line == null ? "" : line.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            LOGGER.warn("Ignoring invalid sanlock host id [{}]", line);
            return null;
        }
    }

    /** Returns false only when the lease stopped renewing long enough that the watchdog has reset the host. */
    public static boolean isHostLeaseAlive(String vgName, Long hostId) {
        if (vgName == null || hostId == null) {
            LOGGER.warn("Cannot check the sanlock lease: VG name [{}] or host id [{}] is unknown, assuming the host is alive.", vgName, hostId);
            return true;
        }
        String output = readHostStatus(vgName);
        if (output == null) {
            return true;
        }
        LeaseState state = parseLeaseState(output, hostId);
        if (state == null) {
            LOGGER.warn("No sanlock lease found for host id [{}] in lockspace [{}{}], assuming the host is alive.", hostId, LOCKSPACE_PREFIX, vgName);
            return true;
        }
        return isLeaseAlive(state);
    }

    private static String readHostStatus(String vgName) {
        Script cmd = new Script("sanlock", CHECK_TIMEOUT_MS, LOGGER);
        cmd.add("client", "host_status", "-s", LOCKSPACE_PREFIX + vgName, "-D");
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = cmd.execute(parser);
        if (result != null) {
            LOGGER.warn("Failed to read sanlock host status for lockspace [{}{}]: {}", LOCKSPACE_PREFIX, vgName, result);
            return null;
        }
        return parser.getLines();
    }

    protected static LeaseState parseLeaseState(String output, long hostId) {
        try {
            return readLeaseState(output, hostId);
        } catch (NumberFormatException e) {
            LOGGER.warn("Unexpected sanlock host status for host id [{}]: {}", hostId, e.getMessage());
            return null;
        }
    }

    private static LeaseState readLeaseState(String output, long hostId) {
        boolean inHost = false;
        Long lastCheck = null;
        Long lastLive = null;
        Long ioTimeout = null;
        for (String line : output.split("\\r?\\n")) {
            Matcher entry = HOST_ENTRY.matcher(line);
            if (entry.matches()) {
                if (inHost) {
                    break;
                }
                inHost = Long.parseLong(entry.group(1)) == hostId;
            } else if (inHost) {
                lastCheck = valueOrCurrent(line, "last_check", lastCheck);
                lastLive = valueOrCurrent(line, "last_live", lastLive);
                ioTimeout = valueOrCurrent(line, "io_timeout", ioTimeout);
            }
        }
        if (lastCheck == null || lastLive == null || ioTimeout == null) {
            return null;
        }
        return new LeaseState(lastCheck, lastLive, ioTimeout);
    }

    private static Long valueOrCurrent(String line, String key, Long current) {
        String trimmed = line.trim();
        if (trimmed.startsWith(key + "=")) {
            return Long.parseLong(trimmed.substring(key.length() + 1));
        }
        return current;
    }

    protected static boolean isLeaseAlive(LeaseState state) {
        long hostDeadSeconds = ID_RENEWAL_FAIL_IO_TIMEOUTS * state.ioTimeout + WATCHDOG_FIRE_TIMEOUT_SECONDS;
        return state.lastCheck - state.lastLive < hostDeadSeconds;
    }
}
