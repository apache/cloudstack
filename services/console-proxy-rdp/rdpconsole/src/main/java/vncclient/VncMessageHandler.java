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

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.MockSink;
import streamer.MockSource;
import streamer.Pipeline;
import streamer.PipelineImpl;
import common.BitmapOrder;
import common.BitmapRectangle;
import common.CopyRectOrder;
import common.ScreenDescription;

public class VncMessageHandler extends BaseElement {
    protected ScreenDescription screen = null;

    // Pad names
    public static final String SERVER_BELL_ADAPTER_PAD = "bell";
    public static final String SERVER_CLIPBOARD_ADAPTER_PAD = "clipboard";
    public static final String PIXEL_ADAPTER_PAD = "pixels";
    public static final String FRAME_BUFFER_UPDATE_REQUEST_ADAPTER_PAD = "fbur";

    // Keys for metadata
    public static final String CLIPBOARD_CONTENT = "content";
    public static final String TARGET_X = "x";
    public static final String TARGET_Y = "y";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SOURCE_X = "srcX";
    public static final String SOURCE_Y = "srcY";
    public static final String PIXEL_FORMAT = "pixel_format";

    private static final String NUM_OF_PROCESSED_RECTANGLES = "rects";
    private static final String SAVED_CURSOR_POSITION = "cursor";

    // Pixel format: RGB888 LE 32
    public static final String RGB888LE32 = "RGB888LE32";

    public VncMessageHandler(String id, ScreenDescription screen) {
        super(id);
        this.screen = screen;
        declarePads();
    }

    private void declarePads() {
        outputPads.put(SERVER_BELL_ADAPTER_PAD, null);
        outputPads.put(SERVER_BELL_ADAPTER_PAD, null);
        outputPads.put(SERVER_CLIPBOARD_ADAPTER_PAD, null);
        outputPads.put(PIXEL_ADAPTER_PAD, null);
        outputPads.put(FRAME_BUFFER_UPDATE_REQUEST_ADAPTER_PAD, null);

        inputPads.put("stdin", null);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        try {
            if (verbose)
                System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

            if (!cap(buf, 1, UNLIMITED, link, false))
                return;

            // Read server message type
            int messageType = buf.readUnsignedByte();

            // Invoke packet handler by packet type.
            switch (messageType) {

                case RfbConstants.SERVER_FRAMEBUFFER_UPDATE: {
                    // Handle frame buffer update
                    if (!handleFBU(buf, link))
                        return;

                    // Frame buffer update is received and fully processed, send request for
                    // another frame buffer update to server.
                    sendFBUR();

                    break;
                }

                case RfbConstants.SERVER_BELL: {
                    if (!handleBell(buf, link))
                        return;
                    break;
                }

                case RfbConstants.SERVER_CUT_TEXT: {
                    if (!handleClipboard(buf, link))
                        return;
                    break;
                }

                default:
                    // TODO: allow to extend functionality
                    throw new RuntimeException("Unknown server packet type: " + messageType + ".");
            }

            // Cut tail, if any
            cap(buf, 0, 0, link, true);
        } finally {

            // Return processed buffer back to pool
            buf.unref();
        }
    }

    private boolean handleClipboard(ByteBuffer buf, Link link) {
        if (!cap(buf, 3 + 4, UNLIMITED, link, true))
            return false;

        // Skip padding
        buf.skipBytes(3);

        // Read text length
        int length = buf.readSignedInt();

        // We need full string to parse it
        if (!cap(buf, length, UNLIMITED, link, true))
            return false;

        String content = buf.readString(length, RfbConstants.US_ASCII_CHARSET);

        // Send content in metadata
        ByteBuffer outBuf = new ByteBuffer(0);
        outBuf.putMetadata(CLIPBOARD_CONTENT, content);

        pushDataToPad(SERVER_CLIPBOARD_ADAPTER_PAD, outBuf);

        return true;
    }

    private boolean handleBell(ByteBuffer buf, Link link) {
        // Send empty packet to bell adapter to produce bell
        pushDataToPad(SERVER_BELL_ADAPTER_PAD, new ByteBuffer(0));

        return true;
    }

