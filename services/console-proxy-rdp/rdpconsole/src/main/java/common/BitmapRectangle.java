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

import streamer.ByteBuffer;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240612.aspx
 */
public class BitmapRectangle {

    /**
     * Left bound of the rectangle.
     */
    public int x;

    /**
     * Top bound of the rectangle.
     */
    public int y;

    /**
     * Width of the rectangle.
     */
    public int width;

    /**
     * Height of the rectangle.
     */
    public int height;

    /**
     * Color depth of the rectangle data in bits-per-pixel.
     */
    public int colorDepth;

    /**
     * Variable-length array of bytes describing a raw uncompressed bitmap image.
     */
    public ByteBuffer bitmapDataStream;

    /**
     * Size of single horizontal scan line.
     */
    public int bufferWidth;

    /**
     * Number of horizontal scan lines in buffer.
     */
    public int bufferHeight;

    @Override
    public String toString() {
        return String.format("BitmapUpdateRectangle [x=%s, y=%s, width=%s, height=%s, bitsPerPixel=%s, bitmapDataStream=%s]", x, y, width, height, colorDepth,
            bitmapDataStream);
    }

}
