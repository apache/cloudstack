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

package org.apache.cloudstack.network;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface Ipv4GuestSubnetNetworkMap extends Identity, InternalIdentity {
    Date getAllocated();

    Date getCreated();

    enum State {
        Allocating, // The subnet will be assigned to a network
        Allocated,  // The subnet is in use.
        Releasing,  // The subnet is being released.
        Free        // The subnet is ready to be allocated.
    }

    Long getParentId();

    String getSubnet();

    Long getVpcId();

    Long getNetworkId();

    State getState();

}
