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
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NioSocketTLSInputStream extends NioSocketInputStream {

    private final NioSocketSSLEngineManager sslEngineManager;

    private static final Logger s_logger = Logger.getLogger(NioSocketTLSInputStream.class);

    public NioSocketTLSInputStream(NioSocketSSLEngineManager sslEngineManager, NioSocket socket) {
        super(sslEngineManager.getSession().getApplicationBufferSize(), socket);
        this.sslEngineManager = sslEngineManager;
    }

    protected int readFromSSLEngineManager(byte[] buffer, int startPos, int length) {
        try {
            int readBytes = sslEngineManager.read(ByteBuffer.wrap(buffer, startPos, length));
            if (readBytes < 0) {
                throw new CloudRuntimeException(String.format("Invalid number of read bytes frm SSL engine manager %s",
                        readBytes));
            }
            return readBytes;
        } catch (IOException e) {
            s_logger.error(String.format("Error reading from SSL engine manager: %s", e.getMessage()), e);
        }
        return 0;
    }

    @Override
    protected int rearrangeBufferToFitSize(int numberItems, int itemSize, boolean wait) {
        checkItemSizeOnBuffer(itemSize);

        if (endPosition - currentPosition != 0) {
            System.arraycopy(buffer, currentPosition, buffer, 0, endPosition - currentPosition);
        }

        offset += currentPosition - start;
        endPosition -= currentPosition - start;
        currentPosition = start;

        while ((endPosition - start) < itemSize) {
            int n = readFromSSLEngineManager(buffer, endPosition, start + buffer.length - endPosition);
            if (!wait && n == 0) {
                return 0;
            }
            endPosition += n;
        }

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);
    }
}
