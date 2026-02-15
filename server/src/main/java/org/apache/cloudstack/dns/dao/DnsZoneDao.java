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

import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.vo.DnsZoneVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface DnsZoneDao extends GenericDao<DnsZoneVO, Long> {
    List<DnsZoneVO> listByServerId(long serverId);
    List<DnsZoneVO> listByAccount(long accountId);
    DnsZoneVO findByNameServerAndType(String name, long dnsServerId, DnsZone.ZoneType type);
    Pair<List<DnsZoneVO>, Integer> searchZones(Long id, Long dnsServerId, String keyword, Long accountId, Filter filter);
}
