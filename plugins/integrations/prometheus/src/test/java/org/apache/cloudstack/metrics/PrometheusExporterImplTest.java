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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import com.cloud.dc.dao.DataCenterDao;

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

    /**
     * Two rapid calls to updateMetrics() within the min refresh interval
     * should result in only one actual recomputation (one call to dcDao.listAll()).
     */
    @Test
    public void testUpdateMetricsTTLGuardSkipsSecondCall() throws Exception {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();

        DataCenterDao mockDcDao = mock(DataCenterDao.class);
        when(mockDcDao.listAll()).thenReturn(Collections.emptyList());
        setField(exporter, "dcDao", mockDcDao);

        // First call should trigger recomputation
        exporter.updateMetrics();
        // Second immediate call should be skipped by the TTL guard
        exporter.updateMetrics();

        verify(mockDcDao, times(1)).listAll();
    }

    /**
     * After the min refresh interval has elapsed, updateMetrics() should
     * trigger a fresh recomputation.
     */
    @Test
    public void testUpdateMetricsTTLGuardAllowsAfterInterval() throws Exception {
        PrometheusExporterImpl exporter = new PrometheusExporterImpl();

        DataCenterDao mockDcDao = mock(DataCenterDao.class);
        when(mockDcDao.listAll()).thenReturn(Collections.emptyList());
        setField(exporter, "dcDao", mockDcDao);

        // First call
        exporter.updateMetrics();

        // Simulate that the min interval has already elapsed by resetting lastMetricsUpdateTime
        setField(exporter, "lastMetricsUpdateTime", 0L);

        // Second call should now trigger recomputation
        exporter.updateMetrics();

        verify(mockDcDao, times(2)).listAll();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }
}
