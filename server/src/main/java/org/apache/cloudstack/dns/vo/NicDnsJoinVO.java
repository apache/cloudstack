/*
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

package org.apache.cloudstack.dns.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.api.query.vo.BaseViewVO;

@Entity
@Table(name = "nic_dns_view")
public class NicDnsJoinVO extends BaseViewVO implements InternalIdentity, Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "instance_id")
    private long instanceId;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "ip4_address")
    private String ip4Address;

    @Column(name = "ip6_address")
    private String ip6Address;

    @Column(name = "nic_dns_name")
    private String nicDnsName;

    @Column(name = "dns_zone_id")
    private long dnsZoneId;

    @Column(name = "sub_domain")
    private String subDomain;

    @Column(name = "removed")
    private Date removed;

    public NicDnsJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public long getDnsZoneId() {
        return dnsZoneId;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public String getNicDnsName() {
        return nicDnsName;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public Date getRemoved() {
        return removed;
    }
}
