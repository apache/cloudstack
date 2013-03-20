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
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.domain.ListDomainChildrenCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;

@Component
@Local(value = { DomainManager.class, DomainService.class })
public class MockDomainManagerImpl extends ManagerBase implements DomainManager, DomainService {

    @Override
    public Domain getDomain(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomain(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteDomain(long domainId, Boolean cleanup) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pair<List<? extends Domain>, Integer> searchForDomains(ListDomainsCmd cmd)
            throws PermissionDeniedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends Domain>, Integer> searchForDomainChildren(
            ListDomainChildrenCmd cmd) throws PermissionDeniedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Long> getDomainChildrenIds(String parentDomainPath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DomainVO findDomainByPath(String domainPath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Long> getDomainParentIds(long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeDomain(long domainId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends Domain> findInactiveDomains() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteDomain(DomainVO domain, Boolean cleanup) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain createDomain(String name, Long parentId,
            String networkDomain, String domainUUID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain updateDomain(UpdateDomainCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain createDomain(String name, Long parentId, Long ownerId,
            String networkDomain, String domainUUID) {
        // TODO Auto-generated method stub
        return null;
    }

}
