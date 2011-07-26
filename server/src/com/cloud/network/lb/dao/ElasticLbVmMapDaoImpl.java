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

package com.cloud.network.lb.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ElasticLbVmMapDao.class})
public class ElasticLbVmMapDaoImpl extends GenericDaoBase<ElasticLbVmMapVO, Long> implements ElasticLbVmMapDao {
    private SearchBuilder<ElasticLbVmMapVO> AllFieldsSearch;
   
    protected ElasticLbVmMapDaoImpl() {
        AllFieldsSearch  = createSearchBuilder();
        AllFieldsSearch.and("ipId", AllFieldsSearch.entity().getIpAddressId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("lbId", AllFieldsSearch.entity().getLbId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("elbVmId", AllFieldsSearch.entity().getElbVmId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
   
    }

    @Override
    public ElasticLbVmMapVO findOneByLbIdAndElbVmId(long lbId, long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        sc.setParameters("elbVmId", elbVmId);
        return findOneBy(sc);
    }

    @Override
    public List<ElasticLbVmMapVO> listByLbId(long lbId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        return listBy(sc);
    }

    @Override
    public List<ElasticLbVmMapVO> listByElbVmId(long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("elbVmId", elbVmId);
        return listBy(sc);
    }

    @Override
    public int deleteLB(long lbId) {
    	SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("lbId", lbId);
        return super.expunge(sc);
    }

    @Override
    public ElasticLbVmMapVO findOneByIpIdAndElbVmId(long ipId, long elbVmId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipId", ipId);
        sc.setParameters("elbVmId", elbVmId);
        return findOneBy(sc);
    }

    @Override
    public ElasticLbVmMapVO findOneByIp(long ipId) {
        SearchCriteria<ElasticLbVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipId", ipId);
        return findOneBy(sc);
    }

	
}
