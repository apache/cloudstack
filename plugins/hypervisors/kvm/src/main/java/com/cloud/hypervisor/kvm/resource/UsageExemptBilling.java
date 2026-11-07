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
package com.cloud.hypervisor.kvm.resource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * The billing side of usage exemption on KVM hosts. It takes each tap's exempt bytes off its libvirt totals and keeps the
 * reported numbers safe to bill. {@link UsageExemptCounters} reads the exempt bytes from nftables and passes them in.
 * <ul>
 * <li>The management server reads a smaller number as a counter reset and bills the previous one again. So for each tap
 * the reported numbers never go down. The record of what was reported lives in /run. It survives an agent restart, and
 * a host reboot clears it along with the tap counters.</li>
 * <li>When a tap's exempt counters are wiped, for example by someone flushing nftables, the bytes they had counted are
 * kept and still taken off.</li>
 * <li>A tap with a new ifindex, or with libvirt totals that went down, is a new tap. Its record starts again from zero,
 * the same as libvirt's counters do.</li>
 * </ul>
 */
public class UsageExemptBilling {
    protected Logger logger = LogManager.getLogger(getClass());

    /** What was reported for one tap last time, and the counts behind it. */
    static class NicState {
        int ifindex;
        long libvirtSent;
        long libvirtReceived;
        long exemptSent;
        long exemptReceived;
        long exemptSentBeforeReset;
        long exemptReceivedBeforeReset;
        long reportedSent;
        long reportedReceived;

        NicState() {
        }

        NicState(int ifindex) {
            this.ifindex = ifindex;
        }
    }

    /** Exempt bytes counted on one tap. Bytes sent include the Ethernet header, the same as libvirt counts them. */
    public static class ExemptCount {
        long sent;
        long received;

        ExemptCount() {
        }

        ExemptCount(long sent, long received) {
            this.sent = sent;
            this.received = received;
        }
    }

    private final Path stateFile;
    private final Gson gson = new Gson();
    private Map<String, NicState> nicStates;

    UsageExemptBilling(Path stateFile) {
        this.stateFile = stateFile;
    }

    /** Whether there's a record from earlier in this boot. */
    boolean hasSavedState() {
        return Files.exists(stateFile);
    }

    /**
     * Takes exempt bytes off one tap's libvirt totals and returns the bytes to report as sent and received.
     * Pass null for exempt when the counters couldn't be read. The tap then keeps its exempt bytes from last time.
     */
    synchronized long[] subtractExempt(String device, int ifindex, long sent, long received, ExemptCount exempt) {
        loadState();
        NicState nic = nicStates.get(device);
        if (nic == null || nic.ifindex != ifindex || sent < nic.libvirtSent || received < nic.libvirtReceived) {
            nic = new NicState(ifindex);
            nicStates.put(device, nic);
        }
        if (exempt == null) {
            exempt = new ExemptCount(nic.exemptSent, nic.exemptReceived);
        }
        if (exempt.sent < nic.exemptSent) {
            nic.exemptSentBeforeReset += nic.exemptSent;
        }
        if (exempt.received < nic.exemptReceived) {
            nic.exemptReceivedBeforeReset += nic.exemptReceived;
        }
        nic.exemptSent = exempt.sent;
        nic.exemptReceived = exempt.received;
        nic.libvirtSent = sent;
        nic.libvirtReceived = received;
        nic.reportedSent = billable(sent, exempt.sent + nic.exemptSentBeforeReset, nic.reportedSent);
        nic.reportedReceived = billable(received, exempt.received + nic.exemptReceivedBeforeReset, nic.reportedReceived);
        return new long[] {nic.reportedSent, nic.reportedReceived};
    }

    /** The total less exempt bytes, held between what was reported last time and the total. */
    static long billable(long total, long exempt, long reported) {
        return Math.max(reported, Math.min(total, Math.max(0, total - exempt)));
    }

    /**
     * Called when a tap is getting a new counter for what it sends, receives or both, because the old one is gone. Keeps
     * what the old counter had counted. By the next stats collection the new counter may have counted past it, and
     * {@link #subtractExempt} would no longer see the drop. Returns true if anything was kept.
     */
    synchronized boolean keepCountsFromLostChains(int ifindex, boolean sent, boolean received) {
        if (!sent && !received) {
            return false;
        }
        loadState();
        boolean keptCounts = false;
        for (NicState nic : nicStates.values()) {
            if (nic.ifindex != ifindex) {
                continue;
            }
            if (sent && nic.exemptSent > 0) {
                nic.exemptSentBeforeReset += nic.exemptSent;
                nic.exemptSent = 0;
                keptCounts = true;
            }
            if (received && nic.exemptReceived > 0) {
                nic.exemptReceivedBeforeReset += nic.exemptReceived;
                nic.exemptReceived = 0;
                keptCounts = true;
            }
        }
        return keptCounts;
    }

    /** Writes the records to /run, dropping taps that no longer exist. tapExists gets each tap's name and ifindex. */
    synchronized void saveState(BiPredicate<String, Integer> tapExists) {
        if (nicStates == null) {
            return;
        }
        nicStates.entrySet().removeIf(nic -> !tapExists.test(nic.getKey(), nic.getValue().ifindex));
        try {
            Files.createDirectories(stateFile.getParent());
            final Path temp = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
            Files.write(temp, gson.toJson(nicStates).getBytes(StandardCharsets.UTF_8));
            Files.move(temp, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warn("Failed to save {}", stateFile, e);
        }
    }

    private void loadState() {
        if (nicStates != null) {
            return;
        }
        nicStates = new HashMap<>();
        if (!Files.exists(stateFile)) {
            return;
        }
        try {
            final Type type = new TypeToken<Map<String, NicState>>() { }.getType();
            final Map<String, NicState> saved = gson.fromJson(new String(Files.readAllBytes(stateFile), StandardCharsets.UTF_8), type);
            if (saved != null) {
                nicStates.putAll(saved);
            }
        } catch (IOException | RuntimeException e) {
            logger.warn("Failed to read {}. Starting afresh.", stateFile, e);
        }
    }
}
