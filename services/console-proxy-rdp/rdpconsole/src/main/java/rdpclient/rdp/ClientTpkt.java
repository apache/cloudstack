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

public class ClientTpkt extends BaseElement {

    public ClientTpkt(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        if (buf.length + 4 > 65535)
            throw new RuntimeException("Packet is too long for TPKT (max length 65535-4): " + buf + ".");

        ByteBuffer data = new ByteBuffer(4);
        // TPKT version
        data.writeByte(3);
        // Reserved
        data.writeByte(0);
        // Packet length, including length of the header
        data.writeShort(buf.length + 4);

        buf.prepend(data);
        data.unref();

        pushDataToPad(STDOUT, buf);
    }

}
