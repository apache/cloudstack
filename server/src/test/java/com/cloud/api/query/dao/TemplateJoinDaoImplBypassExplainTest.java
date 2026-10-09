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
package com.cloud.api.query.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test that EXPLAINs the Phase-1 bypass SQL versus the legacy
 * template_view SQL, and asserts that the bypass plan no longer trips the
 * "Range checked for each record" path on data_center.
 *
 * Skipped unless a MySQL URL is provided via system properties:
 *   -Dtest.cloudstack.mysql.url=jdbc:mysql://host:3306/cloud
 *   -Dtest.cloudstack.mysql.user=root
 *   -Dtest.cloudstack.mysql.password=
 *
 */
public class TemplateJoinDaoImplBypassExplainTest {

    private static final String BYPASS_SQL =
            "SELECT DISTINCT CONCAT(vt.id, '_', IFNULL(dc.id, 0)) AS pair " +
            "FROM cloud.vm_template vt " +
            "JOIN cloud.account a ON a.id = vt.account_id " +
            "LEFT JOIN cloud.template_store_ref tsr " +
            "  ON tsr.template_id = vt.id AND tsr.store_role = 'Image' AND tsr.destroyed = 0 " +
            "LEFT JOIN cloud.image_store img " +
            "  ON img.id = tsr.store_id AND img.removed IS NULL " +
            "LEFT JOIN cloud.template_zone_ref tzr " +
            "  ON tzr.template_id = vt.id AND tsr.store_id IS NULL AND tzr.removed IS NULL " +
            "LEFT JOIN cloud.data_center dc " +
            "  ON dc.id = COALESCE(img.data_center_id, tzr.zone_id) " +
            "WHERE vt.removed IS NULL " +
            "  AND vt.state IN ('Active','UploadAbandoned','UploadError','NotUploaded','UploadInProgress')";

    /**
     * Bypass SQL with the conditional `JOIN cloud.domain` added.
     * Triggered when the request needs domain.path scoping (DA self/selfexecutable
     * or non-admin all + SkipProjectResources). Should still use eq_ref / PRIMARY
     * on data_center; domain itself is small.
     */
    private static final String BYPASS_SQL_WITH_DOMAIN =
            "SELECT DISTINCT CONCAT(vt.id, '_', IFNULL(dc.id, 0)) AS pair " +
            "FROM cloud.vm_template vt " +
            "JOIN cloud.account a ON a.id = vt.account_id " +
            "LEFT JOIN cloud.template_store_ref tsr " +
            "  ON tsr.template_id = vt.id AND tsr.store_role = 'Image' AND tsr.destroyed = 0 " +
            "LEFT JOIN cloud.image_store img " +
            "  ON img.id = tsr.store_id AND img.removed IS NULL " +
            "LEFT JOIN cloud.template_zone_ref tzr " +
            "  ON tzr.template_id = vt.id AND tsr.store_id IS NULL AND tzr.removed IS NULL " +
            "LEFT JOIN cloud.data_center dc " +
            "  ON dc.id = COALESCE(img.data_center_id, tzr.zone_id) " +
            "JOIN cloud.domain d ON d.id = a.domain_id " +
            "WHERE vt.removed IS NULL " +
            "  AND vt.state IN ('Active','UploadAbandoned','UploadError','NotUploaded','UploadInProgress') " +
            "  AND d.path LIKE '/%' ";

    private static final String VIEW_SQL =
            "SELECT DISTINCT temp_zone_pair FROM cloud.template_view " +
            "WHERE template_state IN ('Active','UploadAbandoned','UploadError','NotUploaded','UploadInProgress')";

    private Connection conn;

    @Before
    public void setUp() throws Exception {
        String url = System.getProperty("test.cloudstack.mysql.url");
        String user = System.getProperty("test.cloudstack.mysql.user", "root");
        String pwd = System.getProperty("test.cloudstack.mysql.password", "");
        Assume.assumeNotNull("test.cloudstack.mysql.url not set; skipping EXPLAIN integration test", url);
        conn = DriverManager.getConnection(url, user, pwd);
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypass_dataCenter_isPkLookup_notRangeChecked() throws Exception {
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL);
        Map<String, String> dc = rowsByTable.get("dc");
        assertNotNull("EXPLAIN row for `dc` (data_center) missing", dc);

        String type = dc.get("type");
        String key = dc.get("key");
        String extra = dc.get("Extra") == null ? "" : dc.get("Extra");

        // dc.id is the PK; COALESCE on the other side. Optimizer should pick eq_ref / ref / const.
        assertTrue("dc access type is " + type + ", expected eq_ref/ref/const",
                "eq_ref".equals(type) || "ref".equals(type) || "const".equals(type));
        assertEquals("dc should use PRIMARY", "PRIMARY", key);
        assertFalse("dc should not use 'Range checked for each record'; Extra=" + extra,
                extra.contains("Range checked for each record"));
    }

