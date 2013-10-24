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
package org.apache.cloudstack.api.command.user.offering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listServiceOfferings", description="Lists all available service offerings.", responseObject=ServiceOfferingResponse.class)
public class ListServiceOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listserviceofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = ServiceOfferingResponse.class,
            description="ID of the service offering")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the service offering")
    private String serviceOfferingName;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType = UserVmResponse.class,
            description="the ID of the virtual machine. Pass this in if you want to see the available service offering that a virtual machine can be changed to.")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="the ID of the domain associated with the service offering")
    private Long domainId;

    @Parameter(name=ApiConstants.IS_SYSTEM_OFFERING, type=CommandType.BOOLEAN, description="is this a system vm offering")
    private Boolean isSystem;

    @Parameter(name=ApiConstants.SYSTEM_VM_TYPE, type=CommandType.STRING, description="the system VM type. Possible types are \"consoleproxy\", \"secondarystoragevm\" or \"domainrouter\".")
    private String systemVmType;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getDomainId(){
        return domainId;
    }

    public Boolean getIsSystem() {
        return isSystem == null ? false : isSystem;
    }

    public String getSystemVmType(){
        return systemVmType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ListResponse<ServiceOfferingResponse> response = _queryService.searchForServiceOfferings(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }
}
