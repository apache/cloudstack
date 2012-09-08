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
package com.cloud.consoleproxy.vnc.packet.server;

import java.io.DataInputStream;
import java.io.IOException;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.vnc.RfbConstants;

public class ServerCutText {
    private static final Logger s_logger = Logger.getLogger(ServerCutText.class);

    private String content;

    public String getContent() {
        return content;
    }

    public ServerCutText(DataInputStream is) throws IOException {
        readPacketData(is);
    }

    private void readPacketData(DataInputStream is) throws IOException {
        is.skipBytes(3);// Skip padding
        int length = is.readInt();
        byte buf[] = new byte[length];
        is.readFully(buf);

        content = new String(buf, RfbConstants.CHARSET);

        /* LOG */s_logger.info("Clippboard content: " + content);
    }

}
