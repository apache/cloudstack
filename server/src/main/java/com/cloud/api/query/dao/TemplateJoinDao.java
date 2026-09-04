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

import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

public interface TemplateJoinDao extends GenericDao<TemplateJoinVO, Long> {

    TemplateResponse newTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateJoinVO tmpl);

    TemplateResponse newIsoResponse(TemplateJoinVO tmpl, ResponseView view);

    TemplateResponse newUpdateResponse(TemplateJoinVO tmpl);

    TemplateResponse setTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateResponse tmplData, TemplateJoinVO tmpl);

    List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate tmpl);

    List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate tmpl, long zoneId, boolean readyOnly);

    List<TemplateJoinVO> searchByTemplateZonePair( Boolean showRemoved, String... pairs);

    List<TemplateJoinVO> listActiveTemplates(long storeId);

    List<TemplateJoinVO> listPublicTemplates();

    Pair<List<TemplateJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<TemplateJoinVO> sc, final Filter filter);

    /**
     * Bypass-the-view Phase 1 implementation. Issues a hand-tuned SQL query
     * directly against the underlying tables (vm_template, account,
     * template_store_ref, image_store, template_zone_ref, data_center) instead
     * of going through {@code template_view}. Caller must check
     * {@link TemplateListFilter#canBypass()} first; this method does not handle
     * tags, sharedAccountIds, domainPath, or featured/community-style domainId
     * filters and will throw IllegalArgumentException if those are populated.
     *
     * Returns TemplateJoinVO objects with only {@code id} (when
     * {@code filter.showUnique}) or {@code tempZonePair} populated, plus the
     * total distinct count for pagination.
     */
    Pair<List<TemplateJoinVO>, Integer> findDistinctTempZonePairs(TemplateListFilter filter);

    List<TemplateJoinVO> findByDistinctIds(Long... ids);
}
