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
package org.apache.cloudstack.resource;

import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;

import java.util.Objects;

public class NsxOpObject {
    VpcVO vpcVO;
    NetworkVO networkVO;
    long accountId;
    long domainId;
    long zoneId;

    public VpcVO getVpcVO() {
        return vpcVO;
    }

    public void setVpcVO(VpcVO vpcVO) {
        this.vpcVO = vpcVO;
    }

    public NetworkVO getNetworkVO() {
        return networkVO;
    }

    public void setNetworkVO(NetworkVO networkVO) {
        this.networkVO = networkVO;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getNetworkResourceName() {
        return Objects.nonNull(vpcVO) ? vpcVO.getName() : networkVO.getName();
    }

    public boolean isVpcResource() {
        return Objects.nonNull(vpcVO);
    }

    public long getNetworkResourceId() {
        return Objects.nonNull(vpcVO) ? vpcVO.getId() : networkVO.getId();
    }

    public static final class Builder {
        VpcVO vpcVO;
        NetworkVO networkVO;
        long accountId;
        long domainId;
        long zoneId;

        public Builder() {
            // Default constructor
        }

        public Builder vpcVO(VpcVO vpcVO) {
            this.vpcVO = vpcVO;
            return this;
        }

        public Builder networkVO(NetworkVO networkVO) {
            this.networkVO = networkVO;
            return this;
        }

        public Builder domainId(long domainId) {
            this.domainId = domainId;
            return this;
        }

        public Builder accountId(long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder zoneId(long zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public NsxOpObject build() {
            NsxOpObject object = new NsxOpObject();
            object.setVpcVO(this.vpcVO);
            object.setNetworkVO(this.networkVO);
            object.setDomainId(this.domainId);
            object.setAccountId(this.accountId);
            object.setZoneId(this.zoneId);
            return object;
        }
    }
}
