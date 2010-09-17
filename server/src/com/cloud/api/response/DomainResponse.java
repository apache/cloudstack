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
package com.cloud.api.response;

import com.cloud.serializer.Param;

public class DomainResponse extends BaseResponse {
    @Param(name="id")
    private Long id;

    @Param(name="name")
    private String domainName;

    @Param(name="level")
    private Integer level;

    @Param(name="parentdomainid")
    private Long parentDomainId;

    @Param(name="parentdomainname")
    private String parentDomainName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getParentDomainId() {
        return parentDomainId;
    }

    public void setParentDomainId(Long parentDomainId) {
        this.parentDomainId = parentDomainId;
    }

    public String getParentDomainName() {
        return parentDomainName;
    }

    public void setParentDomainName(String parentDomainName) {
        this.parentDomainName = parentDomainName;
    }
}
