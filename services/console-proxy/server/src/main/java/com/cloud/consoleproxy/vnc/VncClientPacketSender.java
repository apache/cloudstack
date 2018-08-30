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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.packet.client.ClientPacket;
import com.cloud.consoleproxy.vnc.packet.client.FramebufferUpdateRequestPacket;
import com.cloud.consoleproxy.vnc.packet.client.KeyboardEventPacket;
import com.cloud.consoleproxy.vnc.packet.client.MouseEventPacket;
import com.cloud.consoleproxy.vnc.packet.client.SetEncodingsPacket;
import com.cloud.consoleproxy.vnc.packet.client.SetPixelFormatPacket;

public class VncClientPacketSender implements Runnable, PaintNotificationListener, KeyListener, MouseListener, MouseMotionListener, FrameBufferUpdateListener {
    private static final Logger s_logger = Logger.getLogger(VncClientPacketSender.class);

    // Queue for outgoing packets
    private final BlockingQueue<ClientPacket> queue = new ArrayBlockingQueue<ClientPacket>(30);

    private final DataOutputStream os;
    private final VncScreenDescription screen;
    private final VncClient vncConnection;

    private boolean connectionAlive = true;

    // Don't send update request again until we receive next frame buffer update
    private boolean updateRequestSent = false;

    public VncClientPacketSender(DataOutputStream os, VncScreenDescription screen, VncClient vncConnection) {
        this.os = os;
        this.screen = screen;
        this.vncConnection = vncConnection;

        sendSetPixelFormat();
        sendSetEncodings();
        requestFullScreenUpdate();
    }

    public void sendClientPacket(ClientPacket packet) {
        queue.add(packet);
    }

    @Override
    public void run() {
        try {
            while (connectionAlive) {
                ClientPacket packet = queue.poll(1, TimeUnit.SECONDS);
                if (packet != null) {
                    packet.write(os);
                    os.flush();
                }
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: ", e);
            if (connectionAlive) {
                closeConnection();
            }
        } finally {
            s_logger.info("Sending thread exit processing, shutdown connection");
            vncConnection.shutdown();
        }
    }

    private void sendSetEncodings() {
        queue.add(new SetEncodingsPacket(RfbConstants.SUPPORTED_ENCODINGS_ARRAY));
    }

    private void sendSetPixelFormat() {
        if (!screen.isRGB888_32_LE()) {
            queue.add(new SetPixelFormatPacket(screen, 32, 24, RfbConstants.LITTLE_ENDIAN, RfbConstants.TRUE_COLOR, 255, 255, 255, 16, 8, 0));
        }
    }

    public void closeConnection() {
        connectionAlive = false;
    }

    public void requestFullScreenUpdate() {
        queue.add(new FramebufferUpdateRequestPacket(RfbConstants.FRAMEBUFFER_FULL_UPDATE_REQUEST, 0, 0, screen.getFramebufferWidth(), screen.getFramebufferHeight()));
        updateRequestSent = true;
    }

    @Override
    public void imagePaintedOnScreen() {
        if (!updateRequestSent) {
            queue.add(new FramebufferUpdateRequestPacket(RfbConstants.FRAMEBUFFER_INCREMENTAL_UPDATE_REQUEST, 0, 0, screen.getFramebufferWidth(),
                screen.getFramebufferHeight()));
            updateRequestSent = true;
        }
    }

    @Override
    public void frameBufferPacketReceived() {
        updateRequestSent = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        queue.add(new MouseEventPacket(mapAwtModifiersToVncButtonMask(e.getModifiersEx()), e.getX(), e.getY()));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        queue.add(new MouseEventPacket(mapAwtModifiersToVncButtonMask(e.getModifiersEx()), e.getX(), e.getY()));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Nothing to do
    }

    @Override
    public void mousePressed(MouseEvent e) {
        queue.add(new MouseEventPacket(mapAwtModifiersToVncButtonMask(e.getModifiersEx()), e.getX(), e.getY()));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        queue.add(new MouseEventPacket(mapAwtModifiersToVncButtonMask(e.getModifiersEx()), e.getX(), e.getY()));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing to do
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing to do
    }

    /**
     * Current state of buttons 1 to 8 are represented by bits 0 to 7 of
     * button-mask respectively, 0 meaning up, 1 meaning down (pressed). On a
     * conventional mouse, buttons 1, 2 and 3 correspond to the left, middle and
     * right buttons on the mouse. On a wheel mouse, each step of the wheel
     * upwards is represented by a press and release of button 4, and each step
     * downwards is represented by a press and release of button 5.
     *
     * @param modifiers
     *            extended modifiers from AWT mouse event
     * @return VNC mouse button mask
     */
    public static int mapAwtModifiersToVncButtonMask(int modifiers) {
        int mask =
            (((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) ? 0x1 : 0) | (((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) ? 0x2 : 0) |
                (((modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) ? 0x4 : 0);
        return mask;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        ClientPacket request = new KeyboardEventPacket(RfbConstants.KEY_DOWN, mapAwtKeyToVncKey(e.getKeyCode()));
        queue.add(request);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        ClientPacket request = new KeyboardEventPacket(RfbConstants.KEY_UP, mapAwtKeyToVncKey(e.getKeyCode()));
        queue.add(request);
    }

    private int mapAwtKeyToVncKey(int key) {
        switch (key) {
            case KeyEvent.VK_BACK_SPACE:
                return 0xff08;
            case KeyEvent.VK_TAB:
                return 0xff09;
            case KeyEvent.VK_ENTER:
                return 0xff0d;
            case KeyEvent.VK_ESCAPE:
                return 0xff1b;
            case KeyEvent.VK_INSERT:
                return 0xff63;
            case KeyEvent.VK_DELETE:
                return 0xffff;
            case KeyEvent.VK_HOME:
                return 0xff50;
            case KeyEvent.VK_END:
                return 0xff57;
            case KeyEvent.VK_PAGE_UP:
                return 0xff55;
            case KeyEvent.VK_PAGE_DOWN:
                return 0xff56;
            case KeyEvent.VK_LEFT:
                return 0xff51;
            case KeyEvent.VK_UP:
                return 0xff52;
            case KeyEvent.VK_RIGHT:
                return 0xff53;
            case KeyEvent.VK_DOWN:
                return 0xff54;
            case KeyEvent.VK_F1:
                return 0xffbe;
            case KeyEvent.VK_F2:
                return 0xffbf;
            case KeyEvent.VK_F3:
                return 0xffc0;
            case KeyEvent.VK_F4:
                return 0xffc1;
            case KeyEvent.VK_F5:
                return 0xffc2;
            case KeyEvent.VK_F6:
                return 0xffc3;
            case KeyEvent.VK_F7:
                return 0xffc4;
            case KeyEvent.VK_F8:
                return 0xffc5;
            case KeyEvent.VK_F9:
                return 0xffc6;
            case KeyEvent.VK_F10:
                return 0xffc7;
            case KeyEvent.VK_F11:
                return 0xffc8;
            case KeyEvent.VK_F12:
                return 0xffc9;
            case KeyEvent.VK_SHIFT:
                return 0xffe1;
            case KeyEvent.VK_CONTROL:
                return 0xffe3;
            case KeyEvent.VK_META:
                return 0xffe7;
            case KeyEvent.VK_ALT:
                return 0xffe9;
            case KeyEvent.VK_ALT_GRAPH:
                return 0xffea;
            case KeyEvent.VK_BACK_QUOTE:
                return 0x0060;
        }

        return key;
    }

}
