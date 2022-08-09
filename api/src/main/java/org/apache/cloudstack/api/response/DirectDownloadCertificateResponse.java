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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.direct.download.DirectDownloadCertificate;

import java.util.List;

@EntityReference(value = DirectDownloadCertificate.class)
public class DirectDownloadCertificateResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the direct download certificate id")
    private String id;

    @SerializedName(ApiConstants.ALIAS)
    @Param(description = "the direct download certificate alias")
    private String alias;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone id where the certificate is uploaded")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the zone name where the certificate is uploaded")
    private String zoneName;

    @SerializedName(ApiConstants.VERSION)
    @Param(description = "the direct download certificate version")
    private String version;

    @SerializedName(ApiConstants.CERTIFICATE_SUBJECT)
    @Param(description = "the direct download certificate subject")
    private String subject;

    @SerializedName(ApiConstants.CERTIFICATE_ISSUER)
    @Param(description = "the direct download certificate issuer")
    private String issuer;

    @SerializedName(ApiConstants.CERTIFICATE_VALIDITY)
    @Param(description = "the direct download certificate issuer")
    private String validity;

    @SerializedName(ApiConstants.CERTIFICATE_SERIALNUM)
    @Param(description = "the direct download certificate serial num")
    private String serialNum;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "the hypervisor of the hosts where the certificate is uploaded")
    private String hypervisor;

    @SerializedName(ApiConstants.HOSTS_MAP)
    @Param(description = "the hosts where the certificate is uploaded to", responseObject = HostResponse.class)
    private List<DirectDownloadCertificateHostStatusResponse> hostsMap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public List<DirectDownloadCertificateHostStatusResponse> getHostsMap() {
        return hostsMap;
    }

    public void setHostsMap(List<DirectDownloadCertificateHostStatusResponse> hosts) {
        this.hostsMap = hosts;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getValidity() {
        return validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }
}
