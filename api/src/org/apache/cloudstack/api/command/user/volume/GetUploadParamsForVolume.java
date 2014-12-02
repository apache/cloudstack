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
import java.net.URL;
import java.util.UUID;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.AbstractGetUploadParamsCommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

@APICommand(name = "getUploadParamsForVolume", description = "Upload a data disk to the cloudstack cloud.", responseObject = GetUploadParamsResponse.class, since = "4.6.0",
    requestHasSensitiveInfo= false, responseHasSensitiveInfo = false)
public class GetUploadParamsForVolume extends AbstractGetUploadParamsCommand {
    public static final Logger s_logger = Logger.getLogger(GetUploadParamsForVolume.class.getName());

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
        // TODO Auto-generated method stub
        try {
            GetUploadParamsResponse response = createGetUploadParamsResponse(
                UUID.fromString("C7D351D2-F167-4CC8-A9FF-3BECB0A625C4"),
                new URL("https://1-2-3-4.xyz.com/upload/C7D351D2-F167-4CC8-A9FF-3BECB0A625C4"),
                "TKPFeuz2nHmE/kcREEu24mnj1MrLdzOeJIHXR9HLIGgk56bkRJHaD0RRL2lds1rKKhrro4/PuleEh4YhRinhxaAmPpU4e55eprG8gTCX0ItyFAtlZViVdKXMew5Dfp4Qg8W9I1/IsDJd2Kas9/ftDQLiemAlPt0uS7Ou6asOCpifnBaKvhM4UGEjHSnni1KhBzjgEyDW3Y42HKJSSv58Sgmxl9LCewBX8vtn9tXKr+j4afj7Jlh7DFhyo9HOPC5ogR4hPBKqP7xF9tHxAyq6YqfBzsng3Xwe+Pb8TU1kFHg1l2DM4tY6ooW2h8lOhWUkrJu4hOAOeTeRtCjW3H452NKoeA1M8pKWuqMo5zRMti2u2hNZs0YY2yOy8oWMMG+lG0hvIlajqEU=",
                "2014-10-17T12:00:00+0530", "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9");
            response.setResponseName(getCommandName());
            setResponseObject(response);

        } catch (MalformedURLException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "malformedurl exception: " + e.getMessage());
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
