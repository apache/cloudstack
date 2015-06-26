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
package org.apache.cloudstack.api;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.command.user.iso.UpdateIsoCmd;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.TemplateResponse;

import java.util.Collection;
import java.util.Map;

public abstract class BaseUpdateTemplateOrIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIsoCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.BOOTABLE, type = CommandType.BOOLEAN, description = "true if image is bootable, false otherwise; available only for updateIso API")
    private Boolean bootable;

    @Parameter(name = ApiConstants.REQUIRES_HVM, type = CommandType.BOOLEAN, description = "true if the template requres HVM, false otherwise; available only for updateTemplate API")
    private Boolean requiresHvm;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the display text of the image", length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplateResponse.class, required = true, description = "the ID of the image file")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the image file")
    private String templateName;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
               type = CommandType.UUID,
               entityType = GuestOSResponse.class,
               description = "the ID of the OS type that best represents the OS of this image.")
    private Long osTypeId;

    @Parameter(name = ApiConstants.FORMAT, type = CommandType.STRING, description = "the format for the image")
    private String format;

    @Parameter(name = ApiConstants.PASSWORD_ENABLED, type = CommandType.BOOLEAN, description = "true if the image supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER, description = "sort key of the template, integer")
    private Integer sortKey;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE,
               type = CommandType.BOOLEAN,
               description = "true if template/ISO contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory")
    private Boolean isDynamicallyScalable;

    @Parameter(name = ApiConstants.ROUTING, type = CommandType.BOOLEAN, description = "true if the template type is routing i.e., if template is used to deploy router")
    protected Boolean isRoutingType;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].hypervisortoolsversion=xenserver61")
    protected Map details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean getBootable() {
        return bootable;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Boolean getPasswordEnabled() {
        return passwordEnabled;
    }

    public String getFormat() {
        return format;
    }

    public Integer getSortKey() {
        return sortKey;
    }

    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public Boolean isRoutingType() {
        return isRoutingType;
    }

    public Map getDetails() {
        if (this.details == null || this.details.isEmpty()) {
            return null;
        }

        Collection paramsCollection = this.details.values();
        return (Map) (paramsCollection.toArray())[0];
    }
}