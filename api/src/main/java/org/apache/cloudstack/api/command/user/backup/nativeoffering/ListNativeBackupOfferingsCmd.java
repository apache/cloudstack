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
package org.apache.cloudstack.api.command.user.backup.nativeoffering;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NativeBackupOfferingResponse;

@APICommand(name = "listNativeBackupOfferings", description = "List native backup offerings", responseObject = NativeBackupOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin}, since = "4.23.0")
public class ListNativeBackupOfferingsCmd extends BaseListCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NativeBackupOfferingResponse.class, description = "Backup offering ID.")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Backup offering name.")
    private String name;

    @Parameter(name = ApiConstants.COMPRESS, type = CommandType.BOOLEAN, description = "Whether the backups should be compressed or not.")
    private Boolean compress;

    @Parameter(name = ApiConstants.VALIDATE, type = CommandType.BOOLEAN, description = "Whether the backups should be validated or not.")
    private Boolean validate;

    @Parameter(name = ApiConstants.ALLOW_QUICK_RESTORE, type = CommandType.BOOLEAN, description = "Whether the backups are allowed to be restored or not.")
    private Boolean allowQuickRestore;

    @Parameter(name = ApiConstants.ALLOW_EXTRACT_FILE, type = CommandType.BOOLEAN, description = "Whether files may be extracted from backups or not.")
    private Boolean allowExtractFile;

    @Parameter(name = ApiConstants.SHOW_REMOVED, type = CommandType.BOOLEAN, description = "Show removed offerings.")
    private boolean showRemoved = false;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean isCompress() {
        return compress;
    }

    public Boolean isValidate() {
        return validate;
    }

    public Boolean isAllowQuickRestore() {
        return allowQuickRestore;
    }

    public Boolean isAllowExtractFile() {
        return allowExtractFile;
    }

    public boolean isShowRemoved() {
        return showRemoved;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        ListResponse<NativeBackupOfferingResponse> offeringResponseList = _queryService.listNativeBackupOfferings(this);
        offeringResponseList.setResponseName(getCommandName());
        this.setResponseObject(offeringResponseList);
    }
}
