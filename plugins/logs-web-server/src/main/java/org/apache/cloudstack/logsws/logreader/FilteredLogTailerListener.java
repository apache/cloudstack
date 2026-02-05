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

package org.apache.cloudstack.logsws.logreader;

import java.time.LocalDate;
import java.util.List;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;

public class FilteredLogTailerListener extends TailerListenerAdapter {
    private final WebSocketSession session;
    private final List<String> filters;
    private final boolean isFilterEmpty;
    private boolean isLastLineValid;

    public static boolean isValidLine(String line, boolean isFilterEmpty,
                                      boolean isLastLineValid, List<String> filters) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        if (isFilterEmpty) {
            return true;
        }

        // ToDo: Improve. If the last line was valid and the current line does not start with YEAR-MONTH-
        //  consider it valid
        String logLinePrefix = String.format("%d-%02d-", LocalDate.now().getYear(),
                LocalDate.now().getMonthValue());
        if (isLastLineValid && !line.startsWith(logLinePrefix)) {
            return true;
        }

        for (String filter : filters) {
            if (line.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    public FilteredLogTailerListener(WebSocketSession session, List<String> filters) {
        this.session = session;
        this.filters = filters;
        isFilterEmpty = CollectionUtils.isEmpty(filters);
        isLastLineValid = false;
    }

    @Override
    public void handle(String line) {
        // Check if the line contains the filter string
        if (isValidLine(line, isFilterEmpty, isLastLineValid, filters)) {
            session.sendText(line);
            isLastLineValid = true;
        } else {
            isLastLineValid = false;
        }
    }

    @Override
    public void fileNotFound() {
        session.sendText("Log file not found.");
    }

    @Override
    public void fileRotated() {
        session.sendText("Log file rotated.");
    }

    @Override
    public void handle(Exception ex) {
        session.sendText("Tailer error: " + ex.getMessage());
    }
}
