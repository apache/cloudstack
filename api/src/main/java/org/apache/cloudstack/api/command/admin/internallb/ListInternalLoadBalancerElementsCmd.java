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
package org.apache.cloudstack.api.command.admin.internallb;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InternalLoadBalancerElementResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.VirtualRouterProvider;

@APICommand(name = "listInternalLoadBalancerElements",
            description = "Lists all available Internal Load Balancer elements.",
            responseObject = InternalLoadBalancerElementResponse.class,
            since = "4.2.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListInternalLoadBalancerElementsCmd extends BaseListCmd {

    @Inject
    private InternalLoadBalancerElementService _service;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = InternalLoadBalancerElementResponse.class,
               description = "list internal load balancer elements by id")
    private Long id;

    @Parameter(name = ApiConstants.NSP_ID,
               type = CommandType.UUID,
               entityType = ProviderResponse.class,
               description = "list internal load balancer elements by network service provider id")
    private Long nspId;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "list internal load balancer elements by enabled state")
    private Boolean enabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getNspId() {
        return nspId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        List<? extends VirtualRouterProvider> providers = _service.searchForInternalLoadBalancerElements(getId(), getNspId(), getEnabled());
        ListResponse<InternalLoadBalancerElementResponse> response = new ListResponse<InternalLoadBalancerElementResponse>();
        List<InternalLoadBalancerElementResponse> providerResponses = new ArrayList<InternalLoadBalancerElementResponse>();
        for (VirtualRouterProvider provider : providers) {
            InternalLoadBalancerElementResponse providerResponse = _responseGenerator.createInternalLbElementResponse(provider);
            providerResponses.add(providerResponse);
        }
        response.setResponses(providerResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }
}
