package org.apache.cloudstack.api.command.user.backup.repository;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupRepositoryResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.backup.BackupRepositoryService;

import javax.inject.Inject;

@APICommand(name = "deleteBackupRepository",
        description = "delete a backup repository",
        responseObject = SuccessResponse.class, since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteBackupRepositoryCmd extends BaseCmd {

    @Inject
    BackupRepositoryService backupRepositoryService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = BackupRepositoryResponse.class,
            required = true,
            description = "ID of the backup repository to be deleted")
    private Long id;


    /////////////////////////////////////////////////////
    //////////////// Accessors //////////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        boolean result = backupRepositoryService.deleteBackupRepository(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete backup repository");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}