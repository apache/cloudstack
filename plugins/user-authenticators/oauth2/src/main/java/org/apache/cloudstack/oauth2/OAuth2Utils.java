package org.apache.cloudstack.oauth2;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

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
}
