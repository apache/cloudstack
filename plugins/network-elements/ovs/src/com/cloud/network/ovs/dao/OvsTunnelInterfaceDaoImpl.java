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

package com.cloud.network.ovs.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = { OvsTunnelInterfaceDao.class })
public class OvsTunnelInterfaceDaoImpl extends
		GenericDaoBase<OvsTunnelInterfaceVO, Long> implements OvsTunnelInterfaceDao {

	protected final SearchBuilder<OvsTunnelInterfaceVO> hostAndLabelSearch;
	protected final SearchBuilder<OvsTunnelInterfaceVO> labelSearch;
	
	public OvsTunnelInterfaceDaoImpl() {
		hostAndLabelSearch = createSearchBuilder();
		hostAndLabelSearch.and("host_id", hostAndLabelSearch.entity().getHostId(), Op.EQ);
		hostAndLabelSearch.and("label", hostAndLabelSearch.entity().getLabel(), Op.EQ);
		hostAndLabelSearch.done();
		
		labelSearch = createSearchBuilder();
		labelSearch.and("label", labelSearch.entity().getLabel(), Op.EQ);
		labelSearch.done();
		
	}
	
	@Override
	public OvsTunnelInterfaceVO getByHostAndLabel(long hostId, String label) {
		SearchCriteria<OvsTunnelInterfaceVO> sc = hostAndLabelSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("label", label);
		return findOneBy(sc);
	}

    @Override
    public List<OvsTunnelInterfaceVO> listByLabel(String label) {
        SearchCriteria<OvsTunnelInterfaceVO> sc = labelSearch.create();
        sc.setParameters("label", label);
        return listBy(sc);
    }


}
