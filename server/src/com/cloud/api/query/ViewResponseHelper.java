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
package com.cloud.api.query;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AffinityGroupJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.user.Account;

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;

/**
 * Helper class to generate response from DB view VO objects.
 *
 */
public class ViewResponseHelper {

    public static final Logger s_logger = Logger.getLogger(ViewResponseHelper.class);

    public static List<UserResponse> createUserResponse(UserAccountJoinVO... users) {
        List<UserResponse> respList = new ArrayList<UserResponse>();
        for (UserAccountJoinVO vt : users){
            respList.add(ApiDBUtils.newUserResponse(vt));
        }
        return respList;
    }

    public static List<EventResponse> createEventResponse(EventJoinVO... events) {
        List<EventResponse> respList = new ArrayList<EventResponse>();
        for (EventJoinVO vt : events){
            respList.add(ApiDBUtils.newEventResponse(vt));
        }
        return respList;
    }

    public static List<ResourceTagResponse> createResourceTagResponse(boolean keyValueOnly, ResourceTagJoinVO... tags) {
        List<ResourceTagResponse> respList = new ArrayList<ResourceTagResponse>();
        for (ResourceTagJoinVO vt : tags){
            respList.add(ApiDBUtils.newResourceTagResponse(vt, keyValueOnly));
        }
        return respList;
    }

    public static List<InstanceGroupResponse> createInstanceGroupResponse(InstanceGroupJoinVO... groups) {
        List<InstanceGroupResponse> respList = new ArrayList<InstanceGroupResponse>();
        for (InstanceGroupJoinVO vt : groups){
            respList.add(ApiDBUtils.newInstanceGroupResponse(vt));
        }
        return respList;
    }


    public static List<UserVmResponse> createUserVmResponse(String objectName, UserVmJoinVO... userVms) {
        return createUserVmResponse(objectName, EnumSet.of(VMDetails.all), userVms);
    }

    public static List<UserVmResponse> createUserVmResponse(String objectName, EnumSet<VMDetails> details, UserVmJoinVO... userVms) {
        Account caller = CallContext.current().getCallingAccount();

        Hashtable<Long, UserVmResponse> vmDataList = new Hashtable<Long, UserVmResponse>();
        // Initialise the vmdatalist with the input data


        for (UserVmJoinVO userVm : userVms) {
            UserVmResponse userVmData = vmDataList.get(userVm.getId());
            if ( userVmData == null ){
                // first time encountering this vm
                userVmData = ApiDBUtils.newUserVmResponse(objectName, userVm, details, caller);
            } else{
                // update nics, securitygroups, tags, affinitygroups for 1 to many mapping fields
                userVmData = ApiDBUtils.fillVmDetails(userVmData, userVm);
            }
            vmDataList.put(userVm.getId(), userVmData);
        }
        return new ArrayList<UserVmResponse>(vmDataList.values());
    }

