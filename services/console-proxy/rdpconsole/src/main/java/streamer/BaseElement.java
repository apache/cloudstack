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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import streamer.debug.FakeSink;
import streamer.debug.FakeSource;

public class BaseElement implements Element {

    protected Logger logger = LogManager.getLogger(getClass());

    protected String id;

    /**
     * Constant for @see cap() method to indicate that length is not restricted.
     */
    public static final int UNLIMITED = -1;

    /**
     * Set to true to enable debugging messages.
     */
    protected boolean verbose = false;

    /**
     * Limit on number of packets sent to sink from this element. Can be handy for
     * fake elements and handshake-related elements.
     */
    protected int numBuffers = 0;

    /**
     * Number of packets sent to sink.
     */
    protected int packetNumber = 0;

    /**
     * Recommended size for incoming buffer in pull mode.
     */
    protected int incomingBufLength = -1;

    protected Map<String, DataSource> inputPads = new HashMap<String, DataSource>();
    protected Map<String, DataSink> outputPads = new HashMap<String, DataSink>();

    public BaseElement(String id) {
        this.id = id;

        verbose = System.getProperty("streamer.Element.debug", "false").equals("true") || System.getProperty("streamer.Element.debug", "").contains(id);
    }

    @Override
    public String toString() {
        return "Element(" + id + ")";
    }

    @Override
    public Link getLink(String padName) {
        if (inputPads.containsKey(padName))
            return (Link)inputPads.get(padName);
        else if (outputPads.containsKey(padName))
            return (Link)outputPads.get(padName);
        else
            return null;
    }

    @Override
    public Set<String> getPads(Direction direction) {
        switch (direction) {
        case IN:
            return inputPads.keySet();

        case OUT:
            return outputPads.keySet();
        }
        return null;
    }

    @Override
    public void validate() {
        for (String padName : inputPads.keySet()) {
            if (inputPads.get(padName) == null)
                throw new RuntimeException("[ " + this + "] Required input pad is not connected. Pad name: " + padName + ".");
        }

        for (String padName : outputPads.keySet()) {
            if (outputPads.get(padName) == null)
                throw new RuntimeException("[ " + this + "] Required output pad is not connected. Pad name: " + padName + ".");
        }
    }

    @Override
    public void setLink(String padName, Link link, Direction direction) {
        switch (direction) {
        case IN:
            if (inputPads.get(padName) != null)
                throw new RuntimeException("Cannot link more than one wire to same pad. Element: " + this + ", pad: " + padName + ":" + direction + ", new link: "
                        + link + ", existing link: " + inputPads.get(padName) + ".");
            inputPads.put(padName, link);
            link.setSink(this);

            break;

        case OUT:
            if (outputPads.get(padName) != null)
                throw new RuntimeException("Cannot link more than one wire to same pad. Element: " + this + ", pad: " + padName + ":" + direction + ", new link: "
                        + link + ", existing link: " + outputPads.get(padName) + ".");

            outputPads.put(padName, link);
            link.setSource(this);

            break;
        }
    }

    @Override
    public void dropLink(String padName) {
        if (inputPads.containsKey(padName)) {
            Link link = (Link)inputPads.remove(padName);
            if (link != null)
                link.setSink(null);
        }

        if (outputPads.containsKey(padName)) {
            Link link = (Link)outputPads.remove(padName);
            if (link != null)
                link.setSource(null);
        }
    }

    /**
     * By default, try to pull data from input links.
     *
     * Override this method in data source elements.
     */
    @Override
    public void poll(boolean block) {
        // inputPads can be changed in handleData (see switchOff in OneTimeSwitch)
        // as this results in an undefined response from the iterator on inputPads
        // use a copy of the map to iterate over.
        Map<String, DataSource> pads = new HashMap<String, DataSource>();
        pads.putAll(inputPads);
        for (DataSource source : pads.values()) {
            Link link = (Link)source;
            ByteBuffer buf = link.pull(block);

            if (buf != null) {
                handleData(buf, link);
            }
        }
    }

