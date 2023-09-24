//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.oauth2.google;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Collections;

public class GoogleOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {
    private static final Logger s_logger = Logger.getLogger(GoogleOAuth2Provider.class);

    @Inject
    OauthProviderDao _oauthProviderDao;

    @Override
    public String getName() {
        return "google";
    }

    @Override
    public String getDescription() {
        return "Google OAuth2 Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException(String.format("Either email or secret code should not be null/empty"));
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudAuthenticationException("Google provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = verifySecretAndGetEmail(secretCode, providerVO.getClientId());

        if (!verifiedEmail.equals(email)) {
            throw new CloudAuthenticationException("Verification of the email and credentials failed as the email ");
        }

        return true;
    }

    public static String verifySecretAndGetEmail(String credential, String clientId) {
        String jwt = credential;
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(jwt);
        } catch (Exception e) {
            throw new CloudAuthenticationException("Could not verify the credentials provided, failed with exception:" + e.getMessage());
        }

        if (idToken == null) {
            throw new CloudAuthenticationException("Failed to verify the credentials send");
        }

        return idToken.getPayload().getEmail();
    }
}
