/**
 *  Copyright (C) 2011 Citrix.com, Inc.  All rights reserved.
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
package com.cloud.storage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

@Entity
@Table(name="guest_os_hypervisor")
public class GuestOSHypervisorVO  implements GuestOS  {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="hypervisor_type", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
    HypervisorType hypervisorType;
    
    @Column(name="guest_os_name")
    String guest_os_name;
    
    @Column(name="guest_os_id")
    long guest_os_id;
    
    public long getId() {
        return id;
    }
    
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }
    
    public void setHypervisorType(HypervisorType hypervisor_type) {
        this.hypervisorType = hypervisor_type;
    }

    public String getGuestOsName() {
        return guest_os_name; 
    }
    
    public void setGuestOsName(String name) {
        this.guest_os_name = name;
    }
    
    public long getGuestOsId() {
        return guest_os_id;
    }
    
    public void setGuestOsId(long guest_os_id) {
        this.guest_os_id = guest_os_id;
    }

    @Override
    public long getCategoryId() {
        return 0;
    }

    @Override
    public String getDisplayName() {
        return guest_os_name;
    }

    @Override
    public String getName() {
        return guest_os_name;
    }
}
