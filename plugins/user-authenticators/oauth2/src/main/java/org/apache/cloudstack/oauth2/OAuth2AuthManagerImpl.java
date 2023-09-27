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

import com.cloud.user.User;
import com.cloud.user.UserVO;
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
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OAuth2AuthManagerImpl extends ManagerBase implements OAuth2AuthManager, Manager, Configurable {
    private static final Logger s_logger = Logger.getLogger(OAuth2AuthManagerImpl.class);
    @Inject
    private UserDao _userDao;

    @Inject
    OauthProviderDao _oauthProviderDao;

    protected static Map<String, UserOAuth2Authenticator> userOAuth2AuthenticationProvidersMap = new HashMap<>();

    private List<UserOAuth2Authenticator> userOAuth2AuthenticationProviders;

    @Override
    public List<Class<?>> getAuthCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(OauthLoginAPIAuthenticatorCmd.class);
        cmdList.add(ListOAuthProvidersCmd.class);
        return cmdList;
    }

    @Override
    public boolean start() {
        if (isOAuthPluginEnabled()) {
            s_logger.info("OAUTH plugin loaded");
            initializeUserOAuth2AuthenticationProvidersMap();
        } else {
            s_logger.info("OAUTH plugin not enabled so not loading");
        }
        return true;
    }

    private boolean isOAuthPluginEnabled() {
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
    public boolean authorizeUser(Long userId, String oAuthProviderId, boolean enable) {
        UserVO user = _userDao.getUser(userId);
        if (user != null) {
            if (enable) {
                user.setExternalEntity(oAuthProviderId);
                user.setSource(User.Source.OAUTH2);
            } else {
                return false;
            }
            _userDao.update(user.getId(), user);
            return true;
        }
        return false;
    }

    @Override
    public OauthProviderVO registerOauthProvider(RegisterOAuthProviderCmd cmd) {
        String description = cmd.getDescription();
        String provider = cmd.getProvider();
        String clientId = cmd.getClientId();
        String redirectUri = cmd.getRedirectUri();

        if (!OAuth2IsPluginEnabled.value()) {
            throw new CloudRuntimeException("OAuth is not enabled, please enable to register");
        }
        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(provider);
        if (providerVO != null) {
            throw new CloudRuntimeException(String.format("Provider with the name %s is already registered", provider));
        }

        return saveOauthProvider(provider, description, clientId, redirectUri);
    }

    @Override
    public List<OauthProviderVO> listOauthProviders(ListOAuthProvidersCmd cmd) {
        if (OAuth2IsPluginEnabled.value()) {
            return _oauthProviderDao.listAll();
        }

        return new ArrayList<OauthProviderVO>();
    }

    private OauthProviderVO saveOauthProvider(String provider, String description, String clientId, String redirectUri) {
        final OauthProviderVO oauthProviderVO = new OauthProviderVO();

        oauthProviderVO.setProvider(provider);
        oauthProviderVO.setDescription(description);
        oauthProviderVO.setClientId(clientId);
        oauthProviderVO.setRedirectUri(redirectUri);

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
