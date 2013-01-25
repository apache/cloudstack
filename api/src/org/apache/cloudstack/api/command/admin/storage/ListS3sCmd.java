/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.storage;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.S3Response;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.S3;

@APICommand(name = "listS3s", description = "Lists S3s", responseObject = S3Response.class, since = "4.0.0")
public final class ListS3sCmd extends BaseListCmd {

    private static final String COMMAND_NAME = "lists3sresponse";

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException,
            ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {

        final List<? extends S3> result = _resourceService.listS3s(this);
        final ListResponse<S3Response> response = new ListResponse<S3Response>();
        final List<S3Response> s3Responses = new ArrayList<S3Response>();

        if (result != null) {

            for (S3 s3 : result) {

                S3Response s3Response = _responseGenerator.createS3Response(s3);
                s3Response.setResponseName(this.getCommandName());
                s3Response.setObjectName("s3");
                s3Responses.add(s3Response);

            }

        }

        response.setResponses(s3Responses);
        response.setResponseName(this.getCommandName());

        this.setResponseObject(response);

    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

}
