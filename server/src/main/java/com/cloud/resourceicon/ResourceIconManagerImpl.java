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
package com.cloud.resourceicon;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.domain.PartOf;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.metadata.ResourceMetaDataManagerImpl;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.projects.ProjectVO;
import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.resource.icon.dao.ResourceIconDao;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceIconManager;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.VolumeVO;
import com.cloud.tags.ResourceManagerUtilImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
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

public class ResourceIconManagerImpl extends ManagerBase implements ResourceIconManager {
    public static final Logger s_logger = Logger.getLogger(ResourceMetaDataManagerImpl.class);

    @Inject
    AccountService accountService;
    @Inject
    ResourceManagerUtil resourceManagerUtil;
    @Inject
    ResourceIconDao resourceIconDao;
    @Inject
    EntityManager entityMgr;
    @Inject
    AccountDao accountDao;

    private Pair<Long, Long> getAccountDomain(long resourceId, ResourceTag.ResourceObjectType resourceType) {
        Class<?> clazz = ResourceManagerUtilImpl.s_typeMap.get(resourceType);

        Object entity = entityMgr.findById(clazz, resourceId);
        Long accountId = null;
        Long domainId = null;

        // if the resource type is a security group rule, get the accountId and domainId from the security group itself
        if (resourceType == ResourceTag.ResourceObjectType.SecurityGroupRule) {
            SecurityGroupRuleVO rule = (SecurityGroupRuleVO)entity;
            Object SecurityGroup = entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceTag.ResourceObjectType.SecurityGroup), rule.getSecurityGroupId());

