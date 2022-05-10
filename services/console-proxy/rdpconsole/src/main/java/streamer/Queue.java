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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import streamer.debug.FakeSink;
import streamer.debug.FakeSource;

/**
 * Message queue for safe transfer of packets between threads.
 */
public class Queue extends BaseElement {

    protected LinkedBlockingQueue<ByteBuffer> queue = new LinkedBlockingQueue<ByteBuffer>();

    public Queue(String id) {
        super(id);
    }

    @Override
    public void poll(boolean block) {
        try {
            ByteBuffer buf = null;
            if (block) {
                buf = queue.take();
            } else {
                buf = queue.poll(100, TimeUnit.MILLISECONDS);
            }

            if (buf != null)
                pushDataToAllOuts(buf);

        } catch (Exception e) {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
            closeQueue();
        }
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Put incoming data into queue
        try {
            queue.put(buf);
        } catch (Exception e) {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.IN);
            closeQueue();
        }
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        switch (event) {
        case LINK_SWITCH_TO_PULL_MODE:
            // Do not propagate this event, because this element is boundary between
            // threads
            break;
        default:
            super.handleEvent(event, direction);
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        closeQueue();
    }

    private void closeQueue() {
        queue.clear();
        queue.add(null);
        // Drop queue to indicate that upstream is closed.
        // May produce NPE in poll().
        queue = null;
    }

    @Override
    public String toString() {
        return "Queue(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");

        Element source1 = new FakeSource("source1") {
            {
                delay = 100;
                numBuffers = 10;
                incomingBufLength = 10;
            }
        };

        Element source2 = new FakeSource("source2") {
            {
                delay = 100;
                numBuffers = 10;
                incomingBufLength = 10;
            }
        };

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source1);
        pipeline.add(source2);
        pipeline.add(new Queue("queue"));
        pipeline.add(new FakeSink("sink"));

        // Main flow
        pipeline.link("source1", "in1< queue");
        pipeline.link("source2", "in2< queue");
        pipeline.link("queue", "sink");

        new Thread(pipeline.getLink("source1", STDOUT)).start();
        new Thread(pipeline.getLink("source2", STDOUT)).start();
        pipeline.getLink("sink", STDIN).run();
    }
}
