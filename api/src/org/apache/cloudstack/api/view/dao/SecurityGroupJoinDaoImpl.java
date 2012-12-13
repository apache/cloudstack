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
package org.apache.cloudstack.api.view.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.SecurityGroupRuleResponse;
import org.apache.cloudstack.api.view.DBViewUtils;
import org.apache.cloudstack.api.view.ViewResponseHelper;
import org.apache.cloudstack.api.view.vo.ResourceTagJoinVO;
import org.apache.cloudstack.api.view.vo.SecurityGroupJoinVO;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={SecurityGroupJoinDao.class})
public class SecurityGroupJoinDaoImpl extends GenericDaoBase<SecurityGroupJoinVO, Long> implements SecurityGroupJoinDao {
    public static final Logger s_logger = Logger.getLogger(SecurityGroupJoinDaoImpl.class);

    private SearchBuilder<SecurityGroupJoinVO> sgSearch;

    private SearchBuilder<SecurityGroupJoinVO> sgIdSearch;

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

        ViewResponseHelper.populateOwner(sgResponse, vsg);

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

            if (vsg.getRuleType() == SecurityRuleType.IngressRule) {
                ruleData.setObjectName("ingressrule");
                sgResponse.addSecurityGroupIngressRule(ruleData);
            } else {
                ruleData.setObjectName("egressrule");
                sgResponse.addSecurityGroupEgressRule(ruleData);
            }
        }

        // update tag information
        Long tag_id = vsg.getTagId();
        if (tag_id != null && tag_id.longValue() > 0) {
            ResourceTagJoinVO vtag = DBViewUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                sgResponse.addTag(DBViewUtils.newResourceTagResponse(vtag, false));
            }
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
        if (tag_id != null && tag_id.longValue() > 0 ) {
            ResourceTagJoinVO vtag = DBViewUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                vsgData.addTag(DBViewUtils.newResourceTagResponse(vtag, false));
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
    public List<SecurityGroupJoinVO> searchByIds(Long... ids) {
        SearchCriteria<SecurityGroupJoinVO> sc = sgSearch.create();
        sc.setParameters("idIN", ids);
        return searchIncludingRemoved(sc, null, null, false);
    }
}
