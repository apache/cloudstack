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
 * The MCS Channel Join Request PDUs are sent sequentially. The first PDU is
 * sent after receiving the MCS Attach User Confirm PDU and subsequent PDUs are
 * sent after receiving the MCS Channel Join Confirm PDU for the previous
 * request. Sending of the MCS Channel Join Request PDUs MUST continue until all
 * channels have been successfully joined.
 *
 * @see http://msdn.microsoft.com/en-us/library/cc240686.aspx
 */
public class ClientMCSChannelJoinRequestServerMCSChannelConfirmPDUs extends OneTimeSwitch {

    private static final int MCS_CHANNEL_CONFIRM_PDU = 15;

    protected int[] channels;
    protected int channelRequestsSent = 0;

    protected RdpState state;

    public ClientMCSChannelJoinRequestServerMCSChannelConfirmPDUs(String id, int[] channels, RdpState state) {
        super(id);
        this.channels = channels;
        this.state = state;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        // Parse channel confirm response
        int typeAndFlags = buf.readUnsignedByte();
        int type = typeAndFlags >> 2;
        // int flags = typeAndFlags & 0x3;

        if (type != MCS_CHANNEL_CONFIRM_PDU)
            throw new RuntimeException("[" + this + "] ERROR: Incorrect type of MCS AttachUserConfirm PDU. Expected value: 15, actual value: " + type + ", data: " + buf + ".");

        int rtSuccess = buf.readUnsignedByte() >> 4;
        if (rtSuccess != 0)
            throw new RuntimeException("[" + this + "] ERROR: Cannot connect to channel: request failed. Error code: " + rtSuccess + ", channel ID: "
                    + channels[channelRequestsSent - 1]
                            + ", data: " + buf + ".");

        // Initiator and requested fields MAY be ignored, however, the channelId
        // field MUST be examined. If the value of the channelId field does not
        // correspond with the value of the channelId field sent in the previous MCS
        // Channel Join Request PDU the connection SHOULD be dropped.

        // Initiator: 1007 (6+1001)
        // int initiator=buf.readUnsignedShort();
        buf.skipBytes(2);

        // Requested channel
        // int requestedChannel=buf.readUnsignedShort();
        buf.skipBytes(2);

        // Actual channel
        int actualChannel = buf.readUnsignedShort();
        if (actualChannel != channels[channelRequestsSent - 1])
            throw new RuntimeException("Unexpeceted channeld ID returned. Expected channeld ID: " + channels[channelRequestsSent - 1] + ", actual channel ID: "
                    + actualChannel + ", data: " + buf + ".");

        state.channelJoined(actualChannel);

        buf.unref();

        if (channelRequestsSent < channels.length)
            sendChannelRequest(channels[channelRequestsSent++]);
        else
            switchOff();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sendChannelRequest(channels[channelRequestsSent++]);

        // Switch off after receiving response(s)
    }

    private void sendChannelRequest(int channel) {
        ByteBuffer buf = new ByteBuffer(5, true);

        buf.writeByte(0x38); // Channel Join request

        buf.writeShort(state.serverUserChannelId - 1001); // ChannelJoinRequest::initiator: 1004
        buf.writeShort(channel);

        pushDataToOTOut(buf);
    }

    /**
     * Example.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc240834.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
        byte[] clientRequestPacket = new byte[] {
                0x03, 0x00, 0x00, 0x0c,  //  TPKT Header (length = 12 bytes)
                0x02, (byte) 0xf0, (byte) 0x80,  //  X.224 Data TPDU

                // PER encoded (ALIGNED variant of BASIC-PER) PDU contents:
                0x38, 0x00, 0x03, 0x03, (byte) 0xef,

                // 0x38:
                // 0 - --\
                // 0 -   |
                // 1 -   | CHOICE: From DomainMCSPDU select channelJoinRequest (14)
                // 1 -   | of type ChannelJoinRequest
                // 1 -   |
                // 0 - --/
                // 0 - padding
                // 0 - padding

                // 0x00:
                // 0 - --\
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                //       | ChannelJoinRequest::initiator = 0x03 + 1001 = 1004
                // 0x03: |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 1 -   |
                // 1 -   |
                // 0 - --/

                // 0x03:
                // 0 - --\
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 0 -   |
                // 1 -   |
                // 1 -   |
                //       | ChannelJoinRequest::channelId = 0x03ef = 1007
                // 0xef: |
                // 1 -   |
                // 1 -   |
                // 1 -   |
                // 0 -   |
                // 1 -   |
                // 1 -   |
                // 1 -   |
                // 1 - --/
        };

        byte[] serverResponsePacket = new byte[] {
                // MCS Channel Confirm
                (byte)0x3e,

                // result: rt-successful (0)
                (byte)0x00,

                // Initiator: 1007 (6+1001)
                (byte)0x00, (byte)0x06,

                // Requested channel
                (byte)0x03, (byte)0xef,

                // Actual channel
                (byte)0x03, (byte)0xef,
        };
        /* @formatter:on */

        RdpState rdpState = new RdpState();
        rdpState.serverUserChannelId = 1004;
        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(serverResponsePacket, new byte[] {1, 2, 3}));
        Element todo = new ClientMCSChannelJoinRequestServerMCSChannelConfirmPDUs("channels", new int[] {1007}, rdpState);
        Element x224 = new ClientX224DataPDU("x224");
        Element tpkt = new ClientTpkt("tpkt");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(clientRequestPacket));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, todo, x224, tpkt, sink, mainSink);
        pipeline.link("source", "channels", "mainSink");
        pipeline.link("channels >" + OTOUT, "x224", "tpkt", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
