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
package rdpclient.rdp;

import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

/**
 * Server response to MCS Attach User request.
 *
 * Once the User Channel ID has been extracted, the client MUST send an MCS
 * Channel Join Request PDU for the user channel.
 *
 * @see http://msdn.microsoft.com/en-us/library/cc240685.aspx
 */
public class ServerMCSAttachUserConfirmPDU extends OneTimeSwitch {

    public static final int MCS_ATTACH_USER_CONFIRM_PDU = 0xb;

    public static final int INITIATOR_PRESENT = 0x2;

    protected RdpState state;

    public ServerMCSAttachUserConfirmPDU(String id, RdpState state) {
        super(id);
        this.state = state;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        int typeAndFlags = buf.readUnsignedByte();
        int type = typeAndFlags >> 2;
        int flags = typeAndFlags & 0x3;

        if (type != MCS_ATTACH_USER_CONFIRM_PDU)
            throw new RuntimeException("[" + this + "] ERROR: Incorrect type of MCS AttachUserConfirm PDU. Expected value: 11, actual value: " + type + ", data: " + buf + ".");

        if (flags != INITIATOR_PRESENT)
            throw new RuntimeException("Initator field is not present in MCS AttachUserConfirm PDU. Data: " + buf + ".");

        int rtSuccess = buf.readUnsignedByte() >> 4;
        if (rtSuccess != 0)
            throw new RuntimeException("[" + this + "] ERROR: Cannot attach user: request failed. Error code: " + rtSuccess + ", data: " + buf + ".");

        // If the initiator field is present, the client stores the value of the
        // initiator in the User Channel ID store , because the initiator specifies
        // the User Channel ID.
        state.serverUserChannelId = buf.readUnsignedShort() + 1001;

        buf.unref();

        // Next: client MCS Channel Join Request PDU (s)
        switchOff();
    }

    /**
     * Example.
     */
    /**
     * Example.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc240842.aspx
     * @see http://msdn.microsoft.com/en-us/library/cc240500.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        byte[] packet = new byte[] {(byte)0x2E, // MCS user confirm (001011..,
                // 0xb), InitiatorPresent: 1
                // (......01, 0x1)
                (byte)0x00, // RT successful (0000...., 0x0)
                // Initiator: 1001+3 = 1004
                (byte)0x00, (byte)0x03,};

        RdpState rdpState = new RdpState();
        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet, new byte[] {1, 2, 3}));
        Element atachUserConfirm = new ServerMCSAttachUserConfirmPDU("attach_user_confirm", rdpState);
        Element sink = new MockSink("sink");
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, atachUserConfirm, sink, mainSink);
        pipeline.link("source", "attach_user_confirm", "mainSink");
        pipeline.link("attach_user_confirm >" + OTOUT, "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

        if (rdpState.serverUserChannelId != 1004)
            System.err.println("Incorrect user channel ID. Expected value: 1004, actual value: " + rdpState.serverUserChannelId + ".");
    }

}
