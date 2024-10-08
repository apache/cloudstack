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
package com.cloud.consoleproxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class ConsoleProxyNoVNCServer {

    protected static Logger LOGGER = LogManager.getLogger(ConsoleProxyNoVNCServer.class);
    public static final int WS_PORT = 8080;
    public static final int WSS_PORT = 8443;
    private static final String VNC_CONF_FILE_LOCATION = "/root/vncport";

    private Server server;

    public static int getVNCPort() {
        String portStr;
        try {
            portStr = Files.readString(Path.of(VNC_CONF_FILE_LOCATION)).trim();
        } catch (IOException e) {
            LOGGER.error("Cannot read the VNC port from the file " + VNC_CONF_FILE_LOCATION + " setting it to 8080", e);
            return WS_PORT;
        }
        return Integer.parseInt(portStr);
    }

    public ConsoleProxyNoVNCServer() {
        this.server = new Server(WS_PORT);
        ConsoleProxyNoVNCHandler handler = new ConsoleProxyNoVNCHandler();
        this.server.setHandler(handler);
    }

    public ConsoleProxyNoVNCServer(byte[] ksBits, String ksPassword) {
        this.server = new Server();
        ConsoleProxyNoVNCHandler handler = new ConsoleProxyNoVNCHandler();
        this.server.setHandler(handler);

        try {
            final HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(WSS_PORT);

            final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            char[] passphrase = ksPassword != null ? ksPassword.toCharArray() : null;
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new ByteArrayInputStream(ksBits), passphrase);
            sslContextFactory.setKeyStore(ks);
            sslContextFactory.setKeyStorePassword(ksPassword);
            sslContextFactory.setKeyManagerPassword(ksPassword);

            final ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(WSS_PORT);
            server.addConnector(sslConnector);
        } catch (Exception e) {
            LOGGER.error("Unable to secure server due to exception ", e);
        }
    }

    public void start() throws Exception {
        this.server.start();
    }
}
