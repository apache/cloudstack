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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.InetSocketAddress;


import rdpclient.RdpClient;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.SocketWrapper;
import streamer.apr.AprSocketWrapperImpl;
import streamer.ssl.SSLState;

import com.cloud.consoleproxy.rdp.KeysymToKeycode;
import com.cloud.consoleproxy.rdp.RdpBufferedImageCanvas;
import com.cloud.consoleproxy.vnc.FrameBufferCanvas;

import common.AwtKeyEventSource;
import common.AwtMouseEventSource;
import common.ScreenDescription;
import common.SizeChangeListener;

public class ConsoleProxyRdpClient extends ConsoleProxyClientBase {


    private static final int SHIFT_KEY_MASK = 64;
    private static final int CTRL_KEY_MASK = 128;
    private static final int META_KEY_MASK = 256;
    private static final int ALT_KEY_MASK = 512;

    private RdpClient _client;
    private ScreenDescription _screen;
    private SocketWrapper _socket = null;
    private RdpBufferedImageCanvas _canvas = null;

    private Thread _worker;
    private volatile boolean _workerDone = true;
    private volatile long _threadStopTime;

    private AwtMouseEventSource _mouseEventSource = null;
    private AwtKeyEventSource _keyEventSource = null;

    public RdpBufferedImageCanvas getCanvas() {
        return _canvas;
    }

    public void setCanvas(RdpBufferedImageCanvas canvas) {
        _canvas = canvas;
    }

    @Override
    public void onClientConnected() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onClientClose() {
        logger.info("Received client close indication. remove viewer from map.");
        ConsoleProxy.removeViewer(this);
    }

    @Override
    public boolean isHostConnected() {
        //FIXME
        return true;
    }

    @Override
    public boolean isFrontEndAlive() {
        if (_socket != null) {
            if (_workerDone || System.currentTimeMillis() - getClientLastFrontEndActivityTime() > ConsoleProxy.VIEWER_LINGER_SECONDS * 1000) {
                logger.info("Front end has been idle for too long");
                _socket.shutdown();
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers) {
        if (_client == null)
            return;

        updateFrontEndActivityTime();

        KeyEvent keyEvent = map(event, code, modifiers);
        switch (event) {
        case KEY_DOWN:
            _keyEventSource.keyPressed(keyEvent);
            break;

        case KEY_UP:
            _keyEventSource.keyReleased(keyEvent);
            break;

        case KEY_PRESS:
            break;

        default:
            assert (false);
            break;
        }
    }

    private KeyEvent map(InputEventType event, int code, int modifiers) {
        int keycode = KeysymToKeycode.getKeycode(code);
        char keyChar = (char)keycode;

        KeyEvent keyEvent = null;
        int modifier = mapModifier(modifiers);

        switch (event) {
        case KEY_DOWN:
            keyEvent = new KeyEvent(_canvas, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifier, keycode, keyChar);
            break;

        case KEY_UP:
            keyEvent = new KeyEvent(_canvas, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifier, keycode, keyChar);
            break;

        case KEY_PRESS:
            break;

        default:
            assert (false);
            break;
        }
        return keyEvent;
    }

    @Override
    public void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers) {
        if (_client == null)
            return;
        updateFrontEndActivityTime();

        int mousecode = mapMouseButton(code);

        if (event == InputEventType.MOUSE_DOWN) {
            _mouseEventSource.mousePressed(new MouseEvent(_canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), mapMouseDownModifier(code, modifiers), x, y, 1, false,
                    mousecode));
        }

        if (event == InputEventType.MOUSE_UP) {
            _mouseEventSource.mouseReleased((new MouseEvent(_canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), mapMouseUpModifier(code, modifiers), x, y, 1, false,
                    mousecode)));
        }

