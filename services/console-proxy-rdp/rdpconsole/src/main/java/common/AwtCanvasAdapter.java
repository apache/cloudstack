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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import rdpclient.ServerBitmapUpdate;
import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.Order;
import streamer.Pipeline;
import streamer.PipelineImpl;

public class AwtCanvasAdapter extends BaseElement {

    protected ScreenDescription screen;

    public AwtCanvasAdapter(String id, BufferedImageCanvas canvas, ScreenDescription screen) {
        super(id);
        this.canvas = canvas;
        this.screen = screen;
    }

    protected BufferedImageCanvas canvas;

    @Override
    public String toString() {
        return "AwtRdpAdapter(" + id + ")";
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        Order order = buf.getOrder();
        switch ((OrderType)order.type) {

            case BITMAP_UPDATE:
                handleBitmap((BitmapOrder)order, buf);
                break;

            case COPY_RECT:
                handleCopyRect((CopyRectOrder)order, buf);
                break;

            default:
                throw new RuntimeException("Order is not implemented: " + buf + ".");
                // break;
        }

        buf.unref();
    }

    private void handleCopyRect(CopyRectOrder order, ByteBuffer buf) {
        // TODO Auto-generated method stub
        // Copy image
        canvas.getOfflineGraphics().copyArea(order.srcX, order.srcY, order.width, order.height, order.x - order.srcX, order.y - order.srcY);

        // Request update of repainted area
        canvas.repaint(order.x, order.y, order.width, order.height);

    }

    private void handleBitmap(BitmapOrder order, ByteBuffer buf) {
        // Draw rectangle on offline buffer
        BufferedImage image = canvas.getOfflineImage();
        Graphics2D g = (Graphics2D)image.getGraphics();

        for (BitmapRectangle rectangle : order.rectangles) {
            // *DEBUG*/System.out.println("["+this+"] DEBUG: Rectangle: " +
            // rectangle.toString());

            int x = rectangle.x;
            int y = rectangle.y;
            int width = rectangle.width;
            int height = rectangle.height;
            int bufferWidth = rectangle.bufferWidth;
            int bufferHeight = rectangle.bufferHeight;

            BufferedImage rectImage;
            switch (rectangle.colorDepth) {
                case 8: {
                    rectImage = new BufferedImage(bufferWidth, height, BufferedImage.TYPE_BYTE_INDEXED, screen.colorMap);
                    WritableRaster raster = rectImage.getRaster();
                    raster.setDataElements(0, 0, bufferWidth, bufferHeight, rectangle.bitmapDataStream.toByteArray());
                    break;
                }
                case 15: {
                    rectImage = new BufferedImage(bufferWidth, height, BufferedImage.TYPE_USHORT_555_RGB);
                    WritableRaster raster = rectImage.getRaster();
                    raster.setDataElements(0, 0, bufferWidth, bufferHeight, rectangle.bitmapDataStream.toShortArray());
                    break;
                }
                case 16: {
                    rectImage = new BufferedImage(bufferWidth, height, BufferedImage.TYPE_USHORT_565_RGB);
                    WritableRaster raster = rectImage.getRaster();
                    raster.setDataElements(0, 0, bufferWidth, bufferHeight, rectangle.bitmapDataStream.toShortArray());
                    break;
                }
                case 24:
                case 32: {
                    rectImage = new BufferedImage(bufferWidth, height, BufferedImage.TYPE_INT_RGB);
                    WritableRaster raster = rectImage.getRaster();
                    raster.setDataElements(0, 0, bufferWidth, bufferHeight, rectangle.bitmapDataStream.toIntLEArray());
                    break;
                }
                default:
                    throw new RuntimeException("Unsupported color depth: " + rectangle.colorDepth + ".");
            }

            g.setClip(x, y, width, height);
            g.drawImage(rectImage, x, y, null);

            // Request update of repainted area
            canvas.repaint(x, y, width, height);
        }

    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        // System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");
        ByteBuffer packet =
            new ByteBuffer(new byte[] {0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x00, 0x10, 0x00, 0x01, 0x00, 0x10, 0x00, 0x01, 0x04, 0x0a,
                0x00, 0x0c, (byte)0x84, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        Pipeline pipeline = new PipelineImpl("test");

        Element bitmap = new ServerBitmapUpdate("bitmap");

        BufferedImageCanvas canvas = new BufferedImageCanvas(1024, 768);
        Element adapter = new AwtCanvasAdapter("test", canvas, null) {
            {
                verbose = true;
            }
        };
        pipeline.addAndLink(bitmap, adapter);

        bitmap.handleData(packet, null);

    }

}
