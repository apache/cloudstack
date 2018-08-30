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

import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.cloud.consoleproxy.ConsoleProxyClientListener;
import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.RawHTTP;
import com.cloud.consoleproxy.vnc.packet.client.KeyboardEventPacket;
import com.cloud.consoleproxy.vnc.packet.client.MouseEventPacket;

public class VncClient {
    private static final Logger s_logger = Logger.getLogger(VncClient.class);

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    private final VncScreenDescription screen = new VncScreenDescription();

    private VncClientPacketSender sender;
    private VncServerPacketReceiver receiver;

    private boolean noUI = false;
    private ConsoleProxyClientListener clientListener = null;

    public static void main(String args[]) {
        if (args.length < 3) {
            printHelpMessage();
            System.exit(1);
        }

        String host = args[0];
        String port = args[1];
        String password = args[2];

        try {
            new VncClient(host, Integer.parseInt(port), password, false, null);
        } catch (NumberFormatException e) {
            s_logger.error("Incorrect VNC server port number: " + port + ".");
            System.exit(1);
        } catch (UnknownHostException e) {
            s_logger.error("Incorrect VNC server host name: " + host + ".");
            System.exit(1);
        } catch (IOException e) {
            s_logger.error("Cannot communicate with VNC server: " + e.getMessage());
            System.exit(1);
        } catch (Throwable e) {
            s_logger.error("An error happened: " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    private static void printHelpMessage() {
        /* LOG */s_logger.info("Usage: HOST PORT PASSWORD.");
    }

    public VncClient(ConsoleProxyClientListener clientListener) {
        noUI = true;
        this.clientListener = clientListener;
    }

    public VncClient(String host, int port, String password, boolean noUI, ConsoleProxyClientListener clientListener) throws UnknownHostException, IOException {

        this.noUI = noUI;
        this.clientListener = clientListener;
        connectTo(host, port, password);
    }

    public void shutdown() {
        if (sender != null)
            sender.closeConnection();

        if (receiver != null)
            receiver.closeConnection();

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

        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable e) {
                s_logger.info("[ignored]"
                        + "failed to get close resource for socket: " + e.getLocalizedMessage());
            }
        }
    }

    public ConsoleProxyClientListener getClientListener() {
        return clientListener;
    }

    public void connectTo(String host, int port, String path, String session, boolean useSSL, String sid) throws UnknownHostException, IOException {
        if (port < 0) {
            if (useSSL)
                port = 443;
            else
                port = 80;
        }

        RawHTTP tunnel = new RawHTTP("CONNECT", host, port, path, session, useSSL);
        socket = tunnel.connect();
        doConnect(sid);
    }

    public void connectTo(String host, int port, String password) throws UnknownHostException, IOException {
        // Connect to server
        s_logger.info("Connecting to VNC server " + host + ":" + port + "...");
        socket = new Socket(host, port);
        doConnect(password);
    }

    private void doConnect(String password) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());

        // Initialize connection
        handshake();
        authenticate(password);
        initialize();

        s_logger.info("Connecting to VNC server succeeded, start session");

        // Run client-to-server packet sender
        sender = new VncClientPacketSender(os, screen, this);

        // Create buffered image canvas
        BufferedImageCanvas canvas = new BufferedImageCanvas(sender, screen.getFramebufferWidth(), screen.getFramebufferHeight());

        // Subscribe packet sender to various events
        canvas.addMouseListener(sender);
        canvas.addMouseMotionListener(sender);
        canvas.addKeyListener(sender);

        Frame frame = null;
        if (!noUI)
            frame = createVncClientMainWindow(canvas, screen.getDesktopName());

        new Thread(sender).start();

