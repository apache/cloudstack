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

public class NioSocketTLSOutputStream extends NioSocketOutputStream {

    private final SSLEngineManager sslEngineManager;

    public NioSocketTLSOutputStream(SSLEngineManager sslEngineManager, NioSocket socket) {
        super(sslEngineManager.getSession().getApplicationBufferSize(), socket);
        this.sslEngineManager = sslEngineManager;
    }

    @Override
    public void flushWriteBuffer() {
        int sentUpTo = start;
        while (sentUpTo < currentPosition) {
            int n = writeTLS(buffer, sentUpTo, currentPosition - sentUpTo);
            sentUpTo += n;
            offset += n;
        }

        currentPosition = start;
    }

    protected int writeTLS(byte[] data, int dataPtr, int length) {
        int n;
        try {
            n = sslEngineManager.write(ByteBuffer.wrap(data, dataPtr, length), length);
        } catch (java.io.IOException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return n;
    }

    @Override
    protected int rearrangeWriteBuffer(int itemSize, int numberItems) {
        if (itemSize > buffer.length)
            throw new CloudRuntimeException("TLSOutStream overrun: max itemSize exceeded");

        flushWriteBuffer();

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);

    }
}
