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
package com.cloud.baremetal.networkservice;

/**
 * Created by frank on 7/23/14.
 */
public class BaremetalVirtualRouterCommands {

    public abstract static class AgentCommand {
    }

    public abstract static class AgentResponse {
        private boolean success;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class PreparePxeCmd extends AgentCommand {
        private String guestMac;
        private String templateUuid;
        private String kickStartUrl;
        private String initrdUrl;
        private String kernelUrl;

        public String getTemplateUuid() {
            return templateUuid;
        }

        public void setTemplateUuid(String templateUuid) {
            this.templateUuid = templateUuid;
        }

        public String getGuestMac() {
            return guestMac;
        }

        public void setGuestMac(String guestMac) {
            this.guestMac = guestMac;
        }

        public String getKickStartUrl() {
            return kickStartUrl;
        }

        public void setKickStartUrl(String kickStartUrl) {
            this.kickStartUrl = kickStartUrl;
        }

        public String getInitrdUrl() {
            return initrdUrl;
        }

        public void setInitrdUrl(String initrdUrl) {
            this.initrdUrl = initrdUrl;
        }

        public String getKernelUrl() {
            return kernelUrl;
        }

        public void setKernelUrl(String kernelUrl) {
            this.kernelUrl = kernelUrl;
        }
    }

    public static class PreparePxeRsp extends AgentResponse {
    }

    public static class PrepareSourceNatCmd extends AgentCommand {
        private String internalStorageServerIp;
        private String managementNicIp;

        public String getInternalStorageServerIp() {
            return internalStorageServerIp;
        }

        public void setInternalStorageServerIp(String internalStorageServerIp) {
            this.internalStorageServerIp = internalStorageServerIp;
        }

        public String getManagementNicIp() {
            return managementNicIp;
        }

        public void setManagementNicIp(String managementNicIp) {
            this.managementNicIp = managementNicIp;
        }
    }

    public static class PrepareSourceNatRsp extends AgentResponse {
    }
}
