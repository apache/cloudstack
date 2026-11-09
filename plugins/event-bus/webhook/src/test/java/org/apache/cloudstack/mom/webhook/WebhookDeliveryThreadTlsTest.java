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

package org.apache.cloudstack.mom.webhook;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;

public class WebhookDeliveryThreadTlsTest {
    private static final String HOSTNAME = "webhook.test";
    private static final String BLOCKED_HOSTNAME = "internal.test";
    private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();

    private static SSLContext serverSslContext;
    private static SSLContext clientSslContext;

    private HttpsServer server;
    private int port;
    private final List<String> receivedHostHeaders = new CopyOnWriteArrayList<>();
    private final List<String> receivedSniHostNames = new CopyOnWriteArrayList<>();

    @BeforeClass
    public static void setUpTls() throws Exception {
        KeyPair keyPair = CertUtils.generateRandomKeyPair(2048);
        X509Certificate certificate = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(),
                "CN=" + HOSTNAME, "SHA256WithRSAEncryption", 1, Collections.singletonList(HOSTNAME), null);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keyPair.getPrivate(), KEYSTORE_PASSWORD, new Certificate[] {certificate});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);
        serverSslContext = SSLContext.getInstance("TLS");
        serverSslContext.init(kmf.getKeyManagers(), null, null);

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", certificate);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        clientSslContext = SSLContext.getInstance("TLS");
        clientSslContext.init(null, tmf.getTrustManagers(), null);
    }

    @Before
    public void startServer() throws Exception {
        server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverSslContext));
        server.createContext("/hook", exchange -> {
            recordRequest((HttpsExchange) exchange);
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/redirect-blocked", exchange -> {
            recordRequest((HttpsExchange) exchange);
            exchange.getResponseHeaders().add("Location",
                    String.format("https://%s:%d/hook", BLOCKED_HOSTNAME, port));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private void recordRequest(HttpsExchange exchange) {
        receivedHostHeaders.add(exchange.getRequestHeaders().getFirst("Host"));
        SSLSession session = exchange.getSSLSession();
        if (session instanceof ExtendedSSLSession) {
            for (SNIServerName name : ((ExtendedSSLSession) session).getRequestedServerNames()) {
                if (name instanceof SNIHostName) {
                    receivedSniHostNames.add(((SNIHostName) name).getAsciiName());
                }
            }
        }
    }

    private WebhookDeliveryThread.WebhookDeliveryResult deliver(String payloadUrl, String blocklist,
            boolean allowRedirects) {
        Webhook webhook = Mockito.mock(Webhook.class);
        Mockito.when(webhook.isSslVerification()).thenReturn(true);
        Mockito.when(webhook.getPayloadUrl()).thenReturn(payloadUrl);
        Event event = new Event("test", "TEST", "TEST.WEBHOOK", null, null);
        event.setEventUuid("event-uuid");
        event.setDescription("{\"test\":\"hello\"}");

        Map<String, String> hosts = Map.of(HOSTNAME, "127.0.0.1", BLOCKED_HOSTNAME, "10.0.0.1");
        DnsResolver testDnsResolver = host -> {
            String ip = hosts.get(host);
            if (ip == null) {
                throw new UnknownHostException(host);
            }
            return new InetAddress[] {InetAddress.getByName(ip)};
        };

        AtomicReference<WebhookDeliveryThread.WebhookDeliveryResult> result = new AtomicReference<>();
        WebhookDeliveryThread thread = new WebhookDeliveryThread(webhook, event, result::set) {
            @Override
            protected SSLConnectionSocketFactory getSslConnectionSocketFactory() {
                return new SSLConnectionSocketFactory(clientSslContext);
            }

            @Override
            protected DnsResolver getDnsResolver() {
                return new WebhookUrlValidator.DestinationPolicyDnsResolver(testDnsResolver, blocklist, false);
            }
        };
        thread.setDeliveryTries(1);
        thread.setDeliveryTimeout(5);
        thread.setAllowRedirects(allowRedirects);
        thread.run();
        Assert.assertNotNull(result.get());
        return result.get();
    }

    @Test
    public void testDeliveryUsesHostnameForTlsAndHostHeader() {
        WebhookDeliveryThread.WebhookDeliveryResult result =
                deliver(String.format("https://%s:%d/hook", HOSTNAME, port), "10.0.0.0/8", false);

        Assert.assertTrue(result.getResult(), result.isSuccess());
        Assert.assertEquals("ok", result.getResult());
        Assert.assertEquals(List.of(HOSTNAME + ":" + port), receivedHostHeaders);
        Assert.assertEquals(List.of(HOSTNAME), receivedSniHostNames);
    }

    @Test
    public void testDeliveryBlockedWhenHostResolvesToBlockedAddress() {
        WebhookDeliveryThread.WebhookDeliveryResult result =
                deliver(String.format("https://%s:%d/hook", HOSTNAME, port), "127.0.0.0/8", false);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getResult(), result.getResult().contains("blocked IP address"));
        Assert.assertTrue(receivedHostHeaders.isEmpty());
    }

    @Test
    public void testDeliveryBlockedWhenRedirectTargetResolvesToBlockedAddress() {
        WebhookDeliveryThread.WebhookDeliveryResult result =
                deliver(String.format("https://%s:%d/redirect-blocked", HOSTNAME, port), "10.0.0.0/8", true);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getResult(), result.getResult().contains(BLOCKED_HOSTNAME));
        Assert.assertTrue(result.getResult(), result.getResult().contains("blocked IP address"));
        Assert.assertEquals(1, receivedHostHeaders.size());
    }

    @Test
    public void testDeliveryCompletesCallbackForInvalidScheme() {
        WebhookDeliveryThread.WebhookDeliveryResult result =
                deliver(String.format("http://%s:%d/hook", HOSTNAME, port), "", false);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getResult(), result.getResult().contains("Only HTTPS"));
        Assert.assertTrue(receivedHostHeaders.isEmpty());
    }
}
