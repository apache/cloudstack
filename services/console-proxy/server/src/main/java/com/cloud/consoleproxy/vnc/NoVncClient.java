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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.RawHTTP;
import com.cloud.consoleproxy.vnc.network.NioSocket;
import com.cloud.consoleproxy.vnc.network.NioSocketHandler;
import com.cloud.consoleproxy.vnc.network.NioSocketHandlerImpl;
import com.cloud.consoleproxy.vnc.network.NioSocketSSLEngineManager;
import com.cloud.consoleproxy.vnc.security.VncSecurity;
import com.cloud.consoleproxy.vnc.security.VncTLSSecurity;
import com.cloud.consoleproxy.websocket.WebSocketReverseProxy;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.websocket.api.Session;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class NoVncClient {
    private static final Logger s_logger = Logger.getLogger(NoVncClient.class);

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    private NioSocketHandler nioSocketConnection;

    private WebSocketReverseProxy webSocketReverseProxy;

    private boolean flushAfterReceivingNoVNCData = true;
    private boolean securityPhaseCompleted = false;
    private Integer writerLeft = null;

    public NoVncClient() {
    }

    public void connectTo(String host, int port, String path, String session, boolean useSSL) throws IOException {
        if (port < 0) {
            if (useSSL)
                port = 443;
            else
                port = 80;
        }

        RawHTTP tunnel = new RawHTTP("CONNECT", host, port, path, session, useSSL);
        socket = tunnel.connect();
        setTunnelSocketStreams();
    }

    public void connectTo(String host, int port) {
        // Connect to server
        s_logger.info(String.format("Connecting to VNC server %s:%s ...", host, port));
        try {
            NioSocket nioSocket = new NioSocket(host, port);
            this.nioSocketConnection = new NioSocketHandlerImpl(nioSocket);
        } catch (Exception e) {
            s_logger.error(String.format("Cannot create socket to host: %s and port %s: %s", host, port,
                    e.getMessage()), e);
        }
    }

    // VNC over WebSocket connection helpers
    public void connectToWebSocket(String websocketUrl, Session session) throws URISyntaxException {
        webSocketReverseProxy = new WebSocketReverseProxy(new URI(websocketUrl), session);
        webSocketReverseProxy.connect();
    }

    public boolean isVncOverNioSocket() {
        return this.nioSocketConnection != null;
    }

    public boolean isVncOverWebSocketConnection() {
        return webSocketReverseProxy != null;
    }

    public boolean isVncOverWebSocketConnectionOpen() {
        return isVncOverWebSocketConnection() && webSocketReverseProxy.isOpen();
    }

    public boolean isVncOverWebSocketConnectionAlive() {
        return isVncOverWebSocketConnection() && !webSocketReverseProxy.isClosing() && !webSocketReverseProxy.isClosed();
    }

    public void proxyMsgOverWebSocketConnection(ByteBuffer msg) {
        if (isVncOverWebSocketConnection()) {
            webSocketReverseProxy.proxyMsgFromRemoteSessionToEndpoint(msg);
        }
    }

    private void setTunnelSocketStreams() throws IOException {
        this.is = new DataInputStream(this.socket.getInputStream());
        this.os = new DataOutputStream(this.socket.getOutputStream());
    }

    public List<VncSecurity> getVncSecurityStack(int secType, String vmPassword, String host, int port) throws IOException {
        if (secType == RfbConstants.V_ENCRYPT) {
            secType = getVEncryptSecuritySubtype();
        }
        return VncSecurity.getSecurityStack(secType, vmPassword, host, port);
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
            String msg = String.format("Cannot handshake with VNC server. Unsupported protocol version: [%s]",
                    rfbProtocol);
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        // Proxy that we support RFB 3.3 only
        return String.format("%s%s\n", RfbConstants.RFB_PROTOCOL_VERSION_MAJOR,
                RfbConstants.VNC_PROTOCOL_VERSION_MINOR_TUNNEL);
    }

    /**
     * VNC authentication.
     */
    public byte[] authenticateTunnel(String password)
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
        Pair<Boolean, String> pair = processSecurityResultType(authResult);
        boolean success = BooleanUtils.toBoolean(pair.first());
        if (!success) {
            s_logger.error(pair.second());
            throw new CloudRuntimeException(pair.second());
        }
    }

    public static byte flipByte(byte b) {
        int b1_8 = (b & 0x1) << 7;
        int b2_7 = (b & 0x2) << 5;
        int b3_6 = (b & 0x4) << 3;
        int b4_5 = (b & 0x8) << 1;
        int b5_4 = (b & 0x10) >>> 1;
        int b6_3 = (b & 0x20) >>> 3;
        int b7_2 = (b & 0x40) >>> 5;
        int b8_1 = (b & 0x80) >>> 7;
        return (byte) (b1_8 | b2_7 | b3_6 | b4_5 | b5_4 | b6_3 | b7_2 | b8_1);
    }

    public static byte[] encodePassword(byte[] challenge, String password) throws InvalidKeyException,
            InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException {
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

        return cipher.doFinal(challenge);
    }

    private void agreeVEncryptVersion() throws IOException {
        int majorVEncryptVersion = nioSocketConnection.readUnsignedInteger(8);
        int minorVEncryptVersion = nioSocketConnection.readUnsignedInteger(8);
        int vEncryptVersion = (majorVEncryptVersion << 8) | minorVEncryptVersion;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("VEncrypt version offered by the server: " + vEncryptVersion);
        }
        nioSocketConnection.writeUnsignedInteger(8, majorVEncryptVersion);
        if (vEncryptVersion >= 2) {
            nioSocketConnection.writeUnsignedInteger(8, 2);
            nioSocketConnection.flushWriteBuffer();
        } else {
            nioSocketConnection.writeUnsignedInteger(8, 0);
            nioSocketConnection.flushWriteBuffer();
            throw new CloudRuntimeException("Server reported an unsupported VeNCrypt version");
        }
        int ack = nioSocketConnection.readUnsignedInteger(8);
        if (ack != 0) {
            throw new IOException("The VNC server did not agree on the VEncrypt version");
        }
    }

    private int selectVEncryptSubtype() {
        int numberOfSubtypes = nioSocketConnection.readUnsignedInteger(8);
        if (numberOfSubtypes <= 0) {
            throw new CloudRuntimeException("The server reported no VeNCrypt sub-types");
        }
        for (int i = 0; i < numberOfSubtypes; i++) {
            nioSocketConnection.waitForBytesAvailableForReading(4);
            int subtype = nioSocketConnection.readUnsignedInteger(32);
            if (subtype == RfbConstants.V_ENCRYPT_X509_VNC) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Selected VEncrypt subtype " + subtype);
                }
                return subtype;
            }
        }
        throw new CloudRuntimeException("Could not select a VEncrypt subtype");
    }
    /**
     * Obtain the VEncrypt subtype from the VNC server
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#724vencrypt
     */
    protected int getVEncryptSecuritySubtype() throws IOException {
        agreeVEncryptVersion();

        int selectedSubtype = selectVEncryptSubtype();
        nioSocketConnection.writeUnsignedInteger(32, selectedSubtype);
        nioSocketConnection.flushWriteBuffer();

        return selectedSubtype;
    }

    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    public void write(byte[] b) throws IOException {
        if (isVncOverWebSocketConnection()) {
            proxyMsgOverWebSocketConnection(ByteBuffer.wrap(b));
        } else if (isVncOverNioSocket()) {
            writeDataNioSocketConnection(b);
        } else {
            os.write(b);
        }
    }

    private void writeDataAfterSecurityPhase(byte[] data) {
        nioSocketConnection.writeBytes(ByteBuffer.wrap(data), data.length);
        nioSocketConnection.flushWriteBuffer();
        if (writerLeft == null) {
            writerLeft = 3;
            setWaitForNoVnc(false);
        } else if (writerLeft > 0) {
            writerLeft--;
        }
    }

    private void writeDataBeforeSecurityPhase(byte[] data) {
        nioSocketConnection.writeBytes(data, 0, data.length);
        if (flushAfterReceivingNoVNCData) {
            nioSocketConnection.flushWriteBuffer();
            flushAfterReceivingNoVNCData = false;
        }
    }

    protected void writeDataNioSocketConnection(byte[] data) {
        if (securityPhaseCompleted) {
            writeDataAfterSecurityPhase(data);
        } else {
            writeDataBeforeSecurityPhase(data);
        }

        if (!securityPhaseCompleted || (writerLeft != null && writerLeft == 0)) {
            setWaitForNoVnc(false);
        }
    }

    /**
     * Starts the handshake with the VNC server - ProtocolVersion
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#711protocolversion
     */
    public ByteBuffer handshakeProtocolVersion() {
        ByteBuffer verStr = ByteBuffer.allocate(12);

        s_logger.debug("Reading RFB protocol version");

        nioSocketConnection.readBytes(verStr, 12);

        verStr.clear();
        String supportedRfbVersion = RfbConstants.RFB_PROTOCOL_VERSION + "\n";
        verStr.put(supportedRfbVersion.getBytes()).flip();

        setWaitForNoVnc(true);
        return verStr;
    }

    public void waitForNoVNCReply() {
        int cycles = 0;
        while (isWaitForNoVnc()) {
            cycles++;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Waited %d cycles for NoVnc", cycles));
        }
    }

    /**
     * Once the protocol version has been decided, the server and client must agree on the type
     * of security to be used on the connection.
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#712security
     */
    public int handshakeSecurityType() {
        waitForNoVNCReply();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Processing security types message");
        }

        int selectedSecurityType = RfbConstants.CONNECTION_FAILED;

        List<Integer> supportedSecurityTypes = Arrays.asList(RfbConstants.NO_AUTH, RfbConstants.VNC_AUTH,
                RfbConstants.V_ENCRYPT, RfbConstants.V_ENCRYPT_X509_VNC);

        nioSocketConnection.waitForBytesAvailableForReading(1);
        int serverOfferedSecurityTypes = nioSocketConnection.readUnsignedInteger(8);
        if (serverOfferedSecurityTypes == 0) {
            throw new CloudRuntimeException("No security types provided by the server");
        }

        for (int i = 0; i < serverOfferedSecurityTypes; i++) {
            int serverSecurityType = nioSocketConnection.readUnsignedInteger(8);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Server offers security type: %s", serverSecurityType));
            }
            if (supportedSecurityTypes.contains(serverSecurityType)) {
                selectedSecurityType = serverSecurityType;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Selected supported security type: %s", selectedSecurityType));
                }
                break;
            }
        }
        this.flushAfterReceivingNoVNCData = true;
        setWaitForNoVnc(true);
        return selectedSecurityType;
    }

    private final Object lock = new Object();
    public void setWaitForNoVnc(boolean val) {
        synchronized (lock) {
            this.waitForNoVnc = val;
        }
    }

    public boolean isWaitForNoVnc() {
        synchronized (lock) {
            return this.waitForNoVnc;
        }
    }

    private boolean waitForNoVnc = false;

    private Pair<Boolean, String> processSecurityResultType(int authResult) {
        boolean result = false;
        String message;
        switch (authResult) {
            case RfbConstants.VNC_AUTH_OK: {
                result = true;
                message = "Security completed";
                break;
            }
            case RfbConstants.VNC_AUTH_TOO_MANY:
                message = "Connection to VNC server failed: too many wrong attempts.";
                break;
            case RfbConstants.VNC_AUTH_FAILED:
                message = "Connection to VNC server failed: wrong password.";
                break;
            default:
                message = String.format("Connection to VNC server failed, reason code: %s", authResult);
        }
        return new Pair<>(result, message);
    }

    public void processSecurityResultMsg() {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Processing security result message");
        }

        nioSocketConnection.waitForBytesAvailableForReading(1);
        int result = nioSocketConnection.readUnsignedInteger(32);

        Pair<Boolean, String> securityResultType = processSecurityResultType(result);
        boolean success = BooleanUtils.toBoolean(securityResultType.first());
        if (success) {
            securityPhaseCompleted = true;
        } else {
            s_logger.error(securityResultType.second());
            String reason = nioSocketConnection.readString();
            String msg = String.format("%s - Reason: %s", securityResultType.second(), reason);
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    public byte[] readServerInit() {
        return nioSocketConnection.readServerInit();
    }

    public int getNextBytes() {
        return nioSocketConnection.readNextBytes();
    }

    public boolean isTLSConnectionEstablished() {
        return nioSocketConnection.isTLSConnection();
    }

    public void readBytes(byte[] arr, int len) {
        nioSocketConnection.readNextByteArray(arr, len);
    }

    public void processHandshakeSecurityType(int secType, String vmPassword, String host, int port) {
        waitForNoVNCReply();

        try {
            List<VncSecurity> vncSecurityStack = getVncSecurityStack(secType, vmPassword, host, port);
            for (VncSecurity security : vncSecurityStack) {
                security.process(this.nioSocketConnection);
                if (security instanceof VncTLSSecurity) {
                    s_logger.debug("Setting new streams with SSLEngineManger after TLS security has passed");
                    NioSocketSSLEngineManager sslEngineManager = ((VncTLSSecurity) security).getSSLEngineManager();
                    nioSocketConnection.startTLSConnection(sslEngineManager);
                }
            }
        } catch (IOException e) {
            s_logger.error("Error processing handshake security type " + secType, e);
        }
    }
}
