package org.apache.cloudstack.api.command.user.volume;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.log4j.Logger;


@APICommand(name="assignVolume",
        description="Change ownership of a Volume from one account to another. A root administrator can reassign a VM from any account to any other account in any domain. A domain administrator can reassign a VM to any account in the same domain.",
        responseObject = VolumeResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.11.3.0", entityType = {Volume.class},
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin})
public class AssignVolumeCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssignVolumeCmd.class.getName());

    @Parameter(name= ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "id of the volume to be moved")
    private Long volumeId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account name of the new VM owner.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain id of the new VM owner.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the new VM owner.")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVolumeId() {
        return volumeId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return "assignvolumeresponse";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            Volume volume = _volumeService.assignVolume(this);
            if (volume == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign volume");
            }
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseObject.ResponseView.Restricted, volume);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            e.printStackTrace();
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move volume: " + e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _responseGenerator.findVolumeById(getVolumeId());
        if (volume != null) {
            return volume.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
