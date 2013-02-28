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

import com.cloud.network.Networks.TrafficType;

/* User can provide a Label, while configuring a zone, to specify
 * a physical network that is to be used for a traffic type defined
 * by CloudStack. See the enum data type TrafficType. This label is
 * called Traffic label. This might encapsulate physical network
 * specific properties like VLAN ID, name of virtual network object or more.
 * The name of virtual network object is dependent on type of hypervisor.
 * For example it is name of xenserver bridge in case of XenServer and
 * name of virtual switch in case of VMware hypervisor
 */
public interface TrafficLabel {

    public TrafficType getTrafficType();

    public String getNetworkLabel();

}
