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

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.metadata.ResourceMetaDataManagerImpl;
import com.cloud.resource.icon.dao.ResourceIconDao;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceIconManager;

import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceIconManagerImpl extends ManagerBase implements ResourceIconManager {
    public static final Logger s_logger = Logger.getLogger(ResourceMetaDataManagerImpl.class);

//    private static final Map<ResourceTag.ResourceObjectType, Class<?>> s_typeMap = new HashMap<>();
//    static {
//        s_typeMap.put(ResourceTag.ResourceObjectType.UserVm, UserVmVO.class);
//        s_typeMap.put(ResourceTag.ResourceObjectType.Template, VMTemplateVO.class);
//        s_typeMap.put(ResourceTag.ResourceObjectType.ISO, VMTemplateVO.class);
//        s_typeMap.put(ResourceTag.ResourceObjectType.Account, AccountVO.class);
//        s_typeMap.put(ResourceTag.ResourceObjectType.Zone, DataCenterVO.class);
//        s_typeMap.put(ResourceTag.ResourceObjectType.User, UserVO.class);
//    }

    @Inject
    AccountService accountService;
    @Inject
    ResourceManagerUtil resourceManagerUtil;
    @Inject
    ResourceIconDao resourceIconDao;


    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_ICON_UPLOAD, eventDescription = "uploading resource icon")
    public boolean uploadResourceIcon(List<String> resourceIds, ResourceTag.ResourceObjectType resourceType, String base64Image) {
        final Account caller = CallContext.current().getCallingAccount();
        if (!accountService.isAdmin(caller.getId())) {
            throw new PermissionDeniedException("Current Account: " + caller.getAccountName() + " does not have permission to upload resource icons");
        }

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
                    ResourceIconVO resourceIcon = new ResourceIconVO(id, resourceType, resourceUuid, base64Image);
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
    public boolean deleteResourceIcon(List<String> resourceIds, ResourceTag.ResourceObjectType resourceType) {
        Account caller = CallContext.current().getCallingAccount();
        if (!accountService.isAdmin(caller.getId())) {
            throw new PermissionDeniedException("Current Account: " + caller.getAccountName() + " does not have permission to delete resource icons");
        }
        List<? extends ResourceIcon> resourceIcons = searchResourceIcons(resourceIds, resourceType);
        if (resourceIcons.isEmpty()) {
            return false;
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (ResourceIcon resourceIcon : resourceIcons) {
                    resourceIconDao.remove(resourceIcon.getId());
                    s_logger.debug("Removed icon for resources (" +
                            String.join(", ", resourceIds) + ")");
                }
            }
        });
        return false;
    }

    @Override
    public ResourceIcon getByResourceTypeAndId(ResourceTag.ResourceObjectType type, long resourceId) {
        return null;
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
