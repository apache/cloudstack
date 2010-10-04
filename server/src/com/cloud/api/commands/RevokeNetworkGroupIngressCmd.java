package com.cloud.api.commands;

import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;

@SuppressWarnings("rawtypes")
@Implementation(method="revokeNetworkGroupIngress", manager=Manager.NetworkGroupManager)
public class RevokeNetworkGroupIngressCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RevokeNetworkGroupIngressCmd.class.getName());

    private static final String s_name = "revokenetworkgroupingress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="cidrlist", type=CommandType.STRING)
    private String cidrList;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="endport", type=CommandType.INTEGER)
    private Integer endPort;

    @Parameter(name="icmpcode", type=CommandType.INTEGER)
    private Integer icmpCode;

    @Parameter(name="icmptype", type=CommandType.INTEGER)
    private Integer icmpType;

    @Parameter(name="networkgroupname", type=CommandType.STRING, required=true)
    private String networkGroupName;

    @Parameter(name="protocol", type=CommandType.STRING)
    private String protocol;

    @Parameter(name="startport", type=CommandType.INTEGER)
    private Integer startPort;

    @Parameter(name="usernetworkgrouplist", type=CommandType.MAP)
    private Map userNetworkGroupList;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public String getCidrList() {
        return cidrList;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public Map getUserNetworkGroupList() {
        return userNetworkGroupList;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "revokenetworkgroupingress";
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
	}
}
