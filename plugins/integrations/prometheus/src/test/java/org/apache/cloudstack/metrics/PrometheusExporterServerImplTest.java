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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;

public class PrometheusExporterServerImplTest {

    private PrometheusExporterServerImpl server;
    private ConfigDepotImpl mockDepot;

    @Before
    public void setUp() throws Exception {
        server = new PrometheusExporterServerImpl();
        mockDepot = mock(ConfigDepotImpl.class);
        setConfigDepot(mockDepot);
        // EnablePrometheusExporter is a non-dynamic ConfigKey, so its cached _value survives
        // across tests (and even across test classes sharing this JVM) unless cleared here:
        // isDynamic()==false means value() only re-reads the (mocked) depot while _value==null.
        resetConfigKeyValue(PrometheusExporterServer.EnablePrometheusExporter);
        setStaticField("httpServer", null);
        setInstanceField(server, "httpExecutor", null);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        setConfigDepot(null);
        resetConfigKeyValue(PrometheusExporterServer.EnablePrometheusExporter);
        setStaticField("httpServer", null);
    }

    @Test
    public void testStartWhenDisabledDoesNotCreateServerOrExecutor() throws Exception {
        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "false");

        boolean result = server.start();

        assertTrue("start() should always return true", result);
        assertNull("httpServer should not be created when the exporter is disabled", getStaticField("httpServer"));
        assertNull("httpExecutor should not be created when the exporter is disabled", getInstanceField(server, "httpExecutor"));
    }

    @Test
    public void testStopWhenNeverStartedDoesNotThrow() {
        boolean result = server.stop();

        assertTrue("stop() should return true even if the server was never started", result);
    }

    @Test
    public void testStopShutsDownExecutorAndClosesServer() throws Exception {
        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "true");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterServerPort, "0");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterAllowedAddresses, "127.0.0.1");

        assertTrue(server.start());

        HttpServer startedHttpServer = (HttpServer) getStaticField("httpServer");
        ExecutorService startedExecutor = (ExecutorService) getInstanceField(server, "httpExecutor");
        int port = startedHttpServer.getAddress().getPort();

        assertFalse("Executor should be alive right after start()", startedExecutor.isShutdown());

        server.stop();

        assertTrue("stop() should shut down the http executor", startedExecutor.isShutdown());
        assertNull("httpExecutor field should be cleared after stop()", getInstanceField(server, "httpExecutor"));

        try {
            new Socket("127.0.0.1", port).close();
            org.junit.Assert.fail("Server socket should no longer accept connections after stop()");
        } catch (ConnectException expected) {
            // expected: the listening socket was closed by stop()
        }
    }

    @Test
    public void testStartCreatesFixedThreadPoolOfTwoAndWiresItToTheServer() throws Exception {
        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "true");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterServerPort, "0");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterAllowedAddresses, "127.0.0.1");

        assertTrue(server.start());

        Object executor = getInstanceField(server, "httpExecutor");
        assertTrue("httpExecutor should be a ThreadPoolExecutor", executor instanceof ThreadPoolExecutor);
        assertEquals("httpExecutor should be a fixed pool of 2 threads", 2, ((ThreadPoolExecutor) executor).getMaximumPoolSize());
    }

    @Test
    public void testAllowedRemoteAddressReceivesMetrics() throws Exception {
        PrometheusExporter mockExporter = mock(PrometheusExporter.class);
        when(mockExporter.getMetrics()).thenReturn("cloudstack_test_metric 1");
        setInstanceField(server, "prometheusExporter", mockExporter);

        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "true");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterServerPort, "0");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterAllowedAddresses, "127.0.0.1");

        assertTrue(server.start());
        int port = ((HttpServer) getStaticField("httpServer")).getAddress().getPort();

        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/metrics").openConnection();
        try {
            assertEquals(200, connection.getResponseCode());
            String body = readFully(connection.getInputStream());
            assertEquals("cloudstack_test_metric 1", body);
        } finally {
            connection.disconnect();
        }

        verify(mockExporter, times(1)).updateMetrics();
        verify(mockExporter, times(1)).getMetrics();
    }

    @Test
    public void testDisallowedRemoteAddressReceivesForbidden() throws Exception {
        PrometheusExporter mockExporter = mock(PrometheusExporter.class);
        setInstanceField(server, "prometheusExporter", mockExporter);

        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "true");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterServerPort, "0");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterAllowedAddresses, "10.0.0.1");

        assertTrue(server.start());
        int port = ((HttpServer) getStaticField("httpServer")).getAddress().getPort();

        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/metrics").openConnection();
        try {
            assertEquals(403, connection.getResponseCode());
            String body = readFully(connection.getErrorStream());
            assertEquals("Forbidden", body);
        } finally {
            connection.disconnect();
        }

        verify(mockExporter, times(0)).updateMetrics();
    }

    @Test
    public void testRootPathReturnsLandingPage() throws Exception {
        stubConfigValue(PrometheusExporterServer.EnablePrometheusExporter, "true");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterServerPort, "0");
        stubConfigValue(PrometheusExporterServer.PrometheusExporterAllowedAddresses, "127.0.0.1");

        assertTrue(server.start());
        int port = ((HttpServer) getStaticField("httpServer")).getAddress().getPort();

        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/").openConnection();
        try {
            assertEquals(200, connection.getResponseCode());
            String body = readFully(connection.getInputStream());
            assertTrue("Landing page should link to /metrics", body.contains("/metrics"));
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void testGetConfigComponentName() {
        assertEquals("PrometheusExporter", server.getConfigComponentName());
    }

    @Test
    public void testGetConfigKeysIncludesMinRefreshIntervalAddedByTheScrapeThrottlingFix() {
        ConfigKey<?>[] keys = server.getConfigKeys();

        assertArrayEquals(new ConfigKey<?>[]{
                PrometheusExporterServer.EnablePrometheusExporter,
                PrometheusExporterServer.PrometheusExporterServerPort,
                PrometheusExporterServer.PrometheusExporterAllowedAddresses,
                PrometheusExporterServer.PrometheusExporterOfferingCountLimit,
                PrometheusExporterServer.PrometheusExporterMinRefreshInterval
        }, keys);
    }

    private static String readFully(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private void stubConfigValue(ConfigKey<?> configKey, String value) {
        when(mockDepot.getConfigStringValue(eq(configKey.key()), eq(ConfigKey.Scope.Global), isNull())).thenReturn(value);
    }

    private static void setConfigDepot(ConfigDepotImpl depot) throws Exception {
        Field field = ConfigKey.class.getDeclaredField("s_depot");
        field.setAccessible(true);
        field.set(null, depot);
    }

    private static void resetConfigKeyValue(ConfigKey<?> configKey) throws Exception {
        Field field = ConfigKey.class.getDeclaredField("_value");
        field.setAccessible(true);
        field.set(configKey, null);
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field field = PrometheusExporterServerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object getStaticField(String fieldName) throws Exception {
        Field field = PrometheusExporterServerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setInstanceField(Object target, String fieldName, Object value) throws Exception {
        Field field = PrometheusExporterServerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getInstanceField(Object target, String fieldName) throws Exception {
        Field field = PrometheusExporterServerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
