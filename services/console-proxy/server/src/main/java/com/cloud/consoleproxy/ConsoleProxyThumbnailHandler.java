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
package com.cloud.consoleproxy;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.cloud.consoleproxy.util.Logger;

public class ConsoleProxyThumbnailHandler implements HttpHandler {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyThumbnailHandler.class);

    public ConsoleProxyThumbnailHandler() {
    }

    @Override
    @SuppressWarnings("access")
    public void handle(HttpExchange t) throws IOException {
        try {
            Thread.currentThread().setName("JPG Thread " + Thread.currentThread().getId() + " " + t.getRemoteAddress());

            if (s_logger.isDebugEnabled())
                s_logger.debug("ScreenHandler " + t.getRequestURI());

            long startTick = System.currentTimeMillis();
            doHandle(t);

            if (s_logger.isDebugEnabled())
                s_logger.debug(t.getRequestURI() + "Process time " + (System.currentTimeMillis() - startTick) + " ms");
        } catch (IllegalArgumentException e) {
            String response = "Bad query string";
            s_logger.error(response + ", request URI : " + t.getRequestURI());
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (OutOfMemoryError e) {
            s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
            System.exit(1);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception while handing thumbnail request, ", e);

            String queries = t.getRequestURI().getQuery();
            Map<String, String> queryMap = getQueryMap(queries);
            int width = 0;
            int height = 0;
            String ws = queryMap.get("w");
            String hs = queryMap.get("h");
            try {
                width = Integer.parseInt(ws);
                height = Integer.parseInt(hs);
            } catch (NumberFormatException ex) {
                s_logger.debug("Cannot parse width: " + ws + " or height: " + hs, ex);
            }
            width = Math.min(width, 800);
            height = Math.min(height, 600);

            BufferedImage img = generateTextImage(width, height, "Cannot Connect");
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
            javax.imageio.ImageIO.write(img, "jpg", bos);
            byte[] bs = bos.toByteArray();
            Headers hds = t.getResponseHeaders();
            hds.set("content-type", "image/jpeg");
            hds.set("cache-control", "no-cache");
            hds.set("cache-control", "no-store");
            t.sendResponseHeaders(200, bs.length);
            OutputStream os = t.getResponseBody();
            os.write(bs);
            os.close();
            s_logger.error("Cannot get console, sent error JPG response for " + t.getRequestURI());
            return;
        } finally {
            t.close();
        }
    }

    private void doHandle(HttpExchange t) throws Exception, IllegalArgumentException {
        String queries = t.getRequestURI().getQuery();
        Map<String, String> queryMap = getQueryMap(queries);
        int width = 0;
        int height = 0;
        int port = 0;
        String ws = queryMap.get("w");
        String hs = queryMap.get("h");
        String host = queryMap.get("host");
        String portStr = queryMap.get("port");
        String sid = queryMap.get("sid");
        String tag = queryMap.get("tag");
        String ticket = queryMap.get("ticket");
        String console_url = queryMap.get("consoleurl");
        String console_host_session = queryMap.get("sessionref");

        if (tag == null)
            tag = "";

        if (ws == null || hs == null || host == null || portStr == null || sid == null) {
            throw new IllegalArgumentException();
        }
        try {
            width = Integer.parseInt(ws);
            height = Integer.parseInt(hs);
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }

        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(host);
        param.setClientHostPort(port);
        param.setClientHostPassword(sid);
        param.setClientTag(tag);
        param.setTicket(ticket);
        param.setClientTunnelUrl(console_url);
        param.setClientTunnelSession(console_host_session);

        ConsoleProxyClient viewer = ConsoleProxy.getVncViewer(param);

        if (!viewer.isHostConnected()) {
            // use generated image instead of static
            BufferedImage img = generateTextImage(width, height, "Connecting");
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
            javax.imageio.ImageIO.write(img, "jpg", bos);
            byte[] bs = bos.toByteArray();
            Headers hds = t.getResponseHeaders();
            hds.set("content-type", "image/jpeg");
            hds.set("cache-control", "no-cache");
            hds.set("cache-control", "no-store");
            t.sendResponseHeaders(200, bs.length);
            OutputStream os = t.getResponseBody();
            os.write(bs);
            os.close();

            if (s_logger.isInfoEnabled())
                s_logger.info("Console not ready, sent dummy JPG response");
            return;
        }

        {
            Image scaledImage = viewer.getClientScaledImage(width, height);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D bufImageGraphics = bufferedImage.createGraphics();
            bufImageGraphics.drawImage(scaledImage, 0, 0, null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8196);
            javax.imageio.ImageIO.write(bufferedImage, "jpg", bos);
            byte[] bs = bos.toByteArray();
            Headers hds = t.getResponseHeaders();
            hds.set("content-type", "image/jpeg");
            hds.set("cache-control", "no-cache");
            hds.set("cache-control", "no-store");
            t.sendResponseHeaders(200, bs.length);
            OutputStream os = t.getResponseBody();
            os.write(bs);
            os.close();
        }
    }

    public static BufferedImage generateTextImage(int w, int h, String text) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        try {
            g.setFont(new Font(null, Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int startx = (w - textWidth) / 2;
            if (startx < 0)
                startx = 0;
            g.drawString(text, startx, h / 2);
        } catch (Throwable e) {
            s_logger.warn("Problem in generating text to thumnail image, return blank image");
        }
        return img;
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
}
