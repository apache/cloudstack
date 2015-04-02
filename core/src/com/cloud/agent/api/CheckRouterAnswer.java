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

    protected CheckRouterAnswer() {
    }

    public CheckRouterAnswer(final CheckRouterCommand cmd, final String details, final boolean parse) {
        super(cmd, true, details);
        if (parse) {
            if (!parseDetails(details)) {
                result = false;
            }
        }
    }

    public CheckRouterAnswer(final CheckRouterCommand cmd, final String details) {
        super(cmd, false, details);
    }

    protected boolean parseDetails(final String details) {
        if (details == null || "".equals(details.trim())) {
            state = RedundantState.UNKNOWN;
            return false;
        }
        if (details.startsWith("Status: MASTER")) {
            state = RedundantState.MASTER;
        } else if (details.startsWith("Status: BACKUP")) {
            state = RedundantState.BACKUP;
        } else if (details.startsWith("Status: FAULT")) {
            state = RedundantState.FAULT;
        } else {
            state = RedundantState.UNKNOWN;
        }
        return true;
    }

    public void setState(final RedundantState state) {
        this.state = state;
    }

    public RedundantState getState() {
        return state;
    }
}