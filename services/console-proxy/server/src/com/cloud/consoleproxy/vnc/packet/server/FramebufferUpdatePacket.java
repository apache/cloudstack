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
package com.cloud.consoleproxy.vnc.packet.server;

import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.ConsoleProxyClientListener;
import com.cloud.consoleproxy.vnc.BufferedImageCanvas;
import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.VncScreenDescription;

public class FramebufferUpdatePacket {

    private final VncScreenDescription screen;
    private final BufferedImageCanvas canvas;
    private final ConsoleProxyClientListener clientListener;

    public FramebufferUpdatePacket(BufferedImageCanvas canvas, VncScreenDescription screen, DataInputStream is, ConsoleProxyClientListener clientListener)
            throws IOException {

        this.screen = screen;
        this.canvas = canvas;
        this.clientListener = clientListener;
        readPacketData(is);
    }

    private void readPacketData(DataInputStream is) throws IOException {
        is.skipBytes(1);// Skip padding

        // Read number of rectangles
        int numberOfRectangles = is.readUnsignedShort();

        // For all rectangles
        for (int i = 0; i < numberOfRectangles; i++) {

            // Read coordinate of rectangle
            int x = is.readUnsignedShort();
            int y = is.readUnsignedShort();
            int width = is.readUnsignedShort();
            int height = is.readUnsignedShort();

            int encodingType = is.readInt();

            // Process rectangle
            Rect rect;
            switch (encodingType) {

                case RfbConstants.ENCODING_RAW: {
                    rect = new RawRect(screen, x, y, width, height, is);
                    break;
                }

                case RfbConstants.ENCODING_COPY_RECT: {
                    rect = new CopyRect(x, y, width, height, is);
                    break;
                }

                case RfbConstants.ENCODING_DESKTOP_SIZE: {
                    rect = new FrameBufferSizeChangeRequest(canvas, width, height);
                    if (this.clientListener != null)
                        this.clientListener.onFramebufferSizeChange(width, height);
                    break;
                }

                default:
                    throw new RuntimeException("Unsupported ecnoding: " + encodingType);
            }

            paint(rect, canvas);

            if (this.clientListener != null)
                this.clientListener.onFramebufferUpdate(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        }

    }

    public void paint(Rect rect, BufferedImageCanvas canvas) {
        // Draw rectangle on offline buffer
        rect.paint(canvas.getOfflineImage(), canvas.getOfflineGraphics());

        // Request update of repainted area
        canvas.repaint(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

}
