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
package com.cloud.test.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class ProxyLoadTemp {
    public static final Logger s_logger = Logger.getLogger(ProxyLoadTemp.class.getClass());
    public static int numThreads = 0;
    public static ArrayList<ConsoleProxy> proxyList = new ArrayList<ConsoleProxy>();
    public static long begin;
    public static long end;
    public static long sum = 0;

    public ProxyLoadTemp() {
    }

    public static void main(String[] args) {
        begin = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(new ProxyLoadTemp()));
        ConsoleProxy.proxyIp = "172-16-1-101";

        try {
            BufferedReader consoleInput = new BufferedReader(new FileReader("console.input"));
            boolean eof = false;
            s_logger.info("Started reading file");
            while (!eof) {
                String line = consoleInput.readLine();
                s_logger.info("Line is " + line);
                if (line == null) {
                    s_logger.info("Line " + numThreads + " is null");
                    eof = true;
                } else {
                    String[] result = null;
                    try {
                        s_logger.info("Starting parsing line " + line);
                        result = parseLine(line, "[,]");
                        s_logger.info("Line retrieved from the file is " + result[0] + " " + result[1] + " " + result[2]);
                        ConsoleProxy proxy = new ConsoleProxy(result[0], result[1], result[2]);
                        proxyList.add(proxy);
                        new Thread(proxy).start();
                        numThreads++;

                    } catch (Exception ex) {
                        s_logger.warn(ex);
                    }
                }

            }
        } catch (Exception e) {
            s_logger.warn(e);
        }

    }

    public static class ShutdownThread extends Thread {
        ProxyLoadTemp temp;

        public ShutdownThread(ProxyLoadTemp temp) {
            this.temp = temp;
        }

        @Override
        public void run() {
            s_logger.info("Program was running in " + numThreads + " threads");

            for (int j = 0; j < proxyList.size(); j++) {
                long av = 0;
                if (proxyList.get(j).getConnectionsMade() != 0) {
                    av = proxyList.get(j).getResponseTime() / proxyList.get(j).getConnectionsMade();
                }
                s_logger.info("Information for " + j + " thread: Number of requests sent is " + proxyList.get(j).getConnectionsMade() + ". Average response time is " +
                    av + " milliseconds");
                sum = sum + av;

            }
            ProxyLoadTemp.end = System.currentTimeMillis();
            s_logger.info("Summary for all" + numThreads + " threads: Average response time is " + sum / numThreads + " milliseconds");
            s_logger.info("Test was running for " + (ProxyLoadTemp.end - ProxyLoadTemp.begin) / 1000 + " seconds");
        }
    }

    public static String[] parseLine(String line, String del) throws Exception {
        String del1 = del.substring(1, del.length() - 1);
        if (line.contains(del1) != true) {
            throw new Exception();
        } else {
            String[] token = line.split(del);
            return token;
        }

    }

}
