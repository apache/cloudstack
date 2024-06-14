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
package com.cloud.consoleproxy.vnc.security;

import com.cloud.consoleproxy.vnc.NoVncClient;
import com.cloud.consoleproxy.vnc.network.NioSocketHandler;
import com.cloud.utils.exception.CloudRuntimeException;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VncAuthSecurity implements VncSecurity {

    private final String vmPass;

    private static final int VNC_AUTH_CHALLENGE_SIZE = 16;
    protected Logger logger = LogManager.getLogger(getClass());

    public VncAuthSecurity(String vmPass) {
        this.vmPass = vmPass;
    }

    @Override
    public void process(NioSocketHandler socketHandler) throws IOException {
        logger.info("VNC server requires password authentication");

        // Read the challenge & obtain the user's password
        ByteBuffer challenge = ByteBuffer.allocate(VNC_AUTH_CHALLENGE_SIZE);
        socketHandler.readBytes(challenge, VNC_AUTH_CHALLENGE_SIZE);

        byte[] encodedPassword;
        try {
            encodedPassword = NoVncClient.encodePassword(challenge.array(), vmPass);
        } catch (Exception e) {
            logger.error("Cannot encrypt client password to send to server: " + e.getMessage());
            throw new CloudRuntimeException("Cannot encrypt client password to send to server: " + e.getMessage());
        }

        // Return the response to the server
        socketHandler.writeBytes(ByteBuffer.wrap(encodedPassword), encodedPassword.length);
        socketHandler.flushWriteBuffer();
        logger.info("Finished VNCAuth security");
    }
}
