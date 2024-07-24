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

import javax.inject.Inject;

import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.vo.HostTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class HostTagDaoImpl extends GenericDaoBase<HostTagVO, Long> implements HostTagDao {
    public static final Logger s_logger = Logger.getLogger(HostTagDaoImpl.class);

    @Inject
    private ConfigurationDao _configDao;

    private final SearchBuilder<HostTagVO> stSearch;
    private final SearchBuilder<HostTagVO> stIdSearch;

    protected HostTagDaoImpl() {
        stSearch = createSearchBuilder();

        stSearch.and("idIN", stSearch.entity().getId(), SearchCriteria.Op.IN);
        stSearch.done();

        stIdSearch = createSearchBuilder();

        stIdSearch.and("id", stIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        stIdSearch.done();

        _count = "select count(distinct id) from host_tags WHERE ";
    }

    @Override
    public HostTagResponse newHostTagResponse(HostTagVO tag) {
        HostTagResponse tagResponse = new HostTagResponse();

        tagResponse.setName(tag.getName());
        tagResponse.setHostId(tag.getHostId());

        tagResponse.setObjectName("hosttag");

        return tagResponse;
    }

    @Override
    public List<HostTagVO> searchByIds(Long... stIds) {
        String batchCfg = _configDao.getValue("detail.batch.query.size");

        final int detailsBatchSize = batchCfg != null ? Integer.parseInt(batchCfg) : 2000;

        // query details by batches
        List<HostTagVO> uvList = new ArrayList<HostTagVO>();
        int curr_index = 0;

        if (stIds.length > detailsBatchSize) {
            while ((curr_index + detailsBatchSize) <= stIds.length) {
                Long[] ids = new Long[detailsBatchSize];

                for (int k = 0, j = curr_index; j < curr_index + detailsBatchSize; j++, k++) {
                    ids[k] = stIds[j];
                }

                SearchCriteria<HostTagVO> sc = stSearch.create();

                sc.setParameters("idIN", (Object[])ids);

                List<HostTagVO> vms = searchIncludingRemoved(sc, null, null, false);

                if (vms != null) {
                    uvList.addAll(vms);
                }

                curr_index += detailsBatchSize;
            }
        }

        if (curr_index < stIds.length) {
            int batch_size = (stIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];

            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = stIds[j];
            }

            SearchCriteria<HostTagVO> sc = stSearch.create();

            sc.setParameters("idIN", (Object[])ids);

            List<HostTagVO> vms = searchIncludingRemoved(sc, null, null, false);

            if (vms != null) {
                uvList.addAll(vms);
            }
        }

        return uvList;
    }
}
