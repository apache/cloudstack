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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import streamer.debug.FakeSource;

public class OutputStreamSink extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(OutputStreamSink.class);

    protected OutputStream os;
    protected SocketWrapperImpl socketWrapper;

    public OutputStreamSink(String id) {
        super(id);
    }

    public OutputStreamSink(String id, OutputStream os) {
        super(id);
        this.os = os;
    }

    public OutputStreamSink(String id, SocketWrapperImpl socketWrapper) {
        super(id);
        this.socketWrapper = socketWrapper;
    }

    public void setOutputStream(OutputStream os) {
        this.os = os;
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

        try {
            if (verbose)
                System.out.println("[" + this + "] INFO: Writing data to stream: " + buf + ".");

            os.write(buf.data, buf.offset, buf.length);
            os.flush();
        } catch (IOException e) {
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
        default:
            super.handleEvent(event, direction);
        }
    }

    @Override
    public void setLink(String padName, Link link, Direction direction) {
        switch (direction) {
        case IN:
            super.setLink(padName, link, direction);

            if (os == null)
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
        if (verbose)
            System.out.println("[" + this + "] INFO: Closing stream.");

        try {
            os.close();
        } catch (IOException e) {
            s_logger.info("[ignored]"
                    + "io error on output: " + e.getLocalizedMessage());
        }
        try {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.IN);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "error sending output close event: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return "OutputStreamSink(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        Element source = new FakeSource("source") {
            {
                verbose = true;
                numBuffers = 3;
                incommingBufLength = 5;
                delay = 100;
            }
        };

        OutputStreamSink sink = new OutputStreamSink("sink") {
            {
                verbose = true;
            }
        };

        Link link = new SyncLink();

        source.setLink(STDOUT, link, Direction.OUT);
        sink.setLink(STDIN, link, Direction.IN);

        sink.setOutputStream(new ByteArrayOutputStream());

        link.sendEvent(Event.STREAM_START, Direction.IN);
        link.run();

    }
}
