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
package org.apache.cloudstack.api.command.admin.resource;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.CustomCertificateResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "uploadCustomCertificate",
            responseObject = CustomCertificateResponse.class,
            description = "Uploads a custom certificate for the console proxy VMs to use for SSL. Can be used to upload a single certificate signed by a known CA. Can also be used, through multiple calls, to upload a chain of certificates from CA to the custom certificate itself.",
            requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class UploadCustomCertificateCmd extends BaseAsyncCmd {


    @Parameter(name = ApiConstants.CERTIFICATE, type = CommandType.STRING, required = true, description = "The certificate to be uploaded.", length = 65535)
    private String certificate;

    @Parameter(name = ApiConstants.ID,
               type = CommandType.INTEGER,
               required = false,
               description = "An integer providing the location in a chain that the certificate will hold. Usually, this can be left empty. When creating a chain, the top level certificate should have an ID of 1, with each step in the chain incrementing by one. Example, CA with id = 1, Intermediate CA with id = 2, Site certificate with ID = 3")
    private Integer index;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = false, description = "A name / alias for the certificate.")
    private String alias;

    @Parameter(name = ApiConstants.PRIVATE_KEY,
               type = CommandType.STRING,
               required = false,
               description = "The private key for the attached certificate.",
               length = 65535)
    private String privateKey;

    @Parameter(name = ApiConstants.DOMAIN_SUFFIX, type = CommandType.STRING, required = true, description = "DNS domain suffix that the certificate is granted for.")
    private String domainSuffix;

    public String getCertificate() {
        return certificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getDomainSuffix() {
        return domainSuffix;
    }

    public Integer getCertIndex() {
        return index;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_UPLOAD_CUSTOM_CERTIFICATE;
    }

    @Override
    public String getEventDescription() {
        return ("Uploading custom certificate to the db, and applying it to all the cpvms in the system");
    }

    public static String getResultObjectName() {
        return "certificate";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        String result = _mgr.uploadCertificate(this);
        if (result != null) {
            CustomCertificateResponse response = new CustomCertificateResponse();
            response.setResponseName(getCommandName());
            response.setResultMessage(result);
            response.setObjectName("customcertificate");
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to upload custom certificate");
        }
    }

}
