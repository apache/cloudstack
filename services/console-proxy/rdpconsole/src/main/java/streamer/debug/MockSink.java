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

import java.util.Arrays;
import java.util.Set;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Direction;
import streamer.Element;
import streamer.Link;
import streamer.SyncLink;

/**
 * Compare incoming packets with expected packets.
 */
public class MockSink extends BaseElement {

    protected ByteBuffer bufs[];
    protected Dumper dumper;

    public MockSink(String id) {
        super(id);
    }

    public MockSink(String id, ByteBuffer bufs[]) {
        super(id);
        this.bufs = bufs;
    }

    public MockSink(String id, ByteBuffer bufs[], Dumper dumper) {
        super(id);
        this.bufs = bufs;
        this.dumper = dumper;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Received buf #" + (packetNumber) + " " + buf + ".");

        if (buf == null)
            return;

        if (packetNumber >= bufs.length)
            throw new AssertionError("[" + this + "] Incoming buffer #" + packetNumber + " is not expected. Number of expected buffers: " + bufs.length
                    + ", unexpected buffer: " + buf + ".");

        // Compare incoming buffer with expected buffer
        ByteBuffer expectedBuf = bufs[packetNumber];
        if (!Arrays.equals(expectedBuf.toByteArray(), buf.toByteArray())) {
            dump(buf, expectedBuf);
            throw new AssertionError("[" + this + "] Incoming buffer #" + packetNumber + " is not equal to expected buffer.\n  Actual buffer: " + buf
                    + ",\n  expected buffer: " + expectedBuf + ".");
        }

        // If expected buffer has metadata, then compare it too
        Set<String> metadataKeys = expectedBuf.getMetadataKeys();
        if (metadataKeys.size() > 0) {
            for (String key : metadataKeys) {
                Object expectedValue = expectedBuf.getMetadata(key);
                Object actualValue = buf.getMetadata(key);
                if (actualValue == null)
                    throw new AssertionError("[" + this + "] Incoming buffer #" + packetNumber + " is not equal to expected buffer in metadata for key \"" + key
                            + "\".\n  Actual metadata value: " + ",\n  expected value: \"" + expectedValue + "\".");

                if (!expectedValue.equals(actualValue))
                    throw new AssertionError("[" + this + "] Incoming buffer #" + packetNumber + " is not equal to expected buffer in metadata for key \"" + key
                            + "\".\n  Actual metadata value: \"" + actualValue + "\",\n  expected value: \"" + expectedValue + "\".");
            }
        }

        if (verbose)
            System.out.println("[" + this + "] INFO: buffers are equal.");

        // Use packetNumber variable to count incoming packets
        packetNumber++;

        buf.unref();
    }

    private void dump(ByteBuffer actualData, ByteBuffer expectedData) {
        if (dumper != null) {
            System.out.println("[" + this + "] INFO: Actual data:");
            dumper.dump(actualData);
            System.out.println("[" + this + "] INFO: Expected data:");
            dumper.dump(expectedData);
        }
    }

    @Override
    protected void onClose() {
        super.onClose();

        if (packetNumber != bufs.length)
            throw new AssertionError("[" + this + "] Number of expected buffers: " + bufs.length + ", number of actual buffers: " + packetNumber + ".");
    }

    @Override
    public String toString() {
        return "MockSink(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {

        Element mockSource = new MockSource("source") {
            {
                bufs = new ByteBuffer[] {new ByteBuffer(new byte[] {1, 1, 2, 3, 4, 5}), new ByteBuffer(new byte[] {2, 1, 2, 3, 4}),
                        new ByteBuffer(new byte[] {3, 1, 2, 3}), new ByteBuffer(new byte[] {4, 1, 2}), new ByteBuffer(new byte[] {5, 1})};
                verbose = true;
                delay = 100;
                numBuffers = bufs.length;
            }
        };

        Element mockSink = new MockSink("sink") {
            {
                bufs = new ByteBuffer[] {new ByteBuffer(new byte[] {1, 1, 2, 3, 4, 5}), new ByteBuffer(new byte[] {2, 1, 2, 3, 4}),
                        new ByteBuffer(new byte[] {3, 1, 2, 3}), new ByteBuffer(new byte[] {4, 1, 2}), new ByteBuffer(new byte[] {5, 1})};
                verbose = true;
            }
        };

        Link link = new SyncLink() {
            {
                verbose = true;
            }
        };

        mockSource.setLink(STDOUT, link, Direction.OUT);
        mockSink.setLink(STDIN, link, Direction.IN);

        link.run();
    }

}
