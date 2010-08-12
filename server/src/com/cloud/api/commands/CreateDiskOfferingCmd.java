/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.serializer.Param;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.DiskOfferingVO;

@Implementation(method="createDiskOffering", manager=Manager.ConfigManager)
public class CreateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDiskOfferingCmd.class.getName());

    private static final String s_name = "creatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="disksize", type=CommandType.LONG, required=true)
    private Long diskSize;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String offeringName;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDiskSize() {
        return diskSize;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public String getTags() {
        return tags;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private DiskOfferingVO responseObject = null;

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public String getResponse() {
        DiskOfferingResponse response = new DiskOfferingResponse();
        if (responseObject != null) {
            response.setId(responseObject.getId());
            response.setCreated(responseObject.getCreated());
            response.setDiskSize(responseObject.getDiskSize());
            response.setDisplayText(responseObject.getDisplayText());
            response.setDomainId(responseObject.getDomainId());
            // FIXME:  domain name in the response
//            response.setDomain(responseObject.getDomain());
            response.setName(responseObject.getName());
            response.setTags(responseObject.getTags());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create disk offering");
        }
        return SerializerHelper.toSerializedString(responseObject);
    }

    public void setResponseObject(DiskOfferingVO diskOffering) {
        responseObject = diskOffering;
    }

    // helper class for the response object
    private class DiskOfferingResponse {
        @Param(name="id")
        private Long id;

        @Param(name="domainid")
        private Long domainId;

        @Param(name="domain")
        private String domain;

        @Param(name="name")
        private String name;

        @Param(name="displaytext")
        private String displayText;

        @Param(name="disksize")
        private Long diskSize;

        @Param(name="created")
        private Date created;

        @Param(name="tags")
        private String tags;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getDomainId() {
            return domainId;
        }

        public void setDomainId(Long domainId) {
            this.domainId = domainId;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayText() {
            return displayText;
        }

        public void setDisplayText(String displayText) {
            this.displayText = displayText;
        }

        public Long getDiskSize() {
            return diskSize;
        }

        public void setDiskSize(Long diskSize) {
            this.diskSize = diskSize;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }
    }
}
