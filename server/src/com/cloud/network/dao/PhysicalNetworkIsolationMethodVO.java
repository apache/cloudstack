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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * This class is just used to work with the DAO. It shouldn't be used anywhere.
 * 
 */
@Entity
@Table(name = "physical_network_isolation_methods")
public class PhysicalNetworkIsolationMethodVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "isolation_method")
    private String isolationMethod;

    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected PhysicalNetworkIsolationMethodVO() {
    }

    protected PhysicalNetworkIsolationMethodVO(long physicalNetworkId, String isolationMethod) {
        this.physicalNetworkId = physicalNetworkId;
        this.isolationMethod = isolationMethod;
    }

    public long getId() {
        return id;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getIsolationMethod() {
        return isolationMethod;
    }
}
