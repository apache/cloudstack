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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.cloud.consoleproxy.util.Logger;

public class ConsoleProxyAjaxImageHandler extends AbstractHandler {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyAjaxImageHandler.class);

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {
            if (s_logger.isDebugEnabled())
                s_logger.debug("AjaxImageHandler " + request.getRequestURI());

            long startTick = System.currentTimeMillis();

            doHandle(request, httpServletResponse);

            if (s_logger.isDebugEnabled())
                s_logger.debug(request.getRequestURI() + "Process time " + (System.currentTimeMillis() - startTick) + " ms");
        } catch (IOException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            s_logger.warn("Exception, ", e);
            httpServletResponse.setStatus(400);     // bad request
        } catch (OutOfMemoryError e) {
            s_logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
            System.exit(1);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception, ", e);
            httpServletResponse.setStatus(500);
        } finally {
            request.setHandled(true);
        }
    }

    private void doHandle(Request request, HttpServletResponse httpServletResponse) throws Exception, IllegalArgumentException {
        String queries = request.getHttpURI().getQuery();
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
            httpServletResponse.addHeader("Content-Type", "image/jpeg");
            httpServletResponse.addHeader("Cache-Control", "no-cache");
            httpServletResponse.addHeader("Cache-Control", "no-store");
            httpServletResponse.setStatus(200);
            OutputStream os = httpServletResponse.getOutputStream();
            os.write(bs);
            os.close();
        } else {
            AjaxFIFOImageCache imageCache = viewer.getAjaxImageCache();
            byte[] img = imageCache.getImage(key);

            if (img != null) {
                httpServletResponse.setHeader("Content-Type", "image/jpeg");
                httpServletResponse.setStatus(200);

                OutputStream os = httpServletResponse.getOutputStream();
                try {
                    os.write(img, 0, img.length);
                } finally {
                    os.close();
                }
            } else {
                if (s_logger.isInfoEnabled())
                    s_logger.info("Image has already been swept out, key: " + key);
                httpServletResponse.setStatus(404);
            }
        }
    }
}
