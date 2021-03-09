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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AffinityGroupJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.HostTagVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.domain.Domain;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.VolumeStats;
import com.cloud.user.Account;

/**
 * Helper class to generate response from DB view VO objects.
 *
 */
public class ViewResponseHelper {

    public static final Logger s_logger = Logger.getLogger(ViewResponseHelper.class);

    public static List<UserResponse> createUserResponse(UserAccountJoinVO... users) {
        return createUserResponse(null, users);
    }

    public static List<UserResponse> createUserResponse(Long domainId, UserAccountJoinVO... users) {
        List<UserResponse> respList = new ArrayList<UserResponse>();
        for (UserAccountJoinVO vt : users) {
            respList.add(ApiDBUtils.newUserResponse(vt, domainId));
        }
        return respList;
    }

    public static List<EventResponse> createEventResponse(EventJoinVO... events) {
        List<EventResponse> respList = new ArrayList<EventResponse>();
        for (EventJoinVO vt : events) {
            respList.add(ApiDBUtils.newEventResponse(vt));
        }
        return respList;
    }

    public static List<ResourceTagResponse> createResourceTagResponse(boolean keyValueOnly, ResourceTagJoinVO... tags) {
        List<ResourceTagResponse> respList = new ArrayList<ResourceTagResponse>();
        for (ResourceTagJoinVO vt : tags) {
            respList.add(ApiDBUtils.newResourceTagResponse(vt, keyValueOnly));
        }
        return respList;
    }

    public static List<InstanceGroupResponse> createInstanceGroupResponse(InstanceGroupJoinVO... groups) {
        List<InstanceGroupResponse> respList = new ArrayList<InstanceGroupResponse>();
        for (InstanceGroupJoinVO vt : groups) {
            respList.add(ApiDBUtils.newInstanceGroupResponse(vt));
        }
        return respList;
    }


