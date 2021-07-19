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
package com.cloud.uuididentity;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.UUIDManager;
import com.cloud.utils.exception.CloudRuntimeException;

public class UUIDManagerImpl implements UUIDManager {

    @Inject
    EntityManager _entityMgr;
    @Inject
    AccountManager _accountMgr;
    //TODO - Make this configurable.
    private static final int UUID_RETRY = 3;

    @Override
    public <T> void checkUuid(String uuid, Class<T> entityType) {

        if (uuid == null) {
            return;
        }

        Account caller = CallContext.current().getCallingAccount();

        // Only admin and system allowed to do this
        if (!(caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getId()))) {
            throw new PermissionDeniedException("Please check your permissions, you are not allowed to create/update custom id");
        }

        checkUuidSimple(uuid, entityType);
    }

    @Override
    public <T> void checkUuidSimple(String uuid, Class<T> entityType) {

        if (uuid == null) {
            return;
        }

        // check format
        if (!IsUuidFormat(uuid)) {
            throw new InvalidParameterValueException("UUID: " + uuid + " doesn't follow the UUID format");
        }

        // check unique
        if (!IsUuidUnique(entityType, uuid)) {
            throw new InvalidParameterValueException("UUID: " + uuid + " already exists so can't create/update with custom id");
        }

    }

    public boolean IsUuidFormat(String uuid) {

        // Match against UUID regex to check if input is uuid string
        boolean isUuid = uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        return isUuid;
    }

    public <T> boolean IsUuidUnique(Class<T> entityType, String uuid) {

        T obj = _entityMgr.findByUuid(entityType, uuid);
        if (obj != null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public <T> String generateUuid(Class<T> entityType, String customId) {

        if (customId == null) { // if no customid is passed then generate it.
            int retry = UUID_RETRY;
            while (retry-- != 0) {  // there might be collision so retry
                String uuid = UUID.randomUUID().toString();
                if (IsUuidUnique(entityType, uuid)) {
                    return uuid;
                }
            }

            throw new CloudRuntimeException("Unable to generate a unique uuid, please try again");
        } else {
            checkUuid(customId, entityType);
            return customId;
        }
    }

    @Override
    public <T> String getUuid(Class<T> entityType, Long customId) {
        if (customId == null) {
            return null;
        }
        if (entityType == null ) {
            throw new InvalidParameterValueException("Unknown entity type");
        }

        Identity identity = (Identity) this._entityMgr.findById(entityType, customId);
        if (identity == null) {
            throw new InvalidParameterValueException(String.format("Unable to find UUID for id [%s] of type [%s]",
                                                                   customId, entityType.getSimpleName()));

        }
        return identity.getUuid();
    }
}
