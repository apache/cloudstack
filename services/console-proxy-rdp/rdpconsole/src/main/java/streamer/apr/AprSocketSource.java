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
import streamer.DataSink;
import streamer.Direction;
import streamer.Event;
import streamer.Link;

/**
 * Source element, which reads data from InputStream.
 */
public class AprSocketSource extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(AprSocketSource.class);

    protected AprSocketWrapperImpl socketWrapper;
    protected Long socket;

    public AprSocketSource(String id) {
        super(id);
    }

    public AprSocketSource(String id, AprSocketWrapperImpl socketWrapper) {
        super(id);
        this.socketWrapper = socketWrapper;
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        switch (event) {
        case SOCKET_UPGRADE_TO_SSL:
            socketWrapper.upgradeToSsl();
            break;
        case LINK_SWITCH_TO_PULL_MODE:
            // Do nothing - this is the source
            break;
        default:
            super.handleEvent(event, direction);
        }
    }

    @Override
    public void setLink(String padName, Link link, Direction direction) {
        switch (direction) {
        case OUT:
            super.setLink(padName, link, direction);

            if (socket == null) {
                // Pause links until data stream will be ready
                link.pause();
            }
            break;
        case IN:
            throw new RuntimeException("Cannot assign link to input pad in source element. Element: " + this + ", pad: " + padName + ", link: " + link + ".");
        }
    }

    public void setSocket(long socket) {
        this.socket = socket;

        // Resume links
        resumeLinks();
    }

    private void resumeLinks() {
        for (DataSink sink : outputPads.values())
            ((Link)sink).resume();
    }

    /**
     * Read data from input stream.
     */
    @Override
    public void poll(boolean block) {
        if (socketWrapper.shutdown) {
            socketWrapper.destroyPull();
            return;
        }

        try {
            // Create buffer of recommended size and with default offset
            ByteBuffer buf = new ByteBuffer(incommingBufLength);

            if (verbose)
                System.out.println("[" + this + "] INFO: Reading data from stream.");

            // to unblock during reboot
            long startTime = System.currentTimeMillis();
            // FIXME: If pull is destroyed or socket is closed, segfault will happen here
            int actualLength = (block) ? // Blocking read
                    Socket.recv(socket, buf.data, buf.offset, buf.data.length - buf.offset)
                    : // Non-blocking read
                        Socket.recvt(socket, buf.data, buf.offset, buf.data.length - buf.offset, 5000000);

                    if (socketWrapper.shutdown) {
                        socketWrapper.destroyPull();
                        return;
                    }

                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (actualLength < 0 || elapsedTime > 5000) {
                        if (verbose)
                            System.out.println("[" + this + "] INFO: End of stream or timeout");

                        buf.unref();
                        closeStream();
                        sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
                        return;
                    }

                    if (actualLength == 0) {
                        if (verbose)
                            System.out.println("[" + this + "] INFO: Empty buffer is read from stream.");

                        buf.unref();
                        return;
                    }

                    buf.length = actualLength;

                    if (verbose)
                        System.out.println("[" + this + "] INFO: Data read from stream: " + buf + ".");

                    pushDataToAllOuts(buf);

        } catch (Exception e) {
            System.err.println("[" + this + "] ERROR: " + e.getMessage());
            closeStream();
        }
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
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failing sending source event to all pads: " + e.getLocalizedMessage());
        }
        socketWrapper.shutdown();
    }

    @Override
    public String toString() {
        return "AprSocketSource(" + id + ")";
    }

}
