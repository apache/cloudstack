/**
 * Copyright (C) 2012 Citrix Systems, Inc.  All rights reserved
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

package com.cloud.api;

public abstract class BaseListDomainResourcesCmd extends BaseListCmd{
	
    @Parameter(name=ApiConstants.LIST_ALL, type=CommandType.BOOLEAN, description="If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false")
    private Boolean listAll;
    
    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="list only resources belonging to the domain specified")
    private Long domainId;
    
    @Parameter(name=ApiConstants.IS_RECURSIVE, type=CommandType.BOOLEAN, description="defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves.")
    private Boolean recursive;

    public boolean listAll() {
        return listAll == null ? false : listAll;
    }
    
    public boolean isRecursive() {
        return recursive == null ? false : recursive;
    }
    
    public Long getDomainId() {
        return domainId;
    }
}
