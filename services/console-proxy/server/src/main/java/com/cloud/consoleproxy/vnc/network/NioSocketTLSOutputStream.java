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
package com.cloud.consoleproxy.vnc.network;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NioSocketTLSOutputStream extends NioSocketOutputStream {

    private final NioSocketSSLEngineManager sslEngineManager;

    public NioSocketTLSOutputStream(NioSocketSSLEngineManager sslEngineManager, NioSocket socket) {
        super(sslEngineManager.getSession().getApplicationBufferSize(), socket);
        this.sslEngineManager = sslEngineManager;
    }

    @Override
    public void flushWriteBuffer() {
        int sentUpTo = start;
        while (sentUpTo < currentPosition) {
            int n = writeThroughSSLEngineManager(buffer, sentUpTo, currentPosition - sentUpTo);
            sentUpTo += n;
            offset += n;
        }

        currentPosition = start;
    }

    protected int writeThroughSSLEngineManager(byte[] data, int startPos, int length) {
        try {
            return sslEngineManager.write(ByteBuffer.wrap(data, startPos, length));
        } catch (IOException e) {
            logger.error(String.format("Error writing though SSL engine manager: %s", e.getMessage()), e);
            return 0;
        }
    }

    @Override
    protected int rearrangeWriteBuffer(int itemSize, int numberItems) {
        checkItemSizeOnBuffer(itemSize);

        flushWriteBuffer();

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);
    }
}
