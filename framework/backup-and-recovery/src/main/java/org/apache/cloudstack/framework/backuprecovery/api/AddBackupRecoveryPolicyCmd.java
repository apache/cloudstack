package org.apache.cloudstack.framework.backuprecovery.api;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.backuprecovery.BackupRecoveryManager;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupPolicyResponse;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderResponse;
import org.apache.cloudstack.framework.backuprecovery.impl.BackupPolicyVO;

import javax.inject.Inject;

@APICommand(name = AddBackupRecoveryPolicyCmd.APINAME,
        description = "Adds a Backup policy",
        responseObject = BackupPolicyResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class AddBackupRecoveryPolicyCmd extends BaseCmd {

    public static final String APINAME = "addBackupRecoveryPolicy";

    @Inject
    BackupRecoveryManager backupRecoveryManager;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the policy")
    private String policyName;

    @Parameter(name = ApiConstants.BACKUP_POLICY_ID,
            type = CommandType.STRING,
            entityType = BackupRecoveryProviderResponse.class,
            required = true,
            description = "Backup Recovery Provider ID")
    private String policyId;

    @Parameter(name = ApiConstants.BACKUP_PROVIDER_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = BackupRecoveryProviderResponse.class,
            required = true,
            description = "Backup Recovery Provider ID")
    private Long providerId;

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyId() {
        return policyId;
    }

    public Long getProviderId() {
        return providerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BackupPolicyVO policyVO = backupRecoveryManager.addBackupPolicy(this);
            if (policyVO != null) {
                BackupPolicyResponse response = backupRecoveryManager.createBackupPolicyResponse(policyVO);
                response.setObjectName("backuppolicy");
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add a Backup policy");
            }
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (OperationTimedoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
