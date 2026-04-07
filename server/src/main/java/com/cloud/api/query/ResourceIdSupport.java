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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.UUID;
import static org.apache.cloudstack.acl.SecurityChecker.AccessType;

/**
 * Support interface for converting resource UUIDs to internal IDs
 * with validation and access control.
 *
 * @author mprokopchuk
 */
public interface ResourceIdSupport {

    EntityManager getEntityManager();

    AccountManager getAccountManager();

    /**
     * Converts resource UUID to internal database ID with access control checks.
     *
     * @param resourceType type of the resource
     * @param resourceUuid UUID of the resource
     * @return internal resource ID or null if parameters are null
     * @throws InvalidParameterValueException if only one parameter provided or resource not found
     */
    default Long getResourceId(ApiCommandResourceType resourceType, String resourceUuid) {
        String uuid = getResourceUuid(resourceUuid);

        if (resourceType == null || uuid == null) {
            return null;
        } else if ((resourceType == null) ^ (uuid == null)) {
            throw new InvalidParameterValueException(String.format("Both %s and %s required",
                    ApiConstants.RESOURCE_ID, ApiConstants.RESOURCE_TYPE));
        }

        Object object = getEntityManager().findByUuidIncludingRemoved(resourceType.getAssociatedClass(), resourceUuid);
        if (!(object instanceof InternalIdentity)) {
            throw new InvalidParameterValueException(String.format("Invalid %s", ApiConstants.RESOURCE_ID));
        }
        Long resourceId = ((InternalIdentity) object).getId();

        Account caller = CallContext.current().getCallingAccount();
        boolean isRootAdmin = getAccountManager().isRootAdmin(caller.getId());

        if (!isRootAdmin && object instanceof ControlledEntity) {
            ControlledEntity entity = (ControlledEntity) object;
            boolean sameOwner = entity.getAccountId() == caller.getId();
            getAccountManager().checkAccess(caller, AccessType.ListEntry, sameOwner, entity);
        }

        return resourceId;
    }

    /**
     * Parses and validates resource type string.
     *
     * @param resourceType resource type as string
     * @return parsed resource type or null if not provided
     * @throws InvalidParameterValueException if provided type is invalid
     */
    default ApiCommandResourceType getResourceType(String resourceType) {
        Optional<String> resourceTypeOpt = Optional.ofNullable(resourceType).filter(StringUtils::isNotBlank);
        // return null if resource type was not provided
        if (resourceTypeOpt.isEmpty()) {
            return null;
        }
        // return value or throw exception if provided resource type is invalid
        return resourceTypeOpt
                .map(ApiCommandResourceType::fromString)
                .orElseThrow(() -> new InvalidParameterValueException(String.format("Invalid %s",
                        ApiConstants.RESOURCE_TYPE)));
    }

    /**
     * Validates resource UUID format.
     *
     * @param resourceUuid UUID string to validate
     * @return validated UUID or null if not provided
     * @throws InvalidParameterValueException if UUID format is invalid
     */
    default String getResourceUuid(String resourceUuid) {
        if (StringUtils.isBlank(resourceUuid)) {
            return null;
        }

        try {
            UUID.fromString(resourceUuid);
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException(String.format("Invalid %s", ApiConstants.RESOURCE_ID));
        }

        return resourceUuid;
    }

}
