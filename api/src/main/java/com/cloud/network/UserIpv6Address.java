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
package com.cloud.network;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * @author Sheng Yang
 *
 */
public interface UserIpv6Address extends ControlledEntity, Identity, InternalIdentity {
    enum State {
        Allocating, // The IP Address is being propagated to other network elements and is not ready for use yet.
        Allocated, // The IP address is in used.
        Releasing, // The IP address is being released for other network elements and is not ready for allocation.
        Free // The IP address is ready to be allocated.
    }

    long getDataCenterId();

    String getAddress();

    long getVlanId();

    State getState();

    Long getNetworkId();

    Long getSourceNetworkId();

    Long getPhysicalNetworkId();

    void setState(UserIpv6Address.State state);

    String getMacAddress();
}
