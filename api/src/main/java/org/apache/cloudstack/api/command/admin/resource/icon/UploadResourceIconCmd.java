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
package org.apache.cloudstack.api.command.admin.resource.icon;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;


@APICommand(name = "uploadResourceIcon", description = "Uploads an icon for the specified resource(s)",
        responseObject = SuccessResponse.class, since = "4.16.0.0", entityType = {ResourceIcon.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class UploadResourceIconCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.RESOURCE_IDS,
            type = BaseCmd.CommandType.LIST,
            required = true,
            collectionType = BaseCmd.CommandType.STRING,
            description = "list of resources to upload the icon/image for")
    private List<String> resourceIds;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = BaseCmd.CommandType.STRING, required = true, description = "type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.BASE64_IMAGE, type = BaseCmd.CommandType.STRING, required = true,
            description = "Base64 string representation of the resource icon/image", length = 2097152)
    private String image;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getResourceIds() {
        return resourceIds;
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

    public String getImage() {
        if (StringUtils.isEmpty(image)) {
            throw new InvalidParameterValueException("No image provided for resource icon");
        }
        return image;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute()  {
        try {
            if (!imageValidator(getImage())) {
                throw new InvalidParameterValueException("Invalid image uploaded");
            }
            boolean result = resourceIconManager.uploadResourceIcon(getResourceIds(), getResourceType(), getImage());
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to upload resource image");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getLocalizedMessage());
        }
    }

    private boolean imageValidator (String base64Image) {
        BufferedImage image = null;
        byte[] imageByte;
        try {
            imageByte = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
            if (image == null) {
                return false;
            }
        } catch (Exception e) {
            logger.warn("Data uploaded not a valid image");
            return false;
        }
        return true;
    }
    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();// Let's give the caller here for event logging.
        if (account != null) {
            return account.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

}
