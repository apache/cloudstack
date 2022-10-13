package org.apache.cloudstack.ipreservation.api.commands;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ipreservation.IpReservationService;

import javax.inject.Inject;

@APICommand(name = "removeIpReservation", description = "Remove an IP Reservation", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.11.3.0")
public class RemoveIpReservationCmd extends BaseCmd {

    @Inject
    IpReservationService ipReservationService;

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true, description = "IP Reservation ID")
    private String id;

    public String getId() {
        return id;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        ipReservationService.removeReservation(this);
    }

    @Override
    public String getCommandName() {
        return "removeipreservationresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
