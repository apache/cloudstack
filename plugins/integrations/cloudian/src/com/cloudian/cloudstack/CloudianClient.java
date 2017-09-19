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

package com.cloudian.cloudstack;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class CloudianClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final boolean validateSSLCertificate;

    public CloudianClient(final String baseUrl, final String username, final String password, final boolean validateSSlCertificate) {
        this.baseUrl = baseUrl;
        this.validateSSLCertificate = validateSSlCertificate;


        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        this.httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
    }

    private void sendGET() throws IOException {
        HttpResponse response = httpClient.execute(new HttpGet(baseUrl));
        int statusCode = response.getStatusLine().getStatusCode();
    }

    private void sendPOST() throws IOException {
        HttpResponse response = httpClient.execute(new HttpPost(baseUrl));
        int statusCode = response.getStatusLine().getStatusCode();

    }

    private void sendPUT() throws IOException {
        HttpResponse response = httpClient.execute(new HttpPut(baseUrl));
        int statusCode = response.getStatusLine().getStatusCode();
    }

    public boolean addUserAccount() {
        return true;
    }

    public boolean listUserAccount() {
        return true;
    }

    public boolean updateUserAccount() {
        return true;
    }

    public boolean removeUserAccount() {
        return true;
    }

    public boolean addGroup() {
        return true;
    }

    public boolean listGroup() {
        return true;
    }

    public boolean updateGroup() {
        return true;
    }

    public boolean removeGroup() {
        return true;
    }

}
