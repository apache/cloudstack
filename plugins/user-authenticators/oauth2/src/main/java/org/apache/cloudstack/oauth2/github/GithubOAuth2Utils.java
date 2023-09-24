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
package org.apache.cloudstack.oauth2.github;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.server.ServerProperties;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GithubOAuth2Utils {

    private static final Logger s_logger = Logger.getLogger(GithubOAuth2Utils.class);

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static GoogleAuthorizationCodeFlow newFlow() throws IOException {
        String oauthClientId = null;
        String oauthClientSecret = null;

        final File confFile = PropertiesUtil.findConfigFile("server.properties");

        s_logger.info("Server configuration file found: " + confFile.getAbsolutePath());

        try {
            InputStream is = new FileInputStream(confFile);
            final Properties properties = ServerProperties.getServerProperties(is);

            oauthClientId = "345798102268-cfcpg40k6hnfft2m61mf6jbmjcfg4p82.apps.googleusercontent.com";
            oauthClientSecret = "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";
        } catch (final IOException e) {
        }

        List<String> scopes = Arrays.asList(
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/userinfo.email");

        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), oauthClientId, oauthClientSecret, scopes)
                .setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
                .build();
    }

    public static boolean isUserLoggedIn(String userCredential) {
        try {
            Credential credential = newFlow().loadCredential(userCredential);
            return credential != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static Userinfo getUserInfo(String userCredential) throws IOException {
        String appName = "MyCloud";
        Credential credential = newFlow().loadCredential(userCredential);
        Oauth2 oauth2Client =
                new Oauth2.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), credential)
                        .setApplicationName(appName)
                        .build();

        Userinfo userInfo = oauth2Client.userinfo().get().execute();
        return userInfo;
    }

    public static String getEmail(String credential) {
        String jwt = credential;
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),new GsonFactory())
                .setAudience(Collections.singletonList("345798102268-cfcpg40k6hnfft2m61mf6jbmjcfg4p82.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken;

        try {
            idToken = verifier.verify(jwt);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Cannot verify the ID_TOKEN send :" + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(idToken == null){
            throw new RuntimeException("Failed to verify the ID_TOKEN send");
        }

        return  idToken.getPayload().getEmail();
    }
}
