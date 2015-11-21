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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.PartOf;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
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
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.projects.ProjectVO;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.OwnedBy;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.snapshot.VMSnapshotVO;

public class TaggedResourceManagerImpl extends ManagerBase implements TaggedResourceService {
    public static final Logger s_logger = Logger.getLogger(TaggedResourceManagerImpl.class);

    private static final Map<ResourceObjectType, Class<?>> s_typeMap = new HashMap<ResourceObjectType, Class<?>>();
    static {
        s_typeMap.put(ResourceObjectType.UserVm, UserVmVO.class);
        s_typeMap.put(ResourceObjectType.Volume, VolumeVO.class);
        s_typeMap.put(ResourceObjectType.Template, VMTemplateVO.class);
        s_typeMap.put(ResourceObjectType.ISO, VMTemplateVO.class);
        s_typeMap.put(ResourceObjectType.Snapshot, SnapshotVO.class);
        s_typeMap.put(ResourceObjectType.Network, NetworkVO.class);
        s_typeMap.put(ResourceObjectType.LoadBalancer, LoadBalancerVO.class);
        s_typeMap.put(ResourceObjectType.PortForwardingRule, PortForwardingRuleVO.class);
        s_typeMap.put(ResourceObjectType.FirewallRule, FirewallRuleVO.class);
        s_typeMap.put(ResourceObjectType.SecurityGroup, SecurityGroupVO.class);
        s_typeMap.put(ResourceObjectType.SecurityGroupRule, SecurityGroupRuleVO.class);
        s_typeMap.put(ResourceObjectType.PublicIpAddress, IPAddressVO.class);
        s_typeMap.put(ResourceObjectType.Project, ProjectVO.class);
        s_typeMap.put(ResourceObjectType.Vpc, VpcVO.class);
        s_typeMap.put(ResourceObjectType.Nic, NicVO.class);
        s_typeMap.put(ResourceObjectType.NetworkACL, NetworkACLItemVO.class);
        s_typeMap.put(ResourceObjectType.StaticRoute, StaticRouteVO.class);
        s_typeMap.put(ResourceObjectType.VMSnapshot, VMSnapshotVO.class);
        s_typeMap.put(ResourceObjectType.RemoteAccessVpn, RemoteAccessVpnVO.class);
        s_typeMap.put(ResourceObjectType.Zone, DataCenterVO.class);
        s_typeMap.put(ResourceObjectType.ServiceOffering, ServiceOfferingVO.class);
        s_typeMap.put(ResourceObjectType.Storage, StoragePoolVO.class);
        s_typeMap.put(ResourceObjectType.PrivateGateway, RemoteAccessVpnVO.class);
        s_typeMap.put(ResourceObjectType.NetworkACLList, NetworkACLVO.class);
        s_typeMap.put(ResourceObjectType.VpnGateway, Site2SiteVpnGatewayVO.class);
        s_typeMap.put(ResourceObjectType.CustomerGateway, Site2SiteCustomerGatewayVO.class);
        s_typeMap.put(ResourceObjectType.VpnConnection, Site2SiteVpnConnectionVO.class);
        s_typeMap.put(ResourceObjectType.User, UserVO.class);
        s_typeMap.put(ResourceObjectType.DiskOffering, DiskOfferingVO.class);
        s_typeMap.put(ResourceObjectType.AutoScaleVmProfile, AutoScaleVmProfileVO.class);
        s_typeMap.put(ResourceObjectType.AutoScaleVmGroup, AutoScaleVmGroupVO.class);
        s_typeMap.put(ResourceObjectType.LBStickinessPolicy, LBStickinessPolicyVO.class);
        s_typeMap.put(ResourceObjectType.LBHealthCheckPolicy, LBHealthCheckPolicyVO.class);
        s_typeMap.put(ResourceObjectType.SnapshotPolicy, SnapshotPolicyVO.class);

    }

    @Inject
    EntityManager _entityMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    ResourceTagJoinDao _resourceTagJoinDao;
    @Inject
    DomainManager _domainMgr;
    @Inject
    AccountDao _accountDao;


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public long getResourceId(String resourceId, ResourceObjectType resourceType) {
        Class<?> clazz = s_typeMap.get(resourceType);
        Object entity = _entityMgr.findByUuid(clazz, resourceId);
        if (entity != null) {
            return ((InternalIdentity)entity).getId();
        }
        if (!StringUtils.isNumeric(resourceId)) {
            throw new InvalidParameterValueException("Unable to find resource by uuid " + resourceId + " and type " + resourceType);
        }
        entity = _entityMgr.findById(clazz, resourceId);
        if (entity != null) {
            return ((InternalIdentity)entity).getId();
        }
        throw new InvalidParameterValueException("Unable to find resource by id " + resourceId + " and type " + resourceType);
    }

    private Pair<Long, Long> getAccountDomain(long resourceId, ResourceObjectType resourceType) {
        Class<?> clazz = s_typeMap.get(resourceType);

        Object entity = _entityMgr.findById(clazz, resourceId);
        Long accountId = null;
        Long domainId = null;

        // if the resource type is a security group rule, get the accountId and domainId from the security group itself
        if (resourceType == ResourceObjectType.SecurityGroupRule) {
            SecurityGroupRuleVO rule = (SecurityGroupRuleVO)entity;
            Object SecurityGroup = _entityMgr.findById(s_typeMap.get(ResourceObjectType.SecurityGroup), rule.getSecurityGroupId());

            accountId = ((SecurityGroupVO)SecurityGroup).getAccountId();
            domainId = ((SecurityGroupVO)SecurityGroup).getDomainId();
        }

        if (entity instanceof OwnedBy) {
            accountId = ((OwnedBy)entity).getAccountId();
        }

        if (entity instanceof PartOf) {
            domainId = ((PartOf)entity).getDomainId();
        }

        if (accountId == null) {
            accountId = Account.ACCOUNT_ID_SYSTEM;
        }

        if ((domainId == null) || ((accountId != null) && (domainId.longValue() == -1)))
        {
            domainId = _accountDao.getDomainIdForGivenAccountId(accountId);
        }
        return new Pair<Long, Long>(accountId, domainId);
    }

