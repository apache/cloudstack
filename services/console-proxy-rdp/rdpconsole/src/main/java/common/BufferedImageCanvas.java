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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A <code>BuffereImageCanvas</code> component represents frame buffer image on the
 * screen. It also notifies its subscribers when screen is repainted.
 */
public class BufferedImageCanvas extends Canvas {
    private static final long serialVersionUID = 1L;

    // Offline screen buffer
    private BufferedImage offlineImage;

    // Cached Graphics2D object for offline screen buffer
    private Graphics2D graphics;

    public BufferedImageCanvas(int width, int height) {
        super();

        setBackground(Color.black);

        setFocusable(true);

        // Don't intercept TAB key
        setFocusTraversalKeysEnabled(false);

        setCanvasSize(width, height);
    }

    public void setCanvasSize(int width, int height) {
        this.offlineImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        graphics = offlineImage.createGraphics();

        setSize(offlineImage.getWidth(), offlineImage.getHeight());
    }

    @Override
    public void update(Graphics g) {
        // Call paint() directly, without clearing screen first
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        // Only part of image, requested with repaint(Rectangle), will be
        // painted on screen.
        g.drawImage(offlineImage, 0, 0, this);
    }

    public BufferedImage getOfflineImage() {
        return offlineImage;
    }

    public Graphics2D getOfflineGraphics() {
        return graphics;
    }

}
