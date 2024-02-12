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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NioSocketStream {

    protected byte[] buffer;
    protected int currentPosition;
    protected int offset;
    protected int endPosition;
    protected int start;
    protected NioSocket socket;

    protected Logger logger = LogManager.getLogger(getClass());

    public NioSocketStream(int bufferSize, NioSocket socket) {
        this.buffer = new byte[bufferSize];
        this.currentPosition = 0;
        this.offset = 0;
        this.endPosition = 0;
        this.start = 0;
        this.socket = socket;
    }

    protected boolean isUnsignedIntegerSizeAllowed(int sizeInBits) {
        return sizeInBits % 8 == 0 && sizeInBits > 0 && sizeInBits <= 32;
    }

    protected void checkUnsignedIntegerSize(int sizeInBits) {
        if (!isUnsignedIntegerSizeAllowed(sizeInBits)) {
            String msg = "Unsupported size in bits for unsigned integer reading " + sizeInBits;
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    protected int convertByteArrayToUnsignedInteger(byte[] readBytes) {
        if (readBytes.length == 1) {
            return readBytes[0] & 0xff;
        } else if (readBytes.length == 2) {
            int signed = readBytes[0] << 8 | readBytes[1] & 0xff;
            return signed & 0xffff;
        } else if (readBytes.length == 4) {
            return (readBytes[0] << 24) | (readBytes[1] & 0xff) << 16 |
                    (readBytes[2] & 0xff) << 8 | (readBytes[3] & 0xff);
        } else {
            throw new CloudRuntimeException("Error reading unsigned integer from socket stream");
        }
    }

    protected void placeUnsignedIntegerToBuffer(int bytes, int value) {
        if (bytes == 1) {
            buffer[currentPosition++] = (byte) value;
        } else if (bytes == 2) {
            buffer[currentPosition++] = (byte) (value >> 8);
            buffer[currentPosition++] = (byte) value;
        } else if (bytes == 4) {
            buffer[currentPosition++] = (byte) (value >> 24);
            buffer[currentPosition++] = (byte) (value >> 16);
            buffer[currentPosition++] = (byte) (value >> 8);
            buffer[currentPosition++] = (byte) value;
        }
    }

    protected void checkItemSizeOnBuffer(int itemSize) {
        if (itemSize > buffer.length) {
            String msg = String.format("Item size: %s exceeds the buffer size: %s", itemSize, buffer.length);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }
}
