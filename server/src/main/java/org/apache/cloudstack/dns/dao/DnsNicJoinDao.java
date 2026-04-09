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

import org.apache.cloudstack.dns.vo.DnsNicJoinVO;

import com.cloud.utils.db.GenericDao;

public interface DnsNicJoinDao extends GenericDao<DnsNicJoinVO, Long> {

    /**
     * Used for Collision Checks.
     * @param dnsRecordUrl
     * @param dnsZoneId
     * @return active records to see who currently owns the dnsRecordUrl.
     */
    DnsNicJoinVO findActiveByDnsRecordAndZone(String dnsRecordUrl, long dnsZoneId);

    /**
     * Used to sync DNS record url based on available ips for vmId in the dnsZone
     * @param vmId
     * @param dnsZoneId
     * @param dnsRecordUrl
     * @return list of active nics using the dnsRecordUrl, supports null vmId for dnsZone wide query
     */
    List<DnsNicJoinVO> listActiveByVmIdZoneAndDnsRecord(Long vmId, long dnsZoneId, String dnsRecordUrl);

    /**
     * Used for VM Start/Running
     * @param vmId
     * @return records associated to vmId
     */
    List<DnsNicJoinVO> listActiveByVmId(long vmId);

    /**
     * Used by Instance Destroy/Stop or NIC delete
     * @param vmId
     * @return records with soft-delete
     */
    List<DnsNicJoinVO> listIncludingRemovedByVmId(long vmId);
}
