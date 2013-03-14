// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.user;

import java.util.List;

import org.apache.cloudstack.api.command.admin.domain.ListDomainChildrenCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;

import com.cloud.domain.Domain;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.Pair;

public interface DomainService {

    Domain createDomain(String name, Long parentId, String networkDomain, String domainUUID);

    Domain getDomain(long id);

    Domain getDomain(String uuid);

    /**
     * Return whether a domain is a child domain of a given domain.
     *
     * @param parentId
     * @param childId
     */
    boolean isChildDomain(Long parentId, Long childId);

    Pair<List<? extends Domain>, Integer> searchForDomains(ListDomainsCmd cmd)
            throws PermissionDeniedException;

    Pair<List<? extends Domain>, Integer> searchForDomainChildren(ListDomainChildrenCmd cmd)
            throws PermissionDeniedException;

    /**
     * find the domain by its path
     * 
     * @param domainPath
     *            the path to use to lookup a domain
     * @return domainVO the domain with the matching path, or null if no domain with the given path exists
     */
    Domain findDomainByPath(String domainPath);

}
