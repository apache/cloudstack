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

package com.cloud.agent.api;

/**
 *
 */
public class ShutdownCommand extends Command {
    public static final String Requested = "sig.kill";
    public static final String Update = "update";
    public static final String Unknown = "unknown";
    public static final String DeleteHost = "deleteHost";

    private String reason;
    private String detail;

    protected ShutdownCommand() {
        super();
    }

    public ShutdownCommand(String reason, String detail) {
        super();
        this.reason = reason;
        this.detail = detail;
    }

    /**
     * @return return the reason the agent shutdown.  If Unknown, call getDetail() for any details.
     */
    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
