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
package org.apache.cloudstack.api.command.user.volume;

import java.net.MalformedURLException;

import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.AbstractGetUploadParamsCmd;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "getUploadParamsForVolume", description = "Upload a data disk to the cloudstack cloud.", responseObject = GetUploadParamsResponse.class, since = "4.6.0",
    requestHasSensitiveInfo= false, responseHasSensitiveInfo = false)
public class GetUploadParamsForVolumeCmd extends AbstractGetUploadParamsCmd {

    private static final String s_name = "postuploadvolumeresponse";

    @Parameter(name = ApiConstants.IMAGE_STORE_UUID, type = CommandType.STRING, description = "Image store uuid")
    private String imageStoreUuid;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID, required = false, type = CommandType.UUID, entityType = DiskOfferingResponse.class, description = "the ID of the disk "
            + "offering. This must be a custom sized offering since during upload of volume/template size is unknown.")
    private Long diskOfferingId;

    public String getImageStoreUuid() {
        return imageStoreUuid;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    @Override
    public void execute() throws ServerApiException {

        try {
            GetUploadParamsResponse response = _volumeService.uploadVolume(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (MalformedURLException | ResourceAllocationException e) {
            logger.error("exception while uploading volume", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "exception while uploading a volume: " + e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(getAccountName(), getDomainId(), getProjectId(), true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }
}