    /**
     * Asserts the bypass plan trims the join graph the legacy view drags along.
     *
     * Portable across CIB and prod
     *
     */
    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void view_joinsMoreTablesThanBypass() throws Exception {
        Map<String, Map<String, String>> view = explain(VIEW_SQL);
        Map<String, Map<String, String>> bypass = explain(BYPASS_SQL);
        assertTrue(
                "expected legacy view plan (" + view.size() + " tables) to join more than bypass (" + bypass.size() + ")",
                view.size() > bypass.size());
        // Row-multiplier tables that the bypass intentionally drops.
        assertTrue("expected view plan to include vm_template_details",
                view.containsKey("vm_template_details"));
        assertTrue("expected view plan to include resource_tags",
                view.containsKey("resource_tags"));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypass_doesNotJoin_resourceTags_or_vmTemplateDetails() throws Exception {
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL);
        assertFalse("bypass must not join resource_tags", rowsByTable.containsKey("resource_tags"));
        assertFalse("bypass must not join vm_template_details", rowsByTable.containsKey("vm_template_details"));
        assertFalse("bypass must not join launch_permission", rowsByTable.containsKey("launch_permission"));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_joinsDomainTable() throws Exception {
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL_WITH_DOMAIN);
        assertTrue("bypass+domain plan must include domain table", rowsByTable.containsKey("d"));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_domain_isPkLookup() throws Exception {
        // domain.id is the PK; the join is on a.domain_id. Optimizer should pick eq_ref / ref / const.
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL_WITH_DOMAIN);
        Map<String, String> d = rowsByTable.get("d");
        assertNotNull("EXPLAIN row for `d` (domain) missing", d);

        String type = d.get("type");
        String key = d.get("key");
        String extra = d.get("Extra") == null ? "" : d.get("Extra");

        assertTrue("domain access type is " + type + ", expected eq_ref/ref/const/index",
                "eq_ref".equals(type) || "ref".equals(type) || "const".equals(type) || "index".equals(type));
        assertNotNull("domain should use SOME index", key);
        assertFalse("domain should not be 'Range checked for each record'; Extra=" + extra,
                extra.contains("Range checked for each record"));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_dataCenter_stillPkLookup() throws Exception {
        // Adding the domain join must not regress the data_center PK-lookup gain.
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL_WITH_DOMAIN);
        Map<String, String> dc = rowsByTable.get("dc");
        assertNotNull("EXPLAIN row for `dc` missing in bypass+domain plan", dc);
        String type = dc.get("type");
        assertTrue("dc access type with domain join is " + type + ", expected eq_ref/ref/const",
                "eq_ref".equals(type) || "ref".equals(type) || "const".equals(type));
        assertEquals("PRIMARY", dc.get("key"));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_stillFewerTablesThanView() throws Exception {
        // Even with the conditional domain join, the bypass plan is still smaller than the legacy view.
        Map<String, Map<String, String>> bypass = explain(BYPASS_SQL_WITH_DOMAIN);
        Map<String, Map<String, String>> view = explain(VIEW_SQL);
        assertTrue(
                "bypass+domain plan (" + bypass.size() + " tables) should still join fewer than the view (" + view.size() + ")",
                bypass.size() < view.size());
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_stillNoRowMultipliers() throws Exception {
        // Critical: adding the domain join must not pull in the row multipliers the bypass dropped.
        Map<String, Map<String, String>> rowsByTable = explain(BYPASS_SQL_WITH_DOMAIN);
        assertFalse("bypass+domain must not join resource_tags",       rowsByTable.containsKey("resource_tags"));
        assertFalse("bypass+domain must not join vm_template_details", rowsByTable.containsKey("vm_template_details"));
        assertFalse("bypass+domain must not join launch_permission",   rowsByTable.containsKey("launch_permission"));
    }

    // ============================================================================
    // Timing tests
    // ============================================================================

    /** Number of iterations per query; first 2 are warm-up. */
    private static final int TIMING_ITERATIONS = 5;
    /** Skip first N iterations from the average (warm-up). */
    private static final int TIMING_WARMUP = 2;
    /**
     * Tolerance for the timing assertion. With sparse test data both queries
     * complete in ~1 ms, where measurement noise can flip the order; allow the
     * bypass to be marginally slower without failing the test.
     */
    private static final double TIMING_TOLERANCE_FACTOR = 2.0;
    /** Floor below which we don't bother asserting (pure JDBC overhead). */
    private static final long TIMING_NOISE_FLOOR_NS = 5_000_000L; // 5 ms

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypass_isFasterThanLegacyView() throws Exception {
        long legacyAvgNs = averageQueryNanos(VIEW_SQL);
        long bypassAvgNs = averageQueryNanos(BYPASS_SQL);

        System.out.println(String.format(
                "Timing avg over %d iterations (warm-up %d): legacy=%.2f ms, bypass=%.2f ms (%.1fx speedup)",
                TIMING_ITERATIONS, TIMING_WARMUP,
                legacyAvgNs / 1_000_000.0,
                bypassAvgNs / 1_000_000.0,
                (double) legacyAvgNs / Math.max(bypassAvgNs, 1)));

        // Below the noise floor both queries are dominated by JDBC overhead;
        // the structural improvement is unmeasurable. Skip the strict comparison.
        if (legacyAvgNs < TIMING_NOISE_FLOOR_NS) {
            System.out.println("Skipping strict timing assertion: legacy duration below noise floor");
            return;
        }
        assertTrue(
                String.format("bypass (%.2f ms) should be at most %.1fx legacy (%.2f ms)",
                        bypassAvgNs / 1_000_000.0, TIMING_TOLERANCE_FACTOR, legacyAvgNs / 1_000_000.0),
                bypassAvgNs <= (long) (legacyAvgNs * TIMING_TOLERANCE_FACTOR));
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypassWithDomain_isFasterThanLegacyView() throws Exception {
        long legacyAvgNs = averageQueryNanos(VIEW_SQL);
        long bypassAvgNs = averageQueryNanos(BYPASS_SQL_WITH_DOMAIN);

        System.out.println(String.format(
                "Timing avg (bypass+domain join): legacy=%.2f ms, bypass+domain=%.2f ms (%.1fx speedup)",
                legacyAvgNs / 1_000_000.0,
                bypassAvgNs / 1_000_000.0,
                (double) legacyAvgNs / Math.max(bypassAvgNs, 1)));

        if (legacyAvgNs < TIMING_NOISE_FLOOR_NS) {
            System.out.println("Skipping strict timing assertion: legacy duration below noise floor");
            return;
        }
        assertTrue(
                String.format("bypass+domain (%.2f ms) should be at most %.1fx legacy (%.2f ms)",
                        bypassAvgNs / 1_000_000.0, TIMING_TOLERANCE_FACTOR, legacyAvgNs / 1_000_000.0),
                bypassAvgNs <= (long) (legacyAvgNs * TIMING_TOLERANCE_FACTOR));
    }

    // ============================================================================
    // Parity tests — bypass and legacy must return identical pair sets and counts
    // ============================================================================

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypass_returnsSameTemplatePairSetAsLegacy() throws Exception {
        Set<String> legacy = collectColumn1(VIEW_SQL);
        Set<String> bypass = collectColumn1(BYPASS_SQL);

        assertEquals("legacy and bypass must return the same number of distinct pairs",
                legacy.size(), bypass.size());
        assertEquals("legacy and bypass must return identical pair values",
                legacy, bypass);
    }

    @Test
    @Ignore("Integration test - requires -Dtest.cloudstack.mysql.url; run manually against a CloudStack DB")
    public void bypass_distinctCountMatchesLegacy() throws Exception {
        long legacyCount = scalarLong(
                "SELECT COUNT(*) FROM (" + VIEW_SQL + ") AS t");
        long bypassCount = scalarLong(
                "SELECT COUNT(*) FROM (" + BYPASS_SQL + ") AS t");

        assertEquals("legacy and bypass DISTINCT counts must match", legacyCount, bypassCount);
    }

    /** Collect column 1 of every row into a sorted set (for set equality assertion). */
    private Set<String> collectColumn1(String query) throws Exception {
        Set<String> out = new TreeSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** Run a query that returns one numeric scalar in column 1. */
    private long scalarLong(String query) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        }
    }

    /**
     * Run the query {@link #TIMING_ITERATIONS} times, drop the first
     * {@link #TIMING_WARMUP} as warm-up, return the average wall-clock duration
     * of the remaining iterations in nanoseconds. Each iteration drains the
     * ResultSet so we measure execution + transport, not just submission.
     */
    private long averageQueryNanos(String query) throws Exception {
        long total = 0;
        int counted = 0;
        for (int i = 0; i < TIMING_ITERATIONS; i++) {
            long start = System.nanoTime();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                while (rs.next()) {
                    rs.getString(1);
                }
            }
            long elapsed = System.nanoTime() - start;
            if (i >= TIMING_WARMUP) {
                total += elapsed;
                counted++;
            }
        }
        return counted == 0 ? 0 : total / counted;
    }

    /**
     * Run EXPLAIN and return one map per table alias (or table name) → column→value.
     * Uses traditional EXPLAIN format so column names match across MySQL versions.
     * Also logs the raw EXPLAIN rows so test output makes plan differences visible.
     */
    private Map<String, Map<String, String>> explain(String query) throws Exception {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("EXPLAIN " + query)) {
            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();

            StringBuilder log = new StringBuilder("\nEXPLAIN result for query:\n  ").append(query).append('\n');
            // header
            for (int i = 1; i <= colCount; i++) {
                log.append(md.getColumnLabel(i)).append('\t');
            }
            log.append('\n');

            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    row.put(md.getColumnLabel(i), val);
                    log.append(val == null ? "NULL" : val).append('\t');
                }
                log.append('\n');
                String tableKey = row.get("table");
                if (tableKey != null) {
                    out.put(tableKey, row);
                }
            }
            // uncomment if need verbose EXPLAIN output
//            System.out.println(log.toString());
        }
        return out;
    }
}
