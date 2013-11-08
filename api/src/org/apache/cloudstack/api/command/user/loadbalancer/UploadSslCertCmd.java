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
package org.apache.cloudstack.api.command.user.loadbalancer;


import org.apache.cloudstack.api.response.SslCertResponse;
import com.cloud.exception.*;
import com.cloud.network.lb.CertService;
import org.apache.cloudstack.api.*;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "uploadSslCert", description="Upload a certificate to cloudstack", responseObject=SslCertResponse.class)
public class UploadSslCertCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UploadSslCertCmd.class.getName());

    private static final String s_name = "uploadsslcertresponse";

    @Inject CertService _certService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name= ApiConstants.CERTIFICATE, type=CommandType.STRING, required=true, description="SSL certificate",length=16384)
    private String cert;

    @Parameter(name=ApiConstants.PRIVATE_KEY, type=CommandType.STRING, required=true, description="Private key", length=16384)
    private String key;

    @Parameter(name=ApiConstants.CERTIFICATE_CHAIN, type=CommandType.STRING, description="Certificate chain of trust", length=2097152)
    private String chain;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, description="Password for the private key")
    private String password;



    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCert() {
        return cert;
    }

    public String getKey() {
        return key;
    }

    public String getChain() {
        return chain;
    }

    public String getPassword() {
        return password;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

        try{
            SslCertResponse response = _certService.uploadSslCert(this);
            setResponseObject(response);
            response.setResponseName(getCommandName());
        } catch (Exception e){
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}