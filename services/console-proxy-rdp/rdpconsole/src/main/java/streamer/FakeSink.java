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

public class FakeSink extends BaseElement {

    public FakeSink(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Received buf #" + (packetNumber) + " " + buf + ".");

        if (buf == null)
            return;

        // Use packetNumber variable to count incoming packets
        packetNumber++;

        buf.unref();
    }

    @Override
    public String toString() {
        return "FakeSink(" + id + ")";
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Event received: " + event + ".");

    }

    /**
     * Example.
     */
    public static void main(String args[]) {

        Element sink = new FakeSink("sink") {
            {
                verbose = true;
            }
        };

        byte[] data = new byte[] {1, 2, 3};
        ByteBuffer buf = new ByteBuffer(data);
        sink.setLink(STDIN, new SyncLink(), Direction.IN);
        sink.getLink(STDIN).sendData(buf);

    }

}
