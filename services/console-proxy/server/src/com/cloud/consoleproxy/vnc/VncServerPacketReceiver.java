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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.ConsoleProxyClientListener;
import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.packet.server.FramebufferUpdatePacket;
import com.cloud.consoleproxy.vnc.packet.server.ServerCutText;

public class VncServerPacketReceiver implements Runnable {
    private static final Logger s_logger = Logger.getLogger(VncServerPacketReceiver.class);

    private final VncScreenDescription screen;
    private BufferedImageCanvas canvas;
    private DataInputStream is;

    private boolean connectionAlive = true;
    private VncClient vncConnection;
    private final FrameBufferUpdateListener fburListener;
    private final ConsoleProxyClientListener clientListener;

    public VncServerPacketReceiver(DataInputStream is, BufferedImageCanvas canvas, VncScreenDescription screen, VncClient vncConnection,
            FrameBufferUpdateListener fburListener, ConsoleProxyClientListener clientListener) {
        this.screen = screen;
        this.canvas = canvas;
        this.is = is;
        this.vncConnection = vncConnection;
        this.fburListener = fburListener;
        this.clientListener = clientListener;
    }

    public BufferedImageCanvas getCanvas() {
        return canvas;
    }

    @Override
    public void run() {
        try {
            while (connectionAlive) {

                // Read server message type
                int messageType = is.readUnsignedByte();

                // Invoke packet handler by packet type.
                switch (messageType) {

                    case RfbConstants.SERVER_FRAMEBUFFER_UPDATE: {
                        // Notify sender that frame buffer update is received,
                        // so it can send another frame buffer update request
                        fburListener.frameBufferPacketReceived();
                        // Handle frame buffer update
                        new FramebufferUpdatePacket(canvas, screen, is, clientListener);
                        break;
                    }

                    case RfbConstants.SERVER_BELL: {
                        serverBell();
                        break;
                    }

                    case RfbConstants.SERVER_CUT_TEXT: {
                        serverCutText(is);
                        break;
                    }

                    default:
                        throw new RuntimeException("Unknown server packet type: " + messageType + ".");
                }
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: ", e);
            if (connectionAlive) {
                closeConnection();
            }
        } finally {
            s_logger.info("Receiving thread exit processing, shutdown connection");
            vncConnection.shutdown();
        }
    }

    public void closeConnection() {
        connectionAlive = false;
    }

    public boolean isConnectionAlive() {
        return connectionAlive;
    }

    /**
     * Handle server bell packet.
     */
    private void serverBell() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Handle packet with server clip-board.
     */
    private void serverCutText(DataInputStream is) throws IOException {
        ServerCutText clipboardContent = new ServerCutText(is);
        StringSelection contents = new StringSelection(clipboardContent.getContent());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

        s_logger.info("Server clipboard buffer: " + clipboardContent.getContent());
    }
}
