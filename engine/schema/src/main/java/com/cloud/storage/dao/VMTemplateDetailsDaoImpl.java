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
package com.cloud.storage.dao;


import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.storage.ImageStore;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.google.gson.Gson;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.storage.VMTemplateDetailVO;

import java.util.ArrayList;
import java.util.List;

@Component
public class VMTemplateDetailsDaoImpl extends ResourceDetailsDaoBase<VMTemplateDetailVO> implements VMTemplateDetailsDao {

    private final static Logger LOGGER = Logger.getLogger(VMTemplateDetailsDaoImpl.class);

    Gson gson = new Gson();

    SearchBuilder<VMTemplateDetailVO> OptionsSearchBuilder;

    public VMTemplateDetailsDaoImpl() {
        super();
        OptionsSearchBuilder = createSearchBuilder();
        OptionsSearchBuilder.and("resourceId", OptionsSearchBuilder.entity().getResourceId(), SearchCriteria.Op.EQ);
        OptionsSearchBuilder.and("name", OptionsSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        OptionsSearchBuilder.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new VMTemplateDetailVO(resourceId, key, value, display));
    }

    @Override
    public boolean existsOption(long templateId, String key) {
        return findByTemplateAndKey(templateId, key) != null;
    }

    @Override
    public OVFPropertyTO findByTemplateAndKey(long templateId, String key) {
        SearchCriteria<VMTemplateDetailVO> sc = OptionsSearchBuilder.create();
        sc.setParameters("resourceId", templateId);
        sc.setParameters("name", ApiConstants.ACS_PROPERTY + "-" + key);
        OVFPropertyTO property = null;
        VMTemplateDetailVO detail = findOneBy(sc);
        if (detail != null) {
            property = gson.fromJson(detail.getValue(), OVFPropertyTO.class);
        }
        return property;
    }

    @Override
    public void saveOptions(List<OVFPropertyTO> opts) {
        if (CollectionUtils.isEmpty(opts)) {
            return;
        }
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        for (OVFPropertyTO opt : opts) {
            String json = gson.toJson(opt);
            VMTemplateDetailVO templateDetailVO = new VMTemplateDetailVO(opt.getTemplateId(), ApiConstants.ACS_PROPERTY + "-" + opt.getKey(), json, opt.isUserConfigurable());
            persist(templateDetailVO);
        }
        txn.commit();
    }

    @Override
    public List<OVFPropertyTO> listPropertiesByTemplateId(long templateId) {
        SearchCriteria<VMTemplateDetailVO> ssc = createSearchCriteria();
        ssc.addAnd("resourceId", SearchCriteria.Op.EQ, templateId);
        ssc.addAnd("name", SearchCriteria.Op.LIKE, ImageStore.ACS_PROPERTY_PREFIX + "%");

        List<VMTemplateDetailVO> ovfProperties = search(ssc, null);
        List<OVFPropertyTO> properties = new ArrayList<>();
        for (VMTemplateDetailVO property : ovfProperties) {
            OVFPropertyTO ovfPropertyTO = gson.fromJson(property.getValue(), OVFPropertyTO.class);
            properties.add(ovfPropertyTO);
        }
        return properties;
    }

    @Override
    public List<DatadiskTO> listDisksByTemplateId(long templateId) {
        SearchCriteria<VMTemplateDetailVO> ssc = createSearchCriteria();
        ssc.addAnd("resourceId", SearchCriteria.Op.EQ, templateId);
        ssc.addAnd("name", SearchCriteria.Op.LIKE, ImageStore.DISK_DEFINITION_PREFIX + "%");

        List<VMTemplateDetailVO> diskDefinitions = search(ssc, null);
        List<DatadiskTO> disks = new ArrayList<>();
        for (VMTemplateDetailVO detail : diskDefinitions) {
            DatadiskTO datadiskTO = gson.fromJson(detail.getValue(), DatadiskTO.class);
            disks.add(datadiskTO);
        }
        return disks;
    }
}
