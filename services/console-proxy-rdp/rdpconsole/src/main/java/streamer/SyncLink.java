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

import org.apache.log4j.Logger;

/**
 * Link to transfer data in bounds of single thread (synchronized transfer).
 * Must not be used to send data to elements served in different threads.
 */
public class SyncLink implements Link {
    private static final Logger s_logger = Logger.getLogger(SyncLink.class);

    /**
     * When null packet is pulled from source element, then make slight delay to
     * avoid consuming of 100% of CPU in main loop in cases when link is pauses or
     * source element cannot produce data right now.
     */
    public static final long STANDARD_DELAY_FOR_EMPTY_PACKET = 10; // Milliseconds

    /**
     * Delay for null packets in poll method when blocking is requested, in
     * milliseconds.
     */
    protected long delay = STANDARD_DELAY_FOR_EMPTY_PACKET;

    /**
     * Set to true to print debugging messages.
     */
    protected boolean verbose = System.getProperty("streamer.Link.debug", "false").equals("true");;

    /**
     * ID of this link.
     */
    protected String id = null;

    /**
     * Buffer with data to hold because link is paused, on hold, or data is pushed
     * back from output element.
     */
    protected ByteBuffer cacheBuffer = null;

    /**
     * Size of expected packet. Data must be hold in link until full packet will
     * be read.
     */
    protected int expectedPacketSize = 0;

    /**
     * Number of packets and packet header transferred to element.
     */
    protected int packetNumber = 0;

    /**
     * Element to pull data from, when in pull mode.
     */
    protected Element source = null;

    /**
     * Element to send data to in both pull and push modes.
     */
    protected Element sink = null;

    /**
     * When in loop, indicates that loop must be stopped.
     *
     * @see run()
     */
    private boolean shutdown = false;

    /**
     * Indicates that event STREAM_START is passed over this link, so main loop
     * can be started to pull data from source element.
     */
    protected boolean started = false;

    /**
     * Set to true to hold all data in link until it will be set to false again.
     */
    protected boolean paused = false;

    /**
     * Used by pull() method to hold all data in this link to avoid recursion when
     * source element is asked to push new data to it outputs.
     */
    protected boolean hold = false;

    /**
     * Operate in pull mode instead of default push mode. In pull mode, link will
     * ask it source element for new data.
     */
    protected boolean pullMode = false;

    public SyncLink() {
    }

    public SyncLink(String id) {
        this.id = id;
    }

    @Override
    public void pushBack(ByteBuffer buf) {
        if (verbose)
            s_logger.debug("[" + this + "] INFO: Buffer pushed back: " + buf + ".");

        if (cacheBuffer != null) {
            ByteBuffer tmp = cacheBuffer.join(buf);
            cacheBuffer.unref();
            cacheBuffer = tmp;
        } else {
            cacheBuffer = buf;
            cacheBuffer.ref();
        }

        resetCursor();
    }

    private void resetCursor() {
        // Reset cursor
        cacheBuffer.cursor = 0;
    }

    @Override
    public void pushBack(ByteBuffer buf, int lengthOfFullPacket) {
        pushBack(buf);
        expectedPacketSize = lengthOfFullPacket;
    }

    @Override
    public String toString() {
        return "SyncLink(" + ((id != null) ? id + ", " : "") + source + ":" + sink + ")";
    }

    /**
     * Push data to sink. Call with null to push cached data.
     */
    @Override
    public void sendData(ByteBuffer buf) {
        if (!hold && pullMode)
            throw new RuntimeException("[" + this + "] ERROR: link is not in push mode.");

        if (verbose)
            s_logger.debug("[" + this + "] INFO: Incoming buffer: " + buf + ".");

        if (buf == null && cacheBuffer == null)
            return;

        if (cacheBuffer != null && buf != null) {
            // Join old data with fresh data
            buf = cacheBuffer.join(buf);
            cacheBuffer.unref();
            cacheBuffer = buf;
        }

        // Store buffer in cache field to simplify following loop
        if (buf != null)
            cacheBuffer = buf;

        // When data pushed back and length of data is less than length of full
        // packet, then feed data to sink element immediately
        while (cacheBuffer != null) {
            if (paused || hold) {
                if (verbose)
                    s_logger.debug("[" + this + "] INFO: Transfer is paused. Data in cache buffer: " + cacheBuffer + ".");

                // Wait until rest of packet will be read
                return;
            }

            if (expectedPacketSize > 0 && cacheBuffer.length < expectedPacketSize) {
                if (verbose)
                    s_logger.debug("[" + this + "] INFO: Transfer is suspended because available data is less than expected packet size. Expected packet size: "
                            + expectedPacketSize + ", data in cache buffer: " + cacheBuffer + ".");

                // Wait until rest of packet will be read
                return;
            }

            // Full packet or packet header is read, feed it to element
            buf = cacheBuffer;
            cacheBuffer = null;
            expectedPacketSize = 0;
            packetNumber++;

            if (sink == null)
                throw new NullPointerException("[" + this + "] ERROR: Cannot send data to sink: sink is null. Data: " + buf + ".");

            sink.handleData(buf, this);
            // cacheBuffer and expectedPacketSize can be changed at this time
        }

    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void sendEvent(Event event, Direction direction) {

        if (verbose)
            s_logger.debug("[" + this + "] INFO: Event " + event + " is received.");

        // Shutdown main loop (if any) when STREAM_CLOSE event is received.
        switch (event) {
        case STREAM_START: {
            if (!started)
                started = true;
            else
                // Event already sent trough this link
                return;
            break;
        }
        case STREAM_CLOSE: {
            if (!shutdown)
                shutdown = true;
            else
                // Event already sent trough this link
                return;
            break;
        }
        case LINK_SWITCH_TO_PULL_MODE: {
            setPullMode();
            break;
        }

        }

        switch (direction) {
        case IN:
            source.handleEvent(event, direction);
            break;
        case OUT:
            sink.handleEvent(event, direction);
            break;
        }
    }

    @Override
    public ByteBuffer pull(boolean block) {
        if (!pullMode)
            throw new RuntimeException("[" + this + "] ERROR: This link is not in pull mode.");

        if (hold)
            throw new RuntimeException("[" + this + "] ERROR: This link is already on hold, waiting for data to be pulled in. Circular reference?");

        if (paused) {
            if (verbose)
                s_logger.debug("[" + this + "] INFO: Cannot pull, link is paused.");

            // Make slight delay in such case, to avoid consuming 100% of CPU
            if (block) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    s_logger.info("[ignored] interupted during pull", e);
                }
            }

            return null;
        }

