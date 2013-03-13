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
import java.util.Set;

import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;

public interface DomainManager extends DomainService {
    Set<Long> getDomainChildrenIds(String parentDomainPath);

    Domain createDomain(String name, Long parentId, Long ownerId, String networkDomain, String domainUUID);

    Set<Long> getDomainParentIds(long domainId);

    boolean removeDomain(long domainId);

    List<? extends Domain> findInactiveDomains();

    boolean deleteDomain(DomainVO domain, Boolean cleanup);
    
    boolean deleteDomain(long domainId, Boolean cleanup);
    
    /**
     * update an existing domain
     * 
     * @param cmd
     *            - the command containing domainId and new domainName
     * @return Domain object if the command succeeded
     */
    Domain updateDomain(UpdateDomainCmd cmd);    
}
