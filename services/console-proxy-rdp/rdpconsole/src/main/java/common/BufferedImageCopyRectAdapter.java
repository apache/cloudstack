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

import java.awt.image.DataBufferInt;
import java.util.Arrays;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;

public class BufferedImageCopyRectAdapter extends BaseElement {

    public static final String SRC_X = "srcX";
    public static final String SRC_Y = "srcY";
    public static final String TARGET_X = "x";
    public static final String TARGET_Y = "y";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    protected BufferedImageCanvas canvas;

    public BufferedImageCopyRectAdapter(String id, BufferedImageCanvas canvas) {
        super(id);
        this.canvas = canvas;
        declarePads();
    }

    private void declarePads() {
        inputPads.put(STDIN, null);
    }

    @Override
    public String toString() {
        return "Renderer(" + id + ")";
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        int x = (Integer)buf.getMetadata(TARGET_X);
        int y = (Integer)buf.getMetadata(TARGET_Y);
        int width = (Integer)buf.getMetadata(WIDTH);
        int height = (Integer)buf.getMetadata(HEIGHT);
        int srcX = (Integer)buf.getMetadata(SRC_X);
        int srcY = (Integer)buf.getMetadata(SRC_Y);
        buf.unref();

        // Copy image
        canvas.getOfflineGraphics().copyArea(srcX, srcY, width, height, x - srcX, y - srcY);

        // Request update of repainted area
        canvas.repaint(x, y, width, height);
    }

    public static void main(String args[]) {
        System.setProperty("streamer.Element.debug", "true");

        BufferedImageCanvas canvas = new BufferedImageCanvas(4, 4);

        Element renderer = new BufferedImageCopyRectAdapter("renderer", canvas);

        int[] pixelsBeforeCopy = new int[] {
            // 0
            1, 2, 3, 4,
            // 1
            5, 6, 7, 8,
            // 2
            9, 10, 11, 12,
            // 3
            13, 14, 15, 16};
        int[] pixelsAfterCopy = new int[] {
            // 0
            11, 12, 3, 4,
            // 1
            15, 16, 7, 8,
            // 2
            9, 10, 11, 12,
            // 3
            13, 14, 15, 16};

        // Initalize image
        int[] data = ((DataBufferInt)canvas.getOfflineImage().getRaster().getDataBuffer()).getData();
        System.arraycopy(pixelsBeforeCopy, 0, data, 0, pixelsBeforeCopy.length);

        ByteBuffer buf = new ByteBuffer(new byte[0]);
        buf.putMetadata(TARGET_X, 0);
        buf.putMetadata(TARGET_Y, 0);
        buf.putMetadata(WIDTH, 2);
        buf.putMetadata(HEIGHT, 2);
        buf.putMetadata(SRC_X, 2);
        buf.putMetadata(SRC_Y, 2);

        renderer.handleData(buf, null);

        data = ((DataBufferInt)canvas.getOfflineImage().getRaster().getDataBuffer()).getData();
        String actualData = Arrays.toString(data);
        String expectedData = Arrays.toString(pixelsAfterCopy);
        if (!actualData.equals(expectedData))
            System.err.println("Actual image:   " + actualData + "\nExpected image: " + expectedData + ".");

    }

}