        // If data in cache can be sent immediately,
        // then return it instead of asking for more data from source
        if (cacheBuffer != null && (expectedPacketSize == 0 || (expectedPacketSize > 0 && cacheBuffer.length >= expectedPacketSize))) {
            if (verbose)
                s_logger.debug("[" + this + "] INFO: Data pulled from cache buffer: " + cacheBuffer + ".");

            ByteBuffer tmp = cacheBuffer;
            cacheBuffer = null;
            return tmp;
        }

        // Pause this link, so incoming data will not be sent to sink
        // immediately, then ask source element for more data
        try {
            hold = true;
            source.poll(block);
        } finally {
            hold = false;
        }

        // Can return something only when data was stored in buffer
        if (cacheBuffer != null && (expectedPacketSize == 0 || (expectedPacketSize > 0 && cacheBuffer.length >= expectedPacketSize))) {
            if (verbose)
                s_logger.debug("[" + this + "] INFO: Data pulled from source: " + cacheBuffer + ".");

            ByteBuffer tmp = cacheBuffer;
            cacheBuffer = null;
            return tmp;
        } else {
            return null;
        }
    }

    @Override
    public Element setSink(Element sink) {
        if (sink != null && this.sink != null)
            throw new RuntimeException("[" + this + "] ERROR: This link sink element is already set. Link: " + this + ", new sink: " + sink + ", existing sink: "
                    + this.sink + ".");

        if (sink == null && cacheBuffer != null)
            throw new RuntimeException("[" + this + "] ERROR: Cannot drop link: cache is not empty. Link: " + this + ", cache: " + cacheBuffer);

        this.sink = sink;

        return sink;
    }

    @Override
    public Element setSource(Element source) {
        if (this.source != null && source != null)
            throw new RuntimeException("[" + this + "] ERROR: This link source element is already set. Link: " + this + ", new source: " + source
                    + ", existing source: " + this.source + ".");

        this.source = source;
        return source;
    }

    @Override
    public Element getSource() {
        return source;
    }

    @Override
    public Element getSink() {
        return sink;
    }

    @Override
    public void pause() {
        if (paused)
            throw new RuntimeException("[" + this + "] ERROR: Link is already paused.");

        paused = true;

    }

    @Override
    public void resume() {
        paused = false;
    }

    /**
     * Run pull loop to actively pull data from source and push it to sink. It
     * must be only one pull loop per thread.
     *
     * Pull loop will start after event STREAM_START. This link and source element
     * incomming links will be switched to pull mode before pull loop will be
     * started using event LINK_SWITCH_TO_PULL_MODE.
     */
    @Override
    public void run() {
        // Wait until even STREAM_START will arrive
        while (!started) {
            delay();
        }

        sendEvent(Event.LINK_SWITCH_TO_PULL_MODE, Direction.IN);

        if (verbose)
            s_logger.debug("[" + this + "] INFO: Starting pull loop.");

        // Pull source in loop
        while (!shutdown) {
            // Pull data from source element and send it to sink element
            ByteBuffer data = pull(false);
            if (data != null)
                sink.handleData(data, this);

            if (!shutdown && data == null) {
                // Make slight delay to avoid consuming of 100% of CPU
                delay();
            }
        }

        if (verbose)
            s_logger.debug("[" + this + "] INFO: Pull loop finished.");

    }

    protected void delay() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException("[" + this + "] ERROR: Interrupted in main loop.", e);
        }
    }

    @Override
    public void setPullMode() {
        if (verbose)
            s_logger.debug("[" + this + "] INFO: Switching to PULL mode.");

        pullMode = true;
    }

    @Override
    public void drop() {
        if (pullMode)
            throw new RuntimeException("[" + this + "] ERROR: Cannot drop link in pull mode.");

        if (cacheBuffer != null)
            throw new RuntimeException("[" + this + "] ERROR: Cannot drop link when cache conatains data: " + cacheBuffer + ".");

        source.dropLink(this);
        sink.dropLink(this);
    }
}
