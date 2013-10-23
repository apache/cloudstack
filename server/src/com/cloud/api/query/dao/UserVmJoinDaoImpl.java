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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmStats;


@Component
@Local(value={UserVmJoinDao.class})
public class UserVmJoinDaoImpl extends GenericDaoBase<UserVmJoinVO, Long> implements UserVmJoinDao {
    public static final Logger s_logger = Logger.getLogger(UserVmJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;

    private final SearchBuilder<UserVmJoinVO> VmDetailSearch;
    private final SearchBuilder<UserVmJoinVO> activeVmByIsoSearch;

    protected UserVmJoinDaoImpl() {

        VmDetailSearch = createSearchBuilder();
        VmDetailSearch.and("idIN", VmDetailSearch.entity().getId(), SearchCriteria.Op.IN);
        VmDetailSearch.done();

        this._count = "select count(distinct id) from user_vm_view WHERE ";

        activeVmByIsoSearch = createSearchBuilder();
        activeVmByIsoSearch.and("isoId", activeVmByIsoSearch.entity().getIsoId(), SearchCriteria.Op.EQ);
        activeVmByIsoSearch.and("stateNotIn", activeVmByIsoSearch.entity().getState(), SearchCriteria.Op.NIN);
        activeVmByIsoSearch.done();
    }


    @Override
    public List<UserVmJoinVO> listActiveByIsoId(Long isoId) {
        SearchCriteria<UserVmJoinVO> sc = activeVmByIsoSearch.create();
        sc.setParameters("isoId", isoId);
        State[] states = new State[2];
        states[0] = State.Error;
        states[1] = State.Expunging;
        return listBy(sc);
    }


    @Override
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
        userVmResponse.setDisplayVm(userVm.isDisplayVm());

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
        }
        if (details.contains(VMDetails.all) || details.contains(VMDetails.servoff) || details.contains(VMDetails.stats)) {
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
        if (userVm.getJobId() != null) {
            userVmResponse.setJobId(userVm.getJobUuid());
            userVmResponse.setJobStatus(userVm.getJobStatus());
        }
        //userVmResponse.setForVirtualNetwork(userVm.getForVirtualNetwork());

        userVmResponse.setPublicIpId(userVm.getPublicIpUuid());
        userVmResponse.setPublicIp(userVm.getPublicIpAddress());
        userVmResponse.setKeyPairName(userVm.getKeypairName());

        if (details.contains(VMDetails.all) || details.contains(VMDetails.stats)) {
            // stats calculation
            VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
            if (vmStats != null) {
                userVmResponse.setCpuUsed(new DecimalFormat("#.##").format(vmStats.getCPUUtilization()) + "%");

                userVmResponse.setNetworkKbsRead((long) vmStats.getNetworkReadKBs());

                userVmResponse.setNetworkKbsWrite((long) vmStats.getNetworkWriteKBs());

                if ((userVm.getHypervisorType() != null) 
                        && (userVm.getHypervisorType().equals(HypervisorType.KVM)
                        || userVm.getHypervisorType().equals(HypervisorType.XenServer))) { // support KVM and XenServer only util 2013.06.25
                    userVmResponse.setDiskKbsRead((long) vmStats.getDiskReadKBs());

                    userVmResponse.setDiskKbsWrite((long) vmStats.getDiskWriteKBs());

                    userVmResponse.setDiskIORead((long) vmStats.getDiskReadIOs());

                    userVmResponse.setDiskIOWrite((long) vmStats.getDiskWriteIOs());
                }
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
                nicResponse.setNetworkName(userVm.getNetworkName());
                nicResponse.setMacAddress(userVm.getMacAddress());
                nicResponse.setIp6Address(userVm.getIp6Address());
                nicResponse.setIp6Gateway(userVm.getIp6Gateway());
                nicResponse.setIp6Cidr(userVm.getIp6Cidr());
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
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                userVmResponse.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        if (details.contains(VMDetails.all) || details.contains(VMDetails.affgrp)) {
            Long affinityGroupId = userVm.getAffinityGroupId();
            if (affinityGroupId != null && affinityGroupId.longValue() != 0) {
                AffinityGroupResponse resp = new AffinityGroupResponse();
                resp.setId(userVm.getAffinityGroupUuid());
                resp.setName(userVm.getAffinityGroupName());
                resp.setDescription(userVm.getAffinityGroupDescription());
                resp.setObjectName("affinitygroup");
                resp.setAccountName(userVm.getAccountName());
                userVmResponse.addAffinityGroup(resp);
            }
        }
        
        // set resource details map
        // only hypervisortoolsversion can be returned to the end user       }
        if (userVm.getDetailName() != null && userVm.getDetailName().equalsIgnoreCase(VmDetailConstants.HYPERVISOR_TOOLS_VERSION)){
            Map<String, String> resourceDetails = new HashMap<String, String>();
            resourceDetails.put(userVm.getDetailName(), userVm.getDetailValue());
            userVmResponse.setDetails(resourceDetails);
        }

        userVmResponse.setObjectName(objectName);
        if (userVm.isDynamicallyScalable() == null) {
            userVmResponse.setDynamicallyScalable(false);
        } else {
            userVmResponse.setDynamicallyScalable(userVm.isDynamicallyScalable());
        }

        return userVmResponse;
    }

    @Override
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
            nicResponse.setNetworkName(uvo.getNetworkName());
            nicResponse.setMacAddress(uvo.getMacAddress());
            nicResponse.setIp6Address(uvo.getIp6Address());
            nicResponse.setIp6Gateway(uvo.getIp6Gateway());
            nicResponse.setIp6Cidr(uvo.getIp6Cidr());
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
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                userVmData.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        Long affinityGroupId = uvo.getAffinityGroupId();
        if (affinityGroupId != null && affinityGroupId.longValue() != 0) {
            AffinityGroupResponse resp = new AffinityGroupResponse();
            resp.setId(uvo.getAffinityGroupUuid());
            resp.setName(uvo.getAffinityGroupName());
            resp.setDescription(uvo.getAffinityGroupDescription());
            resp.setObjectName("affinitygroup");
            resp.setAccountName(uvo.getAccountName());
            userVmData.addAffinityGroup(resp);
        }

        return userVmData;
    }


    @Override
    public List<UserVmJoinVO> searchByIds(Long... vmIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if ( batchCfg != null ){
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<UserVmJoinVO> uvList = new ArrayList<UserVmJoinVO>();
        // query details by batches
        int curr_index = 0;
        if ( vmIds.length > DETAILS_BATCH_SIZE ){
            while ( (curr_index + DETAILS_BATCH_SIZE ) <= vmIds.length ) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = vmIds[j];
                }
                SearchCriteria<UserVmJoinVO> sc = VmDetailSearch.create();
                sc.setParameters("idIN", ids);
                List<UserVmJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < vmIds.length) {
            int batch_size = (vmIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = vmIds[j];
            }
            SearchCriteria<UserVmJoinVO> sc = VmDetailSearch.create();
            sc.setParameters("idIN", ids);
            List<UserVmJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

    @Override
    public List<UserVmJoinVO> newUserVmView(UserVm... userVms) {

        Hashtable<Long, UserVm> userVmDataHash = new Hashtable<Long, UserVm>();
        for (UserVm vm : userVms){
            if ( !userVmDataHash.containsKey(vm.getId())){
                userVmDataHash.put(vm.getId(), vm);
            }
        }

        Set<Long> vmIdSet = userVmDataHash.keySet();
        List<UserVmJoinVO> uvms = searchByIds(vmIdSet.toArray(new Long[vmIdSet.size()]));
        // populate transit password field from UserVm
        if ( uvms != null ){
            for (UserVmJoinVO uvm : uvms){
                UserVm v = userVmDataHash.get(uvm.getId());
                uvm.setPassword(v.getPassword());
            }
        }
        return uvms;
    }

}
