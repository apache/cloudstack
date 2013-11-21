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
package common;

import java.awt.image.IndexColorModel;
import java.util.HashSet;
import java.util.Set;

/**
 * VncScreenDescription - contains information about remote VNC screen.
 */
public class ScreenDescription {

    protected Set<SizeChangeListener> sizeChangeListeners = new HashSet<SizeChangeListener>();

    // Frame buffer size
    protected int framebufferWidth = -1;
    protected int framebufferHeight = -1;

    // Desktop name
    protected String desktopName = null;

    // Bytes per pixel
    protected int bytesPerPixel;
    protected int colorDepth;
    protected int bitsPerPixel;
    protected int redShift;
    protected int greenShift;
    protected int blueShift;
    protected int redMax;
    protected int greenMax;
    protected int blueMax;
    protected boolean bigEndianFlag;
    protected boolean trueColorFlag;

    public IndexColorModel colorMap = null;

    public ScreenDescription() {
    }

    /**
     * Store information about server pixel format.
     */
    public void setPixelFormat(int bitsPerPixel, int depth, boolean bigEndianFlag, boolean trueColorFlag, int redMax, int greenMax, int blueMax, int redShift,
        int greenShift, int blueShift) {

        this.bytesPerPixel = (bitsPerPixel + 7) / 8;

        this.bitsPerPixel = bitsPerPixel;
        this.colorDepth = depth;
        this.bigEndianFlag = bigEndianFlag;
        this.trueColorFlag = trueColorFlag;

        this.redMax = redMax;
        this.greenMax = greenMax;
        this.blueMax = blueMax;
        this.redShift = redShift;
        this.greenShift = greenShift;
        this.blueShift = blueShift;
    }

    /**
     * Store information about server pixel format.
     */
    public void setPixelFormatRGBTrueColor(int bitsPerPixel) {

        switch (bitsPerPixel) {
            case 8:
                setPixelFormat(8, 8, false, false, -1, -1, -1, -1, -1, -1);
                break;
            case 15:
                setPixelFormat(16, 15, false, true, 31, 31, 31, 0, 5, 10);
                break;
            case 16:
                setPixelFormat(16, 16, false, true, 31, 63, 31, 0, 5, 11);
                break;
            case 24:
                setPixelFormat(24, 24, false, true, 255, 255, 255, 0, 8, 16);
                break;
            case 32:
                setPixelFormat(32, 24, false, true, 255, 255, 255, 0, 8, 16);
                break;
            default:
                throw new RuntimeException("Unknown color depth.");
        }

    }

    /**
     * Store information about server screen size.
     */
    public void setFramebufferSize(int width, int height) {
        if (height <= 0 || width <= 0)
            throw new RuntimeException("Incorrect framebuffer size: " + width + "x" + height + ".");

        this.framebufferWidth = width;
        this.framebufferHeight = height;

        callSizeChangeListeners(width, height);
    }

    protected void callSizeChangeListeners(int width, int height) {
        for (SizeChangeListener listener : sizeChangeListeners) {
            listener.sizeChanged(width, height);
        }
    }

    /**
     * Store server desktop name.
     */
    public void setDesktopName(String desktopName) {
        this.desktopName = desktopName;
    }

    // Getters for variables, as usual

    public String getDesktopName() {
        return desktopName;
    }

    public int getBytesPerPixel() {
        return bytesPerPixel;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public boolean isRGB888_32_LE() {
        return (colorDepth == 24 && bitsPerPixel == 32 && redShift == 0 && greenShift == 8 && blueShift == 16 && redMax == 255 && greenMax == 255 && blueMax == 255 &&
            !bigEndianFlag && trueColorFlag);
    }

    @Override
    public String toString() {
        return "ScreenDescription [framebufferWidth=" + framebufferWidth + ", framebufferHeight=" + framebufferHeight + ", desktopName=" + desktopName +
            ", bytesPerPixel=" + bytesPerPixel + ", depth=" + colorDepth + ", bitsPerPixel=" + bitsPerPixel + ", redShift=" + redShift + ", greenShift=" + greenShift +
            ", blueShift=" + blueShift + ", redMax=" + redMax + ", greenMax=" + greenMax + ", blueMax=" + blueMax + ", bigEndianFlag=" + bigEndianFlag +
            ", trueColorFlag=" + trueColorFlag + "]";
    }

    public void addSizeChangeListener(SizeChangeListener sizeChangeListener) {
        sizeChangeListeners.add(sizeChangeListener);
    }

    public int getColorDeph() {
        return colorDepth;
    }

}
