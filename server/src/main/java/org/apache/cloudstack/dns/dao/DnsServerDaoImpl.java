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

package org.apache.cloudstack.dns.dao;

import java.util.List;

import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DnsServerDaoImpl extends GenericDaoBase<DnsServerVO, Long> implements DnsServerDao {
    static final String PROVIDER_TYPE = "providerType";
    SearchBuilder<DnsServerVO> ProviderSearch;

    public DnsServerDaoImpl() {
        super();
        ProviderSearch = createSearchBuilder();
        ProviderSearch.and(PROVIDER_TYPE, ProviderSearch.entity().getProviderType(), SearchCriteria.Op.EQ);
        ProviderSearch.done();
    }

    @Override
    public List<DnsServerVO> listByProviderType(String providerType) {
        SearchCriteria<DnsServerVO> sc = ProviderSearch.create();
        sc.setParameters(PROVIDER_TYPE, providerType);
        return listBy(sc);
    }
}
