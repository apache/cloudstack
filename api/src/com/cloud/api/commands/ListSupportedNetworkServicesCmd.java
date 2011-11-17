/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ServiceResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.user.Account;


@Implementation(description="Lists all network services provided by CloudStack or for the given Provider.", responseObject=ServiceResponse.class)
public class ListSupportedNetworkServicesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSupportedNetworkServicesCmd.class.getName());
    private static final String _name = "listsupportednetworkservicesresponse";
    
    @Parameter(name=ApiConstants.PROVIDER, type=CommandType.STRING, description="network service provider name")
    private String providerName;
    
    @Parameter(name=ApiConstants.SERVICE, type=CommandType.STRING, description="network service name to list providers and capabilities of")
    private String serviceName;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }


    public String getServiceName() {
        return serviceName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        List<? extends Network.Service> services; 
        if(getServiceName() != null){
            Network.Service service = null;
            if(serviceName != null){
                service = Network.Service.getService(serviceName);
                if(service == null){
                    throw new InvalidParameterValueException("Invalid Network Service=" + serviceName);
                }
            }
            List<Network.Service> serviceList = new ArrayList<Network.Service>();
            serviceList.add(service);
            services = serviceList;
        }else{
            services = _networkService.listNetworkServices(getProviderName());
        }
        
        ListResponse<ServiceResponse> response = new ListResponse<ServiceResponse>();
        List<ServiceResponse> servicesResponses = new ArrayList<ServiceResponse>();
        for (Network.Service service : services) {
        	//skip gateway service
        	if (service == Service.Gateway) {
        		continue;
        	}
            ServiceResponse serviceResponse = _responseGenerator.createNetworkServiceResponse(service);
            servicesResponses.add(serviceResponse);
        }

        response.setResponses(servicesResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
