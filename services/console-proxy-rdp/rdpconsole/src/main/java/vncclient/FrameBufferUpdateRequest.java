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
import common.ScreenDescription;

public class FrameBufferUpdateRequest extends BaseElement {
    // TODO: use object with fields instead of raw values in map
    public static final String INCREMENTAL_UPDATE = "incremental";
    public static final String TARGET_X = "x";
    public static final String TARGET_Y = "y";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    protected ScreenDescription screen;

    public FrameBufferUpdateRequest(String id, ScreenDescription screen) {
        super(id);
        this.screen = screen;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        Boolean incremental = (Boolean)buf.getMetadata(INCREMENTAL_UPDATE);
        Integer x = (Integer)buf.getMetadata(TARGET_X);
        Integer y = (Integer)buf.getMetadata(TARGET_Y);
        Integer width = (Integer)buf.getMetadata(WIDTH);
        Integer height = (Integer)buf.getMetadata(HEIGHT);
        buf.unref();

        // Set default values when parameters are not set
        if (incremental == null)
            incremental = false;

        if (x == null)
            x = 0;
        if (y == null)
            y = 0;

        if (width == null)
            width = screen.getFramebufferWidth();
        if (height == null)
            height = screen.getFramebufferHeight();

        ByteBuffer outBuf = new ByteBuffer(10);

        outBuf.writeByte(RfbConstants.CLIENT_FRAMEBUFFER_UPDATE_REQUEST);
        outBuf.writeByte((incremental) ? RfbConstants.FRAMEBUFFER_INCREMENTAL_UPDATE_REQUEST : RfbConstants.FRAMEBUFFER_FULL_UPDATE_REQUEST);
        outBuf.writeShort(x);
        outBuf.writeShort(y);
        outBuf.writeShort(width);
        outBuf.writeShort(height);

        if (verbose) {
            outBuf.putMetadata("sender", this);
            outBuf.putMetadata("dimensions", width + "x" + height + "@" + x + "x" + y);
        }

        pushDataToAllOuts(outBuf);
    }

    public static void main(String args[]) {
        System.setProperty("streamer.Element.debug", "true");

        ScreenDescription screen = new ScreenDescription();
        screen.setFramebufferSize(120, 80);
        Element adapter = new FrameBufferUpdateRequest("renderer", screen);

        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {
            // Request
            RfbConstants.CLIENT_FRAMEBUFFER_UPDATE_REQUEST,
            // Full update (redraw area)
            RfbConstants.FRAMEBUFFER_FULL_UPDATE_REQUEST,
            // X
            0, 1,
            // Y
            0, 2,
            // Width
            0, 3,
            // Height
            0, 4}));

        ByteBuffer buf = new ByteBuffer(new byte[0]);
        buf.putMetadata(TARGET_X, 1);
        buf.putMetadata(TARGET_Y, 2);
        buf.putMetadata(WIDTH, 3);
        buf.putMetadata(HEIGHT, 4);

        Element source = new MockSource("source", new ByteBuffer[] {buf});

        Pipeline pipeline = new PipelineImpl("test");

        pipeline.addAndLink(source, adapter, sink);
        pipeline.runMainLoop("source", STDOUT, false, false);

    }

}
