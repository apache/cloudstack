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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.net.Ip;

@Entity
@Table(name = ("elastic_lb_vm_map"))
@SecondaryTables({@SecondaryTable(name = "user_ip_address", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ip_addr_id", referencedColumnName = "id")})})
public class ElasticLbVmMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lb_id")
    private Long lbId;

    @Column(name = "ip_addr_id")
    private long ipAddressId;

    @Column(name = "elb_vm_id")
    private long elbVmId;

    /*@Column(name = "name", table = "load_balancing_rules", insertable = false, updatable = false)
    private String lbName;*/

    @Column(name = "public_ip_address", table = "user_ip_address", insertable = false, updatable = false)
    @Enumerated(value = EnumType.STRING)
    private Ip address = null;

    public ElasticLbVmMapVO() {
    }

    public ElasticLbVmMapVO(long ipId, long elbVmId, long lbId) {
        this.ipAddressId = ipId;
        this.elbVmId = elbVmId;
        this.lbId = lbId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getLbId() {
        return lbId;
    }

    public long getElbVmId() {
        return elbVmId;
    }

//    public String getLbName() {
//        return lbName;
//    }

    public long getIpAddressId() {
        return ipAddressId;
    }

    public void setLbId(Long lbId) {
        this.lbId = lbId;
    }

    public Ip getAddress() {
        return address;
    }

}
