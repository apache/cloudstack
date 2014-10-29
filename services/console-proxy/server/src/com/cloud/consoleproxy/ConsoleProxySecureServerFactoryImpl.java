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

import com.cloud.utils.db.DbProperties;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.log4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class ConsoleProxySecureServerFactoryImpl implements ConsoleProxyServerFactory {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxySecureServerFactoryImpl.class);

    private SSLContext sslContext = null;

    public ConsoleProxySecureServerFactoryImpl() {
    }

    @Override
    public void init(byte[] ksBits, String ksPassword) {
        s_logger.info("Start initializing SSL");

        if (ksBits == null) {
            try {
                s_logger.info("Initializing SSL from built-in default certificate");

                final String pass = DbProperties.getDbProperties().getProperty("db.cloud.keyStorePassphrase");
                char[] passphrase = "vmops.com".toCharArray();
                if (pass != null) {
                    passphrase = pass.toCharArray();
                }
                KeyStore ks = KeyStore.getInstance("JKS");

                ks.load(new FileInputStream("certs/realhostip.keystore"), passphrase);
                // ks.load(ConsoleProxy.class.getResourceAsStream("/realhostip.keystore"), passphrase);

                s_logger.info("SSL certificate loaded");

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, passphrase);
                s_logger.info("Key manager factory is initialized");

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                s_logger.info("Trust manager factory is initialized");

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                s_logger.info("SSL context is initialized");
            } catch (Exception ioe) {
                s_logger.error(ioe.toString(), ioe);
            }

        } else {
            char[] passphrase = ksPassword != null ? ksPassword.toCharArray() : null;
            try {
                s_logger.info("Initializing SSL from passed-in certificate");

                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new ByteArrayInputStream(ksBits), passphrase);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, passphrase);
                s_logger.info("Key manager factory is initialized");

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                s_logger.info("Trust manager factory is initialized");

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                s_logger.info("SSL context is initialized");
            } catch (Exception e) {
                s_logger.error("Unable to init factory due to exception ", e);
            }
        }

    }

    @Override
    public HttpServer createHttpServerInstance(int port) throws IOException {
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 5);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {

                    // get the remote address if needed
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();

                    // get the default parameters
                    SSLParameters sslparams = c.getDefaultSSLParameters();

                    params.setSSLParameters(sslparams);
                    // statement above could throw IAE if any params invalid.
                    // eg. if app has a UI and parameters supplied by a user.
                }
            });

            s_logger.info("create HTTPS server instance on port: " + port);
            return server;
        } catch (Exception ioe) {
            s_logger.error(ioe.toString(), ioe);
        }
        return null;
    }

    @Override
    public SSLServerSocket createSSLServerSocket(int port) throws IOException {
        try {
            SSLServerSocket srvSock = null;
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            srvSock = (SSLServerSocket)ssf.createServerSocket(port);

            s_logger.info("create SSL server socket on port: " + port);
            return srvSock;
        } catch (Exception ioe) {
            s_logger.error(ioe.toString(), ioe);
        }
        return null;
    }
}
