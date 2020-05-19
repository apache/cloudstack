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
package com.cloud.consoleproxy.vnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.RawHTTP;

public class NoVncClient {
    private static final Logger s_logger = Logger.getLogger(NoVncClient.class);

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    public NoVncClient() {
    }

    public void connectTo(String host, int port, String path, String session, boolean useSSL) throws UnknownHostException, IOException {
        if (port < 0) {
            if (useSSL)
                port = 443;
            else
                port = 80;
        }

        RawHTTP tunnel = new RawHTTP("CONNECT", host, port, path, session, useSSL);
        socket = tunnel.connect();
        setStreams();
    }

    public void connectTo(String host, int port) throws UnknownHostException, IOException {
        // Connect to server
        s_logger.info("Connecting to VNC server " + host + ":" + port + "...");
        socket = new Socket(host, port);
        setStreams();
    }

    private void setStreams() throws IOException {
        this.is = new DataInputStream(this.socket.getInputStream());
        this.os = new DataOutputStream(this.socket.getOutputStream());
    }

    /**
     * Handshake with VNC server.
     */
    public String handshake() throws IOException {

        // Read protocol version
        byte[] buf = new byte[12];
        is.readFully(buf);
        String rfbProtocol = new String(buf);

        // Server should use RFB protocol 3.x
        if (!rfbProtocol.contains(RfbConstants.RFB_PROTOCOL_VERSION_MAJOR)) {
            s_logger.error("Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
            throw new RuntimeException(
                    "Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
        }

        // Proxy that we support RFB 3.3 only
        return RfbConstants.RFB_PROTOCOL_VERSION + "\n";
    }

    /**
     * VNC authentication.
     */
    public byte[] authenticate(String password)
            throws IOException {
        // Read security type
        int authType = is.readInt();

        switch (authType) {
            case RfbConstants.CONNECTION_FAILED: {
                // Server forbids to connect. Read reason and throw exception
                int length = is.readInt();
                byte[] buf = new byte[length];
                is.readFully(buf);
                String reason = new String(buf, RfbConstants.CHARSET);

                s_logger.error("Authentication to VNC server is failed. Reason: " + reason);
                throw new RuntimeException("Authentication to VNC server is failed. Reason: " + reason);
            }

            case RfbConstants.NO_AUTH: {
                // Client can connect without authorization. Nothing to do.
                break;
            }

            case RfbConstants.VNC_AUTH: {
                s_logger.info("VNC server requires password authentication");
                doVncAuth(is, os, password);
                break;
            }

            default:
                s_logger.error("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
                throw new RuntimeException(
                        "Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
        }
        // Since we've taken care of the auth, we tell the client that there's no auth
        // going on
       return new byte[] { 0, 0, 0, 1 };
    }

    /**
     * Encode client password and send it to server.
     */
    private void doVncAuth(DataInputStream in, DataOutputStream out, String password) throws IOException {

        // Read challenge
        byte[] challenge = new byte[16];
        in.readFully(challenge);

        // Encode challenge with password
        byte[] response;
        try {
            response = encodePassword(challenge, password);
        } catch (Exception e) {
            s_logger.error("Cannot encrypt client password to send to server: " + e.getMessage());
            throw new RuntimeException("Cannot encrypt client password to send to server: " + e.getMessage());
        }

        // Send encoded challenge
        out.write(response);
        out.flush();

        // Read security result
        int authResult = in.readInt();

        switch (authResult) {
            case RfbConstants.VNC_AUTH_OK: {
                // Nothing to do
                break;
            }

            case RfbConstants.VNC_AUTH_TOO_MANY:
                s_logger.error("Connection to VNC server failed: too many wrong attempts.");
                throw new RuntimeException("Connection to VNC server failed: too many wrong attempts.");

            case RfbConstants.VNC_AUTH_FAILED:
                s_logger.error("Connection to VNC server failed: wrong password.");
                throw new RuntimeException("Connection to VNC server failed: wrong password.");

            default:
                s_logger.error("Connection to VNC server failed, reason code: " + authResult);
                throw new RuntimeException("Connection to VNC server failed, reason code: " + authResult);
        }
    }

    private byte flipByte(byte b) {
        int b1_8 = (b & 0x1) << 7;
        int b2_7 = (b & 0x2) << 5;
        int b3_6 = (b & 0x4) << 3;
        int b4_5 = (b & 0x8) << 1;
        int b5_4 = (b & 0x10) >>> 1;
        int b6_3 = (b & 0x20) >>> 3;
        int b7_2 = (b & 0x40) >>> 5;
        int b8_1 = (b & 0x80) >>> 7;
        byte c = (byte) (b1_8 | b2_7 | b3_6 | b4_5 | b5_4 | b6_3 | b7_2 | b8_1);
        return c;
    }

    public byte[] encodePassword(byte[] challenge, String password) throws Exception {
        // VNC password consist of up to eight ASCII characters.
        byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 }; // Padding
        byte[] passwordAsciiBytes = password.getBytes(Charset.availableCharsets().get("US-ASCII"));
        System.arraycopy(passwordAsciiBytes, 0, key, 0, Math.min(password.length(), 8));

        // Flip bytes (reverse bits) in key
        for (int i = 0; i < key.length; i++) {
            key[i] = flipByte(key[i]);
        }

        KeySpec desKeySpec = new DESKeySpec(key);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] response = cipher.doFinal(challenge);
        return response;
    }

    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    public void write(byte[] b) throws IOException {
        os.write(b);
    }

}