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

import com.cloud.consoleproxy.util.RawHTTP;
import com.cloud.consoleproxy.vnc.RfbConstants;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.spec.KeySpec;
import java.util.Map;

@WebSocket
public class WebSocketHandlerForNovnc extends WebSocketHandler {


    public static final Logger s_logger = Logger.getLogger(WebSocketHandlerForNovnc.class.getSimpleName());
    private Socket vncSocket;
    private DataInputStream is;
    private DataOutputStream os;
    private Session session;
    private String hostPassword;
    private double rfbVersion;

    private static enum VncState {
        SERVER_VERSION_SENT, AUTH_TYPES_SENT, AUTH_RESULT_SENT, UNKNOWN
    }
    private static final byte[] M_VNC_AUTH_OK = new byte[]{0, 0, 0, 0};
    private static final byte[] M_VNC_AUTH_TYE_NOAUTH = new byte[]{01, 01};
    private VncState clientState;

    /**
     * Reverse bits in byte, so least significant bit will be most significant
     * bit. E.g. 01001100 will become 00110010.
     * <p>
     * See also: http://www.vidarholen.net/contents/junk/vnc.html ,
     * http://bytecrafter
     * .blogspot.com/2010/09/des-encryption-as-used-in-vnc.html
     *
     * @param b a byte
     * @return byte in reverse order
     */
    private static byte flipByte(byte b) {
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

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.register(WebSocketHandlerForNovnc.class);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (this.getWebSocketFactory().isUpgradeRequest(request, response)) {
            response.addHeader("Sec-WebSocket-Protocol", "binary");
            if (this.getWebSocketFactory().acceptWebSocket(request, response)) {
                baseRequest.setHandled(true);
                return;
            }

            if (response.isCommitted()) {
                return;
            }
        }
    }

    @OnWebSocketConnect
    public void onConnect(final Session session) throws IOException, InterruptedException {

        s_logger.info("Connect: " + session.getRemoteAddress().getAddress());
        s_logger.info(session.getUpgradeRequest().getRequestURI());

        String queries = ((WebSocketSession) session).getRequestURI().getQuery();
        Map<String, String> queryMap = ConsoleProxyHttpHandlerHelper.getQueryMap(queries);
        String host = queryMap.get("host");
        String portStr = queryMap.get("port");
        String sid = queryMap.get("sid");
        // for xenserver
        String ticket = queryMap.get("ticket");
        String console_url = queryMap.get("consoleurl");
        String console_host_session = queryMap.get("sessionref");
        int port;

        if (host == null || portStr == null || sid == null)
            throw new IllegalArgumentException();

        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            s_logger.warn("Invalid number parameter in query string: " + portStr);
            throw new IllegalArgumentException(e);
        }

        try {
            ConsoleProxyClientParam param = new ConsoleProxyClientParam();
            param.setClientHostAddress(host);
            param.setClientHostPort(port);
            param.setTicket(ticket);
            param.setClientTunnelUrl(console_url);
            param.setClientTunnelSession(console_host_session);
            this.hostPassword = sid;
            proxynoVNC(session, param);
        } catch (Exception e) {

            s_logger.error("Failed to create viewer due to " + e.getMessage(), e);

            String[] content =
                    new String[]{"<html><head></head><body>", "<div id=\"main_panel\" tabindex=\"1\">",
                            "<p>Access is denied for the console session check. Please close the window and retry again</p>", "</div></body></html>"};

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < content.length; i++)
                sb.append(content[i]);

