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
package streamer.apr;

import org.apache.log4j.Logger;
import org.apache.tomcat.jni.Socket;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.DataSource;
import streamer.Direction;
import streamer.Event;
import streamer.Link;

public class AprSocketSink extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(AprSocketSink.class);

    protected AprSocketWrapperImpl socketWrapper;
    protected Long socket;

    public AprSocketSink(String id) {
        super(id);
    }

    public AprSocketSink(String id, AprSocketWrapperImpl socketWrapper) {
        super(id);
        this.socketWrapper = socketWrapper;
    }

    public void setSocket(long socket) {
        this.socket = socket;

        // Resume links
        resumeLinks();
    }

    /**
     * Send incoming data to stream.
     */
    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (socketWrapper.shutdown)
            return;

        try {
            if (verbose)
                System.out.println("[" + this + "] INFO: Writing data to stream: " + buf + ".");

            // FIXME: If pull is destroyed or socket is closed, segfault will happen here
            Socket.send(socket, buf.data, buf.offset, buf.length);
        } catch (Exception e) {
            System.err.println("[" + this + "] ERROR: " + e.getMessage());
            closeStream();
        }
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        switch (event) {
        case SOCKET_UPGRADE_TO_SSL:
            socketWrapper.upgradeToSsl();
            break;
        case LINK_SWITCH_TO_PULL_MODE:
            throw new RuntimeException("[" + this + "] ERROR: Unexpected event: sink recived LINK_SWITCH_TO_PULL_MODE event.");
        default:
            super.handleEvent(event, direction);
        }
    }

    @Override
    public void setLink(String padName, Link link, Direction direction) {
        switch (direction) {
        case IN:
            super.setLink(padName, link, direction);

            if (socket == null)
                // Pause links until data stream will be ready
                link.pause();
            break;
        case OUT:
            throw new RuntimeException("Cannot assign link to output pad in sink element. Element: " + this + ", pad: " + padName + ", link: " + link + ".");
        }
    }

    private void resumeLinks() {
        for (DataSource source : inputPads.values())
            ((Link)source).resume();
    }

    @Override
    protected void onClose() {
        closeStream();
    }

    private void closeStream() {
        if (socketWrapper.shutdown)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Closing stream.");

        try {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.IN);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failing sending sink event to all pads: " + e.getLocalizedMessage());
        }
        socketWrapper.shutdown();
    }

    @Override
    public String toString() {
        return "AprSocketSink(" + id + ")";
    }

}
