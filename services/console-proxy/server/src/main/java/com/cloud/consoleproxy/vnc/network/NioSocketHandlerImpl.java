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


import com.cloud.consoleproxy.ConsoleProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class NioSocketHandlerImpl implements NioSocketHandler {

    private NioSocketInputStream inputStream;
    private NioSocketOutputStream outputStream;
    private boolean isTLS = false;

    protected Logger logger = LogManager.getLogger(getClass());

    public NioSocketHandlerImpl(NioSocket socket) {
        this.inputStream = new NioSocketInputStream(ConsoleProxy.defaultBufferSize, socket);
        this.outputStream = new NioSocketOutputStream(ConsoleProxy.defaultBufferSize, socket);
    }

    @Override
    public int readUnsignedInteger(int sizeInBits) {
        return inputStream.readUnsignedInteger(sizeInBits);
    }

    @Override
    public void writeUnsignedInteger(int sizeInBits, int value) {
        outputStream.writeUnsignedInteger(sizeInBits, value);
    }

    @Override
    public void readBytes(ByteBuffer data, int length) {
        inputStream.readBytes(data, length);
    }

    @Override
    public void waitForBytesAvailableForReading(int bytes) {
        while (!inputStream.checkForSizeWithoutWait(bytes)) {
            logger.trace("Waiting for inStream to be ready");
        }
    }

    @Override
    public void writeBytes(byte[] data, int dataPtr, int length) {
        outputStream.writeBytes(data, dataPtr, length);
    }

    @Override
    public void writeBytes(ByteBuffer data, int length) {
        outputStream.writeBytes(data, length);
    }

    @Override
    public void flushWriteBuffer() {
        outputStream.flushWriteBuffer();
    }

    @Override
    public void startTLSConnection(NioSocketSSLEngineManager sslEngineManager) {
        this.inputStream = new NioSocketTLSInputStream(sslEngineManager, this.inputStream.socket);
        this.outputStream = new NioSocketTLSOutputStream(sslEngineManager, this.outputStream.socket);
        this.isTLS = true;
    }

    @Override
    public boolean isTLSConnection() {
        return this.isTLS;
    }

    @Override
    public String readString() {
        return inputStream.readString();
    }

    @Override
    public byte[] readServerInit() {
        return inputStream.readServerInit();
    }

    @Override
    public int readAvailableDataIntoBuffer(ByteBuffer buffer, int maxSize) {
        return inputStream.readAvailableDataIntoBuffer(buffer, maxSize);
    }

    @Override
    public NioSocketInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public NioSocketOutputStream getOutputStream() {
        return outputStream;
    }
}
