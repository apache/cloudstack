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
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.auth.UserAuthenticator;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;

import javax.inject.Inject;
import java.util.Map;

import static org.apache.cloudstack.oauth2.OAuth2AuthManager.OAuth2IsPluginEnabled;

public class OAuth2UserAuthenticator extends AdapterBase implements UserAuthenticator {

    @Inject
    private UserAccountDao userAccountDao;
    @Inject
    private UserDao userDao;

    @Inject
    private OAuth2AuthManager userOAuth2mgr;

    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying OAuth2 auth for user: " + username);
        }

        if (!isOAuthPluginEnabled()) {
            logger.debug("OAuth2 plugin is disabled");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        } else if (requestParameters == null) {
            logger.debug("Request parameters are null");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        }

        final UserAccount userAccount = userAccountDao.getUserAccount(username, domainId);
        if (userAccount == null) {
            logger.debug("Unable to find user with " + username + " in domain " + domainId + ", or user source is not OAUTH2");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        } else {
            User user = userDao.getUser(userAccount.getId());
            final String[] provider = (String[])requestParameters.get(ApiConstants.PROVIDER);
            final String[] emailArray = (String[])requestParameters.get(ApiConstants.EMAIL);
            final String[] secretCodeArray = (String[])requestParameters.get(ApiConstants.SECRET_CODE);

            if (provider == null) {
                return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
            }

            String oauthProvider = ((provider == null) ? null : provider[0]);
            String email = ((emailArray == null) ? null : emailArray[0]);
            String secretCode = ((secretCodeArray == null) ? null : secretCodeArray[0]);

            UserOAuth2Authenticator authenticator = userOAuth2mgr.getUserOAuth2AuthenticationProvider(oauthProvider);
            if (user != null && authenticator.verifyUser(email, secretCode)) {
                return new Pair<Boolean, ActionOnFailedAuthentication>(true, null);
            }
        }
        // Deny all by default
        return new Pair<Boolean, ActionOnFailedAuthentication>(false, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);
    }

    @Override
    public String encode(String password) {
        return null;
    }

    protected boolean isOAuthPluginEnabled() {
        return OAuth2IsPluginEnabled.value();
    }
}
