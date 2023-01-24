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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.cloud.consoleproxy.util.Logger;

public class ConsoleProxyCmdHandler implements HttpHandler {
    protected Logger logger = Logger.getLogger(getClass());

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            Thread.currentThread().setName("Cmd Thread " + Thread.currentThread().getId() + " " + t.getRemoteAddress());
            logger.info("CmdHandler " + t.getRequestURI());
            doHandle(t);
        } catch (Exception e) {
            logger.error(e.toString(), e);
            String response = "Not found";
            t.sendResponseHeaders(404, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (OutOfMemoryError e) {
            logger.error("Unrecoverable OutOfMemory Error, exit and let it be re-launched");
            System.exit(1);
        } catch (Throwable e) {
            logger.error(e.toString(), e);
        } finally {
            t.close();
        }
    }

    public void doHandle(HttpExchange t) throws Exception {
        String path = t.getRequestURI().getPath();
        int i = path.indexOf("/", 1);
        String cmd = path.substring(i + 1);
        logger.info("Get CMD request for " + cmd);
        if (cmd.equals("getstatus")) {
            ConsoleProxyClientStatsCollector statsCollector = ConsoleProxy.getStatsCollector();

            Headers hds = t.getResponseHeaders();
            hds.set("content-type", "text/plain");
            t.sendResponseHeaders(200, 0);
            OutputStreamWriter os = new OutputStreamWriter(t.getResponseBody(),"UTF-8");
            statsCollector.getStatsReport(os);
            os.close();
        }
    }
}