            accountId = ((SecurityGroupVO)SecurityGroup).getAccountId();
            domainId = ((SecurityGroupVO)SecurityGroup).getDomainId();
        }

        if (resourceType == ResourceTag.ResourceObjectType.Account) {
            AccountVO account = (AccountVO)entity;
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        // if the resource type is network acl, get the accountId and domainId from VPC following: NetworkACLItem -> NetworkACL -> VPC
        if (resourceType == ResourceTag.ResourceObjectType.NetworkACL) {
            NetworkACLItemVO aclItem = (NetworkACLItemVO)entity;
            Object networkACL = entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceTag.ResourceObjectType.NetworkACLList), aclItem.getAclId());
            Long vpcId = ((NetworkACLVO)networkACL).getVpcId();

            if (vpcId != null && vpcId != 0) {
                Object vpc = entityMgr.findById(ResourceManagerUtilImpl.s_typeMap.get(ResourceTag.ResourceObjectType.Vpc), vpcId);

                accountId = ((VpcVO)vpc).getAccountId();
                domainId = ((VpcVO)vpc).getDomainId();
            }
        }

        if (resourceType == ResourceTag.ResourceObjectType.Project) {
            accountId = ((ProjectVO)entity).getProjectAccountId();
        }

        if (resourceType == ResourceTag.ResourceObjectType.SnapshotPolicy) {
            accountId = entityMgr.findById(VolumeVO.class, ((SnapshotPolicyVO)entity).getVolumeId()).getAccountId();
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
            domainId = accountDao.getDomainIdForGivenAccountId(accountId);
        }
        return new Pair<>(accountId, domainId);
    }

    private void updateResourceDetailsInContext(Long resourceId, ResourceTag.ResourceObjectType resourceType) {
        Class<?> clazz = ResourceManagerUtilImpl.s_typeMap.get(resourceType);
        ApiCommandResourceType type = ApiCommandResourceType.valueFromAssociatedClass(clazz);
        int depth = 5;
        while (type == null && depth > 0) {
            Class<?>[] clazzes = clazz.getInterfaces();
            if (clazzes.length == 0) {
                break;
            }
            depth--;
            clazz = clazzes[0];
            type = ApiCommandResourceType.valueFromAssociatedClass(clazz);
        }
        CallContext.current().setEventResourceId(resourceId);
        if (!ApiCommandResourceType.None.equals(type)) {
            CallContext.current().setEventResourceType(type);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ICON_UPLOAD, eventDescription = "uploading resource icon")
    public boolean uploadResourceIcon(List<String> resourceIds, ResourceTag.ResourceObjectType resourceType, String base64Image) {
        final Account caller = CallContext.current().getCallingAccount();

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String resourceId : resourceIds) {
                    if (!resourceType.resourceIconSupport()) {
                        throw new InvalidParameterValueException("The resource type " + resourceType + " doesn't support resource icons");
                    }

                    if (base64Image == null) {
                        throw new InvalidParameterValueException("No icon provided to be uploaded for resource: " + resourceId);
                    }

                    long id = resourceManagerUtil.getResourceId(resourceId, resourceType);
                    String resourceUuid = resourceManagerUtil.getUuid(resourceId, resourceType);
                    updateResourceDetailsInContext(id, resourceType);
                    ResourceIconVO existingResourceIcon = resourceIconDao.findByResourceUuid(resourceUuid, resourceType);
                    ResourceIconVO resourceIcon = null;
                    Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                    Long domainId = accountDomainPair.second();
                    Long accountId = accountDomainPair.first();
                    resourceManagerUtil.checkResourceAccessible(accountId, domainId, String.format("Account ' %s ' doesn't have permissions to upload icon for resource ' %s ", caller, id));

                    if (existingResourceIcon == null) {
                        resourceIcon = new ResourceIconVO(id, resourceType, resourceUuid, base64Image);
                    } else {
                        resourceIcon = existingResourceIcon;
                        resourceIcon.setIcon(base64Image);
                        resourceIcon.setUpdated(new Date());
                    }
                    try {
                        resourceIconDao.persist(resourceIcon);
                    } catch (EntityExistsException e) {
                        throw new CloudRuntimeException(String.format("Image already uploaded for resource type: %s with id %s",  resourceType.toString(), resourceId),e);
                    }
                }
            }
        });

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ICON_DELETE, eventDescription = "deleting resource icon")
    public boolean deleteResourceIcon(List<String> resourceIds, ResourceTag.ResourceObjectType resourceType) {
        Account caller = CallContext.current().getCallingAccount();
        List<? extends ResourceIcon> resourceIcons = searchResourceIcons(resourceIds, resourceType);
        if (resourceIcons.isEmpty()) {
            s_logger.debug("No resource Icon(s) uploaded for the specified resources");
            return false;
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (ResourceIcon resourceIcon : resourceIcons) {
                    String resourceId = resourceIcon.getResourceUuid();
                    long id = resourceManagerUtil.getResourceId(resourceId, resourceType);
                    updateResourceDetailsInContext(id, resourceType);
                    Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                    Long domainId = accountDomainPair.second();
                    Long accountId = accountDomainPair.first();
                    resourceManagerUtil.checkResourceAccessible(accountId, domainId, String.format("Account ' %s ' doesn't have permissions to upload icon for resource ' %s ", caller, id));
                    resourceIconDao.remove(resourceIcon.getId());
                    s_logger.debug("Removed icon for resources (" +
                            String.join(", ", resourceIds) + ")");
                }
            }
        });
        return true;
    }

    @Override
    public ResourceIcon getByResourceTypeAndUuid(ResourceTag.ResourceObjectType type, String resourceId) {
        return resourceIconDao.findByResourceUuid(resourceId, type);
    }

    private List<? extends ResourceIcon> searchResourceIcons(List<String> resourceIds, ResourceTag.ResourceObjectType resourceType) {
        List<String> resourceUuids = resourceIds.stream().map(resourceId -> resourceManagerUtil.getUuid(resourceId, resourceType)).collect(Collectors.toList());
        SearchBuilder<ResourceIconVO> sb = resourceIconDao.createSearchBuilder();
        sb.and("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.IN);
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);

        SearchCriteria<ResourceIconVO> sc = sb.create();
        sc.setParameters("resourceUuid", resourceUuids.toArray());
        sc.setParameters("resourceType", resourceType);
        return resourceIconDao.search(sc, null);
    }
}
