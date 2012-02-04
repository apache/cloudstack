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

package com.cloud.configuration.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value = { ResourceLimitDao.class })
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
}
