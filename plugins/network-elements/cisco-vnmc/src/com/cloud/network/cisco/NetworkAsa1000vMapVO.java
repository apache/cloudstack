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
package com.cloud.network.cisco;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="network_asa1000v_map")
public class NetworkAsa1000vMapVO implements NetworkAsa1000vMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="network_id")
    private long networkId;

    @Column(name="asa1000v_id")
    private long asa1000vId;
    
    public NetworkAsa1000vMapVO() {
    }

    public NetworkAsa1000vMapVO(long networkId, long asa1000vId) {
        super();
        this.networkId = networkId;
        this.asa1000vId = asa1000vId;
    }

	@Override
	public long getId() {
		return id;
	}

	@Override
	public long getAsa1000vId() {
		return asa1000vId;
	}

	public void setAsa1000vId(long asa1000vId) {
		this.asa1000vId = asa1000vId;
	}

	@Override
	public long getNetworkId() {
		return networkId;
	}

	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}

}
