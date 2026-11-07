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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.cloud.hypervisor.kvm.resource.UsageExemptBilling.ExemptCount;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * Keeps traffic between VMs and the addresses in network.usage.exempt.cidrs out of the network usage this host reports.
 * <p>
 * The usage we report comes from libvirt, which reads the byte counters on each VM NIC's tap device. Those are plain
 * totals, so we put a second counter on the same tap that only counts exempt traffic, and report the total minus that.
 * The counters are nftables rules in a netdev table. A tap's ingress hook sees what the VM sends and its egress hook sees
 * what the VM receives. Hooking the tap works whichever bridge or switch it's plugged into.
 * <p>
 * This class looks after the nftables side and reads the counts. {@link UsageExemptBilling} does the subtraction and keeps
 * the reported numbers safe to bill.
 * <ul>
 * <li>The ingress hook runs after the kernel has read past the 14 byte Ethernet header, so it doesn't count it. The tap
 * counters do, so we add it back for each packet the VM sends. The egress hook sees the whole frame already.</li>
 * <li>Chains are named after the tap's ifindex, which the kernel doesn't reuse until a reboot. A new tap that gets an old
 * name starts from zero, the same as libvirt's counters do.</li>
 * <li>Only taps that belong to user VMs get counters. Usage from the host is only billed for VMs on shared networks,
 * so system VMs such as virtual routers don't need them. User VMs on isolated and VPC networks still get counters, even
 * though their usage comes from the virtual router. The host can't tell which kind of network a NIC is on. Telling it
 * would mean sending extra NIC details from the management server when a VM starts, gets a new NIC or migrates.</li>
 * </ul>
 * The egress hook needs Linux 5.16 or later, or a kernel with it backported, such as EL9. On older kernels we log one
 * warning and report the libvirt totals unchanged.
 */
public class UsageExemptCounters {
    protected Logger logger = LogManager.getLogger(getClass());

