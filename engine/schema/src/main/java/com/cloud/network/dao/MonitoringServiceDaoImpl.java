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


import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class MonitoringServiceDaoImpl extends GenericDaoBase<MonitoringServiceVO, Long> implements MonitoringServiceDao {
    private final SearchBuilder<MonitoringServiceVO> AllFieldsSearch;

    public MonitoringServiceDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("isDefault", AllFieldsSearch.entity().isDefaultService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("service", AllFieldsSearch.entity().getService(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("processname", AllFieldsSearch.entity().getProcessName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("servicename", AllFieldsSearch.entity().getServiceName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("servicepath", AllFieldsSearch.entity().getServicePath(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("servicePidFile", AllFieldsSearch.entity().getServicePidFile(), SearchCriteria.Op.EQ);

        AllFieldsSearch.done();
    }

    @Override
    public List<MonitoringServiceVO> listAllServices() {
        return null;
    }

    @Override
    public List<MonitoringServiceVO> listDefaultServices(boolean isDefault) {
        SearchCriteria<MonitoringServiceVO> sc = AllFieldsSearch.create();
        sc.setParameters("isDefault", isDefault);
        return listBy(sc);
    }

    @Override
    public MonitoringServiceVO getServiceByName(String service) {
        SearchCriteria<MonitoringServiceVO> sc = AllFieldsSearch.create();
        sc.setParameters("service", service);
        return findOneBy(sc);
    }
}
