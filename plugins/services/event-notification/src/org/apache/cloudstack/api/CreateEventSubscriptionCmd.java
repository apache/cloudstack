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

package org.apache.cloudstack.api;

import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.Implementation;
import com.cloud.api.ServerApiException;
import com.cloud.exception.*;
import org.apache.log4j.Logger;

@Implementation(description="Registers a subscriber for the events", responseObject=EventSubscriptionResponse.class)
public class CreateEventSubscriptionCmd extends BaseAsyncCreateCmd {

    public static final Logger s_logger = Logger.getLogger(CreateEventSubscriptionCmd.class);
    private static final String s_name =  "eventsubscriptionresponse";

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

    }

    @Override
    public String getCommandName() {
       return null;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }

    @Override
    public String getEventType() {
        return null;
    }

    @Override
    public String getEventDescription() {
        return null;
    }

    @Override
    public void create() throws ResourceAllocationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getEntityTable() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}