        // Run server-to-client packet receiver
        receiver = new VncServerPacketReceiver(is, canvas, screen, this, sender, clientListener);
        try {
            receiver.run();
        } finally {
            if (frame != null) {
                frame.setVisible(false);
                frame.dispose();
            }
            shutdown();
        }
    }

    private Frame createVncClientMainWindow(BufferedImageCanvas canvas, String title) {
        // Create AWT windows
        final Frame frame = new Frame(title + " - VNCle");

        // Use scrolling pane to support screens, which are larger than ours
        ScrollPane scroller = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        scroller.add(canvas);
        scroller.setSize(screen.getFramebufferWidth(), screen.getFramebufferHeight());

        frame.add(scroller);
        frame.pack();
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                frame.setVisible(false);
                shutdown();
            }
        });

        return frame;
    }

    /**
     * Handshake with VNC server.
     */
    private void handshake() throws IOException {

        // Read protocol version
        byte[] buf = new byte[12];
        is.readFully(buf);
        String rfbProtocol = new String(buf);

        // Server should use RFB protocol 3.x
        if (!rfbProtocol.contains(RfbConstants.RFB_PROTOCOL_VERSION_MAJOR)) {
            s_logger.error("Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
            throw new RuntimeException("Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");
        }

        // Send response: we support RFB 3.3 only
        String ourProtocolString = RfbConstants.RFB_PROTOCOL_VERSION + "\n";
        os.write(ourProtocolString.getBytes());
        os.flush();
    }

    /**
     * VNC authentication.
     */
    private void authenticate(String password) throws IOException {
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
                doVncAuth(password);
                break;
            }

            default:
                s_logger.error("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
                throw new RuntimeException("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
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
     * Encode password using DES encryption with given challenge.
     *
     * @param challenge
     *            a random set of bytes.
     * @param password
     *            a password
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

    /**
     * Reverse bits in byte, so least significant bit will be most significant
     * bit. E.g. 01001100 will become 00110010.
     *
     * See also: http://www.vidarholen.net/contents/junk/vnc.html ,
     * http://bytecrafter
     * .blogspot.com/2010/09/des-encryption-as-used-in-vnc.html
     *
     * @param b
     *            a byte
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
        byte c = (byte)(b1_8 | b2_7 | b3_6 | b4_5 | b5_4 | b6_3 | b7_2 | b8_1);
        return c;
    }

    private void initialize() throws IOException {
        // Send client initialization message
        {
            // Send shared flag
            os.writeByte(RfbConstants.EXCLUSIVE_ACCESS);
            os.flush();
        }

        // Read server initialization message
        {
            // Read frame buffer size
            int framebufferWidth = is.readUnsignedShort();
            int framebufferHeight = is.readUnsignedShort();
            screen.setFramebufferSize(framebufferWidth, framebufferHeight);
            if (clientListener != null)
                clientListener.onFramebufferSizeChange(framebufferWidth, framebufferHeight);
        }

        // Read pixel format
        {
            int bitsPerPixel = is.readUnsignedByte();
            int depth = is.readUnsignedByte();

            int bigEndianFlag = is.readUnsignedByte();
            int trueColorFlag = is.readUnsignedByte();

            int redMax = is.readUnsignedShort();
            int greenMax = is.readUnsignedShort();
            int blueMax = is.readUnsignedShort();

            int redShift = is.readUnsignedByte();
            int greenShift = is.readUnsignedByte();
            int blueShift = is.readUnsignedByte();

            // Skip padding
            is.skipBytes(3);

            screen.setPixelFormat(bitsPerPixel, depth, bigEndianFlag, trueColorFlag, redMax, greenMax, blueMax, redShift, greenShift, blueShift);
        }

        // Read desktop name
        {
            int length = is.readInt();
            byte buf[] = new byte[length];
            is.readFully(buf);
            String desktopName = new String(buf, RfbConstants.CHARSET);
            screen.setDesktopName(desktopName);
        }
    }

    public FrameBufferCanvas getFrameBufferCanvas() {
        if (receiver != null)
            return receiver.getCanvas();

        return null;
    }

    public void requestUpdate(boolean fullUpdate) {
        if (fullUpdate)
            sender.requestFullScreenUpdate();
        else
            sender.imagePaintedOnScreen();
    }

    public void sendClientKeyboardEvent(int event, int code, int modifiers) {
        sender.sendClientPacket(new KeyboardEventPacket(event, code));
    }

    public void sendClientMouseEvent(int event, int x, int y, int code, int modifiers) {
        sender.sendClientPacket(new MouseEventPacket(event, x, y));
    }

    public boolean isHostConnected() {
        return receiver != null && receiver.isConnectionAlive();
    }
}
