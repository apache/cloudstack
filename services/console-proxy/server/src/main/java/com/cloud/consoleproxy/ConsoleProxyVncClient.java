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

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;


import com.cloud.consoleproxy.vnc.FrameBufferCanvas;
import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.VncClient;

/**
 *
 * ConsoleProxyVncClient bridges a VNC engine with the front-end AJAX viewer
 *
 */
public class ConsoleProxyVncClient extends ConsoleProxyClientBase {

    private static final int SHIFT_KEY_MASK = 64;
    private static final int CTRL_KEY_MASK = 128;
    private static final int META_KEY_MASK = 256;
    private static final int ALT_KEY_MASK = 512;

    private static final int X11_KEY_SHIFT = 0xffe1;
    private static final int X11_KEY_CTRL = 0xffe3;
    private static final int X11_KEY_ALT = 0xffe9;
    private static final int X11_KEY_META = 0xffe7;

    private VncClient client;
    private Thread worker;
    private volatile boolean workerDone = false;

    private int lastModifierStates = 0;
    private int lastPointerMask = 0;

    public ConsoleProxyVncClient() {
    }

    @Override
    public boolean isHostConnected() {
        if (client != null)
            return client.isHostConnected();

        return false;
    }

    @Override
    public boolean isFrontEndAlive() {
        if (workerDone || System.currentTimeMillis() - getClientLastFrontEndActivityTime() > ConsoleProxy.VIEWER_LINGER_SECONDS * 1000) {
            logger.info("Front end has been idle for too long");
            return false;
        }
        return true;
    }

    @Override
    public void initClient(ConsoleProxyClientParam param) {
        setClientParam(param);

        client = new VncClient(this);
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                String tunnelUrl = getClientParam().getClientTunnelUrl();
                String tunnelSession = getClientParam().getClientTunnelSession();

                try {
                    if (tunnelUrl != null && !tunnelUrl.isEmpty() && tunnelSession != null && !tunnelSession.isEmpty()) {
                        URI uri = new URI(tunnelUrl);
                        logger.info("Connect to VNC server via tunnel. url: " + tunnelUrl + ", session: " + tunnelSession);

                        ConsoleProxy.ensureRoute(uri.getHost());
                        client.connectTo(
                                uri.getHost(), uri.getPort(),
                                uri.getPath() + "?" + uri.getQuery(),
                                tunnelSession, "https".equalsIgnoreCase(uri.getScheme()),
                                getClientHostPassword());
                    } else {
                        logger.info("Connect to VNC server directly. host: " + getClientHostAddress() + ", port: " + getClientHostPort());
                        ConsoleProxy.ensureRoute(getClientHostAddress());
                        client.connectTo(getClientHostAddress(), getClientHostPort(), getClientHostPassword());
                    }
                } catch (UnknownHostException e) {
                    logger.error("Unexpected exception", e);
                } catch (IOException e) {
                    logger.error("Unexpected exception", e);
                } catch (Throwable e) {
                    logger.error("Unexpected exception", e);
                }

                logger.info("Receiver thread stopped.");
                workerDone = true;
                client.getClientListener().onClientClose();
            }
        });

        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void closeClient() {
        workerDone = true;
        if (client != null)
            client.shutdown();
    }

    @Override
    public void onClientConnected() {
    }

    @Override
    public void onClientClose() {
        logger.info("Received client close indication. remove viewer from map.");

        ConsoleProxy.removeViewer(this);
    }

    @Override
    public void onFramebufferUpdate(int x, int y, int w, int h) {
        super.onFramebufferUpdate(x, y, w, h);
        client.requestUpdate(false);
    }

    @Override
    public void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers) {
        if (client == null)
            return;

        updateFrontEndActivityTime();

        switch(event) {
        case KEY_DOWN :
            sendModifierEvents(modifiers);
            client.sendClientKeyboardEvent(RfbConstants.KEY_DOWN, code, 0);
            break;

        case KEY_UP :
            client.sendClientKeyboardEvent(RfbConstants.KEY_UP, code, 0);
            sendModifierEvents(0);
            break;

        case KEY_PRESS :
            break;

        default :
            assert(false);
            break;
        }
    }

    @Override
    public void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers) {
        if (client == null)
            return;

        updateFrontEndActivityTime();

        if (event == InputEventType.MOUSE_DOWN) {
            if (code == 2) {
                lastPointerMask |= 4;
            } else if (code == 0) {
                lastPointerMask |= 1;
            }
        }

        if (event == InputEventType.MOUSE_UP) {
            if (code == 2) {
                lastPointerMask ^= 4;
            } else if (code == 0) {
                lastPointerMask ^= 1;
            }
        }

        if (event == InputEventType.MOUSE_SCROLL) {
            if (code == -1) {
                lastPointerMask = 8;
            } else if (code == 1) {
                lastPointerMask = 16;
            } else if (code == 0) {
                lastPointerMask = 0;
            }
        }

        sendModifierEvents(modifiers);
        client.sendClientMouseEvent(lastPointerMask, x, y, code, modifiers);
        if (lastPointerMask == 0)
            sendModifierEvents(0);
    }

    @Override
    protected FrameBufferCanvas getFrameBufferCavas() {
        if (client != null)
            return client.getFrameBufferCanvas();
        return null;
    }

    private void sendModifierEvents(int modifiers) {
        if ((modifiers & SHIFT_KEY_MASK) != (lastModifierStates & SHIFT_KEY_MASK))
            client.sendClientKeyboardEvent((modifiers & SHIFT_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_SHIFT, 0);

        if((modifiers & CTRL_KEY_MASK) != (lastModifierStates & CTRL_KEY_MASK))
            client.sendClientKeyboardEvent((modifiers & CTRL_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_CTRL, 0);

        if ((modifiers & META_KEY_MASK) != (lastModifierStates & META_KEY_MASK))
            client.sendClientKeyboardEvent((modifiers & META_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_META, 0);

        if((modifiers & ALT_KEY_MASK) != (lastModifierStates & ALT_KEY_MASK))
            client.sendClientKeyboardEvent((modifiers & ALT_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_ALT, 0);

        lastModifierStates = modifiers;
    }
}
