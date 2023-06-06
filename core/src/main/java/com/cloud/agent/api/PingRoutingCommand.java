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

import java.util.Map;

import com.cloud.host.Host;

public class PingRoutingCommand extends PingCommand {

    Map<String, HostVmStateReportEntry> _hostVmStateReport;

    boolean _gatewayAccessible = true;
    boolean _vnetAccessible = true;
    private Boolean hostHealthCheckResult;

    protected PingRoutingCommand() {
    }

    public PingRoutingCommand(Host.Type type, long id, Map<String, HostVmStateReportEntry> hostVmStateReport) {
        super(type, id);
        this._hostVmStateReport = hostVmStateReport;
    }

    public Map<String, HostVmStateReportEntry> getHostVmStateReport() {
        return this._hostVmStateReport;
    }

    public boolean isGatewayAccessible() {
        return _gatewayAccessible;
    }

    public void setGatewayAccessible(boolean gatewayAccessible) {
        _gatewayAccessible = gatewayAccessible;
    }

    public boolean isVnetAccessible() {
        return _vnetAccessible;
    }

    public void setVnetAccessible(boolean vnetAccessible) {
        _vnetAccessible = vnetAccessible;
    }

    public Boolean getHostHealthCheckResult() {
        return hostHealthCheckResult;
    }

    public void setHostHealthCheckResult(Boolean hostHealthCheckResult) {
        this.hostHealthCheckResult = hostHealthCheckResult;
    }
}
