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
package org.apache.cloudstack.storage.dao;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.storage.DiskControllerMappingVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class DiskControllerMappingDaoImpl extends GenericDaoBase<DiskControllerMappingVO, Long> implements DiskControllerMappingDao {
    private SearchBuilder<DiskControllerMappingVO> diskControllerMappingSearch;

    @PostConstruct
    public void init() {
        diskControllerMappingSearch = createSearchBuilder();
        diskControllerMappingSearch.and("name", diskControllerMappingSearch.entity().getName(), SearchCriteria.Op.EQ);
        diskControllerMappingSearch.and("controllerReference", diskControllerMappingSearch.entity().getControllerReference(), SearchCriteria.Op.EQ);
        diskControllerMappingSearch.and("hypervisor", diskControllerMappingSearch.entity().getHypervisor(), SearchCriteria.Op.EQ);
        diskControllerMappingSearch.done();
    }

    @Override
    public DiskControllerMappingVO findDiskControllerMapping(String name, String controllerReference, HypervisorType hypervisor) {
        SearchCriteria<DiskControllerMappingVO> sc = diskControllerMappingSearch.create();
        sc.setParametersIfNotNull("name", name);
        sc.setParametersIfNotNull("controllerReference", controllerReference);
        sc.setParameters("hypervisor", hypervisor);
        return findOneBy(sc);
    }

    @Override
    public List<DiskControllerMappingVO> listForHypervisor(HypervisorType hypervisor) {
        SearchCriteria<DiskControllerMappingVO> sc = diskControllerMappingSearch.create();
        sc.setParameters("hypervisor", hypervisor);
        return listBy(sc);
    }
}
