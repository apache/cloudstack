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
package org.apache.cloudstack.report;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.identity.InstallationIdentity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Documents the exact JSON payload {@link UsageReporter} POSTs to the telemetry
 * service, so any change to the wire format is a deliberate one.
 *
 * The report body is assembled by the private {@code get*Report()} methods and
 * serialized inside the private {@code sendReport()}. Rather than open up
 * production code, this test invokes those builders reflectively and applies the
 * same Gson configuration {@code sendReport()} uses.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UsageReporterTest {

    @Mock
    private HostDao hostDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private PrimaryDataStoreDao storagePoolDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private VersionDao versionDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;

    @InjectMocks
    private UsageReporter usageReporter = new UsageReporter();

    private static final String EXPECTED_PAYLOAD_RESOURCE = "usage-report-expected.json";

    private static final long FIVE_GB = 5368709120L;
    private static final long TEN_GB = 10737418240L;

    @Before
    public void setUp() {
        stubHosts();
        stubClusters();
        stubStoragePools();
        stubZones();
        stubInstances();
        stubDiskOfferings();
        stubVersions();
    }

    // ---------------------------------------------------------------- fixtures

    private void stubHosts() {
        HostVO routingKvm1 = Mockito.mock(HostVO.class);
        Mockito.when(routingKvm1.getType()).thenReturn(Host.Type.Routing);
        Mockito.when(routingKvm1.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(routingKvm1.getVersion()).thenReturn("4.23.0.0");

        HostVO routingKvm2 = Mockito.mock(HostVO.class);
        Mockito.when(routingKvm2.getType()).thenReturn(Host.Type.Routing);
        Mockito.when(routingKvm2.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(routingKvm2.getVersion()).thenReturn("4.23.0.0");

        // Secondary storage host: no version reported
        HostVO secondaryStorage = Mockito.mock(HostVO.class);
        Mockito.when(secondaryStorage.getType()).thenReturn(Host.Type.SecondaryStorage);
        Mockito.when(secondaryStorage.getHypervisorType()).thenReturn(HypervisorType.None);
        Mockito.when(secondaryStorage.getVersion()).thenReturn(null);

        // Host with no hypervisor type at all: must be skipped, not counted as null
        HostVO noHypervisor = Mockito.mock(HostVO.class);
        Mockito.when(noHypervisor.getType()).thenReturn(Host.Type.Routing);
        Mockito.when(noHypervisor.getHypervisorType()).thenReturn(null);
        Mockito.when(noHypervisor.getVersion()).thenReturn(null);

        Mockito.when(hostDao.search(Mockito.any(), Mockito.any()))
                .thenReturn(Arrays.asList(routingKvm1, routingKvm2, secondaryStorage, noHypervisor));
    }

    private void stubClusters() {
        ClusterVO kvmCluster = Mockito.mock(ClusterVO.class);
        Mockito.when(kvmCluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);
        Mockito.when(kvmCluster.getHypervisorType()).thenReturn(HypervisorType.KVM);

        ClusterVO vmwareCluster = Mockito.mock(ClusterVO.class);
        Mockito.when(vmwareCluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);
        Mockito.when(vmwareCluster.getHypervisorType()).thenReturn(HypervisorType.VMware);

        ClusterVO empty = Mockito.mock(ClusterVO.class);
        Mockito.when(empty.getClusterType()).thenReturn(null);
        Mockito.when(empty.getHypervisorType()).thenReturn(null);

        Mockito.when(clusterDao.search(Mockito.any(), Mockito.any()))
                .thenReturn(Arrays.asList(kvmCluster, vmwareCluster, empty));
    }

    private void stubStoragePools() {
        StoragePoolVO nfs = Mockito.mock(StoragePoolVO.class);
        Mockito.when(nfs.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(nfs.getStorageProviderName()).thenReturn("DefaultPrimary");
        Mockito.when(nfs.getScope()).thenReturn(ScopeType.ZONE);

        StoragePoolVO local = Mockito.mock(StoragePoolVO.class);
        Mockito.when(local.getPoolType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(local.getStorageProviderName()).thenReturn("DefaultPrimary");
        Mockito.when(local.getScope()).thenReturn(ScopeType.HOST);

        Mockito.when(storagePoolDao.listAll()).thenReturn(Arrays.asList(nfs, local));
    }

    private void stubZones() {
        DataCenterVO advanced = Mockito.mock(DataCenterVO.class);
        Mockito.when(advanced.getNetworkType()).thenReturn(NetworkType.Advanced);
        Mockito.when(advanced.getDnsProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getDhcpProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getLoadBalancerProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getFirewallProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getGatewayProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getUserDataProvider()).thenReturn("VirtualRouter");
        Mockito.when(advanced.getVpnProvider()).thenReturn("VirtualRouter");

        // Zone with no providers configured: only network_type is counted
        DataCenterVO basic = Mockito.mock(DataCenterVO.class);
        Mockito.when(basic.getNetworkType()).thenReturn(NetworkType.Basic);

        Mockito.when(dataCenterDao.listAllZones()).thenReturn(Arrays.asList(advanced, basic));
    }

    private void stubInstances() {
        VMInstanceVO runningUser = Mockito.mock(VMInstanceVO.class);
        Mockito.when(runningUser.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(runningUser.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(runningUser.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(runningUser.isHaEnabled()).thenReturn(true);
        Mockito.when(runningUser.isDynamicallyScalable()).thenReturn(true);

        VMInstanceVO stoppedUser = Mockito.mock(VMInstanceVO.class);
        Mockito.when(stoppedUser.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(stoppedUser.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(stoppedUser.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(stoppedUser.isHaEnabled()).thenReturn(false);
        Mockito.when(stoppedUser.isDynamicallyScalable()).thenReturn(true);

        VMInstanceVO router = Mockito.mock(VMInstanceVO.class);
        Mockito.when(router.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(router.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(router.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        Mockito.when(router.isHaEnabled()).thenReturn(true);
        Mockito.when(router.isDynamicallyScalable()).thenReturn(false);

        // Destroyed but not yet expunged: still a current Instance
        VMInstanceVO destroyedUser = Mockito.mock(VMInstanceVO.class);
        Mockito.when(destroyedUser.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(destroyedUser.getState()).thenReturn(VirtualMachine.State.Destroyed);
        Mockito.when(destroyedUser.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(destroyedUser.isHaEnabled()).thenReturn(false);
        Mockito.when(destroyedUser.isDynamicallyScalable()).thenReturn(false);

        // Removed row: only counted in the lifetime statistics
        VMInstanceVO expungedUser = Mockito.mock(VMInstanceVO.class);
        Mockito.when(expungedUser.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(expungedUser.getState()).thenReturn(VirtualMachine.State.Expunging);
        Mockito.when(expungedUser.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(expungedUser.getRemoved())
                .thenReturn(Date.from(Instant.parse("2025-06-01T12:00:00Z")));

        Mockito.when(vmInstanceDao.searchIncludingRemoved(Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Arrays.asList(runningUser, stoppedUser, router, destroyedUser, expungedUser));
    }

    private void stubDiskOfferings() {
        DiskOfferingVO thinShared = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(thinShared.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(thinShared.isComputeOnly()).thenReturn(false);
        Mockito.when(thinShared.isUseLocalStorage()).thenReturn(false);
        Mockito.when(thinShared.getDiskSize()).thenReturn(FIVE_GB);

        DiskOfferingVO fatLocal = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(fatLocal.getProvisioningType()).thenReturn(Storage.ProvisioningType.FAT);
        Mockito.when(fatLocal.isComputeOnly()).thenReturn(true);
        Mockito.when(fatLocal.isUseLocalStorage()).thenReturn(true);
        Mockito.when(fatLocal.getDiskSize()).thenReturn(TEN_GB);

        DiskOfferingVO customSize = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(customSize.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(customSize.isComputeOnly()).thenReturn(false);
        Mockito.when(customSize.isUseLocalStorage()).thenReturn(false);
        Mockito.when(customSize.getDiskSize()).thenReturn(0L);

        Mockito.when(diskOfferingDao.listAll()).thenReturn(Arrays.asList(thinShared, fatLocal, customSize));
    }

    private void stubVersions() {
        VersionVO older = Mockito.mock(VersionVO.class);
        Mockito.when(older.getVersion()).thenReturn("4.19.0.0");
        Mockito.when(older.getUpdated()).thenReturn(Date.from(Instant.parse("2024-01-15T10:30:00Z")));

        VersionVO current = Mockito.mock(VersionVO.class);
        Mockito.when(current.getVersion()).thenReturn("4.23.0.0");
        Mockito.when(current.getUpdated()).thenReturn(Date.from(Instant.parse("2026-08-27T08:00:00Z")));

        Mockito.when(versionDao.getAllVersions()).thenReturn(Arrays.asList(older, current));
        Mockito.when(versionDao.getCurrentVersion()).thenReturn("4.23.0.0");
    }

    // ----------------------------------------------------------------- helpers

    private Object buildSection(String method) throws Exception {
        Method m = UsageReporter.class.getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(usageReporter);
    }

    /**
     * Mirrors the report assembly in {@code UsageCollector.runInContext()}.
     */
    private Map<String, Object> buildReportMap() throws Exception {
        Map<String, Object> reportMap = new HashMap<String, Object>();
        reportMap.put("schema_version", UsageReporter.SCHEMA_VERSION);
        reportMap.put("hosts", buildSection("getHostReport"));
        reportMap.put("clusters", buildSection("getClusterReport"));
        reportMap.put("primaryStorage", buildSection("getStoragePoolReport"));
        reportMap.put("zones", buildSection("getDataCenterReport"));
        reportMap.put("instances", buildSection("getInstanceReport"));
        reportMap.put("diskOffering", buildSection("getDiskOfferingReport"));
        reportMap.put("versions", buildSection("getVersionReport"));
        reportMap.put("current_version", buildSection("getCurrentVersion"));
        return reportMap;
    }

    /**
     * Mirrors the Gson configuration in {@code UsageReporter.sendReport()}.
     */
    private static Gson reportGson(boolean pretty) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(AtomicLongMap.class, new AtomicGsonAdapter());
        if (pretty) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    private JsonObject reportJson() throws Exception {
        return JsonParser.parseString(reportGson(false).toJson(buildReportMap())).getAsJsonObject();
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = UsageReporterTest.class.getClassLoader().getResourceAsStream(name)) {
            Assert.assertNotNull("missing test resource: " + name, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Rebuilds an element with object keys in sorted order. AtomicLongMap is backed by
     * a ConcurrentHashMap and the report itself by a HashMap, so key order on the wire
     * is not deterministic and must not be part of the comparison. Sorting both sides
     * lets the payload be compared as pretty-printed text, which gives a readable
     * line-by-line diff when it does not match.
     */
    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject source = element.getAsJsonObject();
            JsonObject sorted = new JsonObject();
            for (String key : new TreeSet<>(source.keySet())) {
                sorted.add(key, canonicalize(source.get(key)));
            }
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray sorted = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                sorted.add(canonicalize(child));
            }
            return sorted;
        }
        return element;
    }

    private static String canonicalText(JsonElement element) {
        return reportGson(true).toJson(canonicalize(element));
    }

    private static void assertCount(JsonObject parent, String section, String key, long expected) {
        JsonObject bucket = parent.getAsJsonObject(section);
        Assert.assertTrue("expected key '" + key + "' in section '" + section + "', got: " + bucket,
                bucket.has(key));
        Assert.assertEquals("section '" + section + "', key '" + key + "'",
                expected, bucket.get(key).getAsLong());
    }

    // ------------------------------------------------------------------- tests

    /**
     * The reporting destination is a constant, not a Global Setting: telemetry has
     * to reach the Apache CloudStack project rather than wherever an operator points
     * it. Only the interval is configurable.
     */
    @Test
    public void testTelemetryEndpointIsStaticAndHttps() {
        Assert.assertEquals("https://call-home.cloudstack.org/report", UsageReporter.TELEMETRY_URI);
        Assert.assertTrue(UsageReporter.TELEMETRY_URI.startsWith("https://"));
        Assert.assertEquals(1, UsageReporter.SCHEMA_VERSION);

        ConfigKey<?>[] configKeys = usageReporter.getConfigKeys();
        Assert.assertEquals(1, configKeys.length);
        Assert.assertEquals("telemetry.interval", configKeys[0].key());
    }

    /**
     * The installation identity is derived from the version table rather than stored,
     * so every Management Server sharing the database reports under the same ID.
     */
    @Test
    public void testUniqueIdIsDerivedFromTheInitialVersionRow() {
        VersionVO initial = Mockito.mock(VersionVO.class);
        Mockito.when(initial.getVersion()).thenReturn("4.19.0.0");
        Mockito.when(initial.getUpdated()).thenReturn(Date.from(Instant.parse("2024-01-15T10:30:00Z")));
        Mockito.when(versionDao.getInitialVersion()).thenReturn(initial);

        String uniqueId = usageReporter.getUniqueId();

        Assert.assertEquals(InstallationIdentity.generate("4.19.0.0",
                Date.from(Instant.parse("2024-01-15T10:30:00Z"))), uniqueId);
        Assert.assertTrue(uniqueId, uniqueId.matches("[0-9a-f]{64}"));
    }

    @Test
    public void testUniqueIdIsNullOnAnEmptyVersionTable() {
        Mockito.when(versionDao.getInitialVersion()).thenReturn(null);

        Assert.assertNull(usageReporter.getUniqueId());
    }

    /**
     * The contract test: given the mocked environment set up in {@link #setUp()}, the
     * management server must produce exactly the payload in
     * {@code src/test/resources/usage-report-expected.json} -- no extra sections, no
     * missing counters, no renamed keys. If this fails, the wire format changed and
     * either the fixture or the change needs revisiting.
     */
    @Test
    public void testGeneratedPayloadMatchesExpectedJson() throws Exception {
        JsonElement expected = JsonParser.parseString(readResource(EXPECTED_PAYLOAD_RESOURCE));
        JsonElement actual = JsonParser.parseString(reportGson(false).toJson(buildReportMap()));

        Assert.assertEquals("generated usage report payload does not match "
                + EXPECTED_PAYLOAD_RESOURCE, canonicalText(expected), canonicalText(actual));
    }

    /**
     * Guards the fixture itself: a payload that differs anywhere must be rejected, so a
     * passing contract test above cannot be the result of a comparison that ignores
     * content.
     */
    @Test
    public void testExpectedJsonComparisonDetectsADifference() throws Exception {
        JsonObject tampered = JsonParser.parseString(readResource(EXPECTED_PAYLOAD_RESOURCE)).getAsJsonObject();
        tampered.getAsJsonObject("hosts").getAsJsonObject("type").addProperty("Routing", 99);

        JsonElement actual = JsonParser.parseString(reportGson(false).toJson(buildReportMap()));

        Assert.assertNotEquals(canonicalText(tampered), canonicalText(actual));
    }

    @Test
    public void testTopLevelPayloadKeys() throws Exception {
        JsonObject report = reportJson();

        Assert.assertEquals("unexpected set of top level keys in the usage report",
                new TreeSet<>(Arrays.asList("schema_version", "hosts", "clusters",
                        "primaryStorage", "zones", "instances", "diskOffering", "versions",
                        "current_version")),
                new TreeSet<>(report.keySet()));
    }

    @Test
    public void testHostsSection() throws Exception {
        JsonObject hosts = reportJson().getAsJsonObject("hosts");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("version", "hypervisor_type", "type")),
                new TreeSet<>(hosts.keySet()));

        assertCount(hosts, "type", "Routing", 3);
        assertCount(hosts, "type", "SecondaryStorage", 1);
        assertCount(hosts, "hypervisor_type", "KVM", 2);
        assertCount(hosts, "hypervisor_type", "None", 1);
        assertCount(hosts, "version", "4.23.0.0", 2);

        // A null hypervisor type or version is skipped entirely, never emitted as a "null" key
        Assert.assertFalse(hosts.getAsJsonObject("hypervisor_type").has("null"));
        Assert.assertEquals(1, hosts.getAsJsonObject("version").size());
    }

    @Test
    public void testClustersSection() throws Exception {
        JsonObject clusters = reportJson().getAsJsonObject("clusters");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("hypervisor_type", "type")),
                new TreeSet<>(clusters.keySet()));

        assertCount(clusters, "type", "CloudManaged", 2);
        assertCount(clusters, "hypervisor_type", "KVM", 1);
        assertCount(clusters, "hypervisor_type", "VMware", 1);
    }

    @Test
    public void testPrimaryStorageSection() throws Exception {
        JsonObject storage = reportJson().getAsJsonObject("primaryStorage");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("type", "provider", "scope")),
                new TreeSet<>(storage.keySet()));

        assertCount(storage, "type", "NetworkFilesystem", 1);
        assertCount(storage, "type", "Filesystem", 1);
        assertCount(storage, "provider", "DefaultPrimary", 2);
        assertCount(storage, "scope", "ZONE", 1);
        assertCount(storage, "scope", "HOST", 1);
    }

    @Test
    public void testZonesSection() throws Exception {
        JsonObject zones = reportJson().getAsJsonObject("zones");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("network_type", "dns_provider",
                        "dhcp_provider", "lb_provider", "firewall_provider", "gateway_provider",
                        "userdata_provider", "vpn_provider")),
                new TreeSet<>(zones.keySet()));

        assertCount(zones, "network_type", "Advanced", 1);
        assertCount(zones, "network_type", "Basic", 1);
        for (String provider : Arrays.asList("dns_provider", "dhcp_provider", "lb_provider",
                "firewall_provider", "gateway_provider", "userdata_provider", "vpn_provider")) {
            assertCount(zones, provider, "VirtualRouter", 1);
        }
    }

    /**
     * "current" covers the non-removed rows in any state -- Destroyed Instances
     * that have not been expunged yet included -- while "lifetime" also counts
     * the removed rows and so describes every Instance which ever existed.
     */
    @Test
    public void testInstancesSection() throws Exception {
        JsonObject instances = reportJson().getAsJsonObject("instances");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("current", "lifetime")),
                new TreeSet<>(instances.keySet()));

        JsonObject current = instances.getAsJsonObject("current");
        Assert.assertEquals(new TreeSet<>(Arrays.asList("hypervisor_type", "state", "type",
                        "ha_enabled", "dynamically_scalable")),
                new TreeSet<>(current.keySet()));

        assertCount(current, "hypervisor_type", "KVM", 4);
        assertCount(current, "state", "Running", 2);
        assertCount(current, "state", "Stopped", 1);
        assertCount(current, "state", "Destroyed", 1);
        assertCount(current, "type", "User", 3);
        assertCount(current, "type", "DomainRouter", 1);

        JsonObject lifetime = instances.getAsJsonObject("lifetime");
        Assert.assertEquals(new TreeSet<>(Arrays.asList("total", "removed", "hypervisor_type",
                        "type")),
                new TreeSet<>(lifetime.keySet()));

        Assert.assertEquals(5, lifetime.get("total").getAsLong());
        Assert.assertEquals(1, lifetime.get("removed").getAsLong());
        assertCount(lifetime, "hypervisor_type", "KVM", 5);
        assertCount(lifetime, "type", "User", 4);
        assertCount(lifetime, "type", "DomainRouter", 1);

        // The state of a removed row is meaningless, so lifetime has no state counter
        Assert.assertFalse(lifetime.has("state"));
    }

    /**
     * Booleans are used directly as AtomicLongMap keys, so they reach the wire as
     * the JSON object keys "true" and "false" rather than as booleans.
     */
    @Test
    public void testBooleanCountersBecomeTrueFalseStringKeys() throws Exception {
        JsonObject current = reportJson().getAsJsonObject("instances").getAsJsonObject("current");

        assertCount(current, "ha_enabled", "true", 2);
        assertCount(current, "ha_enabled", "false", 2);
        assertCount(current, "dynamically_scalable", "true", 2);
        assertCount(current, "dynamically_scalable", "false", 2);
    }

    @Test
    public void testDiskOfferingSection() throws Exception {
        JsonObject diskOffering = reportJson().getAsJsonObject("diskOffering");

        Assert.assertEquals(new TreeSet<>(Arrays.asList("compute_only", "provisioning_type",
                        "use_local_storage", "avg_disk_size")),
                new TreeSet<>(diskOffering.keySet()));

        assertCount(diskOffering, "compute_only", "false", 2);
        assertCount(diskOffering, "compute_only", "true", 1);
        assertCount(diskOffering, "use_local_storage", "false", 2);
        assertCount(diskOffering, "use_local_storage", "true", 1);

        // avg_disk_size is a plain number, not a counter map
        Assert.assertEquals((FIVE_GB + TEN_GB) / 3, diskOffering.get("avg_disk_size").getAsLong());
    }

    /**
     * Storage.ProvisioningType overrides toString() to return a lowercase name, and
     * AtomicGsonAdapter keys on String.valueOf(key). The wire format is therefore
     * "thin"/"fat", not the enum constant names THIN/FAT that Gson would emit by default.
     */
    @Test
    public void testProvisioningTypeKeysAreLowercase() throws Exception {
        JsonObject provisioningType = reportJson()
                .getAsJsonObject("diskOffering").getAsJsonObject("provisioning_type");

        assertCount(reportJson().getAsJsonObject("diskOffering"), "provisioning_type", "thin", 2);
        assertCount(reportJson().getAsJsonObject("diskOffering"), "provisioning_type", "fat", 1);
        Assert.assertFalse("enum constant name leaked into the payload",
                provisioningType.has("THIN"));
    }

    @Test
    public void testVersionsSectionUsesUtcIso8601() throws Exception {
        JsonObject report = reportJson();
        JsonObject versions = report.getAsJsonObject("versions");

        Assert.assertEquals("2024-01-15T10:30:00Z", versions.get("4.19.0.0").getAsString());
        Assert.assertEquals("2026-08-27T08:00:00Z", versions.get("4.23.0.0").getAsString());
        Assert.assertEquals("4.23.0.0", report.get("current_version").getAsString());
    }

    /**
     * A brand new install reports empty counter objects rather than nulls or
     * missing sections, and avg_disk_size falls back to 0 instead of dividing by zero.
     */
    @Test
    public void testEmptyEnvironmentStillProducesCompletePayload() throws Exception {
        Mockito.when(hostDao.search(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());
        Mockito.when(clusterDao.search(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());
        Mockito.when(vmInstanceDao.searchIncludingRemoved(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyBoolean())).thenReturn(Collections.emptyList());
        Mockito.when(storagePoolDao.listAll()).thenReturn(Collections.emptyList());
        Mockito.when(dataCenterDao.listAllZones()).thenReturn(Collections.emptyList());
        Mockito.when(diskOfferingDao.listAll()).thenReturn(Collections.emptyList());
        Mockito.when(versionDao.getAllVersions()).thenReturn(Collections.emptyList());

        JsonObject report = reportJson();

        Assert.assertEquals(9, report.keySet().size());
        Assert.assertEquals(0, report.getAsJsonObject("hosts").getAsJsonObject("type").size());
        Assert.assertEquals(0, report.getAsJsonObject("instances")
                .getAsJsonObject("current").getAsJsonObject("state").size());
        Assert.assertEquals(0, report.getAsJsonObject("instances")
                .getAsJsonObject("lifetime").get("total").getAsLong());
        Assert.assertEquals(0, report.getAsJsonObject("versions").size());
        Assert.assertEquals(0, report.getAsJsonObject("diskOffering").get("avg_disk_size").getAsLong());
    }

    /**
     * Not an assertion so much as documentation: prints the payload the management
     * server would POST to the telemetry endpoint, so the shape can be eyeballed and
     * handed to whoever implements the receiving end.
     */
    @Test
    public void testPrintExamplePayload() throws Exception {
        String pretty = reportGson(true).toJson(buildReportMap());
        System.out.println("---8<--- usage report payload POSTed to "
                + UsageReporter.TELEMETRY_URI + "/<uniqueID> ---8<---");
        System.out.println(pretty);
        System.out.println("---8<--- end of usage report payload ---8<---");

        Assert.assertNotNull(pretty);
        Assert.assertTrue(pretty.startsWith("{"));
    }
}
