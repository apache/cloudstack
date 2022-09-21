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

import com.cloud.utils.exception.CloudRuntimeException;

import java.nio.ByteBuffer;

public class NioSocketTLSInputStream extends NioSocketInputStream {

    private final SSLEngineManager sslEngineManager;

    public NioSocketTLSInputStream(SSLEngineManager sslEngineManager, NioSocket socket) {
        super(sslEngineManager.getSession().getApplicationBufferSize(), socket);
        this.sslEngineManager = sslEngineManager;
    }

    protected int readTLS(byte[] buf, int bufPtr, int len) {
        int n = -1;
        try {
            n = sslEngineManager.read(ByteBuffer.wrap(buf, bufPtr, len), len);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        if (n < 0) throw new CloudRuntimeException("readTLS" + n);
        return n;
    }

    @Override
    protected int rearrangeBufferToFitSize(int numberItems, int itemSize, boolean wait) {
        if (itemSize > buffer.length) {
            throw new CloudRuntimeException("Cannot read item longer than the buffer size");
        }

        if (endPosition - currentPosition != 0) {
            System.arraycopy(buffer, currentPosition, buffer, 0, endPosition - currentPosition);
        }

        offset += currentPosition - start;
        endPosition -= currentPosition - start;
        currentPosition = start;

        while ((endPosition - start) < itemSize) {
            int n = readTLS(buffer, endPosition, start + buffer.length - endPosition);
            if (!wait && n == 0) {
                return 0;
            }
            endPosition += n;
        }

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);
    }
}
