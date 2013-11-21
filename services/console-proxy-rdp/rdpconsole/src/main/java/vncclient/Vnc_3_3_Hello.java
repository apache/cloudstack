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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import streamer.ByteBuffer;
import streamer.InputStreamSource;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.OutputStreamSink;
import streamer.Pipeline;
import streamer.PipelineImpl;

/**
 * VNC server sends hello packet with RFB protocol version, e.g.
 * "RFB 003.007\n". We need to send response packet with supported protocol
 * version, e.g. "RFB 003.003\n".
 */
public class Vnc_3_3_Hello extends OneTimeSwitch {

    public Vnc_3_3_Hello(String id) {
        super(id);
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Initial packet is exactly 12 bytes long
        if (!cap(buf, 12, 12, link, false))
            return;

        // Read protocol version
        String rfbProtocol = new String(buf.data, buf.offset, buf.length, RfbConstants.US_ASCII_CHARSET);
        buf.unref();

        // Server should use RFB protocol 3.x
        if (!rfbProtocol.contains(RfbConstants.RFB_PROTOCOL_VERSION_MAJOR))
            throw new RuntimeException("Cannot handshake with VNC server. Unsupported protocol version: \"" + rfbProtocol + "\".");

        // Send response: we support RFB 3.3 only
        String ourProtocolString = RfbConstants.RFB_PROTOCOL_VERSION + "\n";

        ByteBuffer outBuf = new ByteBuffer(ourProtocolString.getBytes(RfbConstants.US_ASCII_CHARSET));

        if (verbose) {
            outBuf.putMetadata("sender", this);
            outBuf.putMetadata("version", RfbConstants.RFB_PROTOCOL_VERSION);
        }

        pushDataToOTOut(outBuf);

        // Switch off this element from circuit
        switchOff();

    }

    @Override
    public String toString() {
        return "Vnc3.3 Hello(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        InputStream is = new ByteArrayInputStream("RFB 003.007\ntest".getBytes(RfbConstants.US_ASCII_CHARSET));
        ByteArrayOutputStream initOS = new ByteArrayOutputStream();
        ByteArrayOutputStream mainOS = new ByteArrayOutputStream();
        InputStreamSource inputStreamSource = new InputStreamSource("source", is);
        OutputStreamSink outputStreamSink = new OutputStreamSink("mainSink", mainOS);

        Vnc_3_3_Hello hello = new Vnc_3_3_Hello("hello");

        Pipeline pipeline = new PipelineImpl("test");

        pipeline.addAndLink(inputStreamSource, hello, outputStreamSink);
        pipeline.add(new OutputStreamSink("initSink", initOS));

        pipeline.link("hello >" + OneTimeSwitch.OTOUT, "initSink");

        pipeline.runMainLoop("source", STDOUT, false, false);

        String initOut = new String(initOS.toByteArray(), RfbConstants.US_ASCII_CHARSET);
        String mainOut = new String(mainOS.toByteArray(), RfbConstants.US_ASCII_CHARSET);

        if (!"RFB 003.003\n".equals(initOut))
            System.err.println("Unexpected value for hello response: \"" + initOut + "\".");

        if (!"test".equals(mainOut))
            System.err.println("Unexpected value for main data: \"" + mainOut + "\".");

    }
}
