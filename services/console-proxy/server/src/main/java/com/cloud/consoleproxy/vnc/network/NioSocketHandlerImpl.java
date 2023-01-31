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

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;

public class NioSocketHandlerImpl implements NioSocketHandler {

    private NioSocketInputStream inputStream;
    private NioSocketOutputStream outputStream;
    private boolean isTLS = false;

    private static final int DEFAULT_BUF_SIZE = 16384;

    private static final Logger s_logger = Logger.getLogger(NioSocketHandlerImpl.class);

    public NioSocketHandlerImpl(NioSocket socket) {
        this.inputStream = new NioSocketInputStream(DEFAULT_BUF_SIZE, socket);
        this.outputStream = new NioSocketOutputStream(DEFAULT_BUF_SIZE, socket);
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
            s_logger.trace("Waiting for inStream to be ready");
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
    public int readNextBytes() {
        return inputStream.getNextBytes();
    }

    @Override
    public void readNextByteArray(byte[] arr, int len) {
        inputStream.readNextByteArrayFromReadBuffer(arr, len);
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
