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
package vncclient;

import streamer.PipelineImpl;
import streamer.Queue;
import common.AwtBellAdapter;
import common.AwtCanvasAdapter;
import common.AwtClipboardAdapter;
import common.AwtKeyEventSource;
import common.AwtMouseEventSource;
import common.BufferedImageCanvas;
import common.ScreenDescription;

public class VncClient extends PipelineImpl {

    public VncClient(String id, String password, ScreenDescription screen, BufferedImageCanvas canvas) {
        super(id);
        assembleVNCPipeline(password, screen, canvas);
    }

    private void assembleVNCPipeline(String password, ScreenDescription screen, BufferedImageCanvas canvas) {

        AwtMouseEventSource mouseEventSource = new AwtMouseEventSource("mouse");
        AwtKeyEventSource keyEventSource = new AwtKeyEventSource("keyboard");

        // Subscribe packet sender to various events
        canvas.addMouseListener(mouseEventSource);
        canvas.addMouseMotionListener(mouseEventSource);
        canvas.addKeyListener(keyEventSource);

        add(
        // Handshake

        // RFB protocol version exchanger
        new Vnc_3_3_Hello("hello"),
        // Authenticator
            new Vnc_3_3_Authentication("auth", password),
            // Initializer
            new VncInitializer("init", true, screen),

            new EncodingsMessage("encodings", RfbConstants.SUPPORTED_ENCODINGS_ARRAY),

            new RGB888LE32PixelFormatRequest("pixel_format", screen),

            // Main

            // Packet receiver
            new VncMessageHandler("message_handler", screen),

            new AwtBellAdapter("bell"),

            new AwtClipboardAdapter("clipboard"),

            new AwtCanvasAdapter("pixels", canvas, screen),

            new Queue("queue"),

            new FrameBufferUpdateRequest("fbur", screen),

            new AwtKeyboardEventToVncAdapter("keyboard_adapter"),

            new AwtMouseEventToVncAdapter("mouse_adapter"),

            mouseEventSource, keyEventSource

        );

        // Link handshake elements
        link("IN", "hello", "auth", "init", "message_handler");
        link("hello >otout", "hello< OUT");
        link("auth >otout", "auth< OUT");
        link("init >otout", "init< OUT");
        link("init >encodings", "encodings");
        link("init >pixel_format", "pixel_format");
        link("encodings", "encodings< OUT");
        link("pixel_format", "pixel_format< OUT");

        // Link main elements
        link("message_handler >bell", "bell");
        link("message_handler >clipboard", "clipboard");
        link("message_handler >pixels", "pixels");
        link("message_handler >fbur", "fbur");

        link("fbur", "fbur< queue");
        link("keyboard", "keyboard_adapter", "keyboard< queue");
        link("mouse", "mouse_adapter", "mouse< queue");
        link("queue", "OUT");

    }

}