        if (event == InputEventType.MOUSE_MOVE) {
            _mouseEventSource.mouseMoved(new MouseEvent(_canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), mapModifier(modifiers), x, y, 0, false));
        }
    }

    public int mapMouseDownModifier(int code, int modifiers) {
        int mod = mapModifier(modifiers);
        switch (code) {
        case 0:
            mod = mod | MouseEvent.BUTTON1_DOWN_MASK;
            break;
        case 2:
            mod = mod | MouseEvent.BUTTON3_DOWN_MASK;
            break;
        default:
        }
        return mod;
    }

    public int mapMouseUpModifier(int code, int modifiers) {
        int mod = mapModifier(modifiers);
        switch (code) {
        case 0:
            mod = mod | MouseEvent.BUTTON1_MASK;
            break;
        case 2:
            mod = mod | MouseEvent.BUTTON3_MASK;
            break;
        default:
        }
        return mod;
    }

    private int mapModifier(int modifiers) {
        int mod = 0;

        if ((modifiers & SHIFT_KEY_MASK) != 0)
            mod = mod | InputEvent.SHIFT_DOWN_MASK;

        if ((modifiers & CTRL_KEY_MASK) != 0)
            mod = mod | InputEvent.CTRL_DOWN_MASK;

        if ((modifiers & META_KEY_MASK) != 0)
            mod = mod | InputEvent.META_DOWN_MASK;

        if ((modifiers & ALT_KEY_MASK) != 0)
            mod = mod | InputEvent.ALT_DOWN_MASK;

        return mod;
    }

    public int mapMouseButton(int code) {
        switch (code) {
        case 0:
            return MouseEvent.BUTTON1;
        case 2:
            return MouseEvent.BUTTON3;
        default:
            return MouseEvent.BUTTON2;
        }

    }

    @Override
    public void initClient(final ConsoleProxyClientParam param) {
        if ((System.currentTimeMillis() - _threadStopTime) < 1000) {
            return;
        }

        try {
            int canvasWidth = 1024;
            int canvasHeight = 768;
            setClientParam(param);

            final String host = param.getHypervHost();
            final String password = param.getPassword();
            final String instanceId = param.getClientHostAddress();
            final int port = param.getClientHostPort();

            final SSLState sslState = new SSLState();

            final String username = param.getUsername();
            String name = null;
            String domain = null;
            if (username.contains("\\")) {
                String[] tokens = username.split("\\\\");
                name = tokens[1];
                domain = tokens[0];
            } else {
                name = username;
                domain = "Workgroup";
            }

            _screen = new ScreenDescription();
            _canvas = new RdpBufferedImageCanvas(this, canvasWidth, canvasHeight);
            onFramebufferSizeChange(canvasWidth, canvasHeight);

            _screen.addSizeChangeListener(new SizeChangeListener() {
                @Override
                public void sizeChanged(int width, int height) {
                    if (_canvas != null) {
                        _canvas.setCanvasSize(width, height);
                    }
                }
            });

            logger.info("connecting to instance " + instanceId + " on host " + host);
            _client = new RdpClient("client", host, domain, name, password, instanceId, _screen, _canvas, sslState);

            _mouseEventSource = _client.getMouseEventSource();
            _keyEventSource = _client.getKeyEventSource();

            _worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    _socket = new AprSocketWrapperImpl("socket", sslState);
                    Pipeline pipeline = new PipelineImpl("Client");
                    pipeline.add(_socket, _client);
                    pipeline.link("socket", _client.getId(), "socket");
                    pipeline.validate();

                    InetSocketAddress address = new InetSocketAddress(host, port);
                    ConsoleProxy.ensureRoute(host);

                    try {
                        _workerDone = false;
                        logger.info("Connecting socket to remote server and run main loop(s)");
                        _socket.connect(address);
                    } catch (Exception e) {
                        logger.info(" error occurred in connecting to socket " + e.getMessage());
                    } finally {
                        shutdown();
                    }

                    _threadStopTime = System.currentTimeMillis();
                    logger.info("Receiver thread stopped.");
                    _workerDone = true;
                }
            });
            _worker.setDaemon(true);
            _worker.start();
        } catch (Exception e) {
            _workerDone = true;
            logger.info("error occurred in initializing rdp client " + e.getMessage());
        }
    }

    @Override
    public void closeClient() {
        _workerDone = true;
        shutdown();
    }

    @Override
    protected FrameBufferCanvas getFrameBufferCavas() {
        return _canvas;
    }

    protected void shutdown() {
        if (_socket != null)
            _socket.shutdown();
    }
}
