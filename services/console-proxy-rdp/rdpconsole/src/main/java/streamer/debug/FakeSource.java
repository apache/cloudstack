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
package streamer.debug;

import org.apache.log4j.Logger;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Direction;
import streamer.Element;
import streamer.Event;
import streamer.Link;
import streamer.SyncLink;

public class FakeSource extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(FakeSource.class);

    /**
     * Delay for null packets in poll method when blocking is requested, in
     * milliseconds.
     */
    protected long delay = SyncLink.STANDARD_DELAY_FOR_EMPTY_PACKET;

    public FakeSource(String id) {
        super(id);
    }

    @Override
    public void poll(boolean block) {
        if (numBuffers > 0 && packetNumber >= numBuffers) {
            // Close stream when limit of packets is reached
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
            return;
        }

        // Prepare new packet
        ByteBuffer buf = initializeData();

        // Push it to output(s)
        if (buf != null)
            pushDataToAllOuts(buf);

        // Make slight delay when blocking input was requested (to avoid
        // consuming of 100% in parent loop)
        if (block)
            delay();

    }

    /**
     * Make slight delay. Should be used when blocking input is requested in pull
     * mode, but null packed was returned by input.
     */
    protected void delay() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            s_logger.info("[ignored] interupted while creating latency", e);
        }
    }

    /**
     * Initialize data.
     */
    public ByteBuffer initializeData() {
        ByteBuffer buf = new ByteBuffer(incommingBufLength);

        // Set first byte of package to it sequance number
        buf.data[buf.offset] = (byte)(packetNumber % 128);

        // Initialize rest of bytes with sequential values, which are
        // corresponding with their position in byte buffer
        for (int i = buf.offset + 1; i < buf.length; i++)
            buf.data[i] = (byte)(i % 128);

        buf.putMetadata(ByteBuffer.SEQUENCE_NUMBER, packetNumber);
        buf.putMetadata("src", id);

        return buf;
    }

    @Override
    public String toString() {
        return "FakeSource(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {

        Element fakeSource = new FakeSource("source 3/10/100") {
            {
                verbose = true;
                incommingBufLength = 3;
                numBuffers = 10;
                delay = 100;
            }
        };

        Element fakeSink = new FakeSink("sink") {
            {
                verbose = true;
            }
        };

        Element fakeSink2 = new FakeSink("sink2") {
            {
                verbose = true;
            }
        };

        Link link = new SyncLink();

        fakeSource.setLink(STDOUT, link, Direction.OUT);
        fakeSink.setLink(STDIN, link, Direction.IN);

        Link link2 = new SyncLink();

        fakeSource.setLink("out2", link2, Direction.OUT);
        fakeSink2.setLink(STDIN, link2, Direction.IN);

        link.sendEvent(Event.STREAM_START, Direction.IN);
        link.run();

    }

}
