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
package com.cloud.cluster.dao;

import java.util.Date;
import java.util.List;

import com.cloud.cluster.ManagementServerHost;
import com.cloud.cluster.ManagementServerHost.State;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface ManagementServerHostDao extends GenericDao<ManagementServerHostVO, Long> {
    @Override
    boolean remove(Long id);

    ManagementServerHostVO findByMsid(long msid);

    int increaseAlertCount(long id);

    void update(long id, long runid, String name, String version, String serviceIP, int servicePort, Date lastUpdate);

    void update(long id, long runid, Date lastUpdate);

    List<ManagementServerHostVO> getActiveList(Date cutTime);

    List<ManagementServerHostVO> getInactiveList(Date cutTime);

    void invalidateRunSession(long id, long runid);

    void update(long id, long runId, State state, Date lastUpdate);

    List<ManagementServerHostVO> listBy(ManagementServerHost.State... states);

    public List<Long> listOrphanMsids();

    ManagementServerHostVO findOneInUpState(Filter filter);
}
