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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.cloud.consoleproxy.util.Logger;
import com.cloud.utils.NumbersUtil;

public class NoVncConsoleHandler extends AbstractHandler {
    private static final Logger s_logger = Logger.getLogger(NoVncConsoleHandler.class);

    public NoVncConsoleHandler() {

    }

    private void doHandle(Request request, HttpServletResponse httpServletResponse) throws IOException {
        String queries = request.getHttpURI().getQuery();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Handle WebSocket Console request " + queries);
        }

        if (queries != null) {
            Map<String, String> queryMap = ConsoleProxyHttpHandlerHelper.getQueryMap(queries);
            String host = queryMap.get("host");
            String portStr = queryMap.get("port");
            String tag = queryMap.get("tag");
            ConsoleProxyClientParam param = new ConsoleProxyClientParam();
            param.setClientHostAddress(host);
            param.setClientHostPort(NumbersUtil.parseInt(portStr, 80));
            param.setClientTag(tag);
            ConsoleProxy.removeViewer(param);
        }

        sendResponse(httpServletResponse, "text/html", getFile("noVNC/start.html"));
    }

    private void sendResponse(HttpServletResponse httpServletResponse, String contentType, String response) throws IOException {
        httpServletResponse.setHeader("Content-Type", contentType);
        httpServletResponse.setStatus(200);

        try(OutputStream os = httpServletResponse.getOutputStream();) {
            os.write(response.getBytes());
        }
    }

    private String getFile(String fileName) {

        StringBuilder result = new StringBuilder("");
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line);
            }

            scanner.close();

        } catch (IOException e) {
            s_logger.error("IO exception occurred",e);
        }

        return result.toString();

    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("noVNC Console handler" + request.getRequestURI());
            }

            long startTick = System.currentTimeMillis();

            doHandle(request, httpServletResponse);

            if (s_logger.isTraceEnabled()) {
                s_logger.trace(request.getRequestURI() + " process time " + (System.currentTimeMillis() - startTick) + " ms");
            }
        } catch (IOException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            s_logger.warn("Exception, ", e);
            httpServletResponse.setStatus(400);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception, ", e);
            httpServletResponse.setStatus(500);
        } finally {
            request.setHandled(true);
        }
    }
}
