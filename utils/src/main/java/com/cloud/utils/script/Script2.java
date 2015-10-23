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

import java.util.HashMap;

import org.apache.log4j.Logger;

public class Script2 extends Script {
    HashMap<String, ParamType> _params = new HashMap<String, ParamType>();

    public static enum ParamType {
        NORMAL, PASSWORD,
    }

    public Script2(String command, Logger logger) {
        this(command, 0, logger);
    }

    public Script2(String command, long timeout, Logger logger) {
        super(command, timeout, logger);
    }

    public void add(String param, ParamType type) {
        _params.put(param, type);
        super.add(param);
    }

    @Override
    public void add(String param) {
        add(param, ParamType.NORMAL);
    }

    private ParamType getType(String cmd) {
        return _params.get(cmd);
    }

    @Override
    protected String buildCommandLine(String[] command) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            String cmd = command[i];
            ParamType type = getType(cmd);
            if (type == ParamType.PASSWORD) {
                builder.append("******").append(" ");
            } else {
                builder.append(command[i]).append(" ");
            }
        }

        return builder.toString();
    }
}
