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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * ConsoleProxyGCThread does house-keeping work for the process, it helps cleanup log files,
 * recycle idle client sessions without front-end activities and report client stats to external
 * management software
 */
public class ConsoleProxyGCThread extends Thread {
    protected Logger logger = LogManager.getLogger(ConsoleProxyGCThread.class);

    private final static int MAX_SESSION_IDLE_SECONDS = 180;

    private final Map<String, ConsoleProxyClient> connMap;
    private final Set<String> removedSessionsSet;
    private long lastLogScan = 0;

    public ConsoleProxyGCThread(Map<String, ConsoleProxyClient> connMap, Set<String> removedSet) {
        this.connMap = connMap;
        this.removedSessionsSet = removedSet;
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
                        logger.info("[ignored]"
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

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("connMap=%s, removedSessions=%s", connMap, removedSessionsSet));
            }
            Set<String> e = connMap.keySet();
            Iterator<String> iterator = e.iterator();
            while (iterator.hasNext()) {
                String key;
                ConsoleProxyClient client;

                synchronized (connMap) {
                    key = iterator.next();
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
                logger.info("Dropping " + client + " which has not been used for " + seconds_unused + " seconds");
                client.closeClient();
            }

            if (bReportLoad || System.currentTimeMillis() - lastReportTick > 5000) {
                // report load changes
                ConsoleProxyClientStatsCollector collector = new ConsoleProxyClientStatsCollector(connMap);
                collector.setRemovedSessions(new ArrayList<>(removedSessionsSet));
                String loadInfo = collector.getStatsReport();
                ConsoleProxy.reportLoadInfo(loadInfo);
                lastReportTick = System.currentTimeMillis();
                synchronized (removedSessionsSet) {
                    removedSessionsSet.clear();
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Report load change : " + loadInfo);
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                logger.debug("[ignored] Console proxy was interrupted during GC.");
            }
        }
    }
}
