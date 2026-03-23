/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.user.kms;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.kms.KMSKey;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "deleteKMSKey",
            description = "Deletes a KMS key (only if not in use)",
            responseObject = SuccessResponse.class,
            since = "4.23.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class DeleteKMSKeyCmd extends BaseAsyncCmd implements UserCmd {

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.ID,
               required = true,
               type = CommandType.UUID,
               entityType = KMSKeyResponse.class,
               description = "The UUID of the KMS key to delete")
    private Long id;

    @Override
    public void execute() {
        try {
            SuccessResponse response = kmsManager.deleteKMSKey(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (KMSException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to delete KMS key: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        KMSKey key = _entityMgr.findById(KMSKey.class, id);
        if (key != null) {
            return key.getAccountId();
        }
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KmsKey;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_KMS_KEY_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "deleting KMS key: " + getId();
    }

    public Long getId() {
        return id;
    }
}
