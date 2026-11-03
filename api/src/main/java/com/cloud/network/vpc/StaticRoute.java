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
package com.cloud.network.vpc;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface StaticRoute extends ControlledEntity, Identity, InternalIdentity {
    enum State {
        Staged, // route been created but has never got through network rule conflict detection.  Routes in this state can not be sent to VPC virtual router.
        Add,    // Add means the route has been created and has gone through network rule conflict detection.
        Active, // Route has been sent to the VPC router and reported to be active.
        Update,
        Revoke,  // Revoke means this route has been revoked. If this route has been sent to the VPC router, the route will be deleted from database.
        Deleting // rule has been revoked and is scheduled for deletion
    }

    /**
     * @return
     */
    Long getVpcGatewayId();

    String getNextHop();

    /**
     * @return
     */
    String getCidr();

    /**
     * @return
     */
    State getState();

    /**
     * @return
     */
    Long getVpcId();
}
