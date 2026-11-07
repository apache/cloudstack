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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UsageExemptCountersTest {

    /** Stands in for nft: keeps the chains and counters we set, and records each script it's given. */
    static class FakeNftCounters extends UsageExemptCounters {
        boolean tableExists;
        boolean failApply;
        String failIfContains;
        String listingOverride;
        boolean egressSupported = true;
        boolean nftInstalled = true;
        int checks;
        final Map<String, String> chains = new LinkedHashMap<>();
        final Map<String, long[]> counters = new LinkedHashMap<>();
        final List<String> scripts = new ArrayList<>();

        FakeNftCounters(Path netDir, Path stateFile) {
            super(netDir, stateFile);
        }

        @Override
        protected ScheduledExecutorService startPeriodicSync() {
            return null;
        }

        void count(String chain, long packets, long bytes) {
            counters.put(chain, new long[] {packets, bytes});
        }

        @Override
        protected boolean tableExists() {
            return tableExists;
        }

        @Override
        protected String listTable() {
            if (!tableExists) {
                return null;
            }
            if (listingOverride != null) {
                return listingOverride;
            }
            StringBuilder json = new StringBuilder("{\"nftables\": [{\"metainfo\": {}}, {\"table\": {\"family\": \"netdev\", \"name\": \"t\"}}");
            for (Map.Entry<String, String> chain : chains.entrySet()) {
                json.append(", {\"chain\": {\"name\": \"").append(chain.getKey()).append("\", \"dev\": \"").append(chain.getValue()).append("\"}}");
                long[] counter = counters.getOrDefault(chain.getKey(), new long[] {0, 0});
                json.append(", {\"rule\": {\"chain\": \"").append(chain.getKey()).append("\", \"expr\": [{\"match\": {}}, {\"counter\": {\"packets\": ")
                        .append(counter[0]).append(", \"bytes\": ").append(counter[1]).append("}}]}}");
                json.append(", {\"rule\": {\"chain\": \"").append(chain.getKey()).append("\", \"expr\": [{\"match\": {}}, {\"counter\": {\"packets\": 0, \"bytes\": 0}}]}}");
            }
            return json.append("]}").toString();
        }

        @Override
        protected boolean applyRules(String script) {
            scripts.add(script);
            if (failApply || (failIfContains != null && script.contains(failIfContains))) {
                return false;
            }
            tableExists = true;
            for (String line : script.split("\n")) {
                String[] words = line.split(" ");
                if (line.startsWith("add chain")) {
                    chains.put(words[4], line.replaceAll(".*device \"([^\"]+)\".*", "$1"));
                } else if (line.startsWith("delete chain")) {
                    chains.remove(words[4]);
                    counters.remove(words[4]);
                }
            }
            return true;
        }

        @Override
        protected boolean isNftInstalled() {
            return nftInstalled;
        }

        @Override
        protected boolean checkRules(String rules) {
            checks++;
            return egressSupported;
        }

        String lastScript() {
            return scripts.get(scripts.size() - 1);
        }

        long[] collect(String device, long sent, long received) {
            readExemptCounts();
            long[] usage = subtractExempt(device, sent, received);
            saveState();
            return usage;
        }
    }

    private Path dir;
    private Path netDir;
    private FakeNftCounters counters;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("usage-exempt");
        netDir = dir.resolve("net");
        counters = new FakeNftCounters(netDir, dir.resolve("run/state.json"));
        addTap("vnet1", 51);
    }

    @After
    public void tearDown() throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Collections.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    private void addTap(String name, int ifindex) throws IOException {
        Path device = netDir.resolve(name);
        Files.createDirectories(device);
        Files.write(device.resolve("ifindex"), (ifindex + "\n").getBytes(StandardCharsets.UTF_8));
        Files.write(device.resolve("tun_flags"), "0x1002\n".getBytes(StandardCharsets.UTF_8));
    }

    private void removeTap(String name) throws IOException {
        Path device = netDir.resolve(name);
        Files.delete(device.resolve("ifindex"));
        Files.delete(device.resolve("tun_flags"));
        Files.delete(device);
    }

    @Test
    public void reportsLibvirtTotalsWhenNeverConfigured() {
        Assert.assertArrayEquals(new long[] {1000, 2000}, counters.collect("vnet1", 1000, 2000));
        Assert.assertTrue(counters.scripts.isEmpty());
    }

    @Test
    public void setCidrsCreatesTheTableSetsAndChains() {
        counters.setCidrs(Arrays.asList("192.0.2.5/32", "2001:db8::/64", "198.51.100.0/24"));
        String script = String.join("", counters.scripts);
        Assert.assertTrue(script.contains("add table netdev cloudstack_usage_exempt\n"));
        Assert.assertTrue(script.contains("add element netdev cloudstack_usage_exempt v4 { 192.0.2.5/32, 198.51.100.0/24 }\n"));
        Assert.assertTrue(script.contains("add element netdev cloudstack_usage_exempt v6 { 2001:db8::/64 }\n"));
        Assert.assertTrue(script.contains("add chain netdev cloudstack_usage_exempt in_51 { type filter hook ingress device \"vnet1\" priority 0; }\n"));
        Assert.assertTrue(script.contains("add rule netdev cloudstack_usage_exempt in_51 ip daddr @v4 counter\n"));
        Assert.assertTrue(script.contains("add rule netdev cloudstack_usage_exempt out_51 ip6 saddr @v6 counter\n"));
    }

    @Test
    public void unchangedListIsNotReapplied() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        int applied = counters.scripts.size();
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertEquals(applied, counters.scripts.size());
        counters.setCidrs(Collections.singletonList("192.0.2.6/32"));
        Assert.assertEquals(applied + 1, counters.scripts.size());
        Assert.assertTrue(counters.lastScript().contains("add element"));
        Assert.assertFalse(counters.lastScript().contains("add chain"));
    }

    @Test
    public void subtractsExemptTrafficAndAddsTheEthernetHeaderToWhatTheVmSent() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 10, 1000);
        counters.count("out_51", 5, 700);
        Assert.assertArrayEquals(new long[] {5000 - 1000 - 10 * 14, 8000 - 700}, counters.collect("vnet1", 5000, 8000));
    }

    @Test
    public void carriesExemptBytesWhenTheChainsAreRecreated() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("out_51", 0, 3000);
        Assert.assertEquals(7000, counters.collect("vnet1", 0, 10000)[1]);
        // someone flushes nftables and the sync puts the table back with fresh counters
        counters.tableExists = false;
        counters.chains.clear();
        counters.counters.clear();
        counters.count("out_51", 0, 500);
        counters.tableExists = true;
        counters.chains.put("in_51", "vnet1");
        counters.chains.put("out_51", "vnet1");
        Assert.assertEquals(12000 - 3000 - 500, counters.collect("vnet1", 0, 12000)[1]);
    }

    @Test
    public void newTapWithAnOldNameStartsAfresh() throws IOException {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(9000, counters.collect("vnet1", 10000, 0)[0]);
        removeTap("vnet1");
        addTap("vnet1", 77);
        counters.readExemptCounts();
        Assert.assertFalse(counters.chains.containsKey("in_51"));
        Assert.assertEquals("vnet1", counters.chains.get("in_77"));
        Assert.assertEquals(300, counters.collect("vnet1", 300, 0)[0]);
    }

    @Test
    public void stateSurvivesAnAgentRestart() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(4000, counters.collect("vnet1", 5000, 0)[0]);

        FakeNftCounters restarted = new FakeNftCounters(netDir, dir.resolve("run/state.json"));
        restarted.tableExists = true;
        restarted.chains.putAll(counters.chains);
        restarted.count("in_51", 0, 2000);
        // the agent reconnects and gets the same list; the table kept counting while it was down
        restarted.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertFalse(restarted.lastScript().contains("add chain"));
        Assert.assertEquals(4000, restarted.collect("vnet1", 5500, 0)[0]);
    }

    @Test
    public void clearingTheListKeepsWhatWasAlreadyExempt() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(4000, counters.collect("vnet1", 5000, 0)[0]);
        counters.setCidrs(Collections.emptyList());
        Assert.assertTrue(counters.lastScript().contains("flush set netdev cloudstack_usage_exempt v4\n"));
        Assert.assertFalse(counters.lastScript().contains("add element"));
        Assert.assertEquals(5000, counters.collect("vnet1", 6000, 0)[0]);
    }

    @Test
    public void missingNftReportsLibvirtTotals() {
        counters.nftInstalled = false;
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertTrue(counters.scripts.isEmpty());
        Assert.assertEquals("skips the kernel check", 0, counters.checks);
        Assert.assertArrayEquals(new long[] {1000, 2000}, counters.collect("vnet1", 1000, 2000));
    }

    @Test
    public void emptyListWithNoTableDoesNothing() {
        counters.setCidrs(Collections.emptyList());
        Assert.assertArrayEquals(new long[] {1000, 2000}, counters.collect("vnet1", 1000, 2000));
        Assert.assertTrue(counters.scripts.isEmpty());
        Assert.assertEquals(0, counters.checks);
    }

    @Test
    public void nicWithoutAKernelDeviceIsReportedUnchanged() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertArrayEquals(new long[] {1000, 2000}, counters.collect("vhost-user-port", 1000, 2000));
    }

    @Test
    public void failedApplyIsRetriedOnTheNextSync() {
        counters.failApply = true;
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.failApply = false;
        int failed = counters.scripts.size();
        counters.readExemptCounts();
        Assert.assertEquals("vnet1", counters.chains.get("in_51"));
        Assert.assertTrue(String.join("", counters.scripts.subList(failed, counters.scripts.size())).contains("add element"));
    }

    @Test
    public void kernelWithoutTheEgressHookReportsLibvirtTotals() {
        counters.egressSupported = false;
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertTrue(counters.scripts.isEmpty());
        Assert.assertArrayEquals(new long[] {1000, 2000}, counters.collect("vnet1", 1000, 2000));
        Assert.assertEquals("checks the kernel once", 1, counters.checks);
    }

    @Test
    public void restartBeforeTheListArrivesKeepsUsingTheTable() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(4000, counters.collect("vnet1", 5000, 0)[0]);

        FakeNftCounters restarted = new FakeNftCounters(netDir, dir.resolve("run/state.json"));
        restarted.tableExists = true;
        restarted.chains.putAll(counters.chains);
        restarted.count("in_51", 0, 2000);
        // a stats request arrives before the server has sent the list
        Assert.assertEquals(4000, restarted.collect("vnet1", 5500, 0)[0]);
        Assert.assertEquals(4500, restarted.collect("vnet1", 6500, 0)[0]);
        Assert.assertTrue(restarted.scripts.isEmpty());
    }

    @Test
    public void flushRepairedBetweenStatsCollectionsKeepsTheCarry() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(4000, counters.collect("vnet1", 5000, 0)[0]);
        // someone flushes nftables, and the background sync puts the table back
        counters.tableExists = false;
        counters.chains.clear();
        counters.counters.clear();
        counters.readExemptCounts();
        Assert.assertEquals("vnet1", counters.chains.get("in_51"));
        // by the next stats collection the new chain has counted past the old one
        counters.count("in_51", 0, 1500);
        Assert.assertEquals(7000 - 1000 - 1500, counters.collect("vnet1", 7000, 0)[0]);
    }

    @Test
    public void unreadableTableKeepsTheExemptCountsFromLastTime() {
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        counters.count("in_51", 0, 1000);
        Assert.assertEquals(4000, counters.collect("vnet1", 5000, 0)[0]);
        counters.listingOverride = "{\"nftables\": [{\"rule\": {\"chain\": \"in_51\", \"expr\": \"surprise\"}}]}";
        Assert.assertEquals(5000, counters.collect("vnet1", 6000, 0)[0]);
    }

    @Test
    public void systemVmTapsGetNoCounters() throws IOException {
        addTap("vnet2", 52);
        int[] lookups = {0};
        Map<String, String> owners = new LinkedHashMap<>();
        owners.put("vnet1", "i-2-10-VM");
        owners.put("vnet2", "r-11-VM");
        counters.setTapOwners(() -> {
            lookups[0]++;
            return owners;
        });
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertEquals("vnet1", counters.chains.get("in_51"));
        Assert.assertFalse(counters.chains.containsKey("in_52"));
        counters.readExemptCounts();
        Assert.assertEquals("asks libvirt once per new tap", 1, lookups[0]);
    }

    @Test
    public void listChangesEvenWhenATapVanishesMidSync() {
        counters.failIfContains = "add chain";
        counters.setCidrs(Collections.singletonList("192.0.2.5/32"));
        Assert.assertTrue(counters.scripts.get(0).contains("add element netdev cloudstack_usage_exempt v4 { 192.0.2.5/32 }"));
        Assert.assertTrue(counters.chains.isEmpty());
        counters.failIfContains = null;
        counters.readExemptCounts();
        Assert.assertEquals("vnet1", counters.chains.get("in_51"));
        Assert.assertFalse("the list isn't applied again", counters.lastScript().contains("add element"));
    }
}
