/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.network.vpc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "network_acl_item_cidrs")
public class NetworkACLItemCidrsVO implements InternalIdentity {
    private static final long serialVersionUID = 7805284475485494754L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "network_acl_item_id")
    private long networkACLItemId;

    @Column(name = "cidr")
    private String cidrList;

    public NetworkACLItemCidrsVO() {
    }

    public NetworkACLItemCidrsVO(long networkAclItemId, String cidrList) {
        this.networkACLItemId = networkAclItemId;
        this.cidrList = cidrList;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.api.InternalIdentity#getId()
     */
    @Override
    public long getId() {
        return id;
    }

    public long getNetworkACLItemId() {
        return networkACLItemId;
    }

    public String getCidr() {
        return cidrList;
    }

    public String getCidrList() {
        return cidrList;
    }

    public void setCidrList(String cidrList) {
        this.cidrList = cidrList;
    }

}
