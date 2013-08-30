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
package com.cloud.dc.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.dc.StorageNetworkIpRangeVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value={StorageNetworkIpRangeDao.class})
@DB
public class StorageNetworkIpRangeDaoImpl extends GenericDaoBase<StorageNetworkIpRangeVO, Long> implements StorageNetworkIpRangeDao {
	protected final GenericSearchBuilder<StorageNetworkIpRangeVO, Long> countRanges;
	
	protected StorageNetworkIpRangeDaoImpl() {
		countRanges = createSearchBuilder(Long.class);
		countRanges.select(null, Func.COUNT, null);
		countRanges.done();
	}
	
	@Override
    public List<StorageNetworkIpRangeVO> listByPodId(long podId) {
        QueryBuilder<StorageNetworkIpRangeVO> sc = QueryBuilder.create(StorageNetworkIpRangeVO.class);
	    sc.and(sc.entity().getPodId(), Op.EQ, podId);
		return sc.list();
    }

	@Override
    public List<StorageNetworkIpRangeVO> listByRangeId(long rangeId) {
        QueryBuilder<StorageNetworkIpRangeVO> sc = QueryBuilder.create(StorageNetworkIpRangeVO.class);
	    sc.and(sc.entity().getId(), Op.EQ, rangeId);
		return sc.list();
    }

	@Override
    public List<StorageNetworkIpRangeVO> listByDataCenterId(long dcId) {
        QueryBuilder<StorageNetworkIpRangeVO> sc = QueryBuilder.create(StorageNetworkIpRangeVO.class);
	    sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
		return sc.list();
    }
	
	@Override
	public long countRanges() {
		SearchCriteria<Long> sc = countRanges.create();
		return customSearch(sc, null).get(0);
	}

}
