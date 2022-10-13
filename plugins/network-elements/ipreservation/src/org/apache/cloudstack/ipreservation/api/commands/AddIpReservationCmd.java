package org.apache.cloudstack.ipreservation.api.commands;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.ipreservation.IpReservationService;
import org.apache.cloudstack.ipreservation.api.response.IpReservationResponse;

import javax.inject.Inject;

@APICommand(name = "addIpReservation", description = "Add a IP Reservation", responseObject = IpReservationResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11.3.0")
public class AddIpReservationCmd extends BaseCmd {

    @Inject
    IpReservationService ipReservationService;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, required = true, description = "Starting IP")
    private String startIp;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, required = true, description = "Ending IP")
    private String endIp;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, required = true, entityType = NetworkResponse.class, description = "Network ID")
    private Long networkId;

    public String getStartIp() {
        return startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        ipReservationService.createReservation(this);
    }

    @Override
    public String getCommandName() {
        return "addipreservationresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
