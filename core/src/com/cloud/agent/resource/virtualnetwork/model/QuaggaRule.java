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

package com.cloud.agent.resource.virtualnetwork.model;

public class QuaggaRule extends ConfigBase {
    private String zebraConfig;
    private String ospfConfig;
    private String tmpCfgFilePath;
    private String tmpCfgFileName;
    private String routerIp;

    public QuaggaRule() {
        super(QuaggaRule.QUAGGA);
    }

    public QuaggaRule(final String zebraConfig, final String ospfConfig, final String tmpCfgFilePath, final String tmpCfgFileName, final String routerIp) {
        super(QuaggaRule.QUAGGA);
        this.zebraConfig = zebraConfig;
        this.ospfConfig = ospfConfig;
        this.tmpCfgFilePath = tmpCfgFilePath;
        this.tmpCfgFileName = tmpCfgFileName;
        this.routerIp = routerIp;
    }

    public String getZebraConfig() {
        return zebraConfig;
    }

    public void setZebraConfig(String zebraConfig) {
        this.zebraConfig = zebraConfig;
    }

    public String getOspfConfig() {
        return ospfConfig;
    }

    public void setOspfConfig(String ospfConfig) {
        this.ospfConfig = ospfConfig;
    }

    public String getTmpCfgFilePath() {
        return tmpCfgFilePath;
    }

    public void setTmpCfgFilePath(String tmpCfgFilePath) {
        this.tmpCfgFilePath = tmpCfgFilePath;
    }

    public String getTmpCfgFileName() {
        return tmpCfgFileName;
    }

    public void setTmpCfgFileName(String tmpCfgFileName) {
        this.tmpCfgFileName = tmpCfgFileName;
    }

    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    @Override
    public String toString() {
        return "QuaggaRule [zebraConfig=" + (zebraConfig) + ", ospfConfig=" + (ospfConfig) + ", tmpCfgFilePath=" + tmpCfgFilePath
                + ", tmpCfgFileName=" + tmpCfgFileName + ", routerIp=" + routerIp + "]";
    }
}
