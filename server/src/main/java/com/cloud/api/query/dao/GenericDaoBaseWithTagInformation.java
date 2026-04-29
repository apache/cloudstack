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

import org.apache.cloudstack.api.BaseResponseWithTagInformation;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.BaseViewWithTagInformationVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.utils.db.GenericDaoBase;

public abstract class GenericDaoBaseWithTagInformation<T extends BaseViewWithTagInformationVO, Z extends BaseResponseWithTagInformation> extends GenericDaoBase<T, Long> {

    /**
     * Update tag information on baseResponse
     * @param baseView base view containing tag information
     * @param baseResponse response to update
     */
    protected void addTagInformation(T baseView, Z baseResponse) {
        ResourceTagJoinVO vtag = new ResourceTagJoinVO();
        vtag.setId(baseView.getTagId());
        vtag.setUuid(baseView.getTagUuid());
        vtag.setKey(baseView.getTagKey());
        vtag.setValue(baseView.getTagValue());
        vtag.setDomainId(baseView.getTagDomainId());
        vtag.setAccountId(baseView.getTagAccountId());
        vtag.setResourceId(baseView.getTagResourceId());
        vtag.setResourceUuid(baseView.getTagResourceUuid());
        vtag.setResourceType(baseView.getTagResourceType());
        vtag.setCustomer(baseView.getTagCustomer());
        vtag.setAccountName(baseView.getTagAccountName());
        vtag.setDomainName(baseView.getTagDomainName());
        vtag.setDomainUuid(baseView.getTagDomainUuid());
        baseResponse.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
    }

}
