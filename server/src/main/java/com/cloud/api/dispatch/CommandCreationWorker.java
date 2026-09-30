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

package com.cloud.api.dispatch;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;

import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.context.CallContext;


/**
 * This worker invokes create on the {@link BaseCmd} itself
 *
 * @author afornie
 */
public class CommandCreationWorker implements DispatchWorker {

    private static final String ATTEMP_TO_CREATE_NON_CREATION_CMD =
            "Trying to invoke creation on a Command that is not " +
            BaseAsyncCreateCmd.class.getName();

    @Override
    public void handle(final DispatchTask task) {
        final BaseCmd cmd = task.getCmd();

        if (cmd instanceof BaseAsyncCreateCmd) {
            try {
                CallContext.current().setEventDisplayEnabled(cmd.isDisplay());
                ((BaseAsyncCreateCmd)cmd).create();
            } catch (final ResourceAllocationException e) {
                throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR,
                        e.getMessage(), e);
            }
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    ATTEMP_TO_CREATE_NON_CREATION_CMD);
        }
    }

}
