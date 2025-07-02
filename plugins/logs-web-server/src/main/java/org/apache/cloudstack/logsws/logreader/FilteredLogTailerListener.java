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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class FilteredLogTailerListener  extends TailerListenerAdapter {
    private final List<String> filters;
    private final Channel channel;
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
        if (isLastLineValid && !line.startsWith("2025")) {
            return true;
        }
        for (String filter : filters) {
            if (line.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    public FilteredLogTailerListener(List<String> filters, Channel channel) {
        this.filters = filters;
        this.channel = channel;
        isFilterEmpty = CollectionUtils.isEmpty(filters);
        isLastLineValid = false;
    }

    @Override
    public void handle(String line) {
        // Check if the line contains the filter string
        if (isValidLine(line, isFilterEmpty, isLastLineValid, filters)) {
            channel.writeAndFlush(new TextWebSocketFrame(line));
            isLastLineValid = true;
        } else {
            isLastLineValid = false;
        }
    }
}
