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

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLServerSocket;

import com.cloud.consoleproxy.util.Logger;
import com.sun.net.httpserver.HttpServer;

public class ConsoleProxyBaseServerFactoryImpl implements ConsoleProxyServerFactory {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyBaseServerFactoryImpl.class);

    @Override
    public void init(byte[] ksBits, String ksPassword) {
    }

    @Override
    public HttpServer createHttpServerInstance(int port) throws IOException {
        if (s_logger.isInfoEnabled())
            s_logger.info("create HTTP server instance at port: " + port);
        return HttpServer.create(new InetSocketAddress(port), 5);
    }

    @Override
    public SSLServerSocket createSSLServerSocket(int port) throws IOException {
        if (s_logger.isInfoEnabled())
            s_logger.info("SSL server socket is not supported in ConsoleProxyBaseServerFactoryImpl");

        return null;
    }
}
