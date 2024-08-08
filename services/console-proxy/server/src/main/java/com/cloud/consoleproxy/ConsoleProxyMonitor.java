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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

//
//
// I switched to a simpler solution to monitor only unrecoverable exceptions, under these cases, console proxy process will exit
// itself and the shell script will re-launch console proxy
//
public class ConsoleProxyMonitor {
    protected Logger logger = LogManager.getLogger(getClass());

    private String[] _argv;
    private Map<String, String> _argMap = new HashMap<String, String>();

    private volatile Process _process;
    private boolean _quit = false;

    public ConsoleProxyMonitor(String[] argv) {
        _argv = argv;

        for (String arg : _argv) {
            String[] tokens = arg.split("=");
            if (tokens.length == 2) {
                logger.info("Add argument " + tokens[0] + "=" + tokens[1] + " to the argument map");

                _argMap.put(tokens[0].trim(), tokens[1].trim());
            } else {
                logger.warn("unrecognized argument, skip adding it to argument map");
            }
        }
    }

    private void run() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                _quit = true;
                onShutdown();
            }
        });

        while (!_quit) {
            String cmdLine = getLaunchCommandLine();

            logger.info("Launch console proxy process with command line: " + cmdLine);

            try {
                _process = Runtime.getRuntime().exec(cmdLine);
            } catch (IOException e) {
                logger.error("Unexpected exception ", e);
                System.exit(1);
            }

            boolean waitSucceeded = false;
            int exitCode = 0;
            while (!waitSucceeded) {
                try {
                    exitCode = _process.waitFor();
                    waitSucceeded = true;

                    if (logger.isInfoEnabled())
                        logger.info("Console proxy process exits with code: " + exitCode);
                } catch (InterruptedException e) {
                    if (logger.isInfoEnabled())
                        logger.info("InterruptedException while waiting for termination of console proxy, will retry");
                }
            }
        }
    }

    private String getLaunchCommandLine() {
        StringBuffer sb = new StringBuffer("java ");
        String jvmOptions = _argMap.get("jvmoptions");

        if (jvmOptions != null)
            sb.append(jvmOptions);

        for (Map.Entry<String, String> entry : _argMap.entrySet()) {
            if (!"jvmoptions".equalsIgnoreCase(entry.getKey()))
                sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }

    private void onShutdown() {
        if (_process != null) {
            if (logger.isInfoEnabled())
                logger.info("Console proxy monitor shuts dwon, terminate console proxy process");
            _process.destroy();
        }
    }

    private static void configLog4j() {
        URL configUrl = System.class.getResource("/conf/log4j-cloud.xml");
        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("log4j-cloud.xml");

        if (configUrl == null)
            configUrl = ClassLoader.getSystemResource("conf/log4j-cloud.xml");

        if (configUrl != null) {
            try {
                System.out.println("Configure log4j using " + configUrl.toURI().toString());
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }

            try {
                File file = new File(configUrl.toURI());

                System.out.println("Log4j configuration from : " + file.getAbsolutePath());
                Configurator.initialize(null, file.getAbsolutePath());
            } catch (URISyntaxException e) {
                System.out.println("Unable to convert log4j configuration Url to URI");
            }
        } else {
            System.out.println("Configure log4j with default properties");
        }
    }

    public static void main(String[] argv) {
        configLog4j();
        (new ConsoleProxyMonitor(argv)).run();
    }
}
