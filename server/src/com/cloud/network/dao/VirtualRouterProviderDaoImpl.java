/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
package com.cloud.network.dao;

import javax.ejb.Local;

import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=VirtualRouterProviderDao.class) @DB(txn=false)
public class VirtualRouterProviderDaoImpl extends GenericDaoBase<VirtualRouterProviderVO, Long> implements VirtualRouterProviderDao {
    final SearchBuilder<VirtualRouterProviderVO> AllFieldsSearch;
    
    public VirtualRouterProviderDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("nsp_id", AllFieldsSearch.entity().getNspId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("type", AllFieldsSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public VirtualRouterProviderVO findByNspIdAndType(long nspId, VirtualRouterProviderType type) {
        SearchCriteria<VirtualRouterProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("nsp_id", nspId);
        sc.setParameters("type", type);
        return findOneBy(sc);
    }
}
