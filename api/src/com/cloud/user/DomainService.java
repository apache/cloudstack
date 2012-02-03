/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.user;

import java.util.List;

import com.cloud.api.commands.ListDomainChildrenCmd;
import com.cloud.api.commands.ListDomainsCmd;
import com.cloud.domain.Domain;
import com.cloud.exception.PermissionDeniedException;

public interface DomainService {

    Domain createDomain(String name, Long parentId, String networkDomain);

    Domain getDomain(long id);

    /**
     * Return whether a domain is a child domain of a given domain.
     * 
     * @param parentId
     * @param childId
     * @return True if the domainIds are equal, or if the second domain is a child of the first domain. False otherwise.
     */
    boolean isChildDomain(Long parentId, Long childId);

    boolean deleteDomain(long domainId, Boolean cleanup);

    List<? extends Domain> searchForDomains(ListDomainsCmd cmd)
            throws PermissionDeniedException;

    List<? extends Domain> searchForDomainChildren(ListDomainChildrenCmd cmd)
            throws PermissionDeniedException;

}
