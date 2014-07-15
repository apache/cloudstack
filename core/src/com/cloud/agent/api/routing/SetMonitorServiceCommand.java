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

import java.util.List;

import com.cloud.agent.api.to.MonitorServiceTO;

/**
 *
 * AccessDetails allow different components to put in information about
 * how to access the components inside the command.
 */
public class SetMonitorServiceCommand extends NetworkElementCommand {
    MonitorServiceTO[] services;

    protected SetMonitorServiceCommand() {
    }

    public SetMonitorServiceCommand(List<MonitorServiceTO> services) {
        this.services = services.toArray(new MonitorServiceTO[services.size()]);
    }

    public MonitorServiceTO[] getRules() {
        return services;
    }

    public String getConfiguration() {

        StringBuilder sb = new StringBuilder();
        for (MonitorServiceTO service : services) {
            sb.append("[").append(service.getService()).append("]").append(":");
            sb.append("processname=").append(service.getProcessname()).append(":");
            sb.append("servicename=").append(service.getServiceName()).append(":");
            sb.append("pidfile=").append(service.getPidFile()).append(":");
            sb.append(",");
        }

        return sb.toString();
    }
}
