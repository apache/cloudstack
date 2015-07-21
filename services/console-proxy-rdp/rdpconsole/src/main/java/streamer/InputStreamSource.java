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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import streamer.debug.FakeSink;

/**
 * Source element, which reads data from InputStream.
 */
public class InputStreamSource extends BaseElement {
    private static final Logger s_logger = Logger.getLogger(InputStreamSource.class);

    protected InputStream is;
    protected SocketWrapperImpl socketWrapper;

    public InputStreamSource(String id) {
        super(id);
    }

    public InputStreamSource(String id, InputStream is) {
        super(id);
        this.is = is;
    }

    public InputStreamSource(String id, SocketWrapperImpl socketWrapper) {
        super(id);
        this.socketWrapper = socketWrapper;
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
        case OUT:
            super.setLink(padName, link, direction);

            if (is == null) {
                // Pause links until data stream will be ready
                link.pause();
            }
            break;
        case IN:
            throw new RuntimeException("Cannot assign link to input pad in source element. Element: " + this + ", pad: " + padName + ", link: " + link + ".");
        }
    }

    public void setInputStream(InputStream is) {
        this.is = is;

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
        try {
            if (!block && is.available() == 0) {

                if (verbose)
                    System.out.println("[" + this + "] INFO: No data in stream is available now, returning.");

                return;
            }

            // Create buffer of recommended size and with default offset
            ByteBuffer buf = new ByteBuffer(incommingBufLength);

            if (verbose)
                System.out.println("[" + this + "] INFO: Reading data from stream.");

            int actualLength = is.read(buf.data, buf.offset, buf.data.length - buf.offset);

            if (actualLength < 0) {
                if (verbose)
                    System.out.println("[" + this + "] INFO: End of stream.");

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

        } catch (IOException e) {
            System.err.println("[" + this + "] ERROR: " + e.getMessage());
            closeStream();
        }
    }

    @Override
    protected void onClose() {
        closeStream();
    }

    private void closeStream() {
        if (verbose)
            System.out.println("[" + this + "] INFO: Closing stream.");

        try {
            is.close();
        } catch (IOException e) {
            s_logger.info("[ignored]"
                    + "io error on input stream: " + e.getLocalizedMessage());
        }
        try {
            sendEventToAllPads(Event.STREAM_CLOSE, Direction.OUT);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "error sending an event to all pods: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return "InputStreamSource(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        InputStream is = new ByteArrayInputStream(new byte[] {1, 2, 3});

        InputStreamSource source = new InputStreamSource("source") {
            {
                verbose = true;
            }
        };
        Element fakeSink = new FakeSink("sink") {
            {
                verbose = true;
            }
        };

        Link link = new SyncLink() {
            {
                verbose = true;
            }
        };

        source.setLink(STDOUT, link, Direction.OUT);
        fakeSink.setLink(STDIN, link, Direction.IN);

        source.setInputStream(is);

        link.sendEvent(Event.STREAM_START, Direction.OUT);
        link.run();

    }

}