    // FIXME: this method is too complex
    private boolean handleFBU(ByteBuffer buf, Link link) {

        // We need at least 3 bytes here, 1 - padding, 2 - number of rectangles
        if (!cap(buf, 3, UNLIMITED, link, true))
            return false;

        buf.skipBytes(1);// Skip padding

        // Read number of rectangles
        int numberOfRectangles = buf.readUnsignedShort();

        if (verbose)
            System.out.println("[" + this + "] INFO: Frame buffer update. Number of rectangles: " + numberOfRectangles + ".");

        // Each rectangle must have header at least, header length is 12 bytes.
        if (!cap(buf, 12 * numberOfRectangles, UNLIMITED, link, true))
            return false;

        // For all rectangles

        // Restore saved point, to avoid flickering and performance problems when
        // frame buffer update is split between few incoming packets.
        int numberOfProcessedRectangles = (buf.getMetadata(NUM_OF_PROCESSED_RECTANGLES) != null) ? (Integer)buf.getMetadata(NUM_OF_PROCESSED_RECTANGLES) : 0;
        if (buf.getMetadata(SAVED_CURSOR_POSITION) != null)
            buf.cursor = (Integer)buf.getMetadata(SAVED_CURSOR_POSITION);

        if (verbose && numberOfProcessedRectangles > 0)
            System.out.println("[" + this + "] INFO: Restarting from saved point. Number of already processed rectangles: " + numberOfRectangles + ", cursor: " +
                buf.cursor + ".");

        // For all new rectangles
        for (int i = numberOfProcessedRectangles; i < numberOfRectangles; i++) {

            // We need coordinates of rectangle (2x4 bytes) and encoding type (4
            // bytes)
            if (!cap(buf, 12, UNLIMITED, link, true))
                return false;

            // Read coordinates of rectangle
            int x = buf.readUnsignedShort();
            int y = buf.readUnsignedShort();
            int width = buf.readUnsignedShort();
            int height = buf.readUnsignedShort();

            // Read rectangle encoding
            int encodingType = buf.readSignedInt();

            // Process rectangle
            switch (encodingType) {

                case RfbConstants.ENCODING_RAW: {
                    if (!handleRawRectangle(buf, link, x, y, width, height))
                        return false;
                    break;
                }

                case RfbConstants.ENCODING_COPY_RECT: {
                    if (!handleCopyRect(buf, link, x, y, width, height))
                        return false;
                    break;
                }

                case RfbConstants.ENCODING_DESKTOP_SIZE: {
                    if (!handleScreenSizeChangeRect(buf, link, x, y, width, height))
                        return false;
                    break;
                }

                default:
                    // TODO: allow to extend functionality
                    throw new RuntimeException("Unsupported ecnoding: " + encodingType + ".");
            }

            // Update information about processed rectangles to avoid handling of same
            // rectangle multiple times.
            // TODO: push back partial rectangle only instead
            buf.putMetadata(NUM_OF_PROCESSED_RECTANGLES, ++numberOfProcessedRectangles);
            buf.putMetadata(SAVED_CURSOR_POSITION, buf.cursor);
        }

        return true;
    }

    private boolean handleScreenSizeChangeRect(ByteBuffer buf, Link link, int x, int y, int width, int height) {
        // Remote screen size is changed
        if (verbose)
            System.out.println("[" + this + "] INFO: Screen size rect. Width: " + width + ", height: " + height + ".");

        screen.setFramebufferSize(width, height);

        return true;
    }

    private boolean handleCopyRect(ByteBuffer buf, Link link, int x, int y, int width, int height) {
        // Copy rectangle from one part of screen to another.
        // Areas may overlap. Antialiasing may cause visible artifacts.

        // We need 4 bytes with coordinates of source rectangle
        if (!cap(buf, 4, UNLIMITED, link, true))
            return false;

        CopyRectOrder order = new CopyRectOrder();

        order.srcX = buf.readUnsignedShort();
        order.srcY = buf.readUnsignedShort();
        order.x = x;
        order.y = y;
        order.width = width;
        order.height = height;

        if (verbose)
            System.out.println("[" + this + "] INFO: Copy rect. X: " + x + ", y: " + y + ", width: " + width + ", height: " + height + ", srcX: " + order.srcX +
                ", srcY: " + order.srcY + ".");

        pushDataToPad(PIXEL_ADAPTER_PAD, new ByteBuffer(order));

        return true;
    }

    private boolean handleRawRectangle(ByteBuffer buf, Link link, int x, int y, int width, int height) {
        // Raw rectangle is just array of pixels to draw on screen.
        int rectDataLength = width * height * screen.getBytesPerPixel();

        // We need at least rectDataLength bytes. Extra bytes may contain other
        // rectangles.
        if (!cap(buf, rectDataLength, UNLIMITED, link, true))
            return false;

        if (verbose)
            System.out.println("[" + this + "] INFO: Raw rect. X: " + x + ", y: " + y + ", width: " + width + ", height: " + height + ", data length: " + rectDataLength +
                ".");

        BitmapRectangle rectangle = new BitmapRectangle();
        rectangle.x = x;
        rectangle.y = y;
        rectangle.width = width;
        rectangle.height = height;
        rectangle.bufferWidth = width;
        rectangle.bufferHeight = height;
        rectangle.bitmapDataStream = buf.readBytes(rectDataLength);
        rectangle.colorDepth = screen.getColorDeph();

        BitmapOrder order = new BitmapOrder();
        order.rectangles = new BitmapRectangle[] {rectangle};

        pushDataToPad(PIXEL_ADAPTER_PAD, new ByteBuffer(order));
        return true;
    }

