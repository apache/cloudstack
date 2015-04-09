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

import java.util.List;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.Pair;

/**
 *
 */
public interface PhysicalNetwork extends Identity, InternalIdentity {

    public enum State {
        Disabled, Enabled;
    }

    public enum IsolationMethod {
        VLAN, L3, GRE, STT, BCF_SEGMENT, MIDO, SSP, VXLAN, ODL, L3VPN, VSP, VCS;
    }

    public enum BroadcastDomainRange {
        POD, ZONE;
    }

    BroadcastDomainRange getBroadcastDomainRange();

    // TrafficType getTrafficType();

    long getDataCenterId();

    State getState();

    List<String> getTags();

    List<String> getIsolationMethods();

    Long getDomainId();

    List<Pair<Integer, Integer>> getVnet();

    String getVnetString();

    String getSpeed();

    String getName();

}
