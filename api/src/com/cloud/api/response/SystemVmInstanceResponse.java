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
package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

/*
 * This is the generic response for all types of System VMs (SSVM, consoleproxy, domain routers(router, LB, DHCP))
 */
public class SystemVmInstanceResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the system VM")
    private Long id;

    @SerializedName("systemvmtype") @Param(description="the system VM type")
    private String systemVmType;

    @SerializedName("name") @Param(description="the name of the system VM")
    private String name;

    @SerializedName("hostid") @Param(description="the host ID for the system VM")
    private Long hostId;

    @SerializedName("state") @Param(description="the state of the system VM")
    private String state;
    
    @SerializedName("role") @Param(description="the role of the system VM")
    private String role;
    
    
    public Long getObjectId() {
    	return getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public void setSystemVmType(String systemVmType) {
        this.systemVmType = systemVmType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }    

}
