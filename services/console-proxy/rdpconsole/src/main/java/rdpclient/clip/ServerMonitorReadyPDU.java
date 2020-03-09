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
package rdpclient.clip;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

public class ServerMonitorReadyPDU extends BaseElement {

    protected ClipboardState state;

    public ServerMonitorReadyPDU(String id, ClipboardState state) {
        super(id);
        this.state = state;
    }

    // 0x01, 0x00, // CLIPRDR_HEADER::msgType = CB_MONITOR_READY (1)
    // 0x00, 0x00, // CLIPRDR_HEADER::msgFlags = 0
    // 0x00, 0x00, 0x00, 0x00, // CLIPRDR_HEADER::dataLen = 0 bytes

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        state.serverReady = true;

        buf.unref();

    }

    /**
     * Example.
     */
    public static void main(String[] args) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
        byte[] packet = new byte[] {
                0x01, 0x00,  //  CLIPRDR_HEADER::msgType = CB_MONITOR_READY (1)
                0x00, 0x00,  //  CLIPRDR_HEADER::msgFlags = 0
                0x00, 0x00, 0x00, 0x00,  //  CLIPRDR_HEADER::dataLen = 0 bytes
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element router = new ServerClipRdrChannelRouter("router");
        ClipboardState state = new ClipboardState();
        Element monitor_ready = new ServerMonitorReadyPDU("monitor_ready", state);
        Element sink = new MockSink("sink", new ByteBuffer[] {});

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, router, monitor_ready, sink);
        pipeline.link("source", "router >monitor_ready", "monitor_ready", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

        // Check state
        if (!state.serverReady)
            throw new RuntimeException("Server monitor ready packet parsed incorrectly.");

    }

}
