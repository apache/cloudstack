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
package com.cloud.vm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import com.cloud.api.response.NicResponse;
import com.cloud.api.response.ResourceTagResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.api.view.vo.UserVmJoinVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VmStats;


@Local(value={UserVmJoinDao.class})
public class UserVmJoinDaoImpl extends GenericDaoBase<UserVmJoinVO, Long> implements UserVmJoinDao {
    public static final Logger s_logger = Logger.getLogger(UserVmJoinDaoImpl.class);

    private static final int VM_DETAILS_BATCH_SIZE=100;

    private SearchBuilder<UserVmJoinVO> VmDetailSearch;

    protected UserVmJoinDaoImpl() {

        VmDetailSearch = createSearchBuilder();
        VmDetailSearch.and("idIN", VmDetailSearch.entity().getId(), SearchCriteria.Op.IN);
        VmDetailSearch.done();

        this._count = "select count(distinct id) from user_vm_view WHERE ";
    }

    public UserVmResponse newUserVmResponse(String objectName, UserVmJoinVO userVm, EnumSet<VMDetails> details, Account caller) {
        UserVmResponse userVmResponse = new UserVmResponse();

        if (userVm.getHypervisorType() != null){
            userVmResponse.setHypervisor(userVm.getHypervisorType().toString());
        }
        userVmResponse.setId(userVm.getUuid());
        userVmResponse.setName(userVm.getName());

        userVmResponse.setDisplayName(userVm.getDisplayName());

        if (userVm.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
            userVmResponse.setProjectId(userVm.getProjectUuid());
            userVmResponse.setProjectName(userVm.getProjectName());
        } else {
            userVmResponse.setAccountName(userVm.getAccountName());
        }

        userVmResponse.setDomainId(userVm.getDomainUuid());
        userVmResponse.setDomainName(userVm.getDomainName());

        userVmResponse.setCreated(userVm.getCreated());

        if (userVm.getState() != null) {
            userVmResponse.setState(userVm.getState().toString());
        }
        userVmResponse.setHaEnable(userVm.isHaEnabled());
        if (details.contains(VMDetails.all) || details.contains(VMDetails.group)) {
            userVmResponse.setGroupId(userVm.getInstanceGroupUuid());
            userVmResponse.setGroup(userVm.getInstanceGroupName());
        }
        userVmResponse.setZoneId(userVm.getDataCenterUuid());
        userVmResponse.setZoneName(userVm.getDataCenterName());
        if ((caller == null) || (caller.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            userVmResponse.setInstanceName(userVm.getInstanceName());
            userVmResponse.setHostId(userVm.getHostUuid());
            userVmResponse.setHostName(userVm.getHostName());
        }

        if (details.contains(VMDetails.all) || details.contains(VMDetails.tmpl)) {
            userVmResponse.setTemplateId(userVm.getTemplateUuid());
            userVmResponse.setTemplateName(userVm.getTemplateName());
            userVmResponse.setTemplateDisplayText(userVm.getTemplateDisplayText());
            userVmResponse.setPasswordEnabled(userVm.isPasswordEnabled());
        }
        if (details.contains(VMDetails.all) || details.contains(VMDetails.iso)) {
            userVmResponse.setIsoId(userVm.getIsoUuid());
            userVmResponse.setIsoName(userVm.getIsoName());
            userVmResponse.setIsoDisplayText(userVm.getIsoDisplayText());
        }
        if (details.contains(VMDetails.all) || details.contains(VMDetails.servoff)) {
            userVmResponse.setServiceOfferingId(userVm.getServiceOfferingUuid());
            userVmResponse.setServiceOfferingName(userVm.getServiceOfferingName());
            userVmResponse.setCpuNumber(userVm.getCpu());
            userVmResponse.setCpuSpeed(userVm.getSpeed());
            userVmResponse.setMemory(userVm.getRamSize());
        }
        userVmResponse.setGuestOsId(userVm.getGuestOsUuid());
        if (details.contains(VMDetails.all) || details.contains(VMDetails.volume)) {
            userVmResponse.setRootDeviceId(userVm.getVolumeDeviceId());
            if (userVm.getVolumeType() != null) {
                userVmResponse.setRootDeviceType(userVm.getVolumeType().toString());
            }
        }
        userVmResponse.setPassword(userVm.getPassword());
        userVmResponse.setJobUuid(userVm.getJobUuid());
        userVmResponse.setJobStatus(userVm.getJobStatus());
        //userVmResponse.setForVirtualNetwork(userVm.getForVirtualNetwork());

        userVmResponse.setPublicIpId(userVm.getPublicIpUuid());
        userVmResponse.setPublicIp(userVm.getPublicIpAddress());
        userVmResponse.setKeyPairName(userVm.getKeypairName());

        if (details.contains(VMDetails.all) || details.contains(VMDetails.stats)) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            // stats calculation
            String cpuUsed = null;
            VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
            if (vmStats != null) {
                float cpuUtil = (float) vmStats.getCPUUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                userVmResponse.setCpuUsed(cpuUsed);

                Double networkKbRead = Double.valueOf(vmStats.getNetworkReadKBs());
                userVmResponse.setNetworkKbsRead(networkKbRead.longValue());

                Double networkKbWrite = Double.valueOf(vmStats.getNetworkWriteKBs());
                userVmResponse.setNetworkKbsWrite(networkKbWrite.longValue());
            }
        }

        if (details.contains(VMDetails.all) || details.contains(VMDetails.secgrp)) {
            Long securityGroupId = userVm.getSecurityGroupId();
            if (securityGroupId != null && securityGroupId.longValue() != 0) {
                SecurityGroupResponse resp = new SecurityGroupResponse();
                resp.setId(userVm.getSecurityGroupUuid());
                resp.setName(userVm.getSecurityGroupName());
                resp.setDescription(userVm.getSecurityGroupDescription());
                resp.setObjectName("securitygroup");
                if (userVm.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
                    resp.setProjectId(userVm.getProjectUuid());
                    resp.setProjectName(userVm.getProjectName());
                } else {
                    resp.setAccountName(userVm.getAccountName());
                }
                userVmResponse.addSecurityGroup(resp);
            }
        }

        if (details.contains(VMDetails.all) || details.contains(VMDetails.nics)) {
            long nic_id = userVm.getNicId();
            if (nic_id > 0) {
                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(userVm.getNicUuid());
                nicResponse.setIpaddress(userVm.getIpAddress());
                nicResponse.setGateway(userVm.getGateway());
                nicResponse.setNetmask(userVm.getNetmask());
                nicResponse.setNetworkid(userVm.getNetworkUuid());
                nicResponse.setMacAddress(userVm.getMacAddress());
                if (userVm.getBroadcastUri() != null) {
                    nicResponse.setBroadcastUri(userVm.getBroadcastUri().toString());
                }
                if (userVm.getIsolationUri() != null) {
                    nicResponse.setIsolationUri(userVm.getIsolationUri().toString());
                }
                if (userVm.getTrafficType() != null) {
                    nicResponse.setTrafficType(userVm.getTrafficType().toString());
                }
                if (userVm.getGuestType() != null) {
                    nicResponse.setType(userVm.getGuestType().toString());
                }
                nicResponse.setIsDefault(userVm.isDefaultNic());
                nicResponse.setObjectName("nic");
                userVmResponse.addNic(nicResponse);
            }
        }

        // update tag information
        long tag_id = userVm.getTagId();
        if (tag_id > 0) {
            ResourceTagResponse tag = new ResourceTagResponse();
            tag.setKey(userVm.getTagKey());
            tag.setValue(userVm.getTagValue());
            if (userVm.getTagResourceType() != null) {
                tag.setResourceType(userVm.getTagResourceType().toString());
            }
            tag.setId(userVm.getTagResourceUuid()); // tag resource uuid
            tag.setCustomer(userVm.getTagCustomer());
            // TODO: assuming tagAccountId and tagDomainId are the same as VM
            // accountId and domainId
            tag.setDomainId(userVm.getTagDomainId());
            if (userVm.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
                tag.setProjectId(userVm.getProjectId());
                tag.setProjectName(userVm.getProjectName());
            } else {
                tag.setAccountName(userVm.getAccountName());
            }
            tag.setDomainId(userVm.getDomainId()); // TODO: pending tag resource
                                                // response uuid change
            tag.setDomainName(userVm.getDomainName());

            tag.setObjectName("tag");
            userVmResponse.addTag(tag);
        }
        userVmResponse.setObjectName(objectName);

        return userVmResponse;
       }

