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
package com.cloud.vpc.dao;

import java.util.List;


import com.cloud.network.Network.Service;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

@DB()
public class MockVpcOfferingServiceMapDaoImpl extends GenericDaoBase<VpcOfferingServiceMapVO, Long> implements VpcOfferingServiceMapDao {

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcOfferingServiceMapDao#listByVpcOffId(long)
     */
    @Override
    public List<VpcOfferingServiceMapVO> listByVpcOffId(long vpcOffId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcOfferingServiceMapDao#areServicesSupportedByNetworkOffering(long, com.cloud.network.Network.Service[])
     */
    @Override
    public boolean areServicesSupportedByVpcOffering(long vpcOfferingId, Service[] services) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcOfferingServiceMapDao#listServicesForVpcOffering(long)
     */
    @Override
    public List<String> listServicesForVpcOffering(long vpcOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcOfferingServiceMapDao#findByServiceProviderAndOfferingId(java.lang.String, java.lang.String, long)
     */
    @Override
    public VpcOfferingServiceMapVO findByServiceProviderAndOfferingId(String service, String provider, long vpcOfferingId) {
        return new VpcOfferingServiceMapVO();
    }

    @Override
    public VpcOfferingServiceMapVO persist(VpcOfferingServiceMapVO vo) {
        return vo;
    }

}
