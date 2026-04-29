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
package com.cloud.deployasis.dao;

import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.deployasis.DeployAsIsConstants;
import com.cloud.deployasis.TemplateDeployAsIsDetailVO;
import com.cloud.utils.db.SearchCriteria;
import com.google.gson.Gson;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TemplateDeployAsIsDetailsDaoImpl extends ResourceDetailsDaoBase<TemplateDeployAsIsDetailVO> implements TemplateDeployAsIsDetailsDao {

    private Gson gson = new Gson();

    public TemplateDeployAsIsDetailsDaoImpl() {
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new TemplateDeployAsIsDetailVO(resourceId, key, value));
    }

    @Override
    public OVFPropertyTO findPropertyByTemplateAndKey(long templateId, String key) {
        SearchCriteria<TemplateDeployAsIsDetailVO> sc = createSearchCriteria();
        sc.addAnd("resourceId", SearchCriteria.Op.EQ, templateId);
        sc.addAnd("name", SearchCriteria.Op.EQ, key.startsWith(DeployAsIsConstants.PROPERTY_PREFIX) ? key : DeployAsIsConstants.PROPERTY_PREFIX + key);
        OVFPropertyTO property = null;
        TemplateDeployAsIsDetailVO detail = findOneBy(sc);
        if (detail != null) {
            property = gson.fromJson(detail.getValue(), OVFPropertyTO.class);
        }
        return property;
    }

    @Override
    public List<TemplateDeployAsIsDetailVO> listDetailsByTemplateIdMatchingPrefix(long templateId, String prefix) {
        SearchCriteria<TemplateDeployAsIsDetailVO> ssc = createSearchCriteria();
        ssc.addAnd("resourceId", SearchCriteria.Op.EQ, templateId);
        ssc.addAnd("name", SearchCriteria.Op.LIKE, prefix + "%");

        return search(ssc, null);
    }

    @Override
    public List<OVFNetworkTO> listNetworkRequirementsByTemplateId(long templateId) {
        List<TemplateDeployAsIsDetailVO> networkDetails = listDetailsByTemplateIdMatchingPrefix(templateId, DeployAsIsConstants.NETWORK_PREFIX);
        List<OVFNetworkTO> networkPrereqs = new ArrayList<>();
        for (TemplateDeployAsIsDetailVO property : networkDetails) {
            OVFNetworkTO ovfPropertyTO = gson.fromJson(property.getValue(), OVFNetworkTO.class);
            networkPrereqs.add(ovfPropertyTO);
        }
        networkPrereqs.sort(new Comparator<OVFNetworkTO>() {
            @Override
            public int compare(OVFNetworkTO o1, OVFNetworkTO o2) {
                return o1.getInstanceID() - o2.getInstanceID();
            }
        });
        return networkPrereqs;
    }
}
