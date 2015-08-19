//
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
//

package com.cloud.utils.script;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 */
public abstract class OutputInterpreter {
    public boolean drain() {
        return false;
    }

    public String processError(BufferedReader reader) throws IOException {
        StringBuilder buff = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            buff.append(line);
        }
        return buff.toString();
    }

    public abstract String interpret(BufferedReader reader) throws IOException;

    public static final OutputInterpreter NoOutputParser = new OutputInterpreter() {
        @Override
        public String interpret(BufferedReader reader) throws IOException {
            return null;
        }
    };

    public static class TimedOutLogger extends OutputInterpreter {
        private static final Logger s_logger = Logger.getLogger(TimedOutLogger.class);
        Process _process;

        public TimedOutLogger(Process process) {
            _process = process;
        }

        @Override
        public boolean drain() {
            return true;
        }

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            StringBuilder buff = new StringBuilder();

            while (reader.ready()) {
                buff.append(reader.readLine());
            }

            _process.destroy();

            try {
                while (reader.ready()) {
                    buff.append(reader.readLine());
                }
            } catch (IOException e) {
                s_logger.info("[ignored] can not append line to buffer",e);
            }

            return buff.toString();
        }
    }

    public static class OutputLogger extends OutputInterpreter {
        Logger _logger;

        public OutputLogger(Logger logger) {
            _logger = logger;
        }

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            if (builder.length() > 0) {
                _logger.debug(builder.toString());
            }
            return null;
        }
    }

    public static class OneLineParser extends OutputInterpreter {
        String line = null;

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            line = reader.readLine();
            return null;
        }

        public String getLine() {
            return line;
        }
    };

    public static class AllLinesParser extends OutputInterpreter {
        String allLines = null;

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            allLines = builder.toString();
            return null;
        }

        public String getLines() {
            return allLines;
        }
    }

}
