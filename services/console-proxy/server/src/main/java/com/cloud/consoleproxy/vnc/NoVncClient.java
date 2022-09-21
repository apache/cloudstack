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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.RawHTTP;
import com.cloud.consoleproxy.vnc.network.NioSocket;
import com.cloud.consoleproxy.vnc.network.NioSocketHandler;
import com.cloud.consoleproxy.vnc.network.NioSocketHandlerImpl;
import com.cloud.consoleproxy.vnc.network.SSLEngineManager;
import com.cloud.consoleproxy.vnc.security.VncSecurity;
import com.cloud.consoleproxy.vnc.security.VncTLSSecurity;
import com.cloud.consoleproxy.websocket.WebSocketReverseProxy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.extensions.Frame;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class NoVncClient {
    private static final Logger s_logger = Logger.getLogger(NoVncClient.class);

    private Socket tunnelSocket;
    private DataInputStream tunnelInputStream;
    private DataOutputStream tunnelOutputStream;

    private NioSocketHandler socketConnection;

    private WebSocketReverseProxy webSocketReverseProxy;

    private boolean flushAfterReceivingNoVNCData = true;
    private boolean securityPhaseCompleted = false;
    private Integer writerLeft = null;

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
        tunnelSocket = tunnel.connect();
        setTunnelSocketStreams();
    }

    public void connectTo(String host, int port) throws UnknownHostException, IOException {
        // Connect to server
        s_logger.info("Connecting to VNC server " + host + ":" + port + "...");
        try {
            NioSocket nioSocket = new NioSocket(host, port);
            this.socketConnection = new NioSocketHandlerImpl(nioSocket);
        } catch (Exception e) {
            s_logger.error("Cannot create socket to host: " + host + " and port " + port, e);
        }
    }

    // VNC over WebSocket connection helpers
    public void connectToWebSocket(String websocketUrl, Session session) throws URISyntaxException {
        webSocketReverseProxy = new WebSocketReverseProxy(new URI(websocketUrl), session);
        webSocketReverseProxy.connect();
    }

    public boolean isVncOverTunnel() {
        return this.tunnelSocket != null;
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
        this.tunnelInputStream = new DataInputStream(this.tunnelSocket.getInputStream());
        this.tunnelOutputStream = new DataOutputStream(this.tunnelSocket.getOutputStream());
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
        tunnelInputStream.readFully(buf);
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
    public byte[] authenticateTunnel(String password)
            throws IOException {
        // Read security type
        int authType = tunnelInputStream.readInt();

        switch (authType) {
            case RfbConstants.CONNECTION_FAILED: {
                // Server forbids to connect. Read reason and throw exception
                int length = tunnelInputStream.readInt();
                byte[] buf = new byte[length];
                tunnelInputStream.readFully(buf);
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
                doVncAuth(tunnelInputStream, tunnelOutputStream, password);
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

    public static byte flipByte(byte b) {
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

    public static byte[] encodePassword(byte[] challenge, String password) throws Exception {
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

    /**
     * Decide the RFB protocol version with the VNC server
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#711protocolversion
     */
    protected String handshakeProtocolVersion(RemoteEndpoint clientRemote) throws IOException {
        // Read protocol version
        byte[] buf = new byte[12];
        tunnelInputStream.readFully(buf);
        String rfbProtocol = new String(buf);

        // Server should use RFB protocol 3.x
        if (!rfbProtocol.contains(RfbConstants.RFB_PROTOCOL_VERSION_MAJOR)) {
            s_logger.error("Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
            throw new RuntimeException(
                    "Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
        }
        tunnelOutputStream.write(buf);
        return RfbConstants.RFB_PROTOCOL_VERSION + "\n";
    }

    /**
     * Agree on the security type with the VNC server
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#712security
     * @return list of the security types to be processed
     */
    protected List<VncSecurity> handshakeSecurityTypes(RemoteEndpoint clientRemote, String vmPassword,
                                                       String host, int port) throws IOException {
        int securityType = selectFromTheServerOfferedSecurityTypes();

        // Inform the server about our decision
        this.tunnelOutputStream.writeByte(securityType);

        byte[] numberTypesToClient = new byte[] { 1, (byte) securityType };
        clientRemote.sendBytes(ByteBuffer.wrap(numberTypesToClient, 0, 2));

        if (securityType == RfbConstants.V_ENCRYPT) {
            securityType = getVEncryptSecuritySubtype();
        }
        return VncSecurity.getSecurityStack(securityType, vmPassword, host, port);
    }

    /**
     * Obtain the VEncrypt subtype from the VNC server
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#724vencrypt
     */
    protected int getVEncryptSecuritySubtype() throws IOException {
        int majorVEncryptVersion = socketConnection.readUnsignedInteger(8);
        int minorVEncryptVersion = socketConnection.readUnsignedInteger(8);
        int vEncryptVersion = (majorVEncryptVersion << 8) | minorVEncryptVersion;
        s_logger.debug("VEncrypt version: " + vEncryptVersion);
        socketConnection.writeUnsignedInteger(8, majorVEncryptVersion);
        if (vEncryptVersion >= 0x0002) {
            socketConnection.writeUnsignedInteger(8, 2);
            socketConnection.flushWriteBuffer();
        } else {
            socketConnection.writeUnsignedInteger(8, 0);
            socketConnection.flushWriteBuffer();
            throw new CloudRuntimeException("Server reported an unsupported VeNCrypt version");
        }
        int ack = socketConnection.readUnsignedInteger(8);
        if (ack != 0) {
            throw new IOException("The VNC server did not agree on the VEncrypt version");
        }

        int numberOfSubtypes = socketConnection.readUnsignedInteger(8);
        if (numberOfSubtypes <= 0) {
            throw new CloudRuntimeException("The server reported no VeNCrypt sub-types");
        }
        int selectedSubtype = 0;
        for (int i = 0; i < numberOfSubtypes; i++) {
            while (!socketConnection.checkIfBytesAreAvailableForReading(4)) {
                s_logger.trace("Waiting for vEncrypt subtype");
            }
            int subtype = socketConnection.readUnsignedInteger(32);
            if (subtype == RfbConstants.V_ENCRYPT_X509_VNC) {
                selectedSubtype = subtype;
                break;
            }
        }

        s_logger.info("Selected VEncrypt subtype " + selectedSubtype);
        socketConnection.writeUnsignedInteger(32, selectedSubtype);
        socketConnection.flushWriteBuffer();

        return selectedSubtype;
    }

    private int selectFromTheServerOfferedSecurityTypes() throws IOException {
        int numberOfSecurityTypes = tunnelInputStream.readByte();
        if (numberOfSecurityTypes == 0) {
            int reasonLength = tunnelInputStream.readInt();
            byte[] reasonBuffer = new byte[reasonLength];
            tunnelInputStream.readFully(reasonBuffer);
            String reason = new String(reasonBuffer);
            String errMsg = "No security type provided by the VNC server, reason: " + reason;
            s_logger.error(errMsg);
            throw new IOException(errMsg);
        }

        for (int i = 0; i < numberOfSecurityTypes; i++) {
            int securityType = tunnelInputStream.readByte();
            if (securityType != 0 && VncSecurity.supportedSecurityTypes.contains(securityType)) {
                s_logger.info("Selected the security type: " + securityType);
                return securityType;
            }
        }
        throw new IOException("Could not select a supported or valid security type from the offered by the server");
    }

    /**
     * VNC authentication.
     */
    public void processSecurityResult(String password)
            throws IOException {
        // Read security result
        int authResult = this.tunnelInputStream.readInt();

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

    public int read(byte[] b) throws IOException {
        return tunnelInputStream.read(b);
    }

    public void write(byte[] b) throws IOException {
        if (isVncOverWebSocketConnection()) {
            proxyMsgOverWebSocketConnection(ByteBuffer.wrap(b));
        } else if (!isVncOverTunnel()) {
            this.socketConnection.writeBytes(b, 0, b.length);
        } else {
            tunnelOutputStream.write(b);
        }
    }

    public void writeFrame(Frame frame) {
        byte[] data = new byte[frame.getPayloadLength()];
        frame.getPayload().get(data);

        if (securityPhaseCompleted) {
            socketConnection.writeBytes(ByteBuffer.wrap(data), data.length);
            socketConnection.flushWriteBuffer();
            if (writerLeft == null) {
                writerLeft = 3;
                setWaitForNoVnc(false);
            } else if (writerLeft > 0) {
                writerLeft--;
            }
        } else {
            socketConnection.writeBytes(data, 0, data.length);
            if (flushAfterReceivingNoVNCData) {
                socketConnection.flushWriteBuffer();
                flushAfterReceivingNoVNCData = false;
            }
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
        int majorVersion;
        int minorVersion;

        s_logger.debug("Reading RFB protocol version");

        socketConnection.readBytes(verStr, 12);

        if ((new String(verStr.array())).matches("RFB \\d{3}\\.\\d{3}\\n")) {
            majorVersion = Integer.parseInt((new String(verStr.array())).substring(4,7));
            minorVersion = Integer.parseInt((new String(verStr.array())).substring(8,11));
        } else {
            throw new CloudRuntimeException("Reading version failed: not an RFB server?");
        }

        s_logger.info("Server supports RFB protocol version " + majorVersion + "." + minorVersion);

        verStr.clear();
        verStr.put(String.format("RFB %03d.%03d\n", majorVersion, minorVersion).getBytes()).flip();

        s_logger.info("Using RFB protocol version " + majorVersion + "." + minorVersion);
        setWaitForNoVnc(true);
        return verStr;
    }

    /**
     * Once the protocol version has been decided, the server and client must agree on the type
     * of security to be used on the connection.
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#712security
     */
    public int handshakeSecurityType() {
        while (isWaitForNoVnc()) {
            s_logger.trace("Waiting for noVNC msg received");
        }
        s_logger.debug("Processing security types message");

        int secType = RfbConstants.CONNECTION_FAILED;

        List<Integer> secTypes = Arrays.asList(1, 2, 19, 261);

        while (!socketConnection.checkIfBytesAreAvailableForReading(1)) {
            s_logger.trace("Waiting for inStream to be ready");
        }
        int nServerSecTypes = socketConnection.readUnsignedInteger(8);
        if (nServerSecTypes == 0) {
            throw new CloudRuntimeException("No security types provided by the server");
        }

        Iterator<Integer> j;
        for (int i = 0; i < nServerSecTypes; i++) {
            int serverSecType = socketConnection.readUnsignedInteger(8);
            s_logger.debug("Server offers security type " + serverSecType);

            /*
             * Use the first type sent by server which matches client's type.
             * It means server's order specifies priority.
             */
            if (secType == RfbConstants.CONNECTION_FAILED) {
                for (j = secTypes.iterator(); j.hasNext(); ) {
                    int refType = (Integer) j.next();
                    if (refType == serverSecType) {
                        secType = refType;
                        break;
                    }
                }
            }
        }
        this.flushAfterReceivingNoVNCData = true;
        setWaitForNoVnc(true);
        return secType;
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

    public void processSecurityResultMsg(int secType) {
        s_logger.info("Processing security result message");
        int result;

        if (secType == RfbConstants.NO_AUTH) {
            result = RfbConstants.VNC_AUTH_OK;
        } else {
            while (!socketConnection.checkIfBytesAreAvailableForReading(1)) {
                s_logger.trace("Waiting for inStream");
            }
            result = socketConnection.readUnsignedInteger(32);
        }

        switch (result) {
            case RfbConstants.VNC_AUTH_OK:
                s_logger.info("Security completed");
                handleSecurityCompleted();
                return;
            case RfbConstants.VNC_AUTH_FAILED:
                s_logger.debug("auth failed");
                break;
            case RfbConstants.VNC_AUTH_TOO_MANY:
                s_logger.debug("auth failed - too many tries");
                break;
            default:
                throw new CloudRuntimeException("Unknown security result from server");
        }
        String reason = socketConnection.readString();
        throw new CloudRuntimeException(reason);
    }

    private void handleSecurityCompleted() {
        s_logger.info("Security completed");
        this.securityPhaseCompleted = true;
    }

    public byte[] readServerInit() {
        return socketConnection.readServerInit();
    }

    public int getNextBytes() {
        return socketConnection.readNextBytes();
    }

    public boolean isTLSConnectionEstablished() {
        return socketConnection.isTLSConnection();
    }

    public void readBytes(byte[] arr, int len) {
        socketConnection.readNextByteArray(arr, len);
    }

    public void processHandshakeSecurityType(int secType, String vmPassword, String host, int port) {
        while (isWaitForNoVnc()) {
            s_logger.trace("Waiting for noVNC msg received");
        }

        try {
            List<VncSecurity> vncSecurityStack = getVncSecurityStack(secType, vmPassword, host, port);
            for (VncSecurity security : vncSecurityStack) {
                security.process(this.socketConnection);
                if (security instanceof VncTLSSecurity) {
                    s_logger.debug("Setting new streams with SSLEngineManger after TLS security has passed");
                    SSLEngineManager sslEngineManager = ((VncTLSSecurity) security).getSSLEngineManager();
                    socketConnection.startTLSConnection(sslEngineManager);
                }
            }
        } catch (IOException e) {
            s_logger.error("Error processing handshake security type " + secType, e);
        }

        processSecurityResultMsg(secType);
    }
}