    /**
     * By default, do nothing with incoming data and retransmit data to all output
     * pads.
     */
    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        pushDataToAllOuts(buf);
    }

    /**
     * Send data to all output pads.
     */
    protected final void pushDataToAllOuts(ByteBuffer buf) {

        if (buf == null)
            throw new NullPointerException();
        //return;

        if (outputPads.size() == 0)
            throw new RuntimeException("Number of outgoing connection is zero. Cannot send data to output. Data: " + buf + ".");

        // Send data to all pads with OUT direction
        for (DataSink out : outputPads.values()) {
            if (verbose)
                System.out.println("[" + this + "] INFO: Sending buf " + buf + " to " + out + ".");

            buf.rewindCursor();
            buf.ref();
            out.sendData(buf);
        }

        buf.unref();
        packetNumber++;
    }

    /**
     * Send data to given pad only.
     */
    protected void pushDataToPad(String padName, ByteBuffer buf) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Sending buf " + buf + " to " + padName + ".");

        DataSink link = outputPads.get(padName);
        if (link == null)
            throw new RuntimeException("Output pad of " + this + " element is not connected to a link. Pad name: " + padName + ".");

        buf.rewindCursor();
        link.sendData(buf);
    }

    /**
     * By default, do nothing with incoming event and retransmit event to all
     * pads.
     */
    @SuppressWarnings("incomplete-switch")
    @Override
    public void handleEvent(Event event, Direction direction) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Event " + event + ":" + direction + " is received.");

        switch (event) {
        case STREAM_CLOSE:
            onClose();
            break;
        case STREAM_START:
            onStart();
            break;
        }

        sendEventToAllPads(event, direction);
    }

    /**
     * Override this method to do something when STREAM_CLOSE event arrives.
     */
    protected void onClose() {
    }

    /**
     * Override this method to do something when STREAM_START event arrives.
     */
    protected void onStart() {
    }

    /**
     * Send event to all outputs.
     *
     * @param event
     *          a event
     * @param direction
     *          IN to send event to input pads, OUT to send event to all output
     *          pads
     */
    protected final void sendEventToAllPads(Event event, Direction direction) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Sending event " + event + ":" + direction + ".");

        switch (direction) {
        case IN:
            // Send event to all pads with IN direction
            for (DataSource in : inputPads.values()) {
                in.sendEvent(event, direction);
            }
            break;
        case OUT:
            // Send event to all pads with OUT direction
            for (DataSink out : outputPads.values()) {
                out.sendEvent(event, direction);
            }
            break;
        }
    }

    /**
     * Ensure that packet has required minimum and maximum length, cuts tail when
     * necessary.
     *
     * @param buf
     *          a buffer
     * @param minLength
     *          minimum length of packet or -1
     * @param maxLength
     *          maximum length of packet or -1
     * @param link
     *          source link, to push unnecessary data back
     * @param fromCursor
     *          if true, then position will be included into calculation
     * @return true, if buffer is long enough, false otherwise
     */
    public boolean cap(ByteBuffer buf, int minLength, int maxLength, Link link, boolean fromCursor) {

        if (buf == null)
            return false;

        int length = buf.length;

        int cursor;
        if (fromCursor)
            cursor = buf.cursor;
        else
            cursor = 0;

        length -= cursor;

        if ((minLength < 0 || length >= minLength) && (maxLength < 0 || length <= maxLength))
            // Buffer is already in bounds
            return true;

        // Buffer is too small, wait for the rest of buffer
        if (minLength >= 0 && length < minLength) {

            if (verbose)
                System.out.println("[" + this + "] INFO: Buffer is too small. Min length: " + minLength + ", data length (after cursor): " + length + ".");

            link.pushBack(buf.slice(0, length + cursor, true), minLength + cursor);
            return false;
        } else if (maxLength >= 0 && length > maxLength) {

            if (verbose)
                System.out.println("[" + this + "] INFO: Buffer is too big. Max length: " + maxLength + ", data length (after cursor): " + length + ".");

            // Buffer is too big, cut unnecessary tail
            link.pushBack(buf.slice(maxLength + cursor, length - maxLength, true));
            buf.length = maxLength + cursor;

        }

        return true;
    }

    @Override
    public void dropLink(Link link) {
        if (inputPads.containsValue(link)) {
            for (Entry<String, DataSource> entry : inputPads.entrySet())
                if (link.equals(entry.getValue())) {
                    inputPads.remove(entry.getKey());
                    break;
                }
        }

        if (outputPads.containsValue(link)) {
            for (Entry<String, DataSink> entry : outputPads.entrySet())
                if (link.equals(entry.getValue())) {
                    outputPads.remove(entry.getKey());
                    break;
                }
        }
    }

    @Override
    public void replaceLink(Link existingLink, Link newLink) {
        if (inputPads.containsValue(existingLink)) {
            for (Entry<String, DataSource> entry : inputPads.entrySet())
                if (existingLink.equals(entry.getValue())) {
                    inputPads.put(entry.getKey(), newLink);
                    newLink.setSink(this);
                    break;
                }
        }

        if (outputPads.containsValue(existingLink)) {
            for (Entry<String, DataSink> entry : outputPads.entrySet())
                if (existingLink.equals(entry.getValue())) {
                    outputPads.put(entry.getKey(), newLink);
                    newLink.setSource(this);
                    break;
                }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        Element source = new FakeSource("source") {
            {
                verbose = true;
                numBuffers = 10;
                incomingBufLength = 3;
                delay = 100;
            }
        };

        Element sink = new FakeSink("sink") {
            {
                verbose = true;
            }
        };

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source);
        pipeline.add(new BaseElement("t1"));
        pipeline.add(new BaseElement("t2"));
        pipeline.add(new BaseElement("t3"));
        pipeline.add(new BaseElement("t4"));
        pipeline.add(sink);

        pipeline.link("source", "t1", "t2", "t3", "t4", "sink");

        // Links between source-t1-t2 will operate in pull mode.
        // Links between t3-t4-sink will operate in push mode.
        // Link between t2-t3 will run main loop (pull from source and push to
        // sink).
        Link link = pipeline.getLink("t3", STDOUT);
        link.sendEvent(Event.STREAM_START, Direction.IN);
        link.run();
    }

}
