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
package vncclient;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.ScreenDescription;

public class RGB888LE32PixelFormatRequest extends BaseElement {
    protected int bitsPerPixel = 32;
    protected int depth = 24;
    protected int bigEndianFlag = RfbConstants.LITTLE_ENDIAN;
    protected int trueColourFlag = RfbConstants.TRUE_COLOR;
    protected int redMax = 255;
    protected int greenMax = 255;
    protected int blueMax = 255;
    protected int redShift = 0;
    protected int greenShift = 8;
    protected int blueShift = 16;

    protected ScreenDescription screen;

    public RGB888LE32PixelFormatRequest(String id, ScreenDescription screen) {
        super(id);
        this.screen = screen;
    }

    protected void declarePads() {
        inputPads.put(STDIN, null);
        outputPads.put(STDOUT, null);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");
        buf.unref();

        ByteBuffer outBuf = new ByteBuffer(20);

        outBuf.writeByte(RfbConstants.CLIENT_SET_PIXEL_FORMAT);

        // Padding
        outBuf.writeByte(0);
        outBuf.writeByte(0);
        outBuf.writeByte(0);

        // Send pixel format
        outBuf.writeByte(bitsPerPixel);
        outBuf.writeByte(depth);
        outBuf.writeByte(bigEndianFlag);
        outBuf.writeByte(trueColourFlag);
        outBuf.writeShort(redMax);
        outBuf.writeShort(greenMax);
        outBuf.writeShort(blueMax);
        outBuf.writeByte(redShift);
        outBuf.writeByte(greenShift);
        outBuf.writeByte(blueShift);

        // Padding
        outBuf.writeByte(0);
        outBuf.writeByte(0);
        outBuf.writeByte(0);

        screen.setPixelFormat(bitsPerPixel, depth, bigEndianFlag != RfbConstants.LITTLE_ENDIAN, trueColourFlag == RfbConstants.TRUE_COLOR, redMax, greenMax, blueMax,
            redShift, greenShift, blueShift);

        pushDataToAllOuts(outBuf);
    }

}
