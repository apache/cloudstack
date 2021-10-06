/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.utils.qemu;

import java.util.LinkedHashMap;
import java.util.Map;

public class QemuCommand {
    //Qemu agent commands
    public static final String AGENT_FREEZE = "guest-fsfreeze-freeze";
    public static final String AGENT_THAW = "guest-fsfreeze-thaw";
    public static final String AGENT_FREEZE_STATUS = "guest-fsfreeze-status";
    //Qemu monitor commands
    public static final String QEMU_QUERY_BLOCK_JOBS = "query-block-jobs";
    public static final String QEMU_BLOCK = "query-block";
    public static final String QEMU_DRIVE_BACKUP = "drive-backup";

    public static final String QEMU_CMD = "execute";

    public static Map<String, Object> executeQemuCommand(String command, Map<String, String> args ){
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(QEMU_CMD, command);
        if (args != null) {
            params.put("arguments", args);
        }
        return params;
    }
}
