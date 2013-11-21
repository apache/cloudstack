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
package vncclient;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

public class EncodingsMessage extends BaseElement {

    protected final int[] encodings;

    public EncodingsMessage(String id, int[] encodings) {
        super(id);
        this.encodings = encodings;
        declarePads();
    }

    protected void declarePads() {
        inputPads.put(STDIN, null);
        outputPads.put(STDOUT, null);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");
        buf.unref();

        ByteBuffer outBuf = new ByteBuffer(4 + encodings.length * 4);

        outBuf.writeByte(RfbConstants.CLIENT_SET_ENCODINGS);

        outBuf.writeByte(0);// padding

        outBuf.writeShort(encodings.length);

        for (int i = 0; i < encodings.length; i++) {
            outBuf.writeInt(encodings[i]);
        }

        pushDataToAllOuts(outBuf);

    }

}
