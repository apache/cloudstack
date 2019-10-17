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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.VncScreenDescription;

public class RawRect extends AbstractRect {
    private static final Logger s_logger = Logger.getLogger(RawRect.class);
    private final int[] buf;

    public RawRect(VncScreenDescription screen, int x, int y, int width, int height, DataInputStream is) throws IOException {
        super(x, y, width, height);

        byte[] bbuf = new byte[width * height * screen.getBytesPerPixel()];
        is.readFully(bbuf);

        // Convert array of bytes to array of int
        int size = width * height;
        buf = new int[size];
        for (int i = 0, j = 0; i < size; i++, j += 4) {
            buf[i] = (bbuf[j + 0] & 0xFF) | ((bbuf[j + 1] & 0xFF) << 8) | ((bbuf[j + 2] & 0xFF) << 16) | ((bbuf[j + 3] & 0xFF) << 24);
        }

    }

    @Override
    public void paint(BufferedImage image, Graphics2D graphics) {

        DataBuffer dataBuf = image.getRaster().getDataBuffer();

        switch (dataBuf.getDataType()) {

        case DataBuffer.TYPE_INT: {
            // We chose RGB888 model, so Raster will use DataBufferInt type
            DataBufferInt dataBuffer = (DataBufferInt)dataBuf;

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Paint rectangle directly on buffer, line by line
            int[] imageBuffer = dataBuffer.getData();
            for (int srcLine = 0, dstLine = y; srcLine < height && dstLine < imageHeight; srcLine++, dstLine++) {
                try {
                    System.arraycopy(buf, srcLine * width, imageBuffer, x + dstLine * imageWidth, width);
                } catch (IndexOutOfBoundsException e) {
                    s_logger.info("[ignored] buffer overflow!?!", e);
                }
            }
            break;
        }

        default:
            throw new RuntimeException("Unsupported data buffer in buffered image: expected data buffer of type int (DataBufferInt). Actual data buffer type: " +
                    dataBuf.getClass().getSimpleName());
        }
    }
}
