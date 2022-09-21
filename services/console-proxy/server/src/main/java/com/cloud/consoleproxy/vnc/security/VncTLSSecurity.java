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
package com.cloud.consoleproxy.vnc.security;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.network.NioSocketHandler;
import com.cloud.consoleproxy.vnc.network.SSLEngineManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.Link;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class VncTLSSecurity implements VncSecurity {

    private static final Logger s_logger = Logger.getLogger(VncTLSSecurity.class);

    private SSLContext ctx;
    private SSLEngine engine;
    private SSLEngineManager manager;

    private boolean anon;
    private final String host;
    private final int port;

    public VncTLSSecurity(String host, int port) {
        this.host = host;
        this.port = port;
        this.anon = false;
    }

    private void initGlobal() {
        try {
            ctx = Link.initClientSSLContext();
        } catch (GeneralSecurityException | IOException e) {
            throw new CloudRuntimeException("Unable to initialize SSL context", e);
        }
    }

    private void setParam() {
        engine = ctx.createSSLEngine(this.host, this.port);
        engine.setUseClientMode(true);

        String[] supported = engine.getSupportedProtocols();
        ArrayList<String> enabled = new ArrayList<String>();
        for (int i = 0; i < supported.length; i++)
            if (supported[i].matches("TLS.*"))
                enabled.add(supported[i]);
        engine.setEnabledProtocols(enabled.toArray(new String[0]));

        if (anon) {
            supported = engine.getSupportedCipherSuites();
            enabled = new ArrayList<String>();
            // prefer ECDH over DHE
            for (int i = 0; i < supported.length; i++)
                if (supported[i].matches("TLS_ECDH_anon.*"))
                    enabled.add(supported[i]);
            for (int i = 0; i < supported.length; i++)
                if (supported[i].matches("TLS_DH_anon.*"))
                    enabled.add(supported[i]);
            engine.setEnabledCipherSuites(enabled.toArray(new String[0]));
        } else {
            engine.setEnabledCipherSuites(engine.getSupportedCipherSuites());
        }
    }

    @Override
    public void process(NioSocketHandler socketHandler) {
        s_logger.info("Processing VNC TLS security");

        initGlobal();

        if (manager == null) {
            if (socketHandler.readUnsignedInteger(8) == 0) {
                int result = socketHandler.readUnsignedInteger(32);
                String reason;
                if (result == RfbConstants.VNC_AUTH_FAILED || result == RfbConstants.VNC_AUTH_TOO_MANY) {
                    reason = socketHandler.readString();
                } else {
                    reason = "Authentication failure (protocol error)";
                }
                throw new CloudRuntimeException(reason);
            }
            setParam();
        }

        try {
            manager = new SSLEngineManager(engine, socketHandler);
            manager.doHandshake();
        } catch(java.lang.Exception e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    public SSLEngineManager getSSLEngineManager() {
        return manager;
    }
}
