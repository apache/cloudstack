package com.cloud.api.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@SuppressWarnings("rawtypes")
@Implementation(responseObject=SuccessResponse.class, description="Deletes a particular ingress rule from this security group")
public class RevokeSecurityGroupIngressCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RevokeSecurityGroupIngressCmd.class.getName());

    private static final String s_name = "revokesecuritygroupingress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the security group. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.CIDR_LIST, type=CommandType.STRING, description="the cidr list associated")
    private String cidrList;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the security group. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.END_PORT, type=CommandType.INTEGER, description="end port for this ingress rule")
    private Integer endPort;

    @Parameter(name=ApiConstants.ICMP_CODE, type=CommandType.INTEGER, description="error code for this icmp message")
    private Integer icmpCode;

    @Parameter(name=ApiConstants.ICMP_TYPE, type=CommandType.INTEGER, description="type for this icmp message")
    private Integer icmpType;

    @Parameter(name=ApiConstants.SECURITY_GROUP_NAME, type=CommandType.STRING, required=true, description="name of the security group")
    private String securityGroupName;

    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, description="protocol used")
    private String protocol;

    @Parameter(name=ApiConstants.START_PORT, type=CommandType.INTEGER,description="start port for this ingress rule")
    private Integer startPort;

    @Parameter(name=ApiConstants.USER_SECURITY_GROUP_LIST, type=CommandType.MAP, description="user to security group mapping")
    private Map userSecurityGroupList;

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

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public Map getUserSecurityGroupList() {
        return userSecurityGroupList;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "revokesecuritygroupingress";
    }

    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SECURITY_GROUP_REVOKE_INGRESS;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (getUserSecurityGroupList() != null) {
            sb.append("group list(group/account): ");
            Collection userGroupCollection = getUserSecurityGroupList().values();
            Iterator iter = userGroupCollection.iterator();

            HashMap userGroup = (HashMap)iter.next();
            String group = (String)userGroup.get("group");
            String authorizedAccountName = (String)userGroup.get("account");
            sb.append(group + "/" + authorizedAccountName);

            while (iter.hasNext()) {
                userGroup = (HashMap)iter.next();
                group = (String)userGroup.get("group");
                authorizedAccountName = (String)userGroup.get("account");
                sb.append(", " + group + "/" + authorizedAccountName);
            }
        } else if (getCidrList() != null) {
            sb.append("cidr list: " + getCidrList());
        } else {
            sb.append("<error:  no ingress parameters>");
        }

        return  "revoking ingress from group: " + getSecurityGroupName() + " for " + sb.toString();
    }
    
    @Override
    public void execute(){
        boolean result = _securityGroupService.revokeSecurityGroupIngress(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to revoke security group ingress rule");
        }
    }
}