    public UserVmResponse setUserVmResponse(UserVmResponse userVmData, UserVmJoinVO uvo) {
        Long securityGroupId = uvo.getSecurityGroupId();
        if (securityGroupId != null && securityGroupId.longValue() != 0) {
            SecurityGroupResponse resp = new SecurityGroupResponse();
            resp.setId(uvo.getSecurityGroupUuid());
            resp.setName(uvo.getSecurityGroupName());
            resp.setDescription(uvo.getSecurityGroupDescription());
            resp.setObjectName("securitygroup");
            if (uvo.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
                resp.setProjectId(uvo.getProjectUuid());
                resp.setProjectName(uvo.getProjectName());
            } else {
                resp.setAccountName(uvo.getAccountName());
            }
            userVmData.addSecurityGroup(resp);
        }

        long nic_id = uvo.getNicId();
        if (nic_id > 0) {
            NicResponse nicResponse = new NicResponse();
            nicResponse.setId(uvo.getNicUuid());
            nicResponse.setIpaddress(uvo.getIpAddress());
            nicResponse.setGateway(uvo.getGateway());
            nicResponse.setNetmask(uvo.getNetmask());
            nicResponse.setNetworkid(uvo.getNetworkUuid());
            nicResponse.setMacAddress(uvo.getMacAddress());
            if (uvo.getBroadcastUri() != null) {
                nicResponse.setBroadcastUri(uvo.getBroadcastUri().toString());
            }
            if (uvo.getIsolationUri() != null) {
                nicResponse.setIsolationUri(uvo.getIsolationUri().toString());
            }
            if (uvo.getTrafficType() != null) {
                nicResponse.setTrafficType(uvo.getTrafficType().toString());
            }
            if (uvo.getGuestType() != null) {
                nicResponse.setType(uvo.getGuestType().toString());
            }
            nicResponse.setIsDefault(uvo.isDefaultNic());
            nicResponse.setObjectName("nic");
            userVmData.addNic(nicResponse);
        }

        long tag_id = uvo.getTagId();
        if (tag_id > 0) {
            ResourceTagResponse tag = new ResourceTagResponse();
            tag.setKey(uvo.getTagKey());
            tag.setValue(uvo.getTagValue());
            if (uvo.getTagResourceType() != null) {
                tag.setResourceType(uvo.getTagResourceType().toString());
            }
            tag.setId(uvo.getTagResourceUuid()); // tag resource uuid
            tag.setCustomer(uvo.getTagCustomer());
            // TODO: assuming tagAccountId and tagDomainId are the same as VM
            // accountId and domainId
            tag.setDomainId(uvo.getTagDomainId());
            if (uvo.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
                tag.setProjectId(uvo.getProjectId());
                tag.setProjectName(uvo.getProjectName());
            } else {
                tag.setAccountName(uvo.getAccountName());
            }
            tag.setDomainId(uvo.getDomainId()); // TODO: pending tag resource
                                                // response uuid change
            tag.setDomainName(uvo.getDomainName());

            tag.setObjectName("tag");
            userVmData.addTag(tag);
        }
        return userVmData;
    }


