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
package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class NsxNetworkCommand extends NsxCommand {
    private Long networkResourceId;
    private String networkResourceName;
    private boolean isResourceVpc;
    private Long vmId;
    private String publicIp;
    private String vmIp;

    public NsxNetworkCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                             boolean isResourceVpc, Long vmId, String publicIp, String vmIp) {
        super(domainId, accountId, zoneId);
        this.networkResourceId = networkResourceId;
        this.networkResourceName = networkResourceName;
        this.isResourceVpc = isResourceVpc;
        this.vmId = vmId;
        this.publicIp = publicIp;
        this.vmIp = vmIp;
    }

    public NsxNetworkCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                             boolean isResourceVpc) {
        super(domainId, accountId, zoneId);
        this.networkResourceId = networkResourceId;
        this.networkResourceName = networkResourceName;
        this.isResourceVpc = isResourceVpc;
    }

    public NsxNetworkCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                            boolean isResourceVpc, Long vmId) {
        this(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc);
        this.vmId = vmId;
    }

    public Long getNetworkResourceId() {
        return networkResourceId;
    }

    public void setNetworkResourceId(long networkResourceId) {
        this.networkResourceId = networkResourceId;
    }

    public String getNetworkResourceName() {
        return networkResourceName;
    }

    public void setNetworkResourceName(String networkResourceName) {
        this.networkResourceName = networkResourceName;
    }

    public boolean isResourceVpc() {
        return isResourceVpc;
    }

    public void setResourceVpc(boolean resourceVpc) {
        isResourceVpc = resourceVpc;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NsxNetworkCommand that = (NsxNetworkCommand) o;
        return networkResourceId == that.networkResourceId && vmId == that.vmId &&
                Objects.equals(networkResourceName, that.networkResourceName) && Objects.equals(publicIp, that.publicIp)
                && Objects.equals(vmIp, that.vmIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkResourceId, networkResourceName, vmId, publicIp, vmIp);
    }
}
