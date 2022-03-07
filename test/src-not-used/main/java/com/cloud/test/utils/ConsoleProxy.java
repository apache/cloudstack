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
import java.io.IOException;

import org.apache.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class ConsoleProxy implements Runnable {
    public static String proxyIp;
    private String command;
    private int connectionsMade;
    private long responseTime;
    public static final Logger s_logger = Logger.getLogger(ConsoleProxy.class.getClass());

    public ConsoleProxy(String port, String sid, String host) {
        this.command = "https://" + proxyIp + ".realhostip.com:8000/getscreen?w=100&h=75&host=" + host + "&port=" + port + "&sid=" + sid;
        s_logger.info("Command for a console proxy is " + this.command);
        this.connectionsMade = 0;
        this.responseTime = 0;
    }

    public int getConnectionsMade() {
        return this.connectionsMade;
    }

    public long getResponseTime() {
        return this.responseTime;
    }

    @Override
    public void run() {
        while (true) {

            Script myScript = new Script("wget");
            myScript.add(command);
            myScript.execute();
            long begin = System.currentTimeMillis();
            WgetInt process = new WgetInt();
            String response = myScript.execute(process);
            long end = process.getEnd();
            if (response != null) {
                s_logger.info("Content lenght is incorrect: " + response);
            }

            long duration = (end - begin);
            this.connectionsMade++;
            this.responseTime = this.responseTime + duration;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interrupted.");
            }

        }
    }

    public class WgetInt extends OutputInterpreter {
        private long end;

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            // TODO Auto-generated method stub
            end = System.currentTimeMillis();
            String status = null;
            String line = null;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf("Length:");
                if (index == -1) {
                    continue;
                } else {
                    int index1 = line.indexOf("Length: 1827");
                    if (index1 == -1) {
                        return status;
                    } else
                        status = line;
                }

            }
            return status;
        }

    }
}
