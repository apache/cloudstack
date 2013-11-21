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
package rdpclient;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

public class ServerTpkt extends BaseElement {

    /**
     * TPKT protocol version (first byte).
     */
    public static final int PROTOCOL_TPKT = 3;

    public ServerTpkt(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // We need at least 4 bytes to get packet length
        if (!cap(buf, 4, UNLIMITED, link, false))
            return;

        int version = buf.readUnsignedByte();
        if (version != PROTOCOL_TPKT)
            throw new RuntimeException("Unexpected data in TPKT header. Expected TPKT version: 0x03,  actual value: " + buf + ".");

        buf.skipBytes(1); // Reserved byte

        // Length of whole packet, including header
        int length = buf.readUnsignedShort();
        if (!cap(buf, length, length, link, false))
            return;

        int payloadLength = length - buf.cursor;

        // Extract payload
        ByteBuffer outBuf = buf.slice(buf.cursor, payloadLength, true);
        buf.unref();

        if (verbose) {
            outBuf.putMetadata("source", this);
        }

        pushDataToAllOuts(outBuf);
    }

}
