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

import com.cloud.utils.db.GenericDao;

public interface Site2SiteVpnConnectionDao extends GenericDao<Site2SiteVpnConnectionVO, Long> {
    List<Site2SiteVpnConnectionVO> listByCustomerGatewayId(long id);

    List<Site2SiteVpnConnectionVO> listByVpnGatewayId(long id);

    List<Site2SiteVpnConnectionVO> listByVpcId(long vpcId);

    Site2SiteVpnConnectionVO findByVpnGatewayIdAndCustomerGatewayId(long vpnId, long customerId);

    Site2SiteVpnConnectionVO findByCustomerGatewayId(long customerId);
}
