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
package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;


import org.apache.cloudstack.engine.cloud.entity.api.db.VMRootDiskTagVO;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = { VMRootDiskTagDao.class })
public class VMRootDiskTagDaoImpl extends GenericDaoBase<VMRootDiskTagVO, Long> implements VMRootDiskTagDao {

    protected SearchBuilder<VMRootDiskTagVO> VmIdSearch;

    public VMRootDiskTagDaoImpl() {
    }

    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();

    }

    @Override
    public void persist(long vmId, List<String> rootDiskTags) {
        Transaction txn = Transaction.currentTxn();

        txn.start();
        SearchCriteria<VMRootDiskTagVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);
        expunge(sc);

        for (String tag : rootDiskTags) {
            if(tag != null){
                tag = tag.trim();
                if(tag.length() > 0) {
                    VMRootDiskTagVO vo = new VMRootDiskTagVO(vmId, tag);
                    persist(vo);
                }
            }
        }
        txn.commit();
    }


    @Override
    public List<String> getRootDiskTags(long vmId) {
        SearchCriteria<VMRootDiskTagVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);

        List<VMRootDiskTagVO> results = search(sc, null);
        List<String> computeTags = new ArrayList<String>(results.size());
        for (VMRootDiskTagVO result : results) {
            computeTags.add(result.getRootDiskTag());
        }
        return computeTags;
    }

}
