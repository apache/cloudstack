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

import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ImageStoreDetailResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.storage.ImageStore;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {ImageStoreJoinDao.class})
public class ImageStoreJoinDaoImpl extends GenericDaoBase<ImageStoreJoinVO, Long> implements ImageStoreJoinDao {

    @Inject
    private ConfigurationDao _configDao;

    private final SearchBuilder<ImageStoreJoinVO> dsSearch;

    private final SearchBuilder<ImageStoreJoinVO> dsIdSearch;

    protected ImageStoreJoinDaoImpl() {

        dsSearch = createSearchBuilder();
        dsSearch.and("idIN", dsSearch.entity().getId(), SearchCriteria.Op.IN);
        dsSearch.done();

        dsIdSearch = createSearchBuilder();
        dsIdSearch.and("id", dsIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dsIdSearch.done();

        _count = "select count(distinct id) from image_store_view WHERE ";
    }

    @Override
    public ImageStoreResponse newImageStoreResponse(ImageStoreJoinVO ids) {
        ImageStoreResponse osResponse = new ImageStoreResponse();
        osResponse.setId(ids.getUuid());
        osResponse.setName(ids.getName());
        osResponse.setProviderName(ids.getProviderName());
        osResponse.setProtocol(ids.getProtocol());
        String url = ids.getUrl();
        //if store is type cifs, remove the password
        if(ids.getProtocol().equals("cifs".toString())) {
            url = StringUtils.cleanString(url);
        }
        osResponse.setUrl(url);
        osResponse.setScope(ids.getScope());
        osResponse.setZoneId(ids.getZoneUuid());
        osResponse.setZoneName(ids.getZoneName());

        String detailName = ids.getDetailName();
        if ( detailName != null && detailName.length() > 0 && !detailName.equals(ApiConstants.PASSWORD)) {
            String detailValue = ids.getDetailValue();
            if (detailName.equals(ApiConstants.KEY) || detailName.equals(ApiConstants.S3_SECRET_KEY)) {
                // ALWAYS return an empty value for the S3 secret key since that key is managed by Amazon and not CloudStack
                detailValue = "";
            }
            ImageStoreDetailResponse osdResponse = new ImageStoreDetailResponse(detailName, detailValue);
            osResponse.addDetail(osdResponse);
        }
        osResponse.setObjectName("imagestore");
        return osResponse;
    }

    @Override
    public ImageStoreResponse setImageStoreResponse(ImageStoreResponse response, ImageStoreJoinVO ids) {
        String detailName = ids.getDetailName();
        if ( detailName != null && detailName.length() > 0 && !detailName.equals(ApiConstants.PASSWORD)) {
            String detailValue = ids.getDetailValue();
            if (detailName.equals(ApiConstants.KEY) || detailName.equals(ApiConstants.S3_SECRET_KEY)) {
                // ALWAYS return an empty value for the S3 secret key since that key is managed by Amazon and not CloudStack
                detailValue = "";
            }
            ImageStoreDetailResponse osdResponse = new ImageStoreDetailResponse(detailName, detailValue);
            response.addDetail(osdResponse);
        }
        return response;
    }

    @Override
    public List<ImageStoreJoinVO> newImageStoreView(ImageStore os) {
        SearchCriteria<ImageStoreJoinVO> sc = dsIdSearch.create();
        sc.setParameters("id", os.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<ImageStoreJoinVO> searchByIds(Long... spIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<ImageStoreJoinVO> uvList = new ArrayList<ImageStoreJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (spIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= spIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = spIds[j];
                }
                SearchCriteria<ImageStoreJoinVO> sc = dsSearch.create();
                sc.setParameters("idIN", ids);
                List<ImageStoreJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
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
            SearchCriteria<ImageStoreJoinVO> sc = dsSearch.create();
            sc.setParameters("idIN", ids);
            List<ImageStoreJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

}
