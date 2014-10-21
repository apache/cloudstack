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
package com.cloud.bridge.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class FileRangeInputStream extends InputStream {
    private RandomAccessFile randomAccessFile;
    private long curPos;
    private long endPos;
    private long fileLength;

    public FileRangeInputStream(File file, long startPos, long endPos) throws IOException {
        fileLength = file.length();

        if (startPos > fileLength)
            startPos = fileLength;

        if (endPos > fileLength)
            endPos = fileLength;

        if (startPos > endPos)
            throw new IllegalArgumentException("Invalid file range " + startPos + "-" + endPos);

        this.curPos = startPos;
        this.endPos = endPos;
        randomAccessFile = new RandomAccessFile(file, "r");
        randomAccessFile.seek(startPos);
    }

    @Override
    public int available() throws IOException {
        return (int)(endPos - curPos);
    }

    @Override
    public int read() throws IOException {
        if (available() > 0) {
            int value = randomAccessFile.read();
            curPos++;
            return value;
        }
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesToRead = Math.min(len, available());
        if (bytesToRead == 0)
            return -1;

        int bytesRead = randomAccessFile.read(b, off, bytesToRead);
        if (bytesRead < 0)
            return -1;

        curPos += bytesRead;
        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = Math.min(n, available());
        randomAccessFile.skipBytes((int)skipped);
        curPos += skipped;
        return skipped;
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }
}
