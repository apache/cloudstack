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
package org.apache.cloudstack.api.command.user.storage.fileshare;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.storage.fileshare.FileShare;

@APICommand(name = "removeFileShare", responseObject= SuccessResponse.class, description = "Remove a File Share by.. ",
        responseView = ResponseObject.ResponseView.Restricted, entityType = FileShare.class, requestHasSensitiveInfo = false, since = "4.20.0")
public class RemoveFileShareCmd extends BaseAsyncCmd {
    @Override
    public String getEventType() {
        return null;
    }

    @Override
    public String getEventDescription() {
        return null;
    }

    @Override
    public void execute() {

    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
