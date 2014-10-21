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
package com.cloud.bridge.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * A DIME stream is actually composed of multiple encoded streams.
 * This class is a wrapper around the DimeDelimitedInputStream inorder
 * to provide a simple iterator like interface for all the streams in a
 * DIME encoded message.
 */
public class MultiPartDimeInputStream {
    protected final static Logger logger = Logger.getLogger(MultiPartDimeInputStream.class);

    protected InputStream is = null;
    protected DimeDelimitedInputStream currentStream = null;

    protected int count = 0;
    protected boolean eos = false;
    protected String contentId = null;
    protected String type = null;
    protected String typeFormat = null;

    /**
     * The SOAP stream must be first, call nextInputStream to get
     * access to the first stream and all streams after that.
     *
     * @param is the true input stream holding the incoming request.
     */
    public MultiPartDimeInputStream(InputStream is) throws IOException {
        this.is = is;
    }

    /**
     * These three methods are DIME specific but provide potentially
     * useful information about the current stream's data.
     *
     * @return URL or MIME type
     */
    public String getStreamType() {
        return type;
    }

    public String getStreamTypeFormat() {
        // Is the type a URI or MIME type or just unknown?
        return typeFormat;
    }

    public String getStreamId() {
        // The soap body might have string identifiers to point to other streams in the message
        return contentId;
    }

    public InputStream getInputStream() {
        return currentStream;
    }

    public int available() throws IOException {
        if (eos)
            return -1;

        if (null == currentStream) {
            throw new IOException("streamClosed -- call nextInputStream()");
        }
        return currentStream.available();
    }

    /**
     * Move on to the next stream encoded in the DIME stream.
     * If the current stream has not been all read, then we skip the remaining bytes of
     * that stream.
     *
     * @return false if no next input stream, true if next input stream ready
     * @throws IOException
     */
    public boolean nextInputStream() throws IOException {
        if (null == currentStream) {
            // on the first call to this function get the first stream
            if (0 == count)
                currentStream = new DimeDelimitedInputStream(is);
        } else {    // make sure the bytes of the previous stream are all skipped before we start the next
            currentStream.close();
            contentId = null;
            type = null;
            typeFormat = null;
            currentStream = currentStream.getNextStream();
        }

        if (null != currentStream) {
            contentId = currentStream.getContentId();
            type = currentStream.getType();
            typeFormat = currentStream.getDimeTypeNameFormat();
            eos = false;
            count++;
            return true;
        } else
            return false;
    }

    public long skip(long n) throws IOException {
        if (eos || null == currentStream) {
            throw new IOException("streamClosed -- call nextInputStream()");
        }
        return currentStream.skip(n);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (eos || null == currentStream)
            return -1;

        int read = currentStream.read(b, off, len);

        if (read < 0)
            eos = true;

        return read;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read() throws IOException {
        if (eos || null == currentStream)
            return -1;

        int ret = currentStream.read();

        if (ret < 0)
            eos = true;

        return ret;
    }
}
