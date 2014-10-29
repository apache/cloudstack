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

import com.cloud.network.router.VirtualRouter.RedundantState;

public class CheckRouterAnswer extends Answer {
    public static final String ROUTER_NAME = "router.name";
    public static final String ROUTER_IP = "router.ip";
    RedundantState state;
    boolean isBumped;

    protected CheckRouterAnswer() {
    }

    public CheckRouterAnswer(CheckRouterCommand cmd, String details, boolean parse) {
        super(cmd, true, details);
        if (parse) {
            if (!parseDetails(details)) {
                this.result = false;
            }
        }
    }

    public CheckRouterAnswer(CheckRouterCommand cmd, String details) {
        super(cmd, false, details);
    }

    protected boolean parseDetails(String details) {
        String[] lines = details.split("&");
        if (lines.length != 2) {
            return false;
        }
        if (lines[0].startsWith("Status: MASTER")) {
            state = RedundantState.MASTER;
        } else if (lines[0].startsWith("Status: BACKUP")) {
            state = RedundantState.BACKUP;
        } else if (lines[0].startsWith("Status: FAULT")) {
            state = RedundantState.FAULT;
        } else {
            state = RedundantState.UNKNOWN;
        }
        if (lines[1].startsWith("Bumped: YES")) {
            isBumped = true;
        } else {
            isBumped = false;
        }
        return true;
    }

    public void setState(RedundantState state) {
        this.state = state;
    }

    public RedundantState getState() {
        return state;
    }

    public boolean isBumped() {
        return isBumped;
    }

    public void setIsBumped(boolean isBumped) {
        this.isBumped = isBumped;
    }

}