            sendResponseString(session, sb.toString());
            return;
        }
    }


    private void proxynoVNC(Session session, ConsoleProxyClientParam param) {
        this.session = session;
        String tunnelUrl = param.getClientTunnelUrl();
        String tunnelSession = param.getClientTunnelSession();

        try {
            if (tunnelUrl != null && !tunnelUrl.isEmpty() && tunnelSession != null && !tunnelSession.isEmpty()) {
                URI uri = new URI(tunnelUrl);
                s_logger.info("Connect to VNC server via tunnel. url: " + tunnelUrl + ", session: " + tunnelSession);
                ConsoleProxy.ensureRoute(uri.getHost());
                connectTo(
                        uri.getHost(), uri.getPort(),
                        uri.getPath() + "?" + uri.getQuery(),
                        tunnelSession, "https".equalsIgnoreCase(uri.getScheme()),
                        hostPassword);
            } else {
                s_logger.info("Connect to VNC server directly. host: " + param.getClientHostAddress() + ", port: " + param.getClientHostPort());
                vncSocket = new Socket(param.getClientHostAddress(), param.getClientHostPort());
                doConnect(vncSocket);
            }
        } catch (UnknownHostException e) {
            s_logger.error("Unexpected exception", e);
        } catch (IOException e) {
            s_logger.error("Unexpected exception", e);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception", e);
        }
    }

    public void connectTo(String host, int port, String path, String session, boolean useSSL, String sid) throws UnknownHostException, IOException {
        if (port < 0) {
            if (useSSL)
                port = 443;
            else
                port = 80;
        }

        RawHTTP tunnel = new RawHTTP("CONNECT", host, port, path, session, useSSL);
        vncSocket = tunnel.connect();
        doConnect(vncSocket);
    }

    private void startProxyThread() {
        byte[] b = new byte[1500];
        int readBytes = -1;
        while (true) {
            try {
                vncSocket.setSoTimeout(0);
                readBytes = is.read(b);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (readBytes == -1){
                break;
            }

            System.out.printf("read bytes %d\n", readBytes);
            if (readBytes > 0) {
                s_logger.warn("sending bytes of size" + readBytes + " from sender thread");
                sendResponseBytes(session, b, readBytes);
            }
        }
    }

    private void sendResponseString(Session session, String s) {
        try {
            session.getRemote().sendString(s);
        } catch (IOException e) {
            s_logger.error("unable to send response", e);
        }
    }

    private void sendResponseBytes(Session session, byte[] bytes, int size) {
        try {
            session.getRemote().sendBytes(ByteBuffer.wrap(bytes, 0, size));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        s_logger.error("Error in WebSocket Connection : ", t);
    }

    private void doConnect(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        initClient();
        handshakeServer(is, os);
        authenticateServer(is, os);
        new Thread(new Runnable() {
            @Override
            public void run() {
                startProxyThread();
            }
        }).start();
    }

    private void initClient() {
        byte[] buf = (RfbConstants.RFB_WEBSCOKETS + "\n").getBytes();
        sendResponseBytes(session, buf, 12);
        this.clientState = VncState.SERVER_VERSION_SENT;
    }

    private void authenticateServer(DataInputStream is, DataOutputStream os) {
        // Read security type
        int readAuthTypeCount = 0;
        int authType = 0;
        try {
            if (rfbVersion >= 3.7) {
                readAuthTypeCount = is.read();

                if (readAuthTypeCount == 0) {
                    authType = 0;
                } else {
                    authType = is.read();
                }

                os.write(authType);
                os.flush();
            } else {
                authType = is.readInt();
            }

            switch (authType) {
                case RfbConstants.CONNECTION_FAILED: {
                    // Server forbids to connect. Read reason and throw exception
                    int length = is.readInt();
                    byte[] buf = new byte[length];
                    is.readFully(buf);
                    sendResponseBytes(session, buf, length);
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
                    doVncAuth(this.hostPassword);
                    break;
                }

                default:
                    s_logger.error("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
                    throw new RuntimeException("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {

        if (is != null) {
            try {
                is.close();
            } catch (Throwable e) {
                s_logger.info("[ignored]"
                        + "failed to close resource for input: " + e.getLocalizedMessage());
            }
        }

        if (os != null) {
            try {
                os.close();
            } catch (Throwable e) {
                s_logger.info("[ignored]"
                        + "failed to get close resource for output: " + e.getLocalizedMessage());
            }
        }

        if (vncSocket != null) {
            try {
                vncSocket.close();
            } catch (Throwable e) {
                s_logger.info("[ignored]"
                        + "failed to get close resource for socket: " + e.getLocalizedMessage());
            }
        }

    }

    @OnWebSocketFrame
    public void onFrame(Frame f) throws IOException {
        System.out.printf("Frame: %d\n", f.getPayloadLength());
        byte[] data = new byte[f.getPayloadLength()];
        f.getPayload().get(data);

        switch (this.clientState){
            case SERVER_VERSION_SENT: {
                if (f.getPayloadLength() == 12){
                    s_logger.debug("recieved noVNC handshakeServer");
                }
                sendResponseBytes(session, M_VNC_AUTH_TYE_NOAUTH, 2);
                this.clientState=VncState.AUTH_TYPES_SENT;
                break;
            }

            case AUTH_TYPES_SENT: {
                // 1 for send auth type count
                // 1 for sending auth type used i.e no auth required
                s_logger.warn("sending auth types and response");
                sendResponseBytes(session, M_VNC_AUTH_OK, 4);
                this.clientState=VncState.AUTH_RESULT_SENT;
                break;
            }

            case AUTH_RESULT_SENT: {
                os.write(data);
                os.flush();
                break;
            }

            default: {
                os.write(data);
                os.flush();
                break;
            }
        }

        if (f.getType().equals(Frame.Type.CLOSE)){
            shutdown();
        }
    }

    /**
     * Encode client password and send it to server.
     */
    private void doVncAuth(String password) throws IOException {

        // Read challenge
        byte[] challenge = new byte[16];
        is.readFully(challenge);
        // Encode challenge with password
        byte[] response;
        try {
            response = encodePassword(challenge, password);
        } catch (Exception e) {
            s_logger.error("Cannot encrypt client password to send to server: " + e.getMessage());
            throw new RuntimeException("Cannot encrypt client password to send to server: " + e.getMessage());
        }

        // Send encoded challenge
        os.write(response);
        os.flush();

        // Read security result
        int authResult = is.readInt();
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

    /**
     * Handshake with VNC server.
     */
    private void handshakeServer(DataInputStream is, DataOutputStream os) throws IOException {

        // Read protocol version
        byte[] buf = new byte[12];
        is.readFully(buf);
        String rfbProtocol = new String(buf);
        String protocol = rfbProtocol.substring(4,11);
        switch (protocol) {
            case "003.003":
            case "003.006":  // UltraVNC
            case "003.889":  // Apple Remote Desktop
                rfbVersion = 3.3;
                break;
            case "003.007":
                rfbVersion = 3.7;
                break;
            case "003.008":
            case "004.000":  // Intel AMT KVM
            case "004.001":  // RealVNC 4.6
                rfbVersion = 3.8;
                break;
            default:
        }

        // Server should use RFB protocol 3.x
        if (!rfbProtocol.contains(RfbConstants.RFB_PROTOCOL_VERSION_MAJOR)) {
            s_logger.error("Cannot handshakeServer with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
            throw new RuntimeException("Cannot handshakeServer with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
        }

        os.write(buf);
        os.flush();
    }

    /**
     * Encode password using DES encryption with given challenge.
     *
     * @param challenge a random set of bytes.
     * @param password  a password
     * @return DES hash of password and challenge
     */
    public byte[] encodePassword(byte[] challenge, String password) throws Exception {
        // VNC password consist of up to eight ASCII characters.
        byte[] key = {0, 0, 0, 0, 0, 0, 0, 0}; // Padding
        byte[] passwordAsciiBytes = password.getBytes(RfbConstants.CHARSET);
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

    private void initialize() throws IOException {
        s_logger.warn("asking for exclusive access");
        os.writeByte(RfbConstants.EXCLUSIVE_ACCESS);
        os.flush();

        //   getting initializer parameter and sending them to server
        byte[] b = new byte[1500];
        int readBytes = -1;
        vncSocket.setSoTimeout(0);
        readBytes = is.read(b);
        sendResponseBytes(session, b, readBytes);
    }
}
