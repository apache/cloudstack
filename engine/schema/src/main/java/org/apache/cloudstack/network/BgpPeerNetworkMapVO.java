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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

@Entity
@Table(name = "bgp_peer_network_map")
public class BgpPeerNetworkMapVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "bgp_peer_id")
    private long bgpPeerId;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "state")
    private BgpPeer.State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected BgpPeerNetworkMapVO() {
    }

    public BgpPeerNetworkMapVO(long bgpPeerId, Long networkId, Long vpcId, BgpPeer.State state) {
        this.bgpPeerId = bgpPeerId;
        this.networkId = networkId;
        this.vpcId = vpcId;
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getBgpPeerId() {
        return bgpPeerId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public BgpPeer.State getState() {
        return state;
    }

    public void setState(BgpPeer.State state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
}
