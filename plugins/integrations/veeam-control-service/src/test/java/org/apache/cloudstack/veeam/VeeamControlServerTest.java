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

package org.apache.cloudstack.veeam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class VeeamControlServerTest {

    private static final String KEY_ENABLED = "integration.veeam.control.enabled";
    private static final String KEY_PORT = "integration.veeam.control.port";
    private static final String KEY_CONTEXT_PATH = "integration.veeam.control.context.path";
    private static final String KEY_DEVELOPER_LOGS = "integration.veeam.control.developer.logs";

    private ConfigDepotImpl previousDepot;
    private Properties previousServerProperties;
    private final Map<String, String> globalValues = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        previousDepot = getConfigDepot();
        final ConfigDepotImpl depot = mock(ConfigDepotImpl.class);
        when(depot.getConfigStringValue(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            final String key = invocation.getArgument(0);
            final Scope scope = invocation.getArgument(1);
            if (scope == Scope.Global) {
                return globalValues.get(key);
            }
            return null;
        });
        setConfigDepot(depot);

        previousServerProperties = getServerProperties();
        Properties props = new Properties();
        props.setProperty("https.keystore", "");
        props.setProperty("https.keystore.password", "");
        setServerProperties(props);
    }

    @After
    public void tearDown() throws Exception {
        setConfigDepot(previousDepot);
        setServerProperties(previousServerProperties);
        resetConfigKeyCache(VeeamControlService.Enabled);
        resetConfigKeyCache(VeeamControlService.Port);
        resetConfigKeyCache(VeeamControlService.ContextPath);
        resetConfigKeyCache(VeeamControlService.DeveloperLogs);
    }

    @Test
    public void testConstructorSortsHandlersByPriorityDescending() throws Exception {
        final RouteHandler low = mock(RouteHandler.class);
        final RouteHandler high = mock(RouteHandler.class);
        when(low.priority()).thenReturn(1);
        when(high.priority()).thenReturn(10);

        final VeeamControlServer server = new VeeamControlServer(List.of(low, high), mock(VeeamControlService.class));
        final List<RouteHandler> handlers = getRouteHandlers(server);

        assertEquals(high, handlers.get(0));
        assertEquals(low, handlers.get(1));
    }

    @Test
    public void testStartIfEnabledReturnsWithoutStartingWhenDisabled() throws Exception {
        globalValues.put(KEY_ENABLED, "false");
        resetConfigKeyCache(VeeamControlService.Enabled);

        final VeeamControlServer server = new VeeamControlServer(List.of(), mock(VeeamControlService.class));
        server.startIfEnabled();

        assertNull(getJettyServer(server));
    }

    @Test
    public void testStartIfEnabledStartsHttpServerWhenEnabled() throws Exception {
        globalValues.put(KEY_ENABLED, "true");
        globalValues.put(KEY_PORT, "0");
        globalValues.put(KEY_CONTEXT_PATH, "/ovirt-engine");
        resetConfigKeyCache(VeeamControlService.Enabled);
        resetConfigKeyCache(VeeamControlService.Port);
        resetConfigKeyCache(VeeamControlService.ContextPath);

        final VeeamControlService service = mock(VeeamControlService.class);
        when(service.getCurrentManagementServerHostId()).thenReturn(1L);

        final VeeamControlServer server = new VeeamControlServer(List.of(), service);
        try {
            server.startIfEnabled();
            final Server jetty = getJettyServer(server);
            assertNotNull(jetty);
            assertTrue(jetty.isStarted());
        } finally {
            server.stop();
        }
    }

    @Test
    public void testStopStopsExistingJettyServerAndClearsReference() throws Exception {
        final VeeamControlServer server = new VeeamControlServer(List.of(), mock(VeeamControlService.class));
        final Server jetty = mock(Server.class);
        setJettyServer(server, jetty);

        server.stop();

        verify(jetty).stop();
        assertNull(getJettyServer(server));
    }

    @Test
    public void testGetRequestResponseMetadataIncludesHeadersWhenDeveloperLogsEnabled() throws Exception {
        globalValues.put(KEY_DEVELOPER_LOGS, "true");
        resetConfigKeyCache(VeeamControlService.DeveloperLogs);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api");
        request.setRemoteAddr("127.0.0.1");
        request.setQueryString("x=1");
        request.addHeader("X-Test", "abc");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(202);

        final Method metadataMethod = VeeamControlServer.class.getDeclaredMethod("getRequestResponseMetadata",
                HttpServletRequest.class, HttpServletResponse.class);
        metadataMethod.setAccessible(true);
        final String metadata = (String) metadataMethod.invoke(null, request, response);

        assertTrue(metadata.contains("remote address: 127.0.0.1"));
        assertTrue(metadata.contains("uri: /api?x=1"));
        assertTrue(metadata.contains("headers: [X-Test=abc;"));
        assertTrue(metadata.contains("status: 202"));
    }

    @SuppressWarnings("unchecked")
    private static List<RouteHandler> getRouteHandlers(final VeeamControlServer server) throws Exception {
        final Field field = VeeamControlServer.class.getDeclaredField("routeHandlers");
        field.setAccessible(true);
        return (List<RouteHandler>) field.get(server);
    }

    private static Server getJettyServer(final VeeamControlServer server) throws Exception {
        final Field field = VeeamControlServer.class.getDeclaredField("server");
        field.setAccessible(true);
        return (Server) field.get(server);
    }

    private static void setJettyServer(final VeeamControlServer server, final Server jetty) throws Exception {
        final Field field = VeeamControlServer.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(server, jetty);
    }

    private static ConfigDepotImpl getConfigDepot() throws Exception {
        final Field field = ConfigKey.class.getDeclaredField("s_depot");
        field.setAccessible(true);
        return (ConfigDepotImpl) field.get(null);
    }

    private static void setConfigDepot(final ConfigDepotImpl depot) throws Exception {
        final Field field = ConfigKey.class.getDeclaredField("s_depot");
        field.setAccessible(true);
        field.set(null, depot);
    }

    @SuppressWarnings("unchecked")
    private static Properties getServerProperties() throws Exception {
        final Field field = ServerPropertiesUtil.class.getDeclaredField("propertiesRef");
        field.setAccessible(true);
        final AtomicReference<Properties> ref = (AtomicReference<Properties>) field.get(null);
        return ref.get();
    }

    @SuppressWarnings("unchecked")
    private static void setServerProperties(final Properties properties) throws Exception {
        final Field field = ServerPropertiesUtil.class.getDeclaredField("propertiesRef");
        field.setAccessible(true);
        final AtomicReference<Properties> ref = (AtomicReference<Properties>) field.get(null);
        ref.set(properties);
    }

    private static void resetConfigKeyCache(final ConfigKey<?> configKey) throws Exception {
        final Field valueField = ConfigKey.class.getDeclaredField("_value");
        valueField.setAccessible(true);
        valueField.set(configKey, null);
    }
}
