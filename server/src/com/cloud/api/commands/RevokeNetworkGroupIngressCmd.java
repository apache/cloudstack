package com.cloud.api.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@SuppressWarnings("rawtypes")
@Implementation(method="revokeNetworkGroupIngress", manager=NetworkGroupManager.class)
public class RevokeNetworkGroupIngressCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RevokeNetworkGroupIngressCmd.class.getName());

    private static final String s_name = "revokenetworkgroupingress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //FIXME - add description
    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    //FIXME - add description
    @Parameter(name="cidrlist", type=CommandType.STRING)
    private String cidrList;

    //FIXME - add description
    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    //FIXME - add description
    @Parameter(name="endport", type=CommandType.INTEGER)
    private Integer endPort;

    //FIXME - add description
    @Parameter(name="icmpcode", type=CommandType.INTEGER)
    private Integer icmpCode;

    //FIXME - add description
    @Parameter(name="icmptype", type=CommandType.INTEGER)
    private Integer icmpType;

    //FIXME - add description
    @Parameter(name="networkgroupname", type=CommandType.STRING, required=true)
    private String networkGroupName;

    //FIXME - add description
    @Parameter(name="protocol", type=CommandType.STRING)
    private String protocol;

    //FIXME - add description
    @Parameter(name="startport", type=CommandType.INTEGER)
    private Integer startPort;

    //FIXME - add description
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

    @Override
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = ApiDBUtils.findAccountByNameDomain(accountName, domainId);
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
        return EventTypes.EVENT_NETWORK_GROUP_REVOKE_INGRESS;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (getUserNetworkGroupList() != null) {
            sb.append("group list(group/account): ");
            Collection userGroupCollection = getUserNetworkGroupList().values();
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

        return  "revoking ingress from group: " + getNetworkGroupName() + " for " + sb.toString();
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
