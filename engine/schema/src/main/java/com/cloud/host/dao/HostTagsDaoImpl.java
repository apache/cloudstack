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
package com.cloud.host.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.host.HostTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.SearchCriteria.Func;

import javax.inject.Inject;

@Component
public class HostTagsDaoImpl extends GenericDaoBase<HostTagVO, Long> implements HostTagsDao, Configurable {
    protected final SearchBuilder<HostTagVO> HostSearch;
    protected final GenericSearchBuilder<HostTagVO, String> DistinctImplictTagsSearch;
    private final SearchBuilder<HostTagVO> stSearch;
    private final SearchBuilder<HostTagVO> tagIdsearch;
    private final SearchBuilder<HostTagVO> ImplicitTagsSearch;

    @Inject
    private ConfigurationDao _configDao;

    public HostTagsDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.and("isImplicit", HostSearch.entity().getIsImplicit(), SearchCriteria.Op.EQ);
        HostSearch.and("isTagARule", HostSearch.entity().getIsTagARule(), SearchCriteria.Op.EQ);
        HostSearch.done();

        DistinctImplictTagsSearch = createSearchBuilder(String.class);
        DistinctImplictTagsSearch.select(null, Func.DISTINCT, DistinctImplictTagsSearch.entity().getTag());
        DistinctImplictTagsSearch.and("hostIds", DistinctImplictTagsSearch.entity().getHostId(), SearchCriteria.Op.IN);
        DistinctImplictTagsSearch.and("implicitTags", DistinctImplictTagsSearch.entity().getTag(), SearchCriteria.Op.IN);
        DistinctImplictTagsSearch.done();

        stSearch = createSearchBuilder();
        stSearch.and("idIN", stSearch.entity().getId(), SearchCriteria.Op.IN);
        stSearch.done();

        tagIdsearch = createSearchBuilder();
        tagIdsearch.and("id", tagIdsearch.entity().getId(), SearchCriteria.Op.EQ);
        tagIdsearch.done();

        ImplicitTagsSearch = createSearchBuilder();
        ImplicitTagsSearch.and("hostId", ImplicitTagsSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        ImplicitTagsSearch.and("isImplicit", ImplicitTagsSearch.entity().getIsImplicit(), SearchCriteria.Op.EQ);
        ImplicitTagsSearch.done();
    }

    @Override
    public List<HostTagVO> getHostTags(long hostId) {
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);

        return search(sc, null);
    }

    @Override
    public List<String> getDistinctImplicitHostTags(List<Long> hostIds, String[] implicitHostTags) {
        SearchCriteria<String> sc = DistinctImplictTagsSearch.create();
        sc.setParameters("hostIds", hostIds.toArray(new Object[hostIds.size()]));
        sc.setParameters("implicitTags", (Object[])implicitHostTags);
        return customSearch(sc, null);
    }

    @Override
    public void deleteTags(long hostId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        expunge(sc);
        txn.commit();
    }

    @Override
    public boolean updateImplicitTags(long hostId, List<String> hostTags) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<HostTagVO> sc = ImplicitTagsSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("isImplicit", true);
        boolean expunged = expunge(sc) > 0;
        boolean persisted = false;
        for (String tag : hostTags) {
            if (StringUtils.isNotBlank(tag)) {
                HostTagVO vo = new HostTagVO(hostId, tag.trim());
                vo.setIsImplicit(true);
                persist(vo);
                persisted = true;
            }
        }
        txn.commit();
        return expunged || persisted;
    }

    @Override
    public List<HostTagVO> getExplicitHostTags(long hostId) {
        SearchCriteria<HostTagVO> sc = ImplicitTagsSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("isImplicit", false);

        return search(sc, null);
    }

    @Override
    public List<HostTagVO> findHostRuleTags() {
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("isTagARule", true);

        return search(sc, null);
    }

    @Override
    public void persist(long hostId, List<String> hostTags, Boolean isTagARule) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("isImplicit", false);
        expunge(sc);

        for (String tag : hostTags) {
            tag = tag.trim();
            if (tag.length() > 0) {
                HostTagVO vo = new HostTagVO(hostId, tag, isTagARule);
                persist(vo);
            }
        }
        txn.commit();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {hostTagRuleExecutionTimeout};
    }

    @Override
    public String getConfigComponentName() {
        return HostTagsDaoImpl.class.getSimpleName();
    }

    @Override
    public HostTagResponse newHostTagResponse(HostTagVO tag) {
        HostTagResponse tagResponse = new HostTagResponse();

        tagResponse.setName(tag.getTag());
        tagResponse.setHostId(tag.getHostId());
        tagResponse.setImplicit(tag.getIsImplicit());

        tagResponse.setObjectName("hosttag");

        return tagResponse;
    }

    @Override
    public List<HostTagVO> searchByIds(Long... tagIds) {
        String batchCfg = _configDao.getValue("detail.batch.query.size");

        final int detailsBatchSize = batchCfg != null ? Integer.parseInt(batchCfg) : 2000;

        // query details by batches
        List<HostTagVO> tagList = new ArrayList<>();
        int curr_index = 0;

        if (tagIds.length > detailsBatchSize) {
            while ((curr_index + detailsBatchSize) <= tagIds.length) {
                Long[] ids = new Long[detailsBatchSize];

                for (int k = 0, j = curr_index; j < curr_index + detailsBatchSize; j++, k++) {
                    ids[k] = tagIds[j];
                }

                SearchCriteria<HostTagVO> sc = stSearch.create();

                sc.setParameters("idIN", (Object[])ids);

                List<HostTagVO> vms = searchIncludingRemoved(sc, null, null, false);

                if (vms != null) {
                    tagList.addAll(vms);
                }

                curr_index += detailsBatchSize;
            }
        }

        if (curr_index < tagIds.length) {
            int batch_size = (tagIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];

            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = tagIds[j];
            }

            SearchCriteria<HostTagVO> sc = stSearch.create();

            sc.setParameters("idIN", (Object[])ids);

            List<HostTagVO> tags = searchIncludingRemoved(sc, null, null, false);

            if (tags != null) {
                tagList.addAll(tags);
            }
        }

        return tagList;
    }
}
