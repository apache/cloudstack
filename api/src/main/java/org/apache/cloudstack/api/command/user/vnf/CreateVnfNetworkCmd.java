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
package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name = "createVnfNetwork",
        description = "Create a VNF network (deploy VR broker + VNF VM)",
        responseObject = CreateNetworkResponse.class)
public class CreateVnfNetworkCmd extends BaseAsyncCreateCmd {
    @Parameter(name="name", type=CommandType.STRING, required=true) private String name;
    @Parameter(name="displaytext", type=CommandType.STRING) private String displayText;
    @Parameter(name="zoneid", type=CommandType.UUID, entityType=ZoneResponse.class, required=true) private Long zoneId;
    @Parameter(name="vnftemplateid", type=CommandType.UUID, entityType=TemplateResponse.class, required=true) private Long vnfTemplateId;
    @Parameter(name="servicehelpers", type=CommandType.STRING) private String serviceHelpers;
    @Parameter(name="dictionaryyaml", type=CommandType.STRING) private String dictionaryYaml;

    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;

    @Override public void execute() {
        CreateNetworkResponse resp = vnfSvc.createVnfNetwork(this);
        setResponseObject(resp); resp.setResponseName(getCommandName());
    }
    @Override public String getCommandName() { return "createvnfnetworkresponse"; }
    @Override public long getEntityOwnerId() { return CallContext.current().getCallingAccountId(); }

    // getters...
    public String getName(){return name;} public String getDisplayText(){return displayText;}
    public Long getZoneId(){return zoneId;} public Long getVnfTemplateId(){return vnfTemplateId;}
    public String getServiceHelpers(){return serviceHelpers;} public String getDictionaryYaml(){return dictionaryYaml;}
}
