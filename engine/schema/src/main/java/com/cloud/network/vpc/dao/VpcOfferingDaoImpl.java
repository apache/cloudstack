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
package com.cloud.network.vpc.dao;


import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.springframework.stereotype.Component;

import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.NetUtils;

@Component
@DB()
public class VpcOfferingDaoImpl extends GenericDaoBase<VpcOfferingVO, Long> implements VpcOfferingDao {
    final SearchBuilder<VpcOfferingVO> AllFieldsSearch;

    @Inject
    VpcOfferingDetailsDao detailsDao;

    protected VpcOfferingDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), Op.EQ);
        AllFieldsSearch.and("uName", AllFieldsSearch.entity().getUniqueName(), Op.EQ);
        AllFieldsSearch.and("displayText", AllFieldsSearch.entity().getDisplayText(), Op.EQ);
        AllFieldsSearch.and("svcOffId", AllFieldsSearch.entity().getServiceOfferingId(), Op.EQ);
        AllFieldsSearch.done();

    }

    @Override
    @DB
    public boolean remove(Long vpcOffId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        VpcOfferingVO offering = findById(vpcOffId);
        offering.setUniqueName(null);
        update(vpcOffId, offering);
        boolean result = super.remove(vpcOffId);
        txn.commit();
        return result;
    }

    @Override
    public VpcOfferingVO findByUniqueName(String uniqueName) {
        SearchCriteria<VpcOfferingVO> sc = AllFieldsSearch.create();
        sc.setParameters("uName", uniqueName);
        return findOneBy(sc);
    }

    @Override
    public NetUtils.InternetProtocol getVpcOfferingInternetProtocol(long offeringId) {
        String internetProtocolStr = detailsDao.getDetail(offeringId, ApiConstants.INTERNET_PROTOCOL);
        return NetUtils.InternetProtocol.fromValue(internetProtocolStr);
    }

    @Override
    public boolean isIpv6Supported(long offeringId) {
        NetUtils.InternetProtocol internetProtocol = getVpcOfferingInternetProtocol(offeringId);
        return NetUtils.InternetProtocol.isIpv6EnabledProtocol(internetProtocol);
    }
}