    @Override
    public List<UserVmJoinVO> searchByIds(Long... vmIds) {
        SearchCriteria<UserVmJoinVO> sc = VmDetailSearch.create();
        sc.setParameters("idIN", vmIds);
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<UserVmJoinVO> newUserVmView(UserVm... userVms) {

        int curr_index = 0;

        Hashtable<Long, UserVm> userVmDataHash = new Hashtable<Long, UserVm>();
        for (UserVm vm : userVms){
            if ( !userVmDataHash.containsKey(vm.getId())){
                userVmDataHash.put(vm.getId(), vm);
            }
        }

        List<UserVmJoinVO> uvList = new ArrayList<UserVmJoinVO>();
        List<Long> userVmIdList = new ArrayList(userVmDataHash.keySet());
         if (userVmIdList.size() > VM_DETAILS_BATCH_SIZE) {
            while ((curr_index + VM_DETAILS_BATCH_SIZE) <= userVmIdList.size()) {
                // set current ids
                Long[] vmIds = new Long[VM_DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + VM_DETAILS_BATCH_SIZE; j++, k++) {
                    vmIds[k] = userVmIdList.get(j);
                }
                SearchCriteria<UserVmJoinVO> sc = VmDetailSearch.create();
                sc.setParameters("idIN", vmIds);
                List<UserVmJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    for (UserVmJoinVO uvm : vms) {
                        uvList.add(uvm);
                    }
                }
                curr_index += VM_DETAILS_BATCH_SIZE;
            }
        }

        if (curr_index < userVmIdList.size()) {
            int batch_size = (userVmIdList.size() - curr_index);
            // set the ids value
            Long[] vmIds = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                vmIds[k] = userVmIdList.get(j);
            }
            SearchCriteria<UserVmJoinVO> sc = VmDetailSearch.create();
            sc.setParameters("idIN", vmIds);
            List<UserVmJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                for (UserVmJoinVO uvm : vms) {
                    UserVm vm = userVmDataHash.get(uvm.getId());
                    assert vm != null : "We should not find details of vm not in the passed UserVm list";
                    uvList.add(uvm);
                }
            }
        }
        return uvList;

    }

}
