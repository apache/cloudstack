package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.network.FirewallRuleVO;
import com.cloud.uservm.UserVm;

@Implementation(method="updatePortForwardingRule", manager=Manager.ManagementServer)
public class UpdateIPForwardingRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIPForwardingRuleCmd.class.getName());
    private static final String s_name = "updateportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="privateip", type=CommandType.STRING)
    private String privateIp;

    @Parameter(name="privateport", type=CommandType.STRING, required=true)
    private String privatePort;

    @Parameter(name="protocol", type=CommandType.STRING, required=true)
    private String protocol;

    @Parameter(name="publicip", type=CommandType.STRING, required=true)
    private String publicIp;

    @Parameter(name="publicport", type=CommandType.STRING, required=true)
    private String publicPort;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
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

	@Override @SuppressWarnings("unchecked")
	public FirewallRuleResponse getResponse() {
	    FirewallRuleVO fwRule = (FirewallRuleVO)getResponseObject();

	    FirewallRuleResponse response = new FirewallRuleResponse();
	    response.setId(fwRule.getId());
	    response.setPrivatePort(fwRule.getPrivatePort());
	    response.setProtocol(fwRule.getProtocol());
	    response.setPublicPort(fwRule.getPublicPort());

	    UserVm vm = ApiDBUtils.findUserVmByPublicIpAndGuestIp(fwRule.getPublicIpAddress(), fwRule.getPrivateIpAddress());
	    response.setVirtualMachineId(vm.getId());
	    response.setVirtualMachineName(vm.getName());

	    response.setResponseName(getName());
	    return response;
	}
}
