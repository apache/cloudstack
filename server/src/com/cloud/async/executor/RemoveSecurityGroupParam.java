/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.async.executor;

public class RemoveSecurityGroupParam {
    private Long userId;
    private Long securityGroupId;
    private String publicIp;
    private Long vmId;
    
    public RemoveSecurityGroupParam() {
    }
    
    public RemoveSecurityGroupParam(Long userId, Long securityGroupId, String publicIp, Long vmId) {
        this.userId = userId;
        this.securityGroupId = securityGroupId;
        this.publicIp = publicIp;
        this.vmId = vmId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getSecurityGroupId() {
        return securityGroupId;
    }
    
    public void setSecurityGroupId(Long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }
    
    public String getPublicIp() {
        return publicIp;
    }
    
    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
    
    public Long getVmId() {
        return vmId;
    }
    
    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }
}