    public static List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, UserVmJoinVO... userVms) {
        return createUserVmResponse(view, objectName, EnumSet.of(VMDetails.all), userVms);
    }

    public static List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, EnumSet<VMDetails> details, UserVmJoinVO... userVms) {
        Account caller = CallContext.current().getCallingAccount();

        Hashtable<Long, UserVmResponse> vmDataList = new Hashtable<Long, UserVmResponse>();
        // Initialise the vmdatalist with the input data

        for (UserVmJoinVO userVm : userVms) {
            UserVmResponse userVmData = vmDataList.get(userVm.getId());
            if (userVmData == null) {
                // first time encountering this vm
                userVmData = ApiDBUtils.newUserVmResponse(view, objectName, userVm, details, caller);
            } else{
                // update nics, securitygroups, tags, affinitygroups for 1 to many mapping fields
                userVmData = ApiDBUtils.fillVmDetails(view, userVmData, userVm);
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
            if (vrData == null) {
                // first time encountering this vm
                vrData = ApiDBUtils.newDomainRouterResponse(vr, caller);
            } else {
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
            if (vrData == null) {
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

    public static List<ProjectResponse> createProjectResponse(EnumSet<DomainDetails> details, ProjectJoinVO... projects) {
        Hashtable<Long, ProjectResponse> prjDataList = new Hashtable<Long, ProjectResponse>();
        // Initialise the prjdatalist with the input data
        for (ProjectJoinVO p : projects) {
            ProjectResponse pData = prjDataList.get(p.getId());
            if (pData == null) {
                // first time encountering this vm
                pData = ApiDBUtils.newProjectResponse(details, p);
                prjDataList.put(p.getId(), pData);
            }
        }
        return new ArrayList<ProjectResponse>(prjDataList.values());
    }

    public static List<ProjectAccountResponse> createProjectAccountResponse(ProjectAccountJoinVO... projectAccounts) {
        List<ProjectAccountResponse> responseList = new ArrayList<ProjectAccountResponse>();
        for (ProjectAccountJoinVO proj : projectAccounts) {
            ProjectAccountResponse resp = ApiDBUtils.newProjectAccountResponse(proj);
            // update user list
            Account caller = CallContext.current().getCallingAccount();
            if (ApiDBUtils.isAdmin(caller)) {
                List<UserAccountJoinVO> users = null;
                if (proj.getUserUuid() != null) {
                    users = Collections.singletonList(ApiDBUtils.findUserAccountById(proj.getUserId()));
                } else {
                    users = ApiDBUtils.findUserViewByAccountId(proj.getAccountId());
                }
                resp.setUsers(ViewResponseHelper.createUserResponse(users.toArray(new UserAccountJoinVO[users.size()])));
            }
            responseList.add(resp);
        }
        return responseList;
    }

    public static List<ProjectInvitationResponse> createProjectInvitationResponse(ProjectInvitationJoinVO... invites) {
        List<ProjectInvitationResponse> respList = new ArrayList<ProjectInvitationResponse>();
        for (ProjectInvitationJoinVO v : invites) {
            respList.add(ApiDBUtils.newProjectInvitationResponse(v));
        }
        return respList;
    }

    public static List<HostResponse> createHostResponse(EnumSet<HostDetails> details, HostJoinVO... hosts) {
        Hashtable<Long, HostResponse> vrDataList = new Hashtable<Long, HostResponse>();
        // Initialise the vrdatalist with the input data
        for (HostJoinVO vr : hosts) {
            HostResponse vrData = ApiDBUtils.newHostResponse(vr, details);
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<HostResponse>(vrDataList.values());
    }

    public static List<HostForMigrationResponse> createHostForMigrationResponse(EnumSet<HostDetails> details, HostJoinVO... hosts) {
        Hashtable<Long, HostForMigrationResponse> vrDataList = new Hashtable<Long, HostForMigrationResponse>();
        // Initialise the vrdatalist with the input data
        for (HostJoinVO vr : hosts) {
            HostForMigrationResponse vrData = ApiDBUtils.newHostForMigrationResponse(vr, details);
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<HostForMigrationResponse>(vrDataList.values());
    }

    public static List<VolumeResponse> createVolumeResponse(ResponseView view, VolumeJoinVO... volumes) {
        Hashtable<Long, VolumeResponse> vrDataList = new Hashtable<Long, VolumeResponse>();
        DecimalFormat df = new DecimalFormat("0.0%");
        for (VolumeJoinVO vr : volumes) {
            VolumeResponse vrData = vrDataList.get(vr.getId());
            if (vrData == null) {
                // first time encountering this volume
                vrData = ApiDBUtils.newVolumeResponse(view, vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillVolumeDetails(view, vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);

            VolumeStats vs = null;
            if (vr.getFormat() == ImageFormat.VHD || vr.getFormat() == ImageFormat.QCOW2 || vr.getFormat() == ImageFormat.RAW) {
                if (vrData.getPath() != null) {
                    vs = ApiDBUtils.getVolumeStatistics(vrData.getPath());
                }
            } else if (vr.getFormat() == ImageFormat.OVA) {
                if (vrData.getChainInfo() != null) {
                    vs = ApiDBUtils.getVolumeStatistics(vrData.getChainInfo());
                }
            }

            if (vs != null) {
                long vsz = vs.getVirtualSize();
                long psz = vs.getPhysicalSize() ;
                double util = (double)psz/vsz;
                vrData.setUtilization(df.format(util));

                if (view == ResponseView.Full) {
                    vrData.setVirtualsize(vsz);
                    vrData.setPhysicalsize(psz);
                }
            }
        }
        return new ArrayList<VolumeResponse>(vrDataList.values());
    }

    public static List<StoragePoolResponse> createStoragePoolResponse(StoragePoolJoinVO... pools) {
        Hashtable<Long, StoragePoolResponse> vrDataList = new Hashtable<Long, StoragePoolResponse>();
        // Initialise the vrdatalist with the input data
        for (StoragePoolJoinVO vr : pools) {
            StoragePoolResponse vrData = vrDataList.get(vr.getId());
            if (vrData == null) {
                // first time encountering this vm
                vrData = ApiDBUtils.newStoragePoolResponse(vr);
            } else {
                // update tags
                vrData = ApiDBUtils.fillStoragePoolDetails(vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<StoragePoolResponse>(vrDataList.values());
    }

    public static List<StorageTagResponse> createStorageTagResponse(StoragePoolTagVO... storageTags) {
        ArrayList<StorageTagResponse> list = new ArrayList<StorageTagResponse>();

        for (StoragePoolTagVO vr : storageTags) {
            list.add(ApiDBUtils.newStorageTagResponse(vr));
        }

        return list;
    }

    public static List<HostTagResponse> createHostTagResponse(HostTagVO... hostTags) {
        ArrayList<HostTagResponse> list = new ArrayList<HostTagResponse>();

        for (HostTagVO vr : hostTags) {
            list.add(ApiDBUtils.newHostTagResponse(vr));
        }

        return list;
    }

    public static List<ImageStoreResponse> createImageStoreResponse(ImageStoreJoinVO... stores) {
        Hashtable<Long, ImageStoreResponse> vrDataList = new Hashtable<Long, ImageStoreResponse>();
        // Initialise the vrdatalist with the input data
        for (ImageStoreJoinVO vr : stores) {
            ImageStoreResponse vrData = vrDataList.get(vr.getId());
            if (vrData == null) {
                // first time encountering this vm
                vrData = ApiDBUtils.newImageStoreResponse(vr);
            } else {
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
            if (vrData == null) {
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

    public static List<DomainResponse> createDomainResponse(ResponseView view, EnumSet<DomainDetails> details, List<DomainJoinVO> domains) {
        List<DomainResponse> respList = new ArrayList<DomainResponse>();
        //-- Coping the list to keep original order
        List<DomainJoinVO> domainsCopy = new ArrayList<>(domains);
        Collections.sort(domainsCopy, DomainJoinVO.domainIdComparator);
        for (DomainJoinVO domainJoinVO : domains){
            //-- Set parent information
            DomainJoinVO parentDomainJoinVO = searchParentDomainUsingBinary(domainsCopy, domainJoinVO);
            if(parentDomainJoinVO == null && domainJoinVO.getParent() != null) {
                //-- fetch the parent from the database
                parentDomainJoinVO = ApiDBUtils.findDomainJoinVOById(domainJoinVO.getParent());
                if(parentDomainJoinVO != null) {
                    //-- Add parent domain to the domain copy for future use
                    domainsCopy.add(parentDomainJoinVO);
                    Collections.sort(domainsCopy, DomainJoinVO.domainIdComparator);
                }
            }
            if(parentDomainJoinVO != null) {
                domainJoinVO.setParentName(parentDomainJoinVO.getName());
                domainJoinVO.setParentUuid(parentDomainJoinVO.getUuid());
            }
            //-- Set correct resource limits
            if(domainJoinVO.getParent() != null && domainJoinVO.getParent() != Domain.ROOT_DOMAIN) {
                Map<Resource.ResourceType, Long> resourceLimitMap = new HashMap<>();
                copyResourceLimitsIntoMap(resourceLimitMap, domainJoinVO);
                //-- Fetching the parent domain resource limit if absent in current domain
                setParentResourceLimitIfNeeded(resourceLimitMap, domainJoinVO, domainsCopy);
                //-- copy the final correct resource limit
                copyResourceLimitsFromMap(resourceLimitMap, domainJoinVO);
            }
            respList.add(ApiDBUtils.newDomainResponse(view, details, domainJoinVO));
        }
        return respList;
    }

    private static DomainJoinVO searchParentDomainUsingBinary(List<DomainJoinVO> domainsCopy, DomainJoinVO domainJoinVO){
        Long parentId = domainJoinVO.getParent() == null ? 0 : domainJoinVO.getParent();
        int totalDomains = domainsCopy.size();
        int left = 0;
        int right = totalDomains -1;
        while(left <= right){
            int middle = (left + right) /2;
            DomainJoinVO middleObject = domainsCopy.get(middle);
            if(middleObject.getId() == parentId){
                return middleObject;
            }
            if(middleObject.getId() > parentId){
                right = middle - 1 ;
            }
            else{
                left = middle + 1;
            }
        }
        return null;
    }

    private static void copyResourceLimitsIntoMap(Map<Resource.ResourceType, Long> resourceLimitMap, DomainJoinVO domainJoinVO){
        resourceLimitMap.put(Resource.ResourceType.user_vm, domainJoinVO.getVmLimit());
        resourceLimitMap.put(Resource.ResourceType.public_ip, domainJoinVO.getIpLimit());
        resourceLimitMap.put(Resource.ResourceType.volume, domainJoinVO.getVolumeLimit());
        resourceLimitMap.put(Resource.ResourceType.snapshot, domainJoinVO.getSnapshotLimit());
        resourceLimitMap.put(Resource.ResourceType.template, domainJoinVO.getTemplateLimit());
        resourceLimitMap.put(Resource.ResourceType.network, domainJoinVO.getNetworkLimit());
        resourceLimitMap.put(Resource.ResourceType.vpc, domainJoinVO.getVpcLimit());
        resourceLimitMap.put(Resource.ResourceType.cpu, domainJoinVO.getCpuLimit());
        resourceLimitMap.put(Resource.ResourceType.memory, domainJoinVO.getMemoryLimit());
        resourceLimitMap.put(Resource.ResourceType.primary_storage, domainJoinVO.getPrimaryStorageLimit());
        resourceLimitMap.put(Resource.ResourceType.secondary_storage, domainJoinVO.getSecondaryStorageLimit());
        resourceLimitMap.put(Resource.ResourceType.project, domainJoinVO.getProjectLimit());
    }

    private static void copyResourceLimitsFromMap(Map<Resource.ResourceType, Long> resourceLimitMap, DomainJoinVO domainJoinVO){
        domainJoinVO.setVmLimit(resourceLimitMap.get(Resource.ResourceType.user_vm));
        domainJoinVO.setIpLimit(resourceLimitMap.get(Resource.ResourceType.public_ip));
        domainJoinVO.setVolumeLimit(resourceLimitMap.get(Resource.ResourceType.volume));
        domainJoinVO.setSnapshotLimit(resourceLimitMap.get(Resource.ResourceType.snapshot));
        domainJoinVO.setTemplateLimit(resourceLimitMap.get(Resource.ResourceType.template));
        domainJoinVO.setNetworkLimit(resourceLimitMap.get(Resource.ResourceType.network));
        domainJoinVO.setVpcLimit(resourceLimitMap.get(Resource.ResourceType.vpc));
        domainJoinVO.setCpuLimit(resourceLimitMap.get(Resource.ResourceType.cpu));
        domainJoinVO.setMemoryLimit(resourceLimitMap.get(Resource.ResourceType.memory));
        domainJoinVO.setPrimaryStorageLimit(resourceLimitMap.get(Resource.ResourceType.primary_storage));
        domainJoinVO.setSecondaryStorageLimit(resourceLimitMap.get(Resource.ResourceType.secondary_storage));
        domainJoinVO.setProjectLimit(resourceLimitMap.get(Resource.ResourceType.project));
    }

    private static void setParentResourceLimitIfNeeded(Map<Resource.ResourceType, Long> resourceLimitMap, DomainJoinVO domainJoinVO, List<DomainJoinVO> domainsCopy) {
        DomainJoinVO parentDomainJoinVO = searchParentDomainUsingBinary(domainsCopy, domainJoinVO);

        if(parentDomainJoinVO != null) {
            Long vmLimit = resourceLimitMap.get(Resource.ResourceType.user_vm);
            Long ipLimit = resourceLimitMap.get(Resource.ResourceType.public_ip);
            Long volumeLimit = resourceLimitMap.get(Resource.ResourceType.volume);
            Long snapshotLimit = resourceLimitMap.get(Resource.ResourceType.snapshot);
            Long templateLimit = resourceLimitMap.get(Resource.ResourceType.template);
            Long networkLimit = resourceLimitMap.get(Resource.ResourceType.network);
            Long vpcLimit = resourceLimitMap.get(Resource.ResourceType.vpc);
            Long cpuLimit = resourceLimitMap.get(Resource.ResourceType.cpu);
            Long memoryLimit = resourceLimitMap.get(Resource.ResourceType.memory);
            Long primaryStorageLimit = resourceLimitMap.get(Resource.ResourceType.primary_storage);
            Long secondaryStorageLimit = resourceLimitMap.get(Resource.ResourceType.secondary_storage);
            Long projectLimit = resourceLimitMap.get(Resource.ResourceType.project);

            if (vmLimit == null) {
                vmLimit = parentDomainJoinVO.getVmLimit();
                resourceLimitMap.put(Resource.ResourceType.user_vm, vmLimit);
            }
            if (ipLimit == null) {
                ipLimit = parentDomainJoinVO.getIpLimit();
                resourceLimitMap.put(Resource.ResourceType.public_ip, ipLimit);
            }
            if (volumeLimit == null) {
                volumeLimit = parentDomainJoinVO.getVolumeLimit();
                resourceLimitMap.put(Resource.ResourceType.volume, volumeLimit);
            }
            if (snapshotLimit == null) {
                snapshotLimit = parentDomainJoinVO.getSnapshotLimit();
                resourceLimitMap.put(Resource.ResourceType.snapshot, snapshotLimit);
            }
            if (templateLimit == null) {
                templateLimit = parentDomainJoinVO.getTemplateLimit();
                resourceLimitMap.put(Resource.ResourceType.template, templateLimit);
            }
            if (networkLimit == null) {
                networkLimit = parentDomainJoinVO.getNetworkLimit();
                resourceLimitMap.put(Resource.ResourceType.network, networkLimit);
            }
            if (vpcLimit == null) {
                vpcLimit = parentDomainJoinVO.getVpcLimit();
                resourceLimitMap.put(Resource.ResourceType.vpc, vpcLimit);
            }
            if (cpuLimit == null) {
                cpuLimit = parentDomainJoinVO.getCpuLimit();
                resourceLimitMap.put(Resource.ResourceType.cpu, cpuLimit);
            }
            if (memoryLimit == null) {
                memoryLimit = parentDomainJoinVO.getMemoryLimit();
                resourceLimitMap.put(Resource.ResourceType.memory, memoryLimit);
            }
            if (primaryStorageLimit == null) {
                primaryStorageLimit = parentDomainJoinVO.getPrimaryStorageLimit();
                resourceLimitMap.put(Resource.ResourceType.primary_storage, primaryStorageLimit);
            }
            if (secondaryStorageLimit == null) {
                secondaryStorageLimit = parentDomainJoinVO.getSecondaryStorageLimit();
                resourceLimitMap.put(Resource.ResourceType.secondary_storage, secondaryStorageLimit);
            }
            if (projectLimit == null) {
                projectLimit = parentDomainJoinVO.getProjectLimit();
                resourceLimitMap.put(Resource.ResourceType.project, projectLimit);
            }
            //-- try till parent present
            if (parentDomainJoinVO.getParent() != null && parentDomainJoinVO.getParent() != Domain.ROOT_DOMAIN) {
                setParentResourceLimitIfNeeded(resourceLimitMap, parentDomainJoinVO, domainsCopy);
            }
        }
    }

    public static List<AccountResponse> createAccountResponse(ResponseView view, EnumSet<DomainDetails> details, AccountJoinVO... accounts) {
        List<AccountResponse> respList = new ArrayList<AccountResponse>();
        for (AccountJoinVO vt : accounts){
            respList.add(ApiDBUtils.newAccountResponse(view, details, vt));
        }
        return respList;
    }

    public static List<AsyncJobResponse> createAsyncJobResponse(AsyncJobJoinVO... jobs) {
        List<AsyncJobResponse> respList = new ArrayList<AsyncJobResponse>();
        for (AsyncJobJoinVO vt : jobs) {
            respList.add(ApiDBUtils.newAsyncJobResponse(vt));
        }
        return respList;
    }

    public static List<DiskOfferingResponse> createDiskOfferingResponse(DiskOfferingJoinVO... offerings) {
        List<DiskOfferingResponse> respList = new ArrayList<DiskOfferingResponse>();
        for (DiskOfferingJoinVO vt : offerings) {
            respList.add(ApiDBUtils.newDiskOfferingResponse(vt));
        }
        return respList;
    }

    public static List<ServiceOfferingResponse> createServiceOfferingResponse(ServiceOfferingJoinVO... offerings) {
        List<ServiceOfferingResponse> respList = new ArrayList<ServiceOfferingResponse>();
        for (ServiceOfferingJoinVO vt : offerings) {
            respList.add(ApiDBUtils.newServiceOfferingResponse(vt));
        }
        return respList;
    }

    public static List<ZoneResponse> createDataCenterResponse(ResponseView view, Boolean showCapacities, DataCenterJoinVO... dcs) {
        List<ZoneResponse> respList = new ArrayList<ZoneResponse>();
        for (DataCenterJoinVO vt : dcs){
            respList.add(ApiDBUtils.newDataCenterResponse(view, vt, showCapacities));
        }
        return respList;
    }

    public static List<TemplateResponse> createTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateJoinVO... templates) {
        LinkedHashMap<String, TemplateResponse> vrDataList = new LinkedHashMap<String, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getTempZonePair());
            if (vrData == null) {
                // first time encountering this volume
                vrData = ApiDBUtils.newTemplateResponse(detailsView, view, vr);
            }
            else{
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(detailsView, view, vrData, vr);
            }
            vrDataList.put(vr.getTempZonePair(), vrData);
        }
        return new ArrayList<TemplateResponse>(vrDataList.values());
    }

    public static List<TemplateResponse> createTemplateUpdateResponse(ResponseView view, TemplateJoinVO... templates) {
        Hashtable<Long, TemplateResponse> vrDataList = new Hashtable<Long, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getId());
            if (vrData == null) {
                // first time encountering this volume
                vrData = ApiDBUtils.newTemplateUpdateResponse(vr);
            } else {
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(EnumSet.of(DomainDetails.all), view, vrData, vr);
            }
            vrDataList.put(vr.getId(), vrData);
        }
        return new ArrayList<TemplateResponse>(vrDataList.values());
    }

    public static List<TemplateResponse> createIsoResponse(ResponseView view, TemplateJoinVO... templates) {
        Hashtable<String, TemplateResponse> vrDataList = new Hashtable<String, TemplateResponse>();
        for (TemplateJoinVO vr : templates) {
            TemplateResponse vrData = vrDataList.get(vr.getTempZonePair());
            if (vrData == null) {
                // first time encountering this volume
                vrData = ApiDBUtils.newIsoResponse(vr);
            } else {
                // update tags
                vrData = ApiDBUtils.fillTemplateDetails(EnumSet.of(DomainDetails.all), view, vrData, vr);
            }
            vrDataList.put(vr.getTempZonePair(), vrData);
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