    @Override
    public ResourceObjectType getResourceType(String resourceTypeStr) {

        for (ResourceObjectType type : ResourceTag.ResourceObjectType.values()) {
            if (type.toString().equalsIgnoreCase(resourceTypeStr)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid resource type " + resourceTypeStr);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_CREATE, eventDescription = "creating resource tags")
    public List<ResourceTag> createTags(final List<String> resourceIds, final ResourceObjectType resourceType, final Map<String, String> tags, final String customer) {
        final Account caller = CallContext.current().getCallingAccount();

        final List<ResourceTag> resourceTags = new ArrayList<ResourceTag>(tags.size());

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String key : tags.keySet()) {
                    for (String resourceId : resourceIds) {
                        if (!resourceType.resourceTagsSupport())  {
                            throw new InvalidParameterValueException("The resource type " + resourceType + " doesn't support resource tags");
                        }

                        long id = getResourceId(resourceId, resourceType);
                        String resourceUuid = getUuid(resourceId, resourceType);

                        Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                        Long domainId = accountDomainPair.second();
                        Long accountId = accountDomainPair.first();

                        if ((domainId != null) && (domainId == -1))
                        {
                           throw new CloudRuntimeException("Invalid DomainId : -1");
                        }
                        if (accountId != null) {
                            _accountMgr.checkAccess(caller, null, false, _accountMgr.getAccount(accountId));
                        } else if (domainId != null && !_accountMgr.isNormalUser(caller.getId())) {
                            //check permissions;
                            _accountMgr.checkAccess(caller, _domainMgr.getDomain(domainId));
                        } else {
                            throw new PermissionDeniedException("Account " + caller + " doesn't have permissions to create tags" + " for resource " + key);
                        }

                        String value = tags.get(key);

                        if (value == null || value.isEmpty()) {
                            throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                        }

                        ResourceTagVO resourceTag = new ResourceTagVO(key, value, accountDomainPair.first(), accountDomainPair.second(), id, resourceType, customer, resourceUuid);
                        resourceTag = _resourceTagDao.persist(resourceTag);
                        resourceTags.add(resourceTag);
                    }
                }
            }
        });

        return resourceTags;
    }

    @Override
    public String getUuid(String resourceId, ResourceObjectType resourceType) {
        if (!StringUtils.isNumeric(resourceId)) {
            return resourceId;
        }

        Class<?> clazz = s_typeMap.get(resourceType);

        Object entity = _entityMgr.findById(clazz, resourceId);
        if (entity != null && entity instanceof Identity) {
            return ((Identity)entity).getUuid();
       }

           return resourceId;
       }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_DELETE, eventDescription = "deleting resource tags")
    public boolean deleteTags(List<String> resourceIds, ResourceObjectType resourceType, Map<String, String> tags) {
        Account caller = CallContext.current().getCallingAccount();

        SearchBuilder<ResourceTagVO> sb = _resourceTagDao.createSearchBuilder();
        sb.and().op("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.IN);
        sb.or("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.IN);
        sb.cp();
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);

        SearchCriteria<ResourceTagVO> sc = sb.create();
        sc.setParameters("resourceId", resourceIds.toArray());
        sc.setParameters("resourceUuid", resourceIds.toArray());
        sc.setParameters("resourceType", resourceType);

        List<? extends ResourceTag> resourceTags = _resourceTagDao.search(sc, null);
        ;
        final List<ResourceTag> tagsToRemove = new ArrayList<ResourceTag>();

        // Finalize which tags should be removed
        for (ResourceTag resourceTag : resourceTags) {
            //1) validate the permissions
            Account owner = _accountMgr.getAccount(resourceTag.getAccountId());
            _accountMgr.checkAccess(caller, null, false, owner);
            //2) Only remove tag if it matches key value pairs
            if (tags != null && !tags.isEmpty()) {
                for (String key : tags.keySet()) {
                    boolean canBeRemoved = false;
                    if (resourceTag.getKey().equalsIgnoreCase(key)) {
                        String value = tags.get(key);
                        if (value != null) {
                            if (resourceTag.getValue().equalsIgnoreCase(value)) {
                                canBeRemoved = true;
                            }
                        } else {
                            canBeRemoved = true;
                        }
                        if (canBeRemoved) {
                            tagsToRemove.add(resourceTag);
                            break;
                        }
                    }
                }
            } else {
                tagsToRemove.add(resourceTag);
            }
        }

        if (tagsToRemove.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find tags by parameters specified");
        }

        //Remove the tags
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (ResourceTag tagToRemove : tagsToRemove) {
                    _resourceTagDao.remove(tagToRemove.getId());
                    s_logger.debug("Removed the tag " + tagToRemove);
                }
            }
        });

        return true;
    }

    @Override
    public List<? extends ResourceTag> listByResourceTypeAndId(ResourceObjectType type, long resourceId) {
        return _resourceTagDao.listBy(resourceId, type);
    }
}
