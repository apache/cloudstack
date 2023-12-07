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

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.oauth2.api.command.RegisterOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.UpdateOAuthProviderCmd;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;

import java.util.List;

public interface OAuth2AuthManager extends PluggableAPIAuthenticator, PluggableService {
    public static ConfigKey<Boolean> OAuth2IsPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "oauth2.enabled", "false",
            "Indicates whether OAuth plugin is enabled or not", false);
    public static final ConfigKey<String> OAuth2Plugins = new ConfigKey<String>("Advanced", String.class, "oauth2.plugins", "google,github",
            "List of OAuth plugins", true);
    public static final ConfigKey<String> OAuth2PluginsExclude = new ConfigKey<String>("Advanced", String.class, "oauth2.plugins.exclude", "",
            "List of OAuth plugins which are excluded", true);

    /**
     * Lists user OAuth2 provider plugins
     * @return list of providers
     */
    List<UserOAuth2Authenticator> listUserOAuth2AuthenticationProviders();

    /**
     * Finds user OAuth2 provider by name
     * @param providerName name of the provider
     * @return OAuth2 provider
     */
    UserOAuth2Authenticator getUserOAuth2AuthenticationProvider(final String providerName);

    String verifyCodeAndFetchEmail(String code, String provider);

    OauthProviderVO registerOauthProvider(RegisterOAuthProviderCmd cmd);

    List<OauthProviderVO> listOauthProviders(String provider, String uuid);

    boolean deleteOauthProvider(Long id);

    OauthProviderVO updateOauthProvider(UpdateOAuthProviderCmd cmd);
}
