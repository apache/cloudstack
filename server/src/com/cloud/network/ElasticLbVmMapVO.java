/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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

import com.cloud.utils.net.Ip;

@Entity
@Table(name = ("elastic_lb_vm_map"))
@SecondaryTables({ 
        @SecondaryTable(name = "user_ip_address", pkJoinColumns = { @PrimaryKeyJoinColumn(name = "ip_addr_id", referencedColumnName = "id") })
        })
public class ElasticLbVmMapVO {
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
    @Enumerated(value=EnumType.STRING)
    private Ip address = null;
    
    public ElasticLbVmMapVO() {
    }

    public ElasticLbVmMapVO(long ipId, long elbVmId, long lbId) {
        this.ipAddressId = ipId;
        this.elbVmId = elbVmId;
        this.lbId = lbId;
    }

    public Long getId() {
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

