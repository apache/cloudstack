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
package org.apache.cloudstack.oauth2.github;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    @Inject
    OauthProviderDao _oauthProviderDao;

    private String accessToken = null;

    @Override
    public String getName() {
        return "github";
    }

    @Override
    public String getDescription() {
        return "Github OAuth2 Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudRuntimeException(String.format("Either email or secretcode should not be null/empty"));
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudRuntimeException("Github provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = getUserEmailAddress();
        if (verifiedEmail == null || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }

        clearAccessToken();

        return true;
    }

    @Override
    public String verifyCodeAndFetchEmail(String secretCode) {
        String accessToken = getAccessToken(secretCode);
        if (accessToken == null) {
            return null;
        }
        return getUserEmailAddress();
    }

    protected String getAccessToken(String secretCode) throws CloudRuntimeException {
        OauthProviderVO githubProvider = _oauthProviderDao.findByProvider(getName());
        String tokenUrl = "https://github.com/login/oauth/access_token";
        String generatedAccessToken = null;
        try {
            URL url = new URL(tokenUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonParams = "{\"client_id\":\"" + githubProvider.getClientId() + "\",\"client_secret\":\"" + githubProvider.getSecretKey() + "\",\"code\":\"" + secretCode + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonParams.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    String regexPattern = "access_token=([^&]+)";
                    Pattern pattern = Pattern.compile(regexPattern);
                    Matcher matcher = pattern.matcher(response);
                    if (matcher.find()) {
                        generatedAccessToken = matcher.group(1);
                    } else {
                        throw new CloudRuntimeException("Could not fetch access token from the given code");
                    }
                }
            } else {
                throw new CloudRuntimeException("HTTP Request while fetching access token from github failed with error code: " + responseCode);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Error while trying to fetch the github access token : %s", e.getMessage()));
        }

        accessToken = generatedAccessToken;
        return accessToken;
    }

    public String getUserEmailAddress() throws CloudRuntimeException {
        if (accessToken == null) {
            throw new CloudRuntimeException("Access Token not found to fetch the email address");
        }

        String apiUrl = "https://api.github.com/user/emails";
        String email = null;
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + accessToken);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(response.toString());
                        if (jsonNode != null  && jsonNode.isArray()) {
                            JsonNode firstObject = jsonNode.get(0);
                            email = firstObject.get("email").asText();
                        } else {
                            throw new CloudRuntimeException("Invalid JSON format found while accessing email from github");
                        }
                    } catch (Exception e) {
                        throw new CloudRuntimeException(String.format("Error occurred while accessing email from github: %s", e.getMessage()));
                    }                }
            } else {
                throw new CloudRuntimeException(String.format("HTTP Request Failed with error code: %s", responseCode));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Error while trying to fetch email from github : %s", e.getMessage()));
        }

        return email;
    }

    private void clearAccessToken() {
        accessToken = null;
    }
}
