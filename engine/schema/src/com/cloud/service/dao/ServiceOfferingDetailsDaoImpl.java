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
package com.cloud.service.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value=ServiceOfferingDetailsDao.class)
public class ServiceOfferingDetailsDaoImpl extends GenericDaoBase<ServiceOfferingDetailsVO, Long>
        implements ServiceOfferingDetailsDao {
    protected final SearchBuilder<ServiceOfferingDetailsVO> ServiceOfferingSearch;
    protected final SearchBuilder<ServiceOfferingDetailsVO> DetailSearch;

    public ServiceOfferingDetailsDaoImpl() {
        ServiceOfferingSearch = createSearchBuilder();
        ServiceOfferingSearch.and("serviceOfferingId", ServiceOfferingSearch.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        ServiceOfferingSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("serviceOfferingId", DetailSearch.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public ServiceOfferingDetailsVO findDetail(long serviceOfferingId, String name) {
        SearchCriteria<ServiceOfferingDetailsVO> sc = DetailSearch.create();
        sc.setParameters("serviceOfferingId", serviceOfferingId);
        sc.setParameters("name", name);
        ServiceOfferingDetailsVO detail = findOneIncludingRemovedBy(sc);
        return detail;
    }

    @Override
    public Map<String, String> findDetails(long serviceOfferingId) {
        SearchCriteria<ServiceOfferingDetailsVO> sc = ServiceOfferingSearch.create();
        sc.setParameters("serviceOfferingId", serviceOfferingId);
        List<ServiceOfferingDetailsVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (ServiceOfferingDetailsVO result : results) {
            details.put(result.getName(), result.getValue());
        }

        return details;
    }

    @Override
    public void deleteDetails(long serviceOfferingId) {
        SearchCriteria sc = ServiceOfferingSearch.create();
        sc.setParameters("serviceOfferingId", serviceOfferingId);
        List<ServiceOfferingDetailsVO> results = search(sc, null);
        for (ServiceOfferingDetailsVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void persist(long serviceOfferingId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<ServiceOfferingDetailsVO> sc = ServiceOfferingSearch.create();
        sc.setParameters("serviceOfferingId", serviceOfferingId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            String value = detail.getValue();
            ServiceOfferingDetailsVO vo = new ServiceOfferingDetailsVO(serviceOfferingId, detail.getKey(), value);
            persist(vo);
        }
        txn.commit();
    }
}
