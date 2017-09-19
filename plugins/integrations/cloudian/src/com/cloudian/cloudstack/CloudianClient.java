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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.cloud.utils.nio.TrustAllManager;

public class CloudianClient {
    private static final Logger LOG = Logger.getLogger(CloudianClient.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final boolean validateSSLCertificate;

    public CloudianClient(final String baseUrl, final String username, final String password, final boolean validateSSlCertificate) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.baseUrl = baseUrl;
        this.validateSSLCertificate = validateSSlCertificate;

        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null,  new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());

        SSLContext sslcontext = SSLContexts.custom().useSSL().build();
        sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);

        this.httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .setSSLSocketFactory(factory)
                .build();
    }

    private void sendGET(final String path) throws IOException {
        HttpResponse response = httpClient.execute(new HttpGet(baseUrl + path));
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        StringBuilder result = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            LOG.debug(output);
            result.append(output);
        }
        try {
            JSONObject o = new JSONObject(result.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        try {
            sendGET("/user/list?groupId=0&userType=all&userStatus=all");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
