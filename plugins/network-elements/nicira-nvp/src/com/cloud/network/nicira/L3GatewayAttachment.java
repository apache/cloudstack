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

/**
 *
 */
public class L3GatewayAttachment extends Attachment {
    private String l3GatewayServiceUuid;
    private final String type = "L3GatewayAttachment";
    private Long vlanId;

    public L3GatewayAttachment(String l3GatewayServiceUuid) {
        this.l3GatewayServiceUuid = l3GatewayServiceUuid;
    }

    public L3GatewayAttachment(final String l3GatewayServiceUuid, final long vlanId) {
        this.l3GatewayServiceUuid = l3GatewayServiceUuid;
        this.vlanId = vlanId;
    }

    public String getL3GatewayServiceUuid() {
        return l3GatewayServiceUuid;
    }

    public void setL3GatewayServiceUuid(final String l3GatewayServiceUuid) {
        this.l3GatewayServiceUuid = l3GatewayServiceUuid;
    }

    public long getVlanId() {
        return vlanId;
    }

    public void setVlanId(long vlanId) {
        this.vlanId = vlanId;
    }

}
