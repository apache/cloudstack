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

import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.MockSink;
import streamer.MockSource;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import common.ScreenDescription;

public class VncInitializer extends OneTimeSwitch {

    // Pad names
    public static final String CLIENT_SUPPORTED_ENCODINGS_ADAPTER_PAD = "encodings";
    public static final String CLIENT_PIXEL_FORMAT_ADAPTER_PAD = "pixel_format";

    protected byte sharedFlag = RfbConstants.EXCLUSIVE_ACCESS;

    /**
     * Properties of remote screen .
     */
    protected ScreenDescription screen;

    public VncInitializer(String id, boolean shared, ScreenDescription screen) {
        super(id);

        setSharedFlag(shared);
        this.screen = screen;

        declarePads();
    }

    @Override
    protected void declarePads() {
        super.declarePads();
        outputPads.put(CLIENT_SUPPORTED_ENCODINGS_ADAPTER_PAD, null);
        outputPads.put(CLIENT_PIXEL_FORMAT_ADAPTER_PAD, null);
    }

    public ScreenDescription getScreen() {
        return screen;
    }

    public void setScreen(ScreenDescription screen) {
        this.screen = screen;
    }

    public void setSharedFlag(boolean shared) {
        if (shared)
            sharedFlag = RfbConstants.SHARED_ACCESS;
        else
            sharedFlag = RfbConstants.EXCLUSIVE_ACCESS;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Server initialization message is at least 24 bytes long + length of
        // desktop name
        if (!cap(buf, 24, UNLIMITED, link, false))
            return;

        // Read server initialization message
        // Read frame buffer size
        int framebufferWidth = buf.readUnsignedShort();
        int framebufferHeight = buf.readUnsignedShort();

        // Read pixel format
        int bitsPerPixel = buf.readUnsignedByte();
        int depth = buf.readUnsignedByte();

        int bigEndianFlag = buf.readUnsignedByte();
        int trueColorFlag = buf.readUnsignedByte();

        int redMax = buf.readUnsignedShort();
        int greenMax = buf.readUnsignedShort();
        int blueMax = buf.readUnsignedShort();

        int redShift = buf.readUnsignedByte();
        int greenShift = buf.readUnsignedByte();
        int blueShift = buf.readUnsignedByte();

        // Skip padding
        buf.skipBytes(3);

        // Read desktop name
        int length = buf.readSignedInt();

        // Consume exactly $length bytes, push back any extra bytes
        if (!cap(buf, length, length, link, true))
            return;

        String desktopName = buf.readString(length, RfbConstants.US_ASCII_CHARSET);
        buf.unref();
        if (verbose)
            System.out.println("[" + this + "] INFO: Desktop name: \"" + desktopName + "\", bpp: " + bitsPerPixel + ", depth: " + depth + ", screen size: " +
                framebufferWidth + "x" + framebufferHeight + ".");

        // Set screen properties
        screen.setFramebufferSize(framebufferWidth, framebufferHeight);
        screen.setPixelFormat(bitsPerPixel, depth, bigEndianFlag != RfbConstants.LITTLE_ENDIAN, trueColorFlag == RfbConstants.TRUE_COLOR, redMax, greenMax, blueMax,
            redShift, greenShift, blueShift);
        screen.setDesktopName(desktopName);

        // If sever screen has different parameters than ours, then change it
        if (!screen.isRGB888_32_LE()) {
            // Send client pixel format
            sendClientPixelFormat();
        }

        // Send encodings supported by client
        sendSupportedEncodings();

        switchOff();

    }

    @Override
    protected void onStart() {
        ByteBuffer buf = new ByteBuffer(new byte[] {sharedFlag});
        pushDataToOTOut(buf);
    }

    private void sendClientPixelFormat() {
        pushDataToPad(CLIENT_PIXEL_FORMAT_ADAPTER_PAD, new ByteBuffer(0));
    }

    private void sendSupportedEncodings() {
        pushDataToPad(CLIENT_SUPPORTED_ENCODINGS_ADAPTER_PAD, new ByteBuffer(0));
    }

    @Override
    public String toString() {
        return "VncInit(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        final String desktopName = "test";

        Element source = new MockSource("source") {
            {
                bufs = ByteBuffer.convertByteArraysToByteBuffers(
                // Send screen description
                    new byte[] {
                        // Framebuffer width (short)
                        0, (byte)200,
                        // Framebuffer height (short)
                        0, 100,
                        // Bits per pixel
                        32,
                        // Depth,
                        24,
                        // Endianness flag
                        RfbConstants.LITTLE_ENDIAN,
                        // Truecolor flag
                        RfbConstants.TRUE_COLOR,
                        // Red max (short)
                        0, (byte)255,
                        // Green max (short)
                        0, (byte)255,
                        // Blue max (short)
                        0, (byte)255,
                        // Red shift
                        16,
                        // Green shift
                        8,
                        // Blue shift
                        0,
                        // Padding
                        0, 0, 0,
                        // Desktop name length (int)
                        0, 0, 0, 4,
                        // Desktop name ("test", 4 bytes)
                        't', 'e', 's', 't',

                        // Tail
                        1, 2, 3

                    },
                    // Tail packet
                    new byte[] {4, 5, 6});
            }
        };

        ScreenDescription screen = new ScreenDescription();
        final VncInitializer init = new VncInitializer("init", true, screen);
        Element initSink = new MockSink("initSink") {
            {
                // Expect shared flag
                bufs = ByteBuffer.convertByteArraysToByteBuffers(new byte[] {RfbConstants.SHARED_ACCESS});
            }
        };
        Element mainSink = new MockSink("mainSink") {
            {
                // Expect two tail packets
                bufs = ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
            }
        };
        ByteBuffer[] emptyBuf = ByteBuffer.convertByteArraysToByteBuffers(new byte[] {});
        Element encodingsSink = new MockSink("encodings", emptyBuf);
        Element pixelFormatSink = new MockSink("pixel_format", emptyBuf);

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.addAndLink(source, init, mainSink);
        pipeline.add(encodingsSink, pixelFormatSink, initSink);
        pipeline.link("init >otout", "initSink");
        pipeline.link("init >" + CLIENT_SUPPORTED_ENCODINGS_ADAPTER_PAD, "encodings");
        pipeline.link("init >" + CLIENT_PIXEL_FORMAT_ADAPTER_PAD, "pixel_format");

        pipeline.runMainLoop("source", STDOUT, false, false);

        if (!screen.isRGB888_32_LE())
            System.err.println("Screen description was read incorrectly: " + screen + ".");
        if (!desktopName.equals(screen.getDesktopName()))
            System.err.println("Screen desktop name was read incorrectly: \"" + screen.getDesktopName() + "\".");

    }
}
