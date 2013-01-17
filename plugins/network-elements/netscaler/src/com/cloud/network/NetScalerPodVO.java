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

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * NetScalerPodVO contains information about a EIP deployment where on datacenter L3 router a PBR (policy
 * based routing) is setup between a POD's subnet IP range to a NetScaler device. This VO object 
 * represents a mapping between a POD and NetScaler device where PBR is setup. 
 *
 */
@Entity
@Table(name="netscaler_pod_ref")
public class NetScalerPodVO implements InternalIdentity {
 
    @Column(name="external_load_balancer_device_id")
    private long netscalerDeviceId;

    @Id
    @Column(name="id")
    private long id;

    @Column(name="pod_id")
    private long podId;

    public NetScalerPodVO() {
        
    }

    public NetScalerPodVO(long netscalerDeviceId, long podId) {
    	this.netscalerDeviceId = netscalerDeviceId;
    	this.podId = podId;
    }

    public long getId() {
        return id;
    }

    public long getPodId() {
        return podId;
    }

    public long getNetscalerDeviceId() {
    	return netscalerDeviceId;
    }
}
