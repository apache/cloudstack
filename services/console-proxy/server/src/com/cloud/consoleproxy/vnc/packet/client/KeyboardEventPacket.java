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
package com.cloud.consoleproxy.vnc.packet.client;

import java.io.DataOutputStream;
import java.io.IOException;

import com.cloud.consoleproxy.vnc.RfbConstants;

public class KeyboardEventPacket implements ClientPacket {

    private final int downFlag, key;

    public KeyboardEventPacket(int downFlag, int key) {
        this.downFlag = downFlag;
        this.key = key;
    }

    @Override
    public void write(DataOutputStream os) throws IOException {
        os.writeByte(RfbConstants.CLIENT_KEYBOARD_EVENT);

        os.writeByte(downFlag);
        os.writeShort(0); // padding
        os.writeInt(key);
    }

}