    static final String TABLE = "cloudstack_usage_exempt";
    static final int ETHERNET_HEADER_BYTES = 14;
    static final long SYNC_INTERVAL_SECONDS = 10;
    private static final String SYS_CLASS_NET = "/sys/class/net";
    private static final String STATE_FILE = "/run/cloudstack-agent/usage-exempt.json";
    private static final long NFT_TIMEOUT_MS = 30000;
    private static final Pattern CHAIN_NAME = Pattern.compile("(in|out)_(\\d+)");
    private static final Pattern DEVICE_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,15}");

    private final Path netDir;
    private final UsageExemptBilling billing;

    private List<String> cidrs;
    private List<String> appliedCidrs;
    private Map<Integer, ExemptCount> exemptCounts;
    private ScheduledExecutorService periodicSync;
    private Boolean egressSupported;
    private boolean active;
    private Supplier<Map<String, String>> tapOwners;
    private final Map<Integer, Boolean> isUserVmTap = new HashMap<>();

    /** Marks a stats collection where we couldn't read the counters. NICs keep the exempt counts from last time. */
    private static final Map<Integer, ExemptCount> COUNTS_UNREADABLE = new HashMap<>();

    public UsageExemptCounters() {
        this(Paths.get(SYS_CLASS_NET), Paths.get(STATE_FILE));
    }

    UsageExemptCounters(Path netDir, Path stateFile) {
        this.netDir = netDir;
        this.billing = new UsageExemptBilling(stateFile);
    }

    /**
     * Sets how we find which VM owns each tap: a map from NIC device name to VM name, or null if it can't be read.
     */
    public synchronized void setTapOwners(Supplier<Map<String, String>> tapOwners) {
        this.tapOwners = tapOwners;
    }

    /**
     * Takes a new exempt list from the management server and applies it straight away. The server sends it on every
     * connect, empty when the setting isn't used. A host with an empty list and no table from before does nothing.
     * Otherwise we also start a sync every {@link #SYNC_INTERVAL_SECONDS} seconds, which puts counters on new taps, such
     * as a VM that just started or migrated here, and repairs anything that went missing.
     */
    public synchronized void setCidrs(List<String> cidrs) {
        this.cidrs = new ArrayList<>(cidrs);
        // An empty list only matters if this host has been counting since it booted.
        active = !cidrs.isEmpty() || (billing.hasSavedState() && tableExists());
        if (!active) {
            return;
        }
        try {
            sync();
        } catch (RuntimeException e) {
            logger.warn("Failed to set up the usage exemption counters. The next sync will try again.", e);
        }
        if (Boolean.FALSE.equals(egressSupported)) {
            active = false;
            return;
        }
        if (periodicSync == null && !cidrs.isEmpty()) {
            periodicSync = startPeriodicSync();
        }
    }

    protected ScheduledExecutorService startPeriodicSync() {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "UsageExemptCounters");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::syncQuietly, SYNC_INTERVAL_SECONDS, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
        return executor;
    }

    private void syncQuietly() {
        try {
            synchronized (this) {
                if (active && cidrs != null && !cidrs.isEmpty()) {
                    sync();
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Failed to sync the usage exemption counters", e);
        }
    }

    /**
     * Reads our counters ahead of a stats collection, after making sure every tap has them. Call {@link #subtractExempt} for
     * each NIC afterwards, then {@link #saveState}, all while holding this object's lock.
     */
    public synchronized void readExemptCounts() {
        if (!active && cidrs == null && billing.hasSavedState() && tableExists()) {
            // The agent restarted and the server hasn't sent the list yet. The table kept counting, so keep using it.
            // Reporting libvirt's totals now would jump above what we reported before, then drop once the list arrives.
            active = true;
        }
        exemptCounts = active ? sync() : null;
    }

    /**
     * Brings the nftables table in line with the exempt list and the taps on this host, and returns the counters as they
     * were before any change. Returns null when there's no table and we can't make one, and {@link #COUNTS_UNREADABLE} when the
     * table can't be read. Until the server sends a list, it only removes stale chains.
     */
    private Map<Integer, ExemptCount> sync() {
        final String listing = listTable();
        if (listing == null && (cidrs == null || !isEgressSupported())) {
            return null;
        }
        final Map<Integer, ExemptCount> exempt = new HashMap<>();
        final Map<String, String> chains = new HashMap<>();
        if (listing != null) {
            try {
                readTable(listing, chains, exempt);
            } catch (RuntimeException e) {
                logger.warn("Couldn't read the {} nftables table. Keeping the exempt counts from last time.", TABLE, e);
                return COUNTS_UNREADABLE;
            }
        }
        final Map<Integer, String> taps = keepUserVmTaps(listTaps());

        // Sets first, in their own transaction, so a tap that vanishes mid-sync can't hold up a change to the list.
        final StringBuilder sets = new StringBuilder();
        if (listing == null) {
            sets.append("add table netdev ").append(TABLE).append('\n');
            sets.append("add set netdev ").append(TABLE).append(" v4 { type ipv4_addr; flags interval; auto-merge; }\n");
            sets.append("add set netdev ").append(TABLE).append(" v6 { type ipv6_addr; flags interval; auto-merge; }\n");
            appliedCidrs = null;
        }
        if (cidrs != null && !cidrs.equals(appliedCidrs)) {
            appendSet(sets, "v4", false);
            appendSet(sets, "v6", true);
        }
        if (sets.length() > 0) {
            if (!applyRules(sets.toString())) {
                return exempt;
            }
            appliedCidrs = cidrs;
        }

        // Chains whose tap has gone. The kernel usually removes them with the tap, so this one can fail harmlessly.
        final StringBuilder stale = new StringBuilder();
        for (Map.Entry<String, String> chain : chains.entrySet()) {
            final Matcher name = CHAIN_NAME.matcher(chain.getKey());
            final int ifindex = name.matches() ? Integer.parseInt(name.group(2)) : -1;
            if (!chain.getValue().equals(taps.get(ifindex))) {
                stale.append("delete chain netdev ").append(TABLE).append(' ').append(chain.getKey()).append('\n');
                exempt.remove(ifindex);
            }
        }
        if (stale.length() > 0) {
            applyRules(stale.toString());
        }

        if (cidrs != null && !cidrs.isEmpty()) {
            final StringBuilder added = new StringBuilder();
            boolean keptCounts = false;
            for (Map.Entry<Integer, String> tap : taps.entrySet()) {
                final boolean in = appendChain(added, chains, "in", "ingress", "daddr", tap.getKey(), tap.getValue());
                final boolean out = appendChain(added, chains, "out", "egress", "saddr", tap.getKey(), tap.getValue());
                keptCounts |= billing.keepCountsFromLostChains(tap.getKey(), in, out);
            }
            if (keptCounts) {
                saveState();
            }
            if (added.length() > 0 && !applyRules(added.toString())) {
                logger.warn("Failed to add {} nftables chains for new NICs. Their exempt traffic is billed until the next sync works.", TABLE);
            }
        }
        return exempt;
    }

    /**
     * Keeps the taps that belong to user VMs. We ask libvirt once per new tap and remember the answer. A tap libvirt
     * doesn't know yet is asked about again on the next sync.
     */
    private Map<Integer, String> keepUserVmTaps(Map<Integer, String> taps) {
        if (tapOwners == null) {
            return taps;
        }
        isUserVmTap.keySet().retainAll(taps.keySet());
        if (!isUserVmTap.keySet().containsAll(taps.keySet())) {
            final Map<String, String> owners = tapOwners.get();
            if (owners != null) {
                for (Map.Entry<Integer, String> tap : taps.entrySet()) {
                    final String owner = owners.get(tap.getValue());
                    if (owner != null) {
                        isUserVmTap.putIfAbsent(tap.getKey(), owner.startsWith("i-"));
                    }
                }
            }
        }
        final Map<Integer, String> userTaps = new HashMap<>();
        for (Map.Entry<Integer, String> tap : taps.entrySet()) {
            if (Boolean.TRUE.equals(isUserVmTap.get(tap.getKey()))) {
                userTaps.put(tap.getKey(), tap.getValue());
            }
        }
        return userTaps;
    }

    /**
     * Checks once whether the kernel supports the netdev egress hook, with a dry run that changes nothing.
     */
    private boolean isEgressSupported() {
        if (egressSupported == null && !isNftInstalled()) {
            logger.warn("The nft command isn't installed (package nftables). " +
                    "network.usage.exempt.cidrs won't apply to VMs on this host, and their usage stays as libvirt reports it.");
            egressSupported = false;
        }
        if (egressSupported == null) {
            egressSupported = checkRules("table netdev " + TABLE + "_probe {\n" +
                    "  chain probe { type filter hook egress device \"lo\" priority 0; }\n}\n");
            if (!egressSupported) {
                logger.warn("This kernel doesn't support the nftables netdev egress hook (Linux 5.16 or later, or EL9). " +
                        "network.usage.exempt.cidrs won't apply to VMs on this host, and their usage stays as libvirt reports it.");
            }
        }
        return egressSupported;
    }

    private void appendSet(StringBuilder script, String set, boolean ipv6) {
        final List<String> family = new ArrayList<>();
        for (String cidr : cidrs) {
            if (cidr.contains(":") == ipv6) {
                family.add(cidr);
            }
        }
        script.append("flush set netdev ").append(TABLE).append(' ').append(set).append('\n');
        if (!family.isEmpty()) {
            script.append("add element netdev ").append(TABLE).append(' ').append(set)
                    .append(" { ").append(String.join(", ", family)).append(" }\n");
        }
    }

    /** Adds a chain for one tap and direction unless it's there already. Returns true if it adds one. */
    private boolean appendChain(StringBuilder script, Map<String, String> chains, String prefix, String hook, String match,
            int ifindex, String device) {
        final String chain = prefix + "_" + ifindex;
        if (chains.containsKey(chain)) {
            return false;
        }
        final String rule = "add rule netdev " + TABLE + " " + chain + " ";
        script.append("add chain netdev ").append(TABLE).append(' ').append(chain)
                .append(" { type filter hook ").append(hook).append(" device \"").append(device).append("\" priority 0; }\n");
        script.append(rule).append("ip ").append(match).append(" @v4 counter\n");
        script.append(rule).append("ip6 ").append(match).append(" @v6 counter\n");
        return true;
    }

    /** Fills in our chains (name to device) and the exempt counters per ifindex from an nft JSON listing. */
    private void readTable(String listing, Map<String, String> chains, Map<Integer, ExemptCount> exempt) {
        final JsonArray items = JsonParser.parseString(listing).getAsJsonObject().getAsJsonArray("nftables");
        for (JsonElement item : items) {
            final JsonObject object = item.getAsJsonObject();
            if (object.has("chain")) {
                final JsonObject chain = object.getAsJsonObject("chain");
                chains.put(chain.get("name").getAsString(), chain.has("dev") ? chain.get("dev").getAsString() : "");
            } else if (object.has("rule")) {
                final JsonObject rule = object.getAsJsonObject("rule");
                final Matcher name = CHAIN_NAME.matcher(rule.get("chain").getAsString());
                if (!name.matches()) {
                    continue;
                }
                final ExemptCount counted = exempt.computeIfAbsent(Integer.parseInt(name.group(2)), k -> new ExemptCount());
                for (JsonElement expr : rule.getAsJsonArray("expr")) {
                    final JsonObject counter = expr.getAsJsonObject().getAsJsonObject("counter");
                    if (counter == null) {
                        continue;
                    }
                    final long bytes = counter.get("bytes").getAsLong();
                    final long packets = counter.get("packets").getAsLong();
                    if ("in".equals(name.group(1))) {
                        counted.sent += bytes + packets * ETHERNET_HEADER_BYTES;
                    } else {
                        counted.received += bytes;
                    }
                }
            }
        }
    }

    /** The tap devices on this host, by ifindex. */
    private Map<Integer, String> listTaps() {
        final Map<Integer, String> taps = new HashMap<>();
        try (Stream<Path> devices = Files.list(netDir)) {
            devices.forEach(device -> {
                final String name = device.getFileName().toString();
                if (DEVICE_NAME.matcher(name).matches() && Files.exists(device.resolve("tun_flags"))) {
                    final Integer ifindex = readIfindex(name);
                    if (ifindex != null) {
                        taps.put(ifindex, name);
                    }
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list the network devices in {}", netDir, e);
        }
        return taps;
    }

    private Integer readIfindex(String device) {
        if (device == null || !DEVICE_NAME.matcher(device).matches()) {
            return null;
        }
        try {
            return Integer.valueOf(new String(Files.readAllBytes(netDir.resolve(device).resolve("ifindex")), StandardCharsets.UTF_8).trim());
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * Takes the exempt traffic off one NIC's libvirt totals. Returns the bytes to report as sent and received.
     * NICs without a kernel network device, such as vhost-user ports, are returned unchanged.
     */
    public synchronized long[] subtractExempt(String device, long sent, long received) {
        final Integer ifindex = readIfindex(device);
        if (exemptCounts == null || ifindex == null) {
            return new long[] {sent, received};
        }
        final ExemptCount exempt = exemptCounts == COUNTS_UNREADABLE ? null : exemptCounts.getOrDefault(ifindex, new ExemptCount());
        return billing.subtractExempt(device, ifindex, sent, received, exempt);
    }

    /** Writes the billing record of each tap to /run, dropping taps that no longer exist. */
    public synchronized void saveState() {
        billing.saveState((device, ifindex) -> ifindex.equals(readIfindex(device)));
    }

    protected boolean isNftInstalled() {
        final Script nft = new Script("nft", NFT_TIMEOUT_MS, logger);
        nft.add("--version");
        return nft.execute() == null;
    }

    /**
     * Checks for our table with a listing that succeeds either way. Script logs a warning for any failed command,
     * so we don't ask nft for a table that isn't there.
     */
    protected boolean tableExists() {
        final Script nft = new Script("nft", NFT_TIMEOUT_MS, logger);
        nft.add("list", "tables", "netdev");
        final OutputInterpreter.AllLinesParser output = new OutputInterpreter.AllLinesParser();
        return nft.execute(output) == null && output.getLines() != null
                && Pattern.compile("^table netdev " + TABLE + "$", Pattern.MULTILINE).matcher(output.getLines()).find();
    }

    /** Lists our table as JSON, or returns null if it doesn't exist. */
    protected String listTable() {
        if (!tableExists()) {
            return null;
        }
        final Script nft = new Script("nft", NFT_TIMEOUT_MS, logger);
        nft.add("-j", "list", "table", "netdev", TABLE);
        final OutputInterpreter.AllLinesParser output = new OutputInterpreter.AllLinesParser();
        return nft.execute(output) == null ? output.getLines() : null;
    }

    /** Applies nft rules in one transaction. */
    protected boolean applyRules(String rules) {
        return runRulesFile(rules, false);
    }

    /** Dry-runs nft rules. The kernel validates them and nothing changes. */
    protected boolean checkRules(String rules) {
        return runRulesFile(rules, true);
    }

    /**
     * Script closes a command's input as soon as it starts it, so the rules can't be piped into nft.
     * They're written to a temp file, and nft reads them from there.
     */
    private boolean runRulesFile(String rules, boolean checkOnly) {
        Path file = null;
        try {
            file = Files.createTempFile("cloudstack-usage-exempt", ".nft");
            Files.write(file, rules.getBytes(StandardCharsets.UTF_8));
            final Script nft = new Script("nft", NFT_TIMEOUT_MS, logger);
            if (checkOnly) {
                nft.add("-c");
            }
            nft.add("-f", file.toString());
            final String error = nft.execute();
            if (error != null && !checkOnly) {
                logger.warn("Failed to apply the {} nftables rules: {}", TABLE, error);
            }
            return error == null;
        } catch (IOException e) {
            logger.warn("Failed to write the {} nftables rules to a temp file", TABLE, e);
            return false;
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    logger.debug("Failed to delete {}", file, e);
                }
            }
        }
    }
}
