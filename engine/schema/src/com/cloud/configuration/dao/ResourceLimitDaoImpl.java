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
package com.cloud.configuration.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {ResourceLimitDao.class})
public class ResourceLimitDaoImpl extends GenericDaoBase<ResourceLimitVO, Long> implements ResourceLimitDao {
    private SearchBuilder<ResourceLimitVO> IdTypeSearch;

    public ResourceLimitDaoImpl() {
        IdTypeSearch = createSearchBuilder();
        IdTypeSearch.and("type", IdTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        IdTypeSearch.and("domainId", IdTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        IdTypeSearch.and("accountId", IdTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        IdTypeSearch.done();
    }

    @Override
    public List<ResourceLimitVO> listByOwner(Long ownerId, ResourceOwnerType ownerType) {
        SearchCriteria<ResourceLimitVO> sc = IdTypeSearch.create();

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return listBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return listBy(sc);
        } else {
            return new ArrayList<ResourceLimitVO>();
        }
    }

    @Override
    public boolean update(Long id, Long max) {
        ResourceLimitVO limit = findById(id);
        if (max != null)
            limit.setMax(max);
        else
            limit.setMax(new Long(-1));
        return update(id, limit);
    }

    @Override
    public ResourceCount.ResourceType getLimitType(String type) {
        ResourceType[] validTypes = Resource.ResourceType.values();

        for (ResourceType validType : validTypes) {
            if (validType.getName().equals(type)) {
                return validType;
            }
        }
        return null;
    }

    @Override
    public ResourceLimitVO findByOwnerIdAndType(long ownerId, ResourceOwnerType ownerType, ResourceCount.ResourceType type) {
        SearchCriteria<ResourceLimitVO> sc = IdTypeSearch.create();
        sc.setParameters("type", type);

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return findOneBy(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return findOneBy(sc);
        } else {
            return null;
        }
    }

    @Override
    public long removeEntriesByOwner(Long ownerId, ResourceOwnerType ownerType) {
        SearchCriteria<ResourceLimitVO> sc = IdTypeSearch.create();

        if (ownerType == ResourceOwnerType.Account) {
            sc.setParameters("accountId", ownerId);
            return remove(sc);
        } else if (ownerType == ResourceOwnerType.Domain) {
            sc.setParameters("domainId", ownerId);
            return remove(sc);
        }
        return 0;
    }
}
