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
package com.cloud.consoleproxy.rdp;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import com.cloud.consoleproxy.ConsoleProxyRdpClient;
import com.cloud.consoleproxy.util.ImageHelper;
import com.cloud.consoleproxy.util.Logger;
import com.cloud.consoleproxy.util.TileInfo;
import com.cloud.consoleproxy.vnc.FrameBufferCanvas;

import common.BufferedImageCanvas;

public class RdpBufferedImageCanvas extends BufferedImageCanvas implements FrameBufferCanvas {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger s_logger = Logger.getLogger(RdpBufferedImageCanvas.class);

    private final ConsoleProxyRdpClient _rdpClient;

    public RdpBufferedImageCanvas(ConsoleProxyRdpClient client, int width, int height) {
        super(width, height);
        _rdpClient = client;
    }

    @Override
    public Image getFrameBufferScaledImage(int width, int height) {
        if (offlineImage != null)
            return offlineImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        return null;
    }

    @Override
    public byte[] getFrameBufferJpeg() {
        int width = offlineImage.getWidth();
        int height = offlineImage.getHeight();

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bufferedImage.createGraphics();
        synchronized (offlineImage) {
            g.drawImage(offlineImage, 0, 0, width, height, 0, 0, width, height, null);
            g.dispose();
        }

        byte[] imgBits = null;
        try {
            imgBits = ImageHelper.jpegFromImage(bufferedImage);
        } catch (IOException e) {
            s_logger.info("[ignored] read error on image", e);
        }

        return imgBits;
    }

    @Override
    public byte[] getTilesMergedJpeg(List<TileInfo> tileList, int tileWidth, int tileHeight) {
        int width = Math.max(tileWidth, tileWidth * tileList.size());

        BufferedImage bufferedImage = new BufferedImage(width, tileHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bufferedImage.createGraphics();

        synchronized (offlineImage) {
            int i = 0;
            for (TileInfo tile : tileList) {
                Rectangle rc = tile.getTileRect();
                g.drawImage(offlineImage, i * tileWidth, 0, i * tileWidth + rc.width, rc.height, rc.x, rc.y, rc.x + rc.width, rc.y + rc.height, null);
                i++;
            }
        }

        byte[] imgBits = null;
        try {
            imgBits = ImageHelper.jpegFromImage(bufferedImage);
        } catch (IOException e) {
            s_logger.info("[ignored] read error on image tiles", e);
        }
        return imgBits;
    }

    @Override
    public void updateFrameBuffer(int x, int y, int w, int h) {
        _rdpClient.onFramebufferUpdate(x, y, w, h);
    }

}
