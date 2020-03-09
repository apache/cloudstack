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

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB()
public class VpcGatewayDaoImpl extends GenericDaoBase<VpcGatewayVO, Long> implements VpcGatewayDao {
    protected final SearchBuilder<VpcGatewayVO> AllFieldsSearch;

    protected VpcGatewayDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("type", AllFieldsSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkid", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipaddress", AllFieldsSearch.entity().getIp4Address(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("aclId", AllFieldsSearch.entity().getNetworkACLId(), SearchCriteria.Op.EQ);
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
    public Long getNetworkAclIdForPrivateIp(long vpcId, long networkId, String ipaddr) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("networkid", networkId);
        sc.setParameters("ipaddress", ipaddr);

        VpcGateway vpcGateway = findOneBy(sc);
        if (vpcGateway != null) {
            return vpcGateway.getNetworkACLId();
        } else {
            return null;
        }
    }

    @Override
    public List<VpcGatewayVO> listByVpcIdAndType(long vpcId, VpcGateway.Type type) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("type", type);
        return listBy(sc);
    }

    @Override
    public List<VpcGatewayVO> listByAclIdAndType(long aclId, VpcGateway.Type type) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("aclId", aclId);
        sc.setParameters("type", type);
        return listBy(sc);
    }

    @Override
    public List<VpcGatewayVO> listByVpcId(long vpcId) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);
        return listBy(sc);
    }

    @Override
    public VpcGatewayVO getVpcGatewayByNetworkId(long networkId) {
        SearchCriteria<VpcGatewayVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkid", networkId);
        return findOneBy(sc);
    }
}
