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

package org.apache.cloudstack.framework.websocket.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketRouter;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

/**
 * Netty WebSocket server that delegates routing to WebSocketRouter.
 * Replaces the previous helper-based WebSocketServer.
 */
public final class WebSocketServer {
    private static final Logger LOG = LogManager.getLogger(WebSocketServer.class);

    private final String host;
    private final int port;
    private final WebSocketRouter router;
    private final String websocketBasePath;
    private final boolean sslEnabled;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running;

    public WebSocketServer(int port, WebSocketRouter router, boolean sslEnabled) {
        this(null, port, router, null, sslEnabled);
    }

    public WebSocketServer(String host, int port, WebSocketRouter router, String websocketBasePath, boolean sslEnabled) {
        this.host = StringUtils.isBlank(host) ? "0.0.0.0" : host;
        this.port = port;
        this.router = router;
        this.websocketBasePath = StringUtils.isBlank(websocketBasePath) ?
                WebSocketRouter.WEBSOCKET_PATH_PREFIX : websocketBasePath;
        this.sslEnabled = sslEnabled;
    }

    protected KeyManagerFactory buildKeyManagerFactory(Path storePath, char[] password) throws
            KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance(detectType(storePath));
        try (InputStream in = Files.newInputStream(storePath)) {
            ks.load(in, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        return kmf;
    }

    private static String detectType(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return (name.endsWith(".p12") || name.endsWith(".pfx")) ? "PKCS12" : "JKS";
    }

    /**
     * Creates a Netty SslContext:
     * uses only a keystore containing the server's private key and certificate chain.
     *
     * @param keystoreFile     Path to the keystore file (JKS or PKCS12)
     * @param keystorePassword Password for both the keystore and key entry
     * @return configured Netty SslContext
     */
    protected SslContext createServerSslContext(String keystoreFile, String keystorePassword) throws
            UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyManagerFactory kmf = buildKeyManagerFactory(Path.of(keystoreFile), keystorePassword.toCharArray());

        // Build Netty SSL context (server mode) – same as Jetty's SSLContextFactory.Server
        return SslContextBuilder.forServer(kmf)
                .sslProvider(SslProvider.JDK)                     // Use JDK provider (same as Jetty)
                .protocols("TLSv1.3", "TLSv1.2")                   // Match Jetty default supported protocols
                .build();
    }

    protected SslContext createServerSslContextIfNeeded() throws IllegalArgumentException {
        if (!sslEnabled) {
            return null;
        }
        String keystoreFile = ServerPropertiesUtil.getProperty(ServerPropertiesUtil.KEY_KEYSTORE_FILE);
        String keystorePassword = ServerPropertiesUtil.getProperty(ServerPropertiesUtil.KEY_KEYSTORE_FILE);
        if (StringUtils.isBlank(keystoreFile) || StringUtils.isBlank(keystorePassword)) {
            throw new IllegalArgumentException("SSL is enabled but keystore file or password is not configured");
        }
        if (Files.exists(Path.of(keystoreFile))) {
            throw new IllegalArgumentException(String.format("SSL is enabled but keystore file does not exist: %s",
                    keystoreFile));
        }
        try {
            return createServerSslContext(keystoreFile, keystorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                 UnrecoverableKeyException e) {
            throw new IllegalArgumentException(String.format(
                    "SSL is enabled but unable to create SSL context with configured keystore: %s", keystoreFile),
                    e);
        }
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        final SslContext nettySslCtx = createServerSslContextIfNeeded();

        final WebSocketServerProtocolConfig wsCfg = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath(websocketBasePath)
                .checkStartsWith(true)
                .allowExtensions(false).handshakeTimeoutMillis(10_000).build();

        ServerBootstrap b = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (nettySslCtx != null) {
                            p.addLast("ssl", nettySslCtx.newHandler(ch.alloc()));
                        }
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(65536));
                        p.addLast(new WebSocketServerProtocolHandler(wsCfg));
                        p.addLast(new WebSocketServerRoutingHandler(router));
                    }
                });

        serverChannel = b.bind(host, port).sync().channel();
        running = true;
        LOG.info("WebSocketServer listening on {}:{} (base path: {}, router={})", host, port,
                websocketBasePath, router);
    }

    public void stop(long maxWaitSeconds) {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(0, maxWaitSeconds, TimeUnit.SECONDS).sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(0, maxWaitSeconds, TimeUnit.SECONDS).sync();
            }
        } catch (InterruptedException e) {
            LOG.warn("Graceful stop interrupted; forcing shutdown", e);
            if (bossGroup != null) bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
            if (workerGroup != null) workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
        } finally {
            running = false;
            LOG.info("WebSocketServer stopped");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}
