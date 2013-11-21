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

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240589.aspx
 */
public class ClientFastPathPDU extends BaseElement {

    public ClientFastPathPDU(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        if (buf.length > 32767 - 3)
            throw new RuntimeException("Packet is too long: " + buf + ".");

        ByteBuffer data = new ByteBuffer(6);

        // FastPath, 1 event, no checksum, not encrypted
        data.writeByte(0x4);

        // Length of full packet, including length field, in network order.
        // Topmost bit of first byte indicates that field has 2 bytes
        data.writeShort((1 + 2 + buf.length) | 0x8000);
        data.trimAtCursor();

        buf.prepend(data);

        pushDataToAllOuts(buf);
    }

}
