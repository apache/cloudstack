package org.apache.cloudstack.api;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name="deleteUcsManager", description="Delete a Ucs manager", responseObject= SuccessResponse.class)
public class DeleteUcsManagerCmd extends BaseCmd {
	private static final Logger logger = Logger.getLogger(DeleteUcsManagerCmd.class);
	
    @Inject
    private UcsManager mgr;

    @Parameter(name=ApiConstants.UCS_MANAGER_ID, type= BaseCmd.CommandType.UUID, description="ucs manager id", entityType=UcsManagerResponse.class, required=true)
    private Long ucsManagerId;

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException,
			NetworkRuleConflictException {
		try {
			mgr.deleteUcsManager(ucsManagerId);
			SuccessResponse rsp = new SuccessResponse();
			rsp.setResponseName(getCommandName());
			rsp.setObjectName("success");
			this.setResponseObject(rsp);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
			throw new CloudRuntimeException(e);
		}
	}

	@Override
	public String getCommandName() {
		return "deleteUcsManagerResponse";
	}

	@Override
	public long getEntityOwnerId() {
		return Account.ACCOUNT_ID_SYSTEM;
	}
}
