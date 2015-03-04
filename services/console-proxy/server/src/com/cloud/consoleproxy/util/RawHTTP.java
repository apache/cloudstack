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
package com.cloud.consoleproxy.util;

import org.apache.cloudstack.utils.security.SSLUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//
// This file is originally from XenConsole with modifications
//

/**
 * Send an HTTP CONNECT or PUT request to a XenAPI host with a Session ID,
 * return the connected socket and the Task ID. Used for tunnelling VNC
 * connections and import/export operations.
 */
public final class RawHTTP {
    private static final Logger s_logger = Logger.getLogger(RawHTTP.class);

    private static final Pattern END_PATTERN = Pattern.compile("^\r\n$");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Z_a-z0-9-]+):\\s*(.*)\r\n$");
    private static final Pattern HTTP_PATTERN = Pattern.compile("^HTTP/\\d+\\.\\d+ (\\d*) (.*)\r\n$");

    /**
     * @uml.property  name="command"
     */
    private final String command;
    /**
     * @uml.property  name="host"
     */
    private final String host;
    /**
     * @uml.property  name="port"
     */
    private final int port;
    /**
     * @uml.property  name="path"
     */
    private final String path;
    /**
     * @uml.property  name="session"
     */
    private final String session;
    /**
     * @uml.property  name="useSSL"
     */
    private final boolean useSSL;

    /**
     * @uml.property  name="responseHeaders"
     * @uml.associationEnd  qualifier="group:java.lang.String java.lang.String"
     */
    private final Map<String, String> responseHeaders = new HashMap<String, String>();

    /**
     * @uml.property  name="ic"
     */
    private InputStream ic;
    /**
     * @uml.property  name="oc"
     */
    private OutputStream oc;
    /**
     * @uml.property  name="s"
     */
    private Socket s;

    public InputStream getInputStream() {
        return ic;
    }

    public OutputStream getOutputStream() {
        return oc;
    }

    public Socket getSocket() {
        return s;
    }

    public RawHTTP(String command, String host, int port, String path, String session, boolean useSSL) {
        this.command = command;
        this.host = host;
        this.port = port;
        this.path = path;
        this.session = session;
        this.useSSL = useSSL;
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    private Socket _getSocket() throws IOException {
        if (useSSL) {
            SSLContext context = null;
            try {
                context = SSLUtils.getSSLContext("SunJSSE");
            } catch (NoSuchAlgorithmException e) {
                s_logger.error("Unexpected exception ", e);
            } catch (NoSuchProviderException e) {
                s_logger.error("Unexpected exception ", e);
            }

            if (context == null)
                throw new IOException("Unable to setup SSL context");

            SSLSocket ssl = null;
            try {
                context.init(null, trustAllCerts, new SecureRandom());
                SocketFactory factory = context.getSocketFactory();
                ssl = (SSLSocket)factory.createSocket(host, port);
                ssl.setEnabledProtocols(SSLUtils.getSupportedProtocols(ssl.getEnabledProtocols()));
                /* ssl.setSSLParameters(context.getDefaultSSLParameters()); */
            } catch (IOException e) {
                s_logger.error("IOException: " + e.getMessage(), e);
                throw e;
            } catch (KeyManagementException e) {
                s_logger.error("KeyManagementException: " + e.getMessage(), e);
            }
            return ssl;
        } else {
            return new Socket(host, port);
        }
    }

    public Socket connect() throws IOException {
        String[] headers = makeHeaders();
        s = _getSocket();
        try {
            oc = s.getOutputStream();
            for (String header : headers) {
                oc.write(header.getBytes());
                oc.write("\r\n".getBytes());
            }
            oc.flush();
            ic = s.getInputStream();
            while (true) {
                String line = readline(ic);

                Matcher m = END_PATTERN.matcher(line);
                if (m.matches()) {
                    return s;
                }

                m = HEADER_PATTERN.matcher(line);
                if (m.matches()) {
                    responseHeaders.put(m.group(1), m.group(2));
                    continue;
                }

                m = HTTP_PATTERN.matcher(line);
                if (m.matches()) {
                    String status_code = m.group(1);
                    String reason_phrase = m.group(2);
                    if (!"200".equals(status_code)) {
                        throw new IOException("HTTP status " + status_code + " " + reason_phrase);
                    }
                } else {
                    throw new IOException("Unknown HTTP line " + line);
                }
            }
        } catch (IOException exn) {
            s.close();
            throw exn;
        } catch (RuntimeException exn) {
            s.close();
            throw exn;
        }
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    private String[] makeHeaders() {
        String[] headers = {String.format("%s %s HTTP/1.0", command, path), String.format("Host: %s", host), String.format("Cookie: session_id=%s", session), ""};
        return headers;
    }

    private static String readline(InputStream ic) throws IOException {
        String result = "";
        while (true) {
            try {
                int c = ic.read();

                if (c == -1) {
                    return result;
                }
                result = result + (char)c;
                if (c == 0x0a /* LF */) {
                    return result;
                }
            } catch (IOException e) {
                ic.close();
                throw e;
            }
        }
    }
}
