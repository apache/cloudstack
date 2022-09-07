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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OAuth2Utils {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static GoogleAuthorizationCodeFlow newFlow() throws IOException {
        final String oauthClientId = System.getenv("OAUTH_CLIENT_ID");
        final String oauthClientSecret = System.getenv("OAUTH_CLIENT_SECRET");

        List<String> scopes = Arrays.asList(
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/userinfo.email");
        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), oauthClientId, oauthClientSecret, scopes)
                .setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
                .build();
    }

    public static boolean isUserLoggedIn(String sessionId) {
        try {
            Credential credential = newFlow().loadCredential(sessionId);
            return credential != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static Userinfo getUserInfo(String sessionId) throws IOException {
        String appName = "CLOUDSTACK";
        Credential credential = newFlow().loadCredential(sessionId);
        Oauth2 oauth2Client =
                new Oauth2.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), credential)
                        .setApplicationName(appName)
                        .build();

        Userinfo userInfo = oauth2Client.userinfo().get().execute();
        return userInfo;
    }
}
