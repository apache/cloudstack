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
package org.apache.cloudstack.api.command.user.volume;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.*;
import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;

import java.util.List;


@APICommand(name = "listVolumeDetails", description="Lists all volume details.", responseObject=VolumeDetailResponse.class)
public class ListVolumeDetailsCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVolumeDetailsCmd.class.getName());

    private static final String s_name = "listvolumedetailsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class,
            required=true, description="the ID of the volume")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the volume detail")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Volume;
    }

    @Override
    public void execute(){
        ListResponse<VolumeDetailResponse> responses = new ListResponse<VolumeDetailResponse>();
        List<VolumeDetailResponse> volumeDetailList = _queryService.searchForVolumeDetails(this);
        responses.setResponses(volumeDetailList);
        responses.setResponseName(getCommandName());
        this.setResponseObject(responses);
    }
}
