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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.domain.PartOf;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.projects.ProjectVO;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.OwnedBy;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class TaggedResourceManagerImpl extends ManagerBase implements TaggedResourceService {
    public static final Logger s_logger = Logger.getLogger(TaggedResourceManagerImpl.class);

    @Inject
    EntityManager _entityMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    DomainManager _domainMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    ResourceManagerUtil resourceManagerUtil;
    @Inject
    VolumeDao volumeDao;
    @Inject
    DataStoreManager dataStoreMgr;

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

    private Pair<Long, Long> getAccountDomain(long resourceId, ResourceObjectType resourceType) {
        Class<?> clazz = ResourceManagerUtilImpl.s_typeMap.get(resourceType);

        Object entity = _entityMgr.findById(clazz, resourceId);
        Long accountId = null;
        Long domainId = null;

        // if the resource type is a security group rule, get the accountId and domainId from the security group itself
        if (resourceType == ResourceObjectType.SecurityGroupRule) {
            SecurityGroupRuleVO rule = (SecurityGroupRuleVO)entity;
            Object SecurityGroup = _entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceObjectType.SecurityGroup), rule.getSecurityGroupId());

            accountId = ((SecurityGroupVO)SecurityGroup).getAccountId();
            domainId = ((SecurityGroupVO)SecurityGroup).getDomainId();
        }

        if (resourceType == ResourceObjectType.Account) {
            AccountVO account = (AccountVO)entity;
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        // if the resource type is network acl, get the accountId and domainId from VPC following: NetworkACLItem -> NetworkACL -> VPC
        if (resourceType == ResourceObjectType.NetworkACL) {
            NetworkACLItemVO aclItem = (NetworkACLItemVO)entity;
            Object networkACL = _entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceObjectType.NetworkACLList), aclItem.getAclId());
            Long vpcId = ((NetworkACLVO)networkACL).getVpcId();

            if (vpcId != null && vpcId != 0) {
                Object vpc = _entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceObjectType.Vpc), vpcId);

                accountId = ((VpcVO)vpc).getAccountId();
                domainId = ((VpcVO)vpc).getDomainId();
            }
        }

        if (resourceType == ResourceObjectType.Project) {
            accountId = ((ProjectVO)entity).getProjectAccountId();
        }

        if (resourceType == ResourceObjectType.SnapshotPolicy) {
            accountId = _entityMgr.findById(VolumeVO.class, ((SnapshotPolicyVO)entity).getVolumeId()).getAccountId();
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

        if ((domainId == null) || ((accountId != null) && (domainId.longValue() == -1))) {
            domainId = _accountDao.getDomainIdForGivenAccountId(accountId);
        }
        return new Pair<>(accountId, domainId);
    }

    protected void checkTagsDeletePermission(List<ResourceTag> tagsToDelete, Account caller) {
        for (ResourceTag resourceTag : tagsToDelete) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Resource Tag Id: " + resourceTag.getResourceId());
                s_logger.debug("Resource Tag AccountId: " + resourceTag.getAccountId());
            }
            if (caller.getAccountId() != resourceTag.getAccountId()) {
                Account owner = _accountMgr.getAccount(resourceTag.getAccountId());
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Resource Owner: " + owner);
                }
                _accountMgr.checkAccess(caller, null, false, owner);
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_CREATE, eventDescription = "creating resource tags")
    public List<ResourceTag> createTags(final List<String> resourceIds, final ResourceObjectType resourceType, final Map<String, String> tags, final String customer) {
        final Account caller = CallContext.current().getCallingAccount();

        final List<ResourceTag> resourceTags = new ArrayList<>(tags.size());

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String key : tags.keySet()) {
                    for (String resourceId : resourceIds) {
                        if (!resourceType.resourceTagsSupport()) {
                            throw new InvalidParameterValueException("The resource type " + resourceType + " doesn't support resource tags");
                        }

                        long id = resourceManagerUtil.getResourceId(resourceId, resourceType);
                        String resourceUuid = resourceManagerUtil.getUuid(resourceId, resourceType);

                        Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                        Long domainId = accountDomainPair.second();
                        Long accountId = accountDomainPair.first();

                        resourceManagerUtil.checkResourceAccessible(accountId, domainId, "Account '" + caller +
                                "' doesn't have permissions to create tags" + " for resource '" + id + "(" + key + ")'.");

                        String value = tags.get(key);

                        if (value == null || value.isEmpty()) {
                            throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                        }

                        ResourceTagVO resourceTag = new ResourceTagVO(key, value, accountDomainPair.first(), accountDomainPair.second(), id, resourceType, customer, resourceUuid);
                        try {
                            resourceTag = _resourceTagDao.persist(resourceTag);
                        } catch (EntityExistsException e) {
                            throw new CloudRuntimeException(String.format("tag %s already on %s with id %s", resourceTag.getKey(), resourceType.toString(), resourceId),e);
                        }
                        resourceTags.add(resourceTag);
                        if (ResourceObjectType.UserVm.equals(resourceType)) {
                            informStoragePoolForVmTags(id, key, value);
                        }
                    }
                }
            }
        });

        return resourceTags;
    }

    private List<? extends ResourceTag> searchResourceTags(List<String> resourceIds, ResourceObjectType resourceType) {
        List<String> resourceUuids = resourceIds.stream().map(resourceId -> resourceManagerUtil.getUuid(resourceId, resourceType)).collect(Collectors.toList());
        SearchBuilder<ResourceTagVO> sb = _resourceTagDao.createSearchBuilder();
        sb.and("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.IN);
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);

        SearchCriteria<ResourceTagVO> sc = sb.create();
        sc.setParameters("resourceUuid", resourceUuids.toArray());
        sc.setParameters("resourceType", resourceType);
        return _resourceTagDao.search(sc, null);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_DELETE, eventDescription = "deleting resource tags")
    public boolean deleteTags(List<String> resourceIds, ResourceObjectType resourceType, Map<String, String> tags) {
        Account caller = CallContext.current().getCallingAccount();
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("ResourceIds to Find " + String.join(", ", resourceIds));
        }
        List<? extends ResourceTag> resourceTags = searchResourceTags(resourceIds, resourceType);
        final List<ResourceTag> tagsToDelete = new ArrayList<>();

        // Finalize which tags should be removed
        for (ResourceTag resourceTag : resourceTags) {
            if (MapUtils.isEmpty(tags)) {
                tagsToDelete.add(resourceTag);
            } else {
                for (String key : tags.keySet()) {
                    boolean deleteTag = false;
                    if (resourceTag.getKey().equalsIgnoreCase(key)) {
                        String value = tags.get(key);
                        if (value != null) {
                            if (resourceTag.getValue().equalsIgnoreCase(value)) {
                                deleteTag = true;
                            }
                        } else {
                            deleteTag = true;
                        }
                        if (deleteTag) {
                            tagsToDelete.add(resourceTag);
                            break;
                        }
                    }
                }
            }
        }

        if (tagsToDelete.isEmpty()) {
            return false;
        }
        checkTagsDeletePermission(tagsToDelete, caller);

        //Remove the tags
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (ResourceTag tagToRemove : tagsToDelete) {
                    _resourceTagDao.remove(tagToRemove.getId());
                    s_logger.debug("Removed the tag '" + tagToRemove + "' for resources (" +
                            String.join(", ", resourceIds) + ")");
                    if (ResourceObjectType.UserVm.equals(resourceType)) {
                        informStoragePoolForVmTags(tagToRemove.getResourceId(), tagToRemove.getKey(), tagToRemove.getValue());
                    }
                }
            }
        });

        return true;
    }

    @Override
    public List<? extends ResourceTag> listByResourceTypeAndId(ResourceObjectType resourceType, long resourceId) {
        return _resourceTagDao.listBy(resourceId, resourceType);
    }

    @Override
    public Map<String, String> getTagsFromResource(ResourceObjectType type, long resourceId) {
        List<? extends ResourceTag> listResourceTags = listByResourceTypeAndId(type, resourceId);
        return listResourceTags == null ? null : listResourceTags.stream().collect(Collectors.toMap(ResourceTag::getKey, ResourceTag::getValue));
    }


    private void informStoragePoolForVmTags(long vmId, String key, String value) {
        List<VolumeVO> volumeVos = volumeDao.findByInstance(vmId);
        for (VolumeVO volume : volumeVos) {
            DataStore dataStore = dataStoreMgr.getDataStore(volume.getPoolId(), DataStoreRole.Primary);
            if (dataStore == null || !(dataStore.getDriver() instanceof PrimaryDataStoreDriver)) {
                continue;
            }
            PrimaryDataStoreDriver dataStoreDriver = (PrimaryDataStoreDriver) dataStore.getDriver();
            if (dataStoreDriver.isVmTagsNeeded(key)) {
                dataStoreDriver.provideVmTags(vmId, volume.getId(), value);
            }
        }
    }
}
