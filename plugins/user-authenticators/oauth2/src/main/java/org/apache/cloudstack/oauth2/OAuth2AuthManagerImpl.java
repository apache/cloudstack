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

import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.oauth2.api.command.DeleteOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.ListOAuthProvidersCmd;
import org.apache.cloudstack.oauth2.api.command.OauthLoginAPIAuthenticatorCmd;
import org.apache.cloudstack.oauth2.api.command.RegisterOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.UpdateOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.VerifyOAuthCodeAndGetUserCmd;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OAuth2AuthManagerImpl extends ManagerBase implements OAuth2AuthManager, Manager, Configurable {
    @Inject
    private UserDao _userDao;

    @Inject
    protected OauthProviderDao _oauthProviderDao;

    protected static Map<String, UserOAuth2Authenticator> userOAuth2AuthenticationProvidersMap = new HashMap<>();

    private List<UserOAuth2Authenticator> userOAuth2AuthenticationProviders;

    @Override
    public List<Class<?>> getAuthCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(OauthLoginAPIAuthenticatorCmd.class);
        cmdList.add(ListOAuthProvidersCmd.class);
        cmdList.add(VerifyOAuthCodeAndGetUserCmd.class);
        return cmdList;
    }

    @Override
    public boolean start() {
        if (isOAuthPluginEnabled()) {
            logger.info("OAUTH plugin loaded");
            initializeUserOAuth2AuthenticationProvidersMap();
        } else {
            logger.info("OAUTH plugin not enabled so not loading");
        }
        return true;
    }

    protected boolean isOAuthPluginEnabled() {
        return OAuth2IsPluginEnabled.value();
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(RegisterOAuthProviderCmd.class);
        cmdList.add(DeleteOAuthProviderCmd.class);
        cmdList.add(UpdateOAuthProviderCmd.class);

        return cmdList;
    }

    @Override
    public List<UserOAuth2Authenticator> listUserOAuth2AuthenticationProviders() {
        return userOAuth2AuthenticationProviders;
    }

    @Override
    public UserOAuth2Authenticator getUserOAuth2AuthenticationProvider(String providerName) {
        if (StringUtils.isEmpty(providerName)) {
            throw new CloudRuntimeException("OAuth2 authentication provider name is empty");
        }
        if (!userOAuth2AuthenticationProvidersMap.containsKey(providerName.toLowerCase())) {
            throw new CloudRuntimeException(String.format("Failed to find OAuth2 authentication provider by the name: %s.", providerName));
        }
        return userOAuth2AuthenticationProvidersMap.get(providerName.toLowerCase());
    }

    public List<UserOAuth2Authenticator> getUserOAuth2AuthenticationProviders() {
        return userOAuth2AuthenticationProviders;
    }

    public void setUserOAuth2AuthenticationProviders(final List<UserOAuth2Authenticator> userOAuth2AuthenticationProviders) {
        this.userOAuth2AuthenticationProviders = userOAuth2AuthenticationProviders;
    }

    protected void initializeUserOAuth2AuthenticationProvidersMap() {
        if (userOAuth2AuthenticationProviders != null) {
            for (final UserOAuth2Authenticator userOAuth2Authenticator : userOAuth2AuthenticationProviders) {
                userOAuth2AuthenticationProvidersMap.put(userOAuth2Authenticator.getName().toLowerCase(), userOAuth2Authenticator);
            }
        }
    }

    @Override
    public String verifyCodeAndFetchEmail(String code, String provider) {
        UserOAuth2Authenticator authenticator = getUserOAuth2AuthenticationProvider(provider);
        String email = authenticator.verifyCodeAndFetchEmail(code);

        return email;
    }

    @Override
    public OauthProviderVO registerOauthProvider(RegisterOAuthProviderCmd cmd) {
        String description = cmd.getDescription();
        String provider = cmd.getProvider();
        String clientId = StringUtils.trim(cmd.getClientId());
        String redirectUri = StringUtils.trim(cmd.getRedirectUri());
        String secretKey = StringUtils.trim(cmd.getSecretKey());

        if (!isOAuthPluginEnabled()) {
            throw new CloudRuntimeException("OAuth is not enabled, please enable to register");
        }
        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(provider);
        if (providerVO != null) {
            throw new CloudRuntimeException(String.format("Provider with the name %s is already registered", provider));
        }

        return saveOauthProvider(provider, description, clientId, secretKey, redirectUri);
    }

    @Override
    public List<OauthProviderVO> listOauthProviders(String provider, String uuid) {
        List<OauthProviderVO> providers;
        if (uuid != null) {
            providers = Collections.singletonList(_oauthProviderDao.findByUuid(uuid));
        } else if (StringUtils.isNotBlank(provider)) {
            providers = Collections.singletonList(_oauthProviderDao.findByProvider(provider));
        } else {
            providers = _oauthProviderDao.listAll();
        }
        return providers;
    }

    @Override
    public OauthProviderVO updateOauthProvider(UpdateOAuthProviderCmd cmd) {
        Long id = cmd.getId();
        String description = cmd.getDescription();
        String clientId = StringUtils.trim(cmd.getClientId());
        String redirectUri = StringUtils.trim(cmd.getRedirectUri());
        String secretKey = StringUtils.trim(cmd.getSecretKey());
        Boolean enabled = cmd.getEnabled();

        OauthProviderVO providerVO = _oauthProviderDao.findById(id);
        if (providerVO == null) {
            throw new CloudRuntimeException("Provider with the given id is not there");
        }

        if (StringUtils.isNotEmpty(description)) {
            providerVO.setDescription(description);
        }
        if (StringUtils.isNotEmpty(clientId)) {
            providerVO.setClientId(clientId);
        }
        if (StringUtils.isNotEmpty(redirectUri)) {
            providerVO.setRedirectUri(redirectUri);
        }
        if (StringUtils.isNotEmpty(secretKey)) {
            providerVO.setSecretKey(secretKey);
        }
        if (enabled != null) {
            providerVO.setEnabled(enabled);
        }

        _oauthProviderDao.update(id, providerVO);

        return _oauthProviderDao.findById(id);
    }

    private OauthProviderVO saveOauthProvider(String provider, String description, String clientId, String secretKey, String redirectUri) {
        final OauthProviderVO oauthProviderVO = new OauthProviderVO();

        oauthProviderVO.setProvider(provider);
        oauthProviderVO.setDescription(description);
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setSecretKey(secretKey);
        oauthProviderVO.setRedirectUri(redirectUri);
        oauthProviderVO.setEnabled(true);

        _oauthProviderDao.persist(oauthProviderVO);

        return oauthProviderVO;
    }

    @Override
    public boolean deleteOauthProvider(Long id) {
        return _oauthProviderDao.remove(id);
    }

    @Override
    public String getConfigComponentName() {
        return "OAUTH2-PLUGIN";
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {OAuth2IsPluginEnabled, OAuth2Plugins, OAuth2PluginsExclude};
    }
}
