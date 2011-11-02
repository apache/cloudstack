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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ClusterResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.org.Cluster;

@Implementation(description="Lists clusters.", responseObject=ClusterResponse.class)
public class ListClustersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listclustersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="cluster")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="lists clusters by the cluster ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="lists clusters by the cluster name")
    private String clusterName;

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="lists clusters by Pod ID")
    private Long podId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists clusters by Zone ID")
    private Long zoneId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="lists clusters by hypervisor type")
    private String hypervisorType;

    @Parameter(name=ApiConstants.CLUSTER_TYPE, type=CommandType.STRING, description="lists clusters by cluster type")
    private String clusterType;
    
    @Parameter(name=ApiConstants.ALLOCATION_STATE, type=CommandType.STRING, description="lists clusters by allocation state")
    private String allocationState;
    
    @Parameter(name=ApiConstants.MANAGED_STATE, type=CommandType.STRING, description="whether this cluster is managed by cloudstack")
    private String managedState;
    
    @Parameter(name=ApiConstants.SHOW_CAPACITIES, type=CommandType.BOOLEAN, description="flag to display the capacity of the clusters")
    private Boolean showCapacities;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getClusterName() {
        return clusterName;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public String getHypervisorType() {
    	return hypervisorType;
    }
    
    public String getClusterType() {
    	return clusterType;
    }

    public String getAllocationState() {
    	return allocationState;
    }
    

    public String getManagedstate() {
        return managedState;
    }

    public void setManagedstate(String managedstate) {
        this.managedState = managedstate;
    }


    public Boolean getShowCapacities() {
		return showCapacities;
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
        List<? extends Cluster> result = _mgr.searchForClusters(this);
        ListResponse<ClusterResponse> response = new ListResponse<ClusterResponse>();
        List<ClusterResponse> clusterResponses = new ArrayList<ClusterResponse>();
        for (Cluster cluster : result) {
            ClusterResponse clusterResponse = _responseGenerator.createClusterResponse(cluster,showCapacities);
            clusterResponse.setObjectName("cluster");
            clusterResponses.add(clusterResponse);
        }

        response.setResponses(clusterResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
