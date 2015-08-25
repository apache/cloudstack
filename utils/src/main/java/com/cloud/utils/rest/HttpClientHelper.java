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

package com.cloud.utils.rest;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

public class HttpClientHelper {

    private static final String HTTPS = HttpConstants.HTTPS;

    public static CloseableHttpClient createHttpClient(final int maxRedirects) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        final Registry<ConnectionSocketFactory> socketFactoryRegistry = createSocketFactoryConfigration();
        final BasicCookieStore cookieStore = new BasicCookieStore();
        return HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry))
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).setMaxRedirects(maxRedirects).build())
            .setDefaultCookieStore(cookieStore)
            .setRetryHandler(new StandardHttpRequestRetryHandler())
            .build();
    }

    private static Registry<ConnectionSocketFactory> createSocketFactoryConfigration() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
        final SSLConnectionSocketFactory cnnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
            .register(HTTPS, cnnectionSocketFactory)
            .build();

        return socketFactoryRegistry;
    }

}