    public static List<DomainRouterResponse> createDomainRouterResponse(DomainRouterJoinVO... routers) {
        Account caller = CallContext.current().getCallingAccount();
        Hashtable<Long, DomainRouterResponse> vrDataList = new Hashtable<Long, DomainRouterResponse>();
        // Initialise the vrdatalist with the input data
        for (DomainRouterJoinVO vr : routers) {
            DomainRouterResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this vm
                vrData = ApiDBUtils.newDomainRouterResponse(vr, caller);
            }
            else{
                // update nics for 1 to many mapping fields
                vrData = ApiDBUtils.fillRouterDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<DomainRouterResponse>(vrDataList.values());
    }


    public static List<SecurityGroupResponse> createSecurityGroupResponses(List<SecurityGroupJoinVO> securityGroups) {
        Account caller = CallContext.current().getCallingAccount();
        Hashtable<Long, SecurityGroupResponse> vrDataList = new Hashtable<Long, SecurityGroupResponse>();
        // Initialise the vrdatalist with the input data
        for (SecurityGroupJoinVO vr : securityGroups) {
            SecurityGroupResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ) {
                // first time encountering this sg
                vrData = ApiDBUtils.newSecurityGroupResponse(vr, caller);

            } else {
                // update rules for 1 to many mapping fields
                vrData = ApiDBUtils.fillSecurityGroupDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<SecurityGroupResponse>(vrDataList.values());
    }


    public static List<ProjectResponse> createProjectResponse(ProjectJoinVO... projects) {
        Hashtable<Long, ProjectResponse> prjDataList = new Hashtable<Long, ProjectResponse>();
        // Initialise the prjdatalist with the input data
        for (ProjectJoinVO p : projects) {
            ProjectResponse pData = prjDataList.get(p.getId());
            if ( pData == null ){
                // first time encountering this vm
                pData = ApiDBUtils.newProjectResponse(p);
            }
            else{
                // update those  1 to many mapping fields
                pData = ApiDBUtils.fillProjectDetails(pData, p);
            }
            prjDataList.put(p.getId(), pData);
        }
        return new ArrayList<ProjectResponse>(prjDataList.values());
    }


    public static List<ProjectAccountResponse> createProjectAccountResponse(ProjectAccountJoinVO... projectAccounts) {
        List<ProjectAccountResponse> responseList = new ArrayList<ProjectAccountResponse>();
        for (ProjectAccountJoinVO proj : projectAccounts){
            ProjectAccountResponse resp = ApiDBUtils.newProjectAccountResponse(proj);
            // update user list
            List<UserAccountJoinVO> users = ApiDBUtils.findUserViewByAccountId(proj.getAccountId());
            resp.setUsers(ViewResponseHelper.createUserResponse(users.toArray(new UserAccountJoinVO[users.size()])));
            responseList.add(resp);
        }
        return responseList;
    }

    public static List<ProjectInvitationResponse> createProjectInvitationResponse(ProjectInvitationJoinVO... invites) {
        List<ProjectInvitationResponse> respList = new ArrayList<ProjectInvitationResponse>();
        for (ProjectInvitationJoinVO v : invites){
            respList.add(ApiDBUtils.newProjectInvitationResponse(v));
        }
        return respList;
    }

    public static List<HostResponse> createHostResponse(EnumSet<HostDetails> details, HostJoinVO... hosts) {
        Hashtable<Long, HostResponse> vrDataList = new Hashtable<Long, HostResponse>();
        // Initialise the vrdatalist with the input data
        for (HostJoinVO vr : hosts) {
            HostResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this vm
                vrData = ApiDBUtils.newHostResponse(vr, details);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillHostDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<HostResponse>(vrDataList.values());
    }

    public static List<HostForMigrationResponse> createHostForMigrationResponse(EnumSet<HostDetails> details,
            HostJoinVO... hosts) {
        Hashtable<Long, HostForMigrationResponse> vrDataList = new Hashtable<Long, HostForMigrationResponse>();
        // Initialise the vrdatalist with the input data
        for (HostJoinVO vr : hosts) {
            HostForMigrationResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ) {
                // first time encountering this vm
                vrData = ApiDBUtils.newHostForMigrationResponse(vr, details);
            } else {
                // update tags
                vrData = ApiDBUtils.fillHostForMigrationDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<HostForMigrationResponse>(vrDataList.values());
    }

    public static List<VolumeResponse> createVolumeResponse(VolumeJoinVO... volumes) {
        Hashtable<Long, VolumeResponse> vrDataList = new Hashtable<Long, VolumeResponse>();
        for (VolumeJoinVO vr : volumes) {
            VolumeResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this volume
                vrData = ApiDBUtils.newVolumeResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillVolumeDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<VolumeResponse>(vrDataList.values());
    }

    public static List<StoragePoolResponse> createStoragePoolResponse(StoragePoolJoinVO... pools) {
        Hashtable<Long, StoragePoolResponse> vrDataList = new Hashtable<Long, StoragePoolResponse>();
        // Initialise the vrdatalist with the input data
        for (StoragePoolJoinVO vr : pools) {
            StoragePoolResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this vm
                vrData = ApiDBUtils.newStoragePoolResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillStoragePoolDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<StoragePoolResponse>(vrDataList.values());
    }


    public static List<ImageStoreResponse> createImageStoreResponse(ImageStoreJoinVO... stores) {
        Hashtable<Long, ImageStoreResponse> vrDataList = new Hashtable<Long, ImageStoreResponse>();
        // Initialise the vrdatalist with the input data
        for (ImageStoreJoinVO vr : stores) {
            ImageStoreResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this vm
                vrData = ApiDBUtils.newImageStoreResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillImageStoreDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<ImageStoreResponse>(vrDataList.values());
    }

    public static List<StoragePoolResponse> createStoragePoolForMigrationResponse(StoragePoolJoinVO... pools) {
        Hashtable<Long, StoragePoolResponse> vrDataList = new Hashtable<Long, StoragePoolResponse>();
        // Initialise the vrdatalist with the input data
        for (StoragePoolJoinVO vr : pools) {
            StoragePoolResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ) {
                // first time encountering this vm
                vrData = ApiDBUtils.newStoragePoolForMigrationResponse(vr);
            } else {
                // update tags
                vrData = ApiDBUtils.fillStoragePoolForMigrationDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<StoragePoolResponse>(vrDataList.values());
    }


    public static List<AccountResponse> createAccountResponse(AccountJoinVO... accounts) {
        List<AccountResponse> respList = new ArrayList<AccountResponse>();
        for (AccountJoinVO vt : accounts){
            respList.add(ApiDBUtils.newAccountResponse(vt));
        }
        return respList;
    }

    public static List<AsyncJobResponse> createAsyncJobResponse(AsyncJobJoinVO... jobs) {
        List<AsyncJobResponse> respList = new ArrayList<AsyncJobResponse>();
        for (AsyncJobJoinVO vt : jobs){
            respList.add(ApiDBUtils.newAsyncJobResponse(vt));
        }
        return respList;
    }

    public static List<DiskOfferingResponse> createDiskOfferingResponse(DiskOfferingJoinVO... offerings) {
        List<DiskOfferingResponse> respList = new ArrayList<DiskOfferingResponse>();
        for (DiskOfferingJoinVO vt : offerings){
            respList.add(ApiDBUtils.newDiskOfferingResponse(vt));
        }
        return respList;
    }

    public static List<ServiceOfferingResponse> createServiceOfferingResponse(ServiceOfferingJoinVO... offerings) {
        List<ServiceOfferingResponse> respList = new ArrayList<ServiceOfferingResponse>();
        for (ServiceOfferingJoinVO vt : offerings){
            respList.add(ApiDBUtils.newServiceOfferingResponse(vt));
        }
        return respList;
    }

    public static List<ZoneResponse> createDataCenterResponse(Boolean showCapacities, DataCenterJoinVO... dcs) {
        List<ZoneResponse> respList = new ArrayList<ZoneResponse>();
        for (DataCenterJoinVO vt : dcs){
            respList.add(ApiDBUtils.newDataCenterResponse(vt, showCapacities));
        }
        return respList;
    }

    public static List<TemplateResponse> createTemplateResponse(TemplateJoinVO... templates) {
        Hashtable<String, TemplateResponse> vrDataList = new Hashtable<String, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getTempZonePair());
            if ( vrData == null ){
                // first time encountering this volume
                vrData = ApiDBUtils.newTemplateResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(vrData, vr);
            }
            vrDataList.put(vr.getTempZonePair(), vrData);
        }
        return new ArrayList<TemplateResponse>(vrDataList.values());
    }

    public static List<TemplateResponse> createTemplateUpdateResponse(TemplateJoinVO... templates) {
        Hashtable<Long, TemplateResponse> vrDataList = new Hashtable<Long, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this volume
                vrData = ApiDBUtils.newTemplateUpdateResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<TemplateResponse>(vrDataList.values());
    }

    public static List<TemplateResponse> createIsoResponse(TemplateJoinVO... templates) {
        Hashtable<Long, TemplateResponse> vrDataList = new Hashtable<Long, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getId());
            if ( vrData == null ){
                // first time encountering this volume
                vrData = ApiDBUtils.newIsoResponse(vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<TemplateResponse>(vrDataList.values());
    }

    public static List<AffinityGroupResponse> createAffinityGroupResponses(List<AffinityGroupJoinVO> groups) {
        Hashtable<Long, AffinityGroupResponse> vrDataList = new Hashtable<Long, AffinityGroupResponse>();
        for (AffinityGroupJoinVO vr : groups) {
            AffinityGroupResponse vrData = vrDataList.get(vr.getId());
            if (vrData == null) {
                // first time encountering this AffinityGroup
                vrData = ApiDBUtils.newAffinityGroupResponse(vr);
            } else {
                // update vms
                vrData = ApiDBUtils.fillAffinityGroupDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<AffinityGroupResponse>(vrDataList.values());
    }
}
