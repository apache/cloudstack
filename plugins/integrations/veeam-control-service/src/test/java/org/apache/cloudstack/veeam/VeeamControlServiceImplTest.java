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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.UuidUtils;

@RunWith(MockitoJUnitRunner.class)
public class VeeamControlServiceImplTest {

    private static final String KEY_ENABLED = "integration.veeam.control.enabled";
    private static final String KEY_PORT = "integration.veeam.control.port";
    private static final String KEY_USERNAME = "integration.veeam.control.api.username";
    private static final String KEY_PASSWORD = "integration.veeam.control.api.password";
    private static final String KEY_ALLOWED_CIDRS = "integration.veeam.control.allowed.client.cidrs";

    private ConfigDepotImpl previousDepot;
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
    }

    @After
    public void tearDown() throws Exception {
        setConfigDepot(previousDepot);
        resetConfigKeyCache(VeeamControlService.Enabled);
        resetConfigKeyCache(VeeamControlService.Port);
        resetConfigKeyCache(VeeamControlService.Username);
        resetConfigKeyCache(VeeamControlService.Password);
        resetConfigKeyCache(VeeamControlService.AllowedClientCidrs);
    }

    @Test
    public void testParseImageTransfer() {
        String data = "{\"active\":false,\"direction\":\"upload\",\"format\":\"cow\",\"inactivity_timeout\":3600,\"phase\":\"cancelled\",\"shallow\":false,\"transferred\":0,\"link\":[],\"disk\":{\"id\":\"dba4d72d-01de-4267-aa8e-305996b53599\"},\"image\":{},\"backup\":{\"creation_date\":0}}";
        Mapper mapper = new Mapper();
        try {
            mapper.jsonMapper().readValue(data, ImageTransfer.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetAllowedClientCidrsInternalSanitizesAndFiltersInvalidEntries() throws Exception {
        globalValues.put(KEY_ALLOWED_CIDRS, " 10.0.0.0/24,invalid-cidr, ,192.168.1.100/32 ");
        resetConfigKeyCache(VeeamControlService.AllowedClientCidrs);
        final VeeamControlServiceImpl service = new VeeamControlServiceImpl();

        final List<String> cidrs = service.getAllowedClientCidrsInternal();

        assertEquals(List.of("10.0.0.0/24", "192.168.1.100/32"), cidrs);
    }

    @Test
    public void testGetAllowedClientCidrsInternalReturnsEmptyListForBlankValue() throws Exception {
        globalValues.put(KEY_ALLOWED_CIDRS, "   ");
        resetConfigKeyCache(VeeamControlService.AllowedClientCidrs);
        final VeeamControlServiceImpl service = new VeeamControlServiceImpl();

        assertTrue(service.getAllowedClientCidrsInternal().isEmpty());
    }

    @Test
    public void testValidateCredentials() throws Exception {
        globalValues.put(KEY_USERNAME, "veeam-user");
        globalValues.put(KEY_PASSWORD, "veeam-pass");
        resetConfigKeyCache(VeeamControlService.Username);
        resetConfigKeyCache(VeeamControlService.Password);
        final VeeamControlServiceImpl service = new VeeamControlServiceImpl();

        assertTrue(service.validateCredentials("veeam-user", "veeam-pass"));
        assertFalse(service.validateCredentials("veeam-user", "wrong"));
        assertFalse(service.validateCredentials("wrong", "veeam-pass"));
    }

    @Test
    public void testGetInstanceIdReturnsSystemAccountUuid() {
        final VeeamControlServiceImpl service = new VeeamControlServiceImpl();
        final AccountService accountService = mock(AccountService.class);
        final Account account = mock(Account.class);
        service.accountService = accountService;
        when(accountService.getSystemAccount()).thenReturn(account);
        when(account.getUuid()).thenReturn("system-account-uuid");

        assertEquals("system-account-uuid", service.getInstanceId());
    }

    @Test
    public void testGetHmacSecretUsesConfiguredInputs() throws Exception {
        globalValues.put(KEY_PORT, "8095");
        globalValues.put(KEY_USERNAME, "api-user");
        globalValues.put(KEY_PASSWORD, "api-pass");
        resetConfigKeyCache(VeeamControlService.Port);
        resetConfigKeyCache(VeeamControlService.Username);
        resetConfigKeyCache(VeeamControlService.Password);

        final VeeamControlServiceImpl service = Mockito.spy(new VeeamControlServiceImpl());
        Mockito.doReturn("instance-uuid").when(service).getInstanceId();

        final String expected = UuidUtils.nameUUIDFromBytes(
                "instance-uuid:8095:api-userapi-pass".getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(expected, service.getHmacSecret());
    }

    @Test
    public void testGetConfigKeysContainsExpectedEntries() {
        final VeeamControlServiceImpl service = new VeeamControlServiceImpl();

        final ConfigKey<?>[] keys = service.getConfigKeys();

        assertEquals(10, keys.length);
        assertEquals(KEY_ENABLED, keys[0].key());
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

    private static void resetConfigKeyCache(final ConfigKey<?> configKey) throws Exception {
        final Field valueField = ConfigKey.class.getDeclaredField("_value");
        valueField.setAccessible(true);
        valueField.set(configKey, null);
    }
}
