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
package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.element.OvsProviderVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = OvsProviderDao.class)
@DB()
public class OvsProviderDaoImpl extends GenericDaoBase<OvsProviderVO, Long>
		implements OvsProviderDao {
	final SearchBuilder<OvsProviderVO> AllFieldsSearch;

	public OvsProviderDaoImpl() {
		super();
		AllFieldsSearch = createSearchBuilder();
		AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("nsp_id", AllFieldsSearch.entity().getNspId(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.and("enabled", AllFieldsSearch.entity().isEnabled(),
				SearchCriteria.Op.EQ);
		AllFieldsSearch.done();
	}

	@Override
	public OvsProviderVO findByNspId(long nspId) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("nsp_id", nspId);
		return findOneBy(sc);
	}

	@Override
	public List<OvsProviderVO> listByEnabled(boolean enabled) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("enabled", enabled);
		return listBy(sc);
	}

	@Override
	public OvsProviderVO findByIdAndEnabled(long id, boolean enabled) {
		SearchCriteria<OvsProviderVO> sc = AllFieldsSearch.create();
		sc.setParameters("id", id);
		sc.setParameters("enabled", enabled);
		return findOneBy(sc);
	}
}
