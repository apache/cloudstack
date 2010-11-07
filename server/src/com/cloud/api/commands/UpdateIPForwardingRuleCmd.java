package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;

@Implementation(method="updatePortForwardingRule", manager=ManagementServer.class, description="Updates a port forwarding rule.  Only the private port and the virtual machine can be updated.")
public class UpdateIPForwardingRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIPForwardingRuleCmd.class.getName());
    private static final String s_name = "updateportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PRIVATE_IP, type=CommandType.STRING, description="the private IP address of the port forwarding rule")
    private String privateIp;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.STRING, required=true, description="the private port of the port forwarding rule")
    private String privatePort;

    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, required=true, description="the protocol for the port fowarding rule. Valid values are TCP or UDP.")
    private String protocol;

    @Parameter(name=ApiConstants.PUBLIC_IP, type=CommandType.STRING, required=true, description="the public IP address of the port forwarding rule")
    private String publicIp;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.STRING, required=true, description="the public port of the port forwarding rule")
    private String publicPort;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine for the port forwarding rule")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public long getAccountId() {
        IPAddressVO addr = ApiDBUtils.findIpAddressById(getPublicIp());
        if (addr != null) {
            return addr.getAccountId();
        }

        // bad address given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_MODIFY;
    }

    @Override
    public String getEventDescription() {
        return  "updating port forwarding rule";
    }

	@Override @SuppressWarnings("unchecked")
	public FirewallRuleResponse getResponse() {
	    FirewallRuleVO fwRule = (FirewallRuleVO)getResponseObject();
	    FirewallRuleResponse response = ApiResponseHelper.createFirewallRuleResponse(fwRule);
	    response.setResponseName(getName());
	    return response;
	}
	
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        FirewallRuleVO result = _mgr.updatePortForwardingRule(this);
        return result;
    }
}
