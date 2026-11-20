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

package com.cloud.hypervisor.kvm.storage.clvm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferAnswer.LeaseMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * Reads the sanlock lease of a CLVM/CLVM_NG volume from the VG's lvmlock LV. Every host in the
 * lockspace can read it, so one host names the lock holder of the volume.
 */
public class ClvmSanlockLeaseReader {

    private static final Logger LOGGER = LogManager.getLogger(ClvmSanlockLeaseReader.class);
    private static final long TIMEOUT_MS = 30000;
    private static final long LEASE_SIZE_BYTES = 1048576;
    private static final long RENEWING_IO_TIMEOUTS = 3;
    private static final Pattern SHARED_HOLDER = Pattern.compile("^\\s*(\\d+)\\s+\\d+\\s+SH\\s*$");

    /** Lease of one volume as seen on disk. */
    public static class Lease {
        final LeaseMode mode;
        final Long ownerId;
        final Long ownerGeneration;
        final List<Long> sharedOwnerIds;

        Lease(LeaseMode mode, Long ownerId, Long ownerGeneration, List<Long> sharedOwnerIds) {
            this.mode = mode;
            this.ownerId = ownerId;
            this.ownerGeneration = ownerGeneration;
            this.sharedOwnerIds = sharedOwnerIds;
        }

        public LeaseMode getMode() {
            return mode;
        }

        public Long getOwnerId() {
            return ownerId;
        }

        public List<Long> getSharedOwnerIds() {
            return sharedOwnerIds;
        }
    }

    public static Lease readLease(String vgName, String lvName) {
        String[] uuidAndOffset = readLockArgs(vgName, lvName);
        String dump = dumpLease(vgName, uuidAndOffset[1]);
        return parseDump(dump, uuidAndOffset[0]);
    }

    /** Liveness of the exclusive holder, as seen in this host's view of the lockspace. */
    public static class OwnerLiveness {
        final boolean alive;
        final boolean renewing;

        public OwnerLiveness(boolean alive, boolean renewing) {
            this.alive = alive;
            this.renewing = renewing;
        }

        public boolean isAlive() {
            return alive;
        }

        public boolean isRenewing() {
            return renewing;
        }
    }

    public static OwnerLiveness readOwnerLiveness(String vgName, Lease lease) {
        String status = ClvmSanlockHaChecker.readHostStatus(vgName);
        ClvmSanlockHaChecker.LeaseState state = status == null ? null : ClvmSanlockHaChecker.parseLeaseState(status, lease.ownerId);
        return new OwnerLiveness(isOwnerAlive(state, lease), isOwnerRenewing(state, lease));
    }

    /** True while the holder renews its lockspace lease, so its locks will not expire. Unknown counts as renewing. */
    protected static boolean isOwnerRenewing(ClvmSanlockHaChecker.LeaseState state, Lease lease) {
        if (state == null) {
            return true;
        }
        if (state.generation != null && !state.generation.equals(lease.ownerGeneration)) {
            return false;
        }
        return state.lastCheck - state.lastLive < RENEWING_IO_TIMEOUTS * state.ioTimeout;
    }

    protected static boolean isOwnerAlive(ClvmSanlockHaChecker.LeaseState state, Lease lease) {
        if (state == null) {
            LOGGER.warn("No lockspace lease found for sanlock host id [{}], assuming it is alive.", lease.ownerId);
            return true;
        }
        if (state.generation != null && !state.generation.equals(lease.ownerGeneration)) {
            return false;
        }
        return ClvmSanlockHaChecker.isLeaseAlive(state);
    }

    /** @return LV uuid (the sanlock resource name) and the lease offset in the lvmlock LV */
    private static String[] readLockArgs(String vgName, String lvName) {
        Script cmd = new Script("lvs", TIMEOUT_MS, LOGGER);
        cmd.add("--noheadings", "-o", "lv_uuid,lv_lockargs", vgName + "/" + lvName);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = cmd.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException(String.format("lvs failed for %s/%s: %s", vgName, lvName, result));
        }
        return parseLockArgs(parser.getLines());
    }

    /** Skips warning lines, e.g. "VG ... lock skipped: lock start in progress" while the lockspace is joining. */
    protected static String[] parseLockArgs(String output) {
        for (String line : output == null ? new String[0] : output.split("\\r?\\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2 && parts[1].matches("^[\\d.]+:\\d+$")) {
                return new String[] {parts[0], parts[1].substring(parts[1].lastIndexOf(':') + 1)};
            }
        }
        throw new CloudRuntimeException(String.format("No sanlock lock args in lvs output [%s]", output));
    }

    private static String dumpLease(String vgName, String offset) {
        Script cmd = new Script("sanlock", TIMEOUT_MS, LOGGER);
        cmd.add("direct", "dump", String.format("%s:%s:%d", lvmlockDevice(vgName), offset, LEASE_SIZE_BYTES));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = cmd.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException(String.format("sanlock direct dump failed for VG %s: %s", vgName, result));
        }
        return parser.getLines();
    }

    protected static String lvmlockDevice(String vgName) {
        return "/dev/mapper/" + vgName.replace("-", "--") + "-lvmlock";
    }

    /**
     * Parses a {@code sanlock direct dump} of one lease: a row "offset lockspace resource timestamp own gen lver",
     * followed by one "host_id gen SH" row per shared holder. A non-zero timestamp means held exclusively.
     */
    protected static Lease parseDump(String output, String lvUuid) {
        Long timestamp = null;
        Long owner = null;
        Long generation = null;
        List<Long> sharedOwnerIds = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String[] tokens = line.trim().split("\\s+");
            if (timestamp == null) {
                if (tokens.length >= 6 && tokens[2].equals(lvUuid)) {
                    timestamp = Long.parseLong(tokens[3]);
                    owner = Long.parseLong(tokens[4]);
                    generation = Long.parseLong(tokens[5]);
                }
                continue;
            }
            Matcher shared = SHARED_HOLDER.matcher(line);
            if (!shared.matches()) {
                break;
            }
            sharedOwnerIds.add(Long.parseLong(shared.group(1)));
        }
        if (timestamp == null) {
            throw new CloudRuntimeException(String.format("No sanlock lease for LV uuid %s in dump output", lvUuid));
        }
        if (timestamp != 0) {
            return new Lease(LeaseMode.EXCLUSIVE, owner, generation, sharedOwnerIds);
        }
        return new Lease(sharedOwnerIds.isEmpty() ? LeaseMode.FREE : LeaseMode.SHARED, null, null, sharedOwnerIds);
    }
}
