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
package com.cloud.api.query.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StorageStats;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
@Local(value={StoragePoolJoinDao.class})
public class StoragePoolJoinDaoImpl extends GenericDaoBase<StoragePoolJoinVO, Long> implements StoragePoolJoinDao {
    public static final Logger s_logger = Logger.getLogger(StoragePoolJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;

    private final SearchBuilder<StoragePoolJoinVO> spSearch;

    private final SearchBuilder<StoragePoolJoinVO> spIdSearch;


    protected StoragePoolJoinDaoImpl() {

        spSearch = createSearchBuilder();
        spSearch.and("idIN", spSearch.entity().getId(), SearchCriteria.Op.IN);
        spSearch.done();

        spIdSearch = createSearchBuilder();
        spIdSearch.and("id", spIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        spIdSearch.done();

        this._count = "select count(distinct id) from storage_pool_view WHERE ";
    }





    @Override
    public StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO pool) {
        StoragePoolResponse poolResponse = new StoragePoolResponse();
        poolResponse.setId(pool.getUuid());
        poolResponse.setName(pool.getName());
        poolResponse.setState(pool.getStatus());
        poolResponse.setPath(pool.getPath());
        poolResponse.setIpAddress(pool.getHostAddress());
        poolResponse.setZoneId(pool.getZoneUuid());
        poolResponse.setZoneName(pool.getZoneName());
        if (pool.getPoolType() != null) {
            poolResponse.setType(pool.getPoolType().toString());
        }
        poolResponse.setPodId(pool.getPodUuid());
        poolResponse.setPodName(pool.getPodName());
        poolResponse.setCreated(pool.getCreated());
        poolResponse.setScope(pool.getScope().toString());


        long allocatedSize = pool.getUsedCapacity() +  pool.getReservedCapacity();
        poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
        poolResponse.setDiskSizeAllocated(allocatedSize);

        //TODO: StatsCollector does not persist data
        StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
        if (stats != null) {
            Long used = stats.getByteUsed();
            poolResponse.setDiskSizeUsed(used);
        }

        poolResponse.setClusterId(pool.getClusterUuid());
        poolResponse.setClusterName(pool.getClusterName());
        poolResponse.setTags(pool.getTag());

        // set async job
        poolResponse.setJobId(pool.getJobUuid());
        poolResponse.setJobStatus(pool.getJobStatus());

        poolResponse.setObjectName("storagepool");
        return poolResponse;
    }





    @Override
    public StoragePoolResponse setStoragePoolResponse(StoragePoolResponse response, StoragePoolJoinVO sp) {
        String tag = sp.getTag();
        if (tag != null) {
            if ( response.getTags() != null && response.getTags().length() > 0){
                response.setTags(response.getTags() + "," + tag);
            }
            else{
                response.setTags(tag);
            }
        }
        return response;
    }



    @Override
    public List<StoragePoolJoinVO> newStoragePoolView(StoragePool host) {
        SearchCriteria<StoragePoolJoinVO> sc = spIdSearch.create();
        sc.setParameters("id", host.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }



    @Override
    public List<StoragePoolJoinVO> searchByIds(Long... spIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if ( batchCfg != null ){
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<StoragePoolJoinVO> uvList = new ArrayList<StoragePoolJoinVO>();
        // query details by batches
        int curr_index = 0;
        if ( spIds.length > DETAILS_BATCH_SIZE ){
            while ( (curr_index + DETAILS_BATCH_SIZE ) <= spIds.length ) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = spIds[j];
                }
                SearchCriteria<StoragePoolJoinVO> sc = spSearch.create();
                sc.setParameters("idIN", ids);
                List<StoragePoolJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < spIds.length) {
            int batch_size = (spIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = spIds[j];
            }
            SearchCriteria<StoragePoolJoinVO> sc = spSearch.create();
            sc.setParameters("idIN", ids);
            List<StoragePoolJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }




}
