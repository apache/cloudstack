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

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.GenericQueryBuilder;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={StorageNetworkIpAddressDao.class})
@DB
public class StorageNetworkIpAddressDaoImpl extends GenericDaoBase<StorageNetworkIpAddressVO, Long> implements StorageNetworkIpAddressDao {
	protected final GenericSearchBuilder<StorageNetworkIpAddressVO, Long> countInUserIp;
	protected final GenericSearchBuilder<StorageNetworkIpAddressVO, String> listInUseIp;
	protected final SearchBuilder<StorageNetworkIpAddressVO> untakenIp;
	protected final SearchBuilder<StorageNetworkIpAddressVO> ipSearch;
	

	protected StorageNetworkIpAddressDaoImpl() {
		countInUserIp = createSearchBuilder(Long.class);
		countInUserIp.select(null, Func.COUNT, null);
		countInUserIp.and("rangeId", countInUserIp.entity().getRangeId(), Op.EQ);
		countInUserIp.and("taken", countInUserIp.entity().getTakenAt(), Op.NNULL);
		countInUserIp.done();
		
		listInUseIp = createSearchBuilder(String.class);
		listInUseIp.selectFields(listInUseIp.entity().getIpAddress());
		listInUseIp.and("rangeId", listInUseIp.entity().getRangeId(), Op.EQ);
		listInUseIp.and("taken", listInUseIp.entity().getTakenAt(), Op.NNULL);
		listInUseIp.done();
		
		untakenIp = createSearchBuilder();
		untakenIp.and("rangeId", untakenIp.entity().getRangeId(), Op.EQ);
		untakenIp.and("taken", untakenIp.entity().getTakenAt(), Op.NULL);
		untakenIp.done();
		
		ipSearch = createSearchBuilder();
		ipSearch.and("ipAddress", ipSearch.entity().getIpAddress(), Op.EQ);
		ipSearch.done();
	}
	
	@Override
	public long countInUseIpByRangeId(long rangeId) {
		SearchCriteria<Long> sc = countInUserIp.create();
		sc.setParameters("rangeId", rangeId);
		return customSearch(sc, null).get(0);
	}

	@Override
	public List<String> listInUseIpByRangeId(long rangeId) {
		SearchCriteria<String> sc = listInUseIp.create();
		sc.setParameters("rangeId", rangeId);
		return customSearch(sc, null);
	}
	
	@Override
	@DB
    public StorageNetworkIpAddressVO takeIpAddress(long rangeId) {
		SearchCriteria<StorageNetworkIpAddressVO> sc = untakenIp.create();
		sc.setParameters("rangeId", rangeId);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        StorageNetworkIpAddressVO ip = lockOneRandomRow(sc, true);
        if (ip == null) {
        	txn.rollback();
        	return null;
        }
        ip.setTakenAt(new Date());
        update(ip.getId(), ip);
        txn.commit();
        return ip;
	}

	@Override
    public void releaseIpAddress(String ip) {
		SearchCriteria<StorageNetworkIpAddressVO> sc = ipSearch.create();
	    sc.setParameters("ipAddress", ip);
	    StorageNetworkIpAddressVO vo = createForUpdate();
	    vo.setTakenAt(null);
	    update(vo, sc);
    }
}
