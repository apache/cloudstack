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

import org.apache.cloudstack.api.response.ObjectStoreDetailResponse;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.vo.ImageDataStoreJoinVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.storage.ObjectStore;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
@Local(value={ImageDataStoreJoinDao.class})
public class ImageDataStoreJoinDaoImpl extends GenericDaoBase<ImageDataStoreJoinVO, Long> implements ImageDataStoreJoinDao {
    public static final Logger s_logger = Logger.getLogger(ImageDataStoreJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;

    private final SearchBuilder<ImageDataStoreJoinVO> dsSearch;

    private final SearchBuilder<ImageDataStoreJoinVO> dsIdSearch;


    protected ImageDataStoreJoinDaoImpl() {

        dsSearch = createSearchBuilder();
        dsSearch.and("idIN", dsSearch.entity().getId(), SearchCriteria.Op.IN);
        dsSearch.done();

        dsIdSearch = createSearchBuilder();
        dsIdSearch.and("id", dsIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dsIdSearch.done();

        this._count = "select count(distinct id) from image_data_store_view WHERE ";
    }





    @Override
    public ObjectStoreResponse newObjectStoreResponse(ImageDataStoreJoinVO ids) {
        ObjectStoreResponse osResponse = new ObjectStoreResponse();
        osResponse.setId(ids.getUuid());
        osResponse.setName(ids.getName());
        osResponse.setProviderName(ids.getProviderName());
        osResponse.setProtocol(ids.getProtocol());
        osResponse.setUrl(ids.getUrl());
        osResponse.setScope(ids.getScope());
        osResponse.setZoneId(ids.getZoneUuid());
        osResponse.setZoneName(ids.getZoneName());
        osResponse.setRegionId(ids.getRegionId());
        osResponse.setRegionName(ids.getRegionName());

        String detailName = ids.getDetailName();
        if ( detailName != null && detailName.length() > 0 ){
            ObjectStoreDetailResponse osdResponse = new ObjectStoreDetailResponse(detailName, ids.getDetailValue());
            osResponse.addDetail(osdResponse);
        }
        osResponse.setObjectName("objectstore");
        return osResponse;
    }





    @Override
    public ObjectStoreResponse setObjectStoreResponse(ObjectStoreResponse response, ImageDataStoreJoinVO ids) {
        String detailName = ids.getDetailName();
        if ( detailName != null && detailName.length() > 0 ){
            ObjectStoreDetailResponse osdResponse = new ObjectStoreDetailResponse(detailName, ids.getDetailValue());
            response.addDetail(osdResponse);
        }
        return response;
    }



    @Override
    public List<ImageDataStoreJoinVO> newObjectStoreView(ObjectStore os) {
        SearchCriteria<ImageDataStoreJoinVO> sc = dsIdSearch.create();
        sc.setParameters("id", os.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }



    @Override
    public List<ImageDataStoreJoinVO> searchByIds(Long... spIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if ( batchCfg != null ){
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<ImageDataStoreJoinVO> uvList = new ArrayList<ImageDataStoreJoinVO>();
        // query details by batches
        int curr_index = 0;
        if ( spIds.length > DETAILS_BATCH_SIZE ){
            while ( (curr_index + DETAILS_BATCH_SIZE ) <= spIds.length ) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = spIds[j];
                }
                SearchCriteria<ImageDataStoreJoinVO> sc = dsSearch.create();
                sc.setParameters("idIN", ids);
                List<ImageDataStoreJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
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
            SearchCriteria<ImageDataStoreJoinVO> sc = dsSearch.create();
            sc.setParameters("idIN", ids);
            List<ImageDataStoreJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }




}
