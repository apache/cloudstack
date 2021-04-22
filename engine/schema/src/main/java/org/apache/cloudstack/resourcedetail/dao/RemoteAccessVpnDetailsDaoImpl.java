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
package org.apache.cloudstack.resourcedetail.dao;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.RemoteAccessVpnDetailVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class RemoteAccessVpnDetailsDaoImpl extends ResourceDetailsDaoBase<RemoteAccessVpnDetailVO> implements RemoteAccessVpnDetailsDao {
    protected final SearchBuilder<RemoteAccessVpnDetailVO> vpnSearch;

    public RemoteAccessVpnDetailsDaoImpl() {
        super();

        vpnSearch = createSearchBuilder();
        vpnSearch.and("remote_access_vpn", vpnSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        vpnSearch.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new RemoteAccessVpnDetailVO(resourceId, key, DBEncryptionUtil.encrypt(value), display));
    }

    @Override
    public Map<String, String> getDetails(long vpnId) {
        SearchCriteria<RemoteAccessVpnDetailVO> sc = vpnSearch.create();
        sc.setParameters("remote_access_vpn", vpnId);

        return listBy(sc).stream().collect(Collectors.toMap(RemoteAccessVpnDetailVO::getName, detail -> {
            return DBEncryptionUtil.decrypt(detail.getValue());
        }));
    }
}
