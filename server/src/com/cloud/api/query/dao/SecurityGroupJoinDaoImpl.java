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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.SecurityGroupRuleResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local(value = {SecurityGroupJoinDao.class})
public class SecurityGroupJoinDaoImpl extends GenericDaoBase<SecurityGroupJoinVO, Long> implements SecurityGroupJoinDao {
    public static final Logger s_logger = Logger.getLogger(SecurityGroupJoinDaoImpl.class);

    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ResourceTagJoinDao _resourceTagJoinDao;
    @Inject
    private SecurityGroupVMMapDao _securityGroupVMMapDao;
    @Inject
    private UserVmDao _userVmDao;

    private final SearchBuilder<SecurityGroupJoinVO> sgSearch;

    private final SearchBuilder<SecurityGroupJoinVO> sgIdSearch;

    protected SecurityGroupJoinDaoImpl() {

        sgSearch = createSearchBuilder();
        sgSearch.and("idIN", sgSearch.entity().getId(), SearchCriteria.Op.IN);
        sgSearch.done();

        sgIdSearch = createSearchBuilder();
        sgIdSearch.and("id", sgIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        sgIdSearch.done();

        this._count = "select count(distinct id) from security_group_view WHERE ";
    }

    @Override
    public SecurityGroupResponse newSecurityGroupResponse(SecurityGroupJoinVO vsg, Account caller) {
        SecurityGroupResponse sgResponse = new SecurityGroupResponse();
        sgResponse.setId(vsg.getUuid());
        sgResponse.setName(vsg.getName());
        sgResponse.setDescription(vsg.getDescription());

        ApiResponseHelper.populateOwner(sgResponse, vsg);

        Long rule_id = vsg.getRuleId();
        if (rule_id != null && rule_id.longValue() > 0) {
            SecurityGroupRuleResponse ruleData = new SecurityGroupRuleResponse();
            ruleData.setRuleId(vsg.getRuleUuid());
            ruleData.setProtocol(vsg.getRuleProtocol());

            if ("icmp".equalsIgnoreCase(vsg.getRuleProtocol())) {
                ruleData.setIcmpType(vsg.getRuleStartPort());
                ruleData.setIcmpCode(vsg.getRuleEndPort());
            } else {
                ruleData.setStartPort(vsg.getRuleStartPort());
                ruleData.setEndPort(vsg.getRuleEndPort());
            }

            if (vsg.getRuleAllowedNetworkId() != null) {
                List<SecurityGroupJoinVO> sgs = this.searchByIds(vsg.getRuleAllowedNetworkId());
                if (sgs != null && sgs.size() > 0) {
                    SecurityGroupJoinVO sg = sgs.get(0);
                    ruleData.setSecurityGroupName(sg.getName());
                    ruleData.setAccountName(sg.getAccountName());
                }
            } else {
                ruleData.setCidr(vsg.getRuleAllowedSourceIpCidr());
            }

            // list the tags by rule uuid
            List<ResourceTagJoinVO> tags = _resourceTagJoinDao.listBy(vsg.getRuleUuid(), ResourceTag.ResourceObjectType.SecurityGroupRule);
            Set<ResourceTagResponse> tagResponse = new HashSet<ResourceTagResponse>();
            for (ResourceTagJoinVO tag: tags) {
                tagResponse.add(ApiDBUtils.newResourceTagResponse(tag, false));
            }

            // add the tags to the rule data
            ruleData.setTags(tagResponse);

            if (vsg.getRuleType() == SecurityRuleType.IngressRule) {
                ruleData.setObjectName("ingressrule");
                sgResponse.addSecurityGroupIngressRule(ruleData);
            } else {
                ruleData.setObjectName("egressrule");
                sgResponse.addSecurityGroupEgressRule(ruleData);
            }
        }

        List<SecurityGroupVMMapVO> securityGroupVmMap = _securityGroupVMMapDao.listBySecurityGroup(vsg.getId());
        s_logger.debug("newSecurityGroupResponse() -> virtualmachine count: " + securityGroupVmMap.size());
        sgResponse.setVirtualMachineCount(securityGroupVmMap.size());

        for(SecurityGroupVMMapVO securityGroupVMMapVO : securityGroupVmMap) {
            final UserVmVO userVmVO = _userVmDao.findById(securityGroupVMMapVO.getInstanceId());
            if (userVmVO != null) {
                sgResponse.addVirtualMachineId(userVmVO.getUuid());
            }
        }

        // update tag information
        Long tag_id = vsg.getTagId();
        if (tag_id != null && tag_id.longValue() > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if (vtag != null) {
                sgResponse.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        // set async job
        if (vsg.getJobId() != null) {
            sgResponse.setJobId(vsg.getJobUuid());
            sgResponse.setJobStatus(vsg.getJobStatus());
        }

        sgResponse.setObjectName("securitygroup");

        return sgResponse;
    }

    @Override
    public SecurityGroupResponse setSecurityGroupResponse(SecurityGroupResponse vsgData, SecurityGroupJoinVO vsg) {
        Long rule_id = vsg.getRuleId();
        if (rule_id != null && rule_id.longValue() > 0) {
            SecurityGroupRuleResponse ruleData = new SecurityGroupRuleResponse();
            ruleData.setRuleId(vsg.getRuleUuid());
            ruleData.setProtocol(vsg.getRuleProtocol());

            if ("icmp".equalsIgnoreCase(vsg.getRuleProtocol())) {
                ruleData.setIcmpType(vsg.getRuleStartPort());
                ruleData.setIcmpCode(vsg.getRuleEndPort());
            } else {
                ruleData.setStartPort(vsg.getRuleStartPort());
                ruleData.setEndPort(vsg.getRuleEndPort());
            }

            if (vsg.getRuleAllowedNetworkId() != null) {
                List<SecurityGroupJoinVO> sgs = this.searchByIds(vsg.getRuleAllowedNetworkId());
                if (sgs != null && sgs.size() > 0) {
                    SecurityGroupJoinVO sg = sgs.get(0);
                    ruleData.setSecurityGroupName(sg.getName());
                    ruleData.setAccountName(sg.getAccountName());
                }
            } else {
                ruleData.setCidr(vsg.getRuleAllowedSourceIpCidr());
            }

            // add the tags to the rule data
            List<ResourceTagJoinVO> tags = _resourceTagJoinDao.listBy(vsg.getRuleUuid(), ResourceTag.ResourceObjectType.SecurityGroupRule);
            Set<ResourceTagResponse> tagResponse = new HashSet<ResourceTagResponse>();
            for (ResourceTagJoinVO tag: tags) {
                tagResponse.add(ApiDBUtils.newResourceTagResponse(tag, false));
            }

            // add the tags to the rule data
            ruleData.setTags(tagResponse);

            if (vsg.getRuleType() == SecurityRuleType.IngressRule) {
                ruleData.setObjectName("ingressrule");
                vsgData.addSecurityGroupIngressRule(ruleData);
            } else {
                ruleData.setObjectName("egressrule");
                vsgData.addSecurityGroupEgressRule(ruleData);
            }
        }

        // update tag information
        Long tag_id = vsg.getTagId();
        if (tag_id != null && tag_id.longValue() > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if (vtag != null) {
                vsgData.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }
        return vsgData;
    }

    @Override
    public List<SecurityGroupJoinVO> newSecurityGroupView(SecurityGroup sg) {

        SearchCriteria<SecurityGroupJoinVO> sc = sgIdSearch.create();
        sc.setParameters("id", sg.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<SecurityGroupJoinVO> searchByIds(Long... sgIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<SecurityGroupJoinVO> uvList = new ArrayList<SecurityGroupJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (sgIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= sgIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = sgIds[j];
                }
                SearchCriteria<SecurityGroupJoinVO> sc = sgSearch.create();
                sc.setParameters("idIN", ids);
                List<SecurityGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < sgIds.length) {
            int batch_size = (sgIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = sgIds[j];
            }
            SearchCriteria<SecurityGroupJoinVO> sc = sgSearch.create();
            sc.setParameters("idIN", ids);
            List<SecurityGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }
}
