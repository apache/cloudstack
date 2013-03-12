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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = VpcGatewayDao.class)
@DB(txn = false)
public class VpcGatewayDaoImpl extends GenericDaoBase<VpcGatewayVO, Long> implements VpcGatewayDao{
    protected final SearchBuilder<VpcGatewayVO> AllFieldsSearch;
    
    protected VpcGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("type", AllFieldsSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }


    @Override
    public VpcGatewayVO getPrivateGatewayForVpc(long vpcId) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("type", VpcGateway.Type.Private);

        return findOneBy(sc);
    }

    @Override
    public VpcGatewayVO getVpnGatewayForVpc(long vpcId) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("type", VpcGateway.Type.Vpn);

        return findOneBy(sc);
    }

}