    @Override
    public void onStart() {
        // Send Frame Buffer Update request
        sendFBUR();
    }

    private void sendFBUR() {
        ByteBuffer buf = new ByteBuffer(0);
        buf.putMetadata("incremental", true);
        pushDataToPad(FRAME_BUFFER_UPDATE_REQUEST_ADAPTER_PAD, buf);
    }

    @Override
    public String toString() {
        return "VNCMessageHandler(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String[] args) {

        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        Element source = new MockSource("source") {
            {
                // Split messages at random boundaries to check "pushback" logic
                bufs =
                    ByteBuffer.convertByteArraysToByteBuffers(new byte[] {
                        // Message type: server bell
                        RfbConstants.SERVER_BELL,

                        // Message type: clipboard text
                        RfbConstants.SERVER_CUT_TEXT,
                        // Padding
                        0, 0, 0,
                        // Length (test)
                        0, 0, 0, 4,

                    }, new byte[] {
                        // Clipboard text
                        't', 'e', 's', 't',

                        // Message type: frame buffer update
                        RfbConstants.SERVER_FRAMEBUFFER_UPDATE,
                        // Padding
                        0,
                        // Number of rectangles
                        0, 3,},

                    new byte[] {

                        // x, y, width, height: 0x0@4x4
                        0, 0, 0, 0, 0, 4, 0, 4,
                        // Encoding: desktop size
                        (byte)((RfbConstants.ENCODING_DESKTOP_SIZE >> 24) & 0xff), (byte)((RfbConstants.ENCODING_DESKTOP_SIZE >> 16) & 0xff),
                        (byte)((RfbConstants.ENCODING_DESKTOP_SIZE >> 8) & 0xff), (byte)((RfbConstants.ENCODING_DESKTOP_SIZE >> 0) & 0xff),},

                    new byte[] {

                        // x, y, width, height: 0x0@4x4
                        0, 0, 0, 0, 0, 4, 0, 4,
                        // Encoding: raw rect
                        (byte)((RfbConstants.ENCODING_RAW >> 24) & 0xff), (byte)((RfbConstants.ENCODING_RAW >> 16) & 0xff),
                        (byte)((RfbConstants.ENCODING_RAW >> 8) & 0xff), (byte)((RfbConstants.ENCODING_RAW >> 0) & 0xff),
                        // Raw pixel data 4x4x1 bpp
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,}, new byte[] {11, 12, 13, 14, 15, 16,

                        // x, y, width, height: 0x0@2x2
                        0, 0, 0, 0, 0, 2, 0, 2,
                        // Encoding: copy rect
                        (byte)((RfbConstants.ENCODING_COPY_RECT >> 24) & 0xff), (byte)((RfbConstants.ENCODING_COPY_RECT >> 16) & 0xff),
                        (byte)((RfbConstants.ENCODING_COPY_RECT >> 8) & 0xff), (byte)((RfbConstants.ENCODING_COPY_RECT >> 0) & 0xff),
                        // srcX, srcY: 2x2
                        0, 2, 0, 2,});
            }
        };

        ScreenDescription screen = new ScreenDescription() {
            {
                this.bytesPerPixel = 1;
            }
        };

        final Element handler = new VncMessageHandler("handler", screen);

        ByteBuffer[] emptyBuf = ByteBuffer.convertByteArraysToByteBuffers(new byte[] {});
        Element fburSink = new MockSink("fbur", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {}, new byte[] {}));
        Element bellSink = new MockSink("bell", emptyBuf);
        Element clipboardSink = new MockSink("clipboard", emptyBuf);
        Element desktopSizeChangeSink = new MockSink("desktop_size", emptyBuf);
        Element pixelsSink = new MockSink("pixels", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,}));
        Element copyRectSink = new MockSink("copy_rect", emptyBuf);

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.addAndLink(source, handler);
        pipeline.add(fburSink, bellSink, clipboardSink, desktopSizeChangeSink, pixelsSink, copyRectSink);

        pipeline.link("handler >" + FRAME_BUFFER_UPDATE_REQUEST_ADAPTER_PAD, "fbur");
        pipeline.link("handler >" + SERVER_BELL_ADAPTER_PAD, "bell");
        pipeline.link("handler >" + SERVER_CLIPBOARD_ADAPTER_PAD, "clipboard");
        pipeline.link("handler >" + PIXEL_ADAPTER_PAD, "pixels");

        pipeline.runMainLoop("source", STDOUT, false, false);

    }

}
