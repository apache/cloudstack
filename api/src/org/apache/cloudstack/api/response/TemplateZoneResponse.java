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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class TemplateZoneResponse extends BaseResponse {
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the ID of the zone for the template")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the name of the zone for the template")
    private String zoneName;

    @SerializedName(ApiConstants.STATUS) @Param(description="the status of the template")
    private String status;

    @SerializedName(ApiConstants.IS_READY) // propName="ready"  (FIXME:  this used to be part of Param annotation, do we need it?)
    @Param(description="true if the template is ready to be deployed from, false otherwise.")
    private boolean isReady;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date this template was created")
    private Date created;

    public TemplateZoneResponse(){
        super();
    }

    public TemplateZoneResponse(String zoneId, String zoneName){
        super();
        this.zoneId = zoneId;
        this.zoneName = zoneName;
    }



    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String oid = this.getZoneId();
        result = prime * result + ((oid== null) ? 0 : oid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TemplateZoneResponse other = (TemplateZoneResponse) obj;
        String oid = this.getZoneId();
        if (oid == null) {
            if (other.getZoneId() != null) {
                return false;
            }
        } else if (!oid.equals(other.getZoneId())) {
            return false;
        } else if ( this.getZoneName().equals(other.getZoneName())) {
            return false;
        }
        return true;
    }

}
