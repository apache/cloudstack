package org.apache.cloudstack.api.command.admin.vm;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMUserDataResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@APICommand(name = "getVirtualMachineUserData", description = "Returns user data associated with the VM", responseObject = VMUserDataResponse.class, since = "4.4")
public class GetVMUserDataCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetVMUserDataCmd.class);
    private static final String s_name = "getvirtualmachineuserdataresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "The ID of the virtual machine")
    private Long vmId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getId() {
        return vmId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        String userData = _userVmService.getVmUserData(getId());
        VMUserDataResponse resp = new VMUserDataResponse();
        resp.setVmId(_entityMgr.findById(UserVm.class, getId()).getUuid());
        resp.setUserData(userData);
        resp.setObjectName("virtualmachineuserdata");
        resp.setResponseName(getCommandName());
        this.setResponseObject(resp);
    }

    @Override
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
