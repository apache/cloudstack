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

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.net.Ip;

/**
 *
 * - Allocated = null
 * - AccountId = null
 * - DomainId = null
 *
 * - State = Allocated
 * - AccountId = account owner.
 * - DomainId = domain of the account owner.
 * - Allocated = time it was allocated.
 */
public interface IpAddress extends ControlledEntity, Identity, InternalIdentity, Displayable {
    enum State {
        Allocating, // The IP Address is being propagated to other network elements and is not ready for use yet.
        Allocated, // The IP address is in used.
        Releasing, // The IP address is being released for other network elements and is not ready for allocation.
        Reserved,   // The IP address is reserved and is not ready for allocation.
        Free // The IP address is ready to be allocated.
    }

    enum Purpose {
        StaticNat, Lb
    }

    long getDataCenterId();

    Ip getAddress();

    Date getAllocatedTime();

    boolean isSourceNat();

    long getVlanId();

    boolean isOneToOneNat();

    State getState();

    boolean readyToUse();

    Long getAssociatedWithNetworkId();

    Long getAssociatedWithVmId();

    public Long getPhysicalNetworkId();

    void setState(IpAddress.State state);

    Long getAllocatedToAccountId();

    Long getAllocatedInDomainId();

    boolean getSystem();

    Long getVpcId();

    String getVmIp();

    boolean isPortable();

    Long getNetworkId();

    boolean isDisplay();

    public Date getRemoved();

    public Date getCreated();

    State getRuleState();

    void setRuleState(State ruleState);

}
