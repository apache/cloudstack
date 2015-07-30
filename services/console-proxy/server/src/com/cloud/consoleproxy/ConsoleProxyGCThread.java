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
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 *
 * ConsoleProxyGCThread does house-keeping work for the process, it helps cleanup log files,
 * recycle idle client sessions without front-end activities and report client stats to external
 * management software
 */
public class ConsoleProxyGCThread extends Thread {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyGCThread.class);

    private final static int MAX_SESSION_IDLE_SECONDS = 180;

    private final Hashtable<String, ConsoleProxyClient> connMap;
    private long lastLogScan = 0;

    public ConsoleProxyGCThread(Hashtable<String, ConsoleProxyClient> connMap) {
        this.connMap = connMap;
    }

    private void cleanupLogging() {
        if (lastLogScan != 0 && System.currentTimeMillis() - lastLogScan < 3600000)
            return;

        lastLogScan = System.currentTimeMillis();

        File logDir = new File("./logs");
        File files[] = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (System.currentTimeMillis() - file.lastModified() >= 86400000L) {
                    try {
                        file.delete();
                    } catch (Throwable e) {
                        s_logger.info("[ignored]"
                                + "failed to delete file: " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        boolean bReportLoad = false;
        long lastReportTick = System.currentTimeMillis();
        while (true) {
            cleanupLogging();
            bReportLoad = false;

            if (s_logger.isDebugEnabled())
                s_logger.debug("connMap=" + connMap);
            Enumeration<String> e = connMap.keys();
            while (e.hasMoreElements()) {
                String key;
                ConsoleProxyClient client;

                synchronized (connMap) {
                    key = e.nextElement();
                    client = connMap.get(key);
                }

                long seconds_unused = (System.currentTimeMillis() - client.getClientLastFrontEndActivityTime()) / 1000;
                if (seconds_unused < MAX_SESSION_IDLE_SECONDS) {
                    continue;
                }

                synchronized (connMap) {
                    connMap.remove(key);
                    bReportLoad = true;
                }

                // close the server connection
                s_logger.info("Dropping " + client + " which has not been used for " + seconds_unused + " seconds");
                client.closeClient();
            }

            if (bReportLoad || System.currentTimeMillis() - lastReportTick > 5000) {
                // report load changes
                String loadInfo = new ConsoleProxyClientStatsCollector(connMap).getStatsReport();
                ConsoleProxy.reportLoadInfo(loadInfo);
                lastReportTick = System.currentTimeMillis();

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Report load change : " + loadInfo);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                s_logger.debug("[ignored] Console proxy was interupted during GC.");
            }
        }
    }
}
