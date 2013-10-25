/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.vm.snapshot.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMSnapshotDetailsDaoImpl extends GenericDaoBase<VMSnapshotDetailsVO, Long> implements VMSnapshotDetailsDao {
    protected final SearchBuilder<VMSnapshotDetailsVO> searchDetails;

    protected VMSnapshotDetailsDaoImpl() {
        super();
        searchDetails = createSearchBuilder();
        searchDetails.and("vmsnapshotId", searchDetails.entity().getVmSnapshotId(), SearchCriteria.Op.EQ);
        searchDetails.done();
    }
    @Override
    public Map<String, String> getDetails(Long vmSnapshotId) {
        SearchCriteria<VMSnapshotDetailsVO> sc = searchDetails.create();
        sc.setParameters("vmsnapshotId", vmSnapshotId);

        List<VMSnapshotDetailsVO> details = listBy(sc);
        Map<String, String> detailsMap = new HashMap<String, String>();
        for (VMSnapshotDetailsVO detail : details) {
            detailsMap.put(detail.getName(), detail.getValue());
        }

        return detailsMap;
    }
}
