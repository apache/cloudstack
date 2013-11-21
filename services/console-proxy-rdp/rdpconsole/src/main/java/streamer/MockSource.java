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
package streamer;

public class MockSource extends FakeSource {

    protected ByteBuffer bufs[] = null;

    public MockSource(String id) {
        super(id);
    }

    public MockSource(String id, ByteBuffer bufs[]) {
        super(id);
        this.bufs = bufs;
    }

    /**
     * Initialize data.
     */
    @Override
    public ByteBuffer initializeData() {
        if (packetNumber >= bufs.length) {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
            return null;
        }

        ByteBuffer buf = bufs[packetNumber];

        buf.putMetadata(ByteBuffer.SEQUENCE_NUMBER, packetNumber);
        return buf;
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Event received: " + event + ".");

    }

    @Override
    public String toString() {
        return "MockSource(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {

        Element mockSource = new MockSource("source") {
            {
                this.bufs =
                    new ByteBuffer[] {new ByteBuffer(new byte[] {1, 1, 2, 3, 4, 5}), new ByteBuffer(new byte[] {2, 1, 2, 3, 4}), new ByteBuffer(new byte[] {3, 1, 2, 3}),
                        new ByteBuffer(new byte[] {4, 1, 2}), new ByteBuffer(new byte[] {5, 1})};
                this.verbose = true;
                this.delay = 100;
                // this.numBuffers = this.bufs.length;
            }
        };

        Element fakeSink = new FakeSink("sink") {
            {
                this.verbose = true;
            }
        };

        Link link = new SyncLink();

        mockSource.setLink(STDOUT, link, Direction.OUT);
        fakeSink.setLink(STDIN, link, Direction.IN);

        link.run();
    }
}
