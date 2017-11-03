package org.apache.cloudstack.api.command.admin.user;

import com.cloud.user.Account;
import com.cloud.user.User;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.region.RegionService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "moveUser", description = "Moves a user to another account", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class MoveUserCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateUserCmd.class.getName());

    private static final String s_name = "moveuseruserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID
            , entityType = UserResponse.class,
            required = true,
            description = "id of the user to be deleted")
    private Long id;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "Creates the user under the specified account. If no account is specified, the username will be used as the account name.")
    private String accountName;

    @Parameter(name = ApiConstants.ACCOUNT_ID,
            type = CommandType.UUID,
            description = "Creates the user under the specified domain. Has to be accompanied with the account parameter")
    private Long accountId;

    @Inject
    RegionService _regionService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getAccountId() {
        return accountId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        User user = _entityMgr.findById(User.class, getId());
        if (user != null) {
            return user.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        Preconditions.checkNotNull(getId(),"I have to have an user to move!");
        Preconditions.checkState(ObjectUtils.anyNotNull(getAccountId(),getAccountName()),"provide either an account name or an account id!");

        CallContext.current().setEventDetails("UserId: " + getId());
        boolean result =
                _regionService.moveUser(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move the user to a new account");
        }
    }

}
