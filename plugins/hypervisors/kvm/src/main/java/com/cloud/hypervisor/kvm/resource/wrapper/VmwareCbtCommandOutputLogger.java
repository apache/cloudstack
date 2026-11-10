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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;

class VmwareCbtCommandOutputLogger extends OutputInterpreter {

    private static final Pattern PASSWORD_ARGUMENT_PATTERN = Pattern.compile("(?i)(password=)([^\\s]+)");

    private final Logger logger;
    private final String logPrefix;
    private String lastOutputLine;
    private String lastImportantOutputLine;

    VmwareCbtCommandOutputLogger(Logger logger, String logPrefix) {
        this.logger = logger;
        this.logPrefix = logPrefix;
    }

    @Override
    public boolean drain() {
        return true;
    }

    @Override
    public String interpret(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String safeLine = sanitize(line);
            logger.info(StringUtils.isNotBlank(logPrefix) ? String.format("%s %s", logPrefix, safeLine) : safeLine);
            captureOutputLine(safeLine);
        }
        return null;
    }

    String getLastRelevantOutputLine() {
        return StringUtils.defaultIfBlank(lastImportantOutputLine, lastOutputLine);
    }

    private void captureOutputLine(String line) {
        String trimmedLine = StringUtils.trimToNull(line);
        if (trimmedLine == null) {
            return;
        }
        lastOutputLine = trimmedLine;
        if (isImportantOutputLine(trimmedLine)) {
            lastImportantOutputLine = trimmedLine;
        }
    }

    private boolean isImportantOutputLine(String line) {
        String lowerCaseLine = StringUtils.lowerCase(line);
        return StringUtils.containsAny(lowerCaseLine,
                "error", "failed", "unable", "cannot", "permission denied", "no such file",
                "not found", "file is empty", "traceback", "exception");
    }

    private String sanitize(String line) {
        return PASSWORD_ARGUMENT_PATTERN.matcher(StringUtils.defaultString(line)).replaceAll("$1******");
    }
}
