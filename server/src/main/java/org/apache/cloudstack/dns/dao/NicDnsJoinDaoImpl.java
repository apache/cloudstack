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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.dns.vo.NicDnsJoinVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class NicDnsJoinDaoImpl extends GenericDaoBase<NicDnsJoinVO, Long> implements NicDnsJoinDao {
    private final SearchBuilder<NicDnsJoinVO> activeDnsRecordZoneSearch;
    private final SearchBuilder<NicDnsJoinVO> activeVmZoneDnsRecordSearch; // Route for null vmId
    private final SearchBuilder<NicDnsJoinVO> activeVmSearch;

    public NicDnsJoinDaoImpl() {

        activeDnsRecordZoneSearch = createSearchBuilder();
        activeDnsRecordZoneSearch.and(ApiConstants.NIC_DNS_NAME, activeDnsRecordZoneSearch.entity().getNicDnsName(), SearchCriteria.Op.EQ);
        activeDnsRecordZoneSearch.and(ApiConstants.DNS_ZONE_ID, activeDnsRecordZoneSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        activeDnsRecordZoneSearch.and(ApiConstants.REMOVED, activeDnsRecordZoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeDnsRecordZoneSearch.done();

        activeVmZoneDnsRecordSearch = createSearchBuilder();
        activeVmZoneDnsRecordSearch.and(ApiConstants.INSTANCE_ID, activeVmZoneDnsRecordSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        activeVmZoneDnsRecordSearch.and(ApiConstants.NIC_DNS_NAME, activeVmZoneDnsRecordSearch.entity().getNicDnsName(), SearchCriteria.Op.EQ);
        activeVmZoneDnsRecordSearch.and(ApiConstants.DNS_ZONE_ID, activeVmZoneDnsRecordSearch.entity().getDnsZoneId(), SearchCriteria.Op.EQ);
        activeVmZoneDnsRecordSearch.and(ApiConstants.REMOVED, activeVmZoneDnsRecordSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeVmZoneDnsRecordSearch.done();

        activeVmSearch = createSearchBuilder();
        activeVmSearch.and(ApiConstants.INSTANCE_ID, activeVmSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        activeVmSearch.done();
    }

    @Override
    public NicDnsJoinVO findActiveByDnsRecordAndZone(String dnsRecordUrl, long dnsZoneId) {
        SearchCriteria<NicDnsJoinVO> sc = activeDnsRecordZoneSearch.create();
        sc.setParameters(ApiConstants.NIC_DNS_NAME, dnsRecordUrl);
        sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
        return findOneBy(sc);
    }

    @Override
    public List<NicDnsJoinVO> listActiveByVmIdZoneAndDnsRecord(Long vmId, long dnsZoneId, String dnsRecordUrl) {
        if (vmId != null) {
            SearchCriteria<NicDnsJoinVO> sc = activeDnsRecordZoneSearch.create();
            sc.setParameters(ApiConstants.INSTANCE_ID, vmId);
            sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
            sc.setParameters(ApiConstants.NIC_DNS_NAME, dnsRecordUrl);
            return listBy(sc);
        } else {
            SearchCriteria<NicDnsJoinVO> sc = activeDnsRecordZoneSearch.create();
            sc.setParameters(ApiConstants.NIC_DNS_NAME, dnsRecordUrl);
            sc.setParameters(ApiConstants.DNS_ZONE_ID, dnsZoneId);
            return listBy(sc);
        }
    }

    @Override
    public List<NicDnsJoinVO> listActiveByVmId(long vmId) {
        SearchCriteria<NicDnsJoinVO> sc = activeVmSearch.create();
        sc.setParameters(ApiConstants.INSTANCE_ID, vmId);
        return listBy(sc);
    }

    @Override
    public List<NicDnsJoinVO> listIncludingRemovedByVmId(long vmId) {
        SearchCriteria<NicDnsJoinVO> sc = activeVmSearch.create();
        sc.setParameters(ApiConstants.INSTANCE_ID, vmId);
        return listIncludingRemovedBy(sc);
    }
}
