/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.utils.security.SSLUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.cloud.utils.exception.CloudRuntimeException;

public class NexentaNmsClient {
    protected Logger logger = LogManager.getLogger(getClass());

    protected NexentaNmsUrl nmsUrl = null;
    protected DefaultHttpClient httpClient = null;

    NexentaNmsClient(NexentaNmsUrl nmsUrl) {
        this.nmsUrl = nmsUrl;
    }

    private static boolean isSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    protected DefaultHttpClient getClient() {
        if (httpClient == null) {
            if (nmsUrl.getSchema().equalsIgnoreCase("http")) {
                httpClient = getHttpClient();
            } else {
                httpClient = getHttpsClient();
            }
            AuthScope authScope = new AuthScope(nmsUrl.getHost(), nmsUrl.getPort(), AuthScope.ANY_SCHEME, "basic");
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(nmsUrl.getUsername(), nmsUrl.getPassword());
            httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
        }
        return httpClient;
    }

    protected DefaultHttpClient getHttpsClient() {
        try {
            SSLContext sslContext = SSLUtils.getSSLContext();
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] {tm}, new SecureRandom());

            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", nmsUrl.getPort(), socketFactory));

            BasicClientConnectionManager mgr = new BasicClientConnectionManager(registry);

            return new DefaultHttpClient(mgr);
        } catch (NoSuchAlgorithmException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (KeyManagementException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    protected DefaultHttpClient getHttpClient() {
        return new DefaultHttpClient();
    }

    @SuppressWarnings("unused")
    static class NmsRequest {
        private String method;
        private String object;
        private Object[] params;

        NmsRequest(String object, String method) {
            this(object, method, (Object[])null);
        }

        NmsRequest(String object, String method, Object... params) {
            this.method = method;
            this.object = object;
            this.params = params;
        }

        @Override
        public String toString() {
            StringBuffer b = new StringBuffer();
            b.append("Request to ").append(object).append(" method ").append(method);
            if (params != null) {
                b.append(" params:");
                for (Object param:params) {
                    b.append(" ").append(param).append(",");
                }
            }
            return b.toString();
        }
    }

    @SuppressWarnings("unused")
    static class NmsError {
        private String message;

        public String getMesssage() {
            return message;
        }
    }

    @SuppressWarnings("unused")
    static class NmsResponse {
        @SerializedName("tg_flash") protected String tgFlash;

        protected NmsError error;

        NmsResponse() {}

        NmsResponse(String tgFlash, NmsError error) {
            this.tgFlash = tgFlash;
            this.error = error;
        }

        public String getTgFlash() {
            return tgFlash;
        }

        public NmsError getError() {
            return error;
        }
    }

    public NmsResponse execute(Class responseClass, String object, String method, Object... params) {
        StringBuilder sb = new StringBuilder();
        NmsRequest nmsRequest = new NmsRequest(object, method, params);
        if (logger.isDebugEnabled()) {
            logger.debug(nmsRequest);
        }
        final Gson gson = new Gson();
        String jsonRequest = gson.toJson(nmsRequest);
        StringEntity input = new StringEntity(jsonRequest, ContentType.APPLICATION_JSON);
        HttpPost postRequest = new HttpPost(nmsUrl.toString());
        postRequest.setEntity(input);

        DefaultHttpClient httpClient = getClient();
        try {
            HttpResponse response = httpClient.execute(postRequest);
            final int status = response.getStatusLine().getStatusCode();
            if (!isSuccess(status)) {
                throw new CloudRuntimeException("Failed on JSON-RPC API call. HTTP error code = " + status);
            }
            try(BufferedReader buffer = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));) {
                String tmp;
                while ((tmp = buffer.readLine()) != null) {
                    sb.append(tmp);
                }
            }catch (IOException ex) {
                throw new CloudRuntimeException(ex.getMessage());
            }
        } catch (ClientProtocolException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (IOException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.getConnectionManager().shutdown();
                } catch (Exception t) {
                    logger.debug(t.getMessage());
                }
            }
        }

        String responseString = sb.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("NexentaStor Appliance response: " + responseString);
        }

        NmsResponse nmsResponse = (NmsResponse) gson.fromJson(responseString, responseClass);
        if (nmsResponse.getError() != null) {
            throw new CloudRuntimeException(nmsResponse.getError().getMesssage());
        }

        return nmsResponse;
    }
}
