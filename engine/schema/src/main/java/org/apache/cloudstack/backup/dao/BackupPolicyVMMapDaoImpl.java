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

package org.apache.cloudstack.backup.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.backup.BackupPolicyVMMapVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class BackupPolicyVMMapDaoImpl extends GenericDaoBase<BackupPolicyVMMapVO, Long> implements BackupPolicyVMMapDao {

    private SearchBuilder<BackupPolicyVMMapVO> mapSearch;

    public BackupPolicyVMMapDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        mapSearch = createSearchBuilder();
        mapSearch.and("vm_id", mapSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        mapSearch.and("policy_id", mapSearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
        mapSearch.done();
    }

    @Override
    public BackupPolicyVMMapVO findByVMId(long vmId) {
        SearchCriteria<BackupPolicyVMMapVO> sc = mapSearch.create();
        sc.setParameters("vm_id", vmId);
        List<BackupPolicyVMMapVO> maps = listBy(sc);
        if (CollectionUtils.isNotEmpty(maps)) {
            if (maps.size() > 1) {
                throw new CloudRuntimeException("Error: Vm " + vmId + " is assigned to multiple policies");
            }
            return maps.get(0);
        }
        return null;
    }

    @Override
    public List<BackupPolicyVMMapVO> listByPolicyId(long policyId) {
        SearchCriteria<BackupPolicyVMMapVO> sc = mapSearch.create();
        sc.setParameters("policy_id", policyId);
        return listBy(sc);
    }

    @Override
    public BackupPolicyVMMapVO findByPolicyIdAndVMId(long policyId, long vmId) {
        SearchCriteria<BackupPolicyVMMapVO> sc = mapSearch.create();
        sc.setParameters("policy_id", policyId);
        sc.setParameters("vm_id", vmId);
        return findOneBy(sc);
    }
}
