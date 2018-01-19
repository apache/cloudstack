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

import java.util.List;

import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface NetworkACLItem extends InternalIdentity, Identity, Displayable {

    String getUuid();

    Action getAction();

    int getNumber();

    enum State {
        Staged, // Rule been created but has never got through network rule conflict detection.  Rules in this state can not be sent to network elements.
        Add,    // Add means the rule has been created and has gone through network rule conflict detection.
        Active, // Rule has been sent to the network elements and reported to be active.
        Revoke  // Revoke means this rule has been revoked. If this rule has been sent to the network elements, the rule will be deleted from database.
    }

    enum TrafficType {
        Ingress, Egress
    }

    enum Action {
        Allow, Deny
    }

    /**
     * @return first port of the source port range.
     */
    Integer getSourcePortStart();

    /**
     * @return last port of the source prot range.  If this is null, that means only one port is mapped.
     */
    Integer getSourcePortEnd();

    /**
     * @return protocol to open these ports for.
     */
    String getProtocol();

    State getState();

    long getAclId();

    Integer getIcmpCode();

    Integer getIcmpType();

    List<String> getSourceCidrList();

    /**
     * @return
     */
    TrafficType getTrafficType();

    @Override
    boolean isDisplay();

}
