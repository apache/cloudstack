//
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
//

package org.apache.cloudstack.oauth2;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.user.DomainService;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.oauth2.api.command.DeleteOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.RegisterOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.UpdateOAuthProviderCmd;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OAuth2AuthManagerImplTest {

    @Spy
    @InjectMocks
    private OAuth2AuthManagerImpl _authManager;

    @Mock
    OauthProviderDao _oauthProviderDao;

    @Mock
    DomainService _domainService;

    @Mock
    MessageBus _messageBus;

    AutoCloseable closeable;
    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testRegisterOauthProvider() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(false);
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("OAuth is not enabled, please enable to register", e.getMessage());
        }

        // Test when provider is already registered
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);
        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider("testProvider");
        when(_authManager._oauthProviderDao.findByProviderAndDomain(Mockito.anyString(), Mockito.isNull())).thenReturn(providerVO);
        when(cmd.getProvider()).thenReturn("testProvider");
        when(cmd.getDomainId()).thenReturn(null);

        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("Global provider with the name testProvider is already registered", e.getMessage());
        }

        // Test when provider is github and secret key is not null
        when(cmd.getSecretKey()).thenReturn("testSecretKey");
        providerVO = null;
        when(_authManager._oauthProviderDao.findByProviderAndDomain(Mockito.anyString(), Mockito.isNull())).thenReturn(providerVO);
        OauthProviderVO savedProviderVO = new OauthProviderVO();
        when(cmd.getProvider()).thenReturn("github");
        when(cmd.getDomainId()).thenReturn(null);
        when(_authManager._oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenReturn(savedProviderVO);
        OauthProviderVO result = _authManager.registerOauthProvider(cmd);
        assertEquals("github", result.getProvider());
        assertEquals("testSecretKey", result.getSecretKey());
    }

    @Test
    public void testUpdateOauthProvider() {
        Long id = 1L;
        String description = "updated description";
        String clientId = "updated client id";
        String redirectUri = "updated redirect uri";
        String secretKey = "updated secret key";

        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getDescription()).thenReturn(description);
        when(cmd.getClientId()).thenReturn(clientId);
        when(cmd.getRedirectUri()).thenReturn(redirectUri);
        when(cmd.getSecretKey()).thenReturn(secretKey);

        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setDescription("old description");
        providerVO.setClientId("old client id");
        providerVO.setRedirectUri("old redirect uri");
        providerVO.setSecretKey("old secret key");

        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);

        OauthProviderVO updatedProviderVO = new OauthProviderVO();
        updatedProviderVO.setDescription(description);
        updatedProviderVO.setClientId(clientId);
        updatedProviderVO.setRedirectUri(redirectUri);
        updatedProviderVO.setSecretKey(secretKey);

        when(_oauthProviderDao.update(id, providerVO)).thenReturn(true);

        OauthProviderVO result = _authManager.updateOauthProvider(cmd);

        assertEquals(description, result.getDescription());
        assertEquals(clientId, result.getClientId());
        assertEquals(redirectUri, result.getRedirectUri());
        assertEquals(secretKey, result.getSecretKey());
    }

    @Test
    public void testUpdateOauthProviderReassignsDomain() {
        Long id = 5L;
        Long oldDomainId = 10L;
        Long newDomainId = 20L;

        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getDomainId()).thenReturn(newDomainId);

        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider("github");
        providerVO.setDomainId(oldDomainId);
        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);

        Domain newDomain = Mockito.mock(Domain.class);
        when(newDomain.getId()).thenReturn(newDomainId);
        when(_domainService.getDomain(Mockito.anyString())).thenReturn(newDomain);
        Mockito.doReturn(newDomainId).when(_authManager).resolveDomainIdFromIdOrPath(newDomainId, null);
        when(_oauthProviderDao.findByProviderAndDomain("github", newDomainId)).thenReturn(null);
        when(_oauthProviderDao.update(Mockito.eq(id), Mockito.any(OauthProviderVO.class))).thenReturn(true);
        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);

        OauthProviderVO result = _authManager.updateOauthProvider(cmd);
        assertEquals(newDomainId, result.getDomainId());
    }

    @Test
    public void testUpdateOauthProviderRejectsDuplicateAtTargetDomain() {
        Long id = 5L;
        Long oldDomainId = 10L;
        Long newDomainId = 20L;

        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getDomainId()).thenReturn(newDomainId);

        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider("github");
        providerVO.setDomainId(oldDomainId);
        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);

        Mockito.doReturn(newDomainId).when(_authManager).resolveDomainIdFromIdOrPath(newDomainId, null);
        OauthProviderVO collision = new OauthProviderVO();
        collision.setProvider("github");
        collision.setDomainId(newDomainId);
        when(_oauthProviderDao.findByProviderAndDomain("github", newDomainId)).thenReturn(collision);

        try {
            _authManager.updateOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException for duplicate at target domain");
        } catch (CloudRuntimeException e) {
            assertTrue(e.getMessage().contains("already registered"));
        }
    }

    @Test
    public void testRegisterOauthProviderForRootDomainTreatedAsGlobal() {
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd.getProvider()).thenReturn("github");
        when(cmd.getDomainId()).thenReturn(com.cloud.domain.Domain.ROOT_DOMAIN);
        when(cmd.getSecretKey()).thenReturn("secret");
        when(cmd.getClientId()).thenReturn("clientId");
        when(cmd.getRedirectUri()).thenReturn("https://redirect");

        // global check must be consulted (domainId resolves to null), not the ROOT domain scope
        when(_authManager.isOAuthPluginEnabled(Mockito.isNull())).thenReturn(true);
        when(_oauthProviderDao.findByProviderAndDomain("github", null)).thenReturn(null);
        when(_oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenAnswer(i -> i.getArgument(0));

        OauthProviderVO result = _authManager.registerOauthProvider(cmd);
        assertNull(result.getDomainId());
        Mockito.verify(_oauthProviderDao).findByProviderAndDomain("github", null);
    }

    @Test
    public void testNormalizeGlobalScopeMapsRootToNull() {
        assertNull(_authManager.normalizeGlobalScope(com.cloud.domain.Domain.ROOT_DOMAIN));
        assertNull(_authManager.normalizeGlobalScope(null));
        assertEquals(Long.valueOf(42L), _authManager.normalizeGlobalScope(42L));
    }

    @Test
    public void testUpdateOauthProviderRejectsEnableWhenPluginDisabledAtScope() {
        Long id = 7L;
        Long domainId = 42L;

        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getEnabled()).thenReturn(true);

        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider("github");
        providerVO.setDomainId(domainId);
        providerVO.setEnabled(false);

        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);
        Mockito.doReturn(false).when(_authManager).isOAuthPluginEnabled(domainId);

        try {
            _authManager.updateOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException when enabling provider while oauth2.enabled is false at scope");
        } catch (CloudRuntimeException e) {
            assertTrue(e.getMessage().contains("OAuth plugin is not enabled"));
        }

        Mockito.verify(_oauthProviderDao, Mockito.never()).update(Mockito.eq(id), Mockito.any(OauthProviderVO.class));
    }

    @Test
    public void testListOauthProviders() {
        String uuid = "1234-5678-9101";
        String provider = "testProvider";
        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider(provider);
        List<OauthProviderVO> providerList = Collections.singletonList(providerVO);

        // Test when uuid is not null
        when(_oauthProviderDao.findByUuid(uuid)).thenReturn(providerVO);
        List<OauthProviderVO> result = _authManager.listOauthProviders(null, uuid, null);
        assertEquals(providerList, result);

        // Test when provider is not blank
        when(_oauthProviderDao.findByProviderAndDomain(provider, null)).thenReturn(providerVO);
        result = _authManager.listOauthProviders(provider, null, null);
        assertEquals(providerList, result);

        // Test when both uuid and provider are null
        when(_oauthProviderDao.listAll()).thenReturn(providerList);
        result = _authManager.listOauthProviders(null, null, null);
        assertEquals(providerList, result);
    }

    @Test
    public void testDeleteOauthProviderHardDeletes() {
        Long providerId = 42L;
        when(_oauthProviderDao.expunge(providerId)).thenReturn(true);

        boolean result = _authManager.deleteOauthProvider(providerId);

        assertTrue(result);
        Mockito.verify(_oauthProviderDao).expunge(providerId);
        Mockito.verify(_oauthProviderDao, Mockito.never()).remove(Mockito.anyLong());
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> expectedCmdList = new ArrayList<>();
        expectedCmdList.add(RegisterOAuthProviderCmd.class);
        expectedCmdList.add(DeleteOAuthProviderCmd.class);
        expectedCmdList.add(UpdateOAuthProviderCmd.class);

        List<Class<?>> cmdList = _authManager.getCommands();

        assertEquals(expectedCmdList, cmdList);
    }

    @Test
    public void testStart() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);
        doNothing().when(_authManager).initializeUserOAuth2AuthenticationProvidersMap();
        boolean result = _authManager.start();
        assertTrue(result);

        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(false);
        result = _authManager.start();
        assertTrue(result);
    }

    @Test
    public void testRegisterOauthProviderWithDomain() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd.getProvider()).thenReturn("github");
        when(cmd.getDomainId()).thenReturn(5L);
        when(cmd.getSecretKey()).thenReturn("secret");
        when(cmd.getClientId()).thenReturn("clientId");
        when(cmd.getRedirectUri()).thenReturn("https://redirect");

        // No existing provider for this domain
        when(_oauthProviderDao.findByProviderAndDomain("github", 5L)).thenReturn(null);
        when(_oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenAnswer(i -> i.getArgument(0));

        OauthProviderVO result = _authManager.registerOauthProvider(cmd);
        assertEquals("github", result.getProvider());
        assertEquals(Long.valueOf(5L), result.getDomainId());
    }

    @Test
    public void testRegisterOauthProviderDuplicateForDomain() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd.getProvider()).thenReturn("github");
        when(cmd.getDomainId()).thenReturn(5L);

        OauthProviderVO existing = new OauthProviderVO();
        existing.setProvider("github");
        existing.setDomainId(5L);
        when(_oauthProviderDao.findByProviderAndDomain("github", 5L)).thenReturn(existing);

        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("Provider with the name github is already registered for domain 5", e.getMessage());
        }
    }

    @Test
    public void testListOauthProvidersWithDomainId() {
        Long domainId = 5L;
        OauthProviderVO globalProvider = new OauthProviderVO();
        globalProvider.setProvider("google");
        OauthProviderVO domainProvider = new OauthProviderVO();
        domainProvider.setProvider("github");
        domainProvider.setDomainId(domainId);
        List<OauthProviderVO> providers = Arrays.asList(globalProvider, domainProvider);

        when(_oauthProviderDao.listByDomainIncludingGlobal(domainId)).thenReturn(providers);
        List<OauthProviderVO> result = _authManager.listOauthProviders(null, null, domainId);
        assertEquals(2, result.size());
    }

    @Test
    public void testListOauthProvidersByProviderAndDomain() {
        Long domainId = 5L;
        OauthProviderVO domainProvider = new OauthProviderVO();
        domainProvider.setProvider("github");
        domainProvider.setDomainId(domainId);

        when(_oauthProviderDao.findByProviderAndDomain("github", domainId)).thenReturn(domainProvider);
        List<OauthProviderVO> result = _authManager.listOauthProviders("github", null, domainId);
        assertEquals(1, result.size());
        assertEquals("github", result.get(0).getProvider());
        assertEquals(Long.valueOf(5L), result.get(0).getDomainId());
    }

    @Test
    public void testResolveDomainIdFromDomainUuid() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"test-uuid-123"});

        Domain domain = Mockito.mock(Domain.class);
        when(domain.getId()).thenReturn(10L);
        when(_domainService.getDomain("test-uuid-123")).thenReturn(domain);

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(10L), result);
    }

    @Test
    public void testResolveDomainIdGlobalFilter() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"-1"});

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(-1L), result);
    }

    @Test
    public void testResolveDomainIdFromDomainPath() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN, new String[]{"ROOT/child"});

        Domain domain = Mockito.mock(Domain.class);
        when(domain.getId()).thenReturn(20L);
        when(_domainService.findDomainByIdOrPath(null, "/ROOT/child/")).thenReturn(domain);

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void testResolveDomainIdFromDomainPathWithSlashes() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN, new String[]{"/ROOT/child/"});

        Domain domain = Mockito.mock(Domain.class);
        when(domain.getId()).thenReturn(20L);
        when(_domainService.findDomainByIdOrPath(null, "/ROOT/child/")).thenReturn(domain);

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void testResolveDomainIdReturnsNullWhenNotFound() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"nonexistent-uuid"});

        when(_domainService.getDomain("nonexistent-uuid")).thenReturn(null);

        Long result = _authManager.resolveDomainId(params);
        assertNull(result);
    }

    @Test
    public void testResolveDomainIdReturnsNullForEmptyParams() {
        Map<String, Object[]> params = new HashMap<>();
        Long result = _authManager.resolveDomainId(params);
        assertNull(result);
    }

    @Test
    public void testResolveDomainIdPrefersUuidOverPath() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"test-uuid"});
        params.put(ApiConstants.DOMAIN, new String[]{"/ROOT/child/"});

        Domain domain = Mockito.mock(Domain.class);
        when(domain.getId()).thenReturn(10L);
        when(_domainService.getDomain("test-uuid")).thenReturn(domain);

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(10L), result);
    }

    @Test
    public void testResolveDomainIdFallsBackToPathWhenUuidNotFound() {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"bad-uuid"});
        params.put(ApiConstants.DOMAIN, new String[]{"/ROOT/"});

        when(_domainService.getDomain("bad-uuid")).thenReturn(null);
        Domain domain = Mockito.mock(Domain.class);
        when(domain.getId()).thenReturn(1L);
        when(_domainService.findDomainByIdOrPath(null, "/ROOT/")).thenReturn(domain);

        Long result = _authManager.resolveDomainId(params);
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testUpdateOauthProviderNotFound() {
        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(999L);
        when(_oauthProviderDao.findById(999L)).thenReturn(null);

        try {
            _authManager.updateOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("Provider with the given id is not there", e.getMessage());
        }
    }

    @Test
    public void testGetUserOAuth2AuthenticationProviderEmptyName() {
        try {
            _authManager.getUserOAuth2AuthenticationProvider("");
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("OAuth2 authentication provider name is empty", e.getMessage());
        }
    }

    @Test
    public void testGetUserOAuth2AuthenticationProviderNotFound() {
        try {
            _authManager.getUserOAuth2AuthenticationProvider("nonexistent");
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertTrue(e.getMessage().contains("nonexistent"));
        }
    }

    //  Multiple-domain OAuth tests

    @Test
    public void testSameProviderRegisteredInTwoDifferentDomains() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);

        // Register github for domain 5
        RegisterOAuthProviderCmd cmd1 = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd1.getProvider()).thenReturn("github");
        when(cmd1.getDomainId()).thenReturn(5L);
        when(cmd1.getSecretKey()).thenReturn("secret1");
        when(_oauthProviderDao.findByProviderAndDomain("github", 5L)).thenReturn(null);
        when(_oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenAnswer(i -> i.getArgument(0));

        OauthProviderVO result1 = _authManager.registerOauthProvider(cmd1);
        assertEquals("github", result1.getProvider());
        assertEquals(Long.valueOf(5L), result1.getDomainId());

        // Register github for domain 10 — should succeed independently
        RegisterOAuthProviderCmd cmd2 = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd2.getProvider()).thenReturn("github");
        when(cmd2.getDomainId()).thenReturn(10L);
        when(cmd2.getSecretKey()).thenReturn("secret2");
        when(_oauthProviderDao.findByProviderAndDomain("github", 10L)).thenReturn(null);

        OauthProviderVO result2 = _authManager.registerOauthProvider(cmd2);
        assertEquals("github", result2.getProvider());
        assertEquals(Long.valueOf(10L), result2.getDomainId());
    }

    @Test
    public void testSameProviderRegisteredGloballyAndForDomain() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);

        // Global registration (domainId = null)
        RegisterOAuthProviderCmd globalCmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(globalCmd.getProvider()).thenReturn("google");
        when(globalCmd.getDomainId()).thenReturn(null);
        when(_oauthProviderDao.findByProviderAndDomain("google", null)).thenReturn(null);
        when(_oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenAnswer(i -> i.getArgument(0));

        OauthProviderVO globalResult = _authManager.registerOauthProvider(globalCmd);
        assertNull(globalResult.getDomainId());

        // Domain-specific registration for same provider — should succeed (different scope)
        RegisterOAuthProviderCmd domainCmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(domainCmd.getProvider()).thenReturn("google");
        when(domainCmd.getDomainId()).thenReturn(7L);
        when(_oauthProviderDao.findByProviderAndDomain("google", 7L)).thenReturn(null);

        OauthProviderVO domainResult = _authManager.registerOauthProvider(domainCmd);
        assertEquals(Long.valueOf(7L), domainResult.getDomainId());
    }

    @Test
    public void testListOauthProvidersForDomainIncludesGlobalProviders() {
        Long domainId = 5L;
        OauthProviderVO globalGoogle = new OauthProviderVO();
        globalGoogle.setProvider("google");
        // domainId is null — global

        OauthProviderVO domainGithub = new OauthProviderVO();
        domainGithub.setProvider("github");
        domainGithub.setDomainId(domainId);

        OauthProviderVO otherDomainGoogle = new OauthProviderVO();
        otherDomainGoogle.setProvider("google");
        otherDomainGoogle.setDomainId(10L);

        // listByDomainIncludingGlobal returns providers for domain 5 + global (not domain 10)
        when(_oauthProviderDao.listByDomainIncludingGlobal(domainId))
                .thenReturn(Arrays.asList(globalGoogle, domainGithub));

        List<OauthProviderVO> result = _authManager.listOauthProviders(null, null, domainId);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getDomainId() == null));         // global included
        assertTrue(result.stream().anyMatch(p -> Long.valueOf(5L).equals(p.getDomainId()))); // domain-specific included
        assertTrue(result.stream().noneMatch(p -> Long.valueOf(10L).equals(p.getDomainId()))); // other domain excluded
    }

    @Test
    public void testListAllProvidersAcrossAllDomains() {
        OauthProviderVO global = new OauthProviderVO();
        global.setProvider("google");

        OauthProviderVO domain5 = new OauthProviderVO();
        domain5.setProvider("github");
        domain5.setDomainId(5L);

        OauthProviderVO domain10 = new OauthProviderVO();
        domain10.setProvider("google");
        domain10.setDomainId(10L);

        when(_oauthProviderDao.listAll()).thenReturn(Arrays.asList(global, domain5, domain10));

        List<OauthProviderVO> result = _authManager.listOauthProviders(null, null, null);
        assertEquals(3, result.size());
    }

    @Test
    public void testDuplicateGlobalProviderRejected() {
        when(_authManager.isOAuthPluginEnabled(Mockito.nullable(Long.class))).thenReturn(true);
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        when(cmd.getProvider()).thenReturn("google");
        when(cmd.getDomainId()).thenReturn(null);

        OauthProviderVO existing = new OauthProviderVO();
        existing.setProvider("google");
        when(_oauthProviderDao.findByProviderAndDomain("google", null)).thenReturn(existing);

        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("Global provider with the name google is already registered", e.getMessage());
        }
    }

    @Test
    public void testDomainDeletionCleansUpOAuthProviders() {
        Long domainId = 42L;

        OauthProviderVO provider1 = new OauthProviderVO();
        provider1.setProvider("github");
        provider1.setDomainId(domainId);

        OauthProviderVO provider2 = new OauthProviderVO();
        provider2.setProvider("google");
        provider2.setDomainId(domainId);

        when(_oauthProviderDao.listByDomain(domainId)).thenReturn(Arrays.asList(provider1, provider2));
        when(_oauthProviderDao.expunge(Mockito.anyLong())).thenReturn(true);

        // Capture the subscriber registered during start()
        doNothing().when(_authManager).initializeUserOAuth2AuthenticationProvidersMap();
        Mockito.doAnswer(invocation -> {
            String subject = invocation.getArgument(0);
            MessageSubscriber subscriber = invocation.getArgument(1);
            // Simulate domain removal event
            DomainVO domain = Mockito.mock(DomainVO.class);
            when(domain.getId()).thenReturn(domainId);
            subscriber.onPublishMessage("", subject, domain);
            return null;
        }).when(_messageBus).subscribe(Mockito.eq(com.cloud.user.DomainManager.MESSAGE_PRE_REMOVE_DOMAIN_EVENT), Mockito.any());

        _authManager.start();

        verify(_oauthProviderDao).listByDomain(domainId);
        verify(_oauthProviderDao, Mockito.times(2)).expunge(Mockito.anyLong());
    }

}
