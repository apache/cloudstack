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
package com.cloud.utils.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class QCOW2UtilsTest {

    InputStream inputStream;

    byte[] qcow2Bytes;

    HttpServer server;

    final Long virtualSize = 21474836480L;

    /**
     * The QCOW2 Header
     *
     * uint32_t magic;
     * uint32_t version;
     *
     * uint64_t backing_file_offset;
     * uint32_t backing_file_size;
     *
     * uint32_t cluster_bits;
     * uint64_t size;
     *
     * uint32_t crypt_method;
     *
     * uint32_t l1_size;
     * int64_t l1_table_offset;
     *
     * uint64_t refcount_table_offset;
     * uint32_t refcount_table_clusters;
     *
     * uint32_t nb_snapshots;
     * uint64_t snapshots_offset;
     *
     * @see https://people.gnome.org/~markmc/qcow-image-format.html
     */

    @Before
    public void setup() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(72);

        // Magic
        byteBuffer.put("QFI".getBytes(StandardCharsets.UTF_8));
        byteBuffer.put((byte)0xfb);

        // Version
        byteBuffer.putInt(2);

        // Backing file offset
        byteBuffer.putLong(0L);

        // Backing file size
        byteBuffer.putInt(0);

        // Cluster bits
        byteBuffer.putInt(0);

        // Size
        byteBuffer.putLong(virtualSize);

        // Crypt method
        byteBuffer.putInt(0);

        // L1 Size
        byteBuffer.putInt(0);

        // L1 Table offset
        byteBuffer.putLong(0L);

        // Refcount table offset
        byteBuffer.putLong(0L);

        // Refcount table cluster
        byteBuffer.putInt(0);

        // NB Snapshots
        byteBuffer.putInt(0);

        // Snapshots offset
        byteBuffer.putLong(0L);

        qcow2Bytes = byteBuffer.array();
        inputStream = new ByteArrayInputStream(qcow2Bytes);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void getVirtualSizeHeaderLocation() {
        assertEquals(24, QCOW2Utils.getVirtualSizeHeaderLocation());
    }

    @Test
    public void getVirtualSizeTest() throws IOException {
        assertEquals(virtualSize.longValue(), QCOW2Utils.getVirtualSize(inputStream, false));
    }

    @Test
    public void getRedirectUrlResolvesRelativeLocation() throws IOException {
        URL redirectUrl = QCOW2Utils.getRedirectUrl(new URL("http://example.com/templates/redirect"), "template.qcow2");
        assertEquals("http://example.com/templates/template.qcow2", redirectUrl.toString());
    }

    @Test
    public void getRedirectUrlAllowsHttpToHttps() throws IOException {
        URL redirectUrl = QCOW2Utils.getRedirectUrl(new URL("http://example.com/template.qcow2"), "https://example.com/template.qcow2");
        assertEquals("https://example.com/template.qcow2", redirectUrl.toString());
    }

    @Test
    public void getRedirectUrlRefusesHttpsToHttp() {
        assertThrows(IOException.class, () -> QCOW2Utils.getRedirectUrl(new URL("https://example.com/template.qcow2"), "http://example.com/template.qcow2"));
    }

    @Test
    public void getRedirectUrlRefusesNonHttpProtocol() {
        assertThrows(IOException.class, () -> QCOW2Utils.getRedirectUrl(new URL("http://example.com/template.qcow2"), "file:///etc/passwd"));
    }

    @Test
    public void getRedirectUrlRefusesMissingLocation() {
        assertThrows(IOException.class, () -> QCOW2Utils.getRedirectUrl(new URL("http://example.com/template.qcow2"), null));
    }

    @Test
    public void getVirtualSizeFromUrlFollowsRelativeRedirect() throws IOException {
        String baseUrl = startServer(exchange -> {
            if ("/template.qcow2".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(200, qcow2Bytes.length);
                exchange.getResponseBody().write(qcow2Bytes);
                exchange.close();
            } else {
                sendRedirect(exchange, "template.qcow2");
            }
        });
        assertEquals(virtualSize.longValue(), QCOW2Utils.getVirtualSizeFromUrl(baseUrl + "/redirect", true));
    }

    @Test
    public void getVirtualSizeFromUrlDoesNotFollowRedirectWhenDisabled() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        String baseUrl = startServer(exchange -> {
            requests.incrementAndGet();
            sendRedirect(exchange, "/template.qcow2");
        });
        assertThrows(IllegalArgumentException.class, () -> QCOW2Utils.getVirtualSizeFromUrl(baseUrl + "/redirect", false));
        assertEquals(1, requests.get());
    }

    @Test
    public void getVirtualSizeFromUrlStopsAfterTooManyRedirects() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        String baseUrl = startServer(exchange -> {
            requests.incrementAndGet();
            sendRedirect(exchange, "/redirect");
        });
        assertThrows(IllegalArgumentException.class, () -> QCOW2Utils.getVirtualSizeFromUrl(baseUrl + "/redirect", true));
        assertEquals(11, requests.get());
    }

    @Test
    public void getVirtualSizeFromUrlFollowsHttpToHttpsRedirect() throws Exception {
        // HttpURLConnection does not follow http to https redirects by itself, so check that the https target is contacted
        try (ServerSocket httpsTarget = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            CountDownLatch httpsConnected = new CountDownLatch(1);
            Thread acceptor = new Thread(() -> {
                try (Socket socket = httpsTarget.accept()) {
                    httpsConnected.countDown();
                } catch (IOException e) {
                    // the target socket was closed without a connection
                }
            });
            acceptor.start();
            String baseUrl = startServer(exchange -> sendRedirect(exchange, "https://127.0.0.1:" + httpsTarget.getLocalPort() + "/template.qcow2"));

            // the TLS handshake fails as the target is not an https server
            assertThrows(IllegalArgumentException.class, () -> QCOW2Utils.getVirtualSizeFromUrl(baseUrl + "/template.qcow2", true));
            assertTrue(httpsConnected.await(10, TimeUnit.SECONDS));
        }
    }

    private String startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
