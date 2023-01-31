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

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NioSocketInputStream extends NioSocketStream {

    public NioSocketInputStream(int bufferSize, NioSocket socket) {
        super(bufferSize, socket);
    }

    public int getReadBytesAvailableToFitSize(int itemSize, int numberItems, boolean wait) {
        int window = endPosition - currentPosition;
        if (itemSize > window) {
            return rearrangeBufferToFitSize(numberItems, itemSize, wait);
        }
        return Math.min(window / itemSize, numberItems);
    }

    protected void moveDataToBufferStart() {
        if (endPosition - currentPosition != 0) {
            System.arraycopy(buffer, currentPosition, buffer, 0, endPosition - currentPosition);
        }
        offset += currentPosition;
        endPosition -= currentPosition;
        currentPosition = 0;
    }

    protected boolean canUseReadSelector(boolean wait) {
        int n = -1;
        Integer timeout = !wait ? 0 : null;
        while (n < 0) {
            n = socket.select(true, timeout);
        }
        return n > 0 || wait;
    }

    protected int readBytesToBuffer(ByteBuffer buf, int bytesToRead, boolean wait) {
        if (!canUseReadSelector(wait)) {
            return 0;
        }
        int readBytes = socket.readFromSocketChannel(buf, bytesToRead);
        if (readBytes == 0) {
            throw new CloudRuntimeException("End of stream exception");
        }
        return readBytes;
    }

    protected int rearrangeBufferToFitSize(int numberItems, int itemSize, boolean wait) {
        checkItemSizeOnBuffer(itemSize);

        moveDataToBufferStart();

        while (endPosition < itemSize) {
            int remainingBufferSize = buffer.length - endPosition;
            int desiredCapacity = itemSize * numberItems;
            int bytesToRead = Math.min(remainingBufferSize, Math.max(desiredCapacity, 8));

            ByteBuffer buf = ByteBuffer.wrap(buffer).position(endPosition);
            int n = readBytesToBuffer(buf, bytesToRead, wait);
            if (n == 0) {
                return 0;
            }
            endPosition += n;
        }

        int window = endPosition - currentPosition;
        return Math.min(window / itemSize, numberItems);
    }

    protected Pair<Integer, byte[]> readAndCopyUnsignedInteger(int sizeInBits) {
        checkUnsignedIntegerSize(sizeInBits);
        int bytes = sizeInBits / 8;
        getReadBytesAvailableToFitSize(bytes, 1, true);
        byte[] unsignedIntegerArray = Arrays.copyOfRange(buffer, currentPosition, currentPosition + bytes);
        currentPosition += bytes;
        return new Pair<>(convertByteArrayToUnsignedInteger(unsignedIntegerArray), unsignedIntegerArray);
    }

    protected int readUnsignedInteger(int sizeInBits) {
        Pair<Integer, byte[]> pair = readAndCopyUnsignedInteger(sizeInBits);
        return pair.first();
    }

    protected void readBytes(ByteBuffer data, int length) {
        while (length > 0) {
            int n = getReadBytesAvailableToFitSize(1, length, true);
            data.put(buffer, currentPosition, n);
            currentPosition += n;
            length -= n;
        }
    }

    public boolean checkForSizeWithoutWait(int size) {
        return getReadBytesAvailableToFitSize(size, 1, false) != 0;
    }

    protected final String readString() {
        int len = readUnsignedInteger(32);

        ByteBuffer str = ByteBuffer.allocate(len);
        readBytes(str, len);
        return new String(str.array(), StandardCharsets.UTF_8);
    }

    /**
     * Read ServerInit message and return it as a byte[] for noVNC
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#732serverinit
     */
    public byte[] readServerInit() {
        // Read width, height, pixel format and VM name
        byte[] bytesRead = new byte[] {};
        Pair<Integer, byte[]> widthPair = readAndCopyUnsignedInteger(16);
        bytesRead = ArrayUtils.addAll(bytesRead, widthPair.second());
        Pair<Integer, byte[]> heightPair = readAndCopyUnsignedInteger(16);
        bytesRead = ArrayUtils.addAll(bytesRead, heightPair.second());

        byte[] pixelFormatByteArr = readPixelFormat();
        bytesRead = ArrayUtils.addAll(bytesRead, pixelFormatByteArr);

        Pair<Integer, byte[]> pair = readAndCopyUnsignedInteger(32);
        int len = pair.first();
        bytesRead = ArrayUtils.addAll(bytesRead, pair.second());

        ByteBuffer str = ByteBuffer.allocate(len);
        readBytes(str, len);
        return ArrayUtils.addAll(bytesRead, str.array());
    }

    protected final void skipReadBytes(int bytes) {
        while (bytes > 0) {
            int n = getReadBytesAvailableToFitSize(1, bytes, true);
            currentPosition += n;
            bytes -= n;
        }
    }

    /**
     * Read PixelFormat and return it as byte[]
     */
    private byte[] readPixelFormat() {
        Pair<Integer, byte[]> bppPair = readAndCopyUnsignedInteger(8);
        byte[] ret = bppPair.second();
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(16).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(16).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(16).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        ret = ArrayUtils.addAll(ret, readAndCopyUnsignedInteger(8).second());
        skipReadBytes(3);
        return ArrayUtils.addAll(ret, (byte) 0, (byte) 0, (byte) 0);
    }

    protected int getNextBytes() {
        int size = 200;
        while (size > 0) {
            if (checkForSizeWithoutWait(size)) {
                break;
            }
            size--;
        }
        return size;
    }

    protected void readNextByteArrayFromReadBuffer(byte[] arr, int len) {
        copyBytesFromReadBuffer(len, arr);
    }

    protected void copyBytesFromReadBuffer(int length, byte[] arr) {
        int ptr = 0;
        while (length > 0) {
            int n = getReadBytesAvailableToFitSize(1, length, true);
            readBytes(ByteBuffer.wrap(arr, ptr, n), n);
            ptr += n;
            length -= n;
        }
    }
}
