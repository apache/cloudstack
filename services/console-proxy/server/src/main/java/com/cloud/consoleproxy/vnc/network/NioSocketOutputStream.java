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

public class NioSocketOutputStream extends NioSocketStream {

    private int sendPosition;

    public NioSocketOutputStream(int bufferSize, NioSocket socket) {
        super(bufferSize, socket);
        this.endPosition = bufferSize;
        this.sendPosition = 0;
    }

    protected final int checkWriteBufferForSingleItems(int items) {
        int window = endPosition - currentPosition;
        return window < 1 ?
                rearrangeWriteBuffer(1, items) :
                Math.min(window, items);
    }

    public final void checkWriteBufferForSize(int itemSize) {
        if (itemSize > endPosition - currentPosition) {
            rearrangeWriteBuffer(itemSize, 1);
        }
    }

    public void flushWriteBuffer() {
        while (sendPosition < currentPosition) {
            int writtenBytes = writeFromWriteBuffer(buffer, sendPosition, currentPosition - sendPosition);

            if (writtenBytes == 0) {
                throw new CloudRuntimeException("Timeout exception");
            }

            sendPosition += writtenBytes;
            offset += writtenBytes;
        }

        if (sendPosition == currentPosition) {
            sendPosition = start;
            currentPosition = start;
        }
    }

    protected boolean canUseWriteSelector() {
        int n = -1;
        while (n < 0) {
            n = socket.select(false, null);
        }
        return n > 0;
    }

    private int writeFromWriteBuffer(byte[] data, int dataPtr, int length) {
        if (!canUseWriteSelector()) {
            return 0;
        }
        return socket.writeToSocketChannel(ByteBuffer.wrap(data, dataPtr, length), length);
    }

    protected int rearrangeWriteBuffer(int itemSize, int numberItems) {
        checkItemSizeOnBuffer(itemSize);

        flushWriteBuffer();

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);

    }

    protected void writeUnsignedInteger(int sizeInBits, int value) {
        checkUnsignedIntegerSize(sizeInBits);
        int bytes = sizeInBits / 8;
        checkWriteBufferForSize(bytes);
        placeUnsignedIntegerToBuffer(bytes, value);
    }

    protected void writeBytes(byte[] data, int dataPtr, int length) {
        int dataEnd = dataPtr + length;
        while (dataPtr < dataEnd) {
            int n = checkWriteBufferForSingleItems(dataEnd - dataPtr);
            System.arraycopy(data, dataPtr, buffer, currentPosition, n);
            currentPosition += n;
            dataPtr += n;
        }
    }

    protected void writeBytes(ByteBuffer data, int length) {
        while (length > 0) {
            int n = checkWriteBufferForSingleItems(length);
            data.get(buffer, currentPosition, n);
            currentPosition += n;
            length -= n;
        }
    }
}
