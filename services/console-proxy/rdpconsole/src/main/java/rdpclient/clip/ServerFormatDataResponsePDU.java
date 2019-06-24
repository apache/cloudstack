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
import common.adapter.AwtClipboardAdapter;

public class ServerFormatDataResponsePDU extends BaseElement {

    public static final String SERVER_CLIPBOARD_ADAPTER_PAD = "clipboard";

    protected ClipboardState state;

    public ServerFormatDataResponsePDU(String id, ClipboardState state) {
        super(id);
        this.state = state;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        parseData(buf);

        buf.unref();
    }

    protected void parseData(ByteBuffer buf) {

        String content = state.serverRequestedFormat.parseServerResponseAsString(buf);

        // Send response to clipboard adapter
        ByteBuffer outBuf = new ByteBuffer(0);
        outBuf.putMetadata(AwtClipboardAdapter.CLIPBOARD_CONTENT, content);

        pushDataToPad(SERVER_CLIPBOARD_ADAPTER_PAD, outBuf);
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
                0x05, 0x00,  //  CLIPRDR_HEADER::msgType = CB_FORMAT_DATA_RESPONSE (5)
                0x01, 0x00,  //  CLIPRDR_HEADER::msgFlags = 0x0001 = CB_RESPONSE_OK
                0x18, 0x00, 0x00, 0x00,  //  CLIPRDR_HEADER::dataLen = 0x18 = 24 bytes

                0x68, 0x00, 0x65, 0x00, 0x6c, 0x00, 0x6c, 0x00, 0x6f, 0x00, 0x20, 0x00, 0x77, 0x00, 0x6f, 0x00,
                0x72, 0x00, 0x6c, 0x00, 0x64, 0x00, 0x00, 0x00,  //  CLIPRDR_FORMAT_DATA_RESPONSE::requestedFormatData: "hello world"
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element router = new ServerClipRdrChannelRouter("router");
        ClipboardState state = new ClipboardState();
        state.serverRequestedFormat = new ClipboardDataFormat(ClipboardDataFormat.CB_FORMAT_UNICODETEXT, "");
        Element format_data_response = new ServerFormatDataResponsePDU("format_data_response", state);

        ByteBuffer clipboardAdapterPacket = new ByteBuffer(0);
        clipboardAdapterPacket.putMetadata(AwtClipboardAdapter.CLIPBOARD_CONTENT, "hello world");
        Element sink = new MockSink("sink", new ByteBuffer[] {clipboardAdapterPacket});

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, router, format_data_response, sink);
        pipeline.link("source", "router >format_data_response", "format_data_response >clipboard", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

    }

}
