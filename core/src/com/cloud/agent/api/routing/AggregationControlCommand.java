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

package com.cloud.agent.api.routing;

public class AggregationControlCommand extends NetworkElementCommand{
    public enum Action {
        Start,
        Finish,
        Cleanup,
    }

    private Action action;

    protected AggregationControlCommand() {
        super();
    }

    public AggregationControlCommand(Action action, String name, String ip, String guestIp) {
        super();
        this.action = action;
        this.setAccessDetail(NetworkElementCommand.ROUTER_NAME, name);
        this.setAccessDetail(NetworkElementCommand.ROUTER_IP, ip);
        this.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, guestIp);
    }

    public Action getAction() {
        return action;
    }
}
