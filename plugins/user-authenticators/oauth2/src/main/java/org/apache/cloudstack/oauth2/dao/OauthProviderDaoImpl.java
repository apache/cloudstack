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

package org.apache.cloudstack.oauth2.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;

public class OauthProviderDaoImpl extends GenericDaoBase<OauthProviderVO, Long> implements OauthProviderDao {

    private final SearchBuilder<OauthProviderVO> oauthProviderSearchByName;

    public OauthProviderDaoImpl() {
        super();

        oauthProviderSearchByName = createSearchBuilder();
        oauthProviderSearchByName.and("provider", oauthProviderSearchByName.entity().getProvider(), SearchCriteria.Op.EQ);
        oauthProviderSearchByName.done();
    }

    @Override
    public OauthProviderVO findByProvider(String provider) {
        SearchCriteria<OauthProviderVO> sc = oauthProviderSearchByName.create();
        sc.setParameters("provider", provider);

        return findOneBy(sc);
    }
}
