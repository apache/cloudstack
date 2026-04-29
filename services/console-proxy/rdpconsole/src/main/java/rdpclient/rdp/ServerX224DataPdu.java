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
package rdpclient.rdp;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

public class ServerX224DataPdu extends BaseElement {

    public static final int X224_TPDU_LAST_DATA_UNIT = 0x80;
    public static final int X224_TPDU_DATA = 0xF0;

    public ServerX224DataPdu(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        int headerLength = buf.readVariableSignedIntLE();

        if (headerLength != 2)
            throw new RuntimeException("Unexpected X224 Data PDU header length. Expected header length: 2 , actual header length: " + headerLength + ".");

        // Read X224 type and options
        int type = buf.readUnsignedByte(); // High nibble: type, low nibble:

        if ((type & 0xf0) != X224_TPDU_DATA)
            throw new RuntimeException("[" + this + "] ERROR: Unexpected X224 packet type. Expected packet type: " + X224_TPDU_DATA
                    + " (X224_TPDU_DATA), actual packet type: " + type + ", buf: " + buf + ".");

        int options = buf.readUnsignedByte();

        if ((options & X224_TPDU_LAST_DATA_UNIT) != X224_TPDU_LAST_DATA_UNIT)
            throw new RuntimeException("Unexpected X224 packet options. Expected options: " + X224_TPDU_LAST_DATA_UNIT
                    + " (X224_TPDU_LAST_DATA_UNIT), actual packet options: " + options + ", buf: " + buf + ".");

        ByteBuffer payload = buf.readBytes(buf.length - buf.cursor);

        buf.unref();

        pushDataToAllOuts(payload);
    }
}
