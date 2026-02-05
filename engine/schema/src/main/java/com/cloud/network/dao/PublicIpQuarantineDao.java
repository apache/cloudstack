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

import com.cloud.network.vo.PublicIpQuarantineVO;
import com.cloud.utils.db.GenericDao;

import java.util.Date;
import java.util.List;

public interface PublicIpQuarantineDao extends GenericDao<PublicIpQuarantineVO, Long> {

    PublicIpQuarantineVO findByPublicIpAddressId(long publicIpAddressId);

    PublicIpQuarantineVO findByIpAddress(String publicIpAddress);

    /**
     * Returns a list of public IP addresses that are actively quarantined at the specified date and the previous owner differs from the specified user.
     *
     * @param userId used to check against the IP's previous owner.
     * @param date used to check if the quarantine is active;
     * @return a list of PublicIpQuarantineVOs
     */
    List<PublicIpQuarantineVO> listQuarantinedIpAddressesToUser(Long userId, Date date);
}
