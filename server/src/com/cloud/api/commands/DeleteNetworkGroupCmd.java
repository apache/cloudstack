package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="deleteNetworkGroup", manager=Manager.NetworkGroupManager)
public class DeleteNetworkGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteNetworkGroupCmd.class.getName());
    private static final String s_name = "deletenetworkgroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true)
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

    public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
//        //Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
//        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
//
//        Long accountId = null;
//        if ((account == null) || isAdmin(account.getType())) {
//            if ((accountName != null) && (domainId != null)) {
//                // if it's an admin account, do a quick permission check
//                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
//                    if (s_logger.isDebugEnabled()) {
//                        s_logger.debug("Unable to find rules network group " + name + ", permission denied.");
//                    }
//                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to network group " + name + ", permission denied.");
//                }
//
//                Account groupOwner = getManagementServer().findActiveAccount(accountName, domainId);
//                if (groupOwner == null) {
//                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
//                }
//                accountId = groupOwner.getId();
//            } else {
//                if (account != null) {
//                    accountId = account.getId();
//                    domainId = account.getDomainId();
//                }
//            }
//        } else {
//            if (account != null) {
//                accountId = account.getId();
//                domainId = account.getDomainId();
//            }
//        }
//
//        if (accountId == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account for network group " + name + "; failed to delete group.");
//        }
//
//        NetworkGroupVO sg = getManagementServer().findNetworkGroupByName(accountId, name);
//        if (sg == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find network group " + name + "; failed to delete group.");
//        }
//
//        try {
//            getManagementServer().deleteNetworkGroup(sg.getId(), accountId);
//        } catch (ResourceInUseException ex) {
//            if (s_logger.isDebugEnabled()) {
//                s_logger.debug("Failed to delete network group " + name + " for account " + accountId + ", group is not empty.");
//            }
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete network group " + name + "; group is not empty.");
//        } catch (PermissionDeniedException pde) {
//        	if (s_logger.isDebugEnabled()) {
//                s_logger.debug("Failed to delete network group " + name + " for account " + accountId + ", default group cannot be deleted");
//            }
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete network group " + name + "; default group cannot be deleted");
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
//        return returnValues;
//    }
    
	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
