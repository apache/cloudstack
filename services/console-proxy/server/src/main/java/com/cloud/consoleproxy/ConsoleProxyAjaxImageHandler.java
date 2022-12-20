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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.cloud.consoleproxy.util.Logger;

public class ConsoleProxyAjaxImageHandler implements HttpHandler {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyAjaxImageHandler.class);

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            if (s_logger.isDebugEnabled())
                s_logger.debug("AjaxImageHandler " + t.getRequestURI());

            long startTick = System.currentTimeMillis();

            doHandle(t);

            if (s_logger.isDebugEnabled())
                s_logger.debug(t.getRequestURI() + "Process time " + (System.currentTimeMillis() - startTick) + " ms");
        } catch (IOException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            s_logger.warn("Exception, ", e);
            t.sendResponseHeaders(400, -1);     // bad request
        } catch (OutOfMemoryError e) {
            s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
            System.exit(1);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception, ", e);
            t.sendResponseHeaders(500, -1);     // server error
        } finally {
            t.close();
        }
    }

    private void doHandle(HttpExchange t) throws Exception, IllegalArgumentException {
        String queries = t.getRequestURI().getQuery();
        Map<String, String> queryMap = ConsoleProxyHttpHandlerHelper.getQueryMap(queries);

        String host = queryMap.get("host");
        String portStr = queryMap.get("port");
        String sid = queryMap.get("sid");
        String tag = queryMap.get("tag");
        String ticket = queryMap.get("ticket");
        String keyStr = queryMap.get("key");
        String console_url = queryMap.get("consoleurl");
        String console_host_session = queryMap.get("sessionref");
        String w = queryMap.get("w");
        String h = queryMap.get("h");

        int key = 0;
        int width = 144;
        int height = 110;

        if (tag == null)
            tag = "";

        int port;
        if (host == null || portStr == null || sid == null)
            throw new IllegalArgumentException();

        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            s_logger.warn("Invalid numeric parameter in query string: " + portStr);
            throw new IllegalArgumentException(e);
        }

        try {
            if (keyStr != null)
                key = Integer.parseInt(keyStr);
            if (null != w)
                width = Integer.parseInt(w);

            if (null != h)
                height = Integer.parseInt(h);

        } catch (NumberFormatException e) {
            s_logger.warn("Invalid numeric parameter in query string: " + keyStr);
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

        if (key == 0) {
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
        } else {
            AjaxFIFOImageCache imageCache = viewer.getAjaxImageCache();
            byte[] img = imageCache.getImage(key);

            if (img != null) {
                Headers hds = t.getResponseHeaders();
                hds.set("content-type", "image/jpeg");
                t.sendResponseHeaders(200, img.length);

                OutputStream os = t.getResponseBody();
                try {
                    os.write(img, 0, img.length);
                } finally {
                    os.close();
                }
            } else {
                if (s_logger.isInfoEnabled())
                    s_logger.info("Image has already been swept out, key: " + key);
                t.sendResponseHeaders(404, -1);
            }
        }
    }
}
