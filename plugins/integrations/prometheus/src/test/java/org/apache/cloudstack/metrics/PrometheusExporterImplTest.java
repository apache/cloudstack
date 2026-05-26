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
package org.apache.cloudstack.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrometheusExporterImplTest {

    private static final String TEST_ZONE_NAME = "zone1";
    private static final String TEST_ZONE_UUID = "zone-uuid-1";
    private static final String TEST_HOST_NAME = "host1";
    private static final String TEST_HOST_UUID = "host-uuid-1";
    private static final String TEST_HOST_IP = "192.168.1.10";
    private static final long CERT_EXPIRY_TIME = 1735689600000L; // 2025-01-01 00:00:00 UTC
    private static final long CERT_EXPIRY_EPOCH = CERT_EXPIRY_TIME / 1000;

    @Test
    public void testItemHostCertExpiryFormat() {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();
        PrometheusExporterImpl.ItemHostCertExpiry item = exporter.new ItemHostCertExpiry(
                TEST_ZONE_NAME,
                TEST_ZONE_UUID,
                TEST_HOST_NAME,
                TEST_HOST_UUID,
                TEST_HOST_IP,
                CERT_EXPIRY_EPOCH
        );

        String metricsString = item.toMetricsString();
        String expected = String.format(
                "cloudstack_host_cert_expiry_timestamp{zone=\"%s\",hostname=\"%s\",ip=\"%s\"} %d",
                TEST_ZONE_NAME,
                TEST_HOST_NAME,
                TEST_HOST_IP,
                CERT_EXPIRY_EPOCH
        );
        assertEquals("Certificate expiry metric format should match expected format", expected, metricsString);
    }

    @Test
    public void testItemHostCertExpiryContainsCorrectMetricName() {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();
        PrometheusExporterImpl.ItemHostCertExpiry item = exporter.new ItemHostCertExpiry(
                TEST_ZONE_NAME,
                TEST_ZONE_UUID,
                TEST_HOST_NAME,
                TEST_HOST_UUID,
                TEST_HOST_IP,
                CERT_EXPIRY_EPOCH
        );

        String metricsString = item.toMetricsString();
        assertTrue("Metric should contain correct metric name",
                metricsString.contains("cloudstack_host_cert_expiry_timestamp"));
    }

    @Test
    public void testItemHostCertExpiryContainsAllLabels() {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();
        PrometheusExporterImpl.ItemHostCertExpiry item = exporter.new ItemHostCertExpiry(
                TEST_ZONE_NAME,
                TEST_ZONE_UUID,
                TEST_HOST_NAME,
                TEST_HOST_UUID,
                TEST_HOST_IP,
                CERT_EXPIRY_EPOCH
        );

        String metricsString = item.toMetricsString();
        assertTrue("Metric should contain zone label", metricsString.contains("zone=\"" + TEST_ZONE_NAME + "\""));
        assertTrue("Metric should contain hostname label", metricsString.contains("hostname=\"" + TEST_HOST_NAME + "\""));
        assertTrue("Metric should contain ip label", metricsString.contains("ip=\"" + TEST_HOST_IP + "\""));
    }

    @Test
    public void testItemHostCertExpiryContainsTimestampValue() {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();
        PrometheusExporterImpl.ItemHostCertExpiry item = exporter.new ItemHostCertExpiry(
                TEST_ZONE_NAME,
                TEST_ZONE_UUID,
                TEST_HOST_NAME,
                TEST_HOST_UUID,
                TEST_HOST_IP,
                CERT_EXPIRY_EPOCH
        );

        String metricsString = item.toMetricsString();
        assertTrue("Metric should contain correct timestamp value",
                metricsString.endsWith(" " + CERT_EXPIRY_EPOCH));
    }
}
