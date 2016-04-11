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

package com.cloud.network.nicira;

public class L2GatewayAttachment extends Attachment {

    private String l2GatewayServiceUuid;
    private final String type = "L2GatewayAttachment";
    private Long vlanId;

    public L2GatewayAttachment(String l2GatewayServiceUuid) {
        this.l2GatewayServiceUuid = l2GatewayServiceUuid;
    }

    public L2GatewayAttachment(final String l2GatewayServiceUuid, final long vlanId) {
        this.l2GatewayServiceUuid = l2GatewayServiceUuid;
        this.vlanId = vlanId;
    }

    public String getL2GatewayServiceUuid() {
        return l2GatewayServiceUuid;
    }
    public void setL2GatewayServiceUuid(String l2GatewayServiceUuid) {
        this.l2GatewayServiceUuid = l2GatewayServiceUuid;
    }
    public Long getVlanId() {
        return vlanId;
    }
    public void setVlanId(Long vlanId) {
        this.vlanId = vlanId;
    }
    public String getType() {
        return type;
    }

}
