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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NativeBackupOfferingResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.NativeBackupOffering;
import org.apache.cloudstack.backup.NativeBackupOfferingService;

import javax.inject.Inject;

@APICommand(name = "createNativeBackupOffering", description = "Creates a native backup offering", responseObject = NativeBackupOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin}, since = "4.23.0")
public class CreateNativeBackupOfferingCmd extends BaseCmd {

    @Inject
    private NativeBackupOfferingService nativeBackupOfferingService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Backup offering name.", required = true)
    private String name;

    @Parameter(name = ApiConstants.COMPRESS, type = CommandType.BOOLEAN, description = "Whether the backups should be compressed or not.")
    private Boolean compress;

    @Parameter(name = ApiConstants.VALIDATE, type = CommandType.BOOLEAN, description = "Whether the backups should be validated or not.")
    private Boolean validate;

    @Parameter(name = ApiConstants.VALIDATION_STEPS, type = CommandType.STRING, description = "Which validation steps should be performed. Accepts a comma-separated list of " +
            "steps. Accepted values are: wait_for_boot, screenshot and execute_command.")
    private String validationSteps;

    @Parameter(name = ApiConstants.ALLOW_QUICK_RESTORE, type = CommandType.BOOLEAN, description = "Whether the backups are allowed to be restored or not.")
    private Boolean allowQuickRestore;

    @Parameter(name = ApiConstants.ALLOW_EXTRACT_FILE, type = CommandType.BOOLEAN, description = "Whether files may be extracted from backups or not.")
    private Boolean allowExtractFile;

    @Parameter(name = ApiConstants.BACKUP_CHAIN_SIZE, type = CommandType.INTEGER, description = "Backup chain size for backups created with this offering.")
    private Integer backupChainSize;

    @Parameter(name = ApiConstants.COMPRESSION_LIBRARY, type = CommandType.STRING, description = "Compression library, for offerings that support compression. Accepted values " +
            "are zstd and zlib. By default, zstd is used for images that support it. If the image only supports zlib, it will be used regardless of this parameter.")
    private String compressionLibrary;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public boolean isCompress() {
        return Boolean.TRUE.equals(compress);
    }

    public boolean isValidate() {
        return Boolean.TRUE.equals(validate);
    }

    public boolean isAllowQuickRestore() {
        return Boolean.TRUE.equals(allowQuickRestore);
    }

    public boolean isAllowExtractFile() {
        return Boolean.TRUE.equals(allowExtractFile);
    }

    public Integer getBackupChainSize() {
        return backupChainSize;
    }

    public Backup.CompressionLibrary getCompressionLibrary() {
        if (compressionLibrary == null) {
            return null;
        }
        try {
            return Backup.CompressionLibrary.valueOf(compressionLibrary);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(String.format("Invalid compression library, accepted values are zstd and zlib, received [%s].", compressionLibrary));
        }
    }

    public String getValidationSteps() {
        if (validationSteps == null) {
            return Backup.ValidationSteps.screenshot.name();
        }
        StringBuilder sb = new StringBuilder();
        for (String step : validationSteps.strip().split(",")) {
            try {
                Backup.ValidationSteps enumStep = Backup.ValidationSteps.valueOf(step);
                sb.append(enumStep.name());
                sb.append(",");
            } catch (IllegalArgumentException ex) {
                logger.error("Invalid validation step informed [{}].", step, ex);
                throw new InvalidParameterValueException(String.format("Invalid validation step [%s] informed. Accepted values are: wait_for_boot, screenshot and script.", step));
            }
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        NativeBackupOffering offering = nativeBackupOfferingService.createNativeBackupOffering(this);
        NativeBackupOfferingResponse response = _responseGenerator.createNativeBackupOfferingResponse(offering);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
