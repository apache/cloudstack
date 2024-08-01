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
import com.cloud.consoleproxy.vnc.network.NioSocketSSLEngineManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.Link;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class VncTLSSecurity implements VncSecurity {

    protected Logger logger = Logger.getLogger(getClass());

    private SSLContext ctx;
    private SSLEngine engine;
    private NioSocketSSLEngineManager manager;

    private final String host;
    private final int port;

    public VncTLSSecurity(String host, int port) {
        this.host = host;
        this.port = port;
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
        ArrayList<String> enabled = new ArrayList<>();
        for (String s : supported) {
            if (s.matches("TLS.*") || s.matches("X509.*")) {
                enabled.add(s);
            }
        }
        engine.setEnabledProtocols(enabled.toArray(new String[0]));

        engine.setEnabledCipherSuites(engine.getSupportedCipherSuites());
    }

    @Override
    public void process(NioSocketHandler socketHandler) {
        logger.info("Processing VNC TLS security");

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
            manager = new NioSocketSSLEngineManager(engine, socketHandler);
            manager.doHandshake();
        } catch(java.lang.Exception e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    public NioSocketSSLEngineManager getSSLEngineManager() {
        return manager;
    }
}
