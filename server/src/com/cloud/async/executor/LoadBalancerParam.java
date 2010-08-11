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

import java.util.List;

public class LoadBalancerParam {
    private Long userId;
    private Long domainRouterId;
    private Long loadBalancerId;
    private List<Long> instanceIdList;

    public LoadBalancerParam() {
    }

    public LoadBalancerParam(Long userId, Long domainRouterId, Long loadBalancerId, List<Long> instanceIdList) {
        this.userId = userId;
        this.domainRouterId = domainRouterId;
        this.loadBalancerId = loadBalancerId;
        this.instanceIdList = instanceIdList;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDomainRouterId() {
        return domainRouterId;
    }

    public void setDomainRouterId(Long domainRouterId) {
        this.domainRouterId = domainRouterId;
    }

    public Long getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(Long securityGroupId) {
        this.loadBalancerId = securityGroupId;
    }

    public List<Long> getInstanceIdList() {
        return instanceIdList;
    }

    public void setInstanceId(List<Long> instanceIdList) {
        this.instanceIdList = instanceIdList;
    }
}
