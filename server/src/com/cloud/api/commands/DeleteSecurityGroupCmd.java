package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;

@Implementation(method="deleteNetworkGroup", manager=Manager.NetworkGroupManager)
public class DeleteSecurityGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSecurityGroupCmd.class.getName());
    private static final String s_name = "deletenetworkgroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="the account of the network group. Must be specified with domain ID")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="the domain ID of account owning the network group")
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true, description="the network group name")
    private String networkGroupName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(Boolean.TRUE);
        response.setResponseName(getName());
        return response;
	}
}
