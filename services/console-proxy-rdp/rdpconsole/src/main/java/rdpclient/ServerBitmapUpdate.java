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
package rdpclient;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.FakeSink;
import streamer.Link;
import streamer.Pipeline;
import streamer.PipelineImpl;
import common.BitmapOrder;
import common.BitmapRectangle;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240624.aspx
 */
public class ServerBitmapUpdate extends BaseElement {
    public static final int UPDATETYPE_BITMAP = 0x0001;

    /**
     * Indicates that the bitmap data is compressed. The bitmapComprHdr field MUST
     * be present if the NO_BITMAP_COMPRESSION_HDR (0x0400) flag is not set.
     */
    public static final int BITMAP_COMPRESSION = 0x0001;

    /**
     * Indicates that the bitmapComprHdr field is not present (removed for
     * bandwidth efficiency to save 8 bytes).
     */
    private static final int NO_BITMAP_COMPRESSION_HDR = 0x0400;

    public ServerBitmapUpdate(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // * DEBUG */System.out.println(buf.toHexString(buf.length));

        BitmapOrder order = new BitmapOrder();

        // (2 bytes): A 16-bit, unsigned integer. The update type. This field MUST
        // be set to UPDATETYPE_BITMAP (0x0001).
        int updateType = buf.readSignedShortLE();
        if (updateType != UPDATETYPE_BITMAP)
            throw new RuntimeException("Unknown update type. Expected update type: UPDATETYPE_BITMAP (0x1). Actual update type: " + updateType + ", buf: " + buf + ".");

        // (2 bytes): A 16-bit, unsigned integer. The number of screen rectangles
        // present in the rectangles field.
        int numberRectangles = buf.readSignedShortLE();

        // (variable): Variable-length array of TS_BITMAP_DATA structures, each of
        // which contains a rectangular clipping taken from the server-side screen
        // frame buffer. The number of screen clippings in the array is specified by
        // the numberRectangles field.
        BitmapRectangle[] rectangles = new BitmapRectangle[numberRectangles];
        for (int i = 0; i < numberRectangles; i++) {
            rectangles[i] = readRectangle(buf);
        }
        order.rectangles = rectangles;

        buf.assertThatBufferIsFullyRead();

        ByteBuffer data = new ByteBuffer(0);
        data.setOrder(order);
        pushDataToAllOuts(data);

        buf.unref();
    }

    public BitmapRectangle readRectangle(ByteBuffer buf) {

        BitmapRectangle rectangle = new BitmapRectangle();

        // (2 bytes): A 16-bit, unsigned integer. Left bound of the rectangle.
        rectangle.x = buf.readSignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. Top bound of the rectangle.
        rectangle.y = buf.readSignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. Inclusive right bound of the
        // rectangle.
        int destRight = buf.readSignedShortLE();
        rectangle.width = destRight - rectangle.x + 1;

        // (2 bytes): A 16-bit, unsigned integer. Inclusive bottom bound of the
        // rectangle.
        int destBottom = buf.readSignedShortLE();
        rectangle.height = destBottom - rectangle.y + 1;

        // (2 bytes): A 16-bit, unsigned integer. The width of the rectangle.
        rectangle.bufferWidth = buf.readSignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. The height of the rectangle.
        rectangle.bufferHeight = buf.readSignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. The color depth of the rectangle
        // data in bits-per-pixel.
        rectangle.colorDepth = buf.readSignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. The flags describing the format of
        // the bitmap data in the bitmapDataStream field.
        int flags = buf.readSignedShortLE();

        // BITMAP_COMPRESSION 0x0001
        // Indicates that the bitmap data is compressed. The bitmapComprHdr field
        // MUST be present if the NO_BITMAP_COMPRESSION_HDR (0x0400) flag is not
        // set.
        boolean compressed = ((flags & BITMAP_COMPRESSION) > 0);

        // (2 bytes): A 16-bit, unsigned integer. The size in bytes of the data in
        // the bitmapComprHdr and bitmapDataStream fields.
        int bitmapLength = buf.readSignedShortLE();

        // NO_BITMAP_COMPRESSION_HDR 0x0400
        // Indicates that the bitmapComprHdr field is not present (removed for
        // bandwidth efficiency to save 8 bytes).
        if (compressed && (flags & NO_BITMAP_COMPRESSION_HDR) == 0) {
            // (8 bytes): Optional Compressed Data Header structure specifying the
            // bitmap data in the bitmapDataStream.
            // This field MUST be present if the BITMAP_COMPRESSION (0x0001) flag is
            // present in the Flags field, but the NO_BITMAP_COMPRESSION_HDR (0x0400)
            // flag is not.

            // Note: Even when compression header is enabled, server sends nothing.
            // rectangle.compressedBitmapHeader = buf.readBytes(8);
        }

        // (variable): A variable-length array of bytes describing a bitmap image.
        // Bitmap data is either compressed or uncompressed, depending on whether
        // the BITMAP_COMPRESSION flag is present in the Flags field. Uncompressed
        // bitmap data is formatted as a bottom-up, left-to-right series of pixels.
        // Each pixel is a whole number of bytes. Each row contains a multiple of
        // four bytes (including up to three bytes of padding, as necessary).
        // Compressed bitmaps not in 32 bpp format are compressed using Interleaved
        // RLE and encapsulated in an RLE Compressed Bitmap Stream structure,
        // while compressed bitmaps at a color depth of 32 bpp are compressed
        // using RDP 6.0 Bitmap Compression and stored inside
        // an RDP 6.0 Bitmap Compressed Stream structure.
        if (!compressed) {
            rectangle.bitmapDataStream = buf.readBytes(bitmapLength);
        } else {
            ByteBuffer compressedImage = buf.readBytes(bitmapLength);
            //* DEBUG */System.out.println("Compressed image: " + compressedImage + ", depth: " + rectangle.bitsPerPixel + ".");
            rectangle.bitmapDataStream = RLEBitmapDecompression.rleDecompress(compressedImage, rectangle.bufferWidth, rectangle.bufferHeight, rectangle.colorDepth);
            compressedImage.unref();
        }

        return rectangle;
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        ByteBuffer packet =
            new ByteBuffer(new byte[] {0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x00, 0x10, 0x00, 0x01, 0x00, 0x10, 0x00, 0x01, 0x04, 0x0a,
                0x00, 0x0c, (byte)0x84, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        Element bitmap = new ServerBitmapUpdate("bitmap") {
            {
                verbose = true;
            }
        };
        FakeSink fakeSink = new FakeSink("sink") {
            {
                verbose = true;
            }
        };
        Pipeline pipeline = new PipelineImpl("test");

        // BufferedImageCanvas canvas = new BufferedImageCanvas(1024, 768);
        // Element adapter = new AwtRdpAdapter("test",canvas );
        // pipeline.addAndLink(bitmap, adapter);
        pipeline.addAndLink(bitmap, fakeSink);

        bitmap.handleData(packet, null);

    }

}
