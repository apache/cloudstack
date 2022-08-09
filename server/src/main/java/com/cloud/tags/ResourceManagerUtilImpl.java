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
package com.cloud.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.LBHealthCheckPolicyVO;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LBStickinessPolicyVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.projects.ProjectVO;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserVO;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.snapshot.VMSnapshotVO;

public class ResourceManagerUtilImpl implements ResourceManagerUtil {
    public static final Map<ResourceTag.ResourceObjectType, Class<?>> s_typeMap = new HashMap<>();

    static {
        s_typeMap.put(ResourceTag.ResourceObjectType.UserVm, UserVmVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Volume, VolumeVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Template, VMTemplateVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.ISO, VMTemplateVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Snapshot, SnapshotVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Network, NetworkVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.LoadBalancer, LoadBalancerVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.PortForwardingRule, PortForwardingRuleVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.FirewallRule, FirewallRuleVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.SecurityGroup, SecurityGroupVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.SecurityGroupRule, SecurityGroupRuleVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.PublicIpAddress, IPAddressVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Project, ProjectVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Account, AccountVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Vpc, VpcVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Nic, NicVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.NetworkACL, NetworkACLItemVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.StaticRoute, StaticRouteVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.VMSnapshot, VMSnapshotVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.RemoteAccessVpn, RemoteAccessVpnVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Zone, DataCenterVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.ServiceOffering, ServiceOfferingVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Storage, StoragePoolVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.PrivateGateway, RemoteAccessVpnVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.NetworkACLList, NetworkACLVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.VpnGateway, Site2SiteVpnGatewayVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.CustomerGateway, Site2SiteCustomerGatewayVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.VpnConnection, Site2SiteVpnConnectionVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.User, UserVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.DiskOffering, DiskOfferingVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.AutoScaleVmProfile, AutoScaleVmProfileVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.AutoScaleVmGroup, AutoScaleVmGroupVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.LBStickinessPolicy, LBStickinessPolicyVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.LBHealthCheckPolicy, LBHealthCheckPolicyVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.SnapshotPolicy, SnapshotPolicyVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.NetworkOffering, NetworkOfferingVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.VpcOffering, VpcOfferingVO.class);
        s_typeMap.put(ResourceTag.ResourceObjectType.Domain, DomainVO.class);
    }

    @Inject
    EntityManager entityMgr;
    @Inject
    AccountManager accountMgr;
    @Inject
    DomainManager domainMgr;

    @Override
    public long getResourceId(String resourceId, ResourceTag.ResourceObjectType resourceType) {
        Class<?> clazz = s_typeMap.get(resourceType);
        Object entity = entityMgr.findByUuid(clazz, resourceId);
        if (entity != null) {
            return ((InternalIdentity)entity).getId();
        }
        if (!StringUtils.isNumeric(resourceId)) {
            throw new InvalidParameterValueException("Unable to find resource by uuid " + resourceId + " and type " + resourceType);
        }
        entity = entityMgr.findById(clazz, resourceId);
        if (entity != null) {
            return ((InternalIdentity)entity).getId();
        }
        throw new InvalidParameterValueException("Unable to find resource by id " + resourceId + " and type " + resourceType);
    }

    @Override
    public String getUuid(String resourceId, ResourceTag.ResourceObjectType resourceType) {
        if (!StringUtils.isNumeric(resourceId)) {
            return resourceId;
        }

        Class<?> clazz = s_typeMap.get(resourceType);

        Object entity = entityMgr.findById(clazz, resourceId);
        if (entity != null && entity instanceof Identity) {
            return ((Identity)entity).getUuid();
        }

        return resourceId;
    }

    @Override
    public ResourceTag.ResourceObjectType getResourceType(String resourceTypeStr) {

        for (ResourceTag.ResourceObjectType type : ResourceTag.ResourceObjectType.values()) {
            if (type.toString().equalsIgnoreCase(resourceTypeStr)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid resource type: " + resourceTypeStr);
    }

    public void checkResourceAccessible(Long accountId, Long domainId, String exceptionMessage) {
        Account caller = CallContext.current().getCallingAccount();
        if (Objects.equals(domainId, -1))
        {
            throw new CloudRuntimeException("Invalid DomainId: -1");
        }
        if (accountId != null) {
            accountMgr.checkAccess(caller, null, false, accountMgr.getAccount(accountId));
        } else if (domainId != null && !accountMgr.isNormalUser(caller.getId())) {
            //check permissions;
            accountMgr.checkAccess(caller, domainMgr.getDomain(domainId));
        } else {
            throw new PermissionDeniedException(exceptionMessage);
        }
    }
}
