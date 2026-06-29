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
package org.apache.cloudstack.storage.datastore.driver;

import javax.net.ssl.SSLContext;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import com.cloud.user.AccountDetailVO;
import com.cloud.utils.exception.CloudRuntimeException;

public final class EcsUtils {

    private EcsUtils() { }

    public static boolean isBlank(final String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Strips a trailing slash from a URL. */
    public static String trimTail(final String s) {
        if (s == null) return null;
        final String t = s.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    public static String valueOrNull(final AccountDetailVO d) {
        return d == null ? null : d.getValue();
    }

    public static CloseableHttpClient buildHttpClient(final boolean insecure) {
        if (!insecure) return HttpClients.createDefault();
        try {
            final TrustStrategy trustAll = (chain, authType) -> true;
            final SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, trustAll)
                    .build();
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS: failed to build HttpClient", e);
        }
    }

    /**
     * Performs a one-shot ECS /login to verify connectivity and credentials.
     * Used by the lifecycle during store initialization.
     */
    public static void verifyLogin(final String mgmtUrl, final String user, final String pass, final boolean insecure) {
        try (CloseableHttpClient http = buildHttpClient(insecure)) {
            final HttpGet get = new HttpGet(mgmtUrl + "/login");
            get.addHeader(new BasicScheme().authenticate(
                    new UsernamePasswordCredentials(user, pass), get, null));
            try (CloseableHttpResponse resp = http.execute(get)) {
                final int status = resp.getStatusLine().getStatusCode();
                if (status != 200 && status != 201) {
                    throw new CloudRuntimeException("ECS /login failed: HTTP " + status);
                }
                if (resp.getFirstHeader("X-SDS-AUTH-TOKEN") == null) {
                    throw new CloudRuntimeException("ECS /login missing X-SDS-AUTH-TOKEN header");
                }
            }
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("ECS: management login error: " + e.getMessage(), e);
        }
    }
}
