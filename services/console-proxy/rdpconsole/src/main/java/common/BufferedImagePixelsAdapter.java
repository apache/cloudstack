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
package common;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import org.apache.log4j.Logger;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;

public class BufferedImagePixelsAdapter extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(BufferedImagePixelsAdapter.class);

    public static final String TARGET_X = "x";
    public static final String TARGET_Y = "y";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String RGB888LE32 = "RGB888LE32";
    public static final String PIXEL_FORMAT = "pixel_format";

    protected BufferedImageCanvas canvas;

    public BufferedImagePixelsAdapter(String id, BufferedImageCanvas canvas) {
        super(id);
        this.canvas = canvas;
        declarePads();
    }

    private void declarePads() {
        inputPads.put(STDIN, null);
    }

    @Override
    public String toString() {
        return "Renderer(" + id + ")";
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            s_logger.debug("[" + this + "] INFO: Data received: " + buf + ".");

        int x = (Integer)buf.getMetadata(TARGET_X);
        int y = (Integer)buf.getMetadata(TARGET_Y);
        int rectWidth = (Integer)buf.getMetadata(WIDTH);
        int rectHeight = (Integer)buf.getMetadata(HEIGHT);
        String format = (String)buf.getMetadata(PIXEL_FORMAT);

        int bpp;
        // Support RGB888/32 little endian only
        if (format != null && RGB888LE32.equals(format)) {
            bpp = 4;
            // TODO: support more formats
        } else
            throw new RuntimeException("Unsupported format: " + format + ". Supported formats: " + RGB888LE32 + ".");

        int dataLength = rectWidth * rectHeight * bpp;
        if (!cap(buf, dataLength, dataLength, link, false))
            return;

        // Draw rectangle on offline buffer
        BufferedImage image = canvas.getOfflineImage();

        DataBuffer dataBuf = image.getRaster().getDataBuffer();

        switch (dataBuf.getDataType()) {

        case DataBuffer.TYPE_INT: {

            // Convert array of bytes to array of int's
            int[] intArray = buf.toIntLEArray();

            // We chose RGB888 model, so Raster will use DataBufferInt type
            DataBufferInt dataBuffer = (DataBufferInt)dataBuf;

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Paint rectangle directly on buffer, line by line
            int[] imageBuffer = dataBuffer.getData();

            for (int srcLine = 0, dstLine = y; srcLine < rectHeight && dstLine < imageHeight; srcLine++, dstLine++) {
                try {
                    System.arraycopy(intArray, srcLine * rectWidth, imageBuffer, x + dstLine * imageWidth, rectWidth);
                } catch (IndexOutOfBoundsException e) {
                    s_logger.info("[ignored] copy error",e);
                }
            }
            break;
        }

        default:
            throw new RuntimeException("Unsupported data buffer in buffered image: expected data buffer of type int (DataBufferInt). Actual data buffer type: "
                    + dataBuf.getClass().getSimpleName());
        }

        // Request update of repainted area
        canvas.repaint(x, y, rectWidth, rectHeight);

        buf.unref();
    }

    public static void main(String args[]) {
        System.setProperty("streamer.Element.debug", "true");

        BufferedImageCanvas canvas = new BufferedImageCanvas(4, 4);

        Element renderer = new BufferedImagePixelsAdapter("renderer", canvas);

        byte[] pixels = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

        int[] pixelsLE = new int[] {0x04030201, 0x08070605, 0x0c0b0a09, 0x100f0e0d, 0x04030201, 0x08070605, 0x0c0b0a09, 0x100f0e0d, 0x04030201, 0x08070605,
                0x0c0b0a09, 0x100f0e0d, 0x04030201, 0x08070605, 0x0c0b0a09, 0x100f0e0d};

        ByteBuffer buf = new ByteBuffer(pixels);
        buf.putMetadata(TARGET_X, 0);
        buf.putMetadata(TARGET_Y, 0);
        buf.putMetadata(WIDTH, 4);
        buf.putMetadata(HEIGHT, 4);
        buf.putMetadata(PIXEL_FORMAT, RGB888LE32);

        renderer.handleData(buf, null);

        String actualData = Arrays.toString(((DataBufferInt)canvas.getOfflineImage().getRaster().getDataBuffer()).getData());
        String expectedData = Arrays.toString(pixelsLE);
        if (!actualData.equals(expectedData))
            s_logger.error("Actual image:   " + actualData + "\nExpected image: " + expectedData + ".");

    }

}
